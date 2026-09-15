import axios from 'axios'
import * as FileApi from '@/api/infra/file'

/**
 * ZS-CLIENT-004：Web 管理端「统一上传完成态」编排。
 *
 * 与移动端 [uploadFileWithCompletion](../../../../zhongshu-miniapp/src/utils/uploadFile.ts) 语义一致：
 * 相较旧 [useUpload](./useUpload.ts) CLIENT 模式（getFilePresignedUrl → PUT → 异步 createFile 仅触发不等待 →
 * 立即 resolve URL，即「记录失败仍报成功」的 fire-and-forget 缺陷），本编排改用 ZS-FILE-003 的
 * upload-credential → PUT → upload-complete：
 * - 不伪报成功：对象已直传但 upload-complete 失败时整体 reject（phase=failed），绝不因对象已上传就报完成；
 * - 只生成一个资产：retry 复用同一 credentialToken，服务端一次性确认幂等；
 * - 状态可见：uploading（直传）→ processing（等待确认）→ complete / failed / cancelled；
 * - 不泄密：credentialToken / uploadUrl 全程不打印。
 *
 * 组件迁移（el-upload 由 URL 语义切换到资产 ID）与 Web E2E 归后续批次；本模块提供经单测验证的编排能力。
 */

/** 上传阶段状态机：上传中 / 处理中 / 失败 / 完成 / 取消 */
export type UploadPhase = 'idle' | 'uploading' | 'processing' | 'complete' | 'failed' | 'cancelled'

export interface UploadCompletionOptions {
  /** 浏览器文件对象（来自 el-upload 的 options.file 或 input） */
  file: File | Blob
  /** 上传用途（绑定凭证，服务端据此校验主体/用途） */
  purpose: string
  /** 原始文件名（可选，缺省从 File.name 推断，再缺省用 blob） */
  name?: string
  /** 可见范围：PRIVATE（默认，私有附件）/ PUBLIC（公开素材） */
  scope?: 'PUBLIC' | 'PRIVATE'
  /** 显式 MIME（可选，缺省用 file.type） */
  contentType?: string
  /** 声明大小（可选，缺省用 file.size） */
  size?: number
  /** 阶段变化回调 */
  onPhase?: (phase: UploadPhase) => void
  /** 直传进度回调（0-100，尽力而为） */
  onProgress?: (progress: number) => void
}

export interface UploadCompletionResult {
  /** 服务端完成确认后发布的正式资产 ID（不可用于认证的资产标识；不返回可用 URL） */
  assetId: number
}

export interface UploadCompletionHandle {
  /** 启动上传：直传 → 等待服务端完成确认 → 返回资产 ID */
  start(): Promise<UploadCompletionResult>
  /** 幂等重试：复用同一凭证重跑（未直传则重传），只生成一个资产 */
  retry(): Promise<UploadCompletionResult>
  /** 取消：中止在途直传，phase→cancelled */
  cancel(): void
  /** 当前阶段 */
  readonly phase: UploadPhase
}

/**
 * 取消错误（ZS-CLIENT-004 codex r0 P1）：异步边界发现已取消 / 本操作已被更新操作取代时抛出，
 * 保持 cancelled 语义、不 resolve 资产 ID、不覆盖 phase=cancelled。
 */
function cancelledError(): Error {
  const err = new Error('上传已取消') as any
  err.code = 'UPLOAD_CANCELLED'
  return err
}

