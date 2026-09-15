# ZS-LOGIN-006 codex 评审处置（r0→r2 三弧）

- 评审工具：codex（gpt-6-astra / xhigh，`--sandbox read-only` 读评工作区，多轮以
  **源码分支内存交互相证越权路径与撤销竞态窗口**）
- 评审对象：分支 `feat/login-006`（ZS-LOGIN-006 建立不暴露凭据的会话管理与踢出合同——
  RespVO 移除令牌秘密 / 会话 ID 撤销 / 归属校验 / 租户纵深防御 / 幂等 / 自助 my-page·revoke-my / 机器主体越权防护）
- 结论：**r2 PASS / 0×P0/P1**（三弧收敛：r0 FAIL 1×P1+3×P2 → r1 FAIL 1×P2 → r2 PASS）
- 说明：各弧 raw 输出保留于 `outputs/login006-codex-r{0..2}.log`，verdict 结构化保留于
  `outputs/login006-codex-r{0..2}-verdict.json`；发现与处置逐条摘录如下，如实登记。

## 交付内容（impl commit `2e6f1be6`，12 files +584/-30；处置至 `64d3c089`）

1. **不暴露凭据的 RespVO**：`OAuth2AccessTokenRespVO` 移除 `accessToken`/`refreshToken` 明文秘密字段，
   管理端与自助端列举会话只返回会话 ID、客户端、创建/过期时间与设备信息，杜绝令牌秘密经列表接口外泄
   （`OAuth2AccessTokenRespVoSecretTest` 以反射断言序列化产物不含任何秘密字段）。
2. **会话 ID 撤销（归属校验 + 租户纵深防御 + 幂等）**：`OAuth2TokenService#removeAccessTokenById` /
   `logoutById` 以会话 ID 为操作对象；`OAuth2TokenServiceImpl` 撤销前校验目标令牌归属（`userId` + `userType`
   双维），并在租户上下文之上做纵深防御，重复撤销同一会话幂等返回成功。
3. **自助会话管理端点**：`OAuth2TokenController` 新增 `my-page`（分页列举本人会话）与 `revoke-my`
   （撤销本人指定会话）；管理端 `delete` / `delete-list` 改为 id-based；两端均要求真实用户登录态。
4. **机器主体越权防护**：`client_credentials` 机器令牌（`userId=0`）无「本人会话」语义，自助端点拒绝其
   列出 / 撤销同为 `userId=0` 的其它客户端会话（跨客户端越权），抛 `OAUTH2_TOKEN_SESSION_SELF_REQUIRES_USER`。
5. **错误码（system 模块 `ErrorCodeConstants` 1-002-029-000~001）**：`OAUTH2_TOKEN_SESSION_NOT_OWNED`
   （无法操作他人的登录会话）、`OAUTH2_TOKEN_SESSION_SELF_REQUIRES_USER`（自助会话管理要求真实用户登录态）。
6. **admin-web 令牌列改会话 ID**：`api/system/oauth2/token.ts` + `views/system/oauth2/token/index.vue`
   以会话 ID 作为撤销操作键，界面不再展示或依赖令牌秘密。

## 评审弧（源码分支内存交互相证）

