package cn.zszj.module.mes.dal.mysql.dv.subject;

import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.module.mes.controller.admin.dv.subject.vo.MesDvSubjectPageReqVO;
import cn.zszj.module.mes.dal.dataobject.dv.subject.MesDvSubjectDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesDvSubjectMapper extends BaseMapperX<MesDvSubjectDO> {

    default PageResult<MesDvSubjectDO> selectPage(MesDvSubjectPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesDvSubjectDO>()
                .likeIfPresent(MesDvSubjectDO::getCode, reqVO.getCode())
                .likeIfPresent(MesDvSubjectDO::getName, reqVO.getName())
                .eqIfPresent(MesDvSubjectDO::getType, reqVO.getType())
                .eqIfPresent(MesDvSubjectDO::getStatus, reqVO.getStatus())
                .orderByDesc(MesDvSubjectDO::getId));
    }

    default MesDvSubjectDO selectByCode(String code) {
        return selectOne(MesDvSubjectDO::getCode, code);
    }

}
