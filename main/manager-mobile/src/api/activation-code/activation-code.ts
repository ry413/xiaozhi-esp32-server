import { http } from '@/http/request/alova'

export function redeemActivationCode(code: string) {
  return http.Post('/activation-code/redeem', { code }, {
    meta: {
      ignoreAuth: false,
      toast: false,
    },
  })
}

export function getMyBenefits() {
  return http.Get('/activation-code/me/benefits', {
    meta: {
      ignoreAuth: false,
      toast: false,
    },
    cacheFor: {
      expire: 0,
    },
  })
}
