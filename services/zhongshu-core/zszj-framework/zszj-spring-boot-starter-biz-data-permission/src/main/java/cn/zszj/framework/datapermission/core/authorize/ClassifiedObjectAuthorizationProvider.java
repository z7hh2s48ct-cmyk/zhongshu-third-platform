package cn.zszj.framework.datapermission.core.authorize;

import java.util.Collections;
import java.util.Map;

/**
 * 带「字段等级目录」的对象授权扩展点（ZS-PERM-003.B，D-12 敏感业务字段目录）。
 *
 * <p>在 {@link ObjectAuthorizationProvider} 候选字段之上声明分级目录：显式编目
 * {@link #getFieldLevels()}（字段 → 等级）+ {@link #getDomainDefaultLevel()} 域级默认
 * （未编目字段的兜底等级，支持「目录先改、代码后接」的回填制）。裁决服务据此按访问者可读上限
 * 裁剪 {@code authorizedFields} 并输出 {@code maskedFields}（F2 脱敏可见）。
 *
 * <p>分级契约（D-12）：
 * <ul>
 *     <li>字段等级 ≤ 访问者上限 → 清晰可见；</li>
 *     <li>F2 且访问者上限低于 F2 → 脱敏可见（进入 authorizedFields + maskedFields）；</li>
 *     <li>F3（或高于上限的其他等级）→ 拒绝输出（不进 authorizedFields，执行侧 fail-closed）。</li>
 * </ul>
 *
 * <p>注册校验：每个非空白候选字段必须有显式编目或域级默认，否则装配 fail-fast。
 *
 * @author ZS-PERM-003.B
 */
public interface ClassifiedObjectAuthorizationProvider extends ObjectAuthorizationProvider {

    /**
     * 显式字段编目（字段名 → 等级）；键不得空白、值不得 null（装配校验）。
     * 编目可先于业务字段存在（目录先改、代码后接），未编目候选字段落域级默认。
     */
    default Map<String, FieldLevel> getFieldLevels() {
        return Collections.emptyMap();
    }

    /**
     * 域级默认等级（D-12 §5.1：B 类域只批域级默认，字段随域设计回填）；null 表示无域级默认。
     */
    default FieldLevel getDomainDefaultLevel() {
        return null;
    }

}
