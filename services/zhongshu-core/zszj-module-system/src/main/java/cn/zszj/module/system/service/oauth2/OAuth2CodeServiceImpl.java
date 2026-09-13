package cn.zszj.module.system.service.oauth2;

import cn.hutool.core.util.IdUtil;
import cn.zszj.framework.common.util.date.DateUtils;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2CodeDO;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2CodeMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.util.List;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.system.enums.ErrorCodeConstants.OAUTH2_CODE_EXPIRE;
import static cn.zszj.module.system.enums.ErrorCodeConstants.OAUTH2_CODE_NOT_EXISTS;

/**
 * OAuth2.0 授权码 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class OAuth2CodeServiceImpl implements OAuth2CodeService {

    /**
     * 授权码的过期时间，默认 5 分钟
     */
    private static final Integer TIMEOUT = 5 * 60;

    @Resource
    private OAuth2CodeMapper oauth2CodeMapper;

    @Override
    public OAuth2CodeDO createAuthorizationCode(Long userId, Integer userType, String clientId,
                                                List<String> scopes, String redirectUri, String state) {
        OAuth2CodeDO codeDO = new OAuth2CodeDO().setCode(generateCode())
                .setUserId(userId).setUserType(userType)
                .setClientId(clientId).setScopes(scopes)
                .setExpiresTime(DateUtils.now().plusSeconds(TIMEOUT))
                .setRedirectUri(redirectUri).setState(state);
        oauth2CodeMapper.insert(codeDO);
        return codeDO;
    }

    @Override
    public OAuth2CodeDO consumeAuthorizationCode(String code) {
        OAuth2CodeDO codeDO = oauth2CodeMapper.selectByCode(code);
        if (codeDO == null) {
            throw exception(OAUTH2_CODE_NOT_EXISTS);
        }
        if (DateUtils.isExpired(codeDO.getExpiresTime())) {
            throw exception(OAUTH2_CODE_EXPIRE);
        }
        // ZS-LOGIN-003 codex r3 P1：条件删除且必须恰好影响 1 行——并发重复消费、或该 code 已被
        // 生命周期撤销（removeAccessToken(userId,userType)）删除时，本方删除影响 0 行，必须拒绝兑换：
        // 否则「兑换读 code → 撤销删 code 并提交 → 兑换继续建令牌」会在撤销完成后复活会话，
        // 同一 code 也可被并发重复兑换
        int deleted = oauth2CodeMapper.deleteById(codeDO.getId());
        if (deleted != 1) {
            throw exception(OAUTH2_CODE_NOT_EXISTS);
        }
        return codeDO;
    }

    private static String generateCode() {
        return IdUtil.fastSimpleUUID();
    }

}
