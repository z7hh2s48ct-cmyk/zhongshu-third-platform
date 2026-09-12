# ZS-SEC-004 codex 评审结论与处置

> 被审提交（r0）：`680f79d4`（ZS-SEC-004 收紧 CORS 并明确浏览器安全边界，B03；前置 ZS-ENG-003、ZS-SEC-002、ZS-SEC-003 已完成；worktree 隔离分支 `feat/sec-004`，测试精炼 `eff2cdd1`，`--no-ff` 合并 main `0ee8e286`）。
> P1/P2 修复提交：`b9568d54`（无端口 loopback 源 + 前端实发头 + 本地 iframe override，4 files +41/-2）。
> 二次复核（r1）：`codex review --commit b9568d54`，结论 PASS / 0 发现。
> 评审工具：`codex-cli 0.154.0`，模型 `gpt-6-astra`，reasoning effort `xhigh`，sandbox `read-only`，approval `never`。
> 完整 stdout：r0 [codex-ZS-SEC-004.raw.md](codex-ZS-SEC-004.raw.md)、r1 [codex-ZS-SEC-004-r1.raw.md](codex-ZS-SEC-004-r1.raw.md)。
> 说明：本任务评审直接对提交执行，未走事前 HANDOFF 交接单；本文件即评审产物 + 处置，遵循 [README.md](README.md)「后续处理约定」。r0 跨读 admin-web 前端（`.env` / `service.ts` / `swagger/index.vue`）抓到两阶段子代理后端孤立评审遗漏的集成回归。

## Codex 原始结论（r0，被审提交 `680f79d4`）

判定 **FAIL**，发现 **2 项 P1 + 1 项 P2**（无 P0 / P3）：

> The new CORS defaults break the existing frontend through both origin matching and missing request headers; both behaviors were verified against Spring 6.2.19. The framing change also breaks the existing local documentation iframe.

### Review comments

- **[P1] Allow loopback origins without an explicit port** — `services/zhongshu-core/zszj-server/src/main/resources/application.yaml:284-285`
  > The checked-in admin configuration uses `VITE_PORT=80` and calls `http://localhost:48080`, so browsers send `Origin: http://localhost` without `:80`. Spring 6.2.19 does not match that origin against `http://localhost:*`; the new filter therefore rejects the default local frontend's cross-origin requests. Use Spring's `http://localhost:[*]` syntax, which includes the default port, or explicitly list the portless origins. Update both this YAML and the matching `WebProperties.Cors` defaults.

- **[P1] Include the headers sent by the admin request interceptor** — `services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/main/java/cn/zszj/framework/web/config/WebProperties.java:116-117`
  > Even from an allowed origin such as `http://localhost:5173`, every admin GET request is blocked because `apps/zhongshu-admin-web/src/config/axios/service.ts:77-79` adds `Cache-Control` and `Pragma`, neither of which is allowed here. Spring omits these headers from `Access-Control-Allow-Headers`, so the browser refuses to send requests including tenant lookup and permission loading. Include the existing client request headers in the allowlist, or remove the unnecessary headers from the client, and verify a preflight using the interceptor's actual header set.

- **[P2] Preserve the existing cross-origin documentation iframe** — `services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-security/src/main/java/cn/zszj/framework/security/config/ZszjWebSecurityConfigurerAdapter.java:125-126`
  > In the checked-in local setup, `apps/zhongshu-admin-web/src/views/infra/swagger/index.vue` embeds `http://localhost:48080/doc.html` inside the frontend served on port 80. Applying `SAMEORIGIN` to that response makes browsers refuse the iframe, breaking the existing API documentation screen even after CORS is corrected. No local override or frontend migration accompanies this change. Preserve the integration through a same-origin proxy or a scoped framing policy, or open the documentation outside an iframe while retaining the hardened default elsewhere.

## 复核与处置

