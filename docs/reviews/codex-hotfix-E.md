# Codex 评审：hotfix-E — ZS-BRAND-002 命名验证器漏排 target/ + ZS-BRAND-001 docs/06 执行顺序两项修复

## 评审元信息

| 项目 | 值 |
|---|---|
| 评审对象 | `ec92cb85`（r0，单轮） |
| 评审工具 | codex-cli 0.153.4, model gpt-6-astra, reasoning effort xhigh |
| 评审轮次 | 1 轮 codex 评审（r0 直接 0 发现，无需迭代修正） |
| 关联评审 | [codex-ZS-BRAND-002.md](codex-ZS-BRAND-002.md)（发现 2）、[codex-ZS-BRAND-001.md](codex-ZS-BRAND-001.md)（单一 P2 发现） |
| 修改文件 | 2 files, +8/-1 |
| 原始日志 | [codex-hotfix-E.raw.md](codex-hotfix-E.raw.md) |
| 最终状态 | ✅ 通过（r0 codex 0 发现；codex A/B 自验修复前 8 假阳性 / exit 1 → 修复后 0 / exit 0；本地门禁 11/11 PASS） |

## 修复的原始缺陷（前序批次遗留 → `ec92cb85` 修复）

本 hotfix 收口 [README.md](README.md)「仍有效」的最后 2 项 P2，分属两个来源评审文档，同为 15.2 品牌专项改名后的门禁 / 文档一致性缺口：

| 来源编号 | 优先级 | 来源评审 | 原始缺陷 | 修复方式 |
|---|---|---|---|---|
| ZS-BRAND-002 发现 2 | P2 | [codex-ZS-BRAND-002.md](codex-ZS-BRAND-002.md) | `verify-backend-naming.mjs` 的 `walk()` 未排除构建产物目录；Maven 在 `target/generated-sources/**/*.java` 生成的类路径不含 `src/main/java` 或 `src/test/java` 前缀，第 45 行剥离为空操作，被第 1 项 package-path 校验误判为 `package-path-mismatch`，`mvn compile` 后本地门禁假阳性、破坏「本地与 CI 同规则」（ZS-OPS-001.A 验收项） | 在唯一遍历原语 `walk()` 统一跳过 `target`/`node_modules`/`.git`/`.idea`，一处修复覆盖全部 5 段 walk 调用 |
| ZS-BRAND-001 单一 P2 发现 | P2 | [codex-ZS-BRAND-001.md](codex-ZS-BRAND-001.md) | `docs/06` 第 110 行执行顺序把 B01 的 ZS-BRAND-006.A 排在 B02 的 ZS-BRAND-004/005 之后，与 docs/05 第 16.1 节批次前置（006.A=B01、004.A/005=B02、B01 先于 B02）冲突，形成循环批次依赖 | 调整为 002 → 003.A → 006.A（B01 收口）→ 004.A（B02）→ 005（B02），并把「ZS-BRAND-004」明确为子项「ZS-BRAND-004.A」 |

### 编号对照说明

| 缺陷 | 来源评审文档内编号（本文与代码注释采用） | [README.md](README.md) 汇总表内编号 |
|---|---|---|
| verify-backend-naming 漏排 target/ | ZS-BRAND-002 发现 2 | 本表 P2 #1 |
| docs/06 执行顺序批次冲突 | ZS-BRAND-001 单一 P2 发现 | 本表 P2 #11 |

代码注释一律引用**来源评审文档**的编号（左列）；README 内一律写作「本表 P2 #n」。

## Codex 评审迭代过程

### 第 1 轮（`ec92cb85`，r0）— ✅ 通过（0 发现）

codex 对 r0 提交直接给出 0 发现结论，并在评审过程中**自主执行**了严格的 A/B 对照验证（见原始日志）：

- **发现 2（target/ 排除）**：codex 用 `git show ec92cb85^:…/verify-backend-naming.mjs` 取**修复前**版本，与**修复后**当前版本在同一真实工作树（本机已有 18 个 `target/`、8 个 MapStruct 生成 `.java`）上对跑：
  - 修复前：`status=1, javaFiles=6784, hardIssueCount=8`——8 个 `package-path-mismatch` 全为真实生成类（`CodegenConvertImpl` / `ConfigConvertImpl` / `FileConfigConvertImpl` / `RedisConvertImpl` / `AuthConvertImpl` / `OAuth2OpenConvertImpl` / `TenantConvertImpl` / `UserConvertImpl`）；
  - 修复后：`status=0, javaFiles=6776, hardIssueCount=0`。
  - codex 明确核实排除集**未误伤任何已跟踪源码**（"without excluding tracked source files"）。
- **执行顺序**：codex 比对 docs/05 第 16.1 节批次表（L964-971），确认修订后顺序「matches the documented prerequisites」。

codex 复审结论：

> "No actionable regressions found. The verifier eliminates eight generated-source false positives without excluding tracked source files, and the revised execution order matches the documented prerequisites. Nine of ten fast baseline gates passed; the remaining gate failed on unrelated Windows symlink permissions."

