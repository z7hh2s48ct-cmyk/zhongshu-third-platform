package cn.zszj.module.system.service.oauth2;

import cn.hutool.core.util.RandomUtil;
import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.common.util.date.DateUtils;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2CodeDO;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2CodeMapper;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

import static cn.zszj.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zszj.framework.test.core.util.RandomUtils.*;
import static cn.zszj.module.system.enums.ErrorCodeConstants.OAUTH2_CODE_EXPIRE;
import static cn.zszj.module.system.enums.ErrorCodeConstants.OAUTH2_CODE_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link OAuth2CodeServiceImpl} 的单元测试类
 *
 * @author 芋道源码
 */
@Import(OAuth2CodeServiceImpl.class)
class OAuth2CodeServiceImplTest extends BaseDbUnitTest {

    @Resource
    private OAuth2CodeServiceImpl oauth2CodeService;

    @Resource
    private OAuth2CodeMapper oauth2CodeMapper;

    @Test
    public void testCreateAuthorizationCode() {
        // 准备参数
        Long userId = randomLongId();
        Integer userType = RandomUtil.randomEle(UserTypeEnum.values()).getValue();
        String clientId = randomString();
        List<String> scopes = Lists.newArrayList("read", "write");
        String redirectUri = randomString();
        String state = randomString();

        // 调用
        OAuth2CodeDO codeDO = oauth2CodeService.createAuthorizationCode(userId, userType, clientId,
                scopes, redirectUri, state);
        // 断言
        OAuth2CodeDO dbCodeDO = oauth2CodeMapper.selectByCode(codeDO.getCode());
        // TODO @芋艿：expiresTime 被屏蔽，仅 win11 会复现，建议后续修复。
        assertPojoEquals(codeDO, dbCodeDO, "expiresTime", "createTime", "updateTime", "deleted");
        assertEquals(userId, codeDO.getUserId());
        assertEquals(userType, codeDO.getUserType());
        assertEquals(clientId, codeDO.getClientId());
        assertEquals(scopes, codeDO.getScopes());
        assertEquals(redirectUri, codeDO.getRedirectUri());
        assertEquals(state, codeDO.getState());
        assertFalse(DateUtils.isExpired(codeDO.getExpiresTime()));
    }

    @Test
    public void testConsumeAuthorizationCode_doubleConsume_secondRejects() {
        // ZS-LOGIN-003 codex r3 P1：同一 code 只能消费一次——条件删除恰好影响 1 行才放行
        Long userId = randomLongId();
        OAuth2CodeDO codeDO = oauth2CodeService.createAuthorizationCode(userId,
                UserTypeEnum.ADMIN.getValue(), randomString(), Lists.newArrayList("read"),
                randomString(), randomString());

        // 第一次消费成功
        oauth2CodeService.consumeAuthorizationCode(codeDO.getCode());
        // 第二次消费必须拒绝（同 code 并发重复兑换防线）
        assertServiceException(() -> oauth2CodeService.consumeAuthorizationCode(codeDO.getCode()),
                OAUTH2_CODE_NOT_EXISTS);
    }

    @Test
    public void testConsumeAuthorizationCode_revokedByLifecycle_secondRejects() {
        // ZS-LOGIN-003 codex r3 P1：code 已被生命周期撤销（用户级撤销 deleteByUserIdAndUserType 删除）后，
        // 兑换必须拒绝——防「兑换读 code → 撤销删 code 并提交 → 兑换继续建令牌」复活会话
        Long userId = randomLongId();
        OAuth2CodeDO codeDO = oauth2CodeService.createAuthorizationCode(userId,
                UserTypeEnum.ADMIN.getValue(), randomString(), Lists.newArrayList("read"),
                randomString(), randomString());
        // 模拟生命周期撤销：直接删除该用户的授权码
        oauth2CodeMapper.deleteByUserIdAndUserType(userId, UserTypeEnum.ADMIN.getValue());

        assertServiceException(() -> oauth2CodeService.consumeAuthorizationCode(codeDO.getCode()),
                OAUTH2_CODE_NOT_EXISTS);
    }

    @Test
    public void testConsumeAuthorizationCode_null() {
        // 调用，并断言
        assertServiceException(() -> oauth2CodeService.consumeAuthorizationCode(randomString()),
                OAUTH2_CODE_NOT_EXISTS);
    }

    @Test
    public void testConsumeAuthorizationCode_expired() {
        // 准备参数
        String code = "test_code";
        // mock 数据
        OAuth2CodeDO codeDO = randomPojo(OAuth2CodeDO.class).setCode(code)
                .setExpiresTime(LocalDateTime.now().minusDays(1));
        oauth2CodeMapper.insert(codeDO);

        // 调用，并断言
        assertServiceException(() -> oauth2CodeService.consumeAuthorizationCode(code),
                OAUTH2_CODE_EXPIRE);
    }

    @Test
    public void testConsumeAuthorizationCode_success() {
        // 准备参数
        String code = "test_code";
        // mock 数据
        OAuth2CodeDO codeDO = randomPojo(OAuth2CodeDO.class).setCode(code)
                .setExpiresTime(LocalDateTime.now().plusDays(1));
        oauth2CodeMapper.insert(codeDO);

        // 调用
        OAuth2CodeDO result = oauth2CodeService.consumeAuthorizationCode(code);
        // TODO @芋艿：expiresTime 被屏蔽，仅 win11 会复现，建议后续修复。
        assertPojoEquals(codeDO, result, "expiresTime");
        assertNull(oauth2CodeMapper.selectByCode(code));
    }

}
