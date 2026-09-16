package cn.zszj.module.system.service.notify;

import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.module.system.controller.admin.notify.vo.message.NotifyMessageMyPageReqVO;
import cn.zszj.module.system.controller.admin.notify.vo.message.NotifyMessagePageReqVO;
import cn.zszj.module.system.dal.dataobject.notify.NotifyMessageDO;
import cn.zszj.module.system.dal.dataobject.notify.NotifyTemplateDO;
import cn.zszj.module.system.dal.mysql.notify.NotifyMessageMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 站内信 Service 实现类
 *
 * @author xrcoder
 */
@Service
@Validated
public class NotifyMessageServiceImpl implements NotifyMessageService {

    /** 未读列表单次拉取条数上限（ZS-MSG-003：与控制器 @Max(100) 同源，防止绕过入口全量拉取） */
    static final int UNREAD_LIST_MAX_SIZE = 100;

    /** 未读列表默认条数（与控制器 defaultValue=10 同源） */
    static final int UNREAD_LIST_DEFAULT_SIZE = 10;

    @Resource
    private NotifyMessageMapper notifyMessageMapper;

    @Override
    public Long createNotifyMessage(Long userId, Integer userType,
                                    NotifyTemplateDO template, String templateContent, Map<String, Object> templateParams) {
        NotifyMessageDO message = new NotifyMessageDO().setUserId(userId).setUserType(userType)
                .setTemplateId(template.getId()).setTemplateCode(template.getCode())
                .setTemplateType(template.getType()).setTemplateNickname(template.getNickname())
                .setTemplateContent(templateContent).setTemplateParams(templateParams).setReadStatus(false);
        notifyMessageMapper.insert(message);
        return message.getId();
    }

    @Override
    public PageResult<NotifyMessageDO> getNotifyMessagePage(NotifyMessagePageReqVO pageReqVO) {
        return notifyMessageMapper.selectPage(pageReqVO);
    }

    @Override
    public PageResult<NotifyMessageDO> getMyMyNotifyMessagePage(NotifyMessageMyPageReqVO pageReqVO, Long userId, Integer userType) {
        return notifyMessageMapper.selectPage(pageReqVO, userId, userType);
    }

    @Override
    public NotifyMessageDO getNotifyMessage(Long id) {
        return notifyMessageMapper.selectById(id);
    }

    @Override
    public List<NotifyMessageDO> getUnreadNotifyMessageList(Long userId, Integer userType, Integer size) {
        return notifyMessageMapper.selectUnreadListByUserIdAndUserType(userId, userType, normalizeUnreadSize(size));
    }

    /**
     * 防御性收敛未读列表条数（ZS-MSG-003「限制未读列表 size」）：null/非正 → 默认 10，
     * 超上限 → 截断到 {@link #UNREAD_LIST_MAX_SIZE}。不信任调用方（HTTP 入口另有
     * @Min/@Max 显式 400；内部调用无校验链，须在此兜底，避免全量拉取未读）。
     */
    static int normalizeUnreadSize(Integer size) {
        if (size == null || size <= 0) {
            return UNREAD_LIST_DEFAULT_SIZE;
        }
        return Math.min(size, UNREAD_LIST_MAX_SIZE);
    }

    @Override
    public Long getUnreadNotifyMessageCount(Long userId, Integer userType) {
        return notifyMessageMapper.selectUnreadCountByUserIdAndUserType(userId, userType);
    }

    @Override
    public int updateNotifyMessageRead(Collection<Long> ids, Long userId, Integer userType) {
        return notifyMessageMapper.updateListRead(ids, userId, userType);
    }

    @Override
    public int updateAllNotifyMessageRead(Long userId, Integer userType) {
        return notifyMessageMapper.updateListRead(userId, userType);
    }

}
