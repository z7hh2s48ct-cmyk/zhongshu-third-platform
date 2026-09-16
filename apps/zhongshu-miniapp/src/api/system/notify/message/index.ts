import type { PageParam, PageResult } from '@/http/types'
import { http } from '@/http/http'

/** 站内信消息信息 */
export interface NotifyMessage {
  id: number
  userId: number
  userType: number
  templateId: number
  templateCode: string
  templateNickname: string
  templateContent: string
  templateType: number
  templateParams: Record<string, any>
  readStatus: boolean
  readTime: Date
  createTime?: Date
}

/** 查询站内信消息列表 */
export function getNotifyMessagePage(params: PageParam) {
  return http.get<PageResult<NotifyMessage>>('/system/notify-message/page', params)
}

/** 查询站内信消息详情 */
export function getNotifyMessage(id: number) {
  return http.get<NotifyMessage>(`/system/notify-message/get`, { id })
}

/** 获取我的站内信分页 */
export function getMyNotifyMessagePage(params: PageParam) {
  return http.get<PageResult<NotifyMessage>>('/system/notify-message/my-page', params)
}

/** ZS-MSG-003：消息落点描述（纯结构性业务引用，不含消息正文） */
export interface NotifyMessageLandingDescriptor {
  module: string
  route: string
  params?: Record<string, any>
}

/** ZS-MSG-003：消息落点解析结果（available=false 时以 unavailableCode/reason 给出明确不可用原因） */
export interface NotifyMessageLandingResult {
  available: boolean
  unavailableCode?: 'NOT_REGISTERED' | 'MODULE_DISABLED' | 'REVOKED' | 'CLIENT_UNSUPPORTED'
  reason?: string
  descriptor?: NotifyMessageLandingDescriptor
}

/**
 * ZS-MSG-003：解析站内信落点（跳转二次授权：归属 → 注册 → 模块 → 业务重授权，服务端统一裁决）。
 * 刻意不提供「我的消息按 ID 取详情」接口：个人收件箱数据一律来自 my-page 列表；
 * 按 ID 取详情属管理面（/system/notify-message/get + system:notify-message:query），不得混用。
 */
export function resolveNotifyMessageLanding(id: number, client: 'WEB' | 'MOBILE') {
  return http.get<NotifyMessageLandingResult>('/system/notify-message/get-landing', { id, client })
}

/** 批量标记站内信已读 */
export function updateNotifyMessageRead(ids: number | number[]) {
  const idsArray = Array.isArray(ids) ? ids : [ids]
  return http.put<boolean>('/system/notify-message/update-read', undefined, { ids: idsArray })
}

/** 标记所有站内信为已读 */
export function updateAllNotifyMessageRead() {
  return http.put<boolean>('/system/notify-message/update-all-read')
}

/** 获取当前用户的未读站内信数量 */
export function getUnreadNotifyMessageCount() {
  return http.get<number>('/system/notify-message/get-unread-count')
}

/** 获取当前用户的最新站内信列表 */
export function getUnreadNotifyMessageList() {
  return http.get<NotifyMessage[]>('/system/notify-message/get-unread-list')
}
