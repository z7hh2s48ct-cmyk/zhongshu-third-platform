/**
 * ZS-CLIENT-002.A/B：移动端「服务端授权导航」路由权限注册表与直达守卫判定。
 *
 * 设计原则（对齐 docs/05 ZS-CLIENT-002.A/B 与 B04 Wave3 计划 §5 交付 1/2/4/5）：
 * - 客户端只做「服务端下发权限标识」的集合成员判断，不在前端重算业务权限；
 * - 注册表直接消费工作台菜单声明 `pages/index/menu.json`（url→permission，与后端 @PreAuthorize 对齐），
 *   与工作台菜单过滤（`pages/index/index.ts` 的 hasMenuAccess）同源同语义，杜绝前端另立一套粗粒度映射
 *   而与服务端授权分歧（如把「我的流程」bpm:process-instance:query 误当 bpm:task:query 拒绝）；
 * - 语义对齐 hasMenuAccess：菜单项无 permission → 登录即放行（如 AI 会话 / IM）；有 permission(s) → 任一命中即放行；
 * - 计划 §5 交付 2「未注册/未授权页面不可进入」+ 交付 4「关闭模块提供不可用落点」：
 *   公共应用外壳（工作台 / 消息 / 我的 / pages-core 登录·错误·个人中心）显式放行；业务页面按注册表授权；
 *   既非公共、又未在注册表登记（含子页继承兜底）的页面 → 拒绝（由 interceptor reLaunch 到 403 落点）；
 * - 子页面（详情 / 表单 / 办理 / 收银台等，menu.json 只登记菜单入口页）继承与显式登记：
 *   ① 「最近声明目录」前缀继承（CLIENT-002.B r3-P2-1 修复）：声明目录 = 注册项 URL 的直接父目录，
 *     子页按最长命中声明目录的权限判定——命中即按其权限判定，未通过则拒绝；不再上溯更远祖先，
 *     杜绝后代言权限上溢兄弟目录（如 hrm:employee:config:query 曾泄漏给 /pages-hrm/employee/detail），
 *     也不回退更宽模块并集（否则持无关权限者可经模块兜底进入受控子页）；
 *   ② 真正跨包 / 跨目录共享的子页（如 BPM 审批详情：主包审批入口 /pages/bpm/ 与分包管理入口 /pages-bpm/…/manager 复用）
 *     在 EXTRA_ROUTE_ACCESS 显式登记其权限并集（含 CLIENT-002.B 差集补登，防「最近声明目录」语义误伤既有导航）；
 *   ③ 皆未命中 → 拒绝（未注册 / 未启用模块）；单条数据的细粒度授权由服务端 API 兜底；
 * - 模块门（CLIENT-002.B r2-P2-2 修复）：服务端下发启用模块清单（enabledModules）后，可归属模块的页若模块未启用
 *   → 直接拒绝（关闭模块提供不可用落点，避免进入后撞服务端 501）；清单未下发（null/缺省）跳过模块门（旧后端兼容）；
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

/**
 * 已知业务模块白名单（对齐服务端 ModuleCatalog.ALL_MODULES）：用于把页面路径归属到业务模块，
 * 配合服务端下发的 enabledModules 关闭停用模块的导航落点（CLIENT-002.B r2-P2-2）。
 * 不在白名单内的包前缀（如 pages-hrm / pages-fms）无法归属模块 → 不受模块门约束。
 */
const KNOWN_MODULES = [
  'system', 'infra', 'bpm', 'mp', 'mall', 'erp', 'wms', 'pms', 'crm', 'mes', 'im', 'report', 'pay', 'ai', 'iot',
]

/**
 * 由移动路由归属业务模块（CLIENT-002.B 模块门）：/pages-ai/** → ai；/pages/<m>/** → <m>（m 须在已知模块内）；
 * 其余（pages-core / 非模块分包 / 未知包）→ null（不可归属，跳过模块门）。
 */
