const AUTH_EXPIRED_KEY = 'auth_expired'

export function isValidToken(token: unknown): token is string {
  return typeof token === 'string' && token.trim() !== '' && token !== 'undefined' && token !== 'null'
}

export function getStoredAuthInfo() {
  try {
    return JSON.parse(uni.getStorageSync('token') || '{}')
  }
  catch (error) {
    console.error('解析本地 token 失败:', error)
    return {}
  }
}

export function hasValidStoredToken() {
  return isValidToken(getStoredAuthInfo()?.token)
}

export function isGuestMode() {
  clearInvalidLoginState()
  return !hasValidStoredToken()
}

export function goToLogin() {
  uni.navigateTo({ url: '/pages-sub/login/index' })
}

export function markAuthExpired() {
  uni.setStorageSync(AUTH_EXPIRED_KEY, '1')
}

export function clearAuthExpired() {
  uni.removeStorageSync(AUTH_EXPIRED_KEY)
}

export function hasAuthExpired() {
  return uni.getStorageSync(AUTH_EXPIRED_KEY) === '1'
}

export function redirectToLoginIfAuthExpired() {
  if (!hasAuthExpired()) {
    return false
  }

  uni.reLaunch({ url: '/pages-sub/login/index' })
  return true
}

export function promptLogin(content = '登录后可使用完整功能') {
  if (!isGuestMode()) {
    return true
  }

  uni.showModal({
    title: '需要登录',
    content,
    confirmText: '去登录',
    cancelText: '先看看',
    success: (res) => {
      if (res.confirm) {
        goToLogin()
      }
    },
  })
  return false
}

export function clearLoginState() {
  uni.removeStorageSync('token')
  uni.removeStorageSync('userInfo')
}

export function clearInvalidLoginState() {
  const tokenValue = uni.getStorageSync('token')
  const userInfoValue = uni.getStorageSync('userInfo')
  if ((tokenValue || userInfoValue) && !hasValidStoredToken()) {
    clearLoginState()
  }
}
