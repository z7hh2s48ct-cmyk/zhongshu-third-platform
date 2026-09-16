# ZS-BRAND-003.B codex 评审处置（r0→r1 两弧）

- 评审工具：codex（gpt-6-astra / xhigh，`exec -s read-only` 读评工作区）
- 评审对象：分支 `feat/brand-003-b`（ZS-BRAND-003.B 登录页品牌残留清理，B06 多端可用性联验；范围经用户裁定仅 Tier 1 文案，不动 SVG 背景〔Tier 2〕/ 二维码演示登录〔Tier 3〕）
- 结论：**r1 PASS / 0×P0/P1/P2**（两弧收敛：r0 PASS 0×P0/P1/P2 + 2 建议性 P3 → r1 PASS 0×P0/P1/P2 + 1 建议性 P3；所有 P3 均已修）
- 说明：r0 / r1 两弧 raw 输出保留于开发窗 `outputs/brand-003b-codex-r{0,1}.log`（未入库，outputs/ 已 gitignore），各弧裁定行与发现已逐条摘录如下，如实登记。

## 交付内容（impl commit `96778cf3`，4 files +171/-6；`--no-ff` 合并 main `d00692a7`，merge-base `7c75f04b`）

1. **登录页品牌标语（`apps/zhongshu-admin-web/src/locales/{zh-CN,en}.ts`）**：`login.message` 由上游旧标语
   （zh「开箱即用的中后台管理系统」/ en「Backstage management system」）改为众墅品牌标语——zh「众墅之家 AI 赋能平台」
   （与 `VITE_APP_TITLE` 一致）/ en「Zhongshu Zhijia AI Enablement Platform」（按已注册 `zszj` 主前缀拼音派生的**临时**
   展示名，非臆造官方英文名）。两端对称修改，各 +1/-3。
2. **死键清除**：删除 `signInTitle` / `signInDesc`（vben 遗留、全仓无 `t()` 引用、承载旧标语），两端对称删除以维持键集一致。
3. **源码契约测试 `apps/zhongshu-admin-web/tests/unit/login-brand-residue.spec.ts`（新增 164 行 / 10 用例）**：
   - 负向残留扫描：`zh-CN.ts` / `en.ts` 全文无旧品牌残留短语（`RESIDUE_PHRASES`）。
   - 正向品牌标语冻结：`login.message` 冻结为上述品牌标语（zh/en）。
   - 死键清除断言：`signInTitle` / `signInDesc` 从两端移除。
   - 可见性守卫：`Login.vue` / `SocialLogin.vue` 确引用 `t('login.message')`。
   - 三重变异自检：`findResidue` / `hasKey` / `blockValue` 检查器有效性（含 r1 补的 `blockValue` 左边界变异断言——
     `old_message` 不得被误取为 `message`）。
4. **G3 品牌命名白名单配套（`scripts/brand/brand-naming-allowlist.json` +5 行）**：新 spec 的 `RESIDUE_PHRASES` 负向检测
   模式与说明注释字面引用上游旧品牌标识（芋道 / yudao），被 G3 品牌命名全仓扫描判为违规；循项目治理惯例
   （`^scripts/brand/` 检查器自身含匹配串即整目录放行），为该**检测测试**新增 `path` 白名单条目（`category=必要保留`），
   非字符串拼接技巧规避门禁。

## 评审弧

