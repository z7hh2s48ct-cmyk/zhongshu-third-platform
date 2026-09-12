# ZS-CFG-001.B codex 评审结论与处置

> 被审提交（r0）：`0c569ef0`（ZS-CFG-001.B 参数读写权限、可见性与输出脱敏，B03；前置 ZS-CFG-001.A、ZS-SEC-007 已完成；worktree 隔离分支 `feat/cfg-001-b`，`--no-ff` 合并 main `4e41e1dd`）。
> r0 P1 修复提交：`b1bd2f5f`（脱敏项回传掩码时保留库中原值，防 `******` 覆盖真实秘密，3 files +91/-2）。
> 二次复核（r1）：`codex review --commit b1bd2f5f`，发现 **1 项 P1**（r0 修复引入的更深旁路）。
> r1 P1 修复提交：`8975cf5c`（保留掩码值时禁止下调敏感级保护，堵两步洗密暴露旁路，4 files +82/-1）。
> 三次复核（r2）：`codex review --commit 8975cf5c`，结论 **PASS / 0 发现**。
> 评审工具：`codex-cli 0.154.0`，模型 `gpt-6-astra`，reasoning effort `xhigh`，sandbox `read-only`，approval `never`。
> 完整 stdout：r0 [codex-ZS-CFG-001.B.raw.md](codex-ZS-CFG-001.B.raw.md)、r1 [codex-ZS-CFG-001.B-r1.raw.md](codex-ZS-CFG-001.B-r1.raw.md)、r2 [codex-ZS-CFG-001.B-r2.raw.md](codex-ZS-CFG-001.B-r2.raw.md)。
> 说明：本任务评审直接对提交执行，未走事前 HANDOFF 交接单；本文件即评审产物 + 处置，遵循 [README.md](README.md)「后续处理约定」。评审弧呈**级联深挖**：r0 修数据损坏 → r1 揪出该修复引入的洗密旁路 → r2 确认旁路封堵，两轮 P1 均以 TDD RED→GREEN 修定。

## Codex 原始结论（r0，被审提交 `0c569ef0`）

判定 **FAIL**，发现 **1 项 P1**（无 P0 / P2 / P3）：

> The new masking breaks the existing edit/save round-trip: routine metadata edits can overwrite real configuration values with the mask. This is a data-corruption regression.

### Review comments

- **[P1] Preserve stored values when saving a masked configuration** — `services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/controller/admin/config/ConfigController.java:81`
  > When an administrator edits only the name or remark of an invisible/secret parameter, `ConfigForm.vue` loads this response into `formData` and submits the masked value to `/infra/config/update`. `ConfigServiceImpl.updateConfig` persists that value unchanged, silently replacing the actual configuration with `******`. This also affects `system.user.init-password`, which supplies passwords for imported users. Add explicit unchanged-value handling across the edit/save flow and a regression test so the display mask cannot overwrite stored values.

## 复核与处置（r0 P1）

### P1-1 — 脱敏掩码往返覆盖真实秘密（数据损坏回归）（✅ 已修复，r1 复评引出更深旁路）

