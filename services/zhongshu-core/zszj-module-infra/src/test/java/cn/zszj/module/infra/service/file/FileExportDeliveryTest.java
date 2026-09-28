package cn.zszj.module.infra.service.file;

import cn.hutool.crypto.digest.DigestUtil;
import cn.zszj.framework.common.biz.system.permission.dto.CrossOrgVisitDecisionDTO;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.datapermission.core.authorize.ObjectAuthorizationProvider;
import cn.zszj.framework.datapermission.core.authorize.ObjectAuthorizationRequest;
import cn.zszj.framework.datapermission.core.authorize.ObjectAuthorizationRespDTO;
import cn.zszj.framework.datapermission.core.authorize.ObjectAuthorizationService;
import cn.zszj.framework.datapermission.core.rule.org.OrgDataPermissionChecker;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.security.core.service.SecurityFrameworkService;
import cn.zszj.framework.security.core.util.CrossOrgVisitScopeHolder;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileDeliveryChunkRespVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileDeliverySessionRespVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileDeliveryTicketIssueReqVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileDeliveryTicketIssueRespVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileExportGenerateReqVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileExportGenerateRespVO;
import cn.zszj.module.infra.dal.dataobject.file.FileDO;
import cn.zszj.module.infra.dal.mysql.file.FileMapper;
import cn.zszj.module.infra.framework.file.config.FileConfiguration;
import cn.zszj.module.infra.framework.file.core.client.FileClient;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants.FORBIDDEN;
import static cn.zszj.framework.test.core.util.RandomUtils.randomString;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_DELIVERY_TICKET_REVOKED;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_EXPORT_FORBIDDEN;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ZS-FILE-004.B：导出生成-交付两阶段重检测试（H2，消费 ZS-PERM-003.A 统一裁决）。
 *
 * <p>合同（docs/05 §16.1 ZS-FILE-004.B 行）：生成时重检敏感字段（声明字段 ⊆ 授权字段否则拒绝；未启用
 * 字段级+非空声明 fail-closed）与业务组织（对象维 org 轴 / visit 收敛，不可见=fail-closed）；导出件
 * 落盘语义（PRIVATE、owner=登录主体、organizationId=源对象组织、purpose=export、保留期）；
 * 生成/交付间撤权不泄露（org 继承 + 交付链 org 门重检）；未接入对象类型 fail-closed（新通道保守，
 * 不改变既有同步导出行为）。字段等级目录 F0～F3 归 ZS-PERM-003.B——技术夹具为中性对象与 provider，
 * 不代表任何商业域。</p>
 *
 * @author ZS-FILE-004.B
 */
@Import({FileServiceImpl.class, FileExportDeliveryServiceImpl.class, FileDeliveryServiceImpl.class,
        FileConfiguration.class, FileExportDeliveryTest.AuthorizationTestConfig.class})
public class FileExportDeliveryTest extends BaseDbUnitTest {

    @Resource
    private FileExportDeliveryService exportDeliveryService;
    @Resource
    private FileDeliveryServiceImpl deliveryService;
    @Resource
    private ObjectAuthorizationService objectAuthorizationService;
    @Resource
    private FileMapper fileMapper;

    @MockitoBean
    private FileConfigService fileConfigService;
    @MockitoBean
    private cn.zszj.framework.common.biz.system.permission.PermissionCommonApi permissionCommonApi;
    @MockitoBean
    private SecurityFrameworkService securityFrameworkService;
    @MockitoBean
    private OrgDataPermissionChecker orgDataPermissionChecker;

    /** 模拟对象存储：正式键命名空间（导出件落盘 + 交付取流共用） */
    private Map<String, byte[]> objectStore;
    private FileClient masterClient;

