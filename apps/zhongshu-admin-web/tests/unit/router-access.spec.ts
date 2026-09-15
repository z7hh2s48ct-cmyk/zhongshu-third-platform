import { describe, expect, it } from 'vitest'
import {
  buildRouteAccessSnapshot,
  HOME_ROUTE,
  hasRouteAccess,
  isNotFoundMatch,
  isPreAuthRoute,
  isPublicRoute,
  isSsoLoginRedirect,
  LOGIN_ROUTE,
  NOT_FOUND_ROUTE,
  normalizeRoutePath,
  resolveBootstrapTarget,
  resolveGuardNavigation,
  resolveModuleKey,
  resolvePostAuthRedirect,
  sanitizeLoginRedirect,
  SSO_ROUTE,
  STATIC_ROUTE_ACCESS,
  UNAUTHORIZED_ROUTE,
} from '@/router/access'
import type { MenuRouteNode, RouteAccessSnapshot } from '@/router/access'

// ZS-CLIENT-001.A：Web 管理端「技术账号导航 / 缓存 / 关闭模块守卫」单元测试。
//
// 被测：@/router/access —— 不依赖 Vue 运行时的纯 TS 授权判定核心。
//
// 与移动端姊妹项 ZS-CLIENT-002.A（apps/zhongshu-miniapp/src/router/access.ts）语义对齐：
//   - 客户端只做「服务端下发数据」的集合成员判断，不在前端重算业务权限；
//   - 单一真相源：移动端是 pages/index/menu.json，Web 端是服务端 get-permission-info 下发的菜单树
//     （后端已按 运行模块白名单 ZS-CFG-003.A × 套餐/角色权限交集 ZS-CFG-003.B 过滤）；
//   - 未注册 / 未授权 → 默认拒绝（CLIENT-002.A codex r0-P1 教训）；
//   - 本地静态声明的隐藏子页显式补登锚点，避免默认拒绝误伤合法页（r1-P1 教训）；
//   - 命中即返回，不回退到更宽的模块级并集（r2-P2 教训）。
//
// 关键差异：Web 侧 AuthPermissionInfoRespVO.MenuVO **不携带 permission 字段**，
// 因此授权判定单位是「服务端是否下发该菜单路径」而非「是否持有权限标识」；
// 动作级 / 字段级授权消费属 ZS-CLIENT-001.B（B08，前置 ZS-PERM-003），本批不做。

/** 构造一份「众墅技术账号」典型授权菜单树：system + infra 已启用，ai/bpm/crm/fms/iot/mes 未启用 */
function techAccountMenus(): MenuRouteNode[] {
  return [
    {
      path: '/system',
      children: [
        { path: '/system/user' },
        { path: '/system/role' },
        { path: '/system/dict' },
        { path: '/system/tenant' },
      ],
    },
    {
      path: '/infra',
      children: [{ path: '/infra/codegen' }, { path: '/infra/job' }],
    },
  ]
}

/** 构造一份「全模块启用」授权菜单树（用于验证合法页不被误伤） */
function fullModuleMenus(): MenuRouteNode[] {
  return [
    { path: '/system/user' },
    { path: '/system/dict' },
    { path: '/infra/codegen' },
    { path: '/infra/job' },
    { path: '/bpm/task/my' },
    { path: '/bpm/manager/model' },
    { path: '/bpm/manager/form' },
    { path: '/bpm/oa/leave' },
    { path: '/bpm/process-instance' },
    { path: '/crm/customer' },
    { path: '/crm/clue' },
    { path: '/mall/product/spu' },
    { path: '/mall/product/property' },
    { path: '/mall/trade/order' },
    { path: '/mall/trade/after-sale' },
    { path: '/mall/promotion/diy-template/diy-template' },
    { path: '/member/user' },
    { path: '/pay/cashier-order' },
    { path: '/hrm/portal/home' },
    { path: '/hrm/employee/list' },
    { path: '/hrm/recruit/post' },
    { path: '/ai/knowledge' },
    { path: '/ai/image' },
    { path: '/ai/console/workflow' },
    { path: '/iot/device/device' },
    { path: '/iot/device/product' },
    { path: '/iot/operation/ota/firmware' },
    { path: '/mes/wm/warehouse' },
    { path: '/mes/pro/task' },
    { path: '/fms/config/auxiliary' },
    { path: '/im/home' },
  ]
}

const techSnapshot = () => buildRouteAccessSnapshot(techAccountMenus())
const fullSnapshot = () => buildRouteAccessSnapshot(fullModuleMenus())
const emptySnapshot = () => buildRouteAccessSnapshot(null)

