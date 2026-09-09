package cn.zszj.server;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ZS-ENG-001：模块启用白名单测试。
 *
 * 不依赖 Spring 上下文，直接以 POM 与源码文本为事实来源校验：
 * 1. 根 pom.xml 实际激活的 zszj-module-* 与 ModuleWhitelist.ENABLED_MODULES 一致；
 * 2. zszj-server/pom.xml 实际引入的业务模块依赖与 ENABLED_MODULES 一致（依赖闭包决定 Bean/Job 装配）；
 * 3. DefaultController 的 @RequestMapping 覆盖所有未启用模块的 API 前缀，且不覆盖启用模块前缀；
 * 4. zszj-server 源码未引用任何未启用模块的 Java 包。
 */
class ModuleWhitelistTest {

    private static final Pattern MODULE_ARTIFACT = Pattern.compile("<artifactId>(zszj-module-[a-z-]+)</artifactId>");

    private String read(String relative) throws IOException {
        return Files.readString(Paths.get(relative));
    }

    private Set<String> activeModuleArtifacts(String pomText) {
        String withoutComments = pomText.replaceAll("(?s)<!--.*?-->", "");
        Set<String> result = new LinkedHashSet<>();
        Matcher matcher = MODULE_ARTIFACT.matcher(withoutComments);
        while (matcher.find()) {
            result.add(matcher.group(1).replace("zszj-module-", ""));
        }
        return result;
    }

    @Test
    void enabledModulesMatchRootPomActiveModules() throws IOException {
        // 根 pom 通过 <module> 标签声明激活模块
        String withoutComments = read("../pom.xml").replaceAll("(?s)<!--.*?-->", "");
        Matcher matcher = Pattern.compile("<module>(zszj-module-[a-z-]+)</module>").matcher(withoutComments);
        Set<String> rootModules = new LinkedHashSet<>();
        while (matcher.find()) {
            rootModules.add(matcher.group(1).replace("zszj-module-", ""));
        }
        assertEquals(new LinkedHashSet<>(ModuleWhitelist.ENABLED_MODULES), rootModules,
                "根 pom.xml 激活的业务模块必须与白名单一致，启用/停用需同步 ModuleWhitelist");
    }

    @Test
    void enabledModulesMatchServerPomDependencies() throws IOException {
        // activeModuleArtifacts 已剥离 "zszj-module-" 前缀（与 enabledModulesMatchRootPomActiveModules 同一约定），
        // 故直接与 ENABLED_MODULES（无前缀，如 system/infra）比对。ZS-ENG-001 交付时本测试从未运行（当时无 JDK
        // 工具链，见 05 文档 ZS-ENG-001 开发记录「测试运行…待 B01 工具链复验」），断言误在期望侧补前缀、又在实际侧按前缀
        // 过滤已剥前缀的值，致实际恒为空、断言恒失败；ZS-SEC-002 首次以工具链运行 zszj-server 全测试时暴露并修复。
        Set<String> serverDeps = activeModuleArtifacts(read("pom.xml"));
        assertEquals(new LinkedHashSet<>(ModuleWhitelist.ENABLED_MODULES), serverDeps,
                "zszj-server 依赖的业务模块必须与白名单一致：未启用模块不得进入依赖闭包（Bean/Job 装配边界）");
    }

    @Test
    void defaultControllerCoversAllDisabledPrefixes() throws IOException {
        String source = read(Paths.get("src", "main", "java", "cn", "zszj", "server",
                "controller", "DefaultController.java").toString());
        List<String> declaredPrefixes = ModuleWhitelist.DISABLED_MODULE_API_PREFIXES.values().stream()
                .flatMap(List::stream).collect(Collectors.toList());
        for (String prefix : declaredPrefixes) {
            assertTrue(source.contains("\"" + prefix + "\""),
                    "DefaultController 缺少未启用模块前缀的兜底映射: " + prefix);
        }
        for (String enabled : ModuleWhitelist.ENABLED_MODULES) {
            assertTrue(!source.contains("\"/admin-api/" + enabled + "/**\""),
                    "启用模块不应被兜底映射拦截: " + enabled);
        }
    }

    @Test
    void serverSourceDoesNotReferenceDisabledModulePackages() throws IOException {
        String modulePackagePrefix = "cn.zszj.module.";
        try (Stream<Path> paths = Files.walk(Paths.get("src", "main", "java"))) {
            for (Path file : paths.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList())) {
                String text = Files.readString(file);
                for (String disabled : ModuleWhitelist.DISABLED_MODULE_API_PREFIXES.keySet()) {
                    assertTrue(!text.contains(modulePackagePrefix + disabled + "."),
                            file + " 不得引用未启用模块的包: " + disabled);
                }
            }
        }
    }

}
