package cn.zszj.module.system.service.tenant;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.common.util.collection.CollectionUtils;
import cn.zszj.framework.common.util.date.DateUtils;
import cn.zszj.framework.common.util.object.BeanUtils;
import cn.zszj.framework.datapermission.core.annotation.DataPermission;
import cn.zszj.framework.tenant.config.TenantProperties;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.framework.tenant.core.util.TenantUtils;
import cn.zszj.module.system.controller.admin.permission.vo.role.RoleSaveReqVO;
import cn.zszj.module.system.controller.admin.tenant.vo.tenant.TenantPageReqVO;
import cn.zszj.module.system.controller.admin.tenant.vo.tenant.TenantSaveReqVO;
import cn.zszj.module.system.convert.tenant.TenantConvert;
import cn.zszj.module.system.dal.dataobject.permission.MenuDO;
import cn.zszj.module.system.dal.dataobject.permission.RoleDO;
import cn.zszj.module.system.dal.dataobject.tenant.TenantDO;
import cn.zszj.module.system.dal.dataobject.tenant.TenantPackageDO;
import cn.zszj.module.system.dal.mysql.tenant.TenantMapper;
import cn.zszj.module.system.dal.mysql.tenant.TenantPackageMapper;
import cn.zszj.module.system.enums.permission.RoleCodeEnum;
import cn.zszj.module.system.enums.permission.RoleTypeEnum;
import cn.zszj.module.system.service.permission.MenuService;
import cn.zszj.module.system.service.permission.PermissionService;
import cn.zszj.module.system.service.permission.RoleService;
import cn.zszj.module.system.service.tenant.handler.TenantInfoHandler;
import cn.zszj.module.system.service.tenant.handler.TenantMenuHandler;
import cn.zszj.module.system.service.user.AdminUserService;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.system.enums.ErrorCodeConstants.*;
import static java.util.Collections.singleton;

