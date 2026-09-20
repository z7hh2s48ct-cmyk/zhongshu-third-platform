# codex 评审处置：ZS-DB-010 账号唯一约束与并发兜底

- 评审对象：`feat/db-010`（worktree `.wt/zszj-wt-db-010`），迁移 `V20260920.001__system_users_account_unique.sql` + `AdminUserServiceImpl`（normalizeUsername/translateDuplicateKey/importUserList/updateUserProfile）+ `run-db010-verify.mjs`（PG 回归第 17 套件）+ `run-pg-regression.mjs` 登记 + 2 个测试类
- 评审方式：codex exec（gpt-6-astra / model_reasoning_effort=high / read-only），r0→r2 共三弧；弧内携带真实 H2 探针与 PG 报错格式核验（codex sandbox 无法跑 Docker/mvn，PG 实跑由本地补证）
- 收敛结论：**r2 PASS — zero findings（0×P0~P3 全零）**；两处 Round 2 修复经复核正确，无遗留可执行 P1/P2
- 合并：`--no-ff` main `7ce92174`（分支单卡提交 `64198df9`，经 r0→r1→r2 amend 收敛；与 V1.79 gov 提交 `55a1c159` 零文件重叠干净合并，merge-base `6dbb4228`）

## 弧次与发现

| 弧 | 发现 | 要点与处置 |
|---|---|---|
| r0 | FAIL 1×P1+3×P2 | **P1** 导入失败后事务未恢复即续跑（AdminUserServiceImpl.java:723-725）：导入行撞 DB 唯一冲突后 PG 中止 `importUserList` 事务，`catch DuplicateKeyException` 不恢复事务，后续查询撞 SQLSTATE 25P02、先前成功行回滚；若冲突发生在末行，响应甚至会列出未提交的创建 → 处置：两个 import catch 块改 rethrow 转译后异常令整批失败（不再同事务续查）。**P2** username 等值查询索引被误删（V20260920.001.sql:75）：`lower()` 表达式/部分唯一索引服务不了 `WHERE col=?` 等值查询 → 处置：保留 idx_01/02/03，唯一约束由新增部分唯一索引承担。**P2** profile 更新未接冲突转译（updateUserProfile）：用他租户 mobile/email 改资料越过租户内预检、产生未处理 `DuplicateKeyException`，端点返回泛化内部错误而非 `USER_MOBILE_EXISTS`/`USER_EMAIL_EXISTS` → 处置：updateUserProfile 写入外层接入 translateDuplicateKey。**P2** 并发回归未跑重叠事务（run-db010-verify.mjs:121-125）：`spawnSync` 等首个 psql 退出并提交后才起第二个，P3/P0 实为顺序插入而非并发 → 处置：改异步 spawn 双会话，A `BEGIN;INSERT;pg_sleep;COMMIT` 持锁未提交时起 B。 |
| r1 | FAIL 2×P2 | **P2** 约束名应按标识符精确比对而非全消息子串（AdminUserServiceImpl.java:600-604）：跨租户/并发邮箱冲突涉及 `uk_system_users_mobile@example.com` 时，PG 把该邮箱回显进 DETAIL 文本，首个 `contains("uk_system_users_mobile")` 子串检查会误返回 `USER_MOBILE_EXISTS`（实为 `uk_system_users_email` 触发）→ 处置：正则 `unique constraint "([^"]+)"` 精确提取约束名后 `equals` 比对，新增含冲突值 DETAIL 的真实异常回归测试。**P2** 并发会话应各自独立完成计时（run-db010-verify.mjs:105-106）：两个 elapsed 均在 `Promise.all` 后计算，B 耗时错误包含等待 A 的时间 → 可能伪证事务重叠 → 处置：每进程 close/error 独立打点 elapsed，并轮询 `pg_stat_activity` 确认 A 处于 `pg_sleep`（active）作为确定性同步点后再起 B。 |
| r2 | —（终弧） | **PASS — zero findings**：两处 Round 2 修复逐一复核正确（约束名精确提取杜绝 DETAIL 值子串误判；每进程独立计时 + pg_stat_activity 持锁同步杜绝伪证重叠），无遗留可执行 P1/P2；JavaScript 语法与 diff 检查通过，PG 实跑因 codex sandbox Docker 权限受限（本地已补证 8/8）。 |

## 验证

- 定向单测 63/63（`AdminUserServiceImplTest` 49 + `AdminUserServiceImplSessionInvalidateTest` 14，BUILD SUCCESS）；含 r1 新增 `testCreateUser_translatesDuplicateKey_emailNotConfusedByDetailValue`（DETAIL 含 mobile 约束名的邮箱冲突仍归 `USER_EMAIL_EXISTS`）
- 真实 PG17 `run-db010-verify.mjs` 8/8（P0 复现回归/P1 结构/P2 历史冲突 fail-loudly/P3 并发兜底/P4 大小写规范化/P5 删除重建/P6 空值多行/P7 跨租户全局唯一；P0/P3 显示 A 持锁同步=true、B 独立耗时 ~2.8s）
- 登记 `run-pg-regression.mjs` 第 17 套件；主树 `run-local-gates --fast` 10/10（含 G8 Flyway/G7 PG 合同）
- codex r2 独立复核：JS 语法 + diff 检查通过；两处修复正确性确认

## 登记边界（范围外与延后）

1. **不创建 Account/Membership/Organization 新表** → 归 ZS-IAM-001（B07 后续）；本卡仅在既有 `system_users` 上以 DB 唯一约束收口账号唯一性。
2. **不做组织级/字段级授权** → 归 ZS-PERM-002.B/003。
3. **禁止擅自删重**：迁移段 1 对存量冲突 fail-loudly（`RAISE EXCEPTION` 打印冲突键与行 id）令升级失败交人工按 D-09 批准方案处置，绝不自动删重/合并；新库/种子无重复则干净通过。
4. 真实环境（B07 批次）多端联调与生产库历史冲突处置演练归批次放行；本卡 SQL 套件聚焦约束/并发契约面。
