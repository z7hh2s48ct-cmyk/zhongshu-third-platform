package cn.zszj.module.infra.service.file;

import cn.hutool.core.util.StrUtil;
import cn.zszj.framework.common.util.json.JsonUtils;
import cn.zszj.framework.tenant.core.util.TenantUtils;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileOrphanCleanupReqVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileOrphanCleanupRespVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileOrphanItemRespVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileOrphanPreviewRespVO;
import cn.zszj.module.infra.dal.dataobject.file.FileConfigDO;
import cn.zszj.module.infra.dal.mysql.file.FileConfigMapper;
import cn.zszj.module.infra.dal.mysql.file.FileMapper;
import cn.zszj.module.infra.dal.mysql.file.FileUploadCredentialMapper;
import cn.zszj.module.infra.framework.file.config.FileCompensationProperties;
import cn.zszj.module.infra.framework.file.core.client.FileClient;
import cn.zszj.module.infra.framework.file.core.client.FileListing;
import cn.zszj.module.infra.framework.file.core.client.FileObjectEntry;
import cn.zszj.module.infra.framework.file.core.client.local.LocalFileClientConfig;
import cn.zszj.module.infra.framework.file.core.enums.FileStorageEnum;
import cn.zszj.module.infra.framework.file.core.utils.FilePathUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_ORPHAN_CLEANUP_BATCH_EXCEED;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_ORPHAN_LISTING_NOT_SUPPORTED;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_ORPHAN_PATH_UNVERIFIABLE;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_ORPHAN_SHARED_STORAGE_REFUSED;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_PATH_INVALID;

/**
 * 孤儿对象清理 Service 实现（ZS-FILE-005.B）。
 *
 * <p>合同（主卡调整「清理前核验引用和授权」+ 退出条件「重复清理不误删且最终对账一致」）：
 * <ul>
 *   <li>孤儿定义：存储对象在 infra_file 无任何租户的记录引用——核验必须跨租户全局
 *       （{@link TenantUtils#executeIgnore}）：若被租户过滤，他租户在同 config+path 的记录会被
 *       本租户操作方误判为孤儿（「不跨技术租户误删」的反面）；引用匹配按 LOWER 折叠
 *       （codex r0 P1-2：大小写不敏感文件系统下清点拼写与记录拼写可能不同，精确匹配漏检会误删活文件；
 *       大小写敏感系统上折叠属保守方向——宁可多保留）；</li>
 *   <li>存储根隔离（codex r0 P1-1 + r1 P1-A/P1-B，保守拒绝方向）：local 类型配置在与其他配置【共享或嵌套】同一
 *       存储根（物理真实路径，toRealPath 解析 junction/symlink 别名——字符串规范化发现不了别名共享，
 *       r1 P1-A；尾分隔符剥离合同防 C:\ 类根的 c:// 边界失配，r1 P1-B；根不存在/解析失败同拒）时
 *       直接拒绝 preview/cleanup——此时两个 client 清点的是同一批物理文件，
 *       而引用核验只能按所选 configId 查，他配置名下引用的同物理文件过保留期会被误删；
 *       物理身份跨配置判定（方案①）需为每种存储建模根身份且嵌套拼写映射复杂，保守拒绝（方案②）
 *       宁可少删——退出条件「不误删」优先于检测最大化；db 类型按 config_id 在 infra_file_content
 *       内命名空间隔离，无物理共享面；配置无法解析为 local 根时同样拒绝（无法证明隔离即拒绝）；</li>
 *   <li>保留期：存储最后修改时间早于 now-retentionDays 才进入候选；lastModified 未知保守跳过
 *       （不清理年龄未知对象）；</li>
 *   <li>temp/ 在途保护：临时对象存在未过期 WAITING 凭证认领即视为在途直传，不进入候选；</li>
 *   <li>两步授权：预览只读，前缀参数使截断扫描可推进（codex r0 P2-2——首段全被引用时
 *       后部孤儿经逐段前缀续扫可见）；清理按显式 path 批次（有界），执行前逐 path 重新清点+重核验
 *       （闭合预览→清理窗口内被引用/被认领的竞态），逐项记录成败，不伪报全成功；</li>
 *   <li>「确认不存在」与「不可验证」分开（codex r3 P2）：执行前清点经 {@code listObjectsDetailed}
 *       取回被跳过（符号链接/别名/解析失败）目录前缀与被跳过文件路径——条目缺失但命中不可验证
 *       盲区（或清点被截断）的 path 按失败拒绝（037 ORPHAN_PATH_UNVERIFIABLE），
 *       绝不计入成功（防止被跳过目录下真实存在的对象收到假成功）；</li>
 *   <li>失败留痕仅受控描述（异常类名），不落异常原文（循 JOB-002/004 脱敏红线）。</li>
 * </ul>
 */
