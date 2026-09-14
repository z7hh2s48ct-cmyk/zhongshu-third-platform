/**
 * ZS-CLIENT-002.A：移动端「服务端授权导航」路由权限注册表与直达守卫判定。
 *
 * 设计原则（对齐 docs/05 ZS-CLIENT-002.A 与 B04 Wave3 计划 §5 交付 1/2/4/5）：
 * - 客户端只做「服务端下发权限标识」的集合成员判断，不在前端重算业务权限；
 * - 注册表直接消费工作台菜单声明 `pages/index/menu.json`（url→permission，与后端 @PreAuthorize 对齐），
 *   与工作台菜单过滤（`pages/index/index.ts` 的 hasMenuAccess）同源同语义，杜绝前端另立一套粗粒度映射
 *   而与服务端授权分歧（如把「我的流程」bpm:process-instance:query 误当 bpm:task:query 拒绝）；
 * - 语义对齐 hasMenuAccess：菜单项无 permission → 登录即放行（如 AI 会话 / IM）；有 permission(s) → 任一命中即放行；
 * - 计划 §5 交付 2「未注册/未授权页面不可进入」+ 交付 4「关闭模块提供不可用落点」：
 *   公共应用外壳（工作台 / 消息 / 我的 / pages-core 登录·错误·个人中心）显式放行；业务页面按注册表授权；
 *   既非公共、又未在注册表登记（含子页继承兜底）的页面 → 拒绝（由 interceptor reLaunch 到 403 落点）；
 * - 子页面（详情 / 表单 / 办理 / 收银台等，menu.json 只登记菜单入口页）继承与显式登记：
 *   ① 最近祖先目录前缀继承（同目录子页，含登录即用目录如 AI 会话 / IM）——命中即按其权限判定，未通过则拒绝，
 *     不再回退到更宽的模块并集（否则持无关权限者可经模块兜底进入受控子页，违背交付 2「未授权页面不可进入」）；
 *   ② 真正跨包 / 跨目录共享的子页（如 BPM 审批详情：主包审批入口 /pages/bpm/ 与分包管理入口 /pages-bpm/…/manager 复用）
 *     在 EXTRA_ROUTE_ACCESS 显式登记其权限并集；③ 皆未命中 → 拒绝（未注册 / 未启用模块）；
 *   单条数据的细粒度授权由服务端 API 兜底；
 * - 不把 Web component 路径当移动路由（menu.json 仅含 /pages 移动路径）；
 * - D-09 之前不实现多身份切换。
 */
import menuConfig from '@/pages/index/menu.json'

/** 授权失败落点页：已登录但无页面授权 → reLaunch 到此（与 404「页面不存在」区分） */
export const UNAUTHORIZED_PAGE = '/pages-core/error/403'

/**
 * 公共应用外壳路由前缀：任一登录用户可达，不受业务权限门禁。
 * 均为非业务模块的 App 外壳页（登录 / 错误落点 / 个人中心工具 / 工作台外壳 / 消息收件箱）。
 */
const PUBLIC_ROUTE_PREFIXES = [
  '/pages-core/', // 登录·注册·错误落点(403/404/pc-only)·个人中心(资料/安全/设置/反馈/FAQ)
  '/pages/index/', // 工作台及其搜索 / 设置
  '/pages/message/', // 消息（个人收件箱）
  '/pages/user/', // 我的（个人中心）
]

/** menu.json 菜单项（仅取授权判定相关字段） */
interface RawMenuItem {
  url?: string
  permission?: string
  permissions?: string[]
  onlyPc?: boolean
}
interface RawSubGroup { key: string, name: string, menus: RawMenuItem[] }
interface RawGroup { key: string, name: string, subGroups: RawSubGroup[] }

