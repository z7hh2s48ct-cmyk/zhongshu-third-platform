import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  ensureTraceId,
  extractPathname,
  httpInterceptor,
  isApprovedApiOrigin,
  isWhitelistedPath,
} from '@/http/interceptor'

// vi.mock 会被提升到 import 之前执行；用 vi.hoisted 提供可在工厂内安全引用的可变状态
const h = vi.hoisted(() => ({
  tokenState: { token: 'ACCESS_TOKEN_123' },
  userState: { tenantId: 1 as number | null, visitTenantId: 2 as number | null },
}))

vi.mock('@/utils', () => ({
  getEnvBaseUrl: () => 'https://api.zszj.test',
  getEnvBaseUrlRoot: () => 'https://api.zszj.test',
}))

vi.mock('@/utils/encrypt', () => ({
  ApiEncrypt: {
    getEncryptHeader: () => 'X-Api-Encrypt',
    encryptRequest: (data: any) => data,
  },
}))

vi.mock('@/store', () => ({
  useTokenStore: () => ({
    updateNowTime: () => ({ validToken: h.tokenState.token }),
    validToken: h.tokenState.token,
    tokenInfo: {},
  }),
  useUserStore: () => h.userState,
}))

const APPROVED = 'https://api.zszj.test'

function invoke(options: Record<string, any>) {
  return httpInterceptor.invoke(options as any) as Record<string, any>
}

beforeEach(() => {
  h.tokenState.token = 'ACCESS_TOKEN_123'
  h.userState.tenantId = 1
  h.userState.visitTenantId = 2
})

describe('extractPathname：剥离 query/hash 得到路径', () => {
  it('绝对 URL 取 pathname', () => {
    expect(extractPathname('https://api.zszj.test/admin-api/system/auth/login?a=1#f'))
      .toBe('/admin-api/system/auth/login')
  })
  it('相对 URL 取自身去 query', () => {
    expect(extractPathname('/admin-api/system/auth/login?a=1')).toBe('/admin-api/system/auth/login')
  })
})

describe('isWhitelistedPath：精确路由匹配（验收①的匹配基础）', () => {
  it('命中登录 / 刷新令牌 / 租户查询路径', () => {
    expect(isWhitelistedPath('/admin-api/system/auth/login')).toBe(true)
    expect(isWhitelistedPath('/admin-api/system/auth/refresh-token')).toBe(true)
    expect(isWhitelistedPath('/admin-api/system/tenant/get-id-by-name')).toBe(true)
  })
  it('不误命中前缀相似的路径（段边界精确匹配）', () => {
    expect(isWhitelistedPath('/admin-api/system/auth/login-history')).toBe(false)
    expect(isWhitelistedPath('/admin-api/member/login-log/page')).toBe(false)
    expect(isWhitelistedPath('/admin-api/system/auth/refresh-token-batch')).toBe(false)
  })
  it('剥离 query 后仍可命中', () => {
    expect(isWhitelistedPath('https://api.zszj.test/admin-api/system/auth/login?redirect=%2Fx'))
      .toBe(true)
  })
})

describe('isApprovedApiOrigin：凭据仅发往批准 API origin（验收②的判定基础）', () => {
  it('相对地址视为同源批准', () => {
    expect(isApprovedApiOrigin('/admin-api/x', APPROVED)).toBe(true)
  })
  it('同 origin 绝对地址批准（大小写不敏感）', () => {
    expect(isApprovedApiOrigin('https://api.zszj.test/admin-api/x', APPROVED)).toBe(true)
    expect(isApprovedApiOrigin('HTTPS://API.ZSZJ.TEST/admin-api/x', APPROVED)).toBe(true)
  })
  it('外部 origin（预签名存储）不批准', () => {
    expect(isApprovedApiOrigin('https://oss.cdn.example.com/bucket/k?sig=1', APPROVED)).toBe(false)
  })
  it('无法确定批准 origin 时对绝对地址失败关闭', () => {
    expect(isApprovedApiOrigin('https://api.zszj.test/x', '')).toBe(false)
  })
})

describe('ensureTraceId：trace/correlation 标识透传（ZS-SEC-006）', () => {
  it('调用方已提供 trace-id 时原样透传', () => {
    const header: Record<string, any> = { 'trace-id': 'caller-trace-1' }
    expect(ensureTraceId(header)).toBe('caller-trace-1')
    expect(header['trace-id']).toBe('caller-trace-1')
  })
  it('缺省时生成非空客户端关联标识', () => {
    const header: Record<string, any> = {}
    const id = ensureTraceId(header)
    expect(typeof id).toBe('string')
    expect(id.length).toBeGreaterThan(0)
    expect(header['trace-id']).toBe(id)
  })
})

describe('httpInterceptor.invoke：B03 登录请求合同凭据范围', () => {
  it('验收①：携带现有 Token 请求登录白名单路径不泄露凭据', () => {
    const options = invoke({
      url: 'https://api.zszj.test/admin-api/system/auth/login',
      method: 'POST',
      header: {},
    })
    expect(options.header.Authorization).toBeUndefined()
  })

  it('批准来源的非白名单请求附带 Bearer Token', () => {
    const options = invoke({
      url: 'https://api.zszj.test/admin-api/system/user/profile/get',
      method: 'GET',
      header: {},
    })
    expect(options.header.Authorization).toBe('Bearer ACCESS_TOKEN_123')
  })

  it('验收②：外部预签名存储绝对 URL 不携带 Token / 租户头 / trace-id', () => {
    const options = invoke({
      url: 'https://oss.cdn.example.com/bucket/key?X-Amz-Signature=abc',
      method: 'PUT',
      header: {},
    })
    expect(options.header.Authorization).toBeUndefined()
    expect(options.header['tenant-id']).toBeUndefined()
    expect(options.header['visit-tenant-id']).toBeUndefined()
    expect(options.header['trace-id']).toBeUndefined()
  })

  it('批准来源请求携带租户头（tenant-id），跨租户头按开关注入', () => {
    const options = invoke({
      url: 'https://api.zszj.test/admin-api/system/user/profile/get',
      method: 'GET',
      header: {},
    })
    expect(options.header['tenant-id']).toBe(1)
    // VITE_APP_TENANT_VISIT_ENABLE=false：不注入 visit-tenant-id
    expect(options.header['visit-tenant-id']).toBeUndefined()
  })

  it('登录白名单仍可携带 tenant-id（登录需租户上下文），但不携带 Token', () => {
    const options = invoke({
      url: 'https://api.zszj.test/admin-api/system/auth/login',
      method: 'POST',
      header: {},
    })
    expect(options.header.Authorization).toBeUndefined()
    expect(options.header['tenant-id']).toBe(1)
  })

  it('批准来源请求生成 trace-id；调用方已提供则透传', () => {
    const generated = invoke({
      url: 'https://api.zszj.test/admin-api/system/user/profile/get',
      method: 'GET',
      header: {},
    })
    expect(typeof generated.header['trace-id']).toBe('string')
    expect(generated.header['trace-id'].length).toBeGreaterThan(0)

    const passed = invoke({
      url: 'https://api.zszj.test/admin-api/system/user/profile/get',
      method: 'GET',
      header: { 'trace-id': 'caller-trace-9' },
    })
    expect(passed.header['trace-id']).toBe('caller-trace-9')
  })

  it('调用方显式 header.isToken=false 时不附带 Token（批准来源）', () => {
    const options = invoke({
      url: 'https://api.zszj.test/admin-api/system/user/profile/get',
      method: 'GET',
      header: { isToken: false },
    })
    expect(options.header.Authorization).toBeUndefined()
  })
})
