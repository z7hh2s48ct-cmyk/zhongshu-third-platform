import type { NotifyMessageLandingResult } from '@/api/system/notify/message'

/**
 * ZS-MSG-003 —— 移动端消息落点消费助手（纯函数，无 uni-app 运行时依赖）。
 *
 * 职责边界（与 Web 端 apps/zhongshu-admin-web/src/views/system/notify/landing.ts 同语义）：
 * - 落点是否可用、落点指向哪里，由服务端 `/system/notify-message/get-landing` 统一裁决
 *   （消息归属 → 落点注册 → 模块启用 → 业务重新授权），本助手只消费裁决结果；
 * - 不可用（未知落点 / 关闭模块 / 业务失权 / 端不支持）一律**不导航**，仅回显服务端原因；
 * - 可用时拼装页面路径并经注入的导航函数跳转，页面可达性由既有路由守卫
 *   （router/interceptor.ts + router/access.ts，ZS-CLIENT-002.A）最终判定——
 *   服务端 API 始终是真实边界，本助手不复制权限判定。
 */

/** 落点消费结果：navigated=已发起导航；unavailable=落点不可用（未发起导航） */
export type NotifyLandingOutcome = 'navigated' | 'unavailable'

/** 导航函数（uni.navigateTo 的最小投影，便于纯函数测试） */
export type LandingNavigator = (url: string) => void

/** 落点描述 → 完整页面路径（route + params 拼 query，route 已含 query 时以 & 续接）；
 * 无 descriptor/无 route 视为不可用返回空串 */
export function buildNotifyLandingRoute(result: NotifyMessageLandingResult): string {
  const descriptor = result?.descriptor
  if (!result?.available || !descriptor?.route) {
    return ''
  }
  const params = descriptor.params ?? {}
  const query = Object.keys(params)
    .filter(key => params[key] !== undefined && params[key] !== null)
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(String(params[key]))}`)
    .join('&')
  if (!query) {
    return descriptor.route
  }
  const separator = descriptor.route.includes('?') ? '&' : '?'
  return `${descriptor.route}${separator}${query}`
}

/**
 * 消费落点结果：可用 → 经导航函数跳转并返回 'navigated'；
 * 不可用 → 不导航并返回 'unavailable'（提示文本用 notifyLandingUnavailableText）。
 */
export function applyNotifyLandingRoute(
  navigate: LandingNavigator,
  result: NotifyMessageLandingResult,
): NotifyLandingOutcome {
  const route = buildNotifyLandingRoute(result)
  if (!route) {
    return 'unavailable'
  }
  navigate(route)
  return 'navigated'
}

/** 不可用提示：优先服务端 reason，缺失时按原因码兜底，杜绝静默无反馈 */
export function notifyLandingUnavailableText(result: NotifyMessageLandingResult): string {
  if (result?.reason) {
    return result.reason
  }
  switch (result?.unavailableCode) {
    case 'NOT_REGISTERED':
      return '该消息类型未注册跳转落点'
    case 'MODULE_DISABLED':
      return '该消息所属模块未启用，落点不可用'
    case 'REVOKED':
      return '该消息关联的业务已不可访问'
    case 'CLIENT_UNSUPPORTED':
      return '该消息在当前端未注册跳转落点'
    default:
      return '该消息暂无可用的跳转落点'
  }
}
