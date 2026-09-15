package cn.zszj.module.system.controller.admin.oauth2;

import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.common.util.object.BeanUtils;
import cn.zszj.module.system.controller.admin.oauth2.vo.token.OAuth2AccessTokenPageReqVO;
import cn.zszj.module.system.controller.admin.oauth2.vo.token.OAuth2AccessTokenRespVO;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.zszj.module.system.enums.logger.LoginLogTypeEnum;
import cn.zszj.module.system.service.auth.AdminAuthService;
import cn.zszj.module.system.service.oauth2.OAuth2TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.zszj.framework.common.pojo.CommonResult.success;
import static cn.zszj.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - OAuth2.0 令牌")
@RestController
@RequestMapping("/system/oauth2-token")
public class OAuth2TokenController {

    @Resource
    private OAuth2TokenService oauth2TokenService;
    @Resource
    private AdminAuthService authService;

    @GetMapping("/page")
    @Operation(summary = "获得访问令牌分页", description = "只返回有效期内的")
    @PreAuthorize("@ss.hasPermission('system:oauth2-token:page')")
    public CommonResult<PageResult<OAuth2AccessTokenRespVO>> getAccessTokenPage(@Valid OAuth2AccessTokenPageReqVO reqVO) {
        PageResult<OAuth2AccessTokenDO> pageResult = oauth2TokenService.getAccessTokenPage(reqVO);
        return success(BeanUtils.toBean(pageResult, OAuth2AccessTokenRespVO.class));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除访问令牌（强制踢出会话）", description = "以不可用于认证的会话 ID 标识，无需持有令牌串")
    @Parameter(name = "id", description = "会话编号（不可用于认证）", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:oauth2-token:delete')")
    public CommonResult<Boolean> deleteAccessToken(@RequestParam("id") Long id) {
        authService.logoutById(id, null, LoginLogTypeEnum.LOGOUT_DELETE.getType());
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除访问令牌（强制踢出会话）")
    @Parameter(name = "ids", description = "会话编号数组（不可用于认证）", required = true)
    @PreAuthorize("@ss.hasPermission('system:oauth2-token:delete')")
    public CommonResult<Boolean> deleteAccessTokenList(@RequestParam("ids") List<Long> ids) {
        ids.forEach(id -> authService.logoutById(id, null, LoginLogTypeEnum.LOGOUT_DELETE.getType()));
        return success(true);
    }

    // ========== 自助会话管理（ZS-LOGIN-006：普通用户仅能列出 / 撤销本人会话，不暴露凭据）==========

    @GetMapping("/my-page")
    @Operation(summary = "获得我的访问令牌分页", description = "只返回本人、有效期内的会话；不回显令牌秘密")
    public CommonResult<PageResult<OAuth2AccessTokenRespVO>> getMyAccessTokenPage(@Valid OAuth2AccessTokenPageReqVO reqVO) {
        // 强制以当前登录用户作用域，杜绝越权列出他人会话（忽略入参中的 userId / userType）
        reqVO.setUserId(getLoginUserId());
        reqVO.setUserType(UserTypeEnum.ADMIN.getValue());
        PageResult<OAuth2AccessTokenDO> pageResult = oauth2TokenService.getAccessTokenPage(reqVO);
        return success(BeanUtils.toBean(pageResult, OAuth2AccessTokenRespVO.class));
    }

    @DeleteMapping("/revoke-my")
    @Operation(summary = "撤销我的会话", description = "仅能撤销本人会话；以不可用于认证的会话 ID 标识")
    @Parameter(name = "id", description = "会话编号（不可用于认证）", required = true, example = "1024")
    public CommonResult<Boolean> revokeMyAccessToken(@RequestParam("id") Long id) {
        // expectedUserId 传当前登录用户，service 校验归属，非本人抛 OAUTH2_TOKEN_SESSION_NOT_OWNED
        authService.logoutById(id, getLoginUserId(), LoginLogTypeEnum.LOGOUT_SELF.getType());
        return success(true);
    }

}
