# Codex 评审：hotfix-A — ZS-BRAND-004.A P1+P2 演示账号迁移安全修正

## 评审元信息

| 项目 | 值 |
|---|---|
| 评审对象 | commit `45ab83e3`（含 r1/r2 两轮修正） |
| 评审工具 | codex-cli 0.153.4, model gpt-6-astra, reasoning effort xhigh |
| 评审轮次 | 3 轮（初始 → r1 修正 → r2 修正） |
| 关联评审 | [codex-ZS-BRAND-004.A.md](codex-ZS-BRAND-004.A.md) |
| 修改文件 | `services/zhongshu-core/sql/postgresql/upgrades/20260908_brand_rename_zszj.sql`（+22/-2） |

## 修复的原始缺陷

本 hotfix 修复 [codex-ZS-BRAND-004.A.md](codex-ZS-BRAND-004.A.md) 中的 2 项发现：

| # | 优先级 | 原始缺陷 | 修复方式 |
|---|---|---|---|
| P1-1 | P1 | username 迁移 `WHERE username='yudao'` 波及所有租户，未限定演示账号 | 改为 `WHERE id=100 AND tenant_id=1 AND username='yudao'`，加 `NOT EXISTS` 冲突检查 |
| P2-6 | P2 | email 映射不完整，`yudao@iocoder.cn` 仅被通用域名替换为 `yudao@example.com`，残留旧标识 | 新增 3.2 节精确映射 `yudao@iocoder.cn → zszj@example.com`，先于 3.3 通用替换 |

## Codex 评审迭代过程

### 第 1 轮（commit `4bc43190`）— 发现 2 × P2

codex 通过 SQLite 内存数据库复现 5 个测试场景（baseline / other_tenant / live_username_conflict / deleted_username_conflict / live_email_conflict），发现：

| # | 优先级 | 发现 | 位置 | 复现证据 |
|---|---|---|---|---|
| A-r1-1 | P2 | username 冲突检查未过滤 `deleted=0`，已软删除的 `zszj` 行永久阻塞演示账号改名 | `sql:43-44` | `deleted_username_conflict` 场景：demo 账号保持 `yudao` |
| A-r1-2 | P2 | email 精确映射未检查目标邮箱是否已被占用，产生重复 active email → `selectByEmail`/`selectOne` 异常 | `sql:52-54` | `live_email_conflict` 场景：`duplicate active emails: [(1, 'zszj@example.com', 2)]` |

**根因分析**：
- A-r1-1：应用层 `BaseDO.@TableLogic` 将 `deleted` 字段作为逻辑删除标记，查询自动过滤 `deleted=0`；迁移脚本的 `NOT EXISTS` 子查询未对齐此语义。
- A-r1-2：PostgreSQL schema 无 email 唯一约束，事务可提交；但应用层 `validateEmailUnique` → `AdminUserMapper.selectByEmail`（`selectOne`）在重复行时抛异常。

### 第 2 轮（commit `deef9e93`，r1 修正）— 发现 1 × P2

r1 修正内容：
- `NOT EXISTS` 子查询增加 `AND u2.deleted = 0`（修复 A-r1-1）
- email 映射增加 `NOT EXISTS` 冲突检查（修复 A-r1-2）

codex 复审发现新 P2：

| # | 优先级 | 发现 | 位置 |
|---|---|---|---|
| A-r2-1 | P2 | email 冲突检查仅覆盖已有 `zszj@example.com`，未覆盖 3.3 通用域名替换后也会变成 `zszj@example.com` 的行（如 `zszj@iocoder.cn`） | `sql:57-59` |

**根因分析**：3.2 精确映射与 3.3 通用替换存在交互——若租户 1 中另有活跃用户持有 `zszj@iocoder.cn`，3.2 的 `NOT EXISTS` 检查通过（目标 `zszj@example.com` 不存在），但 3.3 将该用户邮箱也替换为 `zszj@example.com`，最终产生重复。

