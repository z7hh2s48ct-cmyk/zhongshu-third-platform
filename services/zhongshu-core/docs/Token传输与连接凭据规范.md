# Token 传输与连接凭据规范（ZS-SEC-003）

> 建立日期：2026-09-10。基线：ZS-SEC-002（接口分类与匿名白名单）完成后；与 ZS-SEC-005（统一错误响应）、ZS-SEC-007（日志脱敏）同属 B03 接口与安全链路。
> 性质：登记底座**统一 Token 传输契约**——所有受保护请求经 `SecurityFrameworkUtils.obtainAuthorization` 解析访问令牌：固定 Authorization 严格 Bearer 前缀解析、同源重复/冲突值拒绝、Header 优先且 URL 参数通道受开关约束（仅保留 WebSocket 等无法设置 Header 的获准连接）。
> 复用边界：**Token 有效性验证**（过期/撤销/用户类型）由 [Token 过滤器](../zszj-framework/zszj-spring-boot-starter-security/src/main/java/cn/zszj/framework/security/core/filter/TokenAuthenticationFilter.java) 执行，本规范只管传输解析；**错误响应语义**归 [错误响应与状态码矩阵](错误响应与状态码矩阵.md)（ZS-SEC-005）；**日志脱敏**归 [日志脱敏策略](日志脱敏策略.md)（ZS-SEC-007）；**文件专用短时票据与取流**归 ZS-FILE-004.A（B04）；**OAuth2 开放端点凭据（client_secret/basic）**传输审查随 OAuth2 领域落地。

## 1. 现状证据与缺陷

[SecurityFrameworkUtils](../zszj-framework/zszj-spring-boot-starter-security/src/main/java/cn/zszj/framework/security/core/util/SecurityFrameworkUtils.java)（E31）`obtainAuthorization` 原实现：

- 优先读 `Authorization` Header，缺失时**无条件**回退 URL 请求参数 `token`；
- Bearer 前缀用 `token.indexOf("Bearer ")` **任意位置子串查找** + `substring(index + 7)` 剥离。

缺陷：`indexOf` 子串查找使 `"xBearer y"` 命中位置 1 后被误解析为 `"y"`（凭据解析歧义）；参数回退无开关，普通长效凭据可经 URL 进入访问日志、浏览器历史、Referer、代理留存。宽松解析本身不代表能伪造有效 Token（验证仍由 Token 过滤器执行），但传输层歧义与 URL 泄露须收紧。

## 2. 解析规则（ZS-SEC-003 改造）

三条规则，缺失/畸形/重复统一返回 `null`（视为无有效凭据，交 Token 过滤器走既有认证失败链）：

| 规则 | 行为 | 拒绝/兼容判定 |
|---|---|---|
| ① 严格 Bearer 前缀 | `regionMatches(true, 0, "Bearer", …)` 大小写不敏感匹配开头 + 必须空白分隔 | `"Bearer abc"`→`abc`；`bearer`/`BeArEr` 同样剥离；`"abc"` 裸 Token 兼容返回；`"xBearer y"` 按裸 Token 返回**原值**（不再误解析为 `y`）；`"Bearer"`/`"BearerXyz"`（无空白分隔）/`"Bearer "`（剥离后空）→ `null` |
| ② 重复/冲突拒绝 | `distinctNonEmptyValues` 归一同来源多值（trim + 去空 + `LinkedHashSet` 去重） | 同源多个**不同**值（Header 或 Parameter）→ `null`（凭据走私防护）；多个**相同**值 → 归一为一个 |
| ③ Header 优先 + 参数开关 | Header 优先；仅 Header 缺失且 `parameterEnabled=true` 时回退 URL 参数 | Header 与 Parameter 并存取 Header、忽略 Parameter；`parameterEnabled=false` 完全忽略参数通道 |

方法签名：新增四参重载 `obtainAuthorization(request, headerName, parameterName, parameterEnabled)`；旧三参重载 `obtainAuthorization(request, headerName, parameterName)` 委托 `parameterEnabled=true` 保持**向后兼容**（不改任何既有调用点语义）。

## 3. 参数通道开关（tokenParameterEnabled）

新增 [SecurityProperties](../zszj-framework/zszj-spring-boot-starter-security/src/main/java/cn/zszj/framework/security/config/SecurityProperties.java) 字段 `tokenParameterEnabled`（`Boolean`，默认 `true`，`@NotNull`）：

