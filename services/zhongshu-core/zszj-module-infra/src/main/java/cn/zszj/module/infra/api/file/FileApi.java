package cn.zszj.module.infra.api.file;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 文件 API 接口
 *
 * @author 芋道源码
 */
public interface FileApi {

    /**
     * 保存文件，并返回文件的访问路径
     *
     * @param content 文件内容
     * @return 文件路径
     */
    default String createFile(byte[] content) {
        return createFile(content, null, null, null);
    }

    /**
     * 保存文件，并返回文件的访问路径
     *
     * @param content 文件内容
     * @param name 文件名称，允许空
     * @return 文件路径
     */
    default String createFile(byte[] content, String name) {
        return createFile(content, name, null, null);
    }

    /**
     * 保存文件，并返回文件的访问路径
     *
     * @param content 文件内容
     * @param name 文件名称，允许空
     * @param directory 目录，允许空
     * @param type 文件的 MIME 类型，允许空
     * @return 文件路径
     */
    String createFile(@NotEmpty(message = "文件内容不能为空") byte[] content,
                      String name, String directory, String type);

    /**
     * 生成文件预签名地址，用于读取
     *
     * @param url 完整的文件访问地址
     * @param expirationSeconds 访问有效期，单位秒
     * @return 文件预签名地址
     */
    String presignGetUrl(@NotEmpty(message = "URL 不能为空") String url,
                         Integer expirationSeconds);

    /**
     * 校验业务模块即将挂接的私有附件引用（接入合同 §1.10：业务模块只存 fileId，写入前须经此校验）。
     *
     * <p>每个文件须存在于当前租户、{@code scope=PRIVATE}、非删除中、非导出件，且当前登录主体对其具备读取资格；
     * 任一不满足整批拒绝并抛 {@code FILE_REFERENCE_INVALID}（不泄露存在性）。空集合/null 直接通过。
     *
     * @param fileIds 待引用的文件编号
     */
    void validatePrivateFileReferences(List<Long> fileIds);

}
