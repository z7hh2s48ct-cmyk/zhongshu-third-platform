# ZS-SEC-003 Codex 代码评审

- **任务**：ZS-SEC-003 规范 Token 传输与特殊连接凭据（B03；关联 FND-SYS-002/FND-AUTH-007/FND-INF-001，WP-05/11/12；优先级 P0，类别 改造；前置 ZS-SEC-002 已完成）
- **提交**：`59b592276c0605131bff649c135dcbc711596f07`
- **改动规模**：10 files changed, 378 insertions(+), 18 deletions(-)
- **改动文件**：`SecurityFrameworkUtils.java`（+98）、`SecurityProperties.java`（+13）、`TokenAuthenticationFilter.java`（+2/-1）、`AuthController.java`（+2/-1）、`AppAuthController.java`（+2/-1）、`SecurityFrameworkUtilsTest.java`（新增 +179/18 例）、`zszj-spring-boot-starter-security/pom.xml`（+12）、`docs/Token传输与连接凭据规范.md`（新增 +73）、`docs/05-…清单.md`（+8/-2）、`README.md`（+1/-1）
- **评审时间**：2026-09-10
- **评审工具**：`codex-cli 0.153.4`，模型 `gpt-6-astra`，reasoning effort `xhigh`（首跑遇 `Selected model is at capacity` 中断，重跑成功）
- **交接单**：[HANDOFF-ZS-SEC-003.md](HANDOFF-ZS-SEC-003.md)
- **原始日志**：[codex-ZS-SEC-003.raw.md](codex-ZS-SEC-003.raw.md)

## Codex 原始结论

> No actionable regressions attributable to this commit were found in token parsing, configuration propagation, or affected callers. Test execution was blocked by sandbox permissions on the local Maven dependency cache.

**评审意见数：0**（原始日志中 `[P0]/[P1]/[P2]/[P3]` 标记数 = 0；`reasoning summaries: none`，日志为 codex 的调查 exec 轨迹 + 上述单条终审结论）。

codex 在评审过程中自主执行的调查（见原始日志 exec 轨迹）：`git show --stat`/全量 diff、`SecurityFrameworkUtils`/`SecurityProperties`/`TokenAuthenticationFilter`/两处 `logout` 调用方、`ZszjWebSecurityConfigurerAdapter`、`OAuth2TokenServiceImpl`（Token 校验链）、`LoginUserHandshakeInterceptor`（WebSocket 握手）、SEC-012.A 安全夹具与 `application-fixture.yaml`、两端前端与 `tools/env.sh`，并尝试在隔离工作树跑 `SecurityFrameworkUtilsTest`。

> **codex 侧测试执行受阻说明（非本提交缺陷）**：codex 在隔离副本 `outputs/review-59b59227-isolated` 内跑 `mvn -pl :zszj-spring-boot-starter-security -am test -Dtest=SecurityFrameworkUtilsTest` 时，`zszj-common` 编译阶段对本地 Maven 仓库 `spring-boot-starter-3.5.15.jar` 抛 `java.nio.file.AccessDeniedException`（`WindowsLinkSupport.getRealPath` → `ZipFileSystemProvider.removeFileSystem`），BUILD FAILURE。codex 明确归因为 "sandbox permissions on the local Maven dependency cache"——即 codex 沙箱对 `~/.m2` 的访问权限限制，与被审代码无关。此与 [codex-hotfix-E.md](codex-hotfix-E.md) 记录的 G1 `symlink EPERM` 假失败同类，属 codex 沙箱环境限制。测试通过性以右窗口本机结果为准：`mvn -pl :zszj-spring-boot-starter-security -am test` **18/18 BUILD SUCCESS**（Failures 0 / Errors 0 / Skipped 0）。

## 复核与处置

左窗口对交接单 §3 六个判断点逐一独立复核（读取提交实际 diff + 父提交对照 + 全仓调用方 grep + 文档比对），结论与 codex 一致：**代码层无 P0/P1/P2/P3 缺陷**；另登记 2 项文档口径 P3 观察（非代码问题）。

### 判断点 1 — 严格 Bearer 前缀 vs 裸 Token 兼容（核心）✅ 语义正确

