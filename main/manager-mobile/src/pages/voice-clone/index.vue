<route lang="jsonc" type="page">
{
  "layout": "tabbar",
  "style": {
    "navigationStyle": "custom",
    "navigationBarTitleText": "音色克隆"
  }
}
</route>

<script lang="ts" setup>
import type { VoiceCloneItem } from '@/api/voice-clone'
import { computed, ref } from 'vue'
import { onHide, onPullDownRefresh, onShow, onUnload } from '@dcloudio/uni-app'
import {
  cloneVoiceAudio,
  getVoiceCloneAudioId,
  getVoiceCloneList,
  getVoiceClonePlayUrl,
  updateVoiceCloneName,
  uploadVoiceCloneSample,
} from '@/api/voice-clone'
import { useAppShare } from '@/hooks/useAppShare'
import { isGuestMode, promptLogin, redirectToLoginIfAuthExpired } from '@/utils/auth'
import { toast } from '@/utils/toast'

defineOptions({
  name: 'VoiceCloneMobile',
})

useAppShare({
  title: '音色克隆 - 小助助播',
})

const loading = ref(false)
const refreshing = ref(false)
const searchName = ref('')
const voiceCloneList = ref<VoiceCloneItem[]>([])
const refreshTimer = ref<ReturnType<typeof setInterval> | null>(null)
const uploadingId = ref('')
const cloningId = ref('')
const playingId = ref('')
const recordingId = ref('')
const recordingStartAt = ref(0)
const recordingTarget = ref<VoiceCloneItem | null>(null)
const recordDiscarding = ref(false)
const recordPopupVisible = ref(false)
const recordPermissionChecking = ref(false)
const renamePopupVisible = ref(false)
const renameSubmitting = ref(false)
const currentRenameItem = ref<VoiceCloneItem | null>(null)
const renameValue = ref('')
const audioContext = ref<UniApp.InnerAudioContext | null>(null)
const recorderManager = uni.getRecorderManager()
const isGuest = ref(isGuestMode())

function syncLoginState() {
  isGuest.value = isGuestMode()
  return isGuest.value
}

const filteredVoiceCloneList = computed(() => {
  const keyword = searchName.value.trim()
  if (!keyword) {
    return voiceCloneList.value
  }
  return voiceCloneList.value.filter(item =>
    String(item.name || '').includes(keyword)
    || String(item.voiceId || '').includes(keyword)
    || String(item.modelName || '').includes(keyword),
  )
})

function getTrainStatusText(item: VoiceCloneItem) {
  if (!item.hasVoice) {
    return '待上传'
  }
  switch (item.trainStatus) {
    case 0:
      return '待复刻'
    case 1:
      return '复刻中'
    case 2:
      return '已成功'
    case 3:
      return '已失败'
    default:
      return '-'
  }
}

function getTrainStatusType(item: VoiceCloneItem) {
  if (!item.hasVoice || item.trainStatus === 0) {
    return 'warning'
  }
  if (item.trainStatus === 1) {
    return 'primary'
  }
  if (item.trainStatus === 2) {
    return 'success'
  }
  if (item.trainStatus === 3) {
    return 'danger'
  }
  return 'info'
}

async function fetchVoiceClones(showLoading = true) {
  if (syncLoginState()) {
    voiceCloneList.value = []
    loading.value = false
    refreshing.value = false
    return
  }

  try {
    if (showLoading) {
      loading.value = true
    }
    const data = await getVoiceCloneList({
      page: 1,
      limit: 100,
      name: searchName.value.trim(),
    })
    voiceCloneList.value = data?.list || []
  }
  catch (error: any) {
    toast.error(error?.message || '获取音色列表失败')
  }
  finally {
    loading.value = false
    refreshing.value = false
  }
}

function startAutoRefresh() {
  if (syncLoginState()) {
    stopAutoRefresh()
    return
  }

  stopAutoRefresh()
  refreshTimer.value = setInterval(() => {
    if (!loading.value && !uploadingId.value && !cloningId.value && !recordingId.value) {
      void fetchVoiceClones(false)
    }
  }, 5000)
}

function stopAutoRefresh() {
  if (refreshTimer.value) {
    clearInterval(refreshTimer.value)
    refreshTimer.value = null
  }
}

function openRenamePopup(item: VoiceCloneItem) {
  if (!promptLogin('登录后可管理音色资源')) {
    return
  }

  currentRenameItem.value = item
  renameValue.value = item.name || ''
  renamePopupVisible.value = true
}

