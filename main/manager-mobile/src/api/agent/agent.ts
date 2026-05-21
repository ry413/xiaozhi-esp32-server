import type {
  Agent,
  AgentCreateData,
  AgentDetail,
  ModelOption,
  RoleTemplate,
} from './types'
import { http } from '@/http/request/alova'
import { clearLoginState, getStoredAuthInfo, isValidToken, markAuthExpired } from '@/utils/auth'
import { getEnvBaseUrl } from '@/utils'

// 获取智能体详情
export function getAgentDetail(id: string) {
  return http.Get<AgentDetail>(`/agent/${id}`, {
    meta: {
      ignoreAuth: false,
      toast: false,
    },
    cacheFor: {
      expire: 0,
    },
  })
}

// 获取角色模板列表
export function getRoleTemplates() {
  return http.Get<RoleTemplate[]>('/agent/template', {
    meta: {
      ignoreAuth: false,
      toast: false,
    },
    cacheFor: {
      expire: 0,
    },
  })
}

// 获取模型选项
export function getModelOptions(modelType: string, modelName: string = '') {
  return http.Get<ModelOption[]>('/models/names', {
    params: {
      modelType,
      modelName,
    },
    meta: {
      ignoreAuth: false,
      toast: false,
    },
    cacheFor: {
      expire: 0,
    },
  })
}

// 获取智能体列表
export function getAgentList() {
  return http.Get<Agent[]>('/agent/list', {
    meta: {
      ignoreAuth: false,
      toast: false,
    },
    cacheFor: {
      expire: 0,
    },
  })
}

// 创建智能体
export function createAgent(data: AgentCreateData) {
  return http.Post<string>('/agent', data, {
    meta: {
      ignoreAuth: false,
      toast: true,
    },
  })
}

// 删除智能体
export function deleteAgent(id: string) {
  return http.Delete(`/agent/${id}`, {
    meta: {
      ignoreAuth: false,
      toast: true,
    },
  })
}

// 获取TTS音色列表
export function getTTSVoices(ttsModelId: string, voiceName: string = '') {
  return http.Get<{ id: string, name: string }[]>(`/models/${ttsModelId}/voices`, {
    params: {
      voiceName,
    },
    meta: {
      ignoreAuth: false,
      toast: false,
    },
    cacheFor: {
      expire: 0,
    },
  })
}

// 更新智能体
export function updateAgent(id: string, data: Partial<AgentDetail>) {
  return http.Put(`/agent/${id}`, data, {
    meta: {
      ignoreAuth: false,
      toast: true,
    },
    cacheFor: {
      expire: 0,
    },
  })
}

export interface AgentScriptOptimizeResult {
  business_prompt: string
  readiness?: 'insufficient' | 'usable_but_thin' | 'ready'
  missing_info_hint?: string
}

function normalizeScriptTemplateList(data: any) {
  let result = data
  if (typeof result === 'string') {
    try {
      result = JSON.parse(result)
    }
    catch {
      result = [result]
    }
  }

  if (Array.isArray(result))
    return result.map(item => String(item)).filter(Boolean)

  if (Array.isArray(result?.templates))
    return result.templates.map((item: any) => String(item)).filter(Boolean)

  return []
}

// 优化角色提示词
export function optimizePrompt(prompt: string) {
  return new Promise<AgentScriptOptimizeResult>((resolve, reject) => {
    const authInfo = getStoredAuthInfo()
    if (!isValidToken(authInfo.token)) {
      clearLoginState()
      reject(new Error('未登录'))
      return
    }
    uni.request({
      url: `${getEnvBaseUrl()}/agent/optimize-prompt`,
      method: 'POST',
      timeout: 120000,
      header: {
        'Content-Type': 'application/json',
        'Accept': 'application/json, text/plain, */*',
        'Authorization': `Bearer ${authInfo.token}`,
      },
      data: { prompt },
      success: (response) => {
        const body = response.data as any
        if (response.statusCode === 401 || body?.code === 401) {
          markAuthExpired()
          clearLoginState()
          uni.reLaunch({ url: '/pages-sub/login/index' })
          reject(new Error(body?.msg || '登录已过期'))
          return
        }
        if (response.statusCode !== 200) {
          reject(new Error(`HTTP ${response.statusCode}: ${JSON.stringify(body)}`))
          return
        }
        if (!body || body.code !== 0) {
          reject(new Error(body?.msg || `接口返回异常: ${JSON.stringify(body)}`))
          return
        }
        let result = body.data
        if (typeof result === 'string') {
          try {
            result = JSON.parse(result)
          }
          catch {
            result = { business_prompt: result }
          }
        }
        resolve({
          business_prompt: String(result?.business_prompt || ''),
          readiness: result?.readiness,
          missing_info_hint: result?.missing_info_hint,
        })
      },
      fail: reject,
    })
  })
}

