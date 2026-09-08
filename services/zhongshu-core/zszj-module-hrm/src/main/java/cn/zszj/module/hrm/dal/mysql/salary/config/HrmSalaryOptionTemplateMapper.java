package cn.zszj.module.hrm.dal.mysql.salary.config;

import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.module.hrm.dal.dataobject.salary.config.HrmSalaryOptionTemplateDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface HrmSalaryOptionTemplateMapper extends BaseMapperX<HrmSalaryOptionTemplateDO> {

    default List<HrmSalaryOptionTemplateDO> selectListOrderByCode() {
        return selectList(new LambdaQueryWrapperX<HrmSalaryOptionTemplateDO>()
                .orderByAsc(HrmSalaryOptionTemplateDO::getCode));
    }

}
