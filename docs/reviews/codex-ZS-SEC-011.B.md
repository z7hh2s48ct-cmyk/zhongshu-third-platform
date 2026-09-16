# ZS-SEC-011.B codex 评审处置（r0→r3 四弧）

- 评审工具：codex（gpt-6-astra / xhigh，`codex review --commit <SHA>`，Java-on-H2/PG 隔离探针实测：跨操作重放复现、摘要碰撞复现、servlet 序列化探针、SQLSTATE 22001 复现、Docker 守护进程实操）
- 评审对象：分支 `feat/sec-011-b`（ZS-SEC-011.B〔B05 Wave5，P1，类别改造〕持久化幂等与 HTTP 重试联验——.A Redis 窗口锁之上的 DB 持久化幂等 + .A 延后 2×P2 收编）
- 结论：**r3 PASS / 0 发现**（四弧收敛：r0 FAIL 1×P1+2×P2 → r1 0×P0/P1+4×P2 → r2 0×P0/P1+1×P2 → r3 PASS/0 发现；0×P0/P1 收口阈值于 r1 即达成，P2 全部弧内修复至归零）
- 说明：评审弧 raw 输出留档会话日志（各轮发现与处置逐条摘录如下，如实登记）。

## 交付内容（impl `585526b0`，24 files +2453/-70；r0 处置 `ee533a81`，5 files +348/-100；r1 处置 `8d97ae4c`，10 files +332/-91；r2 处置 `ace620d6`，1 file +2；合并 `--no-ff` main `27a050b3`，冲突解 `run-pg-regression.mjs` 套件 13 同位→14）

1. **Key/Value 职责切分**（收编 .A codex P2-1/REC-2）：Default/User 解析器 Key 去 argsStr（Key=md5(method+租户+主体+userType) 稳定短键），摘要改未截断脱敏表示（`LogSanitizeUtils.sanitizeArgsUntruncated`，原 `sanitizeArgs` 行为零变化）——等长异尾（>2048）入参不再碰撞，「同键异参冲突」在默认路径可达。
2. **.A P2-2 收编**：Redis 读回失败告警携 key 的 md5 指纹不落原值。
3. **持久化幂等**（`@Idempotent(persistent=true)`）：`PersistentIdempotentStore` SPI 定义于 protection、`JdbcPersistentIdempotentStore`（JdbcTemplate）落地于 infra（infra→protection 无环，循 JOB-002 Outbox infra 承载先例）；INSERT RUNNING→业务→markSuccess 经 **MANDATORY 参与调用方事务原子提交**（无事务 fail-closed）；并发由 **DB 唯一约束单层兜底**（PG=`ON CONFLICT DO NOTHING` / H2=语句级冲突捕获，方言懒解析；持久化模式不走 Redis，双层竞态结构上不存在）；丢响应重放**返回原结果快照**（按声明返回类型反序列化；快照缺失/损坏降级状态级复用 900 绝不重执行业务）；失败按 `deleteKeyWhenException` 删记录/置 FAILED；**强制登录主体**且主体因子并入持久化键；`IdempotentAspect` 显式 `@Order(LOWEST_PRECEDENCE)` 钉「方法安全先于幂等切面」次序。
4. **评审弧内加固（r0/r1/r2 处置）**：①持久化键并入操作身份——`storeKey=md5(actionScope:resolvedKey:tenant:userId:userType)`（Method 规范串，代理类名无关/跨进程稳定），重放校验 actionScope 一致性不符 fail-closed 冲突拒绝（堵「取消命中创建快照」跨操作重放，r0 P1）；②重放身份摘要无损化——原始业务入参 JSON 的 **keyed SHA-256**（`computeArgsDigest`），仅敏感字段不同的请求按冲突拒绝而非复用快照；摘要管线与日志脱敏分属两条管线；③摘要降级路径复用「过滤后」入参数组（不重新序列化 servlet 对象破坏二进制响应）；④`action_scope` 列宽 256→1024（迁移未合并直接改）+ 超长降级 `sha256:<64hex>` 定长表示（落库/重放同口径，storeKey 仍用全量串）；⑤**HMAC pepper 部署期注入**——`ZSZJ_SECURITY_IDEMPOTENT_DIGEST_SECRET`（≥32 字符，循 ZSZJ_* 合同）：过短构造即抛、未配置持久化幂等拒绝启用（fail-closed 不静默降级；Redis 窗口模式不受影响），轮换=存量摘要失效（重放冲突拒绝、无重复写风险）；⑥快照反序列化走 `ObjectMapper.readValue` 无日志路径（`JsonUtils.parseObject` 内部先 log 全输入再抛，原文含凭据可达日志）；泄漏测试 appender 同挂 IdempotentAspect+JsonUtils 双 logger（对原始缺陷真正打红）。
5. **迁移与回归**：`V20260916.101__infra_persistent_idempotent.sql`（uk 唯一 + ck 三态 CHECK + action_scope varchar(1024)，H2 `create_tables.sql`/`clean.sql` 同步）；`run-sec011b-verify.mjs`（8 断言：迁移结构/ON CONFLICT 抢占/8 独立连接并发恰 1 胜者/状态机 CHECK/跨进程重放不重复写/失败双路径/快照完整性/超长 action_scope 落库）注册 `run-pg-regression.mjs` **第 14 套件**（合并时与 OPS-002.B 的 13 同位冲突解为并存）。

## 评审弧（探针逐轮实锤）

