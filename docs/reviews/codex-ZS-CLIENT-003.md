# ZS-CLIENT-003 codex 评审处置（r0→r3 四弧）

- 评审工具：codex（gpt-6-astra / xhigh，read-only sandbox）
- 评审对象：分支 `feat/client-003`（ZS-CLIENT-003 B03 登录请求合同：统一 Web/移动凭据范围与异常收敛）
- 结论：**r3 PASS / 0 发现**（评审收敛）

## r0（commit 47572250）FAIL 1×P1 + 1×P2

| 级别 | 发现 | 处置 |
|---|---|---|
| P1 | 小程序 URL 兼容问题 | r1 修复 `6de3fae7` |
| P2 | trace-id 合同不一致 | r1 修复 `6de3fae7` |

## r1（commit 6de3fae7）FAIL 1×P2

| 级别 | 发现 | 处置 |
|---|---|---|
| P2 | origin 默认端口归一化缺失（`:443`/`:80` 与省略端口不一致） | r2 修复 `1e4d4ce3` |

## r2（commit 1e4d4ce3）FAIL 2×P1 + 3×P2

| 级别 | 发现 | 处置 |
|---|---|---|
| P1 | `resolveFullUrl` 把协议相对地址 `//oss.example/upload` 拼到批准 baseURL 后判为批准来源，但 Axios 实际直发外部主机并携带 Authorization/tenant-id/trace-id（已复现） | r3 修复 `71e348f6`：`//` 开头原样返回 → `isApprovedApiOrigin` 失败关闭 |
| P1 | `allowToken=false` 仅阻止新增 Token；401 刷新分支直接给 `config.headers.Authorization` 赋值，外部绝对 URL 刷新重放第二次请求携带平台 Bearer Token（已复现） | r3 修复：回放/队列重放统一走 `reapplyAuthorizationHeader`（按同一凭据合同重放，外部地址/退出方/白名单显式清除 Authorization） |
| P2 | `refreshToken()` 修改全局 `axios.defaults.headers.common['tenant-id']`，useUpload 全局 axios.put 直传外部存储泄露 tenant-id（已复现） | r3 修复：tenant-id 改单请求 config |
| P2 | miniapp 断网/超时/取消丢失自动生成 traceId（uni-app promisify 浅拷贝，拦截器替换副本 header）；测试直接改原配置掩盖问题 | **延后登记**（见下） |
| P2 | miniapp 响应 data 为 null 时 async success 解构抛错未 reject 外层 Promise（调用方永久 loading）；admin-web 业务 403/429 仍 reject 字符串 'error' 丢失 code/message/trace-id | **延后登记**（见下） |

## r3（commit 71e348f6）PASS / 0 发现

评审自核修复正确性（undefined header 值在 axios 序列化的行为、AxiosHeaders 兼容、测试有效性），结论 PASS。

## 验证

- admin-web `service.spec.ts` 12/12（含 3 个 r2 回归：协议相对 URL 不携带平台凭据且不拼 baseURL、刷新回放按合同重放/清除 Authorization、刷新 tenant-id 单请求作用域）
- miniapp vitest 36/36（http.spec + interceptor.spec）
- 合并：`--no-ff` 合并回 main `31a653cc`

## 延后/边界（非阻塞，登记后续处置）

1. miniapp promisify 浅拷贝下错误回调丢失 traceId——需在派发前保存请求 traceId 或令拦截器与错误回调共享最终 header，测试需覆盖 uni-app 配置浅拷贝行为（归 B06 小程序批次）
2. admin-web 业务 403/429 统一错误对象（保留 code、message、响应 trace-id）与 miniapp 响应结构前置校验/异步 success 统一异常桥接（归异常收敛深化的后续小卡）
3. B06 对新页面/附件再次回归；Web E2E 与真实预签名存储联调归环境批次
