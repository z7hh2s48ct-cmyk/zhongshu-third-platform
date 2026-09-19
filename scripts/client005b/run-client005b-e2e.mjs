/**
 * ZS-CLIENT-005.B：两端登录/导航/文件/待办 E2E 回归（真实接口/PG 环境域）。
 *
 * 定位：对**长期联验环境**（deploy/docker-compose.local.yml infra + local profile server）
 * 直发真实 HTTP 用例——真实 Filter/MVC/权限链/PG 持久化，无任何 mock/替身
 * （卡片验收明确：构建和接口替身不代替联调）。浏览器级页面操作归
 * SYS-001.B/BRAND-003.B 联验执行阶段；本套件锚定接口域正反向合同。
 *
 * 前置：联验环境已按 deploy/README-local.md 启动（health UP + admin 种子已重放）。
 * 用法：node scripts/client005b/run-client005b-e2e.mjs
 *   E2E_BASE_URL（默认 http://127.0.0.1:48080）/ E2E_TENANT_ID（默认 1）
 *   E2E_ADMIN_USER（默认 admin）/ E2E_ADMIN_PASS（默认 admin123）
 *
 * codex r0 处置合同：
 *   - 断言锁定业务码（L1=1002000000；T5/T6 落点=1002031000/1002031001 家族），系统错误必须 FAIL；
 *   - 重放容忍：登录 @RateLimiter 5 次/60s 每账号——遇 429 等待 65s 重试一次（有痕输出）；
 *     夹具幂等（用户已存在仅容忍 1002003000 且读回校验；模板 code/文件名时间戳化）；
 *   - 环境还原：文件域测试后恢复原 master 配置（无原 master 时保留本套件配置并在报告注明）；
 *   - 任何用例异常记 FAIL 不中断报告（域级 try/catch）。
 *
 * ⚠️ SEC-009.B 基线耦合（他区在制，ID→string 合同激活后）：本套件对 id/data 做数值严格比较
 * （m.id === testMessageId 等）——激活后序列化变 string 须同步调整比较口径并重录基线。
 * 任一用例 FAIL → 非零退出；报告落 outputs/client005b/。
 */
import { writeFileSync, mkdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));
const BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:48080';
const TENANT_ID = process.env.E2E_TENANT_ID ?? '1';
const ADMIN_USER = process.env.E2E_ADMIN_USER ?? 'admin';
const ADMIN_PASS = process.env.E2E_ADMIN_PASS ?? 'admin123';
const OUT_DIR = join(root, 'outputs/client005b');
mkdirSync(OUT_DIR, { recursive: true });

// 业务码合同（锁码断言，系统错误不得伪装成安全拒绝）
const CODE_LOGIN_FAILED = 1002000000; // 1_002_000_000
const CODE_USERNAME_EXISTS = 1002003000; // 1_002_003_000
const CODE_LANDING_NOT_FOUND = 1002031000; // 1_002_031_000
const CODE_LANDING_ACCESS_DENIED = 1002031001; // 1_002_031_001

const results = [];
const record = (id, name, ok, note = '') => {
  results.push({ id, name, ok, note: String(note).slice(0, 300) });
  console.log(`[${ok ? 'PASS' : 'FAIL'}] ${id} ${name}${note ? ` — ${String(note).slice(0, 200)}` : ''}`);
  return ok;
};
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function api(method, path, { token, body } = {}) {
  const headers = { 'tenant-id': TENANT_ID };
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const res = await fetch(BASE_URL + path, {
    method, headers, body: body !== undefined ? JSON.stringify(body) : undefined, redirect: 'manual',
  });
  const text = await res.text();
  let json = null;
  try { json = JSON.parse(text); } catch { /* 非JSON（如文件流） */ }
  return { httpStatus: res.status, json, text };
}

