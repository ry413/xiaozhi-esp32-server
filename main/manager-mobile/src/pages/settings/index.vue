<route lang="jsonc" type="page">
{
  "layout": "default",
  "style": {
    "navigationBarTitleText": "设置",
    "navigationStyle": "custom"
  }
}
</route>

<script lang="ts" setup>
import type { Language } from '@/store/lang'
import { computed, onMounted, reactive, ref } from 'vue'
import { useMessage, useToast } from 'wot-design-uni'
import { useAppShare } from '@/hooks/useAppShare'
import { changeLanguage, getCurrentLanguage, getSupportedLanguages, t } from '@/i18n'
import { useConfigStore } from '@/store'
import { clearServerBaseUrlOverride, getEnvBaseUrl, getServerBaseUrlOverride, setServerBaseUrlOverride } from '@/utils'
import { clearAuthExpired, clearLoginState, goToLogin, isGuestMode, redirectToLoginIfAuthExpired } from '@/utils/auth'
import { isMp } from '@/utils/platform'

defineOptions({
  name: 'SettingsPage',
})

useAppShare({
  title: '小助助播',
})

const toast = useToast()
const message = useMessage()

// 缓存信息
const cacheInfo = reactive({
  storageSize: '0MB',
  imageCache: '0MB',
  dataCache: '0MB',
})

const configStore = useConfigStore()
const isGuest = ref(isGuestMode())
const showExternalDocGuide = ref(false)
const FEISHU_DOC_URL = 'https://ccnp5x0z5idv.feishu.cn/wiki/Qjnaw0vfAiYuuEkXktncPlRXn0e'
const SUPPORT_PHONE = '17727425832'

// 服务端地址设置
const baseUrlInput = ref('')
const urlError = ref('')

// 系统信息（保留）
const systemInfo = computed(() => {
  const info = uni.getSystemInfoSync()
  return `${info.platform} ${info.system}`
})

// 读取本地覆盖地址
function loadServerBaseUrl() {
  const override = getServerBaseUrlOverride()
  baseUrlInput.value = override || getEnvBaseUrl()
}

// 获取缓存信息
function getCacheInfo() {
  try {
    const info = uni.getStorageInfoSync()
    const totalSize = (info.currentSize || 0) / 1024 // KB to MB
    cacheInfo.storageSize = `${totalSize.toFixed(2)}MB`
  }
  catch (error) {
    console.error('获取缓存信息失败:', error)
  }
}

// 验证URL格式
function validateUrl() {
  urlError.value = ''

  if (!baseUrlInput.value) {
    return
  }

  if (!/^https?:\/\/.+\/xiaozhi$/.test(baseUrlInput.value)) {
    urlError.value = t('settings.validServerUrl')
  }
}

// 测试服务端地址
async function testServerBaseUrl() {
  // 先清除错误信息
  urlError.value = ''

  if (!baseUrlInput.value || !/^https?:\/\/.+\/xiaozhi$/.test(baseUrlInput.value)) {
    return false
  }

  try {
    const response = await uni.request({
      url: `${baseUrlInput.value}/api/ping`,
      method: 'GET',
      timeout: 3000,
    })

    if (response.statusCode === 200) {
      return true
    }
    else {
      toast.error({
        msg: t('message.invalidAddress'),
        duration: 3000,
      })
      return false
    }
  }
  catch (error) {
    console.error('测试服务端地址失败:', error)
    toast.error({
      msg: t('message.invalidAddress'),
      duration: 3000,
    })
    return false
  }
}

// 保存服务端地址
async function saveServerBaseUrl() {
  if (!baseUrlInput.value || !/^https?:\/\/.+\/xiaozhi$/.test(baseUrlInput.value)) {
    toast.warning(t('settings.validServerUrl'))
    return
  }

  // 测试地址有效性
  const isServerValid = await testServerBaseUrl()
  if (!isServerValid) {
    return
  }
  setServerBaseUrlOverride(baseUrlInput.value)
  // 处理config缓存无法更新的问题
  uni.request({
    url: `${getEnvBaseUrl()}/user/pub-config`,
    method: 'GET',
    success: (res: any) => {
      if (res.statusCode === 200) {
        configStore.setConfig(res.data.data)
        uni.setStorageSync('config', res.data.data)
      }
    },
    fail: (err) => {
      console.error('获取SM2公钥失败:', err)
    },
  })

  // 切换请求地址后清空所有缓存
  clearAllCacheAfterUrlChange()

  uni.showModal({
    title: t('settings.restartApp'),
    content: t('settings.serverUrlSavedAndCacheCleared'),
    confirmText: t('settings.restartNow'),
    cancelText: t('settings.restartLater'),
    success: (res) => {
      if (res.confirm) {
        restartApp()
      }
      else {
        toast.success(t('settings.restartSuccess'))
      }
    },
  })
}

