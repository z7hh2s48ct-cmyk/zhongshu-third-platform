# ZS-LOGIN-001 codex 评审结论与处置

> 被审提交（r0）：`28257a25`（ZS-LOGIN-001 分离访问令牌与刷新令牌的用途，B03；前置 B02、ZS-SEC-003 已完成；worktree 隔离分支 `feat/login-001`，`--no-ff` 合并 main `b6b1e4a2`——同分支另含 ZS-IAM-003，两任务分别评审）。
> r0 P1 修复提交：`678bd221`（门控关闭时拒绝并自愈清除缓存的合成访问令牌，堵 Redis 命中绕过 gate，2 files +62）。
> 二次复核（r1）：`codex review --commit 678bd221`，结论 **CLEAN / 0 发现**。
> 评审工具：`codex-cli 0.154.0`，模型 `gpt-6-astra`，reasoning effort `xhigh`，sandbox `read-only`，approval `never`。
> 完整 stdout：r0 [codex-ZS-LOGIN-001.raw.md](codex-ZS-LOGIN-001.raw.md)、r1 [codex-ZS-LOGIN-001.r1.raw.md](codex-ZS-LOGIN-001.r1.raw.md)。
> 说明：本任务评审直接对提交执行，未走事前 HANDOFF 交接单；本文件即评审产物 + 处置，遵循 [README.md](README.md)「后续处理约定」。评审弧呈 **r0 揪「门控机制自身对缓存态不完整」P1 → TDD 修复（gate 关闭时自愈 evict 污染条目）→ r1 确认归零**。本任务按用户选定 **选项 C「部分交付 + 拆 .B」** 收口：门控核心本轮交付、父卡待验收，特殊连接（WS/IM）迁移短时票据与生产 gate 翻转拆 ZS-LOGIN-001.B（见第 16.1 节）。

## 选项 C 部分交付背景

ZS-LOGIN-001 的目标是「普通 API 只接受访问凭据、刷新凭据不得充当访问令牌」。底座现状是 [OAuth2TokenServiceImpl](../../services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/service/oauth2/OAuth2TokenServiceImpl.java) 的 `getAccessToken` 查不到访问令牌时回退查刷新令牌并经 `convertToAccessToken` 转换（合成令牌以 refreshToken 串同时充当 accessToken 与 refreshToken），扩大了刷新凭据使用面。

本轮以**兼容门控**形态交付，避免一次性打断既有 WebSocket 特殊连接：

- 新增开关 `zszj.security.refresh-token-as-access-token-enabled`（`@Value` 注入，**代码默认 `false`** = 安全默认，`getAccessToken` 只认访问令牌、不再回退刷新令牌）；
- `@PostConstruct` 在 gate=true 时打**启动 WARN**，明示处于迁移期兼容态；
- **现网 [application.yaml](../../services/zhongshu-core/zszj-server/src/main/resources/application.yaml)（L298-302）置 `true`**：admin-web IM、miniapp IM/客服 3 处 WebSocket 握手以 `?token=<refreshToken>` 作凭据（浏览器 WS 无法自定义 Header、只能拼 URL 参数），迁移期仍需刷新令牌回退放行；yaml 注释已写明「须在 LOGIN-001.B 完成后改回 false 并删除本键」。

故本轮为**部分交付**：门控核心（gate 机制 + 安全默认 + 缓存自愈）交付并可测，父卡 待开发→**待验收**（待真实环境验收）；WS/IM 迁移短时握手票据端点、生产 gate 翻 false、删 `convertToAccessToken`/fallback 死代码拆 **ZS-LOGIN-001.B**（B06）。父卡验收判据「特殊连接不靠长期刷新凭据放行」挂 .B，父任务待 .B 验收后方可标为已验收（循 §1 分批子项规则）。

## Codex 原始结论（r0，被审提交 `28257a25`）

判定 **FAIL**，发现 **1 项 P1**（无 P0 / P2 / P3）：

> The new security gate does not enforce token-purpose separation for previously cached refresh tokens. Tests were not run in the read-only environment; Java and Maven were unavailable on PATH.

### Review comments

