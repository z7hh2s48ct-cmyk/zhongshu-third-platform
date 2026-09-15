/**
 * ZS-CLIENT-001.A —— Web 管理端「路由 → 授权」注册表与判定核心（纯 TS，无 Vue 运行时依赖）。
 *
 * 设计对齐移动端姊妹项 ZS-CLIENT-002.A（apps/zhongshu-miniapp/src/router/access.ts）：
 *   1. 单一真相源：移动端是 pages/index/menu.json；Web 端是服务端
 *      `/system/auth/get-permission-info` 下发的菜单树（AuthPermissionInfoRespVO.menus）。
 *      后端已按「运行模块白名单 ZS-CFG-003.A」×「套餐/角色权限交集 ZS-CFG-003.B」过滤该树，
 *      因此客户端只做集合成员判断，**不在前端重算业务权限**。
 *   2. 未注册 / 未授权 → 默认拒绝（CLIENT-002.A codex r0-P1 教训）。
 *   3. 本地静态声明的隐藏子页（router/modules/remaining.ts）显式补登锚点，
 *      避免默认拒绝误伤合法页（r1-P1 教训）。
 *   4. 命中即返回，不回退到更宽的模块级并集（r2-P2 教训）。
 *
 * 与移动端的关键差异：AuthPermissionInfoRespVO.MenuVO **不携带 permission 字段**，
 * 所以判定单位是「服务端是否下发该菜单路径」，而不是「是否持有某权限标识」。
 * 动作级 / 字段级授权（allowedActions）消费属 ZS-CLIENT-001.B（B08，前置 ZS-PERM-003），本批不做。
 */

/** 站点根路径 */
export const ROOT_ROUTE = '/'
/** 登录后默认落点（remaining.ts 中 `/` → redirect `/index`） */
export const HOME_ROUTE = '/index'
/** 登录页 */
export const LOGIN_ROUTE = '/login'
/** 未授权落点（remaining.ts NoAccess） */
export const UNAUTHORIZED_ROUTE = '/403'
/** 未找到落点（remaining.ts NoFound） */
export const NOT_FOUND_ROUTE = '/404'
/** 服务端错误落点（remaining.ts Error） */
export const SERVER_ERROR_ROUTE = '/500'
/** SSO 登录回调落地页（remaining.ts SSOLogin，渲染 Login.vue） */
export const SSO_ROUTE = '/sso'
/** 页签刷新助手路由前缀（remaining.ts Redirect） */
export const REDIRECT_ROUTE_PREFIX = '/redirect/'
/** 外链菜单落地路由前缀（utils/routerHelper.ts EXTERNAL_LINK_ROUTE_PREFIX） */
export const EXTERNAL_LINK_ROUTE_PREFIX = '/external-link/'

/**
 * 登录前白名单：沿用 src/permission.ts 原有集合，**逐项精确匹配**，不改变既有可达性。
 * 这些页面在未持有 token 时也可访问。
 *
 * 刻意不做前缀通配：`/oauthLogin/*` 在 remaining.ts 中**不存在任何对应路由**（全仓仅
 * permission.ts 一处引用 `/oauthLogin/gitee`），前缀放行只会无意义地扩大未认证可达面。
 */
export const PRE_AUTH_ROUTE_PATTERNS: string[] = [
  LOGIN_ROUTE,
  '/social-login',
  '/auth-redirect',
  '/bind',
  '/register',
  '/oauthLogin/gitee'
]

/**
 * 公共应用外壳：登录即放行，不参与业务授权门禁。
 * 只登记 remaining.ts 中静态声明的外壳页，业务页一律不在此列。
 */
export const PUBLIC_SHELL_PATTERNS: string[] = [
  ROOT_ROUTE,
  HOME_ROUTE,
  LOGIN_ROUTE,
  UNAUTHORIZED_ROUTE,
  NOT_FOUND_ROUTE,
  SERVER_ERROR_ROUTE,
  SSO_ROUTE,
  '/social-login',
  '/auth-redirect',
  '/bind',
  '/register',
  '/user/profile',
  '/user/notify-message'
]

/** 公共外壳前缀：必须带尾部斜杠，杜绝 `/redirectX` 之类前缀伪装绕过 */
export const PUBLIC_SHELL_PREFIXES: string[] = [REDIRECT_ROUTE_PREFIX]

/**
 * 服务端菜单节点（AuthPermissionInfoRespVO.MenuVO 的最小结构投影）。
 *
 * 刻意**不带索引签名**：`AppCustomRouteRecordRaw` 是 interface（无隐式索引签名），
 * 若这里加 `[key: string]: unknown` 会导致菜单数组无法直接赋值给本类型，
 * 进而在 G10 类型基线上新增错误。
 */
