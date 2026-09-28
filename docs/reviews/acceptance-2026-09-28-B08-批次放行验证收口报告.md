# B08 批次放行验证收口报告（对象、数据、动作、字段和跨组织服务权限）

> 验收日期：2026-09-28
> 验收基线提交：`4f1c6966`（main，B08 Wave1~4 全部子项交付后的最新文档收口）
> 上位依据：[docs/03 §7 批次放行规则 + B08 条款](../03-底座二次开发顺序与验收标准.md)、[docs/05 V2.01](../05-底座模块分析与开发任务清单.md)、[docs/08 敏感业务字段目录决策（D-12）](../08-敏感业务字段目录决策.md)
> 原始日志：`outputs/acceptance/B08-*.log`（本地保留，不入库）

## 1. 验收环境（与 B02~B07 同基线）

| 组件 | 版本 | 供给方式 |
|---|---|---|
| JDK | OpenJDK 17.0.20.1 | `tools/jdk-17.0.20.1+1`（定向单测注入 `JAVA_HOME`） |
| Maven | 3.9.9 | `tools/apache-maven-3.9.9` |
| PostgreSQL | 17（`postgres:16/17-alpine`） | 各 PG 套件自管理一次性容器（验后清理）；G14/G15 自举 compose 环境 |
| Redis | 内嵌（单测）/ Docker 7.4（G14/G15） | — |
| Node.js | v24.19.0 | 系统 PATH |
| Docker | 29.7.2 | Windows 22H2 主机 |

## 2. 验证执行矩阵（全部实跑，非引用旧证据）

| # | 执行入口 | 覆盖 | 结果 | 日志 |
|---|---|---|---|---|
| 1 | `run-local-gates.mjs` 全量 14 门禁 | G1～G15（含 G10 类型基线 / G13 双端 vitest / G14 基础管理 API 层 57473ms / G15 双端联调 CLIENT-005.B E2E 18 用例 420050ms） | **PASS 14/14，失败 0** | `B08-full-gates.log` |
| 2 | `run-pg-regression.mjs`（**20 套件**） | DB-006~018、CFG-002.A、BPM-001、JOB-002~004、OPS-002.B、SEC-011.B、FILE-005.B、LOGIN-005.B、DB-010、IAM-002、IAM-004、FILE-004.B | **PASS 20/20 套件，失败 0，exit 0** | `B08-pg-regression.log` |
| 3 | 批A：biz-data-permission 模块全量 + security 定向 | 对象授权裁决全链（`ObjectAuthorizationServiceTest` 27 + `ObjectAuthorizationClassificationTest` 18〔D-12 分级裁剪〕+ `FieldLevelScopeResolverTest` 10 + `FieldMaskUtilsTest` 7 + `FieldLevelTest` 2 + `OrgDataPermissionCheckerTest` 15 等 **135/135**）+ 跨组织 visit（`CrossOrgVisitScopeHolderTest` 11 + `SecurityFrameworkServiceImplCrossOrgVisitTest` 11 + `SecurityFrameworkUtilsTest` 18 = **40/40**） | **175/175，BUILD SUCCESS** | `B08-targeted-matrix.log` |
| 4 | 批B：module-system 定向 11 类 | org 轴范围解析（`OrgDataScopeResolverTest` 18）+ visit 授权写入收敛（`PermissionServiceTest` 47，含**两组织互授负向矩阵 5 例**：越界组织/无任职/租户管理员/超管/空目标集均拒绝）+ 授权记录（`CrossOrgVisitServiceImplTest` 32）+ 撤权一致性（`OrgDataPermissionRevocationConsistencyTest` 8）+ 缓存驱逐补偿（PERM-004.C 三类 14）+ 消息 org 轴（`NotifyMessageOrgVisibilityTest` 15）+ 对象授权端点（`ObjectAuthorizationControllerTest` 5） | **151/152 实跑，1 失败为既登记 flaky（§3.1），隔离复跑 18/18 全绿** | `B08-targeted-matrix.log` |
| 5 | 批C：module-infra 定向 | 文件 org 轴授权（`FileServiceOrgAuthorizationTest` 14）+ 导出两阶段重检（`FileExportDeliveryTest` 16，含导出不放宽/脱敏字段拒绝） | **30/30，BUILD SUCCESS** | `B08-targeted-matrix.log` |
| 6 | admin-web vitest 全量 | CLIENT-001.A/.B 消费层 + 授权导航 + 会话清理（含对象授权 store/清理竞态回归） | **229/229** | `B08-web-vitest.log` |

## 3. 验收期发现与处置（如实登记）

### 3.1 module-system 全量/批量排序下 OrgDataScopeResolverTest 单例偶发（既登记 flaky 家族，非本批次回归）