describe('normalizeRoutePath：Web 路由归一化（去 query/hash、补前导斜杠）', () => {
  it('剥离 query 与 hash 并补前导斜杠', () => {
    expect(normalizeRoutePath('system/user?page=2')).toBe('/system/user')
    expect(normalizeRoutePath('/system/user?page=2#sec')).toBe('/system/user')
    expect(normalizeRoutePath('/system/user#sec')).toBe('/system/user')
  })
  it('根路径与空串归一化为 /', () => {
    expect(normalizeRoutePath('/')).toBe('/')
    expect(normalizeRoutePath('')).toBe('/')
  })
  it('去除重复斜杠与尾部斜杠，避免前缀判定被绕过', () => {
    expect(normalizeRoutePath('//system///user/')).toBe('/system/user')
  })
})

describe('resolveModuleKey：模块键推导（关闭模块守卫的判定单位）', () => {
  it('取首段作为模块键', () => {
    expect(resolveModuleKey('/crm/customer/detail/1')).toBe('crm')
    expect(resolveModuleKey('/system/user')).toBe('system')
    expect(resolveModuleKey('/mes/wm/warehouse/location')).toBe('mes')
  })
  it('根路径 / 空串无模块键', () => {
    expect(resolveModuleKey('/')).toBeNull()
    expect(resolveModuleKey('')).toBeNull()
  })
})

describe('buildRouteAccessSnapshot：服务端菜单树 → 授权注册表（单一真相源）', () => {
  it('绝对路径菜单直接登记为授权路径模式', () => {
    const s = buildRouteAccessSnapshot([{ path: '/system/user' }])
    expect(hasRouteAccess('/system/user', s)).toBe(true)
  })
  it('相对子路径按父路径解析（对齐 vue-router 嵌套路由解析语义）', () => {
    const s = buildRouteAccessSnapshot([{ path: '/system', children: [{ path: 'post' }] }])
    expect(hasRouteAccess('/system/post', s)).toBe(true)
    expect(hasRouteAccess('/post', s)).toBe(false)
  })
  it('绝对子路径直接采用（vue-router 语义，而非 pathResolve 的无条件拼接）', () => {
    const s = buildRouteAccessSnapshot([
      { path: '/system', children: [{ path: '/system/user' }, { path: '/infra/codegen' }] },
    ])
    expect(hasRouteAccess('/system/user', s)).toBe(true)
    // 子节点以 / 开头时 vue-router 视为绝对路径，不会被拼成 /system/infra/codegen
    expect(hasRouteAccess('/infra/codegen', s)).toBe(true)
    expect(hasRouteAccess('/system/infra/codegen', s)).toBe(false)
  })
  it('多层嵌套逐层累积解析', () => {
    const s = buildRouteAccessSnapshot([
      { path: '/mall', children: [{ path: 'trade', children: [{ path: 'order' }] }] },
    ])
    expect(hasRouteAccess('/mall/trade/order', s)).toBe(true)
  })
  it('菜单路径可携带 :param 模式，实际访问值按通配段匹配', () => {
    const s = buildRouteAccessSnapshot([{ path: '/report/detail/:id' }])
    expect(hasRouteAccess('/report/detail/9527', s)).toBe(true)
    expect(hasRouteAccess('/report/detail', s)).toBe(false)
  })
  it('菜单路径可携带正则约束参数，约束不参与前端判定（只做集合成员判断）', () => {
    const s = buildRouteAccessSnapshot([{ path: '/mall/product/spu/edit/:id(\\d+)' }])
    expect(hasRouteAccess('/mall/product/spu/edit/12', s)).toBe(true)
  })
  it('外链菜单不登记为站内路径，但授权 /external-link 落点', () => {
    const s = buildRouteAccessSnapshot([{ path: 'https://doc.example.com/guide' }])
    expect(hasRouteAccess('https://doc.example.com/guide', s)).toBe(false)
    expect(hasRouteAccess('/external-link/1024', s)).toBe(true)
  })
  it('无外链菜单时 /external-link 落点默认拒绝', () => {
    expect(hasRouteAccess('/external-link/1024', techSnapshot())).toBe(false)
  })
  it('menus 为 null / 空数组 → 空快照（业务路由一律拒绝）', () => {
    expect(buildRouteAccessSnapshot(null).menuPatterns).toHaveLength(0)
    expect(buildRouteAccessSnapshot([]).menuPatterns).toHaveLength(0)
    expect(buildRouteAccessSnapshot(undefined).menuPatterns).toHaveLength(0)
  })
  it('默认不携带模块白名单信号（enabledModules=null，降级为菜单存在性判定）', () => {
    expect(techSnapshot().enabledModules).toBeNull()
  })
  it('显式传入模块白名单时保留原样（供后端接线后精确判定）', () => {
    expect(buildRouteAccessSnapshot(techAccountMenus(), ['system', 'infra']).enabledModules).toEqual([
      'system',
      'infra',
    ])
  })
})

