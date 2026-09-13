package cn.zszj.module.system.service.permission;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.common.exception.ErrorCode;
import cn.zszj.framework.common.util.collection.CollectionUtils;
import cn.zszj.framework.datapermission.core.annotation.DataPermission;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.zszj.module.system.dal.dataobject.dept.DeptDO;
import cn.zszj.module.system.dal.dataobject.permission.MenuDO;
import cn.zszj.module.system.dal.dataobject.permission.RoleDO;
import cn.zszj.module.system.dal.dataobject.permission.RoleMenuDO;
import cn.zszj.module.system.dal.dataobject.permission.UserRoleDO;
import cn.zszj.module.system.dal.dataobject.user.AdminUserDO;
import cn.zszj.module.system.dal.mysql.permission.RoleMenuMapper;
import cn.zszj.module.system.dal.mysql.permission.UserRoleMapper;
import cn.zszj.module.system.dal.redis.RedisKeyConstants;
import cn.zszj.module.system.enums.permission.DataScopeEnum;
import cn.zszj.module.system.service.dept.DeptService;
import cn.zszj.module.system.service.user.AdminUserService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.function.Supplier;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.zszj.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.zszj.framework.common.util.json.JsonUtils.toJsonString;
import static cn.zszj.module.system.enums.ErrorCodeConstants.*;

