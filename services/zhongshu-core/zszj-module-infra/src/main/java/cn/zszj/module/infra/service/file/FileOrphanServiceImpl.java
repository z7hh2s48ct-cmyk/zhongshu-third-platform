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
 *   <li>存储根隔离（codex r0 P1-1，保守拒绝方向）：local 类型配置在与其他配置【共享或嵌套】同一
 *       存储根（basePath）时直接拒绝 preview/cleanup——此时两个 client 清点的是同一批物理文件，
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
                FileObjectEntry entry = findExactEntry(client, path);
                if (entry == null) {
                    // 对象已不存在（重复清理/已被补偿等收敛）：幂等视为成功，不重复调用删除
                    respVO.getSuccessPaths().add(path);
                    continue;
                }
                if (!isOrphanCandidate(configId, entry, retentionBefore, now)) {
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
     * 存储根隔离核验（codex r0 P1-1，保守拒绝）：所选配置为 local 类型时，任何其他 local 配置的
     * 存储根与之【相等或嵌套】（大小写折叠比较）即拒绝；配置无法解析/不可定位同样拒绝。
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
        String selfRoot = localRootOf(selected);
        if (selfRoot == null) {
            throw exception(FILE_ORPHAN_SHARED_STORAGE_REFUSED, selected.getName());
        }
        List<FileConfigDO> others = fileConfigMapper.selectList().stream()
                .filter(c -> !Objects.equals(c.getId(), selected.getId())
                        && FileStorageEnum.LOCAL.getStorage().equals(c.getStorage()))
                .toList();
        for (FileConfigDO other : others) {
            String otherRoot = localRootOf(other);
            // 他配置根不可解析同样拒绝（无法证明不共享）
            if (otherRoot == null || sharesPhysicalRoot(selfRoot, otherRoot)) {
                throw exception(FILE_ORPHAN_SHARED_STORAGE_REFUSED, other.getName());
            }
        }
    }

    /**
     * local 配置的存储根（绝对路径规范化 + 正斜杠统一 + 大小写折叠）；
     * 解析失败返回 null（调用方按「无法证明隔离」拒绝）。
     */
    private String localRootOf(FileConfigDO config) {
        try {
            LocalFileClientConfig localConfig = asLocalConfig(config);
            if (localConfig == null || StrUtil.isEmpty(localConfig.getBasePath())) {
                return null;
            }
            return Paths.get(localConfig.getBasePath()).toAbsolutePath().normalize().toString()
                    .replace('\\', '/').toLowerCase(Locale.ROOT);
        } catch (Exception ex) {
            log.warn("[localRootOf][配置({}) 存储根解析失败，按共享拒绝处理: {}]",
                    config.getId(), ex.getClass().getSimpleName());
            return null;
        }
    }

    private LocalFileClientConfig asLocalConfig(FileConfigDO config) {
        if (config.getConfig() instanceof LocalFileClientConfig localConfig) {
            return localConfig;
        }
        // TypeHandler 兼容场景下可能反序列化为基类/Map，经 JSON 往返按字段映射到 local 配置
        return JsonUtils.parseObject2(JsonUtils.toJsonString(config.getConfig()), LocalFileClientConfig.class);
    }

    /**
     * 物理根共享判定：相等，或一方是另一方的前缀目录（嵌套——外层清点会覆盖内层全部对象）。
     * 入参须为已「小写 + 正斜杠」规范化的根。
     */
    private boolean sharesPhysicalRoot(String selfRoot, String otherRoot) {
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
     * 精确 path 清点（执行前重核验用）：以 path 为前缀清点后取完全匹配项；
     * 未在清点结果中出现视为对象已不存在（幂等成功路径）。
     */
    private FileObjectEntry findExactEntry(FileClient client, String path) {
        List<FileObjectEntry> entries = client.listObjects(path, properties.getOrphan().getScanMaxObjects());
        return entries.stream().filter(e -> StrUtil.equals(e.getPath(), path)).findFirst().orElse(null);
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
