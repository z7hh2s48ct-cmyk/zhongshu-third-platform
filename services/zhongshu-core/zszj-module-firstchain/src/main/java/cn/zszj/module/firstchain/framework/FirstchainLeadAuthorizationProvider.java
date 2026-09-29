package cn.zszj.module.firstchain.framework;

import cn.zszj.framework.datapermission.core.authorize.ClassifiedObjectAuthorizationProvider;
import cn.zszj.framework.datapermission.core.authorize.FieldLevel;
import cn.zszj.module.bpm.firstchain.LeadStatus;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 线索域动作与字段授权 Provider（ZS-FC-002，接入 ZS-PERM-003.A/.B 机制与 D-12 A 类字段目录）。
 *
 * <p>字段分级编目循 D-12 §4.1（A 类一期首链线索域，目录先改、代码后接的回填制——
 * 新增敏感字段先回填 docs/08 §7 再编目接入）：
 * <ul>
 *     <li>F1：customer_name（客户姓名）、source（线索来源）、followup_content（跟进记录内容）；</li>
 *     <li>F2：customer_phone（客户手机号）、customer_wechat（客户微信号）、customer_address（客户详细地址），
 *         员工默认脱敏可见（尾四位，D-12 §5.2），由裁决服务输出 maskedFields 供消费端裁剪；</li>
 * </ul>
 * 金额类字段一期不建（M8/D-12 B 类随阶段 3 报价域回填），不得在本 Provider 预编目。
 *
 * <p>状态维钩子（D-07 M6/M7 拍板口径，object 传线索状态字符串 {@link LeadStatus} 的 name）：
 * assign 仅 DISTRIBUTED、claim/reassign 仅 ASSIGNED、followup/convert/invalidate 仅 FOLLOWING
 * （终态锁定由状态机 fail-closed 保证，本钩子为授权输出维的同源表达）；object 为 null 时放行
 * （调用方未提供对象实例）。
 *
 * @author ZS-FC-002
 */
public class FirstchainLeadAuthorizationProvider implements ClassifiedObjectAuthorizationProvider {

    /** 对象类型路由键（同一容器内唯一） */
    public static final String OBJECT_TYPE = "firstchain:lead";

    private static final List<String> CANDIDATE_ACTIONS = List.of(
            "distribute", "assign", "claim", "reassign", "followup", "convert", "invalidate");

    private static final List<String> CANDIDATE_FIELDS = List.of(
            "customer_name", "customer_phone", "customer_wechat", "customer_address", "source", "followup_content");

    private static final Map<String, FieldLevel> FIELD_LEVELS = Map.of(
            "customer_name", FieldLevel.F1,
            "customer_phone", FieldLevel.F2,
            "customer_wechat", FieldLevel.F2,
            "customer_address", FieldLevel.F2,
            "source", FieldLevel.F1,
            "followup_content", FieldLevel.F1);

    @Override
    public String getObjectType() {
        return OBJECT_TYPE;
    }

    @Override
    public Collection<String> getCandidateActions() {
        return CANDIDATE_ACTIONS;
    }

    @Override
    public Collection<String> getCandidateFields() {
        return CANDIDATE_FIELDS;
    }

    @Override
    public Map<String, FieldLevel> getFieldLevels() {
        return FIELD_LEVELS;
    }

    @Override
    public boolean isActionAllowedInStatus(String action, Object object) {
        if (!(object instanceof String status)) {
            return true; // 调用方未提供对象实例，不施加状态约束（SPI 默认语义）
        }
        return switch (action) {
            case "distribute" -> true; // 下发创建前置，无来源状态
            case "assign" -> LeadStatus.DISTRIBUTED.name().equals(status);
            case "claim", "reassign" -> LeadStatus.ASSIGNED.name().equals(status);
            case "followup", "convert", "invalidate" -> LeadStatus.FOLLOWING.name().equals(status);
            default -> true;
        };
    }

}
