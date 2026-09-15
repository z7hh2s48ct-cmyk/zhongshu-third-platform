/**
 * ZS-OPS-002.A 部署模板可审查性校验——纯静态单测。
 * 运行：node --test scripts/ops/verify-deploy-templates.test.mjs
 *
 * 测试策略：用合成模板内容驱动各 check 纯函数，正向（合规模板→0 issue）
 * 与负向（违规模板→命中对应规则）对照，证明校验非空洞。
 */
import test from 'node:test';
import assert from 'node:assert/strict';
import {
  checkSecrets,
  checkProbeConsistency,
  checkManagementClosure,
  checkPortExposure,
  checkEnvContract,
  CONTRACT_VARS,
  DEPLOY_TARGETS,
} from './verify-deploy-templates.mjs';

// ---- C1 秘密扫描 ----

test('C1：合规模板（占位符/空值）不报 issue', () => {
  const text = [
    'ZSZJ_DATASOURCE_PASSWORD=${ZSZJ_DATASOURCE_PASSWORD}',
    'ZSZJ_REDIS_PASSWORD=',
    'JAVA_OPTS=-Xms512m -Xmx512m',
  ].join('\n');
  assert.deepEqual(checkSecrets('deploy/.env.example', text), []);
});

test('C1：硬编码口令触发 issue', () => {
  const text = 'MYSQL_ROOT_PASSWORD=123456\nZSZJ_DATASOURCE_PASSWORD=RealP@ssw0rd!';
  const issues = checkSecrets('deploy/.env.example', text);
  assert.ok(issues.length >= 2, `应至少报 2 条，实际 ${issues.length}`);
  assert.ok(issues.every((i) => i.rule === 'C1-secret'));
});

test('C1：PEM 私钥块触发 issue', () => {
  const text = '-----BEGIN RSA PRIVATE KEY-----\nMIIEow...\n-----END RSA PRIVATE KEY-----';
  const issues = checkSecrets('deploy/nginx/tls/server.key', text);
  assert.ok(issues.some((i) => i.rule === 'C1-secret' && /私钥/.test(i.message)));
});

test('C1：长十六进制串（≥32）触发 issue', () => {
  const text = 'VUE_APP_BAIDU_CODE=fadc1bd5db1a1d6f581df60a1807f8ab';
  const issues = checkSecrets('deploy/.env.example', text);
  assert.ok(issues.some((i) => i.rule === 'C1-secret' && /十六进制/.test(i.message)));
});

test('C1：注释行中的秘密仍被检出（防「注释掉即安全」伪绿）', () => {
  const text = '# PASSWORD=SuperSecret123\n# 历史遗留，勿删';
  const issues = checkSecrets('deploy/.env.example', text);
  assert.ok(issues.length > 0, '注释行中的真实秘密也应报出');
});

test('C1：命令行参数硬编码凭据触发 issue（codex r0 P2 修复）', () => {
  const text = 'command: redis-server --requirepass RealSecret123';
  const issues = checkSecrets('deploy/docker-compose.deploy.yml', text);
  assert.ok(issues.some((i) => i.rule === 'C1-secret' && /命令行/.test(i.message)));
});

test('C1：命令行参数使用占位符不报 issue', () => {
  const text = 'command: ["redis-server", "--requirepass", "${ZSZJ_REDIS_PASSWORD}"]';
  assert.deepEqual(checkSecrets('deploy/docker-compose.deploy.yml', text), []);
});

// ---- C2 探针一致性 ----

test('C2：compose healthcheck 路径与 actuator include 一致→通过', () => {
  const compose = 'healthcheck:\n  test:\n    - "curl -f http://localhost:48080/actuator/health || exit 1"';
  assert.deepEqual(checkProbeConsistency(compose, 'health'), []);
});

