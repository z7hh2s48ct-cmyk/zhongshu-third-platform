import { beforeEach, describe, expect, it, vi } from 'vitest'

const h = vi.hoisted(() => ({
  accessToken: 'TK' as string | null,
  cache: new Map<string, any>(),
  getInfoImpl: (() => Promise.resolve(null)) as () => Promise<any>,
  loginOutCalls: 0,
  deleteUserCacheCalls: 0,
  generateRouteInputs: [] as any[][],
}))

vi.mock('@/store', async () => {
  const { createPinia } = await import('pinia')
  return { store: createPinia(), setupStore: () => {} }
})
vi.mock('@/utils/auth', () => ({
  getAccessToken: () => h.accessToken,
  removeToken: () => {
    h.cache.delete('accessToken')
  },
}))
vi.mock('@/api/login', () => ({
  getInfo: () => h.getInfoImpl(),
  loginOut: () => {
    h.loginOutCalls++
    return Promise.resolve(true)
  },
}))
vi.mock('@/hooks/web/useCache', () => ({
  CACHE_KEY: {
    ROLE_ROUTERS: 'roleRouters',
    USER: 'user',
    VisitTenantId: 'visitTenantId',
    DICT_CACHE: 'dictCache',
    IS_DARK: 'isDark',
    LANG: 'lang',
    THEME: 'theme',
    LAYOUT: 'layout',
    LoginForm: 'loginForm',
    TenantId: 'tenantId',
  },
  useCache: () => ({
    wsCache: {
      get: (key: string) => (h.cache.has(key) ? h.cache.get(key) : null),
      set: (key: string, value: any) => {
        h.cache.set(key, value)
      },
      delete: (key: string) => {
        h.cache.delete(key)
      },
    },
  }),
  deleteUserCache: () => {
    h.deleteUserCacheCalls++
    h.cache.delete('user')
    h.cache.delete('roleRouters')
    h.cache.delete('visitTenantId')
  },
}))
vi.mock('@/router/modules/remaining', () => ({ default: [] }))
vi.mock('@/utils/routerHelper', () => ({
  flatMultiLevelRoutes: (routes: any[]) => routes,
  generateRoute: (routes: any[]) => {
    h.generateRouteInputs.push(routes)
    return routes.map((route: any) => ({
      path: route.path,
      name: route.path,
      meta: { title: route.path },
    }))
  },
}))

import { hasRouteAccess } from '@/router/access'
import { usePermissionStoreWithOut } from '@/store/modules/permission'
import { useUserStoreWithOut } from '@/store/modules/user'

// ZS-CLIENT-001.A：授权快照来源合同。
//
// 卡片「验收②」：关闭模块、失效缓存、伪造前端角色不能操作。
// 旧实现（src/store/modules/user.ts#setUserInfoAction）在 getInfo 失败时静默回退 localStorage
// 中的 USER 缓存身份，使「失效缓存 / 伪造前端角色」可继续操作 —— 本文件锁死该回归。
//
// 卡片「验收④」：登录失败不留下半初始化路由。
// 装配失败必须返回 null 且把授权快照清成空，permission.ts 据此不执行任何 addRoute。

const SERVER_MENUS = [
  { path: '/system', children: [{ path: '/system/user' }, { path: '/system/dict' }] },
  { path: '/infra', children: [{ path: '/infra/codegen' }] },
]

/** 伪造的本地缓存身份：额外塞入 crm / mall 菜单与高权限标识 */
const FORGED_CACHE = {
  user: { id: 1, avatar: '', nickname: 'forged', deptId: 0 },
  roles: ['super_admin'],
  permissions: ['crm:customer:query', 'mall:product-spu:query', 'system:user:query'],
  menus: [
    { path: '/system/user' },
    { path: '/crm/customer' },
    { path: '/mall/product/spu' },
  ],
}

