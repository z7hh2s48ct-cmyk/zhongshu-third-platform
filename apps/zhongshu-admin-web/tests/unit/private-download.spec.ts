import { beforeEach, describe, expect, it, vi } from 'vitest'

/**
 * ZS-CLIENT-004：Web 管理端「私有下载」编排单测（对接 ZS-FILE-004.A 主体绑定下载会话，与移动端一致）。
 *
 * 覆盖验收：
 * - 走后端鉴权取流（issue→redeem→chunk 循环），按 Range 组装字节，返回内容而非存储 URL；
 * - 识别业务错误（撤权/过期）而非存成文件，给出明确提示；
 * - cancel 停止取流并撤权下载会话；
 * - 不打印票据 token（ticketToken）。
 *
 * 依赖通过 vi.mock 切断：@/api/infra/file（交付契约）。
 */
const h = vi.hoisted(() => ({
  issueDeliveryTicket: vi.fn(),
  redeemDeliveryTicket: vi.fn(),
  readDeliveryChunk: vi.fn(),
  revokeDelivery: vi.fn()
}))

vi.mock('@/api/infra/file', () => ({
  issueDeliveryTicket: h.issueDeliveryTicket,
  redeemDeliveryTicket: h.redeemDeliveryTicket,
  readDeliveryChunk: h.readDeliveryChunk,
  revokeDelivery: h.revokeDelivery
}))

let downloadPrivateFile: any

beforeEach(async () => {
  vi.resetModules()
  h.issueDeliveryTicket.mockReset()
  h.redeemDeliveryTicket.mockReset()
  h.readDeliveryChunk.mockReset()
  h.revokeDelivery.mockReset()
  h.revokeDelivery.mockResolvedValue(true)
  const mod = await import('@/utils/privateDownload')
  downloadPrivateFile = mod.downloadPrivateFile
})

// base64([1,2]) = 'AQI='，base64([3,4]) = 'AwQ='
function twoChunkStream() {
  h.issueDeliveryTicket.mockResolvedValue({ ticketToken: 'TT-SECRET-xyz' })
  h.redeemDeliveryTicket.mockResolvedValue({ deliverySessionId: 'DS-1', totalSize: 4 })
  h.readDeliveryChunk.mockImplementation(async (_sid: string, start: number) => {
    if (start === 0) return { content: 'AQI=', totalSize: 4, last: false }
    return { content: 'AwQ=', totalSize: 4, last: true }
  })
}

describe('admin-web downloadPrivateFile：主体绑定下载会话鉴权取流', () => {
  it('成功流：issue→redeem→chunk 循环按 Range 组装字节', async () => {
    twoChunkStream()
    const phases: string[] = []
    const progress: Array<[number, number]> = []
    const handle = downloadPrivateFile({
      fileId: 1001,
      purpose: 'preview',
      chunkSize: 2,
      onPhase: (p: string) => phases.push(p),
      onProgress: (loaded: number, total: number) => progress.push([loaded, total])
    })
    const result = await handle.start()
    expect(Array.from(result.content)).toEqual([1, 2, 3, 4])
    expect(result.totalSize).toBe(4)
    expect(handle.phase).toBe('complete')
    // 票据签发以 fileId + purpose；兑换以 ticketToken + purpose
    expect(h.issueDeliveryTicket).toHaveBeenCalledWith({ fileId: 1001, purpose: 'preview' })
    expect(h.redeemDeliveryTicket).toHaveBeenCalledWith('TT-SECRET-xyz', 'preview')
    // Range [start, endInclusive]：先 0..1，再 2..3
    expect(h.readDeliveryChunk).toHaveBeenNthCalledWith(1, 'DS-1', 0, 1)
    expect(h.readDeliveryChunk).toHaveBeenNthCalledWith(2, 'DS-1', 2, 3)
    expect(phases).toEqual(['issuing', 'redeeming', 'downloading', 'complete'])
    expect(progress).toEqual([[2, 4], [4, 4]])
  })

  it('撤权业务错误 → 明确提示、不存成文件（phase=failed，无内容返回）', async () => {
    h.issueDeliveryTicket.mockResolvedValue({ ticketToken: 'TT' })
    h.redeemDeliveryTicket.mockResolvedValue({ deliverySessionId: 'DS-1', totalSize: 4 })
    h.readDeliveryChunk.mockRejectedValue({ code: 1001003030, msg: '交付已被撤权（或读权限已回收），拒绝继续交付' })
    const handle = downloadPrivateFile({ fileId: 1001, purpose: 'preview', chunkSize: 2 })
    const err: any = await handle.start().catch((e: any) => e)
    expect(err).toBeInstanceOf(Error)
    expect(err.code).toBe(1001003030)
    expect(err.message).toContain('撤权')
    expect(handle.phase).toBe('failed')
  })

  it('过期业务错误 → 明确提示（映射过期码）', async () => {
    h.issueDeliveryTicket.mockResolvedValue({ ticketToken: 'TT' })
    h.redeemDeliveryTicket.mockRejectedValue({ code: 1001003031, msg: '交付票据/下载会话已过期' })
    const handle = downloadPrivateFile({ fileId: 1001, purpose: 'preview' })
    const err: any = await handle.start().catch((e: any) => e)
    expect(err.code).toBe(1001003031)
    expect(err.message).toContain('过期')
    expect(handle.phase).toBe('failed')
  })

  it('cancel 停止取流并撤权下载会话', async () => {
    h.issueDeliveryTicket.mockResolvedValue({ ticketToken: 'TT' })
    h.redeemDeliveryTicket.mockResolvedValue({ deliverySessionId: 'DS-1', totalSize: 4 })
    h.readDeliveryChunk.mockImplementation(async (_sid: string, start: number) => {
      if (start === 0) return { content: 'AQI=', totalSize: 4, last: false }
      return { content: 'AwQ=', totalSize: 4, last: true }
    })
    const handle = downloadPrivateFile({
      fileId: 1001,
      purpose: 'preview',
      chunkSize: 2,
      // 首块到达后取消
      onProgress: (loaded: number) => {
        if (loaded >= 2) handle.cancel()
      }
    })
    await expect(handle.start()).rejects.toBeTruthy()
    expect(handle.phase).toBe('cancelled')
    expect(h.revokeDelivery).toHaveBeenCalledWith('DS-1')
    // 取消后不再继续取第二块
    expect(h.readDeliveryChunk).toHaveBeenCalledTimes(1)
  })

  it('不打印票据 token（安全）', async () => {
    twoChunkStream()
    const logs: string[] = []
    const spy = () => (...args: any[]) => logs.push(args.map((a) => String(a)).join(' '))
    const l = vi.spyOn(console, 'log').mockImplementation(spy())
    const e = vi.spyOn(console, 'error').mockImplementation(spy())
    const w = vi.spyOn(console, 'warn').mockImplementation(spy())
    try {
      const handle = downloadPrivateFile({ fileId: 1001, purpose: 'preview', chunkSize: 2 })
      await handle.start()
    } finally {
      l.mockRestore()
      e.mockRestore()
      w.mockRestore()
    }
    expect(logs.join('\n')).not.toContain('TT-SECRET-xyz')
  })
})
