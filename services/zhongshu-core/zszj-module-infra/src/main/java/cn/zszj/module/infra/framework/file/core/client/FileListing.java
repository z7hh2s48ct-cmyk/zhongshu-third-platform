package cn.zszj.module.infra.framework.file.core.client;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 对象清点结果（ZS-FILE-005.B codex r3 P2）：条目 + 「不可验证」上报。
 *
 * <p>背景：local 清点对符号链接/别名/解析失败的目录保守跳过（不下钻）——被跳过目录之下的
 * 对象可能真实存在却不出现在 {@link #entries} 中，「未出现在清单」≠「已不存在」。
 * 清理侧必须据此区分两种语义：条目命中 → 可核验；条目缺失但命中 {@link #unverifiablePrefixes}
 * / {@link #unverifiablePaths} → 不可验证（拒绝清理，绝不假报幂等成功）。
 * 清点即权威的存储（如 db）默认无可验证盲区（两个上报均为空列表）。</p>
 */
@Getter
@AllArgsConstructor
public class FileListing {

    /**
     * 清点条目（语义与 {@link FileClient#listObjects} 一致：按 path 排序、受 maxEntries 有界）
     */
    private final List<FileObjectEntry> entries;

    /**
     * 不可验证目录前缀（相对路径、正斜杠、以 {@code /} 结尾，覆盖其下整棵子树）：
     * 枚举期被跳过（符号链接/别名/解析失败）的目录。大小写敏感存储上与请求 path 的
     * 匹配按忽略大小写进行（大小写不敏感文件系统上变体拼写寻址同一物理目录）
     */
    private final List<String> unverifiablePrefixes;

    /**
     * 不可验证文件路径（相对路径，精确匹配、忽略大小写）：
     * 枚举期被跳过的符号链接文件（删除会穿透到链接目标，不清洗进清单）
     */
    private final List<String> unverifiablePaths;

}
