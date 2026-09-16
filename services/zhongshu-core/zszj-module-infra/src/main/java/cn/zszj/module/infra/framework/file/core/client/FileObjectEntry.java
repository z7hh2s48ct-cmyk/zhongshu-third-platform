package cn.zszj.module.infra.framework.file.core.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 存储对象清点条目（ZS-FILE-005.B：孤儿对象「预览」的存储侧能力）。
 *
 * @author ZS-FILE-005.B
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileObjectEntry {

    /**
     * 对象相对路径（与 upload/delete 的 path 同一坐标系，正斜杠）
     */
    private String path;

    /**
     * 对象大小（字节）；存储侧无法低成本获取时为 null（不阻塞保留期判定）
     */
    private Long size;

    /**
     * 对象最后修改时间；孤儿保留期判定的锚点。存储侧无法提供时为 null——
     * 服务层对 null 保守跳过（不清理年龄未知对象）
     */
    private LocalDateTime lastModified;

}
