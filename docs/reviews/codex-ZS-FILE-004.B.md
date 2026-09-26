# ZS-FILE-004.B CodeReview 评审处置（单弧）

- 评审工具：CodeReview（会话内代理，独立复审替代；codex 账号用量上限阻断，承 PERM-002.B/PERM-004.B/LOGIN-003.B/SEC-001.B/PERM-001.B/FILE-001.B/MSG-003.C/CLIENT-002.B/BPM-002/BPM-004/PERM-004.C/PERM-003.A 先例链）
- 评审对象：分支 `feat/file-004-b`（ZS-FILE-004.B〔B08 Wave4，P0，类别补建〕导出生成-交付两阶段重检与用途保留期清理——父卡 ZS-FILE-004「接入短时票据和导出全程权限重检」的 .B 子项）
- 结论：**PASS with P2**（1×P2 + 4×P3 全部闭环，处置后 0×P0/P1/P2/P3 遗留）
- 说明：评审原始输出留档会话日志，发现与处置逐条摘录如下，如实登记。

## 交付内容（impl `1bb54295`，27 files +1588/-8；处置 `6e2116ea`，6 files +209/-90；合并 `--no-ff` main `93d0a425`，27 files +1707/-8）

1. **导出生成通道**（内部 Java API，无 HTTP 端点）：`FileExportDeliveryService#generateExportFile` 五步链——接入契约（登录主体+对象类型非空白）→ 安全上下文锚定（请求线程上下文缺失或与入参主体不一致 fail-closed）→ PERM-003.A 统一裁决消费（未注册 provider=未接入域零变化拒绝；对象维=业务组织重检〔visit/org 轴 checker 不可见即 FORBIDDEN〕）→ 字段维拒绝路径（声明字段非空必须 ⊆ authorizedFields；未启用字段级输出+非空声明 fail-closed）→ 落盘私有导出件（owner=登录主体、organizationId=源对象组织、purpose='export'、保留期=now+retention-days）；org 继承源对象组织使生成后转岗/离任/组织停用时交付链（issue/redeem/chunk 逐环节重检）精确收敛。
2. **用途保留期清理**（FILE-005.B L46/L97 明文移交闭环）：`GET /infra/file/export/retention/preview`（只读候选清点：租户内 purpose='export' ∧ PUBLISHED ∧ retention_expire_time<=now；LIMIT 上限+truncated 如实标注）+ `POST /infra/file/export/retention/cleanup`（显式 id 有界批次、distinct 去重、逐项三重门重核验、走 deleteFile 既有保护、失败留痕 `[code=xxx]`）。
3. **载体与配置**：FileDO +purpose/+retention_expire_time（迁移 `V20260925.002`：列+idx_infra_file_05+COMMENT，存量 NULL 零变化）；FileExportProperties（`infra.file.export`：retention-days=7/preview-max-items=200/cleanup-max-ids=100 全保守占位待实测）；FileConfiguration 注册 + yaml 显式登记；错误码 038~042；数据权限授权矩阵 §9；ApiInventory 基线 351→353。

## 评审发现与处置（单弧）

| 级别 | 发现 → 处置 |
|---|---|
| P2-1 | **安全上下文锚定缺位（对象维 fail-open 面）**——对象维裁决经 `OrgDataPermissionChecker`，请求线程无线程上下文时其护栏（loginUser==null→可见）会静默放行对象维，入参主体可被伪造 → `generateExportFile` 增请求线程上下文读取，缺失或与入参主体 id 不一致一律 `FILE_EXPORT_FORBIDDEN` fail-closed。探针红证：`generate_missingThreadContext_forbidden`/`generate_contextSubjectMismatch_forbidden` 先行红 2F 实证。 |
| P3-1 | 清理逐项跳过原因未携真实注册码（对账不友好）→ 统一 `[code=xxx] msg` 格式（`codeMessage()` 经 ErrorCode `getMsg()`）；补注册 040~042（非导出件/非 PUBLISHED/保留期未到）逐项跳过均携注册码。 |
| P3-2 | cleanup 入参重复 id 可同时出现在成功与失败账本（互斥性破坏，且重复处理）→ 入参 `distinct()` 去重（用例 `cleanup_duplicateIds_deduped` 看守）。 |
| P3-3 | 用例未补 `verify(isObjectVisible)` 交互断言（重核验链的显式证据缺位）→ `cleanup_revalidatesBeforeDelete` 补断言。 |
| P3-4 | PG 脚本截断判定仅单侧（trunc1 真）→ 双向对照（trunc1=true/trunc10=false，假阳性不可通过）。 |

