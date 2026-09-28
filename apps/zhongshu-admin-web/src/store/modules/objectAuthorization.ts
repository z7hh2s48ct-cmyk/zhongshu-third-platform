import { defineStore } from 'pinia'
import { getObjectAuthorization } from '@/api/system/permission'
import { store } from '@/store'
import {
  buildObjectAuthKey,
  normalizeObjectAuthorization,
  type ObjectAuthSnapshot
} from '@/utils/objectAuthorization'

/**
 * ZS-CLIENT-001.B：对象授权快照 store——Web 消费统一「动作 / 字段」授权的唯一缓存面。
 *
 * 唯一写入路径 = 服务端响应（`fetchObjectAuthorization` → normalize）：不提供任何本地
 * 「授予动作 / 字段」的写入 action——「前端伪造动作不生效」的结构保证之一（执行侧另有
 * 服务端 checkActionAllowed/checkFieldsAllowed 独立拒绝，PERM-003.A）。
 *
 * 缓存为内存级（不落 localStorage）：授权快照属会话敏感数据，退出 / 撤权 / 技术租户变化时
 * 由 `clearAuthorizedSession` 单一入口经 `clearObjectAuthorizationState` 清空
 * （「旧页签不泄露」，见 utils/authSession.ts）；刷新 / 新开页后由页面按需重新拉取。
 *
 * 在途竞态防护（R1 P2-1）：清理后完成的在途 fetch **不得回写**新缓存——`clearEpoch` 代数计数，
 * fetch 在 await 前捕获代数、响应返回后代数不一致即丢弃（防止「上一主体的授权快照跨清理存活、
 * 被同窗口下一次登录免请求复用」）。
 *
 * 失败语义（登记）：fetch 被拒绝（网络瞬断 / 对象维不可见 FORBIDDEN）时**不缓存**——瞬态失败
 * 不得固化为「未接入域」（fail-closed 由消费层 null 快照语义天然保证），代价是失败后每次读取
 * 重试并触发全局错误提示；页面接线时若用于列表逐行场景，由调用方自行捕获静默（接线期决策）。
 */
export const useObjectAuthorizationStore = defineStore('objectAuthorization', {
  state: () => ({
    // 键 = buildObjectAuthKey(objectType, orgId, ownerUserId)；值 = 归一化快照（null=未接入域）
    snapshotMap: {} as Record<string, ObjectAuthSnapshot | null>,
    // 清理代数：clearObjectAuthorizationState 自增；在途 fetch 据此放弃回写（R1 P2-1）
    clearEpoch: 0
  }),
  actions: {
    /**
     * 拉取对象授权快照：命中缓存直接返回（含 null 快照的「未接入域」缓存，避免重复请求）；
     * 未命中调服务端唯一入口并归一化后写入。清理代数在 await 前捕获，返回时不一致即丢弃
     * （不回写、不返回旧会话快照，调用方下一次 fetch 将重新请求）。
     */
    async fetchObjectAuthorization(
      objectType: string,
      orgId?: number | string | null,
      ownerUserId?: number | string | null
    ): Promise<ObjectAuthSnapshot | null> {
      const key = buildObjectAuthKey(objectType, orgId, ownerUserId)
      if (key in this.snapshotMap) {
        return this.snapshotMap[key]
      }
      const epoch = this.clearEpoch
      const resp = await getObjectAuthorization({
        objectType,
        orgId: orgId ?? undefined,
        ownerUserId: ownerUserId ?? undefined
      })
      if (epoch !== this.clearEpoch) {
        // 清理发生在请求在途期间：上一会话的授权快照不得落入新缓存（旧页签不泄露）
        return null
      }
      const snapshot = normalizeObjectAuthorization(resp)
      this.snapshotMap[key] = snapshot
      return snapshot
    },
    /**
     * 清空授权快照缓存（同步、幂等）：供 clearAuthorizedSession 单一入口调用，
     * 退出 / 撤权 / 技术租户变化后旧页签不得残留上一主体 / 上一租户的对象级授权。
     */
    clearObjectAuthorizationState() {
      this.snapshotMap = {}
      this.clearEpoch++
    }
  }
})

/** WithOut 变体（authSession 等非组件上下文使用；显式传入全局 pinia，循 dict store 先例） */
export const useObjectAuthorizationStoreWithOut = () => useObjectAuthorizationStore(store)
