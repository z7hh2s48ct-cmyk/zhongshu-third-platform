/**
 * ZS-SYS-001.A 用例模块：角色（SYS-ROLE-P / SYS-ROLE-N）。
 *
 * 断言的既有交付：ZS-PERM-001.A（越界赋权/自我提权/超上限/他租户角色拒绝）、
 * ZS-PERM-004.A（停用/撤权后的取权变化持久化与立即失权）、ZS-CFG-003.B（套餐内授权交集）。
 * 停用角色立即失权以「T2 套餐内角色 → 授予 T2 用户 → 停用 → 旧 Token 立即 403」全真链验证。
 */
export async function run(ctx) {
  const { record, request, state, pgQuery } = ctx;
  const t1 = state.t1;
  const t2 = state.t2;
  const tag = ctx.runId.replace(/\D/g, '').slice(-8);

  // ---------- SYS-ROLE-P1：新增角色（T2 套餐内），PG 读回 ----------
  const rCreate = await request('POST', '/admin-api/system/role/create', {
    token: t2.token, tenantId: t2.tenantId,
    body: { name: `SYS001T2角色${tag}`, code: `sys001r${tag}`, sort: 5, status: 0 },
  });
  const roleId = rCreate.body?.data;
  record('SYS-ROLE-P1 新增角色（PG 读回）',
    rCreate.body?.code === 0 && roleId != null
    && pgQuery(`SELECT count(*) FROM system_role WHERE id=${roleId} AND code='sys001r${tag}' AND status=0 AND deleted=0 AND tenant_id=${t2.tenantId}`) === '1',
    `code=${rCreate.body?.code} id=${roleId}（PG：code/status/tenant 一致）`);

  // ---------- SYS-ROLE-P2：菜单分配（套餐内 100 用户管理 + 1001 用户查询），PG 读回 ----------
  const rAssign = await request('POST', '/admin-api/system/permission/assign-role-menu', {
    token: t2.token, tenantId: t2.tenantId,
    body: { roleId, menuIds: [100, 1001] },
  });
  record('SYS-ROLE-P2 套餐内菜单分配（PG 读回）',
    rAssign.body?.code === 0
    && pgQuery(`SELECT count(*) FROM system_role_menu WHERE role_id=${roleId} AND deleted=0 AND menu_id IN (100,1001)`) === '2',
    `assign-role-menu code=${rAssign.body?.code}，PG role_menu 行=${pgQuery(`SELECT count(*) FROM system_role_menu WHERE role_id=${roleId} AND deleted=0`)}`);

  // ---------- SYS-ROLE-P3：角色授予 T2 测试用户，用户取权生效 ----------
  const rUserCreate = await request('POST', '/admin-api/system/user/create', {
    token: t2.token, tenantId: t2.tenantId,
    body: { username: `t2user${tag}`.slice(0, 30), password: 'T2user123', nickname: 'T2测试用户', status: 0 },
  });
  const t2UserId = rUserCreate.body?.data;
  const rGrant = await request('POST', '/admin-api/system/permission/assign-user-role', {
    token: t2.token, tenantId: t2.tenantId,
    body: { userId: t2UserId, roleIds: [roleId] },
  });
  const rUserLogin = await request('POST', '/admin-api/system/auth/login', {
    tenantId: t2.tenantId, body: { username: `t2user${tag}`.slice(0, 30), password: 'T2user123' },
  });
  const t2UserToken = rUserLogin.body?.data?.accessToken;
  const rUserPageBefore = await request('GET', '/admin-api/system/user/page?pageNo=1&pageSize=10', { token: t2UserToken, tenantId: t2.tenantId });
  record('SYS-ROLE-P3 角色授予用户后取权生效',
    rUserCreate.body?.code === 0 && rGrant.body?.code === 0 && rUserLogin.body?.code === 0 && rUserPageBefore.body?.code === 0,
    `授予 code=${rGrant.body?.code}，被授予用户 page code=${rUserPageBefore.body?.code}（期望 0）`);

  // ---------- SYS-ROLE-P4：停用角色立即失权（旧 Token 立即 403），PG 读回 status=1 ----------
  const rDisable = await request('PUT', '/admin-api/system/role/update', {
    token: t2.token, tenantId: t2.tenantId,
    body: { id: roleId, name: `SYS001T2角色${tag}`, code: `sys001r${tag}`, sort: 5, status: 1 },
  });
  const rUserPageAfter = await request('GET', '/admin-api/system/user/page?pageNo=1&pageSize=10', { token: t2UserToken, tenantId: t2.tenantId });
  record('SYS-ROLE-P4 停用角色旧 Token 立即失权',
    rDisable.body?.code === 0 && rUserPageBefore.body?.code === 0 && rUserPageAfter.body?.code === 403
    && pgQuery(`SELECT status FROM system_role WHERE id=${roleId} AND deleted=0`) === '1',
    `停用 code=${rDisable.body?.code}，停用后旧 Token page code=${rUserPageAfter.body?.code}（期望 403），PG status=1`);

  // ---------- SYS-ROLE-N1：越界赋权（套餐外菜单 102 菜单管理）TENANT_PACKAGE_MENU_EXCEED ----------
  //   以 T2 管理员角色（套餐内授权对象）为载体；其 id 自 DB 读出（租户管理员由租户创建链生成）
  const rExceed = await request('POST', '/admin-api/system/permission/assign-role-menu', {
    token: t2.token, tenantId: t2.tenantId,
    body: { roleId, menuIds: [100, 1001, 102] }, // 载体=本模块创建的普通角色；102 菜单管理不在套餐
  });
  const n1Count = pgQuery(`SELECT count(*) FROM system_role_menu WHERE role_id=${roleId} AND deleted=0 AND menu_id=102`);
  const pkgMenuIds = pgQuery(`SELECT menu_ids FROM system_tenant_package WHERE id=${state.packageId}`);
  record('SYS-ROLE-N1 越界赋权拒绝（1002016005，CFG-003.B 跨 PG 复验）',
    rExceed.body?.code === 1002016005 && n1Count === '0',
    `assign-role-menu（混入 102）code=${rExceed.body?.code}（期望 1002016005），PG 越界行=${n1Count}，套餐 menu_ids=${pkgMenuIds}，resp=${JSON.stringify(rExceed.body).slice(0,120)}（role ${roleId} 租户=${t2.tenantId}）`);

  // ---------- SYS-ROLE-N2：自我提权拒绝（PERMISSION_SELF_ELEVATION） ----------
  const rRole2 = await request('POST', '/admin-api/system/role/create', {
    token: t2.token, tenantId: t2.tenantId,
    body: { name: `SYS001T2自提${tag}`, code: `sys001s${tag}`, sort: 6, status: 0 }, // 开启状态的新角色
  });
  const role2Id = rRole2.body?.data;
  const rSelf = await request('POST', '/admin-api/system/permission/assign-user-role', {
    token: t2.token, tenantId: t2.tenantId,
    body: { userId: state.t2AdminUserId, roleIds: [role2Id] }, // 为自身新增角色
  });
  record('SYS-ROLE-N2 自我提权拒绝（1002009004）',
    rRole2.body?.code === 0 && rSelf.body?.code === 1002009004,
    `新建载体 code=${rRole2.body?.code}/${rRole2.body?.msg}，assign-user-role(self) code=${rSelf.body?.code}/${rSelf.body?.msg}（期望 1002009004）`);

  // ---------- SYS-ROLE-N3：超上限（授予 super_admin 编码角色）PERMISSION_GRANT_EXCEED_CEILING ----------
  //   该角色由夹具直种（API 创建被 ROLE_ADMIN_CODE_ERROR 拦截，本身即为反向受控）
  const rCeiling = await request('POST', '/admin-api/system/permission/assign-user-role', {
    token: t2.token, tenantId: t2.tenantId,
    body: { userId: t2UserId, roleIds: [910301] },
  });
  record('SYS-ROLE-N3 授予特权角色超上限拒绝（1002009003）',
    rCeiling.body?.code === 1002009003
    && pgQuery(`SELECT count(*) FROM system_user_role WHERE user_id=${t2UserId} AND role_id=910301 AND deleted=0`) === '0',
    `assign-user-role(super_admin 编码) code=${rCeiling.body?.code}（期望 1002009003），PG 无授权行`);

  // ---------- SYS-ROLE-N4：他租户角色拒绝（批量混入 T1 的 super_admin 角色 1） ----------
  const rCross = await request('POST', '/admin-api/system/permission/assign-user-role', {
    token: t2.token, tenantId: t2.tenantId,
    body: { userId: t2UserId, roleIds: [roleId, 1] }, // roleId 为 T2 角色，1 为 T1 的 super_admin
  });
  // 混入的 T1 角色 1 在租户 123 上下文不可见 → 命中角色存在性受控（1002002000）；可见场景（如系统侧批量）为 1002009001。
  // 两者均为受控拒绝且不落库，满足「他租户角色拒绝」验收。
  const n4Ok = [1002009001, 1002002000].includes(rCross.body?.code)
    && pgQuery(`SELECT count(*) FROM system_user_role WHERE user_id=${t2UserId} AND role_id=1 AND deleted=0`) === '0';
  record('SYS-ROLE-N4 他租户角色批量混入拒绝', n4Ok,
    `assign-user-role（混入 T1 角色 1）code=${rCross.body?.code}/${rCross.body?.msg}（1002002000=不可见拒绝 / 1002009001=归属拒绝），PG 无越租户行`);

  // 状态供后续模块复用
  state.roleState = { roleId, t2UserId, t2UserToken };
}
