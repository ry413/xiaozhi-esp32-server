<script lang="ts" setup>
import { onShow } from '@dcloudio/uni-app'
import { computed, onMounted, ref } from 'vue'
import { updatePlanConfig } from '@/api/live-streaming/live-streaming'
import { addLivePlan, deleteLivePlan, getDouyinRoomId, getLivePlanList, updateLivePlan } from '@/api/live-plan/live-plan'
import { toast } from '@/utils/toast'

defineOptions({
  name: 'MaihuoConfigMobile',
})

interface KeywordReply {
  keyword: string
  reply: string
}

interface TextTemplatePanel {
  latestCount: number
  templates: string[]
}

interface SchemePanels {
  timed: {
    enabled: boolean
    interval: number
    sequential: boolean
    templates: string[]
  }
  awkward: {
    enabled: boolean
    interval: number
    sequential: boolean
    interruptEnabled: boolean
    templates: string[]
  }
  manual: {
    latestCount: number
    fixedTemplate: string
  }
  basic: {
    ignoreNumericName: boolean
    ignoreMaskedName: boolean
  }
  welcome: TextTemplatePanel
  danmu: {
    latestCount: number
    fixedTemplate: string
    keywordReplies: KeywordReply[]
    blockedKeywords: string[]
  }
  like: TextTemplatePanel
  follow: TextTemplatePanel
  gift: TextTemplatePanel
}

interface SchemeItem {
  id: string
  planNo: string
  roomId: string
  platform: string
  name: string
  status: string
  updatedAt: string
  panels: SchemePanels
}

const tabs = [
  { label: '定时强制', name: 'timed' },
  { label: '防冷场', name: 'awkward' },
  { label: '手动指挥', name: 'manual' },
  { label: '基础设置', name: 'basic' },
  { label: '进入欢迎', name: 'welcome' },
  { label: '弹幕互动', name: 'danmu' },
  { label: '点赞反馈', name: 'like' },
  { label: '关注反馈', name: 'follow' },
  { label: '礼物反馈', name: 'gift' },
]

const activePlatform = ref('抖音')
const newSchemeRoomId = ref('')
const schemes = ref<SchemeItem[]>([])
const selectedSchemeId = ref('')
const activeTab = ref('timed')
const listLoading = ref(false)
const addPlanLoading = ref(false)
const savePlanLoading = ref(false)
const showSchemeActions = ref(false)
const showEditPopup = ref(false)
const editingField = ref<'name' | 'roomId' | ''>('')
const editingValue = ref('')

const selectedScheme = computed(() => {
  return schemes.value.find(item => item.id === selectedSchemeId.value) || null
})

function createFeedbackPanel(latestCount: number, templates: string[]): TextTemplatePanel {
  return {
    latestCount,
    templates,
  }
}

function createDanmuPanel() {
  return {
    latestCount: 5,
    fixedTemplate: '【弹幕】{name} 说：{text}',
    keywordReplies: [
      { keyword: '合作', reply: '{name} 想合作，你让他私信主播联系' },
      { keyword: '价格', reply: '{name} 问价格，你可以告诉他今天的优惠' },
    ],
    blockedKeywords: ['垃圾', '100遍', '音量', '退款', '无聊'],
  }
}

function createDefaultPanels(): SchemePanels {
  return {
    timed: {
      enabled: false,
      interval: 180,
      sequential: false,
      templates: [
        '三二一上一下链接，制造一下库存的紧张感',
        '说3个让用户非买不可的理由',
        '今天直播间有哪些优惠给大家介绍下',
      ],
    },
    awkward: {
      enabled: false,
      interval: 120,
      sequential: false,
      interruptEnabled: false,
      templates: [
        '现在冷场了，从你上次冷场聊的话题从找一个点接着聊。',
        '随便强调一下直播间福利和限时活动',
        '问观众还有什么问题或者想看的内容，调动一下氛围',
      ],
    },
    manual: {
      latestCount: 10,
      fixedTemplate: '【Admin】{text}',
    },
    basic: {
      ignoreNumericName: true,
      ignoreMaskedName: true,
    },
    welcome: createFeedbackPanel(3, ['【进场】{name} 进入直播间']),
    danmu: createDanmuPanel(),
    like: createFeedbackPanel(3, ['【点赞】{name} 给你点了 {count} 个赞']),
    follow: createFeedbackPanel(3, ['【关注】{name} 关注了你']),
    gift: createFeedbackPanel(3, ['【送礼】{name} 给你送了 {count} 个 {giftName}']),
  }
}

function parsePanelNumber(value: any, fallback: number) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : fallback
}