@Slf4j
@Service
@Validated
public class FileOrphanServiceImpl implements FileOrphanService {

    @Resource
    private FileConfigService fileConfigService;
    @Resource
    private FileConfigMapper fileConfigMapper;
    @Resource
    private FileMapper fileMapper;
    @Resource
    private FileUploadCredentialMapper fileUploadCredentialMapper;
    @Resource
    private FileCompensationProperties properties;

    @Override
    public FileOrphanPreviewRespVO preview(Long configId, String prefix) {
        String safePrefix = validatePrefix(prefix);
        FileClient client = resolveClient(configId);
        ensureStorageRootIsolated(configId);
        Long resolvedConfigId = client.getId();
        int max = properties.getOrphan().getScanMaxObjects();
        List<FileObjectEntry> entries;
        try {
            entries = client.listObjects(safePrefix, max);
        } catch (UnsupportedOperationException ex) {
            // 存储不支持清点：保守报错，不假报「空清单」（假空会诱导操作方认为无孤儿）
            throw exception(FILE_ORPHAN_LISTING_NOT_SUPPORTED);
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime retentionBefore = now.minusDays(properties.getOrphan().getRetentionDays());

        FileOrphanPreviewRespVO respVO = new FileOrphanPreviewRespVO();
        respVO.setConfigId(resolvedConfigId);
        respVO.setPrefix(safePrefix);
        respVO.setTruncated(entries.size() >= max);
        respVO.setItems(entries.stream()
                .filter(entry -> isOrphanCandidate(resolvedConfigId, entry, retentionBefore, now))
                .map(this::toItem)
                .toList());
        log.info("[preview][存储({}) 孤儿预览：前缀[{}] 清点 {} 个对象，候选 {} 个，截断={}]",
                resolvedConfigId, safePrefix, entries.size(), respVO.getItems().size(), respVO.isTruncated());
        return respVO;
    }

    @Override
    public FileOrphanCleanupRespVO cleanup(FileOrphanCleanupReqVO reqVO) {
        int maxPaths = properties.getOrphan().getCleanupMaxPaths();
        if (reqVO.getPaths().size() > maxPaths) {
            throw exception(FILE_ORPHAN_CLEANUP_BATCH_EXCEED, reqVO.getPaths().size(), maxPaths);
        }
        FileClient client = resolveClient(reqVO.getConfigId());
        ensureStorageRootIsolated(reqVO.getConfigId());
        Long configId = client.getId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime retentionBefore = now.minusDays(properties.getOrphan().getRetentionDays());

        FileOrphanCleanupRespVO respVO = new FileOrphanCleanupRespVO();
        for (String path : reqVO.getPaths()) {
            try {
                // 执行前重清点 + 重核验（预览→清理窗口内的引用/认领竞态防御）
                FilePathUtils.validatePath(path);
                ExactEntryLookup lookup = findExactEntry(client, path);
                if (lookup.entry == null) {
                    if (lookup.unverifiable) {
                        // codex r3 P2：「未出现在清点」≠「已不存在」——路径位于被跳过（符号链接/
                        // 别名/解析失败）目录下、与其被跳过对象同名或清点被截断时存在性不可证明，
                        // 按失败处理，绝不计入 successPaths（「确认不存在」与「不可验证」分开）
                        addFailure(respVO, path, "ORPHAN_PATH_UNVERIFIABLE("
                                + FILE_ORPHAN_PATH_UNVERIFIABLE.getCode() + ")："
                                + FILE_ORPHAN_PATH_UNVERIFIABLE.getMsg());
                        continue;
                    }
                    // 对象已不存在（重复清理/已被补偿等收敛）：幂等视为成功，不重复调用删除
                    respVO.getSuccessPaths().add(path);
                    continue;
                }
                if (!isOrphanCandidate(configId, lookup.entry, retentionBefore, now)) {
                    addFailure(respVO, path, "ORPHAN_CONDITION_NOT_HOLD（引用/保留期/在途凭证核验未通过，跳过）");
                    continue;
                }
                client.delete(path);
                respVO.getSuccessPaths().add(path);
                log.info("[cleanup][存储({}) 孤儿对象({}) 已清理]", configId, path);
            } catch (Exception ex) {
                addFailure(respVO, path, "STORAGE_ERROR:" + ex.getClass().getSimpleName());
                log.warn("[cleanup][存储({}) 孤儿对象({}) 清理失败: {}]", configId, path,
                        ex.getClass().getName());
            }
        }
        log.info("[cleanup][存储({}) 孤儿清理：请求 {} 个，成功 {} 个，失败 {} 个]",
                configId, reqVO.getPaths().size(), respVO.getSuccessPaths().size(), respVO.getFailures().size());
        return respVO;
    }

    // ========== 内部方法 ==========

    private FileClient resolveClient(Long configId) {
        FileClient client = configId == null
                ? fileConfigService.getMasterFileClient()
                : fileConfigService.getFileClient(configId);
        Objects.requireNonNull(client, "客户端(" + configId + ") 不能为空");
        return client;
    }

    /**
     * 清点前缀校验（codex r0 P2-2）：前缀是「路径片段」而非完整路径（如 asset/z，允许尾段截断），
     * 宽容校验——禁绝对路径/盘符/反斜杠/空字符/目录穿越段，其余常规片段放行。
     */
    private String validatePrefix(String prefix) {
        if (StrUtil.isEmpty(prefix)) {
            return "";
        }
        if (StrUtil.startWithAny(prefix, "/", "\\") || StrUtil.contains(prefix, "\\")
                || prefix.indexOf('\0') >= 0
                || (prefix.length() >= 2 && Character.isLetter(prefix.charAt(0)) && prefix.charAt(1) == ':')) {
            throw exception(FILE_PATH_INVALID);
        }
        for (String segment : prefix.split("/", -1)) {
            if ("..".equals(segment) || ".".equals(segment)) {
                throw exception(FILE_PATH_INVALID);
            }
        }
        return prefix;
    }

    /**
     * 存储根隔离核验（codex r0 P1-1 + r1 P1-A/P1-B + r2 P1-A/P1-B，保守拒绝方向）：
     * 所选配置为 local 类型时，任何其他 local 配置的存储根与之【相等或嵌套】即拒绝。
     * codex r2 根治：隔离判定必须【词汇视角】与【物理视角】同时证明不共享——
     * 词汇视角 = client 实际 I/O 使用的根（{@code toAbsolutePath().normalize()}，与
     * LocalFileClient.getFilePath 同一变换，不解链接；Windows Files.walk 默认遍历 junction，
     * 词汇嵌套的 alias junction 会被外层清点洗进内层物理文件）；物理视角 = toRealPath 解析
     * 别名后的真实根（同一目录不同别名共享）。两视角任一判「共享/嵌套」即拒绝，
     * 只有双视角都互斥才放行；校验路径先词汇规范化再 toRealPath（r2 P1-B：保证
     * 「校验的根 == I/O 实际用的根」——否则 {@code alias/..} 形状经物理解析被洗到别处）。
     * 配置无法解析/不可定位/物理解析失败（不存在/IO/环）同样拒绝（无法证明隔离即拒绝）。
     */
    private void ensureStorageRootIsolated(Long configId) {
        FileConfigDO selected = configId != null
                ? fileConfigMapper.selectById(configId)
                : fileConfigMapper.selectByMaster();
        // 无法证明隔离即拒绝（保守方向优于检测最大化）
        if (selected == null || selected.getConfig() == null) {
            throw exception(FILE_ORPHAN_SHARED_STORAGE_REFUSED, "自身配置不可解析");
        }
        if (!FileStorageEnum.LOCAL.getStorage().equals(selected.getStorage())) {
            // db 类型按 config_id 在 infra_file_content 内命名空间隔离，无物理共享面；
            // s3/ftp/sftp 不支持清点（034），本检查不适用
            return;
        }
        LocalRootViews selfViews = localRootViewsOf(selected);
        if (selfViews == null) {
            throw exception(FILE_ORPHAN_SHARED_STORAGE_REFUSED, selected.getName());
        }
        List<FileConfigDO> others = fileConfigMapper.selectList().stream()
                .filter(c -> !Objects.equals(c.getId(), selected.getId())
                        && FileStorageEnum.LOCAL.getStorage().equals(c.getStorage()))
                .toList();
        for (FileConfigDO other : others) {
            LocalRootViews otherViews = localRootViewsOf(other);
            // 他配置根不可解析同样拒绝（无法证明不共享）
            if (otherViews == null
                    || sharesPhysicalRoot(selfViews.lexical, otherViews.lexical)
                    || sharesPhysicalRoot(selfViews.physical, otherViews.physical)) {
                throw exception(FILE_ORPHAN_SHARED_STORAGE_REFUSED, other.getName());
            }
        }
    }

    /**
     * local 配置存储根的【双视角】规范化：词汇视角（client I/O 实际使用的根）与
     * 物理视角（toRealPath 解析别名后的真实根）。任一解析失败/设备形态（{@code \\?\}、
     * {@code \\.\}——subst/UNC 等无法安全比较的形态）返回 null（保守拒绝）。
     */
    private LocalRootViews localRootViewsOf(FileConfigDO config) {
        try {
            LocalFileClientConfig localConfig = asLocalConfig(config);
            if (localConfig == null || StrUtil.isEmpty(localConfig.getBasePath())) {
                return null;
            }
            // r2 P1-B：先做与 LocalFileClient.getFilePath 完全相同的词汇规范化（不解链接），
            // 再解析物理别名——保证「校验的根 == I/O 实际用的根」
            java.nio.file.Path lexicalPath = Paths.get(localConfig.getBasePath()).toAbsolutePath().normalize();
            String lexical = toCanonicalRoot(lexicalPath);
            if (isDeviceForm(lexical)) {
                return null;
            }
            // toRealPath 要求路径真实存在：未创建的存储根同样拒绝（无物可清点，拒绝无损失且保守）
            String physical = toCanonicalRoot(lexicalPath.toRealPath());
            if (isDeviceForm(physical)) {
                return null;
            }
            return new LocalRootViews(lexical, physical);
        } catch (Exception ex) {
            log.warn("[localRootViewsOf][配置({}) 存储根解析失败，按共享拒绝处理: {}]",
                    config.getId(), ex.getClass().getSimpleName());
            return null;
        }
    }

    /**
     * Windows 设备形态路径（{@code \\?\} 前缀 / 设备命名空间 {@code \\.\}，subst/UNC 解析的
     * 可能产物）：字符串比较无法安全判定物理同一性 → 保守拒绝。
     */
    private static boolean isDeviceForm(String canonicalRoot) {
        return canonicalRoot.startsWith("//?/") || canonicalRoot.startsWith("//./");
    }

    /**
     * 双视角根（词汇 + 物理）
     */
    private static final class LocalRootViews {

        private final String lexical;
        private final String physical;

        private LocalRootViews(String lexical, String physical) {
            this.lexical = lexical;
            this.physical = physical;
        }

    }

    /**
     * 物理路径 → 规范化根字符串（纯字符串变换，不触 FS）：正斜杠统一 + 大小写折叠 +
     * 尾分隔符剥离（根路径如 {@code Z:\} 规范化后仍带尾分隔符，P1-B 合同：无尾分隔符）；
     * 剥离至空（整个文件系统根 {@code /}）时保留 {@code /}。
     * 包内可见供单测固化合同。
     */
    static String toCanonicalRoot(java.nio.file.Path realPath) {
        String root = realPath.normalize().toString().replace('\\', '/').toLowerCase(Locale.ROOT);
        while (root.length() > 1 && root.endsWith("/")) {
            root = root.substring(0, root.length() - 1);
        }
        return root.isEmpty() ? "/" : root;
    }

    private LocalFileClientConfig asLocalConfig(FileConfigDO config) {
        if (config.getConfig() instanceof LocalFileClientConfig localConfig) {
            return localConfig;
        }
        // TypeHandler 兼容场景下可能反序列化为基类/Map，经 JSON 往返按字段映射到 local 配置
        return JsonUtils.parseObject2(JsonUtils.toJsonString(config.getConfig()), LocalFileClientConfig.class);
    }

    /**
     * 根共享判定（对单一视角）：相等，或一方是另一方的前缀目录（嵌套——外层清点会覆盖内层
     * 全部对象）。入参合同（{@link #toCanonicalRoot}）：无尾分隔符——边界分隔符只补不加，
     * 杜绝 {@code c://} 式失配（P1-B）；任一方为文件系统根 {@code /} 时视为共享（包罗一切）。
     * 包内可见供单测固化合同。
     */
    static boolean sharesPhysicalRoot(String selfRoot, String otherRoot) {
        if ("/".equals(selfRoot) || "/".equals(otherRoot)) {
            return true;
        }
        return selfRoot.equals(otherRoot)
                || selfRoot.startsWith(otherRoot + "/")
                || otherRoot.startsWith(selfRoot + "/");
    }

    /**
     * 孤儿候选判定（三项核验，任一不过即排除）。
     */
    private boolean isOrphanCandidate(Long configId, FileObjectEntry entry,
                                      LocalDateTime retentionBefore, LocalDateTime now) {
        // ① 保留期：lastModified 未知保守跳过（不清理年龄未知对象）
        if (entry.getLastModified() == null || entry.getLastModified().isAfter(retentionBefore)) {
            return false;
        }
        String path = entry.getPath();
        // ② 引用核验（跨租户全局 + LOWER 折叠，codex r0 P1-2）：任意租户存在引用等价记录即非孤儿
        Long fileRefs = TenantUtils.executeIgnore(() ->
                fileMapper.selectCountByConfigIdAndPathIgnoreTenant(configId, path));
        if (fileRefs != null && fileRefs > 0) {
            return false;
        }
        // ③ temp/ 在途保护（LOWER 折叠同上）：存在未过期 WAITING 凭证认领即非孤儿
        if (path.startsWith(FileServiceImpl.TEMP_PATH_PREFIX)) {
            Long activeCredentials = TenantUtils.executeIgnore(() ->
                    fileUploadCredentialMapper.selectCountActiveByTempPathIgnoreTenant(path, now));
            return activeCredentials == null || activeCredentials == 0;
        }
        return true;
    }

    /**
     * 精确 path 清点 + 可验证性判定（执行前重核验用；codex r3 P2 把「条目缺失」的两种语义分开）：
     * <ul>
     *   <li>{@code entry != null}：对象在清点中出现，可正常核验孤儿条件；</li>
     *   <li>{@code entry == null ∧ unverifiable=false}：完整（未截断）清点中未出现、且路径不落任何
     *       不可验证盲区 → 对象确认不存在（幂等收敛）；</li>
     *   <li>{@code entry == null ∧ unverifiable=true}：路径命中被跳过（符号链接/别名/解析失败）
     *       目录前缀、与其被跳过对象同名，或清点被截断（缺失可能只是截断产物）→ 存在性无法核验，
     *       调用方必须按失败拒绝，不得假报幂等成功。</li>
     * </ul>
     */
    private ExactEntryLookup findExactEntry(FileClient client, String path) {
        int max = properties.getOrphan().getScanMaxObjects();
        FileListing listing = client.listObjectsDetailed(path, max);
        FileObjectEntry entry = listing.getEntries().stream()
                .filter(e -> StrUtil.equals(e.getPath(), path)).findFirst().orElse(null);
        if (entry != null) {
            return new ExactEntryLookup(entry, false);
        }
        boolean unverifiable = listing.getEntries().size() >= max
                || isPathUnverifiable(listing, path);
        return new ExactEntryLookup(null, unverifiable);
    }

    /**
     * codex r3 P2：path 命中存储侧「不可验证」集合即拒绝假成功——
     * ①位于被跳过（符号链接/别名/解析失败）目录前缀之下；②与被跳过文件精确同名。
     * 两者均按忽略大小写匹配：大小写不敏感文件系统上，变体拼写与被跳过目录/文件寻址
     * 同一物理对象（大小写敏感存储上属保守方向——宁可多拒绝）。
     */
    private static boolean isPathUnverifiable(FileListing listing, String path) {
        for (String prefix : listing.getUnverifiablePrefixes()) {
            if (path.length() >= prefix.length()
                    && path.regionMatches(true, 0, prefix, 0, prefix.length())) {
                return true;
            }
        }
        for (String skipped : listing.getUnverifiablePaths()) {
            if (path.equalsIgnoreCase(skipped)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 精确清点结果（codex r3 P2）：见 {@link #findExactEntry}
     */
    private static final class ExactEntryLookup {

        private final FileObjectEntry entry;
        private final boolean unverifiable;

        private ExactEntryLookup(FileObjectEntry entry, boolean unverifiable) {
            this.entry = entry;
            this.unverifiable = unverifiable;
        }

    }

    private FileOrphanItemRespVO toItem(FileObjectEntry entry) {
        FileOrphanItemRespVO item = new FileOrphanItemRespVO();
        item.setPath(entry.getPath());
        item.setSize(entry.getSize());
        item.setLastModified(entry.getLastModified());
        item.setCategory(entry.getPath().startsWith(FileServiceImpl.TEMP_PATH_PREFIX)
                ? FileOrphanItemRespVO.CATEGORY_TEMP : FileOrphanItemRespVO.CATEGORY_ASSET);
        return item;
    }

    private void addFailure(FileOrphanCleanupRespVO respVO, String path, String message) {
        FileOrphanCleanupRespVO.Failure failure = new FileOrphanCleanupRespVO.Failure();
        failure.setPath(path);
        failure.setErrorMessage(message);
        respVO.getFailures().add(failure);
    }

}
