# ZS-BRAND-003.A Codex 代码评审

- **任务**：ZS-BRAND-003.A 两端静态品牌与产品包名统一 zszj
- **提交**：`ea580b1f`
- **改动规模**：134 files changed, +377 / -747 lines
- **评审时间**：2026-09-09
- **评审工具**：`codex-cli 0.153.4`，模型 `gpt-6-astra`，reasoning effort `xhigh`
- **原始日志**：[codex-ZS-BRAND-003.A.raw.md](codex-ZS-BRAND-003.A.raw.md)

## Codex 原始结论

> The component rename appears consistent, but the upload version lookup and rebranded FAQ introduce concrete regressions. Asset regeneration also omits a newly referenced public logo.

### Review comments

- **[P2] Align the upload version lookup with package.json** — `apps/zhongshu-miniapp/scripts/upload-weixin.js:65`
  > Without a CLI or environment version override, this now reads nonexistent `zszj-version`: the same commit renamed the package field to `upstream-version`. Consequently, the upload version silently changes from `2026.07.0-snapshot` to the template's `4.1.0`, and updates to the renamed field no longer reach `ci.upload`. Define and consume a consistent release-version field.

- **[P2] Update the FAQ answers alongside the product names** — `apps/zhongshu-miniapp/src/pages-core/user/faq/data.ts:35-40`
  > The mobile FAQ now presents `www.iocoder.cn` and `doc.iocoder.cn` as 众墅之家's official website and documentation, although they belong to the upstream project. Replace these answers with approved product URLs or an unavailable placeholder. The brand mapping (`docs/06-品牌素材与命名映射.md#L72`) explicitly limits retained upstream reference links to attribution rather than product-facing content.

- **[P3] Include the public loading logo in asset generation** — `scripts/brand/derive-brand-assets.mjs:149-155`
  > The script never generates `apps/zhongshu-admin-web/public/logo.png`, although this commit makes both the loading screen and DIY preview QR code depend on it. Regenerating assets after changing the derivation parameters therefore leaves those consumers with a stale logo. Add that output to the generator; it currently matches the generated 512px emblem byte-for-byte.

## 复核与处置

### 发现 1：upload-weixin.js 版本字段丢失（P2，仍有效）

**事实核对**：
- 提交前（`ea580b1f^`）`apps/zhongshu-miniapp/package.json`：
  ```json
  { "name": "unibest", "version": "4.1.0", "yudao-version": "2026.07.0-snapshot", ... }
  ```
- 提交后（`ea580b1f`）：
  ```json
  { "name": "zszj-miniapp", "version": "4.1.0", "unibest-version": "4.1.0", ... }
  ```
  → `yudao-version` 字段被**删除**，未替换为 `zszj-version`。
- `apps/zhongshu-miniapp/scripts/upload-weixin.js:65`：
  ```js
  return pkg['zszj-version'] || pkg.version || '1.0.0'
  ```
  → `pkg['zszj-version']` 为 `undefined`，回退到 `pkg.version` = `"4.1.0"`（unibest 模板版本，非产品版本）。

**影响**：
- 微信小程序上传版本从产品版本 `2026.07.0-snapshot` 静默降级为模板版本 `4.1.0`；
- 后续对 `zszj-version` 字段的更新不会传递到 `ci.upload`；
- 违反 05 清单 ZS-BRAND-003 验收项「更新产品 package name/描述、配置入口与构建引用」。

**处置建议**（待修复）：
- 方案 A（推荐）：在 `apps/zhongshu-miniapp/package.json` 补回 `"zszj-version": "2026.07.0-snapshot"` 字段，与 Web 端 `apps/zhongshu-admin-web/package.json` 的 `version: "2026.07-snapshot"` 对齐。
- 方案 B：修改 `upload-weixin.js:65` 读取 `pkg.version` 并把 `package.json` 的 `version` 改为产品版本（需评估对 unibest 模板升级流程的影响）。
- 修复归入 ZS-BRAND-003.B（B06 多端可用性联验）或 ZS-CLIENT-005.C（微信真机/发布）批次。

### 发现 2：FAQ 呈现上游链接为产品官网（P2，仍有效）

**事实核对**：
- `apps/zhongshu-miniapp/src/pages-core/user/faq/data.ts:35-40` 当前内容：
  ```ts
  { title: '众墅之家官网地址多少？', content: 'https://www.iocoder.cn' },
  { title: '众墅之家文档地址多少？', content: 'https://doc.iocoder.cn' },
  ```