function normalizePanels(rawPanels: Partial<SchemePanels> = {}): SchemePanels {
  const defaults = createDefaultPanels()
  const merged: SchemePanels = JSON.parse(JSON.stringify(defaults))

  if (rawPanels.timed) {
    merged.timed.enabled = !!rawPanels.timed.enabled
    merged.timed.interval = parsePanelNumber(rawPanels.timed.interval, defaults.timed.interval)
    merged.timed.sequential = !!rawPanels.timed.sequential
    merged.timed.templates = Array.isArray(rawPanels.timed.templates)
      ? rawPanels.timed.templates.map(item => String(item))
      : defaults.timed.templates
  }

  if (rawPanels.awkward) {
    merged.awkward.enabled = !!rawPanels.awkward.enabled
    merged.awkward.interval = parsePanelNumber(rawPanels.awkward.interval, defaults.awkward.interval)
    merged.awkward.sequential = !!rawPanels.awkward.sequential
    merged.awkward.interruptEnabled = !!rawPanels.awkward.interruptEnabled
    merged.awkward.templates = Array.isArray(rawPanels.awkward.templates)
      ? rawPanels.awkward.templates.map(item => String(item))
      : defaults.awkward.templates
  }

  if (rawPanels.manual) {
    merged.manual.latestCount = parsePanelNumber(rawPanels.manual.latestCount, defaults.manual.latestCount)
    merged.manual.fixedTemplate = typeof rawPanels.manual.fixedTemplate === 'string'
      ? rawPanels.manual.fixedTemplate
      : defaults.manual.fixedTemplate
  }

  if (rawPanels.basic) {
    merged.basic.ignoreNumericName = !!rawPanels.basic.ignoreNumericName
    merged.basic.ignoreMaskedName = !!rawPanels.basic.ignoreMaskedName
  }

  ;['welcome', 'like', 'follow', 'gift'].forEach((key) => {
    const panel = rawPanels[key as keyof SchemePanels] as TextTemplatePanel | undefined
    const defaultPanel = defaults[key as keyof SchemePanels] as TextTemplatePanel
    if (!panel) {
      return
    }
    ;(merged[key as keyof SchemePanels] as TextTemplatePanel).latestCount = parsePanelNumber(panel.latestCount, defaultPanel.latestCount)
    ;(merged[key as keyof SchemePanels] as TextTemplatePanel).templates = Array.isArray(panel.templates)
      ? panel.templates.map(item => String(item))
      : defaultPanel.templates
  })

  if (rawPanels.danmu) {
    merged.danmu.latestCount = parsePanelNumber(rawPanels.danmu.latestCount, defaults.danmu.latestCount)
    merged.danmu.fixedTemplate = typeof rawPanels.danmu.fixedTemplate === 'string'
      ? rawPanels.danmu.fixedTemplate
      : defaults.danmu.fixedTemplate
    merged.danmu.keywordReplies = Array.isArray(rawPanels.danmu.keywordReplies)
      ? rawPanels.danmu.keywordReplies.map(item => ({
          keyword: item?.keyword ? String(item.keyword) : '',
          reply: item?.reply ? String(item.reply) : '',
        }))
      : defaults.danmu.keywordReplies
    merged.danmu.blockedKeywords = Array.isArray(rawPanels.danmu.blockedKeywords)
      ? rawPanels.danmu.blockedKeywords.map(item => String(item))
      : defaults.danmu.blockedKeywords
  }

  return merged
}

function formatRelativeTime(value?: string) {
  if (!value)
    return '刚刚'

  const date = new Date(value)
  if (Number.isNaN(date.getTime()))
    return '刚刚'

  const diffMs = Date.now() - date.getTime()
  const diffMinutes = Math.floor(diffMs / 60000)
  if (diffMinutes < 1)
    return '刚刚'
  if (diffMinutes < 60)
    return `${diffMinutes}分钟前`

  const diffHours = Math.floor(diffMinutes / 60)
  if (diffHours < 24)
    return `${diffHours}小时前`

  const diffDays = Math.floor(diffHours / 24)
  return `${diffDays}天前`
}

function mapLivePlanToScheme(plan: any): SchemeItem {
  return {
    id: plan.planNo || plan.id,
    planNo: plan.planNo || '',
    roomId: plan.roomId || '',
    platform: plan.platform || '抖音',
    name: plan.planName || '',
    status: Number(plan.status) === 1 ? '使用中' : '空闲',
    updatedAt: formatRelativeTime(plan.updateDate || plan.createDate),
    panels: normalizePanels(plan.configJson),
  }
}

function buildLivePlanPayload(roomId: string) {
  return {
    planName: `${activePlatform.value}${roomId}方案`,
    platform: activePlatform.value,
    roomId,
    configJson: createDefaultPanels(),
    status: 0,
    remark: '',
  }
}

function buildUpdatePayload(scheme: SchemeItem) {
  return {
    planName: scheme.name,
    platform: scheme.platform,
    roomId: scheme.roomId,
    configJson: JSON.parse(JSON.stringify(scheme.panels)),
    status: scheme.status === '使用中' ? 1 : 0,
    remark: '',
  }
}

async function applyRunningPlanConfig(scheme: SchemeItem) {
  return await updatePlanConfig({
    plan_no: scheme.planNo,
    live_id: scheme.roomId,
    platform: scheme.platform,
    config_json: JSON.parse(JSON.stringify(scheme.panels)),
  }) as any
}

async function fetchLivePlanData() {
  try {
    listLoading.value = true
    const response = await getLivePlanList({ page: 1, limit: 100 })
    const list = ((response || {}).list || []).map((item: any) => mapLivePlanToScheme(item))
    schemes.value = list
    if (!list.length) {
      selectedSchemeId.value = ''
      return
    }

    const stillExists = list.some(item => item.id === selectedSchemeId.value)
    selectedSchemeId.value = stillExists ? selectedSchemeId.value : list[0].id
  }
  catch (error: any) {
    toast.error(error?.message || '获取方案列表失败')
  }
  finally {
    listLoading.value = false
  }
}

async function resolveRoomId(input: string) {
  const trimmedInput = (input || '').trim()
  if (!trimmedInput)
    return ''
  if (/^\d{6,}$/.test(trimmedInput))
    return trimmedInput

  const directMatch = trimmedInput.match(/live\.douyin\.com\/(\d+)/)
  if (directMatch)
    return directMatch[1]

  return await getDouyinRoomId(trimmedInput)
}

