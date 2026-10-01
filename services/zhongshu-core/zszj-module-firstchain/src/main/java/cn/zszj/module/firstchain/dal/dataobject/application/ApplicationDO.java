package cn.zszj.module.firstchain.dal.dataobject.application;

import cn.zszj.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 首链加盟商申请 DO（ZS-FC-001，PILOT-REQ-001/002）——映射既有表 {@code bpm_first_chain_application}。
 *
 * <p>表由 ZS-BPM-003 迁移 V20260928.001 建立（bigserial 主键、uk(tenant_id, app_key) WHERE deleted=FALSE），
 * 本卡不新增任何表/迁移（接入合同「不为未来域预建表」+ 本卡硬约束）。bpm 域层
 * （cn.zszj.module.bpm.firstchain）以 JdbcTemplate 裸 SQL 写本表；本 DO 供 firstchain 模块
 * REST 面分页/详情查询（MyBatis Plus 租户插件自动租户过滤 + @TableLogic 逻辑删除）。
 *
 * <p>主键说明：表主键为 bigserial 且无配对 {@code *_seq} 序列（本卡禁止新增迁移），而 PG 运行态
 * MyBatis Plus 全局 id-type=INPUT（IdTypeEnvironmentPostProcessor）按序列取号——故 {@link #id}
 * 显式 {@code ASSIGN_ID}（雪花），与 bpm 域服务裸 INSERT 走 bigserial 自增并存无冲突
 * （雪花值域远超自增水位，序列回追碰撞不可达）。不声明 @KeySequence（无对应 PG 序列可取）。
 *
 * <p>状态机与版本语义权威在 bpm 域层，本 DO 只承载 firstchain 侧读模型与出入参转换，不做直写状态。
 *
 * @author ZS-FC-001
 */
@TableName("bpm_first_chain_application")
@Data
@EqualsAndHashCode(callSuper = true)
public class ApplicationDO extends TenantBaseDO {

    /**
     * 申请 ID（雪花取号，见类注释主键说明）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 申请编号（服务端生成、租户内唯一；开通幂等键 = 申请编号，D-07 M1/M4）
     */
    private String appKey;
    /**
     * 申请方名称（M8 字段）
     */
    private String applicantName;
    /**
     * 联系人（M8 字段）
     */
    private String contactName;
    /**
     * 联系人电话（M8 字段，建议 F2 挂级——docs/08 §7 回填制随接入登记，本卡只存自由文本列）
     */
    private String contactPhone;
    /**
     * 资质附件 fileIds（原始列透传：bpm 域层以 JSON 数组字符串写入，如 {@code [101,102]}；
     * FILE 私有——只存引用，下载走 FileDeliveryService 票据交付既有设施，不自建通道，接入合同 §1.10）
     */
    private String attachmentFileIds;
    /**
     * 拒绝意见（REJECTED 必填，由 bpm 域服务 onApprovalCompleted 落）
     */
    private String rejectReason;
    /**
     * 申请状态（M3 三态 DRAFT→SUBMITTED→APPROVED/REJECTED；字符串枚举见 bpm 域 FranchiseeApplicationStatus）
     */
    private String status;
    /**
     * 乐观锁版本（一切更新携带 expectedVersion 条件迁移，M3/接入合同 §1.4）
     */
    private Long version;

    /**
     * 逻辑删除（覆写字面量为 FALSE/TRUE）——bpm_first_chain_application.deleted 为 boolean（V20260928.001，对齐 MSG 域惯例；
     * bpm 域手写 JDBC 一律 {@code deleted = FALSE}），而全局 {@code @TableLogic} 为 0/1 数值字面量：MyBatis-Plus 的
     * selectById/selectPage 在 PG 拼 {@code deleted = 0}，报 {@code operator does not exist: boolean = integer}，
     * 申请域 get/page 真实 PG 返回 500（H2 bit 接受 0 故此前单测与 PG 运行期套件均未暴露）。字段级覆写优先于全局配置，
     * 与 MSG-004 {@code NotifyChannelSendDO} 先例一致，PG/H2 双方言可移植；无需迁移、不触碰既有手写 SQL。
     */
    @com.baomidou.mybatisplus.annotation.TableLogic(value = "FALSE", delval = "TRUE")
    private Boolean deleted;

}