    @BeforeEach
    public void beforeEach() {
        TenantContextHolder.setTenantId(1L);
        objectStore = new ConcurrentHashMap<>();
        masterClient = mock(FileClient.class);
        when(masterClient.getId()).thenReturn(1L);
        try {
            when(masterClient.upload(any(), anyString(), anyString())).thenAnswer(inv -> {
                objectStore.put(inv.getArgument(1, String.class), inv.getArgument(0, byte[].class));
                return "https://oss.example.com/" + inv.getArgument(1, String.class);
            });
            when(masterClient.getContentRange(anyString(), anyLong(), anyInt())).thenAnswer(inv -> {
                byte[] content = objectStore.get(inv.getArgument(0, String.class));
                if (content == null) {
                    return null;
                }
                long start = inv.getArgument(1, Long.class);
                int length = inv.getArgument(2, Integer.class);
                int from = (int) Math.min(start, content.length);
                int to = (int) Math.min(start + length, content.length);
                return Arrays.copyOfRange(content, from, to);
            });
        } catch (Exception ignored) {
        }
        when(fileConfigService.getMasterFileClient()).thenReturn(masterClient);
        when(fileConfigService.getFileClient(anyLong())).thenReturn(masterClient);
    }

    @AfterEach
    public void afterEach() {
        TenantContextHolder.clear();
    }

    // ========== ① 生成通道接入契约：登录主体与对象类型必填 ==========

    @Test
    public void generate_noLoginUser_rejected() {
        FileExportGenerateReqVO req = request("demo-export", 100L, 101L, sequencedBytes(32));
        ServiceException ex = assertThrows(ServiceException.class,
                () -> exportDeliveryService.generateExportFile(req, null));
        assertEquals(FILE_EXPORT_FORBIDDEN.getCode(), ex.getCode(), "无登录主体不得生成导出件");
        assertTrue(fileMapper.selectList().isEmpty(), "拒绝路径不得落库");
        assertTrue(objectStore.isEmpty(), "拒绝路径不得写入存储");
    }

    @Test
    public void generate_blankObjectType_rejected() {
        FileExportGenerateReqVO req = request(" ", 100L, 101L, sequencedBytes(32));
        ServiceException ex = assertThrows(ServiceException.class,
                () -> exportDeliveryService.generateExportFile(req, user(101L, 1L)));
        assertEquals(FILE_EXPORT_FORBIDDEN.getCode(), ex.getCode(), "对象类型为导出通道接入契约，不得空白");
        assertTrue(fileMapper.selectList().isEmpty());
        assertTrue(objectStore.isEmpty());
    }

    @Test
    public void generate_missingThreadContext_forbidden() {
        // 入参主体合法但无线程安全上下文：对象维裁决护栏（loginUser==null→可见）会 fail-open，
        // 锚定必须在此之前关闭该面——不得静默放行落盘（codex r1 P2-1）
        when(orgDataPermissionChecker.isObjectVisible(100L, 101L)).thenReturn(true);
        FileExportGenerateReqVO req = request("demo-export", 100L, 101L, sequencedBytes(32));
        ServiceException ex = assertThrows(ServiceException.class,
                () -> exportDeliveryService.generateExportFile(req, user(101L, 1L)));
        assertEquals(FILE_EXPORT_FORBIDDEN.getCode(), ex.getCode(), "线程安全上下文缺失必须 fail-closed");
        assertTrue(fileMapper.selectList().isEmpty(), "拒绝路径不得落库");
        assertTrue(objectStore.isEmpty(), "拒绝路径不得写入存储");
    }

