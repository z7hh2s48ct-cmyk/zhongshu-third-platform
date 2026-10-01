package cn.zszj.module.firstchain.service.opening;

import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.module.firstchain.framework.FirstchainMenus;
import cn.zszj.module.system.controller.admin.permission.vo.role.RoleSaveReqVO;
import cn.zszj.module.system.service.permission.PermissionService;
import cn.zszj.module.system.service.permission.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 首链默认授权模板注册中心（ZS-FC-001 默认授权 wave；开通服务与员工创建服务共用）。
 *
 * <p>职责：租户级默认角色 create-or-reuse（编码 {@code firstchain:franchisee:leader/member}，
 * 角色定义「能做什么」、组织归属经任职承载——ZS-IAM-002 模型，角色不随组织增殖）+
 * 默认菜单幂等绑定（{@link FirstchainMenus} 编号合同，接入合同 §1.1 第⑤步）。
 *
 * <p>菜单绑定语义（本 wave 起「空集登记」升级为真实菜单面）：
 * <ul>
 *     <li>新建角色：直接绑定 {@code FirstchainMenus} 规定的菜单集；</li>
 *     <li>复用角色（既有绑定）：<b>仅当绑定为空时补绑</b>——修复本迁移前「空集登记」期的存量模板角色，
 *     不重绑非空集合（{@code assignRoleMenu} 为全量替换语义，防清空运维侧追加的既有授权）；
 *     存量角色的定向修复另有迁移 SQL（V20261001.001 修复段）兜底；</li>
 * </ul>
 *
 * <p>并发窗口：进程内 synchronized + 锁后复查串行化低频开通路径；跨实例残余竞态为重复模板角色，
 * 无权限语义危害——继承 ZS-FC-001 交付轮登记的已知限制，多实例互斥随运维扩容决策后置。
 *
 * @author ZS-FC-001
 */
@Service
@Slf4j
public class FirstchainDefaultRoleRegistry {

    private final JdbcTemplate jdbcTemplate;

    private final RoleService roleService;

    private final PermissionService permissionService;

    public FirstchainDefaultRoleRegistry(DataSource dataSource, RoleService roleService,
                                         PermissionService permissionService) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.roleService = roleService;
        this.permissionService = permissionService;
    }

    /**
     * 取用或创建默认角色并确保默认菜单面就位（幂等，可重复调用）。
     *
     * @return 角色编号（既有或新建）
     */
    public Long ensureRoleWithMenus(Long tenantId, String code, String name, int sort, Set<Long> menuIds) {
        Long roleId = ensureRole(tenantId, code, name, sort);
        ensureMenusBound(roleId, code, menuIds);
        return roleId;
    }

    /**
     * 默认角色 create-or-reuse（租户内编码唯一）：先查（JdbcTemplate 显式租户 + deleted=FALSE），
     * 缺失才经 {@code RoleService#createRole} 创建（type=null → CUSTOM 自定义角色）。
     */
    public Long ensureRole(Long tenantId, String code, String name, int sort) {
        Long existing = queryRoleIdByCode(tenantId, code);
        if (existing != null) {
            return existing;
        }
        synchronized (this) {
            existing = queryRoleIdByCode(tenantId, code);
            if (existing != null) {
                return existing;
            }
            RoleSaveReqVO saveReqVO = new RoleSaveReqVO();
            saveReqVO.setName(name);
            saveReqVO.setCode(code);
            saveReqVO.setSort(sort);
            saveReqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
            saveReqVO.setRemark("首链开通默认授权模板（ZS-FC-001）");
            try {
                return roleService.createRole(saveReqVO, null);
            } catch (DuplicateKeyException concurrentCreation) {
                // 跨实例并发建模板角色：uk 兜底后复查取用（先到者生效，角色编码即幂等键）
                Long raced = queryRoleIdByCode(tenantId, code);
                if (raced != null) {
                    return raced;
                }
                throw concurrentCreation;
            }
        }
    }

    /**
     * 默认菜单幂等绑定：新建角色（绑定为空）→ 绑定编号合同菜单集；既有绑定非空 → 不动
     * （防全量替换清空运维侧既有授权）；查询失败不阻断开通（菜单绑定滞后可经迁移修复段/下次调用补齐）。
     */
    private void ensureMenusBound(Long roleId, String roleCode, Set<Long> menuIds) {
        try {
            Set<Long> current = permissionService.getRoleMenuListByRoleId(roleId);
            if (current == null || current.isEmpty()) {
                permissionService.assignRoleMenu(roleId, new HashSet<>(menuIds));
                log.info("[ensureMenusBound][默认角色({}) 绑定首链默认菜单 {} 项]", roleCode, menuIds.size());
            }
        } catch (RuntimeException menuBindingFailure) {
            // 菜单面滞后不回滚主体开通（M4-A 开通主体=组织/账号/任职；菜单经迁移修复段兜底可补）
            log.warn("[ensureMenusBound][默认角色({}) 菜单绑定未完成，留待迁移修复段兜底：{}]",
                    roleCode, menuBindingFailure.getMessage());
        }
    }

    private Long queryRoleIdByCode(Long tenantId, String code) {
        // system_role.deleted 为 int2（基线形态）：谓词必须写 `= 0`——真实 PG 无 `smallint = boolean` 算符
        //（run-firstchain-verify 真实容器实证，勿改回 FALSE；H2 bit 列同样兼容 0）
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id FROM system_role WHERE tenant_id = ? AND code = ? AND deleted = 0",
                tenantId, code);
        return rows.isEmpty() ? null : ((Number) rows.get(0).get("id")).longValue();
    }

}
