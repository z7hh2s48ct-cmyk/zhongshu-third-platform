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
     * 一律不下钻；物理解析失败（断链/环/权限）同样按别名跳过——少清点永远安全（fail-safe 少删）。</p>
     *
     * <p>codex r3 P1：符号链接目录【显式】拒下钻（{@code Files.isSymbolicLink}，先于别名比较、
     * 不依赖大小写比较）——大小写敏感文件系统上 {@code Temp -> temp} 形态链接的物理解析与
     * 词汇路径仅大小写不同，忽略大小写的别名比较会误放行，{@code Temp/live.bin} 以 {@code Temp/}
     * 前缀洗进清单绕过服务层只认 {@code temp/} 的在途凭证保护；符号链接文件同理不清洗进清单
     * （删除会穿透到链接目标）。</p>
     *
     * <p>codex r3 P2：被跳过（符号链接/别名/解析失败）目录的前缀与被跳过文件路径经
     * {@link #listObjectsDetailed} 上报——「未出现在清单」与「已不存在」是两种语义，
     * 清理侧对命中被跳过前缀的 path 必须按不可验证拒绝，不得假报成功。</p>
     */
    @Override
    public java.util.List<cn.zszj.module.infra.framework.file.core.client.FileObjectEntry> listObjects(
            String prefix, int maxEntries) {
        return listObjectsDetailed(prefix, maxEntries).getEntries();
    }

    /**
     * ZS-FILE-005.B codex r3 P2：对象清点（带不可验证上报）——条目语义与 {@link #listObjects}
     * 完全一致，额外返回枚举期被跳过（符号链接/别名/解析失败）目录的相对前缀（正斜杠、
     * 以 {@code /} 结尾）与被跳过文件的相对路径。
     */
    @Override
    public cn.zszj.module.infra.framework.file.core.client.FileListing listObjectsDetailed(
            String prefix, int maxEntries) {
        Path basePath = Paths.get(config.getBasePath()).toAbsolutePath().normalize();
        if (!java.nio.file.Files.exists(basePath)) {
            return new cn.zszj.module.infra.framework.file.core.client.FileListing(
                    java.util.List.of(), java.util.List.of(), java.util.List.of());
        }
        Path baseReal;
        try {
            baseReal = basePath.toRealPath();
        } catch (java.io.IOException ex) {
            // 根不可解析（断链/环/权限）：无法安全清点 → 空清单（服务层隔离检查对该配置已 036 拒绝）
            return new cn.zszj.module.infra.framework.file.core.client.FileListing(
                    java.util.List.of(), java.util.List.of(), java.util.List.of());
        }
        java.util.ArrayDeque<Path> pendingDirs = new java.util.ArrayDeque<>();
        pendingDirs.push(baseReal);
        java.util.List<Path> files = new java.util.ArrayList<>();
        // codex r3 P2：被跳过（不可验证）目录前缀（以 / 结尾）与文件路径——其下/其身对象可能
        // 真实存在却不出现在条目中，供清理侧区分「确认不存在」与「不可验证」
        java.util.List<String> skippedPrefixes = new java.util.ArrayList<>();
        java.util.List<String> skippedPaths = new java.util.ArrayList<>();
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
                    // codex r3 P1：符号链接显式判先（不依赖别名比较的大小写容差）——目录不下钻、
                    // 文件不清洗进清单，其路径登记为不可验证（删除会穿透到链接目标）
                    if (java.nio.file.Files.isSymbolicLink(child)) {
                        recordSkipped(baseReal, child, skippedPrefixes, skippedPaths);
                        continue;
                    }
                    if (java.nio.file.Files.isDirectory(child)) {
                        if (isAliasDirectory(child)) {
                            // 别名目录不下钻（无法安全判定其物理归属 → 保守少清点），
                            // codex r3 P2：其前缀登记为不可验证（清理侧不得假报成功）
                            recordSkipped(baseReal, child, skippedPrefixes, skippedPaths);
                            continue;
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
        return new cn.zszj.module.infra.framework.file.core.client.FileListing(entries, skippedPrefixes, skippedPaths);
    }

    /**
     * 登记被跳过条目的相对路径（codex r3 P2）：目录记为前缀（以 {@code /} 结尾，覆盖其下整棵
     * 子树），文件记为精确路径。
     */
    private static void recordSkipped(Path baseReal, Path child,
                                      java.util.List<String> skippedPrefixes,
                                      java.util.List<String> skippedPaths) {
        String rel = baseReal.relativize(child).toString().replace(java.io.File.separatorChar, '/');
        if (java.nio.file.Files.isDirectory(child)) {
            skippedPrefixes.add(rel + "/");
        } else {
            skippedPaths.add(rel);
        }
    }

    /**
     * 别名目录判定（codex r2）：目录的「自身物理解析（toRealPath）」与「自身词汇路径」不一致即为
     * junction/symlink 别名（真实子目录两者相同——父目录已在真实空间）；物理解析失败
     * （断链/环/权限）同样按别名处理（不下钻）。大小写不敏感比较规避盘符/目录拼写差异的误报
     * （误报方向是少清点，保守可接受）。
     *
     * <p>codex r3 P1：符号链接目录已在遍历处显式拒下钻（{@code Files.isSymbolicLink} 判先）——
     * 本比较只兜底 junction 等非符号链接别名；大小写敏感文件系统上 {@code Temp -> temp} 形态
     * 链接「物理解析 vs 词汇路径」仅大小写不同，依赖本比较会误放行。</p>
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
