/**
 * ZS-SYS-001.A 用例模块：配置（SYS-CONFIG-P / SYS-CONFIG-N）。
 *
 * 断言的既有交付：ZS-CFG-001.B（敏感/秘密分级、掩码、TOCTOU 守卫）、ZS-CFG-004（值合同强校验
 * 与整数 version 乐观锁）、ConfigParamCatalog 参数目录（create/update 均按合同强校验）。
 * 配置为全局资源（infra_config 无 tenant_id）。目录参数（sys.login.captcha-max-retry 等）无种子行，
 * 由本夹具经真实 API 创建（值合同在 create 即生效）。
 */
export async function run(ctx) {
  const { record, request, state, pgQuery } = ctx;
  const t1 = state.t1;
  const tag = ctx.runId.replace(/\D/g, '').slice(-8);
  const C = '/admin-api/infra/config';
  const probeKey = `sys001probe${tag}`;
  const RETRY_KEY = 'sys.login.captcha-max-retry'; // 目录：INTEGER(1..10)，预留未接入消费方，值合同仍强校验

  // ---------- SYS-CONFIG-P1：创建参数（version 初始 0），PG 读回 ----------
  const rCreate = await request('POST', `${C}/create`, {
    token: t1.token, tenantId: t1.tenantId,
    body: { category: 'biz', name: `SYS001探针${tag}`, key: probeKey, value: '8', visible: true, remark: '回归夹具' },
  });
  const configId = rCreate.body?.data;
  const rGetNew = await request('GET', `${C}/get?id=${configId}`, { token: t1.token, tenantId: t1.tenantId });
  record('SYS-CONFIG-P1 创建参数（PG 读回，version=0）',
    rCreate.body?.code === 0 && configId != null && rGetNew.body?.data?.version === 0
    && pgQuery(`SELECT count(*) FROM infra_config WHERE id=${configId} AND config_key='${probeKey}' AND value='8' AND deleted=0`) === '1',
    `code=${rCreate.body?.code} id=${configId}，详情 version=${rGetNew.body?.data?.version}（PG：key/value 一致）`);

  // ---------- SYS-CONFIG-P2：获准参数修改（目录参数创建 + version 回传更新），PG 读回 ----------
  const rSeedCatalog = await request('POST', `${C}/create`, {
    token: t1.token, tenantId: t1.tenantId,
    body: { category: 'biz', name: 'SYS001-验证码重试上限', key: RETRY_KEY, value: '5', visible: true, remark: 'ZS-SYS-001.A 夹具创建' },
  });
  const retryId = rSeedCatalog.body?.data;
  const rGetVal = await request('GET', `${C}/get-value-by-key?key=${RETRY_KEY}`, { token: t1.token, tenantId: t1.tenantId });
  const rRow = await request('GET', `${C}/get?id=${retryId}`, { token: t1.token, tenantId: t1.tenantId });
  const curVersion = rRow.body?.data?.version;
  const rUpdate = await request('PUT', `${C}/update`, {
    token: t1.token, tenantId: t1.tenantId,
    body: { id: retryId, category: 'biz', name: 'SYS001-验证码重试上限', key: RETRY_KEY, value: '6', visible: true, version: curVersion },
  });
  const rAfter = await request('GET', `${C}/get-value-by-key?key=${RETRY_KEY}`, { token: t1.token, tenantId: t1.tenantId });
  record('SYS-CONFIG-P2 获准参数修改（version 回传，PG 读回）',
    rSeedCatalog.body?.code === 0 && rGetVal.body?.data === '5' && rUpdate.body?.code === 0 && rAfter.body?.data === '6'
    && pgQuery(`SELECT value FROM infra_config WHERE config_key='${RETRY_KEY}' AND deleted=0`) === '6',
    `目录参数 create code=${rSeedCatalog.body?.code}（值合同放行 5），version=${curVersion} 回传更新 code=${rUpdate.body?.code}，改后=${rAfter.body?.data}（PG 一致）`);

  // ---------- SYS-CONFIG-N1/N2/N3：值合同强校验（类型不匹配/越界/枚举外） ----------
  const rBadType = await request('PUT', `${C}/update`, {
    token: t1.token, tenantId: t1.tenantId,
    body: { id: retryId, category: 'biz', name: 'SYS001-验证码重试上限', key: RETRY_KEY, value: 'abc', visible: true, version: curVersion + 1 },
  });
  const rOutOfRange = await request('PUT', `${C}/update`, {
    token: t1.token, tenantId: t1.tenantId,
    body: { id: retryId, category: 'biz', name: 'SYS001-验证码重试上限', key: RETRY_KEY, value: '99', visible: true, version: curVersion + 1 },
  });
  const rBadEnum = await request('POST', `${C}/create`, {
    token: t1.token, tenantId: t1.tenantId,
    body: { category: 'biz', name: 'SYS001-文件模式越界', key: 'sys.file.upload-mode', value: 's4', visible: true }, // 枚举集 {local,oss,s3}
  });
  record('SYS-CONFIG-N1~3 值合同强校验（1001000007/8/9）',
    rBadType.body?.code === 1001000007 && rOutOfRange.body?.code === 1001000008 && rBadEnum.body?.code === 1001000009,
    `类型不匹配 code=${rBadType.body?.code}，越界 code=${rOutOfRange.body?.code}，枚举外 code=${rBadEnum.body?.code}，PG 仍为 6=${pgQuery(`SELECT value FROM infra_config WHERE config_key='${RETRY_KEY}' AND deleted=0`) === '6'}`);

  // ---------- SYS-CONFIG-N4：version 冲突（CONFIG_UPDATE_CONFLICT，ZS-CFG-004 乐观锁） ----------
  const rFresh = await request('GET', `${C}/get?id=${configId}`, { token: t1.token, tenantId: t1.tenantId });
  const staleVersion = rFresh.body?.data?.version;
  const rWin = await request('PUT', `${C}/update`, {
    token: t1.token, tenantId: t1.tenantId,
    body: { id: configId, category: 'biz', name: `SYS001探针${tag}`, key: probeKey, value: '9', visible: true, version: staleVersion },
  });
  const rStale = await request('PUT', `${C}/update`, {
    token: t1.token, tenantId: t1.tenantId,
    body: { id: configId, category: 'biz', name: `SYS001探针${tag}`, key: probeKey, value: '10', visible: true, version: staleVersion }, // 旧表单重放
  });
  record('SYS-CONFIG-N4 version 冲突拒绝（1001000010）',
    rWin.body?.code === 0 && rStale.body?.code === 1001000010
    && pgQuery(`SELECT value FROM infra_config WHERE id=${configId} AND deleted=0`) === '9',
    `先写 code=${rWin.body?.code}，旧版本重放 code=${rStale.body?.code}（期望 1001000010），PG value 仍为 9`);

  // ---------- SYS-CONFIG-N5/N6：敏感项脱敏与可见化拒绝（ZS-CFG-001.B，秘密键 system.user.init-password） ----------
  // 定位内置秘密键行（selectByIdFromCache → 详情走掩码合同）；version/掩码均以详情为准
  const rSensDetail = await request('GET', `${C}/get-value-by-key?key=system.user.init-password`, { token: t1.token, tenantId: t1.tenantId });
  const rSensPage = await request('GET', `${C}/page?pageNo=1&pageSize=100`, { token: t1.token, tenantId: t1.tenantId });
  const sensRow = (rSensPage.body?.data?.list || []).find((r) => r.key === 'system.user.init-password'); // ConfigRespVO 字段名为 key（configKey 经 convert 映射）
  const rGetById = await request('GET', `${C}/get?id=${sensRow?.id}`, { token: t1.token, tenantId: t1.tenantId });
  const rSetVisible = await request('PUT', `${C}/update`, {
    token: t1.token, tenantId: t1.tenantId,
    body: { id: sensRow?.id, category: sensRow?.category, name: sensRow?.name, key: 'system.user.init-password', value: rGetById.body?.data?.value, visible: true, version: rGetById.body?.data?.version },
  });
  const dbValue = pgQuery(`SELECT value FROM infra_config WHERE config_key='system.user.init-password' AND deleted=0`);
  const detailMasked = rGetById.body?.data?.value === '******';
  record('SYS-CONFIG-N5/N6 敏感项脱敏+可见化拒绝（1001000005）',
    detailMasked && rSetVisible.body?.code === 1001000005 && dbValue !== '******',
    `get-value-by-key code=${rSensDetail.body?.code}（敏感值拒绝返回），详情掩码=${detailMasked}，秘密键翻可见 code=${rSetVisible.body?.code}（期望 1001000005），PG 真实值未被掩码覆盖（${dbValue === '******' ? '已泄漏掩码落库' : '未落库掩码'}）`);

  // ---------- SYS-CONFIG-N7：内置保护（删除系统内置参数被拒） ----------
  const rDelBuiltIn = await request('DELETE', `${C}/delete?id=${sensRow?.id}`, { token: t1.token, tenantId: t1.tenantId });
  record('SYS-CONFIG-N7 内置保护拒绝（1001000003）',
    rDelBuiltIn.body?.code === 1001000003,
    `delete(系统内置) code=${rDelBuiltIn.body?.code}（期望 1001000003）`);

  state.configState = { configId, probeKey, retryId };
}
