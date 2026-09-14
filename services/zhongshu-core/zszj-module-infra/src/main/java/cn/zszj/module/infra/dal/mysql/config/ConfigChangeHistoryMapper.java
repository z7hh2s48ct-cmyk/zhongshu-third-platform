package cn.zszj.module.infra.dal.mysql.config;

import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.module.infra.controller.admin.config.vo.ConfigChangeHistoryPageReqVO;
import cn.zszj.module.infra.dal.dataobject.config.ConfigChangeHistoryDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 参数配置变更历史 Mapper（ZS-CFG-004 B04）
 */
@Mapper
public interface ConfigChangeHistoryMapper extends BaseMapperX<ConfigChangeHistoryDO> {

    default PageResult<ConfigChangeHistoryDO> selectPageByConfigId(ConfigChangeHistoryPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ConfigChangeHistoryDO>()
                .eq(ConfigChangeHistoryDO::getConfigId, reqVO.getConfigId())
                .orderByDesc(ConfigChangeHistoryDO::getId));
    }

}