function serverUserInfo(menus: any[] = SERVER_MENUS) {
  return {
    user: { id: 1, avatar: '', nickname: 'tech', deptId: 0 },
    roles: ['tech_account'],
    permissions: ['system:user:query'],
    menus,
  }
}

beforeEach(() => {
  h.accessToken = 'TK'
  h.cache = new Map<string, any>()
  h.loginOutCalls = 0
  h.deleteUserCacheCalls = 0
  h.generateRouteInputs = []
  h.getInfoImpl = () => Promise.resolve(serverUserInfo())
  useUserStoreWithOut().$reset()
  usePermissionStoreWithOut().$reset()
})

describe('setUserInfoAction：授权快照只能来自服务端本次响应（验收①②）', () => {
  it('正向：getInfo 成功 → 快照由服务端菜单构建，已下发页面可达', async () => {
    const userStore = useUserStoreWithOut()
    const result = await userStore.setUserInfoAction()
    expect(result).not.toBeNull()
    expect(userStore.getIsSetUser).toBe(true)
    expect(userStore.getMenus).toEqual(SERVER_MENUS)
    expect(hasRouteAccess('/system/user', userStore.getRouteAccess)).toBe(true)
    expect(hasRouteAccess('/system/dict', userStore.getRouteAccess)).toBe(true)
    expect(hasRouteAccess('/infra/codegen', userStore.getRouteAccess)).toBe(true)
  })

  it('反向：服务端未下发的模块 → 快照拒绝（关闭模块不可进入）', async () => {
    const userStore = useUserStoreWithOut()
    await userStore.setUserInfoAction()
    expect(hasRouteAccess('/crm/customer', userStore.getRouteAccess)).toBe(false)
    expect(hasRouteAccess('/mall/product/spu', userStore.getRouteAccess)).toBe(false)
    expect(hasRouteAccess('/crm/customer/detail/1', userStore.getRouteAccess)).toBe(false)
  })

  it('反向：失效缓存 —— getInfo 失败时不得回退 localStorage 身份（返回 null + 空快照 + 清缓存）', async () => {
    const userStore = useUserStoreWithOut()
    h.cache.set('user', FORGED_CACHE)
    h.cache.set('roleRouters', FORGED_CACHE.menus)
    h.getInfoImpl = () => Promise.reject(new Error('500 Internal Server Error'))

    const result = await userStore.setUserInfoAction()

    expect(result).toBeNull()
    expect(userStore.getIsSetUser).toBe(false)
    expect(userStore.getMenus).toEqual([])
    expect(userStore.getRouteAccess.menuPatterns).toHaveLength(0)
    // 伪造缓存身份不得转化为任何可达路由
    expect(hasRouteAccess('/crm/customer', userStore.getRouteAccess)).toBe(false)
    expect(hasRouteAccess('/system/user', userStore.getRouteAccess)).toBe(false)
    // 失效缓存被清除，不留下一次装配可复用的脏数据
    expect(h.deleteUserCacheCalls).toBe(1)
  })

  it('反向：伪造前端角色 —— 本地缓存的高权限标识 / 宽菜单不进入快照', async () => {
    const userStore = useUserStoreWithOut()
    h.cache.set('user', FORGED_CACHE)
    h.cache.set('roleRouters', FORGED_CACHE.menus)
    h.getInfoImpl = () => Promise.resolve(serverUserInfo([{ path: '/system/user' }]))

    await userStore.setUserInfoAction()

    // 快照只反映服务端本次响应，缓存中伪造的 crm / mall 菜单无效
    expect(hasRouteAccess('/crm/customer', userStore.getRouteAccess)).toBe(false)
    expect(hasRouteAccess('/mall/product/spu', userStore.getRouteAccess)).toBe(false)
    expect(hasRouteAccess('/system/user', userStore.getRouteAccess)).toBe(true)
    expect(userStore.getRoles).toEqual(['tech_account'])
  })

  it('反向：无 token → 直接重置，不发起授权拉取', async () => {
    const userStore = useUserStoreWithOut()
    h.accessToken = null
    let called = false
    h.getInfoImpl = () => {
      called = true
      return Promise.resolve(serverUserInfo())
    }
    const result = await userStore.setUserInfoAction()
    expect(result).toBeNull()
    expect(called).toBe(false)
    expect(userStore.getIsSetUser).toBe(false)
    expect(userStore.getRouteAccess.menuPatterns).toHaveLength(0)
  })

  it('反向：getInfo 返回空响应 → 视为装配失败，不留半初始化授权', async () => {
    const userStore = useUserStoreWithOut()
    h.getInfoImpl = () => Promise.resolve(null)
    const result = await userStore.setUserInfoAction()
    expect(result).toBeNull()
    expect(userStore.getIsSetUser).toBe(false)
    expect(userStore.getRouteAccess.menuPatterns).toHaveLength(0)
  })

  it('验收③：退出后授权快照立即失效（旧页签判定依据被清空）', async () => {
    const userStore = useUserStoreWithOut()
    await userStore.setUserInfoAction()
    expect(hasRouteAccess('/system/user', userStore.getRouteAccess)).toBe(true)

    await userStore.loginOut()

    expect(h.loginOutCalls).toBe(1)
    expect(userStore.getIsSetUser).toBe(false)
    expect(userStore.getMenus).toEqual([])
    expect(hasRouteAccess('/system/user', userStore.getRouteAccess)).toBe(false)
  })

  it('resetState 清空授权快照（供清理合同复用）', async () => {
    const userStore = useUserStoreWithOut()
    await userStore.setUserInfoAction()
    userStore.resetState()
    expect(userStore.getRouteAccess.menuPatterns).toHaveLength(0)
    expect(userStore.getMenus).toEqual([])
  })
})

