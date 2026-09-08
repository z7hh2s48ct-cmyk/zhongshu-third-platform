package cn.zszj.module.pms.controller.admin.kb.library.vo.library;

import cn.zszj.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - PMS 知识库分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PmsKnowledgeLibraryPageReqVO extends PageParam {

    @Schema(description = "知识库名称", example = "产品")
    private String name;

    @Schema(description = "个人知识库分组编号", example = "1024")
    private Long groupId;

}