/** 登录：429（每账号 60s/5 次限流）等待 65s 重试一次（重放容忍，有痕） */
async function login(username, password) {
  for (let attempt = 1; attempt <= 2; attempt++) {
    const r = await api('POST', '/admin-api/system/auth/login', { body: { username, password } });
    if (r.json?.code === 0 && r.json.data?.accessToken) return r.json.data;
    if (r.json?.code === 429 && attempt === 1) {
      console.log(`[rate-limit] ${username} 登录被限流，等待 65s 重试（@RateLimiter 5 次/60s 合同）`);
      await sleep(65_000);
      continue;
    }
    throw new Error(`login ${username} failed: code=${r.json?.code} ${r.text.slice(0, 100)}`);
  }
  throw new Error(`login ${username}: unreachable`);
}

/** 域级守卫：异常记 FAIL 不中断报告 */
const guard = async (id, name, fn) => {
  try { await fn(); } catch (e) { record(id, name, false, `EXCEPTION: ${e.message}`); }
};

// ---------- 环境健康预检（联验环境未起 → 明确失败退出码 3，不静默跳过） ----------
{
  const health = await fetch(BASE_URL + '/actuator/health').then((r) => r.json()).catch(() => null);
  if (health?.status !== 'UP') {
    console.error(`[client005b] 联验环境未就绪（health=${JSON.stringify(health)}）——按 deploy/README-local.md 启动后重试`);
    process.exit(3);
  }
  console.log(`[env] ${BASE_URL} health=UP`);
}

// ========== 登录域 ==========
let admin = null;
let testUser = null; // { id, accessToken }
await guard('L0', '管理员登录（前置）', async () => {
  admin = await login(ADMIN_USER, ADMIN_PASS);
  record('L0', '管理员登录（前置）', !!admin.accessToken, `userId=${admin.userId}`);
});

await guard('L1', '登录反向：错误口令拒绝（锁定业务码）', async () => {
  // r1 P2-2：L1 也排限流（连续重放时 429 被误当「非 0 码通过」）——429 等待重试后再严格断言
  let r = await api('POST', '/admin-api/system/auth/login', { body: { username: ADMIN_USER, password: 'wrong-password' } });
  if (r.json?.code === 429) {
    console.log('[rate-limit] L1 遇限流，等待 65s 重试');
    await sleep(65_000);
    r = await api('POST', '/admin-api/system/auth/login', { body: { username: ADMIN_USER, password: 'wrong-password' } });
  }
  record('L1', '登录反向：错误口令拒绝（锁定业务码）', r.json?.code === CODE_LOGIN_FAILED,
    `code=${r.json?.code}（期望 ${CODE_LOGIN_FAILED}；500/429 视为缺陷）`);
});

await guard('L2', '登录反向：未登录访问被拒', async () => {
  const r = await api('GET', '/admin-api/system/auth/get-permission-info');
  record('L2', '登录反向：未登录访问被拒', r.httpStatus === 401 || r.json?.code === 401, `http=${r.httpStatus} code=${r.json?.code}`);
});

await guard('L3', '刷新令牌正向：双令牌轮换', async () => {
  const refreshed = await api('POST', `/admin-api/system/auth/refresh-token?refreshToken=${admin.refreshToken}`);
  const ok = refreshed.json?.code === 0 && refreshed.json.data?.accessToken && refreshed.json.data.accessToken !== admin.accessToken;
  record('L3', '刷新令牌正向：双令牌轮换', ok, `code=${refreshed.json?.code} new=${refreshed.json?.data?.accessToken?.slice(0, 8)}…`);
  if (ok) admin.accessToken = refreshed.json.data.accessToken;
});

await guard('L4', '登出反向：旧令牌失效', async () => {
  const second = await login(ADMIN_USER, ADMIN_PASS); // 第二会话，不污染主令牌
  const out = await api('POST', '/admin-api/system/auth/logout', { token: second.accessToken });
  const after = await api('GET', '/admin-api/system/auth/get-permission-info', { token: second.accessToken });
  record('L4', '登出反向：旧令牌失效', out.json?.code === 0 && (after.httpStatus === 401 || after.json?.code === 401),
    `logout=${out.json?.code} after=http${after.httpStatus}/code${after.json?.code}`);
});

