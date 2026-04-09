import { getServiceUrl } from '../api';
import RequestService from '../httpRequest';

export default {
    getLivePlanList(params, callback) {
        const queryParams = new URLSearchParams({
            page: params.page,
            limit: params.limit,
        }).toString();

        RequestService.sendRequest()
            .url(`${getServiceUrl()}/livePlan?${queryParams}`)
            .method('GET')
            .success((res) => {
                RequestService.clearRequestTime()
                callback(res)
            })
            .fail((res) => {
                RequestService.clearRequestTime()
                callback(res)
            })
            .networkFail((err) => {
                console.error('请求失败:', err)
                RequestService.reAjaxFun(() => {
                    this.getLivePlanList(params, callback)
                })
            }).send()
    },
    getLivePlanDetail(planNo, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/livePlan/${planNo}`)
            .method('GET')
            .success((res) => {
                RequestService.clearRequestTime()
                callback(res)
            })
            .fail((res) => {
                RequestService.clearRequestTime()
                callback(res)
            })
            .networkFail((err) => {
                console.error('请求失败:', err)
                RequestService.reAjaxFun(() => {
                    this.getLivePlanDetail(planNo, callback)
                })
            }).send()
    },
    addLivePlan(data, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/livePlan`)
            .method('POST')
            .data(data)
            .success((res) => {
                RequestService.clearRequestTime()
                callback(res)
            })
            .fail((res) => {
                RequestService.clearRequestTime()
                callback(res)
            })
            .networkFail((err) => {
                console.error('请求失败:', err)
                RequestService.reAjaxFun(() => {
                    this.addLivePlan(data, callback)
                })
            }).send()
    },
    updateLivePlan(planNo, data, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/livePlan/${planNo}`)
            .method('PUT')
            .data(data)
            .success((res) => {
                RequestService.clearRequestTime()
                callback(res)
            })
            .fail((res) => {
                RequestService.clearRequestTime()
                callback(res)
            })
            .networkFail((err) => {
                console.error('请求失败:', err)
                RequestService.reAjaxFun(() => {
                    this.updateLivePlan(planNo, data, callback)
                })
            }).send()
    },
    deleteLivePlan(planNo, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/livePlan/${planNo}`)
            .method('DELETE')
            .success((res) => {
                RequestService.clearRequestTime()
                callback(res)
            })
            .fail((res) => {
                RequestService.clearRequestTime()
                callback(res)
            })
            .networkFail((err) => {
                console.error('请求失败:', err)
                RequestService.reAjaxFun(() => {
                    this.deleteLivePlan(planNo, callback)
                })
            }).send()
    },
}
