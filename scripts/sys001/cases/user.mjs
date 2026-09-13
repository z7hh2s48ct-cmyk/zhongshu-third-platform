/**
 * ZS-SYS-001.A 用例模块：用户（SYS-USER-P / SYS-USER-N）。
 *
 * 断言的既有交付：ZS-LOGIN-003（禁用即 invalidateUserSessions）、ZS-LOGIN-005.A（DB 权威校验）、
 * ZS-PERM-001.A（他租户 ID 拒绝）、ZS-SEC-008（非法参数 400）。
 * 全部经真实 HTTP（Bearer Token + tenant-id 头）触发，PG 读回以 docker exec psql 断言。
 */
export async function run(ctx) {
  const { record, request, state, pgQuery } = ctx;
  const t1 = state.t1;
  const t2 = state.t2;
  const tag = ctx.runId.replace(/\D/g, '').slice(-8); // 数字标签（username 仅允许字母数字）

  // ---------- SYS-USER-P1：创建技术用户 + 岗位关联，PG 读回一致 ----------
  const username = `sys001u${tag}`;
  const rCreate = await request('POST', '/admin-api/system/user/create', {
    token: t1.token, tenantId: t1.tenantId,
    body: { username, password: 'Usr001Pass', nickname: 'SYS001技术用户', deptId: 101, postIds: [2], mobile: '15601690101', status: 0 },
  });
  const uid = rCreate.body?.data;
  record('SYS-USER-P1 创建用户+岗位关联（PG 读回）',
    rCreate.body?.code === 0 && uid != null
    && pgQuery(`SELECT count(*) FROM system_users WHERE id=${uid} AND username='${username}' AND deleted=0 AND tenant_id=${t1.tenantId} AND post_ids='[2]' AND status=0`) === '1',
    `code=${rCreate.body?.code} id=${uid}（PG：username/post_ids/status/deleted 一致）`);

  // ---------- SYS-USER-P2：查询/修改，PG 读回一致；岗位关联可读回 ----------
  const rUpdate = await request('PUT', '/admin-api/system/user/update', {
    token: t1.token, tenantId: t1.tenantId,
    body: { id: uid, username, nickname: 'SYS001技术用户改', deptId: 101, postIds: [2, 4], mobile: '15601690101', status: 0 },
  });
  const rGet = await request('GET', `/admin-api/system/user/get?id=${uid}`, { token: t1.token, tenantId: t1.tenantId });
  record('SYS-USER-P2 修改+查询读回（含岗位关联）',
    rUpdate.body?.code === 0 && rGet.body?.data?.nickname === 'SYS001技术用户改'
    && JSON.stringify(rGet.body?.data?.postIds || []).includes('4')
    && pgQuery(`SELECT nickname FROM system_users WHERE id=${uid} AND deleted=0`) === 'SYS001技术用户改',
    `update code=${rUpdate.body?.code}，get nickname=${rGet.body?.data?.nickname}，postIds=${JSON.stringify(rGet.body?.data?.postIds)}（PG 读回一致）`);

  // ---------- SYS-USER-N1：无权操作 403（T2 无角色用户直调创建被拒） ----------
  const t2PlainUsername = `t2plain${tag}`.slice(0, 30);
  const rNoRoleUser = await request('POST', '/admin-api/system/user/create', {
    token: t2.token, tenantId: t2.tenantId,
    body: { username: t2PlainUsername, password: 'Plain1234', nickname: 'T2无角色用户', status: 0 },
  });
  const noRoleUserId = rNoRoleUser.body?.data;
  const rPlainLogin = await request('POST', '/admin-api/system/auth/login', {
    tenantId: t2.tenantId, body: { username: t2PlainUsername, password: 'Plain1234' },
  });
  const plainToken = rPlainLogin.body?.data?.accessToken;
  const rForbidden = await request('POST', '/admin-api/system/user/create', {
    token: plainToken, tenantId: t2.tenantId,
    body: { username: `x${tag}xx`, password: 'Xpass1234', nickname: '越权创建', status: 0 },
  });
  record('SYS-USER-N1 无权操作 403',
    rPlainLogin.body?.code === 0 && rForbidden.body?.code === 403,
    `无角色用户创建用户 business=${rForbidden.body?.code}（期望 403，方法级 @PreAuthorize 经真实链拒绝）`);

  // ---------- SYS-USER-N2：他租户用户 ID 拒绝（PERMISSION_ASSIGN_USER_OTHER_TENANT） ----------
  const rCrossAssign = await request('POST', '/admin-api/system/permission/assign-user-role', {
    token: t1.token, tenantId: t1.tenantId,
    body: { userId: noRoleUserId, roleIds: [1] }, // noRoleUserId 属于租户 2，T1 管理员越租户授权
  });
  const n2Ok = [1002009000, 1002003003].includes(rCrossAssign.body?.code)
    && pgQuery(`SELECT count(*) FROM system_user_role WHERE user_id=${noRoleUserId} AND deleted=0`) === '0';
  record('SYS-USER-N2 他租户用户 ID 拒绝',
    n2Ok,
    `assign-user-role code=${rCrossAssign.body?.code}（租户 1 上下文对租户 2 用户不可见→1002003003 用户不存在，可见即 1002009000 拒绝；两者均为受控拒绝），PG 无越租户授权行`);

  // ---------- SYS-USER-N3：非法参数 400 ----------
  const rBad = await request('POST', '/admin-api/system/user/create', {
    token: t1.token, tenantId: t1.tenantId,
    body: { password: 'Bad001Pass', nickname: '缺账号', status: 0 }, // 缺 username（@NotEmpty）
  });
  const rBadMobile = await request('POST', '/admin-api/system/user/create', {
    token: t1.token, tenantId: t1.tenantId,
    body: { username: `badm${tag}`.slice(0, 30), password: 'Bad001Pass', nickname: '坏手机号', mobile: 'not-a-mobile', status: 0 },
  });
  record('SYS-USER-N3 非法参数 400',
    rBad.body?.code === 400 && rBadMobile.body?.code === 400,
    `缺 username code=${rBad.body?.code}，非法手机号 code=${rBadMobile.body?.code}（均期望 400）`);

  // ---------- SYS-USER-P3：禁用后旧 Token 立即 401（LOGIN-003 invalidateUserSessions） ----------
  const targetUsername = `t1sess${tag}`.slice(0, 30);
  const rTarget = await request('POST', '/admin-api/system/user/create', {
    token: t1.token, tenantId: t1.tenantId,
    body: { username: targetUsername, password: 'Sess001Pass', nickname: '禁用撤销验证', status: 0 },
  });
  const targetId = rTarget.body?.data;
  const rTargetLogin = await request('POST', '/admin-api/system/auth/login', {
    tenantId: t1.tenantId, body: { username: targetUsername, password: 'Sess001Pass' },
  });
  const oldToken = rTargetLogin.body?.data?.accessToken;
  const rBefore = await request('GET', '/admin-api/system/auth/get-permission-info', { token: oldToken, tenantId: t1.tenantId });
  const rDisable = await request('PUT', '/admin-api/system/user/update-status', {
    token: t1.token, tenantId: t1.tenantId, body: { id: targetId, status: 1 },
  });
  const rAfter = await request('GET', '/admin-api/system/auth/get-permission-info', { token: oldToken, tenantId: t1.tenantId });
  record('SYS-USER-P3 禁用后旧 Token 立即 401',
    rTargetLogin.body?.code === 0 && rBefore.body?.code === 0 && rDisable.body?.code === 0
    && rAfter.body?.code === 401
    && pgQuery(`SELECT status FROM system_users WHERE id=${targetId} AND deleted=0`) === '1',
    `禁用前 code=${rBefore.body?.code}，禁用后旧 Token code=${rAfter.body?.code}（期望 401），PG status=1`);

  // 状态供后续模块复用
  state.createdUserIds = { normal: uid, t2NoRole: noRoleUserId, disabled: targetId };
}
