# codex 评审处置：ZS-IAM-004 任职生命周期与历史归属迁移

- 评审对象：`feat/iam-004`（worktree `.wt/zszj-wt-iam-004`），`MembershipServiceImpl`（生命周期失权联动 D1 方案 B + `shouldRevokeSessions` 精确失权判定）+ `LogRecordConstants`（生命周期审计常量）+ `MembershipServiceImplLifecycleTest`（10 用例）+ `MembershipServiceImplTest`（补测）+ `run-iam004-verify.mjs`（迁移前后核对/恢复方案 PG 回归第 19 套件，5 用例 R1-R5）
- 评审方式：codex exec（gpt-6-astra / model_reasoning_effort=high / read-only），r0→r1→r2 共三弧；codex sandbox 无法跑 Docker/mvn（PG 实跑与 Java 单测由本地补证），r2 携带真实签发/撤销路径 `selectByIdForUpdate` 锁协议核验
- 收敛结论：**r2 PASS — zero findings**；P1（rollback SQL 误删业务任职/孤儿流水）以「物化纯回填 ID 集 + IS DISTINCT FROM 三值逻辑 + 两条 DELETE 限定该集」闭合；P2（登录签发与生命周期撤销的幻影写竞态）经核实为**登录签发层既有跨切面缺陷**，以 pre-existing / out-of-scope / tracked-separately 收敛，另立 LOGIN 层跟进卡
- 合并：`--no-ff` main `79ccd2fa`（分支三提交 `95678eb2` impl → `bab468de` codex r0 P1+P2 闭环 → `0ed94f5b` P2 残余窗口边界与范围决策精确记录；5 files, +657；与 main `98d2e959`（IAM-002 文档同步）零冲突干净合并）

## 弧次与发现

| 弧 | 发现 | 要点与处置 |
|---|---|---|
| r0 | FAIL 1×P1+1×P2 | **P1** rollback SQL 迁移后误删业务任职并留孤儿流水：`reason` 可空，`createMembership` 写 null reason 的 CREATE 流水，`NULL <> '历史账号迁移回填'` 返回 unknown 不匹配 → 普通新建任职、及被迁移后以 null reason 变更的任职均被删除；前置无条件 DELETE 又会删除保留任职的迁移起点流水 → 处置：删除前先物化「纯回填」ID 集（要求回填标记且排除所有后续流水含 null reason），两条 DELETE 限定该集；R4 原纯回填 fixture 恰好掩盖了这些情形，改用混合 fixture 暴露。**P2** 生命周期撤销未与初始令牌签发串行化：并发口令登录时 `OAuth2TokenServiceImpl.createAccessToken` 可将 primary 解析为 ACTIVE 并留下未提交令牌行，撤销路径的账号锁不覆盖该签发路径（签发不取对应锁）、令牌查询看不到未提交行 → 登录可在停用后提交存活凭据；上下文复验在停用期拒绝它，但 RESUME 使同一凭据重新可用而无需再次登录，违反「恢复不复活旧授权」保证 → 处置：以共享串行化边界协调签发与生命周期撤销，并用真实 token 服务而非仅 mock 测试该交错。 |
| r1 | P1 CLOSED / P2 未完全闭环 | **P1 闭合确认**：「rollback fix correctly handles NULL reasons and uses one psql session for the temporary table」（物化 TEMP 表 + IS DISTINCT FROM + 单 psql 会话）。plan B、事务传播、SpEL 模板、懒注入均未发现新缺陷。**P2 深化论证**：登录与快速 suspend/resume 循环重叠时，resume 再次撤销仍允许令牌复活——`createAccessToken` 既不取用户锁也不取任职锁，可在停用提交前解析 ACTIVE、令牌行延迟到两次撤销扫描后才提交，扫描看不到这些行（即便撤销自身锁了用户）；若签发在 resume 提交前落库，令牌在停用期先被拒、resume 后因内嵌上下文重新匹配而被接受。codex 建议以共享锁协议串行化签发与生命周期变更、或每请求校验生命周期代际；mock 的撤销计数测试未覆盖此残余逃逸窗口。 |
| r2 | PASS/0×P0~P3 | **终弧裁定 PASS**：「P1 is closed: both rollback deletes use the materialized pure-backfill ID set. The documented token-issuance race is pre-existing and appropriately deferred to a LOGIN-layer follow-up; review found no new IAM-004 defect or demonstrated over-broad authorization revival.」codex 独立核验签发/撤销锁协议（`OAuth2TokenServiceImpl.doRemoveAccessTokenByUser:528` 取 `adminUserMapper.selectByIdForUpdate`、`OAuth2GrantServiceImpl:96` 授权码兑换路径同锁、`AdminAuthServiceImpl.createTokenAfterLoginSuccess` 直接签发不取锁），确认 P2 根因在登录签发层、早于 IAM-004、对 ZS-LOGIN-003 管理员禁用撤销同样存在，且任何**越界授权**都被 `checkAccessToken` 每请求 `organizationContextMatches` 精确拦截并 401、不会复活旧越界授权，残余仅「suspend→resume 回到完全相同上下文」的低危会话卫生 → 判定为既有跨切面缺陷、越界收敛、另立跟进卡。 |

