import { describe, expect, it, vi } from 'vitest'
import {
  applyNotifyLandingRoute,
  buildNotifyLandingRoute,
  notifyLandingUnavailableText
} from '@/views/system/notify/landing'
import type { NotifyMessageLandingResult } from '@/api/system/notify/message'

/**
 * ZS-MSG-003：Web 端消息落点消费助手单测。
 *
 * 覆盖验收（Web/移动落点和未知模块提示）：
 * - 可用落点 → 拼装路由并发起导航（是否可达由路由守卫最终判定，本层不复制权限判定）；
 * - 不可用落点（未知类型/关闭模块/业务失权/端不支持）→ 绝不导航，且提示文本绝不静默；
 * - 落点响应不含消息正文（结构性引用），助手只消费 descriptor，不触碰 templateContent。
 */
describe('buildNotifyLandingRoute', () => {
  it('可用落点 + params → 路由拼 query', () => {
    const result: NotifyMessageLandingResult = {
      available: true,
      descriptor: { module: 'system', route: '/system/fixture/detail', params: { id: 1024 } }
    }
    expect(buildNotifyLandingRoute(result)).toBe('/system/fixture/detail?id=1024')
  })

  it('可用落点无 params → 原样路由', () => {
    const result: NotifyMessageLandingResult = {
      available: true,
      descriptor: { module: 'system', route: '/system/fixture/list' }
    }
    expect(buildNotifyLandingRoute(result)).toBe('/system/fixture/list')
  })

  it('params 中 null/undefined 被过滤，特殊字符被编码', () => {
    const result: NotifyMessageLandingResult = {
      available: true,
      descriptor: {
        module: 'system',
        route: '/system/fixture/detail',
        params: { id: 7, skip: undefined, omit: null, key: 'a b&c' }
      }
    }
    expect(buildNotifyLandingRoute(result)).toBe('/system/fixture/detail?id=7&key=a%20b%26c')
  })

  it('route 自带 query 时以 & 续接，不产生第二个 ?', () => {
    const result: NotifyMessageLandingResult = {
      available: true,
      descriptor: {
        module: 'system',
        route: '/system/fixture/detail?tab=main',
        params: { id: 9 }
      }
    }
    expect(buildNotifyLandingRoute(result)).toBe('/system/fixture/detail?tab=main&id=9')
  })

  it('不可用 / 缺 descriptor / 缺 route → null（不猜测跳转）', () => {
    expect(buildNotifyLandingRoute({ available: false })).toBeNull()
    expect(buildNotifyLandingRoute({ available: true })).toBeNull()
    expect(
      buildNotifyLandingRoute({ available: true, descriptor: { module: 'system', route: '' } })
    ).toBeNull()
  })
})

describe('applyNotifyLandingRoute', () => {
  it('可用 → push 导航一次并返回 navigated', () => {
    const push = vi.fn()
    const outcome = applyNotifyLandingRoute(
      { push },
      {
        available: true,
        descriptor: { module: 'system', route: '/system/fixture/detail', params: { id: 1 } }
      }
    )
    expect(outcome).toBe('navigated')
    expect(push).toHaveBeenCalledTimes(1)
    expect(push).toHaveBeenCalledWith('/system/fixture/detail?id=1')
  })

  it.each(['NOT_REGISTERED', 'MODULE_DISABLED', 'REVOKED', 'CLIENT_UNSUPPORTED'] as const)(
    '不可用（%s）→ 不导航并返回 unavailable',
    (code) => {
      const push = vi.fn()
      const outcome = applyNotifyLandingRoute({ push }, { available: false, unavailableCode: code })
      expect(outcome).toBe('unavailable')
      expect(push).not.toHaveBeenCalled()
    }
  )
})

describe('notifyLandingUnavailableText', () => {
  it('优先服务端 reason', () => {
    expect(
      notifyLandingUnavailableText({ available: false, reason: '业务对象已删除或您已无权访问' })
    ).toBe('业务对象已删除或您已无权访问')
  })

  it('reason 缺失时按原因码兜底', () => {
    expect(
      notifyLandingUnavailableText({ available: false, unavailableCode: 'MODULE_DISABLED' })
    ).toBe('该消息所属模块未启用，落点不可用')
    expect(
      notifyLandingUnavailableText({ available: false, unavailableCode: 'NOT_REGISTERED' })
    ).toBe('该消息类型未注册跳转落点')
    expect(notifyLandingUnavailableText({ available: false, unavailableCode: 'REVOKED' })).toBe(
      '该消息关联的业务已不可访问'
    )
    expect(
      notifyLandingUnavailableText({ available: false, unavailableCode: 'CLIENT_UNSUPPORTED' })
    ).toBe('该消息在当前端未注册跳转落点')
  })

  it('完全未知的结果也有兜底提示，不静默', () => {
    expect(notifyLandingUnavailableText({ available: false })).toBe('该消息暂无可用的跳转落点')
  })
})
