package cn.zszj.module.system.dal.mysql.permission;

import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.module.system.dal.dataobject.permission.CrossOrgVisitGrantDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 跨组织访问授权记录 Mapper（ZS-SEC-001.B）。
 *
 * <p>{@link CrossOrgVisitGrantDO} 为 {@code @TenantIgnore} 平台级跨租户表，查询不受 MP 租户插件自动注入的
 * tenant_id 过滤；逻辑删除（{@code deleted = 0}）由 MP 全局逻辑删除自动追加。
 *
 * @author ZS-SEC-001.B
 */
@Mapper
public interface CrossOrgVisitGrantMapper extends BaseMapperX<CrossOrgVisitGrantDO> {

    /**
     * 取 (原主体, 目标租户) 的最新一条授权记录（未删除，含 ACTIVE/REVOKED），按 id 倒序取首条。
     *
     * <p>判定序在 service 层区分 {@code REVOKED}（GRANT_REVOKED）/过期（GRANT_EXPIRED）/无记录（NO_GRANT），
     * 故此处<b>不</b>预过滤 status——否则已撤销记录会被误报为「无记录」，丢失 line331「授权撤销有效」的精确留痕。
     *
     * @param visitorUserId  原主体账号编号
     * @param targetTenantId 目标租户编号
     * @return 最新授权记录；不存在返回 {@code null}
     */
    default CrossOrgVisitGrantDO selectLatestByVisitorAndTarget(Long visitorUserId, Long targetTenantId) {
        return selectOne(new LambdaQueryWrapperX<CrossOrgVisitGrantDO>()
                .eq(CrossOrgVisitGrantDO::getVisitorUserId, visitorUserId)
                .eq(CrossOrgVisitGrantDO::getTargetTenantId, targetTenantId)
                .orderByDesc(CrossOrgVisitGrantDO::getId)
                .last("LIMIT 1"));
    }

}