/**
 * 权限 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Slf4j
public class PermissionServiceImpl implements PermissionService {

    @Resource
    private RoleMenuMapper roleMenuMapper;
    @Resource
    private UserRoleMapper userRoleMapper;

    @Resource
    private RoleService roleService;
    @Resource
    private MenuService menuService;
    @Resource
    private DeptService deptService;
    @Resource
    private AdminUserService userService;

    @Override
    public boolean hasAnyPermissions(Long userId, String... permissions) {
        // 如果为空，说明已经有权限
        if (ArrayUtil.isEmpty(permissions)) {
            return true;
        }

        // 获得当前登录的角色。如果为空，说明没有权限
        List<RoleDO> roles = getEnableUserRoleListByUserIdFromCache(userId);
        if (CollUtil.isEmpty(roles)) {
            return false;
        }

        // 情况一：遍历判断每个权限，如果有一满足，说明有权限
        for (String permission : permissions) {
            if (hasAnyPermission(roles, permission)) {
                return true;
            }
        }

        // 情况二：如果是超管，也说明有权限
        return roleService.hasAnySuperAdmin(convertSet(roles, RoleDO::getId));
    }

    /**
     * 判断指定角色，是否拥有该 permission 权限
     *
     * @param roles 指定角色数组
     * @param permission 权限标识
     * @return 是否拥有
     */
    private boolean hasAnyPermission(List<RoleDO> roles, String permission) {
        List<Long> menuIds = menuService.getMenuIdListByPermissionFromCache(permission);
        // 采用严格模式，如果权限找不到对应的 Menu 的话，也认为没有权限
        if (CollUtil.isEmpty(menuIds)) {
            return false;
        }

        // 判断是否有权限
        Set<Long> roleIds = convertSet(roles, RoleDO::getId);
        for (Long menuId : menuIds) {
            // 获得拥有该菜单的角色编号集合
            Set<Long> menuRoleIds = getSelf().getMenuRoleIdListByMenuIdFromCache(menuId);
            // 如果有交集，说明有权限
            if (CollUtil.containsAny(menuRoleIds, roleIds)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasAnyRoles(Long userId, String... roles) {
        // 如果为空，说明已经有权限
        if (ArrayUtil.isEmpty(roles)) {
            return true;
        }

        // 获得当前登录的角色。如果为空，说明没有权限
        List<RoleDO> roleList = getEnableUserRoleListByUserIdFromCache(userId);
        if (CollUtil.isEmpty(roleList)) {
            return false;
        }

        // 判断是否有角色
        Set<String> userRoles = convertSet(roleList, RoleDO::getCode);
        return CollUtil.containsAny(userRoles, Sets.newHashSet(roles));
    }

    // ========== 角色-菜单的相关方法  ==========

    @Override
    // ZS-PERM-004.A：改用 Spring @Transactional——@DSTransactional（dynamic-datasource 本地事务）
    // 不激活 Spring 事务同步，缓存驱逐时机与其他 Spring 事务方法不一致（system 模块单库，语义等价）
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(value = RedisKeyConstants.MENU_ROLE_ID_LIST,
            allEntries = true),
            @CacheEvict(value = RedisKeyConstants.PERMISSION_MENU_ID_LIST,
            allEntries = true) // allEntries 清空所有缓存，主要一次更新涉及到的 menuIds 较多，反倒批量会更快
    })
    public void assignRoleMenu(Long roleId, Set<Long> menuIds) {
        // ZS-PERM-001.A：分配前校验角色归属，防止篡改他租户角色 ID 写入关联
        validateRoleForAssign(roleId);
        // 获得角色拥有菜单编号
        Set<Long> dbMenuIds = convertSet(roleMenuMapper.selectListByRoleId(roleId), RoleMenuDO::getMenuId);
        // 计算新增和删除的菜单编号
        Set<Long> menuIdList = CollUtil.emptyIfNull(menuIds);
        Collection<Long> createMenuIds = CollUtil.subtract(menuIdList, dbMenuIds);
        Collection<Long> deleteMenuIds = CollUtil.subtract(dbMenuIds, menuIdList);
        // 执行新增和删除。对于已经授权的菜单，不用做任何处理
        if (CollUtil.isNotEmpty(createMenuIds)) {
            roleMenuMapper.insertBatch(CollectionUtils.convertList(createMenuIds, menuId -> {
                RoleMenuDO entity = new RoleMenuDO();
                entity.setRoleId(roleId);
                entity.setMenuId(menuId);
                return entity;
            }));
        }
        if (CollUtil.isNotEmpty(deleteMenuIds)) {
            roleMenuMapper.deleteListByRoleIdAndMenuIds(roleId, deleteMenuIds);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(value = RedisKeyConstants.MENU_ROLE_ID_LIST,
                    allEntries = true), // allEntries 清空所有缓存，此处无法方便获得 roleId 对应的 menu 缓存们
            @CacheEvict(value = RedisKeyConstants.USER_ROLE_ID_LIST,
                    allEntries = true) // allEntries 清空所有缓存，此处无法方便获得 roleId 对应的 user 缓存们
    })
    public void processRoleDeleted(Long roleId) {
        // 标记删除 UserRole
        userRoleMapper.deleteListByRoleId(roleId);
        // 标记删除 RoleMenu
        roleMenuMapper.deleteListByRoleId(roleId);
    }

    @Override
    @CacheEvict(value = RedisKeyConstants.MENU_ROLE_ID_LIST, key = "#menuId")
    public void processMenuDeleted(Long menuId) {
        roleMenuMapper.deleteListByMenuId(menuId);
    }

    @Override
    public Set<Long> getRoleMenuListByRoleId(Collection<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptySet();
        }

        // 如果是管理员的情况下，获取全部菜单编号
        if (roleService.hasAnySuperAdmin(roleIds)) {
            return convertSet(menuService.getMenuList(), MenuDO::getId);
        }
        // 如果是非管理员的情况下，获得拥有的菜单编号
        return convertSet(roleMenuMapper.selectListByRoleId(roleIds), RoleMenuDO::getMenuId);
    }

    @Override
    @Cacheable(value = RedisKeyConstants.MENU_ROLE_ID_LIST, key = "#menuId")
    public Set<Long> getMenuRoleIdListByMenuIdFromCache(Long menuId) {
        return convertSet(roleMenuMapper.selectListByMenuId(menuId), RoleMenuDO::getRoleId);
    }

    // ========== 用户-角色的相关方法  ==========

    @Override
    // ZS-PERM-004.A：改用 Spring @Transactional——@DSTransactional（dynamic-datasource 本地事务）
    // 不激活 Spring 事务同步，缓存驱逐时机与其他 Spring 事务方法不一致（system 模块单库，语义等价）
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = RedisKeyConstants.USER_ROLE_ID_LIST, key = "#userId")
    public void assignUserRole(Long userId, Set<Long> roleIds) {
        // 获得角色拥有角色编号
        Set<Long> dbRoleIds = convertSet(userRoleMapper.selectListByUserId(userId),
                UserRoleDO::getRoleId);
        // 计算新增和删除的角色编号
        Set<Long> roleIdList = CollUtil.emptyIfNull(roleIds);
        Collection<Long> createRoleIds = CollUtil.subtract(roleIdList, dbRoleIds);
        Collection<Long> deleteMenuIds = CollUtil.subtract(dbRoleIds, roleIdList);
        // ZS-PERM-001.A：写入前统一校验，防止篡改他租户 用户/角色 ID、批量混入、越权授予与自我提权
        validateUserForAssign(userId);
        validateRolesForAssign(roleIdList, createRoleIds);
        validateUserRoleGrantCeiling(userId, createRoleIds);
        // 执行新增和删除。对于已经授权的角色，不用做任何处理
        if (!CollectionUtil.isEmpty(createRoleIds)) {
            userRoleMapper.insertBatch(CollectionUtils.convertList(createRoleIds, roleId -> {
                UserRoleDO entity = new UserRoleDO();
                entity.setUserId(userId);
                entity.setRoleId(roleId);
                return entity;
            }));
        }
        if (!CollectionUtil.isEmpty(deleteMenuIds)) {
            userRoleMapper.deleteListByUserIdAndRoleIdIds(userId, deleteMenuIds);
        }
    }

    @Override
    @CacheEvict(value = RedisKeyConstants.USER_ROLE_ID_LIST, key = "#userId")
    public void processUserDeleted(Long userId) {
        userRoleMapper.deleteListByUserId(userId);
    }

    @Override
    public Set<Long> getUserRoleIdListByUserId(Long userId) {
        return convertSet(userRoleMapper.selectListByUserId(userId), UserRoleDO::getRoleId);
    }

    @Override
    @Cacheable(value = RedisKeyConstants.USER_ROLE_ID_LIST, key = "#userId")
    public Set<Long> getUserRoleIdListByUserIdFromCache(Long userId) {
        return getUserRoleIdListByUserId(userId);
    }

    @Override
    public Set<Long> getUserRoleIdListByRoleId(Collection<Long> roleIds) {
        return convertSet(userRoleMapper.selectListByRoleIds(roleIds), UserRoleDO::getUserId);
    }

    /**
     * 获得用户拥有的角色，并且这些角色是开启状态的
     *
     * @param userId 用户编号
     * @return 用户拥有的角色
     */
    @VisibleForTesting
    List<RoleDO> getEnableUserRoleListByUserIdFromCache(Long userId) {
        // 获得用户拥有的角色编号
        Set<Long> roleIds = getSelf().getUserRoleIdListByUserIdFromCache(userId);
        // 获得角色数组，并移除被禁用的
        List<RoleDO> roles = roleService.getRoleListFromCache(roleIds);
        roles.removeIf(role -> !CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus()));
        return roles;
    }

    // ========== 用户-部门的相关方法  ==========

    @Override
    public void assignRoleDataScope(Long roleId, Integer dataScope, Set<Long> dataScopeDeptIds) {
        // ZS-PERM-001.A：校验角色与数据权限部门归属，防止篡改他租户 角色/部门 ID
        validateRoleForAssign(roleId);
        validateDeptListForAssign(dataScopeDeptIds);
        roleService.updateRoleDataScope(roleId, dataScope, dataScopeDeptIds);
    }

    @Override
    @DataPermission(enable = false) // 关闭数据权限，不然就会出现递归获取数据权限的问题
    public DeptDataPermissionRespDTO getDeptDataPermission(Long userId) {
        // 获得用户的角色
        List<RoleDO> roles = getEnableUserRoleListByUserIdFromCache(userId);

        // 如果角色为空，则只能查看自己
        DeptDataPermissionRespDTO result = new DeptDataPermissionRespDTO();
        if (CollUtil.isEmpty(roles)) {
            result.setSelf(true);
            return result;
        }

        // 获得用户的部门编号的缓存，通过 Guava 的 Suppliers 惰性求值，即有且仅有第一次发起 DB 的查询
        Supplier<Long> userDeptId = Suppliers.memoize(() -> userService.getUser(userId).getDeptId());
        // 遍历每个角色，计算
        for (RoleDO role : roles) {
            // 为空时，跳过
            if (role.getDataScope() == null) {
                continue;
            }
            // 情况一，ALL
            if (Objects.equals(role.getDataScope(), DataScopeEnum.ALL.getScope())) {
                result.setAll(true);
                continue;
            }
            // 情况二，DEPT_CUSTOM
            if (Objects.equals(role.getDataScope(), DataScopeEnum.DEPT_CUSTOM.getScope())) {
                CollUtil.addAll(result.getDeptIds(), role.getDataScopeDeptIds());
                // 自定义可见部门时，保证可以看到自己所在的部门。否则，一些场景下可能会有问题。
                // 例如说，登录时，基于 t_user 的 username 查询会可能被 dept_id 过滤掉
                CollectionUtils.addIfNotNull(result.getDeptIds(), userDeptId.get());
                continue;
            }
            // 情况三，DEPT_ONLY
            if (Objects.equals(role.getDataScope(), DataScopeEnum.DEPT_ONLY.getScope())) {
                CollectionUtils.addIfNotNull(result.getDeptIds(), userDeptId.get());
                continue;
            }
            // 情况四，DEPT_DEPT_AND_CHILD
            if (Objects.equals(role.getDataScope(), DataScopeEnum.DEPT_AND_CHILD.getScope())) {
                Long deptId = userDeptId.get();
                // 用户未设置部门，直接跳过；否则 getChildDeptIdListFromCache 走缓存注解会因 null key 报错
                if (deptId == null) {
                    continue;
                }
                CollUtil.addAll(result.getDeptIds(), deptService.getChildDeptIdListFromCache(deptId));
                // 添加本身部门编号
                result.getDeptIds().add(deptId);
                continue;
            }
            // 情况五，SELF
            if (Objects.equals(role.getDataScope(), DataScopeEnum.SELF.getScope())) {
                result.setSelf(true);
                continue;
            }
            // 未知情况，error log 即可
            log.error("[getDeptDataPermission][LoginUser({}) role({}) 无法处理]", userId, toJsonString(result));
        }
        return result;
    }

    // ========== ZS-PERM-001.A 授权目标归属与上限校验  ==========

    /**
     * 校验被授权用户：必须存在，且归属当前技术租户。
     *
     * 防止篡改他租户用户 ID 进行授权。
     */
    private void validateUserForAssign(Long userId) {
        AdminUserDO user = userService.getUser(userId);
        if (user == null) {
            throw exception(USER_NOT_EXISTS);
        }
        validateTenantScope(user.getTenantId(), userId, PERMISSION_ASSIGN_USER_OTHER_TENANT);
    }

    /**
     * 校验被授权角色集合：每个角色必须存在、归属当前技术租户；新授予的角色还必须处于开启状态。
     *
     * 防止篡改他租户角色 ID、批量混入无效或越权角色。状态仅校验新授予的角色，
     * 以保证「重复授权幂等」时不会因为既有角色的状态变化而报错。
     */
    private void validateRolesForAssign(Collection<Long> roleIds, Collection<Long> createRoleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return;
        }
        Map<Long, RoleDO> roleMap = convertMap(roleService.getRoleList(roleIds), RoleDO::getId);
        Set<Long> createSet = new HashSet<>();
        if (CollUtil.isNotEmpty(createRoleIds)) {
            createSet.addAll(createRoleIds);
        }
        roleIds.forEach(roleId -> {
            RoleDO role = roleMap.get(roleId);
            // 存在性：角色不存在（含被租户过滤掉的他租户角色）则拒绝
            if (role == null) {
                throw exception(ROLE_NOT_EXISTS);
            }
            // 归属：显式比对租户，不能只凭请求 tenant_id 为关系表填值就认定外键安全
            validateTenantScope(role.getTenantId(), roleId, PERMISSION_ASSIGN_ROLE_OTHER_TENANT);
            // 状态：仅对新授予的角色校验，禁用角色不可被授予
            if (createSet.contains(roleId) && !CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus())) {
                throw exception(ROLE_IS_DISABLE, role.getName());
            }
        });
    }

    /**
     * 校验单个被授权角色：必须存在，且归属当前技术租户。
     */
    private void validateRoleForAssign(Long roleId) {
        RoleDO role = roleService.getRole(roleId);
        if (role == null) {
            throw exception(ROLE_NOT_EXISTS);
        }
        validateTenantScope(role.getTenantId(), roleId, PERMISSION_ASSIGN_ROLE_OTHER_TENANT);
    }

    /**
     * 校验数据权限部门集合：每个部门必须存在，且归属当前技术租户。
     */
    private void validateDeptListForAssign(Collection<Long> deptIds) {
        if (CollUtil.isEmpty(deptIds)) {
            return;
        }
        Map<Long, DeptDO> deptMap = convertMap(deptService.getDeptList(deptIds), DeptDO::getId);
        deptIds.forEach(deptId -> {
            DeptDO dept = deptMap.get(deptId);
            if (dept == null) {
                throw exception(DEPT_NOT_FOUND);
            }
            validateTenantScope(dept.getTenantId(), deptId, PERMISSION_ASSIGN_DEPT_OTHER_TENANT);
        });
    }

    /**
     * 校验授予上限与自我提权（仅针对新授予的角色）。
     *
     * 非超级管理员：既不能授予超级管理员等特权角色，也不能为自身新增角色。
     * 无登录上下文（如系统内部/租户供给路径）时跳过，仅保留归属校验；超级管理员保留完整管理能力。
     */
    private void validateUserRoleGrantCeiling(Long userId, Collection<Long> createRoleIds) {
        // 无新增授予时无需校验，保证重复授权幂等
        if (CollUtil.isEmpty(createRoleIds)) {
            return;
        }
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        // 无登录上下文（系统内部调用/租户供给）时，跳过上限与自我提权校验
        if (loginUserId == null) {
            return;
        }
        // 超级管理员保留完整管理能力
        if (isSuperAdminUser(loginUserId)) {
            return;
        }
        // 上限：非超管不得授予超管等特权角色
        if (roleService.hasAnySuperAdmin(createRoleIds)) {
            throw exception(PERMISSION_GRANT_EXCEED_CEILING);
        }
        // 自我提权：非超管不得为自身新增角色
        if (loginUserId.equals(userId)) {
            throw exception(PERMISSION_SELF_ELEVATION);
        }
    }

    /**
     * 判断用户是否为超级管理员。
     */
    private boolean isSuperAdminUser(Long userId) {
        Set<Long> roleIds = getUserRoleIdListByUserId(userId);
        return CollUtil.isNotEmpty(roleIds) && roleService.hasAnySuperAdmin(roleIds);
    }

    /**
     * 校验目标对象的租户归属：必须属于当前技术租户。
     *
     * 仅在存在明确技术租户上下文、且未忽略租户时执行。租户供给路径通过
     * {@link cn.zszj.framework.tenant.core.util.TenantUtils#execute} 设置具体租户且不忽略，
     * 故新建用户/角色的归属天然满足校验；系统级忽略租户的操作则跳过。
     */
    private void validateTenantScope(Long targetTenantId, Long targetId, ErrorCode errorCode) {
        if (!isTenantScopeValidationEnabled()) {
            return;
        }
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (!currentTenantId.equals(targetTenantId)) {
            throw exception(errorCode, targetId);
        }
    }

    /**
     * 是否启用租户归属校验：存在技术租户上下文，且未忽略租户。
     */
    private boolean isTenantScopeValidationEnabled() {
        return !TenantContextHolder.isIgnore() && TenantContextHolder.getTenantId() != null;
    }

    /**
     * 获得自身的代理对象，解决 AOP 生效问题
     *
     * @return 自己
     */
    private PermissionServiceImpl getSelf() {
        return SpringUtil.getBean(getClass());
    }

}
