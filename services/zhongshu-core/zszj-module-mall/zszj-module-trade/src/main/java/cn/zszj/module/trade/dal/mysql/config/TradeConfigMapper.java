package cn.zszj.module.trade.dal.mysql.config;

import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.module.trade.dal.dataobject.config.TradeConfigDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 交易中心配置 Mapper
 *
 * @author owen
 */
@Mapper
public interface TradeConfigMapper extends BaseMapperX<TradeConfigDO> {

}
