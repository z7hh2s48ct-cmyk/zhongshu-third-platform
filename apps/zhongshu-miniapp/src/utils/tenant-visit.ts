// ZS-CLIENT-002.B：跨组织访问（visit）切换纯逻辑模块（获批业务组织导航）。
//
// 服务端授权是唯一真相源：目标列表来自 `GET /system/cross-org-visit/my-targets`（listMyAuthorizedTargets），
// 客户端不做授权推导——仅消费服务端结论构建切换选项 / 判定切换计划 / 识别失效访问态 / 执行原子切换。
//
// 设计约束：
//   - 切换入口按「服务端返回目标列表」显隐（未获批验无切换入口），不再依赖旧粗粒度 system:tenant:visit；
//   - 回滚原子性：refreshUserInfo 失败必须恢复原访问目标并清缓存，绝不留下「半切换」状态；
//   - 本模块不依赖 uni / pinia，IO 经参数注入（纯函数可测模式），由 pages/user/index.vue 注入真实实现。

/** 服务端下发的授权目标条目（对齐 CrossOrgVisitMyTargetsRespVO.TargetVO） */
export interface VisitTarget {
  tenantId: number
  tenantName: string
  targetOrgIds: number[] | null // 限定组织范围（null = 目标租户内全部组织）
  validTo: string | null // 授权有效期至（null = 无固定期限）
}

/** 切换选项（登录租户恒为首项） */
export interface VisitTenantOption {
  tenantId: number
  name: string
  isLoginTenant: boolean
  isCurrentVisit: boolean
}

/** 切换计划：noop=无操作（幂等）；restore=恢复登录租户（清空访问态）；next=切换到其他目标 */
export interface VisitSwitchPlan {
  action: 'noop' | 'restore' | 'next'
  tenantId: number | null
  previousVisitTenantId: number | null
}

/** 切换执行 IO（由页面注入真实实现；toast 可选） */
export interface VisitSwitchIO {
  setVisitTenantId: (id: number | null) => void
  clearDictCache: () => void
  refreshUserInfo: () => Promise<void>
  reLaunch: (url: string) => void
  toast?: (message: string) => void
}

/**
 * 构建切换选项：登录租户恒为首项（标记 isLoginTenant，永不标记 isCurrentVisit——登录项显示「当前登录」）；
 * 授权目标依服务端顺序排列、去重（防御服务端已过滤，重复 / 与登录同 id 保留首现）；
 * 当前访问目标标记 isCurrentVisit。
 */
export function buildVisitTenantOptions(
  loginTenantId: number,
  loginTenantName: string,
  targets: VisitTarget[],
  currentVisitTenantId: number | null,
): VisitTenantOption[] {
  const options: VisitTenantOption[] = [{
    tenantId: loginTenantId,
    name: loginTenantName,
    isLoginTenant: true,
    isCurrentVisit: false,
  }]
  const seen = new Set<number>([loginTenantId])
  for (const target of targets ?? []) {
    if (seen.has(target.tenantId)) {
      continue
    }
    seen.add(target.tenantId)
    options.push({
      tenantId: target.tenantId,
      name: target.tenantName,
      isLoginTenant: false,
      isCurrentVisit: currentVisitTenantId === target.tenantId,
    })
  }
  return options
}

/**
 * 判定切换计划：
 * - 选择登录租户：已在登录态 → noop；处于访问态 → restore（清理为 null，可失败回滚到原访问目标）；
 * - 选择当前访问目标 → noop（幂等，不重复切换）；
 * - 选择其他授权目标 → next（记录原访问目标供失败回滚）。
 */
export function planVisitSwitch(
  selection: number,
  loginTenantId: number,
  currentVisitTenantId: number | null,
): VisitSwitchPlan {
  if (selection === loginTenantId) {
    return currentVisitTenantId == null
      ? { action: 'noop', tenantId: null, previousVisitTenantId: null }
      : { action: 'restore', tenantId: null, previousVisitTenantId: currentVisitTenantId }
  }
  if (selection === currentVisitTenantId) {
    return { action: 'noop', tenantId: selection, previousVisitTenantId: currentVisitTenantId }
  }
  return { action: 'next', tenantId: selection, previousVisitTenantId: currentVisitTenantId }
}

/**
 * 识别失效访问态（需清理）：访问态等于登录租户（脏状态，服务端判定等效于非跨组织）、
 * 访问目标已不在授权列表（授权撤销 / 过期 / 非平台角色）；无访问态则无需清理。
 */
export function resolveStaleVisit(
  visitTenantId: number | null,
  loginTenantId: number,
  targets: VisitTarget[],
): boolean {
  if (visitTenantId == null) {
    return false
  }
  if (visitTenantId === loginTenantId) {
    return true
  }
  return !(targets ?? []).some(target => target.tenantId === visitTenantId)
}

/**
 * 执行切换（原子）：noop 零 IO 返回 true；否则按「设置访问租户 → 清字典缓存 → 重新拉取权限 → reLaunch 工作台」
 * 顺序执行；任一步失败（典型为权限刷新 403：目标授权已被撤销）→ 回滚原访问目标 + 清缓存 + 提示，
 * 不 reLaunch、返回 false（绝不遗留半切态）。
 */
export async function performVisitSwitch(io: VisitSwitchIO, plan: VisitSwitchPlan): Promise<boolean> {
  if (plan.action === 'noop') {
    return true
  }
  try {
    io.setVisitTenantId(plan.tenantId)
    io.clearDictCache()
    await io.refreshUserInfo()
    io.reLaunch('/pages/index/index')
    return true
  } catch {
    io.setVisitTenantId(plan.previousVisitTenantId)
    io.clearDictCache()
    io.toast?.('切换失败，已恢复原有访问范围')
    return false
  }
}
