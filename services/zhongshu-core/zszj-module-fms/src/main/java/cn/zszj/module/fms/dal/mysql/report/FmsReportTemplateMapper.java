package cn.zszj.module.fms.dal.mysql.report;

import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.module.fms.dal.dataobject.report.FmsReportTemplateDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * FMS 报表模板 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface FmsReportTemplateMapper extends BaseMapperX<FmsReportTemplateDO> {

    default List<FmsReportTemplateDO> selectListByType(Integer type) {
        return selectList(new LambdaQueryWrapperX<FmsReportTemplateDO>()
                .eq(FmsReportTemplateDO::getType, type)
                .orderByAsc(FmsReportTemplateDO::getSort)
                .orderByAsc(FmsReportTemplateDO::getId));
    }

}