原始日志 `[P0]/[P1]/[P2]/[P3]` 标记数：**0**（单轮通过）。

> **关于 codex 侧 G1 FAIL 的说明**：codex 在沙箱内跑 `run-local-gates.mjs --fast` 时 G1（`verify-source-copy.test.mjs`）失败，根因是其中 2 个测试在 fixture 建符号链接时抛 `EPERM: operation not permitted, symlink`（errno -4048）——Windows 创建 symlink 需管理员权限或开发者模式，codex 沙箱不具备。该测试文件与本次改动（`verify-backend-naming.mjs` + `docs/06`）无任何关系；本机完整门禁 G1 PASS、11/11 全通过。codex 亦判定为 "unrelated Windows symlink permissions"，不影响 0 发现结论。

## 验证证据

| 验证项 | 方法 | 结果 |
|---|---|---|
| 命名验证器（修复后） | `node scripts/brand/verify-backend-naming.mjs` | `javaFiles=6776, hardIssueCount=0`，退出码 0 |
| 反事实（修复前逻辑） | 复刻旧 `walk()`（不跳过）+ 旧第 45 行校验，跑当前真实 `target/` | `hardIssueCount=8`，退出码 1（8 个生成类误报） |
| codex A/B 对照 | `git show ec92cb85^` 版本 vs 当前版本，同树对跑 | 前 `6784 / 8 / exit 1` → 后 `6776 / 0 / exit 0` |
| 合成注入复现 | `target/generated-sources/annotations/…/AuthConvertImpl.java`（package 合法、路径无 src 根） | 修复后被跳过（`javaFiles` 不变）；旧逻辑 `MISMATCH=true` |
| 排除集安全性 | `git ls-files services/zhongshu-core` 过滤 `target/build/dist/node_modules/out` | 前四者 0 个已跟踪源码；`out` 有 6 个（erp `vo/out/` 出库 VO），故 `out/build/dist` **不纳入**跳过集 |
| 本地基线门禁 | `node scripts/ops/run-local-gates.mjs` | **11/11 PASS** |
| codex r0 复审 | `codex review --commit ec92cb85` | 0 发现（"No actionable regressions found"） |

## 复核与处置

| 项 | 状态 | 说明 |
|---|---|---|
| ZS-BRAND-002 发现 2（verify-backend-naming 漏排 target/） | ✅ 已修复 | `ec92cb85`；`walk()` 跳过构建产物目录，codex A/B 自验 8 假阳性 → 0 |
| ZS-BRAND-001 单一 P2 发现（docs/06 执行顺序） | ✅ 已修复 | `ec92cb85`；006.A 提前至 004.A/005 之前、004 明确为 004.A，符合 docs/05 §16.1 批次前置 |
| 15.2 品牌专项「仍有效」缺陷 | ✅ 全部清零 | 本表 P2 #1/#11 闭合后，README「仍有效」2 → 0；hotfix-A～E 五个 hotfix 全部完成 |

## 经验登记

1. **静态检查器必须排除构建产物，且排除集要按「是否含源码」而非「名字像不像产物」界定**：`target/generated-sources` 的生成类路径不含 `src/main/java`，被 package-path 校验误判。关键取舍是——`out/build/dist` 看似产物目录，但 `out` 实为 erp `vo/out/`（出库 VO）的合法源码包名（6 个已跟踪文件），一并跳过会制造**假阴性**（漏检真实残留，正是 hotfix-B 的教训）。排除集只收「绝不含后端源码」的 `target/node_modules/.git/.idea`，并以 `git ls-files` 实测佐证、在代码注释中记录取舍理由。
2. **一处修复优于多处补丁**：`walk()` 是全部 5 段扫描的唯一遍历原语，在其中跳过构建目录一处即覆盖所有段；若按来源评审的初始建议在各段 `walk` 调用分别追加过滤（或抽 `isExcluded(p)` 逐段调用），改动面更大且易漏段。收敛到遍历原语更稳，还顺带免去遍历 `node_modules` 的开销。
3. **文档中的执行顺序是「批次前置」的投影，必须与权威批次表一致**：docs/06 L110 把 B01 收口项（006.A）排在 B02 项（004/005）之后，形成循环批次依赖。纯文档缺陷不影响运行时，却会误导后续开发者按错误顺序推进批次。顺序类文档应以批次前置表（docs/05 §16.1）为单一权威源，并用主编号→子项编号（004→004.A）消除跨批次歧义。
4. **A/B 对照是「修 bug 不引入回归」的最强证据**：codex 用 `git show <commit>^` 取修复前脚本与修复后同树对跑，直接量化「前 8 假阳性 / exit 1 → 后 0 / exit 0」，比只跑修复后版本更有说服力。本地门禁脚本类修复宜保留这种「修复前后同输入对照」的验证范式（本次本地反事实脚本与 codex A/B 得出同一组数字，双向印证）。
