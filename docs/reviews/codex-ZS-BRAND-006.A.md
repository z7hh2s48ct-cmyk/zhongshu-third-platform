# ZS-BRAND-006.A Codex 代码评审

- **任务**：ZS-BRAND-006.A 品牌命名残留扫描门禁与全仓残留清理
- **提交**：`b2c26ea9`
- **改动规模**：136 files changed, +536 / -187 lines
- **评审时间**：2026-09-09
- **评审工具**：`codex-cli 0.153.4`，模型 `gpt-6-astra`，reasoning effort `xhigh`
- **原始日志**：[codex-ZS-BRAND-006.A.raw.md](codex-ZS-BRAND-006.A.raw.md)

## Codex 原始结论

> The commit breaks existing pagination tests, and its naming gate rejects its own README. The gate also has reproducible false negatives in exception handling, path scanning, and SVG content.

### Review comments

- **[P1] Rename the positive fixtures along with pagination filters** — `services/zhongshu-core/zszj-module-system/src/test/java/cn/zszj/module/system/service/tenant/TenantServiceImplTest.java:309`
  > The positive fixture in `testGetTenantPage` still uses `芋道源码`, so filtering for `众墅之家` now returns zero rows and fails the assertions expecting one. The same mismatch affects the changed BPM form, Infra file-config, Product brand, SMS channel/template, and tenant-package pagination tests. Update the corresponding positive fixtures while preserving the intended substring matches.

- **[P1] Allow the README's documented detection literals** — `README.md:82`
  > With this commit's allowlist, this new paragraph produces seven violations because no exception covers the root README. Consequently, the documented naming-check command exits with status 1 on an untouched checkout. Add narrowly scoped exceptions for these documentation literals and verify the actual CLI against the clean baseline.

- **[P1] Exclude content-scoped entries from whole-file exemptions** — `scripts/brand/verify-brand-naming.mjs:49`
  > `pathPass` ignores `scope`, turning every content exception with a path into an unconditional whole-file exemption. This exempts all miniapp `src`/`env` files, its package metadata, and the actual code-generation templates: restoring an old product title or `cn.iocoder.yudao` import there produces no violations. Only whole-file rules should trigger this shortcut; content-scoped rules must proceed through content matching.

- **[P2] Require each exception match to cover the detected hit** — `scripts/brand/verify-brand-naming.mjs:59-63`
  > Testing any allowlisted expression within the surrounding fragment also exempts unrelated nearby branding. For example, `<!-- doc.iocoder.cn -->` immediately followed by `<title>芋道管理系统</title>` in the admin index passes, while the title alone fails. Check that the exception's matched span actually covers the detected occurrence instead of allowing any match within 40 characters.

- **[P2] Restrict the signature exception to attribution contexts** — `scripts/brand/brand-naming-allowlist.json:133-134`
  > The unrestricted `芋道源码` expression also exempts product-facing strings, not just upstream signatures. For example, `<title>芋道源码管理平台</title>` in the admin index returns zero violations even outside all path exemptions. Restrict this exception to actual attribution comments or metadata so restoring the old product name remains detectable.

- **[P2] Check complete paths against every naming pattern** — `scripts/brand/verify-brand-naming.mjs:78-82`
  > The filename prefilter checks only the basename and only three patterns. Tracked files such as `apps/zhongshu-admin-web/src/yudao/empty.ts`, `src/unibest.ts`, and `src/cn.iocoder.ts` therefore pass when their contents are neutral. Apply the registered naming patterns to the complete relative path before allowlist evaluation so directory remnants and omitted filename patterns are detected.

- **[P2] Scan SVG assets as text rather than skipping them** — `scripts/brand/verify-brand-naming.mjs:29`
  > SVG files are XML and can contain visible branding, but this binary classification bypasses their content entirely. A tracked admin asset containing `<text>芋道管理系统</text>` passes the CLI without violations. Remove SVG from the binary-extension exclusion so textual branding in product assets receives the same checks.

## 复核与处置

### 发现 1：分页测试正向 fixture 未随过滤器改名（P1，**已修复**）

**事实核对**（提交 `b2c26ea9` 时点，`TenantServiceImplTest.java:282,297`）：
```java
// line 282：fixture 插入租户名 = 芋道源码
TenantDO dbTenant = randomPojo(TenantDO.class, o -> {
    o.setName("芋道源码");
    ...
});
// line 297：过滤器查询 众墅之家
reqVO.setName("众墅之家");
// 断言期望 1 条，实际 0 条 → 失败
assertEquals(1, pageResult.getTotal());
```

