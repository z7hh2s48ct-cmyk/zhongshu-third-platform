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
     * ZS-FILE-005.B：对象清点——目录遍历返回相对路径（正斜杠）、受 maxEntries 有界、结果按 path
     * 排序返回，lastModified 取文件修改时间（孤儿保留期判定锚点）。
     *
     * <p>codex r2 第三形状自查：Windows 上 junction 属 reparse 目录而 NIO 不视其为符号链接，
     * 目录遍历默认即下钻——树内游离 junction 会把【他配置物理目录】的对象以本配置相对路径
     * 洗进清点清单（引用查不到 → 误删）。故清点在【真实路径空间】进行（根先 toRealPath，
     * 其下子目录逐个比对「自身物理解析 vs 自身词汇路径」）：不一致即为别名（junction/symlink），
     * 一律不下钻；物理解析失败（断链/环/权限）同样按别名跳过——少清点永远安全（fail-safe 少删）。
     * 遍历序为「父目录字典序 DFS」，截断取清点序前 N 个（确定性依赖树形态，分前缀续扫合同不变）。</p>
     */
    @Override
    public java.util.List<cn.zszj.module.infra.framework.file.core.client.FileObjectEntry> listObjects(
            String prefix, int maxEntries) {
        Path basePath = Paths.get(config.getBasePath()).toAbsolutePath().normalize();
        if (!java.nio.file.Files.exists(basePath)) {
            return java.util.List.of();
        }
        Path baseReal;
        try {
            baseReal = basePath.toRealPath();
        } catch (java.io.IOException ex) {
            // 根不可解析（断链/环/权限）：无法安全清点 → 空清单（服务层隔离检查对该配置已 036 拒绝）
            return java.util.List.of();
        }
        java.util.ArrayDeque<Path> pendingDirs = new java.util.ArrayDeque<>();
        pendingDirs.push(baseReal);
        java.util.List<Path> files = new java.util.ArrayList<>();
        try {
            while (!pendingDirs.isEmpty() && files.size() < maxEntries) {
                Path dir = pendingDirs.pop();
                java.util.List<Path> children = new java.util.ArrayList<>();
                try (java.nio.file.DirectoryStream<Path> stream = java.nio.file.Files.newDirectoryStream(dir)) {
                    for (Path child : stream) {
                        children.add(child);
                    }
                }
                children.sort(java.util.Comparator.comparing(p -> p.getFileName().toString()));
                for (Path child : children) {
                    if (java.nio.file.Files.isDirectory(child)) {
                        if (isAliasDirectory(child)) {
                            continue; // 别名目录不下钻（无法安全判定其物理归属 → 保守少清点）
                        }
                        pendingDirs.push(child);
                    } else if (java.nio.file.Files.isRegularFile(child)) {
                        // 前缀过滤在收集期生效：maxEntries 界定的是【匹配前缀的条目数】，
                        // 前缀外文件不挤占配额（P2-2 续扫合同：分前缀清点可推进）
                        String rel = baseReal.relativize(child).toString().replace(java.io.File.separatorChar, '/');
                        if (!rel.startsWith(prefix)) {
                            continue;
                        }
                        files.add(child);
                        if (files.size() >= maxEntries) {
                            break;
                        }
                    }
                }
            }
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("对象清点失败: " + ex.getMessage(), ex);
        }
        java.util.List<cn.zszj.module.infra.framework.file.core.client.FileObjectEntry> entries =
                new java.util.ArrayList<>(files.size());
        for (Path file : files) {
            String rel = baseReal.relativize(file).toString().replace(java.io.File.separatorChar, '/');
            try {
                java.nio.file.attribute.BasicFileAttributes attrs = java.nio.file.Files.readAttributes(
                        file, java.nio.file.attribute.BasicFileAttributes.class);
                entries.add(new cn.zszj.module.infra.framework.file.core.client.FileObjectEntry(
                        rel, attrs.size(),
                        java.time.LocalDateTime.ofInstant(attrs.lastModifiedTime().toInstant(),
                                java.time.ZoneId.systemDefault())));
            } catch (java.io.IOException ex) {
                // 条目属性读取失败保守降级：lastModified 置空（服务层对 null 保守跳过，不清理）
                entries.add(new cn.zszj.module.infra.framework.file.core.client.FileObjectEntry(rel, null, null));
            }
        }
        entries.sort(java.util.Comparator.comparing(
                cn.zszj.module.infra.framework.file.core.client.FileObjectEntry::getPath));
        return entries;
    }

    /**
     * 别名目录判定（codex r2）：目录的「自身物理解析（toRealPath）」与「自身词汇路径」不一致即为
     * junction/symlink 别名（真实子目录两者相同——父目录已在真实空间）；物理解析失败
     * （断链/环/权限）同样按别名处理（不下钻）。大小写不敏感比较规避盘符/目录拼写差异的误报
     * （误报方向是少清点，保守可接受）。
     */
    private static boolean isAliasDirectory(Path dir) {
        try {
            return !dir.toRealPath().toString().replace('\\', '/')
                    .equalsIgnoreCase(dir.toString().replace('\\', '/'));
        } catch (java.io.IOException ex) {
            return true;
        }
    }

}
