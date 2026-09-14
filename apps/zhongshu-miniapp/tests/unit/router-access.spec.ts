import { beforeEach, describe, expect, it, vi } from 'vitest'
import { hasRouteAccess, normalizeRoute, UNAUTHORIZED_PAGE } from '@/router/access'
import { navigateToInterceptor } from '@/router/interceptor'
import { customTabbarList } from '@/tabbar/config'

// ZS-CLIENT-002.A：移动端「服务端授权导航 + 直达页守卫」单元测试。
//
// 被测：
//   1) @/router/access 的 hasRouteAccess——注册表直接消费真实 pages/index/menu.json（单一真相源），
//      公共外壳放行 / 业务页按 menu.json 权限 / 子页按模块目录前缀继承 / 未登记默认拒绝（计划 §5 交付 2/4）。
//   2) @/router/interceptor 的 navigateToInterceptor——登录后授权门禁 → 未授权 reLaunch 到 403 落点。
//   3) TabBar 授权化——isTabbarItemVisible 委托 hasRouteAccess，以真实 customTabbarList 验证入口分类。
//
// 与 http interceptor.spec（ZS-CLIENT-003）一致的隔离策略：
//   vi.mock 切断 @/utils(index)→@/pages.json（测试环境未构建、pages.json 未生成）、
//   @/store/token、@/store/user、@/tabbar/store；@/router/access（含其 menu.json 导入）与 @/router/config 用真实实现。
//   所有被 mock 工厂引用的可变状态（含受控页面表 PAGES）一律置于 vi.hoisted，避免 import 提升求值时的 TDZ。

const h = vi.hoisted(() => ({
  hasLogin: true,
  permissions: [] as string[],
  // 受控页面表：替代测试环境缺失的 @/pages.json（getAllPages 数据源）；含被 interceptor 存在性校验的业务页
  PAGES: [
    { path: '/pages/index/index' }, // 工作台（公共外壳）
    { path: '/pages/bpm/index' }, // 审批（menu.json：task/process-instance/process-instance-cc 任一）
    { path: '/pages/contact/index' }, // 通讯录（补充登记：system:user:list）
    { path: '/pages/message/index' }, // 消息（公共外壳）
    { path: '/pages/user/index' }, // 我的（公共外壳）
    { path: '/pages-core/error/403' }, // 授权失败落点（公共外壳，避免 reLaunch 自环）
    { path: '/pages-core/error/404' }, // 未知页落点
    { path: '/pages-system/user/index' }, // 系统用户管理（menu.json：system:user:list）
    { path: '/pages-system/user/detail/index' }, // 用户详情（子页，前缀继承 system:user:list）
    { path: '/pages-bpm/oa/leave/index' }, // OA 请假（menu.json：bpm:oa-leave:query）
    { path: '/pages-ai/chat/index' }, // AI 会话（menu.json：无 permission → 登录即放行）
    { path: '/pages-bpm/processInstance/detail/index' }, // BPM 审批详情（跨包共享子页，EXTRA 显式登记）
    { path: '/pages-im/home/contact/index' }, // IM 通讯录（登录即用目录子页，祖先前缀继承）
    { path: '/pages-crm/followup/form/index' }, // CRM 跟进表单（跨目录子页，crm 前缀继承）
    { path: '/pages-pay/cashier/index' }, // 收银台（显式补登：登录即用）
  ] as Array<Record<string, any>>,
}))

vi.mock('@/utils', () => ({
  getAllPages: (key?: string) => (key ? h.PAGES.filter(p => p[key]) : h.PAGES),
  getLastPage: () => ({ route: '/pages/index/index' }),
  HOME_PAGE: '/pages/index/index',
  parseUrlToObj: (url: string) => {
    const [path, q] = url.split('?')
    const query: Record<string, string> = {}
    if (q) {
      q.split('&').forEach((kv) => {
        const [k, v] = kv.split('=')
        query[k] = decodeURIComponent(v ?? '')
      })
    }
    return { path, query }
  },
}))

vi.mock('@/store/token', () => ({
  useTokenStore: () => ({ updateNowTime: () => ({ hasLogin: h.hasLogin }) }),
}))

