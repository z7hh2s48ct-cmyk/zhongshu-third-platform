package cn.zszj.framework.datapermission.core.authorize;

import java.util.Collection;
import java.util.Collections;

/**
 * 业务对象的「动作 / 字段」授权扩展点（SPI，ZS-PERM-003.A）。
 *
 * <p>各业务域按对象类型实现本接口并注册为 Bean，声明该对象的候选动作、候选字段与状态维约束；
 * {@link ObjectAuthorizationService} 将候选与登录主体的功能权限（RBAC / visit 收敛）取交集，输出
 * {@link ObjectAuthorizationRespDTO}，同一裁决核心供执行侧实时校验复用（输出与执行共用服务端策略）。
 *
 * <p>默认实现语义（渐进接入，未覆写即零变化）：候选动作/字段默认空集（空=该维不输出/未启用）；
 * 状态钩子默认放行。字段等级目录（F0～F3）与具体商业字段名归 ZS-PERM-003.B，本接口不定义任何等级。
 *
 * @author ZS-PERM-003.A
 */
public interface ObjectAuthorizationProvider {

    /**
     * 对象类型标识（非空白且同一容器内唯一，空白/重复注册使装配 fail-fast）。
     */
    String getObjectType();

    /**
     * 候选动作集合（对象维可见时的待裁剪动作全集）；默认空集（空=未启用动作输出）。
     */
    default Collection<String> getCandidateActions() {
        return Collections.emptyList();
    }

    /**
     * 候选字段集合；非空表示启用字段级输出，默认空集（未启用）。
     */
    default Collection<String> getCandidateFields() {
        return Collections.emptyList();
    }

    /**
     * 状态维钩子：对象当前状态下该动作是否可执行；默认放行（不施加状态约束）。
     *
     * @param action 候选动作
     * @param object 业务对象，可为 null（调用方未提供对象实例时不施加状态约束）
     */
    default boolean isActionAllowedInStatus(String action, Object object) {
        return true;
    }

}
