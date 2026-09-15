/**
 * 文件上传工具
 *
 * 支持两种上传模式：
 * - server: 后端上传（默认）
 * - client: 前端直连上传（仅支持 S3 服务）
 *
 * 通过环境变量 VITE_UPLOAD_TYPE 配置
 */

import { useToast } from '@wot-ui/ui/components/wd-toast'
import * as FileApi from '@/api/infra/file'

/** 上传类型 */
export enum UploadType {
  /** 客户端直接上传（只支持 S3 服务） */
  CLIENT = 'client',
  /** 客户端发送到后端上传 */
  SERVER = 'server',
}

/**
 * 读取文件二进制内容（按路径，兼容 微信/App 文件系统 与 H5 blob）
 * @param filePath 文件路径 / blob URL
 * @returns 二进制内容；H5 下额外返回探测到的真实 MIME
 */
async function readFile(filePath: string): Promise<{ buffer: ArrayBuffer, mime?: string }> {
  // 微信小程序 / App：通过文件系统从路径读取
  if (uni.getFileSystemManager) {
    const buffer = uni.getFileSystemManager().readFileSync(filePath) as ArrayBuffer
    return { buffer }
  }
  // H5：从 blob / 临时 URL 拉取二进制，并顺带拿到真实 MIME
  const response = await fetch(filePath)
  const blob = await response.blob()
  return { buffer: await blob.arrayBuffer(), mime: blob.type || undefined }
}

/**
 * 创建文件记录（异步）
 * @param presignedInfo 预签名信息
 * @param file 文件信息
 */
function createFileRecord(presignedInfo: FileApi.FilePresignedUrlRespVO, file: { name: string, type?: string, size?: number }) {
  const fileVo: FileApi.FileCreateReqVO = {
    configId: presignedInfo.configId,
    url: presignedInfo.url,
    path: presignedInfo.path,
    name: file.name,
    type: file.type,
    size: file.size,
  }
  FileApi.createFile(fileVo).catch((err) => {
    console.error('创建文件记录失败:', err, fileVo)
  })
}

/**
 * 从文件路径上传文件（纯文件上传）
 * @param filePath 文件路径
 * @param directory 目录（可选）
 * @param fileType 显式指定的 MIME 类型（可选）
 * @param fileName 原始文件名（可选；H5 的 blob 路径不含文件名/扩展名时需传入，否则 S3 key 会丢扩展名）
 * @returns 文件访问 URL
 */
export async function uploadFileFromPath(
  filePath: string,
  directory?: string,
  fileType?: string,
  fileName?: string,
  onProgress?: (progress: number) => void,
): Promise<string> {
  // 优先用传入的原始文件名（H5 的 blob:xxx 路径推不出文件名/扩展名）
  const name = fileName || (filePath.includes('/') ? filePath.substring(filePath.lastIndexOf('/') + 1) : filePath)
  const uploadType = import.meta.env.VITE_UPLOAD_TYPE || UploadType.SERVER

  // 情况一：前端直连上传（仅 S3）
  if (uploadType === UploadType.CLIENT) {
    // 1.1 获取文件预签名地址
    const presignedInfo = await FileApi.getFilePresignedUrl(name, directory)

    // 1.2 读取二进制内容（H5 可顺带拿到真实 MIME）
    const { buffer, mime } = await readFile(filePath)
    // Content-Type 优先级：显式传入 > H5 探测 > 文件后缀推断
    const contentType = fileType || mime || getMimeType(name)

    // 返回上传的 Promise
    return new Promise((resolve, reject) => {
      // 1.3 上传到 S3
      uni.request({
        url: presignedInfo.uploadUrl,
        method: 'PUT',
        header: {
          'Content-Type': contentType,
        },
        data: buffer,
        success: (res) => {
          // uni.request 对 HTTP 4xx/5xx 也会进 success，需显式判断状态码，否则会误判上传成功
          if (res.statusCode >= 200 && res.statusCode < 300) {
            // 1.4. 记录文件信息到后端（异步）
            createFileRecord(presignedInfo, { name, type: contentType })
            // 1.5 返回文件访问 URL
            resolve(presignedInfo.url)
          } else {
            console.error('上传到S3失败:', res.statusCode, presignedInfo)
            reject(new Error(`上传失败（HTTP ${res.statusCode}）`))
          }
        },
        fail: (err) => {
          console.error('上传到S3失败:', err, presignedInfo)
          reject(err)
        },
      })
    })
  } else {
    // 情况二：后端上传
    return FileApi.uploadFile(filePath, directory, onProgress)
  }
}

