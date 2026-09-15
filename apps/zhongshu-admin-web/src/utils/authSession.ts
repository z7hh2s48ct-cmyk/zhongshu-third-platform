import { resetRouter } from '@/router'
import { removeToken } from '@/utils/auth'
import { CACHE_KEY, deleteUserCache, useCache } from '@/hooks/web/useCache'
import { usePermissionStoreWithOut } from '@/store/modules/permission'
import { useUserStoreWithOut } from '@/store/modules/user'
import { useTagsViewStoreWithOut } from '@/store/modules/tagsView'
import { useDictStoreWithOut } from '@/store/modules/dict'

const { wsCache } = useCache()

/**
 * ZS-CLIENT-001.A：授权会话清理合同（单一入口）。
 *
 * 卡片「调整」要求：退出、撤权、技术租户变化时清理**旧路由、缓存、页签和数据**。
 * 卡片「验收③」要求：撤权后旧页签 / 返回缓存不展示旧敏感数据。
 *
 * 前端职责边界（刻意声明，避免越界）：
 *   - 前端只负责「清理与不展示」—— 卸动态路由、清 keep-alive 缓存名单、清页签、清字典快照、
 *     清身份与授权快照、清本地用户缓存与访问凭据；
 *   - 「接口仍独立拒绝」是**服务端 `@PreAuthorize` 的职责**，前端不重算业务权限、
 *     也不承担越权访问的最终防线（见 ZS-CLIENT-001 卡片「调整」与 .B 子项边界）。
 *
 * 撤权后的刷新合同：
 *   会话内不刷新的撤权由服务端独立拒绝；一旦刷新 / 新开页，pinia 内存清空 →
 *   全局守卫强制重新 bootstrap（`getInfo`），授权快照只来自服务端**本次响应**
 *   （见 `src/store/modules/user.ts#setUserInfoAction`），不再回退 localStorage 旧身份。
 */

/**
 * 清理原因。三者的差异：
 *
 * - `logout`           用户主动退出：清页签 / keep-alive / 字典 + 卸动态路由 + 清身份与凭据
 * - `bootstrap-failed` 授权装配失败（登录失败 / getInfo 失败）：同 `logout`，杜绝半初始化路由
 * - `tenant-switch`    技术租户（visitTenantId）变化：清页签 / keep-alive / 字典 + 卸旧路由 +
 *                      清身份与授权快照（强制下一次导航重新 bootstrap）；但**保留凭据**
 *                      （登录主体未变）与 **VisitTenantId**（刚设置的访问租户不得被抹除）
 */
export type AuthorizedSessionClearReason = 'logout' | 'bootstrap-failed' | 'tenant-switch'

const AUTHORIZED_SESSION_CLEAR_REASONS: string[] = ['logout', 'bootstrap-failed', 'tenant-switch']

/** reason 白名单校验：避免调用方拼写错误导致静默不清理 */
export function isAuthorizedSessionClearReason(
  value: unknown
): value is AuthorizedSessionClearReason {
  return typeof value === 'string' && AUTHORIZED_SESSION_CLEAR_REASONS.indexOf(value) !== -1
}

/**
 * 按原因清理授权会话。幂等：重复调用把状态反复清到「空」，不存在只清一次的分支。
 *
 * @param reason 清理原因；未知值（JS 调用方绕过类型约束时）按最严格的 `logout` 处理，
 *               保证「宁可多清，不可漏清」
 */
export function clearAuthorizedSession(reason: AuthorizedSessionClearReason): void {
  const effectiveReason: AuthorizedSessionClearReason = isAuthorizedSessionClearReason(reason)
    ? reason
    : 'logout'

  // 1. 页签 + keep-alive 缓存名单：旧页签与「返回缓存」不再展示上一主体 / 上一租户的数据。
  //    注意不能用 delAllViews() —— 它保留 affix 页签，且不会清空 cachedViews。
  useTagsViewStoreWithOut().clearAuthorizedViews()

  // 2. 字典快照：字典是租户级业务数据，属于卡片所说的「缓存和数据」。
  //    用同步的 clearDictState()，不能用 async resetDict()（后者会立即重新拉取）。
  useDictStoreWithOut().clearDictState()

  // 3. 权限快照 store：清空 addRouters / routers，登录失败不留半初始化路由。
  usePermissionStoreWithOut().$reset()

  // 4. 用户身份与授权快照：清空 roles / permissions / menus / routeAccess。
  //    同时把 isSetUser 置为 false，使下一次导航强制重新 bootstrap（getInfo）——
  //    visit-tenant-id 会随每个请求上送，菜单与授权可能随租户变化，不得沿用旧快照。
  useUserStoreWithOut().resetState()

  // 5. 卸载登录后动态装配的路由（只卸动态部分，静态路由表必须保留，
  //    否则「退出 → 再登录（无整页刷新）」后 /403、/500、个人中心与 hidden 子页永久不可达）。
  resetRouter()

  if (effectiveReason === 'tenant-switch') {
    // 技术租户切换：登录主体未变 → 保留凭据，否则技术账号会被踢下线。
    // 但旧租户的身份缓存必须清掉，且**不得**调用 deleteUserCache() ——
    // 它会连带删除 VisitTenantId，抹掉刚设置的访问租户，因此这里做定向删除。
    wsCache.delete(CACHE_KEY.USER)
    wsCache.delete(CACHE_KEY.ROLE_ROUTERS)
    return
  }

  // 6. 本地用户缓存（USER / ROLE_ROUTERS / VisitTenantId）：杜绝下一次装配复用脏数据。
  deleteUserCache()

  // 7. 访问凭据。放在最后，保证前面各步仍在「已认证」上下文内完成。
  removeToken()
}
