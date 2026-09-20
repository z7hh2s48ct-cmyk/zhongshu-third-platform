package cn.zszj.framework.common.biz.system.permission;

import cn.zszj.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.zszj.framework.common.biz.system.permission.dto.OrgDataPermissionRespDTO;

/**
 * 权限 API 接口
 *
 * @author 芋道源码
 */
public interface PermissionCommonApi {

    /**
     * 判断是否有权限，任一一个即可
     *
     * @param userId 用户编号
     * @param permissions 权限
     * @return 是否
     */
    boolean hasAnyPermissions(Long userId, String... permissions);

    /**
     * 判断是否有角色，任一一个即可
     *
     * @param userId 用户编号
     * @param roles 角色数组
     * @return 是否
     */
    boolean hasAnyRoles(Long userId, String... roles);

    /**
     * 获得登陆用户的部门数据权限
     *
     * @param userId 用户编号
     * @return 部门数据权限
     */
    DeptDataPermissionRespDTO getDeptDataPermission(Long userId);

    /**
     * 获得登陆用户的组织（跨组织）数据权限（ZS-PERM-002.B，org 轴）
     *
     * <p>与 {@link #getDeptDataPermission} 的 dept/self 轴正交并存：本方法产出组织树维度的授权范围
     * （全部组织 / 本组织子树 / 仅本人），供对象级检查入口判定「已知他组织对象 ID」的越界拒绝。
     * 一期跨组织只允许显式平台角色（D-09）。
     *
     * @param userId 用户编号
     * @return 组织数据权限
     */
    OrgDataPermissionRespDTO getOrgDataPermission(Long userId);

}