export interface MenuRouteNode {
  path?: string
  children?: MenuRouteNode[]
}

/**
 * 一次会话的授权快照。由服务端本次响应构建，**不来自 localStorage**：
 * 撤权 / 换租户后重建快照即可让旧授权立即失效。
 */
export interface RouteAccessSnapshot {
  /** 服务端下发菜单解析出的绝对路径模式（可含 `:param` 段） */
  menuPatterns: string[]
  /** 由 menuPatterns 派生的祖先目录前缀，用于「锚点目录内存在任一授权菜单」判定 */
  menuPrefixes: string[]
  /** 是否下发了外链菜单（决定 `/external-link/**` 落点是否可达） */
  hasExternalMenu: boolean
  /**
   * 运行模块白名单（ZS-CFG-003.A）。
   * `null` 表示前端当前拿不到该信号 → 降级为「服务端菜单存在性」判定：
   * 后端下发的菜单树本身已按运行白名单过滤，因此降级仍然是收敛的（关闭模块不会出现在菜单树里）。
   * 后端接线后传入数组即可启用精确的模块级拒绝。
   */
  enabledModules: string[] | null
}

/** 静态路由补登条目：pattern 为绝对路径模式，anchors 为其合法入口的授权锚点 */
export interface StaticRouteAccessEntry {
  pattern: string
  anchors: string[]
}

const EMPTY_SNAPSHOT: RouteAccessSnapshot = {
  menuPatterns: [],
  menuPrefixes: [],
  hasExternalMenu: false,
  enabledModules: null
}

/**
 * 本地静态路由表（router/modules/remaining.ts）中**无条件注册**的隐藏业务子页补登表。
 *
 * 为什么必须显式补登：这些页面不来自服务端菜单树，若不补登，默认拒绝会把合法页锁死
 * （CLIENT-002.A codex r1-P1）。补登不是放行——anchors 仍须命中服务端已授权菜单。
 *
 * anchors 语义（多入口共享页须登记全部合法入口，不得压平为单一锚点，r0-P2 教训）：
 *   - 锚点精确命中服务端菜单，或
 *   - 锚点目录内存在任一服务端授权菜单（应对 activeMenu 与真实菜单层级不一致）
 */
