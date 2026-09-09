# Codex 评审：hotfix-B — ZS-BRAND-006.A 品牌命名门禁失效缺口修复

## 评审元信息

| 项目 | 值 |
|---|---|
| 评审对象 | `86bca7f7`（r0）→ `c21fbea5`（r1）→ `0cf982ce`（r2）→ `a5ce1032`（r3）→ `5d11b205`（编号订正） |
| 评审工具 | codex-cli 0.153.4, model gpt-6-astra, reasoning effort xhigh |
| 评审轮次 | 4 轮 codex 评审（r0/r1/r2/r3）+ 1 次人工自查加固 |
| 关联评审 | [codex-ZS-BRAND-006.A.md](codex-ZS-BRAND-006.A.md) |
| 修改文件 | 6 files, +257/-38（`verify-brand-naming.mjs`、`brand-naming-allowlist.json`、`verify-brand-naming.test.mjs` + 3 个 `.http` 样例） |
| 原始日志 | [codex-hotfix-B.raw.md](codex-hotfix-B.raw.md)、[-r1](codex-hotfix-B-r1.raw.md)、[-r2](codex-hotfix-B-r2.raw.md)、[-r3](codex-hotfix-B-r3.raw.md) |
| 最终状态 | ✅ 通过（r2、r3 连续两轮 codex 0 发现；15/15 单测；全仓扫描 0 违规；本地门禁 9/9） |

## 修复的原始缺陷

本 hotfix 修复 [codex-ZS-BRAND-006.A.md](codex-ZS-BRAND-006.A.md) 结论中「仍有效」的全部 5 项发现：

| 编号 | 优先级 | 原始缺陷 | 修复方式 |
|---|---|---|---|
| P1-3 | P1 | `pathPass` 忽略 `scope`，`scope=content` 条目被误当 whole-file 豁免，门禁有效性被架空 | `judge()` 的 `pathPass` 增加 `(!e.scope \|\| e.scope === 'path')` 前置条件；content 条目必须逐命中经内容匹配判定 |
| P2-4 | P2 | 例外匹配未验证覆盖，±40 字符窗内任一白名单串可庇护不相关命中 | coverage check：要求白名单正则的匹配区间 `[cm.index, cm.index+len)` 覆盖命中点偏移；`contentRes` 补 `g` 标志以遍历全部匹配 |
| P2-5 | P2 | 署名例外过宽，裸 token `芋道源码` 无上下文限定，产品可见位置可滥用 | 拆分为带上下文锚点的具体形态（`@author`／行注释／冒号标题／URL／署名括号）；测试夹具形态（`set*`/`thenReturn`/`assertEquals`/`regex =`）迁出全局条目并限定 `(^|/)src/test/` |
| P2-6 | P2 | 文件名前过滤只检查 basename + 3 个模式，遗漏目录残留与 `cn.iocoder`/`unibest`/`yd-` | `scanTree()` 对**完整相对路径**应用全部 6 个 PATTERNS，并以 `judge(rel, rel, …)` 复用路径白名单判定 |
| P2-7 | P2 | SVG 资产被作为二进制跳过，产品可见 SVG 品牌残留不被检测 | 从 `BINARY` 移除 `svg` 并导出该常量以供单测固化；SVG 作为 XML 文本进入内容扫描 |

### 编号对照说明（避免与 README 汇总表混淆）

本仓库存在两套编号，查阅时须注意：

| 缺陷 | ZS-BRAND-006.A 评审文档内编号（本文与代码注释采用） | [README.md](README.md) 汇总表内编号 |
|---|---|---|
| pathPass 忽略 scope | P1-3 | P1 表 #2 |
| 例外匹配未验证覆盖 | P2-4 | P2 表 #7 |
| 署名例外过宽 | P2-5 | P2 表 #8 |
| 文件名前过滤不全 | P2-6 | P2 表 #9 |
| SVG 被跳过 | P2-7 | P2 表 #10 |

代码注释一律引用**来源评审文档**的编号（即左列），`5d11b205` 已订正早期写错的 3 处。