批B 实跑 `OrgDataScopeResolverTest` 18 例中 `testResolve_orgLeader_ledOrgIdsFilled` 1F（org/任职行查询在跨类残留上下文下返回空集）。该家族已于 2026-09-28 PERM-003.B 交付轮**主树对照实锤**（main `8fbf83d0` 无关改动同命令复现同签名失败，失败集合随排序轮换；本轮批B 失败例与当日 run2/run3 失败例互不相同，同为轮换形状）。处置：隔离复跑 **18/18 全绿**（本轮日志在案）；定性为「跨类线程上下文残留」既有测试基建问题（`SmsCodeServiceImplAttemptLimitTest` 3 例为同一家族的稳定成员，main 基线在案），不属任何 B08 交付卡回归。**建议立测试基建小卡根治**（TenantContextHolder/Security 上下文跨类清理审计），不阻塞本批放行。

### 3.2 PG 回归聚合器含 FILE-004.B 第 20 套件（交付时已注册，无缺口）

与 B07 §3.1（IAM-004 套件漏注册）不同，本批 12 个子项交付链的 PG 套件注册齐全（FILE-004.B 交付时注册第 20 套件），聚合 20/20 一次全绿，无验收期编排适配。

## 4. 逐卡验收结论（10 卡，开发中→已验收）

验收规则：以 docs/05 各卡「验收」条款逐条对照 §2 实跑证据；字段等级目录以 **D-12 批准清单（docs/08）**为决策依据。各卡开发期证据（失败测试、实现、CodeReview 复审弧）见 docs/05 卡内开发记录与 docs/reviews/ 各档案，本节登记放行轮实证。

| 模块 | 卡 | 验收条款 → 本轮放行证据（§2 编号） | 结论 |
|---|---|---|---|
| SEC | ZS-SEC-001 | 无访问权限/伪造头/停用无效目标拒绝 → #3 CrossOrgVisitServiceImpl 32（10 步 fail-closed 判定序）；具备入口不自动获得全部动作/敏感字段 → #3 SecurityFrameworkServiceImplCrossOrgVisit 11（D6 动作收敛）+ #3 Holder 11（对象/字段维 fail-closed）；获批后补充正向矩阵 → #4 PermissionService 47（visit 范围正反向）+ #1 G15 E2E；撤权与上下文清理有效 → #4 RevocationConsistency 8 | **已验收** |
| PERM | ZS-PERM-001 | 篡改用户/角色/部门 ID、批量混入、自我提权拒绝；合法授权成功、重复幂等 → #4 PermissionService 47（授权目标归属/上限校验/visit 范围收敛正反向 5 例负向）；外键对象不以 tenant_id 填值认定安全 → #4 OrgDataScopeResolver 18（有效任职/启用口径 fail-closed） | **已验收** |
| PERM | ZS-PERM-002 | 同租户无对象权限/跨租户/已知 ID/批量混入/导出统计按矩阵限制 → #3 OrgDataPermissionChecker 15（对象级/批量整批拒绝）+ #5 FileServiceOrg 14（org 轴对象门）+ #4 NotifyMessageOrg 15；合法汇总不暴露敏感明细 → #3 分类裁剪 18（超上限字段不进授权集合）；DB 回归复用 → #2 20/20 | **已验收** |
| PERM | ZS-PERM-003 | 隐藏字段不经详情/批量/导出/文件/错误信息旁路 → #3 分类裁剪 18（超上限拒绝输出）+ #5 FileExportDelivery 16（导出声明字段 ⊆ authorizedFields + masked 拒绝）；前端伪造 allowedActions 不生效 → #6 object-authorization.spec（唯一写路径=服务端响应）+ 服务端 checkActionAllowed/checkFieldsAllowed 同核；状态/授权变更后重校验 → #3 ObjectAuthorizationService 27（实时裁决） | **已验收** |
| PERM | ZS-PERM-004 | 撤权后旧 Token/菜单/缓存不能继续受限动作 → #4 RevocationConsistency 8（任职失效即时收缩/恢复不复活越界授权）+ #4 缓存驱逐补偿 14（失败键持久化重放/防旧值写回）；缓存故障不扩大权限 → #4 驱逐失败 fail-closed（DEAD 兜底）；审计含操作者/目标/变更/结果/trace → #4 一致性套件审计断言 | **已验收** |
| FILE | ZS-FILE-001 | 授权者上传下载成功；无凭据/他人/另一租户/猜 ID/批量混入均拒绝 → #5 FileServiceOrg 14（org 轴对象门 + visit 收敛）；公开素材按批准用途 → #2 FILE 域套件 | **已验收** |
| FILE | ZS-FILE-004 | 票据原子兑换/转发/跨租户/撤权后不能取流 → #2 第 20 套件 + #5 FileExportDelivery 16；导出生成期间撤权停止后续输出 → #5（organizationId=源对象组织结构保证）；在途分块重检 → #2 P 套件（交付时证据 + 本轮聚合实跑） | **已验收** |
| MSG | ZS-MSG-003 | 他人 ID 不能改已读/另一租户不能读消息/撤权后旧正文受限/未知模块落点明确不可用 → #4 NotifyMessageOrgVisibility 15（org 轴可见范围 + 落点对象门 fail-closed）+ 交付链 MSG-003.A/.B 既有套件 | **已验收** |
| CLIENT | ZS-CLIENT-001 | 刷新/直达授权页可用、关闭模块/伪造角色不能操作 → #1 G15 E2E 18 用例 + #6 vitest 229（access.ts 授权核心）；撤权后旧页签不展示旧敏感数据、接口独立拒绝 → #6 auth-session/object-authorization spec（三原因清理 + clearEpoch 竞态守卫）；登录失败不留半初始化路由 → #6 bootstrap-failed 断言 | **已验收** |
| CLIENT | ZS-CLIENT-002 | 不同技术权限对应入口、无授权/关闭模块/未知落点拦截 → #1 G15 E2E + 交付链服务端 33 例（#4 CrossOrgVisitServiceImpl 32 + Controller）+ 客户端 79 例（#1 G13 双端 vitest 覆盖）；D-09 未批准身份切换 → 交付面零切换入口（my-targets 仅返回授权目标） | **已验收** |

