<script lang="ts" setup>
import { onShow } from '@dcloudio/uni-app'
import { computed, onMounted, ref } from 'vue'
import { getMyBenefits, redeemActivationCode } from '@/api/activation-code/activation-code'
import { toast } from '@/utils/toast'

defineOptions({
  name: 'DirectorBenefitsPanel',
})

const emit = defineEmits<{
  (event: 'benefitsChanged'): void
}>()

const loading = ref(false)
const redeemLoading = ref(false)
const redeemCode = ref('')
const membershipActive = ref(false)
const membershipStartAt = ref('')
const membershipEndAt = ref('')
const membershipDailyLimitSeconds = ref(0)
const membershipDailyConsumedSeconds = ref(0)
const membershipDailyRemainingSeconds = ref(0)
const balanceSeconds = ref(0)

const monthlyQuotaPercent = computed(() => {
  if (!membershipDailyLimitSeconds.value)
    return 0
  return Math.min(100, Math.round((membershipDailyConsumedSeconds.value / membershipDailyLimitSeconds.value) * 100))
})

const hasUsableBenefit = computed(() => {
  return membershipDailyRemainingSeconds.value > 0 || balanceSeconds.value > 0
})

function parseDateValue(value?: string | number | Date | null) {
  if (value instanceof Date)
    return value

  if (typeof value === 'number')
    return new Date(value)

  if (!value)
    return new Date(NaN)

  const raw = String(value).trim()
  const normalized = /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(raw)
    ? raw.replace(' ', 'T')
    : raw

  return new Date(normalized)
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

async function fetchBenefits() {
  try {
    loading.value = true
    const benefit: any = await getMyBenefits()
    membershipActive.value = benefit?.membershipActive === true
    membershipStartAt.value = benefit?.membershipStartAt || ''
    membershipEndAt.value = benefit?.membershipEndAt || ''
    membershipDailyLimitSeconds.value = Number(benefit?.membershipDailyLimitSeconds) || 0
    membershipDailyConsumedSeconds.value = Number(benefit?.membershipDailyConsumedSeconds) || 0
    membershipDailyRemainingSeconds.value = Number(benefit?.membershipDailyRemainingSeconds) || 0
    balanceSeconds.value = Number(benefit?.balanceSeconds) || 0
  }
  catch (error: any) {
    toast.error(error?.message || '获取权益失败')
    membershipActive.value = false
    membershipStartAt.value = ''
    membershipEndAt.value = ''
    membershipDailyLimitSeconds.value = 0
    membershipDailyConsumedSeconds.value = 0
    membershipDailyRemainingSeconds.value = 0
    balanceSeconds.value = 0
  }
  finally {
    loading.value = false
  }
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
    await fetchBenefits()
    emit('benefitsChanged')
  }
  catch (error: any) {
    toast.error(error?.message || '兑换失败')
  }
  finally {
    redeemLoading.value = false
  }
}

onShow(() => {
  void fetchBenefits()
})

onMounted(() => {
  void fetchBenefits()
})
</script>

