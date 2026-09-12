package cn.zszj.module.system.dal.mysql.dept;

import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.module.system.controller.admin.dept.vo.dept.DeptListReqVO;
import cn.zszj.module.system.dal.dataobject.dept.DeptDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface DeptMapper extends BaseMapperX<DeptDO> {

    default List<DeptDO> selectList(DeptListReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<DeptDO>()
                .likeIfPresent(DeptDO::getName, reqVO.getName())
                .eqIfPresent(DeptDO::getStatus, reqVO.getStatus()));
    }

    default DeptDO selectByParentIdAndName(Long parentId, String name) {
        return selectOne(DeptDO::getParentId, parentId, DeptDO::getName, name);
    }

    default Long selectCountByParentId(Long parentId) {
        return selectCount(DeptDO::getParentId, parentId);
    }

    /**
     * 统计以指定用户为负责人的部门数量
     *
     * 用于删除用户前的引用保护：用户是部门负责人时，禁止删除，避免 {@link DeptDO#getLeaderUserId()} 悬空
     *
     * @param leaderUserId 负责人用户编号
     * @return 部门数量
     */
    default Long selectCountByLeaderUserId(Long leaderUserId) {
        return selectCount(DeptDO::getLeaderUserId, leaderUserId);
    }

    default List<DeptDO> selectListByParentId(Collection<Long> parentIds) {
        return selectList(DeptDO::getParentId, parentIds);
    }

    default List<DeptDO> selectListByLeaderUserId(Long id) {
        return selectList(DeptDO::getLeaderUserId, id);
    }

}