vi.mock('@/store/user', () => ({
  useUserStore: () => ({ permissions: h.permissions, roles: [] as string[] }),
}))

vi.mock('@/tabbar/store', () => ({
  isPageTabbar: () => false,
  tabbarStore: { setAutoCurIdx: vi.fn() },
}))

const uni = (globalThis as any).uni

beforeEach(() => {
  h.hasLogin = true
  h.permissions = []
  uni.reLaunch.mockClear()
  uni.navigateTo.mockClear()
  uni.switchTab?.mockClear?.()
  uni.redirectTo.mockClear()
})

describe('normalizeRoute：移动路由归一化（去 query、补前导斜杠）', () => {
  it('剥离 query 并补前导斜杠', () => {
    expect(normalizeRoute('pages/bpm/index?id=1')).toBe('/pages/bpm/index')
    expect(normalizeRoute('/pages/bpm/index?a=1&b=2')).toBe('/pages/bpm/index')
  })
})

describe('hasRouteAccess：公共应用外壳（登录即放行，不受业务权限门禁）', () => {
  it('工作台 / 消息 / 我的 主包公共页放行', () => {
    expect(hasRouteAccess('/pages/index/index', [])).toBe(true)
    expect(hasRouteAccess('/pages/message/index', [])).toBe(true)
    expect(hasRouteAccess('/pages/user/index', [])).toBe(true)
  })
  it('工作台子页（搜索 / 设置）随 /pages/index/ 前缀放行', () => {
    expect(hasRouteAccess('/pages/index/settings/index', [])).toBe(true)
    expect(hasRouteAccess('/pages/index/search/index', [])).toBe(true)
  })
  it('pages-core 外壳（登录 / 错误落点 / 个人中心）放行——403 落点自身放行杜绝 reLaunch 自环', () => {
    expect(hasRouteAccess('/pages-core/error/403', [])).toBe(true)
    expect(hasRouteAccess('/pages-core/error/404', [])).toBe(true)
    expect(hasRouteAccess('/pages-core/auth/login', [])).toBe(true)
    expect(hasRouteAccess('/pages-core/user/profile/index', [])).toBe(true)
  })
})

describe('hasRouteAccess：业务页按 menu.json 服务端授权（P1 未授权/未注册拒绝）', () => {
  it('系统用户管理：持 system:user:list 放行，否则拒绝（P1：不再基线放行）', () => {
    expect(hasRouteAccess('/pages-system/user/index', ['system:user:list'])).toBe(true)
    expect(hasRouteAccess('/pages-system/user/index', [])).toBe(false)
    expect(hasRouteAccess('/pages-system/user/index', ['bpm:task:query'])).toBe(false)
  })
  it('客户管理（CRM）：持 crm:customer:query 放行，否则拒绝（关闭模块落 403）', () => {
    expect(hasRouteAccess('/pages-crm/customer/index', ['crm:customer:query'])).toBe(true)
    expect(hasRouteAccess('/pages-crm/customer/index', [])).toBe(false)
  })
  it('子页按模块目录前缀继承列表页权限（详情 / 表单未逐条登记）', () => {
    expect(hasRouteAccess('/pages-system/user/detail/index', ['system:user:list'])).toBe(true)
    expect(hasRouteAccess('/pages-system/user/detail/index', [])).toBe(false)
  })
  it('menu.json 无 permission 的登记项 = 登录即放行（AI 会话 / IM）', () => {
    expect(hasRouteAccess('/pages-ai/chat/index', [])).toBe(true)
    expect(hasRouteAccess('/pages-im/home/conversation/index', [])).toBe(true)
  })
  it('既非公共又未登记的页面 → 拒绝（计划 §5 交付 2「未注册页面不可进入」）', () => {
    expect(hasRouteAccess('/pages/some/unregistered', [])).toBe(false)
    expect(hasRouteAccess('/pages-mall/order/index', ['system:user:list'])).toBe(false)
  })
})

