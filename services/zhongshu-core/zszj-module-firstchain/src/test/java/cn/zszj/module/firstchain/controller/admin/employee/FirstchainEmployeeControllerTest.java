package cn.zszj.module.firstchain.controller.admin.employee;

import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.module.firstchain.controller.admin.employee.vo.EmployeeCreateReqVO;
import cn.zszj.module.firstchain.controller.admin.employee.vo.EmployeeCreatedRespVO;
import cn.zszj.module.firstchain.service.employee.FirstchainEmployeeService;
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
 * {@link FirstchainEmployeeController} 的单元测试（ZS-FC-001 员工授权 wave）——REST 面权限矩阵冒烟。
 *
 * @author ZS-FC-001
 */
class FirstchainEmployeeControllerTest {

    /** 方法名 → 期望权限串（M5-A 负责人面） */
    private static final Map<String, String> EXPECTED_PERMISSIONS = Map.of(
            "createEmployee", "firstchain:employee:create");

    @Test
    void everyEndpoint_declaresFirstchainEmployeePermission() {
        Map<String, Method> endpoints = Arrays.stream(FirstchainEmployeeController.class.getDeclaredMethods())
                .filter(method -> EXPECTED_PERMISSIONS.containsKey(method.getName()))
                .collect(Collectors.toMap(Method::getName, method -> method));
        assertThat(endpoints.keySet()).containsExactlyInAnyOrderElementsOf(EXPECTED_PERMISSIONS.keySet());
        for (Map.Entry<String, Method> entry : endpoints.entrySet()) {
            PreAuthorize preAuthorize = entry.getValue().getAnnotation(PreAuthorize.class);
            assertThat(preAuthorize)
                    .as("端点 %s 必须挂 @PreAuthorize（接入合同 §1.6：无注解写入口为零）", entry.getKey())
                    .isNotNull();
            String spel = preAuthorize.value();
            assertThat(spel).isEqualTo("@ss.hasPermission('" + EXPECTED_PERMISSIONS.get(entry.getKey()) + "')");
        }
    }

    @Test
    void createEmployee_delegatesToEmployeeService() {
        FirstchainEmployeeService employeeService = mock(FirstchainEmployeeService.class);
        FirstchainEmployeeController controller = new FirstchainEmployeeController();
        ReflectionTestUtils.setField(controller, "employeeService", employeeService);
        EmployeeCreateReqVO createReqVO = new EmployeeCreateReqVO();
        createReqVO.setUsername("zhangsan001");
        createReqVO.setNickname("张三");
        EmployeeCreatedRespVO respVO = new EmployeeCreatedRespVO();
        respVO.setUserId(950L);
        respVO.setUsername("zhangsan001");
        respVO.setInitialPassword("aB3dEf7hIj9kLm2N");
        when(employeeService.createEmployee(any())).thenReturn(respVO);

        CommonResult<EmployeeCreatedRespVO> result = controller.createEmployee(createReqVO);

        assertThat(result.getCode()).isZero();
        assertThat(result.getData().getUserId()).isEqualTo(950L);
        assertThat(result.getData().getInitialPassword()).isEqualTo("aB3dEf7hIj9kLm2N");
        verify(employeeService).createEmployee(createReqVO);
    }

}