async function handleAddScheme() {
  const rawInput = newSchemeRoomId.value.trim()
  if (!rawInput) {
    toast.warning('请输入直播间 ID 或分享链接')
    return
  }

  try {
    addPlanLoading.value = true
    const roomId = await resolveRoomId(rawInput)
    if (!roomId) {
      toast.warning('未能解析出直播间 ID')
      return
    }

    const planNo = await addLivePlan(buildLivePlanPayload(roomId))
    selectedSchemeId.value = String(planNo || '')
    activeTab.value = 'timed'
    newSchemeRoomId.value = ''
    toast.success('方案创建成功')
    await fetchLivePlanData()
  }
  catch (error: any) {
    toast.error(error?.message || '方案创建失败')
  }
  finally {
    addPlanLoading.value = false
  }
}

function selectScheme(id: string) {
  selectedSchemeId.value = id
  showSchemeActions.value = false
}

async function duplicateScheme(id: string) {
  const target = schemes.value.find(item => item.id === id)
  if (!target || addPlanLoading.value)
    return

  try {
    addPlanLoading.value = true
    const planNo = await addLivePlan({
      planName: `${target.name} - 复制`,
      platform: target.platform,
      roomId: target.roomId,
      configJson: JSON.parse(JSON.stringify(target.panels)),
      status: target.status === '使用中' ? 1 : 0,
      remark: '',
    })
    selectedSchemeId.value = String(planNo || '')
    toast.success('方案复制成功')
    await fetchLivePlanData()
  }
  catch (error: any) {
    toast.error(error?.message || '方案复制失败')
  }
  finally {
    addPlanLoading.value = false
  }
}

async function removeScheme(id: string) {
  const target = schemes.value.find(item => item.id === id)
  console.log('删除方案', id, target)
  if (!target?.planNo) {
    toast.warning('方案不存在，无法删除')
    return
  }

  const result = await uni.showModal({
    title: '删除确认',
    content: '确定删除这个方案吗？删除后不可恢复。',
    confirmText: '删除',
    cancelText: '取消',
    confirmColor: '#e95b5b',
  })

  if (!result.confirm)
    return

  try {
    await deleteLivePlan(target.planNo)
    if (selectedSchemeId.value === id)
      selectedSchemeId.value = ''
    toast.success('方案删除成功')
    await fetchLivePlanData()
  }
  catch (error: any) {
    toast.error(error?.message || '方案删除失败')
  }
}

function editField(field: 'name' | 'roomId') {
  if (!selectedScheme.value)
    return
  editingField.value = field
  editingValue.value = field === 'name' ? selectedScheme.value.name : selectedScheme.value.roomId
  showEditPopup.value = true
}

function cancelEditField() {
  showEditPopup.value = false
  editingField.value = ''
  editingValue.value = ''
}

function confirmEditField() {
  const nextValue = editingValue.value.trim()
  if (!nextValue || !selectedScheme.value || !editingField.value)
    return

  if (editingField.value === 'name')
    selectedScheme.value.name = nextValue
  else
    selectedScheme.value.roomId = nextValue

  cancelEditField()
}

function updateNumberField(target: any, key: string, value: string, min = 0) {
  const digitsOnly = String(value).replace(/\D/g, '')
  if (!digitsOnly) {
    target[key] = min
    return
  }
  target[key] = Math.max(min, Number(digitsOnly))
}

function stepNumberField(target: any, key: string, delta: number, min = 0) {
  const currentValue = Number(target[key]) || min
  target[key] = Math.max(min, currentValue + delta)
}

function addTextListItem(list: string[], value: string) {
  list.push(value)
}

function removeListItem(list: string[], index: number) {
  if (list.length === 1) {
    list.splice(index, 1, '')
    return
  }
  list.splice(index, 1)
}

function getTemplatePlaceholder(tabName: string) {
  const map: Record<string, string> = {
    welcome: '新增欢迎话术',
    like: '新增点赞反馈话术',
    follow: '新增关注反馈话术',
    gift: '新增礼物反馈话术',
  }
  return map[tabName] || '新增模板'
}

function addKeywordReply() {
  if (!selectedScheme.value)
    return
  selectedScheme.value.panels.danmu.keywordReplies.push({
    keyword: '',
    reply: '',
  })
}

function removeKeywordReply(index: number) {
  if (!selectedScheme.value)
    return
  const list = selectedScheme.value.panels.danmu.keywordReplies
  if (list.length === 1) {
    list.splice(index, 1, { keyword: '', reply: '' })
    return
  }
  list.splice(index, 1)
}

function resetScheme() {
  if (!selectedScheme.value)
    return
  selectedScheme.value.panels = createDefaultPanels()
  activeTab.value = 'timed'
}

async function handleSaveScheme() {
  if (!selectedScheme.value?.planNo) {
    toast.warning('请先选择方案')
    return
  }

  try {
    savePlanLoading.value = true
    const scheme = selectedScheme.value
    await updateLivePlan(scheme.planNo, buildUpdatePayload(scheme))

    try {
      const applyResult: any = await applyRunningPlanConfig(scheme)
      const updatedCount = Number(applyResult?.updated_count) || 0
      const matchedCount = Number(applyResult?.matched_count) || 0
      if (matchedCount > 0)
        toast.success(`方案保存成功，已同步到 ${updatedCount}/${matchedCount} 个运行中实例`)
      else
        toast.success('方案保存成功')
    }
    catch (error: any) {
      toast.warning(`方案已保存，但运行中实例同步失败：${error?.message || '请稍后重试'}`)
    }

    await fetchLivePlanData()
  }
  catch (error: any) {
    toast.error(error?.message || '方案保存失败')
  }
  finally {
    savePlanLoading.value = false
  }
}

