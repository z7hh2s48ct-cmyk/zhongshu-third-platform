package cn.zszj.module.system.service.notify.landing;

import cn.zszj.module.system.dal.dataobject.notify.NotifyMessageDO;

import java.util.Map;

/**
 * 消息落点注册条目 SPI（ZS-MSG-003）——业务模块按「模板编码」注册自己的消息落点。
 *
 * <p>使用方式：实现本接口并声明为 Spring Bean，{@link NotifyLandingRegistry} 启动时按
 * {@link #templateCode()} 收集（重复编码启动即失败）。当前启用模块（system/infra）尚无
 * 生产发送方注册落点；首个真实注册方预期为 BPM 待办/流程通知（D-07 首链启用时接入）。
 *
 * <p>两条硬性合同：
 * <ul>
 *   <li><b>重新授权</b>：{@link #authorize} 必须重新读取业务对象并校验当前用户权限——
 *       站内信正文不是访问业务对象的授权凭据，旧消息在业务撤权/删除后必须拒绝进入详情、
 *       下载附件；实现不得依据消息内容放行，也不得缓存授权结论（组织/权限变化须即时生效）。</li>
 *   <li><b>结构化落点</b>：{@link #resolve} 只返回结构性引用（模块/路由/业务 ID），
 *       不得把业务敏感正文复制进落点响应。</li>
 * </ul>
 */
public interface NotifyLandingProvider {

    /**
     * @return 本条目对应的站内信模板编码（NotifyMessageDO.templateCode，全局唯一注册）
     */
    String templateCode();

    /**
     * @return 落点归属业务模块名（ModuleCatalog 命名空间，如 system / bpm）；
     *         模块未启用时解析直接判 {@link NotifyLandingUnavailable#MODULE_DISABLED}，
     *         不会调用本条目的任何方法
     */
    String module();

    /**
     * 重新读取业务并授权（跳转前置防线）。
     *
     * @param message        站内信（已确认归属当前登录用户）
     * @param templateParams 消息模板参数（业务引用从此提取）
     * @throws cn.zszj.framework.common.exception.ServiceException 业务失权/对象不存在等拒绝；
     *                                                             异常消息将作为不可用原因原样返回给前端（实现方须保证消息可呈现、无敏感信息）
     */
    void authorize(NotifyMessageDO message, Map<String, Object> templateParams);

    /**
     * 解析指定端的落点描述。
     *
     * @param client         请求端
     * @param templateParams 消息模板参数
     * @return 落点描述；该端未注册落点时返回 {@code null}（解析判
     *         {@link NotifyLandingUnavailable#CLIENT_UNSUPPORTED}）
     */
    NotifyLandingDescriptor resolve(NotifyLandingClient client, Map<String, Object> templateParams);

}