// 语言切换
const supportedLanguages = getSupportedLanguages()
const currentLanguage = ref<Language>(getCurrentLanguage())
const showLanguageSheet = ref(false)

function handleLanguageChange(lang: Language) {
  changeLanguage(lang)
  showLanguageSheet.value = false
  currentLanguage.value = lang
  toast.success(t('settings.languageChanged'))
}

// 重置为 env 默认
function resetServerBaseUrl() {
  clearServerBaseUrlOverride()
  baseUrlInput.value = getEnvBaseUrl()

  // 切换请求地址后清空所有缓存
  clearAllCacheAfterUrlChange()

  uni.showModal({
    title: t('settings.restartApp'),
    content: t('settings.resetToDefaultAndCacheCleared'),
    confirmText: t('settings.restartNow'),
    cancelText: t('settings.restartLater'),
    success: (res) => {
      if (res.confirm) {
        restartApp()
      }
      else {
        toast.success(t('settings.resetSuccess'))
      }
    },
  })
}

// 重启应用（App 原生重启；其他端回到首页）
function restartApp() {
  // #ifdef APP-PLUS
  plus.runtime.restart()
  // #endif
  // #ifndef APP-PLUS
  uni.reLaunch({ url: '/pages/index/index' })
  // #endif
}

// 切换地址后自动清空所有缓存
function clearAllCacheAfterUrlChange() {
  try {
    // 备份运行时覆盖地址，确保清理后恢复
    const preservedOverride = getServerBaseUrlOverride()

    // 完全清空所有缓存，包括token
    uni.clearStorageSync()

    // 清空localStorage（H5环境）
    // #ifdef H5
    if (typeof localStorage !== 'undefined') {
      localStorage.clear()
    }
    // #endif

    // 恢复运行时覆盖地址（如有），需要在清理完成后再写入
    if (preservedOverride) {
      setServerBaseUrlOverride(preservedOverride)
    }

    // 重新获取缓存信息
    getCacheInfo()
  }
  catch (error) {
    console.error('清除缓存失败:', error)
  }
}

// 清除缓存
async function clearCache() {
  try {
    uni.showModal({
      title: t('settings.confirmClear'),
      content: t('settings.confirmClearMessage'),
      confirmText: t('common.confirm'),
      cancelText: t('common.cancel'),
      success: (res) => {
        if (res.confirm) {
          clearAllCacheAfterUrlChange()
          toast.success(t('settings.cacheCleared'))

          // 延迟跳转到登录页
          setTimeout(() => {
            uni.reLaunch({ url: '/pages-sub/login/index' })
          }, 1500)
        }
      },
    })
  }
  catch (error) {
    console.error('清除缓存失败:', error)
    toast.error(t('settings.clearCacheFailed'))
  }
}

// 关于我们
function showAbout() {
  uni.showModal({
    title: t('settings.aboutApp', { appName: import.meta.env.VITE_APP_TITLE }),
    content: t('settings.aboutContent', {
      appName: import.meta.env.VITE_APP_TITLE,
      version: '0.9.2'
    }),
    showCancel: false,
    confirmText: t('common.confirm'),
  })
}

function handleLogout() {
  if (isGuestMode()) {
    goToLogin()
    return
  }

  message.confirm({
    title: '退出登录',
    msg: '确定退出当前账号吗？',
    confirmButtonText: '退出',
    cancelButtonText: '取消',
  }).then(() => {
    clearAuthExpired()
    clearLoginState()
    uni.reLaunch({ url: '/pages-sub/login/index' })
  }).catch(() => {})
}

function openExternalDocGuide() {
  uni.setClipboardData({
    data: FEISHU_DOC_URL,
    success: () => {
      showExternalDocGuide.value = true
    },
    fail: () => {
      showExternalDocGuide.value = true
    },
  })
}

function callSupportPhone() {
  uni.makePhoneCall({
    phoneNumber: SUPPORT_PHONE,
    fail: (error) => {
      console.error('拨打技术支持电话失败:', error)
    },
  })
}

