package cn.zszj.module.infra.service.file;

import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileCreateReqVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileDeleteBatchRespVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FilePageReqVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FilePresignedUrlRespVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileUploadCredentialCompleteReqVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileUploadCredentialCreateReqVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileUploadCredentialCreateRespVO;
import cn.zszj.module.infra.dal.dataobject.file.FileDO;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 文件 Service 接口
 *
 * @author 芋道源码
 */
public interface FileService {

    /**
     * 获得文件分页
     *
     * @param pageReqVO 分页查询
     * @return 文件分页
     */
    PageResult<FileDO> getFilePage(FilePageReqVO pageReqVO);

    /**
     * 保存文件，并返回文件的访问路径
     *
     * @param content   文件内容
     * @param name      文件名称，允许空
     * @param directory 目录，允许空
     * @param type      文件的 MIME 类型，允许空
     * @return 文件路径
     */
    String createFile(@NotEmpty(message = "文件内容不能为空") byte[] content,
                      String name, String directory, String type);

    /**
     * 生成文件预签名地址信息，用于上传
     *
     * @param name      文件名
     * @param directory 目录
     * @return 预签名地址信息
     */
    FilePresignedUrlRespVO presignPutUrl(@NotEmpty(message = "文件名不能为空") String name,
                                         String directory);
    /**
     * 生成文件预签名地址信息，用于读取
     *
     * @param url 完整的文件访问地址
     * @param expirationSeconds 访问有效期，单位秒
     * @return 文件预签名地址
     */
    String presignGetUrl(String url, Integer expirationSeconds);

    /**
     * 创建文件
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createFile(FileCreateReqVO createReqVO);
    FileDO getFile(Long id);

    /**
     * 删除文件（ZS-FILE-005.A：引用保护 + DELETING 可恢复中间态）
     *
     * @param id 编号
     */
    void deleteFile(Long id) throws Exception;

    /**
     * 批量删除文件（ZS-FILE-005.A：逐项执行并逐项记录结果——中段失败不伪报全成功）
     *
     * @param ids 编号列表
     * @return 逐项删除结果（成功列表 + 失败明细）
     */
    FileDeleteBatchRespVO deleteFileList(List<Long> ids) throws Exception;

    /**
     * 人工对账：列出删除中（DELETING）的可恢复记录（ZS-FILE-005.A）
     *
     * @return 处于删除中间态的文件列表
     */
    List<FileDO> getDeletingFileList();

    /**
     * 人工对账：对删除中间态的记录重试清理（对象已不存在则仅移除记录）（ZS-FILE-005.A）
     *
     * @param id 编号
     */
    void reconcileCleanupFile(Long id) throws Exception;

    /**
     * 获得文件内容
     *
     * @param configId 配置编号
     * @param path     文件路径
     * @return 文件内容
     */
    byte[] getFileContent(Long configId, String path) throws Exception;

    /**
     * 获得文件
     *
     * @param configId 配置编号
     * @param path     文件路径
     * @return 文件
     */
    FileDO getFileByConfigIdAndPath(Long configId, String path);


    /**
     * ZS-FILE-001.A：统一读取授权——PUBLIC 匿名可读；PRIVATE 需登录且同技术租户
     */
    void validateFileReadable(FileDO file, cn.zszj.framework.security.core.LoginUser loginUser);

    /**
     * ZS-FILE-001.A：管理员显式调整文件可见范围（PUBLIC/PRIVATE）
     */
    void updateFileScope(Long id, String scope);

    /**
     * ZS-FILE-001.A（codex r0 P2）：下载场景跨租户定位文件记录（PUBLIC 对任意来源同址可用）
     */
    FileDO getFileByConfigIdAndPathIgnoreTenant(Long configId, String path);

    /**
     * ZS-FILE-003：创建预签名直传凭证——绑定主体/租户/临时键/大小/类型/有效期；
     * 平台凭据只允许写临时区。local 等不支持 presign 的存储抛 FILE_PRESIGN_NOT_SUPPORTED。
     */
    FileUploadCredentialCreateRespVO createUploadCredential(FileUploadCredentialCreateReqVO reqVO);

    /**
     * ZS-FILE-003：完成确认——一次性原子迁移凭证状态，服务端读取临时对象校验大小/散列后
     * 发布为正式资产并核验最终散列；不信任客户端 URL/configId/校验声明。
     */
    Long completeUpload(FileUploadCredentialCompleteReqVO reqVO) throws Exception;
}