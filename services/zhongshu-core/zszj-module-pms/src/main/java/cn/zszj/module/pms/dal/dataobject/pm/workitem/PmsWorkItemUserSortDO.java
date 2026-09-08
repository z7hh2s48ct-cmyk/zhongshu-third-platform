package cn.zszj.module.pms.dal.dataobject.pm.workitem;

import cn.zszj.framework.mybatis.core.dataobject.BaseDO;
import cn.zszj.module.pms.dal.dataobject.pm.project.PmsProjectDO;
import cn.zszj.module.system.dal.dataobject.user.AdminUserDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * PMS 工作项个人排序 DO
 *
 * @author 芋道源码
 */
@TableName("pms_work_item_user_sort")
@KeySequence("pms_work_item_user_sort_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PmsWorkItemUserSortDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 项目编号
     *
     * 关联 {@link PmsProjectDO#getId()}
     */
    private Long projectId;
    /**
     * 工作项编号
     *
     * 关联 {@link PmsWorkItemDO#getId()}
     */
    private Long workItemId;
    /**
     * 用户编号
     *
     * 关联 {@link AdminUserDO#getId()}
     */
    private Long userId;
    /**
     * 显示顺序
     */
    private Integer sort;

}
