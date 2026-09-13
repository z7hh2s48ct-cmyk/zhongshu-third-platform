/**
 * ZS-SYS-001.A 用例模块：公告（SYS-NOTICE-P / SYS-NOTICE-N）。
 *
 * 断言的既有交付：ZS-SEC-002（端点清单）、ZS-SEC-008（校验契约：缺参数 400、富文本边界——
 * 「全局字符串清洗不当作权限控制」，服务端不静默变异数据，输出编码归 Web 渲染层 B06）。
 */
export async function run(ctx) {
  const { record, request, state, pgQuery } = ctx;
  const t1 = state.t1;
  const t2 = state.t2;
  const tag = ctx.runId.replace(/\D/g, '').slice(-8);
  const N = '/admin-api/system/notice';

  // ---------- SYS-NOTICE-P1：新增公告（合法富文本），PG 读回 ----------
  const richText = '<p>SYS001 通知正文 <b>加粗</b> 与 <a href="https://zszj.example.com">链接</a></p>';
  const rCreate = await request('POST', `${N}/create`, {
    token: t1.token, tenantId: t1.tenantId,
    body: { title: `SYS001公告${tag}`, type: 1, content: richText, status: 0 },
  });
  const noticeId = rCreate.body?.data;
  const rGet = await request('GET', `${N}/get?id=${noticeId}`, { token: t1.token, tenantId: t1.tenantId });
  record('SYS-NOTICE-P1 新增公告（合法富文本，PG 读回）',
    rCreate.body?.code === 0 && noticeId != null && rGet.body?.data?.content === richText
    && pgQuery(`SELECT count(*) FROM system_notice WHERE id=${noticeId} AND deleted=0 AND tenant_id=${t1.tenantId}`) === '1',
    `code=${rCreate.body?.code} id=${noticeId}，读回内容与写入一致（服务端不静默变异）`);

  // ---------- SYS-NOTICE-P2：编辑 + 状态变更 + 状态过滤（停用公告不混入启用过滤结果） ----------
  const rUpdate = await request('PUT', `${N}/update`, {
    token: t1.token, tenantId: t1.tenantId,
    body: { id: noticeId, title: `SYS001公告改${tag}`, type: 1, content: richText, status: 1 }, // 编辑并停用
  });
  const rEnabled = await request('POST', `${N}/create`, {
    token: t1.token, tenantId: t1.tenantId,
    body: { title: `SYS001启用公告${tag}`, type: 1, content: '<p>启用中</p>', status: 0 },
  });
  const enabledId = rEnabled.body?.data;
  const rPageEnabled = await request('GET', `${N}/page?pageNo=1&pageSize=100&status=0`, { token: t1.token, tenantId: t1.tenantId });
  const enabledList = rPageEnabled.body?.data?.list || [];
  const pageFilterOk = enabledList.some((n) => n.id === enabledId) && !enabledList.some((n) => n.id === noticeId);
  record('SYS-NOTICE-P2 编辑+状态变更+状态过滤（PG 读回）',
    rUpdate.body?.code === 0 && rEnabled.body?.code === 0 && pageFilterOk
    && pgQuery(`SELECT status FROM system_notice WHERE id=${noticeId} AND deleted=0`) === '1',
    `编辑停用 code=${rUpdate.body?.code}；page(status=0) 含启用 ${enabledId}、不含停用 ${noticeId}（过滤一致）`);

  // ---------- SYS-NOTICE-P3：删除持久化（逻辑删除 deleted=1） ----------
  const rDelete = await request('DELETE', `${N}/delete?id=${noticeId}`, { token: t1.token, tenantId: t1.tenantId });
  record('SYS-NOTICE-P3 删除持久化（PG deleted=1）',
    rDelete.body?.code === 0
    && pgQuery(`SELECT deleted FROM system_notice WHERE id=${noticeId}`) === '1',
    `delete code=${rDelete.body?.code}，PG deleted=1`);

  // ---------- SYS-NOTICE-N1：无权创建/改删 403（T2 套餐无公告管理） ----------
  const rNoPermCreate = await request('POST', `${N}/create`, {
    token: t2.token, tenantId: t2.tenantId,
    body: { title: `T2越权公告${tag}`, type: 1, content: '<p>x</p>', status: 0 },
  });
  const rNoPermUpdate = await request('PUT', `${N}/update`, {
    token: t2.token, tenantId: t2.tenantId,
    body: { id: enabledId, title: 'T2越权改', type: 1, content: '<p>x</p>', status: 0 },
  });
  const rNoPermDelete = await request('DELETE', `${N}/delete?id=${enabledId}`, { token: t2.token, tenantId: t2.tenantId });
  record('SYS-NOTICE-N1 无权创建/改删 403',
    rNoPermCreate.body?.code === 403 && rNoPermUpdate.body?.code === 403 && rNoPermDelete.body?.code === 403
    && pgQuery(`SELECT count(*) FROM system_notice WHERE id=${enabledId} AND deleted=0`) === '1',
    `T2 create/update/delete code=${rNoPermCreate.body?.code}/${rNoPermUpdate.body?.code}/${rNoPermDelete.body?.code}（均期望 403），PG 公告完好`);

  // ---------- SYS-NOTICE-N2：富文本可执行内容边界（服务端不静默变异；传输层 JSON 安全编码；输出编码归 B06） ----------
  const xssContent = '<p>ok</p><script>alert("sys001")</script><img src=x onerror="alert(1)">';
  const rXss = await request('POST', `${N}/create`, {
    token: t1.token, tenantId: t1.tenantId,
    body: { title: `SYS001XSS探针${tag}`, type: 1, content: xssContent, status: 0 },
  });
  const xssId = rXss.body?.data;
  const rXssGet = await request('GET', `${N}/get?id=${xssId}`, { token: t1.token, tenantId: t1.tenantId });
  const jsonTransport = rXssGet.contentType.includes('application/json');
  record('SYS-NOTICE-N2 富文本边界登记（JSON 传输安全编码；清洗/输出编码归 Web 渲染层）',
    rXss.body?.code === 0 && jsonTransport
    && pgQuery(`SELECT content FROM system_notice WHERE id=${xssId} AND deleted=0`) === xssContent,
    `存储原样（服务端不变异、不把全局清洗当权限）；响应 Content-Type=${rXssGet.contentType.split(';')[0]}（JSON 安全编码）；可执行内容在 Web 端输出编码归 ZS-SYS-001.B（B06）验证`);
  await request('DELETE', `${N}/delete?id=${xssId}`, { token: t1.token, tenantId: t1.tenantId });

  // ---------- SYS-NOTICE-N3：标题超长 400（@Size(50)） ----------
  const rLongTitle = await request('POST', `${N}/create`, {
    token: t1.token, tenantId: t1.tenantId,
    body: { title: '超'.repeat(51), type: 1, content: '<p>x</p>', status: 0 },
  });
  record('SYS-NOTICE-N3 标题超长 400',
    rLongTitle.body?.code === 400,
    `title 51 字符 code=${rLongTitle.body?.code}（期望 400，@Size(50)）`);

  // 清理启用探针公告
  await request('DELETE', `${N}/delete?id=${enabledId}`, { token: t1.token, tenantId: t1.tenantId });
  state.noticeState = { noticeId, enabledId };
}
