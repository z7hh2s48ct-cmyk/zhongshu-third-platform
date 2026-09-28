package cn.zszj.framework.datapermission.core.authorize;

import cn.zszj.framework.common.biz.system.permission.PermissionCommonApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 访问者可读字段等级上限解析器（ZS-PERM-003.B，D-12 §3 等级×角色默认映射）。
 *
 * @author ZS-PERM-003.B
 */
@RequiredArgsConstructor
@Slf4j
public class FieldLevelScopeResolver {

    private final PermissionCommonApi permissionApi;

    /**
     * 解析登录主体对目标组织对象的可读字段等级上限（RED 骨架：恒 F1，待 GREEN 实现）。
     */
    public FieldLevel resolveMaxLevel(Long orgId) {
        return FieldLevel.F1;
    }

}
