package cn.zszj.server.controller;

import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.server.ModuleWhitelist;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants.NOT_IMPLEMENTED;

/**
 * 默认 Controller：对未启用模块的 admin-api 前缀返回明确的不可用响应（501），
 * 解决部分 module 未开启时的 404 提示问题。
 *
 * 未启用模块及其 API 前缀的唯一清单见 {@link ModuleWhitelist#DISABLED_MODULE_API_PREFIXES}；
 * 本类的 @RequestMapping 路径必须与该清单保持一致，由 ModuleWhitelistTest 与
 * scripts/eng/verify-module-whitelist.mjs 双重校验。
 *
 * 边界（ZS-ENG-001）：未启用模块不得执行业务、写入数据或注册后台任务——
 * 本类只返回错误响应，不委托任何业务逻辑。
 */
@RestController
@Slf4j
public class DefaultController {

    @RequestMapping("/admin-api/mp/**")
    public CommonResult<Boolean> mp404() {
        return notImplemented("mp", "微信公众号");
    }

    @RequestMapping(value = {"/admin-api/product/**", // 商品中心
            "/admin-api/trade/**", // 交易中心
            "/admin-api/promotion/**"}) // 营销中心
    public CommonResult<Boolean> mall404() {
        return notImplemented("mall", "商城系统");
    }

    @RequestMapping("/admin-api/erp/**")
    public CommonResult<Boolean> erp404() {
        return notImplemented("erp", "ERP 模块");
    }

    @RequestMapping(value = {"/admin-api/wms/**"})
    public CommonResult<Boolean> wms404() {
        return notImplemented("wms", "WMS 仓库管理系统");
    }

    @RequestMapping("/admin-api/pms/**")
    public CommonResult<Boolean> pms404() {
        return notImplemented("pms", "PMS 项目管理系统");
    }

    @RequestMapping("/admin-api/crm/**")
    public CommonResult<Boolean> crm404() {
        return notImplemented("crm", "CRM 模块");
    }

    @RequestMapping(value = {"/admin-api/mes/**"})
    public CommonResult<Boolean> mes404() {
        return notImplemented("mes", "MES 系统");
    }

    @RequestMapping(value = {"/admin-api/im/**"})
    public CommonResult<Boolean> im404() {
        return notImplemented("im", "IM 即时通讯");
    }

    @RequestMapping(value = {"/admin-api/report/**"})
    public CommonResult<Boolean> report404() {
        return notImplemented("report", "报表模块");
    }

    @RequestMapping(value = {"/admin-api/pay/**"})
    public CommonResult<Boolean> pay404() {
        return notImplemented("pay", "支付模块");
    }

    @RequestMapping(value = {"/admin-api/ai/**"})
    public CommonResult<Boolean> ai404() {
        return notImplemented("ai", "AI 大模型");
    }

    @RequestMapping(value = {"/admin-api/iot/**"})
    public CommonResult<Boolean> iot404() {
        return notImplemented("iot", "IoT 物联网");
    }

    private static CommonResult<Boolean> notImplemented(String module, String displayName) {
        List<String> enabled = ModuleWhitelist.ENABLED_MODULES;
        return CommonResult.error(NOT_IMPLEMENTED.getCode(),
                "[" + displayName + " zszj-module-" + module + " - 未启用]"
                        + "[启用需在 zszj-server/pom.xml 引入依赖并经底座批次验收，当前启用模块：" + enabled + "]");
    }

}
