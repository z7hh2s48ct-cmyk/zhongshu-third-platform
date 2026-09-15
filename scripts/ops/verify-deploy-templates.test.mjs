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

test('C1：YAML exec-list 硬编码凭据（--requirepass 引号逗号分隔）触发 issue（codex r1 P2 修复）', () => {
  const text = 'command: ["redis-server", "--requirepass", "RealSecret123"]';
  const issues = checkSecrets('deploy/docker-compose.deploy.yml', text);
  assert.ok(issues.some((i) => i.rule === 'C1-secret' && /命令行/.test(i.message)));
});

test('C1：healthcheck exec-list redis-cli -a 硬编码凭据触发 issue（codex r1 P2 修复）', () => {
  const text = 'test: ["CMD", "redis-cli", "-a", "RealSecret123", "ping"]';
  const issues = checkSecrets('deploy/docker-compose.deploy.yml', text);
  assert.ok(issues.some((i) => i.rule === 'C1-secret' && /命令行/.test(i.message)));
});

test('C1：SQL PASSWORD 字面量触发 issue（codex r2 P2 修复）', () => {
  const text = "CREATE ROLE zhongshu_app LOGIN PASSWORD 'RealSecret123';";
  const issues = checkSecrets('deploy/postgres-init/01-create-app-role.sql', text);
  assert.ok(issues.some((i) => i.rule === 'C1-secret' && /SQL/.test(i.message)));
});

test('C1：psql \\set 口令字面量触发 issue（codex r2 P2 修复）', () => {
  const text = "\\set app_pass 'RealSecret123'";
  const issues = checkSecrets('deploy/postgres-init/01-create-app-role.sql', text);
  assert.ok(issues.some((i) => i.rule === 'C1-secret' && /SQL|psql/.test(i.message)));
});

test('C1：SQL format(%L) 占位符与 psql 变量引用不误报（codex r2 P2 修复）', () => {
  const text = [
    "SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'app_user', :'app_pass')",
    '\\set app_pass `echo "$ZSZJ_DATASOURCE_PASSWORD"`',
  ].join('\n');
  assert.deepEqual(checkSecrets('deploy/postgres-init/01-create-app-role.sql', text), []);
});

test('C1：SQL 字符串拼接 quote_literal(:变量) 不误报为硬编码口令（codex r3 P2 修复）', () => {
  // PASSWORD 后的引号是字符串闭合引号（前面有奇数个引号），非字面量起始
  const text = "SELECT 'CREATE ROLE app LOGIN PASSWORD ' || quote_literal(:'app_pass') \\gexec";
  assert.deepEqual(checkSecrets('deploy/postgres-init/01-create-app-role.sql', text), []);
});

test('C1：块注释内撇号不干扰——PASSWORD 字面量仍检出（codex r4 P2 修复）', () => {
  // 注释 application's 的撇号不得让真口令起始引号被误判为闭合（需按 SQL 词法上下文去注释）
  const text = "/* application's role */ CREATE ROLE app LOGIN PASSWORD 'RealSecret123';";
  const issues = checkSecrets('deploy/postgres-init/01-create-app-role.sql', text);
  assert.ok(issues.some((i) => i.rule === 'C1-secret' && /SQL/.test(i.message)));
});

test('C1：注释掉的 SQL 硬编码凭据仍检出（codex r5 P2-1：注释不消除版本库秘密）', () => {
  // 去注释后匹配会漏检——注释掉的凭据仍留在版本库模板中，必须检出
  const text = "-- CREATE ROLE app LOGIN PASSWORD 'RealSecret123';";
  const issues = checkSecrets('deploy/postgres-init/01-create-app-role.sql', text);
  assert.ok(issues.some((i) => i.rule === 'C1-secret' && /SQL/.test(i.message)));
});

test('C1：双引号标识符内 -- 不当注释——PASSWORD 字面量仍检出（codex r5 P2-2）', () => {
  // "app--readonly" 内的 -- 属标识符内容，不得截断，后续真口令须检出
  const text = 'CREATE ROLE "app--readonly" LOGIN PASSWORD \'RealSecret123\';';
  const issues = checkSecrets('deploy/postgres-init/01-create-app-role.sql', text);
  assert.ok(issues.some((i) => i.rule === 'C1-secret' && /SQL/.test(i.message)));
});