export const STATIC_ROUTE_ACCESS: StaticRouteAccessEntry[] = [
  // ---- 系统 / 基础设施（activeMenu 指向 system、infra 下的真实菜单）----
  { pattern: '/dict/type/data/:dictType', anchors: ['/system/dict'] },
  { pattern: '/fms/auxiliary/type/item/:auxiliaryTypeId', anchors: ['/fms/config/auxiliary'] },
  // remaining.ts 中这两条 activeMenu 缺前导斜杠（'infra/codegen/index'），此处按归一化后的绝对锚点登记
  { pattern: '/codegen/edit', anchors: ['/infra/codegen', '/infra/codegen/index'] },
  { pattern: '/job/job-log', anchors: ['/infra/job', '/infra/job/index'] },

  // ---- BPM：多入口共享页登记全部合法入口 ----
  { pattern: '/bpm/manager/form/edit', anchors: ['/bpm/manager/form'] },
  { pattern: '/bpm/manager/definition', anchors: ['/bpm/manager/model'] },
  {
    pattern: '/bpm/process-instance/detail',
    anchors: ['/bpm/task/my', '/bpm/task', '/bpm/process-instance', '/bpm/manager/model']
  },
  {
    pattern: '/bpm/process-instance/report',
    anchors: ['/bpm/manager/model', '/bpm/process-instance']
  },
  { pattern: '/bpm/oa/leave/create', anchors: ['/bpm/oa/leave'] },
  { pattern: '/bpm/oa/leave/detail', anchors: ['/bpm/oa/leave'] },
  { pattern: '/bpm/manager/model/create', anchors: ['/bpm/manager/model'] },
  { pattern: '/bpm/manager/model/:type/:id', anchors: ['/bpm/manager/model'] },

  // ---- 商城：商品 / 交易 / 会员 / 收银台 / 装修 ----
  { pattern: '/mall/product/spu/add', anchors: ['/mall/product/spu'] },
  { pattern: '/mall/product/spu/edit/:id', anchors: ['/mall/product/spu'] },
  { pattern: '/mall/product/spu/detail/:id', anchors: ['/mall/product/spu'] },
  // remaining.ts 此处 activeMenu 为 '/product/property'（疑似历史遗留），两个锚点都登记以免误伤
  {
    pattern: '/mall/product/property/value/:propertyId',
    anchors: ['/mall/product/property', '/product/property']
  },
  { pattern: '/mall/trade/order/detail/:id', anchors: ['/mall/trade/order'] },
  { pattern: '/mall/trade/after-sale/detail/:id', anchors: ['/mall/trade/after-sale'] },
  { pattern: '/member/user/detail/:id', anchors: ['/member/user'] },
  // 收银台无 activeMenu；由任意已授权 /pay/** 菜单授权（支付流程由订单页跳入）
  { pattern: '/pay/cashier', anchors: ['/pay'] },
  {
    pattern: '/diy/template/decorate/:id',
    anchors: [
      '/mall/promotion/diy-template/diy-template',
      '/mall/promotion/diy-template',
      '/diy/template'
    ]
  },
  {
    pattern: '/diy/page/decorate/:id',
    anchors: ['/mall/promotion/diy-template/diy-page', '/mall/promotion/diy-template', '/diy/page']
  },

  // ---- CRM：8 个详情子页 ----
  { pattern: '/crm/clue/detail/:id', anchors: ['/crm/clue'] },
  { pattern: '/crm/customer/detail/:id', anchors: ['/crm/customer'] },
  { pattern: '/crm/business/detail/:id', anchors: ['/crm/business'] },
  { pattern: '/crm/contract/detail/:id', anchors: ['/crm/contract'] },
  { pattern: '/crm/receivable-plan/detail/:id', anchors: ['/crm/receivable-plan'] },
  { pattern: '/crm/receivable/detail/:id', anchors: ['/crm/receivable'] },
  { pattern: '/crm/contact/detail/:id', anchors: ['/crm/contact'] },
  { pattern: '/crm/product/detail/:id', anchors: ['/crm/product'] },

  // ---- HRM：14 个详情 / 表单子页 ----
  { pattern: '/hrm/portal/opening-guide', anchors: ['/hrm/portal/home', '/hrm/portal'] },
  { pattern: '/hrm/recruit/post/detail/:id', anchors: ['/hrm/recruit/post'] },
  { pattern: '/hrm/recruit/candidate/detail/:id', anchors: ['/hrm/recruit/candidate'] },
  { pattern: '/hrm/employee/detail/:id', anchors: ['/hrm/employee/list', '/hrm/employee'] },
  { pattern: '/hrm/dept/detail/:id', anchors: ['/hrm/dept'] },
  { pattern: '/hrm/attendance/month/detail/:employeeId', anchors: ['/hrm/attendance/month'] },
  { pattern: '/hrm/performance/plan/detail/:id', anchors: ['/hrm/performance/plan'] },
  { pattern: '/hrm/performance/plan/form', anchors: ['/hrm/performance/plan'] },
  {
    pattern: '/hrm/performance/assessment/employee/:employeeId',
    anchors: ['/hrm/performance/assessment']
  },
  { pattern: '/hrm/performance/assessment/detail/:id', anchors: ['/hrm/performance/assessment'] },
  { pattern: '/hrm/insurance/month-record/detail/:id', anchors: ['/hrm/insurance/month-record'] },
  { pattern: '/hrm/salary/employee-info/detail/:id', anchors: ['/hrm/salary/employee-info'] },
  { pattern: '/hrm/salary/history/detail/:id', anchors: ['/hrm/salary/history'] },
  { pattern: '/hrm/salary/slip/detail/:id', anchors: ['/hrm/salary/slip'] },

  // ---- AI：绘图广场 / 知识库 / 工作流 ----
  { pattern: '/ai/image/square', anchors: ['/ai/image'] },
  { pattern: '/ai/knowledge/document', anchors: ['/ai/knowledge'] },
  { pattern: '/ai/knowledge/document/create', anchors: ['/ai/knowledge'] },
  { pattern: '/ai/knowledge/document/update', anchors: ['/ai/knowledge'] },
  { pattern: '/ai/knowledge/retrieval', anchors: ['/ai/knowledge'] },
  { pattern: '/ai/knowledge/segment', anchors: ['/ai/knowledge'] },
  { pattern: '/ai/console/workflow/create', anchors: ['/ai/console/workflow'] },
  { pattern: '/ai/console/workflow/:type/:id', anchors: ['/ai/console/workflow'] },

  // ---- IoT：路径段与 activeMenu 段序不一致，双锚点登记 ----
  { pattern: '/iot/product/product/detail/:id', anchors: ['/iot/device/product', '/iot/product'] },
  { pattern: '/iot/device/detail/:id', anchors: ['/iot/device/device', '/iot/device'] },
  {
    pattern: '/iot/ota/operation/firmware/detail/:id',
    anchors: ['/iot/operation/ota/firmware', '/iot/ota']
  },

  // ---- MES ----
  { pattern: '/mes/wm/warehouse/location', anchors: ['/mes/wm/warehouse'] },
  { pattern: '/mes/wm/warehouse/area', anchors: ['/mes/wm/warehouse'] },
  { pattern: '/mes/pro/task/gantt-edit', anchors: ['/mes/pro/task'] },

  // ---- IM：remaining.ts 中整棵树 hidden，无 activeMenu，以 /im 模块入口为锚点 ----
  { pattern: '/im', anchors: ['/im'] },
  { pattern: '/im/home', anchors: ['/im'] },
  { pattern: '/im/home/conversation', anchors: ['/im'] },
  { pattern: '/im/home/contact', anchors: ['/im'] }
]

