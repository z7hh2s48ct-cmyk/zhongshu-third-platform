# ZS-OPS-002.A codex 评审处置（r0→r19 二十弧）

- 评审工具：codex（gpt-6-astra / xhigh，逐轮以**内存复现串实测**校验器判定——r8~r18 弧各带复现脚本与
  逐条翻转证据；r12~r14 另实测多项 O(n²)→O(n) 性能回归：16000 次美元闭合 2299ms→11ms、64000 次有界换行
  897ms→37.8ms、8000 次 fragEnd 2325ms→20.5ms、32000 次 `--` 搜索 840ms→111ms）
- 评审对象：分支 `feat/ops-002-a`（ZS-OPS-002.A 脱敏部署模板与可审查性校验——deploy/ compose·nginx·
  PG 初始化模板 + scripts/ops/verify-deploy-templates.mjs 校验器）
- 结论：**r19 复评 PASS（0 findings）——r0→r19 二十弧正式收敛**；r2 起 0×P0/P1、全部 P2 弧内修复推进、
  存量归零，r18 处置（`1bf8de05`）经 r19 复评确认，不以处置提交自证
- 说明：r0/r5 两弧原始 stdout 已入库（[codex-ZS-OPS-002.raw.md](codex-ZS-OPS-002.raw.md)、
  [codex-ZS-OPS-002-r5.raw.md](codex-ZS-OPS-002-r5.raw.md)）；r6→r19 各弧裁决 JSON 落盘 outputs/；
  r1~r4 裁决未随暂存保留，发现与处置按提交说明摘录，如实登记。

## 交付内容（impl commit `8159cf2c`，8 files +3047；处置至 `1bf8de05`；`--no-ff` 合并 main `63a20334`（r11）+ `19a53722`（r12→r19））

1. **脱敏部署模板**（`services/zhongshu-core/deploy/`）：
   - `docker-compose.deploy.yml`：PostgreSQL 16 + Redis 7 + zszj-server + Nginx 编排，敏感配置全经
     `${ZSZJ_*}` 环境变量注入、无硬编码凭据；zszj-server healthcheck 探针 `/actuator/health/liveness`；
     postgres/redis 端口仅绑定 127.0.0.1（不公开暴露）。
   - `.env.example`：配置注入合同（ZSZJ_SERVER_PORT/PROFILE/DATASOURCE_*/REDIS_*/TLS_*），全占位符/空值；
     `.gitignore` 追加例外使合同模板可入库。
   - `nginx/zszj-server.conf`：TLS 终结（证书 volume mount 注入）+ `/actuator/` 与 `/admin/` deny all +
     `/infra/ws` WebSocket 转发。
   - `postgres-init/01-create-app-role.sql`：应用低权限角色幂等 provision（`\gexec` + `format(%I/%L)`，
     部署时从环境变量读取凭据，与 owner 分离，符合 ZS-DB-002）。
2. **校验器**（`scripts/ops/verify-deploy-templates.mjs`）：C1 模板无真实秘密（复用 ZS-CFG-001.A 判定口径）/
   C2 探针路径与 actuator exposure.include 一致 / C3 敏感管理路径反代关闭 / C4 内部端口不公开暴露 /
   C5 环境变量合同双向一致；`--self-test` 负向对照。
3. **测试**：`verify-deploy-templates.test.mjs` 21→114 用例（单文件 node --test 口径，正向 + 负向对照逐弧累积；r11 文档所记 116 为 scripts/ops 三套件合计）。
4. **文档**：`services/zhongshu-core/docs/部署模板与探针边界.md`（部署/撤回步骤、配置注入合同、探针边界、
   TLS 约束；如实标注 .B/.C 批次未执行项，不虚报生产可用）。

## 评审弧（校验器词法/语义逐轮加固）

