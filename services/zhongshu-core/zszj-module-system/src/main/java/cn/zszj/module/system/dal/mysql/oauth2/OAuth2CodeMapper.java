package cn.zszj.module.system.dal.mysql.oauth2;

import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2CodeDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OAuth2CodeMapper extends BaseMapperX<OAuth2CodeDO> {

    default OAuth2CodeDO selectByCode(String code) {
        return selectOne(OAuth2CodeDO::getCode, code);
    }

    /**
     * ZS-LOGIN-003 codex r1 P1：删除指定用户的全部未消费授权码（用户被禁用/删除/改密时，
     * 残留的 code 在有效期内仍可兑换出新会话，须随生命周期撤销一并失效）。
     *
     * @return 删除的授权码数量
     */
    default int deleteByUserIdAndUserType(Long userId, Integer userType) {
        return delete(new LambdaQueryWrapperX<OAuth2CodeDO>()
                .eq(OAuth2CodeDO::getUserId, userId)
                .eq(OAuth2CodeDO::getUserType, userType));
    }

}