| 轮次 | 结论 | 发现 → 处置 |
|---|---|---|
| r0 | **PASS 0×P0/P1/P2**；2 建议性 P3 | 六项核查全 PASS（残留清除彻底性 / 死键删除安全性 / 两端键集对称 / 可见性守卫 / 文案一致性 / 其他 Tier1 文案）。**P3-1** spec 注释对 docs/06 引用不准确 → 订正为 §2（第 26 行）登记前缀 `zszj`、§1（第 21 行）官方英文名待用户提供，明确 en 串为临时拼音派生占位、非长期契约；**P3-2** `blockValue` 正则无左边界（理论上可误命中 `old_message` 等子串）→ 加左边界 `(^|[\s{,])` 与 `m` 标志，捕获组调整为 `m[2]`。修后 spec 9/9、prettier、eslint exit 0 复验全绿 |
| r1（P3-1/P3-2 处置 + G3 白名单条目） | **PASS 0×P0/P1/P2**；1 建议性 P3 | 白名单 `path` 方案、`path` 正则转义、`category`/`reason` 合规、`blockValue` 左边界 + `m[2]` 均 PASS；codex 认可白名单方案优于字符串拼接规避（符治理哲学）、优于 `scope=content`（`judge()` 仅约 40 字符上下文无法可靠锤定多行 `RESIDUE_PHRASES`，反更脆弱）。**P3-1（复发·仍偏差）** 注释仍称「§2 登记 zszj=ZhongShu ZhiJia 展开」「§1 官方英文名待提供」，但 docs/06 §2（第 26 行）仅登记 `zszj`/`Zszj`/`ZSZJ_`（未展开拼音全称）、§1（第 21 行）仅说工商全称待提供（未提官方英文名）→ 订正为「全篇既未展开 zszj 拼音全称、也未登记官方英文名，en 串系产品侧临时拼音派生占位」；另按 r1 建议补 `blockValue` 左边界变异断言（`old_message` 不得被误取为 `message`）固化边界行为。修后 spec 10/10、全量 194/194、prettier、eslint exit 0、G3 violations=0、门禁复验全绿 |

## 验证

- TDD：RED 先行（`login-brand-residue.spec.ts` 首跑 5 失败 / 4 通过）→ GREEN（清理后 9/9；r1 补变异断言后 10/10）。
- 全量单测：**194/194 通过**（新增本 spec 10 用例；`service.spec.ts` 故意打印解密错误到 stderr 属既有陷阱，以内容判定通过）。
- 静态：`prettier --check` 通过 / `eslint` exit 0 / `vue-tsc` 基线 `currentErrors 11 = baseline 11`、`newErrors []`（0 新增）。
- 门禁：`run-local-gates --fast` **10/10 PASS**（含 G3 品牌命名全仓扫描 violations=0；G2 白名单结构自测 exit 0）。
- 合并：`--no-ff` 合并 main `d00692a7`（无冲突；merge-base `7c75f04b`，4 文件均前端 admin-web + scripts/brand 白名单，
  与主树在途 JOB-004 计划稿〔未跟踪〕零交集，不扰动在途工作区）。

## 登记边界（需求约束 + 分批交付，非阻塞，如实登记）

1. **本轮仅交付代码侧可静态验证部分**：消除登录页旧品牌文字回退（Tier 1 文案）。B06 真实 API 环境的登录 / 导航 / 刷新 /
   退出真机联验、桌面 / 窄屏 / 折叠 / 浅深主题回归（Tier 2 SVG 背景、图文正确性）仍按卡片独立放行，未在本轮完成。
2. **Tier 2 / Tier 3 未纳入**：范围经用户裁定仅 Tier 1 文案；SVG 背景品牌残留（Tier 2）、二维码演示登录（Tier 3）不在本轮。
3. **en 展示名为临时占位**：`Zhongshu Zhijia AI Enablement Platform` 系产品侧按已注册 `zszj` 主前缀拼音派生的临时展示名；
   docs/06 全篇既未展开 `zszj` 拼音全称、也未登记官方英文名，正式英文名待用户 / 工商全称确认后回填。
4. **卡片状态不变（开发中）**：ZS-BRAND-003 主卡状态保持「开发中」——.B 的验收（B06 真机 / 多端联验）仍未完成，
   第 2 节统计分布不变；本记录不表示任何主任务已验收。微信开发者工具 / 真机构建归 ZS-CLIENT-005。
