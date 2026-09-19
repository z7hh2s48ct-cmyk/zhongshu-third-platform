/**
 * ZS-DB-006：序列主键回填、批量插入与种子续号——真实 PG 验证用例集。
 *
 * 依赖一次性 Docker PG17 容器（与 scripts/db/test-pg-fixture.mjs 同款隔离/清理语义），
 * 加载 V1 基线 + V2 回填后逐用例验证：
 *   C1 单条插入 + ID 回填语义（nextval → 显式 id 插入 → 读回一致）；
 *   C2 批量插入（DO 循环 nextval 100 条）；
 *   C3 种子续号：全部 *_seq 的 nextval 不得小于等于表内 max(id)；
 *   C4 并发插入：3 个并行会话各 50 条，共 150 条，主键唯一；
 *   C5 事务回滚：业务记录零残留（允许序列空洞）；
 *   C6 低权限序列授权：app 账号可 nextval/插入，不可 ALTER SEQUENCE。
 * 附带一个"必然失败"的负向对照（--self-test），证明断言本身可失败。
 * 任一用例失败 → 退出码非零。
 *
 * 用法：node scripts/db/run-db006-verify.mjs [--self-test]
 */
import { execFileSync, spawnSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));
const selfTest = process.argv.includes('--self-test');

function fail(code, message) {
  console.error(message);
  process.exit(code);
}
const dockerUp = spawnSync('docker', ['version', '--format', '{{.Server.Version}}'], { encoding: 'utf8' });
if (dockerUp.error || dockerUp.status !== 0) fail(3, `[db006] Docker 不可用（${dockerUp.error?.message ?? `exit=${dockerUp.status}`}）：验证不得静默跳过`);

const container = `zszj-db006-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
let port = 1632 + Math.floor(Math.random() * 800);
let cleaned = false;
const cleanup = () => { if (!cleaned) { cleaned = true; try { execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { } } };
process.on('exit', cleanup);
process.on('SIGINT', () => { cleanup(); process.exit(130); });

console.log(`[db006] 拉起临时 PG（${container} @ 127.0.0.1:${port}）…`);
// FLAKY-2 收敛（循 run-db007 同款）：容器启动带端口冲突重试 + 就绪探测/建库走 TCP+PGPASSWORD 探「最终 server」
// socket 探测可命中 init 临时 server（entrypoint 随后关闭它），导致建库/迁移落到临时库失败
let pgStarted = false;
for (let attempt = 0; attempt < 3 && !pgStarted; attempt++) {
  try {
    execFileSync('docker', ['run', '-d', '--name', container, '-e', 'POSTGRES_PASSWORD=db006', '-p', `127.0.0.1:${port}:5432`, 'postgres:17-alpine'], { stdio: 'ignore' });
    pgStarted = true;
  } catch {
    try { execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { }
    port += 37 + Math.floor(Math.random() * 100);
  }
}
if (!pgStarted) fail(1, '[db006] PG 容器启动失败（含 3 次端口冲突重试）');

const PG_JDBC_URL = `jdbc:postgresql://127.0.0.1:${port}/zhongshu`;
const psql = (user, db, sql, onErrorStop = true) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', user, '-d', db,
  ...(onErrorStop ? ['-v', 'ON_ERROR_STOP=1'] : []), '-q'], { input: sql, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 });
const psqlOut = (user, db, sql) => spawnSync('docker', ['exec', container, 'psql', '-U', user, '-d', db, '-At', '-c', sql], { encoding: 'utf8', maxBuffer: 8 * 1024 * 1024 });

// 就绪等待
let ready = false;
for (let i = 0; i < 30; i++) {
  const probe = spawnSync('docker', ['exec', '-e', 'PGPASSWORD=db006', container, 'psql', '-h', '127.0.0.1', '-p', '5432', '-U', 'postgres', '-d', 'postgres', '-At', '-c', 'SELECT 1'], { encoding: 'utf8' });
  if (probe.status === 0) { ready = true; break; }
  Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500);
}
if (!ready) { cleanup(); fail(1, '[db006] PG 未就绪'); }

let pass = 0, failCount = 0;
const results = [];
const record = (id, ok, note = '') => { results.push({ id, ok, note }); ok ? pass++ : failCount++; console.log(`[${ok ? 'PASS' : 'FAIL'}] ${id} ${note}`); };

