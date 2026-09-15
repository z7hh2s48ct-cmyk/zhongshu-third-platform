package cn.zszj.module.system.controller.admin.oauth2;

import cn.zszj.module.system.controller.admin.oauth2.vo.token.OAuth2AccessTokenRespVO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link OAuth2AccessTokenRespVO} 的凭据暴露护栏测试（ZS-LOGIN-006）。
 *
 * <p>会话管理列表/详情不得返回可用于认证的秘密：访问令牌、刷新令牌一旦经管理端点回显，
 * 即等同把「持有即可认证」的凭据暴露给任何具备查看权限的主体（含前端表格、浏览器缓存、
 * 访问日志）。本测试以反射固化「RespVO 不声明令牌字段、不暴露令牌 getter」的结构合同，
 * 任何回归（重新加回 accessToken/refreshToken）都会在此失败并要求评审。
 *
 * <p>纯静态反射，不启动 Spring 上下文、不依赖 DB/Redis。
 */
public class OAuth2AccessTokenRespVoSecretTest {

    /** 会话管理响应体禁止出现的凭据字段名（归一化小写比对）。 */
    private static final String[] FORBIDDEN_SECRET_FIELDS = {"accesstoken", "refreshtoken"};

    @Test
    public void testRespVoDeclaresNoCredentialFields() {
        for (Field field : OAuth2AccessTokenRespVO.class.getDeclaredFields()) {
            String normalized = field.getName().toLowerCase().replace("_", "").replace("-", "");
            assertFalse(Arrays.asList(FORBIDDEN_SECRET_FIELDS).contains(normalized),
                    "会话管理响应体不得声明凭据字段：" + field.getName()
                            + "（ZS-LOGIN-006：列表/日志不返回可用秘密）");
        }
    }

    @Test
    public void testRespVoExposesNoCredentialGetter() {
        for (Method method : OAuth2AccessTokenRespVO.class.getDeclaredMethods()) {
            String lower = method.getName().toLowerCase();
            boolean isTokenGetter = (lower.startsWith("get") || lower.startsWith("is"))
                    && (lower.contains("accesstoken") || lower.contains("refreshtoken"));
            assertFalse(isTokenGetter,
                    "会话管理响应体不得暴露凭据 getter：" + method.getName()
                            + "（ZS-LOGIN-006：不可用于认证的会话 ID 管理）");
        }
    }

    @Test
    public void testRespVoRetainsNonSecretSessionAttributes() {
        // 会话展示所需「归属/客户端/创建/到期」非秘密属性必须保留（ZS-LOGIN-006 调整项）
        assertTrue(hasField("id"), "应保留会话 ID（不可用于认证的管理标识）");
        assertTrue(hasField("userId"), "应保留归属用户编号");
        assertTrue(hasField("userType"), "应保留归属用户类型");
        assertTrue(hasField("clientId"), "应保留客户端信息");
        assertTrue(hasField("createTime"), "应保留创建时间");
        assertTrue(hasField("expiresTime"), "应保留到期时间");
    }

    private boolean hasField(String name) {
        return Arrays.stream(OAuth2AccessTokenRespVO.class.getDeclaredFields())
                .anyMatch(f -> f.getName().equals(name));
    }

}
