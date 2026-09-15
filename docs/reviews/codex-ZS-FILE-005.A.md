# codex 评审处置：ZS-FILE-005.A 删除/发布中间态、引用保护与人工对账入口

> 被审对象：worktree 隔离分支 `feat/file-004-a-005-a` 的渐进提交（impl + r0~r3 四轮处置），各弧以 `codex review --commit <SHA>` 评审；`--no-ff` 合并 main（`285ab19c`，与 FILE-004.A 同分支同合并、分别评审）。
> 评审工具：`codex-cli 0.154.0`，模型 `gpt-6-astra`，reasoning effort `xhigh`，sandbox `read-only`，approval `never`。
> 完整 stdout：`outputs/file005a-codex-r{0..4}.log`（UTF-16，gitignored 未入库；结论逐字引用于本文）。

## 交付内容（迁移 `V20260914.013`：infra_file 资产状态列）

- **资产状态**：PUBLISHED（正常可用；存量行迁移默认回填）/ DELETING（删除中间态，可恢复、可对账）。
- **删除流程**：引用保护（存在进行中的交付会话即 REDEEMED 未过期 → 拒绝删除，不误删引用对象）→ PUBLISHED→DELETING 条件中间态转移（并发双删败者按 FILE_DELETE_IN_PROGRESS 拒绝）→ 对象删除 → 记录移除；对象删除失败**保留 DELETING 记录并上抛**（中段失败不假报成功）。
- **竞态闭合**（codex 深挖链）：中间态转移**先行**锁定删除意图——兑换侧据 DELETING 拒绝新建会话、取流侧存量会话由引用保护与对象删除事实兜底；引用检查后置于转移，使命中回退与新建阻断串行化（codex r0 探针实证的「删除-兑换竞态」闭合）。
- **批量删除**：逐项执行、逐项记录（`FileDeleteBatchRespVO`：successIds + failures[id,errorMessage]），中段失败不伪报全成功；前端 `handleDeleteBatch` 同步更新——部分失败如实呈现（warning 列出失败编号），不伪报全成功。
- **人工对账入口**：`GET /infra/file/reconcile/deleting`（列出 DELETING 可恢复记录）+ `POST /infra/file/reconcile/cleanup`（重试清理：对象仍在则重删，已确认缺失则仅移除记录；对账路径同样受引用保护约束）。错误码 1-001-003-032~033。
- **测试**：`FileLifecycleReconcileTest` 6 用例（成功移除/对象失败保留中间态/引用阻塞/过期会话不阻塞/批量逐项/对账列表+重试+幂等）。

## 评审弧（r0→r4，`gpt-6-astra`/`xhigh`/`read-only`）

| 轮次 | 结论 | 要点与处置 |
|---|---|---|
| **r0** | **1×P1 + 2×P2** | P1 删除-兑换竞态（引用检查后可新建兑换，codex 确定性探针实证「删除后仍存活跃 REDEEMED」）→ 转移先行 + 兑换/取流拒 DELETING 串行化；P2 对账遇「对象已缺失」（SFTP delete 对缺失对象抛 SSH_FX_NO_SUCH_FILE）永久卡死 → 存在性探测确认缺失仅移除记录；P2 前端 caller 忽略逐项结果伪报全成功 → API 类型与 handler 同步更新 |
| **r1** | **2×P2** | P2-1 对账回退绕过引用保护（PUBLISHED+活跃会话+缺失对象：守卫回退后仍被仅元数据清理）→ 对账路径前置引用检查、仅元数据清理限 DELETING 记录的存储失败；P2-2 引用保护短暂暴露 DELETING 误伤合法在途取流（回退前窗口）→ 移除取流侧 DELETING 检查（兑换侧阻断保留，r0 P1 闭合不变） |
| **r2** | **1×P2** | 移除取流侧检查重开交错：兑换读 PUBLISHED 后暂停、删除完成、票据 CAS 仍成功（codex 以失败存储删除复现 HEAD 服务 DELETING 记录分块）→ 兑换事务化 + CAS 后同事务复核资产状态（DELETING/移除即回滚） |
| **r3** | **1×P2** | 事务化引入新交错：删除引用检查读到未提交 WAITING → 兑换以 `FOR UPDATE` 锁定文件行持有至提交，与删除侧中间态转移串行化（锁序统一「文件行→票据行」，无死锁）；并发确定性用例登记归 FILE-005.B/联调（避免单元层 flaky） |
| **r4** | **PASS / 0 发现** | 「No actionable regressions were found in HEAD. The row lock is consistent with the existing transaction and deletion flow, and all 18 focused file-delivery and lifecycle tests passed.」 |

**收口依据**：r4 达 0×P0/P1 阀值；五弧逐级收敛（每轮修复引出的更深交错均由 codex 探针/推演捕获并弧内修复）；无延后阻塞项。边界：自动补偿与孤儿对象清理归 FILE-005.B（B05，JOB-002 Outbox 已就绪）。

## Codex 原始结论（r4 逐字引用）

> No actionable regressions were found in HEAD. The row lock is consistent with the existing transaction and deletion flow, and all 18 focused file-delivery and lifecycle tests passed.