describe('hasRouteAccess：BPM 页保留各自权限（P2 修复：不再一律要求 bpm:task:query）', () => {
  it('共享审批页 /pages/bpm/index 聚合多入口权限，任一命中即放行', () => {
    expect(hasRouteAccess('/pages/bpm/index', ['bpm:task:query'])).toBe(true)
    expect(hasRouteAccess('/pages/bpm/index?tab=my', ['bpm:process-instance:query'])).toBe(true)
    expect(hasRouteAccess('/pages/bpm/index?tab=copy', ['bpm:process-instance-cc:query'])).toBe(true)
    expect(hasRouteAccess('/pages/bpm/index', [])).toBe(false)
  })
  it('请假页（OA）按 bpm:oa-leave:query 授权——仅持 bpm:task:query 不得进入（P2 核心）', () => {
    expect(hasRouteAccess('/pages-bpm/oa/leave/index', ['bpm:oa-leave:query'])).toBe(true)
    expect(hasRouteAccess('/pages-bpm/oa/leave/index', ['bpm:task:query'])).toBe(false)
    expect(hasRouteAccess('/pages-bpm/oa/leave/index', [])).toBe(false)
  })
})

describe('hasRouteAccess：通讯录（补充登记）与路由边界', () => {
  it('通讯录按 system:user:list 授权', () => {
    expect(hasRouteAccess('/pages/contact/index', ['system:user:list'])).toBe(true)
    expect(hasRouteAccess('/pages/contact/index', [])).toBe(false)
  })
  it('不把 Web component 路径当移动路由（注册表源自 menu.json，仅含 /pages 移动路径）', () => {
    // admin-web 组件路径（system/user/index）即便持有对应权限也非已登记移动路由 → 拒绝
    expect(hasRouteAccess('system/user/index', ['system:user:list'])).toBe(false)
    expect(hasRouteAccess('/system/user/index', ['system:user:list'])).toBe(false)
  })
  it('query 不影响授权判定', () => {
    expect(hasRouteAccess('/pages-system/user/index?redirect=%2Fx', ['system:user:list'])).toBe(true)
    expect(hasRouteAccess('/pages-system/user/index?redirect=%2Fx', [])).toBe(false)
  })
})

describe('hasRouteAccess：跨包 / 中转子页继承（r1-P1：默认拒绝不得误伤既有授权导航）', () => {
  it('审批详情（BPM）：普通审批用户（process-instance:query，来自主包 /pages/bpm/）可进入', () => {
    expect(hasRouteAccess('/pages-bpm/processInstance/detail/index', ['bpm:process-instance:query'])).toBe(true)
    expect(hasRouteAccess('/pages-bpm/processInstance/detail/audit/index', ['bpm:task:query'])).toBe(true)
    expect(hasRouteAccess('/pages-bpm/processInstance/create/index', ['bpm:process-instance-cc:query'])).toBe(true)
  })
  it('审批详情（BPM）：管理员（manager-query）可进入，但非 BPM 权限 / 无权限仍拒', () => {
    expect(hasRouteAccess('/pages-bpm/processInstance/detail/index', ['bpm:process-instance:manager-query'])).toBe(true)
    expect(hasRouteAccess('/pages-bpm/processInstance/detail/index', ['crm:customer:query'])).toBe(false)
    expect(hasRouteAccess('/pages-bpm/processInstance/detail/index', [])).toBe(false)
  })
  it('登录即用模块的中转子页（IM 通讯录）随目录继承登录即用', () => {
    expect(hasRouteAccess('/pages-im/home/contact/index', [])).toBe(true)
    expect(hasRouteAccess('/pages-im/home/contact/request/index', [])).toBe(true)
  })
  it('客户跟进表单（CRM，跨目录子页）继承 crm 模块权限：持 crm 放行、非 crm 拒', () => {
    expect(hasRouteAccess('/pages-crm/followup/form/index', ['crm:customer:query'])).toBe(true)
    expect(hasRouteAccess('/pages-crm/followup/form/index', ['bpm:task:query'])).toBe(false)
  })
  it('收银台（跨目录登录即用落点，显式补登）：任意登录用户可达', () => {
    expect(hasRouteAccess('/pages-pay/cashier/index', [])).toBe(true)
    expect(hasRouteAccess('/pages-pay/cashier/index?id=1', [])).toBe(true)
  })
  it('模块根前缀包络隔离：持无关模块权限者不得进入未登记子页（不回退跨模块放行）', () => {
    expect(hasRouteAccess('/pages-mall/promotion/bargain/help/index', ['system:user:list', 'bpm:task:query'])).toBe(false)
  })
})

