OpenAI Codex v0.154.0
--------
workdir: E:\zszj-wt-bpm-001-a
model: gpt-6-astra
provider: openai
approval: never
sandbox: read-only
reasoning effort: xhigh
reasoning summaries: none
session id: 01a09c49-1ae8-7102-b906-97660f603499
--------
user
你是资深后端与测试基建评审员。这是 ZS-BPM-001（分支 feat/bpm-001-a）评审弧的 r2 复核。r1 复核判定 8/10 闭合但遗留 2×P1 未闭合 + 1×P2 新问题（另 1×P2 未闭合），本轮修复提交为 HEAD（相对其父提交），请逐项核对是否闭合：

r1 遗留与本轮申报处置：
1. P1 CI 缺 BOM 安装（-pl system,infra -am 不含 zszj-dependencies，BPM 独立构建解析 BOM 失败 Non-resolvable import POM）→ 已改：pg-regression.yml 与编排器安装命令均显式加入 zszj-dependencies（-pl zszj-dependencies,zszj-module-system,zszj-module-infra -am）。
2. P1 引导后结构查询失败伪绿（catch 只打印+finish，failCount=0 exit 0）→ 已改：该分支先 record('P1b 引导后结构读取', false, ...) 记失败再 finish()。
3. P2 SIGTERM 清理不可靠（主流程同步阻塞，信号回调不可达）→ 已改：依赖模块安装与 bootstrap/runtime 两个长 Maven 阶段改为事件驱动异步 spawn（runAsync，Promise+close 事件），事件循环保活，SIGINT/SIGTERM 处理器可在长阶段期间触发 cleanup 后退出；文件头注释登记「kill -9 级强杀不可拦截」边界。
4. P2（新）Windows PATH 回退探测误判（spawnSync('mvn') 无法启动 mvn.cmd）→ 已改：探测与后续执行统一走 cmd.exe /d /s /c（win32 分支）。

输出格式：逐项判定「闭合/未闭合」（未闭合给 文件:行号 与理由）；如发现修复引入的新问题单独列出（P1/P2）；最后一行总结论：PASS（可合并）或 NEEDS_FIX。

codex
我会先核对 HEAD 相对父提交的改动，再检查四项修复的执行路径和回归风险。
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'git status --short; git branch --show-current; git log -1 --oneline; git diff --stat HEAD''^ HEAD; rg --files -g AGENTS.md -g '"'*pg*regression*' -g '*bpm*' -g '*BPM*'" in E:\zszj-wt-bpm-001-a
 succeeded in 156ms:
warning: unable to access 'C:\Users\Administrator/.config/git/ignore': Permission denied
warning: unable to access 'C:\Users\Administrator/.config/git/ignore': Permission denied
?? outputs/
feat/bpm-001-a
6d826348 fix(bpm-001-a): codex r1 处置（2×P1 闭合+1×P2+1×新P2 全修）——BOM 显式安装/失败路径必记 FAIL/异步 spawn 保信号可达 [ZS-BPM-001]
 .github/workflows/pg-regression.yml |   7 +-
 scripts/db/run-bpm001-verify.mjs    | 361 +++++++++++++++++++-----------------
 2 files changed, 197 insertions(+), 171 deletions(-)
scripts\db\run-pg-regression.mjs
scripts\db\run-bpm001-verify.mjs
apps\zhongshu-miniapp\src\utils\constants\biz-bpm-enum.ts
apps\zhongshu-miniapp\src\static\my-icons\bpm\bpm-running.svg
apps\zhongshu-miniapp\src\static\my-icons\bpm\bpm-reject.svg
apps\zhongshu-miniapp\src\static\my-icons\bpm\bpm-cancel.svg
apps\zhongshu-miniapp\src\static\my-icons\bpm\bpm-approve.svg
services\zhongshu-core\docs\BPM引擎表与业务扩展表迁移责任决策.md
services\zhongshu-core\zszj-module-bpm\src\test\resources\cn\zszj\module\bpm\harness\neutral-async-echo.bpmn20.xml
services\zhongshu-core\zszj-module-bpm\src\test\resources\cn\zszj\module\bpm\harness\neutral-approval.bpmn20.xml
services\zhongshu-core\zszj-module-bpm\src\test\resources\cn\zszj\module\bpm\harness\neutral-approval-v2.bpmn20.xml

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'git diff --no-ext-diff --unified=60 HEAD''^ HEAD -- .github/workflows/pg-regression.yml scripts/db/run-bpm001-verify.mjs' in E:\zszj-wt-bpm-001-a
 succeeded in 0ms:
diff --git a/.github/workflows/pg-regression.yml b/.github/workflows/pg-regression.yml
index 24828e1f..91c696b2 100644
--- a/.github/workflows/pg-regression.yml
+++ b/.github/workflows/pg-regression.yml
@@ -1,31 +1,32 @@
 name: pg-regression
 
 # ZS-DB-019.B 地基：PG 技术回归套件（一次性 PG17 容器，验后自动清理）。
 # 与本地 `node scripts/db/run-pg-regression.mjs` 同一规则。
 # 覆盖：ZS-DB-006 序列/批量/续号、007 字段映射与逻辑删除、008 事务与锁、
 #       CFG-002.A 字典唯一约束、DB-011~015 有界删除、DB-016/017 结构级验证、
 #       ZS-BPM-001 BPM 独立装配与 PG 验收（两阶段）。
 
 on:
   push:
     branches: [main]
   pull_request:
 
 jobs:
   pg-regression:
     runs-on: ubuntu-latest
     steps:
       - uses: actions/checkout@v4
       - uses: actions/setup-node@v4
         with:
           node-version: 22
       - uses: actions/setup-java@v4
         with:
           distribution: temurin
           java-version: 17
-      # ZS-BPM-001 第 9 套件：BPM 独立构建消费本地仓库产物，须先安装当前提交的兄弟依赖模块
+      # ZS-BPM-001 第 9 套件：BPM 独立构建消费本地仓库产物，须先安装当前提交的依赖模块；
+      # zszj-dependencies BOM 不会经 -am 传递，必须显式列入（否则 Non-resolvable import POM）
       # （根 reactor 保持 BPM 注释态=关闭模块，ModuleWhitelistTest 门禁不变）
-      - name: 安装兄弟依赖模块产物
-        run: mvn -B -f services/zhongshu-core/pom.xml -pl zszj-module-system,zszj-module-infra -am install -Dmaven.test.skip=true
+      - name: 安装依赖模块产物
+        run: mvn -B -f services/zhongshu-core/pom.xml -pl zszj-dependencies,zszj-module-system,zszj-module-infra -am install -Dmaven.test.skip=true
       - name: 运行 PG 技术回归套件
         run: node scripts/db/run-pg-regression.mjs
diff --git a/scripts/db/run-bpm001-verify.mjs b/scripts/db/run-bpm001-verify.mjs
index 0ab1155e..fbd7f45b 100644
--- a/scripts/db/run-bpm001-verify.mjs
+++ b/scripts/db/run-bpm001-verify.mjs
@@ -1,317 +1,342 @@
 /**
  * ZS-BPM-001：BPM 独立装配与 PostgreSQL 验收编排器（B09 技术准备，本地与 CI 同一规则）。
  *
  * 两阶段语义（同一一次性 PG 容器内的同一物理库）：
  *   阶段1 bootstrap（zhongshu_owner，schema-update=true，异步执行器挂起）：
  *     - 空库上由 Flowable 引擎自建 ACT_ 表（建表责任=迁移期 owner 账号）；
  *     - 部署中性技术夹具（重复部署幂等、变更部署出新版本）、发起运行实例、留下异步积压。
  *   阶段2 runtime（zhongshu_app 低权限，schema-update=false，异步执行器挂起待 R70 显式启停）：
  *     - 新 JVM 重启接入同库（重启恢复：定义/实例/历史全部可见、schema 版本不变=运行期零 DDL）；
  *     - 通过/拒绝/撤回/转办/分页历史、Spring 事务回滚不留引擎痕迹、积压任务被显式启停的执行器消化且无死信。
  * 结构检查（编排器 psql 直证，不经 Java；全部查询强制成功，失败即 FAIL 不吞错）：
  *   S0 关闭 BPM 结构零残留：System/Infra 基线（全部 Flyway V* 应用后）零 ACT_/FLW_ 表
  *      （运行期装配面证明=既有 V1.6 真实启动联验 + ModuleWhitelistTest 静态门禁，见决策文档边界）；
  *   S1 低权限运行账号无 DDL：app 连接可用（SELECT 1 成功）且 CREATE TABLE 以 SQLSTATE 42501 被拒；
  *   S2 引擎表数量与 schema.version 前后一致（阶段2 未改结构）；
  *   S3 异步回声探针落库。
  * 工具链：优先仓库 tools/（worktree 供给）；否则使用 PATH 上的 java/mvn（CI 已由工作流 provision）；
- *   两者皆缺以退出码 3 明确失败，不静默跳过。套件前置 `mvn install` 兄弟依赖模块
- *   （system/infra 及其依赖链，-Dmaven.test.skip=true），保证 BPM 独立构建消费的是当前提交的产物。
+ *   两者皆缺以退出码 3 明确失败，不静默跳过。套件前置 `mvn install` 依赖模块
+ *   （zszj-dependencies BOM + system/infra 及其依赖链，-Dmaven.test.skip=true），保证 BPM 独立
+ *   构建消费的是当前提交的产物（BPM 不入 reactor，BOM 不会经 -am 传递，须显式列出）。
  *
  * 语义（对齐 scripts/db/run-db018-verify.mjs / test-pg-fixture.mjs 夹具约定）：
  *   - 实例隔离：容器名随机唯一，验后强制清理（docker rm -f -v，重试后验证不存在）；
  *   - 失败非零：任一阶段/检查失败 → 非零退出；
  *   - 缺依赖不静默跳过：Docker/工具链不可用 → 退出码 3；
  *   - Docker 负载 flaky：容器启动失败先清理残件、换端口重试一次。
+ *   - 长 Maven 阶段走异步 spawn：SIGINT/SIGTERM 在事件循环内可达，触发清理后退出
+ *     （sync 快操作除外；kill -9 级强杀不可拦截，容器遗留交由环境清理）。
  * 证据报告：JSON 摘要落 outputs/bpm-001/runtime-report.json（outputs/ 不入库）。
  *
  * 用法：node scripts/db/run-bpm001-verify.mjs
  */
-import { execFileSync, spawnSync } from 'node:child_process';
+import { execFileSync, spawn, spawnSync } from 'node:child_process';
 import { readFileSync, existsSync, mkdirSync, writeFileSync, readdirSync, rmSync } from 'node:fs';
 import { fileURLToPath } from 'node:url';
 import { join } from 'node:path';
 
 const root = fileURLToPath(new URL('../../', import.meta.url));
 const core = join(root, 'services', 'zhongshu-core');
 const migrationDir = join(core, 'zszj-server', 'src', 'main', 'resources', 'db', 'migration');
 const outDir = join(root, 'outputs', 'bpm-001');