**复核结论：成立。** [ConfigController#getConfig](../../services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/controller/admin/config/ConfigController.java) 详情统一走 `getMaskedConfigRespVO` 将敏感/秘密项 `value` 掩码为 `******`；[ConfigForm.vue](../../apps/zhongshu-admin-web/src/views/infra/config/ConfigForm.vue) 把该响应整体载入 `formData`，管理员仅改名/备注后提交，`updateConfig` 原样持久化 → 真实值被 `******` 静默覆盖，`system.user.init-password`（导入用户初始密码源）等首当其冲。此为 ZS-CFG-001.B 输出脱敏引入的**编辑/保存往返数据损坏回归**。

**TDD 修复（commit `b1bd2f5f`）**：[ConfigSensitiveClassifier](../../services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/service/config/ConfigSensitiveClassifier.java) 增 `MASK_VALUE="******"` 常量与 `isMaskedEcho(exists, submitted)`（提交值等于掩码且原项非 NORMAL 判定为"回显掩码"）；[ConfigServiceImpl#updateConfig](../../services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/service/config/ConfigServiceImpl.java) 命中 `isMaskedEcho` 时用库中原值回填 `updateObj.setValue(exists.getValue())`，掩码不再落库；`ConfigServiceImplMaskTest` 增回归护栏（掩码往返保原值、显式改值仍生效）。config 套件 GREEN 26/0。

## 二次复核（r1，被审提交 `b1bd2f5f`）

对 r0 修复提交重跑 `codex review --commit b1bd2f5f`，判定 **FAIL**，发现 **1 项 P1**——r0 的"保留掩码值"修复本身引入了一条更深的秘密暴露旁路：

> The patch fixes masked-value corruption but introduces a two-update bypass that exposes stored secrets. This was reproduced using the compiled service and controller with an in-memory mapper.

### Review comments

- **[P1] Prevent secret downgrades when preserving masked values** — `services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/service/config/ConfigServiceImpl.java:67-68`
  > A caller with configuration-update permission can now expose an existing secret without knowing its value: rename `sys.db.password` to `biz.alias` with `visible=false` and `value="******"`, then submit another update with `visible=true` and the same mask. This branch preserves the secret through both updates, but the second update no longer classifies the key as SECRET, so `/get-value-by-key` and detail responses return the original plaintext. Previously, the first update overwrote that secret with the mask. Reject secret-to-nonsecret renames when retaining the stored value, or preserve its secret classification across renames, and cover this two-update sequence with a regression test.

**codex 实证复现**（jshell + `URLClassLoader` 加载已编译 `ConfigSensitiveClassifier`/`ConfigServiceImpl`/`ConfigController` + 内存 mapper，r1 raw L14802-14804）：

```
AFTER_RENAME=SENSITIVE,ReviewDummySecret123      # 第一步：改名 sys.db.password→biz.review.alias + 掩码保原值 → 秘密降 SENSITIVE，真值仍在库
AFTER_VISIBLE=NORMAL,ReviewDummySecret123        # 第二步：翻 visible=true + 同掩码 → 降 NORMAL
GET_VALUE_BY_KEY=ReviewDummySecret123            # /get-value-by-key 直接读出明文
```

## 复核与处置（r1 P1）

### P1-2 — 两步洗密暴露旁路（保留掩码值时可下调保护级）（✅ 已修复 + r2 确认 CLEAN）

**复核结论：成立（codex jshell 实证，非静态推断）。** r0 修复保住真值不丢，但既有 TOCTOU 守卫（`(wasSecret||nowSecret) && toVisible` 抛 `CONFIG_SENSITIVE_CAN_NOT_SET_VISIBLE`）**只拦单步 SECRET→visible**，拦不住"改名脱密"分级降级链：第一步把秘密键改成非秘密键（掩码保真值，`classify` 由 SECRET 降 SENSITIVE），第二步再翻 visible（由 SENSITIVE 降 NORMAL），此后 `/get-value-by-key` 与详情按 NORMAL 放行 → 无需知晓原值即可读出明文。

**TDD 修复（commit `8975cf5c`）**：
- [ConfigSensitiveClassifier](../../services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/service/config/ConfigSensitiveClassifier.java) 增 `isProtectionDowngrade(exists, updated)`（`protectionRank(updated) < protectionRank(exists)`，rank：SECRET=2 / SENSITIVE=1 / NORMAL=0，按枚举声明序）；
- [ConfigServiceImpl#updateConfig](../../services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/service/config/ConfigServiceImpl.java) 在 `isMaskedEcho` 分支内、回填原值**之前**加降级守卫：回传掩码即调用方不掌握真值，禁止在同一次更新里下调该值保护级，命中抛新错误码；同级编辑与显式改真值不受影响；
- 新错误码 `CONFIG_SENSITIVE_CAN_NOT_DOWNGRADE_ON_MASKED_ECHO`（`1_001_000_006`）；
- `ConfigServiceImplMaskTest` 增 2 例两步洗密回归护栏（改名脱密 + 掩码保值应拒、掩码保值翻 visible 应拒，均 `verify(configMapper, never()).updateById(...)`）。

**RED→GREEN 经验裁决**：2 新测试首跑 RED（`Expected ServiceException...but nothing was thrown`，实证旁路可走通）→ 加守卫后 GREEN。config 套件 **28 测试 / 0 失败**（Classifier 6 + Mask 10 + ServiceImplTest 12），`MVN_EXIT=0`，反应堆 `BUILD SUCCESS`。

**编译教训**：MyBatis-Plus `BaseMapper` 有 `updateById(T)` 与 `updateById(Collection<T>)` 双重载，Mockito `verify(...).updateById(any())` 裸 `any()` 返 `Object` 触发"引用不明确"编译错误 → 须类型化 `any(ConfigDO.class)` 消歧义。

## 三次复核（r2，被审提交 `8975cf5c`）

对 r1 修复提交重跑 `codex review --commit 8975cf5c`（`gpt-6-astra`/`xhigh`/`read-only`），结论 **PASS，0 发现**（无 P0 / P1 / P2 / P3）：

> No actionable regressions were found. The guard rejects protection downgrades when preserving masked values without disrupting same-level edits or explicit replacement values. Tests were inspected but not rerun in this read-only environment.

codex r2 在 exec trace 中自核 `git diff --numstat`（ErrorCodeConstants 1/0、Classifier 31/0、ServiceImpl 7/1、MaskTest 43/0）与 surefire 报告（MaskTest Tests run:10, Failures:0），确认降级守卫封堵两步洗密旁路，且不干扰同级编辑与显式改值。**r0/r1 的 2×P1 已全部消除。**（codex sandbox `read-only` 未执行测试，通过性以本机 28/28 `BUILD SUCCESS` + `MVN_EXIT=0` 为准。）

## 结论

**评审通过（r0 发现 1×P1 数据损坏已修，r1 发现 1×P1 两步洗密旁路已修，r2 复核 0 发现确认消除）。**

- **r0**（被审 `0c569ef0`）：FAIL，1×P1——输出脱敏后管理员仅改元数据即把掩码 `******` 落库覆盖真实秘密（数据损坏回归，波及 `system.user.init-password`）。
- **r0 修复**（`b1bd2f5f`）：`isMaskedEcho` 命中时以库中原值回填，掩码不落库；GREEN 26/0。
- **r1**（被审 `b1bd2f5f`）：FAIL，1×P1——r0 修复引入两步洗密旁路（改名脱密降 SENSITIVE → 翻 visible 降 NORMAL → `/get-value-by-key`/详情读明文），codex jshell 实证复现。
- **r1 修复**（`8975cf5c`）：`isProtectionDowngrade` + `protectionRank` + 掩码分支降级守卫 + 错误码 `1_001_000_006` + 2 回归测试；RED→GREEN，config 套件 28/0。
- **r2**（被审 `8975cf5c`）：PASS，0 发现，codex 自核 diff/surefire 独立确认旁路封堵、无新回归。
- **本机复验证据**：config 套件 28 测试全绿（Classifier 6 + Mask 10 + ServiceImplTest 12），`MVN_EXIT=0`，`BUILD SUCCESS`。
- ZS-CFG-001.B 为 ZS-CFG-001 分批子项（B03），父卡状态维持 **开发中**；本子项契约层已交付，退出条件"敏感项不能经 visible、详情、导出、日志泄露"已由 TOCTOU 守卫 + 掩码保原值 + 降级守卫三重覆盖（详情/`get-value-by-key` 路径）。真实 PG/API 端到端与导出/日志泄露面归 ZS-SYS-001.A、值校验/审计归 ZS-CFG-004。
- codex 发现总数 **2**（2×P1），已修复 **2**，仍有效 **0**。
- **延后 Minor**（非阻塞，归 ZS-CFG-004 值校验/审计批次）：①`classify` 键归一 `contains` 匹配存在过匹配（前缀/子串误判普通键为秘密）；②`SECRET_KEY_TOKENS` 白名单外缩写可绕过秘密识别；③变更审计依赖框架 `ApiAccessLogFilter`（ZS-SEC-007 脱敏），非本卡新增专项审计。
