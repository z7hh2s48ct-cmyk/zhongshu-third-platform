package cn.zszj.module.firstchain.controller.admin.lead.vo;

import cn.zszj.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 首链线索分页 Request VO（ZS-FC-002，PILOT-REQ-009；视角范围服务端解析——"
        + "平台看授权范围/负责人看本组织/员工看本人，不接受客户端声明归属或视角）")
@Data
@EqualsAndHashCode(callSuper = true)
public class LeadPageReqVO extends PageParam {

    @Schema(description = "线索状态（DISTRIBUTED/ASSIGNED/FOLLOWING/CONVERTED/INVALID，D-07 M6 五态）",
            example = "FOLLOWING")
    private String status;

}
