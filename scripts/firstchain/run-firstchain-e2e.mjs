/**
 * 首链全链真实环境 E2E（ZS-FC-001/002/003；真实 server + PG17 + 真实安全链 + 真实 Flowable 引擎，非 Stub）。
 *
 * 背景：首链此前只有 H2 单测与「Stub 流程端口」的服务层 PG 套件，从未经 REST 在真实 server 上走通——
 * 本套件首轮实跑即暴露多处只在 java -jar + 真实装配下出现的缺陷（BPMN XSD 在 fat jar 内校验失败、上游 BPM 监听器查
 * 不存在的扩展表、上游候选人策略断言、通知模板 params 非 JSON、流程部署缓存在回滚后残留、boolean deleted 的 MP 方言等）。
 * 本套件把整条链路固化为回归：申请→提交(真实流程)→审批开通→负责人登录→建员工→线索下发/分配/领取/跟进/转商机/无效→
 * 三视角与 D-12 字段裁剪→跨组织隔离→停用即失权→通知待办审计，并含正反向与幂等。
 *
 * 前置（均由本脚本在夹具内完成，运行期不依赖手工数据）：
 *   - 平台组织(PLATFORM)与平台任职：**产品内目前没有任何途径创建 PLATFORM 组织/任职**（无迁移种子、无 REST、无管理工具），
 *     审批与线索下发要求操作人具备 PLATFORM 有效任职，故由本脚本经 SQL 种子（E2E_PG_CONTAINER 指向夹具 PG 容器）。
 *     这是单独登记的产品缺口，本脚本只是不被其阻塞。
 *   - 管理员作为平台运营（超管绕过动作权限，对象级资格仍由 PLATFORM 任职裁决）。
 *
 * 环境变量：E2E_BASE_URL（必填）/ E2E_PG_CONTAINER（必填，夹具 PG 容器名）/ E2E_ADMIN_USER（默认 admin）/
 *   E2E_ADMIN_PASS / E2E_TENANT_ID（默认 1）。
 * 用法：node scripts/firstchain/run-firstchain-e2e.mjs（通常由 G15 门禁在同一夹具上顺带运行）。
 * 退出码：任一 FAIL → 1；缺必需环境 → 3。
 */
import { spawnSync } from 'node:child_process';
import { mkdirSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('../../', import.meta.url));
const BASE_URL = process.env.E2E_BASE_URL;
const PG = process.env.E2E_PG_CONTAINER;
const TENANT = process.env.E2E_TENANT_ID ?? '1';
const ADMIN_USER = process.env.E2E_ADMIN_USER ?? 'admin';
const ADMIN_PASS = process.env.E2E_ADMIN_PASS ?? 'Sys001Pass';
const OUT_DIR = join(root, 'outputs', 'firstchain-e2e');
mkdirSync(OUT_DIR, { recursive: true });

if (!BASE_URL || !PG) {
  console.error('[fc-e2e] 缺少必需环境变量 E2E_BASE_URL / E2E_PG_CONTAINER（本套件需经 SQL 种子平台任职与 DB 断言，不静默跳过）');
  process.exit(3);
}

// ---------- 业务码合同（锁码；500/429 视为缺陷） ----------
const C = {
  OK: 0,
  FORBIDDEN: 403,
  UNAUTHORIZED: 401,
  APP_STATUS_CONFLICT: 1021000002,
  REJECT_REASON_REQUIRED: 1021000004,
  APPROVE_QUALIFICATION_DENIED: 1021000007,
  EMPLOYEE_ACTOR_NOT_LEADER: 1021000008,
  LEAD_NOT_EXISTS: 1070001000,
  LEAD_STATE_TRANSITION: 1070001003,
  LEAD_VERSION_CONFLICT: 1070001004,
  FIRST_CHAIN_VERSION_CONFLICT: 1009020004, // bpm 域执行器分类（条件更新 0 行读回）
  FIRST_CHAIN_STATE_CONFLICT: 1009020005,
  LEAD_STATE_CONFLICT: 1070001005,
  LEAD_ASSIGN_TARGET_NOT_IN_ORG: 1070001007,
  LEAD_CLAIM_NOT_ASSIGNEE: 1070001008,
  LEAD_ACTOR_NOT_LEADER: 1070001011,
  LEAD_VISIBLE_DENIED: 1070001013,
  LEAD_DISTRIBUTE_NOT_PLATFORM: 1070001014,
  FOLLOWUP_LEAD_TERMINAL: 1070002001,
  CONVERT_REQUIRE_FOLLOWUP: 1070003000,
  INVALIDATE_REASON_REQUIRED: 1070003002,
  INVALIDATE_REASON_DETAIL_REQUIRED: 1070003003,
};

const results = [];
const record = (id, name, ok, note = '') => {
  results.push({ id, name, ok, note: String(note).slice(0, 400) });
  console.log(`[${ok ? 'PASS' : 'FAIL'}] ${id} ${name}${note ? ` — ${String(note).slice(0, 260)}` : ''}`);
  return ok;
};
const guard = async (id, name, fn) => {
  try { await fn(); } catch (e) { record(id, name, false, `EXCEPTION: ${e.stack?.split('\n')[0] ?? e.message}`); }
};
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
/** 反向用例：业务码属于期望集合（500/429 等系统错误不得伪装成安全拒绝） */
const codeIn = (r, ...codes) => codes.includes(r.json?.code);

async function api(method, path, { token, body } = {}) {
  const headers = { 'tenant-id': TENANT };
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const res = await fetch(BASE_URL + path, { method, headers, body: body !== undefined ? JSON.stringify(body) : undefined });
  const text = await res.text();
  let json = null;
  try { json = JSON.parse(text); } catch { /* 非 JSON */ }
  return { http: res.status, json, text };
}
const data = (r) => r.json?.data;

