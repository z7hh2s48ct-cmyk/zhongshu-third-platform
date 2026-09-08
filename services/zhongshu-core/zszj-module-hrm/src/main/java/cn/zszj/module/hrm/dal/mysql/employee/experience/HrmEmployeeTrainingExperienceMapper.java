package cn.zszj.module.hrm.dal.mysql.employee.experience;

import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.module.hrm.dal.dataobject.employee.experience.HrmEmployeeTrainingExperienceDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface HrmEmployeeTrainingExperienceMapper extends BaseMapperX<HrmEmployeeTrainingExperienceDO> {

    default List<HrmEmployeeTrainingExperienceDO> selectListByEmployeeId(Long employeeId) {
        return selectList(new LambdaQueryWrapperX<HrmEmployeeTrainingExperienceDO>()
                .eq(HrmEmployeeTrainingExperienceDO::getEmployeeId, employeeId)
                .orderByAsc(HrmEmployeeTrainingExperienceDO::getSort)
                .orderByDesc(HrmEmployeeTrainingExperienceDO::getId));
    }

}
