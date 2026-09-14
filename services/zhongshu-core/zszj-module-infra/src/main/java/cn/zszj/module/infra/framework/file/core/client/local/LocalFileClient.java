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

}
