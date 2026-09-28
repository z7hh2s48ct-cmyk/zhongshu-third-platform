import { beforeEach, describe, expect, it, vi } from 'vitest'

const h = vi.hoisted(() => ({
  getImpl: vi.fn(),
}))

vi.mock('@/api/system/permission', () => ({
  getObjectAuthorization: (...args: any[]) => h.getImpl(...args)
}))

import {
  buildObjectAuthKey,
  normalizeObjectAuthorization,
  resolveActionAllowed,
  resolveFieldPresentation,
  type ObjectAuthSnapshot
} from '@/utils/objectAuthorization'
import { useObjectAuthorizationStore } from '@/store/modules/objectAuthorization'
import { createPinia, setActivePinia } from 'pinia'

// ZS-CLIENT-001.B：Web 消费业务动作与字段授权。
//
// 卡片验收锚点（§16.1 line1060）：
//   「旧页签不泄露」——授权快照随 clearAuthorizedSession 单一入口清理（见 auth-session.spec.ts 扩展断言）；
//   「前端伪造动作不生效」——store 的唯一写入路径是服务端响应（normalize 只接受服务端形状），
//     动作执行由服务端 PERM-003.A checkActionAllowed 独立拒绝（前端只消费不推断，卡片「调整」）。
// D-12 §5.2：masked 字段=渲染服务端脱敏值 + 脱敏标识；hidden 字段=前端必须不渲染（防旁路展示）。

describe('normalizeObjectAuthorization：只接受服务端形状（伪造不生效的入口闸门）', () => {
  it('null 响应（未接入域）→ null 快照', () => {
    expect(normalizeObjectAuthorization(null)).toBeNull()
  })

  it('合法响应 → 三集合快照，allowedActions 为数组', () => {
    const snapshot = normalizeObjectAuthorization({
      allowedActions: ['lead:query', 'lead:update'],
      authorizedFields: ['name', 'phone'],
      maskedFields: ['phone']
    })
    expect(snapshot).not.toBeNull()
    expect(snapshot!.allowedActions).toEqual(['lead:query', 'lead:update'])
    expect(snapshot!.authorizedFields).toEqual(['name', 'phone'])
    expect(snapshot!.maskedFields).toEqual(['phone'])
  })

  it('语义保留：authorizedFields=null（未启用字段级）与 []（启用但全隐）不得被合并', () => {
    const disabled = normalizeObjectAuthorization({
      allowedActions: [],
      authorizedFields: null,
      maskedFields: null
    })
    expect(disabled!.authorizedFields).toBeNull()

    const allHidden = normalizeObjectAuthorization({
      allowedActions: [],
      authorizedFields: [],
      maskedFields: []
    })
    expect(allHidden!.authorizedFields).toEqual([])
  })

  it('形状防御：集合中的非字符串条目被过滤，非法顶层形状按空集处理', () => {
    const snapshot = normalizeObjectAuthorization({
      allowedActions: ['lead:query', 42, null],
      authorizedFields: ['name', {}],
      maskedFields: 'not-an-array'
    })
    expect(snapshot!.allowedActions).toEqual(['lead:query'])
    expect(snapshot!.authorizedFields).toEqual(['name'])
    expect(snapshot!.maskedFields).toEqual([])
  })

  it('非对象响应（字符串/数字）→ null 快照（fail-closed）', () => {
    expect(normalizeObjectAuthorization('junk' as any)).toBeNull()
    expect(normalizeObjectAuthorization(123 as any)).toBeNull()
  })
})

describe('buildObjectAuthKey：对象键规范化（orgId/ownerUserId 可选维）', () => {
  it('同参同键、异参异键；null 维归一为空段', () => {
    expect(buildObjectAuthKey('lead', 100n, 1n)).toBe(buildObjectAuthKey('lead', 100n, 1n))
    expect(buildObjectAuthKey('lead', 100n, 1n)).not.toBe(buildObjectAuthKey('lead', 200n, 1n))
    expect(buildObjectAuthKey('lead', null, null)).toBe(buildObjectAuthKey('lead', undefined, undefined))
    expect(buildObjectAuthKey('lead', 100n, null)).not.toBe(buildObjectAuthKey('lead', null, 100n))
  })
})

describe('resolveActionAllowed：动作消费（null 域 fail-closed，精确匹配）', () => {
  it('null 快照（未接入域）→ 一律 false（不因未接入而放宽）', () => {
    expect(resolveActionAllowed(null, 'lead:query')).toBe(false)
  })

  it('授权动作 true；未授权动作 false；大小写精确', () => {
    const snapshot: ObjectAuthSnapshot = {
      allowedActions: ['lead:query'],
      authorizedFields: null,
      maskedFields: null
    }
    expect(resolveActionAllowed(snapshot, 'lead:query')).toBe(true)
    expect(resolveActionAllowed(snapshot, 'lead:update')).toBe(false)
    expect(resolveActionAllowed(snapshot, 'LEAD:QUERY')).toBe(false)
  })

  it('空集 allowedActions → false（恒非 null 合同下的零动作语义）', () => {
    const snapshot: ObjectAuthSnapshot = {
      allowedActions: [],
      authorizedFields: null,
      maskedFields: null
    }
    expect(resolveActionAllowed(snapshot, 'lead:query')).toBe(false)
  })
})

