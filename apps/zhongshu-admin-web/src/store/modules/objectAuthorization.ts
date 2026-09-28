import { defineStore } from 'pinia'
import { getObjectAuthorization } from '@/api/system/permission'
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
 */
export const useObjectAuthorizationStore = defineStore('objectAuthorization', {
  state: () => ({
    // 键 = buildObjectAuthKey(objectType, orgId, ownerUserId)；值 = 归一化快照（null=未接入域）
    snapshotMap: {} as Record<string, ObjectAuthSnapshot | null>
  }),
  actions: {
    /**
     * 拉取对象授权快照：命中缓存直接返回（含 null 快照的「未接入域」缓存，避免重复请求）；
     * 未命中调服务端唯一入口并归一化后写入。
     */
    async fetchObjectAuthorization(
      objectType: string,
      orgId?: number | null,
      ownerUserId?: number | null
    ): Promise<ObjectAuthSnapshot | null> {
      const key = buildObjectAuthKey(objectType, orgId, ownerUserId)
      if (key in this.snapshotMap) {
        return this.snapshotMap[key]
      }
      const resp = await getObjectAuthorization({
        objectType,
        orgId: orgId ?? undefined,
        ownerUserId: ownerUserId ?? undefined
      })
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
    }
  }
})

/** WithOut 变体（authSession 等非组件上下文使用，循 dict store 先例） */
export const useObjectAuthorizationStoreWithOut = () => useObjectAuthorizationStore()
