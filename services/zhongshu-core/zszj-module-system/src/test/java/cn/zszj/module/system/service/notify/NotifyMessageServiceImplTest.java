package cn.zszj.module.system.service.notify;

import cn.hutool.core.map.MapUtil;
import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.module.system.controller.admin.notify.vo.message.NotifyMessageMyPageReqVO;
import cn.zszj.module.system.controller.admin.notify.vo.message.NotifyMessagePageReqVO;
import cn.zszj.module.system.dal.dataobject.notify.NotifyMessageDO;
import cn.zszj.module.system.dal.dataobject.notify.NotifyTemplateDO;
import cn.zszj.module.system.dal.mysql.notify.NotifyMessageMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static cn.hutool.core.util.RandomUtil.randomEle;
import static cn.zszj.framework.common.util.date.LocalDateTimeUtils.buildBetweenTime;
import static cn.zszj.framework.common.util.date.LocalDateTimeUtils.buildTime;
import static cn.zszj.framework.common.util.object.ObjectUtils.cloneIgnoreId;
import static cn.zszj.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.zszj.framework.test.core.util.RandomUtils.*;
import static org.junit.jupiter.api.Assertions.*;

/**
* {@link NotifyMessageServiceImpl} 的单元测试类
*
* @author 芋道源码
*/
@Import(NotifyMessageServiceImpl.class)
public class NotifyMessageServiceImplTest extends BaseDbUnitTest {

    @Resource
    private NotifyMessageServiceImpl notifyMessageService;

    @Resource
    private NotifyMessageMapper notifyMessageMapper;

    @Test
    public void testCreateNotifyMessage_success() {
        // 准备参数
        Long userId = randomLongId();
        Integer userType = randomEle(UserTypeEnum.values()).getValue();
        NotifyTemplateDO template = randomPojo(NotifyTemplateDO.class);
        String templateContent = randomString();
        Map<String, Object> templateParams = randomTemplateParams();
        // mock 方法

        // 调用
        Long messageId = notifyMessageService.createNotifyMessage(userId, userType,
                template, templateContent, templateParams);
        // 断言
        NotifyMessageDO message = notifyMessageMapper.selectById(messageId);
        assertNotNull(message);
        assertEquals(userId, message.getUserId());
        assertEquals(userType, message.getUserType());
        assertEquals(template.getId(), message.getTemplateId());
        assertEquals(template.getCode(), message.getTemplateCode());
        assertEquals(template.getType(), message.getTemplateType());
        assertEquals(template.getNickname(), message.getTemplateNickname());
        assertEquals(templateContent, message.getTemplateContent());
        assertEquals(templateParams, message.getTemplateParams());
        assertEquals(false, message.getReadStatus());
        assertNull(message.getReadTime());
    }

    @Test
    public void testGetNotifyMessagePage() {
       // mock 数据
       NotifyMessageDO dbNotifyMessage = randomPojo(NotifyMessageDO.class, o -> { // 等会查询到
           o.setUserId(1L);
           o.setUserType(UserTypeEnum.ADMIN.getValue());
           o.setTemplateCode("test_01");
           o.setTemplateType(10);
           o.setCreateTime(buildTime(2022, 1, 2));
           o.setTemplateParams(randomTemplateParams());
       });
       notifyMessageMapper.insert(dbNotifyMessage);
       // 测试 userId 不匹配
       notifyMessageMapper.insert(cloneIgnoreId(dbNotifyMessage, o -> o.setUserId(2L)));
       // 测试 userType 不匹配
       notifyMessageMapper.insert(cloneIgnoreId(dbNotifyMessage, o -> o.setUserType(UserTypeEnum.MEMBER.getValue())));
       // 测试 templateCode 不匹配
       notifyMessageMapper.insert(cloneIgnoreId(dbNotifyMessage, o -> o.setTemplateCode("test_11")));
       // 测试 templateType 不匹配
       notifyMessageMapper.insert(cloneIgnoreId(dbNotifyMessage, o -> o.setTemplateType(20)));
       // 测试 createTime 不匹配
       notifyMessageMapper.insert(cloneIgnoreId(dbNotifyMessage, o -> o.setCreateTime(buildTime(2022, 2, 1))));
       // 准备参数
       NotifyMessagePageReqVO reqVO = new NotifyMessagePageReqVO();
       reqVO.setUserId(1L);
       reqVO.setUserType(UserTypeEnum.ADMIN.getValue());
       reqVO.setTemplateCode("est_01");
       reqVO.setTemplateType(10);
       reqVO.setCreateTime(buildBetweenTime(2022, 1, 1, 2022, 1, 10));

       // 调用
       PageResult<NotifyMessageDO> pageResult = notifyMessageService.getNotifyMessagePage(reqVO);
       // 断言
       assertEquals(1, pageResult.getTotal());
       assertEquals(1, pageResult.getList().size());
       assertPojoEquals(dbNotifyMessage, pageResult.getList().get(0));
    }

