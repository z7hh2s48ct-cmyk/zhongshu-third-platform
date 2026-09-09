# ZS-BRAND-004.A Codex 代码评审

- **任务**：ZS-BRAND-004.A 种子新装基线与存量库改名迁移（真实 PG 验证）
- **提交**：`581927d0`
- **改动规模**：25 files changed, +1268 / -1069 lines
- **评审时间**：2026-09-09
- **评审工具**：`codex-cli 0.153.4`，模型 `gpt-6-astra`，reasoning effort `xhigh`
- **原始日志**：[codex-ZS-BRAND-004.A.raw.md](codex-ZS-BRAND-004.A.raw.md)

## Codex 原始结论

> The patch introduces unsafe account migration, conflicting tenant aliases, and a replacement workflow that corrupts its own upgrade script. Read-only replay and in-memory seed comparisons confirmed these issues; native PostgreSQL execution was unavailable because Docker access was denied.

### Review comments

- **[P1] Restrict the username migration to the identified demo account** — `services/zhongshu-core/sql/postgresql/upgrades/20260908_brand_rename_zszj.sql:37-39`
  > On an existing database, this updates every tenant's account named `yudao`, not just the seeded account (id 100, tenant 1). Legitimate users consequently lose their login name; if their tenant already contains `zszj`, the update creates duplicate usernames and `AdminUserMapper.selectByUsername()` fails with multiple results. Restrict the update to the demo identity and check for target-name conflicts before changing it.