await guard('L5', 'WS 握手票据签发正向（LOGIN-001.B 联验）', async () => {
  const r = await api('POST', '/admin-api/system/auth/ws-ticket', { token: admin.accessToken });
  const ticket = r.json?.data ?? '';
  record('L5', 'WS 握手票据签发正向（LOGIN-001.B 联验）', r.json?.code === 0 && /^[0-9a-f]{64}$/.test(ticket),
    `code=${r.json?.code} len=${ticket.length}（SecureRandom 32 字节 hex）`);
});

// ========== 导航域 ==========
const flatLeaf = (menus) => {
  const out = [];
  const walk = (ms) => { for (const m of ms) { out.push(m.name); walk(m.children ?? []); } };
  walk(menus ?? []);
  return out;
};
const DISCRIMINATOR_MENUS = ['邮箱管理', '站内信管理', '模板管理', '三方登录']; // admin 独有叶子（种子基线）
await guard('N1', 'admin 权限菜单树正向（导航单一真相源）', async () => {
  const r = await api('GET', '/admin-api/system/auth/get-permission-info', { token: admin.accessToken });
  const menus = r.json?.data?.menus ?? [];
  const roles = r.json?.data?.roles ?? [];
  const leafs = flatLeaf(menus);
  const discOk = DISCRIMINATOR_MENUS.every((d) => leafs.includes(d)); // admin 对照确实包含判别菜单
  record('N1', 'admin 权限菜单树正向（导航单一真相源）',
    r.json?.code === 0 && menus.length > 0 && roles.includes('super_admin') && discOk,
    `menus=${menus.length} leafs=${leafs.length} roles=${JSON.stringify(roles)} 判别菜单齐全=${discOk}`);
});

await guard('N2', '导航反向：common 用户菜单为 admin 严格子集且判别菜单缺席', async () => {
  // 夹具幂等：已存在仅容忍 1002003000（USER_USERNAME_EXISTS），其余失败必须暴露
  const create = await api('POST', '/admin-api/system/user/create', {
    token: admin.accessToken,
    body: { username: 'lianyantest', password: 'Test123456', nickname: '联验测试账号', deptId: 103, mobile: '15600000001' },
  });
  const created = create.json?.code === 0 ? create.json.data : null;
  if (!created && create.json?.code !== CODE_USERNAME_EXISTS) {
    record('N2', '导航反向：common 用户菜单为 admin 严格子集且判别菜单缺席', false,
      `用户创建失败且非已存在码：code=${create.json?.code}`);
    return;
  }
  // 按用户名读回（r1 P2-1 / r2 P2：后端为模糊匹配且可能超一页——精确 find + 逐页扫尽，防同名 backup 截胡或分页截断误报）
  let row = null;
  for (let pageNo = 1; pageNo <= 10 && !row; pageNo++) {
    const lookup = await api('GET', `/admin-api/system/user/page?pageNo=${pageNo}&pageSize=100&username=lianyantest`, { token: admin.accessToken });
    const list = lookup.json?.data?.list ?? [];
    row = list.find((u) => u.username === 'lianyantest') ?? null;
    if (list.length < 100) break;
  }
  if (!row) { record('N2', '导航反向：common 用户菜单为 admin 严格子集且判别菜单缺席', false, '用户名读回未命中精确账号'); return; }
  await api('PUT', '/admin-api/system/user/update-status', { token: admin.accessToken, body: { id: row.id, status: 0 } }); // 0=启用
  const assign = await api('POST', '/admin-api/system/permission/assign-user-role', {
    token: admin.accessToken, body: { userId: row.id, roleIds: [2] }, // 2=common
  });
  const rolesRead = await api('GET', `/admin-api/system/permission/list-user-roles?userId=${row.id}`, { token: admin.accessToken });
  // SEC-009.B 对账（r1 VO 包装后）：list-user-roles 返回 { roleIds: ["2"] }（ID 恒 string）；兼容旧数组形态
  const _roleData = rolesRead.json?.data;
  const bound = (Array.isArray(_roleData) ? _roleData : (_roleData?.roleIds ?? []))
    .map((x) => Number(x)).includes(2); // 绑定读回（数值归一比较）
  testUser = { id: row.id, accessToken: (await login('lianyantest', 'Test123456')).accessToken };
  const mine = await api('GET', '/admin-api/system/auth/get-permission-info', { token: testUser.accessToken });
  const commonLeafs = flatLeaf(mine.json?.data?.menus);
  const adminInfo = await api('GET', '/admin-api/system/auth/get-permission-info', { token: admin.accessToken });
  const adminLeafs = flatLeaf(adminInfo.json?.data?.menus);
  const isSubset = commonLeafs.every((m) => adminLeafs.includes(m));
  const hitDisc = DISCRIMINATOR_MENUS.filter((d) => commonLeafs.includes(d)); // 必须为空
  const adminHasDisc = DISCRIMINATOR_MENUS.every((d) => adminLeafs.includes(d)); // 判别菜单在 admin 侧在场（防空断言）
  record('N2', '导航反向：common 用户菜单为 admin 严格子集且判别菜单缺席',
    mine.json?.code === 0 && (mine.json?.data?.roles ?? []).includes('common') && bound && assign.json?.code === 0
      && isSubset && hitDisc.length === 0 && adminHasDisc && commonLeafs.length < adminLeafs.length,
    `common=${commonLeafs.length} admin=${adminLeafs.length} subset=${isSubset} 判别命中=${hitDisc.length} admin侧齐全=${adminHasDisc} 角色绑定读回=${bound}`);
});

