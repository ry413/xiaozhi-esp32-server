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
    page(params, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/agent/llm-report/page${buildQuery(params)}`)
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
                console.error('获取大模型调用记录失败:', err);
                RequestService.reAjaxFun(() => {
                    this.page(params, callback);
                });
            }).send();
    },
}
