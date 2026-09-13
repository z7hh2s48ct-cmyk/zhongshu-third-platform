import axios, { AxiosError, AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios'

import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import qs from 'qs'
import { config } from '@/config/axios/config'
import {
  getAccessToken,
  getRefreshToken,
  getTenantId,
  getVisitTenantId,
  removeToken,
  setToken
} from '@/utils/auth'
import errorCode from './errorCode'

import { resetRouter } from '@/router'
import { deleteUserCache } from '@/hooks/web/useCache'
import { ApiEncrypt } from '@/utils/encrypt'

const tenantEnable = import.meta.env.VITE_APP_TENANT_ENABLE
// ZS-SEC-001.A：跨租户访问能力总开关，默认关闭；关闭时不注入 visit-tenant-id 头
const tenantVisitEnable = import.meta.env.VITE_APP_TENANT_VISIT_ENABLE
const { result_code, base_url, request_timeout } = config

// 需要忽略的提示。忽略后，自动 Promise.reject('error')
const ignoreMsgs = [
  '无效的刷新令牌', // 刷新令牌被删除时，不用提示
  '刷新令牌已过期' // 使用刷新令牌，刷新获取新的访问令牌时，结果因为过期失败，此时需要忽略。否则，会导致继续 401，无法跳转到登出界面
]
// 是否显示重新登录
export const isRelogin = { show: false }
// Axios 无感知刷新令牌，参考 https://www.dashingdog.cn/article/11 与 https://segmentfault.com/a/1190000020210980 实现
// 请求队列
let requestList: any[] = []
// 是否正在刷新中
let isRefreshToken = false
// 请求白名单，无须 token 的接口
const whiteList: string[] = ['/login', '/refresh-token']

/* ------------------------------------------------------------------ *
 * ZS-CLIENT-003（B03 登录请求合同）：凭据范围与可追踪性辅助
 * 与移动端 interceptor.ts 保持同一请求合同：凭据仅发往批准 API origin，
 * 白名单精确段边界匹配，trace/correlation 标识透传（ZS-SEC-006）。
 * ------------------------------------------------------------------ */

/** 标准 scheme 绝对地址判定 */
const ABSOLUTE_URL_RE = /^[a-zA-Z][a-zA-Z\d+\-.]*:\/\//

/** 生成客户端关联标识（trace/correlation id） */
function generateTraceId(): string {
  const g = globalThis as any
  try {
    if (g.crypto && typeof g.crypto.randomUUID === 'function') {
      return g.crypto.randomUUID()
    }
  } catch {
    // 运行时不提供 crypto 时回退到时间戳 + 随机数
  }
  return `c-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`
}

/** 剥离 query/hash 得到路径；绝对 URL 取其 pathname */
function extractPathname(url: string): string {
  if (!url) {
    return ''
  }
  if (ABSOLUTE_URL_RE.test(url)) {
    try {
      return new URL(url).pathname
    } catch {
      // 解析失败时退回手动剥离
    }
  }
  return url.split('#')[0].split('?')[0]
}

/** 精确路由匹配：白名单条目以 '/' 起始，用段边界 endsWith 判定，避免 '/login' 误命中 '/login-history' */
function isWhitelistedPath(url: string, list: string[] = whiteList): boolean {
  const pathname = extractPathname(url)
  if (!pathname) {
    return false
  }
  return list.some(entry => pathname === entry || pathname.endsWith(entry))
}

/** 批准来源解析：由 base_url 推导 origin；base_url 为相对地址时回退当前页面 origin */
function resolveApprovedOrigin(): string {
  const base = base_url || ''
  if (ABSOLUTE_URL_RE.test(base)) {
    try {
      return new URL(base).origin
    } catch {
      return ''
    }
  }
  try {
    const g = globalThis as any
    if (g.window && g.window.location && g.window.location.origin) {
      return g.window.location.origin
    }
  } catch {
    // 无 window 环境（如 SSR/测试）忽略
  }
  return ''
}

/**
 * 凭据仅发往批准 API origin：
 * - 相对地址视为同源批准（协议相对 // 失败关闭）
 * - 绝对地址需 origin 与批准 origin 一致（大小写不敏感）
 * - 批准 origin 未知时对绝对地址失败关闭
 */
function isApprovedApiOrigin(url: string, approvedOrigin: string): boolean {
  if (!url) {
    return false
  }
  if (!ABSOLUTE_URL_RE.test(url)) {
    return !url.startsWith('//')
  }
  if (!approvedOrigin) {
    return false
  }
  try {
    return new URL(url).origin.toLowerCase() === approvedOrigin.toLowerCase()
  } catch {
    return false
  }
}

/** 组合 baseURL 与 url 得到用于来源判定的完整地址 */
function resolveFullUrl(config: InternalAxiosRequestConfig): string {
  const url = config.url || ''
  if (ABSOLUTE_URL_RE.test(url)) {
    return url
  }
  const base = config.baseURL || base_url || ''
  if (!base) {
    return url
  }
  return `${base.replace(/\/+$/, '')}/${url.replace(/^\/+/, '')}`
}

/** trace/correlation 标识透传：调用方已提供则原样保留，否则生成；返回最终值（ZS-SEC-006） */
function ensureTraceId(header: Record<string, any>): string {
  let existing: any
  for (const key of Object.keys(header)) {
    if (key.toLowerCase() === 'trace-id' && header[key]) {
      existing = header[key]
      break
    }
  }
  if (!existing && typeof (header as any).get === 'function') {
    existing = (header as any).get('trace-id')
  }
  const value = existing ? String(existing) : generateTraceId()
  header['trace-id'] = value
  return value
}

/** 从 axios 响应/错误对象读取 trace/correlation 标识（大小写不敏感），用于异常回显 */
function readTraceIdFromAxios(source: any): string | undefined {
  const candidates = [
    source && source.headers,
    source && source.response && source.response.headers,
    source && source.config && source.config.headers,
    source && source.response && source.response.config && source.response.config.headers,
  ]
  for (const header of candidates) {
    if (!header) {
      continue
    }
    for (const key of Object.keys(header)) {
      if (key.toLowerCase() === 'trace-id' && header[key]) {
        return String(header[key])
      }
    }
    if (typeof header.get === 'function') {
      const v = header.get('trace-id')
      if (v) {
        return String(v)
      }
    }
  }
  return undefined
}

// 创建axios实例
const service: AxiosInstance = axios.create({
  baseURL: base_url, // api 的 base_url
  timeout: request_timeout, // 请求超时时间
  withCredentials: false, // 禁用 Cookie 等信息
  // 自定义参数序列化函数
  paramsSerializer: (params) => {
    return qs.stringify(params, { allowDots: true })
  }
})

// request拦截器
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // ZS-CLIENT-003（B03 登录请求合同）：凭据范围判定
    //   - isApproved：仅批准 API origin 才携带平台凭据（Token / 租户头 / trace-id）
    //   - callerOptOut：调用方显式 headers.isToken === false
    //   - whitelisted：登录 / 刷新令牌等白名单路径（精确段边界匹配，不再用 includes 子串）
    const isApproved = isApprovedApiOrigin(resolveFullUrl(config), resolveApprovedOrigin())
    const callerOptOut = (config!.headers || {}).isToken === false
    const whitelisted = isWhitelistedPath(config.url || '')
    const allowToken = isApproved && !callerOptOut && !whitelisted
    if (allowToken && getAccessToken()) {
      config.headers.Authorization = 'Bearer ' + getAccessToken() // 让每个请求携带自定义 token
    }
    // 设置租户（仅批准来源；登录仍需租户上下文，故 tenant-id 只受 isApproved 约束）
    if (isApproved && tenantEnable && tenantEnable === 'true') {
      const tenantId = getTenantId()
      if (tenantId) config.headers['tenant-id'] = tenantId
      // ZS-SEC-001.A：仅当跨租户访问能力显式开启、且已登录时才注入 visit-tenant-id 访问租户头；
      // 默认关闭时前端不发送该头，后端拦截器亦会拒绝，形成前后端双重收口，避免越权范围被自动放大
      if (tenantVisitEnable === 'true') {
        const visitTenantId = getVisitTenantId()
        if (config.headers.Authorization && visitTenantId) {
          config.headers['visit-tenant-id'] = visitTenantId
        }
      }
    }
    // trace/correlation 标识透传（ZS-SEC-006）：仅批准来源注入/回显，外部来源不携带
    if (isApproved) {
      ensureTraceId(config.headers)
    }
    const method = config.method?.toUpperCase()
    // 防止 GET 请求缓存
    if (method === 'GET') {
      config.headers['Cache-Control'] = 'no-cache'
      config.headers['Pragma'] = 'no-cache'
    }
    // 自定义参数序列化函数
    else if (method === 'POST') {
      const contentType = config.headers['Content-Type'] || config.headers['content-type']
      if (contentType === 'application/x-www-form-urlencoded') {
        if (config.data && typeof config.data !== 'string') {
          config.data = qs.stringify(config.data)
        }
      }
    }
    // 是否 API 加密
    if ((config!.headers || {}).isEncrypt && !(config!.headers || {}).isEncrypted) {
      try {
        // 加密请求数据
        if (config.data) {
          config.data = ApiEncrypt.encryptRequest(config.data)
          // 设置加密标识头
          config.headers[ApiEncrypt.getEncryptHeader()] = 'true'
        }
      } catch (error) {
        console.error('请求数据加密失败:', error)
        throw error
      }
    }
    return config
  },
  (error: AxiosError) => {
    // Do something with request error
    console.log(error) // for debug
    return Promise.reject(error)
  }
)