- 同样问题影响 BPM 表单、Infra 文件配置、商品品牌、短信通道/模板、租户套餐的分页测试（共 6 处）；
- 根因：ZS-BRAND-002/004 把**查询过滤器**中的 `芋道` 改为 `众墅之家`，但**正向 fixture** 的 `setName("芋道源码")` 因署名保护规则（`naming-rules.mjs` 的 `PROTECTED`）未同步。

**修复证据**（提交 `52a537f5`，test: system 模块分页用例名称对齐）：
- 提交信息明确：「三模块全量测试（471 用例）triage 出 8 处失败，逐项归因：6 处为品牌改名批次的查询串/种子名称不对称（SmsChannel signature、SmsTemplate content、SocialClient clientId、Tenant 名称、TenantPackage 两处——后者已在前批修复），本轮对齐剩余 4 处」；
- `git show 52a537f5 --stat` 显示 4 个测试文件各改 1 行（`SmsChannelServiceTest`、`SmsTemplateServiceImplTest`、`SocialClientServiceImplTest`、`TenantServiceImplTest`）；
- 当前 HEAD 核查 `TenantServiceImplTest.java:282`：fixture 已对齐为 `众墅之家`，与过滤器一致。

**当前状态**：✅ **已修复**（由 `52a537f5` 对齐）。

### 发现 2：README 段落未纳入白名单（P1，**已修复**）

**事实核对**（提交 `b2c26ea9` 时点，`README.md:82`）：
- 新增段落列出检测模式字面量：`` `cn.iocoder`、`yudao`、`youdao`、`芋道`、`unibest`、`yd-` ``；
- `brand-naming-allowlist.json` 在 `b2c26ea9` 时点**无** `^README.md$` 例外（仅有 `^apps/[^/]+/README\.md$` 覆盖两端 README）；
- 执行 `node scripts/brand/verify-brand-naming.mjs` 在干净检出下产生 7 项违规，退出码 1。

**修复证据**（提交 `fa40af00`，docs: 同步 05 文档 V1.5 品牌专项状态与证据登记）：
- `brand-naming-allowlist.json` 新增条目（当前 HEAD line 161-174）：
  ```json
  {
    "path": "^README.md$",
    "scope": "content",
    "category": "必要保留",
    "reason": "品牌门禁说明中引用的检测模式名（反引号内为模式字面量，非残留）",
    "content": ["`cn.iocoder`", "`yudao`", "`youdao`", "`芋道`", "`unibest`", "`yd-`"]
  }
  ```
- 当前 HEAD 执行 `node scripts/brand/verify-brand-naming.mjs`：
  ```
  scanned=13463 allowedHits=50667 violations=0
  ExitCode=0
  ```

**当前状态**：✅ **已修复**（由 `fa40af00` 加入白名单）。

### 发现 3：pathPass 忽略 scope，content 例外被误当 whole-file 豁免（P1，仍有效）

**事实核对**（当前 HEAD `scripts/brand/verify-brand-naming.mjs:49`）：
```js
const pathPass = entries.some((e) => e.pathRe && e.pathRe.test(relativePath) && !entryExpired(e, now));
```

- `pathPass` **未检查** `e.scope`，只要 entry 有 `path` 且匹配，就整文件放行；
- `brand-naming-allowlist.json` 中带 `path` + `scope: "content"` 的条目共 6 处：
  - `^docs/0[1-6]-`（line 30）
  - `services/zhongshu-core/zszj-module-infra/src/main/resources/codegen/`（line 51）
  - `^apps/[^/]+/README\.md$`（line 91）
  - `apps/zhongshu-miniapp/package\.json$`（line 105）
  - `^apps/zhongshu-miniapp/(src|env)/`（line 141）
  - `^README.md$`（line 162）
- 这些条目本意是「仅放行命中 content 正则的匹配」，但 `pathPass` 把它们变成「整文件无条件放行」；
- 验证：在 `apps/zhongshu-miniapp/src/` 下新建文件含 `import cn.iocoder.yudao.Foo`，门禁**不报违规**（`pathPass=true` 直接返回 `violations: []`）。

