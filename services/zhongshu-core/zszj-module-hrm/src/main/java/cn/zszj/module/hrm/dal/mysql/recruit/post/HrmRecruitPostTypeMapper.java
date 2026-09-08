package cn.zszj.module.hrm.dal.mysql.recruit.post;

import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.module.hrm.dal.dataobject.recruit.post.HrmRecruitPostTypeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface HrmRecruitPostTypeMapper extends BaseMapperX<HrmRecruitPostTypeDO> {

    default List<HrmRecruitPostTypeDO> selectListByStatus(Integer status) {
        return selectList(new LambdaQueryWrapperX<HrmRecruitPostTypeDO>()
                .eqIfPresent(HrmRecruitPostTypeDO::getStatus, status)
                .orderByAsc(HrmRecruitPostTypeDO::getSort)
                .orderByAsc(HrmRecruitPostTypeDO::getId));
    }

}