export function uploadFileWithCompletion(options: UploadCompletionOptions): UploadCompletionHandle {
  let phase: UploadPhase = 'idle'
  let cred: FileApi.FileUploadCredentialCreateRespVO | null = null
  let putDone = false
  let cancelled = false
  let abort: (() => void) | null = null
  // codex r0 P1：操作版本标识——每次 start/retry/cancel 递增，隔离「取消后迟到响应」与「重试 vs 旧请求」：
  // 任一异步边界发现 cancelled 或本操作已被更新的操作取代（myOp !== opId）即中止，绝不覆盖 cancelled / 伪报 complete
  let opId = 0

  const fileLike = options.file as File
  const name = options.name || fileLike.name || 'blob'

  const setPhase = (p: UploadPhase) => {
    phase = p
    options.onPhase?.(p)
  }

  /** 直传文件到凭证返回的 uploadUrl（AbortController 支持 cancel 中止）；不打印 uploadUrl / 凭证 */
  function putToUploadUrl(uploadUrl: string, contentType: string): Promise<void> {
    const controller = new AbortController()
    abort = () => controller.abort()
    return axios
      .put(uploadUrl, options.file, {
        headers: { 'Content-Type': contentType },
        signal: controller.signal,
        onUploadProgress: (evt: any) =>
          options.onProgress?.(evt && evt.progress ? Math.round(evt.progress * 100) : 0)
      })
      .then(() => {
        putDone = true
        abort = null
      })
  }

  /** 等待服务端完成确认，返回资产 ID（processing → complete） */
  async function confirm(myOp: number): Promise<UploadCompletionResult> {
    setPhase('processing')
    const assetId = await FileApi.completeUpload(cred!.credentialToken)
    // codex r0 P1：completeUpload 挂起期间 cancel 后，其成功响应不得覆盖 cancelled / 伪报 complete
    if (cancelled || myOp !== opId) {
      throw cancelledError()
    }
    setPhase('complete')
    return { assetId }
  }

  async function start(): Promise<UploadCompletionResult> {
    cancelled = false
    const myOp = ++opId
    try {
      setPhase('uploading')
      const contentType = options.contentType || fileLike.type || 'application/octet-stream'
      const size = options.size ?? fileLike.size ?? 0
      cred = await FileApi.createUploadCredential({
        name,
        purpose: options.purpose,
        size,
        contentType,
        scope: options.scope
      })
      // codex r0 P1：签发凭证挂起期间被取消 → 不得继续 PUT / 完成确认
      if (cancelled || myOp !== opId) {
        throw cancelledError()
      }
      await putToUploadUrl(cred.uploadUrl, contentType)
      // 直传挂起期间被取消（abort 会 reject，此处再兜底）→ 不得继续完成确认
      if (cancelled || myOp !== opId) {
        throw cancelledError()
      }
      return await confirm(myOp)
    } catch (err) {
      // 取消优先于失败：cancel 已置 phase=cancelled，不被覆盖为 failed；被更新操作取代的旧操作也不得改 phase
      if (!cancelled && myOp === opId) setPhase('failed')
      throw err
    }
  }

  async function retry(): Promise<UploadCompletionResult> {
    // 尚无凭证（首次失败于凭证签发前）→ 全量重启
    if (!cred) return start()
    cancelled = false
    const myOp = ++opId
    try {
      setPhase('uploading')
      // 直传未完成才重传（复用同一凭证的 uploadUrl）；已完成则直接重确认
      if (!putDone) {
        const contentType = options.contentType || fileLike.type || 'application/octet-stream'
        await putToUploadUrl(cred.uploadUrl, contentType)
        if (cancelled || myOp !== opId) {
          throw cancelledError()
        }
      }
      // 复用同一 credentialToken → 服务端一次性确认 → 只生成一个资产。
      // codex r0 P2 落地依赖（登记）：首次确认「已提交但响应丢失」后，重试需后端 FILE-003 对「已完成凭证 token」
      // 返回既有资产 ID（幂等成功）方能取回；当前后端对已完成凭证返回 FILE_UPLOAD_CREDENTIAL_ALREADY_USED(1001003022)，
      // 此「同 token → 既有资产 ID」恢复语义待后端批次落地或对接确认结果查询接口（客户端重试逻辑本身已复用同一 token）。
      return await confirm(myOp)
    } catch (err) {
      if (!cancelled && myOp === opId) setPhase('failed')
      throw err
    }
  }

  function cancel(): void {
    cancelled = true
    opId++ // 作废在途操作：其迟到响应经 myOp !== opId 判定后不得覆盖 cancelled / 伪报 complete
    abort?.()
    setPhase('cancelled')
  }

  return {
    start,
    retry,
    cancel,
    get phase(): UploadPhase {
      return phase
    }
  }
}