/** 根据文件名获取 MIME 类型 */
function getMimeType(fileName: string): string {
  const ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase()
  const mimeTypes: Record<string, string> = {
    jpg: 'image/jpeg',
    jpeg: 'image/jpeg',
    png: 'image/png',
    gif: 'image/gif',
    webp: 'image/webp',
    bmp: 'image/bmp',
    svg: 'image/svg+xml',
    mp4: 'video/mp4',
    mov: 'video/quicktime',
    avi: 'video/x-msvideo',
    pdf: 'application/pdf',
    doc: 'application/msword',
    docx: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    xls: 'application/vnd.ms-excel',
    xlsx: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  }
  return mimeTypes[ext] || 'application/octet-stream'
}

export interface UploadOptions {
  /** 最大可选择的图片数量，默认为1 */
  count?: number
  /** 所选的图片的尺寸，original-原图，compressed-压缩图 */
  sizeType?: Array<'original' | 'compressed'>
  /** 选择图片的来源，album-相册，camera-相机 */
  sourceType?: Array<'album' | 'camera'>
  /** 文件大小限制，单位：MB */
  maxSize?: number //
  /** 上传进度回调函数 */
  onProgress?: (progress: number) => void
  /** 上传成功回调函数 */
  onSuccess?: (res: Record<string, any>) => void
  /** 上传失败回调函数 */
  onError?: (err: Error | UniApp.GeneralCallbackResult) => void
  /** 上传完成回调函数（无论成功失败） */
  onComplete?: () => void
}

/**
 * 文件上传钩子函数（带 formData）
 * @template T 上传成功后返回的数据类型
 * @param url 上传地址
 * @param formData 额外的表单数据
 * @param options 上传选项
 * @returns 上传状态和控制对象
 */
export function useUpload<T = string>(url: string, formData: Record<string, any> = {}, options: UploadOptions = {},
  /** 直接传入文件路径，跳过选择器 */
  directFilePath?: string) {
  /** 上传中状态 */
  const loading = ref(false)
  /** 上传错误状态 */
  const error = ref(false)
  /** 上传成功后的响应数据 */
  const data = ref<T>()
  /** 上传进度（0-100） */
  const progress = ref(0)
  const toast = useToast()

  /** 解构上传选项，设置默认值 */
  const {
    /** 最大可选择的图片数量 */
    count = 1,
    /** 所选的图片的尺寸 */
    sizeType = ['original', 'compressed'],
    /** 选择图片的来源 */
    sourceType = ['album', 'camera'],
    /** 文件大小限制（MB） */
    maxSize = 10,
    /** 进度回调 */
    onProgress,
    /** 成功回调 */
    onSuccess,
    /** 失败回调 */
    onError,
    /** 完成回调 */
    onComplete,
  } = options

  /**
   * 检查文件大小是否超过限制
   * @param size 文件大小（字节）
   * @returns 是否通过检查
   */
  const checkFileSize = (size: number) => {
    const sizeInMB = size / 1024 / 1024
    if (sizeInMB > maxSize) {
      // 注释 by 芋艿：使用 wd-toast 替代
      // uni.showToast({
      //   title: `文件大小不能超过${maxSize}MB`,
      //   icon: 'none',
      // })
      toast.show(`文件大小不能超过${maxSize}MB`)
      return false
    }
    return true
  }
  /**
   * 触发文件选择和上传
   * 根据平台使用不同的选择器：
   * - 微信小程序使用 chooseMedia
   * - 其他平台使用 chooseImage
   */
  const run = () => {
    if (directFilePath) {
      // 直接使用传入的文件路径
      loading.value = true
      progress.value = 0
      uploadFile<T>({
        url,
        tempFilePath: directFilePath,
        formData,
        data,
        error,
        loading,
        progress,
        onProgress,
        onSuccess,
        onError,
        onComplete,
      })
      return
    }

    // #ifdef MP-WEIXIN
    // 微信小程序环境下使用 chooseMedia API
    uni.chooseMedia({
      count,
      mediaType: ['image'], // 仅支持图片类型
      sourceType,
      success: (res) => {
        const file = res.tempFiles[0]
        // 检查文件大小是否符合限制
        if (!checkFileSize(file.size))
          return

        // 开始上传
        loading.value = true
        progress.value = 0
        uploadFile<T>({
          url,
          tempFilePath: file.tempFilePath,
          formData,
          data,
          error,
          loading,
          progress,
          onProgress,
          onSuccess,
          onError,
          onComplete,
        })
      },
      fail: (err) => {
        console.error('选择媒体文件失败:', err)
        error.value = true
        onError?.(err)
      },
    })
    // #endif

    // #ifndef MP-WEIXIN
    // 非微信小程序环境下使用 chooseImage API
    uni.chooseImage({
      count,
      sizeType,
      sourceType,
      success: (res) => {
        console.log('选择图片成功:', res)

        // 开始上传
        loading.value = true
        progress.value = 0
        uploadFile<T>({
          url,
          tempFilePath: res.tempFilePaths[0],
          formData,
          data,
          error,
          loading,
          progress,
          onProgress,
          onSuccess,
          onError,
          onComplete,
        })
      },
      fail: (err) => {
        console.error('选择图片失败:', err)
        error.value = true
        onError?.(err)
      },
    })
    // #endif
  }

  return { loading, error, data, progress, run }
}

