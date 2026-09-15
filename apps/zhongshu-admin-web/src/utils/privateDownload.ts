import * as FileApi from '@/api/infra/file'

/**
 * ZS-CLIENT-004：Web 管理端「私有下载」编排（对接 ZS-FILE-004.A 主体绑定下载会话鉴权取流）。
 *
 * 与移动端 [downloadPrivateFile](../../../zhongshu-miniapp/src/utils/download.ts) 语义一致：
 * 不接收短时/长期存储 URL——以 fileId + purpose 签发一次性票据 → 兑换下载会话 → 逐块 Range 取流组装。
 * 业务错误（过期/撤权/无权/会话失效）由请求层一致 reject，本编排映射为明确提示并 reject，
 * **绝不将错误响应当成文件内容保存**；ticketToken 全程不打印。cancel 停止取流并撤权在途会话。
 *
 * 组件接线（附件列表下载按钮切换到本编排 + 触发浏览器保存）与 Web E2E 归后续批次；
 * 本模块提供经单测验证的取流编排能力，返回组装后的字节，交由调用方决定保存/预览方式。
 */

/** 私有下载阶段状态机 */
export type DownloadPhase = 'idle' | 'issuing' | 'redeeming' | 'downloading' | 'complete' | 'failed' | 'cancelled'

export interface PrivateDownloadOptions {
  /** 资产 ID（文件编号） */
  fileId: number
  /** 下载用途（绑定票据，服务端据此校验主体/用途） */
  purpose: string
  /** 单块请求字节数（缺省 1MiB；服务端会收敛分块上限，以返回字节数推进游标） */
  chunkSize?: number
  /** 阶段回调 */
  onPhase?: (phase: DownloadPhase) => void
  /** 进度回调（已下载字节, 总字节） */
  onProgress?: (loaded: number, total: number) => void
}

export interface PrivateDownloadResult {
  /** 组装后的文件字节（内容来自后端鉴权取流，非存储 URL） */
  content: Uint8Array
  /** 已下载总字节数 */
  totalSize: number
}

export interface PrivateDownloadHandle {
  /** 启动下载：签票 → 兑换会话 → 逐块鉴权取流 → 组装字节 */
  start(): Promise<PrivateDownloadResult>
  /** 取消：停止取流并撤权下载会话 */
  cancel(): void
  /** 当前阶段 */
  readonly phase: DownloadPhase
}

/**
 * FILE-004.A 交付业务错误码 → 明确提示（验收：过期/撤权下载给出明确提示）。
 * 后端 msg 已足够清晰，此处仅作端侧友好映射与兜底；不吞掉业务错误、不将错误体存成文件。
 */
const DELIVERY_ERROR_MESSAGES: Record<number, string> = {
  1001003028: '无权下载该文件',
  1001003029: '下载会话已失效，请重试',
  1001003030: '下载已被撤权，请重新获取',
  1001003031: '下载链接已过期，请重新获取'
}

/** 将请求层 reject 的业务错误（{code,msg,traceId}）映射为携带明确提示的 Error，保留 code/traceId 可追踪 */
function toDownloadError(e: any): Error {
  const code = e?.code
  const message = (code != null && DELIVERY_ERROR_MESSAGES[code]) || e?.msg || e?.message || '下载失败'
  const err = new Error(message) as any
  if (code != null) err.code = code
  if (e?.msg != null) err.msg = e.msg
  if (e?.traceId != null) err.traceId = e.traceId
  return err
}

/** base64 → 字节（浏览器/Node 均提供全局 atob） */
function base64ToBytes(base64: string): Uint8Array {
  const binary = atob(base64)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i)
  }
  return bytes
}

/**
 * 下载不完整错误（ZS-CLIENT-004 codex r0 P2）：累计字节与声明总长不一致（缺失 / 提前结束 / 超长），
 * 或非末块返回零字节（取流停滞 / 截断）。绝不把截断内容伪报为完整成功。
 */
function incompleteDownloadError(loaded: number, declaredTotal: number): Error {
  const err = new Error('下载内容不完整，请重试') as any
  err.code = 'DOWNLOAD_INCOMPLETE'
  err.loaded = loaded
  err.expected = declaredTotal
  return err
}

