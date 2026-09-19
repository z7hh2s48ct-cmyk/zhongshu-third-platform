# codex 评审处置：ZS-CLIENT-005.B 两端登录/导航/文件/待办 E2E 回归

- 评审对象：`feat/client-005-b`（worktree `.wt/zszj-wt-client-005-b`），联验本地环境编排（deploy/docker-compose.local.yml + env 模板 + README-local）+ 接口域 E2E 套件（scripts/client005b/run-client005b-e2e.mjs，18 用例）+ MP 3.5.17→3.5.12 降级
- 评审方式：codex exec（gpt-6-astra / model_reasoning_effort=xhigh / bypass sandbox），r0→r2 共三弧；弧内真实回环 TCP/WebSocket、在线 E2E 连跑 5 轮、内存时序与隔离重放实证
- 收敛结论：**r2 PASS/0×P0~P3 全零**；HEAD 前后校验一致
- 合并：`--no-ff` main `e1076b3f`（分支提交链 `ed8f0f6b` 环境编排 → `5da7f124` E2E 套件 → `7977f80d` MP 降级 → `beb5bc52` r0 处置 → `cc5194f6` r1 处置 → `e5fbc277` r2 处置；`46fd2370` 台账移交登记随 main 并入）

## 弧次与发现

| 弧 | 对应处置 | 发现 | 要点 |
|---|---|---|---|
| r0 | `beb5bc52` | 2×P1+8×P2+2×P3 | P1 server 未绑回环暴露默认管理员（实测 :::48080 可登录）→ README 启动命令补 --server.address=127.0.0.1 并重启实证；P1 T5「他人消息」实为自读（发送与读取同一用户）→ 重设计 T5/T6 发两条（本人+他人对照）；P2 断言未锁码（L1/T5 code!==0 连 500/429 都算拒绝）→ 锁定业务码；P2 N2 判别菜单断言失效（flat 父路径 vs 裸名匹配错位）→ 修复并加 admin 侧在场断言；P2 F1 未验 master 切换；P2 F3 非全等比较+匿名边界缺失；P2 限流无容忍（重放中断）；P2 夹具不幂等；P2 用户定位依赖分页窗口；P2 种子 SQL 缺失；P3 MP 表述误述+SEC-009.B 耦合未登记 |
| r1 | `cc5194f6` | 4×P2+1×P3 | 用户读回精确匹配（模糊匹配同名 backup 截胡，隔离重放实证）；L1 限流容忍（连续重放 429 被当通过，实测 17/18 exit=1）；FX restore code 校验+读回（恢复 500 后环境留测试配置无 FAIL）；F1 原 master 完整分页扫描（单页 100 累积漏扫）；README 显式序列名（pg_get_serial_sequence 对 @KeySequence 独立序列返回 NULL，在线实测） |
| r2 | `e5fbc277` | 1×P2+1×P3 | 用户读回逐页扫尽（超 100 用户场景 N2 错报不存在且 T2-T6 连锁失败）——精确 find + 逐页扫尽；FX 异常同步 restoreOk（网络异常不得让报告宣称恢复成功） |

## 验证

- E2E 18/18 ×连续多轮（r2 弧内在线连跑 5 轮全绿，第 4 轮 L1 真实触发 429→等待 65s→严格命中 1002000000）
- system 全量 -am 852 用例（MP 3.5.12 降级后维持已知基线 2F+3E，隔离复跑 29/29）；PG 回归 16/16；fast 10/10
- admin ts:check 11 错误=上游基线；miniapp type-check 0 错误
- codex 独立核验：T5/T6 归属边界经 DB 确认（消息分属 145/1）；MP 内置方法 javap 实证；Lua 配额脚本 jedis-mock 四形态验证；MPJ 父类字节码核实

## 登记边界

1. 本套件锚定**接口域合同**；浏览器级页面操作归 SYS-001.B/BRAND-003.B 联验执行；miniapp 微信开发者工具自动化（用户选定形态）随批次放行执行。
2. SEC-009.B ID→string 激活后，本套件 id 数值基线须同步调整（脚本头已登记耦合）。
3. MP 降级边界：MPJ 1.5.9 的 MPJBaseServiceImpl/JoinCrudRepository 父类引用 MP 3.5.13+ 类——当前项目未使用这两个入口（启动与全链 E2E 验证正常），启用前须升级 MPJ 或固定验证（登记于 README-local）。
4. jmreport iframe `?token=` 为报表引擎自有认证域，观察项非本卡范围。
5. Outbox 积压告警（login/logout 产生 LOGIN-005.B 补偿事件 PENDING）为 D-07 延后的正确行为——投递触发器接线归 D-07 联验链；联验数据重置命令见 README-local。
