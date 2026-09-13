/**
 * ZS-SYS-001.A 用例模块：字典（SYS-DICT-P / SYS-DICT-N）。
 *
 * 断言的既有交付：ZS-CFG-002.A（字典编码唯一约束，PG 部分唯一索引）、ZS-CFG-002.B（改码受控）、
 * ZS-DB-018 C8（字典为全局表，无 tenant_id，不按租户机械复制）。
 */
export async function run(ctx) {
  const { record, request, state, pgQuery } = ctx;
  const t1 = state.t1;
  const t2 = state.t2;
  const tag = ctx.runId.replace(/\D/g, '').slice(-8);
  const typeCode = `sys001_dict_${tag}`;
  const D = '/admin-api/system/dict-type';
  const DD = '/admin-api/system/dict-data';

  // ---------- SYS-DICT-P1：类型创建 + PG 读回（全局表） ----------
  const rType = await request('POST', `${D}/create`, {
    token: t1.token, tenantId: t1.tenantId,
    body: { name: `SYS001字典${tag}`, type: typeCode, status: 0, remark: '回归夹具' },
  });
  const typeId = rType.body?.data;
  record('SYS-DICT-P1 字典类型创建（PG 读回）',
    rType.body?.code === 0 && typeId != null
    && pgQuery(`SELECT count(*) FROM system_dict_type WHERE id=${typeId} AND type='${typeCode}' AND deleted=0 AND status=0`) === '1',
    `code=${rType.body?.code} id=${typeId}（PG：type/status 一致）`);

  // ---------- SYS-DICT-P2：字典项增改查 + 排序读回（PG 读回 + simple-list 排序一致） ----------
  const rData1 = await request('POST', `${DD}/create`, {
    token: t1.token, tenantId: t1.tenantId,
    body: { dictType: typeCode, label: '选项乙', value: '2', sort: 2, status: 0 },
  });
  const rData2 = await request('POST', `${DD}/create`, {
    token: t1.token, tenantId: t1.tenantId,
    body: { dictType: typeCode, label: '选项甲', value: '1', sort: 1, status: 0 },
  });
  const data2Id = rData2.body?.data;
  const rDataUpdate = await request('PUT', `${DD}/update`, {
    token: t1.token, tenantId: t1.tenantId,
    body: { id: data2Id, dictType: typeCode, label: '选项甲改', value: '1', sort: 1, status: 0 },
  });
  const rSimple = await request('GET', `${DD}/list-all-simple`, { token: t1.token, tenantId: t1.tenantId });
  const mine = (rSimple.body?.data || []).filter((d) => d.dictType === typeCode);
  record('SYS-DICT-P2 字典项增改查+排序读回',
    rData1.body?.code === 0 && rData2.body?.code === 0 && rDataUpdate.body?.code === 0
    && mine.length === 2 && mine[0]?.value === '1' && mine[0]?.label === '选项甲改' && mine[1]?.value === '2'
    && pgQuery(`SELECT count(*) FROM system_dict_data WHERE dict_type='${typeCode}' AND deleted=0`) === '2'
    && pgQuery(`SELECT label FROM system_dict_data WHERE id=${data2Id} AND deleted=0`) === '选项甲改',
    `两项按 sort 读回：[${mine.map((m) => `${m.value}:${m.label}`).join(', ')}]（simple 总数=${(rSimple.body?.data || []).length}，create 码=${rData1.body?.code}/${rData2.body?.code}/${rDataUpdate.body?.code}；PG label 读回一致）`);

  // ---------- SYS-DICT-N1：重复编码冲突（类型 type 重复 + 数据 value 重复均受控） ----------
  const rDupType = await request('POST', `${D}/create`, {
    token: t1.token, tenantId: t1.tenantId,
    body: { name: `SYS001字典重复${tag}`, type: typeCode, status: 0 },
  });
  const rDupData = await request('POST', `${DD}/create`, {
    token: t1.token, tenantId: t1.tenantId,
    body: { dictType: typeCode, label: '重复值', value: '1', sort: 3, status: 0 },
  });
  record('SYS-DICT-N1 重复编码冲突受控',
    rDupType.body?.code === 1002006004 && rDupData.body?.code !== 0
    && pgQuery(`SELECT count(*) FROM system_dict_type WHERE type='${typeCode}' AND deleted=0`) === '1'
    && pgQuery(`SELECT count(*) FROM system_dict_data WHERE dict_type='${typeCode}' AND value='1' AND deleted=0`) === '1',
    `类型重复 code=${rDupType.body?.code}（期望 1002006004），数据重复 code=${rDupData.body?.code}（期望非 0），PG 均未新增`);

  // ---------- SYS-DICT-N2：停用类型后新写入拒绝（DICT_TYPE_NOT_ENABLE） ----------
  const rDisableType = await request('PUT', `${D}/update`, {
    token: t1.token, tenantId: t1.tenantId,
    body: { id: typeId, name: `SYS001字典${tag}`, type: typeCode, status: 1, remark: '回归夹具' },
  });
  const rWriteDisabled = await request('POST', `${DD}/create`, {
    token: t1.token, tenantId: t1.tenantId,
    body: { dictType: typeCode, label: '停用后写入', value: '9', sort: 9, status: 0 },
  });
  record('SYS-DICT-N2 停用类型新写入拒绝（1002006002）',
    rDisableType.body?.code === 0 && rWriteDisabled.body?.code === 1002006002
    && pgQuery(`SELECT count(*) FROM system_dict_data WHERE dict_type='${typeCode}' AND value='9' AND deleted=0`) === '0',
    `停用 code=${rDisableType.body?.code}，停用后写入 code=${rWriteDisabled.body?.code}（期望 1002006002），PG 无新行`);

  // ---------- SYS-DICT-N3：有引用改码拒绝（DICT_TYPE_HAS_CHILDREN_ON_TYPE_CHANGE，ZS-CFG-002.B） ----------
  const rChangeCode = await request('PUT', `${D}/update`, {
    token: t1.token, tenantId: t1.tenantId,
    body: { id: typeId, name: `SYS001字典${tag}`, type: `sys001_changed_${tag}`, status: 1 },
  });
  record('SYS-DICT-N3 有引用改码拒绝（1002006006）',
    rChangeCode.body?.code === 1002006006
    && pgQuery(`SELECT type FROM system_dict_type WHERE id=${typeId} AND deleted=0`) === typeCode,
    `改码 code=${rChangeCode.body?.code}（期望 1002006006），PG type 未漂移`);

  // ---------- SYS-DICT-N4：历史显示一致（类型停用后既有字典项仍可读可解释） ----------
  const rHistory = await request('GET', `${DD}/list-all-simple`, { token: t1.token, tenantId: t1.tenantId });
  const historyOk = (rHistory.body?.data || []).some((d) => d.dictType === typeCode && d.value === '1');
  record('SYS-DICT-N4 停用类型历史项仍可读（ZS-CFG-002.B）',
    rHistory.body?.code === 0 && historyOk,
    `list-all-simple 仍含 ${typeCode}:1 = ${historyOk}`);

  // ---------- SYS-DICT-N5：越权拒绝（T2 套餐无字典管理） ----------
  const rNoPerm = await request('POST', `${D}/create`, {
    token: t2.token, tenantId: t2.tenantId,
    body: { name: `T2越权字典${tag}`, type: `t2x_${tag}`, status: 0 },
  });
  record('SYS-DICT-N5 越权拒绝（403）',
    rNoPerm.body?.code === 403,
    `T2 create dict-type code=${rNoPerm.body?.code}（期望 403）`);

  state.dictState = { typeId, typeCode };
}