+const isWin = process.platform === 'win32';
 
 function fail(code, message) { console.error(message); process.exit(code); }
 
 // ---- 工具链解析：tools/ 优先（本地/worktree），否则 PATH（CI 由工作流 provision），皆缺退出码 3 ----
 const toolsDir = join(root, 'tools');
 const jdkDir = join(toolsDir, 'jdk-17.0.20.1+1');
 const mavenBin = join(toolsDir, 'apache-maven-3.9.9', 'bin');
 let javaHomeEnv, pathPrefix;
 if (existsSync(jdkDir) && existsSync(mavenBin)) {
   javaHomeEnv = jdkDir;
-  pathPrefix = process.platform === 'win32' ? `${jdkDir}\\bin;${mavenBin};` : `${jdkDir}/bin:${mavenBin}:`;
+  pathPrefix = isWin ? `${jdkDir}\\bin;${mavenBin};` : `${jdkDir}/bin:${mavenBin}:`;
 } else {
-  const probe = spawnSync('mvn', ['-v'], { encoding: 'utf8' });
-  const hasJava = spawnSync(process.platform === 'win32' ? 'java.exe' : 'java', ['-version'], { encoding: 'utf8' });
+  // Windows 下 mvn 是 .cmd，须经 cmd.exe 调起（与后续执行方式一致），否则探测恒 ENOENT 误判缺失
+  const probe = isWin ? spawnSync('cmd.exe', ['/d', '/s', '/c', 'mvn -v'], { encoding: 'utf8' })
+    : spawnSync('mvn', ['-v'], { encoding: 'utf8' });
+  const hasJava = isWin ? spawnSync('cmd.exe', ['/d', '/s', '/c', 'java -version'], { encoding: 'utf8' })
+    : spawnSync('java', ['-version'], { encoding: 'utf8' });
   if (probe.error || probe.status !== 0 || hasJava.error || hasJava.status !== 0) {
     fail(3, `[bpm001] 工具链缺失：无 tools/ 供给且 PATH 上无可用 mvn/java（本地请供给 tools/，CI 由 pg-regression.yml provision）`);
   }
   javaHomeEnv = process.env.JAVA_HOME;
   pathPrefix = '';
 }
 const childEnv = {
   ...process.env,
   ...(javaHomeEnv ? { JAVA_HOME: javaHomeEnv } : {}),
   PATH: `${pathPrefix}${process.env.PATH}`,
 };
 
 // ---- Docker 预检（缺依赖不静默跳过）----
 const dockerUp = spawnSync('docker', ['version', '--format', '{{.Server.Version}}'], { encoding: 'utf8' });
 if (dockerUp.error || dockerUp.status !== 0) {
   fail(3, `[bpm001] Docker 不可用（${dockerUp.error?.message ?? `exit=${dockerUp.status}`}）：验证不得静默跳过`);
 }
 
 const container = `zszj-bpm001-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
 let port = 4332 + Math.floor(Math.random() * 700);
 let cleaned = false;
 let cleanupError = '';
 const cleanup = () => {
   if (cleaned) return;
   for (let attempt = 0; attempt < 3 && !cleaned; attempt++) {
     const r = spawnSync('docker', ['rm', '-f', '-v', container], { encoding: 'utf8' });
     if (r.status === 0) { cleaned = true; break; }
     cleanupError = (r.stderr ?? '').trim();
     Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500);
   }
 };
 const onSignal = (signal) => { cleanup(); process.exit(signal === 'SIGINT' ? 130 : 143); };
 process.on('exit', () => { if (!cleaned) cleanup(); });
 process.on('SIGINT', () => onSignal('SIGINT'));
 process.on('SIGTERM', () => onSignal('SIGTERM'));
 
 // ---- psql 严格封装：任何查询失败/空结果都必须显式暴露，不吞错 ----
 const psqlRun = (user, db, sql) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', user, '-d', db,
   '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 });
 function psqlScalar(user, db, sql) {
   const r = spawnSync('docker', ['exec', container, 'psql', '-U', user, '-d', db, '-At', '-c', sql],
     { encoding: 'utf8', maxBuffer: 8 * 1024 * 1024 });
   if (r.error || r.status !== 0) throw new Error(`psql 查询失败（exit=${r.status ?? 'ERR'}）：${((r.stderr ?? '') || (r.stdout ?? '')).slice(0, 300)}`);
   const value = (r.stdout ?? '').trim();
   if (value === '') throw new Error(`psql 查询返回空：${sql.slice(0, 120)}`);
   return value;
 }
 function psqlScalarInt(user, db, sql) {
   const value = psqlScalar(user, db, sql);
   const n = Number(value);
   if (!Number.isInteger(n) || n < 0) throw new Error(`期望非负整数，实际「${value}」：${sql.slice(0, 120)}`);
   return n;
 }
 
 let pass = 0, failCount = 0;
 const results = [];
 const record = (id, ok, note = '') => { results.push({ id, ok, note }); ok ? pass++ : failCount++; console.log(`[${ok ? 'PASS' : 'FAIL'}] ${id} ${note}`); };
 
+// ---- 长 Maven 阶段：异步 spawn（事件循环保活，SIGINT/SIGTERM 可达并触发清理）----
+// Windows 下 Node spawn .cmd 有 EINVAL 防护，须经 cmd.exe
+function runAsync(argv, opts = {}) {
+  return new Promise((resolve) => {
+    const child = spawn(argv[0], argv.slice(1), { cwd: core, env: { ...childEnv, ...opts.env }, stdio: ['ignore', 'pipe', 'pipe'] });
+    let output = '';
+    child.stdout.on('data', (d) => { output += d; });
+    child.stderr.on('data', (d) => { output += d; });
+    const timer = opts.timeoutMs ? setTimeout(() => child.kill(), opts.timeoutMs) : null;
+    child.on('error', (e) => { if (timer) clearTimeout(timer); resolve({ status: -1, output: String(e) }); });
+    child.on('close', (code) => { if (timer) clearTimeout(timer); resolve({ status: code, output }); });
+  });
+}
+const mvnArgv = (mvnArgs) => isWin ? ['cmd.exe', '/d', '/s', '/c', 'mvn ' + mvnArgs.join(' ')] : ['mvn', ...mvnArgs];
+
 // ---- 拉起一次性 PG（负载 flaky：清理残件+换端口重试一次）----
 function dockerRunOnce() {
   const args = ['run', '-d', '--name', container, '-e', 'POSTGRES_PASSWORD=bpm001', '-p', `127.0.0.1:${port}:5432`, 'postgres:17-alpine'];
-  const r = process.platform === 'win32'
-    ? spawnSync('cmd.exe', ['/d', '/s', '/c', 'docker ' + args.join(' ')], { encoding: 'utf8' })
+  const r = isWin ? spawnSync('cmd.exe', ['/d', '/s', '/c', 'docker ' + args.join(' ')], { encoding: 'utf8' })
     : spawnSync('docker', args, { encoding: 'utf8' });
   if (r.status === 0) return true;
   console.error(`[bpm001] docker run 失败（port=${port}）：${((r.stderr ?? '') || '').trim().slice(0, 200)}`);
   return false;
 }
-console.log(`[bpm001] 拉起临时 PG（${container} @ 127.0.0.1:${port}）…`);
-if (!dockerRunOnce()) {
-  spawnSync('docker', ['rm', '-f', '-v', container], { stdio: 'ignore' });
-  port = 4332 + Math.floor(Math.random() * 700);
-  console.log(`[bpm001] 重试：新端口 ${port}`);
-  if (!dockerRunOnce()) fail(3, '[bpm001] PG 夹具容器两次启动失败（Docker 负载竞态按约定重试一次后仍失败）');
-}
-
-let ready = false;
-// 就绪探测必须走 TCP（-h 127.0.0.1）：initdb 期间的临时服务器只监听 unix socket，
-// socket 探测可能误判就绪导致 CREATE DATABASE 落到临时库上失败
-for (let i = 0; i < 40; i++) {
-  const r = spawnSync('docker', ['exec', container, 'psql', '-h', '127.0.0.1', '-U', 'postgres', '-d', 'postgres', '-At', '-c', 'SELECT 1'], { encoding: 'utf8' });
-  if (r.status === 0) { ready = true; break; }
-  Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500);
-}
-if (!ready) { cleanup(); fail(1, '[bpm001] PG 未就绪'); }
-
-// ---- 基线库：建库 + 角色授权 + 全部 Flyway 迁移 ----
-{
-  let created = false, last = '';
-  for (let i = 0; i < 10 && !created; i++) {
-    const r = psqlRun('postgres', 'postgres', 'CREATE DATABASE zhongshu;');
-    created = r.status === 0;
-    if (!created) { last = ((r.stderr ?? '') + (r.stdout ?? '')).trim(); Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 1000); }
-  }
-  if (!created) { cleanup(); fail(1, `[bpm001] 建库失败（重试 10 次）：${last.slice(0, 400)}`); }
-}
-{
-  const setup = readFileSync(join(core, 'sql', 'postgresql', 'env-setup-test.sql'), 'utf8');
-  const r = psqlRun('postgres', 'zhongshu', setup);
-  if (r.status !== 0) { cleanup(); fail(1, `[bpm001] 角色授权失败:\n${r.stdout}${r.stderr}`); }
-}
-{
-  const migrations = readdirSync(migrationDir).filter((f) => /^V.*\.sql$/.test(f)).sort();
-  const bundled = migrations.map((f) => readFileSync(join(migrationDir, f), 'utf8')).join('\n');
-  const r = psqlRun('zhongshu_owner', 'zhongshu', bundled);
-  if (r.status !== 0) { cleanup(); fail(1, `[bpm001] 基线迁移执行失败（${migrations.length} 个 V*）:\n${r.stdout}${r.stderr}`); }
-  console.log(`[bpm001] 已应用基线迁移 ${migrations.length} 个：${migrations.join(', ')}`);
-}
-if (psqlRun('zhongshu_owner', 'zhongshu',
-  'CREATE TABLE bpm_harness_probe(id bigserial PRIMARY KEY, note text NOT NULL, created_at timestamptz NOT NULL DEFAULT now());').status !== 0) {
-  cleanup(); fail(1, '[bpm001] 探针表创建失败');
-}
-
-// ---- 兄弟依赖模块产物安装（BPM 独立构建不在 reactor，消费本地仓库产物须来自当前提交）----
-console.log('[bpm001] mvn install 兄弟依赖模块（system/infra 及依赖链，跳过测试编译）…');
-const installArgs = ['-B', '-pl', 'zszj-module-system,zszj-module-infra', '-am', 'install', '-Dmaven.test.skip=true'];
-const install = spawnSync(process.platform === 'win32' ? 'cmd.exe' : 'mvn',
-  process.platform === 'win32' ? ['/d', '/s', '/c', 'mvn ' + installArgs.join(' ')] : installArgs,
-  { cwd: core, env: childEnv, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024, timeout: 20 * 60 * 1000 });
-if (install.status !== 0) {
-  cleanup();
-  fail(1, '[bpm001] 兄弟依赖模块安装失败：\n' + ((install.stdout ?? '') + (install.stderr ?? '')).split('\n').slice(-25).join('\n'));
-}
 
-// ---- Maven 子进程（Windows 下 Node spawn .cmd 有 EINVAL 防护，须经 cmd.exe）----
-function runHarnessTest(testClass, username, password, schemaUpdate, asyncExecutor, timeoutMs) {
-  const args = ['-B', '-f', 'zszj-module-bpm/pom.xml', 'test', `-Dtest=${testClass}`];
-  const cmd = `mvn ${args.join(' ')}`;
-  const r = spawnSync(process.platform === 'win32' ? 'cmd.exe' : 'mvn',
-    process.platform === 'win32' ? ['/d', '/s', '/c', cmd] : args,
-    {
-      cwd: core, env: {
-        ...childEnv,
-        ZSZJ_BPM_HARNESS_JDBC_URL: `jdbc:postgresql://127.0.0.1:${port}/zhongshu`,
-        ZSZJ_BPM_HARNESS_USERNAME: username,
-        ZSZJ_BPM_HARNESS_PASSWORD: password,
-        ZSZJ_BPM_HARNESS_SCHEMA_UPDATE: schemaUpdate,
-        ZSZJ_BPM_HARNESS_ASYNC_EXECUTOR: asyncExecutor,
-      },
-      encoding: 'utf8', maxBuffer: 64 * 1024 * 1024, timeout: timeoutMs,
-    });
-  return { ...r, output: (r.stdout ?? '') + (r.stderr ?? '') };
-}
+const reportXml = (testClass) => join(core, 'zszj-module-bpm', 'target', 'surefire-reports',
+  `TEST-cn.zszj.module.bpm.harness.${testClass}.xml`);
+const countActFlw = (pattern) => psqlScalarInt('postgres', 'zhongshu',
+  `SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND lower(table_name) LIKE '${pattern}'`);
+const schemaVersion = () => psqlScalar('postgres', 'zhongshu', "SELECT VALUE_ FROM ACT_GE_PROPERTY WHERE NAME_='schema.version'");
 
 // surefire 严格校验：先删本轮报告（防陈旧文件复用），跑后要求存在、身份匹配、
 // 用例数精确、零跳过零失败零错误
-const reportXml = (testClass) => join(core, 'zszj-module-bpm', 'target', 'surefire-reports',
-  `TEST-cn.zszj.module.bpm.harness.${testClass}.xml`);
 function assertSurefire(testClass, expectedTests) {
   const xml = reportXml(testClass);
   rmSync(xml, { force: true });
   return () => {
     if (!existsSync(xml)) throw new Error(`本轮 surefire 报告未生成：${xml}`);
     const text = readFileSync(xml, 'utf8');
     const attr = (name) => (text.match(new RegExp(`${name}="(\\d+)"`)) ?? [])[1];
     const suiteName = (text.match(/name="([^"]+)"/) ?? [])[1];
     if (suiteName !== `cn.zszj.module.bpm.harness.${testClass}`) throw new Error(`报告身份不符：${suiteName}`);
     const tests = Number(attr('tests')), failures = Number(attr('failures')),
       errors = Number(attr('errors')), skipped = Number(attr('skipped'));
     if (tests !== expectedTests) throw new Error(`用例数 ${tests} ≠ 预期 ${expectedTests}`);
     if (skipped !== 0) throw new Error(`存在跳过用例 ${skipped}（必测集不得跳过）`);
     if (failures !== 0 || errors !== 0) throw new Error(`failures=${failures} errors=${errors}`);
     return `tests=${tests} failures=0 errors=0 skipped=0`;
   };
 }
 
