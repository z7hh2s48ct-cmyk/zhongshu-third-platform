import request from '@/config/axios'

// 首链加盟商申请 VO（ZS-FC-001；与后端 ApplicationRespVO 对齐）
export interface ApplicationVO {
  id: number
  appKey: string
  applicantName: string
  contactName?: string
  contactPhone?: string
  attachmentFileIds?: number[]
  rejectReason?: string
  status: string
  version: number
  createTime?: Date
  updateTime?: Date
}

export interface ApplicationPageReqVO extends PageParam {
  status?: string
}

// 审批开通结果（M5-A：initialPassword 仅本次实际建账时一次性下发）
export interface ApplicationOpeningRespVO {
  organizationId: number
  initialPassword?: string
}

// 查询申请分页（视角范围服务端解析）
export const getApplicationPage = (params: ApplicationPageReqVO) => {
  return request.get({ url: '/firstchain/application/page', params })
}

// 查询申请详情
export const getApplication = (id: number) => {
  return request.get({ url: '/firstchain/application/get?id=' + id })
}

// 审批通过并幂等开通（返回一次性初始密码，重复处理为空）
export const approveApplication = (data: { appKey: string; reason?: string }) => {
  return request.post<ApplicationOpeningRespVO>({ url: '/firstchain/application/approve', data })
}

// 审批拒绝（意见必填，不建任何主体）
export const rejectApplication = (data: { appKey: string; reason: string }) => {
  return request.post({ url: '/firstchain/application/reject', data })
}

// 撤回审批流
export const withdrawApplication = (data: { id: number; expectedVersion: number }) => {
  return request.post({ url: '/firstchain/application/withdraw', data })
}