### 第 3 轮（commit `45ab83e3`，r2 修正）— ✅ 通过

r2 修正内容：
- email `NOT EXISTS` 检查同时覆盖 `zszj@example.com`（直接冲突）和 `zszj@iocoder.cn`（3.3 替换后冲突）

codex 复审结论：

> "No actionable regressions were identified. The guards match tenant and soft-delete semantics, and isolated SQL checks passed collision and rerun scenarios."

**验证测试**（SQLite 内存 DML，6 场景全 PASS）：
- `seed only`：基线，仅演示账号 → 改名成功
- `other tenant`：其他租户有 `zszj` → 不阻塞租户 1
- `active username conflict`：租户 1 有活跃 `zszj` → 正确跳过改名
- `deleted target conflict`：租户 1 有已删除 `zszj` → 不阻塞改名
- `active email conflict`：租户 1 有活跃 `zszj@example.com` → 正确跳过 email 映射
- `future email conflict`：租户 1 有活跃 `zszj@iocoder.cn`（3.3 后会变 `zszj@example.com`）→ 正确跳过

**穷举回归**：2664 个有效三用户场景，与 parent commit 对比 0 个新增唯一性回归。

## 最终修改内容（`45ab83e3` vs parent `8b784695`）

```sql
-- 3.1 演示账号用户名
UPDATE system_users
   SET username = 'zszj'
 WHERE id = 100 AND tenant_id = 1 AND username = 'yudao'
   AND NOT EXISTS (
     SELECT 1 FROM system_users u2
      WHERE u2.tenant_id = 1 AND u2.username = 'zszj' AND u2.id <> 100 AND u2.deleted = 0
   );

-- 3.2 演示账号邮箱：精确映射
UPDATE system_users
   SET email = 'zszj@example.com'
 WHERE id = 100 AND tenant_id = 1 AND email = 'yudao@iocoder.cn'
   AND NOT EXISTS (
     SELECT 1 FROM system_users u2
      WHERE u2.tenant_id = 1 AND u2.id <> 100 AND u2.deleted = 0
        AND (u2.email = 'zszj@example.com' OR u2.email = 'zszj@iocoder.cn')
   );
```

## 复核与处置

| 项 | 状态 | 说明 |
|---|---|---|
| P1-1 username 迁移波及所有租户 | ✅ 已修复 | 限定 `id=100 AND tenant_id=1`，加 `NOT EXISTS` + `deleted=0` 冲突检查 |
| P2-6 email 映射不完整 | ✅ 已修复 | 精确映射 + 冲突检查覆盖直接和间接（3.3 替换后）两种冲突路径 |
| A-r1-1 soft-delete 阻塞改名 | ✅ 已修复 | `NOT EXISTS` 增加 `AND u2.deleted = 0` |
| A-r1-2 email 冲突未检查 | ✅ 已修复 | 增加 `NOT EXISTS` 子查询 |
| A-r2-1 3.3 交互冲突 | ✅ 已修复 | `NOT EXISTS` 同时检查 `zszj@example.com` 和 `zszj@iocoder.cn` |
| PostgreSQL 实际执行验证 | ⚠️ 未验证 | Docker 不可用；SQLite 内存 DML 验证通过；真实 PG 验证归 B02 |

## 经验登记

1. **迁移脚本的冲突检查须与应用软删除语义对齐**：`@TableLogic` 使应用查询自动过滤 `deleted=0`，迁移脚本的 `NOT EXISTS` 也应限定 `deleted=0`，否则已删除行永久阻塞迁移。
2. **多步 UPDATE 的冲突检查须考虑后续步骤的副作用**：精确映射（3.2）与通用替换（3.3）存在交互，冲突检查应覆盖"当前已存在"和"后续步骤会产生"两种冲突路径。
3. **SQLite 内存 DML 是迁移脚本的有效验证手段**：在 Docker/PG 不可用时，SQLite 可复现 UPDATE + NOT EXISTS 的核心逻辑，穷举场景验证唯一性回归。