- **(a) `"xBearer y"` 收紧正确**：`git show 59b59227^` 证实旧实现为 `int index = token.indexOf(AUTHORIZATION_BEARER + " "); return index >= 0 ? token.substring(index + 7).trim() : token;`——`"xBearer y"` 中 `"Bearer "` 命中 index=1，`substring(8)` 误返回 `"y"`。新 `parseBearerToken` 以 `regionMatches(true, 0, "Bearer", 0, 6)` 仅匹配**开头**，`"xBearer y"` 首字符 `x`≠`B` → 不匹配 → 按裸 Token 返回**原值** `"xBearer y"`。回归护栏用例 `substringBearerNotMisparsed` 断言返回 `"xBearer y"`，锁定旧缺陷不复现。原值随后交 [TokenAuthenticationFilter](../../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-security/src/main/java/cn/zszj/framework/security/core/filter/TokenAuthenticationFilter.java) → `oauth2TokenApi.checkAccessToken` 判无效，不产生越权。**判定正确。**
- **(b) 裸 Token 兼容的歧义面可控**：保留裸 Token（无前缀返回原值）确为宽松面，但 Token **有效性验证在下游** `checkAccessToken`，解析宽松不授予访问权——非法/歧义值仍认证失败。故歧义面被下游验证兜底，不构成安全缺陷。是否为既有客户端所必需、能否后续移除，属产品/兼容决策，非本提交代码问题（专项文档 §2 已登记为"兼容既有客户端"）。
- **(c) `"BearerXyz"` 判 null 合理**：`regionMatches` 命中 `Bearer` 前缀后，`value.length()<=6 || !isWhitespace(charAt(6))` → `"BearerXyz"`（`charAt(6)='X'`）判畸形返回 `null`。取舍合理：以 `Bearer` 开头却无空白分隔，几乎必是 scheme 畸形而非合法裸 Token，拒绝（返回 null 走认证失败链）比按裸 Token 放行更安全。三类畸形（`"Bearer "` 剥离后空 / `"Bearer"` 单独 / `"BearerXyz"` 无分隔）均判 null，测试 `malformedEmptyAfterBearer`/`malformedBearerAlone`/`malformedNoWhitespaceSeparator` 逐一锁定。

### 判断点 2 — 重复/冲突值拒绝的凭据走私语义 ✅ 覆盖到位

- **(a) 覆盖 HPP/请求走私歧义**：Header 走 `Collections.list(request.getHeaders(headerName))`（复数）、Parameter 走 `request.getParameterValues`（复数），同源 `distinctNonEmptyValues` 归一后 `size()>1` 即返回 `null`。这消除了旧实现 `getHeader`/`getParameter`（单数）"多值静默取其一"的歧义——正是 HPP/走私下攻击者期望的"取对我有利的那个值"。测试 `conflictingDuplicateHeaders`/`conflictingDuplicateParameters` 锁定。
- **(b) 合法多值误拒风险极低**：RFC 9110/7235 下 `Authorization` 应单值，代理不得叠加/改写；相同值经 `LinkedHashSet` 去重归一为 1（测试 `identicalDuplicateHeaders` 验证），仅**不同值**才拒绝。真实客户端不会发送两个不同 Authorization，误拒面可忽略，且拒绝（返回 null）是安全侧失败。
- **(c) 归一顺序不误判"空值+有效值"**：`distinctNonEmptyValues` 先 `StringUtils.hasText(value)` 过滤（`null`/空串/纯空白均剔除）再 `trim` 入 `LinkedHashSet`。故 `["", "abc"]`、`["   ", "abc"]` → 剔除空白后仅 `{"abc"}`，size=1 不判冲突；`["abc", "abc "]` → trim 后同为 `"abc"` 去重为 1。测试 `blankHeaderFallsBackToParameter`（空白 Header + 参数）验证空白 Header 不阻断回退。**归一顺序正确，不会把"空值+有效值"误判冲突。**

### 判断点 3 — tokenParameterEnabled 默认 true 的安全权衡 ✅ 取舍合理，向后兼容无静默泄漏

