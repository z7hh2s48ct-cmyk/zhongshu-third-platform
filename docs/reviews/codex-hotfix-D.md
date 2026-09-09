# Codex 评审：hotfix-D — ZS-BRAND-003.A 上传版本字段 / FAQ 上游链接 / logo 生成三项修复

## 评审元信息

| 项目 | 值 |
|---|---|
| 评审对象 | `4c679e18`（r0，单轮） |
| 评审工具 | codex-cli 0.153.4, model gpt-6-astra, reasoning effort xhigh |
| 评审轮次 | 1 轮 codex 评审（r0 直接 0 发现，无需迭代修正） |
| 关联评审 | [codex-ZS-BRAND-003.A.md](codex-ZS-BRAND-003.A.md) |
| 修改文件 | 4 files, +11/-2 |
| 原始日志 | [codex-hotfix-D.raw.md](codex-hotfix-D.raw.md) |
| 最终状态 | ✅ 通过（r0 codex 0 发现；命名门禁 violations=0；G2b 单测 9/9；命名门禁单测 15/15；本地门禁 11/11；codex 自验 23 项派生资产逐字节匹配已提交文件、默认上传版本解析为 `2026.07.0-snapshot`） |

## 修复的原始缺陷（`ea580b1f` 遗留 → `4c679e18` 修复）

本 hotfix 修复 [codex-ZS-BRAND-003.A.md](codex-ZS-BRAND-003.A.md) 结论中「仍有效」的 3 项发现（2 × P2 + 1 × P3），
三项同属 ZS-BRAND-003.A 两端静态品牌与产品包名统一的收尾缺口，评审建议合并为同一 hotfix：

| 内部编号 | 优先级 | 原始缺陷 | 修复方式 |
|---|---|---|---|
| P2-1 | P2 | ZS-BRAND-003.A 命名迁移把消费端 `upload-weixin.js:65` 自动改为读 `pkg['zszj-version']`，但 `package.json` 未定义该字段（原 `yudao-version` 被**刻意**改名为 `upstream-version` 作上游溯源元数据，未走通用 `yudao-`→`zszj-` 规则），致微信上传版本静默回退到脚手架模板版本 `4.1.0` | `package.json` 补定义 `"zszj-version": "2026.07.0-snapshot"`（产品发布版本），使「定义—消费」一致；`upstream-version` 按 docs/06 映射表第 9 行「上游版本字段保留」原样保留，消费端补注释说明版本字段模型（`version`＝模板版本、`upstream-version`＝上游溯源） |
| P2-2 | P2 | FAQ「众墅之家官网/文档」两条答案直接呈现上游官网与文档域名，属产品可见界面引用上游链接，违反 docs/06 第 4.1 节（上游参考链接仅保留于注释 / `.http` 示例 / 上游元数据） | 两条答案改为占位文案「官网/文档正在建设中，敬请期待」，待正式域名就绪再填；补注释记录品牌边界依据 |
| P3 | P3 | `derive-brand-assets.mjs` 的 `outputs` 未含 `apps/zhongshu-admin-web/public/logo.png`，而该文件被 `index.html` 加载屏与 `DiyEditor` 预览二维码以 `/logo.png` 引用；派生参数调整后重跑脚本会残留陈旧 Logo | `outputs` 追加 `public/logo.png`（与 512 徽标同源同尺寸），纳入生成保证派生可追溯 |

### 编号对照说明

| 缺陷 | ZS-BRAND-003.A 评审文档内编号（本文与代码注释采用） | [README.md](README.md) 汇总表内编号 |
|---|---|---|
| upload-weixin 版本字段 | P2-1 | 本表 P2 #2 |
| FAQ 上游链接 | P2-2 | 本表 P2 #3 |
| logo.png 未纳入生成 | P3 | 本表 P3 #1 |

代码注释一律引用**来源评审文档**的编号（左列）；README 内一律写作「本表 P2 #n」。

## Codex 评审迭代过程

### 第 1 轮（`4c679e18`，r0）— ✅ 通过（0 发现）

codex 对 r0 提交直接给出 0 发现结论，并在评审过程中**自主执行**了下列验证（见原始日志）：

- **P2-1**：`PASS: default upload version resolves to 2026.07.0-snapshot`——确认补定义 `zszj-version` 后
  `readPackageVersion()` 解析出产品版本，不再回退脚手架模板版本 `4.1.0`。