describe('isPublicRoute / isPreAuthRoute：应用外壳与登录前白名单', () => {
  it('登录前白名单沿用 permission.ts 原有集合（不改既有可达性）', () => {
    for (const p of ['/login', '/social-login', '/auth-redirect', '/bind', '/register', '/oauthLogin/gitee']) {
      expect(isPreAuthRoute(p)).toBe(true)
    }
    expect(isPreAuthRoute('/system/user')).toBe(false)
  })
  it('登录前白名单不做前缀通配（/oauthLogin/* 不存在对应路由，不得放宽未认证可达面）', () => {
    expect(isPreAuthRoute('/oauthLogin/evil')).toBe(false)
    expect(isPreAuthRoute('/logina')).toBe(false)
    expect(isPreAuthRoute('/bindx')).toBe(false)
  })
  it('公共外壳：首页 / 错误落点 / 个人中心 / redirect 助手，空快照下仍放行', () => {
    const s = emptySnapshot()
    for (const p of [
      '/',
      '/index',
      HOME_ROUTE,
      LOGIN_ROUTE,
      UNAUTHORIZED_ROUTE,
      '/404',
      '/500',
      '/sso',
      '/user/profile',
      '/user/notify-message',
      '/redirect/system/user',
    ]) {
      expect(isPublicRoute(p)).toBe(true)
      expect(hasRouteAccess(p, s)).toBe(true)
    }
  })
  it('403 落点自身放行，杜绝未授权重定向自环', () => {
    expect(hasRouteAccess(UNAUTHORIZED_ROUTE, emptySnapshot())).toBe(true)
  })
  it('公共外壳不得被前缀伪装绕过（/loginx、/index-evil 不放行）', () => {
    expect(isPublicRoute('/loginx')).toBe(false)
    expect(isPublicRoute('/index-evil')).toBe(false)
    expect(isPublicRoute('/4033')).toBe(false)
    expect(isPublicRoute('/user/profile-extra')).toBe(false)
  })
})

describe('验收①正向：刷新 / 直达已授权页面可用', () => {
  it('直达服务端已下发的菜单页放行', () => {
    expect(hasRouteAccess('/system/user', techSnapshot())).toBe(true)
    expect(hasRouteAccess('/infra/codegen', techSnapshot())).toBe(true)
  })
  it('刷新（带 query / hash）后仍放行', () => {
    expect(hasRouteAccess('/system/user?pageNo=2&pageSize=20', techSnapshot())).toBe(true)
    expect(hasRouteAccess('/system/role#tab', techSnapshot())).toBe(true)
  })
  it('目录节点自身（有子菜单）放行，避免面包屑 / 重定向落点被误伤', () => {
    expect(hasRouteAccess('/system', techSnapshot())).toBe(true)
  })
})

describe('验收②反向：关闭模块不可进入', () => {
  it('服务端未下发 crm / bpm / ai 菜单 → 模块内页面一律拒绝', () => {
    const s = techSnapshot()
    for (const p of ['/crm/customer', '/bpm/task/my', '/ai/knowledge', '/mes/wm/warehouse', '/iot/device/device']) {
      expect(hasRouteAccess(p, s)).toBe(false)
    }
  })
  it('关闭模块的本地静态隐藏子页同样拒绝（静态路由表不成为绕过通道）', () => {
    const s = techSnapshot()
    for (const p of [
      '/crm/customer/detail/1',
      '/bpm/process-instance/detail',
      '/ai/console/workflow/create',
      '/mes/wm/warehouse/location',
      '/iot/device/detail/5',
      '/fms/auxiliary/type/item/7',
    ]) {
      expect(hasRouteAccess(p, s)).toBe(false)
    }
  })
  it('模块白名单显式可用时：白名单外模块即使菜单被伪造下发也拒绝（命中即返回）', () => {
    const forged = buildRouteAccessSnapshot([{ path: '/crm/customer' }], ['system', 'infra'])
    expect(hasRouteAccess('/crm/customer', forged)).toBe(false)
    expect(
      hasRouteAccess('/system/user', buildRouteAccessSnapshot(techAccountMenus(), ['system', 'infra'])),
    ).toBe(true)
  })
  it('模块白名单缺失（null）时降级为菜单存在性判定，且放行已下发模块', () => {
    const s = buildRouteAccessSnapshot([{ path: '/crm/customer' }], null)
    expect(s.enabledModules).toBeNull()
    expect(hasRouteAccess('/crm/customer', s)).toBe(true)
  })
})

