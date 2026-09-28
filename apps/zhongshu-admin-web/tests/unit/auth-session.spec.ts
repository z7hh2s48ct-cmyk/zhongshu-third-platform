import { beforeEach, describe, expect, it, vi } from 'vitest'

const h = vi.hoisted(() => ({
  resetRouterCalls: 0,
  removeTokenCalls: 0,
  deleteUserCacheCalls: 0,
  permissionResetCalls: 0,
  userResetCalls: 0,
  tagsClearCalls: 0,
  dictClearCalls: 0,
  objectAuthClearCalls: 0,
  sessionCacheDeletes: [] as string[],
  localCacheDeletes: [] as string[],
}))

vi.mock('@/router', () => ({
  resetRouter: () => {
    h.resetRouterCalls++
  },
  default: {},
}))
vi.mock('@/utils/auth', () => ({
  removeToken: () => {
    h.removeTokenCalls++
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
  useCache: (type?: string) => ({
    wsCache: {
      delete: (key: string) => {
        if (type === 'sessionStorage') h.sessionCacheDeletes.push(key)
        else h.localCacheDeletes.push(key)
      },
    },
  }),
  deleteUserCache: () => {
    h.deleteUserCacheCalls++
  },
}))
vi.mock('@/store/modules/permission', () => ({
  usePermissionStoreWithOut: () => ({
    $reset: () => {
      h.permissionResetCalls++
    },
  }),
}))
vi.mock('@/store/modules/user', () => ({
  useUserStoreWithOut: () => ({
    resetState: () => {
      h.userResetCalls++
    },
  }),
}))
vi.mock('@/store/modules/tagsView', () => ({
  useTagsViewStoreWithOut: () => ({
    clearAuthorizedViews: () => {
      h.tagsClearCalls++
    },
  }),
}))
vi.mock('@/store/modules/dict', () => ({
  useDictStoreWithOut: () => ({
    clearDictState: () => {
      h.dictClearCalls++
    },
  }),
}))
vi.mock('@/store/modules/objectAuthorization', () => ({
  useObjectAuthorizationStoreWithOut: () => ({
    clearObjectAuthorizationState: () => {
      h.objectAuthClearCalls++
    },
  }),
}))

import { clearAuthorizedSession, isAuthorizedSessionClearReason } from '@/utils/authSession'

// ZS-CLIENT-001.A：授权会话清理合同。
//
// 卡片「调整」要求：退出、撤权、技术租户变化时清理旧路由、缓存、页签和数据。
// 卡片「验收③」要求：撤权后旧页签 / 返回缓存不展示旧敏感数据。
//   —— 前端职责边界：只负责「清理与不展示」；接口独立拒绝属服务端 @PreAuthorize 职责。
//
// 三种 reason 的差异：
//   logout           用户主动退出：清页签 / keep-alive / 字典 + 卸动态路由 + 清身份与凭据
//   bootstrap-failed 授权装配失败（登录失败 / getInfo 失败）：同 logout，杜绝半初始化路由
//   tenant-switch    技术租户（visitTenantId）变化：清页签 / keep-alive / 字典 + 卸旧路由 +
//                    清身份快照（强制重新 bootstrap），但保留凭据与 VisitTenantId

function resetCounters() {
  h.resetRouterCalls = 0
  h.removeTokenCalls = 0
  h.deleteUserCacheCalls = 0
  h.permissionResetCalls = 0
  h.userResetCalls = 0
  h.tagsClearCalls = 0
  h.dictClearCalls = 0
  h.objectAuthClearCalls = 0
  h.sessionCacheDeletes = []
  h.localCacheDeletes = []
}

beforeEach(() => {
  resetCounters()
})

describe('clearAuthorizedSession：退出 / 装配失败清理合同（验收③④）', () => {
  it('logout：清理页签与 keep-alive 缓存（旧页签 / 返回缓存不展示旧敏感数据）', () => {
    clearAuthorizedSession('logout')
    expect(h.tagsClearCalls).toBe(1)
  })

  it('logout：清理字典快照（字典为租户级业务数据，属「缓存和数据」）', () => {
    clearAuthorizedSession('logout')
    expect(h.dictClearCalls).toBe(1)
  })

  it('logout：卸载登录后动态装配的路由（resetRouter），杜绝退出后旧路由仍可达', () => {
    clearAuthorizedSession('logout')
    expect(h.resetRouterCalls).toBe(1)
  })

  it('logout：重置权限快照 store 与用户身份 store（登录失败不留半初始化路由）', () => {
    clearAuthorizedSession('logout')
    expect(h.permissionResetCalls).toBe(1)
    expect(h.userResetCalls).toBe(1)
  })

  it('logout：删除用户缓存与访问凭据', () => {
    clearAuthorizedSession('logout')
    expect(h.deleteUserCacheCalls).toBe(1)
    expect(h.removeTokenCalls).toBe(1)
  })

  it('logout：清理对象授权快照（ZS-CLIENT-001.B 旧页签不泄露——对象级动作/字段快照不得跨主体残留）', () => {
    clearAuthorizedSession('logout')
    expect(h.objectAuthClearCalls).toBe(1)
  })

  it('bootstrap-failed：与 logout 同等彻底（授权装配失败即视为会话不可信）', () => {
    clearAuthorizedSession('bootstrap-failed')
    expect(h.tagsClearCalls).toBe(1)
    expect(h.dictClearCalls).toBe(1)
    expect(h.resetRouterCalls).toBe(1)
    expect(h.permissionResetCalls).toBe(1)
    expect(h.userResetCalls).toBe(1)
    expect(h.deleteUserCacheCalls).toBe(1)
    expect(h.removeTokenCalls).toBe(1)
    expect(h.objectAuthClearCalls).toBe(1)
  })

  it('幂等：重复清理不产生额外副作用累积以外的状态残留', () => {
    clearAuthorizedSession('logout')
    clearAuthorizedSession('logout')
    expect(h.resetRouterCalls).toBe(2)
    expect(h.tagsClearCalls).toBe(2)
    // 幂等意味着第二次调用同样把状态清到「空」，不存在只清一次的分支
  })
})

describe('clearAuthorizedSession：技术租户变化清理合同（验收③）', () => {
  it('tenant-switch：清理页签 / keep-alive / 字典（旧租户数据不得残留展示）', () => {
    clearAuthorizedSession('tenant-switch')
    expect(h.tagsClearCalls).toBe(1)
    expect(h.dictClearCalls).toBe(1)
  })

  it('tenant-switch：清理对象授权快照（ZS-CLIENT-001.B——旧租户对象级授权不得残留展示）', () => {
    clearAuthorizedSession('tenant-switch')
    expect(h.objectAuthClearCalls).toBe(1)
  })

  it('tenant-switch：卸旧路由 + 清身份/权限快照（卡片「调整」明列技术租户变化须清旧路由）', () => {
    // visit-tenant-id 会随每一个请求（含 getInfo）上送，菜单与授权可能随租户变化；
    // 清掉 isSetUser 后下一次导航会强制重新 bootstrap，不得沿用旧租户的路由表。
    clearAuthorizedSession('tenant-switch')
    expect(h.resetRouterCalls).toBe(1)
    expect(h.permissionResetCalls).toBe(1)
    expect(h.userResetCalls).toBe(1)
  })

  it('tenant-switch：保留访问凭据（登录主体未变，技术账号不得被踢下线）', () => {
    clearAuthorizedSession('tenant-switch')
    expect(h.removeTokenCalls).toBe(0)
  })

  it('tenant-switch：不调用 deleteUserCache（该函数会连带删除 VisitTenantId，抹掉刚设置的访问租户）', () => {
    clearAuthorizedSession('tenant-switch')
    expect(h.deleteUserCacheCalls).toBe(0)
  })

  it('tenant-switch：定向删除 USER / ROLE_ROUTERS 本地缓存，但保留 VisitTenantId', () => {
    clearAuthorizedSession('tenant-switch')
    expect(h.localCacheDeletes).toContain('user')
    expect(h.localCacheDeletes).toContain('roleRouters')
    expect(h.localCacheDeletes).not.toContain('visitTenantId')
  })
})

describe('isAuthorizedSessionClearReason：reason 白名单校验', () => {
  it('只接受三种既定 reason', () => {
    expect(isAuthorizedSessionClearReason('logout')).toBe(true)
    expect(isAuthorizedSessionClearReason('bootstrap-failed')).toBe(true)
    expect(isAuthorizedSessionClearReason('tenant-switch')).toBe(true)
  })
  it('未知 reason 拒绝（避免调用方拼写错误导致静默不清理）', () => {
    expect(isAuthorizedSessionClearReason('revoked')).toBe(false)
    expect(isAuthorizedSessionClearReason('')).toBe(false)
    expect(isAuthorizedSessionClearReason(undefined)).toBe(false)
    expect(isAuthorizedSessionClearReason(null)).toBe(false)
    expect(isAuthorizedSessionClearReason(123 as unknown)).toBe(false)
  })
})
