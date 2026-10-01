# FC-003 放行验证收口报告（双端工作台与待办/通知接线，PILOT-REQ-010）

> 验收日期：2026-10-01
> 验收基线提交：`9c9d96f1`（main，docs/05 V2.11——FC-003 服务端接线 + 前端 wave 全部交付后的最新文档收口）
> 上位依据：[docs/03 §7 批次放行规则](../03-底座二次开发顺序与验收标准.md)、[docs/05 V2.11→V2.12](../05-底座模块分析与开发任务清单.md)、[docs/09 首链业务闭环决策输入](../09-首链业务闭环决策输入.md)
> 原始日志：`outputs/fc003-release-full-gates.log`、`outputs/fc003-release-pg-regression.log`、`outputs/fc-release-firstchain-targeted.log`（本地保留，不入库）

## 1. 验收环境（与 B02～B09/FC 批次同基线）

| 组件 | 版本 | 供给方式 |
|---|---|---|
| JDK | OpenJDK 17.0.20.1 | `tools/jdk-17.0.20.1+1` |
| Maven | 3.9.9 | `tools/apache-maven-3.9.9` |
| PostgreSQL | 17-alpine（17.11） | 各 PG 套件自管理一次性容器（验后清理）；G14/G15/G16 自举 compose 环境 |
| Node.js / pnpm | v24.x | 系统 PATH |
| Docker | 29.7.2 | Windows 22H2 主机 |

## 2. 验证执行矩阵（全部实跑，非引用旧证据）

| # | 执行入口 | 覆盖 | 结果 | 日志 |
|---|---|---|---|---|
| 1 | `run-local-gates.mjs` 全量 15 门禁 | G1～G10/G13～G16（含 G10 类型基线 / G13 双端合同 vitest 150 用例含守卫 66 / G15 双端联调 E2E 18 用例 / G14 真实 server 七类矩阵 50 用例 / **G16 三套件联合门禁：bpm001 + bpm003 + firstchain**；并发总耗时 571892ms） | **PASS 15/15，失败 0，exit 0** | `fc003-release-full-gates.log` |
| 2 | `run-pg-regression.mjs`（**22 套件**） | 全量真实 PG 回归（含第 21 套件 BPM-003 真实引擎、第 22 套件 ZS-FC-001/002 首链申请/线索域 PG 运行期 8 例） | **PASS 22/22 套件，失败 0，exit 0** | `fc003-release-pg-regression.log` |
| 3 | firstchain 模块定向测试批（H2 面；交付后含 MOBILE 落点分支） | Controller 权限矩阵 10 + OpeningService 12 + LeadService 21 + LeadAppService 17（三视角/资格门/D-12 裁剪）+ EmployeeService 6 + **NotifyWiring 7（六节点接线/待办 Outbox 事件/通知派发命令）** + **NotifyLandingProvider 8（authorize 硬合同 + WEB/MOBILE 落点）** + ApplicationService 门面 4 | **85/85，BUILD SUCCESS** | `fc-release-firstchain-targeted.log` |
| 4 | MSG-003 五道防线回归（system 模块定向） | `NotifyLandingServiceTest` 11（归属/未注册/**模块停用 MODULE_DISABLED**/authorize 拒绝→REVOKED/端不支持 CLIENT_UNSUPPORTED/租户缺失）+ `NotifyMessageOrgVisibilityTest` 15（org 归属门/REVOKED/他人消息拒绝） | **26/26，BUILD SUCCESS** | 会话留档（定向 `-Dtest` 实跑） |

前端 wave 专项（交付轮已取证，本轮经 #1 回归复验）：G10 类型基线零新增、双端 eslint 0、`pnpm build:mp` DONE（pages-firstchain 入产物）、守卫单测 66/66（含 FC-003 授权/前缀继承/模块门启停两向 +4）。

## 3. 验收期发现与处置（如实登记）

