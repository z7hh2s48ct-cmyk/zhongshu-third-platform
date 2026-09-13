package cn.zszj.module.system.dal.mysql.tenant;

import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.module.system.controller.admin.tenant.vo.packages.TenantPackagePageReqVO;
import cn.zszj.module.system.dal.dataobject.tenant.TenantPackageDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TenantPackageMapper extends BaseMapperX<TenantPackageDO> {
    /**
     * ZS-CFG-003.B codex r1 P1：按套餐编号加行锁读取（SELECT ... FOR UPDATE）。
     * 套餐→租户统一锁序的起点：换套餐/创建租户先锁目标套餐行，再动租户绑定与角色授权，
     * 与套餐收缩流程串行化。
     */
    default TenantPackageDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<TenantPackageDO>()
                .eq(TenantPackageDO::getId, id)
                .last("FOR UPDATE"));
    }


    default PageResult<TenantPackageDO> selectPage(TenantPackagePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TenantPackageDO>()
                .likeIfPresent(TenantPackageDO::getName, reqVO.getName())
                .eqIfPresent(TenantPackageDO::getStatus, reqVO.getStatus())
                .likeIfPresent(TenantPackageDO::getRemark, reqVO.getRemark())
                .betweenIfPresent(TenantPackageDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(TenantPackageDO::getId));
    }

    default List<TenantPackageDO> selectListByStatus(Integer status) {
        return selectList(TenantPackageDO::getStatus, status);
    }

    default TenantPackageDO selectByName(String name) {
        return selectOne(TenantPackageDO::getName, name);
    }
}
