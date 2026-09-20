package cn.zszj.module.system.dal.dataobject.membership;

import cn.zszj.framework.tenant.core.db.TenantBaseDO;
import cn.zszj.module.system.enums.membership.MembershipActionEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 任职历史流水 DO
 *
 * <p>ZS-IAM-002（D-09 FND-IAM-003）：入职、转岗、停用、复职、离职、过期、变更均留痕，只增不改，
 * 承载历史归属。转岗记录 from/to 组织，状态流转记录 from/to 状态，操作者由服务端上下文取（非客户端入参）。
 *
 * @author ZS-IAM-002
 */
@TableName("system_membership_history")
@KeySequence("system_membership_history_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class MembershipHistoryDO extends TenantBaseDO {

    /**
     * 历史流水 ID
     */
    @TableId
    private Long id;
    /**
     * 任职 ID
     *
     * 关联 {@link MembershipDO#getId()}
     */
    private Long membershipId;
    /**
     * 账号（用户）ID
     */
    private Long userId;
    /**
     * 动作
     *
     * 枚举 {@link MembershipActionEnum}
     */
    private Integer action;
    /**
     * 变更前组织 ID
     */
    private Long fromOrganizationId;
    /**
     * 变更后组织 ID
     */
    private Long toOrganizationId;
    /**
     * 变更前状态
     */
    private Integer fromStatus;
    /**
     * 变更后状态
     */
    private Integer toStatus;
    /**
     * 操作者用户 ID（服务端上下文取，非客户端入参）
     */
    private Long operatorId;
    /**
     * 变更原因
     */
    private String reason;

}