## 验证

- 定向单测 20/20（`MembershipServiceImplLifecycleTest` 10 + `MembershipServiceImplTest` 10，`MVN_EXIT=0`、BUILD SUCCESS）；含失权联动 D1 方案 B（primary 失活/无任何 ACTIVE 任职才撤全会话、次级任职停用不越权中断）、恢复语义（复职仅默认任职撤销）、生命周期审计、回滚方案演练用例
- 真实 PG17 `run-iam004-verify.mjs` 5/5：R1 前后核对 parity（合格账号数==回填任职数==distinct 账号数，无孤儿/遗漏，eligibleBefore=2）、R2 一账号一 primary（primaryActive=2、multiPrimaryUsers=0）、R3 不合格不回填（null 部门/无匹配组织/已删除账号均 0 任职，不执行未批准账号合并）、R4 恢复方案演练（rollback 只回收纯回填任职 1002+其起点流水，保留已变更 1001/业务新建 1003，保留行起点流水不误删，orphanHis=0）、R5 幂等重放（恢复后重新回填，distinct/起点流水计数与迁移前一致，无重复/悬挂）
- `run-local-gates --fast`（合并前）与 full（合并后）均 10/10（含 G7 数据库 PG 合同、G8 Flyway 迁移规范、G4/G5 文档一致性）
- P1 修复经 `IS DISTINCT FROM` 三值逻辑正确处理 nullable reason（`NULL IS DISTINCT FROM 'x'` 返回 true，避免 `NULL <> 'x'` 的 unknown 漏判）；R4 混合 fixture（fixture A 给回填任职追加 null reason 业务流水使其不再「纯回填」、fixture B 为未回填账号新建纯业务任职）证明 rollback 精确回收边界

## 登记边界（范围外与延后）

1. **登录签发与用户级撤销的串行化根因修复（P2）** → 归后续 **LOGIN 层跟进卡**：`AdminAuthServiceImpl.createTokenAfterLoginSuccess → OAuth2TokenService.createAccessToken`（无 `@Transactional`、不取 `selectByIdForUpdate(userId)` 用户行锁）与 `removeAccessToken(userId)` 所持用户锁不互斥，构成幻影写逃逸窗口。此为登录签发层既有跨切面竞态，对 ZS-LOGIN-003 管理员禁用撤销同样存在、且早于 IAM-004；IAM-004 边界保持收紧，不越界改动登录关键路径。候选方案：签发路径纳入共享用户行锁协议，或引入生命周期代际（generation）每请求校验。
2. **越界授权复活** → 已由 ZS-IAM-002 `checkAccessToken` 每请求 `organizationContextMatches`（orgId/orgType/membershipId 三维精确比对）fail-closed 拦截并 401，本卡不重复建设；残余仅「suspend→resume 回到完全相同上下文」的低危会话卫生，非越权。
3. **组织级授权（PERM-002.B）/ 字段级权限（PERM-003）** → 归各自卡片；本卡仅在 IAM 层维护任职生命周期状态机与历史归属，不修改既有数据权限规则。
4. **真实环境（B07 批次）多端联调** → 归批次放行；本卡 SQL 套件聚焦迁移前后核对/恢复契约面。
