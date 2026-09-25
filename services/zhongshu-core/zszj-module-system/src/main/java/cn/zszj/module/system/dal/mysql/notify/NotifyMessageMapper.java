package cn.zszj.module.system.dal.mysql.notify;

import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.framework.mybatis.core.query.QueryWrapperX;
import cn.zszj.module.system.controller.admin.notify.vo.message.NotifyMessageMyPageReqVO;
import cn.zszj.module.system.controller.admin.notify.vo.message.NotifyMessagePageReqVO;
import cn.zszj.module.system.dal.dataobject.notify.NotifyMessageDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Mapper
public interface NotifyMessageMapper extends BaseMapperX<NotifyMessageDO> {

    /**
     * org 轴查询范围（ZS-MSG-003.C）：服务层解析、Mapper 统一转 SQL 条件，保证列表/未读/计数过滤口径一致。
     *
     * <p>语义（与 {@code OrgDataPermissionChecker} 对象判据同构，fail-closed）：
     * <ul>
     *   <li>{@link #visibleOrgIds()} 为 {@code null}：不受限（无登录 / 非 ADMIN / 全部组织 /
     *       visit whole-tenant），不追加任何条件；</li>
     *   <li>非空：命中组织集合；{@link #includeNullOrg()} 为 true 时并集无组织列消息（本人兜底）；</li>
     *   <li>空集：仅 {@link #includeNullOrg()} 为 true 时放行无组织列消息，否则恒假（fail-closed）。</li>
     * </ul>
     *
     * @param visibleOrgIds 授权可见组织编号集合；{@code null} = 不受限
     * @param includeNullOrg 是否放行无组织列（organization_id IS NULL）消息：「我的」读路径为 true
     *                       （本人历史/无任职消息兜底）；管理读路径恒为 false（他人无组织列消息不出现在管理视图）
     */
    record OrgScope(Collection<Long> visibleOrgIds, boolean includeNullOrg) {

        /** 不受限范围（不追加 org 条件）。 */
        public static OrgScope unrestricted() {
            return new OrgScope(null, false);
        }

        /** 按授权组织集合收敛的受限范围。 */
        public static OrgScope of(Collection<Long> visibleOrgIds, boolean includeNullOrg) {
            return new OrgScope(visibleOrgIds, includeNullOrg);
        }

    }

    default PageResult<NotifyMessageDO> selectPage(NotifyMessagePageReqVO reqVO, OrgScope orgScope) {
        LambdaQueryWrapperX<NotifyMessageDO> wrapper = new LambdaQueryWrapperX<NotifyMessageDO>()
                .eqIfPresent(NotifyMessageDO::getUserId, reqVO.getUserId())
                .eqIfPresent(NotifyMessageDO::getUserType, reqVO.getUserType())
                .likeIfPresent(NotifyMessageDO::getTemplateCode, reqVO.getTemplateCode())
                .eqIfPresent(NotifyMessageDO::getTemplateType, reqVO.getTemplateType())
                .betweenIfPresent(NotifyMessageDO::getCreateTime, reqVO.getCreateTime());
        applyOrgScope(wrapper, orgScope);
        return selectPage(reqVO, wrapper.orderByDesc(NotifyMessageDO::getId));
    }

    default PageResult<NotifyMessageDO> selectPage(NotifyMessageMyPageReqVO reqVO, Long userId, Integer userType, OrgScope orgScope) {
        LambdaQueryWrapperX<NotifyMessageDO> wrapper = new LambdaQueryWrapperX<NotifyMessageDO>()
                .eqIfPresent(NotifyMessageDO::getReadStatus, reqVO.getReadStatus())
                .betweenIfPresent(NotifyMessageDO::getCreateTime, reqVO.getCreateTime())
                .eq(NotifyMessageDO::getUserId, userId)
                .eq(NotifyMessageDO::getUserType, userType);
        applyOrgScope(wrapper, orgScope);
        return selectPage(reqVO, wrapper.orderByDesc(NotifyMessageDO::getId));
    }