function resolvePageModule(path: string): string | null {
  const parts = path.split('/').filter(Boolean)
  const first = parts[0]
  if (!first) {
    return null
  }
  const module = first === 'pages' ? parts[1] : first.startsWith('pages-') ? first.slice('pages-'.length) : null
  return module && KNOWN_MODULES.includes(module) ? module : null
}

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
 * - BPM 审批详情 / 发起（跨包共享子页）：见上 BPM_SHARED_APPROVAL_PERMISSIONS 说明——显式登记其声明目录，
 *   使 detail/audit、detail/reassign、create/form 等派生子页经最近声明目录前缀继承到审批流并集；
 * - CLIENT-002.B 差集补登（「最近声明目录」语义下 21 页面失去旧祖先继承，按 OLD 权限逐条补齐防回归）：
 *   CRM 跟进表单（14 权限并集）/ FMS 凭证详情（三联）/ HRM 门户 2 页 / IM 通讯录子页（8 页组）/
 *   IM 人脸管理子页（3 页组）/ IoT OTA 固件（4 页组）/ Mall 砍价助力 / MES 安灯配置；
 * - 部门共享表单（CLIENT-002.B r3-P2-2 修复）：/pages-system/dept/form 是「新增 / 编辑」共用表单页，
 *   按动作级登记（create / update 任一），不再经 /pages-system/dept/ 无条件要求 query——
 *   仅持 query 者走 dept/detail 查看，按钮守卫不会跳 form。
 */
const EXTRA_ROUTE_ACCESS: Array<{ path: string, permissions: string[] }> = [
  { path: '/pages/contact/index', permissions: ['system:user:list'] },
  { path: '/pages-pay/cashier/index', permissions: [] },
  { path: '/pages-bpm/processInstance/detail/index', permissions: BPM_SHARED_APPROVAL_PERMISSIONS },
  { path: '/pages-bpm/processInstance/create/index', permissions: BPM_SHARED_APPROVAL_PERMISSIONS },
  { path: '/pages-crm/followup/form/index', permissions: ['crm:clue:query', 'crm:customer:query', 'crm:contact:query', 'crm:business:query', 'crm:contract:query', 'crm:receivable:query', 'crm:receivable-plan:query', 'crm:product:query', 'crm:product-category:query', 'crm:business-status:query', 'crm:customer-limit-config:query', 'crm:customer-pool-config:query', 'crm:contract-config:query', 'crm:performance-config:query'] },
  { path: '/pages-fms/voucher/detail/index', permissions: ['fms:voucher:create', 'fms:voucher:query', 'fms:voucher:statistics:query'] },
  { path: '/pages-hrm/portal/attendance/leave/form/index', permissions: ['hrm:portal:query'] },
  { path: '/pages-hrm/portal/opening-guide/index', permissions: ['hrm:portal:query'] },
  { path: '/pages-im/home/contact/index', permissions: [] },
  { path: '/pages-im/manager/face/item/index', permissions: [] },
  { path: '/pages-iot/ota/record/index', permissions: ['iot:ota-firmware:query'] },
  { path: '/pages-iot/ota/task/index', permissions: ['iot:ota-firmware:query'] },
  { path: '/pages-mall/promotion/bargain/help/index', permissions: ['promotion:bargain-activity:query', 'promotion:bargain-record:query'] },
  { path: '/pages-mes/pro/andon/config/index', permissions: ['mes:pro-andon-record:query'] },
  { path: '/pages-system/dept/form/index', permissions: ['system:dept:create', 'system:dept:update'] },
]

/** 归一化移动路由：剥离 query、补前导斜杠（与 tabbar normalizeRoutePath 同语义） */
export function normalizeRoute(route: string): string {
  const path = route.split('?')[0]
  return path.startsWith('/') ? path : `/${path}`
}

/**
 * 由页面路径推导其「声明目录」前缀（仅直接父目录，CLIENT-002.B r3-P2-1），供未登记子页按最近声明目录继承。
 * 仅直接父目录 = 注册项声明的权限只覆盖其直属子页（如 /pages-system/user/index → 声明目录 /pages-system/user/），
 * 不再逐级上溯祖先（旧语义会把兄弟子目录的权限上溢到整个模块包）。
 * 排除过宽的 `/pages/`（主包根，会覆盖全部主包页）与 `/`（根，会使默认拒绝失效）。
 * 例：/pages-bpm/processInstance/detail/index → ['/pages-bpm/processInstance/detail/']
 */
