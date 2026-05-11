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

export function clearLoginState() {
  uni.removeStorageSync('token')
  uni.removeStorageSync('userInfo')
}

export function clearInvalidLoginState() {
  const tokenValue = uni.getStorageSync('token')
  if (tokenValue && !hasValidStoredToken()) {
    clearLoginState()
  }
}