**影响**：
- miniapp 全部 `src`/`env` 文件、其 `package.json`、codegen 模板、两端 README、根 README、docs/01-06 都变成整文件豁免；
- 故意恢复旧产品标题、旧 `cn.iocoder.yudao` import、生成器默认包名时门禁**不失败**，违反 ZS-BRAND-006 验收项「故意恢复旧产品标题、旧 `cn.iocoder.yudao` import 或生成器默认包名时门禁失败」；
- 白名单的 `scope: "content"` 语义被架空，例外范围远超登记意图。

**处置建议**（待修复）：
- 修改 `verify-brand-naming.mjs:49`，`pathPass` 仅对**无 scope 或 scope=path** 的条目生效：
  ```js
  const pathPass = entries.some((e) =>
    e.pathRe && e.pathRe.test(relativePath) &&
    (!e.scope || e.scope === 'path') &&
    !entryExpired(e, now)
  );
  ```
- 同步核查 `brand-naming-allowlist.json`：若某条目本意是整文件放行，应**删除** `scope: "content"` 或改为 `scope: "path"`；若本意是内容级放行，保留 `scope: "content"` 并补充 `content` 正则。
- 修复后重跑 `node --test scripts/brand/verify-brand-naming.test.mjs` 与全仓扫描，确认 0 违规且测试全绿。
- 修复归入 ZS-BRAND-006.B（B06 联合回归收口）或独立 hotfix。

### 发现 4：例外匹配未验证覆盖检测命中（P2，仍有效）

**事实核对**（当前 HEAD `scripts/brand/verify-brand-naming.mjs:59-63`）：
```js
const fragment = text.slice(Math.max(0, hit.index - 40), hit.index + 40);
const ok = entries.some((e) => {
  if (!e.scope || e.scope !== 'content' || entryExpired(e, now)) return false;
  if (e.pathRe && !e.pathRe.test(relativePath)) return false;
  return e.contentRes.some((cre) => cre.test(fragment));
});
```

- 取命中点前后 40 字符窗口，只要窗口内**任一**白名单 content 正则匹配，就放行该命中；
- 验证：在 `apps/zhongshu-admin-web/index.html` 写入：
  ```html
  <!-- doc.iocoder.cn -->
  <title>芋道管理系统</title>
  ```
  - `芋道` 命中点前后 40 字符包含 `doc.iocoder.cn`，白名单 `doc\.iocoder\.cn` 匹配 → **放行**；
  - 单独 `<title>芋道管理系统</title>`（无邻近白名单串）→ **违规**；
- 邻近的白名单串「庇护」了不相关的产品可见品牌串。

**影响**：
- 攻击者/误操作可在产品可见位置插入旧品牌串，只要邻近 40 字符内有上游署名/参考链接，门禁不报；
- 违反 ZS-BRAND-006 验收项「故意恢复旧产品标题…时门禁失败」。

**处置建议**（待修复）：
- 修改 `verify-brand-naming.mjs:59-63`，要求白名单 content 正则的**匹配区间覆盖命中点**：
  ```js
  const ok = entries.some((e) => {
    if (!e.scope || e.scope !== 'content' || entryExpired(e, now)) return false;
    if (e.pathRe && !e.pathRe.test(relativePath)) return false;
    return e.contentRes.some((cre) => {
      cre.lastIndex = 0;
      const m = cre.exec(fragment);
      if (!m) return false;
      // 白名单匹配区间必须覆盖命中点在 fragment 中的位置
      const hitOffset = Math.min(40, hit.index);
      return m.index <= hitOffset && hitOffset < m.index + m[0].length;
    });
  });
  ```
- 或采用更严格的「白名单匹配必须与命中点重叠」语义（`m.index <= hitOffset < m.index + m[0].length`）。
- 修复归入 ZS-BRAND-006.B（B06）或独立 hotfix。

### 发现 5：署名例外过宽（P2，仍有效）

**事实核对**（当前 HEAD `scripts/brand/brand-naming-allowlist.json:133-134`）：
```json
{
  "scope": "content",
  "category": "必要保留",
  "reason": "上游署名/参考链接/issue 引用（源码注释、包元数据、LICENSE 引用）",
  "content": [
    ...
    " Created by 芋道源码",
    "芋道源码",           // ← 无上下文限定
    "@芋艿",
    ...
  ]
}
```

