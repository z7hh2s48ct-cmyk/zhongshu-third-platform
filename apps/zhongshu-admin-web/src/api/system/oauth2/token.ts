import request from '@/config/axios'

export interface OAuth2TokenVO {
  id: number // 会话编号（不可用于认证的管理标识；ZS-LOGIN-006：不再回显 accessToken/refreshToken）
  userId: number
  userType: number
  clientId: string
  createTime: Date
  expiresTime: Date
}

// 查询 token列表
export const getAccessTokenPage = (params: PageParam) => {
  return request.get({ url: '/system/oauth2-token/page', params })
}

// 删除 token（强制踢出会话；ZS-LOGIN-006：以不可用于认证的会话 ID 标识，前端无需持有令牌串）
export const deleteAccessToken = (id: number) => {
  return request.delete({ url: '/system/oauth2-token/delete?id=' + id })
}