// ---------------------------------------------------------------------------
// 路径归一化与基础判定
// ---------------------------------------------------------------------------

function splitSegments(path: string): string[] {
  return path.split('/').filter((segment) => segment.length > 0)
}

/** 是否为外链地址（绝对 URL / 协议相对 URL / 带 scheme） */
function isExternalUrl(value: string): boolean {
  if (value.startsWith('//')) {
    return true
  }
  return /^[a-zA-Z][a-zA-Z\d+\-.]*:/.test(value)
}

/**
 * 归一化站内路由路径：去 query / hash、补前导斜杠、折叠重复斜杠、去尾部斜杠。
 * 折叠重复斜杠是必需的——否则 `//system///user/` 可绕过前缀判定。
 */
export function normalizeRoutePath(route: string): string {
  if (typeof route !== 'string') {
    return ROOT_ROUTE
  }
  let value = route.trim()
  const hashIndex = value.indexOf('#')
  if (hashIndex !== -1) {
    value = value.slice(0, hashIndex)
  }
  const queryIndex = value.indexOf('?')
  if (queryIndex !== -1) {
    value = value.slice(0, queryIndex)
  }
  value = value.trim()
  if (value.length === 0) {
    return ROOT_ROUTE
  }
  if (!value.startsWith('/')) {
    value = '/' + value
  }
  value = value.replace(/\/{2,}/g, '/')
  if (value.length > 1 && value.endsWith('/')) {
    value = value.slice(0, -1)
  }
  return value.length === 0 ? ROOT_ROUTE : value
}

/**
 * 菜单树路径拼接，对齐 **vue-router 嵌套路由解析**语义：
 * 子路径以 `/` 开头视为绝对路径（直接采用），否则相对父路径拼接。
 *
 * 为何不是 `routerHelper.pathResolve`：`generateRoute` 并不调用 pathResolve，
 * 而是把 `route.path` 原样交给 vue-router（`data.children = generateRoute(route.children)`），
 * 最终绝对路径由 vue-router 的嵌套规则得出。pathResolve 会把 `/user` 拼成 `/system/user`，
 * 与 vue-router 实际注册的路径不一致，因此不能沿用。
 */
function joinMenuPath(parentPath: string, path: string): string {
  const child = path.trim()
  if (child.length === 0) {
    return normalizeRoutePath(parentPath)
  }
  if (child.startsWith('/')) {
    return normalizeRoutePath(child)
  }
  const base = normalizeRoutePath(parentPath)
  return normalizeRoutePath(base === ROOT_ROUTE ? ROOT_ROUTE + child : base + '/' + child)
}

/**
 * 推导模块键（路径首段）。关闭模块守卫以此为单位。
 * 根路径无模块键。
 */
export function resolveModuleKey(path: string): string | null {
  const segments = splitSegments(normalizeRoutePath(path))
  if (segments.length === 0) {
    return null
  }
  return segments[0]
}

/** 是否为登录前白名单页（未持 token 也可访问）：逐项精确匹配，不做前缀通配 */
export function isPreAuthRoute(path: string): boolean {
  return PRE_AUTH_ROUTE_PATTERNS.indexOf(normalizeRoutePath(path)) !== -1
}

/**
 * 是否为公共应用外壳（登录即放行，不受业务授权门禁）。
 *
 * 登录前白名单是其子集：已认证用户必然可访问未认证也能访问的页面，
 * 否则 `/oauthLogin/gitee` 之类白名单项在登录后反而被 403 拦下（可达性回归）。
 */
export function isPublicRoute(path: string): boolean {
  const normalized = normalizeRoutePath(path)
  if (PUBLIC_SHELL_PATTERNS.indexOf(normalized) !== -1) {
    return true
  }
  if (isPreAuthRoute(normalized)) {
    return true
  }
  return PUBLIC_SHELL_PREFIXES.some((prefix) => normalized.startsWith(prefix))
}