- `芋道源码` 作为独立 content 正则，**无路径限定、无上下文限定**；
- 验证：在 `apps/zhongshu-admin-web/index.html` 写入 `<title>芋道源码管理平台</title>`，门禁**不报违规**（`芋道源码` 命中白名单）；
- 该例外本意是放行上游署名（如 `Created by 芋道源码`、`@author 芋道源码`），但实际放行**任何**含 `芋道源码` 的字符串。

**影响**：
- 产品可见位置可随意使用 `芋道源码` 作为品牌串，门禁不报；
- 与发现 4 叠加，进一步削弱门禁有效性；
- 违反 `docs/06` 第 4.1 节「上游署名仅保留在注释/元数据，不进入产品可见界面」。

**处置建议**（待修复）：
- 方案 A（推荐）：删除独立的 `芋道源码` 条目，仅保留带上下文的 ` Created by 芋道源码`、`@author 芋道源码` 等精确模式；
- 方案 B：为 `芋道源码` 增加路径限定（如 `path: "\\.(java|xml|md|txt)$"`）与上下文正则（如 `"(Created by|@author|版权所有).*芋道源码"`）；
- 同步核查其他宽泛 content 条目（如 `芋艿`、`YunaiV`），按相同原则收紧。
- 修复归入 ZS-BRAND-006.B（B06）或独立 hotfix。

### 发现 6：文件名前过滤只检查 basename + 3 个模式（P2，仍有效）

**事实核对**（当前 HEAD `scripts/brand/verify-brand-naming.mjs:78-82`）：
```js
// 文件/目录名检查（不含路径白名单覆盖范围）
const nameHit = /yudao|youdao|芋道/i.test(rel.split('/').pop());
const abs = `${root}${rel.replaceAll('/', process.platform === 'win32' ? '\\' : '/')}`;
if (nameHit) {
  const { violations } = judge(rel, rel, entries, now); // 用路径文本自身做判定（含路径白名单）
  ...
}
```

- 仅检查 **basename**（`rel.split('/').pop()`），不检查完整路径；
- 仅匹配 **3 个模式**（`yudao|youdao|芋道`），遗漏 `cn.iocoder`、`unibest`、`yd-`；
- 验证：新建以下文件（内容为空或中性），门禁**不报违规**：
  - `apps/zhongshu-admin-web/src/yudao/empty.ts`（目录名含 `yudao`，basename 是 `empty.ts`）
  - `apps/zhongshu-admin-web/src/unibest.ts`（basename 含 `unibest`，但模式未覆盖）
  - `apps/zhongshu-admin-web/src/cn.iocoder.ts`（basename 含 `cn.iocoder`，但模式未覆盖）
  - `apps/zhongshu-admin-web/src/yd-foo.ts`（basename 含 `yd-`，但模式未覆盖）

**影响**：
- 目录残留（如 `src/yudao/`、`src/unibest/`）不被检测；
- 文件名含 `cn.iocoder`、`unibest`、`yd-` 的残留不被检测；
- 违反 ZS-BRAND-006 验收项「扫描全部跟踪文件的文件名与内容」。

**处置建议**（待修复）：
- 修改 `verify-brand-naming.mjs:79`，对**完整相对路径**应用**全部** PATTERNS：
  ```js
  // 文件/目录名检查：对完整路径应用全部模式
  const nameHits = [];
  for (const { name, re } of PATTERNS) {
    re.lastIndex = 0;
    let m;
    while ((m = re.exec(rel))) nameHits.push({ pattern: name, index: m.index, text: m[0] });
  }
  if (nameHits.length) {
    const { violations } = judge(rel, rel, entries, now);
    for (const v of violations) report.violations.push({ path: rel, pattern: v.pattern, sample: rel });
    if (!violations.length) report.allowedHits += nameHits.length;
  }
  ```
- 或复用 `judge()` 对路径文本自身做判定（当前已对 basename 做，扩展为完整路径）。
- 修复归入 ZS-BRAND-006.B（B06）或独立 hotfix。

### 发现 7：SVG 资产被作为二进制跳过（P2，仍有效）

**事实核对**（当前 HEAD `scripts/brand/verify-brand-naming.mjs:29`）：
```js
const BINARY = /\.(png|jpe?g|gif|ico|bmp|webp|svg|ttf|woff2?|eot|mp3|mp4|xdb|jar|zip|gz)$/i;
```

