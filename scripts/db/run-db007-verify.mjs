/**
 * ZS-DB-007：字段映射与逻辑删除合同——真实 PG 验证用例集。
 *
 * 前置：一次性 PG17 容器 + V1/V2 基线 + 环境方案双账号（同 run-db006-verify.mjs）。
 * 用例：
 *   C1 逻辑删除/状态布尔语义：deleted 列 int2、默认 0；
 *   C2 时间语义：create_time timestamp 写入读回一致（无时区本地时间约定）；
 *   C3 长文本边界：remark varchar(500)，500 字符可入、501 拒绝；
 *   C4 JSON-in-varchar 合同：post_ids 存 JSON 数组、::jsonb 校验，坏 JSON 被拒；
 *   C5 空值合同：可空列接受 NULL，NOT NULL 列拒绝；
 *   C6 逻辑删除合同：deleted 0→1 后行仍物理存在（应用层过滤归 B02 测试链）；
 *   C7 审计列存在性：creator/updater 由服务端填充（伪造防护归应用层测试链）。
 * 任一失败退出非零。用法：node scripts/db/run-db007-verify.mjs
 */
import { execFileSync, spawnSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));

function fail(code, message) { console.error(message); process.exit(code); }
const dockerUp = spawnSync('docker', ['version', '--format', '{{.Server.Version}}'], { encoding: 'utf8' });
if (dockerUp.error || dockerUp.status !== 0) fail(3, `[db007] Docker 不可用：验证不得静默跳过`);