function psql(sql) {
  const r = spawnSync('docker', ['exec', '-i', PG, 'psql', '-U', 'postgres', '-d', 'zhongshu', '-At', '-F', '|', '-v', 'ON_ERROR_STOP=1'],
    { input: sql, encoding: 'utf8' });
  if (r.status !== 0) throw new Error(`psql 失败：${(r.stderr || r.stdout).trim().slice(0, 300)}`);
  return r.stdout.trim();
}
const psqlOne = (sql) => psql(sql).split('\n').filter(Boolean).pop() ?? '';
const psqlRows = (sql) => psql(sql).split('\n').filter(Boolean).map((l) => l.split('|'));

/** 轮询（Outbox/Sink 异步最终一致）：fn 返回真值即成功 */
async function eventually(fn, { timeoutMs = 30000, stepMs = 1000 } = {}) {
  const deadline = Date.now() + timeoutMs;
  let last;
  while (Date.now() < deadline) {
    last = await fn();
    if (last) return last;
    await sleep(stepMs);
  }
  return last;
}

async function login(username, password) {
  for (let attempt = 0; attempt < 2; attempt++) {
    const r = await api('POST', '/admin-api/system/auth/login', { body: { username, password } });
    if (r.json?.code === 0) return { token: r.json.data.accessToken, userId: String(r.json.data.userId), raw: r };
    if (r.json?.code === 429 && attempt === 0) { await sleep(65000); continue; }
    return { token: null, raw: r };
  }
  return { token: null, raw: null };
}

async function permissions(token) {
  const r = await api('GET', '/admin-api/system/auth/get-permission-info', { token });
  return new Set(data(r)?.permissions ?? []);
}

// ======================================================================
// S：夹具前置——平台组织 + 平台运营任职（产品内无创建途径，见头注释）
// ======================================================================
let admin = null; // { token, userId }
let platformOrgId = null;
await guard('S1', '前置：种子平台组织(PLATFORM) + 管理员平台有效任职（产品内无创建途径，登记缺口）', async () => {
  platformOrgId = psqlOne(`
    INSERT INTO system_organization (id,name,code,type,parent_id,sort,status,remark,creator,updater,tenant_id)
    SELECT nextval('system_organization_seq'),'E2E 平台运营部','E2E-PLATFORM',1,0,0,0,'首链 E2E 种子','e2e','e2e',${TENANT}
    WHERE NOT EXISTS (SELECT 1 FROM system_organization WHERE code='E2E-PLATFORM' AND tenant_id=${TENANT} AND deleted=0);
    INSERT INTO system_membership (id,user_id,organization_id,status,valid_from,is_primary,creator,updater,tenant_id)
    SELECT nextval('system_membership_seq'),u.id,(SELECT id FROM system_organization WHERE code='E2E-PLATFORM' AND tenant_id=${TENANT} AND deleted=0),1,CURRENT_TIMESTAMP,1,'e2e','e2e',${TENANT}
    FROM system_users u WHERE u.username='${ADMIN_USER}' AND u.tenant_id=${TENANT} AND u.deleted=0
      AND NOT EXISTS (SELECT 1 FROM system_membership m WHERE m.user_id=u.id AND m.deleted=0 AND m.is_primary=1);
    SELECT id FROM system_organization WHERE code='E2E-PLATFORM' AND tenant_id=${TENANT} AND deleted=0;`);
  const l = await login(ADMIN_USER, ADMIN_PASS);
  admin = l.token ? { token: l.token, userId: l.userId } : null;
  const tenure = psqlOne(`SELECT o.type FROM system_membership m JOIN system_organization o ON o.id=m.organization_id
                          WHERE m.user_id=${admin?.userId ?? 0} AND m.deleted=0 AND m.is_primary=1 AND m.status=1`);
  record('S1', '前置：种子平台组织(PLATFORM) + 管理员平台有效任职（产品内无创建途径，登记缺口）',
    !!admin && tenure === '1', `platformOrg=${platformOrgId} adminUserId=${admin?.userId} 主任职组织类型=${tenure}`);
});
if (!admin) { console.error('[fc-e2e] 管理员不可用，终止'); finish(1); }

// ======================================================================
// 申请开通链（franchisee A）
// ======================================================================
const A = { label: 'A' };
const B = { label: 'B' };

/** 创建 + 提交一份申请，返回 {id, appKey}（不审批） */
async function createAndSubmit(label) {
  const cr = await api('POST', '/admin-api/firstchain/application/create', {
    token: admin.token, body: { applicantName: `E2E 加盟商${label}-${Date.now()}`, contactName: `联系人${label}`, contactPhone: '13900001111' } });
  const id = data(cr);
  const got = await api('GET', `/admin-api/firstchain/application/get?id=${id}`, { token: admin.token });
  const sub = await api('POST', '/admin-api/firstchain/application/submit', { token: admin.token, body: { id, expectedVersion: data(got)?.version } });
  return { id, appKey: data(got)?.appKey, createCode: cr.json?.code, getCode: got.json?.code, submitCode: sub.json?.code, draft: data(got) };
}

await guard('A1', '申请创建 → DRAFT（服务端生成申请编号）', async () => {
  const cr = await api('POST', '/admin-api/firstchain/application/create', {
    token: admin.token, body: { applicantName: `E2E 加盟商A-${Date.now()}`, contactName: '联系人A', contactPhone: '13900001111' } });
  A.id = data(cr);
  const got = await api('GET', `/admin-api/firstchain/application/get?id=${A.id}`, { token: admin.token });
  A.appKey = data(got)?.appKey; A.version = data(got)?.version;
  record('A1', '申请创建 → DRAFT（服务端生成申请编号）',
    cr.json?.code === 0 && data(got)?.status === 'DRAFT' && /^FC\d{8}-[A-Z0-9]{8}$/.test(A.appKey ?? ''), `code=${cr.json?.code} appKey=${A.appKey} status=${data(got)?.status}`);
});