describe('resolveFieldPresentation：字段消费三态（hidden/masked/clear）', () => {
  it('null 快照 → hidden（未接入域不展示任何业务字段）', () => {
    expect(resolveFieldPresentation(null, 'name')).toBe('hidden')
  })

  it('authorizedFields=null（未启用字段级输出）→ clear（.A 零变化合同，不得误判全隐）', () => {
    const snapshot: ObjectAuthSnapshot = {
      allowedActions: [],
      authorizedFields: null,
      maskedFields: null
    }
    expect(resolveFieldPresentation(snapshot, 'name')).toBe('clear')
    expect(resolveFieldPresentation(snapshot, 'cost')).toBe('clear')
  })

  it('字段不在 authorizedFields → hidden（隐藏字段不得经详情旁路展示）', () => {
    const snapshot: ObjectAuthSnapshot = {
      allowedActions: [],
      authorizedFields: ['name'],
      maskedFields: []
    }
    expect(resolveFieldPresentation(snapshot, 'cost')).toBe('hidden')
  })

  it('字段在 authorizedFields 且在 maskedFields → masked（渲染服务端脱敏值 + 脱敏标识）', () => {
    const snapshot: ObjectAuthSnapshot = {
      allowedActions: [],
      authorizedFields: ['name', 'phone'],
      maskedFields: ['phone']
    }
    expect(resolveFieldPresentation(snapshot, 'phone')).toBe('masked')
    expect(resolveFieldPresentation(snapshot, 'name')).toBe('clear')
  })

  it('maskedFields=null 而 authorizedFields 启用 → 命中字段 clear（无脱敏集合=无脱敏字段）', () => {
    const snapshot: ObjectAuthSnapshot = {
      allowedActions: [],
      authorizedFields: ['name'],
      maskedFields: null
    }
    expect(resolveFieldPresentation(snapshot, 'name')).toBe('clear')
  })
})

describe('useObjectAuthorizationStore：唯一写路径=服务端响应 + 会话清理', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    h.getImpl.mockReset()
  })

  it('fetch 拉取服务端响应并缓存；同键第二次 fetch 不再发起请求', async () => {
    h.getImpl.mockResolvedValueOnce({
      allowedActions: ['lead:query'],
      authorizedFields: ['name'],
      maskedFields: []
    })
    const store = useObjectAuthorizationStore()

    const first = await store.fetchObjectAuthorization('lead', 100n, 1n)
    expect(resolveActionAllowed(first, 'lead:query')).toBe(true)
    const second = await store.fetchObjectAuthorization('lead', 100n, 1n)
    // Pinia reactive 包装下每次读取返回新代理：以「深等 + 单次请求」断言缓存命中
    expect(second).toStrictEqual(first)
    expect(h.getImpl).toHaveBeenCalledTimes(1)
  })

  it('未接入域（服务端 data=null）缓存为 null 快照，后续读取 fail-closed 且不重复请求', async () => {
    h.getImpl.mockResolvedValueOnce(null)
    const store = useObjectAuthorizationStore()

    const snapshot = await store.fetchObjectAuthorization('unknown-type', null, null)
    expect(snapshot).toBeNull()
    const again = await store.fetchObjectAuthorization('unknown-type', null, null)
    expect(again).toBeNull()
    expect(h.getImpl).toHaveBeenCalledTimes(1)
    expect(resolveActionAllowed(again, 'lead:query')).toBe(false)
  })

  it('clear 后缓存清空，下一次 fetch 重新请求服务端（撤权后的新快照只来自服务端本次响应）', async () => {
    h.getImpl.mockResolvedValue({
      allowedActions: ['lead:query'],
      authorizedFields: null,
      maskedFields: null
    })
    const store = useObjectAuthorizationStore()

    await store.fetchObjectAuthorization('lead', 100n, null)
    store.clearObjectAuthorizationState()
    await store.fetchObjectAuthorization('lead', 100n, null)
    expect(h.getImpl).toHaveBeenCalledTimes(2)
  })

  it('clearObjectAuthorizationState 幂等且同步（供 authSession 单一入口调用）', () => {
    const store = useObjectAuthorizationStore()
    expect(() => {
      store.clearObjectAuthorizationState()
      store.clearObjectAuthorizationState()
    }).not.toThrow()
  })

  it('异维键不串缓存：同 objectType 不同 orgId 各自拉取', async () => {
    h.getImpl.mockImplementation((params: any) =>
      Promise.resolve({
        allowedActions: [String(params.orgId) === '100' ? 'lead:query' : 'lead:update'],
        authorizedFields: null,
        maskedFields: null
      })
    )
    const store = useObjectAuthorizationStore()

    const a = await store.fetchObjectAuthorization('lead', 100n, null)
    const b = await store.fetchObjectAuthorization('lead', 200n, null)
    expect(resolveActionAllowed(a, 'lead:query')).toBe(true)
    expect(resolveActionAllowed(b, 'lead:update')).toBe(true)
    expect(h.getImpl).toHaveBeenCalledTimes(2)
  })
})