function handleSmartScript() {
  toast.info('智能话术移动版后续接入')
}

function readClipboardText() {
  return new Promise<string>((resolve, reject) => {
    uni.getClipboardData({
      success: res => resolve(String(res.data || '')),
      fail: reject,
    })
  })
}

function writeClipboardText(text: string) {
  return new Promise<void>((resolve, reject) => {
    uni.setClipboardData({
      data: text,
      success: () => resolve(),
      fail: reject,
    })
  })
}

async function handleExportConfig() {
  if (!selectedScheme.value) {
    toast.warning('请先选择方案')
    return
  }

  try {
    await writeClipboardText(JSON.stringify(selectedScheme.value.panels, null, 2))
    toast.success('已导出到剪切板')
  }
  catch {
    toast.error('导出失败')
  }
}

async function handleImportConfig() {
  if (!selectedScheme.value) {
    toast.warning('请先选择方案')
    return
  }

  try {
    const clipboardText = await readClipboardText()
    selectedScheme.value.panels = normalizePanels(JSON.parse(clipboardText))
    toast.success('已从剪切板导入')
  }
  catch {
    toast.error('导入失败，请确认剪切板内容为合法 JSON')
  }
}

onShow(() => {
  fetchLivePlanData()
})

onMounted(() => {
  fetchLivePlanData()
})
</script>

