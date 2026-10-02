# 综合风险排查报告（2026-10-02，FC-003 收口后全量体检）

> 范围：启用模块（system/infra/bpm/firstchain）+ 框架 starters + 门禁脚本 + 配置面；方法为「登记项清点 × 代码扫描 × 事件/方言交叉核对 × 本会话新增件自审」。
> 结论：**发现并修复 1 项 P1 运营风险；登记 4 项 P2/P3 风险与限制；确认 6 个面无风险**。明细如下。

## 1. 本轮发现并已修复

### R1（P1，已修复）：记录型 Outbox 事件无 Sink → DEAD 积累 + 严重告警噪音

- **现象推演**：`NOTIFY_DISPATCHED`（每封站内信一条，MSG-001）与 `AUDIT_CLEANED`（审计保留期清理留痕，AUDIT-002）两类事件**无任何 Sink 声明**。JOB-002 合同语义为「无 Sink → 退避重试至 DEAD 不丢弃」；FC-003 起 `OutboxDispatchScheduler` 默认启用（5s 轮询）后，每封站内信将在 ~50s 内经 5 次退避进入 DEAD，污染人工处置台账并触发 `DEAD_EVENTS_PRESENT` 严重告警（探针逐 60s ERROR）。
- **根因**：两类事件是「记录型」（无消费语义：站内信已同步落库/清理动作已完成同事务留痕），而 JOB-002 的无-Sink 语义面向「有消费意图但消费失败」的事件——设计口径缺口，非编码缺陷。
- **修复**（本提交）：新增 `OutboxRecordEventSink`（system，@Component，循补偿类 Sink 同款注册）显式确认两类记录型事件为 DISPATCHED（info 留痕、幂等重投安全、不抢占有独立消费语义的事件类型）；单测 2 例（支持面/幂等确认）。未来若赋予消费语义（如移动端推送回执），以新 Sink 替换本条目。
- **验证**：OutboxRecordEventSinkTest 2/2 + Notify 回归（landing 11/LogicDelete 3）全绿。

## 2. 待处置风险与限制（登记在案，按等级排序；均为已知边界非新缺陷）

| # | 等级 | 风险/限制 | 现状与缓解 | 解锁条件 |
|---|---|---|---|---|
| R2 | P2 | **D-12 逐行裁决性能**：线索 page 对每行调用 `ObjectAuthorizationService.authorize`（对象维 + 7 动作权限判定 + 字段等级解析），页深/行数放大时开销线性增长 | 页大小默认 10、上限受 PaginationContract 约束，当前数据量下可用；裁决实时重算是 D-12 验收要求（不可缓存），如需优化应做「同 org 同上限批量裁决」专用路径而非引入缓存 | 出现实测性能诉求时立优化小卡（WP-19 实测回填窗口） |
| R3 | P2 | **强制首改缺失**：负责人/员工初始密码长期有效，存在账号安全窗口 | 密码一次性下发（不落审计/日志）+ 管理员重置通道（LOGIN-003 已验）；强制首改随 LOGIN 域首改合同排期 | LOGIN 域首改合同排期 |
| R4 | P2 | **多实例默认角色模板互斥为进程内 synchronized**：跨实例残余竞态为重复模板角色（无权限语义危害，编码取用以「查到即用」为准） | 单实例部署形态下无影响； uk 兜底防重 | 运维扩容决策后置（随 B11 容量批次） |
| R5 | P3 | **附件业务引用保护缺失**：资质附件被申请引用后仍可经文件管理删除（FILE-005.A 引用保护仅覆盖活跃交付会话）；写入侧校验已堵「引用时不存在/越权」（V2.13） | 引用后被删的极端路径为登记边界；如需强保护需文件引用登记表设计 | 需要时另立设计 |
| R6 | P3 | **B10 UAT 是缺陷发现期**：真实角色/真机走查（docs/11 脚本已备）大概率暴露页面一致性与交互类缺陷 | 属批次正常节奏；自动化面已全部收口，UAT 缺陷修复有完整门禁护栏 | 真机 + 真实账号到位 |

## 3. 本轮确认无风险的面（排查过、有证据）

| 面 | 结论与证据 |
|---|---|
| 方言一致性（双向） | ①基线 int2 表手写 SQL：全仓 `deleted = FALSE` 扫描对 system_* 表**零命中**（本轮复核）；②boolean 表 MP 路径：G8 第 6 项机械守护（按版本回放迁移取最终类型，DO 必须覆写），0 issue |
| Outbox 事件 × Sink 全覆盖 | 全量事件类型交叉核对：TOKEN_REVOCATION_COMPENSATION / CACHE_EVICTION_COMPENSATION / NOTIFY_CHANNEL_SEND / NOTIFY_TODO_TRANSITION 均有专属 Sink；记录型两类由 R1 修复补齐；无第三类缺口 |
| 冻结上游代码 | mp/mall/erp 等 12 个冻结模块 + `zszj-ui` 为源码迁入策略的**有意保留**（非重复代码），ModuleWhitelistTest + verify-module-whitelist 双门禁守护；TODO 扫描启用模块仅命中 1 处上游冻结件（justauth，不修改） |
| 生产配置面 | swagger/springdoc 默认 `enabled: false`（ZS-ENG-005）；mock-enable/harness 配置隔离在 harness profile；G9 秘密门禁持续在跑 |
| 模块门与授权锚点 | firstchain 全部 19 端点 PERMISSION 零匿名（ApiInventory 452）；新增权限串均有菜单锚点（V20261001.001/V20261002.001 种子 + 既有角色补绑），严格模式（权限无菜单即无权限）不破 |
| 事件接线同事务性 | 六节点接线全部在调用方业务事务内（@Transactional 边界 + Outbox MANDATORY 合同），Sink 投递由派发器统一事务包裹（infra 69/69）；待办键=appKey+流程实例，撤回重发产新待办无键漂移 |
| 门禁与登记台账一致性 | run-local-gates 计数断言 17 项已同步（0 fail）；reviews/README 待处置台账 GAP-4 已闭合；§16.2 可开发清单四项全部完成 |

## 4. 遗留开口总览（不变，全部外部依赖）

- **B10 真实角色 UAT 执行**（脚本已备 docs/11）——UAT 期为缺陷发现窗口（R6）；
- **DB-002 服务器终核**（PEND-004 第 2/5/6/7 项，待环境负责人；本地口径已核）；
- **BRAND-003.B Tier-2 真机走查**（差额清单 7 项已落卡）；
- **B11 三张 .C 卡**（CLIENT-005/OPS-002/FILE-005）+ **OPS-003**（待 D-10/D-11 决策，PEND-006/007）；
- **R2~R5**（本报告 §2）：择机项，不阻塞收口。
