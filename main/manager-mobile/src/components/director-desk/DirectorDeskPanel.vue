<script lang="ts" setup>
import type { Agent } from '@/api/agent/types'
import type { Device } from '@/api/device/types'
import { onHide, onShow, onUnload } from '@dcloudio/uni-app'
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { getMyBenefits, redeemActivationCode } from '@/api/activation-code/activation-code'
import { getAgentList } from '@/api/agent/agent'
import { getBindDevices, getDeviceStatus, sendDeviceCommand } from '@/api/device'
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
  rawBindTime: number
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

const livePlanOptions = ref<LivePlanOption[]>([])
const selectedPlanNo = ref('')

const pendingMessage = ref('')
const consoleMessages = ref<ConsoleMessage[]>([])
const consoleMessageMap = ref<Record<string, ConsoleMessage[]>>({})
const sentMsgAfterIdMap = ref<Record<string, number>>({})

const benefitLabel = ref('有效期')
const benefitValue = ref('-')
const canStartLiveByBenefits = ref(false)
const benefitMembershipActive = ref(false)
const benefitMembershipEndAt = ref('')

const redeemCode = ref('')
const redeemLoading = ref(false)
const startLiveLoading = ref(false)
const listLoading = ref(false)

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
const activeMonitorDeviceId = ref('')
const sentMsgAfterId = ref(0)
const sentMsgLimit = 20

const selectedRobot = computed(() => {
  return robots.value.find(item => item.id === selectedRobotId.value) || null
})

const selectedPlanLabel = computed(() => {
  const target = livePlanOptions.value.find(item => item.planNo === selectedPlanNo.value)
  return target ? target.displayLabel : '请选择导播方案'
})

const selectedPlanIndex = computed(() => {
  return livePlanOptions.value.findIndex(item => item.planNo === selectedPlanNo.value)
})

