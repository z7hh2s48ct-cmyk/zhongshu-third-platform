# ZS-SEC-002 codex 评审处置（HANDOFF 补评，5×P2 登记处置）

- 评审工具：codex（gpt-6-astra / xhigh，read-only sandbox，`codex review --commit 3fe87023` @ main）
- 评审对象：ZS-SEC-002 接口分类、匿名白名单与方法权限清单（HANDOFF-ZS-SEC-002 交接单的待评审提交；2026-09-14 补评入库）
- 结论：**0×P0/P1，5×P2 全部登记处置**（按 reviews/README「后续处理约定」第 2 条，非阻塞）
- 原始日志：`outputs/handoff/ZS-SEC-002.txt`（未入库；本档案为处置入库）

## 发现与处置（5×P2，全部指向 `ApiInventoryTest` 静态扫描器）

| # | 发现 | 处置 |
|---|---|---|
| 1 | **Fail verification when the committed baseline is missing**（L84-86）：从 checkout 省略基线文件运行时，验证分支静默写入当前清单再与自身比对——路由/权限变化在匿名目录不变时可不带 `api.inventory.update=true` 通过 | 登记处置：门禁硬化项——基线缺失必须失败、仅 `update=true` 允许创建。归 ZS-SEC-002 后续小卡（与 ZS-OPS-001.C 门禁收口同窗处理；当前 CI 固定提交环境基线恒存在，实际漏检需多重条件叠加） |
| 2 | **Exclude block comments before scanning annotations**（L231-233）：行首 `*` Javadoc 行已忽略，但整段块注释（非行首 `*` 形态）中的注解样例可能被误计 | 登记处置：扫描器健壮性——真实 Controller 无此类形态（交付时 PERM=251 与 grep 差 2 已实证注释识别有效），归同上小卡 |
| 3 | **Expand every class-level request-mapping path**（L218-220）：类级 `@RequestMapping` 含占位符/多值时路径展开不完全 | 登记处置：现网 328 端点清单与运行时 HandlerMapping 一致性已由 SEC-012.B 端到端合同间接覆盖；归同上小卡 |
| 4 | **Include effective class-level PreAuthorize guards**（L216-223）：类级 @PreAuthorize 的生效语义未并入权限判定 | 登记处置：启用模块无类级 @PreAuthorize 用法（清单 PERMISSION=251 全部方法级）；防御性登记归同上小卡 |
| 5 | **Preserve authorization expression semantics in the baseline**（L318-324）：基线行仅存权限标识，不保留完整 SpEL 表达式语义（hasAnyAuthority/scope 组合） | 登记处置：结构断言已保证标识合法形态；表达式级语义审计归 ZS-SEC-002 后续小卡/B03 放行评审裁量 |

## 结论原文（总述）

codex 对清单生成器、主体类型推导（ADMIN=322/MEMBER=6）、匿名白名单三来源核清、双向漂移门禁均未发现 P0/P1 回归；5 项发现全部为建议级（P2）扫描器健壮性/门禁硬化项。

## 验证（交付时点证据，沿用卡片开发记录）

- `-pl :zszj-server -Dtest=ApiInventoryTest,ModuleWhitelistTest test`：8/8 BUILD SUCCESS；`-pl :zszj-spring-boot-starter-biz-tenant -Dtest=SecurityFilterChainFixtureTest`：35/35；`run-local-gates --fast` 10/10

## 2026-09-15 P2 硬化收口（feat/sec-002-p2 → main 8f6efc3c）

5×P2 中 #1/#4 收口（`ApiInventoryTest.java` +32/-4），#2/#3/#5 保留登记：
- #1 基线缺失门禁硬化：`!Files.exists(BASELINE)` 时仅 `-Dapi.inventory.update=true` 允许创建；否则 assertTrue 失败
- #4 类级 @PreAuthorize 生效语义：类级 for 循环识别 `@PreAuthorize`；方法级有注解时使用方法级权限（即使为 null 如 `isAuthenticated()`），仅当方法级无注解时才回落到类级

附带修复：AUDIT-002 合并时遗漏的基线更新（新增 AuditEventController 2 端点，331→333）

验证：`mvn -pl :zszj-server -Dtest=ApiInventoryTest test` 4/4 BUILD SUCCESS
codex 评审：r0 `codex review --commit cc8a8362` → 1×P2（`isAuthenticated()` 回落缺陷）；r1 `codex review --commit 10d3b163` → "No actionable regressions" 0×P0/P1
