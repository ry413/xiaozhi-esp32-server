<route lang="jsonc" type="page">
{
  "layout": "default",
  "style": {
    "navigationBarTitleText": "语音设置",
    "navigationStyle": "custom"
  }
}
</route>

<script lang="ts" setup>
import { t } from '@/i18n'
import { useSpeedPitch } from '@/store'

defineOptions({
  name: 'SpeedPitch',
})

const speedPitchStore = useSpeedPitch()

const localSettings = ref({
  ttsVolume: 0,
  ttsRate: 0,
  ttsPitch: 0,
})

type SpeedPitchKey = 'ttsVolume' | 'ttsRate' | 'ttsPitch'

function updateLocalSetting(key: SpeedPitchKey, value: number) {
  localSettings.value[key] = Number(value)
}

function handleConfirm() {
  speedPitchStore.updateSpeedPitch(localSettings.value)
  goBack()
}

// 返回上一页并更新配置
function goBack() {
  uni.navigateBack()
}

onMounted(() => {
  localSettings.value = {
    ttsVolume: speedPitchStore.speedPitch.ttsVolume,
    ttsRate: speedPitchStore.speedPitch.ttsRate,
    ttsPitch: speedPitchStore.speedPitch.ttsPitch,
  }
})
</script>

<template>
  <view class="h-screen flex flex-col bg-[#f5f7fb]">
    <!-- 头部导航 -->
    <wd-navbar
      :title="t('agent.languageConfig')"
      safe-area-inset-top
      left-arrow
      :bordered="false"
      @click-left="goBack"
    >
      <template #left>
        <wd-icon name="arrow-left" size="18" />
      </template>
    </wd-navbar>
    <view class="flex flex-1 flex-col overflow-hidden">
      <text class="px-[40rpx] py-[30rpx] text-[32rpx] text-[#232338] font-bold">
        并非所有语音模型都支持以下设置，具体效果请以实际使用为准。
      </text>
      <view class="flex flex-1 flex-col gap-[50rpx] overflow-y-auto px-[40rpx] py-[50rpx]">
        <!-- 音量调节 -->
        <view class="flex flex-col gap-[20rpx]">
          <text class="text-[30rpx] text-[#232338] font-semibold">
            {{ t('agent.ttsVolume') }}
          </text>
          <view class="flex items-center gap-[20rpx]">
            <slider
              class="flex-1"
              :value="localSettings.ttsVolume"
              :min="-100"
              :max="100"
              :step="1"
              active-color="#336cff"
              background-color="#d9e2ff"
              block-color="#336cff"
              :block-size="22"
              @changing="event => updateLocalSetting('ttsVolume', event.detail.value)"
              @change="event => updateLocalSetting('ttsVolume', event.detail.value)"
            />
            <text class="min-w-[80rpx] text-right text-[28rpx] text-[#336cff] font-medium">
              {{ localSettings.ttsVolume }}
            </text>
          </view>
          <text class="mt-[10rpx] text-[24rpx] text-[#9d9ea3]">
            {{ t('agent.volumeHint') }}
          </text>
        </view>

        <!-- 语速调节 -->
        <view class="flex flex-col gap-[20rpx]">
          <text class="text-[30rpx] text-[#232338] font-semibold">
            {{ t('agent.ttsRate') }}
          </text>
          <view class="flex items-center gap-[20rpx]">
            <slider
              class="flex-1"
              :value="localSettings.ttsRate"
              :min="-100"
              :max="100"
              :step="1"
              active-color="#336cff"
              background-color="#d9e2ff"
              block-color="#336cff"
              :block-size="22"
              @changing="event => updateLocalSetting('ttsRate', event.detail.value)"
              @change="event => updateLocalSetting('ttsRate', event.detail.value)"
            />
            <text class="min-w-[80rpx] text-right text-[28rpx] text-[#336cff] font-medium">
              {{ localSettings.ttsRate }}
            </text>
          </view>
          <text class="mt-[10rpx] text-[24rpx] text-[#9d9ea3]">
            {{ t('agent.speedHint') }}
          </text>
        </view>

        <!-- 音调调节 -->
        <view class="flex flex-col gap-[20rpx]">
          <text class="text-[30rpx] text-[#232338] font-semibold">
            {{ t('agent.ttsPitch') }}
          </text>
          <view class="flex items-center gap-[20rpx]">
            <slider
              class="flex-1"
              :value="localSettings.ttsPitch"
              :min="-100"
              :max="100"
              :step="1"
              active-color="#336cff"
              background-color="#d9e2ff"
              block-color="#336cff"
              :block-size="22"
              @changing="event => updateLocalSetting('ttsPitch', event.detail.value)"
              @change="event => updateLocalSetting('ttsPitch', event.detail.value)"
            />
            <text class="min-w-[80rpx] text-right text-[28rpx] text-[#336cff] font-medium">
              {{ localSettings.ttsPitch }}
            </text>
          </view>
          <text class="mt-[10rpx] text-[24rpx] text-[#9d9ea3]">
            {{ t('agent.pitchHint') }}
          </text>
        </view>
      </view>

      <view class="flex gap-[20rpx] border-t border-[#eee] px-[30rpx] py-[30rpx]">
        <wd-button type="primary" class="h-[80rpx] flex-1 rounded-[12rpx] text-[28rpx] !bg-[#336cff]" @click="handleConfirm">
          {{ t('agent.save') }}
        </wd-button>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
</style>
