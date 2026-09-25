# ZS-FILE-001.B 评审处置（业务组织和对象文件策略：CodeReview 独立复审两轮，R2 PASSED / 0×P0/P1/P2/P3）

- 评审对象：ZS-FILE-001.B 业务组织和对象文件策略——文件域 org 轴对象授权 + 获批 visit 收敛（B08；docs/05 §16.1 line1039 子项「按批准组织/对象授权下载，不把 tenant 简单改名」，前置 ZS-FILE-001.A〔技术账号/tenant 私有文件归属与授权〕+ ZS-PERM-002.B〔org 轴范围解析/对象检查入口 OrgDataPermissionChecker〕+ ZS-IAM-002〔组织/任职模型 + 服务端组织上下文〕+ ZS-SEC-001.B〔获批跨组织 visit 上下文 CrossOrgVisitScopeHolder〕+ D-09 均已交付）
- 隔离分支：worktree `.wt/zszj-wt-file-001-b` 分支 `feat/file-001-b`（自 main `a5659d61`；feat `73c7c7a7` + test `2c04a7a4` + 评审响应 fix `aaa6802f` / test `53cf328e`）
- 合并：`--no-ff` main `ff88bdfa`（8 files +475/-5，feat 分支自 `a5659d61` 线性领先、干净合并）
- 卡片性质：**产线文件授权路径落地卡**——文件域首个 org 轴生产消费方（对象级门 + visit 收敛），属真实产线代码变更，走完整 9 步周期
- 评审工具：codex 账号用量上限阻断（承 ZS-PERM-002.B / ZS-PERM-004.B / ZS-LOGIN-003.B / ZS-SEC-001.B / ZS-PERM-001.B 先例），改以 **CodeReview 独立复审替代**
- 结论：**R1 报 2×P1 授权绕过口子 → 修复（`aaa6802f`）+ 看守 2 用例 → R2 PASSED（0×P0 / 0×P1 / 0×P2 / 0×P3）**
- 原始日志：`outputs/_file001b-*.log`（本地未入库，gitignore；本档案为处置入库）

## 根因/背景（§16.1 line1039 验收 + 三前置移交）

ZS-FILE-001.A（B04）已交付技术账号/tenant 轴私有文件授权（PUBLIC 匿名可读 / PRIVATE 登录 + 同租户 + owner 或 `infra:file:query`），ZS-PERM-002.B 交付 org 轴对象级检查入口（`OrgDataPermissionChecker`，镜像 dept 轴 checker），ZS-SEC-001.B 交付获批跨组织 visit 上下文（`CrossOrgVisitScopeHolder`，D7「各业务路径逐一接入随下游领域模块」）——但文件域仍是**纯 tenant 轴**：`FileDO` 无业务组织载体，同租户异组织用户持 `infra:file:query` 即可读取/删除他组织文件，FND-INF-001「未授权用户不能通过已知 URL/文件 ID 下载其他组织文件」与 FND-AUTH-004「已知其他组织的对象 ID 也不能读取/修改/下载/导出」的文件域落地缺失；且 visit 上下文下本地 tenant 轴必然失配（home≠target），需对象维收敛后放行。§16.1 line1039 验收：「按批准组织/对象授权下载，不把 tenant 简单改名」。

## 交付内容（feat `73c7c7a7` + 评审响应 `aaa6802f`）

**module-infra（文件域 org 轴落地）**：
- `FileDO` 新增 `organizationId`（org 轴载体，正交于 tenant 轴）+ 迁移 `V20260924.002__infra_file_organization.sql`（int8 + idx_infra_file_04）+ H2 测试 schema（`create_tables.sql`）同步；`pom.xml` 引入 data-permission starter（`@Autowired(required=false)` 可选注入，无实现时 org 门对 `organizationId!=null` fail-closed、不 fail-open）；
- 上传三路径（`doCreateFile` / `createFile(reqVO)` / `completeUpload`）一律 `currentOrgIdOrNull()` 取服务端签发 `LoginUser.getOrgId()` 落组织归属，无组织/匿名/系统上下文为 null（仍由 tenant 轴治理）——`completeUpload` 为 R1 P1-1 修复点；
- `isFileOrgAllowed(file)`（新，读取 `validateFileReadable` / 单删 `deleteFile` / 批量删除 `deleteFileList` 三路共用）：
  - 获批 visit 上下文（`CrossOrgVisitScopeHolder.getScope()` 且 authorized）：对象维由 `CrossOrgVisitScopeHolder.isObjectAllowed(file.getOrganizationId())` 收敛（whole-tenant 放行 / 限定组织须命中），**不走本地 org 门**；
  - 非 visit：`organizationId==null` 历史/匿名文件由 tenant 轴治理（不触碰 checker）；`organizationId!=null` 交 `OrgDataPermissionChecker.isObjectVisible(organizationId, ownerUserId)` 独占裁决（owner 不凌驾组织排除——D-09 FND-AUTH-004；checker 未装配 fail-closed 拒绝）；
- 读取路径 visit 收敛：org 门通过后，若为获批 visit 上下文**早返回**、跳过必然失配的本地 tenant 轴（home≠target 误拒）——SEC-001.B D7 边界「数据范围收敛随下游 FILE-001.B」首落；
- 删除路径：`deleteFileList` 批量任一越权整批拒绝零删除（先于任何 `deleteFile` 逐项校验）；`deleteFile` 单删门（R1 P1-2 修复点，存在性校验后、路径校验前），越权按 `FILE_NOT_EXISTS` 拒绝（不泄露存在性，与批量门对齐）。

## 新增/改测试（test `2c04a7a4` + 评审响应 `53cf328e`）

