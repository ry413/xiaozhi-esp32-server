import type { PageData, VoiceCloneItem } from './types'
import { http } from '@/http/request/alova'
import { getEnvBaseUrl } from '@/utils'

export function getVoiceCloneList(params: { page: number, limit: number, name?: string }) {
  return http.Get<PageData<VoiceCloneItem>>('/voiceClone', {
    params,
    meta: {
      ignoreAuth: false,
      toast: false,
    },
    cacheFor: {
      expire: 0,
    },
  })
}

export function updateVoiceCloneName(id: string, name: string) {
  return http.Post('/voiceClone/updateName', { id, name }, {
    meta: {
      ignoreAuth: false,
      toast: true,
    },
  })
}

export function cloneVoiceAudio(cloneId: string) {
  return http.Post('/voiceClone/cloneAudio', { cloneId }, {
    meta: {
      ignoreAuth: false,
      toast: true,
    },
  })
}

export function getVoiceCloneAudioId(id: string) {
  return http.Post<string>(`/voiceClone/audio/${id}`, {}, {
    meta: {
      ignoreAuth: false,
      toast: false,
    },
  })
}

export function getVoiceClonePlayUrl(uuid: string) {
  return `${getEnvBaseUrl()}/voiceClone/play/${uuid}`
}

export function uploadVoiceCloneSample(params: {
  id: string
  filePath: string
  fileName?: string
}) {
  const authInfo = JSON.parse(uni.getStorageSync('token') || '{}')
  const header: Record<string, string> = {}
  if (authInfo.token) {
    header.Authorization = `Bearer ${authInfo.token}`
  }

  return new Promise<void>((resolve, reject) => {
    uni.uploadFile({
      url: `${getEnvBaseUrl()}/voiceClone/upload`,
      filePath: params.filePath,
      name: 'voiceFile',
      formData: {
        id: params.id,
      },
      header,
      success: (res) => {
        try {
          const data = JSON.parse(res.data || '{}')
          if (data.code === 0) {
            resolve()
            return
          }
          reject(new Error(data.msg || '上传音频失败'))
        }
        catch (error) {
          reject(error)
        }
      },
      fail: reject,
    })
  })
}
