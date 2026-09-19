import request from '@/config/axios'

// 文件预签名地址 Response VO
export interface FilePresignedUrlRespVO {
  // 文件配置编号
  configId: string
  // 文件上传 URL
  uploadUrl: string
  // 文件 URL
  url: string
  // 文件路径
  path: string
}

// 查询文件列表
export const getFilePage = (params: PageParam) => {
  return request.get({ url: '/infra/file/page', params })
}

// 删除文件
export const deleteFile = (id: string) => {
  return request.delete({ url: '/infra/file/delete?id=' + id })
}

// 批量删除文件（ZS-FILE-005.A：返回逐项结果，中段失败不伪报全成功）
export const deleteFileList = (ids: string[]) => {
  return request.delete<{ successIds: number[]; failures: { id: number; errorMessage: string }[] }>({
    url: '/infra/file/delete-list',
    params: { ids: ids.join(',') }
  })
}

// 获取文件预签名地址
export const getFilePresignedUrl = (name: string, directory?: string) => {
  return request.get<FilePresignedUrlRespVO>({
    url: '/infra/file/presigned-url',
    params: { name, directory }
  })
}

// 创建文件
export const createFile = (data: any) => {
  return request.post({ url: '/infra/file/create', data })
}

// 上传文件
export const updateFile = (data: any, onUploadProgress?: Function) => {
  return request.upload({ url: '/infra/file/upload', data, onUploadProgress })
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
  fileId: string
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
export const createUploadCredential = (data: FileUploadCredentialCreateReqVO) => {
  return request.post<FileUploadCredentialCreateRespVO>({
    url: '/infra/file/upload-credential',
    data
  })
}

/**
 * ZS-FILE-003：直传完成确认——服务端核验临时对象后发布正式资产，返回资产 ID（一次性确认，幂等）。
 * CLIENT-004：上传必须等待本确认成功才返回资产 ID；确认失败即整体失败，绝不因对象已直传就报成功。
 */
export const completeUpload = (credentialToken: string) => {
  return request.post<number>({ url: '/infra/file/upload-complete', data: { credentialToken } })
}

/** ZS-FILE-004.A：签发一次性交付票据（本人主体绑定）。 */
export const issueDeliveryTicket = (data: FileDeliveryTicketIssueReqVO) => {
  return request.post<FileDeliveryTicketIssueRespVO>({ url: '/infra/file/delivery/issue', data })
}

/** ZS-FILE-004.A：原子兑换票据建立下载会话（同主体同登录会话重复兑换幂等）。 */
export const redeemDeliveryTicket = (ticketToken: string, purpose: string) => {
  return request.post<FileDeliverySessionRespVO>({
    url: '/infra/file/delivery/redeem',
    params: { ticketToken, purpose }
  })
}

/** ZS-FILE-004.A：鉴权取流分块，Range 语义 [start, endInclusive]，逐块重检身份/会话/撤权/过期。 */
export const readDeliveryChunk = (deliverySessionId: string, start: number, end: number) => {
  return request.get<FileDeliveryChunkRespVO>({
    url: '/infra/file/delivery/chunk',
    params: { deliverySessionId, start, end }
  })
}

/** ZS-FILE-004.A：撤权下载会话（在途传输的后续分块将被重检拦截）。 */
export const revokeDelivery = (deliverySessionId: string) => {
  return request.post<boolean>({
    url: '/infra/file/delivery/revoke',
    params: { deliverySessionId }
  })
}
