package cn.zszj.module.infra.framework.file.core.client.local;

import cn.zszj.module.infra.framework.file.core.client.FileObjectEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ZS-FILE-005.B：LocalFileClient 对象清点（孤儿对象「预览」的存储侧能力）。
 *
 * <p>合同：返回相对路径（正斜杠）、按 path 稳定排序、受 maxEntries 有界、前缀过滤；lastModified 供保留期判定。</p>
 */
public class LocalFileClientListTest {

    @TempDir
    Path tempDir;

    private LocalFileClient newClient() {
        LocalFileClientConfig config = new LocalFileClientConfig();
        config.setBasePath(tempDir.toString());
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

    @Test
    public void listObjects_emptyStorage_returnsEmpty() {
        LocalFileClient client = newClient();
        assertTrue(client.listObjects("", 100).isEmpty());
    }

}
