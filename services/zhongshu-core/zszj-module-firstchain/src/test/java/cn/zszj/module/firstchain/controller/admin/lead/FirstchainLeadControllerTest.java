package cn.zszj.module.firstchain.controller.admin.lead;

import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.module.firstchain.controller.admin.lead.vo.LeadAssignReqVO;
import cn.zszj.module.firstchain.controller.admin.lead.vo.LeadClaimReqVO;
import cn.zszj.module.firstchain.controller.admin.lead.vo.LeadConvertReqVO;
import cn.zszj.module.firstchain.controller.admin.lead.vo.LeadDistributeReqVO;
import cn.zszj.module.firstchain.controller.admin.lead.vo.LeadFollowupReqVO;
import cn.zszj.module.firstchain.controller.admin.lead.vo.LeadInvalidateReqVO;
import cn.zszj.module.firstchain.controller.admin.lead.vo.LeadPageReqVO;
import cn.zszj.module.firstchain.controller.admin.lead.vo.LeadReassignReqVO;
import cn.zszj.module.firstchain.controller.admin.lead.vo.LeadRespVO;
import cn.zszj.module.firstchain.service.lead.FirstchainLeadAppService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link FirstchainLeadController} 的单元测试（ZS-FC-002 REST wave）——REST 面权限矩阵冒烟。
 *
 * <p>循 {@code FirstchainApplicationControllerTest} 同款两道断言：
 * <ul>
 *     <li><b>权限注解扫描（接入合同 §1.6「无注解写入口为零」）</b>：反射枚举全部端点方法，
 *     断言每个端点都挂 {@code @PreAuthorize("@ss.hasPermission('firstchain:lead:xxx')")} 且权限串
 *     与 M2 三角色矩阵一致——新增端点漏挂注解即红；</li>
 *     <li><b>委托冒烟</b>：端点把 VO 原样交给门面服务（mock），返回 {@code CommonResult.success}。</li>
 * </ul>
 *
 * @author ZS-FC-002
 */
class FirstchainLeadControllerTest {

    private static final String PERMISSION_PREFIX = "firstchain:lead:";

    /** 方法名 → 期望权限串（M2 三角色矩阵在 REST 面的落点；query 覆盖三视角查询面） */
    private static final Map<String, String> EXPECTED_PERMISSIONS = Map.of(
            "distributeLead", PERMISSION_PREFIX + "distribute",
            "assignLead", PERMISSION_PREFIX + "assign",
            "claimLead", PERMISSION_PREFIX + "claim",
            "reassignLead", PERMISSION_PREFIX + "reassign",
            "followupLead", PERMISSION_PREFIX + "followup",
            "convertLead", PERMISSION_PREFIX + "convert",
            "invalidateLead", PERMISSION_PREFIX + "invalidate",
            "getLeadPage", PERMISSION_PREFIX + "query",
            "getLead", PERMISSION_PREFIX + "query",
            "getLeadStatusMetrics", PERMISSION_PREFIX + "query");

    // ========== ① 权限注解扫描（M2 矩阵：每个端点必须挂 @PreAuthorize） ==========

    @Test
    void everyEndpoint_declaresFirstchainLeadPermission() {
        Map<String, Method> endpoints = Arrays.stream(FirstchainLeadController.class.getDeclaredMethods())
                .filter(method -> EXPECTED_PERMISSIONS.containsKey(method.getName()))
                .collect(Collectors.toMap(Method::getName, method -> method));
        // 端点全集齐备（防误删端点导致扫描空转）
        assertThat(endpoints.keySet()).containsExactlyInAnyOrderElementsOf(EXPECTED_PERMISSIONS.keySet());
        for (Map.Entry<String, Method> entry : endpoints.entrySet()) {
            PreAuthorize preAuthorize = entry.getValue().getAnnotation(PreAuthorize.class);
            assertThat(preAuthorize)
                    .as("端点 %s 必须挂 @PreAuthorize（接入合同 §1.6：无注解写入口为零）", entry.getKey())
                    .isNotNull();
            String permission = unwrapPermission(preAuthorize.value());
            assertThat(permission)
                    .as("端点 %s 的权限串必须与 M2 矩阵一致", entry.getKey())
                    .isEqualTo(EXPECTED_PERMISSIONS.get(entry.getKey()));
        }
    }

