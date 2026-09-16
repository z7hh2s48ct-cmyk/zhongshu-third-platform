package cn.zszj.module.infra.controller.admin.file.vo.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 孤儿对象清点条目 Response VO（ZS-FILE-005.B）
 *
 * @author ZS-FILE-005.B
 */
@Schema(description = "管理后台 - 孤儿对象清点条目 Response VO")
@Data
public class FileOrphanItemRespVO {

    /**
     * 资产区分类：有 infra_file 记录体系之外的正式对象
     */
    public static final String CATEGORY_ASSET = "ASSET";

    /**
     * 临时区分类：temp/ 前缀（直传凭证临时对象），无活跃凭证认领且过保留期才进入候选
     */
    public static final String CATEGORY_TEMP = "TEMP";

    @Schema(description = "对象相对路径", requiredMode = Schema.RequiredMode.REQUIRED, example = "asset/20260916/xxx/a.png")
    private String path;

    @Schema(description = "对象大小（字节），存储侧无法低成本获取时为空", example = "1024")
    private Long size;

    @Schema(description = "存储最后修改时间（保留期锚点），存储侧无法提供时为空（该类对象保守不清理）")
    private LocalDateTime lastModified;

    @Schema(description = "分类：ASSET=资产区孤儿；TEMP=临时区孤儿", requiredMode = Schema.RequiredMode.REQUIRED, example = "ASSET")
    private String category;

}
