import { getServiceUrl } from '../api';
import RequestService from '../httpRequest';

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
}
