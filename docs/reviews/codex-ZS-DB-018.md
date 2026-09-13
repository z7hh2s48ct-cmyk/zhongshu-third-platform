# ZS-DB-018 codex 评审处置（HANDOFF 补评，3×P2 登记处置）

- 评审工具：codex（gpt-6-astra / xhigh，read-only sandbox，`codex review --commit c3ae2e8e` @ main）
- 评审对象：ZS-DB-018 ORM、手写 SQL 与租户隔离的 PG 回归（HANDOFF-ZS-DB-018 交接单的待评审提交；2026-09-14 补评入库）
- 结论：**0×P0/P1，3×P2 全部登记处置**（按 reviews/README「后续处理约定」第 2 条，非阻塞）
- 原始日志：`outputs/handoff/ZS-DB-018.txt`（未入库；本档案为处置入库）

## 发现与处置（3×P2，全部指向验证脚本 `run-db018-verify.mjs` 的断言强度）

| # | 发现 | 处置 |
|---|---|---|
| 1 | **Assert joined-user data in the LEFT JOIN isolation case**（L179-182）：C3 关联 JOIN 用例断言本租户可见/他租户 0 行，但未断言 JOIN 到的用户列数据内容正确 | 登记处置：断言强化——JOIN 内容断言随 ZS-SYS-001.A 真实 API 级回归自然覆盖（七类矩阵含关联读回），脚本侧列为后续小卡改进项 |
| 2 | **Assert the actual paginated result's size and contents**（L132-137）：C2 分页用例未断言分页结果集大小与内容 | 同上：SYS-001.A 分页矩阵用例已覆盖内容级断言；脚本侧登记 |
| 3 | **Make the batch-update filter target both tenants**（L152-156）：C4 批量更新用例的过滤条件未同时覆盖双租户形态 | 同上：批量混入拒绝已由 SYS-001.A 跨类用例覆盖；脚本侧登记 |

## 验证（交付时点证据，沿用卡片开发记录）

- 真实 PG17（一次性 Docker 容器）C1~C10 全过：CRUD/分页/JOIN/批量/逻辑删除/聚合按 tenant_id 隔离、伪造上下文 PK 越权 0 行、三类合法范围（全局表/@TenantIgnore/系统清理）结构断言、`--self-test` 负向对照证明隔离非空洞；已注册进 `run-pg-regression.mjs`（8 套件，2026-09-13 空载复跑 8/8 全绿）
- SQL 级等效语句验证的边界（非 PG 原生 RLS、拦截器端到端归 SEC-012.A/B）已在卡片登记
