# codex 评审处置：ZS-LOGIN-005.B 可持久恢复的缓存失效补偿——Outbox 预写与 Sink 幂等重放

- 评审对象：`feat/login-005-b`（worktree `.wt/zszj-wt-login-005-b`），OAuth2 撤销 Outbox 预写 + `OAuth2TokenRevocationCompensationSink` + `run-login005b-verify.mjs`（回归第 16 套件）+ 5 个测试类
- 评审方式：codex exec（gpt-6-astra / model_reasoning_effort=xhigh / bypass sandbox），r0→r2 共三弧；弧内携带真实 H2 探针、UTC 时区实测、PG 内存变异验证
- 收敛结论：**r2 PASS/0×P0~P3 全零（0 发现）**；HEAD 前后校验一致，未发生弧内 git 状态变更
- 合并：`--no-ff` main `75f4a40e`（分支提交链 `7bdffbb1` impl → `2cd3a26c` PG 脚本补交 → `08d2aa0d` P3 重写 → `4a8e1549` r0 处置 → `99926662` r1 处置；`46fd2370` 台账登记经 merge main 并入）

## 弧次与发现

| 弧 | 对应处置 | 发现 | 要点 |
|---|---|---|---|
| r0 | `4a8e1549` | 2×P1+6×P2+1×P3 | P1-1 事件租户归属错位（跨租户管理端撤销时事件落调用者租户、Sink 反查不可见 → 补偿静默丢失）；P1-2 提交宣称收编 PERM-004.A/LOGIN-003 延后项但未实现；P2 时钟违反 SEC-009.A GMT+8 合同（Sink 用 LocalDateTime.now()）；P2 parseObject 失败日志泄漏 payload 原文 + 异常消息拼 raw payload；P2 三个裸 new 旧单测缺 provider 注入 NPE；P2 P3 探针未真正行使 SKIP LOCKED（单语句 autocommit 纯 SELECT）；P2 P8/P4/P5/P7 夹具自证不验证行为；P2 P6 未覆盖 LOGIN-003 交错却宣称收编；P3 actorType/actorId/traceId 缺失。处置：TenantUtils.execute 以被撤销令牌行租户 append（5 调用点）；收编宣称删除、移交按登记原文替代去向登记；Sink 改 DateUtils.now() + parseObjectQuietly + 受控摘要日志；旧夹具注入 null provider mock；P3 重写持锁会话三段式；P8 重写同事务原子性契约探针；SinkTest 用例 7 改「行/缓存活跃+仅载荷过期」+缓存原样断言；actor 上下文解析 |
| r1 | `99926662` | 1×P1+2×P2+1×P3 | P1 脚本 P6 残留收编宣称 + 台账未登记（处置：宣称清除 + 主树 reviews/README「ZS-LOGIN-005.B 移交登记」段 `46fd2370`：M1 权限缓存驱逐补偿→ZS-PERM-004.C、M2 LOGIN-003 交错→ZS-SYS-001 PG 扩展，并 merge main 入分支）；P2 P8 仍无真撤销（处置：预置活跃令牌 → 事务内 deleted=1 + 预写，回滚/提交双侧断言）；P2 时钟夹具未同步（codex `-Duser.timezone=UTC` 实测 3 例失败；处置：4 类 25 处 LocalDateTime.now() → DateUtils.now()）；P3 actorType 按共享方法硬编码错标（处置：resolveRevocationActorType() 按 SecurityContext 主体解析，签名收敛） |
| r2 | —（终弧） | **0×P0~P3 全零** | 四项处置逐一核验通过；UTC 下五类测试 21/21、PG 8/8、`git diff --check` 通过；确认 `46fd2370` 纳入分支、merge 未改登录实现 |

## 验证

- PG 套件 `run-login005b-verify.mjs` 8/8（P3：A 持锁=true、locked=2、B=3、C=2、确定性=true；P8：回滚=活跃令牌 1/事件 0、提交=撤销令牌 1/事件 1）
- 定向 H2 8 测试类 BUILD SUCCESS（默认时区）；SinkTest `-Duser.timezone=UTC` 探针 7/7（25 处夹具时钟统一 DateUtils 后）
- 主树 `run-local-gates --fast` 10/10
- codex r2 独立实测：UTC 五类 21/21、租户/actor 解析三面正确、P3 护栏变异（移除 SKIP LOCKED）转红

## 登记边界（移交与延后）

1. **M1 权限缓存驱逐失败可靠补偿** → 立后续子项 ZS-PERM-004.C（失败键清单持久化或版本机制；原 PERM-004.A 延后项，批次随权限域排期）——本卡仅覆盖 OAuth2 令牌域补偿。
2. **M2 LOGIN-003 双连接真实事务交错回归**（兑换↔改密两序/同 code 并发消费/建令牌失败回滚/MEMBER 边界）→ 并入 ZS-SYS-001 真实 PG 并发回归扩展（随 B06 ZS-SYS-001.B 排期）。
3. Sink 过期 skip/重放撤销的行为级 Java-on-PG 联验归 D-07 正式联验链（循 JOB-003/FILE-005.B 登记）；SQL 套件聚焦契约面。
4. 补偿链对 Redis 持续不可用的兜底依赖 JOB-004 DEAD 台账人工恢复路径。
