import type { App } from 'vue'
import type { RouteRecordRaw } from 'vue-router'
import { createRouter, createWebHistory } from 'vue-router'
import remainingRouter from './modules/remaining'

// 创建路由实例
const router = createRouter({
  history: createWebHistory(import.meta.env.VITE_BASE_PATH), // createWebHashHistory URL带#，createWebHistory URL不带#
  strict: true,
  routes: remainingRouter as RouteRecordRaw[],
  scrollBehavior: () => {
    // 新开标签时、返回标签时，滚动条回到顶部，否则会保留上次标签的滚动位置。
    const scrollbarWrap = document.querySelector('.v-layout-content-scrollbar .el-scrollbar__wrap')
    if (scrollbarWrap) {
      // scrollbarWrap.scrollTo({ left: 0, top: 0, behavior: 'auto' })
      scrollbarWrap.scrollTop = 0
    }
    return { left: 0, top: 0 }
  }
})

// 处理动态导入失败（如重新构建后 chunk 哈希变化），自动跳转到目标页面
router.onError((error, to) => {
  if (
    error.message.includes('Failed to fetch dynamically imported module') ||
    error.message.includes('Importing a module script failed')
  ) {
    window.location.assign(to.fullPath)
  }
})

const collectRouteNames = (routes: AppRouteRecordRaw[], names: Set<string>): void => {
  routes.forEach((route) => {
    if (route?.name) {
      names.add(String(route.name))
    }
    if (Array.isArray(route?.children) && route.children.length > 0) {
      collectRouteNames(route.children, names)
    }
  })
}

/**
 * ZS-CLIENT-001.A：静态路由表（remaining.ts）中登记的全部路由名。
 *
 * `resetRouter` 只卸载**不在该集合内**的路由，即登录后由 `permissionStore.generateRoutes`
 * 动态 `addRoute` 的服务端菜单路由与 404 catch-all（name `404Page`）。
 *
 * 原实现使用固定 5 项白名单（Redirect / RedirectRoot / Login / NoFound / Home），会把
 * `/403`（NoAccess）、`/500`（Error）、`/sso`、`/social-login`、个人中心、`/user/notify-message`、
 * 以及 remaining.ts 中全部 hidden 业务子页一并移除；而这些静态路由**不会**被
 * `generateRoutes` 重新 addRoute，导致「退出 → 再登录（无整页刷新）」后它们永久不可达
 * —— 其中 `/403` 不可达会直接让本卡片的未授权落点失效。
 */
const STATIC_ROUTE_NAMES: Set<string> = (() => {
  const names = new Set<string>()
  collectRouteNames(remainingRouter, names)
  return names
})()

export const resetRouter = (): void => {
  router.getRoutes().forEach((route) => {
    const { name } = route
    if (!name) {
      return
    }
    if (STATIC_ROUTE_NAMES.has(String(name))) {
      // 静态路由表常驻，退出 / 撤权 / 换租户都不卸载
      return
    }
    router.hasRoute(name) && router.removeRoute(name)
  })
}

export const setupRouter = (app: App<Element>) => {
  app.use(router)
}

export default router
