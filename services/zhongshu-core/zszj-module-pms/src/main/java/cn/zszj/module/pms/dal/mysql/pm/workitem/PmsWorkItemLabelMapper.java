package cn.zszj.module.pms.dal.mysql.pm.workitem;

import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.module.pms.dal.dataobject.pm.workitem.PmsWorkItemLabelDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PmsWorkItemLabelMapper extends BaseMapperX<PmsWorkItemLabelDO> {

    default List<PmsWorkItemLabelDO> selectListByName(String name) {
        return selectList(new LambdaQueryWrapperX<PmsWorkItemLabelDO>()
                .likeIfPresent(PmsWorkItemLabelDO::getName, name)
                .orderByAsc(PmsWorkItemLabelDO::getCreateTime)
                .orderByAsc(PmsWorkItemLabelDO::getId));
    }

}