/** 段级模式匹配：`:xxx` / `:xxx(\\d+)` 视为通配段，`*` 匹配余下全部 */
function isParamSegment(segment: string): boolean {
  return segment.startsWith(':') || segment === '*'
}

function matchPathPattern(pattern: string, path: string): boolean {
  const patternSegments = splitSegments(pattern)
  const pathSegments = splitSegments(path)
  for (let i = 0; i < patternSegments.length; i++) {
    const segment = patternSegments[i]
    if (segment === '*') {
      return true
    }
    if (i >= pathSegments.length) {
      return false
    }
    if (isParamSegment(segment)) {
      continue
    }
    if (segment !== pathSegments[i]) {
      return false
    }
  }
  return patternSegments.length === pathSegments.length
}

function matchAnyPattern(patterns: string[], path: string): boolean {
  return patterns.some((pattern) => matchPathPattern(pattern, path))
}

// ---------------------------------------------------------------------------
// 快照构建
// ---------------------------------------------------------------------------

interface CollectState {
  patterns: string[]
  hasExternalMenu: boolean
}

function collectMenuNodes(nodes: unknown, parentPath: string, state: CollectState): void {
  if (!Array.isArray(nodes)) {
    return
  }
  nodes.forEach((node: unknown) => {
    if (!node || typeof node !== 'object') {
      return
    }
    const record = node as MenuRouteNode
    const rawPath = typeof record.path === 'string' ? record.path.trim() : ''
    let currentParent = parentPath
    if (rawPath.length > 0) {
      if (isExternalUrl(rawPath)) {
        // 外链菜单不落站内路径，但授权 /external-link/** 落点
        state.hasExternalMenu = true
      } else {
        const resolved = joinMenuPath(parentPath, rawPath)
        if (state.patterns.indexOf(resolved) === -1) {
          state.patterns.push(resolved)
        }
        currentParent = resolved
      }
    }
    collectMenuNodes(record.children, currentParent, state)
  })
}

function ancestorPrefixes(pattern: string): string[] {
  const segments = splitSegments(pattern)
  const prefixes: string[] = []
  // 排除自身与过宽的根前缀（CLIENT-002.A ancestorPrefixes 同款约束）
  for (let i = 1; i < segments.length; i++) {
    prefixes.push(ROOT_ROUTE + segments.slice(0, i).join('/'))
  }
  return prefixes
}

/**
 * 由服务端菜单树构建授权快照。
 *
 * @param menus          AuthPermissionInfoRespVO.menus（本次响应的内存值，非 localStorage）
 * @param enabledModules 运行模块白名单；缺省 null 表示信号不可得（降级为菜单存在性判定）
 */
export function buildRouteAccessSnapshot(
  menus: MenuRouteNode[] | null | undefined,
  enabledModules: string[] | null = null
): RouteAccessSnapshot {
  const state: CollectState = { patterns: [], hasExternalMenu: false }
  collectMenuNodes(menus, ROOT_ROUTE, state)

  const menuPrefixes: string[] = []
  state.patterns.forEach((pattern) => {
    ancestorPrefixes(pattern).forEach((prefix) => {
      if (menuPrefixes.indexOf(prefix) === -1) {
        menuPrefixes.push(prefix)
      }
    })
  })

  return {
    menuPatterns: state.patterns,
    menuPrefixes,
    hasExternalMenu: state.hasExternalMenu,
    enabledModules: Array.isArray(enabledModules) ? enabledModules.slice() : null
  }
}

/** 空快照：装配失败 / 缓存失效 / 登录未完成时的默认状态（业务路由一律拒绝） */
export function emptyRouteAccessSnapshot(): RouteAccessSnapshot {
  return {
    menuPatterns: [],
    menuPrefixes: [],
    hasExternalMenu: false,
    enabledModules: null
  }
}

// ---------------------------------------------------------------------------
// 授权判定
// ---------------------------------------------------------------------------

function isAnchorAuthorized(anchor: string, snapshot: RouteAccessSnapshot): boolean {
  const normalizedAnchor = normalizeRoutePath(anchor)
  // ① 锚点自身就是服务端下发的菜单
  if (matchAnyPattern(snapshot.menuPatterns, normalizedAnchor)) {
    return true
  }
  // ② 锚点目录内存在任一服务端授权菜单（应对 activeMenu 与真实菜单层级不一致）
  return snapshot.menuPrefixes.indexOf(normalizedAnchor) !== -1
}

function matchStaticEntries(path: string): StaticRouteAccessEntry[] {
  const matched: StaticRouteAccessEntry[] = []
  STATIC_ROUTE_ACCESS.forEach((entry) => {
    if (matchPathPattern(entry.pattern, path)) {
      matched.push(entry)
    }
  })
  return matched
}

