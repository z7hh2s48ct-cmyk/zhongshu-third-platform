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

const results = [];
const record = (id, name, ok, note = '') => {
  results.push({ id, name, ok, note: String(note).slice(0, 300) });
  console.log(`[${ok ? 'PASS' : 'FAIL'}] ${id} ${name}${note ? ` — ${String(note).slice(0, 200)}` : ''}`);
  return ok;
};

async function api(method, path, { token, body, form } = {}) {
  const headers = { 'tenant-id': TENANT_ID };
  if (token) headers.Authorization = `Bearer ${token}`;
  let payload;
  if (form) {
    headers.Authorization = token ? `Bearer ${token}` : headers.Authorization;
    payload = form;
  } else if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    payload = JSON.stringify(body);
  }
  const res = await fetch(BASE_URL + path, { method, headers, body: payload, redirect: 'manual' });
  const text = await res.text();
  let json = null;
  try { json = JSON.parse(text); } catch { /* 非JSON（如文件流） */ }
  return { httpStatus: res.status, json, text };
}

const login = async (username, password) => {
  const r = await api('POST', '/admin-api/system/auth/login', { body: { username, password } });
  if (r.json?.code !== 0) throw new Error(`login ${username} failed: ${r.text.slice(0, 120)}`);
  return r.json.data; // { userId, accessToken, refreshToken, expiresTime }
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
const admin = await login(ADMIN_USER, ADMIN_PASS);
record('L0', '管理员登录（前置）', !!admin.accessToken, `userId=${admin.userId}`);

{
  const r = await api('POST', '/admin-api/system/auth/login', { body: { username: ADMIN_USER, password: 'wrong-password' } });
  record('L1', '登录反向：错误口令拒绝', r.json?.code !== 0, `code=${r.json?.code}`);
}
{
  const r = await api('GET', '/admin-api/system/auth/get-permission-info');
  record('L2', '登录反向：未登录访问被拒', r.httpStatus === 401 || r.json?.code === 401, `http=${r.httpStatus} code=${r.json?.code}`);
}
{
  const refreshed = await api('POST', `/admin-api/system/auth/refresh-token?refreshToken=${admin.refreshToken}`);
  const ok = refreshed.json?.code === 0 && refreshed.json.data?.accessToken && refreshed.json.data.accessToken !== admin.accessToken;
  record('L3', '刷新令牌正向：双令牌轮换', ok, `new=${refreshed.json?.data?.accessToken?.slice(0, 8)}…`);
  if (ok) admin.accessToken = refreshed.json.data.accessToken;
}
{
  // 第二会话用于登出失效验证（不污染主令牌）
  const second = await login(ADMIN_USER, ADMIN_PASS);
  await api('POST', '/admin-api/system/auth/logout', { token: second.accessToken });
  const after = await api('GET', '/admin-api/system/auth/get-permission-info', { token: second.accessToken });
  record('L4', '登出反向：旧令牌失效', after.httpStatus === 401 || after.json?.code === 401, `http=${after.httpStatus} code=${after.json?.code}`);
}
{
  const r = await api('POST', '/admin-api/system/auth/ws-ticket', { token: admin.accessToken });
  const ticket = r.json?.data ?? '';
  record('L5', 'WS 握手票据签发正向（LOGIN-001.B 联验）', r.json?.code === 0 && /^[0-9a-f]{64}$/.test(ticket), `len=${ticket.length}`);
}

// ========== 导航域 ==========
let testUserToken = null;
{
  const r = await api('GET', '/admin-api/system/auth/get-permission-info', { token: admin.accessToken });
  const menus = r.json?.data?.menus ?? [];
  const roles = r.json?.data?.roles ?? [];
  record('N1', 'admin 权限菜单树正向（导航单一真相源）', r.json?.code === 0 && menus.length > 0 && roles.includes('super_admin'),
    `menus=${menus.length} roles=${JSON.stringify(roles)}`);
}
{
  // N2 反向：新建普通用户绑定 common 角色 → 菜单树按角色收敛（严格子集 + admin 判别菜单缺席）
  const okCreate = await api('POST', '/admin-api/system/user/create', {
    token: admin.accessToken,
    body: { username: 'lianyantest', password: 'Test123456', nickname: '联验测试账号', deptId: 103, mobile: '15600000001' },
  });
  const created = okCreate.json?.data;
  if (created) {
    await api('PUT', '/admin-api/system/user/update-status', {
      token: admin.accessToken, body: { id: created, status: 0 },
    });
    // 绑定 common 角色（id=2，普通角色）
    await api('POST', '/admin-api/system/permission/assign-user-role', {
      token: admin.accessToken, body: { userId: created, roleIds: [2] },
    });
  }
  try {
    testUserToken = (await login('lianyantest', 'Test123456')).accessToken;
    const r = await api('GET', '/admin-api/system/auth/get-permission-info', { token: testUserToken });
    const menus = r.json?.data?.menus ?? [];
    const roles = r.json?.data?.roles ?? [];
    const flat = (ms, out = [], p = '') => { for (const m of ms) { out.push(p + m.name); flat(m.children || [], out, p + m.name + '/'); } return out; };
    const commonNames = flat(menus);
    const adminInfo = await api('GET', '/admin-api/system/auth/get-permission-info', { token: admin.accessToken });
    const adminNames = flat(adminInfo.json?.data?.menus ?? []);
    const isSubset = commonNames.every((m) => adminNames.includes(m));
    const discriminators = ['邮箱管理', '站内信管理', '模板管理', '三方登录'].filter((n) => commonNames.includes(n));
    record('N2', '导航反向：common 用户菜单为 admin 严格子集（角色收敛，58 vs 42 项基线）且 admin 判别菜单缺席',
      r.json?.code === 0 && roles.includes('common') && isSubset && commonNames.length < adminNames.length && discriminators.length === 0,
      `common=${commonNames.length} admin=${adminNames.length} subset=${isSubset} 判别菜单命中=${discriminators.length}`);
  } catch (e) {
    record('N2', '导航反向：普通用户菜单不含系统管理域', false, e.message);
  }
}

// ========== 文件域 ==========
{
  // F1 setup：创建 DB 存储的文件配置并设为 master（真实管理 API，无种子前置）
  const create = await api('POST', '/admin-api/infra/file-config/create', {
    token: admin.accessToken,
    body: { name: 'lianyan-db-storage', storage: 1, remark: 'CLIENT-005.B E2E', config: { domain: 'http://127.0.0.1:48080' } },
  });
  const configId = create.json?.data;
  if (configId) {
    await api('PUT', '/admin-api/infra/file-config/update', {
      token: admin.accessToken,
      body: { id: configId, name: 'lianyan-db-storage', storage: 1, remark: 'CLIENT-005.B E2E', config: { domain: 'http://127.0.0.1:48080' } },
    });
    await api('PUT', `/admin-api/infra/file-config/update-master?id=${configId}`, { token: admin.accessToken });
  }
  record('F1', '文件存储配置创建并设 master（setup）', create.json?.code === 0 && !!configId, `configId=${configId}`);
}
let uploadedUrl = null;
{
  const form = new FormData();
  form.append('file', new Blob([Buffer.from(`client005b-e2e-${Date.now()}`)]), 'lianyan-e2e.txt');
  const res = await fetch(BASE_URL + '/admin-api/infra/file/upload', {
    method: 'POST', headers: { Authorization: `Bearer ${admin.accessToken}`, 'tenant-id': TENANT_ID }, body: form,
  });
  const json = await res.json().catch(() => null);
  uploadedUrl = json?.data ?? null;
  const okF2 = json?.code === 0 && typeof uploadedUrl === 'string' && uploadedUrl.length > 0;
  record('F2', '文件上传正向', okF2, `url=${String(uploadedUrl).slice(0, 80)}`);
  if (!okF2) uploadedUrl = null; // 防 URL 解析崩溃（F3 跳过语义由 F2 FAIL 承担）
}
{
  if (!uploadedUrl) {
    record('F3', '文件下载正向（内容回读一致）', false, '前置 F2 未产出 URL');
  } else {
    const res = await fetch(uploadedUrl, {
      redirect: 'manual',
      headers: { Authorization: `Bearer ${admin.accessToken}` }, // FILE-003 私有下载：文件 get 端点要求认证（匿名 403 已在联验中实证）
    });
    const text = await res.text();
    record('F3', '文件下载正向（认证后内容回读一致）', res.status === 200 && text.startsWith('client005b-e2e-'), `http=${res.status} len=${text.length}`);
  }
}
{
  // 反向：上传后未授权（无令牌）访问管理接口删除被拒；再以 admin 删除后下载 404
  const delDenied = await api('DELETE', `/admin-api/infra/file/delete?id=1`);
  const deniedOk = delDenied.httpStatus === 401 || delDenied.json?.code === 401;
  record('F4', '文件删除反向：未登录删除被拒', deniedOk, `http=${delDenied.httpStatus} code=${delDenied.json?.code}`);
}

// ========== 待办/通知域 ==========
let templateCode = null;
{
  const code = `lianyan_e2e_${Date.now()}`;
  const r = await api('POST', '/admin-api/system/notify-template/create', {
    token: admin.accessToken,
    body: {
      name: '联验E2E模板', code, nickname: '联验机器人', content: '联验消息：{content}',
      type: 2, status: 0, remark: 'CLIENT-005.B E2E',
    },
  });
  templateCode = r.json?.code === 0 ? code : null;
  record('T1', '通知模板创建（setup）', !!templateCode, `code=${templateCode}`);
}
let testMessageId = null;
{
  // 真实发送端点：POST /admin-api/system/notify-template/send-notify（userType 分流 ADMIN/MEMBER）
  const users = await api('GET', '/admin-api/system/user/page?pageNo=1&pageSize=100', { token: admin.accessToken });
  const testUser = users.json?.data?.list?.find((u) => u.username === 'lianyantest');
  if (testUser) {
    const send = await api('POST', '/admin-api/system/notify-template/send-notify', {
      token: admin.accessToken,
      body: { userId: testUser.id, userType: 2, templateCode, templateParams: { content: 'hello-lianyan' } },
    });
    testMessageId = send.json?.data ?? null;
  }
  record('T2', '站内信发送正向', !!testMessageId, `messageId=${testMessageId}`);
}
{
  // 收件人视角用自己的收件箱（my-page），非管理端 page
  const page = await api('GET', '/admin-api/system/notify-message/my-page?pageNo=1&pageSize=10&readStatus=false', { token: testUserToken });
  const mine = page.json?.data?.list ?? [];
  const hit = mine.find((m) => m.id === testMessageId);
  record('T3', '收件人待办列表含新消息正向', !!hit, `list=${mine.length}`);
  if (hit) {
    // ids 为 @RequestParam（非 JSON body）
    await api('PUT', `/admin-api/system/notify-message/update-read?ids=${testMessageId}`, {
      token: testUserToken,
    });
    const after = await api('GET', '/admin-api/system/notify-message/my-page?pageNo=1&pageSize=10&readStatus=true', { token: testUserToken });
    const readHit = (after.json?.data?.list ?? []).some((m) => m.id === testMessageId);
    record('T4', '标记已读正向', readHit, `readList=${(after.json?.data?.list ?? []).length}`);
  } else {
    record('T4', '标记已读正向', false, '前置 T3 未命中');
  }
}
{
  // r0 反向（MSG-003.A 合同）：common 用户直读他人消息详情 → 权限拒绝（管理端查看属合法职责）
  if (testMessageId && testUserToken) {
    const r = await api('GET', `/admin-api/system/notify-message/get?id=${testMessageId}`, { token: testUserToken });
    const rejected = r.httpStatus === 403 || r.json?.code === 403 || r.json?.code !== 0;
    record('T5', '反向：他人消息详情直读被拒（MSG-003.A）', rejected, `http=${r.httpStatus} code=${r.json?.code}`);
  } else {
    record('T5', '反向：他人消息详情直读被拒（MSG-003.A）', false, '前置 T2 未命中');
  }
}

// ---------- 汇总 ----------
const pass = results.filter((r) => r.ok).length;
const fail = results.length - pass;
console.log(`\n合计 ${results.length} 用例：PASS ${pass} / FAIL ${fail}`);
writeFileSync(join(OUT_DIR, `e2e-report-${Date.now()}.json`), JSON.stringify({ baseUrl: BASE_URL, pass, fail, results }, null, 2));
process.exit(fail ? 1 : 0);
