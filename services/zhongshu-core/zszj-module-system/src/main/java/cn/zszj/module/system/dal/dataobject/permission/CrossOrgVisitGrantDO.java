package cn.zszj.module.system.dal.dataobject.permission;

import cn.zszj.framework.mybatis.core.dataobject.BaseDO;
import cn.zszj.framework.mybatis.core.type.LongListTypeHandler;
import cn.zszj.framework.mybatis.core.type.StringListTypeHandler;
import cn.zszj.framework.tenant.core.aop.TenantIgnore;
import cn.zszj.module.system.enums.permission.CrossOrgVisitStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 跨组织访问授权记录 DO（ZS-SEC-001.B）。
 *
 * <p>以<b>服务端授权记录/策略</b>取代 ZS-SEC-001.A 关闭的旧越权放大链：获批跨组织访问由本表逐条限定
 * <b>目标租户、对象（组织）、动作、字段与有效期</b>，落实 docs/05 line330「获批后以服务端授权记录/策略限制
 * 目标租户、对象、动作、字段和有效期」。跨原主体租户与目标租户，故为平台级跨租户表
 * （{@code extends BaseDO}（无 tenant_id）+ {@link TenantIgnore}，循 {@code TenantDO} 范式）。
 *
 * <p>{@link #visitorTenantId}（原主体 home 租户）与 {@link #targetTenantId}（目标租户）为业务列，
 * 非 MP 租户插件的技术租户隔离列——本表查询一律 {@code @TenantIgnore}，不受当前上下文租户过滤。
 *
 * @author ZS-SEC-001.B
 */
@TableName(value = "system_cross_org_visit_grant", autoResultMap = true)
@KeySequence("system_cross_org_visit_grant_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TenantIgnore
public class CrossOrgVisitGrantDO extends BaseDO {

    /**
     * 授权记录编号
     */
    @TableId
    private Long id;
    /**
     * 原主体：发起跨组织访问的账号（用户）编号
     */
    private Long visitorUserId;
    /**
     * 原主体所属（home）技术租户编号
     */
    private Long visitorTenantId;
    /**
     * 目标租户编号
     */
    private Long targetTenantId;
    /**
     * 授权可见的目标组织编号集合（逗号分隔存储，{@link LongListTypeHandler}）；
     * {@code null} = 目标租户内全部组织（whole-tenant 授权）
     */
    @TableField(typeHandler = LongListTypeHandler.class)
    private List<Long> targetOrgIds;
    /**
     * 授权允许的动作（权限标识）集合（逗号分隔存储，{@link StringListTypeHandler}）；
     * <b>空/null = fail-closed，不允许任何动作</b>（不能自动获得所有目标动作）
     */
    @TableField(typeHandler = StringListTypeHandler.class)
    private List<String> allowedActions;
    /**
     * 授权允许访问的字段集合（逗号分隔存储，{@link StringListTypeHandler}）；
     * {@code null} = 不允许任何显式敏感字段（字段目录归 ZS-PERM-003，本卡交付判定机制）
     */
    @TableField(typeHandler = StringListTypeHandler.class)
    private List<String> allowedFields;
    /**
     * 生效时间
     */
    private LocalDateTime validFrom;
    /**
     * 失效时间（{@code null} = 无固定期限）
     */
    private LocalDateTime validTo;
    /**
     * 授权状态
     *
     * 枚举 {@link CrossOrgVisitStatusEnum}（0 生效 / 1 已撤销）
     */
    private Integer status;
    /**
     * 授权/撤销理由（不含敏感明文）
     */
    private String reason;

}
