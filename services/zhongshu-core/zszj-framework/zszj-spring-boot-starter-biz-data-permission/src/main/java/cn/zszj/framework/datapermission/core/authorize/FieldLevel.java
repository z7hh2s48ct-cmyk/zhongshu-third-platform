package cn.zszj.framework.datapermission.core.authorize;

/**
 * 字段等级（F0～F3，D-12 敏感业务字段目录 §2）。
 *
 * <ul>
 *     <li>{@link #F0} 公开级：授权范围内所有角色可见；</li>
 *     <li>{@link #F1} 内部级：授权范围内可见，跨组织/低角色默认隐藏；</li>
 *     <li>{@link #F2} 敏感级：限组织负责人及以上管理角色，低角色默认脱敏可见（非整字段隐藏）；</li>
 *     <li>{@link #F3} 机密级：仅平台角色或显式授权，默认拒绝读取（不泄露存在性）。</li>
 * </ul>
 *
 * @author ZS-PERM-003.B
 */
public enum FieldLevel {

    F0(0),
    F1(1),
    F2(2),
    F3(3);

    private final int rank;

    FieldLevel(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }

    /**
     * 字段等级是否不超过访问者可读上限（含等号，即清晰可见）。
     */
    public boolean isWithin(FieldLevel maxLevel) {
        return maxLevel != null && rank <= maxLevel.rank;
    }

}
