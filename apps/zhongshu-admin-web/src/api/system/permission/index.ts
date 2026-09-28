import request from '@/config/axios'

export interface PermissionAssignUserRoleReqVO {
  userId: string
  roleIds: string[]
}

export interface PermissionAssignRoleMenuReqVO {
  roleId: string
  menuIds: string[]
}

export interface PermissionAssignRoleDataScopeReqVO {
  roleId: string
  dataScope: number
  dataScopeDeptIds: string[]
}

// 查询角色拥有的菜单权限
export const getRoleMenuList = async (roleId: string) => {
  return await request.get({ url: '/system/permission/list-role-menus?roleId=' + roleId })
}

// 赋予角色菜单权限
export const assignRoleMenu = async (data: PermissionAssignRoleMenuReqVO) => {
  return await request.post({ url: '/system/permission/assign-role-menu', data })
}

// 赋予角色数据权限
export const assignRoleDataScope = async (data: PermissionAssignRoleDataScopeReqVO) => {
  return await request.post({ url: '/system/permission/assign-role-data-scope', data })
}

// 查询用户拥有的角色数组
export const getUserRoleList = async (userId: string) => {
  return await request.get({ url: '/system/permission/list-user-roles?userId=' + userId })
}

// 赋予用户角色
export const assignUserRole = async (data: PermissionAssignUserRoleReqVO) => {
  return await request.post({ url: '/system/permission/assign-user-role', data })
}

// ZS-CLIENT-001.B：对象授权输出（允许动作 / 授权字段 / 脱敏字段）
export interface ObjectAuthorizationRespVO {
  allowedActions: string[]
  authorizedFields: string[] | null
  maskedFields: string[] | null
}

// 查询对象授权输出（null data = 未接入域；消费逻辑见 utils/objectAuthorization.ts）
export const getObjectAuthorization = async (params: {
  objectType: string
  orgId?: number
  ownerUserId?: number
}) => {
  return await request.get({ url: '/system/object-authorization/get', params })
}
