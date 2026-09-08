package cn.zszj.module.hrm.service.recruit.post;

import cn.hutool.core.collection.CollUtil;
import cn.zszj.module.hrm.dal.dataobject.recruit.post.HrmRecruitPostTypeDO;
import cn.zszj.module.hrm.dal.mysql.recruit.post.HrmRecruitPostTypeMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.hrm.enums.ErrorCodeConstants.RECRUIT_POST_TYPE_NOT_EXISTS;

/**
 * 招聘职位类型 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class HrmRecruitPostTypeServiceImpl implements HrmRecruitPostTypeService {

    @Resource
    private HrmRecruitPostTypeMapper recruitPostTypeMapper;

    @Override
    public void validateRecruitPostTypeExists(Long id) {
        if (id == null) {
            return;
        }
        if (recruitPostTypeMapper.selectById(id) == null) {
            throw exception(RECRUIT_POST_TYPE_NOT_EXISTS);
        }
    }

    @Override
    public List<HrmRecruitPostTypeDO> getRecruitPostTypeList(Integer status) {
        return recruitPostTypeMapper.selectListByStatus(status);
    }

    @Override
    public List<HrmRecruitPostTypeDO> getRecruitPostTypeListByIds(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return recruitPostTypeMapper.selectByIds(ids);
    }

}
