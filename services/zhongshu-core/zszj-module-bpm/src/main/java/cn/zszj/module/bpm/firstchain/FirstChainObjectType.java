package cn.zszj.module.bpm.firstchain;

/**
 * 首链对象类型（ZS-BPM-003，D-07 M1 批准的五对象中归本卡状态机管辖的两个）。
 *
 * @author ZS-BPM-003
 */
public enum FirstChainObjectType {

    /** 加盟商申请（D-07 M3 三态，Flowable 单节点审批，M10=B） */
    APPLICATION("franchisee_application"),

    /** 线索（D-07 M6 五态，事件驱动的领域状态机，业务表随首链业务模块落卡） */
    LEAD("lead");

    private final String key;

    FirstChainObjectType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

}