    // ========== ② 委托冒烟 ==========

    @Test
    void distributeLead_delegatesToAppService() {
        FirstchainLeadAppService leadAppService = mock(FirstchainLeadAppService.class);
        FirstchainLeadController controller = new FirstchainLeadController();
        ReflectionTestUtils.setField(controller, "leadAppService", leadAppService);
        LeadDistributeReqVO distributeReqVO = new LeadDistributeReqVO();
        distributeReqVO.setCustomerName("李四");
        distributeReqVO.setOrgId(300L);
        when(leadAppService.distributeLead(any())).thenReturn(2048L);

        CommonResult<Long> result = controller.distributeLead(distributeReqVO);

        assertThat(result.getCode()).isZero();
        assertThat(result.getData()).isEqualTo(2048L);
        verify(leadAppService).distributeLead(distributeReqVO);
    }

    @Test
    void claimLead_delegatesToAppService() {
        FirstchainLeadAppService leadAppService = mock(FirstchainLeadAppService.class);
        FirstchainLeadController controller = new FirstchainLeadController();
        ReflectionTestUtils.setField(controller, "leadAppService", leadAppService);
        LeadClaimReqVO claimReqVO = new LeadClaimReqVO();
        claimReqVO.setId(2048L);
        claimReqVO.setExpectedVersion(1L);

        CommonResult<Boolean> result = controller.claimLead(claimReqVO);

        assertThat(result.getData()).isTrue();
        verify(leadAppService).claimLead(claimReqVO);
    }

    @Test
    void getLead_delegatesToAppService() {
        FirstchainLeadAppService leadAppService = mock(FirstchainLeadAppService.class);
        FirstchainLeadController controller = new FirstchainLeadController();
        ReflectionTestUtils.setField(controller, "leadAppService", leadAppService);
        LeadRespVO respVO = new LeadRespVO();
        respVO.setId(2048L);
        respVO.setStatus("FOLLOWING");
        when(leadAppService.getLead(2048L)).thenReturn(respVO);

        CommonResult<LeadRespVO> result = controller.getLead(2048L);

        assertThat(result.getData().getStatus()).isEqualTo("FOLLOWING");
        verify(leadAppService).getLead(2048L);
    }

    // ========== ③ 写端点全集委托冒烟（防端点错绑服务方法） ==========

    @Test
    void writeEndpoints_delegateSameNamedAppServiceMethods() {
        FirstchainLeadAppService leadAppService = mock(FirstchainLeadAppService.class);
        FirstchainLeadController controller = new FirstchainLeadController();
        ReflectionTestUtils.setField(controller, "leadAppService", leadAppService);

        LeadAssignReqVO assignReqVO = new LeadAssignReqVO();
        controller.assignLead(assignReqVO);
        LeadReassignReqVO reassignReqVO = new LeadReassignReqVO();
        controller.reassignLead(reassignReqVO);
        LeadFollowupReqVO followupReqVO = new LeadFollowupReqVO();
        controller.followupLead(followupReqVO);
        LeadConvertReqVO convertReqVO = new LeadConvertReqVO();
        controller.convertLead(convertReqVO);
        LeadInvalidateReqVO invalidateReqVO = new LeadInvalidateReqVO();
        controller.invalidateLead(invalidateReqVO);
        controller.getLeadPage(new LeadPageReqVO());
        controller.getLeadStatusMetrics();

        verify(leadAppService).assignLead(assignReqVO);
        verify(leadAppService).reassignLead(reassignReqVO);
        verify(leadAppService).followupLead(followupReqVO);
        verify(leadAppService).convertLead(convertReqVO);
        verify(leadAppService).invalidateLead(invalidateReqVO);
        verify(leadAppService).getLeadPage(any());
        verify(leadAppService).getLeadStatusMetrics();
    }

    // ========== 反射辅助 ==========

    private static String unwrapPermission(String spel) {
        String prefix = "@ss.hasPermission('";
        String suffix = "')";
        assertThat(spel).startsWith(prefix).endsWith(suffix);
        return spel.substring(prefix.length(), spel.length() - suffix.length());
    }

}
