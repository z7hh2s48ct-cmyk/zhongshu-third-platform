package cn.zszj.module.hrm.dal.mysql.employee.info;

import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.module.hrm.dal.dataobject.employee.info.HrmEmployeePersonalNoteDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface HrmEmployeePersonalNoteMapper extends BaseMapperX<HrmEmployeePersonalNoteDO> {

    default List<HrmEmployeePersonalNoteDO> selectListByEmployeeIdAndReminderTimeBetween(
            Long employeeId, LocalDateTime[] reminderTimes) {
        return selectList(new LambdaQueryWrapperX<HrmEmployeePersonalNoteDO>()
                .eq(HrmEmployeePersonalNoteDO::getEmployeeId, employeeId)
                .betweenIfPresent(HrmEmployeePersonalNoteDO::getReminderTime, reminderTimes)
                .orderByAsc(HrmEmployeePersonalNoteDO::getReminderTime)
                .orderByAsc(HrmEmployeePersonalNoteDO::getId));
    }

}
