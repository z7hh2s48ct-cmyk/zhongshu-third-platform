package cn.zszj.framework.security;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import cn.zszj.framework.security.fixture.SecurityFixtureApplication;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;

/**
 * ZS-SEC-012.B 联合回归测试基类：复用 ZS-SEC-012.A 真实安全链夹具
 * （{@link SecurityFixtureApplication} 全量装配真实 Filter/Security 链/方法权限/异常出口，
 * {@code mock-enable=false} 未禁用任何安全过滤器）。
 *
 * @author ZS-SEC-012.B
 */
@org.springframework.boot.test.context.SpringBootTest(classes = SecurityFixtureApplication.class,
        webEnvironment = org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@ActiveProfiles("fixture")
public abstract class SecurityChainJointRegressionTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected org.springframework.context.ApplicationContext applicationContext;

    @AfterEach
    public void tearDownTenantContext() {
        // 清理 ThreadLocal，避免跨用例泄漏
        TenantContextHolder.clear();
    }
}
