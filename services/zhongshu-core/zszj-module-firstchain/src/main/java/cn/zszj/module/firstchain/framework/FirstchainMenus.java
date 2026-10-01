package cn.zszj.module.firstchain.framework;

import java.util.Set;

/**
 * 首链业务菜单合同（ZS-FC-001 默认授权 wave；接入合同 §1.1 第⑤步「默认授权绑菜单」落点）。
 *
 * <p>本类是 {@code V20261001.001__firstchain_menus.sql} 迁移与开通服务
 * （{@code FirstchainDefaultRoleRegistry}）之间的<b>编号合同唯一事实源</b>：迁移按本表插入
 * {@code system_menu}，开通服务按本表给默认角色绑菜单——两侧不得漂移（漂移即菜单挂空授权）。
 * ID 段 5300~5329 为 firstchain 模块专用登记（基线最大菜单 ID 5010，段内无冲突）。
 *
 * <p>M2 三角色矩阵的菜单面：平台运营（申请管理 + 线索下发）经角色管理界面授予（超管豁免外
 * 由租户管理员分配，不作种子）；加盟商负责人/员工两套默认模板角色在开通时由服务端绑定
 * {@link #LEADER_ROLE_MENUS}/{@link #MEMBER_ROLE_MENUS}。
 *
 * @author ZS-FC-001
 */
public final class FirstchainMenus {

    // ========== 目录 / 菜单 ==========

    /** 目录：首链业务 */
    public static final long MENU_FIRSTCHAIN_ROOT = 5300L;

    /** 菜单：加盟商申请（平台运营审批面；firstchain:application:query） */
    public static final long MENU_APPLICATION = 5301L;

    /** 菜单：线索管理（三角色工作台首页面；firstchain:lead:query） */
    public static final long MENU_LEAD = 5302L;

    /** 菜单：员工账号（加盟商负责人面；页面随 FC-003 工作台交付） */
    public static final long MENU_EMPLOYEE = 5303L;

    // ========== 按钮（type=3） ==========

    public static final long BUTTON_APPLICATION_CREATE = 5311L;
    public static final long BUTTON_APPLICATION_SUBMIT = 5312L;
    public static final long BUTTON_APPLICATION_APPROVE = 5313L;
    public static final long BUTTON_APPLICATION_REJECT = 5314L;
    public static final long BUTTON_APPLICATION_WITHDRAW = 5315L;
    public static final long BUTTON_LEAD_DISTRIBUTE = 5316L;
    public static final long BUTTON_LEAD_ASSIGN = 5317L;
    public static final long BUTTON_LEAD_CLAIM = 5318L;
    public static final long BUTTON_LEAD_REASSIGN = 5319L;
    public static final long BUTTON_LEAD_FOLLOWUP = 5320L;
    public static final long BUTTON_LEAD_CONVERT = 5321L;
    public static final long BUTTON_LEAD_INVALIDATE = 5322L;
    public static final long BUTTON_EMPLOYEE_CREATE = 5323L;

    // ========== 默认模板角色的菜单面（D-07 M2；申请审批面不授加盟商侧） ==========

    /** 加盟商负责人：首链业务 + 线索管理（分配/领取/改派/跟进/转商机/无效）+ 员工账号创建 */
    public static final Set<Long> LEADER_ROLE_MENUS = Set.of(
            MENU_FIRSTCHAIN_ROOT, MENU_LEAD, MENU_EMPLOYEE,
            BUTTON_LEAD_ASSIGN, BUTTON_LEAD_CLAIM, BUTTON_LEAD_REASSIGN,
            BUTTON_LEAD_FOLLOWUP, BUTTON_LEAD_CONVERT, BUTTON_LEAD_INVALIDATE,
            BUTTON_EMPLOYEE_CREATE);

    /** 加盟商员工：首链业务 + 线索管理（领取/跟进/转商机/无效；无分配/改派/员工创建） */
    public static final Set<Long> MEMBER_ROLE_MENUS = Set.of(
            MENU_FIRSTCHAIN_ROOT, MENU_LEAD,
            BUTTON_LEAD_CLAIM, BUTTON_LEAD_FOLLOWUP, BUTTON_LEAD_CONVERT, BUTTON_LEAD_INVALIDATE);

    private FirstchainMenus() {
    }

}
