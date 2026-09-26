package cn.zszj.module.infra.dal.dataobject.file;

import cn.zszj.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 文件表
 * 每次文件上传，都会记录一条记录到该表中
 *
 * @author 芋道源码
 */
@TableName("infra_file")
@KeySequence("infra_file_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileDO extends TenantBaseDO {

    /**
     * 资产状态（ZS-FILE-005.A）：已发布（正常可用；存量行迁移默认值）
     */
    public static final String STATUS_PUBLISHED = "PUBLISHED";

    /**
     * 资产状态（ZS-FILE-005.A）：删除中（对象删除尚未完成的可恢复中间态，供人工对账）
     */
    public static final String STATUS_DELETING = "DELETING";

    /**
     * 用途（ZS-FILE-004.B）：导出件——按用途保留期清理的判别依据（普通上传/历史为 null）
     */
    public static final String PURPOSE_EXPORT = "export";


    /**
     * 上传主体用户编号（ZS-FILE-001.A：服务端确认的所有者，匿名/系统上传为 0）
     */
    private Long ownerUserId;
    /**
     * 可见范围（ZS-FILE-001.A）：PUBLIC=公开素材（匿名可读）；PRIVATE=私有附件（默认，需登录且同租户）
     *
     * 枚举 {@link cn.zszj.module.infra.enums.file.FileScopeEnum}
     */
    private String scope;

    /**
     * 业务组织归属（ZS-FILE-001.B）：服务端签发的上传组织（源 {@link cn.zszj.framework.security.core.LoginUser#getOrgId()}），
     * 匿名/系统/无默认任职为 null——org 轴对象授权的数据载体。非 null 时读取/删除由组织范围独占裁决
     * （D-09 FND-AUTH-004：本人所有权不凌驾组织排除，转岗/离任不得凭 owner 访问旧组织文件）；
     * null 历史文件仍由 tenant 轴（ZS-FILE-001.A）治理，org 门不介入。
     */
    private Long organizationId;

    /**
     * 编号，数据库自增
     */
    private Long id;
    /**
     * 配置编号
     *
     * 关联 {@link FileConfigDO#getId()}
     */
    private Long configId;
    /**
     * 原文件名
     */
    private String name;
    /**
     * 路径，即文件名
     */
    private String path;
    /**
     * 访问地址
     */
    private String url;
    /**
     * 文件的 MIME 类型，例如 "application/octet-stream"
     */
    private String type;
    /**
     * 文件大小
     */
    private Long size;
    /**
     * 内容 SHA-256 摘要（ZS-FILE-003：下载内容与入库散列一致的验收基准）
     */
    private String fileHash;

    /**
     * 资产状态（ZS-FILE-005.A）：{@link #STATUS_PUBLISHED} / {@link #STATUS_DELETING}；
     * 上传发布即 PUBLISHED（存量行由迁移默认值回填）
     */
    private String status;

    /**
     * 删除中间态进入时刻（ZS-FILE-005.B）：自动补偿的超时依据与领取租约——
     * 转移 DELETING 时写入；补偿领取时前推（领取即租约 + 重试退避点）；
     * 引用保护拒绝回退 PUBLISHED 时清空。迁移 V20260916.102 对存量 DELETING 按 update_time 回填。
     */
    private LocalDateTime deletingTime;

    /**
     * 用途（ZS-FILE-004.B）：导出件固定 'export'（按用途保留期清理的判别依据）；普通上传为 null。
     */
    private String purpose;

    /**
     * 保留期到期时间（ZS-FILE-004.B）：purpose='export' 的导出件写入（生成时刻 + 配置保留天数，
     * 见 FileExportProperties#retentionDays）；null=无保留期约束（历史/普通上传零变化）。
     */
    private LocalDateTime retentionExpireTime;

}
