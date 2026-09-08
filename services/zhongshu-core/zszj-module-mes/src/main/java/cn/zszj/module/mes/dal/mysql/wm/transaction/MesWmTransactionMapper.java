package cn.zszj.module.mes.dal.mysql.wm.transaction;

import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.module.mes.dal.dataobject.wm.transaction.MesWmTransactionDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MES 库存事务流水 Mapper
 */
@Mapper
public interface MesWmTransactionMapper extends BaseMapperX<MesWmTransactionDO> {

}
