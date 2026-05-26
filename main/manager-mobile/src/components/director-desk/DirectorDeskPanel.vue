<script lang="ts" setup>
import type { Agent } from '@/api/agent/types'
import type { Device } from '@/api/device/types'
import { onHide, onShow, onUnload } from '@dcloudio/uni-app'
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { getMyBenefits } from '@/api/activation-code/activation-code'
import { getAgentList } from '@/api/agent/agent'
import { getBindDevices, getDeviceAutoStartPlan, getDeviceStatus, sendDeviceCommand, updateDeviceAutoStartPlan } from '@/api/device'
import { getLivePlanDetail, getLivePlanList } from '@/api/live-plan/live-plan'
import { getLiveStatus, getSentMsg, sendManualMsg, startLive, stopLive } from '@/api/live-streaming/live-streaming'
import { toast } from '@/utils/toast'

defineOptions({
  name: 'DirectorDeskMobile',
})

interface RobotItem {
  id: string
  agentId: string
  mac: string
  model: string
  agentName: string
  name: string
  meta: string
  updatedAt: string
  online: boolean
  deviceStatus: 'online' | 'offline'
  rawUpdatedTime: number
}

interface LivePlanOption {
  planNo: string
  planName: string
  roomId: string
  platform: string
  displayLabel: string
}

interface ConsoleMessage {
  id: string
  text: string
  time: string
}

const robots = ref<RobotItem[]>([])
const selectedRobotId = ref('')
const showRobotPopup = ref(false)
const currentRobotFocus = ref(false)

const livePlanOptions = ref<LivePlanOption[]>([])
const selectedPlanNoMap = ref<Record<string, string>>({})
const autoStartPlanNoMap = ref<Record<string, string>>({})

const pendingMessage = ref('')
const consoleMessages = ref<ConsoleMessage[]>([])
const consoleMessageMap = ref<Record<string, ConsoleMessage[]>>({})
const sentMsgAfterIdMap = ref<Record<string, number>>({})
const screenBrightness = ref(100)

const benefitLabel = ref('有效期')
const benefitValue = ref('-')
const canStartLiveByBenefits = ref(false)
const benefitMembershipActive = ref(false)
const benefitMembershipEndAt = ref('')
const benefitMembershipDailyRemainingSeconds = ref(0)
const benefitBalanceSeconds = ref(0)

const startLiveLoading = ref(false)
const autoStartPlanLoading = ref(false)
const screenBrightnessLoading = ref(false)
const listLoading = ref(false)
const refreshing = ref(false)

const isLiveRunning = ref(false)
const liveStartedAtText = ref('')
const liveRuntimeText = ref('')
const roomStatusText = ref('-')
const roomStatusRaw = ref('')
const currentViewerCountText = ref('-')
const deviceOnline = ref<boolean | null>(null)
const deviceOfflineDurationSeconds = ref(0)
const roomEndedDurationSeconds = ref(0)

const sentMsgTimer = ref<ReturnType<typeof setTimeout> | null>(null)
const liveStatusTimer = ref<ReturnType<typeof setTimeout> | null>(null)
const robotStatusTimer = ref<ReturnType<typeof setTimeout> | null>(null)
const currentRobotFocusTimer = ref<ReturnType<typeof setTimeout> | null>(null)
const robotStatusPolling = ref(false)
const activeMonitorDeviceId = ref('')
const sentMsgAfterId = ref(0)
const sentMsgLimit = 20
const maxConsoleMessagesPerDevice = 200

const selectedRobot = computed(() => {
  return robots.value.find(item => item.id === selectedRobotId.value) || null
})

const selectedPlanLabel = computed(() => {
  const target = livePlanOptions.value.find(item => item.planNo === currentSelectedPlanNo.value)
  return target ? target.displayLabel : '请选择导播方案'
})

const selectedPlanIndex = computed(() => {
  return livePlanOptions.value.findIndex(item => item.planNo === currentSelectedPlanNo.value)
})

const currentSelectedPlanNo = computed({
  get() {
    if (!selectedRobotId.value)
      return ''
    return selectedPlanNoMap.value[selectedRobotId.value] || ''
  },
  set(value: string) {
    if (!selectedRobotId.value)
      return
    selectedPlanNoMap.value = {
      ...selectedPlanNoMap.value,
      [selectedRobotId.value]: value || '',
    }
  },
})

const currentAutoStartPlanNo = computed(() => {
  if (!selectedRobotId.value)
    return ''
  return autoStartPlanNoMap.value[selectedRobotId.value] || ''
})

const isCurrentPlanAutoStart = computed(() => {
  return !!currentSelectedPlanNo.value && currentAutoStartPlanNo.value === currentSelectedPlanNo.value
})

function delay(ms: number) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

function setDeviceMicrophoneEnabled(deviceId: string, enabled: boolean) {
  return sendDeviceCommand(deviceId, {
    type: 'mcp',
    payload: {
      jsonrpc: '2.0',
      id: 1,
      method: 'tools/call',
      params: {
        name: 'self.audio_microphone.set_enabled',
        arguments: {
          enabled,
        },
      },
    },
  })
}

function setDeviceScreenBrightness(deviceId: string, brightness: number) {
  return sendDeviceCommand(deviceId, {
    type: 'mcp',
    payload: {
      jsonrpc: '2.0',
      id: 1,
      method: 'tools/call',
      params: {
        name: 'self.screen.set_brightness',
        arguments: {
          brightness,
        },
      },
    },
  })
}

async function handleManualMicrophoneSwitch(enabled: boolean) {
  const target = selectedRobot.value
  if (!target) {
    toast.warning('请先选择机器人')
    return
  }

  try {
    await setDeviceMicrophoneEnabled(target.id, enabled)
    toast.success(enabled ? '已开启麦克风' : '已关闭麦克风')
  }
  catch (error: any) {
    console.error(`${enabled ? '开启' : '关闭'}麦克风失败:`, error)
    toast.error(error?.message || `${enabled ? '开启' : '关闭'}麦克风失败`)
  }
}

function updateScreenBrightness(value: number) {
  const nextValue = Math.round(Number(value))
  if (!Number.isFinite(nextValue))
    return
  screenBrightness.value = Math.min(100, Math.max(0, nextValue))
}

