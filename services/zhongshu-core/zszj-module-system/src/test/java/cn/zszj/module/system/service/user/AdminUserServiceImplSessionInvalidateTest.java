package cn.zszj.module.system.service.user;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.common.util.collection.ArrayUtils;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.module.infra.api.config.ConfigApi;
import cn.zszj.module.infra.api.file.FileApi;
import cn.zszj.module.system.controller.admin.user.vo.profile.UserProfileUpdatePasswordReqVO;
import cn.zszj.module.system.dal.dataobject.dept.DeptDO;
import cn.zszj.module.system.dal.dataobject.user.AdminUserDO;
import cn.zszj.module.system.dal.mysql.dept.DeptMapper;
import cn.zszj.module.system.dal.mysql.user.AdminUserMapper;
import cn.zszj.module.system.enums.LogRecordConstants;
import cn.zszj.module.system.enums.common.SexEnum;
import cn.zszj.module.system.mq.producer.user.AdminUserProducer;
import cn.zszj.module.system.service.dept.DeptService;
import cn.zszj.module.system.service.dept.PostService;
import cn.zszj.module.system.service.oauth2.OAuth2TokenService;
import cn.zszj.module.system.service.permission.PermissionService;
import cn.zszj.module.system.service.tenant.TenantService;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static cn.hutool.core.util.RandomUtil.randomEle;
import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zszj.framework.test.core.util.RandomUtils.randomPojo;
import static cn.zszj.module.system.enums.ErrorCodeConstants.USER_IS_DEPT_LEADER;
import static cn.zszj.module.system.enums.ErrorCodeConstants.USER_PASSWORD_FAILED;
import static cn.zszj.module.system.service.user.AdminUserServiceImpl.USER_INIT_PASSWORD_KEY;
import static org.assertj.core.util.Lists.newArrayList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ZS-LOGIN-003：技术账号（B03）生命周期入口的会话失效单元测试类。
 *
 * <p>合同：禁用 / 删除 / 改密三类生命周期入口，都必须复用既有撤销能力
 * {@link OAuth2TokenService#removeAccessToken(Long, Integer)} 使该用户的<b>全部</b>会话失效。
 *
 * <p><b>改密失效策略（本任务决策）：全端失效</b>。理由：
 * <ol>
 *     <li>{@code LoginUser} 与 {@code OAuth2AccessTokenCheckRespDTO} 均<b>不携带</b>访问令牌串，
 *         {@code TokenAuthenticationFilter} 也不会把它写进安全上下文，因此服务端<b>当前无法识别「当前端」</b>；</li>
 *     <li>任何由客户端上送（请求体 / 请求头 / URL 参数）的「保留哪一端」提示都可被伪造，
 *         攻击者持失窃凭据改密后可指定保留自己的会话，使改密失去止损意义；</li>
 *     <li>管理员重置密码（{@code updateUserPassword(id, password)}）针对的是<b>他人</b>账号，
 *         根本不存在「目标用户的当前端」概念，只能全端失效；</li>
 *     <li>改密本身即是「怀疑凭据泄露」的止损动作，全端失效是安全默认。</li>
 * </ol>
 * 故本类断言改密走的是<b>用户维度</b>撤销，且<b>不</b>走单令牌维度撤销。
 *
 * <p><b>范围边界</b>：仅覆盖技术账号闭环（B03）。任职撤销 / 业务组织语义归 B07（D-09 后），本类不涉及。
 *
 * @author ZS-LOGIN-003
 */
@Import(AdminUserServiceImpl.class)
public class AdminUserServiceImplSessionInvalidateTest extends BaseDbUnitTest {

    @Resource
    private AdminUserServiceImpl userService;

    @Resource
    private AdminUserMapper userMapper;
    @Resource
    private DeptMapper deptMapper;

    @MockitoBean
    private DeptService deptService;
    @MockitoBean
    private PostService postService;
    @MockitoBean
    private PermissionService permissionService;
    @MockitoBean
    private PasswordEncoder passwordEncoder;
    @MockitoBean
    private TenantService tenantService;
    @MockitoBean
    private FileApi fileApi;
    @MockitoBean
    private ConfigApi configApi;
    @MockitoBean
    private OAuth2TokenService oauth2TokenService;
    @MockitoBean
    private AdminUserProducer adminUserProducer;

    private ListAppender<ILoggingEvent> logAppender;
    private Logger serviceLogger;

    @BeforeEach
    public void before() {
        when(configApi.getConfigValueByKey(USER_INIT_PASSWORD_KEY)).thenReturn("zszjyuanma");
        // 捕获 AdminUserServiceImpl 的审计日志（显式设 INFO，避免受全局日志级别影响造成假失败）
        serviceLogger = (Logger) LoggerFactory.getLogger(AdminUserServiceImpl.class);
        serviceLogger.setLevel(Level.INFO);
        logAppender = new ListAppender<>();
        logAppender.start();
        serviceLogger.addAppender(logAppender);
    }

    @AfterEach
    public void after() {
        if (serviceLogger != null && logAppender != null) {
            serviceLogger.detachAppender(logAppender);
            logAppender.stop();
        }
    }

    // ========== ① 删除用户：全部会话必须失效 ==========

    /**
     * 场景 ①：删除单个用户后，必须撤销该用户的全部会话。
     *
     * <p>RED：修复前 {@code deleteUser} 只删用户行与关联数据，从不触碰令牌 ——
     * 被删用户的 access / refresh 凭据在自然过期前仍然可用（{@code checkAccessToken} 只校验令牌存在与到期）。
     */
    @Test
    public void testDeleteUser_shouldRevokeAllSessions() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO();
        userMapper.insert(dbUser);
        Long userId = dbUser.getId();

        // 调用
        userService.deleteUser(userId);

        // 断言：走「用户维度」全端撤销（改密/删除策略一律全端失效，不做保留当前端）
        verify(oauth2TokenService, times(1)).removeAccessToken(eq(userId), eq(UserTypeEnum.ADMIN.getValue()));
        verify(oauth2TokenService, never()).removeAccessToken(anyString());
    }

    /**
     * 场景 ①-2：批量删除用户后，每个被删用户都必须撤销会话。
     *
     * <p>RED：修复前 {@code deleteUserList} 完全不调用撤销能力。
     */
    @Test
    public void testDeleteUserList_shouldRevokeAllSessionsForEachUser() {
        // mock 数据
        AdminUserDO dbUser1 = randomAdminUserDO();
        userMapper.insert(dbUser1);
        AdminUserDO dbUser2 = randomAdminUserDO();
        userMapper.insert(dbUser2);
        List<Long> ids = newArrayList(dbUser1.getId(), dbUser2.getId());

        // 调用
        userService.deleteUserList(ids);

        // 断言
        verify(oauth2TokenService, times(1)).removeAccessToken(eq(dbUser1.getId()), eq(UserTypeEnum.ADMIN.getValue()));
        verify(oauth2TokenService, times(1)).removeAccessToken(eq(dbUser2.getId()), eq(UserTypeEnum.ADMIN.getValue()));
        verify(oauth2TokenService, never()).removeAccessToken(anyString());
    }

    /**
     * 场景 ①-3：删除前置校验失败（部门负责人）时，不得撤销会话——避免「校验没过却把用户踢下线」。
     */
    @Test
    public void testDeleteUser_isDeptLeader_shouldNotRevokeSessions() {
        // mock 数据：用户是部门负责人
        AdminUserDO dbUser = randomAdminUserDO();
        userMapper.insert(dbUser);
        deptMapper.insert(randomPojo(DeptDO.class, o -> o.setLeaderUserId(dbUser.getId())));

        // 调用，并断言异常
        assertServiceException(() -> userService.deleteUser(dbUser.getId()), USER_IS_DEPT_LEADER);

        // 断言：未撤销任何会话
        verify(oauth2TokenService, never()).removeAccessToken(anyLong(), any());
        verify(oauth2TokenService, never()).removeAccessToken(anyString());
    }

    // ========== ② 禁用用户：既有能力回归看守 ==========

    /**
     * 场景 ②：禁用用户后撤销全部会话（既有能力的回归看守，防止本次改造把它改坏）。
     */
    @Test
    public void testUpdateUserStatusDisable_shouldRevokeAllSessions() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO(o -> o.setStatus(CommonStatusEnum.ENABLE.getStatus()));
        userMapper.insert(dbUser);

        // 调用
        userService.updateUserStatus(dbUser.getId(), CommonStatusEnum.DISABLE.getStatus());

        // 断言
        assertEquals(CommonStatusEnum.DISABLE.getStatus(), userMapper.selectById(dbUser.getId()).getStatus());
        verify(oauth2TokenService, times(1)).removeAccessToken(eq(dbUser.getId()), eq(UserTypeEnum.ADMIN.getValue()));
    }

    /**
     * 场景 ②-2：启用用户时不得撤销会话（避免过度撤销把正常用户踢下线）。
     */
    @Test
    public void testUpdateUserStatusEnable_shouldNotRevokeSessions() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO(o -> o.setStatus(CommonStatusEnum.DISABLE.getStatus()));
        userMapper.insert(dbUser);

        // 调用
        userService.updateUserStatus(dbUser.getId(), CommonStatusEnum.ENABLE.getStatus());

        // 断言
        assertEquals(CommonStatusEnum.ENABLE.getStatus(), userMapper.selectById(dbUser.getId()).getStatus());
        verify(oauth2TokenService, never()).removeAccessToken(anyLong(), any());
    }

    // ========== ③ 改密：全端失效 ==========

    /**
     * 场景 ③：用户自助修改密码后，必须<b>全端</b>失效（含当前端）。
     *
     * <p>RED：修复前该重载只更新密码列，从不调用撤销能力 —— 失窃凭据在改密后依旧可用。
     */
    @Test
    public void testUpdateUserPasswordBySelf_shouldRevokeAllSessions() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO(o -> o.setPassword("encode:tudou"));
        userMapper.insert(dbUser);
        UserProfileUpdatePasswordReqVO reqVO = randomPojo(UserProfileUpdatePasswordReqVO.class, o -> {
            o.setOldPassword("tudou");
            o.setNewPassword("yuanma");
        });
        when(passwordEncoder.encode(anyString())).then(
                (Answer<String>) invocationOnMock -> "encode:" + invocationOnMock.getArgument(0));
        when(passwordEncoder.matches(eq(reqVO.getOldPassword()), eq(dbUser.getPassword()))).thenReturn(true);

        // 调用
        userService.updateUserPassword(dbUser.getId(), reqVO);

        // 断言：密码已更新
        assertEquals("encode:yuanma", userMapper.selectById(dbUser.getId()).getPassword());
        // 断言：全端失效（用户维度撤销），且不走单令牌维度撤销
        verify(oauth2TokenService, times(1)).removeAccessToken(eq(dbUser.getId()), eq(UserTypeEnum.ADMIN.getValue()));
        verify(oauth2TokenService, never()).removeAccessToken(anyString());
    }

    /**
     * 场景 ③-2：管理员重置他人密码后，必须<b>全端</b>失效。
     *
     * <p>RED：修复前该重载只更新密码列，从不调用撤销能力。
     */
    @Test
    public void testUpdateUserPasswordByAdmin_shouldRevokeAllSessions() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO();
        userMapper.insert(dbUser);
        String password = "zszj";
        when(passwordEncoder.encode(anyString())).then(
                (Answer<String>) invocationOnMock -> "encode:" + invocationOnMock.getArgument(0));

        // 调用
        userService.updateUserPassword(dbUser.getId(), password);

        // 断言
        assertEquals("encode:" + password, userMapper.selectById(dbUser.getId()).getPassword());
        verify(oauth2TokenService, times(1)).removeAccessToken(eq(dbUser.getId()), eq(UserTypeEnum.ADMIN.getValue()));
        verify(oauth2TokenService, never()).removeAccessToken(anyString());
    }

    /**
     * 场景 ③-3：自助改密的旧密码校验失败时，不得撤销会话（避免用错误旧密码把用户踢下线，构成 DoS）。
     */
    @Test
    public void testUpdateUserPasswordBySelf_oldPasswordMismatch_shouldNotRevokeSessions() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO(o -> o.setPassword("encode:tudou"));
        userMapper.insert(dbUser);
        UserProfileUpdatePasswordReqVO reqVO = randomPojo(UserProfileUpdatePasswordReqVO.class, o -> {
            o.setOldPassword("wrong-password");
            o.setNewPassword("yuanma");
        });
        when(passwordEncoder.matches(eq(reqVO.getOldPassword()), eq(dbUser.getPassword()))).thenReturn(false);

        // 调用，并断言异常
        assertServiceException(() -> userService.updateUserPassword(dbUser.getId(), reqVO), USER_PASSWORD_FAILED);

        // 断言：密码未变、会话未撤销
        assertEquals("encode:tudou", userMapper.selectById(dbUser.getId()).getPassword());
        verify(oauth2TokenService, never()).removeAccessToken(anyLong(), any());
        verify(oauth2TokenService, never()).removeAccessToken(anyString());
    }

    // ========== ④ 事件与审计可追踪 ==========

    /**
     * 场景 ④：四类生命周期入口都必须挂在底座既有的操作日志机制（{@code @LogRecord} → system_operate_log）上，
     * 使「禁用 / 删除 / 改密导致的会话失效事件」可追踪、可回溯操作人。
     *
     * <p>RED：修复前 {@code updateUserStatus}（禁用）与自助改密重载<b>没有</b> {@code @LogRecord}，
     * 会话被大批撤销却无任何操作日志留痕。
     *
     * <p>刻意<b>不</b>另造审计机制：复用 mzt-biz-log 既有注解与既有 {@code SYSTEM_USER_TYPE}。
     */
    @Test
    public void testLifecycleEntries_shouldBeAuditableByLogRecord() throws Exception {
        assertLogRecordPresent("updateUserStatus", Long.class, Integer.class);
        assertLogRecordPresent("updateUserPassword", Long.class, UserProfileUpdatePasswordReqVO.class);
        assertLogRecordPresent("updateUserPassword", Long.class, String.class);
        assertLogRecordPresent("deleteUser", Long.class);
    }

    /**
     * 场景 ④-2：每个生命周期入口触发撤销时，必须留下结构化审计日志（含用户编号 + 原因），
     * 与操作日志经 trace-id 关联，满足「事件及审计可追踪」。
     *
     * <p>RED：修复前禁用路径虽已撤销会话，但<b>无任何日志</b>；删除 / 改密路径连撤销都没有。
     */
    @Test
    public void testSessionInvalidation_shouldEmitAuditLogWithReason() {
        // 禁用
        AdminUserDO disableUser = randomAdminUserDO(o -> o.setStatus(CommonStatusEnum.ENABLE.getStatus()));
        userMapper.insert(disableUser);
        userService.updateUserStatus(disableUser.getId(), CommonStatusEnum.DISABLE.getStatus());
        // 删除
        AdminUserDO deleteUser = randomAdminUserDO();
        userMapper.insert(deleteUser);
        userService.deleteUser(deleteUser.getId());
        // 管理员重置密码
        AdminUserDO resetUser = randomAdminUserDO();
        userMapper.insert(resetUser);
        when(passwordEncoder.encode(anyString())).then(
                (Answer<String>) invocationOnMock -> "encode:" + invocationOnMock.getArgument(0));
        userService.updateUserPassword(resetUser.getId(), "zszj");

        // 断言：三类事件各自的审计日志均可检索到（含用户编号与原因关键字）
        assertTrue(findAuditLog(disableUser.getId(), "禁用"),
                "禁用用户必须留下会话失效审计日志，实际日志：" + formattedLogs());
        assertTrue(findAuditLog(deleteUser.getId(), "删除"),
                "删除用户必须留下会话失效审计日志，实际日志：" + formattedLogs());
        assertTrue(findAuditLog(resetUser.getId(), "密码"),
                "改密必须留下会话失效审计日志，实际日志：" + formattedLogs());
    }

    // ========== 工具方法 ==========

    private void assertLogRecordPresent(String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = AdminUserServiceImpl.class.getMethod(methodName, parameterTypes);
        LogRecord logRecord = method.getAnnotation(LogRecord.class);
        assertNotNull(logRecord, "生命周期入口 " + methodName + " 必须挂 @LogRecord（复用底座既有操作日志机制）");
        assertEquals(LogRecordConstants.SYSTEM_USER_TYPE, logRecord.type(),
                "生命周期入口 " + methodName + " 的操作日志 type 必须为 SYSTEM 用户");
        assertTrue(logRecord.subType() != null && !logRecord.subType().isBlank(),
                "生命周期入口 " + methodName + " 的操作日志 subType 不得为空");
        assertTrue(logRecord.success() != null && !logRecord.success().isBlank(),
                "生命周期入口 " + methodName + " 的操作日志 success 模板不得为空");
    }

    /**
     * 检索会话失效审计日志：同一行内既包含用户编号，也包含原因关键字。
     */
    private boolean findAuditLog(Long userId, String reasonKeyword) {
        for (ILoggingEvent event : logAppender.list) {
            String message = event.getFormattedMessage();
            if (message.contains("会话") && message.contains(String.valueOf(userId))
                    && message.contains(reasonKeyword)) {
                return true;
            }
        }
        return false;
    }

    private List<String> formattedLogs() {
        List<String> messages = new ArrayList<>();
        for (ILoggingEvent event : logAppender.list) {
            messages.add(event.getFormattedMessage());
        }
        return messages;
    }

    @SafeVarargs
    private static AdminUserDO randomAdminUserDO(Consumer<AdminUserDO>... consumers) {
        Consumer<AdminUserDO> consumer = (o) -> {
            o.setStatus(randomEle(CommonStatusEnum.values()).getStatus()); // 保证 status 的范围
            o.setSex(randomEle(SexEnum.values()).getSex()); // 保证 sex 的范围
        };
        return randomPojo(AdminUserDO.class, ArrayUtils.append(consumer, consumers));
    }

}