test('C2：compose 引用 /actuator/health/liveness 但 actuator 未启用 probes→报 issue', () => {
  const compose = 'healthcheck:\n  test:\n    - "curl -f http://localhost:48080/actuator/health/liveness || exit 1"';
  const issues = checkProbeConsistency(compose, 'health');
  assert.ok(issues.some((i) => i.rule === 'C2-probe' && /probes/i.test(i.message)));
});

test('C2：compose 无 healthcheck 定义→报 issue（部署模板必须含探针）', () => {
  const compose = 'services:\n  server:\n    image: zszj-server\n    ports:\n      - "48080:48080"';
  const issues = checkProbeConsistency(compose, 'health');
  assert.ok(issues.some((i) => i.rule === 'C2-probe' && /healthcheck/i.test(i.message)));
});

test('C2：actuator include 为空时 compose 任何 /actuator 探针都报 issue', () => {
  const compose = 'healthcheck:\n  test:\n    - "curl -f http://localhost:48080/actuator/health || exit 1"';
  const issues = checkProbeConsistency(compose, '');
  assert.ok(issues.some((i) => i.rule === 'C2-probe'));
});

test('C2：zszj-server 无专属 healthcheck→报 issue（codex r0 P2 修复）', () => {
  // 其他服务有 healthcheck 但 zszj-server 没有
  const compose = [
    'services:',
    '  postgres:',
    '    healthcheck:',
    '      test: pg_isready',
    '  zszj-server:',
    '    image: zszj-server:latest',
    '    ports:',
    '      - "48080:48080"',
  ].join('\n');
  const issues = checkProbeConsistency(compose, 'health');
  assert.ok(issues.some((i) => i.rule === 'C2-probe' && /zszj-server/.test(i.message)));
});

test('C2：zszj-server healthcheck 未引用 /actuator→报 issue', () => {
  const compose = [
    'services:',
    '  zszj-server:',
    '    image: zszj-server:latest',
    '    healthcheck:',
    '      test: curl http://localhost:48080/ping',
  ].join('\n');
  const issues = checkProbeConsistency(compose, 'health');
  assert.ok(issues.some((i) => i.rule === 'C2-probe' && /actuator/.test(i.message)));
});

// ---- C3 管理路径关闭 ----

test('C3：nginx deny /actuator/ + /admin/ →通过', () => {
  const nginx = [
    'location /actuator/ { deny all; return 403; }',
    'location /admin/ { deny all; return 403; }',
    'location / { proxy_pass http://zszj-server:48080; }',
  ].join('\n');
  assert.deepEqual(checkManagementClosure(nginx), []);
});

test('C3：nginx 未关闭 /actuator/ →报 issue', () => {
  const nginx = 'location / {\n    proxy_pass http://zszj-server:48080;\n  }';
  const issues = checkManagementClosure(nginx);
  assert.ok(issues.some((i) => i.rule === 'C3-mgmt-closure' && /actuator/.test(i.message)));
});

test('C3：nginx 未关闭 /admin/ →报 issue', () => {
  const nginx = 'location /actuator/ {\n    deny all;\n  }\nlocation / {\n    proxy_pass http://backend;\n  }';
  const issues = checkManagementClosure(nginx);
  assert.ok(issues.some((i) => i.rule === 'C3-mgmt-closure' && /admin/.test(i.message)));
});

test('C3：注释掉的 deny 不算关闭→报 issue（codex r0 P2 修复）', () => {
  const nginx = [
    '# location /actuator/ { deny all; return 403; }',
    '# location /admin/ { deny all; return 403; }',
    'location / { proxy_pass http://zszj-server:48080; }',
  ].join('\n');
  const issues = checkManagementClosure(nginx);
  assert.ok(issues.length >= 2, '注释行不应被视为有效关闭');
  assert.ok(issues.some((i) => /注释行无效/.test(i.message)));
});

// ---- C4 端口暴露 ----

test('C4：compose 不暴露 DB/Redis 端口→通过', () => {
  const compose = [
    'services:',
    '  postgres:',
    '    image: postgres:16',
    '  redis:',
    '    image: redis:7-alpine',
    '  server:',
    '    ports:',
    '      - "127.0.0.1:48080:48080"',
  ].join('\n');
  assert.deepEqual(checkPortExposure(compose), []);
});