async function handleSetScreenBrightness() {
  const target = selectedRobot.value
  if (!target) {
    toast.warning('请先选择机器人')
    return
  }

  if (screenBrightnessLoading.value)
    return

  try {
    screenBrightnessLoading.value = true
    await setDeviceScreenBrightness(target.id, screenBrightness.value)
    toast.success(`已设置屏幕亮度为 ${screenBrightness.value}%`)
  }
  catch (error: any) {
    console.error('设置屏幕亮度失败:', error)
    toast.error(error?.message || '设置屏幕亮度失败')
  }
  finally {
    screenBrightnessLoading.value = false
  }
}

const planPickerRange = computed(() => {
  return livePlanOptions.value.map(item => item.displayLabel)
})

const abnormalStatusList = computed(() => {
  const list: { key: string, label: string, durationText: string }[] = []

  if (roomStatusRaw.value === 'ended') {
    list.push({
      key: 'roomClosed',
      label: '直播间关闭',
      durationText: formatRuntimeSeconds(roomEndedDurationSeconds.value),
    })
  }

  if (deviceOnline.value === false) {
    list.push({
      key: 'robotOffline',
      label: '机器人不在线',
      durationText: formatRuntimeSeconds(deviceOfflineDurationSeconds.value),
    })
  }

  return list
})

const consoleAnchorId = computed(() => {
  const last = consoleMessages.value[consoleMessages.value.length - 1]
  return last ? `console-${last.id}` : ''
})

function parseDateValue(value?: string | number | Date | null) {
  if (value instanceof Date)
    return value

  if (typeof value === 'number')
    return new Date(value)

  if (!value)
    return new Date(Number.NaN)

  const raw = String(value).trim()
  const normalized = /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(raw)
    ? raw.replace(' ', 'T')
    : raw

  return new Date(normalized)
}

function formatDisplayTime(value?: string) {
  if (!value)
    return '刚刚'

  const date = parseDateValue(value)
  if (Number.isNaN(date.getTime()))
    return String(value)

  const diffMs = Date.now() - date.getTime()
  if (diffMs < 60000)
    return '刚刚'

  const diffMinutes = Math.floor(diffMs / 60000)
  if (diffMinutes < 60)
    return `${diffMinutes}分钟前`

  const diffHours = Math.floor(diffMinutes / 60)
  if (diffHours < 24)
    return `${diffHours}小时前`

  const diffDays = Math.floor(diffHours / 24)
  if (diffDays <= 7)
    return `${diffDays}天前`

  return formatAbsoluteTime(value)
}

