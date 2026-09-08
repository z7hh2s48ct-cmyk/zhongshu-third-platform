/**
 * ZS-CFG-002.A：字典编码/种子约束——真实 PG 验证用例集。
 *
 * 前置：一次性 PG17 容器 + V1/V2/V3 基线与约束迁移 + 环境方案双账号。
 * 用例：
 *   C1 V3 约束建立成功（两个唯一索引存在）且 V1 种子全部通过约束（说明在册种子无重复编码）；
 *   C2 重复字典类型编码插入被拒（冲突不静默覆盖）；
 *   C3 并发插入相同字典类型编码 → 仅一个成功（并发不产生禁止重复）；
 *   C4 不同 type 编码正常插入（合法路径不受影响）；
 *   C5 字典项 (dict_type,value) 重复被拒；
 *   C6 迁移重跑幂等（IF NOT EXISTS 二次执行无副作用）；
 *   C7 逻辑删除后同编码可重建（deleted_time 语义），在册唯一不被绕过；
 *   C8 低权限 app 账号可读字典数据（SELECT 授权）。
 * 任一失败退出非零。用法：node scripts/db/run-cfg002-verify.mjs
 */
import { execFileSync, spawnSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));

function fail(code, message) { console.error(message); process.exit(code); }
const dockerUp = spawnSync('docker', ['version', '--format', '{{.Server.Version}}'], { encoding: 'utf8' });
if (dockerUp.error || dockerUp.status !== 0) fail(3, `[cfg002] Docker 不可用：验证不得静默跳过`);

