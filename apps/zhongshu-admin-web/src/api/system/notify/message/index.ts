import request from '@/config/axios'
import qs from 'qs'

export interface NotifyMessageVO {
  id: string
  userId: string
  userType: number
  templateId: string
  templateCode: string
  templateNickname: string
  templateContent: string
  templateType: number
  templateParams: string
  readStatus: boolean
  readTime: Date
  createTime: Date
}

// 查询站内信消息列表
export const getNotifyMessagePage = async (params: PageParam) => {
  return await request.get({ url: '/system/notify-message/page', params })
}

// 获得我的站内信分页
export const getMyNotifyMessagePage = async (params: PageParam) => {
  return await request.get({ url: '/system/notify-message/my-page', params })
}

// 批量标记已读
export const updateNotifyMessageRead = async (ids) => {
  return await request.put({
    url: '/system/notify-message/update-read?' + qs.stringify({ ids: ids }, { indices: false })
  })
}

// 标记所有站内信为已读
export const updateAllNotifyMessageRead = async () => {
  return await request.put({ url: '/system/notify-message/update-all-read' })
}

// 获取当前用户的最新站内信列表
export const getUnreadNotifyMessageList = async () => {
  return await request.get({ url: '/system/notify-message/get-unread-list' })
}

// 获得当前用户的未读站内信数量
export const getUnreadNotifyMessageCount = async () => {
  return await request.get({ url: '/system/notify-message/get-unread-count' })
}

// ZS-MSG-003：消息落点描述（纯结构性业务引用，不含消息正文）
export interface NotifyMessageLandingDescriptor {
  module: string
  route: string
  params?: Record<string, any>
}

// ZS-MSG-003：消息落点解析结果（available=false 时以 unavailableCode/reason 给出明确不可用原因）
export interface NotifyMessageLandingResult {
  available: boolean
  unavailableCode?: 'NOT_REGISTERED' | 'MODULE_DISABLED' | 'REVOKED' | 'CLIENT_UNSUPPORTED'
  reason?: string
  descriptor?: NotifyMessageLandingDescriptor
}

// ZS-MSG-003：解析站内信落点（跳转二次授权：归属 → 注册 → 模块 → 业务重授权，服务端统一裁决）
export const resolveNotifyMessageLanding = async (
  id: string,
  client: 'WEB' | 'MOBILE'
): Promise<NotifyMessageLandingResult> => {
  return await request.get({ url: '/system/notify-message/get-landing', params: { id, client } })
}
