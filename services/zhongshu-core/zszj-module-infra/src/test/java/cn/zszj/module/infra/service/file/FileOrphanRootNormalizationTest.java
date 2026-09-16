package cn.zszj.module.infra.service.file;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * codex r1 P1-B：存储根规范化/包含判定合同单测（纯字符串变换，不触文件系统，双平台确定）。
 *
 * <p>合同（{@link FileOrphanServiceImpl#toCanonicalRoot}）：正斜杠统一 + 大小写折叠 +
 * 尾分隔符剥离（根路径如 {@code Z:\} 规范化后仍带尾分隔符，剥离后追加边界分隔符才不会得到
 * {@code z://} 式失配——修复前 {@code c:/uploads} 不以 {@code c://} 开头，内层配置绕过隔离）；
 * 剥离至空保留 {@code /}（整个文件系统根）。{@link FileOrphanServiceImpl#sharesPhysicalRoot}：
 * 入参无尾分隔符，边界只补不加；任一方为 {@code /} 视为共享。</p>
 */
public class FileOrphanRootNormalizationTest {

    @Test
    public void toCanonicalRoot_foldsCaseUnifiesSeparatorsAndStripsTrailing() {
        assertEquals("z:", FileOrphanServiceImpl.toCanonicalRoot(Paths.get("Z:")),
                "盘根规范化：小写折叠（Windows 下 Z:/ 规范化仍带尾分隔符，剥离后同为 z:）");
        assertEquals("z:/storage", FileOrphanServiceImpl.toCanonicalRoot(Paths.get("Z:/storage")),
                "普通路径小写 + 正斜杠统一");
        assertEquals("/", FileOrphanServiceImpl.toCanonicalRoot(Paths.get("/")),
                "文件系统根保留 /（包罗一切语义）");
    }

    @Test
    public void sharesPhysicalRoot_boundaryComparisonIsTailSlashSafe() {
        // P1-B 回归形态：根 z:（原 Z:/）与内层 z:/uploads——修复前边界 z:// 永不失配
        assertTrue(FileOrphanServiceImpl.sharesPhysicalRoot("z:", "z:/uploads"),
                "盘根包含一切同盘子目录（尾分隔符剥离后边界不失配）");
        assertTrue(FileOrphanServiceImpl.sharesPhysicalRoot("z:/storage", "z:/storage/sub"),
                "嵌套根共享");
        assertTrue(FileOrphanServiceImpl.sharesPhysicalRoot("z:/storage/sub", "z:/storage"),
                "嵌套根共享（方向无关）");
        assertTrue(FileOrphanServiceImpl.sharesPhysicalRoot("z:/storage", "z:/storage"),
                "相等根共享");
        // 边界必须落在分隔符上：storage2 不是 storage 的子目录
        assertFalse(FileOrphanServiceImpl.sharesPhysicalRoot("z:/storage", "z:/storage2"),
                "仅前缀字符串相同但目录分量不同 → 不共享");
        assertFalse(FileOrphanServiceImpl.sharesPhysicalRoot("z:/storage-a", "z:/storage-b"),
                "互斥根不共享");
    }

    @Test
    public void sharesPhysicalRoot_filesystemRootContainsEverything() {
        assertTrue(FileOrphanServiceImpl.sharesPhysicalRoot("/", "z:/anything"),
                "文件系统根 / 包罗一切（保守共享）");
        assertTrue(FileOrphanServiceImpl.sharesPhysicalRoot("c:/x", "/"),
                "文件系统根 / 包罗一切（方向无关）");
    }

}