describe('验收②反向：失效缓存 / 伪造前端角色不能操作', () => {
  it('授权快照为空（缓存被清 / 未装配 / getInfo 失败）→ 业务路由拒绝，外壳仍可达', () => {
    const s = emptySnapshot()
    expect(hasRouteAccess('/system/user', s)).toBe(false)
    expect(hasRouteAccess('/crm/customer/detail/1', s)).toBe(false)
    expect(hasRouteAccess('/index', s)).toBe(true)
    expect(hasRouteAccess('/login', s)).toBe(true)
  })
  it('快照是唯一授权输入：重建快照后旧授权立即失效（篡改本地缓存无法延续旧权限）', () => {
    const stale = techSnapshot()
    expect(hasRouteAccess('/system/user', stale)).toBe(true)
    const rebuilt = buildRouteAccessSnapshot([])
    expect(hasRouteAccess('/system/user', rebuilt)).toBe(false)
  })
  it('持无关模块授权的主体不得经静态子页进入受控模块（不回退更宽模块并集）', () => {
    // 服务端只下发 /system/* 与 /infra/*；/mall/product/spu/detail/1 的锚点是 /mall/product/spu，未授权 → 拒绝
    expect(hasRouteAccess('/mall/product/spu/detail/1', techSnapshot())).toBe(false)
  })
})

describe('验收②反向：未注册路由默认拒绝 ↔ 合法页不被误伤（成对断言）', () => {
  it('未注册 / 未知路由默认拒绝（CLIENT-002.A codex r0-P1 回归）', () => {
    const s = fullSnapshot()
    for (const p of [
      '/totally/unknown',
      '/system/user/detail/1', // 既非服务端菜单，也未在静态表补登
      '/system/userx',
      '/crm/customer/detail', // 缺 :id 段，模式不匹配
      '/admin/secret',
    ]) {
      expect(hasRouteAccess(p, s)).toBe(false)
    }
  })
  it('静态表补登的合法隐藏子页在其锚点被授权时放行（CLIENT-002.A codex r1-P1 回归）', () => {
    const s = fullSnapshot()
    const legit = [
      '/dict/type/data/user_type',
      '/codegen/edit',
      '/job/job-log',
      '/fms/auxiliary/type/item/3',
      '/bpm/manager/form/edit',
      '/bpm/manager/definition',
      '/bpm/process-instance/detail',
      '/bpm/process-instance/report',
      '/bpm/oa/leave/create',
      '/bpm/oa/leave/detail',
      '/bpm/manager/model/create',
      '/bpm/manager/model/simple/10',
      '/mall/product/spu/add',
      '/mall/product/spu/edit/12',
      '/mall/product/spu/detail/12',
      '/mall/product/property/value/8',
      '/mall/trade/order/detail/99',
      '/mall/trade/after-sale/detail/99',
      '/member/user/detail/5',
      '/pay/cashier',
      '/diy/template/decorate/7',
      '/diy/page/decorate/7',
      '/crm/clue/detail/1',
      '/crm/customer/detail/1',
      '/hrm/portal/opening-guide',
      '/hrm/recruit/post/detail/2',
      '/hrm/employee/detail/3',
      '/ai/image/square',
      '/ai/knowledge/document',
      '/ai/knowledge/document/create',
      '/ai/knowledge/retrieval',
      '/ai/knowledge/segment',
      '/ai/console/workflow/create',
      '/ai/console/workflow/simple/6',
      '/iot/product/product/detail/4',
      '/iot/device/detail/4',
      '/iot/ota/operation/firmware/detail/4',
      '/mes/wm/warehouse/location',
      '/mes/wm/warehouse/area',
      '/mes/pro/task/gantt-edit',
      '/im/home/conversation',
      '/im/home/contact',
    ]
    for (const p of legit) {
      expect(hasRouteAccess(p, s), `合法页被误伤: ${p}`).toBe(true)
    }
  })
  it('静态表条目自身规范：pattern 以 / 开头且 anchors 非空（空 anchors 会造成失效缓存下放行）', () => {
    expect(STATIC_ROUTE_ACCESS.length).toBeGreaterThanOrEqual(40)
    for (const entry of STATIC_ROUTE_ACCESS) {
      expect(entry.pattern.startsWith('/'), `pattern 必须以 / 开头: ${entry.pattern}`).toBe(true)
      expect(entry.anchors.length, `anchors 不得为空: ${entry.pattern}`).toBeGreaterThan(0)
      for (const a of entry.anchors) {
        expect(a.startsWith('/'), `anchor 必须以 / 开头: ${a}`).toBe(true)
      }
    }
  })
  it('锚点未授权时静态子页拒绝，且不因同模块其它菜单被授权而放行（r2-P2 回归）', () => {
    // 只下发 /crm/clue；/crm/customer/detail/1 的锚点是 /crm/customer → 拒绝（不得回退到 /crm 模块并集）
    const s = buildRouteAccessSnapshot([{ path: '/crm/clue' }])
    expect(hasRouteAccess('/crm/clue/detail/1', s)).toBe(true)
    expect(hasRouteAccess('/crm/customer/detail/1', s)).toBe(false)
  })
  it('锚点可通过「锚点目录内存在任一授权菜单」命中（activeMenu 与真实菜单层级不一致时不误伤）', () => {
    const s = buildRouteAccessSnapshot([{ path: '/mall/promotion/diy-template/diy-template' }])
    expect(hasRouteAccess('/diy/template/decorate/7', s)).toBe(true)
  })
})