-const countActFlw = (pattern) => psqlScalarInt('postgres', 'zhongshu',
-  `SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND lower(table_name) LIKE '${pattern}'`);
-const schemaVersion = () => psqlScalar('postgres', 'zhongshu', "SELECT VALUE_ FROM ACT_GE_PROPERTY WHERE NAME_='schema.version'");
-
-// ---- S0：关闭 BPM 结构零残留——System/Infra 全基线零流程表（查询失败即 FAIL）----
-let actAfterBootstrap = -1, versionAfterBootstrap = '';
-try {
-  const act = countActFlw('act\\_%'), flw = countActFlw('flw\\_%');
-  record('S0 关闭BPM结构零残留：基线零 ACT_/FLW_ 表', act === 0 && flw === 0, `ACT_=${act} FLW_=${flw}`);
-} catch (e) {
-  record('S0 关闭BPM结构零残留：基线零 ACT_/FLW_ 表', false, String(e.message ?? e));
-}
-
-// ---- S1：低权限运行账号连接可用且无 DDL（区分权限拒绝 SQLSTATE 42501 与基础设施错误）----
-// psql 默认 verbosity 不回显 SQLSTATE，须 verbose 才能对 42501 做精确判定（PG15+ 建表被拒为 schema 级）
-{
-  let ok = false, note = '';
-  try {
-    psqlScalar('zhongshu_app', 'zhongshu', 'SELECT 1');
-    const r = spawnSync('docker', ['exec', '-i', container, 'psql', '-U', 'zhongshu_app', '-d', 'zhongshu',
-      '-v', 'ON_ERROR_STOP=1', '-q'], { input: '\\set VERBOSITY verbose\nCREATE TABLE zszj_app_should_fail(id int);', encoding: 'utf8' });
-    const output = ((r.stderr ?? '') + (r.stdout ?? '')).trim();
-    if (r.status === 0) { note = 'app 竟然建表成功'; }
-    else if (output.includes('42501')) { ok = true; note = '建表按 SQLSTATE 42501（权限不足）被拒，app 连接本身可用'; }
-    else { note = `建表失败但非权限拒绝：${output.slice(0, 160)}`; }
-  } catch (e) { note = String(e.message ?? e); }
-  record('S1 运行账号无DDL：app 连接可用且建表被 42501 拒绝', ok, note);
-}
-
-// ---- 阶段1 bootstrap（owner 账号：建表 + 部署 + 发起 + 留积压）----
-console.log('[bpm001] 阶段1 bootstrap：owner 账号建引擎表/部署中性夹具/发起实例/留异步积压…');
-mkdirSync(outDir, { recursive: true });
-const bootAssert = assertSurefire('BpmPgHarnessBootstrapTest', 5);
-const bootLog = runHarnessTest('BpmPgHarnessBootstrapTest', 'zhongshu_owner', 'owner_local_1', 'true', 'false', 30 * 60 * 1000);
-writeFileSync(join(outDir, 'bootstrap.log'), bootLog.output);
-let bootOk = false, bootNote = '';
-try { bootNote = bootAssert(); bootOk = bootLog.status === 0; if (!bootOk) bootNote += `，但 mvn exit=${bootLog.status}`; }
-catch (e) { bootNote = String(e.message ?? e); }
-record('P1 bootstrap 引擎建表/部署幂等/发起/租户标签/版本钉住', bootOk, bootNote);
-if (!bootOk) {
-  console.error('[bpm001] bootstrap 阶段失败，日志见 outputs/bpm-001/bootstrap.log（尾部）：\n' + bootLog.output.split('\n').slice(-40).join('\n'));
-  finish();
+function runHarnessTestArgs(testClass, username, password, schemaUpdate, asyncExecutor) {
+  return mvnArgv(['-B', '-f', 'zszj-module-bpm/pom.xml', 'test', `-Dtest=${testClass}`]);
 }
-
-try {
-  actAfterBootstrap = countActFlw('act\\_%');
-  versionAfterBootstrap = schemaVersion();
-} catch (e) {
-  console.error('[bpm001] 引导后结构读取失败：' + String(e.message ?? e));
-  finish();
-}
-
-// ---- 阶段2 runtime（app 低权限：重启恢复 + 全操作断言 + 零 DDL；执行器由 R70 显式启停）----
-console.log('[bpm001] 阶段2 runtime：app 低权限重启接入同库/审批操作/事务回滚/积压恢复…');
-const runAssert = assertSurefire('BpmPgHarnessRuntimeTest', 9);
-const runLog = runHarnessTest('BpmPgHarnessRuntimeTest', 'zhongshu_app', 'app_local_1', 'false', 'false', 20 * 60 * 1000);
-writeFileSync(join(outDir, 'runtime.log'), runLog.output);
-let runOk = false, runNote = '';
-try { runNote = runAssert(); runOk = runLog.status === 0; if (!runOk) runNote += `，但 mvn exit=${runLog.status}`; }
-catch (e) { runNote = String(e.message ?? e); }
-record('P2 runtime 重启恢复/通过/拒绝/撤回/转办/分页历史/事务回滚/积压恢复', runOk, runNote);
-if (!runOk) {
-  console.error('[bpm001] runtime 阶段失败，日志见 outputs/bpm-001/runtime.log（尾部）：\n' + runLog.output.split('\n').slice(-40).join('\n'));
-}
-
-// ---- S2/S3：运行期零 DDL（表数量与 schema.version 不变）+ 探针落库 ----
-{
-  let ok = false, note = '';
-  try {
-    const actAfterRuntime = countActFlw('act\\_%');
-    const versionAfterRuntime = schemaVersion();
-    ok = actAfterBootstrap === actAfterRuntime && versionAfterBootstrap === versionAfterRuntime;
-    note = `ACT_ ${actAfterBootstrap}→${actAfterRuntime}, schema.version ${versionAfterBootstrap}→${versionAfterRuntime}`;
-  } catch (e) { note = String(e.message ?? e); }
-  record('S2 运行期零DDL：引擎表数量与 schema.version 不变', ok, note);
-}
-{
-  let ok = false, note = '';
-  try {
-    const probeRows = psqlScalarInt('postgres', 'zhongshu', 'SELECT count(*) FROM bpm_harness_probe');
-    ok = probeRows >= 3;
-    note = `探针行=${probeRows}`;
-  } catch (e) { note = String(e.message ?? e); }
-  record('S3 异步回声探针落库（owner 引导版本 + app 运行期回声）', ok, note);
+function harnessEnv(username, password, schemaUpdate, asyncExecutor) {
+  return {
+    ZSZJ_BPM_HARNESS_JDBC_URL: `jdbc:postgresql://127.0.0.1:${port}/zhongshu`,
+    ZSZJ_BPM_HARNESS_USERNAME: username,
+    ZSZJ_BPM_HARNESS_PASSWORD: password,
+    ZSZJ_BPM_HARNESS_SCHEMA_UPDATE: schemaUpdate,
+    ZSZJ_BPM_HARNESS_ASYNC_EXECUTOR: asyncExecutor,
+  };
 }
 
 function finish() {
   cleanup();
   if (!cleaned) record('S4 夹具容器清理', false, `docker rm 重试后仍失败：${cleanupError}`);
   console.log('\n===== ZS-BPM-001 BPM 独立装配与 PG 验收汇总 =====');
   for (const r of results) console.log(`${r.ok ? 'PASS' : 'FAIL'}  ${r.id} ${r.note}`);
   console.log(`合计 ${results.length} 项，失败 ${failCount} 项`);
   try {
     writeFileSync(join(outDir, 'runtime-report.json'),
       JSON.stringify({ finishedAt: new Date().toISOString(), container, results, pass, failCount }, null, 2));
   } catch { /* 报告落盘失败不影响退出码语义 */ }
   process.exit(failCount === 0 ? 0 : 1);
 }
-finish();
+
+(async () => {
+  console.log(`[bpm001] 拉起临时 PG（${container} @ 127.0.0.1:${port}）…`);
+  if (!dockerRunOnce()) {
+    spawnSync('docker', ['rm', '-f', '-v', container], { stdio: 'ignore' });
+    port = 4332 + Math.floor(Math.random() * 700);
+    console.log(`[bpm001] 重试：新端口 ${port}`);
+    if (!dockerRunOnce()) fail(3, '[bpm001] PG 夹具容器两次启动失败（Docker 负载竞态按约定重试一次后仍失败）');
+  }
+
+  let ready = false;
+  // 就绪探测必须走 TCP（-h 127.0.0.1）：initdb 期间的临时服务器只监听 unix socket，
+  // socket 探测可能误判就绪导致 CREATE DATABASE 落到临时库上失败
+  for (let i = 0; i < 40; i++) {
+    const r = spawnSync('docker', ['exec', container, 'psql', '-h', '127.0.0.1', '-U', 'postgres', '-d', 'postgres', '-At', '-c', 'SELECT 1'], { encoding: 'utf8' });
+    if (r.status === 0) { ready = true; break; }
+    Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500);
+  }
+  if (!ready) { cleanup(); fail(1, '[bpm001] PG 未就绪'); }
+
+  // ---- 基线库：建库 + 角色授权 + 全部 Flyway 迁移 ----
+  {
+    let created = false, last = '';
+    for (let i = 0; i < 10 && !created; i++) {
+      const r = psqlRun('postgres', 'postgres', 'CREATE DATABASE zhongshu;');
+      created = r.status === 0;
+      if (!created) { last = ((r.stderr ?? '') + (r.stdout ?? '')).trim(); Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 1000); }
+    }
+    if (!created) { cleanup(); fail(1, `[bpm001] 建库失败（重试 10 次）：${last.slice(0, 400)}`); }
+  }
+  {
+    const setup = readFileSync(join(core, 'sql', 'postgresql', 'env-setup-test.sql'), 'utf8');
+    const r = psqlRun('postgres', 'zhongshu', setup);
+    if (r.status !== 0) { cleanup(); fail(1, `[bpm001] 角色授权失败:\n${r.stdout}${r.stderr}`); }
+  }
+  {
+    const migrations = readdirSync(migrationDir).filter((f) => /^V.*\.sql$/.test(f)).sort();
+    const bundled = migrations.map((f) => readFileSync(join(migrationDir, f), 'utf8')).join('\n');
+    const r = psqlRun('zhongshu_owner', 'zhongshu', bundled);
+    if (r.status !== 0) { cleanup(); fail(1, `[bpm001] 基线迁移执行失败（${migrations.length} 个 V*）:\n${r.stdout}${r.stderr}`); }
+    console.log(`[bpm001] 已应用基线迁移 ${migrations.length} 个：${migrations.join(', ')}`);
+  }
+  if (psqlRun('zhongshu_owner', 'zhongshu',
+    'CREATE TABLE bpm_harness_probe(id bigserial PRIMARY KEY, note text NOT NULL, created_at timestamptz NOT NULL DEFAULT now());').status !== 0) {
+    cleanup(); fail(1, '[bpm001] 探针表创建失败');
+  }
+
+  // ---- 依赖模块产物安装（BPM 独立构建不在 reactor，消费本地仓库产物须来自当前提交；
+  //      zszj-dependencies BOM 不会经 -am 传递，必须显式列入）----
+  console.log('[bpm001] mvn install 依赖模块（BOM+system/infra 及依赖链，跳过测试编译）…');
+  const install = await runAsync(
+    mvnArgv(['-B', '-pl', 'zszj-dependencies,zszj-module-system,zszj-module-infra', '-am', 'install', '-Dmaven.test.skip=true']),
+    { timeoutMs: 20 * 60 * 1000 });
+  if (install.status !== 0) {
+    cleanup();
+    fail(1, '[bpm001] 依赖模块安装失败：\n' + install.output.split('\n').slice(-25).join('\n'));
+  }
+
+  // ---- S0：关闭 BPM 结构零残留——System/Infra 全基线零流程表（查询失败即 FAIL）----
+  let actAfterBootstrap = -1, versionAfterBootstrap = '';
+  try {
+    const act = countActFlw('act\\_%'), flw = countActFlw('flw\\_%');
+    record('S0 关闭BPM结构零残留：基线零 ACT_/FLW_ 表', act === 0 && flw === 0, `ACT_=${act} FLW_=${flw}`);
+  } catch (e) {
+    record('S0 关闭BPM结构零残留：基线零 ACT_/FLW_ 表', false, String(e.message ?? e));
+  }
+
+  // ---- S1：低权限运行账号连接可用且无 DDL（区分权限拒绝 SQLSTATE 42501 与基础设施错误）----
+  // psql 默认 verbosity 不回显 SQLSTATE，须 verbose 才能对 42501 做精确判定（PG15+ 建表被拒为 schema 级）
+  {
+    let ok = false, note = '';
+    try {
+      psqlScalar('zhongshu_app', 'zhongshu', 'SELECT 1');
+      const r = spawnSync('docker', ['exec', '-i', container, 'psql', '-U', 'zhongshu_app', '-d', 'zhongshu',
+        '-v', 'ON_ERROR_STOP=1', '-q'], { input: '\\set VERBOSITY verbose\nCREATE TABLE zszj_app_should_fail(id int);', encoding: 'utf8' });
+      const output = ((r.stderr ?? '') + (r.stdout ?? '')).trim();
+      if (r.status === 0) { note = 'app 竟然建表成功'; }
+      else if (output.includes('42501')) { ok = true; note = '建表按 SQLSTATE 42501（权限不足）被拒，app 连接本身可用'; }
+      else { note = `建表失败但非权限拒绝：${output.slice(0, 160)}`; }
+    } catch (e) { note = String(e.message ?? e); }
+    record('S1 运行账号无DDL：app 连接可用且建表被 42501 拒绝', ok, note);
+  }
+
+  // ---- 阶段1 bootstrap（owner 账号：建表 + 部署 + 发起 + 留积压）----
+  console.log('[bpm001] 阶段1 bootstrap：owner 账号建引擎表/部署中性夹具/发起实例/留异步积压…');
+  mkdirSync(outDir, { recursive: true });
+  const bootAssert = assertSurefire('BpmPgHarnessBootstrapTest', 5);
+  const bootLog = await runAsync(
+    runHarnessTestArgs('BpmPgHarnessBootstrapTest'),
+    { env: harnessEnv('zhongshu_owner', 'owner_local_1', 'true', 'false'), timeoutMs: 30 * 60 * 1000 });
+  writeFileSync(join(outDir, 'bootstrap.log'), bootLog.output);
+  let bootOk = false, bootNote = '';
+  try { bootNote = bootAssert(); bootOk = bootLog.status === 0; if (!bootOk) bootNote += `，但 mvn exit=${bootLog.status}`; }
+  catch (e) { bootNote = String(e.message ?? e); }
+  record('P1 bootstrap 引擎建表/部署幂等/发起/租户标签/版本钉住', bootOk, bootNote);
+  if (!bootOk) {
+    console.error('[bpm001] bootstrap 阶段失败，日志见 outputs/bpm-001/bootstrap.log（尾部）：\n' + bootLog.output.split('\n').slice(-40).join('\n'));
+    finish();
+    return;
+  }
+
+  try {
+    actAfterBootstrap = countActFlw('act\\_%');
+    versionAfterBootstrap = schemaVersion();
+  } catch (e) {
+    // 引导后结构读取失败必须记 FAIL 再收尾，不得伪绿
+    record('P1b 引导后结构读取（表数量/schema.version）', false, String(e.message ?? e));
+    finish();
+    return;
+  }
+
+  // ---- 阶段2 runtime（app 低权限：重启恢复 + 全操作断言 + 零 DDL；执行器由 R70 显式启停）----
+  console.log('[bpm001] 阶段2 runtime：app 低权限重启接入同库/审批操作/事务回滚/积压恢复…');
+  const runAssert = assertSurefire('BpmPgHarnessRuntimeTest', 9);
+  const runLog = await runAsync(
+    runHarnessTestArgs('BpmPgHarnessRuntimeTest'),
+    { env: harnessEnv('zhongshu_app', 'app_local_1', 'false', 'false'), timeoutMs: 20 * 60 * 1000 });
+  writeFileSync(join(outDir, 'runtime.log'), runLog.output);
+  let runOk = false, runNote = '';
+  try { runNote = runAssert(); runOk = runLog.status === 0; if (!runOk) runNote += `，但 mvn exit=${runLog.status}`; }
+  catch (e) { runNote = String(e.message ?? e); }
+  record('P2 runtime 重启恢复/通过/拒绝/撤回/转办/分页历史/事务回滚/积压恢复', runOk, runNote);
+  if (!runOk) {
+    console.error('[bpm001] runtime 阶段失败，日志见 outputs/bpm-001/runtime.log（尾部）：\n' + runLog.output.split('\n').slice(-40).join('\n'));
+  }
+
+  // ---- S2/S3：运行期零 DDL（表数量与 schema.version 不变）+ 探针落库 ----
+  {
+    let ok = false, note = '';
+    try {
+      const actAfterRuntime = countActFlw('act\\_%');
+      const versionAfterRuntime = schemaVersion();
+      ok = actAfterBootstrap === actAfterRuntime && versionAfterBootstrap === versionAfterRuntime;
+      note = `ACT_ ${actAfterBootstrap}→${actAfterRuntime}, schema.version ${versionAfterBootstrap}→${versionAfterRuntime}`;
+    } catch (e) { note = String(e.message ?? e); }
+    record('S2 运行期零DDL：引擎表数量与 schema.version 不变', ok, note);
+  }
+  {
+    let ok = false, note = '';
+    try {
+      const probeRows = psqlScalarInt('postgres', 'zhongshu', 'SELECT count(*) FROM bpm_harness_probe');
+      ok = probeRows >= 3;
+      note = `探针行=${probeRows}`;
+    } catch (e) { note = String(e.message ?? e); }
+    record('S3 异步回声探针落库（owner 引导版本 + app 运行期回声）', ok, note);
+  }
+
+  finish();
+})().catch((e) => { console.error('[bpm001] 编排器异常：' + String(e?.stack ?? e)); cleanup(); process.exit(1); });

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '$reviewPaths = @('"'E:\\AGENTS.md','E:\\zszj-wt-bpm-001-a\\AGENTS.md'); foreach ("'$reviewPath in $reviewPaths) { if (Test-Path -LiteralPath $reviewPath) { Get-Content -LiteralPath $reviewPath } }; rg --files --hidden -g AGENTS.md -g '"'"'!**/.git/**'"'; Get-ChildItem -Force outputs" in E:\zszj-wt-bpm-001-a
 succeeded in 10ms:
Mode                 LastWriteTime         Length Name                                                                 
----                 -------------         ------ ----                                                                 
d-----         2026/9/14      3:39                bpm-001                                                              



exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '$reviewLine = 0; Get-Content -LiteralPath scripts/db/run-bpm001-verify.mjs -Encoding utf8 | ForEach-Object { $reviewLine++; '"'{0,4}: {1}' -f "'$reviewLine, $_ }; $reviewLine = 0; Get-Content -LiteralPath .github/workflows/pg-regression.yml -Encoding utf8 | ForEach-Object { $reviewLine++; '"'{0,4}: {1}' -f "'$reviewLine, $_ }' in E:\zszj-wt-bpm-001-a
 succeeded in 0ms:
   1: /**
   2:  * ZS-BPM-001��BPM ����װ���� PostgreSQL ���ձ�������B09 ����׼���������� CI ͬһ���򣩡�
   3:  *
   4:  * ���׶����壨ͬһһ���� PG �����ڵ�ͬһ�����⣩��
   5:  *   �׶�1 bootstrap��zhongshu_owner��schema-update=true���첽ִ�������𣩣�
   6:  *     - �տ����� Flowable �����Խ� ACT_ ������������=Ǩ���� owner �˺ţ���
   7:  *     - �������Լ����оߣ��ظ������ݵȡ����������°汾������������ʵ���������첽��ѹ��
   8:  *   �׶�2 runtime��zhongshu_app ��Ȩ�ޣ�schema-update=false���첽ִ��������� R70 ��ʽ��ͣ����
   9:  *     - �� JVM ��������ͬ�⣨�����ָ�������/ʵ��/��ʷȫ���ɼ���schema �汾����=�������� DDL����
  10:  *     - ͨ��/�ܾ�/����/ת��/��ҳ��ʷ��Spring ����ع���������ۼ�����ѹ������ʽ��ͣ��ִ���������������š�
  11:  * �ṹ��飨������ psql ֱ֤������ Java��ȫ����ѯǿ�Ƴɹ���ʧ�ܼ� FAIL ���̴�����
  12:  *   S0 �ر� BPM �ṹ�������System/Infra ���ߣ�ȫ�� Flyway V* Ӧ�ú��� ACT_/FLW_ ��
  13:  *      ��������װ����֤��=���� V1.6 ��ʵ�������� + ModuleWhitelistTest ��̬�Ž����������ĵ��߽磩��
  14:  *   S1 ��Ȩ�������˺��� DDL��app ���ӿ��ã�SELECT 1 �ɹ����� CREATE TABLE �� SQLSTATE 42501 ���ܣ�
  15:  *   S2 ����������� schema.version ǰ��һ�£��׶�2 δ�Ľṹ����
  16:  *   S3 �첽����̽����⡣
  17:  * �����������Ȳֿ� tools/��worktree ������������ʹ�� PATH �ϵ� java/mvn��CI ���ɹ����� provision����
  18:  *   ���߽�ȱ���˳��� 3 ��ȷʧ�ܣ�����Ĭ�������׼�ǰ�� `mvn install` ����ģ��
  19:  *   ��zszj-dependencies BOM + system/infra ������������-Dmaven.test.skip=true������֤ BPM ����
  20:  *   �������ѵ��ǵ�ǰ�ύ�Ĳ��BPM ���� reactor��BOM ���ᾭ -am ���ݣ�����ʽ�г�����
  21:  *
  22:  * ���壨���� scripts/db/run-db018-verify.mjs / test-pg-fixture.mjs �о�Լ������
  23:  *   - ʵ�����룺���������Ψһ�����ǿ��������docker rm -f -v�����Ժ���֤�����ڣ���
  24:  *   - ʧ�ܷ��㣺��һ�׶�/���ʧ�� �� �����˳���
  25:  *   - ȱ��������Ĭ������Docker/������������ �� �˳��� 3��
  26:  *   - Docker ���� flaky����������ʧ���������м������˿�����һ�Ρ�
  27:  *   - �� Maven �׶����첽 spawn��SIGINT/SIGTERM ���¼�ѭ���ڿɴ�����������˳�
  28:  *     ��sync ��������⣻kill -9 ��ǿɱ�������أ������������ɻ�����������
  29:  * ֤�ݱ��棺JSON ժҪ�� outputs/bpm-001/runtime-report.json��outputs/ ����⣩��
  30:  *
  31:  * �÷���node scripts/db/run-bpm001-verify.mjs
  32:  */
  33: import { execFileSync, spawn, spawnSync } from 'node:child_process';
  34: import { readFileSync, existsSync, mkdirSync, writeFileSync, readdirSync, rmSync } from 'node:fs';
  35: import { fileURLToPath } from 'node:url';
  36: import { join } from 'node:path';
  37: 
  38: const root = fileURLToPath(new URL('../../', import.meta.url));
  39: const core = join(root, 'services', 'zhongshu-core');
  40: const migrationDir = join(core, 'zszj-server', 'src', 'main', 'resources', 'db', 'migration');
  41: const outDir = join(root, 'outputs', 'bpm-001');
  42: const isWin = process.platform === 'win32';
  43: 
  44: function fail(code, message) { console.error(message); process.exit(code); }
  45: 
  46: // ---- ������������tools/ ���ȣ�����/worktree�������� PATH��CI �ɹ����� provision������ȱ�˳��� 3 ----
  47: const toolsDir = join(root, 'tools');
  48: const jdkDir = join(toolsDir, 'jdk-17.0.20.1+1');
  49: const mavenBin = join(toolsDir, 'apache-maven-3.9.9', 'bin');
  50: let javaHomeEnv, pathPrefix;
  51: if (existsSync(jdkDir) && existsSync(mavenBin)) {
  52:   javaHomeEnv = jdkDir;
  53:   pathPrefix = isWin ? `${jdkDir}\\bin;${mavenBin};` : `${jdkDir}/bin:${mavenBin}:`;
  54: } else {
  55:   // Windows �� mvn �� .cmd���뾭 cmd.exe ���������ִ�з�ʽһ�£�������̽��� ENOENT ����ȱʧ
  56:   const probe = isWin ? spawnSync('cmd.exe', ['/d', '/s', '/c', 'mvn -v'], { encoding: 'utf8' })
  57:     : spawnSync('mvn', ['-v'], { encoding: 'utf8' });
  58:   const hasJava = isWin ? spawnSync('cmd.exe', ['/d', '/s', '/c', 'java -version'], { encoding: 'utf8' })
  59:     : spawnSync('java', ['-version'], { encoding: 'utf8' });
  60:   if (probe.error || probe.status !== 0 || hasJava.error || hasJava.status !== 0) {
  61:     fail(3, `[bpm001] ������ȱʧ���� tools/ ������ PATH ���޿��� mvn/java�������빩�� tools/��CI �� pg-regression.yml provision��`);
  62:   }
  63:   javaHomeEnv = process.env.JAVA_HOME;
  64:   pathPrefix = '';
  65: }
  66: const childEnv = {
  67:   ...process.env,
  68:   ...(javaHomeEnv ? { JAVA_HOME: javaHomeEnv } : {}),
  69:   PATH: `${pathPrefix}${process.env.PATH}`,
  70: };
  71: 
  72: // ---- Docker Ԥ�죨ȱ��������Ĭ������----
  73: const dockerUp = spawnSync('docker', ['version', '--format', '{{.Server.Version}}'], { encoding: 'utf8' });
  74: if (dockerUp.error || dockerUp.status !== 0) {
  75:   fail(3, `[bpm001] Docker �����ã�${dockerUp.error?.message ?? `exit=${dockerUp.status}`}������֤���þ�Ĭ����`);
  76: }
  77: 
  78: const container = `zszj-bpm001-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
  79: let port = 4332 + Math.floor(Math.random() * 700);
  80: let cleaned = false;
  81: let cleanupError = '';
  82: const cleanup = () => {
  83:   if (cleaned) return;
  84:   for (let attempt = 0; attempt < 3 && !cleaned; attempt++) {
  85:     const r = spawnSync('docker', ['rm', '-f', '-v', container], { encoding: 'utf8' });
  86:     if (r.status === 0) { cleaned = true; break; }
  87:     cleanupError = (r.stderr ?? '').trim();
  88:     Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500);
  89:   }
  90: };
  91: const onSignal = (signal) => { cleanup(); process.exit(signal === 'SIGINT' ? 130 : 143); };
  92: process.on('exit', () => { if (!cleaned) cleanup(); });
  93: process.on('SIGINT', () => onSignal('SIGINT'));
  94: process.on('SIGTERM', () => onSignal('SIGTERM'));
  95: 
  96: // ---- psql �ϸ��װ���κβ�ѯʧ��/�ս����������ʽ��¶�����̴� ----
  97: const psqlRun = (user, db, sql) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', user, '-d', db,
  98:   '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 });
  99: function psqlScalar(user, db, sql) {
 100:   const r = spawnSync('docker', ['exec', container, 'psql', '-U', user, '-d', db, '-At', '-c', sql],
 101:     { encoding: 'utf8', maxBuffer: 8 * 1024 * 1024 });
 102:   if (r.error || r.status !== 0) throw new Error(`psql ��ѯʧ�ܣ�exit=${r.status ?? 'ERR'}����${((r.stderr ?? '') || (r.stdout ?? '')).slice(0, 300)}`);
 103:   const value = (r.stdout ?? '').trim();
 104:   if (value === '') throw new Error(`psql ��ѯ���ؿգ�${sql.slice(0, 120)}`);
 105:   return value;
 106: }
 107: function psqlScalarInt(user, db, sql) {
 108:   const value = psqlScalar(user, db, sql);
 109:   const n = Number(value);
 110:   if (!Number.isInteger(n) || n < 0) throw new Error(`�����Ǹ�������ʵ�ʡ�${value}����${sql.slice(0, 120)}`);
 111:   return n;
 112: }
 113: 
 114: let pass = 0, failCount = 0;
 115: const results = [];
 116: const record = (id, ok, note = '') => { results.push({ id, ok, note }); ok ? pass++ : failCount++; console.log(`[${ok ? 'PASS' : 'FAIL'}] ${id} ${note}`); };
 117: 
 118: // ---- �� Maven �׶Σ��첽 spawn���¼�ѭ�����SIGINT/SIGTERM �ɴﲢ����������----
 119: // Windows �� Node spawn .cmd �� EINVAL �������뾭 cmd.exe
 120: function runAsync(argv, opts = {}) {
 121:   return new Promise((resolve) => {
 122:     const child = spawn(argv[0], argv.slice(1), { cwd: core, env: { ...childEnv, ...opts.env }, stdio: ['ignore', 'pipe', 'pipe'] });
 123:     let output = '';
 124:     child.stdout.on('data', (d) => { output += d; });
 125:     child.stderr.on('data', (d) => { output += d; });
 126:     const timer = opts.timeoutMs ? setTimeout(() => child.kill(), opts.timeoutMs) : null;
 127:     child.on('error', (e) => { if (timer) clearTimeout(timer); resolve({ status: -1, output: String(e) }); });
 128:     child.on('close', (code) => { if (timer) clearTimeout(timer); resolve({ status: code, output }); });
 129:   });
 130: }
 131: const mvnArgv = (mvnArgs) => isWin ? ['cmd.exe', '/d', '/s', '/c', 'mvn ' + mvnArgs.join(' ')] : ['mvn', ...mvnArgs];
 132: 
 133: // ---- ����һ���� PG������ flaky�������м�+���˿�����һ�Σ�----
 134: function dockerRunOnce() {
 135:   const args = ['run', '-d', '--name', container, '-e', 'POSTGRES_PASSWORD=bpm001', '-p', `127.0.0.1:${port}:5432`, 'postgres:17-alpine'];
 136:   const r = isWin ? spawnSync('cmd.exe', ['/d', '/s', '/c', 'docker ' + args.join(' ')], { encoding: 'utf8' })
 137:     : spawnSync('docker', args, { encoding: 'utf8' });
 138:   if (r.status === 0) return true;
 139:   console.error(`[bpm001] docker run ʧ�ܣ�port=${port}����${((r.stderr ?? '') || '').trim().slice(0, 200)}`);
 140:   return false;
 141: }
 142: 
 143: const reportXml = (testClass) => join(core, 'zszj-module-bpm', 'target', 'surefire-reports',
 144:   `TEST-cn.zszj.module.bpm.harness.${testClass}.xml`);
 145: const countActFlw = (pattern) => psqlScalarInt('postgres', 'zhongshu',
 146:   `SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND lower(table_name) LIKE '${pattern}'`);
 147: const schemaVersion = () => psqlScalar('postgres', 'zhongshu', "SELECT VALUE_ FROM ACT_GE_PROPERTY WHERE NAME_='schema.version'");
 148: 
 149: // surefire �ϸ�У�飺��ɾ���ֱ��棨���¾��ļ����ã����ܺ�Ҫ����ڡ�����ƥ�䡢
 150: // ��������ȷ����������ʧ�������
 151: function assertSurefire(testClass, expectedTests) {
 152:   const xml = reportXml(testClass);
 153:   rmSync(xml, { force: true });
 154:   return () => {
 155:     if (!existsSync(xml)) throw new Error(`���� surefire ����δ���ɣ�${xml}`);
 156:     const text = readFileSync(xml, 'utf8');
 157:     const attr = (name) => (text.match(new RegExp(`${name}="(\\d+)"`)) ?? [])[1];
 158:     const suiteName = (text.match(/name="([^"]+)"/) ?? [])[1];
 159:     if (suiteName !== `cn.zszj.module.bpm.harness.${testClass}`) throw new Error(`�������ݲ�����${suiteName}`);
 160:     const tests = Number(attr('tests')), failures = Number(attr('failures')),
 161:       errors = Number(attr('errors')), skipped = Number(attr('skipped'));
 162:     if (tests !== expectedTests) throw new Error(`������ ${tests} �� Ԥ�� ${expectedTests}`);
 163:     if (skipped !== 0) throw new Error(`������������ ${skipped}���ز⼯����������`);
 164:     if (failures !== 0 || errors !== 0) throw new Error(`failures=${failures} errors=${errors}`);
 165:     return `tests=${tests} failures=0 errors=0 skipped=0`;
 166:   };
 167: }
 168: 
 169: function runHarnessTestArgs(testClass, username, password, schemaUpdate, asyncExecutor) {
 170:   return mvnArgv(['-B', '-f', 'zszj-module-bpm/pom.xml', 'test', `-Dtest=${testClass}`]);
 171: }
 172: function harnessEnv(username, password, schemaUpdate, asyncExecutor) {
 173:   return {
 174:     ZSZJ_BPM_HARNESS_JDBC_URL: `jdbc:postgresql://127.0.0.1:${port}/zhongshu`,
 175:     ZSZJ_BPM_HARNESS_USERNAME: username,
 176:     ZSZJ_BPM_HARNESS_PASSWORD: password,
 177:     ZSZJ_BPM_HARNESS_SCHEMA_UPDATE: schemaUpdate,
 178:     ZSZJ_BPM_HARNESS_ASYNC_EXECUTOR: asyncExecutor,
 179:   };
 180: }
 181: 
 182: function finish() {
 183:   cleanup();
 184:   if (!cleaned) record('S4 �о���������', false, `docker rm ���Ժ���ʧ�ܣ�${cleanupError}`);
 185:   console.log('\n===== ZS-BPM-001 BPM ����װ���� PG ���ջ��� =====');
 186:   for (const r of results) console.log(`${r.ok ? 'PASS' : 'FAIL'}  ${r.id} ${r.note}`);
 187:   console.log(`�ϼ� ${results.length} �ʧ�� ${failCount} ��`);
 188:   try {
 189:     writeFileSync(join(outDir, 'runtime-report.json'),
 190:       JSON.stringify({ finishedAt: new Date().toISOString(), container, results, pass, failCount }, null, 2));
 191:   } catch { /* ��������ʧ�ܲ�Ӱ���˳������� */ }
 192:   process.exit(failCount === 0 ? 0 : 1);
 193: }
 194: 
 195: (async () => {
 196:   console.log(`[bpm001] ������ʱ PG��${container} @ 127.0.0.1:${port}����`);
 197:   if (!dockerRunOnce()) {
 198:     spawnSync('docker', ['rm', '-f', '-v', container], { stdio: 'ignore' });
 199:     port = 4332 + Math.floor(Math.random() * 700);
 200:     console.log(`[bpm001] ���ԣ��¶˿� ${port}`);
 201:     if (!dockerRunOnce()) fail(3, '[bpm001] PG �о�������������ʧ�ܣ�Docker ���ؾ�̬��Լ������һ�κ���ʧ�ܣ�');
 202:   }
 203: 
 204:   let ready = false;
 205:   // ����̽������� TCP��-h 127.0.0.1����initdb �ڼ����ʱ������ֻ���� unix socket��
 206:   // socket ̽��������о������� CREATE DATABASE �䵽��ʱ����ʧ��
 207:   for (let i = 0; i < 40; i++) {
 208:     const r = spawnSync('docker', ['exec', container, 'psql', '-h', '127.0.0.1', '-U', 'postgres', '-d', 'postgres', '-At', '-c', 'SELECT 1'], { encoding: 'utf8' });
 209:     if (r.status === 0) { ready = true; break; }
 210:     Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500);
 211:   }
 212:   if (!ready) { cleanup(); fail(1, '[bpm001] PG δ����'); }
 213: 
 214:   // ---- ���߿⣺���� + ��ɫ��Ȩ + ȫ�� Flyway Ǩ�� ----
 215:   {
 216:     let created = false, last = '';
 217:     for (let i = 0; i < 10 && !created; i++) {
 218:       const r = psqlRun('postgres', 'postgres', 'CREATE DATABASE zhongshu;');
 219:       created = r.status === 0;
 220:       if (!created) { last = ((r.stderr ?? '') + (r.stdout ?? '')).trim(); Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 1000); }
 221:     }
 222:     if (!created) { cleanup(); fail(1, `[bpm001] ����ʧ�ܣ����� 10 �Σ���${last.slice(0, 400)}`); }
 223:   }
 224:   {
 225:     const setup = readFileSync(join(core, 'sql', 'postgresql', 'env-setup-test.sql'), 'utf8');
 226:     const r = psqlRun('postgres', 'zhongshu', setup);
 227:     if (r.status !== 0) { cleanup(); fail(1, `[bpm001] ��ɫ��Ȩʧ��:\n${r.stdout}${r.stderr}`); }
 228:   }
 229:   {
 230:     const migrations = readdirSync(migrationDir).filter((f) => /^V.*\.sql$/.test(f)).sort();
 231:     const bundled = migrations.map((f) => readFileSync(join(migrationDir, f), 'utf8')).join('\n');
 232:     const r = psqlRun('zhongshu_owner', 'zhongshu', bundled);
 233:     if (r.status !== 0) { cleanup(); fail(1, `[bpm001] ����Ǩ��ִ��ʧ�ܣ�${migrations.length} �� V*��:\n${r.stdout}${r.stderr}`); }
 234:     console.log(`[bpm001] ��Ӧ�û���Ǩ�� ${migrations.length} ����${migrations.join(', ')}`);
 235:   }
 236:   if (psqlRun('zhongshu_owner', 'zhongshu',
 237:     'CREATE TABLE bpm_harness_probe(id bigserial PRIMARY KEY, note text NOT NULL, created_at timestamptz NOT NULL DEFAULT now());').status !== 0) {
 238:     cleanup(); fail(1, '[bpm001] ̽�������ʧ��');
 239:   }
 240: 
 241:   // ---- ����ģ����ﰲװ��BPM ������������ reactor�����ѱ��زֿ���������Ե�ǰ�ύ��
 242:   //      zszj-dependencies BOM ���ᾭ -am ���ݣ�������ʽ���룩----
 243:   console.log('[bpm001] mvn install ����ģ�飨BOM+system/infra �����������������Ա��룩��');
 244:   const install = await runAsync(
 245:     mvnArgv(['-B', '-pl', 'zszj-dependencies,zszj-module-system,zszj-module-infra', '-am', 'install', '-Dmaven.test.skip=true']),
 246:     { timeoutMs: 20 * 60 * 1000 });
 247:   if (install.status !== 0) {
 248:     cleanup();
 249:     fail(1, '[bpm001] ����ģ�鰲װʧ�ܣ�\n' + install.output.split('\n').slice(-25).join('\n'));
 250:   }
 251: 
 252:   // ---- S0���ر� BPM �ṹ���������System/Infra ȫ���������̱�����ѯʧ�ܼ� FAIL��----
 253:   let actAfterBootstrap = -1, versionAfterBootstrap = '';
 254:   try {
 255:     const act = countActFlw('act\\_%'), flw = countActFlw('flw\\_%');
 256:     record('S0 �ر�BPM�ṹ������������� ACT_/FLW_ ��', act === 0 && flw === 0, `ACT_=${act} FLW_=${flw}`);
 257:   } catch (e) {
 258:     record('S0 �ر�BPM�ṹ������������� ACT_/FLW_ ��', false, String(e.message ?? e));
 259:   }
 260: 
 261:   // ---- S1����Ȩ�������˺����ӿ������� DDL������Ȩ�޾ܾ� SQLSTATE 42501 �������ʩ����----
 262:   // psql Ĭ�� verbosity ������ SQLSTATE���� verbose ���ܶ� 42501 ����ȷ�ж���PG15+ ��������Ϊ schema ����
 263:   {
 264:     let ok = false, note = '';
 265:     try {
 266:       psqlScalar('zhongshu_app', 'zhongshu', 'SELECT 1');
 267:       const r = spawnSync('docker', ['exec', '-i', container, 'psql', '-U', 'zhongshu_app', '-d', 'zhongshu',
 268:         '-v', 'ON_ERROR_STOP=1', '-q'], { input: '\\set VERBOSITY verbose\nCREATE TABLE zszj_app_should_fail(id int);', encoding: 'utf8' });
 269:       const output = ((r.stderr ?? '') + (r.stdout ?? '')).trim();
 270:       if (r.status === 0) { note = 'app ��Ȼ�����ɹ�'; }
 271:       else if (output.includes('42501')) { ok = true; note = '������ SQLSTATE 42501��Ȩ�޲��㣩���ܣ�app ���ӱ�������'; }
 272:       else { note = `����ʧ�ܵ���Ȩ�޾ܾ���${output.slice(0, 160)}`; }
 273:     } catch (e) { note = String(e.message ?? e); }
 274:     record('S1 �����˺���DDL��app ���ӿ����ҽ����� 42501 �ܾ�', ok, note);
 275:   }
 276: 
 277:   // ---- �׶�1 bootstrap��owner �˺ţ����� + ���� + ���� + ����ѹ��----
 278:   console.log('[bpm001] �׶�1 bootstrap��owner �˺Ž������/�������Լо�/����ʵ��/���첽��ѹ��');
 279:   mkdirSync(outDir, { recursive: true });
 280:   const bootAssert = assertSurefire('BpmPgHarnessBootstrapTest', 5);
 281:   const bootLog = await runAsync(
 282:     runHarnessTestArgs('BpmPgHarnessBootstrapTest'),
 283:     { env: harnessEnv('zhongshu_owner', 'owner_local_1', 'true', 'false'), timeoutMs: 30 * 60 * 1000 });
 284:   writeFileSync(join(outDir, 'bootstrap.log'), bootLog.output);
 285:   let bootOk = false, bootNote = '';
 286:   try { bootNote = bootAssert(); bootOk = bootLog.status === 0; if (!bootOk) bootNote += `���� mvn exit=${bootLog.status}`; }
 287:   catch (e) { bootNote = String(e.message ?? e); }
 288:   record('P1 bootstrap ���潨��/�����ݵ�/����/�⻧��ǩ/�汾��ס', bootOk, bootNote);
 289:   if (!bootOk) {
 290:     console.error('[bpm001] bootstrap �׶�ʧ�ܣ���־�� outputs/bpm-001/bootstrap.log��β������\n' + bootLog.output.split('\n').slice(-40).join('\n'));
 291:     finish();
 292:     return;
 293:   }
 294: 
 295:   try {
 296:     actAfterBootstrap = countActFlw('act\\_%');
 297:     versionAfterBootstrap = schemaVersion();
 298:   } catch (e) {
 299:     // ������ṹ��ȡʧ�ܱ���� FAIL ����β������α��
 300:     record('P1b ������ṹ��ȡ��������/schema.version��', false, String(e.message ?? e));
 301:     finish();
 302:     return;
 303:   }
 304: 
 305:   // ---- �׶�2 runtime��app ��Ȩ�ޣ������ָ� + ȫ�������� + �� DDL��ִ������ R70 ��ʽ��ͣ��----
 306:   console.log('[bpm001] �׶�2 runtime��app ��Ȩ����������ͬ��/��������/����ع�/��ѹ�ָ���');
 307:   const runAssert = assertSurefire('BpmPgHarnessRuntimeTest', 9);
 308:   const runLog = await runAsync(
 309:     runHarnessTestArgs('BpmPgHarnessRuntimeTest'),
 310:     { env: harnessEnv('zhongshu_app', 'app_local_1', 'false', 'false'), timeoutMs: 20 * 60 * 1000 });
 311:   writeFileSync(join(outDir, 'runtime.log'), runLog.output);
 312:   let runOk = false, runNote = '';
 313:   try { runNote = runAssert(); runOk = runLog.status === 0; if (!runOk) runNote += `���� mvn exit=${runLog.status}`; }
 314:   catch (e) { runNote = String(e.message ?? e); }
 315:   record('P2 runtime �����ָ�/ͨ��/�ܾ�/����/ת��/��ҳ��ʷ/����ع�/��ѹ�ָ�', runOk, runNote);
 316:   if (!runOk) {
 317:     console.error('[bpm001] runtime �׶�ʧ�ܣ���־�� outputs/bpm-001/runtime.log��β������\n' + runLog.output.split('\n').slice(-40).join('\n'));
 318:   }
 319: 
 320:   // ---- S2/S3���������� DDL���������� schema.version ���䣩+ ̽����� ----
 321:   {
 322:     let ok = false, note = '';
 323:     try {
 324:       const actAfterRuntime = countActFlw('act\\_%');
 325:       const versionAfterRuntime = schemaVersion();
 326:       ok = actAfterBootstrap === actAfterRuntime && versionAfterBootstrap === versionAfterRuntime;
 327:       note = `ACT_ ${actAfterBootstrap}��${actAfterRuntime}, schema.version ${versionAfterBootstrap}��${versionAfterRuntime}`;
 328:     } catch (e) { note = String(e.message ?? e); }
 329:     record('S2 ��������DDL������������� schema.version ����', ok, note);
 330:   }
 331:   {
 332:     let ok = false, note = '';
 333:     try {
 334:       const probeRows = psqlScalarInt('postgres', 'zhongshu', 'SELECT count(*) FROM bpm_harness_probe');
 335:       ok = probeRows >= 3;
 336:       note = `̽����=${probeRows}`;
 337:     } catch (e) { note = String(e.message ?? e); }
 338:     record('S3 �첽����̽����⣨owner �����汾 + app �����ڻ�����', ok, note);
 339:   }
 340: 
 341:   finish();
 342: })().catch((e) => { console.error('[bpm001] �������쳣��' + String(e?.stack ?? e)); cleanup(); process.exit(1); });
   1: name: pg-regression
   2: 
   3: # ZS-DB-019.B �ػ���PG �����ع��׼���һ���� PG17 ����������Զ���������
   4: # �뱾�� `node scripts/db/run-pg-regression.mjs` ͬһ����
   5: # ���ǣ�ZS-DB-006 ����/����/���š�007 �ֶ�ӳ�����߼�ɾ����008 ����������
   6: #       CFG-002.A �ֵ�ΨһԼ����DB-011~015 �н�ɾ����DB-016/017 �ṹ����֤��
   7: #       ZS-BPM-001 BPM ����װ���� PG ���գ����׶Σ���
   8: 
   9: on:
  10:   push:
  11:     branches: [main]
  12:   pull_request:
  13: 
  14: jobs:
  15:   pg-regression:
  16:     runs-on: ubuntu-latest
  17:     steps:
  18:       - uses: actions/checkout@v4
  19:       - uses: actions/setup-node@v4
  20:         with:
  21:           node-version: 22
  22:       - uses: actions/setup-java@v4
  23:         with:
  24:           distribution: temurin
  25:           java-version: 17
  26:       # ZS-BPM-001 �� 9 �׼���BPM �����������ѱ��زֿ������Ȱ�װ��ǰ�ύ������ģ�飻
  27:       # zszj-dependencies BOM ���ᾭ -am ���ݣ�������ʽ���루���� Non-resolvable import POM��
  28:       # ���� reactor ���� BPM ע��̬=�ر�ģ�飬ModuleWhitelistTest �Ž����䣩
  29:       - name: ��װ����ģ�����
  30:         run: mvn -B -f services/zhongshu-core/pom.xml -pl zszj-dependencies,zszj-module-system,zszj-module-infra -am install -Dmaven.test.skip=true
  31:       - name: ���� PG �����ع��׼�
  32:         run: node scripts/db/run-pg-regression.mjs

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'Get-Content -LiteralPath services/zhongshu-core/pom.xml -Encoding utf8; Get-Content -LiteralPath services/zhongshu-core/zszj-module-bpm/pom.xml -Encoding utf8; Get-Content -LiteralPath services/zhongshu-core/zszj-dependencies/pom.xml -Encoding utf8 -TotalCount 90; Get-Content -LiteralPath scripts/db/run-pg-regression.mjs -Encoding utf8' in E:\zszj-wt-bpm-001-a
 succeeded in 0ms:
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>cn.zszj</groupId>
    <artifactId>zszj</artifactId>
    <version>${revision}</version>
    <packaging>pom</packaging>
    <modules>
        <module>zszj-dependencies</module>
        <module>zszj-framework</module>
        <!-- Server ����Ŀ -->
        <module>zszj-server</module>
        <!-- ���� module ��չ -->
        <module>zszj-module-system</module>
        <module>zszj-module-infra</module>
