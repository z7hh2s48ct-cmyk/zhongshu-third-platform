# ZS-CLIENT-002.B 评审处置（获批业务组织导航：CodeReview 独立复审两轮，R2 PASSED / 0×P0/P1）

- 评审对象：ZS-CLIENT-002.B 获批业务组织导航——服务端 my-targets 授权目标数据源 + 客户端五级路由判定/模块门 + 访问租户切换原子回滚（B08；docs/05 §16.1 line1052 子项「获批业务组织导航；身份切换仅按 D-09」「非法身份切换拒绝，切换后导航和缓存刷新；未批准则验无切换入口」，前置 ZS-CLIENT-002.A〔移动导航注册表与直达页守卫〕+ ZS-IAM-002〔组织/任职模型 + 服务端组织上下文〕+ ZS-PERM-002.B〔org 轴范围解析/对象检查入口〕均已交付）
- 隔离分支：worktree `.wt/zszj-wt-client-002-b` 分支 `feat/client-002-b`（自 main `0b75da1c`；feat `e474f67e` + test `2a926ea7` + 评审响应 `99b06b47`）
- 合并：`--no-ff` main `475dc6dd`（22 files +1049/-115，feat 分支自 `0b75da1c` 线性领先、干净合并）
- 卡片性质：**产线导航消费落地卡**——服务端授权目标数据源（my-targets）+ 客户端消费端收敛（五级判定 + 模块门 + 切换原子回滚），属真实产线代码变更，走完整 9 步周期
- 评审工具：codex 账号用量上限阻断（承 ZS-PERM-002.B / ZS-PERM-004.B / ZS-LOGIN-003.B / ZS-SEC-001.B / ZS-PERM-001.B / ZS-FILE-001.B / ZS-MSG-003.C 先例），改以 **CodeReview 独立复审替代**
- 结论：**R1 报 1×P0（0×P1/P2/P3）→ 修复（接口清单基线登记 + 清单文档统计同步 + 门禁复跑）→ R2 独立复审 PASSED（0×P0 / 0×P1）**
- 原始日志：`outputs/_client002b-*.log`（本地未入库，gitignore；本档案为处置入库）

## 根因/背景（§16.1 line1052 验收 + 前置移交）

ZS-CLIENT-002.A（B06）已交付移动端服务端授权导航注册表与直达页守卫（menu.json 单一真相源 + 四级判定），但其登记边界「停用模块启用信号需接线 `AuthPermissionInfo`、前缀继承语义、共享表单动作级权限」与 SEC-001.B D7 边界「各业务路径逐一接入随下游 .../CLIENT-002.B」均指向本卡；§16.1 line1052 验收：「非法身份切换拒绝，切换后导航和缓存刷新；未批准则验无切换入口」。本卡在 .A 注册表之上落地三块：①服务端 my-targets 授权目标数据源（授权唯一真相源）；②客户端五级路由判定（补 enabledModules 模块门）+ EXTRA_ROUTE_ACCESS 登记收口；③获批业务组织导航（访问租户切换 + 原子回滚）——身份切换仅按 D-09，D-09 之前不实现多身份切换。

## 交付内容（feat `e474f67e`）

**服务端 module-system（my-targets 数据源）**：
- `CrossOrgVisitController`：`GET /admin-api/system/cross-org-visit/my-targets`（`AUTHENTICATED`），身份取自服务端会话（不信任客户端入参），`@DataPermission(enable=false)`，返回 loginTenantId/loginTenantName/targets[]；
- `CrossOrgVisitServiceImpl#listMyAuthorizedTargets`：与 authorizeVisit 同四要素 fail-closed 过滤（最新记录有效 / 目标租户启用 / 组织范围全部有效 / 非登录租户），未获批或记录失效返回空列表不报错（fail-closed）；`CrossOrgVisitMyTargetsRespVO`（新）+ Mapper 查询 + 接口方法；
- `AuthPermissionInfoRespVO.enabledModules`（`ModuleCatalog.ENABLED_MODULES` 经 `AuthConvert` 下发）。

