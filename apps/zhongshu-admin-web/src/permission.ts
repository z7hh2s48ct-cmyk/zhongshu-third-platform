import router from './router'
import type { RouteLocationNormalized, RouteRecordRaw } from 'vue-router'
import { isRelogin } from '@/config/axios/service'
import { getAccessToken } from '@/utils/auth'
import { useTitle } from '@/hooks/web/useTitle'
import { useNProgress } from '@/hooks/web/useNProgress'
import { usePageLoading } from '@/hooks/web/usePageLoading'
import { useDictStoreWithOut } from '@/store/modules/dict'
import { useUserStoreWithOut } from '@/store/modules/user'
import { usePermissionStoreWithOut } from '@/store/modules/permission'
import { parseRouteLocation } from '@/utils/routeParams'
import { clearAuthorizedSession } from '@/utils/authSession'
import {
  HOME_ROUTE,
  LOGIN_ROUTE,
  NOT_FOUND_ROUTE,
  UNAUTHORIZED_ROUTE,
  isNotFoundMatch,
  resolveBootstrapTarget,
  resolveGuardNavigation,
  sanitizeLoginRedirect
} from '@/router/access'

const { start, done } = useNProgress()

const { loadStart, loadDone } = usePageLoading()

/**
 * ZS-CLIENT-001.A：全局前置守卫。
 *
 * 判定逻辑全部下沉到 `@/router/access` 的纯函数（可被 vitest 直接覆盖），本文件只做编排：
 *   - 未认证：仅登录前白名单放行，其余引导登录页，redirect **已消毒**（防开放重定向）
 *   - 已认证但授权快照未装配：bootstrap（拉取服务端授权 + 重建动态路由）
 *   - 已装配：按服务端本次响应构建的快照做集合成员判断，未授权一律拒绝
 *
 * 路由不重定向白名单不再在此处硬编码，统一由 `PRE_AUTH_ROUTE_PATTERNS` 提供，
 * 避免「白名单」与「授权判定」两处真相源漂移。
 */

/**
 * 未授权落点：区分「路由不存在」（保持既有 404 UX）与「已注册但当前授权快照不允许」（403）。
 * 两者都是拒绝，不构成绕过通道；区分只为不破坏既有 404 体验。
 */
const resolveDenyRoute = (to: RouteLocationNormalized): string => {
  return isNotFoundMatch(to.matched) ? NOT_FOUND_ROUTE : UNAUTHORIZED_ROUTE
}

/**
 * 授权装配：拉取服务端授权快照 → 由本次响应的内存菜单重建动态路由 → addRoute。
 *
 * 装配失败（getInfo 抛错 / 返回空 / 无凭据）时执行 `clearAuthorizedSession('bootstrap-failed')`，
 * 清掉页签、keep-alive 缓存、字典、权限 store、身份 store、动态路由与凭据，
 * 保证卡片「验收④」：登录失败不留下半初始化路由。
 *
 * @returns 是否装配成功
 */
const bootstrapAuthorizedSession = async (): Promise<boolean> => {
  const userStore = useUserStoreWithOut()
  const permissionStore = usePermissionStoreWithOut()
  let userInfo: unknown = null
  isRelogin.show = true
  try {
    userInfo = await userStore.setUserInfoAction()
  } catch (error) {
    console.error('[permission] 授权信息拉取失败，拒绝装配动态路由', error)
    userInfo = null
  } finally {
    isRelogin.show = false
  }
  if (!userInfo) {
    clearAuthorizedSession('bootstrap-failed')
    return false
  }
  // 后端过滤菜单：只用服务端本次响应的内存菜单，本地缓存不参与装配
  await permissionStore.generateRoutes(userStore.getMenus)
  permissionStore.getAddRouters.forEach((route) => {
    router.addRoute(route as unknown as RouteRecordRaw) // 动态添加可访问路由表
  })
  return true
}

// 路由加载前
router.beforeEach(async (to, from, next) => {
  start()
  loadStart()

  const hasToken = !!getAccessToken()
  // 异步加载字典（沿用原时机：持 token 且目的地不是登录页）
  // 另外，间接 issue：https://gitee.com/yudaocode/yudao-ui-admin-vue3/issues/ID9FLI
  if (hasToken && to.path !== LOGIN_ROUTE) {
    const dictStore = useDictStoreWithOut()
    if (!dictStore.getIsSetDict) {
      dictStore.setDictMap().then()
    }
  }

  const userStore = useUserStoreWithOut()
  const navigation = resolveGuardNavigation({
    toPath: to.path,
    toFullPath: to.fullPath,
    redirectQuery: from.query.redirect,
    hasToken,
    isUserResolved: userStore.getIsSetUser,
    snapshot: userStore.getRouteAccess
  })

  if (navigation.type === 'bootstrap') {
    const bootstrapped = await bootstrapAuthorizedSession()
    if (!bootstrapped) {
      // 装配失败：凭据与身份已被清理，回到登录页并保留（消毒后的）原目的地
      next({
        path: LOGIN_ROUTE,
        query: { redirect: sanitizeLoginRedirect(to.fullPath, HOME_ROUTE) },
        replace: true
      })
      return
    }
    // 装配完成后再校验目的地：redirect 与 to.fullPath 都必须落在新快照的授权集合内
    const target = resolveBootstrapTarget({
      toFullPath: to.fullPath,
      redirectQuery: from.query.redirect,
      snapshot: userStore.getRouteAccess
    })
    if (!target.authorized) {
      next({ path: resolveDenyRoute(to), replace: true })
      return
    }
    if (target.preserveToLocation) {
      // 修复跳转时不带参数的问题：保留原始 to 的 params / query / hash
      next({ ...to, replace: true })
      return
    }
    next({ ...parseRouteLocation(target.target), replace: true })
    return
  }

  if (navigation.type === 'unauthorized') {
    next({ path: resolveDenyRoute(to), replace: true })
    return
  }

  if (navigation.type === 'login') {
    // redirect 已由 sanitizeLoginRedirect 消毒：只允许站内路径，杜绝开放重定向
    next({ path: LOGIN_ROUTE, query: { redirect: navigation.redirect }, replace: true })
    return
  }

  if (navigation.type === 'allow-home') {
    next({ path: HOME_ROUTE, replace: true })
    return
  }

  next()
})

router.afterEach((to) => {
  useTitle(to?.meta?.title as string)
  done() // 结束Progress
  loadDone()
})
