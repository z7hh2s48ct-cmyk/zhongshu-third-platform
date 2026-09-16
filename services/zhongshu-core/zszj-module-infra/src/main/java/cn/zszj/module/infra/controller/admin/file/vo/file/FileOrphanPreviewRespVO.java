package cn.zszj.module.infra.controller.admin.file.vo.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 孤儿对象预览 Response VO（ZS-FILE-005.B：先预览后清理，清理须以本预览结果显式授权）
 *
 * @author ZS-FILE-005.B
 */
@Schema(description = "管理后台 - 孤儿对象预览 Response VO")
@Data
public class FileOrphanPreviewRespVO {

    @Schema(description = "存储配置编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long configId;

    @Schema(description = "本次清点使用的前缀（空串=全部；截断时以逐段前缀续扫推进，codex r0 P2-2）",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "asset/")
    private String prefix;

    @Schema(description = "清点是否达到上限被截断（true 时应以更细前缀多次清点，不得以本次结果为完整清单）",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
    private boolean truncated;

    @Schema(description = "孤儿候选清单（已过滤：被任意租户记录引用 / 未过保留期 / temp 活跃凭证认领 的对象不在内）")
    private List<FileOrphanItemRespVO> items;

}
