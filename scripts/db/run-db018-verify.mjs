/**
 * ZS-DB-018：ORM、手写 SQL 与租户隔离的 PG 回归——两技术租户真实 PG 验证用例集。
 *
 * 依赖一次性 Docker PG17 容器（与 scripts/db/run-db006-verify.mjs / test-pg-fixture.mjs 同款隔离/清理语义），
 * 加载 V1 基线 + V2 回填后，用「与 TenantDatabaseInterceptor 改写结果逐字等价的 SQL」验证数据层租户链。
 *
 * 边界声明（与 05 文档 ZS-DB-018 卡一致）：
 *   TenantDatabaseInterceptor 是 MyBatis-Plus 层的 SQL 改写（对继承 TenantBaseDO 的表注入 tenant_id=ctx），
 *   不是 PG 原生 RLS；直接 JDBC 不会自动继承该规则。本套件验证的是「拦截器改写后的等效 SQL」在真实 PG 上的
 *   隔离语义（作用域内只见本租户、伪造他租户对象 ID 被拒），以及全局表/忽略注解/系统清理三类合法绕过范围；
 *   ORM 拦截器 Java 级装配与 HTTP 层租户比对（普通 tenant-id 与 Token 不一致拒绝）由 ZS-SEC-012.A 的
 *   SecurityFilterChainFixtureTest（双技术租户夹具）与 ZS-SEC-001.A 覆盖，本套件不重复、只做数据层补强。
 *   tenant_id 与业务组织 tenant_org_id 的映射属 D-09 后任务，本套件不改名、不建身份表，仅用 System 技术租户 1/2。
 *
 * 用例（ctx = TenantContextHolder 注入的租户号；作用域 SQL = ... AND tenant_id = ctx）：
 *   C1 CRUD 读取隔离：ctx=1 只见本租户部门，看不到租户2（SELECT 注入 tenant_id）；
 *   C2 分页隔离：LIMIT/OFFSET 在 tenant_id=ctx 之内，页内不越租户；忽略路径跨两租户（对照）；
 *   C3 关联隔离：users ⋈ dept 两端注入 tenant_id；伪造跨租户外键（T1 用户指向 T2 部门）被 JOIN 排除；
 *   C4 批量隔离：批量插入（造数多行 VALUES 跨两租户）+ 批量 UPDATE 作用域限本租户，他租户零影响；
 *   C5 逻辑删除隔离：本租户逻辑删除生效；跨租户按 PK 逻辑删除被拒（0 行）；
 *   C6 手写 SQL 隔离：自定义聚合/LEFT JOIN 注入 tenant_id=ctx，只见本租户分组；
 *   C7 伪造上下文/他租户对象 ID 被拒绝：ctx=1 按 PK 读/改/删租户2 对象均 0 行，目标行完好；
 *   C8 全局表合法范围：system_dict_data/infra_job_log 无 tenant_id 列（BaseDO+@TenantIgnore）→ 结构性不可租户化；
 *   C9 忽略注解合法范围：system_oauth2_access_token 有 tenant_id 列，仅 selectByAccessToken 方法级 @TenantIgnore 显式绕过；
 *   C10 系统清理合法范围：TokenCleanJob.execute @TenantIgnore → 跨租户全局有界清过期令牌（对照 ZS-DB-014/015）。
 * 附带 --self-test 负向对照：证明隔离断言非空洞（作用域内 0、忽略路径可见同一行）。
 * 任一用例失败 → 退出码非零。
 *
 * 2026-09-15 P2 硬化（codex-ZS-DB-018 评审处置 #1/#2/#3）：
 *   C2 分页追加「LIMIT 3 返回行 name 序列」内容断言；
 *   C3 关联追加「JOIN 到的 username@dept.name 序列」内容断言；
 *   C4 批量追加「ctx=2 UPDATE tenant_id=2 命中 5 行、tenant_id=1 二次影响 0 行」双租户形态过滤；
 *   C6 手写 SQL 追加「LEFT JOIN 分组后 dept.id 集合」内容断言。
 *   与 SYS-001.A 真实 API 级回归的内容断言互补，脚本侧自证充分。
 *
 * 用法：node scripts/db/run-db018-verify.mjs [--self-test]
 */