describe('hasRouteAccess：具体子目录拒绝不被更宽兜底覆盖（r2-P2-1：持无关权限不得进入受控子页）', () => {
  it('仅持 system:dept:query：用户管理列表页及其详情/表单子页均拒（dept 与 user 是 /pages-system/ 下兄弟目录）', () => {
    expect(hasRouteAccess('/pages-system/user/index', ['system:dept:query'])).toBe(false)
    expect(hasRouteAccess('/pages-system/user/detail/index', ['system:dept:query'])).toBe(false)
    expect(hasRouteAccess('/pages-system/user/form/index', ['system:dept:query'])).toBe(false)
    // 对照：持 system:user:list 时子页经最近祖先 /pages-system/user/ 继承放行
    expect(hasRouteAccess('/pages-system/user/detail/index', ['system:user:list'])).toBe(true)
  })
  it('仅持无关 hrm 权限：员工详情子页拒（最近祖先 /pages-hrm/employee/ 要求 employee 权限，不回退 hrm 模块并集）', () => {
    expect(hasRouteAccess('/pages-hrm/employee/detail/index', ['hrm:recruit:candidate:query'])).toBe(false)
    expect(hasRouteAccess('/pages-hrm/employee/detail/index', ['hrm:employee:query'])).toBe(true)
  })
})

