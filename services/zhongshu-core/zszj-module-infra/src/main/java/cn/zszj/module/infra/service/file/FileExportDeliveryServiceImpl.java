package cn.zszj.module.infra.service.file;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.zszj.framework.datapermission.core.authorize.ObjectAuthorizationRequest;
import cn.zszj.framework.datapermission.core.authorize.ObjectAuthorizationRespDTO;
import cn.zszj.framework.datapermission.core.authorize.ObjectAuthorizationService;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileExportGenerateReqVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileExportGenerateRespVO;
import cn.zszj.module.infra.framework.file.config.FileExportProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

import static cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants.FORBIDDEN;
import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_EXPORT_FORBIDDEN;

/**
 * 导出生成通道 Service 实现（ZS-FILE-004.B）。
 *
 * <p>流程（每次调用实时裁决、无缓存——撤权/组织状态变更后立即生效）：
 * <ol>
 *     <li>接入契约：登录主体必填 + 对象类型非空白（导出通道声明）；裁决服务未装配 fail-closed
 *     （{@link #objectAuthorizationService}，循 {@link FileServiceImpl} org 门先例）；</li>
 *     <li>统一裁决（ZS-PERM-003.A）：{@link ObjectAuthorizationService#authorize}——未注册 provider
 *     返回 {@code null} → FORBIDDEN（未接入域继续走既有同步导出，零变化）；authorize 内部完成
 *     对象维（非 visit org 轴 checker / visit 经 visit scope 收敛；不可见即 FORBIDDEN）——
 *     即「业务组织导出重检」落点；</li>
 *     <li>字段维拒绝路径：声明敏感字段非空时必须 ⊆ {@code authorizedFields}（未启用字段级输出
 *     〔null〕+ 非空声明 fail-closed；超出授权字段拒绝）——即「敏感字段导出重检」落点；
 *     通过后裁决输出 {@code authorizedFields} 供调用方裁剪（裁剪=消费输出生成内容，D5 口径）；</li>
 *     <li>落盘：私有导出件（owner=登录主体、organizationId=<b>源对象组织</b>、purpose='export'、
 *     保留期=now+retention-days）。organizationId 取源对象组织而非登录用户组织——生成后转岗/离任/
 *     组织停用时，交付链（issue/redeem/chunk 逐环节 validateFileReadable）org 门精确收敛，
 *     构成「生成/交付间撤权不泄露」的结构保证（FND-AUTH-004：本人所有权不凌驾组织排除）。</li>
 * </ol>
 *
 * @author ZS-FILE-004.B
 */
@Slf4j
@Service
@Validated
public class FileExportDeliveryServiceImpl implements FileExportDeliveryService {

    @Resource
    private FileService fileService;

    @Resource
    private FileExportProperties exportProperties;

    /**
     * ZS-PERM-003.A 统一裁决服务：{@code required=false}——未装配 biz-data-permission 的窄上下文
     * fail-closed 拒绝（导出生成不经裁决不放行）。
     */
    @Autowired(required = false)
    private ObjectAuthorizationService objectAuthorizationService;

    @Override
    public FileExportGenerateRespVO generateExportFile(FileExportGenerateReqVO req, LoginUser loginUser) {
        // 1. 接入契约：登录主体必须存在；对象类型为导出通道接入声明，不得空白
        if (loginUser == null || req == null || StrUtil.isBlank(req.getObjectType())) {
            log.warn("[generateExportFile][导出生成拒绝：登录主体缺失或对象类型空白]");
            throw exception(FILE_EXPORT_FORBIDDEN);
        }
        // 2. 机制可用性：统一裁决服务未装配 fail-closed（宁可拒绝，不绕过重检落盘）
        if (objectAuthorizationService == null) {
            log.warn("[generateExportFile][导出生成拒绝：统一裁决服务未装配，fail-closed]");
            throw exception(FILE_EXPORT_FORBIDDEN);
        }
        // 3. 统一裁决：未注册对象类型=未接入，走既有同步导出通道，不在本通道（新通道保守，零变化）
        ObjectAuthorizationRespDTO auth = objectAuthorizationService.authorize(ObjectAuthorizationRequest.of(
                req.getObjectType(), req.getOrgId(), req.getOwnerUserId(), req.getObject()));
        if (auth == null) {
            log.warn("[generateExportFile][登录用户({}) 对象类型({}) 未接入统一裁决，拒绝生成]",
                    loginUser.getId(), req.getObjectType());
            throw exception(FORBIDDEN);
        }
        // 4. 字段维拒绝路径：声明字段必须 ⊆ 授权字段（未启用字段级输出 + 非空声明 fail-closed）。
        //    空字段声明不校验字段维；对象维已在 authorize 内完成——不因空字段声明跳过对象维
        if (CollUtil.isNotEmpty(req.getFields())
                && (auth.getAuthorizedFields() == null || !auth.getAuthorizedFields().containsAll(req.getFields()))) {
            log.warn("[generateExportFile][登录用户({}) 对象类型({}) 声明字段超出授权字段，拒绝生成]",
                    loginUser.getId(), req.getObjectType());
            throw exception(FORBIDDEN);
        }
        // 5. 落盘私有导出件（org 继承源对象组织=生成/交付间撤权载体；保留期=生成时刻+配置天数）
        LocalDateTime retentionExpireTime = LocalDateTime.now().plusDays(exportProperties.getRetentionDays());
        Long fileId = fileService.createExportFile(req.getContent(), req.getName(), req.getType(),
                loginUser.getId(), req.getOrgId(), retentionExpireTime);

        FileExportGenerateRespVO resp = new FileExportGenerateRespVO();
        resp.setFileId(fileId);
        resp.setRetentionExpireTime(retentionExpireTime);
        log.info("[generateExportFile][登录用户({}) 对象类型({}) 导出件({}) 已生成（私有，保留期至 {}）]",
                loginUser.getId(), req.getObjectType(), fileId, retentionExpireTime);
        return resp;
    }

}
