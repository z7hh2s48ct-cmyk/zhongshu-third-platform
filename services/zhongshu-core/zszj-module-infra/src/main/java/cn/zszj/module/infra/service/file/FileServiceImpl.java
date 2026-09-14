package cn.zszj.module.infra.service.file;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.common.util.http.HttpUtils;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.framework.tenant.core.util.TenantUtils;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.framework.common.util.object.BeanUtils;
import cn.zszj.module.infra.enums.file.FileScopeEnum;
import cn.zszj.module.infra.framework.file.config.FileProperties;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileCreateReqVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FilePageReqVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FilePresignedUrlRespVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileUploadCredentialCompleteReqVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileUploadCredentialCreateReqVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileUploadCredentialCreateRespVO;
import cn.zszj.module.infra.dal.dataobject.file.FileDO;
import cn.zszj.module.infra.dal.dataobject.file.FileUploadCredentialDO;
import cn.zszj.module.infra.dal.mysql.file.FileMapper;
import cn.zszj.module.infra.dal.mysql.file.FileUploadCredentialMapper;
import cn.zszj.module.infra.framework.file.core.client.FileClient;
import cn.zszj.module.infra.framework.file.core.utils.FilePathUtils;
import cn.zszj.module.infra.framework.file.core.utils.FileTypeUtils;
import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static cn.hutool.core.date.DatePattern.PURE_DATE_PATTERN;
import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_NOT_EXISTS;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_PRESIGN_NOT_SUPPORTED;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_UPLOAD_CREDENTIAL_NOT_EXISTS;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_UPLOAD_CREDENTIAL_EXPIRED;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_UPLOAD_CREDENTIAL_ALREADY_USED;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_UPLOAD_CREDENTIAL_FORBIDDEN;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_UPLOAD_TEMP_EMPTY;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_UPLOAD_TEMP_SIZE_MISMATCH;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_UPLOAD_TEMP_HASH_MISMATCH;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_DANGEROUS_CONTENT;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_UPLOAD_CONCURRENT_LIMIT;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_PUBLIC_TYPE_NOT_ALLOWED;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_SIZE_EXCEED;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_TYPE_MISMATCH;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_SCOPE_INVALID;

/**
 * 文件 Service 实现类
 *
 * @author 芋道源码
 */
@Slf4j
@Service
public class FileServiceImpl implements FileService {

    /**
     * 上传文件的前缀，是否包含日期（yyyyMMdd）
     *
     * 目的：按照日期，进行分目录
     */
    static boolean PATH_PREFIX_DATE_ENABLE = true;
    /**
     * 上传文件的后缀，是否启用
     *
     * 算法：当前时间戳（毫秒）+ 5 位随机数；目的是保证文件的唯一性，避免覆盖
     * 定制：可按需调整成 UUID、或者其他方式
     */
    static boolean PATH_SUFFIX_TIMESTAMP_ENABLE = true; // ZS-FILE-002：服务端唯一对象键（同日同名不覆盖）
    /**
     * 后缀是否作为上级目录
     *
     * true：{@code yyyyMMdd/<后缀>/原文件名.ext}；保留原文件名
     * false：{@code yyyyMMdd/原文件名_<后缀>.ext}；后缀拼到文件名
     */
    static boolean PATH_SUFFIX_AS_DIRECTORY = true;

    @Resource
    private FileConfigService fileConfigService;
    @Resource
    private cn.zszj.framework.common.biz.system.permission.PermissionCommonApi permissionCommonApi;
    @Resource
    private cn.zszj.module.infra.framework.file.config.FileProperties fileProperties;
    @Resource
    private FileUploadCredentialMapper fileUploadCredentialMapper;

    /**
     * ZS-FILE-002 codex r0 P2：在途上传许可（批量/内存占用预算）。包级可见便于测试注入许可。
     */
    java.util.concurrent.Semaphore uploadPermits;

