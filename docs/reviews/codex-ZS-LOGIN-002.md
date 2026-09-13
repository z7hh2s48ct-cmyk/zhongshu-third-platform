# ZS-LOGIN-002 codex 评审处置（r0→r1 两弧）

- 评审工具：codex（gpt-6-astra / xhigh，read-only sandbox）
- 评审对象：分支 `feat/login-002`（ZS-LOGIN-002 实现刷新并发、重放与退出竞态控制；worktree `E:/zszj-wt-login-002`，与 ZS-LOGIN-004 同分支串行、分别评审）
- 结论：**r1 CLEAN / 0 发现**（评审收敛）
- 证据说明：本评审的原始 stdout 未随当次会话保留于 `outputs/`（暂存目录清理）；r1 最终结论自 codex 会话日志（`~/.codex/sessions/2026/09/13/rollout-2026-09-13T02-17-56-*.jsonl`）恢复，r0 发现以修复提交 `f913bac1` 提交说明为准，如实登记。

## r0（commit 63bfe97d，impl）FAIL 2×P2

RED 复现先行：`OAuth2TokenServiceImplRefreshConcurrencyTest` 6 用例在修复前全失败——8 线程并发同一 refreshToken 产生 8 个有效访问令牌（expected 1 but was 8）、重试风暴每轮叠加 4 个、退出后晚到刷新复活会话（expected 0 but was 1）、会话代际不可定位。

| 级别 | 发现 | 处置 |
|---|---|---|
| P2 | 批量撤销 `removeAccessToken(Long,Integer)` 未纳入与刷新/单令牌撤销一致的锁序，并发下存在死锁窗口 | r0 修复 `f913bac1`：批量撤销按 refresh-token **稳定锁序**（排序后逐个加锁）防死锁，并补 `@Transactional`（行锁生效前提：该方法此前无事务，行锁随每条语句自动提交立即释放） |
| P2 | 会话代际键 TTL 秒级粒度：近到期（<1s）凭据的代际键向上取整秒后永久残留 | r0 修复 `f913bac1`：近到期代际键改**毫秒 TTL** 防永久泄漏 |

## r1（commit f913bac1）CLEAN / 0 发现

codex 复评结论（自会话日志恢复，findings=[]，overall_correctness="patch is correct"，confidence 0.88）：

> "No actionable regressions were found in the lock-order changes, millisecond TTL handling, or affected callers. Existing reports show the added tests passing; tests were not rerun in the read-only environment."

## 交付摘要（对应 docs/05 卡片）

- `OAuth2RefreshTokenMapper.selectByRefreshTokenForUpdate`（MyBatis-Plus Wrapper + `last("FOR UPDATE")`，避开裸 `@Select` 手写 resultMap 与 PG boolean `deleted=0` 兼容问题）；选型「优先行锁」：PG/MySQL/H2(MVStore) 原生语义、不新增 DB 列、不引入 Flyway 迁移（避开与 ZS-DB-019.B 耦合）。
- `refreshAccessToken` 行锁临界区内完成「读刷新令牌→校验 Client→删旧访问令牌→插新访问令牌→登记代际」；同一会话刷新串行化，任一时刻恰 1 个有效访问令牌，代际号==成功刷新次数。
- `removeAccessToken(String)` 同锁序（先锁刷新令牌行再撤销访问令牌）+ 获锁后重读，把并发刷新已提交的新代际一并撤销——退出与晚到刷新两种交错都不复活会话。
- 会话代际/重放状态只存 Redis（`OAuth2AccessTokenRedisDAO` 5 个新方法 + `RedisKeyConstants` 2 个 key），best-effort try/catch+warn 降级；代际 INCR 刻意置于 DB 插入成功之后防回滚虚高。
- 重放（旧代际）拒绝沿用既有 401「访问令牌不存在」语义（不新增业务错误码、不破坏前端/网关 401→跳登录契约、不改 ZS-LOGIN-001 门控）；决策为「沿用原 refresh token + 会话代际号 + 重放检测」，不轮换刷新令牌。

## 验证

- 新增并发测试 6/6 GREEN；`zszj-module-system` 全量 **507 用例 / 0 失败 / 0 错误 / 8 跳过，BUILD SUCCESS**
- ZS-LOGIN-001 共存：`OAuth2TokenServiceImplTest` 17/17、`OAuth2TokenServiceImplCompatTest` 2/2；调用方 `OAuth2GrantServiceImplTest` 7/7、`AdminAuthServiceImplTest` 17/17、`AdminUserServiceImplTest` 40/40
- 并发测试连跑 5 轮无抖动（栅栏同步 + `validOAuthClientFromCache` 注入延时放大竞态窗口，修复后该延时位于行锁临界区内，同时验证锁确实被持有）
- 合并：`--no-ff` 合并回 main（`087be5f0`，与 ZS-LOGIN-004 同 merge）
- 文档同步复验（2026-09-13，本收口补丁）：当前 main 复跑目标测试类全绿（`outputs/login002-004-docsync-test.log`）

## 延后/边界（非阻塞，登记后续处置）

1. 以「已被前一次刷新取代的旧访问令牌串」调用 `removeAccessToken` 时按既有契约早退返回 null、不撤销当前代际会话（现实退出路径携带的即当前令牌，不受影响）；如需「凭旧令牌串杀会话」需 accessToken→refreshToken 反查（含软删行）——ZS-LOGIN-003 已以「access 反推 ∪ refresh 直查」全集撤销覆盖该面，本遗留已被后置任务消化。
2. H2 与 PG 行锁等待/超时语义差异（H2 LOCK_TIMEOUT 默认 1000ms、PG 默认无限等待）：H2 下验证串行化效果，PG 侧锁等待与死锁检测归 ZS-DB-019.B PG 回归 / ZS-SYS-001.A 真实 PG 批次复验。
3. Redis 抖动时代际登记 best-effort 降级（不阻断合法刷新）：令牌 DB/缓存一致性恢复与可靠补偿归 ZS-LOGIN-005.A/.B（.A 已交付 DB 权威校验）。