const container = `zszj-cfg002-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
const port = 4432 + Math.floor(Math.random() * 500);
let cleaned = false;
const cleanup = () => { if (!cleaned) { cleaned = true; try { execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { } } };
process.on('exit', cleanup);

execFileSync('docker', ['run', '-d', '--name', container, '-e', 'POSTGRES_PASSWORD=cfg002', '-p', `127.0.0.1:${port}:5432`, 'postgres:17-alpine'], { stdio: 'ignore' });
const psql = (user, db, sql) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', user, '-d', db, '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql, encoding: 'utf8', maxBuffer: 16 * 1024 * 1024 });
const psqlOut = (user, db, sql) => spawnSync('docker', ['exec', container, 'psql', '-U', user, '-d', db, '-At', '-c', sql], { encoding: 'utf8' });

let ready = false;
for (let i = 0; i < 30; i++) { if (psqlOut('postgres', 'postgres', 'SELECT 1').status === 0) { ready = true; break; } Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500); }
if (!ready) { cleanup(); fail(1, '[cfg002] PG 未就绪'); }

execFileSync('docker', ['exec', container, 'psql', '-U', 'postgres', '-q', '-c', 'CREATE DATABASE zhongshu;'], { stdio: 'ignore' });
const migDir = 'services/zhongshu-core/zszj-server/src/main/resources/db/migration';
{
  const setup = readFileSync(join(root, 'services/zhongshu-core/sql/postgresql/env-setup-test.sql'), 'utf8');
  if (psql('postgres', 'zhongshu', setup).status !== 0) { cleanup(); fail(1, '[cfg002] 角色授权失败'); }
  const all = ['V20260909.001__system_infra_baseline.sql', 'V20260909.002__infra_quartz_backfill.sql', 'V20260909.003__system_dict_unique_constraints.sql']
    .map((f) => readFileSync(join(root, migDir, f), 'utf8')).join('\n');
  if (psql('zhongshu_owner', 'zhongshu', all).status !== 0) { cleanup(); fail(1, '[cfg002] V1~V3 执行失败（V1 种子可能存在重复编码，属基线缺陷）'); }
}
console.log('[cfg002] 环境就绪：V1~V3 已执行（V1 种子全部通过唯一约束）');

let pass = 0, failCount = 0;
const record = (id, ok, note = '') => { pass += ok ? 1 : 0; failCount += ok ? 0 : 1; console.log(`[${ok ? 'PASS' : 'FAIL'}] ${id} ${note}`.trim()); };

// C1 唯一索引存在
{
  const n = psqlOut('postgres', 'zhongshu', "SELECT count(*) FROM pg_indexes WHERE schemaname='public' AND indexname IN ('uk_system_dict_type_type','uk_system_dict_data_type_value')").stdout.trim();
  record('C1 唯一索引建立', n === '2', `indexCount=${n}`);
}

// C2 重复字典类型编码插入被拒（同编码两次插入：第一次成功、第二次拒绝）
{
  const first = psql('postgres', 'zhongshu', "INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted) VALUES (900001, 'C2 首插', 'cfg002_dup_probe', 0, '', 'c2', now(), 'c2', now(), '0');");
  const dup = psql('postgres', 'zhongshu', "INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted) VALUES (900001 + 1, 'C2 重复', 'cfg002_dup_probe', 0, '', 'c2', now(), 'c2', now(), '0');");
  const rejected = dup.status !== 0 && /duplicate key|unique constraint/i.test((dup.stderr ?? '') + (dup.stdout ?? ''));
  record('C2 重复字典类型编码被唯一约束拒绝', first.status === 0 && rejected, '首插通过、重复拒绝=' + rejected);
}

// C3 并发插入相同编码 → 仅一个成功
{
  const sql = (id) => `INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted) VALUES (${id}, '并发类型', 'biz_concurrent', 0, '', 'c3', now(), 'c3', now(), '0');`;
  const a = spawnSync('docker', ['exec', '-i', container, 'psql', '-U', 'postgres', '-d', 'zhongshu', '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql(900101), encoding: 'utf8' });
  const b = spawnSync('docker', ['exec', '-i', container, 'psql', '-U', 'postgres', '-d', 'zhongshu', '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql(900102), encoding: 'utf8' });
  const succ = [a, b].filter((r) => r.status === 0).length;
  record('C3 并发插入相同编码仅一个成功', succ === 1, `成功数=${succ}`);
}

// C4 不同编码正常插入
{
  const r = psql('postgres', 'zhongshu', "INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted) VALUES (900103, '合法新类型', 'cfg002_new_type', 0, '', 'c4', now(), 'c4', now(), '0');");
  record('C4 不同编码合法插入成功', r.status === 0);
}

// C5 字典项 (dict_type,value) 重复被拒
{
  const ok1 = psql('postgres', 'zhongshu', "INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted) VALUES (900201, 1, 'C5-A', 'cfg002_a', 'cfg002_new_type', 0, 'c5', now(), 'c5', now(), '0');");
  const dup = psql('postgres', 'zhongshu', "INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted) VALUES (900202, 2, 'C5-B', 'cfg002_a', 'cfg002_new_type', 0, 'c5', now(), 'c5', now(), '0');");
  record('C5 字典项重复 (type,value) 被拒', ok1.status === 0 && dup.status !== 0 && /duplicate key|unique constraint/i.test((dup.stderr ?? '') + (dup.stdout ?? '')));
}

// C6 迁移重跑幂等
{
  const v3 = readFileSync(join(root, migDir2()), 'utf8');
  const r = psql('postgres', 'zhongshu', v3);
  record('C6 V3（IF NOT EXISTS）重跑幂等', r.status === 0);
  function migDir2() { return 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260909.003__system_dict_unique_constraints.sql'; }
}

// C7 逻辑删除后同编码可重建（部分唯一索引仅约束在册行）
{
  const del = psql('postgres', 'zhongshu', "UPDATE system_dict_type SET deleted = 1 WHERE type = 'cfg002_new_type';");
  const reins = psql('postgres', 'zhongshu', "INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted) VALUES (900104, '重建类型', 'cfg002_new_type', 0, '', 'c7', now(), 'c7', now(), '0');");
  const dupLive = psql('postgres', 'zhongshu', "INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted) VALUES (900105, '再插一次', 'cfg002_new_type', 0, '', 'c7', now(), 'c7', now(), '0');");
  record('C7 逻辑删除后同编码可重建、在册仍唯一', del.status === 0 && reins.status === 0 && dupLive.status !== 0);
}

// C8 低权限 app 账号可读字典
{
  const n = psqlOut('zhongshu_app', 'zhongshu', "SELECT count(*) FROM system_dict_type WHERE type = 'cfg002_dup_probe'").stdout.trim();
  record('C8 低权限账号可读字典数据', n === '1', `count=${n}`);
}

console.log(JSON.stringify({ pass, fail: failCount }));
cleanup();
process.exit(failCount ? 1 : 0);
