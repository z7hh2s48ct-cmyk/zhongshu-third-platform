# ZS-LOGIN-003 codex 评审处置（r0→r4 五弧）

- 评审工具：codex（gpt-6-astra / xhigh，read-only sandbox）
- 评审对象：分支 `feat/login-003`（ZS-LOGIN-003 统一禁用/删除/改密后的会话失效）
- 结论：**r4 PASS**（收敛；P2 测试深化项登记延后）

## r0（commit a02f7a20）FAIL 1×P1

| 级别 | 发现 | 处置 |
|---|---|---|
| P1 | 批量删除补齐用户归属校验缺失，跨租户强制下线 | r0 修复 `e7d44c34`（validateUsersExists） |

## r1（commit e7d44c34）FAIL 3×P1 + 2×P2

| 级别 | 发现 | 处置 |
|---|---|---|
| P1 | Excel 覆盖导入（updateSupport=true）将 status 直接更新入账号却不撤销会话——导入禁用后旧 Access/Refresh/合成凭据仍可用 | r1 修复 `882e0ca5`：importUserList 覆盖导入置禁用时同事务 invalidateUserSessions |
| P1 | getAccessToken 缓存回填复活竞态：读旧 DB 快照 → 撤销提交删缓存 → 旧快照回填 Redis，后续命中不再校验用户状态 | r1 修复：撤销墓碑（RedisDAO `markRevoked` SETNX + `set()` Lua「查墓碑+写缓存」原子门闩），覆盖访问令牌串与充当合成凭据 key 的刷新令牌串，TTL=凭据剩余有效期自清理 |
| P1 | 撤销遗漏未消费 OAuth2 授权码——禁用/改密前签发的 code 有效期内仍可兑换出新会话 | r1 修复：用户级撤销一并 `deleteByUserIdAndUserType` + 兑换时校验 ADMIN 账号状态 |
| P2 | 批量删除未接 @LogRecord、审计无操作者；logback 默认输出不含 trace-id | **延后登记**（见下） |
| P2 | 撤销成功审计在外层事务提交前写入，批量回滚后日志仍宣称成功 | **延后登记**（见下） |

## r2（commit 882e0ca5）FAIL 4×P1

| 级别 | 发现 | 处置 |
|---|---|---|
| P1 | 授权码删除被「空会话 early-return」跳过（取得 code 后退出登录再改密 → 无 Access/Refresh 记录 → code 残留可兑换） | r2 修复 `1a06f533` 前置提交：code 失效移到 early-return 之前，无会话也执行 |
| P1 | 兑换与撤销无共同事务/用户行锁：兑换先消费 code 读到启用态，撤销随后提交，兑换仍建新会话 | r2 修复：统一锁序——撤销与兑换都先 `adminUserMapper.selectByIdForUpdate` 用户行锁（仅 ADMIN），兑换加 @Transactional，锁内重读状态/消费 code/建令牌；与账号状态 UPDATE 隐式行锁互斥 |
| P1 | refreshAccessToken 淘汰旧代际访问令牌不落墓碑：「读 A 快照 → 刷新删 A 建 B → 撤销 B/R → A 快照回填」复活 A | r2 修复：刷新路径逐个 `revokeWithTombstone`（墓碑 + 删缓存） |
| P1 | 导入撤销决策依赖未加锁读的 existUser.status：「导入读旧快照(禁用) → 他人启用并登录 → 导入写禁用但跳过撤销」 | r2 修复：写禁用状态即撤销（幂等，移除旧状态条件） |

## r3（commit 1a06f533 前置提交）FAIL 2×P1 + 1×P2

| 级别 | 发现 | 处置 |
|---|---|---|
| P1 | 兑换先消费 code（持 code 行锁）再取用户行锁，与「撤销（持用户行锁）删同一 code」构成锁环 → 禁用/改密事务可能回滚 | r3 修复 `1a06f533`：先【只读】selectByCode 确定归属 → client/redirect/state 校验 → 用户行锁 → 锁内条件消费 → 建令牌；锁序固定「用户→code→令牌」无环 |
| P1 | 消费非条件删除：撤销删 code 提交后，兑换 deleteById 影响 0 行仍返回旧 codeDO 继续建令牌；同一 code 可并发重复兑换 | r3 修复：`consumeAuthorizationCode` 校验 deleteById 恰 1 行否则 OAUTH2_CODE_NOT_EXISTS，+2 H2 用例（双消费第二次拒绝、生命周期撤销后消费拒绝） |
| P2 | 双连接真实事务交错测试缺失（兑换↔改密两序、同 code 并发消费、建令牌失败回滚、MEMBER 兑换边界） | **延后登记**（见下） |

## r4（commit 1a06f533）PASS

评审自核锁序完备性、MEMBER 边界、事务与行锁语义、测试有效性，结论 PASS。

## 验证

- OAuth2 相关 11 个测试类 + AdminUserServiceImplSessionInvalidateTest：**83 tests / 0 failures，BUILD SUCCESS**（outputs/login003-r3-green.log）
- 新增用例：导入禁用撤销 ×2、墓碑阻塞回填 ×2、锁序 InOrder（用户锁先于刷新锁）×1、空会话仍失效 code、授权码条件消费 ×2、禁用/删除账号兑换拒绝 ×2
- 合并：`--no-ff` 合并回 main（见 docs/05 V1.31 记录）

## 延后/边界（非阻塞，登记后续处置）

1. **双连接真实事务交错测试**（兑换↔改密两序、同 code 并发消费、建令牌失败消费回滚、MEMBER 兑换不访问 ADMIN 用户表）——需独立 Redis 夹具 + 双连接控制，归 ZS-LOGIN-005.B（B05 可靠事件补偿与一致性恢复）或真实 PG 并发回归（ZS-SYS-001.A）批次；当前以「条件删除恰 1 行 + 用户行锁互斥」的确定性单测覆盖核心不变量
2. **审计时序**：撤销成功审计在事务提交前写入、批量回滚后日志虚报成功；批量删除 deleteUserList 缺 @LogRecord（无操作者）——归操作日志批次统一收口（与 IAM-003 延后 Minor ④ 同类）
3. **MEMBER（会员）账号兑换状态校验**归会员模块任务；本卡按「技术账号先闭环」边界仅覆盖 ADMIN
4. 任职撤销/业务组织语义归 B07（D-09 后），本卡不预埋
