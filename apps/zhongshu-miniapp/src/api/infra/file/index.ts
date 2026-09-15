import type { PageParam, PageResult } from '@/http/types'
import { http } from '@/http/http'
import { useTokenStore } from '@/store/token'
import { useUserStore } from '@/store/user'
import { getEnvBaseUrl } from '@/utils'

/** 文件信息 */
export interface FileVO {
  id?: number
  configId?: number
  path: string
  name?: string
  url?: string
  size?: number
  type?: string
  createTime?: Date
}

/** 文件预签名信息 */
export interface FilePresignedUrlRespVO {
  configId: number // 配置编号
  uploadUrl: string // 文件上传 URL
  url: string // 文件访问 URL
  path: string // 文件路径
}

/** 创建文件请求 */
export interface FileCreateReqVO {
  configId: number
  url: string
  path: string
  name: string
  type?: string
  size?: number
}

/** 获取文件预签名地址 */
export function getFilePresignedUrl(name: string, directory?: string) {
  return http.get<FilePresignedUrlRespVO>('/infra/file/presigned-url', { name, directory })
}

/** 创建文件记录 */
export function createFile(data: FileCreateReqVO) {
  return http.post<string>('/infra/file/create', data)
}

/** 获取文件分页列表 */
export function getFilePage(params: PageParam) {
  return http.get<PageResult<FileVO>>('/infra/file/page', params)
}

/** 获取文件详情 */
export function getFile(id: number) {
  return http.get<FileVO>(`/infra/file/get?id=${id}`)
}

/** 删除文件 */
export function deleteFile(id: number) {
  return http.delete(`/infra/file/delete?id=${id}`)
}

/**
 * 上传文件到后端
 *
 * @param filePath 本地文件路径
 * @param directory 目录（可选）
 * @returns 文件访问 URL
 */
export async function uploadFile(
  filePath: string,
  directory?: string,
  onProgress?: (progress: number) => void,
): Promise<string> {
  const userStore = useUserStore()
  const token = await useTokenStore().tryGetValidToken()
  return new Promise((resolve, reject) => {
    const task = uni.uploadFile({
      url: `${getEnvBaseUrl()}/infra/file/upload`,
      filePath,
      name: 'file',
      header: {
        'Accept': '*/*',
        'tenant-id': userStore.tenantId,
        'Authorization': `Bearer ${token}`,
      },
      formData: directory ? { directory } : undefined,
      success: (res) => {
        if (res.statusCode === 200) {
          const result = JSON.parse(res.data)
          if (result.code === 0) {
            resolve(result.data)
          } else {
            reject(new Error(result.msg || '上传失败'))
          }
        } else {
          reject(new Error('上传失败'))
        }
      },
      fail: (err) => {
        console.error('上传失败：', err)
        reject(err)
      },
    })
    task.onProgressUpdate?.(event => onProgress?.(event.progress))
  })
}

/* ==================== ZS-CLIENT-004：对接 FILE-003 直传完成确认 / FILE-004.A 主体绑定下载会话 ==================== */

/**
 * ZS-FILE-003：直传凭证创建请求。
 * scope=PRIVATE（默认）为私有附件，PUBLIC 为公开素材。
 */
export interface FileUploadCredentialCreateReqVO {
  name: string
  purpose: string
  size?: number
  contentType?: string
  scope?: 'PUBLIC' | 'PRIVATE'
}

/**
 * ZS-FILE-003：直传凭证创建响应。
 * 安全：credentialToken / uploadUrl 为敏感直传凭据，禁止打印或回显给日志（CLIENT-004：不打印预签名 URL/凭证）。
 */
export interface FileUploadCredentialCreateRespVO {
  credentialToken: string
  uploadUrl: string
  tempPath: string
  expiresTime: string
}

/** ZS-FILE-004.A：交付票据签发请求（主体绑定，fileId + 用途）。 */
export interface FileDeliveryTicketIssueReqVO {
  fileId: number
  purpose: string
}

/** ZS-FILE-004.A：交付票据签发响应。安全：ticketToken 仅返回一次，禁止打印。 */
export interface FileDeliveryTicketIssueRespVO {
  ticketToken: string
}

/** ZS-FILE-004.A：兑换票据后建立的下载会话。 */
export interface FileDeliverySessionRespVO {
  deliverySessionId: string
  totalSize: number
}

/** ZS-FILE-004.A：鉴权取流分块。content 为后端 byte[] 经 JSON 序列化的 base64 串；last 标记末块。 */
export interface FileDeliveryChunkRespVO {
  content: string
  totalSize: number
  last: boolean
}

/** ZS-FILE-003：创建预签名直传凭证（凭据仅可写临时区，绑定主体/有效期）。 */
export function createUploadCredential(data: FileUploadCredentialCreateReqVO) {
  return http.post<FileUploadCredentialCreateRespVO>('/infra/file/upload-credential', data)
}

/**
 * ZS-FILE-003：直传完成确认——服务端核验临时对象后发布正式资产，返回资产 ID（一次性确认，幂等）。
 * CLIENT-004：上传必须等待本确认成功才返回资产 ID；确认失败即整体失败，绝不因对象已直传就报成功。
 */
export function completeUpload(credentialToken: string) {
  return http.post<number>('/infra/file/upload-complete', { credentialToken })
}

/** ZS-FILE-004.A：签发一次性交付票据（本人主体绑定）。 */
export function issueDeliveryTicket(data: FileDeliveryTicketIssueReqVO) {
  return http.post<FileDeliveryTicketIssueRespVO>('/infra/file/delivery/issue', data)
}

/** ZS-FILE-004.A：原子兑换票据建立下载会话（同主体同登录会话重复兑换幂等）。 */
export function redeemDeliveryTicket(ticketToken: string, purpose: string) {
  return http.post<FileDeliverySessionRespVO>('/infra/file/delivery/redeem', undefined, { ticketToken, purpose })
}

/** ZS-FILE-004.A：鉴权取流分块，Range 语义 [start, endInclusive]，逐块重检身份/会话/撤权/过期。 */
export function readDeliveryChunk(deliverySessionId: string, start: number, end: number) {
  return http.get<FileDeliveryChunkRespVO>('/infra/file/delivery/chunk', { deliverySessionId, start, end })
}

/** ZS-FILE-004.A：撤权下载会话（在途传输的后续分块将被重检拦截）。 */
export function revokeDelivery(deliverySessionId: string) {
  return http.post<boolean>('/infra/file/delivery/revoke', undefined, { deliverySessionId })
}
