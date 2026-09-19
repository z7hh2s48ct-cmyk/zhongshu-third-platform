/**
 * 下载工具类 - 支持多端（H5、小程序、APP）
 */

import { isH5, isMpWeixin } from '@uni-helper/uni-env'
import { useTokenStore, useUserStore } from '@/store'
import { getEnvBaseUrl } from '@/utils'
import { stringifyQuery } from '@/http/tools/queryString'
import { openSafeUrl } from '@/utils/url'
import * as FileApi from '@/api/infra/file'

/** 下载后端接口文件 */
export async function downloadApiFile(url: string, params?: Record<string, any>, fileName?: string): Promise<void> {
  const requestUrl = buildApiDownloadUrl(url, params)
  const header = buildDownloadHeader()
  if (isH5) {
    const response = await fetch(requestUrl, { headers: header as HeadersInit })
    if (!response.ok) {
      throw new Error('下载失败')
    }
    const blob = await response.blob()
    const objectUrl = URL.createObjectURL(blob)
    try {
      await downloadFileH5(objectUrl, resolveDownloadFileName(response.headers.get('content-disposition'), fileName || 'download.xlsx'))
    } finally {
      URL.revokeObjectURL(objectUrl)
    }
    return
  }

  return new Promise((resolve, reject) => {
    uni.downloadFile({
      url: requestUrl,
      header,
      success: (res) => {
        if (res.statusCode !== 200) {
          uni.showToast({ icon: 'none', title: '下载失败' })
          reject(new Error('下载失败'))
          return
        }
        uni.openDocument({
          filePath: res.tempFilePath,
          success: () => resolve(),
          fail: reject,
        })
      },
      fail: reject,
    })
  })
}

/** 保存图片到相册 */
export async function saveImageToAlbum(url: string, fileName?: string): Promise<void> {
  if (isH5) {
    await downloadFileH5(url, fileName)
    return
  }
  // 小程序和 APP 端保存图片到相册
  return new Promise((resolve, reject) => {
    // 如果是网络图片，先下载
    if (url.startsWith('http')) {
      uni.downloadFile({
        url,
        success: (downloadResult) => {
          if (downloadResult.statusCode === 200) {
            saveToAlbum(downloadResult.tempFilePath, resolve, reject)
          } else {
            uni.showToast({ icon: 'none', title: '下载失败' })
            reject(new Error('Download failed'))
          }
        },
        fail: (err) => {
          uni.showToast({ icon: 'none', title: '下载失败' })
          reject(err)
        },
      })
    } else {
      // 本地图片直接保存
      saveToAlbum(url, resolve, reject)
    }
  })
}

/** 保存图片到相册（内部方法） */
function saveToAlbum(
  filePath: string,
  resolve: () => void,
  reject: (err: unknown) => void,
): void {
  uni.saveImageToPhotosAlbum({
    filePath,
    success: () => {
      uni.showToast({
        icon: 'success',
        title: '已保存到相册',
      })
      resolve()
    },
    fail: (err) => {
      // 微信小程序需要授权
      if (isMpWeixin && err.errMsg?.includes('auth deny')) {
        uni.showModal({
          title: '提示',
          content: '需要您授权保存相册权限',
          success: (res) => {
            if (res.confirm) {
              uni.openSetting({
                success: (settingRes) => {
                  if (settingRes.authSetting['scope.writePhotosAlbum']) {
                    // 重新尝试保存
                    saveToAlbum(filePath, resolve, reject)
                  } else {
                    reject(new Error('User denied'))
                  }
                },
              })
            } else {
              reject(new Error('User cancelled'))
            }
          },
        })
      } else {
        uni.showToast({
          icon: 'none',
          title: '保存失败',
        })
        reject(err)
      }
    },
  })
}

