package cn.zszj.framework.datapermission.core.authorize;

import lombok.Data;

/**
 * 对象授权裁决请求（ZS-PERM-003.A）。
 *
 * <p>承载裁决所需的上下文：对象类型（路由扩展点）、对象所属组织与负责人（对象维）、业务对象实例
 * （状态维钩子入参）。不承载用户身份——登录主体始终来自服务端安全上下文。
 *
 * @author ZS-PERM-003.A
 */
@Data
public class ObjectAuthorizationRequest {

    /**
     * 对象类型标识，路由到对应 {@link ObjectAuthorizationProvider}
     */
    private String objectType;

    /**
     * 对象所属组织编号，可为 null（无组织列对象）
     */
    private Long orgId;

    /**
     * 对象负责人（创建人/归属人）用户编号，可为 null
     */
    private Long ownerUserId;

    /**
     * 业务对象实例（可为 null；非 null 时供状态维钩子判定）
     */
    private Object object;

    public static ObjectAuthorizationRequest of(String objectType, Long orgId, Long ownerUserId) {
        return of(objectType, orgId, ownerUserId, null);
    }

    public static ObjectAuthorizationRequest of(String objectType, Long orgId, Long ownerUserId, Object object) {
        ObjectAuthorizationRequest request = new ObjectAuthorizationRequest();
        request.setObjectType(objectType);
        request.setOrgId(orgId);
        request.setOwnerUserId(ownerUserId);
        request.setObject(object);
        return request;
    }

}
