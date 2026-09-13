# ZS-SEC-012.B codex 评审处置（r0→r3 四弧）

- 评审工具：codex（gpt-6-astra / xhigh、high，read-only sandbox）
- 评审对象：分支 `feat/sec-012-b`（ZS-SEC-012.B 安全与双端请求联合验收，B03 终环）
- 结论：**r3 PASS / 0 发现**（评审收敛）

## 交付内容

1. **全面 async 运行时合同**（夹具新增 3 端点 + 联合回归 9+1 用例）：跨线程租户/用户上下文传播（池化 applicationTaskExecutor + TTL BPP 交替租户防串号精确断言）、异步异常统一出口（禁泄漏类名/原始消息）、流式 StreamingResponseBody 经 ASYNC 派发、流式不免认证。
2. **CORS 端到端**：嵌入容器（RANDOM_PORT）真实过滤器链测试——evil 预检/实际请求 403 短路、批准源放行 + ACAO/ACAH/ACEH（trace-id）断言；MockMvc 不按容器顺序应用 FilterRegistrationBean 的局限如实登记。
3. **异步访问日志产品修复**（r0/r1/r2 三轮深化）：`ApiAccessLogFilter` 改为在 ASYNC 派发内记录（`shouldNotFilterAsyncDispatch=false`），AsyncListener 回调方案因租户上下文已被清理（日志归属 tenant_id=0）被 r1 否决；`TenantContextWebFilter` 同样参与 ASYNC 派发重建上下文（r2 P1）；beginTime 请求属性跨派发复用保完整耗时（r2 P2）；无 CommonResult 且状态 ≥400 记 500 防伪报成功；请求属性幂等护栏防二次派发重复。
4. **本地与 CI 同入口**：G12 门禁（--mvn，安全链 .A 31 + .B 联合 13 + visit 夹具 + 异步日志 5）+ CI 独立 job（setup-java temurin 17 跨平台 mvn）；G13 门禁（双端请求层合同 vitest）+ CI 由 CLIENT-005.A 流水线覆盖；门禁自测更新 17/17。

## 评审弧

| 轮次 | 结论 | 要点 |
|---|---|---|
| r0 | FAIL 3×P1+5×P2 | P1 G12 未入 CI（跨平台 mvn）；P1 CORS 负向仅 checkOrigin 不证真实链（嵌入容器方案）；P1 异步访问日志伪报成功（产品修复起点）；P2×5（TTL 传播未装配/前缀断言漏检/G12 注释 over-claim/异常断言 OR 弱/门禁自测未更新） |
| r1 | FAIL 2×P1+3×P2 | P1 AsyncListener 回调在租户上下文清理后执行、日志归属 tenant_id=0（改 ASYNC 派发内记录）；P1 G13 脚本块注释被 `*/` 提前闭合（语法错误）；P2 MVC 接线名 applicationTaskExecutor + TTL BPP 被排除（放开）；P2 npx Windows 不可用（node 直启 vitest）；P2 CI/G12 漏异步日志测试 |
| r2 | FAIL P1+P2 | P1 TenantContextWebFilter 仍跳过 ASYNC 派发（上下文未重建，归属问题未解决）→ 同步参与；P2 beginTime 跨派发重置（请求属性复用） |
| r3 | **PASS / 0 发现** | — |

## 验证

- 全量相关回归（web + biz-tenant 六测试类）：**132 tests / 0 failures，BUILD SUCCESS**。
- 门禁自测：`run-local-gates.test.mjs` 17/17。
- 本地门禁：`run-local-gates.mjs` 14 项定义（--fast 10 项与 CI 一致）全绿。
- 合并：`--no-ff` 合并回 main（见 docs/05 V1.35 记录）。

## 环境坑（重要发现，登记复用）

**本地 Maven 仓库陈旧构件会使测试结果失真**：本轮初版 CORS 断言全部失败的根因是 biz-tenant 解析到本地仓库中 SEC-004 收紧前的旧版 web starter（`patterns=[*]`、无 exposedHeaders）。重装最新 framework 构件后即恢复。任何依赖跨模块构件的测试前，先 `mvn install` 相关上游模块。

## 延后/边界（非阻塞）

1. SSE/长连接（流式超时、跨线程 trace 一致性）的长时间行为归环境批次；本批覆盖流式派发与异常出口。
2. 文件错误路径（FILE-001.A 交付后的导出失败合同）归 B04。
3. 真实 PG/Redis 场景归 ZS-SYS-001.A（他区开发中，按协调约定本区不触碰）。
4. B08 业务组织模型复验归 PERM 子项（D-09 后）。
