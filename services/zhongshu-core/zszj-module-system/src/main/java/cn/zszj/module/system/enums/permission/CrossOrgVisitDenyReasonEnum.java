package cn.zszj.module.system.enums.permission;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 跨组织访问授权判定理由枚举（ZS-SEC-001.B）。
 *
 * <p>{@code CrossOrgVisitService#authorizeVisit} 的判定序对每次裁决给出唯一理由：{@link #AUTHORIZED} 表示获批，
 * 其余均为拒绝原因（fail-closed）。枚举名（{@link #name()}）作为 {@code CrossOrgVisitDecisionDTO#reason} 落库/回传，
 * {@link #message} 用于审计 reason 与拦截器 403 文案（不含敏感明文）。逐条锚定 docs/05 line330-331 与 §16.1 line1014：
 *
 * <ul>
 *   <li>{@link #NO_GRANT}：无授权记录即拒——<b>取代</b>旧 {@code system:tenant:visit} 粗粒度放大（不再因持旧权限跳过全部功能权限）；</li>
 *   <li>{@link #GRANT_EXPIRED}/{@link #GRANT_REVOKED}：过期/撤销拒绝（有效期 + 状态维度）；</li>
 *   <li>{@link #NOT_PLATFORM_ROLE}：须 D-09（跨组织只允许显式平台角色）；</li>
 *   <li>{@link #TARGET_INVALID}/{@link #TARGET_ORG_INVALID}：停用/无效目标拒绝；</li>
 *   <li>{@link #ACTION_NOT_ALLOWED}/{@link #OBJECT_OUT_OF_SCOPE}/{@link #FIELD_NOT_ALLOWED}：
 *       即使具备访问入口权限，也不能自动获得所有目标动作/对象/敏感字段（错动作/错对象/错字段拒绝）。</li>
 * </ul>
 *
 * @author ZS-SEC-001.B
 */
@Getter
@AllArgsConstructor
public enum CrossOrgVisitDenyReasonEnum {

    /**
     * 授权有效，获批跨组织访问（非拒绝；判定序全部通过）
     */
    AUTHORIZED("授权有效，获批跨组织访问"),
    /**
     * 请求参数非法（缺 visitorUserId/targetTenantId，或目标即原租户等无意义请求）
     */
    INVALID_REQUEST("跨组织访问请求参数非法"),
    /**
     * 无有效跨组织授权记录（取代旧粗粒度权限：无记录即拒，不放大）
     */
    NO_GRANT("无有效跨组织授权记录"),
    /**
     * 授权记录已撤销
     */
    GRANT_REVOKED("跨组织授权已撤销"),
    /**
     * 授权不在有效期内（valid_from 未到或 valid_to 已过）
     */
    GRANT_EXPIRED("跨组织授权不在有效期内"),
    /**
     * 访问者非显式平台角色（D-09 一期跨组织仅限平台角色）
     */
    NOT_PLATFORM_ROLE("访问者非显式平台角色，禁止跨组织访问"),
    /**
     * 目标租户不存在或已停用
     */
    TARGET_INVALID("目标租户不存在或已停用"),
    /**
     * 目标组织不存在或已停用
     */
    TARGET_ORG_INVALID("目标组织不存在或已停用"),
    /**
     * 请求动作超出授权动作范围
     */
    ACTION_NOT_ALLOWED("请求动作超出跨组织授权范围"),
    /**
     * 请求对象所属组织超出授权组织范围
     */
    OBJECT_OUT_OF_SCOPE("请求对象超出跨组织授权组织范围"),
    /**
     * 请求字段超出授权字段范围
     */
    FIELD_NOT_ALLOWED("请求字段超出跨组织授权字段范围");

    /**
     * 判定理由说明（审计 reason / 403 文案，不含敏感明文）
     */
    private final String message;

    public boolean isAuthorized() {
        return this == AUTHORIZED;
    }

}
