# codex 评审处置：ZS-BRAND-004.C 任务与消息持久化引用盘点门禁与运行期新旧切换联验

- 评审对象：`feat/brand-004-c`（worktree `.wt/zszj-wt-brand-004-c`），`scripts/brand/verify-brand-004c-persistence.mjs`（静态门禁）+ `scripts/brand/run-brand-004c-runtime.mjs`（运行期新旧切换联验，待真实环境）
- 评审方式：codex exec（gpt-6-astra / model_reasoning_effort=xhigh / bypass sandbox），r0→r7 共八弧；r4 起弧内携带 javac 17 词法基线对照与内存变异测试（不改仓库文件）
- 收敛结论：**每弧 VERDICT PASS/0×P0/P1（r4 起），P2 逐弧修至归零；r7 PASS（0×P0/P1）后余 1×P2+1×P3 已于 r7 处置（5905f791）修至归零并以成对自检场景锁回归**
- 合并：`--no-ff` main `cceb8598`（处置链 `c8b83381` r0 → `6bc37e05` r1 → `cf60c474` r2 → `e01671a5` r3+r4 前段 → `5e251d61` r4 → `5740bff6` r6 前段 → `5905f791` r7；r3/r5/r6/r7 弧分别对应其前一处置后的复评）

## 弧次与发现

| 弧 | 对应处置 | 发现 | 处置要点 |
|---|---|---|---|
| r0 | `c8b83381` | 4×P2 | 门禁/断言强度加固：白名单两层各自取证、二次重试后重查审计、package 声明推导持久化 FQCN、消息路由覆写判定 |
| r1 | `6bc37e05` | 2×P2 | FQCN 推导覆盖 record/enum 且空结果 fail-loud、路由方法体花括号平衡扫描 |
| r2 | `cf60c474` | 2×P2 | 单一注释掩码 `maskComments` 根治（全部 Java 提取路径统一走掩码后文本），四探针场景入自检 |
| r3 | `e01671a5` | 3×P2 | 裸 `\r` 行终止符（JLS 3.4）、合格 Unicode 转义 JLS 3.3 翻译预处理、文本块开界符 FF；JAVA_LEXICAL_MODEL_BOUNDARIES fail-loud 合同（模型外输入 masked=null 跳过该文件全部判定）；扫描/掩码坐标系统一（r3 自检场景 `masker-unicode-escape-in-comment-translated` 实证初版「原文扫描/译文抹除」坐标错位） |
| r4 | `5e251d61` | 7×P2+3×P3 | JLS 3.3 订正为单遍翻译（产物不再参与转义判定；非法 hex → `unicode-escape-invalid-hex` fatal 不静默修复）；文本块转义感知扫描器先提取摘除再跑字面量正则（根除闭界符余引号错配）；`decodeJavaEscapeValues` 值解码器（八进制/续行/转义全集，`\u` 残留 fail-loud）+ incidental-indent 剥离（先剥离后转义）；反斜杠不吞行终止符；花括号提取器文本块 +3/+1 越界修正；检出力强化；fatal 返回合同统一；fatal 原因索引订正；qrtzSeedRows/sqlFilesScanned 统计接线 |
| r5 | `5740bff6` | 2×P2+2×P3 | `\s` 合法于文本块/普通字符串/char（JLS 3.10.7，r4 误限仅文本块）；空白判定改 Java `Character.isWhitespace` 集合（NBSP 非 Java 空白，JS trim() 误判致假红）；剥离对齐 `String.stripIndent`（行尾裁剪+空行归空）；harness 新增 `maxHits` 负向断言 + 6 个变异回归场景 |
| r6 | `5905f791` 前段 | 1×P2+2×P3 | 「`\`+空白+行尾」剥离前校验（javac 非法转义不得被行尾裁剪洗白为合法续行）；maxHits:0 场景解析失败不再冒充成功；`\s` 与行尾裁剪 expectDerived 精确值断言 |
| r7 | `5905f791` | 1×P2+1×P3 | 非法转义校验补反斜杠奇偶判定（偶数 `\\`+空格 javac 合法，误拦假红订正）；odd/even 成对自检场景（防护删除/奇偶误判变异均转红） |

## 验证

- 自检 45 场景全过（判定命中/fatal 合同/变异回归，关键场景 expectDerived 值形状与 maxHits 负向断言承担检出力）
- 全仓 7000 个 Java 文件：oldNameViolations=0、contractProblems=0、fatal/空掩码跳过=0（排除跳过洗白）
- codex 独立对照：isJavaWs 全 Unicode 码点 vs JDK17 一致（25 空白字符）；2,405 组剥离/解码合同对照无差异；六项变异（八进制吞三位/空行参与缩进/闭界偏移+3/删转义感知/删跨行检查/NBSP 用 trim）全部被场景检出
- 运行期脚本 `run-brand-004c-runtime.mjs` 仅只读审阅，待真实环境全面联验（归 B06 ZS-BRAND-006.B）

## 登记边界

1. 运行期新旧切换联验需真实 PG/Redis 与两端环境，归 B06 ZS-BRAND-006.B 批次放行验证。
2. 门禁判定面聚焦持久化引用（路由值/任务种子/FQCN/审计常量/键前缀），不替代 BRAND-006.A 的全仓残留 CI 检查。
3. r5 起 codex 以「每弧对上一轮处置做变异验证」的方式驱动，若后续再改门禁词法模型，须同步补 expectDerived/maxHits 级别的值断言场景（纯命中断言无检出力，r5 P2-7 教训）。
