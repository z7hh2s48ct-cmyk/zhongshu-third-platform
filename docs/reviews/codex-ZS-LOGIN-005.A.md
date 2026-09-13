# ZS-LOGIN-005.A codex 评审处置（r0→r3 四弧）

- 评审工具：codex（gpt-6-astra / xhigh、high，read-only sandbox）
- 评审对象：分支 `feat/login-005-a`（ZS-LOGIN-005.A 权威撤销校验与提交后缓存失效）
- 结论：**r3 PASS / 0 发现**（评审收敛）

## 交付内容（impl commit 4208bdc0）

1. **DB 权威校验（checkAccessToken）**：缓存命中后回源核验——普通令牌查 access 表、gate 合成令牌查 refresh 表；核验使用 `@Select count + @Options(flushCache=TRUE)` 专用语句（绕开 MyBatis SESSION 一级缓存旧快照，`@TenantIgnore` 与 PG/H2 基线 int2 deleted 一致）；查无即已撤销 → 自愈（`revokeWithTombstone`：墓碑 + evict，各自容错）+ 401；DB 查询异常失败关闭（安全默认）并告警。
2. **事务结束单次失效/发布**：缓存失效类动作（墓碑/删缓存/代际键，`List<Runnable>` 逐项 try/catch 隔离）与发布类动作（新令牌写缓存）统一注册到 `TransactionSynchronization.afterCompletion`——COMMITTED 即提交后执行；ROLLED_BACK / STATUS_UNKNOWN 幂等兜底（回滚补偿）或保守清理（发布类），同时覆盖「前序同步回调异常跳过 afterCommit」情形；无事务上下文立即执行。
3. **故障语义**：Redis 动作失败不阻断 DB 提交，WARN 结构化日志（脱敏 token、剩余 TTL、步骤序号、引导自愈修复路径）；可靠重放补偿归 ZS-LOGIN-005.B（B05 JOB-002）。

## 评审弧

| 轮次 | 结论 | 发现 → 处置 |
|---|---|---|
| r0（`--commit 4208bdc0`） | FAIL 1×P1 + 4×P2 | P1 MyBatis SESSION 一级缓存使权威核验可能读到撤销前旧快照（长事务内 SqlSession 复用）→ 修复 `b155d307`：flushCache=TRUE count 专用语句；P2×4（UNKNOWN 状态未处理 / 失效 forEach 整体中断 / 修复证据缺少对象与步骤 / 测试未覆盖故障注入）→ 同 commit 修复 |
| r1（`--commit b155d307`） | FAIL 2×P2 + P3 | P2 自愈路径 markRevoked/delete 串行无隔离、Redis 异常误记为 DB 异常 → 修复 `e8d36aaf`：复用 revokeWithTombstone；P2 测试未证 SESSION 重查/UNKNOWN/前序回调异常 → 补 3 测试；P3 afterCommit+afterCompletion 双跑翻倍 → 统一改 afterCompletion 单次执行 |
| r2（`--commit e8d36aaf`） | FAIL 1×P2 | 长事务重核验测试被 T2 服务级撤销短路（缓存也删了，权威 count 未被执行）→ 修复 `4df6993c`：T2 改 mapper 直改 DB 行保留缓存，补「缓存仍在/自愈 evict」断言 |
| r3（`--commit 4df6993c`） | **PASS / 0 发现** | — |

## 验证

- TDD：RED 先行（幽灵缓存放行 / 回滚残留新令牌缓存 2 用例先失败）→ GREEN。
- 全量回归：OAuth2 全部测试类 + AdminUserServiceImplSessionInvalidateTest = **132 tests / 0 failures，BUILD SUCCESS**（新增 CacheConsistencyTest 8 + GateOnTest 1 + AuthorityUnitTest 5）。
- 合并：`--no-ff` 合并回 main `10552888`。

## 延后/边界（非阻塞）

1. 真实 PG 提交失败注入与双物理连接故障演练 → 归 ZS-SYS-001.A（真实 PG 回归）；UNKNOWN 路径已以保守清理逻辑覆盖并登记。
2. 可靠重放补偿任务（失败键清单持久化 + 定时重放）→ ZS-LOGIN-005.B（B05，前置 ZS-JOB-002），本子项以「DB 权威核验自愈 evict」为修复路径、无需 Outbox。
3. 权威核验为每请求一次 count 主键/索引查询的性能影响 → 验收时按批次真实环境复验；Redis 仍承担用户信息组装加速。