/**
 * 判定某站内路径在当前授权快照下是否可达。**命中即返回**，不回退更宽并集。
 *
 * 判定顺序：
 *   1. 公共应用外壳 → 放行（登录即可达，与业务授权无关）
 *   2. 外链落地路由 `/external-link/**` → 仅当服务端下发过外链菜单
 *   3. 运行模块白名单可用时：模块键不在白名单 → 拒绝（关闭模块命中即返回）
 *   4. 快照无任何授权菜单 → 拒绝（缓存失效 / 未装配 / 装配失败）
 *   5. 服务端菜单精确模式命中 → 放行
 *   6. 本地静态隐藏子页补登表命中 → 由其 anchors 决定（多入口取并集，不回退模块并集）
 *   7. 默认拒绝
 */
export function hasRouteAccess(route: string, snapshot?: RouteAccessSnapshot | null): boolean {
  const path = normalizeRoutePath(route)
  const current: RouteAccessSnapshot = snapshot ?? EMPTY_SNAPSHOT

  if (isPublicRoute(path)) {
    return true
  }
  if (path.startsWith(EXTERNAL_LINK_ROUTE_PREFIX)) {
    return current.hasExternalMenu === true
  }

  const enabledModules = current.enabledModules
  if (Array.isArray(enabledModules)) {
    const moduleKey = resolveModuleKey(path)
    if (moduleKey === null || enabledModules.indexOf(moduleKey) === -1) {
      return false
    }
  }

  if (current.menuPatterns.length === 0) {
    return false
  }
  if (matchAnyPattern(current.menuPatterns, path)) {
    return true
  }

  const staticEntries = matchStaticEntries(path)
  if (staticEntries.length > 0) {
    return staticEntries.some((entry) =>
      entry.anchors.some((anchor) => isAnchorAuthorized(anchor, current))
    )
  }

  return false
}

// ---------------------------------------------------------------------------
// 登录重定向目的地校验（防开放重定向）
// ---------------------------------------------------------------------------

const SCHEME_PATTERN = /^[a-zA-Z][a-zA-Z\d+\-.]*:/
const CONTROL_CHAR_PATTERN = /[\u0000-\u001F\u007F]/
const PARENT_DIR_PATTERN = /(^|\/)\.\.(\/|$)/

/** 是否为可信的站内目的地（只允许 `/` 开头的站内路径） */
function isSafeInternalTarget(value: unknown): boolean {
  if (typeof value !== 'string') {
    return false
  }
  const trimmed = value.trim()
  if (trimmed.length === 0) {
    return false
  }
  // 控制字符（含 \n / \r / \0）可用于响应头拆分与绕过
  if (CONTROL_CHAR_PATTERN.test(trimmed)) {
    return false
  }
  // 反斜杠在部分浏览器中等价于 `/`，可构造 `/\evil.com` 式绕过
  if (trimmed.indexOf('\\') !== -1) {
    return false
  }
  if (SCHEME_PATTERN.test(trimmed)) {
    return false
  }
  // 协议相对 URL
  if (trimmed.startsWith('//')) {
    return false
  }
  if (!trimmed.startsWith('/')) {
    return false
  }
  let decoded = trimmed
  try {
    decoded = decodeURIComponent(trimmed)
  } catch {
    decoded = trimmed
  }
  if (decoded.indexOf('\\') !== -1) {
    return false
  }
  if (SCHEME_PATTERN.test(decoded)) {
    return false
  }
  if (decoded.startsWith('//')) {
    return false
  }
  if (PARENT_DIR_PATTERN.test(decoded)) {
    return false
  }
  return true
}

/**
 * 消毒登录重定向目的地：只放行站内绝对路径，其余一律回退。
 *
 * @param raw      来自 `route.query.redirect` 的原始值（可能是数组 / 对象 / 非字符串）
 * @param fallback 回退值；若回退值自身不可信，则退回首页（防止调用方传入被污染的兜底值）
 */
export function sanitizeLoginRedirect(raw: unknown, fallback: string = HOME_ROUTE): string {
  const safeFallback = isSafeInternalTarget(fallback) ? (fallback as string) : HOME_ROUTE
  if (typeof raw !== 'string') {
    return safeFallback
  }
  const candidate = raw.trim()
  if (!isSafeInternalTarget(candidate)) {
    return safeFallback
  }
  return candidate
}

/**
 * 是否为 SSO 登录回调目的地。
 *
 * 登录 / 注册 / 社交登录三个入口原本统一用 `redirect.indexOf('sso') !== -1` 做子串匹配，
 * 任何**含** "sso" 的站内路径（如 `/system/sso-config`）都会被误判为 SSO 回调，
 * 进而触发整页跳转、绕过 SPA 路由与授权守卫。此处收紧为「可信站内路径 + 归一化后等于 /sso」。
 */
