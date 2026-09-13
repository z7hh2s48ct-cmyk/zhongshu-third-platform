package cn.zszj.module.system.dal.mysql.user;

import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.module.system.controller.admin.user.vo.user.UserPageReqVO;
import cn.zszj.module.system.dal.dataobject.user.AdminUserDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface AdminUserMapper extends BaseMapperX<AdminUserDO> {

    /**
     * ZS-LOGIN-003 codex r2 P1：按用户编号加行锁读取（SELECT ... FOR UPDATE）。
     * 作为「账号生命周期撤销 ↔ 凭据兑换」的统一锁序最外层：撤销与兑换先取用户行锁，
     * 与账号状态更新（UPDATE 隐式行锁）互斥，杜绝「兑换读到启用态 → 撤销提交 → 仍建新会话」交错。
     *
     * @param id 用户编号
     * @return 用户；不存在（含已被并发删除）时返回 {@code null}（此时不持有该行锁）
     */
    default AdminUserDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<AdminUserDO>()
                .eq(AdminUserDO::getId, id)
                .last("FOR UPDATE"));
    }

    default AdminUserDO selectByUsername(String username) {
        return selectOne(AdminUserDO::getUsername, username);
    }

    default AdminUserDO selectByEmail(String email) {
        return selectOne(AdminUserDO::getEmail, email);
    }

    default AdminUserDO selectByMobile(String mobile) {
        return selectOne(AdminUserDO::getMobile, mobile);
    }

    default PageResult<AdminUserDO> selectPage(UserPageReqVO reqVO, Collection<Long> deptIds, Collection<Long> userIds) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AdminUserDO>()
                .likeIfPresent(AdminUserDO::getUsername, reqVO.getUsername())
                .likeIfPresent(AdminUserDO::getMobile, reqVO.getMobile())
                .eqIfPresent(AdminUserDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(AdminUserDO::getCreateTime, reqVO.getCreateTime())
                .inIfPresent(AdminUserDO::getDeptId, deptIds)
                .inIfPresent(AdminUserDO::getId, userIds)
                .orderByDesc(AdminUserDO::getId));
    }

    default List<AdminUserDO> selectListByNickname(String nickname) {
        return selectList(new LambdaQueryWrapperX<AdminUserDO>().like(AdminUserDO::getNickname, nickname));
    }

    default List<AdminUserDO> selectListByStatus(Integer status) {
        return selectListByStatusAndDeptId(status, null);
    }

    default List<AdminUserDO> selectListByStatusAndDeptId(Integer status, Long deptId) {
        return selectList(new LambdaQueryWrapperX<AdminUserDO>()
                .eq(AdminUserDO::getStatus, status)
                .eqIfPresent(AdminUserDO::getDeptId, deptId));
    }

    default List<AdminUserDO> selectListByDeptIds(Collection<Long> deptIds) {
        return selectList(AdminUserDO::getDeptId, deptIds);
    }

    /**
     * 统计指定部门下的用户数量
     *
     * 用于删除部门前的引用保护：部门下挂有成员时，禁止删除，避免 {@link AdminUserDO#getDeptId()} 悬空
     *
     * @param deptId 部门编号
     * @return 用户数量
     */
    default Long selectCountByDeptId(Long deptId) {
        return selectCount(AdminUserDO::getDeptId, deptId);
    }

}
