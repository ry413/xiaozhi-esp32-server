<route lang="jsonc" type="page">
{
  "layout": "tabbar",
  "style": {
    "navigationStyle": "custom",
    "navigationBarTitleText": "导播"
  }
}
</route>

<script lang="ts" setup>
import DirectorDeskPanel from '@/components/director-desk/DirectorDeskPanel.vue'
import MaihuoConfigPanel from '@/components/maihuo-config/MaihuoConfigPanel.vue'
import { isGuestMode, promptLogin, redirectToLoginIfAuthExpired } from '@/utils/auth'

type DeskTab = 'director' | 'plan'

const activeTab = ref<DeskTab>('director')
const isGuest = ref(isGuestMode())

const tabs: Array<{ label: string, value: DeskTab }> = [
  { label: '导播台', value: 'director' },
  { label: '方案配置', value: 'plan' },
]

function switchTab(value: DeskTab) {
  activeTab.value = value
}

onShow(() => {
  if (redirectToLoginIfAuthExpired()) {
    return
  }

  isGuest.value = isGuestMode()
})
</script>

<template>
  <view class="director-shell">
    <wd-navbar title="导播" safe-area-inset-top placeholder fixed />

    <view class="sub-tabs">
      <view
        v-for="item in tabs"
        :key="item.value"
        class="sub-tab"
        :class="{ 'sub-tab--active': activeTab === item.value }"
        @click="switchTab(item.value)"
      >
        {{ item.label }}
      </view>
    </view>

    <view v-if="isGuest" class="guest-panel">
      <view class="guest-card">
        <view class="guest-title">
          游客模式
        </view>
        <view class="guest-desc">
          导播台和方案配置需要读取你的机器人、直播方案和权益信息。你可以先浏览页面结构，登录后即可使用完整功能。
        </view>
        <wd-button type="primary" custom-class="guest-login-btn" @click="promptLogin('登录后可使用导播台和卖货方案配置')">
          去登录
        </wd-button>
      </view>
    </view>
    <template v-else>
      <DirectorDeskPanel v-if="activeTab === 'director'" />
      <MaihuoConfigPanel v-else />
    </template>
  </view>
</template>

<style scoped lang="scss">
.director-shell {
  min-height: 100vh;
  background: #f4f6fb;
}

.sub-tabs {
  position: sticky;
  top: 0;
  z-index: 20;
  height: 76rpx;
  padding: 0 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
  background: #fff;
  border-bottom: 2rpx solid #eef1f6;
}

.sub-tab {
  position: relative;
  min-width: 150rpx;
  height: 76rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  font-weight: 500;
  color: #8f96a3;
}

.sub-tab--active {
  color: #336cff;
  font-weight: 700;
}

.sub-tab--active::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 0;
  width: 48rpx;
  height: 4rpx;
  border-radius: 999rpx;
  background: #336cff;
  transform: translateX(-50%);
}

.guest-panel {
  padding: 28rpx;
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
</style>
