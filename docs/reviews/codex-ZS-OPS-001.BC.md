# ZS-OPS-001.B/.C codex 评审处置（r0→r1→r2 三弧，连续两轮揪出修复自身缺陷）

- 评审工具：codex（gpt-6-astra / xhigh，read-only sandbox，`codex review --commit <SHA>` @ main）
- 评审对象：OPS-001.B/.C 门禁接入提交 `d92924ad`（G14 门禁 + CI sys001 job + .B 证据登记）及其修复链 `d057df3d`/`04e8516e`
- 结论：**r2 CLEAN / 0 发现**（评审收敛）
- 原始日志：`outputs/ops001-codex-r{0,1,2}.txt`（未入库；本档案为处置入库）

## 交付摘要（被审内容）

- **.B PG 层流水线**：pg-regression.yml（CI，9 套件含 BPM-001 两阶段）+ run-pg-regression.mjs 本地同入口；固定提交多轮实跑全绿、缺 PG 退出码 3、H2 不作门禁（方言决策固化）、flaky 处置约定。
- **.C 安全/基础管理 API 层门禁**：安全层 = G12（SEC-012.A/.B 交付）；基础管理 API 层 = **新增 G14**（run-local-gates 第 15 项 slow+exclusive 门禁，cmd=`run-sys001-regression.mjs` 七类 50 用例）+ CI `sys001-regression` job 同入口；gates 单测同步 15 项 17/17、--fast 保持 10 项。
- **G14 当前红灯＝门禁本职**：实跑 48/49，唯一 FAIL=SYS-ROLE-N1（GAP-3，真实 PG 越界赋权不拒绝，归口 CFG-003.B 跨 PG 复验 + DB-001 依赖升级）——「越权/基础功能失败阻止 B03」按设计生效。

## 评审弧

| 弧 | 提交 | 发现 | 处置 |
|---|---|---|---|
| r0 | `d92924ad` | **P1** sys001 harness 硬编码 Windows/tools 工具链（`tools/.../mvn`、`tools/.../java.exe`）——CI ubuntu runner 无 `tools/`（不入库），job 会在断言前就失败；**P2** G14 与 G11/G12 并发时其 `clean package` 删改共享 target/ 致时序性构建/缺类失败 | `d057df3d`：工具链解析改「JAVA_HOME → tools/ → PATH」三级回退（mvn 同理，launcher 按平台）、java 可执行名平台化；G14 加 `exclusive: true` + runner 增独占串行道 |
| r1 | `d057df3d` | **P1** 串行实现错误——`Promise.all([serial(), ...workers])` 独占道与并发池仍同起，G14 照样与 G11/G12 重叠，且 `--jobs 1` 也会双跑（违背显式并发上限） | `04e8516e`：改「并发池先行、独占串行随后」顺序执行 |
| r2 | `04e8516e` | **CLEAN / 0 发现**：「The change correctly waits for all non-exclusive gates to finish before starting exclusive gates, eliminating overlap while preserving result ordering and the configured concurrency limit.」 | — |

## 验证

- `node --test scripts/ops/run-local-gates.test.mjs`：17/17（含 GATES 15 项、slow=['G10','G13','G14']、exclusive=['G14'] 断言）
- `run-local-gates --fast`：10/10（与 CI fast job 一致）
- G14 实跑留痕：48/49（outputs/ops001c-g14.log，EXIT=1 如实反映 GAP-3）
- P1 修复的本地行为不变性：tools/ 存在时解析结果与修复前一致（existsSync 判定），CI 行为按 setup-java 供给

## 经验登记

1. **治理脚本自身是最高危改动面**：门禁接线提交连续两轮被复评揪出「修复自身缺陷」（r1 串行实现错误是 r0 P2 修复引入的）——与 SEC-011.A（包名前缀→instanceof）、SEC-007（物化 fallback 携带原文）同构，确认「每修复提交必复评至归零」无例外。
2. **并发原语的语义审查点**：`Promise.all` 组合两条本应有序的执行道 = 隐式并发；`--jobs N` 是显式契约，任何实现不得在 N=1 时双跑。
3. **工具链假设必须显式声明**：本地自引导（tools/）与 CI 供给（setup-java/JAVA_HOME/PATH）是两套事实，跨环境脚本须三级回退并以 existsSync 判定。
