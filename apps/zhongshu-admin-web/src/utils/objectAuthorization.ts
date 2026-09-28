/**
 * ZS-CLIENT-001.B：对象授权输出消费核心（纯逻辑，无框架依赖，循 access.ts 先例）。
 *
 * 消费合同（服务端 `/system/object-authorization/get`，ZS-PERM-003.A/.B 统一裁决）：
 *  - `allowedActions`：恒非 null 数组（空集=零动作可用）——按钮/操作显隐只消费本集合；
 *  - `authorizedFields`：null=该域未启用字段级输出（零变化，字段照常渲染）；
 *    非 null（可空集）=已启用，字段按三态渲染（hidden / masked / clear）；
 *  - `maskedFields`：authorizedFields 非 null 时恒非 null，⊆ authorizedFields——masked 字段
 *    渲染<b>服务端返回的脱敏值</b>并带脱敏标识（D-12 §5.2），hidden 字段前端必须不渲染
 *    （隐藏字段不得经详情/批量旁路展示）。
 *
 * 职责边界（卡片「调整」）：前端只消费结果、不做任何授权推导；动作执行由服务端
 * checkActionAllowed/checkFieldsAllowed 独立拒绝——「前端伪造动作不生效」为结构保证。
 */

/** 服务端对象授权响应形状（null data = 未接入域） */
export interface ObjectAuthorizationRespVO {
  allowedActions: string[]
  authorizedFields: string[] | null
  maskedFields: string[] | null
}

/** 归一化后的授权快照（null = 未接入域，消费侧 fail-closed） */
export interface ObjectAuthSnapshot {
  allowedActions: string[]
  authorizedFields: string[] | null
  maskedFields: string[] | null
}

/** 字段展示三态 */
export type FieldPresentation = 'hidden' | 'masked' | 'clear'

/** 只保留字符串条目（服务端形状防御的镜像；伪造/脏形状不得进入消费层） */
function toStringArray(value: unknown): string[] {
  if (!Array.isArray(value)) {
    return []
  }
  return value.filter((item): item is string => typeof item === 'string')
}

/**
 * 归一化服务端响应为消费快照。
 *
 * - null / 非对象响应 → null（未接入域，fail-closed）；
 * - `authorizedFields` 的 null 与 [] 语义严格保留（未启用 ≠ 启用但全隐）；
 * - 集合中的非字符串条目被过滤（形状防御）。
 */
export function normalizeObjectAuthorization(resp: unknown): ObjectAuthSnapshot | null {
  if (resp === null || resp === undefined || typeof resp !== 'object' || Array.isArray(resp)) {
    return null
  }
  const raw = resp as Record<string, unknown>
  const authorizedFieldsRaw = raw.authorizedFields
  return {
    allowedActions: toStringArray(raw.allowedActions),
    authorizedFields:
      authorizedFieldsRaw === null || authorizedFieldsRaw === undefined
        ? null
        : toStringArray(authorizedFieldsRaw),
    maskedFields: toStringArray(raw.maskedFields)
  }
}

/**
 * 对象授权缓存键：objectType + orgId + ownerUserId 三维规范化（null/undefined 归一为空段）。
 */
export function buildObjectAuthKey(
  objectType: string,
  orgId?: number | null,
  ownerUserId?: number | null
): string {
  return `${objectType}:${orgId ?? ''}:${ownerUserId ?? ''}`
}

/**
 * 动作消费：null 快照（未接入域）一律 false——不因未接入而放宽；
 * 授权集合精确匹配（大小写敏感）。
 */
export function resolveActionAllowed(
  snapshot: ObjectAuthSnapshot | null,
  action: string
): boolean {
  if (snapshot === null) {
    return false
  }
  return snapshot.allowedActions.includes(action)
}

/**
 * 字段消费三态：
 *  - null 快照 → hidden（未接入域不展示任何业务字段）；
 *  - authorizedFields=null（未启用字段级）→ clear（.A 零变化合同，不得误判全隐）；
 *  - 不在 authorizedFields → hidden（隐藏字段不得经详情旁路展示）；
 *  - 在 authorizedFields 且在 maskedFields → masked（渲染服务端脱敏值 + 脱敏标识）；
 *  - 其余 → clear。
 */
export function resolveFieldPresentation(
  snapshot: ObjectAuthSnapshot | null,
  field: string
): FieldPresentation {
  if (snapshot === null) {
    return 'hidden'
  }
  if (snapshot.authorizedFields === null) {
    return 'clear'
  }
  if (!snapshot.authorizedFields.includes(field)) {
    return 'hidden'
  }
  return snapshot.maskedFields !== null && snapshot.maskedFields.includes(field)
    ? 'masked'
    : 'clear'
}
