package cn.zszj.module.system.enums.organization;

import cn.zszj.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 组织类型枚举类
 *
 * <p>ZS-IAM-002（D-09 FND-IAM-002）：组织作为独立业务对象，不由部门表承载全部语义。
 * 平台、品牌、加盟商、门店、供应商、部门以单一 {@code system_organization} 表的 type 区分，
 * 层级由 {@code parent_id} 自引用树表达。既有 {@code system_dept} 保持不动，
 * DEPARTMENT 类型组织经 {@code ref_dept_id} 桥接回填，向后兼容。
 *
 * @author ZS-IAM-002
 */
@Getter
@AllArgsConstructor
public enum OrganizationTypeEnum implements ArrayValuable<Integer> {

    PLATFORM(1, "平台"),
    BRAND(2, "品牌"),
    FRANCHISEE(3, "加盟商"),
    STORE(4, "门店"),
    SUPPLIER(5, "供应商"),
    DEPARTMENT(6, "部门");

    /**
     * 类型值
     */
    private final Integer type;
    /**
     * 类型名
     */
    private final String name;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(OrganizationTypeEnum::getType).toArray(Integer[]::new);

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    public static boolean isDepartment(Integer type) {
        return DEPARTMENT.getType().equals(type);
    }

}