## 验证

- RED 双证：编译红（102 错误行/13 缺失符号，BUILD FAILURE）+ 断言红（20 新用例全红〔19F+1E〕、FILE-004.A 保护性 12/12 绿）；首跑两处测试自身缺陷（装配红：NoSuchBeanDefinitionException ObjectAuthorizationService；R2 假绿：空壳返回空列表恰好通过 isEmpty 断言）如实记录并当场修复。
- P 级处置探针红证：23 用例 6F 先行失败（2F 上下文锚定 + 4F 去重/重核验/未过期/非导出跳过注册码断言）→ 修复后 23/23 全绿。
- GREEN：定向 55/55（20 新+35 保护）→ fix 后 23/23 全绿；模块 infra 全量 **529/529**（526 基线+3 处置新增用例；Skipped 10 与基线一致）+ biz-tenant 63/63。
- PG：单套 `run-file004b-verify.mjs` **6/6**（P1 迁移重放/P2 候选查询形状含双向截断/P3 保留期边界含 NULL 与 <=/P4 用途·状态·逻辑删除门/P5 清理数据路径含活跃票据引用谓词/P6 租户隔离）+ 聚合 `run-pg-regression.mjs` **20/20**。
- 门禁：`run-local-gates.mjs --fast` **10/10**。
- flaky 归因：全量 R2 biz-tenant `SecurityChainJointRegressionTest$AsyncFullContract#streamingEndpointWithTokenStreamsFully` 失败经隔离探针 3 连绿 + R3 同代码全量 63/63 绿对照，判定既有异步流式 flaky 非本卡回归（R3 同时取得 infra 529 结果）。

## 登记边界

1. **字段等级目录 F0～F3 与按等级裁剪归 ZS-PERM-003.B**（前置=敏感业务字段目录获批）；本卡不定义任何商业字段名/等级，字段维=声明字段拒绝校验+输出供调用方裁剪。
2. **25+ 既有同步 Excel 导出端点不改造**：业务域逐域接入导出生成通道随领域模块落地（循「入口先行」）；本卡以测试内技术夹具证明机制全链路。
3. **导出生成通道为内部 Java API（无 HTTP 端点）**：内容必须由服务端业务代码传入；裁剪=调用方消费 authorize 输出（服务不解析内容字节）。
4. **保留期清理为两步人工授权**（preview 只读 + cleanup 显式 ids 重核验），无自动定时清理；定时化归 OPS-002.C（B11）评估。
5. **`retention-days=7`/`preview-max-items=200`/`cleanup-max-ids=100` 全为保守占位待实测回填**，禁解读为时效/容量承诺。
6. 短时 GET 预签名 URL 禁用保持、公开素材走既有已批准通道；**在途传输撤权上限配置实测、CDN/缓存/重定向暴露面归环境批次**（循 FILE-004.A 计划 L108 原样）；真实 PG+HTTP 端到端联验随 B08 批次真实环境放行。
7. s3/ftp/sftp 存储的导出件交付/清理未实测（循 FileClient 既有边界 local/db 可测）；清理不跨技术租户。
8. 主卡 ZS-FILE-004 维持「开发中」（.A/.B 均子项交付，主卡验收须 B08 批次真实环境放行），本卡不标主卡已验收。