<template>
  <view class="page">
    <scroll-view scroll-y class="page-scroll" enable-back-to-top>
      <view class="page-body">
        <view class="benefit-card">
          <view class="card-title">
            账户权益
          </view>
          <view class="benefit-grid">
            <view class="benefit-item">
              <view class="benefit-label">
                月卡状态
              </view>
              <view class="benefit-value" :class="{ 'benefit-value--active': membershipActive }">
                {{ membershipActive ? '生效中' : '无有效月卡' }}
              </view>
            </view>
            <view class="benefit-item">
              <view class="benefit-label">
                点卡余额
              </view>
              <view class="benefit-value">
                {{ formatBenefitDuration(balanceSeconds) }}
              </view>
            </view>
            <view class="benefit-item benefit-item--wide">
              <view class="benefit-label">
                月卡今日余额
              </view>
              <view class="benefit-value">
                {{ membershipActive ? formatBenefitDuration(membershipDailyRemainingSeconds) : '-' }}
              </view>
              <view v-if="membershipActive" class="quota-wrap">
                <view class="quota-line">
                  <text>已用 {{ formatBenefitDuration(membershipDailyConsumedSeconds) }}</text>
                  <text>/ {{ formatBenefitDuration(membershipDailyLimitSeconds) }}</text>
                </view>
                <view class="quota-track">
                  <view class="quota-bar" :style="{ width: `${monthlyQuotaPercent}%` }" />
                </view>
              </view>
            </view>
            <view class="benefit-item benefit-item--wide">
              <view class="benefit-label">
                月卡有效期
              </view>
              <view class="benefit-value benefit-value--time">
                {{ membershipActive ? `${formatAbsoluteTime(membershipStartAt)} 至 ${formatAbsoluteTime(membershipEndAt)}` : '-' }}
              </view>
            </view>
          </view>
          <view class="benefit-hint" :class="{ 'benefit-hint--warning': !hasUsableBenefit }">
            {{ hasUsableBenefit ? '启动导播时会优先消耗月卡今日额度，额度用完后消耗点卡余额。' : '当前没有可用时长，请先兑换点卡或月卡。' }}
          </view>
        </view>

        <view class="redeem-card">
          <view class="card-title">
            兑换激活码
          </view>
          <view class="redeem-desc">
            兑换后的权益属于当前账号，可用于启动导播。
          </view>
          <view class="redeem-row">
            <input
              v-model.trim="redeemCode"
              class="redeem-input"
              placeholder="输入激活码"
              @confirm="handleRedeem"
            >
            <view class="redeem-btn" @click="handleRedeem">
              {{ redeemLoading ? '兑换中...' : '兑换' }}
            </view>
          </view>
        </view>

        <view v-if="loading" class="loading-text">
          正在刷新权益信息...
        </view>
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

.benefit-card,
.redeem-card {
  margin-bottom: 24rpx;
  padding: 28rpx;
  border-radius: 28rpx;
  background: #ffffff;
  box-shadow: 0 10rpx 30rpx rgba(48, 74, 138, 0.08);
}

.card-title {
  margin-bottom: 22rpx;
  font-size: 32rpx;
  font-weight: 700;
  color: #20283a;
}

.benefit-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18rpx;
}

.benefit-item {
  min-height: 112rpx;
  padding: 22rpx;
  border-radius: 20rpx;
  background: #f7faff;
  box-sizing: border-box;
}

.benefit-item--wide {
  grid-column: 1 / -1;
}

.benefit-label {
  margin-bottom: 12rpx;
  font-size: 22rpx;
  color: #8b94a8;
}

.benefit-value {
  font-size: 28rpx;
  font-weight: 700;
  color: #273042;
  word-break: break-all;
}

.benefit-value--time {
  font-size: 24rpx;
  line-height: 1.5;
}

.benefit-value--active {
  color: #4ba863;
}

.quota-wrap {
  margin-top: 18rpx;
}

.quota-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 10rpx;
  font-size: 22rpx;
  color: #8b94a8;
}

.quota-track {
  height: 12rpx;
  border-radius: 999rpx;
  overflow: hidden;
  background: #e9eef8;
}

.quota-bar {
  height: 100%;
  border-radius: 999rpx;
  background: linear-gradient(90deg, #69c587 0%, #5d90ea 100%);
}

.benefit-hint {
  margin-top: 22rpx;
  padding: 18rpx 20rpx;
  border-radius: 18rpx;
  background: #f5f8ff;
  color: #627089;
  font-size: 23rpx;
  line-height: 1.6;
}

.benefit-hint--warning {
  background: #fff5f5;
  color: #d34f4f;
}

.redeem-desc {
  margin-bottom: 20rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: #7a8498;
}

.redeem-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.redeem-input {
  flex: 1;
  min-width: 0;
  height: 74rpx;
  padding: 0 22rpx;
  border: 2rpx solid #e5eaf3;
  border-radius: 16rpx;
  background: #fff;
  color: #1f2430;
  font-size: 24rpx;
  box-sizing: border-box;
}

.redeem-btn {
  min-width: 136rpx;
  height: 74rpx;
  padding: 0 24rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #5d90ea;
  color: #fff;
  font-size: 24rpx;
  font-weight: 700;
  flex-shrink: 0;
}

.loading-text {
  padding: 20rpx 0;
  text-align: center;
  color: #9aa2b3;
  font-size: 23rpx;
}
</style>
