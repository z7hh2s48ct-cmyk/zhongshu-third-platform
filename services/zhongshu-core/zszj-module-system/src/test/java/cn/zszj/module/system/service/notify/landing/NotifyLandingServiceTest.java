package cn.zszj.module.system.service.notify.landing;

import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.common.exception.ErrorCode;
import cn.zszj.framework.common.util.json.JsonUtils;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.system.controller.admin.notify.vo.message.NotifyMessageLandingRespVO;
import cn.zszj.module.system.dal.dataobject.notify.NotifyMessageDO;
import cn.zszj.module.system.dal.mysql.notify.NotifyMessageMapper;
import cn.zszj.module.system.enums.ErrorCodeConstants;
import cn.zszj.module.system.service.notify.landing.NotifyLandingServiceTest.FixtureConfig.DisabledModuleLandingProvider;
import cn.zszj.module.system.service.notify.landing.NotifyLandingServiceTest.FixtureConfig.RevokedLandingProvider;
import cn.zszj.module.system.service.notify.landing.NotifyLandingServiceTest.FixtureConfig.WebAndMobileLandingProvider;
import cn.zszj.module.system.service.notify.landing.NotifyLandingServiceTest.FixtureConfig.WebOnlyLandingProvider;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Map;

import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zszj.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link NotifyLandingServiceImpl} 的单元测试（ZS-MSG-003 技术收件箱与落点二次授权）
 *
 * <p>边界：跨技术租户的行级隔离由生产装配的 tenant 拦截器提供（system_notify_message 非
 * ignore-tables）；H2 测试上下文不含该拦截器，消息链路的跨租户专项回归（真实 Mapper/HTTP 链验证
 * 跨租户消息 ID 不可读、不可解析、不可改已读）已登记为后续专项、随真实环境联调收口（r0-P2 订正：
 * 不得宣称复用 ZS-DB-018 既有 PG 回归——其用例未覆盖消息表与本接口）。本测试覆盖应用层
 * fail-closed 防线（租户缺失/存在性/归属/注册/模块/重授权/端描述）。
 */
@Import({NotifyLandingServiceImpl.class, NotifyLandingServiceTest.FixtureConfig.class})
public class NotifyLandingServiceTest extends BaseDbUnitTest {

    private static final Long USER_ID = 1L;
    private static final Integer USER_TYPE = UserTypeEnum.ADMIN.getValue();
    private static final Long TENANT_ID = 1L;

    @Resource
    private NotifyLandingService notifyLandingService;

    @Resource
    private NotifyMessageMapper notifyMessageMapper;