async function submitRename() {
  if (!promptLogin('登录后可管理音色资源')) {
    return
  }

  const target = currentRenameItem.value
  const nextName = renameValue.value.trim()
  if (!target) {
    return
  }
  if (!nextName) {
    toast.warning('请输入名称')
    return
  }

  try {
    renameSubmitting.value = true
    await updateVoiceCloneName(target.id, nextName)
    toast.success('名称更新成功')
    renamePopupVisible.value = false
    await fetchVoiceClones(false)
  }
  catch (error: any) {
    toast.error(error?.message || '名称更新失败')
  }
  finally {
    renameSubmitting.value = false
  }
}

function chooseAudioFile() {
  return new Promise<any>((resolve, reject) => {
    // #ifdef MP-WEIXIN
    const chooseMessageFile = (uni as any).chooseMessageFile || (typeof wx !== 'undefined' ? wx.chooseMessageFile : null)
    if (!chooseMessageFile) {
      reject(new Error('当前微信版本不支持选择文件'))
      return
    }

    chooseMessageFile({
      count: 1,
      type: 'file',
      extension: ['mp3', 'wav'],
      success: resolve,
      fail: reject,
    })
    // #endif

    // #ifndef MP-WEIXIN
    uni.chooseFile({
      count: 1,
      type: 'all',
      extension: ['mp3', 'wav'],
      success: resolve,
      fail: reject,
    })
    // #endif
  })
}

async function chooseAndUpload(item: VoiceCloneItem) {
  if (!promptLogin('登录后可上传音色样本')) {
    return
  }

  try {
    const chooseResult = await chooseAudioFile()

    const tempFile = chooseResult.tempFiles?.[0]
    const filePath = tempFile?.path || chooseResult.tempFilePaths?.[0]
    const fileName = tempFile?.name || ''
    const fileSize = Number(tempFile?.size || 0)

    if (!filePath) {
      toast.warning('未选择音频文件')
      return
    }
    if (fileSize > 10 * 1024 * 1024) {
      toast.warning('音频文件不能超过10MB')
      return
    }

    uploadingId.value = item.id
    await uploadVoiceCloneSample({
      id: item.id,
      filePath,
      fileName,
    })
    toast.success('音频上传成功')
    await fetchVoiceClones(false)
  }
  catch (error: any) {
    if (error?.errMsg?.includes('cancel')) {
      return
    }
    toast.error(error?.message || '上传音频失败')
  }
  finally {
    uploadingId.value = ''
  }
}

async function uploadRecordedAudio(item: VoiceCloneItem, filePath: string) {
  uploadingId.value = item.id
  await uploadVoiceCloneSample({
    id: item.id,
    filePath,
    fileName: `record-${Date.now()}.mp3`,
  })
  toast.success('录音上传成功')
  await fetchVoiceClones(false)
}

function resetRecordingState() {
  recordingId.value = ''
  recordingStartAt.value = 0
  recordingTarget.value = null
  recordDiscarding.value = false
}

function ensureRecordPermission() {
  return new Promise<void>((resolve, reject) => {
    // #ifdef MP-WEIXIN
    uni.authorize({
      scope: 'scope.record',
      success: () => resolve(),
      fail: (error) => reject(error),
    })
    // #endif

    // #ifndef MP-WEIXIN
    resolve()
    // #endif
  })
}

async function openRecordPopup(item: VoiceCloneItem) {
  if (!promptLogin('登录后可录制音色样本')) {
    return
  }

  if (recordingId.value) {
    if (recordingId.value !== item.id) {
      toast.warning('请先停止当前录音')
      return
    }
    recordPopupVisible.value = true
    return
  }

  try {
    recordPermissionChecking.value = true
    await ensureRecordPermission()
    stopAudio()
    recordingTarget.value = item
    recordPopupVisible.value = true
  }
  catch {
    const result = await uni.showModal({
      title: '需要麦克风权限',
      content: '录制音频需要使用麦克风，请在设置中允许麦克风权限。',
      confirmText: '去设置',
      cancelText: '取消',
    })
    if (result.confirm)
      uni.openSetting()
  }
  finally {
    recordPermissionChecking.value = false
  }
}

function startRecording() {
  const target = recordingTarget.value
  if (!target) {
    toast.warning('请选择要录制的音色')
    return
  }

  if (recordingId.value) {
    recorderManager.stop()
    return
  }

  try {
    stopAudio()
    recordingId.value = target.id
    recordingStartAt.value = Date.now()
    recorderManager.start({
      duration: 120000,
      sampleRate: 16000,
      numberOfChannels: 1,
      encodeBitRate: 48000,
      format: 'mp3',
    })
    toast.success('开始录音')
  }
  catch (error: any) {
    resetRecordingState()
    toast.error(error?.message || '录音启动失败')
  }
}