- **[P2] Exclude upgrade scripts before applying either replacement pass** — `scripts/brand/apply-naming-migration.mjs:65-67`
  > Running `node scripts/brand/apply-naming-migration.mjs sql` now also rewrites the newly tracked upgrade script. Read-only replay confirms that old-value predicates become new-value predicates and replacements become no-ops, so the resulting script no longer upgrades the old baseline. Exclude `upgrades/` before `applyRules()`, not merely from the additional seed replacements; this also preserves the [frozen migration requirement](docs/06-品牌素材与命名映射.md#L58).

- **[P2] Preserve distinct website aliases for different tenants** — `scripts/brand/naming-rules.mjs:78`
  > Mapping `zsxq.iocoder.cn` to the same destination as `www.iocoder.cn` gives tenants 121 and 1 the identical website `www.zszj.example.com`, in both fresh seeds and upgraded databases. `getTenantByWebsite()` returns the first matching tenant, making domain-based tenant selection ambiguous, while `validTenantWebsiteDuplicate()` rejects subsequent updates to tenant 121. Use a distinct placeholder and synchronize the seeds and upgrade mapping.

- **[P2] Apply the complete demo email mapping during upgrades** — `services/zhongshu-core/sql/postgresql/upgrades/20260908_brand_rename_zszj.sql:45-47`
  > Upgrading the supplied baseline changes account 100's email to `yudao@example.com`, whereas the new seed and `SQL_SEED_REPLACEMENTS` specify `zszj@example.com`. The old product identifier therefore remains in the upgraded user profile, and rerunning the migration cannot repair it because the original domain is already gone. Apply the exact demo-email mapping before the general domain replacement.

## 复核与处置

### 发现 1：username 迁移波及所有租户（P1，仍有效）

**事实核对**（当前 HEAD `services/zhongshu-core/sql/postgresql/upgrades/20260908_brand_rename_zszj.sql:37-39`）：
```sql
UPDATE system_users
   SET username = 'zszj'
 WHERE username = 'yudao';
```

- `system_users` 表按 `(tenant_id, username)` 唯一（见 `sql/postgresql/ruoyi-vue-pro.sql` 中 `uk_system_users_tenant_id_username`），跨租户允许重名；
- 迁移脚本的 `WHERE username = 'yudao'` **未限定 tenant_id**，会命中所有租户中用户名为 `yudao` 的账号；
- 演示账号仅在租户 1（id=100）存在，其他租户的 `yudao` 账号属于真实用户；
- 若某租户已存在 `zszj` 账号（新装/历史注册），执行后该租户出现 `(tenant_id, 'zszj')` 重复，触发唯一约束冲突使事务整体回滚；
- 即使不冲突，`AdminUserMapper.selectByUsername()` 在同一租户内返回多条记录，登录链路报 `TooManyResultsException`。

**影响**：
- 存量库升级时，**所有租户**中名为 `yudao` 的真实用户被强制改名，登录凭据失效；
- 与 `zszj` 重名时事务回滚，迁移失败；
- 违反 05 清单 ZS-BRAND-004 验收项「仅迁移承载旧产品标识的种子/演示数据；不改动业务内容」。

**处置建议**（待修复）：
- 方案 A（推荐）：限定演示账号身份，并先检查目标名冲突：
  ```sql
  -- 3.1 演示账号用户名：仅租户 1 的 id=100 演示账号
  UPDATE system_users
     SET username = 'zszj'
   WHERE id = 100 AND tenant_id = 1 AND username = 'yudao'
     AND NOT EXISTS (
       SELECT 1 FROM system_users u2
        WHERE u2.tenant_id = 1 AND u2.username = 'zszj' AND u2.id <> 100
     );
  ```
- 方案 B：若演示账号 id 不可假定，按 `(tenant_id=1, username='yudao', nickname LIKE '%众墅之家%')` 复合条件定位。
- 修复归入 ZS-BRAND-004.B（B03 缓存/会话联验）前置补丁，或独立 hotfix 提交。

### 发现 2：apply-naming-migration.mjs 会重写自身升级脚本（P2，仍有效）

**事实核对**（当前 HEAD `scripts/brand/apply-naming-migration.mjs:42`）：
```js
const exclusions = {
  backend: [...],
  sql: [],       // ← 无任何排除
  web: [...],
  miniapp: [...],
}[scope];
```

- `sql` scope 的 `prefixes` 是 `['services/zhongshu-core/sql/']`（line 28），覆盖整个 sql 目录；
- `exclusions.sql = []`，意味着 `sql/postgresql/upgrades/20260908_brand_rename_zszj.sql` 也会被扫描；
- 执行 `node scripts/brand/apply-naming-migration.mjs sql` 时，`applyRules()` 会把脚本内的 `WHERE username = 'yudao'` 改写为 `WHERE username = 'zszj'`、`WHERE name = '芋道源码'` 改写为 `WHERE name = '众墅之家'`；
- 改写后脚本的 `WHERE` 谓词变成「新值 = 新值」的恒真匹配，但 `SET` 子句不变，导致：
  - 对已升级库：无操作（幂等仍成立）；
  - 对旧基线库：**不再匹配旧值**，升级失效；
- 违反 `docs/06-品牌素材与命名映射.md` 第 4.1 节「迁移脚本冻结，不随后续改名批次重写」。

**影响**：
- 升级脚本被自身工具链污染，旧库无法升级；
- 与 ZS-BRAND-002 引入的 `naming-rules.mjs` 保护规则冲突（保护规则仅覆盖代码注释/署名，不覆盖 SQL 谓词）。

**处置建议**（待修复）：
- 在 `apply-naming-migration.mjs:42` 的 `sql` 数组加入 upgrades 排除：
  ```js
  sql: [/^services\/zhongshu-core\/sql\/postgresql\/upgrades\//],
  ```
- 同步在脚本头注释（line 7）补充说明：「sql scope 排除 upgrades/，迁移脚本按 docs/06 第 4.1 节冻结」。
- 修复归入 ZS-BRAND-006.A 增强或独立 hotfix。

### 发现 3：zsxq.iocoder.cn 与 www.iocoder.cn 映射到同一目标（P2，仍有效）

**事实核对**（当前 HEAD `scripts/brand/naming-rules.mjs:74,78`）：
```js
['www.iocoder.cn', 'www.zszj.example.com'],   // line 74
...
['zsxq.iocoder.cn', 'www.zszj.example.com'],  // line 78
```

- 上游 `ruoyi-vue-pro.sql` 中租户 1 的 `websites` 含 `www.iocoder.cn`，租户 121 的 `websites` 含 `zsxq.iocoder.cn`；
- 改名后两者都变成 `www.zszj.example.com`；
- `TenantServiceImpl.getTenantByWebsite()` 按 `websites` 字段 LIKE 匹配返回**第一条**，租户 1 与 121 的域名解析结果不确定；
- `TenantServiceImpl.validTenantWebsiteDuplicate()` 在新增/修改租户时校验 `websites` 唯一，租户 121 后续任何涉及 websites 的更新都会被拒绝；
- 升级脚本 `20260908_brand_rename_zszj.sql:23-28` 同样把两者映射到 `www.zszj.example.com`，存量库升级后问题一致。

**影响**：
- 多租户域名路由歧义，`getTenantByWebsite()` 返回错误租户；
- 租户 121 的 websites 字段被锁死，无法通过管理界面修改；
- 违反 05 清单 ZS-BRAND-004 验收项「演示域中性化后保持租户可区分」。

**处置建议**（待修复）：
- 方案 A（推荐）：为 `zsxq.iocoder.cn` 分配独立占位域：
  ```js
  ['zsxq.iocoder.cn', 'zsxq.zszj.example.com'],
  ```
  同步修改升级脚本 line 28 的 `REPLACE` 链，把 `zsxq.iocoder.cn → www.zszj.example.com` 改为 `zsxq.iocoder.cn → zsxq.zszj.example.com`。
- 方案 B：若租户 121 是上游演示数据且产品不启用，直接在种子中删除该租户（需评估对 `system_tenant_package`、`system_users` 等关联数据的影响）。
- 修复归入 ZS-BRAND-004.B（B03）或独立 hotfix。

### 发现 4：升级脚本 email 映射不完整（P2，仍有效）

**事实核对**（当前 HEAD `services/zhongshu-core/sql/postgresql/upgrades/20260908_brand_rename_zszj.sql:45-47`）：
```sql
UPDATE system_users
   SET email = REPLACE(email, '@iocoder.cn', '@example.com')
 WHERE email LIKE '%@iocoder.cn%';
```

- 演示账号 id=100 的旧 email 是 `yudao@iocoder.cn`；
- 上述 `REPLACE` 仅替换域名部分，结果是 `yudao@example.com`；
- 但新装种子（`sql/postgresql/ruoyi-vue-pro.sql`）与 `SQL_SEED_REPLACEMENTS`（`naming-rules.mjs:72`）都指定 `yudao@iocoder.cn → zszj@example.com`；
- 升级后账号 100 的 email 与新装基线不一致，旧产品标识 `yudao` 残留在用户资料中；
- 由于原域名 `@iocoder.cn` 已被替换为 `@example.com`，**重跑迁移无法修复**（幂等但不可逆）。

**影响**：
- 升级库与新装库的演示账号 email 不一致，违反「新装/升级两路径终态一致」验收；
- 旧产品标识 `yudao` 残留在用户资料，违反 ZS-BRAND-004「演示数据品牌串中性化」目标；
- 若后续按 email 做用户识别/通知，升级库与新装库行为分叉。

**处置建议**（待修复）：
- 在升级脚本 line 45 之前插入精确映射：
  ```sql
  -- 3.0 演示账号邮箱：精确映射（先于通用域名替换）
  UPDATE system_users
     SET email = 'zszj@example.com'
   WHERE id = 100 AND tenant_id = 1 AND email = 'yudao@iocoder.cn';
  ```
- 通用域名替换（line 45-47）保留，处理其他 `@iocoder.cn` 邮箱（如 `test@iocoder.cn`）。
- 修复归入 ZS-BRAND-004.B（B03）前置补丁，或与发现 1 合并为同一 hotfix。

## 其他核对项（Codex 已验证）

| 项目 | 结果 |
|---|---|
| 种子新装基线：芋道→众墅之家、yudao→zszj、演示域中性化 | ✅ 通过（`SQL_SEED_REPLACEMENTS` 顺序正确，精确匹配先于通用替换） |
| 演示表 `yudao_demo*` → `zszj_demo*` 改名（表/序列/约束/索引） | ✅ 通过（DO $$ 块按旧名定位，幂等） |
| 单事务原子执行、失败整体回滚 | ✅ 通过（BEGIN/COMMIT 包裹，真实 PG 验证注入失败语句回滚成功） |
| 幂等性：重复执行无效果、无报错 | ✅ 通过（真实 PG 验证重复执行仅 1 处 LIKE 命中，已由补漏规则消除） |
| 上游署名链接恢复（gitee.com/zhijiantianya、github.com/YunaiV） | ✅ 通过（评审修正已落地，`naming-rules.mjs` 增加保护规则） |
| 真实 PG17 新装/旧样本升级/重复迁移/失败回滚验证 | ✅ 通过（docker postgres:17-alpine 临时容器，已清理） |
| 字典/菜单/参数/任务种子不含旧产品标识 | ✅ 通过（基线核查证据见脚本头注释） |

## 结论

**评审通过（含 1 项 P1 + 3 项 P2 待修复）**。

- 四项发现均在当前 HEAD 仍然有效，须在下轮批次修复：
  - **P1**（username 迁移波及所有租户）：影响存量库升级安全，**优先级最高**，归 ZS-BRAND-004.B 前置 hotfix。
  - **P2-1**（apply-naming-migration.mjs 重写升级脚本）：影响工具链自洽，归 ZS-BRAND-006.A 增强或独立 hotfix。
  - **P2-2**（zsxq/www 域名映射冲突）：影响多租户路由，归 ZS-BRAND-004.B（B03）。
  - **P2-3**（email 映射不完整）：影响新装/升级终态一致，归 ZS-BRAND-004.B 前置 hotfix（与 P1 合并）。
- 种子新装基线与存量迁移脚本整体达标，真实 PG 验证通过，符合 B01 放行条件；缓存/会话/任务持久化联验按 05 清单归 ZS-BRAND-004.B（B03）与 ZS-BRAND-004.C（B05）。
- 建议把 P1 + P2-3 合并为同一 hotfix 提交（均涉及 `system_users` 表演示账号迁移），P2-1 + P2-2 合并为另一 hotfix（均涉及 `naming-rules.mjs` / `apply-naming-migration.mjs` 工具链）。
