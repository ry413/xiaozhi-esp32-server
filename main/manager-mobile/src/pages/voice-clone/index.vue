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
import { toast } from '@/utils/toast'

defineOptions({
  name: 'VoiceCloneMobile',
})

const loading = ref(false)
const refreshing = ref(false)
const searchName = ref('')
const voiceCloneList = ref<VoiceCloneItem[]>([])
const refreshTimer = ref<ReturnType<typeof setInterval> | null>(null)
const uploadingId = ref('')
const cloningId = ref('')
const playingId = ref('')
const renamePopupVisible = ref(false)
const renameSubmitting = ref(false)
const currentRenameItem = ref<VoiceCloneItem | null>(null)
const renameValue = ref('')
const audioContext = ref<UniApp.InnerAudioContext | null>(null)

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
  stopAutoRefresh()
  refreshTimer.value = setInterval(() => {
    if (!loading.value && !uploadingId.value) {
      void fetchVoiceClones(false)
    }
  }, 10000)
}

function stopAutoRefresh() {
  if (refreshTimer.value) {
    clearInterval(refreshTimer.value)
    refreshTimer.value = null
  }
}

function openRenamePopup(item: VoiceCloneItem) {
  currentRenameItem.value = item
  renameValue.value = item.name || ''
  renamePopupVisible.value = true
}

async function submitRename() {
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

async function chooseAndUpload(item: VoiceCloneItem) {
  try {
    const chooseResult = await new Promise<any>((resolve, reject) => {
      uni.chooseFile({
        count: 1,
        type: 'all',
        extension: ['mp3', 'wav'],
        success: resolve,
        fail: reject,
      })
    })

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

async function handleClone(item: VoiceCloneItem) {
  if (!item.hasVoice) {
    toast.warning('请先上传音频样本')
    return
  }

  try {
    cloningId.value = item.id
    await cloneVoiceAudio(item.id)
    toast.success('已提交复刻任务')
    await fetchVoiceClones(false)
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

async function handlePlay(item: VoiceCloneItem) {
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
  refreshing.value = true
  void fetchVoiceClones(false).finally(() => {
    uni.stopPullDownRefresh()
  })
})

onShow(() => {
  void fetchVoiceClones()
  startAutoRefresh()
})

onHide(() => {
  stopAutoRefresh()
  stopAudio()
})

onUnload(() => {
  stopAutoRefresh()
  stopAudio()
})

</script>

<template>
  <view class="voice-clone-page">
    <view class="page-header">
      <view class="page-title">
        音色克隆
      </view>
      <view class="page-subtitle">
        上传样本音频并发起复刻，状态会自动刷新
      </view>
    </view>

    <view class="search-bar">
      <wd-input
        v-model="searchName"
        clearable
        no-border
        placeholder="搜索名称 / 音色ID / 平台"
      />
      <wd-button size="small" type="primary" @click="fetchVoiceClones">
        查询
      </wd-button>
    </view>

    <view v-if="loading" class="loading-container">
      <wd-loading color="#336cff" />
      <text class="loading-text">加载中...</text>
    </view>

    <view v-else-if="!filteredVoiceCloneList.length" class="empty-container">
      <wd-status-tip image="content" tip="暂无可用音色资源" />
    </view>

    <scroll-view v-else scroll-y class="list-scroll">
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

        <view class="clone-card__info">
          <text class="clone-card__label">音色ID</text>
          <text class="clone-card__value">{{ item.voiceId || '-' }}</text>
        </view>

        <view class="clone-card__info">
          <text class="clone-card__label">创建时间</text>
          <text class="clone-card__value">{{ item.createDate || '-' }}</text>
        </view>

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
            type="primary"
            :disabled="!item.hasVoice"
            :loading="cloningId === item.id"
            @click="handleClone(item)"
          >
            立即复刻
          </wd-button>
        </view>
      </view>
    </scroll-view>

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
  </view>
</template>

<style lang="scss" scoped>
.voice-clone-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f5f8ff 0%, #f8fbff 30%, #ffffff 100%);
  padding: 24rpx;
  box-sizing: border-box;
}

.page-header {
  padding: 16rpx 8rpx 24rpx;
}

.page-title {
  font-size: 44rpx;
  font-weight: 700;
  color: #232338;
}

.page-subtitle {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #8a94a6;
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

.loading-text {
  margin-top: 20rpx;
  font-size: 28rpx;
  color: #666666;
}

.list-scroll {
  height: calc(100vh - 260rpx);
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
</style>