describe('验收③反向：撤权后旧页签 / 返回缓存不展示旧敏感数据（判定层）', () => {
  it('撤权后重建快照：原先可达的页面立即变为拒绝', () => {
    const before = buildRouteAccessSnapshot([
      { path: '/system/user' },
      { path: '/crm/customer' },
      { path: '/mall/trade/order' },
    ])
    expect(hasRouteAccess('/crm/customer', before)).toBe(true)
    expect(hasRouteAccess('/mall/trade/order/detail/1', before)).toBe(true)

    // 服务端撤权后重新下发（仅剩 /system/user）
    const after = buildRouteAccessSnapshot([{ path: '/system/user' }])
    expect(hasRouteAccess('/crm/customer', after)).toBe(false)
    expect(hasRouteAccess('/mall/trade/order/detail/1', after)).toBe(false)
    expect(hasRouteAccess('/system/user', after)).toBe(true)
  })
  it('旧快照不得被复用于新判定（前端只负责清理与不展示；接口独立拒绝属服务端职责）', () => {
    const stale: RouteAccessSnapshot = buildRouteAccessSnapshot([{ path: '/crm/customer' }])
    const fresh: RouteAccessSnapshot = buildRouteAccessSnapshot([])
    expect(hasRouteAccess('/crm/customer', stale)).toBe(true)
    expect(hasRouteAccess('/crm/customer', fresh)).toBe(false)
  })
})

describe('sanitizeLoginRedirect：登录重定向目的地校验（防开放重定向）', () => {
  it('站内绝对路径原样保留（含 query / hash）', () => {
    expect(sanitizeLoginRedirect('/index')).toBe('/index')
    expect(sanitizeLoginRedirect('/system/user?pageNo=2')).toBe('/system/user?pageNo=2')
    expect(sanitizeLoginRedirect('/crm/customer/detail/1#tab')).toBe('/crm/customer/detail/1#tab')
    expect(sanitizeLoginRedirect('/')).toBe('/')
  })
  it('跨站绝对地址拒绝并回退', () => {
    expect(sanitizeLoginRedirect('https://evil.example.com/x')).toBe(HOME_ROUTE)
    expect(sanitizeLoginRedirect('http://evil.example.com')).toBe(HOME_ROUTE)
    expect(sanitizeLoginRedirect('//evil.example.com/x')).toBe(HOME_ROUTE)
    expect(sanitizeLoginRedirect('///evil.example.com')).toBe(HOME_ROUTE)
  })
  it('反斜杠 / 编码绕过拒绝并回退', () => {
    expect(sanitizeLoginRedirect('/\\evil.example.com')).toBe(HOME_ROUTE)
    expect(sanitizeLoginRedirect('\\evil.example.com')).toBe(HOME_ROUTE)
    expect(sanitizeLoginRedirect('/%2F%2Fevil.example.com')).toBe(HOME_ROUTE)
    expect(sanitizeLoginRedirect('%2F%2Fevil.example.com')).toBe(HOME_ROUTE)
    expect(sanitizeLoginRedirect('/..%2F..%2Fevil')).toBe(HOME_ROUTE)
  })
  it('危险 scheme 拒绝并回退', () => {
    expect(sanitizeLoginRedirect('javascript:alert(1)')).toBe(HOME_ROUTE)
    expect(sanitizeLoginRedirect('JaVaScRiPt:alert(1)')).toBe(HOME_ROUTE)
    expect(sanitizeLoginRedirect('data:text/html;base64,PHNjcmlwdD4=')).toBe(HOME_ROUTE)
    expect(sanitizeLoginRedirect('vbscript:msgbox(1)')).toBe(HOME_ROUTE)
  })
  it('相对路径（无前导斜杠）拒绝并回退，避免 vue-router 相对解析歧义', () => {
    expect(sanitizeLoginRedirect('index')).toBe(HOME_ROUTE)
    expect(sanitizeLoginRedirect('./system/user')).toBe(HOME_ROUTE)
    expect(sanitizeLoginRedirect('system/user')).toBe(HOME_ROUTE)
  })
  it('空值 / 非字符串 / 控制字符拒绝并回退；支持自定义 fallback', () => {
    expect(sanitizeLoginRedirect('')).toBe(HOME_ROUTE)
    expect(sanitizeLoginRedirect(null)).toBe(HOME_ROUTE)
    expect(sanitizeLoginRedirect(undefined)).toBe(HOME_ROUTE)
    expect(sanitizeLoginRedirect(123 as unknown)).toBe(HOME_ROUTE)
    expect(sanitizeLoginRedirect({ path: '/index' } as unknown)).toBe(HOME_ROUTE)
    expect(sanitizeLoginRedirect(['/index'] as unknown)).toBe(HOME_ROUTE)
    expect(sanitizeLoginRedirect('/index\n')).toBe(HOME_ROUTE)
    expect(sanitizeLoginRedirect('/in\u0000dex')).toBe(HOME_ROUTE)
    expect(sanitizeLoginRedirect('  /index  ')).toBe('/index')
    expect(sanitizeLoginRedirect('https://evil.example.com', '/403')).toBe('/403')
    expect(sanitizeLoginRedirect('', '/403')).toBe('/403')
  })
  it('fallback 自身也须是站内路径，否则回退到首页（防止调用方传入被污染的兜底值）', () => {
    expect(sanitizeLoginRedirect('https://evil.example.com', '//evil2.example.com')).toBe(HOME_ROUTE)
    expect(sanitizeLoginRedirect(null, 'javascript:alert(1)')).toBe(HOME_ROUTE)
  })
})

