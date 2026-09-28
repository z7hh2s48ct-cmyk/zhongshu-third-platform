package cn.zszj.module.system.controller.admin.permission;

import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.framework.datapermission.core.authorize.ObjectAuthorizationRequest;
import cn.zszj.framework.datapermission.core.authorize.ObjectAuthorizationRespDTO;
import cn.zszj.framework.datapermission.core.authorize.ObjectAuthorizationService;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.framework.test.core.ut.BaseMockitoUnitTest;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link ObjectAuthorizationController} 的单元测试（ZS-CLIENT-001.B）。
 *
 * <p>覆盖：授权输出端点的消费合同——注册域委派统一裁决输出（对象/动作/字段/脱敏四维由 PERM-003.A/.B
 * 裁决核心负责，本卡只透传）/ 未接入域与未装配裁决服务与未登录与空白 objectType 一律 success(null)
 * （零输出；已注册但对象不可见域经裁决核心 FORBIDDEN 拒绝，属 PERM-003.A 既有行为不在本卡范围）/
 * 参数透传完整性（objectType/orgId/ownerUserId）。
 *
 * @author ZS-CLIENT-001.B
 */
class ObjectAuthorizationControllerTest extends BaseMockitoUnitTest {

    private ObjectAuthorizationController newController(ObjectAuthorizationService service) {
        ObjectAuthorizationController controller = new ObjectAuthorizationController();
        org.springframework.test.util.ReflectionTestUtils.setField(controller,
                "objectAuthorizationService", service);
        return controller;
    }

    private LoginUser loginUser() {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(1L);
        return loginUser;
    }

    @Test // 注册域：委派统一裁决并原样输出（消费合同=透传，不二次加工）
    void getObjectAuthorization_registeredType_delegatesAndReturns() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser());
            ObjectAuthorizationService service = org.mockito.Mockito.mock(ObjectAuthorizationService.class);
            ObjectAuthorizationRespDTO dto = new ObjectAuthorizationRespDTO();
            dto.setAllowedActions(Set.of("lead:query"));
            dto.setAuthorizedFields(Set.of("name", "phone"));
            dto.setMaskedFields(Set.of("phone"));
            when(service.authorize(org.mockito.ArgumentMatchers.any(ObjectAuthorizationRequest.class)))
                    .thenReturn(dto);
            ObjectAuthorizationController controller = newController(service);

            CommonResult<ObjectAuthorizationRespDTO> result =
                    controller.getObjectAuthorization("lead", 100L, 1L);

            assertEquals(dto, result.getData());
            verify(service).authorize(org.mockito.ArgumentMatchers.argThat(req ->
                    "lead".equals(req.getObjectType())
                            && Long.valueOf(100L).equals(req.getOrgId())
                            && Long.valueOf(1L).equals(req.getOwnerUserId())));
        }
    }

    @Test // 未接入域（裁决返回 null）→ success(null)：零输出，不泄露域注册状态
    void getObjectAuthorization_unregisteredType_returnsNullData() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser());
            ObjectAuthorizationService service = org.mockito.Mockito.mock(ObjectAuthorizationService.class);
            when(service.authorize(org.mockito.ArgumentMatchers.any())).thenReturn(null);
            ObjectAuthorizationController controller = newController(service);

            CommonResult<ObjectAuthorizationRespDTO> result =
                    controller.getObjectAuthorization("unknown-type", null, null);

            assertNull(result.getData());
            verify(service).authorize(org.mockito.ArgumentMatchers.any());
        }
    }

    @Test // 空白 objectType → success(null)，不触达裁决（fail-closed 零输出）
    void getObjectAuthorization_blankObjectType_returnsNullDataWithoutDelegate() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser());
            ObjectAuthorizationService service = org.mockito.Mockito.mock(ObjectAuthorizationService.class);
            ObjectAuthorizationController controller = newController(service);

            assertNull(controller.getObjectAuthorization(" ", null, null).getData());
            assertNull(controller.getObjectAuthorization(null, null, null).getData());
            verifyNoInteractions(service);
        }
    }

    @Test // 未登录（异常态）→ success(null)，不触达裁决（同 my-targets 消费端惯例）
    void getObjectAuthorization_noLoginUser_returnsNullDataWithoutDelegate() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(null);
            ObjectAuthorizationService service = org.mockito.Mockito.mock(ObjectAuthorizationService.class);
            ObjectAuthorizationController controller = newController(service);

            assertNull(controller.getObjectAuthorization("lead", 100L, null).getData());
            verifyNoInteractions(service);
        }
    }

    @Test // 统一裁决服务未装配（窄上下文）→ success(null)：消费端零输出，不报 500
    void getObjectAuthorization_serviceNotAssembled_returnsNullData() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser());
            ObjectAuthorizationController controller = newController(null);

            assertNull(controller.getObjectAuthorization("lead", 100L, null).getData());
        }
    }

}
