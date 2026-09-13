/* eslint-disable brace-style */ // 原因：unibest 官方维护的代码，尽量不要大概，避免难以合并
import type { CustomRequestOptions } from '@/http/types'
import { useTokenStore, useUserStore } from '@/store'
import { getEnvBaseUrl } from '@/utils'
import { ApiEncrypt } from '@/utils/encrypt'
import { stringifyQuery } from './tools/queryString'

// 请求基准地址
const baseUrl = getEnvBaseUrl()
const tenantEnable = import.meta.env.VITE_APP_TENANT_ENABLE
// ZS-SEC-001.A：跨租户访问能力总开关，默认关闭；关闭时不注入 visit-tenant-id 头
const tenantVisitEnable = import.meta.env.VITE_APP_TENANT_VISIT_ENABLE

const whiteList: string[] = [
  '/login',
  '/refresh-token',
  '/system/tenant/get-id-by-name',
] // 白名单列表，不需要传递 token 字段

/* ------------------------------------------------------------------ *
 * ZS-CLIENT-003（B03 登录请求合同）：凭据范围与可追踪性辅助
 * 统一 Web/移动请求合同：凭据仅发往批准 API origin，白名单精确段边界匹配，
 * trace/correlation 标识透传（ZS-SEC-006）。
 * ------------------------------------------------------------------ */