function cancelRecording() {
  if (recordingId.value) {
    recordDiscarding.value = true
    recorderManager.stop()
    return
  }
  recordPopupVisible.value = false
  recordingTarget.value = null
}

async function handleClone(item: VoiceCloneItem) {
  if (!promptLogin('登录后可发起音色复刻')) {
    return
  }

  if (!item.hasVoice) {
    toast.warning('请先上传音频样本')
    return
  }

  try {
    cloningId.value = item.id
    await cloneVoiceAudio(item.id)
    const target = voiceCloneList.value.find(voice => voice.id === item.id)
    if (target) {
      target.trainStatus = 1
      target.trainError = null
    }
    toast.success('已提交复刻任务')
  }
  catch (error: any) {
    toast.error(error?.message || '复刻失败')
  }
  finally {
    cloningId.value = ''
  }
}

function stopAudio() {
  if (audioContext.value) {
    audioContext.value.stop()
    audioContext.value.destroy()
    audioContext.value = null
  }
  playingId.value = ''
}

recorderManager.onStop(async (result) => {
  const shouldDiscard = recordDiscarding.value
  const target = recordingTarget.value
  const duration = Date.now() - recordingStartAt.value
  resetRecordingState()

  if (shouldDiscard) {
    recordPopupVisible.value = false
    toast.success('已取消录音')
    return
  }

  if (!target) {
    return
  }
  if (duration < 8000) {
    recordingTarget.value = target
    uni.showModal({
      title: '录音太短',
      content: '请至少录制 8 秒清晰人声后再上传。',
      showCancel: false,
      confirmText: '知道了',
    })
    return
  }
  if (duration > 60000) {
    recordingTarget.value = target
    uni.showModal({
      title: '录音太长',
      content: '请录制 60 秒以内的音频后再上传。',
      showCancel: false,
      confirmText: '知道了',
    })
    return
  }
  if (!result.tempFilePath) {
    recordingTarget.value = target
    toast.error('录音文件生成失败')
    return
  }

  try {
    await uploadRecordedAudio(target, result.tempFilePath)
  }
  catch (error: any) {
    toast.error(error?.message || '录音上传失败')
  }
  finally {
    uploadingId.value = ''
    recordPopupVisible.value = false
  }
})

recorderManager.onError((error) => {
  console.error('录音失败:', error)
  resetRecordingState()
  recordPopupVisible.value = false
  toast.error('录音失败')
})

async function handlePlay(item: VoiceCloneItem) {
  if (!promptLogin('登录后可试听音色')) {
    return
  }

  if (playingId.value === item.id) {
    stopAudio()
    return
  }

  try {
    stopAudio()
    const uuid = await getVoiceCloneAudioId(item.id)
    const ctx = uni.createInnerAudioContext()
    ctx.autoplay = true
    ctx.src = getVoiceClonePlayUrl(uuid)
    audioContext.value = ctx
    playingId.value = item.id

    ctx.onEnded(() => {
      stopAudio()
    })
    ctx.onError(() => {
      stopAudio()
      toast.error('播放失败')
    })
    ctx.play()
  }
  catch (error: any) {
    stopAudio()
    toast.error(error?.message || '播放失败')
  }
}

onPullDownRefresh(() => {
  if (redirectToLoginIfAuthExpired()) {
    uni.stopPullDownRefresh()
    return
  }

  if (syncLoginState()) {
    uni.stopPullDownRefresh()
    return
  }

  refreshing.value = true
  void fetchVoiceClones(false).finally(() => {
    uni.stopPullDownRefresh()
  })
})

onShow(() => {
  if (redirectToLoginIfAuthExpired()) {
    return
  }

  if (syncLoginState()) {
    stopAutoRefresh()
    stopAudio()
    voiceCloneList.value = []
    loading.value = false
    return
  }

  void fetchVoiceClones()
  startAutoRefresh()
})

onHide(() => {
  if (recordingId.value) {
    recorderManager.stop()
  }
  stopAutoRefresh()
  stopAudio()
})

onUnload(() => {
  if (recordingId.value) {
    recorderManager.stop()
  }
  stopAutoRefresh()
  stopAudio()
})

</script>

