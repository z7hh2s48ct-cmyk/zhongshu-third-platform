package cn.zszj.module.infra.dal.dataobject.file;

import cn.zszj.framework.mybatis.core.dataobject.BaseDO;
import cn.zszj.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 文件交付票据/下载会话（ZS-FILE-004.A）。
 *
 * <p>一行两态：签发后为 {@link #STATUS_WAITING}（仅存票据散列，不明文），原子兑换后为
 * {@link #STATUS_REDEEMED}（同一行承载下载会话：deliverySessionId + 登录会话绑定 + 有效期），
 * 撤权为 {@link #STATUS_REVOKED}。兑换谓词（owner·tenant·purpose·status·有效期）在消费 SQL 上
 * 显式匹配——供体 JdbcDeliveryPort 的消费 SQL 缺失这些谓词，不能原样当作完整授权。</p>
 *
 * <p>全局表（{@code @TenantIgnore}）：跨租户兑换须能定位票据行并按谓词显式拒绝（FORBIDDEN），
 * 而非被租户插件静默过滤成「不存在」；tenant_id 由服务层显式写入与校验。</p>
 */
@TableName("infra_file_delivery_ticket")
@KeySequence("infra_file_delivery_ticket_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TenantIgnore
public class FileDeliveryTicketDO extends BaseDO {

    /** 状态：已签发待兑换（一次性，原子兑换前）。 */
    public static final String STATUS_WAITING = "WAITING";

    /** 状态：已兑换（同一行承载下载会话）。 */
    public static final String STATUS_REDEEMED = "REDEEMED";

    /** 状态：已撤权（兑换前撤回票据 / 兑换后撤回下载会话，均停止后续取流）。 */
    public static final String STATUS_REVOKED = "REVOKED";

    /**
     * 票据编号
     */
    @TableId
    private Long id;

    /**
     * 票据散列（SHA-256 hex，票据 token 不明文落库）
     */
    private String ticketHash;

    /**
     * 交付文件编号（infra_file.id）
     */
    private Long fileId;

    /**
     * 绑定主体（签发时登录用户 ID；兑换/取流须匹配）
     */
    private Long ownerUserId;

    /**
     * 用途（download / export；兑换须匹配）
     */
    private String purpose;

    /**
     * 状态：{@link #STATUS_WAITING} / {@link #STATUS_REDEEMED} / {@link #STATUS_REVOKED}
     */
    private String status;

    /**
     * 下载会话 ID（兑换时生成；重复兑换幂等返回同一会话）
     */
    private String deliverySessionId;

    /**
     * 登录会话绑定（兑换时写入；后续取流须携带一致标识——会话 ID 不单独代替认证）
     */
    private String loginSession;

    /**
     * 技术租户（签发主体所在租户；兑换/取流须匹配）
     */
    private Long tenantId;

    /**
     * 有效期（票据过期时间，兼作下载会话过期时间）
     */
    private LocalDateTime expiresTime;

    /**
     * 兑换时间
     */
    private LocalDateTime redeemTime;

}
