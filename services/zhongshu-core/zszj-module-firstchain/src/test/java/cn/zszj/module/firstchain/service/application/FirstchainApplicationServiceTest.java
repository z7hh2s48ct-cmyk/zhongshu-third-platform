package cn.zszj.module.firstchain.service.application;

import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.bpm.firstchain.FirstChainApplicationService;
import cn.zszj.module.bpm.firstchain.FirstChainApplicationService.CreateApplicationCmd;
import cn.zszj.module.bpm.firstchain.FirstChainObjectType;
import cn.zszj.module.bpm.firstchain.FirstChainProcessBindingService;
import cn.zszj.module.firstchain.controller.admin.application.vo.ApplicationCreateReqVO;
import cn.zszj.module.firstchain.controller.admin.application.vo.ApplicationSubmitReqVO;
import cn.zszj.module.firstchain.controller.admin.application.vo.ApplicationWithdrawReqVO;
import cn.zszj.module.firstchain.dal.dataobject.application.ApplicationDO;
import cn.zszj.module.firstchain.dal.mysql.application.FirstchainApplicationMapper;
import cn.zszj.module.firstchain.service.wiring.FirstchainNotifyWiringService;
import cn.zszj.module.infra.api.file.FileApi;
import cn.zszj.module.infra.enums.ErrorCodeConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link FirstchainApplicationService}（REST 面门面）的单元测试（ZS-FC-003 接线 wave）——
 * 提交/撤回的六节点接线合同（ZS-FC-003）。
 *
 * <p>创建（ZS-FC-001 附件写入侧）：资质附件引用写入前经 {@link FileApi#validatePrivateFileReferences} 校验——
 * 校验先于落库、非法整单拒绝不创建申请、去重保序、无附件不触发校验。
 *
 * <p>覆盖：提交后从最新绑定读流程实例并接线（待办注册 + 审批提醒，approverUserId 缺省=提交人）；
 * 绑定缺失时 WARN 跳过接线（可观测不阻断）；撤回后经同一绑定实例撤销待办（WITHDRAW 事件驱动）。
 *
 * @author ZS-FC-003
 */
class FirstchainApplicationServiceTest {

    private static final Long TENANT_ID = 1L;

    private static final Long APPLICATION_ID = 1024L;

    private static final Long LOGIN_USER_ID = 100L;

    private FirstChainApplicationService bpmApplicationService;

    private FirstChainProcessBindingService processBindingService;

    private FirstchainNotifyWiringService notifyWiringService;

    private FirstchainApplicationMapper applicationMapper;

    private FileApi fileApi;

    private FirstchainApplicationService applicationService;

    @BeforeEach
    void setUp() {
        bpmApplicationService = mock(FirstChainApplicationService.class);
        processBindingService = mock(FirstChainProcessBindingService.class);
        notifyWiringService = mock(FirstchainNotifyWiringService.class);
        applicationMapper = mock(FirstchainApplicationMapper.class);
        fileApi = mock(FileApi.class);
        applicationService = new FirstchainApplicationService();
        ReflectionTestUtils.setField(applicationService, "firstChainApplicationService", bpmApplicationService);
        ReflectionTestUtils.setField(applicationService, "processBindingService", processBindingService);
        ReflectionTestUtils.setField(applicationService, "notifyWiringService", notifyWiringService);
        ReflectionTestUtils.setField(applicationService, "applicationMapper", applicationMapper);
        ReflectionTestUtils.setField(applicationService, "fileApi", fileApi);
        TenantContextHolder.setTenantId(TENANT_ID);
        loginAs(LOGIN_USER_ID);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    @Test
    void submitApplication_wiresApprovalTodoAndNotifyWithBindingInstance() {
        when(applicationMapper.selectById(APPLICATION_ID)).thenReturn(application());
        Map<String, Object> binding = binding("PINST-42", "BOUND");
        when(processBindingService.findLatestByDomain(TENANT_ID, FirstChainObjectType.APPLICATION, APPLICATION_ID))
                .thenReturn(binding);

        ApplicationSubmitReqVO reqVO = new ApplicationSubmitReqVO();
        reqVO.setId(APPLICATION_ID);
        reqVO.setExpectedVersion(0L);

        applicationService.submitApplication(reqVO);

        // bpm 域委托（审批人缺省=提交人）
        verify(bpmApplicationService).submitApplication(APPLICATION_ID, 0L, LOGIN_USER_ID,
                String.valueOf(LOGIN_USER_ID));
        // 接线：待办注册 + 审批提醒（流程实例自绑定读回；版本=提交后基线）
        verify(notifyWiringService).onApplicationSubmitted("FC20260929-A1B2C3D4", APPLICATION_ID, "PINST-42",
                1L, "众墅家装联盟（华东）", LOGIN_USER_ID, String.valueOf(LOGIN_USER_ID));
    }

    @Test
    void submitApplication_bindingMissing_skipsWiringObservably() {
        when(applicationMapper.selectById(APPLICATION_ID)).thenReturn(application());
        when(processBindingService.findLatestByDomain(TENANT_ID, FirstChainObjectType.APPLICATION, APPLICATION_ID))
                .thenReturn(null);

        ApplicationSubmitReqVO reqVO = new ApplicationSubmitReqVO();
        reqVO.setId(APPLICATION_ID);
        reqVO.setExpectedVersion(0L);

        applicationService.submitApplication(reqVO);

        verify(bpmApplicationService).submitApplication(anyLong(), anyLong(), anyLong(), anyString());
        verify(notifyWiringService, org.mockito.Mockito.never())
                .onApplicationSubmitted(any(), anyLong(), any(), anyLong(), any(), anyLong(), any());
    }

    @Test
    void withdrawApproval_wiresTodoWithdrawWithSameInstance() {
        when(applicationMapper.selectById(APPLICATION_ID)).thenReturn(application());
        Map<String, Object> binding = binding("PINST-42", "WITHDRAWN");
        when(processBindingService.findLatestByDomain(TENANT_ID, FirstChainObjectType.APPLICATION, APPLICATION_ID))
                .thenReturn(binding);

        ApplicationWithdrawReqVO reqVO = new ApplicationWithdrawReqVO();
        reqVO.setId(APPLICATION_ID);
        reqVO.setExpectedVersion(1L);

        applicationService.withdrawApproval(reqVO);

        verify(bpmApplicationService).withdrawApproval(APPLICATION_ID, 1L, String.valueOf(LOGIN_USER_ID));
        verify(notifyWiringService).onApprovalWithdrawn("FC20260929-A1B2C3D4", APPLICATION_ID, "PINST-42",
                1L, String.valueOf(LOGIN_USER_ID));
    }

    @Test
    void submitApplication_explicitApproverWinsOverLoginUser() {
        when(applicationMapper.selectById(APPLICATION_ID)).thenReturn(application());
        when(processBindingService.findLatestByDomain(eq(TENANT_ID), any(), eq(APPLICATION_ID)))
                .thenReturn(binding("PINST-42", "BOUND"));

        ApplicationSubmitReqVO reqVO = new ApplicationSubmitReqVO();
        reqVO.setId(APPLICATION_ID);
        reqVO.setExpectedVersion(0L);
        reqVO.setApproverUserId(200L);

        applicationService.submitApplication(reqVO);

        verify(bpmApplicationService).submitApplication(APPLICATION_ID, 0L, 200L, String.valueOf(LOGIN_USER_ID));
        verify(notifyWiringService).onApplicationSubmitted(any(), eq(APPLICATION_ID), any(), eq(1L), any(),
                eq(200L), any());
    }

    // ========== 创建：资质附件写入侧校验（ZS-FC-001，PILOT-REQ-001） ==========

    @Test
    void createApplication_validatesAttachmentsBeforePersisting() {
        when(bpmApplicationService.createApplication(any())).thenReturn(APPLICATION_ID);

        Long id = applicationService.createApplication(createReq(101L, 102L));

        assertThat(id).isEqualTo(APPLICATION_ID);
        InOrder order = inOrder(fileApi, bpmApplicationService);
        order.verify(fileApi).validatePrivateFileReferences(List.of(101L, 102L));
        ArgumentCaptor<CreateApplicationCmd> cmd = ArgumentCaptor.forClass(CreateApplicationCmd.class);
        order.verify(bpmApplicationService).createApplication(cmd.capture());
        assertThat(cmd.getValue().attachmentFileIds()).isEqualTo("[101,102]");
    }

    @Test
    void createApplication_invalidAttachment_rejectsWholeApplicationWithoutPersisting() {
        ServiceException invalid = new ServiceException(ErrorCodeConstants.FILE_REFERENCE_INVALID.getCode(),
                "附件文件（101）不存在或不可作为私有附件引用");
        doThrow(invalid).when(fileApi).validatePrivateFileReferences(List.of(101L));

        assertThatThrownBy(() -> applicationService.createApplication(createReq(101L)))
                .isSameAs(invalid);

        // 校验失败整单拒绝：不得落任何申请行（先校验后落库，非法引用不入库）
        verify(bpmApplicationService, never()).createApplication(any());
    }

    @Test
    void createApplication_duplicateAttachmentIds_dedupedPreservingOrder() {
        when(bpmApplicationService.createApplication(any())).thenReturn(APPLICATION_ID);

        applicationService.createApplication(createReq(102L, 101L, 102L, 101L));

        verify(fileApi).validatePrivateFileReferences(List.of(102L, 101L));
        ArgumentCaptor<CreateApplicationCmd> cmd = ArgumentCaptor.forClass(CreateApplicationCmd.class);
        verify(bpmApplicationService).createApplication(cmd.capture());
        assertThat(cmd.getValue().attachmentFileIds()).isEqualTo("[102,101]");
    }

    @Test
    void createApplication_noAttachments_skipsValidationAndStoresNull() {
        when(bpmApplicationService.createApplication(any())).thenReturn(APPLICATION_ID);

        applicationService.createApplication(createReq());

        verify(fileApi, never()).validatePrivateFileReferences(any());
        ArgumentCaptor<CreateApplicationCmd> cmd = ArgumentCaptor.forClass(CreateApplicationCmd.class);
        verify(bpmApplicationService).createApplication(cmd.capture());
        assertThat(cmd.getValue().attachmentFileIds()).isNull();
    }

    @Test
    void createApplication_nullAttachmentList_skipsValidation() {
        when(bpmApplicationService.createApplication(any())).thenReturn(APPLICATION_ID);
        ApplicationCreateReqVO reqVO = createReq();
        reqVO.setAttachmentFileIds(null);

        applicationService.createApplication(reqVO);

        verify(fileApi, never()).validatePrivateFileReferences(any());
    }

    // ========== 夹具 ==========

    private static ApplicationCreateReqVO createReq(Long... attachmentFileIds) {
        ApplicationCreateReqVO reqVO = new ApplicationCreateReqVO();
        reqVO.setApplicantName("众墅家装联盟（华东）");
        reqVO.setContactName("张三");
        reqVO.setContactPhone("13800000000");
        reqVO.setAttachmentFileIds(Arrays.asList(attachmentFileIds));
        return reqVO;
    }

    private static ApplicationDO application() {
        ApplicationDO application = new ApplicationDO();
        application.setId(APPLICATION_ID);
        application.setAppKey("FC20260929-A1B2C3D4");
        application.setApplicantName("众墅家装联盟（华东）");
        application.setVersion(0L);
        return application;
    }

    private static Map<String, Object> binding(String processInstanceId, String status) {
        Map<String, Object> binding = new HashMap<>();
        binding.put("process_instance_id", processInstanceId);
        binding.put("status", status);
        return binding;
    }

    private static void loginAs(Long userId) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(userId);
        loginUser.setUserType(2);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null));
    }

}
