package cn.zszj.module.system.service.permission;

import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.module.system.controller.admin.permission.vo.role.RoleSaveReqVO;
import cn.zszj.module.system.enums.permission.DataScopeEnum;
import cn.zszj.framework.redis.config.ZszjCacheAutoConfiguration;
import cn.zszj.framework.test.core.ut.BaseDbAndRedisUnitTest;
import cn.zszj.module.system.dal.dataobject.permission.RoleDO;
import cn.zszj.module.system.dal.dataobject.permission.UserRoleDO;
import cn.zszj.module.system.dal.mysql.permission.RoleMapper;
import cn.zszj.module.system.dal.mysql.permission.UserRoleMapper;
import cn.zszj.module.system.enums.permission.RoleTypeEnum;
import cn.zszj.module.system.service.dept.DeptService;
import cn.zszj.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static cn.zszj.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * ZS-PERM-004.A：角色/菜单缓存与停用撤权的技术一致性测试（真实 H2 + 内嵌 Redis + 真实 Spring Cache）。
 *
 * <p><b>RED 依据</b>：①{@code @CacheEvict} 在方法返回后、事务提交前执行——「已驱逐、未提交」窗口内，
 * 其他节点（另一连接）回读库中旧值重新入缓存，提交后陈旧权限存活（并发回填）；②事务回滚路径同样已驱逐，
 * 「清缓存当万能修复」损失一致性且属可用性误伤。
 *
 * <p><b>GREEN 合同</b>：驱逐注册到事务结束后（afterCompletion 全状态）单次执行——提交后驱逐使并发回填
 * 被最终清除；回滚不误清；停用角色/撤销授权后旧 Token 的后续鉴权立即失效（权限每请求经缓存解析）。
 *
 * @author ZS-PERM-004.A
 */
@Import({ZszjCacheAutoConfiguration.class, RoleServiceImpl.class, PermissionServiceImpl.class, OrgDataScopeResolver.class})
@TestPropertySource(properties = "spring.main.allow-circular-references=true") // 与生产 zszj-server 一致（Role↔Permission 循环依赖）
public class PermissionCacheConsistencyTest extends BaseDbAndRedisUnitTest {

    @Resource
    private RoleService roleService;      // @EnableCaching 代理后按接口注入（impl 直注会 BeanNotOfRequiredType）
    @Resource
    private PermissionService permissionService;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private UserRoleMapper userRoleMapper;
    @Resource
    private PlatformTransactionManager transactionManager;
    @Resource
    private CacheManager cacheManager;
    @Resource
    private cn.zszj.module.system.dal.mysql.permission.RoleMenuMapper roleMenuMapper;

    private TransactionTemplate transactionTemplate;

    @MockitoBean
    private MenuService menuService;
    @MockitoBean
    private DeptService deptService;
    @MockitoBean
    private AdminUserService adminUserService;