await guard('A2', '申请提交 → SUBMITTED（真实 Flowable：部署+实例+绑定；fat jar/监听器/候选人策略回归）', async () => {
  const sub = await api('POST', '/admin-api/firstchain/application/submit', { token: admin.token, body: { id: A.id, expectedVersion: A.version } });
  const got = await api('GET', `/admin-api/firstchain/application/get?id=${A.id}`, { token: admin.token });
  const binding = psqlOne(`SELECT status || ':' || process_instance_id FROM bpm_first_chain_process_binding WHERE domain_type='franchisee_application' AND domain_id=${A.id} ORDER BY id DESC LIMIT 1`);
  const task = psqlOne(`SELECT assignee_ FROM act_ru_task WHERE proc_inst_id_ = '${binding.split(':')[1] ?? ''}'`);
  A.version = data(got)?.version;
  record('A2', '申请提交 → SUBMITTED（真实 Flowable：部署+实例+绑定；fat jar/监听器/候选人策略回归）',
    sub.json?.code === 0 && data(got)?.status === 'SUBMITTED' && binding.startsWith('BOUND:') && task === admin.userId,
    `submit=${sub.json?.code}(${sub.json?.msg ?? ''}) status=${data(got)?.status} 绑定=${binding.split(':')[0]} 审批任务assignee=${task}（期望 ${admin.userId}）`);
});

await guard('A3', '审批待办已注册（MSG-002，收件人=审批人，PENDING）', async () => {
  const row = await eventually(() => {
    const r = psqlRows(`SELECT status, recipient_id FROM system_notify_todo WHERE todo_key LIKE '%${A.appKey}%' AND deleted = FALSE`)[0];
    return r ?? null;
  }, { timeoutMs: 15000 });
  record('A3', '审批待办已注册（MSG-002，收件人=审批人，PENDING）', row?.[0] === 'PENDING' && row?.[1] === admin.userId,
    `todo=${JSON.stringify(row)}`);
});

await guard('A4', '审批通过 → 幂等开通：返回组织编号 + 一次性初始密码，申请 APPROVED', async () => {
  const ap = await api('POST', '/admin-api/firstchain/application/approve', { token: admin.token, body: { appKey: A.appKey, reason: '资质齐备，同意开通' } });
  A.orgId = String(data(ap)?.organizationId ?? ''); A.leaderPassword = data(ap)?.initialPassword;
  const got = await api('GET', `/admin-api/firstchain/application/get?id=${A.id}`, { token: admin.token });
  record('A4', '审批通过 → 幂等开通：返回组织编号 + 一次性初始密码，申请 APPROVED',
    ap.json?.code === 0 && !!A.orgId && typeof A.leaderPassword === 'string' && A.leaderPassword.length >= 12 && data(got)?.status === 'APPROVED',
    `code=${ap.json?.code}(${ap.json?.msg ?? ''}) org=${A.orgId} 密码长度=${A.leaderPassword?.length} status=${data(got)?.status}`);
});

await guard('A5', '重复审批被幂等吸收：同一组织、不重发密码、不重复建主体', async () => {
  const again = await api('POST', '/admin-api/firstchain/application/approve', { token: admin.token, body: { appKey: A.appKey, reason: '重复回调' } });
  const orgs = psqlOne(`SELECT count(*) FROM system_organization WHERE code='${A.appKey}' AND tenant_id=${TENANT} AND deleted=0`);
  const leaders = psqlOne(`SELECT count(*) FROM system_users WHERE username LIKE 'fc%${A.appKey.replace(/[^A-Za-z0-9]/g, '').toLowerCase()}' AND tenant_id=${TENANT} AND deleted=0`);
  record('A5', '重复审批被幂等吸收：同一组织、不重发密码、不重复建主体',
    again.json?.code === 0 && String(data(again)?.organizationId) === A.orgId && data(again)?.initialPassword == null && orgs === '1' && leaders === '1',
    `code=${again.json?.code} 同org=${String(data(again)?.organizationId) === A.orgId} 密码=${data(again)?.initialPassword} 组织数=${orgs} 负责人账号数=${leaders}`);
});

await guard('A6', '开通主体落库：FRANCHISEE 组织(编码=申请编号) + 负责人任职 ACTIVE + 默认角色', async () => {
  const org = psqlRows(`SELECT type, leader_user_id, status FROM system_organization WHERE id=${A.orgId}`)[0];
  A.leaderUserId = org?.[1];
  A.leaderUsername = psqlOne(`SELECT username FROM system_users WHERE id=${A.leaderUserId ?? 0}`);
  const member = psqlRows(`SELECT status, is_primary FROM system_membership WHERE user_id=${A.leaderUserId ?? 0} AND organization_id=${A.orgId} AND deleted=0`)[0];
  const roles = psqlOne(`SELECT string_agg(code, ',' ORDER BY code) FROM system_role WHERE tenant_id=${TENANT} AND code LIKE 'firstchain:franchisee:%' AND deleted=0`);
  record('A6', '开通主体落库：FRANCHISEE 组织(编码=申请编号) + 负责人任职 ACTIVE + 默认角色',
    org?.[0] === '3' && !!A.leaderUserId && member?.[0] === '1' && member?.[1] === '1' && roles.includes('leader') && roles.includes('member'),
    `org类型=${org?.[0]} leader=${A.leaderUserId}(${A.leaderUsername}) 任职=${JSON.stringify(member)} 角色=${roles}`);
});

await guard('A7', '审批待办事件驱动流转为 COMPLETED（Outbox→Sink）', async () => {
  const st = await eventually(() => {
    const s = psqlOne(`SELECT status FROM system_notify_todo WHERE todo_key LIKE '%${A.appKey}%' AND deleted = FALSE`);
    return s === 'COMPLETED' ? s : null;
  }, { timeoutMs: 45000, stepMs: 2000 });
  const now = psqlOne(`SELECT status FROM system_notify_todo WHERE todo_key LIKE '%${A.appKey}%' AND deleted = FALSE`);
  record('A7', '审批待办事件驱动流转为 COMPLETED（Outbox→Sink）', st === 'COMPLETED', `todo.status=${now}`);
});

