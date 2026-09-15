package cn.zszj.module.infra.controller.admin.file;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.common.util.http.HttpUtils;
import cn.zszj.framework.common.util.object.BeanUtils;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.module.infra.controller.admin.file.vo.file.*;
import cn.zszj.module.infra.dal.dataobject.file.FileDO;
import cn.zszj.module.infra.service.file.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static cn.zszj.framework.common.pojo.CommonResult.success;
import static cn.zszj.module.infra.framework.file.core.utils.FileTypeUtils.writeAttachment;

@Tag(name = "管理后台 - 文件存储")
@RestController
@RequestMapping("/infra/file")
@Validated
@Slf4j
public class FileController {

    @Resource
    private FileService fileService;

    @PostMapping("/upload")
    @Operation(summary = "上传文件", description = "模式一：后端上传文件")
    @Parameter(name = "file", description = "文件附件", required = true,
            schema = @Schema(type = "string", format = "binary"))
    public CommonResult<String> uploadFile(@Valid FileUploadReqVO uploadReqVO) throws Exception {
        MultipartFile file = uploadReqVO.getFile();
        byte[] content = IoUtil.readBytes(file.getInputStream());
        return success(fileService.createFile(content, file.getOriginalFilename(),
                uploadReqVO.getDirectory(), file.getContentType()));
    }

    @GetMapping("/presigned-url")
    @Operation(summary = "获取文件预签名地址（上传）", description = "模式二：前端上传文件：用于前端直接上传七牛、阿里云 OSS 等文件存储器")
    @Parameters({
            @Parameter(name = "name", description = "文件名称", required = true),
            @Parameter(name = "directory", description = "文件目录")
    })
    public CommonResult<FilePresignedUrlRespVO> getFilePresignedUrl(
            @RequestParam("name") String name,
            @RequestParam(value = "directory", required = false) String directory) {
        return success(fileService.presignPutUrl(name, directory));
    }

    @GetMapping("/get")
    @Operation(summary = "获得文件")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('infra:file:query')")
    public CommonResult<FileRespVO> getFile(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(fileService.getFile(id), FileRespVO.class));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除文件")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('infra:file:delete')")
    public CommonResult<Boolean> deleteFile(@RequestParam("id") Long id) throws Exception {
        fileService.deleteFile(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除文件", description = "ZS-FILE-005.A：逐项执行并逐项记录结果，中段失败不伪报全成功")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @PreAuthorize("@ss.hasPermission('infra:file:delete')")
    public CommonResult<FileDeleteBatchRespVO> deleteFileList(@RequestParam("ids") List<Long> ids) throws Exception {
        return success(fileService.deleteFileList(ids));
    }

    @GetMapping("/reconcile/deleting")
    @Operation(summary = "人工对账：列出删除中的可恢复记录", description = "ZS-FILE-005.A：对象删除未完成的 DELETING 中间态")
    @PreAuthorize("@ss.hasPermission('infra:file:query')")
    public CommonResult<List<FileRespVO>> getDeletingFileList() {
        return success(BeanUtils.toBean(fileService.getDeletingFileList(), FileRespVO.class));
    }

    @PostMapping("/reconcile/cleanup")
    @Operation(summary = "人工对账：重试清理删除中间态记录", description = "ZS-FILE-005.A：对象仍在则重试删除，已不存在则仅移除记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('infra:file:delete')")
    public CommonResult<Boolean> reconcileCleanupFile(@RequestParam("id") Long id) throws Exception {
        fileService.reconcileCleanupFile(id);
        return success(true);
    }

    @GetMapping("/{configId}/get/**")
    @PermitAll // ZS-FILE-001.A：匿名仅可读 PUBLIC 公开素材；PRIVATE 在 service 校验登录+同租户
    @Operation(summary = "下载文件")
    @Parameter(name = "configId", description = "配置编号", required = true)
    public void getFileContent(HttpServletRequest request,
                               HttpServletResponse response,
                               @PathVariable("configId") Long configId) throws Exception {
        // 获取请求的路径
        String path = StrUtil.subAfter(request.getRequestURI(), "/get/", false);
        if (StrUtil.isEmpty(path)) {
            throw new IllegalArgumentException("结尾的 path 路径必须传递");
        }
        // 解码，解决中文、%、+ 等特殊字符路径的问题
        // https://gitee.com/zhijiantianya/ruoyi-vue-pro/pulls/807/
        // https://gitee.com/zhijiantianya/ruoyi-vue-pro/pulls/1432/
        path = HttpUtils.decodeUrlPath(path);

        // ZS-FILE-001.A（codex r0 P2）：跨租户定位记录——PUBLIC 对任意来源同址可用；
        // PRIVATE 的租户归属校验以记录自身 tenant_id 执行（忽略请求携带租户，防租户过滤 404 误伤公开素材）
        FileDO file = fileService.getFileByConfigIdAndPathIgnoreTenant(configId, path);
        if (file == null) {
            log.warn("[getFileContent][configId({}) path({}) 文件不存在]", configId, path);
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }
        fileService.validateFileReadable(file, SecurityFrameworkUtils.getLoginUser());

        // 读取内容
        byte[] content = fileService.getFileContent(configId, path);
        if (content == null) {
            log.warn("[getFileContent][configId({}) path({}) 文件不存在]", configId, path);
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }
        String filename = StrUtil.isNotEmpty(file.getName()) ? file.getName() : FileUtil.getName(path);
        writeAttachment(response, filename, content);
    }

    @PostMapping("/upload-credential")
    @Operation(summary = "创建预签名直传凭证", description = "ZS-FILE-003：凭证绑定主体/临时键/有效期，凭据仅可写临时区")
    @PreAuthorize("@ss.hasPermission('infra:file:create')")
    public CommonResult<cn.zszj.module.infra.controller.admin.file.vo.file.FileUploadCredentialCreateRespVO> createUploadCredential(
            @Valid @RequestBody cn.zszj.module.infra.controller.admin.file.vo.file.FileUploadCredentialCreateReqVO reqVO) {
        return success(fileService.createUploadCredential(reqVO));
    }

    @PostMapping("/upload-complete")
    @Operation(summary = "直传完成确认", description = "ZS-FILE-003：服务端核验临时对象后发布正式资产（一次性确认）")
    @PreAuthorize("@ss.hasPermission('infra:file:create')")
    public CommonResult<Long> completeUpload(
            @Valid @RequestBody cn.zszj.module.infra.controller.admin.file.vo.file.FileUploadCredentialCompleteReqVO reqVO) throws Exception {
        return success(fileService.completeUpload(reqVO));
    }

    @PutMapping("/update-scope")
    @Operation(summary = "调整文件可见范围", description = "ZS-FILE-001.A：PUBLIC=公开素材（匿名可读）；PRIVATE=私有附件（默认）")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('infra:file:update')")
    public CommonResult<Boolean> updateFileScope(@RequestParam("id") Long id,
                                                 @RequestParam("scope") String scope) {
        fileService.updateFileScope(id, scope);
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获得文件分页")
    @PreAuthorize("@ss.hasPermission('infra:file:query')")
    public CommonResult<PageResult<FileRespVO>> getFilePage(@Valid FilePageReqVO pageVO) {
        PageResult<FileDO> pageResult = fileService.getFilePage(pageVO);
        return success(BeanUtils.toBean(pageResult, FileRespVO.class));
    }

}
