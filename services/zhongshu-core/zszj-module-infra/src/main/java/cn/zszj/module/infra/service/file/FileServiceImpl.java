package cn.zszj.module.infra.service.file;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.collection.CollUtil;
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
import cn.zszj.module.infra.controller.admin.file.vo.file.FileCreateReqVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FilePageReqVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FilePresignedUrlRespVO;
import cn.zszj.module.infra.dal.dataobject.file.FileDO;
import cn.zszj.module.infra.dal.mysql.file.FileMapper;
import cn.zszj.module.infra.framework.file.core.client.FileClient;
import cn.zszj.module.infra.framework.file.core.utils.FilePathUtils;
import cn.zszj.module.infra.framework.file.core.utils.FileTypeUtils;
import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

import static cn.hutool.core.date.DatePattern.PURE_DATE_PATTERN;
import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_NOT_EXISTS;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_SCOPE_INVALID;

/**
 * 文件 Service 实现类
 *
 * @author 芋道源码
 */
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
    static boolean PATH_SUFFIX_TIMESTAMP_ENABLE = false;
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
    private FileMapper fileMapper;

    @Override
    public PageResult<FileDO> getFilePage(FilePageReqVO pageReqVO) {
        return fileMapper.selectPage(pageReqVO);
    }

    @Override
    @SneakyThrows
    public String createFile(byte[] content, String name, String directory, String type) {
        // 1.1 处理 name 的合法性，禁止携带目录路径
        name = FilePathUtils.validateFileName(name);

        // 1.2.1 处理 type 为空的情况
        if (StrUtil.isEmpty(type)) {
            type = FileTypeUtils.getMineType(content, name);
        }
        // 1.2.2 处理 name 为空的情况
        if (StrUtil.isEmpty(name)) {
            name = DigestUtil.sha256Hex(content);
        }
        if (StrUtil.isEmpty(FileUtil.extName(name))) {
            // 如果 name 没有后缀 type，则补充后缀
            String extension = FileTypeUtils.getExtension(type);
            if (StrUtil.isNotEmpty(extension)) {
                name = name + extension;
            }
        }

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
            // 5 位随机数，避免同一毫秒内的重复
            suffix = String.valueOf(System.currentTimeMillis()) + RandomUtil.randomInt(10000, 100000);
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
        // 私有附件（codex r0 P1）：必须登录，且满足其一——
        // ① 上传所有者本人；② 同技术租户且持有文件查询权限（管理面，scopes 含通配或显式权限）
        if (loginUser == null) {
            throw new AccessDeniedException("私有文件禁止匿名读取");
        }
        if (Objects.equals(file.getOwnerUserId(), loginUser.getId())) {
            return;
        }
        boolean tenantMatched = Objects.equals(loginUser.getTenantId(), file.getTenantId());
        boolean manager = loginUser.getScopes() != null
                && (loginUser.getScopes().contains("*")
                    || loginUser.getScopes().contains("infra:file:query"));
        if (!tenantMatched || !manager) {
            throw new AccessDeniedException("私有文件仅所有者或租户管理员可读取");
        }
    }

    /**
     * ZS-FILE-001.A：管理员显式调整文件可见范围（历史存量迁移默认 PRIVATE，公开须显式标注）
     */
    @Override
    public void updateFileScope(Long id, String scope) {
        if (!FileScopeEnum.isValid(scope)) {
            throw exception(FILE_SCOPE_INVALID);
        }
        FileDO file = validateFileExists(id);
        FileDO updateObj = new FileDO().setId(file.getId()).setScope(scope);
        fileMapper.updateById(updateObj);
    }

    // ZS-FILE-001.A 类尾占位
}