/**
 * BPM 审批流「跨包共享子页」的授权并集：审批详情 / 发起等子页物理位于分包 /pages-bpm/processInstance/**，
 * 却同时被两个入口流复用——主包审批入口 /pages/bpm/index（process-instance / task / cc 三权限）与分包管理入口
 * /pages-bpm/processInstance/manager（manager-query）。menu.json 仅登记管理入口，若只按目录前缀继承会漏掉主包
 * 审批用户（普通审批人打不开自己发起 / 待办流程的详情），故显式登记这些真正共享的子页为「四权限任一命中即放行」。
 * 单条流程实例的数据级授权仍由服务端 BPM API 兜底，客户端守卫只确保主体属于该审批流。
 */
const BPM_SHARED_APPROVAL_PERMISSIONS = [
  'bpm:process-instance:query',
  'bpm:task:query',
  'bpm:process-instance-cc:query',
  'bpm:process-instance:manager-query',
]

/**
 * 未在 menu.json 逐条声明、但需显式授权 / 放行的移动页（permissions 为空 = 登录即用）：
 * - 通讯录（主包 TabBar）：消费用户列表，与后端 system:user:list 对齐（同工作台「用户管理」权限）；
 * - 收银台：从登录即用的 demo 订单流 /pages-pay/demo/order 跨目录跳入，虽处 pay 管理包但非管理页，
 *   故登录即用；实际支付归属与金额由服务端支付 API 校验，客户端不做业务授权；
 * - BPM 审批详情 / 发起（跨包共享子页）：见上 BPM_SHARED_APPROVAL_PERMISSIONS 说明——显式登记其子目录前缀，
 *   使 detail/audit、detail/reassign、create/form 等派生子页经最近祖先前缀继承到审批流并集。
 */
const EXTRA_ROUTE_ACCESS: Array<{ path: string, permissions: string[] }> = [
  { path: '/pages/contact/index', permissions: ['system:user:list'] },
  { path: '/pages-pay/cashier/index', permissions: [] },
  { path: '/pages-bpm/processInstance/detail/index', permissions: BPM_SHARED_APPROVAL_PERMISSIONS },
  { path: '/pages-bpm/processInstance/create/index', permissions: BPM_SHARED_APPROVAL_PERMISSIONS },
]

/** 归一化移动路由：剥离 query、补前导斜杠（与 tabbar normalizeRoutePath 同语义） */
export function normalizeRoute(route: string): string {
  const path = route.split('?')[0]
  return path.startsWith('/') ? path : `/${path}`
}

/**
 * 由页面路径推导其全部祖先目录前缀（从最近父目录逐级上溯至包根），供未登记子页按最近祖先继承。
 * 排除过宽的 `/pages/`（主包根，会覆盖全部主包页）与 `/`（根，会使默认拒绝失效）。
 * 例：/pages-bpm/processInstance/detail/index → ['/pages-bpm/processInstance/detail/', '/pages-bpm/processInstance/', '/pages-bpm/']
 */
function ancestorPrefixes(path: string): string[] {
  const parts = path.split('/').filter(Boolean)
  const prefixes: string[] = []
  for (let i = parts.length - 1; i >= 1; i--) {
    prefixes.push(`/${parts.slice(0, i).join('/')}/`)
  }
  return prefixes.filter(prefix => prefix !== '/pages/' && prefix !== '/')
}

/** 精确路由 → 所需权限（并集）；空数组 = 已登记但无需权限（登录即放行，如 AI 会话 / IM / 收银台） */
const exactRegistry = new Map<string, string[]>()
/** 祖先目录前缀 → 所需权限（并集），供 menu.json 未逐条登记的子页按最近祖先目录继承（含登录即用目录） */
const prefixRegistry = new Map<string, string[]>()

function addMapping(map: Map<string, string[]>, key: string, perms: string[]) {
  if (key === '')
    return
  const existing = map.get(key)
  if (!existing) {
    map.set(key, [...perms])
    return
  }
  for (const p of perms) {
    if (!existing.includes(p))
      existing.push(p)
  }
}

function registerRoute(url: string, permission?: string, permissions?: string[]) {
  const path = normalizeRoute(url)
  const perms = [permission, ...(permissions ?? [])].filter(Boolean) as string[]
  addMapping(exactRegistry, path, perms)
  for (const prefix of ancestorPrefixes(path)) {
    addMapping(prefixRegistry, prefix, perms)
  }
}