三项发现独立复核**全部成立，非误报**——关键在于两阶段子代理评审在 worktree 内孤立看后端代码未发现，codex 跨读 admin-web 前端（`.env` VITE_PORT=80、`service.ts` 请求拦截器、`swagger/index.vue` iframe）抓到集成回归。三项前提均经 ground-truth 核实为真。

### P1-1 — Spring `:*` 模式不匹配无端口 loopback 源（✅ 已修复 + 经验裁决 + r1 确认）

**复核结论：成立。** admin-web [.env](../../apps/zhongshu-admin-web/.env) `VITE_PORT=80`，浏览器对默认端口省略 → 发出 `Origin: http://localhost`（无端口）；[config.ts](../../apps/zhongshu-admin-web/src/config/axios/config.ts) `base_url = VITE_BASE_URL + VITE_API_URL` 指向后端 `:48080`（跨源）。Spring 6.2.19 `CorsConfiguration.checkOrigin` 对 `http://localhost:*` 模式**不匹配**无端口源 → 本地前端跨域请求被拒。

**TDD 经验裁决（不轻信断言，以 Spring 真实语义为准）**：`CorsConfigTest.allowedOriginPatterns_shouldMatchPortlessLocalhost` 用 `CorsConfiguration.checkOrigin("http://localhost")` 断言——修前返 `null`（**RED，经验证明 P1-1 属实**，同时推翻「`:*` 端口可选」的错误记忆），修后返 `http://localhost`（**GREEN**）。

**修复（commit `b9568d54`，采 codex「explicitly list the portless origins」方案）**：[WebProperties.Cors](../../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/main/java/cn/zszj/framework/web/config/WebProperties.java) 默认 `allowedOriginPatterns` + [application.yaml](../../services/zhongshu-core/zszj-server/src/main/resources/application.yaml) 白名单**同步**增补无端口 `http://localhost`、`http://127.0.0.1`（与 `:*` 并存，覆盖有端口/无端口两种本地源）。**双改必要性**：application.yaml 覆盖了 `allowed-origin-patterns`（Spring Boot list 属性整体替换而非合并），仅改 WebProperties 默认不足以修复运行时。

### P1-2 — allowedHeaders 缺前端实发头，预检拦截所有 admin GET（✅ 已修复 + r1 确认）

**复核结论：成立。** [service.ts](../../apps/zhongshu-admin-web/src/config/axios/service.ts) L77-80 对**每个 GET** 注入 `Cache-Control: no-cache` + `Pragma: no-cache`（非 CORS 安全列表头 → 触发预检）；L71 跨租户开启注入 `visit-tenant-id`；L97 API 加密注入 `X-Api-Encrypt`。收窄后的 allowedHeaders 仅含 `Authorization/Content-Type/X-Requested-With/tenant-id`，预检 `Access-Control-Request-Headers` 校验失败 → **每个 admin GET 被拦**（含租户查询/权限加载）。改前 `allowedHeader("*")` 放行一切，故为 SEC-004 收窄引入的回归。

**修复（commit `b9568d54`）**：[WebProperties.Cors](../../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/main/java/cn/zszj/framework/web/config/WebProperties.java) 默认 `allowedHeaders` 增补 `Cache-Control`、`Pragma`、`visit-tenant-id`、`X-Api-Encrypt`（application.yaml 未覆盖 allowed-headers，仅需改默认）。`CorsConfigTest.allowedHeaders_shouldCoverAllFrontendSentHeaders` 以 `containsAll` 断言 8 头齐全（修前 RED / 修后 GREEN）。

### P2 — SAMEORIGIN 阻断本地跨源 swagger doc.html iframe（✅ 已修复 + r1 确认）

