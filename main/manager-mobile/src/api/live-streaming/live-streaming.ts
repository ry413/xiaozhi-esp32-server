import { http } from '@/http/request/alova'

function getLiveStreamingBaseUrl() {
  return import.meta.env.VITE_LIVE_STREAMING_BASEURL || 'http://127.0.0.1:18080'
}

export function startLive(data: {
  platform: string
  live_id: string
  device_id: string
  config_json: Record<string, any>
}) {
  return http.Post('/monitors', data, {
    meta: {
      ignoreAuth: false,
      toast: false,
      domain: getLiveStreamingBaseUrl(),
    },
  })
}

export function stopLive(deviceId: string) {
  return http.Delete(
    `/monitors/device/${deviceId}`,
    undefined,
    {
      meta: {
        ignoreAuth: false,
        toast: false,
        domain: getLiveStreamingBaseUrl(),
      },
    },
  )
}

export function getLiveStatus(deviceId: string) {
  return http.Get(`/monitors/device/${deviceId}`, {
    meta: {
      ignoreAuth: false,
      toast: false,
      domain: getLiveStreamingBaseUrl(),
    },
    cacheFor: {
      expire: 0,
    },
  })
}

export function getSentMsg(deviceId: string, params: { afterId: number, limit: number }) {
  return http.Get(`/sent-prompts/device/${deviceId}`, {
    params: {
      after_id: params.afterId,
      limit: params.limit,
    },
    meta: {
      ignoreAuth: false,
      toast: false,
      domain: getLiveStreamingBaseUrl(),
    },
    cacheFor: {
      expire: 0,
    },
  })
}

export function sendManualMsg(deviceId: string, data: { message: string }) {
  return http.Post(`/monitors/manual/${deviceId}`, data, {
    meta: {
      ignoreAuth: false,
      toast: false,
      domain: getLiveStreamingBaseUrl(),
    },
  })
}
