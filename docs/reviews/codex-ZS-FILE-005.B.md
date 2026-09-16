# ZS-FILE-005.B codex 评审处置（r0→r4 五弧）

- 评审工具：codex（gpt-6-astra / xhigh，`codex review --commit <SHA>`，**真实文件系统探针实测**：真实目录 junction/symlink（含大小写差异形态）+ 真实 LocalFileClient/Service 复现误删与假成功、真实 PG PREPARE 验证 LIKE 转义）
- 评审对象：分支 `feat/file-005-b`（ZS-FILE-005.B〔B05 Wave5，P1，类别补建〕自动补偿与孤儿对象清理——.A 删除中间态/引用保护/人工对账之上的自动化）
- 结论：**r4 PASS / 0 发现**（五弧收敛：r0 FAIL 2×P1+2×P2 → r1 FAIL 2×P1+1×P2 → r2 FAIL 2×P1 → r3 FAIL 1×P1+1×P2 → r4 PASS「No actionable regressions were identified」；每一弧的修复都被下一弧以更深的文件系统形状击穿再修复，直至归零）
- 说明：评审弧 raw 输出留档会话日志，发现与处置逐条摘录如下，如实登记。

## 交付内容（impl `cb8d2664`，29 files +1585/-11；r0 处置 `9d96357d` +325/-32；r1 处置 `6693f1b4` +260/-22；r2 处置 `1727eeea` +237/-39；r3 处置 `998e51e5`，7 files +392/-14；合并 `--no-ff` main `e7599e9a`，冲突解 `run-pg-regression.mjs` 套件号→15）

1. **超时 DELETING 自动补偿（轮询收口，非 Outbox 事件，论证在计划 §0.3-1）**：infra_file 加 `deleting_time`（迁移 `V20260916.102`：存量 DELETING 按 update_time 回填 + `(status,deleting_time)` 索引；update_time 因 `update(null,wrapper)` 不触发自动填充不可靠）；`FileDeleteCompensationJob` `@Scheduled` 周期扫描 → 逐记录在其租户内 **DB 条件 CAS 领取**（`SET deleting_time=now WHERE status='DELETING' AND deleting_time<=graceBefore`，恰一实例 affected=1，领取即租约兼退避点，循 JOB-002 栅栏思想）→ **复用 .A `reconcileCleanupFile`**（引用保护/确认缺失仅移记录全继承，不绕过已闭合竞态）；候选 FIFO 排序（`deleting_time ASC, id ASC`，失败记录领取时前推即自然队尾，防持续失败记录霸占批次饿死他项）。
2. **孤儿对象两步清理**：`FileClient.listObjects`/`listObjectsDetailed`（local 目录遍历 + db 分组 path 实装；s3/ftp/sftp 抛 UnsupportedOperationException→错误码 034 保守拒绝，未实测清点不放大为误删）；`GET /infra/file/orphan/preview`（只读清点+截断如实+前缀续扫参数）→ `POST /infra/file/orphan/cleanup`（显式 path 有界批次≤100、执行前逐项重清点+重核验、逐项成败不伪报全成功、幂等）。
3. **评审弧内加固（误删级 P1 五连修）**：①引用核验 LOWER 两侧折叠（大小写不敏感 FS 变体拼写不漏检，敏感系统多保留为保守取舍）+ 孤儿候选跨租户全局核验（executeIgnore，防他租户记录被误判）；②共享/嵌套存储根**保守拒绝**（local 配置存储根物理解析后相等/嵌套即拒 036；配置根不可解析同拒——无法证明隔离即拒绝）；③**词汇+物理双视角隔离判定**（先 client 同款 `toAbsolutePath().normalize()` 再 `toRealPath()`，两视角任一判共享/嵌套即拒——junction 嵌套洗白与 `alias/..` 洗白两形状分别击穿后收敛）；④树内游离别名 DFS 不下钻 + **符号链接显式先判**（`Files.isSymbolicLink` 先于别名比较，不依赖大小写——`Temp->temp` 仅大小写差异形态曾绕过比较致删在途对象；符号链接文件不入清单）；⑤**不可验证 ≠ 已缺失**：`FileListing` 上报被跳过前缀/文件 + 清点截断态，cleanup 命中盲区按 **037 `FILE_ORPHAN_PATH_UNVERIFIABLE`（1_001_003_037）失败拒绝**绝不计入 successPaths（假成功曾使真实存在的 `alias/old.bin` 被报成功）；DB 清点前缀 LIKE `ESCAPE '\'` 转义（`_`/`%` 字面语义，PG PREPARE 实证）。
4. **硬化**：记录移除条件化（仅 DELETING 态可移除，交错自愈）；配置 `infra.file.compensation.*`（enabled 代码默认 false/yaml 显式 true，interval/grace/retention/上限全保守占位待实测）；错误码 1-001-003-034~037；ApiInventory 基线 347→349。

## 评审弧（真实 FS 探针逐轮实锤）

