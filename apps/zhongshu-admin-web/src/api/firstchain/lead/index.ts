import request from '@/config/axios'

// 首链线索 VO（ZS-FC-002；D-12 A 类字段经服务端统一裁决输出——F2 无授权时脱敏尾四位）
export interface LeadVO {
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
  createTime?: Date
  updateTime?: Date
}

export interface LeadPageReqVO extends PageParam {
  status?: string
}

// 线索状态指标（三视角同源：权威状态列 GROUP BY，视角服务端解析）
export interface LeadStatusMetrics {
  DISTRIBUTED?: number
  ASSIGNED?: number
  FOLLOWING?: number
  CONVERTED?: number
  INVALID?: number
}

// 查询线索分页（视角服务端解析：平台=授权范围/负责人=本组织/员工=本人）
export const getLeadPage = (params: LeadPageReqVO) => {
  return request.get({ url: '/firstchain/lead/page', params })
}

// 查询线索详情
export const getLead = (id: number) => {
  return request.get({ url: '/firstchain/lead/get?id=' + id })
}

// 线索状态指标
export const getLeadStatusMetrics = () => {
  return request.get<LeadStatusMetrics>({ url: '/firstchain/lead/metrics' })
}

// 下发线索（平台运营；线索编号服务端生成，归属组织服务端写入）
export const distributeLead = (data: {
  customerName: string
  customerPhone?: string
  customerWechat?: string
  customerAddress?: string
  source?: string
  orgId: number
}) => {
  return request.post<number>({ url: '/firstchain/lead/distribute', data })
}

// 分配线索（负责人分配本组织员工）
export const assignLead = (data: { id: number; assigneeUserId: number; expectedVersion: number }) => {
  return request.post<boolean>({ url: '/firstchain/lead/assign', data })
}

// 领取线索（员工领取分配给自己的线索）
export const claimLead = (data: { id: number; expectedVersion: number }) => {
  return request.post<boolean>({ url: '/firstchain/lead/claim', data })
}

// 改派线索（负责人改派，状态不变推版本）
export const reassignLead = (data: { id: number; newAssigneeUserId: number; expectedVersion: number }) => {
  return request.post<boolean>({ url: '/firstchain/lead/reassign', data })
}

// 追加跟进记录（仅被分配员工本人）
export const followupLead = (data: {
  leadId: number
  content: string
  nextStep?: string
  followupTime: Date
}) => {
  return request.post<number>({ url: '/firstchain/lead/followup', data })
}

// 转商机（被分配员工发起；商机编号服务端生成）
export const convertLead = (data: { leadId: number; expectedVersion: number }) => {
  return request.post<number>({ url: '/firstchain/lead/convert', data })
}

// 无效关闭（员工发起或负责人代操作；OTHER 时说明必填）
export const invalidateLead = (data: {
  leadId: number
  reasonName: string
  reasonDetail?: string
  expectedVersion: number
}) => {
  return request.post<boolean>({ url: '/firstchain/lead/invalidate', data })
}