| 轮次 | 结论 | 发现 → 处置 |
|---|---|---|
| r0（`8159cf2c`） | FAIL 3×P1+7×P2 | P1-1 dynamic-datasource 属性绑定错（SPRING_DATASOURCE_* 直映不适用于 yaml 占位符）→ volume mount application-prod.yaml 供占位符解析；P1-2 Spring Boot 3 Redis 属性路径错（SPRING_REDIS_* 为 2.x）；P1-3 PG 凭据未分离 → ZSZJ_PG_BOOTSTRAP_*（owner）与 ZSZJ_DATASOURCE_*（应用）分离；P2 修复 6 条（Redis exec-list 分词/Nginx healthcheck 443/WebSocket 转发/C4 容器端口分类/C3 排除注释行/C2 专属探针/C1 命令行凭据），1 条延后（upstream 端口对齐归 .B/.C）→ `685644d0`（28 测试、self-test 9） |
| r1（`685644d0`） | FAIL 2×P1+2×P2 | P1-1 配置挂载路径与镜像 WORKDIR 不一致（/app/config 被 prod profile 忽略 → 占位符无法解析）→ 对齐 `/zszj-server/config/`；P1-2 应用低权限角色未 provision（官方镜像仅建 owner）→ postgres-init SQL 幂等建角授予 CRUD；P2-1 C1 exec-list 引号逗号分隔凭据漏检；P2-2 extractServiceBlock 空行截断假阳性 → `ef403957`（31/12） |
| r2（`ef403957`） | FAIL 0×P0/P1+2×P2 | P2-1 服务头带注记（行内注释/anchor/缩进）静默跳过（父提交能拒）；P2-2 SQL 原生凭据语法（`PASSWORD '字面量'`/`\set`）揭穿「scanned 3→4 却检不出」虚报 → 任意缩进/anchor/注释兼容 + SQL_PASSWORD_RE/SQL_SETPASS_RE → `ca7ef24d`（38/16） |
| r3（`ca7ef24d`） | FAIL 0×P0/P1+3×P2 | r2 修复引入假阳性：P2-1 任意缩进误命中 nginx.depends_on 深层同名键（服务重排改变校验结果）→ 限定 services 直接子键；P2-2 低缩进注释截断块误报缺探针 → 注释行归入块内；P2-3 SQL 引号奇偶把闭合引号当字面量起始（`|| quote_literal(:'app_pass')` 误报）→ isQuoteOpener → `257c3185`（41/19） |
| r4（`257c3185`） | FAIL 0×P0/P1+2×P2 | r3 修复引入假阴性（伪绿）：P2-1 注释内撇号使引号奇偶失真、硬编码口令漏检 → stripSqlComments 词法去注释；P2-2 注释行纳入块后被搜出伪造探针证据（`# TODO: add healthcheck at /actuator/...`）→ 注释 continue 跳过（不终止、不作证据） → `8ea43a81`（43/21） |
| r5（`8ea43a81`） | FAIL 0×P0/P1+3×P2 | P2-1 注释掉的凭据仍检出（注释不消除版本库秘密）；P2-2 双引号标识符内 `--`、美元引用内 `/*`、跨行字符串状态不重置 → sqlContextMap 词法上下文图（NORMAL/行注释/嵌套块注释/单引号/双引号/美元引用）替代裸奇偶；P2-3 `.sql` 扩展名 gate 使 compose 内嵌 `psql -c` 漏检 → 全文件生效 → `76e61678`（48） |
| r6（`76e61678`） | FAIL 6×P2 | 同行多匹配漏检（被排除候选消费后续候选）→ 候选独立解析；全文件套用 SQL 词法器未隔离 YAML 上下文（注释撇号开跨行串）→ YAML command 先解码再建 SQL 上下文；注释中安全拼接误报；quote_literal 硬编码参数与变量参数不区分；E 串反斜杠转义误当闭引号；美元 tag 边界（非 ASCII tag/标识符内 `$tag$`） → `25e65e23` |
| r7（`25e65e23`） | FAIL 8×P2 | YAML 标量边界（plain 标量内 SQL 引号被误拆）/双引号 `\xNN`·`\uNNNN`·`\UNNNNNNNN` 转义解码/fragSq 自然语言撇号/首个安全字面量后无条件 break/物理换行与注释分号提前终止/PASSWORD NULL 与注释空串误报/format %L 占位符-实参映射/占位符前缀豁免过宽；+`\set` 单行元命令边界（r7.1 真实模板 format 串误报） → `b1aea707`（self-test 47、测试 72） |
| r8（`b1aea707`） | FAIL 9×P2 | 嵌套块注释深度消费/节点属性（&anchor/!!tag）后保持标量态/format 逐 token（%% 不消耗实参、%n$ 分计）/双单引号转义对/splitCallArgs 词法跳过/`\|`·`>` 块标量内容收集还原+行号回映/相邻串跨行拼接/expr 窗口括号深度状态机/元命令行内未引用反斜杠边界 → `0f65d227`（82/82） |
| r9（`0f65d227`） | FAIL 9×P2 | format 参数游标（「从最后消费的参数继续」语义）/YAML 块头词法（`\|2-` 与 `\|-2` 两序、序列与映射键基准、注释 `# >` 不误触发）/显式缩进 `\|2`/`>` 折叠（单换行折空格、空行保留、more-indented）/序列节点层级/expr 外层括号（SELECT ( / CAST( 包装）/片段注释消费/反引号 shell 引用/变量插值 `:'name'`·`:"name"` → `58f12b16`（82/82） |
| r10（`58f12b16`） | FAIL 10×P2+1×P3 | 块头节点属性/冒号前空格/plain 标量中段 `- \|` 回溯/format 宽度占位符（consumeFormatSpecifier 与游标共用词法，%L/%n$L/%*L/%*n$L）/子查询隔离（操作数模型，WHERE 条件字面量不再当拼接值）/片段括号（DO $$ EXECUTE…，自文本头正向扫描）/片段注释引号（DOLLAR_QUOTE 体内注释整体消费 + markFragmentQuotePairs）/反引号字面量/`:name` 未引用插值/空格缩进与制表符折叠；P3 折叠行映射（outSegs 段列表精确回映物理行） → `1f454b37`（116/116） |
| r11（`1f454b37`） | FAIL 10×P2 | YAML 映射分隔空白（`:\|` 伪块头）/引号键扫描起点（scalarStart 状态机、撇号键）/CAST 完整类型短语（多词/带参/引用）/注释括号隔离（buildParenDepthMap）/WHERE 子句跳过（skipToClosingParen 相对深度配对）/美元体注释配对（上限为闭合定界符）/psql 变量词法（sticky 全量消费、数字起始、非 ASCII、去 64 字符截断）/反引号 shell 隔离/括号深度 O(n²)→O(n) 缓存 → `b6f06019`（self-test 81/81、复现脚本 12/12、116/116、fast 10/10） |
| r12（`e07a9dd2`） | FAIL 8×P2 | YAML 属性词法完整消费（anchor 点号/非 ASCII）+注释片段独立深度+美元体局部词法+CAST 类型参数词法配对+引用类型独立解析+真实 CAST 判定（openAt 栈）+片段局部配对+美元闭合上限预缓存（16000 次 2299ms→11ms）→ `e07a9dd2`（90） |
| r13（`1595f2a4`） | FAIL 9×P2 | isCastCallParen 词法化（取消 8 字符窗口）+AS 按所在层+注释片段层栈同步 openAt+嵌套注释独立层+撇号统一+splitYamlSegments 复用 yamlAttrNameEnd+fragCtx 双引号消费+片段边界硬上限+newlineWithin 有界换行（64000 次 897ms→37.8ms）→ `1595f2a4`（99） |
| r14（`0243983f`） | FAIL 5×P2 | isCastCallParen ctxMap 左向扫描（字符串内 `--` 不误判）+fillFragment 双引号消费+注释定界符第二字符补 ctx+nextNormal 表 fragEnd O(1)（8000 次 2325ms→20.5ms）+删全文 `--` 搜索（32000 次 840ms→111ms）→ `0243983f`（104） |
| r15（`244fc03e`） | FAIL 3×P2 | 注释区间表驱动 CAST 判定（buildParenDepthMap）+fillSpan/markInnerCommentSpans 片段内局部注释整段跳过+双引号消费层界约束 → `244fc03e`（107） |
| r16（`f27b8e1d`） | FAIL 3×P2 | 注释区间真实边界独立回填（引号语义不改写外层终点）+区间跳转前置于片段 ctx 检查+美元体行注释局部块注释入表 → `f27b8e1d`（110） |
| r17（`d27a5bb7`） | FAIL 1×P2 | 收窄片段范围丢失外层注释状态、L1088/L1116 层界保护回退 → markInnerCommentSpans 外层哨兵 coversOuterComment（BLOCK + 美元体块注释两调用点）→ `d27a5bb7`（112） |
| r18（`1bf8de05`） | FAIL 1×P2 | 哨兵永驻与 fillFragment 层状态分叉 → `*/` 遇哨兵栈顶弹出但不填区间，与体内嵌套闭合弹出外层层、回落基层引号语义同步 → `1bf8de05`（114） |
| r19（`1bf8de05`） | **PASS（0 findings）** | 以复评结论自证收敛——r18 处置经 r19 复评零发现，二十弧 r2 起 0×P0/P1、P2 存量归零 |

## 验证（纯静态/node 校验器，未跑 Docker）

- `node verify-deploy-templates.mjs --self-test`：负向对照逐弧累积加固（impl 5 项起，终态 **81/81 PASS**）。
- `node --test verify-deploy-templates.test.mjs`（单文件口径）：21→28→31→38→41→43→48→…→72→82→90→99→104→
  107→110→112→**114/114** 用例逐弧累积（r11 所记「116」为 scripts/ops 三套件合计：run-local-gates 17 +
  verify-deploy 82 + worktree-orchestrate 17，非单文件计数）。
- r11 复现脚本（`outputs/ops002a-r11-repro.mjs`）**12/12**（漏检×9 检出、误报×2 消除、回归×1 保持）。
- `run-local-gates --fast` **10/10**；`verify-docs` 0 issue；`runAll` 对真实模板 scanned=4、issueCount=0。
- 合并 `--no-ff` main：r11 增量 `63a20334`（父 `622719ca` + `b6f06019`）；r12→r19 增量 `19a53722`（父 `50f9e51c` + `1bf8de05`，verify-deploy-templates.mjs +577、test.mjs +293）。
- 零触碰边界：application*.yaml / Java 源码 / HTTP 端点 / run-local-gates.mjs / .github/workflows。

## 登记边界（如实登记）

1. **复评闭环（原「末轮修复未经下一弧复评」已消除）**：r11 处置 `b6f06019` 续经 r12→r18 七弧处置
   （`e07a9dd2`/`1595f2a4`/`0243983f`/`244fc03e`/`f27b8e1d`/`d27a5bb7`/`1bf8de05`），r19 复评 PASS
   （0 findings）确认收敛——不以处置提交自证，以复评结论闭环。
2. **纯静态校验，未跑 Docker**：本批为模板可审查性（.A 批次退出条件），真实业务启动联验归 B02/B03；
   监测告警落地归 .B（前置 ZS-DB-005 + ZS-JOB-004）、备份恢复演练与 RPO/RTO 归 .C（前置
   ZS-FILE-005.B）——不虚报生产可用。
3. **r0 一项 P2 延后**：upstream 端口对齐（ZSZJ_SERVER_PORT 默认 48080 须与 nginx 同步）归 .B/.C
   动态端口渲染增强，已在部署文档注明。
4. r1~r4 裁决原文未随暂存保留：发现与处置按对应提交说明摘录（r0/r5 原始 stdout 已入库本目录，
   r6→r11 裁决 JSON 落盘 outputs/ 供追溯）。