    @BeforeEach
    public void beforeEach() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        when(adminUserService.getUser(anyLong())).thenReturn(randomPojo(cn.zszj.module.system.dal.dataobject.user.AdminUserDO.class));
    }

    @AfterEach
    public void afterEach() {
        // 清理 ThreadLocal，避免影响其它用例
        //（Redis 缓存条目由各用例自清：本类缓存键固定，互不依赖干净状态）
    }

    private RoleDO seedRole(CommonStatusEnum status) {
        RoleDO role = randomPojo(RoleDO.class);
        role.setId(null);
        role.setCode("test_" + System.nanoTime());
        role.setName("测试角色");
        role.setType(RoleTypeEnum.CUSTOM.getType());
        role.setStatus(status.getStatus());
        role.setDataScope(DataScopeEnum.ALL.getScope());
        roleMapper.insert(role);
        return role;
    }

    private static final String TEST_PERMISSION = "zs:perm-004:consistency";

    // ========== ① 并发回填：提交后驱逐使「他连接回填旧授权」最终自愈 ==========

    /**
     * 契约验证（codex r0 P2 改良）：本用例实证本仓库当前装配下（cache advisor 在 tx advisor 外层）
     * 驱逐发生在提交后——「已驱逐、未提交」窗口不存在，他连接回填的旧值在提交后被驱逐清除。
     *
     * <p><b>已知残余窗口（已由 ZS-PERM-004.C 版本校验闭环）</b>：若他连接在「DB 读完成、缓存写入」间
     * 被屏障暂停，且写事务提交并完成驱逐后该读线程才把旧值写回缓存——命中白名单的受管缓存由
     * 「evict bump 版本 + put 对比丢弃」拦截晚到旧值；驱逐失败的可靠补偿经 outbox 清单持久化 + 重放。
     * 残余亚毫秒边界（verify 与 put 之间）与降级路径见 ZS-PERM-004.C 登记（§5）。
     */
    @Test
    public void testUpdateRoleStatus_committedTx_staleBackfillFromOtherConnection_selfHeals() throws Exception {
        RoleDO role = seedRole(CommonStatusEnum.ENABLE);
        Long roleId = role.getId();

        // 预热缓存（ENABLE 条目）
        assertEquals(CommonStatusEnum.ENABLE.getStatus(), roleService.getRoleFromCache(roleId).getStatus());

        CountDownLatch evicted = new CountDownLatch(1);
        CountDownLatch backfilled = new CountDownLatch(1);
        AtomicReference<Throwable> readerError = new AtomicReference<>();

        // 模拟「其他节点」：另一连接在驱逐后、提交前回读旧值并入缓存
        Thread otherNode = new Thread(() -> {
            try {
                assertTrue(evicted.await(10, TimeUnit.SECONDS), "等待驱逐窗口超时");
                // 读到的是【未提交变更前的已提交旧值】——另一连接不可见本事务未提交修改
                RoleDO cached = roleService.getRoleFromCache(roleId);
                assertEquals(CommonStatusEnum.ENABLE.getStatus(), cached.getStatus(),
                        "前置：另一连接在提交前只应读到旧值");
            } catch (Throwable ex) {
                readerError.set(ex);
            } finally {
                backfilled.countDown();
            }
        });
        otherNode.start();

        transactionTemplate.executeWithoutResult(status -> {
            RoleSaveReqVO disableReq = buildDisableReq(roleId);
            roleService.updateRole(disableReq);
            evicted.countDown(); // 方法已返回（修复前驱逐已发生；修复后驱逐推迟到提交后）
            try {
                assertTrue(backfilled.await(10, TimeUnit.SECONDS), "等待其他节点回填超时");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(ex);
            }
        });

        otherNode.join(10_000);
        if (readerError.get() != null) {
            throw new RuntimeException("其他节点读取失败", readerError.get());
        }

        // 提交后：并发回填的陈旧条目必须被提交后驱逐清除——旧 Token 后续鉴权取到停用态
        assertEquals(CommonStatusEnum.DISABLE.getStatus(), roleService.getRoleFromCache(roleId).getStatus(),
                "提交后缓存必须反映停用态（并发回填的陈旧条目必须被清除）");
    }

    // ========== ② 回滚不误清缓存 ==========

    /**
     * RED：修复前事务回滚路径也已驱逐——回滚后 DB 未变、缓存被清（一致性证据丢失 + 可用性误伤）。
     * 修复后驱逐推迟到事务结束且回滚按幂等兜底执行：回滚后缓存条目仍为旧值（DB 与缓存一致）。
     */
    @Test
    public void testUpdateRoleStatus_rolledBackTx_cacheKeptConsistent() throws Exception {
        RoleDO role = seedRole(CommonStatusEnum.ENABLE);
        Long roleId = role.getId();
        assertEquals(CommonStatusEnum.ENABLE.getStatus(), roleService.getRoleFromCache(roleId).getStatus(),
                "前置：缓存条目已预热");

        try {
            transactionTemplate.executeWithoutResult(status -> {
                RoleSaveReqVO disableReq = buildDisableReq(roleId);
            roleService.updateRole(disableReq);
                throw new RuntimeException("强制回滚");
            });
        } catch (RuntimeException ignored) {
            // 预期回滚
        }

        // DB 未变（回滚）
        assertEquals(CommonStatusEnum.ENABLE.getStatus(), roleMapper.selectById(roleId).getStatus(),
                "前置：DB 随回滚恢复启用态");
        // 缓存与 DB 一致（codex 关注的「回滚不误清」）：回滚后原缓存条目必须原样保留——
        // 修复前驱逐发生在事务内（提交与否都清），条目丢失；修复后驱逐仅提交后执行，回滚不驱逐
        Cache roleCache = cacheManager.getCache("role");
        Cache.ValueWrapper wrapper = roleCache == null ? null : roleCache.get(roleId);
        assertTrue(wrapper != null && wrapper.get() != null, "回滚后原缓存条目不得被误清");
        assertEquals(CommonStatusEnum.ENABLE.getStatus(), ((RoleDO) wrapper.get()).getStatus(),
                "回滚后缓存条目必须仍为启用态（与 DB 一致）");
    }

    // ========== ③ 停用角色后，旧 Token 后续鉴权立即失效 ==========

    /**
     * GREEN 护栏：停用角色提交后，hasAnyPermissions 立即变 false——权限每请求经缓存解析，
     * 缓存驱逐正确即「旧 Token 不保留撤销权限」（§16.1 验收）。
     */
    @Test
    public void testDisableRole_hasAnyPermissionsDeniedAfterCommit() {
        RoleDO role = seedRole(CommonStatusEnum.ENABLE);
        Long roleId = role.getId();
        Long userId = 9_100_001L;
        insertUserRole(userId, roleId);
        when(menuService.getMenuIdListByPermissionFromCache(TEST_PERMISSION)).thenReturn(List.of(5_001L));
        insertRoleMenu(roleId, 5_001L);

        // 授权生效
        assertTrue(permissionService.hasAnyPermissions(userId, TEST_PERMISSION), "前置：启用角色应有权");
        assertTrue(permissionService.hasAnyPermissions(userId, TEST_PERMISSION), "前置：缓存命中仍有权");

        // 提交事务内停用
        transactionTemplate.executeWithoutResult(status ->
                roleService.updateRole(buildDisableReq(roleId)));

        // 停用后立即失效（旧 Token 继续请求被拒）
        assertFalse(permissionService.hasAnyPermissions(userId, TEST_PERMISSION),
                "停用角色后旧 Token 的后续鉴权必须立即失效");
    }

    // ========== ④ 撤销用户授权后立即失效 ==========

    @Test
    public void testRevokeUserRole_hasAnyPermissionsDeniedAfterCommit() {
        RoleDO role = seedRole(CommonStatusEnum.ENABLE);
        Long roleId = role.getId();
        Long userId = 9_100_002L;
        insertUserRole(userId, roleId);
        when(menuService.getMenuIdListByPermissionFromCache(TEST_PERMISSION)).thenReturn(List.of(5_002L));
        insertRoleMenu(roleId, 5_002L);

        assertTrue(permissionService.hasAnyPermissions(userId, TEST_PERMISSION), "前置：有角色应有权");

        // 提交事务内撤销用户全部角色
        transactionTemplate.executeWithoutResult(status ->
                permissionService.assignUserRole(userId, Set.of()));

        assertFalse(permissionService.hasAnyPermissions(userId, TEST_PERMISSION),
                "撤销授权后旧 Token 的后续鉴权必须立即失效");
    }

    private RoleSaveReqVO buildDisableReq(Long roleId) {
        RoleDO role = roleMapper.selectById(roleId);
        RoleSaveReqVO reqVO = new RoleSaveReqVO();
        reqVO.setId(roleId);
        reqVO.setName(role.getName());
        reqVO.setCode(role.getCode());
        reqVO.setSort(role.getSort() == null ? 1 : role.getSort());
        reqVO.setStatus(CommonStatusEnum.DISABLE.getStatus());
        reqVO.setRemark(role.getRemark());
        return reqVO;
    }

    // ========== ⑤ RED：批量删除角色缺少 ROLE 缓存驱逐 ==========

    /**
     * RED：修复前 deleteRoleList 无任何 @CacheEvict——批量删除角色后，已预热的 ROLE 缓存条目残留，
     * getRoleFromCache 继续返回已删除角色（unless null 不缓存，但陈旧条目先命中直接返回）。
     */
    @Test
    public void testDeleteRoleList_staleRoleCacheEvicted() {
        RoleDO role = seedRole(CommonStatusEnum.ENABLE);
        Long roleId = role.getId();
        assertEquals(CommonStatusEnum.ENABLE.getStatus(), roleService.getRoleFromCache(roleId).getStatus(),
                "前置：缓存条目已预热");

        transactionTemplate.executeWithoutResult(status ->
                roleService.deleteRoleList(List.of(roleId)));

        assertNull(roleService.getRoleFromCache(roleId), "批量删除角色后陈旧缓存必须被驱逐（不得返回已删除角色）");
    }

    private void insertRoleMenu(Long roleId, Long menuId) {
        cn.zszj.module.system.dal.dataobject.permission.RoleMenuDO roleMenu =
                new cn.zszj.module.system.dal.dataobject.permission.RoleMenuDO();
        roleMenu.setRoleId(roleId);
        roleMenu.setMenuId(menuId);
        roleMenuMapper.insert(roleMenu);
    }

    private void insertUserRole(Long userId, Long roleId) {
        UserRoleDO userRole = new UserRoleDO();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        userRoleMapper.insert(userRole);
    }
}
