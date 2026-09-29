package cn.zszj.module.firstchain.service.application;

import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.module.bpm.firstchain.FirstChainApplicationService;
import cn.zszj.module.firstchain.controller.admin.application.vo.ApplicationCreateReqVO;
import cn.zszj.module.firstchain.controller.admin.application.vo.ApplicationPageReqVO;
import cn.zszj.module.firstchain.controller.admin.application.vo.ApplicationRespVO;
import cn.zszj.module.firstchain.controller.admin.application.vo.ApplicationSubmitReqVO;
import cn.zszj.module.firstchain.controller.admin.application.vo.ApplicationWithdrawReqVO;
import cn.zszj.module.firstchain.dal.mysql.application.FirstchainApplicationMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 首链申请域应用服务（ZS-FC-001，PILOT-REQ-001 服务端；D-07 M3 状态入口 / M8 字段 / M10 流程发起）。
 *
 * <p>RED 骨架：方法体 {@code UnsupportedOperationException}，GREEN 提交实现（feat 提交转绿）。
 *
 * <p>GREEN 合同：本类为 firstchain 模块 REST 面的薄门面——申请编号服务端生成（M1：业务键服务端生成、
 * 租户内唯一）、写侧显式租户上下文缺失即拒（接入合同 §1.3）、状态写路径全部委托 bpm 域层
 * {@link FirstChainApplicationService}（幂等命令门与版本条件迁移在域层内聚，本层不做双写），
 * 查询走 {@link FirstchainApplicationMapper}（租户插件自动过滤）。
 *
 * @author ZS-FC-001
 */
@Service
public class FirstchainApplicationService {

    @Resource
    private FirstChainApplicationService firstChainApplicationService;

    @Resource
    private FirstchainApplicationMapper applicationMapper;

    /**
     * 创建申请（PILOT-REQ-001：草稿基线 DRAFT；申请编号服务端生成，租户内唯一）。
     *
     * @return 申请行 ID
     */
    public Long createApplication(ApplicationCreateReqVO createReqVO) {
        throw new UnsupportedOperationException("ZS-FC-001 GREEN 待实现");
    }

    /**
     * 提交申请（DRAFT→SUBMITTED，M3；同事务发起 Flowable 单节点审批流 + 绑定，M10=B）。
     */
    public void submitApplication(ApplicationSubmitReqVO submitReqVO) {
        throw new UnsupportedOperationException("ZS-FC-001 GREEN 待实现");
    }

    /**
     * 撤回审批流（M3：领域状态不变仍 SUBMITTED，仅解绑流程实例，可重新发起）。
     */
    public void withdrawApproval(ApplicationWithdrawReqVO withdrawReqVO) {
        throw new UnsupportedOperationException("ZS-FC-001 GREEN 待实现");
    }

    /**
     * 分页查询（租户过滤自动施加；跨租户不可见）。
     */
    public PageResult<ApplicationRespVO> getApplicationPage(ApplicationPageReqVO pageReqVO) {
        throw new UnsupportedOperationException("ZS-FC-001 GREEN 待实现");
    }

    /**
     * 详情查询（跨租户 0 行即 NOT_EXISTS，不静默）。
     */
    public ApplicationRespVO getApplication(Long id) {
        throw new UnsupportedOperationException("ZS-FC-001 GREEN 待实现");
    }

}
