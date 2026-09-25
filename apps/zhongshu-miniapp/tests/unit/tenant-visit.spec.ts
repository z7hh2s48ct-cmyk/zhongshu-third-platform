import { describe, expect, it, vi } from 'vitest'
import {
  buildVisitTenantOptions,
  performVisitSwitch,
  planVisitSwitch,
  resolveStaleVisit,
} from '@/utils/tenant-visit'

// ZS-CLIENT-002.B：跨组织访问（visit）切换的纯逻辑单元测试（获批业务组织导航）。
//
// 被测 @/utils/tenant-visit：从服务端下发的「我的授权目标」（listMyAuthorizedTargets）构建切换选项、
// 判定切换计划（noop / restore / next）、识别失效访问态（授权撤销 / 过期 / 脏状态），并执行
// 「设置访问租户 → 清字典缓存 → 重新拉取权限 → reLaunch 工作台」的原子切换（失败回滚，不遗留半切态）。
//
// 设计约束：
//   - 服务端授权是唯一真相源：目标列表来自服务端，客户端不做授权推导；
//   - 切换入口按「服务端返回目标列表」显隐（未获批验无切换入口），不再依赖旧粗粒度 system:tenant:visit；
//   - 回滚原子性：refreshUserInfo 失败必须恢复原访问目标并清缓存，绝不留下「半切换」状态；
//   - 本模块不依赖 uni / pinia，IO 经参数注入（纯函数可测模式）。

/** 构造服务端目标条目（VisitTarget） */
const target = (tenantId: number, tenantName = `租户${tenantId}`) => ({
  tenantId,
  tenantName,
  targetOrgIds: null as number[] | null,
  validTo: null as string | null,
})

/** 构造可控 IO（模拟 user/index.vue 注入的真实实现） */
const makeIO = () => ({
  setVisitTenantId: vi.fn(),
  clearDictCache: vi.fn(),
  refreshUserInfo: vi.fn().mockResolvedValue(undefined),
  reLaunch: vi.fn(),
  toast: vi.fn(),
})

describe('buildVisitTenantOptions：切换选项构建（登录租户恒为首项 + 去重 + 当前标记）', () => {
  it('登录租户恒为首项并标记 isLoginTenant；授权目标依序排列', () => {
    const options = buildVisitTenantOptions(1, '总部', [target(2, '华东'), target(3, '华南')], null)
    expect(options).toHaveLength(3)
    expect(options[0]).toMatchObject({ tenantId: 1, name: '总部', isLoginTenant: true, isCurrentVisit: false })
    expect(options[1]).toMatchObject({ tenantId: 2, name: '华东', isLoginTenant: false })
    expect(options[2]).toMatchObject({ tenantId: 3, name: '华南', isLoginTenant: false })
  })
  it('当前访问目标标记 isCurrentVisit；登录项永不标记（登录项显示「当前登录」）', () => {
    const options = buildVisitTenantOptions(1, '总部', [target(2)], 2)
    expect(options[0].isCurrentVisit).toBe(false)
    expect(options[1].isCurrentVisit).toBe(true)
  })
  it('重复目标 / 与登录租户同 id 的目标被剔除（防御服务端已过滤，保留首个出现）', () => {
    const options = buildVisitTenantOptions(1, '总部', [target(2), target(2, '华东二号'), target(1, '总部别名')], null)
    expect(options.map(o => o.tenantId)).toEqual([1, 2])
    expect(options[1].name).toBe('租户2')
  })
  it('无授权目标（未获批）：仅登录租户一项', () => {
    const options = buildVisitTenantOptions(1, '总部', [], null)
    expect(options).toHaveLength(1)
    expect(options[0].isLoginTenant).toBe(true)
  })
})

describe('planVisitSwitch：切换计划判定（noop / restore / next）', () => {
  it('选择登录租户且处于访问态：restore（清理为 null，可回滚到原访问目标）', () => {
    expect(planVisitSwitch(1, 1, 2)).toEqual({ action: 'restore', tenantId: null, previousVisitTenantId: 2 })
  })
  it('已在登录态选择登录租户：noop（不产生任何切换）', () => {
    expect(planVisitSwitch(1, 1, null)).toEqual({ action: 'noop', tenantId: null, previousVisitTenantId: null })
  })
  it('选择当前访问目标：noop（幂等，不重复切换）', () => {
    expect(planVisitSwitch(2, 1, 2)).toEqual({ action: 'noop', tenantId: 2, previousVisitTenantId: 2 })
  })
  it('选择其他授权目标：next（记录原访问目标供失败回滚）', () => {
    expect(planVisitSwitch(3, 1, 2)).toEqual({ action: 'next', tenantId: 3, previousVisitTenantId: 2 })
    expect(planVisitSwitch(3, 1, null)).toEqual({ action: 'next', tenantId: 3, previousVisitTenantId: null })
  })
})

