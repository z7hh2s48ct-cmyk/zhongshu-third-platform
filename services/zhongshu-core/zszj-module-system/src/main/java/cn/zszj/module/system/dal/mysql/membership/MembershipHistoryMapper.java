package cn.zszj.module.system.dal.mysql.membership;

import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.module.system.dal.dataobject.membership.MembershipHistoryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 任职历史流水 Mapper
 *
 * @author ZS-IAM-002
 */
@Mapper
public interface MembershipHistoryMapper extends BaseMapperX<MembershipHistoryDO> {

    default List<MembershipHistoryDO> selectListByMembershipId(Long membershipId) {
        return selectList(new LambdaQueryWrapperX<MembershipHistoryDO>()
                .eq(MembershipHistoryDO::getMembershipId, membershipId)
                .orderByAsc(MembershipHistoryDO::getId));
    }

}