await guard('A8', '审计可回查：bizId=申请编号 的开通成功审计', async () => {
  const rows = psqlRows(`SELECT event_type, result FROM audit_event WHERE biz_id='${A.appKey}' AND tenant_id=${TENANT} ORDER BY id`);
  record('A8', '审计可回查：bizId=申请编号 的开通成功审计', rows.length >= 2 && rows.some((r) => r[1] === 'SUCCESS'),
    `审计事件=${JSON.stringify(rows)}`);
});

// ======================================================================
// 负责人 / 员工
// ======================================================================
const L = {}; // leader { token, userId }
await guard('L1', '负责人用一次性初始密码登录成功', async () => {
  const l = await login(A.leaderUsername, A.leaderPassword);
  L.token = l.token; L.userId = l.userId;
  record('L1', '负责人用一次性初始密码登录成功', !!l.token && L.userId === A.leaderUserId, `code=${l.raw?.json?.code}(${l.raw?.json?.msg ?? ''}) userId=${L.userId}`);
});

await guard('L2', '负责人权限面：含线索分配/员工创建/成员列表/线索查询；不含审批与下发', async () => {
  const p = L.token ? await permissions(L.token) : new Set();
  const must = ['firstchain:lead:assign', 'firstchain:lead:reassign', 'firstchain:lead:query', 'firstchain:employee:create', 'firstchain:employee:list'];
  const mustNot = ['firstchain:application:approve', 'firstchain:application:create', 'firstchain:lead:distribute'];
  const missing = must.filter((x) => !p.has(x)); const leaked = mustNot.filter((x) => p.has(x));
  record('L2', '负责人权限面：含线索分配/员工创建/成员列表/线索查询；不含审批与下发', !!L.token && !missing.length && !leaked.length,
    `缺少=${JSON.stringify(missing)} 越权=${JSON.stringify(leaked)}`);
});

await guard('L3', '反向：负责人不能审批申请（动作权限/对象级资格受控拒绝）', async () => {
  const x = await createAndSubmit('L3');
  const r = await api('POST', '/admin-api/firstchain/application/approve', { token: L.token, body: { appKey: x.appKey, reason: '越权审批' } });
  const st = data(await api('GET', `/admin-api/firstchain/application/get?id=${x.id}`, { token: admin.token }))?.status;
  record('L3', '反向：负责人不能审批申请（动作权限/对象级资格受控拒绝）', codeIn(r, C.FORBIDDEN, C.APPROVE_QUALIFICATION_DENIED) && st === 'SUBMITTED',
    `code=${r.json?.code}（期望 403/${C.APPROVE_QUALIFICATION_DENIED}）申请状态=${st}`);
});

const E1 = {}, E2 = {};
await guard('E1', '负责人创建员工 → 一次性初始密码；员工任职只进目标组织', async () => {
  const r = await api('POST', '/admin-api/firstchain/employee/create', { token: L.token, body: { username: `e2eemp${Date.now() % 100000}a`, nickname: 'E2E员工一' } });
  E1.userId = String(data(r)?.userId ?? ''); E1.username = data(r)?.username; E1.password = data(r)?.initialPassword;
  const member = psqlRows(`SELECT organization_id, status FROM system_membership WHERE user_id=${E1.userId || 0} AND deleted=0`)[0];
  const r2 = await api('POST', '/admin-api/firstchain/employee/create', { token: L.token, body: { username: `e2eemp${Date.now() % 100000}b`, nickname: 'E2E员工二' } });
  E2.userId = String(data(r2)?.userId ?? ''); E2.username = data(r2)?.username; E2.password = data(r2)?.initialPassword;
  record('E1', '负责人创建员工 → 一次性初始密码；员工任职只进目标组织',
    r.json?.code === 0 && r2.json?.code === 0 && !!E1.password && member?.[0] === A.orgId && member?.[1] === '1',
    `创建=${r.json?.code}/${r2.json?.code}(${r.json?.msg ?? ''}) 员工一任职组织=${member?.[0]}（期望 ${A.orgId}）`);
});

await guard('E2', '员工登录；权限面含领取/跟进/转化，不含分配/建员工/审批', async () => {
  const l1 = await login(E1.username, E1.password); E1.token = l1.token;
  const l2 = await login(E2.username, E2.password); E2.token = l2.token;
  const p = E1.token ? await permissions(E1.token) : new Set();
  const must = ['firstchain:lead:claim', 'firstchain:lead:followup', 'firstchain:lead:convert', 'firstchain:lead:invalidate', 'firstchain:lead:query'];
  const mustNot = ['firstchain:lead:assign', 'firstchain:lead:reassign', 'firstchain:employee:create', 'firstchain:application:approve', 'firstchain:lead:distribute'];
  const missing = must.filter((x) => !p.has(x)); const leaked = mustNot.filter((x) => p.has(x));
  record('E2', '员工登录；权限面含领取/跟进/转化，不含分配/建员工/审批', !!E1.token && !!E2.token && !missing.length && !leaked.length,
    `登录=${!!E1.token}/${!!E2.token} 缺少=${JSON.stringify(missing)} 越权=${JSON.stringify(leaked)}`);
});

await guard('E3', '成员列表（员工选择器数据源）：本组织 ACTIVE 成员，含负责人与两名员工', async () => {
  const r = await api('GET', '/admin-api/firstchain/employee/list-members', { token: L.token });
  const ids = (data(r) ?? []).map((m) => String(m.userId));
  record('E3', '成员列表（员工选择器数据源）：本组织 ACTIVE 成员，含负责人与两名员工',
    r.json?.code === 0 && [L.userId, E1.userId, E2.userId].every((x) => ids.includes(x)) && ids.length === 3, `code=${r.json?.code} ids=${JSON.stringify(ids)}`);
});

await guard('E4', '反向：员工不能创建员工', async () => {
  const r = await api('POST', '/admin-api/firstchain/employee/create', { token: E1.token, body: { username: `e2eemp${Date.now() % 100000}x`, nickname: '越权员工' } });
  record('E4', '反向：员工不能创建员工', codeIn(r, C.FORBIDDEN, C.EMPLOYEE_ACTOR_NOT_LEADER), `code=${r.json?.code}（期望 403/${C.EMPLOYEE_ACTOR_NOT_LEADER}）`);
});

