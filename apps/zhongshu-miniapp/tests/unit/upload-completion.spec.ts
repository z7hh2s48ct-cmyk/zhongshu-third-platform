import { beforeEach, describe, expect, it, vi } from 'vitest'

/**
 * ZS-CLIENT-004：移动端「上传完成态」编排单测。
 *
 * 覆盖验收：
 * - 等待服务端完成确认（upload-complete）后才返回资产 ID，阶段序列 uploading→processing→complete；
 * - 对象已直传但完成确认失败时不显示完整成功（整体 reject、phase=failed）；
 * - 重试复用同一凭证 → 只创建一个凭证（只生成一个资产）；
 * - cancel 中止直传 → phase=cancelled 且 reject；
 * - 不打印预签名 URL / 凭证（credentialToken、uploadUrl）。
 *
 * 依赖通过 vi.mock 切断：@/api/infra/file（契约层）、uni.request（直传 PUT）、uni.getFileSystemManager（读字节）。
 */
const h = vi.hoisted(() => ({
  createUploadCredential: vi.fn(),
  completeUpload: vi.fn(),
}))

vi.mock('@/api/infra/file', () => ({
  createUploadCredential: h.createUploadCredential,
  completeUpload: h.completeUpload,
}))
// uploadFile.ts 顶层 import useToast（wd-toast），node 环境无需真实组件
vi.mock('@wot-ui/ui/components/wd-toast', () => ({ useToast: () => ({ show: vi.fn() }) }))

const uni = (globalThis as any).uni

let uploadFileWithCompletion: any

beforeEach(async () => {
  vi.resetModules()
  h.createUploadCredential.mockReset()
  h.completeUpload.mockReset()
  // 读字节桩：返回 3 字节，走 getFileSystemManager 分支（避免 H5 fetch）
  uni.getFileSystemManager = () => ({ readFileSync: () => new Uint8Array([1, 2, 3]).buffer })
  uni.request = vi.fn()
  const mod = await import('@/utils/uploadFile')
  uploadFileWithCompletion = mod.uploadFileWithCompletion
})

/** 轮询微任务直至条件满足（精确停在某个挂起的 await 边界，避免脆弱的固定 tick 数） */
async function waitFor(cond: () => boolean, max = 50) {
  for (let i = 0; i < max && !cond(); i++) await Promise.resolve()
}

/** 让 uni.request PUT 立即成功，并返回可 abort 的 task */
function putSuccess() {
  const abort = vi.fn()
  uni.request = vi.fn((cfg: any) => {
    cfg.success({ statusCode: 200 })
    return { abort }
  })
  return abort
}

const CRED = {
  credentialToken: 'CT-SECRET-abc123',
  uploadUrl: 'https://s3.private.test/put?sig=SECRET',
  tempPath: 'tmp/xyz',
  expiresTime: '2026-09-15T23:59:59',
}

