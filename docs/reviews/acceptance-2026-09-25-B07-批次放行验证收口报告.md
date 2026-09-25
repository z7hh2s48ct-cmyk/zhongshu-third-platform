# B07 批次放行验证收口报告（DB-010 / IAM-002 / IAM-004）

> 验收日期：2026-09-25
> 验收基线提交：`86bb2c06`（main）+ 本轮验收期测试适配补丁（`scripts/db/run-pg-regression.mjs` 补注册 ZS-IAM-004 第 19 套件，随放行提交入库）
> 上位依据：[docs/03 §7 批次放行规则 + B07 条款](../03-底座二次开发顺序与验收标准.md)、[docs/05 V1.93](../05-底座模块分析与开发任务清单.md)、[docs/07 §4 D-09 批准清单](../07-账号唯一性与组织任职决策输入.md)
> 原始日志：`outputs/acceptance/`（本地保留，不入库）

## 1. 验收环境（B07 要求：JDK17 + Maven + PG17，与 B02~B06 同基线）

| 组件 | 版本 | 供给方式 |
|---|---|---|
| JDK | OpenJDK 17.0.20.1 | `tools/jdk-17.0.20.1+1`（定向单测注入 `JAVA_HOME`） |
| Maven | 3.9.9 | `tools/apache-maven-3.9.9` |
| PostgreSQL | 17（`postgres:17-alpine`） | 各 PG 套件自管理一次性容器（验后清理） |
| Redis | 内嵌（单测 `BaseDbAndRedisUnitTest`） | 本轮 PG 套件不依赖 Redis |
| Node.js | v24.19.0 | 系统 PATH |
| Docker | 29.7.2 | Windows 22H2 主机 |

## 2. 验证执行矩阵（全部实跑，非引用旧证据）

| # | 执行入口 | 覆盖 | 结果 | 日志 |
|---|---|---|---|---|
| 1 | `run-local-gates.mjs` 全量 14 门禁 | G1～G15（含 G2b/G13/G14/G15） | **PASS 14/14，失败 0**（放行轮复跑） | `outputs/acceptance/B07-full-gates.log` |
| 2 | `run-pg-regression.mjs`（补注册后 **19 套件**） | DB-006~018、CFG-002.A、BPM-001、JOB-002~004、OPS-002.B、SEC-011.B、FILE-005.B、LOGIN-005.B、**DB-010 #17、IAM-002 #18、IAM-004 #19** | **PASS 19/19 套件，失败 0，exit 0** | `outputs/acceptance/B07-pg-regression.log` |
| 3 | `run-db010-verify.mjs` 独立实跑 | P0~P7（8 用例）：并发复现回归/结构核对/历史冲突 fail-loudly/并发兜底 23505/大小写归一/删除重建/空值多行/跨租户全局唯一 | **8/8 PASS，exit 0** | `outputs/acceptance/B07-DB010-verify.log` |
| 4 | `run-iam002-verify.mjs` 独立实跑 | P1~P8（8 用例）：迁移重放+三表结构/部门回填桥接/不误合并/起点流水/默认任职唯一/同组织去重+离职再入职/组织编码唯一/跨组织隔离+role_ids | **8/8 PASS，exit 0** | `outputs/acceptance/B07-IAM002-verify.log` |
| 5 | `run-iam004-verify.mjs` 独立实跑 | R1~R5（5 用例）：前后核对 parity/一账号一 primary/不合格不回填/恢复方案演练/幂等重放 | **5/5 PASS，exit 0** | `outputs/acceptance/B07-IAM004-verify.log` |
| 6 | module-system 定向单测（9 类，两批实跑） | IAM-002 行为级（上下文复验/组织树/任职解析）+ IAM-004 生命周期 + DB-010（唯一冲突转译/会话失效） | **143/143 PASS，BUILD SUCCESS**（批 A 108 + 批 B 21 + 批 C 14） | `outputs/acceptance/B07-module-tests.log`、`B07-module-tests-b.log` |