test('C4：compose 暴露 5432 到 0.0.0.0→报 issue', () => {
  const compose = 'services:\n  postgres:\n    ports:\n      - "5432:5432"';
  const issues = checkPortExposure(compose);
  assert.ok(issues.some((i) => i.rule === 'C4-port' && /5432/.test(i.message)));
});

test('C4：compose 暴露 6379→报 issue', () => {
  const compose = 'services:\n  redis:\n    ports:\n      - "6379:6379"';
  const issues = checkPortExposure(compose);
  assert.ok(issues.some((i) => i.rule === 'C4-port' && /6379/.test(i.message)));
});

test('C4：绑定 127.0.0.1 的 DB 端口不报（本地调试合法）', () => {
  const compose = 'services:\n  postgres:\n    ports:\n      - "127.0.0.1:5432:5432"';
  assert.deepEqual(checkPortExposure(compose), []);
});

test('C4：端口重映射 15432:5432 仍检出容器端口→报 issue（codex r0 P2 修复）', () => {
  const compose = 'services:\n  postgres:\n    ports:\n      - "15432:5432"';
  const issues = checkPortExposure(compose);
  assert.ok(issues.some((i) => i.rule === 'C4-port' && /5432/.test(i.message)));
});

// ---- C5 环境变量合同双向一致 ----

test('C5：.env.example 与 compose 引用的变量完全覆盖合同→通过', () => {
  const envExample = CONTRACT_VARS.map((v) => `${v}=`).join('\n');
  const compose = CONTRACT_VARS.map((v) => `      - \${${v}}`).join('\n');
  assert.deepEqual(checkEnvContract(envExample, compose, CONTRACT_VARS), []);
});

test('C5：合同变量在 .env.example 中缺失→报 issue', () => {
  const envExample = 'ZSZJ_SERVER_PORT=\n';
  const compose = '';
  const issues = checkEnvContract(envExample, compose, CONTRACT_VARS);
  assert.ok(issues.some((i) => i.rule === 'C5-env-contract' && /ZSZJ_DATASOURCE_URL/.test(i.message)));
});

test('C5：compose 引用了合同外变量→报 issue（防幽灵变量）', () => {
  const envExample = CONTRACT_VARS.map((v) => `${v}=`).join('\n');
  const compose = envExample + '\n      - ${GHOST_VAR_NOT_IN_CONTRACT}';
  const issues = checkEnvContract(envExample, compose, CONTRACT_VARS);
  assert.ok(issues.some((i) => i.rule === 'C5-env-contract' && /GHOST_VAR/.test(i.message)));
});

test('C5：CONTRACT_VARS 包含 ZS-ENG-003 已定义的全部必填项', () => {
  const required = ['ZSZJ_DATASOURCE_URL', 'ZSZJ_DATASOURCE_USERNAME', 'ZSZJ_DATASOURCE_PASSWORD', 'ZSZJ_REDIS_HOST', 'ZSZJ_REDIS_PASSWORD'];
  for (const v of required) assert.ok(CONTRACT_VARS.includes(v), `合同缺 ${v}`);
});

test('C5：CONTRACT_VARS 包含 PG bootstrap 凭据（codex r0 P1 修复）', () => {
  assert.ok(CONTRACT_VARS.includes('ZSZJ_PG_BOOTSTRAP_USERNAME'));
  assert.ok(CONTRACT_VARS.includes('ZSZJ_PG_BOOTSTRAP_PASSWORD'));
});

// ---- 结构完整性 ----

test('DEPLOY_TARGETS 非空且路径均为 deploy/ 前缀', () => {
  assert.ok(DEPLOY_TARGETS.length > 0);
  for (const t of DEPLOY_TARGETS) {
    assert.ok(t.startsWith('services/zhongshu-core/deploy/'), `路径应以 deploy/ 为根: ${t}`);
  }
});
