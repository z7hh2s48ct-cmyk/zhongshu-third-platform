package cn.zszj.module.member.dal.mysql.config;

import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.module.member.dal.dataobject.config.MemberConfigDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 积分设置 Mapper
 *
 * @author QingX
 */
@Mapper
public interface MemberConfigMapper extends BaseMapperX<MemberConfigDO> {
}