<template>
  <view class="page">
    <view class="page-body">
      <view class="current-scheme-card" @click="showSchemeActions = true">
        <view class="current-label">
          当前方案
        </view>
        <template v-if="selectedScheme">
          <view class="current-row">
            <view class="current-info">
              <view class="current-name">
                {{ selectedScheme.name }}
              </view>
              <view class="current-meta-row">
                <view class="current-room">
                  直播间id {{ selectedScheme.roomId }}
                </view>
                <view class="current-platform">
                  {{ selectedScheme.platform }}
                </view>
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
              暂无方案
            </text>
            <view class="current-switch" @click="showSchemeActions = true">
              添加
            </view>
          </view>
        </template>
      </view>

      <template v-if="selectedScheme">
        <view class="summary-card">
          <view class="summary-actions-grid">
            <view class="action-btn action-btn--plain" @click="editField('name')">
              编辑方案名称
            </view>
            <view class="action-btn action-btn--plain" @click="editField('roomId')">
              编辑直播间id
            </view>
            <view class="action-btn action-btn--plain" @click="handleExportConfig">
              导出方案
            </view>
            <view class="action-btn action-btn--plain" @click="handleImportConfig">
              导入方案
            </view>
          </view>
        </view>

        <!-- <view class="priority-card">
            <text class="priority-icon">💡</text>
            <text class="priority-text">
              机器人处理优先级： 强制插播 ＞ 刷礼物 ＞ 弹幕 ＞ 关注 ＞ 欢迎 ＞ 防冷场
            </text>
          </view> -->

        <view class="panel-card">
          <scroll-view scroll-x class="tabs-scroll" enable-flex>
            <view class="tabs-row">
              <view
                v-for="tab in tabs"
                :key="tab.name"
                class="tab-chip"
                :class="{ 'tab-chip--active': activeTab === tab.name }"
                @click="activeTab = tab.name"
              >
                {{ tab.label }}
              </view>
            </view>
          </scroll-view>

          <view class="panel-body">
            <template v-if="activeTab === 'timed'">
              <view class="form-row">
                <text class="field-label">
                  启用定时强制
                </text>
                <wd-switch v-model="selectedScheme.panels.timed.enabled" />
              </view>
              <view class="form-row">
                <text class="field-label">
                  间隔时长（秒）
                </text>
                <view class="stepper">
                  <view class="step-btn" @click="stepNumberField(selectedScheme.panels.timed, 'interval', -10, 1)">
                    -
                  </view>
                  <input
                    :value="String(selectedScheme.panels.timed.interval)"
                    class="step-input"
                    type="number"
                    @input="updateNumberField(selectedScheme.panels.timed, 'interval', $event.detail.value, 1)"
                  >
                  <view class="step-btn" @click="stepNumberField(selectedScheme.panels.timed, 'interval', 10, 1)">
                    +
                  </view>
                </view>
              </view>
              <view class="form-row">
                <text class="field-label">
                  按顺序循环发送
                </text>
                <wd-switch v-model="selectedScheme.panels.timed.sequential" />
              </view>
              <view class="template-block">
                <text class="block-title">
                  随机模板
                </text>
                <view v-for="(item, index) in selectedScheme.panels.timed.templates" :key="`timed-${index}`" class="template-item">
                  <input v-model="selectedScheme.panels.timed.templates[index]" class="template-input" maxlength="200">
                  <view class="remove-btn" @click="removeListItem(selectedScheme.panels.timed.templates, index)">
                    ×
                  </view>
                </view>
                <view class="add-template-btn" @click="addTextListItem(selectedScheme.panels.timed.templates, '新增模板')">
                  + 添加模板
                </view>
              </view>
            </template>

            <template v-else-if="activeTab === 'awkward'">
              <view class="form-row">
                <text class="field-label">
                  启用防冷场
                </text>
                <wd-switch v-model="selectedScheme.panels.awkward.enabled" />
              </view>
              <view class="form-row">
                <text class="field-label">
                  间隔时长（秒）
                </text>
                <view class="stepper">
                  <view class="step-btn" @click="stepNumberField(selectedScheme.panels.awkward, 'interval', -10, 1)">
                    -
                  </view>
                  <input
                    :value="String(selectedScheme.panels.awkward.interval)"
                    class="step-input"
                    type="number"
                    @input="updateNumberField(selectedScheme.panels.awkward, 'interval', $event.detail.value, 1)"
                  >
                  <view class="step-btn" @click="stepNumberField(selectedScheme.panels.awkward, 'interval', 10, 1)">
                    +
                  </view>
                </view>
              </view>
              <view class="form-row">
                <text class="field-label">
                  按顺序循环发送
                </text>
                <wd-switch v-model="selectedScheme.panels.awkward.sequential" />
              </view>
              <view class="form-row">
                <text class="field-label">
                  防冷场可被打断
                </text>
                <wd-switch v-model="selectedScheme.panels.awkward.interruptEnabled" />
              </view>
              <view class="template-block">
                <text class="block-title">
                  随机模板
                </text>
                <view v-for="(item, index) in selectedScheme.panels.awkward.templates" :key="`awkward-${index}`" class="template-item">
                  <input v-model="selectedScheme.panels.awkward.templates[index]" class="template-input" maxlength="200">
                  <view class="remove-btn" @click="removeListItem(selectedScheme.panels.awkward.templates, index)">
                    ×
                  </view>
                </view>
                <view class="add-template-btn" @click="addTextListItem(selectedScheme.panels.awkward.templates, '新增模板')">
                  + 添加模板
                </view>
              </view>
            </template>

            <template v-else-if="activeTab === 'manual'">
              <view class="form-row">
                <text class="field-label">
                  只处理最新的几条消息
                </text>
                <view class="stepper">
                  <view class="step-btn" @click="stepNumberField(selectedScheme.panels.manual, 'latestCount', -1, 1)">
                    -
                  </view>
                  <input
                    :value="String(selectedScheme.panels.manual.latestCount)"
                    class="step-input"
                    type="number"
                    @input="updateNumberField(selectedScheme.panels.manual, 'latestCount', $event.detail.value, 1)"
                  >
                  <view class="step-btn" @click="stepNumberField(selectedScheme.panels.manual, 'latestCount', 1, 1)">
                    +
                  </view>
                </view>
              </view>
              <view class="template-block">
                <text class="block-title">
                  固定模板
                </text>
                <input
                  v-model="selectedScheme.panels.manual.fixedTemplate"
                  class="template-input template-input--full"
                  maxlength="400"
                >
              </view>
            </template>

            <template v-else-if="activeTab === 'basic'">
              <view class="form-row">
                <text class="field-label">
                  忽略纯数字用户名
                </text>
                <wd-switch v-model="selectedScheme.panels.basic.ignoreNumericName" />
              </view>
              <view class="form-row">
                <text class="field-label">
                  忽略包含多个*的用户名
                </text>
                <wd-switch v-model="selectedScheme.panels.basic.ignoreMaskedName" />
              </view>
            </template>

            <template v-else-if="['welcome', 'like', 'follow', 'gift'].includes(activeTab)">
              <view class="form-row">
                <text class="field-label">
                  只处理最新的几条消息
                </text>
                <view class="stepper">
                  <view class="step-btn" @click="stepNumberField((selectedScheme.panels as any)[activeTab], 'latestCount', -1, 1)">
                    -
                  </view>
                  <input
                    :value="String((selectedScheme.panels as any)[activeTab].latestCount)"
                    class="step-input"
                    type="number"
                    @input="updateNumberField((selectedScheme.panels as any)[activeTab], 'latestCount', $event.detail.value, 1)"
                  >
                  <view class="step-btn" @click="stepNumberField((selectedScheme.panels as any)[activeTab], 'latestCount', 1, 1)">
                    +
                  </view>
                </view>
              </view>
              <view class="template-block">
                <text class="block-title">
                  随机模板
                </text>
                <view v-for="(item, index) in (selectedScheme.panels as any)[activeTab].templates" :key="`${activeTab}-${index}`" class="template-item">
                  <input v-model="(selectedScheme.panels as any)[activeTab].templates[index]" class="template-input" maxlength="200">
                  <view class="remove-btn" @click="removeListItem((selectedScheme.panels as any)[activeTab].templates, index)">
                    ×
                  </view>
                </view>
                <view class="add-template-btn" @click="addTextListItem((selectedScheme.panels as any)[activeTab].templates, getTemplatePlaceholder(activeTab))">
                  + 添加模板
                </view>
              </view>
            </template>

            <template v-else-if="activeTab === 'danmu'">
              <view class="form-row">
                <text class="field-label">
                  只处理最新的几条消息
                </text>
                <view class="stepper">
                  <view class="step-btn" @click="stepNumberField(selectedScheme.panels.danmu, 'latestCount', -1, 1)">
                    -
                  </view>
                  <input
                    :value="String(selectedScheme.panels.danmu.latestCount)"
                    class="step-input"
                    type="number"
                    @input="updateNumberField(selectedScheme.panels.danmu, 'latestCount', $event.detail.value, 1)"
                  >
                  <view class="step-btn" @click="stepNumberField(selectedScheme.panels.danmu, 'latestCount', 1, 1)">
                    +
                  </view>
                </view>
              </view>
              <view class="template-block">
                <text class="block-title">
                  固定模板
                </text>
                <input
                  v-model="selectedScheme.panels.danmu.fixedTemplate"
                  class="template-input template-input--full"
                  maxlength="400"
                >
              </view>
              <view class="template-block">
                <text class="block-title">
                  关键词回复
                </text>
                <view v-for="(item, index) in selectedScheme.panels.danmu.keywordReplies" :key="`reply-${index}`" class="keyword-item">
                  <input v-model="item.keyword" class="keyword-input" placeholder="关键词" maxlength="40">
                  <input v-model="item.reply" class="template-input keyword-reply-input" placeholder="回复内容" maxlength="160">
                  <view class="remove-btn" @click="removeKeywordReply(index)">
                    ×
                  </view>
                </view>
                <view class="add-template-btn" @click="addKeywordReply">
                  + 添加关键词回复
                </view>
              </view>
              <view class="template-block">
                <text class="block-title">
                  屏蔽词跳过
                </text>
                <view v-for="(item, index) in selectedScheme.panels.danmu.blockedKeywords" :key="`blocked-${index}`" class="template-item">
                  <input v-model="selectedScheme.panels.danmu.blockedKeywords[index]" class="template-input" maxlength="60">
                  <view class="remove-btn" @click="removeListItem(selectedScheme.panels.danmu.blockedKeywords, index)">
                    ×
                  </view>
                </view>
                <view class="add-template-btn" @click="addTextListItem(selectedScheme.panels.danmu.blockedKeywords, '新增屏蔽词')">
                  + 添加屏蔽词
                </view>
              </view>
            </template>
          </view>

          <view class="footer-actions">
            <view class="ghost-btn" @click="resetScheme">
              重置
            </view>
            <view class="primary-btn" @click="handleSaveScheme">
              {{ savePlanLoading ? '保存中...' : '保存方案' }}
            </view>
          </view>
        </view>
      </template>
    </view>

    <view v-if="showSchemeActions" class="scheme-popup-mask" @click="showSchemeActions = false">
      <view class="scheme-popup" @click.stop>
        <view class="scheme-popup__header">
          <text class="scheme-popup__title">
            选择方案
          </text>
          <text class="scheme-popup__close" @click="showSchemeActions = false">
            ×
          </text>
        </view>

        <view class="scheme-popup__content">
          <input
            v-model.trim="newSchemeRoomId"
            class="scheme-popup__input"
            placeholder="输入直播间 ID 或分享链接 后点添加"
          >

          <view class="platform-row">
            <view class="platform-chip platform-chip--active">
              抖音
            </view>
          </view>

          <view
            class="scheme-popup__add-btn"
            :class="{ 'scheme-popup__add-btn--loading': addPlanLoading }"
            @click="handleAddScheme"
          >
            {{ addPlanLoading ? '添加中...' : '添加方案' }}
          </view>

          <scroll-view scroll-y class="scheme-list-scroll">
            <view class="scheme-list-inner">
              <view
                v-for="scheme in schemes"
                :key="scheme.id"
                class="scheme-card"
                :class="{ 'scheme-card--active': selectedSchemeId === scheme.id }"
                @click="selectScheme(scheme.id)"
              >
                <view class="scheme-card__header">
                  <view class="scheme-card__id">
                    {{ scheme.roomId }}
                  </view>
                  <view class="scheme-card__icon-actions">
                    <text class="scheme-card__icon-action" @click.stop="duplicateScheme(scheme.id)">
                      复制
                    </text>
                    <text class="scheme-card__icon-action scheme-card__icon-action--danger" @click.stop="removeScheme(scheme.id)">
                      删除
                    </text>
                  </view>
                </view>

                <view class="scheme-card__name">
                  {{ scheme.name }}
                </view>

                <view class="scheme-card__footer">
                  <view class="scheme-card__meta">
                    <text class="scheme-card__status-tag">
                      {{ scheme.status }}
                    </text>
                    <text class="scheme-card__time">
                      {{ scheme.updatedAt }}
                    </text>
                  </view>
                  <view class="scheme-card__footer-actions">
                    <text
                      v-if="selectedSchemeId === scheme.id"
                      class="scheme-card__action scheme-card__action--active"
                    >
                      当前使用
                    </text>
                    <text
                      v-else
                      class="scheme-card__action scheme-card__action--active"
                      @click.stop="selectScheme(scheme.id)"
                    >
                      切换
                    </text>
                  </view>
                </view>
              </view>

              <view class="scheme-list-bottom-spacer" />
            </view>
          </scroll-view>
        </view>
      </view>
    </view>

    <wd-popup
      v-model="showEditPopup"
      position="center"
      custom-style="width: 88%; max-width: 640rpx; border-radius: 24rpx;"
      safe-area-inset-bottom
      @close="cancelEditField"
    >
      <view class="edit-popup">
        <view class="edit-popup__title">
          {{ editingField === 'name' ? '修改方案名' : '修改直播间 ID' }}
        </view>
        <input
          v-model.trim="editingValue"
          class="edit-popup__input"
          :placeholder="editingField === 'name' ? '请输入方案名' : '请输入直播间 ID'"
          :focus="showEditPopup"
          @confirm="confirmEditField"
        >
        <view class="edit-popup__actions">
          <view class="edit-popup__btn edit-popup__btn--ghost" @click="cancelEditField">
            取消
          </view>
          <view class="edit-popup__btn edit-popup__btn--primary" @click="confirmEditField">
            确定
          </view>
        </view>
      </view>
    </wd-popup>
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

