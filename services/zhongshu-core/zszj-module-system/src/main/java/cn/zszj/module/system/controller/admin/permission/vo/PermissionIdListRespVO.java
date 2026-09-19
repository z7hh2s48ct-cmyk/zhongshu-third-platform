package cn.zszj.module.system.controller.admin.permission.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

/**
 * 「裸 ID 集合」响应的显式字段包装（ZS-SEC-009.B r1 P2-B）。
 *
 * <p>原 {@code CommonResult<Set<Long>>} 顶层属性名为 {@code data} 且经泛型擦除后声明类型为 Object，
 * ID→string 序列化合同（按字段名 id/Id/Ids 识别）无法作用于集合元素，wire 仍输出 number——
 * 与已迁移 string 的前端选项 ID 比较失配（已授权限被清空风险，codex r0 P1/r1 P2-B 实证）。
 * 包装为显式命名字段（menuIds/roleIds）后命中命名约定，元素恒输出 string。
 *
 * <p>字段语义即「编号集合」，命名即合同；两个端点共用本 VO，字段按端点各自取用。
 */
@Schema(description = "管理后台 - 编号集合 Response VO（ID 恒 string 输出）")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Validated
public class PermissionIdListRespVO {

    @Schema(description = "菜单编号集合", requiredMode = Schema.RequiredMode.REQUIRED, example = "[\"1\",\"2\"]")
    private Set<Long> menuIds;

    @Schema(description = "角色编号集合", requiredMode = Schema.RequiredMode.REQUIRED, example = "[\"1\",\"2\"]")
    private Set<Long> roleIds;

}
