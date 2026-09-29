package cn.zszj.module.firstchain.dal.mysql.application;

import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.module.firstchain.controller.admin.application.vo.ApplicationPageReqVO;
import cn.zszj.module.firstchain.dal.dataobject.application.ApplicationDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 首链加盟商申请 Mapper（ZS-FC-001，PILOT-REQ-001 查询侧）。
 *
 * <p>租户过滤与逻辑删除由 MyBatis Plus 租户插件 + {@code @TableLogic} 自动施加（接入合同 §1.3：
 * 读写按租户过滤、跨租户 0 行即不可见）；状态写路径不走本 Mapper——权威在 bpm 域层
 * {@code FirstChainStateTransitionExecutor} 的版本条件更新。
 *
 * @author ZS-FC-001
 */
@Mapper
public interface FirstchainApplicationMapper extends BaseMapperX<ApplicationDO> {

    default PageResult<ApplicationDO> selectPage(ApplicationPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ApplicationDO>()
                .eqIfPresent(ApplicationDO::getAppKey, reqVO.getAppKey())
                .likeIfPresent(ApplicationDO::getApplicantName, reqVO.getApplicantName())
                .likeIfPresent(ApplicationDO::getContactName, reqVO.getContactName())
                .eqIfPresent(ApplicationDO::getStatus, reqVO.getStatus())
                .orderByDesc(ApplicationDO::getId));
    }

    default ApplicationDO selectByAppKey(String appKey) {
        return selectOne(ApplicationDO::getAppKey, appKey);
    }

}
