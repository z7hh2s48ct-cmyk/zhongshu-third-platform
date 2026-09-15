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

describe('service codex r2 回归：协议相对 URL 与刷新重放泄露（P1）', () => {
  it('协议相对 URL（//host/path）不携带平台凭据，且不得拼进 baseURL 误判为批准来源', async () => {
    await service.request({ url: '//oss.example/upload', method: 'PUT' })
    expect(getHeader(captured.headers, 'Authorization')).toBeUndefined()
    expect(getHeader(captured.headers, 'tenant-id')).toBeUndefined()
    expect(getHeader(captured.headers, 'trace-id')).toBeUndefined()
    // axios 将 //host 视为绝对地址直发外部主机；判定用的完整地址必须是原样 //host，而非 baseURL 拼接
    expect(captured.url).toBe('//oss.example/upload')
  })

  it('刷新成功后回放：批准来源请求重放 Bearer；外部地址请求显式清除残留 Authorization', async () => {
    const calls: any[] = []
    ;(service as any).defaults.adapter = async (config: any) => {
      calls.push(config)
      const isFirst = calls.length === 1
      return {
        data: isFirst ? { code: 401, msg: '账号未登录' } : { code: 0, data: 'ok' },
        status: 200,
        statusText: 'OK',
        headers: {},
        config,
        request: { responseType: 'json' },
      }
    }
    // 拦截全局 axios.post（refreshToken 使用原始 axios 实例）
    const axiosMod = await import('axios')
    const postSpy = vi.spyOn(axiosMod.default, 'post').mockResolvedValue({
      data: { code: 0, data: { userId: 1, accessToken: 'NEW_TOKEN', refreshToken: 'RT' } },
    } as any)
    postSpy.mockClear() // 同文件前序用例可能已触发刷新，清空计数

    // 队首请求为外部地址且 401 → 刷新后回放，不得把平台凭据重放到外部主机
    await service.request({ url: 'https://oss.cdn.example.com/bucket/key', method: 'PUT' })
    expect(postSpy).toHaveBeenCalledTimes(1)
    expect(getHeader(calls[0].headers, 'Authorization')).toBeUndefined()
    expect(getHeader(calls[1].headers, 'Authorization')).toBeUndefined()

    // 批准来源请求 401 → 刷新后回放，应携带重放的 Bearer
    const calls2: any[] = []
    ;(service as any).defaults.adapter = async (config: any) => {
      calls2.push(config)
      const isFirst = calls2.length === 1
      return {
        data: isFirst ? { code: 401, msg: '账号未登录' } : { code: 0, data: 'ok' },
        status: 200,
        statusText: 'OK',
        headers: {},
        config,
        request: { responseType: 'json' },
      }
    }
    await service.request({ url: '/system/user/profile/get', method: 'GET' })
    expect(getHeader(calls2[1].headers, 'Authorization')).toBe('Bearer ADMIN_TOKEN')
  })

  it('刷新请求的 tenant-id 只随本次请求发送，不写入 axios 全局 defaults（防 useUpload 外部直传泄露）', async () => {
    const calls: any[] = []
    ;(service as any).defaults.adapter = async (config: any) => {
      calls.push(config)
      const isFirst = calls.length === 1
      return {
        data: isFirst ? { code: 401, msg: '账号未登录' } : { code: 0, data: 'ok' },
        status: 200,
        statusText: 'OK',
        headers: {},
        config,
        request: { responseType: 'json' },
      }
    }
    const axiosMod = await import('axios')
    const postSpy = vi.spyOn(axiosMod.default, 'post').mockResolvedValue({
      data: { code: 0, data: { userId: 1, accessToken: 'NEW_TOKEN', refreshToken: 'RT' } },
    } as any)
    postSpy.mockClear() // 同文件前序用例可能已触发刷新，清空计数

    await service.request({ url: '/system/user/profile/get', method: 'GET' })
    expect(postSpy).toHaveBeenCalledTimes(1)
    // 刷新请求自身携带 tenant-id
    const refreshArg: any = postSpy.mock.calls[0][2]
    expect(refreshArg.headers['tenant-id']).toBe(1)
    // 全局 defaults 不被污染
    expect((axiosMod.default.defaults.headers.common as any)['tenant-id']).toBeUndefined()
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

describe('service codex r0 回归：trace-id 合同（P2）', () => {
  it('批准来源生成的 trace-id 为 32 位小写十六进制（后端 TracerUtils 格式，P2）', async () => {
    await service.request({ url: '/system/user/profile/get', method: 'GET' })
    expect(getHeader(captured.headers, 'trace-id')).toMatch(/^[0-9a-f]{32}$/)
  })

  it('调用方大写 Trace-Id 去重为单一 trace-id 键且原值透传（P2）', async () => {
    await service.request({
      url: '/system/user/profile/get',
      method: 'GET',
      headers: { 'Trace-Id': '0123456789abcdef0123456789abcdef' },
    })
    const keys = Object.keys(captured.headers).filter(k => k.toLowerCase() === 'trace-id')
    expect(keys).toHaveLength(1)
    expect(getHeader(captured.headers, 'trace-id')).toBe('0123456789abcdef0123456789abcdef')
  })
})

describe('service 响应拦截器：业务错误结构化传递（ZS-CLIENT-004 codex r0 P2）', () => {
  it('业务错误（code≠0/200/401/500/901）→ reject 携带 code/msg/data/traceId 的结构化错误，不再丢成裸字符串', async () => {
    installAdapter({
      data: { code: 1001003030, msg: '交付已被撤权（或读权限已回收），拒绝继续交付', data: { sessionId: 'DS-9' } },
      headers: { 'trace-id': 'srv-biz-77' },
    })
    const err: any = await service
      .request({ url: '/infra/file/delivery/read', method: 'GET' })
      .catch((e: any) => e)
    // 旧实现 reject 裸字符串 'error'，会丢失 code/msg/traceId，使上层下载错误映射全部退化为通用「下载失败」
    expect(err).toBeInstanceOf(Error)
    expect(err.code).toBe(1001003030)
    expect(err.msg).toContain('撤权')
    expect(err.message).toContain('撤权')
    expect(err.data).toEqual({ sessionId: 'DS-9' })
    expect(err.traceId).toBe('srv-biz-77')
  })

  it('业务错误仍弹出用户提示（ElNotification.error），结构化 reject 不静默吞错', async () => {
    const { ElNotification } = await import('element-plus')
    ;(ElNotification.error as any).mockClear()
    installAdapter({
      data: { code: 1001003031, msg: '下载链接已过期，请重新获取' },
      headers: { 'trace-id': 'srv-biz-88' },
    })
    const err: any = await service
      .request({ url: '/infra/file/delivery/read', method: 'GET' })
      .catch((e: any) => e)
    expect(ElNotification.error).toHaveBeenCalledWith({ title: '下载链接已过期，请重新获取' })
    expect(err.code).toBe(1001003031)
    expect(err.traceId).toBe('srv-biz-88')
  })
})