    @Test
    public void generate_contextSubjectMismatch_forbidden() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            // 线程上下文主体（202）≠ 入参主体（101）：不得凭调用方声明的主体通过裁决
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(user(202L, 1L));
            when(orgDataPermissionChecker.isObjectVisible(100L, 101L)).thenReturn(true);
            FileExportGenerateReqVO req = request("demo-export", 100L, 101L, sequencedBytes(32));
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> exportDeliveryService.generateExportFile(req, user(101L, 1L)));
            assertEquals(FILE_EXPORT_FORBIDDEN.getCode(), ex.getCode(), "上下文主体与入参主体不一致必须 fail-closed");
            assertTrue(fileMapper.selectList().isEmpty(), "拒绝路径不得落库");
            assertTrue(objectStore.isEmpty(), "拒绝路径不得写入存储");
        }
    }

    // ========== ② 未接入对象类型：fail-closed（新通道保守，不改既有行为） ==========

    @Test
    public void generate_unknownObjectType_forbidden() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            stubContext(ms);
            FileExportGenerateReqVO req = request("unknown-export", 100L, 101L, sequencedBytes(32));
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> exportDeliveryService.generateExportFile(req, user(101L, 1L)));
            assertEquals(FORBIDDEN.getCode(), ex.getCode(), "未注册 provider 的对象类型 fail-closed");
            assertTrue(fileMapper.selectList().isEmpty(), "拒绝路径不得落库");
            assertTrue(objectStore.isEmpty(), "拒绝路径不得写入存储");
        }
    }

    // ========== ③ 业务组织导出重检（对象维） ==========

    @Test
    public void generate_objectNotVisible_forbidden() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            stubContext(ms);
            when(orgDataPermissionChecker.isObjectVisible(100L, 101L)).thenReturn(false);
            FileExportGenerateReqVO req = request("demo-export", 100L, 101L, sequencedBytes(32));
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> exportDeliveryService.generateExportFile(req, user(101L, 1L)));
            assertEquals(FORBIDDEN.getCode(), ex.getCode(), "对象不可见（业务组织越权）fail-closed");
            assertTrue(fileMapper.selectList().isEmpty(), "拒绝路径不得落库");
            assertTrue(objectStore.isEmpty(), "拒绝路径不得写入存储");
        }
    }

    @Test
    public void generate_visitRequest_objectOutOfScope_forbidden() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            stubVisit(ms);
            CrossOrgVisitScopeHolder.setScope(visitScope(Set.of(4000L), Set.of("demo:export"), null));
            FileExportGenerateReqVO req = request("demo-export", 5000L, 101L, sequencedBytes(32));
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> exportDeliveryService.generateExportFile(req, user(101L, 1L)));
            assertEquals(FORBIDDEN.getCode(), ex.getCode(), "visit 授权组织集不含目标组织 fail-closed");
            assertTrue(fileMapper.selectList().isEmpty());
        }
    }

    @Test
    public void generate_visitRequest_wholeTenant_passes() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            stubVisit(ms);
            CrossOrgVisitScopeHolder.setScope(visitScope(null, Set.of("demo:export"), null));
            FileExportGenerateReqVO req = request("demo-export", 5000L, 101L, sequencedBytes(64));

            FileExportGenerateRespVO resp = exportDeliveryService.generateExportFile(req, user(101L, 1L));

            assertNotNull(resp.getFileId());
            FileDO saved = fileMapper.selectById(resp.getFileId());
            assertEquals(5000L, saved.getOrganizationId(), "whole-tenant visit 授权下对象维放行");
        }
    }

    // ========== ④ 敏感字段导出重检（字段维；拒绝路径 + 输出供裁剪） ==========

    @Test
    public void generate_declaredFieldsWhenFieldLevelDisabled_forbidden() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            stubContext(ms);
            when(orgDataPermissionChecker.isObjectVisible(100L, 101L)).thenReturn(true);
            FileExportGenerateReqVO req = request("demo-export-nofields", 100L, 101L, sequencedBytes(32));
            req.setFields(Set.of("fieldA"));
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> exportDeliveryService.generateExportFile(req, user(101L, 1L)));
            assertEquals(FORBIDDEN.getCode(), ex.getCode(), "未启用字段级 + 非空敏感字段声明 fail-closed");
            assertTrue(fileMapper.selectList().isEmpty());
            assertTrue(objectStore.isEmpty());
        }
    }

    @Test
    public void generate_declaredFieldsExceedAuthorized_forbidden_andOutputForTrimming() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            stubVisit(ms);
            CrossOrgVisitScopeHolder.setScope(visitScope(null, Set.of("demo:export"), Set.of("fieldA")));
            FileExportGenerateReqVO req = request("demo-export", 5000L, 101L, sequencedBytes(32));
            req.setFields(Set.of("fieldA", "fieldB"));

            // 拒绝路径（执行侧）：声明字段超出授权字段 → FORBIDDEN
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> exportDeliveryService.generateExportFile(req, user(101L, 1L)));
            assertEquals(FORBIDDEN.getCode(), ex.getCode(), "声明字段超出授权字段必须拒绝");
            assertTrue(fileMapper.selectList().isEmpty(), "拒绝路径不得落库");
            assertTrue(objectStore.isEmpty(), "拒绝路径不得写入存储");

            // 裁剪路径（输出侧）：同一裁决输出只含授权字段，供调用方裁剪导出内容
            ObjectAuthorizationRespDTO resp = objectAuthorizationService.authorize(
                    ObjectAuthorizationRequest.of("demo-export", 5000L, 101L, null));
            assertEquals(Set.of("fieldA"), resp.getAuthorizedFields(), "授权输出即裁剪依据（超权字段不在内）");
        }
    }

    @Test
    public void generate_emptyFields_stillChecksObject() {
        when(orgDataPermissionChecker.isObjectVisible(100L, 101L)).thenReturn(false);
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            stubContext(ms);
            FileExportGenerateReqVO req = request("demo-export", 100L, 101L, sequencedBytes(32));
            // fields 为空：不得跳过对象维（字段维与对象维相互独立）
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> exportDeliveryService.generateExportFile(req, user(101L, 1L)));
            assertEquals(FORBIDDEN.getCode(), ex.getCode(), "空字段声明仍必须执行对象维重检");
            // codex r1 P3-3：补强——空字段声明下对象维检查必须真实发生（防与不可见用例重合弱覆盖）
            verify(orgDataPermissionChecker).isObjectVisible(100L, 101L);
        }
    }

    // ========== ⑤ 导出件落盘语义 ==========

    @Test
    public void generate_valid_createsPrivateExportAsset() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            stubContext(ms);
            when(orgDataPermissionChecker.isObjectVisible(100L, 101L)).thenReturn(true);
            byte[] content = sequencedBytes(256);
            FileExportGenerateReqVO req = request("demo-export", 100L, 101L, content);
            req.setName("export-" + randomString() + ".bin");

            FileExportGenerateRespVO resp = exportDeliveryService.generateExportFile(req, user(101L, 1L));

            assertNotNull(resp.getFileId());
            FileDO saved = fileMapper.selectById(resp.getFileId());
            assertNotNull(saved);
            assertEquals("PRIVATE", saved.getScope(), "导出件一律私有（交付不返回存储 URL）");
            assertEquals(101L, saved.getOwnerUserId(), "owner=登录主体");
            assertEquals(100L, saved.getOrganizationId(), "organizationId=源对象组织（生成/交付间撤权载体）");
            assertEquals(1L, saved.getTenantId());
            assertEquals("export", saved.getPurpose());
            assertTrue(saved.getPath().startsWith("export/"), "导出件落盘路径须带用途前缀");
            assertEquals(DigestUtil.sha256Hex(content), saved.getFileHash(), "散列须与内容一致");
            assertNotNull(saved.getRetentionExpireTime(), "导出件必须有保留期到期时间");
            assertTrue(objectStore.containsKey(saved.getPath()), "存储对象必须存在");
            assertEquals((long) content.length, saved.getSize());
        }
    }

    // ========== ⑥ 生成/交付间撤权不泄露（主证据） ==========

    @Test
    public void generate_thenOrgRevoked_beforeIssue_rejected() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            stubContext(ms);
            when(orgDataPermissionChecker.isObjectVisible(100L, 101L)).thenReturn(true);
            FileExportGenerateReqVO req = request("demo-export", 100L, 101L, sequencedBytes(64));
            FileExportGenerateRespVO resp = exportDeliveryService.generateExportFile(req, user(101L, 1L));
            assertNotNull(resp.getFileId(), "生成时重检通过");

            // 生成后组织撤权（转岗/离任/组织停用）：org 门翻转——交付（issue）重检必须拒绝
            when(orgDataPermissionChecker.isObjectVisible(100L, 101L)).thenReturn(false);

            assertThrows(AccessDeniedException.class, () -> deliveryService.issueDeliveryTicket(
                    new FileDeliveryTicketIssueReqVO().setFileId(resp.getFileId()).setPurpose("export"),
                    user(101L, 1L), "sess-101"), "生成/交付间撤权，交付重检必须拒绝");
        }
    }

    @Test
    public void generate_thenRevoke_chunkStops() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            stubContext(ms);
            when(orgDataPermissionChecker.isObjectVisible(100L, 101L)).thenReturn(true);
            byte[] content = sequencedBytes(300);
            FileExportGenerateReqVO req = request("demo-export", 100L, 101L, content);
            FileExportGenerateRespVO resp = exportDeliveryService.generateExportFile(req, user(101L, 1L));
            LoginUser owner = user(101L, 1L);

            FileDeliveryTicketIssueRespVO issued = deliveryService.issueDeliveryTicket(
                    new FileDeliveryTicketIssueReqVO().setFileId(resp.getFileId()).setPurpose("export"),
                    owner, "sess-101");
            FileDeliverySessionRespVO session = deliveryService.redeemDeliveryTicket(
                    issued.getTicketToken(), "export", owner, "sess-101");
            FileDeliveryChunkRespVO chunk = deliveryService.readDeliveryChunk(
                    session.getDeliverySessionId(), 0L, 99L, owner, "sess-101");
            assertArrayEquals(Arrays.copyOfRange(content, 0, 100), chunk.getContent(), "导出件交付取流正常");

            // 在途撤权：后续分块必须停止输出
            deliveryService.revokeDelivery(session.getDeliverySessionId(), owner);
            ServiceException ex = assertThrows(ServiceException.class, () -> deliveryService.readDeliveryChunk(
                    session.getDeliverySessionId(), 100L, 199L, owner, "sess-101"));
            assertEquals(FILE_DELIVERY_TICKET_REVOKED.getCode(), ex.getCode(), "撤权后在途分块必须停止输出");
        }
    }

    // ========== ⑥ 字段等级目录导出不放宽（ZS-PERM-003.B / D-12 §3：脱敏字段不得以明文导出） ==========

    @Test
    public void generate_classified_declaredMaskedField_forbidden() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            stubContext(ms);
            when(orgDataPermissionChecker.isObjectVisible(100L, 101L)).thenReturn(true);
            FileExportGenerateReqVO req = request("demo-export-classified", 100L, 101L, sequencedBytes(32));
            req.setFields(Set.of("secretField"));

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> exportDeliveryService.generateExportFile(req, user(101L, 1L)));
            assertEquals(FORBIDDEN.getCode(), ex.getCode(),
                    "声明字段命中 maskedFields（F2 脱敏字段）必须拒绝——导出按读取等级收敛，不为导出放宽");
            assertTrue(fileMapper.selectList().isEmpty(), "拒绝路径不得落库");
            assertTrue(objectStore.isEmpty(), "拒绝路径不得写入存储");
        }
    }

    @Test
    public void generate_classified_declaredClearField_passes_andOutputForTrimming() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            stubContext(ms);
            when(orgDataPermissionChecker.isObjectVisible(100L, 101L)).thenReturn(true);
            FileExportGenerateReqVO req = request("demo-export-classified", 100L, 101L, sequencedBytes(64));
            req.setFields(Set.of("publicField"));

            FileExportGenerateRespVO resp = exportDeliveryService.generateExportFile(req, user(101L, 1L));
            assertNotNull(resp.getFileId(), "清晰可见字段（F0）导出放行");
            FileDO saved = fileMapper.selectById(resp.getFileId());
            assertNotNull(saved);
            assertEquals("export", saved.getPurpose());

            // 输出侧：同一裁决的 authorizedFields 含脱敏字段、maskedFields 指明脱敏集合（供调用方裁剪依据）
            ObjectAuthorizationRespDTO auth = objectAuthorizationService.authorize(
                    ObjectAuthorizationRequest.of("demo-export-classified", 100L, 101L, null));
            assertEquals(Set.of("publicField", "secretField"), auth.getAuthorizedFields());
            assertEquals(Set.of("secretField"), auth.getMaskedFields());
        }
    }

    // ========== 测试装配：手建统一裁决服务（循 ObjectAuthorizationServiceTest 先例） ==========

    @TestConfiguration
    static class AuthorizationTestConfig {

        @Bean
        public ObjectAuthorizationService objectAuthorizationService(
                SecurityFrameworkService securityFrameworkService,
                OrgDataPermissionChecker orgDataPermissionChecker,
                cn.zszj.framework.common.biz.system.permission.PermissionCommonApi permissionCommonApi) {
            return new ObjectAuthorizationService(securityFrameworkService, orgDataPermissionChecker,
                    new cn.zszj.framework.datapermission.core.authorize.FieldLevelScopeResolver(permissionCommonApi),
                    List.of(new DemoExportProvider(), new NoFieldsExportProvider(),
                            new ClassifiedExportProvider()));
        }

    }

    // ========== 技术夹具（中性演示对象 / provider，不代表任何商业域） ==========

    /** 演示 provider：候选动作为 demo:export；候选字段 fieldA/fieldB（启用字段级输出） */
    static class DemoExportProvider implements ObjectAuthorizationProvider {

        @Override
        public String getObjectType() {
            return "demo-export";
        }

        @Override
        public Collection<String> getCandidateActions() {
            return List.of("demo:export");
        }

        @Override
        public Collection<String> getCandidateFields() {
            return List.of("fieldA", "fieldB");
        }

    }

    /** 不声明候选字段的 provider（未启用字段级输出） */
    static class NoFieldsExportProvider implements ObjectAuthorizationProvider {

        @Override
        public String getObjectType() {
            return "demo-export-nofields";
        }

        @Override
        public Collection<String> getCandidateActions() {
            return List.of("demo:export");
        }

    }

    /**
     * 分级编目 provider（ZS-PERM-003.B 导出不放宽验证用）：publicField=F0、secretField=F2。
     * 测试访问者非 ADMIN/MEMBER 类型（见 {@link #stubContext}）→ 解析上限 F1 → secretField 落 maskedFields。
     */
    static class ClassifiedExportProvider
            implements cn.zszj.framework.datapermission.core.authorize.ClassifiedObjectAuthorizationProvider {

        @Override
        public String getObjectType() {
            return "demo-export-classified";
        }

        @Override
        public Collection<String> getCandidateActions() {
            return List.of("demo:export");
        }

        @Override
        public Collection<String> getCandidateFields() {
            return List.of("publicField", "secretField");
        }

        @Override
        public Map<String, cn.zszj.framework.datapermission.core.authorize.FieldLevel> getFieldLevels() {
            Map<String, cn.zszj.framework.datapermission.core.authorize.FieldLevel> levels = new java.util.HashMap<>();
            levels.put("publicField", cn.zszj.framework.datapermission.core.authorize.FieldLevel.F0);
            levels.put("secretField", cn.zszj.framework.datapermission.core.authorize.FieldLevel.F2);
            return levels;
        }

    }

    // ========== 测试辅助 ==========

    /** 模拟一次跨租户访问请求（循 SEC-001.B 先例：有登录用户 + skipPermissionCheck=true） */
    private void stubVisit(MockedStatic<SecurityFrameworkUtils> ms) {
        LoginUser loginUser = user(101L, 1L);
        loginUser.setVisitTenantId(2L);
        ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
        ms.when(SecurityFrameworkUtils::skipPermissionCheck).thenReturn(true);
    }

    /** 模拟同源安全上下文（codex r1 P2-1 锚定：线程上下文主体与入参主体一致） */
    private static void stubContext(MockedStatic<SecurityFrameworkUtils> ms) {
        ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(user(101L, 1L));
    }

    private static CrossOrgVisitDecisionDTO visitScope(Set<Long> targetOrgIds, Set<String> allowedActions,
                                                       Set<String> allowedFields) {
        CrossOrgVisitDecisionDTO dto = new CrossOrgVisitDecisionDTO();
        dto.setAuthorized(true);
        dto.setTargetTenantId(2L);
        dto.setTargetOrgIds(targetOrgIds);
        dto.setAllowedActions(allowedActions);
        dto.setAllowedFields(allowedFields);
        return dto;
    }

    private FileExportGenerateReqVO request(String objectType, Long orgId, Long ownerUserId, byte[] content) {
        FileExportGenerateReqVO req = new FileExportGenerateReqVO();
        req.setContent(content);
        req.setName("export-" + randomString() + ".bin");
        req.setType("application/octet-stream");
        req.setObjectType(objectType);
        req.setOrgId(orgId);
        req.setOwnerUserId(ownerUserId);
        return req;
    }

    private static LoginUser user(Long id, Long tenantId) {
        return new LoginUser().setId(id).setTenantId(tenantId);
    }

    private static byte[] sequencedBytes(int n) {
        byte[] b = new byte[n];
        for (int i = 0; i < n; i++) {
            b[i] = (byte) (i % 251);
        }
        return b;
    }

}