## 5. 正向/反向验收对照（docs/03 B08 条款）

- **B08 正向**：总部/负责人/员工在授权范围内获得正确对象、字段、动作、附件和导出结果——对象维（#3 Checker 15 + #5 File 14 + #4 Notify 15）、字段维（#3 分类裁剪：F3 对员工不可见、F2 脱敏可见按 D-12 §3 映射）、动作维（#3 ObjectAuthorizationService 27 状态钩子 + RBAC 交集）、附件和导出（#5 FileExportDelivery 16 两阶段重检）；授权撤销同步生效（#4 RevocationConsistency 8 + 缓存驱逐补偿 14 + PERM-004.B 撤权审计）。
- **B08 反向**：篡改租户头（租户拦截器 + IAM-002 三维复验，B07 已验，本轮 #3 Authority 链回归）、对象 ID/批量 ID（#3 Checker 批量整批拒绝）、导出条件（#5 声明字段超集拒绝 + masked 拒绝——**导出不放宽**）、文件 ID（#5 File org 门）均不能越权；**管理员汇总权限不自动获得全部敏感明细**（#3 分类裁剪：超管外访问者 F3 字段不进 authorizedFields、执行侧 fail-closed；D-12 F2 对低于 F2 访问者脱敏）。

## 6. 未验证项与登记边界（不随本轮放行提升）

1. **org 轴 SQL 列表静默过滤规则 `OrgDataPermissionRule` + 业务表 `org_id` 列注册**仍未落地（B07 §6.4 移交项延续）——对象级门（Checker）与各卡显式接入已验，SQL 级列表过滤随 B09/B10 下游领域模块首张业务表注册时落地并联验。
2. **字段等级目录的业务域接入留后**（循「入口先行」）：PERM-003.B 交付机制与 D-12 目录配置接入（#3 分类裁剪 18 例以中性夹具证明全链路），A 类线索域字段存在性待 D-07 阶段 2 确认（PEND-001）；「对象 owner 维 F2 升级」等域级扩展随域接入评估（codex-ZS-PERM-003.B 登记项）。
3. **CLIENT-001.B 页面/组件级渲染接线**（按钮指令、字段组件）随业务域接入；本期端点对未接入域返回 null=零变化（#4 Controller 5 例零输出语义）。
4. visit 字段维 F1 默认上限属授权发放环节约定（SEC-001.B 判定序），分级机制不叠加（矩阵 §10.3-2）；checker/resolver 双缓存 key 为登记权衡。
5. **测试基建小卡（建议立卡）**：§3.1 顺序依赖 flaky 家族根治（跨类上下文残留审计）；`SmsCodeServiceImplAttemptLimitTest` 3 例为其稳定成员（main 基线在案）。
6. SERIALIZABLE 性能监控归 B11（ZS-OPS-002.C）；容量/性能与真机验收归 B10/B11。

## 7. 回滚路径

- 本轮验证全部使用一次性 Docker 容器与 `outputs/` 本地日志，**未触碰任何长驻数据**。
- 状态翻转（10 卡）经 `scripts/gov/close-task.mjs` 幂等执行 + `verify-docs`（G5）0 issue 复核；如需回退，`git revert` 放行提交即可整体还原（docs/05 §2/§19/README 统计随提交原子回退）。
- 本轮无产品代码变更、无迁移、无脚本适配（与 B07 的验收期适配补丁不同，§3.2），回滚面仅文档。
