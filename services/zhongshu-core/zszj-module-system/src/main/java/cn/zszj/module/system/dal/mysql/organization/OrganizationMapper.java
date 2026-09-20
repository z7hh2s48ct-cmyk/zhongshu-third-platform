package cn.zszj.module.system.dal.mysql.organization;

import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.module.system.dal.dataobject.organization.OrganizationDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 组织 Mapper
 *
 * @author ZS-IAM-002
 */
@Mapper
public interface OrganizationMapper extends BaseMapperX<OrganizationDO> {

    default OrganizationDO selectByCode(String code) {
        return selectOne(OrganizationDO::getCode, code);
    }

    /**
     * 按组织编号加行锁读取（SELECT ... FOR UPDATE，复用 {@code AdminUserMapper.selectByIdForUpdate} 范式）。
     *
     * <p>ZS-IAM-002（codex r0 P2）：重挂父级前对涉及行加锁，与成环校验同处一个受保护事务，
     * 串行化冲突的层级变更，杜绝两并发重挂都读原始树、双双通过校验后留环的 write-skew。
     * 需在 {@code @Transactional} 内调用。
     *
     * @param id 组织编号
     * @return 组织；不存在返回 {@code null}（此时不持有行锁）
     */
    default OrganizationDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<OrganizationDO>()
                .eq(OrganizationDO::getId, id)
                .last("FOR UPDATE"));
    }

    default List<OrganizationDO> selectListByParentId(Long parentId) {
        return selectList(OrganizationDO::getParentId, parentId);
    }

    default Long selectCountByParentId(Long parentId) {
        return selectCount(OrganizationDO::getParentId, parentId);
    }

    default List<OrganizationDO> selectListByType(Integer type) {
        return selectList(new LambdaQueryWrapperX<OrganizationDO>()
                .eqIfPresent(OrganizationDO::getType, type)
                .orderByAsc(OrganizationDO::getSort));
    }

}