    private java.util.concurrent.Semaphore uploadPermits() {
        if (uploadPermits == null) {
            synchronized (this) {
                if (uploadPermits == null) {
                    uploadPermits = new java.util.concurrent.Semaphore(
                            fileProperties.getMaxConcurrentUploads() != null
                                    ? fileProperties.getMaxConcurrentUploads() : 32);
                }
            }
        }
        return uploadPermits;
    }

    @Resource
    private FileMapper fileMapper;

    @Override
    public PageResult<FileDO> getFilePage(FilePageReqVO pageReqVO) {
        return fileMapper.selectPage(pageReqVO);
    }

    @Override
    @SneakyThrows
    public String createFile(byte[] content, String name, String directory, String type) {
        // ZS-FILE-002 codex r0 P2：在途上传准入（内存占用预算），超出即拒绝
        boolean acquired = uploadPermits().tryAcquire();
        if (!acquired) {
            throw exception(FILE_UPLOAD_CONCURRENT_LIMIT);
        }
        try {
            return doCreateFile(content, name, directory, type);
        } finally {
            uploadPermits.release();
        }
    }

    @SneakyThrows
    private String doCreateFile(byte[] content, String name, String directory, String type) {
        // 1.1 处理 name 的合法性，禁止携带目录路径
        name = FilePathUtils.validateFileName(name);

        // 1.2.1 ZS-FILE-002：大小限额（service 层显式，不依赖 multipart 兜底）
        if (content.length > fileProperties.getMaxSize()) {
            throw exception(FILE_SIZE_EXCEED, content.length, fileProperties.getMaxSize());
        }
        // 1.2.2 codex r0 P1：服务端以【纯内容】探测类型（不含文件名提示）——文件名参与探测会自证
        // （未知二进制随 fake.png 名被升级为 image/png 通过白名单）；调用方声明的 type 一律不信任。
        // 未知内容保持 octet-stream，不凭扩展名升级为可信类型
        String declaredType = type;
        type = FileTypeUtils.getMineType(content);
        // 1.2.3 处理 name 为空：以内容散列为名（原行为保留）
        if (StrUtil.isEmpty(name)) {
            name = DigestUtil.sha256Hex(content);
        }
        // 1.2.4 无扩展名时按探测类型补全（基于纯内容探测，可信）
        if (StrUtil.isEmpty(FileUtil.extName(name))) {
            String extension = FileTypeUtils.getExtension(type);
            if (StrUtil.isNotEmpty(extension)) {
                name = name + extension;
            }
        }
        // 1.2.4 codex r0 P1：危险扩展名黑名单作用于【补全后的最终名】且优先于一致性校验
        validateDangerExtension(name);
        // 1.2.5 codex r0 P2：一致性按探测 MIME 的【合法扩展名集合】归一比较（jpg/jpeg/jfif 同为 image/jpeg）        // 1.2.2 处理 name 为空的情况
        validateExtensionConsistency(name, type);

        // 2.1 生成上传的 path，需要保证唯一
        String path = generateUploadPath(name, directory);
        // 2.2 上传到文件存储器
        FileClient client = fileConfigService.getMasterFileClient();
        Assert.notNull(client, "客户端(master) 不能为空");
        String url = client.upload(content, path, type);

        // 3. 保存到数据库。ZS-FILE-001.A：记录上传主体、默认私有（公开素材须管理员显式调整）
        FileDO file = new FileDO().setConfigId(client.getId())
                .setName(name).setPath(path).setUrl(url)
                .setType(type).setSize((long) content.length)
                .setFileHash(DigestUtil.sha256Hex(content))
                .setOwnerUserId(currentUserOrZero()).setScope(FileScopeEnum.PRIVATE.getScope());
        // ZS-FILE-001.A：显式记录技术租户（服务端确认归属，不依赖拦截器装配）
        file.setTenantId(TenantContextHolder.getTenantId());
        fileMapper.insert(file);
        return url;
    }