## P1-3 的实际影响量化

修复前门禁对外报告 `violations=0`（ZS-BRAND-006.A 评审时记录为 `scanned=13463 allowedHits=50667 violations=0`），但该 0 是**假阴性**。

以修复后的扫描器逻辑回测 hotfix-B 之前（`e612ef4e`）的白名单：

```
e612ef4e  scanned=13538  allowedHits=51052  violations=257
          {"cn.iocoder":22,"yudao":185,"unibest":21,"youdao":9,"芋道":20}
86bca7f7  scanned=13538  allowedHits=51309  violations=0
```

即：**257 个命中在旧门禁下仅凭路径匹配即被整文件放行，从未经过任何内容级校验**。这些命中覆盖全部 5 个品牌模式。修复后每一项都必须由某条 content 例外的匹配区间逐一覆盖才可放行；无法正当化的 4 处（3 个 `.http` 样例文件中的 `detailAddress`/`name`/`nickname` 取值 `芋道源码`）按先例 `52a537f5` 直接改名为 `众墅之家`，而非扩大白名单。

> 该数字的含义是「从无条件豁免转为逐命中举证」，不等于 257 处都是必须改名的残留——其中多数是正当的上游署名与来源链接，但修复前它们与真实残留**在门禁眼中不可区分**。

## Codex 评审迭代过程

### 第 1 轮（`86bca7f7`，r0）— 结论「无回归」，但自带证据显示 3 项未闭合 ⚠️

codex 结论：

> "No actionable regressions introduced by this commit were found. All 12 naming tests, 13 targeted probes, and the full-tree naming scan passed."

**未采信该结论。** codex 的审查范围是「本次提交引入的回归」，而其自身输出的 before/after 探针中，有 3 个样本 `after` 仍为 0（即修复后依旧被庇护），与「P2-4/P2-5/P1-3 已闭合」的目标直接矛盾：

```json
{"sample":"<a href=\"https://doc.iocoder.cn\">芋道管理系统</a>","before":0,"after":0}
{"sample":"setTitle(\"芋道源码管理平台\");","before":0,"after":0}
{"sample":"\"name\": \"unibest\"","before":0,"after":0}
```

根因诊断（3 条）：

| # | 根因 | 说明 |
|---|---|---|
| r0-a | 尾随通配 `\S*` 过贪 | `[^\s"'<>()]*` 虽排除引号，但 URL 后的 `">` 仍被并入匹配区间，跨越 HTML 属性边界庇护紧随的产品标题 |
| r0-b | 测试形态模式位于无 `path` 限定的全局条目 | `set\w+\(.*芋道源码` 原意是放行测试夹具，却使应用侧 `setTitle("芋道源码管理平台")` 同形态调用被庇护（实测真实命中全部位于 `/src/test/`） |
| r0-c | `package.json` 条目含裸 token `"unibest"` | 庇护该文件内任意位置，包括 `name` 等产品元数据字段 |

### 第 2 轮（`c21fbea5`，r1 修正）— 发现 2 × P2（本次提交引入的回归）

r1 修正内容：URL 字符集收紧为 `[^\s"'<>()]*`；4 条测试形态模式迁入新的 `(^|/)src/test/` 条目；`example\s*=[^)]*`；`package.json` 改为 provenance 字段键／URL／模板路径三类结构化模式；删除失效的 `feige996`/`1020103647`。

codex 复审确认 3 个探针已闭合，但发现 r1 收紧本身引入 2 项新 P2：

| # | 优先级 | 发现 | 位置 | 复现证据 |
|---|---|---|---|---|
| B-r1-1 | P2 | `[^)]*` 与 `[^"'<>()]*` **跨行终止符**（不同于 `.*`），无关赋值可跳行豁免后续产品代码；注释模式在「注释后接反引号标题」时同理 | `brand-naming-allowlist.json:158`、`:151` | `const example = 1;\nconst title = "芋道源码管理平台";` → 本提交 0 违规，父提交 1 违规 |
| B-r1-2 | P2 | package URL 豁免**不止于 JSON 转义**：`\s` 不含反斜杠，转义序列 `\n` 被读作两个非空白字符，URL 匹配一路覆盖到产品标题 | `brand-naming-allowlist.json:128` | `{"description":"https://unibest.tech\n芋道管理系统"}` → 本提交 0 违规，父提交 1 违规 |

