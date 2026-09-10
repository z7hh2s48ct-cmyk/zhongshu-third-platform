# 评审交接：ZS-SEC-003（右窗口 → 左窗口 codex 评审）

> 本文件为**评审交接单**，非评审结论。左窗口据此对下列提交执行 `codex review`，产出
> `codex-ZS-SEC-003.md`（人工复核 + codex 原始结论）与 `codex-ZS-SEC-003.raw.md`（完整 stdout），
> 并将本交接单与评审文档一并入库（遵循 [README.md](README.md) 「后续处理约定」）。

## 1. 评审目标

| 项 | 值 |
|---|---|
| 任务 | ZS-SEC-003（规范 Token 传输与特殊连接凭据；关联 FND-SYS-002/FND-AUTH-007/FND-INF-001，WP-05/11/12，B03 批次（文件票据在 B04）；优先级 P0，类别 改造；前置 ZS-SEC-002 已完成） |
| 交付 | `SecurityFrameworkUtils.obtainAuthorization`（E31）严格 Bearer 前缀解析 + 同源重复/冲突值拒绝 + URL 参数通道开关 `tokenParameterEnabled` + 三调用方（E30 过滤器 / admin 登出 / app 登出）统一传参 + Token 传输与连接凭据规范文档 |
| 交付形态 | 非破坏收紧：① 固定 Authorization 严格解析（`regionMatches(true,0,"Bearer",…)` 大小写不敏感 + 必须空白分隔）替代旧 `indexOf("Bearer ")` 任意位置子串查找 + magic `substring(+7)`，修复 `"xBearer y"` 被误解析为 `"y"`；② 同源多值经 `distinctNonEmptyValues` 归一，出现多个不同值视为歧义（凭据走私）拒绝；③ Header 优先 + 参数通道开关（默认 `true` 保留 WebSocket，共享/部署环境可置 `false` 禁 URL 长效凭据）。新增四参重载，旧三参重载委托 `parameterEnabled=true` 严格向后兼容，不改任何既有调用点语义 |
| 待评审提交 | `59b59227` |
| 评审命令 | `codex review --commit 59b59227` |
| 变更规模 | 10 files changed, 378 insertions(+), 18 deletions(-) |
| 本地验证（后端） | security starter `-am test` **BUILD SUCCESS**，`SecurityFrameworkUtilsTest` **18/18**（Failures 0 / Errors 0 / Skipped 0）；调用方 `AuthController`（module-system `-am compile`）、`AppAuthController`（module-member 独立 `-f` compile，该模块在根 pom 注释排除、不在 reactor）均 **BUILD SUCCESS** |
| 本地验证（门禁） | `node scripts/ops/run-local-gates.mjs --fast` 10/10；`node scripts/gov/verify-docs.mjs` issueCount 0 |
| 复现入口（后端） | `mvn -pl ':zszj-spring-boot-starter-security' -am test`（PowerShell 下按 [tools/env.sh](../../tools/env.sh) 设 `$env:JAVA_HOME`/`$env:PATH`；`-pl` 冒号列表整体加引号防逗号被拆成数组）。member 调用方须先 `mvn -pl ':zszj-spring-boot-starter-security' -am -DskipTests install` 刷新本地仓库，再 `mvn -f zszj-module-member/pom.xml -DskipTests compile` |
| 解锁 | SEC-003 转待验收后，底座统一 Token 传输契约（严格解析 + 冲突拒绝 + 参数开关）就绪，与 SEC-002（接口分类）、SEC-005（错误响应）、SEC-007（日志脱敏）共同构成 B03 接口与安全链路；**SEC-004（收紧 CORS）前置 ZS-SEC-003 就此解锁** |

## 2. 变更文件清单（仅这 10 个，未含其他窗口改动）

