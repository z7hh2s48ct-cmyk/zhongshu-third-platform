# codex 评审处置：ZS-IAM-002 组织任职模型与服务端组织上下文

- 评审对象：`feat/iam-002`（worktree `.wt/zszj-wt-iam-002`），迁移 `V20260921.001__system_organization.sql` + `V20260921.002__system_membership.sql` + `OrganizationServiceImpl`（隔离级分级策略）+ `MembershipContextResolver`（OAuth2 Token 组织上下文复验）+ `LoginUser`（IAM 上下文字段扩展）+ `run-iam002-verify.mjs`（PG 回归第 18 套件）+ `run-pg-regression.mjs` 登记 + 测试类
- 评审方式：codex exec（gpt-6-astra / model_reasoning_effort=high / read-only），r0→r4 共五弧；弧内携带真实 H2 探针与 PG 报错格式核验（codex sandbox 无法跑 Docker/mvn，PG 实跑由本地补证）
- 收敛结论：**r4 PASS — zero findings（0×P0~P3 全零）**；SERIALIZABLE 隔离级收窄至仅 updateOrganization（重挂路径），create/delete 回退 READ COMMITTED 保 #8b 锁后新鲜复验
- 合并：`--no-ff` main `fc3f782a`（分支单卡提交 `b8182e56`，经 r0→r1→r2→r3→r4 amend 收敛；与 V1.80 gov 提交 `13e8b796` 零文件重叠干净合并，merge-base `55a1c159`）

## 弧次与发现

| 弧 | 发现 | 要点与处置 |
|---|---|---|
| r0 | FAIL 2×P1+7×P2 | **P1** OAuth2 checkAccessToken 未复验组织上下文（provider 空则跳过校验）：Token 内嵌的组织上下文未在每次调用时重新解析比对 → 处置：增加 provider 空值检查和组织上下文一致性比对。**P1** MembershipContextResolver 拒绝生效期未到任职：未显式校验任职的 effective_date ≤ now < expire_date → 处置：增加时间范围校验。**P2** 任职查询匹配部分唯一索引：role_ids 字段长度限制导致多角色聚合失败 → 处置：改为 text 类型。**P2** 账号租户归属校验：跨租户账号访问未严格校验 → 处置：增加租户归属验证。**P2** 任职 (#7)/组织重挂 (#8) 串行化防 write-skew：并发操作可能导致环或死锁 → 处置：引入行锁和祖先链锁定。**P2** 组织重挂锁完整祖先链：仅锁定当前节点不足以防止大环 → 处置：向上遍历锁定整条祖先链。**P2** 删除引用计数：删除组织时未复查成员/子组织计数 → 处置：锁后新鲜读取 selectByIdForUpdate 重新计数。**P2** 环校验上限：visited 集合耗尽时静默通过 → 处置：深度耗尽即拒绝 fail-closed。 |
| r1 | FAIL 1×P1+3×P2 | **P1** OAuth2 checkAccessToken 比对内嵌上下文 vs 当前解析不一致即拒 401：holder lambda 用赋值表达式导致 Callable 重载包装异常绕过 catch → 处置：改为 void 块 lambda 强制 Runnable 重载。**P2** MyBatis SESSION 本地缓存陈旧读：FOR UPDATE 查询不失效普通查询缓存 → 处置：锁后用 selectByIdForUpdate 新鲜读。**P2** 祖先锁覆盖集陈旧 write-skew：SERIALIZABLE 下快照定格在阻塞于行锁前 → 处置：SERIALIZABLE 只 scope 到 updateOrganization，create/delete 回退 READ COMMITTED。**P2** 环校验 visited 检测耗尽上限：深度耗尽时应 fail-closed 而非静默通过 → 处置：增加 MAX_ANCESTOR_DEPTH=256 限制。 |
| r2 | FAIL 3×P2 | **P2** checkAccessToken holder lambda 改 void 块：确保 resolver 抛异常转 401 测试覆盖。**P2** 组织层级变更 SERIALIZABLE 隔离级串行化：防止跨事务大环 write-skew。**P2** 祖先锁覆盖集陈旧 write-skew：create/delete 回退 READ COMMITTED 保锁后新鲜复验。 |
| r3 | PASS 1×P2 | **P2** SERIALIZABLE 引入 delete 快照定格回退 #8b：delete 的 post-lock 计数在 SERIALIZABLE 下使用旧快照误判 → 处置：SERIALIZABLE 只用于 updateOrganization（唯一能形成跨事务大环的路径），create/delete 保持 READ COMMITTED（二者只挂叶子/删节点，不可能自成环，依赖锁后新鲜复验）。 |
| r4 | PASS/0×P0~P3 | **终弧裁定 PASS**：隔离级分级策略正确——updateOrganization 用 SERIALIZABLE 防止 re-parent 形成大环，create/delete 用 READ COMMITTED 保证锁后新鲜计数；Lambda 重载修复避免异常绕过；MyBatis 缓存陈旧读通过 selectByIdForUpdate 规避；环校验有深度限制 fail-closed。无遗留可执行 P1/P2。 |

## 验证

- 定向单测 119/119（`OrganizationServiceImplTest` 41 + `MembershipContextResolverTest` 41 + `OAuth2TokenServiceImplTest` 37，BUILD SUCCESS）；含 r1 新增 `checkAccessToken_rejectsWhenEmbeddedContextMismatch`（Token 内嵌上下文与当前解析不一致时返回 401）、r2 新增 `testDeleteWithStaleSnapshot_failsClosed`（SERIALIZABLE 下陈旧快照不误判删除）
- 真实 PG17 `run-iam002-verify.mjs` 8/8（组织创建/更新/删除/成员绑定/跨租户隔离/role_ids JSON 聚合/部分唯一索引/并发环校验；A 持锁同步=true、B 独立耗时 ~2.5s）
- 登记 `run-pg-regression.mjs` 第 18 套件（连同 DB-010 #17 并存）；主树 `run-local-gates --fast` 10/10（含 G8 Flyway/G7 PG 合同）
- 合并后重验证：IAM 单测 119 BUILD SUCCESS✅；biz-tenant SecurityChainJointRegressionTest 隔离 3/3 全绿判定 flaky 非回归；module-system 897/2F+3E 与基线同源零新增失败
- codex r4 独立复核：JS 语法 + diff 检查通过；隔离级分级策略正确性确认（re-parent 唯一能形成大环、create/delete 无环风险）

## 登记边界（范围外与延后）

1. **不做组织级授权（PERM-002.B）** → 归 ZS-PERM-002.B；本卡仅在 IAM 层建立组织/任职数据结构与上下文解析。
2. **不做字段级权限** → 归 ZS-PERM-003；本卡不修改既有数据权限规则。
3. **真实环境（B07 批次）多端联调** → 归批次放行；本卡 SQL 套件聚焦约束/并发契约面。
4. **组织层级变更 SERIALIZABLE 性能影响** → 监控归 B11（ZS-OPS-002.C）；本卡仅确保正确性，性能优化延后。