// ======================================================================
// 线索链（PILOT-REQ-005~009）
// ======================================================================
const LD = {}; // 主线索（转商机）
const PHONE = '13812345678';
await guard('D1', '平台下发线索到加盟商 → DISTRIBUTED，归属由服务端写入', async () => {
  const r = await api('POST', '/admin-api/firstchain/lead/distribute', { token: admin.token,
    body: { customerName: '王客户', customerPhone: PHONE, customerWechat: 'wx_wang_2026', customerAddress: '上海市浦东新区世纪大道100号', source: '官网表单', orgId: A.orgId } });
  LD.id = String(data(r) ?? '');
  const g = await api('GET', `/admin-api/firstchain/lead/get?id=${LD.id}`, { token: admin.token });
  LD.version = data(g)?.version;
  record('D1', '平台下发线索到加盟商 → DISTRIBUTED，归属由服务端写入',
    r.json?.code === 0 && data(g)?.status === 'DISTRIBUTED' && String(data(g)?.orgId) === A.orgId, `code=${r.json?.code}(${r.json?.msg ?? ''}) status=${data(g)?.status} org=${data(g)?.orgId}`);
});

await guard('D2', '反向：负责人不能下发线索（仅平台运营）', async () => {
  const r = await api('POST', '/admin-api/firstchain/lead/distribute', { token: L.token, body: { customerName: '越权下发', orgId: A.orgId } });
  record('D2', '反向：负责人不能下发线索（仅平台运营）', codeIn(r, C.FORBIDDEN, C.LEAD_DISTRIBUTE_NOT_PLATFORM), `code=${r.json?.code}`);
});

await guard('V1', '视角（PILOT-REQ-009）：平台与归属组织负责人可见；员工（未被分配）不可见', async () => {
  const find = async (tok) => (data(await api('GET', '/admin-api/firstchain/lead/page?pageNo=1&pageSize=50', { token: tok }))?.list ?? []).some((x) => String(x.id) === LD.id);
  const [p, l, e] = [await find(admin.token), await find(L.token), await find(E1.token)];
  record('V1', '视角（PILOT-REQ-009）：平台与归属组织负责人可见；员工（未被分配）不可见', p && l && !e, `平台=${p} 负责人=${l} 员工=${e}`);
});

await guard('V2', 'D-12 字段裁剪：负责人/平台见完整手机号（F2），员工脱敏仅留尾四位', async () => {
  const lead = async (tok) => data(await api('GET', `/admin-api/firstchain/lead/get?id=${LD.id}`, { token: tok }));
  const lp = (await lead(L.token))?.customerPhone, ap = (await lead(admin.token))?.customerPhone;
  // 员工需先被分配才可见：此处仅断言负责人/平台，员工脱敏在 P2 断言
  record('V2', 'D-12 字段裁剪：负责人/平台见完整手机号（F2），员工脱敏仅留尾四位（员工侧见 P2）', lp === PHONE && ap === PHONE, `负责人=${lp} 平台=${ap}`);
});

await guard('P1', '负责人分配给本组织员工 → ASSIGNED；反向：分配给他组织成员被拒', async () => {
  const bad = await api('POST', '/admin-api/firstchain/lead/assign', { token: L.token, body: { id: LD.id, assigneeUserId: admin.userId, expectedVersion: LD.version } });
  const ok = await api('POST', '/admin-api/firstchain/lead/assign', { token: L.token, body: { id: LD.id, assigneeUserId: E1.userId, expectedVersion: LD.version } });
  const g = await api('GET', `/admin-api/firstchain/lead/get?id=${LD.id}`, { token: L.token });
  LD.version = data(g)?.version;
  record('P1', '负责人分配给本组织员工 → ASSIGNED；反向：分配给他组织成员被拒',
    codeIn(bad, C.LEAD_ASSIGN_TARGET_NOT_IN_ORG) && ok.json?.code === 0 && data(g)?.status === 'ASSIGNED' && String(data(g)?.assigneeUserId) === E1.userId,
    `越组织分配=${bad.json?.code}（期望 ${C.LEAD_ASSIGN_TARGET_NOT_IN_ORG}）分配=${ok.json?.code}(${ok.json?.msg ?? ''}) status=${data(g)?.status}`);
});

await guard('P2', '员工视角：被分配员工可见且手机号脱敏；另一员工不可见；员工不能分配', async () => {
  const mine = data(await api('GET', `/admin-api/firstchain/lead/get?id=${LD.id}`, { token: E1.token }));
  const e1page = (data(await api('GET', '/admin-api/firstchain/lead/page?pageNo=1&pageSize=50', { token: E1.token }))?.list ?? []).some((x) => String(x.id) === LD.id);
  const e2page = (data(await api('GET', '/admin-api/firstchain/lead/page?pageNo=1&pageSize=50', { token: E2.token }))?.list ?? []).some((x) => String(x.id) === LD.id);
  const e2get = await api('GET', `/admin-api/firstchain/lead/get?id=${LD.id}`, { token: E2.token });
  const assign = await api('POST', '/admin-api/firstchain/lead/assign', { token: E1.token, body: { id: LD.id, assigneeUserId: E2.userId, expectedVersion: LD.version } });
  const ph = mine?.customerPhone;
  const masked = typeof ph === 'string' && ph !== PHONE && ph.endsWith(PHONE.slice(-4)) && ph.includes('*');
  record('P2', '员工视角：被分配员工可见且手机号脱敏；另一员工不可见；员工不能分配',
    !!mine && masked && e1page && !e2page && codeIn(e2get, C.LEAD_VISIBLE_DENIED, C.LEAD_NOT_EXISTS) && codeIn(assign, C.FORBIDDEN, C.LEAD_ACTOR_NOT_LEADER),
    `员工手机号=${ph} 脱敏=${masked} 本人可见=${e1page} 他人可见=${e2page} 他人get=${e2get.json?.code} 员工分配=${assign.json?.code}`);
});

