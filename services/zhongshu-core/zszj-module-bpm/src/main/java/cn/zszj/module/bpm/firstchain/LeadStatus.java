package cn.zszj.module.bpm.firstchain;

/**
 * 线索状态（ZS-BPM-003，D-07 M6 已批准五态：DISTRIBUTED→ASSIGNED→FOLLOWING→CONVERTED/INVALID）。
 *
 * <p>领取并发唯一（条件 UPDATE 保证）；负责人可改派（ASSIGNED 域内换人，不改状态）；转商机前置
 * ≥1 条跟进 + 客户姓名非空（D-07 M7，随首链业务模块落卡校验）；CONVERTED/INVALID 终态锁定。
 *
 * @author ZS-BPM-003
 */
public enum LeadStatus {

    DISTRIBUTED,
    ASSIGNED,
    FOLLOWING,
    CONVERTED,
    INVALID;

}