核心改造（`zszj-spring-boot-starter-security/src/main/`）：
- [`java/cn/zszj/framework/security/core/util/SecurityFrameworkUtils.java`](../../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-security/src/main/java/cn/zszj/framework/security/core/util/SecurityFrameworkUtils.java)（**+98**：新增 `AUTHORIZATION_BEARER="Bearer"` 常量；四参重载 `obtainAuthorization(request,headerName,parameterName,parameterEnabled)` + 旧三参委托 `parameterEnabled=true`；`distinctNonEmptyValues`（trim + 去空 + `LinkedHashSet` 去重）归一同源多值、Header 或 Parameter 多个不同值→`null`（凭据走私拒绝）；`parseBearerToken`（`regionMatches(true,0,"Bearer",…)` 大小写不敏感 + 空白分隔校验 + `substring` 剥离再 trim）替代旧 `indexOf` 子串查找；Header 优先、缺失且 `parameterEnabled` 时回退 Parameter。删 `StrUtil` 导入，加 `Arrays/Collections/LinkedHashSet/List/Set`）
- [`java/cn/zszj/framework/security/config/SecurityProperties.java`](../../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-security/src/main/java/cn/zszj/framework/security/config/SecurityProperties.java)（**+13**：新增 `@NotNull Boolean tokenParameterEnabled = true`；默认 `true` 保留 WebSocket 握手（浏览器不能自定义 Header），共享/部署环境可置 `false` 从服务端禁 URL 长效凭据）
- [`java/cn/zszj/framework/security/core/filter/TokenAuthenticationFilter.java`](../../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-security/src/main/java/cn/zszj/framework/security/core/filter/TokenAuthenticationFilter.java)（**+2/-1**：`doFilterInternal`（E30）改传 `securityProperties.getTokenParameterEnabled()`）

调用方（module-system admin / module-member app）：
- [`zszj-module-system/.../controller/admin/auth/AuthController.java`](../../services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/controller/admin/auth/AuthController.java)（**+2/-1**：`logout` 同上传入 `getTokenParameterEnabled()`）
- [`zszj-module-member/.../controller/app/auth/AppAuthController.java`](../../services/zhongshu-core/zszj-module-member/src/main/java/cn/zszj/module/member/controller/app/auth/AppAuthController.java)（**+2/-1**：`logout` 同上；member 在根 pom `<!--<module>zszj-module-member</module>-->` 注释排除、不在 reactor，独立编译验证）

测试（1 新增测试类，18 例）：
- [`zszj-spring-boot-starter-security/src/test/.../core/util/SecurityFrameworkUtilsTest.java`](../../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-security/src/test/java/cn/zszj/framework/security/core/util/SecurityFrameworkUtilsTest.java)（**新增 +179 / 18 例**：`MockHttpServletRequest` 纯静态单测无 DB/Redis——标准 Bearer、大小写不敏感（bearer/BeArEr）、裸 Token 兼容、`"xBearer y"` 子串误解析**回归护栏**（断言返回原值 `"xBearer y"` 非 `"y"`）、畸形三类（`"Bearer "`/`"Bearer"`/`"BearerXyz"`→null）、缺失、多空白 trim、参数回退、参数带 Bearer、参数通道关闭（enabled=false→null）、Header 优先、重复头冲突/相同、重复参数冲突、空 Header 回退参数、parameterName 空忽略）

构建：
- [`zszj-spring-boot-starter-security/pom.xml`](../../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-security/pom.xml)（**+12**：补 `spring-boot-starter-test` + `mockito-inline`（test scope））

文档：
- [`services/zhongshu-core/docs/Token传输与连接凭据规范.md`](../../services/zhongshu-core/docs/Token传输与连接凭据规范.md)（**新增 +73**：§1 现状证据与缺陷 / §2 三解析规则表 / §3 参数通道开关语义 / §4 三调用方落地矩阵 / §5 两端前端与连接通道证据 / §6 验收对齐 / §7 测试证据 / §8 边界与待验收）
- [`docs/05-底座模块分析与开发任务清单.md`](../05-底座模块分析与开发任务清单.md)（**+8/-2**：SEC-003 卡状态 **待开发→待验收** + 追加开发记录（含专项文档链接）+ 第 2 节 V1.13 记录与统计 44/28→43/29 + 第 19 节 V1.13 行 + 版本头 V1.12→V1.13）
- [`README.md`](../../README.md)（**+1/-1**：docs/05 索引版本 V1.12→V1.13）

> ⚠️ 工作树中 `zszj-spring-boot-starter-web`（`WebFrameworkUtils`/`WebProperties`/`ZszjWebAutoConfiguration`/`CacheRequestBodyFilter` + 新增 `src/test` filter/util 目录）、`zszj-spring-boot-starter-biz-tenant`（`TenantContextWebFilter`）、`zszj-module-system`（`SocialUserController`）等 ` M` 改动属**并行 ZS-SEC-008 窗口**（参数校验与请求资源限制）；`docs/reviews/codex-hotfix-*.raw.md`（左窗口 codex 评审产物）、`outputs/`（无关输出）等 `??` 文件**均不属于本提交 `59b59227`**，评审时勿纳入范围。

## 3. 请重点核查的判断点