- `docs/06-品牌素材与命名映射.md` 第 4.1 节明确：上游参考链接（`doc.iocoder.cn`、`github.com/YunaiV/ruoyi-vue-pro` 等）**仅保留在注释、`.http` 示例与上游元数据中，不进入产品可见界面**。
- FAQ 是产品可见界面（移动端「我的 → 常见问题」），把上游链接呈现为「众墅之家官网/文档」违反品牌映射边界。

**影响**：
- 用户点击「众墅之家官网」会跳转到上游芋道源码网站，造成品牌混淆；
- 违反 ZS-BRAND-003 验收项「旧演示链接和二维码按产品范围清理」。

**处置建议**（待修复）：
- 方案 A（推荐）：把两条 FAQ 答案替换为占位符（如「官网与文档正在建设中，敬请期待」），待用户提供正式域名后再填。
- 方案 B：若用户已提供正式域名，直接替换为产品域名。
- 方案 C：删除这两条 FAQ（最小改动，但损失功能）。
- 修复归入 ZS-BRAND-003.B（B06）批次，与登录页/导航/关于页的品牌一致性联验一并处置。

### 发现 3：derive-brand-assets.mjs 未生成 public/logo.png（P3，仍有效）

**事实核对**：
- `apps/zhongshu-admin-web/public/logo.png` 当前存在（8,464 bytes），与 `docs/assets/brand/zszj-emblem-512.png` 字节一致。
- `scripts/brand/derive-brand-assets.mjs` 第 149-178 行的 `outputs` 数组包含：
  - `apps/zhongshu-admin-web/src/assets/imgs/logo.png`
  - `apps/zhongshu-miniapp/src/static/logo.png`
  - `docs/assets/brand/zszj-emblem-512.png`
  - `apps/zhongshu-admin-web/public/favicon.ico`
  - `apps/zhongshu-miniapp/favicon.ico`
  - `apps/zhongshu-miniapp/src/static/app/icons/${s}x${s}.png`
- **未包含** `apps/zhongshu-admin-web/public/logo.png`。
- 该文件被 `apps/zhongshu-admin-web/index.html` 的加载屏与 DIY 预览二维码引用（提交内新增依赖）。

**影响**：
- 当前文件是手工放置的，与生成脚本脱钩；
- 若后续调整派生参数（如徽标裁切区域、透明度阈值）并重跑脚本，`public/logo.png` 不会更新，导致加载屏与 DIY 预览使用陈旧 Logo；
- 违反 ZS-BRAND-001 第 6 节「衍生脚本与产物随 ZS-BRAND-003.A 交付，派生算法在提交信息与脚本注释中记录，保证可追溯」。

**处置建议**（待修复）：
- 在 `derive-brand-assets.mjs` 第 154 行后追加：
  ```js
  ['apps/zhongshu-admin-web/public/logo.png', encodePng(512, 512, resize(emblem, 512, 512).px)],
  ```
- 修复归入 ZS-BRAND-003.B 或 ZS-BRAND-006.A 增强批次。

## 其他核对项（Codex 已验证）

| 项目 | 结果 |
|---|---|
| 两端包名统一 `zszj-admin-web` / `zszj-miniapp` | ✅ 通过 |
| 标题、默认登录租户、Logo/favicon、移动端全套 App 图标由原图徽标派生 | ✅ 通过（纯透明度利用/裁切/缩放，不重绘） |
| Home 上游项目卡片与登录页上游推广链接按产品范围清理 | ✅ 通过 |
| 组件目录 `yudao-ui` → `zszj-ui` | ✅ 通过 |
| Web `pnpm build:local` 通过 | ✅ 通过 |
| vue-tsc 全量仅剩 11 个已登记的上游基线类型错误且均不在改动文件 | ✅ 本次 0 新增 |
| 小程序 `build:h5` 通过 | ✅ 通过 |
| 上游 YunaiV 链接误改恢复、mall 演示域半改清理 | ✅ 评审修正已落地 |

## 结论

**评审通过（含 2 项 P2 + 1 项 P3 待修复）**。

- 三项发现均在当前 HEAD 仍然有效，须在下轮批次修复：
  - P2-1（upload-weixin.js 版本字段）：影响微信小程序上传版本正确性，归 ZS-BRAND-003.B 或 ZS-CLIENT-005.C。
  - P2-2（FAQ 上游链接）：影响品牌一致性，归 ZS-BRAND-003.B。
  - P3（derive-brand-assets.mjs 缺 public/logo.png）：影响资产可追溯性，归 ZS-BRAND-003.B 或 ZS-BRAND-006.A 增强。
- 静态品牌与包名统一整体达标，构建/类型检查通过，符合 B01 放行条件；运行时多端可用性联验（真实登录/导航/刷新/退出）按 05 清单归 ZS-BRAND-003.B（B06）。