describe('resolveGuardNavigation：全局前置守卫决策（纯函数）', () => {
  it('未登录 + 登录前白名单 → 放行', () => {
    const d = resolveGuardNavigation({
      toPath: '/login',
      toFullPath: '/login',
      hasToken: false,
      isUserResolved: false,
      snapshot: emptySnapshot(),
    })
    expect(d.type).toBe('allow')
  })
  it('未登录 + 业务路由 → 重定向登录页，redirect 为消毒后的原目的地', () => {
    const d = resolveGuardNavigation({
      toPath: '/system/user',
      toFullPath: '/system/user?pageNo=2',
      hasToken: false,
      isUserResolved: false,
      snapshot: emptySnapshot(),
    })
    expect(d.type).toBe('login')
    expect(d.type === 'login' && d.redirect).toBe('/system/user?pageNo=2')
  })
  it('未登录 + 目的地不可消毒（异常构造）→ 回退首页，不带开放重定向参数', () => {
    const d = resolveGuardNavigation({
      toPath: '/x',
      toFullPath: '//evil.example.com/x',
      hasToken: false,
      isUserResolved: false,
      snapshot: emptySnapshot(),
    })
    expect(d.type).toBe('login')
    expect(d.type === 'login' && d.redirect).toBe(HOME_ROUTE)
  })
  it('未登录 + 公共外壳（首页 / 403 / 个人中心）→ 仍重定向登录页（不放宽未认证可达面）', () => {
    for (const p of ['/', HOME_ROUTE, UNAUTHORIZED_ROUTE, '/404', '/user/profile', '/redirect/system/user']) {
      const d = resolveGuardNavigation({
        toPath: p,
        toFullPath: p,
        hasToken: false,
        isUserResolved: false,
        snapshot: emptySnapshot(),
      })
      expect(d.type).toBe('login')
    }
  })
  it('已登录访问登录页 → 回首页', () => {
    const d = resolveGuardNavigation({
      toPath: LOGIN_ROUTE,
      toFullPath: LOGIN_ROUTE,
      hasToken: true,
      isUserResolved: true,
      snapshot: techSnapshot(),
    })
    expect(d.type).toBe('allow-home')
  })
  it('已登录但授权快照未装配 → bootstrap（触发 setUserInfoAction + 动态路由装配）', () => {
    const d = resolveGuardNavigation({
      toPath: '/system/user',
      toFullPath: '/system/user',
      hasToken: true,
      isUserResolved: false,
      snapshot: emptySnapshot(),
    })
    expect(d.type).toBe('bootstrap')
  })
  it('已装配 + 已授权 → allow；未授权 → unauthorized（落点 /403）', () => {
    expect(
      resolveGuardNavigation({
        toPath: '/system/user',
        toFullPath: '/system/user',
        hasToken: true,
        isUserResolved: true,
        snapshot: techSnapshot(),
      }).type,
    ).toBe('allow')
    expect(
      resolveGuardNavigation({
        toPath: '/crm/customer',
        toFullPath: '/crm/customer',
        hasToken: true,
        isUserResolved: true,
        snapshot: techSnapshot(),
      }).type,
    ).toBe('unauthorized')
    expect(UNAUTHORIZED_ROUTE).toBe('/403')
  })
  it('已装配 + 公共外壳 → allow（不受业务授权门禁）', () => {
    expect(
      resolveGuardNavigation({
        toPath: '/index',
        toFullPath: '/index',
        hasToken: true,
        isUserResolved: true,
        snapshot: emptySnapshot(),
      }).type,
    ).toBe('allow')
  })
})