export function isSsoLoginRedirect(target: unknown): boolean {
  if (typeof target !== 'string' || !isSafeInternalTarget(target)) {
    return false
  }
  return normalizeRoutePath(target) === SSO_ROUTE
}

/**
 * 把站内路径拼上部署 base（`VITE_BASE_PATH`）。
 *
 * 整页跳转（`window.location.assign`）不经过 vue-router，因此必须自己带上 base；
 * 否则 `.env.test` 之类的子目录部署（`/admin-ui-vue3/`）会落到站点根而 404。
 * base 不可信（如 `.env.stage` 把 base 指向跨站静态域名）时不拼接，只返回站内路径。
 */
function joinBasePath(target: string, basePath: unknown): string {
  if (typeof basePath !== 'string') {
    return target
  }
  const base = basePath.trim().replace(/\/+$/, '')
  if (base.length === 0 || !isSafeInternalTarget(base)) {
    return target
  }
  return base + target
}

/** 登录成功后的落地决策 */
export interface PostAuthRedirect {
  /** 已消毒的站内目的地，可直接交给 `router.push({ path })` */
  target: string
  /** SSO 回调需要整页跳转时的站内 URL；非 SSO 场景为 null */
  fullPageUrl: string | null
}

/**
 * 解析登录 / 注册 / 社交登录成功后的落地目的地（卡片「调整」：校验登录重定向目的地）。
 *
 * 三个入口（LoginForm.vue / RegisterForm.vue / SocialLogin.vue）共用本函数，避免三份漂移实现。
 * 同时消除两个既有隐患：
 *   1. `window.location.href.replace('/login?redirect=', '')` —— 直接对地址栏做字符串裁剪，
 *      绕过消毒结果，且对 percent-encoding（守卫写入的 redirect 已 encodeURIComponent）不成立；
 *   2. `redirect || permissionStore.addRouters[0].path` —— 登录成功时动态路由尚未装配，
 *      `addRouters` 为空数组，该兜底一旦生效就是 TypeError（实际为死代码，仍予清除）。
 *
 * @param raw      原始 redirect（通常来自 `route.query.redirect` 或 URL 参数）
 * @param basePath 部署 base，调用方传 `import.meta.env.VITE_BASE_PATH`
 */
export function resolvePostAuthRedirect(raw: unknown, basePath?: unknown): PostAuthRedirect {
  const target = sanitizeLoginRedirect(raw, HOME_ROUTE)
  if (!isSsoLoginRedirect(target)) {
    return { target, fullPageUrl: null }
  }
  return { target, fullPageUrl: joinBasePath(target, basePath) }
}

// ---------------------------------------------------------------------------
// 全局前置守卫决策（纯函数，permission.ts 只做副作用编排）
// ---------------------------------------------------------------------------

export type GuardNavigation =
  | { type: 'allow' }
  | { type: 'allow-home' }
  | { type: 'bootstrap' }
  | { type: 'unauthorized' }
  | { type: 'login'; redirect: string }

export interface GuardNavigationInput {
  /** `to.path` */
  toPath: string
  /** `to.fullPath`（含 query / hash） */
  toFullPath: string
  /** `from.query.redirect`，仅在 bootstrap 决策中作为参考 */
  redirectQuery?: unknown
  /** 是否持有有效 accessToken */
  hasToken: boolean
  /** 授权快照是否已装配（user store isSetUser 且动态路由已 addRoute） */
  isUserResolved: boolean
  /** 当前会话授权快照 */
  snapshot?: RouteAccessSnapshot | null
}

/**
 * 计算全局前置守卫的导航决策。
 *
 * - 未持 token：**仅**登录前白名单（`PRE_AUTH_ROUTE_PATTERNS`）放行，其余一律重定向登录页
 *   （redirect 已消毒）。公共外壳（首页 / 个人中心 / 错误落点）不属于未认证可达面——
 *   放宽它等于把「未登录直达 /index」变成合法路径，与卡片验收②冲突。
 * - 持 token 访问登录页：回首页（`allow-home`）
 * - 持 token 但快照未装配：触发 bootstrap（拉取授权 + 装配动态路由）
 * - 持 token 且已装配：授权则放行，否则落未授权分支（守卫再按 `isNotFoundMatch` 分 403/404）
 */
