package cn.zszj.module.hrm.dal.mysql.employee.experience;

import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.module.hrm.dal.dataobject.employee.experience.HrmEmployeeWorkExperienceDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface HrmEmployeeWorkExperienceMapper extends BaseMapperX<HrmEmployeeWorkExperienceDO> {

    default List<HrmEmployeeWorkExperienceDO> selectListByEmployeeId(Long employeeId) {
        return selectList(new LambdaQueryWrapperX<HrmEmployeeWorkExperienceDO>()
                .eq(HrmEmployeeWorkExperienceDO::getEmployeeId, employeeId)
                .orderByAsc(HrmEmployeeWorkExperienceDO::getSort)
                .orderByDesc(HrmEmployeeWorkExperienceDO::getId));
    }

}