describe('generateRoutes：动态路由只由服务端内存菜单生成（验收②④）', () => {
  it('使用传入的服务端菜单，不读取 ROLE_ROUTERS 本地缓存', async () => {
    const permissionStore = usePermissionStoreWithOut()
    h.cache.set('roleRouters', FORGED_CACHE.menus)

    await permissionStore.generateRoutes(SERVER_MENUS as any)

    expect(h.generateRouteInputs).toHaveLength(1)
    expect(h.generateRouteInputs[0]).toEqual(SERVER_MENUS)
    const paths = permissionStore.getAddRouters.map((route: any) => route.path)
    expect(paths).not.toContain('/crm/customer')
    expect(paths).not.toContain('/mall/product/spu')
  })

  it('传入空菜单（装配失败 / 撤权后）→ 只剩 404 catch-all，不生成任何业务路由', async () => {
    const permissionStore = usePermissionStoreWithOut()
    h.cache.set('roleRouters', FORGED_CACHE.menus)

    await permissionStore.generateRoutes([])

    const paths = permissionStore.getAddRouters.map((route: any) => route.path)
    expect(paths).toEqual(['/:path(.*)*'])
  })

  it('未传入菜单 → 按空处理，绝不回退本地缓存（缓存不是授权来源）', async () => {
    const permissionStore = usePermissionStoreWithOut()
    h.cache.set('roleRouters', FORGED_CACHE.menus)

    await permissionStore.generateRoutes()

    expect(h.generateRouteInputs[0]).toEqual([])
    const paths = permissionStore.getAddRouters.map((route: any) => route.path)
    expect(paths).toEqual(['/:path(.*)*'])
  })

  it('$reset 后 addRouters 清空（登录失败不留半初始化路由）', async () => {
    const permissionStore = usePermissionStoreWithOut()
    await permissionStore.generateRoutes(SERVER_MENUS as any)
    expect(permissionStore.getAddRouters.length).toBeGreaterThan(1)

    permissionStore.$reset()

    expect(permissionStore.getAddRouters).toEqual([])
    expect(permissionStore.getRouters).toEqual([])
  })
})
