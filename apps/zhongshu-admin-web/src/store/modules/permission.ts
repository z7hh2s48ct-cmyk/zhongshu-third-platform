import { defineStore } from 'pinia'
import { store } from '@/store'
import { cloneDeep } from 'lodash-es'
import remainingRouter from '@/router/modules/remaining'
import { flatMultiLevelRoutes, generateRoute } from '@/utils/routerHelper'

export interface PermissionState {
  routers: AppRouteRecordRaw[]
  addRouters: AppRouteRecordRaw[]
  menuTabRouters: AppRouteRecordRaw[]
  menuRootPath: string
}

export const usePermissionStore = defineStore('permission', {
  state: (): PermissionState => ({
    routers: [],
    addRouters: [],
    menuTabRouters: [],
    menuRootPath: ''
  }),
  getters: {
    getRouters(): AppRouteRecordRaw[] {
      return this.routers
    },
    getAddRouters(): AppRouteRecordRaw[] {
      return flatMultiLevelRoutes(cloneDeep(this.addRouters))
    },
    getMenuTabRouters(): AppRouteRecordRaw[] {
      return this.menuTabRouters
    },
    getMenuRootPath(): string {
      return this.menuRootPath
    }
  },
  actions: {
    /**
     * 生成动态路由。
     *
     * ZS-CLIENT-001.A 关键收敛：菜单**只接受调用方传入的内存值**（服务端本次响应），
     * 不再从 `CACHE_KEY.ROLE_ROUTERS` 本地缓存回读。原实现以缓存为来源，使
     * 「失效缓存 / 伪造前端角色」可以凭空生成路由（卡片「验收②」），且撤权后
     * 旧菜单仍会被重新装配（卡片「验收③」）。
     *
     * @param menus 服务端下发的菜单树（`userStore.getMenus`）。
     *              缺省 / 非数组一律按空处理 —— 宁可生成空路由表（只剩 404 兜底），
     *              也绝不回退本地缓存。
     */
    async generateRoutes(menus?: AppCustomRouteRecordRaw[] | null): Promise<unknown> {
      return new Promise<void>((resolve) => {
        const res: AppCustomRouteRecordRaw[] = Array.isArray(menus) ? menus : []
        const routerMap: AppRouteRecordRaw[] = generateRoute(res)
        // 动态路由，404一定要放到最后面
        // preschooler：vue-router@4以后已支持静态404路由，此处可不再追加
        this.addRouters = routerMap.concat([
          {
            path: '/:path(.*)*',
            // redirect: '/404',
            component: () => import('@/views/Error/404.vue'),
            name: '404Page',
            meta: {
              hidden: true,
              breadcrumb: false
            }
          }
        ])
        // 渲染菜单的所有路由
        this.routers = cloneDeep(remainingRouter).concat(routerMap)
        resolve()
      })
    },
    setMenuTabRouters(routers: AppRouteRecordRaw[]): void {
      this.menuTabRouters = routers
    },
    setMenuRootPath(path: string): void {
      this.menuRootPath = path
    }
  },
  persist: false
})

export const usePermissionStoreWithOut = () => {
  return usePermissionStore(store)
}