.current-scheme-card,
.summary-card,
.panel-card {
  margin-bottom: 24rpx;
  border-radius: 28rpx;
  background: #fff;
  box-shadow: 0 10rpx 30rpx rgba(48, 74, 138, 0.08);
}

.current-scheme-card {
  padding: 28rpx;
}

.current-label {
  margin-bottom: 12rpx;
  font-size: 20rpx;
  color: #999;
}

.current-row,
.empty-box {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20rpx;
}

.current-info {
  min-width: 0;
  flex: 1;
}

.current-name {
  font-size: 30rpx;
  font-weight: 700;
  color: #1f2430;
}

.current-room {
  font-size: 24rpx;
  color: #8d93a6;
}

.current-meta-row {
  display: flex;
  align-items: center;
  gap: 14rpx;
  margin-top: 10rpx;
  flex-wrap: wrap;
}

.current-platform {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 78rpx;
  height: 42rpx;
  padding: 0 16rpx;
  border-radius: 999rpx;
  border: 2rpx solid #cfe0ff;
  background: #f4f8ff;
  color: #5d90ea;
  font-size: 22rpx;
}

.current-switch {
  flex-shrink: 0;
  color: #5d90ea;
  font-size: 24rpx;
  font-weight: 600;
  line-height: 1.8;
}