await guard('C1', '领取：非被分配员工被拒；版本过期冲突；被分配员工领取成功 → FOLLOWING', async () => {
  const notMine = await api('POST', '/admin-api/firstchain/lead/claim', { token: E2.token, body: { id: LD.id, expectedVersion: LD.version } });
  const stale = await api('POST', '/admin-api/firstchain/lead/claim', { token: E1.token, body: { id: LD.id, expectedVersion: Number(LD.version) - 1 } });
  const ok = await api('POST', '/admin-api/firstchain/lead/claim', { token: E1.token, body: { id: LD.id, expectedVersion: LD.version } });
  const g = await api('GET', `/admin-api/firstchain/lead/get?id=${LD.id}`, { token: E1.token });
  LD.version = data(g)?.version;
  record('C1', '领取：非被分配员工被拒；版本过期冲突；被分配员工领取成功 → FOLLOWING',
    codeIn(notMine, C.LEAD_CLAIM_NOT_ASSIGNEE, C.LEAD_VISIBLE_DENIED, C.LEAD_NOT_EXISTS) && codeIn(stale, C.LEAD_VERSION_CONFLICT, C.FIRST_CHAIN_VERSION_CONFLICT) && ok.json?.code === 0 && data(g)?.status === 'FOLLOWING',
    `非本人=${notMine.json?.code} 过期版本=${stale.json?.code}（期望 ${C.LEAD_VERSION_CONFLICT}/${C.FIRST_CHAIN_VERSION_CONFLICT}）领取=${ok.json?.code}(${ok.json?.msg ?? ''}) status=${data(g)?.status}`);
});

await guard('F1', '转商机前置：无跟进记录时被拒', async () => {
  const r = await api('POST', '/admin-api/firstchain/lead/convert', { token: E1.token, body: { leadId: LD.id, expectedVersion: LD.version } });
  record('F1', '转商机前置：无跟进记录时被拒', codeIn(r, C.CONVERT_REQUIRE_FOLLOWUP), `code=${r.json?.code}（期望 ${C.CONVERT_REQUIRE_FOLLOWUP}）`);
});

await guard('F2', '跟进记录：被分配员工可提交；非本人被拒', async () => {
  const ok = await api('POST', '/admin-api/firstchain/lead/followup', { token: E1.token,
    body: { leadId: LD.id, content: '首次电话沟通，客户有装修意向', nextStep: '周五上门量房', followupTime: new Date().toISOString().slice(0, 19) } });
  const other = await api('POST', '/admin-api/firstchain/lead/followup', { token: E2.token,
    body: { leadId: LD.id, content: '越权跟进', followupTime: new Date().toISOString().slice(0, 19) } });
  const rows = psqlOne(`SELECT count(*) FROM bpm_first_chain_followup WHERE lead_id=${LD.id} AND deleted = FALSE`);
  record('F2', '跟进记录：被分配员工可提交；非本人被拒', ok.json?.code === 0 && other.json?.code !== 0 && other.json?.code !== 500 && rows === '1',
    `本人=${ok.json?.code}(${ok.json?.msg ?? ''}) 他人=${other.json?.code} 落库跟进数=${rows}`);
});

await guard('F3', '转商机 → CONVERTED；商机双向引用落库；终态锁定（再跟进/无效被拒）', async () => {
  const g0 = await api('GET', `/admin-api/firstchain/lead/get?id=${LD.id}`, { token: E1.token });
  LD.version = data(g0)?.version;
  const cv = await api('POST', '/admin-api/firstchain/lead/convert', { token: E1.token, body: { leadId: LD.id, expectedVersion: LD.version } });
  const g = await api('GET', `/admin-api/firstchain/lead/get?id=${LD.id}`, { token: E1.token });
  LD.version = data(g)?.version;
  const opp = psqlRows(`SELECT status, customer_name FROM bpm_first_chain_opportunity WHERE lead_id=${LD.id} AND deleted = FALSE`);
  const fu = await api('POST', '/admin-api/firstchain/lead/followup', { token: E1.token, body: { leadId: LD.id, content: '终态后跟进', followupTime: new Date().toISOString().slice(0, 19) } });
  const inv = await api('POST', '/admin-api/firstchain/lead/invalidate', { token: E1.token, body: { leadId: LD.id, reasonName: 'NOT_TARGET', expectedVersion: LD.version } });
  record('F3', '转商机 → CONVERTED；商机双向引用落库；终态锁定（再跟进/无效被拒）',
    cv.json?.code === 0 && data(g)?.status === 'CONVERTED' && opp.length === 1 && codeIn(fu, C.FOLLOWUP_LEAD_TERMINAL) && codeIn(inv, C.LEAD_STATE_TRANSITION, C.LEAD_STATE_CONFLICT, C.FIRST_CHAIN_STATE_CONFLICT),
    `转化=${cv.json?.code}(${cv.json?.msg ?? ''}) status=${data(g)?.status} 商机=${JSON.stringify(opp)} 终态后跟进=${fu.json?.code} 终态后无效=${inv.json?.code}`);
});

