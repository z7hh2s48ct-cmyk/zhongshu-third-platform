# ZS-PERM-004.C 评审处置（机制修复卡：CodeReview 独立复审 R1 FAIL P0=1/P1=1/P2=1/P3=3 → 全部闭环 → R2 PASSED 0×P0/P1 + 3×P3 全闭环）

- 评审对象：ZS-PERM-004.C 权限缓存驱逐失败可靠补偿与防旧值写回双机制（B08 Wave3；§16.1 子项，前置 ZS-PERM-004.A〔RetryEvictCache/重试语义基线〕+ ZS-PERM-004.B〔撤权审计，边界移交「驱逐失败可靠补偿归 ZS-PERM-004.C」〕+ ZS-JOB-002〔outbox 基建〕均已交付；承 reviews/README.md 移交登记 `46fd2370` M1〔LOGIN005B-M1〕与 codex-ZS-PERM-004.A 延后 1）
- 隔离分支：worktree `.wt/zszj-wt-perm-004-c` 分支 `feat/perm-004-c`（自 main `10e76791`；impl `2577c991`〔22 files〕→ CodeReview R1 闭环 `80225720`〔8 files +226/-19〕→ R2 闭环 `38ec92e4`〔5 files +183/-18〕）
- 合并：`--no-ff` main `af759827`（净 22 files +2105/-23；feat 分支自 `10e76791` 线性领先，merge-base `10e76791` 干净合并）
- 卡片性质：**产线机制修复卡**——闭环「撤权提交后缓存删除全失败时旧授权残留」（M1）与「驱逐与加载交错时旧值晚到写回」（PERM-004.A 延后 1）两个子问题
- 评审工具：codex 账号用量上限阻断（承 PERM-002.B/PERM-004.B/LOGIN-003.B/SEC-001.B/PERM-001.B/FILE-001.B/MSG-003.C/CLIENT-002.B/BPM-002/BPM-004 先例），改以 **CodeReview 独立复审替代**（R1 → 修复 → R2 两轮）
- 结论：**R1 FAIL（P0=1 / P1=1 / P2=1 / P3=3）→ 全部闭环 `80225720` → R2 独立复审 PASSED（0×P0 / 0×P1）+ 3×P3 建议全部闭环 `38ec92e4`**（评审门实质闭环）
- 原始日志：`outputs/_perm004c-*.log`（本地未入库，gitignore；本档案为处置入库）

## 交付内容（impl `2577c991` + R1 闭环 `80225720` + R2 闭环 `38ec92e4`）

**机制①失败键清单持久化**（框架层 SPI + 应用层复用 outbox）：框架层新增 `CacheEvictionFailureRecorder` / `CacheEvictionOperation` / `CacheEvictionReplayContext` SPI；`RetryEvictCache.boundedRetry` 重试耗尽触发 `recordFailure`（replay scope 内不记录，杜绝「记录→重放→再记录」自激）；应用层 `OutboxCacheEvictionFailureRecorder` 预写 outbox 事件（`CACHE_EVICTION_COMPENSATION`，5s 去重窗口 + 事务内 append〔MANDATORY〕+ 租户缺失拒绝伪造 + 降级释放窗口双护栏）；`CacheEvictionCompensationSink` 随 dispatcher 幂等重放驱逐，重放失败异常上抛（退避/超限转 DEAD 兜底归 JOB-004 人工台账）。

**机制②版本校验防旧值写回**：新增 `CacheVersionGuard`（Redis 共享版本键 INCR）；`RetryEvictCache` get miss 时快照版本、put 时对比不一致即丢弃（一次性消费；版本读取失败以哨兵保守丢弃）；`evict`/`clear` 受管 bump 版本，阻断「驱逐期间读旧值 → 驱逐后晚到写回」窗口。

**装配与白名单**：`ZszjCacheProperties` 新增 `consistency-guarded-cache-names`（默认空 = 框架行为与现状完全一致）；`ZszjCacheAutoConfiguration` 白名单非空才注入；`application.yaml` 登记 4 个权限缓存（`role` / `user_role_ids` / `menu_role_ids` / `permission_menu_ids`）。

**过时文本更新**：`RetryCacheErrorHandler` 3 处「可靠重放归 ZS-LOGIN-005.B」订正为本卡交付；`PermissionCacheConsistencyTest` 注释同步。

