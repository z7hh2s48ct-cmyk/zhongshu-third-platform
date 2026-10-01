import request from '@/config/axios'

// 首链员工账号创建结果（ZS-FC-001，D-07 M5-A：initialPassword 一次性下发，强制首改后置）
export interface EmployeeCreatedRespVO {
  userId: number
  username: string
  initialPassword: string
}

// 创建员工账号（负责人直接创建；返回一次性初始密码）
export const createEmployee = (data: { username: string; nickname: string }) => {
  return request.post<EmployeeCreatedRespVO>({ url: '/firstchain/employee/create', data })
}