// response 拦截器
service.interceptors.response.use(
  async (response: AxiosResponse<any>) => {
    let { data } = response
    const config = response.config
    if (!data) {
      // 返回“[HTTP]请求没有返回值”;
      throw new Error()
    }

    // 检查是否需要解密响应数据
    const encryptHeader = ApiEncrypt.getEncryptHeader()
    const isEncryptResponse =
      response.headers[encryptHeader] === 'true' ||
      response.headers[encryptHeader.toLowerCase()] === 'true'
    if (isEncryptResponse && typeof data === 'string') {
      try {
        // 解密响应数据
        data = ApiEncrypt.decryptResponse(data)
      } catch (error) {
        console.error('响应数据解密失败:', error)
        // ZS-CLIENT-003（验收④）：解密失败显式 reject，并回显 trace/correlation 标识，避免调用方永久 loading
        const decryptError = new Error('响应数据解密失败: ' + (error as Error).message)
        ;(decryptError as any).traceId = readTraceIdFromAxios(response)
        return Promise.reject(decryptError)
      }
    }

    const { t } = useI18n()
    // 未设置状态码则默认成功状态
    // 二进制数据则直接返回，例如说 Excel 导出
    if (
      response.request.responseType === 'blob' ||
      response.request.responseType === 'arraybuffer'
    ) {
      // 注意：如果导出的响应为 json，说明可能失败了，不直接返回进行下载
      if (response.data.type !== 'application/json') {
        return response.data
      }
      data = await new Response(response.data).json()
    }
    const code = data.code ?? result_code
    // 获取错误信息
    const msg = data.msg || errorCode[code] || errorCode['default']
    if (ignoreMsgs.indexOf(msg) !== -1) {
      // 如果是忽略的错误码，直接返回 msg 异常
      return Promise.reject(msg)
    } else if (code === 401) {
      // 如果未认证，并且未进行刷新令牌，说明可能是访问令牌过期了
      if (!isRefreshToken) {
        isRefreshToken = true
        // 1. 如果获取不到刷新令牌，则只能执行登出操作
        if (!getRefreshToken()) {
          return handleAuthorized()
        }
        // 2. 进行刷新访问令牌
        try {
          const refreshTokenRes = await refreshToken()
          // 2.1 刷新成功，则回放队列的请求 + 当前请求
          setToken((await refreshTokenRes).data.data)
          config.headers!.Authorization = 'Bearer ' + getAccessToken()
          requestList.forEach((cb: any) => {
            cb()
          })
          requestList = []
          if ((config!.headers || {}).isEncrypt) {
            ;(config!.headers || {}).isEncrypted = true
          }
          return service(config)
        } catch (e) {
          // 为什么需要 catch 异常呢？刷新失败时，请求因为 Promise.reject 触发异常。
          // 2.2 刷新失败，只回放队列的请求
          requestList.forEach((cb: any) => {
            cb()
          })
          // 提示是否要登出。即不回放当前请求！不然会形成递归
          return handleAuthorized()
        } finally {
          requestList = []
          isRefreshToken = false
        }
      } else {
        // 添加到队列，等待刷新获取到新的令牌
        return new Promise((resolve) => {
          requestList.push(() => {
            config.headers!.Authorization = 'Bearer ' + getAccessToken() // 让每个请求携带自定义token 请根据实际情况自行修改
            resolve(service(config))
          })
        })
      }
    } else if (code === 500) {
      ElMessage.error(t('sys.api.errMsg500'))
      const err500 = new Error(msg)
      ;(err500 as any).traceId = readTraceIdFromAxios(response)
      return Promise.reject(err500)
    } else if (code === 901) {
      ElMessage.error({
        offset: 300,
        dangerouslyUseHTMLString: true,
        message:
          '<div>' +
          t('sys.api.errMsg901') +
          '</div>' +
          '<div> &nbsp; </div>' +
          '<div>参考 https://doc.iocoder.cn/ 教程</div>' +
          '<div> &nbsp; </div>' +
          '<div>5 分钟搭建本地环境</div>'
      })
      const err901 = new Error(msg)
      ;(err901 as any).traceId = readTraceIdFromAxios(response)
      return Promise.reject(err901)
    } else if (code !== 0 && code !== 200) {
      if (msg === '无效的刷新令牌') {
        // hard coding：忽略这个提示，直接登出
        console.log(msg)
        return handleAuthorized()
      } else {
        ElNotification.error({ title: msg })
      }
      return Promise.reject('error')
    } else {
      return data
    }
  },
  (error: AxiosError) => {
    console.log('err' + error) // for debug
    let { message } = error
    const { t } = useI18n()
    if (message === 'Network Error') {
      message = t('sys.api.errorMessage')
    } else if (message.includes('timeout')) {
      message = t('sys.api.apiTimeoutMessage')
    } else if (message.includes('Request failed with status code')) {
      message = t('sys.api.apiRequestFailed') + message.substr(message.length - 3)
    }
    ElMessage.error(message)
    // ZS-CLIENT-003（验收⑤）：断网/超时/取消等一致 reject 且回显 trace/correlation 标识
    ;(error as any).traceId = readTraceIdFromAxios(error)
    return Promise.reject(error)
  }
)

const refreshToken = async () => {
  axios.defaults.headers.common['tenant-id'] = getTenantId()
  return await axios.post(base_url + '/system/auth/refresh-token?refreshToken=' + getRefreshToken())
}
const handleAuthorized = () => {
  const { t } = useI18n()
  if (!isRelogin.show) {
    // 如果已经到登录页面则不进行弹窗提示
    if (window.location.href.includes('login')) {
      return
    }
    isRelogin.show = true
    ElMessageBox.confirm(t('sys.api.timeoutMessage'), t('common.confirmTitle'), {
      showCancelButton: false,
      closeOnClickModal: false,
      showClose: false,
      closeOnPressEscape: false,
      confirmButtonText: t('login.relogin'),
      type: 'warning'
    }).then(() => {
      resetRouter() // 重置静态路由表
      deleteUserCache() // 删除用户缓存
      removeToken()
      isRelogin.show = false
      // 干掉token后再走一次路由让它过router.beforeEach的校验
      window.location.href = window.location.href
    })
  }
  return Promise.reject(t('sys.api.timeoutMessage'))
}
export { service }