// 第二条线索：无效关闭路径
const LD2 = {};
await guard('I1', '无效关闭：原因枚举必填、OTHER 须说明；成功 → INVALID', async () => {
  const d = await api('POST', '/admin-api/firstchain/lead/distribute', { token: admin.token, body: { customerName: '李客户', customerPhone: '13700001234', source: '电话', orgId: A.orgId } });
  LD2.id = String(data(d) ?? '');
  let g = data(await api('GET', `/admin-api/firstchain/lead/get?id=${LD2.id}`, { token: admin.token }));
  await api('POST', '/admin-api/firstchain/lead/assign', { token: L.token, body: { id: LD2.id, assigneeUserId: E2.userId, expectedVersion: g.version } });
  g = data(await api('GET', `/admin-api/firstchain/lead/get?id=${LD2.id}`, { token: E2.token }));
  await api('POST', '/admin-api/firstchain/lead/claim', { token: E2.token, body: { id: LD2.id, expectedVersion: g.version } });
  g = data(await api('GET', `/admin-api/firstchain/lead/get?id=${LD2.id}`, { token: E2.token }));
  const noReason = await api('POST', '/admin-api/firstchain/lead/invalidate', { token: E2.token, body: { leadId: LD2.id, reasonName: '', expectedVersion: g.version } });
  const otherNoDetail = await api('POST', '/admin-api/firstchain/lead/invalidate', { token: E2.token, body: { leadId: LD2.id, reasonName: 'OTHER', expectedVersion: g.version } });
  const ok = await api('POST', '/admin-api/firstchain/lead/invalidate', { token: E2.token, body: { leadId: LD2.id, reasonName: 'OTHER', reasonDetail: '客户已选择其他装修公司', expectedVersion: g.version } });
  const after = data(await api('GET', `/admin-api/firstchain/lead/get?id=${LD2.id}`, { token: E2.token }));
  record('I1', '无效关闭：原因枚举必填、OTHER 须说明；成功 → INVALID',
    noReason.json?.code !== 0 && noReason.json?.code !== 500 && codeIn(otherNoDetail, C.INVALIDATE_REASON_DETAIL_REQUIRED) && ok.json?.code === 0 && after?.status === 'INVALID',
    `无原因=${noReason.json?.code} OTHER无说明=${otherNoDetail.json?.code}（期望 ${C.INVALIDATE_REASON_DETAIL_REQUIRED}）关闭=${ok.json?.code}(${ok.json?.msg ?? ''}) status=${after?.status}`);
});

await guard('M1', '指标同源：平台/负责人/员工三视角均由权威状态列 GROUP BY 派生', async () => {
  const m = async (tok) => data(await api('GET', '/admin-api/firstchain/lead/metrics', { token: tok }));
  const [mp, ml, me] = [await m(admin.token), await m(L.token), await m(E1.token)];
  const n = (x, k) => Number((x ?? {})[k] ?? 0);
  // A 组织：1 个 CONVERTED（员工一）+ 1 个 INVALID（员工二）；平台/负责人应各见 1/1；员工一仅见本人（1 CONVERTED / 0 INVALID）
  record('M1', '指标同源：平台/负责人/员工三视角均由权威状态列 GROUP BY 派生',
    n(mp, 'CONVERTED') >= 1 && n(ml, 'CONVERTED') === 1 && n(ml, 'INVALID') === 1 && n(me, 'CONVERTED') === 1 && n(me, 'INVALID') === 0,
    `平台=${JSON.stringify(mp)} 负责人=${JSON.stringify(ml)} 员工一=${JSON.stringify(me)}`);
});

// ======================================================================
// 通知与落点（PILOT-REQ-010）
// ======================================================================
await guard('N1', '站内通知：负责人收到下发/领取/结束通知，落点授权通过', async () => {
  const codes = await eventually(async () => {
    const r = await api('GET', '/admin-api/system/notify-message/my-page?pageNo=1&pageSize=50', { token: L.token });
    const list = data(r)?.list ?? [];
    const got = new Set(list.map((m) => m.templateCode));
    return ['firstchain_application_approved', 'firstchain_lead_distributed', 'firstchain_lead_claimed', 'firstchain_lead_closed'].every((c) => got.has(c)) ? list : null;
  }, { timeoutMs: 30000, stepMs: 2000 });
  const list = codes ?? [];
  const landingMsg = list.find((m) => m.templateCode === 'firstchain_lead_distributed');
  const landing = landingMsg ? await api('GET', `/admin-api/system/notify-message/get-landing?id=${landingMsg.id}&client=WEB`, { token: L.token }) : null;
  record('N1', '站内通知：负责人收到下发/领取/结束通知，落点授权通过', !!codes && landing?.json?.code === 0,
    `模板=${JSON.stringify([...new Set(list.map((m) => m.templateCode))])} 落点=${landing?.json?.code}(${landing?.json?.msg ?? ''})`);
});

await guard('N2', '站内通知：被分配员工收到分配通知；他人消息落点被拒', async () => {
  const mine = await eventually(async () => {
    const list = data(await api('GET', '/admin-api/system/notify-message/my-page?pageNo=1&pageSize=50', { token: E1.token }))?.list ?? [];
    return list.find((m) => m.templateCode === 'firstchain_lead_assigned') ?? null;
  }, { timeoutMs: 30000, stepMs: 2000 });
  const leaderMsg = psqlOne(`SELECT id FROM system_notify_message WHERE user_id=${L.userId} AND template_code='firstchain_lead_distributed' AND deleted=0 ORDER BY id DESC LIMIT 1`);
  const cross = leaderMsg ? await api('GET', `/admin-api/system/notify-message/get-landing?id=${leaderMsg}&client=WEB`, { token: E1.token }) : null;
  record('N2', '站内通知：被分配员工收到分配通知；他人消息落点被拒', !!mine && !!cross && cross.json?.code !== 0 && cross.json?.code !== 500,
    `员工分配通知=${!!mine} 他人消息落点=${cross?.json?.code}（须被拒，非 0/500）`);
});

// ======================================================================
// 跨组织隔离（franchisee B）
// ======================================================================
await guard('B1', '第二加盟商 B：创建→提交→审批开通，负责人登录', async () => {
  const x = await createAndSubmit('B');
  B.id = x.id; B.appKey = x.appKey;
  const ap = await api('POST', '/admin-api/firstchain/application/approve', { token: admin.token, body: { appKey: B.appKey, reason: 'B 同意开通' } });
  B.orgId = String(data(ap)?.organizationId ?? ''); B.password = data(ap)?.initialPassword;
  B.leaderUserId = psqlOne(`SELECT leader_user_id FROM system_organization WHERE id=${B.orgId || 0}`);
  B.username = psqlOne(`SELECT username FROM system_users WHERE id=${B.leaderUserId || 0}`);
  const l = await login(B.username, B.password); B.token = l.token;
  record('B1', '第二加盟商 B：创建→提交→审批开通，负责人登录', x.submitCode === 0 && ap.json?.code === 0 && !!B.token && B.orgId !== A.orgId,
    `提交=${x.submitCode} 审批=${ap.json?.code}(${ap.json?.msg ?? ''}) orgB=${B.orgId} 登录=${!!B.token}`);
});

