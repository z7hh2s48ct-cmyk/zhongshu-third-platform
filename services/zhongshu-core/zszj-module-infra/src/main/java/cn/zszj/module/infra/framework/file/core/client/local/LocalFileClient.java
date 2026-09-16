package cn.zszj.module.infra.framework.file.core.client.local;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.zszj.module.infra.framework.file.core.client.AbstractFileClient;
import cn.zszj.module.infra.framework.file.core.utils.FilePathUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_PATH_INVALID;

/**
 * 本地文件客户端
 *
 * @author 芋道源码
 */
public class LocalFileClient extends AbstractFileClient<LocalFileClientConfig> {

    public LocalFileClient(Long id, LocalFileClientConfig config) {
        super(id, config);
    }

    @Override
    protected void doInit() {
    }

    @Override
    public String upload(byte[] content, String path, String type) {
        // 执行写入
        String filePath = getFilePath(path);
        FileUtil.writeBytes(content, filePath);
        // 拼接返回路径
        return super.formatFileUrl(config.getDomain(), path);
    }

    @Override
    public void delete(String path) {
        String filePath = getFilePath(path);
        FileUtil.del(filePath);
    }

    @Override
    public byte[] getContent(String path) {
        String filePath = getFilePath(path);
        try {
            return FileUtil.readBytes(filePath);
        } catch (IORuntimeException ex) {
            if (ex.getMessage().startsWith("File not exist:")) {
                return null;
            }
            throw ex;
        }
    }

    /**
     * ZS-FILE-004.A（codex r1 P2）：本地文件真范围读取——RandomAccessFile 定位读取，
     * 存储流量与内存随分块规模伸缩，不再整文件读取。
     */
    @Override
    public byte[] getContentRange(String path, long start, int length) throws Exception {
        String filePath = getFilePath(path);
        java.io.File file = new java.io.File(filePath);
        if (!file.exists()) {
            return null;
        }
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "r")) {
            long seek = Math.min(start, raf.length());
            int readLength = (int) Math.min(length, raf.length() - seek);
            byte[] buffer = new byte[Math.max(readLength, 0)];
            raf.seek(seek);
            raf.readFully(buffer);
            return buffer;
        }
    }

    private String getFilePath(String path) {
        FilePathUtils.validatePath(path);
        Path basePath = Paths.get(config.getBasePath()).toAbsolutePath().normalize();
        Path filePath = basePath.resolve(path).normalize();
        if (!filePath.startsWith(basePath)) {
            throw exception(FILE_PATH_INVALID);
        }
        return filePath.toString();
    }

    /**
     * ZS-FILE-005.B：对象清点——目录遍历返回相对路径（正斜杠）、按 path 稳定排序、受 maxEntries 有界，
     * lastModified 取文件修改时间（孤儿保留期判定锚点）。仅清点常规文件，不解析符号链接。
     */
    @Override
    public java.util.List<cn.zszj.module.infra.framework.file.core.client.FileObjectEntry> listObjects(
            String prefix, int maxEntries) {
        Path basePath = Paths.get(config.getBasePath()).toAbsolutePath().normalize();
        if (!java.nio.file.Files.exists(basePath)) {
            return java.util.List.of();
        }
        try (java.util.stream.Stream<Path> stream = java.nio.file.Files.walk(basePath)) {
            return stream.filter(java.nio.file.Files::isRegularFile)
                    .map(basePath::relativize)
                    .map(p -> p.toString().replace(java.io.File.separatorChar, '/'))
                    .filter(p -> p.startsWith(prefix))
                    .sorted()
                    .limit(maxEntries)
                    .map(p -> {
                        try {
                            java.nio.file.attribute.BasicFileAttributes attrs = java.nio.file.Files.readAttributes(
                                    basePath.resolve(p), java.nio.file.attribute.BasicFileAttributes.class);
                            return new cn.zszj.module.infra.framework.file.core.client.FileObjectEntry(
                                    p, attrs.size(),
                                    java.time.LocalDateTime.ofInstant(attrs.lastModifiedTime().toInstant(),
                                            java.time.ZoneId.systemDefault()));
                        } catch (java.io.IOException ex) {
                            // 条目属性读取失败保守降级：lastModified 置空（服务层对 null 保守跳过，不清理）
                            return new cn.zszj.module.infra.framework.file.core.client.FileObjectEntry(p, null, null);
                        }
                    })
                    .toList();
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("对象清点失败: " + ex.getMessage(), ex);
        }
    }

}
