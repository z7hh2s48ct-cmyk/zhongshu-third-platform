import type { VisitTarget } from '@/utils/tenant-visit'
import { http } from '@/http/http'

/** 跨组织访问「我的授权目标」响应（对齐服务端 CrossOrgVisitMyTargetsRespVO） */
export interface VisitTargetsResp {
  loginTenantId: number | null
  loginTenantName: string | null
  targets: VisitTarget[]
}

/** 服务端原始响应：admin-api 对具名 Long 字段全局转字符串（精度保护），此处防御式声明 */
interface RawVisitTarget {
  tenantId: number | string
  tenantName: string
  targetOrgIds?: Array<number | string> | null
  validTo?: string | null
}
interface RawVisitTargetsResp {
  loginTenantId?: number | string | null
  loginTenantName?: string | null
  targets?: RawVisitTarget[] | null
}

/**
 * 获取我的跨组织访问授权目标列表（ZS-CLIENT-002.B：获批业务组织导航的唯一数据源）。
 *
 * 服务端授权是唯一真相源：客户端不做授权推导，仅消费服务端结论（最新记录有效 / 目标租户启用 /
 * 组织范围全部有效 / 非登录租户，均由服务端过滤）；id 字段统一 Number() 归一化（Long→String 合同），
 * targets 缺省归一化为空数组（未获批 / 非平台角色）。
 */
export async function getMyVisitTargets(): Promise<VisitTargetsResp> {
  const res = await http.get<RawVisitTargetsResp>('/system/cross-org-visit/my-targets')
  return {
    loginTenantId: res?.loginTenantId != null ? Number(res.loginTenantId) : null,
    loginTenantName: res?.loginTenantName ?? null,
    targets: (res?.targets ?? []).map(target => ({
      tenantId: Number(target.tenantId),
      tenantName: target.tenantName,
      targetOrgIds: Array.isArray(target.targetOrgIds) ? target.targetOrgIds.map(Number) : null,
      validTo: target.validTo ?? null,
    })),
  }
}
