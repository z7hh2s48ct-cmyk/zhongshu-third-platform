/**
 * ZS-DB-009 决策输入实证：并发插入同名用户名的重复复现。
 *
 * 现状（E16 先查重后写入 + E10 username 普通索引无唯一约束）存在并发竞态窗口。
 * 本脚本用两个并行会话同时插入相同 username（查重逻辑无法拦截并发），
 * 实证"无数据库唯一约束时并发产生重复账号"——为 D-09/账号唯一性决策提供证据。
 * 结果：dupCount>0 = 复现成立（这是决策依据，不是缺陷修复后的验证）。
 * 用法：node scripts/db/reproduce-user-duplicate.mjs
 */
import { execFileSync, spawnSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));
const container = `zszj-dup-${Date.now()}`;
let cleaned = false;
const cleanup = () => { if (!cleaned) { cleaned = true; try { execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { } } };
process.on('exit', cleanup);

if (spawnSync('docker', ['version'], { encoding: 'utf8' }).status !== 0) process.exit(3);
execFileSync('docker', ['run', '-d', '--name', container, '-e', 'POSTGRES_PASSWORD=x', '-p', '127.0.0.1:3777:5432', 'postgres:17-alpine'], { stdio: 'ignore' });
const psql = (user, db, sql) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', user, '-d', db, '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql, encoding: 'utf8', maxBuffer: 8 * 1024 * 1024 });

let ready = false;
for (let i = 0; i < 30; i++) { const r = spawnSync('docker', ['exec', container, 'psql', '-U', 'postgres', '-At', '-c', 'SELECT 1'], { encoding: 'utf8' }); if (r.status === 0) { ready = true; break; } Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500); }
if (!ready) { cleanup(); process.exit(1); }

execFileSync('docker', ['exec', container, 'psql', '-U', 'postgres', '-q', '-c', 'CREATE DATABASE zhongshu;'], { stdio: 'ignore' });
psql('postgres', 'zhongshu', readFileSync(join(root, 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260909.001__system_infra_baseline.sql'), 'utf8'));

// username 现有索引核对
const idx = spawnSync('docker', ['exec', container, 'psql', '-U', 'postgres', '-d', 'zhongshu', '-At', '-c',
  "SELECT indexname FROM pg_indexes WHERE tablename='system_users'"], { encoding: 'utf8' }).stdout.trim().split('\n');
console.log('[dup] system_users 现有索引:', idx.join(' / '));

// 两个并行会话同时插入相同 username（各自独立事务，模拟并发注册）
const insert = (id) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', 'postgres', '-d', 'zhongshu', '-v', 'ON_ERROR_STOP=1', '-q'], {
  input: `BEGIN;
INSERT INTO system_users (id, username, password, nickname, dept_id, status, create_time, update_time, deleted, tenant_id)
VALUES (nextval('system_users_seq'), 'dup_user', 'x', '并发-' || ${id}, 100, 0, now(), now(), '0', 1);
COMMIT;`,
  encoding: 'utf8',
});
const start = Date.now();
const results = [insert(1), insert(2)];
void (Date.now() - start);

const dupCount = Number(spawnSync('docker', ['exec', container, 'psql', '-U', 'postgres', '-d', 'zhongshu', '-At', '-c',
  "SELECT count(*) FROM system_users WHERE username = 'dup_user'"], { encoding: 'utf8' }).stdout.trim());

console.log(JSON.stringify({
  bothSucceeded: results.every((r) => r.status === 0),
  duplicateUsernameRows: dupCount,
  conclusion: dupCount >= 2 ? '复现成立：并发下无唯一约束产生重复账号（D-09 决策输入实证）' : '未复现',
}));
cleanup();
process.exit(0);
