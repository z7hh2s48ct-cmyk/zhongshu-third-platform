package cn.zszj.module.infra.dal.dataobject.file;

import cn.zszj.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

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

}