export function resolveGuardNavigation(input: GuardNavigationInput): GuardNavigation {
  const toPath = normalizeRoutePath(input.toPath)
  const toFullPath =
    typeof input.toFullPath === 'string' && input.toFullPath.trim().length > 0
      ? input.toFullPath.trim()
      : toPath

  if (!input.hasToken) {
    // 未认证只放行登录前白名单；公共外壳（首页 / 个人中心 / 错误落点）**不属于**
    // 未认证可达面，沿用原 permission.ts 行为统一引导到登录页。
    if (isPreAuthRoute(toPath)) {
      return { type: 'allow' }
    }
    return { type: 'login', redirect: sanitizeLoginRedirect(toFullPath, HOME_ROUTE) }
  }

  if (toPath === LOGIN_ROUTE) {
    return { type: 'allow-home' }
  }
  if (!input.isUserResolved) {
    return { type: 'bootstrap' }
  }
  if (hasRouteAccess(toPath, input.snapshot)) {
    return { type: 'allow' }
  }
  return { type: 'unauthorized' }
}

export interface BootstrapTargetInput {
  /** 装配完成时的 `to.fullPath` */
  toFullPath: string
  /** `from.query.redirect` 原始值 */
  redirectQuery: unknown
  /** 装配完成后的授权快照 */
  snapshot?: RouteAccessSnapshot | null
}

export interface BootstrapTarget {
  /** 装配后应落地的目的地（已消毒 + 已授权校验） */
  target: string
  /**
   * 为 true 时守卫应以 `next({ ...to, replace: true })` 保留原始定位（含 params / query）；
   * 为 false 时应以 `next({ path: target, replace: true })` 跳转到新目的地。
   */
  preserveToLocation: boolean
  /**
   * 落地是否已授权。为 false 时守卫可结合 `isNotFoundMatch(to.matched)`
   * 在 403（已注册但未授权）与 404（路由不存在）之间选择正确语义。
   */
  authorized: boolean
}

function fallbackFullPath(value: unknown): string {
  if (typeof value === 'string' && value.trim().length > 0) {
    return value.trim()
  }
  return HOME_ROUTE
}

/**
 * 装配（bootstrap）完成后的目的地决策 —— 覆盖「登录失败 / 直达关闭模块」两类场景：
 *
 * - redirect 可信且已授权 → 落 redirect
 * - redirect 可信但未授权（关闭模块 / 未注册）→ 落 403，**不进入半初始化页面**
 * - redirect 不可信（跨站 / 数组污染 / scheme）→ 忽略之，回到 to.fullPath
 * - to.fullPath 自身未授权 → 落 403
 */
export function resolveBootstrapTarget(input: BootstrapTargetInput): BootstrapTarget {
  const toFullPath = fallbackFullPath(input.toFullPath)
  const rawRedirect = typeof input.redirectQuery === 'string' ? input.redirectQuery.trim() : ''

  if (rawRedirect.length > 0 && isSafeInternalTarget(rawRedirect)) {
    const candidate = sanitizeLoginRedirect(rawRedirect, HOME_ROUTE)
    if (hasRouteAccess(normalizeRoutePath(candidate), input.snapshot)) {
      return { target: candidate, preserveToLocation: false, authorized: true }
    }
    return { target: UNAUTHORIZED_ROUTE, preserveToLocation: false, authorized: false }
  }

  if (hasRouteAccess(normalizeRoutePath(toFullPath), input.snapshot)) {
    return { target: toFullPath, preserveToLocation: true, authorized: true }
  }
  return { target: UNAUTHORIZED_ROUTE, preserveToLocation: false, authorized: false }
}

// ---------------------------------------------------------------------------
// 404 / 403 语义区分
// ---------------------------------------------------------------------------

/** vue-router 兜底记录的路径特征：remaining.ts 的 `/:pathMatch(.*)*` 与动态追加的 `/:path(.*)*` */
const CATCH_ALL_RECORD_PATTERN = /:(pathMatch|path)\(/

/** 路由匹配记录的最小结构投影（避免依赖 vue-router 运行时类型） */
export interface MatchedRouteRecordLike {
  path?: string
  name?: unknown
}

/**
 * 判断 vue-router 的匹配结果是否「只命中 404 兜底记录」。
 *
 * 用途：未授权时区分两种语义 —— 路径根本不存在（保持既有 404 UX），
 * 与路径已在路由表注册但当前授权快照不允许（403）。两者都是拒绝，不构成绕过通道。
 */
export function isNotFoundMatch(matched: MatchedRouteRecordLike[] | null | undefined): boolean {
  if (!Array.isArray(matched) || matched.length === 0) {
    return true
  }
  return matched.every((record) => {
    if (!record || typeof record.path !== 'string') {
      return true
    }
    return CATCH_ALL_RECORD_PATTERN.test(record.path)
  })
}
