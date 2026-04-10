import { getServiceUrl } from '../api';
import RequestService from '../httpRequest';

function buildQuery(params = {}) {
    const searchParams = new URLSearchParams();

    Object.entries(params).forEach(([key, value]) => {
        if (value === undefined || value === null || value === '') {
            return;
        }
        searchParams.append(key, value);
    });

    const query = searchParams.toString();
    return query ? `?${query}` : '';
}

export default {
    redeem(code, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/activation-code/redeem`)
            .method('POST')
            .data({ code })
            .success((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .fail((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .networkFail((err) => {
                console.error('兑换激活码失败:', err);
                RequestService.reAjaxFun(() => {
                    this.redeem(code, callback);
                });
            }).send();
    },
    getMyBenefits(callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/activation-code/me/benefits`)
            .method('GET')
            .success((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .fail((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .networkFail((err) => {
                console.error('获取我的权益摘要失败:', err);
                RequestService.reAjaxFun(() => {
                    this.getMyBenefits(callback);
                });
            }).send();
    },
    pageBatches(params, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/activation-code/admin/batches${buildQuery(params)}`)
            .method('GET')
            .success((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .fail((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .networkFail((err) => {
                console.error('获取激活码批次失败:', err);
                RequestService.reAjaxFun(() => {
                    this.pageBatches(params, callback);
                });
            }).send();
    },
    createBatch(data, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/activation-code/admin/batches`)
            .method('POST')
            .data(data)
            .success((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .fail((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .networkFail((err) => {
                console.error('创建激活码批次失败:', err);
                RequestService.reAjaxFun(() => {
                    this.createBatch(data, callback);
                });
            }).send();
    },
    updateBatchStatus(batchNo, status, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/activation-code/admin/batches/${batchNo}/status/${status}`)
            .method('PUT')
            .success((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .fail((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .networkFail((err) => {
                console.error('更新批次状态失败:', err);
                RequestService.reAjaxFun(() => {
                    this.updateBatchStatus(batchNo, status, callback);
                });
            }).send();
    },
    pageCodes(params, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/activation-code/admin/codes${buildQuery(params)}`)
            .method('GET')
            .success((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .fail((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .networkFail((err) => {
                console.error('获取激活码列表失败:', err);
                RequestService.reAjaxFun(() => {
                    this.pageCodes(params, callback);
                });
            }).send();
    },
    voidCode(code, reason, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/activation-code/admin/codes/${code}/void`)
            .method('PUT')
            .data({ reason })
            .success((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .fail((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .networkFail((err) => {
                console.error('作废激活码失败:', err);
                RequestService.reAjaxFun(() => {
                    this.voidCode(code, reason, callback);
                });
            }).send();
    },
}