| 轮次 | 结论 | 发现 → 处置 |
|---|---|---|
| r0（`--commit 585526b0`） | **FAIL** 1×P1+2×P2 | **P1** 跨操作结果重放——两个持久化方法同 Expression 业务键同参时 storeKey 相同、actionScope 落库但不进键不校验，「取消」可命中「创建」的 SUCCESS 快照不执行取消 → 键并入 actionScope+重放一致性校验（`ee533a81`）。**P2-1** 脱敏摘要碰撞——仅 password/token 不同的请求经脱敏摘要相同，第二请求被当重复返回首请求快照（比 .A Redis 拒绝行为倒退）→ 原始入参 keyed SHA-256 非有损摘要。**P2-2** 快照反序列化失败原文落日志（`JsonUtils.parseObject` 内部先 log 全输入再抛）→ 无日志反序列化路径+双 logger 断言。 |
| r1（`--commit ee533a81`） | 0×P0/P1，4×P2 | **P1 RESOLVED**（codex 确认）。P2-A 摘要降级路径重引入 servlet 入参（抛异常 getter DTO+HttpServletResponse 复现 getWriter/getOutputStream 被调用）→ 过滤一次两路共用。P2-B `action_scope` varchar(256) 溢出（两长参数类型方法 135→262 字符 SQLSTATE 22001，持久化方法业务执行前直接失败）→ 列宽 1024+超长定长降级。P2-C HMAC pepper 硬编码可离线枚举低熵凭据 → 部署期注入+fail-fast/fail-closed。P2-D 泄漏断言挂错 logger（JsonUtils 泄漏事件对 IdempotentAspect appender 不可见，回退漏洞实现仍假绿）→ 双 logger。 |
| r2（`--commit 8d97ae4c`） | 0×P0/P1，1×P2 | 四项 P2 处置全部 PASS（34 切面测试+8 PG 检查常规路径全绿）。残留：`run-sec011b-verify.mjs` Docker 端口冲突重试路径——`docker run` 绑定失败时同名容器已建（created 态），仅换端口重试必因容器名冲突再败（本地 daemon 复现）→ catch 内先 `docker rm -f`（`ace620d6`）。 |
| r3（`--commit ace620d6`） | **PASS / 0 发现** | 「removes the failed container before retrying, preventing container-name conflicts without disabling final cleanup…no actionable regressions were identified」；mock 启动/重试全场景断言通过。 |

## 验证

- TDD：protection 全量 **51/0 ×3 稳定**（PersistentModeTest 21 + AspectTest 13 + DefaultResolver 7 + 限流/签名 10）；infra `-am` 全量反应堆 **BUILD SUCCESS**（common 61/web 18/mybatis 13/biz-tenant 61/protection 51/biz-ip 9/excel 21/infra 440 Skipped 10）；system/zszj-server test-compile 通过。
- 门禁：`run-local-gates.mjs --fast` **10/10**。
- PG：`run-sec011b-verify.mjs` **8/8 ×3 稳定**（含 8 独立连接并发同键恰 1 行恰 1 胜者、跨会话重放不重复写、64 位 keyed 摘要不截断）；注册回归第 14 套件。
- 合并后集成验证（main `27a050b3`）：`ApiInventoryTest`/`ModuleWhitelistTest` + protection 定向 + OPS-002.B/SEC-011.B 两 PG 套件（结果回登 docs/05）。
- 四项退出条件证据：丢响应=快照复用测试+PG P5；并发=切面并发+H2 4 线程+PG 8 连接 P3；重启不重复写=新实例重放+跨会话 SQL 证据；重试重检授权=`@PreAuthorize` 次序合同测试（撤权重放被拒、快照不返回、业务不重执行）。

## 登记边界

1. 快照按声明返回类型反序列化：HTTP JSON 形状不变；Java 直调泛型元素退化为 LinkedHashMap 不做保真。
2. 业务异常不快照：失败重试语义由 `deleteKeyWhenException` 决定；FAILED 保留路径重放恒 900，不回放原异常。
3. 持久化幂等要求**事务内 service 方法 + 登录主体**，不满足即 fail-closed 拒绝；不做匿名持久化幂等。
4. **部署合同新必填项**：启用 `persistent=true` 的环境必须注入 `ZSZJ_SECURITY_IDEMPOTENT_DIGEST_SECRET`（≥32 字符，循 OPS-002.A 配置注入合同体系）；缺省应用可启动但持久化模式拒绝（Redis 窗口模式照常）；pepper 轮换=存量持久化摘要全部失效（重放冲突拒绝、客户端重试即可，无重复写风险）。
5. 记录保留期/清理任务、容量与性能标定不在本卡（归 B11/WP-19 实测）；不声称生产级吞吐/时延。
6. 跨实例时钟：`complete_time` 应用侧单一时钟源（循 outbox）；状态机不依赖时钟（MANDATORY 已结构性排除悬挂 RUNNING）。
7. Redis 窗口模式（非 persistent）行为维持 .A 原状；Expression/User 解析器 Redis 路径的租户作用域缺口归 REC-1（显式登记后续：跨 SEC-010+011 解析器族重构/IMP-7 生产端 clientIP/MIN-9 畸形头 400）。
8. HTTP 过滤器链端到端重放联验归 ZS-SEC-012 既有全链回归入口，本卡钉方法安全×幂等切面次序 + 主体隔离合同。
9. 主卡 ZS-SEC-011 维持「开发中」（.A+.B 均交付，待验收须真实环境放行）。
