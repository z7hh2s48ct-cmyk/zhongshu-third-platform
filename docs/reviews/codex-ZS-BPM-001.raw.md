OpenAI Codex v0.154.0
--------
workdir: E:\zszj-wt-bpm-001-a
model: gpt-6-astra
provider: openai
approval: never
sandbox: read-only
reasoning effort: xhigh
reasoning summaries: none
session id: 01a09c30-7874-7d92-bd9b-7a9abc23ba06
--------
user
你是资深后端与测试基建评审员。评审当前分支 feat/bpm-001-a 相对 main 的全部改动（ZS-BPM-001：BPM 独立装配与 PostgreSQL 验收，B09 批次「技术准备可先行」件）。用 git diff main 查看全部改动。

改动范围：
① 新增 zszj-module-bpm 测试夹具（src/test 下 harness 包）：BpmPgHarnessConfiguration 手工装配 SpringProcessEngineConfiguration+DataSourceTransactionManager+HikariPG，两阶段测试 BpmPgHarnessBootstrapTest（owner 账号建表/部署幂等/版本钉住/发起/租户标签/留积压）与 BpmPgHarnessRuntimeTest（低权限 app 重启恢复/通过/拒绝/撤回/转办/分页历史/事务回滚/执行器显式启停）、NeutralEchoDelegate、中性 BPMN 夹具×3；
② 新增 scripts/db/run-bpm001-verify.mjs 两阶段编排器（一次性 Docker PG 容器、结构检查 S0~S3、surefire 解析、失败非零、缺 Docker 退出码 3），并注册为 scripts/db/run-pg-regression.mjs 第 9 套件；
③ zszj-module-bpm/pom.xml 补 test 作用域 postgresql 驱动；
④ 新增决策文档 services/zhongshu-core/docs/BPM引擎表与业务扩展表迁移责任决策.md。

评审重点：
1) 测试装配正确性与安全性：环境变量缺失/错误是否快速失败而非伪报；低权限账号语义是否真实（schema-update=false + app 无 DDL）；断言是否可能空洞或恒真。
2) 卡片验收覆盖度：PG 部署/发起/通过/拒绝/撤回/转办/分页历史、重复部署、重启恢复、事务失败恢复、关闭 BPM 无副作用、异步执行器与启停——有无漏验或弱断言。
3) D-07 §6.5 合规：夹具是否固化任何众墅业务字段、业务状态、不可逆约束（必须保持中性技术夹具）。
4) 编排器健壮性：容器/端口泄漏路径、Windows cmd.exe 包 mvn 的正确性、退出码语义、flaky 重试约定、surefire XML 解析是否可能把未跑过判成通过。
5) 对既有门禁的影响：ModuleWhitelistTest（根 POM 不解注释的前提是否守住）、run-pg-regression 其余 8 套件、docs 规则（文档链接、状态附日期依据）。

已实测事实（供校验，不要盲信）：单套件 6/6 全绿 exit 0；聚合 9 套件 bpm001 即绿、db006/db008 首轮 Docker 竞态失败后单独复跑全绿（既有 flaky 约定）；G5 在 worktree 因 .omx 未入库环境性失败、主树绿。

输出格式：逐条发现，每条标 P0（不修不得合并）/P1/P2，附 文件:行号 与具体修复建议；无发现的方面明确写「未发现」；最后一行给总结论：PASS（可合并）或 NEEDS_FIX。

codex
我会先用 `git diff main` 核对全部改动，再检查测试断言、两阶段权限隔离、编排器失败路径和文档门禁；实测结果会结合代码交叉验证。

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "git status --short; git branch --show-current; git diff main --stat; rg --files -g AGENTS.md -g '*D-07*' -g '*BPM*' -g '*bpm001*' -g '*pg-regression*' -g '*ModuleWhitelistTest*' -g '*BpmPgHarness*'" in E:\zszj-wt-bpm-001-a
 succeeded in 89ms:
warning: unable to access 'C:\Users\Administrator/.config/git/ignore': Permission denied
 README.md                                          |   4 +-
 ...273\273\345\212\241\346\270\205\345\215\225.md" |   9 +-
 docs/reviews/README.md                             |  10 +-
 docs/reviews/codex-ZS-DB-018.md                    |  19 --
 docs/reviews/codex-ZS-FILE-001.A.md                |  35 ---
 docs/reviews/codex-ZS-PERM-001.A.md                |  20 --
 docs/reviews/codex-ZS-PERM-002.A.md                |  20 --
 docs/reviews/codex-ZS-SEC-001.A.md                 |  21 --
 docs/reviews/codex-ZS-SEC-002.md                   |  24 --
 docs/reviews/codex-ZS-SEC-005.md                   |  21 --
 docs/reviews/codex-ZS-SEC-007.md                   |  20 --
 docs/reviews/codex-ZS-SEC-012.A.md                 |  20 --
 docs/reviews/codex-hotfix-SEC-007-P1.md            |  39 ----
 scripts/db/run-bpm001-verify.mjs                   | 215 ++++++++++++++++++
 scripts/db/run-pg-regression.mjs                   |   2 +
 ...264\243\344\273\273\345\206\263\347\255\226.md" |  62 +++++
 .../common/util/log/LogSanitizeUtils.java          |  63 +-----
 .../common/util/log/LogSanitizeUtilsTest.java      |  62 -----
 services/zhongshu-core/zszj-module-bpm/pom.xml     |   7 +
 .../cn/zszj/module/bpm/harness/BpmPgHarness.java   |  94 ++++++++
 .../bpm/harness/BpmPgHarnessBootstrapTest.java     | 173 ++++++++++++++
 .../bpm/harness/BpmPgHarnessConfiguration.java     | 155 +++++++++++++
 .../bpm/harness/BpmPgHarnessRuntimeTest.java       | 240 ++++++++++++++++++++
 .../module/bpm/harness/NeutralEchoDelegate.java    |  37 +++
 .../bpm/harness/neutral-approval-v2.bpmn20.xml     |  23 ++
 .../module/bpm/harness/neutral-approval.bpmn20.xml |  22 ++
 .../bpm/harness/neutral-async-echo.bpmn20.xml      |  16 ++
 .../controller/admin/file/FileController.java      |  34 +--
 .../controller/admin/file/vo/file/FileRespVO.java  |   9 +-
 .../controller/app/file/AppFileController.java     |   8 +-
 .../module/infra/dal/dataobject/file/FileDO.java   |  18 +-
 .../module/infra/enums/ErrorCodeConstants.java     |   2 -
 .../module/infra/enums/file/FileScopeEnum.java     |  33 ---
 .../module/infra/service/file/FileService.java     |  17 +-
 .../module/infra/service/file/FileServiceImpl.java | 126 +----------
 .../service/db/DatabaseTableServiceImplTest.java   |   4 +-
 .../service/file/FileServiceAuthorizationTest.java | 249 ---------------------
 .../infra/service/file/FileServiceImplTest.java    |   2 -
 .../src/test/resources/sql/create_tables.sql       |   2 -
 .../src/main/resources/application.yaml            |   3 -
 .../V20260914.001__infra_file_tenancy.sql          |   7 -
 .../src/test/resources/api-inventory-baseline.txt  |   5 +-
 42 files changed, 1089 insertions(+), 863 deletions(-)
services\zhongshu-core\zszj-server\src\test\java\cn\zszj\server\ModuleWhitelistTest.java
scripts\db\run-pg-regression.mjs
scripts\db\run-bpm001-verify.mjs
services\zhongshu-core\docs\BPM引擎表与业务扩展表迁移责任决策.md
services\zhongshu-core\zszj-module-bpm\src\test\java\cn\zszj\module\bpm\harness\BpmPgHarnessRuntimeTest.java
services\zhongshu-core\zszj-module-bpm\src\test\java\cn\zszj\module\bpm\harness\BpmPgHarnessConfiguration.java
services\zhongshu-core\zszj-module-bpm\src\test\java\cn\zszj\module\bpm\harness\BpmPgHarnessBootstrapTest.java
services\zhongshu-core\zszj-module-bpm\src\test\java\cn\zszj\module\bpm\harness\BpmPgHarness.java

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'git diff main -- scripts/db/run-bpm001-verify.mjs scripts/db/run-pg-regression.mjs services/zhongshu-core/zszj-module-bpm' in E:\zszj-wt-bpm-001-a
 succeeded in 0ms:
warning: unable to access 'C:\Users\Administrator/.config/git/ignore': Permission denied
diff --git a/scripts/db/run-bpm001-verify.mjs b/scripts/db/run-bpm001-verify.mjs
new file mode 100644
index 00000000..f0db1dd8
--- /dev/null
+++ b/scripts/db/run-bpm001-verify.mjs
@@ -0,0 +1,215 @@
+/**
+ * ZS-BPM-001：BPM 独立装配与 PostgreSQL 验收编排器（B09 技术准备，本地与 CI 同一规则）。
+ *
+ * 两阶段语义（同一一次性 PG 容器内的同一物理库）：
+ *   阶段1 bootstrap（zhongshu_owner，schema-update=true，异步执行器挂起）：
+ *     - 空库上由 Flowable 引擎自建 ACT_ 表（建表责任=迁移期 owner 账号）；
+ *     - 部署中性技术夹具（重复部署幂等、变更部署出新版本）、发起运行实例、留下异步积压。
+ *   阶段2 runtime（zhongshu_app 低权限，schema-update=false，异步执行器开启）：
+ *     - 新 JVM 重启接入同库（重启恢复：定义/实例/历史全部可见、schema 版本不变=运行期零 DDL）；
+ *     - 通过/拒绝/撤回/转办/分页历史、Spring 事务回滚不留引擎痕迹、积压任务被消化且无死信。
+ * 结构检查（编排器 psql 直证，不经 Java）：
+ *   S0 关闭 BPM 无副作用：System/Infra 基线（全部 Flyway V* 应用后）零 ACT_/FLW_ 表；
+ *   S1 低权限运行账号无 DDL：zhongshu_app 建表必须失败；
+ *   S2 引擎表数量与 schema.version 前后一致（阶段2 未改结构）。
+ *
+ * 语义（对齐 scripts/db/run-db018-verify.mjs / test-pg-fixture.mjs 夹具约定）：
+ *   - 实例隔离：容器名/端口随机唯一，进程退出自动清理；
+ *   - 失败非零：任一阶段/检查失败 → 非零退出；
+ *   - 缺依赖不静默跳过：Docker 不可用 → 明确报错并以退出码 3 结束；
+ *   - Docker 负载 flaky：容器启动失败重试一次。
+ * 证据报告：JSON 摘要落 outputs/bpm-001/runtime-report.json（outputs/ 不入库）。
+ *
+ * 用法：node scripts/db/run-bpm001-verify.mjs
+ */
+import { execFileSync, spawnSync } from 'node:child_process';
+import { readFileSync, existsSync, mkdirSync, writeFileSync, readdirSync } from 'node:fs';
+import { fileURLToPath } from 'node:url';
+import { join } from 'node:path';
+
+const root = fileURLToPath(new URL('../../', import.meta.url));
+const core = join(root, 'services', 'zhongshu-core');
+const migrationDir = join(core, 'zszj-server', 'src', 'main', 'resources', 'db', 'migration');
+const outDir = join(root, 'outputs', 'bpm-001');
+
+function fail(code, message) { console.error(message); process.exit(code); }
+
+// ---- 工具链与 Docker 预检（缺依赖不静默跳过）----
+const toolsDir = join(root, 'tools');
+const jdkDir = join(toolsDir, 'jdk-17.0.20.1+1');
+const mavenBin = join(toolsDir, 'apache-maven-3.9.9', 'bin');
+if (!existsSync(jdkDir) || !existsSync(mavenBin)) {
+  fail(3, `[bpm001] 工具链缺失（${jdkDir} / ${mavenBin}）：worktree 须供给 tools/`);
+}
+const dockerUp = spawnSync('docker', ['version', '--format', '{{.Server.Version}}'], { encoding: 'utf8' });
+if (dockerUp.error || dockerUp.status !== 0) {
+  fail(3, `[bpm001] Docker 不可用（${dockerUp.error?.message ?? `exit=${dockerUp.status}`}）：验证不得静默跳过`);
+}
+
+const container = `zszj-bpm001-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
+const port = 4332 + Math.floor(Math.random() * 700);
+let cleaned = false;
+const cleanup = () => { if (!cleaned) { cleaned = true; try { execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { } } };
+process.on('exit', cleanup);
+process.on('SIGINT', () => { cleanup(); process.exit(130); });
+
+const psql = (user, db, sql) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', user, '-d', db,
+  '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 });
+const psqlOut = (user, db, sql, host) => spawnSync('docker', ['exec', container, 'psql', ...(host ? ['-h', host] : []), '-U', user, '-d', db, '-At', '-c', sql],
+  { encoding: 'utf8', maxBuffer: 8 * 1024 * 1024 });
+const psqlOk = (user, db, sql) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', user, '-d', db,
+  '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql, encoding: 'utf8' }).status === 0;
+
+let pass = 0, failCount = 0;
+const results = [];
+const record = (id, ok, note = '') => { results.push({ id, ok, note }); ok ? pass++ : failCount++; console.log(`[${ok ? 'PASS' : 'FAIL'}] ${id} ${note}`); };
+
+// ---- 拉起一次性 PG（负载 flaky 重试一次）----
+function dockerRunOnce() {
+  const args = ['run', '-d', '--name', container, '-e', 'POSTGRES_PASSWORD=bpm001', '-p', `127.0.0.1:${port}:5432`, 'postgres:17-alpine'];
+  if (process.platform === 'win32') {
+    return spawnSync('cmd.exe', ['/d', '/s', '/c', 'docker ' + args.join(' ')], { encoding: 'utf8' }).status === 0;
+  }
+  return spawnSync('docker', args, { encoding: 'utf8' }).status === 0;
+}
+console.log(`[bpm001] 拉起临时 PG（${container} @ 127.0.0.1:${port}）…`);
+if (!dockerRunOnce() && !dockerRunOnce()) {
+  fail(3, '[bpm001] PG 夹具容器两次启动失败（Docker 负载竞态按约定重试一次后仍失败）');
+}
+
+let ready = false;
+// 就绪探测必须走 TCP（-h 127.0.0.1）：initdb 期间的临时服务器只监听 unix socket，
+// socket 探测可能误判就绪导致 CREATE DATABASE 落到临时库上失败
+for (let i = 0; i < 40; i++) {
+  if (psqlOut('postgres', 'postgres', 'SELECT 1', '127.0.0.1').status === 0) { ready = true; break; }
+  Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500);
+}
+if (!ready) { cleanup(); fail(1, '[bpm001] PG 未就绪'); }
+
+// ---- 基线库：建库 + 角色授权 + 全部 Flyway 迁移 ----
+{
+  let created = false, last = '';
+  for (let i = 0; i < 10 && !created; i++) {
+    const r = spawnSync('docker', ['exec', container, 'psql', '-h', '127.0.0.1', '-U', 'postgres', '-q', '-c', 'CREATE DATABASE zhongshu;'], { encoding: 'utf8' });
+    created = r.status === 0;
+    if (!created) { last = (r.stderr ?? '') + (r.stdout ?? ''); Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 1000); }
+  }
+  if (!created) { cleanup(); fail(1, `[bpm001] 建库失败（重试 10 次）：${last.slice(0, 400)}`); }
+}
+{
+  const setup = readFileSync(join(core, 'sql', 'postgresql', 'env-setup-test.sql'), 'utf8');
+  const r = psql('postgres', 'zhongshu', setup);
+  if (r.status !== 0) { cleanup(); fail(1, `[bpm001] 角色授权失败:\n${r.stdout}${r.stderr}`); }
+}
+{
+  const migrations = readdirSync(migrationDir).filter((f) => /^V.*\.sql$/.test(f)).sort();
+  const bundled = migrations.map((f) => readFileSync(join(migrationDir, f), 'utf8')).join('\n');
+  const r = psql('zhongshu_owner', 'zhongshu', bundled);
+  if (r.status !== 0) { cleanup(); fail(1, `[bpm001] 基线迁移执行失败（${migrations.length} 个 V*）:\n${r.stdout}${r.stderr}`); }
+  console.log(`[bpm001] 已应用基线迁移 ${migrations.length} 个：${migrations.join(', ')}`);
+}
+if (spawnSync('docker', ['exec', container, 'psql', '-U', 'zhongshu_owner', '-d', 'zhongshu', '-q', '-c',
+  'CREATE TABLE bpm_harness_probe(id bigserial PRIMARY KEY, note text NOT NULL, created_at timestamptz NOT NULL DEFAULT now());'], { encoding: 'utf8' }).status !== 0) {
+  cleanup(); fail(1, '[bpm001] 探针表创建失败');
+}
+
+// ---- Maven 子进程（Windows 下 Node spawn .cmd 有 EINVAL 防护，须经 cmd.exe）----
+const childEnv = {
+  ...process.env,
+  JAVA_HOME: jdkDir,
+  PATH: process.platform === 'win32'
+    ? `${jdkDir}\\bin;${mavenBin};${process.env.PATH}`
+    : `${jdkDir}/bin:${mavenBin}:${process.env.PATH}`,
+};
+function runHarnessTest(testClass, username, password, schemaUpdate, asyncExecutor, timeoutMs) {
+  const args = ['-B', '-f', 'zszj-module-bpm/pom.xml', 'test', `-Dtest=${testClass}`];
+  const cmd = `mvn ${args.join(' ')}`;
+  const r = spawnSync(process.platform === 'win32' ? 'cmd.exe' : 'mvn',
+    process.platform === 'win32' ? ['/d', '/s', '/c', cmd] : args,
+    {
+      cwd: core, env: {
+        ...childEnv,
+        ZSZJ_BPM_HARNESS_JDBC_URL: `jdbc:postgresql://127.0.0.1:${port}/zhongshu`,
+        ZSZJ_BPM_HARNESS_USERNAME: username,
+        ZSZJ_BPM_HARNESS_PASSWORD: password,
+        ZSZJ_BPM_HARNESS_SCHEMA_UPDATE: schemaUpdate,
+        ZSZJ_BPM_HARNESS_ASYNC_EXECUTOR: asyncExecutor,
+      },
+      encoding: 'utf8', maxBuffer: 64 * 1024 * 1024, timeout: timeoutMs,
+    });
+  return { ...r, output: (r.stdout ?? '') + (r.stderr ?? '') };
+}
+function surefireSummary(testClass) {
+  const xml = join(core, 'zszj-module-bpm', 'target', 'surefire-reports',
+    `TEST-cn.zszj.module.bpm.harness.${testClass}.xml`);
+  if (!existsSync(xml)) return null;
+  const text = readFileSync(xml, 'utf8');
+  const attr = (name) => Number((text.match(new RegExp(`${name}="(\\d+)"`)) ?? [])[1] ?? -1);
+  return { tests: attr('tests'), failures: attr('failures'), errors: attr('errors'), skipped: attr('skipped') };
+}
+const countTables = (pattern) => Number((psqlOut('postgres', 'zhongshu',
+  `SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND lower(table_name) LIKE '${pattern}'`).stdout ?? '0').trim()) || 0;
+
+// ---- S0：关闭 BPM 的结构性证明——System/Infra 全基线零流程表 ----
+{
+  const act = countTables('act\\_%'), flw = countTables('flw\\_%');
+  record('S0 关闭BPM无副作用：基线零 ACT_/FLW_ 表', act === 0 && flw === 0, `ACT_=${act} FLW_=${flw}`);
+}
+// ---- S1：低权限运行账号无 DDL ----
+{
+  const r = psqlOk('zhongshu_app', 'zhongshu', 'CREATE TABLE zszj_app_should_fail(id int);');
+  record('S1 运行账号无DDL：zhongshu_app 建表被拒', r === false, r ? 'app 竟然建表成功' : '按预期失败');
+}
+
+// ---- 阶段1 bootstrap（owner 账号：建表 + 部署 + 发起 + 留积压）----
+console.log('[bpm001] 阶段1 bootstrap：owner 账号建引擎表/部署中性夹具/发起实例/留异步积压…');
+mkdirSync(outDir, { recursive: true });
+const bootLog = runHarnessTest('BpmPgHarnessBootstrapTest', 'zhongshu_owner', 'owner_local_1', 'true', 'false', 20 * 60 * 1000);
+writeFileSync(join(outDir, 'bootstrap.log'), bootLog.output);
+const bootSummary = surefireSummary('BpmPgHarnessBootstrapTest');
+const bootOk = bootLog.status === 0 && bootSummary && bootSummary.tests > 0 && bootSummary.failures === 0 && bootSummary.errors === 0;
+record('P1 bootstrap 引擎建表/部署幂等/发起/租户标签/版本钉住', bootOk,
+  bootSummary ? `tests=${bootSummary.tests} failures=${bootSummary.failures} errors=${bootSummary.errors} skipped=${bootSummary.skipped}` : `exit=${bootLog.status}（无 surefire 报告）`);
+if (!bootOk) {
+  console.error('[bpm001] bootstrap 阶段失败，日志见 outputs/bpm-001/bootstrap.log（尾部）：\n' + bootLog.output.split('\n').slice(-40).join('\n'));
+  finish();
+}
+
+const actAfterBootstrap = countTables('act\\_%');
+const schemaVersionAfterBootstrap = (psqlOut('postgres', 'zhongshu', "SELECT VALUE_ FROM ACT_GE_PROPERTY WHERE NAME_='schema.version'").stdout ?? '').trim();
+
+// ---- 阶段2 runtime（app 低权限：重启恢复 + 全操作断言 + 零 DDL；执行器由 R70 显式启停）----
+console.log('[bpm001] 阶段2 runtime：app 低权限重启接入同库/审批操作/事务回滚/积压恢复…');
+const runLog = runHarnessTest('BpmPgHarnessRuntimeTest', 'zhongshu_app', 'app_local_1', 'false', 'false', 15 * 60 * 1000);
+writeFileSync(join(outDir, 'runtime.log'), runLog.output);
+const runSummary = surefireSummary('BpmPgHarnessRuntimeTest');
+const runOk = runLog.status === 0 && runSummary && runSummary.tests > 0 && runSummary.failures === 0 && runSummary.errors === 0;
+record('P2 runtime 重启恢复/通过/拒绝/撤回/转办/分页历史/事务回滚/积压恢复', runOk,
+  runSummary ? `tests=${runSummary.tests} failures=${runSummary.failures} errors=${runSummary.errors} skipped=${runSummary.skipped}` : `exit=${runLog.status}（无 surefire 报告）`);
+if (!runOk) {
+  console.error('[bpm001] runtime 阶段失败，日志见 outputs/bpm-001/runtime.log（尾部）：\n' + runLog.output.split('\n').slice(-40).join('\n'));
+}
+
+// ---- S2：运行期零 DDL（表数量与 schema.version 不变）+ 探针落库 ----
+{
+  const actAfterRuntime = countTables('act\\_%');
+  const schemaVersionAfterRuntime = (psqlOut('postgres', 'zhongshu', "SELECT VALUE_ FROM ACT_GE_PROPERTY WHERE NAME_='schema.version'").stdout ?? '').trim();
+  const probeRows = Number((psqlOut('postgres', 'zhongshu', 'SELECT count(*) FROM bpm_harness_probe').stdout ?? '0').trim()) || 0;
+  record('S2 运行期零DDL：引擎表数量与 schema.version 不变',
+    actAfterBootstrap === actAfterRuntime && schemaVersionAfterBootstrap === schemaVersionAfterRuntime,
+    `ACT_ ${actAfterBootstrap}→${actAfterRuntime}, schema.version ${schemaVersionAfterBootstrap}→${schemaVersionAfterRuntime}`);
+  record('S3 异步回声探针落库（owner 引导版本 + app 运行期回声）', probeRows >= 3, `探针行=${probeRows}`);
+}
+
+function finish() {
+  cleanup();
+  console.log('\n===== ZS-BPM-001 BPM 独立装配与 PG 验收汇总 =====');
+  for (const r of results) console.log(`${r.ok ? 'PASS' : 'FAIL'}  ${r.id} ${r.note}`);
+  console.log(`合计 ${results.length} 项，失败 ${failCount} 项`);
+  try {
+    writeFileSync(join(outDir, 'runtime-report.json'),
+      JSON.stringify({ finishedAt: new Date().toISOString(), container: cleaned ? container : `${container}(运行中)`, results, pass, failCount }, null, 2));
+  } catch { /* 报告落盘失败不影响退出码语义 */ }
+  process.exit(failCount === 0 ? 0 : 1);
+}
+finish();
diff --git a/scripts/db/run-pg-regression.mjs b/scripts/db/run-pg-regression.mjs
index beef414e..5910fefe 100644
--- a/scripts/db/run-pg-regression.mjs
+++ b/scripts/db/run-pg-regression.mjs
@@ -10,6 +10,7 @@
  *   6. ZS-DB-018 ORM/手写 SQL/租户隔离（两技术租户 CRUD/分页/关联/批量/逻辑删除 + 全局表/忽略注解/系统清理合法范围）
  *   7. DB-016 Quartz 调度表结构级验证（委托核心语句）
  *   8. DB-017 PG 元数据测试表与注释/索引核对
+ *   9. ZS-BPM-001 BPM 独立装配与 PG 验收（两阶段：owner 建表引导 + app 低权限运行）
  * 任一套件失败退出非零。
  * 用法：node scripts/db/run-pg-regression.mjs
  */
@@ -28,6 +29,7 @@ const cases = [
   { id: 'ZS-DB-018 ORM/手写SQL/租户隔离', cmd: ['node', 'scripts/db/run-db018-verify.mjs'] },
   { id: 'DB-016 Quartz 调度表结构级验证', caseFile: 'scripts/db/cases/db016-quartz-schema.sql' },
   { id: 'DB-017 元数据测试表', caseFile: 'scripts/db/cases/db017-metadata-testtable.sql' },
+  { id: 'ZS-BPM-001 BPM 独立装配与PG验收', cmd: ['node', 'scripts/db/run-bpm001-verify.mjs'] },
 ];
 
 let failed = false;
diff --git a/services/zhongshu-core/zszj-module-bpm/pom.xml b/services/zhongshu-core/zszj-module-bpm/pom.xml
index a1d52c2a..57c278b1 100644
--- a/services/zhongshu-core/zszj-module-bpm/pom.xml
+++ b/services/zhongshu-core/zszj-module-bpm/pom.xml
@@ -75,5 +75,12 @@
             <groupId>org.flowable</groupId>
             <artifactId>flowable-spring-boot-starter-actuator</artifactId>
         </dependency>
+
+        <!-- ZS-BPM-001：PG 集成夹具驱动（仅测试作用域；模块运行时不携带 JDBC 驱动，驱动由 server 侧统一引入） -->
+        <dependency>
+            <groupId>org.postgresql</groupId>
+            <artifactId>postgresql</artifactId>
+            <scope>test</scope>
+        </dependency>
     </dependencies>
 </project>
diff --git a/services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarness.java b/services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarness.java
new file mode 100644
index 00000000..da940ef5
--- /dev/null
+++ b/services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarness.java
@@ -0,0 +1,94 @@
+package cn.zszj.module.bpm.harness;
+
+import java.time.Duration;
+import java.util.Map;
+import java.util.Objects;
+import java.util.concurrent.ConcurrentHashMap;
+import java.util.concurrent.atomic.AtomicLong;
+
+/**
+ * ZS-BPM-001 中性技术夹具常量与引擎事件记录器。
+ *
+ * 边界（对齐 docs/02 第 6.5 节 D-07 确认前禁令）：夹具流程只含引擎技术语义
+ * （部署/发起/审批完成/拒绝分支/撤回/转办/异步任务），不固化任何众墅业务字段、
+ * 业务状态机或组织任职语义；流程键统一 tech_neutral_* 前缀。
+ */
+public final class BpmPgHarness {
+
+    /** 中性审批流程键（发起→人工任务→按完成变量分支→两个结束态）。 */
+    public static final String PROCESS_APPROVAL = "tech_neutral_approval";
+    /** 中性异步回声流程键（发起→异步服务任务→结束）。 */
+    public static final String PROCESS_ASYNC_ECHO = "tech_neutral_async_echo";
+
+    /** 审批完成变量名（通过/拒绝两分支的唯一路由依据，属夹具技术变量而非业务字段）。 */
+    public static final String VAR_OUTCOME = "harness_outcome";
+    public static final String OUTCOME_APPROVE = "approve";
+    public static final String OUTCOME_REJECT = "reject";
+
+    /** 技术租户标签（引擎 tenantId 仅是标签，不含 ACL 语义——业务组织隔离归 B08/D-07）。 */
+    public static final String TENANT_1 = "1";
+    public static final String TENANT_2 = "2";
+
+    /** 撤回原因（runtimeService.deleteProcessInstance 的技术语义）。 */
+    public static final String WITHDRAW_REASON = "harness:withdraw";
+
+    private BpmPgHarness() {
+    }
+
+    /**
+     * 引擎事件计数器：证明 EngineConfigurationConfigurer#setEventListeners 装配扩展点
+     * （与 BpmFlowableConfiguration 同一机制）在真实 PG 引擎上确实生效。
+     * 静态存储：同一 JVM 内跨 Spring 上下文可读。
+     */
+    public static final class EngineEventRecorder implements
+            org.flowable.common.engine.api.delegate.event.FlowableEventListener {
+
+        private static final Map<String, AtomicLong> COUNTERS = new ConcurrentHashMap<>();
+
+        public static long count(String eventType) {
+            return COUNTERS.getOrDefault(eventType, new AtomicLong()).get();
+        }
+
+        public static void reset() {
+            COUNTERS.clear();
+        }
+
+        @Override
+        public void onEvent(org.flowable.common.engine.api.delegate.event.FlowableEvent event) {
+            COUNTERS.computeIfAbsent(event.getType().name(), k -> new AtomicLong()).incrementAndGet();
+        }
+
+        @Override
+        public boolean isFailOnException() {
+            return false;
+        }
+
+        @Override
+        public boolean isFireOnTransactionLifecycleEvent() {
+            return false;
+        }
+
+        @Override
+        public String getOnTransaction() {
+            return null;
+        }
+    }
+
+    /** 简单轮询等待（无 Awaitility 依赖）。 */
+    public static boolean waitUntil(Duration timeout, java.util.function.BooleanSupplier condition) {
+        Objects.requireNonNull(condition);
+        long deadline = System.nanoTime() + timeout.toNanos();
+        while (System.nanoTime() < deadline) {
+            if (condition.getAsBoolean()) {
+                return true;
+            }
+            try {
+                Thread.sleep(500L);
+            } catch (InterruptedException e) {
+                Thread.currentThread().interrupt();
+                return false;
+            }
+        }
+        return condition.getAsBoolean();
+    }
+}
diff --git a/services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessBootstrapTest.java b/services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessBootstrapTest.java
new file mode 100644
index 00000000..083d27ef
--- /dev/null
+++ b/services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessBootstrapTest.java
@@ -0,0 +1,173 @@
+package cn.zszj.module.bpm.harness;
+
+import org.flowable.engine.ManagementService;
+import org.flowable.engine.RepositoryService;
+import org.flowable.engine.RuntimeService;
+import org.flowable.engine.runtime.ProcessInstance;
+import org.junit.jupiter.api.BeforeAll;
+import org.junit.jupiter.api.MethodOrderer;
+import org.junit.jupiter.api.Order;
+import org.junit.jupiter.api.Test;
+import org.junit.jupiter.api.TestMethodOrder;
+import org.springframework.beans.factory.annotation.Autowired;
+import org.springframework.boot.test.context.SpringBootTest;
+import org.springframework.jdbc.core.JdbcTemplate;
+
+import java.util.List;
+
+import static org.junit.jupiter.api.Assertions.assertEquals;
+import static org.junit.jupiter.api.Assertions.assertNotEquals;
+import static org.junit.jupiter.api.Assertions.assertNotNull;
+import static org.junit.jupiter.api.Assertions.assertTrue;
+
+/**
+ * ZS-BPM-001 引导阶段（owner 账号，引擎自管建表，异步执行器挂起）：
+ * 在空 PG 上建引擎表、部署中性夹具、发起运行实例并留下异步积压，
+ * 供 runtime 阶段以低权限 app 账号重启后接续验证。
+ * 由 scripts/db/run-bpm001-verify.mjs 注入环境变量并作为第一阶段调用。
+ */
+@SpringBootTest(classes = BpmPgHarnessConfiguration.class)
+@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
+class BpmPgHarnessBootstrapTest {
+
+    private static final String RES_APPROVAL = "cn/zszj/module/bpm/harness/neutral-approval.bpmn20.xml";
+    private static final String RES_APPROVAL_V2 = "cn/zszj/module/bpm/harness/neutral-approval-v2.bpmn20.xml";
+    private static final String RES_ASYNC = "cn/zszj/module/bpm/harness/neutral-async-echo.bpmn20.xml";
+
+    @Autowired
+    private RepositoryService repositoryService;
+    @Autowired
+    private RuntimeService runtimeService;
+    @Autowired
+    private ManagementService managementService;
+    @Autowired
+    private JdbcTemplate jdbcTemplate;
+
+    @BeforeAll
+    static void requireBootstrapPhase() {
+        // 引导阶段语义：允许建表（owner）+ 执行器挂起（留积压）；错阶段即快速失败
+        assertTrue(Boolean.parseBoolean(BpmPgHarnessConfiguration.requireEnv(BpmPgHarnessConfiguration.ENV_SCHEMA_UPDATE)),
+                "[bpm-pg-harness] bootstrap 阶段要求 ZSZJ_BPM_HARNESS_SCHEMA_UPDATE=true");
+        assertTrue(!Boolean.parseBoolean(BpmPgHarnessConfiguration.requireEnv(BpmPgHarnessConfiguration.ENV_ASYNC_EXECUTOR)),
+                "[bpm-pg-harness] bootstrap 阶段要求 ZSZJ_BPM_HARNESS_ASYNC_EXECUTOR=false");
+        BpmPgHarness.EngineEventRecorder.reset();
+    }
+
+    @Test
+    @Order(10)
+    void engineCreatesSchemaOnEmptyPostgres() {
+        // PG 会把未加引号的标识符折叠为小写：Flowable 在 PG 上建的引擎表为小写 act_*
+        Integer actTables = jdbcTemplate.queryForObject(
+                "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND lower(table_name) LIKE 'act\\_%'",
+                Integer.class);
+        assertTrue(actTables != null && actTables >= 35,
+                "[bpm-pg-harness] Flowable 引擎表未在 PG 建齐，实际 ACT_ 表数量=" + actTables);
+        String schemaVersion = jdbcTemplate.queryForObject(
+                "SELECT VALUE_ FROM ACT_GE_PROPERTY WHERE NAME_ = 'schema.version'", String.class);
+        assertNotNull(schemaVersion, "引擎 schema.version 缺失");
+        // 以 owner 身份把版本写入探针表，供 runtime 阶段校验「重启不改结构」
+        jdbcTemplate.update("INSERT INTO bpm_harness_probe(note) VALUES (?)",
+                "bootstrap:schema.version=" + schemaVersion);
+        System.out.println("[bpm-pg-harness] bootstrap schema.version=" + schemaVersion
+                + " actTables=" + actTables);
+    }
+
+    @Test
+    @Order(20)
+    void deployNeutralFixturesWithDuplicateFilteringIdempotency() {
+        deploy(RES_APPROVAL, BpmPgHarness.TENANT_1);
+        deploy(RES_ASYNC, BpmPgHarness.TENANT_1);
+        deploy(RES_APPROVAL, BpmPgHarness.TENANT_2);
+        long deployments = repositoryService.createDeploymentQuery().count();
+        // 同名同资源重复部署（重复过滤）不产生新部署——重复部署幂等
+        deploy(RES_APPROVAL, BpmPgHarness.TENANT_1);
+        assertEquals(deployments, repositoryService.createDeploymentQuery().count(),
+                "重复部署不应新增 deployment");
+        assertEquals(1, repositoryService.createProcessDefinitionQuery()
+                .processDefinitionKey(BpmPgHarness.PROCESS_APPROVAL)
+                .processDefinitionTenantId(BpmPgHarness.TENANT_1).count());
+    }
+
+    @Test
+    @Order(30)
+    void startInstancesForRuntimePhase() {
+        // A1~A4 技术租户1（v1）；A5 技术租户2；审批实例共 6 个在 runtime 阶段逐项处置
+        startApproval("bpm001-A1");
+        startApproval("bpm001-A2");
+        startApproval("bpm001-A3");
+        startApproval("bpm001-A4");
+        startApproval("bpm001-A5", BpmPgHarness.TENANT_2);
+        // E1 异步实例：执行器挂起 → 任务留积压
+        runtimeService.startProcessInstanceByKeyAndTenantId(
+                BpmPgHarness.PROCESS_ASYNC_ECHO, "bpm001-E1", null, BpmPgHarness.TENANT_1);
+        assertEquals(6, runtimeService.createProcessInstanceQuery().count(),
+                "应发起 6 个运行实例（5 审批 + 1 异步）");
+        assertEquals(1, managementService.createJobQuery().count(),
+                "异步执行器挂起时应留下 1 个积压任务");
+        assertEquals(0, countProbeRowsForInstance(instanceIdByBusinessKey("bpm001-E1")),
+                "执行器挂起时探针表不应有异步回声记录");
+    }
+
+    @Test
+    @Order(40)
+    void engineTenantIdIsLabelNotAcl() {
+        // 引擎 tenantId 是技术标签：按租户过滤可见各自集合，但不过滤的查询跨租户可见
+        // （业务组织隔离归 B08/D-07，本断言钉住引擎真实语义防止误依赖）
+        assertEquals(5, runtimeService.createProcessInstanceQuery()
+                .processInstanceTenantId(BpmPgHarness.TENANT_1).count());
+        assertEquals(1, runtimeService.createProcessInstanceQuery()
+                .processInstanceTenantId(BpmPgHarness.TENANT_2).count());
+        assertEquals(6, runtimeService.createProcessInstanceQuery().count());
+    }
+
+    @Test
+    @Order(50)
+    void changedContentDeploysNewVersionAndOldInstancesStayPinned() {
+        deploy(RES_APPROVAL_V2, BpmPgHarness.TENANT_1);
+        // 新发起的实例落在最新版本 v2
+        ProcessInstance onV2 = startApproval("bpm001-A6");
+        assertEquals(2, repositoryService.createProcessDefinitionQuery()
+                .processDefinitionKey(BpmPgHarness.PROCESS_APPROVAL)
+                .processDefinitionTenantId(BpmPgHarness.TENANT_1)
+                .orderByProcessDefinitionVersion().desc().list().get(0).getVersion());
+        // 已运行实例钉在原版本 v1
+        ProcessInstance onV1 = runtimeService.createProcessInstanceQuery()
+                .processInstanceBusinessKey("bpm001-A1").singleResult();
+        assertNotEquals(onV2.getProcessDefinitionVersion(), onV1.getProcessDefinitionVersion());
+        assertEquals(1, onV1.getProcessDefinitionVersion());
+        assertEquals(2, onV2.getProcessDefinitionVersion());
+    }
+
+    private ProcessInstance startApproval(String businessKey) {
+        return startApproval(businessKey, BpmPgHarness.TENANT_1);
+    }
+
+    private ProcessInstance startApproval(String businessKey, String tenantId) {
+        return runtimeService.startProcessInstanceByKeyAndTenantId(
+                BpmPgHarness.PROCESS_APPROVAL, businessKey, null, tenantId);
+    }
+
+    private void deploy(String resource, String tenantId) {
+        repositoryService.createDeployment()
+                .addClasspathResource(resource)
+                .name("bpmPgHarness@" + resource.substring(resource.lastIndexOf('/') + 1) + "@" + tenantId)
+                .tenantId(tenantId)
+                .enableDuplicateFiltering()
+                .deploy();
+    }
+
+    private String instanceIdByBusinessKey(String businessKey) {
+        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
+                .processInstanceBusinessKey(businessKey).singleResult();
+        return instance == null ? null : instance.getId();
+    }
+
+    private int countProbeRowsForInstance(String instanceId) {
+        if (instanceId == null) {
+            return -1;
+        }
+        List<Integer> counts = jdbcTemplate.queryForList(
+                "SELECT count(*) FROM bpm_harness_probe WHERE note = ?", Integer.class, instanceId);
+        return counts.isEmpty() ? -1 : counts.get(0);
+    }
+}
diff --git a/services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessConfiguration.java b/services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessConfiguration.java
new file mode 100644
index 00000000..31c92cd9
--- /dev/null
+++ b/services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessConfiguration.java
@@ -0,0 +1,155 @@
+package cn.zszj.module.bpm.harness;
+
+import com.zaxxer.hikari.HikariConfig;
+import com.zaxxer.hikari.HikariDataSource;
+import org.flowable.engine.HistoryService;
+import org.flowable.engine.ManagementService;
+import org.flowable.engine.ProcessEngine;
+import org.flowable.engine.ProcessEngineConfiguration;
+import org.flowable.engine.RepositoryService;
+import org.flowable.engine.RuntimeService;
+import org.flowable.engine.TaskService;
+import org.flowable.spring.ProcessEngineFactoryBean;
+import org.flowable.spring.SpringProcessEngineConfiguration;
+import org.springframework.context.annotation.Bean;
+import org.springframework.context.annotation.Configuration;
+import org.springframework.jdbc.core.JdbcTemplate;
+import org.springframework.jdbc.datasource.DataSourceTransactionManager;
+import org.springframework.transaction.PlatformTransactionManager;
+import org.springframework.transaction.support.TransactionTemplate;
+
+import javax.sql.DataSource;
+
+/**
+ * ZS-BPM-001 批准的 BPM 测试装配：手工装配 SpringProcessEngineConfiguration + Spring 事务管理器，
+ * 与 BpmFlowableConfiguration 所依赖的引擎装配机制同构（同一扩展点），但不引入 System 模块与
+ * 候选人策略（审批资格归 ZS-BPM-002），专验引擎在真实 PostgreSQL 上的建表、事务、租户标签、
+ * 异步执行器与启停。
+ *
+ * 环境变量（由 scripts/db/run-bpm001-verify.mjs 按阶段注入，缺失即快速失败、不静默跳过）：
+ *   ZSZJ_BPM_HARNESS_JDBC_URL      必须 jdbc:postgresql:// 开头；
+ *   ZSZJ_BPM_HARNESS_USERNAME      bootstrap 阶段=zhongshu_owner，runtime 阶段=zhongshu_app（低权限）；
+ *   ZSZJ_BPM_HARNESS_PASSWORD
+ *   ZSZJ_BPM_HARNESS_SCHEMA_UPDATE true=引擎自管建表/升级（仅 owner 迁移阶段），false=运行期不改结构；
+ *   ZSZJ_BPM_HARNESS_ASYNC_EXECUTOR true=启动异步执行器（runtime 积压恢复），false=挂起留积压（bootstrap）。
+ */
+@Configuration(proxyBeanMethods = false)
+public class BpmPgHarnessConfiguration {
+
+    static final String ENV_URL = "ZSZJ_BPM_HARNESS_JDBC_URL";
+    static final String ENV_USERNAME = "ZSZJ_BPM_HARNESS_USERNAME";
+    static final String ENV_PASSWORD = "ZSZJ_BPM_HARNESS_PASSWORD";
+    static final String ENV_SCHEMA_UPDATE = "ZSZJ_BPM_HARNESS_SCHEMA_UPDATE";
+    static final String ENV_ASYNC_EXECUTOR = "ZSZJ_BPM_HARNESS_ASYNC_EXECUTOR";
+
+    static String requireEnv(String key) {
+        String value = System.getenv(key);
+        if (value == null || value.isBlank()) {
+            throw new IllegalStateException("[bpm-pg-harness] 缺少环境变量 " + key + "：验证不得静默跳过，须由 run-bpm001-verify.mjs 注入");
+        }
+        return value.trim();
+    }
+
+    static boolean requireFlag(String key) {
+        String value = requireEnv(key);
+        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
+            throw new IllegalStateException("[bpm-pg-harness] 环境变量 " + key + " 须为 true/false，实际=" + value);
+        }
+        return Boolean.parseBoolean(value);
+    }
+
+    @Bean(destroyMethod = "close")
+    public HikariDataSource bpmHarnessDataSource() {
+        String url = requireEnv(ENV_URL);
+        if (!url.startsWith("jdbc:postgresql://")) {
+            throw new IllegalStateException("[bpm-pg-harness] 夹具仅面向真实 PostgreSQL，收到 " + url);
+        }
+        HikariConfig config = new HikariConfig();
+        config.setJdbcUrl(url);
+        config.setUsername(requireEnv(ENV_USERNAME));
+        config.setPassword(System.getenv(ENV_PASSWORD) == null ? "" : System.getenv(ENV_PASSWORD));
+        config.setMaximumPoolSize(4);
+        config.setMinimumIdle(1);
+        config.setPoolName("bpm-pg-harness");
+        // 驱动由 zszj-spring-boot-starter-mybatis 传入 PG 驱动
+        config.setDriverClassName("org.postgresql.Driver");
+        return new HikariDataSource(config);
+    }
+
+    @Bean
+    public PlatformTransactionManager bpmHarnessTransactionManager(DataSource dataSource) {
+        // 与应用运行形态同构：引擎与业务共用 Spring DataSourceTransactionManager 的事务边界
+        return new DataSourceTransactionManager(dataSource);
+    }
+
+    @Bean
+    public TransactionTemplate bpmHarnessTransactionTemplate(PlatformTransactionManager txManager) {
+        return new TransactionTemplate(txManager);
+    }
+
+    @Bean
+    public SpringProcessEngineConfiguration bpmHarnessProcessEngineConfiguration(
+            DataSource dataSource, PlatformTransactionManager transactionManager) {
+        SpringProcessEngineConfiguration configuration = new SpringProcessEngineConfiguration();
+        configuration.setDataSource(dataSource);
+        configuration.setTransactionManager(transactionManager);
+        // 引擎表迁移责任：bootstrap 阶段（owner 账号）允许引擎自建/升级；runtime 阶段（app 低权限）禁止改结构
+        configuration.setDatabaseSchemaUpdate(requireFlag(ENV_SCHEMA_UPDATE)
+                ? ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE
+                : ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE);
+        configuration.setAsyncExecutorActivate(requireFlag(ENV_ASYNC_EXECUTOR));
+        configuration.setDeploymentName("bpmPgHarness");
+        // 与 BpmFlowableConfiguration 相同的扩展点：注册引擎事件监听
+        configuration.setEventListeners(java.util.List.of(new BpmPgHarness.EngineEventRecorder()));
+        return configuration;
+    }
+
+    @Bean
+    public ProcessEngineFactoryBean bpmHarnessProcessEngine(SpringProcessEngineConfiguration configuration) {
+        ProcessEngineFactoryBean factoryBean = new ProcessEngineFactoryBean();
+        factoryBean.setProcessEngineConfiguration(configuration);
+        return factoryBean;
+    }
+
+    @Bean
+    public RepositoryService repositoryService(ProcessEngine processEngine) {
+        return processEngine.getRepositoryService();
+    }
+
+    @Bean
+    public RuntimeService runtimeService(ProcessEngine processEngine) {
+        return processEngine.getRuntimeService();
+    }
+
+    @Bean
+    public TaskService taskService(ProcessEngine processEngine) {
+        return processEngine.getTaskService();
+    }
+
+    @Bean
+    public HistoryService historyService(ProcessEngine processEngine) {
+        return processEngine.getHistoryService();
+    }
+
+    @Bean
+    public ManagementService managementService(ProcessEngine processEngine) {
+        return processEngine.getManagementService();
+    }
+
+    @Bean
+    public org.flowable.job.service.impl.asyncexecutor.AsyncExecutor bpmHarnessAsyncExecutor(
+            SpringProcessEngineConfiguration configuration) {
+        // 与引擎构建使用同一实例（configuration 惰性创建默认执行器），供测试显式启停（isActive/start/shutdown）
+        return configuration.getAsyncExecutor();
+    }
+
+    @Bean
+    public JdbcTemplate bpmHarnessJdbcTemplate(DataSource dataSource) {
+        return new JdbcTemplate(dataSource);
+    }
+
+    @Bean
+    public NeutralEchoDelegate neutralEchoDelegate(DataSource dataSource) {
+        return new NeutralEchoDelegate(dataSource);
+    }
+}
diff --git a/services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessRuntimeTest.java b/services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessRuntimeTest.java
new file mode 100644
index 00000000..6ce0e930
--- /dev/null
+++ b/services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessRuntimeTest.java
@@ -0,0 +1,240 @@
+package cn.zszj.module.bpm.harness;
+
+import org.flowable.engine.HistoryService;
+import org.flowable.engine.ManagementService;
+import org.flowable.engine.RepositoryService;
+import org.flowable.engine.RuntimeService;
+import org.flowable.engine.TaskService;
+import org.flowable.engine.history.HistoricProcessInstance;
+import org.flowable.engine.runtime.ProcessInstance;
+import org.flowable.task.api.Task;
+import org.junit.jupiter.api.BeforeAll;
+import org.junit.jupiter.api.MethodOrderer;
+import org.junit.jupiter.api.Order;
+import org.junit.jupiter.api.Test;
+import org.junit.jupiter.api.TestMethodOrder;
+import org.springframework.beans.factory.annotation.Autowired;
+import org.springframework.boot.test.context.SpringBootTest;
+import org.springframework.jdbc.core.JdbcTemplate;
+import org.springframework.transaction.support.TransactionTemplate;
+
+import java.time.Duration;
+import java.util.List;
+import java.util.Map;
+
+import static org.junit.jupiter.api.Assertions.assertEquals;
+import static org.junit.jupiter.api.Assertions.assertFalse;
+import static org.junit.jupiter.api.Assertions.assertNotNull;
+import static org.junit.jupiter.api.Assertions.assertNull;
+import static org.junit.jupiter.api.Assertions.assertTrue;
+
+/**
+ * ZS-BPM-001 运行阶段（低权限 zhongshu_app 账号，禁止改引擎结构，异步执行器开启）：
+ * 以新引擎进程重启到 bootstrap 阶段留下的同一 PG 库，验证重启恢复、部署/发起/通过/拒绝/
+ * 撤回/转办/分页历史、事务原子性、异步积压恢复与运行期零 DDL。
+ * 由 scripts/db/run-bpm001-verify.mjs 作为第二阶段调用。
+ */
+@SpringBootTest(classes = BpmPgHarnessConfiguration.class)
+@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
+class BpmPgHarnessRuntimeTest {
+
+    private static final Duration ASYNC_WAIT = Duration.ofSeconds(60);
+
+    @Autowired
+    private RepositoryService repositoryService;
+    @Autowired
+    private RuntimeService runtimeService;
+    @Autowired
+    private TaskService taskService;
+    @Autowired
+    private HistoryService historyService;
+    @Autowired
+    private ManagementService managementService;
+    @Autowired
+    private TransactionTemplate transactionTemplate;
+    @Autowired
+    private JdbcTemplate jdbcTemplate;
+    @Autowired
+    private org.flowable.job.service.impl.asyncexecutor.AsyncExecutor asyncExecutor;
+
+    @BeforeAll
+    static void requireRuntimePhase() {
+        // 运行阶段语义：低权限账号禁止改结构 + 执行器不自动启动（R70 显式启停，保证重启恢复断言确定性）
+        assertFalse(Boolean.parseBoolean(BpmPgHarnessConfiguration.requireEnv(BpmPgHarnessConfiguration.ENV_SCHEMA_UPDATE)),
+                "[bpm-pg-harness] runtime 阶段要求 ZSZJ_BPM_HARNESS_SCHEMA_UPDATE=false");
+        assertFalse(Boolean.parseBoolean(BpmPgHarnessConfiguration.requireEnv(BpmPgHarnessConfiguration.ENV_ASYNC_EXECUTOR)),
+                "[bpm-pg-harness] runtime 阶段要求 ZSZJ_BPM_HARNESS_ASYNC_EXECUTOR=false（执行器由 R70 显式启停）");
+    }
+
+    @Test
+    @Order(10)
+    void engineRestartsOnExistingSchemaWithoutStructureChange() {
+        // 重启直证：新进程以 schema-update=false 启动，bootstrap 阶段的定义/实例/历史全部可见
+        assertEquals(2, repositoryService.createProcessDefinitionQuery()
+                .processDefinitionKey(BpmPgHarness.PROCESS_APPROVAL)
+                .processDefinitionTenantId(BpmPgHarness.TENANT_1).count(), "租户1 审批流程应保留 v1+v2 两个版本");
+        assertEquals(7, runtimeService.createProcessInstanceQuery().count(), "重启后 7 个运行实例应全部恢复");
+        // schema.version 与 bootstrap 写入探针表的记录一致 → 重启未改引擎结构
+        String recorded = jdbcTemplate.queryForObject(
+                "SELECT note FROM bpm_harness_probe WHERE note LIKE 'bootstrap:schema.version=%' ORDER BY id LIMIT 1",
+                String.class);
+        String current = jdbcTemplate.queryForObject(
+                "SELECT VALUE_ FROM ACT_GE_PROPERTY WHERE NAME_ = 'schema.version'", String.class);
+        assertEquals(recorded, "bootstrap:schema.version=" + current,
+                "重启后引擎 schema 版本应与 bootstrap 记录一致（运行期零 DDL）");
+    }
+
+    @Test
+    @Order(20)
+    void approveCompletesInstanceOnOriginalDefinitionVersion() {
+        ProcessInstance instance = requireRunning("bpm001-A1");
+        Task task = requireSingleTask(instance.getId());
+        taskService.complete(task.getId(), Map.of(BpmPgHarness.VAR_OUTCOME, BpmPgHarness.OUTCOME_APPROVE));
+        HistoricProcessInstance historic = historyService.createHistoricProcessInstanceQuery()
+                .processInstanceId(instance.getId()).finished().singleResult();
+        assertNotNull(historic, "审批通过后实例应进入历史");
+        assertEquals(1, historic.getProcessDefinitionVersion(), "旧实例应在原版本 v1 上完成（版本钉住）");
+        assertEquals(BpmPgHarness.OUTCOME_APPROVE, historyService.createHistoricVariableInstanceQuery()
+                .processInstanceId(instance.getId()).variableName(BpmPgHarness.VAR_OUTCOME)
+                .singleResult().getValue());
+        assertNull(runtimeService.createProcessInstanceQuery()
+                .processInstanceId(instance.getId()).singleResult());
+    }
+
+    @Test
+    @Order(30)
+    void rejectTakesRejectedBranch() {
+        ProcessInstance instance = requireRunning("bpm001-A2");
+        Task task = requireSingleTask(instance.getId());
+        taskService.complete(task.getId(), Map.of(BpmPgHarness.VAR_OUTCOME, BpmPgHarness.OUTCOME_REJECT));
+        assertNotNull(historyService.createHistoricProcessInstanceQuery()
+                .processInstanceId(instance.getId()).finished().singleResult(), "拒绝后实例应进入历史");
+        assertEquals(1, historyService.createHistoricActivityInstanceQuery()
+                .processInstanceId(instance.getId()).activityId("rejectedEnd").count(), "应走 reject 分支结束");
+    }
+
+    @Test
+    @Order(40)
+    void withdrawCancelsInstanceWithReasonInHistory() {
+        ProcessInstance instance = requireRunning("bpm001-A3");
+        runtimeService.deleteProcessInstance(instance.getId(), BpmPgHarness.WITHDRAW_REASON);
+        HistoricProcessInstance historic = historyService.createHistoricProcessInstanceQuery()
+                .processInstanceId(instance.getId()).finished().singleResult();
+        assertNotNull(historic, "撤回后实例应进入历史");
+        assertEquals(BpmPgHarness.WITHDRAW_REASON, historic.getDeleteReason(), "撤回原因应落历史可回查");
+        assertNull(runtimeService.createProcessInstanceQuery()
+                .processInstanceId(instance.getId()).singleResult());
+    }
+
+    @Test
+    @Order(50)
+    void transferReassignsTaskAndCompletesNormally() {
+        ProcessInstance instance = requireRunning("bpm001-A4");
+        Task task = requireSingleTask(instance.getId());
+        taskService.claim(task.getId(), "tech-user-a");
+        assertEquals("tech-user-a", taskService.createTaskQuery().taskId(task.getId()).singleResult().getAssignee());
+        taskService.setAssignee(task.getId(), "tech-user-b");
+        assertEquals("tech-user-b", taskService.createTaskQuery().taskId(task.getId()).singleResult().getAssignee(),
+                "转办后受理人应更新");
+        taskService.complete(task.getId(), Map.of(BpmPgHarness.VAR_OUTCOME, BpmPgHarness.OUTCOME_APPROVE));
+        assertEquals("tech-user-b", historyService.createHistoricTaskInstanceQuery()
+                .taskId(task.getId()).singleResult().getAssignee(), "任务历史应记录转办后的受理人");
+    }
+
+    @Test
+    @Order(60)
+    void springTransactionRollbackLeavesNoEngineTrace() {
+        long before = runtimeService.createProcessInstanceQuery().count();
+        // 控制组：提交事务内的发起持久化
+        transactionTemplate.executeWithoutResult(status ->
+                runtimeService.startProcessInstanceByKeyAndTenantId(
+                        BpmPgHarness.PROCESS_APPROVAL, "bpm001-A7", null, BpmPgHarness.TENANT_1));
+        assertEquals(before + 1, runtimeService.createProcessInstanceQuery().count());
+        // 实验组：回滚事务内的发起不留下任何引擎痕迹（证明引擎走 Spring 事务管理器）
+        try {
+            transactionTemplate.executeWithoutResult(status -> {
+                runtimeService.startProcessInstanceByKeyAndTenantId(
+                        BpmPgHarness.PROCESS_APPROVAL, "bpm001-A8", null, BpmPgHarness.TENANT_1);
+                throw new IllegalStateException("harness: 模拟业务事务失败");
+            });
+        } catch (IllegalStateException expected) {
+            // 由回滚断言承接
+        }
+        assertEquals(before + 1, runtimeService.createProcessInstanceQuery().count(),
+                "回滚事务内的发起不应持久化");
+        assertNull(runtimeService.createProcessInstanceQuery()
+                .processInstanceBusinessKey("bpm001-A8").singleResult());
+    }
+
+    @Test
+    @Order(70)
+    void asyncBacklogRecoversAfterRestartAndExecutorStartStopWorks() {
+        // 执行器未自启：bootstrap 留下的 E1 积压跨重启持久保留
+        String e1 = requireRunning("bpm001-E1").getId();
+        assertEquals(1, managementService.createJobQuery().count(), "执行器未启动时积压任务应保留");
+        assertEquals(0, probeCount(e1), "执行器未启动时探针不应有回声记录");
+        // 显式启动执行器（启停控制直证）：积压被消化且实例完结
+        asyncExecutor.start();
+        assertTrue(BpmPgHarness.waitUntil(ASYNC_WAIT, () -> probeCount(e1) > 0),
+                "执行器启动后异步积压任务未被执行（探针无记录）");
+        assertTrue(BpmPgHarness.waitUntil(ASYNC_WAIT, () -> historyService.createHistoricProcessInstanceQuery()
+                .processInstanceId(e1).finished().count() == 1), "E1 实例应执行完毕");
+        // 实时路径：执行器在线时新发起的 E2 同样被执行
+        ProcessInstance e2 = runtimeService.startProcessInstanceByKeyAndTenantId(
+                BpmPgHarness.PROCESS_ASYNC_ECHO, "bpm001-E2", null, BpmPgHarness.TENANT_1);
+        assertTrue(BpmPgHarness.waitUntil(ASYNC_WAIT, () -> probeCount(e2.getId()) > 0),
+                "执行器在线时异步任务未被执行");
+        assertTrue(BpmPgHarness.waitUntil(ASYNC_WAIT, () -> historyService.createHistoricProcessInstanceQuery()
+                .processInstanceId(e2.getId()).finished().count() == 1));
+        assertEquals(0, managementService.createJobQuery().count(), "不应残留待执行任务");
+        assertEquals(0, managementService.createDeadLetterJobQuery().count(), "不应有死信任务");
+        // 优雅停机：执行器显式关闭后回到非活动态（启停收尾，JVM 退出时不再挂起）
+        asyncExecutor.shutdown();
+        assertFalse(asyncExecutor.isActive(), "执行器 shutdown 后应回到非活动态");
+    }
+
+    @Test
+    @Order(80)
+    void historyAndRuntimeQueriesSupportPaging() {
+        List<HistoricProcessInstance> page = historyService.createHistoricProcessInstanceQuery()
+                .finished()
+                .orderByProcessInstanceEndTime().asc()
+                .listPage(0, 2);
+        assertEquals(2, page.size(), "分页历史查询应返回整页");
+        assertTrue(historyService.createHistoricProcessInstanceQuery().finished().count() >= 4,
+                "通过/拒绝/撤回/转办完成共 4 个实例应进入历史");
+        assertEquals(2, historyService.createHistoricTaskInstanceQuery().finished()
+                .listPage(0, 2).size(), "任务历史分页应可用");
+        assertTrue(runtimeService.createProcessInstanceQuery()
+                .listPage(0, 3).size() <= 3, "运行实例分页应可用");
+    }
+
+    @Test
+    @Order(90)
+    void configuredEventListenersFireOnRealEngine() {
+        // 与 BpmFlowableConfiguration 相同的 setEventListeners 扩展点在真实 PG 引擎上生效
+        assertTrue(BpmPgHarness.EngineEventRecorder.count("PROCESS_STARTED") >= 2,
+                "引擎事件监听器应记录 PROCESS_STARTED");
+        assertTrue(BpmPgHarness.EngineEventRecorder.count("TASK_COMPLETED") >= 3,
+                "引擎事件监听器应记录 TASK_COMPLETED");
+    }
+
+    private ProcessInstance requireRunning(String businessKey) {
+        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
+                .processInstanceBusinessKey(businessKey).singleResult();
+        assertNotNull(instance, "实例 " + businessKey + " 应处于运行态（重启恢复）");
+        return instance;
+    }
+
+    private Task requireSingleTask(String instanceId) {
+        Task task = taskService.createTaskQuery().processInstanceId(instanceId).singleResult();
+        assertNotNull(task, "审批实例应有且仅有一个待办任务");
+        return task;
+    }
+
+    private int probeCount(String instanceId) {
+        List<Integer> counts = jdbcTemplate.queryForList(
+                "SELECT count(*) FROM bpm_harness_probe WHERE note = ?", Integer.class, instanceId);
+        return counts.isEmpty() ? 0 : counts.get(0);
+    }
+}
diff --git a/services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/NeutralEchoDelegate.java b/services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/NeutralEchoDelegate.java
new file mode 100644
index 00000000..a6cef691
--- /dev/null
+++ b/services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/NeutralEchoDelegate.java
@@ -0,0 +1,37 @@
+package cn.zszj.module.bpm.harness;
+
+import org.flowable.common.engine.api.FlowableException;
+import org.flowable.engine.delegate.DelegateExecution;
+import org.flowable.engine.delegate.JavaDelegate;
+
+import javax.sql.DataSource;
+import java.sql.Connection;
+import java.sql.PreparedStatement;
+import java.sql.SQLException;
+
+/**
+ * ZS-BPM-001 异步回声委托：异步服务任务执行时向夹具探针表写入一行（流程实例 ID），
+ * 供「异步执行器积压恢复/实时消费」断言。探针表 bpm_harness_probe 由编排器以 owner 账号
+ * 预建（默认权限自动授权 app 账号 DML），属一次性测试脚手架，随容器销毁。
+ */
+public class NeutralEchoDelegate implements JavaDelegate {
+
+    private final DataSource dataSource;
+
+    public NeutralEchoDelegate(DataSource dataSource) {
+        this.dataSource = dataSource;
+    }
+
+    @Override
+    public void execute(DelegateExecution execution) {
+        String instanceId = execution.getProcessInstanceId();
+        try (Connection connection = dataSource.getConnection();
+             PreparedStatement statement = connection.prepareStatement(
+                     "INSERT INTO bpm_harness_probe(note) VALUES (?)")) {
+            statement.setString(1, instanceId);
+            statement.executeUpdate();
+        } catch (SQLException e) {
+            throw new FlowableException("[bpm-pg-harness] 探针写入失败", e);
+        }
+    }
+}
diff --git a/services/zhongshu-core/zszj-module-bpm/src/test/resources/cn/zszj/module/bpm/harness/neutral-approval-v2.bpmn20.xml b/services/zhongshu-core/zszj-module-bpm/src/test/resources/cn/zszj/module/bpm/harness/neutral-approval-v2.bpmn20.xml
new file mode 100644
index 00000000..72495dbc
--- /dev/null
+++ b/services/zhongshu-core/zszj-module-bpm/src/test/resources/cn/zszj/module/bpm/harness/neutral-approval-v2.bpmn20.xml
@@ -0,0 +1,23 @@
+<?xml version="1.0" encoding="UTF-8"?>
+<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
+             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
+             xmlns:flowable="http://flowable.org/bpmn"
+             targetNamespace="urn:zszj:bpm-pg-harness">
+  <!-- ZS-BPM-001 中性技术夹具 V2：同键改内容，验证变更部署产生新版本且旧实例钉在原版本 -->
+  <process id="tech_neutral_approval" name="技术中性审批夹具 V2" isExecutable="true">
+    <documentation>ZS-BPM-001 版本演进验证用：与 V1 仅名称/文档不同</documentation>
+    <startEvent id="start"/>
+    <sequenceFlow id="f1" sourceRef="start" targetRef="auditTask"/>
+    <userTask id="auditTask" name="技术审核任务"/>
+    <sequenceFlow id="f2" sourceRef="auditTask" targetRef="outcomeGateway"/>
+    <exclusiveGateway id="outcomeGateway" name="结果分支"/>
+    <sequenceFlow id="f3" sourceRef="outcomeGateway" targetRef="approvedEnd">
+      <conditionExpression xsi:type="tFormalExpression">${harness_outcome == 'approve'}</conditionExpression>
+    </sequenceFlow>
+    <sequenceFlow id="f4" sourceRef="outcomeGateway" targetRef="rejectedEnd">
+      <conditionExpression xsi:type="tFormalExpression">${harness_outcome == 'reject'}</conditionExpression>
+    </sequenceFlow>
+    <endEvent id="approvedEnd"/>
+    <endEvent id="rejectedEnd"/>
+  </process>
+</definitions>
diff --git a/services/zhongshu-core/zszj-module-bpm/src/test/resources/cn/zszj/module/bpm/harness/neutral-approval.bpmn20.xml b/services/zhongshu-core/zszj-module-bpm/src/test/resources/cn/zszj/module/bpm/harness/neutral-approval.bpmn20.xml
new file mode 100644
index 00000000..d77dc031
--- /dev/null
+++ b/services/zhongshu-core/zszj-module-bpm/src/test/resources/cn/zszj/module/bpm/harness/neutral-approval.bpmn20.xml
@@ -0,0 +1,22 @@
+<?xml version="1.0" encoding="UTF-8"?>
+<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
+             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
+             xmlns:flowable="http://flowable.org/bpmn"
+             targetNamespace="urn:zszj:bpm-pg-harness">
+  <!-- ZS-BPM-001 中性技术夹具：不含任何众墅业务字段/状态语义（D-07 确认前禁令） -->
+  <process id="tech_neutral_approval" name="技术中性审批夹具" isExecutable="true">
+    <startEvent id="start"/>
+    <sequenceFlow id="f1" sourceRef="start" targetRef="auditTask"/>
+    <userTask id="auditTask" name="技术审核任务"/>
+    <sequenceFlow id="f2" sourceRef="auditTask" targetRef="outcomeGateway"/>
+    <exclusiveGateway id="outcomeGateway" name="结果分支"/>
+    <sequenceFlow id="f3" sourceRef="outcomeGateway" targetRef="approvedEnd">
+      <conditionExpression xsi:type="tFormalExpression">${harness_outcome == 'approve'}</conditionExpression>
+    </sequenceFlow>
+    <sequenceFlow id="f4" sourceRef="outcomeGateway" targetRef="rejectedEnd">
+      <conditionExpression xsi:type="tFormalExpression">${harness_outcome == 'reject'}</conditionExpression>
+    </sequenceFlow>
+    <endEvent id="approvedEnd"/>
+    <endEvent id="rejectedEnd"/>
+  </process>
+</definitions>
diff --git a/services/zhongshu-core/zszj-module-bpm/src/test/resources/cn/zszj/module/bpm/harness/neutral-async-echo.bpmn20.xml b/services/zhongshu-core/zszj-module-bpm/src/test/resources/cn/zszj/module/bpm/harness/neutral-async-echo.bpmn20.xml
new file mode 100644
index 00000000..ac779508
--- /dev/null
+++ b/services/zhongshu-core/zszj-module-bpm/src/test/resources/cn/zszj/module/bpm/harness/neutral-async-echo.bpmn20.xml
@@ -0,0 +1,16 @@
+<?xml version="1.0" encoding="UTF-8"?>
+<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
+             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
+             xmlns:flowable="http://flowable.org/bpmn"
+             targetNamespace="urn:zszj:bpm-pg-harness">
+  <!-- ZS-BPM-001 中性技术夹具：异步服务任务（委托写探针表）验证异步执行器 -->
+  <process id="tech_neutral_async_echo" name="技术异步回声夹具" isExecutable="true">
+    <startEvent id="start"/>
+    <sequenceFlow id="f1" sourceRef="start" targetRef="echoTask"/>
+    <serviceTask id="echoTask" name="技术异步回声任务"
+                 flowable:delegateExpression="${neutralEchoDelegate}"
+                 flowable:async="true"/>
+    <sequenceFlow id="f2" sourceRef="echoTask" targetRef="end"/>
+    <endEvent id="end"/>
+  </process>
+</definitions>

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "git diff main -- . ':"'!scripts/db/run-bpm001-verify.mjs'"' ':"'!scripts/db/run-pg-regression.mjs'"' ':"'!services/zhongshu-core/zszj-module-bpm'"'; git log --oneline --decorate -8; git log --oneline --left-right main...HEAD" in E:\zszj-wt-bpm-001-a
 succeeded in 23ms:
ba13c81d fix(brand-004-b): codex r1 处置（2×P2 全修）——恢复 fail() 基础设施失败退出码 3、MONITOR 就绪应答等待替代固定延时 [ZS-BRAND-004.B]
930d365e fix(brand-004-b): codex r0 处置（1×P1+7×P2 全修）——POSIX 子进程终止、缺 Docker 退出码 3 修复、键提取不漏报、取证时序与命名空间隔离直证 [ZS-BRAND-004.B]
345c1c12 feat(brand-004-b): 缓存/浏览器存储/会话键清单静态门禁 + 运行期隔离证明（36 断言全绿） [ZS-BRAND-004.B]
6d67c357 chore: 撤销误提交的 outputs/ 与 .omx/ 跟踪（保持未跟踪约定，旧命名日志触发品牌扫描误报）
> 3ea8051c feat(bpm-001-a): ZS-BPM-001 BPM 独立装配与 PostgreSQL 验收（B09 技术准备先行件）
< 270edff2 docs(sec-007): ZS-SEC-007 HANDOFF 补评 + hotfix 收口文档同步（POJONode 穿透 P1 + Locale.ROOT P2 修复归零）+ docs/05 V1.39
< 8aafcdd6 docs(file-001-a): ZS-FILE-001.A 收口文档同步（五项交付 + codex r0→r2 三弧处置入库 + 陈旧构件/边界登记）+ 主卡 FILE-001 转开发中 + docs/05 V1.40
< 27d7f550 Merge branch 'feat/file-001-a' (ZS-FILE-001.A 文件租户化与归属授权，codex r0→r2 三弧评审收敛：租户化+归属/下载关匿名旁路/批删越权整批拒绝/禁用未凭证化 create/update-scope)
< 2c07a190 fix(file-001-a): codex r1 2×P1+2×P2+P3（PRIVATE 管理分支改走 PermissionCommonApi 与 @ss.hasPermission 同源+owner=0 禁止冒领 + App 端 /create 同步禁用[基线 328→327] + 批删测试拆分真实他租户 id 与不存在 id 两用例 + FileServiceImplTest 补权限 mock）
< a1e3a505 Merge branch 'feat/sec-007'
< 39c8b8dd fix(sec-007): codex r1 P1——物化失败改 fail-closed 上抛（外层既有 catch 降级安全摘要），杜绝 toString 原文经 TextNode 穿透 [ZS-SEC-007 hotfix]
< 1d69e91c fix(file-001-a): codex r0 3×P1+3×P2+P3（PRIVATE 归属校验 owner/租户管理员[下载侧] + 禁用 presigned create 端点堵冒领[凭证化归 FILE-003] + 批删显式租户校验混真实他租户 id 用例 + 下载跨租户定位 PUBLIC 同址可用 + 错误码 1_001_003_005 解冲突；S3 直链绕过登记边界归 FILE-004.A、历史归属认领归 FILE-001.B）
< 9728b369 fix(sec-007): 日志脱敏补 POJONode(RawValue) 物化堵 @JsonRawValue 原文穿透 + 键归一改 Locale.ROOT [ZS-SEC-007 hotfix]
< a6de4c86 feat(file-001-a): 文件租户化与归属授权——infra_file 加 tenant_id/owner_user_id/scope 列[迁移+H2]、FileDO 改 TenantBaseDO、上传默认 PRIVATE 记录归属、下载端点关匿名旁路（PUBLIC 匿名/PRIVATE 登录同租户校验）、批量删除混入越权整批拒绝、update-scope 管理端点+权限、接口清单基线 328→329、补他区欠账列数断言 13→14 [ZS-FILE-001.A]
< e9354226 docs(review): HANDOFF 补评批次2入库（SEC-012.A/PERM-002.A CLEAN、PERM-001.A 1×P2、DB-018 3×P2 全部登记）+ 8 份交接单补评全收齐
< 64f6c8ae docs(review): HANDOFF 补评批次1入库（SEC-001.A/005 CLEAN、SEC-002 5×P2 登记、SEC-007 1×P1+1×P2 转 hotfix）

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "Get-Item -Force AGENTS.md, E:\\AGENTS.md, services\\AGENTS.md, services\\zhongshu-core\\AGENTS.md -ErrorAction SilentlyContinue; rg --files --hidden -g AGENTS.md -g '"'!**/.git/**'"' -g '"'!outputs/**'"'; rg -n --hidden -g '*.md' -g '"'!**/.git/**'"' -g '"'!outputs/**'"' 'D-07|6\\.5|ZS-BPM-001|flaky|附日期|文档.*规则' ." in E:\zszj-wt-bpm-001-a
 succeeded in 88ms:
.\docs\02-一期底座需求规格与待决策台账.md:6:> 状态：底座需求基线；D-09 已确认（2026-09-10），D-07 方向已采纳、细节待阶段 2 前确认，D-10、D-11 延后决策  
.\docs\02-一期底座需求规格与待决策台账.md:20:| `README.md` | 项目入口、当前阶段、文档索引、同步规则 | 不展开完整需求和技术论证 |
.\docs\02-一期底座需求规格与待决策台账.md:41:- D-01～D-06、D-08 保持已确认，D-07/D-09 保持待决，D-10/D-11 保持延后；本轮仅提升有证据的来源治理项，不改变众墅产品运行能力的需求状态。
.\docs\02-一期底座需求规格与待决策台账.md:43:- 按用户新增要求，每次模块分析必须对照众墅底座需求记录代码差距，并逐项维护 [模块分析与开发任务清单](05-底座模块分析与开发任务清单.md)。首版补录工程装配 6 项、数据库持久化 20 项、文档治理 1 项；均无本轮实现/运行验收证据，D-09 相关唯一性规则保持待决，不提升本文需求状态。
.\docs\02-一期底座需求规格与待决策台账.md:45:- 已继续完成 M04～M12 剩余 9 个模块静态分析，新增登录、组织、权限、配置、文件审计、通知、事件、BPM、多端和运行保障任务 45 项；当时累计 84 项（76 待开发、2 待决策、6 待前置）。具体编号、差距、依赖与正反向验收见 05 清单；D-07/D-09 仍待决，D-10/D-11 仍后置。本轮仅同步文档，没有运行代码、迁移、构建、真实集成或部署证据，任何 FND 状态均不提升。
.\docs\02-一期底座需求规格与待决策台账.md:46:- 已按清单评审修订依赖与文件交付合同，新增 ZS-SYS-001 基础管理 PG/API 和 Web 回归，覆盖 FND-SYS-001 的用户、角色、菜单、岗位、字典、配置、公告。当时为 85 项主任务（77 待开发、2 待决策、6 待前置）；22 项拆为 50 个分批子项，不重复计数。FND-INF-001 的验收覆盖不可变上传资产和主体绑定实际取流；所有 FND 状态与 D-07/D-09/D-10/D-11 状态不变。
.\docs\02-一期底座需求规格与待决策台账.md:47:- 用户新增品牌与代码命名要求：采用其提供的“众墅之家”Logo/展示名，产品代码改用 zszj 或 zs 前缀。本版以 zszj 主前缀规划，登记 FND-BRAND-001～003 和 ZS-BRAND-001～006；累计 91 主任务（83 待开发、2 待决策、6 待前置），25 主项拆为 57 个分批子项。仅原图留档与文档更新，尚未替换运行资源、执行源码改名或迁移；D-07/D-09/D-10/D-11 不变。
.\docs\02-一期底座需求规格与待决策台账.md:110:- Flowable 接入准备，并在 D-07 确认后承载首个审批流程；
.\docs\02-一期底座需求规格与待决策台账.md:272:## 6. 首条薄纵向业务链建议（D-07）
.\docs\02-一期底座需求规格与待决策台账.md:305:以下状态仅是建议，D-07 确认前不得视为最终业务规则：
.\docs\02-一期底座需求规格与待决策台账.md:313:### 6.5 D-07 确认前允许和禁止的工作
.\docs\02-一期底座需求规格与待决策台账.md:329:### 6.6 D-07 业务需求与验收草案
.\docs\02-一期底座需求规格与待决策台账.md:331:以下编号用于确保 D-07 确认后可以逐项落地。当前全部状态为“草案”，不代表已经批准开发。
.\docs\02-一期底座需求规格与待决策台账.md:335:| PILOT-REQ-001 | 平台运营创建加盟商申请，申请方提交资料和资质附件 | 草稿可保存；提交后字段锁定规则明确；未授权组织不能读取附件 | D-07 待确认 |
.\docs\02-一期底座需求规格与待决策台账.md:336:| PILOT-REQ-002 | 平台负责人审批通过或拒绝加盟商申请 | 通过和拒绝均记录意见、人员、时间；拒绝不得创建正式组织和负责人身份 | D-07 待确认 |
.\docs\02-一期底座需求规格与待决策台账.md:337:| PILOT-REQ-003 | 审批通过后幂等开通加盟商 | 重复回调或重复处理只能产生一个组织、一个负责人身份和一套默认授权 | D-07 待确认 |
.\docs\02-一期底座需求规格与待决策台账.md:338:| PILOT-REQ-004 | 加盟商负责人创建或邀请员工并分配授权 | 员工只进入目标组织；停用后立即失去访问；授权变化有审计 | D-07/D-09 待确认 |
.\docs\02-一期底座需求规格与待决策台账.md:339:| PILOT-REQ-005 | 平台运营向指定加盟商下发首条线索 | 线索归属由服务端写入；重复下发按幂等规则处理；其他加盟商不可见 | D-07 待确认 |
.\docs\02-一期底座需求规格与待决策台账.md:340:| PILOT-REQ-006 | 加盟商负责人分配线索，员工领取 | 负责人只能分配本组织人员；员工只能领取分配给自己的线索；并发领取结果唯一 | D-07 待确认 |
.\docs\02-一期底座需求规格与待决策台账.md:341:| PILOT-REQ-007 | 员工提交跟进记录 | 跟进人、时间、内容、下一步和状态变化可追踪；无权限用户不能新增或篡改 | D-07 待确认 |
.\docs\02-一期底座需求规格与待决策台账.md:342:| PILOT-REQ-008 | 线索转为有效商机或以原因关闭为无效 | 两种结束路径都有明确条件、必填原因和不可跳过的状态校验 | D-07 待确认 |
.\docs\02-一期底座需求规格与待决策台账.md:343:| PILOT-REQ-009 | 三类角色按范围查询链路结果 | 员工看本人、负责人看本组织、平台人员看授权范围；对象 ID 越权被拒绝 | D-07/D-09 待确认 |
.\docs\02-一期底座需求规格与待决策台账.md:344:| PILOT-REQ-010 | 关键节点发送站内通知并形成审计 | 审批、开通、下发、分配、领取和结束状态均可通过 trace/业务 ID 追踪；通知失败不静默 | D-07 待确认 |
.\docs\02-一期底座需求规格与待决策台账.md:354:| PEND-001 | D-07 | 方向已采纳（2026-09-10） | 是否采用“加盟商开通—首条线索闭环”；参与角色、字段、状态、转商机和无效条件是什么 | 用户已采纳建议薄链方向（不含报价/合同/回款）；PILOT-REQ-001~010 字段/状态/转商机条件待阶段 2 前逐项确认 | 首个业务纵向切片 | 阶段 2 开始前 |
.\docs\02-一期底座需求规格与待决策台账.md:387:| 阶段 2：薄业务链 | 加盟商开通、审批、负责人/员工授权、首条线索闭环 | D-07 确认；阶段 1A/1B 通过 | 三角色端到端验收、状态/权限/审计记录 |
.\docs\02-一期底座需求规格与待决策台账.md:391:众墅之家设计小程序 V1.2 的详细后端架构和实施蓝图属于阶段 3/4 候选专项，分别记录在 [设计小程序后端技术架构](../../../企业FDE项目/众墅之家设计小程序/03-众墅之家设计小程序后端技术架构-V1.0.md)与 [后端实施蓝图](../../../企业FDE项目/众墅之家设计小程序/众墅之家设计小程序后端实施蓝图-V1.0.md)。当前状态均为 `PROPOSED`：不得重开 D-04。阶段 0、1A、1B、阶段 2/D-07 与 D-09 验收后，才可由本专项承担阶段 3 的领域/Stub；D-10 未关闭不得做真实微信/支付，D-11 未关闭不得做真实 AI Provider。
.\docs\02-一期底座需求规格与待决策台账.md:403:| D-07 | PILOT-REQ-001～010、PEND-001 | 阶段 2 | 三角色端到端业务链、失败路径、权限和审计证据 |
.\docs\02-一期底座需求规格与待决策台账.md:433:- `PILOT-REQ-001～010` 当前仍是 D-07 待确认草案，不进入上述实施状态序列；
.\docs\02-一期底座需求规格与待决策台账.md:507:| 2026-08-29 | V0.1 | 记录 D-01～D-06 已确认；将 PostgreSQL 设为唯一数据库基线；加入一期底座需求、PostgreSQL 兼容门禁、薄纵向链建议、D-07/D-09～D-11 台账和文档同步规则 |
.\docs\02-一期底座需求规格与待决策台账.md:513:| 2026-09-08 | V0.7 | 增加每轮模块分析必须记录众墅需求差距与细化任务的规则，关联 05 清单并扩充文档检查契约；保留所有需求实现状态和 D-07/D-09/D-10/D-11 门禁 |
.\docs\02-一期底座需求规格与待决策台账.md:515:| 2026-09-08 | V0.9 | 同步 M04～M12 剩余模块的 45 项细化任务；全 12 模块累计 84 项，保留 D-07/D-09 待决、D-10/D-11 后置及全部分层验收状态 |
.\docs\02-一期底座需求规格与待决策台账.md:518:| 2026-09-10 | V0.12 | 用户拍板 D-09（一个 Account 多 Membership、用户名全平台唯一、手机/邮箱全局唯一、trim+小写、逻辑删除可重建、跨组织仅限显式平台角色）为已确认，PEND-002/003 状态更新、FND-IAM 门禁解除进入阶段 1B；D-07 标为方向已采纳、细节待阶段 2 前确认（PEND-001）；verify-docs 门禁将 D-09 移出未确认守护列表；未提升任何实现验收状态 |
.\README.md:11:- 方向已采纳：D-07“加盟商开通—首条线索闭环”薄链（PILOT-REQ 字段/状态/转商机条件细节待阶段 2 前确认）
.\README.md:64:本轮没有实施真实 PostgreSQL、全平台身份、多组织权限、业务链、微信支付、AI Provider 或部署。D-09 已于 2026-09-10 确认最小模型（决策门禁解除，实现待阶段 1B）；D-07 方向已采纳、细节待阶段 2 前确认；D-10、D-11 延后。
.\README.md:95:## 文档同步规则
.\docs\01-底座代码复用与改造方案.md:6:> 状态：D-01～D-06、D-08、D-09 已确认（D-09 于 2026-09-10 确认最小模型）；D-07 方向已采纳、细节待阶段 2 前确认；D-10、D-11 按阶段跟踪  
.\docs\01-底座代码复用与改造方案.md:12:2026-09-08 模块分析进展：M01～M12 已完成关键入口与需求差距的静态分析；评审与新增品牌专项后累计 91 项主任务（83 待开发、2 待决策、6 待前置），其中 25 项拆为 57 个不重复计数的分批子项。复用 System/Infra、会话、权限、存储、日志、消息、调度、Flowable 和两个前端壳；补齐实际 PG/Web 基础管理回归，并明确上传确认后的资产不可变性、主体绑定下载及撤权/续传边界。依赖按早期交付和后续扩展分别验收，组织、首链和外部接入仍遵守 D-07/D-09/D-10/D-11；本轮仅修订文档，不提升运行验收状态。
.\docs\01-底座代码复用与改造方案.md:632:- D-07 确认前只允许编写需求与验收草案，不得固化业务字段和状态；
.\docs\01-底座代码复用与改造方案.md:634:- 建议采用“加盟商开通 → 负责人/员工授权 → 首条线索分配 → 跟进 → 转商机或关闭”的薄纵向链；最终范围仍需确认 D-07；
.\docs\01-底座代码复用与改造方案.md:655:- 众墅之家设计小程序 V1.2 的候选后端合同见 [设计小程序后端技术架构](../../../企业FDE项目/众墅之家设计小程序/03-众墅之家设计小程序后端技术架构-V1.0.md)，工作包见 [后端实施蓝图](../../../企业FDE项目/众墅之家设计小程序/众墅之家设计小程序后端实施蓝图-V1.0.md)；两份文件均为 `PROPOSED`，必须遵守 D-04。阶段 0、1A、1B、阶段 2/D-07 与 D-09 通过后，才可进入本专项承担的阶段 3 领域/Stub 实施；D-10 只解锁真实微信/支付，D-11 只解锁真实 AI Provider。
.\docs\01-底座代码复用与改造方案.md:702:| D-07 | 建议方案 | 推荐“加盟商开通—首条线索闭环”；需确认角色、状态、结束条件及明确排除项 | 首个业务纵向切片开发前 |
.\docs\01-底座代码复用与改造方案.md:734:| 2026-08-29 | V0.2 | 同步 D-01～D-06 已确认；将 D-08 更新为 PostgreSQL 唯一数据库基线；加入 D-07 薄纵向链建议、D-09～D-11 状态、PostgreSQL 适配门禁及一期需求主台账链接 |
.\docs\01-底座代码复用与改造方案.md:735:| 2026-08-29 | V0.3 | 将多身份改为 D-09 条件式候选设计；同步阶段 1A/1B 门禁、D-07 可追踪验收草案，并明确阶段 4 为一期外范围 |
.\docs\01-底座代码复用与改造方案.md:738:| 2026-09-08 | V0.6 | 刷新 Git/源码路径和 JDK 17 专项供体现状，登记 24 个开发工作包与多端架构；保留 D-07/D-09/D-10/D-11 门禁，不提升产品功能状态 |
.\docs\01-底座代码复用与改造方案.md:745:| 2026-09-10 | V0.13 | 用户拍板 D-09 已确认（一个 Account 多 Membership、账号唯一性细则），更新状态行与 §4.3 供体身份表述；D-07 方向已采纳、细节待阶段 2 前确认；同步 docs/02 §3、docs/07 §4 与 verify-docs 门禁；不提升运行验收状态 |
.\docs\03-底座二次开发顺序与验收标准.md:6:> 决策边界：JDK 17、PostgreSQL、模块化单体、Flowable、Vue3 Web、Admin UniApp 不变；D-09 已确认（2026-09-10），D-07 方向已采纳、细节待阶段 2 前确认，D-10、D-11 后置\
.\docs\03-底座二次开发顺序与验收标准.md:35:7. D-07 方向已采纳、细节待阶段 2 前确认；确认前只定义首链接入规范，不把“加盟商开通—首条线索闭环”的字段/状态写成批准范围。
.\docs\03-底座二次开发顺序与验收标准.md:57:| WP-16 审批状态 | BPM/Flowable | 资格、组织、版本、撤回/驳回/转办、幂等写回 | D-07 首链状态机 | B09 |
.\docs\03-底座二次开发顺序与验收标准.md:58:| WP-17 对象规范 | DO、审计字段、校验、代码生成 | 版本并发、引用和责任历史规范 | D-07 首链业务对象 | B09 |
.\docs\03-底座二次开发顺序与验收标准.md:168:### B09：D-07 首条薄链、Flowable 和领域状态
.\docs\03-底座二次开发顺序与验收标准.md:170:- 前置：B05、B08；必须先批准 D-07 的对象、角色、状态、异常和停止条件。
.\docs\03-底座二次开发顺序与验收标准.md:171:- 修改：`zszj-module-bpm/**`、首链业务模块目录（由 D-07 命名）、对应 Web/UniApp 页面与迁移；不按部门复制 System/Infra。
.\docs\03-底座二次开发顺序与验收标准.md:223:下一阶段仍按本文批次推进：B01/B02 工程与 PG 基线 → B03 登录/API/安全合同 → B04 文件审计、B05 事件待办、B06 两端闭环。D-09 后进入 B07/B08，D-07 后以 B09/B10 首链和真实接入验证；B11 后置能力单独验收。技术测试准备可以拆项先行，但不绕过批次依赖或把移动构建当成真机发布证据。05 的分析模块编号不是替代 WP-01～24 或 B00～B11 的新开发顺序。
.\docs\03-底座二次开发顺序与验收标准.md:250:| 2026-09-10 | V1.6 | 用户拍板 D-09 已确认，更新决策边界与第 2 节实施纪律（B07 前不提前创建组织/任职表）；D-07 方向已采纳、细节待阶段 2 前确认；同步 docs/02/docs/05/docs/07 与 verify-docs 门禁；批次依赖与验收门禁不变 |
.\docs\05-底座模块分析与开发任务清单.md:20:- D-07、D-09 仍待决；D-10、D-11 仍后置。账号/身份/组织规则不得借数据库技术改造提前固化；本清单不授权连接或改动生产数据库。
.\docs\05-底座模块分析与开发任务清单.md:39:| M11 Flowable 与业务接入规范 | 已完成静态分析，已登记 | ZS-BPM-001～004，共 4 项；正式首链受 D-07/D-09 门禁 |
.\docs\05-底座模块分析与开发任务清单.md:49:D-09 已于 2026-09-10 确认：ZS-DB-009、ZS-IAM-001 决策包已形成书面结论（docs/02 §3、docs/07 §4），由待决策转待验收；ZS-DB-010 账号唯一性迁移的决策前置已解除，可排期（仍须遵守 B07 批次前置）。待前置：ZS-DB-010、ZS-IAM-002/004、ZS-PERM-003、ZS-BPM-003、ZS-OPS-003（其中 ZS-BPM-003/ZS-OPS-003 前置 D-07 细节确认）。其余任务中的“待开发”表示具备技术拆解入口，仍须遵守各卡片的批次前置；它们并非可同时立即开工。
.\docs\05-底座模块分析与开发任务清单.md:59:V1.6 变更与验证记录（2026-09-09）：M01 工程骨架六项全部完成（ZS-ENG-001 白名单 295040b3、002 JDK17 基线 f58fd09b、003 环境配置分离 38430267、004 关闭模拟认证 77b5f090、005 收紧管理端点 7c8fd2c0、006 任务启停边界 4557cc00）；M02 推进七项——ZS-DB-001.A PG 驱动与数据源合同（1f2eb7b6）、ZS-DB-003 Flyway 迁移机制（9106476f）、ZS-DB-004 V1 基线迁移与真实 PG 双库验证（391faeef）、ZS-DB-005 升级与恢复规程含 V2 qrtz 回填与三段演练（f7702928）、ZS-DB-002 环境方案与 PEND-004 清单（99e1a93c）、ZS-DB-019.A Docker 夹具自证三条语义（3a0d5d04）；另完成 ZS-CFG-001.A 秘密门禁（ebc7c9ba）、ZS-CFG-003.A 功能目录与套餐校验（2160d600）、ZS-CLIENT-005.A 构建与类型基线门禁（65272cdf）、ZS-GOV-001 文档一致性检查器（1d618920）、ZS-OPS-001.A 聚合流水线骨架（bda57b95）。持续防线：`node scripts/ops/run-local-gates.mjs` 10/10 通过（本地与 CI 同规则）。剩余 66 项待开发任务的推进前提：B02 环境（JDK 17 + Maven ≥3.8 + PostgreSQL 17，按数据库环境方案落地）解锁 M02 真实库验证与 ZS-OPS-001.B；D-07/D-09 决策解锁 M05/M06/B07~B09；B03~B06 联验环境解锁 M03/M04/M08~M12 运行验证；D-10/D-11 与终端范围解锁微信/支付/AI/App。本记录不表示任何主任务已验收。
.\docs\05-底座模块分析与开发任务清单.md:289:- 运行注意事项（2026-09-13，ZS-DB-019.B 本地复跑登记）：`run-pg-regression.mjs` 8 套件在本机与 Maven 构建/codex 评审并行高负载下偶发单套件失败（三次复跑失败套件各不相同：DB-008/011~015 → DB-017 → DB-006，报错均为容器启动/CREATE DATABASE 竞态），各套件单独运行均绿、空载聚合复跑 8/8 全绿——属环境负载性 flaky 非套件缺陷；CI 与收口验证建议空载执行或失败重试一次后再判定。
.\docs\05-底座模块分析与开发任务清单.md:292:- 开发记录（2026-09-13，ZS-DB-019.B 本地收口补丁）：①**实跑全绿**——当前 main（收口补丁后 `c50c6bf6`）空载复跑 `node scripts/db/run-pg-regression.mjs` **8 套件全绿/退出 0**（`outputs/db019b-docsync-retry.log`；同日首轮在机外 Docker 负载[病毒视频处理容器并行]下 3 套件 CREATE DATABASE 竞态失败 `outputs/db019b-docsync.log`，重试即全绿且失败集合与 09-13 既有登记各不相同，按已登记 flaky 处置约定「重试一次后再判定」确认为环境负载性、非套件缺陷）；②**H2→PG 方言回归决策交付**——[H2与PG方言回归决策](../services/zhongshu-core/docs/H2与PG方言回归决策.md)：H2 保留为快速反馈层永不作数据库门禁、PG 回归以 run-pg-regression.mjs 为唯一持续入口（本地 Docker PG17 与 CI pg-regression.yml 同入口、失败非零、缺 Docker 退出码 3 不静默回退——三条语义由 ZS-DB-019.A 夹具 `--self-test` 自证）、方言敏感变更（迁移 SQL/分批删除/行锁并发/序列/JSON/Quartz/元数据）准入必须同步补 PG 用例、纯业务逻辑保留 H2/Mockito，并登记 H2≠PG 六类已知差异守护矩阵；③至此 §16.1 ZS-DB-019.B「PG 技术回归与持续执行入口」的本地先行件闭环。**待验收说明**：父卡维持「开发中」——.A 完整验收（Maven 测试链接入）依赖 ZS-DB-001.B 真实连接与 B02 正式环境授权，按 16.1 前置另行收口；真集群/正式环境 PG 的授权、备份恢复与容量证据归 B02 环境批次。本记录不表示任何主任务已验收。
.\docs\05-底座模块分析与开发任务清单.md:345:- 开发记录（2026-09-10，ZS-SEC-003 规范 Token 传输与特殊连接凭据）：按「固定 Authorization 严格解析 + 重复/冲突拒绝 + URL 参数通道开关」非破坏形态交付。核心改造 [SecurityFrameworkUtils][E31] `obtainAuthorization`：（1）**固定 Authorization 解析**——以 `regionMatches(true,0,"Bearer",…)` 大小写不敏感匹配前缀且要求空白分隔，替代旧 `indexOf("Bearer ")` 任意位置子串查找 + magic `substring(+7)`，修复 `"xBearer y"` 被误解析为 `"y"`（回归护栏用例锁定）；`"Bearer"` 单独出现、`"BearerXyz"` 无空白分隔、剥离后为空均判畸形返回 null；无前缀时按裸 Token 兼容既有客户端。（2）**重复/冲突值处理和拒绝规则**——新增 `distinctNonEmptyValues`（trim + 去空 + `LinkedHashSet` 去重）归一同来源多值，Header 或 Parameter 出现多个不同值视为歧义（凭据走私）拒绝返回 null、多个相同值归一为一个。（3）**共享/部署环境禁止普通长效凭据进入 URL**——新增四参重载 `obtainAuthorization(request,headerName,parameterName,parameterEnabled)`，旧三参重载委托 `parameterEnabled=true` 向后兼容；Header 优先，仅当 Header 缺失且 `parameterEnabled=true` 时回退 URL 参数，保留 WebSocket `/ws?token=` 等无法设置 Header 的获准连接、不直接删除全部参数支持而破坏批准能力。新增配置 `SecurityProperties.tokenParameterEnabled`（Boolean 默认 true、@NotNull）：默认保留 WebSocket 连接能力，共享/部署环境可置 false 从服务端禁止长效凭据进入 URL，规避访问日志、浏览器历史、Referer、代理留存导致的泄露。三处调用方（[Token 过滤器][E30] `doFilterInternal`、`AuthController` admin 登出、`AppAuthController` app 登出）统一传入 `securityProperties.getTokenParameterEnabled()`。**前端、代理和日志同步**：两端前端实证零改动兼容——Web `service.ts` axios、小程序 `interceptor.ts`/`sse.ts`/`download.ts` 一律 `Authorization: Bearer ${token}` Header 传输，无一走 URL 参数，严格前缀解析不破坏双端；WebSocket 是唯一 `?token=` 通道（`WebSocketProperties.path=/ws`、`LoginUserHandshakeInterceptor` 靠 getLoginUser 判定握手），由默认 true 保留；日志脱敏由 ZS-SEC-007 覆盖。**测试**：新增 `SecurityFrameworkUtilsTest`（security starter `src/test`，`MockHttpServletRequest` 纯单测无 DB/Redis）18 用例——标准 Bearer、大小写不敏感、裸 Token 兼容、`xBearer y` 子串误解析回归护栏、畸形（`Bearer `/`Bearer`/`BearerXyz`）、缺失、多空白 trim、参数回退、参数带 Bearer、参数通道关闭、Header 优先、重复头冲突/相同、重复参数冲突、空 Header 回退参数、parameterName 空忽略；security starter pom 补 spring-boot-starter-test + mockito-inline（test scope）。**文档交付物**：登记[Token 传输与连接凭据规范](../services/zhongshu-core/docs/Token传输与连接凭据规范.md)（现状缺陷、三解析规则、参数开关语义、三调用方矩阵、两端前端证据、验收对齐、测试矩阵、边界）。**验证**：`mvn -pl :zszj-spring-boot-starter-security -am test` BUILD SUCCESS（`SecurityFrameworkUtilsTest` 18/18，上游 common 8/web 全绿）；`AuthController`（`mvn -pl :zszj-module-system -am -DskipTests compile`）、`AppAuthController`（`mvn -f zszj-module-member/pom.xml -DskipTests compile`，member 模块在根 pom 注释排除、不在 reactor，先 install security starter 刷新本地仓库后独立编译）均 BUILD SUCCESS；`node scripts/ops/run-local-gates.mjs --fast` 10/10。**对齐验收（本卡验收项）**：缺失、畸形、失效、重复及错用户类型凭据结果一致（缺失、畸形三类、重复冲突均统一返回 null，交 [Token 过滤器][E30] 走既有认证失败链，与失效/错用户类型 401/403 结果一致）；URL 不泄露正常会话凭据（`tokenParameterEnabled=false` 服务端禁参数通道 + 两端前端本就走 Header）；合法双端请求和获准特殊连接通过（标准 Bearer/裸 Token/大小写/参数回退/WebSocket 默认 true 均放行）。**待验收说明**：本项完成 B03 普通 API 凭据传输规范化与获准连接合同；文件专用短时票据与取流归 ZS-FILE-004.A 在 B04，票据过期/重用/错用途失败属文件票据语义同归该项；SSE/下载两端均走 Header 不受参数开关影响；`tokenParameterEnabled=false` 生产硬化开关、真实代理/日志同步与 WebSocket 端到端连接在真实环境验收归 ZS-SEC-012.B；OAuth2 开放端点凭据（client_secret/basic）传输审查承接 ZS-SEC-002 登记的残余审查点，随 OAuth2 领域开发落地。主卡状态 待开发→待验收。
.\docs\05-底座模块分析与开发任务清单.md:671:- 关联：FND-INF-002、FND-BPM-001；WP-15；B05/B09。优先级 P0；类别 补建；状态 待开发；前置 ZS-MSG-001；首链规则待 D-07。
.\docs\05-底座模块分析与开发任务清单.md:674:- 验收：阅读消息不会完成业务任务；业务完成、撤回与转派更新正确；重复/乱序事件不复活已失效待办；首链启用前按 D-07 验证状态映射。
.\docs\05-底座模块分析与开发任务清单.md:713:- 关联：FND-INF-002/003、FND-BPM-002；WP-14；B05/B09。优先级 P0；类别 补建；状态 待开发；前置 ZS-JOB-002、ZS-SEC-011.A；领域规则待 D-07。
.\docs\05-底座模块分析与开发任务清单.md:729:D-06 已确认使用 Flowable，不代表已通过 PG 验收。BPM 是审批路由和任务引擎，不能成为众墅业务对象的状态数据库。技术合同、隔离测试与中性测试流程可以先准备，正式首链对象、角色、状态、指标口径及异常路径仍依赖 D-07/D-09。
.\docs\05-底座模块分析与开发任务清单.md:731:### ZS-BPM-001：建立 BPM 独立装配与 PostgreSQL 验收
.\docs\05-底座模块分析与开发任务清单.md:736:- 验收：PG 上部署、发起、通过、拒绝、撤回、转办和分页/历史查询通过；重复部署、重启、事务失败可恢复；关闭 BPM 时无流程路由/后台副作用；正式首链另受 D-07 门禁。
.\docs\05-底座模块分析与开发任务清单.md:740:- 关联：FND-BPM-003、FND-AUTH-004/008；WP-07/16；B09（B08 提供权限策略）。优先级 P0；类别 改造；状态 待开发；前置 ZS-BPM-001、ZS-PERM-002.B；任职资格待 D-09。
.\docs\05-底座模块分析与开发任务清单.md:744:- 放行边界：B08 只交付本项消费的组织/对象权限策略；实际 Flowable 审批资格在 B09 与 ZS-BPM-001 联验，B08 不等待流程运行验收。
.\docs\05-底座模块分析与开发任务清单.md:748:- 关联：FND-BPM-001/002；WP-16/17/22；B09。优先级 P0；类别 改造；状态 待前置；前置 D-07、D-09，B05/B08。
.\docs\05-底座模块分析与开发任务清单.md:750:- 调整：先由 D-07 明确首链权威对象、角色、状态/异常/停止条件及指标事实，再实现领域状态机、版本条件更新、流程实例绑定、原因审计和幂等命令；流程结果转成领域动作，不直接复制流程枚举；指标从领域事实派生。
.\docs\05-底座模块分析与开发任务清单.md:751:- 验收：首链正常通过/拒绝/撤回/转派与业务状态一致；重复回调、旧流程晚到、并发审批/撤回不重复建对象或覆盖新版本；失败可回查补偿，指标口径和来源可追溯；D-07 未批准前不提交业务模型。
.\docs\05-底座模块分析与开发任务清单.md:755:- 关联：FND-ARCH-002、FND-BPM-001/002、FND-AUTH-004/007；WP-14/16/17/23；B09/B10。优先级 P1；类别 补建；状态 待开发；前置 B03～B05 技术合同；真实业务适配待 D-07/D-09。
.\docs\05-底座模块分析与开发任务清单.md:810:- 调整：建立固定提交的构建、单元、PG/Redis、双技术租户/组织、存储、流程、多端及文档检查流水线；各层先有本地入口再接 CI，失败阻止对应批次放行；保留跳过原因、测试数据隔离和来源差异追踪，文档规则实现归 ZS-GOV-001。
.\docs\05-底座模块分析与开发任务清单.md:813:- 开发记录（2026-09-09，ZS-OPS-001.A）：交付聚合门禁入口 `node scripts/ops/run-local-gates.mjs`（--fast 跳过慢速项）：聚合来源复制校验单测、品牌命名门禁、文档一致性、模块白名单、数据源 PG 合同、Flyway 规范、配置秘密、Web 类型基线共 10 项，任一失败阻止放行；新增 [.github/workflows/local-baseline-gates.yml](../.github/workflows/local-baseline-gates.yml) 以同一入口（--fast）运行，实现"本地与 CI 同规则"。评审修正：来源复制校验器的可执行位用例在 Windows 无 chmod 语义导致误失败，改为 POSIX 条件执行（CI ubuntu 仍覆盖）；README 门禁章节写入损坏已重建。验证：聚合门禁全量 10/10 通过。待验收说明：.B~.E（PG/安全/多端/流程层流水线）按各批前置接入，真实 CI 在固定提交通过前不登记 CI_VERIFIED。
.\docs\05-底座模块分析与开发任务清单.md:827:- 关联：FND-INF-004/005、FND-ARCH-002；WP-21/23/24；B11（B10 接入合同归 ZS-BPM-004）。优先级 P1；类别 验证适配；状态 待前置；前置 D-07/D-09 后确认实际接入；微信/支付待 D-10，AI 待 D-11，App 发布需明确交付范围。
.\docs\05-底座模块分析与开发任务清单.md:893:- 开发记录（2026-09-08）：提交 43d71fad，10,059 文件。目录/包/类名以 git mv 保留历史（22 个顶层模块、framework 15 个子模块、67 个 Java 源码根、39 个 Yudao*→Zszj* 类）；配置键、Spring 应用名、环境变量（ZS_*→ZSZJ_*）、Dockerfile/部署脚本、接口文档标题同步。静态一致性检查 `node scripts/brand/verify-backend-naming.mjs` 通过：6,763 个 Java 包声明与路径一致，imports/spring.factories/Mapper XML 的 cn.zszj FQCN 全部可解析，POM 坐标一致，硬性问题 0。评审修正随 581927d0/b2c26ea9 落地：恢复被误改的上游署名链接（github.com/YunaiV/*、gitee.com/zhijiantianya/*）并补保护规则。未验证项（如实登记）：干净构建、依赖树与真实 PG 启动待 ZS-ENG-002 工具链（本机无 JDK/Maven）在 B01/B02 补证。
.\docs\05-底座模块分析与开发任务清单.md:937:- 开发记录（2026-09-09）：交付 `scripts/gov/verify-docs.mjs` + 7 项测试（verify-docs.test.mjs）。规则：R1 仓库内链接有效性（仓库外/绝对路径引用降级为提示）、R2 任务编号唯一、R3 状态枚举合法、R4 未确认决策（D-07/D-09/D-10/D-11）不得描述为既成事实（否定句式豁免）、R5 README 索引版本与文档头一致。接入即发现并修复真实问题：05 文档中指向供体快照的 6 条链接被品牌改名批次误改（供体保持旧路径，已回修）、logo.svg 替换后的死链。验证：当前文档 0 违规；故意注入断链/重复编号/非法状态/未确认决策既成事实化/版本不一致五类破坏全部失败。待验收说明：真实 CI 接入与 CI_VERIFIED 登记归 ZS-OPS-001（固定提交通过前不登记）。
.\docs\05-底座模块分析与开发任务清单.md:949:7. D-09 批准后完成 B07 组织任职与 B08 对象/动作/字段授权；D-07 批准后进入 B09 首链/Flowable，B10 做真实系统接入与角色 UAT。B11 的微信/支付、AI、App 发布和容量按各自门禁启用。
.\docs\05-底座模块分析与开发任务清单.md:1006:| ZS-OPS-001.A | B01 | ZS-GOV-001、ZS-CLIENT-005.A | 基础构建/文档流水线骨架 | 本地与 CI 同规则，坏链接/构建失败阻止放行；不等待 PG/多端 E2E |
.\docs\05-底座模块分析与开发任务清单.md:1010:| ZS-OPS-001.E | B09 | ZS-OPS-001.D、ZS-BPM-001、ZS-BPM-002、ZS-BPM-003 | 获批首链/流程及组织权限门禁 | PG 流程、幂等写回和跨组织反向通过；D-07/D-09 为前置 |
.\docs\05-底座模块分析与开发任务清单.md:1024:未拆分任务仍按主卡执行；跨批次补充不得被解释为早期整卡前置：ZS-ENG-004/005/006 在 B01 完成开关/暴露检查，B03/B05 的深测分别归安全/任务卡；ZS-DB-005/014/015/018/020 在 B02 首验，会话、组织唯一性和领域并发分别由 LOGIN、ZS-DB-010/IAM/PERM、BPM 承接；ZS-SEC-003 的文件交付归 ZS-FILE-004.A。ZS-LOGIN-003 与 ZS-IAM-003 的技术生命周期在 B03 验，任职扩展归 ZS-IAM-002/004。ZS-CFG-004 在 B03 验值校验、B04 复验审计；ZS-AUDIT-001 在 B04 使用同库审计，不先依赖 B05。ZS-MSG-001/002/004 与 ZS-JOB-003 的技术闭环在 B05 验，任职/领域/真实渠道在获批时分别复验；ZS-BPM-002 的实际流程资格在 B09，不阻塞 B08。ZS-CLIENT-003 的登录请求合同在 B03 验，B06 对新页面/附件再次回归。ZS-GOV-001 在 B00/B01 建立本地与文档 CI 规则，后续仅扩充覆盖；ZS-OPS-003 不作为 B01～B10 的整体验收前置，各后置能力独立解锁。
.\docs\05-底座模块分析与开发任务清单.md:1117:| 2026-09-08 | V1.2 | 完成剩余 M04～M12 共 9 个模块静态分析，新增 45 项；12 个模块累计 84 项（76 待开发、2 待决策、6 待前置）；补齐逐项证据、调整、前置与正反向验收，保留 D-07/D-09/D-10/D-11 门禁；本轮只更新文档 |
.\docs\05-底座模块分析与开发任务清单.md:1129:| 2026-09-10 | V1.14 | D-09 用户拍板确认（一个 Account 多 Membership、用户名全平台唯一、手机/邮箱全局唯一、trim+小写、逻辑删除可重建、跨组织仅限显式平台角色）：ZS-DB-009、ZS-IAM-001 决策包由待决策转待验收，第 2 节统计 2 待决策→0、29 待验收→31；M05 说明与 ZS-DB-010 前置门禁更新；D-07 方向已采纳（细节待阶段 2 前）；同步 verify-docs 将 D-09 移出未确认守护列表；纯决策落库，未提升任何实现验收状态或改动运行代码 |
.\docs\05-底座模块分析与开发任务清单.md:1145:| 2026-09-13 | V1.30 | ZS-CFG-004（补齐配置值校验、变更审计和恢复流程，B03 部分交付值校验；前置 ZS-CFG-001.B、ZS-CFG-002.B、ZS-DB-003 均已完成）经 worktree 隔离分支 `feat/cfg-004`（impl `38dde380`）`--no-ff` 合并 main（`e218fe04`）：新增 ConfigParamCatalog 参数目录（6 核心参数值合同 + wired 预留标注 + url.druid 热生效修正）与 ConfigValueValidator 值校验（类型/范围/枚举强校验、错误码 CONFIG_VALUE_TYPE_MISMATCH/OUT_OF_RANGE/NOT_IN_ALLOWED_SET；掩码回显改 key 时按目标 key 重校验堵旁路）；乐观锁并发冲突以**独立整数 version 列**交付（迁移 `V20260913.001__infra_config_optimistic_version.sql` 幂等，详情返回 version、更新必须回传、快照==请求才递增、条件 UPDATE 兜底，冲突抛 `CONFIG_UPDATE_CONFLICT`）。**codex 评审弧（gpt-6-astra/xhigh，r0→r4 五弧）**：r0 FAIL 1×P1（乐观锁仅服务端短窗、旧表单覆盖不可检测，H2 实证）→ r1 初版回传 updateTime 作版本 FAIL 2×P1（JSON 毫秒截断误拒 + datetime 秒级精度版本不推进）+1×P2（MapStruct 可伪造初始版本）→ **r2 改型整数 version 列**（创建置 0/更新原子 +1）→ r3 FAIL 1×P2（快照/请求版本未比对一致可致 7→8 复用不推进）→ r4 修复后 **PASS/0 发现**。验证：TDD RED 2 用例 → GREEN 50 tests/0 failures BUILD SUCCESS。卡片 待开发→待验收（B03 部分）；变更审计与恢复流程归 B04 复验、前端 version 契约联调归 Web E2E、真实 PG/API 归 ZS-SYS-001.A。另登记 ZS-DB-019.B 运行注意事项：run-pg-regression.mjs 8 套件在本机与其他构建/评审并行高负载下偶发单套件 Docker 容器启动竞态失败（三次复跑失败套件各不相同、套件单独运行均绿、空载聚合复跑 8/8 全绿），属环境负载性 flaky 非套件缺陷，CI 与收口验证建议空载或重试一次。本记录不表示任何主任务已验收。处置详见 [codex-ZS-CFG-004.md](reviews/codex-ZS-CFG-004.md)。README 索引版本同步 V1.30 |
.\docs\05-底座模块分析与开发任务清单.md:1153:| 2026-09-13 | V1.37 | ZS-DB-019.B 本地收口补丁（PG 技术回归与持续执行入口，B02 尾巴；前置 ZS-DB-019.A 及 DB-005~008/011~018/020 均已交付待验收）：①当前 main（`c50c6bf6`）空载复跑 `run-pg-regression.mjs` **8 套件全绿/退出 0**（首轮机外 Docker 负载下 3 套件 CREATE DATABASE 竞态失败、重试即全绿且失败集合与既有登记各不相同，按已登记 flaky 处置约定确认为环境负载性）；②交付 [H2与PG方言回归决策](../services/zhongshu-core/docs/H2与PG方言回归决策.md)——H2 保留快速反馈层永不作数据库门禁、PG 回归以 run-pg-regression.mjs 为唯一持续入口（本地 Docker PG17 与 CI pg-regression.yml 同入口、失败非零、缺 Docker 退出码 3 不静默回退，三条语义由 019.A 夹具 self-test 自证）、方言敏感变更准入必须同步补 PG 用例、纯业务逻辑保留 H2/Mockito，附 H2≠PG 六类已知差异守护矩阵；③§16.1 ZS-DB-019.B「PG 技术回归与持续执行入口」本地先行件闭环。父卡维持**开发中**（.A 完整验收依赖 ZS-DB-001.B 真实连接与 B02 正式环境授权，按 16.1 另行收口）。本记录不表示任何主任务已验收。README 索引版本同步 V1.37 |
.\docs\04-源码迁入与验证报告.md:96:| B09 | D-07 后首条业务薄链、Flowable 与领域状态 | 入口→处理→审批→交接→通知→下一动作闭环；重复回调、并发版本与非法状态测试通过 |
.\docs\04-源码迁入与验证报告.md:104:本轮是独立产品仓库中的源码迁入：原仓库不变，没有数据库迁移、生产配置或外部资源变更。回退只涉及本分支新文件及文档，不需要数据库回滚；回退时应按具体提交或明确清单处理，不能清空项目根目录。已有方案和需求定义保持，D-07、D-09、D-10、D-11 未被自动批准。
.\docs\reviews\codex-hotfix-C-r1.raw.md:596:鉁?P3锛歱lanScope 鎺ュ彈鏃犲熬鍒嗛殧绗︾殑鑷畾涔夋牴锛坧ath.join 鑰岄潪瀛楃涓叉嫾鎺ワ級 (146.5318ms)
.\docs\reviews\codex-hotfix-B.raw.md:91:- 寤鸿寰呯‘璁わ細D-07鈥滃姞鐩熷晢寮€閫氣€旈鏉＄嚎绱㈤棴鐜€?
.\docs\reviews\codex-hotfix-B-r1.raw.md:2477: 17.5523 20.5521 18 19.9998 18H16.9998V20.9991C16.9998 21.5519 16.5499 22 15.993 22H4.00666C3.45059 22 3 21.5554 3 20.9
.\docs\reviews\codex-hotfix-B-r1.raw.md:2481: 8 2H16C16.5523 2 17 2.44772 17 3V6ZM18 8H6V20H18V8ZM9 11H11V17H9V11ZM13 11H15V17H13V11ZM9 4V6H15V4H9Z"></path></svg>')
.\docs\reviews\codex-hotfix-B-r1.raw.md:2621: 17H15V12.7519L16.5497 12.0881L15.0072 9.66262L14.9501 9.22118C14.5665 6.25141 12.0243 4 9 4ZM19.4893 16.9929L21.1535 1
.\docs\reviews\codex-hotfix-B-r1.raw.md:2720:34 6.84992 9.09302C6.74442 8.8672 6.74488 8.55621 6.74529 8.22764C6.74529 7.8112 6.74529 7.34029 6.54129 6.88256C6.4624
.\docs\reviews\codex-hotfix-B-r1.raw.md:2721:6 6.70541 6.35689 6.56446 6.23509 6.45329ZM12 22C6.47715 22 2 17.5228 2 12C2 6.47715 6.47715 2 12 2C17.5228 2 22 6.4771
.\docs\reviews\codex-hotfix-B-r1.raw.md:2778:"><path d="M15.5 5C13.567 5 12 6.567 12 8.5C12 10.433 13.567 12 15.5 12C17.433 12 19 10.433 19 8.5C19 6.567 17.433 5 15
.\docs\reviews\codex-hotfix-B-r1.raw.md:2914:H11.0007L11.0017 17H15V12.7519L16.5497 12.0881L15.0072 9.66262L14.9501 9.22118C14.5665 6.25141 12.0243 4 9 4ZM19.4893 1
.\docs\reviews\codex-hotfix-B-r1.raw.md:2918:="M15.5 5C13.567 5 12 6.567 12 8.5C12 10.433 13.567 12 15.5 12C17.433 12 19 10.433 19 8.5C19 6.567 17.433 5 15.5 5ZM10 
.\docs\reviews\codex-hotfix-B-r1.raw.md:2938:.74529 7.8112 6.74529 7.34029 6.54129 6.88256C6.46246 6.70541 6.35689 6.56446 6.23509 6.45329ZM12 22C6.47715 22 2 17.52
.\apps\zhongshu-miniapp\docs\index.md:46:  - icon: <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 32 32"><g stroke-width=".13"><path fill="#858585" d="M17.007 23.491a6.52 6.52 0 1 1 13.04 0a6.52 6.52 0 0 1-13.04 0"/><path fill="#ccc" d="M17.007 8.51a6.52 6.52 0 0 1 13.04 0v5.867c0 .36-.292.652-.652.652H17.659a.652.652 0 0 1-.652-.652z"/><path fill="#4d4d4d" d="M14.993 23.491a6.52 6.52 0 1 1-13.04 0v-5.868c0-.36.292-.652.652-.652h11.736c.36 0 .652.292.652.652z"/></g></svg>
.\apps\zhongshu-miniapp\docs\index.md:63:  # - icon: <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" fill="none" version="1.1" width="284.0000305175781" height="284" viewBox="0 0 300 300"><defs><linearGradient x1="0.8546710014343262" y1="0.8034360408782959" x2="0.8333213329315186" y2="0" id="master_svg0_132_03714"><stop offset="0%" stop-color="#FFB74D" stop-opacity="1"/><stop offset="100.00011920928955%" stop-color="#EA5F2A" stop-opacity="1"/></linearGradient><linearGradient x1="0.5" y1="0.6703797578811646" x2="0.29553526639938354" y2="-0.4002380669116974" id="master_svg1_132_13164"><stop offset="0%" stop-color="#FFB74D" stop-opacity="1"/><stop offset="100.00028610229492%" stop-color="#CB3800" stop-opacity="1"/></linearGradient><linearGradient x1="0.9371286034584045" y1="0" x2="0.09386466443538666" y2="0.9283419847488403" id="master_svg2_132_13158"><stop offset="0%" stop-color="#FF500F" stop-opacity="1"/><stop offset="99.9998927116394%" stop-color="#D9773B" stop-opacity="1"/></linearGradient><linearGradient x1="0.8452901840209961" y1="0.8194817900657654" x2="0.8502801060676575" y2="0.059159498661756516" id="master_svg3_132_14394"><stop offset="0%" stop-color="#00B2ED" stop-opacity="1"/><stop offset="100.00021457672119%" stop-color="#002FED" stop-opacity="1"/></linearGradient><linearGradient x1="0.5" y1="0" x2="0.5" y2="1" id="master_svg4_132_18996"><stop offset="0%" stop-color="#143AD4" stop-opacity="1"/><stop offset="69.9999988079071%" stop-color="#00B2ED" stop-opacity="1"/></linearGradient><linearGradient x1="0.3482012450695038" y1="1.2638264894485474" x2="0.448713093996048" y2="0.15451215207576752" id="master_svg5_132_22620"><stop offset="0%" stop-color="#0267E4" stop-opacity="1"/><stop offset="100%" stop-color="#3054E7" stop-opacity="1"/></linearGradient></defs><g><g><g><path d="M113.743,60.721448861694334L57.7072,132.42234886169433C54.9276,135.97934886169435,54.9144,140.92834886169433,57.675,144.49934886169433L61.3262,149.22234886169434C65.3681,154.45034886169432,73.3605,154.44934886169435,77.4007,149.21934886169433L106.601,111.42234886169433L106.601,180.64534886169434C106.601,199.51334886169434,82.3918,207.76434886169434,70.5352,192.93834886169432L27.9967,139.74534886169434L122.896,21.075048861694334C126.346,16.762048861694335,125.589,10.513938861694337,121.207,7.119458861694336L117.239,4.046338861694336C112.857,0.6518588616943359,106.508,1.396456861694336,103.059,5.709438861694336L3.24421,130.52534886169434Q4.46644e-13,134.58234886169433,4.46644e-13,139.74534886169434Q-0.00000120375,144.90734886169435,3.24421,148.96434886169433L107.05,278.7713488616943Q109.936,282.38034886169436,114.43,283.5403488616943Q118.509,284.5933488616943,122.504,283.23234886169433Q126.5,281.8703488616943,129.043,278.5603488616943Q131.845,274.91234886169434,131.845,270.32034886169436L131.845,66.78024886169433C131.845,57.290848861694336,119.622,53.19954886169434,113.743,60.721448861694334Z" fill-rule="evenodd" fill="url(#master_svg0_132_03714)" fill-opacity="1"/></g><g><path d="M106.60069274902344,237.87509727478027C106.60069274902344,237.98309727478028,106.60128806002344,238.09109727478028,106.60247865902343,238.19909727478029L107.05015274902344,278.77109727478023Q109.93642274902344,282.38009727478027,114.43039274902344,283.54109727478027Q118.50929274902344,284.59409727478027,122.50439274902344,283.23209727478024Q126.49959274902344,281.8700972747803,129.04289274902345,278.5600972747803Q131.84509274902342,274.9130972747803,131.84509274902342,270.32009727478027L131.84509274902342,78.87509727478027L106.60069274902344,111.42229727478028L106.60069274902344,237.87509727478027Z" fill="url(#master_svg1_132_13164)" fill-opacity="1"/></g><g><path d="M11.93413257598877,119.65935601425171L27.99673257598877,139.7453560142517L122.89613257598877,21.075056014251707C126.34513257598877,16.76205601425171,125.58913257598877,10.51394601425171,121.20713257598877,7.119476014251709L117.23913257598878,4.046346014251709C112.85713257598877,0.651866014251709,106.50813257598877,1.396464014251709,103.05913257598877,5.709446014251709L11.93413257598877,119.65935601425171Z" fill="url(#master_svg2_132_13158)" fill-opacity="1"/></g></g><g transform="matrix(-1,0,0,-1,568,564.165283203125)"><g><path d="M397.74296948242187,340.8867416015625L341.7071694824219,412.5876416015625C338.92756948242186,416.1446416015625,338.9143694824219,421.09364160156247,341.6749694824219,424.6646416015625L345.32616948242185,429.3876416015625C349.3680694824219,434.6156416015625,357.3604694824219,434.61464160156254,361.4006694824219,429.3846416015625L390.6009694824219,391.5876416015625L390.6009694824219,460.8106416015625C390.6009694824219,479.6786416015625,366.39176948242186,487.9296416015625,354.5351694824219,473.10364160156246L311.99666948242185,419.9106416015625L406.8959694824219,301.2403416015625C410.3459694824219,296.9273416015625,409.5889694824219,290.6792316015625,405.20696948242187,287.2847516015625L401.2389694824219,284.2116316015625C396.85696948242185,280.8171516015625,390.50796948242186,281.5617496015625,387.05896948242184,285.8747316015625L287.2441794824219,410.6906416015625Q283.99996948242233,414.74764160156246,283.99996948242233,419.9106416015625Q283.9999682786719,425.0726416015625,287.2441794824219,429.1296416015625L391.0499694824219,558.9366416015625Q393.9359694824219,562.5456416015625,398.4299694824219,563.7056416015625Q402.5089694824219,564.7586416015624,406.5039694824219,563.3976416015626Q410.4999694824219,562.0356416015625,413.0429694824219,558.7256416015625Q415.8449694824219,555.0776416015625,415.8449694824219,550.4856416015625L415.8449694824219,346.94554160156247C415.8449694824219,337.4561416015625,403.6219694824219,333.3648416015625,397.74296948242187,340.8867416015625Z" fill-rule="evenodd" fill="url(#master_svg3_132_14394)" fill-opacity="1"/></g><g><path d="M390.6006622314453,518.0403900146484C390.6006622314453,518.1483900146484,390.6012575424453,518.2563900146484,390.60244814144534,518.3643900146485L391.0501222314453,558.9363900146484Q393.9363922314453,562.5453900146484,398.4303622314453,563.7063900146484Q402.5092622314453,564.7593900146485,406.5043622314453,563.3973900146484Q410.49956223144534,562.0353900146484,413.0428622314453,558.7253900146484Q415.8450622314453,555.0783900146484,415.8450622314453,550.4853900146484L415.8450622314453,359.04039001464844L390.6006622314453,391.5875900146484L390.6006622314453,518.0403900146484Z" fill="url(#master_svg4_132_18996)" fill-opacity="1"/></g><g><path d="M295.93410205841064,399.8246487541199L311.9967020584106,419.91064875411985L406.89610205841063,301.24034875411985C410.34510205841065,296.92734875411986,409.5891020584106,290.67923875411987,405.20710205841067,287.28476875411985L401.23910205841065,284.21163875411986C396.85710205841065,280.8171587541199,390.50810205841066,281.56175675411987,387.05910205841064,285.87473875411985L295.93410205841064,399.8246487541199Z" fill="url(#master_svg5_132_22620)" fill-opacity="1"/></g></g></g></svg>
.\apps\zhongshu-miniapp\docs\index.md:68:  # - icon: <svg xmlns="http://www.w3.org/2000/svg" width="30" viewBox="0 0 256 256.32"><defs><linearGradient id="a" x1="-.828%" x2="57.636%" y1="7.652%" y2="78.411%"><stop offset="0%" stop-color="#41D1FF"/><stop offset="100%" stop-color="#BD34FE"/></linearGradient><linearGradient id="b" x1="43.376%" x2="50.316%" y1="2.242%" y2="89.03%"><stop offset="0%" stop-color="#FFEA83"/><stop offset="8.333%" stop-color="#FFDD35"/><stop offset="100%" stop-color="#FFA800"/></linearGradient></defs><path fill="url(#a)" d="M255.153 37.938 134.897 252.976c-2.483 4.44-8.862 4.466-11.382.048L.875 37.958c-2.746-4.814 1.371-10.646 6.827-9.67l120.385 21.517a6.537 6.537 0 0 0 2.322-.004l117.867-21.483c5.438-.991 9.574 4.796 6.877 9.62Z"/><path fill="url(#b)" d="M185.432.063 96.44 17.501a3.268 3.268 0 0 0-2.634 3.014l-5.474 92.456a3.268 3.268 0 0 0 3.997 3.378l24.777-5.718c2.318-.535 4.413 1.507 3.936 3.838l-7.361 36.047c-.495 2.426 1.782 4.5 4.151 3.78l15.304-4.649c2.372-.72 4.652 1.36 4.15 3.788l-11.698 56.621c-.732 3.542 3.979 5.473 5.943 2.437l1.313-2.028 72.516-144.72c1.215-2.423-.88-5.186-3.54-4.672l-25.505 4.922c-2.396.462-4.435-1.77-3.759-4.114l16.646-57.705c.677-2.35-1.37-4.583-3.769-4.113Z"/></svg>
.\reference\donors\zhongshu-design\docs\zhongshu-design\众墅之家设计小程序后端开发工作任务清单-V1.0.md:61:  - [ ] 核验平台阶段 0、1A、1B、阶段 2/D-07 的验收证据
.\reference\donors\zhongshu-design\docs\zhongshu-design\众墅之家设计小程序后端开发工作任务清单-V1.0.md:144:- **正本**：蓝图 §5-P4A；架构 §6.5
.\reference\donors\zhongshu-design\docs\zhongshu-design\众墅之家设计小程序后端开发工作任务清单-V1.0.md:149:  - [ ] 7 种流水类型（架构 §6.5）各记录 biz_type、biz_id、变化值、变化前后余额、幂等键、操作者、原因
.\docs\reviews\codex-ZS-BRAND-001.raw.md:134:- 寤鸿寰呯‘璁わ細D-07鈥滃姞鐩熷晢寮€閫氣€旈鏉＄嚎绱㈤棴鐜€?
.\docs\reviews\codex-ZS-BRAND-001.raw.md:311:  - D-07銆丏-09 浠嶅緟鍐筹紱D-10銆丏-11 浠嶅悗缃€傝处鍙?韬唤/缁勭粐瑙勫垯涓嶅緱鍊熸暟鎹簱鎶€鏈敼閫犳彁鍓嶅浐鍖栵紱鏈竻鍗曚笉
.\docs\reviews\codex-ZS-BRAND-001.raw.md:313:  | M11 Flowable 涓庝笟鍔℃帴鍏ヨ鑼?| 宸插畬鎴愰潤鎬佸垎鏋愶紝宸茬櫥璁?| ZS-BPM-001锝?04锛屽叡 4 椤癸紱姝ｅ紡棣栭摼鍙
.\docs\reviews\codex-ZS-BRAND-001.raw.md:314:?D-07/D-09 闂ㄧ |
.\docs\reviews\codex-ZS-BRAND-001.raw.md:1034:紭鍏堢骇 P1锛涚被鍒?楠岃瘉閫傞厤锛涚姸鎬?寰呭墠缃紱鍓嶇疆 D-07/D-09 鍚庣‘璁ゅ疄闄呮帴鍏ワ紱寰俊
.\docs\reviews\codex-ZS-BRAND-001.raw.md:1292:浠诲姟缂栧彿鍞竴銆丷3 鐘舵€佹灇涓惧悎娉曘€丷4 鏈‘璁ゅ喅绛栵紙D-07/D-09/D-10/D-11锛変笉寰楁弿杩颁负鏃㈡垚浜嬪疄锛堝惁瀹氬彞寮忚眮
.\docs\reviews\codex-ZS-BRAND-001.raw.md:1390:椤癸紙76 寰呭紑鍙戙€? 寰呭喅绛栥€? 寰呭墠缃級锛涜ˉ榻愰€愰」璇佹嵁銆佽皟鏁淬€佸墠缃笌姝ｅ弽鍚戦獙鏀讹紝淇濈暀 D-07/D
.\reference\donors\zhongshu-design\docs\zhongshu-design\众墅之家设计小程序后端实施蓝图-V1.0.md:48:必须具备：阶段 0、1A、1B、阶段 2/D-07 的验收证据；D-04 Admin UniApp 决策；D-09 身份/组织模型；产品合同、输入 Hash 与受控归档 URI；唯一、可取回的干净 Git 实现基线。
.\reference\donors\zhongshu-design\docs\zhongshu-design\众墅之家设计小程序后端实施蓝图-V1.0.md:434:当前只有设计文档，没有已验收平台代码，因此先执行 G0A：核验阶段 0、1A、1B、阶段 2/D-07 与 D-09，确认输入哈希、受控归档 URI 和唯一实现仓库；通过后从 P1A 开始，P2A/P3A/P4A 在 P1C 后并行。G0B、G0C 分别等待微信支付与 AI Provider 决策，只阻断 P2B/P8B/P8C 与 P4D，不反向阻断领域/Stub 研发。
.\reference\donors\zhongshu-design\docs\zhongshu-design\T00-G0A门禁与实现基线登记.md:48:| 平台阶段 0、1A、1B、阶段 2/D-07 验收证据 | 待补 | 平台正本仍为文档态（无代码/库验收记录）；本专项承担阶段 3，底座阶段证据由平台负责人补签 |
.\reference\donors\zhongshu-design\docs\zhongshu-design\README.md:3:本目录是设计小程序后端专项在实现仓库内的受控文档区。实施规则、工作包定义与验收门禁以下列正本为准；工作包进度与证据按蓝图 §9 每包登记。
.\docs\reviews\codex-hotfix-E.raw.md:2067:舵€佹灇涓惧悎娉曘€丷4 鏈‘璁ゅ喅绛栵紙D-07/D-09/D-10/D-11锛変笉寰楁弿杩颁负鏃㈡垚浜嬪疄锛堝惁瀹氬彞寮忚眮鍏嶏級銆丷5 README 
.\reference\donors\zhongshu-design\docs\zhongshu-design\04-设计小程序V1.2输入基线与追踪.md:134:2. 产品规则修改后，同时更新产品文档、架构、实施蓝图、OpenAPI/测试合同；效果图仅表示视觉，不覆盖安全和后端规则。
.\docs\reviews\HANDOFF-ZS-SEC-003.md:58:6. **文档与代码一致性**：[Token 传输与连接凭据规范](../../services/zhongshu-core/docs/Token传输与连接凭据规范.md) 的三规则表 / 三调用方矩阵 / 测试证据是否与 `SecurityFrameworkUtils` 实际实现、三处调用方 diff 一致？docs/05 V1.13 统计 44/28→43/29 与 SEC-003 卡状态 待开发→待验收、第 19 节变更记录是否自洽？README 索引 V1.12→V1.13 是否同步？
.\reference\donors\zhongshu-design\docs\zhongshu-design\03-众墅之家设计小程序后端技术架构-V1.0.md:11:众墅之家设计小程序不应再建设一套独立业务后端。推荐把它拆成 `zhongshu-ai-platform` 的阶段 3 设计业务与阶段 4 外部集成：平台阶段 0、1A、1B、阶段 2/D-07 和 D-09 通过后，可实施本文的领域合同、数据库、Stub 与自动化验证；D-10 只解锁真实微信/支付，D-11 只解锁真实 AI Provider：
.\reference\donors\zhongshu-design\docs\zhongshu-design\03-众墅之家设计小程序后端技术架构-V1.0.md:38:10. **三层门禁不混用**：G0A（平台阶段 0～2、D-07/D-09、输入基线）解锁阶段 3 的领域/Stub；G0B（D-10）解锁真实微信支付；G0C（D-11）解锁真实 AI Provider。任一层未通过都不能冒充相应真实链路已上线。
.\reference\donors\zhongshu-design\docs\zhongshu-design\03-众墅之家设计小程序后端技术架构-V1.0.md:338:### 6.5 设计点与计价
.\reference\donors\zhongshu-design\docs\zhongshu-design\03-众墅之家设计小程序后端技术架构-V1.0.md:942:| G0A 平台/阶段 3 业务门 | 阶段 0、1A、1B、阶段 2/D-07 与 D-09 已验收；输入 Hash、受控归档 URI、迁移规则和唯一实现仓库冻结 | 未通过不能落业务表或把本地原型当实现基线 |
.\reference\donors\zhongshu-design\docs\zhongshu-design\03-众墅之家设计小程序后端技术架构-V1.0.md:943:| D-07/平台阶段 2 | 先完成经确认的加盟商—线索薄链与三角色验收，再进入本专项 | 不能证明通用身份、组织、权限和流程底座可承载专项 |
.\reference\donors\zhongshu-design\docs\zhongshu-design\03-众墅之家设计小程序后端技术架构-V1.0.md:976:G0A：阶段 0、1A、1B、阶段 2/D-07 + D-09 + 受控输入基线
.\docs\reviews\codex-ZS-BRAND-006.A.raw.md:3614:- 寤鸿寰呯‘璁わ細D-07鈥滃姞鐩熷晢寮€閫氣€旈鏉＄嚎绱㈤棴鐜€?
.\docs\reviews\codex-ZS-BRAND-004.A.raw.md:1285:- 寤鸿寰呯‘璁わ細D-07鈥滃姞鐩熷晢寮€閫氣€旈鏉＄嚎绱㈤棴鐜€?
.\docs\reviews\codex-ZS-SEC-008.raw.md:3912:9 待验收→31；M05 说明与 ZS-DB-010 前置门禁更新；D-07 方向已采纳（细节待阶段 2 前）；同步 verify-docs 将 D-09 移
.\docs\reviews\codex-ZS-SEC-008-r1.raw.md:1269:        g\springframework\security\spring-security-core\6.5.11\spring-security-
.\docs\reviews\codex-ZS-SEC-008-r1.raw.md:1270:core-6.5.11.jar;C:\Users\Administrator\.
.\docs\reviews\codex-ZS-SEC-008-r1.raw.md:1271:        m2\repository\org\springframework\security\spring-security-crypto\6.5.1
.\docs\reviews\codex-ZS-SEC-008-r1.raw.md:1272:1\spring-security-crypto-6.5.11.jar;C:\U
.\docs\reviews\codex-ZS-OPS-001-step4.raw.md:815: *  R4 决策门禁：未确认决策（D-07/D-10/D-11）不得被写成已批准/已确认/已落地；D-09 已于 2026-09-10 确认最小模型与账号唯
.\docs\reviews\codex-ZS-OPS-001-step4.raw.md:837:const UNCONFIRMED_DECISIONS = ['D-07', 'D-10', 'D-11']; // D-09 已于 2026-09-10 确
.\docs\reviews\codex-ZS-GOV-001-P0.raw.md:88:const UNCONFIRMED_DECISIONS = ['D-07', 'D-10', 'D-11']; // D-09 宸蹭簬 2026-09-10 
.\docs\reviews\codex-ZS-GOV-001-P0.raw.md:395:熻矗浜烘殏鏈垎閰嶃€傞潤鎬佷唬鐮佷綅缃彧鏄樊璺濊瘉鎹紝涓嶆槸淇璇佹嵁銆?- D-07銆丏-09 浠嶅緟鍐筹紱D-10銆丏-11 浠嶅悗缃€傝处鍙
.\docs\reviews\codex-ZS-GOV-001-P0.raw.md:417:| M11 Flowable 涓庝笟鍔℃帴鍏ヨ鑼?| 宸插畬鎴愰潤鎬佸垎鏋愶紝宸茬櫥璁?| ZS-BPM-001锝?04锛屽叡 4 椤癸紱姝ｅ紡棣栭摼鍙?D
.\docs\reviews\codex-ZS-GOV-001-P0.raw.md:440:/ZS-OPS-003 鍓嶇疆 D-07 缁嗚妭纭锛夈€傚叾浣欎换鍔′腑鐨勨€滃緟寮€鍙戔€濊〃绀哄叿澶囨妧鏈媶瑙ｅ叆鍙ｏ紝浠嶉』閬靛畧鍚勫崱鐗囩殑鎵规
.\docs\reviews\codex-ZS-GOV-001-P0.raw.md:1070: *  R4 决策门禁：未确认决策（D-07/D-10/D-11）不得被写成已批准/已确认/已落地；D-09 已于 2026-09-10 确认最小模型与账号唯
.\docs\reviews\codex-ZS-GOV-001-P0.raw.md:1092:const UNCONFIRMED_DECISIONS = ['D-07', 'D-10', 'D-11']; // D-09 已于 2026-09-10 确
.\docs\reviews\codex-ZS-SEC-010.raw.md:4030:- 方向已采纳：D-07“加盟商开通—首条线索闭环”薄链（PILOT-REQ 字段/状态/转商机条件细节待阶段 2 前确认）
.\docs\reviews\codex-ZS-SEC-010.raw.md:4088:本轮没有实施真实 PostgreSQL、全平台身份、多组织权限、业务链、微信支付、AI Provider 或部署。D-09 已于 2026-09-10 确认最小模型（决策门禁解除，实现待阶段 1B）；D-07 方向已采纳、细节待阶段 2 
.\docs\reviews\codex-ZS-SEC-010.raw.md:4126:## 文档同步规则
.\docs\reviews\codex-ZS-GOV-001-P0.md:30:**复核结论：成立，且代码违背其自身文档契约。** [verify-docs.mjs](../../scripts/gov/verify-docs.mjs) 的 R7（L140-142）与 R6（L124）均以 `for (const s of Object.keys(rm.declared))` / `Object.keys(declared)` 遍历——**只比对文档已声明的状态键**。而 L12 的 R7 规则注释明写「**省略的类别按 0 计**」：若 README 摘要删去某个非零类目（如 `、31 待验收`），按契约应判 `声明 0 ≠ 实际 31` 而 FAIL，但实现因该键根本不在 `rm.declared` 中而**从不进入比对** → 放行；此时 L137 的总数校验（`rm.total !== actual05.cardCount`）仍等于 91、不受影响，于是「分布只覆盖 60/91」的自相矛盾摘要能通过全部门禁。**实现与文档契约不一致，确认成立。**
.\docs\reviews\codex-ZS-SEC-009-r2.raw.md:713:AFTER_HEAD=19b736f4fe2bf8b022b0a4736a4c4684a687973b DONE=2026-09-11T13:05:06.5423324+08:00
.\docs\reviews\codex-ZS-SEC-003.md:58:- **专项文档 [Token传输与连接凭据规范](../../services/zhongshu-core/docs/Token传输与连接凭据规范.md)**：§2 三规则表（严格前缀 / 重复冲突 / Header 优先+参数开关）与 `SecurityFrameworkUtils` 实现逐条一致；§4 三调用方矩阵与 grep 结果一致（filter/admin logout/app logout）；§7 测试证据（18 例、`MockHttpServletRequest` 纯单测）与实际测试类一致；§1 现状缺陷描述（`indexOf("Bearer ")` + `substring(index+7)`）与父提交实际旧代码一致。
.\services\zhongshu-core\docs\H2与PG方言回归决策.md:31:`run-pg-regression.mjs` 在本机与 Maven 构建/codex 评审**并行高负载**下偶发单套件 Docker 容器启动/CREATE DATABASE 竞态失败（三次复跑失败套件各不相同，套件单独运行均绿，空载聚合复跑 8/8 全绿）——属环境负载性 flaky、非套件缺陷。**CI 与收口验证建议空载执行，或失败重试一次后再判定**；连续两次同套件失败按真实缺陷 triage。
.\services\zhongshu-core\docs\BPM引擎表与业务扩展表迁移责任决策.md:1:# BPM 引擎表与业务扩展表迁移责任决策（ZS-BPM-001）
.\services\zhongshu-core\docs\BPM引擎表与业务扩展表迁移责任决策.md:3:> 文档日期：2026-09-14（ZS-BPM-001 技术准备先行件交付，B09 批次开工项）
.\services\zhongshu-core\docs\BPM引擎表与业务扩展表迁移责任决策.md:4:> 上位依据：[03-底座二次开发顺序与验收标准](../../../docs/03-底座二次开发顺序与验收标准.md) B09（前置 B05/B08 与 D-07 批准，本卡「技术准备可先行」）、[05-任务清单](../../../docs/05-底座模块分析与开发任务清单.md) ZS-BPM-001 卡、[02-需求台账](../../../docs/02-一期底座需求规格与待决策台账.md) §6.5（D-07 确认前允许 PG 适配与中性技术夹具、禁止固化业务字段/状态）
.\services\zhongshu-core\docs\BPM引擎表与业务扩展表迁移责任决策.md:25:4. **夹具边界（D-07 门禁）：** 验收只使用中性技术夹具（`tech_neutral_approval` 人工审批、
.\services\zhongshu-core\docs\BPM引擎表与业务扩展表迁移责任决策.md:28:   业务组织/对象隔离归 B08/D-07 后的 ZS-BPM-002/003。
.\services\zhongshu-core\docs\BPM引擎表与业务扩展表迁移责任决策.md:32:| 验收项（ZS-BPM-001 卡） | 实现位置 | 结果 |
.\services\zhongshu-core\docs\BPM引擎表与业务扩展表迁移责任决策.md:56:  2026-09-14 聚合首跑中二者再次竞态失败、单独复跑即绿——历史登记的「Docker 负载 flaky」
.\services\zhongshu-core\docs\BPM引擎表与业务扩展表迁移责任决策.md:61:- 本件为 ZS-BPM-001「技术准备先行」交付：**B09 主卡链路仍受门禁**——ZS-BPM-002 审批资格
.\services\zhongshu-core\docs\BPM引擎表与业务扩展表迁移责任决策.md:62:  与对象授权待 ZS-PERM-002.B（B08）；ZS-BPM-003 首链领域状态机待 D-07 批准 + B05/B08；

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "git log --oneline --decorate -8; git log --oneline --left-right main...HEAD; Get-Item -Force AGENTS.md, E:\\AGENTS.md, services\\AGENTS.md, services\\zhongshu-core\\AGENTS.md -ErrorAction SilentlyContinue; rg --files --hidden -g AGENTS.md -g '"'!**/.git/**'"' -g '"'!outputs/**'"'; "'$files = @('"'services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessConfiguration.java','services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessBootstrapTest.java'); foreach ("'$f in $files) { $f; $i=0; Get-Content -Encoding UTF8 -LiteralPath $f | ForEach-Object { $i++; '"'{0,4}: {1}' -f "'$i,$_ } }' in E:\zszj-wt-bpm-001-a
 succeeded in 132ms:
services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessConfiguration.java
   1: package cn.zszj.module.bpm.harness;
   2: 
   3: import com.zaxxer.hikari.HikariConfig;
   4: import com.zaxxer.hikari.HikariDataSource;
   5: import org.flowable.engine.HistoryService;
   6: import org.flowable.engine.ManagementService;
   7: import org.flowable.engine.ProcessEngine;
   8: import org.flowable.engine.ProcessEngineConfiguration;
   9: import org.flowable.engine.RepositoryService;
  10: import org.flowable.engine.RuntimeService;
  11: import org.flowable.engine.TaskService;
  12: import org.flowable.spring.ProcessEngineFactoryBean;
  13: import org.flowable.spring.SpringProcessEngineConfiguration;
  14: import org.springframework.context.annotation.Bean;
  15: import org.springframework.context.annotation.Configuration;
  16: import org.springframework.jdbc.core.JdbcTemplate;
  17: import org.springframework.jdbc.datasource.DataSourceTransactionManager;
  18: import org.springframework.transaction.PlatformTransactionManager;
  19: import org.springframework.transaction.support.TransactionTemplate;
  20: 
  21: import javax.sql.DataSource;
  22: 
  23: /**
  24:  * ZS-BPM-001 ��׼�� BPM ����װ�䣺�ֹ�װ�� SpringProcessEngineConfiguration + Spring �����������
  25:  * �� BpmFlowableConfiguration ������������װ�����ͬ����ͬһ��չ�㣩���������� System ģ����
  26:  * ��ѡ�˲��ԣ������ʸ�� ZS-BPM-002����ר����������ʵ PostgreSQL �ϵĽ����������⻧��ǩ��
  27:  * �첽ִ��������ͣ��
  28:  *
  29:  * ������������ scripts/db/run-bpm001-verify.mjs ���׶�ע�룬ȱʧ������ʧ�ܡ�����Ĭ��������
  30:  *   ZSZJ_BPM_HARNESS_JDBC_URL      ���� jdbc:postgresql:// ��ͷ��
  31:  *   ZSZJ_BPM_HARNESS_USERNAME      bootstrap �׶�=zhongshu_owner��runtime �׶�=zhongshu_app����Ȩ�ޣ���
  32:  *   ZSZJ_BPM_HARNESS_PASSWORD
  33:  *   ZSZJ_BPM_HARNESS_SCHEMA_UPDATE true=�����Թܽ���/�������� owner Ǩ�ƽ׶Σ���false=�����ڲ��Ľṹ��
  34:  *   ZSZJ_BPM_HARNESS_ASYNC_EXECUTOR true=�����첽ִ������runtime ��ѹ�ָ�����false=��������ѹ��bootstrap����
  35:  */
  36: @Configuration(proxyBeanMethods = false)
  37: public class BpmPgHarnessConfiguration {
  38: 
  39:     static final String ENV_URL = "ZSZJ_BPM_HARNESS_JDBC_URL";
  40:     static final String ENV_USERNAME = "ZSZJ_BPM_HARNESS_USERNAME";
  41:     static final String ENV_PASSWORD = "ZSZJ_BPM_HARNESS_PASSWORD";
  42:     static final String ENV_SCHEMA_UPDATE = "ZSZJ_BPM_HARNESS_SCHEMA_UPDATE";
  43:     static final String ENV_ASYNC_EXECUTOR = "ZSZJ_BPM_HARNESS_ASYNC_EXECUTOR";
  44: 
  45:     static String requireEnv(String key) {
  46:         String value = System.getenv(key);
  47:         if (value == null || value.isBlank()) {
  48:             throw new IllegalStateException("[bpm-pg-harness] ȱ�ٻ������� " + key + "����֤���þ�Ĭ���������� run-bpm001-verify.mjs ע��");
  49:         }
  50:         return value.trim();
  51:     }
  52: 
  53:     static boolean requireFlag(String key) {
  54:         String value = requireEnv(key);
  55:         if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
  56:             throw new IllegalStateException("[bpm-pg-harness] �������� " + key + " ��Ϊ true/false��ʵ��=" + value);
  57:         }
  58:         return Boolean.parseBoolean(value);
  59:     }
  60: 
  61:     @Bean(destroyMethod = "close")
  62:     public HikariDataSource bpmHarnessDataSource() {
  63:         String url = requireEnv(ENV_URL);
  64:         if (!url.startsWith("jdbc:postgresql://")) {
  65:             throw new IllegalStateException("[bpm-pg-harness] �о߽�������ʵ PostgreSQL���յ� " + url);
  66:         }
  67:         HikariConfig config = new HikariConfig();
  68:         config.setJdbcUrl(url);
  69:         config.setUsername(requireEnv(ENV_USERNAME));
  70:         config.setPassword(System.getenv(ENV_PASSWORD) == null ? "" : System.getenv(ENV_PASSWORD));
  71:         config.setMaximumPoolSize(4);
  72:         config.setMinimumIdle(1);
  73:         config.setPoolName("bpm-pg-harness");
  74:         // ������ zszj-spring-boot-starter-mybatis ���� PG ����
  75:         config.setDriverClassName("org.postgresql.Driver");
  76:         return new HikariDataSource(config);
  77:     }
  78: 
  79:     @Bean
  80:     public PlatformTransactionManager bpmHarnessTransactionManager(DataSource dataSource) {
  81:         // ��Ӧ��������̬ͬ����������ҵ���� Spring DataSourceTransactionManager ������߽�
  82:         return new DataSourceTransactionManager(dataSource);
  83:     }
  84: 
  85:     @Bean
  86:     public TransactionTemplate bpmHarnessTransactionTemplate(PlatformTransactionManager txManager) {
  87:         return new TransactionTemplate(txManager);
  88:     }
  89: 
  90:     @Bean
  91:     public SpringProcessEngineConfiguration bpmHarnessProcessEngineConfiguration(
  92:             DataSource dataSource, PlatformTransactionManager transactionManager) {
  93:         SpringProcessEngineConfiguration configuration = new SpringProcessEngineConfiguration();
  94:         configuration.setDataSource(dataSource);
  95:         configuration.setTransactionManager(transactionManager);
  96:         // �����Ǩ�����Σ�bootstrap �׶Σ�owner �˺ţ����������Խ�/������runtime �׶Σ�app ��Ȩ�ޣ���ֹ�Ľṹ
  97:         configuration.setDatabaseSchemaUpdate(requireFlag(ENV_SCHEMA_UPDATE)
  98:                 ? ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE
  99:                 : ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE);
 100:         configuration.setAsyncExecutorActivate(requireFlag(ENV_ASYNC_EXECUTOR));
 101:         configuration.setDeploymentName("bpmPgHarness");
 102:         // �� BpmFlowableConfiguration ��ͬ����չ�㣺ע�������¼�����
 103:         configuration.setEventListeners(java.util.List.of(new BpmPgHarness.EngineEventRecorder()));
 104:         return configuration;
 105:     }
 106: 
 107:     @Bean
 108:     public ProcessEngineFactoryBean bpmHarnessProcessEngine(SpringProcessEngineConfiguration configuration) {
 109:         ProcessEngineFactoryBean factoryBean = new ProcessEngineFactoryBean();
 110:         factoryBean.setProcessEngineConfiguration(configuration);
 111:         return factoryBean;
 112:     }
 113: 
 114:     @Bean
 115:     public RepositoryService repositoryService(ProcessEngine processEngine) {
 116:         return processEngine.getRepositoryService();
 117:     }
 118: 
 119:     @Bean
 120:     public RuntimeService runtimeService(ProcessEngine processEngine) {
 121:         return processEngine.getRuntimeService();
 122:     }
 123: 
 124:     @Bean
 125:     public TaskService taskService(ProcessEngine processEngine) {
 126:         return processEngine.getTaskService();
 127:     }
 128: 
 129:     @Bean
 130:     public HistoryService historyService(ProcessEngine processEngine) {
 131:         return processEngine.getHistoryService();
 132:     }
 133: 
 134:     @Bean
 135:     public ManagementService managementService(ProcessEngine processEngine) {
 136:         return processEngine.getManagementService();
 137:     }
 138: 
 139:     @Bean
 140:     public org.flowable.job.service.impl.asyncexecutor.AsyncExecutor bpmHarnessAsyncExecutor(
 141:             SpringProcessEngineConfiguration configuration) {
 142:         // �����湹��ʹ��ͬһʵ����configuration ���Դ���Ĭ��ִ����������������ʽ��ͣ��isActive/start/shutdown��
 143:         return configuration.getAsyncExecutor();
 144:     }
 145: 
 146:     @Bean
 147:     public JdbcTemplate bpmHarnessJdbcTemplate(DataSource dataSource) {
 148:         return new JdbcTemplate(dataSource);
 149:     }
 150: 
 151:     @Bean
 152:     public NeutralEchoDelegate neutralEchoDelegate(DataSource dataSource) {
 153:         return new NeutralEchoDelegate(dataSource);
 154:     }
 155: }
services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessBootstrapTest.java
   1: package cn.zszj.module.bpm.harness;
   2: 
   3: import org.flowable.engine.ManagementService;
   4: import org.flowable.engine.RepositoryService;
   5: import org.flowable.engine.RuntimeService;
   6: import org.flowable.engine.runtime.ProcessInstance;
   7: import org.junit.jupiter.api.BeforeAll;
   8: import org.junit.jupiter.api.MethodOrderer;
   9: import org.junit.jupiter.api.Order;
  10: import org.junit.jupiter.api.Test;
  11: import org.junit.jupiter.api.TestMethodOrder;
  12: import org.springframework.beans.factory.annotation.Autowired;
  13: import org.springframework.boot.test.context.SpringBootTest;
  14: import org.springframework.jdbc.core.JdbcTemplate;
  15: 
  16: import java.util.List;
  17: 
  18: import static org.junit.jupiter.api.Assertions.assertEquals;
  19: import static org.junit.jupiter.api.Assertions.assertNotEquals;
  20: import static org.junit.jupiter.api.Assertions.assertNotNull;
  21: import static org.junit.jupiter.api.Assertions.assertTrue;
  22: 
  23: /**
  24:  * ZS-BPM-001 �����׶Σ�owner �˺ţ������Թܽ������첽ִ�������𣩣�
  25:  * �ڿ� PG �Ͻ���������������Լоߡ���������ʵ���������첽��ѹ��
  26:  * �� runtime �׶��Ե�Ȩ�� app �˺������������֤��
  27:  * �� scripts/db/run-bpm001-verify.mjs ע�뻷����������Ϊ��һ�׶ε��á�
  28:  */
  29: @SpringBootTest(classes = BpmPgHarnessConfiguration.class)
  30: @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  31: class BpmPgHarnessBootstrapTest {
  32: 
  33:     private static final String RES_APPROVAL = "cn/zszj/module/bpm/harness/neutral-approval.bpmn20.xml";
  34:     private static final String RES_APPROVAL_V2 = "cn/zszj/module/bpm/harness/neutral-approval-v2.bpmn20.xml";
  35:     private static final String RES_ASYNC = "cn/zszj/module/bpm/harness/neutral-async-echo.bpmn20.xml";
  36: 
  37:     @Autowired
  38:     private RepositoryService repositoryService;
  39:     @Autowired
  40:     private RuntimeService runtimeService;
  41:     @Autowired
  42:     private ManagementService managementService;
  43:     @Autowired
  44:     private JdbcTemplate jdbcTemplate;
  45: 
  46:     @BeforeAll
  47:     static void requireBootstrapPhase() {
  48:         // �����׶����壺����������owner��+ ִ������������ѹ�������׶μ�����ʧ��
  49:         assertTrue(Boolean.parseBoolean(BpmPgHarnessConfiguration.requireEnv(BpmPgHarnessConfiguration.ENV_SCHEMA_UPDATE)),
  50:                 "[bpm-pg-harness] bootstrap �׶�Ҫ�� ZSZJ_BPM_HARNESS_SCHEMA_UPDATE=true");
  51:         assertTrue(!Boolean.parseBoolean(BpmPgHarnessConfiguration.requireEnv(BpmPgHarnessConfiguration.ENV_ASYNC_EXECUTOR)),
  52:                 "[bpm-pg-harness] bootstrap �׶�Ҫ�� ZSZJ_BPM_HARNESS_ASYNC_EXECUTOR=false");
  53:         BpmPgHarness.EngineEventRecorder.reset();
  54:     }
  55: 
  56:     @Test
  57:     @Order(10)
  58:     void engineCreatesSchemaOnEmptyPostgres() {
  59:         // PG ���δ�����ŵı�ʶ���۵�ΪСд��Flowable �� PG �Ͻ��������ΪСд act_*
  60:         Integer actTables = jdbcTemplate.queryForObject(
  61:                 "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND lower(table_name) LIKE 'act\\_%'",
  62:                 Integer.class);
  63:         assertTrue(actTables != null && actTables >= 35,
  64:                 "[bpm-pg-harness] Flowable �����δ�� PG ���룬ʵ�� ACT_ ������=" + actTables);
  65:         String schemaVersion = jdbcTemplate.queryForObject(
  66:                 "SELECT VALUE_ FROM ACT_GE_PROPERTY WHERE NAME_ = 'schema.version'", String.class);
  67:         assertNotNull(schemaVersion, "���� schema.version ȱʧ");
  68:         // �� owner ���ݰѰ汾д��̽������� runtime �׶�У�顸�������Ľṹ��
  69:         jdbcTemplate.update("INSERT INTO bpm_harness_probe(note) VALUES (?)",
  70:                 "bootstrap:schema.version=" + schemaVersion);
  71:         System.out.println("[bpm-pg-harness] bootstrap schema.version=" + schemaVersion
  72:                 + " actTables=" + actTables);
  73:     }
  74: 
  75:     @Test
  76:     @Order(20)
  77:     void deployNeutralFixturesWithDuplicateFilteringIdempotency() {
  78:         deploy(RES_APPROVAL, BpmPgHarness.TENANT_1);
  79:         deploy(RES_ASYNC, BpmPgHarness.TENANT_1);
  80:         deploy(RES_APPROVAL, BpmPgHarness.TENANT_2);
  81:         long deployments = repositoryService.createDeploymentQuery().count();
  82:         // ͬ��ͬ��Դ�ظ������ظ����ˣ��������²��𡪡��ظ������ݵ�
  83:         deploy(RES_APPROVAL, BpmPgHarness.TENANT_1);
  84:         assertEquals(deployments, repositoryService.createDeploymentQuery().count(),
  85:                 "�ظ�����Ӧ���� deployment");
  86:         assertEquals(1, repositoryService.createProcessDefinitionQuery()
  87:                 .processDefinitionKey(BpmPgHarness.PROCESS_APPROVAL)
  88:                 .processDefinitionTenantId(BpmPgHarness.TENANT_1).count());
  89:     }
  90: 
  91:     @Test
  92:     @Order(30)
  93:     void startInstancesForRuntimePhase() {
  94:         // A1~A4 �����⻧1��v1����A5 �����⻧2������ʵ���� 6 ���� runtime �׶������
  95:         startApproval("bpm001-A1");
  96:         startApproval("bpm001-A2");
  97:         startApproval("bpm001-A3");
  98:         startApproval("bpm001-A4");
  99:         startApproval("bpm001-A5", BpmPgHarness.TENANT_2);
 100:         // E1 �첽ʵ����ִ�������� �� ��������ѹ
 101:         runtimeService.startProcessInstanceByKeyAndTenantId(
 102:                 BpmPgHarness.PROCESS_ASYNC_ECHO, "bpm001-E1", null, BpmPgHarness.TENANT_1);
 103:         assertEquals(6, runtimeService.createProcessInstanceQuery().count(),
 104:                 "Ӧ���� 6 ������ʵ����5 ���� + 1 �첽��");
 105:         assertEquals(1, managementService.createJobQuery().count(),
 106:                 "�첽ִ��������ʱӦ���� 1 ����ѹ����");
 107:         assertEquals(0, countProbeRowsForInstance(instanceIdByBusinessKey("bpm001-E1")),
 108:                 "ִ��������ʱ̽�����Ӧ���첽������¼");
 109:     }
 110: 
 111:     @Test
 112:     @Order(40)
 113:     void engineTenantIdIsLabelNotAcl() {
 114:         // ���� tenantId �Ǽ�����ǩ�����⻧���˿ɼ����Լ��ϣ��������˵Ĳ�ѯ���⻧�ɼ�
 115:         // ��ҵ����֯����� B08/D-07�������Զ�ס������ʵ�����ֹ��������
 116:         assertEquals(5, runtimeService.createProcessInstanceQuery()
 117:                 .processInstanceTenantId(BpmPgHarness.TENANT_1).count());
 118:         assertEquals(1, runtimeService.createProcessInstanceQuery()
 119:                 .processInstanceTenantId(BpmPgHarness.TENANT_2).count());
 120:         assertEquals(6, runtimeService.createProcessInstanceQuery().count());
 121:     }
 122: 
 123:     @Test
 124:     @Order(50)
 125:     void changedContentDeploysNewVersionAndOldInstancesStayPinned() {
 126:         deploy(RES_APPROVAL_V2, BpmPgHarness.TENANT_1);
 127:         // �·����ʵ���������°汾 v2
 128:         ProcessInstance onV2 = startApproval("bpm001-A6");
 129:         assertEquals(2, repositoryService.createProcessDefinitionQuery()
 130:                 .processDefinitionKey(BpmPgHarness.PROCESS_APPROVAL)
 131:                 .processDefinitionTenantId(BpmPgHarness.TENANT_1)
 132:                 .orderByProcessDefinitionVersion().desc().list().get(0).getVersion());
 133:         // ������ʵ������ԭ�汾 v1
 134:         ProcessInstance onV1 = runtimeService.createProcessInstanceQuery()
 135:                 .processInstanceBusinessKey("bpm001-A1").singleResult();
 136:         assertNotEquals(onV2.getProcessDefinitionVersion(), onV1.getProcessDefinitionVersion());
 137:         assertEquals(1, onV1.getProcessDefinitionVersion());
 138:         assertEquals(2, onV2.getProcessDefinitionVersion());
 139:     }
 140: 
 141:     private ProcessInstance startApproval(String businessKey) {
 142:         return startApproval(businessKey, BpmPgHarness.TENANT_1);
 143:     }
 144: 
 145:     private ProcessInstance startApproval(String businessKey, String tenantId) {
 146:         return runtimeService.startProcessInstanceByKeyAndTenantId(
 147:                 BpmPgHarness.PROCESS_APPROVAL, businessKey, null, tenantId);
 148:     }
 149: 
 150:     private void deploy(String resource, String tenantId) {
 151:         repositoryService.createDeployment()
 152:                 .addClasspathResource(resource)
 153:                 .name("bpmPgHarness@" + resource.substring(resource.lastIndexOf('/') + 1) + "@" + tenantId)
 154:                 .tenantId(tenantId)
 155:                 .enableDuplicateFiltering()
 156:                 .deploy();
 157:     }
 158: 
 159:     private String instanceIdByBusinessKey(String businessKey) {
 160:         ProcessInstance instance = runtimeService.createProcessInstanceQuery()
 161:                 .processInstanceBusinessKey(businessKey).singleResult();
 162:         return instance == null ? null : instance.getId();
 163:     }
 164: 
 165:     private int countProbeRowsForInstance(String instanceId) {
 166:         if (instanceId == null) {
 167:             return -1;
 168:         }
 169:         List<Integer> counts = jdbcTemplate.queryForList(
 170:                 "SELECT count(*) FROM bpm_harness_probe WHERE note = ?", Integer.class, instanceId);
 171:         return counts.isEmpty() ? -1 : counts.get(0);
 172:     }
 173: }

codex
实际 `git diff main` 涉及 42 个文件，超出了列出的四项范围：当前分支还落后于 main 的文件授权、日志脱敏和评审归档提交。我会分别说明这些差异和 BPM 新增代码的问题，避免混淆旧分支快照与合并后的结果。

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new(); $files = @('"'services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessRuntimeTest.java','services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/NeutralEchoDelegate.java','services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarness.java'); foreach ("'$f in $files) { $f; $i=0; Get-Content -Encoding UTF8 -LiteralPath $f | ForEach-Object { $i++; '"'{0,4}: {1}' -f "'$i,$_ } }' in E:\zszj-wt-bpm-001-a
 succeeded in 54ms:
   1: package cn.zszj.module.bpm.harness;
   2: 
   3: import org.flowable.engine.HistoryService;
   4: import org.flowable.engine.ManagementService;
   5: import org.flowable.engine.RepositoryService;
   6: import org.flowable.engine.RuntimeService;
   7: import org.flowable.engine.TaskService;
   8: import org.flowable.engine.history.HistoricProcessInstance;
   9: import org.flowable.engine.runtime.ProcessInstance;
  10: import org.flowable.task.api.Task;
  11: import org.junit.jupiter.api.BeforeAll;
  12: import org.junit.jupiter.api.MethodOrderer;
  13: import org.junit.jupiter.api.Order;
  14: import org.junit.jupiter.api.Test;
  15: import org.junit.jupiter.api.TestMethodOrder;
  16: import org.springframework.beans.factory.annotation.Autowired;
  17: import org.springframework.boot.test.context.SpringBootTest;
  18: import org.springframework.jdbc.core.JdbcTemplate;
  19: import org.springframework.transaction.support.TransactionTemplate;
  20: 
  21: import java.time.Duration;
  22: import java.util.List;
  23: import java.util.Map;
  24: 
  25: import static org.junit.jupiter.api.Assertions.assertEquals;
  26: import static org.junit.jupiter.api.Assertions.assertFalse;
  27: import static org.junit.jupiter.api.Assertions.assertNotNull;
  28: import static org.junit.jupiter.api.Assertions.assertNull;
  29: import static org.junit.jupiter.api.Assertions.assertTrue;
  30: 
  31: /**
  32:  * ZS-BPM-001 ���н׶Σ���Ȩ�� zhongshu_app �˺ţ���ֹ������ṹ���첽ִ������������
  33:  * ����������������� bootstrap �׶����µ�ͬһ PG �⣬��֤�����ָ�������/����/ͨ��/�ܾ�/
  34:  * ����/ת��/��ҳ��ʷ������ԭ���ԡ��첽��ѹ�ָ����������� DDL��
  35:  * �� scripts/db/run-bpm001-verify.mjs ��Ϊ�ڶ��׶ε��á�
  36:  */
  37: @SpringBootTest(classes = BpmPgHarnessConfiguration.class)
  38: @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  39: class BpmPgHarnessRuntimeTest {
  40: 
  41:     private static final Duration ASYNC_WAIT = Duration.ofSeconds(60);
  42: 
  43:     @Autowired
  44:     private RepositoryService repositoryService;
  45:     @Autowired
  46:     private RuntimeService runtimeService;
  47:     @Autowired
  48:     private TaskService taskService;
  49:     @Autowired
  50:     private HistoryService historyService;
  51:     @Autowired
  52:     private ManagementService managementService;
  53:     @Autowired
  54:     private TransactionTemplate transactionTemplate;
  55:     @Autowired
  56:     private JdbcTemplate jdbcTemplate;
  57:     @Autowired
  58:     private org.flowable.job.service.impl.asyncexecutor.AsyncExecutor asyncExecutor;
  59: 
  60:     @BeforeAll
  61:     static void requireRuntimePhase() {
  62:         // ���н׶����壺��Ȩ���˺Ž�ֹ�Ľṹ + ִ�������Զ�������R70 ��ʽ��ͣ����֤�����ָ�����ȷ���ԣ�
  63:         assertFalse(Boolean.parseBoolean(BpmPgHarnessConfiguration.requireEnv(BpmPgHarnessConfiguration.ENV_SCHEMA_UPDATE)),
  64:                 "[bpm-pg-harness] runtime �׶�Ҫ�� ZSZJ_BPM_HARNESS_SCHEMA_UPDATE=false");
  65:         assertFalse(Boolean.parseBoolean(BpmPgHarnessConfiguration.requireEnv(BpmPgHarnessConfiguration.ENV_ASYNC_EXECUTOR)),
  66:                 "[bpm-pg-harness] runtime �׶�Ҫ�� ZSZJ_BPM_HARNESS_ASYNC_EXECUTOR=false��ִ������ R70 ��ʽ��ͣ��");
  67:     }
  68: 
  69:     @Test
  70:     @Order(10)
  71:     void engineRestartsOnExistingSchemaWithoutStructureChange() {
  72:         // ����ֱ֤���½����� schema-update=false ������bootstrap �׶εĶ���/ʵ��/��ʷȫ���ɼ�
  73:         assertEquals(2, repositoryService.createProcessDefinitionQuery()
  74:                 .processDefinitionKey(BpmPgHarness.PROCESS_APPROVAL)
  75:                 .processDefinitionTenantId(BpmPgHarness.TENANT_1).count(), "�⻧1 ��������Ӧ���� v1+v2 �����汾");
  76:         assertEquals(7, runtimeService.createProcessInstanceQuery().count(), "������ 7 ������ʵ��Ӧȫ���ָ�");
  77:         // schema.version �� bootstrap д��̽����ļ�¼һ�� �� ����δ������ṹ
  78:         String recorded = jdbcTemplate.queryForObject(
  79:                 "SELECT note FROM bpm_harness_probe WHERE note LIKE 'bootstrap:schema.version=%' ORDER BY id LIMIT 1",
  80:                 String.class);
  81:         String current = jdbcTemplate.queryForObject(
  82:                 "SELECT VALUE_ FROM ACT_GE_PROPERTY WHERE NAME_ = 'schema.version'", String.class);
  83:         assertEquals(recorded, "bootstrap:schema.version=" + current,
  84:                 "���������� schema �汾Ӧ�� bootstrap ��¼һ�£��������� DDL��");
  85:     }
  86: 
  87:     @Test
  88:     @Order(20)
  89:     void approveCompletesInstanceOnOriginalDefinitionVersion() {
  90:         ProcessInstance instance = requireRunning("bpm001-A1");
  91:         Task task = requireSingleTask(instance.getId());
  92:         taskService.complete(task.getId(), Map.of(BpmPgHarness.VAR_OUTCOME, BpmPgHarness.OUTCOME_APPROVE));
  93:         HistoricProcessInstance historic = historyService.createHistoricProcessInstanceQuery()
  94:                 .processInstanceId(instance.getId()).finished().singleResult();
  95:         assertNotNull(historic, "����ͨ����ʵ��Ӧ������ʷ");
  96:         assertEquals(1, historic.getProcessDefinitionVersion(), "��ʵ��Ӧ��ԭ�汾 v1 ����ɣ��汾��ס��");
  97:         assertEquals(BpmPgHarness.OUTCOME_APPROVE, historyService.createHistoricVariableInstanceQuery()
  98:                 .processInstanceId(instance.getId()).variableName(BpmPgHarness.VAR_OUTCOME)
  99:                 .singleResult().getValue());
 100:         assertNull(runtimeService.createProcessInstanceQuery()
 101:                 .processInstanceId(instance.getId()).singleResult());
 102:     }
 103: 
 104:     @Test
 105:     @Order(30)
 106:     void rejectTakesRejectedBranch() {
 107:         ProcessInstance instance = requireRunning("bpm001-A2");
 108:         Task task = requireSingleTask(instance.getId());
 109:         taskService.complete(task.getId(), Map.of(BpmPgHarness.VAR_OUTCOME, BpmPgHarness.OUTCOME_REJECT));
 110:         assertNotNull(historyService.createHistoricProcessInstanceQuery()
 111:                 .processInstanceId(instance.getId()).finished().singleResult(), "�ܾ���ʵ��Ӧ������ʷ");
 112:         assertEquals(1, historyService.createHistoricActivityInstanceQuery()
 113:                 .processInstanceId(instance.getId()).activityId("rejectedEnd").count(), "Ӧ�� reject ��֧����");
 114:     }
 115: 
 116:     @Test
 117:     @Order(40)
 118:     void withdrawCancelsInstanceWithReasonInHistory() {
 119:         ProcessInstance instance = requireRunning("bpm001-A3");
 120:         runtimeService.deleteProcessInstance(instance.getId(), BpmPgHarness.WITHDRAW_REASON);
 121:         HistoricProcessInstance historic = historyService.createHistoricProcessInstanceQuery()
 122:                 .processInstanceId(instance.getId()).finished().singleResult();
 123:         assertNotNull(historic, "���غ�ʵ��Ӧ������ʷ");
 124:         assertEquals(BpmPgHarness.WITHDRAW_REASON, historic.getDeleteReason(), "����ԭ��Ӧ����ʷ�ɻز�");
 125:         assertNull(runtimeService.createProcessInstanceQuery()
 126:                 .processInstanceId(instance.getId()).singleResult());
 127:     }
 128: 
 129:     @Test
 130:     @Order(50)
 131:     void transferReassignsTaskAndCompletesNormally() {
 132:         ProcessInstance instance = requireRunning("bpm001-A4");
 133:         Task task = requireSingleTask(instance.getId());
 134:         taskService.claim(task.getId(), "tech-user-a");
 135:         assertEquals("tech-user-a", taskService.createTaskQuery().taskId(task.getId()).singleResult().getAssignee());
 136:         taskService.setAssignee(task.getId(), "tech-user-b");
 137:         assertEquals("tech-user-b", taskService.createTaskQuery().taskId(task.getId()).singleResult().getAssignee(),
 138:                 "ת���������Ӧ����");
 139:         taskService.complete(task.getId(), Map.of(BpmPgHarness.VAR_OUTCOME, BpmPgHarness.OUTCOME_APPROVE));
 140:         assertEquals("tech-user-b", historyService.createHistoricTaskInstanceQuery()
 141:                 .taskId(task.getId()).singleResult().getAssignee(), "������ʷӦ��¼ת����������");
 142:     }
 143: 
 144:     @Test
 145:     @Order(60)
 146:     void springTransactionRollbackLeavesNoEngineTrace() {
 147:         long before = runtimeService.createProcessInstanceQuery().count();
 148:         // �����飺�ύ�����ڵķ���־û�
 149:         transactionTemplate.executeWithoutResult(status ->
 150:                 runtimeService.startProcessInstanceByKeyAndTenantId(
 151:                         BpmPgHarness.PROCESS_APPROVAL, "bpm001-A7", null, BpmPgHarness.TENANT_1));
 152:         assertEquals(before + 1, runtimeService.createProcessInstanceQuery().count());
 153:         // ʵ���飺�ع������ڵķ��������κ�����ۼ���֤�������� Spring �����������
 154:         try {
 155:             transactionTemplate.executeWithoutResult(status -> {
 156:                 runtimeService.startProcessInstanceByKeyAndTenantId(
 157:                         BpmPgHarness.PROCESS_APPROVAL, "bpm001-A8", null, BpmPgHarness.TENANT_1);
 158:                 throw new IllegalStateException("harness: ģ��ҵ������ʧ��");
 159:             });
 160:         } catch (IllegalStateException expected) {
 161:             // �ɻع����Գн�
 162:         }
 163:         assertEquals(before + 1, runtimeService.createProcessInstanceQuery().count(),
 164:                 "�ع������ڵķ���Ӧ�־û�");
 165:         assertNull(runtimeService.createProcessInstanceQuery()
 166:                 .processInstanceBusinessKey("bpm001-A8").singleResult());
 167:     }
 168: 
 169:     @Test
 170:     @Order(70)
 171:     void asyncBacklogRecoversAfterRestartAndExecutorStartStopWorks() {
 172:         // ִ����δ������bootstrap ���µ� E1 ��ѹ�������־ñ���
 173:         String e1 = requireRunning("bpm001-E1").getId();
 174:         assertEquals(1, managementService.createJobQuery().count(), "ִ����δ����ʱ��ѹ����Ӧ����");
 175:         assertEquals(0, probeCount(e1), "ִ����δ����ʱ̽�벻Ӧ�л�����¼");
 176:         // ��ʽ����ִ��������ͣ����ֱ֤������ѹ��������ʵ�����
 177:         asyncExecutor.start();
 178:         assertTrue(BpmPgHarness.waitUntil(ASYNC_WAIT, () -> probeCount(e1) > 0),
 179:                 "ִ�����������첽��ѹ����δ��ִ�У�̽���޼�¼��");
 180:         assertTrue(BpmPgHarness.waitUntil(ASYNC_WAIT, () -> historyService.createHistoricProcessInstanceQuery()
 181:                 .processInstanceId(e1).finished().count() == 1), "E1 ʵ��Ӧִ�����");
 182:         // ʵʱ·����ִ��������ʱ�·���� E2 ͬ����ִ��
 183:         ProcessInstance e2 = runtimeService.startProcessInstanceByKeyAndTenantId(
 184:                 BpmPgHarness.PROCESS_ASYNC_ECHO, "bpm001-E2", null, BpmPgHarness.TENANT_1);
 185:         assertTrue(BpmPgHarness.waitUntil(ASYNC_WAIT, () -> probeCount(e2.getId()) > 0),
 186:                 "ִ��������ʱ�첽����δ��ִ��");
 187:         assertTrue(BpmPgHarness.waitUntil(ASYNC_WAIT, () -> historyService.createHistoricProcessInstanceQuery()
 188:                 .processInstanceId(e2.getId()).finished().count() == 1));
 189:         assertEquals(0, managementService.createJobQuery().count(), "��Ӧ������ִ������");
 190:         assertEquals(0, managementService.createDeadLetterJobQuery().count(), "��Ӧ����������");
 191:         // ����ͣ����ִ������ʽ�رպ�ص��ǻ̬����ͣ��β��JVM �˳�ʱ���ٹ���
 192:         asyncExecutor.shutdown();
 193:         assertFalse(asyncExecutor.isActive(), "ִ���� shutdown ��Ӧ�ص��ǻ̬");
 194:     }
 195: 
 196:     @Test
 197:     @Order(80)
 198:     void historyAndRuntimeQueriesSupportPaging() {
 199:         List<HistoricProcessInstance> page = historyService.createHistoricProcessInstanceQuery()
 200:                 .finished()
 201:                 .orderByProcessInstanceEndTime().asc()
 202:                 .listPage(0, 2);
 203:         assertEquals(2, page.size(), "��ҳ��ʷ��ѯӦ������ҳ");
 204:         assertTrue(historyService.createHistoricProcessInstanceQuery().finished().count() >= 4,
 205:                 "ͨ��/�ܾ�/����/ת����ɹ� 4 ��ʵ��Ӧ������ʷ");
 206:         assertEquals(2, historyService.createHistoricTaskInstanceQuery().finished()
 207:                 .listPage(0, 2).size(), "������ʷ��ҳӦ����");
 208:         assertTrue(runtimeService.createProcessInstanceQuery()
 209:                 .listPage(0, 3).size() <= 3, "����ʵ����ҳӦ����");
 210:     }
 211: 
 212:     @Test
 213:     @Order(90)
 214:     void configuredEventListenersFireOnRealEngine() {
 215:         // �� BpmFlowableConfiguration ��ͬ�� setEventListeners ��չ������ʵ PG ��������Ч
 216:         assertTrue(BpmPgHarness.EngineEventRecorder.count("PROCESS_STARTED") >= 2,
 217:                 "�����¼�������Ӧ��¼ PROCESS_STARTED");
 218:         assertTrue(BpmPgHarness.EngineEventRecorder.count("TASK_COMPLETED") >= 3,
 219:                 "�����¼�������Ӧ��¼ TASK_COMPLETED");
 220:     }
 221: 
 222:     private ProcessInstance requireRunning(String businessKey) {
 223:         ProcessInstance instance = runtimeService.createProcessInstanceQuery()
 224:                 .processInstanceBusinessKey(businessKey).singleResult();
 225:         assertNotNull(instance, "ʵ�� " + businessKey + " Ӧ��������̬�������ָ���");
 226:         return instance;
 227:     }
 228: 
 229:     private Task requireSingleTask(String instanceId) {
 230:         Task task = taskService.createTaskQuery().processInstanceId(instanceId).singleResult();
 231:         assertNotNull(task, "����ʵ��Ӧ���ҽ���һ����������");
 232:         return task;
 233:     }
 234: 
 235:     private int probeCount(String instanceId) {
 236:         List<Integer> counts = jdbcTemplate.queryForList(
 237:                 "SELECT count(*) FROM bpm_harness_probe WHERE note = ?", Integer.class, instanceId);
 238:         return counts.isEmpty() ? 0 : counts.get(0);
 239:     }
 240: }
services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/NeutralEchoDelegate.java
   1: package cn.zszj.module.bpm.harness;
   2: 
   3: import org.flowable.common.engine.api.FlowableException;
   4: import org.flowable.engine.delegate.DelegateExecution;
   5: import org.flowable.engine.delegate.JavaDelegate;
   6: 
   7: import javax.sql.DataSource;
   8: import java.sql.Connection;
   9: import java.sql.PreparedStatement;
  10: import java.sql.SQLException;
  11: 
  12: /**
  13:  * ZS-BPM-001 �첽����ί�У��첽��������ִ��ʱ��о�̽���д��һ�У�����ʵ�� ID����
  14:  * �����첽ִ������ѹ�ָ�/ʵʱ���ѡ����ԡ�̽��� bpm_harness_probe �ɱ������� owner �˺�
  15:  * Ԥ����Ĭ��Ȩ���Զ���Ȩ app �˺� DML������һ���Բ��Խ��ּܣ����������١�
  16:  */
  17: public class NeutralEchoDelegate implements JavaDelegate {
  18: 
  19:     private final DataSource dataSource;
  20: 
  21:     public NeutralEchoDelegate(DataSource dataSource) {
  22:         this.dataSource = dataSource;
  23:     }
  24: 
  25:     @Override
  26:     public void execute(DelegateExecution execution) {
  27:         String instanceId = execution.getProcessInstanceId();
  28:         try (Connection connection = dataSource.getConnection();
  29:              PreparedStatement statement = connection.prepareStatement(
  30:                      "INSERT INTO bpm_harness_probe(note) VALUES (?)")) {
  31:             statement.setString(1, instanceId);
  32:             statement.executeUpdate();
  33:         } catch (SQLException e) {
  34:             throw new FlowableException("[bpm-pg-harness] ̽��д��ʧ��", e);
  35:         }
  36:     }
  37: }
services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarness.java
   1: package cn.zszj.module.bpm.harness;
   2: 
   3: import java.time.Duration;
   4: import java.util.Map;
   5: import java.util.Objects;
   6: import java.util.concurrent.ConcurrentHashMap;
   7: import java.util.concurrent.atomic.AtomicLong;
   8: 
   9: /**
  10:  * ZS-BPM-001 ���Լ����о߳����������¼���¼����
  11:  *
  12:  * �߽磨���� docs/02 �� 6.5 �� D-07 ȷ��ǰ������о�����ֻ�����漼������
  13:  * ������/����/�������/�ܾ���֧/����/ת��/�첽���񣩣����̻��κ�����ҵ���ֶΡ�
  14:  * ҵ��״̬������֯��ְ���壻���̼�ͳһ tech_neutral_* ǰ׺��
  15:  */
  16: public final class BpmPgHarness {
  17: 
  18:     /** �����������̼���������˹����������ɱ�����֧����������̬���� */
  19:     public static final String PROCESS_APPROVAL = "tech_neutral_approval";
  20:     /** �����첽�������̼���������첽����������������� */
  21:     public static final String PROCESS_ASYNC_ECHO = "tech_neutral_async_echo";
  22: 
  23:     /** ������ɱ�������ͨ��/�ܾ�����֧��Ψһ·�����ݣ����о߼�����������ҵ���ֶΣ��� */
  24:     public static final String VAR_OUTCOME = "harness_outcome";
  25:     public static final String OUTCOME_APPROVE = "approve";
  26:     public static final String OUTCOME_REJECT = "reject";
  27: 
  28:     /** �����⻧��ǩ������ tenantId ���Ǳ�ǩ������ ACL ���塪��ҵ����֯����� B08/D-07���� */
  29:     public static final String TENANT_1 = "1";
  30:     public static final String TENANT_2 = "2";
  31: 
  32:     /** ����ԭ��runtimeService.deleteProcessInstance �ļ������壩�� */
  33:     public static final String WITHDRAW_REASON = "harness:withdraw";
  34: 
  35:     private BpmPgHarness() {
  36:     }
  37: 
  38:     /**
  39:      * �����¼���������֤�� EngineConfigurationConfigurer#setEventListeners װ����չ��
  40:      * ���� BpmFlowableConfiguration ͬһ���ƣ�����ʵ PG ������ȷʵ��Ч��
  41:      * ��̬�洢��ͬһ JVM �ڿ� Spring �����Ŀɶ���
  42:      */
  43:     public static final class EngineEventRecorder implements
  44:             org.flowable.common.engine.api.delegate.event.FlowableEventListener {
  45: 
  46:         private static final Map<String, AtomicLong> COUNTERS = new ConcurrentHashMap<>();
  47: 
  48:         public static long count(String eventType) {
  49:             return COUNTERS.getOrDefault(eventType, new AtomicLong()).get();
  50:         }
  51: 
  52:         public static void reset() {
  53:             COUNTERS.clear();
  54:         }
  55: 
  56:         @Override
  57:         public void onEvent(org.flowable.common.engine.api.delegate.event.FlowableEvent event) {
  58:             COUNTERS.computeIfAbsent(event.getType().name(), k -> new AtomicLong()).incrementAndGet();
  59:         }
  60: 
  61:         @Override
  62:         public boolean isFailOnException() {
  63:             return false;
  64:         }
  65: 
  66:         @Override
  67:         public boolean isFireOnTransactionLifecycleEvent() {
  68:             return false;
  69:         }
  70: 
  71:         @Override
  72:         public String getOnTransaction() {
  73:             return null;
  74:         }
  75:     }
  76: 
  77:     /** ����ѯ�ȴ����� Awaitility �������� */
  78:     public static boolean waitUntil(Duration timeout, java.util.function.BooleanSupplier condition) {
  79:         Objects.requireNonNull(condition);
  80:         long deadline = System.nanoTime() + timeout.toNanos();
  81:         while (System.nanoTime() < deadline) {
  82:             if (condition.getAsBoolean()) {
  83:                 return true;
  84:             }
  85:             try {
  86:                 Thread.sleep(500L);
  87:             } catch (InterruptedException e) {
  88:                 Thread.currentThread().interrupt();
  89:                 return false;
  90:             }
  91:         }
  92:         return condition.getAsBoolean();
  93:     }
  94: }

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'git -c core.quotepath=false diff main -- README.md docs services/zhongshu-core/docs services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common/util/log/LogSanitizeUtils.java services/zhongshu-core/zszj-framework/zszj-common/src/test/java/cn/zszj/framework/common/util/log/LogSanitizeUtilsTest.java' in E:\zszj-wt-bpm-001-a
 succeeded in 0ms:
warning: unable to access 'C:\Users\Administrator/.config/git/ignore': Permission denied
diff --git a/README.md b/README.md
index 0b0f34f2..781c31ff 100644
--- a/README.md
+++ b/README.md
@@ -16,7 +16,7 @@
 - 未通过/未执行：Web 类型检查有上游基线错误；小程序离线依赖不全，构建未验证；真实 PostgreSQL 初始化、众墅规则、多端联调和部署尚未执行
 - 工程治理：独立 Git；迁入分支 `feat/foundation-source-import-20260908`，起点 `5758eda`；远程为 `phlong026/zhongshu-ai-platform` 私有仓库；源码快照归档不代表合并主分支、CI 通过或部署
 - 文档治理：`FND-DOC-001～003` 已进入需求基线，当前均为 `DOCUMENTED`；本地文档一致性检查器（ZS-GOV-001 `verify-docs` R1～R7）已实现并纳入 `run-local-gates`，含 §2/README 统计分布一致性门禁（R6/R7）与 `close-task` 一键收口回填；真实 CI 接入与 CI_VERIFIED 登记归 ZS-OPS-001
-- 模块分析：M01～M12 共 12 个底座模块已完成静态分析；加入品牌命名专项后累计 91 项主任务（26 待开发、15 开发中、44 待验收、6 待前置）；27 项拆成 60 个分批子项且不重复计数；尚未实施或验收
+- 模块分析：M01～M12 共 12 个底座模块已完成静态分析；加入品牌命名专项后累计 91 项主任务（27 待开发、14 开发中、44 待验收、6 待前置）；27 项拆成 60 个分批子项且不重复计数；尚未实施或验收
 - 品牌命名：ZS-BRAND-001～006 首轮开发完成（映射冻结、后端改名、两端静态品牌、种子与存量迁移脚本、代码生成模板、命名门禁，2026-09-08，提交 887bbc4f…b2c26ea9，见 [docs/06](docs/06-品牌素材与命名映射.md) 第 9 节执行记录）；分批联验（B02 工具链构建、B03/B05 缓存会话与任务、B06 多端）与工商全称/矢量原稿仍待后续补证
 
 ## 文档索引
@@ -27,7 +27,7 @@
 | [一期底座需求规格与待决策台账](docs/02-一期底座需求规格与待决策台账.md) | V0.12 | 一期需求、验收条件、决策状态、阻塞关系和变更台账 |
 | [底座二次开发顺序与验收标准](docs/03-底座二次开发顺序与验收标准.md) | V1.6 | 24 个工作包对应 12 个批次，逐批前置条件、修改位置、正反向验收和执行入口 |
 | [源码迁入与验证报告](docs/04-源码迁入与验证报告.md) | V1.0 | 源码范围、凭据净化、文件完整性、构建/测试结果和未通过门禁 |
-| [底座模块分析与开发任务清单](docs/05-底座模块分析与开发任务清单.md) | V1.40 | 12 个模块静态分析；按众墅要求记录代码差距、调整任务、优先级、依赖和验收；累计 91 项主任务 |
+| [底座模块分析与开发任务清单](docs/05-底座模块分析与开发任务清单.md) | V1.38 | 12 个模块静态分析；按众墅要求记录代码差距、调整任务、优先级、依赖和验收；累计 91 项主任务 |
 | [品牌素材与命名映射](docs/06-品牌素材与命名映射.md) | V1.1 | ZS-BRAND-001 冻结稿：品牌依据、zszj 命名映射、保留例外、外部标识排除与首轮执行记录 |
 | [第三方来源与许可证](THIRD_PARTY_NOTICES.md) | 2026-09-08 | 固定 SHA、许可证、供体边界与可追溯差异 |
 | [现阶段底座开发清单与多端架构](.omx/plans/2026-09-08-底座开发清单与多端架构.md) | V1.0 建议稿 | 24 个工作包、当前源码证据、复用/二开/自研边界、Web/小程序/App/iOS 接入与验收 |
diff --git a/docs/05-底座模块分析与开发任务清单.md b/docs/05-底座模块分析与开发任务清单.md
index a5e6f050..aae96c75 100644
--- a/docs/05-底座模块分析与开发任务清单.md
+++ b/docs/05-底座模块分析与开发任务清单.md
@@ -1,6 +1,6 @@
 # 众墅之家 AI 赋能平台：底座模块分析与开发任务清单
 
-> 文档版本：V1.40
+> 文档版本：V1.38
 > 建立与更新日期：2026-09-08  
 > 状态：模块分析形成的开发待办；M01 工程骨架、M02 数据库与品牌命名专项 ZS-BRAND-001～006 已完成首轮开发（含真实运行代码与数据库迁移变更，见各卡开发记录），其余任务待按批次实施；0 项已验收（验收须真实环境按批次放行）  
 > 代码核查基线：000e1dfb77d0b46f58a4a8993c4e4011a90a699d（迁入基线；品牌改名前基线见标签 brand-rename-baseline=4ffaaf29）  
@@ -42,7 +42,7 @@
 | 横切：品牌与代码命名统一 | 已按用户要求登记；V1.5 完成首轮开发 | ZS-BRAND-001～006，共 6 项；B01 起实施，B06 技术闭环验收 |
 | 横切：文档与任务治理 | 已登记缺口 | ZS-GOV-001，共 1 项 |
 
-12 个底座模块已完成静态分析。V1.3 为 85 项；本版按用户要求新增品牌与代码命名专项 ZS-BRAND-001～006，共 6 项，累计 91 项主任务。V1.20 统计（2026-09-11，按各卡「状态」字段重新计数；2026-09-13 V1.30/V1.31 订正：ZS-CFG-004、ZS-CLIENT-003、ZS-LOGIN-003 三卡转待验收，自 35/37 订正为 32/40）：26 项待开发、15 项开发中、44 项待验收、0 项待决策、6 项待前置；0 项已验收（待验收=ENG-001～006、DB-003～009/011～018/020、SEC-002/003/005/007/008、IAM-001、品牌 001/002/005、GOV-001；V1.30/V1.31 订正（2026-09-13）：ZS-CFG-004（B03 值校验部分）、ZS-CLIENT-003（B03 登录请求合同部分）、ZS-LOGIN-003 三卡由待开发转待验收，统计随之 35/37→32/40；V1.32 订正（2026-09-13）：ZS-LOGIN-005 主卡由待开发转开发中（.A 交付、.B 归 B05），统计随之 32/13→31/14，待验收不变；V1.33 订正（2026-09-13）：ZS-PERM-004 主卡由待开发转开发中（.A 交付、.B 归 B08），统计随之 31/14→30/15，待验收不变；V1.34 订正（2026-09-13）：ZS-CFG-003 主卡（.A+.B 均交付）由开发中转待验收，统计随之 30/15→30/14/41；V1.35 订正（2026-09-13）：ZS-SEC-012 主卡（.A+.B 均交付）由待开发转待验收，统计随之 30/14/41→29/14/42；V1.36/V1.37 订正（2026-09-13，他区）：ZS-LOGIN-002/004 收口补录转待验收；V1.35 订正（2026-09-13，本区）：ZS-SEC-012 主卡（.A+.B 均交付）由待开发转待验收；截至本版实际聚合 27/14/44；开发中=DB-001/002/019、SEC-009、CFG-001/002/003、CLIENT-005、OPS-001 与品牌 003/004/006，均为分批子项未全部收口的主卡状态；待验收/开发中指首轮开发已完成、待对应批次真实环境放行，非已验收）。27 项主任务拆为 60 个分批子项，子项不叠加到 91 项统计。任务覆盖当前底座模块分析和品牌命名范围，不等于穷尽未来业务系统、Provider 或渠道的全部开发任务。
+12 个底座模块已完成静态分析。V1.3 为 85 项；本版按用户要求新增品牌与代码命名专项 ZS-BRAND-001～006，共 6 项，累计 91 项主任务。V1.20 统计（2026-09-11，按各卡「状态」字段重新计数；2026-09-13 V1.30/V1.31 订正：ZS-CFG-004、ZS-CLIENT-003、ZS-LOGIN-003 三卡转待验收，自 35/37 订正为 32/40）：27 项待开发、14 项开发中、44 项待验收、0 项待决策、6 项待前置；0 项已验收（待验收=ENG-001～006、DB-003～009/011～018/020、SEC-002/003/005/007/008、IAM-001、品牌 001/002/005、GOV-001；V1.30/V1.31 订正（2026-09-13）：ZS-CFG-004（B03 值校验部分）、ZS-CLIENT-003（B03 登录请求合同部分）、ZS-LOGIN-003 三卡由待开发转待验收，统计随之 35/37→32/40；V1.32 订正（2026-09-13）：ZS-LOGIN-005 主卡由待开发转开发中（.A 交付、.B 归 B05），统计随之 32/13→31/14，待验收不变；V1.33 订正（2026-09-13）：ZS-PERM-004 主卡由待开发转开发中（.A 交付、.B 归 B08），统计随之 31/14→30/15，待验收不变；V1.34 订正（2026-09-13）：ZS-CFG-003 主卡（.A+.B 均交付）由开发中转待验收，统计随之 30/15→30/14/41；V1.35 订正（2026-09-13）：ZS-SEC-012 主卡（.A+.B 均交付）由待开发转待验收，统计随之 30/14/41→29/14/42；V1.36/V1.37 订正（2026-09-13，他区）：ZS-LOGIN-002/004 收口补录转待验收；V1.35 订正（2026-09-13，本区）：ZS-SEC-012 主卡（.A+.B 均交付）由待开发转待验收；截至本版实际聚合 27/14/44；开发中=DB-001/002/019、SEC-009、CFG-001/002/003、CLIENT-005、OPS-001 与品牌 003/004/006，均为分批子项未全部收口的主卡状态；待验收/开发中指首轮开发已完成、待对应批次真实环境放行，非已验收）。27 项主任务拆为 60 个分批子项，子项不叠加到 91 项统计。任务覆盖当前底座模块分析和品牌命名范围，不等于穷尽未来业务系统、Provider 或渠道的全部开发任务。
 
 本轮完成的是关键入口、调用链、数据对象与需求差距的静态分析，不是全仓逐行安全审计，也未执行运行代码修改、数据库连接/迁移、构建、真实集成或部署。后续开发中发现的新路径或失败测试应继续更新稳定任务，不以本轮分析结论代替运行验证。
 
@@ -375,7 +375,6 @@ V1.13 变更记录（2026-09-10）：ZS-SEC-003（规范 Token 传输与特殊
 - 调整：建立共享脱敏/允许记录字段策略，覆盖嵌套数组、大小写、凭据别名、错误消息和非 JSON；脱敏失败只记摘要/长度/原因，不回退原文；设置日志体积上限及敏感端点默认不记正文；异常日志权限和保留期在 M08 补充。
 - 验收：正常、畸形 JSON、脱敏器失败、业务/系统错误、限流和重复提交时，测试秘密均不出现在应用日志/日志表/响应；保留可定位的错误码与关联 ID；日志写入失败不再次输出未净化对象。
 - 开发记录（2026-09-09，ZS-SEC-007）：新增共享脱敏工具 `LogSanitizeUtils`（zszj-common，不依赖 servlet/web，供 web 与 protection 两 starter 复用），四入口 sanitizeJson/sanitizeMap/sanitizeArgs/sanitizeResponseBody：键归一（小写去下划线/连字符）后以内置凭据根集（password/token/secret/authorization/apikey/cookie/session/otp 等）contains 模糊匹配 + 端点级 extraKeys 精确匹配，对象/数组逐层递归掩码为 `***` 并保留字段名，解析/序列化失败只记摘要（长度或类型 + 原因类别）绝不回退原文，结果超 2048 字符截断附总长度。落地 5 写点：[访问日志][E39] query/body 净化 + responseBody 默认不记（responseEnable=false）+ 写库失败只记 url/traceId 不再打印 DTO；[异常处理器][E37] query/body 净化 + 写库失败同上；[幂等][E49]、[限流切面][E50] 拒绝日志方法参数经 sanitizeArgs 脱敏；**同类第 5 写点 `ApiSignatureAspect`（范围扩展）**：签名失败日志 joinPoint.getArgs() 经 sanitizeArgs 脱敏、重复请求日志 sign 派生值以 `MASK` 掩码（appId/timestamp/nonce 保留定位），与 E49/E50 同模块同缺陷模式故纳入本轮，仅改日志不动验签逻辑；为供切面就地掩码单个派生值，`LogSanitizeUtils.MASK` 由 private 提升为 public。测试：`LogSanitizeUtilsTest`（common 10 用例）+ 5 写点组件测试（`IdempotentAspectTest`/`RateLimiterAspectTest`/`ApiSignatureAspectTest` 以 Logback ListAppender 捕获日志断言秘密不出现、非敏感字段与方法描述保留；`ApiAccessLogFilterTest`/`GlobalExceptionHandlerTest` 断言日志表 DTO 净化 + 写库失败不输出未净化对象），Maven common 10、protection 6、web 4 全 BUILD SUCCESS，`run-local-gates --fast` 10/10；共享策略与字段允许清单见 [日志脱敏策略](../services/zhongshu-core/docs/日志脱敏策略.md)。待验收说明：本轮为代码级脱敏与组件测试，真实请求日志表落库净化、生产日志采样与敏感端点默认不记正文的运行时验证归 B03/B04；异常日志权限与保留期在 M08（ZS-AUDIT-002）补充。
-- 开发记录（2026-09-14，HANDOFF 补评 + hotfix）：8 份早期 B03 交接单按「每批 4 份并行」补齐正式 codex 评审（见 reviews/README 尾注汇总），本卡补评（`codex review --commit 332b4d7c`）发现 **1×P1+1×P2** 并经 hotfix 修复归零：①P1——`@JsonRawValue` 字段（现网 [AppDiyPagePropertyRespVO].property）经 `valueToTree` 保留为 POJONode(RawValue) 被 `sanitizeNode` 跳过，内嵌 `{"password":...}` 原文穿透响应日志脱敏（违反「脱敏失败绝不回退原文」合同）；②P2——`normalizeKey` 的 `toLowerCase()` 缺 `Locale.ROOT`，土耳其 locale 下 `AUTHORIZATION`/`PRIVATEKEY`/`X-API-KEY` 不命中敏感根集。**hotfix**（worktree 分支 `feat/sec-007`，`9728b369`→`39c8b8dd`，`--no-ff` 合并 main `a1e3a505`）：新增 `toSanitizableTree`/`materializeRaw` 统一物化（RED 调试实证 POJONode 内包 Jackson `RawValue` 包装器、须 `rawValue()` 解包后 `readTree`，三处 valueToTree 入口全接）+ `Locale.ROOT` + 3 回归测试；**r1 揪出修复自身引入的新 P1**（物化失败 fallback `TextNode.valueOf(toString())` 携带原文穿透）→ 改 **fail-closed 上抛**交既有外层 catch 降级安全摘要 → r2 **CLEAN/0 发现**。验证：TDD RED 3 失败实证 → `zszj-common` 全量 13/0 BUILD SUCCESS；处置详见 [codex-ZS-SEC-007.md](reviews/codex-ZS-SEC-007.md) 与 [codex-hotfix-SEC-007-P1.md](reviews/codex-hotfix-SEC-007-P1.md)。待验收说明：responseBody 默认不记（responseEnable=false）不变，P1 暴露面为开启响应体日志的部署形态；生产采样验证仍归 B03/B04 环境批次。本记录不表示任何主任务已验收。
 
 ### ZS-SEC-008：补齐参数校验、上下文头解析与请求资源限制
 
@@ -603,7 +602,7 @@ B04 先按现有技术账号/tenant 实现私有文件机制；D-09 后在 B08 
 
 ### ZS-FILE-001：建立文件归属与统一对象授权入口
 
-- 关联：FND-INF-001、FND-AUTH-004/008；WP-12；B04/B08（按子项独立放行）。优先级 P0；类别 改造；状态 开发中；前置 按第 16.1 节各子项，禁止将后置子项作为早期批次前置。
+- 关联：FND-INF-001、FND-AUTH-004/008；WP-12；B04/B08（按子项独立放行）。优先级 P0；类别 改造；状态 待开发；前置 按第 16.1 节各子项，禁止将后置子项作为早期批次前置。
 - 众墅要求与现状：[FileDO](../services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/dal/dataobject/file/FileDO.java) 继承 BaseDO，未显式包含 tenant、业务对象、所有者或敏感级；[下载 Controller](../services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/controller/admin/file/FileController.java) 的 get/** 标注 PermitAll 和 TenantIgnore。上传需要登录，不能误写为所有文件接口均匿名。
 - 调整：区分公开素材和私有附件；记录服务端确认的主体、技术租户、用途、对象引用和状态；统一读取/删除/批量/导出授权，关闭私有附件的原路径匿名旁路；存储桶/CDN 策略与接口一并检查。
 - 验收：授权者上传和下载成功；无凭据、他人/另一技术租户、猜 ID/路径、批量混入越权文件均不能读取或删除；公开素材仍按批准用途可用。历史文件分类迁移不能默认全部公开。
@@ -1153,5 +1152,3 @@ Vue3 已有登录初始化、后端菜单转动态路由和按钮指令；Admin
 | 2026-09-13 | V1.36 | ZS-LOGIN-004（隔离演示登录配置并保证验证码一次性消费，B03；前置 ZS-ENG-003、ZS-ENG-004、ZS-SEC-010 均已完成）经 worktree 隔离分支 `feat/login-002`（与 ZS-LOGIN-002 同分支串行、分别评审；impl `517863ee`，19 files +1878/-9）`--no-ff` 合并 main（`087be5f0`）：**六项交付**——①验证码原子消费（条件 UPDATE `WHERE used=false` 按影响行数判定，消除 check-then-act 竞态）②尝试次数上限（Redis 计数手机号+场景，`SMS_CODE_EXCEED_ATTEMPT_LIMIT` 1_002_014_006）③每 IP 发送频控（小时/天滚动桶 1_002_014_007，封堵同 IP 轮换手机号喷洒）④固定演示码隔离（yaml 9999 → 环境变量占位+安全默认 1000~9999，固定值下沉 local profile，`SmsCodePropertiesValidator` 启动期校验不安全取值域即失败）⑤登录方式门控（register/smsLogin/socialLogin/resetPassword/sendSmsCode 服务层 `zszj.security.login-mode.*` 默认全关，直调同样被拒 1_002_000_009）⑥通道就绪（未就绪明确失败 1_002_014_008，不写库不占配额不假报）。**codex 评审弧（gpt-6-astra/xhigh，r0→r4 五弧 4×P1+8×P2 全部处置）**：r0 P1 XFF 伪造配额 IP+原子性 3×P2（`10fb7e92`）→ r1 P1 Redis Cluster 裸 IP 键 CROSSSLOT→hash tag+3×P2（`11820138`）→ r2 P1 结构升级无迁移 WRONGTYPE 正确验证码被拒→惰性迁移保留计数 TTL（`5692f8f4`）→ r3 P1 滚动部署无条件删旧桶反复绕过→保留旧桶+P2 PTTL≤0 永久锁（`80845994`）→ r4 **PASS/0 发现**（r2/r3 发现与 r4 结论自 codex 会话日志恢复，原始 stdout 未随暂存保留已如实登记）。**验证**：4 新测试类 46 用例+Redis 安全 5 测试类+TtlTest 全绿、既有三套回归不破、不调用真实短信通道；收口补丁（本日）复验当前 main 6 测试类 **56/0 BUILD SUCCESS**。卡片 待开发→**待验收**，第 2 节统计 29/14/42→28/14/43；真实通道端到端归 B11 外部条件、真集群滚动部署演练归环境批次（SEC-012.B/OPS-002）。本记录不表示任何主任务已验收。处置详见 [codex-ZS-LOGIN-004.md](reviews/codex-ZS-LOGIN-004.md)。README 索引版本同步 V1.36 |
 | 2026-09-13 | V1.37 | ZS-DB-019.B 本地收口补丁（PG 技术回归与持续执行入口，B02 尾巴；前置 ZS-DB-019.A 及 DB-005~008/011~018/020 均已交付待验收）：①当前 main（`c50c6bf6`）空载复跑 `run-pg-regression.mjs` **8 套件全绿/退出 0**（首轮机外 Docker 负载下 3 套件 CREATE DATABASE 竞态失败、重试即全绿且失败集合与既有登记各不相同，按已登记 flaky 处置约定确认为环境负载性）；②交付 [H2与PG方言回归决策](../services/zhongshu-core/docs/H2与PG方言回归决策.md)——H2 保留快速反馈层永不作数据库门禁、PG 回归以 run-pg-regression.mjs 为唯一持续入口（本地 Docker PG17 与 CI pg-regression.yml 同入口、失败非零、缺 Docker 退出码 3 不静默回退，三条语义由 019.A 夹具 self-test 自证）、方言敏感变更准入必须同步补 PG 用例、纯业务逻辑保留 H2/Mockito，附 H2≠PG 六类已知差异守护矩阵；③§16.1 ZS-DB-019.B「PG 技术回归与持续执行入口」本地先行件闭环。父卡维持**开发中**（.A 完整验收依赖 ZS-DB-001.B 真实连接与 B02 正式环境授权，按 16.1 另行收口）。本记录不表示任何主任务已验收。README 索引版本同步 V1.37 |
 | 2026-09-14 | V1.38 | ZS-BRAND-004.B（缓存、浏览器存储和会话命名切换联验，B03 部分；前置 ZS-BRAND-004.A、ZS-LOGIN-005.A、ZS-CLIENT-003 均已完成）经 worktree 隔离分支 `feat/brand-004-b`（impl `345c1c12`，2 scripts）`--no-ff` 合并 main（`b1ba1b24`）：①键清单静态门禁 verify-brand-004b-storage.mjs（后端 51 个 Redis 键模式 + 两端 35 个浏览器存储键，旧名残留 0、命名合同缺失 0、8 组注入自检）②运行期隔离证明 run-brand-004b-runtime.mjs（Docker Redis 7.4+PG17 一次性容器 + 真实 server 账密登录，39/39 断言全绿：新命名空间签发/租户后缀/LOGIN-002 并发刷新无双键/LOGIN-003 撤权键清理+墓碑/旧名隔离直证 MONITOR 零读取+伪造旧名凭据 401）。**兼容窗口与重登录策略**：兼容窗口=0 无迁移义务、存量自然过期+重登录、双键不可绕过、用户无感（浏览器 E2E 归 B06）。**codex 评审弧（gpt-6-astra/xhigh，r0→r2 三弧）**：r0 1×P1+7×P2 → r1 2×P2 → r2 **PASS/0 发现**。验证：run-local-gates --fast 10/10。主卡 ZS-BRAND-004 维持开发中（.A✅.B✅、.C 归 B05）；移交登记：Druid PSCache+PG 游标 local profile 缺陷归 DB-001 复核、键清单门禁并入聚合门禁留收口定、代际键「登录即登记」如需归 LOGIN-002 后续。本记录不表示任何主任务已验收。处置详见 [codex-ZS-BRAND-004.B.md](reviews/codex-ZS-BRAND-004.B.md)。README 索引版本同步 V1.38 |
-| 2026-09-14 | V1.39 | ZS-SEC-007 HANDOFF 补评 + hotfix（日志脱敏 POJONode 穿透 + Locale.ROOT）：8 份早期 B03 交接单按每批 4 份并行补齐正式 codex 评审（4 份 r0 CLEAN：SEC-001.A/005/012.A/PERM-002.A；9×P2 全部登记处置：SEC-002 5×扫描器健壮性/PERM-001.A 1×超管豁免未滤禁用角色/DB-018 3×脚本断言强度）。本卡补评 `332b4d7c` 发现 **1×P1+1×P2**：P1 `@JsonRawValue` 字段经 valueToTree 成 POJONode(RawValue) 被 sanitizeNode 跳过、内嵌密码原文穿透响应日志脱敏；P2 键归一缺 Locale.ROOT 土耳其 locale 下大写敏感键不命中。**hotfix**（`feat/sec-007`，`9728b369`→`39c8b8dd` 合并 `a1e3a505`）：materializeRaw 统一物化（RED 实证须解包 Jackson RawValue）+ Locale.ROOT + 3 回归测试；**r1 揪出修复自身新 P1**（物化失败 fallback 携带 toString 原文）→ fail-closed 上抛交安全摘要 → r2 **CLEAN/0 发现**。验证：zszj-common 13/0 BUILD SUCCESS。卡片 ZS-SEC-007 待验收（补评+hotfix 登记）；responseBody 默认不记不变、生产采样归环境批次。本记录不表示任何主任务已验收。处置详见 [codex-ZS-SEC-007.md](reviews/codex-ZS-SEC-007.md) 与 [codex-hotfix-SEC-007-P1.md](reviews/codex-hotfix-SEC-007-P1.md)。README 索引版本同步 V1.39 |
-| 2026-09-14 | V1.40 | ZS-FILE-001.A（技术账号/tenant 私有文件归属与授权，B04 首任务；前置 ZS-SEC-012.B、ZS-PERM-002.A 均已完成）经 worktree 隔离分支 `feat/file-001-a`（impl `a6de4c86`）`--no-ff` 合并 main（`27d7f550`）：①文件租户化（infra_file 加 tenant_id/owner_user_id/scope 迁移，存量默认 PRIVATE）②写路径归属（owner+PRIVATE+显式 tenantId；configId 不信任客户端 master 兜底）③读取授权统一（下载去 @TenantIgnore 跨租户定位 PUBLIC 同址可用；PRIVATE owner>0 本人或同租户 PermissionCommonApi 查得 infra:file:query；ignore-urls 放行下载路径）④删除授权（批删全部存在+全部同租户整批拒绝）⑤攻击面收敛（presigned create 双端禁用[328→327]、update-scope 端点）。**codex 评审弧（gpt-6-astra，r0→r2 三弧 5×P1+5×P2+P3 全部处置）**：r0 无归属校验/create 冒领/S3 直链绕过 → r1 owner=0 冒领/App create 漏禁/scopes 否决 → r2 **PASS**。验证：infra 252/0 BUILD SUCCESS。主卡 ZS-FILE-001 待开发→开发中（.A 交付、.B 归 B08）；S3 受控取流归 FILE-004.A、归属认领归 FILE-001.B、client 模式假成功归 FILE-003/B06、全链测试归 SYS-001.A（他区）。本记录不表示任何主任务已验收。处置详见 [codex-ZS-FILE-001.A.md](reviews/codex-ZS-FILE-001.A.md)。README 索引版本同步 V1.40 |
diff --git a/docs/reviews/README.md b/docs/reviews/README.md
index a24e08d8..2307f180 100644
--- a/docs/reviews/README.md
+++ b/docs/reviews/README.md
@@ -121,18 +121,10 @@
 | ZS-CLIENT-003 修正请求凭据范围与异常收敛（B03 登录请求合同） | `47572250`（隔离分支 feat/client-003）→ `6de3fae7`（r0）→ `1e4d4ce3`（r1）→ `71e348f6`（r2），合并 `31a653cc` | impl Web+小程序两端请求层 | ✅ 评审通过（r0 1×P1+1×P2 → r1 1×P2 → r2 2×P1+1×P2 → r3 PASS / 0 发现；2×P2 延后登记） | [codex-ZS-CLIENT-003.md](codex-ZS-CLIENT-003.md) | r0 小程序 URL 兼容 + trace-id 合同 → r2 2×P1：协议相对 URL 不拼 baseURL 误判、刷新回放按凭据合同重放/清除 Authorization + 刷新 tenant-id 改单请求作用域堵外部直传泄露；Web vitest + 小程序 vitest 双端全绿 |
 | ZS-PERM-004.A 角色/菜单缓存和停用撤权的技术一致性（分批子项，父卡 ZS-PERM-004） | `f9e4a6a0`（隔离分支 feat/perm-004-a）→ `3c9eb759`（r0）→ `8d4ffa24`（r1）→ `75ad8ac4`（r2），合并 `9085f402` | impl 含 5 缓存一致性契约测试 | ✅ 评审通过（r0 3×P1+2×P2 → r1 2×P1+P3 → r2 2×P1 → r3 PASS，四弧 8×P1 全部处置） | [codex-ZS-PERM-004.A.md](codex-ZS-PERM-004.A.md) | r0 3×P1：ignore-caches 误增项被实证推翻回滚（底层 SQL 有租户过滤裸键共享会跨租户串数据）/租户套餐三方法 @DSTransactional 统一 @Transactional 消除 DS 空提交窗口/驱逐失败无补偿（RetryCacheErrorHandler+ERROR 证据）→ r1 createTenant 补回 @Transactional + 重试下沉 Cache 层 RetryEvictCache 修事务感知 afterCommit 绕过 errorHandler → r2 重试包装改 decorateCache 内层（TransactionAware→RetryEvict→RedisCache）+ 有界重试打 delegate 杜绝无界递归 → r3 PASS。TDD RED（deleteRoleList 不清缓存）→ 115/0 BUILD SUCCESS |
 | ZS-CFG-003.B 套餐/角色权限交集与变更生效（分批子项，父卡 ZS-CFG-003） | `ebd1c066`（隔离分支 feat/cfg-003-b）→ `788d80a1`（r0）→ `94f6a141`（r1）→ `8a7fa8e8`（r2）→ `df146763`（r3），合并 `8fea1a6c` | impl 含 5 契约测试 | ✅ 评审通过（r0→r4 五弧 8×P1+3×P2+P3 全部处置 → r4 PASS） | [codex-ZS-CFG-003.B.md](codex-ZS-CFG-003.B.md) | r0 TOCTOU+防御性放行推翻（授权/套餐变更/换套餐统一租户行锁 + 并发收缩×授权 FOR UPDATE 阻塞序列化测试）→ r1 套餐行锁统一「package→tenant」锁序+锁内重查绑定换出跳过 → r2 锁内读纪律（以锁定绑定/锁定套餐最新菜单判断与授权）→ r3 锁定读 null 拒绝 → r4 PASS。交付授权入口套餐子集校验（TENANT_PACKAGE_MENU_EXCEED 1-002-016-005，系统租户豁免）+ 8 契约测试，123/0 BUILD SUCCESS；主卡 CFG-003 转待验收 |
-| ZS-SEC-001.A 默认关闭跨租户权限跳过（HANDOFF 交接单补评，2026-09-14） | `5b8c702e` | 4 files（门控 + 两端 .env/头注入/切换 UI + 夹具翻转） | ✅ 评审通过（r0 直接 CLEAN 0 发现） | [codex-ZS-SEC-001.A.md](codex-ZS-SEC-001.A.md) | codex 确认后端门控在上下文变更前拦截租户切换、两端前端一致门控头注入与切换入口；测试沙箱未执行，通过性以交付时 33/33 夹具 + 前端 ts 基线为准。受控跨组织授权归 SEC-001.B（B08/D-09） |
-| ZS-SEC-002 接口分类、匿名白名单与方法权限清单（HANDOFF 交接单补评，2026-09-14） | `3fe87023` | ApiInventoryTest 静态扫描器 + 328 端点基线 | ✅ 评审通过（0×P0/P1；5×P2 全部登记处置） | [codex-ZS-SEC-002.md](codex-ZS-SEC-002.md) | 5×P2 均指向扫描器健壮性/门禁硬化：基线缺失静默自比（须改「缺失即失败」）、块注释注解误计、类级 RequestMapping/PreAuthorize 展开与生效语义、基线不保留完整表达式语义——登记归 SEC-002 后续小卡（与 OPS-001.C 门禁收口同窗）；现网 328 端点/25 匿名目录/主体推导未发现回归 |
-| ZS-SEC-005 统一错误响应、HTTP 状态与失败日志结果（HANDOFF 交接单补评，2026-09-14） | `e2b40185` | writeJSON 统一出口 6 文件 8 写点 + 畸形 JSON 归 400 | ✅ 评审通过（r0 直接 CLEAN 0 发现） | [codex-ZS-SEC-005.md](codex-ZS-SEC-005.md) | codex 确认响应写出口迁移、异常处理与新增测试无回归；方案 B（真实 HTTP 状态码）会断 Web axios 刷新链已按矩阵文档登记否决依据；端到端归 SEC-012.B（已收口） |
-| ZS-SEC-007 统一访问、异常与保护切面的日志脱敏（HANDOFF 交接单补评，2026-09-14） | `332b4d7c` | LogSanitizeUtils 四入口 + 5 写点集成 | ✅ r0 1×P1+1×P2 → hotfix 两弧（r1 揪出修复自身新 P1）→ r2 PASS / 0 发现 | [codex-ZS-SEC-007.md](codex-ZS-SEC-007.md)、[codex-hotfix-SEC-007-P1.md](codex-hotfix-SEC-007-P1.md) | P1：`@JsonRawValue` 字段（现网 AppDiyPagePropertyRespVO.property）经 valueToTree 成 POJONode(RawValue) 被 sanitizeNode 跳过，内嵌原文穿透响应日志脱敏；P2：`toLowerCase()` 缺 Locale.ROOT（土耳其 locale 下大写敏感键不命中）。hotfix（feat/sec-007，`9728b369`→`39c8b8dd`，合并 `a1e3a505`）：materializeRaw 物化（`RawValue.rawValue()` 解包实证）+ Locale.ROOT + 3 回归测试（RED 3 失败→GREEN 13/13）；r1 揪出修复自身引入的新 P1（物化失败 fallback 携带 toString 原文）→ fail-closed 上抛交安全摘要 → r2 CLEAN。经验：Jackson @JsonRawValue×valueToTree 须解包 RawValue；脱敏器失败路径也是安全面 |
-| ZS-SEC-012.A 真实安全链失败夹具（HANDOFF 交接单补评，2026-09-14） | `a4d9c6e1` | SecurityFilterChainFixtureTest 7 组 31 用例 | ✅ 评审通过（r0 直接 CLEAN 0 发现） | [codex-ZS-SEC-012.A.md](codex-ZS-SEC-012.A.md) | codex 确认夹具与提交时点安全/租户行为一致（真实 Filter 链全装配未禁用、双技术租户、基线失败如实暴露），移除的死测试依赖正确；端到端扩展归 SEC-012.B（已收口） |
-| ZS-PERM-001.A 授权目标归属与可授予上限校验（HANDOFF 交接单补评，2026-09-14） | `6eb81717` | PermissionServiceImpl 三授权方法写入前统一校验 + 9 夹具 | ✅ 评审通过（0×P0/P1；1×P2 登记处置） | [codex-ZS-PERM-001.A.md](codex-ZS-PERM-001.A.md) | P2：超管豁免经 hasAnySuperAdmin 只查角色编码不查状态——「禁用超管角色仍挂载 + 双角色组合」条件下可条件式绕过自我提权上限；登记归 PERM-001 后续小卡（与 IAM-003 岗位引用校验同窗）。33/33 夹具交付证据沿用 |
-| ZS-PERM-002.A 技术授权矩阵与通用对象级检查入口（HANDOFF 交接单补评，2026-09-14） | `1644070c` | DeptDataPermissionChecker + 授权矩阵文档 + 13 夹具 | ✅ 评审通过（r0 直接 CLEAN 0 发现） | [codex-ZS-PERM-002.A.md](codex-ZS-PERM-002.A.md) | codex 确认检查器与 rule 的 ALL/部门/本人语义一致、共享上下文缓存、批量整批拒绝与 fail-closed 正确；56/56 模块测试交付证据沿用 |
-| ZS-DB-018 ORM、手写 SQL 与租户隔离的 PG 回归（HANDOFF 交接单补评，2026-09-14） | `c3ae2e8e` | run-db018-verify.mjs C1~C10 + self-test | ✅ 评审通过（0×P0/P1；3×P2 登记处置） | [codex-ZS-DB-018.md](codex-ZS-DB-018.md) | 3×P2 均为验证脚本断言强度（JOIN 内容/分页内容/批量双租户）——已由 ZS-SYS-001.A 真实 API 级七类矩阵自然覆盖断言内容，脚本侧列为后续小卡改进；隔离本体（C1~C10、伪造上下文 0 行、三类合法范围）未发现回归 |
 | ZS-SEC-012.B 安全与双端请求联合验收（分批子项，父卡 ZS-SEC-012，B03 终环；他区会话交付、本表 2026-09-14 索引补登） | `25f3ff9e`（隔离分支 feat/sec-012-b），合并见 docs/05 V1.35 行 | impl 含 async/CORS/异步访问日志/G12/G13 门禁 | ✅ 评审通过（r0→r3 四弧 6×P1+8×P2 全部处置 → r3 PASS） | [codex-ZS-SEC-012.B.md](codex-ZS-SEC-012.B.md) | 四项交付：①全面 async 运行时合同（池化 executor+TTL BPP 交替租户防串号/异步异常统一出口/流式 ASYNC 派发不免认证）②CORS 端到端（嵌入容器真实链 evil 403 短路+批准源 ACAO/ACAH/ACEH）③异步访问日志产品修复（ASYNC 派发内记录+租户上下文重建+≥400 防伪报成功）④G12/G13 门禁与 CI security-chain job 本地 CI 同入口。132/0 BUILD SUCCESS；环境坑登记：本地 Maven 仓库陈旧构件致跨模块测试失真、须先 install 上游模块 |
 | ZS-BRAND-004.B 缓存、浏览器存储和会话命名切换联验（分批子项，父卡 ZS-BRAND-004） | `345c1c12`（隔离分支 feat/brand-004-b）→ `930d365e`（r0 1×P1+7×P2 全修）→ `ba13c81d`（r1 2×P2 全修），合并 `b1ba1b24` | 2 scripts（静态键清单门禁 + 运行期隔离证明） | ✅ 评审通过（r0 1×P1+7×P2 → r1 2×P2 → r2 PASS / 0 发现，三弧收敛） | [codex-ZS-BRAND-004.B.md](codex-ZS-BRAND-004.B.md) | r0 codex 揪出：POSIX 子进程泄漏（P1）、键提取三处漏报（无分号声明/.vue/DAO 值形状）、撤权取证被认证自愈掩盖（时序提前）、旧名反向用例区分度不足（升级为「真凭据旧名副本阳性对照 + MONITOR 命令流零读取」隔离直证）、并发刷新断言误过、缺 Docker TDZ → r1 误删 fail() 助手与 MONITOR 固定延时竞态 → r2 PASS。验证：51 Redis 键模式 + 35 两端存储键旧名残留 0、注入自检 8/8、真实 server + Docker Redis/PG 运行期 **39/39 断言全绿**；兼容窗口=0/自然过期+重登录策略已登记（浏览器 E2E 归 B06） |
 
-> 本目录另存有同批次的评审交接单（`HANDOFF-ZS-SEC-001.A`/`002`/`005`/`007`/`012.A`、`HANDOFF-ZS-PERM-001.A`/`002.A`、`HANDOFF-ZS-DB-018`）——**8 份交接单已于 2026-09-14 按「每批 4 份并行」补评收齐并入库**（SEC-001.A/005/012.A/PERM-002.A 四份 r0 CLEAN；SEC-002 5×P2、PERM-001.A 1×P2、DB-018 3×P2 全部登记处置；SEC-007 1×P1+1×P2 经 feat/sec-007 hotfix 两弧修复归零，见 codex-hotfix-SEC-007-P1.md），ZS-SEC-003 为该批次首份完成的 codex 评审，ZS-SEC-008 为第二份（首个经 r0→修复→r1 两轮闭环），ZS-SEC-009 为第三份（首个经 r0→返工→r1→P2 修复→r2 三轮闭环、且触发 Option B 分批拆出 SEC-009.B 的评审），ZS-CFG-002.B 为第四份、也是**首个非 SEC 且首个经 worktree 并行编排试点（提效方案 P1 步骤⑤）在隔离分支交付的 B03 子项**（r0 直接 0 发现），ZS-SEC-004 为第五份、也是**首个在 `--no-ff` 合并入 main 后由 codex r0 跨读前端发现集成回归（2×P1+1×P2）、经 TDD 经验裁决（Spring `checkOrigin` 复现）修复并 r1 复评归零的评审**——两阶段子代理评审因 worktree 内后端孤立视角遗漏，凸显「消费方审计必含 apps 前端」的跨栈评审必要性；ZS-CFG-001.B 为第六份、也是**首个经 r0→r1→r2 三弧级联深挖（r0 修数据损坏 → r1 揪出该修复引入的两步洗密旁路、codex jshell 实证复现 → r2 确认封堵归零）的评审**——凸显安全修复本身可能引入更深旁路，须对每个修复提交持续复评直至归零，而非修一轮即收口；ZS-SEC-010 为第七份、也是**首个 r0 同时揪出「安全能力自身引入的凭据泄露回归」（P1，给凭据端点加 `@RateLimiter` 反而激活拒绝日志泄露标量 `refreshToken` 与短信 `code`）与「跨模块租户解析缺口」（P2），P1 经 TDD 参数名感知脱敏修复并 r1 复评归零、P2 依约定延后（预认证端点不显现 + 需 protection 反依赖 biz-tenant）的评审**——凸显给凭据端点加限流/日志类防护时，防护自身可能成为新的泄露面，须以「复刻真实入参形态」的拒绝测试看守；ZS-SEC-011.A 为第八份、也是**首个经 r0→r1→r2→r3 四弧级联、且 P1 修复本身历经「包名前缀版→类型 `instanceof` 版」两轮深挖（r0 前缀排除 servlet → r1 揪出前缀漏排 Tomcat `ResponseFacade` 且 `mock(HttpServletResponse.class)` 因 ByteBuddy 类名以 `jakarta.servlet` 开头而假 GREEN → r2 确认类型排除正确、P1 归零 → r3 CLEAN）的评审**——凸显「排除基础设施对象须按类型 `instanceof` 而非包名前缀」（容器实现类落在 `org.apache.catalina.*` 等非 servlet 包，前缀过滤漏排）与「测容器行为须用具体子类而非接口 mock」（Mockito 接口 mock 的 ByteBuddy 类名以被 mock 类型包名开头、会假命中前缀过滤，须用 `ContainerLikeResponse extends MockHttpServletResponse` 复现运行时特征）两条教训；ZS-LOGIN-001 为第九份、也是**首个揪出「安全门控/开关机制自身对缓存态不完整」的评审**（r0 P1：新增令牌用途分离门控 `refresh-token-as-access-token-enabled` 只覆盖「Redis 未命中→DB→回退」路径、漏「Redis 命中」提前 return，致 gate 翻 false 后旧缓存合成令牌仍被放行至刷新令牌 TTL 30 天；TDD 修复使 gate 关闭时自愈 evict 污染条目、r1 CLEAN），且属**选项 C「部分交付 + 拆 .B」**（门控核心交付、父卡待验收、WS/IM 迁移与生产 gate 翻转拆 ZS-LOGIN-001.B）——凸显「新增安全开关/门控须覆盖全部数据路径（含缓存态），且开关翻转后历史残留数据须自愈清理，否则安全语义仅对新请求生效、对存量失效」；ZS-IAM-003 为第十份、也是**首个揪出「引用完整性/存在性保护自身被数据权限过滤架空」的评审**（r0 P2：负责人引用计数 `selectCountByLeaderUserId` 查 `DeptDO` 受 `DataPermissionConfiguration.addDeptColumn(DeptDO.class,"id")` 数据权限过滤，受限范围调用者删除用户时其范围外部门——该用户恰任负责人——被过滤漏计，删除放行遗留 `leaderUserId` 悬空，IAM-003 要防的悬空对范围外引用失效；用户选 Path A TDD 修复以 `DataPermissionUtils.executeIgnore` 包裹计数、保留租户过滤、以规避 CGLIB 代理的裸实例反射注入忠实断言计数作用域 RED→GREEN、r1 CLEAN），且与 ZS-LOGIN-001 同 worktree 分支 `feat/login-001`、同 `--no-ff` 合并 `b6b1e4a2`（两任务分别评审、串行文档同步）——凸显「新增引用完整性/存在性校验若查询落在受数据权限过滤的列上，保护会被调用者数据范围架空（范围外引用漏判），此类计数须 `executeIgnore` 关闭数据权限、仅保留租户过滤；且测试须规避 Spring CGLIB 代理（裸实例 + 反射注入）才能忠实断言作用域」。ZS-SEC-006 为第十一份、也是**首个揪出「新增客户端可发请求头未同步 CORS `allowedHeaders` 放行（跨域预检 403）+ 关联 ID 无参访问器每次新生成破坏关联一致性」双 P2 的评审**（r0 `--commit 8785b9f3`：① SEC-004 已在 `exposedHeaders` 暴露 trace-id 供跨域**读响应头**，但 SEC-006 新增「客户端可**发** trace-id 请求头」后 `allowedHeaders` 未放行 → 跨域预检 `Access-Control-Request-Headers: trace-id` 被 403 拦截、请求到不了 `TraceFilter`；② 无参 `getCorrelationId()` 无 OTel Span 时每次 `generateCorrelationId()`、即使 `TraceFilter` 已绑定请求属性 → 与响应头/访问日志/错误日志各不相同，违背「同一请求可关联」目标；r0 TDD 修复 `30e77350`：`allowedHeaders` 精确追加 trace-id + `CorsConfigTest` 预检断言、无参 `getCorrelationId()` 改三级委托（OTel → `ServletUtils.getRequest()` 复用绑定值 → 请求外一次性），r1 `--commit 30e77350` CLEAN/0 发现）——凸显「新增客户端可发请求头须同步 CORS `allowedHeaders` 放行（与 `exposedHeaders` 暴露响应头是两件事，缺一则跨域预检 403 拦截请求）、关联 ID 无参访问器须复用请求已绑定值而非每次新生成，否则与响应头/日志各不一致而破坏关联」。ZS-SEC-006 为整卡任务（非分批子项）、亦为 B03 Wave2 五项并行批次首份合并入 main（`e9daa398`）的评审。2026-09-13 收口补丁：补登上表 8 行此前面板漂移的已入库评审（ZS-CFG-004、ZS-LOGIN-002/003/004、ZS-CLIENT-003、ZS-PERM-004.A、ZS-CFG-003.B；其中 ZS-LOGIN-002/004 为随收口补丁新建的评审处置文档，其评审弧与最终结论自修复提交说明与 codex 会话日志恢复、原始 stdout 未随暂存保留已在文档内如实登记）。
+> 本目录另存有同批次的评审交接单（`HANDOFF-ZS-SEC-001.A`/`002`/`005`/`007`/`012.A`、`HANDOFF-ZS-PERM-001.A`/`002.A`、`HANDOFF-ZS-DB-018`），其对应 `codex-<TASK>.md` 评审产物尚待补齐；ZS-SEC-003 为该批次首份完成的 codex 评审，ZS-SEC-008 为第二份（首个经 r0→修复→r1 两轮闭环），ZS-SEC-009 为第三份（首个经 r0→返工→r1→P2 修复→r2 三轮闭环、且触发 Option B 分批拆出 SEC-009.B 的评审），ZS-CFG-002.B 为第四份、也是**首个非 SEC 且首个经 worktree 并行编排试点（提效方案 P1 步骤⑤）在隔离分支交付的 B03 子项**（r0 直接 0 发现），ZS-SEC-004 为第五份、也是**首个在 `--no-ff` 合并入 main 后由 codex r0 跨读前端发现集成回归（2×P1+1×P2）、经 TDD 经验裁决（Spring `checkOrigin` 复现）修复并 r1 复评归零的评审**——两阶段子代理评审因 worktree 内后端孤立视角遗漏，凸显「消费方审计必含 apps 前端」的跨栈评审必要性；ZS-CFG-001.B 为第六份、也是**首个经 r0→r1→r2 三弧级联深挖（r0 修数据损坏 → r1 揪出该修复引入的两步洗密旁路、codex jshell 实证复现 → r2 确认封堵归零）的评审**——凸显安全修复本身可能引入更深旁路，须对每个修复提交持续复评直至归零，而非修一轮即收口；ZS-SEC-010 为第七份、也是**首个 r0 同时揪出「安全能力自身引入的凭据泄露回归」（P1，给凭据端点加 `@RateLimiter` 反而激活拒绝日志泄露标量 `refreshToken` 与短信 `code`）与「跨模块租户解析缺口」（P2），P1 经 TDD 参数名感知脱敏修复并 r1 复评归零、P2 依约定延后（预认证端点不显现 + 需 protection 反依赖 biz-tenant）的评审**——凸显给凭据端点加限流/日志类防护时，防护自身可能成为新的泄露面，须以「复刻真实入参形态」的拒绝测试看守；ZS-SEC-011.A 为第八份、也是**首个经 r0→r1→r2→r3 四弧级联、且 P1 修复本身历经「包名前缀版→类型 `instanceof` 版」两轮深挖（r0 前缀排除 servlet → r1 揪出前缀漏排 Tomcat `ResponseFacade` 且 `mock(HttpServletResponse.class)` 因 ByteBuddy 类名以 `jakarta.servlet` 开头而假 GREEN → r2 确认类型排除正确、P1 归零 → r3 CLEAN）的评审**——凸显「排除基础设施对象须按类型 `instanceof` 而非包名前缀」（容器实现类落在 `org.apache.catalina.*` 等非 servlet 包，前缀过滤漏排）与「测容器行为须用具体子类而非接口 mock」（Mockito 接口 mock 的 ByteBuddy 类名以被 mock 类型包名开头、会假命中前缀过滤，须用 `ContainerLikeResponse extends MockHttpServletResponse` 复现运行时特征）两条教训；ZS-LOGIN-001 为第九份、也是**首个揪出「安全门控/开关机制自身对缓存态不完整」的评审**（r0 P1：新增令牌用途分离门控 `refresh-token-as-access-token-enabled` 只覆盖「Redis 未命中→DB→回退」路径、漏「Redis 命中」提前 return，致 gate 翻 false 后旧缓存合成令牌仍被放行至刷新令牌 TTL 30 天；TDD 修复使 gate 关闭时自愈 evict 污染条目、r1 CLEAN），且属**选项 C「部分交付 + 拆 .B」**（门控核心交付、父卡待验收、WS/IM 迁移与生产 gate 翻转拆 ZS-LOGIN-001.B）——凸显「新增安全开关/门控须覆盖全部数据路径（含缓存态），且开关翻转后历史残留数据须自愈清理，否则安全语义仅对新请求生效、对存量失效」；ZS-IAM-003 为第十份、也是**首个揪出「引用完整性/存在性保护自身被数据权限过滤架空」的评审**（r0 P2：负责人引用计数 `selectCountByLeaderUserId` 查 `DeptDO` 受 `DataPermissionConfiguration.addDeptColumn(DeptDO.class,"id")` 数据权限过滤，受限范围调用者删除用户时其范围外部门——该用户恰任负责人——被过滤漏计，删除放行遗留 `leaderUserId` 悬空，IAM-003 要防的悬空对范围外引用失效；用户选 Path A TDD 修复以 `DataPermissionUtils.executeIgnore` 包裹计数、保留租户过滤、以规避 CGLIB 代理的裸实例反射注入忠实断言计数作用域 RED→GREEN、r1 CLEAN），且与 ZS-LOGIN-001 同 worktree 分支 `feat/login-001`、同 `--no-ff` 合并 `b6b1e4a2`（两任务分别评审、串行文档同步）——凸显「新增引用完整性/存在性校验若查询落在受数据权限过滤的列上，保护会被调用者数据范围架空（范围外引用漏判），此类计数须 `executeIgnore` 关闭数据权限、仅保留租户过滤；且测试须规避 Spring CGLIB 代理（裸实例 + 反射注入）才能忠实断言作用域」。ZS-SEC-006 为第十一份、也是**首个揪出「新增客户端可发请求头未同步 CORS `allowedHeaders` 放行（跨域预检 403）+ 关联 ID 无参访问器每次新生成破坏关联一致性」双 P2 的评审**（r0 `--commit 8785b9f3`：① SEC-004 已在 `exposedHeaders` 暴露 trace-id 供跨域**读响应头**，但 SEC-006 新增「客户端可**发** trace-id 请求头」后 `allowedHeaders` 未放行 → 跨域预检 `Access-Control-Request-Headers: trace-id` 被 403 拦截、请求到不了 `TraceFilter`；② 无参 `getCorrelationId()` 无 OTel Span 时每次 `generateCorrelationId()`、即使 `TraceFilter` 已绑定请求属性 → 与响应头/访问日志/错误日志各不相同，违背「同一请求可关联」目标；r0 TDD 修复 `30e77350`：`allowedHeaders` 精确追加 trace-id + `CorsConfigTest` 预检断言、无参 `getCorrelationId()` 改三级委托（OTel → `ServletUtils.getRequest()` 复用绑定值 → 请求外一次性），r1 `--commit 30e77350` CLEAN/0 发现）——凸显「新增客户端可发请求头须同步 CORS `allowedHeaders` 放行（与 `exposedHeaders` 暴露响应头是两件事，缺一则跨域预检 403 拦截请求）、关联 ID 无参访问器须复用请求已绑定值而非每次新生成，否则与响应头/日志各不一致而破坏关联」。ZS-SEC-006 为整卡任务（非分批子项）、亦为 B03 Wave2 五项并行批次首份合并入 main（`e9daa398`）的评审。2026-09-13 收口补丁：补登上表 8 行此前面板漂移的已入库评审（ZS-CFG-004、ZS-LOGIN-002/003/004、ZS-CLIENT-003、ZS-PERM-004.A、ZS-CFG-003.B；其中 ZS-LOGIN-002/004 为随收口补丁新建的评审处置文档，其评审弧与最终结论自修复提交说明与 codex 会话日志恢复、原始 stdout 未随暂存保留已在文档内如实登记）。
 
 ## 治理与门禁工具链专项评审状态（ZS-GOV-001 / ZS-OPS-001.A）
 
diff --git a/docs/reviews/codex-ZS-DB-018.md b/docs/reviews/codex-ZS-DB-018.md
deleted file mode 100644
index 64d7c3b2..00000000
--- a/docs/reviews/codex-ZS-DB-018.md
+++ /dev/null
@@ -1,19 +0,0 @@
-# ZS-DB-018 codex 评审处置（HANDOFF 补评，3×P2 登记处置）
-
-- 评审工具：codex（gpt-6-astra / xhigh，read-only sandbox，`codex review --commit c3ae2e8e` @ main）
-- 评审对象：ZS-DB-018 ORM、手写 SQL 与租户隔离的 PG 回归（HANDOFF-ZS-DB-018 交接单的待评审提交；2026-09-14 补评入库）
-- 结论：**0×P0/P1，3×P2 全部登记处置**（按 reviews/README「后续处理约定」第 2 条，非阻塞）
-- 原始日志：`outputs/handoff/ZS-DB-018.txt`（未入库；本档案为处置入库）
-
-## 发现与处置（3×P2，全部指向验证脚本 `run-db018-verify.mjs` 的断言强度）
-
-| # | 发现 | 处置 |
-|---|---|---|
-| 1 | **Assert joined-user data in the LEFT JOIN isolation case**（L179-182）：C3 关联 JOIN 用例断言本租户可见/他租户 0 行，但未断言 JOIN 到的用户列数据内容正确 | 登记处置：断言强化——JOIN 内容断言随 ZS-SYS-001.A 真实 API 级回归自然覆盖（七类矩阵含关联读回），脚本侧列为后续小卡改进项 |
-| 2 | **Assert the actual paginated result's size and contents**（L132-137）：C2 分页用例未断言分页结果集大小与内容 | 同上：SYS-001.A 分页矩阵用例已覆盖内容级断言；脚本侧登记 |
-| 3 | **Make the batch-update filter target both tenants**（L152-156）：C4 批量更新用例的过滤条件未同时覆盖双租户形态 | 同上：批量混入拒绝已由 SYS-001.A 跨类用例覆盖；脚本侧登记 |
-
-## 验证（交付时点证据，沿用卡片开发记录）
-
-- 真实 PG17（一次性 Docker 容器）C1~C10 全过：CRUD/分页/JOIN/批量/逻辑删除/聚合按 tenant_id 隔离、伪造上下文 PK 越权 0 行、三类合法范围（全局表/@TenantIgnore/系统清理）结构断言、`--self-test` 负向对照证明隔离非空洞；已注册进 `run-pg-regression.mjs`（8 套件，2026-09-13 空载复跑 8/8 全绿）
-- SQL 级等效语句验证的边界（非 PG 原生 RLS、拦截器端到端归 SEC-012.A/B）已在卡片登记
diff --git a/docs/reviews/codex-ZS-FILE-001.A.md b/docs/reviews/codex-ZS-FILE-001.A.md
deleted file mode 100644
index 1d3e3e14..00000000
--- a/docs/reviews/codex-ZS-FILE-001.A.md
+++ /dev/null
@@ -1,35 +0,0 @@
-# ZS-FILE-001.A codex 评审处置（r0→r2 三弧）
-
-- 评审工具：codex（gpt-6-astra / xhigh、high，read-only sandbox）
-- 评审对象：分支 `feat/file-001-a`（ZS-FILE-001.A 技术账号/tenant 私有文件归属与授权）
-- 结论：**r2 PASS / 0 发现**（评审收敛）
-
-## 交付内容
-
-1. **文件租户化**：迁移 `V20260914.001__infra_file_tenancy.sql`（infra_file 加 tenant_id/owner_user_id/scope，存量默认 PRIVATE=「不能默认全部公开」）；FileDO 改 TenantBaseDO + ownerUserId/scope；FileScopeEnum（PUBLIC/PRIVATE）。
-2. **写路径归属**：createFile 两路径记录 ownerUserId（服务端确认）+ scope=PRIVATE + 显式 tenantId；presigned create 的 configId 不信任客户端、空值由 master 兜底。
-3. **读取授权统一**：下载端点去 @TenantIgnore（保留 @PermitAll），先跨租户定位记录（getFileByConfigIdAndPathIgnoreTenant，PUBLIC 对任意来源同址可用）再 `validateFileReadable`：PUBLIC 匿名可读；PRIVATE 需「ownerUserId>0 且本人且同租户」或「同租户且 PermissionCommonApi 查得 infra:file:query」（与 @ss.hasPermission 同源；scopes 与后台权限是两套体系被否决）；yaml tenant ignore-urls 放行下载路径。
-4. **删除授权**：deleteFileList 显式校验「全部存在且全部同租户」（不依赖拦截器装配），混入越权/不存在 id 整批拒绝。
-5. **update-scope 管理端点**（infra:file:update 权限 + scope 合法性校验）；presigned create 端点管理端+App 端**同步禁用**（无上传申请绑定可冒领他人文件，凭证化重新交付归 FILE-003）；ApiInventory 基线 328→327 审查通过。
-6. **测试**：FileServiceAuthorizationTest 12 用例（默认 PRIVATE+归属/PUBLIC 匿名/PRIVATE 匿名拒/他租户拒/owner 本人/owner=0 userId=0 冒领拒/管理员放行/无权限拒/批删两向/update-scope 两向）+ 补他区欠账 DatabaseTable 列数断言。
-
-## 评审弧
-
-| 轮次 | 结论 | 要点 |
-|---|---|---|
-| r0 | FAIL 3×P1+3×P2+P3 | P1 PRIVATE 只查租户不查归属；P1 presigned create 可冒领他人文件；P1 S3 直链绕过平台授权；P2 PUBLIC 登录后 404（跨租户查询）；P2 迁移历史归属无恢复路径；P2 测试未证跨租户隔离；P3 错误码冲突 |
-| r1 | FAIL 2×P1+2×P2+P3 | P1 owner=0 凭 userId=0 client-credentials 令牌冒领（owner 分支加 ownerUserId>0）；P1 App 端 /create 漏禁（同步移除）；P2 scopes≠后台权限（管理分支改 PermissionCommonApi 同源）；P2 双端 client 上传模式残留假成功（登记 FILE-003/B06 处置）；P3 批删测试被数量校验短路（拆分） |
-| r2 | **PASS / 0 发现** | — |
-
-## 验证
-
-- infra 全量：**248 → 252 tests / 0 failures，BUILD SUCCESS**（含新增授权 12 用例）。
-- ApiInventoryTest 基线匹配（327 端点）。
-- 合并：`--no-ff` 合并回 main（见 docs/05 V1.38 记录）。
-
-## 延后/边界（非阻塞）
-
-1. **S3/云存储直链绕过**：PRIVATE 的存储桶/CDN 直读限制与受控取流 → ZS-FILE-004.A（主体绑定票据/取流协议，B04 下一环）。
-2. **历史文件归属认领与分类迁移流程**（存量 owner=0/tenant=0 的运营处置）→ FILE-001.B（B08）或运营规程；管理面可经 update-scope 显式调整。
-3. **双端 client 上传模式残留假成功**（后端端点已禁用，前端模式开关未清）→ FILE-003 凭证化时一并收敛（或 B06 前端批次）。
-4. **真实容器全链下载授权测试**（TokenAuthenticationFilter×ignore-urls×下载端点）→ ZS-SYS-001.A 真实 PG/API 回归（他区开发中）。
diff --git a/docs/reviews/codex-ZS-PERM-001.A.md b/docs/reviews/codex-ZS-PERM-001.A.md
deleted file mode 100644
index 58d0e46d..00000000
--- a/docs/reviews/codex-ZS-PERM-001.A.md
+++ /dev/null
@@ -1,20 +0,0 @@
-# ZS-PERM-001.A codex 评审处置（HANDOFF 补评，1×P2 登记处置）
-
-- 评审工具：codex（gpt-6-astra / xhigh，read-only sandbox，`codex review --commit 6eb81717` @ main）
-- 评审对象：ZS-PERM-001.A 授权目标归属与可授予上限校验（HANDOFF-ZS-PERM-001.A 交接单的待评审提交；2026-09-14 补评入库）
-- 结论：**0×P0/P1，1×P2 登记处置**（按 reviews/README「后续处理约定」第 2 条，非阻塞）
-- 原始日志：`outputs/handoff/ZS-PERM-001.A.txt`（未入库；本档案为处置入库）
-
-## 发现与处置
-
-| 级别 | 发现 | 处置 |
-|---|---|---|
-| P2 | **Exclude disabled roles from the super-admin exemption**（PermissionServiceImpl L460-462）：`validateUserRoleGrantCeiling` 的超管豁免经 `RoleServiceImpl.hasAnySuperAdmin()` 只查角色编码、不查角色状态——操作者保留已禁用的 super_admin 角色 + 经另一启用角色持 `assign-user-role` 权限时，豁免仍生效可绕过自我提权上限（条件式绕过，与 `hasAnyPermissions()` 过滤禁用角色的语义不一致） | 登记处置：超管豁免应在判定前过滤禁用角色（对齐 hasAnyPermissions 语义）。触发需「禁用超管角色仍挂载 + 双角色组合」双重条件，实际风险受控；归 ZS-PERM-001 后续小卡（与 ZS-IAM-003 岗位引用校验 hotfix 同窗处置） |
-
-## 验证（交付时点证据，沿用卡片开发记录）
-
-- `-pl :zszj-module-system "-Dtest=PermissionServiceTest" test`：33 tests / 0 failures（2026-09-09T14:05:26+08:00，24 既有 + 9 新增：错租户 user/role/dept 拒绝、批量混入、禁用角色拒绝、自我提权、超上限、重复授权幂等等）
-
-## 边界
-
-本子项仅同技术租户归属校验；获准跨组织范围归 ZS-PERM-001.B（B08/D-09）。前置 ZS-LOGIN-005.A、ZS-PERM-004.A 已按 §16.1 满足并分别评审（codex-ZS-LOGIN-005.A.md / codex-ZS-PERM-004.A.md）。
diff --git a/docs/reviews/codex-ZS-PERM-002.A.md b/docs/reviews/codex-ZS-PERM-002.A.md
deleted file mode 100644
index 4e29d0fe..00000000
--- a/docs/reviews/codex-ZS-PERM-002.A.md
+++ /dev/null
@@ -1,20 +0,0 @@
-# ZS-PERM-002.A codex 评审处置（HANDOFF 补评，r0 一次通过）
-
-- 评审工具：codex（gpt-6-astra / xhigh，read-only sandbox，`codex review --commit 1644070c` @ main）
-- 评审对象：ZS-PERM-002.A 技术授权矩阵与通用对象级检查入口（HANDOFF-ZS-PERM-002.A 交接单的待评审提交；2026-09-14 补评入库）
-- 结论：**r0 CLEAN / 0 发现**（评审通过，无需修复弧）
-- 原始日志：`outputs/handoff/ZS-PERM-002.A.txt`（未入库；本档案为处置入库）
-
-## 结论原文
-
-> "No actionable defects were identified. The checker preserves the existing department/self permission semantics and shares the rule's cache correctly. Tests were reviewed but not executed in the read-only environment."
-
-codex 确认 `DeptDataPermissionChecker` 与 `DeptDataPermissionRule` 的 ALL/命中部门/本人语义完全一致、共享同一 LoginUser 上下文缓存（无重复计算与语义漂移），批量「任一越权整批拒绝」与 fail-closed 护栏正确。
-
-## 验证（交付时点证据，沿用卡片开发记录）
-
-- `mvn -pl :zszj-spring-boot-starter-biz-data-permission test`：全模块 56 tests / 0 failures（43 既有含 DeptDataPermissionRuleTest 8 + 新增 13：ALL/部门/本人/批量混入整批拒绝/空批量/护栏跳过/fail-closed/上下文缓存复用）；`run-local-gates --fast` 10/10
-
-## 边界
-
-检查器对各业务表/路径的逐一接入随领域模块落地；真实 PG+HTTP 端到端对象授权联验归 ZS-SEC-012.B（已收口）/ZS-SYS-001.A（在制）；业务组织 SELF/ASSIGNED 矩阵归 ZS-PERM-002.B（B08/D-09）。
diff --git a/docs/reviews/codex-ZS-SEC-001.A.md b/docs/reviews/codex-ZS-SEC-001.A.md
deleted file mode 100644
index 0691cc04..00000000
--- a/docs/reviews/codex-ZS-SEC-001.A.md
+++ /dev/null
@@ -1,21 +0,0 @@
-# ZS-SEC-001.A codex 评审处置（HANDOFF 补评，r0 一次通过）
-
-- 评审工具：codex（gpt-6-astra / xhigh，read-only sandbox，`codex review --commit 5b8c702e` @ main）
-- 评审对象：ZS-SEC-001.A 默认关闭跨租户权限跳过（HANDOFF-ZS-SEC-001.A 交接单的待评审提交；2026-09-14 补评入库）
-- 结论：**r0 CLEAN / 0 发现**（评审通过，无需修复弧）
-- 原始日志：`outputs/handoff/ZS-SEC-001.A.txt`（未入库；本档案为处置入库）
-
-## 结论原文
-
-> "No actionable regressions were found. The backend gate blocks tenant switching before context mutation, and both frontends consistently gate header injection and switching controls. Tests were inspected but not executed in the read-only environment."
-
-codex 确认三件事：①后端门控在上下文变更（visitTenantId 写入）之前拦截租户切换；②admin-web 与 miniapp 两端一致门控 `visit-tenant-id` 头注入与切换入口 UI；③测试经检视未在沙箱执行（通过性以交付时本机 `SecurityFilterChainFixtureTest,CrossTenantVisitEnabledFixtureTest` 33/33 + 前端 ts 基线证据为准）。
-
-## 验证（交付时点证据，沿用卡片开发记录）
-
-- Maven `-pl :zszj-spring-boot-starter-biz-tenant "-Dtest=SecurityFilterChainFixtureTest,CrossTenantVisitEnabledFixtureTest" test`：33 tests / 0 failures（2026-09-09T13:26:09+08:00）
-- admin-web `verify-ts-baseline.mjs`（currentErrors 11 = baseline 11）、miniapp `vue-tsc --noEmit` 0 错误
-
-## 边界
-
-获批业务组织方案（D-09）后的受控跨组织授权（服务端授权记录/策略、正向矩阵）归 ZS-SEC-001.B（B08），本子项仅关闭旧放大能力。
diff --git a/docs/reviews/codex-ZS-SEC-002.md b/docs/reviews/codex-ZS-SEC-002.md
deleted file mode 100644
index 76e8a2ee..00000000
--- a/docs/reviews/codex-ZS-SEC-002.md
+++ /dev/null
@@ -1,24 +0,0 @@
-# ZS-SEC-002 codex 评审处置（HANDOFF 补评，5×P2 登记处置）
-
-- 评审工具：codex（gpt-6-astra / xhigh，read-only sandbox，`codex review --commit 3fe87023` @ main）
-- 评审对象：ZS-SEC-002 接口分类、匿名白名单与方法权限清单（HANDOFF-ZS-SEC-002 交接单的待评审提交；2026-09-14 补评入库）
-- 结论：**0×P0/P1，5×P2 全部登记处置**（按 reviews/README「后续处理约定」第 2 条，非阻塞）
-- 原始日志：`outputs/handoff/ZS-SEC-002.txt`（未入库；本档案为处置入库）
-
-## 发现与处置（5×P2，全部指向 `ApiInventoryTest` 静态扫描器）
-
-| # | 发现 | 处置 |
-|---|---|---|
-| 1 | **Fail verification when the committed baseline is missing**（L84-86）：从 checkout 省略基线文件运行时，验证分支静默写入当前清单再与自身比对——路由/权限变化在匿名目录不变时可不带 `api.inventory.update=true` 通过 | 登记处置：门禁硬化项——基线缺失必须失败、仅 `update=true` 允许创建。归 ZS-SEC-002 后续小卡（与 ZS-OPS-001.C 门禁收口同窗处理；当前 CI 固定提交环境基线恒存在，实际漏检需多重条件叠加） |
-| 2 | **Exclude block comments before scanning annotations**（L231-233）：行首 `*` Javadoc 行已忽略，但整段块注释（非行首 `*` 形态）中的注解样例可能被误计 | 登记处置：扫描器健壮性——真实 Controller 无此类形态（交付时 PERM=251 与 grep 差 2 已实证注释识别有效），归同上小卡 |
-| 3 | **Expand every class-level request-mapping path**（L218-220）：类级 `@RequestMapping` 含占位符/多值时路径展开不完全 | 登记处置：现网 328 端点清单与运行时 HandlerMapping 一致性已由 SEC-012.B 端到端合同间接覆盖；归同上小卡 |
-| 4 | **Include effective class-level PreAuthorize guards**（L216-223）：类级 @PreAuthorize 的生效语义未并入权限判定 | 登记处置：启用模块无类级 @PreAuthorize 用法（清单 PERMISSION=251 全部方法级）；防御性登记归同上小卡 |
-| 5 | **Preserve authorization expression semantics in the baseline**（L318-324）：基线行仅存权限标识，不保留完整 SpEL 表达式语义（hasAnyAuthority/scope 组合） | 登记处置：结构断言已保证标识合法形态；表达式级语义审计归 ZS-SEC-002 后续小卡/B03 放行评审裁量 |
-
-## 结论原文（总述）
-
-codex 对清单生成器、主体类型推导（ADMIN=322/MEMBER=6）、匿名白名单三来源核清、双向漂移门禁均未发现 P0/P1 回归；5 项发现全部为建议级（P2）扫描器健壮性/门禁硬化项。
-
-## 验证（交付时点证据，沿用卡片开发记录）
-
-- `-pl :zszj-server -Dtest=ApiInventoryTest,ModuleWhitelistTest test`：8/8 BUILD SUCCESS；`-pl :zszj-spring-boot-starter-biz-tenant -Dtest=SecurityFilterChainFixtureTest`：35/35；`run-local-gates --fast` 10/10
diff --git a/docs/reviews/codex-ZS-SEC-005.md b/docs/reviews/codex-ZS-SEC-005.md
deleted file mode 100644
index d4c8a00f..00000000
--- a/docs/reviews/codex-ZS-SEC-005.md
+++ /dev/null
@@ -1,21 +0,0 @@
-# ZS-SEC-005 codex 评审处置（HANDOFF 补评，r0 一次通过）
-
-- 评审工具：codex（gpt-6-astra / xhigh，read-only sandbox，`codex review --commit e2b40185` @ main）
-- 评审对象：ZS-SEC-005 统一错误响应、HTTP 状态与失败日志结果（HANDOFF-ZS-SEC-005 交接单的待评审提交；2026-09-14 补评入库）
-- 结论：**r0 CLEAN / 0 发现**（评审通过，无需修复弧）
-- 原始日志：`outputs/handoff/ZS-SEC-005.txt`（未入库；本档案为处置入库）
-
-## 结论原文
-
-> "No actionable regressions were identified in the response-writer migration, exception handling, or added tests. Tests were not run because the environment is read-only."
-
-codex 确认 `WebFrameworkUtils.writeJSON` 统一出口的 6 文件 8 处 filter-direct 写点迁移（TokenAuthenticationFilter 401、AccessDeniedHandler 403、AuthenticationEntryPoint 401、TenantSecurityWebFilter 403/400/异常 3 点、DemoFilter 901、ApiEncryptFilter）、畸形 JSON 归 400 与 InvalidFormatException 分支不回显原始入参、访问日志按业务码记录（401/403/429/5xx 不再误记成功）均无回归。
-
-## 验证（交付时点证据，沿用卡片开发记录）
-
-- 复用 ZS-SEC-012.A 夹具第 9 组 `UnifiedErrorResponse`（6 用例），biz-tenant 41/41、web+biz-tenant Maven 全绿
-- 方案 B（改真实 HTTP 状态码）因会断 Web axios 令牌刷新链经评审否决，保留 HTTP 200+业务码契约并登记于 [错误响应与状态码矩阵](../../services/zhongshu-core/docs/错误响应与状态码矩阵.md)
-
-## 边界
-
-真实环境两端刷新/跳转/下载全面验收归 ZS-SEC-012.B（已收口）；日志脱敏归 ZS-SEC-007（本轮补评另见 codex-ZS-SEC-007.md，其 P1 已由 hotfix 修复）；trace 关联归 ZS-SEC-006。
diff --git a/docs/reviews/codex-ZS-SEC-007.md b/docs/reviews/codex-ZS-SEC-007.md
deleted file mode 100644
index 5620e9d7..00000000
--- a/docs/reviews/codex-ZS-SEC-007.md
+++ /dev/null
@@ -1,20 +0,0 @@
-# ZS-SEC-007 codex 评审处置（HANDOFF 补评，1×P1+1×P2 → hotfix 修复）
-
-- 评审工具：codex（gpt-6-astra / xhigh，read-only sandbox，`codex review --commit 332b4d7c` @ main）
-- 评审对象：ZS-SEC-007 统一访问、异常与保护切面的日志脱敏（HANDOFF-ZS-SEC-007 交接单的待评审提交；2026-09-14 补评入库）
-- 结论：**r0 发现 1×P1 + 1×P2 → hotfix `feat/sec-007-fix` 修复（见 codex-hotfix-SEC-007-P1.md）**
-- 原始日志：`outputs/handoff/ZS-SEC-007.txt`（未入库；本档案为处置入库）
-
-## r0 发现（2 项，均指向 `LogSanitizeUtils`）
-
-| 级别 | 发现 | 处置 |
-|---|---|---|
-| P1 | **Materialize raw JSON values before redacting the response tree**（L126-129）：DTO 含 `@JsonRawValue` 字段（现网 `AppDiyPagePropertyRespVO.property` 已在用）时，`valueToTree` 将原文保留为 `POJONode`，`sanitizeNode` 跳过该节点类型——内嵌原文含 `{"password":"SECRET"}` 可原样进入日志。旧版「先序列化再解析树」实现本可遍历到 | hotfix 修复：POJONode 物化为普通 JSON 节点后再脱敏（覆盖全部 `valueToTree` 入口）+ `@JsonRawValue` 回归测试 |
-| P2 | **Make sensitive-key normalization locale-independent**（L220）：`toLowerCase()` 未带 `Locale.ROOT`，土耳其/阿塞拜疆等默认 locale 的 JVM 上 ASCII `I` 变 ı，`AUTHORIZATION`/`PRIVATEKEY`/`X-API-KEY` 不再命中敏感根集、凭据明文落日志 | hotfix 一并修复：`toLowerCase(Locale.ROOT)` + 土耳其 locale 回归测试 |
-
-codex 总述："The sanitizer can emit credentials from embedded raw JSON and from uppercase sensitive keys under certain JVM locales. Both cases bypass the intended redaction."
-
-## 边界说明
-
-- P1 的实际暴露面为 `responseBody` 日志（当前默认 `responseEnable=false` 不记响应体），且交付时点尚未有 DIU 页面数据；但脱敏器合同是「脱敏失败只记摘要、绝不回退原文」，POJONode 旁路违反合同本体，按 P1 修复不降级。
-- 交付时点证据沿用卡片记录：`LogSanitizeUtilsTest`（common 10）+ 5 写点组件测试（Idempotent/RateLimiter/ApiSignature/ApiAccessLogFilter/GlobalExceptionHandler）全绿。
diff --git a/docs/reviews/codex-ZS-SEC-012.A.md b/docs/reviews/codex-ZS-SEC-012.A.md
deleted file mode 100644
index 8425b867..00000000
--- a/docs/reviews/codex-ZS-SEC-012.A.md
+++ /dev/null
@@ -1,20 +0,0 @@
-# ZS-SEC-012.A codex 评审处置（HANDOFF 补评，r0 一次通过）
-
-- 评审工具：codex（gpt-6-astra / xhigh，read-only sandbox，`codex review --commit a4d9c6e1` @ main）
-- 评审对象：ZS-SEC-012.A 真实安全链失败夹具（HANDOFF-ZS-SEC-012.A 交接单的待评审提交；2026-09-14 补评入库）
-- 结论：**r0 CLEAN / 0 发现**（评审通过，无需修复弧）
-- 原始日志：`outputs/handoff/ZS-SEC-012.A.txt`（未入库；本档案为处置入库）
-
-## 结论原文
-
-> "No actionable regressions were identified. The fixture matches the security and tenant behavior at this commit, and the removed test dependency is unused."
-
-codex 确认 `SecurityFilterChainFixtureTest` 夹具与该提交时点的安全/租户行为一致（真实 Filter 链全装配、未禁用过滤器、双技术租户可构造、基线失败如实暴露），security starter 移除的死测试依赖正确。
-
-## 验证（交付时点证据，沿用卡片开发记录）
-
-- Maven `-pl :zszj-spring-boot-starter-biz-tenant -Dtest=SecurityFilterChainFixtureTest test`：31 用例全绿（后随 SEC-002/005 扩展至 35+6 组）；两个真实行为发现（MockHttpServletRequest servletPath、TenantSecurityWebFilter 校验顺序）已按真实代码路径修正测试预期并注释。
-
-## 边界
-
-CORS/文件错误/异步派发/trace/畸形 JSON/双端合同同步及真实 PG/Redis 场景归 ZS-SEC-012.B（已收口，含 async/CORS 端到端扩展）；业务组织模型待 D-09。
diff --git a/docs/reviews/codex-hotfix-SEC-007-P1.md b/docs/reviews/codex-hotfix-SEC-007-P1.md
deleted file mode 100644
index 0762e4ae..00000000
--- a/docs/reviews/codex-hotfix-SEC-007-P1.md
+++ /dev/null
@@ -1,39 +0,0 @@
-# ZS-SEC-007 hotfix codex 评审处置（r0→r1→r2 三弧，首个「修复自身引入新 P1 被复评揪出」的主会话 hotfix）
-
-- 评审工具：codex（gpt-6-astra / xhigh，read-only sandbox，`codex review --commit <SHA>` @ worktree `E:/zszj-wt-sec-007`）
-- 评审对象：分支 `feat/sec-007`（HANDOFF 补评 ZS-SEC-007 所发现 1×P1+1×P2 的修复）
-- 结论：**r2 CLEAN / 0 发现**（评审收敛）
-- 原始日志：worktree `outputs/sec007-codex-r1.txt`/`r2.txt`（未入库；本档案为处置入库）
-
-## 修复弧总览
-
-| 弧 | 提交 | 结论 | 说明 |
-|---|---|---|---|
-| 补评 r0 | `332b4d7c`（被审对象） | FAIL 1×P1+1×P2 | 见 [codex-ZS-SEC-007.md](codex-ZS-SEC-007.md) |
-| hotfix r1 | `9728b369`（修复提交） | FAIL 1×P1（新引入） | 见下 |
-| hotfix r2 | `39c8b8dd`（fail-closed 修正） | **CLEAN / 0 发现** | 「Materialization failures now reach the existing safe-summary handlers instead of returning potentially sensitive raw text.」 |
-
-## r0 发现 → 修复（`9728b369`，2 files +125/-4）
-
-| 级别 | 发现 | 修复 |
-|---|---|---|
-| P1 | `sanitizeNode` 跳过 POJONode——`@JsonRawValue` 字段经 `valueToTree` 保留为 POJONode，内嵌原文穿透响应日志脱敏 | 新增 `toSanitizableTree`/`materializeRaw`：脱敏前统一物化（对象/数组递归复制；POJONode 解包），三处 `valueToTree` 入口（sanitizeObject/sanitizeArgValue/sanitizeResponseBody）全接。**实现中发现 POJONode 内包的是 Jackson `RawValue` 包装器而非裸 String**（RED 调试实证输出 `[RawValue of type java.lang.String]`），须经 `rawValue()` 解包后再 `readTree` |
-| P2 | `normalizeKey` 的 `toLowerCase()` 缺 `Locale.ROOT`，土耳其 locale 下大写敏感键（AUTHORIZATION/PRIVATEKEY/X-API-KEY）不命中 | `toLowerCase(Locale.ROOT)` |
-
-## r1 新发现（修复自身引入）→ 修复（`39c8b8dd`，1 file +3/-7）
-
-| 级别 | 发现 | 修复 |
-|---|---|---|
-| P1 | **物化失败 fallback 携带原文**：初版 `catch → TextNode.valueOf(String.valueOf(effective))` 把对象 `toString()`（可含 `password=...` 形态凭据）转成 TextNode，而 `sanitizeNode` 跳过 TextNode——该路径此前走「序列化失败 → 安全摘要」，修复反而引入可复现泄露（sanitizeMap/sanitizeArgs/sanitizeResponseBody 三入口可复现） | **fail-closed**：物化失败一律上抛 `IllegalStateException`，交既有外层 catch 降级为安全摘要/占位（恢复修复前的失败语义） |
-
-## 验证
-
-- TDD：3 回归测试先行（responseBody raw 字段掩码 / sanitizeMap raw 字段掩码 / 土耳其 locale 大写敏感键），RED 3 失败实证两项发现 → GREEN
-- `zszj-common` 全量：**13 tests / 0 failures，BUILD SUCCESS**（10 既有 + 3 新增）
-- 合并：`--no-ff` 合并回 main（`a1e3a505`）
-
-## 经验登记
-
-1. **Jackson `@JsonRawValue` × `valueToTree` 的真实形态**：POJONode 内包 `RawValue` 包装器（非裸 String），须经 `rawValue()` 解包——凭 RED 调试输出 `[RawValue of type java.lang.String]` 实证，纯推理会漏。
-2. **脱敏器的失败路径也是安全面**：任何「无法处理 → 保留原文」的 fallback 都违反 fail-closed 合同；正确语义是上抛交安全摘要。
-3. 与 ZS-SEC-011.A r0→r1「mock 假 GREEN」、ZS-LOGIN-001 r1「缓存态门控」同类：**安全修复本身须经复评直至归零**，本例为主会话直接 hotfix 流程首次触发 r1 揪出新 P1。
diff --git a/services/zhongshu-core/docs/BPM引擎表与业务扩展表迁移责任决策.md b/services/zhongshu-core/docs/BPM引擎表与业务扩展表迁移责任决策.md
new file mode 100644
index 00000000..17c8f516
--- /dev/null
+++ b/services/zhongshu-core/docs/BPM引擎表与业务扩展表迁移责任决策.md
@@ -0,0 +1,65 @@
+# BPM 引擎表与业务扩展表迁移责任决策（ZS-BPM-001）
+
+> 文档日期：2026-09-14（ZS-BPM-001 技术准备先行件交付，B09 批次开工项）
+> 上位依据：[03-底座二次开发顺序与验收标准](../../../docs/03-底座二次开发顺序与验收标准.md) B09（前置 B05/B08 与 D-07 批准，本卡「技术准备可先行」）、[05-任务清单](../../../docs/05-底座模块分析与开发任务清单.md) ZS-BPM-001 卡、[02-需求台账](../../../docs/02-一期底座需求规格与待决策台账.md) §6.5（D-07 确认前允许 PG 适配与中性技术夹具、禁止固化业务字段/状态）
+> 执行入口：`node scripts/db/run-bpm001-verify.mjs`（已注册为 `run-pg-regression.mjs` 第 9 套件，本地与 CI 同规则）
+
+## 1. 决策结论
+
+1. **Flowable 引擎表（`ACT_*`）由引擎 schema 管理器自管，禁止任何手写 DDL 或 Flyway 接管。**
+   建表/升级只允许发生在「迁移期」：以库 owner 账号（`zhongshu_owner`）在部署窗口执行
+   `database-schema-update=true` 的引导（本验收阶段 1 即此形态）。引擎版本由
+   `zszj-dependencies` 的 `flowable.version`（当前 8.0.0）钉住；引擎升级 = Flowable 自身
+   schema 升级机制执行，升级前须在真实 PG 演练（同库 `schema-update=true` 幂等引导 +
+   `ACT_GE_PROPERTY.schema.version` 前后比对，S2 检查守护）。
+2. **运行期零 DDL：低权限运行账号（`zhongshu_app`）以 `database-schema-update=false` 启动。**
+   运行账号对引擎表仅有 DML 权限（`env-setup-test.sql` 的 default privileges 自动授权
+   owner 新建表），S1 检查直证其建表被拒。这同时满足 B02「低权限运行账号不能建库/越权」
+   反向验收在 BPM 域的延伸。
+3. **BPM 业务扩展表（`bpm_form_info`、`bpm_user_group`、`bpm_process_instance_ext` 等
+   模块自有表）走 Flyway `V*__*.sql` 版本化迁移，与引擎表责任严格分离。** 未来启用
+   zszj-server 的 BPM 装配（现在 server POM 与根 reactor 均保持注释=关闭模块，B01 白名单
+   门禁守护）的批次，必须同时新增业务扩展表迁移；不得依赖引擎 schema 管理器创建业务表，
+   也不得用 Flyway 创建/修改任何 `ACT_*` 表。当前 V1 基线不含任何 `ACT_*`/`bpm_*` 表
+   （S0 检查：全基线零流程表 = 关闭 BPM 无流程路由/后台副作用的结构性证明）。
+4. **夹具边界（D-07 门禁）：** 验收只使用中性技术夹具（`tech_neutral_approval` 人工审批、
+   `tech_neutral_async_echo` 异步回声），不导入请假示例，不固化任何众墅业务对象、状态机、
+   组织任职语义；引擎 `tenantId` 在夹具中验证为「技术标签、无 ACL」（跨租户不过滤查询可见），
+   业务组织/对象隔离归 B08/D-07 后的 ZS-BPM-002/003。
+
+## 2. 验收断言与证据位置（2026-09-14 实测）
+
+| 验收项（ZS-BPM-001 卡） | 实现位置 | 结果 |
+|---|---|---|
+| PG 上部署、发起、通过、拒绝、撤回、转办、分页/历史查询 | `BpmPgHarnessBootstrapTest`（部署幂等/版本钉住/发起/租户标签）+ `BpmPgHarnessRuntimeTest` R20~R80 | 全部 PASS（见 outputs/bpm-001/runtime-report.json） |
+| 重复部署可恢复（幂等） | bootstrap：同内容重复部署 deployment 数不变 | PASS |
+| 重启可恢复 | 阶段 2 以独立 JVM、`schema-update=false`、低权限账号重启接入同库：定义/实例/历史/schema.version 全部一致（R10） | PASS |
+| 事务失败可恢复 | R60：Spring `DataSourceTransactionManager` 回滚事务内的发起不留引擎痕迹；提交事务正常持久化 | PASS |
+| 异步执行器与启停 | bootstrap 执行器挂起留积压（`ACT_RU_JOB`=1、探针零记录）→ runtime 重启后积压跨重启持久保留（执行器不自启），显式 `asyncExecutor.start()` 消化积压且无死信、实时路径再验、`shutdown()` 回到非活动态（R70，isActive 直证） | PASS |
+| 关闭 BPM 无副作用 | S0：System/Infra 全基线零 `ACT_`/`FLW_` 表；server POM/根 reactor 保持 BPM 注释 + `ModuleWhitelistTest` 门禁不变 | PASS |
+| 装配扩展点同构直证 | `BpmPgHarnessConfiguration` 与 `BpmFlowableConfiguration` 同用 `SpringProcessEngineConfiguration` + `setEventListeners` 扩展点，R90 证明监听器在真实 PG 引擎触发 | PASS |
+
+两阶段账号/参数：阶段 1 `zhongshu_owner` + `SCHEMA_UPDATE=true` + `ASYNC_EXECUTOR=false`；
+阶段 2 `zhongshu_app` + `SCHEMA_UPDATE=false` + `ASYNC_EXECUTOR=false`（执行器由 R70 显式
+`start()`/`shutdown()` 控制生命周期，保证重启恢复断言的确定性）。编排器注入
+`ZSZJ_BPM_HARNESS_*` 环境变量，缺失即快速失败，不静默跳过。
+
+## 3. 运行注意
+
+- 首次运行会联网拉取 Flowable 8.0.0 依赖树（此前 server 未启用 BPM，本地仓库无 Flowable）。
+- zszj-module-bpm 不在默认 reactor（根 POM 注释态），套件以 `mvn -f zszj-module-bpm/pom.xml`
+  独立构建，兄弟模块依赖取自本地仓库已安装产物；**不通过根 POM profile 启用 reactor 成员**
+  （`ModuleWhitelistTest` 以根 POM 非注释 `<module>` 为门禁事实源，引入 profile 会被判违规）。
+- 就绪探测必须走 TCP（`-h 127.0.0.1`）：postgres 镜像 initdb 期间的临时服务器只监听
+  unix socket，socket 探测可能误判就绪导致建库落到临时服务器失败（2026-09-14 首跑实测）。
+  **移交给量**：本套件已按此修复；`run-db006/008-verify.mjs` 等既有套件仍是 socket 探测，
+  2026-09-14 聚合首跑中二者再次竞态失败、单独复跑即绿——历史登记的「Docker 负载 flaky」
+  相当部分可能即此根因，建议后续批次统一改为 TCP 探测（属各套件所属卡，不在本件改动）。
+
+## 4. 边界与后续
+
+- 本件为 ZS-BPM-001「技术准备先行」交付：**B09 主卡链路仍受门禁**——ZS-BPM-002 审批资格
+  与对象授权待 ZS-PERM-002.B（B08）；ZS-BPM-003 首链领域状态机待 D-07 批准 + B05/B08；
+  ZS-OPS-001.E 门禁待 ZS-OPS-001.D。B09 整批放行不因本件提前。
+- 正式首链启用 BPM 时须：server POM/根 reactor 解注释 + `ModuleWhitelist.ENABLED_MODULES`
+  同步 + 业务扩展表 Flyway 迁移（§1.3）+ 以 owner 账号执行引擎表引导。
diff --git a/services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common/util/log/LogSanitizeUtils.java b/services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common/util/log/LogSanitizeUtils.java
index 1aa4117d..9957b218 100644
--- a/services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common/util/log/LogSanitizeUtils.java
+++ b/services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common/util/log/LogSanitizeUtils.java
@@ -6,17 +6,12 @@ import cn.hutool.core.util.StrUtil;
 import cn.zszj.framework.common.util.json.JsonUtils;
 import com.fasterxml.jackson.databind.JsonNode;
 import com.fasterxml.jackson.databind.ObjectMapper;
-import com.fasterxml.jackson.databind.node.ArrayNode;
-import com.fasterxml.jackson.databind.node.NullNode;
-import com.fasterxml.jackson.databind.node.POJONode;
 import com.fasterxml.jackson.databind.node.ObjectNode;
-import com.fasterxml.jackson.databind.util.RawValue;
 import lombok.extern.slf4j.Slf4j;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Iterator;
-import java.util.Locale;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
@@ -128,7 +123,7 @@ public class LogSanitizeUtils {
         }
         Set<String> extra = normalizeKeys(extraKeys);
         try {
-            JsonNode node = toSanitizableTree(result);
+            JsonNode node = mapper().valueToTree(result);
             JsonNode data = node.get("data");
             if (data != null) {
                 sanitizeNode(data, extra);
@@ -142,7 +137,7 @@ public class LogSanitizeUtils {
     private static String sanitizeObject(Object obj, String... extraKeys) {
         Set<String> extra = normalizeKeys(extraKeys);
         try {
-            JsonNode node = toSanitizableTree(obj);
+            JsonNode node = mapper().valueToTree(obj);
             sanitizeNode(node, extra);
             return truncate(mapper().writeValueAsString(node));
         } catch (Throwable t) {
@@ -155,7 +150,7 @@ public class LogSanitizeUtils {
             return "null";
         }
         try {
-            JsonNode node = toSanitizableTree(arg);
+            JsonNode node = mapper().valueToTree(arg);
             sanitizeNode(node, extra);
             return mapper().writeValueAsString(node);
         } catch (Throwable t) {
@@ -163,56 +158,6 @@ public class LogSanitizeUtils {
         }
     }
 
-
-    /**
-     * 物化 POJONode：@JsonRawValue 等原文直出字段经 valueToTree 保留为 POJONode，
-     * 递归脱敏会跳过非对象/数组节点导致原文穿透（SEC-007 HANDOFF 补评 P1）。
-     * 统一在脱敏前把整棵树物化为普通节点（String 原文按 JSON 解析，解析失败降级为文本节点）。
-     */
-    private static JsonNode toSanitizableTree(Object obj) {
-        return materializeRaw(mapper().valueToTree(obj));
-    }
-
-    private static JsonNode materializeRaw(JsonNode node) {
-        if (node == null || node.isNull() || (node.isValueNode() && !node.isPojo())) {
-            return node;
-        }
-        if (node.isPojo()) {
-            Object pojo = ((POJONode) node).getPojo();
-            // @JsonRawValue 场景 POJONode 包装的是 RawValue（getValue() 才是原文）
-            Object effective = pojo instanceof RawValue ? ((RawValue) pojo).rawValue() : pojo;
-            if (effective == null) {
-                return NullNode.getInstance();
-            }
-            if (effective instanceof String) {
-                try {
-                    return materializeRaw(mapper().readTree((String) effective));
-                } catch (Throwable t) {
-                    // fail-closed：原文不可解析时不得降级为携带原文的 TextNode（r1 P1），交外层安全摘要
-                    throw new IllegalStateException("Raw value is not valid JSON", t);
-                }
-            }
-            return materializeRaw(mapper().valueToTree(effective));
-        }
-        if (node.isObject()) {
-            ObjectNode copy = mapper().createObjectNode();
-            Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
-            while (iterator.hasNext()) {
-                Map.Entry<String, JsonNode> entry = iterator.next();
-                copy.set(entry.getKey(), materializeRaw(entry.getValue()));
-            }
-            return copy;
-        }
-        if (node.isArray()) {
-            ArrayNode copy = mapper().createArrayNode();
-            for (JsonNode child : node) {
-                copy.add(materializeRaw(child));
-            }
-            return copy;
-        }
-        return node;
-    }
-
     /**
      * 递归脱敏 JSON 节点：数组逐元素递归，对象命中敏感键则掩码、否则递归其值
      */
@@ -272,7 +217,7 @@ public class LogSanitizeUtils {
     }
 
     private static String normalizeKey(String key) {
-        return key.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
+        return key.toLowerCase().replace("_", "").replace("-", "");
     }
 
     private static String truncate(String text) {
diff --git a/services/zhongshu-core/zszj-framework/zszj-common/src/test/java/cn/zszj/framework/common/util/log/LogSanitizeUtilsTest.java b/services/zhongshu-core/zszj-framework/zszj-common/src/test/java/cn/zszj/framework/common/util/log/LogSanitizeUtilsTest.java
index 922b1b5b..3476b82f 100644
--- a/services/zhongshu-core/zszj-framework/zszj-common/src/test/java/cn/zszj/framework/common/util/log/LogSanitizeUtilsTest.java
+++ b/services/zhongshu-core/zszj-framework/zszj-common/src/test/java/cn/zszj/framework/common/util/log/LogSanitizeUtilsTest.java
@@ -196,66 +196,4 @@ class LogSanitizeUtilsTest {
         }
     }
 
-
-    // ========== SEC-007 HANDOFF 补评 hotfix：POJONode 原文穿透 + Locale 无关归一 ==========
-
-    /**
-     * 模拟现网 @JsonRawValue 字段形态（如 AppDiyPagePropertyRespVO.property）：
-     * valueToTree 会把原文直出字段保留为 POJONode(RawValue)，递归脱敏不得跳过
-     */
-    static class RawValueDto {
-        @com.fasterxml.jackson.annotation.JsonRawValue
-        private final String property = "{\"password\":\"" + SECRET + "\",\"title\":\"page\"}";
-        private final String name = "demo";
-
-        public String getProperty() {
-            return property;
-        }
-
-        public String getName() {
-            return name;
-        }
-    }
-
-    @Test
-    void sanitizeResponseBody_rawJsonField_shouldBeTraversedAndMasked() {
-        cn.zszj.framework.common.pojo.CommonResult<RawValueDto> result =
-                cn.zszj.framework.common.pojo.CommonResult.success(new RawValueDto());
-
-        String sanitized = LogSanitizeUtils.sanitizeResponseBody(result);
-
-        assertFalse(sanitized.contains(SECRET)); // 原文穿透回归：SECRET 绝不出现
-        assertTrue(sanitized.contains("\"password\":\"***\"")); // raw 值内部敏感键掩码
-        assertTrue(sanitized.contains("\"title\":\"page\"")); // raw 值内部非敏感字段保留
-        assertTrue(sanitized.contains("\"name\":\"demo\"")); // 常规字段原样
-    }
-
-    @Test
-    void sanitizeMap_rawJsonField_shouldBeTraversedAndMasked() {
-        java.util.Map<String, Object> map = new LinkedHashMap<>();
-        map.put("payload", new RawValueDto());
-
-        String sanitized = LogSanitizeUtils.sanitizeMap(map);
-
-        assertFalse(sanitized.contains(SECRET));
-        assertTrue(sanitized.contains("\"password\":\"***\""));
-    }
-
-    @Test
-    void sanitizeJson_turkishLocale_shouldStillMaskUppercaseSensitiveKeys() {
-        java.util.Locale original = java.util.Locale.getDefault();
-        try {
-            java.util.Locale.setDefault(new java.util.Locale("tr", "TR"));
-            String json = "{\"AUTHORIZATION\":\"Bearer " + SECRET + "\",\"PRIVATEKEY\":\"" + SECRET + "\"}";
-
-            String result = LogSanitizeUtils.sanitizeJson(json);
-
-            assertFalse(result.contains(SECRET)); // 土耳其 locale 下大写敏感键仍须命中
-            assertTrue(result.contains("\"AUTHORIZATION\":\"***\""));
-            assertTrue(result.contains("\"PRIVATEKEY\":\"***\""));
-        } finally {
-            java.util.Locale.setDefault(original);
-        }
-    }
-
 }

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "node -e \"const fs=require('fs'); for(const p of process.argv.slice(1)){ console.log(p);console.log(fs.readFileSync(p,'utf8').split('\\n').map((l,i)=>(i+1)+': '+l).join('\\n'));}\" scripts/db/run-pg-regression.mjs .github/workflows/pg-regression.yml services/zhongshu-core/sql/postgresql/env-setup-test.sql services/zhongshu-core/zszj-server/src/test/java/cn/zszj/server/ModuleWhitelistTest.java services/zhongshu-core/zszj-module-bpm/src/test/resources/cn/zszj/module/bpm/harness/neutral-approval.bpmn20.xml services/zhongshu-core/zszj-module-bpm/src/test/resources/cn/zszj/module/bpm/harness/neutral-approval-v2.bpmn20.xml services/zhongshu-core/zszj-module-bpm/src/test/resources/cn/zszj/module/bpm/harness/neutral-async-echo.bpmn20.xml" in E:\zszj-wt-bpm-001-a
 succeeded in 0ms:
scripts/db/run-pg-regression.mjs
1: /**
2:  * ZS-DB-019.B 地基：PG 技术回归聚合入口（本地与 CI 同一规则）。
3:  *
4:  * 串联执行全部真实 PG 验证套件（每套件自管理一次性容器，验后清理）：
5:  *   1. ZS-DB-006 序列主键/批量/并发/回滚/低权限（6 用例）
6:  *   2. ZS-DB-007 字段映射与逻辑删除合同（7 用例）
7:  *   3. ZS-DB-008 事务原子性与锁行为（6 用例）
8:  *   4. ZS-CFG-002.A 字典编码唯一约束（8 用例）
9:  *   5. ZS-DB-011~015 有界删除（5 表 × 有界/边界/空集）
10:  *   6. ZS-DB-018 ORM/手写 SQL/租户隔离（两技术租户 CRUD/分页/关联/批量/逻辑删除 + 全局表/忽略注解/系统清理合法范围）
11:  *   7. DB-016 Quartz 调度表结构级验证（委托核心语句）
12:  *   8. DB-017 PG 元数据测试表与注释/索引核对
13:  *   9. ZS-BPM-001 BPM 独立装配与 PG 验收（两阶段：owner 建表引导 + app 低权限运行）
14:  * 任一套件失败退出非零。
15:  * 用法：node scripts/db/run-pg-regression.mjs
16:  */
17: import { execFileSync, spawnSync } from 'node:child_process';
18: import { readFileSync } from 'node:fs';
19: import { fileURLToPath } from 'node:url';
20: import { join } from 'node:path';
21: 
22: const root = fileURLToPath(new URL('../../', import.meta.url));
23: const cases = [
24:   { id: 'ZS-DB-006 序列/批量/续号', cmd: ['node', 'scripts/db/run-db006-verify.mjs'] },
25:   { id: 'ZS-DB-007 字段映射与逻辑删除', cmd: ['node', 'scripts/db/run-db007-verify.mjs'] },
26:   { id: 'ZS-DB-008 事务与锁', cmd: ['node', 'scripts/db/run-db008-verify.mjs'] },
27:   { id: 'ZS-CFG-002.A 字典约束', cmd: ['node', 'scripts/db/run-cfg002-verify.mjs'] },
28:   { id: 'ZS-DB-011~015 有界删除', cmd: ['node', 'scripts/db/run-db011-015-verify.mjs'] },
29:   { id: 'ZS-DB-018 ORM/手写SQL/租户隔离', cmd: ['node', 'scripts/db/run-db018-verify.mjs'] },
30:   { id: 'DB-016 Quartz 调度表结构级验证', caseFile: 'scripts/db/cases/db016-quartz-schema.sql' },
31:   { id: 'DB-017 元数据测试表', caseFile: 'scripts/db/cases/db017-metadata-testtable.sql' },
32:   { id: 'ZS-BPM-001 BPM 独立装配与PG验收', cmd: ['node', 'scripts/db/run-bpm001-verify.mjs'] },
33: ];
34: 
35: let failed = false;
36: const summary = [];
37: for (const c of cases) {
38:   let ok = true, note = '';
39:   try {
40:     if (c.caseFile) {
41:       // DB-016/017：独立容器 + V1/V2 + 用例 SQL
42:       const port = 5532 + Math.floor(Math.random() * 400);
43:       const container = `zszj-pgreg-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
44:       const boot = (sql) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', 'postgres', '-d', 'zhongshu', '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql, encoding: 'utf8', maxBuffer: 32 * 1024 * 1024 });
45:       execFileSync('docker', ['run', '-d', '--name', container, '-e', 'POSTGRES_PASSWORD=pgreg', '-p', `127.0.0.1:${port}:5432`, 'postgres:17-alpine'], { stdio: 'ignore' });
46:       try {
47:         let ready = false;
48:         for (let i = 0; i < 30; i++) { const r = spawnSync('docker', ['exec', container, 'psql', '-U', 'postgres', '-At', '-c', 'SELECT 1'], { encoding: 'utf8' }); if (r.status === 0) { ready = true; break; } Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500); }
49:         if (!ready) throw new Error('PG 未就绪');
50:         execFileSync('docker', ['exec', container, 'psql', '-U', 'postgres', '-q', '-c', 'CREATE DATABASE zhongshu;'], { stdio: 'ignore' });
51:         const setup = readFileSync(join(root, 'services/zhongshu-core/sql/postgresql/env-setup-test.sql'), 'utf8');
52:         const s1 = spawnSync('docker', ['exec', '-i', container, 'psql', '-U', 'postgres', '-d', 'zhongshu', '-v', 'ON_ERROR_STOP=1', '-q'], { input: setup, encoding: 'utf8' });
53:         if (s1.status !== 0) throw new Error('角色授权失败');
54:         const v1 = readFileSync(join(root, 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260909.001__system_infra_baseline.sql'), 'utf8');
55:         const v2 = readFileSync(join(root, 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260909.002__infra_quartz_backfill.sql'), 'utf8');
56:         const s2 = spawnSync('docker', ['exec', '-i', container, 'psql', '-U', 'zhongshu_owner', '-d', 'zhongshu', '-v', 'ON_ERROR_STOP=1', '-q'], { input: v1 + '\n' + v2, encoding: 'utf8', maxBuffer: 16 * 1024 * 1024 });
57:         if (s2.status !== 0) throw new Error('V1/V2 执行失败');
58:         const sql = readFileSync(join(root, c.caseFile), 'utf8');
59:         const s3 = spawnSync('docker', ['exec', '-i', container, 'psql', '-U', 'zhongshu_owner', '-d', 'zhongshu', '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql, encoding: 'utf8' });
60:         if (s3.status !== 0) throw new Error('用例执行失败');
61:       } finally {
62:         try { execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { }
63:       }
64:     } else {
65:       const r = spawnSync(c.cmd[0], c.cmd.slice(1), { cwd: root, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024, stdio: ['ignore', 'pipe', 'pipe'] });
66:       if (r.status !== 0) throw new Error((r.stdout ?? '') + (r.stderr ?? ''));
67:     }
68:   } catch (e) {
69:     ok = false;
70:     note = String(e.message ?? e).split('\n')[0].slice(0, 160);
71:   }
72:   summary.push({ id: c.id, ok, note });
73:   console.log(`[${ok ? 'PASS' : 'FAIL'}] ${c.id} ${note}`);
74:   if (!ok) failed = true;
75: }
76: 
77: console.log('\n===== PG 技术回归汇总（ZS-DB-019.B 地基）=====');
78: for (const s of summary) console.log(`${s.ok ? 'PASS' : 'FAIL'}  ${s.id} ${s.note}`);
79: console.log(`合计 ${summary.length} 套件，失败 ${summary.filter((s) => !s.ok).length} 套件`);
80: process.exit(failed ? 1 : 0);
81: 
.github/workflows/pg-regression.yml
1: name: pg-regression
2: 
3: # ZS-DB-019.B 地基：PG 技术回归套件（一次性 PG17 容器，验后自动清理）。
4: # 与本地 `node scripts/db/run-pg-regression.mjs` 同一规则。
5: # 覆盖：ZS-DB-006 序列/批量/续号、007 字段映射与逻辑删除、008 事务与锁、
6: #       CFG-002.A 字典唯一约束、DB-011~015 有界删除、DB-016/017 结构级验证。
7: 
8: on:
9:   push:
10:     branches: [main]
11:   pull_request:
12: 
13: jobs:
14:   pg-regression:
15:     runs-on: ubuntu-latest
16:     steps:
17:       - uses: actions/checkout@v4
18:       - uses: actions/setup-node@v4
19:         with:
20:           node-version: 22
21:       - name: 运行 PG 技术回归套件
22:         run: node scripts/db/run-pg-regression.mjs
23: 
services/zhongshu-core/sql/postgresql/env-setup-test.sql
1: -- ZS-DB-002：按 docs/数据库环境方案.md 执行的建库/授权操作（本地独立测试环境实测脚本）
2: -- 以超管连接执行；口令仅用于一次性本地验证，不入任何环境配置
3: \set ON_ERROR_STOP on
4: 
5: -- 1. 角色（库级）
6: DO $$ BEGIN
7:   IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'zhongshu_owner') THEN
8:     CREATE ROLE zhongshu_owner LOGIN PASSWORD 'owner_local_1';
9:   END IF;
10:   IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'zhongshu_app') THEN
11:     CREATE ROLE zhongshu_app LOGIN PASSWORD 'app_local_1';
12:   END IF;
13: END $$;
14: 
15: -- 2. 库属主
16: ALTER DATABASE zhongshu OWNER TO zhongshu_owner;
17: 
18: -- 3. 授权（模板第 2 节）
19: GRANT ALL ON SCHEMA public TO zhongshu_owner;
20: GRANT USAGE ON SCHEMA public TO zhongshu_app;
21: GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO zhongshu_app;
22: GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO zhongshu_app;
23: ALTER DEFAULT PRIVILEGES FOR ROLE zhongshu_owner IN SCHEMA public
24:   GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO zhongshu_app;
25: ALTER DEFAULT PRIVILEGES FOR ROLE zhongshu_owner IN SCHEMA public
26:   GRANT USAGE, SELECT ON SEQUENCES TO zhongshu_app;
27: 
services/zhongshu-core/zszj-server/src/test/java/cn/zszj/server/ModuleWhitelistTest.java
1: package cn.zszj.server;
2: 
3: import org.junit.jupiter.api.Test;
4: 
5: import java.io.IOException;
6: import java.nio.file.Files;
7: import java.nio.file.Path;
8: import java.nio.file.Paths;
9: import java.util.LinkedHashSet;
10: import java.util.List;
11: import java.util.Set;
12: import java.util.regex.Matcher;
13: import java.util.regex.Pattern;
14: import java.util.stream.Collectors;
15: import java.util.stream.Stream;
16: 
17: import static org.junit.jupiter.api.Assertions.assertEquals;
18: import static org.junit.jupiter.api.Assertions.assertTrue;
19: 
20: /**
21:  * ZS-ENG-001：模块启用白名单测试。
22:  *
23:  * 不依赖 Spring 上下文，直接以 POM 与源码文本为事实来源校验：
24:  * 1. 根 pom.xml 实际激活的 zszj-module-* 与 ModuleWhitelist.ENABLED_MODULES 一致；
25:  * 2. zszj-server/pom.xml 实际引入的业务模块依赖与 ENABLED_MODULES 一致（依赖闭包决定 Bean/Job 装配）；
26:  * 3. DefaultController 的 @RequestMapping 覆盖所有未启用模块的 API 前缀，且不覆盖启用模块前缀；
27:  * 4. zszj-server 源码未引用任何未启用模块的 Java 包。
28:  */
29: class ModuleWhitelistTest {
30: 
31:     private static final Pattern MODULE_ARTIFACT = Pattern.compile("<artifactId>(zszj-module-[a-z-]+)</artifactId>");
32: 
33:     private String read(String relative) throws IOException {
34:         return Files.readString(Paths.get(relative));
35:     }
36: 
37:     private Set<String> activeModuleArtifacts(String pomText) {
38:         String withoutComments = pomText.replaceAll("(?s)<!--.*?-->", "");
39:         Set<String> result = new LinkedHashSet<>();
40:         Matcher matcher = MODULE_ARTIFACT.matcher(withoutComments);
41:         while (matcher.find()) {
42:             result.add(matcher.group(1).replace("zszj-module-", ""));
43:         }
44:         return result;
45:     }
46: 
47:     @Test
48:     void enabledModulesMatchRootPomActiveModules() throws IOException {
49:         // 根 pom 通过 <module> 标签声明激活模块
50:         String withoutComments = read("../pom.xml").replaceAll("(?s)<!--.*?-->", "");
51:         Matcher matcher = Pattern.compile("<module>(zszj-module-[a-z-]+)</module>").matcher(withoutComments);
52:         Set<String> rootModules = new LinkedHashSet<>();
53:         while (matcher.find()) {
54:             rootModules.add(matcher.group(1).replace("zszj-module-", ""));
55:         }
56:         assertEquals(new LinkedHashSet<>(ModuleWhitelist.ENABLED_MODULES), rootModules,
57:                 "根 pom.xml 激活的业务模块必须与白名单一致，启用/停用需同步 ModuleWhitelist");
58:     }
59: 
60:     @Test
61:     void enabledModulesMatchServerPomDependencies() throws IOException {
62:         // activeModuleArtifacts 已剥离 "zszj-module-" 前缀（与 enabledModulesMatchRootPomActiveModules 同一约定），
63:         // 故直接与 ENABLED_MODULES（无前缀，如 system/infra）比对。ZS-ENG-001 交付时本测试从未运行（当时无 JDK
64:         // 工具链，见 05 文档 ZS-ENG-001 开发记录「测试运行…待 B01 工具链复验」），断言误在期望侧补前缀、又在实际侧按前缀
65:         // 过滤已剥前缀的值，致实际恒为空、断言恒失败；ZS-SEC-002 首次以工具链运行 zszj-server 全测试时暴露并修复。
66:         Set<String> serverDeps = activeModuleArtifacts(read("pom.xml"));
67:         assertEquals(new LinkedHashSet<>(ModuleWhitelist.ENABLED_MODULES), serverDeps,
68:                 "zszj-server 依赖的业务模块必须与白名单一致：未启用模块不得进入依赖闭包（Bean/Job 装配边界）");
69:     }
70: 
71:     @Test
72:     void defaultControllerCoversAllDisabledPrefixes() throws IOException {
73:         String source = read(Paths.get("src", "main", "java", "cn", "zszj", "server",
74:                 "controller", "DefaultController.java").toString());
75:         List<String> declaredPrefixes = ModuleWhitelist.DISABLED_MODULE_API_PREFIXES.values().stream()
76:                 .flatMap(List::stream).collect(Collectors.toList());
77:         for (String prefix : declaredPrefixes) {
78:             assertTrue(source.contains("\"" + prefix + "\""),
79:                     "DefaultController 缺少未启用模块前缀的兜底映射: " + prefix);
80:         }
81:         for (String enabled : ModuleWhitelist.ENABLED_MODULES) {
82:             assertTrue(!source.contains("\"/admin-api/" + enabled + "/**\""),
83:                     "启用模块不应被兜底映射拦截: " + enabled);
84:         }
85:     }
86: 
87:     @Test
88:     void serverSourceDoesNotReferenceDisabledModulePackages() throws IOException {
89:         String modulePackagePrefix = "cn.zszj.module.";
90:         try (Stream<Path> paths = Files.walk(Paths.get("src", "main", "java"))) {
91:             for (Path file : paths.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList())) {
92:                 String text = Files.readString(file);
93:                 for (String disabled : ModuleWhitelist.DISABLED_MODULE_API_PREFIXES.keySet()) {
94:                     assertTrue(!text.contains(modulePackagePrefix + disabled + "."),
95:                             file + " 不得引用未启用模块的包: " + disabled);
96:                 }
97:             }
98:         }
99:     }
100: 
101: }
102: 
services/zhongshu-core/zszj-module-bpm/src/test/resources/cn/zszj/module/bpm/harness/neutral-approval.bpmn20.xml
1: <?xml version="1.0" encoding="UTF-8"?>
2: <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
3:              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
4:              xmlns:flowable="http://flowable.org/bpmn"
5:              targetNamespace="urn:zszj:bpm-pg-harness">
6:   <!-- ZS-BPM-001 中性技术夹具：不含任何众墅业务字段/状态语义（D-07 确认前禁令） -->
7:   <process id="tech_neutral_approval" name="技术中性审批夹具" isExecutable="true">
8:     <startEvent id="start"/>
9:     <sequenceFlow id="f1" sourceRef="start" targetRef="auditTask"/>
10:     <userTask id="auditTask" name="技术审核任务"/>
11:     <sequenceFlow id="f2" sourceRef="auditTask" targetRef="outcomeGateway"/>
12:     <exclusiveGateway id="outcomeGateway" name="结果分支"/>
13:     <sequenceFlow id="f3" sourceRef="outcomeGateway" targetRef="approvedEnd">
14:       <conditionExpression xsi:type="tFormalExpression">${harness_outcome == 'approve'}</conditionExpression>
15:     </sequenceFlow>
16:     <sequenceFlow id="f4" sourceRef="outcomeGateway" targetRef="rejectedEnd">
17:       <conditionExpression xsi:type="tFormalExpression">${harness_outcome == 'reject'}</conditionExpression>
18:     </sequenceFlow>
19:     <endEvent id="approvedEnd"/>
20:     <endEvent id="rejectedEnd"/>
21:   </process>
22: </definitions>
23: 
services/zhongshu-core/zszj-module-bpm/src/test/resources/cn/zszj/module/bpm/harness/neutral-approval-v2.bpmn20.xml
1: <?xml version="1.0" encoding="UTF-8"?>
2: <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
3:              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
4:              xmlns:flowable="http://flowable.org/bpmn"
5:              targetNamespace="urn:zszj:bpm-pg-harness">
6:   <!-- ZS-BPM-001 中性技术夹具 V2：同键改内容，验证变更部署产生新版本且旧实例钉在原版本 -->
7:   <process id="tech_neutral_approval" name="技术中性审批夹具 V2" isExecutable="true">
8:     <documentation>ZS-BPM-001 版本演进验证用：与 V1 仅名称/文档不同</documentation>
9:     <startEvent id="start"/>
10:     <sequenceFlow id="f1" sourceRef="start" targetRef="auditTask"/>
11:     <userTask id="auditTask" name="技术审核任务"/>
12:     <sequenceFlow id="f2" sourceRef="auditTask" targetRef="outcomeGateway"/>
13:     <exclusiveGateway id="outcomeGateway" name="结果分支"/>
14:     <sequenceFlow id="f3" sourceRef="outcomeGateway" targetRef="approvedEnd">
15:       <conditionExpression xsi:type="tFormalExpression">${harness_outcome == 'approve'}</conditionExpression>
16:     </sequenceFlow>
17:     <sequenceFlow id="f4" sourceRef="outcomeGateway" targetRef="rejectedEnd">
18:       <conditionExpression xsi:type="tFormalExpression">${harness_outcome == 'reject'}</conditionExpression>
19:     </sequenceFlow>
20:     <endEvent id="approvedEnd"/>
21:     <endEvent id="rejectedEnd"/>
22:   </process>
23: </definitions>
24: 
services/zhongshu-core/zszj-module-bpm/src/test/resources/cn/zszj/module/bpm/harness/neutral-async-echo.bpmn20.xml
1: <?xml version="1.0" encoding="UTF-8"?>
2: <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
3:              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
4:              xmlns:flowable="http://flowable.org/bpmn"
5:              targetNamespace="urn:zszj:bpm-pg-harness">
6:   <!-- ZS-BPM-001 中性技术夹具：异步服务任务（委托写探针表）验证异步执行器 -->
7:   <process id="tech_neutral_async_echo" name="技术异步回声夹具" isExecutable="true">
8:     <startEvent id="start"/>
9:     <sequenceFlow id="f1" sourceRef="start" targetRef="echoTask"/>
10:     <serviceTask id="echoTask" name="技术异步回声任务"
11:                  flowable:delegateExpression="${neutralEchoDelegate}"
12:                  flowable:async="true"/>
13:     <sequenceFlow id="f2" sourceRef="echoTask" targetRef="end"/>
14:     <endEvent id="end"/>
15:   </process>
16: </definitions>
17: 

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "rg -n -C 7 'ZS-BPM-001|6\\.5|技术准备可先行' docs/02* docs/03* docs/05*; rg -n 'tools|JAVA_HOME|maven|install|surefire|skipTests|flowable.version|zszj-module-bpm' scripts/db/run-db018-verify.mjs scripts/gov/*.mjs services/zhongshu-core/pom.xml services/zhongshu-core/zszj-dependencies/pom.xml services/zhongshu-core/zszj-server/pom.xml; rg --files outputs .github scripts/gov -g '*'; git diff main -- services/zhongshu-core/zszj-module-infra services/zhongshu-core/zszj-server/src/main/resources services/zhongshu-core/zszj-server/src/test/resources" in E:\zszj-wt-bpm-001-a
 succeeded in 0ms:
rg: docs/02*: 文件名、目录名或卷标语法不正确。 (os error 123)
rg: docs/03*: 文件名、目录名或卷标语法不正确。 (os error 123)
rg: docs/05*: 文件名、目录名或卷标语法不正确。 (os error 123)
rg: scripts/gov/*.mjs: 文件名、目录名或卷标语法不正确。 (os error 123)
services/zhongshu-core/zszj-dependencies/pom.xml:2:<project xmlns="http://maven.apache.org/POM/4.0.0"
services/zhongshu-core/zszj-dependencies/pom.xml:4:         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
services/zhongshu-core/zszj-dependencies/pom.xml:18:        <flatten-maven-plugin.version>1.7.2</flatten-maven-plugin.version>
services/zhongshu-core/zszj-dependencies/pom.xml:50:        <flowable.version>8.0.0</flowable.version>
services/zhongshu-core/zszj-dependencies/pom.xml:448:                <version>${flowable.version}</version>
services/zhongshu-core/zszj-dependencies/pom.xml:453:                <version>${flowable.version}</version>
services/zhongshu-core/zszj-dependencies/pom.xml:768:                <artifactId>flatten-maven-plugin</artifactId>
services/zhongshu-core/zszj-dependencies/pom.xml:769:                <version>${flatten-maven-plugin.version}</version>
services/zhongshu-core/zszj-server/pom.xml:2:<project xmlns="http://maven.apache.org/POM/4.0.0"
services/zhongshu-core/zszj-server/pom.xml:4:         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
services/zhongshu-core/zszj-server/pom.xml:82:<!--            <artifactId>zszj-module-bpm</artifactId>-->
services/zhongshu-core/zszj-server/pom.xml:213:                <artifactId>spring-boot-maven-plugin</artifactId>
services/zhongshu-core/pom.xml:2:<project xmlns="http://maven.apache.org/POM/4.0.0"
services/zhongshu-core/pom.xml:4:         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
services/zhongshu-core/pom.xml:19:<!--        <module>zszj-module-bpm</module>-->
services/zhongshu-core/pom.xml:45:        <maven.compiler.source>${java.version}</maven.compiler.source>
services/zhongshu-core/pom.xml:46:        <maven.compiler.target>${java.version}</maven.compiler.target>
services/zhongshu-core/pom.xml:47:        <maven-surefire-plugin.version>3.5.3</maven-surefire-plugin.version>
services/zhongshu-core/pom.xml:48:        <maven-compiler-plugin.version>3.14.0</maven-compiler-plugin.version>
services/zhongshu-core/pom.xml:49:        <flatten-maven-plugin.version>1.7.2</flatten-maven-plugin.version>
services/zhongshu-core/pom.xml:50:        <!-- maven-surefire-plugin 暂时无法通过 bom 的依赖读取（兼容老版本 IDEA 2024 及以前版本） -->
services/zhongshu-core/pom.xml:72:                <!-- maven-surefire-plugin 插件，用于运行单元测试。 -->
services/zhongshu-core/pom.xml:75:                    <groupId>org.apache.maven.plugins</groupId>
services/zhongshu-core/pom.xml:76:                    <artifactId>maven-surefire-plugin</artifactId>
services/zhongshu-core/pom.xml:77:                    <version>${maven-surefire-plugin.version}</version>
services/zhongshu-core/pom.xml:79:                <!-- maven-compiler-plugin 插件，解决 spring-boot-configuration-processor + Lombok + MapStruct 组合 -->
services/zhongshu-core/pom.xml:82:                    <groupId>org.apache.maven.plugins</groupId>
services/zhongshu-core/pom.xml:83:                    <artifactId>maven-compiler-plugin</artifactId>
services/zhongshu-core/pom.xml:84:                    <version>${maven-compiler-plugin.version}</version>
services/zhongshu-core/pom.xml:119:                    <artifactId>flatten-maven-plugin</artifactId>
services/zhongshu-core/pom.xml:128:                <groupId>org.apache.maven.plugins</groupId>
services/zhongshu-core/pom.xml:129:                <artifactId>maven-enforcer-plugin</artifactId>
services/zhongshu-core/pom.xml:155:                <artifactId>flatten-maven-plugin</artifactId>
services/zhongshu-core/pom.xml:156:                <version>${flatten-maven-plugin.version}</version>
services/zhongshu-core/pom.xml:186:            <url>https://mirrors.huaweicloud.com/repository/maven/</url>
services/zhongshu-core/pom.xml:189:            <id>aliyunmaven</id>
services/zhongshu-core/pom.xml:191:            <url>https://maven.aliyun.com/repository/public</url>
scripts/gov\verify-docs.test.mjs
scripts/gov\verify-docs.mjs
scripts/gov\task-stats.mjs
scripts/gov\close-task.mjs
.github\workflows\pg-regression.yml
.github\workflows\local-baseline-gates.yml
outputs\bpm-001\runtime.log
outputs\bpm-001\runtime-report.json
outputs\bpm-001\bootstrap.log
warning: unable to access 'C:\Users\Administrator/.config/git/ignore': Permission denied
diff --git a/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/controller/admin/file/FileController.java b/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/controller/admin/file/FileController.java
index 314947ed..8bb9de71 100644
--- a/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/controller/admin/file/FileController.java
+++ b/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/controller/admin/file/FileController.java
@@ -7,7 +7,7 @@ import cn.zszj.framework.common.pojo.CommonResult;
 import cn.zszj.framework.common.pojo.PageResult;
 import cn.zszj.framework.common.util.http.HttpUtils;
 import cn.zszj.framework.common.util.object.BeanUtils;
-import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
+import cn.zszj.framework.tenant.core.aop.TenantIgnore;
 import cn.zszj.module.infra.controller.admin.file.vo.file.*;
 import cn.zszj.module.infra.dal.dataobject.file.FileDO;
 import cn.zszj.module.infra.service.file.FileService;
@@ -66,6 +66,12 @@ public class FileController {
         return success(fileService.presignPutUrl(name, directory));
     }
 
+    @PostMapping("/create")
+    @Operation(summary = "创建文件", description = "模式二：前端上传文件：配合 presigned-url 接口，记录上传了上传的文件")
+    public CommonResult<Long> createFile(@Valid @RequestBody FileCreateReqVO createReqVO) {
+        return success(fileService.createFile(createReqVO));
+    }
+
     @GetMapping("/get")
     @Operation(summary = "获得文件")
     @Parameter(name = "id", description = "编号", required = true)
@@ -93,7 +99,8 @@ public class FileController {
     }
 
     @GetMapping("/{configId}/get/**")
-    @PermitAll // ZS-FILE-001.A：匿名仅可读 PUBLIC 公开素材；PRIVATE 在 service 校验登录+同租户
+    @PermitAll
+    @TenantIgnore
     @Operation(summary = "下载文件")
     @Parameter(name = "configId", description = "配置编号", required = true)
     public void getFileContent(HttpServletRequest request,
@@ -109,16 +116,6 @@ public class FileController {
         // https://gitee.com/zhijiantianya/ruoyi-vue-pro/pulls/1432/
         path = HttpUtils.decodeUrlPath(path);
 
-        // ZS-FILE-001.A（codex r0 P2）：跨租户定位记录——PUBLIC 对任意来源同址可用；
-        // PRIVATE 的租户归属校验以记录自身 tenant_id 执行（忽略请求携带租户，防租户过滤 404 误伤公开素材）
-        FileDO file = fileService.getFileByConfigIdAndPathIgnoreTenant(configId, path);
-        if (file == null) {
-            log.warn("[getFileContent][configId({}) path({}) 文件不存在]", configId, path);
-            response.setStatus(HttpStatus.NOT_FOUND.value());
-            return;
-        }
-        fileService.validateFileReadable(file, SecurityFrameworkUtils.getLoginUser());
-
         // 读取内容
         byte[] content = fileService.getFileContent(configId, path);
         if (content == null) {
@@ -126,20 +123,11 @@ public class FileController {
             response.setStatus(HttpStatus.NOT_FOUND.value());
             return;
         }
-        String filename = StrUtil.isNotEmpty(file.getName()) ? file.getName() : FileUtil.getName(path);
+        FileDO file = fileService.getFileByConfigIdAndPath(configId, path);
+        String filename = file != null && StrUtil.isNotEmpty(file.getName()) ? file.getName() : FileUtil.getName(path);
         writeAttachment(response, filename, content);
     }
 
-    @PutMapping("/update-scope")
-    @Operation(summary = "调整文件可见范围", description = "ZS-FILE-001.A：PUBLIC=公开素材（匿名可读）；PRIVATE=私有附件（默认）")
-    @Parameter(name = "id", description = "编号", required = true)
-    @PreAuthorize("@ss.hasPermission('infra:file:update')")
-    public CommonResult<Boolean> updateFileScope(@RequestParam("id") Long id,
-                                                 @RequestParam("scope") String scope) {
-        fileService.updateFileScope(id, scope);
-        return success(true);
-    }
-
     @GetMapping("/page")
     @Operation(summary = "获得文件分页")
     @PreAuthorize("@ss.hasPermission('infra:file:query')")
diff --git a/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/controller/admin/file/vo/file/FileRespVO.java b/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/controller/admin/file/vo/file/FileRespVO.java
index a98fcf2a..6de99852 100644
--- a/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/controller/admin/file/vo/file/FileRespVO.java
+++ b/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/controller/admin/file/vo/file/FileRespVO.java
@@ -33,11 +33,4 @@ public class FileRespVO {
     @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
     private LocalDateTime createTime;
 
-
-    @Schema(description = "可见范围（ZS-FILE-001.A）", example = "PRIVATE")
-    private String scope;
-
-    @Schema(description = "上传主体用户编号（ZS-FILE-001.A）", example = "1")
-    private Long ownerUserId;
-
-}
\ No newline at end of file
+}
diff --git a/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/controller/app/file/AppFileController.java b/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/controller/app/file/AppFileController.java
index 32dfd9d3..3408f530 100644
--- a/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/controller/app/file/AppFileController.java
+++ b/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/controller/app/file/AppFileController.java
@@ -54,6 +54,10 @@ public class AppFileController {
         return success(fileService.presignPutUrl(name, directory));
     }
 
-    // ZS-FILE-001.A（codex r1 P1）：App 端 /create 同步禁用——presigned create 无上传申请绑定，
-    // 可冒领他人 configId/path 生成归属记录；凭证化重新交付归 ZS-FILE-003
+    @PostMapping("/create")
+    @Operation(summary = "创建文件", description = "模式二：前端上传文件：配合 presigned-url 接口，记录上传了上传的文件")
+    public CommonResult<Long> createFile(@Valid @RequestBody FileCreateReqVO createReqVO) {
+        return success(fileService.createFile(createReqVO));
+    }
+
 }
diff --git a/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/dal/dataobject/file/FileDO.java b/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/dal/dataobject/file/FileDO.java
index 2210bc1d..df4d1d6f 100644
--- a/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/dal/dataobject/file/FileDO.java
+++ b/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/dal/dataobject/file/FileDO.java
@@ -1,6 +1,7 @@
 package cn.zszj.module.infra.dal.dataobject.file;
 
-import cn.zszj.framework.tenant.core.db.TenantBaseDO;
+import cn.zszj.framework.mybatis.core.dataobject.BaseDO;
+import cn.zszj.framework.tenant.core.aop.TenantIgnore;
 import com.baomidou.mybatisplus.annotation.KeySequence;
 import com.baomidou.mybatisplus.annotation.TableName;
 import lombok.*;
@@ -19,19 +20,8 @@ import lombok.*;
 @Builder
 @NoArgsConstructor
 @AllArgsConstructor
-public class FileDO extends TenantBaseDO {
-
-
-    /**
-     * 上传主体用户编号（ZS-FILE-001.A：服务端确认的所有者，匿名/系统上传为 0）
-     */
-    private Long ownerUserId;
-    /**
-     * 可见范围（ZS-FILE-001.A）：PUBLIC=公开素材（匿名可读）；PRIVATE=私有附件（默认，需登录且同租户）
-     *
-     * 枚举 {@link cn.zszj.module.infra.enums.file.FileScopeEnum}
-     */
-    private String scope;
+@TenantIgnore
+public class FileDO extends BaseDO {
 
     /**
      * 编号，数据库自增
diff --git a/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/enums/ErrorCodeConstants.java b/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/enums/ErrorCodeConstants.java
index 745a3033..9aec3e43 100644
--- a/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/enums/ErrorCodeConstants.java
+++ b/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/enums/ErrorCodeConstants.java
@@ -39,8 +39,6 @@ public interface ErrorCodeConstants {
     // ========= 文件相关 1-001-003-000 =================
     ErrorCode FILE_PATH_EXISTS = new ErrorCode(1_001_003_000, "文件路径已存在");
     ErrorCode FILE_NOT_EXISTS = new ErrorCode(1_001_003_001, "文件不存在");
-    // ZS-FILE-001.A：文件可见范围非法
-    ErrorCode FILE_SCOPE_INVALID = new ErrorCode(1_001_003_005, "文件可见范围（{}）非法，仅支持 PUBLIC/PRIVATE"); // codex r0 P3：改用未占用码，原 1_001_003_002 与 FILE_IS_EMPTY 冲突
     ErrorCode FILE_IS_EMPTY = new ErrorCode(1_001_003_002, "文件为空");
     ErrorCode FILE_PATH_INVALID = new ErrorCode(1_001_003_003, "文件路径不正确");
 
diff --git a/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/enums/file/FileScopeEnum.java b/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/enums/file/FileScopeEnum.java
deleted file mode 100644
index 267a200e..00000000
--- a/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/enums/file/FileScopeEnum.java
+++ /dev/null
@@ -1,33 +0,0 @@
-package cn.zszj.module.infra.enums.file;
-
-import lombok.Getter;
-import lombok.RequiredArgsConstructor;
-
-/**
- * 文件可见范围枚举（ZS-FILE-001.A）
- *
- * @author ZS-FILE-001.A
- */
-@RequiredArgsConstructor
-@Getter
-public enum FileScopeEnum {
-
-    /** 公开素材：匿名可读（批准用途） */
-    PUBLIC("PUBLIC"),
-
-    /** 私有附件：需登录且同技术租户（默认） */
-    PRIVATE("PRIVATE");
-
-    private final String scope;
-
-    /** 校验给定 scope 是否为合法枚举值 */
-    public static boolean isValid(String scope) {
-        for (FileScopeEnum e : values()) {
-            if (e.scope.equals(scope)) {
-                return true;
-            }
-        }
-        return false;
-    }
-
-}
diff --git a/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/service/file/FileService.java b/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/service/file/FileService.java
index 1d42869b..ac2b28da 100644
--- a/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/service/file/FileService.java
+++ b/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/service/file/FileService.java
@@ -95,19 +95,4 @@ public interface FileService {
      */
     FileDO getFileByConfigIdAndPath(Long configId, String path);
 
-
-    /**
-     * ZS-FILE-001.A：统一读取授权——PUBLIC 匿名可读；PRIVATE 需登录且同技术租户
-     */
-    void validateFileReadable(FileDO file, cn.zszj.framework.security.core.LoginUser loginUser);
-
-    /**
-     * ZS-FILE-001.A：管理员显式调整文件可见范围（PUBLIC/PRIVATE）
-     */
-    void updateFileScope(Long id, String scope);
-
-    /**
-     * ZS-FILE-001.A（codex r0 P2）：下载场景跨租户定位文件记录（PUBLIC 对任意来源同址可用）
-     */
-    FileDO getFileByConfigIdAndPathIgnoreTenant(Long configId, String path);
-}
\ No newline at end of file
+}
diff --git a/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/service/file/FileServiceImpl.java b/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/service/file/FileServiceImpl.java
index be01e785..8c4f0ff2 100644
--- a/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/service/file/FileServiceImpl.java
+++ b/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/service/file/FileServiceImpl.java
@@ -4,17 +4,11 @@ import cn.hutool.core.date.LocalDateTimeUtil;
 import cn.hutool.core.io.FileUtil;
 import cn.hutool.core.lang.Assert;
 import cn.hutool.core.util.RandomUtil;
-import cn.hutool.core.collection.CollUtil;
 import cn.hutool.core.util.StrUtil;
 import cn.hutool.crypto.digest.DigestUtil;
 import cn.zszj.framework.common.pojo.PageResult;
 import cn.zszj.framework.common.util.http.HttpUtils;
-import cn.zszj.framework.security.core.LoginUser;
-import cn.zszj.framework.tenant.core.context.TenantContextHolder;
-import cn.zszj.framework.tenant.core.util.TenantUtils;
-import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
 import cn.zszj.framework.common.util.object.BeanUtils;
-import cn.zszj.module.infra.enums.file.FileScopeEnum;
 import cn.zszj.module.infra.controller.admin.file.vo.file.FileCreateReqVO;
 import cn.zszj.module.infra.controller.admin.file.vo.file.FilePageReqVO;
 import cn.zszj.module.infra.controller.admin.file.vo.file.FilePresignedUrlRespVO;
@@ -26,24 +20,19 @@ import cn.zszj.module.infra.framework.file.core.utils.FileTypeUtils;
 import com.google.common.annotations.VisibleForTesting;
 import jakarta.annotation.Resource;
 import lombok.SneakyThrows;
-import org.springframework.security.access.AccessDeniedException;
-import lombok.extern.slf4j.Slf4j;
 import org.springframework.stereotype.Service;
 
 import java.util.List;
-import java.util.Objects;
 
 import static cn.hutool.core.date.DatePattern.PURE_DATE_PATTERN;
 import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
 import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_NOT_EXISTS;
-import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_SCOPE_INVALID;
 
 /**
  * 文件 Service 实现类
  *
  * @author 芋道源码
  */
-@Slf4j
 @Service
 public class FileServiceImpl implements FileService {
 
@@ -70,8 +59,6 @@ public class FileServiceImpl implements FileService {
 
     @Resource
     private FileConfigService fileConfigService;
-    @Resource
-    private cn.zszj.framework.common.biz.system.permission.PermissionCommonApi permissionCommonApi;
 
     @Resource
     private FileMapper fileMapper;
@@ -110,14 +97,10 @@ public class FileServiceImpl implements FileService {
         Assert.notNull(client, "客户端(master) 不能为空");
         String url = client.upload(content, path, type);
 
-        // 3. 保存到数据库。ZS-FILE-001.A：记录上传主体、默认私有（公开素材须管理员显式调整）
-        FileDO file = new FileDO().setConfigId(client.getId())
+        // 3. 保存到数据库
+        fileMapper.insert(new FileDO().setConfigId(client.getId())
                 .setName(name).setPath(path).setUrl(url)
-                .setType(type).setSize((long) content.length)
-                .setOwnerUserId(currentUserOrZero()).setScope(FileScopeEnum.PRIVATE.getScope());
-        // ZS-FILE-001.A：显式记录技术租户（服务端确认归属，不依赖拦截器装配）
-        file.setTenantId(TenantContextHolder.getTenantId());
-        fileMapper.insert(file);
+                .setType(type).setSize((long) content.length));
         return url;
     }
 
@@ -190,15 +173,8 @@ public class FileServiceImpl implements FileService {
         // 1.2 处理 URL 的合法性，移除 URL 中的查询参数（例如签名参数），保证 URL 的唯一性
         createReqVO.setUrl(HttpUtils.removeUrlQuery(createReqVO.getUrl())); // 目的：移除私有桶情况下，URL 的签名参数
 
-        // 2. 保存到数据库。ZS-FILE-001.A（codex r0 P1）：presigned create 端点已禁用（无上传申请绑定
-        // 无法证明对象归属，可冒领他人 configId/path 生成记录），本方法保留待 FILE-003 凭证化后重新接线；
-        // configId 不信任客户端指定，空值由服务端取 master 存储配置兜底；显式记录技术租户
+        // 2. 保存到数据库
         FileDO file = BeanUtils.toBean(createReqVO, FileDO.class);
-        if (file.getConfigId() == null) {
-            file.setConfigId(fileConfigService.getMasterFileClient().getId());
-        }
-        file.setOwnerUserId(currentUserOrZero()).setScope(FileScopeEnum.PRIVATE.getScope())
-                .setTenantId(TenantContextHolder.getTenantId());
         fileMapper.insert(file);
         return file.getId();
     }
@@ -227,15 +203,8 @@ public class FileServiceImpl implements FileService {
     @Override
     @SneakyThrows
     public void deleteFileList(List<Long> ids) {
-        // ZS-FILE-001.A（codex r0 P2）：批量删除前显式校验「全部存在且全部属于当前技术租户」——
-        // 混入他租户/不存在 id 整批拒绝（原先静默跳过，越权文件混入无感知）；不依赖租户拦截器装配
-        Long currentTenantId = TenantContextHolder.getTenantId();
-        List<FileDO> files = fileMapper.selectByIds(ids);
-        if (files.size() != CollUtil.distinct(ids).size()
-                || files.stream().anyMatch(f -> !Objects.equals(f.getTenantId(), currentTenantId))) {
-            throw exception(FILE_NOT_EXISTS);
-        }
         // 删除文件
+        List<FileDO> files = fileMapper.selectByIds(ids);
         for (FileDO file : files) {
             FilePathUtils.validatePath(file.getPath());
             // 获取客户端
@@ -269,92 +238,9 @@ public class FileServiceImpl implements FileService {
         return client.getContent(path);
     }
 
-    /**
-     * ZS-FILE-001.A（codex r0 P2）：下载场景跨租户定位文件记录——公开素材必须对任意租户/匿名
-     * 保持同一可用地址（租户过滤会把他租户 PUBLIC 过滤成 404）；PRIVATE 的租户归属校验
-     * 由 {@link #validateFileReadable} 以记录自身 tenant_id 执行。
-     */
-    @Override
-    public FileDO getFileByConfigIdAndPathIgnoreTenant(Long configId, String path) {
-        return TenantUtils.executeIgnore(() ->
-                fileMapper.selectLatestByConfigIdAndPath(configId, path));
-    }
-
     @Override
     public FileDO getFileByConfigIdAndPath(Long configId, String path) {
         return fileMapper.selectLatestByConfigIdAndPath(configId, path);
     }
 
-
-    /**
-     * ZS-FILE-001.A：当前登录用户编号；匿名/系统上下文返回 0（owner 列 NOT NULL DEFAULT 0 语义一致）。
-     */
-    private Long currentUserOrZero() {
-        Long userId = SecurityFrameworkUtils.getLoginUserId();
-        return userId != null ? userId : 0L;
-    }
-
-    /**
-     * ZS-FILE-001.A：统一读取授权——PUBLIC 匿名可读；PRIVATE 需登录且与文件同技术租户。
-     *
-     * @param file      文件（含 scope 与 tenantId）
-     * @param loginUser 下载发起者（匿名传 null；由 Controller 从安全上下文透传，便于单测）
-     * @throws AccessDeniedException 私有文件匿名/跨租户读取
-     */
-    @Override
-    public void validateFileReadable(FileDO file, LoginUser loginUser) {
-        if (file == null) {
-            return; // 不存在由调用方按 404 处理
-        }
-        if (FileScopeEnum.PUBLIC.getScope().equals(file.getScope())) {
-            return; // 公开素材：批准用途内匿名可读
-        }
-        // 私有附件（codex r0 P1 + r1 P1/P2）：必须登录，且满足其一——
-        // ① 上传所有者本人（ownerUserId>0 且与登录主体匹配且同租户；0=无个人所有者，禁止凭 userId=0 凭证冒领）；
-        // ② 同技术租户且实际持有 infra:file:query 权限（经 PermissionCommonApi 查询，与 @ss.hasPermission
-        //    同一数据源——OAuth scopes 与后台菜单权限是两套体系，不能作为判定依据）
-        if (loginUser == null) {
-            throw new AccessDeniedException("私有文件禁止匿名读取");
-        }
-        boolean ownerMatched = file.getOwnerUserId() != null && file.getOwnerUserId() > 0
-                && Objects.equals(file.getOwnerUserId(), loginUser.getId())
-                && Objects.equals(loginUser.getTenantId(), file.getTenantId());
-        if (ownerMatched) {
-            return;
-        }
-        boolean tenantMatched = Objects.equals(loginUser.getTenantId(), file.getTenantId());
-        boolean manager = tenantMatched && filePermissionFallback.apply(loginUser.getId(), "infra:file:query");
-        if (!manager) {
-            throw new AccessDeniedException("私有文件仅所有者或租户管理员可读取");
-        }
-    }
-
-    /**
-     * ZS-FILE-001.A codex r1 P2：管理面权限判定，与 {@code @ss.hasPermission} 一致走 PermissionCommonApi。
-     * 以函数字段注入便于单测；系统异常时保守返回 false（宁可拒绝也不放行）。
-     */
-    private final java.util.function.BiFunction<Long, String, Boolean> filePermissionFallback =
-            (userId, permission) -> {
-                try {
-                    return permissionCommonApi.hasAnyPermissions(userId, permission);
-                } catch (Exception ex) {
-                    log.warn("[filePermissionFallback][用户({}) 权限查询失败，保守拒绝 permission({})]", userId, permission, ex);
-                    return false;
-                }
-            };
-
-    /**
-     * ZS-FILE-001.A：管理员显式调整文件可见范围（历史存量迁移默认 PRIVATE，公开须显式标注）
-     */
-    @Override
-    public void updateFileScope(Long id, String scope) {
-        if (!FileScopeEnum.isValid(scope)) {
-            throw exception(FILE_SCOPE_INVALID);
-        }
-        FileDO file = validateFileExists(id);
-        FileDO updateObj = new FileDO().setId(file.getId()).setScope(scope);
-        fileMapper.updateById(updateObj);
-    }
-
-    // ZS-FILE-001.A 类尾占位
-}
\ No newline at end of file
+}
diff --git a/services/zhongshu-core/zszj-module-infra/src/test/java/cn/zszj/module/infra/service/db/DatabaseTableServiceImplTest.java b/services/zhongshu-core/zszj-module-infra/src/test/java/cn/zszj/module/infra/service/db/DatabaseTableServiceImplTest.java
index 97d879d8..a12fc1e2 100644
--- a/services/zhongshu-core/zszj-module-infra/src/test/java/cn/zszj/module/infra/service/db/DatabaseTableServiceImplTest.java
+++ b/services/zhongshu-core/zszj-module-infra/src/test/java/cn/zszj/module/infra/service/db/DatabaseTableServiceImplTest.java
@@ -64,9 +64,7 @@ public class DatabaseTableServiceImplTest extends BaseDbUnitTest {
     private void assertTableInfo(TableInfo tableInfo) {
         assertEquals("infra_config", tableInfo.getName());
         assertEquals("参数配置表", tableInfo.getComment());
-        // codex 前欠账补同步：CFG-004 为 infra_config 新增 version 列（13→14）；
-        // ZS-FILE-001.A 的 infra_file 列变化不影响本断言
-        assertEquals(14, tableInfo.getFields().size());
+        assertEquals(13, tableInfo.getFields().size());
         // id 字段
         TableField idField = tableInfo.getFields().get(0);
         assertEquals("id", idField.getName());
diff --git a/services/zhongshu-core/zszj-module-infra/src/test/java/cn/zszj/module/infra/service/file/FileServiceAuthorizationTest.java b/services/zhongshu-core/zszj-module-infra/src/test/java/cn/zszj/module/infra/service/file/FileServiceAuthorizationTest.java
deleted file mode 100644
index a9a96810..00000000
--- a/services/zhongshu-core/zszj-module-infra/src/test/java/cn/zszj/module/infra/service/file/FileServiceAuthorizationTest.java
+++ /dev/null
@@ -1,249 +0,0 @@
-package cn.zszj.module.infra.service.file;
-
-import cn.zszj.framework.security.core.LoginUser;
-import cn.zszj.module.infra.enums.file.FileScopeEnum;
-import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
-import cn.zszj.module.infra.controller.admin.file.vo.file.FileCreateReqVO;
-import cn.zszj.module.infra.dal.dataobject.file.FileDO;
-import cn.zszj.module.infra.dal.mysql.file.FileMapper;
-import cn.zszj.framework.common.exception.ServiceException;
-import cn.zszj.framework.tenant.core.context.TenantContextHolder;
-import jakarta.annotation.Resource;
-import org.junit.jupiter.api.AfterEach;
-import org.junit.jupiter.api.BeforeEach;
-import org.junit.jupiter.api.Test;
-import org.springframework.context.annotation.Import;
-import org.springframework.security.access.AccessDeniedException;
-
-import java.util.List;
-
-
-import static cn.zszj.framework.test.core.util.RandomUtils.randomString;
-import static org.mockito.ArgumentMatchers.any;
-import static org.mockito.ArgumentMatchers.anyString;
-import static org.mockito.Mockito.when;
-import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_NOT_EXISTS;
-import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_SCOPE_INVALID;
-import static org.junit.jupiter.api.Assertions.*;
-
-/**
- * ZS-FILE-001.A：文件归属与统一读取授权测试（技术账号/tenant，H2）。
- *
- * <p>合同：①上传默认 PRIVATE 且记录 tenant_id + owner；②PUBLIC 匿名可读、PRIVATE 匿名拒绝；
- * ③PRIVATE 跨租户登录拒绝、同租户登录放行；④批量删除混入越权/不存在 id 整批拒绝且零删除；
- * ⑤update-scope 显式调整（合法/非法）。
- *
- * @author ZS-FILE-001.A
- */
-@Import(FileServiceImpl.class)
-public class FileServiceAuthorizationTest extends BaseDbUnitTest {
-
-    @Resource
-    private FileServiceImpl fileService;
-
-    @Resource
-    private FileMapper fileMapper;
-
-    @org.springframework.test.context.bean.override.mockito.MockitoBean
-    private cn.zszj.module.infra.service.file.FileConfigService fileConfigService;
-
-    @org.springframework.test.context.bean.override.mockito.MockitoBean
-    private cn.zszj.framework.common.biz.system.permission.PermissionCommonApi permissionCommonApi;
-
-
-    @BeforeEach
-    public void beforeEach() {
-        TenantContextHolder.setTenantId(1L);
-        // createFile 依赖 master FileClient 上传内容——mock 客户端记录路径即返回 URL
-        cn.zszj.module.infra.framework.file.core.client.FileClient masterClient =
-                org.mockito.Mockito.mock(cn.zszj.module.infra.framework.file.core.client.FileClient.class);
-        when(masterClient.getId()).thenReturn(1L);
-        try {
-            when(masterClient.upload(any(), anyString(), anyString())).thenReturn("https://oss.example.com/mock.txt");
-        } catch (Exception ignored) { }
-        when(fileConfigService.getMasterFileClient()).thenReturn(masterClient);
-    }
-
-    @AfterEach
-    public void afterEach() {
-        TenantContextHolder.clear();
-    }
-
-    // ========== ① 写路径：默认 PRIVATE + 租户 + 归属 ==========
-
-    @Test
-    public void createFile_defaultsPrivateWithTenantAndOwner() {
-        String url = fileService.createFile("test-content".getBytes(java.nio.charset.StandardCharsets.UTF_8), "test.txt", null, "text/plain");
-
-        FileDO file = fileMapper.selectList(new cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX<FileDO>()
-                .eq(FileDO::getUrl, url)).get(0);
-        assertEquals("PRIVATE", file.getScope(),
-                "新上传必须默认 PRIVATE（历史迁移不能默认全部公开）");
-        assertEquals(1L, file.getTenantId(), "必须记录上传时的技术租户");
-        assertEquals(0L, file.getOwnerUserId().longValue(), "匿名/系统上下文 owner 为 0");
-    }
-
-    /**
-     * ZS-FILE-001.A（codex r0 P1）：presigned create 端点已禁用；service 方法保留供 FILE-003
-     * 凭证化后重新接线——此处验证即便内部调用，归属/私有语义仍然成立。
-     */
-    @Test
-    public void createFileByReqVO_defaultsPrivate() {
-        FileCreateReqVO reqVO = new FileCreateReqVO();
-        reqVO.setName("presigned.txt");
-        reqVO.setPath("presigned/" + randomString() + ".txt");
-        reqVO.setUrl("https://oss.example.com/" + randomString() + ".txt");
-        reqVO.setType("text/plain");
-        reqVO.setSize(100L);
-
-        Long id = fileService.createFile(reqVO);
-
-        FileDO file = fileMapper.selectById(id);
-        assertEquals("PRIVATE", file.getScope(), "presigned 直传记录同样默认 PRIVATE");
-        assertEquals(1L, file.getTenantId());
-        assertEquals(1L, file.getConfigId().longValue(), "configId 由服务端 master 兜底，不信任客户端");
-    }
-
-    // ========== ②③ 读取授权 ==========
-
-    @Test
-    public void validateReadable_public_anonymousAllowed() {
-        FileDO file = seedFile(1L, "PUBLIC");
-
-        assertDoesNotThrow(() -> fileService.validateFileReadable(file, null),
-                "公开素材按批准用途匿名可读");
-    }
-
-    @Test
-    public void validateReadable_private_anonymousRejected() {
-        FileDO file = seedFile(1L, "PRIVATE");
-
-        assertThrows(AccessDeniedException.class,
-                () -> fileService.validateFileReadable(file, null),
-                "私有附件必须关闭匿名旁路");
-    }
-
-    @Test
-    public void validateReadable_private_otherTenantRejected() {
-        FileDO file = seedFile(1L, "PRIVATE");
-        LoginUser otherTenantUser = new LoginUser().setId(201L).setTenantId(2L);
-
-        assertThrows(AccessDeniedException.class,
-                () -> fileService.validateFileReadable(file, otherTenantUser),
-                "另一技术租户登录不得读取他租户私有附件");
-    }
-
-    @Test
-    public void validateReadable_private_sameTenantAllowed() {
-        FileDO file = seedFile(1L, "PRIVATE");
-        file.setOwnerUserId(101L);
-        fileMapper.updateById(file);
-        LoginUser owner = new LoginUser().setId(101L).setTenantId(1L);
-
-        assertDoesNotThrow(() -> fileService.validateFileReadable(file, owner), "所有者本人可读");
-    }
-
-    @Test
-    public void validateReadable_private_ownerZero_clientCredentialToken_rejected() {
-        // codex r1 P1：owner=0（无个人所有者，如存量迁移）+ userId=0 的 client-credentials 令牌
-        // ——owner 分支必须要求 ownerUserId>0，禁止凭 userId=0 凭证跨租户冒领
-        FileDO file = seedFile(1L, "PRIVATE");
-        file.setOwnerUserId(0L);
-        fileMapper.updateById(file);
-        LoginUser zeroUser = new LoginUser().setId(0L).setTenantId(1L);
-
-        assertThrows(AccessDeniedException.class,
-                () -> fileService.validateFileReadable(file, zeroUser),
-                "userId=0 令牌不得凭 owner=0 冒领私有文件");
-    }
-
-    @Test
-    public void validateReadable_private_tenantAdminWithQueryPermission_allowed() {
-        // codex r1 P2：管理分支走 PermissionCommonApi（与 @ss.hasPermission 同源），scopes 无关
-        FileDO file = seedFile(1L, "PRIVATE");
-        file.setOwnerUserId(0L);
-        fileMapper.updateById(file);
-        LoginUser admin = new LoginUser().setId(103L).setTenantId(1L); // 非 owner
-        when(permissionCommonApi.hasAnyPermissions(103L, "infra:file:query")).thenReturn(true);
-
-        assertDoesNotThrow(() -> fileService.validateFileReadable(file, admin),
-                "同租户持有 infra:file:query 的管理员可读");
-    }
-
-    @Test
-    public void validateReadable_private_tenantUserWithoutPermission_rejected() {
-        FileDO file = seedFile(1L, "PRIVATE");
-        file.setOwnerUserId(0L);
-        fileMapper.updateById(file);
-        LoginUser plainUser = new LoginUser().setId(105L).setTenantId(1L); // 同租户非 owner
-        when(permissionCommonApi.hasAnyPermissions(105L, "infra:file:query")).thenReturn(false);
-
-        assertThrows(AccessDeniedException.class,
-                () -> fileService.validateFileReadable(file, plainUser),
-                "同租户无查询权限的非所有者必须拒绝");
-    }
-
-    // ========== ④ 批量删除混入越权 ==========
-
-    @Test
-    public void deleteFileList_mixedForeignIds_rejectsAllAndDeletesNothing() throws Exception {
-        FileDO mine = seedFile(1L, "PRIVATE");
-        FileDO foreign = seedFile(2L, "PRIVATE"); // 他租户真实文件
-
-        // codex r1 P3：仅混真实他租户 id——证明跨租户隔离（非「不存在 id」数量校验短路）
-        ServiceException ex = assertThrows(ServiceException.class,
-                () -> fileService.deleteFileList(List.of(mine.getId(), foreign.getId())));
-        assertEquals(FILE_NOT_EXISTS.getCode(), ex.getCode());
-        assertNotNull(fileMapper.selectById(mine.getId()), "本租户文件必须保留");
-        assertNotNull(fileMapper.selectById(foreign.getId()), "他租户文件必须保留（零删除）");
-    }
-
-    @Test
-    public void deleteFileList_mixedNonexistentId_rejectsAll() throws Exception {
-        FileDO mine = seedFile(1L, "PRIVATE");
-
-        ServiceException ex = assertThrows(ServiceException.class,
-                () -> fileService.deleteFileList(List.of(mine.getId(), 999_999L)));
-        assertEquals(FILE_NOT_EXISTS.getCode(), ex.getCode(), "混入不存在 id 必须整批拒绝");
-        assertNotNull(fileMapper.selectById(mine.getId()), "整批拒绝后合法文件不得被删除");
-    }
-
-    // ========== ⑤ update-scope ==========
-
-    @Test
-    public void updateFileScope_toPublic_anonymousBecomesReadable() {
-        FileDO file = seedFile(1L, "PRIVATE");
-
-        fileService.updateFileScope(file.getId(), "PUBLIC");
-
-        FileDO after = fileMapper.selectById(file.getId());
-        assertEquals("PUBLIC", after.getScope());
-        assertDoesNotThrow(() -> fileService.validateFileReadable(after, null),
-                "显式公开后匿名可读");
-    }
-
-    @Test
-    public void updateFileScope_invalidScope_rejected() {
-        FileDO file = seedFile(1L, "PRIVATE");
-
-        ServiceException ex = assertThrows(ServiceException.class,
-                () -> fileService.updateFileScope(file.getId(), "FRIENDS"));
-        assertEquals(FILE_SCOPE_INVALID.getCode(), ex.getCode());
-        assertEquals("PRIVATE", fileMapper.selectById(file.getId()).getScope(), "非法 scope 不得落库");
-    }
-
-    // ========== 造数辅助 ==========
-
-    private FileDO seedFile(Long tenantId, String scope) {
-        FileDO file = FileDO.builder()
-                .configId(1L).name(randomString() + ".txt")
-                .path("test/" + randomString() + ".txt")
-                .url("https://oss.example.com/" + randomString() + ".txt")
-                .type("text/plain").size(100L)
-                .ownerUserId(101L).scope(scope)
-                .build();
-        file.setTenantId(tenantId);
-        fileMapper.insert(file);
-        return file;
-    }
-}
diff --git a/services/zhongshu-core/zszj-module-infra/src/test/java/cn/zszj/module/infra/service/file/FileServiceImplTest.java b/services/zhongshu-core/zszj-module-infra/src/test/java/cn/zszj/module/infra/service/file/FileServiceImplTest.java
index 6a159091..902c16e3 100644
--- a/services/zhongshu-core/zszj-module-infra/src/test/java/cn/zszj/module/infra/service/file/FileServiceImplTest.java
+++ b/services/zhongshu-core/zszj-module-infra/src/test/java/cn/zszj/module/infra/service/file/FileServiceImplTest.java
@@ -39,8 +39,6 @@ public class FileServiceImplTest extends BaseDbUnitTest {
 
     @MockitoBean
     private FileConfigService fileConfigService;
-    @org.springframework.test.context.bean.override.mockito.MockitoBean
-    private cn.zszj.framework.common.biz.system.permission.PermissionCommonApi permissionCommonApi;
 
     @BeforeEach
     public void setUp() {
diff --git a/services/zhongshu-core/zszj-module-infra/src/test/resources/sql/create_tables.sql b/services/zhongshu-core/zszj-module-infra/src/test/resources/sql/create_tables.sql
index c5d50250..7698a40b 100644
--- a/services/zhongshu-core/zszj-module-infra/src/test/resources/sql/create_tables.sql
+++ b/services/zhongshu-core/zszj-module-infra/src/test/resources/sql/create_tables.sql
@@ -40,8 +40,6 @@ CREATE TABLE IF NOT EXISTS "infra_file" (
     "url" varchar(1024),
     "type" varchar(63) DEFAULT NULL,
     "size" bigint NOT NULL,
-    "owner_user_id" bigint NOT NULL DEFAULT 0,
-    "scope" varchar(16) NOT NULL DEFAULT 'PRIVATE',
     "creator" varchar(64) DEFAULT '',
     "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
     "updater" varchar(64) DEFAULT '',
diff --git a/services/zhongshu-core/zszj-server/src/main/resources/application.yaml b/services/zhongshu-core/zszj-server/src/main/resources/application.yaml
index 465ff0e6..2bf570d5 100644
--- a/services/zhongshu-core/zszj-server/src/main/resources/application.yaml
+++ b/services/zhongshu-core/zszj-server/src/main/resources/application.yaml
@@ -338,9 +338,6 @@ zszj:
     enable: true
     ignore-urls:
       - /jmreport/* # 积木报表，无法携带租户编号
-      # ZS-FILE-001.A：文件下载（含匿名 PUBLIC 素材，浏览器 img/src 不携带 tenant-id 头）；
-      # PRIVATE 的登录+同租户校验在 FileService#validateFileReadable 以 LoginUser 执行
-      - /admin-api/infra/file/*/get/**
     ignore-visit-urls:
       - /admin-api/system/user/profile/**
       - /admin-api/system/auth/**
diff --git a/services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260914.001__infra_file_tenancy.sql b/services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260914.001__infra_file_tenancy.sql
deleted file mode 100644
index 2e6a8a43..00000000
--- a/services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260914.001__infra_file_tenancy.sql
+++ /dev/null
@@ -1,7 +0,0 @@
--- ZS-FILE-001.A：文件租户化与归属——私有附件归属技术租户与上传主体，公开素材显式标注。
--- 历史存量默认 PRIVATE（不能默认全部公开），由管理员经 update-scope 端点显式调整。
-ALTER TABLE infra_file ADD COLUMN IF NOT EXISTS tenant_id int8 NOT NULL DEFAULT 0;
-ALTER TABLE infra_file ADD COLUMN IF NOT EXISTS owner_user_id int8 NOT NULL DEFAULT 0;
-ALTER TABLE infra_file ADD COLUMN IF NOT EXISTS scope varchar(16) NOT NULL DEFAULT 'PRIVATE';
-CREATE INDEX IF NOT EXISTS idx_infra_file_02 ON infra_file (tenant_id);
-CREATE INDEX IF NOT EXISTS idx_infra_file_03 ON infra_file (scope);
diff --git a/services/zhongshu-core/zszj-server/src/test/resources/api-inventory-baseline.txt b/services/zhongshu-core/zszj-server/src/test/resources/api-inventory-baseline.txt
index 0357b3bd..8325987d 100644
--- a/services/zhongshu-core/zszj-server/src/test/resources/api-inventory-baseline.txt
+++ b/services/zhongshu-core/zszj-server/src/test/resources/api-inventory-baseline.txt
@@ -1,6 +1,6 @@
 # ZS-SEC-002 接口清单基线（自动生成，勿手改；变更须经评审后以 -Dapi.inventory.update=true 重新生成）
 # 列：HTTP方法|完整路径|主体类型|授权类别(ANONYMOUS/PERMISSION/AUTHENTICATED)|权限标识|模块|Controller
-# 端点总数：327
+# 端点总数：328
 DELETE|/admin-api/infra/codegen/delete-list|ADMIN|PERMISSION|infra:codegen:delete|infra|CodegenController
 DELETE|/admin-api/infra/codegen/delete|ADMIN|PERMISSION|infra:codegen:delete|infra|CodegenController
 DELETE|/admin-api/infra/config/delete-list|ADMIN|PERMISSION|infra:config:delete|infra|ConfigController
@@ -238,6 +238,7 @@ POST|/admin-api/infra/demo03-student-erp/demo03-grade/create|ADMIN|PERMISSION|in
 POST|/admin-api/infra/demo03-student-inner/create|ADMIN|PERMISSION|infra:demo03-student:create|infra|Demo03StudentInnerController
 POST|/admin-api/infra/demo03-student-normal/create|ADMIN|PERMISSION|infra:demo03-student:create|infra|Demo03StudentNormalController
 POST|/admin-api/infra/file-config/create|ADMIN|PERMISSION|infra:file-config:create|infra|FileConfigController
+POST|/admin-api/infra/file/create|ADMIN|AUTHENTICATED|-|infra|FileController
 POST|/admin-api/infra/file/upload|ADMIN|AUTHENTICATED|-|infra|FileController
 POST|/admin-api/infra/job/create|ADMIN|PERMISSION|infra:job:create|infra|JobController
 POST|/admin-api/infra/job/sync|ADMIN|PERMISSION|infra:job:create|infra|JobController
@@ -285,6 +286,7 @@ POST|/admin-api/system/tenant-package/create|ADMIN|PERMISSION|system:tenant-pack
 POST|/admin-api/system/tenant/create|ADMIN|PERMISSION|system:tenant:create|system|TenantController
 POST|/admin-api/system/user/create|ADMIN|PERMISSION|system:user:create|system|UserController
 POST|/admin-api/system/user/import|ADMIN|PERMISSION|system:user:import|system|UserController
+POST|/app-api/infra/file/create|MEMBER|AUTHENTICATED|-|infra|AppFileController
 POST|/app-api/infra/file/upload|MEMBER|AUTHENTICATED|-|infra|AppFileController
 PUT|/admin-api/infra/api-error-log/update-status|ADMIN|PERMISSION|infra:api-error-log:update-status|infra|ApiErrorLogController
 PUT|/admin-api/infra/codegen/sync-from-db|ADMIN|PERMISSION|infra:codegen:update|infra|CodegenController
@@ -300,7 +302,6 @@ PUT|/admin-api/infra/demo03-student-inner/update|ADMIN|PERMISSION|infra:demo03-s
 PUT|/admin-api/infra/demo03-student-normal/update|ADMIN|PERMISSION|infra:demo03-student:update|infra|Demo03StudentNormalController
 PUT|/admin-api/infra/file-config/update-master|ADMIN|PERMISSION|infra:file-config:update|infra|FileConfigController
 PUT|/admin-api/infra/file-config/update|ADMIN|PERMISSION|infra:file-config:update|infra|FileConfigController
-PUT|/admin-api/infra/file/update-scope|ADMIN|PERMISSION|infra:file:update|infra|FileController
 PUT|/admin-api/infra/job/trigger|ADMIN|PERMISSION|infra:job:trigger|infra|JobController
 PUT|/admin-api/infra/job/update-status|ADMIN|PERMISSION|infra:job:update|infra|JobController
 PUT|/admin-api/infra/job/update|ADMIN|PERMISSION|infra:job:update|infra|JobController

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'git diff main -- services/zhongshu-core/zszj-module-infra/src/main' in E:\zszj-wt-bpm-001-a
 succeeded in 0ms:
warning: unable to access 'C:\Users\Administrator/.config/git/ignore': Permission denied
diff --git a/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/controller/admin/file/FileController.java b/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/controller/admin/file/FileController.java
index 314947ed..8bb9de71 100644
--- a/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/controller/admin/file/FileController.java
+++ b/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/controller/admin/file/FileController.java
@@ -7,7 +7,7 @@ import cn.zszj.framework.common.pojo.CommonResult;
 import cn.zszj.framework.common.pojo.PageResult;
 import cn.zszj.framework.common.util.http.HttpUtils;
 import cn.zszj.framework.common.util.object.BeanUtils;
-import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
+import cn.zszj.framework.tenant.core.aop.TenantIgnore;
 import cn.zszj.module.infra.controller.admin.file.vo.file.*;
 import cn.zszj.module.infra.dal.dataobject.file.FileDO;
 import cn.zszj.module.infra.service.file.FileService;
@@ -66,6 +66,12 @@ public class FileController {
         return success(fileService.presignPutUrl(name, directory));
     }
 
+    @PostMapping("/create")
+    @Operation(summary = "创建文件", description = "模式二：前端上传文件：配合 presigned-url 接口，记录上传了上传的文件")
+    public CommonResult<Long> createFile(@Valid @RequestBody FileCreateReqVO createReqVO) {
+        return success(fileService.createFile(createReqVO));
+    }
+
     @GetMapping("/get")
     @Operation(summary = "获得文件")
     @Parameter(name = "id", description = "编号", required = true)
@@ -93,7 +99,8 @@ public class FileController {
     }
 
     @GetMapping("/{configId}/get/**")
-    @PermitAll // ZS-FILE-001.A：匿名仅可读 PUBLIC 公开素材；PRIVATE 在 service 校验登录+同租户
+    @PermitAll
+    @TenantIgnore
     @Operation(summary = "下载文件")
     @Parameter(name = "configId", description = "配置编号", required = true)
     public void getFileContent(HttpServletRequest request,
@@ -109,16 +116,6 @@ public class FileController {
         // https://gitee.com/zhijiantianya/ruoyi-vue-pro/pulls/1432/
         path = HttpUtils.decodeUrlPath(path);
 
-        // ZS-FILE-001.A（codex r0 P2）：跨租户定位记录——PUBLIC 对任意来源同址可用；
-        // PRIVATE 的租户归属校验以记录自身 tenant_id 执行（忽略请求携带租户，防租户过滤 404 误伤公开素材）
-        FileDO file = fileService.getFileByConfigIdAndPathIgnoreTenant(configId, path);
-        if (file == null) {
-            log.warn("[getFileContent][configId({}) path({}) 文件不存在]", configId, path);
-            response.setStatus(HttpStatus.NOT_FOUND.value());
-            return;
-        }
-        fileService.validateFileReadable(file, SecurityFrameworkUtils.getLoginUser());
-
         // 读取内容
         byte[] content = fileService.getFileContent(configId, path);
         if (content == null) {
@@ -126,20 +123,11 @@ public class FileController {
             response.setStatus(HttpStatus.NOT_FOUND.value());
             return;
         }
-        String filename = StrUtil.isNotEmpty(file.getName()) ? file.getName() : FileUtil.getName(path);
+        FileDO file = fileService.getFileByConfigIdAndPath(configId, path);
+        String filename = file != null && StrUtil.isNotEmpty(file.getName()) ? file.getName() : FileUtil.getName(path);
         writeAttachment(response, filename, content);
     }
 
-    @PutMapping("/update-scope")
-    @Operation(summary = "调整文件可见范围", description = "ZS-FILE-001.A：PUBLIC=公开素材（匿名可读）；PRIVATE=私有附件（默认）")
-    @Parameter(name = "id", description = "编号", required = true)
-    @PreAuthorize("@ss.hasPermission('infra:file:update')")
-    public CommonResult<Boolean> updateFileScope(@RequestParam("id") Long id,
-                                                 @RequestParam("scope") String scope) {
-        fileService.updateFileScope(id, scope);
-        return success(true);
-    }
-
     @GetMapping("/page")
     @Operation(summary = "获得文件分页")
     @PreAuthorize("@ss.hasPermission('infra:file:query')")
diff --git a/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/controller/admin/file/vo/file/FileRespVO.java b/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/controller/admin/file/vo/file/FileRespVO.java
index a98fcf2a..6de99852 100644
--- a/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/controller/admin/file/vo/file/FileRespVO.java
+++ b/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/controller/admin/file/vo/file/FileRespVO.java
@@ -33,11 +33,4 @@ public class FileRespVO {
     @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
     private LocalDateTime createTime;
 
-
-    @Schema(description = "可见范围（ZS-FILE-001.A）", example = "PRIVATE")
-    private String scope;
-
-    @Schema(description = "上传主体用户编号（ZS-FILE-001.A）", example = "1")
-    private Long ownerUserId;
-
-}
\ No newline at end of file
+}
diff --git a/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/controller/app/file/AppFileController.java b/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/controller/app/file/AppFileController.java
index 32dfd9d3..3408f530 100644
--- a/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/controller/app/file/AppFileController.java
+++ b/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/controller/app/file/AppFileController.java
@@ -54,6 +54,10 @@ public class AppFileController {
         return success(fileService.presignPutUrl(name, directory));
     }
 
-    // ZS-FILE-001.A（codex r1 P1）：App 端 /create 同步禁用——presigned create 无上传申请绑定，
-    // 可冒领他人 configId/path 生成归属记录；凭证化重新交付归 ZS-FILE-003
+    @PostMapping("/create")
+    @Operation(summary = "创建文件", description = "模式二：前端上传文件：配合 presigned-url 接口，记录上传了上传的文件")
+    public CommonResult<Long> createFile(@Valid @RequestBody FileCreateReqVO createReqVO) {
+        return success(fileService.createFile(createReqVO));
+    }
+
 }
diff --git a/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/dal/dataobject/file/FileDO.java b/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/dal/dataobject/file/FileDO.java
index 2210bc1d..df4d1d6f 100644
--- a/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/dal/dataobject/file/FileDO.java
+++ b/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/dal/dataobject/file/FileDO.java
@@ -1,6 +1,7 @@
 package cn.zszj.module.infra.dal.dataobject.file;
 
-import cn.zszj.framework.tenant.core.db.TenantBaseDO;
+import cn.zszj.framework.mybatis.core.dataobject.BaseDO;
+import cn.zszj.framework.tenant.core.aop.TenantIgnore;
 import com.baomidou.mybatisplus.annotation.KeySequence;
 import com.baomidou.mybatisplus.annotation.TableName;
 import lombok.*;
@@ -19,19 +20,8 @@ import lombok.*;
 @Builder
 @NoArgsConstructor
 @AllArgsConstructor
-public class FileDO extends TenantBaseDO {
-
-
-    /**
-     * 上传主体用户编号（ZS-FILE-001.A：服务端确认的所有者，匿名/系统上传为 0）
-     */
-    private Long ownerUserId;
-    /**
-     * 可见范围（ZS-FILE-001.A）：PUBLIC=公开素材（匿名可读）；PRIVATE=私有附件（默认，需登录且同租户）
-     *
-     * 枚举 {@link cn.zszj.module.infra.enums.file.FileScopeEnum}
-     */
-    private String scope;
+@TenantIgnore
+public class FileDO extends BaseDO {
 
     /**
      * 编号，数据库自增
diff --git a/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/enums/ErrorCodeConstants.java b/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/enums/ErrorCodeConstants.java
index 745a3033..9aec3e43 100644
--- a/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/enums/ErrorCodeConstants.java
+++ b/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/enums/ErrorCodeConstants.java
@@ -39,8 +39,6 @@ public interface ErrorCodeConstants {
     // ========= 文件相关 1-001-003-000 =================
     ErrorCode FILE_PATH_EXISTS = new ErrorCode(1_001_003_000, "文件路径已存在");
     ErrorCode FILE_NOT_EXISTS = new ErrorCode(1_001_003_001, "文件不存在");
-    // ZS-FILE-001.A：文件可见范围非法
-    ErrorCode FILE_SCOPE_INVALID = new ErrorCode(1_001_003_005, "文件可见范围（{}）非法，仅支持 PUBLIC/PRIVATE"); // codex r0 P3：改用未占用码，原 1_001_003_002 与 FILE_IS_EMPTY 冲突
     ErrorCode FILE_IS_EMPTY = new ErrorCode(1_001_003_002, "文件为空");
     ErrorCode FILE_PATH_INVALID = new ErrorCode(1_001_003_003, "文件路径不正确");
 
diff --git a/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/enums/file/FileScopeEnum.java b/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/enums/file/FileScopeEnum.java
deleted file mode 100644
index 267a200e..00000000
--- a/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/enums/file/FileScopeEnum.java
+++ /dev/null
@@ -1,33 +0,0 @@
-package cn.zszj.module.infra.enums.file;
-
-import lombok.Getter;
-import lombok.RequiredArgsConstructor;
-
-/**
- * 文件可见范围枚举（ZS-FILE-001.A）
- *
- * @author ZS-FILE-001.A
- */
-@RequiredArgsConstructor
-@Getter
-public enum FileScopeEnum {
-
-    /** 公开素材：匿名可读（批准用途） */
-    PUBLIC("PUBLIC"),
-
-    /** 私有附件：需登录且同技术租户（默认） */
-    PRIVATE("PRIVATE");
-
-    private final String scope;
-
-    /** 校验给定 scope 是否为合法枚举值 */
-    public static boolean isValid(String scope) {
-        for (FileScopeEnum e : values()) {
-            if (e.scope.equals(scope)) {
-                return true;
-            }
-        }
-        return false;
-    }
-
-}
diff --git a/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/service/file/FileService.java b/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/service/file/FileService.java
index 1d42869b..ac2b28da 100644
--- a/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/service/file/FileService.java
+++ b/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/service/file/FileService.java
@@ -95,19 +95,4 @@ public interface FileService {
      */
     FileDO getFileByConfigIdAndPath(Long configId, String path);
 
-
-    /**
-     * ZS-FILE-001.A：统一读取授权——PUBLIC 匿名可读；PRIVATE 需登录且同技术租户
-     */
-    void validateFileReadable(FileDO file, cn.zszj.framework.security.core.LoginUser loginUser);
-
-    /**
-     * ZS-FILE-001.A：管理员显式调整文件可见范围（PUBLIC/PRIVATE）
-     */
-    void updateFileScope(Long id, String scope);
-
-    /**
-     * ZS-FILE-001.A（codex r0 P2）：下载场景跨租户定位文件记录（PUBLIC 对任意来源同址可用）
-     */
-    FileDO getFileByConfigIdAndPathIgnoreTenant(Long configId, String path);
-}
\ No newline at end of file
+}
diff --git a/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/service/file/FileServiceImpl.java b/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/service/file/FileServiceImpl.java
index be01e785..8c4f0ff2 100644
--- a/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/service/file/FileServiceImpl.java
+++ b/services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/service/file/FileServiceImpl.java
@@ -4,17 +4,11 @@ import cn.hutool.core.date.LocalDateTimeUtil;
 import cn.hutool.core.io.FileUtil;
 import cn.hutool.core.lang.Assert;
 import cn.hutool.core.util.RandomUtil;
-import cn.hutool.core.collection.CollUtil;
 import cn.hutool.core.util.StrUtil;
 import cn.hutool.crypto.digest.DigestUtil;
 import cn.zszj.framework.common.pojo.PageResult;
 import cn.zszj.framework.common.util.http.HttpUtils;
-import cn.zszj.framework.security.core.LoginUser;
-import cn.zszj.framework.tenant.core.context.TenantContextHolder;
-import cn.zszj.framework.tenant.core.util.TenantUtils;
-import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
 import cn.zszj.framework.common.util.object.BeanUtils;
-import cn.zszj.module.infra.enums.file.FileScopeEnum;
 import cn.zszj.module.infra.controller.admin.file.vo.file.FileCreateReqVO;
 import cn.zszj.module.infra.controller.admin.file.vo.file.FilePageReqVO;
 import cn.zszj.module.infra.controller.admin.file.vo.file.FilePresignedUrlRespVO;
@@ -26,24 +20,19 @@ import cn.zszj.module.infra.framework.file.core.utils.FileTypeUtils;
 import com.google.common.annotations.VisibleForTesting;
 import jakarta.annotation.Resource;
 import lombok.SneakyThrows;
-import org.springframework.security.access.AccessDeniedException;
-import lombok.extern.slf4j.Slf4j;
 import org.springframework.stereotype.Service;
 
 import java.util.List;
-import java.util.Objects;
 
 import static cn.hutool.core.date.DatePattern.PURE_DATE_PATTERN;
 import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
 import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_NOT_EXISTS;
-import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_SCOPE_INVALID;
 
 /**
  * 文件 Service 实现类
  *
  * @author 芋道源码
  */
-@Slf4j
 @Service
 public class FileServiceImpl implements FileService {
 
@@ -70,8 +59,6 @@ public class FileServiceImpl implements FileService {
 
     @Resource
     private FileConfigService fileConfigService;
-    @Resource
-    private cn.zszj.framework.common.biz.system.permission.PermissionCommonApi permissionCommonApi;
 
     @Resource
     private FileMapper fileMapper;
@@ -110,14 +97,10 @@ public class FileServiceImpl implements FileService {
         Assert.notNull(client, "客户端(master) 不能为空");
         String url = client.upload(content, path, type);
 
-        // 3. 保存到数据库。ZS-FILE-001.A：记录上传主体、默认私有（公开素材须管理员显式调整）
-        FileDO file = new FileDO().setConfigId(client.getId())
+        // 3. 保存到数据库
+        fileMapper.insert(new FileDO().setConfigId(client.getId())
                 .setName(name).setPath(path).setUrl(url)
-                .setType(type).setSize((long) content.length)
-                .setOwnerUserId(currentUserOrZero()).setScope(FileScopeEnum.PRIVATE.getScope());
-        // ZS-FILE-001.A：显式记录技术租户（服务端确认归属，不依赖拦截器装配）
-        file.setTenantId(TenantContextHolder.getTenantId());
-        fileMapper.insert(file);
+                .setType(type).setSize((long) content.length));
         return url;
     }
 
@@ -190,15 +173,8 @@ public class FileServiceImpl implements FileService {
         // 1.2 处理 URL 的合法性，移除 URL 中的查询参数（例如签名参数），保证 URL 的唯一性
         createReqVO.setUrl(HttpUtils.removeUrlQuery(createReqVO.getUrl())); // 目的：移除私有桶情况下，URL 的签名参数
 
-        // 2. 保存到数据库。ZS-FILE-001.A（codex r0 P1）：presigned create 端点已禁用（无上传申请绑定
-        // 无法证明对象归属，可冒领他人 configId/path 生成记录），本方法保留待 FILE-003 凭证化后重新接线；
-        // configId 不信任客户端指定，空值由服务端取 master 存储配置兜底；显式记录技术租户
+        // 2. 保存到数据库
         FileDO file = BeanUtils.toBean(createReqVO, FileDO.class);
-        if (file.getConfigId() == null) {
-            file.setConfigId(fileConfigService.getMasterFileClient().getId());
-        }
-        file.setOwnerUserId(currentUserOrZero()).setScope(FileScopeEnum.PRIVATE.getScope())
-                .setTenantId(TenantContextHolder.getTenantId());
         fileMapper.insert(file);
         return file.getId();
     }
@@ -227,15 +203,8 @@ public class FileServiceImpl implements FileService {
     @Override
     @SneakyThrows
     public void deleteFileList(List<Long> ids) {
-        // ZS-FILE-001.A（codex r0 P2）：批量删除前显式校验「全部存在且全部属于当前技术租户」——
-        // 混入他租户/不存在 id 整批拒绝（原先静默跳过，越权文件混入无感知）；不依赖租户拦截器装配
-        Long currentTenantId = TenantContextHolder.getTenantId();
-        List<FileDO> files = fileMapper.selectByIds(ids);
-        if (files.size() != CollUtil.distinct(ids).size()
-                || files.stream().anyMatch(f -> !Objects.equals(f.getTenantId(), currentTenantId))) {
-            throw exception(FILE_NOT_EXISTS);
-        }
         // 删除文件
+        List<FileDO> files = fileMapper.selectByIds(ids);
         for (FileDO file : files) {
             FilePathUtils.validatePath(file.getPath());
             // 获取客户端
@@ -269,92 +238,9 @@ public class FileServiceImpl implements FileService {
         return client.getContent(path);
     }
 
-    /**
-     * ZS-FILE-001.A（codex r0 P2）：下载场景跨租户定位文件记录——公开素材必须对任意租户/匿名
-     * 保持同一可用地址（租户过滤会把他租户 PUBLIC 过滤成 404）；PRIVATE 的租户归属校验
-     * 由 {@link #validateFileReadable} 以记录自身 tenant_id 执行。
-     */
-    @Override
-    public FileDO getFileByConfigIdAndPathIgnoreTenant(Long configId, String path) {
-        return TenantUtils.executeIgnore(() ->
-                fileMapper.selectLatestByConfigIdAndPath(configId, path));
-    }
-
     @Override
     public FileDO getFileByConfigIdAndPath(Long configId, String path) {
         return fileMapper.selectLatestByConfigIdAndPath(configId, path);
     }
 
-
-    /**
-     * ZS-FILE-001.A：当前登录用户编号；匿名/系统上下文返回 0（owner 列 NOT NULL DEFAULT 0 语义一致）。
-     */
-    private Long currentUserOrZero() {
-        Long userId = SecurityFrameworkUtils.getLoginUserId();
-        return userId != null ? userId : 0L;
-    }
-
-    /**
-     * ZS-FILE-001.A：统一读取授权——PUBLIC 匿名可读；PRIVATE 需登录且与文件同技术租户。
-     *
-     * @param file      文件（含 scope 与 tenantId）
-     * @param loginUser 下载发起者（匿名传 null；由 Controller 从安全上下文透传，便于单测）
-     * @throws AccessDeniedException 私有文件匿名/跨租户读取
-     */
-    @Override
-    public void validateFileReadable(FileDO file, LoginUser loginUser) {
-        if (file == null) {
-            return; // 不存在由调用方按 404 处理
-        }
-        if (FileScopeEnum.PUBLIC.getScope().equals(file.getScope())) {
-            return; // 公开素材：批准用途内匿名可读
-        }
-        // 私有附件（codex r0 P1 + r1 P1/P2）：必须登录，且满足其一——
-        // ① 上传所有者本人（ownerUserId>0 且与登录主体匹配且同租户；0=无个人所有者，禁止凭 userId=0 凭证冒领）；
-        // ② 同技术租户且实际持有 infra:file:query 权限（经 PermissionCommonApi 查询，与 @ss.hasPermission
-        //    同一数据源——OAuth scopes 与后台菜单权限是两套体系，不能作为判定依据）
-        if (loginUser == null) {
-            throw new AccessDeniedException("私有文件禁止匿名读取");
-        }
-        boolean ownerMatched = file.getOwnerUserId() != null && file.getOwnerUserId() > 0
-                && Objects.equals(file.getOwnerUserId(), loginUser.getId())
-                && Objects.equals(loginUser.getTenantId(), file.getTenantId());
-        if (ownerMatched) {
-            return;
-        }
-        boolean tenantMatched = Objects.equals(loginUser.getTenantId(), file.getTenantId());
-        boolean manager = tenantMatched && filePermissionFallback.apply(loginUser.getId(), "infra:file:query");
-        if (!manager) {
-            throw new AccessDeniedException("私有文件仅所有者或租户管理员可读取");
-        }
-    }
-
-    /**
-     * ZS-FILE-001.A codex r1 P2：管理面权限判定，与 {@code @ss.hasPermission} 一致走 PermissionCommonApi。
-     * 以函数字段注入便于单测；系统异常时保守返回 false（宁可拒绝也不放行）。
-     */
-    private final java.util.function.BiFunction<Long, String, Boolean> filePermissionFallback =
-            (userId, permission) -> {
-                try {
-                    return permissionCommonApi.hasAnyPermissions(userId, permission);
-                } catch (Exception ex) {
-                    log.warn("[filePermissionFallback][用户({}) 权限查询失败，保守拒绝 permission({})]", userId, permission, ex);
-                    return false;
-                }
-            };
-
-    /**
-     * ZS-FILE-001.A：管理员显式调整文件可见范围（历史存量迁移默认 PRIVATE，公开须显式标注）
-     */
-    @Override
-    public void updateFileScope(Long id, String scope) {
-        if (!FileScopeEnum.isValid(scope)) {
-            throw exception(FILE_SCOPE_INVALID);
-        }
-        FileDO file = validateFileExists(id);
-        FileDO updateObj = new FileDO().setId(file.getId()).setScope(scope);
-        fileMapper.updateById(updateObj);
-    }
-
-    // ZS-FILE-001.A 类尾占位
-}
\ No newline at end of file
+}

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "rg -n -C 5 'ZS-BPM-001|"'^### 6'"\\.5|"'^## 6'"\\.5' docs -g '02-*.md' -g '03-*.md' -g '05-*.md'; rg --files outputs scripts/gov; rg -n 'tools|JAVA_HOME|maven|install|surefire|skipTests' scripts/db/run-db018-verify.mjs services/zhongshu-core/pom.xml; git status --short; git rev-parse HEAD main" in E:\zszj-wt-bpm-001-a
 succeeded in 16ms:
3ea8051c176cf6403f0aff8c279982d101b0a76e
270edff26c445dbc841451876e6383b12846f3d4

codex
两阶段确实切换为 owner/app，根 POM 和 server POM 也保持 BPM 关闭。已找到几处验收证据缺口：Surefire 放行条件未检查跳过数，分页只查第一页，“关闭 BPM 无副作用”则仅查了数据库结构。接下来会核对失败路径和现有实测日志，确认影响范围。

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "node -e \"const fs=require('fs'); for(const p of process.argv.slice(1)){ console.log(p);console.log(fs.readFileSync(p,'utf8').split('\\n').map((l,i)=>(i+1)+': '+l).join('\\n'));}\" scripts/db/run-db018-verify.mjs scripts/gov/verify-docs.mjs services/zhongshu-core/docs/BPM引擎表与业务扩展表迁移责任决策.md outputs/bpm-001/runtime-report.json" in E:\zszj-wt-bpm-001-a
 succeeded in 0ms:
scripts/db/run-db018-verify.mjs
1: /**
2:  * ZS-DB-018：ORM、手写 SQL 与租户隔离的 PG 回归——两技术租户真实 PG 验证用例集。
3:  *
4:  * 依赖一次性 Docker PG17 容器（与 scripts/db/run-db006-verify.mjs / test-pg-fixture.mjs 同款隔离/清理语义），
5:  * 加载 V1 基线 + V2 回填后，用「与 TenantDatabaseInterceptor 改写结果逐字等价的 SQL」验证数据层租户链。
6:  *
7:  * 边界声明（与 05 文档 ZS-DB-018 卡一致）：
8:  *   TenantDatabaseInterceptor 是 MyBatis-Plus 层的 SQL 改写（对继承 TenantBaseDO 的表注入 tenant_id=ctx），
9:  *   不是 PG 原生 RLS；直接 JDBC 不会自动继承该规则。本套件验证的是「拦截器改写后的等效 SQL」在真实 PG 上的
10:  *   隔离语义（作用域内只见本租户、伪造他租户对象 ID 被拒），以及全局表/忽略注解/系统清理三类合法绕过范围；
11:  *   ORM 拦截器 Java 级装配与 HTTP 层租户比对（普通 tenant-id 与 Token 不一致拒绝）由 ZS-SEC-012.A 的
12:  *   SecurityFilterChainFixtureTest（双技术租户夹具）与 ZS-SEC-001.A 覆盖，本套件不重复、只做数据层补强。
13:  *   tenant_id 与业务组织 tenant_org_id 的映射属 D-09 后任务，本套件不改名、不建身份表，仅用 System 技术租户 1/2。
14:  *
15:  * 用例（ctx = TenantContextHolder 注入的租户号；作用域 SQL = ... AND tenant_id = ctx）：
16:  *   C1 CRUD 读取隔离：ctx=1 只见本租户部门，看不到租户2（SELECT 注入 tenant_id）；
17:  *   C2 分页隔离：LIMIT/OFFSET 在 tenant_id=ctx 之内，页内不越租户；忽略路径跨两租户（对照）；
18:  *   C3 关联隔离：users ⋈ dept 两端注入 tenant_id；伪造跨租户外键（T1 用户指向 T2 部门）被 JOIN 排除；
19:  *   C4 批量隔离：批量插入（造数多行 VALUES 跨两租户）+ 批量 UPDATE 作用域限本租户，他租户零影响；
20:  *   C5 逻辑删除隔离：本租户逻辑删除生效；跨租户按 PK 逻辑删除被拒（0 行）；
21:  *   C6 手写 SQL 隔离：自定义聚合/LEFT JOIN 注入 tenant_id=ctx，只见本租户分组；
22:  *   C7 伪造上下文/他租户对象 ID 被拒绝：ctx=1 按 PK 读/改/删租户2 对象均 0 行，目标行完好；
23:  *   C8 全局表合法范围：system_dict_data/infra_job_log 无 tenant_id 列（BaseDO+@TenantIgnore）→ 结构性不可租户化；
24:  *   C9 忽略注解合法范围：system_oauth2_access_token 有 tenant_id 列，仅 selectByAccessToken 方法级 @TenantIgnore 显式绕过；
25:  *   C10 系统清理合法范围：TokenCleanJob.execute @TenantIgnore → 跨租户全局有界清过期令牌（对照 ZS-DB-014/015）。
26:  * 附带 --self-test 负向对照：证明隔离断言非空洞（作用域内 0、忽略路径可见同一行）。
27:  * 任一用例失败 → 退出码非零。
28:  *
29:  * 用法：node scripts/db/run-db018-verify.mjs [--self-test]
30:  */
31: import { execFileSync, spawnSync } from 'node:child_process';
32: import { readFileSync } from 'node:fs';
33: import { fileURLToPath } from 'node:url';
34: import { join } from 'node:path';
35: 
36: const root = fileURLToPath(new URL('../../', import.meta.url));
37: const selfTest = process.argv.includes('--self-test');
38: 
39: function fail(code, message) { console.error(message); process.exit(code); }
40: const dockerUp = spawnSync('docker', ['version', '--format', '{{.Server.Version}}'], { encoding: 'utf8' });
41: if (dockerUp.error || dockerUp.status !== 0) fail(3, `[db018] Docker 不可用（${dockerUp.error?.message ?? `exit=${dockerUp.status}`}）：验证不得静默跳过`);
42: 
43: const container = `zszj-db018-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
44: const port = 2832 + Math.floor(Math.random() * 800);
45: let cleaned = false;
46: const cleanup = () => { if (!cleaned) { cleaned = true; try { execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { } } };
47: process.on('exit', cleanup);
48: process.on('SIGINT', () => { cleanup(); process.exit(130); });
49: 
50: console.log(`[db018] 拉起临时 PG（${container} @ 127.0.0.1:${port}）…`);
51: execFileSync('docker', ['run', '-d', '--name', container, '-e', 'POSTGRES_PASSWORD=db018', '-p', `127.0.0.1:${port}:5432`, 'postgres:17-alpine'], { stdio: 'ignore' });
52: 
53: const psql = (user, db, sql, onErrorStop = true) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', user, '-d', db,
54:   ...(onErrorStop ? ['-v', 'ON_ERROR_STOP=1'] : []), '-q'], { input: sql, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 });
55: const psqlOut = (user, db, sql) => spawnSync('docker', ['exec', container, 'psql', '-U', user, '-d', db, '-At', '-c', sql], { encoding: 'utf8', maxBuffer: 8 * 1024 * 1024 });
56: // 计数断言以超管读取（隔离语义与角色无关；写路径以 zhongshu_app 执行，贴近应用连接）
57: const count = (sql) => psqlOut('postgres', 'zhongshu', sql).stdout.trim();
58: 
59: // 就绪等待
60: let ready = false;
61: for (let i = 0; i < 30; i++) {
62:   if (psqlOut('postgres', 'postgres', 'SELECT 1').status === 0) { ready = true; break; }
63:   Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500);
64: }
65: if (!ready) { cleanup(); fail(1, '[db018] PG 未就绪'); }
66: 
67: let pass = 0, failCount = 0;
68: const results = [];
69: const record = (id, ok, note = '') => { results.push({ id, ok, note }); ok ? pass++ : failCount++; console.log(`[${ok ? 'PASS' : 'FAIL'}] ${id} ${note}`); };
70: 
71: // 环境：建库 + 环境方案角色 + V1/V2 迁移（以 owner 身份 = 迁移账号）
72: execFileSync('docker', ['exec', container, 'psql', '-U', 'postgres', '-q', '-c', 'CREATE DATABASE zhongshu;'], { stdio: 'ignore' });
73: {
74:   const setup = readFileSync(join(root, 'services/zhongshu-core/sql/postgresql/env-setup-test.sql'), 'utf8');
75:   const r = psql('postgres', 'zhongshu', setup);
76:   if (r.status !== 0) { cleanup(); fail(1, `[db018] 角色授权失败:\n${r.stdout}${r.stderr}`); }
77: }
78: {
79:   const v1 = readFileSync(join(root, 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260909.001__system_infra_baseline.sql'), 'utf8');
80:   const v2 = readFileSync(join(root, 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260909.002__infra_quartz_backfill.sql'), 'utf8');
81:   const r = psql('zhongshu_owner', 'zhongshu', v1 + '\n' + v2);
82:   if (r.status !== 0) { cleanup(); fail(1, `[db018] V1/V2 以迁移账号执行失败:\n${r.stdout}${r.stderr}`); }
83: }
84: console.log('[db018] 环境就绪：V1+V2 已由迁移账号（zhongshu_owner）执行');
85: 
86: // 造数：两个 System 技术租户（tenant_id=1 / tenant_id=2）跨四类表，含 C3 伪造跨租户外键与 C9/C10 令牌。
87: // 批量插入以多行 VALUES 一次性写入（覆盖卡片「批量」要求），全部经 zhongshu_app（应用连接）执行。
88: {
89:   const seed = `
90: BEGIN;
91: INSERT INTO system_dept (id, name, parent_id, sort, status, creator, create_time, updater, update_time, deleted, tenant_id) VALUES
92:  (900101, 'DB018-T1-研发部', 900100, 1, 0, 'db018', now(), 'db018', now(), 0, 1),
93:  (900102, 'DB018-T1-市场部', 900100, 2, 0, 'db018', now(), 'db018', now(), 0, 1),
94:  (900103, 'DB018-T2-研发部', 900100, 1, 0, 'db018', now(), 'db018', now(), 0, 2),
95:  (900104, 'DB018-T2-财务部', 900100, 2, 0, 'db018', now(), 'db018', now(), 0, 2),
96:  (900111, 'DB018-T1-PG-1', 900100, 11, 0, 'db018', now(), 'db018', now(), 0, 1),
97:  (900112, 'DB018-T1-PG-2', 900100, 12, 0, 'db018', now(), 'db018', now(), 0, 1),
98:  (900113, 'DB018-T1-PG-3', 900100, 13, 0, 'db018', now(), 'db018', now(), 0, 1),
99:  (900114, 'DB018-T1-PG-4', 900100, 14, 0, 'db018', now(), 'db018', now(), 0, 1),
100:  (900115, 'DB018-T1-PG-5', 900100, 15, 0, 'db018', now(), 'db018', now(), 0, 1),
101:  (900121, 'DB018-T2-PG-1', 900100, 11, 0, 'db018', now(), 'db018', now(), 0, 2),
102:  (900122, 'DB018-T2-PG-2', 900100, 12, 0, 'db018', now(), 'db018', now(), 0, 2),
103:  (900123, 'DB018-T2-PG-3', 900100, 13, 0, 'db018', now(), 'db018', now(), 0, 2);
104: INSERT INTO system_users (id, username, password, nickname, dept_id, status, creator, create_time, updater, update_time, deleted, tenant_id) VALUES
105:  (900201, 'db018_t1_u1', '', 'T1用户1', 900101, 0, 'db018', now(), 'db018', now(), 0, 1),
106:  (900202, 'db018_t1_u2', '', 'T1用户2', 900102, 0, 'db018', now(), 'db018', now(), 0, 1),
107:  (900203, 'db018_t2_u1', '', 'T2用户1', 900103, 0, 'db018', now(), 'db018', now(), 0, 2),
108:  (900204, 'db018_t1_u3', '', 'T1用户3-伪造跨租户部门', 900103, 0, 'db018', now(), 'db018', now(), 0, 1);
109: INSERT INTO system_role (id, name, code, sort, data_scope, data_scope_dept_ids, status, type, creator, create_time, updater, update_time, deleted, tenant_id) VALUES
110:  (900301, 'DB018-T1-角色', 'db018_t1_role', 1, 1, '', 0, 2, 'db018', now(), 'db018', now(), 0, 1),
111:  (900302, 'DB018-T2-角色', 'db018_t2_role', 1, 1, '', 0, 2, 'db018', now(), 'db018', now(), 0, 2);
112: INSERT INTO system_oauth2_access_token (id, user_id, user_type, user_info, access_token, refresh_token, client_id, expires_time, creator, create_time, updater, update_time, deleted, tenant_id) VALUES
113:  (900401, 900201, 2, '{}', 'db018-t1-token', 'db018-t1-rt', 'default', '2026-12-31 12:00:00', 'db018', now(), 'db018', now(), 0, 1),
114:  (900402, 900203, 2, '{}', 'db018-t2-token', 'db018-t2-rt', 'default', '2026-12-31 12:00:00', 'db018', now(), 'db018', now(), 0, 2),
115:  (900403, 900201, 2, '{}', 'db018-t1-expired', 'db018-t1-rtx', 'default', '2026-01-01 12:00:00', 'db018', now(), 'db018', now(), 0, 1),
116:  (900404, 900203, 2, '{}', 'db018-t2-expired', 'db018-t2-rtx', 'default', '2026-01-01 12:00:00', 'db018', now(), 'db018', now(), 0, 2);
117: COMMIT;`;
118:   const r = psql('zhongshu_app', 'zhongshu', seed);
119:   if (r.status !== 0) { cleanup(); fail(1, `[db018] 两租户造数失败（app 账号）:\n${r.stdout}${r.stderr}`); }
120:   console.log('[db018] 造数就绪：tenant_id=1/2 双技术租户（dept/users/role/oauth2_access_token）');
121: }
122: 
123: // C1 CRUD 读取隔离：SELECT 注入 tenant_id=ctx
124: {
125:   const own = count("SELECT count(*) FROM system_dept WHERE deleted=0 AND tenant_id=1 AND name LIKE 'DB018-T1-%'");
126:   const leak = count("SELECT count(*) FROM system_dept WHERE deleted=0 AND tenant_id=1 AND name LIKE 'DB018-T2-%'");
127:   record('C1 CRUD 读取隔离（SELECT 注入 tenant_id=ctx）', own === '7' && leak === '0', `本租户可见=${own}（期望7） 他租户可见=${leak}（期望0）`);
128: }
129: 
130: // C2 分页隔离：LIMIT/OFFSET 在 tenant_id=ctx 之内；忽略路径跨两租户作对照
131: {
132:   const pageBad = count("SELECT count(*) FROM (SELECT tenant_id FROM system_dept WHERE deleted=0 AND tenant_id=1 AND name LIKE 'DB018-T1-PG-%' ORDER BY id LIMIT 3 OFFSET 0) p WHERE p.tenant_id <> 1");
133:   const scopedTenants = count("SELECT count(DISTINCT tenant_id) FROM system_dept WHERE deleted=0 AND tenant_id=1 AND name LIKE 'DB018-%-PG-%'");
134:   const ignoredTenants = count("SELECT count(DISTINCT tenant_id) FROM system_dept WHERE deleted=0 AND name LIKE 'DB018-%-PG-%'");
135:   const t1Page = count("SELECT count(*) FROM system_dept WHERE deleted=0 AND tenant_id=1 AND name LIKE 'DB018-T1-PG-%'");
136:   record('C2 分页隔离（页内不越租户；作用域仅本租户，忽略路径跨租户）',
137:     pageBad === '0' && scopedTenants === '1' && ignoredTenants === '2' && t1Page === '5',
138:     `页内越界=${pageBad} 作用域租户数=${scopedTenants} 忽略路径租户数=${ignoredTenants} T1分页集=${t1Page}`);
139: }
140: 
141: // C3 关联隔离：JOIN 两端注入 tenant_id；伪造跨租户外键被排除
142: {
143:   const joined = count("SELECT count(*) FROM system_users u JOIN system_dept d ON u.dept_id=d.id AND d.tenant_id=1 AND d.deleted=0 WHERE u.tenant_id=1 AND u.deleted=0 AND u.username LIKE 'db018_t1_%'");
144:   const forgedDept = count("SELECT count(*) FROM system_dept WHERE id=900103 AND tenant_id=1 AND deleted=0");
145:   record('C3 关联隔离（JOIN 两端注入 tenant_id；伪造跨租户外键被排除）',
146:     joined === '2' && forgedDept === '0', `本租户关联命中=${joined}（期望2，u3跨租户被排除） 伪造部门在ctx=1可见=${forgedDept}（期望0）`);
147: }
148: 
149: // C4 批量隔离：批量 UPDATE 作用域限本租户，他租户零影响（批量插入见造数多行 VALUES）
150: {
151:   const beforeT2 = count("SELECT count(*) FROM system_dept WHERE tenant_id=2 AND name LIKE 'DB018-T2-%' AND sort >= 100");
152:   const upd = psql('zhongshu_app', 'zhongshu', "UPDATE system_dept SET sort = sort + 100 WHERE deleted=0 AND tenant_id=1 AND name LIKE 'DB018-T1-%'");
153:   const t1Updated = count("SELECT count(*) FROM system_dept WHERE tenant_id=1 AND name LIKE 'DB018-T1-%' AND sort >= 100");
154:   const afterT2 = count("SELECT count(*) FROM system_dept WHERE tenant_id=2 AND name LIKE 'DB018-T2-%' AND sort >= 100");
155:   record('C4 批量隔离（批量 UPDATE 作用域限本租户，他租户零影响）',
156:     upd.status === 0 && beforeT2 === '0' && t1Updated === '7' && afterT2 === '0',
157:     `T1批量更新=${t1Updated}（期望7） T2受影响=${afterT2}（期望0）`);
158: }
159: 
160: // C5 逻辑删除隔离：本租户生效；跨租户按 PK 逻辑删除被拒（0 行）
161: {
162:   const r = psql('zhongshu_app', 'zhongshu', `DO $$ DECLARE a int; BEGIN
163:     UPDATE system_dept SET deleted=1 WHERE id=900102 AND tenant_id=1 AND deleted=0;
164:     GET DIAGNOSTICS a=ROW_COUNT;
165:     IF a<>1 THEN RAISE EXCEPTION 'C5 本租户逻辑删除应影响1行，实际 %', a; END IF;
166:     UPDATE system_dept SET deleted=1 WHERE id=900103 AND tenant_id=1 AND deleted=0;
167:     GET DIAGNOSTICS a=ROW_COUNT;
168:     IF a<>0 THEN RAISE EXCEPTION 'C5 跨租户逻辑删除未被拒绝，影响 % 行', a; END IF;
169:   END $$;`);
170:   const t2Active = count("SELECT count(*) FROM system_dept WHERE id=900103 AND tenant_id=2 AND deleted=0");
171:   const t1Hidden = count("SELECT count(*) FROM system_dept WHERE id=900102 AND tenant_id=1 AND deleted=0");
172:   record('C5 逻辑删除隔离（本租户生效；跨租户按 PK 被拒）',
173:     r.status === 0 && t2Active === '1' && t1Hidden === '0',
174:     `${r.status === 0 ? 'DO校验通过' : (r.stderr.split('\n')[0] ?? '')} 租户2目标仍活跃=${t2Active}（期望1） 租户1已删隐藏=${t1Hidden}（期望0）`);
175: }
176: 
177: // C6 手写 SQL 隔离：自定义聚合/LEFT JOIN 注入 tenant_id=ctx
178: {
179:   const groups = psqlOut('zhongshu_app', 'zhongshu', "SELECT count(*) FROM (SELECT d.id FROM system_dept d LEFT JOIN system_users u ON u.dept_id=d.id AND u.deleted=0 AND u.tenant_id=1 WHERE d.deleted=0 AND d.tenant_id=1 AND d.name LIKE 'DB018-T1-PG-%' GROUP BY d.id) t").stdout.trim();
180:   const leak = count("SELECT count(*) FROM system_dept d WHERE d.deleted=0 AND d.tenant_id=1 AND d.name LIKE 'DB018-T2-%'");
181:   record('C6 手写 SQL 隔离（自定义聚合/JOIN 注入 tenant_id=ctx）',
182:     groups === '5' && leak === '0', `本租户分组数=${groups}（期望5） 他租户可见=${leak}（期望0）`);
183: }
184: 
185: // C7 伪造上下文/他租户对象 ID 被拒绝：ctx=1 按 PK 读/改/删租户2 对象均 0 行
186: {
187:   const r = psql('zhongshu_app', 'zhongshu', `DO $$ DECLARE a int; c int; BEGIN
188:     SELECT count(*) INTO c FROM system_dept WHERE id=900103 AND tenant_id=1 AND deleted=0;
189:     IF c<>0 THEN RAISE EXCEPTION 'C7 越权读未拒绝：% 行', c; END IF;
190:     UPDATE system_users SET status=1 WHERE id=900203 AND tenant_id=1;
191:     GET DIAGNOSTICS a=ROW_COUNT;
192:     IF a<>0 THEN RAISE EXCEPTION 'C7 越权改未拒绝：% 行', a; END IF;
193:     DELETE FROM system_role WHERE id=900302 AND tenant_id=1;
194:     GET DIAGNOSTICS a=ROW_COUNT;
195:     IF a<>0 THEN RAISE EXCEPTION 'C7 越权删未拒绝：% 行', a; END IF;
196:   END $$;`);
197:   const t2User = count("SELECT count(*) FROM system_users WHERE id=900203 AND tenant_id=2 AND status=0");
198:   const t2Role = count("SELECT count(*) FROM system_role WHERE id=900302 AND tenant_id=2 AND deleted=0");
199:   record('C7 伪造上下文/他租户对象 ID 被拒绝（读/改/删均 0 行，目标完好）',
200:     r.status === 0 && t2User === '1' && t2Role === '1',
201:     `${r.status === 0 ? 'DO校验通过' : (r.stderr.split('\n')[0] ?? '')} 租户2用户完好=${t2User}（期望1） 租户2角色完好=${t2Role}（期望1）`);
202: }
203: 
204: // C8 全局表合法范围：无 tenant_id 列者（BaseDO+@TenantIgnore）结构性不可租户化
205: {
206:   const col = (t) => count(`SELECT count(*) FROM information_schema.columns WHERE table_schema='public' AND table_name='${t}' AND column_name='tenant_id'`);
207:   const dict = col('system_dict_data'), jobLog = col('infra_job_log');
208:   const dept = col('system_dept'), users = col('system_users'), role = col('system_role');
209:   record('C8 全局表合法范围（全局表无 tenant_id 列；租户感知表有）',
210:     dict === '0' && jobLog === '0' && dept === '1' && users === '1' && role === '1',
211:     `dict_data=${dict} job_log=${jobLog}（期望0，全局） dept=${dept} users=${users} role=${role}（期望1，租户感知）`);
212: }
213: 
214: // C9 忽略注解合法范围：表有 tenant_id，仅注解方法（selectByAccessToken）显式绕过
215: {
216:   const col = count("SELECT count(*) FROM information_schema.columns WHERE table_schema='public' AND table_name='system_oauth2_access_token' AND column_name='tenant_id'");
217:   const scoped = count("SELECT count(*) FROM system_oauth2_access_token WHERE access_token='db018-t2-token' AND tenant_id=1 AND deleted=0");
218:   const ignored = count("SELECT count(*) FROM system_oauth2_access_token WHERE access_token='db018-t2-token' AND deleted=0");
219:   record('C9 忽略注解合法范围（作用域内隔离；仅 @TenantIgnore 方法路径显式绕过）',
220:     col === '1' && scoped === '0' && ignored === '1',
221:     `tenant_id列=${col}（期望1） ctx=1作用域查T2令牌=${scoped}（期望0，隔离） 忽略路径查T2令牌=${ignored}（期望1，合法令牌校验）`);
222: }
223: 
224: // C10 系统清理合法范围：TokenCleanJob @TenantIgnore → 跨租户全局有界清过期令牌
225: {
226:   const deadline = '2026-06-01 00:00:00';
227:   const before = count(`SELECT count(*) FROM system_oauth2_access_token WHERE expires_time < '${deadline}' AND access_token LIKE 'db018-%-expired'`);
228:   const del = psql('postgres', 'zhongshu', `DELETE FROM system_oauth2_access_token WHERE id IN (SELECT id FROM system_oauth2_access_token WHERE expires_time < '${deadline}' AND access_token LIKE 'db018-%-expired' LIMIT 10)`);
229:   const after = count(`SELECT count(*) FROM system_oauth2_access_token WHERE expires_time < '${deadline}' AND access_token LIKE 'db018-%-expired'`);
230:   const futureKept = count("SELECT count(*) FROM system_oauth2_access_token WHERE access_token IN ('db018-t1-token','db018-t2-token') AND deleted=0");
231:   record('C10 系统清理合法范围（清理 Job 忽略租户，跨租户有界清过期，未过期保留）',
232:     del.status === 0 && before === '2' && after === '0' && futureKept === '2',
233:     `清理前过期=${before}（期望2，跨两租户） 清理后过期=${after}（期望0） 未过期保留=${futureKept}（期望2）`);
234: }
235: 
236: // 负向对照（--self-test）：证明隔离断言非空洞——同一行作用域内不可见、忽略路径可见
237: if (selfTest) {
238:   const scoped = count("SELECT count(*) FROM system_dept WHERE id=900103 AND tenant_id=1 AND deleted=0");
239:   const ignored = count("SELECT count(*) FROM system_dept WHERE id=900103 AND deleted=0");
240:   record('S0 负向对照（隔离非空洞：作用域内 0，忽略路径可见同一行）', scoped === '0' && ignored === '1', `作用域=${scoped}（期望0） 忽略=${ignored}（期望1）`);
241: }
242: 
243: console.log(JSON.stringify({ pass, fail: failCount }, null, 0));
244: cleanup();
245: process.exit(failCount ? 1 : 0);
246: 
scripts/gov/verify-docs.mjs
1: /**
2:  * ZS-GOV-001 文档一致性检查器（本地与 CI 同一入口）。
3:  *
4:  * 校验范围：README.md 与 docs/0*.md（历史报告 docs/04 只查链接，不查内容口径）。
5:  * 规则：
6:  *  R1 链接有效：内联链接、引用定义的本地目标必须存在（外链/锚点跳过）；
7:  *  R2 任务编号唯一：05 文档 `### ZS-XXX-NNN` 标题不得重复；
8:  *  R3 状态枚举合法：05 卡片"状态 X"只允许 7 个枚举值；
9:  *  R4 决策门禁：未确认决策（D-07/D-10/D-11）不得被写成已批准/已确认/已落地；D-09 已于 2026-09-10 确认最小模型与账号唯一性细则，移出守护列表；
10:  *  R5 版本一致：README 文档索引的版本号与各文档头部"文档版本：V*"一致。
11:  *  R6 统计一致：05 §2 声明的状态分布必须等于卡片实际聚合（见 task-stats.mjs）；声明中省略的类别按 0 计；无声明句则整体跳过；
12:  *  R7 README 摘要一致：README"累计 N 项主任务（…）"的总数与各状态数必须等于 05 卡片实际聚合；省略的类别按 0 计；无声明句则整体跳过。
13:  * 用法：node scripts/gov/verify-docs.mjs（退出码非 0 = 不一致）
14:  */
15: import { readFileSync, existsSync, statSync } from 'node:fs';
16: import { fileURLToPath } from 'node:url';
17: import { dirname, join, resolve } from 'node:path';
18: import { countStatus, parseSection2Declared, parseReadmeDeclared } from './task-stats.mjs';
19: 
20: const root = fileURLToPath(new URL('../../', import.meta.url));
21: const DOCS = ['README.md',
22:   'docs/01-底座代码复用与改造方案.md',
23:   'docs/02-一期底座需求规格与待决策台账.md',
24:   'docs/03-底座二次开发顺序与验收标准.md',
25:   'docs/05-底座模块分析与开发任务清单.md',
26:   'docs/06-品牌素材与命名映射.md'];
27: const HISTORICAL = /^docs\/04-/; // 历史报告：只查链接，不重写口径
28: const STATUS_ENUM = ['待开发', '待决策', '待前置', '开发中', '待验收', '已验收', '暂缓'];
29: const UNCONFIRMED_DECISIONS = ['D-07', 'D-10', 'D-11']; // D-09 已于 2026-09-10 确认，移出守护列表
30: const VERSION_HEADER = /文档版本：\s*([A-Za-z0-9.]+)/;
31: 
32: export function checkDocs(files, readFile, rootDir, { exists = existsSync } = {}) {
33:   const issues = [];
34:   const contents = new Map();
35:   for (const f of files) {
36:     try {
37:       contents.set(f, readFile(f));
38:     } catch {
39:       issues.push({ rule: 'R0-read', file: f, message: '文件无法读取' });
40:     }
41:   }
42: 
43:   // R1 链接有效（校验范围：仓库内目标；仓库外引用属跨工作区引用，打印提示不计失败）
44:   for (const f of files) {
45:     const text = contents.get(f) ?? '';
46:     const baseDir = dirname(resolve(rootDir, f));
47:     const refs = [];
48:     for (const m of text.matchAll(/\]\(([^)\s]+)\)/g)) refs.push(m[1]);
49:     for (const m of text.matchAll(/^\[[^\]]+\]:\s+(\S+)$/gm)) refs.push(m[1]);
50:     for (const target of refs) {
51:       if (/^(https?:|mailto:|#)/.test(target)) continue;
52:       const clean = target.split('#')[0];
53:       if (!clean) continue;
54:       // 绝对路径 / 尖括号包裹目标：作者环境的仓库外引用，不校验
55:       if (/^<.+>$/.test(clean) || /^[A-Za-z]:[\\/]/.test(clean) || clean.startsWith('/')) {
56:         console.error(`[info] ${f}: 仓库外/绝对路径引用（不校验）: ${target}`);
57:         continue;
58:       }
59:       const abs = resolve(baseDir, decodeURI(clean));
60:       if (!exists(abs)) {
61:         if (!abs.startsWith(resolve(rootDir))) {
62:           console.error(`[info] ${f}: 仓库外引用（不校验）: ${target}`);
63:         } else {
64:           issues.push({ rule: 'R1-link', file: f, message: `链接目标不存在: ${target}` });
65:         }
66:       }
67:     }
68:   }
69: 
70:   // R2 任务编号唯一（仅 05）
71:   for (const f of files) {
72:     if (!f.includes('05-')) continue;
73:     const ids = [...(contents.get(f) ?? '').matchAll(/^### (ZS-[A-Z]+-\d{3})/gm)].map((m) => m[1]);
74:     const seen = new Set();
75:     for (const id of ids) {
76:       if (seen.has(id)) issues.push({ rule: 'R2-dup-id', file: f, message: `任务编号重复: ${id}` });
77:       seen.add(id);
78:     }
79:   }
80: 
81:   // R3 状态枚举（仅 05）
82:   for (const f of files) {
83:     if (!f.includes('05-')) continue;
84:     for (const m of (contents.get(f) ?? '').matchAll(/^-\s*关联[^\n]*?状态\s*([^\s；;，]+)/gm)) {
85:       if (!STATUS_ENUM.includes(m[1])) {
86:         issues.push({ rule: 'R3-status', file: f, message: `非法任务状态: "${m[1]}"（允许：${STATUS_ENUM.join('/')}）` });
87:       }
88:     }
89:   }
90: 
91:   // R4 未确认决策不得写成既成事实（否定句式如"不是已确认结论"属合规表述）
92:   for (const f of files) {
93:     const text = contents.get(f) ?? '';
94:     for (const d of UNCONFIRMED_DECISIONS) {
95:       for (const m of text.matchAll(new RegExp(`${d}[^。\\n|]{0,12}(已确认|已批准|已通过|已落地)`, 'g'))) {
96:         // 关键词前的否定/禁止措辞视为合规表述（规则自述文本同样会命中模式）
97:         const beforeKeyword = text.slice(m.index, m.index + m[0].length).slice(-8, -3);
98:         if (/不得|不应|禁止|不是|非 |未经|尚未$|未$/.test(beforeKeyword)) continue;
99:         issues.push({ rule: 'R4-decision', file: f, message: `未确认决策 ${d} 被写成既成事实: …${m[0]}…` });
100:       }
101:     }
102:   }
103: 
104:   // R5 版本一致（README 索引 vs 文档头）
105:   const readme = contents.get('README.md') ?? '';
106:   for (const m of readme.matchAll(/\[(文档[^\]]*|[^[\]]+)\]\((docs\/0[0-9][^)]*)\)\s*\|\s*([A-Za-z0-9.]+)/g)) {
107:     const [, , docPath, version] = m;
108:     const doc = files.find((f) => f === docPath);
109:     if (!doc) continue;
110:     if (HISTORICAL.test(docPath)) continue;
111:     const header = VERSION_HEADER.exec(contents.get(doc) ?? '');
112:     if (!header) issues.push({ rule: 'R5-version', file: docPath, message: '文档缺少"文档版本："头' });
113:     else if (header[1] !== version) {
114:       issues.push({ rule: 'R5-version', file: docPath, message: `README 索引版本 ${version} 与文档头 ${header[1]} 不一致` });
115:     }
116:   }
117: 
118:   // R6 统计一致（仅 05）：§2 声明分布必须等于卡片实际聚合；无声明句则整体跳过
119:   const doc05 = files.find((f) => f.includes('05-'));
120:   const actual05 = doc05 ? countStatus(contents.get(doc05) ?? '') : null;
121:   if (doc05) {
122:     const declared = parseSection2Declared(contents.get(doc05) ?? '');
123:     if (declared) {
124:       // 遍历全部状态枚举：声明中省略的类别按 0 计，杜绝「漏写某非零类别」逃过校验（codex P2）
125:       for (const s of STATUS_ENUM) {
126:         const d = declared[s] ?? 0;
127:         if (d !== actual05.counts[s]) {
128:           issues.push({ rule: 'R6-count', file: doc05, message: `§2 声明 ${s} ${d} 项，实际卡片聚合 ${actual05.counts[s]} 项` });
129:         }
130:       }
131:     }
132:   }
133: 
134:   // R7 README 摘要一致：README 声明的总数与各状态数必须等于 05 卡片实际聚合；无声明句或缺 05 则跳过
135:   const readmeFile = files.find((f) => f === 'README.md');
136:   if (readmeFile && actual05) {
137:     const rm = parseReadmeDeclared(contents.get(readmeFile) ?? '');
138:     if (rm) {
139:       if (rm.total !== actual05.cardCount) {
140:         issues.push({ rule: 'R7-readme-sync', file: readmeFile, message: `README 累计 ${rm.total} 项主任务，实际 ${actual05.cardCount} 项` });
141:       }
142:       for (const s of STATUS_ENUM) {
143:         const d = rm.declared[s] ?? 0;
144:         if (d !== actual05.counts[s]) {
145:           issues.push({ rule: 'R7-readme-sync', file: readmeFile, message: `README 声明 ${s} ${d} 项，实际 ${actual05.counts[s]} 项` });
146:         }
147:       }
148:     }
149:   }
150: 
151:   return issues;
152: }
153: 
154: const invokedDirectly = process.argv[1] && process.argv[1].replaceAll('\\', '/').endsWith('scripts/gov/verify-docs.mjs');
155: if (invokedDirectly) {
156:   const issues = checkDocs(DOCS, (f) => readFileSync(join(root, f), 'utf8'), root);
157:   console.log(JSON.stringify({ files: DOCS.length, issueCount: issues.length, issues }, null, 2));
158:   process.exitCode = issues.length ? 1 : 0;
159: }
160: 
services/zhongshu-core/docs/BPM引擎表与业务扩展表迁移责任决策.md
1: # BPM 引擎表与业务扩展表迁移责任决策（ZS-BPM-001）
2: 
3: > 文档日期：2026-09-14（ZS-BPM-001 技术准备先行件交付，B09 批次开工项）
4: > 上位依据：[03-底座二次开发顺序与验收标准](../../../docs/03-底座二次开发顺序与验收标准.md) B09（前置 B05/B08 与 D-07 批准，本卡「技术准备可先行」）、[05-任务清单](../../../docs/05-底座模块分析与开发任务清单.md) ZS-BPM-001 卡、[02-需求台账](../../../docs/02-一期底座需求规格与待决策台账.md) §6.5（D-07 确认前允许 PG 适配与中性技术夹具、禁止固化业务字段/状态）
5: > 执行入口：`node scripts/db/run-bpm001-verify.mjs`（已注册为 `run-pg-regression.mjs` 第 9 套件，本地与 CI 同规则）
6: 
7: ## 1. 决策结论
8: 
9: 1. **Flowable 引擎表（`ACT_*`）由引擎 schema 管理器自管，禁止任何手写 DDL 或 Flyway 接管。**
10:    建表/升级只允许发生在「迁移期」：以库 owner 账号（`zhongshu_owner`）在部署窗口执行
11:    `database-schema-update=true` 的引导（本验收阶段 1 即此形态）。引擎版本由
12:    `zszj-dependencies` 的 `flowable.version`（当前 8.0.0）钉住；引擎升级 = Flowable 自身
13:    schema 升级机制执行，升级前须在真实 PG 演练（同库 `schema-update=true` 幂等引导 +
14:    `ACT_GE_PROPERTY.schema.version` 前后比对，S2 检查守护）。
15: 2. **运行期零 DDL：低权限运行账号（`zhongshu_app`）以 `database-schema-update=false` 启动。**
16:    运行账号对引擎表仅有 DML 权限（`env-setup-test.sql` 的 default privileges 自动授权
17:    owner 新建表），S1 检查直证其建表被拒。这同时满足 B02「低权限运行账号不能建库/越权」
18:    反向验收在 BPM 域的延伸。
19: 3. **BPM 业务扩展表（`bpm_form_info`、`bpm_user_group`、`bpm_process_instance_ext` 等
20:    模块自有表）走 Flyway `V*__*.sql` 版本化迁移，与引擎表责任严格分离。** 未来启用
21:    zszj-server 的 BPM 装配（现在 server POM 与根 reactor 均保持注释=关闭模块，B01 白名单
22:    门禁守护）的批次，必须同时新增业务扩展表迁移；不得依赖引擎 schema 管理器创建业务表，
23:    也不得用 Flyway 创建/修改任何 `ACT_*` 表。当前 V1 基线不含任何 `ACT_*`/`bpm_*` 表
24:    （S0 检查：全基线零流程表 = 关闭 BPM 无流程路由/后台副作用的结构性证明）。
25: 4. **夹具边界（D-07 门禁）：** 验收只使用中性技术夹具（`tech_neutral_approval` 人工审批、
26:    `tech_neutral_async_echo` 异步回声），不导入请假示例，不固化任何众墅业务对象、状态机、
27:    组织任职语义；引擎 `tenantId` 在夹具中验证为「技术标签、无 ACL」（跨租户不过滤查询可见），
28:    业务组织/对象隔离归 B08/D-07 后的 ZS-BPM-002/003。
29: 
30: ## 2. 验收断言与证据位置（2026-09-14 实测）
31: 
32: | 验收项（ZS-BPM-001 卡） | 实现位置 | 结果 |
33: |---|---|---|
34: | PG 上部署、发起、通过、拒绝、撤回、转办、分页/历史查询 | `BpmPgHarnessBootstrapTest`（部署幂等/版本钉住/发起/租户标签）+ `BpmPgHarnessRuntimeTest` R20~R80 | 全部 PASS（见 outputs/bpm-001/runtime-report.json） |
35: | 重复部署可恢复（幂等） | bootstrap：同内容重复部署 deployment 数不变 | PASS |
36: | 重启可恢复 | 阶段 2 以独立 JVM、`schema-update=false`、低权限账号重启接入同库：定义/实例/历史/schema.version 全部一致（R10） | PASS |
37: | 事务失败可恢复 | R60：Spring `DataSourceTransactionManager` 回滚事务内的发起不留引擎痕迹；提交事务正常持久化 | PASS |
38: | 异步执行器与启停 | bootstrap 执行器挂起留积压（`ACT_RU_JOB`=1、探针零记录）→ runtime 重启后积压跨重启持久保留（执行器不自启），显式 `asyncExecutor.start()` 消化积压且无死信、实时路径再验、`shutdown()` 回到非活动态（R70，isActive 直证） | PASS |
39: | 关闭 BPM 无副作用 | S0：System/Infra 全基线零 `ACT_`/`FLW_` 表；server POM/根 reactor 保持 BPM 注释 + `ModuleWhitelistTest` 门禁不变 | PASS |
40: | 装配扩展点同构直证 | `BpmPgHarnessConfiguration` 与 `BpmFlowableConfiguration` 同用 `SpringProcessEngineConfiguration` + `setEventListeners` 扩展点，R90 证明监听器在真实 PG 引擎触发 | PASS |
41: 
42: 两阶段账号/参数：阶段 1 `zhongshu_owner` + `SCHEMA_UPDATE=true` + `ASYNC_EXECUTOR=false`；
43: 阶段 2 `zhongshu_app` + `SCHEMA_UPDATE=false` + `ASYNC_EXECUTOR=false`（执行器由 R70 显式
44: `start()`/`shutdown()` 控制生命周期，保证重启恢复断言的确定性）。编排器注入
45: `ZSZJ_BPM_HARNESS_*` 环境变量，缺失即快速失败，不静默跳过。
46: 
47: ## 3. 运行注意
48: 
49: - 首次运行会联网拉取 Flowable 8.0.0 依赖树（此前 server 未启用 BPM，本地仓库无 Flowable）。
50: - zszj-module-bpm 不在默认 reactor（根 POM 注释态），套件以 `mvn -f zszj-module-bpm/pom.xml`
51:   独立构建，兄弟模块依赖取自本地仓库已安装产物；**不通过根 POM profile 启用 reactor 成员**
52:   （`ModuleWhitelistTest` 以根 POM 非注释 `<module>` 为门禁事实源，引入 profile 会被判违规）。
53: - 就绪探测必须走 TCP（`-h 127.0.0.1`）：postgres 镜像 initdb 期间的临时服务器只监听
54:   unix socket，socket 探测可能误判就绪导致建库落到临时服务器失败（2026-09-14 首跑实测）。
55:   **移交给量**：本套件已按此修复；`run-db006/008-verify.mjs` 等既有套件仍是 socket 探测，
56:   2026-09-14 聚合首跑中二者再次竞态失败、单独复跑即绿——历史登记的「Docker 负载 flaky」
57:   相当部分可能即此根因，建议后续批次统一改为 TCP 探测（属各套件所属卡，不在本件改动）。
58: 
59: ## 4. 边界与后续
60: 
61: - 本件为 ZS-BPM-001「技术准备先行」交付：**B09 主卡链路仍受门禁**——ZS-BPM-002 审批资格
62:   与对象授权待 ZS-PERM-002.B（B08）；ZS-BPM-003 首链领域状态机待 D-07 批准 + B05/B08；
63:   ZS-OPS-001.E 门禁待 ZS-OPS-001.D。B09 整批放行不因本件提前。
64: - 正式首链启用 BPM 时须：server POM/根 reactor 解注释 + `ModuleWhitelist.ENABLED_MODULES`
65:   同步 + 业务扩展表 Flyway 迁移（§1.3）+ 以 owner 账号执行引擎表引导。
66: 
outputs/bpm-001/runtime-report.json
1: {
2:   "finishedAt": "2026-09-13T19:10:42.768Z",
3:   "container": "zszj-bpm001-1789326617788-99614",
4:   "results": [
5:     {
6:       "id": "S0 关闭BPM无副作用：基线零 ACT_/FLW_ 表",
7:       "ok": true,
8:       "note": "ACT_=0 FLW_=0"
9:     },
10:     {
11:       "id": "S1 运行账号无DDL：zhongshu_app 建表被拒",
12:       "ok": true,
13:       "note": "按预期失败"
14:     },
15:     {
16:       "id": "P1 bootstrap 引擎建表/部署幂等/发起/租户标签/版本钉住",
17:       "ok": true,
18:       "note": "tests=5 failures=0 errors=0 skipped=0"
19:     },
20:     {
21:       "id": "P2 runtime 重启恢复/通过/拒绝/撤回/转办/分页历史/事务回滚/积压恢复",
22:       "ok": true,
23:       "note": "tests=9 failures=0 errors=0 skipped=0"
24:     },
25:     {
26:       "id": "S2 运行期零DDL：引擎表数量与 schema.version 不变",
27:       "ok": true,
28:       "note": "ACT_ 39→39, schema.version 8.0.0.0→8.0.0.0"
29:     },
30:     {
31:       "id": "S3 异步回声探针落库（owner 引导版本 + app 运行期回声）",
32:       "ok": true,
33:       "note": "探针行=3"
34:     }
35:   ],
36:   "pass": 6,
37:   "failCount": 0
38: }

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'git -c core.quotepath=false diff main -- docs/reviews/README.md docs/reviews/codex-ZS-DB-018.md docs/reviews/codex-ZS-FILE-001.A.md docs/reviews/codex-ZS-PERM-001.A.md docs/reviews/codex-ZS-PERM-002.A.md docs/reviews/codex-ZS-SEC-001.A.md docs/reviews/codex-ZS-SEC-002.md docs/reviews/codex-ZS-SEC-005.md docs/reviews/codex-ZS-SEC-007.md docs/reviews/codex-ZS-SEC-012.A.md' in E:\zszj-wt-bpm-001-a
 succeeded in 0ms:
warning: unable to access 'C:\Users\Administrator/.config/git/ignore': Permission denied
diff --git a/docs/reviews/README.md b/docs/reviews/README.md
index a24e08d8..2307f180 100644
--- a/docs/reviews/README.md
+++ b/docs/reviews/README.md
@@ -121,18 +121,10 @@
 | ZS-CLIENT-003 修正请求凭据范围与异常收敛（B03 登录请求合同） | `47572250`（隔离分支 feat/client-003）→ `6de3fae7`（r0）→ `1e4d4ce3`（r1）→ `71e348f6`（r2），合并 `31a653cc` | impl Web+小程序两端请求层 | ✅ 评审通过（r0 1×P1+1×P2 → r1 1×P2 → r2 2×P1+1×P2 → r3 PASS / 0 发现；2×P2 延后登记） | [codex-ZS-CLIENT-003.md](codex-ZS-CLIENT-003.md) | r0 小程序 URL 兼容 + trace-id 合同 → r2 2×P1：协议相对 URL 不拼 baseURL 误判、刷新回放按凭据合同重放/清除 Authorization + 刷新 tenant-id 改单请求作用域堵外部直传泄露；Web vitest + 小程序 vitest 双端全绿 |
 | ZS-PERM-004.A 角色/菜单缓存和停用撤权的技术一致性（分批子项，父卡 ZS-PERM-004） | `f9e4a6a0`（隔离分支 feat/perm-004-a）→ `3c9eb759`（r0）→ `8d4ffa24`（r1）→ `75ad8ac4`（r2），合并 `9085f402` | impl 含 5 缓存一致性契约测试 | ✅ 评审通过（r0 3×P1+2×P2 → r1 2×P1+P3 → r2 2×P1 → r3 PASS，四弧 8×P1 全部处置） | [codex-ZS-PERM-004.A.md](codex-ZS-PERM-004.A.md) | r0 3×P1：ignore-caches 误增项被实证推翻回滚（底层 SQL 有租户过滤裸键共享会跨租户串数据）/租户套餐三方法 @DSTransactional 统一 @Transactional 消除 DS 空提交窗口/驱逐失败无补偿（RetryCacheErrorHandler+ERROR 证据）→ r1 createTenant 补回 @Transactional + 重试下沉 Cache 层 RetryEvictCache 修事务感知 afterCommit 绕过 errorHandler → r2 重试包装改 decorateCache 内层（TransactionAware→RetryEvict→RedisCache）+ 有界重试打 delegate 杜绝无界递归 → r3 PASS。TDD RED（deleteRoleList 不清缓存）→ 115/0 BUILD SUCCESS |
 | ZS-CFG-003.B 套餐/角色权限交集与变更生效（分批子项，父卡 ZS-CFG-003） | `ebd1c066`（隔离分支 feat/cfg-003-b）→ `788d80a1`（r0）→ `94f6a141`（r1）→ `8a7fa8e8`（r2）→ `df146763`（r3），合并 `8fea1a6c` | impl 含 5 契约测试 | ✅ 评审通过（r0→r4 五弧 8×P1+3×P2+P3 全部处置 → r4 PASS） | [codex-ZS-CFG-003.B.md](codex-ZS-CFG-003.B.md) | r0 TOCTOU+防御性放行推翻（授权/套餐变更/换套餐统一租户行锁 + 并发收缩×授权 FOR UPDATE 阻塞序列化测试）→ r1 套餐行锁统一「package→tenant」锁序+锁内重查绑定换出跳过 → r2 锁内读纪律（以锁定绑定/锁定套餐最新菜单判断与授权）→ r3 锁定读 null 拒绝 → r4 PASS。交付授权入口套餐子集校验（TENANT_PACKAGE_MENU_EXCEED 1-002-016-005，系统租户豁免）+ 8 契约测试，123/0 BUILD SUCCESS；主卡 CFG-003 转待验收 |
-| ZS-SEC-001.A 默认关闭跨租户权限跳过（HANDOFF 交接单补评，2026-09-14） | `5b8c702e` | 4 files（门控 + 两端 .env/头注入/切换 UI + 夹具翻转） | ✅ 评审通过（r0 直接 CLEAN 0 发现） | [codex-ZS-SEC-001.A.md](codex-ZS-SEC-001.A.md) | codex 确认后端门控在上下文变更前拦截租户切换、两端前端一致门控头注入与切换入口；测试沙箱未执行，通过性以交付时 33/33 夹具 + 前端 ts 基线为准。受控跨组织授权归 SEC-001.B（B08/D-09） |
-| ZS-SEC-002 接口分类、匿名白名单与方法权限清单（HANDOFF 交接单补评，2026-09-14） | `3fe87023` | ApiInventoryTest 静态扫描器 + 328 端点基线 | ✅ 评审通过（0×P0/P1；5×P2 全部登记处置） | [codex-ZS-SEC-002.md](codex-ZS-SEC-002.md) | 5×P2 均指向扫描器健壮性/门禁硬化：基线缺失静默自比（须改「缺失即失败」）、块注释注解误计、类级 RequestMapping/PreAuthorize 展开与生效语义、基线不保留完整表达式语义——登记归 SEC-002 后续小卡（与 OPS-001.C 门禁收口同窗）；现网 328 端点/25 匿名目录/主体推导未发现回归 |
-| ZS-SEC-005 统一错误响应、HTTP 状态与失败日志结果（HANDOFF 交接单补评，2026-09-14） | `e2b40185` | writeJSON 统一出口 6 文件 8 写点 + 畸形 JSON 归 400 | ✅ 评审通过（r0 直接 CLEAN 0 发现） | [codex-ZS-SEC-005.md](codex-ZS-SEC-005.md) | codex 确认响应写出口迁移、异常处理与新增测试无回归；方案 B（真实 HTTP 状态码）会断 Web axios 刷新链已按矩阵文档登记否决依据；端到端归 SEC-012.B（已收口） |
-| ZS-SEC-007 统一访问、异常与保护切面的日志脱敏（HANDOFF 交接单补评，2026-09-14） | `332b4d7c` | LogSanitizeUtils 四入口 + 5 写点集成 | ✅ r0 1×P1+1×P2 → hotfix 两弧（r1 揪出修复自身新 P1）→ r2 PASS / 0 发现 | [codex-ZS-SEC-007.md](codex-ZS-SEC-007.md)、[codex-hotfix-SEC-007-P1.md](codex-hotfix-SEC-007-P1.md) | P1：`@JsonRawValue` 字段（现网 AppDiyPagePropertyRespVO.property）经 valueToTree 成 POJONode(RawValue) 被 sanitizeNode 跳过，内嵌原文穿透响应日志脱敏；P2：`toLowerCase()` 缺 Locale.ROOT（土耳其 locale 下大写敏感键不命中）。hotfix（feat/sec-007，`9728b369`→`39c8b8dd`，合并 `a1e3a505`）：materializeRaw 物化（`RawValue.rawValue()` 解包实证）+ Locale.ROOT + 3 回归测试（RED 3 失败→GREEN 13/13）；r1 揪出修复自身引入的新 P1（物化失败 fallback 携带 toString 原文）→ fail-closed 上抛交安全摘要 → r2 CLEAN。经验：Jackson @JsonRawValue×valueToTree 须解包 RawValue；脱敏器失败路径也是安全面 |
-| ZS-SEC-012.A 真实安全链失败夹具（HANDOFF 交接单补评，2026-09-14） | `a4d9c6e1` | SecurityFilterChainFixtureTest 7 组 31 用例 | ✅ 评审通过（r0 直接 CLEAN 0 发现） | [codex-ZS-SEC-012.A.md](codex-ZS-SEC-012.A.md) | codex 确认夹具与提交时点安全/租户行为一致（真实 Filter 链全装配未禁用、双技术租户、基线失败如实暴露），移除的死测试依赖正确；端到端扩展归 SEC-012.B（已收口） |
-| ZS-PERM-001.A 授权目标归属与可授予上限校验（HANDOFF 交接单补评，2026-09-14） | `6eb81717` | PermissionServiceImpl 三授权方法写入前统一校验 + 9 夹具 | ✅ 评审通过（0×P0/P1；1×P2 登记处置） | [codex-ZS-PERM-001.A.md](codex-ZS-PERM-001.A.md) | P2：超管豁免经 hasAnySuperAdmin 只查角色编码不查状态——「禁用超管角色仍挂载 + 双角色组合」条件下可条件式绕过自我提权上限；登记归 PERM-001 后续小卡（与 IAM-003 岗位引用校验同窗）。33/33 夹具交付证据沿用 |
-| ZS-PERM-002.A 技术授权矩阵与通用对象级检查入口（HANDOFF 交接单补评，2026-09-14） | `1644070c` | DeptDataPermissionChecker + 授权矩阵文档 + 13 夹具 | ✅ 评审通过（r0 直接 CLEAN 0 发现） | [codex-ZS-PERM-002.A.md](codex-ZS-PERM-002.A.md) | codex 确认检查器与 rule 的 ALL/部门/本人语义一致、共享上下文缓存、批量整批拒绝与 fail-closed 正确；56/56 模块测试交付证据沿用 |
-| ZS-DB-018 ORM、手写 SQL 与租户隔离的 PG 回归（HANDOFF 交接单补评，2026-09-14） | `c3ae2e8e` | run-db018-verify.mjs C1~C10 + self-test | ✅ 评审通过（0×P0/P1；3×P2 登记处置） | [codex-ZS-DB-018.md](codex-ZS-DB-018.md) | 3×P2 均为验证脚本断言强度（JOIN 内容/分页内容/批量双租户）——已由 ZS-SYS-001.A 真实 API 级七类矩阵自然覆盖断言内容，脚本侧列为后续小卡改进；隔离本体（C1~C10、伪造上下文 0 行、三类合法范围）未发现回归 |
 | ZS-SEC-012.B 安全与双端请求联合验收（分批子项，父卡 ZS-SEC-012，B03 终环；他区会话交付、本表 2026-09-14 索引补登） | `25f3ff9e`（隔离分支 feat/sec-012-b），合并见 docs/05 V1.35 行 | impl 含 async/CORS/异步访问日志/G12/G13 门禁 | ✅ 评审通过（r0→r3 四弧 6×P1+8×P2 全部处置 → r3 PASS） | [codex-ZS-SEC-012.B.md](codex-ZS-SEC-012.B.md) | 四项交付：①全面 async 运行时合同（池化 executor+TTL BPP 交替租户防串号/异步异常统一出口/流式 ASYNC 派发不免认证）②CORS 端到端（嵌入容器真实链 evil 403 短路+批准源 ACAO/ACAH/ACEH）③异步访问日志产品修复（ASYNC 派发内记录+租户上下文重建+≥400 防伪报成功）④G12/G13 门禁与 CI security-chain job 本地 CI 同入口。132/0 BUILD SUCCESS；环境坑登记：本地 Maven 仓库陈旧构件致跨模块测试失真、须先 install 上游模块 |
 | ZS-BRAND-004.B 缓存、浏览器存储和会话命名切换联验（分批子项，父卡 ZS-BRAND-004） | `345c1c12`（隔离分支 feat/brand-004-b）→ `930d365e`（r0 1×P1+7×P2 全修）→ `ba13c81d`（r1 2×P2 全修），合并 `b1ba1b24` | 2 scripts（静态键清单门禁 + 运行期隔离证明） | ✅ 评审通过（r0 1×P1+7×P2 → r1 2×P2 → r2 PASS / 0 发现，三弧收敛） | [codex-ZS-BRAND-004.B.md](codex-ZS-BRAND-004.B.md) | r0 codex 揪出：POSIX 子进程泄漏（P1）、键提取三处漏报（无分号声明/.vue/DAO 值形状）、撤权取证被认证自愈掩盖（时序提前）、旧名反向用例区分度不足（升级为「真凭据旧名副本阳性对照 + MONITOR 命令流零读取」隔离直证）、并发刷新断言误过、缺 Docker TDZ → r1 误删 fail() 助手与 MONITOR 固定延时竞态 → r2 PASS。验证：51 Redis 键模式 + 35 两端存储键旧名残留 0、注入自检 8/8、真实 server + Docker Redis/PG 运行期 **39/39 断言全绿**；兼容窗口=0/自然过期+重登录策略已登记（浏览器 E2E 归 B06） |
 
-> 本目录另存有同批次的评审交接单（`HANDOFF-ZS-SEC-001.A`/`002`/`005`/`007`/`012.A`、`HANDOFF-ZS-PERM-001.A`/`002.A`、`HANDOFF-ZS-DB-018`）——**8 份交接单已于 2026-09-14 按「每批 4 份并行」补评收齐并入库**（SEC-001.A/005/012.A/PERM-002.A 四份 r0 CLEAN；SEC-002 5×P2、PERM-001.A 1×P2、DB-018 3×P2 全部登记处置；SEC-007 1×P1+1×P2 经 feat/sec-007 hotfix 两弧修复归零，见 codex-hotfix-SEC-007-P1.md），ZS-SEC-003 为该批次首份完成的 codex 评审，ZS-SEC-008 为第二份（首个经 r0→修复→r1 两轮闭环），ZS-SEC-009 为第三份（首个经 r0→返工→r1→P2 修复→r2 三轮闭环、且触发 Option B 分批拆出 SEC-009.B 的评审），ZS-CFG-002.B 为第四份、也是**首个非 SEC 且首个经 worktree 并行编排试点（提效方案 P1 步骤⑤）在隔离分支交付的 B03 子项**（r0 直接 0 发现），ZS-SEC-004 为第五份、也是**首个在 `--no-ff` 合并入 main 后由 codex r0 跨读前端发现集成回归（2×P1+1×P2）、经 TDD 经验裁决（Spring `checkOrigin` 复现）修复并 r1 复评归零的评审**——两阶段子代理评审因 worktree 内后端孤立视角遗漏，凸显「消费方审计必含 apps 前端」的跨栈评审必要性；ZS-CFG-001.B 为第六份、也是**首个经 r0→r1→r2 三弧级联深挖（r0 修数据损坏 → r1 揪出该修复引入的两步洗密旁路、codex jshell 实证复现 → r2 确认封堵归零）的评审**——凸显安全修复本身可能引入更深旁路，须对每个修复提交持续复评直至归零，而非修一轮即收口；ZS-SEC-010 为第七份、也是**首个 r0 同时揪出「安全能力自身引入的凭据泄露回归」（P1，给凭据端点加 `@RateLimiter` 反而激活拒绝日志泄露标量 `refreshToken` 与短信 `code`）与「跨模块租户解析缺口」（P2），P1 经 TDD 参数名感知脱敏修复并 r1 复评归零、P2 依约定延后（预认证端点不显现 + 需 protection 反依赖 biz-tenant）的评审**——凸显给凭据端点加限流/日志类防护时，防护自身可能成为新的泄露面，须以「复刻真实入参形态」的拒绝测试看守；ZS-SEC-011.A 为第八份、也是**首个经 r0→r1→r2→r3 四弧级联、且 P1 修复本身历经「包名前缀版→类型 `instanceof` 版」两轮深挖（r0 前缀排除 servlet → r1 揪出前缀漏排 Tomcat `ResponseFacade` 且 `mock(HttpServletResponse.class)` 因 ByteBuddy 类名以 `jakarta.servlet` 开头而假 GREEN → r2 确认类型排除正确、P1 归零 → r3 CLEAN）的评审**——凸显「排除基础设施对象须按类型 `instanceof` 而非包名前缀」（容器实现类落在 `org.apache.catalina.*` 等非 servlet 包，前缀过滤漏排）与「测容器行为须用具体子类而非接口 mock」（Mockito 接口 mock 的 ByteBuddy 类名以被 mock 类型包名开头、会假命中前缀过滤，须用 `ContainerLikeResponse extends MockHttpServletResponse` 复现运行时特征）两条教训；ZS-LOGIN-001 为第九份、也是**首个揪出「安全门控/开关机制自身对缓存态不完整」的评审**（r0 P1：新增令牌用途分离门控 `refresh-token-as-access-token-enabled` 只覆盖「Redis 未命中→DB→回退」路径、漏「Redis 命中」提前 return，致 gate 翻 false 后旧缓存合成令牌仍被放行至刷新令牌 TTL 30 天；TDD 修复使 gate 关闭时自愈 evict 污染条目、r1 CLEAN），且属**选项 C「部分交付 + 拆 .B」**（门控核心交付、父卡待验收、WS/IM 迁移与生产 gate 翻转拆 ZS-LOGIN-001.B）——凸显「新增安全开关/门控须覆盖全部数据路径（含缓存态），且开关翻转后历史残留数据须自愈清理，否则安全语义仅对新请求生效、对存量失效」；ZS-IAM-003 为第十份、也是**首个揪出「引用完整性/存在性保护自身被数据权限过滤架空」的评审**（r0 P2：负责人引用计数 `selectCountByLeaderUserId` 查 `DeptDO` 受 `DataPermissionConfiguration.addDeptColumn(DeptDO.class,"id")` 数据权限过滤，受限范围调用者删除用户时其范围外部门——该用户恰任负责人——被过滤漏计，删除放行遗留 `leaderUserId` 悬空，IAM-003 要防的悬空对范围外引用失效；用户选 Path A TDD 修复以 `DataPermissionUtils.executeIgnore` 包裹计数、保留租户过滤、以规避 CGLIB 代理的裸实例反射注入忠实断言计数作用域 RED→GREEN、r1 CLEAN），且与 ZS-LOGIN-001 同 worktree 分支 `feat/login-001`、同 `--no-ff` 合并 `b6b1e4a2`（两任务分别评审、串行文档同步）——凸显「新增引用完整性/存在性校验若查询落在受数据权限过滤的列上，保护会被调用者数据范围架空（范围外引用漏判），此类计数须 `executeIgnore` 关闭数据权限、仅保留租户过滤；且测试须规避 Spring CGLIB 代理（裸实例 + 反射注入）才能忠实断言作用域」。ZS-SEC-006 为第十一份、也是**首个揪出「新增客户端可发请求头未同步 CORS `allowedHeaders` 放行（跨域预检 403）+ 关联 ID 无参访问器每次新生成破坏关联一致性」双 P2 的评审**（r0 `--commit 8785b9f3`：① SEC-004 已在 `exposedHeaders` 暴露 trace-id 供跨域**读响应头**，但 SEC-006 新增「客户端可**发** trace-id 请求头」后 `allowedHeaders` 未放行 → 跨域预检 `Access-Control-Request-Headers: trace-id` 被 403 拦截、请求到不了 `TraceFilter`；② 无参 `getCorrelationId()` 无 OTel Span 时每次 `generateCorrelationId()`、即使 `TraceFilter` 已绑定请求属性 → 与响应头/访问日志/错误日志各不相同，违背「同一请求可关联」目标；r0 TDD 修复 `30e77350`：`allowedHeaders` 精确追加 trace-id + `CorsConfigTest` 预检断言、无参 `getCorrelationId()` 改三级委托（OTel → `ServletUtils.getRequest()` 复用绑定值 → 请求外一次性），r1 `--commit 30e77350` CLEAN/0 发现）——凸显「新增客户端可发请求头须同步 CORS `allowedHeaders` 放行（与 `exposedHeaders` 暴露响应头是两件事，缺一则跨域预检 403 拦截请求）、关联 ID 无参访问器须复用请求已绑定值而非每次新生成，否则与响应头/日志各不一致而破坏关联」。ZS-SEC-006 为整卡任务（非分批子项）、亦为 B03 Wave2 五项并行批次首份合并入 main（`e9daa398`）的评审。2026-09-13 收口补丁：补登上表 8 行此前面板漂移的已入库评审（ZS-CFG-004、ZS-LOGIN-002/003/004、ZS-CLIENT-003、ZS-PERM-004.A、ZS-CFG-003.B；其中 ZS-LOGIN-002/004 为随收口补丁新建的评审处置文档，其评审弧与最终结论自修复提交说明与 codex 会话日志恢复、原始 stdout 未随暂存保留已在文档内如实登记）。
+> 本目录另存有同批次的评审交接单（`HANDOFF-ZS-SEC-001.A`/`002`/`005`/`007`/`012.A`、`HANDOFF-ZS-PERM-001.A`/`002.A`、`HANDOFF-ZS-DB-018`），其对应 `codex-<TASK>.md` 评审产物尚待补齐；ZS-SEC-003 为该批次首份完成的 codex 评审，ZS-SEC-008 为第二份（首个经 r0→修复→r1 两轮闭环），ZS-SEC-009 为第三份（首个经 r0→返工→r1→P2 修复→r2 三轮闭环、且触发 Option B 分批拆出 SEC-009.B 的评审），ZS-CFG-002.B 为第四份、也是**首个非 SEC 且首个经 worktree 并行编排试点（提效方案 P1 步骤⑤）在隔离分支交付的 B03 子项**（r0 直接 0 发现），ZS-SEC-004 为第五份、也是**首个在 `--no-ff` 合并入 main 后由 codex r0 跨读前端发现集成回归（2×P1+1×P2）、经 TDD 经验裁决（Spring `checkOrigin` 复现）修复并 r1 复评归零的评审**——两阶段子代理评审因 worktree 内后端孤立视角遗漏，凸显「消费方审计必含 apps 前端」的跨栈评审必要性；ZS-CFG-001.B 为第六份、也是**首个经 r0→r1→r2 三弧级联深挖（r0 修数据损坏 → r1 揪出该修复引入的两步洗密旁路、codex jshell 实证复现 → r2 确认封堵归零）的评审**——凸显安全修复本身可能引入更深旁路，须对每个修复提交持续复评直至归零，而非修一轮即收口；ZS-SEC-010 为第七份、也是**首个 r0 同时揪出「安全能力自身引入的凭据泄露回归」（P1，给凭据端点加 `@RateLimiter` 反而激活拒绝日志泄露标量 `refreshToken` 与短信 `code`）与「跨模块租户解析缺口」（P2），P1 经 TDD 参数名感知脱敏修复并 r1 复评归零、P2 依约定延后（预认证端点不显现 + 需 protection 反依赖 biz-tenant）的评审**——凸显给凭据端点加限流/日志类防护时，防护自身可能成为新的泄露面，须以「复刻真实入参形态」的拒绝测试看守；ZS-SEC-011.A 为第八份、也是**首个经 r0→r1→r2→r3 四弧级联、且 P1 修复本身历经「包名前缀版→类型 `instanceof` 版」两轮深挖（r0 前缀排除 servlet → r1 揪出前缀漏排 Tomcat `ResponseFacade` 且 `mock(HttpServletResponse.class)` 因 ByteBuddy 类名以 `jakarta.servlet` 开头而假 GREEN → r2 确认类型排除正确、P1 归零 → r3 CLEAN）的评审**——凸显「排除基础设施对象须按类型 `instanceof` 而非包名前缀」（容器实现类落在 `org.apache.catalina.*` 等非 servlet 包，前缀过滤漏排）与「测容器行为须用具体子类而非接口 mock」（Mockito 接口 mock 的 ByteBuddy 类名以被 mock 类型包名开头、会假命中前缀过滤，须用 `ContainerLikeResponse extends MockHttpServletResponse` 复现运行时特征）两条教训；ZS-LOGIN-001 为第九份、也是**首个揪出「安全门控/开关机制自身对缓存态不完整」的评审**（r0 P1：新增令牌用途分离门控 `refresh-token-as-access-token-enabled` 只覆盖「Redis 未命中→DB→回退」路径、漏「Redis 命中」提前 return，致 gate 翻 false 后旧缓存合成令牌仍被放行至刷新令牌 TTL 30 天；TDD 修复使 gate 关闭时自愈 evict 污染条目、r1 CLEAN），且属**选项 C「部分交付 + 拆 .B」**（门控核心交付、父卡待验收、WS/IM 迁移与生产 gate 翻转拆 ZS-LOGIN-001.B）——凸显「新增安全开关/门控须覆盖全部数据路径（含缓存态），且开关翻转后历史残留数据须自愈清理，否则安全语义仅对新请求生效、对存量失效」；ZS-IAM-003 为第十份、也是**首个揪出「引用完整性/存在性保护自身被数据权限过滤架空」的评审**（r0 P2：负责人引用计数 `selectCountByLeaderUserId` 查 `DeptDO` 受 `DataPermissionConfiguration.addDeptColumn(DeptDO.class,"id")` 数据权限过滤，受限范围调用者删除用户时其范围外部门——该用户恰任负责人——被过滤漏计，删除放行遗留 `leaderUserId` 悬空，IAM-003 要防的悬空对范围外引用失效；用户选 Path A TDD 修复以 `DataPermissionUtils.executeIgnore` 包裹计数、保留租户过滤、以规避 CGLIB 代理的裸实例反射注入忠实断言计数作用域 RED→GREEN、r1 CLEAN），且与 ZS-LOGIN-001 同 worktree 分支 `feat/login-001`、同 `--no-ff` 合并 `b6b1e4a2`（两任务分别评审、串行文档同步）——凸显「新增引用完整性/存在性校验若查询落在受数据权限过滤的列上，保护会被调用者数据范围架空（范围外引用漏判），此类计数须 `executeIgnore` 关闭数据权限、仅保留租户过滤；且测试须规避 Spring CGLIB 代理（裸实例 + 反射注入）才能忠实断言作用域」。ZS-SEC-006 为第十一份、也是**首个揪出「新增客户端可发请求头未同步 CORS `allowedHeaders` 放行（跨域预检 403）+ 关联 ID 无参访问器每次新生成破坏关联一致性」双 P2 的评审**（r0 `--commit 8785b9f3`：① SEC-004 已在 `exposedHeaders` 暴露 trace-id 供跨域**读响应头**，但 SEC-006 新增「客户端可**发** trace-id 请求头」后 `allowedHeaders` 未放行 → 跨域预检 `Access-Control-Request-Headers: trace-id` 被 403 拦截、请求到不了 `TraceFilter`；② 无参 `getCorrelationId()` 无 OTel Span 时每次 `generateCorrelationId()`、即使 `TraceFilter` 已绑定请求属性 → 与响应头/访问日志/错误日志各不相同，违背「同一请求可关联」目标；r0 TDD 修复 `30e77350`：`allowedHeaders` 精确追加 trace-id + `CorsConfigTest` 预检断言、无参 `getCorrelationId()` 改三级委托（OTel → `ServletUtils.getRequest()` 复用绑定值 → 请求外一次性），r1 `--commit 30e77350` CLEAN/0 发现）——凸显「新增客户端可发请求头须同步 CORS `allowedHeaders` 放行（与 `exposedHeaders` 暴露响应头是两件事，缺一则跨域预检 403 拦截请求）、关联 ID 无参访问器须复用请求已绑定值而非每次新生成，否则与响应头/日志各不一致而破坏关联」。ZS-SEC-006 为整卡任务（非分批子项）、亦为 B03 Wave2 五项并行批次首份合并入 main（`e9daa398`）的评审。2026-09-13 收口补丁：补登上表 8 行此前面板漂移的已入库评审（ZS-CFG-004、ZS-LOGIN-002/003/004、ZS-CLIENT-003、ZS-PERM-004.A、ZS-CFG-003.B；其中 ZS-LOGIN-002/004 为随收口补丁新建的评审处置文档，其评审弧与最终结论自修复提交说明与 codex 会话日志恢复、原始 stdout 未随暂存保留已在文档内如实登记）。
 
 ## 治理与门禁工具链专项评审状态（ZS-GOV-001 / ZS-OPS-001.A）
 
diff --git a/docs/reviews/codex-ZS-DB-018.md b/docs/reviews/codex-ZS-DB-018.md
deleted file mode 100644
index 64d7c3b2..00000000
--- a/docs/reviews/codex-ZS-DB-018.md
+++ /dev/null
@@ -1,19 +0,0 @@
-# ZS-DB-018 codex 评审处置（HANDOFF 补评，3×P2 登记处置）
-
-- 评审工具：codex（gpt-6-astra / xhigh，read-only sandbox，`codex review --commit c3ae2e8e` @ main）
-- 评审对象：ZS-DB-018 ORM、手写 SQL 与租户隔离的 PG 回归（HANDOFF-ZS-DB-018 交接单的待评审提交；2026-09-14 补评入库）
-- 结论：**0×P0/P1，3×P2 全部登记处置**（按 reviews/README「后续处理约定」第 2 条，非阻塞）
-- 原始日志：`outputs/handoff/ZS-DB-018.txt`（未入库；本档案为处置入库）
-
-## 发现与处置（3×P2，全部指向验证脚本 `run-db018-verify.mjs` 的断言强度）
-
-| # | 发现 | 处置 |
-|---|---|---|
-| 1 | **Assert joined-user data in the LEFT JOIN isolation case**（L179-182）：C3 关联 JOIN 用例断言本租户可见/他租户 0 行，但未断言 JOIN 到的用户列数据内容正确 | 登记处置：断言强化——JOIN 内容断言随 ZS-SYS-001.A 真实 API 级回归自然覆盖（七类矩阵含关联读回），脚本侧列为后续小卡改进项 |
-| 2 | **Assert the actual paginated result's size and contents**（L132-137）：C2 分页用例未断言分页结果集大小与内容 | 同上：SYS-001.A 分页矩阵用例已覆盖内容级断言；脚本侧登记 |
-| 3 | **Make the batch-update filter target both tenants**（L152-156）：C4 批量更新用例的过滤条件未同时覆盖双租户形态 | 同上：批量混入拒绝已由 SYS-001.A 跨类用例覆盖；脚本侧登记 |
-
-## 验证（交付时点证据，沿用卡片开发记录）
-
-- 真实 PG17（一次性 Docker 容器）C1~C10 全过：CRUD/分页/JOIN/批量/逻辑删除/聚合按 tenant_id 隔离、伪造上下文 PK 越权 0 行、三类合法范围（全局表/@TenantIgnore/系统清理）结构断言、`--self-test` 负向对照证明隔离非空洞；已注册进 `run-pg-regression.mjs`（8 套件，2026-09-13 空载复跑 8/8 全绿）
-- SQL 级等效语句验证的边界（非 PG 原生 RLS、拦截器端到端归 SEC-012.A/B）已在卡片登记
diff --git a/docs/reviews/codex-ZS-FILE-001.A.md b/docs/reviews/codex-ZS-FILE-001.A.md
deleted file mode 100644
index 1d3e3e14..00000000
--- a/docs/reviews/codex-ZS-FILE-001.A.md
+++ /dev/null
@@ -1,35 +0,0 @@
-# ZS-FILE-001.A codex 评审处置（r0→r2 三弧）
-
-- 评审工具：codex（gpt-6-astra / xhigh、high，read-only sandbox）
-- 评审对象：分支 `feat/file-001-a`（ZS-FILE-001.A 技术账号/tenant 私有文件归属与授权）
-- 结论：**r2 PASS / 0 发现**（评审收敛）
-
-## 交付内容
-
-1. **文件租户化**：迁移 `V20260914.001__infra_file_tenancy.sql`（infra_file 加 tenant_id/owner_user_id/scope，存量默认 PRIVATE=「不能默认全部公开」）；FileDO 改 TenantBaseDO + ownerUserId/scope；FileScopeEnum（PUBLIC/PRIVATE）。
-2. **写路径归属**：createFile 两路径记录 ownerUserId（服务端确认）+ scope=PRIVATE + 显式 tenantId；presigned create 的 configId 不信任客户端、空值由 master 兜底。
-3. **读取授权统一**：下载端点去 @TenantIgnore（保留 @PermitAll），先跨租户定位记录（getFileByConfigIdAndPathIgnoreTenant，PUBLIC 对任意来源同址可用）再 `validateFileReadable`：PUBLIC 匿名可读；PRIVATE 需「ownerUserId>0 且本人且同租户」或「同租户且 PermissionCommonApi 查得 infra:file:query」（与 @ss.hasPermission 同源；scopes 与后台权限是两套体系被否决）；yaml tenant ignore-urls 放行下载路径。
-4. **删除授权**：deleteFileList 显式校验「全部存在且全部同租户」（不依赖拦截器装配），混入越权/不存在 id 整批拒绝。
-5. **update-scope 管理端点**（infra:file:update 权限 + scope 合法性校验）；presigned create 端点管理端+App 端**同步禁用**（无上传申请绑定可冒领他人文件，凭证化重新交付归 FILE-003）；ApiInventory 基线 328→327 审查通过。
-6. **测试**：FileServiceAuthorizationTest 12 用例（默认 PRIVATE+归属/PUBLIC 匿名/PRIVATE 匿名拒/他租户拒/owner 本人/owner=0 userId=0 冒领拒/管理员放行/无权限拒/批删两向/update-scope 两向）+ 补他区欠账 DatabaseTable 列数断言。
-
-## 评审弧
-
-| 轮次 | 结论 | 要点 |
-|---|---|---|
-| r0 | FAIL 3×P1+3×P2+P3 | P1 PRIVATE 只查租户不查归属；P1 presigned create 可冒领他人文件；P1 S3 直链绕过平台授权；P2 PUBLIC 登录后 404（跨租户查询）；P2 迁移历史归属无恢复路径；P2 测试未证跨租户隔离；P3 错误码冲突 |
-| r1 | FAIL 2×P1+2×P2+P3 | P1 owner=0 凭 userId=0 client-credentials 令牌冒领（owner 分支加 ownerUserId>0）；P1 App 端 /create 漏禁（同步移除）；P2 scopes≠后台权限（管理分支改 PermissionCommonApi 同源）；P2 双端 client 上传模式残留假成功（登记 FILE-003/B06 处置）；P3 批删测试被数量校验短路（拆分） |
-| r2 | **PASS / 0 发现** | — |
-
-## 验证
-
-- infra 全量：**248 → 252 tests / 0 failures，BUILD SUCCESS**（含新增授权 12 用例）。
-- ApiInventoryTest 基线匹配（327 端点）。
-- 合并：`--no-ff` 合并回 main（见 docs/05 V1.38 记录）。
-
-## 延后/边界（非阻塞）
-
-1. **S3/云存储直链绕过**：PRIVATE 的存储桶/CDN 直读限制与受控取流 → ZS-FILE-004.A（主体绑定票据/取流协议，B04 下一环）。
-2. **历史文件归属认领与分类迁移流程**（存量 owner=0/tenant=0 的运营处置）→ FILE-001.B（B08）或运营规程；管理面可经 update-scope 显式调整。
-3. **双端 client 上传模式残留假成功**（后端端点已禁用，前端模式开关未清）→ FILE-003 凭证化时一并收敛（或 B06 前端批次）。
-4. **真实容器全链下载授权测试**（TokenAuthenticationFilter×ignore-urls×下载端点）→ ZS-SYS-001.A 真实 PG/API 回归（他区开发中）。
diff --git a/docs/reviews/codex-ZS-PERM-001.A.md b/docs/reviews/codex-ZS-PERM-001.A.md
deleted file mode 100644
index 58d0e46d..00000000
--- a/docs/reviews/codex-ZS-PERM-001.A.md
+++ /dev/null
@@ -1,20 +0,0 @@
-# ZS-PERM-001.A codex 评审处置（HANDOFF 补评，1×P2 登记处置）
-
-- 评审工具：codex（gpt-6-astra / xhigh，read-only sandbox，`codex review --commit 6eb81717` @ main）
-- 评审对象：ZS-PERM-001.A 授权目标归属与可授予上限校验（HANDOFF-ZS-PERM-001.A 交接单的待评审提交；2026-09-14 补评入库）
-- 结论：**0×P0/P1，1×P2 登记处置**（按 reviews/README「后续处理约定」第 2 条，非阻塞）
-- 原始日志：`outputs/handoff/ZS-PERM-001.A.txt`（未入库；本档案为处置入库）
-
-## 发现与处置
-
-| 级别 | 发现 | 处置 |
-|---|---|---|
-| P2 | **Exclude disabled roles from the super-admin exemption**（PermissionServiceImpl L460-462）：`validateUserRoleGrantCeiling` 的超管豁免经 `RoleServiceImpl.hasAnySuperAdmin()` 只查角色编码、不查角色状态——操作者保留已禁用的 super_admin 角色 + 经另一启用角色持 `assign-user-role` 权限时，豁免仍生效可绕过自我提权上限（条件式绕过，与 `hasAnyPermissions()` 过滤禁用角色的语义不一致） | 登记处置：超管豁免应在判定前过滤禁用角色（对齐 hasAnyPermissions 语义）。触发需「禁用超管角色仍挂载 + 双角色组合」双重条件，实际风险受控；归 ZS-PERM-001 后续小卡（与 ZS-IAM-003 岗位引用校验 hotfix 同窗处置） |
-
-## 验证（交付时点证据，沿用卡片开发记录）
-
-- `-pl :zszj-module-system "-Dtest=PermissionServiceTest" test`：33 tests / 0 failures（2026-09-09T14:05:26+08:00，24 既有 + 9 新增：错租户 user/role/dept 拒绝、批量混入、禁用角色拒绝、自我提权、超上限、重复授权幂等等）
-
-## 边界
-
-本子项仅同技术租户归属校验；获准跨组织范围归 ZS-PERM-001.B（B08/D-09）。前置 ZS-LOGIN-005.A、ZS-PERM-004.A 已按 §16.1 满足并分别评审（codex-ZS-LOGIN-005.A.md / codex-ZS-PERM-004.A.md）。
diff --git a/docs/reviews/codex-ZS-PERM-002.A.md b/docs/reviews/codex-ZS-PERM-002.A.md
deleted file mode 100644
index 4e29d0fe..00000000
--- a/docs/reviews/codex-ZS-PERM-002.A.md
+++ /dev/null
@@ -1,20 +0,0 @@
-# ZS-PERM-002.A codex 评审处置（HANDOFF 补评，r0 一次通过）
-
-- 评审工具：codex（gpt-6-astra / xhigh，read-only sandbox，`codex review --commit 1644070c` @ main）
-- 评审对象：ZS-PERM-002.A 技术授权矩阵与通用对象级检查入口（HANDOFF-ZS-PERM-002.A 交接单的待评审提交；2026-09-14 补评入库）
-- 结论：**r0 CLEAN / 0 发现**（评审通过，无需修复弧）
-- 原始日志：`outputs/handoff/ZS-PERM-002.A.txt`（未入库；本档案为处置入库）
-
-## 结论原文
-
-> "No actionable defects were identified. The checker preserves the existing department/self permission semantics and shares the rule's cache correctly. Tests were reviewed but not executed in the read-only environment."
-
-codex 确认 `DeptDataPermissionChecker` 与 `DeptDataPermissionRule` 的 ALL/命中部门/本人语义完全一致、共享同一 LoginUser 上下文缓存（无重复计算与语义漂移），批量「任一越权整批拒绝」与 fail-closed 护栏正确。
-
-## 验证（交付时点证据，沿用卡片开发记录）
-
-- `mvn -pl :zszj-spring-boot-starter-biz-data-permission test`：全模块 56 tests / 0 failures（43 既有含 DeptDataPermissionRuleTest 8 + 新增 13：ALL/部门/本人/批量混入整批拒绝/空批量/护栏跳过/fail-closed/上下文缓存复用）；`run-local-gates --fast` 10/10
-
-## 边界
-
-检查器对各业务表/路径的逐一接入随领域模块落地；真实 PG+HTTP 端到端对象授权联验归 ZS-SEC-012.B（已收口）/ZS-SYS-001.A（在制）；业务组织 SELF/ASSIGNED 矩阵归 ZS-PERM-002.B（B08/D-09）。
diff --git a/docs/reviews/codex-ZS-SEC-001.A.md b/docs/reviews/codex-ZS-SEC-001.A.md
deleted file mode 100644
index 0691cc04..00000000
--- a/docs/reviews/codex-ZS-SEC-001.A.md
+++ /dev/null
@@ -1,21 +0,0 @@
-# ZS-SEC-001.A codex 评审处置（HANDOFF 补评，r0 一次通过）
-
-- 评审工具：codex（gpt-6-astra / xhigh，read-only sandbox，`codex review --commit 5b8c702e` @ main）
-- 评审对象：ZS-SEC-001.A 默认关闭跨租户权限跳过（HANDOFF-ZS-SEC-001.A 交接单的待评审提交；2026-09-14 补评入库）
-- 结论：**r0 CLEAN / 0 发现**（评审通过，无需修复弧）
-- 原始日志：`outputs/handoff/ZS-SEC-001.A.txt`（未入库；本档案为处置入库）
-
-## 结论原文
-
-> "No actionable regressions were found. The backend gate blocks tenant switching before context mutation, and both frontends consistently gate header injection and switching controls. Tests were inspected but not executed in the read-only environment."
-
-codex 确认三件事：①后端门控在上下文变更（visitTenantId 写入）之前拦截租户切换；②admin-web 与 miniapp 两端一致门控 `visit-tenant-id` 头注入与切换入口 UI；③测试经检视未在沙箱执行（通过性以交付时本机 `SecurityFilterChainFixtureTest,CrossTenantVisitEnabledFixtureTest` 33/33 + 前端 ts 基线证据为准）。
-
-## 验证（交付时点证据，沿用卡片开发记录）
-
-- Maven `-pl :zszj-spring-boot-starter-biz-tenant "-Dtest=SecurityFilterChainFixtureTest,CrossTenantVisitEnabledFixtureTest" test`：33 tests / 0 failures（2026-09-09T13:26:09+08:00）
-- admin-web `verify-ts-baseline.mjs`（currentErrors 11 = baseline 11）、miniapp `vue-tsc --noEmit` 0 错误
-
-## 边界
-
-获批业务组织方案（D-09）后的受控跨组织授权（服务端授权记录/策略、正向矩阵）归 ZS-SEC-001.B（B08），本子项仅关闭旧放大能力。
diff --git a/docs/reviews/codex-ZS-SEC-002.md b/docs/reviews/codex-ZS-SEC-002.md
deleted file mode 100644
index 76e8a2ee..00000000
--- a/docs/reviews/codex-ZS-SEC-002.md
+++ /dev/null
@@ -1,24 +0,0 @@
-# ZS-SEC-002 codex 评审处置（HANDOFF 补评，5×P2 登记处置）
-
-- 评审工具：codex（gpt-6-astra / xhigh，read-only sandbox，`codex review --commit 3fe87023` @ main）
-- 评审对象：ZS-SEC-002 接口分类、匿名白名单与方法权限清单（HANDOFF-ZS-SEC-002 交接单的待评审提交；2026-09-14 补评入库）
-- 结论：**0×P0/P1，5×P2 全部登记处置**（按 reviews/README「后续处理约定」第 2 条，非阻塞）
-- 原始日志：`outputs/handoff/ZS-SEC-002.txt`（未入库；本档案为处置入库）
-
-## 发现与处置（5×P2，全部指向 `ApiInventoryTest` 静态扫描器）
-
-| # | 发现 | 处置 |
-|---|---|---|
-| 1 | **Fail verification when the committed baseline is missing**（L84-86）：从 checkout 省略基线文件运行时，验证分支静默写入当前清单再与自身比对——路由/权限变化在匿名目录不变时可不带 `api.inventory.update=true` 通过 | 登记处置：门禁硬化项——基线缺失必须失败、仅 `update=true` 允许创建。归 ZS-SEC-002 后续小卡（与 ZS-OPS-001.C 门禁收口同窗处理；当前 CI 固定提交环境基线恒存在，实际漏检需多重条件叠加） |
-| 2 | **Exclude block comments before scanning annotations**（L231-233）：行首 `*` Javadoc 行已忽略，但整段块注释（非行首 `*` 形态）中的注解样例可能被误计 | 登记处置：扫描器健壮性——真实 Controller 无此类形态（交付时 PERM=251 与 grep 差 2 已实证注释识别有效），归同上小卡 |
-| 3 | **Expand every class-level request-mapping path**（L218-220）：类级 `@RequestMapping` 含占位符/多值时路径展开不完全 | 登记处置：现网 328 端点清单与运行时 HandlerMapping 一致性已由 SEC-012.B 端到端合同间接覆盖；归同上小卡 |
-| 4 | **Include effective class-level PreAuthorize guards**（L216-223）：类级 @PreAuthorize 的生效语义未并入权限判定 | 登记处置：启用模块无类级 @PreAuthorize 用法（清单 PERMISSION=251 全部方法级）；防御性登记归同上小卡 |
-| 5 | **Preserve authorization expression semantics in the baseline**（L318-324）：基线行仅存权限标识，不保留完整 SpEL 表达式语义（hasAnyAuthority/scope 组合） | 登记处置：结构断言已保证标识合法形态；表达式级语义审计归 ZS-SEC-002 后续小卡/B03 放行评审裁量 |
-
-## 结论原文（总述）
-
-codex 对清单生成器、主体类型推导（ADMIN=322/MEMBER=6）、匿名白名单三来源核清、双向漂移门禁均未发现 P0/P1 回归；5 项发现全部为建议级（P2）扫描器健壮性/门禁硬化项。
-
-## 验证（交付时点证据，沿用卡片开发记录）
-
-- `-pl :zszj-server -Dtest=ApiInventoryTest,ModuleWhitelistTest test`：8/8 BUILD SUCCESS；`-pl :zszj-spring-boot-starter-biz-tenant -Dtest=SecurityFilterChainFixtureTest`：35/35；`run-local-gates --fast` 10/10
diff --git a/docs/reviews/codex-ZS-SEC-005.md b/docs/reviews/codex-ZS-SEC-005.md
deleted file mode 100644
index d4c8a00f..00000000
--- a/docs/reviews/codex-ZS-SEC-005.md
+++ /dev/null
@@ -1,21 +0,0 @@
-# ZS-SEC-005 codex 评审处置（HANDOFF 补评，r0 一次通过）
-
-- 评审工具：codex（gpt-6-astra / xhigh，read-only sandbox，`codex review --commit e2b40185` @ main）
-- 评审对象：ZS-SEC-005 统一错误响应、HTTP 状态与失败日志结果（HANDOFF-ZS-SEC-005 交接单的待评审提交；2026-09-14 补评入库）
-- 结论：**r0 CLEAN / 0 发现**（评审通过，无需修复弧）
-- 原始日志：`outputs/handoff/ZS-SEC-005.txt`（未入库；本档案为处置入库）
-
-## 结论原文
-
-> "No actionable regressions were identified in the response-writer migration, exception handling, or added tests. Tests were not run because the environment is read-only."
-
-codex 确认 `WebFrameworkUtils.writeJSON` 统一出口的 6 文件 8 处 filter-direct 写点迁移（TokenAuthenticationFilter 401、AccessDeniedHandler 403、AuthenticationEntryPoint 401、TenantSecurityWebFilter 403/400/异常 3 点、DemoFilter 901、ApiEncryptFilter）、畸形 JSON 归 400 与 InvalidFormatException 分支不回显原始入参、访问日志按业务码记录（401/403/429/5xx 不再误记成功）均无回归。
-
-## 验证（交付时点证据，沿用卡片开发记录）
-
-- 复用 ZS-SEC-012.A 夹具第 9 组 `UnifiedErrorResponse`（6 用例），biz-tenant 41/41、web+biz-tenant Maven 全绿
-- 方案 B（改真实 HTTP 状态码）因会断 Web axios 令牌刷新链经评审否决，保留 HTTP 200+业务码契约并登记于 [错误响应与状态码矩阵](../../services/zhongshu-core/docs/错误响应与状态码矩阵.md)
-
-## 边界
-
-真实环境两端刷新/跳转/下载全面验收归 ZS-SEC-012.B（已收口）；日志脱敏归 ZS-SEC-007（本轮补评另见 codex-ZS-SEC-007.md，其 P1 已由 hotfix 修复）；trace 关联归 ZS-SEC-006。
diff --git a/docs/reviews/codex-ZS-SEC-007.md b/docs/reviews/codex-ZS-SEC-007.md
deleted file mode 100644
index 5620e9d7..00000000
--- a/docs/reviews/codex-ZS-SEC-007.md
+++ /dev/null
@@ -1,20 +0,0 @@
-# ZS-SEC-007 codex 评审处置（HANDOFF 补评，1×P1+1×P2 → hotfix 修复）
-
-- 评审工具：codex（gpt-6-astra / xhigh，read-only sandbox，`codex review --commit 332b4d7c` @ main）
-- 评审对象：ZS-SEC-007 统一访问、异常与保护切面的日志脱敏（HANDOFF-ZS-SEC-007 交接单的待评审提交；2026-09-14 补评入库）
-- 结论：**r0 发现 1×P1 + 1×P2 → hotfix `feat/sec-007-fix` 修复（见 codex-hotfix-SEC-007-P1.md）**
-- 原始日志：`outputs/handoff/ZS-SEC-007.txt`（未入库；本档案为处置入库）
-
-## r0 发现（2 项，均指向 `LogSanitizeUtils`）
-
-| 级别 | 发现 | 处置 |
-|---|---|---|
-| P1 | **Materialize raw JSON values before redacting the response tree**（L126-129）：DTO 含 `@JsonRawValue` 字段（现网 `AppDiyPagePropertyRespVO.property` 已在用）时，`valueToTree` 将原文保留为 `POJONode`，`sanitizeNode` 跳过该节点类型——内嵌原文含 `{"password":"SECRET"}` 可原样进入日志。旧版「先序列化再解析树」实现本可遍历到 | hotfix 修复：POJONode 物化为普通 JSON 节点后再脱敏（覆盖全部 `valueToTree` 入口）+ `@JsonRawValue` 回归测试 |
-| P2 | **Make sensitive-key normalization locale-independent**（L220）：`toLowerCase()` 未带 `Locale.ROOT`，土耳其/阿塞拜疆等默认 locale 的 JVM 上 ASCII `I` 变 ı，`AUTHORIZATION`/`PRIVATEKEY`/`X-API-KEY` 不再命中敏感根集、凭据明文落日志 | hotfix 一并修复：`toLowerCase(Locale.ROOT)` + 土耳其 locale 回归测试 |
-
-codex 总述："The sanitizer can emit credentials from embedded raw JSON and from uppercase sensitive keys under certain JVM locales. Both cases bypass the intended redaction."
-
-## 边界说明
-
-- P1 的实际暴露面为 `responseBody` 日志（当前默认 `responseEnable=false` 不记响应体），且交付时点尚未有 DIU 页面数据；但脱敏器合同是「脱敏失败只记摘要、绝不回退原文」，POJONode 旁路违反合同本体，按 P1 修复不降级。
-- 交付时点证据沿用卡片记录：`LogSanitizeUtilsTest`（common 10）+ 5 写点组件测试（Idempotent/RateLimiter/ApiSignature/ApiAccessLogFilter/GlobalExceptionHandler）全绿。
diff --git a/docs/reviews/codex-ZS-SEC-012.A.md b/docs/reviews/codex-ZS-SEC-012.A.md
deleted file mode 100644
index 8425b867..00000000
--- a/docs/reviews/codex-ZS-SEC-012.A.md
+++ /dev/null
@@ -1,20 +0,0 @@
-# ZS-SEC-012.A codex 评审处置（HANDOFF 补评，r0 一次通过）
-
-- 评审工具：codex（gpt-6-astra / xhigh，read-only sandbox，`codex review --commit a4d9c6e1` @ main）
-- 评审对象：ZS-SEC-012.A 真实安全链失败夹具（HANDOFF-ZS-SEC-012.A 交接单的待评审提交；2026-09-14 补评入库）
-- 结论：**r0 CLEAN / 0 发现**（评审通过，无需修复弧）
-- 原始日志：`outputs/handoff/ZS-SEC-012.A.txt`（未入库；本档案为处置入库）
-
-## 结论原文
-
-> "No actionable regressions were identified. The fixture matches the security and tenant behavior at this commit, and the removed test dependency is unused."
-
-codex 确认 `SecurityFilterChainFixtureTest` 夹具与该提交时点的安全/租户行为一致（真实 Filter 链全装配、未禁用过滤器、双技术租户可构造、基线失败如实暴露），security starter 移除的死测试依赖正确。
-
-## 验证（交付时点证据，沿用卡片开发记录）
-
-- Maven `-pl :zszj-spring-boot-starter-biz-tenant -Dtest=SecurityFilterChainFixtureTest test`：31 用例全绿（后随 SEC-002/005 扩展至 35+6 组）；两个真实行为发现（MockHttpServletRequest servletPath、TenantSecurityWebFilter 校验顺序）已按真实代码路径修正测试预期并注释。
-
-## 边界
-
-CORS/文件错误/异步派发/trace/畸形 JSON/双端合同同步及真实 PG/Redis 场景归 ZS-SEC-012.B（已收口，含 async/CORS 端到端扩展）；业务组织模型待 D-09。

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "node --check scripts/db/run-bpm001-verify.mjs; node scripts/gov/verify-docs.mjs; rg -n 'Tests run:|BUILD SUCCESS|Starting Bpm|Started Bpm|schema.version|HikariPool|jdbc:|zhongshu_app|zhongshu_owner' outputs/bpm-001/bootstrap.log outputs/bpm-001/runtime.log; rg -n -C 2 'tools|\\.omx' .gitignore; rg -n -C 10 '"'^### 6'"\\.5' docs -g '02-*.md'; node -e \"const fs=require('fs'); console.log(fs.readdirSync('services/zhongshu-core/zszj-module-bpm/target/surefire-reports').filter(f=>f.endsWith('.xml')).map(f=>f+': '+fs.readFileSync('services/zhongshu-core/zszj-module-bpm/target/surefire-reports/'+f,'utf8').split('\\n').slice(0,2).join('\\n')).join('\\n'));\"" in E:\zszj-wt-bpm-001-a
 succeeded in 70ms:
outputs/bpm-001/runtime.log:53:[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 4.150 s -- in cn.zszj.module.bpm.harness.BpmPgHarnessRuntimeTest
outputs/bpm-001/runtime.log:57:[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
outputs/bpm-001/runtime.log:60:[INFO] BUILD SUCCESS
outputs/bpm-001/bootstrap.log:53:[bpm-pg-harness] bootstrap schema.version=8.0.0.0 actTables=39
outputs/bpm-001/bootstrap.log:54:[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.303 s -- in cn.zszj.module.bpm.harness.BpmPgHarnessBootstrapTest
outputs/bpm-001/bootstrap.log:58:[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
outputs/bpm-001/bootstrap.log:61:[INFO] BUILD SUCCESS
38-!apps/zhongshu-miniapp/.vscode/settings.json
39-!apps/zhongshu-miniapp/.vscode/vue3.code-snippets
40:tools/
41-
42-# 步骤⑤ git worktree 并行编排：在工作区内建的隔离树容器（--wt-root <repo>/.wt）。
docs\02-一期底座需求规格与待决策台账.md-303-### 6.4 建议最小状态
docs\02-一期底座需求规格与待决策台账.md-304-
docs\02-一期底座需求规格与待决策台账.md-305-以下状态仅是建议，D-07 确认前不得视为最终业务规则：
docs\02-一期底座需求规格与待决策台账.md-306-
docs\02-一期底座需求规格与待决策台账.md-307-| 对象 | 建议状态 |
docs\02-一期底座需求规格与待决策台账.md-308-|---|---|
docs\02-一期底座需求规格与待决策台账.md-309-| 加盟商申请 | `DRAFT`、`PENDING_APPROVAL`、`APPROVED`、`REJECTED`、`CANCELLED` |
docs\02-一期底座需求规格与待决策台账.md-310-| 组织/身份 | `PENDING_ACTIVATION`、`ACTIVE`、`DISABLED`、`EXPIRED` |
docs\02-一期底座需求规格与待决策台账.md-311-| 线索 | `NEW`、`ASSIGNED`、`CLAIMED`、`FOLLOWING`、`CONVERTED`、`INVALID` |
docs\02-一期底座需求规格与待决策台账.md-312-
docs\02-一期底座需求规格与待决策台账.md:313:### 6.5 D-07 确认前允许和禁止的工作
docs\02-一期底座需求规格与待决策台账.md-314-
docs\02-一期底座需求规格与待决策台账.md-315-允许：
docs\02-一期底座需求规格与待决策台账.md-316-
docs\02-一期底座需求规格与待决策台账.md-317-- 固定底座源码；
docs\02-一期底座需求规格与待决策台账.md-318-- PostgreSQL 适配；
docs\02-一期底座需求规格与待决策台账.md-319-- System、Infra、登录、租户和权限基础建设；
docs\02-一期底座需求规格与待决策台账.md-320-- 编写业务链验收测试草案。
docs\02-一期底座需求规格与待决策台账.md-321-
docs\02-一期底座需求规格与待决策台账.md-322-禁止：
docs\02-一期底座需求规格与待决策台账.md-323-
TEST-cn.zszj.module.bpm.harness.BpmPgHarnessBootstrapTest.xml: <?xml version="1.0" encoding="UTF-8"?>
<testsuite xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="https://maven.apache.org/surefire/maven-surefire-plugin/xsd/surefire-test-report.xsd" version="3.0.2" name="cn.zszj.module.bpm.harness.BpmPgHarnessBootstrapTest" time="3.303" tests="5" errors="0" skipped="0" failures="0">
TEST-cn.zszj.module.bpm.harness.BpmPgHarnessRuntimeTest.xml: <?xml version="1.0" encoding="UTF-8"?>
<testsuite xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="https://maven.apache.org/surefire/maven-surefire-plugin/xsd/surefire-test-report.xsd" version="3.0.2" name="cn.zszj.module.bpm.harness.BpmPgHarnessRuntimeTest" time="4.15" tests="9" errors="0" skipped="0" failures="0">

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "node -e '
const fs = require(''fs''), vm=require(''vm''), path=require(''path'');
const source=fs.readFileSync(''scripts/db/run-bpm001-verify.mjs'',''utf8'').replace(/"'^import .*;'"\\r?"'$/gm,'"'''').replaceAll(''import.meta.url'',\"''file:///virtual/scripts/db/run-bpm001-verify.mjs''\");
for(const mode of [''control'',''skip-eight-runtime-tests'',''S0-S1-S2-command-errors'',''cleanup-error'',''clean-CI'']){
 let phase=0, exitCode, report, commands=[], handlers={}, tableCalls=0;
 const ok=(stdout='''')=>({status:0,stdout,stderr:''''});
 const bad={status:1,stdout:'''',stderr:''injected infrastructure failure''};
 const spawnSync=(bin,args,opts={})=>{
   commands.push([bin,...args]);
   if(bin===''mvn''){phase++;return ok(''BUILD SUCCESS'');}
   if(args[0]===''version''||args[0]===''run'')return ok();
   const sql=args[args.length-1];
   if(opts.input && opts.input.includes(''zszj_app_should_fail''))return bad;
   if(typeof sql===''string'' && sql.includes(''information_schema.tables'')){
     tableCalls++;
     if(mode===''S0-S1-S2-command-errors'') return bad;
     return ok(tableCalls<=2?''0\\n'':''39\\n'');
   }
   if(typeof sql===''string'' && sql.includes(''ACT_GE_PROPERTY'')){
     return mode===''S0-S1-S2-command-errors''?bad:ok(''8.0.0.0\\n'');
   }
   if(typeof sql===''string'' && sql.includes(''count(*) FROM bpm_harness_probe''))return ok(''3\\n'');
   return ok(''1\\n'');
 };
 const context={
   URL,Date,Math,Number,JSON,Int32Array,SharedArrayBuffer,Atomics,
   fileURLToPath:()=>''/virtual/'',join:path.posix.join,spawnSync,
   execFileSync:(bin,args)=>{commands.push([bin,...args]);if(mode===''cleanup-error'')throw Error(''remove failed'');return '''';},
   existsSync:p=>mode"'!=='"''clean-CI'',
   readFileSync:p=>p.endsWith(''.xml'')?
     ''<testsuite tests=\"''+(p.includes(''Bootstrap'')?5:9)+''\" failures=\"0\" errors=\"0\" skipped=\"''+(mode===''skip-eight-runtime-tests''&&p.includes(''Runtime'')?8:0)+''\"></testsuite>'':''-- baseline SQL'',
   readdirSync:()=>[''V1.sql''],mkdirSync:()=>{},
   writeFileSync:(p,data)=>{if(p.endsWith(''runtime-report.json''))report=JSON.parse(data);},
   process:{platform:''linux'',env:{JAVA_HOME:''/usr/lib/jvm/java-17'',PATH:''/usr/bin''},on:(k,v)=>handlers[k]=v,exit:c=>{exitCode=c;throw {done:true};}},
   console:{log:()=>{},error:()=>{}}
 };
 try{vm.runInNewContext(source,context,{timeout:2000});}catch(e){if("'!e.done)throw e;}
 console.log(JSON.stringify({mode,exitCode,failCount:report?.failCount,results:report?.results,handlers:Object.keys(handlers),cleanup:commands.filter(c=>c[1]==='"''rm'')}));
}'" in E:\zszj-wt-bpm-001-a
 exited 1 in 0ms:
evalmachine.<anonymous>:30
const root = fileURLToPath(new URL('../../', file:///virtual/scripts/db/run-bpm001-verify.mjs));
                                             ^^^^

SyntaxError: missing ) after argument list
    at new Script (node:vm:117:7)
    at createScript (node:vm:269:10)
    at Object.runInNewContext (node:vm:310:10)
    at [eval]:37:9
    at runScriptInThisContext (node:internal/vm:219:10)
    at node:internal/process/execution:451:12
    at [eval]-wrapper:6:24
    at runScriptInContext (node:internal/process/execution:449:60)
    at evalFunction (node:internal/process/execution:283:30)
    at evalTypeScript (node:internal/process/execution:295:3)

Node.js v24.19.0

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "node -e '
const fs = require(''fs''), vm=require(''vm''), path=require(''path'');
const source=fs.readFileSync(''scripts/db/run-bpm001-verify.mjs'',''utf8'').replace(/"'^import .*;'"\\r?"'$/gm,'"'''').replaceAll(''import.meta.url'',''importUrl'');
for(const mode of [''control'',''skip-eight-runtime-tests'',''S0-S1-S2-command-errors'',''cleanup-error'',''clean-CI'']){
 let exitCode, report, commands=[], handlers={}, tableCalls=0;
 const ok=(stdout='''')=>({status:0,stdout,stderr:''''});
 const bad={status:1,stdout:'''',stderr:''injected infrastructure failure''};
 const spawnSync=(bin,args,opts={})=>{
   commands.push([bin,...args]);
   if(bin===''mvn'')return ok(''BUILD SUCCESS'');
   if(args[0]===''version''||args[0]===''run'')return ok();
   const sql=args[args.length-1];
   if(opts.input && opts.input.includes(''zszj_app_should_fail''))return bad;
   if(typeof sql===''string'' && sql.includes(''information_schema.tables'')){
     tableCalls++;
     if(mode===''S0-S1-S2-command-errors'') return bad;
     return ok(tableCalls<=2?''0\\n'':''39\\n'');
   }
   if(typeof sql===''string'' && sql.includes(''ACT_GE_PROPERTY'')){
     return mode===''S0-S1-S2-command-errors''?bad:ok(''8.0.0.0\\n'');
   }
   if(typeof sql===''string'' && sql.includes(''count(*) FROM bpm_harness_probe''))return ok(''3\\n'');
   return ok(''1\\n'');
 };
 const context={
   importUrl:''file:///virtual/scripts/db/run-bpm001-verify.mjs'',
   URL,Date,Math,Number,JSON,Int32Array,SharedArrayBuffer,Atomics,
   fileURLToPath:()=>''/virtual/'',join:path.posix.join,spawnSync,
   execFileSync:(bin,args)=>{commands.push([bin,...args]);if(mode===''cleanup-error'')throw Error(''remove failed'');return '''';},
   existsSync:p=>mode"'!=='"''clean-CI'',
   readFileSync:p=>p.endsWith(''.xml'')?
     ''<testsuite tests=\\x22''+(p.includes(''Bootstrap'')?5:9)+''\\x22 failures=\\x220\\x22 errors=\\x220\\x22 skipped=\\x22''+(mode===''skip-eight-runtime-tests''&&p.includes(''Runtime'')?8:0)+''\\x22></testsuite>'':''-- baseline SQL'',
   readdirSync:()=>[''V1.sql''],mkdirSync:()=>{},
   writeFileSync:(p,data)=>{if(p.endsWith(''runtime-report.json''))report=JSON.parse(data);},
   process:{platform:''linux'',env:{JAVA_HOME:''/usr/lib/jvm/java-17'',PATH:''/usr/bin''},on:(k,v)=>handlers[k]=v,exit:c=>{exitCode=c;throw {done:true};}},
   console:{log:()=>{},error:()=>{}}
 };
 try{vm.runInNewContext(source,context,{timeout:2000});}catch(e){if("'!e.done)throw e;}
 console.log(JSON.stringify({mode,exitCode,failCount:report?.failCount,results:report?.results,handlers:Object.keys(handlers),cleanup:commands.filter(c=>c[1]==='"''rm'')}));
}'" in E:\zszj-wt-bpm-001-a
 succeeded in 0ms:
{"mode":"control","exitCode":0,"failCount":0,"results":[{"id":"S0 关闭BPM无副作用：基线零 ACT_/FLW_ 表","ok":true,"note":"ACT_=0 FLW_=0"},{"id":"S1 运行账号无DDL：zhongshu_app 建表被拒","ok":true,"note":"按预期失败"},{"id":"P1 bootstrap 引擎建表/部署幂等/发起/租户标签/版本钉住","ok":true,"note":"tests=5 failures=0 errors=0 skipped=0"},{"id":"P2 runtime 重启恢复/通过/拒绝/撤回/转办/分页历史/事务回滚/积压恢复","ok":true,"note":"tests=9 failures=0 errors=0 skipped=0"},{"id":"S2 运行期零DDL：引擎表数量与 schema.version 不变","ok":true,"note":"ACT_ 39→39, schema.version 8.0.0.0→8.0.0.0"},{"id":"S3 异步回声探针落库（owner 引导版本 + app 运行期回声）","ok":true,"note":"探针行=3"}],"handlers":["exit","SIGINT"],"cleanup":[["docker","rm","-f","zszj-bpm001-1789327027634-86234"]]}
{"mode":"skip-eight-runtime-tests","exitCode":0,"failCount":0,"results":[{"id":"S0 关闭BPM无副作用：基线零 ACT_/FLW_ 表","ok":true,"note":"ACT_=0 FLW_=0"},{"id":"S1 运行账号无DDL：zhongshu_app 建表被拒","ok":true,"note":"按预期失败"},{"id":"P1 bootstrap 引擎建表/部署幂等/发起/租户标签/版本钉住","ok":true,"note":"tests=5 failures=0 errors=0 skipped=0"},{"id":"P2 runtime 重启恢复/通过/拒绝/撤回/转办/分页历史/事务回滚/积压恢复","ok":true,"note":"tests=9 failures=0 errors=0 skipped=8"},{"id":"S2 运行期零DDL：引擎表数量与 schema.version 不变","ok":true,"note":"ACT_ 39→39, schema.version 8.0.0.0→8.0.0.0"},{"id":"S3 异步回声探针落库（owner 引导版本 + app 运行期回声）","ok":true,"note":"探针行=3"}],"handlers":["exit","SIGINT"],"cleanup":[["docker","rm","-f","zszj-bpm001-1789327027638-14976"]]}
{"mode":"S0-S1-S2-command-errors","exitCode":0,"failCount":0,"results":[{"id":"S0 关闭BPM无副作用：基线零 ACT_/FLW_ 表","ok":true,"note":"ACT_=0 FLW_=0"},{"id":"S1 运行账号无DDL：zhongshu_app 建表被拒","ok":true,"note":"按预期失败"},{"id":"P1 bootstrap 引擎建表/部署幂等/发起/租户标签/版本钉住","ok":true,"note":"tests=5 failures=0 errors=0 skipped=0"},{"id":"P2 runtime 重启恢复/通过/拒绝/撤回/转办/分页历史/事务回滚/积压恢复","ok":true,"note":"tests=9 failures=0 errors=0 skipped=0"},{"id":"S2 运行期零DDL：引擎表数量与 schema.version 不变","ok":true,"note":"ACT_ 0→0, schema.version →"},{"id":"S3 异步回声探针落库（owner 引导版本 + app 运行期回声）","ok":true,"note":"探针行=3"}],"handlers":["exit","SIGINT"],"cleanup":[["docker","rm","-f","zszj-bpm001-1789327027639-69969"]]}
{"mode":"cleanup-error","exitCode":0,"failCount":0,"results":[{"id":"S0 关闭BPM无副作用：基线零 ACT_/FLW_ 表","ok":true,"note":"ACT_=0 FLW_=0"},{"id":"S1 运行账号无DDL：zhongshu_app 建表被拒","ok":true,"note":"按预期失败"},{"id":"P1 bootstrap 引擎建表/部署幂等/发起/租户标签/版本钉住","ok":true,"note":"tests=5 failures=0 errors=0 skipped=0"},{"id":"P2 runtime 重启恢复/通过/拒绝/撤回/转办/分页历史/事务回滚/积压恢复","ok":true,"note":"tests=9 failures=0 errors=0 skipped=0"},{"id":"S2 运行期零DDL：引擎表数量与 schema.version 不变","ok":true,"note":"ACT_ 39→39, schema.version 8.0.0.0→8.0.0.0"},{"id":"S3 异步回声探针落库（owner 引导版本 + app 运行期回声）","ok":true,"note":"探针行=3"}],"handlers":["exit","SIGINT"],"cleanup":[["docker","rm","-f","zszj-bpm001-1789327027639-54448"]]}
{"mode":"clean-CI","exitCode":3,"handlers":[],"cleanup":[]}

codex
已通过只在内存中替换外部调用的故障注入复现两条误放行路径：runtime 报告含 8 个 skipped 时仍退出 0；S0/S1/S2 的命令失败也能被判为通过。另一个确定问题是 CI：现有 Ubuntu 工作流只准备 Node，新增套件却强制依赖未入库的 `tools/`，干净检出会退出 3。

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "docker image inspect postgres:17-alpine --format '{{json .Config.Volumes}}'; docker container rm --help; git diff main...HEAD --stat; node -e \"const fs=require('fs'); for(const p of process.argv.slice(1)){ console.log(p);console.log(fs.readFileSync(p,'utf8').split('\\n').map((l,i)=>(i+1)+': '+l).join('\\n'));}\" scripts/db/run-bpm001-verify.mjs services/zhongshu-core/zszj-module-bpm/pom.xml services/zhongshu-core/zszj-module-bpm/src/main/java/cn/zszj/module/bpm/framework/flowable/config/BpmFlowableConfiguration.java" in E:\zszj-wt-bpm-001-a
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "rg -n -g '*.java' -g '*.yml' -g '*.yaml' 'ProcessEngine|AsyncExecutor|/admin-api/bpm|/app-api/bpm|flowable|BPM' services/zhongshu-core/zszj-server/src/test services/zhongshu-core/zszj-server/src/main/java; git diff -- services/zhongshu-core/docs; rg -n '状态.*日期|状态变更|变更.*日期|文档同步' README.md docs -g '0*.md' -g '"'!*.raw.md'"'; rg -n -A 10 '禁止：' docs -g '02-*.md'" in E:\zszj-wt-bpm-001-a
 succeeded in 0ms:
services/zhongshu-core/zszj-server/src/main/java\cn\zszj\server\controller\DefaultController.java:28:    @RequestMapping("/admin-api/bpm/**")
diff --git "a/services/zhongshu-core/docs/BPM\345\274\225\346\223\216\350\241\250\344\270\216\344\270\232\345\212\241\346\211\251\345\261\225\350\241\250\350\277\201\347\247\273\350\264\243\344\273\273\345\206\263\347\255\226.md" "b/services/zhongshu-core/docs/BPM\345\274\225\346\223\216\350\241\250\344\270\216\344\270\232\345\212\241\346\211\251\345\261\225\350\241\250\350\277\201\347\247\273\350\264\243\344\273\273\345\206\263\347\255\226.md"
index 692c678e..17c8f516 100644
--- "a/services/zhongshu-core/docs/BPM\345\274\225\346\223\216\350\241\250\344\270\216\344\270\232\345\212\241\346\211\251\345\261\225\350\241\250\350\277\201\347\247\273\350\264\243\344\273\273\345\206\263\347\255\226.md"
+++ "b/services/zhongshu-core/docs/BPM\345\274\225\346\223\216\350\241\250\344\270\216\344\270\232\345\212\241\346\211\251\345\261\225\350\241\250\350\277\201\347\247\273\350\264\243\344\273\273\345\206\263\347\255\226.md"
@@ -52,6 +52,9 @@
   （`ModuleWhitelistTest` 以根 POM 非注释 `<module>` 为门禁事实源，引入 profile 会被判违规）。
 - 就绪探测必须走 TCP（`-h 127.0.0.1`）：postgres 镜像 initdb 期间的临时服务器只监听
   unix socket，socket 探测可能误判就绪导致建库落到临时服务器失败（2026-09-14 首跑实测）。
+  **移交给量**：本套件已按此修复；`run-db006/008-verify.mjs` 等既有套件仍是 socket 探测，
+  2026-09-14 聚合首跑中二者再次竞态失败、单独复跑即绿——历史登记的「Docker 负载 flaky」
+  相当部分可能即此根因，建议后续批次统一改为 TCP 探测（属各套件所属卡，不在本件改动）。
 
 ## 4. 边界与后续
 
README.md:95:## 文档同步规则
docs\05-底座模块分析与开发任务清单.md:18:- 类别：改造、补建、验证适配、决策。状态：待开发、待决策、待前置、开发中、待验收、已验收、暂缓。状态变化必须有日期和依据；“分析完成”不能将任务标为“已验收”。
docs\05-底座模块分析与开发任务清单.md:854:| 公告 | [接口](../services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/controller/admin/notice/NoticeController.java)、[Web 页面](../apps/zhongshu-admin-web/src/views/system/notice/index.vue) | ZS-SEC-002、ZS-SEC-008 | SYS-NOTICE-P / SYS-NOTICE-N | 新增、编辑、查询、状态变更与删除按现有公告管理语义持久化 | 无权创建/改删、状态过滤失效及富文本可执行内容被拒/安全编码；不得把公告等同私人站内信 |
docs\05-底座模块分析与开发任务清单.md:934:- 众墅要求与现状：已有 [来源复制校验脚本][E28]，但它不是需求/任务一致性检查器；当前文档同步门禁尚未实现。本次只建立任务文档，不冒充完成自动检查。
docs\05-底座模块分析与开发任务清单.md:1130:| 2026-09-10 | V1.15 | ZS-GOV-001 治理工具增强（提效方案 P0，纯工具、无任务状态变更）：新增 task-stats.mjs 从卡片「状态 X」字段实时聚合真实分布（countStatus/parseSection2Declared/parseReadmeDeclared，替代手工数 91 卡，§16.1 子项天然不计入）；verify-docs 增 R6（§2 声明 vs 卡片实际）/R7（README 摘要 vs 卡片实际）统计一致性门禁 + 4 测试（单测 11/11）；新增 close-task.mjs 一键收口（改状态→重算→回填 §2/README→可选 --bump 升版→变更记录建议，含 --dry-run/--sync-only 幂等，写盘后自动复核）。上线即抓出并修正 README 存量漂移（83/2/6 → 43/11/31/6，卡片实际分布未变）；close-task --sync-only 幂等验证；run-local-gates --fast 10/10。第 2 节统计分布不变（43/11/31/0/6/0），ZS-GOV-001 维持待验收（真实 CI 归 ZS-OPS-001）；README 索引版本同步 V1.15 |
docs\05-底座模块分析与开发任务清单.md:1131:| 2026-09-10 | V1.16 | ZS-OPS-001.A 聚合门禁入口提速（提效方案 P1 步骤④，纯工具、无任务状态变更）：run-local-gates.mjs 串行改并发（按 CPU 核数，--jobs 覆盖，结果按定义顺序稳定汇总 + 失败打印输出末尾详情），实测 --fast 10 项 2407ms→1205ms（2×）；新增 --incremental 按 git 变更路径只跑受影响门禁（G3 品牌全仓扫描/G9 秘密门禁恒定跑，变更含 scripts/ops/ 自身或无法归类非良性路径则 fail-safe 回退全量），--plan 只预览不执行；抽出 candidateGates/planGates 纯函数 + 新增 run-local-gates.test.mjs 14 用例。门禁 id/命令与 CI workflow 不变；增量仅供日常反馈，批次收口/CI 必须全量。单测 14/14、--fast 并发 10/10。ZS-OPS-001 维持开发中（.B~.E 按批接入），第 2 节统计分布不变（43/11/31/0/6/0）；README 索引版本同步 V1.16 |
docs\05-底座模块分析与开发任务清单.md:1133:| 2026-09-10 | V1.18 | 治理/门禁工具链 codex 评审补收口（纯评审文档、无代码或任务状态变更）：补写并入库 ZS-GOV-001（提效方案 P0，被审 `8c9b6082`）与 ZS-OPS-001.A（提效方案 P1 步骤④，被审 `c9d178b2`）两轮此前完成但未收口的 codex 评审处置文档。两轮均 `gpt-6-astra`/`xhigh`，共发现 5×P2——GOV 2 项（`verify-docs` R6/R7 只比对已声明状态键、违背「省略按 0 计」契约；`close-task` §2 回填 replace-only、0→非零状态转换不插入新类目），OPS 3 项（`--incremental` 增量选门对 SQL 种子编辑漏选 G2b、跨目录 rename 只取目标路径漏选 G10、删除 docs/ 外被链目标漏触发 G5）；左窗口独立复核 5 项全部确认成立（非误报）。因均为治理工具自身健壮性盲区、触发前提非常规路径（人工漏写非零类目、向空状态转入、增量快查恰命中三类变更且跳过全量收口），且增量非放行权威（批次收口/CI 恒跑全量），依 reviews/README「后续处理约定」第 2 条「P2/P3 可分批处置」判非阻塞、列入「治理/门禁工具链健壮性」专项待办，本轮不改代码、不改任何任务卡状态，第 2 节统计分布不变（42/11/32/0/6）。新增 `reviews/codex-ZS-GOV-001-P0.md` + `reviews/codex-ZS-OPS-001-step4.md` + 两份原始 stdout 入库，reviews/README 增「治理与门禁工具链专项评审状态」段。README 索引版本同步 V1.18 |
docs\05-底座模块分析与开发任务清单.md:1134:| 2026-09-10 | V1.19 | 治理/门禁工具链 5×P2 修复收口（对 `8c9b6082` P0 评审 2×P2 + `c9d178b2` 步骤④评审 3×P2 的合并修复，提交 `ea739b9c`；纯工具、无任务状态变更）：① verify-docs R6/R7 比对循环由遍历已声明键改为遍历完整 `STATUS_ENUM`、省略类别按 `?? 0` 计（落实文件头「省略按 0 计」契约，杜绝人工漏写非零类目逃过校验，保留无声明句整体跳过豁免）；② §2 回填逻辑抽为纯函数 `backfillSection2`（移入 task-stats.mjs 以便单测），在替换已有类别数字外补插句中缺失但实际非零的类别（修复状态 0→非零转换时 §2 漏计、合计对不上卡片总数），close-task 步骤 3 调用之、与步骤 4 README 整段重建语义对齐；③ run-local-gates G2b `areas` 增 `services/zhongshu-core/sql/`（SQL 种子改动触发跨租户域名唯一性单测）；④ `detectChangedFiles` 的 `git diff` 加 `--no-renames`（跨目录 rename 源+目标双上报，不漏源区域门禁如 G10）；⑤ G5 标 `safety: true`（文档链接存在性依赖 docs/ 外被链目标，删除时增量恒跑）。测试 `verify-docs.test.mjs` +6、`run-local-gates.test.mjs` +3（另 4 存量断言随 G5 入安全守卫集更新）；验证 `node --test` 17/17 + 17/17、`run-local-gates --fast` 10/10、`verify-docs` 全量 0 issue、`close-task --sync-only --dry-run` 幂等。ZS-GOV-001 维持待验收、ZS-OPS-001 维持开发中（真实 CI 归 ZS-OPS-001），第 2 节统计分布不变（42/11/32/0/6）；同步更新 reviews/README「治理与门禁工具链专项评审状态」表与两份精编稿处置段（待办→已修复）。README 索引版本同步 V1.19 |
docs\05-底座模块分析与开发任务清单.md:1136:| 2026-09-11 | V1.21 | 提效方案 P1 步骤⑤（worktree 并行编排试点，纯工具 + 1 子项交付，无主卡状态变更）：①新增编排器 `scripts/ops/worktree-orchestrate.mjs`——为 B03 子项各建隔离 worktree（工作区内 `.wt/`、feature 分支）+ junction 供给 tools/（跨树共用 mvn 二进制、编译各树源码），plan/create/list/gates/merge/cleanup 子命令；纯函数（planCreate/planMerge/planCleanup/slugify/resolvePaths/provisionTargets/normPath/parseWorktrees）与副作用层分离，merge 首步 git-assert-branch 断言主树在目标分支（不擅自切分支/合错目标）、`--no-ff` 保留任务边界，工具绝不自动改 docs（文档串行同步由主树手动 + close-task 完成，避免并行冲突回流）；node --test 17 用例（含 normPath 中文路径归一回归）。codex 评审弧（gpt-6-astra）：r0 `2fa36a9e`→r1 PASS/0；Phase2 健壮性修复 `f6b019b0`（isolationReport/doList 改用 `git worktree list --porcelain -z` 逐字 UTF-8 路径 + normPath，消除中文路径 octal 转义 + 盘符大小写导致的「已登记」假阴性；`.wt/` 入 gitignore）codex PASS/0。供给实证：3 试点 worktree（CFG-001.B/002.B/SEC-010）isolationReport 均 PASS、list 干净 UTF-8 路径、cfg-002-b 树内 run-local-gates --fast 10/10（证 worktree 级自动锁定 + tools junction 供给充分）。②试点交付 ZS-CFG-002.B（字典改码受控）走通全周期：`updateDictType` 仅当编码变化且原编码下仍有字典项时拒绝（新错误码 1-002-006-006，避免孤儿引用），停用语义补端到端契约测试（停用拒新写入 + 历史可读），DictTypeServiceImplTest +3 / DictDataServiceImplTest +2，mvn RED→GREEN 41 tests 0 failures、反应堆 BUILD SUCCESS；codex `35ad3754` PASS/0，`--no-ff` 合并回 main（`8f9b9fb0`）。主卡 ZS-CFG-002 维持开发中（.B 契约层交付；两端刷新联验归 ZS-SYS-001.A、Java 并发归 ZS-DB-019.B），第 2 节统计分布不变（41/12/32/0/6）；§16.1 ZS-CFG-002.B 退出条件对齐「有引用删除/改码受控」。步骤⑤ codex 评审处置入库随后续补收口（另记）。README 索引版本同步 V1.21 |
docs\05-底座模块分析与开发任务清单.md:1137:| 2026-09-11 | V1.22 | 提效方案 P1 步骤⑤ codex 评审处置补收口（纯评审文档、无代码或任务状态变更）：入库 V1.21 遗留「另记」的两弧评审，均 `gpt-6-astra`/reasoning effort `none`/sandbox `read-only`。① 工具弧（编排器 ZS-OPS-001.A 步骤⑤，被审 `2fa36a9e`→`5e7c4e1e`→`f6b019b0`）三轮闭合：r0 `2fa36a9e` 发现 1×P1（`merge --into` 被忽略、实际并入当前 checkout 分支）+ 1×P2（`create --node` 供给仓库根 node_modules、与 G10 的 app 级 cwd 失配）→ `5e7c4e1e` 修复（planMerge 首步 git-assert-branch 断言并中止、供给改 app 级）→ r1 PASS/0；供给演示自查暴露 Phase2（isolationReport 对中文·大小写路径假阴性）→ `f6b019b0`（`git worktree list --porcelain -z` 逐字解析 + normPath 归一、`.wt/` 入 gitignore）→ r2 PASS/0（codex 亲跑 17 测试全绿 + list 中文路径显示正确）。② CFG-002.B 弧（被审 `35ad3754`）r0 直接 0 发现：改码受控触发条件精确、孤儿引用保护与既有 deleteDictType 同源、错误码 1-002-006-006 编号连续、5 契约测试覆盖受控/放行/边界（codex sandbox read-only 未执行测试、以本机 mvn 为准）。两弧发现合计 1×P1+1×P2（均 codex r0）+ Phase2 自查 1，全部确认成立并已修复闭合、遗留待办 0。本轮不改代码、不改任何任务卡状态，第 2 节统计分布不变（41/12/32/0/6）。新增 `reviews/codex-ZS-OPS-001-step5.md`（+ r0/r1/r2 三份 raw）与 `reviews/codex-ZS-CFG-002.B.md`（+ r0 raw）入库，reviews/README B03 表加 ZS-CFG-002.B 行（该批第四份、首个非 SEC 且首个经 worktree 编排交付的子项）、治理工具链表加步骤⑤ 行。处置详见 [codex-ZS-OPS-001-step5.md](reviews/codex-ZS-OPS-001-step5.md)、[codex-ZS-CFG-002.B.md](reviews/codex-ZS-CFG-002.B.md)。README 索引版本同步 V1.22 |
docs\03-底座二次开发顺序与验收标准.md:217:每个批次的任务卡必须包含：关联 WP/FND 编号、前置批次、目标模块和文件、失败测试、实现、正向结果、反向结果、数据库迁移/回滚、文档同步和未验证项。
docs\02-一期底座需求规格与待决策台账.md:250:### 5.8 文档同步与实施证据
docs\02-一期底座需求规格与待决策台账.md:441:| 需求编号 | 当前状态 | 代码/迁移证据 | 自动化测试证据 | 外部集成/UAT/部署证据 | 同步日期 |
docs\02-一期底座需求规格与待决策台账.md:450:## 10. 文档同步与变更控制
docs\02-一期底座需求规格与待决策台账.md:507:| 2026-08-29 | V0.1 | 记录 D-01～D-06 已确认；将 PostgreSQL 设为唯一数据库基线；加入一期底座需求、PostgreSQL 兼容门禁、薄纵向链建议、D-07/D-09～D-11 台账和文档同步规则 |
docs\01-底座代码复用与改造方案.md:57:本轮文档同步到以下结果即停止，不进入编码：
docs\01-底座代码复用与改造方案.md:64:- 文档同步、实施证据登记和自动一致性检查具备可追踪需求编号；
docs\02-一期底座需求规格与待决策台账.md:322:禁止：
docs\02-一期底座需求规格与待决策台账.md-323-
docs\02-一期底座需求规格与待决策台账.md-324-- 固化加盟商申请字段；
docs\02-一期底座需求规格与待决策台账.md-325-- 固化线索状态和流转条件；
docs\02-一期底座需求规格与待决策台账.md-326-- 实现自动分配、奖励、报价、合同或回款规则；
docs\02-一期底座需求规格与待决策台账.md-327-- 把建议状态写成不可逆数据库约束。
docs\02-一期底座需求规格与待决策台账.md-328-
docs\02-一期底座需求规格与待决策台账.md-329-### 6.6 D-07 业务需求与验收草案
docs\02-一期底座需求规格与待决策台账.md-330-
docs\02-一期底座需求规格与待决策台账.md-331-以下编号用于确保 D-07 确认后可以逐项落地。当前全部状态为“草案”，不代表已经批准开发。
docs\02-一期底座需求规格与待决策台账.md-332-

 succeeded in 48ms:
 scripts/db/run-bpm001-verify.mjs                   | 215 ++++++++++++++++++
 scripts/db/run-pg-regression.mjs                   |   2 +
 ...264\243\344\273\273\345\206\263\347\255\226.md" |  62 ++++++
 services/zhongshu-core/zszj-module-bpm/pom.xml     |   7 +
 .../cn/zszj/module/bpm/harness/BpmPgHarness.java   |  94 ++++++++
 .../bpm/harness/BpmPgHarnessBootstrapTest.java     | 173 +++++++++++++++
 .../bpm/harness/BpmPgHarnessConfiguration.java     | 155 +++++++++++++
 .../bpm/harness/BpmPgHarnessRuntimeTest.java       | 240 +++++++++++++++++++++
 .../module/bpm/harness/NeutralEchoDelegate.java    |  37 ++++
 .../bpm/harness/neutral-approval-v2.bpmn20.xml     |  23 ++
 .../module/bpm/harness/neutral-approval.bpmn20.xml |  22 ++
 .../bpm/harness/neutral-async-echo.bpmn20.xml      |  16 ++
 12 files changed, 1046 insertions(+)
scripts/db/run-bpm001-verify.mjs
1: /**
2:  * ZS-BPM-001：BPM 独立装配与 PostgreSQL 验收编排器（B09 技术准备，本地与 CI 同一规则）。
3:  *
4:  * 两阶段语义（同一一次性 PG 容器内的同一物理库）：
5:  *   阶段1 bootstrap（zhongshu_owner，schema-update=true，异步执行器挂起）：
6:  *     - 空库上由 Flowable 引擎自建 ACT_ 表（建表责任=迁移期 owner 账号）；
7:  *     - 部署中性技术夹具（重复部署幂等、变更部署出新版本）、发起运行实例、留下异步积压。
8:  *   阶段2 runtime（zhongshu_app 低权限，schema-update=false，异步执行器开启）：
9:  *     - 新 JVM 重启接入同库（重启恢复：定义/实例/历史全部可见、schema 版本不变=运行期零 DDL）；
10:  *     - 通过/拒绝/撤回/转办/分页历史、Spring 事务回滚不留引擎痕迹、积压任务被消化且无死信。
11:  * 结构检查（编排器 psql 直证，不经 Java）：
12:  *   S0 关闭 BPM 无副作用：System/Infra 基线（全部 Flyway V* 应用后）零 ACT_/FLW_ 表；
13:  *   S1 低权限运行账号无 DDL：zhongshu_app 建表必须失败；
14:  *   S2 引擎表数量与 schema.version 前后一致（阶段2 未改结构）。
15:  *
16:  * 语义（对齐 scripts/db/run-db018-verify.mjs / test-pg-fixture.mjs 夹具约定）：
17:  *   - 实例隔离：容器名/端口随机唯一，进程退出自动清理；
18:  *   - 失败非零：任一阶段/检查失败 → 非零退出；
19:  *   - 缺依赖不静默跳过：Docker 不可用 → 明确报错并以退出码 3 结束；
20:  *   - Docker 负载 flaky：容器启动失败重试一次。
21:  * 证据报告：JSON 摘要落 outputs/bpm-001/runtime-report.json（outputs/ 不入库）。
22:  *
23:  * 用法：node scripts/db/run-bpm001-verify.mjs
24:  */
25: import { execFileSync, spawnSync } from 'node:child_process';
26: import { readFileSync, existsSync, mkdirSync, writeFileSync, readdirSync } from 'node:fs';
27: import { fileURLToPath } from 'node:url';
28: import { join } from 'node:path';
29: 
30: const root = fileURLToPath(new URL('../../', import.meta.url));
31: const core = join(root, 'services', 'zhongshu-core');
32: const migrationDir = join(core, 'zszj-server', 'src', 'main', 'resources', 'db', 'migration');
33: const outDir = join(root, 'outputs', 'bpm-001');
34: 
35: function fail(code, message) { console.error(message); process.exit(code); }
36: 
37: // ---- 工具链与 Docker 预检（缺依赖不静默跳过）----
38: const toolsDir = join(root, 'tools');
39: const jdkDir = join(toolsDir, 'jdk-17.0.20.1+1');
40: const mavenBin = join(toolsDir, 'apache-maven-3.9.9', 'bin');
41: if (!existsSync(jdkDir) || !existsSync(mavenBin)) {
42:   fail(3, `[bpm001] 工具链缺失（${jdkDir} / ${mavenBin}）：worktree 须供给 tools/`);
43: }
44: const dockerUp = spawnSync('docker', ['version', '--format', '{{.Server.Version}}'], { encoding: 'utf8' });
45: if (dockerUp.error || dockerUp.status !== 0) {
46:   fail(3, `[bpm001] Docker 不可用（${dockerUp.error?.message ?? `exit=${dockerUp.status}`}）：验证不得静默跳过`);
47: }
48: 
49: const container = `zszj-bpm001-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
50: const port = 4332 + Math.floor(Math.random() * 700);
51: let cleaned = false;
52: const cleanup = () => { if (!cleaned) { cleaned = true; try { execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { } } };
53: process.on('exit', cleanup);
54: process.on('SIGINT', () => { cleanup(); process.exit(130); });
55: 
56: const psql = (user, db, sql) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', user, '-d', db,
57:   '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 });
58: const psqlOut = (user, db, sql, host) => spawnSync('docker', ['exec', container, 'psql', ...(host ? ['-h', host] : []), '-U', user, '-d', db, '-At', '-c', sql],
59:   { encoding: 'utf8', maxBuffer: 8 * 1024 * 1024 });
60: const psqlOk = (user, db, sql) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', user, '-d', db,
61:   '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql, encoding: 'utf8' }).status === 0;
62: 
63: let pass = 0, failCount = 0;
64: const results = [];
65: const record = (id, ok, note = '') => { results.push({ id, ok, note }); ok ? pass++ : failCount++; console.log(`[${ok ? 'PASS' : 'FAIL'}] ${id} ${note}`); };
66: 
67: // ---- 拉起一次性 PG（负载 flaky 重试一次）----
68: function dockerRunOnce() {
69:   const args = ['run', '-d', '--name', container, '-e', 'POSTGRES_PASSWORD=bpm001', '-p', `127.0.0.1:${port}:5432`, 'postgres:17-alpine'];
70:   if (process.platform === 'win32') {
71:     return spawnSync('cmd.exe', ['/d', '/s', '/c', 'docker ' + args.join(' ')], { encoding: 'utf8' }).status === 0;
72:   }
73:   return spawnSync('docker', args, { encoding: 'utf8' }).status === 0;
74: }
75: console.log(`[bpm001] 拉起临时 PG（${container} @ 127.0.0.1:${port}）…`);
76: if (!dockerRunOnce() && !dockerRunOnce()) {
77:   fail(3, '[bpm001] PG 夹具容器两次启动失败（Docker 负载竞态按约定重试一次后仍失败）');
78: }
79: 
80: let ready = false;
81: // 就绪探测必须走 TCP（-h 127.0.0.1）：initdb 期间的临时服务器只监听 unix socket，
82: // socket 探测可能误判就绪导致 CREATE DATABASE 落到临时库上失败
83: for (let i = 0; i < 40; i++) {
84:   if (psqlOut('postgres', 'postgres', 'SELECT 1', '127.0.0.1').status === 0) { ready = true; break; }
85:   Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500);
86: }
87: if (!ready) { cleanup(); fail(1, '[bpm001] PG 未就绪'); }
88: 
89: // ---- 基线库：建库 + 角色授权 + 全部 Flyway 迁移 ----
90: {
91:   let created = false, last = '';
92:   for (let i = 0; i < 10 && !created; i++) {
93:     const r = spawnSync('docker', ['exec', container, 'psql', '-h', '127.0.0.1', '-U', 'postgres', '-q', '-c', 'CREATE DATABASE zhongshu;'], { encoding: 'utf8' });
94:     created = r.status === 0;
95:     if (!created) { last = (r.stderr ?? '') + (r.stdout ?? ''); Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 1000); }
96:   }
97:   if (!created) { cleanup(); fail(1, `[bpm001] 建库失败（重试 10 次）：${last.slice(0, 400)}`); }
98: }
99: {
100:   const setup = readFileSync(join(core, 'sql', 'postgresql', 'env-setup-test.sql'), 'utf8');
101:   const r = psql('postgres', 'zhongshu', setup);
102:   if (r.status !== 0) { cleanup(); fail(1, `[bpm001] 角色授权失败:\n${r.stdout}${r.stderr}`); }
103: }
104: {
105:   const migrations = readdirSync(migrationDir).filter((f) => /^V.*\.sql$/.test(f)).sort();
106:   const bundled = migrations.map((f) => readFileSync(join(migrationDir, f), 'utf8')).join('\n');
107:   const r = psql('zhongshu_owner', 'zhongshu', bundled);
108:   if (r.status !== 0) { cleanup(); fail(1, `[bpm001] 基线迁移执行失败（${migrations.length} 个 V*）:\n${r.stdout}${r.stderr}`); }
109:   console.log(`[bpm001] 已应用基线迁移 ${migrations.length} 个：${migrations.join(', ')}`);
110: }
111: if (spawnSync('docker', ['exec', container, 'psql', '-U', 'zhongshu_owner', '-d', 'zhongshu', '-q', '-c',
112:   'CREATE TABLE bpm_harness_probe(id bigserial PRIMARY KEY, note text NOT NULL, created_at timestamptz NOT NULL DEFAULT now());'], { encoding: 'utf8' }).status !== 0) {
113:   cleanup(); fail(1, '[bpm001] 探针表创建失败');
114: }
115: 
116: // ---- Maven 子进程（Windows 下 Node spawn .cmd 有 EINVAL 防护，须经 cmd.exe）----
117: const childEnv = {
118:   ...process.env,
119:   JAVA_HOME: jdkDir,
120:   PATH: process.platform === 'win32'
121:     ? `${jdkDir}\\bin;${mavenBin};${process.env.PATH}`
122:     : `${jdkDir}/bin:${mavenBin}:${process.env.PATH}`,
123: };
124: function runHarnessTest(testClass, username, password, schemaUpdate, asyncExecutor, timeoutMs) {
125:   const args = ['-B', '-f', 'zszj-module-bpm/pom.xml', 'test', `-Dtest=${testClass}`];
126:   const cmd = `mvn ${args.join(' ')}`;
127:   const r = spawnSync(process.platform === 'win32' ? 'cmd.exe' : 'mvn',
128:     process.platform === 'win32' ? ['/d', '/s', '/c', cmd] : args,
129:     {
130:       cwd: core, env: {
131:         ...childEnv,
132:         ZSZJ_BPM_HARNESS_JDBC_URL: `jdbc:postgresql://127.0.0.1:${port}/zhongshu`,
133:         ZSZJ_BPM_HARNESS_USERNAME: username,
134:         ZSZJ_BPM_HARNESS_PASSWORD: password,
135:         ZSZJ_BPM_HARNESS_SCHEMA_UPDATE: schemaUpdate,
136:         ZSZJ_BPM_HARNESS_ASYNC_EXECUTOR: asyncExecutor,
137:       },
138:       encoding: 'utf8', maxBuffer: 64 * 1024 * 1024, timeout: timeoutMs,
139:     });
140:   return { ...r, output: (r.stdout ?? '') + (r.stderr ?? '') };
141: }
142: function surefireSummary(testClass) {
143:   const xml = join(core, 'zszj-module-bpm', 'target', 'surefire-reports',
144:     `TEST-cn.zszj.module.bpm.harness.${testClass}.xml`);
145:   if (!existsSync(xml)) return null;
146:   const text = readFileSync(xml, 'utf8');
147:   const attr = (name) => Number((text.match(new RegExp(`${name}="(\\d+)"`)) ?? [])[1] ?? -1);
148:   return { tests: attr('tests'), failures: attr('failures'), errors: attr('errors'), skipped: attr('skipped') };
149: }
150: const countTables = (pattern) => Number((psqlOut('postgres', 'zhongshu',
151:   `SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND lower(table_name) LIKE '${pattern}'`).stdout ?? '0').trim()) || 0;
152: 
153: // ---- S0：关闭 BPM 的结构性证明——System/Infra 全基线零流程表 ----
154: {
155:   const act = countTables('act\\_%'), flw = countTables('flw\\_%');
156:   record('S0 关闭BPM无副作用：基线零 ACT_/FLW_ 表', act === 0 && flw === 0, `ACT_=${act} FLW_=${flw}`);
157: }
158: // ---- S1：低权限运行账号无 DDL ----
159: {
160:   const r = psqlOk('zhongshu_app', 'zhongshu', 'CREATE TABLE zszj_app_should_fail(id int);');
161:   record('S1 运行账号无DDL：zhongshu_app 建表被拒', r === false, r ? 'app 竟然建表成功' : '按预期失败');
162: }
163: 
164: // ---- 阶段1 bootstrap（owner 账号：建表 + 部署 + 发起 + 留积压）----
165: console.log('[bpm001] 阶段1 bootstrap：owner 账号建引擎表/部署中性夹具/发起实例/留异步积压…');
166: mkdirSync(outDir, { recursive: true });
167: const bootLog = runHarnessTest('BpmPgHarnessBootstrapTest', 'zhongshu_owner', 'owner_local_1', 'true', 'false', 20 * 60 * 1000);
168: writeFileSync(join(outDir, 'bootstrap.log'), bootLog.output);
169: const bootSummary = surefireSummary('BpmPgHarnessBootstrapTest');
170: const bootOk = bootLog.status === 0 && bootSummary && bootSummary.tests > 0 && bootSummary.failures === 0 && bootSummary.errors === 0;
171: record('P1 bootstrap 引擎建表/部署幂等/发起/租户标签/版本钉住', bootOk,
172:   bootSummary ? `tests=${bootSummary.tests} failures=${bootSummary.failures} errors=${bootSummary.errors} skipped=${bootSummary.skipped}` : `exit=${bootLog.status}（无 surefire 报告）`);
173: if (!bootOk) {
174:   console.error('[bpm001] bootstrap 阶段失败，日志见 outputs/bpm-001/bootstrap.log（尾部）：\n' + bootLog.output.split('\n').slice(-40).join('\n'));
175:   finish();
176: }
177: 
178: const actAfterBootstrap = countTables('act\\_%');
179: const schemaVersionAfterBootstrap = (psqlOut('postgres', 'zhongshu', "SELECT VALUE_ FROM ACT_GE_PROPERTY WHERE NAME_='schema.version'").stdout ?? '').trim();
180: 
181: // ---- 阶段2 runtime（app 低权限：重启恢复 + 全操作断言 + 零 DDL；执行器由 R70 显式启停）----
182: console.log('[bpm001] 阶段2 runtime：app 低权限重启接入同库/审批操作/事务回滚/积压恢复…');
183: const runLog = runHarnessTest('BpmPgHarnessRuntimeTest', 'zhongshu_app', 'app_local_1', 'false', 'false', 15 * 60 * 1000);
184: writeFileSync(join(outDir, 'runtime.log'), runLog.output);
185: const runSummary = surefireSummary('BpmPgHarnessRuntimeTest');
186: const runOk = runLog.status === 0 && runSummary && runSummary.tests > 0 && runSummary.failures === 0 && runSummary.errors === 0;
187: record('P2 runtime 重启恢复/通过/拒绝/撤回/转办/分页历史/事务回滚/积压恢复', runOk,
188:   runSummary ? `tests=${runSummary.tests} failures=${runSummary.failures} errors=${runSummary.errors} skipped=${runSummary.skipped}` : `exit=${runLog.status}（无 surefire 报告）`);
189: if (!runOk) {
190:   console.error('[bpm001] runtime 阶段失败，日志见 outputs/bpm-001/runtime.log（尾部）：\n' + runLog.output.split('\n').slice(-40).join('\n'));
191: }
192: 
193: // ---- S2：运行期零 DDL（表数量与 schema.version 不变）+ 探针落库 ----
194: {
195:   const actAfterRuntime = countTables('act\\_%');
196:   const schemaVersionAfterRuntime = (psqlOut('postgres', 'zhongshu', "SELECT VALUE_ FROM ACT_GE_PROPERTY WHERE NAME_='schema.version'").stdout ?? '').trim();
197:   const probeRows = Number((psqlOut('postgres', 'zhongshu', 'SELECT count(*) FROM bpm_harness_probe').stdout ?? '0').trim()) || 0;
198:   record('S2 运行期零DDL：引擎表数量与 schema.version 不变',
199:     actAfterBootstrap === actAfterRuntime && schemaVersionAfterBootstrap === schemaVersionAfterRuntime,
200:     `ACT_ ${actAfterBootstrap}→${actAfterRuntime}, schema.version ${schemaVersionAfterBootstrap}→${schemaVersionAfterRuntime}`);
201:   record('S3 异步回声探针落库（owner 引导版本 + app 运行期回声）', probeRows >= 3, `探针行=${probeRows}`);
202: }
203: 
204: function finish() {
205:   cleanup();
206:   console.log('\n===== ZS-BPM-001 BPM 独立装配与 PG 验收汇总 =====');
207:   for (const r of results) console.log(`${r.ok ? 'PASS' : 'FAIL'}  ${r.id} ${r.note}`);
208:   console.log(`合计 ${results.length} 项，失败 ${failCount} 项`);
209:   try {
210:     writeFileSync(join(outDir, 'runtime-report.json'),
211:       JSON.stringify({ finishedAt: new Date().toISOString(), container: cleaned ? container : `${container}(运行中)`, results, pass, failCount }, null, 2));
212:   } catch { /* 报告落盘失败不影响退出码语义 */ }
213:   process.exit(failCount === 0 ? 0 : 1);
214: }
215: finish();
216: 
services/zhongshu-core/zszj-module-bpm/pom.xml
1: <?xml version="1.0" encoding="UTF-8"?>
2: <project xmlns="http://maven.apache.org/POM/4.0.0"
3:          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
4:          xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
5:     <parent>
6:         <artifactId>zszj</artifactId>
7:         <groupId>cn.zszj</groupId>
8:         <version>${revision}</version>
9:     </parent>
10:     <modelVersion>4.0.0</modelVersion>
11:     <artifactId>zszj-module-bpm</artifactId>
12:     <packaging>jar</packaging>
13: 
14:     <name>${project.artifactId}</name>
15:     <description>
16:         bpm 包下，业务流程管理（Business Process Management），我们放工作流的功能。
17:         例如说：流程定义、表单配置、审核中心（我的申请、我的待办、我的已办）等等
18:         bpm 解释：https://baike.baidu.com/item/BPM/1933
19: 
20:         工作流基于 Flowable 6 实现，分成流程定义、流程表单、流程实例、流程任务等功能模块。
21:     </description>
22: 
23:     <dependencies>
24:         <dependency>
25:             <groupId>cn.zszj</groupId>
26:             <artifactId>zszj-module-system</artifactId>
27:             <version>${revision}</version>
28:         </dependency>
29: 
30:         <!-- 业务组件 -->
31:         <dependency>
32:             <groupId>cn.zszj</groupId>
33:             <artifactId>zszj-spring-boot-starter-biz-data-permission</artifactId>
34:         </dependency>
35:         <dependency>
36:             <groupId>cn.zszj</groupId>
37:             <artifactId>zszj-spring-boot-starter-biz-tenant</artifactId>
38:         </dependency>
39: 
40:         <!-- Web 相关 -->
41:         <dependency>
42:             <groupId>cn.zszj</groupId>
43:             <artifactId>zszj-spring-boot-starter-web</artifactId>
44:         </dependency>
45: 
46:         <dependency>
47:             <groupId>cn.zszj</groupId>
48:             <artifactId>zszj-spring-boot-starter-security</artifactId>
49:         </dependency>
50: 
51:         <!-- DB 相关 -->
52:         <dependency>
53:             <groupId>cn.zszj</groupId>
54:             <artifactId>zszj-spring-boot-starter-mybatis</artifactId>
55:         </dependency>
56: 
57:         <!-- Test 测试相关 -->
58:         <dependency>
59:             <groupId>cn.zszj</groupId>
60:             <artifactId>zszj-spring-boot-starter-test</artifactId>
61:         </dependency>
62: 
63:         <!-- 工具类相关 -->
64:         <dependency>
65:             <groupId>cn.zszj</groupId>
66:             <artifactId>zszj-spring-boot-starter-excel</artifactId>
67:         </dependency>
68: 
69:         <!-- Flowable 工作流相关 -->
70:         <dependency>
71:             <groupId>org.flowable</groupId>
72:             <artifactId>flowable-spring-boot-starter-process</artifactId>
73:         </dependency>
74:         <dependency>
75:             <groupId>org.flowable</groupId>
76:             <artifactId>flowable-spring-boot-starter-actuator</artifactId>
77:         </dependency>
78: 
79:         <!-- ZS-BPM-001：PG 集成夹具驱动（仅测试作用域；模块运行时不携带 JDBC 驱动，驱动由 server 侧统一引入） -->
80:         <dependency>
81:             <groupId>org.postgresql</groupId>
82:             <artifactId>postgresql</artifactId>
83:             <scope>test</scope>
84:         </dependency>
85:     </dependencies>
86: </project>
87: 
services/zhongshu-core/zszj-module-bpm/src/main/java/cn/zszj/module/bpm/framework/flowable/config/BpmFlowableConfiguration.java
1: package cn.zszj.module.bpm.framework.flowable.config;
2: 
3: import cn.hutool.core.collection.ListUtil;
4: import cn.zszj.module.bpm.framework.flowable.core.behavior.BpmActivityBehaviorFactory;
5: import cn.zszj.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateInvoker;
6: import cn.zszj.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateStrategy;
7: import cn.zszj.module.bpm.framework.flowable.core.event.BpmProcessInstanceEventPublisher;
8: import cn.zszj.module.system.api.user.AdminUserApi;
9: import org.flowable.common.engine.api.delegate.FlowableFunctionDelegate;
10: import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
11: import org.flowable.spring.SpringProcessEngineConfiguration;
12: import org.flowable.spring.boot.EngineConfigurationConfigurer;
13: import org.springframework.beans.factory.ObjectProvider;
14: import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
15: import org.springframework.context.ApplicationEventPublisher;
16: import org.springframework.context.annotation.Bean;
17: import org.springframework.context.annotation.Configuration;
18: import org.springframework.core.task.AsyncTaskExecutor;
19: import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
20: 
21: import java.util.List;
22: 
23: /**
24:  * BPM 模块的 Flowable 配置类
25:  *
26:  * @author jason
27:  */
28: @Configuration(proxyBeanMethods = false)
29: public class BpmFlowableConfiguration {
30: 
31:     /**
32:      * 参考 {@link org.flowable.spring.boot.FlowableJobConfiguration} 类，创建对应的 AsyncListenableTaskExecutor Bean
33:      * <p>
34:      * 如果不创建，会导致项目启动时，Flowable 报错的问题
35:      */
36:     @Bean(name = "applicationTaskExecutor")
37:     @ConditionalOnMissingBean(name = "applicationTaskExecutor")
38:     public AsyncTaskExecutor taskExecutor() {
39:         ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
40:         executor.setCorePoolSize(8);
41:         executor.setMaxPoolSize(8);
42:         executor.setQueueCapacity(100);
43:         executor.setThreadNamePrefix("flowable-task-Executor-");
44:         executor.setAwaitTerminationSeconds(30);
45:         executor.setWaitForTasksToCompleteOnShutdown(true);
46:         executor.setAllowCoreThreadTimeOut(true);
47:         executor.initialize();
48:         return executor;
49:     }
50: 
51:     /**
52:      * BPM 模块的 ProcessEngineConfigurationConfigurer 实现类：
53:      *
54:      * 1. 设置各种监听器
55:      * 2. 设置自定义的 ActivityBehaviorFactory 实现
56:      */
57:     @Bean
58:     public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> bpmProcessEngineConfigurationConfigurer(
59:             ObjectProvider<FlowableEventListener> listeners,
60:             ObjectProvider<FlowableFunctionDelegate> customFlowableFunctionDelegates,
61:             BpmActivityBehaviorFactory bpmActivityBehaviorFactory) {
62:         return configuration -> {
63:             // 注册监听器，例如说 BpmActivityEventListener
64:             configuration.setEventListeners(ListUtil.toList(listeners.iterator()));
65:             // 设置 ActivityBehaviorFactory 实现类，用于流程任务的审核人的自定义
66:             configuration.setActivityBehaviorFactory(bpmActivityBehaviorFactory);
67:             // 设置自定义的函数
68:             configuration.setCustomFlowableFunctionDelegates(ListUtil.toList(customFlowableFunctionDelegates.stream().iterator()));
69:         };
70:     }
71: 
72:     // =========== 审批人相关的 Bean ==========
73: 
74:     @Bean
75:     public BpmActivityBehaviorFactory bpmActivityBehaviorFactory(BpmTaskCandidateInvoker bpmTaskCandidateInvoker) {
76:         BpmActivityBehaviorFactory bpmActivityBehaviorFactory = new BpmActivityBehaviorFactory();
77:         bpmActivityBehaviorFactory.setTaskCandidateInvoker(bpmTaskCandidateInvoker);
78:         return bpmActivityBehaviorFactory;
79:     }
80: 
81:     @Bean
82:     @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") // adminUserApi 可以注入成功
83:     public BpmTaskCandidateInvoker bpmTaskCandidateInvoker(List<BpmTaskCandidateStrategy> strategyList,
84:                                                            AdminUserApi adminUserApi) {
85:         return new BpmTaskCandidateInvoker(strategyList, adminUserApi);
86:     }
87: 
88:     // =========== 自己拓展的 Bean ==========
89: 
90:     @Bean
91:     public BpmProcessInstanceEventPublisher processInstanceEventPublisher(ApplicationEventPublisher publisher) {
92:         return new BpmProcessInstanceEventPublisher(publisher);
93:     }
94: 
95: }

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "node -e '
const fs=require(''fs''),vm=require(''vm''),path=require(''path'');
const source=fs.readFileSync(''scripts/db/run-bpm001-verify.mjs'',''utf8'').replace(/"'^import .*;'"\\r?"'$/gm,'"'''').replaceAll(''import.meta.url'',''importUrl'');
const commands=[],errors=[];
let exists=false,exitCode;
const context={
 importUrl:''file:///virtual/scripts/db/run-bpm001-verify.mjs'',URL,Date,Math,
 fileURLToPath:()=>''/virtual/'',join:path.posix.join,existsSync:()=>true,
 process:{platform:''linux'',on:()=>{},exit:c=>{exitCode=c;throw {done:true};}},
 console:{log:()=>{},error:m=>errors.push(m)},
 spawnSync:(bin,args)=>{
  commands.push([bin,...args]);
  if(args[0]===''version'')return {status:0};
  if(args[0]===''run''){
   const reason=exists?''Conflict: container name is already in use'':''bind: address already in use (container object created)'';
   exists=true;errors.push(reason);return {status:125,stderr:reason};
  }
 },
 execFileSync:(bin,args)=>{commands.push([bin,...args]);exists=false;}
};
try{vm.runInNewContext(source,context);}catch(e){if("'!e.done)throw e;}
console.log(JSON.stringify({exitCode,commands,errors,containerReserved:exists}));
'"'; git status --short; node -e \"const fs=require('fs'); console.log(fs.readFileSync('README.md','utf8').split('\\n').slice(94,105).join('\\n'));\"" in E:\zszj-wt-bpm-001-a
 succeeded in 42ms:
 M "services/zhongshu-core/docs/BPM\345\274\225\346\223\216\350\241\250\344\270\216\344\270\232\345\212\241\346\211\251\345\261\225\350\241\250\350\277\201\347\247\273\350\264\243\344\273\273\345\206\263\347\255\226.md"
?? outputs/
## 文档同步规则

文档一致性检查器（ZS-GOV-001）入口，任何文档变更后必须运行：

```bash
node --test scripts/gov/verify-docs.test.mjs
node scripts/gov/verify-docs.mjs
```

以下规则由主台账中的 `FND-DOC-001～003` 统一约束。任何架构、数据库、模块、身份权限、业务链、外部集成或验收状态发生变化时，必须在同一次变更中检查并同步：


codex
已按 `git diff main` 检查全部 42 个文件。基线为 `HEAD=3ea8051c`、`main=270edff2`；其中文件授权、日志脱敏及评审归档差异来自 main 后续提交，**不是 BPM 提交主动回退**，正常合并应保留这些修复。

发现 **3×P1、7×P2，未发现 P0**：

1. **P1：新增第 9 套件会使现有 CI 确定失败。**  
   [run-bpm001-verify.mjs:38](/E:/zszj-wt-bpm-001-a/scripts/db/run-bpm001-verify.mjs:38) 强制要求未入库的 `tools/jdk-17.0.20.1+1` 和 Maven 目录；[pg-regression.yml:17](/E:/zszj-wt-bpm-001-a/.github/workflows/pg-regression.yml:17) 仅 checkout、安装 Node。干净 Ubuntu runner 会退出 3；补齐工具后，独立 Maven 构建仍缺本仓库兄弟模块的已安装产物。  
   **修复：**支持 `JAVA_HOME`/PATH，工作流准备 Java/Maven，并先安装当前提交的必要依赖模块，再执行 BPM 套件；保持根 reactor 的 BPM 注释。

2. **P1：跳过必测用例仍能全绿。**  
   [run-bpm001-verify.mjs:186](/E:/zszj-wt-bpm-001-a/scripts/db/run-bpm001-verify.mjs:186) 与第 170 行只检查 `tests>0`、零失败/错误，忽略已解析的 `skipped`。内存故障注入中，runtime 报告 `tests=9, skipped=8` 仍得到 **6/6、exit 0**。报告路径还复用旧文件，未验证本轮生成。  
   **修复：**要求零跳过、预期用例完整；使用独立报告目录或启动前清除目标报告，并校验 testsuite 身份、报告完整性及本轮生成时间。

3. **P1：结构检查把命令失败转换成通过。**  
   [run-bpm001-verify.mjs:150](/E:/zszj-wt-bpm-001-a/scripts/db/run-bpm001-verify.mjs:150) 将查询错误/空输出转换成 0；第 179、196 行将版本查询失败转换成空串；第 160 行把任意失败当作权限拒绝。故障注入已复现 **S0/S1/S2 查询失败，最终仍 exit 0**。  
   **修复：**统一检查进程错误、退出状态和结果格式；S2 要求非空版本及有效表数；S1 必须确认连接成功且失败为权限不足 SQLSTATE `42501`，不能接受基础设施错误。

4. **P2：清理失败被吞掉，容器和端口可能遗留却报告成功。**  
   [run-bpm001-verify.mjs:52](/E:/zszj-wt-bpm-001-a/scripts/db/run-bpm001-verify.mjs:52) 在删除前就设置 `cleaned=true`，失败后不再重试。注入 `docker rm` 失败后仍 exit 0。也未处理 SIGTERM。  
   **修复：**成功删除后才标记完成；有界重试并验证容器不存在，清理失败记录非零结果；补终止信号处理，并清理关联匿名卷。

5. **P2：启动重试复用失败容器名称和冲突端口，无法恢复常见故障。**  
   [run-bpm001-verify.mjs:76](/E:/zszj-wt-bpm-001-a/scripts/db/run-bpm001-verify.mjs:76) 原样连续执行两次 `docker run`。首次若创建容器后端口绑定失败，第二次会撞同名容器；端口冲突也没有消除。  
   **修复：**重试前删除失败容器，重新分配端口；优先让 Docker 分配宿主端口后读取，并保留每次失败原因。

6. **P2：“关闭 BPM 无副作用”的验收结论超出证据。**  
   [run-bpm001-verify.mjs:153](/E:/zszj-wt-bpm-001-a/scripts/db/run-bpm001-verify.mjs:153) 仅执行迁移并数表，没有启动关闭 BPM 的 server。零引擎表不能证明无流程处理器、引擎 Bean 或后台执行器；`ModuleWhitelistTest` 也是静态检查。  
   **修复：**补关闭 BPM 的真实装配检查，验证无引擎/执行器、流程请求仅走禁用兜底，并观察启动后数据库无流程副作用；此前将文档该项标为部分覆盖。

7. **P2：分页未验证 OFFSET，运行态断言可空洞通过。**  
   [BpmPgHarnessRuntimeTest.java:198](/E:/zszj-wt-bpm-001-a/services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessRuntimeTest.java:198) 所有分页从 0 开始；运行态只断言 `size<=3`，空结果也通过，而且此时恰好仅剩 3 个实例。  
   **修复：**准备超过一页的数据，使用稳定排序读取至少两页，断言准确 ID、无重复、无遗漏及非零 OFFSET 的结果。

8. **P2：回滚测试可能把非预期异常当作模拟失败，且未证明完整恢复。**  
   [BpmPgHarnessRuntimeTest.java:154](/E:/zszj-wt-bpm-001-a/services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessRuntimeTest.java:154) 吞掉任意 `IllegalStateException`；如果发起本身提前抛出该异常，后续“无 A8”仍通过。检查也仅覆盖运行实例，没有历史、任务残留或失败后的重试。  
   **修复：**确认事务内发起成功，再抛专用异常并断言其身份；验证运行/历史/任务无残留，随后重试同一操作并确认成功。

9. **P2：通过测试没有验证实际走通过分支。**  
   [BpmPgHarnessRuntimeTest.java:92](/E:/zszj-wt-bpm-001-a/services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessRuntimeTest.java:92) 只检查结束、版本和输入变量。将 approve 连线误接到 `rejectedEnd`，该测试仍可通过。  
   **修复：**像拒绝测试一样断言结束活动：`approvedEnd=1`、`rejectedEnd=0`。

10. **P2：PASSWORD 缺失不符合“环境变量缺失即失败”的合同。**  
    [BpmPgHarnessConfiguration.java:70](/E:/zszj-wt-bpm-001-a/services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessConfiguration.java:70) 将缺失密码替换为空串；直跑夹具连接 trust 认证 PG 时仍可能通过。  
    **修复：**显式校验密码存在且非空，保留原始密码字符；在创建连接池前完成全部环境参数校验。

其余方面：

- **D-07 §6.5：未发现**固化众墅业务字段、业务状态或不可逆约束；三个流程保持中性。
- **权限与装配：未发现**编排正常路径偷用 owner 运行的问题；app 角色授权与 `schema-update=false` 相符，Spring 事务管理器接入正确。
- **部署幂等、版本钉住、发起、拒绝、撤回、转办、运行实例及积压跨 JVM 恢复、执行器实际消费与显式 shutdown：未发现**明确实现错误。
- **既有门禁：未发现**根/server POM 解注释、PG 驱动作用域扩散、其余 8 套件代码被修改；Windows 固定参数经 `cmd.exe /d /s /c` 调用 Maven 未发现拼接错误。
- **文档链接和日期依据：未发现**新增坏链接或无日期的验收声明。实跑 docs 检查仅报三个既有 `.omx` 链接缺失，与所述环境问题一致。

现有日志/XML支持 bootstrap **5**、runtime **9** 用例零跳过；“6/6”是编排检查项数。本轮完成静态检查及内存故障注入，因沙箱无法访问 Docker，未重新执行真实 PG 回归。

**NEEDS_FIX**
tokens used
131,115
已按 `git diff main` 检查全部 42 个文件。基线为 `HEAD=3ea8051c`、`main=270edff2`；其中文件授权、日志脱敏及评审归档差异来自 main 后续提交，**不是 BPM 提交主动回退**，正常合并应保留这些修复。

发现 **3×P1、7×P2，未发现 P0**：

1. **P1：新增第 9 套件会使现有 CI 确定失败。**  
   [run-bpm001-verify.mjs:38](/E:/zszj-wt-bpm-001-a/scripts/db/run-bpm001-verify.mjs:38) 强制要求未入库的 `tools/jdk-17.0.20.1+1` 和 Maven 目录；[pg-regression.yml:17](/E:/zszj-wt-bpm-001-a/.github/workflows/pg-regression.yml:17) 仅 checkout、安装 Node。干净 Ubuntu runner 会退出 3；补齐工具后，独立 Maven 构建仍缺本仓库兄弟模块的已安装产物。  
   **修复：**支持 `JAVA_HOME`/PATH，工作流准备 Java/Maven，并先安装当前提交的必要依赖模块，再执行 BPM 套件；保持根 reactor 的 BPM 注释。

2. **P1：跳过必测用例仍能全绿。**  
   [run-bpm001-verify.mjs:186](/E:/zszj-wt-bpm-001-a/scripts/db/run-bpm001-verify.mjs:186) 与第 170 行只检查 `tests>0`、零失败/错误，忽略已解析的 `skipped`。内存故障注入中，runtime 报告 `tests=9, skipped=8` 仍得到 **6/6、exit 0**。报告路径还复用旧文件，未验证本轮生成。  
   **修复：**要求零跳过、预期用例完整；使用独立报告目录或启动前清除目标报告，并校验 testsuite 身份、报告完整性及本轮生成时间。

3. **P1：结构检查把命令失败转换成通过。**  
   [run-bpm001-verify.mjs:150](/E:/zszj-wt-bpm-001-a/scripts/db/run-bpm001-verify.mjs:150) 将查询错误/空输出转换成 0；第 179、196 行将版本查询失败转换成空串；第 160 行把任意失败当作权限拒绝。故障注入已复现 **S0/S1/S2 查询失败，最终仍 exit 0**。  
   **修复：**统一检查进程错误、退出状态和结果格式；S2 要求非空版本及有效表数；S1 必须确认连接成功且失败为权限不足 SQLSTATE `42501`，不能接受基础设施错误。

4. **P2：清理失败被吞掉，容器和端口可能遗留却报告成功。**  
   [run-bpm001-verify.mjs:52](/E:/zszj-wt-bpm-001-a/scripts/db/run-bpm001-verify.mjs:52) 在删除前就设置 `cleaned=true`，失败后不再重试。注入 `docker rm` 失败后仍 exit 0。也未处理 SIGTERM。  
   **修复：**成功删除后才标记完成；有界重试并验证容器不存在，清理失败记录非零结果；补终止信号处理，并清理关联匿名卷。

5. **P2：启动重试复用失败容器名称和冲突端口，无法恢复常见故障。**  
   [run-bpm001-verify.mjs:76](/E:/zszj-wt-bpm-001-a/scripts/db/run-bpm001-verify.mjs:76) 原样连续执行两次 `docker run`。首次若创建容器后端口绑定失败，第二次会撞同名容器；端口冲突也没有消除。  
   **修复：**重试前删除失败容器，重新分配端口；优先让 Docker 分配宿主端口后读取，并保留每次失败原因。

6. **P2：“关闭 BPM 无副作用”的验收结论超出证据。**  
   [run-bpm001-verify.mjs:153](/E:/zszj-wt-bpm-001-a/scripts/db/run-bpm001-verify.mjs:153) 仅执行迁移并数表，没有启动关闭 BPM 的 server。零引擎表不能证明无流程处理器、引擎 Bean 或后台执行器；`ModuleWhitelistTest` 也是静态检查。  
   **修复：**补关闭 BPM 的真实装配检查，验证无引擎/执行器、流程请求仅走禁用兜底，并观察启动后数据库无流程副作用；此前将文档该项标为部分覆盖。

7. **P2：分页未验证 OFFSET，运行态断言可空洞通过。**  
   [BpmPgHarnessRuntimeTest.java:198](/E:/zszj-wt-bpm-001-a/services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessRuntimeTest.java:198) 所有分页从 0 开始；运行态只断言 `size<=3`，空结果也通过，而且此时恰好仅剩 3 个实例。  
   **修复：**准备超过一页的数据，使用稳定排序读取至少两页，断言准确 ID、无重复、无遗漏及非零 OFFSET 的结果。

8. **P2：回滚测试可能把非预期异常当作模拟失败，且未证明完整恢复。**  
   [BpmPgHarnessRuntimeTest.java:154](/E:/zszj-wt-bpm-001-a/services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessRuntimeTest.java:154) 吞掉任意 `IllegalStateException`；如果发起本身提前抛出该异常，后续“无 A8”仍通过。检查也仅覆盖运行实例，没有历史、任务残留或失败后的重试。  
   **修复：**确认事务内发起成功，再抛专用异常并断言其身份；验证运行/历史/任务无残留，随后重试同一操作并确认成功。

9. **P2：通过测试没有验证实际走通过分支。**  
   [BpmPgHarnessRuntimeTest.java:92](/E:/zszj-wt-bpm-001-a/services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessRuntimeTest.java:92) 只检查结束、版本和输入变量。将 approve 连线误接到 `rejectedEnd`，该测试仍可通过。  
   **修复：**像拒绝测试一样断言结束活动：`approvedEnd=1`、`rejectedEnd=0`。

10. **P2：PASSWORD 缺失不符合“环境变量缺失即失败”的合同。**  
    [BpmPgHarnessConfiguration.java:70](/E:/zszj-wt-bpm-001-a/services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessConfiguration.java:70) 将缺失密码替换为空串；直跑夹具连接 trust 认证 PG 时仍可能通过。  
    **修复：**显式校验密码存在且非空，保留原始密码字符；在创建连接池前完成全部环境参数校验。

其余方面：

- **D-07 §6.5：未发现**固化众墅业务字段、业务状态或不可逆约束；三个流程保持中性。
- **权限与装配：未发现**编排正常路径偷用 owner 运行的问题；app 角色授权与 `schema-update=false` 相符，Spring 事务管理器接入正确。
- **部署幂等、版本钉住、发起、拒绝、撤回、转办、运行实例及积压跨 JVM 恢复、执行器实际消费与显式 shutdown：未发现**明确实现错误。
- **既有门禁：未发现**根/server POM 解注释、PG 驱动作用域扩散、其余 8 套件代码被修改；Windows 固定参数经 `cmd.exe /d /s /c` 调用 Maven 未发现拼接错误。
- **文档链接和日期依据：未发现**新增坏链接或无日期的验收声明。实跑 docs 检查仅报三个既有 `.omx` 链接缺失，与所述环境问题一致。

现有日志/XML支持 bootstrap **5**、runtime **9** 用例零跳过；“6/6”是编排检查项数。本轮完成静态检查及内存故障注入，因沙箱无法访问 Docker，未重新执行真实 PG 回归。

**NEEDS_FIX**