describe('resolveBootstrapTarget：验收④ 登录失败 / 装配后目的地校验', () => {
  it('无 redirect 参数 → 使用 to.fullPath', () => {
    const r = resolveBootstrapTarget({
      toFullPath: '/system/user',
      redirectQuery: undefined,
      snapshot: techSnapshot(),
    })
    expect(r.target).toBe('/system/user')
    expect(r.preserveToLocation).toBe(true)
  })
  it('redirect 合法且已授权 → 使用 redirect（不保留 to 定位）', () => {
    const r = resolveBootstrapTarget({
      toFullPath: '/login',
      redirectQuery: '/system/role',
      snapshot: techSnapshot(),
    })
    expect(r.target).toBe('/system/role')
    expect(r.preserveToLocation).toBe(false)
  })
  it('redirect 为跨站地址 → 消毒回退到 to.fullPath（防开放重定向）', () => {
    const r = resolveBootstrapTarget({
      toFullPath: '/index',
      redirectQuery: 'https://evil.example.com/steal',
      snapshot: techSnapshot(),
    })
    expect(r.target).toBe('/index')
    expect(r.preserveToLocation).toBe(true)
  })
  it('redirect 指向关闭模块 / 未注册路由 → 落 403，不进入半初始化页面', () => {
    const r = resolveBootstrapTarget({
      toFullPath: '/index',
      redirectQuery: '/crm/customer/detail/1',
      snapshot: techSnapshot(),
    })
    expect(r.target).toBe(UNAUTHORIZED_ROUTE)
    expect(r.preserveToLocation).toBe(false)
  })
  it('redirect 为非字符串（数组污染 query）→ 回退 to.fullPath', () => {
    const r = resolveBootstrapTarget({
      toFullPath: '/index',
      redirectQuery: ['/crm/customer', '/system/user'],
      snapshot: techSnapshot(),
    })
    expect(r.target).toBe('/index')
  })
  it('to.fullPath 自身未授权 → 落 403（直达关闭模块）', () => {
    const r = resolveBootstrapTarget({
      toFullPath: '/mes/wm/warehouse/location',
      redirectQuery: undefined,
      snapshot: techSnapshot(),
    })
    expect(r.target).toBe(UNAUTHORIZED_ROUTE)
  })
  it('authorized 标记区分「已授权落地」与「拒绝落地」，供守卫选择 403 / 404 语义', () => {
    expect(
      resolveBootstrapTarget({ toFullPath: '/system/user', redirectQuery: undefined, snapshot: techSnapshot() })
        .authorized,
    ).toBe(true)
    expect(
      resolveBootstrapTarget({ toFullPath: '/login', redirectQuery: '/system/role', snapshot: techSnapshot() })
        .authorized,
    ).toBe(true)
    expect(
      resolveBootstrapTarget({ toFullPath: '/index', redirectQuery: '/crm/customer', snapshot: techSnapshot() })
        .authorized,
    ).toBe(false)
    expect(
      resolveBootstrapTarget({ toFullPath: '/totally/unknown', redirectQuery: undefined, snapshot: techSnapshot() })
        .authorized,
    ).toBe(false)
  })
})

