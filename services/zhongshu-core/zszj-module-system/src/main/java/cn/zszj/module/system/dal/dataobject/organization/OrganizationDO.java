package cn.zszj.module.system.dal.dataobject.organization;

import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.tenant.core.db.TenantBaseDO;
import cn.zszj.module.system.enums.organization.OrganizationTypeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 组织 DO
 *
 * <p>ZS-IAM-002（D-09 FND-IAM-002）：组织作为独立业务对象，单表 {@code type} + {@code parent_id}
 * 自引用树承载平台/品牌/加盟商/门店/供应商/部门语义。既有 {@code system_dept} 保持不动，
 * DEPARTMENT 类型组织经 {@link #refDeptId} 桥接回填，向后兼容。
 *
 * @author ZS-IAM-002
 */
@TableName("system_organization")
@KeySequence("system_organization_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class OrganizationDO extends TenantBaseDO {

    public static final Long PARENT_ID_ROOT = 0L;

    /**
     * 组织 ID
     */
    @TableId
    private Long id;
    /**
     * 组织名称
     */
    private String name;
    /**
     * 组织编码（租户内唯一，逻辑删除后可重用）
     */
    private String code;
    /**
     * 组织类型
     *
     * 枚举 {@link OrganizationTypeEnum}
     */
    private Integer type;
    /**
     * 父组织 ID
     *
     * 关联 {@link #id}，根为 {@link #PARENT_ID_ROOT}
     */
    private Long parentId;
    /**
     * 桥接的部门 ID
     *
     * 仅 DEPARTMENT 类型回填 {@code system_dept.id}，其余类型为 null。
     */
    private Long refDeptId;
    /**
     * 显示顺序
     */
    private Integer sort;
    /**
     * 负责人用户 ID
     */
    private Long leaderUserId;
    /**
     * 组织状态
     *
     * 枚举 {@link CommonStatusEnum}
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}