test('C1：美元引用 $$/*$$ 内 /* 不当块注释——后续 PASSWORD 仍检出（codex r5 P2-2）', () => {
  // $$ ... $$ 内的 /* 是字符串内容而非块注释起始，不得吞掉后续语句
  const text = "SELECT $$/*$$; CREATE ROLE x LOGIN PASSWORD 'RealSecret123';";
  const issues = checkSecrets('deploy/postgres-init/01-create-app-role.sql', text);
  assert.ok(issues.some((i) => i.rule === 'C1-secret' && /SQL/.test(i.message)));
});

test('C1：跨行字符串含 /* 不破坏状态——后续 PASSWORD 仍检出（codex r5 P2-2）', () => {
  // 词法状态须跨行保持：第一行开启的字符串未在行尾重置，第二行 PASSWORD 在 NORMAL 上下文
  const text = "SELECT '/*\nstill string'; CREATE ROLE x LOGIN PASSWORD 'RealSecret123';";
  const issues = checkSecrets('deploy/postgres-init/01-create-app-role.sql', text);
  assert.ok(issues.some((i) => i.rule === 'C1-secret' && /SQL/.test(i.message)));
});

test('C1：compose 内嵌 psql -c 的 PASSWORD 字面量仍检出（codex r5 P2-3）', () => {
  // 非 .sql 文件也须做 SQL 凭据检测：compose command 里内嵌的 SQL 不得因扩展名 gate 漏检
  const text = 'command: ["psql", "-c", "CREATE ROLE app LOGIN PASSWORD \'RealSecret123\';"]';
  const issues = checkSecrets('deploy/docker-compose.deploy.yml', text);
  assert.ok(issues.some((i) => i.rule === 'C1-secret' && /SQL/.test(i.message)));
});

// ---- C1 codex r6 回归（候选独立解析 + YAML 隔离 + 词法补全） ----

test('C1：同行安全拼接不消费后续真口令候选（codex r6 P2-1）', () => {
  // 首个候选的拼接闭合引号不得跨过并吞掉第二个 PASSWORD 候选（matchAll 非重叠陷阱）
  const text = "SELECT 'CREATE ROLE app LOGIN PASSWORD ' || quote_literal(current_user); CREATE ROLE x LOGIN PASSWORD 'RealSecret123';";
  const issues = checkSecrets('deploy/postgres-init/01-create-app-role.sql', text);
  assert.ok(issues.some((i) => i.rule === 'C1-secret' && /RealSecret123/.test(i.message)));
});

test('C1：YAML 注释撇号不跨行污染——DQ 内嵌 SQL 真口令检出（codex r6 P2-2）', () => {
  const text = "# application's init\ncommand: [\"psql\", \"-c\", \"CREATE ROLE app LOGIN PASSWORD 'RealSecret123';\"]";
  const issues = checkSecrets('deploy/docker-compose.deploy.yml', text);
  assert.ok(issues.some((i) => i.rule === 'C1-secret' && /RealSecret123/.test(i.message)));
});

test('C1：YAML 单引号标量双撇号转义解码后检出真口令（codex r6 P2-2）', () => {
  // YAML SQ 标量内 '' 转义为一个单引号字符（解码前直接按 SQL 看会漏检）
  const text = "command: ['psql', '-c', 'CREATE ROLE app LOGIN PASSWORD ''RealSecret123'';']";
  const issues = checkSecrets('deploy/docker-compose.deploy.yml', text);
  assert.ok(issues.some((i) => i.rule === 'C1-secret' && /RealSecret123/.test(i.message)));
});

test('C1：注释内安全拼接不误报（codex r6 P2-3）', () => {
  // 注释内被注释掉的拼接表达式：闭合引号角色应正确识别，不得报硬编码字面量
  const text = "-- SELECT 'CREATE ROLE app LOGIN PASSWORD ' || quote_literal(:'app_pass') \\gexec";
  assert.deepEqual(checkSecrets('deploy/postgres-init/01-create-app-role.sql', text), []);
});

test('C1：YAML DQ 内安全拼接不误报（codex r6 P2-3）', () => {
  const text = '[\"psql\", \"-c\", \"SELECT \'CREATE ROLE app LOGIN PASSWORD \' || quote_literal(:\'app_pass\') \\gexec\"]';
  assert.deepEqual(checkSecrets('deploy/docker-compose.deploy.yml', text), []);
});