/**
 * 租户 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class TenantServiceImpl implements TenantService {

    @SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
    @Autowired(required = false) // 由于 zszj.tenant.enable 配置项，可以关闭多租户的功能，所以这里只能不强制注入
    private TenantProperties tenantProperties;

    @Resource
    private TenantMapper tenantMapper;
    @Resource
    private TenantPackageMapper tenantPackageMapper;

    @Resource
    private TenantPackageService tenantPackageService;
    @Resource
    @Lazy // 延迟，避免循环依赖报错
    private AdminUserService userService;
    @Resource
    private RoleService roleService;
    @Resource
    private MenuService menuService;
    @Resource
    private PermissionService permissionService;

    @Override
    public List<Long> getTenantIdList() {
        List<TenantDO> tenants = tenantMapper.selectList();
        return CollectionUtils.convertList(tenants, TenantDO::getId);
    }

    @Override
    public void validTenant(Long id) {
        TenantDO tenant = getTenant(id);
        if (tenant == null) {
            throw exception(TENANT_NOT_EXISTS);
        }
        if (tenant.getStatus().equals(CommonStatusEnum.DISABLE.getStatus())) {
            throw exception(TENANT_DISABLE, tenant.getName());
        }
        if (DateUtils.isExpired(tenant.getExpireTime())) {
            throw exception(TENANT_EXPIRE, tenant.getName());
        }
    }

    @Override
    // ZS-PERM-004.A codex r0 P1：改用 Spring @Transactional——外层 @DSTransactional 下 DS ConnectionProxy.commit()
    // 为空操作，内层 Spring 事务提交驱逐缓存而 DB 要等外层 DS 结束才提交，留有「他连接回填旧授权、外层提交后无最终驱逐」窗口；
    // system 模块单库，统一 Spring 事务生命周期
    // ZS-PERM-004.A codex r1 P1：createTenant 原也有 @DSTransactional，替换注解时此处遗漏——
    // 必须补 @Transactional，否则租户插入与内部角色/授权操作分段提交，中途失败留下不完整租户
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false) // 参见 https://gitee.com/zhijiantianya/ruoyi-vue-pro/pulls/1154 说明
    public Long createTenant(TenantSaveReqVO createReqVO) {
        // 校验租户名称是否重复
        validTenantNameDuplicate(createReqVO.getName(), null);
        // 校验租户域名是否重复
        validTenantWebsiteDuplicate(createReqVO.getWebsites(), null);
        // 校验套餐被禁用
        TenantPackageDO tenantPackage = tenantPackageService.validTenantPackage(createReqVO.getPackageId());
        // ZS-CFG-003.B codex r1 P1：创建租户同样先取套餐行锁——租户尚未绑定、不会出现在收缩枚举中，
        // 若并发套餐收缩，管理员角色必须按收缩后的套餐菜单授予
        // codex r3 P2：使用锁定查询返回的套餐（收缩/扩大后的最新菜单）完成管理员授权；锁定读为空即拒绝
        TenantPackageDO lockedPackage = tenantPackageMapper.selectByIdForUpdate(createReqVO.getPackageId());
        if (lockedPackage == null) {
            throw exception(TENANT_PACKAGE_NOT_EXISTS);
        }
        tenantPackage = lockedPackage;
        // lambda 引用需 effectively-final：以新变量承载锁定套餐
        TenantPackageDO finalLockedPackage = lockedPackage;

        // 创建租户
        TenantDO tenant = BeanUtils.toBean(createReqVO, TenantDO.class);
        tenantMapper.insert(tenant);
        // 创建租户的管理员
        TenantUtils.execute(tenant.getId(), () -> {
            // 创建角色
            Long roleId = createRole(finalLockedPackage);
            // 创建用户，并分配角色
            Long userId = createUser(roleId, createReqVO);
            // 修改租户的管理员
            tenantMapper.updateById(new TenantDO().setId(tenant.getId()).setContactUserId(userId));
        });
        return tenant.getId();
    }

    private Long createUser(Long roleId, TenantSaveReqVO createReqVO) {
        // 创建用户
        Long userId = userService.createUser(TenantConvert.INSTANCE.convert02(createReqVO));
        // 分配角色
        permissionService.assignUserRole(userId, singleton(roleId));
        return userId;
    }

    private Long createRole(TenantPackageDO tenantPackage) {
        // 创建角色
        RoleSaveReqVO reqVO = new RoleSaveReqVO();
        reqVO.setName(RoleCodeEnum.TENANT_ADMIN.getName()).setCode(RoleCodeEnum.TENANT_ADMIN.getCode())
                .setSort(0).setRemark("系统自动生成");
        Long roleId = roleService.createRole(reqVO, RoleTypeEnum.SYSTEM.getType());
        // 分配权限
        // ZS-CFG-003.B GAP-3 codex r1 P1：套餐 menu_ids 可能残留已删除菜单（deleteMenu 只清 role_menu），
        // 内部供给路径须先按现有菜单消毒，避免被 assignRoleMenu 的外部伪造拒绝语义（MENU_NOT_EXISTS）误伤开通
        permissionService.assignRoleMenu(roleId, sanitizeExistingMenuIds(tenantPackage.getMenuIds()));
        return roleId;
    }

    // ZS-CFG-003.B GAP-3 codex r1 P1：套餐 menu_ids 可能残留已删除菜单（deleteMenu 只清 role_menu、不维护
    // 套餐不变量）——内部供给/收敛路径传入 assignRoleMenu 前先按现有菜单消毒，避免 MENU_NOT_EXISTS 误伤；
    // 套餐不变量维护（删除/停用菜单时同步清理 package.menu_ids）归 ZS-CFG-003 后续小卡
    private Set<Long> sanitizeExistingMenuIds(Set<Long> menuIds) {
        if (CollUtil.isEmpty(menuIds)) {
            return menuIds;
        }
        Set<Long> existingIds = CollectionUtils.convertSet(menuService.getMenuList(menuIds), MenuDO::getId);
        return CollUtil.intersectionDistinct(menuIds, existingIds);
    }

    @Override
    // ZS-PERM-004.A codex r0 P1：改用 Spring @Transactional——外层 @DSTransactional 下 DS ConnectionProxy.commit()
    // 为空操作，内层 Spring 事务提交驱逐缓存而 DB 要等外层 DS 结束才提交，留有「他连接回填旧授权、外层提交后无最终驱逐」窗口；
    // system 模块单库，统一 Spring 事务生命周期
    @Transactional(rollbackFor = Exception.class)
    public void updateTenant(TenantSaveReqVO updateReqVO) {
        // 校验存在
        TenantDO tenant = validateUpdateTenant(updateReqVO.getId());
        // 校验租户名称是否重复
        validTenantNameDuplicate(updateReqVO.getName(), updateReqVO.getId());
        // 校验租户域名是否重复
        validTenantWebsiteDuplicate(updateReqVO.getWebsites(), updateReqVO.getId());
        // 校验套餐被禁用
        tenantPackageService.validTenantPackage(updateReqVO.getPackageId());
        // ZS-CFG-003.B codex r1 P1：先取【目标套餐行锁】再动租户绑定——与 updateTenantPackage（套餐→租户锁序）
        // 统一，堵「换套餐换入正在收缩的套餐、逃过该租户的收敛」的交错；换出套餐由其自身收缩流程在
        // 锁内重查绑定后跳过（见 TenantPackageServiceImpl）
        // codex r2 P2：使用锁定查询返回的套餐（收缩/扩大后的最新菜单）完成授权
        TenantPackageDO tenantPackage = tenantPackageMapper.selectByIdForUpdate(updateReqVO.getPackageId());
        if (tenantPackage == null) {
            throw exception(TENANT_PACKAGE_NOT_EXISTS);
        }
        // codex r2 P1：锁租户行后读取【当前绑定】——并发换绑提交后，锁前快照已失效，
        // 收敛判断与绑定写回必须以锁内数据为准
        TenantDO lockedTenant = tenantMapper.selectByIdForUpdate(updateReqVO.getId());
        if (lockedTenant == null) {
            throw exception(TENANT_NOT_EXISTS);
        }

        // 更新租户
        TenantDO updateObj = BeanUtils.toBean(updateReqVO, TenantDO.class);
        tenantMapper.updateById(updateObj);
        // 如果套餐发生变化（以锁内绑定为准），则修改其角色的权限
        if (ObjectUtil.notEqual(lockedTenant.getPackageId(), updateReqVO.getPackageId())) {
            updateTenantRoleMenu(lockedTenant.getId(), tenantPackage.getMenuIds());
        }
    }

    private void validTenantNameDuplicate(String name, Long id) {
        TenantDO tenant = tenantMapper.selectByName(name);
        if (tenant == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同名字的租户
        if (id == null) {
            throw exception(TENANT_NAME_DUPLICATE, name);
        }
        if (!tenant.getId().equals(id)) {
            throw exception(TENANT_NAME_DUPLICATE, name);
        }
    }

    private void validTenantWebsiteDuplicate(List<String> websites, Long excludeId) {
        if (CollUtil.isEmpty(websites)) {
            return;
        }
        websites.forEach(website -> {
            List<TenantDO> tenants = tenantMapper.selectListByWebsite(website);
            if (excludeId != null) {
                tenants.removeIf(tenant -> tenant.getId().equals(excludeId));
            }
            if (CollUtil.isNotEmpty(tenants)) {
                throw exception(TENANT_WEBSITE_DUPLICATE, website);
            }
        });
    }

    @Override
    // ZS-PERM-004.A codex r0 P1：同上——统一 Spring 事务生命周期（本方法由套餐/租户更新链调用，驱逐须与提交同序）
    @Transactional(rollbackFor = Exception.class)
    public void updateTenantRoleMenu(Long tenantId, Set<Long> menuIds) {
        // ZS-CFG-003.B codex r0 P1：先取租户行锁（与授权入口同一把锁），收敛与套餐校验/授权写入串行化
        tenantMapper.selectByIdForUpdate(tenantId);
        TenantUtils.execute(tenantId, () -> {
            // 获得所有角色
            List<RoleDO> roles = roleService.getRoleList();
            roles.forEach(role -> Assert.isTrue(tenantId.equals(role.getTenantId()), "角色({}/{}) 租户不匹配",
                    role.getId(), role.getTenantId(), tenantId)); // 兜底校验
            // 重新分配每个角色的权限
            roles.forEach(role -> {
                // 如果是租户管理员，重新分配其权限为租户套餐的权限
                if (Objects.equals(role.getCode(), RoleCodeEnum.TENANT_ADMIN.getCode())) {
                    permissionService.assignRoleMenu(role.getId(), sanitizeExistingMenuIds(menuIds));
                    log.info("[updateTenantRoleMenu][租户管理员({}/{}) 的权限修改为({})]", role.getId(), role.getTenantId(), menuIds);
                    return;
                }
                // 如果是其他角色，则去掉超过套餐的权限
                Set<Long> roleMenuIds = permissionService.getRoleMenuListByRoleId(role.getId());
                roleMenuIds = CollUtil.intersectionDistinct(roleMenuIds, menuIds);
                permissionService.assignRoleMenu(role.getId(), sanitizeExistingMenuIds(roleMenuIds));
                log.info("[updateTenantRoleMenu][角色({}/{}) 的权限修改为({})]", role.getId(), role.getTenantId(), roleMenuIds);
            });
        });
    }

    @Override
    public void deleteTenant(Long id) {
        // 校验存在
        validateUpdateTenant(id);
        // 删除
        tenantMapper.deleteById(id);
    }

    @Override
    public void deleteTenantList(List<Long> ids) {
        // 1. 校验存在
        ids.forEach(this::validateUpdateTenant);

        // 2. 批量删除
        tenantMapper.deleteByIds(ids);
    }

    private TenantDO validateUpdateTenant(Long id) {
        TenantDO tenant = tenantMapper.selectById(id);
        if (tenant == null) {
            throw exception(TENANT_NOT_EXISTS);
        }
        // 内置租户，不允许删除
        if (isSystemTenant(tenant)) {
            throw exception(TENANT_CAN_NOT_UPDATE_SYSTEM);
        }
        return tenant;
    }

    @Override
    public TenantDO getTenant(Long id) {
        return tenantMapper.selectById(id);
    }

    @Override
    public PageResult<TenantDO> getTenantPage(TenantPageReqVO pageReqVO) {
        return tenantMapper.selectPage(pageReqVO);
    }

    @Override
    public TenantDO getTenantByName(String name) {
        return tenantMapper.selectByName(name);
    }

    @Override
    public TenantDO getTenantByWebsite(String website) {
        List<TenantDO> tenants = tenantMapper.selectListByWebsite(website);
        return CollUtil.getFirst(tenants);
    }

    @Override
    public Long getTenantCountByPackageId(Long packageId) {
        return tenantMapper.selectCountByPackageId(packageId);
    }

    @Override
    public List<TenantDO> getTenantListByPackageId(Long packageId) {
        return tenantMapper.selectListByPackageId(packageId);
    }

    @Override
    public List<TenantDO> getTenantListByStatus(Integer status) {
        return tenantMapper.selectListByStatus(status);
    }

    @Override
    public void handleTenantInfo(TenantInfoHandler handler) {
        // 如果禁用，则不执行逻辑
        if (isTenantDisable()) {
            return;
        }
        // 获得租户
        TenantDO tenant = getTenant(TenantContextHolder.getRequiredTenantId());
        // 执行处理器
        handler.handle(tenant);
    }

    @Override
    public void handleTenantMenu(TenantMenuHandler handler) {
        // 如果禁用，则不执行逻辑
        if (isTenantDisable()) {
            return;
        }
        // 获得租户，然后获得菜单
        TenantDO tenant = getTenant(TenantContextHolder.getRequiredTenantId());
        Set<Long> menuIds;
        if (isSystemTenant(tenant)) { // 系统租户，菜单是全量的
            menuIds = CollectionUtils.convertSet(menuService.getMenuList(), MenuDO::getId);
        } else {
            menuIds = tenantPackageService.getTenantPackage(tenant.getPackageId()).getMenuIds();
        }
        // 执行处理器
        handler.handle(menuIds);
    }

    private static boolean isSystemTenant(TenantDO tenant) {
        return Objects.equals(tenant.getPackageId(), TenantDO.PACKAGE_ID_SYSTEM);
    }

    private boolean isTenantDisable() {
        return tenantProperties == null || Boolean.FALSE.equals(tenantProperties.getEnable());
    }

}
