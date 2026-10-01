import request from '@/config/axios'

// 首链员工账号创建结果（ZS-FC-001，M5-A 初始密码下发载体）
export interface EmployeeCreatedRespVO {
  userId: number
  username: string
  initialPassword: string
}

// 本组织在职成员（ZS-FC-003 员工选择器数据源）
export interface EmployeeMemberVO {
  userId: number
  username: string
  nickname: string
}

// 创建员工账号（负责人直接创建；返回一次性初始密码）
export const createEmployee = (data: { username: string; nickname: string }) => {
  return request.post<EmployeeCreatedRespVO>({ url: '/firstchain/employee/create', data })
}

// 获得本组织在职成员列表（分配/改派选择器数据源；对象级资格服务端校验）
export const listOrgMembers = () => {
  return request.get<EmployeeMemberVO[]>({ url: '/firstchain/employee/list-members' })
}