test('C1：quote_literal 硬编码参数检出、变量参数不报（codex r6 P2-4）', () => {
  const bad = checkSecrets('deploy/postgres-init/01-create-app-role.sql', "SELECT 'CREATE ROLE app LOGIN PASSWORD ' || quote_literal('RealSecret123') \\gexec");
  assert.ok(bad.some((i) => i.rule === 'C1-secret' && /RealSecret123/.test(i.message)));
  const ok = checkSecrets('deploy/postgres-init/01-create-app-role.sql', "SELECT 'CREATE ROLE app LOGIN PASSWORD ' || quote_literal(:'app_pass') \\gexec");
  assert.deepEqual(ok, []);
});

test('C1：E 串反斜杠转义不吞后续口令（codex r6 P2-5）', () => {
  // E'application\'s' 的转义引号不得被当成闭引号，使后续真口令起始引号被误判为开串
  const text = "SELECT E'application\\'s'; CREATE ROLE app LOGIN PASSWORD 'RealSecret123';";
  const issues = checkSecrets('deploy/postgres-init/01-create-app-role.sql', text);
  assert.ok(issues.some((i) => i.rule === 'C1-secret' && /RealSecret123/.test(i.message)));
});

test('C1：非 ASCII 美元 tag 识别；标识符内 $tag$ 不开启美元引用（codex r6 P2-6）', () => {
  const nonAscii = "SELECT $标签$application's$标签$; CREATE ROLE app LOGIN PASSWORD 'RealSecret123';";
  const issues = checkSecrets('deploy/postgres-init/01-create-app-role.sql', nonAscii);
  assert.ok(issues.some((i) => i.rule === 'C1-secret' && /RealSecret123/.test(i.message)));
  const ident = "CREATE TABLE foo$tag$(id int); SELECT 'CREATE ROLE app LOGIN PASSWORD ' || quote_literal(:'app_pass') \\gexec";
  assert.deepEqual(checkSecrets('deploy/postgres-init/01-create-app-role.sql', ident), []);
});

// ---- C1 codex r7 回归（YAML 标量边界 / 转义解码 / 撇号配对 / 语句级取值解析） ----

test('C1：plain 流标量内 SQL 引号不误拆，口令检出（codex r7 P2-1）', () => {
  // 非引号起点的流式标量中，SQL 单引号属普通文本，不按 YAML 引号标量拆分
  const text = "command: [psql, -c, CREATE ROLE app LOGIN PASSWORD 'RealSecret123';]";
  const issues = checkSecrets('deploy/docker-compose.deploy.yml', text);
  assert.ok(issues.some((i) => i.rule === 'C1-secret' && /RealSecret123/.test(i.message)));
});

test('C1：| 块标量内容行口令检出（codex r7 P2-1）', () => {
  const text = "init: |\n  CREATE ROLE app LOGIN PASSWORD 'RealSecret123';";
  const issues = checkSecrets('deploy/docker-compose.deploy.yml', text);
  assert.ok(issues.some((i) => i.rule === 'C1-secret' && /RealSecret123/.test(i.message)));
});

test('C1：\\x27 十六进制转义解码后检出（codex r7 P2-2）', () => {
  const text = 'command: ["psql", "-c", "CREATE ROLE app LOGIN PASSWORD \\x27RealSecret123\\x27;"]';
  const issues = checkSecrets('deploy/docker-compose.deploy.yml', text);
  assert.ok(issues.some((i) => i.rule === 'C1-secret' && /RealSecret123/.test(i.message)));
});

test('C1：\\u0027 转义解码后检出（codex r7 P2-2）', () => {
  const text = 'command: ["psql", "-c", "CREATE ROLE app LOGIN PASSWORD \\u0027RealSecret123\\u0027;"]';
  const issues = checkSecrets('deploy/docker-compose.deploy.yml', text);
  assert.ok(issues.some((i) => i.rule === 'C1-secret' && /RealSecret123/.test(i.message)));
});

test('C1：行注释撇号不占用引号配对，口令检出（codex r7 P2-3）', () => {
  // application's 的撇号不得让同行 PASSWORD 起始引号被误判为配对
  const text = "-- application's old credential: CREATE ROLE app LOGIN PASSWORD 'RealSecret123';";
  const issues = checkSecrets('deploy/postgres-init/01-create-app-role.sql', text);
  assert.ok(issues.some((i) => i.rule === 'C1-secret' && /RealSecret123/.test(i.message)));
});

