import cn.zszj.framework.web.core.filter.CacheRequestBodyFilter;
import cn.zszj.framework.web.core.filter.CacheRequestBodyWrapper;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

public class ReviewProbe {
    public static void main(String[] args) throws Exception {
        int passed = 0;
        for (String name : new String[] {
            "cn.zszj.framework.web.core.util.WebFrameworkUtilsTest",
            "cn.zszj.framework.web.core.filter.CacheRequestBodyFilterTest",
            "cn.zszj.server.ValidationContractTest"
        }) {
            Class<?> type = Class.forName(name);
            var ctor = type.getDeclaredConstructor();
            ctor.setAccessible(true);
            for (Method test : type.getDeclaredMethods()) {
                if (!test.isAnnotationPresent(org.junit.jupiter.api.Test.class)) continue;
                Object instance = ctor.newInstance();
                for (Method setup : type.getDeclaredMethods()) {
                    if (setup.isAnnotationPresent(org.junit.jupiter.api.BeforeEach.class)) {
                        setup.setAccessible(true);
                        setup.invoke(instance);
                    }
                }
                test.setAccessible(true);
                test.invoke(instance);
                passed++;
            }
        }
        System.out.println("Existing targeted test methods passed: " + passed);

        int limit = 1024 * 1024;
        byte[] body = ("{\"payload\":\"" + "a".repeat(2 * limit) + "\"}").getBytes(StandardCharsets.UTF_8);
        for (boolean knownLength : new boolean[] {true, false}) {
            MockHttpServletRequest request = knownLength ? new MockHttpServletRequest() : new MockHttpServletRequest() {
                @Override public long getContentLengthLong() { return -1L; }
                @Override public int getContentLength() { return -1; }
            };
            request.setMethod("POST");
            request.setRequestURI("/admin-api/system/auth/login");
            request.setContentType("application/json");
            request.setContent(body);
            if (!knownLength) request.addHeader("Transfer-Encoding", "chunked");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();
            new CacheRequestBodyFilter(limit).doFilter(request, response, chain);
            System.out.println("knownLength=" + knownLength
                + " configuredLimit=" + limit
                + " actualBytes=" + body.length
                + " chainReached=" + (chain.getRequest() != null)
                + " cachedBytes=" + (chain.getRequest() instanceof CacheRequestBodyWrapper ? chain.getRequest().getContentLengthLong() : 0)
                + " response=" + response.getContentAsString());
        }
    }
}