    @BeforeEach
    public void setUpTenant() {
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    public void restoreTenant() {
        TenantContextHolder.clear();
    }

    // ========== 可用落点 ==========

    @Test
    public void testResolveMessageLanding_webSuccess() {
        NotifyMessageDO message = insertOwnMessage(WebAndMobileLandingProvider.TEMPLATE_CODE,
                Map.of("bizId", 1024));
        NotifyMessageLandingRespVO result = notifyLandingService.resolveMessageLanding(
                message.getId(), USER_ID, USER_TYPE, NotifyLandingClient.WEB);
        assertTrue(result.getAvailable());
        assertNull(result.getUnavailableCode());
        assertEquals("system", result.getDescriptor().getModule());
        assertEquals("/system/fixture/detail", result.getDescriptor().getRoute());
        assertEquals(1024, result.getDescriptor().getParams().get("id"));
    }

    @Test
    public void testResolveMessageLanding_mobileSuccess() {
        NotifyMessageDO message = insertOwnMessage(WebAndMobileLandingProvider.TEMPLATE_CODE,
                Map.of("bizId", 2048));
        NotifyMessageLandingRespVO result = notifyLandingService.resolveMessageLanding(
                message.getId(), USER_ID, USER_TYPE, NotifyLandingClient.MOBILE);
        assertTrue(result.getAvailable());
        assertEquals("/pages-fixture/detail/index", result.getDescriptor().getRoute());
        assertEquals(2048, result.getDescriptor().getParams().get("id"));
    }

    @Test
    public void testResolveMessageLanding_responseCarriesNoMessageBody() {
        // 落点响应不得复制消息正文/模板参数全量（业务敏感正文不长期复制进通知响应）
        NotifyMessageDO message = randomPojo(NotifyMessageDO.class, o -> {
            o.setUserId(USER_ID);
            o.setUserType(USER_TYPE);
            o.setTemplateCode(WebAndMobileLandingProvider.TEMPLATE_CODE);
            o.setTemplateContent("机密业务正文-SECRET-BODY-1024");
            o.setTemplateParams(Map.of("token", "机密参数-SECRET-PARAM", "bizId", 1024));
        });
        notifyMessageMapper.insert(message);
        NotifyMessageLandingRespVO result = notifyLandingService.resolveMessageLanding(
                message.getId(), USER_ID, USER_TYPE, NotifyLandingClient.WEB);
        assertTrue(result.getAvailable());
        String json = JsonUtils.toJsonString(result);
        assertFalse(json.contains("SECRET-BODY"), "落点响应不得携带消息正文");
        assertFalse(json.contains("SECRET-PARAM"), "落点响应不得携带模板参数全量");
    }

    // ========== 安全拒绝（ServiceException） ==========

    @Test
    public void testResolveMessageLanding_messageNotFound() {
        assertServiceException(() -> notifyLandingService.resolveMessageLanding(
                        999999L, USER_ID, USER_TYPE, NotifyLandingClient.WEB),
                ErrorCodeConstants.NOTIFY_LANDING_MESSAGE_NOT_FOUND);
    }

    @Test
    public void testResolveMessageLanding_otherUserDenied() {
        NotifyMessageDO otherUserMessage = insertMessage(2L, USER_TYPE,
                WebAndMobileLandingProvider.TEMPLATE_CODE, Map.of("bizId", 1024));
        NotifyMessageDO otherTypeMessage = insertMessage(USER_ID, UserTypeEnum.MEMBER.getValue(),
                WebAndMobileLandingProvider.TEMPLATE_CODE, Map.of("bizId", 1024));
        // 他人 userId 拒绝
        assertServiceException(() -> notifyLandingService.resolveMessageLanding(
                        otherUserMessage.getId(), USER_ID, USER_TYPE, NotifyLandingClient.WEB),
                ErrorCodeConstants.NOTIFY_LANDING_ACCESS_DENIED);
        // userType 不匹配同样拒绝（ADMIN 主体不能以 MEMBER 收件箱身份触碰）
        assertServiceException(() -> notifyLandingService.resolveMessageLanding(
                        otherTypeMessage.getId(), USER_ID, USER_TYPE, NotifyLandingClient.WEB),
                ErrorCodeConstants.NOTIFY_LANDING_ACCESS_DENIED);
    }

    @Test
    public void testResolveMessageLanding_tenantRequired() {
        NotifyMessageDO message = insertOwnMessage(WebAndMobileLandingProvider.TEMPLATE_CODE, Map.of("bizId", 1024));
        TenantContextHolder.clear();
        try {
            assertServiceException(() -> notifyLandingService.resolveMessageLanding(
                            message.getId(), USER_ID, USER_TYPE, NotifyLandingClient.WEB),
                    ErrorCodeConstants.NOTIFY_LANDING_TENANT_REQUIRED);
        } finally {
            TenantContextHolder.setTenantId(TENANT_ID);
        }
    }

    // ========== 落点不可用（业务可呈现状态） ==========

    @Test
    public void testResolveMessageLanding_notRegistered() {
        NotifyMessageDO message = insertOwnMessage("zs_unknown_template", Map.of("bizId", 1024));
        NotifyMessageLandingRespVO result = notifyLandingService.resolveMessageLanding(
                message.getId(), USER_ID, USER_TYPE, NotifyLandingClient.WEB);
        assertFalse(result.getAvailable());
        assertEquals(NotifyLandingUnavailable.NOT_REGISTERED.name(), result.getUnavailableCode());
        assertNotNull(result.getReason());
    }

    @Test
    public void testResolveMessageLanding_moduleDisabled() {
        // bpm 在 ModuleCatalog.ENABLED_MODULES 之外：关闭模块落点明确不可用，且不触碰业务授权
        NotifyMessageDO message = insertOwnMessage(DisabledModuleLandingProvider.TEMPLATE_CODE, Map.of("bizId", 1024));
        NotifyMessageLandingRespVO result = notifyLandingService.resolveMessageLanding(
                message.getId(), USER_ID, USER_TYPE, NotifyLandingClient.WEB);
        assertFalse(result.getAvailable());
        assertEquals(NotifyLandingUnavailable.MODULE_DISABLED.name(), result.getUnavailableCode());
    }

    @Test
    public void testResolveMessageLanding_revoked() {
        // 业务重新授权未通过：旧消息在业务撤权后不得借落点进入详情/附件
        NotifyMessageDO message = insertOwnMessage(RevokedLandingProvider.TEMPLATE_CODE, Map.of("bizId", 1024));
        NotifyMessageLandingRespVO result = notifyLandingService.resolveMessageLanding(
                message.getId(), USER_ID, USER_TYPE, NotifyLandingClient.WEB);
        assertFalse(result.getAvailable());
        assertEquals(NotifyLandingUnavailable.REVOKED.name(), result.getUnavailableCode());
        assertTrue(result.getReason().contains("已无权访问"));
        assertNull(result.getDescriptor());
    }

    @Test
    public void testResolveMessageLanding_clientUnsupported() {
        NotifyMessageDO message = insertOwnMessage(WebOnlyLandingProvider.TEMPLATE_CODE, Map.of("bizId", 1024));
        NotifyMessageLandingRespVO result = notifyLandingService.resolveMessageLanding(
                message.getId(), USER_ID, USER_TYPE, NotifyLandingClient.MOBILE);
        assertFalse(result.getAvailable());
        assertEquals(NotifyLandingUnavailable.CLIENT_UNSUPPORTED.name(), result.getUnavailableCode());
    }

    // ========== 注册表装配合同 ==========

    @Test
    public void testRegistry_duplicateTemplateCodeRejected() {
        NotifyLandingProvider duplicated = new WebAndMobileLandingProvider();
        assertThrows(IllegalStateException.class,
                () -> new NotifyLandingRegistry(List.of(new WebAndMobileLandingProvider(), duplicated)));
    }

    // ========== 夹具 ==========

    private NotifyMessageDO insertOwnMessage(String templateCode, Map<String, Object> params) {
        return insertMessage(USER_ID, USER_TYPE, templateCode, params);
    }

    private NotifyMessageDO insertMessage(Long userId, Integer userType, String templateCode,
                                          Map<String, Object> params) {
        NotifyMessageDO message = randomPojo(NotifyMessageDO.class, o -> {
            o.setUserId(userId);
            o.setUserType(userType);
            o.setTemplateCode(templateCode);
            o.setTemplateParams(params);
        });
        notifyMessageMapper.insert(message);
        return message;
    }

    @TestConfiguration(proxyBeanMethods = false)
    public static class FixtureConfig {

        /** 双端落点夹具：WEB/MOBILE 各声明一条路由 */
        public static class WebAndMobileLandingProvider implements NotifyLandingProvider {

            static final String TEMPLATE_CODE = "zs_fixture_landing";

            @Override
            public String templateCode() {
                return TEMPLATE_CODE;
            }

            @Override
            public String module() {
                return "system";
            }

            @Override
            public void authorize(NotifyMessageDO message, Map<String, Object> templateParams) {
                // 夹具：授权通过
            }

            @Override
            public NotifyLandingDescriptor resolve(NotifyLandingClient client, Map<String, Object> templateParams) {
                if (client == NotifyLandingClient.WEB) {
                    return NotifyLandingDescriptor.builder().module(module()).route("/system/fixture/detail")
                            .params(Map.of("id", templateParams.get("bizId"))).build();
                }
                return NotifyLandingDescriptor.builder().module(module()).route("/pages-fixture/detail/index")
                        .params(Map.of("id", templateParams.get("bizId"))).build();
            }

        }

        /** 仅 Web 端落点夹具：移动端查询应判 CLIENT_UNSUPPORTED */
        public static class WebOnlyLandingProvider implements NotifyLandingProvider {

            static final String TEMPLATE_CODE = "zs_fixture_web_only";

            @Override
            public String templateCode() {
                return TEMPLATE_CODE;
            }

            @Override
            public String module() {
                return "system";
            }

            @Override
            public void authorize(NotifyMessageDO message, Map<String, Object> templateParams) {
                // 夹具：授权通过
            }

            @Override
            public NotifyLandingDescriptor resolve(NotifyLandingClient client, Map<String, Object> templateParams) {
                return client == NotifyLandingClient.WEB
                        ? NotifyLandingDescriptor.builder().module(module()).route("/system/fixture/web-only").build()
                        : null;
            }

        }

        /** 业务失权夹具：重新授权抛业务异常 → REVOKED */
        public static class RevokedLandingProvider implements NotifyLandingProvider {

            static final String TEMPLATE_CODE = "zs_fixture_revoked";

            static final ErrorCode REVOKED_ERROR = new ErrorCode(2_002_000_001, "业务对象已删除或您已无权访问");

            @Override
            public String templateCode() {
                return TEMPLATE_CODE;
            }

            @Override
            public String module() {
                return "system";
            }

            @Override
            public void authorize(NotifyMessageDO message, Map<String, Object> templateParams) {
                throw cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception(REVOKED_ERROR);
            }

            @Override
            public NotifyLandingDescriptor resolve(NotifyLandingClient client, Map<String, Object> templateParams) {
                throw new IllegalStateException("授权未通过时不应解析落点");
            }

        }

        /** 关闭模块夹具：module=bpm（ModuleCatalog.DISABLED_MODULES），任何方法都不应被调用 */
        public static class DisabledModuleLandingProvider implements NotifyLandingProvider {

            static final String TEMPLATE_CODE = "zs_fixture_disabled_module";

            @Override
            public String templateCode() {
                return TEMPLATE_CODE;
            }

            @Override
            public String module() {
                return "bpm";
            }

            @Override
            public void authorize(NotifyMessageDO message, Map<String, Object> templateParams) {
                throw new IllegalStateException("关闭模块的落点条目不应被触碰");
            }

            @Override
            public NotifyLandingDescriptor resolve(NotifyLandingClient client, Map<String, Object> templateParams) {
                throw new IllegalStateException("关闭模块的落点条目不应被触碰");
            }

        }

        @Bean
        public NotifyLandingRegistry notifyLandingRegistry() {
            return new NotifyLandingRegistry(List.of(
                    new WebAndMobileLandingProvider(),
                    new WebOnlyLandingProvider(),
                    new RevokedLandingProvider(),
                    new DisabledModuleLandingProvider()));
        }

    }

}
