# ZS-CLIENT-004 codex 评审处置（r0→r3 四弧）

- 评审工具：codex（gpt-6-astra / xhigh，`--sandbox read-only` 读评工作区，多轮以
  **两端时序交错推演实证取消/重试再入竞态与孤儿会话泄漏**）
- 评审对象：分支 `feat/client-004`（ZS-CLIENT-004 统一附件上传完成态与私有下载体验——
  两端对接 FILE-003 直传完成确认 + FILE-004.A 主体绑定下载会话；`uploadFileWithCompletion` / `downloadPrivateFile` 两端对称）
- 结论：**r3 PASS / 0×P0/P1**（四弧收敛：r0 FAIL 1×P1+3×P2 → r1 FAIL 1×P1+1×P2 → r2 FAIL 1×P1+1×P2 → r3 PASS）
- 说明：各弧 raw 输出保留于 `outputs/client004-codex-r{0..3}.log`，verdict 结构化保留于
  `outputs/client004-codex-r{0..3}-verdict.json`；发现与处置逐条摘录如下，如实登记。

## 交付内容（impl commit `70ec02e0`，12 files +2149/-1；处置至 `18730537`）

1. **上传完成态编排（两端对称）**：admin-web `components/UploadFile/src/uploadCompletion.ts`（+205）、
   miniapp `utils/uploadFile.ts`（+219）——`uploadFileWithCompletion` 对接 FILE-003 直传票据，
   直传成功后调用完成确认端点落库，区分「已直传未确认」与「已确认完成」两态，避免半成品附件被当作完成。
2. **私有下载（两端对称）**：admin-web `utils/privateDownload.ts`（+232）、miniapp `utils/download.ts`（+224）
   ——`downloadPrivateFile` 对接 FILE-004.A 主体绑定下载会话，先申请一次性下载会话再取流，取流完成/失败
   集中撤权（`revokeOnce` 幂等），杜绝会话泄漏与重复取流。
3. **契约层**：admin-web `api/infra/file/index.ts`（+94）、miniapp `api/infra/file/index.ts`（+82）——
   完成确认 / 下载会话申请 / 撤权的类型化契约。
4. **错误映射对齐 CLIENT-003**：admin-web `config/axios/service.ts`（+10）——上传/下载失败按既有错误契约
   映射，零字节截断防护，重试幂等。

## 评审弧（两端时序交错推演）

| 轮次 | 结论 | 发现 → 处置 |
|---|---|---|
| r0（`70ec02e0`） | FAIL 1×P1+3×P2 | **P1** 两端上传取消竞态——`cancel` 后紧接 `retry`/新 `start`，前次操作的取消标记与回调被后次继承，导致新操作被误判取消或旧回调污染新态；**P2** 私有下载撤权缺失/时机错误——取流异常路径未撤权致下载会话泄漏；**P2** 零字节文件被截断处理；**P2** 下载/上传错误未按 CLIENT-003 契约映射。处置 `a4dbf0c3`（两端上传取消竞态 P1 + 私有下载撤权/截断/错误映射 P2 收口） |
| r1（`a4dbf0c3`） | FAIL 1×P1+1×P2 | **P1** `uploadCompletion.ts:121-131` 凭证先写后校 + `cancel→retry/start` 再入竞态未彻底隔离——无操作标识（opId）时多次操作共享同一 `cancelled` 标记，再入仍互相干扰；**P2** `privateDownload.ts:117-125` 无 opId 时私有下载会话共享 `cancelled`，并发取流相互误撤 → 引入**操作版本 + 会话/凭证绑定**：每次 `start`/`retry` 递增操作版本并绑定独立会话 ID，回调仅在同版本匹配时生效。处置 `2aa6d45e` |
| r2（`2aa6d45e`） | FAIL 1×P1+1×P2 | **P1** 两端跨操作 `putDone` 继承——`retry`/`start` 继承了上一操作的 `putDone=true`，新操作尚未直传即被当作已完成，跳过完成确认；**P2** 取流中被取代的孤儿会话泄漏——操作被新版本取代时，旧操作已申请的下载会话未在异常/取代路径撤权 → `start` 显式重置 `putDone=false`；`revokeOnce` 在 catch 分支集中撤权（覆盖异常与被取代孤儿会话）。补两端 4 区分力测试（P1 putDone 重置 + P2 orphan 会话补撤权），r0/r1 实现验 RED → r2 GREEN 19+19。处置 `18730537` |
| r3（`18730537`） | **PASS / 0×P0/P1** | findings[]——48 项时序检查全通过；操作版本隔离再入、`putDone` 归属当前操作凭证、`revokeOnce` 幂等集中撤权（含被取代孤儿会话）、零字节截断防护、错误映射对齐 CLIENT-003 逐条复核；两端对称无回归 |

## 验证

- TDD：RED（`upload-completion.spec.ts` + `private-download.spec.ts` 两端骨架全败）→ GREEN。
- G13 双端契约：admin-web **184**（7 files）+ miniapp **94**（5 files）= **278 PASS**（含 r2 补入的两端 4 区分力测试，
  P1 putDone 重置 + P2 orphan 会话补撤权，GREEN 19+19）。
- G10 类型基线：admin-web currentErrors 11 = baselineEntries 11、newErrors[]（上传完成态 + 私有下载改造未引入类型回归）。
- 区分力测试：r2 四个新增用例对 r0/r1 未修复实现验 RED、对修复实现 GREEN，证明再入隔离与孤儿撤权修复非空洞。

## 登记边界（如实登记，非本次回归）

1. **两端对称**为设计约束：admin-web（Vue3+TS）与 miniapp（uni-app）各自实现同构的 `uploadFileWithCompletion` /
   `downloadPrivateFile` 编排，契约层类型化对齐；因运行时差异（浏览器 XHR/fetch vs uni API）不强行抽公共实现。
2. **操作版本 + 会话绑定**隔离 `cancel→retry/start` 再入竞态：回调仅在同操作版本匹配时生效，`putDone` 归属当前
   操作凭证不跨操作继承；被取代的旧操作下载会话由 `revokeOnce` 在 catch 分支集中撤权。
3. **私有下载一次性会话**对接 FILE-004.A 主体绑定语义，取流完成/失败/被取代均幂等撤权（`revokeOnce`），零字节文件
   不截断；错误按 CLIENT-003 既有契约映射。
4. **真并发/真实直传服务**依赖 FILE-003/FILE-004.A 后端票据与会话真实行为，本轮以两端 vitest 确定性时序交错证明
   编排正确性，真实对象存储 + 票据服务联验归 B06 批次真实环境。
5. 本记录不表示任何主任务已验收；卡片状态 待开发→待验收，等待 B06 批次真实环境放行。
