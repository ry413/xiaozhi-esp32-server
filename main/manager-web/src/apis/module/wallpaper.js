import { getServiceUrl } from '../api';
import RequestService from '../httpRequest';

export default {
  // 获取当前用户可用的壁纸列表
  getWallpaperList(callback) {
    RequestService.sendRequest()
      .url(`${getServiceUrl()}/wallpaper/list`)
      .method('GET')
      .success((res) => {
        RequestService.clearRequestTime();
        callback(res);
      })
      .networkFail((err) => {
        console.error('获取壁纸列表失败:', err);
        RequestService.reAjaxFun(() => {
          this.getWallpaperList(callback);
        });
      })
      .send();
  },

  // // 按id数组批量获取壁纸
  // getWallpaperListByIds(callback) {
  //   RequestService.sendRequest()
  //     .url(`${getServiceUrl()}/wallpaper/list`)
  //     .method('GET')
  //     .success((res) => {
  //       RequestService.clearRequestTime();
  //       callback(res);
  //     })
  //     .networkFail((err) => {
  //       console.error('获取壁纸列表失败:', err);
  //       RequestService.reAjaxFun(() => {
  //         this.getWallpaperList(callback);
  //       });
  //     })
  //     .send();
  // },

  // 删除壁纸
  deleteWallpaper(id, callback) {
    RequestService.sendRequest()
      .url(`${getServiceUrl()}/wallpaper/${id}`)
      .method('DELETE')
      .success((res) => {
        RequestService.clearRequestTime();
        callback(res);
      })
      .networkFail((err) => {
        console.error('删除壁纸失败:', err);
        RequestService.reAjaxFun(() => {
          this.deleteWallpaper(id, callback);
        });
      })
      .send();
  },

  // 上传壁纸
  uploadWallpaper(file, name, callback) {
    const formData = new FormData();
    formData.append('file', file);
    RequestService.sendRequest()
      .url(`${getServiceUrl()}/wallpaper/upload`)
      .method('POST')
      .data(formData)
      .success((res) => {
        RequestService.clearRequestTime();
        callback(res);
      })
      .networkFail((err) => {
        console.error('上传壁纸失败:', err);
        RequestService.reAjaxFun(() => {
          this.uploadWallpaper(file, name, callback);
        });
      })
      .send();
  },
}