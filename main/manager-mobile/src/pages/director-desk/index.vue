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

type DeskTab = 'director' | 'plan'

const activeTab = ref<DeskTab>('director')

const tabs: Array<{ label: string, value: DeskTab }> = [
  { label: '导播台', value: 'director' },
  { label: '方案配置', value: 'plan' },
]

function switchTab(value: DeskTab) {
  activeTab.value = value
}
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

    <DirectorDeskPanel v-if="activeTab === 'director'" />
    <MaihuoConfigPanel v-else />
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
</style>
