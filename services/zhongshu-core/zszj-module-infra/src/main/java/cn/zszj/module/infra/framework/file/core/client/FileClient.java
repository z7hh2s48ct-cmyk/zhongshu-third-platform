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

    /**
     * 范围读取（ZS-FILE-004.A）：返回 [start, start+length-1] 区间内容，越界收敛到实际内容末尾。
     *
     * <p>使交付分块的存储流量与内存随分块规模伸缩（codex r1 P2）。默认实现整读后裁切
     * （ftp/sftp 等未实装客户端回退）；local/db/s3 已按客户端能力实装真范围读取。</p>
     *
     * @param path   相对路径
     * @param start  起始字节（含）
     * @param length 读取长度
     * @return 范围内容；对象不存在返回 {@code null}
     * @throws Exception 读取文件时，抛出 Exception 异常
     */
    default byte[] getContentRange(String path, long start, int length) throws Exception {
        byte[] content = getContent(path);
        if (content == null) {
            return null;
        }
        int from = (int) Math.min(start, content.length);
        int to = (int) Math.min(start + length, content.length);
        return java.util.Arrays.copyOfRange(content, from, to);
    }

    /**
     * 对象清点（ZS-FILE-005.B 孤儿对象「预览」）：返回 path 以 prefix 开头的对象条目，
     * 按 path 稳定排序、受 maxEntries 有界。
     *
     * <p>默认抛 {@link UnsupportedOperationException}——当前仅 local/db 实装；
     * s3/ftp/sftp 待真实对象存储接入并实测后回填（未经实测的清点实现若前缀/边界有缺陷，
     * 会直接放大为孤儿误删，保守不支持优于假支持）。</p>
     *
     * @param prefix     前缀过滤（空串=全部）
     * @param maxEntries 返回条目上限
     * @return 清点条目列表（size/lastModified 允许为 null，服务层对 null 保守跳过）
     * @throws UnsupportedOperationException 当前存储不支持清点
     */
    default java.util.List<FileObjectEntry> listObjects(String prefix, int maxEntries) {
        throw new UnsupportedOperationException("当前存储不支持对象清点");
    }

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
