package cn.zszj.module.system.service.tenant;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
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
import cn.zszj.module.system.dal.mysql.tenant.TenantPackageMapper;
import cn.zszj.module.system.service.permission.MenuService;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
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
public class TenantPackageServiceImpl implements TenantPackageService {

    @Resource
    private TenantPackageMapper tenantPackageMapper;

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
            tenants.forEach(tenant -> tenantService.updateTenantRoleMenu(tenant.getId(), updateReqVO.getMenuIds()));
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
