import type { PageParam, PageResult } from '@/http/types'
import { http } from '@/http/http'

/** 邮件日志信息 */
export interface MailLog {
  id?: string
  userId?: string
  userType?: number
  templateId?: string
  templateCode?: string
  templateNickname?: string
  templateTitle?: string
  templateContent?: string
  templateParams?: Record<string, any>
  accountId?: string
  fromMail?: string
  toMails?: string[]
  ccMails?: string[]
  bccMails?: string[]
  sendStatus?: number
  sendTime?: string
  sendMessageId?: string
  sendException?: string
  createTime?: string
}

/** 获取邮件日志分页列表 */
export function getMailLogPage(params: PageParam) {
  return http.get<PageResult<MailLog>>('/system/mail-log/page', params)
}

/** 获取邮件日志详情 */
export function getMailLog(id: string) {
  return http.get<MailLog>(`/system/mail-log/get?id=${id}`)
}
