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
@org.springframework.boot.test.context.SpringBootTest(classes = {SecurityFixtureApplication.class,
        SecurityChainJointRegressionTestBase.AsyncExecutorConfiguration.class},
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

    /**
     * ZS-SEC-012.B codex r0 P2：提供与生产同名的【池化】taskExecutor——Spring MVC 异步（Callable）
     * 优先取名为 taskExecutor 的 bean；池化使工作线程跨请求复用，配合 job starter 的
     * ZszjAsyncAutoConfiguration（TtlRunnable 装饰 BPP）构成真实的「跨线程上下文传播」路径，
     * 池不传播则交替租户用例必现串号（SimpleAsyncTaskExecutor 每请求新线程会掩盖缺陷）。
     */
    @org.springframework.context.annotation.Configuration
    public static class AsyncExecutorConfiguration {

        @org.springframework.context.annotation.Bean("taskExecutor")
        public org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor taskExecutor() {
            org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor executor =
                    new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor();
            executor.setCorePoolSize(2);
            executor.setMaxPoolSize(2);
            executor.setThreadNamePrefix("fixture-async-");
            executor.initialize();
            return executor;
        }
    }
}
