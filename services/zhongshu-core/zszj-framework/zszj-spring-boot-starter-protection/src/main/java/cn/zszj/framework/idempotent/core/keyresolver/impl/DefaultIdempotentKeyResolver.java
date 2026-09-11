package cn.zszj.framework.idempotent.core.keyresolver.impl;

import cn.hutool.crypto.SecureUtil;
import cn.zszj.framework.common.util.servlet.ServletUtils;
import cn.zszj.framework.common.util.string.StrUtils;
import cn.zszj.framework.idempotent.core.annotation.Idempotent;
import cn.zszj.framework.idempotent.core.keyresolver.IdempotentKeyResolver;
import cn.zszj.framework.web.core.util.WebFrameworkUtils;
import org.aspectj.lang.JoinPoint;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 默认（全局级别）幂等 Key 解析器，使用方法名 + 方法参数 + 租户 + 用户主体，组装成一个 Key
 *
 * 为了避免 Key 过长，使用 MD5 进行"压缩"
 *
 * ZS-SEC-011.A：加入租户 + 用户主体作用域，实现主体隔离（不同主体/租户不误用他人幂等结果）
 *
 * 作用域来源说明（与 SEC-010 同深度）：租户/用户因子取自当前请求上下文
 * （{@link ServletUtils#getRequest()} + {@link WebFrameworkUtils} 的 getTenantId/getLoginUserId/getLoginUserType）——
 * 租户来自 tenant-id 请求头（与 TenantContextHolder 同源，受 TenantSecurityWebFilter 校验，不可伪造成他人租户），
 * userId/userType 来自权威 request attribute（不可伪造）；
 * 无请求上下文（如异步/定时任务）时三者均以 null 占位拼接，不抛 NPE。
 * 注意：省略 tenant-id 头会得到不同 Key（同主体自绕过防重窗口，非跨主体越权）；
 * 且因 argsStr 已烘入 Key，同键必然同参，切面的「同键异参冲突」分支对本解析器恒不可达。
 * 上述缺口的权威租户源可注入 port 与 SubjectScope 统一抽取归 REC-1（详见 IdempotentAspect 类注释的已知缺口登记）。
 *
 * @author 芋道源码
 */
public class DefaultIdempotentKeyResolver implements IdempotentKeyResolver {

    @Override
    public String resolver(JoinPoint joinPoint, Idempotent idempotent) {
        String methodName = joinPoint.getSignature().toString();
        String argsStr = StrUtils.joinMethodArgs(joinPoint);
        // ZS-SEC-011.A：加租户 + 主体作用域，实现主体隔离（不同主体/租户不误用他人幂等结果）
        HttpServletRequest request = ServletUtils.getRequest();
        Long tenantId = request != null ? WebFrameworkUtils.getTenantId(request) : null;
        Long userId = request != null ? WebFrameworkUtils.getLoginUserId(request) : null;
        Integer userType = request != null ? WebFrameworkUtils.getLoginUserType(request) : null;
        return SecureUtil.md5(methodName + ":" + tenantId + ":" + userId + ":" + userType + ":" + argsStr);
    }

}