**客户端 zhongshu-miniapp（路由与切换）**：
- `access.ts` 五级判定：公共外壳 → enabledModules 模块门 → 精确登记 → 最近声明目录继承（仅直接父目录）→ 拒绝；EXTRA_ROUTE_ACCESS 4→15 条（补 21 页差集 + dept/form 动作级），消除兄弟目录权限上溢；
- `interceptor.ts` / `tabbar/store.ts` 双落点传 enabledModules 第三参；`store/user.ts` 缓存 + 类型扩展；
- `utils/tenant-visit.ts`（新）：buildVisitTenantOptions / planVisitSwitch 三态 / resolveStaleVisit / performVisitSwitch 原子回滚（失败恢复 previousVisitTenantId + clearDictCache + toast）；
- `api/system/cross-org-visit/index.ts`（新）getMyVisitTargets：Long→Number 归一化（ID 字符串合同）；
- `tenant-visit-picker.vue` props 化重写（移除旧 getTenantSimpleList）+ `pages/user/index.vue` 接线（失效访问态清理、确认弹窗）。

## 新增/改测试（test `2a926ea7`）

| 测试 | 模块 | 用例 | 断言 |
|---|---|---|---|
| `CrossOrgVisitServiceImplTest`（+8 例，总 32） | system | my-targets 四要素过滤 | 最新记录有效 / 目标租户启用 / 组织范围全部有效 / 非登录租户剔除；fail-closed 空列表不报错；D-09 资格门；目标组织失效剔除；Long 精度链路 |
| `AuthConvertTest`（+1 例） | system | enabledModules 下发映射 | ModuleCatalog→VO 映射 |
| `router-access.spec.ts`（+28 条 it/describe，总 63 例） | miniapp | 五级判定 + 模块门 + 继承 + 登记 | 逐级顺序、模块门（null 跳过 / 停用拒绝 / 外壳豁免）、最近声明目录继承、EXTRA 15 条登记、拦截链路与 tabbar 隐藏联动 |
| `tenant-visit.spec.ts`（新，16 例） | miniapp | 选项构建 + 三态计划 + 回滚 | 选项去重（登录项恒首）、noop/restore/next 三态、失效识别、切换失败原子回滚（恢复原有访问范围） |

## RED → GREEN

- **RED**（实现前）：Java testCompile 失败（`cn.zszj.module.system.controller.admin.permission.vo.crossorgvisit` 包缺失 = 预期缺失符号）；miniapp `8 failed | 55 passed (63)`（router-access/tenant-visit 新用例失败 = 预期缺失行为）；
- **GREEN**（实现后）：Java targeted **33/33 绿**（`CrossOrgVisitServiceImplTest` 32 + `AuthConvertTest` 1）BUILD SUCCESS；miniapp targeted **79/79 绿**；miniapp 全量 **147/147 绿**；
- **flake 归因**：module-system 全量 992（本卡 +9）与 main 基线 983 两轮对照——失败集合（`OrgDataScopeResolverTest` 2F + `SmsCodeServiceImplAttemptLimitTest` 3E）在三次全量间随机漂移且 main 基线同源失败、三个失败类 targeted 单跑 72/72 全绿 → 既有隔离债非本卡回归（`outputs/_client002b-c5-flake-isolation.log`）。

## CodeReview 独立复审发现（R1 → 修复 → R2 PASSED，0×P0/P1）

