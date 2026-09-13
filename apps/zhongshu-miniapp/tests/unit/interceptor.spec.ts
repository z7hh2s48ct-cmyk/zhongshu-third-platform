import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  ensureTraceId,
  extractPathname,
  httpInterceptor,
  isApprovedApiOrigin,
  isWhitelistedPath,
  resolveApprovedOrigin,
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

describe('codex r0 回归：小程序运行时兼容与 trace-id 合同（ZS-CLIENT-003）', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  // P1：微信小程序运行时无浏览器 URL 全局；origin/pathname 解析不得依赖 new URL()，
  // 否则 approvedOrigin 退化为空 → 所有绝对地址请求丢失 Token 与 tenant-id（登录失效）
  it('无 URL 全局时 resolveApprovedOrigin 仍解析出批准 origin（P1）', () => {
    vi.stubGlobal('URL', undefined)
    expect(resolveApprovedOrigin('https://api.zszj.test/admin-api')).toBe('https://api.zszj.test')
  })
  it('无 URL 全局时 isApprovedApiOrigin 对批准来源 true、外部来源 false（P1）', () => {
    vi.stubGlobal('URL', undefined)
    expect(isApprovedApiOrigin('https://api.zszj.test/admin-api/x', 'https://api.zszj.test')).toBe(true)
    expect(isApprovedApiOrigin('https://oss.cdn.example.com/k', 'https://api.zszj.test')).toBe(false)
  })
  it('无 URL 全局时 extractPathname 仍剥离 scheme+authority（P1）', () => {
    vi.stubGlobal('URL', undefined)
    expect(extractPathname('https://api.zszj.test/admin-api/login?a=1')).toBe('/admin-api/login')
  })

  // P2：后端 TracerUtils.isValidTraceIdFormat 要求恰好 32 位十六进制，否则被 TraceFilter 替换、无法关联日志
  it('生成的 trace-id 为 32 位小写十六进制（P2）', () => {
    expect(ensureTraceId({})).toMatch(/^[0-9a-f]{32}$/)
  })
  it('无 crypto 运行时回退仍生成 32 位十六进制（P2）', () => {
    vi.stubGlobal('crypto', undefined)
    expect(ensureTraceId({})).toMatch(/^[0-9a-f]{32}$/)
  })

  // P2：调用方提供大写 Trace-Id 时应规范为单一 trace-id 键，避免 H5 上行合并成 "id, id"
  it('调用方大写 Trace-Id 去重为单一 trace-id 键且原值透传（P2）', () => {
    const header: Record<string, any> = { 'Trace-Id': '0123456789abcdef0123456789abcdef' }
    const id = ensureTraceId(header)
    expect(id).toBe('0123456789abcdef0123456789abcdef')
    expect(Object.keys(header).filter(k => k.toLowerCase() === 'trace-id')).toHaveLength(1)
    expect(header['trace-id']).toBe('0123456789abcdef0123456789abcdef')
    expect(header['Trace-Id']).toBeUndefined()
  })
})

describe('codex r1 回归：origin 默认端口归一化（P2）', () => {
  it('https 显式 :443 与省略端口视为同一 origin', () => {
    expect(resolveApprovedOrigin('https://api.zszj.test:443/admin-api')).toBe('https://api.zszj.test')
    expect(isApprovedApiOrigin('https://api.zszj.test:443/admin-api/x', 'https://api.zszj.test')).toBe(true)
    expect(isApprovedApiOrigin('https://api.zszj.test/admin-api/x', 'https://api.zszj.test:443')).toBe(true)
  })
  it('http 显式 :80 归一化；非默认端口保留', () => {
    expect(resolveApprovedOrigin('http://api.zszj.test:80/admin-api')).toBe('http://api.zszj.test')
    expect(resolveApprovedOrigin('https://api.zszj.test:8443/admin-api')).toBe('https://api.zszj.test:8443')
    expect(isApprovedApiOrigin('https://api.zszj.test:8443/x', 'https://api.zszj.test')).toBe(false)
  })
})
