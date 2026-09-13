import { beforeEach, describe, expect, it, vi } from 'vitest'

const h = vi.hoisted(() => ({
  accessToken: 'ADMIN_TOKEN',
  tenantId: 1 as number | null,
  visitTenantId: 2 as number | null,
  refreshToken: 'RT',
  encryptHeader: 'X-Api-Encrypt',
  decryptImpl: ((d: any) => d) as (d: any) => any,
}))

vi.mock('@/config/axios/config', () => ({
  config: {
    base_url: 'https://api.zszj.test/admin-api',
    result_code: 200,
    request_timeout: 30000,
    default_headers: 'application/json',
  },
}))
vi.mock('@/utils/auth', () => ({
  getAccessToken: () => h.accessToken,
  getRefreshToken: () => h.refreshToken,
  getTenantId: () => h.tenantId,
  getVisitTenantId: () => h.visitTenantId,
  removeToken: vi.fn(),
  setToken: vi.fn(),
}))
vi.mock('@/utils/encrypt', () => ({
  ApiEncrypt: {
    getEncryptHeader: () => h.encryptHeader,
    decryptResponse: (d: any) => h.decryptImpl(d),
    encryptRequest: (d: any) => d,
  },
}))
vi.mock('element-plus', () => ({
  ElMessage: { error: vi.fn(), success: vi.fn(), warning: vi.fn() },
  ElMessageBox: { confirm: vi.fn(() => Promise.resolve()) },
  ElNotification: { error: vi.fn(), success: vi.fn() },
}))
vi.mock('@/router', () => ({ resetRouter: vi.fn(), default: {} }))
vi.mock('@/hooks/web/useCache', () => ({
  deleteUserCache: vi.fn(),
  useCache: () => ({ wsCache: { get: vi.fn(), set: vi.fn(), delete: vi.fn() } }),
  CACHE_KEY: {},
}))

import { service } from '@/config/axios/service'

// 大小写不敏感读取请求头（AxiosHeaders 可能规范化存储，也可能保留原始大小写）
function getHeader(headers: any, name: string) {
  if (!headers)
    return undefined
  const lower = name.toLowerCase()
  for (const key of Object.keys(headers)) {
    if (key.toLowerCase() === lower)
      return headers[key]
  }
  if (typeof headers.get === 'function') {
    const v = headers.get(name) ?? headers.get(lower)
    if (v !== undefined && v !== null)
      return v
  }
  return undefined
}

let captured: any

function installAdapter(responseOverride?: Partial<Record<string, any>>) {
  ;(service as any).defaults.adapter = async (config: any) => {
    captured = config
    return {
      data: { code: 0, data: 'ok' },
      status: 200,
      statusText: 'OK',
      headers: {},
      config,
      request: { responseType: 'json' },
      ...responseOverride,
    }
  }
}

beforeEach(() => {
  h.accessToken = 'ADMIN_TOKEN'
  h.tenantId = 1
  h.visitTenantId = 2
  h.decryptImpl = (d: any) => d
  captured = undefined
  // 让适配层返回的 data 原样透传，避免默认 JSON 转换干扰断言
  ;(service as any).defaults.transformResponse = [(d: any) => d]
  installAdapter()
})

describe('service 请求拦截器：B03 登录请求合同凭据范围', () => {
  it('验收①：登录白名单路径不携带 Authorization', async () => {
    await service.request({ url: '/system/auth/login', method: 'POST' })
    expect(getHeader(captured.headers, 'Authorization')).toBeUndefined()
  })

  it('精确路由匹配：/login-history 等非白名单路径应携带 Token（不被 /login 误命中）', async () => {
    await service.request({ url: '/system/auth/login-history', method: 'GET' })
    expect(getHeader(captured.headers, 'Authorization')).toBe('Bearer ADMIN_TOKEN')
  })

  it('批准来源的非白名单请求携带 Bearer Token 与 tenant-id', async () => {
    await service.request({ url: '/system/user/profile/get', method: 'GET' })
    expect(getHeader(captured.headers, 'Authorization')).toBe('Bearer ADMIN_TOKEN')
    // axios 将 header 值规范化为字符串（与实际上行一致）；移动端为普通对象故断言数字 1
    expect(getHeader(captured.headers, 'tenant-id')).toBe('1')
  })

  it('验收②：外部预签名存储绝对 URL 不携带 Token / 租户头 / trace-id', async () => {
    await service.request({ url: 'https://oss.cdn.example.com/bucket/key?X-Amz-Signature=abc', method: 'PUT' })
    expect(getHeader(captured.headers, 'Authorization')).toBeUndefined()
    expect(getHeader(captured.headers, 'tenant-id')).toBeUndefined()
    expect(getHeader(captured.headers, 'visit-tenant-id')).toBeUndefined()
    expect(getHeader(captured.headers, 'trace-id')).toBeUndefined()
  })

  it('trace-id：批准来源请求生成 trace-id；调用方已提供则透传（ZS-SEC-006）', async () => {
    await service.request({ url: '/system/user/profile/get', method: 'GET' })
    const generated = getHeader(captured.headers, 'trace-id')
    expect(typeof generated).toBe('string')
    expect((generated as string).length).toBeGreaterThan(0)

    await service.request({ url: '/system/user/profile/get', method: 'GET', headers: { 'trace-id': 'caller-trace-7' } })
    expect(getHeader(captured.headers, 'trace-id')).toBe('caller-trace-7')
  })

  it('调用方显式 headers.isToken=false 时不携带 Token', async () => {
    await service.request({ url: '/system/user/profile/get', method: 'GET', headers: { isToken: false } })
    expect(getHeader(captured.headers, 'Authorization')).toBeUndefined()
  })
})

describe('service 响应拦截器：异常收敛与可追踪', () => {
  it('验收④：解密失败显式 reject，且携带 trace-id', async () => {
    h.decryptImpl = () => {
      throw new Error('bad cipher')
    }
    installAdapter({
      data: 'CIPHERTEXT',
      headers: { 'X-Api-Encrypt': 'true', 'trace-id': 'srv-dec' },
    })
    const err: any = await service.request({ url: '/system/user/profile/get', method: 'GET' }).catch((e: any) => e)
    expect(err).toBeInstanceOf(Error)
    expect(String(err.message)).toContain('响应数据解密失败')
    expect(err.traceId).toBe('srv-dec')
  })
})