import { execFileSync, spawnSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));
const selfTest = process.argv.includes('--self-test');

function fail(code, message) { console.error(message); process.exit(code); }
const dockerUp = spawnSync('docker', ['version', '--format', '{{.Server.Version}}'], { encoding: 'utf8' });
if (dockerUp.error || dockerUp.status !== 0) fail(3, `[db018] Docker 不可用（${dockerUp.error?.message ?? `exit=${dockerUp.status}`}）：验证不得静默跳过`);

const container = `zszj-db018-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
const port = 2832 + Math.floor(Math.random() * 800);
let cleaned = false;
const cleanup = () => { if (!cleaned) { cleaned = true; try { execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { } } };
process.on('exit', cleanup);
process.on('SIGINT', () => { cleanup(); process.exit(130); });

console.log(`[db018] 拉起临时 PG（${container} @ 127.0.0.1:${port}）…`);
execFileSync('docker', ['run', '-d', '--name', container, '-e', 'POSTGRES_PASSWORD=db018', '-p', `127.0.0.1:${port}:5432`, 'postgres:17-alpine'], { stdio: 'ignore' });

const psql = (user, db, sql, onErrorStop = true) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', user, '-d', db,
  ...(onErrorStop ? ['-v', 'ON_ERROR_STOP=1'] : []), '-q'], { input: sql, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 });
const psqlOut = (user, db, sql) => spawnSync('docker', ['exec', container, 'psql', '-U', user, '-d', db, '-At', '-c', sql], { encoding: 'utf8', maxBuffer: 8 * 1024 * 1024 });
// 计数断言以超管读取（隔离语义与角色无关；写路径以 zhongshu_app 执行，贴近应用连接）
const count = (sql) => psqlOut('postgres', 'zhongshu', sql).stdout.trim();

// 就绪等待
let ready = false;
for (let i = 0; i < 30; i++) {
  if (psqlOut('postgres', 'postgres', 'SELECT 1').status === 0) { ready = true; break; }
  Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500);
}
if (!ready) { cleanup(); fail(1, '[db018] PG 未就绪'); }

let pass = 0, failCount = 0;
const results = [];
const record = (id, ok, note = '') => { results.push({ id, ok, note }); ok ? pass++ : failCount++; console.log(`[${ok ? 'PASS' : 'FAIL'}] ${id} ${note}`); };

// 环境：建库 + 环境方案角色 + V1/V2 迁移（以 owner 身份 = 迁移账号）
execFileSync('docker', ['exec', container, 'psql', '-U', 'postgres', '-q', '-c', 'CREATE DATABASE zhongshu;'], { stdio: 'ignore' });
{
  const setup = readFileSync(join(root, 'services/zhongshu-core/sql/postgresql/env-setup-test.sql'), 'utf8');
  const r = psql('postgres', 'zhongshu', setup);
  if (r.status !== 0) { cleanup(); fail(1, `[db018] 角色授权失败:\n${r.stdout}${r.stderr}`); }
}
{
  const v1 = readFileSync(join(root, 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260909.001__system_infra_baseline.sql'), 'utf8');
  const v2 = readFileSync(join(root, 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260909.002__infra_quartz_backfill.sql'), 'utf8');
  const r = psql('zhongshu_owner', 'zhongshu', v1 + '\n' + v2);
  if (r.status !== 0) { cleanup(); fail(1, `[db018] V1/V2 以迁移账号执行失败:\n${r.stdout}${r.stderr}`); }
}
console.log('[db018] 环境就绪：V1+V2 已由迁移账号（zhongshu_owner）执行');

// 造数：两个 System 技术租户（tenant_id=1 / tenant_id=2）跨四类表，含 C3 伪造跨租户外键与 C9/C10 令牌。
// 批量插入以多行 VALUES 一次性写入（覆盖卡片「批量」要求），全部经 zhongshu_app（应用连接）执行。
{
  const seed = `