1. **严格 Bearer 前缀 vs 裸 Token 兼容的边界（核心）**：新 `parseBearerToken` 以 `regionMatches(true,0,"Bearer",…)` + 必须空白分隔严格解析，无前缀时按裸 Token 兼容返回原值。请判断：(a) `"xBearer y"` 由旧版误解析为 `"y"` 收紧为返回原值 `"xBearer y"`（回归护栏锁定）是否确为正确语义（原值随后交 Token 过滤器判无效，不产生越权）；(b) 保留裸 Token 兼容是否留下歧义面、是否为既有客户端所必需、能否在后续批次移除；(c) `"BearerXyz"`（无空白分隔）判 `null` 而非按裸 Token 返回 `"BearerXyz"` 的取舍是否合理。

2. **重复/冲突值拒绝的凭据走私语义**：`distinctNonEmptyValues` 归一同源多值，Header 或 Parameter 出现多个**不同**值即返回 `null`（拒绝）。请核查：(a) 该拒绝是否覆盖 HTTP 请求走私 / 参数污染（HPP）下"多值取其一"的歧义；(b) 是否存在合法多值 Header 场景（如某些代理叠加 Authorization）被误拒；(c) trim + 去空 + 去重的归一顺序是否会把"空值 + 有效值"误判为冲突。

3. **tokenParameterEnabled 默认 true 的安全权衡**：默认保留 URL 参数通道以兼容 WebSocket（浏览器握手不能自定义 Header）。请判断：(a) 默认 `true`（保留能力）vs 默认 `false`（默认安全）的取舍是否合理，是否应在部署基线强制 `false` 并为 WebSocket 单列豁免；(b) 四参重载 + 旧三参委托 `true` 的向后兼容是否会让未显式配置的调用方静默保留参数通道；(c) WebSocket 之外是否还有其它依赖 `?token=` 的获准连接被遗漏。

4. **member 模块 reactor 外独立编译验证的充分性**：`AppAuthController`（module-member）在根 pom 注释排除、不在 reactor，本次以先 `install` security starter 刷新本地仓库再 `mvn -f zszj-module-member/pom.xml -DskipTests compile` 独立验证编译。请判断：仅编译验证（未跑 member 测试）对"传入 `getTokenParameterEnabled()`"这一改动是否充分；本地仓库旧 security jar 缺新字段导致的假阴性风险是否已完全规避。

5. **18 测试护栏成立性**：`SecurityFrameworkUtilsTest` 用 `MockHttpServletRequest` 纯静态单测。请核查：(a) 18 例是否真覆盖三条规则全部分支（严格前缀 / 重复冲突 / 参数开关）与边界（大小写、多空白、空值、parameterName 空）；(b) `"xBearer y"` 回归护栏是否确实锁定旧缺陷不复现；(c) 是否遗漏 Header 与 Parameter **同时**多值冲突、或 Header 有效 + Parameter 冲突的组合场景。

6. **文档与代码一致性**：[Token 传输与连接凭据规范](../../services/zhongshu-core/docs/Token传输与连接凭据规范.md) 的三规则表 / 三调用方矩阵 / 测试证据是否与 `SecurityFrameworkUtils` 实际实现、三处调用方 diff 一致？docs/05 V1.13 统计 44/28→43/29 与 SEC-003 卡状态 待开发→待验收、第 19 节变更记录是否自洽？README 索引 V1.12→V1.13 是否同步？

## 4. 已知边界（非缺陷，属分批范围）

- **文件专用短时票据与取流**：归 ZS-FILE-004.A（B04 批次）；票据过期/重用/错用途失败属文件票据语义同归该项。本项只做 B03 普通 API 凭据传输规范化，不以 B04 文件实现反向阻塞 B03（卡片放行边界明示）。
- **生产硬化与端到端**：`tokenParameterEnabled=false` 生产硬化开关、真实代理/日志同步与 WebSocket 端到端连接在真实环境的验收归 ZS-SEC-012.B；本项以 `MockHttpServletRequest` 组件级单测 + 两端前端静态实证证明，未在真实 PG/Redis/代理环境联验。
- **OAuth2 开放端点凭据**：`client_secret`/basic 传输审查承接 ZS-SEC-002 登记的残余审查点，随 OAuth2 领域开发落地，本项不涉。
- **Token 生命周期**：刷新/撤销/停用生命周期归 M04 的 ZS-LOGIN-001～005；本项只管传输解析，不改 Token 有效性验证（仍由 [Token 过滤器](../../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-security/src/main/java/cn/zszj/framework/security/core/filter/TokenAuthenticationFilter.java) 执行）。
- **member 测试未跑**：member 模块不在 reactor，仅独立编译验证 `AppAuthController` 改动，未跑其单测（避免触发其无关环境依赖的失败）。
- **待验收语义**：本项未拆 .A/.B 子项，主卡状态由 待开发 转 待验收（同 SEC-002/005 模式）；真实环境端到端联验随后续批次收口。