<!--        <module>zszj-module-member</module>-->
<!--        <module>zszj-module-bpm</module>-->
<!--        <module>zszj-module-report</module>-->
<!--        <module>zszj-module-mp</module>-->
<!--        <module>zszj-module-pay</module>-->
<!--        <module>zszj-module-mall</module>-->
<!--        <module>zszj-module-crm</module>-->
<!--        <module>zszj-module-erp</module>-->
<!--        <module>zszj-module-iot</module>-->
<!--        <module>zszj-module-mes</module>-->
<!--        <module>zszj-module-wms</module>-->
<!--        <module>zszj-module-hrm</module>-->
<!--        <module>zszj-module-fms</module>-->
<!--        <module>zszj-module-pms</module>-->
<!--        <module>zszj-module-im</module>-->
<!-- ��ο� https://doc.iocoder.cn/ai/build/ �ĵ������ AI ģ������������� -->
<!--        <module>zszj-module-ai</module>-->
    </modules>

    <name>${project.artifactId}</name>
    <description>����֮����Ŀ�������ּ�</description>
    <url>https://github.com/YunaiV/ruoyi-vue-pro</url>

    <properties>
        <revision>2026.08-SNAPSHOT</revision>
        <!-- Maven ��� -->
        <java.version>17</java.version>
        <maven.compiler.source>${java.version}</maven.compiler.source>
        <maven.compiler.target>${java.version}</maven.compiler.target>
        <maven-surefire-plugin.version>3.5.3</maven-surefire-plugin.version>
        <maven-compiler-plugin.version>3.14.0</maven-compiler-plugin.version>
        <flatten-maven-plugin.version>1.7.2</flatten-maven-plugin.version>
        <!-- maven-surefire-plugin ��ʱ�޷�ͨ�� bom ��������ȡ�������ϰ汾 IDEA 2024 ����ǰ�汾�� -->
        <lombok.version>1.18.42</lombok.version>
        <spring.boot.version>3.5.15</spring.boot.version>
        <mapstruct.version>1.6.3</mapstruct.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>cn.zszj</groupId>
                <artifactId>zszj-dependencies</artifactId>
                <version>${revision}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <!-- maven-surefire-plugin ������������е�Ԫ���ԡ� -->
                <!-- ע�⣬��Ҫʹ�� 3.0.X+����ΪҪ֧�� Junit 5 �汾 -->
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <version>${maven-surefire-plugin.version}</version>
                </plugin>
                <!-- maven-compiler-plugin �������� spring-boot-configuration-processor + Lombok + MapStruct ��� -->
                <!-- https://stackoverflow.com/questions/33483697/re-run-spring-boot-configuration-annotation-processor-to-update-generated-metada -->
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>${maven-compiler-plugin.version}</version>
                    <configuration>
                        <annotationProcessorPaths>
                            <path>
                                <groupId>org.springframework.boot</groupId>
                                <artifactId>spring-boot-configuration-processor</artifactId>
                                <version>${spring.boot.version}</version>
                            </path>
                            <path>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok</artifactId>
                                <version>${lombok.version}</version>
                            </path>
                            <path>
                                <!-- ȷ�� Lombok ���ɵ� getter/setter �����ܱ� MapStruct ��ȷʶ��
                                     ������� No property named ��xxx" exists �ı������ -->
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok-mapstruct-binding</artifactId>
                                <version>0.2.0</version>
                            </path>
                            <path>
                                <groupId>org.mapstruct</groupId>
                                <artifactId>mapstruct-processor</artifactId>
                                <version>${mapstruct.version}</version>
                            </path>
                        </annotationProcessorPaths>
                        <!-- �������д�� arg �ڣ���� Spring Boot 3.2 �� Parameter Name Discovery ���� -->
                        <debug>false</debug>
                        <compilerArgs>
                            <arg>-parameters</arg>
                        </compilerArgs>
                    </configuration>
                </plugin>
                <plugin>
                    <groupId>org.codehaus.mojo</groupId>
                    <artifactId>flatten-maven-plugin</artifactId>
                </plugin>
            </plugins>
        </pluginManagement>

        <plugins>
            <!-- ZS-ENG-002��ǿ�� JDK 17 / Maven 3.8+ �������ߣ�D-02 ��ȷ�� JDK 17 ά���ߣ���
                 ������������ʱ������ȷʧ�ܣ�������������ʽ�汾 -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-enforcer-plugin</artifactId>
                <version>3.5.0</version>
                <executions>
                    <execution>
                        <id>enforce-jdk17-baseline</id>
                        <goals>
                            <goal>enforce</goal>
                        </goals>
                        <configuration>
                            <rules>
                                <requireJavaVersion>
                                    <version>[17,18)</version>
                                    <message>�������̶� JDK 17 �������ߣ�D-02������ʹ�� JDK 17 ������</message>
                                </requireJavaVersion>
                                <requireMavenVersion>
                                    <version>[3.8,)</version>
                                    <message>������Ҫ�� Maven 3.8 �����ϰ汾��</message>
                                </requireMavenVersion>
                            </rules>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            <!-- ͳһ revision �汾 -->
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>flatten-maven-plugin</artifactId>
                <version>${flatten-maven-plugin.version}</version>
                <configuration>
                    <flattenMode>oss</flattenMode>
                    <updatePomFile>true</updatePomFile>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>flatten</goal>
                        </goals>
                        <id>flatten</id>
                        <phase>process-resources</phase>
                    </execution>
                    <execution>
                        <goals>
                            <goal>clean</goal>
                        </goals>
                        <id>flatten.clean</id>
                        <phase>clean</phase>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

    <!-- ʹ�� huawei / aliyun �� Maven Դ�����������ٶ� -->
    <repositories>
        <repository>
            <id>huaweicloud</id>
            <name>huawei</name>
            <url>https://mirrors.huaweicloud.com/repository/maven/</url>
        </repository>
        <repository>
            <id>aliyunmaven</id>
            <name>aliyun</name>
            <url>https://maven.aliyun.com/repository/public</url>
        </repository>
    </repositories>