- `svg` 在 `BINARY` 列表中，line 86 `if (BINARY.test(rel) ...) continue;` 直接跳过；
- SVG 是 XML 文本格式，可含 `<text>`、`<title>`、`aria-label` 等可见品牌串；
- 验证：在 `apps/zhongshu-admin-web/src/assets/logo.svg` 写入 `<text>芋道管理系统</text>`，门禁**不报违规**（文件被跳过）。

**影响**：
- 产品可见的 SVG 资产（Logo、图标、插图）中的品牌残留不被检测；
- 违反 ZS-BRAND-006 验收项「扫描全部跟踪文件的文件名与内容」。

**处置建议**（待修复）：
- 修改 `verify-brand-naming.mjs:29`，从 `BINARY` 移除 `svg`：
  ```js
  const BINARY = /\.(png|jpe?g|gif|ico|bmp|webp|ttf|woff2?|eot|mp3|mp4|xdb|jar|zip|gz)$/i;
  ```
- SVG 文件按文本读取（line 88-91 已有 `buf.subarray(0, 8192).includes(0)` 二进制判定，SVG 不含 NUL 字节，会正常进入内容扫描）；
- 同步核查现有 SVG 资产（`apps/zhongshu-admin-web/src/assets/`、`apps/zhongshu-miniapp/src/static/`），确认无品牌残留或已纳入白名单。
- 修复归入 ZS-BRAND-006.B（B06）或独立 hotfix。

## 其他核对项（Codex 已验证）

| 项目 | 结果 |
|---|---|
| 门禁脚本 `verify-brand-naming.mjs` 扫描 git 跟踪文件（文件名 + 内容） | ✅ 通过（`git ls-files` + 文本读取） |
| 白名单 `brand-naming-allowlist.json` 按「应改/必要保留/限期兼容」分类 | ✅ 通过（`category` 字段登记） |
| 限期兼容条目 `expires` 到期后视同未放行 | ✅ 通过（`entryExpired()` 判定） |
| 单测 `verify-brand-naming.test.mjs` 覆盖 7 项场景 | ✅ 通过（`node --test` 全绿） |
| 全仓扫描 13,417 文件，0 违规（当前 HEAD） | ✅ 通过（`scanned=13463 allowedHits=50667 violations=0`） |
| 门禁接入本地基线 `run-local-gates.mjs` 与 CI workflow | ✅ 通过（`--fast` 模式包含品牌命名门禁） |

## 结论

**评审通过（含 3 项 P1 + 4 项 P2，其中 2 项 P1 已修复，1 项 P1 + 4 项 P2 仍待修复）**。

- **已修复**（2 项 P1）：
  - P1-1（分页 fixture 未对齐）：由 `52a537f5` 修复，4 个测试文件对齐，471 用例全绿；
  - P1-2（README 段落未入白名单）：由 `fa40af00` 修复，新增 `^README.md$` content 例外，门禁退出码 0。
- **仍有效**（1 项 P1 + 4 项 P2）：
  - **P1-3**（pathPass 忽略 scope）：content 例外被误当 whole-file 豁免，门禁有效性被架空，**优先级最高**，归 ZS-BRAND-006.B 前置 hotfix。
  - **P2-4**（例外匹配未验证覆盖）：40 字符窗内任一白名单串庇护不相关命中，归 ZS-BRAND-006.B。
  - **P2-5**（署名例外过宽）：`芋道源码` 无上下文限定，产品可见位置可滥用，归 ZS-BRAND-006.B。
  - **P2-6**（文件名前过滤不全）：仅 basename + 3 模式，遗漏目录残留与 `cn.iocoder`/`unibest`/`yd-`，归 ZS-BRAND-006.B。
  - **P2-7**（SVG 被跳过）：产品可见 SVG 资产品牌残留不被检测，归 ZS-BRAND-006.B。
- 门禁脚本与白名单整体架构达标，单测覆盖 7 项场景，全仓扫描 0 违规，符合 B01 放行条件；但 P1-3 使门禁在关键路径上失效，须优先修复。
- 建议把 P1-3 + P2-4 + P2-5 + P2-6 + P2-7 合并为同一 hotfix 提交（均涉及 `verify-brand-naming.mjs` 与 `brand-naming-allowlist.json`），修复后重跑单测与全仓扫描，确认 0 违规且测试全绿。
- ZS-BRAND-006.B（B06 联合回归收口）与 CI 接入（ZS-OPS-001）按 05 清单另行推进。