## CodeReview R1 findings（FAIL，P0=1/P1=1/P2=1/P3=3）全部闭环（`80225720`）

| 级别 | 编号 | 缺陷 | 闭环（`80225720`） |
|---|---|---|---|
| P0 | P0-1 | 重放失败静默吞：`RetryEvictCache` 重试耗尽分支在 replay scope 内不 rethrow，Sink 重放失败被 dispatcher 误标 DISPATCHED、补偿静默丢失（核心合同不成立） | replay scope 内 rethrow（业务路径 afterCommit 驱逐维持吞异常 + 证据化契约不变）；探针 RED 直证（Sink 用例 22 重放异常未抛） |
| P1 | P1-1 | `OutboxCacheEvictionFailureRecorder` 事务传播不足：afterCommit 主路径下 ConnectionHolder 仍绑定且 `isTransactionActive()==true`，REQUIRED 静默并入已提交事务（参与者无显式 commit，落库只剩连接归还副作用兜底） | 显式 `REQUIRES_NEW` 自开独立短事务；反射锁定测试 + 探针 RED（Recorder 用例 6） |
| P2 | P2-1 | 去重台账无清扫：随时间无界增长 | `record` 入口惰性清扫超窗条目 + 反射测试 2 例 |
| P3 | P3-1 | 快照残留边界未登记（加载中止残留不作 evict/clear 清理的取舍） | javadoc 登记过度保守方向与上界（清理会破坏「驱逐后晚到写回拦截」语义，刻意保留） |
| P3 | P3-2 | 版本键值损坏时 INCR 报错使 bump 在 delegate.evict 之前抛错、受管缓存驱逐永久失效 | 遇 INCR 报错 DEL 损坏键后重试一次 INCR 自愈（R2 F2 进一步精确化为探测式） |
| P3 | P3-3 | ERROR 文案过度宣称「可靠重放补偿已交付」 | 订正为依赖调度接线（D-07）（R2 F1 进一步去除无条件断言） |

## CodeReview R2 findings（PASSED，0×P0/0×P1 + 3×P3 建议）全部闭环（`38ec92e4`）

| 级别 | 编号 | 建议（非阻塞） | 闭环（`38ec92e4`） |
|---|---|---|---|
| P3 | F1 | `boundedRetry` ERROR 文案仍无条件断言「失败键清单已持久化」（该日志先于 record 输出，且非受管缓存 / evictIfPresent / recorder 降级路径下为假） | 改为「见紧随其后的 record 日志（可能降级为仅日志）」 |
| P3 | F2 | 版本键自愈捕获面过宽（catch RuntimeException 含超时/连接类瞬时故障）会误 DEL 正常版本键、破坏「版本只增不减」单调性；无日志；`ex` 未用（javadoc「原样上抛」不符） | 重构为**探测式自愈**：仅 `isValueCorrupted(key)` 确认（GET 存在且非数值）才 DEL + 重试 INCR；瞬态/探测失败原样上抛；补 WARN 日志；`addSuppressed(incrEx)` 保留根因；javadoc 全部订正；新增用例 6/7 看守 |
| P3 | F3a | Recorder 类 javadoc「写入随连接归还丢失」与证据不符（H2/Druid 归还兜底）；用例 30 无判别力 | javadoc 订正为「参与者无显式 commit，落库仅依赖池/驱动的连接归还副作用兜底（H2/Druid 下为 setAutoCommit(true) 隐式提交，非合同保证）」 |
| P3 | F3b | REQUIRES_NEW 仅反射白色盒锁定，建议补行为层判别 | 集成用例 32 `TransactionCountingManager`（数 `status.isNewTransaction()`）断言 afterCommit 窗口内 record 必须额外开启 1 个独立事务 |

**判别口径纠偏（探针实测坐实，随 F3b 闭环）**：首版用例 32 以 commit 调用计数判别，探针下 REQUIRED 仍绿——根因：`TransactionTemplate` 对参与式事务同样调用 `commit(status)`（`processCommit` 对非新事务为 no-op 不提交），两种传播 commit 计数均为 2，**commit 调用计数无判别力**；改为「新开启事务计数」（`status.isNewTransaction()`：REQUIRES_NEW=2 外层+记录，REQUIRED=1）；探针重跑 RED（`expected:<2> but was:<1>`）判别成立；该口径说明已登记入用例 32 javadoc + 提交信息。

