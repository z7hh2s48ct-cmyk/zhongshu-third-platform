package cn.zszj.module.infra.controller.admin.codegen.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 「裸 ID 集合」响应的显式字段包装（ZS-SEC-009.B r1 P2-B）。
 *
 * <p>{@code CommonResult<List<Long>>} 顶层属性名为 {@code data} 且经泛型擦除后声明类型为 Object，
 * ID→string 序列化合同（按字段名 id/Id/Ids 识别）无法作用于其集合元素，wire 仍输出 number。
 * 包装为显式命名字段 {@code tableIds}（命中 Ids 命名约定）后元素恒输出 string。
 */
@Schema(description = "管理后台 - 代码生成表编号集合 Response VO（ID 恒 string 输出）")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CodegenTableIdListRespVO {

    @Schema(description = "表编号集合", requiredMode = Schema.RequiredMode.REQUIRED, example = "[\"1\",\"2\"]")
    private List<Long> tableIds;

}
