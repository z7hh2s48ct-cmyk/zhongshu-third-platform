package cn.zszj.module.infra.controller.admin.config;

import cn.zszj.framework.apilog.core.annotation.ApiAccessLog;
import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.framework.common.pojo.PageParam;
import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.excel.core.util.ExcelUtils;
import cn.zszj.module.infra.controller.admin.config.vo.ConfigChangeHistoryPageReqVO;
import cn.zszj.module.infra.controller.admin.config.vo.ConfigChangeHistoryRespVO;
import cn.zszj.module.infra.controller.admin.config.vo.ConfigPageReqVO;
import cn.zszj.module.infra.controller.admin.config.vo.ConfigRestoreReqVO;
import cn.zszj.module.infra.controller.admin.config.vo.ConfigRespVO;
import cn.zszj.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.zszj.module.infra.dal.dataobject.config.ConfigDO;
import cn.zszj.module.infra.enums.ErrorCodeConstants;
import cn.zszj.module.infra.service.config.ConfigSensitiveClassifier;
import cn.zszj.module.infra.service.config.ConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static cn.zszj.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 参数配置")
@RestController
@RequestMapping("/infra/config")
@Validated
public class ConfigController {

    @Resource
    private ConfigService configService;

    @PostMapping("/create")
    @Operation(summary = "创建参数配置")
    @PreAuthorize("@ss.hasPermission('infra:config:create')")
    public CommonResult<Long> createConfig(@Valid @RequestBody ConfigSaveReqVO createReqVO) {
        return success(configService.createConfig(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改参数配置")
    @PreAuthorize("@ss.hasPermission('infra:config:update')")
    public CommonResult<Boolean> updateConfig(@Valid @RequestBody ConfigSaveReqVO updateReqVO) {
        configService.updateConfig(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除参数配置")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('infra:config:delete')")
    public CommonResult<Boolean> deleteConfig(@RequestParam("id") Long id) {
        configService.deleteConfig(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除参数配置")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @PreAuthorize("@ss.hasPermission('infra:config:delete')")
    public CommonResult<Boolean> deleteConfigList(@RequestParam("ids") List<Long> ids) {
        configService.deleteConfigList(ids);
        return success(true);
    }

    @GetMapping(value = "/get")
    @Operation(summary = "获得参数配置")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('infra:config:query')")
    public CommonResult<ConfigRespVO> getConfig(@RequestParam("id") Long id) {
        // ZS-CFG-001.B：详情输出统一脱敏（秘密/敏感项 value 掩码）
        return success(configService.getMaskedConfigRespVO(configService.getConfig(id)));
    }

    @GetMapping(value = "/get-value-by-key")
    @Operation(summary = "根据参数键名查询参数值", description = "不可见的配置，不允许返回给前端")
    @Parameter(name = "key", description = "参数键", required = true, example = "yunai.biz.username")
    public CommonResult<String> getConfigKey(@RequestParam("key") String key) {
        ConfigDO config = configService.getConfigByKey(key);
        if (config == null) {
            return success(null);
        }
        // ZS-CFG-001.B：即使被误标为可见，命中秘密键模式的参数仍拒绝返回（防“秘密键误标 visible”旁路）
        if (!config.getVisible()
                || configService.classifySensitive(config) == ConfigSensitiveClassifier.SensitiveLevel.SECRET) {
            throw exception(ErrorCodeConstants.CONFIG_GET_VALUE_ERROR_IF_VISIBLE);
        }
        return success(config.getValue());
    }

    @GetMapping("/page")
    @Operation(summary = "获取参数配置分页")
    @PreAuthorize("@ss.hasPermission('infra:config:query')")
    public CommonResult<PageResult<ConfigRespVO>> getConfigPage(@Valid ConfigPageReqVO pageReqVO) {
        PageResult<ConfigDO> page = configService.getConfigPage(pageReqVO);
        // ZS-CFG-001.B：分页输出逐行脱敏
        List<ConfigRespVO> list = page.getList().stream()
                .map(configService::getMaskedConfigRespVO).collect(Collectors.toList());
        return success(new PageResult<>(list, page.getTotal()));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出参数配置")
    @PreAuthorize("@ss.hasPermission('infra:config:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportConfig(@Validated ConfigPageReqVO exportReqVO,
                             HttpServletResponse response) throws IOException {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ConfigDO> list = configService.getConfigPage(exportReqVO).getList();
        // ZS-CFG-001.B：导出输出逐行脱敏
        List<ConfigRespVO> exportList = list.stream()
                .map(configService::getMaskedConfigRespVO).collect(Collectors.toList());
        // 输出
        ExcelUtils.write(response, "参数配置.xls", "数据", ConfigRespVO.class, exportList);
    }

    @PutMapping("/restore")
    @Operation(summary = "恢复参数配置至历史值",
            description = "ZS-CFG-004 B04：审查后恢复——仅回写 value，走与更新同一值校验与乐观锁契约；" +
                    "秘密/敏感参数的历史值已脱敏不可自动恢复，须附审查依据")
    @PreAuthorize("@ss.hasPermission('infra:config:update')")
    public CommonResult<Boolean> restoreConfig(@Valid @RequestBody ConfigRestoreReqVO reqVO) {
        configService.restoreConfig(reqVO);
        return success(true);
    }

    @GetMapping("/history/page")
    @Operation(summary = "获得参数配置变更历史分页",
            description = "ZS-CFG-004 B04：变更历史审查入口；敏感/秘密配置整页掩码输出")
    @PreAuthorize("@ss.hasPermission('infra:config:query')")
    public CommonResult<PageResult<ConfigChangeHistoryRespVO>> getConfigChangeHistoryPage(
            @Valid ConfigChangeHistoryPageReqVO pageReqVO) {
        return success(configService.getConfigChangeHistoryPage(pageReqVO));
    }

}