</project>
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>zszj</artifactId>
        <groupId>cn.zszj</groupId>
        <version>${revision}</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>zszj-module-bpm</artifactId>
    <packaging>jar</packaging>

    <name>${project.artifactId}</name>
    <description>
        bpm ���£�ҵ�����̹�����Business Process Management�������ǷŹ������Ĺ��ܡ�
        ����˵�����̶��塢�������á�������ģ��ҵ����롢�ҵĴ��졢�ҵ��Ѱ죩�ȵ�
        bpm ���ͣ�https://baike.baidu.com/item/BPM/1933

        ���������� Flowable 6 ʵ�֣��ֳ����̶��塢���̱���������ʵ������������ȹ���ģ�顣
    </description>

    <dependencies>
        <dependency>
            <groupId>cn.zszj</groupId>
            <artifactId>zszj-module-system</artifactId>
            <version>${revision}</version>
        </dependency>

        <!-- ҵ����� -->
        <dependency>
            <groupId>cn.zszj</groupId>
            <artifactId>zszj-spring-boot-starter-biz-data-permission</artifactId>
        </dependency>
        <dependency>
            <groupId>cn.zszj</groupId>
            <artifactId>zszj-spring-boot-starter-biz-tenant</artifactId>
        </dependency>

        <!-- Web ��� -->
        <dependency>
            <groupId>cn.zszj</groupId>
            <artifactId>zszj-spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>cn.zszj</groupId>
            <artifactId>zszj-spring-boot-starter-security</artifactId>
        </dependency>

        <!-- DB ��� -->
        <dependency>
            <groupId>cn.zszj</groupId>
            <artifactId>zszj-spring-boot-starter-mybatis</artifactId>
        </dependency>

        <!-- Test ������� -->
        <dependency>
            <groupId>cn.zszj</groupId>
            <artifactId>zszj-spring-boot-starter-test</artifactId>
        </dependency>

        <!-- ��������� -->
        <dependency>
            <groupId>cn.zszj</groupId>
            <artifactId>zszj-spring-boot-starter-excel</artifactId>
        </dependency>

        <!-- Flowable ��������� -->
        <dependency>
            <groupId>org.flowable</groupId>
            <artifactId>flowable-spring-boot-starter-process</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flowable</groupId>
            <artifactId>flowable-spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- ZS-BPM-001��PG ���ɼо�������������������ģ������ʱ��Я�� JDBC ������������ server ��ͳһ���룩 -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>cn.zszj</groupId>
    <artifactId>zszj-dependencies</artifactId>
    <version>${revision}</version>
    <packaging>pom</packaging>

    <name>${project.artifactId}</name>
    <description>���� bom �ļ�������������Ŀ�������汾</description>
    <url>https://github.com/YunaiV/ruoyi-vue-pro</url>

    <properties>
        <revision>2026.08-SNAPSHOT</revision>
        <flatten-maven-plugin.version>1.7.2</flatten-maven-plugin.version>
        <!-- ͳһ�������� -->
        <spring.boot.version>3.5.15</spring.boot.version>
        <!-- Web ��� -->
        <springdoc.version>2.8.17</springdoc.version>
        <knife4j.version>4.5.0</knife4j.version>
        <!-- DB ��� -->
        <druid.version>1.2.28</druid.version>
        <mybatis.version>3.5.19</mybatis.version>
        <mybatis-plus.version>3.5.17</mybatis-plus.version>
        <mybatis-plus-join.version>1.5.9</mybatis-plus-join.version>
        <dynamic-datasource.version>4.5.0</dynamic-datasource.version>
        <easy-trans.version>3.1.8</easy-trans.version>
        <redisson.version>4.7.0</redisson.version>
        <dm8.jdbc.version>8.1.3.140</dm8.jdbc.version>
        <kingbase.jdbc.version>9.0.1.jre7</kingbase.jdbc.version>
        <opengauss.jdbc.version>7.0.0-RC3-og</opengauss.jdbc.version>
        <taos.version>3.9.0</taos.version>
        <!-- ��Ϣ���� -->
        <rocketmq-spring.version>2.3.6</rocketmq-spring.version>
        <!-- ��������� -->
        <lock4j.version>2.2.7</lock4j.version>
        <!-- ������ -->
        <skywalking.version>9.6.0</skywalking.version>
        <spring-boot-admin.version>3.5.9</spring-boot-admin.version>
        <opentelemetry.version>1.65.0</opentelemetry.version>
        <!-- Test ������� -->
        <h2.version>2.3.232</h2.version>
        <podam.version>8.0.2.RELEASE</podam.version>
        <jedis-mock.version>1.1.18</jedis-mock.version>
        <mockito-inline.version>5.2.0</mockito-inline.version>
        <!-- Bpm ��������� -->
        <flowable.version>8.0.0</flowable.version>
        <!-- ��������� -->
        <anji-plus-captcha.version>1.4.0</anji-plus-captcha.version>
        <jsoup.version>1.23.2</jsoup.version>
        <sensitive-word.version>0.29.5</sensitive-word.version>
        <pinyin4j.version>2.5.1</pinyin4j.version>
        <lombok.version>1.18.46</lombok.version>
        <mapstruct.version>1.6.3</mapstruct.version>
        <hutool-5.version>5.8.47</hutool-5.version>
        <hutool-6.version>6.0.0-M22</hutool-6.version>
        <fastexcel.version>1.3.0</fastexcel.version>
        <aviator.version>5.4.4</aviator.version>
        <velocity.version>2.4.1</velocity.version>
        <fastjson2.version>2.0.64</fastjson2.version>
        <guava.version>33.7.1-jre</guava.version>
        <transmittable-thread-local.version>2.14.5</transmittable-thread-local.version>
        <commons-net.version>3.13.0</commons-net.version>
        <commons-lang3.version>3.20.0</commons-lang3.version>
        <jsch.version>2.28.7</jsch.version>
        <tika-core.version>3.3.1</tika-core.version>
        <ip2region.version>2.7.0</ip2region.version>
        <bizlog-sdk.version>3.0.6</bizlog-sdk.version>
        <netty.version>4.2.17.Final</netty.version>
        <mqtt.version>1.2.5</mqtt.version>
        <vertx.version>4.5.26</vertx.version>
        <okhttp.version>4.12.0</okhttp.version>
        <californium.version>3.14.0</californium.version>
        <j2mod.version>3.3.0</j2mod.version>
        <!-- �����Ʒ������ -->
        <awssdk.version>2.54.7</awssdk.version>
        <justauth.version>1.16.7</justauth.version>
        <justauth-starter.version>1.4.0</justauth-starter.version>
        <jimureport.version>2.5.1</jimureport.version>
        <jimubi.version>2.5.0</jimubi.version>
        <weixin-java.version>4.8.6-20260825.155844</weixin-java.version>
        <bouncycastle.version>1.80</bouncycastle.version>
        <alipay-sdk-java.version>4.40.978.ALL</alipay-sdk-java.version>
    </properties>

    <dependencyManagement>
        <dependencies>
/**
 * ZS-DB-019.B �ػ���PG �����ع�ۺ���ڣ������� CI ͬһ���򣩡�
 *
 * ����ִ��ȫ����ʵ PG ��֤�׼���ÿ�׼��Թ���һ���������������������
 *   1. ZS-DB-006 ��������/����/����/�ع�/��Ȩ�ޣ�6 ������
 *   2. ZS-DB-007 �ֶ�ӳ�����߼�ɾ����ͬ��7 ������
 *   3. ZS-DB-008 ����ԭ����������Ϊ��6 ������
 *   4. ZS-CFG-002.A �ֵ����ΨһԼ����8 ������
 *   5. ZS-DB-011~015 �н�ɾ����5 �� �� �н�/�߽�/�ռ���
 *   6. ZS-DB-018 ORM/��д SQL/�⻧���루�������⻧ CRUD/��ҳ/����/����/�߼�ɾ�� + ȫ�ֱ�/����ע��/ϵͳ�����Ϸ���Χ��
 *   7. DB-016 Quartz ���ȱ��ṹ����֤��ί�к�����䣩
 *   8. DB-017 PG Ԫ���ݲ��Ա���ע��/�����˶�
 *   9. ZS-BPM-001 BPM ����װ���� PG ���գ����׶Σ�owner �������� + app ��Ȩ�����У�
 * ��һ�׼�ʧ���˳����㡣
 * �÷���node scripts/db/run-pg-regression.mjs
 */
import { execFileSync, spawnSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));
const cases = [
  { id: 'ZS-DB-006 ����/����/����', cmd: ['node', 'scripts/db/run-db006-verify.mjs'] },
  { id: 'ZS-DB-007 �ֶ�ӳ�����߼�ɾ��', cmd: ['node', 'scripts/db/run-db007-verify.mjs'] },
  { id: 'ZS-DB-008 ��������', cmd: ['node', 'scripts/db/run-db008-verify.mjs'] },
  { id: 'ZS-CFG-002.A �ֵ�Լ��', cmd: ['node', 'scripts/db/run-cfg002-verify.mjs'] },
  { id: 'ZS-DB-011~015 �н�ɾ��', cmd: ['node', 'scripts/db/run-db011-015-verify.mjs'] },
  { id: 'ZS-DB-018 ORM/��дSQL/�⻧����', cmd: ['node', 'scripts/db/run-db018-verify.mjs'] },
  { id: 'DB-016 Quartz ���ȱ��ṹ����֤', caseFile: 'scripts/db/cases/db016-quartz-schema.sql' },
  { id: 'DB-017 Ԫ���ݲ��Ա�', caseFile: 'scripts/db/cases/db017-metadata-testtable.sql' },
  { id: 'ZS-BPM-001 BPM ����װ����PG����', cmd: ['node', 'scripts/db/run-bpm001-verify.mjs'] },
];