.empty-text {
  color: #999;
  font-size: 28rpx;
}

.summary-card {
  padding: 28rpx;
}

.summary-actions-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
}

.action-btn {
  width: 100%;
  min-width: 0;
  height: 72rpx;
  padding: 0 18rpx;
  border-radius: 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  font-weight: 600;
  box-sizing: border-box;
}

.action-btn--soft {
  background: #f3fbf3;
  color: #55a36a;
  border: 2rpx solid #b8e2c3;
}

.action-btn--plain {
  background: #fff;
  color: #485168;
  border: 2rpx solid #e3e8f4;
}

.priority-card {
  display: flex;
  gap: 16rpx;
  margin-bottom: 24rpx;
  padding: 22rpx 24rpx;
  border-radius: 20rpx;
  border: 2rpx solid rgba(244, 194, 80, 0.35);
  background: #fffaf0;
}

.priority-icon {
  font-size: 34rpx;
}

.priority-text {
  color: #9d7420;
  font-size: 22rpx;
  line-height: 1.7;
}

.panel-card {
  padding: 12rpx 0 28rpx;
}

.tabs-scroll {
  white-space: nowrap;
  width: 100%;
  border-bottom: 2rpx solid #f0f1f5;
}

.tabs-row {
  display: inline-flex;
  min-width: 100%;
  padding: 0 24rpx;
}

.tab-chip {
  position: relative;
  padding: 20rpx 14rpx 16rpx;
  margin-right: 18rpx;
  font-size: 24rpx;
  color: #474c59;
  flex-shrink: 0;
}

.tab-chip--active {
  color: #5d90ea;
  font-weight: 700;
}

.tab-chip--active::after {
  content: '';
  position: absolute;
  left: 10rpx;
  right: 10rpx;
  bottom: -2rpx;
  height: 6rpx;
  border-radius: 999rpx;
  background: #5d90ea;
}

.panel-body {
  padding: 30rpx 24rpx 0;
}

.form-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 30rpx;
}

.field-label,
.block-title {
  color: #303749;
}

.field-label {
  font-size: 24rpx;
}

.block-title {
  display: block;
  margin-bottom: 18rpx;
  font-size: 24rpx;
  font-weight: 600;
}

.stepper {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  border: 2rpx solid #eceef4;
  border-radius: 12rpx;
  overflow: hidden;
  background: #fff;
}

.step-btn {
  width: 64rpx;
  height: 58rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30rpx;
  color: #40485a;
  background: #f7f8fb;
}

.step-input {
  width: 108rpx;
  height: 58rpx;
  text-align: center;
  font-size: 24rpx;
  color: #202430;
  background: #fff;
}

.template-block {
  margin-bottom: 34rpx;
}

.template-item,
.keyword-item {
  display: flex;
  align-items: center;
  gap: 14rpx;
  margin-bottom: 16rpx;
}

.template-input,
.keyword-input,
.long-textarea {
  box-sizing: border-box;
  border: 2rpx solid #e8ebf2;
  border-radius: 14rpx;
  background: #fff;
  color: #202430;
}

.template-input,
.keyword-input {
  height: 66rpx;
  padding: 0 22rpx;
  font-size: 23rpx;
}

.template-input {
  flex: 1;
}

.template-input--full {
  width: 100%;
  flex: none;
}

.keyword-input {
  width: 160rpx;
  flex-shrink: 0;
}

.keyword-reply-input {
  flex: 1;
}

.long-textarea {
  width: 100%;
  min-height: 140rpx;
  padding: 22rpx;
  font-size: 23rpx;
  line-height: 1.6;
}

