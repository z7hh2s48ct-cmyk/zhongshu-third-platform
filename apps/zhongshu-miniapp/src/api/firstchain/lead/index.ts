import type { PageParam, PageResult } from '@/http/types'
import { http } from '@/http/http'

/** 首链线索（ZS-FC-002；D-12 A 类字段经服务端统一裁决输出——F2 无授权时脱敏尾四位） */
export interface Lead {
  id: number
  leadKey: string
  customerName?: string
  customerPhone?: string
  customerWechat?: string
  customerAddress?: string
  source?: string
  orgId: number
  assigneeUserId?: number
  status: string
  version: number
  createTime?: string
  updateTime?: string
}

export interface LeadStatusMetrics {
  DISTRIBUTED?: number
  ASSIGNED?: number
  FOLLOWING?: number
  CONVERTED?: number
  INVALID?: number
}

export interface LeadPageReq extends PageParam {
  status?: string
}

/** 线索状态（D-07 M6 五态） */
export const LEAD_STATUS = {
  DISTRIBUTED: 'DISTRIBUTED',
  ASSIGNED: 'ASSIGNED',
  FOLLOWING: 'FOLLOWING',
  CONVERTED: 'CONVERTED',
  INVALID: 'INVALID',
} as const

/** 查询线索分页（视角服务端解析：平台=授权范围/负责人=本组织/员工=本人） */
export function getLeadPage(params: LeadPageReq) {
  return http.get<PageResult<Lead>>('/firstchain/lead/page', params)
}

/** 查询线索详情（越权显式拒绝） */
export function getLead(id: number) {
  return http.get<Lead>(`/firstchain/lead/get?id=${id}`)
}

/** 线索状态指标（三视角同源） */
export function getLeadStatusMetrics() {
  return http.get<LeadStatusMetrics>('/firstchain/lead/metrics')
}

/** 领取线索（员工领取分配给自己的线索） */
export function claimLead(data: { id: number, expectedVersion: number }) {
  return http.post<boolean>('/firstchain/lead/claim', data)
}

/** 追加跟进记录（仅被分配员工本人；时间取提交时刻） */
export function followupLead(data: { leadId: number, content: string, nextStep?: string, followupTime: number }) {
  return http.post<number>('/firstchain/lead/followup', data)
}

/** 分配线索（负责人分配本组织员工） */
export function assignLead(data: { id: number, assigneeUserId: number, expectedVersion: number }) {
  return http.post<boolean>('/firstchain/lead/assign', data)
}

/** 改派线索（负责人改派，状态不变推版本） */
export function reassignLead(data: { id: number, newAssigneeUserId: number, expectedVersion: number }) {
  return http.post<boolean>('/firstchain/lead/reassign', data)
}

/** 转商机（被分配员工发起） */
export function convertLead(data: { leadId: number, expectedVersion: number }) {
  return http.post<number>('/firstchain/lead/convert', data)
}

/** 无效关闭（员工发起或负责人代操作） */
export function invalidateLead(data: { leadId: number, reasonName: string, reasonDetail?: string, expectedVersion: number }) {
  return http.post<boolean>('/firstchain/lead/invalidate', data)
}