定向单测明细（9 类）：`OrganizationServiceImplTest` 12 + `MembershipContextResolverTest` 10 + `OAuth2TokenServiceImplTest` 17 + `OAuth2TokenServiceImplAuthorityUnitTest` 9（含 `checkAccessToken_embeddedOrgContextMismatch_rejected` 等 7 项上下文复验）+ `OAuth2TokenServiceImplOrphanRevokeTest` 12（含 `testRemoveAccessTokenByUser_tombstoneBlocksStaleSnapshotBackfill`）+ `MembershipServiceImplTest` 10 + `MembershipServiceImplLifecycleTest` 10（含动作序列 CREATE→TRANSFER→SUSPEND→RESUME→TERMINATE 精确匹配）+ `AdminUserServiceImplTest` 49 + `AdminUserServiceImplSessionInvalidateTest` 14。

## 3. 验收期发现与处置（如实登记）

### 3.1 IAM-004 PG 回归第 19 套件未注册聚合入口（证据链缺口，已修复）

IAM-004 交付链（`95678eb2`/`bab468de`/`0ed94f5b`）仅修改脚本自身 `run-iam004-verify.mjs`，未同步修改聚合入口 `run-pg-regression.mjs`（该文件最后更新为 `e5a78380`，仅登记 #17/#18）；codex-ZS-IAM-004.md 称"PG 回归第 19 套件"，但该套件实际从未被聚合跑批覆盖。处置：放行轮内补注册（头注释 #19 + cases 数组一行，随放行提交入库），补注册后聚合实跑 **19/19 全 PASS**（其中第 19 套件 5/5）。定性：验证编排适配（不改产品代码），与 B02~B06 放行"验收期测试适配补丁"先例同类。

### 3.2 codex-ZS-IAM-002.md 单测数字登记口径偏差（如实登记）

档案登记"定向单测 119/119（OrganizationServiceImplTest 41 + MembershipContextResolverTest 41 + OAuth2TokenServiceImplTest 37）"；实测三类 `@Test` 分别为 **12/10/17**（历史版本 `b8182e56` 亦为 12，非后续拆分所致）。档案点名的两个关键回归测试：`checkAccessToken_rejectsWhenEmbeddedContextMismatch` 实际方法名为 `checkAccessToken_embeddedOrgContextMismatch_rejected`（`OAuth2TokenServiceImplAuthorityUnitTest` L120，本轮已跑并通过）；`testDeleteWithStaleSnapshot_failsClosed` 库内无此方法名，语义最近为 `testRemoveAccessTokenByUser_tombstoneBlocksStaleSnapshotBackfill`（`OAuth2TokenServiceImplOrphanRevokeTest` L416，本轮已跑并通过），SERIALIZABLE 防陈旧快照主路径由 `OrganizationServiceImplTest` 12 例（含祖先环校验）承接。处置：本放行轮以实跑数字为准（9 类 143 例全绿）；档案数字口径偏差不影响交付物与验收覆盖判定，如后续需要单独修订档案。

## 4. 逐卡验收结论（3 卡）

验收规则：以 docs/05 各卡「验收」条款逐条对照 §2 实跑证据；D-09 决策检查器以 **[docs/07 §4 批准清单（M1~M6）](../07-账号唯一性与组织任职决策输入.md) 逐项对照**形式落地（无独立脚本，属决策台账核验）。各卡开发期证据（失败测试、实现、codex 评审弧）见 docs/05 卡内开发记录，本节登记放行轮实证。