/**
 * 文件上传选项接口
 * @template T 上传成功后返回的数据类型
 */
interface UploadFileOptions<T> {
  /** 上传地址 */
  url: string
  /** 临时文件路径 */
  tempFilePath: string
  /** 额外的表单数据 */
  formData: Record<string, any>
  /** 上传成功后的响应数据 */
  data: Ref<T | undefined>
  /** 上传错误状态 */
  error: Ref<boolean>
  /** 上传中状态 */
  loading: Ref<boolean>
  /** 上传进度（0-100） */
  progress: Ref<number>
  /** 上传进度回调 */
  onProgress?: (progress: number) => void
  /** 上传成功回调 */
  onSuccess?: (res: Record<string, any>) => void
  /** 上传失败回调 */
  onError?: (err: Error | UniApp.GeneralCallbackResult) => void
  /** 上传完成回调 */
  onComplete?: () => void
}

/**
 * 执行文件上传（带 formData）
 * @template T 上传成功后返回的数据类型
 * @param options 上传选项
 */
function uploadFile<T>({
  url,
  tempFilePath,
  formData,
  data,
  error,
  loading,
  progress,
  onProgress,
  onSuccess,
  onError,
  onComplete,
}: UploadFileOptions<T>) {
  try {
    // 创建上传任务
    const uploadTask = uni.uploadFile({
      url,
      filePath: tempFilePath,
      name: 'file', // 文件对应的 key
      formData,
      header: {
        // H5环境下不需要手动设置Content-Type，让浏览器自动处理multipart格式
        // #ifndef H5
        'Content-Type': 'multipart/form-data',
        // #endif
      },
      // 确保文件名称合法
      success: (uploadFileRes) => {
        console.log('上传文件成功:', uploadFileRes)
        try {
          // 解析响应数据
          const { data: _data } = JSON.parse(uploadFileRes.data)
          // 上传成功
          data.value = _data as T
          onSuccess?.(_data)
        } catch (err) {
          // 响应解析错误
          console.error('解析上传响应失败:', err)
          error.value = true
          onError?.(new Error('上传响应解析失败'))
        }
      },
      fail: (err) => {
        // 上传请求失败
        console.error('上传文件失败:', err)
        error.value = true
        onError?.(err)
      },
      complete: () => {
        // 无论成功失败都执行
        loading.value = false
        onComplete?.()
      },
    })

    // 监听上传进度
    uploadTask.onProgressUpdate((res) => {
      progress.value = res.progress
      onProgress?.(res.progress)
    })
  } catch (err) {
    // 创建上传任务失败
    console.error('创建上传任务失败:', err)
    error.value = true
    loading.value = false
    onError?.(new Error('创建上传任务失败'))
  }
}

