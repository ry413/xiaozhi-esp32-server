import { onShareAppMessage, onShareTimeline, onShow } from '@dcloudio/uni-app'

interface AppShareOptions {
  title?: string
  path?: string
  imageUrl?: string
}

const defaultShareOptions: Required<Pick<AppShareOptions, 'title' | 'path'>> & Pick<AppShareOptions, 'imageUrl'> = {
  title: import.meta.env.VITE_APP_TITLE || '小智',
  path: '/pages/index/index',
}

function buildShareOptions(options: AppShareOptions = {}) {
  return {
    ...defaultShareOptions,
    ...options,
  }
}

function getTimelineQuery(path: string) {
  const query = path.split('?')[1]
  return query || ''
}

export function useAppShare(options: AppShareOptions = {}) {
  const shareOptions = () => buildShareOptions(options)

  // #ifdef MP-WEIXIN
  onShow(() => {
    uni.showShareMenu({
      withShareTicket: true,
      menus: ['shareAppMessage', 'shareTimeline'],
    })
  })
  // #endif

  onShareAppMessage(() => shareOptions())

  onShareTimeline(() => {
    const current = shareOptions()
    return {
      title: current.title,
      query: getTimelineQuery(current.path),
      imageUrl: current.imageUrl,
    }
  })
}
