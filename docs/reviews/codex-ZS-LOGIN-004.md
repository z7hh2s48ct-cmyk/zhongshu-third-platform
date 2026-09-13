# ZS-LOGIN-004 codex 评审处置（r0→r4 五弧）

- 评审工具：codex（gpt-6-astra / xhigh，read-only sandbox）
- 评审对象：分支 `feat/login-002`（ZS-LOGIN-004 隔离演示登录配置并保证验证码一次性消费；worktree `E:/zszj-wt-login-002`，与 ZS-LOGIN-002 同分支串行、分别评审）
- 结论：**r4 PASS / 0 发现**（评审收敛）
- 证据说明：本评审的原始 stdout 未随当次会话保留于 `outputs/`（暂存目录清理）；r2（WRONGTYPE 迁移）与 r3（滚动部署旧桶）发现及 r4 最终结论自 codex 会话日志（`~/.codex/sessions/2026/09/13/rollout-2026-09-13T03-03-54-*`、`rollout-2026-09-13T06-59-03-*`、`rollout-2026-09-13T09-00-32-*.jsonl`）恢复，r0/r1 发现以修复提交 `10fb7e92`/`11820138` 提交说明为准，如实登记。

## r0（commit 517863ee，impl）FAIL 1×P1 + 3×P2

| 级别 | 发现 | 处置 |
|---|---|---|
| P1 | SMS 发送配额 Key 的 IP 取自 `X-Forwarded-For`（客户端可伪造），轮换 XFF 即绕过同 IP 频控（同 IP 轮换手机号喷洒缺口重新打开） | r0 修复 `10fb7e92`：配额 IP 改用不可伪造的连接对端地址（remoteAddr），防 XFF 伪造 |
| P2 | 校验尝试计数与 IP 发送配额为「读-判-写」非原子，并发下可超限 | r0 修复：尝试/IP 发送配额改**原子预留**语义 |
| P2 | 计数器 INCR 与 EXPIRE 两条命令分离，INCR 后进程崩溃留下无 TTL 永久计数 | r0 修复：计数器 INCR/EXPIRE 原子化（Lua） |

## r1（commit 10fb7e92）FAIL 1×P1 + 3×P2

| 级别 | 发现 | 处置 |
|---|---|---|
| P1 | IP 配额键以裸 IP 拼接，Redis Cluster 下不同 IP 落不同 hash slot，配额原子脚本 `CROSSSLOT` 报错（集群部署频控失效） | r1 修复 `11820138`：IP 配额键共享 **hash tag**（`{...}`）固定 slot |
| P2 | 预留成功后释放用了重算的新桶键，可释放错桶/把计数打成负数 | r1 修复：释放**原预留桶键**防错桶/负计数 |
| P2 | 尝试释放未绑定预留代际，过期后误放他人预留 | r1 修复：尝试释放绑定预留代际 |
| P2 | DB 查询异常路径不释放预留，合法请求被误占额度 | r1 修复：DB 查询异常释放预留 |

## r2（commit 11820138）FAIL 1×P1 + 1×P2

| 级别 | 发现 | 处置 |
|---|---|---|
| P1 | Redis 结构升级（string 计数→hash）无迁移策略：滚动部署/升级窗口内，旧版写入的 string 键被新版 `HGET` 直读抛 `WRONGTYPE`——**正确验证码也拒绝**，直至计数器过期（默认至多十分钟）；旧实例同样读不了新实例的 hash | r2 修复 `5692f8f4`：**惰性迁移**——命中旧 string 结构时保留计数与剩余 TTL 原地迁移为新结构，配回归测试覆盖「既有 string 计数器」 |
| P2 | 迁移未保留原 TTL（计数被重置为满窗口） | r2 修复：迁移保留计数与 TTL |

## r3（commit 5692f8f4）FAIL 1×P1 + 1×P2

