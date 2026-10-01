import { http } from '@/http/http'

/** 本组织在职成员（ZS-FC-003 员工选择器数据源） */
export interface EmployeeMember {
  userId: number
  username: string
  nickname: string
}

/** 获得本组织在职成员列表（分配/改派选择器数据源；对象级资格服务端校验） */
export function listOrgMembers() {
  return http.get<EmployeeMember[]>('/firstchain/employee/list-members')
}