/** H5 端下载文件 */
async function downloadFileH5(url: string, fileName?: string): Promise<void> {
  const link = document.createElement('a')
  link.href = url
  link.download = fileName || resolveFileName(url)
  link.style.display = 'none'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

/** H5 端下载内存文件 */
export async function downloadBlobH5(blob: Blob, fileName: string): Promise<void> {
  const objectUrl = URL.createObjectURL(blob)
  try {
    await downloadFileH5(objectUrl, fileName)
  } finally {
    URL.revokeObjectURL(objectUrl)
  }
}

/** H5 端下载文本文件 */
export function downloadTextFileH5(content: string, fileName: string, mimeType = 'text/plain;charset=utf-8'): Promise<void> {
  return downloadBlobH5(new Blob([content], { type: mimeType }), fileName)
}

/** H5 端下载 SVG 文件 */
export function downloadSvgFileH5(svg: string, fileName: string): Promise<void> {
  return downloadTextFileH5(svg, fileName, 'image/svg+xml;charset=utf-8')
}

/** 清理下载文件名 */
export function sanitizeFileName(value: string) {
  return value.replace(/[^\w-]+/g, '-').replace(/^-+|-+$/g, '').slice(0, 48) || 'content'
}

/** 构造后端下载地址 */
function buildApiDownloadUrl(url: string, params?: Record<string, any>) {
  let requestUrl = url
  if (!requestUrl.startsWith('http')) {
    // #ifdef H5
    if (JSON.parse(import.meta.env.VITE_APP_PROXY_ENABLE)) {
      requestUrl = import.meta.env.VITE_APP_PROXY_PREFIX + requestUrl
    } else {
      requestUrl = getEnvBaseUrl() + requestUrl
    }
    // #endif
    // #ifndef H5
    requestUrl = getEnvBaseUrl() + requestUrl
    // #endif
  }
  if (params) {
    const query = stringifyQuery(params)
    if (query) {
      requestUrl += requestUrl.includes('?') ? `&${query}` : `?${query}`
    }
  }
  return requestUrl
}

/** 构造下载请求头 */
function buildDownloadHeader() {
  const header: Record<string, any> = {}
  const token = useTokenStore().updateNowTime().validToken
  if (token) {
    header.Authorization = `Bearer ${token}`
  }
  const tenantId = useUserStore().tenantId
  if (tenantId) {
    header['tenant-id'] = tenantId
  }
  return header
}

/** 解析下载文件名 */
function resolveDownloadFileName(contentDisposition: string | null, fallback: string) {
  if (!contentDisposition) {
    return fallback
  }
  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1])
  }
  const filenameMatch = contentDisposition.match(/filename="?([^";]+)"?/i)
  return filenameMatch?.[1] ? decodeURIComponent(filenameMatch[1]) : fallback
}

/** 从 URL 中解析文件名 */
function resolveFileName(url: string): string {
  const defaultName = 'downloaded_file'
  try {
    const pathname = new URL(url).pathname
    return pathname.slice(pathname.lastIndexOf('/') + 1) || defaultName
  } catch {
    return url.slice(url.lastIndexOf('/') + 1) || defaultName
  }
}

/** 格式化文件大小 */
export function formatFileSize(size?: number): string {
  if (!size) {
    return '-'
  }
  if (size < 1024) {
    return `${size} B`
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(2)} KB`
  }
  if (size < 1024 * 1024 * 1024) {
    return `${(size / 1024 / 1024).toFixed(2)} MB`
  }
  return `${(size / 1024 / 1024 / 1024).toFixed(2)} GB`
}

const IMAGE_FILE_EXTENSIONS = ['bmp', 'gif', 'jpeg', 'jpg', 'png', 'webp']

/** 从 URL 中解析文件扩展名 */
export function getFileExtFromUrl(url?: string): string {
  const fileName = getFileNameFromUrl(url)
  const extIndex = fileName.lastIndexOf('.')
  return extIndex > -1 ? fileName.slice(extIndex + 1).toLowerCase() : ''
}

/** 判断是否图片文件 */
export function isImageFile(url?: string): boolean {
  return IMAGE_FILE_EXTENSIONS.includes(getFileExtFromUrl(url))
}

/** 从 URL 中解析文件名 */
export function getFileNameFromUrl(url?: string): string {
  const cleanUrl = String(url || '').split(/[?#]/)[0]
  const fileName = cleanUrl.slice(cleanUrl.lastIndexOf('/') + 1)
  try {
    return decodeURIComponent(fileName)
  } catch {
    return fileName
  }
}

/** 打开附件：图片预览，其他文件 H5 新窗口打开，非 H5 下载后用系统能力打开 */
export function openAttachment(url?: string) {
  if (!url) {
    return
  }
  const fullUrl = staticUrl(url)
  if (isImageFile(fullUrl)) {
    uni.previewImage({
      urls: [fullUrl],
      current: fullUrl,
    })
    return
  }
  // #ifdef H5
  openSafeUrl(fullUrl)
  // #endif
  // #ifndef H5
  uni.showLoading({
    title: '打开中...',
    mask: true,
  })
  uni.downloadFile({
    url: fullUrl,
    success: (res) => {
      if (res.statusCode && res.statusCode !== 200) {
        uni.hideLoading()
        uni.showToast({
          icon: 'none',
          title: '附件下载失败',
        })
        return
      }
      const fileType = getFileExtFromUrl(fullUrl)
      uni.openDocument({
        filePath: res.tempFilePath,
        ...(fileType ? { fileType } : {}),
        complete: () => {
          uni.hideLoading()
        },
        fail: () => {
          uni.showToast({
            icon: 'none',
            title: '附件打开失败',
          })
        },
      })
    },
    fail: () => {
      uni.hideLoading()
      uni.showToast({
        icon: 'none',
        title: '附件下载失败',
      })
    },
  })
  // #endif
}

/**
 * 获取静态资源完整 URL 地址
 * @param path 资源路径
 * @returns 完整的静态资源 URL 地址
 */
export function staticUrl(path: string): string {
  if (/^https?:\/\//.test(path) || path.startsWith('blob:') || path.startsWith('data:')) {
    return path
  }
  const baseUrl = import.meta.env.VITE_STATIC_BASEURL || ''
  // 确保 path 以 / 开头
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${baseUrl}${normalizedPath}`
}

