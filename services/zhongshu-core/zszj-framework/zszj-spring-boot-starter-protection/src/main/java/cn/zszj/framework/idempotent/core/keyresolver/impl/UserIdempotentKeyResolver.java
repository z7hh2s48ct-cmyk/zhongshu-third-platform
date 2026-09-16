package cn.zszj.framework.idempotent.core.keyresolver.impl;

import cn.hutool.crypto.SecureUtil;
import cn.zszj.framework.idempotent.core.annotation.Idempotent;
import cn.zszj.framework.idempotent.core.keyresolver.IdempotentKeyResolver;
import cn.zszj.framework.web.core.util.WebFrameworkUtils;
import org.aspectj.lang.JoinPoint;

/**
 * 用户级别的幂等 Key 解析器，使用方法名 + userId + userType，组装成一个稳定短键
 *
 * 为了避免 Key 过长，使用 MD5 进行“压缩”
 *
 * ZS-SEC-011.B（Key/Value 职责切分，与 {@link DefaultIdempotentKeyResolver} 同批）：
 * Key 不再烘入方法参数——入参差异由切面层的「未截断脱敏摘要」全量参与比对，
 * 「同键异参冲突检测」在本解析器路径同样可达。
 * 已知边界（与 .A 一致，登记不改）：本解析器无租户作用域（跨租户同 userId 共用键空间），
 * 与 Expression 解析器缺租户作用域同归 REC-1 统一抽取。
 *
 * @author 芋道源码
 */
public class UserIdempotentKeyResolver implements IdempotentKeyResolver {

    @Override
    public String resolver(JoinPoint joinPoint, Idempotent idempotent) {
        String methodName = joinPoint.getSignature().toString();
        Long userId = WebFrameworkUtils.getLoginUserId();
        Integer userType = WebFrameworkUtils.getLoginUserType();
        return SecureUtil.md5(methodName + ":" + userId + ":" + userType);
    }

}
