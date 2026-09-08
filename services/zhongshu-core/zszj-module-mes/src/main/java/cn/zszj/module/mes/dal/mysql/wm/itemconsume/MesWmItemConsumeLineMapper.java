package cn.zszj.module.mes.dal.mysql.wm.itemconsume;

import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.module.mes.controller.admin.wm.itemconsume.vo.MesWmItemConsumeLinePageReqVO;
import cn.zszj.module.mes.dal.dataobject.wm.itemconsume.MesWmItemConsumeLineDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES 物料消耗记录行 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface MesWmItemConsumeLineMapper extends BaseMapperX<MesWmItemConsumeLineDO> {

    default PageResult<MesWmItemConsumeLineDO> selectPage(MesWmItemConsumeLinePageReqVO reqVO,
                                                          Long consumeId) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesWmItemConsumeLineDO>()
                .eq(MesWmItemConsumeLineDO::getConsumeId, consumeId)
                .orderByDesc(MesWmItemConsumeLineDO::getId));
    }

    default List<MesWmItemConsumeLineDO> selectListByConsumeId(Long consumeId) {
        return selectList(MesWmItemConsumeLineDO::getConsumeId, consumeId);
    }

}
