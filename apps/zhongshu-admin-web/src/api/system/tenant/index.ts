import request from '@/config/axios'

export interface TenantVO {
  id: string
  name: string
  contactName: string
  contactMobile: string
  status: number
  domain: string
  packageId: string
  username: string
  password: string
  expireTime: Date
  accountCount: number
  websites: string[]
  createTime: Date
}

export interface TenantPageReqVO extends PageParam {
  name?: string
  contactName?: string
  contactMobile?: string
  status?: number
  createTime?: Date[]
}

export interface TenantExportReqVO {
  name?: string
  contactName?: string
  contactMobile?: string
  status?: number
  createTime?: Date[]
}

// 查询租户列表
export const getTenantPage = (params: TenantPageReqVO) => {
  return request.get({ url: '/system/tenant/page', params })
}

// 查询租户详情
export const getTenant = (id: string) => {
  return request.get({ url: '/system/tenant/get?id=' + id })
}

// 获取租户精简信息列表
export const getTenantList = () => {
  return request.get({ url: '/system/tenant/simple-list' })
}

// 新增租户
export const createTenant = (data: TenantVO) => {
  return request.post({ url: '/system/tenant/create', data })
}

// 修改租户
export const updateTenant = (data: TenantVO) => {
  return request.put({ url: '/system/tenant/update', data })
}

// 删除租户
export const deleteTenant = (id: string) => {
  return request.delete({ url: '/system/tenant/delete?id=' + id })
}

// 批量删除租户
export const deleteTenantList = (ids: string[]) => {
  return request.delete({ url: '/system/tenant/delete-list', params: { ids: ids.join(',') } })
}

// 导出租户
export const exportTenant = (params: TenantExportReqVO) => {
  return request.download({ url: '/system/tenant/export-excel', params })
}
