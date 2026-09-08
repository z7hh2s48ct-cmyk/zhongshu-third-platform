package cn.zszj.module.fms.dal.mysql.report.balance;

import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.module.fms.dal.dataobject.report.balance.FmsBalanceSheetConfigDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * FMS 资产负债表配置 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface FmsBalanceSheetConfigMapper extends BaseMapperX<FmsBalanceSheetConfigDO> {

    default List<FmsBalanceSheetConfigDO> selectListByAccountSetId(Long accountSetId) {
        return selectList(new LambdaQueryWrapperX<FmsBalanceSheetConfigDO>()
                .eq(FmsBalanceSheetConfigDO::getAccountSetId, accountSetId)
                .orderByAsc(FmsBalanceSheetConfigDO::getSort)
                .orderByAsc(FmsBalanceSheetConfigDO::getId));
    }

}
