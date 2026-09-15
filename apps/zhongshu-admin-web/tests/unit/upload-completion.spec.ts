import { beforeEach, describe, expect, it, vi } from 'vitest'

/**
 * ZS-CLIENT-004：Web 管理端「上传完成态」编排单测（与移动端语义一致）。
 *
 * 覆盖验收：
 * - 等待服务端完成确认（upload-complete）后才返回资产 ID，阶段序列 uploading→processing→complete；
 * - 对象已直传但完成确认失败时不显示完整成功（整体 reject、phase=failed）；
 * - 重试复用同一凭证 → 只创建一个凭证（只生成一个资产）；
 * - cancel 中止直传（AbortController）→ phase=cancelled 且 reject；
 * - 不打印预签名 URL / 凭证（credentialToken、uploadUrl）。
 *
 * 依赖通过 vi.mock 切断：@/api/infra/file（契约层）、axios（直传 PUT）。
 */
const h = vi.hoisted(() => ({
  createUploadCredential: vi.fn(),
  completeUpload: vi.fn(),
  put: vi.fn()
}))

vi.mock('@/api/infra/file', () => ({
  createUploadCredential: h.createUploadCredential,
  completeUpload: h.completeUpload
}))
vi.mock('axios', () => ({ default: { put: h.put } }))

let uploadFileWithCompletion: any

beforeEach(async () => {
  vi.resetModules()
  h.createUploadCredential.mockReset()
  h.completeUpload.mockReset()
  h.put.mockReset()
  const mod = await import('@/components/UploadFile/src/uploadCompletion')
  uploadFileWithCompletion = mod.uploadFileWithCompletion
})

/** 浏览器文件桩：编排只读 name/type/size，并将其交给（被 mock 的）axios.put */
const fakeFile = () => ({ name: 'a.png', type: 'image/png', size: 3 }) as any

const CRED = {
  credentialToken: 'CT-SECRET-abc123',
  uploadUrl: 'https://s3.private.test/put?sig=SECRET',
  tempPath: 'tmp/xyz',
  expiresTime: '2026-09-15T23:59:59'
}

describe('admin-web uploadFileWithCompletion：等待完成确认后返回资产 ID', () => {
  it('成功流：uploading→processing→complete，completeUpload 以凭证 token 调用，返回资产 ID', async () => {
    h.createUploadCredential.mockResolvedValue(CRED)
    h.completeUpload.mockResolvedValue(4242)
    h.put.mockResolvedValue({ status: 200 })
    const phases: string[] = []
    const handle = uploadFileWithCompletion({
      file: fakeFile(),
      purpose: 'avatar',
      onPhase: (p: string) => phases.push(p)
    })
    const result = await handle.start()
    expect(result).toEqual({ assetId: 4242 })
    expect(phases).toEqual(['uploading', 'processing', 'complete'])
    expect(handle.phase).toBe('complete')
    expect(h.completeUpload).toHaveBeenCalledWith('CT-SECRET-abc123')
    // 直传 PUT 发往凭证返回的 uploadUrl
    expect(h.put).toHaveBeenCalledWith(CRED.uploadUrl, expect.anything(), expect.objectContaining({
      headers: { 'Content-Type': 'image/png' }
    }))
    // 凭证声明大小/类型来自文件
    expect(h.createUploadCredential).toHaveBeenCalledWith(
      expect.objectContaining({ name: 'a.png', purpose: 'avatar', size: 3, contentType: 'image/png' })
    )
  })

  it('对象已直传但完成确认失败 → 整体 reject，不报成功，phase=failed', async () => {
    h.createUploadCredential.mockResolvedValue(CRED)
    h.completeUpload.mockRejectedValue({ code: 1001003024, msg: '临时对象不存在或为空上传' })
    h.put.mockResolvedValue({ status: 200 })
    const phases: string[] = []
    const handle = uploadFileWithCompletion({
      file: fakeFile(),
      purpose: 'avatar',
      onPhase: (p: string) => phases.push(p)
    })
    await expect(handle.start()).rejects.toMatchObject({ code: 1001003024 })
    expect(handle.phase).toBe('failed')
    expect(phases).toContain('failed')
    expect(phases).not.toContain('complete')
  })

  it('重试复用同一凭证 → 只创建一个凭证（只生成一个资产），直传不重复', async () => {
    h.createUploadCredential.mockResolvedValue(CRED)
    h.put.mockResolvedValue({ status: 200 })
    // 首次确认失败，重试后成功
    h.completeUpload
      .mockRejectedValueOnce({ code: 1001003025, msg: '大小不一致' })
      .mockResolvedValueOnce(4242)
    const handle = uploadFileWithCompletion({ file: fakeFile(), purpose: 'avatar' })
    await expect(handle.start()).rejects.toBeTruthy()
    const retried = await handle.retry()
    expect(retried).toEqual({ assetId: 4242 })
    // 关键：凭证只创建一次（重试复用 credentialToken，服务端幂等 → 只生成一个资产）
    expect(h.createUploadCredential).toHaveBeenCalledTimes(1)
    expect(h.completeUpload).toHaveBeenCalledTimes(2)
    expect(h.completeUpload).toHaveBeenLastCalledWith('CT-SECRET-abc123')
    // 直传已成功，重试不再重复 PUT
    expect(h.put).toHaveBeenCalledTimes(1)
    expect(handle.phase).toBe('complete')
  })

  it('cancel 中止直传（AbortController）→ phase=cancelled 且 start() reject', async () => {
    h.createUploadCredential.mockResolvedValue(CRED)
    // PUT 挂起：直到 signal abort 才 reject
    h.put.mockImplementation(
      (_url: string, _file: any, cfg: any) =>
        new Promise((_resolve, reject) => {
          cfg.signal.addEventListener('abort', () => reject(new Error('aborted')))
        })
    )
    const handle = uploadFileWithCompletion({ file: fakeFile(), purpose: 'avatar' })
    const p = handle.start()
    // 让 createUploadCredential 的微任务先跑完，进入 PUT 挂起
    await Promise.resolve()
    await Promise.resolve()
    await Promise.resolve()
    handle.cancel()
    await expect(p).rejects.toBeTruthy()
    expect(handle.phase).toBe('cancelled')
    // 取消后不得触发完成确认
    expect(h.completeUpload).not.toHaveBeenCalled()
  })

  it('不打印预签名 URL / 凭证（安全）', async () => {
    h.createUploadCredential.mockResolvedValue(CRED)
    h.completeUpload.mockResolvedValue(4242)
    h.put.mockResolvedValue({ status: 200 })
    const logs: string[] = []
    const spy = () => (...args: any[]) => logs.push(args.map((a) => String(a)).join(' '))
    const l = vi.spyOn(console, 'log').mockImplementation(spy())
    const e = vi.spyOn(console, 'error').mockImplementation(spy())
    const w = vi.spyOn(console, 'warn').mockImplementation(spy())
    try {
      const handle = uploadFileWithCompletion({ file: fakeFile(), purpose: 'avatar' })
      await handle.start()
    } finally {
      l.mockRestore()
      e.mockRestore()
      w.mockRestore()
    }
    const all = logs.join('\n')
    expect(all).not.toContain('CT-SECRET-abc123')
    expect(all).not.toContain('https://s3.private.test/put')
    expect(all).not.toContain('sig=SECRET')
  })
})
