package cn.zszj.module.infra.framework.file.core.client.local;

import cn.zszj.module.infra.framework.file.core.client.FileObjectEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * ZS-FILE-005.B：LocalFileClient 对象清点（孤儿对象「预览」的存储侧能力）。
 *
 * <p>合同：返回相对路径（正斜杠）、按 path 稳定排序、受 maxEntries 有界、前缀过滤；lastModified 供保留期判定。</p>
 */
public class LocalFileClientListTest {

    @TempDir
    Path tempDir;

    private LocalFileClient newClient() {
        return newClient(tempDir);
    }

    private LocalFileClient newClient(Path basePath) {
        LocalFileClientConfig config = new LocalFileClientConfig();
        config.setBasePath(basePath.toString());
        config.setDomain("http://localhost:48080");
        LocalFileClient client = new LocalFileClient(1L, config);
        client.init();
        return client;
    }

    @Test
    public void listObjects_returnsRelativePathsSortedAndBounded() throws Exception {
        LocalFileClient client = newClient();
        client.upload("a".getBytes(), "2026/day1/a.txt", "text/plain");
        client.upload("b".getBytes(), "2026/day1/sub/b.txt", "text/plain");
        client.upload("c".getBytes(), "asset/c.bin", "application/octet-stream");

        List<FileObjectEntry> all = client.listObjects("", 100);

        assertEquals(3, all.size());
        assertEquals("2026/day1/a.txt", all.get(0).getPath(), "相对路径 + 稳定排序");
        assertEquals("2026/day1/sub/b.txt", all.get(1).getPath());
        assertEquals("asset/c.bin", all.get(2).getPath());
        assertNotNull(all.get(0).getLastModified(), "lastModified 供保留期判定");
        assertNotNull(all.get(0).getSize());

        List<FileObjectEntry> prefixed = client.listObjects("2026/", 100);
        assertEquals(2, prefixed.size());

        List<FileObjectEntry> bounded = client.listObjects("", 2);
        assertEquals(2, bounded.size(), "清点受 maxEntries 有界");
    }

    /**
     * codex r2 第三形状自查：树内【游离】junction（不是任何配置的清点根）指向外部物理目录——
     * Windows 上 Files.walk 默认下钻 junction，外部文件会被洗进本配置清点清单（引用查不到 → 误删）。
     * listObjects 必须不下钻别名目录（少清点永远安全）。
     */
    @Test
    public void listObjects_skipsAliasDirectories_insideTree() throws Exception {
        // 清点根独立成目录；外部目标在根之外（模拟「他配置物理目录」）
        Path root = Files.createDirectories(tempDir.resolve("root"));
        Path external = Files.createDirectories(tempDir.resolve("external"));
        Files.write(external.resolve("secret.bin"), "x".getBytes());
        Files.createDirectories(root.resolve("base-tree")); // 链接父目录须先存在
        Path alias = createAliasLink(root.resolve("base-tree").resolve("alias-inner"), external);
        assumeTrue(alias != null && Files.exists(alias), "环境不支持符号链接/junction 创建，跳过别名目录用例");
        LocalFileClient client = newClient(root);
        client.upload("a".getBytes(), "base-tree/real.bin", "application/octet-stream");

        List<String> paths = client.listObjects("", 100).stream()
                .map(FileObjectEntry::getPath).toList();

        assertTrue(paths.contains("base-tree/real.bin"), "真实文件正常清点");
        assertTrue(paths.stream().noneMatch(p -> p.startsWith("base-tree/alias-inner/")),
                "树内别名目录不得下钻（外部物理文件不进清单）");
        assertTrue(paths.stream().noneMatch(p -> p.contains("secret.bin")),
                "外部物理文件不得以任何路径进入本配置清单");
    }

    @Test
    public void listObjects_emptyStorage_returnsEmpty() {
        LocalFileClient client = newClient();
        assertTrue(client.listObjects("", 100).isEmpty());
    }

    /** 别名链接创建：优先符号链接，Windows 无特权退回目录 junction（mklink /J），均不可用返回 null */
    private Path createAliasLink(Path link, Path target) {
        try {
            return Files.createSymbolicLink(link, target);
        } catch (UnsupportedOperationException | IOException | SecurityException ex) {
            // fallthrough 到 junction
        }
        try {
            Process p = new ProcessBuilder("cmd", "/c", "mklink", "/J",
                    link.toString(), target.toString()).start();
            if (p.waitFor() == 0 && Files.exists(link)) {
                return link;
            }
        } catch (Exception ex) {
            // 非 Windows/无 cmd
        }
        return null;
    }

    /**
     * ZS-FILE-005.B codex r0 P1-2（文件系统层佐证）：大小写不敏感文件系统（Windows/默认 macOS）下，
     * 先上传 "Asset" 再上传 "asset" 只有一个物理文件，Files.walk 只返回既有目录拼写；
     * 清点结果必须能以 LOWER 折叠覆盖两种拼写（服务层引用核验按折叠匹配，见
     * FileOrphanServiceTest.caseInsensitiveFilesystem_referenceMatchedByFoldedPath_notOrphan）。
     * 大小写敏感文件系统（Linux）上两路径为独立文件、双双返回——断言两种平台行为均满足折叠覆盖。
     */
    @Test
    public void listObjects_caseVariantPaths_foldedCoverage() throws Exception {
        LocalFileClient client = newClient();
        client.upload("a".getBytes(), "Asset/a.bin", "text/plain");
        client.upload("b".getBytes(), "asset/a.bin", "text/plain");

        List<String> foldedPaths = client.listObjects("", 100).stream()
                .map(e -> e.getPath().toLowerCase(java.util.Locale.ROOT)).toList();

        assertTrue(foldedPaths.contains("asset/a.bin"),
                "清点结果折叠后必须覆盖 case 变体路径（不敏感 FS 单拼写 / 敏感 FS 双拼写）");
    }

}