- **(a) 默认 true 合理**：浏览器 WebSocket 握手不能自定义 Header，只能 `/ws?token=` 拼接（`SecurityProperties.tokenParameter` 注释与专项文档 §3 均登记）；默认 `false` 会直接断掉该获准连接能力。默认 `true` 保留能力 + `@NotNull` 显式约束 + 文档指引"共享/部署环境置 false 硬化"，是"能力默认保留、硬化按需开启"的稳妥取舍。是否在部署基线**强制** false 并为 WebSocket 单列豁免，属 ZS-SEC-012.B 生产硬化范畴（交接单 §4 已明示边界），非本提交职责。
- **(b) 向后兼容无静默保留参数通道**：全仓 grep `obtainAuthorization` 确认——**三处生产调用方**（[TokenAuthenticationFilter](../../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-security/src/main/java/cn/zszj/framework/security/core/filter/TokenAuthenticationFilter.java) `doFilterInternal`、[AuthController](../../services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/controller/admin/auth/AuthController.java) `logout`、[AppAuthController](../../services/zhongshu-core/zszj-module-member/src/main/java/cn/zszj/module/member/controller/app/auth/AppAuthController.java) `logout`）**均已改用四参重载并显式传入 `getTokenParameterEnabled()`**；旧三参重载在本仓库内**无任何生产调用方**（仅测试与自身委托引用）。故"未显式配置的调用方静默保留参数通道"的担忧在本仓库**不成立**——三参重载纯属对外部/未来调用方的防御性兼容，其行为（委托 `true`）与旧语义一致，不构成新增泄漏面。
- **(c) WebSocket 外的 `?token=` 通道**：专项文档 §5 实证两端前端（Web `service.ts`、小程序 `interceptor.ts`/`sse.ts`/`download.ts`/上传）一律走 `Authorization: Bearer` Header，SSE/下载/上传均不依赖 URL 参数，WebSocket 是唯一 `?token=` 通道。codex 亦读取了 `LoginUserHandshakeInterceptor` 复核握手链，未发现遗漏的其它参数依赖连接。

### 判断点 4 — member 模块 reactor 外独立编译验证 ✅ 对本改动充分

- `AppAuthController.logout` 的改动是**纯参数传递**（多传一个 `securityProperties.getTokenParameterEnabled()`），属编译期签名使用，无新增运行时分支逻辑。`getTokenParameterEnabled()` 由 `@Data`（Lombok）生成，编译通过即证明字段/getter 存在且类型匹配。
- **假阴性风险已规避**：交接单记录先 `mvn -pl :zszj-spring-boot-starter-security -am -DskipTests install` 刷新本地仓库，再 `mvn -f zszj-module-member/pom.xml -DskipTests compile`——确保 member 编译时链接的是**含新字段的** security jar，而非旧 jar。故"旧 jar 缺字段导致的假阴性"已被 install-first 步骤排除。
- 未跑 member 单测的取舍合理：member 在根 pom 注释排除、不在 reactor，其单测可能触发无关环境依赖；本次改动不涉 member 业务逻辑，编译验证足以覆盖"传参正确性"。运行时行为由 security starter 的 18 例单测 + 三处调用方共用同一 `obtainAuthorization` 保证一致。

### 判断点 5 — 18 测试护栏成立性 ✅ 三规则全分支覆盖，仅组合场景可增补（非缺陷）

- **(a) 覆盖三条规则全分支 + 边界**：18 个 `@Test` 方法（`bearerSchemeCaseInsensitive` 内含 lower+mixed 两断言）覆盖——规则①严格前缀：标准 Bearer、大小写不敏感（bearer/BeArEr）、裸 Token、`xBearer y` 回归、畸形三类、多空白 trim；规则②重复冲突：重复头冲突/相同、重复参数冲突；规则③参数开关：参数回退、参数带 Bearer、参数通道关闭（enabled=false）、Header 优先、空 Header 回退、parameterName 空忽略；缺失：无 Header 无参数。与专项文档 §7 及交接单描述逐条吻合。
- **(b) `xBearer y` 回归护栏确实锁定**：`substringBearerNotMisparsed` 断言 `assertEquals("xBearer y", …)`（非 `"y"`），若回退到旧 `indexOf` 逻辑该断言必失败，护栏有效。
- **(c) 组合场景为可增补项（非缺陷，不单列 OBS）**：未显式覆盖"Header 有效 + Parameter 冲突（两个不同参数值）"与"Header 冲突 + Parameter 有效"两组合。但由代码结构，二者行为**确定**：Header 冲突在 L74-76 **先返回 null**（Parameter 不再读取）；Header 有效则 L79 `!hasText(raw)` 为 false（Parameter 不再读取）。即组合结果由"Header 优先 + 提前返回"完全决定，不存在未定义行为。建议后续补 2 例把该确定性显式钉死（提升护栏完备度），**不阻塞本提交**。

