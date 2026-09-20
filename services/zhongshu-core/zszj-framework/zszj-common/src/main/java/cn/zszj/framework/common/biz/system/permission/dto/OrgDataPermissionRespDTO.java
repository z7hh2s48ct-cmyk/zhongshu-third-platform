package cn.zszj.framework.common.biz.system.permission.dto;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * 组织（跨组织）的数据权限 Response DTO
 *
 * <p>ZS-PERM-002.B（D-09 FND-AUTH-003/004）：承载 <b>org 轴</b>授权范围，与 {@link DeptDataPermissionRespDTO}
 * 的 dept/self 轴正交并存。由 module-system 的组织范围解析产出、经
 * {@code PermissionCommonApi#getOrgDataPermission} 暴露给框架级 {@code OrgDataPermissionChecker} 消费。
 *
 * <p>字段语义（与 dept 轴同构，便于检查器复用判定骨架）：
 * <ul>
 *     <li>{@link #all}=true：可见租户内全部组织（超级管理员，或 D-09「显式平台角色」——PLATFORM 类型组织任职）；</li>
 *     <li>{@link #orgIds}：授权可见的组织编号集合（非平台任职 = 本人各在职组织 + 其组织树后代）；</li>
 *     <li>{@link #self}=true：对象负责人为登录用户本人即可见（无组织列对象的兜底，避免本人对象因 orgId=null 被判不可见）；</li>
 *     <li>{@link #scopeType}：范围类型（{@code OrgDataScopeEnum} 的值，仅用于审计/授权矩阵登记，检查器判定不依赖）。</li>
 * </ul>
 *
 * <p>不携带跨租户语义：tenant_id 隔离轴由租户拦截器与 ZS-DB-018 覆盖，本 DTO 只在同一技术租户内表达 org 范围。
 *
 * @author ZS-PERM-002.B
 */
@Data
public class OrgDataPermissionRespDTO {

    /**
     * 是否可查看全部组织数据（超管 / 显式平台角色）
     */
    private Boolean all;
    /**
     * 是否可查看自己负责的对象（无组织列对象兜底）
     */
    private Boolean self;
    /**
     * 可查看的组织编号集合
     */
    private Set<Long> orgIds;
    /**
     * 授权范围类型（关联 module-system {@code OrgDataScopeEnum}，仅审计/矩阵用；框架层不解释其枚举语义）
     */
    private Integer scopeType;

    public OrgDataPermissionRespDTO() {
        this.all = false;
        this.self = false;
        this.orgIds = new HashSet<>();
    }

}