const container = `zszj-db007-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
const port = 2632 + Math.floor(Math.random() * 700);
let cleaned = false;
const cleanup = () => { if (!cleaned) { cleaned = true; try { execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { } } };
process.on('exit', cleanup);

execFileSync('docker', ['run', '-d', '--name', container, '-e', 'POSTGRES_PASSWORD=db007', '-p', `127.0.0.1:${port}:5432`, 'postgres:17-alpine'], { stdio: 'ignore' });
const psql = (user, db, sql) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', user, '-d', db, '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql, encoding: 'utf8', maxBuffer: 16 * 1024 * 1024 });
const psqlOut = (user, db, sql) => spawnSync('docker', ['exec', container, 'psql', '-U', user, '-d', db, '-At', '-c', sql], { encoding: 'utf8' });

let ready = false;
for (let i = 0; i < 30; i++) { if (psqlOut('postgres', 'postgres', 'SELECT 1').status === 0) { ready = true; break; } Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500); }
if (!ready) { cleanup(); fail(1, '[db007] PG 未就绪'); }

execFileSync('docker', ['exec', container, 'psql', '-U', 'postgres', '-q', '-c', 'CREATE DATABASE zhongshu;'], { stdio: 'ignore' });
{
  const setup = readFileSync(join(root, 'services/zhongshu-core/sql/postgresql/env-setup-test.sql'), 'utf8');
  if (psql('postgres', 'zhongshu', setup).status !== 0) { cleanup(); fail(1, '[db007] 角色授权失败'); }
  const v1 = readFileSync(join(root, 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260909.001__system_infra_baseline.sql'), 'utf8');
  const v2 = readFileSync(join(root, 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260909.002__infra_quartz_backfill.sql'), 'utf8');
  if (psql('zhongshu_owner', 'zhongshu', v1 + '\n' + v2).status !== 0) { cleanup(); fail(1, '[db007] V1/V2 执行失败'); }
}
console.log('[db007] 环境就绪');

let pass = 0, failCount = 0;
const record = (id, ok, note = '') => { pass += ok ? 1 : 0; failCount += ok ? 0 : 1; console.log(`[${ok ? 'PASS' : 'FAIL'}] ${id} ${note}`.trim()); };
const one = (sql) => psqlOut('postgres', 'zhongshu', sql).stdout.trim();

// C1 布尔语义
{
  const type = one("SELECT data_type FROM information_schema.columns WHERE table_name='system_users' AND column_name='deleted'");
  const def = one("SELECT column_default FROM information_schema.columns WHERE table_name='system_users' AND column_name='deleted'");
  record('C1 逻辑删除列 int2 语义与默认 0', (type === 'smallint' || type === 'integer') && def.includes('0'), `type=${type} default=${def}`);
}

// C2 时间语义
{
  const type = one("SELECT data_type FROM information_schema.columns WHERE table_name='system_users' AND column_name='create_time'");
  const sql = `DO $$ DECLARE t timestamp; BEGIN
    INSERT INTO system_users (id, username, password, nickname, dept_id, status, create_time, update_time, deleted, tenant_id)
    VALUES (900001, 'c2time', 'x', 'C2', 100, 0, '2026-01-02 03:04:05', '2026-01-02 03:04:05', '0', 1);
    SELECT create_time INTO t FROM system_users WHERE id = 900001;
    IF t <> '2026-01-02 03:04:05' THEN RAISE EXCEPTION '时间读回不一致: %', t; END IF;
    DELETE FROM system_users WHERE id = 900001;
  END $$;`;
  record('C2 timestamp 写入读回一致', psql('postgres', 'zhongshu', sql).status === 0, `type=${type}`);
}

// C3 长文本边界（system_users.remark，按 information_schema 登记的最大长度动态构造）
{
  const maxLen = Number(one("SELECT coalesce(character_maximum_length, 0) FROM information_schema.columns WHERE table_name='system_users' AND column_name='remark'"));
  if (!maxLen) { cleanup(); fail(1, '[db007] system_users.remark 未登记最大长度，用例需修正'); }
  const okMax = psql('postgres', 'zhongshu', `INSERT INTO system_users (id, username, password, nickname, remark, status, create_time, update_time, deleted, tenant_id) VALUES (900101, 'c3max', 'x', 'C3-边界', repeat('字', ${maxLen}), 0, now(), now(), '0', 1);`);
  const over = psql('postgres', 'zhongshu', `INSERT INTO system_users (id, username, password, nickname, remark, status, create_time, update_time, deleted, tenant_id) VALUES (900102, 'c3over', 'x', 'C3-超长', repeat('字', ${maxLen + 1}), 0, now(), now(), '0', 1);`);
  record(`C3 remark varchar(${maxLen}) 边界（恰好最大可入 / 超长拒）`, okMax.status === 0 && over.status !== 0, `maxLen=${maxLen}`);
}

// C4 JSON-in-varchar 合同
{
  const good = psql('postgres', 'zhongshu', `DO $$ DECLARE n int; BEGIN
    INSERT INTO system_users (id, username, password, nickname, dept_id, post_ids, status, create_time, update_time, deleted, tenant_id)
    VALUES (900201, 'c4json', 'x', 'C4', 100, '[1,2]', 0, now(), now(), '0', 1);
    SELECT jsonb_array_length(post_ids::jsonb) INTO n FROM system_users WHERE id = 900201;
    IF n <> 2 THEN RAISE EXCEPTION 'JSON 读回长度 %', n; END IF;
  END $$;`);
  const bad = psql('postgres', 'zhongshu', `DO $$ BEGIN PERFORM ('[1,2'::varchar)::jsonb; END $$;`);
  record('C4 post_ids JSON 数组合同（::jsonb 校验，坏 JSON 被拒）', good.status === 0 && bad.status !== 0);
}

// C5 空值合同
{
  const nullOk = psql('postgres', 'zhongshu', "INSERT INTO system_dept (id, name, parent_id, sort, leader_user_id, status, creator, create_time, updater, update_time, deleted, tenant_id) VALUES (900301, 'C5-空值', 100, 1, NULL, 0, 'c5', now(), 'c5', now(), '0', 1);");
  const notNull = psql('postgres', 'zhongshu', "INSERT INTO system_dept (id, name, parent_id, sort, status, creator, create_time, updater, update_time, deleted, tenant_id) VALUES (900302, NULL, 100, 2, 0, 'c5', now(), 'c5', now(), '0', 1);");
  record('C5 可空列接受 NULL / NOT NULL 列拒绝', nullOk.status === 0 && notNull.status !== 0);
}

// C6 逻辑删除合同（单次执行：插入→置 deleted=1→验证物理行仍在且标记正确）
{
  const sql = `DO $$ DECLARE n0 int; n0del int; n1 int; n2 int; BEGIN
    INSERT INTO system_dept (id, name, parent_id, sort, status, creator, create_time, updater, update_time, deleted, tenant_id)
    VALUES (900401, 'C6-逻辑删除', 100, 1, 0, 'c6', now(), 'c6', now(), '0', 1);
    SELECT count(*) INTO n0 FROM system_dept WHERE id = 900401;
    SELECT count(*) INTO n0del FROM system_dept WHERE id = 900401 AND deleted = 0;
    UPDATE system_dept SET deleted = 1 WHERE id = 900401;
    SELECT count(*) INTO n1 FROM system_dept WHERE id = 900401 AND deleted = 0;
    SELECT count(*) INTO n2 FROM system_dept WHERE id = 900401 AND deleted = 1;
    IF n0 <> 1 OR n0del <> 1 OR n1 <> 0 OR n2 <> 1 THEN
      RAISE EXCEPTION '逻辑删除状态异常 n0=% n0del=% n1=% n2=%', n0, n0del, n1, n2;
    END IF;
  END $$;`;
  const c6 = psql('postgres', 'zhongshu', sql);
  const physical = one("SELECT count(*) FROM system_dept WHERE id = 900401 AND deleted = 1");
  const note = c6.status !== 0
    ? (c6.stderr || c6.stdout).split('\n')[0]?.slice(0, 120)
    : 'physical=' + physical;
  record('C6 逻辑删除：deleted=1 后物理行仍在（应用层过滤归 B02 测试链）', c6.status === 0 && physical === '1', note);
}

// C7 审计列存在性
{
  const has = one("SELECT count(*) FROM information_schema.columns WHERE table_name='system_dept' AND column_name IN ('creator','updater')");
  record('C7 审计列 creator/updater 存在（服务端填充，伪造防护归应用层测试链）', has === '2');
}

console.log(JSON.stringify({ pass, fail: failCount }));
cleanup();
process.exit(failCount ? 1 : 0);
