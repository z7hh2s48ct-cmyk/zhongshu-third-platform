package cn.zszj.module.hrm.dal.mysql.salary.config;

import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.module.hrm.dal.dataobject.salary.config.HrmSalaryChangeTemplateDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface HrmSalaryChangeTemplateMapper extends BaseMapperX<HrmSalaryChangeTemplateDO> {

    default List<HrmSalaryChangeTemplateDO> selectListByIdDesc() {
        return selectList(new LambdaQueryWrapperX<HrmSalaryChangeTemplateDO>()
                .orderByDesc(HrmSalaryChangeTemplateDO::getId));
    }

    default List<HrmSalaryChangeTemplateDO> selectListByDefaultStatus(Boolean defaultStatus) {
        return selectList(HrmSalaryChangeTemplateDO::getDefaultStatus, defaultStatus);
    }

}