**共同根因**：`.` 不匹配 `\n`，但**显式否定字符集既匹配 `\n` 也匹配 `\r`**；且 `\s` 不含 `\`。r1 把 `.*` 换成否定字符集时，在收紧引号边界的同时意外放宽了行边界。

### 第 3 轮（`0cf982ce`，r2 修正）— ✅ 通过（0 发现）

r2 修正内容：

- 全部 13 处 URL/域名否定字符集补入 `\` 排除 → `[^\s"'<>()\\]*`，使匹配止于 JSON 转义；
- `example` 与行注释两处字符集补入 `\r\n` 排除。

前置核查：`git grep -E 'https?:\\/'` 结果为空，仓库内不存在转义斜杠 URL，故排除 `\` 不会截断任何真实上游链接。

codex 复审结论：

> "No actionable regressions were found. The narrowed allowlist patterns address the reported boundary issues; all 14 brand-naming tests pass, and the repository scan reports zero violations."

原始日志 `[P1]/[P2]/[P3]` 标记数：**0**。

### 第 4 轮（`a5ce1032`，r3 人工自查加固）— ✅ 通过（0 发现）

**触发理由**：r1/r2 两项发现同属一类根因——「`\s` 与显式字符集比 `.*` 宽，会吞掉换行」。codex 的审查范围限于「本次提交引入的回归」，因此不会检查**既有**模式中的同类缺陷。据此对全部 59 条 content 模式做系统化穷举自查（无关前缀 × 旧品牌串 × `{\n, \r\n}`，676 组探针/模式），发现 3 条既有模式仍可跨行豁免：

| 既有模式 | 可利用输入 | 说明 |
|---|---|---|
| `@author\s+芋道源码` | `"@author \n芋道源码管理平台"` | javadoc `@author` 尾随换行 |
| `-+\s*芋道源码` | `"---\n芋道源码管理平台"` | markdown 分隔线／列表符后换行 |
| `\)\s*芋道源码` | `"[文档](https://x.com)\n芋道源码管理平台"` | 任意右括号紧邻换行 |

其中 markdown 分隔线与右括号紧邻换行在文档/代码中极为常见，一旦下一行以「芋道源码」起头即被静默放行。

r3 修正：将全部 9 条署名类模式中的 `\s` 统一收窄为 `[^\S\r\n]`（仅水平空白）。URL/域名类模式使用 `[^\s…]` 负向字符集，本身已排除换行，未作改动。

codex 复审结论：

> "No actionable regressions were found. All 15 brand-naming tests, 57 cross-commit probes, and the full repository scan passed. The broader baseline only failed on unchanged symlink tests due to Windows EPERM restrictions."

原始日志 `[P1]/[P2]/[P3]` 标记数：**0**。（唯一失败项为 `scripts/verify-source-copy.test.mjs` 的符号链接用例，属 Windows 非管理员环境 `EPERM` 限制，与本 hotfix 无关。）

## 自查发现的额外缺陷（超出 codex 清单）

hotfix-B 期间另发现并修复 4 项 codex 未列出的门禁缺陷：

| # | 缺陷 | 影响 | 修复 |
|---|---|---|---|
| E-1 | `contentRes` 编译时缺 `g` 标志，coverage check 只取首个匹配 | 同一白名单模式在 ±40 窗内多次出现时后续命中无法被覆盖 → **970 处假阳性** | `new RegExp(c, 'g')`，每次使用前重置 `lastIndex` |
| E-2 | `yd-*` 限期兼容条目**无 `path` 限定** | `judge()` 中 `if (e.pathRe && !e.pathRe.test(…)) return false;` 在 `pathRe` 为 null 时直接放行 → `yd-*` 例外**全仓生效**，管理端等其他端残留被放行 | 补 path，覆盖小程序本体 + codegen uniapp 模板 + 其同批夹具/断言 + 门禁脚本自身 |
| E-3 | `^docs/0[1-6]-` 条目标注 `scope:"content"` 但**无任何 content 模式** | P1-3 修复后该条目将放行 0 个命中——它此前完全依赖 pathPass 缺陷生效，属静默失效 | 移除错误的 `scope`，改为整文件放行 |
| E-4 | codegen `README.md` 中反引号包裹的检测模式字面量（`` `yudao` `` 等）被判为残留 | 5 处假阳性；因 PATTERNS 的 `yudao` 为 `/gi` 而 content 正则大小写敏感，需 `Yudao`/`YUDAO` 独立字面量 | 新增精确锚定 `^…/codegen/README\.md$` 的 content 条目 |

E-2 的发现过程本身即是一条教训：初次用 `git grep … | Select-Object -First 30` 评估影响面，**截断的输出隐藏了 `src/test/` 下的 117 处真实命中**，导致首轮 path 范围划得过窄；补全后按 codegen README 记载的「模板、夹具、断言三者必须同批修改」原则扩展至测试夹具与测试 java。

## 验证证据

| 验证项 | 方法 | 结果 |
|---|---|---|
| 单元测试 | `node --test scripts/brand/verify-brand-naming.test.mjs` | **15/15 通过**（hotfix-B 前为 7 项） |
| 全仓扫描 | `node scripts/brand/verify-brand-naming.mjs` | `scanned=13538 allowedHits=51309 violations=0` |
| 本地基线门禁 | `node scripts/ops/run-local-gates.mjs --fast` | **9/9 PASS**（G1–G9） |
| 端到端探针 | 临时 harness：建探针文件 → `git add --force` → 真实 `scanTree()` → 断言 → 清理 | **16/16 通过**（10 负向含 codex 原始复现路径 + 6 正向） |
| 真实文件名覆盖 | 对 199 个已跟踪文件名逐一跑 PATTERNS | 全部按预期判定 |
| 跨提交对照（r2） | 同一探针分别以父提交与当前白名单判定 | 4 负向 `shielded → VIOLATION`；4 正向两侧均 0 违规 |
| 跨提交对照（r3） | 同上，含 6 项同行真实署名正向对照 | 4 负向 `shielded → VIOLATION`；6 正向两侧均 0 违规；`fail=0` |
| 豁免范围未扩大 | 对比 `allowedHits` 增量与改动文件新增夹具 token 数 | r2：+15 = 2（allowlist）+13（test）；r3：+20 = 0+20，逐项吻合 |
| 跨行豁免不变式 | 穷举 59 模式 × 25 前缀 × 8 品牌串 × 2 换行形式（约 23,600 组） | **risk=0**，已固化为常驻单测 |

**跨提交对照是本轮的关键方法**：仅断言「当前判定为违规」不足以证明测试有效——该断言可能因路径被整文件豁免、或输入本身不触发模式而空洞通过。r3 首轮即由此发现自写的括号用例 `foo();\n芋道源码…` 无效（`)` 与换行之间隔着 `;`，本就不会被庇护），订正为 `foo()\n…` 后才真实成立。

## 复核与处置

| 项 | 状态 | 说明 |
|---|---|---|
| P1-3 pathPass 忽略 scope | ✅ 已修复 | `86bca7f7`；量化影响 257 处命中由无条件豁免转为逐命中举证 |
| P2-4 例外匹配未验证覆盖 | ✅ 已修复 | `86bca7f7` 引入 coverage check；`c21fbea5`/`0cf982ce` 收紧字符集边界 |
| P2-5 署名例外过宽 | ✅ 已修复 | `86bca7f7` 拆分锚点形态；`c21fbea5` 测试形态迁入 `src/test`；`a5ce1032` 消除跨行豁免 |
| P2-6 文件名前过滤不全 | ✅ 已修复 | `86bca7f7`；完整相对路径 × 全部 6 PATTERNS |
| P2-7 SVG 被跳过 | ✅ 已修复 | `86bca7f7`；`BINARY` 移除 `svg` 并导出以供单测固化 |
| B-r1-1 字符集跨行终止符 | ✅ 已修复 | `0cf982ce`（codex r1 发现） |
| B-r1-2 URL 不止于 JSON 转义 | ✅ 已修复 | `0cf982ce`（codex r1 发现） |
| E-1 缺 `g` 标志（970 假阳性） | ✅ 已修复 | `86bca7f7`（自查） |
| E-2 `yd-*` 例外全仓生效 | ✅ 已修复 | `86bca7f7`（自查） |
| E-3 `scope=content` 无模式的静默失效条目 | ✅ 已修复 | `86bca7f7`（自查） |
| E-4 codegen README 模式字面量假阳性 | ✅ 已修复 | `86bca7f7`（自查） |
| 代码注释发现编号错误 | ✅ 已订正 | `5d11b205`（P2-7→P2-4、P2-9→P2-6、补齐 P2-5/P2-7） |
| 现有 SVG 资产品牌残留核查 | ⚠️ 部分 | 门禁已可检测；`docs/assets/brand/` 下 SVG 资产的逐一人工核查归 ZS-BRAND-006.B |
| `yd-*` 例外撤销 | ⏳ 待条件 | 登记为「限期兼容」，撤除条件：ZS-CLIENT-005 移动端构建门禁可用后按批次替换为 `zszj-*` |

## 经验登记

1. **codex「无回归」结论不等于目标达成**：其审查范围是「本次提交引入的回归」。r0 结论为通过，但同一份输出中的 before/after 探针有 3 项 `after:0`，直接 contradict 修复目标。**必须核对评审输出中的证据数据，而非只读结论文本。**
2. **收紧一类边界时须检查是否放宽了另一类**：r1 把 `.*` 换成否定字符集以收紧引号边界，却意外放宽了行边界（`.` 不含 `\n`，而 `[^)]` 含）。同类根因应在**全部**模式中一次性排查，而非只改被点名的两处——这正是 r3 自查发现 3 条既有模式仍可利用的原因。
3. **`\s` 与显式字符集默认跨行**：凡用于「同一逻辑单元内」的正则，应显式使用 `[^\S\r\n]`（水平空白）或在否定字符集中加入 `\r\n`。JSON 字符串中的 `\s` 还需注意转义层级（`\\s`），prose 中宜改用自然语言描述以免产生非法转义。
4. **断言「当前失败」的测试可能是空洞的**：路径整文件豁免、输入不触发模式、锚点位置错误都会让它假通过。**跨提交对照**（同一探针在父提交与当前分别判定，要求 `shielded → VIOLATION`）是成本最低的防空洞手段。
5. **无 `path` 的白名单条目 = 全仓豁免**：`judge()` 的 `if (e.pathRe && !e.pathRe.test(…)) return false;` 在 `pathRe` 为 null 时短路放行。新增例外必须显式限定 path，并由常驻结构自检单测把关。
6. **`scope=content` 而无 `content` 模式属静默失效**：修复 P1-3 后此类条目放行 0 个命中，其此前生效完全依赖缺陷。已加入结构自检单测固化该不变式。
7. **截断的 grep 会导致错误的范围决策**：`Select-Object -First 30` 隐藏了 `src/test/` 下 117 处真实命中。评估影响面必须取全量计数，或先 `--count` 再决定是否截断。
8. **扩大白名单 vs 改名**：能改名且不损失语义的（如 `.http` 样例数据），按先例 `52a537f5` 直接改名，不为其新增例外——白名单每条都是长期负债。
9. **`allowedHits` 增量应与新增夹具 token 数逐项对账**：这是检测「豁免范围被意外扩大」的廉价哨兵指标；只要它随违规数下降而异常上升，就说明有命中被新例外错误吞掉。