BEGIN;
INSERT INTO system_dept (id, name, parent_id, sort, status, creator, create_time, updater, update_time, deleted, tenant_id) VALUES
 (900101, 'DB018-T1-研发部', 900100, 1, 0, 'db018', now(), 'db018', now(), 0, 1),
 (900102, 'DB018-T1-市场部', 900100, 2, 0, 'db018', now(), 'db018', now(), 0, 1),
 (900103, 'DB018-T2-研发部', 900100, 1, 0, 'db018', now(), 'db018', now(), 0, 2),
 (900104, 'DB018-T2-财务部', 900100, 2, 0, 'db018', now(), 'db018', now(), 0, 2),
 (900111, 'DB018-T1-PG-1', 900100, 11, 0, 'db018', now(), 'db018', now(), 0, 1),
 (900112, 'DB018-T1-PG-2', 900100, 12, 0, 'db018', now(), 'db018', now(), 0, 1),
 (900113, 'DB018-T1-PG-3', 900100, 13, 0, 'db018', now(), 'db018', now(), 0, 1),
 (900114, 'DB018-T1-PG-4', 900100, 14, 0, 'db018', now(), 'db018', now(), 0, 1),
 (900115, 'DB018-T1-PG-5', 900100, 15, 0, 'db018', now(), 'db018', now(), 0, 1),
 (900121, 'DB018-T2-PG-1', 900100, 11, 0, 'db018', now(), 'db018', now(), 0, 2),
 (900122, 'DB018-T2-PG-2', 900100, 12, 0, 'db018', now(), 'db018', now(), 0, 2),
 (900123, 'DB018-T2-PG-3', 900100, 13, 0, 'db018', now(), 'db018', now(), 0, 2);
INSERT INTO system_users (id, username, password, nickname, dept_id, status, creator, create_time, updater, update_time, deleted, tenant_id) VALUES
 (900201, 'db018_t1_u1', '', 'T1用户1', 900101, 0, 'db018', now(), 'db018', now(), 0, 1),
 (900202, 'db018_t1_u2', '', 'T1用户2', 900102, 0, 'db018', now(), 'db018', now(), 0, 1),
 (900203, 'db018_t2_u1', '', 'T2用户1', 900103, 0, 'db018', now(), 'db018', now(), 0, 2),
 (900204, 'db018_t1_u3', '', 'T1用户3-伪造跨租户部门', 900103, 0, 'db018', now(), 'db018', now(), 0, 1);
INSERT INTO system_role (id, name, code, sort, data_scope, data_scope_dept_ids, status, type, creator, create_time, updater, update_time, deleted, tenant_id) VALUES
 (900301, 'DB018-T1-角色', 'db018_t1_role', 1, 1, '', 0, 2, 'db018', now(), 'db018', now(), 0, 1),
 (900302, 'DB018-T2-角色', 'db018_t2_role', 1, 1, '', 0, 2, 'db018', now(), 'db018', now(), 0, 2);
INSERT INTO system_oauth2_access_token (id, user_id, user_type, user_info, access_token, refresh_token, client_id, expires_time, creator, create_time, updater, update_time, deleted, tenant_id) VALUES
 (900401, 900201, 2, '{}', 'db018-t1-token', 'db018-t1-rt', 'default', '2026-12-31 12:00:00', 'db018', now(), 'db018', now(), 0, 1),
 (900402, 900203, 2, '{}', 'db018-t2-token', 'db018-t2-rt', 'default', '2026-12-31 12:00:00', 'db018', now(), 'db018', now(), 0, 2),
 (900403, 900201, 2, '{}', 'db018-t1-expired', 'db018-t1-rtx', 'default', '2026-01-01 12:00:00', 'db018', now(), 'db018', now(), 0, 1),
 (900404, 900203, 2, '{}', 'db018-t2-expired', 'db018-t2-rtx', 'default', '2026-01-01 12:00:00', 'db018', now(), 'db018', now(), 0, 2);