| 轮次 | 结论 | 发现 → 处置 |
|---|---|---|
| r0（`--commit cb8d2664`） | **FAIL** 2×P1+2×P2 | **P1-1** 跨配置共享 basePath 引用漏检（两 local 配置同根时他配置引用被无视→误删）→ 保守拒绝方案（036）。**P1-2** 大小写不敏感 FS 引用漏检（Files.walk 返回既有拼写，精确匹配漏检→误删）→ LOWER 折叠。**P2-1** 补偿饥饿（低位 ID 持续失败霸占批次）→ FIFO。**P2-2** 截断扫描无续扫 → preview 前缀参数。 |
| r1（`--commit 9d96357d`） | **FAIL** 2×P1+1×P2 | FIFO/续扫方向 PASS。**P1-A** junction/symlink 别名使字符串规范化后不同的两配置实为同根（codex 真实 junction 复现误删）→ `toRealPath` 物理解析+失败保守拒绝。**P1-B** 根路径含尾分隔符时包含判定失配（`c://`）→ `toCanonicalRoot` 合同（尾分隔符剥离+边界只补不加+`/` 包罗一切）。**P2** DB 前缀 LIKE 通配符未转义 → ESCAPE 合同。 |
| r2（`--commit 6693f1b4`） | **FAIL** 2×P1 | LIKE 转义 PASS。**P1-A** Windows junction 嵌套被物理解析「洗白」（`C:/store` vs `C:/store/alias→elsewhere` 物理不相交但 Files.walk 会跟进 junction）→ 保留词汇嵌套检查与物理比较**并存**。**P1-B** toRealPath 前未做 client 的词汇规范化（`/store/alias/..` 形状校验根 ≠ I/O 根）→ 先 normalize 再 toRealPath。 |
| r3（`--commit 1727eeea`） | **FAIL** 1×P1+1×P2 | 双视角判定/第三形状保守处置 PASS。**P1** 大小写差异符号链接 `Temp->temp` 绕过忽略大小写比较 → 枚举跟进后在途对象被误删 → **符号链接显式先判拒下钻**。**P2** junction 内容静默略过使 `findExactEntry` null 被 cleanup 当「已缺失」→ 假成功 → `FileListing` 盲区上报 + 037 拒绝。 |
| r4（`--commit 998e51e5`） | **PASS / 0 发现** | 「No actionable regressions were identified in this commit. The symlink rejection and unverifiable-path handling are consistent with the cleanup flow, and all 23 tests in LocalFileClientListTest and FileOrphanServiceTest passed.」 |

## 验证

- TDD：修复相关 4 测试类 **36 用例全绿**（List 6 / LocalFileClientTest 5〔含 2 既有 @Disabled〕/ OrphanService 17 / CompensationJob 8）；关键新用例 pre-fix 全红、post-fix 全绿实测（junction 嵌套/`alias/..`/大小写差异符号链接/盲区假成功四形状各具红绿双向证据；符号链接用例 Windows 经 `fsutil file setCaseSensitiveInfo` 真实执行 0 skip）。
- H2：infra 全量 `-am` **BUILD SUCCESS**（reactor 16 模块，infra surefire 汇总 455 用例 0 失败 0 错误、10 既有平台 skip）。
- 门禁：`run-local-gates.mjs --fast` **10/10**。
- PG：`run-file005b-verify.mjs` **9/9**（P1 迁移重放+回填 / P2 条件转移 / P3 双 psql 并发领取恰一 / P4 条件删除恰一次+不误删他行 / P5 引用谓词 / P6 回退清空 / P7 孤儿清点全局核验 / P8 FIFO 排序形状 / P9 LIKE 转义含未转义对照）；注册 `run-pg-regression.mjs` **第 15 套件**（合并冲突解为 13/14/15 并存）。
- 合并后集成验证（main `e7599e9a`）：`ApiInventoryTest`（基线 347→349）+ 三新套件（13/14/15）结果回登 docs/05。

## 登记边界

1. **s3/ftp/sftp 对象清点未实装**（034 保守报错）；真实对象存储接入后回填 `listObjects` 并实测前缀/边界行为。
2. **local 多配置共享/嵌套存储根被保守拒绝**（036）——运维应保持各 local 配置存储根物理互斥；**经合法符号链接暴露的跨根共享同样被拒**（多配置共用物理目录应合并为单配置）；local 存储根必须真实存在方可预览/清理（toRealPath 前提）。
3. **不可验证路径拒绝合同**（037）：命中被跳过（符号链接/别名/解析失败）前缀或清点截断的 path 一律失败拒绝、绝不假成功；前缀续扫为操作方规程。
4. 引用核验 LOWER 折叠在大小写敏感系统上可能多保留「异拼写同路径」孤儿（保守取舍）。
5. interval-ms(30min)/grace-minutes(60)/max-per-cycle(100)/retention-days(7)/scan-max-objects(1000)/cleanup-max-paths(100) 全为**保守占位待实测回填**，禁解读为容量/时效承诺。
6. 孤儿清理为「预览→授权」两步人工流程，无自动定时孤儿清理（防误删最保守）；导出文件按用途保留期清理归 FILE-004.B（B08）；补偿不投递 JOB-002 Outbox 事件（两选一收口，论证在计划 §0.3-1）。
7. FILE-005 主卡维持「开发中」（.A+.B 均交付），本卡不标主卡已验收。