    @VisibleForTesting
    String generateUploadPath(String name, String directory) {
        // 1.1 处理 name 和 directory 的合法性
        name = FilePathUtils.validateFileName(name);
        FilePathUtils.validatePath(name);
        FilePathUtils.validateDirectory(directory);
        // 1.2 生成前缀、后缀
        String prefix = null;
        if (PATH_PREFIX_DATE_ENABLE) {
            prefix = LocalDateTimeUtil.format(LocalDateTimeUtil.now(), PURE_DATE_PATTERN);
        }
        String suffix = null;
        if (PATH_SUFFIX_TIMESTAMP_ENABLE) {
            // ZS-FILE-002 codex r1 P2：UUID 熵增——原毫秒+5 位随机每毫秒仅 9 万种，
            // 同毫秒并发同名仍有 1/90000 撞键概率；UUID 32 hex 使碰撞概率可忽略
            suffix = IdUtil.fastSimpleUUID();
        }

        // 2.1 先拼接 suffix 后缀
        if (StrUtil.isNotEmpty(suffix)) {
            if (PATH_SUFFIX_AS_DIRECTORY) {
                name = suffix + StrUtil.SLASH + name;
            } else {
                String ext = FileUtil.extName(name);
                if (StrUtil.isNotEmpty(ext)) {
                    name = FileUtil.mainName(name) + StrUtil.C_UNDERLINE + suffix + StrUtil.DOT + ext;
                } else {
                    name = name + StrUtil.C_UNDERLINE + suffix;
                }
            }
        }
        // 2.2 再拼接 prefix 前缀
        if (StrUtil.isNotEmpty(prefix)) {
            name = prefix + StrUtil.SLASH + name;
        }
        // 2.3 最后拼接 directory 目录
        if (StrUtil.isNotEmpty(directory)) {
            name = directory + StrUtil.SLASH + name;
        }
        return name;
    }

    @Override
    @SneakyThrows
    public FilePresignedUrlRespVO presignPutUrl(String name, String directory) {
        // 1. 生成上传的 path，需要保证唯一
        String path = generateUploadPath(name, directory);

        // 2. 获取文件预签名地址
        FileClient fileClient = fileConfigService.getMasterFileClient();
        String uploadUrl = fileClient.presignPutUrl(path);
        String visitUrl = fileClient.presignGetUrl(path, null);
        return new FilePresignedUrlRespVO().setConfigId(fileClient.getId())
                .setPath(path).setUploadUrl(uploadUrl).setUrl(visitUrl);
    }

    @Override
    public String presignGetUrl(String url, Integer expirationSeconds) {
        FileClient fileClient = fileConfigService.getMasterFileClient();
        return fileClient.presignGetUrl(url, expirationSeconds);
    }

    @Override
    public Long createFile(FileCreateReqVO createReqVO) {
        // 1.1 校验参数的合法性
        FilePathUtils.validatePath(createReqVO.getPath());
        createReqVO.setName(FilePathUtils.validateFileName(createReqVO.getName()));
        // 1.2 处理 URL 的合法性，移除 URL 中的查询参数（例如签名参数），保证 URL 的唯一性
        createReqVO.setUrl(HttpUtils.removeUrlQuery(createReqVO.getUrl())); // 目的：移除私有桶情况下，URL 的签名参数

        // 2. 保存到数据库。ZS-FILE-001.A（codex r0 P1）：presigned create 端点已禁用（无上传申请绑定
        // 无法证明对象归属，可冒领他人 configId/path 生成记录），本方法保留待 FILE-003 凭证化后重新接线；
        // configId 不信任客户端指定，空值由服务端取 master 存储配置兜底；显式记录技术租户
        FileDO file = BeanUtils.toBean(createReqVO, FileDO.class);
        if (file.getConfigId() == null) {
            file.setConfigId(fileConfigService.getMasterFileClient().getId());
        }
        file.setOwnerUserId(currentUserOrZero()).setScope(FileScopeEnum.PRIVATE.getScope())
                .setTenantId(TenantContextHolder.getTenantId());
        fileMapper.insert(file);
        return file.getId();
    }