| 级别 | 发现 | 处置 |
|---|---|---|
| P1 | 滚动部署混合版本窗口：新版请求无条件删除旧版（无 hash tag）桶键——已耗尽的 5 次配额桶被（哪怕是最终被拒的）新版请求删除，旧版实例又能放行 5 次，反复绕过无限喷洒 | r3 修复 `80845994`：**滚动部署保留旧桶**——旧桶保留至旧版写者排空，不无条件删除 |
| P2 | `PTTL` 返回 0/负值时按永久处理，把将过期锁升级为永久锁/永久桶 | r3 修复：PTTL≤0 一律视为已过期，杜绝永久锁/永久桶 |

## r4（commit 80845994）PASS / 0 发现

codex 复评结论（自会话日志恢复，findings=[]，overall_correctness="patch is correct"，confidence 0.9）：

> "No actionable regressions were found in the changed migration logic, its callers, or the updated tests. Tests were not executed in the read-only environment."

## 交付摘要（对应 docs/05 卡片）

- **验证码原子消费**：`useSmsCode` check-then-act 竞态 → 条件 UPDATE（`SmsCodeMapper.consumeById`，`WHERE used=false`），按影响行数判定。
- **尝试次数上限**：Redis 计数（手机号+场景），超阈值锁定期内直接拒绝（`SMS_CODE_EXCEED_ATTEMPT_LIMIT` 1_002_014_006）。
- **每 IP 发送频控**：Redis 小时/天滚动桶（`SMS_CODE_EXCEED_SEND_MAXIMUM_QUANTITY_PER_IP` 1_002_014_007），封堵同 IP 轮换手机号喷洒。
- **固定演示码隔离**：`application.yaml` `begin-code/end-code=9999` → 环境变量占位 + 安全默认（1000~9999），固定 9999 下沉 `application-local.yaml` 非生产 profile；`SmsCodePropertiesValidator` 启动期校验，非宽松 profile 取值域不安全即启动失败。
- **登录方式门控**：register/smsLogin/socialLogin/resetPassword/sendSmsCode 服务层 `zszj.security.login-mode.*` 门控（默认全关、生产模板显式声明），绕过 HTTP 直调同样被拒（`AUTH_LOGIN_MODE_DISABLED` 1_002_000_009）。
- **通道就绪**：`SmsSendServiceImpl.isTemplateSendable` 通道未就绪明确失败（`SMS_CODE_SEND_CHANNEL_NOT_READY` 1_002_014_008），不写库、不占配额、不假报已发送。

## 验证

- impl 时点：4 个新测试类 46 用例全绿 + 既有 `SmsCodeServiceImplTest`/`AdminAuthServiceImplTest`/LOGIN-002 三套回归不破；不调用任何真实短信通道（`SmsSendService` 始终 Mock）
- 评审弧累计新增 Redis 安全测试：`SmsCodeSecurityRedisDao*Test` 5 类（原子脚本/Cluster slot/尝试迁移/配额迁移/桶释放）+ `OAuth2AccessTokenRedisDaoTtlTest`
- 合并：`--no-ff` 合并回 main（`087be5f0`，与 ZS-LOGIN-002 同 merge）
- 文档同步复验（2026-09-13，本收口补丁）：当前 main 复跑 6 测试类 **56 用例 / 0 失败，BUILD SUCCESS**（`outputs/login002-004-docsync-test.log`：RefreshConcurrency 6 + AtomicConsume 5 + AttemptLimit 12 + PropertiesValidation 19 + DemoIsolation 10 + RedisDaoTtl 4）

## 延后/边界（非阻塞，登记后续处置）

1. 真实短信通道端到端（真实供应商回执/失败分支）按范围门禁归 B11/外部条件；本卡以「通道未就绪明确失败」语义收口。
2. 限流能力本身归 ZS-SEC-010（已交付，登录 5 端点复用）；本卡聚焦验证码/配额/门控。
3. Redis Cluster CROSSSLOT/迁移语义已在无集群本地 Redis 以键结构与 Lua 脚本等价验证；真集群滚动部署演练归环境批次（ZS-SEC-012.B/OPS-002）。
