# ZS-PERM-003.B 评审处置（目录接入卡：CodeReview 独立复审 R1 0×P0/P1 + 1×P2 + 2×P3 → 处置 d9afd715）

- 评审对象：ZS-PERM-003.B 批准字段等级目录 F0～F3 配置接入（敏感域字段分级裁剪；§16.1 子项，父卡 ZS-PERM-003；前置 ZS-PERM-003.A〔机制〕+ D-12 敏感业务字段目录〔2026-09-28 用户拍板，docs/08 V1.0〕均已交付）
- 隔离分支：worktree `.wt/zszj-wt-perm-003-b` 分支 `feat/perm-003-b`（自 main `8fbf83d0`；RED `43c9ddf9`〔15 files +1069/-7，骨架+测试〕→ GREEN `ec305f98`〔7 files +227/-35〕→ R1 处置 `d9afd715`〔2 files +99/-3〕）
- 卡片性质：**产线机制接入卡**——以 D-12 为唯一权威输入交付分级裁剪与脱敏输出（FieldLevel 枚举 / ClassifiedObjectAuthorizationProvider SPI / FieldLevelScopeResolver 上限解析 / maskedFields 合同 / FieldMaskUtils / 导出不放宽）；业务域逐域接入留后
- 评审工具：codex 账号用量上限阻断（承 PERM-002.B 起 12 卡先例），改以 **CodeReview 独立复审替代**（R1 → 处置 → R2 两轮）
- 结论：**R1 0×P0/P1 + 1×P2 + 2×P3（安全/正确性/约定三域判 clean）→ P2-1/P3-1 修复 + P3-2 登记不改 `d9afd715` → R2 增量复审 PASS（0×P0/P1/P2/P3）**（评审门实质闭环）

## R1 findings 与处置（`d9afd715`）

| 级别 | 编号 | 缺陷 | 处置（`d9afd715`） |
|---|---|---|---|
| P2 | P2-1 | `resolveFieldLevel` 可返回 null（SPI 允许运行期计算候选集，构造期装配校验不可达运行期新增候选）：运行期未编目且无域默认的候选字段使 `level.isWithin(maxLevel)` 抛裸 NPE，中断整次 authorize（动作维一并 500），违反 fail-closed 统一 FORBIDDEN 纪律 | 分类循环内 null 等级 → WARN + fail-closed 拒绝输出（continue，方向与超上限拒绝一致），不以 NPE 中断；新增用例 5c（DynamicCandidateProvider 构造期/运行期双形态看守：ghost 字段被拒且已编目字段不受影响） |
| P3 | P3-1 | `validateClassified` 对 `getCandidateFields()` 返回 null 的 provider NPE（运行期路径经 `CollUtil.isEmpty` 容忍 null，装配路径不容忍——口径不一致，启动期以不透明 NPE 失败） | 装配校验 null 候选集合按「字段维未启用」同语义放行（与运行期 CollUtil.isEmpty 口径一致）；新增用例 5b（null 候选：构造通过 + 裁决输出双 null） |
| P3 | P3-2 | classified 对象单次 authorize 内两次组织数据权限查询（checker/resolver 各自 LoginUser 上下文缓存 key）——冗余查询 + 两维快照可轻微不一致（更严者后到，无泄漏方向） | **登记不改**（javadoc 已声明为有意权衡：两维解耦、无跨请求缓存；共享缓存收敛留后续，登记于数据权限授权矩阵 §10.3-3） |

## R2 复审（增量 `ec305f98..d9afd715`）

**PASS / 0×P0/P1/P2/P3**：处置确实闭合 P2-1/P3-1 且无新缺陷引入；用例 5c 对「修复回退为 NPE 路径」「ghost 泄入授权集合」两类回归均有检出力。

## 评审余项（登记，不阻塞）

- A 类线索域「客户手机号 F2 负责人可见完整」的「负责人」若需含对象 owner 维（非组织负责人），机制需小扩展（`ObjectAuthorizationRequest` 已携带 ownerUserId），随 D-07 阶段 2 域接入评估（矩阵 §10.3-4）。

## 验证（交付时点证据）

- **RED 双证**：
  - 编译红：找不到符号 `FieldLevel`/`ClassifiedObjectAuthorizationProvider`/`FieldLevelScopeResolver`/`FieldMaskUtils` 等，BUILD FAILURE（/tmp/perm003b-red-compile.log）；
  - 断言红：新套件 27 例红（resolver 4F+4E / mask 6F / classification 8F+5E），既有 `.A` 保护性 `ObjectAuthorizationServiceTest` 27/27 绿；
- **GREEN**：starter 模块 **135/135**（`.A` 27 + 新 4 套件 35 + R1 回归 2 → classification 18/18）+ 上游 common/security/web/mybatis/redis/monitor/test 全量随 `-am` 链 EXIT=0；
- module-system：定向 65/65（`OrgDataScopeResolverTest` 18 含 ledOrgIds 5 新例 + `PermissionServiceTest` 47）+ 全量复跑（1019 例，见 flaky 登记）；
- infra：`FileExportDeliveryTest` **16/16**（含导出不放宽 2 新例）；
- fast 门禁 **10/10**（worktree 侧）；
- PG 回归 19/19：零迁移零脚本变更（本卡不改任何 SQL/迁移）。

## flaky 登记（主树对照实证非本卡回归，循 FILE-004.B 处置协议）

1. `SecurityChainJointRegressionTest$AsyncFullContract.streamingEndpointWithTokenStreamsFully`（biz-tenant）：全量并发下偶发，隔离复跑 **9/9 绿**——台账 FLAKY-1 在案（SEC-012 异步流式夹具非线程安全）；
2. **module-system 全量顺序/负载依赖家族（主树对照实锤）**：全量三跑失败集合轮换——本分支 run2 `testResolve_multiMembership_union` 1F + `SmsCodeServiceImplAttemptLimitTest` 3×ERROR；run3 `testResolve_orgLeader_ledOrgIdsFilled` 1F + `PermissionServiceTest.testAssignUserRole_visitScope_targetUserInApprovedOrg_success` 1E + Sms 3E；**主树（`8fbf83d0`，无本卡改动）同命令全量同样复现 `multiMembership_union` 1F + Sms 3E**（/tmp/perm003b-main-fulldiscr.log，1014 例 1F+3E）。失败签名一致（org/任职行查询在跨类残留上下文下返回空集→范围断言失败），失败集合随排序轮换；本分支隔离复跑 `OrgDataScopeResolverTest` 18/18（多轮）+ `SmsCodeServiceImplAttemptLimitTest` 12/12 + `PermissionServiceTest` 47/47 全绿。判定：既有「跨类线程上下文残留」家族（memory 在案：module-system 全量既有顺序依赖失败），非本卡回归；本分支新增用例 `testResolve_orgLeader_ledOrgIdsFilled` 的 run3 偶发与主树 multiMembership 同机制同负载（ledOrgIds/orgIds 空集签名一致），不以单次全量偶发否定交付。