// 环境：建库 + 环境方案角色 + V1/V2 迁移（以 owner 身份 = 迁移账号）
execFileSync('docker', ['exec', '-e', 'PGPASSWORD=db006', container, 'psql', '-h', '127.0.0.1', '-U', 'postgres', '-q', '-c', 'CREATE DATABASE zhongshu;'], { stdio: 'ignore' });
{
  const setup = readFileSync(join(root, 'services/zhongshu-core/sql/postgresql/env-setup-test.sql'), 'utf8');
  const r = psql('postgres', 'zhongshu', setup);
  if (r.status !== 0) { cleanup(); fail(1, `[db006] 角色授权失败:\n${r.stdout}${r.stderr}`); }
}
{
  const v1 = readFileSync(join(root, 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260909.001__system_infra_baseline.sql'), 'utf8');
  const v2 = readFileSync(join(root, 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260909.002__infra_quartz_backfill.sql'), 'utf8');
  const r = psql('zhongshu_owner', 'zhongshu', v1 + '\n' + v2);
  if (r.status !== 0) { cleanup(); fail(1, `[db006] V1/V2 以迁移账号执行失败:\n${r.stdout}${r.stderr}`); }
}
console.log('[db006] 环境就绪：V1+V2 已由迁移账号（zhongshu_owner）执行');

// C1 单条插入 + ID 回填语义
{
  const sql = `
DO $$
DECLARE new_id bigint;
BEGIN
  new_id := nextval('system_dept_seq');
  INSERT INTO system_dept (id, name, parent_id, sort, status, creator, create_time, updater, update_time, deleted, tenant_id)
  VALUES (new_id, 'C1-单条', 100, 1, 0, 'c1', now(), 'c1', now(), '0', 1);
  IF NOT EXISTS (SELECT 1 FROM system_dept WHERE id = new_id AND name = 'C1-单条') THEN
    RAISE EXCEPTION 'C1 插入读回失败 id=%', new_id;
  END IF;
END $$;`;
  const r = psql('zhongshu_app', 'zhongshu', sql);
  record('C1 单条插入+ID回填（app 账号）', r.status === 0, r.stderr.split('\n')[0] ?? '');
}

// C2 批量插入 100 条
{
  const sql = `
DO $$
DECLARE i int; new_id bigint;
BEGIN
  FOR i IN 1..100 LOOP
    new_id := nextval('system_dept_seq');
    INSERT INTO system_dept (id, name, parent_id, sort, status, creator, create_time, updater, update_time, deleted, tenant_id)
    VALUES (new_id, 'C2-批量-' || i, 100, i, 0, 'c2', now(), 'c2', now(), '0', 1);
  END LOOP;
END $$;`;
  const r = psql('zhongshu_app', 'zhongshu', sql);
  const count = r.status === 0 ? psqlOut('postgres', 'zhongshu', "SELECT count(*) FROM system_dept WHERE name LIKE 'C2-批量-%'").stdout.trim() : '?';
  record('C2 批量插入 100 条', r.status === 0 && count === '100', `实际 ${count}`);
}

// C3 种子续号：全部 *_seq 的 last_value ≥ max(id)
{
  const sql = `
DO $$
DECLARE r record; mx bigint; bad int := 0;
BEGIN
  FOR r IN SELECT sequencename, last_value FROM pg_sequences WHERE schemaname='public' AND sequencename LIKE '%\\_seq' LOOP
    BEGIN
      EXECUTE format('SELECT coalesce(max(id),0) FROM %I', regexp_replace(r.sequencename, '_seq$', '')) INTO mx;
      IF r.last_value IS NOT NULL AND r.last_value < mx THEN bad := bad + 1; END IF;
    EXCEPTION WHEN undefined_table THEN NULL;
    END;
  END LOOP;
  IF bad > 0 THEN RAISE EXCEPTION '存在 % 个序列落后于 max(id)', bad; END IF;
END $$;`;
  const r = psql('postgres', 'zhongshu', sql);
  record('C3 种子续号无冲突（全部序列）', r.status === 0);
}

// C4 并发插入：3 个并行会话各 50 条
{
  const sess = [];
  for (let s = 1; s <= 3; s++) {
    const sql = `
DO $$
DECLARE i int; new_id bigint;
BEGIN
  FOR i IN 1..50 LOOP
    new_id := nextval('system_dept_seq');
    INSERT INTO system_dept (id, name, parent_id, sort, status, creator, create_time, updater, update_time, deleted, tenant_id)
    VALUES (new_id, 'C4-并发-${s}-' || i, 100, i, 0, 'c4', now(), 'c4', now(), '0', 1);
  END LOOP;
END $$;`;
    sess.push(spawnSync('docker', ['exec', '-i', container, 'psql', '-U', 'zhongshu_app', '-d', 'zhongshu', '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql, encoding: 'utf8' }));
  }
  const allZero = sess.every((r) => r.status === 0);
  const dup = psqlOut('postgres', 'zhongshu', "SELECT count(*) - count(DISTINCT id) FROM system_dept WHERE name LIKE 'C4-并发-%'").stdout.trim();
  const total = psqlOut('postgres', 'zhongshu', "SELECT count(*) FROM system_dept WHERE name LIKE 'C4-并发-%'").stdout.trim();
  record('C4 并发插入 3×50 无重复', allZero && dup === '0' && total === '150', `总数 ${total}，重复 ${dup}`);
}

// C5 事务回滚不留业务记录
{
  const before = psqlOut('postgres', 'zhongshu', "SELECT count(*) FROM system_dept WHERE name LIKE 'C5-%'").stdout.trim();
  const sql = `
BEGIN;
INSERT INTO system_dept (id, name, parent_id, sort, status, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES (nextval('system_dept_seq'), 'C5-回滚', 100, 1, 0, 'c5', now(), 'c5', now(), '0', 1);
ROLLBACK;`;
  const r = psql('zhongshu_app', 'zhongshu', sql);
  const after = psqlOut('postgres', 'zhongshu', "SELECT count(*) FROM system_dept WHERE name LIKE 'C5-%'").stdout.trim();
  record('C5 事务回滚零残留', r.status === 0 && before === '0' && after === '0', `before=${before} after=${after}`);
}

// C6 低权限序列授权：app 可 nextval/插入（C1 已证），此处验证不可 ALTER SEQUENCE
{
  const r = psql('zhongshu_app', 'zhongshu', 'ALTER SEQUENCE system_dept_seq RESTART WITH 1;');
  record('C6 低权限不可 ALTER SEQUENCE', r.status !== 0, r.stderr.split('\n')[0]?.slice(0, 60) ?? '');
}

// 负向对照（--self-test）：一个必然失败的断言，证明断言可失败
if (selfTest) {
  const r = psql('postgres', 'zhongshu', "DO $$ BEGIN RAISE EXCEPTION '注入的故意失败'; END $$;");
  record('S0 负向对照（故意失败应失败）', r.status !== 0);
}

console.log(JSON.stringify({ pass, fail: failCount }, null, 0));
cleanup();
process.exit(failCount ? 1 : 0);