## 验证（交付时点证据）

- **探针 RED 双证**（临时撤销修复 → 定向测试精准 RED → 手工恢复 → Grep 无残留 → 复跑 GREEN）：
  - R1 三证：Sink 用例 22（重放异常未抛）/ Recorder 用例 6（REQUIRES_NEW 反射锁定）/ 集成用例 31（第 2 轮领取 0 条——事件被误标 DISPATCHED）；
  - R2 双证：框架层 3 红（命令序列缺探测 get / 瞬时故障仍 DEL / 不可判定未原样上抛）+ 集成用例 32 `expected:<2> but was:<1>`（REQUIRED 静默并入 → 无独立新事务）；
- 定向测试全绿：框架层 **19/19**（`CacheVersionGuardTest` 7 + `RetryEvictCacheTest` 12）+ 应用层定向 **22/22**（静扫 1 + Sink 6 + Recorder 7 + 集成 8）；
- 模块回归：starter-redis **15/15**；module-system 全量 **1013**（5F+4E 三类：OrgDataScope/PermissionService/Sms，隔离复跑 **72/72** 全绿 + 失败集合跨运行漂移 + 三先例同源家族，判定既有 flake〔surefire 单 JVM 跨类状态污染〕、非本卡回归）；module-infra **506/0F0E**；
- `node scripts/db/run-pg-regression.mjs` **19/19**（零迁移零脚本变更，保持）；
- `node scripts/ops/run-local-gates.mjs --fast` **10/10**（worktree 侧；主树合并后复跑同）。

## 卡片状态与统计

主卡 ZS-PERM-004 维持**开发中**（循子项拆分卡先例「子项交付、待验收须真实环境放行」）。理由：①ZS-PERM-004.A 延后的驱逐失败可靠补偿已由本卡交付（理由①解除）；②主卡完整验收（撤权后旧 Token/旧菜单/缓存不继续执行受限动作、多节点缓存广播、审计端到端）须 B08 批次真实环境放行，仍挂账。第 2 节统计不变（0/20/2/0/3/66，合计 91）。**本记录不表示任何主任务已验收。**

## 边界（登记）

- **版本机制理论残余窗口**：verify（GET 版本）与 `delegate.put` 之间的亚毫秒间隙内并发驱逐仍可致旧值一次存活（彻底原子化需 Lua 侵入 RedisCache 写入语义，不做）；相对修复前窗口（跨 DB 查询毫秒级、可被屏障拉长至秒级）收窄 3~4 个数量级，**不承诺零窗口**；
- **`get(key, Callable)`（sync=true）/ `putIfAbsent` 路径不做版本校验**：当前仓库零使用（静态搜索证据）；若未来引入需扩展本机制（登记防漂移）；
- **重放驱逐的时序语义**：重放可能清除重放时点之后新写入的条目（毫秒级误清）→ 回源重建自愈，性能抖动非一致性缺口（对齐 PERM-004.A「权限链路宁可多清不留旧」）；
- **补偿链对 Redis 持续不可用的兜底依赖 JOB-004 DEAD 台账人工恢复路径**（循 LOGIN-005.B 边界 4）；
- **多节点/集群 Redis 语义联验归环境批次**：本卡以 H2+内嵌 Redis 单机语义等价论证；版本键存 Redis 共享的设计对多节点天然成立（任意节点 bump 全局可见）；
- **真实 PG 层**：零迁移零表结构变更；`outbox_event` 的 PG 语义由 JOB-002/JOB-004 既有套件持续保障；`dispatchOnce` 生产调度接线归 D-07（本卡以测试内手动 `dispatchOnce` 证明 Sink 语义正确）；
- **租户上下文缺失时记录降级**：不阻断、不伪造归属（循 `JdbcReliableEventPort` 合同）；该场景补偿证据丢失，仅 ERROR 留痕；
- **框架层对非白名单缓存零变化**：默认配置（白名单空）= 裸框架现状行为；
- **`OAuth2TokenServiceImpl` 同类过时文本**（L828/L840 附近「归 ZS-LOGIN-005.B」）属登录域留痕，不在本卡更新。
