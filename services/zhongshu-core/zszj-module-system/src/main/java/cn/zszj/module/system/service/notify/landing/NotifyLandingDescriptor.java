package cn.zszj.module.system.service.notify.landing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 消息落点描述（ZS-MSG-003）——纯结构性业务引用。
 *
 * <p>刻意<strong>不携带消息正文</strong>（templateContent/templateParams 全量）：
 * 落点响应只包含「跳到哪个模块的哪个页面 + 最小业务参数（如业务 ID）」，
 * 正文始终以收件箱自身数据展示，业务详情须由落点页面重新读取并授权后呈现
 * （对齐 docs/05 ZS-MSG-003「跳转后重新读取业务并授权，不把业务敏感正文长期复制进通知」）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotifyLandingDescriptor {

    /** 落点归属业务模块（ModuleCatalog 命名空间，如 system / bpm），仅用于展示与注册校验 */
    private String module;

    /**
     * 落点路由：WEB 端为 Vue Router 绝对路径（如 /bpm/process-instance/detail），
     * MOBILE 端为 uni-app 页面绝对路径（如 /pages-bpm/processInstance/detail/index）。
     * 进入页面后的导航授权由各端自有守卫判定（服务端 API 为最终边界）。
     */
    private String route;

    /** 附加业务参数（如 {"id": 1024}），前端按端语义拼 query / 页面参数；可为空 */
    private Map<String, Object> params;

}