export function downloadPrivateFile(options: PrivateDownloadOptions): PrivateDownloadHandle {
  let phase: DownloadPhase = 'idle'
  let cancelled = false
  let deliverySessionId: string | null = null

  const setPhase = (p: DownloadPhase) => {
    phase = p
    options.onPhase?.(p)
  }

  async function start(): Promise<PrivateDownloadResult> {
    cancelled = false
    try {
      // 1. 签发一次性交付票据（主体绑定）——ticketToken 不打印
      setPhase('issuing')
      const { ticketToken } = await FileApi.issueDeliveryTicket({
        fileId: options.fileId,
        purpose: options.purpose
      })
      // 签票挂起期间被取消：尚无会话，无需撤权，直接以 cancelled 拒绝
      if (cancelled) {
        throw new Error('下载已取消')
      }
      // 2. 原子兑换建立下载会话
      setPhase('redeeming')
      const session = await FileApi.redeemDeliveryTicket(ticketToken, options.purpose)
      deliverySessionId = session.deliverySessionId
      const declaredTotal = session.totalSize ?? 0
      // codex r0 P2：兑换挂起期间被取消时 cancel() 因 deliverySessionId 尚为空而跳过撤权；
      // 兑换返回后若已取消，立即对刚建立的会话 best-effort 撤权，杜绝活跃会话泄漏，再以 cancelled 拒绝
      if (cancelled) {
        FileApi.revokeDelivery(deliverySessionId).catch(() => {
          /* 撤权失败不掩盖取消语义 */
        })
        throw new Error('下载已取消')
      }
      // 3. 逐块鉴权取流（Range [start, endInclusive]），业务错误由请求层 reject → 映射明确提示
      setPhase('downloading')
      const chunkSize = options.chunkSize && options.chunkSize > 0 ? options.chunkSize : 1024 * 1024
      const parts: Uint8Array[] = []
      let loaded = 0
      let cursor = 0
      for (;;) {
        if (cancelled) break
        const end =
          declaredTotal > 0 ? Math.min(cursor + chunkSize - 1, declaredTotal - 1) : cursor + chunkSize - 1
        const chunk = await FileApi.readDeliveryChunk(deliverySessionId, cursor, end)
        const bytes = base64ToBytes(chunk.content ?? '')
        parts.push(bytes)
        loaded += bytes.length
        options.onProgress?.(loaded, chunk.totalSize ?? declaredTotal)
        // 服务端 last 为权威终止信号；以返回字节数推进游标（服务端可能收敛分块上限）
        if (chunk.last) break
        // codex r0 P2：非末块却零字节 = 取流停滞 / 截断，判为下载不完整（而非静默 break 后伪报成功）
        if (bytes.length === 0) {
          throw incompleteDownloadError(loaded, declaredTotal)
        }
        cursor += bytes.length
        if (declaredTotal > 0 && loaded >= declaredTotal) break
      }
      if (cancelled) {
        throw new Error('下载已取消')
      }
      // codex r0 P2：进入 complete 前校验累计长度与声明总长一致（缺失 / 提前结束 / 超长均判不完整）
      if (declaredTotal > 0 && loaded !== declaredTotal) {
        throw incompleteDownloadError(loaded, declaredTotal)
      }
      // 4. 组装完整字节
      const content = new Uint8Array(loaded)
      let offset = 0
      for (const part of parts) {
        content.set(part, offset)
        offset += part.length
      }
      setPhase('complete')
      return { content, totalSize: loaded }
    } catch (err) {
      if (cancelled) {
        if (phase !== 'cancelled') setPhase('cancelled')
        throw err instanceof Error ? err : new Error('下载已取消')
      }
      setPhase('failed')
      // 下载不完整错误已是端侧构造的明确 Error（携带 loaded/expected 诊断字段），直接透传，
      // 不再经 toDownloadError（面向后端业务错误码映射）以免丢失诊断字段
      if ((err as any)?.code === 'DOWNLOAD_INCOMPLETE') {
        throw err
      }
      throw toDownloadError(err)
    }
  }

  function cancel(): void {
    cancelled = true
    setPhase('cancelled')
    // 撤权在途下载会话（best-effort，不阻塞取消、不打印会话/票据）
    if (deliverySessionId) {
      FileApi.revokeDelivery(deliverySessionId).catch(() => {
        /* 撤权失败不掩盖取消语义 */
      })
    }
  }

  return {
    start,
    cancel,
    get phase(): DownloadPhase {
      return phase
    }
  }
}