- **默认 `true`**：保留 WebSocket 等无法设置 Header 的获准连接——浏览器 WebSocket 握手不能自定义 Header，只能 `/ws?token=` 拼接（见 `WebSocketProperties.path=/ws`、`LoginUserHandshakeInterceptor` 靠 `getLoginUser()` 判定握手）。
- **置 `false`（共享/部署环境硬化）**：从服务端禁止普通长效凭据进入 URL，规避访问日志、浏览器历史、Referer、代理留存导致的泄露。SSE、文件下载两端均走 Header，不受影响。

## 4. 三调用方落地

| # | 调用方 | 代码入口 | 变更 |
|---|---|---|---|
| 1 | Token 过滤器 | [TokenAuthenticationFilter](../zszj-framework/zszj-spring-boot-starter-security/src/main/java/cn/zszj/framework/security/core/filter/TokenAuthenticationFilter.java) `doFilterInternal`（E30） | 传入 `securityProperties.getTokenParameterEnabled()` |
| 2 | 管理端登出 | [AuthController](../zszj-module-system/src/main/java/cn/zszj/module/system/controller/admin/auth/AuthController.java) `logout` | 同上 |
| 3 | 用户端登出 | [AppAuthController](../zszj-module-member/src/main/java/cn/zszj/module/member/controller/app/auth/AppAuthController.java) `logout` | 同上（member 模块在根 pom 注释排除、不在 reactor，独立编译验证） |

## 5. 两端前端与连接通道证据

- **Web**：`apps/zhongshu-admin-web/src/config/axios/service.ts` — `config.headers.Authorization = 'Bearer ' + getAccessToken()`（含令牌刷新链），一律 Header。
- **小程序**：`interceptor.ts`、`sse.ts`（SSE）、`download.ts`（下载）、文件上传均 `Authorization: Bearer ${token}` Header，无一走 URL 参数。
- **WebSocket**：唯一 `?token=` 通道（`/ws?token=`），由 `tokenParameterEnabled` 默认 `true` 保留。

结论：严格 Bearer 前缀解析对两端**零改动兼容**；参数通道收紧不影响任何现有 Header 客户端。

## 6. 验收对齐（05 文档 ZS-SEC-003 卡）

- **缺失/畸形/失效/重复/错用户类型结果一致**：缺失、畸形三类（`Bearer `/`Bearer`/`BearerXyz`）、重复冲突均统一返回 `null` → Token 过滤器走既有认证失败链（与失效/错用户类型 401/403 一致，见 [错误响应与状态码矩阵](错误响应与状态码矩阵.md)）。
- **URL 不泄露正常会话凭据**：`tokenParameterEnabled=false` 服务端禁参数通道；两端前端本就走 Header。
- **合法双端请求和获准特殊连接通过**：标准 Bearer/裸 Token/大小写/参数回退/WebSocket（默认 `true`）均放行；票据过期/重用/错用途失败属文件票据语义，归 ZS-FILE-004.A。

## 7. 测试证据

| 模块 | 测试 | 用例 | 结果 |
|---|---|---|---|
| zszj-spring-boot-starter-security | `SecurityFrameworkUtilsTest`（`MockHttpServletRequest` 纯单测，无 DB/Redis） | 18 | BUILD SUCCESS |

18 用例覆盖：标准 Bearer、大小写不敏感、裸 Token 兼容、`xBearer y` 子串误解析**回归护栏**、畸形三类、缺失、多空白 trim、参数回退、参数带 Bearer、参数通道关闭、Header 优先、重复头冲突/相同、重复参数冲突、空 Header 回退参数、parameterName 空忽略。

编译验证：`mvn -pl :zszj-spring-boot-starter-security -am test` 18/18（上游 common 8/web 全绿）；`AuthController`（`-pl :zszj-module-system -am -DskipTests compile`）、`AppAuthController`（`-f zszj-module-member/pom.xml -DskipTests compile`，先 `install` security starter 刷新本地仓库）均 BUILD SUCCESS。门禁 `node scripts/ops/run-local-gates.mjs --fast` 10/10（含 G4/G5 文档一致性）。

## 8. 边界与待验收说明

1. 本轮为**代码级解析规范化 + 单元测试**；`tokenParameterEnabled=false` 生产硬化开关、真实代理/日志同步与 WebSocket 端到端连接在真实环境验收归 ZS-SEC-012.B。
2. 文件专用短时票据与取流归 ZS-FILE-004.A（B04）；票据过期/重用/错用途失败同归该项。
3. OAuth2 开放端点凭据（client_secret/basic）传输审查承接 ZS-SEC-002 登记的残余审查点，随 OAuth2 领域开发落地。
4. Token 刷新/撤销/停用生命周期归 M04 的 ZS-LOGIN-001～005，本规范只管传输解析。
5. 本记录不表示任何主任务已验收。