// ========== 文件域 ==========
let originalMasterId = null; // 环境还原
let originalMasterScanned = false; // r1 P2-4：区分「确实无原 master」与「分页未扫全」
await guard('F1', '文件存储配置创建并设 master（setup + 切换断言）', async () => {
  // r1 P2-4：完整分页扫描原 master（每轮新增一条配置，单页 100 会漏）
  originalMasterId = null;
  originalMasterScanned = false;
  for (let pageNo = 1; ; pageNo++) {
    const page = await api('GET', `/admin-api/infra/file-config/page?pageNo=${pageNo}&pageSize=100`, { token: admin.accessToken });
    const list = page.json?.data?.list ?? [];
    if (!list.length) break;
    const hit = list.find((c) => c.master === true);
    if (hit != null) { originalMasterId = hit.id; break; }
    if (list.length < 100) break;
  }
  originalMasterScanned = true;
  const create = await api('POST', '/admin-api/infra/file-config/create', {
    token: admin.accessToken,
    body: { name: `lianyan-db-${Date.now()}`, storage: 1, remark: 'CLIENT-005.B E2E', config: { domain: new URL(BASE_URL).origin } } // 端口可移植：跟随 E2E_BASE_URL,
  });
  const configId = create.json?.data ?? null;
  if (!configId) { record('F1', '文件存储配置创建并设 master（setup + 切换断言）', false, `创建失败 code=${create.json?.code}`); return; }
  const setMaster = await api('PUT', `/admin-api/infra/file-config/update-master?id=${configId}`, { token: admin.accessToken });
  const after = await api('GET', '/admin-api/infra/file-config/page?pageNo=1&pageSize=100', { token: admin.accessToken });
  const masterRow = (after.json?.data?.list ?? []).find((c) => c.master === true);
  const readbackOk = String(masterRow?.id) === String(configId); // 读回 master 归属（SEC-009.B：ID wire 恒 string，字符串化比较）
  record('F1', '文件存储配置创建并设 master（setup + 切换断言）',
    setMaster.json?.code === 0 && readbackOk, `configId=${configId} setMaster=${setMaster.json?.code} 读回master=${masterRow?.id}`);
});

let uploadedUrl = null;
let uploadedContent = null;
await guard('F2', '文件上传正向', async () => {
  uploadedContent = `client005b-e2e-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`; // 本轮唯一内容（重放安全 + F3 全等比较）
  const form = new FormData();
  form.append('file', new Blob([Buffer.from(uploadedContent)]), 'lianyan-e2e.txt');
  const res = await fetch(BASE_URL + '/admin-api/infra/file/upload', {
    method: 'POST', headers: { Authorization: `Bearer ${admin.accessToken}`, 'tenant-id': TENANT_ID }, body: form,
  });
  const json = await res.json().catch(() => null);
  uploadedUrl = json?.data ?? null;
  record('F2', '文件上传正向', json?.code === 0 && typeof uploadedUrl === 'string' && uploadedUrl.length > 0,
    `url=${String(uploadedUrl).slice(0, 80)}`);
});