await guard('B2', '隔离：负责人 B 看不到 A 的线索；直接按 ID 访问/分配被拒', async () => {
  const page = (data(await api('GET', '/admin-api/firstchain/lead/page?pageNo=1&pageSize=50', { token: B.token }))?.list ?? []);
  const sees = page.some((x) => [LD.id, LD2.id].includes(String(x.id)));
  const get = await api('GET', `/admin-api/firstchain/lead/get?id=${LD2.id}`, { token: B.token });
  const assign = await api('POST', '/admin-api/firstchain/lead/assign', { token: B.token, body: { id: LD2.id, assigneeUserId: B.leaderUserId, expectedVersion: 1 } });
  const mB = data(await api('GET', '/admin-api/firstchain/lead/metrics', { token: B.token }));
  const total = Object.values(mB ?? {}).reduce((a, b) => a + Number(b || 0), 0);
  record('B2', '隔离：负责人 B 看不到 A 的线索；直接按 ID 访问/分配被拒',
    !sees && codeIn(get, C.LEAD_VISIBLE_DENIED, C.LEAD_NOT_EXISTS) && assign.json?.code !== 0 && assign.json?.code !== 500 && total === 0,
    `列表可见=${sees} get=${get.json?.code} 分配=${assign.json?.code} B指标合计=${total}`);
});

// ======================================================================
// 拒绝 / 撤回 / 停用即失权
// ======================================================================
await guard('R1', '审批拒绝：意见必填；拒绝不建任何主体；终态后审批冲突', async () => {
  const x = await createAndSubmit('R');
  const noReason = await api('POST', '/admin-api/firstchain/application/reject', { token: admin.token, body: { appKey: x.appKey, reason: '' } });
  const rj = await api('POST', '/admin-api/firstchain/application/reject', { token: admin.token, body: { appKey: x.appKey, reason: '资质材料不全' } });
  const g = data(await api('GET', `/admin-api/firstchain/application/get?id=${x.id}`, { token: admin.token }));
  const orgs = psqlOne(`SELECT count(*) FROM system_organization WHERE code='${x.appKey}' AND tenant_id=${TENANT} AND deleted=0`);
  const ap = await api('POST', '/admin-api/firstchain/application/approve', { token: admin.token, body: { appKey: x.appKey, reason: '拒绝后再审批' } });
  record('R1', '审批拒绝：意见必填；拒绝不建任何主体；终态后审批冲突',
    noReason.json?.code !== 0 && noReason.json?.code !== 500 && rj.json?.code === 0 && g?.status === 'REJECTED' && g?.rejectReason === '资质材料不全' && orgs === '0' && codeIn(ap, C.APP_STATUS_CONFLICT),
    `无意见=${noReason.json?.code} 拒绝=${rj.json?.code}(${rj.json?.msg ?? ''}) status=${g?.status} 组织数=${orgs} 再审批=${ap.json?.code}（期望 ${C.APP_STATUS_CONFLICT}）`);
});

await guard('R2', '撤回审批流：领域状态不变（SUBMITTED），可重新发起', async () => {
  const x = await createAndSubmit('W');
  const g1 = data(await api('GET', `/admin-api/firstchain/application/get?id=${x.id}`, { token: admin.token }));
  const wd = await api('POST', '/admin-api/firstchain/application/withdraw', { token: admin.token, body: { id: x.id, expectedVersion: g1.version } });
  const g2 = data(await api('GET', `/admin-api/firstchain/application/get?id=${x.id}`, { token: admin.token }));
  const binding = psqlOne(`SELECT status FROM bpm_first_chain_process_binding WHERE domain_type='franchisee_application' AND domain_id=${x.id} ORDER BY id DESC LIMIT 1`);
  record('R2', '撤回审批流：领域状态不变（SUBMITTED），可重新发起', wd.json?.code === 0 && g2?.status === 'SUBMITTED' && binding === 'WITHDRAWN',
    `撤回=${wd.json?.code}(${wd.json?.msg ?? ''}) status=${g2?.status} 绑定=${binding}`);
});

await guard('X1', '停用即失权：管理员停用员工后，旧令牌立即失效且无法再登录', async () => {
  const before = await api('GET', '/admin-api/firstchain/lead/page?pageNo=1&pageSize=5', { token: E2.token });
  const off = await api('PUT', '/admin-api/system/user/update-status', { token: admin.token, body: { id: E2.userId, status: 1 } });
  const after = await api('GET', '/admin-api/firstchain/lead/page?pageNo=1&pageSize=5', { token: E2.token });
  const relogin = await login(E2.username, E2.password);
  record('X1', '停用即失权：管理员停用员工后，旧令牌立即失效且无法再登录',
    before.json?.code === 0 && off.json?.code === 0 && after.json?.code === 401 && !relogin.token,
    `停用前=${before.json?.code} 停用=${off.json?.code} 停用后旧令牌=${after.json?.code}（期望 401）重新登录=${relogin.raw?.json?.code}`);
});

finish();

function finish(forcedExit) {
  const pass = results.filter((r) => r.ok).length;
  const fail = results.length - pass;
  console.log(`\n合计 ${results.length} 用例：PASS ${pass} / FAIL ${fail}`);
  try {
    writeFileSync(join(OUT_DIR, `fc-e2e-report-${Date.now()}.json`),
      JSON.stringify({ baseUrl: BASE_URL, tenant: TENANT, pass, fail, results }, null, 2));
  } catch { /* 报告落盘失败不影响退出码 */ }
  process.exit(forcedExit ?? (fail ? 1 : 0));
}