describe('navigateToInterceptor：登录后授权门禁与直达守卫', () => {
  it('已登录 + 持有权限 → 放行受保护页（不跳 403）', () => {
    h.permissions = ['bpm:task:query']
    const ret = navigateToInterceptor.invoke({ url: '/pages/bpm/index' })
    expect(ret).toBe(true)
    expect(uni.reLaunch).not.toHaveBeenCalledWith({ url: UNAUTHORIZED_PAGE })
  })

  it('已登录 + 无权限 → 拦截到 403 落点并阻止原路由（验收：登录但无页面授权拦截）', () => {
    h.permissions = []
    const ret = navigateToInterceptor.invoke({ url: '/pages/bpm/index' })
    expect(ret).toBe(false)
    expect(uni.reLaunch).toHaveBeenCalledWith({ url: UNAUTHORIZED_PAGE })
  })

  it('已登录 + 直达未授权业务页（系统用户管理）→ 拦截到 403（P1：直达守卫覆盖业务分包）', () => {
    h.permissions = []
    const ret = navigateToInterceptor.invoke({ url: '/pages-system/user/index' })
    expect(ret).toBe(false)
    expect(uni.reLaunch).toHaveBeenCalledWith({ url: UNAUTHORIZED_PAGE })
  })

  it('已登录 + 直达「我的流程」持 process-instance 权限 → 放行（P2：不误当 task:query 拒绝）', () => {
    h.permissions = ['bpm:process-instance:query']
    const ret = navigateToInterceptor.invoke({ url: '/pages/bpm/index?tab=my' })
    expect(ret).toBe(true)
    expect(uni.reLaunch).not.toHaveBeenCalledWith({ url: UNAUTHORIZED_PAGE })
  })

  it('已登录 + 直达 OA 请假仅持 task:query → 拦截到 403（P2：各页保留自身权限）', () => {
    h.permissions = ['bpm:task:query']
    const ret = navigateToInterceptor.invoke({ url: '/pages-bpm/oa/leave/index' })
    expect(ret).toBe(false)
    expect(uni.reLaunch).toHaveBeenCalledWith({ url: UNAUTHORIZED_PAGE })
  })

  it('已登录 + 公共页（工作台）→ 放行', () => {
    h.permissions = []
    const ret = navigateToInterceptor.invoke({ url: '/pages/index/index' })
    expect(ret).toBe(true)
  })

  it('未知页面（不在页面表）→ 走 NOT_FOUND，不误判为授权失败', () => {
    h.permissions = ['bpm:task:query']
    const ret = navigateToInterceptor.invoke({ url: '/pages/does/not-exist' })
    expect(ret).toBe(false)
    expect(uni.reLaunch).not.toHaveBeenCalledWith({ url: UNAUTHORIZED_PAGE })
    expect(uni.navigateTo).toHaveBeenCalled()
  })

  it('未登录 → 走登录门禁（白名单策略），不进入授权判定', () => {
    h.hasLogin = false
    h.permissions = ['bpm:task:query']
    const ret = navigateToInterceptor.invoke({ url: '/pages/bpm/index' })
    expect(ret).toBe(false)
    expect(uni.reLaunch).not.toHaveBeenCalledWith({ url: UNAUTHORIZED_PAGE })
  })

  it('冷启动/恢复一致：同一权限集对同一路由判定稳定（重复调用结果一致）', () => {
    h.permissions = ['system:user:list']
    const r1 = navigateToInterceptor.invoke({ url: '/pages/contact/index' })
    const r2 = navigateToInterceptor.invoke({ url: '/pages/contact/index' })
    expect(r1).toBe(true)
    expect(r2).toBe(true)
    // 无 bpm 权限时审批始终被拦，行为一致
    const r3 = navigateToInterceptor.invoke({ url: '/pages/bpm/index' })
    const r4 = navigateToInterceptor.invoke({ url: '/pages/bpm/index' })
    expect(r3).toBe(false)
    expect(r4).toBe(false)
  })

  it('已登录 + 普通审批用户直达 BPM 审批详情（跨包子页）→ 放行（r1-P1：不误伤既有授权导航）', () => {
    h.permissions = ['bpm:process-instance:query']
    const ret = navigateToInterceptor.invoke({ url: '/pages-bpm/processInstance/detail/index?id=1' })
    expect(ret).toBe(true)
    expect(uni.reLaunch).not.toHaveBeenCalledWith({ url: UNAUTHORIZED_PAGE })
  })

  it('已登录 + 无权限直达收银台（登录即用落点，显式补登）→ 放行', () => {
    h.permissions = []
    const ret = navigateToInterceptor.invoke({ url: '/pages-pay/cashier/index?id=1' })
    expect(ret).toBe(true)
  })

  it('已登录 + 无权限直达 IM 通讯录（登录即用模块子页）→ 放行（r1-P1）', () => {
    h.permissions = []
    const ret = navigateToInterceptor.invoke({ url: '/pages-im/home/contact/index' })
    expect(ret).toBe(true)
  })

  it('已登录 + 非 crm 权限直达 CRM 跟进表单 → 拦截到 403（crm 前缀包络不跨模块放行）', () => {
    h.permissions = ['bpm:task:query']
    const ret = navigateToInterceptor.invoke({ url: '/pages-crm/followup/form/index' })
    expect(ret).toBe(false)
    expect(uni.reLaunch).toHaveBeenCalledWith({ url: UNAUTHORIZED_PAGE })
  })
})

describe('tabbar 授权化：入口按服务端权限过滤（验收：不同技术权限看到对应入口）', () => {
  // isTabbarItemVisible 委托 hasRouteAccess（menu.json + 补充登记为单一真相源）；此处以真实 customTabbarList
  // 数据验证「哪些入口对哪些权限可见」的端到端分类结果。
  const visibleTabs = (perms: string[]) =>
    customTabbarList.filter(item => hasRouteAccess(item.pagePath.startsWith('/') ? item.pagePath : `/${item.pagePath}`, perms)).map(item => item.text)

  it('无业务权限：仅公共入口（工作台/消息/我的），审批与通讯录隐藏', () => {
    expect(visibleTabs([])).toEqual(['工作台', '消息', '我的'])
  })
  it('持 bpm:task:query：审批入口可见', () => {
    expect(visibleTabs(['bpm:task:query'])).toContain('审批')
  })
  it('持 system:user:list：通讯录入口可见', () => {
    expect(visibleTabs(['system:user:list'])).toContain('通讯录')
  })
  it('全权限：五入口按配置顺序全可见', () => {
    expect(visibleTabs(['bpm:task:query', 'system:user:list']))
      .toEqual(['工作台', '审批', '通讯录', '消息', '我的'])
  })
})
