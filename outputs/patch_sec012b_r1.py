import io

base = r"E:\zszj-wt-sec-012-b"

# 1) filter already rewritten? verify marker
p = base + r"\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\main\java\cn\zszj\framework\apilog\core\filter\ApiAccessLogFilter.java"
s = io.open(p, encoding='utf-8').read()
print('filter has new logic:', 'asyncDispatch' in s)

# 2) rewrite async filter test
test = '''package cn.zszj.framework.apilog.core.filter;

import cn.zszj.framework.common.biz.infra.logger.ApiAccessLogCommonApi;
import cn.zszj.framework.common.biz.infra.logger.dto.ApiAccessLogCreateReqDTO;
import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.framework.web.config.WebProperties;
import jakarta.servlet.DispatcherType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * {@link ApiAccessLogFilter} 异步请求日志行为的单元测试（ZS-SEC-012.B codex r0/r1 P1）。
 *
 * <p>合同：①异步请求首次派发返回时【不得】记录（结果未定，必然伪报成功）；②ASYNC 派发内
 * 以最终结果记录，且全程恰好一条（幂等护栏防二次派发重复）；③异步派发失败（无 CommonResult
 * 且状态 >= 400）不得伪报成功；④同步请求立即记录的行为不变。
 *
 * <p>设计说明（codex r1 P1）：记录发生在【ASYNC 派发内】而非 AsyncListener 回调——
 * 回调在租户上下文清理后执行会使日志归属 tenant_id=0；ASYNC 派发内 TenantContextWebFilter
 * 已从 tenant-id 头重建上下文，租户归属正确。
 *
 * @author ZS-SEC-012.B
 */
public class ApiAccessLogFilterAsyncTest {

    private ApiAccessLogCommonApi apiAccessLogApi;
    private ApiAccessLogFilter filter;

    @BeforeEach
    public void setUp() {
        apiAccessLogApi = mock(ApiAccessLogCommonApi.class);
        WebProperties webProperties = new WebProperties();
        filter = new ApiAccessLogFilter(webProperties, "fixture-app", apiAccessLogApi);
        // WebFrameworkUtils 静态 properties（getLoginUserType 依赖）在非 Spring 环境需手动注入
        org.springframework.test.util.ReflectionTestUtils.setField(
                cn.zszj.framework.web.core.util.WebFrameworkUtils.class, "properties", webProperties);
    }

    private MockHttpServletRequest request(String uri, DispatcherType type) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.addHeader("tenant-id", 1L);
        request.setAsyncSupported(true);
        request.setDispatcherType(type);
        return request;
    }

    /** 模拟异步处理器：写入结果属性并启动异步（首次派发返回后仍未完成） */
    private static FilterChain startAsyncChain() {
        return (req, res) -> {
            req.setAttribute("common_result", CommonResult.success("ok"));
            req.startAsync(req, res);
        };
    }

    @Test
    public void asyncInitialDispatch_notLogged() throws Exception {
        MockHttpServletRequest request = request("/admin-api/fixture/async/profile", DispatcherType.REQUEST);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, startAsyncChain());

        verify(apiAccessLogApi, times(0)).createApiAccessLogAsync(any());
    }

    @Test
    public void asyncDispatch_loggedOnceWithFinalResult() throws Exception {
        MockHttpServletRequest initial = request("/admin-api/fixture/async/profile", DispatcherType.REQUEST);
        filter.doFilter(initial, new MockHttpServletResponse(), startAsyncChain());
        verify(apiAccessLogApi, times(0)).createApiAccessLogAsync(any());

        MockHttpServletRequest asyncReq = request("/admin-api/fixture/async/profile", DispatcherType.ASYNC);
        asyncReq.setAttribute("common_result", CommonResult.success("ok"));
        filter.doFilter(asyncReq, new MockHttpServletResponse(), (req, res) -> { });

        ArgumentCaptor<ApiAccessLogCreateReqDTO> captor = ArgumentCaptor.forClass(ApiAccessLogCreateReqDTO.class);
        verify(apiAccessLogApi, times(1)).createApiAccessLogAsync(captor.capture());
        assertEquals(0L, captor.getValue().getResultCode().longValue(), "ASYNC 派发必须以最终成功码记录");
    }

    @Test
    public void asyncDispatchFailure_notReportedAsSuccess() throws Exception {
        MockHttpServletRequest asyncReq = request("/admin-api/fixture/async/error", DispatcherType.ASYNC);
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(500);

        filter.doFilter(asyncReq, response, (req, res) -> { });

        ArgumentCaptor<ApiAccessLogCreateReqDTO> captor = ArgumentCaptor.forClass(ApiAccessLogCreateReqDTO.class);
        verify(apiAccessLogApi, times(1)).createApiAccessLogAsync(captor.capture());
        assertTrue(captor.getValue().getResultCode() == null || captor.getValue().getResultCode() != 0L,
                "异步派发失败不得伪报成功，实际: " + captor.getValue().getResultCode());
    }

    @Test
    public void asyncRedispatch_logStillExactlyOnce() throws Exception {
        MockHttpServletRequest initial = request("/admin-api/fixture/async/profile", DispatcherType.REQUEST);
        filter.doFilter(initial, new MockHttpServletResponse(), startAsyncChain());

        MockHttpServletRequest asyncReq = request("/admin-api/fixture/async/profile", DispatcherType.ASYNC);
        asyncReq.setAttribute("common_result", CommonResult.success("ok"));
        filter.doFilter(asyncReq, new MockHttpServletResponse(), (req, res) -> { });
        filter.doFilter(asyncReq, new MockHttpServletResponse(), (req, res) -> { });

        ArgumentCaptor<ApiAccessLogCreateReqDTO> captor = ArgumentCaptor.forClass(ApiAccessLogCreateReqDTO.class);
        verify(apiAccessLogApi, times(1)).createApiAccessLogAsync(captor.capture());
        assertEquals(0L, captor.getValue().getResultCode().longValue());
    }

    @Test
    public void syncRequest_logImmediately_unchanged() throws Exception {
        MockHttpServletRequest request = request("/admin-api/fixture/public/hello", DispatcherType.REQUEST);
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setAttribute("common_result", CommonResult.success("ok"));

        filter.doFilter(request, response, (req, res) -> { });

        ArgumentCaptor<ApiAccessLogCreateReqDTO> captor = ArgumentCaptor.forClass(ApiAccessLogCreateReqDTO.class);
        verify(apiAccessLogApi, times(1)).createApiAccessLogAsync(captor.capture());
        assertEquals(0L, captor.getValue().getResultCode().longValue());
    }
}
'''
io.open(base + r"\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\java\cn\zszj\framework\apilog\core\filter\ApiAccessLogFilterAsyncTest.java", 'w', encoding='utf-8', newline='\n').write(test)
print('test rewritten')