| 轮次 | 结论 | 发现 → 处置 |
|---|---|---|
| r0（`2e6f1be6`） | FAIL 1×P1+3×P2 | **P1** `OAuth2TokenController:68-69` `client_credentials` 机器令牌（`userId=0`）可跨客户端列举/撤销会话——同租户客户端 A、B 各取此类令牌后，A 调 `my-page` 列出 B 的会话再 `revoke-my?id=<B会话>`，归属校验因双方 `userId=0` 放行 → 自助端点要求真实用户主体（ADMIN 且 `userId>0`）；**P2** `OAuth2TokenServiceImpl:340-341` 归属仅比 `userId` 遗漏 `userType`——同租户 ADMIN(100) 可撤销 MEMBER(100) 会话（两主体独立主键序列同值）→ 双维校验 `userId`+`userType`；**P2** `OAuth2TokenServiceImpl:344` 撤销竞态静默失效窗口——READ COMMITTED 下 T1 selectById 校验 A、T2 删 A 建 B 保留刷新令牌 R、T1 `removeAccessToken(A)` 重查得 null 早退仍返回 `success(true)`，B/R 仍有效 → 抽取共享撤销逻辑用已校验 DO 的 refreshToken 取刷新行锁再撤销当代际；**P2** `OAuth2TokenController:64` `my-page`/`revoke-my` 未同步 `api-inventory-baseline.txt`（ApiInventoryTest 会因多出两个 ADMIN/AUTHENTICATED 路由失败）→ 登记两个 AUTHENTICATED 路由入基线。处置 `062bed52`（会话自助管理硬化——机器主体拒绝 / 类型双维 / 撤销竞态收口） |
| r1（`062bed52`） | FAIL 1×P2 | **P2** `OAuth2TokenServiceImplSessionRevokeTest.java:275-283` 撤销竞态回归测试无区分力——原测试不能证明「共享撤销逻辑取刷新行锁」修复真正生效（对未修复实现同样通过）→ 改为 spy 注入确定性交错（读后删 A：在 T1 校验通过后、撤销前由 spy 精确注入 T2 的删 A 建 B，断言修复实现撤销当代际而非早退漏撤），r0 实现已验 RED → r1 GREEN 8/8。处置 `64d3c089` |
| r2（`64d3c089`） | **PASS / 0×P0/P1** | findings[]——r0 四项与 r1 一项均 Resolved；机器主体越权、`userType` 双维、撤销竞态行锁、api-inventory 同步、竞态测试区分力逐条复核通过；无回归 |

## 验证

- TDD：RED（`OAuth2TokenServiceImplSessionRevokeTest` + `OAuth2AccessTokenRespVoSecretTest` 骨架全败）→ GREEN。
- 后端：`OAuth2TokenServiceImplSessionRevokeTest` **8/8** + `OAuth2AccessTokenRespVoSecretTest` **3/3**
  + `NotifyDispatcherTest` **17/17**（MSG-001 共存回归）= **28/28 BUILD SUCCESS**（合并 main 含 JOB-001 后于集成树复跑，8.5s）。
- `ApiInventoryTest` **4/4**（`my-page`/`revoke-my` 两个 AUTHENTICATED 路由已登记基线，端点清单漂移门禁通过）。
- 前端：G10 类型基线 admin-web currentErrors 11 = baselineEntries 11、newErrors[]（会话 ID 改造未引入类型回归）。
- 竞态测试区分力：r1 spy 注入确定性交错对 r0 未修复实现验 RED、对修复实现 GREEN，证明测试非空洞。

## 登记边界（如实登记，非本次回归）

1. **不暴露凭据**为设计约束：RespVO 移除令牌秘密后，管理端/自助端一律以会话 ID 操作；令牌明文仅在签发响应
   一次性返回，任何列举/撤销路径不再回传秘密。
2. **归属校验双维**（`userId` + `userType`）覆盖同租户跨主体（ADMIN vs MEMBER）主键同值越权；跨租户隔离复用
   既有租户纵深防御，不重写。
3. **机器主体（`client_credentials` `userId=0`）拒绝自助会话管理**：无「本人会话」语义，防止跨客户端越权列举/撤销；
   客户端凭据撤销继续走既有校验 `clientId` 的 OAuth2 撤销接口。
4. **撤销竞态**以 READ COMMITTED 下共享撤销逻辑 + 刷新令牌行锁收口，spy 注入确定性交错证明；真并发多实例竞态
   依赖真实 PG 行锁语义，归批次真实环境联验。
5. 本记录不表示任何主任务已验收；卡片状态 待开发→待验收，等待 B03 批次真实环境放行。
