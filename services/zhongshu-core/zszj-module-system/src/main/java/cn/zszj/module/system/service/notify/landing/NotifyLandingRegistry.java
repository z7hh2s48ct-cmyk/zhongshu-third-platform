package cn.zszj.module.system.service.notify.landing;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息落点注册表（ZS-MSG-003）——按模板编码聚合全部 {@link NotifyLandingProvider} Bean。
 *
 * <p>注册表只在装配期构建（编码冲突启动即失败，防静默覆盖）；运行期只读。
 * <b>不缓存任何解析/授权结论</b>：授权决策由 Provider 在每次解析时现算，
 * 组织/权限变化即时生效，无需失效通知（对齐卡片「组织/权限变化后刷新缓存」——
 * 以无缓存结构消除缓存失效问题）。
 */
@Component
public class NotifyLandingRegistry {

    private final Map<String, NotifyLandingProvider> providersByTemplateCode;

    public NotifyLandingRegistry(List<NotifyLandingProvider> providers) {
        Map<String, NotifyLandingProvider> map = new HashMap<>();
        for (NotifyLandingProvider provider : providers) {
            NotifyLandingProvider existing = map.putIfAbsent(provider.templateCode(), provider);
            if (existing != null) {
                throw new IllegalStateException("消息落点模板编码重复注册：" + provider.templateCode()
                        + "（" + existing.getClass().getName() + " 与 " + provider.getClass().getName() + "）");
            }
        }
        this.providersByTemplateCode = Map.copyOf(map);
    }

    /**
     * @return 该模板编码的落点条目；未注册返回 {@code null}
     */
    public NotifyLandingProvider getByTemplateCode(String templateCode) {
        return templateCode == null ? null : providersByTemplateCode.get(templateCode);
    }

}