<template>
  <view class="voice-clone-page">
    <wd-navbar title="音色克隆" safe-area-inset-top fixed placeholder />

    <!-- <view class="page-intro">
      <view class="page-subtitle">
        上传样本音频并发起复刻，状态会自动刷新
      </view>
    </view>

    <view v-if="!isGuest" class="search-bar">
      <wd-input
        v-model="searchName"
        clearable
        no-border
        placeholder="搜索名称 / 音色ID / 平台"
      />
      <wd-button size="small" type="primary" @click="fetchVoiceClones">
        查询
      </wd-button>
    </view> -->

    <view v-if="isGuest" class="guest-container">
      <view class="guest-card">
        <view class="guest-title">
          游客模式
        </view>
        <view class="guest-desc">
          音色资源属于账号数据，登录后可上传样本、复刻音色并试听。
        </view>
        <wd-button type="primary" custom-class="guest-login-btn" @click="promptLogin('登录后可使用音色克隆功能')">
          去登录
        </wd-button>
      </view>
    </view>

    <view v-else-if="loading" class="loading-container">
      <wd-loading color="#336cff" />
      <text class="loading-text">加载中...</text>
    </view>

    <view v-else-if="!filteredVoiceCloneList.length" class="empty-container">
      <wd-status-tip image="content" tip="暂无可用音色资源" />
    </view>

    <view v-else class="list-wrap">
      <view
        v-for="item in filteredVoiceCloneList"
        :key="item.id"
        class="clone-card"
      >
        <view class="clone-card__header">
          <view class="clone-card__title-wrap">
            <text class="clone-card__title">
              {{ item.name || '-' }}
            </text>
            <wd-tag custom-class="status-tag" :type="getTrainStatusType(item)">
              {{ getTrainStatusText(item) }}
            </wd-tag>
          </view>
          <text class="clone-card__edit" @click="openRenamePopup(item)">
            重命名
          </text>
        </view>

        <view class="clone-card__meta">
          <text>{{ item.modelName || '-' }}</text>
          <text>{{ item.languages || '-' }}</text>
        </view>

        <!-- <view class="clone-card__info">
          <text class="clone-card__label">音色ID</text>
          <text class="clone-card__value">{{ item.voiceId || '-' }}</text>
        </view> -->
<!-- 
        <view class="clone-card__info">
          <text class="clone-card__label">创建时间</text>
          <text class="clone-card__value">{{ item.createDate || '-' }}</text>
        </view> -->

        <view v-if="item.trainError" class="clone-card__error">
          {{ item.trainError }}
        </view>

        <view class="clone-card__actions">
          <wd-button size="small" plain @click="handlePlay(item)">
            {{ playingId === item.id ? '停止试听' : '试听' }}
          </wd-button>
          <wd-button size="small" :loading="uploadingId === item.id" @click="chooseAndUpload(item)">
            上传音频
          </wd-button>
          <wd-button
            size="small"
            plain
            :type="recordingId === item.id ? 'danger' : 'info'"
            :loading="recordPermissionChecking || (uploadingId === item.id && recordingId !== item.id)"
            @click="openRecordPopup(item)"
          >
            {{ recordingId === item.id ? '录制中' : '录制音频' }}
          </wd-button>
          <wd-button
            size="small"
            type="primary"
            :disabled="!item.hasVoice"
            :loading="cloningId === item.id"
            @click="handleClone(item)"
          >
            立即复刻
          </wd-button>
        </view>
      </view>
    </view>

    <wd-popup
      v-model="renamePopupVisible"
      position="center"
      custom-style="width: 88%; max-width: 640rpx; border-radius: 24rpx;"
      safe-area-inset-bottom
    >
      <view class="popup-wrap">
        <view class="popup-title">
          修改名称
        </view>
        <wd-input
          v-model="renameValue"
          clearable
          placeholder="请输入新的音色名称"
        />
        <view class="popup-actions">
          <wd-button type="info" custom-class="popup-btn" @click="renamePopupVisible = false">
            取消
          </wd-button>
          <wd-button type="primary" custom-class="popup-btn" :loading="renameSubmitting" @click="submitRename">
            保存
          </wd-button>
        </view>
      </view>
    </wd-popup>

    <wd-popup
      v-model="recordPopupVisible"
      position="center"
      custom-style="width: 88%; max-width: 640rpx; border-radius: 24rpx;"
      safe-area-inset-bottom
      @close="cancelRecording"
    >
      <view class="popup-wrap record-popup">
        <view class="popup-title">
          录制音频样本
        </view>
        <view class="record-target">
          {{ recordingTarget?.name || '当前音色' }}
        </view>
        <view class="record-desc">
          请录制 8 - 30秒清晰人声。
        </view>
        <view
          class="record-main-btn"
          :class="{ 'record-main-btn--recording': !!recordingId }"
          @click="startRecording"
        >
          {{ recordingId ? '停止录制并上传' : '开始录制' }}
        </view>
        <view class="record-cancel-btn" @click="cancelRecording">
          {{ recordingId ? '取消录音' : '取消' }}
        </view>
      </view>
    </wd-popup>
  </view>
