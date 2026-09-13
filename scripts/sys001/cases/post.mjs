/**
 * ZS-SYS-001.A 用例模块：岗位（SYS-POST-P / SYS-POST-N）。
 *
 * 断言的既有交付：ZS-IAM-003（岗位与用户岗位关联）。
 * 反向含「有引用删除受控」（docs/05 §15.1 岗位行反向验收），按基线先行口径：若当前实现放行
 * 有引用删除，则记录 FAIL 并登记缺口（归口见报告），不以放宽断言制造绿灯。
 */
export async function run(ctx) {
  const { record, request, state, pgQuery } = ctx;
  const t1 = state.t1;
  const t2 = state.t2;
  const tag = ctx.runId.replace(/\D/g, '').slice(-8);

  // ---------- SYS-POST-P1：新增岗位，PG 读回 ----------
  const rCreate = await request('POST', '/admin-api/system/post/create', {
    token: t1.token, tenantId: t1.tenantId,
    body: { name: `SYS001岗位${tag}`, code: `sys001p${tag}`, sort: 9, status: 0 },
  });
  const postId = rCreate.body?.data;
  record('SYS-POST-P1 新增岗位（PG 读回）',
    rCreate.body?.code === 0 && postId != null
    && pgQuery(`SELECT count(*) FROM system_post WHERE id=${postId} AND code='sys001p${tag}' AND deleted=0 AND tenant_id=${t1.tenantId} AND status=0`) === '1',
    `code=${rCreate.body?.code} id=${postId}（PG：code/status/tenant 一致）`);

  // ---------- SYS-POST-P2：修改 + 查询读回（get/page/simple-list） ----------
  const rUpdate = await request('PUT', '/admin-api/system/post/update', {
    token: t1.token, tenantId: t1.tenantId,
    body: { id: postId, name: `SYS001岗位改${tag}`, code: `sys001p${tag}`, sort: 9, status: 0 },
  });
  const rGet = await request('GET', `/admin-api/system/post/get?id=${postId}`, { token: t1.token, tenantId: t1.tenantId });
  const rSimple = await request('GET', '/admin-api/system/post/simple-list', { token: t1.token, tenantId: t1.tenantId });
  record('SYS-POST-P2 修改+查询读回',
    rUpdate.body?.code === 0 && rGet.body?.data?.name === `SYS001岗位改${tag}`
    && JSON.stringify(rSimple.body?.data || []).includes(String(postId))
    && pgQuery(`SELECT name FROM system_post WHERE id=${postId} AND deleted=0`) === `SYS001岗位改${tag}`,
    `update code=${rUpdate.body?.code}，get name=${rGet.body?.data?.name}（PG 读回一致；simple-list 含新岗位）`);

  // ---------- SYS-POST-P3：用户岗位关联读回（新建用户挂岗，user/get 与 PG 双向一致） ----------
  const rUser = await request('POST', '/admin-api/system/user/create', {
    token: t1.token, tenantId: t1.tenantId,
    body: { username: `t1post${tag}`.slice(0, 30), password: 'Post001Pass', nickname: '岗位关联用户', postIds: [postId], status: 0 },
  });
  const userId = rUser.body?.data;
  const rUserGet = await request('GET', `/admin-api/system/user/get?id=${userId}`, { token: t1.token, tenantId: t1.tenantId });
  record('SYS-POST-P3 用户岗位关联读回',
    rUser.body?.code === 0 && JSON.stringify(rUserGet.body?.data?.postIds || []).includes(String(postId))
    && pgQuery(`SELECT post_ids FROM system_users WHERE id=${userId} AND deleted=0`) === `[${postId}]`,
    `user/get postIds=${JSON.stringify(rUserGet.body?.data?.postIds)}（PG post_ids=${pgQuery(`SELECT post_ids FROM system_users WHERE id=${userId}`)}）`);

  // ---------- SYS-POST-N1：有引用删除受控（岗位被用户引用时删除应被拒） ----------
  const rDelBusy = await request('DELETE', `/admin-api/system/post/delete?id=${postId}`, { token: t1.token, tenantId: t1.tenantId });
  const stillThere = pgQuery(`SELECT count(*) FROM system_post WHERE id=${postId} AND deleted=0`) === '1';
  // 受控拒绝=业务码非 0 且非 500 基础设施异常；500 不得计为保护成功（缺口登记见报告 GAP-2）
  const controlled = rDelBusy.body?.code !== 0 && rDelBusy.body?.code !== 500 && stillThere;
  record('SYS-POST-N1 有引用删除受控',
    controlled,
    `delete(被引用岗位) code=${rDelBusy.body?.code}（期望受控业务拒绝而非放行/基础设施异常），PG 行${stillThere ? '仍在' : '已被删'}`);

  // ---------- SYS-POST-N2：非法状态 400 ----------
  const rBadStatus = await request('POST', '/admin-api/system/post/create', {
    token: t1.token, tenantId: t1.tenantId,
    body: { name: `SYS001坏状态${tag}`, code: `sys001bs${tag}`, sort: 10, status: 3 }, // 3 不在 CommonStatusEnum
  });
  record('SYS-POST-N2 非法状态 400',
    rBadStatus.body?.code === 400,
    `create(status=3) code=${rBadStatus.body?.code}（期望 400，@InEnum 校验）`);

  // ---------- SYS-POST-N3：错租户不能修改（T2 管理员改 T1 岗位） ----------
  const rCross = await request('PUT', '/admin-api/system/post/update', {
    token: t2.token, tenantId: t2.tenantId,
    body: { id: postId, name: 'T2越权改岗', code: `sys001p${tag}`, sort: 9, status: 0 },
  });
  // T2 套餐不含岗位管理 → 方法级 @PreAuthorize 403 先于租户范围校验；若可达则 1002005000。
  // 两者均满足 §15.1「错租户不能修改」反向验收。
  const pgName = pgQuery(`SELECT name FROM system_post WHERE id=${postId} AND deleted=0`);
  const n3Ok = [403, 1002005000].includes(rCross.body?.code)
    && pgName !== 'T2越权改岗'; // 无论命中 403 还是租户范围拒绝，越权改名都不得落库
  record('SYS-POST-N3 错租户修改拒绝', n3Ok,
    `T2 update T1 岗位 code=${rCross.body?.code}（403=无权直调 / 1002005000=租户范围拒绝），PG 名称=${pgName}（未变为 T2 越权值）`);

  state.postState = { postId, userId };
}
