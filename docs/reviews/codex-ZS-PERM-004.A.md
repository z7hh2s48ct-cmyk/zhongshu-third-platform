# ZS-PERM-004.A codex 评审处置（r0→r3 四弧）

- 评审工具：codex（gpt-6-astra / xhigh、high，read-only sandbox）
- 评审对象：分支 `feat/perm-004-a`（ZS-PERM-004.A 角色/菜单缓存和停用撤权的技术一致性）
- 结论：**r3 PASS / 0 发现**（评审收敛）

## 交付内容（impl commit f9e4a6a0 + r0/r1/r2 修复）

1. **deleteRoleList 补 @CacheEvict(ROLE, allEntries)**：批量删除角色此前不清缓存，陈旧角色可继续取权（TDD RED 实证）。
2. **assignRoleMenu / assignUserRole：@DSTransactional → @Transactional**：DS 本地事务不激活 Spring 事务同步、与缓存驱逐时序脱钩；system 模块单库，统一 Spring 事务生命周期。
3. **租户/套餐调用链统一**（r0 P1 采纳）：TenantServiceImpl `updateTenant`/`createTenant`/`updateTenantRoleMenu`、TenantPackageServiceImpl `updateTenantPackage` 外层 @DSTransactional → @Transactional——外层 DS 事务下 ConnectionProxy.commit() 为空操作，内层 Spring 事务驱逐缓存而 DB 等外层才提交，留有「他连接回填旧授权、外层提交后无最终驱逐」窗口。
4. **驱逐失败重试与证据下沉 Cache 层**（r1/r2 两轮修正）：新增 `RetryCacheErrorHandler`（CachingConfigurer.errorHandler 注册）+ `RetryEvictCache` 装饰器，`TimeoutRedisCacheManager.decorateCache` 在【事务装饰器内侧】包装（最终链 TransactionAwareCacheDecorator → RetryEvictCache → RedisCache）——覆盖「事务感知 afterCommit 绕过 errorHandler」路径；有界重试恰 2 次直接打 delegate（r2 P1 修正无界递归）；失败输出 ERROR 结构化证据（键 + 风险 + 修复动作），可靠重放补偿归 ZS-LOGIN-005.B。
5. **5 个缓存一致性契约测试**（真实 H2 + 内嵌 Redis + 真实 Spring Cache）：跨连接并发回填自愈、回滚不误清（native cache 观察）、停用角色后 hasAnyPermissions 立即失效、撤销授权立即失效、批量删除驱逐（RED→GREEN）。

## 评审弧

| 轮次 | 结论 | 要点 |
|---|---|---|
| r0 | FAIL 3×P1+2×P2 | P1 ignore-caches 增项错误（role/menu_role_ids 底层 SQL 有租户过滤，裸键共享跨租户串数据——本方假设被推翻，增项完全回滚）；P1 租户/套餐外层 @DSTransactional 空提交窗口（采纳统一 @Transactional）；P1 撤权驱逐失败无补偿（采纳 RetryCacheErrorHandler + 登记版本机制延后）；P2 测试并发顺序不真实（如实登记残余窗口） |
| r1 | FAIL 2×P1+P3 | P1 createTenant 替换注解时遗漏 @Transactional（补回）；P1 TransactionAwareCacheDecorator afterCommit 绕过 errorHandler（重试下沉 Cache 层）；P3 重试计数 off-by-one（恰 2 次） |
| r2 | FAIL 2×P1 | P1 重试包装在事务装饰器外侧未覆盖 afterCommit（改 decorateCache 内层）；P1 处理器回调 wrapper 无界递归（改 delegate 有界循环） |
| r3 | **PASS / 0 发现** | — |

## 验证

- TDD：deleteRoleList 驱逐缺失 RED（缓存陈旧条目命中）→ 修复后 GREEN。
- 回归：PermissionCacheConsistencyTest 5 + RoleServiceImplTest + PermissionServiceTest + MenuServiceImplTest + TenantServiceImplTest + TenantPackageServiceImplTest = **115 tests / 0 failures，BUILD SUCCESS**。
- 合并：`--no-ff` 合并回 main（见 docs/05 V1.33 记录）。

## 延后/边界（非阻塞）

1. **权限版本校验 / 防旧值写回**：「他连接读库完成→缓存写入」间被屏障暂停、写事务提交并驱逐后才写回旧值的「晚到回填」残余窗口，以及驱逐彻底失败（重试耗尽）的可靠补偿——需版本机制或失败键清单持久化，归 ZS-LOGIN-005.B（B05 JOB-002）或独立小卡。
2. **真实多节点联验**：本批以事务同步语义 + 真实 Redis 单测等价证明；多节点缓存广播行为归环境批次。
3. **撤权审计字段**（操作者/目标/变更/结果）→ ZS-PERM-004.B（B08，前置 IAM-004）。
