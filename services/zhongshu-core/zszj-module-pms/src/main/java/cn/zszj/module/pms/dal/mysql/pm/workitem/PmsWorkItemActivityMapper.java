package cn.zszj.module.pms.dal.mysql.pm.workitem;

import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.module.pms.dal.dataobject.pm.workitem.PmsWorkItemActivityDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface PmsWorkItemActivityMapper extends BaseMapperX<PmsWorkItemActivityDO> {

    default List<PmsWorkItemActivityDO> selectListByWorkItemId(Long workItemId) {
        return selectList(new LambdaQueryWrapperX<PmsWorkItemActivityDO>()
                .eq(PmsWorkItemActivityDO::getWorkItemId, workItemId)
                .orderByDesc(PmsWorkItemActivityDO::getCreateTime)
                .orderByDesc(PmsWorkItemActivityDO::getId));
    }

    default List<PmsWorkItemActivityDO> selectListByWorkItemIds(Collection<Long> workItemIds, int limit) {
        return selectList(new LambdaQueryWrapperX<PmsWorkItemActivityDO>()
                .in(PmsWorkItemActivityDO::getWorkItemId, workItemIds)
                .orderByDesc(PmsWorkItemActivityDO::getCreateTime)
                .orderByDesc(PmsWorkItemActivityDO::getId)
                .last("LIMIT " + limit));
    }

    default void deleteByWorkItemId(Long workItemId) {
        delete(PmsWorkItemActivityDO::getWorkItemId, workItemId);
    }

    default void deleteByProjectId(Long projectId) {
        delete(PmsWorkItemActivityDO::getProjectId, projectId);
    }

}