// 从 menu.json 构建注册表（单一真相源，与工作台菜单过滤同源）
for (const group of (menuConfig as { groups: RawGroup[] }).groups) {
  for (const sub of group.subGroups ?? []) {
    for (const item of sub.menus ?? []) {
      // onlyPc 菜单在移动端落地为公共 pc-only 提示页（pages-core），不参与业务路由登记
      if (!item.url || item.onlyPc)
        continue
      registerRoute(item.url, item.permission, item.permissions)
    }
  }
}
// 补充未声明于 menu.json 的受保护页 / 跨目录登录即用落点
for (const extra of EXTRA_ROUTE_ACCESS) {
  registerRoute(extra.path, undefined, extra.permissions)
}

/** 权限集合判定：无需权限（空）→ 放行；否则任一命中即放行（对齐 useAccess.hasAccessByCodes 的 ANY 语义） */
function allowByPermissions(required: string[], held: string[]): boolean {
  return required.length === 0 || required.some(code => held.includes(code))
}

/** 最长祖先目录前缀匹配：为未逐条登记的子页继承其最近已登记祖先目录的权限（含登录即用目录） */
function matchLongestPrefix(path: string): string[] | undefined {
  let best: string | undefined
  let bestPerms: string[] | undefined
  // Map.forEach 规避 for...of 迭代 MapIterator（当前 tsconfig target 未开 downlevelIteration）
  prefixRegistry.forEach((perms, prefix) => {
    if (path.startsWith(prefix) && (best === undefined || prefix.length > best.length)) {
      best = prefix
      bestPerms = perms
    }
  })
  return bestPerms
}

/**
 * 判定已登录主体是否有权进入某移动路由。
 *
 * 四级判定（计划 §5 交付 2「未注册/未授权页面不可进入」+ 交付 4「关闭模块提供不可用落点」）：
 * ① 公共应用外壳放行；② 精确命中（菜单入口 / 显式补登的跨包共享子页）强制其声明权限；
 * ③ 子页按最近祖先目录前缀继承——命中即按其权限判定，未通过则拒绝（保留具体子目录的拒绝，不回退更宽并集）；
 * ④ 皆未命中 → 拒绝。空权限=登录即用、否则任一命中即放行。
 *
 * @param route 目标路由（可带 query，内部归一化）
 * @param permissions 服务端下发的权限标识集合（useUserStore().permissions）
 * @returns true=放行；false=拒绝（拦截到 UNAUTHORIZED_PAGE）
 */
export function hasRouteAccess(route: string, permissions: string[]): boolean {
  const path = normalizeRoute(route)
  const held = permissions ?? []
  // 1) 公共应用外壳（含根路径）：登录即放行
  if (path === '/' || PUBLIC_ROUTE_PREFIXES.some(prefix => path.startsWith(prefix))) {
    return true
  }
  // 2) 精确命中（菜单入口 / 显式补登）：强制其声明的具体权限——未授权即拒（保「未注册/未授权不可进入」，且 BPM 各页权限不被压平）
  const exact = exactRegistry.get(path)
  if (exact !== undefined) {
    return allowByPermissions(exact, held)
  }
  // 3) 子页按最近祖先目录前缀继承（详情 / 表单 / 办理等 menu.json 未逐条登记者）：取最长匹配前缀的权限判定，
  //    命中即返回其判定——通过则放行、未通过则拒绝（保留具体子目录的拒绝，不再回退到更宽的模块并集，
  //    否则持无关权限者会经模块兜底进入受控子页，如仅持 system:dept:query 却进入 /pages-system/user/detail——codex r2-P2-1）；
  //    登录即用目录（如 AI 会话 / IM）所需权限为空，任意登录用户可达
  const inherited = matchLongestPrefix(path)
  if (inherited !== undefined) {
    return allowByPermissions(inherited, held)
  }
  // 4) 既非公共、又未登记（含未注册 / 未启用模块）→ 拒绝（交付 2「未注册页面不可进入」+ 交付 4「关闭模块落点」）
  return false
}