// 生成卖货话术模板
export function generateAgentScript(prompt: string) {
  return new Promise<string[]>((resolve, reject) => {
    const authInfo = getStoredAuthInfo()
    if (!isValidToken(authInfo.token)) {
      clearLoginState()
      reject(new Error('未登录'))
      return
    }
    uni.request({
      url: `${getEnvBaseUrl()}/agent/generate-script`,
      method: 'POST',
      timeout: 120000,
      header: {
        'Content-Type': 'application/json',
        'Accept': 'application/json, text/plain, */*',
        'Authorization': `Bearer ${authInfo.token}`,
      },
      data: { prompt },
      success: (response) => {
        const body = response.data as any
        if (response.statusCode === 401 || body?.code === 401) {
          markAuthExpired()
          clearLoginState()
          uni.reLaunch({ url: '/pages-sub/login/index' })
          reject(new Error(body?.msg || '登录已过期'))
          return
        }
        if (response.statusCode !== 200) {
          reject(new Error(`HTTP ${response.statusCode}: ${JSON.stringify(body)}`))
          return
        }
        if (!body || body.code !== 0) {
          reject(new Error(body?.msg || `接口返回异常: ${JSON.stringify(body)}`))
          return
        }
        resolve(normalizeScriptTemplateList(body.data))
      },
      fail: reject,
    })
  })
}

// 获取插件列表
export function getPluginFunctions() {
  return new Promise<any[]>((resolve, reject) => {
    const authInfo = getStoredAuthInfo()
    if (!isValidToken(authInfo.token)) {
      clearLoginState()
      reject(new Error('未登录'))
      return
    }
    uni.request({
      url: `${getEnvBaseUrl()}/models/provider/plugin/names`,
      method: 'GET',
      header: {
        Accept: 'application/json, text/plain, */*',
        Authorization: `Bearer ${authInfo.token}`,
      },
      success: (response) => {
        let body = response.data as any
        if (typeof body === 'string') {
          try {
            body = JSON.parse(body)
          }
          catch (error) {
            reject(new Error(`插件列表响应解析失败: ${body}`))
            return
          }
        }
        console.log('插件列表接口返回:', body)
        if (response.statusCode === 401 || body?.code === 401) {
          markAuthExpired()
          clearLoginState()
          uni.reLaunch({ url: '/pages-sub/login/index' })
          reject(new Error(body?.msg || '登录已过期'))
          return
        }
        if (response.statusCode !== 200) {
          reject(new Error(`HTTP ${response.statusCode}: ${JSON.stringify(body)}`))
          return
        }
        if (!body || body.code !== 0) {
          reject(new Error(body?.msg || `接口返回异常: ${JSON.stringify(body)}`))
          return
        }
        resolve(Array.isArray(body.data) ? body.data : [])
      },
      fail: reject,
    })
  })
}

// 获取mcp接入点
export function getMcpAddress(agentId: string) {
  return http.Get<string>(`/agent/mcp/address/${agentId}`, {
    meta: {
      ignoreAuth: false,
      toast: false,
      isExposeError: true,
    },
  })
}

// 获取mcp工具
export function getMcpTools(agentId: string) {
  return http.Get<string[]>(`/agent/mcp/tools/${agentId}`, {
    meta: {
      ignoreAuth: false,
      toast: false,
    },
    cacheFor: {
      expire: 0,
    },
  })
}

// 获取声纹列表
export function getVoicePrintList(agentId: string) {
  return http.Get<any[]>(`/agent/voice-print/list/${agentId}`, {
    meta: {
      ignoreAuth: false,
      toast: false,
    },
    cacheFor: {
      expire: 0,
    },
  })
}

// 获取语音对话记录
export function getChatHistoryUser(agentId: string) {
  return http.Get<any[]>(`/agent/${agentId}/chat-history/user`, {
    meta: {
      ignoreAuth: false,
      toast: false,
    },
    cacheFor: {
      expire: 0,
    },
  })
}

// 新增声纹说话人
export function createVoicePrint(data: { agentId: string, audioId: string, sourceName: string, introduce: string }) {
  return http.Post('/agent/voice-print', data, {
    meta: {
      ignoreAuth: false,
      toast: true,
    },
  })
}

// 获取智能体标签
export function getAgentTags(agentId: string) {
  return http.Get<any[]>(`/agent/${agentId}/tags`, {
    meta: {
      ignoreAuth: false,
      toast: false,
    },
    cacheFor: {
      expire: 0,
    },
  })
}

// 更新智能体标签
export function updateAgentTags(agentId: string, data) {
  return http.Put(`/agent/${agentId}/tags`, data, {
    meta: {
      ignoreAuth: false,
      isExposeError: true,
    },
  })
}

// 获取所有语言
export function getAllLanguage(modelId: string) {
  return http.Get<{ id: string, name: string, languages: string }[]>(`/models/${modelId}/voices`, {
    meta: {
      ignoreAuth: false,
      toast: false,
    },
    cacheFor: {
      expire: 0,
    },
  })
}
