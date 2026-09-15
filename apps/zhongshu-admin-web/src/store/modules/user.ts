import { store } from '@/store'
import { defineStore } from 'pinia'
import { getAccessToken, removeToken } from '@/utils/auth'
import { CACHE_KEY, useCache, deleteUserCache } from '@/hooks/web/useCache'
import { getInfo, loginOut } from '@/api/login'
import { buildRouteAccessSnapshot, emptyRouteAccessSnapshot } from '@/router/access'
import type { RouteAccessSnapshot } from '@/router/access'

const { wsCache } = useCache()

interface UserVO {
  id: number
  avatar: string
  nickname: string
  deptId: number
}

interface UserInfoVO {
  // USER 缓存
  permissions: Set<string>
  roles: string[]
  isSetUser: boolean
  user: UserVO
  /**
   * ZS-CLIENT-001.A：服务端本次响应下发的菜单树（内存值）。
   * 它是动态路由装配与授权判定的**唯一真相源**，不再从 localStorage 回读。
   */
  menus: AppCustomRouteRecordRaw[]
  /** ZS-CLIENT-001.A：由 menus 构建的授权快照，供全局守卫做集合成员判断 */
  routeAccess: RouteAccessSnapshot
}

export const useUserStore = defineStore('admin-user', {
  state: (): UserInfoVO => ({
    permissions: new Set<string>(),
    roles: [],
    isSetUser: false,
    user: {
      id: 0,
      avatar: '',
      nickname: '',
      deptId: 0
    },
    menus: [],
    routeAccess: emptyRouteAccessSnapshot()
  }),
  getters: {
    getPermissions(): Set<string> {
      return this.permissions
    },
    getRoles(): string[] {
      return this.roles
    },
    getIsSetUser(): boolean {
      return this.isSetUser
    },
    getUser(): UserVO {
      return this.user
    },
    getMenus(): AppCustomRouteRecordRaw[] {
      return this.menus
    },
    getRouteAccess(): RouteAccessSnapshot {
      return this.routeAccess
    }
  },
  actions: {
    /**
     * 拉取服务端授权信息并构建本次会话的授权快照。
     *
     * ZS-CLIENT-001.A 关键收敛（卡片「验收②」：失效缓存、伪造前端角色不能操作）：
     * 原实现在 localStorage 存在 USER 缓存时，即使 `getInfo()` 失败也**静默沿用缓存身份**
     * （注释原文：「即使加载失败，也不影响后续的操作，保证可以进入系统」），使得
     * 篡改 / 过期的本地缓存可以继续操作。现改为：授权快照只接受服务端**本次响应**，
     * 任何失败或空响应都清空身份与快照、删除用户缓存并返回 null，由守卫拒绝装配路由
     * （卡片「验收④」：登录失败不留下半初始化路由）。
     *
     * @returns 服务端授权信息；装配失败返回 null
     */
    async setUserInfoAction() {
      if (!getAccessToken()) {
        this.resetState()
        return null
      }
      let userInfo: any
      try {
        userInfo = await getInfo()
      } catch (error) {
        // 服务端不可达 / 凭据失效：不得回退本地缓存身份
        deleteUserCache()
        this.resetState()
        return null
      }
      if (!userInfo) {
        deleteUserCache()
        this.resetState()
        return null
      }
      this.permissions = new Set(userInfo.permissions || []) // 兜底为 [] https://t.zsxq.com/xCJew
      this.roles = userInfo.roles || []
      this.user = userInfo.user
      this.menus = Array.isArray(userInfo.menus) ? userInfo.menus : []
      // 前端只做「服务端下发菜单」的集合成员判断，不重算业务权限（.B 子项才消费动作/字段授权）
      this.routeAccess = buildRouteAccessSnapshot(this.menus)
      this.isSetUser = true
      wsCache.set(CACHE_KEY.USER, userInfo)
      // 注意：不再写入 CACHE_KEY.ROLE_ROUTERS —— 缓存不是授权来源，
      // 动态路由改由本次响应的内存菜单生成（见 store/modules/permission.ts#generateRoutes）
      return userInfo
    },
    async setUserAvatarAction(avatar: string) {
      const userInfo = wsCache.get(CACHE_KEY.USER)
      // NOTE: 是否需要像`setUserInfoAction`一样判断`userInfo != null`
      this.user.avatar = avatar
      userInfo.user.avatar = avatar
      wsCache.set(CACHE_KEY.USER, userInfo)
    },
    async setUserNicknameAction(nickname: string) {
      const userInfo = wsCache.get(CACHE_KEY.USER)
      // NOTE: 是否需要像`setUserInfoAction`一样判断`userInfo != null`
      this.user.nickname = nickname
      userInfo.user.nickname = nickname
      wsCache.set(CACHE_KEY.USER, userInfo)
    },
    async loginOut() {
      await loginOut()
      removeToken()
      deleteUserCache() // 删除用户缓存
      this.resetState()
    },
    resetState() {
      this.permissions = new Set<string>()
      this.roles = []
      this.isSetUser = false
      this.user = {
        id: 0,
        avatar: '',
        nickname: '',
        deptId: 0
      }
      // 授权快照与菜单必须一并清空：否则撤权 / 退出后旧判定依据仍被复用（卡片「验收③」）
      this.menus = []
      this.routeAccess = emptyRouteAccessSnapshot()
    }
  }
})

export const useUserStoreWithOut = () => {
  return useUserStore(store)
}
