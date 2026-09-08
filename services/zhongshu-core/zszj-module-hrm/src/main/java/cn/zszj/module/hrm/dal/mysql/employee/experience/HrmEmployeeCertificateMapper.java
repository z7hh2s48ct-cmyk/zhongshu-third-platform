package cn.zszj.module.hrm.dal.mysql.employee.experience;

import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.module.hrm.dal.dataobject.employee.experience.HrmEmployeeCertificateDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface HrmEmployeeCertificateMapper extends BaseMapperX<HrmEmployeeCertificateDO> {

    default List<HrmEmployeeCertificateDO> selectListByEmployeeId(Long employeeId) {
        return selectList(new LambdaQueryWrapperX<HrmEmployeeCertificateDO>()
                .eq(HrmEmployeeCertificateDO::getEmployeeId, employeeId)
                .orderByAsc(HrmEmployeeCertificateDO::getSort)
                .orderByDesc(HrmEmployeeCertificateDO::getId));
    }

}