| 测试 | 模块 | 用例 | 断言 |
|---|---|---|---|
| `FileServiceOrgAuthorizationTest`（新，14 例） | infra | org 轴 + visit 收敛 | org 归属写入三路径；读取 org 门（越界拒绝/owner 不凌驾组织排除/org==null 由 tenant 轴）；visit 收敛（对象维放行早返回/限定组织越界拒绝）；单删/批量删除 org 门（整批拒绝零删除）；checker 未装配 fail-closed |
| `FileUploadCredentialTest`（+2 例） | infra | 凭证直传完成 org 归属 | completeUpload 落服务端组织归属（P1-1 看守） |
| `FileServiceImplTest`（基线修正） | infra | 既有删除用例 | 显式 `.setOrganizationId(null)`——`randomPojo(FileDO.class)` 随机 non-null organizationId 会被新 org 门 fail-closed 拦截（P1 修复的 landmine 教训） |

## RED → GREEN

- **RED**（实现前）：org 轴用例编译失败（`organizationId` / `isFileOrgAllowed` 未定义 = 预期缺失符号）；
- **GREEN**（实现后）：targeted file 域 **148/148 绿**（含 FILE-001.A 既有 13 例无回归 + 新增单删/凭证 org 归属 2 例）；module-infra 全量 **506 绿**（Failures 0 / Errors 0 / Skipped 10）BUILD SUCCESS；
- **flake 归因**：全量首跑 `SecurityChainJointRegressionTest$AsyncFullContract.streamingEndpointWithTokenStreamsFully` 异步流式 timing flake（FLAKY-1 已登记），隔离单独运行 10/10 绿，与基线同源、非本卡回归。

## CodeReview 独立复审发现（R1 → 修复 → R2 PASSED，0×P0/P1/P2/P3）

| # | 严重度 | 位置 | 发现 | 处置 |
|---|---|---|---|---|
| P1-1 | **P1** | `FileServiceImpl#completeUpload` | 凭证直传完成（ZS-FILE-003，**主要生产上传路径**）遗漏 `organizationId` 归属——凭证流上传的文件 org=null 仅由 tenant 轴治理，同租户异组织用户持 `infra:file:query` 即可读取，org 轴独占裁决被架空 | **修复**（`aaa6802f`）：与 `doCreateFile`/`createFile` 统一取 `currentOrgIdOrNull()` 落组织归属 + `FileUploadCredentialTest` 看守用例 |
| P1-2 | **P1** | `FileServiceImpl#deleteFile(Long)` | 单文件删除路径无 org 门——`FileController` 单删端点直接调用，绕过 `deleteFileList` 的批量 org 收敛；同租户异组织用户持 `infra:file:delete` 即可删除他组织文件，违反 D-09 FND-AUTH-004 | **修复**（`aaa6802f`）：存在性校验后、路径校验前加 `isFileOrgAllowed` 门，越权按 `FILE_NOT_EXISTS` 拒绝（不泄露存在性，与批量门对齐）+ 单删 org 归属看守用例 |

R2 复审（`2c04a7a4..53cf328e`）确认 2×P1 全部闭合，**PASSED / 0×P0/P1/P2/P3**。

## 验证（交付时点证据）

- targeted file 域隔离单测：**148/148** BUILD SUCCESS（修复后，含 P1 看守 2 例）；
- module-infra 全量：**506 绿**（Failures 0 / Errors 0 / Skipped 10）BUILD SUCCESS；既有异步流式 flake 隔离 **10/10** 绿（FLAKY-1 已登记、非回归）；
- 两轮 CodeReview 独立复审替代（R2 0×全级）；
- `randomPojo` landmine：P1 修复使 `FileServiceImplTest` 2 既有用例被新 org 门 fail-closed 拦截，以显式 `.setOrganizationId(null)` 修正（教训：新增业务列后 `randomPojo` 随机值会命中新授权门，须显式置 null 保持原语义）。

## 卡片状态与统计

主卡 ZS-FILE-001 维持**开发中**（.A 技术账号/tenant 私有文件 + .B 业务组织/对象文件策略均交付；循子项拆分卡先例 SEC-001/SEC-011/DB-019/MSG-003/PERM-001/PERM-002/PERM-004「.A+.B 均交付、待验收须真实环境放行」+ §16.1「不能把技术夹具算业务策略完成」，须 B08 批次真实环境放行才转待验收）。第 2 节统计不变（2/20/3/0/3/63，合计 91，权威来源 `node scripts/gov/task-stats.mjs`）。**本记录不表示任何主任务验收状态变化。**

## 边界（登记）

- **org 轴 SQL 列表静默过滤规则 `OrgDataPermissionRule` 注册仍未落地**——本卡交付对象级门（组织范围独占裁决），列表 SQL 过滤随 MSG-003.C/CLIENT-002.B 或后续（org 轴 **首个业务表 org 归属** `infra_file.organization_id` 已在数据权限授权矩阵 §7.7 登记）；
- **历史文件分类迁移不能默认全部公开**（§16.1 口径）——`organization_id==null` 历史文件由 tenant 轴治理，不因本卡自动获得 org 归属；
- **导出/其他读取旁路全覆盖**与**真实 PG + HTTP 端到端跨组织文件联验**随 B08 批次真实环境放行——本卡以 H2 targeted 套件坐实 org 门 + visit 收敛语义；
- 文件域接入接线详见 [跨组织访问授权矩阵.md](../../services/zhongshu-core/docs/跨组织访问授权矩阵.md) §5/§8 与 [数据权限授权矩阵.md](../../services/zhongshu-core/docs/数据权限授权矩阵.md) §7.7。
