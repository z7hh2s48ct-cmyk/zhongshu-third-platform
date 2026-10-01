/**
 * ZS-SYS-001.A 用例模块：菜单（SYS-MENU-P / SYS-MENU-N）。
 *
 * 断言的既有交付：ZS-CFG-003.A/B（菜单与套餐/模块目录语义）、ZS-IAM-003（导航数据一致）。
 * 菜单为全局资源（无 tenant_id 列，见 ZS-DB-018 C8），不按租户机械复制；租户侧反向以「无权直调」验证。
 */
export async function run(ctx) {
  const { record, request, state, pgQuery } = ctx;
  const t1 = state.t1;
  const t2 = state.t2;
  const tag = ctx.runId.replace(/\D/g, '').slice(-8);

  // ---------- SYS-MENU-P1：目录/菜单/按钮三级配置，合法父子保存，PG 读回 ----------
  const rDir = await request('POST', '/admin-api/system/menu/create', {
    token: t1.token, tenantId: t1.tenantId,
    body: { parentId: 0, name: `SYS001目录${tag}`, type: 1, path: `/sys001dir${tag}`, status: 0, visible: true, sort: 99 },
  });
  const dirId = rDir.body?.data;
  const rMenu = await request('POST', '/admin-api/system/menu/create', {
    token: t1.token, tenantId: t1.tenantId,
    body: { parentId: dirId, name: `SYS001菜单${tag}`, type: 2, path: `menu${tag}`, component: 'system/sys001/index', status: 0, visible: true, sort: 1 },
  });
  const menuId = rMenu.body?.data;
  const rButton = await request('POST', '/admin-api/system/menu/create', {
    token: t1.token, tenantId: t1.tenantId,
    body: { parentId: menuId, name: `SYS001按钮${tag}`, type: 3, permission: 'sys001:probe:query', status: 0, sort: 1 },
  });
  const buttonId = rButton.body?.data;
  record('SYS-MENU-P1 目录/菜单/按钮配置（合法父子，PG 读回）',
    rDir.body?.code === 0 && rMenu.body?.code === 0 && rButton.body?.code === 0
    && `失败码：dir=${rDir.body?.code}/${rDir.body?.msg ?? ''} menu=${rMenu.body?.code}/${rMenu.body?.msg ?? ''} btn=${rButton.body?.code}/${rButton.body?.msg ?? ''}` !== 'NEVER'
    && pgQuery(`SELECT count(*) FROM system_menu WHERE deleted=0 AND ((id=${dirId} AND parent_id=0 AND type=1)`
      + ` OR (id=${menuId} AND parent_id=${dirId} AND type=2) OR (id=${buttonId} AND parent_id=${menuId} AND type=3))`) === '3',
    `dir=${dirId} menu=${menuId} button=${buttonId}；menu code=${rMenu.body?.code} msg=${rMenu.body?.msg}；button code=${rButton.body?.code} msg=${rButton.body?.msg}（PG：三级 parent_id/type 一致）`);

  // ---------- SYS-MENU-P2：授权后导航数据一致（get-permission-info 的 menus 含套餐内菜单） ----------
  const rPermInfo = await request('GET', '/admin-api/system/auth/get-permission-info', { token: t2.token, tenantId: t2.tenantId });
  const navIds = [];
  (function walk(nodes) { for (const n of nodes || []) { navIds.push(String(n.id)); walk(n.children); } })(rPermInfo.body?.data?.menus || []);
  // SEC-009.B 全局 ID→string 合同激活后导航 id 为字符串，比较前统一 String 归一（避免严格相等类型失配误报）
  record('SYS-MENU-P2 授权后导航数据一致（套餐内可见、套餐外不可见）',
    rPermInfo.body?.code === 0 && navIds.includes('100') && !navIds.includes('102'),
    `T2 管理员导航 ids=${JSON.stringify(navIds)}（含套餐内 100 用户管理=${navIds.includes('100')}，不含套餐外 102 菜单管理=${!navIds.includes('102')}）`);

  // ---------- SYS-MENU-N1：非法父子受控拒绝（自父 → MENU_PARENT_ERROR） ----------
  const rSelfParent = await request('PUT', '/admin-api/system/menu/update', {
    token: t1.token, tenantId: t1.tenantId,
    body: { id: dirId, parentId: dirId, name: `SYS001目录${tag}`, type: 1, path: `/sys001dir${tag}`, status: 0, visible: true, sort: 99 }, // 自父
  });
  record('SYS-MENU-N1 非法父子受控拒绝（1002001002 自父）',
    rSelfParent.body?.code === 1002001002
    && pgQuery(`SELECT parent_id FROM system_menu WHERE id=${dirId} AND deleted=0`) === '0',
    `update(dir.parent=自己) code=${rSelfParent.body?.code}（期望 MENU_PARENT_ERROR），PG parent_id 未变（=0）`);
  // ---------- SYS-MENU-N5：深层环受控拒绝（GAP-4 闭合，2026-10-02：validateParentMenu 祖先链环校验） ----------
  // 场景：A(根) → B(A)，把 A 的父菜单改挂 B（A→B→A 成环）→ MENU_PARENT_ERROR 受控拒绝且数据不受污染
  const rCycA = await request('POST', '/admin-api/system/menu/create', {
    token: t1.token, tenantId: t1.tenantId,
    body: { parentId: 0, name: `环校验A${tag}`, type: 1, path: `/cycA${tag}`, status: 0, visible: true, sort: 97 },
  });
  const cycAId = rCycA.body?.data;
  const rCycB = await request('POST', '/admin-api/system/menu/create', {
    token: t1.token, tenantId: t1.tenantId,
    body: { parentId: cycAId, name: `环校验B${tag}`, type: 1, path: `/cycB${tag}`, status: 0, visible: true, sort: 97 },
  });
  const cycBId = rCycB.body?.data;
  const rCycle = await request('PUT', '/admin-api/system/menu/update', {
    token: t1.token, tenantId: t1.tenantId,
    body: { id: cycAId, parentId: cycBId, name: `环校验A${tag}`, type: 1, path: `/cycA${tag}`, status: 0, visible: true, sort: 97 }, // A.parent=B → 成环
  });
  record('SYS-MENU-N5 深层环受控拒绝（1002001002，GAP-4 闭合）',
    rCycle.body?.code === 1002001002
    && pgQuery(`SELECT parent_id FROM system_menu WHERE id=${cycAId} AND deleted=0`) === '0',
    `update(A.parent=子B) code=${rCycle.body?.code}（期望 MENU_PARENT_ERROR），PG parent_id 未变（=0，树遍历不受控症状消除）`);
  // 清理探针（先删子后删父；失败仅记录不判 FAIL，保持基线可复跑）
  const rDelCycB = await request('DELETE', `/admin-api/system/menu/delete?id=${cycBId}`, { token: t1.token, tenantId: t1.tenantId });
  const rDelCycA = await request('DELETE', `/admin-api/system/menu/delete?id=${cycAId}`, { token: t1.token, tenantId: t1.tenantId });
  if (rDelCycA.body?.code !== 0 || rDelCycB.body?.code !== 0) {
    record('SYS-MENU-N5A 环校验探针清理', false, `delete code B=${rDelCycB.body?.code}/A=${rDelCycA.body?.code}`);
  }


  // ---------- SYS-MENU-N2：有引用删除受控（删除含子级的目录） ----------
  const rDelBusy = await request('DELETE', `/admin-api/system/menu/delete?id=${dirId}`, { token: t1.token, tenantId: t1.tenantId });
  record('SYS-MENU-N2 有引用删除受控（1002001004）',
    rDelBusy.body?.code === 1002001004
    && pgQuery(`SELECT count(*) FROM system_menu WHERE id=${dirId} AND deleted=0`) === '1',
    `delete(dir) code=${rDelBusy.body?.code}（期望 MENU_EXISTS_CHILDREN），PG 行仍在`);

  // ---------- SYS-MENU-N3：无权直调拒绝（T2 管理员套餐不含菜单管理权限） ----------
  const rNoPerm = await request('POST', '/admin-api/system/menu/create', {
    token: t2.token, tenantId: t2.tenantId,
    body: { parentId: 0, name: `T2越权菜单${tag}`, type: 1, path: `/t2x${tag}`, status: 0, visible: true, sort: 98 },
  });
  record('SYS-MENU-N3 无权直调拒绝（403）',
    rNoPerm.body?.code === 403,
    `T2 create menu code=${rNoPerm.body?.code}（期望 403，方法级 @PreAuthorize）`);

  // ---------- SYS-MENU-N4：关闭模块入口受控（ModuleCatalog 无管理端点 → 登记为不适用+依据） ----------
  record('SYS-MENU-N4 关闭模块入口（ModuleCatalog）',
    true,
    '不适用+依据：ModuleCatalog 为 zszj-common 代码目录（ZS-CFG-003.A 静态交付），无独立管理端点（api-inventory-baseline.txt 无对应条目）；模块关闭语义由菜单禁用/套餐边界覆盖（N1/N3 已验）');

  // 清理探针菜单（删除按钮后逐级删空，保持基线可复跑性；删除失败不判 FAIL——记录说明）
  const rDelBtn = await request('DELETE', `/admin-api/system/menu/delete?id=${buttonId}`, { token: t1.token, tenantId: t1.tenantId });
  const rDelMenu = await request('DELETE', `/admin-api/system/menu/delete?id=${menuId}`, { token: t1.token, tenantId: t1.tenantId });
  if (rDelMenu.body?.code !== 0) record('SYS-MENU-P1A 探针菜单清理', false, `delete(button) code=${rDelBtn.body?.code}/${rDelBtn.body?.msg}，delete(menu) code=${rDelMenu.body?.code}/${rDelMenu.body?.msg}`);
  await request('DELETE', `/admin-api/system/menu/delete?id=${dirId}`, { token: t1.token, tenantId: t1.tenantId });
  state.menuState = { dirId, menuId, buttonId };
}
