# ZS-JOB-004 后续小卡 P2-1/P2-2 codex 评审处置（r0→r1 两弧）

- 评审工具：codex（gpt-6-astra / xhigh，`codex review --commit <SHA>`，**多 locale 编译后方法实测**：对 tr-TR/az-AZ/en-US/zh-CN 四 locale 驱动编译产物验证凭据词根过滤行为）
- 评审对象：分支 `feat/job-004-p2`（ZS-JOB-004 两张正式后续小卡合并交付：P2-1 脱敏异常名来源白名单收敛 + P2-2 并发恢复确定性锁阻塞观测测试）
- 结论：**r1 PASS / 0 发现**（两弧收敛：r0 1×P2〔locale 依赖绕过〕→ 处置 → r1 PASS「Locale.ROOT correctly makes credential-name filtering independent of the deployment locale…all 27 tests passed」）
- 说明：循 ZS-OPS-001.B/.C 先例两小卡同分支串行交付、合并评审（同为 JOB-004 登记的 P2 非阻塞小卡）；raw 输出留档会话日志。

## 交付内容（impl `58ff5dbd`，2 files +84/-4；r0 处置 `d4f9af6a`，2 files +29/-1；合并 `--no-ff` main `4643dde6`）

1. **P2-1 脱敏异常名凭据词根二次过滤**：`OutboxRecoveryServiceImpl.isControlledExceptionName` 增第四道门槛——全名逐段大小写折叠 `contains` 命中凭据词根集（`password/passwd/pwd/secret/credential/apikey/api_key/accesskey/access_key/secretkey/secret_key/privatekey/private_key`）即视为来源不可信，降级 `UNPARSEABLE_ERROR` 不回显——堵 codex r2 登记的「`password_real_secret_123Exception`（合法 Java 限定名 + Throwable 后缀）极窄来源信任残留」。词根集刻意**不收 `token`**（`Token*Exception` 真实异常族防误杀，用例 30 护栏）；残余风险（无法命中的伪装形态）与取舍在 JOB-004 开发记录登记。
2. **P2-1 配套测试（用例 28~31）**：28 凭据词根 simpleName 降级（TDD RED→GREEN）；29 包段词根降级；30 `TokenExpiredException` 不受影响（防过度拦截护栏）；31 **tr-TR locale 回归**（Locale.setDefault 驱动回查路径，锁定折叠与部署机 locale 无关）。
3. **P2-2 并发锁阻塞观测断言**：并发恢复用例 A 栅栏 `await(250ms)` 返回值不再忽略——新增断言 0b「await 必须超时返回 false」：修复实现（FOR UPDATE 行锁）下 B 被阻塞在载入处、不可能在 A 持锁期间到达计数栅栏；await 提前返回 true = B 在 A 提交前读到过期计数 = 行锁串行化被破坏（缺陷实现回归的直接信号，250ms 窗口 ≫ 缺陷路径毫秒级到达耗时）。

## 评审弧

| 轮次 | 结论 | 发现 → 处置 |
|---|---|---|
| r0（`--commit 58ff5dbd`） | 0×P0/P1，1×P2 | 26/26 测试过；但 codex 以 **tr-TR/az-AZ/en-US/zh-CN 四 locale 驱动编译后方法**实测：默认 locale 为土耳其语/阿塞拜疆语时 `toLowerCase()` 把 ASCII `I` 折成无点 `ı`，`APIKEY_real_123Exception`/`CREDENTIAL_real_123Exception`/`PRIVATEKEY_real_123Exception` 绕过过滤直显于回查与审计 → **修复**：折叠改 `toLowerCase(Locale.ROOT)`（凭据词根全为 ASCII，与部署机 locale 无关）+ 用例 31 locale 回归（`d4f9af6a`）。 |
| r1（`--commit d4f9af6a`） | **PASS / 0 发现** | 「Locale.ROOT correctly makes credential-name filtering independent of the deployment locale. The regression test exercises the Turkish-locale bypass and restores the original locale; all 27 tests passed.」 |

## 验证

- TDD：RED（用例 28/29 确认失败、30 与断言 0b 过）→ GREEN **27/27**；infra 全量 `-am` **455/0 BUILD SUCCESS**；`run-local-gates --fast` **10/10**。
- 教训登记：安全过滤/规范化逻辑凡涉大小写折叠必须 `Locale.ROOT`（或等价 locale 无关原语）——默认 locale 依赖在 tr-TR/az-AZ 部署机上直接构成绕过面；本条与 SEC-011.A「排除须按类型 instanceof 而非包名前缀」同属「过滤器自身实现细节即安全边界」系列。

## 登记边界

1. 词根过滤是「极窄信任残留」的收窄而非穷举——无法命中凭据词根的伪装形态（如非凭据词根的随机串）仍可能通过（与 JOB-004 r2 登记口径一致，残余风险已知且触发面极窄）。
2. `token` 词根刻意排除（真实异常族误杀取舍）；如未来出现 Token 形态泄露实例，再评估收词根并补护栏用例。
3. P2-2 断言依赖 250ms 窗口与缺陷路径毫秒级到达的比值——残余非确定性（极端调度饥饿下缺陷实现可能滑入良性路径）如实登记；断言 0b 在修复实现下为确定性恒真。
4. 主卡 ZS-JOB-004 维持「待验收」不变；两小卡随本卡后续批次一并放行。
