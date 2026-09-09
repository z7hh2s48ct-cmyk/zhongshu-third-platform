package cn.zszj.framework.security.fixture;

import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * ZS-SEC-002：/app-api（MEMBER 主体）测试 Controller。
 *
 * <p>背景：现有夹具端点全部位于 /admin-api（ADMIN 主体），{@code WebFrameworkUtils.getLoginUserType} 依
 * servletPath 前缀推导 userType 的「/app-api → MEMBER」分支从未被运行时验证。SEC-002 的静态接口清单同时
 * 分类 admin-api(ADMIN) 与 app-api(MEMBER) 端点，故需运行时证明该前缀→主体推导对 MEMBER 侧同样成立。
 *
 * <p>本 Controller 提供一个受保护（需登录、非 {@code @PermitAll}）的 /app-api 端点，用于验证 ADMIN/MEMBER
 * Token 双向不能串用：ADMIN token 访问 /app-api → 403（用户类型不匹配）；MEMBER token → 200（正确主体放行）。
 * 与 {@link SecurityFilterChainFixtureTest} 中 MEMBER token 访问 /admin-api → 403 构成完整的双向串用矩阵。
 */
@RestController
@RequestMapping("/app-api/fixture")
public class TestAppController {

    /**
     * 受保护的 MEMBER 侧端点：仅需登录（无 @PreAuthorize），返回当前登录主体信息。
     * userType 由 /app-api 前缀推导为 MEMBER，与 ADMIN token 不匹配时在 TokenAuthenticationFilter 即被拒。
     */
    @GetMapping("/member/profile")
    public CommonResult<Map<String, Object>> memberProfile() {
        LoginUser user = SecurityFrameworkUtils.getLoginUser();
        Map<String, Object> profile = new HashMap<>();
        profile.put("userId", user != null ? user.getId() : null);
        profile.put("userType", user != null ? user.getUserType() : null);
        profile.put("tenantId", user != null ? user.getTenantId() : null);
        return CommonResult.success(profile);
    }
}
