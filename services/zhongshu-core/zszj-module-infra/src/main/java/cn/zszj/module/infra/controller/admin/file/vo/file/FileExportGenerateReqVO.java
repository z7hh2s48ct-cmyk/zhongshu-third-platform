package cn.zszj.module.infra.controller.admin.file.vo.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Set;

/**
 * 导出生成请求 VO（ZS-FILE-004.B：内部 Java API 入参——导出生成通道无 HTTP 端点）。
 *
 * <p>内容必须由服务端业务代码传入（裁剪由调用方消费统一裁决输出实现，服务不解析内容字节）；
 * objectType/orgId/ownerUserId/fields 为导出条件，一律服务端裁决（客户端不可绕）。</p>
 *
 * @author ZS-FILE-004.B
 */
@Schema(description = "管理后台 - 导出生成 Request VO（ZS-FILE-004.B）")
@Data
public class FileExportGenerateReqVO {

    @Schema(description = "导出内容（服务端业务代码按批准字段裁剪后的字节）", requiredMode = Schema.RequiredMode.REQUIRED)
    private byte[] content;

    @Schema(description = "导出文件名", requiredMode = Schema.RequiredMode.REQUIRED, example = "export-2026.bin")
    private String name;

    @Schema(description = "MIME 类型（仅声明；服务端以纯内容探测为准）", example = "application/octet-stream")
    private String type;

    @Schema(description = "对象类型（统一对象授权接入契约）", requiredMode = Schema.RequiredMode.REQUIRED, example = "demo-export")
    private String objectType;

    @Schema(description = "对象所属业务组织（对象维重检目标）", example = "100")
    private Long orgId;

    @Schema(description = "对象所有者用户编号（裁决输入）", example = "101")
    private Long ownerUserId;

    @Schema(description = "声明的敏感字段集合（字段维重检：须 ⊆ 授权字段；未启用字段级时非空=fail-closed）")
    private Set<String> fields;

    @Schema(description = "源对象实例（状态维钩子输入；可空）")
    private Object object;

}
