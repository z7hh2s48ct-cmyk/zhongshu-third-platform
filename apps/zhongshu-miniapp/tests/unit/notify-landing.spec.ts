import type { NotifyMessageLandingResult } from '@/api/system/notify/message'
import { describe, expect, it, vi } from 'vitest'
import {
  applyNotifyLandingRoute,
  buildNotifyLandingRoute,
  notifyLandingUnavailableText,
} from '@/pages/message/landing'

/**
 * ZS-MSG-003：移动端消息落点消费助手单测（与 Web 端 tests/unit/notify-landing.spec.ts 同语义）。
 *
 * 覆盖验收（Web/移动落点和未知模块提示）：
 * - 可用落点 → 拼装页面路径并发起导航（可达性由 interceptor/access 守卫最终判定）；
 * - 不可用落点（未知类型/关闭模块/业务失权/端不支持）→ 绝不导航，且提示文本绝不静默。
 */
describe('buildNotifyLandingRoute', () => {
  it('可用落点 + params → 页面路径拼 query', () => {
    const result: NotifyMessageLandingResult = {
      available: true,
      descriptor: { module: 'system', route: '/pages-fixture/detail/index', params: { id: 2048 } },
    }
    expect(buildNotifyLandingRoute(result)).toBe('/pages-fixture/detail/index?id=2048')
  })

  it('可用落点无 params → 原样路径', () => {
    const result: NotifyMessageLandingResult = {
      available: true,
      descriptor: { module: 'system', route: '/pages-fixture/list/index' },
    }
    expect(buildNotifyLandingRoute(result)).toBe('/pages-fixture/list/index')
  })

  it('params 中 null/undefined 被过滤，特殊字符被编码', () => {
    const result: NotifyMessageLandingResult = {
      available: true,
      descriptor: {
        module: 'system',
        route: '/pages-fixture/detail/index',
        params: { id: 7, skip: undefined, omit: null, key: 'a b&c' },
      },
    }
    expect(buildNotifyLandingRoute(result)).toBe(
      '/pages-fixture/detail/index?id=7&key=a%20b%26c',
    )
  })

  it('不可用 / 缺 descriptor / 缺 route → 空串（不猜测跳转）', () => {
    expect(buildNotifyLandingRoute({ available: false })).toBe('')
    expect(buildNotifyLandingRoute({ available: true })).toBe('')
    expect(
      buildNotifyLandingRoute({ available: true, descriptor: { module: 'system', route: '' } }),
    ).toBe('')
  })
})

describe('applyNotifyLandingRoute', () => {
  it('可用 → 经注入的导航函数跳转一次并返回 navigated', () => {
    const navigate = vi.fn()
    const outcome = applyNotifyLandingRoute(
      navigate,
      {
        available: true,
        descriptor: { module: 'system', route: '/pages-fixture/detail/index', params: { id: 1 } },
      },
    )
    expect(outcome).toBe('navigated')
    expect(navigate).toHaveBeenCalledTimes(1)
    expect(navigate).toHaveBeenCalledWith('/pages-fixture/detail/index?id=1')
  })

  it.each(['NOT_REGISTERED', 'MODULE_DISABLED', 'REVOKED', 'CLIENT_UNSUPPORTED'] as const)(
    '不可用（%s）→ 不导航并返回 unavailable',
    (code) => {
      const navigate = vi.fn()
      const outcome = applyNotifyLandingRoute(navigate, { available: false, unavailableCode: code })
      expect(outcome).toBe('unavailable')
      expect(navigate).not.toHaveBeenCalled()
    },
  )
})

describe('notifyLandingUnavailableText', () => {
  it('优先服务端 reason', () => {
    expect(
      notifyLandingUnavailableText({ available: false, reason: '业务对象已删除或您已无权访问' }),
    ).toBe('业务对象已删除或您已无权访问')
  })

  it('reason 缺失时按原因码兜底', () => {
    expect(notifyLandingUnavailableText({ available: false, unavailableCode: 'MODULE_DISABLED' })).toBe(
      '该消息所属模块未启用，落点不可用',
    )
    expect(notifyLandingUnavailableText({ available: false, unavailableCode: 'NOT_REGISTERED' })).toBe(
      '该消息类型未注册跳转落点',
    )
    expect(notifyLandingUnavailableText({ available: false, unavailableCode: 'REVOKED' })).toBe(
      '该消息关联的业务已不可访问',
    )
    expect(
      notifyLandingUnavailableText({ available: false, unavailableCode: 'CLIENT_UNSUPPORTED' }),
    ).toBe('该消息在当前端未注册跳转落点')
  })

  it('完全未知的结果也有兜底提示，不静默', () => {
    expect(notifyLandingUnavailableText({ available: false })).toBe('该消息暂无可用的跳转落点')
  })
})