**复核结论：成立。** [swagger/index.vue](../../apps/zhongshu-admin-web/src/views/infra/swagger/index.vue) L5 `<IFrame :src="VITE_BASE_URL + '/doc.html'">` 在 admin-web（`:80`）内嵌后端（`:48080`）Knife4j UI（跨源）。[ZszjWebSecurityConfigurerAdapter](../../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-security/src/main/java/cn/zszj/framework/security/config/ZszjWebSecurityConfigurerAdapter.java) 由 SEC-004 前 `frameOptions().disable()` 改为配置驱动默认 `SAMEORIGIN` → 浏览器拒绝该跨源 iframe，破坏本地 API 文档屏（改前能用，为回归）。

**修复（commit `b9568d54`）**：[application-local.yaml](../../services/zhongshu-core/zszj-server/src/main/resources/application-local.yaml) 增 `zszj.web.cors.frame-options: DISABLE`（Spring profile 标量属性级覆盖，仅覆盖 frame-options，其余 cors 属性继承 base）——本地恢复 swagger iframe，**生产默认仍 SAMEORIGIN**（契合 SEC-004「frameOptions 可配置」设计意图，与本地 `spring.boot.admin.server.frame-ancestors` 既有 iframe 放行先例一致）。

**本机复验证据**：`mvn -pl :zszj-spring-boot-starter-web -am test` — `CorsConfigTest` **5 测试全绿**（3 原 + 2 新经验断言，RED→GREEN）、web starter 合计 **37 测试 / 0 失败 / 1 跳过**、`MVN_EXIT=0`；`run-local-gates --fast` **10/10**；`verify-docs` **0 issue**。

## 二次复核（r1，被审提交 `b9568d54`）

对 P1/P2 修复提交重跑 `codex review --commit b9568d54`（`gpt-6-astra`/`xhigh`/`read-only`），结论 **PASS，0 发现**（无 P0 / P1 / P2 / P3）：

> The CORS additions match the frontend's local origins and request headers, and the iframe override is confined to the local profile. No actionable regressions introduced by this commit were found. Tests were not run in the read-only environment.

codex r1 在 exec trace 中跨读 `ZszjWebSecurityConfigurerAdapter.java`、`service.ts`、`.env.local`、`swagger/index.vue`、`Dockerfile`、`docker-compose.yml` 独立核对，确认：CORS 增补匹配前端本地源与请求头、iframe override 限于 local profile、无新回归。**r0 的 2×P1 + 1×P2 已全部消除。**（codex sandbox `read-only` 未执行测试，通过性以本机 37/37 `BUILD SUCCESS` + `MVN_EXIT=0` 为准。）

## 结论

**评审通过（r0 发现 2×P1 + 1×P2，已修复；r1 复核 0 发现，确认消除）。**

- **r0**（被审 `680f79d4`）：FAIL，2×P1 + 1×P2——CORS 收紧后 (1) 无端口 loopback 源不被 `:*` 匹配、(2) allowedHeaders 缺前端实发头拦截所有 admin GET、(3) SAMEORIGIN 阻断本地 swagger iframe；均为跨读前端才发现的集成回归，两阶段子代理后端孤立评审未捕获。
- **修复**（`b9568d54`）：TDD 经验裁决（Spring `checkOrigin` RED→GREEN 证明 P1-1、`containsAll` 证明 P1-2）+ 无端口源/前端头白名单增补 + 本地 frame-options DISABLE；web starter 37 测试全绿 + 门禁 10/10 + verify-docs 0 issue。
- **r1**（被审 `b9568d54`）：PASS，0 发现，codex 跨读前端与部署文件独立确认修复正确、无新回归。
- SEC-004 主卡 待开发→**待验收**：本轮为代码级 CORS 精确白名单收紧 + frameOptions 可配置 + 单元/经验测试；真实域名与代理配置、生产环境预检端到端、Cookie 型 CSRF 正反向测试（当前 STATELESS + Token 无 Cookie 攻击面，CSRF 保持 disable，若未来引入 Cookie 会话须补）归环境验收 / ZS-SEC-012.B；trace-id 跨域读取端到端归 ZS-SEC-006。
- codex 发现总数 **3**（2×P1 + 1×P2），已修复 **3**，仍有效 **0**。
