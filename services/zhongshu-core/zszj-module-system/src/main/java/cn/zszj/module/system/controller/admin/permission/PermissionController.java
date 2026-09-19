package cn.zszj.module.system.controller.admin.permission;

import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.module.system.controller.admin.permission.vo.PermissionIdListRespVO;
import cn.zszj.module.system.controller.admin.permission.vo.permission.PermissionAssignRoleDataScopeReqVO;
import cn.zszj.module.system.controller.admin.permission.vo.permission.PermissionAssignRoleMenuReqVO;
import cn.zszj.module.system.controller.admin.permission.vo.permission.PermissionAssignUserRoleReqVO;
import cn.zszj.module.system.service.permission.PermissionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.Set;

import static cn.zszj.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 权限")
@RestController
@RequestMapping("/system/permission")
public class PermissionController {

    @Resource
    private PermissionService permissionService;
    @Operation(summary = "获得角色拥有的菜单编号")
    @Parameter(name = "roleId", description = "角色编号", required = true)
    @GetMapping("/list-role-menus")
    @PreAuthorize("@ss.hasPermission('system:permission:assign-role-menu')")
    public CommonResult<PermissionIdListRespVO> getRoleMenuList(@RequestParam("roleId") Long roleId) {
        // ZS-SEC-009.B r1 P2-B：裸 Set<Long> 经 CommonResult.data（泛型擦除为 Object）不受 ID→string 合同
        // 作用，wire 仍 number 与前端 string 选项失配——包装为显式 menuIds 字段命中命名约定
        return success(new PermissionIdListRespVO(permissionService.getRoleMenuListByRoleId(roleId), null));
    }

    @PostMapping("/assign-role-menu")
    @Operation(summary = "赋予角色菜单")
    @PreAuthorize("@ss.hasPermission('system:permission:assign-role-menu')")
    public CommonResult<Boolean> assignRoleMenu(@Validated @RequestBody PermissionAssignRoleMenuReqVO reqVO) {
        // ZS-CFG-003.B GAP-3：移除上游项目遗留的 handleTenantMenu 套餐静默过滤（removeIf 丢弃套餐外菜单）。
        // 静默过滤会在服务端校验之前吞掉越界菜单，把「越界显式拒绝（TENANT_PACKAGE_MENU_EXCEED，见
        // PermissionServiceImpl#validateMenusInTenantPackage）」降级为「部分成功」——请求 code=0 但越界菜单
        // 未写入，违反 ZS-CFG-003.B「套餐回收后直调拒绝」的安全合同（SYS-001.A 真实 HTTP 回归 SYS-ROLE-N1 实证）。
        // 租户/套餐交集约束由服务端 @Transactional 内 validateMenusInTenantPackage 显式拒绝，系统租户不受影响。
        permissionService.assignRoleMenu(reqVO.getRoleId(), reqVO.getMenuIds());
        return success(true);
    }

    @PostMapping("/assign-role-data-scope")
    @Operation(summary = "赋予角色数据权限")
    @PreAuthorize("@ss.hasPermission('system:permission:assign-role-data-scope')")
    public CommonResult<Boolean> assignRoleDataScope(@Valid @RequestBody PermissionAssignRoleDataScopeReqVO reqVO) {
        permissionService.assignRoleDataScope(reqVO.getRoleId(), reqVO.getDataScope(), reqVO.getDataScopeDeptIds());
        return success(true);
    }

    @Operation(summary = "获得管理员拥有的角色编号列表")
    @Parameter(name = "userId", description = "用户编号", required = true)
    @GetMapping("/list-user-roles")
    @PreAuthorize("@ss.hasPermission('system:permission:assign-user-role')")
    public CommonResult<PermissionIdListRespVO> listAdminRoles(@RequestParam("userId") Long userId) {
        // 同上：包装为显式 roleIds 字段（ZS-SEC-009.B r1 P2-B）
        return success(new PermissionIdListRespVO(null, permissionService.getUserRoleIdListByUserId(userId)));
    }

    @Operation(summary = "赋予用户角色")
    @PostMapping("/assign-user-role")
    @PreAuthorize("@ss.hasPermission('system:permission:assign-user-role')")
    public CommonResult<Boolean> assignUserRole(@Validated @RequestBody PermissionAssignUserRoleReqVO reqVO) {
        permissionService.assignUserRole(reqVO.getUserId(), reqVO.getRoleIds());
        return success(true);
    }

}