/**
 * 打开文件：H5 浏览器新窗口打开/下载；其他端下载后用系统能力打开
 * @param url 文件地址
 */
export function openFile(url?: string) {
  if (!url) {
    uni.showToast({ title: '文件地址为空', icon: 'none' })
    return
  }
  // #ifdef H5
  window.open(url)
  // #endif
  // #ifndef H5
  uni.showLoading({ title: '打开中...' })
  uni.downloadFile({
    url,
    success: (res) => {
      if (res.statusCode === 200) {
        uni.openDocument({ filePath: res.tempFilePath, showMenu: true })
      }
    },
    complete: () => uni.hideLoading(),
  })
  // #endif
}

/* ==================== ZS-CLIENT-004：私有下载（对接 ZS-FILE-004.A 主体绑定下载会话鉴权取流） ==================== */

/** 私有下载阶段状态机 */
export type DownloadPhase = 'idle' | 'issuing' | 'redeeming' | 'downloading' | 'complete' | 'failed' | 'cancelled'

export interface PrivateDownloadOptions {
  /** 资产 ID（文件编号） */
  fileId: string
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
  1001003031: '下载链接已过期，请重新获取',
}

/** 将 http 层 reject 的业务错误（{code,msg,traceId}）映射为携带明确提示的 Error，保留 code/traceId 可追踪 */
function toDownloadError(e: any): Error {
  const code = e?.code
  const message = (code != null && DELIVERY_ERROR_MESSAGES[code]) || e?.msg || e?.message || '下载失败'
  const err = new Error(message) as any
  if (code != null) err.code = code
  if (e?.msg != null) err.msg = e.msg
  if (e?.traceId != null) err.traceId = e.traceId
  return err
}

