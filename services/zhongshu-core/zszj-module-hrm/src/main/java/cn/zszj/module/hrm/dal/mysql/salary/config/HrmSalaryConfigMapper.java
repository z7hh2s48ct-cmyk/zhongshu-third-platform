package cn.zszj.module.hrm.dal.mysql.salary.config;

import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.module.hrm.dal.dataobject.salary.config.HrmSalaryConfigDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HrmSalaryConfigMapper extends BaseMapperX<HrmSalaryConfigDO> {

    default HrmSalaryConfigDO selectFirst() {
        return selectOne(new LambdaQueryWrapperX<HrmSalaryConfigDO>()
                .orderByAsc(HrmSalaryConfigDO::getId)
                .last("LIMIT 1"));
    }

}
