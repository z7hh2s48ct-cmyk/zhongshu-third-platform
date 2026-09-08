package cn.zszj.module.hrm.dal.mysql.salary.config;

import cn.hutool.core.map.MapUtil;
import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.common.util.collection.CollectionUtils;
import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.zszj.module.hrm.controller.admin.salary.vo.group.HrmSalaryGroupPageReqVO;
import cn.zszj.module.hrm.dal.dataobject.salary.config.HrmSalaryGroupDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Mapper
public interface HrmSalaryGroupMapper extends BaseMapperX<HrmSalaryGroupDO> {

    default PageResult<HrmSalaryGroupDO> selectPage(HrmSalaryGroupPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HrmSalaryGroupDO>()
                .likeIfPresent(HrmSalaryGroupDO::getName, reqVO.getName())
                .eqIfPresent(HrmSalaryGroupDO::getTaxRuleId, reqVO.getTaxRuleId())
                .orderByDesc(HrmSalaryGroupDO::getId));
    }

    default HrmSalaryGroupDO selectByName(String name) {
        return selectFirstOne(HrmSalaryGroupDO::getName, name);
    }

    default List<HrmSalaryGroupDO> selectListByIdDesc() {
        return selectList(new LambdaQueryWrapperX<HrmSalaryGroupDO>()
                .orderByDesc(HrmSalaryGroupDO::getId));
    }

    default Map<Long, Long> selectCountMapByTaxRuleIds(Collection<Long> taxRuleIds) {
        List<Map<String, Object>> result = selectMaps(new MPJLambdaWrapperX<HrmSalaryGroupDO>()
                .selectAs(HrmSalaryGroupDO::getTaxRuleId, "taxRuleId")
                .selectCount(HrmSalaryGroupDO::getId, "count")
                .in(HrmSalaryGroupDO::getTaxRuleId, taxRuleIds)
                .groupBy(HrmSalaryGroupDO::getTaxRuleId));
        return CollectionUtils.convertMap(result,
                record -> MapUtil.getLong(record, "taxRuleId"),
                record -> MapUtil.getLong(record, "count"));
    }

}