onShow(() => {
  if (redirectToLoginIfAuthExpired()) {
    return
  }

  isGuest.value = isGuestMode()
})

onMounted(async () => {
  // 仅在非小程序环境加载服务端地址设置
  if (!isMp) {
    loadServerBaseUrl()
  }
  getCacheInfo()

  // 动态设置导航栏标题为国际化文本
  uni.setNavigationBarTitle({
    title: t('settings.title'),
  })
})
</script>

<template>
  <view class="min-h-screen bg-[#f5f7fb]">
    <wd-navbar :title="t('settings.title')" placeholder safe-area-inset-top fixed />

    <view class="p-[24rpx]">
      <!-- 网络设置 - 仅在非小程序环境显示 -->
      <view v-if="!isMp" class="mb-[32rpx]">
        <view class="mb-[24rpx] flex items-center">
          <text class="text-[32rpx] text-[#232338] font-bold">
            {{ t('settings.networkSettings') }}
          </text>
        </view>

        <view
          class="overflow-hidden border border-[#eeeeee] rounded-[24rpx] bg-[#fbfbfb] p-[32rpx]"
          style="box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.06);"
        >
          <view class="mb-[24rpx]">
            <text class="text-[28rpx] text-[#232338] font-semibold">
              {{ t('settings.serverApiUrl') }}
            </text>
            <text class="mt-[8rpx] block text-[24rpx] text-[#9d9ea3]">
              {{ t('settings.modifyWillClearCache') }}
            </text>
          </view>

          <view class="mb-[24rpx]">
            <view class="w-full overflow-hidden border border-[#eeeeee] rounded-[16rpx] bg-[#f5f7fb]">
              <wd-input
                v-model="baseUrlInput" type="text" clearable :maxlength="200"
                :placeholder="t('settings.enterServerUrl')"
                custom-class="!border-none !bg-transparent h-[64rpx] px-[24rpx] items-center"
                input-class="text-[28rpx] text-[#232338]" @input="validateUrl" @blur="validateUrl"
              />
            </view>
            <text v-if="urlError" class="mt-[8rpx] block text-[24rpx] text-[#ff4d4f]">
              {{ urlError }}
            </text>
          </view>

          <view class="flex gap-[16rpx]">
            <wd-button
              type="primary"
              custom-class="flex-1 h-[88rpx] rounded-[20rpx] text-[28rpx] font-semibold bg-[#336cff] border-none shadow-[0_4rpx_16rpx_rgba(51,108,255,0.3)] active:shadow-[0_2rpx_8rpx_rgba(51,108,255,0.4)] active:scale-98"
              @click="saveServerBaseUrl"
            >
              {{ t('settings.saveSettings') }}
            </wd-button>
            <wd-button
              type="default"
              custom-class="flex-1 h-[88rpx] rounded-[20rpx] text-[28rpx] font-semibold bg-white border-[#eeeeee] text-[#65686f] active:bg-[#f5f7fb]"
              @click="resetServerBaseUrl"
            >
              {{ t('settings.resetDefault') }}
            </wd-button>
          </view>
        </view>
      </view>

      <!-- 缓存管理 -->
      <view class="mb-[32rpx]">
        <view class="mb-[24rpx] flex items-center">
          <text class="text-[32rpx] text-[#232338] font-bold">
            {{ t('settings.cacheManagement') }}
          </text>
        </view>

        <view
          class="border border-[#eeeeee] rounded-[24rpx] bg-[#fbfbfb] p-[32rpx]"
          style="box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.06);"
        >
          <view class="space-y-[16rpx]">
            <!-- 缓存信息展示，参考插件样式 -->
            <view
              class="flex items-center justify-between border border-[#eeeeee] rounded-[16rpx] bg-[#f5f7fb] p-[24rpx] transition-all active:bg-[#eef3ff]"
            >
              <view>
                <text class="text-[28rpx] text-[#232338] font-medium">
                  {{ t('settings.totalCacheSize') }}
                </text>
                <text class="mt-[4rpx] block text-[24rpx] text-[#9d9ea3]">
                  {{ t('settings.appDataSize') }}
                </text>
              </view>
              <text class="text-[28rpx] text-[#65686f] font-semibold">
                {{ cacheInfo.storageSize }}
              </text>
            </view>

            <!-- 清除缓存按钮，参考插件编辑按钮样式 -->
            <view
              class="flex items-center justify-between border border-[#eeeeee] rounded-[16rpx] bg-[#f5f7fb] p-[24rpx]"
            >
              <view>
                <text class="text-[28rpx] text-[#232338] font-medium">
                  {{ t('settings.cacheClear') }}
                </text>
                <text class="mt-[4rpx] block text-[24rpx] text-[#9d9ea3]">
                  {{ t('settings.clearAllCache') }}
                </text>
              </view>
              <view
                class="cursor-pointer rounded-[24rpx] bg-[rgba(255,107,107,0.1)] px-[28rpx] py-[16rpx] text-[24rpx] text-[#ff6b6b] font-semibold transition-all duration-300 active:scale-95 active:bg-[#ff6b6b] active:text-white"
                @click="clearCache"
              >
                {{ t('settings.clearCache') }}
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- 应用信息 -->
      <!-- <view class="mb-[32rpx]">
        <view class="mb-[24rpx] flex items-center">
          <text class="text-[32rpx] text-[#232338] font-bold">
            {{ t('settings.appInfo') }}
          </text>
        </view>

        <view
          class="border border-[#eeeeee] rounded-[24rpx] bg-[#fbfbfb] p-[32rpx]"
          style="box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.06);"
        >
          <view
            class="flex cursor-pointer items-center justify-between border border-[#eeeeee] rounded-[16rpx] bg-[#f5f7fb] p-[24rpx] transition-all active:bg-[#eef3ff]"
            @click="showAbout"
          >
            <view>
              <text class="text-[28rpx] text-[#232338] font-medium">
                {{ t('settings.aboutUs') }}
              </text>
              <text class="mt-[4rpx] block text-[24rpx] text-[#9d9ea3]">
                {{ t('settings.appVersion') }}
              </text>
            </view>
            <wd-icon name="arrow-right" custom-class="text-[32rpx] text-[#9d9ea3]" />
          </view>
        </view>
      </view> -->

      <!-- 语言设置 -->
      <!-- <view class="mb-[32rpx]">
        <view class="mb-[24rpx] flex items-center">
          <text class="text-[32rpx] text-[#232338] font-bold">
            {{ t('settings.languageSettings') }}
          </text>
        </view>

        <view
          class="border border-[#eeeeee] rounded-[24rpx] bg-[#fbfbfb] p-[32rpx]"
          style="box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.06);"
        >
          <view
            class="flex cursor-pointer items-center justify-between border border-[#eeeeee] rounded-[16rpx] bg-[#f5f7fb] p-[24rpx] transition-all active:bg-[#eef3ff]"
            @click="showLanguageSheet = true"
          >
            <view>
              <text class="text-[32rpx] text-[#232338] font-medium">
                {{ t('settings.language') }}
              </text>
              <text class="mt-[4rpx] block text-[24rpx] text-[#9d9ea3]">
                {{ t('settings.selectLanguage') }}
              </text>
            </view>
            <view class="flex items-center">
              <text class="mr-[16rpx] text-[32rpx] text-[#9d9ea3] font-semibold">
                {{ supportedLanguages.find(lang => lang.code === currentLanguage)?.name }}
              </text>
              <wd-icon name="arrow-right" custom-class="text-[32rpx] text-[#9d9ea3]" />
            </view>
          </view>
        </view>
      </view> -->

      <!-- 语言选择弹窗 -->
      <!-- <wd-action-sheet v-model="showLanguageSheet" :title="t('settings.selectLanguage')" :close-on-click-modal="true">
        <view class="language-sheet">
          <scroll-view scroll-y class="language-list">
            <view
              v-for="lang in supportedLanguages" :key="lang.code" class="language-item"
              @click="handleLanguageChange(lang.code)"
            >
              <text class="language-name">
                {{ lang.name }}
              </text>
            </view>
          </scroll-view>
        </view>
      </wd-action-sheet> -->

      <view class="mb-[32rpx]">
        <view class="mb-[24rpx] flex items-center">
          <text class="text-[32rpx] text-[#232338] font-bold">
            关于我们
          </text>
        </view>

        <view
          class="border border-[#eeeeee] rounded-[24rpx] bg-[#fbfbfb] p-[32rpx]"
          style="box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.06);"
        >
          <view
            class="flex cursor-pointer items-center justify-between border border-[#eeeeee] rounded-[16rpx] bg-[#f5f7fb] p-[24rpx] transition-all active:bg-[#eef3ff]"
            @click="openExternalDocGuide"
          >
            <view>
              <text class="text-[28rpx] text-[#232338] font-medium">
                使用教程
              </text>
              <text class="mt-[4rpx] block text-[24rpx] text-[#9d9ea3]">
                复制链接后在浏览器打开
              </text>
            </view>
            <wd-icon name="arrow-right" custom-class="text-[32rpx] text-[#9d9ea3]" />
          </view>

          <view class="contact-row" @click="callSupportPhone">
            <view>
              <text class="text-[28rpx] text-[#232338] font-medium">
                技术支持
              </text>
              <text class="mt-[4rpx] block text-[24rpx] text-[#9d9ea3]">
                工作日: 09:00 - 18:00
              </text>
            </view>
            <view class="flex items-center">
              <text class="mr-[16rpx] text-[28rpx] text-[#65686f] font-semibold">
                {{ SUPPORT_PHONE }}
              </text>
              <wd-icon name="call" custom-class="text-[30rpx] text-[#9d9ea3]" />
            </view>
          </view>
        </view>
      </view>

      <view class="mt-[16rpx] mb-[32rpx]">
        <wd-button
          type="default"
          custom-class="w-full h-[88rpx] rounded-[20rpx] text-[28rpx] font-semibold bg-white border-[#ffd8d8] text-[#e95b5b] active:bg-[#fff5f5]"
          @click="handleLogout"
        >
          {{ isGuest ? '登录账号' : '退出登录' }}
        </wd-button>
      </view>

      <wd-popup
        v-model="showExternalDocGuide"
        position="center"
        custom-style="width: 86%; border-radius: 28rpx; overflow: visible;"
        safe-area-inset-bottom
      >
        <view class="external-doc-guide">
          <view class="guide-title">
            文档链接已复制
          </view>
          <view class="guide-desc">
            请切换到手机浏览器粘贴链接打开。
          </view>
          <view class="guide-url">
            {{ FEISHU_DOC_URL }}
          </view>
          <wd-button type="primary" custom-class="guide-btn" @click="showExternalDocGuide = false">
            知道了
          </wd-button>
        </view>
      </wd-popup>

      <!-- 底部安全距离 -->
      <!-- 底部安全距离 -->
      <view style="height: env(safe-area-inset-bottom);" />
    </view>
  </view>