    @Test
    public void testGetNotifyMessage() {
        // mock 数据
        NotifyMessageDO dbNotifyMessage = randomPojo(NotifyMessageDO.class,
                o -> o.setTemplateParams(randomTemplateParams()));
        notifyMessageMapper.insert(dbNotifyMessage);
        // 准备参数
        Long id = dbNotifyMessage.getId();

        // 调用
        NotifyMessageDO notifyMessage = notifyMessageService.getNotifyMessage(id);
        assertPojoEquals(dbNotifyMessage, notifyMessage);
    }

    @Test
    public void testGetMyNotifyMessagePage() {
        // mock 数据
        NotifyMessageDO dbNotifyMessage = randomPojo(NotifyMessageDO.class, o -> { // 等会查询到
            o.setUserId(1L);
            o.setUserType(UserTypeEnum.ADMIN.getValue());
            o.setReadStatus(true);
            o.setCreateTime(buildTime(2022, 1, 2));
            o.setTemplateParams(randomTemplateParams());
        });
        notifyMessageMapper.insert(dbNotifyMessage);
        // 测试 userId 不匹配
        notifyMessageMapper.insert(cloneIgnoreId(dbNotifyMessage, o -> o.setUserId(2L)));
        // 测试 userType 不匹配
        notifyMessageMapper.insert(cloneIgnoreId(dbNotifyMessage, o -> o.setUserType(UserTypeEnum.MEMBER.getValue())));
        // 测试 readStatus 不匹配
        notifyMessageMapper.insert(cloneIgnoreId(dbNotifyMessage, o -> o.setReadStatus(false)));
        // 测试 createTime 不匹配
        notifyMessageMapper.insert(cloneIgnoreId(dbNotifyMessage, o -> o.setCreateTime(buildTime(2022, 2, 1))));
        // 准备参数
        Long userId = 1L;
        Integer userType = UserTypeEnum.ADMIN.getValue();
        NotifyMessageMyPageReqVO reqVO = new NotifyMessageMyPageReqVO();
        reqVO.setReadStatus(true);
        reqVO.setCreateTime(buildBetweenTime(2022, 1, 1, 2022, 1, 10));

        // 调用
        PageResult<NotifyMessageDO> pageResult = notifyMessageService.getMyMyNotifyMessagePage(reqVO, userId, userType);
        // 断言
        assertEquals(1, pageResult.getTotal());
        assertEquals(1, pageResult.getList().size());
        assertPojoEquals(dbNotifyMessage, pageResult.getList().get(0));
    }