COMMIT;`;
  const r = psql('zhongshu_app', 'zhongshu', seed);
  if (r.status !== 0) { cleanup(); fail(1, `[db018] 两租户造数失败（app 账号）:\n${r.stdout}${r.stderr}`); }
  console.log('[db018] 造数就绪：tenant_id=1/2 双技术租户（dept/users/role/oauth2_access_token）');
}

// C1 CRUD 读取隔离：SELECT 注入 tenant_id=ctx
{
  const own = count("SELECT count(*) FROM system_dept WHERE deleted=0 AND tenant_id=1 AND name LIKE 'DB018-T1-%'");
  const leak = count("SELECT count(*) FROM system_dept WHERE deleted=0 AND tenant_id=1 AND name LIKE 'DB018-T2-%'");
  record('C1 CRUD 读取隔离（SELECT 注入 tenant_id=ctx）', own === '7' && leak === '0', `本租户可见=${own}（期望7） 他租户可见=${leak}（期望0）`);
}

// C2 分页隔离：LIMIT/OFFSET 在 tenant_id=ctx 之内；忽略路径跨两租户作对照
{
  const pageBad = count("SELECT count(*) FROM (SELECT tenant_id FROM system_dept WHERE deleted=0 AND tenant_id=1 AND name LIKE 'DB018-T1-PG-%' ORDER BY id LIMIT 3 OFFSET 0) p WHERE p.tenant_id <> 1");
  const scopedTenants = count("SELECT count(DISTINCT tenant_id) FROM system_dept WHERE deleted=0 AND tenant_id=1 AND name LIKE 'DB018-%-PG-%'");
  const ignoredTenants = count("SELECT count(DISTINCT tenant_id) FROM system_dept WHERE deleted=0 AND name LIKE 'DB018-%-PG-%'");
  const t1Page = count("SELECT count(*) FROM system_dept WHERE deleted=0 AND tenant_id=1 AND name LIKE 'DB018-T1-PG-%'");
  // P2 硬化 #2：断言分页 LIMIT 3 返回的具体行内容（前 3 行 name 序列 = T1-PG-1/2/3）
  const page3Names = psqlOut('postgres', 'zhongshu', "SELECT string_agg(name, ',' ORDER BY id) FROM (SELECT id, name FROM system_dept WHERE deleted=0 AND tenant_id=1 AND name LIKE 'DB018-T1-PG-%' ORDER BY id LIMIT 3 OFFSET 0) p").stdout.trim();
  record('C2 分页隔离（页内不越租户；作用域仅本租户，忽略路径跨租户；分页结果内容断言）',
    pageBad === '0' && scopedTenants === '1' && ignoredTenants === '2' && t1Page === '5'
      && page3Names === 'DB018-T1-PG-1,DB018-T1-PG-2,DB018-T1-PG-3',
    `页内越界=${pageBad} 作用域租户数=${scopedTenants} 忽略路径租户数=${ignoredTenants} T1分页集=${t1Page} 前3行=${page3Names}`);
}

// C3 关联隔离：JOIN 两端注入 tenant_id；伪造跨租户外键被排除
{
  const joined = count("SELECT count(*) FROM system_users u JOIN system_dept d ON u.dept_id=d.id AND d.tenant_id=1 AND d.deleted=0 WHERE u.tenant_id=1 AND u.deleted=0 AND u.username LIKE 'db018_t1_%'");
  const forgedDept = count("SELECT count(*) FROM system_dept WHERE id=900103 AND tenant_id=1 AND deleted=0");
  // P2 硬化 #1：断言 JOIN 到的用户列数据内容（username@dept.name 序列 = u1@研发部, u2@市场部；u3 因跨租户 dept 被 JOIN 排除）
  const joinRows = psqlOut('postgres', 'zhongshu', "SELECT string_agg(u.username || '@' || d.name, ',' ORDER BY u.id) FROM system_users u JOIN system_dept d ON u.dept_id=d.id AND d.tenant_id=1 AND d.deleted=0 WHERE u.tenant_id=1 AND u.deleted=0 AND u.username LIKE 'db018_t1_%'").stdout.trim();
  record('C3 关联隔离（JOIN 两端注入 tenant_id；伪造跨租户外键被排除；JOIN 列内容断言）',
    joined === '2' && forgedDept === '0'
      && joinRows === 'db018_t1_u1@DB018-T1-研发部,db018_t1_u2@DB018-T1-市场部',
    `本租户关联命中=${joined}（期望2，u3跨租户被排除） 伪造部门在ctx=1可见=${forgedDept}（期望0） JOIN内容=${joinRows}`);
}

// C4 批量隔离：批量 UPDATE 作用域限本租户，他租户零影响（批量插入见造数多行 VALUES）
{
  const beforeT2 = count("SELECT count(*) FROM system_dept WHERE tenant_id=2 AND name LIKE 'DB018-T2-%' AND sort >= 100");
  const upd = psql('zhongshu_app', 'zhongshu', "UPDATE system_dept SET sort = sort + 100 WHERE deleted=0 AND tenant_id=1 AND name LIKE 'DB018-T1-%'");
  const t1Updated = count("SELECT count(*) FROM system_dept WHERE tenant_id=1 AND name LIKE 'DB018-T1-%' AND sort >= 100");
  const afterT2 = count("SELECT count(*) FROM system_dept WHERE tenant_id=2 AND name LIKE 'DB018-T2-%' AND sort >= 100");
  // P2 硬化 #3：过滤条件同时覆盖双租户形态——ctx=2 时 UPDATE tenant_id=2 命中 5 行、tenant_id=1 二次影响 0 行
  const beforeT1 = count("SELECT count(*) FROM system_dept WHERE tenant_id=1 AND name LIKE 'DB018-T1-%' AND sort >= 200");
  const upd2 = psql('zhongshu_app', 'zhongshu', "UPDATE system_dept SET sort = sort + 100 WHERE deleted=0 AND tenant_id=2 AND name LIKE 'DB018-T2-%'");
  const t2Updated = count("SELECT count(*) FROM system_dept WHERE tenant_id=2 AND name LIKE 'DB018-T2-%' AND sort >= 100");
  const afterT1 = count("SELECT count(*) FROM system_dept WHERE tenant_id=1 AND name LIKE 'DB018-T1-%' AND sort >= 200");
  record('C4 批量隔离（批量 UPDATE 作用域限本租户，他租户零影响；双租户形态过滤均命中）',
    upd.status === 0 && beforeT2 === '0' && t1Updated === '7' && afterT2 === '0'
      && upd2.status === 0 && beforeT1 === '0' && t2Updated === '5' && afterT1 === '0',
    `T1批量更新=${t1Updated}（期望7） T2受影响=${afterT2}（期望0） T2批量更新=${t2Updated}（期望5） T1二次受影响=${afterT1}（期望0）`);
}

// C5 逻辑删除隔离：本租户生效；跨租户按 PK 逻辑删除被拒（0 行）
{
  const r = psql('zhongshu_app', 'zhongshu', `DO $$ DECLARE a int; BEGIN
    UPDATE system_dept SET deleted=1 WHERE id=900102 AND tenant_id=1 AND deleted=0;
    GET DIAGNOSTICS a=ROW_COUNT;
    IF a<>1 THEN RAISE EXCEPTION 'C5 本租户逻辑删除应影响1行，实际 %', a; END IF;
    UPDATE system_dept SET deleted=1 WHERE id=900103 AND tenant_id=1 AND deleted=0;
    GET DIAGNOSTICS a=ROW_COUNT;
    IF a<>0 THEN RAISE EXCEPTION 'C5 跨租户逻辑删除未被拒绝，影响 % 行', a; END IF;
  END $$;`);
  const t2Active = count("SELECT count(*) FROM system_dept WHERE id=900103 AND tenant_id=2 AND deleted=0");
  const t1Hidden = count("SELECT count(*) FROM system_dept WHERE id=900102 AND tenant_id=1 AND deleted=0");
  record('C5 逻辑删除隔离（本租户生效；跨租户按 PK 被拒）',
    r.status === 0 && t2Active === '1' && t1Hidden === '0',
    `${r.status === 0 ? 'DO校验通过' : (r.stderr.split('\n')[0] ?? '')} 租户2目标仍活跃=${t2Active}（期望1） 租户1已删隐藏=${t1Hidden}（期望0）`);
}

// C6 手写 SQL 隔离：自定义聚合/LEFT JOIN 注入 tenant_id=ctx
{
  const groups = psqlOut('zhongshu_app', 'zhongshu', "SELECT count(*) FROM (SELECT d.id FROM system_dept d LEFT JOIN system_users u ON u.dept_id=d.id AND u.deleted=0 AND u.tenant_id=1 WHERE d.deleted=0 AND d.tenant_id=1 AND d.name LIKE 'DB018-T1-PG-%' GROUP BY d.id) t").stdout.trim();
  const leak = count("SELECT count(*) FROM system_dept d WHERE d.deleted=0 AND d.tenant_id=1 AND d.name LIKE 'DB018-T2-%'");
  // P2 硬化 #1（C6 侧）：断言 LEFT JOIN 分组后 dept.id 集合内容（T1-PG-1~5 = 900111~900115）
  const deptIds = psqlOut('zhongshu_app', 'zhongshu', "SELECT string_agg(id::text, ',' ORDER BY id) FROM (SELECT d.id FROM system_dept d LEFT JOIN system_users u ON u.dept_id=d.id AND u.deleted=0 AND u.tenant_id=1 WHERE d.deleted=0 AND d.tenant_id=1 AND d.name LIKE 'DB018-T1-PG-%' GROUP BY d.id) t").stdout.trim();
  record('C6 手写 SQL 隔离（自定义聚合/JOIN 注入 tenant_id=ctx；分组 ID 集内容断言）',
    groups === '5' && leak === '0' && deptIds === '900111,900112,900113,900114,900115',
    `本租户分组数=${groups}（期望5） 他租户可见=${leak}（期望0） 分组ID集=${deptIds}`);
}

// C7 伪造上下文/他租户对象 ID 被拒绝：ctx=1 按 PK 读/改/删租户2 对象均 0 行
{
  const r = psql('zhongshu_app', 'zhongshu', `DO $$ DECLARE a int; c int; BEGIN
    SELECT count(*) INTO c FROM system_dept WHERE id=900103 AND tenant_id=1 AND deleted=0;
    IF c<>0 THEN RAISE EXCEPTION 'C7 越权读未拒绝：% 行', c; END IF;
    UPDATE system_users SET status=1 WHERE id=900203 AND tenant_id=1;
    GET DIAGNOSTICS a=ROW_COUNT;
    IF a<>0 THEN RAISE EXCEPTION 'C7 越权改未拒绝：% 行', a; END IF;
    DELETE FROM system_role WHERE id=900302 AND tenant_id=1;
    GET DIAGNOSTICS a=ROW_COUNT;
    IF a<>0 THEN RAISE EXCEPTION 'C7 越权删未拒绝：% 行', a; END IF;
  END $$;`);
  const t2User = count("SELECT count(*) FROM system_users WHERE id=900203 AND tenant_id=2 AND status=0");
  const t2Role = count("SELECT count(*) FROM system_role WHERE id=900302 AND tenant_id=2 AND deleted=0");
  record('C7 伪造上下文/他租户对象 ID 被拒绝（读/改/删均 0 行，目标完好）',
    r.status === 0 && t2User === '1' && t2Role === '1',
    `${r.status === 0 ? 'DO校验通过' : (r.stderr.split('\n')[0] ?? '')} 租户2用户完好=${t2User}（期望1） 租户2角色完好=${t2Role}（期望1）`);
}

// C8 全局表合法范围：无 tenant_id 列者（BaseDO+@TenantIgnore）结构性不可租户化
{
  const col = (t) => count(`SELECT count(*) FROM information_schema.columns WHERE table_schema='public' AND table_name='${t}' AND column_name='tenant_id'`);
  const dict = col('system_dict_data'), jobLog = col('infra_job_log');
  const dept = col('system_dept'), users = col('system_users'), role = col('system_role');
  record('C8 全局表合法范围（全局表无 tenant_id 列；租户感知表有）',
    dict === '0' && jobLog === '0' && dept === '1' && users === '1' && role === '1',
    `dict_data=${dict} job_log=${jobLog}（期望0，全局） dept=${dept} users=${users} role=${role}（期望1，租户感知）`);
}

// C9 忽略注解合法范围：表有 tenant_id，仅注解方法（selectByAccessToken）显式绕过
{
  const col = count("SELECT count(*) FROM information_schema.columns WHERE table_schema='public' AND table_name='system_oauth2_access_token' AND column_name='tenant_id'");
  const scoped = count("SELECT count(*) FROM system_oauth2_access_token WHERE access_token='db018-t2-token' AND tenant_id=1 AND deleted=0");
  const ignored = count("SELECT count(*) FROM system_oauth2_access_token WHERE access_token='db018-t2-token' AND deleted=0");
  record('C9 忽略注解合法范围（作用域内隔离；仅 @TenantIgnore 方法路径显式绕过）',
    col === '1' && scoped === '0' && ignored === '1',
    `tenant_id列=${col}（期望1） ctx=1作用域查T2令牌=${scoped}（期望0，隔离） 忽略路径查T2令牌=${ignored}（期望1，合法令牌校验）`);
}

// C10 系统清理合法范围：TokenCleanJob @TenantIgnore → 跨租户全局有界清过期令牌
{
  const deadline = '2026-06-01 00:00:00';
  const before = count(`SELECT count(*) FROM system_oauth2_access_token WHERE expires_time < '${deadline}' AND access_token LIKE 'db018-%-expired'`);
  const del = psql('postgres', 'zhongshu', `DELETE FROM system_oauth2_access_token WHERE id IN (SELECT id FROM system_oauth2_access_token WHERE expires_time < '${deadline}' AND access_token LIKE 'db018-%-expired' LIMIT 10)`);
  const after = count(`SELECT count(*) FROM system_oauth2_access_token WHERE expires_time < '${deadline}' AND access_token LIKE 'db018-%-expired'`);
  const futureKept = count("SELECT count(*) FROM system_oauth2_access_token WHERE access_token IN ('db018-t1-token','db018-t2-token') AND deleted=0");
  record('C10 系统清理合法范围（清理 Job 忽略租户，跨租户有界清过期，未过期保留）',
    del.status === 0 && before === '2' && after === '0' && futureKept === '2',
    `清理前过期=${before}（期望2，跨两租户） 清理后过期=${after}（期望0） 未过期保留=${futureKept}（期望2）`);
}

// 负向对照（--self-test）：证明隔离断言非空洞——同一行作用域内不可见、忽略路径可见
if (selfTest) {
  const scoped = count("SELECT count(*) FROM system_dept WHERE id=900103 AND tenant_id=1 AND deleted=0");
  const ignored = count("SELECT count(*) FROM system_dept WHERE id=900103 AND deleted=0");
  record('S0 负向对照（隔离非空洞：作用域内 0，忽略路径可见同一行）', scoped === '0' && ignored === '1', `作用域=${scoped}（期望0） 忽略=${ignored}（期望1）`);
}

console.log(JSON.stringify({ pass, fail: failCount }, null, 0));
cleanup();
process.exit(failCount ? 1 : 0);