- **[P1] Enforce the compatibility gate on cached tokens** — `services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/service/oauth2/OAuth2TokenServiceImpl.java:140-141`
  > If a refresh token was accepted while compatibility was enabled—or before this upgrade—disabling the flag and restarting still leaves it usable as an access token. `convertToAccessToken` caches the synthetic token in Redis, and the early return in `getAccessToken` bypasses this new gate. Consequently, `checkAccessToken` continues authenticating it for its remaining refresh-token lifetime, potentially 30 days. Reject cached synthetic tokens when compatibility is disabled, and add a regression test that populates the cache with compatibility enabled before checking the same token with it disabled.

## 复核与处置（r0 P1）

### P1-1 — 缓存态合成令牌绕过门控（✅ 已修复，r1 确认归零）

**复核结论：成立（门控机制自身对「缓存态」不完整的回归）。** 本轮 gate 只加在 `getAccessToken` 的**「Redis 未命中 → 查 DB → 回退刷新令牌」**分支（L141 附近），却漏了**「Redis 命中」提前 return 分支**（L133-136）：`convertToAccessToken` 生成合成令牌时会把它写入 Redis（`oauth2AccessTokenRedisDAO`），故 gate=true 兼容期（或本次升级前）产生的合成令牌会**驻留 Redis**；一旦 gate 翻 false 重启，同一令牌再次访问时先在 Redis 命中、于 L133-136 **提前 return**，**绕过下方 gate 判断**，`checkAccessToken` 继续按访问令牌放行，直至其**刷新令牌 TTL 耗尽**（default client `refresh_token_validity_seconds=2592000` = 30 天）。即：门控对「新请求」生效、对「已缓存的旧合成令牌」失效——安全语义不完整。

**TDD 修复（commit `678bd221`，RED→GREEN，2 files +62）**：

