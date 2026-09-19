# codex 评审处置：ZS-LOGIN-001.B 短时握手票据端点与两端 WS 迁移

- 评审对象：`feat/login-001-b`（worktree `.wt/zszj-wt-login-001-b`），ws-ticket 端点/DAO/Service/握手拦截器/starter 透传 + 两端 6 处 WS 迁移 + gate 翻转 + 测试 3 类 17 用例
- 评审方式：codex exec（gpt-6-astra / model_reasoning_effort=xhigh / bypass sandbox），r0→r5 共六弧；弧内携带真实回环 TCP/WebSocket 实证、Hutool/VueUse 依赖源码与字节码核对、内存时序验证
- 收敛结论：**r5 PASS/0×P0~P3 全零**（r3/r4 各余 1×P2 均已处置并经下一弧同场景真实 TCP 验证）；HEAD 前后校验一致
- 合并：`--no-ff` main `e787ecb7`（分支提交链 `172cf72d` impl → `c43aefb5` r0 → `1ecb19ed` r1 → `7444f4fb` r2 → `ebc4784a` r3 → `6dfe7298` r4）

## 弧次与发现

| 弧 | 对应处置 | 发现 | 要点 |
|---|---|---|---|
| r0 | `c43aefb5` | 4×P1+4×P2+1×P3 | P1 票据随机源 fastSimpleUUID=ThreadLocalRandom 非密码学安全（字节码核对）→ SecureRandom 32 字节；P1 票据脱离会话撤销/过期 → 签发绑定当前访问令牌+消费时 checkAccessToken DB 权威复核；P1 取票悬挂期间 disconnect 后旧身份连接复活 → 连接代际守卫（三处前端）；P1 miniapp IM 登出后 manualClosed 永久封锁再连接 → 显式 connect 复位；P2 身份去重丢失（凭据替换不生效）→ connectionIdentity；P2 客服页断线无恢复 → 退避换票重连链；P2 VueUse autoConnect 双建连双消费票据 → immediate/autoConnect:false；P2 端点无限流 → 见 r1 订正；P3 注释误述「票据非凭据」→ 订正为短时 bearer 凭据四层收敛 |
| r1 | `1ecb19ed` | 1×P1+6×P2+1×P3 | P1 客服页 watch 引用未定义 getIsOpen（初始化即 ReferenceError）→ 删除；P2 miniapp IM 二次复检缺身份条件（凭据替换时新票据被丢弃、旧连接不关）→ 补 connectionIdentity；P2 admin IM 代际捕获早于旧连接清理（disconnect 自作废本次建连）→ 移到清理后；P2 客服页 wsDisposed 复检在 open() 之后 → 前移；P2 心跳超时清空 ws 但 status 停留 OPEN（重连盲区）→ ws 句柄 watcher；P2 配额 INCR/EXPIRE 非原子（EXPIRE 失败留永久键永久拒绝）→ Lua 原子+TTL 自愈；P2 @RateLimiter 键混入请求对象（UserRateLimiterKeyResolver 拼 HttpServletRequest、StrUtils 前缀排除不覆盖 security wrapper）→ 撤销端点注解，签发频次由 DAO 按用户配额承担；P3 配额口径正名 |
| r2 | `7444f4fb` | 1×P1+1×P2+1×P3 | P1 重连守卫误拦两种断开形态（VueUse 普通断线保留旧 ws 引用≠null、心跳超时清空 ws 但 status=OPEN，守卫两侧全拦）→ 连接中判定改实例实际 readyState===OPEN；P2 wsDisposed 复检位于 open() 之后（晚到票据仍建连）→ 前移到改地址/open 之前；P3 javadoc 失效 {@value} 引用与口径措辞统一 |
| r3 | `ebc4784a` | 1×P2 | 旧实例迟到 close 无条件清 VueUse 共享心跳定时器（新连接被停心跳、半开无法自愈；真实回环 TCP 实证新连 3.09s/旧 close 4.59s/心跳 1→0）→ 建连后隔离旧实例回调 |
| r4 | `6dfe7298` | 1×P2 | 心跳超时路径 VueUse 先清空 ws.value——connectWithTicket 建连时 previousWs 已 undefined、r3 隔离被跳过 → ws watcher 增加 previousInstance 旧值参数就地解绑（与建连后隔离幂等并存） |
| r5 | —（终弧） | **0×P0~P3 全零** | r4 修复经同场景真实 TCP 验证（A 心跳超时→B 打开→A 延迟断开，B 心跳保持）；七项定向核对无新增问题；17/17 后端用例维持 |

## 验证

- 后端定向 17/17（DAO 6 / 拦截器 5 / Service 6，含撤销拒绝/用户不匹配/配额上限/密码学随机唯一性）
- system 全量 -am 845 用例（仅 5 处既有顺序依赖基线失败，隔离复跑 29/29——与主树基线一致）
- admin-web ts:check 11 错误=上游基线不变（0 新增）；miniapp type-check 0 错误
- ApiInventory 基线重生成（端点 349→350：POST /admin-api/system/auth/ws-ticket）
- 合并后主树 fast 门禁 / PG 回归 / 全量复验按批次收口执行

## 登记边界

1. 消费复核与建连之间的在途撤销窗口按 .A 鉴权时点语义接受（不提供「撤销返回后绝无新连接」保证，也不提供已建连接即时踢下线）。
2. jmreport 两处 iframe `?token=<refreshToken>` 为报表引擎自有认证域，非 WS 握手链，不在本卡范围——作为观察项登记（如需收敛另立小卡）。
3. 票据 TTL（60s）与签发配额（60s ≤10 次）为保守占位值，随 B06 真实环境联调可调（zszj.security.ws-ticket.ttl-seconds）。