1. **一次全绿**：full 门禁 15 项与 PG 回归 22 套件均首轮一次全绿，无验收期补丁、无失败复跑（承 B08/B09/FC 批次先例）。
2. **交付期预存在缺陷已闭环**（`00a4e001`，前端 wave 验收期定向复跑暴露）：`NotifyLandingServiceTest` 停用模块夹具以 bpm 为停用假设——B09 骨架（`5531487`）已将 bpm 入启用面，假设失效致夹具 authorize 被触碰；改用真停用模块 mall 后 11/11。该缺陷自 B09 起潜伏（system 模块测试不在 G1～G16 门禁面），非本轮交付引入。

## 4. 逐卡验收结论

| 模块 | 卡 | 验收条款 → 本轮放行证据（§2 编号） | 结论 |
|---|---|---|---|
| FC | ZS-FC-003 双端最小工作台与待办/通知接线 | **PILOT-REQ-010：审批、开通、下发、分配、领取和结束状态均可通过 trace/业务 ID 追踪** → #3 NotifyWiring 7（待办注册命令/NOTIFY_TODO_TRANSITION Outbox 事件〔transitionType/todoKey/sourceType 载荷合同〕/七模板派发命令〔eventId 幂等键携版本、收件人服务端解析、INBOX 渠道〕）+ #1 G14 真实 server（六节点写路径经真实安全链/真实 PG 落审计与 send_log）；**通知失败不静默** → MSG-001 dispatcher 失败以持久化状态落 `system_notify_send_log`（NO_SINK/TEMPLATE_* 等可查询，B05 已验收机制）+ #3 技术异常上抛回滚；**PILOT-REQ-009 三角色呈现范围与服务端一致** → Web 三页（视角服务端解析，请求不携带归属/视角参数）+ UniApp 详情动作条按权限×状态显隐 + #3 LeadAppService 视角收敛组 + 守卫单测 66（授权/前缀继承/模块门）；**反向：他人待办不可操作** → recipient 服务端绑定（wiring 注册 recipient=审批人/负责人，服务端判定）+ MSG-002 既有合同（已验收）；**撤权后旧落点 REVOKED** → #4 NotifyLandingServiceTest authorize 拒绝路径 + org 归属门（NotifyMessageOrgVisibilityTest 15）；**模块停用落点明确不可用** → #4 MODULE_DISABLED 用例（夹具修正后直证，§3.2）+ CLIENT-002 模块门单测（enabledModules 不含 firstchain → 403）；**伪造落点参数不越过 authorize** → #4 畸形 leadId fail-closed（LEAD_NOT_EXISTS）+ 线索域 authorize 复用三视角可见性（#3 落点 8 例）+ LEAD_VISIBLE_DENIED 服务端拒绝；**两端循 CLIENT-001 授权导航注册表** → menu.json 授权组 + 动态路由 import.meta.glob（不复制权限判定，页面可达性由既有守卫最终判定） | **已验收** |

**不随本轮翻卡**（维持原状）：ZS-BRAND-003（.B Tier-2 真机回归未完成，开发中）；ZS-DB-002（服务器终核待负责人，开发中）；ZS-CLIENT-005/ZS-OPS-002/ZS-FILE-005（.C 归 B11，开发中）；ZS-OPS-003（待前置）。

## 5. 未验证项与登记边界（不随本轮放行提升）

1. **多端业务 E2E 与真实角色 UAT 归 B10**（循 §B10「真实角色 UAT 报告单列，不能由自动化测试代替」）：三角色工作台的真实账号走查（平台审批→开通→下发→分配→领取→跟进→结束全链真机操作）与 G15 技术联调层 E2E 的业务扩展用例不在本轮。
2. **分配/改派目标员工为编号输入**（Web/UniApp 同）：员工选择器（组织成员列表组件）后续优化，归属校验由服务端 `UserOrgChecker` 兜底。
3. **APPLICATION 域 MOBILE 落点未注册**（平台运营审批台为 Web-only，移动端解析判 CLIENT_UNSUPPORTED）：随平台移动端需求另立。
4. **强制首改后置**（M5-A 登记）；**派发节奏参数**（interval-ms 等）随 OPS-002 运行监控实测回填。

## 6. 回滚路径

本轮放行为**验证收口 + 文档翻卡**，验收执行与基线 `9c9d96f1` 同源、无产品代码变更；翻卡以 docs/05 V2.12 单独提交承载，如需回滚直接 revert 该文档提交即可，不影响任何运行时行为。