test('C1：空串豁免后继续窗口遍历，检出真口令（codex r7 P2-4）', () => {
  // quote_literal('' || 'RealSecret123')：空串是安全值，须继续遍历拼接表达式其余字面量
  const text = "SELECT 'CREATE ROLE app LOGIN PASSWORD ' || quote_literal('' || 'RealSecret123') \\gexec";
  const issues = checkSecrets('deploy/postgres-init/01-create-app-role.sql', text);
  assert.ok(issues.some((i) => i.rule === 'C1-secret' && /RealSecret123/.test(i.message)));
});

test('C1：拼接窗口跨行继续，检出真口令（codex r7 P2-5）', () => {
  const text = "SELECT 'CREATE ROLE app LOGIN PASSWORD ' ||\nquote_literal('RealSecret123')\n\\gexec";
  const issues = checkSecrets('deploy/postgres-init/01-create-app-role.sql', text);
  assert.ok(issues.some((i) => i.rule === 'C1-secret' && /RealSecret123/.test(i.message)));
});

test('C1：注释内分号不截断窗口，检出真口令（codex r7 P2-5）', () => {
  const text = "SELECT 'CREATE ROLE app LOGIN PASSWORD ' || /* ; */ quote_literal('RealSecret123') \\gexec";
  const issues = checkSecrets('deploy/postgres-init/01-create-app-role.sql', text);
  assert.ok(issues.some((i) => i.rule === 'C1-secret' && /RealSecret123/.test(i.message)));
});

test('C1：PASSWORD NULL 不误取后续语句字面量（codex r7 P2-6）', () => {
  const text = "CREATE ROLE app LOGIN PASSWORD NULL; SELECT 'hello';";
  assert.deepEqual(checkSecrets('deploy/postgres-init/01-create-app-role.sql', text), []);
});

test('C1：关键字与取值间注释不干扰，口令检出（codex r7 P2-6）', () => {
  const text = "CREATE ROLE app LOGIN PASSWORD /* '' */ 'RealSecret123';";
  const issues = checkSecrets('deploy/postgres-init/01-create-app-role.sql', text);
  assert.ok(issues.some((i) => i.rule === 'C1-secret' && /RealSecret123/.test(i.message)));
});

test('C1：format %L 参数为变量不误报角色名参数（codex r7 P2-7）', () => {
  // %I 对应 'app'（角色名）、%L 对应 :'app_pass'（变量）→ 无硬编码口令
  const text = "SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', 'app', :'app_pass')";
  assert.deepEqual(checkSecrets('deploy/postgres-init/01-create-app-role.sql', text), []);
});

test('C1：format %L 参数为硬编码口令仍检出（codex r7 P2-7）', () => {
  const text = "SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', 'app', 'RealSecret123')";
  const issues = checkSecrets('deploy/postgres-init/01-create-app-role.sql', text);
  assert.ok(issues.some((i) => i.rule === 'C1-secret' && /RealSecret123/.test(i.message)));
});

test('C1：占位符前缀硬编码后缀仍检出（codex r7 P2-8）', () => {
  // 仅「完整 ${VAR}」豁免；前缀拼接真实口令后缀仍是硬编码秘密
  const text = "CREATE ROLE app LOGIN PASSWORD '${VAR}RealSecret123';";
  const issues = checkSecrets('deploy/postgres-init/01-create-app-role.sql', text);
  assert.ok(issues.some((i) => i.rule === 'C1-secret' && /RealSecret123/.test(i.message)));
});

test('C1：完整占位符不误报（codex r7 P2-8）', () => {
  const text = "CREATE ROLE app LOGIN PASSWORD '${ZSZJ_DATASOURCE_PASSWORD}';";
  assert.deepEqual(checkSecrets('deploy/postgres-init/01-create-app-role.sql', text), []);
});

test('C1：\\set 反引号命令替换止于行边界，不误报后续 format 串（codex r7.1 真实模板）', () => {
  // 真实模板 01-create-app-role.sql 结构：\set 取值来自命令替换，后续 format(%L) 为变量传递
  const text = '\\set app_pass `echo "$ZSZJ_DATASOURCE_PASSWORD"`\n\n-- create role\nSELECT format(\'CREATE ROLE %I LOGIN PASSWORD %L\', :\'app_user\', :\'app_pass\')\n\\gexec';
  assert.deepEqual(checkSecrets('deploy/postgres-init/01-create-app-role.sql', text), []);
});