describe('uploadFileWithCompletion：等待完成确认后返回资产 ID', () => {
  it('成功流：uploading→processing→complete，completeUpload 以凭证 token 调用，返回资产 ID', async () => {
    h.createUploadCredential.mockResolvedValue(CRED)
    h.completeUpload.mockResolvedValue(4242)
    putSuccess()
    const phases: string[] = []
    const handle = uploadFileWithCompletion({
      filePath: '/tmp/a.png',
      purpose: 'avatar',
      onPhase: (p: string) => phases.push(p),
    })
    const result = await handle.start()
    expect(result).toEqual({ assetId: 4242 })
    expect(phases).toEqual(['uploading', 'processing', 'complete'])
    expect(handle.phase).toBe('complete')
    expect(h.completeUpload).toHaveBeenCalledWith('CT-SECRET-abc123')
    // 直传 PUT 发往凭证返回的 uploadUrl
    expect(uni.request).toHaveBeenCalledWith(expect.objectContaining({ method: 'PUT', url: CRED.uploadUrl }))
  })

  it('对象已直传但完成确认失败 → 整体 reject，不报成功，phase=failed', async () => {
    h.createUploadCredential.mockResolvedValue(CRED)
    h.completeUpload.mockRejectedValue({ code: 1001003024, msg: '临时对象不存在或为空上传' })
    putSuccess()
    const phases: string[] = []
    const handle = uploadFileWithCompletion({
      filePath: '/tmp/a.png',
      purpose: 'avatar',
      onPhase: (p: string) => phases.push(p),
    })
    await expect(handle.start()).rejects.toMatchObject({ code: 1001003024 })
    expect(handle.phase).toBe('failed')
    expect(phases).toContain('failed')
    expect(phases).not.toContain('complete')
  })

  it('重试复用同一凭证 → 只创建一个凭证（只生成一个资产）', async () => {
    h.createUploadCredential.mockResolvedValue(CRED)
    putSuccess()
    // 首次确认失败，重试后成功
    h.completeUpload
      .mockRejectedValueOnce({ code: 1001003025, msg: '大小不一致' })
      .mockResolvedValueOnce(4242)
    const handle = uploadFileWithCompletion({ filePath: '/tmp/a.png', purpose: 'avatar' })
    await expect(handle.start()).rejects.toBeTruthy()
    const retried = await handle.retry()
    expect(retried).toEqual({ assetId: 4242 })
    // 关键：凭证只创建一次（重试复用 credentialToken，服务端幂等 → 只生成一个资产）
    expect(h.createUploadCredential).toHaveBeenCalledTimes(1)
    expect(h.completeUpload).toHaveBeenCalledTimes(2)
    expect(h.completeUpload).toHaveBeenLastCalledWith('CT-SECRET-abc123')
    expect(handle.phase).toBe('complete')
  })

  it('cancel 中止直传 → phase=cancelled 且 start() reject', async () => {
    h.createUploadCredential.mockResolvedValue(CRED)
    const abort = vi.fn()
    // PUT 挂起：既不 success 也不 fail，直到 abort 触发 fail
    uni.request = vi.fn((cfg: any) => {
      abort.mockImplementation(() => cfg.fail({ errMsg: 'request:fail abort' }))
      return { abort }
    })
    const handle = uploadFileWithCompletion({ filePath: '/tmp/a.png', purpose: 'avatar' })
    const p = handle.start()
    // 让 readFile/createUploadCredential 的微任务先跑完，进入 PUT 挂起
    await Promise.resolve()
    await Promise.resolve()
    await Promise.resolve()
    handle.cancel()
    await expect(p).rejects.toBeTruthy()
    expect(handle.phase).toBe('cancelled')
    expect(abort).toHaveBeenCalled()
    // 取消后不得触发完成确认
    expect(h.completeUpload).not.toHaveBeenCalled()
  })

  it('不打印预签名 URL / 凭证（安全）', async () => {
    h.createUploadCredential.mockResolvedValue(CRED)
    h.completeUpload.mockResolvedValue(4242)
    putSuccess()
    const logs: string[] = []
    const spy = () => (...args: any[]) => logs.push(args.map(a => String(a)).join(' '))
    const l = vi.spyOn(console, 'log').mockImplementation(spy())
    const e = vi.spyOn(console, 'error').mockImplementation(spy())
    const w = vi.spyOn(console, 'warn').mockImplementation(spy())
    try {
      const handle = uploadFileWithCompletion({ filePath: '/tmp/a.png', purpose: 'avatar' })
      await handle.start()
    } finally {
      l.mockRestore(); e.mockRestore(); w.mockRestore()
    }
    const all = logs.join('\n')
    expect(all).not.toContain('CT-SECRET-abc123')
    expect(all).not.toContain('https://s3.private.test/put')
    expect(all).not.toContain('sig=SECRET')
  })

  it('签发凭证挂起期间 cancel → 不再 PUT / 完成确认，phase=cancelled，reject UPLOAD_CANCELLED（codex r0 P1）', async () => {
    let resolveCred!: (v: any) => void
    h.createUploadCredential.mockImplementation(() => new Promise((r) => { resolveCred = r }))
    const handle = uploadFileWithCompletion({ filePath: '/tmp/a.png', purpose: 'avatar' })
    const p = handle.start()
    await waitFor(() => h.createUploadCredential.mock.calls.length > 0) // 停在签发凭证挂起（readFile 已解析）
    handle.cancel()
    resolveCred(CRED) // 凭证迟到返回
    const err: any = await p.catch((e: any) => e)
    expect(err.code).toBe('UPLOAD_CANCELLED')
    expect(handle.phase).toBe('cancelled')
    // 取消后不得继续直传（uni.request PUT）/ 完成确认
    expect(uni.request).not.toHaveBeenCalled()
    expect(h.completeUpload).not.toHaveBeenCalled()
  })

  it('completeUpload 挂起期间 cancel → 迟到的成功响应不覆盖 cancelled / 不伪报 complete（codex r0 P1）', async () => {
    h.createUploadCredential.mockResolvedValue(CRED)
    putSuccess()
    let resolveComplete!: (v: any) => void
    h.completeUpload.mockImplementation(() => new Promise((r) => { resolveComplete = r }))
    const handle = uploadFileWithCompletion({ filePath: '/tmp/a.png', purpose: 'avatar' })
    const p = handle.start()
    await waitFor(() => h.completeUpload.mock.calls.length > 0) // 停在完成确认挂起（phase=processing）
    handle.cancel()
    resolveComplete(4242) // 完成确认迟到成功
    const err: any = await p.catch((e: any) => e)
    expect(err.code).toBe('UPLOAD_CANCELLED')
    // 关键：迟到的成功不得把 phase 翻成 complete
    expect(handle.phase).toBe('cancelled')
  })

  it('cancel 后立即 retry，旧签发迟到不得污染共享凭证 → 完成确认使用新操作凭证（codex r1 P1）', async () => {
    const CRED_A = { ...CRED, credentialToken: 'CT-A-SECRET', uploadUrl: 'https://s3.private.test/a?sig=SECRET_A' }
    const CRED_B = { ...CRED, credentialToken: 'CT-B-SECRET', uploadUrl: 'https://s3.private.test/b?sig=SECRET_B' }
    let resolveA!: (v: any) => void
    let resolveB!: (v: any) => void
    let credCall = 0
    h.createUploadCredential.mockImplementation(() => {
      credCall++
      return credCall === 1 ? new Promise((r) => { resolveA = r }) : new Promise((r) => { resolveB = r })
    })
    // PUT 挂起：捕获 success，稍后手动完成（让 A 在 B 的 PUT 在途时迟到）
    let putOk!: () => void
    uni.request = vi.fn((cfg: any) => {
      putOk = () => cfg.success({ statusCode: 200 })
      return { abort: vi.fn() }
    })
    h.completeUpload.mockResolvedValue(999)
    const handle = uploadFileWithCompletion({ filePath: '/tmp/a.png', purpose: 'avatar' })
    const pA = handle.start() // op A：读文件→签发挂起
    await waitFor(() => h.createUploadCredential.mock.calls.length === 1)
    handle.cancel() // 取消 A
    const pB = handle.retry() // retry：cred 仍 null → 全量 start（op B）：签发挂起
    await waitFor(() => h.createUploadCredential.mock.calls.length === 2)
    resolveB(CRED_B) // B 先返回 → op B 提交 cred=B 并进入 PUT 挂起
    await waitFor(() => uni.request.mock.calls.length === 1)
    resolveA(CRED_A) // A 迟到返回 → 不得覆盖 op B 已提交的凭证
    const errA: any = await pA.catch((e: any) => e)
    expect(errA.code).toBe('UPLOAD_CANCELLED')
    putOk() // B 的 PUT 完成 → op B 进入完成确认，读取凭证
    await waitFor(() => h.completeUpload.mock.calls.length === 1)
    const resB = await pB
    expect(resB).toEqual({ assetId: 999 })
    // 区分力：完成确认必须用 B 的 token；旧 A 迟到不得污染共享凭证 → 绝不使用 A 的 token
    expect(h.completeUpload).toHaveBeenCalledWith('CT-B-SECRET')
    expect(h.completeUpload).not.toHaveBeenCalledWith('CT-A-SECRET')
    expect(handle.phase).toBe('complete')
  })

  it('A 直传成功后取消，B 直传失败 → retry 必须重传（新凭证不继承 A 的 putDone）（codex r2 P1）', async () => {
    const CRED_A = { ...CRED, credentialToken: 'CT-A-SECRET', uploadUrl: 'https://s3.private.test/a?sig=SECRET_A' }
    const CRED_B = { ...CRED, credentialToken: 'CT-B-SECRET', uploadUrl: 'https://s3.private.test/b?sig=SECRET_B' }
    h.createUploadCredential
      .mockResolvedValueOnce(CRED_A) // A 签发
      .mockResolvedValueOnce(CRED_B) // B 签发
    // PUT：A 成功（共享 putDone=true）；B 首次非 2xx 失败；B-retry 重传成功
    let putCall = 0
    uni.request = vi.fn((cfg: any) => {
      putCall++
      if (putCall === 1) cfg.success({ statusCode: 200 })
      else if (putCall === 2) cfg.success({ statusCode: 500 }) // 非 2xx → 直传失败 reject
      else cfg.success({ statusCode: 200 })
      return { abort: vi.fn() }
    })
    // A 的完成确认挂起：让 A 停在 processing（此刻 putDone 已为 true），再 cancel
    let resolveCompleteA!: (v: any) => void
    h.completeUpload.mockImplementation(() => new Promise((r) => { resolveCompleteA = r }))
    const handle = uploadFileWithCompletion({ filePath: '/tmp/a.png', purpose: 'avatar' })
    const pA = handle.start() // A：直传成功 → 完成确认挂起
    await waitFor(() => h.completeUpload.mock.calls.length === 1)
    handle.cancel() // 取消 A（opId++、phase=cancelled；共享 putDone 仍为 true）
    resolveCompleteA(111) // A 完成确认迟到返回 → 因 cancelled 抛 UPLOAD_CANCELLED
    await pA.catch(() => {})
    const pB = handle.start() // B：全新签发 CRED_B → 直传失败
    await expect(pB).rejects.toBeTruthy()
    expect(handle.phase).toBe('failed')
    // retry 前放行完成确认
    h.completeUpload.mockResolvedValue(222)
    const retried = await handle.retry()
    expect(retried).toEqual({ assetId: 222 })
    // 区分力：B 的对象从未直传成功，retry 必须重传 → PUT 共 3 次（A成功 + B失败 + B-retry成功）。
    // 若新凭证继承 A 遗留的 putDone=true，retry 会跳过重传（PUT 仅 2 次）并确认未上传的对象。
    expect(uni.request).toHaveBeenCalledTimes(3)
    // retry 复用 B 的凭证 token（未被 A 污染）
    expect(h.completeUpload).toHaveBeenLastCalledWith('CT-B-SECRET')
    expect(handle.phase).toBe('complete')
  })
})
