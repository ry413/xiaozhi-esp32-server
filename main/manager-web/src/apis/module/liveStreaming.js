import RequestService from '../httpRequest';

const liveStreamingApi = process.env.VUE_APP_LIVE_STREAMING_BASE_URL
// const liveStreamingApi = "http://localhost:18080" // 开发用, 生产环境得改成上面那个

export default {
    // 开始直播
    startLive(params, callback) {
        RequestService.sendRequest()
            .url(`${liveStreamingApi}/monitors`)
            .method('POST')
            .data(params)
            .success((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .fail((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .networkFail((err) => {
                console.error('开始直播失败:', err);
                RequestService.reAjaxFun(() => {
                    this.startLive(params, callback);
                });
            }).send();
    },
    // 结束直播
    stopLive(deviceID, callback) {
        RequestService.sendRequest()
            .url(`${liveStreamingApi}/monitors/device/${deviceID}`)
            .method('DELETE')
            .success((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .fail((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .networkFail((err) => {
                console.error('结束直播失败:', err);
                RequestService.reAjaxFun(() => {
                    this.stopLive(deviceID, callback);
                });
            }).send();
    },
    // 获取直播状态
    getLiveStatus(deviceId, callback) {
        RequestService.sendRequest()
            .url(`${liveStreamingApi}/monitors/device/${deviceId}`)
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
                console.error('获取直播状态失败:', err);
                RequestService.reAjaxFun(() => {
                    this.getLiveStatus(deviceId, callback);
                });
            }).send();
    },
    // 获取已发送的消息列表
    getSentMsg(deviceId, params, callback) {
        const queryParams = new URLSearchParams({
            after_id: params.afterId,
            limit: params.limit,
        }).toString();

        RequestService.sendRequest()
            .url(`${liveStreamingApi}/sent-prompts/device/${deviceId}?${queryParams}`)
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
                console.error('获取已发送消息列表失败:', err);
                RequestService.reAjaxFun(() => {
                    this.getSentMsg(deviceId, params, callback);
                });
            }).send();
    },
    // 发送手动指挥消息
    sendManualMsg(deviceId, params, callback) {
        RequestService.sendRequest()
            .url(`${liveStreamingApi}/monitors/manual/${deviceId}`)
            .method('POST')
            .data(params)
            .success((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .fail((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .networkFail((err) => {
                console.error('发送手动消息失败:', err);
                RequestService.reAjaxFun(() => {
                    this.sendManualMsg(deviceId, params, callback);
                });
            }).send();
    }
}