test('C1：\\set 行尾反斜杠续行后字面量仍检出（codex r7.1）', () => {
  const text = "\\set app_pass \\\n'RealSecret123'";
  const issues = checkSecrets('deploy/postgres-init/01-create-app-role.sql', text);
  assert.ok(issues.some((i) => i.rule === 'C1-secret' && /RealSecret123/.test(i.message)));
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

test('C2：zszj-server 块内空行不误判缺 healthcheck（codex r1 P2 修复）', () => {
  // YAML 映射中空行不终止服务块；healthcheck 前有空行仍应被识别
  const compose = [
    'services:',
    '  zszj-server:',
    '    image: zszj-server:latest',
    '',
    '    healthcheck:',
    '      test: ["CMD","curl","-f","http://localhost:48080/actuator/health"]',
  ].join('\n');
  assert.deepEqual(checkProbeConsistency(compose, 'health'), []);
});

test('C2：带行内注释服务头 zszj-server: # backend 缺 healthcheck 仍检出（codex r2 P2 修复）', () => {
  const compose = [
    'services:',
    '  postgres:',
    '    healthcheck:',
    '      test: pg_isready',
    '  zszj-server: # backend',
    '    image: zszj-server:latest',
  ].join('\n');
  const issues = checkProbeConsistency(compose, 'health');
  assert.ok(issues.some((i) => i.rule === 'C2-probe' && /zszj-server/.test(i.message)));
});

test('C2：YAML anchor 服务头 zszj-server: &backend 缺 healthcheck 仍检出（codex r2 P2 修复）', () => {
  const compose = [
    'services:',
    '  postgres:',
    '    healthcheck:',
    '      test: pg_isready',
    '  zszj-server: &backend',
    '    image: zszj-server:latest',
  ].join('\n');
  const issues = checkProbeConsistency(compose, 'health');
  assert.ok(issues.some((i) => i.rule === 'C2-probe' && /zszj-server/.test(i.message)));
});

test('C2：四空格缩进服务头缺 healthcheck 仍检出（codex r2 P2 修复）', () => {
  const compose = [
    'services:',
    '    postgres:',
    '        healthcheck:',
    '            test: pg_isready',
    '    zszj-server:',
    '        image: zszj-server:latest',
  ].join('\n');
  const issues = checkProbeConsistency(compose, 'health');
  assert.ok(issues.some((i) => i.rule === 'C2-probe' && /zszj-server/.test(i.message)));
});

test('C2：带注释服务头且有 /actuator healthcheck 不误报（codex r2 P2 修复）', () => {
  const compose = [
    'services:',
    '  zszj-server: # backend',
    '    image: zszj-server:latest',
    '    healthcheck:',
    '      test: ["CMD","curl","-f","http://localhost:48080/actuator/health"]',
  ].join('\n');
  assert.deepEqual(checkProbeConsistency(compose, 'health'), []);
});

test('C2：nginx.depends_on.zszj-server 不被误当作服务头（codex r3 P2 修复）', () => {
  // nginx 服务排在 zszj-server 之前；depends_on 下的深层同名键不得被当成服务头
  const compose = [
    'services:',
    '  nginx:',
    '    depends_on:',
    '      zszj-server:',
    '        condition: service_healthy',
    '  zszj-server:',
    '    image: zszj-server:latest',
    '    healthcheck:',
    '      test: ["CMD","curl","-f","http://localhost:48080/actuator/health"]',
  ].join('\n');
  assert.deepEqual(checkProbeConsistency(compose, 'health'), []);
});

test('C2：healthcheck 前的低缩进注释行不截断服务块（codex r3 P2 修复）', () => {
  // YAML 注释缩进可低于周围映射而不结束该映射；块提取应忽略注释行
  const compose = [
    'services:',
    '  zszj-server:',
    '    image: zszj-server:latest',
    '  # probe configuration',
    '    healthcheck:',
    '      test: ["CMD","curl","-f","http://localhost:48080/actuator/health"]',
  ].join('\n');
  assert.deepEqual(checkProbeConsistency(compose, 'health'), []);
});

test('C2：probe-less zszj-server 后的同缩进注释不伪造探针证据（codex r4 P2 修复）', () => {
  // 注释含 healthcheck 与 /actuator/ 字样，不得被当作有效探针证据（跳过注释不纳入块）
  const compose = [
    'services:',
    '  zszj-server:',
    '    image: zszj-server:latest',
    '  # TODO: add healthcheck at /actuator/health',
  ].join('\n');
  const issues = checkProbeConsistency(compose, 'health');
  assert.ok(issues.some((i) => i.rule === 'C2-probe' && /zszj-server/.test(i.message)));
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