    default int updateListRead(Collection<Long> ids, Long userId, Integer userType) {
        return update(new NotifyMessageDO().setReadStatus(true).setReadTime(LocalDateTime.now()),
                new LambdaQueryWrapperX<NotifyMessageDO>()
                        .in(NotifyMessageDO::getId, ids)
                        .eq(NotifyMessageDO::getUserId, userId)
                        .eq(NotifyMessageDO::getUserType, userType)
                        .eq(NotifyMessageDO::getReadStatus, false));
    }

    default int updateListRead(Long userId, Integer userType) {
        return update(new NotifyMessageDO().setReadStatus(true).setReadTime(LocalDateTime.now()),
                new LambdaQueryWrapperX<NotifyMessageDO>()
                        .eq(NotifyMessageDO::getUserId, userId)
                        .eq(NotifyMessageDO::getUserType, userType)
                        .eq(NotifyMessageDO::getReadStatus, false));
    }

    default List<NotifyMessageDO> selectUnreadListByUserIdAndUserType(Long userId, Integer userType, Integer size, OrgScope orgScope) {
        QueryWrapperX<NotifyMessageDO> wrapper = new QueryWrapperX<NotifyMessageDO>() // 由于要使用 limitN 语句，所以只能用 QueryWrapperX
                .eq("user_id", userId)
                .eq("user_type", userType)
                .eq("read_status", false);
        applyOrgScope(wrapper, orgScope); // 必须在 limitN 之前：limitN 经 last 追加到 SQL 末尾
        return selectList(wrapper.orderByDesc("id").limitN(size));
    }

    default Long selectUnreadCountByUserIdAndUserType(Long userId, Integer userType, OrgScope orgScope) {
        LambdaQueryWrapperX<NotifyMessageDO> wrapper = new LambdaQueryWrapperX<NotifyMessageDO>()
                .eq(NotifyMessageDO::getReadStatus, false)
                .eq(NotifyMessageDO::getUserId, userId)
                .eq(NotifyMessageDO::getUserType, userType);
        applyOrgScope(wrapper, orgScope);
        return selectCount(wrapper);
    }

    /**
     * 追加 org 轴条件（Lambda 列版）。
     *
     * <p>{@code visibleOrgIds} 为空集时不得退化为「无过滤」（in 空集会拼 {@code IN ()} 语义异常）：
     * includeNullOrg=true → 仅 {@code organization_id IS NULL}；false → {@code 1 = 0} 恒假。
     */
    default void applyOrgScope(LambdaQueryWrapperX<NotifyMessageDO> wrapper, OrgScope orgScope) {
        if (orgScope == null || orgScope.visibleOrgIds() == null) {
            return; // 不受限
        }
        if (orgScope.visibleOrgIds().isEmpty()) {
            if (orgScope.includeNullOrg()) {
                wrapper.isNull(NotifyMessageDO::getOrganizationId);
            } else {
                wrapper.apply("1 = 0");
            }
            return;
        }
        if (orgScope.includeNullOrg()) {
            wrapper.and(w -> w.in(NotifyMessageDO::getOrganizationId, orgScope.visibleOrgIds())
                    .or().isNull(NotifyMessageDO::getOrganizationId));
        } else {
            wrapper.in(NotifyMessageDO::getOrganizationId, orgScope.visibleOrgIds());
        }
    }

    /**
     * 追加 org 轴条件（字符串列版，供 limitN 场景的 {@link QueryWrapperX}；语义同 Lambda 版）。
     */
    default void applyOrgScope(QueryWrapperX<NotifyMessageDO> wrapper, OrgScope orgScope) {
        if (orgScope == null || orgScope.visibleOrgIds() == null) {
            return; // 不受限
        }
        if (orgScope.visibleOrgIds().isEmpty()) {
            if (orgScope.includeNullOrg()) {
                wrapper.isNull("organization_id");
            } else {
                wrapper.apply("1 = 0");
            }
            return;
        }
        if (orgScope.includeNullOrg()) {
            wrapper.and(w -> w.in("organization_id", orgScope.visibleOrgIds())
                    .or().isNull("organization_id"));
        } else {
            wrapper.in("organization_id", orgScope.visibleOrgIds());
        }
    }

}