/** base64 → 字节：小程序用 uni.base64ToArrayBuffer，H5/node 用 atob */
function base64ToBytes(base64: string): Uint8Array {
  if (typeof uni !== 'undefined' && typeof uni.base64ToArrayBuffer === 'function') {
    return new Uint8Array(uni.base64ToArrayBuffer(base64))
  }
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

/**
 * ZS-CLIENT-004：私有下载——按 ZS-FILE-004.A 主体绑定下载会话走后端鉴权取流。
 *
 * <p>不接收短时/长期存储 URL：以 fileId + purpose 签发一次性票据 → 兑换下载会话 → 逐块 Range 取流组装。
 * 业务错误（过期/撤权/无权/会话失效）由 http 层一致 reject，本编排映射为明确提示并 reject，
 * <b>绝不将错误响应当成文件内容保存</b>；ticketToken 全程不打印。cancel 停止取流并撤权在途会话。</p>
 */
export function downloadPrivateFile(options: PrivateDownloadOptions): PrivateDownloadHandle {
  let phase: DownloadPhase = 'idle'
  let cancelled = false
  let deliverySessionId: string | null = null
  // codex r1 P2：操作版本标识——每次 start/cancel 递增，隔离「取消后旧兑换/取流迟到」与「同 handle 再次 start 的新操作」：
  // 被取代的旧操作只对自身会话补撤权、绝不写共享 deliverySessionId / 继续取流 / 改写 phase
  let opId = 0

  const setPhase = (p: DownloadPhase) => {
    phase = p
    options.onPhase?.(p)
  }

  // codex r2 P2：已撤权会话集合——对同一会话的撤权去重。会话撤权可能由多条路径触发
  // （cancel() 撤共享会话 / 操作在 catch 中撤自身会话），去重确保「至少撤一次、至多撤一次」，
  // 既杜绝被取代操作的孤儿会话泄漏，又避免与 cancel() 对同一会话重复撤权。
  const revokedSessions = new Set<string>()

  /** 幂等撤权下载会话：同一会话仅撤一次（best-effort，不打印会话/票据、撤权失败不掩盖主流程语义） */
  function revokeOnce(sessionId: string): void {
    if (revokedSessions.has(sessionId)) return
    revokedSessions.add(sessionId)
    FileApi.revokeDelivery(sessionId).catch(() => { /* 撤权失败不掩盖取消/主流程语义 */ })
  }

  async function start(): Promise<PrivateDownloadResult> {
    cancelled = false
    const myOp = ++opId
    // codex r1 P2：本操作私有的下载会话——仅获胜操作提交共享 deliverySessionId，
    // 被取消/被取代的旧操作只对自身会话补撤权，绝不写共享状态或继续取流（否则会污染新操作）
    let mySessionId: string | null = null
    try {
      // 1. 签发一次性交付票据（主体绑定）——ticketToken 不打印
      setPhase('issuing')
      const { ticketToken } = await FileApi.issueDeliveryTicket({ fileId: options.fileId, purpose: options.purpose })
      // 签票挂起期间被取消 / 被更新操作取代：尚无会话，无需撤权，直接以 cancelled 拒绝
      if (cancelled || myOp !== opId) {
        throw new Error('下载已取消')
      }
      // 2. 原子兑换建立下载会话
      setPhase('redeeming')
      const session = await FileApi.redeemDeliveryTicket(ticketToken, options.purpose)
      mySessionId = session.deliverySessionId
      const declaredTotal = session.totalSize ?? 0
      // codex r0/r1/r2 P2：兑换挂起期间被取消或被更新操作取代时，本操作会话尚未提交共享 deliverySessionId，
      // cancel() 无从撤权；此处直接抛出，由 catch 统一对本操作会话（mySessionId）幂等补撤权，杜绝活跃会话泄漏
      if (cancelled || myOp !== opId) {
        throw new Error('下载已取消')
      }
      // 仅获胜操作提交共享会话（供 cancel() 撤权）
      deliverySessionId = mySessionId
      // 3. 逐块鉴权取流（Range [start, endInclusive]），业务错误由 http 层 reject → 映射明确提示
      setPhase('downloading')
      const chunkSize = options.chunkSize && options.chunkSize > 0 ? options.chunkSize : 1024 * 1024
      const parts: Uint8Array[] = []
      let loaded = 0
      let cursor = 0
      for (;;) {
        if (cancelled || myOp !== opId) break
        const end = declaredTotal > 0 ? Math.min(cursor + chunkSize - 1, declaredTotal - 1) : cursor + chunkSize - 1
        const chunk = await FileApi.readDeliveryChunk(mySessionId, cursor, end)
        // codex r1 P2：取流期间被取消/被取代 → 立即停止（不得据迟到块推进游标 / 伪报成功）
        if (cancelled || myOp !== opId) break
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
      if (cancelled || myOp !== opId) {
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
      // codex r2 P2：本操作若已建立会话（mySessionId），任何异常退出（取消 / 被更新操作取代 / 失败）都幂等撤权自身会话。
      // 关键补口：取流途中被「直接再次 start」取代（未经 cancel）时，共享 deliverySessionId 已被新操作覆盖，
      // 旧会话失去共享跟踪 → 必须由本操作自行撤权，否则永久孤儿泄漏（cancel() 此后只能撤新会话）。
      // revokeOnce 去重确保与 cancel() 对同一会话不重复撤权。
      if (mySessionId) {
        revokeOnce(mySessionId)
      }
      // codex r1 P2：被更新操作取代的旧操作不得改写共享 phase（新操作拥有 phase），仅以自身取消语义透传
      if (myOp !== opId) {
        throw err instanceof Error ? err : new Error('下载已取消')
      }
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
    opId++ // codex r1 P2：作废在途操作——其迟到兑换/取流经 myOp !== opId 判定后只撤权自身会话、不写共享状态、不改 phase
    setPhase('cancelled')
    // 撤权在途下载会话（best-effort，不阻塞取消、不打印会话/票据）；revokeOnce 与操作 catch 的自撤权去重
    if (deliverySessionId) {
      revokeOnce(deliverySessionId)
    }
  }

  return {
    start,
    cancel,
    get phase(): DownloadPhase { return phase },
  }
}
