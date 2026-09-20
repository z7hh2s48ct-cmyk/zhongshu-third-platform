package cn.zszj.module.system.enums.permission;

import cn.zszj.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 组织级数据范围枚举类（org 轴）
 *
 * <p>ZS-PERM-002.B（D-09 FND-AUTH-003/004）：与 {@link DataScopeEnum}（dept 轴，部门树）<b>正交并存</b>，
 * 表达 <b>组织树</b>（{@code system_organization.parent_id} 自引用）维度的跨组织授权范围。用于实现组织级别的
 * 数据权限——回答「目标对象所属组织是否落在登录主体的授权组织范围内」。
 *
 * <p>一期口径（D-09，docs/02 §3 line94 / §5.3 line356 PEND-003 采纳 A）：<b>跨组织只允许显式平台角色</b>；
 * 临时跨组织授权（双人审批 + 到期回收）后置，不在本枚举。故 {@link #ORG_ALL} 仅由超级管理员或 PLATFORM 类型
 * 组织任职派生，非平台主体一律收敛到 {@link #ORG_AND_CHILD}（本组织子树）或 {@link #ORG_SELF}（无组织上下文）。
 *
 * @author ZS-PERM-002.B
 */
@Getter
@AllArgsConstructor
public enum OrgDataScopeEnum implements ArrayValuable<Integer> {

    ORG_ALL(1), // 全部组织数据权限（超管 / 显式平台角色）

    ORG_AND_CHILD(3), // 本组织及以下（组织树后代）数据权限
    ORG_ONLY(4), // 仅本组织数据权限（不含后代，保留位）

    ORG_SELF(5); // 仅本人数据权限（无组织上下文的最窄兜底）

    /**
     * 范围
     */
    private final Integer scope;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(OrgDataScopeEnum::getScope).toArray(Integer[]::new);

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
