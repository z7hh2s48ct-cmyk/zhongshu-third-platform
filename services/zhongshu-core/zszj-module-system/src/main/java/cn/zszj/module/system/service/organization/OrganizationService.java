package cn.zszj.module.system.service.organization;

import cn.zszj.module.system.dal.dataobject.organization.OrganizationDO;

import java.util.List;

/**
 * 组织 Service 接口
 *
 * <p>ZS-IAM-002（D-09 FND-IAM-002）：组织作为独立业务对象的创建、查询、层级维护与停用校验。
 *
 * @author ZS-IAM-002
 */
public interface OrganizationService {

    /**
     * 创建组织
     *
     * @param organization 组织
     * @return 组织编号
     */
    Long createOrganization(OrganizationDO organization);

    /**
     * 更新组织
     *
     * @param organization 组织
     */
    void updateOrganization(OrganizationDO organization);

    /**
     * 删除组织（存在子组织或任职成员时拒绝）
     *
     * @param id 组织编号
     */
    void deleteOrganization(Long id);

    /**
     * 获得组织
     *
     * @param id 组织编号
     * @return 组织
     */
    OrganizationDO getOrganization(Long id);

    /**
     * 按类型获得组织列表
     *
     * @param type 组织类型
     * @return 组织列表
     */
    List<OrganizationDO> getOrganizationListByType(Integer type);

    /**
     * 校验组织存在且处于开启状态
     *
     * @param id 组织编号
     */
    void validateOrganizationEnabled(Long id);

    /**
     * 校验组织存在且处于开启状态，并对组织行加锁（SELECT ... FOR UPDATE）。
     *
     * <p>ZS-IAM-002（codex r1 P2）：供任职创建 / 转岗等「引用组织」的写路径使用——锁行与组织删除串行化，
     * 杜绝并发下「删除见零引用后本事务再插入引用」留下指向已删组织的孤引用。须在 {@code @Transactional} 内调用。
     *
     * @param id 组织编号
     */
    void validateOrganizationEnabledForUpdate(Long id);

}