- **P3**：`PASS: 23 generated assets match tracked files without writing`——以校验模式重跑派生脚本，
  确认含新增 `public/logo.png` 在内的全部 23 项产物与已提交文件逐字节一致（修复前为 22 项）。
- **命名门禁**：15 项单测全通过、全仓扫描 `violations=0`；语法检查通过。

codex 复审结论：

> "No actionable regressions were found. The upload version resolves correctly, generated assets match committed files, and syntax checks plus all 15 brand-naming tests passed."

原始日志 `[P0]/[P1]/[P2]/[P3]` 标记数：**0**（单轮通过）。

## 验证证据

| 验证项 | 方法 | 结果 |
|---|---|---|
| 命名门禁单测 | `node --test scripts/brand/verify-brand-naming.test.mjs` | **15/15 通过** |
| G2b 迁移工具单测 | `node --test scripts/brand/apply-naming-migration.test.mjs` | **9/9 通过** |
| 命名门禁全仓扫描 | `node scripts/brand/verify-brand-naming.mjs` | **violations=0**（scanned=13542） |
| 本地基线门禁 | `node scripts/ops/run-local-gates.mjs` | **11/11 PASS**（含 G3 全仓扫描 0 违规） |
| 派生资产确定性 | `node scripts/brand/derive-brand-assets.mjs` 重跑 | 23 项产物字节一致，`git status` 无二进制 diff（`public/logo.png`＝8464B，与 `zszj-emblem-512.png` 一致） |
| codex r0 复审 | `codex review --commit 4c679e18` | 0 发现（"No actionable regressions were found"） |

## 复核与处置

| 项 | 状态 | 说明 |
|---|---|---|
| P2-1 upload-weixin 版本字段 | ✅ 已修复 | `4c679e18`；补定义 `zszj-version=2026.07.0-snapshot`，codex 自验默认上传版本解析正确 |
| P2-2 FAQ 上游链接 | ✅ 已修复 | `4c679e18`；两条答案改占位文案，产品可见界面无上游域名 |
| P3 logo.png 未纳入生成 | ✅ 已修复 | `4c679e18`；`outputs` 追加 `public/logo.png`，重跑字节一致（22→23 项产物） |
| 微信小程序真机上传联验 | ⏳ 待条件 | 需微信上传私钥与 AppID；归 ZS-CLIENT-005.C 真机 / 发布批次 |

## 经验登记

1. **命名迁移的「刻意保留」必须同步到全部消费端**：P2-1 根因是 `yudao-version` 在 `package.json` 被**人工刻意**
   改名为 `upstream-version`（上游溯源，覆盖通用 `yudao-`→`zszj-` 规则），而消费端 `upload-weixin.js` 被**通用规则
   自动**改成读 `zszj-version`——两端各自演化出不同字段名，中间无人对账。凡对某处施加「例外 / 保留」而偏离通用
   规则时，必须同步核查该标识的**全部读取点**，否则定义与消费静默失配。
2. **静默回退是版本类缺陷的放大器**：`pkg['zszj-version'] || pkg.version || '1.0.0'` 的三级回退在读不到字段时
   不报错、直接降级到模板版本 `4.1.0`，缺陷被完全掩盖到发布环节才暴露。消费「必须存在」的口径字段时，回退链
   要么去掉、要么在回退时告警，不能让错误值静默通过。
3. **产品可见界面是品牌边界的硬约束面**：FAQ 答案经 `{{ content }}` 纯文本渲染，把上游域名呈现为产品官网 / 文档，
   即便不可点击也构成品牌混淆。docs/06 第 4.1 节允许上游链接存于**注释 / `.http` / 上游元数据**，但产品可见界面
   不在豁免范围；无正式域名时用占位文案，而非就近沿用上游链接。
4. **被引用的派生产物必须纳入生成器**：`public/logo.png` 被加载屏与二维码引用却是手工放置、与派生脚本脱钩，
   调参重跑后必然陈旧。凡「由源图派生 + 被代码引用」的产物都应登记进生成器 `outputs`，并以「重跑字节一致」验证
   其与已提交产物同源（本次 22→23 项、`git status` 零二进制 diff 即为证据）。
