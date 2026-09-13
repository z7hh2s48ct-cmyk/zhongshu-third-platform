import { beforeEach, describe, expect, it, vi } from 'vitest'

// 稳定共享的 store mock（跨用例可断言调用次数），以及可切换的解密实现
const h = vi.hoisted(() => {
  const refreshToken = vi.fn()
  const logout = vi.fn(async () => {})
  const store = {
    tokenInfo: { accessToken: 'AT', refreshToken: 'RT' },
    refreshToken,
    logout,
    updateNowTime: () => ({ validToken: 'AT' }),
    validToken: 'AT',
  }
  return {
    store,
    refreshToken,
    logout,
    encryptHeader: 'X-Api-Encrypt',
    decryptImpl: ((d: any) => d) as (d: any) => any,
  }
})

vi.mock('@/store/token', () => ({ useTokenStore: () => h.store }))
vi.mock('@/utils', () => ({ getLastPage: () => null, isDoubleTokenMode: true }))
vi.mock('@/utils/encrypt', () => ({
  ApiEncrypt: {
    getEncryptHeader: () => h.encryptHeader,
    decryptResponse: (d: any) => h.decryptImpl(d),
    encryptRequest: (d: any) => d,
  },
}))
vi.mock('@/utils/toLoginPage', () => ({ toLoginPage: vi.fn() }))

const uni = (globalThis as any).uni

// 每个用例使用独立的 http 模块实例，隔离其模块级刷新队列 / 状态标志
let http: (options: any) => Promise<any>

beforeEach(async () => {
  vi.resetModules()
  h.refreshToken.mockReset()
  h.logout.mockReset()
  h.decryptImpl = (d: any) => d
  uni.showToast = vi.fn()
  uni.hideToast = vi.fn()
  uni.request = vi.fn()
  const mod = await import('@/http/http')
  http = mod.http as any
})

function approved(path: string, header: Record<string, any> = {}) {
  return { url: `https://api.zszj.test${path}`, method: 'GET', header } as any
}

describe('http 异常收敛：解密失败不永久 loading（验收④）', () => {
  it('解密失败分支显式 reject，且携带服务端 trace-id', async () => {
    h.decryptImpl = () => {
      throw new Error('bad cipher')
    }
    uni.request = vi.fn((cfg: any) => {
      cfg.success({
        statusCode: 200,
        data: 'CIPHERTEXT',
        header: { 'X-Api-Encrypt': 'true', 'trace-id': 'srv-decrypt' },
      })
    })
    const p = http(approved('/system/user/profile/get'))
    const outcome: any = await Promise.race([
      p.then(() => ({ tag: 'resolved' }), (e: any) => ({ tag: 'rejected', e })),
      new Promise(resolve => setTimeout(() => resolve('pending'), 300)),
    ])
    // RED：修复前 throw 发生在 uni success 回调内，外层 Promise 永不 settle → 'pending'
    expect(outcome).not.toBe('pending')
    expect(outcome.tag).toBe('rejected')
    expect(outcome.e).toBeInstanceOf(Error)
    expect(outcome.e.traceId).toBe('srv-decrypt')
  })
})

describe('http 一致可追踪结果（验收⑤）', () => {
  it('业务错误(500)一致 reject 且回显 trace-id', async () => {
    uni.request = vi.fn((cfg: any) =>
      cfg.success({ statusCode: 200, data: { code: 500, msg: 'boom', data: null }, header: { 'trace-id': 'srv-500' } }),
    )
    await expect(http(approved('/x'))).rejects.toMatchObject({ code: 500, traceId: 'srv-500' })
  })

  it('403 得到一致可追踪结果', async () => {
    uni.request = vi.fn((cfg: any) =>
      cfg.success({ statusCode: 200, data: { code: 403, msg: 'forbidden' }, header: { 'trace-id': 'srv-403' } }),
    )
    await expect(http(approved('/x'))).rejects.toMatchObject({ code: 403, traceId: 'srv-403' })
  })

  it('429 得到一致可追踪结果', async () => {
    uni.request = vi.fn((cfg: any) =>
      cfg.success({ statusCode: 200, data: { code: 429, msg: 'too many requests' }, header: { 'trace-id': 'srv-429' } }),
    )
    await expect(http(approved('/x'))).rejects.toMatchObject({ code: 429, traceId: 'srv-429' })
  })

  it('断网 reject 且携带请求侧 trace-id', async () => {
    uni.request = vi.fn((cfg: any) => cfg.fail({ errMsg: 'request:fail' }))
    await expect(http(approved('/x', { 'trace-id': 'client-net' }))).rejects.toMatchObject({
      errMsg: 'request:fail',
      traceId: 'client-net',
    })
  })

  it('请求取消 reject 且可追踪', async () => {
    uni.request = vi.fn((cfg: any) => cfg.fail({ errMsg: 'request:fail abort' }))
    await expect(http(approved('/x', { 'trace-id': 'client-cancel' }))).rejects.toMatchObject({
      errMsg: 'request:fail abort',
      traceId: 'client-cancel',
    })
  })
})

describe('http 刷新队列（验收③：至多刷新一次、失败全部 reject、不循环）', () => {
  it('并发 401 至多触发一次刷新，其余按合同等待重放并全部 resolve', async () => {
    h.refreshToken.mockImplementation(async () => ({ data: { data: { accessToken: 'AT2', refreshToken: 'RT2' } } }))
    let n = 0
    uni.request = vi.fn((cfg: any) => {
      if (cfg.__isRefreshTokenRetry) {
        cfg.success({ statusCode: 200, data: { code: 0, data: `ok-${++n}` }, header: {} })
        return
      }
      cfg.success({ statusCode: 200, data: { code: 401 }, header: {} })
    })
    const p1 = http(approved('/a'))
    const p2 = http(approved('/b'))
    const results = await Promise.all([p1, p2])
    expect(h.refreshToken).toHaveBeenCalledTimes(1)
    expect(results).toEqual(['ok-1', 'ok-2'])
  })

  it('刷新失败时排队请求全部 reject，且不进入无限刷新', async () => {
    vi.useFakeTimers()
    try {
      h.refreshToken.mockImplementation(async () => {
        throw new Error('refresh failed')
      })
      uni.request = vi.fn((cfg: any) => cfg.success({ statusCode: 200, data: { code: 401 }, header: {} }))
      const p1 = http(approved('/a'))
      const p2 = http(approved('/b'))
      const settled = await Promise.allSettled([p1, p2])
      expect(settled.every(s => s.status === 'rejected')).toBe(true)
      expect(h.refreshToken).toHaveBeenCalledTimes(1)
    } finally {
      vi.useRealTimers()
    }
  })

  it('重试后仍 401 不再刷新（避免无限循环），直接 reject', async () => {
    uni.request = vi.fn((cfg: any) => cfg.success({ statusCode: 200, data: { code: 401 }, header: {} }))
    await expect(http({ ...approved('/a'), __isRefreshTokenRetry: true })).rejects.toBeTruthy()
    expect(h.refreshToken).not.toHaveBeenCalled()
  })
})