let failed = false;
const summary = [];
for (const c of cases) {
  let ok = true, note = '';
  try {
    if (c.caseFile) {
      // DB-016/017���������� + V1/V2 + ���� SQL
      const port = 5532 + Math.floor(Math.random() * 400);
      const container = `zszj-pgreg-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
      const boot = (sql) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', 'postgres', '-d', 'zhongshu', '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql, encoding: 'utf8', maxBuffer: 32 * 1024 * 1024 });
      execFileSync('docker', ['run', '-d', '--name', container, '-e', 'POSTGRES_PASSWORD=pgreg', '-p', `127.0.0.1:${port}:5432`, 'postgres:17-alpine'], { stdio: 'ignore' });
      try {
        let ready = false;
        for (let i = 0; i < 30; i++) { const r = spawnSync('docker', ['exec', container, 'psql', '-U', 'postgres', '-At', '-c', 'SELECT 1'], { encoding: 'utf8' }); if (r.status === 0) { ready = true; break; } Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500); }
        if (!ready) throw new Error('PG δ����');
        execFileSync('docker', ['exec', container, 'psql', '-U', 'postgres', '-q', '-c', 'CREATE DATABASE zhongshu;'], { stdio: 'ignore' });
        const setup = readFileSync(join(root, 'services/zhongshu-core/sql/postgresql/env-setup-test.sql'), 'utf8');
        const s1 = spawnSync('docker', ['exec', '-i', container, 'psql', '-U', 'postgres', '-d', 'zhongshu', '-v', 'ON_ERROR_STOP=1', '-q'], { input: setup, encoding: 'utf8' });
        if (s1.status !== 0) throw new Error('��ɫ��Ȩʧ��');
        const v1 = readFileSync(join(root, 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260909.001__system_infra_baseline.sql'), 'utf8');
        const v2 = readFileSync(join(root, 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260909.002__infra_quartz_backfill.sql'), 'utf8');
        const s2 = spawnSync('docker', ['exec', '-i', container, 'psql', '-U', 'zhongshu_owner', '-d', 'zhongshu', '-v', 'ON_ERROR_STOP=1', '-q'], { input: v1 + '\n' + v2, encoding: 'utf8', maxBuffer: 16 * 1024 * 1024 });
        if (s2.status !== 0) throw new Error('V1/V2 ִ��ʧ��');
        const sql = readFileSync(join(root, c.caseFile), 'utf8');
        const s3 = spawnSync('docker', ['exec', '-i', container, 'psql', '-U', 'zhongshu_owner', '-d', 'zhongshu', '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql, encoding: 'utf8' });
        if (s3.status !== 0) throw new Error('����ִ��ʧ��');
      } finally {
        try { execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { }
      }
    } else {
      const r = spawnSync(c.cmd[0], c.cmd.slice(1), { cwd: root, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024, stdio: ['ignore', 'pipe', 'pipe'] });
      if (r.status !== 0) throw new Error((r.stdout ?? '') + (r.stderr ?? ''));
    }
  } catch (e) {
    ok = false;
    note = String(e.message ?? e).split('\n')[0].slice(0, 160);
  }
  summary.push({ id: c.id, ok, note });
  console.log(`[${ok ? 'PASS' : 'FAIL'}] ${c.id} ${note}`);
  if (!ok) failed = true;
}

console.log('\n===== PG �����ع���ܣ�ZS-DB-019.B �ػ���=====');
for (const s of summary) console.log(`${s.ok ? 'PASS' : 'FAIL'}  ${s.id} ${s.note}`);
console.log(`�ϼ� ${summary.length} �׼���ʧ�� ${summary.filter((s) => !s.ok).length} �׼�`);
process.exit(failed ? 1 : 0);

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'rg --files outputs/bpm-001; Get-Command node,java,mvn,docker -ErrorAction SilentlyContinue | Select-Object Name,Source; Get-ChildItem -LiteralPath tools -ErrorAction SilentlyContinue | Select-Object Name; node --check scripts/db/run-bpm001-verify.mjs' in E:\zszj-wt-bpm-001-a
 succeeded in 51ms:
Name               Source                                                                              
----               ------                                                                              
node.exe           C:\Program Files\nodejs\node.exe                                                    
docker.exe         C:\Users\Administrator\AppData\Local\Programs\DockerDesktop\resources\bin\docker.exe
apache-maven-3.9.9                                                                                     
jdk-17.0.20.1+1                                                                                        
env.sh                                                                                                 
jdk17.zip                                                                                              
maven.zip                                                                                              



codex
两处安装命令都已补入 BOM；引导后查询异常也会先计入失败再退出。我正在验证异步改造的信号响应和超时行为，重点检查 Windows 下 `cmd.exe` 与 Maven 子进程的退出关系。
web search: 
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "@'
const fs = require('node:fs');
for (const [file,start,end] of [['services/zhongshu-core/pom.xml',1,110],['scripts/db/run-bpm001-verify.mjs',275,342],['.github/workflows/pg-regression.yml',23,32]]) {
  console.log('\\nFILE '+file);
  fs.readFileSync(file,'utf8').split(/\\r?\\n/).slice(start-1,end).forEach((line,i)=>console.log("'`${start+i}: ${line}`));
}
for (const file of ['"'outputs/bpm-001/runtime-report.json','outputs/bpm-001/r2-final.log']) {
  console.log('\\nEXISTING ARTIFACT '+file);
  console.log(fs.readFileSync(file,'utf8').split(/\\r?\\n/).slice(-48).join('\\n'));
}
'@ | node" in E:\zszj-wt-bpm-001-a
 succeeded in 0ms:

FILE services/zhongshu-core/pom.xml
1: <?xml version="1.0" encoding="UTF-8"?>
2: <project xmlns="http://maven.apache.org/POM/4.0.0"
3:          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
4:          xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
5:     <modelVersion>4.0.0</modelVersion>
6:     <groupId>cn.zszj</groupId>
7:     <artifactId>zszj</artifactId>
8:     <version>${revision}</version>
9:     <packaging>pom</packaging>
10:     <modules>
11:         <module>zszj-dependencies</module>
12:         <module>zszj-framework</module>
13:         <!-- Server 主项目 -->
14:         <module>zszj-server</module>
15:         <!-- 各种 module 拓展 -->
16:         <module>zszj-module-system</module>
17:         <module>zszj-module-infra</module>
18: <!--        <module>zszj-module-member</module>-->
19: <!--        <module>zszj-module-bpm</module>-->
20: <!--        <module>zszj-module-report</module>-->
21: <!--        <module>zszj-module-mp</module>-->
22: <!--        <module>zszj-module-pay</module>-->
23: <!--        <module>zszj-module-mall</module>-->
24: <!--        <module>zszj-module-crm</module>-->
25: <!--        <module>zszj-module-erp</module>-->
26: <!--        <module>zszj-module-iot</module>-->
27: <!--        <module>zszj-module-mes</module>-->
28: <!--        <module>zszj-module-wms</module>-->
29: <!--        <module>zszj-module-hrm</module>-->
30: <!--        <module>zszj-module-fms</module>-->
31: <!--        <module>zszj-module-pms</module>-->
32: <!--        <module>zszj-module-im</module>-->
33: <!-- 请参考 https://doc.iocoder.cn/ai/build/ 文档，完成 AI 模块的启动！！！ -->
34: <!--        <module>zszj-module-ai</module>-->
35:     </modules>
36: 
37:     <name>${project.artifactId}</name>
38:     <description>众墅之家项目基础脚手架</description>
39:     <url>https://github.com/YunaiV/ruoyi-vue-pro</url>
40: 
41:     <properties>
42:         <revision>2026.08-SNAPSHOT</revision>
43:         <!-- Maven 相关 -->
44:         <java.version>17</java.version>
45:         <maven.compiler.source>${java.version}</maven.compiler.source>
46:         <maven.compiler.target>${java.version}</maven.compiler.target>
47:         <maven-surefire-plugin.version>3.5.3</maven-surefire-plugin.version>
48:         <maven-compiler-plugin.version>3.14.0</maven-compiler-plugin.version>
49:         <flatten-maven-plugin.version>1.7.2</flatten-maven-plugin.version>
50:         <!-- maven-surefire-plugin 暂时无法通过 bom 的依赖读取（兼容老版本 IDEA 2024 及以前版本） -->
51:         <lombok.version>1.18.42</lombok.version>
52:         <spring.boot.version>3.5.15</spring.boot.version>
53:         <mapstruct.version>1.6.3</mapstruct.version>
54:         <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
55:     </properties>
56: 
57:     <dependencyManagement>
58:         <dependencies>
59:             <dependency>
60:                 <groupId>cn.zszj</groupId>
61:                 <artifactId>zszj-dependencies</artifactId>
62:                 <version>${revision}</version>
63:                 <type>pom</type>
64:                 <scope>import</scope>
65:             </dependency>
66:         </dependencies>
67:     </dependencyManagement>
68: 
69:     <build>
70:         <pluginManagement>
71:             <plugins>
72:                 <!-- maven-surefire-plugin 插件，用于运行单元测试。 -->
73:                 <!-- 注意，需要使用 3.0.X+，因为要支持 Junit 5 版本 -->
74:                 <plugin>
75:                     <groupId>org.apache.maven.plugins</groupId>
76:                     <artifactId>maven-surefire-plugin</artifactId>
77:                     <version>${maven-surefire-plugin.version}</version>
78:                 </plugin>
79:                 <!-- maven-compiler-plugin 插件，解决 spring-boot-configuration-processor + Lombok + MapStruct 组合 -->
80:                 <!-- https://stackoverflow.com/questions/33483697/re-run-spring-boot-configuration-annotation-processor-to-update-generated-metada -->
81:                 <plugin>
82:                     <groupId>org.apache.maven.plugins</groupId>
83:                     <artifactId>maven-compiler-plugin</artifactId>
84:                     <version>${maven-compiler-plugin.version}</version>
85:                     <configuration>
86:                         <annotationProcessorPaths>
87:                             <path>
88:                                 <groupId>org.springframework.boot</groupId>
89:                                 <artifactId>spring-boot-configuration-processor</artifactId>
90:                                 <version>${spring.boot.version}</version>
91:                             </path>
92:                             <path>
93:                                 <groupId>org.projectlombok</groupId>
94:                                 <artifactId>lombok</artifactId>
95:                                 <version>${lombok.version}</version>
96:                             </path>
97:                             <path>
98:                                 <!-- 确保 Lombok 生成的 getter/setter 方法能被 MapStruct 正确识别，
99:                                      避免出现 No property named “xxx" exists 的编译错误 -->
100:                                 <groupId>org.projectlombok</groupId>
101:                                 <artifactId>lombok-mapstruct-binding</artifactId>
102:                                 <version>0.2.0</version>
103:                             </path>
104:                             <path>
105:                                 <groupId>org.mapstruct</groupId>
106:                                 <artifactId>mapstruct-processor</artifactId>
107:                                 <version>${mapstruct.version}</version>
108:                             </path>
109:                         </annotationProcessorPaths>
110:                         <!-- 编译参数写在 arg 内，解决 Spring Boot 3.2 的 Parameter Name Discovery 问题 -->

FILE scripts/db/run-bpm001-verify.mjs
275:   }
276: 
277:   // ---- 阶段1 bootstrap（owner 账号：建表 + 部署 + 发起 + 留积压）----
278:   console.log('[bpm001] 阶段1 bootstrap：owner 账号建引擎表/部署中性夹具/发起实例/留异步积压…');
279:   mkdirSync(outDir, { recursive: true });
280:   const bootAssert = assertSurefire('BpmPgHarnessBootstrapTest', 5);
281:   const bootLog = await runAsync(
282:     runHarnessTestArgs('BpmPgHarnessBootstrapTest'),
283:     { env: harnessEnv('zhongshu_owner', 'owner_local_1', 'true', 'false'), timeoutMs: 30 * 60 * 1000 });
284:   writeFileSync(join(outDir, 'bootstrap.log'), bootLog.output);
285:   let bootOk = false, bootNote = '';
286:   try { bootNote = bootAssert(); bootOk = bootLog.status === 0; if (!bootOk) bootNote += `，但 mvn exit=${bootLog.status}`; }
287:   catch (e) { bootNote = String(e.message ?? e); }
288:   record('P1 bootstrap 引擎建表/部署幂等/发起/租户标签/版本钉住', bootOk, bootNote);
289:   if (!bootOk) {
290:     console.error('[bpm001] bootstrap 阶段失败，日志见 outputs/bpm-001/bootstrap.log（尾部）：\n' + bootLog.output.split('\n').slice(-40).join('\n'));
291:     finish();
292:     return;
293:   }
294: 
295:   try {
296:     actAfterBootstrap = countActFlw('act\\_%');
297:     versionAfterBootstrap = schemaVersion();
298:   } catch (e) {
299:     // 引导后结构读取失败必须记 FAIL 再收尾，不得伪绿
300:     record('P1b 引导后结构读取（表数量/schema.version）', false, String(e.message ?? e));
301:     finish();
302:     return;
303:   }
304: 
305:   // ---- 阶段2 runtime（app 低权限：重启恢复 + 全操作断言 + 零 DDL；执行器由 R70 显式启停）----
306:   console.log('[bpm001] 阶段2 runtime：app 低权限重启接入同库/审批操作/事务回滚/积压恢复…');
307:   const runAssert = assertSurefire('BpmPgHarnessRuntimeTest', 9);
308:   const runLog = await runAsync(
309:     runHarnessTestArgs('BpmPgHarnessRuntimeTest'),
310:     { env: harnessEnv('zhongshu_app', 'app_local_1', 'false', 'false'), timeoutMs: 20 * 60 * 1000 });
311:   writeFileSync(join(outDir, 'runtime.log'), runLog.output);
312:   let runOk = false, runNote = '';
313:   try { runNote = runAssert(); runOk = runLog.status === 0; if (!runOk) runNote += `，但 mvn exit=${runLog.status}`; }
314:   catch (e) { runNote = String(e.message ?? e); }
315:   record('P2 runtime 重启恢复/通过/拒绝/撤回/转办/分页历史/事务回滚/积压恢复', runOk, runNote);
316:   if (!runOk) {
317:     console.error('[bpm001] runtime 阶段失败，日志见 outputs/bpm-001/runtime.log（尾部）：\n' + runLog.output.split('\n').slice(-40).join('\n'));
318:   }
319: 
320:   // ---- S2/S3：运行期零 DDL（表数量与 schema.version 不变）+ 探针落库 ----
321:   {
322:     let ok = false, note = '';
323:     try {
324:       const actAfterRuntime = countActFlw('act\\_%');
325:       const versionAfterRuntime = schemaVersion();
326:       ok = actAfterBootstrap === actAfterRuntime && versionAfterBootstrap === versionAfterRuntime;
327:       note = `ACT_ ${actAfterBootstrap}→${actAfterRuntime}, schema.version ${versionAfterBootstrap}→${versionAfterRuntime}`;
328:     } catch (e) { note = String(e.message ?? e); }
329:     record('S2 运行期零DDL：引擎表数量与 schema.version 不变', ok, note);
330:   }
331:   {
332:     let ok = false, note = '';
333:     try {
334:       const probeRows = psqlScalarInt('postgres', 'zhongshu', 'SELECT count(*) FROM bpm_harness_probe');
335:       ok = probeRows >= 3;
336:       note = `探针行=${probeRows}`;
337:     } catch (e) { note = String(e.message ?? e); }
338:     record('S3 异步回声探针落库（owner 引导版本 + app 运行期回声）', ok, note);
339:   }
340: 
341:   finish();
342: })().catch((e) => { console.error('[bpm001] 编排器异常：' + String(e?.stack ?? e)); cleanup(); process.exit(1); });

FILE .github/workflows/pg-regression.yml
23:         with:
24:           distribution: temurin
25:           java-version: 17
26:       # ZS-BPM-001 第 9 套件：BPM 独立构建消费本地仓库产物，须先安装当前提交的依赖模块；
27:       # zszj-dependencies BOM 不会经 -am 传递，必须显式列入（否则 Non-resolvable import POM）
28:       # （根 reactor 保持 BPM 注释态=关闭模块，ModuleWhitelistTest 门禁不变）
29:       - name: 安装依赖模块产物
30:         run: mvn -B -f services/zhongshu-core/pom.xml -pl zszj-dependencies,zszj-module-system,zszj-module-infra -am install -Dmaven.test.skip=true
31:       - name: 运行 PG 技术回归套件
32:         run: node scripts/db/run-pg-regression.mjs

EXISTING ARTIFACT outputs/bpm-001/runtime-report.json
{
  "finishedAt": "2026-09-13T19:40:04.093Z",
  "container": "zszj-bpm001-1789328381042-26088",
  "results": [
    {
      "id": "S0 关闭BPM结构零残留：基线零 ACT_/FLW_ 表",
      "ok": true,
      "note": "ACT_=0 FLW_=0"
    },
    {
      "id": "S1 运行账号无DDL：app 连接可用且建表被 42501 拒绝",
      "ok": true,
      "note": "建表按 SQLSTATE 42501（权限不足）被拒，app 连接本身可用"
    },
    {
      "id": "P1 bootstrap 引擎建表/部署幂等/发起/租户标签/版本钉住",
      "ok": true,
      "note": "tests=5 failures=0 errors=0 skipped=0"
    },
    {
      "id": "P2 runtime 重启恢复/通过/拒绝/撤回/转办/分页历史/事务回滚/积压恢复",
      "ok": true,
      "note": "tests=9 failures=0 errors=0 skipped=0"
    },
    {
      "id": "S2 运行期零DDL：引擎表数量与 schema.version 不变",
      "ok": true,
      "note": "ACT_ 39→39, schema.version 8.0.0.0→8.0.0.0"
    },
    {
      "id": "S3 异步回声探针落库（owner 引导版本 + app 运行期回声）",
      "ok": true,
      "note": "探针行=3"
    }
  ],
  "pass": 6,
  "failCount": 0
}

EXISTING ARTIFACT outputs/bpm-001/r2-final.log
[bpm001] 拉起临时 PG（zszj-bpm001-1789328381042-26088 @ 127.0.0.1:4475）…
[bpm001] 已应用基线迁移 5 个：V20260909.001__system_infra_baseline.sql, V20260909.002__infra_quartz_backfill.sql, V20260909.003__system_dict_unique_constraints.sql, V20260913.001__infra_config_optimistic_version.sql, V20260914.001__infra_file_tenancy.sql
[bpm001] mvn install 依赖模块（BOM+system/infra 及依赖链，跳过测试编译）…
[PASS] S0 关闭BPM结构零残留：基线零 ACT_/FLW_ 表 ACT_=0 FLW_=0
[PASS] S1 运行账号无DDL：app 连接可用且建表被 42501 拒绝 建表按 SQLSTATE 42501（权限不足）被拒，app 连接本身可用
[bpm001] 阶段1 bootstrap：owner 账号建引擎表/部署中性夹具/发起实例/留异步积压…
[PASS] P1 bootstrap 引擎建表/部署幂等/发起/租户标签/版本钉住 tests=5 failures=0 errors=0 skipped=0
[bpm001] 阶段2 runtime：app 低权限重启接入同库/审批操作/事务回滚/积压恢复…
[PASS] P2 runtime 重启恢复/通过/拒绝/撤回/转办/分页历史/事务回滚/积压恢复 tests=9 failures=0 errors=0 skipped=0
[PASS] S2 运行期零DDL：引擎表数量与 schema.version 不变 ACT_ 39→39, schema.version 8.0.0.0→8.0.0.0
[PASS] S3 异步回声探针落库（owner 引导版本 + app 运行期回声） 探针行=3

===== ZS-BPM-001 BPM 独立装配与 PG 验收汇总 =====
PASS  S0 关闭BPM结构零残留：基线零 ACT_/FLW_ 表 ACT_=0 FLW_=0
PASS  S1 运行账号无DDL：app 连接可用且建表被 42501 拒绝 建表按 SQLSTATE 42501（权限不足）被拒，app 连接本身可用
PASS  P1 bootstrap 引擎建表/部署幂等/发起/租户标签/版本钉住 tests=5 failures=0 errors=0 skipped=0
PASS  P2 runtime 重启恢复/通过/拒绝/撤回/转办/分页历史/事务回滚/积压恢复 tests=9 failures=0 errors=0 skipped=0
PASS  S2 运行期零DDL：引擎表数量与 schema.version 不变 ACT_ 39→39, schema.version 8.0.0.0→8.0.0.0
PASS  S3 异步回声探针落库（owner 引导版本 + app 运行期回声） 探针行=3
合计 6 项，失败 0 项


exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "@'
const {spawn,spawnSync}=require('node:child_process');
const fs=require('node:fs');
const path=require('node:path');
const vm=require('node:vm');
const source=fs.readFileSync('scripts/db/run-bpm001-verify.mjs','utf8');
const fn=source.slice(source.indexOf('function runAsync('),source.indexOf('const mvnArgv'));
const core=process.cwd(), childEnv=process.env;
const runAsync=vm.runInNewContext(fn+'\\nrunAsync',{spawn,core,childEnv,setTimeout,clearTimeout});
(async()=>{
  let started=Date.now();
  const baseline=spawnSync('cmd.exe',['/d','/s','/c','ping -n 4 127.0.0.1'],{encoding:'utf8',timeout:150});
  console.log(JSON.stringify({case:'parent-style sync timeout',elapsedMs:Date.now()-started,status:baseline.status,error:baseline.error?.code}));
  started=Date.now();
  let settled=false;
  const current=runAsync(['cmd.exe','/d','/s','/c','ping -n 4 127.0.0.1'],{timeoutMs:150}).then(r=>{settled=true;console.log(JSON.stringify({case:'HEAD runAsync timeout completion',elapsedMs:Date.now()-started,status:r.status}));});
  await new Promise(r=>setTimeout(r,700));
  console.log(JSON.stringify({case:'HEAD after deadline',elapsedMs:Date.now()-started,settled}));
  await current;
  const env={...process.env,JAVA_HOME:path.join(core,'tools','jdk-17.0.20.1+1'),PATH:path.join(core,'tools','jdk-17.0.20.1+1','bin')+';'+path.join(core,'tools','apache-maven-3.9.9','bin')+';'+process.env.PATH};
  const bare=spawnSync('mvn',['-v'],{env,encoding:'utf8'});
  const wrapped=spawnSync('cmd.exe',['/d','/s','/c','mvn -v'],{env,encoding:'utf8'});
  console.log(JSON.stringify({case:'Windows mvn.cmd PATH probe',bare:{status:bare.status,error:bare.error?.code},wrapped:{status:wrapped.status,error:wrapped.error?.code,stdout:wrapped.stdout?.split('\\n').slice(0,3)}}));
})();
'@ | node" in E:\zszj-wt-bpm-001-a
web search: https://nodejs.org/docs/latest-v22.x/api/child_process.html
 succeeded in 3316ms:
{"case":"parent-style sync timeout","elapsedMs":157,"status":null,"error":"ETIMEDOUT"}
{"case":"HEAD after deadline","elapsedMs":714,"settled":false}
{"case":"HEAD runAsync timeout completion","elapsedMs":3043,"status":null}
{"case":"Windows mvn.cmd PATH probe","bare":{"status":null,"error":"ENOENT"},"wrapped":{"status":0,"stdout":["Apache Maven 3.9.9 (8e8579a9e76f7d015ee5ec7bfcdc97d260186937)\r","Maven home: E:\\zszj-wt-bpm-001-a\\tools\\apache-maven-3.9.9\r","Java version: 17.0.20.1, vendor: Eclipse Adoptium, runtime: E:\\zszj-wt-bpm-001-a\\tools\\jdk-17.0.20.1+1\r"]}}

web search: 
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "@'
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const assert = require('node:assert/strict');
const {EventEmitter} = require('node:events');
const source = fs.readFileSync('scripts/db/run-bpm001-verify.mjs','utf8')
 .replace(/"'^import .*;'"\\r?"'$/gm,'"'')
 .replace(\"fileURLToPath(new URL('../../', import.meta.url))\",JSON.stringify(process.cwd()));
class Exit extends Error {}
async function verify(config) {
 const state={stage:'setup',commands:[],cleanup:0,firstExit:undefined,files:new Map(),timers:new Set()};
 const proc=new EventEmitter();
 proc.platform=config.platform||'win32'; proc.env={PATH:'test-path',JAVA_HOME:'test-java'};
 proc.exit=code=>{if(state.firstExit===undefined)state.firstExit=code;throw new Exit(String(code));};
 function spawnSync(file,args,opts={}) {
  state.commands.push({file,args});
  if(args[0]==='rm'){state.cleanup++;return {status:0};}
  const sql=args[args.length-1];
  if(typeof opts.input==='string'&&opts.input.includes('zszj_app_should_fail'))return {status:1,stderr:'ERROR: 42501'};
  if(typeof sql==='string'&&sql.includes('information_schema.tables')) {
   if(state.stage==='bootstrap'&&config.fault==='act-error')return {status:1,stderr:'injected count query error'};
   return {status:0,stdout:state.stage==='setup'||state.stage==='install'?'0':'39'};
  }
  if(typeof sql==='string'&&sql.includes('schema.version')){
   if(state.stage==='bootstrap'&&config.fault==='version-error')return {status:1,stderr:'injected version query error'};
   if(state.stage==='bootstrap'&&config.fault==='version-empty')return {status:0,stdout:''};
   return {status:0,stdout:'8.0.0.0'};
  }
  if(typeof sql==='string'&&sql.includes('count(*) FROM bpm_harness_probe'))return {status:0,stdout:'3'};
  return {status:0,stdout:'1',stderr:''};
 }
 function spawn(file,args,opts) {
  const stage=args.join(' ').includes('Bootstrap')?'bootstrap':args.join(' ').includes('Runtime')?'runtime':'install';
  state.stage=stage; state.commands.push({file,args,env:opts.env,async:true});
  const child=new EventEmitter(); child.stdout=new EventEmitter();child.stderr=new EventEmitter();child.kill=()=>true;
  setImmediate(()=>{
   if(config.signalStage===stage){try{proc.emit(config.signal||'SIGTERM');}catch(e){if("'!(e instanceof Exit))throw e;}return;}
   if(stage!=='"'install'){
    const testClass=stage==='bootstrap'?'BpmPgHarnessBootstrapTest':'BpmPgHarnessRuntimeTest';
    const report=path.join(process.cwd(),'services','zhongshu-core','zszj-module-bpm','target','surefire-reports',"'`TEST-cn.zszj.module.bpm.harness.${testClass}.xml`);
    state.files.set(report,`<testsuite name="cn.zszj.module.bpm.harness.${testClass}" tests="${stage==='"'bootstrap'?5:9}\" failures=\"0\" errors=\"0\" skipped=\"0\"></testsuite>"'`);
   }
   child.emit('"'close',0);
  });
  return child;
 }
 const sandbox={process:proc,spawnSync,spawn,join:path.join,console:{log(){},error(){}},URL,SharedArrayBuffer,Int32Array,Atomics,
  existsSync:p=>state.files.has(p),mkdirSync(){},rmSync:p=>state.files.delete(p),readdirSync:()=>[],
  readFileSync:p=>state.files.get(p)||'',writeFileSync:(p,v)=>state.files.set(p,v),
  setTimeout:(cb,ms)=>{const t=setTimeout(cb,ms);state.timers.add(t);return t;},clearTimeout:t=>{clearTimeout(t);state.timers.delete(t);}};
 const pending=vm.runInNewContext(source,sandbox);pending.catch(e=>{if("'!(e instanceof Exit))throw e;});
 for(let i=0;i<12&&state.firstExit===undefined;i++)await new Promise(setImmediate);
 for(const timer of state.timers)clearTimeout(timer);
 const reportText=[...state.files].find(([p])=>p.endsWith('"'runtime-report.json'))?.[1];
 const report=reportText?JSON.parse(reportText):null;
 const expected=config.signalStage?(config.signal==='SIGINT'?130:143):config.fault?1:0;
 assert.equal(state.firstExit,expected);assert.equal(state.cleanup,1);
 if(config.fault){assert.equal(report.failCount,1);assert.ok(report.results.some(r=>r.id.startsWith('P1b')&&"'!r.ok));assert.equal(state.stage,'"'bootstrap');}
 const install=state.commands.find(c=>c.async&&c.args.join(' ').includes('install'));
 assert.ok(install.args.join(' ').includes('zszj-dependencies,zszj-module-system,zszj-module-infra'));
 if(proc.platform==='win32'){assert.equal(state.commands[0].file,'cmd.exe');assert.deepEqual(state.commands[0].args,['/d','/s','/c','mvn -v']);}
 console.log(JSON.stringify({case:config,exit:state.firstExit,cleanup:state.cleanup,failCount:report?.failCount,stage:state.stage,pass:true}));
}
(async()=>{
 for(const config of [{},{platform:'linux'},{fault:'act-error'},{fault:'version-error'},{fault:'version-empty'},
 {signalStage:'install'},{signalStage:'bootstrap'},{signalStage:'runtime'},{signalStage:'runtime',signal:'SIGINT'}])await verify(config);
})().catch(e=>{console.error(e);process.exitCode=1;});
'@ | node" in E:\zszj-wt-bpm-001-a
 exited 1 in 0ms:
AssertionError [ERR_ASSERTION]: Values have same structure but are not reference-equal:

[
  '/d',
  '/s',
  '/c',
  'mvn -v'
]

    at verify ([stdin]:61:84)
    at async [stdin]:66:117 {
  generatedMessage: true,
  code: 'ERR_ASSERTION',
  actual: [Array],
  expected: [Array],
  operator: 'deepStrictEqual',
  diff: 'simple'
}

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'git diff --check HEAD''^ HEAD; rg -n '"'flattenMode|updatePomFile|flatten-maven-plugin|<build>|<phase>process-resources|<phase>clean' services/zhongshu-core/zszj-dependencies/pom.xml; rg -n 'mvn|java.exe|JAVACMD|MAVEN_OPTS' tools/apache-maven-3.9.9/bin/mvn.cmd" in E:\zszj-wt-bpm-001-a
 succeeded in 0ms:
18:        <flatten-maven-plugin.version>1.7.2</flatten-maven-plugin.version>
763:    <build>
768:                <artifactId>flatten-maven-plugin</artifactId>
769:                <version>${flatten-maven-plugin.version}</version>
771:                    <flattenMode>bom</flattenMode>
772:                    <updatePomFile>true</updatePomFile>
780:                        <phase>process-resources</phase>
787:                        <phase>clean</phase>
27:@REM   MAVEN_OPTS        (Optional) Java runtime options used when Maven is executed.
49:for %%i in (java.exe) do set "JAVACMD=%%~$PATH:i"
53:set "JAVACMD=%JAVA_HOME%\bin\java.exe"
56:if exist "%JAVACMD%" goto chkMHome
69:if exist "%MAVEN_HOME%\bin\mvn.cmd" goto init
77:@REM Find the project basedir, i.e., the directory that contains the directory ".mvn".
86:@REM Look for the --file switch and start the search for the .mvn directory from the specified
142:if exist ".mvn" goto baseDirFound
160:set "jvmConfig=\.mvn\jvm.config"
164:for /F "usebackq delims=" %%a in ("%MAVEN_PROJECTBASEDIR%\.mvn\jvm.config") do set JVM_CONFIG_MAVEN_PROPS=!JVM_CONFIG_MAVEN_PROPS! %%a
172:"%JAVACMD%" ^
174:  %MAVEN_OPTS% ^

web search: site.nodejs.org/api/child_process.html "close" "stdio streams" ...
web search: 
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "@'
const fs=require('node:fs'), vm=require('node:vm'), path=require('node:path'), assert=require('node:assert/strict');
const {EventEmitter}=require('node:events');
const source=fs.readFileSync('scripts/db/run-bpm001-verify.mjs','utf8').replace(/"'^import .*;'"\\r?"'$/gm,'"'').replace(\"fileURLToPath(new URL('../../', import.meta.url))\",JSON.stringify(process.cwd()));
class Exit extends Error {}
async function verify(config){
 const s={stage:'setup',commands:[],cleanup:0,exit:undefined,files:new Map(),timers:new Set()};
 const proc=new EventEmitter();proc.platform=config.platform||'win32';proc.env={PATH:'test-path',JAVA_HOME:'test-java'};
 proc.exit=code=>{if(s.exit===undefined)s.exit=code;throw new Exit();};
 function spawnSync(file,args,opts={}){
  s.commands.push({file,args});
  if(args[0]==='rm'){s.cleanup++;return {status:0};}
  const sql=args.at(-1);
  if(opts.input?.includes('zszj_app_should_fail'))return {status:1,stderr:'ERROR: 42501'};
  if(sql?.includes('information_schema.tables')){
   if(s.stage==='bootstrap'&&config.fault==='act-error')return {status:1,stderr:'injected count error'};
   return {status:0,stdout:['setup','install'].includes(s.stage)?'0':'39'};
  }
  if(sql?.includes('schema.version')){
   if(s.stage==='bootstrap'&&config.fault==='version-error')return {status:1,stderr:'injected version error'};
   return {status:0,stdout:s.stage==='bootstrap'&&config.fault==='version-empty'?'':'8.0.0.0'};
  }
  if(sql?.includes('count(*) FROM bpm_harness_probe'))return {status:0,stdout:'3'};
  return {status:0,stdout:'1',stderr:''};
 }
 function spawn(file,args,opts){
  const stage=args.join(' ').includes('Bootstrap')?'bootstrap':args.join(' ').includes('Runtime')?'runtime':'install';
  s.stage=stage;s.commands.push({file,args,env:opts.env,async:true});
  const child=new EventEmitter();child.stdout=new EventEmitter();child.stderr=new EventEmitter();child.kill=()=>true;
  setImmediate(()=>{
   if(config.signalStage===stage){try{proc.emit(config.signal||'SIGTERM');}catch(e){if("'!(e instanceof Exit))throw e;}return;}
   if(stage!=='"'install'){
    const cls=stage==='bootstrap'?'BpmPgHarnessBootstrapTest':'BpmPgHarnessRuntimeTest';
    const report=path.join(process.cwd(),'services','zhongshu-core','zszj-module-bpm','target','surefire-reports',"'`TEST-cn.zszj.module.bpm.harness.${cls}.xml`);
    s.files.set(report,`<testsuite name="cn.zszj.module.bpm.harness.${cls}" tests="${stage==='"'bootstrap'?5:9}\" failures=\"0\" errors=\"0\" skipped=\"0\"></testsuite>"'`);
   }
   child.emit('"'close',0);
  });return child;
 }
 const sandbox={process:proc,spawnSync,spawn,join:path.join,console:{log(){},error(){}},URL,SharedArrayBuffer,Int32Array,Atomics,
  existsSync:p=>s.files.has(p),mkdirSync(){},rmSync:p=>s.files.delete(p),readdirSync:()=>[],readFileSync:p=>s.files.get(p)||'',writeFileSync:(p,v)=>s.files.set(p,v),
  setTimeout:(cb,ms)=>{const t=setTimeout(cb,ms);s.timers.add(t);return t;},clearTimeout:t=>{clearTimeout(t);s.timers.delete(t);}};
 vm.runInNewContext(source,sandbox).catch(e=>{if("'!(e instanceof Exit))throw e;});
 for(let i=0;i<12&&s.exit===undefined;i++)await new Promise(setImmediate);
 for(const t of s.timers)clearTimeout(t);
 const json=[...s.files].find(([p])=>p.endsWith('"'runtime-report.json'))?.[1], report=json?JSON.parse(json):null;
 assert.equal(s.exit,config.signalStage?(config.signal==='SIGINT'?130:143):config.fault?1:0);assert.equal(s.cleanup,1);
 if(config.fault){assert.equal(report.failCount,1);assert.ok(report.results.some(r=>r.id.startsWith('P1b')&&"'!r.ok));assert.equal(s.stage,'"'bootstrap');}
 const install=s.commands.find(c=>c.async&&c.args.join(' ').includes('install'));
 assert.ok(install.args.join(' ').includes('zszj-dependencies,zszj-module-system,zszj-module-infra'));
 if(proc.platform==='win32'){assert.equal(s.commands[0].file,'cmd.exe');assert.equal(JSON.stringify(s.commands[0].args),JSON.stringify(['/d','/s','/c','mvn -v']));}
 console.log(JSON.stringify({case:config,exit:s.exit,cleanup:s.cleanup,failCount:report?.failCount,stage:s.stage,pass:true}));
}
(async()=>{for(const config of [{},{platform:'linux'},{fault:'act-error'},{fault:'version-error'},{fault:'version-empty'},{signalStage:'install'},{signalStage:'bootstrap'},{signalStage:'runtime'},{signalStage:'runtime',signal:'SIGINT'}])await verify(config);})().catch(e=>{console.error(e);process.exitCode=1;});
'@ | node" in E:\zszj-wt-bpm-001-a
 succeeded in 0ms:
{"case":{},"exit":0,"cleanup":1,"failCount":0,"stage":"runtime","pass":true}
{"case":{"platform":"linux"},"exit":0,"cleanup":1,"failCount":0,"stage":"runtime","pass":true}
{"case":{"fault":"act-error"},"exit":1,"cleanup":1,"failCount":1,"stage":"bootstrap","pass":true}
{"case":{"fault":"version-error"},"exit":1,"cleanup":1,"failCount":1,"stage":"bootstrap","pass":true}
{"case":{"fault":"version-empty"},"exit":1,"cleanup":1,"failCount":1,"stage":"bootstrap","pass":true}
{"case":{"signalStage":"install"},"exit":143,"cleanup":1,"stage":"install","pass":true}
{"case":{"signalStage":"bootstrap"},"exit":143,"cleanup":1,"stage":"bootstrap","pass":true}
{"case":{"signalStage":"runtime"},"exit":143,"cleanup":1,"stage":"runtime","pass":true}
{"case":{"signalStage":"runtime","signal":"SIGINT"},"exit":130,"cleanup":1,"stage":"runtime","pass":true}

web search: https://nodejs.org/download/release/v22.19.0/docs/api/child_process.html
codex
针对 HEAD `6d826348`，四项原问题均闭合，但发现 1 项新增 P2。

1. **闭合（P1 BOM 安装）**：CI 与编排器的安装命令均显式加入 `zszj-dependencies`，覆盖 BPM 独立构建所需的 BOM。
2. **闭合（P1 查询失败伪绿）**：异常分支先记录失败再 `finish()`。故障注入覆盖表数量查询失败、版本查询失败及空结果，均得到 `failCount=1`、退出码 1，且未进入 runtime。
3. **闭合（P2 信号不可达）**：安装、bootstrap、runtime 均改为异步等待；信号事件注入验证清理分支可达，SIGTERM/SIGINT 分别退出 143/130。
4. **闭合（P2 Windows PATH 探测）**：探测与执行统一经过 `cmd.exe /d /s /c`。实际验证 `mvn.cmd` 经包装后探测成功。

新增问题：

- **P2：Windows Maven 超时后可能持续挂起**——[scripts/db/run-bpm001-verify.mjs:126](E:/zszj-wt-bpm-001-a/scripts/db/run-bpm001-verify.mjs:126)。超时仅 `child.kill()` 终止外层 `cmd.exe`，Promise 仍等待第 128 行的 `close`；Maven/Java 子进程继续持有输出管道时，`close` 不会到达，后续失败处理及容器清理也无法执行。[Node 文档确认 `close` 需要进程结束且 stdio 关闭](https://nodejs.org/download/release/v22.19.0/docs/api/child_process.html#event-close)。实测以 `cmd → ping` 模拟该进程关系，设置 150ms 超时：原同步方式约 157ms 返回，HEAD 实现约 3043ms、直到子进程自然结束才返回。应终止进程树，并保证超时后在有界时间内返回失败、触发清理。

已完成语法检查、内存故障注入及 Windows 子进程复现；只读环境下未重跑完整 Maven/PG 验收。

NEEDS_FIX
tokens used
58,474
针对 HEAD `6d826348`，四项原问题均闭合，但发现 1 项新增 P2。

1. **闭合（P1 BOM 安装）**：CI 与编排器的安装命令均显式加入 `zszj-dependencies`，覆盖 BPM 独立构建所需的 BOM。
2. **闭合（P1 查询失败伪绿）**：异常分支先记录失败再 `finish()`。故障注入覆盖表数量查询失败、版本查询失败及空结果，均得到 `failCount=1`、退出码 1，且未进入 runtime。
3. **闭合（P2 信号不可达）**：安装、bootstrap、runtime 均改为异步等待；信号事件注入验证清理分支可达，SIGTERM/SIGINT 分别退出 143/130。
4. **闭合（P2 Windows PATH 探测）**：探测与执行统一经过 `cmd.exe /d /s /c`。实际验证 `mvn.cmd` 经包装后探测成功。

新增问题：

- **P2：Windows Maven 超时后可能持续挂起**——[scripts/db/run-bpm001-verify.mjs:126](E:/zszj-wt-bpm-001-a/scripts/db/run-bpm001-verify.mjs:126)。超时仅 `child.kill()` 终止外层 `cmd.exe`，Promise 仍等待第 128 行的 `close`；Maven/Java 子进程继续持有输出管道时，`close` 不会到达，后续失败处理及容器清理也无法执行。[Node 文档确认 `close` 需要进程结束且 stdio 关闭](https://nodejs.org/download/release/v22.19.0/docs/api/child_process.html#event-close)。实测以 `cmd → ping` 模拟该进程关系，设置 150ms 超时：原同步方式约 157ms 返回，HEAD 实现约 3043ms、直到子进程自然结束才返回。应终止进程树，并保证超时后在有界时间内返回失败、触发清理。

已完成语法检查、内存故障注入及 Windows 子进程复现；只读环境下未重跑完整 Maven/PG 验收。

NEEDS_FIX