# 3) client script: comment + cross-platform
p = base + r"\scripts\ops\run-client-contract-tests.mjs"
s = io.open(p, encoding='utf-8').read()
s = s.replace(" * 依赖：apps/*/node_modules 已由 pnpm install 安装（CI 由 ZS-CLIENT-005.A 流水线负责）。",
              " * 依赖：各 app 的 node_modules 已由 pnpm install 安装（CI 由 ZS-CLIENT-005.A 流水线负责）。")
s = s.replace("  const r = spawnSync('npx', ['vitest', 'run'], { cwd: join(root, app.dir), encoding: 'utf8', stdio: 'inherit' });",
              "  // codex r1 P2：跨平台——Windows 下 npx 是 npx.cmd，spawnSync 不经 shell 无法运行；改为 node 直启本地 vitest 入口\n"
              "  const vitestEntry = join(root, app.dir, 'node_modules', 'vitest', 'vitest.mjs');\n"
              "  const r = spawnSync(process.execPath, [vitestEntry, 'run'], { cwd: join(root, app.dir), encoding: 'utf8', stdio: 'inherit' });")
io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('client script ok')

# 4) G12 mvnArgs + module list add web module + async test
p = base + r"\scripts\ops\run-local-gates.mjs"
s = io.open(p, encoding='utf-8').read()
old = "    mvnArgs: '-pl zszj-framework/zszj-spring-boot-starter-biz-tenant -am -Dtest=SecurityFilterChainFixtureTest,CrossTenantVisitEnabledFixtureTest,SecurityChainJointRegressionTest,SecurityChainEmbeddedCorsTest -Dsurefire.failIfNoSpecifiedTests=false test' },"
new = "    mvnArgs: '-pl zszj-framework/zszj-spring-boot-starter-web,zszj-framework/zszj-spring-boot-starter-biz-tenant -am -Dtest=SecurityFilterChainFixtureTest,CrossTenantVisitEnabledFixtureTest,SecurityChainJointRegressionTest,SecurityChainEmbeddedCorsTest,ApiAccessLogFilterAsyncTest -Dsurefire.failIfNoSpecifiedTests=false test' },"
assert old in s
s = s.replace(old, new)
io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('G12 ok')

# 5) CI workflow: add async test + web module
p = base + r"\.github\workflows\local-baseline-gates.yml"
s = io.open(p, encoding='utf-8').read()
s = s.replace("-Dtest=SecurityFilterChainFixtureTest,CrossTenantVisitEnabledFixtureTest,SecurityChainJointRegressionTest,SecurityChainEmbeddedCorsTest",
              "-Dtest=SecurityFilterChainFixtureTest,CrossTenantVisitEnabledFixtureTest,SecurityChainJointRegressionTest,SecurityChainEmbeddedCorsTest,ApiAccessLogFilterAsyncTest")
s = s.replace("mvn -pl zszj-framework/zszj-spring-boot-starter-biz-tenant -am",
              "mvn -pl zszj-framework/zszj-spring-boot-starter-web,zszj-framework/zszj-spring-boot-starter-biz-tenant -am")
io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('CI ok')

# 6) fixture yaml: un-exclude ZszjAsyncAutoConfiguration (TTL BPP) if excluded
p = base + r"\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-biz-tenant\src\test\resources\application-fixture.yaml"
s = io.open(p, encoding='utf-8').read()
print('yaml excludes ZszjAsyncAutoConfiguration:', 'ZszjAsyncAutoConfiguration' in s)
