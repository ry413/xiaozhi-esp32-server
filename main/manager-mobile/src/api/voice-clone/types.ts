export interface VoiceCloneItem {
  id: string
  name: string
  modelId: string
  modelName: string
  voiceId: string
  languages: string
  userId: string
  userName: string
  trainStatus: number
  trainError?: string | null
  createDate: string
  hasVoice: boolean
  voiceSourceUrl?: string | null
}

export interface PageData<T> {
  list: T[]
  total: number
}