</template>

<style lang="scss" scoped>
.voice-clone-page {
  min-height: 100vh;
  background: #f5f7fb;
  padding: 24rpx;
  box-sizing: border-box;
}

.page-intro {
  padding: 8rpx 8rpx 20rpx;
}

.page-subtitle {
  font-size: 24rpx;
  color: #8a94a6;
  line-height: 1.6;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx 20rpx;
  margin-bottom: 20rpx;
  border-radius: 24rpx;
  background: #ffffff;
  box-shadow: 0 10rpx 30rpx rgba(85, 113, 175, 0.08);
}

.loading-container,
.empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 40rpx;
}

.guest-container {
  padding: 28rpx 0;
}

.guest-card {
  padding: 40rpx 34rpx;
  border-radius: 28rpx;
  background: #ffffff;
  box-shadow: 0 12rpx 36rpx rgba(76, 98, 146, 0.08);
}

.guest-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #20283a;
}

.guest-desc {
  margin-top: 18rpx;
  font-size: 26rpx;
  line-height: 1.7;
  color: #778197;
}

:deep(.guest-login-btn) {
  margin-top: 32rpx;
  min-width: 220rpx;
  height: 76rpx;
  border-radius: 999rpx;
  background: #336cff;
  border: none;
}

.loading-text {
  margin-top: 20rpx;
  font-size: 28rpx;
  color: #666666;
}

.list-wrap {
  padding-bottom: calc(40rpx + env(safe-area-inset-bottom));
}

.clone-card {
  margin-bottom: 20rpx;
  padding: 28rpx;
  border-radius: 28rpx;
  background: #ffffff;
  box-shadow: 0 14rpx 40rpx rgba(76, 98, 146, 0.08);
}

.clone-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
}

.clone-card__title-wrap {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex-wrap: wrap;
}

.clone-card__title {
  font-size: 32rpx;
  font-weight: 700;
  color: #232338;
}

.clone-card__edit {
  flex: none;
  font-size: 24rpx;
  color: #336cff;
}

.clone-card__meta {
  display: flex;
  gap: 16rpx;
  flex-wrap: wrap;
  margin-top: 16rpx;
  font-size: 24rpx;
  color: #8a94a6;
}

.clone-card__info {
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
  margin-top: 16rpx;
}

.clone-card__label {
  flex: none;
  font-size: 24rpx;
  color: #8a94a6;
}

.clone-card__value {
  font-size: 24rpx;
  color: #3a4459;
  word-break: break-all;
}

.clone-card__error {
  margin-top: 16rpx;
  padding: 16rpx 18rpx;
  border-radius: 18rpx;
  background: rgba(238, 92, 102, 0.08);
  color: #d9485f;
  font-size: 24rpx;
  line-height: 1.5;
}

.clone-card__actions {
  display: flex;
  gap: 14rpx;
  flex-wrap: wrap;
  margin-top: 24rpx;
}

.status-tag {
  flex: none;
}

.popup-wrap {
  padding: 32rpx;
}

.popup-title {
  margin-bottom: 24rpx;
  text-align: center;
  font-size: 32rpx;
  font-weight: 700;
  color: #232338;
}

.popup-actions {
  display: flex;
  gap: 16rpx;
  margin-top: 28rpx;
}

.popup-btn {
  flex: 1;
}

.record-popup {
  text-align: center;
}

.record-popup :deep(.popup-title) {
  margin-bottom: 18rpx;
}

.record-target {
  margin-bottom: 18rpx;
  padding: 14rpx 18rpx;
  border-radius: 16rpx;
  background: #f5f7fb;
  font-size: 28rpx;
  font-weight: 600;
  color: #273042;
  word-break: break-all;
}

.record-desc {
  margin-bottom: 28rpx;
  color: #7a8498;
  font-size: 24rpx;
  line-height: 1.6;
  text-align: left;
}

.record-main-btn {
  height: 92rpx;
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #336cff;
  color: #fff;
  font-size: 30rpx;
  font-weight: 700;
  box-shadow: 0 16rpx 34rpx rgba(51, 108, 255, 0.24);
}

.record-main-btn--recording {
  background: #e95b5b;
  box-shadow: 0 16rpx 34rpx rgba(233, 91, 91, 0.24);
}

.record-cancel-btn {
  margin: 22rpx auto 0;
  width: 240rpx;
  height: 66rpx;
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f1f3f8;
  color: #4f5667;
  font-size: 26rpx;
  font-weight: 600;
}
</style>
