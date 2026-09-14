package cn.zszj.module.infra.dal.dataobject.file;

import cn.zszj.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 预签名直传凭证（ZS-FILE-003）
 *
 * 绑定主体/租户/临时对象键/大小/类型/有效期；完成确认后原子迁移为 COMPLETED 并关联正式文件。
 *
 * @author ZS-FILE-003
 */
@TableName("infra_file_upload_credential")
@KeySequence("infra_file_upload_credential_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FileUploadCredentialDO extends TenantBaseDO {

    /**
     * 状态：待上传
     */
    public static final String STATUS_WAITING_UPLOAD = "WAITING_UPLOAD";
    /**
     * 状态：已完成（正式资产已发布）
     */
    public static final String STATUS_COMPLETED = "COMPLETED";

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 凭证令牌（一次性完成确认的唯一凭据）
     */
    private String credentialToken;
    /**
     * 存储配置编号（服务端指定，客户端不可指定）
     */
    private Long configId;
    /**
     * 上传主体用户编号
     */
    private Long ownerUserId;
    /**
     * 上传用途
     */
    private String purpose;
    /**
     * 临时对象键（平台凭据只允许写临时区）
     */
    private String tempPath;
    /**
     * 原文件名
     */
    private String fileName;
    /**
     * 声明的内容类型
     */
    private String contentType;
    /**
     * 声明的大小（字节）
     */
    private Long declaredSize;
    /**
     * 可见范围（PRIVATE/PUBLIC，PUBLIC 完成时校验白名单）
     */
    private String scope;
    /**
     * 状态
     */
    private String status;
    /**
     * 过期时间
     */
    private LocalDateTime expiresTime;
    /**
     * 完成时间
     */
    private LocalDateTime completedTime;
    /**
     * 完成后关联的正式文件编号
     */
    private Long fileId;

}