describe('isNotFoundMatch：区分「路由不存在」与「已注册但未授权」', () => {
  it('只命中 remaining.ts 的 catch-all（/:pathMatch(.*)*）→ 视为不存在（404 语义）', () => {
    expect(isNotFoundMatch([{ path: '/:pathMatch(.*)*', name: '' }])).toBe(true)
  })
  it('只命中动态追加的 catch-all（/:path(.*)*）→ 视为不存在（404 语义）', () => {
    expect(isNotFoundMatch([{ path: '/:path(.*)*', name: '404Page' }])).toBe(true)
  })
  it('命中真实路由记录 → 已注册（403 语义）', () => {
    expect(isNotFoundMatch([{ path: '/crm', name: 'CrmCenter' }, { path: 'customer/detail/:id' }])).toBe(false)
    expect(isNotFoundMatch([{ path: '/system/user', name: 'SystemUser' }])).toBe(false)
  })
  it('空匹配结果 / 非数组 → 视为不存在', () => {
    expect(isNotFoundMatch([])).toBe(true)
    expect(isNotFoundMatch(undefined as unknown as never[])).toBe(true)
    expect(isNotFoundMatch(null as unknown as never[])).toBe(true)
  })
  it('404 与 403 落点常量正确（守卫据此选择落点）', () => {
    expect(NOT_FOUND_ROUTE).toBe('/404')
    expect(UNAUTHORIZED_ROUTE).toBe('/403')
  })
})

describe('resolvePostAuthRedirect：登录/注册/社交登录成功后的落地目的地（验收① + 防开放重定向）', () => {
  it('SSO 回调精确判定：只认站内 /sso，不做子串匹配', () => {
    // 原实现 `redirect.indexOf('sso') !== -1` 会把任何含 "sso" 的站内路径误判为 SSO 回调，
    // 进而触发整页跳转、绕过 SPA 路由与授权守卫。
    expect(isSsoLoginRedirect('/sso')).toBe(true)
    expect(isSsoLoginRedirect('/sso?state=abc')).toBe(true)
    expect(isSsoLoginRedirect('/system/sso-config')).toBe(false)
    expect(isSsoLoginRedirect('/ssox')).toBe(false)
    expect(isSsoLoginRedirect('sso')).toBe(false)
    expect(isSsoLoginRedirect('https://evil.com/sso')).toBe(false)
    expect(isSsoLoginRedirect(undefined)).toBe(false)
    expect(isSsoLoginRedirect(['/sso'])).toBe(false)
    expect(SSO_ROUTE).toBe('/sso')
  })

  it('普通站内目的地 → 走 SPA 路由，不做整页跳转', () => {
    const r = resolvePostAuthRedirect('/system/user')
    expect(r.target).toBe('/system/user')
    expect(r.fullPageUrl).toBe(null)
  })

  it('空值 / 非字符串 → 回退首页（不得沿用 `permissionStore.addRouters[0].path` 之类会抛错的兜底）', () => {
    expect(resolvePostAuthRedirect('').target).toBe(HOME_ROUTE)
    expect(resolvePostAuthRedirect(undefined).target).toBe(HOME_ROUTE)
    expect(resolvePostAuthRedirect(null).target).toBe(HOME_ROUTE)
    expect(resolvePostAuthRedirect(['/index']).target).toBe(HOME_ROUTE)
  })

  it('跨站 / 协议相对 / 反斜杠 / 目录穿越目的地 → 一律回退首页，且不触发整页跳转', () => {
    for (const evil of [
      'https://evil.com/x',
      '//evil.com/x',
      '/\\evil.com/x',
      'javascript:alert(1)',
      '/../etc/passwd',
      '/%2F%2Fevil.com'
    ]) {
      const r = resolvePostAuthRedirect(evil)
      expect(r.target).toBe(HOME_ROUTE)
      expect(r.fullPageUrl).toBe(null)
    }
  })

  it('SSO 回调 → 给出可直接整页跳转的站内 URL，并继承部署 base', () => {
    // VITE_BASE_PATH 在 .env.test 下为 /admin-ui-vue3/，整页跳转必须带上 base，
    // 否则 SSO 回调会落到站点根而 404。
    expect(resolvePostAuthRedirect('/sso', '/').fullPageUrl).toBe('/sso')
    expect(resolvePostAuthRedirect('/sso', '/admin-ui-vue3/').fullPageUrl).toBe('/admin-ui-vue3/sso')
    expect(resolvePostAuthRedirect('/sso?state=abc', '/').fullPageUrl).toBe('/sso?state=abc')
  })

  it('base 不可信（跨站 CDN 域名 / 空值）时不拼接，只返回站内路径', () => {
    expect(resolvePostAuthRedirect('/sso', 'http://static.example.com/').fullPageUrl).toBe('/sso')
    expect(resolvePostAuthRedirect('/sso', '').fullPageUrl).toBe('/sso')
    expect(resolvePostAuthRedirect('/sso', undefined).fullPageUrl).toBe('/sso')
  })

  it('非 SSO 目的地即使传入 base 也不产生整页跳转 URL', () => {
    expect(resolvePostAuthRedirect('/index', '/admin-ui-vue3/').fullPageUrl).toBe(null)
  })
})
