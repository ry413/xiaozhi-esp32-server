import { http } from '@/http/request/alova'

export function getLivePlanList(params: { page: number, limit: number }) {
  return http.Get<{ list: any[] }>(`/livePlan`, {
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

export function getLivePlanDetail(planNo: string) {
  return http.Get<any>(`/livePlan/${planNo}`, {
    meta: {
      ignoreAuth: false,
      toast: false,
    },
    cacheFor: {
      expire: 0,
    },
  })
}

export function addLivePlan(data: any) {
  return http.Post<string>(`/livePlan`, data, {
    meta: {
      ignoreAuth: false,
      toast: false,
    },
  })
}

export function updateLivePlan(planNo: string, data: any) {
  return http.Put(`/livePlan/${planNo}`, data, {
    meta: {
      ignoreAuth: false,
      toast: false,
    },
  })
}

export function deleteLivePlan(planNo: string) {
  return http.Delete(
    `/livePlan/${planNo}`,
    undefined,
    {
      meta: {
        ignoreAuth: false,
        toast: false,
      },
    },
  )
}

export function getDouyinRoomId(input: string) {
  const query = `input=${encodeURIComponent(input)}`
  return http.Get<string>(`/livePlan/douyinRoomId?${query}`, {
    meta: {
      ignoreAuth: false,
      toast: false,
      isExposeError: true,
    },
    cacheFor: {
      expire: 0,
    },
  })
}