    @Override
    public FileDO getFile(Long id) {
        return validateFileExists(id);
    }

    @Override
    public void deleteFile(Long id) throws Exception {
        // 1.1 校验存在
        FileDO file = validateFileExists(id);
        // 1.2 校验路径合法性，避免误删文件存储器中的其他文件
        FilePathUtils.validatePath(file.getPath());

        // 2.1 从文件存储器中删除
        FileClient client = fileConfigService.getFileClient(file.getConfigId());
        Assert.notNull(client, "客户端({}) 不能为空", file.getConfigId());
        client.delete(file.getPath());

        // 2.2 删除记录
        fileMapper.deleteById(id);
    }

    @Override
    @SneakyThrows
    public void deleteFileList(List<Long> ids) {
        // ZS-FILE-001.A（codex r0 P2）：批量删除前显式校验「全部存在且全部属于当前技术租户」——
        // 混入他租户/不存在 id 整批拒绝（原先静默跳过，越权文件混入无感知）；不依赖租户拦截器装配
        Long currentTenantId = TenantContextHolder.getTenantId();
        List<FileDO> files = fileMapper.selectByIds(ids);
        if (files.size() != CollUtil.distinct(ids).size()
                || files.stream().anyMatch(f -> !Objects.equals(f.getTenantId(), currentTenantId))) {
            throw exception(FILE_NOT_EXISTS);
        }
        // 删除文件
        for (FileDO file : files) {
            FilePathUtils.validatePath(file.getPath());
            // 获取客户端
            FileClient client = fileConfigService.getFileClient(file.getConfigId());
            Assert.notNull(client, "客户端({}) 不能为空", file.getPath());
            // 删除文件
            client.delete(file.getPath());
        }

        // 删除记录
        fileMapper.deleteByIds(ids);
    }

    private FileDO validateFileExists(Long id) {
        FileDO fileDO = fileMapper.selectById(id);
        if (fileDO == null) {
            throw exception(FILE_NOT_EXISTS);
        }
        return fileDO;
    }

    @Override
    public byte[] getFileContent(Long configId, String path) throws Exception {
        // 1. 校验路径合法性
        FilePathUtils.validatePath(path);

        // 2.1 获取客户端
        FileClient client = fileConfigService.getFileClient(configId);
        Assert.notNull(client, "客户端({}) 不能为空", configId);
        // 2.2 获取文件内容
        return client.getContent(path);
    }

    /**
     * ZS-FILE-001.A（codex r0 P2）：下载场景跨租户定位文件记录——公开素材必须对任意租户/匿名
     * 保持同一可用地址（租户过滤会把他租户 PUBLIC 过滤成 404）；PRIVATE 的租户归属校验
     * 由 {@link #validateFileReadable} 以记录自身 tenant_id 执行。
     */
    @Override
    public FileDO getFileByConfigIdAndPathIgnoreTenant(Long configId, String path) {
        return TenantUtils.executeIgnore(() ->
                fileMapper.selectLatestByConfigIdAndPath(configId, path));
    }

    @Override
    public FileDO getFileByConfigIdAndPath(Long configId, String path) {
        return fileMapper.selectLatestByConfigIdAndPath(configId, path);
    }


    /**
     * ZS-FILE-002 codex r1 P2：ZIP 容器家族扩展名（纯内容探测为 application/zip，但业务扩展名合法）
     */
    private static final java.util.Set<String> ZIP_CONTAINER_EXTENSIONS = java.util.Set.of(
            "jar", "war", "ear", "apk", "docx", "xlsx", "pptx", "odt", "ods", "odp", "epub");

