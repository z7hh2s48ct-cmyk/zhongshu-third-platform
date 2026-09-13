package cn.zszj.module.system.service.tenant;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.common.catalog.ModuleCatalog;
import cn.zszj.framework.common.util.object.BeanUtils;
import cn.zszj.module.system.controller.admin.tenant.vo.packages.TenantPackagePageReqVO;
import cn.zszj.module.system.controller.admin.tenant.vo.packages.TenantPackageSaveReqVO;
import cn.zszj.module.system.dal.dataobject.permission.MenuDO;
import cn.zszj.module.system.dal.dataobject.tenant.TenantDO;
import cn.zszj.module.system.dal.dataobject.tenant.TenantPackageDO;
import cn.zszj.module.system.dal.mysql.tenant.TenantMapper;
import cn.zszj.module.system.dal.mysql.tenant.TenantPackageMapper;
import cn.zszj.module.system.service.permission.MenuService;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.system.enums.ErrorCodeConstants.*;

/**
 * 租户套餐 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class TenantPackageServiceImpl implements TenantPackageService {

    @Resource
    private TenantPackageMapper tenantPackageMapper;
    @Resource
    private TenantMapper tenantMapper;

    @Resource
    @Lazy // 避免循环依赖的报错
    private TenantService tenantService;

    @Resource
    @Lazy // 避免循环依赖的报错
    private MenuService menuService;

    @Override
    public Long createTenantPackage(TenantPackageSaveReqVO createReqVO) {
        // 校验菜单不得启用关闭模块（ZS-CFG-003.A）
        validateTenantPackageMenus(createReqVO.getMenuIds());
        // 校验套餐名是否重复
        validateTenantPackageNameUnique(null, createReqVO.getName());
        // 插入
        TenantPackageDO tenantPackage = BeanUtils.toBean(createReqVO, TenantPackageDO.class);
        tenantPackageMapper.insert(tenantPackage);
        // 返回
        return tenantPackage.getId();
    }

    @Override
    // ZS-PERM-004.A codex r0 P1：改用 Spring @Transactional（同 TenantServiceImpl——统一事务生命周期，驱逐与提交同序）
    @Transactional(rollbackFor = Exception.class)
    public void updateTenantPackage(TenantPackageSaveReqVO updateReqVO) {
        // 校验菜单不得启用关闭模块（ZS-CFG-003.A）
        validateTenantPackageMenus(updateReqVO.getMenuIds());
        // ZS-CFG-003.B codex r2 P1：先取本套餐行锁再读取——并发套餐更新用锁前旧值判断"菜单未变化"
        // 跳过收敛的交错必须串行化；比较基准一律取锁定读
        tenantPackageMapper.selectByIdForUpdate(updateReqVO.getId());
        // 校验存在
        TenantPackageDO tenantPackage = validateTenantPackageExists(updateReqVO.getId());
        // 校验套餐名是否重复
        validateTenantPackageNameUnique(updateReqVO.getId(), updateReqVO.getName());
        // 更新
        TenantPackageDO updateObj = BeanUtils.toBean(updateReqVO, TenantPackageDO.class);
        tenantPackageMapper.updateById(updateObj);
        // 如果菜单发生变化，则修改每个租户的菜单
        if (!CollUtil.isEqualList(tenantPackage.getMenuIds(), updateReqVO.getMenuIds())) {
            List<TenantDO> tenants = tenantService.getTenantListByPackageId(tenantPackage.getId());
            // ZS-CFG-003.B codex r0 P1：按租户 id 排序后收敛——多租户行锁获取顺序确定，避免并发套餐更新死锁
            tenants.sort(java.util.Comparator.comparing(TenantDO::getId));
            tenants.forEach(tenant -> {
                // ZS-CFG-003.B codex r1 P1：锁内重查绑定——并发「换套餐」可能在枚举快照后把租户换入/换出本套餐，
                // 已不再绑定本套餐的租户跳过收敛（其授权由换入套餐的锁序约束），防止收敛到错误套餐的菜单
                TenantDO locked = tenantMapper.selectByIdForUpdate(tenant.getId());
                if (locked == null || !Objects.equals(locked.getPackageId(), tenantPackage.getId())) {
                    log.info("[updateTenantPackage][租户({}) 已换绑套餐，跳过本套餐收敛]", tenant.getId());
                    return;
                }
                tenantService.updateTenantRoleMenu(tenant.getId(), updateReqVO.getMenuIds());
            });
        }
    }

    @Override
    public void deleteTenantPackage(Long id) {
        // 校验存在
        validateTenantPackageExists(id);
        // 校验正在使用
        validateTenantUsed(id);
        // 删除
        tenantPackageMapper.deleteById(id);
    }

    @Override
    public void deleteTenantPackageList(List<Long> ids) {
        // 1. 校验是否有租户正在使用该套餐
        for (Long id : ids) {
            if (tenantService.getTenantCountByPackageId(id) > 0) {
                throw exception(TENANT_PACKAGE_USED);
            }
        }

        // 2. 批量删除
        tenantPackageMapper.deleteByIds(ids);
    }

    private TenantPackageDO validateTenantPackageExists(Long id) {
        TenantPackageDO tenantPackage = tenantPackageMapper.selectById(id);
        if (tenantPackage == null) {
            throw exception(TENANT_PACKAGE_NOT_EXISTS);
        }
        return tenantPackage;
    }

    private void validateTenantUsed(Long id) {
        if (tenantService.getTenantCountByPackageId(id) > 0) {
            throw exception(TENANT_PACKAGE_USED);
        }
    }

    @Override
    public TenantPackageDO getTenantPackage(Long id) {
        return tenantPackageMapper.selectById(id);
    }

    @Override
    public PageResult<TenantPackageDO> getTenantPackagePage(TenantPackagePageReqVO pageReqVO) {
        return tenantPackageMapper.selectPage(pageReqVO);
    }

    @Override
    public TenantPackageDO validTenantPackage(Long id) {
        TenantPackageDO tenantPackage = tenantPackageMapper.selectById(id);
        if (tenantPackage == null) {
            throw exception(TENANT_PACKAGE_NOT_EXISTS);
        }
        if (tenantPackage.getStatus().equals(CommonStatusEnum.DISABLE.getStatus())) {
            throw exception(TENANT_PACKAGE_DISABLE, tenantPackage.getName());
        }
        return tenantPackage;
    }

    @Override
    public List<TenantPackageDO> getTenantPackageListByStatus(Integer status) {
        return tenantPackageMapper.selectListByStatus(status);
    }


    /**
     * ZS-CFG-003.A：套餐菜单不得包含未启用模块的功能入口。
     * 无法归属模块的菜单（纯目录容器等）放行；归属到未启用模块的菜单拒绝。
     */
    @VisibleForTesting
    void validateTenantPackageMenus(Set<Long> menuIds) {
        if (CollUtil.isEmpty(menuIds)) {
            return;
        }
        for (MenuDO menu : menuService.getMenuList(menuIds)) {
            String module = ModuleCatalog.moduleOfMenu(menu.getPermission(), menu.getComponent(), menu.getPath());
            if (module != null && !ModuleCatalog.ENABLED_MODULES.contains(module)) {
                throw exception(TENANT_PACKAGE_MENU_MODULE_DISABLED, menu.getName(), module);
            }
        }
    }

    @VisibleForTesting
    void validateTenantPackageNameUnique(Long id, String name) {
        if (StrUtil.isBlank(name)) {
            return;
        }
        TenantPackageDO tenantPackage = tenantPackageMapper.selectByName(name);
        if (tenantPackage == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的用户
        if (id == null) {
            throw exception(TENANT_PACKAGE_NAME_DUPLICATE);
        }
        if (!tenantPackage.getId().equals(id)) {
            throw exception(TENANT_PACKAGE_NAME_DUPLICATE);
        }
    }

}