function formatAbsoluteTime(value?: string) {
  if (!value)
    return '-'

  const date = parseDateValue(value)
  if (Number.isNaN(date.getTime()))
    return String(value)

  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  const hour = `${date.getHours()}`.padStart(2, '0')
  const minute = `${date.getMinutes()}`.padStart(2, '0')
  const second = `${date.getSeconds()}`.padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`
}

function formatBenefitDuration(value: number) {
  const totalSeconds = Number(value)
  if (!Number.isFinite(totalSeconds) || totalSeconds <= 0)
    return '0分'

  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor(totalSeconds / 60)
  if (hours > 0)
    return `${hours}小时${minutes % 60}分`

  return `${minutes}分`
}

function formatRuntimeSeconds(value: number) {
  const totalSeconds = Number(value)
  if (!Number.isFinite(totalSeconds) || totalSeconds < 0)
    return '0秒'

  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60

  if (hours > 0)
    return `${hours}小时${minutes}分${seconds}秒`
  if (minutes > 0)
    return `${minutes}分${seconds}秒`
  return `${seconds}秒`
}

function formatMessageTime(value?: string) {
  if (!value)
    return buildTimeText()

  const date = parseDateValue(value)
  if (Number.isNaN(date.getTime()))
    return buildTimeText()

  const hour = `${date.getHours()}`.padStart(2, '0')
  const minute = `${date.getMinutes()}`.padStart(2, '0')
  const second = `${date.getSeconds()}`.padStart(2, '0')
  return `${hour}:${minute}:${second}`
}

function buildTimeText() {
  const now = new Date()
  const hour = `${now.getHours()}`.padStart(2, '0')
  const minute = `${now.getMinutes()}`.padStart(2, '0')
  const second = `${now.getSeconds()}`.padStart(2, '0')
  return `${hour}:${minute}:${second}`
}

function updateDeviceStatusFromResponse(targetRobots: RobotItem[], deviceStatusMap: Record<string, any>) {
  targetRobots.forEach((robot) => {
    const macAddress = robot.mac ? robot.mac.replace(/:/g, '_') : 'unknown'
    const groupId = robot.model ? robot.model.replace(/:/g, '_') : 'GID_default'
    const mqttClientId = `${groupId}@@@${macAddress}@@@${macAddress}`

    const statusInfo = deviceStatusMap[mqttClientId]
    if (!statusInfo) {
      robot.deviceStatus = 'offline'
      robot.online = false
      return
    }

    let isOnline = false
    if (statusInfo.isAlive === true) {
      isOnline = true
    }
    else if (statusInfo.isAlive === false) {
      isOnline = false
    }
    else if (statusInfo.isAlive === null && statusInfo.exists === true) {
      isOnline = true
    }

    robot.deviceStatus = isOnline ? 'online' : 'offline'
    robot.online = isOnline
  })
}

async function refreshRobotOnlineStatuses() {
  if (!robots.value.length || robotStatusPolling.value)
    return

  robotStatusPolling.value = true
  try {
    const robotsByAgent = robots.value.reduce<Record<string, RobotItem[]>>((groups, robot) => {
      if (!robot.agentId)
        return groups
      if (!groups[robot.agentId])
        groups[robot.agentId] = []
      groups[robot.agentId].push(robot)
      return groups
    }, {})

    await Promise.all(Object.entries(robotsByAgent).map(async ([agentId, agentRobots]) => {
      try {
        const rawStatus: any = await getDeviceStatus(agentId)
        const statusData = typeof rawStatus === 'string' ? JSON.parse(rawStatus) : rawStatus
        if (statusData && typeof statusData === 'object')
          updateDeviceStatusFromResponse(agentRobots, statusData)
      }
      catch {
        // Keep the last known status for this agent group.
      }
    }))

    sortRobots()
  }
  finally {
    robotStatusPolling.value = false
  }
}

function sortRobots() {
  robots.value = [...robots.value].sort((a, b) => {
    if (a.online !== b.online)
      return a.online ? -1 : 1
    return (b.rawUpdatedTime || 0) - (a.rawUpdatedTime || 0)
  })
}

async function fetchUserBenefits() {
  try {
    const benefit: any = await getMyBenefits()
    benefitMembershipActive.value = benefit?.membershipActive === true
    benefitMembershipEndAt.value = benefit?.membershipEndAt || ''
    const balanceSeconds = Number(benefit?.balanceSeconds) || 0
    benefitBalanceSeconds.value = balanceSeconds

    if (benefitMembershipActive.value && benefitMembershipEndAt.value) {
      const dailyRemainingSeconds = Number(benefit?.membershipDailyRemainingSeconds) || 0
      benefitMembershipDailyRemainingSeconds.value = dailyRemainingSeconds
      benefitLabel.value = '账户余额'
      benefitValue.value = `月卡今日 ${formatBenefitDuration(dailyRemainingSeconds)} / 点卡 ${formatBenefitDuration(balanceSeconds)}`
      canStartLiveByBenefits.value = true
      return
    }

    benefitLabel.value = '账户余额'
    benefitValue.value = `点卡 ${formatBenefitDuration(balanceSeconds)}`
    canStartLiveByBenefits.value = balanceSeconds > 0
  }
  catch {
    benefitMembershipActive.value = false
    benefitMembershipEndAt.value = ''
    benefitMembershipDailyRemainingSeconds.value = 0
    benefitBalanceSeconds.value = 0
    benefitLabel.value = '有效期'
    benefitValue.value = '-'
    canStartLiveByBenefits.value = false
  }
}

async function fetchLivePlanData() {
  try {
    const response: any = await getLivePlanList({ page: 1, limit: 100 })
    const list = (response?.list || []).map((item: any) => ({
      planNo: item.planNo,
      planName: item.planName,
      roomId: item.roomId,
      platform: item.platform,
      displayLabel: `${item.planName}(${item.roomId})`,
    }))
    livePlanOptions.value = list
    ensureRobotPlanSelections()
  }
  catch (error: any) {
    toast.error(error?.message || '获取导播方案失败')
  }
}

function ensureRobotPlanSelection(robotId?: string) {
  if (!robotId || !livePlanOptions.value.length)
    return
  if (!selectedPlanNoMap.value[robotId]) {
    selectedPlanNoMap.value = {
      ...selectedPlanNoMap.value,
      [robotId]: livePlanOptions.value[0].planNo,
    }
  }
}

function ensureRobotPlanSelections() {
  robots.value.forEach(robot => ensureRobotPlanSelection(robot.id))
}

function setRobotPlanSelectionByPlanNo(robotId?: string, planNo?: string) {
  if (!robotId || !planNo)
    return false
  const matchedPlan = livePlanOptions.value.find(item => String(item.planNo) === String(planNo))
  if (!matchedPlan)
    return false
  selectedPlanNoMap.value = {
    ...selectedPlanNoMap.value,
    [robotId]: matchedPlan.planNo,
  }
  return true
}

async function fetchAutoStartPlan(robotId?: string) {
  if (!robotId)
    return

  try {
    const plan: any = await getDeviceAutoStartPlan(robotId)
    const planNo = plan?.planNo || plan?.plan_no || ''
    if (!planNo) {
      autoStartPlanNoMap.value = {
        ...autoStartPlanNoMap.value,
        [robotId]: '',
      }
      return
    }

    const matched = livePlanOptions.value.find(item => String(item.planNo) === String(planNo))
    if (!matched) {
      await updateDeviceAutoStartPlan(robotId, '')
      autoStartPlanNoMap.value = {
        ...autoStartPlanNoMap.value,
        [robotId]: '',
      }
      return
    }

    setRobotPlanSelectionByPlanNo(robotId, matched.planNo)
    autoStartPlanNoMap.value = {
      ...autoStartPlanNoMap.value,
      [robotId]: matched.planNo,
    }
  }
  catch {
    autoStartPlanNoMap.value = {
      ...autoStartPlanNoMap.value,
      [robotId]: '',
    }
  }
}

async function fetchBindDevicesWithStatus(agent: Agent) {
  const devices = await getBindDevices(agent.id)
  const mappedList: RobotItem[] = (devices || []).map((device: Device) => {
    const updatedAtSource = device.lastConnectedAt || device.updateDate || device.createDate
    return {
      id: device.id,
      agentId: agent.id,
      mac: device.macAddress,
      model: device.board,
      agentName: agent.agentName,
      name: `${device.board || 'Unknown'} - ${agent.agentName || '未命名智能体'}`,
      meta: `${device.macAddress || '-'} | ${device.board || '-'} | ${device.appVersion || '-'}`,
      updatedAt: formatDisplayTime(updatedAtSource),
      online: false,
      deviceStatus: 'offline',
      rawUpdatedTime: parseDateValue(updatedAtSource || 0).getTime() || 0,
    }
  })

  try {
    const rawStatus: any = await getDeviceStatus(agent.id)
    const statusData = typeof rawStatus === 'string' ? JSON.parse(rawStatus) : rawStatus
    if (statusData && typeof statusData === 'object')
      updateDeviceStatusFromResponse(mappedList, statusData)
  }
  catch {
    // ignore status error, keep offline default
  }

  return mappedList
}

async function fetchRobotList() {
  try {
    listLoading.value = true
    const agents = await getAgentList()
    if (!agents?.length) {
      robots.value = []
      selectedRobotId.value = ''
      resetLiveStatusDisplay()
      return
    }

    const robotGroups = await Promise.all(agents.map(agent => fetchBindDevicesWithStatus(agent)))
    const nextRobots = robotGroups.flat()
    robots.value = nextRobots
    sortRobots()

    if (!robots.value.length) {
      selectedRobotId.value = ''
      resetLiveStatusDisplay()
      return
    }

    const stillExists = robots.value.some(item => item.id === selectedRobotId.value)
    selectedRobotId.value = stillExists ? selectedRobotId.value : robots.value[0].id
    ensureRobotPlanSelections()
    await fetchLiveStatus()
    await fetchAutoStartPlan(selectedRobotId.value)
  }
  catch (error: any) {
    robots.value = []
    selectedRobotId.value = ''
    resetLiveStatusDisplay()
    toast.error(error?.message || '获取机器人列表失败')
  }
  finally {
    listLoading.value = false
  }
}

function setConsoleMessagesForDevice(deviceId: string) {
  consoleMessages.value = deviceId && consoleMessageMap.value[deviceId]
    ? consoleMessageMap.value[deviceId].slice()
    : []
}

function appendSentMessages(messages: any[], deviceId: string) {
  if (!deviceId)
    return

  const deviceMessages = consoleMessageMap.value[deviceId]
    ? consoleMessageMap.value[deviceId].slice()
    : []

  messages.forEach((item) => {
    deviceMessages.push({
      id: `sent-${item.id}`,
      text: item.prompt || '',
      time: formatMessageTime(item.created_at),
    })
    sentMsgAfterIdMap.value[deviceId] = Math.max(
      Number(sentMsgAfterIdMap.value[deviceId]) || 0,
      Number(item.id) || 0,
    )
  })

  const trimmedMessages = deviceMessages.slice(-maxConsoleMessagesPerDevice)
  consoleMessageMap.value[deviceId] = trimmedMessages
  if (selectedRobot.value?.mac === deviceId)
    consoleMessages.value = trimmedMessages.slice()

  sentMsgAfterId.value = Number(sentMsgAfterIdMap.value[deviceId]) || 0
}

function stopSentMsgPolling() {
  if (sentMsgTimer.value) {
    clearTimeout(sentMsgTimer.value)
    sentMsgTimer.value = null
  }
}

function scheduleSentMsgPolling() {
  stopSentMsgPolling()
  sentMsgTimer.value = setTimeout(() => {
    void pollSentMessages()
  }, 3000)
}

async function pollSentMessages() {
  const deviceId = activeMonitorDeviceId.value
  if (!deviceId)
    return

  try {
    const messages: any[] = await getSentMsg(deviceId, {
      afterId: sentMsgAfterId.value,
      limit: sentMsgLimit,
    })

    if (activeMonitorDeviceId.value !== deviceId)
      return

    if (Array.isArray(messages) && messages.length)
      appendSentMessages(messages, deviceId)
  }
  catch {
    // ignore polling errors
  }
  finally {
    if (activeMonitorDeviceId.value === deviceId)
      scheduleSentMsgPolling()
  }
}

function startSentMsgPolling(deviceId: string) {
  stopSentMsgPolling()
  activeMonitorDeviceId.value = deviceId
  sentMsgAfterId.value = Number(sentMsgAfterIdMap.value[deviceId]) || 0
  setConsoleMessagesForDevice(deviceId)
  void pollSentMessages()
}

function stopSentMsgTracking() {
  stopSentMsgPolling()
  activeMonitorDeviceId.value = ''
  sentMsgAfterId.value = 0
}

function stopLiveStatusPolling() {
  if (liveStatusTimer.value) {
    clearTimeout(liveStatusTimer.value)
    liveStatusTimer.value = null
  }
}

function stopRobotStatusPolling() {
  if (robotStatusTimer.value) {
    clearTimeout(robotStatusTimer.value)
    robotStatusTimer.value = null
  }
}

function scheduleLiveStatusPolling() {
  stopLiveStatusPolling()
  liveStatusTimer.value = setTimeout(() => {
    void fetchLiveStatus()
  }, 5000)
}

function scheduleRobotStatusPolling() {
  stopRobotStatusPolling()
  if (!showRobotPopup.value)
    return

  robotStatusTimer.value = setTimeout(() => {
    void pollRobotOnlineStatuses()
  }, 5000)
}

async function pollRobotOnlineStatuses() {
  if (!showRobotPopup.value)
    return

  try {
    await refreshRobotOnlineStatuses()
  }
  finally {
    if (showRobotPopup.value)
      scheduleRobotStatusPolling()
  }
}

function resetLiveStatusDisplay() {
  isLiveRunning.value = false
  liveStartedAtText.value = ''
  liveRuntimeText.value = ''
  roomStatusText.value = '-'
  roomStatusRaw.value = ''
  currentViewerCountText.value = '-'
  deviceOnline.value = null
  deviceOfflineDurationSeconds.value = 0
  roomEndedDurationSeconds.value = 0
}

async function fetchLiveStatus() {
  if (!selectedRobot.value) {
    stopSentMsgTracking()
    consoleMessages.value = []
    resetLiveStatusDisplay()
    return
  }

  const deviceId = selectedRobot.value.mac

  try {
    const liveData: any = await getLiveStatus(deviceId)
    if (!selectedRobot.value || selectedRobot.value.mac !== deviceId)
      return

    isLiveRunning.value = liveData?.status === 'running'
    liveStartedAtText.value = liveData?.started_at ? formatAbsoluteTime(liveData.started_at) : ''
    liveRuntimeText.value = isLiveRunning.value ? formatRuntimeSeconds(Number(liveData?.runtime_seconds) || 0) : ''
    roomStatusRaw.value = liveData?.room_status || ''
    roomStatusText.value = liveData?.room_status === 'running'
      ? '直播中'
      : liveData?.room_status === 'ended'
        ? '直播间关闭'
        : '-'
    currentViewerCountText.value = liveData?.current_viewer_count !== undefined && liveData?.current_viewer_count !== null
      ? String(liveData.current_viewer_count)
      : '-'

    if (!benefitMembershipActive.value && liveData?.benefit_balance_seconds !== undefined) {
      const balanceSeconds = Number(liveData.benefit_balance_seconds) || 0
      benefitBalanceSeconds.value = balanceSeconds
      benefitLabel.value = '账户余额'
      benefitValue.value = `点卡 ${formatBenefitDuration(balanceSeconds)}`
      canStartLiveByBenefits.value = balanceSeconds > 0
    }
    else if (benefitMembershipActive.value && benefitMembershipEndAt.value) {
      if (liveData?.benefit_balance_seconds !== undefined)
        benefitBalanceSeconds.value = Number(liveData.benefit_balance_seconds) || 0
      benefitLabel.value = '账户余额'
      benefitValue.value = `月卡今日 ${formatBenefitDuration(benefitMembershipDailyRemainingSeconds.value)} / 点卡 ${formatBenefitDuration(benefitBalanceSeconds.value)}`
      canStartLiveByBenefits.value = true
    }

    deviceOnline.value = typeof liveData?.device_online === 'boolean' ? liveData.device_online : null
    deviceOfflineDurationSeconds.value = Number(liveData?.device_offline_duration_seconds) || 0
    roomEndedDurationSeconds.value = Number(liveData?.room_ended_duration_seconds) || 0

    const target = robots.value.find(item => item.id === selectedRobotId.value)
    if (target && deviceOnline.value !== null) {
      target.online = deviceOnline.value
      target.deviceStatus = deviceOnline.value ? 'online' : 'offline'
      sortRobots()
    }

    if (isLiveRunning.value)
      setRobotPlanSelectionByPlanNo(target?.id || selectedRobotId.value, liveData?.plan_no)

    if (isLiveRunning.value) {
      if (activeMonitorDeviceId.value !== deviceId) {
        startSentMsgPolling(deviceId)
      }
      else if (!sentMsgTimer.value) {
        void pollSentMessages()
      }
      scheduleLiveStatusPolling()
      return
    }

    if (activeMonitorDeviceId.value === deviceId)
      stopSentMsgTracking()

    setConsoleMessagesForDevice(deviceId)
    scheduleLiveStatusPolling()
  }
  catch {
    if (activeMonitorDeviceId.value === deviceId)
      stopSentMsgTracking()

    setConsoleMessagesForDevice(deviceId)
    resetLiveStatusDisplay()
    scheduleLiveStatusPolling()
  }
}

function selectRobot(id: string) {
  selectedRobotId.value = id
  ensureRobotPlanSelection(id)
  showRobotPopup.value = false
  flashCurrentRobotFocus()
  stopSentMsgTracking()
  setConsoleMessagesForDevice(selectedRobot.value?.mac || '')
  void refreshSelectedRobotState(id)
}

function flashCurrentRobotFocus() {
  if (currentRobotFocusTimer.value)
    clearTimeout(currentRobotFocusTimer.value)

  currentRobotFocus.value = false
  currentRobotFocusTimer.value = setTimeout(() => {
    currentRobotFocus.value = true
    currentRobotFocusTimer.value = setTimeout(() => {
      currentRobotFocus.value = false
      currentRobotFocusTimer.value = null
    }, 900)
  }, 0)
}

async function refreshSelectedRobotState(robotId: string) {
  await fetchLiveStatus()
  if (selectedRobotId.value === robotId)
    await fetchAutoStartPlan(robotId)
}

async function handleSendMessage() {
  if (!selectedRobot.value) {
    toast.warning('请先选择机器人')
    return
  }

  if (!pendingMessage.value)
    return

  try {
    await sendManualMsg(selectedRobot.value.mac, { message: pendingMessage.value })
    toast.success('消息已发送')
    pendingMessage.value = ''
  }
  catch (error: any) {
    toast.error(error?.message || '消息发送失败')
  }
}

async function handleStartStopLive() {
  if (!selectedRobot.value) {
    toast.warning('请先选择机器人')
    return
  }

  // if (!selectedRobot.value.online && !isLiveRunning.value) {
  //   toast.warning('机器人不在线')
  //   return
  // }

  const targetRobot = {
    id: selectedRobot.value.id,
    mac: selectedRobot.value.mac,
    online: selectedRobot.value.online,
  }
  const targetPlanNo = currentSelectedPlanNo.value
  const targetIsRunning = isLiveRunning.value

  if (!targetPlanNo) {
    toast.warning('请选择导播方案')
    return
  }

  if (!targetIsRunning && !canStartLiveByBenefits.value) {
    toast.warning('当前账户没有可用权益，无法启动导播')
    return
  }

  if (startLiveLoading.value)
    return

  try {
    startLiveLoading.value = true

    if (targetIsRunning) {
      await stopLive(targetRobot.mac)
      try {
        await setDeviceMicrophoneEnabled(targetRobot.id, true)
      }
      catch (error) {
        console.warn('开启设备麦克风失败:', error)
      }
      toast.success('导播已停止')
      if (selectedRobot.value?.mac === targetRobot.mac) {
        stopSentMsgTracking()
        stopLiveStatusPolling()
        resetLiveStatusDisplay()
        setConsoleMessagesForDevice(targetRobot.mac)
      }
      return
    }

    try {
      await sendDeviceCommand(targetRobot.id, {
        type: 'mcp',
        payload: {
          jsonrpc: '2.0',
          id: 1,
          method: 'tools/call',
          params: {
            name: 'self.enter_chat_state',
            arguments: {},
          },
        },
      })

      await sendDeviceCommand(targetRobot.id, {
        type: 'mcp',
        payload: {
          jsonrpc: '2.0',
          id: 1,
          method: 'tools/call',
          params: {
            name: 'self.notify_thalora_instance',
            arguments: {},
          },
        },
      })
    }
    catch {
      // ignore command failure, continue starting live
    }

    // 两秒等提示词模板加载
    await delay(2000)

    const plan: any = await getLivePlanDetail(targetPlanNo)
    await startLive({
      platform: plan?.platform,
      live_id: plan?.roomId,
      device_id: targetRobot.mac,
      plan_no: targetPlanNo,
      config_json: plan?.configJson,
    })
    try {
      await setDeviceMicrophoneEnabled(targetRobot.id, false)
    }
    catch (error) {
      console.warn('关闭设备麦克风失败:', error)
    }

    toast.success('导播已启动')
    selectedPlanNoMap.value = {
      ...selectedPlanNoMap.value,
      [targetRobot.id]: targetPlanNo,
    }
    if (selectedRobot.value?.mac === targetRobot.mac) {
      startSentMsgPolling(targetRobot.mac)
      await fetchLiveStatus()
    }
  }
  catch (error: any) {
    toast.error(error?.message || (targetIsRunning ? '停止导播失败' : '启动导播失败'))
  }
  finally {
    startLiveLoading.value = false
  }
}

async function handleToggleAutoStartPlan() {
  if (!selectedRobot.value) {
    toast.warning('请先选择机器人')
    return
  }

  const targetPlanNo = currentSelectedPlanNo.value
  if (!targetPlanNo) {
    toast.warning('请选择导播方案')
    return
  }

  if (autoStartPlanLoading.value)
    return

  try {
    autoStartPlanLoading.value = true
    const nextPlanNo = isCurrentPlanAutoStart.value ? '' : targetPlanNo
    await updateDeviceAutoStartPlan(selectedRobot.value.id, nextPlanNo)
    autoStartPlanNoMap.value = {
      ...autoStartPlanNoMap.value,
      [selectedRobot.value.id]: nextPlanNo,
    }
    toast.success(nextPlanNo ? '已开启开机自动导播' : '已关闭开机自动导播')
  }
  catch (error: any) {
    toast.error(error?.message || '设置开机自动导播失败')
  }
  finally {
    autoStartPlanLoading.value = false
  }
}

function handlePlanChange(event: any) {
  const index = Number(event?.detail?.value)
  const target = livePlanOptions.value[index]
  if (target)
    currentSelectedPlanNo.value = target.planNo
}

async function initializePage() {
  await Promise.all([
    fetchLivePlanData(),
    fetchUserBenefits(),
  ])
  await fetchRobotList()
}

defineExpose({
  refreshPlans: async () => {
    await fetchLivePlanData()
    ensureRobotPlanSelection(selectedRobotId.value)
    await fetchAutoStartPlan(selectedRobotId.value)
  },
  refreshBenefits: fetchUserBenefits,
  refresh: initializePage,
})

async function handleRefresh() {
  if (refreshing.value)
    return

  refreshing.value = true
  try {
    await initializePage()
  }
  catch (error) {
    console.error('刷新导播台失败:', error)
  }
  finally {
    refreshing.value = false
  }
}

onShow(() => {
  void initializePage()
})

onMounted(() => {
  void initializePage()
})

watch(showRobotPopup, (isOpen) => {
  if (isOpen) {
    void pollRobotOnlineStatuses()
  }
  else {
    stopRobotStatusPolling()
  }
})

onHide(() => {
  stopSentMsgPolling()
  stopLiveStatusPolling()
  stopRobotStatusPolling()
})

onUnload(() => {
  stopSentMsgPolling()
  stopLiveStatusPolling()
  stopRobotStatusPolling()
})

onUnmounted(() => {
  stopSentMsgPolling()
  stopLiveStatusPolling()
  stopRobotStatusPolling()
  if (currentRobotFocusTimer.value)
    clearTimeout(currentRobotFocusTimer.value)
})
</script>

<template>
  <view class="page">
    <scroll-view
      scroll-y
      class="page-scroll"
      enable-back-to-top
      :refresher-enabled="true"
      :refresher-triggered="refreshing"
      @refresherrefresh="handleRefresh"
    >
      <view class="page-body">
      <view class="current-robot-card" :class="{ 'current-robot-card--open': showRobotPopup, 'current-robot-card--focus': currentRobotFocus }" @click="showRobotPopup = !showRobotPopup">
        <view class="section-label">
          当前机器人 - 智能体
        </view>
        <template v-if="selectedRobot">
          <view class="current-robot-row">
            <view class="current-robot-info">
              <view class="current-robot-name">
                {{ selectedRobot.name }}
              </view>
              <!-- <view class="current-robot-mac">
                {{ selectedRobot.mac }}
              </view> -->
            </view>
            <view class="current-switch">
              {{ showRobotPopup ? '收起列表' : '点击切换机器人' }}
            </view>
          </view>

          <!-- <view class="meta-line">
            <view class="meta-tag">
              当前设备信息
            </view>
            <text class="meta-text">
              {{ selectedRobot.meta }}
            </text>
          </view> -->

          <view class="meta-line">
            <view class="meta-tag">
              {{ benefitLabel }}
            </view>
            <text class="meta-text">
              {{ benefitValue }}
            </text>
          </view>
        </template>
        <template v-else>
          <view class="empty-box">
            <text class="empty-text">
              暂无机器人
            </text>
            <view class="current-switch">
              {{ showRobotPopup ? '收起列表' : '查看列表' }}
            </view>
          </view>
        </template>

        <view v-if="showRobotPopup" class="robot-drawer" @click.stop>
          <view class="robot-drawer__header">
            <text class="robot-drawer__title">
              选择机器人
            </text>
            <text class="robot-drawer__count">
              {{ listLoading ? '加载中' : `${robots.length} 台` }}
            </text>
          </view>

          <scroll-view scroll-y class="robot-list-scroll">
            <view class="robot-list-inner">
              <template v-if="robots.length">
                <view
                  v-for="robot in robots"
                  :key="robot.id"
                  class="robot-card"
                  :class="{ 'robot-card--active': selectedRobotId === robot.id }"
                  @click="selectRobot(robot.id)"
                >
                  <view class="robot-card__header">
                    <view class="robot-card__id">
                      {{ robot.mac }}
                    </view>
                    <text class="robot-card__use">
                      {{ selectedRobotId === robot.id ? '当前使用' : '切换' }}
                    </text>
                  </view>

                  <view class="robot-card__name">
                    {{ robot.name }}
                  </view>

                  <view class="robot-card__footer">
                    <view class="robot-card__status" :class="{ 'robot-card__status--online': robot.online }">
                      {{ robot.online ? '在线' : '离线' }}
                    </view>
                    <view class="robot-card__time">
                      {{ robot.updatedAt }}
                    </view>
                  </view>
                </view>
              </template>
              <view v-else class="robot-empty">
                {{ listLoading ? '加载中...' : '暂无机器人' }}
              </view>
              <view class="robot-list-bottom-spacer" />
            </view>
          </scroll-view>
        </view>
      </view>

      <template v-if="selectedRobot">
        <view class="summary-card">
          <!-- <view class="summary-title">
            {{ selectedRobot.name }}
          </view>
          <view class="meta-line">
            <view class="meta-tag">
              当前设备信息
            </view>
            <text class="meta-text">
              {{ selectedRobot.meta }}
            </text>
          </view>

          <view class="meta-line">
            <view class="meta-tag">
              {{ benefitLabel }}
            </view>
            <text class="meta-text">
              {{ benefitValue }}
            </text>
          </view> -->

          <view class="plan-row">
            <picker mode="selector" :range="planPickerRange" :value="selectedPlanIndex < 0 ? 0 : selectedPlanIndex" @change="handlePlanChange">
              <view class="plan-selector">
                <text class="plan-selector__label">
                  选择导播方案
                </text>
                <text class="plan-selector__value">
                  {{ selectedPlanLabel }}
                </text>
              </view>
            </picker>

            <view class="start-btn" @click="handleStartStopLive">
              {{ startLiveLoading ? (isLiveRunning ? '停止中...' : '启动中...') : (isLiveRunning ? '停止导播' : '启动导播') }}
            </view>
          </view>

          <view class="auto-start-row">
            <view class="auto-start-text">
              <view class="auto-start-title">
                开机自动导播
              </view>
              <view class="auto-start-desc">
                {{ isCurrentPlanAutoStart ? '当前方案已设为开机自动启动' : '开启后设备上线时自动启动当前方案' }}
              </view>
            </view>
            <wd-switch
              :model-value="isCurrentPlanAutoStart"
              :disabled="autoStartPlanLoading"
              @change="handleToggleAutoStartPlan"
            />
          </view>

          <view v-if="liveStartedAtText || liveRuntimeText || roomStatusText !== '-'" class="status-row">
            <view v-if="liveStartedAtText" class="status-pill">
              开始时间：{{ liveStartedAtText }}
            </view>
            <view v-if="liveRuntimeText" class="status-pill">
              已运行：{{ liveRuntimeText }}
            </view>
            <!-- <view class="status-pill">
              直播间：{{ roomStatusText }}
            </view> -->
          </view>

          <view v-if="abnormalStatusList.length" class="abnormal-row">
            <view v-for="item in abnormalStatusList" :key="item.key" class="abnormal-item">
              {{ item.label }}，已持续{{ item.durationText }}
            </view>
          </view>

          <view class="microphone-actions">
            <view class="microphone-btn microphone-btn--on" @click="handleManualMicrophoneSwitch(true)">
              开启麦克风
            </view>
            <view class="microphone-btn microphone-btn--off" @click="handleManualMicrophoneSwitch(false)">
              关闭麦克风
            </view>
          </view>

          <view class="brightness-control">
            <view class="brightness-track">
              <view class="brightness-title">
                屏幕亮度
              </view>
              <slider
                class="brightness-slider"
                :value="screenBrightness"
                :min="0"
                :max="100"
                :step="1"
                active-color="#336cff"
                background-color="#d9e2ff"
                block-color="#336cff"
                :block-size="20"
                @changing="event => updateScreenBrightness(event.detail.value)"
                @change="event => updateScreenBrightness(event.detail.value)"
              />
              <view class="brightness-value">
                {{ screenBrightness }}%
              </view>
            </view>
            <view class="brightness-apply-btn" :class="{ 'brightness-apply-btn--loading': screenBrightnessLoading }" @click="handleSetScreenBrightness">
              {{ screenBrightnessLoading ? '...' : '应用' }}
            </view>
          </view>
        </view>

        <view class="message-card">
          <view class="message-header">
            <view class="message-title">
              消息面板
            </view>
            <view class="viewer-wrap">
              <text class="viewer-label">
                在线人数
              </text>
              <text class="viewer-value">
                {{ currentViewerCountText }}
              </text>
            </view>
          </view>

          <view class="message-toolbar">
            <input
              v-model.trim="pendingMessage"
              class="message-input"
              placeholder="输入消息，指挥机器人..."
              @confirm="handleSendMessage"
            >
            <view class="send-btn" @click="handleSendMessage">
              发送
            </view>
          </view>

          <scroll-view scroll-y class="console-panel" :scroll-into-view="consoleAnchorId">
            <template v-if="consoleMessages.length">
              <view
                v-for="message in consoleMessages"
                :id="`console-${message.id}`"
                :key="message.id"
                class="console-item"
              >
                <view class="console-item__text">
                  {{ message.text }}
                </view>
                <view class="console-item__time">
                  {{ message.time }}
                </view>
              </view>
            </template>
            <view v-else class="console-empty">
              暂无消息
            </view>
          </scroll-view>
        </view>
      </template>
    </view>

    </scroll-view>
  </view>
</template>

<style scoped lang="scss">
.page {
  height: 100%;
  background: #f5f7fb;
}

.page-scroll {
  height: 100%;
  background: #f5f7fb;
}

.page-body {
  padding: 24rpx 24rpx calc(40rpx + env(safe-area-inset-bottom));
}

.current-robot-card,
.summary-card,
.message-card {
  margin-bottom: 24rpx;
  border-radius: 28rpx;
  border: 2rpx solid transparent;
  background: #fff;
  box-shadow: 0 10rpx 30rpx rgba(48, 74, 138, 0.08);
  box-sizing: border-box;
}

.current-robot-card,
.summary-card,
.message-card {
  padding: 28rpx;
}

.current-robot-card {
  transition:
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.current-robot-card--open {
  box-shadow: 0 16rpx 42rpx rgba(48, 74, 138, 0.12);
}

.current-robot-card--focus {
  animation: current-card-focus 0.9s ease-out;
}

.section-label {
  margin-bottom: 12rpx;
  font-size: 20rpx;
  color: #999;
}

.current-robot-row,
.empty-box,
.message-header,
.message-toolbar,
.plan-row,
.benefit-line,
.meta-line,
.status-row,
.robot-card__header,
.robot-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}

.current-robot-name,
.summary-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1f2430;
}

.current-robot-mac {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #8d93a6;
}

.current-switch,
.robot-card__use {
  flex-shrink: 0;
  color: #5d90ea;
  font-size: 24rpx;
  font-weight: 600;
}

.empty-text,
.robot-empty,
.console-empty {
  color: #99a0b2;
  font-size: 24rpx;
}

.meta-line {
  margin-top: 18rpx;
  align-items: flex-start;
}

.meta-tag {
  min-width: 92rpx;
  height: 46rpx;
  padding: 0 18rpx;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 22rpx;
}

.meta-tag {
  border: 2rpx solid #cfe0ff;
  background: #f4f8ff;
  color: #5d90ea;
}

.meta-text {
  flex: 1;
  min-width: 0;
  font-size: 23rpx;
  line-height: 1.6;
  color: #4b5568;
  align-self: center;
}

.message-input,
.plan-selector {
  box-sizing: border-box;
  border: 2rpx solid #e5eaf3;
  border-radius: 16rpx;
  background: #fff;
}

.message-input {
  flex: 1;
  height: 68rpx;
  padding: 0 20rpx;
  font-size: 23rpx;
  color: #1f2430;
}

.send-btn,
.start-btn,
.brightness-apply-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 68rpx;
  border-radius: 16rpx;
  font-size: 24rpx;
  font-weight: 600;
  color: #fff;
  background: #5d90ea;
  flex-shrink: 0;
}

.auto-start-row {
  margin-top: 16rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  min-height: 76rpx;
  padding: 16rpx 20rpx;
  border-radius: 16rpx;
  border: 2rpx solid #d8e4ff;
  background: #f5f8ff;
}

.auto-start-text {
  min-width: 0;
  flex: 1;
}

.auto-start-title {
  color: #4f7fe8;
  font-size: 23rpx;
  font-weight: 600;
}

.auto-start-desc {
  margin-top: 6rpx;
  color: #8892a8;
  font-size: 20rpx;
  line-height: 1.45;
}

.send-btn {
  min-width: 120rpx;
  padding: 0 22rpx;
}

.plan-row {
  margin-top: 0;
  align-items: stretch;
}

.microphone-actions {
  display: flex;
  gap: 16rpx;
  margin-top: 22rpx;
}

.microphone-btn {
  flex: 1;
  height: 68rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 16rpx;
  font-size: 24rpx;
  font-weight: 600;
}

.microphone-btn--on {
  color: #2d8a55;
  background: rgba(61, 173, 100, 0.12);
}

.microphone-btn--off {
  color: #d34f4f;
  background: rgba(238, 92, 92, 0.12);
}

.brightness-control {
  margin-top: 18rpx;
  height: 68rpx;
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.brightness-track {
  flex: 1;
  min-width: 0;
  height: 68rpx;
  padding: 0 14rpx;
  border-radius: 16rpx;
  background: #f7f9ff;
  border: 2rpx solid #edf2ff;
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.brightness-title {
  flex: none;
  font-size: 24rpx;
  font-weight: 700;
  color: #273042;
}

.brightness-value {
  flex: none;
  min-width: 64rpx;
  text-align: right;
  font-size: 24rpx;
  font-weight: 700;
  color: #336cff;
}

.brightness-slider {
  flex: 1;
  min-width: 0;
}

.brightness-apply-btn {
  min-width: 88rpx;
  padding: 0 18rpx;
}

.brightness-apply-btn--loading {
  opacity: 0.65;
}

.plan-row picker {
  flex: 1;
}

.plan-selector {
  min-height: 96rpx;
  padding: 18rpx 20rpx;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8rpx;
}

.plan-selector__label {
  font-size: 20rpx;
  color: #9aa2b3;
}

.plan-selector__value {
  font-size: 24rpx;
  color: #273042;
  line-height: 1.5;
}

.start-btn {
  min-width: 176rpx;
  padding: 0 24rpx;
  height: auto;
  min-height: 96rpx;
}

.status-row {
  margin-top: 20rpx;
  flex-wrap: wrap;
  justify-content: flex-start;
}

.status-pill {
  padding: 10rpx 16rpx;
  border-radius: 999rpx;
  background: #f3f7ff;
  color: #5b6780;
  font-size: 21rpx;
}

.abnormal-row {
  margin-top: 18rpx;
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.abnormal-item {
  padding: 16rpx 18rpx;
  border-radius: 18rpx;
  background: #fff5f5;
  color: #d35b5b;
  font-size: 22rpx;
  line-height: 1.5;
}

.message-header {
  margin-bottom: 18rpx;
}

.message-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1f2430;
}

.viewer-wrap {
  display: flex;
  align-items: baseline;
  gap: 10rpx;
  color: #6b7386;
}

.viewer-label {
  font-size: 20rpx;
}

.viewer-value {
  font-size: 24rpx;
  font-weight: 600;
  color: #202430;
}

.console-panel {
  max-height: 860rpx;
  padding-top: 20rpx;
}

.console-item {
  margin-bottom: 16rpx;
  padding: 20rpx;
  border-radius: 20rpx;
  border: 2rpx solid #edf1f7;
  background: #fff;
}

.console-item__text {
  font-size: 23rpx;
  line-height: 1.7;
  color: #31394d;
  text-align: left;
}

.console-item__time {
  margin-top: 12rpx;
  font-size: 20rpx;
  color: #98a0b1;
  text-align: left;
}

.robot-drawer {
  margin-top: 24rpx;
  padding-top: 22rpx;
  border-top: 2rpx solid #eef2f8;
  animation: robot-drawer-in 0.18s ease-out;
}

.robot-drawer__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18rpx;
}

.robot-drawer__title {
  font-size: 26rpx;
  font-weight: 700;
  color: #1f2430;
}

.robot-drawer__count {
  font-size: 22rpx;
  color: #98a0b1;
}

.robot-list-scroll {
  max-height: 620rpx;
  border-radius: 22rpx;
  background: #f7f9fc;
}

.robot-list-inner {
  padding: 16rpx;
  box-sizing: border-box;
}

.robot-card {
  margin-bottom: 18rpx;
  padding: 22rpx 24rpx;
  border-radius: 24rpx;
  background: #fff;
  border: 2rpx solid #edf1f7;
  box-shadow: 0 8rpx 20rpx rgba(48, 74, 138, 0.06);
}

.robot-card--active {
  border-color: #d6e2ff;
  background: #f7faff;
}

.robot-card__id {
  font-size: 26rpx;
  line-height: 1.4;
  font-weight: 700;
  color: #202430;
}

.robot-card__name {
  margin-top: 14rpx;
  font-size: 24rpx;
  color: #7f8798;
}

.robot-card__footer {
  margin-top: 18rpx;
  padding-top: 16rpx;
  border-top: 2rpx solid #f0f2f7;
}

.robot-card__status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 72rpx;
  height: 38rpx;
  padding: 0 14rpx;
  border-radius: 999rpx;
  border: 2rpx solid #dfe4ef;
  color: #98a0b1;
  background: #f7f9fc;
  font-size: 20rpx;
}

.robot-card__status--online {
  border-color: #b7e28e;
  color: #75bc4a;
  background: #f8fff1;
}

.robot-card__time {
  font-size: 22rpx;
  color: #b0b6c3;
}

.robot-list-bottom-spacer {
  height: 2rpx;
}

@keyframes robot-drawer-in {
  from {
    opacity: 0;
    transform: translateY(-12rpx);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes current-card-focus {
  0% {
    border-color: #bcd0ff;
    box-shadow: 0 0 0 0 rgba(93, 144, 234, 0.22), 0 10rpx 30rpx rgba(48, 74, 138, 0.08);
    transform: translateY(0);
  }

  35% {
    border-color: #9ab8ff;
    box-shadow: 0 0 0 8rpx rgba(93, 144, 234, 0.12), 0 18rpx 42rpx rgba(48, 74, 138, 0.14);
    transform: translateY(-2rpx);
  }

  100% {
    border-color: transparent;
    box-shadow: 0 10rpx 30rpx rgba(48, 74, 138, 0.08);
    transform: translateY(0);
  }
}
</style>