describe('resolveStaleVisit：失效访问态清理判定（授权撤销 / 过期 / 脏状态）', () => {
  it('无访问态：不需清理', () => {
    expect(resolveStaleVisit(null, 1, [])).toBe(false)
  })
  it('访问态等于登录租户（脏状态，服务端判定等效于非跨组织）：需清理', () => {
    expect(resolveStaleVisit(1, 1, [target(2)])).toBe(true)
  })
  it('访问目标已不在授权列表（撤销 / 过期 / 非平台）：需清理', () => {
    expect(resolveStaleVisit(2, 1, [target(3)])).toBe(true)
    expect(resolveStaleVisit(2, 1, [])).toBe(true)
  })
  it('访问目标仍在授权列表：保持', () => {
    expect(resolveStaleVisit(2, 1, [target(2)])).toBe(false)
  })
})

describe('performVisitSwitch：切换执行与失败回滚（顺序 + 回滚原子性）', () => {
  it('noop：不产生任何 IO 调用', async () => {
    const io = makeIO()
    const ok = await performVisitSwitch(io, { action: 'noop', tenantId: null, previousVisitTenantId: null })
    expect(ok).toBe(true)
    expect(io.setVisitTenantId).not.toHaveBeenCalled()
    expect(io.clearDictCache).not.toHaveBeenCalled()
    expect(io.refreshUserInfo).not.toHaveBeenCalled()
    expect(io.reLaunch).not.toHaveBeenCalled()
  })
  it('next：设置访问租户 → 清字典缓存 → 刷新权限 → reLaunch 工作台（严格顺序）', async () => {
    const io = makeIO()
    const ok = await performVisitSwitch(io, { action: 'next', tenantId: 2, previousVisitTenantId: null })
    expect(ok).toBe(true)
    expect(io.setVisitTenantId).toHaveBeenCalledWith(2)
    expect(io.reLaunch).toHaveBeenCalledWith('/pages/index/index')
    const order = [
      io.setVisitTenantId.mock.invocationCallOrder[0],
      io.clearDictCache.mock.invocationCallOrder[0],
      io.refreshUserInfo.mock.invocationCallOrder[0],
      io.reLaunch.mock.invocationCallOrder[0],
    ]
    expect(order[0]).toBeLessThan(order[1])
    expect(order[1]).toBeLessThan(order[2])
    expect(order[2]).toBeLessThan(order[3])
  })
  it('restore：恢复登录租户 = 设置 null 并完成缓存刷新与导航', async () => {
    const io = makeIO()
    const ok = await performVisitSwitch(io, { action: 'restore', tenantId: null, previousVisitTenantId: 2 })
    expect(ok).toBe(true)
    expect(io.setVisitTenantId).toHaveBeenCalledWith(null)
    expect(io.clearDictCache).toHaveBeenCalled()
    expect(io.refreshUserInfo).toHaveBeenCalled()
    expect(io.reLaunch).toHaveBeenCalledWith('/pages/index/index')
  })
  it('刷新失败：回滚到原访问目标 + 清缓存 + 提示，不 reLaunch、返回 false', async () => {
    const io = makeIO()
    io.refreshUserInfo.mockRejectedValueOnce(new Error('403'))
    const ok = await performVisitSwitch(io, { action: 'next', tenantId: 2, previousVisitTenantId: 1 })
    expect(ok).toBe(false)
    expect(io.setVisitTenantId).toHaveBeenNthCalledWith(1, 2)
    expect(io.setVisitTenantId).toHaveBeenNthCalledWith(2, 1)
    expect(io.clearDictCache).toHaveBeenCalledTimes(2)
    expect(io.toast).toHaveBeenCalled()
    expect(io.reLaunch).not.toHaveBeenCalled()
  })
})
