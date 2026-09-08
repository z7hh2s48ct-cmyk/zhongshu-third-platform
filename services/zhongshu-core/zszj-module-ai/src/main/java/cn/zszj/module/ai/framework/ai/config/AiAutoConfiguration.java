package cn.zszj.module.ai.framework.ai.config;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.zszj.module.ai.framework.ai.core.model.AiModelFactory;
import cn.zszj.module.ai.framework.ai.core.model.AiModelFactoryImpl;
import cn.zszj.module.ai.framework.ai.core.model.baichuan.BaiChuanChatModel;
import cn.zszj.module.ai.framework.ai.core.model.doubao.DouBaoChatModel;
import cn.zszj.module.ai.framework.ai.core.model.gemini.GeminiChatModel;
import cn.zszj.module.ai.framework.ai.core.model.grok.GrokChatModel;
import cn.zszj.module.ai.framework.ai.core.model.hunyuan.HunYuanChatModel;
import cn.zszj.module.ai.framework.ai.core.model.midjourney.api.MidjourneyApi;
import cn.zszj.module.ai.framework.ai.core.model.minimax.MiniMaxChatModel;
import cn.zszj.module.ai.framework.ai.core.model.moonshot.MoonshotChatModel;
import cn.zszj.module.ai.framework.ai.core.model.siliconflow.SiliconFlowApiConstants;
import cn.zszj.module.ai.framework.ai.core.model.siliconflow.SiliconFlowChatModel;
import cn.zszj.module.ai.framework.ai.core.model.stepfun.StepFunChatModel;
import cn.zszj.module.ai.framework.ai.core.model.suno.api.SunoApi;
import cn.zszj.module.ai.framework.ai.core.model.xinghuo.XingHuoChatModel;
import cn.zszj.module.ai.framework.ai.core.model.yiyan.YiYanChatModel;
import cn.zszj.module.ai.framework.ai.core.model.zhipu.ZhiPuChatModel;
import cn.zszj.module.ai.framework.ai.core.webserch.AiWebSearchClient;
import cn.zszj.module.ai.framework.ai.core.webserch.bocha.AiBoChaWebSearchClient;
import cn.zszj.module.ai.tool.method.PersonService;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.milvus.autoconfigure.MilvusServiceClientProperties;
import org.springframework.ai.vectorstore.milvus.autoconfigure.MilvusVectorStoreProperties;
import org.springframework.ai.vectorstore.qdrant.autoconfigure.QdrantVectorStoreProperties;
import org.springframework.ai.vectorstore.redis.autoconfigure.RedisVectorStoreProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 众墅之家 AI 自动配置
 *
 * @author fansili
 */
@Configuration
@EnableConfigurationProperties({ ZszjAiProperties.class,
        QdrantVectorStoreProperties.class, // 解析 Qdrant 配置
        RedisVectorStoreProperties.class, // 解析 Redis 配置
        MilvusVectorStoreProperties.class, MilvusServiceClientProperties.class // 解析 Milvus 配置
})
@Slf4j
public class AiAutoConfiguration {

