/**
 * ZS-SYS-001.A 用例模块：跨类断言（伪造租户头 / ADMIN-MEMBER Token 串用 / LOGIN-004 门控 / 坏 Token）。
 *
 * 断言的既有交付：ZS-SEC-012.A（服务端主体不被 tenant-id 头改写，在真实进程复证）、
 * ZS-LOGIN-004（登录方式门控默认全关、账密不受限）、ZS-SEC-005（错误响应契约）。
 */
export async function run(ctx) {
  const { record, request, state, pgQuery } = ctx;
  const t1 = state.t1;
  const t2 = state.t2;

  // ---------- SYS-X-1：伪造 tenant-id 头不能改服务端主体（Token 租户与头不一致 → 403） ----------
  const rForged = await request('GET', '/admin-api/system/user/page?pageNo=1&pageSize=10', {
    token: t2.token, tenantId: t1.tenantId, // T2 的 Token + 伪造 T1 的租户头
  });
  const rForgedProbe = await request('GET', `/admin-api/system/user/get?id=${state.createdUserIds?.normal ?? 1}`, {
    token: t2.token, tenantId: t1.tenantId,
  });
  record('SYS-X-1 伪造 tenant-id 头不能改服务端主体',
    rForged.body?.code === 403 && rForgedProbe.body?.code === 403,
    `T2 Token + tenant-id=${t1.tenantId}：page code=${rForged.body?.code}，get code=${rForgedProbe.body?.code}（均期望 403，TenantSecurityWebFilter）`);

  // ---------- SYS-X-2：ADMIN/MEMBER Token 串用拒绝（MEMBER 类型 Token 调 admin-api） ----------
  const rMemberType = await request('GET', '/admin-api/system/user/page?pageNo=1&pageSize=10', {
    token: 'sys001-member-type-token', tenantId: t1.tenantId, // 夹具直种 user_type=2 的 Token 行
  });
  record('SYS-X-2 MEMBER Token 串用 admin-api 拒绝',
    rMemberType.body?.code === 403,
    `MEMBER 类型 Token 调 /admin-api code=${rMemberType.body?.code}（期望 403，用户类型不匹配 AccessDenied）`);

  // ---------- SYS-X-3：LOGIN-004 门控（sms/social/register/reset 默认关；账密不受限） ----------
  const rSmsLogin = await request('POST', '/admin-api/system/auth/sms-login', {
    tenantId: t1.tenantId, body: { mobile: '15601690101', code: '613842' },
  });
  const rSendSms = await request('POST', '/admin-api/system/auth/send-sms-code', {
    tenantId: t1.tenantId, body: { mobile: '15601690101', scene: 21 }, // SmsSceneEnum.ADMIN_MEMBER_LOGIN=21
  });
  const rPwdLogin = await request('POST', '/admin-api/system/auth/login', {
    tenantId: t1.tenantId, body: { username: state.T1.username, password: state.T1.password },
  });
  record('SYS-X-3 LOGIN-004 门控默认关、账密不受限',
    rSmsLogin.body?.code === 1002000009 && rSendSms.body?.code === 1002000009 && rPwdLogin.body?.code === 0,
    `sms-login code=${rSmsLogin.body?.code}，send-sms-code code=${rSendSms.body?.code}（均期望 1002000009 门控关闭），账密登录 code=${rPwdLogin.body?.code}（期望 0）`);

  // ---------- SYS-X-4：伪造/垃圾 Token 401 ----------
  const rBadToken = await request('GET', '/admin-api/system/user/page?pageNo=1&pageSize=10', {
    token: 'sys001-forged-token-0123456789', tenantId: t1.tenantId,
  });
  record('SYS-X-4 伪造 Token 401',
    rBadToken.body?.code === 401,
    `伪造 Token code=${rBadToken.body?.code}（期望 401，AuthenticationEntryPoint；LOGIN-005.A DB 权威校验后自愈拒绝）`);
}