/* ==================== ZS-CLIENT-004：统一上传完成态（等待服务端确认 → 资产 ID） ==================== */

/** 上传阶段状态机：上传中 / 处理中 / 失败 / 完成 / 取消 */
export type UploadPhase = 'idle' | 'uploading' | 'processing' | 'complete' | 'failed' | 'cancelled'

export interface UploadCompletionOptions {
  /** 本地文件路径 / blob URL */
  filePath: string
  /** 上传用途（绑定凭证，服务端据此校验主体/用途） */
  purpose: string
  /** 原始文件名（可选，缺省从路径推断） */
  name?: string
  /** 可见范围：PRIVATE（默认，私有附件）/ PUBLIC（公开素材） */
  scope?: 'PUBLIC' | 'PRIVATE'
  /** 显式 MIME（可选，缺省按 H5 探测 / 后缀推断） */
  contentType?: string
  /** 声明大小（可选，缺省用读到的字节长度） */
  size?: number
  /** 阶段变化回调 */
  onPhase?: (phase: UploadPhase) => void
  /** 直传进度回调（0-100，尽力而为，取决于平台能力） */
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

/**
 * ZS-CLIENT-004：统一「上传完成态」编排——Web/移动一致等待服务端完成确认后返回资产 ID。
 *
 * 相较旧直传（getFilePresignedUrl → PUT → 异步 createFileRecord 仅 catch 日志 → 立即 resolve URL），
 * 本编排改用 ZS-FILE-003 的 upload-credential → PUT → upload-complete：
 * - 不伪报成功：对象已直传但 upload-complete 失败时整体 reject（phase=failed），绝不因对象已上传就报完成；
 * - 只生成一个资产：retry 复用同一 credentialToken，服务端一次性确认幂等；
 * - 状态可见：uploading（直传）→ processing（等待确认）→ complete / failed / cancelled；
 * - 不泄密：credentialToken / uploadUrl 全程不打印。
 */
export function uploadFileWithCompletion(options: UploadCompletionOptions): UploadCompletionHandle {
  let phase: UploadPhase = 'idle'
  let cred: FileApi.FileUploadCredentialCreateRespVO | null = null
  let putDone = false
  let cancelled = false
  let putTask: { abort?: () => void, onProgressUpdate?: (cb: (res: any) => void) => void } | null = null
  // codex r0 P1：操作版本标识——每次 start/retry/cancel 递增，隔离「取消后迟到响应」与「重试 vs 旧请求」：
  // 任一异步边界发现 cancelled 或本操作已被更新操作取代（myOp !== opId）即中止，绝不覆盖 cancelled / 伪报 complete
  let opId = 0

  const name = options.name
    || (options.filePath.includes('/') ? options.filePath.substring(options.filePath.lastIndexOf('/') + 1) : options.filePath)

  const setPhase = (p: UploadPhase) => {
    phase = p
    options.onPhase?.(p)
  }

  /**
   * 直传字节到凭证返回的 uploadUrl（捕获 task 以支持 cancel 中止）；不打印 uploadUrl / 凭证。
   * codex r1 P1：绑定到具体操作（myOp）——仅当前获胜操作登记 putTask / 置 putDone，
   * 被取代的旧操作即便迟到完成也不得改写共享直传状态（否则会污染新操作的重试判定）。
   */
  function putToUploadUrl(myOp: number, uploadUrl: string, buffer: ArrayBuffer, contentType: string): Promise<void> {
    return new Promise((resolve, reject) => {
      const task = uni.request({
        url: uploadUrl,
        method: 'PUT',
        header: { 'Content-Type': contentType },
        data: buffer,
        success: (res: any) => {
          // uni.request 对 4xx/5xx 也进 success，须显式判定状态码，否则会误判直传成功
          if (res.statusCode >= 200 && res.statusCode < 300) {
            if (myOp === opId) {
              putDone = true
              putTask = null
            }
            resolve()
          } else {
            reject(new Error(`直传失败（HTTP ${res.statusCode}）`))
          }
        },
        fail: (err: any) => reject(err),
      }) as any
      // codex r1 P1：仅当前获胜操作登记 putTask（供 cancel abort）+ 进度回调；被取代的旧操作不得改写共享直传状态
      if (myOp === opId) {
        putTask = task
        // 尽力而为的进度（部分平台 request 任务支持 onProgressUpdate）
        putTask?.onProgressUpdate?.((res: any) => options.onProgress?.(res.progress))
      }
    })
  }

  /**
   * 等待服务端完成确认，返回资产 ID（processing → complete）。
   * codex r1 P1：显式绑定本操作的 credentialToken（而非读共享 cred），杜绝旧操作迟到污染共享 cred 后
   * 新操作误用旧凭证确认（旧凭证对应对象未直传 → 确认失败且 putDone 已真 → 重试持续确认旧凭证不可恢复）。
   */
  async function confirm(myOp: number, credentialToken: string): Promise<UploadCompletionResult> {
    setPhase('processing')
    const assetId = await FileApi.completeUpload(credentialToken)
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
      const { buffer, mime } = await readFile(options.filePath)
      // codex r0 P1：读文件挂起期间被取消 → 不再签发凭证 / 直传
      if (cancelled || myOp !== opId) {
        throw cancelledError()
      }
      const contentType = options.contentType || mime || getMimeType(name)
      const size = options.size ?? buffer.byteLength
      // codex r1 P1：先存局部变量，通过操作版本校验后才提交共享 cred——
      // 杜绝「取消后旧签发迟到」覆盖新操作已提交的凭证（旧请求与新 retry 隔离）
      const myCred = await FileApi.createUploadCredential({
        name,
        purpose: options.purpose,
        size,
        contentType,
        scope: options.scope,
      })
      // codex r0 P1：签发凭证挂起期间被取消 / 被更新操作取代 → 不得继续直传 / 完成确认，也不提交共享 cred
      if (cancelled || myOp !== opId) {
        throw cancelledError()
      }
      cred = myCred
      await putToUploadUrl(myOp, myCred.uploadUrl, buffer, contentType)
      // 直传挂起期间被取消（abort 会 reject，此处再兜底）→ 不得继续完成确认
      if (cancelled || myOp !== opId) {
        throw cancelledError()
      }
      return await confirm(myOp, myCred.credentialToken)
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
    // codex r1 P1：快照本操作复用的凭证与直传状态，全程绑定 myCred，不读可能被并发操作改写的共享 cred/putDone
    const myCred = cred
    const myPutDone = putDone
    try {
      setPhase('uploading')
      // 直传未完成才重传（复用同一凭证的 uploadUrl）；已完成则直接重确认
      if (!myPutDone) {
        const { buffer, mime } = await readFile(options.filePath)
        if (cancelled || myOp !== opId) {
          throw cancelledError()
        }
        const contentType = options.contentType || mime || getMimeType(name)
        await putToUploadUrl(myOp, myCred.uploadUrl, buffer, contentType)
        if (cancelled || myOp !== opId) {
          throw cancelledError()
        }
      }
      // 复用同一 credentialToken → 服务端一次性确认 → 只生成一个资产。
      // codex r0 P2 落地依赖（登记）：首次确认「已提交但响应丢失」后，重试需后端 FILE-003 对「已完成凭证 token」
      // 返回既有资产 ID（幂等成功）方能取回；当前后端对已完成凭证返回 FILE_UPLOAD_CREDENTIAL_ALREADY_USED(1001003022)，
      // 此「同 token → 既有资产 ID」恢复语义待后端批次落地或对接确认结果查询接口（客户端重试逻辑本身已复用同一 token）。
      return await confirm(myOp, myCred.credentialToken)
    } catch (err) {
      if (!cancelled && myOp === opId) setPhase('failed')
      throw err
    }
  }

  function cancel(): void {
    cancelled = true
    opId++ // 作废在途操作：其迟到响应经 myOp !== opId 判定后不得覆盖 cancelled / 伪报 complete
    putTask?.abort?.()
    setPhase('cancelled')
  }

  return {
    start,
    retry,
    cancel,
    get phase(): UploadPhase { return phase },
  }
}