.remove-btn {
  width: 50rpx;
  height: 50rpx;
  border-radius: 50%;
  background: #eef1f6;
  color: #f16a67;
  font-size: 30rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.add-template-btn,
.ghost-btn,
.primary-btn {
  display: flex;
  align-items: center;
  justify-content: center;
}

.add-template-btn {
  width: 184rpx;
  height: 62rpx;
  border: 2rpx solid #dfe6f4;
  border-radius: 16rpx;
  color: #2b2f3a;
  background: #fff;
  font-size: 22rpx;
}

.footer-actions {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 18rpx 24rpx 0;
  border-top: 2rpx solid #f3f4f7;
}

.ghost-btn,
.primary-btn {
  height: 68rpx;
  border-radius: 18rpx;
  font-size: 24rpx;
  font-weight: 600;
}

.ghost-btn {
  flex: 1;
  border: 2rpx solid #e2e4eb;
  color: #333;
  background: #fff;
}

.primary-btn {
  flex: 1.6;
  background: #5d90ea;
  color: #fff;
}

.scheme-popup-mask {
  position: fixed;
  inset: 0;
  z-index: 99;
  background: rgba(17, 24, 39, 0.42);
  display: flex;
  align-items: flex-end;
}

.scheme-popup {
  width: 100%;
  max-height: 68vh;
  border-radius: 32rpx 32rpx 0 0;
  background: #fff;
  overflow: hidden;
  margin-bottom: calc(110rpx + env(safe-area-inset-bottom));
}

.scheme-popup__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx;
  border-bottom: 2rpx solid #eef1f6;
}

.scheme-popup__title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1f2430;
}

.scheme-popup__close {
  font-size: 44rpx;
  color: #c2c7d4;
  line-height: 1;
}

.scheme-popup__content {
  padding: 24rpx 24rpx calc(36rpx + env(safe-area-inset-bottom));
}

.scheme-popup__input {
  height: 70rpx;
  padding: 0 20rpx;
  border: 2rpx solid #dfe5f0;
  border-radius: 14rpx;
  font-size: 23rpx;
  color: #1f2430;
  background: #fff;
}

.platform-row {
  display: flex;
  margin-top: 16rpx;
}

.platform-chip {
  min-width: 96rpx;
  height: 50rpx;
  padding: 0 20rpx;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22rpx;
  color: #6c7384;
  background: #f4f7fc;
}

.platform-chip--active {
  color: #4c9a64;
  border: 2rpx solid #87d39d;
  background: #eff9f2;
}

.scheme-popup__add-btn {
  margin-top: 18rpx;
  height: 70rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #5d90ea;
  color: #fff;
  font-size: 24rpx;
  font-weight: 600;
}

.scheme-popup__add-btn--loading {
  opacity: 0.72;
}

.scheme-list-scroll {
  max-height: calc(68vh - 260rpx);
  margin-top: 20rpx;
}

.scheme-list-inner {
  padding-bottom: calc(140rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.scheme-list-bottom-spacer {
  height: calc(60rpx + env(safe-area-inset-bottom));
}

.scheme-card {
  margin-bottom: 18rpx;
  padding: 22rpx 24rpx;
  border-radius: 24rpx;
  background: #fff;
  border: 2rpx solid #edf1f7;
  box-shadow: 0 8rpx 20rpx rgba(48, 74, 138, 0.06);
}

.scheme-card--active {
  border-color: #d6e2ff;
  background: #f7faff;
}

.scheme-card__header,
.scheme-card__footer,
.scheme-card__meta,
.scheme-card__footer-actions,
.scheme-card__icon-actions {
  display: flex;
  align-items: center;
}

.scheme-card__header,
.scheme-card__footer {
  justify-content: space-between;
}

.scheme-card__id {
  font-size: 28rpx;
  line-height: 1.3;
  font-weight: 700;
  color: #202430;
}

.scheme-card__icon-actions {
  gap: 18rpx;
  flex-shrink: 0;
}

.scheme-card__icon-action {
  color: #5d7cff;
  font-size: 28rpx;
  line-height: 1;
}

.scheme-card__icon-action--danger {
  color: #f16a67;
}

.scheme-card__name {
  margin-top: 16rpx;
  font-size: 24rpx;
  color: #7f8798;
}

.scheme-card__footer {
  margin-top: 18rpx;
  padding-top: 16rpx;
  border-top: 2rpx solid #f0f2f7;
}

.scheme-card__meta {
  gap: 16rpx;
}

.scheme-card__status-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 72rpx;
  height: 38rpx;
  padding: 0 14rpx;
  border-radius: 999rpx;
  border: 2rpx solid #b7e28e;
  color: #75bc4a;
  background: #f8fff1;
  font-size: 20rpx;
}

.scheme-card__time {
  font-size: 22rpx;
  color: #b0b6c3;
}

.scheme-card__footer-actions {
  gap: 16rpx;
  flex-shrink: 0;
}

.scheme-card__action {
  font-size: 22rpx;
  color: #5d90ea;
}

.scheme-card__action--active {
  color: #5d90ea;
  font-weight: 600;
}

.scheme-card__action--danger {
  color: #e95b5b;
}

.edit-popup {
  padding: 32rpx;
}

.edit-popup__title {
  margin-bottom: 24rpx;
  text-align: center;
  font-size: 32rpx;
  font-weight: 700;
  color: #232338;
}

.edit-popup__input {
  height: 74rpx;
  padding: 0 20rpx;
  border: 2rpx solid #dfe5f0;
  border-radius: 16rpx;
  background: #fff;
  font-size: 24rpx;
  color: #1f2430;
}

.edit-popup__actions {
  display: flex;
  gap: 16rpx;
  margin-top: 28rpx;
}

.edit-popup__btn {
  flex: 1;
  height: 72rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26rpx;
  font-weight: 600;
}

.edit-popup__btn--ghost {
  border: 2rpx solid #dfe5f0;
  background: #fff;
  color: #5b6578;
}

.edit-popup__btn--primary {
  background: #5d90ea;
  color: #fff;
}
</style>
