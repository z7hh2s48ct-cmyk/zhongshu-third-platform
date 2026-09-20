package cn.zszj.module.system.dal.mysql.membership;

import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.module.system.dal.dataobject.membership.MembershipDO;
import cn.zszj.module.system.enums.membership.MembershipStatusEnum;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 任职 Mapper
 *
 * @author ZS-IAM-002
 */
@Mapper
public interface MembershipMapper extends BaseMapperX<MembershipDO> {

    default MembershipDO selectPrimaryByUserId(Long userId) {
        return selectOne(new LambdaQueryWrapperX<MembershipDO>()
                .eq(MembershipDO::getUserId, userId)
                .eq(MembershipDO::getIsPrimary, 1));
    }

    /**
     * 按任职编号加行锁读取（SELECT ... FOR UPDATE，复用 {@code AdminUserMapper.selectByIdForUpdate} 范式）。
     *
     * <p>ZS-IAM-002（codex r0 P2）：状态流转/转岗先锁行再读审计态（fromStatus/fromOrganization），
     * 串行化并发变更，杜绝两个事务读到同一原始行导致 history 记错起始态。需在 {@code @Transactional} 内调用。
     *
     * @param id 任职编号
     * @return 任职；不存在返回 {@code null}（此时不持有行锁）
     */
    default MembershipDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<MembershipDO>()
                .eq(MembershipDO::getId, id)
                .last("FOR UPDATE"));
    }

    default List<MembershipDO> selectListByUserId(Long userId) {
        return selectList(MembershipDO::getUserId, userId);
    }

    /**
     * 查询账号在指定组织的「在任」任职（ACTIVE/SUSPENDED）——与部分唯一索引 uk_system_membership_user_org
     * （{@code WHERE deleted = 0 AND status IN (1,2)}）同口径。
     *
     * <p>ZS-IAM-002（codex r0 P2）：去重查询若含 EXPIRED/TERMINATED 历史行，会误拒「仅存历史任职」组织的再入职，
     * 且历史行与在任行并存时 {@code selectOne} 会因多结果抛错。故按索引谓词过滤到 ACTIVE/SUSPENDED。
     */
    default MembershipDO selectInServiceByUserIdAndOrganizationId(Long userId, Long organizationId) {
        return selectOne(new LambdaQueryWrapperX<MembershipDO>()
                .eq(MembershipDO::getUserId, userId)
                .eq(MembershipDO::getOrganizationId, organizationId)
                .in(MembershipDO::getStatus,
                        MembershipStatusEnum.ACTIVE.getStatus(), MembershipStatusEnum.SUSPENDED.getStatus()));
    }

    /**
     * 降级账号既有默认任职（is_primary 1→0），保留历史行不物理删除。
     *
     * <p>ZS-IAM-002（codex r0 P2）：显式主职替换的原子前置——释放 uk_system_membership_primary 占位，
     * 使新任职可承接默认身份；旧默认任职（含已离任）行保留承载历史归属，仅摘除 primary 标志。
     *
     * @return 受影响行数
     */
    default int clearPrimaryByUserId(Long userId) {
        return update(null, new LambdaUpdateWrapper<MembershipDO>()
                .eq(MembershipDO::getUserId, userId)
                .eq(MembershipDO::getIsPrimary, 1)
                .set(MembershipDO::getIsPrimary, 0));
    }

    default Long selectCountByOrganizationId(Long organizationId) {
        return selectCount(MembershipDO::getOrganizationId, organizationId);
    }

}