| # | 严重度 | 位置 | 发现 | 处置 |
|---|---|---|---|---|
| P0-1 | **P0** | `api-inventory-baseline.txt` / `接口清单与匿名白名单.md` | 新增端点 `GET /admin-api/system/cross-org-visit/my-targets` 未同步接口清单基线，漂移门禁 `ApiInventoryTest` 实测失败（reviewer 自行复跑证实） | **修复**：`-Dapi.inventory.update=true` 重新生成基线（350→351 端点，净 +1 行登记 `GET|/admin-api/system/cross-org-visit/my-targets|ADMIN|AUTHENTICATED|-|system|CrossOrgVisitController`）；清单文档 §3.1/§3.3/§8/§9 统计全量对齐基线事实（四维分布 + 主体类型 + `@PreAuthorize` 差 7 处注释逐列）+ 加注历史缺口（2026-09-09 建立后中间各卡漂移未回写文档，本次一并对齐 328→351）；`ApiInventoryTest` 复跑 4/4 绿 |

R1 对八大维度（服务端四要素对齐、Long 精度、access.ts 五级判定、tenant-visit 原子回滚、picker/user 页接线、测试覆盖、工程规范）逐项确认通过——**除 P0 外 0×P1/P2/P3**。

R2 独立复审确认 P0 修复闭合：基线 diff 文件级核验（354 行 = 3 头部 + 351 数据、重复行 0、字段数异常 0、排序位次正确）、文档四维重算一致、§3.3 七处注释位置 100% 命中、`@PermitAll` 三向闭合（25 裸 grep = 25 基线 = 25 目录）、门禁证据时间戳与基线 mtime 秒级吻合、R2 自行复跑 `ApiInventoryTest` 4/4 绿（`outputs/_r2-verify-apiinventory.log`）、时序无缝隙（无晚于校验时点的 .java 变更）、门禁纯校验模式未污染工作树。**PASSED / 0×P0 / 0×P1**。

## 验证（交付时点证据）

- Java targeted：**33/33** BUILD SUCCESS（`CrossOrgVisitServiceImplTest` 32 + `AuthConvertTest` 1）；
- miniapp targeted：**79/79**（router-access 63 + tenant-visit 16）；miniapp 全量：**147/147**（7 文件）；
- module-system 全量：992（本卡 +9）与 main 基线 983 失败清单随机漂移对照 → 既有隔离债非本卡回归；三个失败类 targeted 单跑 72/72 全绿；
- 漂移门禁 `ApiInventoryTest`：P0 修复后复跑 4/4 绿（351 端点）；
- 两轮 CodeReview 独立复审替代（R2 PASSED / 0×P0/P1）。

## 卡片状态与统计

主卡 ZS-CLIENT-002 维持**开发中**（.A 移动导航注册表 + .B 获批业务组织导航均交付；循子项拆分卡先例 SEC-001/SEC-011/DB-019/PERM-001/PERM-002/PERM-004/FILE-001/MSG-003「.A+.B 均交付、待验收须真实环境放行」+ §16.1「不能把技术夹具算业务策略完成」，须 B08 批次真实环境放行 + D-09 后验收才转待验收）。第 2 节统计不变（2/20/3/0/3/63，合计 91，权威来源 `node scripts/gov/task-stats.mjs`）。**本记录不表示任何主任务验收状态变化。**

## 边界（登记）

- **身份切换仅按 D-09**（§16.1）——本卡交付单向访问态切换（获批目标导航 + 原子回滚），多身份/双向切换随 D-09 决策后另立；
- **org 轴 SQL 列表静默过滤规则 `OrgDataPermissionRule` 注册仍未落地**——本卡为客户端导航消费端收敛（消费服务端 my-targets 授权结论、不在前端重算业务权限），自动规则注册随后续卡；
- **客户端只做权限集合成员判断**：服务端 `@PreAuthorize` / `authorizeCrossOrgVisit` 为最终边界，前端判定仅为导航展示收敛（未获批/记录失效 fail-closed 空列表 → 无切换入口）；
- **真实 PG + HTTP 端到端跨组织导航联验**随 B08 批次真实环境放行——本卡以 H2 targeted 套件 + miniapp 单测坐实数据源过滤与切换语义；
- 客户端导航接入行详见 [跨组织访问授权矩阵.md](../../services/zhongshu-core/docs/跨组织访问授权矩阵.md) §5/§8。