## 5. 状态

- [x] 右窗口：开发 + 本地验证（security `-am test` BUILD SUCCESS，`SecurityFrameworkUtilsTest` 18/18；system/member 调用方编译 BUILD SUCCESS；门禁 `--fast` 10/10 + verify-docs 0 issue）+ 提交（`59b59227`）+ 回填 05 文档 SEC-003 开发记录、V1.13 变更记录/统计 44/28→43/29 + README 索引同步 + 新增 Token 传输与连接凭据规范文档
- [x] 左窗口：`codex review --commit 59b59227` → 产出 [`codex-ZS-SEC-003.md`](codex-ZS-SEC-003.md) + [`codex-ZS-SEC-003.raw.md`](codex-ZS-SEC-003.raw.md)（model `gpt-6-astra` / reasoning `xhigh`；首次运行遇 `Selected model is at capacity` 中断，重跑成功）
- [x] 左窗口：**无 P0/P1/P2/P3 缺陷**——codex r0 直接 0 发现（"No actionable regressions attributable to this commit were found in token parsing, configuration propagation, or affected callers"）；处置见 §6

## 6. 处置（左窗口回填）

**r0 直接通过（0 发现）。** codex `review --commit 59b59227`（`gpt-6-astra` / `xhigh`）对 Token 解析、配置传播与三处调用方做静态复核后判定 **"No actionable regressions attributable to this commit"**，未产出任何 `[P0]/[P1]/[P2]/[P3]` 评审意见。左窗口独立复核（详见 [codex-ZS-SEC-003.md](codex-ZS-SEC-003.md) §复核与处置）与 codex 结论一致，另登记 2 项非阻断观察（均属文档口径，非代码缺陷）：

| 编号 | 级别 | 观察 | 处置 |
|---|---|---|---|
| OBS-1 | P3（文档准确性） | 本交接单 §2 称"新增 `AUTHORIZATION_BEARER="Bearer"` 常量"，但 `git show 59b59227^` 证实该常量在父提交已为 `public static final String AUTHORIZATION_BEARER = "Bearer";`（旧 `indexOf(AUTHORIZATION_BEARER + " ")` 即引用它），本提交未新增/未改可见性，仅复用 | 无需改代码；docs/05 与专项文档均未误称"新增常量"（仅交接单口径偏差），随后续文档整理校正措辞即可，不阻塞 |
| OBS-2 | P3（文档一致性） | docs/05 第 2 节统计行已正确更新为"V1.13 统计…43 项待开发…29 项待验收"，但括注日期仍为"2026-09-09"，与 V1.13 变更记录/第 19 节的"2026-09-10"不一致（统计**数值**正确，仅日期串未随版本递进） | 建议下次触碰 docs/05 时把 §2 括注日期改为 2026-09-10；纯文档、不阻塞 |

> **codex 沙箱测试执行说明**：codex 在评审中尝试于隔离工作树 `outputs/review-59b59227-isolated` 跑 `SecurityFrameworkUtilsTest`，因沙箱对本地 Maven 仓库 `spring-boot-starter-3.5.15.jar` 抛 `java.nio.file.AccessDeniedException`（Windows sandbox 权限）而编译失败——codex 明确归因为"sandbox permissions on the local Maven dependency cache"，**非本提交缺陷**。右窗口本机 `mvn -pl :zszj-spring-boot-starter-security -am test` 已 18/18 BUILD SUCCESS，测试通过性以右窗口本机结果为准（与 hotfix-E 的 G1 symlink EPERM 假失败同类，属 codex 沙箱环境限制）。
>
> **遗留清理项（非本提交）**：codex 隔离工作树 `outputs/review-59b59227-isolated`（约 8343 个文件，本仓库副本）因沙箱策略拒绝而未被 codex 自行清除；该目录 untracked 且不属于 `59b59227`，可手动 `Remove-Item -Recurse -Force outputs\review-59b59227-isolated` 清理，不影响任何交付物。

**结论**：SEC-003 无 P0/P1/P2 缺陷，2 项 P3 均为文档口径/日期小疑（非代码问题、不阻塞）。主卡维持 **待验收**；SEC-004（收紧 CORS）前置就此解锁。