    @Bean
    public AiModelFactory aiModelFactory() {
        return new AiModelFactoryImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    public ObservationRegistry observationRegistry() {
        // 特殊：兜底有 ObservationRegistry Bean，避免相关的 ChatModel 创建报错。相关 issue：https://t.zsxq.com/CuPu4
        return ObservationRegistry.NOOP;
    }

    // ========== 各种 AI Client 创建 ==========

    @Bean
    @ConditionalOnProperty(value = "zszj.ai.gemini.enable", havingValue = "true")
    public GeminiChatModel geminiChatModel(ZszjAiProperties zszjAiProperties) {
        ZszjAiProperties.Gemini properties = zszjAiProperties.getGemini();
        return buildGeminiChatClient(properties);
    }

    public GeminiChatModel buildGeminiChatClient(ZszjAiProperties.Gemini properties) {
        if (StrUtil.isEmpty(properties.getModel())) {
            properties.setModel(GeminiChatModel.MODEL_DEFAULT);
        }
        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder()
                        .baseUrl(GeminiChatModel.BASE_URL)
                        .completionsPath(GeminiChatModel.COMPLETE_PATH)
                        .apiKey(properties.getApiKey())
                        .build())
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(properties.getModel())
                        .temperature(properties.getTemperature())
                        .maxTokens(properties.getMaxTokens())
                        .topP(properties.getTopP())
                        .build())
                .toolCallingManager(getToolCallingManager())
                .build();
        return new GeminiChatModel(openAiChatModel);
    }

    @Bean
    @ConditionalOnProperty(value = "zszj.ai.doubao.enable", havingValue = "true")
    public DouBaoChatModel douBaoChatClient(ZszjAiProperties zszjAiProperties) {
        ZszjAiProperties.DouBao properties = zszjAiProperties.getDoubao();
        return buildDouBaoChatClient(properties);
    }

    public DouBaoChatModel buildDouBaoChatClient(ZszjAiProperties.DouBao properties) {
        if (StrUtil.isEmpty(properties.getModel())) {
            properties.setModel(DouBaoChatModel.MODEL_DEFAULT);
        }
        DeepSeekChatModel openAiChatModel = DeepSeekChatModel.builder()
                .deepSeekApi(DeepSeekApi.builder()
                        .baseUrl(DouBaoChatModel.BASE_URL)
                        .completionsPath(DouBaoChatModel.COMPLETE_PATH)
                        .apiKey(properties.getApiKey())
                        .build())
                .defaultOptions(DeepSeekChatOptions.builder()
                        .model(properties.getModel())
                        .temperature(properties.getTemperature())
                        .maxTokens(properties.getMaxTokens())
                        .topP(properties.getTopP())
                        .build())
                .toolCallingManager(getToolCallingManager())
                .build();
        return new DouBaoChatModel(openAiChatModel);
    }

    @Bean
    @ConditionalOnProperty(value = "zszj.ai.siliconflow.enable", havingValue = "true")
    public SiliconFlowChatModel siliconFlowChatClient(ZszjAiProperties zszjAiProperties) {
        ZszjAiProperties.SiliconFlow properties = zszjAiProperties.getSiliconflow();
        return buildSiliconFlowChatClient(properties);
    }

    public SiliconFlowChatModel buildSiliconFlowChatClient(ZszjAiProperties.SiliconFlow properties) {
        if (StrUtil.isEmpty(properties.getModel())) {
            properties.setModel(SiliconFlowApiConstants.MODEL_DEFAULT);
        }
        DeepSeekChatModel openAiChatModel = DeepSeekChatModel.builder()
                .deepSeekApi(DeepSeekApi.builder()
                        .baseUrl(SiliconFlowApiConstants.DEFAULT_BASE_URL)
                        .apiKey(properties.getApiKey())
                        .build())
                .defaultOptions(DeepSeekChatOptions.builder()
                        .model(properties.getModel())
                        .temperature(properties.getTemperature())
                        .maxTokens(properties.getMaxTokens())
                        .topP(properties.getTopP())
                        .build())
                .toolCallingManager(getToolCallingManager())
                .build();
        return new SiliconFlowChatModel(openAiChatModel);
    }

    @Bean
    @ConditionalOnProperty(value = "zszj.ai.hunyuan.enable", havingValue = "true")
    public HunYuanChatModel hunYuanChatClient(ZszjAiProperties zszjAiProperties) {
        ZszjAiProperties.HunYuan properties = zszjAiProperties.getHunyuan();
        return buildHunYuanChatClient(properties);
    }

    public HunYuanChatModel buildHunYuanChatClient(ZszjAiProperties.HunYuan properties) {
        if (StrUtil.isEmpty(properties.getModel())) {
            properties.setModel(HunYuanChatModel.MODEL_DEFAULT);
        }
        if (StrUtil.isEmpty(properties.getBaseUrl())) {
            properties.setBaseUrl(HunYuanChatModel.BASE_URL);
        }
        // 创建 DeepSeekChatModel、HunYuanChatModel 对象
        DeepSeekChatModel openAiChatModel = DeepSeekChatModel.builder()
                .deepSeekApi(DeepSeekApi.builder()
                        .baseUrl(properties.getBaseUrl())
                        .completionsPath(HunYuanChatModel.COMPLETE_PATH)
                        .apiKey(properties.getApiKey())
                        .build())
                .defaultOptions(DeepSeekChatOptions.builder()
                        .model(properties.getModel())
                        .temperature(properties.getTemperature())
                        .maxTokens(properties.getMaxTokens())
                        .topP(properties.getTopP())
                        .build())
                .toolCallingManager(getToolCallingManager())
                .build();
        return new HunYuanChatModel(openAiChatModel);
    }

    @Bean
    @ConditionalOnProperty(value = "zszj.ai.xinghuo.enable", havingValue = "true")
    public XingHuoChatModel xingHuoChatClient(ZszjAiProperties zszjAiProperties) {
        ZszjAiProperties.XingHuo properties = zszjAiProperties.getXinghuo();
        return buildXingHuoChatClient(properties);
    }

    public XingHuoChatModel buildXingHuoChatClient(ZszjAiProperties.XingHuo properties) {
        if (StrUtil.isEmpty(properties.getModel())) {
            properties.setModel(XingHuoChatModel.MODEL_DEFAULT);
        }
        return XingHuoChatModel.builder()
                .apiKey(properties.getApiKey())
                .options(DeepSeekChatOptions.builder()
                        .model(properties.getModel())
                        .temperature(properties.getTemperature())
                        .maxTokens(properties.getMaxTokens())
                        .topP(properties.getTopP())
                        .build())
                .build();
    }

    @Bean
    @ConditionalOnProperty(value = "zszj.ai.baichuan.enable", havingValue = "true")
    public BaiChuanChatModel baiChuanChatClient(ZszjAiProperties zszjAiProperties) {
        ZszjAiProperties.BaiChuan properties = zszjAiProperties.getBaichuan();
        return buildBaiChuanChatClient(properties);
    }

    public BaiChuanChatModel buildBaiChuanChatClient(ZszjAiProperties.BaiChuan properties) {
        if (StrUtil.isEmpty(properties.getModel())) {
            properties.setModel(BaiChuanChatModel.MODEL_DEFAULT);
        }
        DeepSeekChatModel deepSeekChatModel = DeepSeekChatModel.builder()
                .deepSeekApi(DeepSeekApi.builder()
                        .baseUrl(BaiChuanChatModel.BASE_URL)
                        .apiKey(properties.getApiKey())
                        .completionsPath(BaiChuanChatModel.COMPLETE_PATH)
                        .build())
                .defaultOptions(DeepSeekChatOptions.builder()
                        .model(properties.getModel())
                        .temperature(properties.getTemperature())
                        .maxTokens(properties.getMaxTokens())
                        .topP(properties.getTopP())
                        .build())
                .toolCallingManager(getToolCallingManager())
                .build();
        return new BaiChuanChatModel(deepSeekChatModel);
    }

    @Bean
    @ConditionalOnProperty(value = "zszj.ai.yiyan.enable", havingValue = "true")
    public YiYanChatModel yiYanChatClient(ZszjAiProperties zszjAiProperties) {
        ZszjAiProperties.YiYan properties = zszjAiProperties.getYiyan();
        return buildYiYanChatClient(properties);
    }

    public YiYanChatModel buildYiYanChatClient(ZszjAiProperties.YiYan properties) {
        if (StrUtil.isEmpty(properties.getModel())) {
            properties.setModel(YiYanChatModel.MODEL_DEFAULT);
        }
        return new YiYanChatModel(buildDeepSeekCompatibleChatModel(
                StrUtil.blankToDefault(properties.getBaseUrl(), YiYanChatModel.BASE_URL),
                null, properties.getApiKey(), properties.getModel(), properties.getTemperature(),
                properties.getMaxTokens(), properties.getTopP()));
    }

    @Bean
    @ConditionalOnProperty(value = "zszj.ai.zhipu.enable", havingValue = "true")
    public ZhiPuChatModel zhiPuChatClient(ZszjAiProperties zszjAiProperties) {
        ZszjAiProperties.ZhiPu properties = zszjAiProperties.getZhipu();
        return buildZhiPuChatClient(properties);
    }

    public ZhiPuChatModel buildZhiPuChatClient(ZszjAiProperties.ZhiPu properties) {
        if (StrUtil.isEmpty(properties.getModel())) {
            properties.setModel(ZhiPuChatModel.MODEL_DEFAULT);
        }
        DeepSeekChatModel deepSeekChatModel = DeepSeekChatModel.builder()
                .deepSeekApi(DeepSeekApi.builder()
                        .baseUrl(StrUtil.blankToDefault(properties.getBaseUrl(), ZhiPuChatModel.BASE_URL))
                        .apiKey(properties.getApiKey())
                        .build())
                .defaultOptions(DeepSeekChatOptions.builder()
                        .model(properties.getModel())
                        .temperature(properties.getTemperature())
                        .maxTokens(properties.getMaxTokens())
                        .topP(properties.getTopP())
                        .build())
                .build();
        return new ZhiPuChatModel(deepSeekChatModel);
    }

    @Bean
    @ConditionalOnProperty(value = "zszj.ai.minimax.enable", havingValue = "true")
    public MiniMaxChatModel miniMaxChatClient(ZszjAiProperties zszjAiProperties) {
        ZszjAiProperties.MiniMax properties = zszjAiProperties.getMinimax();
        return buildMiniMaxChatClient(properties);
    }

    public MiniMaxChatModel buildMiniMaxChatClient(ZszjAiProperties.MiniMax properties) {
        if (StrUtil.isEmpty(properties.getModel())) {
            properties.setModel(MiniMaxChatModel.MODEL_DEFAULT);
        }
        return new MiniMaxChatModel(buildDeepSeekCompatibleChatModel(
                StrUtil.blankToDefault(properties.getBaseUrl(), MiniMaxChatModel.BASE_URL),
                null, properties.getApiKey(), properties.getModel(), properties.getTemperature(),
                properties.getMaxTokens(), properties.getTopP()));
    }

    @Bean
    @ConditionalOnProperty(value = "zszj.ai.moonshot.enable", havingValue = "true")
    public MoonshotChatModel moonshotChatClient(ZszjAiProperties zszjAiProperties) {
        ZszjAiProperties.Moonshot properties = zszjAiProperties.getMoonshot();
        return buildMoonshotChatClient(properties);
    }

    public MoonshotChatModel buildMoonshotChatClient(ZszjAiProperties.Moonshot properties) {
        if (StrUtil.isEmpty(properties.getModel())) {
            properties.setModel(MoonshotChatModel.MODEL_DEFAULT);
        }
        return new MoonshotChatModel(buildDeepSeekCompatibleChatModel(
                StrUtil.blankToDefault(properties.getBaseUrl(), MoonshotChatModel.BASE_URL),
                MoonshotChatModel.COMPLETE_PATH, properties.getApiKey(), properties.getModel(),
                properties.getTemperature(), properties.getMaxTokens(), properties.getTopP()));
    }

    @Bean
    @ConditionalOnProperty(value = "zszj.ai.stepfun.enable", havingValue = "true")
    public StepFunChatModel stepFunChatClient(ZszjAiProperties zszjAiProperties) {
        ZszjAiProperties.StepFun properties = zszjAiProperties.getStepfun();
        return buildStepFunChatClient(properties);
    }

    public StepFunChatModel buildStepFunChatClient(ZszjAiProperties.StepFun properties) {
        if (StrUtil.isEmpty(properties.getModel())) {
            properties.setModel(StepFunChatModel.MODEL_DEFAULT);
        }
        return new StepFunChatModel(buildDeepSeekCompatibleChatModel(
                StrUtil.blankToDefault(properties.getBaseUrl(), StepFunChatModel.BASE_URL),
                StepFunChatModel.COMPLETE_PATH, properties.getApiKey(), properties.getModel(),
                properties.getTemperature(), properties.getMaxTokens(), properties.getTopP()));
    }

    private static DeepSeekChatModel buildDeepSeekCompatibleChatModel(String baseUrl, String completionsPath,
                                                                     String apiKey, String model,
                                                                     Double temperature, Integer maxTokens,
                                                                     Double topP) {
        DeepSeekApi.Builder apiBuilder = DeepSeekApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey);
        if (StrUtil.isNotEmpty(completionsPath)) {
            apiBuilder.completionsPath(completionsPath);
        }
        return DeepSeekChatModel.builder()
                .deepSeekApi(apiBuilder.build())
                .defaultOptions(DeepSeekChatOptions.builder()
                        .model(model)
                        .temperature(temperature)
                        .maxTokens(maxTokens)
                        .topP(topP)
                        .build())
                .toolCallingManager(getToolCallingManager())
                .build();
    }

    @Bean
    @ConditionalOnProperty(value = "zszj.ai.midjourney.enable", havingValue = "true")
    public MidjourneyApi midjourneyApi(ZszjAiProperties zszjAiProperties) {
        ZszjAiProperties.Midjourney config = zszjAiProperties.getMidjourney();
        return new MidjourneyApi(config.getBaseUrl(), config.getApiKey(), config.getNotifyUrl());
    }

    @Bean
    @ConditionalOnProperty(value = "zszj.ai.suno.enable", havingValue = "true")
    public SunoApi sunoApi(ZszjAiProperties zszjAiProperties) {
        return new SunoApi(zszjAiProperties.getSuno().getBaseUrl());
    }

    @Bean
    @ConditionalOnProperty(value = "zszj.ai.grok.enable", havingValue = "true")
    public GrokChatModel grokChatClient(ZszjAiProperties zszjAiProperties) {
        ZszjAiProperties.Grok properties = zszjAiProperties.getGrok();
        return buildGrokChatClient(properties);
    }

    public GrokChatModel buildGrokChatClient(ZszjAiProperties.Grok properties) {
        if (StrUtil.isEmpty(properties.getModel())) {
            properties.setModel(GrokChatModel.MODEL_DEFAULT);
        }
        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder()
                        .baseUrl(StrUtil.blankToDefault(properties.getBaseUrl(), GrokChatModel.BASE_URL))
                        .completionsPath(GrokChatModel.COMPLETE_PATH)
                        .apiKey(properties.getApiKey())
                        .build())
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(properties.getModel())
                        .temperature(properties.getTemperature())
                        .maxTokens(properties.getMaxTokens())
                        .topP(properties.getTopP())
                        .build())
                .toolCallingManager(getToolCallingManager())
                .build();
        return new GrokChatModel(openAiChatModel);
    }

    // ========== RAG 相关 ==========

    @Bean
    public TokenCountEstimator tokenCountEstimator() {
        return new JTokkitTokenCountEstimator();
    }

    @Bean
    public BatchingStrategy batchingStrategy() {
        return new TokenCountBatchingStrategy();
    }

    private static ToolCallingManager getToolCallingManager() {
        return SpringUtil.getBean(ToolCallingManager.class);
    }

    // ========== Web Search 相关 ==========

    @Bean
    @ConditionalOnProperty(value = "zszj.ai.web-search.enable", havingValue = "true")
    public AiWebSearchClient webSearchClient(ZszjAiProperties zszjAiProperties) {
        return new AiBoChaWebSearchClient(zszjAiProperties.getWebSearch().getApiKey());
    }

    // ========== MCP 相关 ==========

    /**
     * 参考自 <a href="https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html">MCP Server Boot Starter</>
     */
    @Bean
    public List<ToolCallback> toolCallbacks(PersonService personService) {
        return List.of(ToolCallbacks.from(personService));
    }

}
