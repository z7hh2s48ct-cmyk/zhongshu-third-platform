# db/migration —— 唯一数据库迁移目录（ZS-DB-003）

命名、版本号、回滚与环境开关规范见 [docs/数据库迁移规范](../../../docs/数据库迁移规范.md)。

- 文件名格式：`V<yyyyMMdd>.<NNN>__<module>_<描述>.sql`，版本号全局唯一。
- `V20260909.001__system_infra_baseline.sql`：底座基线（49+11 张表 DDL + 必要种子；用户账号与演示数据分离，由 ZS-DB-004 交付，构建规则见 scripts/db/build-baseline-migration.mjs）。
- 本目录外的任何 SQL 目录（含 `sql/postgresql/`）仅为来源证据或专用场景脚本，
  不作为应用启动的迁移入口；禁止新增第二套会随应用执行的迁移位置。