function nearestParentPrefix(path: string): string[] {
  const parts = path.split('/').filter(Boolean)
  if (parts.length < 2) {
    return []
  }
  const prefix = `/${parts.slice(0, parts.length - 1).join('/')}/`
  return prefix === '/pages/' || prefix === '/' ? [] : [prefix]
}

/** 精确路由 → 所需权限（并集）；空数组 = 已登记但无需权限（登录即放行，如 AI 会话 / IM / 收银台） */
const exactRegistry = new Map<string, string[]>()
/** 声明目录前缀 → 所需权限（并集），供 menu.json 未逐条登记的子页按最近声明目录继承（含登录即用目录） */
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
  for (const prefix of nearestParentPrefix(path)) {
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

/** 最长声明目录前缀匹配：为未逐条登记的子页继承其最近声明目录的权限（含登录即用目录） */
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
 * 五级判定（计划 §5 交付 2「未注册/未授权页面不可进入」+ 交付 4「关闭模块提供不可用落点」）：
 * ① 公共应用外壳放行；② 模块门：页面可归属模块且服务端启用清单未含该模块 → 拒绝（CLIENT-002.B；清单未下发跳过）；
 * ③ 精确命中（菜单入口 / 显式补登的跨包共享子页）强制其声明权限；
 * ④ 子页按最近声明目录前缀继承——命中即按其权限判定，未通过则拒绝（保留具体声明目录的拒绝，不回退更宽并集）；
 * ⑤ 皆未命中 → 拒绝。空权限=登录即用、否则任一命中即放行。
 *
 * @param route 目标路由（可带 query，内部归一化）
 * @param permissions 服务端下发的权限标识集合（useUserStore().permissions）
 * @param enabledModules 服务端下发的启用模块清单（useUserStore().enabledModules）；null/缺省 = 旧后端未下发，跳过模块门
 * @returns true=放行；false=拒绝（拦截到 UNAUTHORIZED_PAGE）
 */
export function hasRouteAccess(route: string, permissions: string[], enabledModules?: string[] | null): boolean {
  const path = normalizeRoute(route)
  const held = permissions ?? []
  // 1) 公共应用外壳（含根路径）：登录即放行
  if (path === '/' || PUBLIC_ROUTE_PREFIXES.some(prefix => path.startsWith(prefix))) {
    return true
  }
  // 2) 模块门（CLIENT-002.B r2-P2-2）：可归属模块的页在停用模块时直接拒绝——关闭模块提供不可用落点，
  //    避免进入后撞服务端 501；清单未下发（null/缺省）跳过（旧后端兼容，行为与注册表一致）
  const pageModule = resolvePageModule(path)
  if (pageModule !== null && enabledModules != null && !enabledModules.includes(pageModule)) {
    return false
  }
  // 3) 精确命中（菜单入口 / 显式补登）：强制其声明的具体权限——未授权即拒（保「未注册/未授权不可进入」，且 BPM 各页权限不被压平）
  const exact = exactRegistry.get(path)
  if (exact !== undefined) {
    return allowByPermissions(exact, held)
  }
  // 4) 子页按最近声明目录前缀继承（详情 / 表单 / 办理等 menu.json 未逐条登记者）：取最长匹配声明目录的权限判定，
  //    命中即返回其判定——通过则放行、未通过则拒绝（保留具体声明目录的拒绝，不再回退到更宽的模块并集，
  //    否则持无关权限者会经模块兜底进入受控子页，如仅持 system:dept:query 却进入 /pages-system/user/detail——codex r2-P2-1）；
  //    登录即用目录（如 AI 会话 / IM）所需权限为空，任意登录用户可达
  const inherited = matchLongestPrefix(path)
  if (inherited !== undefined) {
    return allowByPermissions(inherited, held)
  }
  // 5) 既非公共、又未登记（含未注册 / 未启用模块）→ 拒绝（交付 2「未注册页面不可进入」+ 交付 4「关闭模块落点」）
  return false
}