    /**
     * ZS-FILE-001.A：当前登录用户编号；匿名/系统上下文返回 0（owner 列 NOT NULL DEFAULT 0 语义一致）。
     */
    private Long currentUserOrZero() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        return userId != null ? userId : 0L;
    }

    /**
     * ZS-FILE-001.A：统一读取授权——PUBLIC 匿名可读；PRIVATE 需登录且与文件同技术租户。
     *
     * @param file      文件（含 scope 与 tenantId）
     * @param loginUser 下载发起者（匿名传 null；由 Controller 从安全上下文透传，便于单测）
     * @throws AccessDeniedException 私有文件匿名/跨租户读取
     */
    @Override
    public void validateFileReadable(FileDO file, LoginUser loginUser) {
        if (file == null) {
            return; // 不存在由调用方按 404 处理
        }
        if (FileScopeEnum.PUBLIC.getScope().equals(file.getScope())) {
            return; // 公开素材：批准用途内匿名可读
        }
        // 私有附件（codex r0 P1 + r1 P1/P2）：必须登录，且满足其一——
        // ① 上传所有者本人（ownerUserId>0 且与登录主体匹配且同租户；0=无个人所有者，禁止凭 userId=0 凭证冒领）；
        // ② 同技术租户且实际持有 infra:file:query 权限（经 PermissionCommonApi 查询，与 @ss.hasPermission
        //    同一数据源——OAuth scopes 与后台菜单权限是两套体系，不能作为判定依据）
        if (loginUser == null) {
            throw new AccessDeniedException("私有文件禁止匿名读取");
        }
        boolean ownerMatched = file.getOwnerUserId() != null && file.getOwnerUserId() > 0
                && Objects.equals(file.getOwnerUserId(), loginUser.getId())
                && Objects.equals(loginUser.getTenantId(), file.getTenantId());
        if (ownerMatched) {
            return;
        }
        boolean tenantMatched = Objects.equals(loginUser.getTenantId(), file.getTenantId());
        boolean manager = tenantMatched && filePermissionFallback.apply(loginUser.getId(), "infra:file:query");
        if (!manager) {
            throw new AccessDeniedException("私有文件仅所有者或租户管理员可读取");
        }
    }

    /**
     * ZS-FILE-001.A codex r1 P2：管理面权限判定，与 {@code @ss.hasPermission} 一致走 PermissionCommonApi。
     * 以函数字段注入便于单测；系统异常时保守返回 false（宁可拒绝也不放行）。
     */
    private final java.util.function.BiFunction<Long, String, Boolean> filePermissionFallback =
            (userId, permission) -> {
                try {
                    return permissionCommonApi.hasAnyPermissions(userId, permission);
                } catch (Exception ex) {
                    log.warn("[filePermissionFallback][用户({}) 权限查询失败，保守拒绝 permission({})]", userId, permission, ex);
                    return false;
                }
            };

    /**
     * ZS-FILE-001.A：管理员显式调整文件可见范围（历史存量迁移默认 PRIVATE，公开须显式标注）
     */
    @Override
    public void updateFileScope(Long id, String scope) {
        if (!FileScopeEnum.isValid(scope)) {
            throw exception(FILE_SCOPE_INVALID);
        }
        FileDO file = validateFileExists(id);
        // ZS-FILE-002（codex r1 P2）：转 PUBLIC 需类型【精确匹配】公开素材白名单——
        // 前缀匹配 image/ 会放进 image/svg+xml（可携带脚本，存储型 XSS 面）
        if (FileScopeEnum.PUBLIC.getScope().equals(scope)
                && CollUtil.isNotEmpty(fileProperties.getPublicAllowedTypes())
                && fileProperties.getPublicAllowedTypes().stream()
                        .noneMatch(allowed -> file.getType() != null
                                && file.getType().equalsIgnoreCase(allowed))) {
            throw exception(FILE_PUBLIC_TYPE_NOT_ALLOWED, file.getType());
        }
        FileDO updateObj = new FileDO().setId(file.getId()).setScope(scope);
        fileMapper.updateById(updateObj);
    }

    // ZS-FILE-001.A 类尾占位

    /**
     * ZS-FILE-002：扩展名/MIME 伪装校验——探测类型可解析出扩展名、且文件名带扩展名时，
     * 二者必须一致；无法判定（octet-stream/文件名无扩展名）跳过。
     */
    private void validateExtensionConsistency(String name, String detectedType) {
        if (StrUtil.isEmpty(detectedType) || "application/octet-stream".equals(detectedType)) {
            return; // 内容无法判定：不凭扩展名升级为可信类型，直接放行（后续无个人敏感面）
        }
        // codex r0 P2：按探测 MIME 的【合法扩展名集合】归一比较（jpg/jpeg/jfif 同为 image/jpeg，
        // 首选扩展名会误拒 .jpeg/.jfif）；集合为空（注册表无扩展名）跳过
        String nameExt = FileUtil.extName(name);
        if (StrUtil.isEmpty(nameExt)) {
            return;
        }
        // codex r1 P2：ZIP 容器兼容——jar/war/docx/xlsx 等纯内容探测为 application/zip，
        // 其扩展名集合不含真实业务扩展名；属已知容器形态则放行（内容仍是合法 ZIP 容器）
        if ("application/zip".equals(detectedType)
                && ZIP_CONTAINER_EXTENSIONS.contains(nameExt.toLowerCase())) {
            return;
        }
        try {
            List<String> aliases = org.apache.tika.mime.MimeTypes.getDefaultMimeTypes()
                    .forName(detectedType).getExtensions().stream()
                    .map(ext -> StrUtil.removePrefix(ext, ".").toLowerCase())
                    .toList();
            if (CollUtil.isEmpty(aliases)) {
                return;
            }
            if (!aliases.contains(nameExt.toLowerCase())) {
                throw exception(FILE_TYPE_MISMATCH, detectedType, nameExt);
            }
        } catch (org.apache.tika.mime.MimeTypeException ex) {
            // 未知 MIME 注册项：跳过一致性校验
        }
    }

    /**
     * ZS-FILE-002：危险扩展名隔离——按最终扩展名命中黑名单即拒绝上传。
     */
    /**
     * ZS-FILE-003：凭证创建时声明 scope 为 PUBLIC 的类型白名单校验
     */
    private void validatePublicScopeWhitelist(String scope, String contentType) {
        if (FileScopeEnum.PUBLIC.getScope().equals(scope)
                && CollUtil.isNotEmpty(fileProperties.getPublicAllowedTypes())
                && fileProperties.getPublicAllowedTypes().stream()
                .noneMatch(allowed -> contentType != null && contentType.equalsIgnoreCase(allowed))) {
            throw exception(FILE_PUBLIC_TYPE_NOT_ALLOWED, contentType);
        }
    }

    private void validateDangerExtension(String name) {
        String ext = StrUtil.emptyIfNull(FileUtil.extName(name)).toLowerCase();
        if (StrUtil.isNotEmpty(ext) && fileProperties.getDangerExtensions().stream()
                .anyMatch(d -> d.equalsIgnoreCase(ext))) {
            throw exception(FILE_DANGEROUS_CONTENT, ext);
        }
    }

    // ========== ZS-FILE-003：预签名直传凭证与完成确认 ==========

    private static final String TEMP_PATH_PREFIX = "temp/";
    private static final long CREDENTIAL_EXPIRE_MINUTES = 30;

    @Override
    @SuppressWarnings("RedundantThrows")
    public cn.zszj.module.infra.controller.admin.file.vo.file.FileUploadCredentialCreateRespVO createUploadCredential(
            cn.zszj.module.infra.controller.admin.file.vo.file.FileUploadCredentialCreateReqVO reqVO) {
        // 1. 复用 FILE-002 合同：大小限额 / 危险扩展名 / PUBLIC 白名单（按声明）
        if (reqVO.getSize() > fileProperties.getMaxSize()) {
            throw exception(FILE_SIZE_EXCEED, reqVO.getSize(), fileProperties.getMaxSize());
        }
        String name = FilePathUtils.validateFileName(reqVO.getName());
        validateDangerExtension(name);
        validatePublicScopeWhitelist(reqVO.getScope(), reqVO.getContentType());

        // 2. master 客户端必须支持 presign——local 等禁用直传，走服务端受控上传（不降低校验）
        FileClient client = fileConfigService.getMasterFileClient();
        if (client == null) {
            throw exception(FILE_PRESIGN_NOT_SUPPORTED);
        }
        // 3. 服务端生成临时对象键（temp/ 前缀 + UUID，主体只能写临时区，键不经过客户端）
        String tempPath = TEMP_PATH_PREFIX + IdUtil.fastSimpleUUID() + "/" + name;
        String uploadUrl;
        try {
            // codex r0 P2：签名有效期与凭证有效期一致（凭证过期即 PUT 失效）
            uploadUrl = client.presignPutUrl(tempPath, (int) (CREDENTIAL_EXPIRE_MINUTES * 60));
        } catch (UnsupportedOperationException ex) {
            throw exception(FILE_PRESIGN_NOT_SUPPORTED);
        }

        // 4. 落凭证记录：绑定主体/租户/临时键/大小/类型/有效期
        FileUploadCredentialDO credential = new FileUploadCredentialDO()
                .setCredentialToken(IdUtil.fastSimpleUUID())
                .setConfigId(client.getId())
                .setOwnerUserId(currentUserOrZero())
                .setPurpose(reqVO.getPurpose())
                .setTempPath(tempPath)
                .setFileName(name)
                .setContentType(reqVO.getContentType())
                .setDeclaredSize(reqVO.getSize())
                .setScope(StrUtil.blankToDefault(reqVO.getScope(), FileScopeEnum.PRIVATE.getScope()))
                .setStatus(FileUploadCredentialDO.STATUS_WAITING_UPLOAD)
                .setExpiresTime(LocalDateTime.now().plusMinutes(CREDENTIAL_EXPIRE_MINUTES));
        fileUploadCredentialMapper.insert(credential);

        cn.zszj.module.infra.controller.admin.file.vo.file.FileUploadCredentialCreateRespVO respVO =
                new cn.zszj.module.infra.controller.admin.file.vo.file.FileUploadCredentialCreateRespVO()
                        .setCredentialToken(credential.getCredentialToken())
                        .setUploadUrl(uploadUrl)
                        .setTempPath(tempPath)
                        .setExpiresTime(credential.getExpiresTime());
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long completeUpload(cn.zszj.module.infra.controller.admin.file.vo.file.FileUploadCredentialCompleteReqVO reqVO) throws Exception {
        // codex r0 P1：完成确认同样吃在途上传预算（读取大对象的堆占用也在预算窗口内）
        boolean acquired = uploadPermits().tryAcquire();
        if (!acquired) {
            throw exception(FILE_UPLOAD_CONCURRENT_LIMIT);
        }
        try {
            // 1. 读凭证（不信任客户端 configId/path/url——一切以凭证记录为准）
            FileUploadCredentialDO credential = fileUploadCredentialMapper.selectByToken(reqVO.getCredentialToken());
            if (credential == null) {
                throw exception(FILE_UPLOAD_CREDENTIAL_NOT_EXISTS);
            }
            // 2. 归属校验：凭证绑定 owner>0 时必须与当前登录主体匹配（匿名/他人凭证均拒绝）
            Long loginUserId = currentUserOrZero();
            if (credential.getOwnerUserId() != null && credential.getOwnerUserId() > 0
                    && !Objects.equals(credential.getOwnerUserId(), loginUserId)) {
                throw exception(FILE_UPLOAD_CREDENTIAL_FORBIDDEN);
            }
            // 3. 状态预检：已完成凭证快速失败（并发下仍由步骤 9 的 CAS 兜底）
            if (FileUploadCredentialDO.STATUS_COMPLETED.equals(credential.getStatus())) {
                throw exception(FILE_UPLOAD_CREDENTIAL_ALREADY_USED);
            }
            // 4. 过期校验
            if (LocalDateTime.now().isAfter(credential.getExpiresTime())) {
                throw exception(FILE_UPLOAD_CREDENTIAL_EXPIRED);
            }

            // 5. 读取临时对象内容（快照进内存）——空上传/未上传拒绝
            FileClient client = fileConfigService.getFileClient(credential.getConfigId());
            if (client == null) {
                throw exception(FILE_PRESIGN_NOT_SUPPORTED);
            }
            byte[] content = client.getContent(credential.getTempPath());
            if (content == null || content.length == 0) {
                throw exception(FILE_UPLOAD_TEMP_EMPTY);
            }
            // 5.1 实际大小与凭证声明一致性校验 + 平台上限复验
            if (content.length != credential.getDeclaredSize()) {
                throw exception(FILE_UPLOAD_TEMP_SIZE_MISMATCH, content.length, credential.getDeclaredSize());
            }
            if (content.length > fileProperties.getMaxSize()) {
                throw exception(FILE_SIZE_EXCEED, content.length, fileProperties.getMaxSize());
            }

            // 6. FILE-002 硬化复验：按实际内容探测类型；补全扩展名后做危险/一致性校验（codex r0 P2）
            String detectedType = FileTypeUtils.getMineType(content);
            String finalName = credential.getFileName();
            if (StrUtil.isNotEmpty(detectedType) && StrUtil.isEmpty(FileUtil.extName(finalName))) {
                String extension = FileTypeUtils.getExtension(detectedType);
                if (StrUtil.isNotEmpty(extension)) {
                    finalName = finalName + extension;
                }
            }
            validateDangerExtension(finalName);
            validateExtensionConsistency(finalName, detectedType);
            // codex r0 P1：PUBLIC 发布前按【实际探测类型】复验白名单（防声明 png 实传 zip 绕过）
            validatePublicScopeWhitelist(credential.getScope(), detectedType);

            // 7. 发布为正式对象：正式键服务端生成（与临时键物理隔离），写入即锁定快照——
            //    PUT URL 之后重用只会改写临时键，不影响已发布的正式对象
            String path = generateUploadPath(finalName, null);
            String url = client.upload(content, path, detectedType);
            String contentHash = DigestUtil.sha256Hex(content);

            // 8. 核验正式对象散列与快照一致
            byte[] published = client.getContent(path);
            if (published == null
                    || !Objects.equals(contentHash, DigestUtil.sha256Hex(published))) {
                // codex r0 P2：失败路径记录正式键便于人工清理（可靠补偿归 FILE-005.A）
                log.warn("[completeUpload][ZS-FILE-003 散列核验失败，正式键({}) 需人工清理]", path);
                throw exception(FILE_UPLOAD_TEMP_HASH_MISMATCH);
            }

            // 9. 落正式文件记录（同事务：CAS 失败回滚本插入，不生成重复资产）
            FileDO file = new FileDO().setConfigId(credential.getConfigId())
                    .setName(finalName).setPath(path).setUrl(url)
                    .setType(detectedType).setSize((long) content.length)
                    .setFileHash(contentHash)
                    .setOwnerUserId(credential.getOwnerUserId())
                    .setScope(credential.getScope());
            file.setTenantId(TenantContextHolder.getTenantId());
            fileMapper.insert(file);

            // 10. 凭证一次性 CAS 迁移（同事务：affected=0 抛异常 → 插入随事务回滚），清理临时对象
            int updated = fileUploadCredentialMapper.updateStatusToCompletedIfWaiting(credential.getId(), file.getId());
            if (updated == 0) {
                throw exception(FILE_UPLOAD_CREDENTIAL_ALREADY_USED);
            }
            try {
                client.delete(credential.getTempPath());
            } catch (Exception cleanupEx) {
                log.warn("[completeUpload][清理临时对象({}) 失败，不阻塞返回]", credential.getTempPath(), cleanupEx);
            }
            return file.getId();
        } finally {
            uploadPermits.release();
        }
    }

    // ZS-FILE-002 类尾占位
}