await guard('F3', '文件下载边界：匿名 403 + 认证后内容全等回读', async () => {
  if (!uploadedUrl) { record('F3', '文件下载边界：匿名 403 + 认证后内容全等回读', false, '前置 F2 未产出 URL'); return; }
  const anon = await fetch(uploadedUrl, { redirect: 'manual' }); // FILE-003 私有下载：匿名必须拒绝
  const anonJson = await anon.json().catch(() => null);
  const anonDenied = anon.status === 403 || anonJson?.code === 403;
  const authed = await fetch(uploadedUrl, { redirect: 'manual', headers: { Authorization: `Bearer ${admin.accessToken}` } });
  const text = await authed.text();
  record('F3', '文件下载边界：匿名 403 + 认证后内容全等回读',
    anonDenied && authed.status === 200 && text === uploadedContent,
    `anon=${anon.status}/${anonJson?.code} authed=${authed.status} 全等=${text === uploadedContent}`);
});

await guard('F4', '文件删除反向：未登录删除被拒', async () => {
  const delDenied = await api('DELETE', '/admin-api/infra/file/delete?id=1');
  record('F4', '文件删除反向：未登录删除被拒', delDenied.httpStatus === 401 || delDenied.json?.code === 401,
    `http=${delDenied.httpStatus} code=${delDenied.json?.code}`);
});

// ========== 待办/通知域 ==========
let templateCode = null;
await guard('T1', '通知模板创建（setup，code 时间戳化重放安全）', async () => {
  const code = `lianyan_e2e_${Date.now()}`;
  const r = await api('POST', '/admin-api/system/notify-template/create', {
    token: admin.accessToken,
    body: { name: `联验E2E模板${Date.now()}`, code, nickname: '联验机器人', content: '联验消息：{content}', type: 2, status: 0, remark: 'CLIENT-005.B E2E' },
  });
  templateCode = r.json?.code === 0 ? code : null;
  record('T1', '通知模板创建（setup，code 时间戳化重放安全）', !!templateCode, `code=${templateCode} resp=${r.json?.code}`);
});

let ownMessageId = null;
let otherMessageId = null; // 发给 admin 的消息（T6 他人消息边界）
await guard('T2', '站内信发送正向（收件人 + 他人对照各一）', async () => {
  if (!templateCode || !testUser) { record('T2', '站内信发送正向（收件人 + 他人对照各一）', false, '前置缺失'); return; }
  const sendOwn = await api('POST', '/admin-api/system/notify-template/send-notify', {
    token: admin.accessToken, body: { userId: testUser.id, userType: 2, templateCode, templateParams: { content: 'hello-lianyan' } },
  });
  ownMessageId = sendOwn.json?.data ?? null;
  const sendOther = await api('POST', '/admin-api/system/notify-template/send-notify', {
    token: admin.accessToken, body: { userId: 1, userType: 2, templateCode, templateParams: { content: 'to-admin' } }, // admin 自己收
  });
  otherMessageId = sendOther.json?.data ?? null;
  record('T2', '站内信发送正向（收件人 + 他人对照各一）', !!ownMessageId && !!otherMessageId,
    `own=${ownMessageId} other=${otherMessageId}`);
});

await guard('T3', '收件人收件箱含新消息正向（my-page）', async () => {
  if (!ownMessageId) { record('T3', '收件人收件箱含新消息正向（my-page）', false, '前置 T2 未命中'); return; }
  const page = await api('GET', '/admin-api/system/notify-message/my-page?pageNo=1&pageSize=10&readStatus=false', { token: testUser.accessToken });
  const hit = (page.json?.data?.list ?? []).some((m) => Number(m.id) === Number(ownMessageId));
  record('T3', '收件人收件箱含新消息正向（my-page）', hit, `list=${(page.json?.data?.list ?? []).length} 命中=${hit}`);
});