    @Test
    public void testGetUnreadNotifyMessageList() {
        // mock 数据
        NotifyMessageDO dbNotifyMessage = randomPojo(NotifyMessageDO.class, o -> { // 等会查询到
            o.setUserId(1L);
            o.setUserType(UserTypeEnum.ADMIN.getValue());
            o.setReadStatus(false);
            o.setTemplateParams(randomTemplateParams());
        });
        notifyMessageMapper.insert(dbNotifyMessage);
        // 测试 userId 不匹配
        notifyMessageMapper.insert(cloneIgnoreId(dbNotifyMessage, o -> o.setUserId(2L)));
        // 测试 userType 不匹配
        notifyMessageMapper.insert(cloneIgnoreId(dbNotifyMessage, o -> o.setUserType(UserTypeEnum.MEMBER.getValue())));
        // 测试 readStatus 不匹配
        notifyMessageMapper.insert(cloneIgnoreId(dbNotifyMessage, o -> o.setReadStatus(true)));
        // 准备参数
        Long userId = 1L;
        Integer userType = UserTypeEnum.ADMIN.getValue();
        Integer size = 10;

        // 调用
        List<NotifyMessageDO> list = notifyMessageService.getUnreadNotifyMessageList(userId, userType, size);
        // 断言
        assertEquals(1, list.size());
        assertPojoEquals(dbNotifyMessage, list.get(0));
    }

    @Test
    public void testGetUnreadNotifyMessageList_sizeNormalized() {
        // ZS-MSG-003「限制未读列表 size」：服务端防御性收敛，不信任调用方（HTTP 入口另有 @Min/@Max）
        assertEquals(10, NotifyMessageServiceImpl.normalizeUnreadSize(null));
        assertEquals(10, NotifyMessageServiceImpl.normalizeUnreadSize(0));
        assertEquals(10, NotifyMessageServiceImpl.normalizeUnreadSize(-5));
        assertEquals(50, NotifyMessageServiceImpl.normalizeUnreadSize(50));
        assertEquals(100, NotifyMessageServiceImpl.normalizeUnreadSize(100));
        assertEquals(100, NotifyMessageServiceImpl.normalizeUnreadSize(1000));
    }

    @Test
    public void testGetUnreadNotifyMessageList_sizeCappedAtMax() {
        // 端到端证据：请求超上限条数时最多返回 UNREAD_LIST_MAX_SIZE 条，不全量拉取
        NotifyMessageDO dbNotifyMessage = randomPojo(NotifyMessageDO.class, o -> {
            o.setUserId(1L);
            o.setUserType(UserTypeEnum.ADMIN.getValue());
            o.setReadStatus(false);
            o.setTemplateParams(randomTemplateParams());
        });
        for (int i = 0; i < 105; i++) {
            notifyMessageMapper.insert(cloneIgnoreId(dbNotifyMessage,
                    o -> o.setTemplateParams(randomTemplateParams())));
        }
        // 调用：请求 1000 条（超上限）
        List<NotifyMessageDO> list = notifyMessageService.getUnreadNotifyMessageList(1L,
                UserTypeEnum.ADMIN.getValue(), 1000);
        // 断言：截断到上限 100，且全部属于本人
        assertEquals(100, list.size());
        list.forEach(message -> {
            assertEquals(1L, message.getUserId());
            assertFalse(message.getReadStatus());
        });
    }

    @Test
    public void testGetUnreadNotifyMessageCount() {
        // mock 数据
        NotifyMessageDO dbNotifyMessage = randomPojo(NotifyMessageDO.class, o -> { // 等会查询到
            o.setUserId(1L);
            o.setUserType(UserTypeEnum.ADMIN.getValue());
            o.setReadStatus(false);
            o.setTemplateParams(randomTemplateParams());
        });
        notifyMessageMapper.insert(dbNotifyMessage);
        // 测试 userId 不匹配
        notifyMessageMapper.insert(cloneIgnoreId(dbNotifyMessage, o -> o.setUserId(2L)));
        // 测试 userType 不匹配
        notifyMessageMapper.insert(cloneIgnoreId(dbNotifyMessage, o -> o.setUserType(UserTypeEnum.MEMBER.getValue())));
        // 测试 readStatus 不匹配
        notifyMessageMapper.insert(cloneIgnoreId(dbNotifyMessage, o -> o.setReadStatus(true)));
        // 准备参数
        Long userId = 1L;
        Integer userType = UserTypeEnum.ADMIN.getValue();

        // 调用，并断言
        assertEquals(1, notifyMessageService.getUnreadNotifyMessageCount(userId, userType));
    }