/** 标准 scheme 绝对地址判定（协议相对 // 不在此列，单独失败关闭处理） */
const ABSOLUTE_URL_RE = /^[a-z][a-z\d+\-.]*:\/\//i
/** 匹配 scheme://authority 前缀，用于剥离得到路径（替代运行时不一定存在的 new URL） */
const AUTHORITY_RE = /^[a-z][a-z\d+\-.]*:\/\/[^/?#]+/i
/** 提取 origin（scheme://host[:port]）；小程序运行时无浏览器 URL 全局，故用正则解析 */
const ORIGIN_RE = /^([a-z][a-z\d+\-.]*):\/\/([^/?#]+)/i

/** 由绝对地址解析 origin（统一小写、归一化默认端口）；非绝对地址或解析失败返回空串。不依赖 new URL */
function parseOrigin(url: string): string {
  const m = ORIGIN_RE.exec(url)
  if (!m) {
    return ''
  }
  const scheme = m[1].toLowerCase()
  let authority = m[2].toLowerCase()
  // 归一化默认端口（与 URL.origin 行为一致）：http 省略 :80、https 省略 :443，
  // 避免同一 origin 因显式/省略默认端口写法不同而被误判为外部来源、丢失凭据。
  const portMatch = /:(\d+)$/.exec(authority)
  if (portMatch) {
    const port = portMatch[1]
    if ((scheme === 'http' && port === '80') || (scheme === 'https' && port === '443')) {
      authority = authority.slice(0, authority.length - portMatch[0].length)
    }
  }
  return `${scheme}://${authority}`
}

/**
 * 生成客户端关联标识（trace/correlation id）。
 * 后端 TracerUtils.isValidTraceIdFormat 要求恰好 32 位十六进制，否则会被 TraceFilter 替换，
 * 导致超时/取消后上报的标识无法关联服务端日志；故统一产出 32 位小写十六进制。
 */
function generateTraceId(): string {
  const g = globalThis as any
  try {
    if (g.crypto && typeof g.crypto.getRandomValues === 'function') {
      const bytes = new Uint8Array(16)
      g.crypto.getRandomValues(bytes)
      let hex = ''
      for (let i = 0; i < bytes.length; i++) {
        hex += bytes[i].toString(16).padStart(2, '0')
      }
      return hex
    }
    if (g.crypto && typeof g.crypto.randomUUID === 'function') {
      return String(g.crypto.randomUUID()).replace(/-/g, '')
    }
  }
  catch {
    // 部分小程序运行时不提供 crypto，回退到 Math.random
  }
  let hex = ''
  for (let i = 0; i < 32; i++) {
    hex += Math.floor(Math.random() * 16).toString(16)
  }
  return hex
}

/** 剥离 query/hash 得到路径；绝对 URL 取其 pathname */
export function extractPathname(url: string): string {
  if (!url) {
    return ''
  }
  // 不依赖 new URL（小程序运行时无浏览器 URL 全局）：绝对地址剥离 scheme://authority 前缀
  const rest = ABSOLUTE_URL_RE.test(url) ? url.replace(AUTHORITY_RE, '') : url
  return rest.split('#')[0].split('?')[0]
}

/**
 * 精确路由匹配：白名单条目以 '/' 起始，用段边界 endsWith 判定，
 * 避免 '/login' 误命中 '/login-history' 之类前缀相似路径。
 */
export function isWhitelistedPath(url: string, list: string[] = whiteList): boolean {
  const pathname = extractPathname(url)
  if (!pathname) {
    return false
  }
  return list.some(entry => pathname === entry || pathname.endsWith(entry))
}

/** 批准来源解析：由 baseUrl 推导 origin；无法解析时返回空串（后续对绝对地址失败关闭） */
export function resolveApprovedOrigin(base?: string): string {
  const target = base ?? getEnvBaseUrl()
  if (!target) {
    return ''
  }
  // 不依赖 new URL（小程序运行时无浏览器 URL 全局）
  return parseOrigin(target)
}

/**
 * 凭据仅发往批准 API origin：
 * - 相对地址视为同源批准（协议相对 // 无法确定 origin，失败关闭）
 * - 绝对地址需 origin 与批准 origin 一致（大小写不敏感）
 * - 批准 origin 未知时，对绝对地址失败关闭
 */
export function isApprovedApiOrigin(url: string, approvedOrigin: string): boolean {
  if (!url) {
    return false
  }
  if (!ABSOLUTE_URL_RE.test(url)) {
    return !url.startsWith('//')
  }
  if (!approvedOrigin) {
    return false
  }
  // 不依赖 new URL（小程序运行时无浏览器 URL 全局）
  const origin = parseOrigin(url)
  // approvedOrigin 亦归一化（兼容调用方直接传入显式默认端口的写法）
  const approved = parseOrigin(approvedOrigin) || approvedOrigin.toLowerCase()
  return !!origin && origin === approved
}

/** trace/correlation 标识透传：调用方已提供则原样保留，否则生成；返回最终值（ZS-SEC-006） */
export function ensureTraceId(header: Record<string, any>): string {
  let existing: any
  const variants: string[] = []
  for (const key of Object.keys(header)) {
    if (key.toLowerCase() === 'trace-id') {
      variants.push(key)
      if (!existing && header[key]) {
        existing = header[key]
      }
    }
  }
  if (!existing && typeof (header as any).get === 'function') {
    existing = (header as any).get('trace-id')
  }
  const value = existing ? String(existing) : generateTraceId()
  // 移除大小写变体（如调用方传入的 'Trace-Id'），统一规范为单一 'trace-id' 键，
  // 避免 H5 下对每个属性调 setRequestHeader 时合并成 "id, id" 破坏透传（P2）
  for (const key of variants) {
    if (key !== 'trace-id') {
      if (typeof (header as any).delete === 'function') {
        (header as any).delete(key)
      }
      else {
        delete header[key]
      }
    }
  }
  header['trace-id'] = value
  return value
}

// 批准 API origin（模块加载期由 baseUrl 推导一次）
const approvedOrigin = resolveApprovedOrigin(baseUrl)

// 拦截器配置
export const httpInterceptor = {
  // 拦截前触发
  invoke(options: CustomRequestOptions) {
    // 接口请求支持通过 query 参数配置 queryString
    if (options.query) {
      const queryStr = stringifyQuery(options.query)
      if (options.url.includes('?')) {
        options.url += `&${queryStr}`
      }
      else {
        options.url += `?${queryStr}`
      }
    }
    // 非 http 开头需拼接地址
    if (!options.url.startsWith('http')) {
      // #ifdef H5
      if (JSON.parse(import.meta.env.VITE_APP_PROXY_ENABLE)) {
        // 自动拼接代理前缀
        options.url = import.meta.env.VITE_APP_PROXY_PREFIX + options.url
      }
      else {
        options.url = baseUrl + options.url
      }
      // #endif
      // 非H5正常拼接
      // #ifndef H5
      options.url = baseUrl + options.url
      // #endif
      // TIPS: 如果需要对接多个后端服务，也可以在这里处理，拼接成所需要的地址
    }
    // 1. 请求超时
    options.timeout = 60000 // 60s
    // 2. （可选）添加小程序端请求头标识
    options.header = {
      ...options.header,
    }
    // 3. ZS-CLIENT-003（B03）：凭据范围判定（修正原布尔反义缺陷）
    //    - isApproved：仅批准 API origin 才携带平台凭据（Token / 租户头 / trace-id）
    //    - callerOptOut：调用方显式 header.isToken === false
    //    - whitelisted：登录 / 刷新令牌 / 租户查询等白名单路径（精确段边界匹配）
    //    仅当三者共同允许（allowToken）时才附带 Token，杜绝白名单/外部来源泄露凭据
    const isApproved = isApprovedApiOrigin(options.url, approvedOrigin)
    const callerOptOut = (options.header || {}).isToken === false
    const whitelisted = isWhitelistedPath(options.url)
    const allowToken = isApproved && !callerOptOut && !whitelisted

    const tokenStore = useTokenStore()
    const token = tokenStore.updateNowTime().validToken
    if (allowToken && token) {
      options.header.Authorization = `Bearer ${token}`
    }

    // 4. 添加租户标识（仅批准来源；登录仍需租户上下文，故 tenant-id 只受 isApproved 约束）
    if (isApproved && tenantEnable && tenantEnable === 'true') {
      const tenantId = useUserStore().tenantId
      if (tenantId) {
        options.header['tenant-id'] = tenantId
      }
      // ZS-SEC-001.A：仅当跨租户访问能力显式开启、且本次请求确实允许携带 Token 时才注入 visit-tenant-id 头；
      // 默认关闭时前端不发送该头，后端拦截器亦会拒绝，形成前后端双重收口
      if (tenantVisitEnable === 'true' && allowToken) {
        const visitTenantId = useUserStore().visitTenantId
        if (token && visitTenantId) {
          options.header['visit-tenant-id'] = visitTenantId
        }
      }
    }

    // 5. trace/correlation 标识透传（ZS-SEC-006）：仅批准来源注入/回显，外部来源不携带
    if (isApproved) {
      ensureTraceId(options.header)
    }

    // 6. add by panda：是否 API 加密
    if (options.isEncrypt) {
      try {
        // 加密请求数据
        if (options.data) {
          options.data = ApiEncrypt.encryptRequest(options.data)
          // 设置加密标识头
          options.header[ApiEncrypt.getEncryptHeader()] = 'true'
        }
      } catch (error) {
        console.error('请求数据加密失败:', error)
        throw error
      }
    }

    return options
  },
}

export const requestInterceptor = {
  install() {
    // 拦截 request 请求
    uni.addInterceptor('request', httpInterceptor)
    // 拦截 uploadFile 文件上传
    uni.addInterceptor('uploadFile', httpInterceptor)
  },
}
