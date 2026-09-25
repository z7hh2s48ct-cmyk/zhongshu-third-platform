package cn.zszj.module.system.dal.dataobject.notify;

import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.mybatis.core.dataobject.BaseDO;
import cn.zszj.module.system.dal.dataobject.mail.MailTemplateDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

/**
 * 站内信 DO
 *
 * @author xrcoder
 */
@TableName(value = "system_notify_message", autoResultMap = true)
@KeySequence("system_notify_message_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotifyMessageDO extends BaseDO {

    /**
     * 站内信编号，自增
     */
    @TableId
    private Long id;
    /**
     * 用户编号
     *
     * 关联 MemberUserDO 的 id 字段、或者 AdminUserDO 的 id 字段
     */
    private Long userId;
    /**
     * 用户类型
     *
     * 枚举 {@link UserTypeEnum}
     */
    private Integer userType;
    /**
     * 业务组织归属编号（ZS-MSG-003.C）
     *
     * <p>派发时刻收件人的服务端组织上下文（默认任职组织，ZS-IAM-002）；NULL = 无默认任职 / 任职失效 /
     * MEMBER 收件人（会员命名空间不参与任职解析）/ 系统上下文（历史存量）。org 轴可见范围按本列裁决：
     * 非空由组织范围独占，为空由本人兜底放行（超管/平台任职等全部组织授权亦可见）。
     */
    private Long organizationId;

    // ========= 模板相关字段 =========

    /**
     * 模版编号
     *
     * 关联 {@link NotifyTemplateDO#getId()}
     */
    private Long templateId;
    /**
     * 模版编码
     *
     * 关联 {@link NotifyTemplateDO#getCode()}
     */
    private String templateCode;
    /**
     * 模版类型
     *
     * 冗余 {@link NotifyTemplateDO#getType()}
     */
    private Integer templateType;
    /**
     * 模版发送人名称
     *
     * 冗余 {@link NotifyTemplateDO#getNickname()}
     */
    private String templateNickname;
    /**
     * 模版内容
     *
     * 基于 {@link NotifyTemplateDO#getContent()} 格式化后的内容
     */
    private String templateContent;
    /**
     * 模版参数
     *
     * 基于 {@link NotifyTemplateDO#getParams()} 输入后的参数
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> templateParams;

    // ========= 读取相关字段 =========

    /**
     * 是否已读
     */
    private Boolean readStatus;
    /**
     * 阅读时间
     */
    private LocalDateTime readTime;

}