    @Test
    public void testUpdateNotifyMessageRead() {
        // mock 数据
        NotifyMessageDO dbNotifyMessage = randomPojo(NotifyMessageDO.class, o -> { // 等会查询到
            o.setUserId(1L);
            o.setUserType(UserTypeEnum.ADMIN.getValue());
            o.setReadStatus(false);
            o.setReadTime(null);
            o.setTemplateParams(randomTemplateParams());
        });
        notifyMessageMapper.insert(dbNotifyMessage);
        // 测试 userId 不匹配
        NotifyMessageDO otherUserMessage = cloneIgnoreId(dbNotifyMessage, o -> o.setUserId(2L));
        notifyMessageMapper.insert(otherUserMessage);
        // 测试 userType 不匹配
        NotifyMessageDO otherTypeMessage = cloneIgnoreId(dbNotifyMessage,
                o -> o.setUserType(UserTypeEnum.MEMBER.getValue()));
        notifyMessageMapper.insert(otherTypeMessage);
        // 测试 readStatus 不匹配
        notifyMessageMapper.insert(cloneIgnoreId(dbNotifyMessage, o -> o.setReadStatus(true)));
        // 准备参数：本人消息 + 混入他人 userId / 他人 userType 的 IDs + 不存在的 ID
        Collection<Long> ids = Arrays.asList(dbNotifyMessage.getId(), otherUserMessage.getId(),
                otherTypeMessage.getId(), dbNotifyMessage.getId() + 10000);
        Long userId = 1L;
        Integer userType = UserTypeEnum.ADMIN.getValue();

        // 调用
        int updateCount = notifyMessageService.updateNotifyMessageRead(ids, userId, userType);
        // 断言
        assertEquals(1, updateCount);
        NotifyMessageDO notifyMessage = notifyMessageMapper.selectById(dbNotifyMessage.getId());
        assertTrue(notifyMessage.getReadStatus());
        assertNotNull(notifyMessage.getReadTime());
        // ZS-MSG-003 显式证据：混入的他人 IDs（不同 userId / 不同 userType）已读状态不被改变
        NotifyMessageDO otherUserAfter = notifyMessageMapper.selectById(otherUserMessage.getId());
        assertFalse(otherUserAfter.getReadStatus());
        assertNull(otherUserAfter.getReadTime());
        NotifyMessageDO otherTypeAfter = notifyMessageMapper.selectById(otherTypeMessage.getId());
        assertFalse(otherTypeAfter.getReadStatus());
        assertNull(otherTypeAfter.getReadTime());
    }

    @Test
    public void testUpdateAllNotifyMessageRead() {
        // mock 数据
        NotifyMessageDO dbNotifyMessage = randomPojo(NotifyMessageDO.class, o -> { // 等会查询到
            o.setUserId(1L);
            o.setUserType(UserTypeEnum.ADMIN.getValue());
            o.setReadStatus(false);
            o.setReadTime(null);
            o.setTemplateParams(randomTemplateParams());
        });
        notifyMessageMapper.insert(dbNotifyMessage);
        // 测试 userId 不匹配
        notifyMessageMapper.insert(cloneIgnoreId(dbNotifyMessage, o -> o.setUserId(2L)));
        // 测试 userType 不匹配
        notifyMessageMapper.insert(cloneIgnoreId(dbNotifyMessage, o -> o.setUserType(UserTypeEnum.MEMBER.getValue())));
        // 测试 readStatus 不匹配
        notifyMessageMapper.insert(cloneIgnoreId(dbNotifyMessage, o -> o.setReadStatus(true)));
        // 准备参数
        Long userId = 1L;
        Integer userType = UserTypeEnum.ADMIN.getValue();

        // 调用
        int updateCount = notifyMessageService.updateAllNotifyMessageRead(userId, userType);
        // 断言
        assertEquals(1, updateCount);
        NotifyMessageDO notifyMessage = notifyMessageMapper.selectById(dbNotifyMessage.getId());
        assertTrue(notifyMessage.getReadStatus());
        assertNotNull(notifyMessage.getReadTime());
    }

    private static Map<String, Object> randomTemplateParams() {
        return MapUtil.<String, Object>builder().put(randomString(), randomString())
                .put(randomString(), randomString()).build();
    }

}
