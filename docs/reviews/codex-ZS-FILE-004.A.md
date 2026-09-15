# codex 评审处置：ZS-FILE-004.A 主体绑定票据与鉴权取流协议

> 被审对象：worktree 隔离分支 `feat/file-004-a-005-a` 的渐进提交（impl + r0/r1/r2 三轮处置，共 4 提交），各弧以 `codex review --commit <SHA>` 评审；`--no-ff` 合并 main（`285ab19c`，B04 收口件）。
> **接管说明**：本任务原由其它工作区在制（`.wt/zszj-wt-file-004-a-005-a`，仅有 1 个未提交的 TDD 规格测试、零提交），经用户授权由本工作区接管；其 12 用例规格测试**原样采纳**为交付合同，实现完全按该规格 TDD 落地。
> 评审工具：`codex-cli 0.154.0`，模型 `gpt-6-astra`，reasoning effort `xhigh`，sandbox `read-only`，approval `never`。
> 完整 stdout：`outputs/file004a-codex-r{0,1,2}.log`（UTF-16，gitignored 未入库；结论逐字引用于本文）。

## 交付内容

- **一次性交付票据**（`infra_file_delivery_ticket`，迁移 `V20260914.012`，`@TenantIgnore` 全局表 + 显式 tenant 谓词）：签发仅存 SHA-256 散列（token 明文仅返回一次）；绑定主体/租户/用途/有效期 30 分钟（与 FILE-003 凭证合同对齐）。
- **原子兑换**：消费 CAS 带 owner·tenant·purpose·status·有效期谓词——补齐供体 `JdbcDeliveryPort` 消费 SQL 缺失的全部匹配谓词（卡片明确「不能原样当作完整授权」）；重复兑换同主体同登录会话幂等返回既有会话（断线重连不新建），换会话重放拒绝。
- **后端鉴权取流**：兑换建立下载会话（deliverySessionId），同会话 Range [start,endInclusive]/断线续传按同一不可变版本取流；每块重检身份/登录会话/撤权/过期/读授权；服务端分块上限 1MiB（客户端自选边界不得架空在途撤权重检）；`FileClient` 新增 `getContentRange` 范围读取契约（local=RandomAccessFile / db=内存裁切 / s3=GetObject Range 实装，ftp/sftp 默认整读裁切回退）。绝不向私有附件调用者返回存储 URL。
- **事务语义**：兑换事务化（codex r2/r3 竞态链闭合）——CAS 后同事务复核资产状态（DELETING/已移除即回滚会话），并以 `FOR UPDATE` 文件行锁持有至提交与删除侧中间态转移串行化（锁序统一「文件行→票据行」）。
- **登录会话绑定**（服务端权威）：控制器以当前访问令牌 SHA-256 派生会话标识（token 即登录会话凭据，退出失效/重登录变更；提取源走同一 `SecurityProperties` 配置），绝不采信客户端自报；同主体令牌刷新/重登录派生新标识时**重绑定续传**（定点条件更新，防逆转并发撤权），跨主体由身份谓词拒绝。
- **端点**：`/infra/file/delivery/issue|redeem|chunk|revoke`（认证层鉴权，授权在服务层——与 FILE-001.A 下载端点同策）；错误码 1-001-003-028~031；API 清单基线重生成（顺带补登 AUDIT-002 收口漏登的 `system/audit-event/page|clean` 2 条——系统性漏登第三次实例，main 上 ApiInventoryTest 再度红）。
- **测试**：采纳规格 12 用例（原子兑换/Range 续传/幂等/转发拒/跨租户拒/退出·撤权·过期拒/导出中撤权拒/在途撤权停输出/身份重检/用途谓词/散列存储看守）+ 令牌刷新续传与跨主体拒绝看守；infra 全量 307/0。

## 评审弧（r0→r3，`gpt-6-astra`/`xhigh`/`read-only`）

| 轮次 | 结论 | 要点与处置 |
|---|---|---|
| **r0** | **2×P1 + 3×P2** | P1 revokeDelivery 无授权（任何认证者可凭他人 deliverySessionId 撤权，含跨租户）→ 加 LoginUser 本人或同租户管理员判定；P1 login-session 客户端可控（header 等值不构成服务端验证）→ 改服务端令牌派生；P2 start=0/end=MAX 单块架空在途撤权 → 服务端 1MiB 分块上限；P2 幂等兑换绕过过期/撤权重检（直接返回与 CAS 败者两条路径）→ 返回前重检；P2 每块整对象读取 → getContentRange 契约 |
| **r1** | **3×P2** | P2 令牌刷新断链（access token 轮换使散列变化中断续传）→ 同主体重绑定语义；P2 凭据提取硬编码 Authorization 偏离 `zszj.security.token-header` 配置 → 注入 SecurityProperties 同源提取；P2 分块上限下仍整对象读（16MiB×16 块=256MiB 流量）→ 范围读取实装 |
| **r2** | **1×P1 + 1×P2** | P1 重绑定整行回写逆转并发撤权（codex DB 探针实证：revoke 后 rebind 写回 REDEEMED）→ 仅定点更新 login_session 且条件化于 REDEEMED+未过期，无命中按撤权拒绝；P2 范围读返回长度与请求不符（存储/元数据不一致）未显式失败 → 长度一致性校验 |
| **r3** | **PASS / 0 发现** | 「No actionable regressions were found in HEAD. The row lock is consistent with the existing transaction and deletion flow, and all 18 focused file-delivery and lifecycle tests passed.」 |

**收口依据**：r3 达 0×P0/P1 阀值；全部弧内修复并配看守用例；无延后项。flaky 登记：biz-tenant 流式安全用例（SEC-012.B 领地）并发负载偶发，单独复跑绿，与本卡无关。

## Codex 原始结论（r3 逐字引用）

> No actionable regressions were found in HEAD. The row lock is consistent with the existing transaction and deletion flow, and all 18 focused file-delivery and lifecycle tests passed.