</template>

<style lang="scss" scoped>
// 保持与 edit.vue 一致的风格，样式主要通过类名控制

.external-doc-guide {
  position: relative;
  padding: 48rpx 34rpx 34rpx;
  border-radius: 28rpx;
  background: #ffffff;
  text-align: center;
}

.guide-arrow {
  position: absolute;
  top: -18rpx;
  right: 32rpx;
  color: #30333a;
  font-size: 64rpx;
  line-height: 1;
  transform: rotate(-12deg);
}

.guide-title {
  color: #232338;
  font-size: 34rpx;
  font-weight: 700;
}

.guide-desc {
  margin-top: 22rpx;
  color: #5f6878;
  font-size: 28rpx;
  line-height: 1.7;
}

.guide-url {
  margin-top: 22rpx;
  padding: 18rpx 20rpx;
  border-radius: 16rpx;
  background: #f5f7fb;
  color: #8a94a6;
  font-size: 24rpx;
  line-height: 1.5;
  word-break: break-all;
}

:deep(.guide-btn) {
  width: 100%;
  height: 78rpx;
  margin-top: 28rpx;
  border-radius: 16rpx;
}

.contact-row {
  width: 100%;
  margin: 16rpx 0 0;
  padding: 24rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 1rpx solid #eeeeee;
  border-radius: 16rpx;
  background: #f5f7fb;
  box-sizing: border-box;
  line-height: normal;
  text-align: left;

  &::after {
    border: none;
  }

  &:active {
    background: #eef3ff;
  }
}

// 语言选择弹窗样式
.language-sheet {
  .language-list {
    max-height: 50vh;

    .language-item {
      padding: 30rpx 0;
      text-align: center;
      border-bottom: 1rpx solid #f0f0f0;

      .language-name {
        font-size: 28rpx;
        color: #333;
      }

      &:last-child {
        border-bottom: none;
      }

      &:active {
        background-color: #f5f7fb;
      }
    }
  }
}
</style>
