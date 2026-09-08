package cn.zszj.module.pms.controller.admin.kb.library.vo.template;

import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.common.pojo.PageParam;
import cn.zszj.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - PMS 知识库模板分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class PmsKnowledgeLibraryTemplatePageReqVO extends PageParam {

    @Schema(description = "模板名称，模糊匹配", example = "产品研发")
    private String name;

    @Schema(description = "模板状态", example = "0")
    @InEnum(CommonStatusEnum.class)
    private Integer status;

}