function delay(ms: number) {
  return new Promise(resolve => setTimeout(resolve, ms))
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

function formatDisplayTime(value?: string) {
  if (!value)
    return '刚刚'

  const date = new Date(value)
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

  const date = new Date(value)
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

  const date = new Date(value)
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

async function fetchUserBenefits() {
  try {
    const benefit: any = await getMyBenefits()
    benefitMembershipActive.value = benefit?.membershipActive === true
    benefitMembershipEndAt.value = benefit?.membershipEndAt || ''

    if (benefitMembershipActive.value && benefitMembershipEndAt.value) {
      benefitLabel.value = '有效期'
      benefitValue.value = formatAbsoluteTime(benefitMembershipEndAt.value)
      canStartLiveByBenefits.value = true
      return
    }

    const balanceSeconds = Number(benefit?.balanceSeconds) || 0
    benefitLabel.value = '剩余可用时长'
    benefitValue.value = formatBenefitDuration(balanceSeconds)
    canStartLiveByBenefits.value = balanceSeconds > 0
  }
  catch {
    benefitMembershipActive.value = false
    benefitMembershipEndAt.value = ''
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
    if (!selectedPlanNo.value && list.length)
      selectedPlanNo.value = list[0].planNo
  }
  catch (error: any) {
    toast.error(error?.message || '获取导播方案失败')
  }
}

async function fetchBindDevicesWithStatus(agent: Agent) {
  const devices = await getBindDevices(agent.id)
  const mappedList: RobotItem[] = (devices || []).map((device: Device) => ({
    id: device.id,
    agentId: agent.id,
    mac: device.macAddress,
    model: device.board,
    agentName: agent.agentName,
    name: `${device.board || 'Unknown'} - ${agent.agentName || '未命名智能体'}`,
    meta: `${device.macAddress || '-'} | ${device.board || '-'} | ${device.appVersion || '-'}`,
    updatedAt: formatDisplayTime(device.lastConnectedAt || device.createDate),
    online: false,
    deviceStatus: 'offline',
    rawBindTime: new Date(device.createDate || 0).getTime() || 0,
  }))

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
    const nextRobots = robotGroups.flat().sort((a, b) => a.rawBindTime - b.rawBindTime)
    robots.value = nextRobots

    if (!nextRobots.length) {
      selectedRobotId.value = ''
      resetLiveStatusDisplay()
      return
    }

    const stillExists = nextRobots.some(item => item.id === selectedRobotId.value)
    selectedRobotId.value = stillExists ? selectedRobotId.value : nextRobots[0].id
    await fetchLiveStatus()
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

  consoleMessageMap.value[deviceId] = deviceMessages
  if (selectedRobot.value?.mac === deviceId)
    consoleMessages.value = deviceMessages.slice()

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

function scheduleLiveStatusPolling() {
  stopLiveStatusPolling()
  liveStatusTimer.value = setTimeout(() => {
    void fetchLiveStatus()
  }, 5000)
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
      benefitLabel.value = '剩余可用时长'
      benefitValue.value = formatBenefitDuration(balanceSeconds)
      canStartLiveByBenefits.value = balanceSeconds > 0
    }
    else if (benefitMembershipActive.value && benefitMembershipEndAt.value) {
      benefitLabel.value = '有效期'
      benefitValue.value = formatAbsoluteTime(benefitMembershipEndAt.value)
      canStartLiveByBenefits.value = true
    }

    deviceOnline.value = typeof liveData?.device_online === 'boolean' ? liveData.device_online : null
    deviceOfflineDurationSeconds.value = Number(liveData?.device_offline_duration_seconds) || 0
    roomEndedDurationSeconds.value = Number(liveData?.room_ended_duration_seconds) || 0

    const target = robots.value.find(item => item.id === selectedRobotId.value)
    if (target && deviceOnline.value !== null) {
      target.online = deviceOnline.value
      target.deviceStatus = deviceOnline.value ? 'online' : 'offline'
    }

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
    stopLiveStatusPolling()
  }
  catch {
    if (activeMonitorDeviceId.value === deviceId)
      stopSentMsgTracking()

    setConsoleMessagesForDevice(deviceId)
    resetLiveStatusDisplay()
    stopLiveStatusPolling()
  }
}

function selectRobot(id: string) {
  selectedRobotId.value = id
  showRobotPopup.value = false
  stopSentMsgTracking()
  setConsoleMessagesForDevice(selectedRobot.value?.mac || '')
  void fetchLiveStatus()
}

async function handleRedeem() {
  if (!redeemCode.value) {
    toast.warning('请输入激活码')
    return
  }

  if (redeemLoading.value)
    return

  try {
    redeemLoading.value = true
    await redeemActivationCode(redeemCode.value)
    redeemCode.value = ''
    toast.success('兑换成功')
    await fetchUserBenefits()
  }
  catch (error: any) {
    toast.error(error?.message || '兑换失败')
  }
  finally {
    redeemLoading.value = false
  }
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

  if (!selectedRobot.value.online && !isLiveRunning.value) {
    toast.warning('机器人不在线')
    return
  }

  if (!selectedPlanNo.value) {
    toast.warning('请选择导播方案')
    return
  }

  if (!isLiveRunning.value && !canStartLiveByBenefits.value) {
    toast.warning('当前账户没有可用权益，无法启动导播')
    return
  }

  if (startLiveLoading.value)
    return

  try {
    startLiveLoading.value = true

    if (isLiveRunning.value) {
      await stopLive(selectedRobot.value.mac)
      toast.success('导播已停止')
      stopSentMsgTracking()
      stopLiveStatusPolling()
      resetLiveStatusDisplay()
      setConsoleMessagesForDevice(selectedRobot.value.mac)
      return
    }

    try {
      await sendDeviceCommand(selectedRobot.value.id, {
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
    }
    catch {
      // ignore command failure, continue starting live
    }

    await delay(2000)

    const plan: any = await getLivePlanDetail(selectedPlanNo.value)
    await startLive({
      platform: plan?.platform,
      live_id: plan?.roomId,
      device_id: selectedRobot.value.mac,
      config_json: plan?.configJson,
    })

    toast.success('导播已启动')
    startSentMsgPolling(selectedRobot.value.mac)
    await fetchLiveStatus()
  }
  catch (error: any) {
    toast.error(error?.message || (isLiveRunning.value ? '停止导播失败' : '启动导播失败'))
  }
  finally {
    startLiveLoading.value = false
  }
}

function handlePlanChange(event: any) {
  const index = Number(event?.detail?.value)
  const target = livePlanOptions.value[index]
  if (target)
    selectedPlanNo.value = target.planNo
}

async function initializePage() {
  await Promise.all([
    fetchLivePlanData(),
    fetchUserBenefits(),
    fetchRobotList(),
  ])
}

onShow(() => {
  void initializePage()
})

onMounted(() => {
  void initializePage()
})

onHide(() => {
  stopSentMsgPolling()
  stopLiveStatusPolling()
})

onUnload(() => {
  stopSentMsgPolling()
  stopLiveStatusPolling()
})

onUnmounted(() => {
  stopSentMsgPolling()
  stopLiveStatusPolling()
})
</script>

<template>
  <view class="page">
    <view class="page-body">
      <view class="current-robot-card" @click="showRobotPopup = true">
        <view class="section-label">
          当前机器人
        </view>
        <template v-if="selectedRobot">
          <view class="current-robot-row">
            <view class="current-robot-info">
              <view class="current-robot-name">
                {{ selectedRobot.name }}
              </view>
              <view class="current-robot-mac">
                {{ selectedRobot.mac }}
              </view>
            </view>
            <view class="current-switch">
              切换
            </view>
          </view>
        </template>
        <template v-else>
          <view class="empty-box">
            <text class="empty-text">
              暂无机器人
            </text>
            <view class="current-switch" @click="showRobotPopup = true">
              查看列表
            </view>
          </view>
        </template>
      </view>

      <template v-if="selectedRobot">
        <view class="summary-card">
          <view class="summary-title">
            {{ selectedRobot.name }}
          </view>

          <view class="meta-line">
            <view class="meta-tag">
              设备信息
            </view>
            <text class="meta-text">
              {{ selectedRobot.meta }}
            </text>
          </view>

          <view class="benefit-card">
            <view class="benefit-line">
              <view class="benefit-tag">
                {{ benefitLabel }}
              </view>
              <text class="benefit-value">
                {{ benefitValue }}
              </text>
            </view>

            <view class="redeem-row">
              <input
                v-model.trim="redeemCode"
                class="redeem-input"
                placeholder="输入激活码后兑换"
                @confirm="handleRedeem"
              >
              <view class="redeem-btn" @click="handleRedeem">
                {{ redeemLoading ? '兑换中...' : '兑换' }}
              </view>
            </view>
          </view>

          <view class="plan-row">
            <picker mode="selector" :range="planPickerRange" :value="selectedPlanIndex < 0 ? 0 : selectedPlanIndex" @change="handlePlanChange">
              <view class="plan-selector">
                <text class="plan-selector__label">
                  导播方案
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

    <view v-if="showRobotPopup" class="robot-popup-mask" @click="showRobotPopup = false">
      <view class="robot-popup" @click.stop>
        <view class="robot-popup__header">
          <text class="robot-popup__title">
            选择机器人
          </text>
          <text class="robot-popup__close" @click="showRobotPopup = false">
            ×
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
  </view>
</template>

<style scoped lang="scss">
.page {
  min-height: 100vh;
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
  background: #fff;
  box-shadow: 0 10rpx 30rpx rgba(48, 74, 138, 0.08);
}

.current-robot-card,
.summary-card,
.message-card {
  padding: 28rpx;
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

.meta-tag,
.benefit-tag {
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

.benefit-card {
  margin-top: 22rpx;
  padding: 22rpx;
  border-radius: 22rpx;
  background: #f8fbff;
}

.benefit-tag {
  border: 2rpx solid #b8e2c3;
  background: #f3fbf3;
  color: #55a36a;
}

.meta-text,
.benefit-value {
  flex: 1;
  min-width: 0;
  font-size: 23rpx;
  line-height: 1.6;
  color: #4b5568;
}

.redeem-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 18rpx;
}

.redeem-input,
.message-input,
.plan-selector {
  box-sizing: border-box;
  border: 2rpx solid #e5eaf3;
  border-radius: 16rpx;
  background: #fff;
}

.redeem-input,
.message-input {
  flex: 1;
  height: 68rpx;
  padding: 0 20rpx;
  font-size: 23rpx;
  color: #1f2430;
}

.redeem-btn,
.send-btn,
.start-btn {
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

.redeem-btn,
.send-btn {
  min-width: 120rpx;
  padding: 0 22rpx;
}

.plan-row {
  margin-top: 22rpx;
  align-items: stretch;
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

.robot-popup-mask {
  position: fixed;
  inset: 0;
  z-index: 99;
  background: rgba(17, 24, 39, 0.42);
  display: flex;
  align-items: flex-end;
}

.robot-popup {
  width: 100%;
  max-height: 68vh;
  border-radius: 32rpx 32rpx 0 0;
  background: #fff;
  overflow: hidden;
  margin-bottom: calc(110rpx + env(safe-area-inset-bottom));
}

.robot-popup__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx;
  border-bottom: 2rpx solid #eef1f6;
}

.robot-popup__title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1f2430;
}

.robot-popup__close {
  font-size: 44rpx;
  color: #c2c7d4;
  line-height: 1;
}

.robot-list-scroll {
  max-height: calc(68vh - 96rpx);
}

.robot-list-inner {
  padding: 24rpx 24rpx calc(140rpx + env(safe-area-inset-bottom));
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
  height: calc(60rpx + env(safe-area-inset-bottom));
}
</style>