- [OAuth2TokenServiceImpl#getAccessToken](../../services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/service/oauth2/OAuth2TokenServiceImpl.java) Redis 命中路径加门控拒绝 + 自愈清除：

  ```java
  if (accessTokenDO != null) {
      // ZS-LOGIN-001（codex P1 修复）：门控关闭时拒绝此前兼容期缓存进 Redis 的合成访问令牌，并自愈清除污染条目
      if (!refreshTokenAsAccessTokenEnabled && isSyntheticAccessToken(accessTokenDO)) {
          oauth2AccessTokenRedisDAO.delete(accessToken);
          return null;
      }
      return accessTokenDO;
  }
  ```

- 新增 `isSyntheticAccessToken` 助手：`convertToAccessToken` 以 refreshToken 串同时充当 accessToken 与 refreshToken，故二者相等即合成令牌；正常访问令牌的 accessToken 与 refreshToken 是两个独立生成的 UUID、不会相等。判据 `StrUtil.isNotEmpty(accessToken) && StrUtil.equals(accessToken, refreshToken)`。
- [OAuth2TokenServiceImplTest](../../services/zhongshu-core/zszj-module-system/src/test/java/cn/zszj/module/system/service/oauth2/OAuth2TokenServiceImplTest.java)（gate=false 类）+2 测试：①P1 看守 `testCheckAccessToken_cachedSyntheticToken_compatDisabled_shouldThrowUnauthorizedAndEvict`（预置合成令牌入 Redis + 断言 DB 无此访问令牌 → gate=false 下 `checkAccessToken` 抛 `401「访问令牌不存在」`+ 断言 Redis 污染条目已被清除）；②正向对照 `testGetAccessToken_cachedRealToken_compatDisabled_shouldStillReturn`（真实令牌 accessToken≠refreshToken + gate=false → `getAccessToken` 仍正常放行，防误伤）。

**RED→GREEN 经验裁决**：新测试首跑 RED（`Tests run: 17, Failures: 1`，P1 看守「Expected ServiceException...but nothing was thrown」实证 gate=false 下缓存合成令牌确实被放行），正向对照 PASS；修复后 GREEN（`Tests run: 17, Failures: 0`）。**共存复验**：`OAuth2TokenServiceImplCompatTest`（gate=true 类）2/0 + `OAuth2TokenServiceImplTest`（gate=false 类）17/0 = **19/0，BUILD SUCCESS**，`MVN_EXIT=0`——证明修复在 gate=true 兼容态不改变放行（合成令牌仍可用）、仅在 gate=false 安全态拒绝并 evict。（gate 由类级 `@TestPropertySource` 固定，禁用 `ReflectionTestUtils.setField` 改 gate 以免造成 CGLIB 代理假绿。）

**修复的额外收益（部分交付 I3）**：gate 关闭时**代码自愈 evict 污染条目**，即留痕登记的 I3（Redis 污染条目清理）已由本轮代码交付——ZS-LOGIN-001.B 翻生产 gate=false 时**不再依赖手工 Redis 清理**，命中即自愈。

## 二次复核（r1，被审提交 `678bd221`）

对 r0 P1 修复提交重跑 `codex review --commit 678bd221`（`gpt-6-astra`/`xhigh`/`read-only`），结论 **CLEAN，0 发现**（无 P0 / P1 / P2 / P3）：

> No actionable regressions were found. The change rejects and evicts cached synthetic tokens when compatibility is disabled, while preserving normal access-token and compatibility-enabled behavior. Tests were inspected but not run in the read-only environment.

codex r1 在 exec trace 中自核 `OAuth2GrantServiceImpl`（grantRefreshToken/revokeToken/getAccessToken 调用）、`BeanUtils`（toBean/copyProperties）、`RandomUtils`（getStringValue，确认正常访问令牌 accessToken≠refreshToken 的 UUID 独立性）、`TokenAuthenticationFilter`（checkAccessToken 链路）与 `git status`，独立确认「gate 关闭拒绝并 evict 缓存合成令牌、同时保留正常访问令牌与 gate=true 兼容行为、无新回归」。**r0 的 P1 已消除。**（codex sandbox `read-only` 且 Java/Maven 不在 PATH，两轮均未执行测试，通过性以本机 19/0 BUILD SUCCESS 为准。）

## 运维交接要点（gate 兼容态 + I3 自愈 + .B 依赖）

- **生产现状**：现网 `application.yaml` `zszj.security.refresh-token-as-access-token-enabled=true`（gate=true），维持 admin-web IM、miniapp IM/客服 3 处 WS 握手（`?token=<refreshToken>`）兼容；**代码默认 false（安全）**。gate=true 启动打 WARN 明示迁移期兼容态。安全修复（拒绝刷新令牌充当访问令牌）仅在 gate=false 时生效——**故生产实际启用待 .B 翻 false**。
- **I3 Redis 污染清理**：已由本轮 P1 修复代码自愈（gate=false 时 Redis 命中合成令牌即 `delete` + return null），无需手工清理 Redis。
- **.B 依赖与发布前置**：ZS-LOGIN-001.B 迁移 3 处 WS/IM 到短时握手票据端点后，**翻生产 gate=false 并删除 yaml 该键**（发布前置步骤），再删 `convertToAccessToken`/fallback 死代码。翻 false 前须确认 WS 已全量迁移，否则特殊连接断连。

## 结论

**评审通过（r0 发现 1×P1 门控缓存态绕过已修，r1 复核 0 发现确认消除）。**

- **r0**（被审 `28257a25`）：FAIL，1×P1——门控只拦「Redis 未命中 → DB → 回退刷新令牌」分支，漏「Redis 命中」提前 return；gate=true 期/升级前缓存的合成令牌在 gate 翻 false 后仍被 `checkAccessToken` 放行至刷新令牌 TTL（default client 30 天）。
- **r0 P1 修复**（`678bd221`）：`getAccessToken` Redis 命中路径加 `!gate && isSyntheticAccessToken(accessToken==refreshToken)` → `delete` + `return null`，加 `isSyntheticAccessToken` 助手，+2 测试（P1 看守 + 正向对照）；RED 17/1 → GREEN 17/0，共存 19/0 BUILD SUCCESS。
- **r1**（被审 `678bd221`）：CLEAN，0 发现，codex 自核 grant/bean/random/filter 链独立确认 gate 关闭拒绝并 evict 合成令牌、保留正常与兼容行为、无新回归。
- **本机复验证据**：`OAuth2TokenServiceImplTest`（gate=false）17/0 + `OAuth2TokenServiceImplCompatTest`（gate=true）2/0 = **19/0**，`MVN_EXIT=0`，`BUILD SUCCESS`。
- ZS-LOGIN-001 按**选项 C 部分交付**：门控核心（gate 机制 + 安全默认 false + 缓存自愈 evict）本轮交付，卡片 待开发→**待验收**；特殊连接（WS/IM）迁移短时票据 + 生产 gate 翻 false + 删死代码拆 **ZS-LOGIN-001.B**（B06，见第 16.1 节），父任务待 .B 验收后方可标为已验收。
- codex 发现总数 **1**（1×P1），已修复 **1**，延后 **0**，仍有效阻塞项 **0**。
- **本记录不表示任何主任务已验收。**