### 判断点 6 — 文档与代码一致性 ✅ 一致（2 项 P3 文档小疑另记）

- **专项文档 [Token传输与连接凭据规范](../../services/zhongshu-core/docs/Token传输与连接凭据规范.md)**：§2 三规则表（严格前缀 / 重复冲突 / Header 优先+参数开关）与 `SecurityFrameworkUtils` 实现逐条一致；§4 三调用方矩阵与 grep 结果一致（filter/admin logout/app logout）；§7 测试证据（18 例、`MockHttpServletRequest` 纯单测）与实际测试类一致；§1 现状缺陷描述（`indexOf("Bearer ")` + `substring(index+7)`）与父提交实际旧代码一致。
- **docs/05**：SEC-003 卡状态 待开发→待验收 ✅；V1.13 变更记录已追加 ✅；第 2 节统计 44/28→43/29 ✅（SEC-003 由待开发移入待验收，待开发 44→43、待验收 28→29，且待验收清单已含"SEC-002/003/005/007"）；第 19 节 V1.13 行已加 ✅；版本头 V1.12→V1.13 ✅。
- **README（根）**：docs/05 索引版本 V1.12→V1.13 已同步 ✅。

### 严重度评估与处置

| 编号 | 级别 | 观察 | 处置 |
|---|---|---|---|
| — | P0/P1/P2 | **无** | codex r0 0 发现；左窗口独立复核亦未发现代码级缺陷 |
| OBS-1 | P3（文档准确性） | 交接单 §2 称"新增 `AUTHORIZATION_BEARER="Bearer"` 常量"，但 `git show 59b59227^` 证实该常量在父提交已为 `public static final String AUTHORIZATION_BEARER = "Bearer";`（旧 `indexOf(AUTHORIZATION_BEARER + " ")` 即引用它）。本提交未新增、未改可见性，仅在 `parseBearerToken` 中复用 | 无需改代码；docs/05 与专项文档均未误称"新增常量"（仅交接单口径偏差），后续文档整理校正措辞即可，**不阻塞** |
| OBS-2 | P3（文档一致性） | docs/05 第 2 节统计行数值已正确更新为"V1.13 统计…43…29"，但括注日期仍为"2026-09-09"，与 V1.13 变更记录/第 19 节的"2026-09-10"不一致（仅日期串未随版本递进，统计数值正确） | 建议下次触碰 docs/05 时把 §2 括注日期改为 2026-09-10；纯文档、**不阻塞** |

**P0/P1 缺陷：无。** 依 [README.md](README.md)「后续处理约定」#2，无需在合入前修复的阻断项；OBS-1/2 两项 P3 均可分批处置。

## 结论

**评审通过（r0 直接 0 发现）。** codex（`gpt-6-astra`/`xhigh`）判定本提交在 Token 解析、配置传播与三处调用方**无可归因回归**；左窗口独立复核六个判断点全部成立——严格 Bearer 前缀修复了 `"xBearer y"` 子串误解析（回归护栏锁定）、重复/冲突拒绝覆盖 HPP/走私歧义且归一顺序不误判空值、`tokenParameterEnabled` 默认 true 取舍合理且**三处生产调用方均已显式传参、旧三参重载无生产调用方故无静默泄漏**、member 编译验证对传参改动充分（install-first 规避假阴性）、18 测试覆盖三规则全分支、文档与代码一致。codex 沙箱测试受阻属环境权限限制（非本提交缺陷），测试通过性以右窗口本机 18/18 BUILD SUCCESS 为准。

2 项 P3 观察（OBS-1 交接单"新增常量"口径 / OBS-2 docs/05 §2 日期串）均为文档口径建议，非代码缺陷、不阻塞合入。SEC-003 主卡维持 **待验收**，与 SEC-002/005/007 共同构成 B03 接口与安全链路；**SEC-004（收紧 CORS）前置就此解锁**。

> **遗留清理项（非本提交、非缺陷）**：codex 评审残留隔离工作树 `outputs/review-59b59227-isolated`（约 8343 文件，本仓库副本），因沙箱策略拒绝未被 codex 自行清除；该目录 untracked、不属于 `59b59227`，可手动 `Remove-Item -Recurse -Force outputs\review-59b59227-isolated` 清理。
