package cn.zszj.module.firstchain.controller.admin.application;

import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.module.firstchain.controller.admin.application.vo.ApplicationApproveReqVO;
import cn.zszj.module.firstchain.controller.admin.application.vo.ApplicationCreateReqVO;
import cn.zszj.module.firstchain.controller.admin.application.vo.ApplicationPageReqVO;
import cn.zszj.module.firstchain.controller.admin.application.vo.ApplicationRejectReqVO;
import cn.zszj.module.firstchain.controller.admin.application.vo.ApplicationRespVO;
import cn.zszj.module.firstchain.controller.admin.application.vo.ApplicationSubmitReqVO;
import cn.zszj.module.firstchain.controller.admin.application.vo.ApplicationWithdrawReqVO;
import cn.zszj.module.firstchain.service.application.FirstchainApplicationService;
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
 * {@link FirstchainApplicationController} 的单元测试（ZS-FC-001）——REST 面权限矩阵冒烟。
 *
 * <p>两道断言：
 * <ul>
 *     <li><b>权限注解扫描（接入合同 §1.6「无注解写入口为零」）</b>：反射枚举全部端点方法，
 *     断言每个端点都挂 {@code @PreAuthorize("@ss.hasPermission('firstchain:application:xxx')")}
 *     且权限串落在 firstchain:application 资源段（M2 三角色矩阵落点）——新增端点漏挂注解即红；</li>
 *     <li><b>委托冒烟</b>：端点把 VO 原样交给应用服务（mock），返回 {@code CommonResult.success}。</li>
 * </ul>
 *
 * <p>循 PMS 模块 Controller 直测先例：直 new Controller + {@code ReflectionTestUtils} 注入 mock，不起 Web 容器。
 *
 * @author ZS-FC-001
 */
class FirstchainApplicationControllerTest {

    private static final String PERMISSION_PREFIX = "firstchain:application:";

    /** 方法名 → 期望权限串（M2 三角色矩阵在 REST 面的落点） */
    private static final Map<String, String> EXPECTED_PERMISSIONS = Map.of(
            "createApplication", PERMISSION_PREFIX + "create",
            "submitApplication", PERMISSION_PREFIX + "submit",
            "approveApplication", PERMISSION_PREFIX + "approve",
            "rejectApplication", PERMISSION_PREFIX + "reject",
            "withdrawApplication", PERMISSION_PREFIX + "withdraw",
            "getApplicationPage", PERMISSION_PREFIX + "query",
            "getApplication", PERMISSION_PREFIX + "query");

    // ========== ① 权限注解扫描（M2 矩阵：每个端点必须挂 @PreAuthorize） ==========

    @Test
    void everyEndpoint_declaresFirstchainApplicationPermission() {
        Map<String, Method> endpoints = Arrays.stream(FirstchainApplicationController.class.getDeclaredMethods())
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
    void createApplication_delegatesToApplicationService() {
        FirstchainApplicationService applicationService = mock(FirstchainApplicationService.class);
        FirstchainApplicationController controller = new FirstchainApplicationController();
        ReflectionTestUtils.setField(controller, "applicationService", applicationService);
        ApplicationCreateReqVO createReqVO = new ApplicationCreateReqVO();
        createReqVO.setApplicantName("众墅家装联盟（华东）");
        when(applicationService.createApplication(any())).thenReturn(1024L);

        CommonResult<Long> result = controller.createApplication(createReqVO);

        assertThat(result.getCode()).isZero();
        assertThat(result.getData()).isEqualTo(1024L);
        verify(applicationService).createApplication(createReqVO);
    }

    @Test
    void getApplication_delegatesToApplicationService() {
        FirstchainApplicationService applicationService = mock(FirstchainApplicationService.class);
        FirstchainApplicationController controller = new FirstchainApplicationController();
        ReflectionTestUtils.setField(controller, "applicationService", applicationService);
        ApplicationRespVO respVO = new ApplicationRespVO();
        respVO.setId(1024L);
        respVO.setStatus("SUBMITTED");
        when(applicationService.getApplication(1024L)).thenReturn(respVO);

        CommonResult<ApplicationRespVO> result = controller.getApplication(1024L);

        assertThat(result.getData().getStatus()).isEqualTo("SUBMITTED");
        verify(applicationService).getApplication(1024L);
    }

    // ========== 反射辅助 ==========

    private static String unwrapPermission(String spel) {
        String prefix = "@ss.hasPermission('";
        String suffix = "')";
        assertThat(spel).startsWith(prefix).endsWith(suffix);
        return spel.substring(prefix.length(), spel.length() - suffix.length());
    }

}
