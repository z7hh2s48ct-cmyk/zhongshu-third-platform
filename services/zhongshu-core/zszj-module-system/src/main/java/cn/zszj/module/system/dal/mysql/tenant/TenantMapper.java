package cn.zszj.module.system.dal.mysql.tenant;

import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.framework.mybatis.core.util.MyBatisUtils;
import cn.zszj.module.system.controller.admin.tenant.vo.tenant.TenantPageReqVO;
import cn.zszj.module.system.dal.dataobject.tenant.TenantDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TenantMapper extends BaseMapperX<TenantDO> {

    /**
     * ZS-CFG-003.B codex r0 P1：按租户编号加行锁读取（SELECT ... FOR UPDATE）。
     * 作为「授权写入 ↔ 套餐变更收敛 ↔ 租户换套餐」的统一锁：堵住套餐校验到授权提交之间的 TOCTOU
     * （并发套餐收缩后仍能写入越界菜单）。
     *
     * @param id 租户编号
     * @return 租户；不存在时返回 {@code null}（不持有该行锁）
     */
    default TenantDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<TenantDO>()
                .eq(TenantDO::getId, id)
                .last("FOR UPDATE"));
    }


    default PageResult<TenantDO> selectPage(TenantPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TenantDO>()
                .likeIfPresent(TenantDO::getName, reqVO.getName())
                .likeIfPresent(TenantDO::getContactName, reqVO.getContactName())
                .likeIfPresent(TenantDO::getContactMobile, reqVO.getContactMobile())
                .eqIfPresent(TenantDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(TenantDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(TenantDO::getId));
    }

    default TenantDO selectByName(String name) {
        return selectOne(TenantDO::getName, name);
    }

    default List<TenantDO> selectListByWebsite(String website) {
        return selectList(new LambdaQueryWrapperX<TenantDO>()
                .apply(MyBatisUtils.findInSet("websites"), website));
    }

    default Long selectCountByPackageId(Long packageId) {
        return selectCount(TenantDO::getPackageId, packageId);
    }

    default List<TenantDO> selectListByPackageId(Long packageId) {
        return selectList(TenantDO::getPackageId, packageId);
    }

    default List<TenantDO> selectListByStatus(Integer status) {
        return selectList(TenantDO::getStatus, status);
    }

}
