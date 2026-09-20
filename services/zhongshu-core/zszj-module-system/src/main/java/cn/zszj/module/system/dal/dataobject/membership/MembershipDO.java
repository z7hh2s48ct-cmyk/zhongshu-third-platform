package cn.zszj.module.system.dal.dataobject.membership;

import cn.zszj.framework.tenant.core.db.TenantBaseDO;
import cn.zszj.module.system.enums.membership.MembershipStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 任职 DO
 *
 * <p>ZS-IAM-002（D-09 最小模型 / FND-IAM-001、003）：一个账号（{@link #userId}）可含多个任职，
 * 每任职绑定组织（{@link #organizationId}）、岗位、角色、状态与有效期。用户表仅承载登录身份，
 * 组织归属全部落在本表。每账号至多一条 {@link #isPrimary} = true 的默认任职，作为服务端上下文来源。
 *
 * @author ZS-IAM-002
 */
@TableName(value = "system_membership", autoResultMap = true)
@KeySequence("system_membership_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class MembershipDO extends TenantBaseDO {

    /**
     * 任职 ID
     */
    @TableId
    private Long id;
    /**
     * 账号（用户）ID
     *
     * 关联 {@code system_users.id}，仅承载登录身份
     */
    private Long userId;
    /**
     * 组织 ID
     *
     * 关联 {@code system_organization.id}
     */
    private Long organizationId;
    /**
     * 岗位编号集合
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Set<Long> postIds;
    /**
     * 角色编号集合
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Set<Long> roleIds;
    /**
     * 任职状态
     *
     * 枚举 {@link MembershipStatusEnum}
     */
    private Integer status;
    /**
     * 生效时间
     */
    private LocalDateTime validFrom;
    /**
     * 失效时间（null = 无固定期限）
     */
    private LocalDateTime validTo;
    /**
     * 是否默认任职（1 = 是，服务端上下文取此；0 = 否）
     */
    private Integer isPrimary;

}
