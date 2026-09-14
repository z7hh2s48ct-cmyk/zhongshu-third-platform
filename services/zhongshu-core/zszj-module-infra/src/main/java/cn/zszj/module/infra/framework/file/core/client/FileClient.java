package cn.zszj.module.infra.framework.file.core.client;

/**
 * 文件客户端
 *
 * @author 芋道源码
 */
public interface FileClient {

    /**
     * 获得客户端编号
     *
     * @return 客户端编号
     */
    Long getId();

    /**
     * 上传文件
     *
     * @param content 文件流
     * @param path    相对路径
     * @return 完整路径，即 HTTP 访问地址
     * @throws Exception 上传文件时，抛出 Exception 异常
     */
    String upload(byte[] content, String path, String type) throws Exception;

    /**
     * 删除文件
     *
     * @param path 相对路径
     * @throws Exception 删除文件时，抛出 Exception 异常
     */
    void delete(String path) throws Exception;

    /**
     * ZS-FILE-003 codex r1 P1：有界读取——内容超过 maxBytes 立即抛出（拒绝完成确认），
     * 保证超限对象不会被发布为正式资产。默认实现读后即检；各客户端可覆写为真流式截断。
     */
    default byte[] getContentBounded(String path, long maxBytes) {
        try {
            byte[] content = getContent(path);
            if (content != null && content.length > maxBytes) {
                throw new IllegalStateException("temp object exceeds limit: " + content.length);
            }
            return content;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("bounded read failed: " + e.getMessage(), e);
        }
    }

    /**
     * 获得文件的内容
     *
     * @param path 相对路径
     * @return 文件的内容
     */
    byte[] getContent(String path) throws Exception;

    // ========== 文件签名，目前仅 S3 支持 ==========

    /**
     * 获得文件预签名地址，用于上传
     *
     * @param path 相对路径
     * @return 文件预签名地址
     */
    default String presignPutUrl(String path) {
        throw new UnsupportedOperationException("不支持的操作");
    }

    /**
     * ZS-FILE-003：带有效期的预签名上传地址——凭证有效期与签名有效期共用同一截止时间。
     * 不支持时回退到无参版本（由调用方禁用直传兜底）。
     */
    default String presignPutUrl(String path, Integer expirationSeconds) {
        return presignPutUrl(path);
    }

    /**
     * 生成文件预签名地址，用于读取
     *
     * @param url 完整的文件访问地址
     * @param expirationSeconds 访问有效期，单位秒
     * @return 文件预签名地址
     */
    default String presignGetUrl(String url, Integer expirationSeconds) {
        throw new UnsupportedOperationException("不支持的操作");
    }

}