| 模块 | 卡 | 验收条款 → 本轮放行证据（§2 编号） | 结论 |
|---|---|---|---|
| DB | ZS-DB-010 | ①同范围并发最多一成功、其余明确冲突 → #2 #17 + #3 P0/P3（真并发重叠事务：A 持锁同步=true、失败方撞 23505、B 被阻塞独立计时 2876ms）；②不同范围不误拦 → #3 P7（M1-A 全平台唯一按批准）+ P5（逻辑删除排除）；③空值/删除重建/升级冲突 → #3 P6/P5/P2（fail-loudly 禁擅自删重）/P1/P4（M2 归一撞表达式索引）+ 单测 49（冲突码转译） | **已验收** |
| IAM | ZS-IAM-002 | ①批准组织/任职关系可创建查询 → #4 P1/P2/P8 + 单测 12；②伪造或过期上下文拒绝 → #6 批 B Authority 9/9（内嵌上下文不一致/解析变空/解析异常→401、DB 权威 fail-closed）+ Resolver 10；③双组织隔离 → #4 P8（跨双组织两任职仅一 primary）+ #5 R1/R2；④未批准多身份无切换入口 → D-09 未批准身份切换，交付面零切换 API（`LoginUser` 纯增量、客户端不可伪造）；⑤历史迁移不误合并 → #4 P3/P4 + #5 R1/R3 | **已验收** |
| IAM | ZS-IAM-004 | ①转岗不重写责任历史 → #5 R4（rollback 只回收纯回填，保留已变更/业务新建）+ 单测 Lifecycle 10（动作序列精确匹配）+ 生命周期审计常量；②停用/离职及时失权 → Lifecycle 10（primary 变更或账号无 ACTIVE 任职才撤会话，次级任职停用不越权中断）+ SessionInvalidate 14 + OrphanRevoke 12；③恢复不意外恢复旧越界授权 → Lifecycle 复职（仅默认任职强制重登）+ IAM-002 每请求 orgId/orgType/membershipId 三维复验 fail-closed（批 B）；④迁移有前后核对和恢复方案、不执行未批准合并 → #5 R1/R3/R4/R5 | **已验收** |

## 5. 正向/反向验收对照（docs/03 B07 条款）

- **B07 正向**：合法主体关系可创建、变更和追溯——创建/变更（#2 #18/#19、单测 Organization 12 含父级环/子级/成员引用守卫）；追溯（#4 P4 回填起点流水 + #5 R1 parity + LogRecord 生命周期常量）；停用后会话及时失权（#6 Lifecycle 10 + SessionInvalidate 14 + OrphanRevoke 12）；历史业务责任不被抹除（#5 R4 混合 fixture 演练：带业务流水的回填任职完整保留、起点流水不误删）。
- **B07 反向**：两组织人员不能互授角色——本批次守底（#6 Authority 9 例伪造/过期上下文 401 fail-closed + 服务端上下文服务端复验）；组织级授权写入路径收敛（PERM-001.B/002.B visit 范围）归 B08 批次真实环境放行（卡边界已登记）。手机号/OpenID 相同不能自动合并账号（#4 P3 按 user_id 逐一驱动 + #5 R3 不合格不回填）。若 D-09 未批准多身份则不提供身份切换——D-09 未批准身份切换，交付面零切换入口（#4/#6 佐证客户端无法伪造主体）。

## 6. 未验证项与登记边界（不随本轮放行提升）

1. **组织级/字段级授权**（ZS-PERM-002.B 业务表接入、ZS-PERM-003 字段级）与两组织互授写入负向矩阵随 **B08 批次**真实环境放行；本批次范围为 B07「D-09 决策与组织任职实现」。
2. **登录签发/撤销串行化根因修复**：IAM-004 codex r0 P2 判定 pre-existing 越界收敛（越界授权复活已由 IAM-002 每请求复验覆盖），归后续 LOGIN 层跟进卡。
3. **SERIALIZABLE 性能监控**归 B11（ZS-OPS-002.C）；容量/性能与真机验收归 B10/B11，本轮不宣称。
4. org 轴 SQL 列表过滤规则 `OrgDataPermissionRule` + 业务表 `org_id` 列注册随 B08 下游领域模块。
5. §3.2 档案数字口径偏差为评审档案登记问题（不影响验收）；§3.1 已修复并在本轮实跑覆盖。

## 7. 回滚路径

- 本轮验证全部使用一次性 Docker 容器与 `outputs/` 本地日志，**未触碰任何长驻数据**。
- 文档回填（docs/05 §2/§19、README）与状态翻转经 `scripts/gov/close-task.mjs` 幂等执行，`verify-docs`（G5）复核；如需回退，`git revert` 放行提交即可整体还原。
- 测试适配补丁仅 1 文件（`scripts/db/run-pg-regression.mjs` +2 行）；revert 后聚合回到 18 套件（IAM-004 落于跑批外），不影响产品代码。
- 迁移回滚方案：`V20260921.002` 回填段带 R4 演练 rollback（物化纯回填 ID 集 + 两条限定 DELETE，生产回滚须先备份 + 停写窗口）；`V20260920.001` 升级冲突 fail-loudly 探测（禁止自动降级/擅自删重）。