await guard('T4', '标记已读正向', async () => {
  if (!ownMessageId) { record('T4', '标记已读正向', false, '前置 T3 未命中'); return; }
  const upd = await api('PUT', `/admin-api/system/notify-message/update-read?ids=${ownMessageId}`, { token: testUser.accessToken });
  const after = await api('GET', '/admin-api/system/notify-message/my-page?pageNo=1&pageSize=10&readStatus=true', { token: testUser.accessToken });
  const readHit = (after.json?.data?.list ?? []).some((m) => Number(m.id) === Number(ownMessageId));
  record('T4', '标记已读正向', upd.json?.code === 0 && readHit, `upd=${upd.json?.code} 读回已读=${readHit}`);
});

await guard('T5', '落点正向：本人消息 get-landing 授权通过', async () => {
  if (!ownMessageId || !testUser) { record('T5', '落点正向：本人消息 get-landing 授权通过', false, '前置缺失'); return; }
  const r = await api('GET', `/admin-api/system/notify-message/get-landing?id=${ownMessageId}&client=WEB`, { token: testUser.accessToken });
  record('T5', '落点正向：本人消息 get-landing 授权通过', r.json?.code === 0,
    `code=${r.json?.code}（MSG-003.A 二次授权：归属确认通过）`);
});

await guard('T6', '落点反向：他人消息 get-landing 被拒（MSG-003.A，锁定落点错误码族）', async () => {
  if (!otherMessageId || !testUser) { record('T6', '落点反向：他人消息 get-landing 被拒（MSG-003.A，锁定落点错误码族）', false, '前置缺失'); return; }
  const r = await api('GET', `/admin-api/system/notify-message/get-landing?id=${otherMessageId}&client=WEB`, { token: testUser.accessToken });
  const denied = r.json?.code === CODE_LANDING_NOT_FOUND || r.json?.code === CODE_LANDING_ACCESS_DENIED;
  record('T6', '落点反向：他人消息 get-landing 被拒（MSG-003.A，锁定落点错误码族）', denied,
    `code=${r.json?.code}（期望 ${CODE_LANDING_NOT_FOUND}/${CODE_LANDING_ACCESS_DENIED}；500/429 视为缺陷）`);
});

// ---------- 环境还原：恢复文件 master 原状（r1 P2-3 / r2 P3：code 校验+读回确认；异常同步 restoreOk 计入退出码） ----------
let restoreOk = true;
let restoreNote = '未执行';
await guard('FX', '环境还原：恢复文件 master 原配置', async () => {
  try {
    if (originalMasterId != null) {
      const r = await api('PUT', `/admin-api/infra/file-config/update-master?id=${originalMasterId}`, { token: admin.accessToken });
      const after = await api('GET', '/admin-api/infra/file-config/page?pageNo=1&pageSize=100', { token: admin.accessToken });
      const stillMaster = (after.json?.data?.list ?? []).find((c) => String(c.id) === String(originalMasterId))?.master === true;
      restoreOk = r.json?.code === 0 && stillMaster;
      restoreNote = `恢复 ${originalMasterId}：code=${r.json?.code} 读回master=${stillMaster}`;
    } else {
      restoreNote = '无原 master（首启），保留本套件配置供后续复跑';
    }
  } catch (e) {
    restoreOk = false; // r2 P3：网络等异常不得让 JSON 报告宣称恢复成功
    restoreNote = `恢复异常：${e.message}`;
  }
  console.log(`[restore] ${restoreNote}`);
});

// ---------- 汇总 ----------
const pass = results.filter((r) => r.ok).length;
const fail = results.length - pass;
const finalFail = fail + (restoreOk ? 0 : 1); // r1 P2-3：restore 失败计入退出码
console.log(`\n合计 ${results.length} 用例：PASS ${pass} / FAIL ${fail}${restoreOk ? '' : ' + 环境还原失败'}`);
writeFileSync(join(OUT_DIR, `e2e-report-${Date.now()}.json`),
  JSON.stringify({ baseUrl: BASE_URL, idBaseline: 'numeric（SEC-009.B 激活后须调整）', pass, fail, restoreOk, restoreNote, results }, null, 2));
process.exit(finalFail ? 1 : 0);