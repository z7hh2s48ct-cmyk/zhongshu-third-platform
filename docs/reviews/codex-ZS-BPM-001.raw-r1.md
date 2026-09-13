OpenAI Codex v0.154.0
--------
workdir: E:\zszj-wt-bpm-001-a
model: gpt-6-astra
provider: openai
approval: never
sandbox: read-only
reasoning effort: xhigh
reasoning summaries: none
session id: 01a09c3f-b592-7281-ac24-47389d859828
--------
user
你是资深后端与测试基建评审员。这是 ZS-BPM-001（BPM 独立装配与 PostgreSQL 验收，分支 feat/bpm-001-a）r0 评审（3×P1+7×P2，NEEDS_FIX）之后的修复复核。请只评审修复提交 HEAD（相对其父提交 3ea8051c 的全部改动），并核对 r0 十项发现是否逐项闭合：

r0 发现与申报处置：
1. P1 CI 必失败 → 已改：pg-regression.yml 补 setup-java@17 + mvn install 兄弟模块（system/infra，-Dmaven.test.skip=true）；编排器工具链解析改为 tools/ 优先、回退 PATH mvn/java、皆缺退出码 3；套件前置 mvn install 保证消费当前提交产物。
2. P1 跳过仍绿/陈旧报告 → 已改：跑前 force 删除本轮 TEST-*.xml；跑后要求存在、testsuite name 身份匹配、用例数精确（bootstrap=5/runtime=9）、skipped=0、failures=0、errors=0，且 mvn exit==0。
3. P1 结构检查吞错 → 已改：psqlScalar/psqlScalarInt 严格封装（status/空输出/数值格式），S0/S2/S3 查询失败即 FAIL；S1 先证 app 连接可用（SELECT 1），再用 \set VERBOSITY verbose 使建表失败输出带 SQLSTATE，仅接受 42501（PG15+ schema 级拒绝亦归 42501）。
4. P2 清理吞错 → 已改：docker rm -f -v 重试 3 次且成功才标记 cleaned；SIGTERM 处理；清理失败记 S4 FAIL 并非零退出。
5. P2 启动重试撞名/撞端口 → 已改：失败先 docker rm -f -v 残件、换随机端口再试，失败原因带端口落日志。
6. P2 关闭 BPM 结论超证据 → 已改：S0 更名「结构零残留」；决策文档改为「结构面+装配面（既有 V1.6 真实启动联验+ModuleWhitelistTest）」，明确启动后零流程副作用运行期观测归启用 BPM 批次。
7. P2 分页空洞 → 已改：历史与运行面均按唯一列（实例 ID）全序排序，OFFSET=0/1/2 页与全量 subList 精确相等、两页不重复、运行面精确计数=4。
8. P2 回滚吞异常 → 已改：专用 HarnessRollbackSignal + assertThrows 身份断言；发起成功（start 返回非空）后才抛信号；回滚后运行/历史实例/历史任务三面零残留；重试同一操作提交成功（execute 返回值非空）证明恢复。
9. P2 通过分支未直证 → 已改：approve 用例补 historicActivity approvedEnd=1、rejectedEnd=0。
10. P2 密码合同 → 已改：ENV_PASSWORD 缺失/空白即 IllegalStateException，连接池创建前完成全部校验。

输出格式：逐项判定「闭合/未闭合」（未闭合给 文件:行号 与理由）；如发现修复引入的新问题单独列出（P1/P2）；最后一行总结论：PASS（可合并）或 NEEDS_FIX。

codex
我会先核对 HEAD 相对 3ea8051c 的改动范围，再逐项检查十项修复及其验证证据，并检查是否引入新的 P1/P2 问题。

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'Get-Location; git status --short; git rev-parse HEAD; git rev-parse HEAD''^; git diff --stat 3ea8051c HEAD; rg --files -g AGENTS.md -g '"'*pg*regress*' -g '*Rollback*' -g '*Runtime*' -g '*Bootstrap*'" in E:\zszj-wt-bpm-001-a
 succeeded in 72ms:
8a1639f2e2f1d5f65b4dbb29cc7bd01506cab421
27ee56b8edd6392ff915e0dd58662436fc860c44
 .github/workflows/pg-regression.yml                |  11 +-
 README.md                                          |   4 +-
 ...273\273\345\212\241\346\270\205\345\215\225.md" |  14 +-
 docs/reviews/README.md                             |  11 +-
 docs/reviews/codex-ZS-DB-018.md                    |  19 +
 docs/reviews/codex-ZS-FILE-001.A.md                |  35 ++
 docs/reviews/codex-ZS-PERM-001.A.md                |  20 +
 docs/reviews/codex-ZS-PERM-002.A.md                |  20 +
 docs/reviews/codex-ZS-SEC-001.A.md                 |  21 +
 docs/reviews/codex-ZS-SEC-002.md                   |  24 ++
 docs/reviews/codex-ZS-SEC-005.md                   |  21 +
 docs/reviews/codex-ZS-SEC-007.md                   |  20 +
 docs/reviews/codex-ZS-SEC-012.A.md                 |  20 +
 docs/reviews/codex-ZS-SYS-001.A.md                 |  41 ++
 docs/reviews/codex-hotfix-SEC-007-P1.md            |  39 ++
 scripts/db/run-bpm001-verify.mjs                   | 262 ++++++++----
 scripts/sys001/cases/config.mjs                    | 104 +++++
 scripts/sys001/cases/cross.mjs                     |  52 +++
 scripts/sys001/cases/dict.mjs                      | 108 +++++
 scripts/sys001/cases/menu.mjs                      |  82 ++++
 scripts/sys001/cases/notice.mjs                    |  94 +++++
 scripts/sys001/cases/post.mjs                      |  83 ++++
 scripts/sys001/cases/role.mjs                      | 116 ++++++
 scripts/sys001/cases/user.mjs                      | 105 +++++
 scripts/sys001/run-sys001-regression.mjs           | 451 +++++++++++++++++++++
 ...264\243\344\273\273\345\206\263\347\255\226.md" |   9 +-
 .../common/util/log/LogSanitizeUtils.java          |  63 ++-
 .../common/util/log/LogSanitizeUtilsTest.java      |  62 +++
 .../bpm/harness/BpmPgHarnessConfiguration.java     |   3 +-
 .../bpm/harness/BpmPgHarnessRuntimeTest.java       |  80 +++-
 .../controller/admin/file/FileController.java      |  34 +-
 .../controller/admin/file/vo/file/FileRespVO.java  |   9 +-
 .../controller/app/file/AppFileController.java     |   8 +-
 .../module/infra/dal/dataobject/file/FileDO.java   |  18 +-
 .../module/infra/enums/ErrorCodeConstants.java     |   2 +
 .../module/infra/enums/file/FileScopeEnum.java     |  33 ++
 .../module/infra/service/file/FileService.java     |  17 +-
 .../module/infra/service/file/FileServiceImpl.java | 126 +++++-
 .../service/db/DatabaseTableServiceImplTest.java   |   4 +-
 .../service/file/FileServiceAuthorizationTest.java | 249 ++++++++++++
 .../infra/service/file/FileServiceImplTest.java    |   2 +
 .../src/test/resources/sql/create_tables.sql       |   2 +
 .../system/dal/mysql/dept/UserPostMapper.java      |   4 +
 .../module/system/enums/ErrorCodeConstants.java    |   1 +
 .../system/service/dept/PostServiceImpl.java       |  17 +
 .../system/service/dept/PostServiceImplTest.java   |  51 +++
 .../src/main/resources/application-harness.yaml    |  29 ++
 .../src/main/resources/application.yaml            |   3 +
 .../V20260914.001__infra_file_tenancy.sql          |   7 +
 .../src/test/resources/api-inventory-baseline.txt  |   5 +-
 50 files changed, 2466 insertions(+), 149 deletions(-)
scripts\db\run-pg-regression.mjs
services\zhongshu-core\zszj-module-bpm\src\test\java\cn\zszj\module\bpm\harness\BpmPgHarnessRuntimeTest.java
services\zhongshu-core\zszj-module-bpm\src\test\java\cn\zszj\module\bpm\harness\BpmPgHarnessBootstrapTest.java
Path                
----                
E:\zszj-wt-bpm-001-a



exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'git log -10 --format="%h %p %s"; git show --stat --oneline HEAD; git diff --name-only 3ea8051c HEAD; git branch --show-current' in E:\zszj-wt-bpm-001-a
 succeeded in 0ms:
8a1639f2 27ee56b8 fix(bpm-001-a): codex r0 处置（3×P1+7×P2 全修）——严格校验/CI 供给/断言强化 [ZS-BPM-001]
27ee56b8 3ea8051c 68ea53c7 Merge branch 'main' into feat/bpm-001-a
68ea53c7 e3f5ef29 docs(sys-001-a): ZS-SYS-001.A 收口文档同步（七类真实 PG/API 回归 48/49 + 缺口五项归口登记）+ 主卡转开发中 + docs/05 V1.41
e3f5ef29 270edff2 30ec1cb9 Merge branch 'feat/sys-001-a'
3ea8051c 41be7b06 feat(bpm-001-a): ZS-BPM-001 BPM 独立装配与 PostgreSQL 验收（B09 技术准备先行件）
270edff2 8aafcdd6 docs(sec-007): ZS-SEC-007 HANDOFF 补评 + hotfix 收口文档同步（POJONode 穿透 P1 + Locale.ROOT P2 修复归零）+ docs/05 V1.39
8aafcdd6 27d7f550 docs(file-001-a): ZS-FILE-001.A 收口文档同步（五项交付 + codex r0→r2 三弧处置入库 + 陈旧构件/边界登记）+ 主卡 FILE-001 转开发中 + docs/05 V1.40
30ec1cb9 87e68cf2 fix(sys-001-a): codex P2 处置——报告结论按实测结果推导（不再静态硬编码）、shell 调用 mvn 路径加引号 [ZS-SYS-001.A]
27d7f550 a1e3a505 2c07a190 Merge branch 'feat/file-001-a' (ZS-FILE-001.A 文件租户化与归属授权，codex r0→r2 三弧评审收敛：租户化+归属/下载关匿名旁路/批删越权整批拒绝/禁用未凭证化 create/update-scope)
2c07a190 1d69e91c fix(file-001-a): codex r1 2×P1+2×P2+P3（PRIVATE 管理分支改走 PermissionCommonApi 与 @ss.hasPermission 同源+owner=0 禁止冒领 + App 端 /create 同步禁用[基线 328→327] + 批删测试拆分真实他租户 id 与不存在 id 两用例 + FileServiceImplTest 补权限 mock）
8a1639f2 fix(bpm-001-a): codex r0 处置（3×P1+7×P2 全修）——严格校验/CI 供给/断言强化 [ZS-BPM-001]
 .github/workflows/pg-regression.yml                |  11 +-
 scripts/db/run-bpm001-verify.mjs                   | 262 ++++++++++++++-------
 ...264\243\344\273\273\345\206\263\347\255\226.md" |   9 +-
 .../bpm/harness/BpmPgHarnessConfiguration.java     |   3 +-
 .../bpm/harness/BpmPgHarnessRuntimeTest.java       |  80 +++++--
 5 files changed, 260 insertions(+), 105 deletions(-)
.github/workflows/pg-regression.yml
README.md
"docs/05-\345\272\225\345\272\247\346\250\241\345\235\227\345\210\206\346\236\220\344\270\216\345\274\200\345\217\221\344\273\273\345\212\241\346\270\205\345\215\225.md"
docs/reviews/README.md
docs/reviews/codex-ZS-DB-018.md
docs/reviews/codex-ZS-FILE-001.A.md
docs/reviews/codex-ZS-PERM-001.A.md
docs/reviews/codex-ZS-PERM-002.A.md
docs/reviews/codex-ZS-SEC-001.A.md
docs/reviews/codex-ZS-SEC-002.md
docs/reviews/codex-ZS-SEC-005.md
docs/reviews/codex-ZS-SEC-007.md
docs/reviews/codex-ZS-SEC-012.A.md
docs/reviews/codex-ZS-SYS-001.A.md
docs/reviews/codex-hotfix-SEC-007-P1.md
scripts/db/run-bpm001-verify.mjs
scripts/sys001/cases/config.mjs
scripts/sys001/cases/cross.mjs
scripts/sys001/cases/dict.mjs
scripts/sys001/cases/menu.mjs
scripts/sys001/cases/notice.mjs
scripts/sys001/cases/post.mjs
scripts/sys001/cases/role.mjs
scripts/sys001/cases/user.mjs
scripts/sys001/run-sys001-regression.mjs
"services/zhongshu-core/docs/BPM\345\274\225\346\223\216\350\241\250\344\270\216\344\270\232\345\212\241\346\211\251\345\261\225\350\241\250\350\277\201\347\247\273\350\264\243\344\273\273\345\206\263\347\255\226.md"
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common/util/log/LogSanitizeUtils.java
services/zhongshu-core/zszj-framework/zszj-common/src/test/java/cn/zszj/framework/common/util/log/LogSanitizeUtilsTest.java
services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessConfiguration.java
services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessRuntimeTest.java
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/controller/admin/file/FileController.java
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/controller/admin/file/vo/file/FileRespVO.java
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/controller/app/file/AppFileController.java
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/dal/dataobject/file/FileDO.java
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/enums/ErrorCodeConstants.java
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/enums/file/FileScopeEnum.java
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/service/file/FileService.java
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/service/file/FileServiceImpl.java
services/zhongshu-core/zszj-module-infra/src/test/java/cn/zszj/module/infra/service/db/DatabaseTableServiceImplTest.java
services/zhongshu-core/zszj-module-infra/src/test/java/cn/zszj/module/infra/service/file/FileServiceAuthorizationTest.java
services/zhongshu-core/zszj-module-infra/src/test/java/cn/zszj/module/infra/service/file/FileServiceImplTest.java
services/zhongshu-core/zszj-module-infra/src/test/resources/sql/create_tables.sql
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/dal/mysql/dept/UserPostMapper.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/enums/ErrorCodeConstants.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/service/dept/PostServiceImpl.java
services/zhongshu-core/zszj-module-system/src/test/java/cn/zszj/module/system/service/dept/PostServiceImplTest.java
services/zhongshu-core/zszj-server/src/main/resources/application-harness.yaml
services/zhongshu-core/zszj-server/src/main/resources/application.yaml
services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260914.001__infra_file_tenancy.sql
services/zhongshu-core/zszj-server/src/test/resources/api-inventory-baseline.txt
feat/bpm-001-a

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "rg --files --hidden -g AGENTS.md -g '"'!**/.git/**'"' -g '"'!outputs/**'"'; Get-Content .github/workflows/pg-regression.yml; Get-Content scripts/db/run-bpm001-verify.mjs" in E:\zszj-wt-bpm-001-a
 succeeded in 0ms:
name: pg-regression

# ZS-DB-019.B 地基：PG 技术回归套件（一次�?PG17 容器，验后自动清理）�?# 与本�?`node scripts/db/run-pg-regression.mjs` 同一规则�?# 覆盖：ZS-DB-006 序列/批量/续号�?07 字段映射与逻辑删除�?08 事务与锁�?#       CFG-002.A 字典唯一约束、DB-011~015 有界删除、DB-016/017 结构级验证�?#       ZS-BPM-001 BPM 独立装配�?PG 验收（两阶段）�?
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
      # ZS-BPM-001 �?9 套件：BPM 独立构建消费本地仓库产物，须先安装当前提交的兄弟依赖模块
      # （根 reactor 保持 BPM 注释�?关闭模块，ModuleWhitelistTest 门禁不变�?      - name: 安装兄弟依赖模块产物
        run: mvn -B -f services/zhongshu-core/pom.xml -pl zszj-module-system,zszj-module-infra -am install -Dmaven.test.skip=true
      - name: 运行 PG 技术回归套�?        run: node scripts/db/run-pg-regression.mjs
/**
 * ZS-BPM-001：BPM 独立装配�?PostgreSQL 验收编排器（B09 技术准备，本地�?CI 同一规则）�? *
 * 两阶段语义（同一一次�?PG 容器内的同一物理库）�? *   阶段1 bootstrap（zhongshu_owner，schema-update=true，异步执行器挂起）：
 *     - 空库上由 Flowable 引擎自建 ACT_ 表（建表责任=迁移�?owner 账号）；
 *     - 部署中性技术夹具（重复部署幂等、变更部署出新版本）、发起运行实例、留下异步积压�? *   阶段2 runtime（zhongshu_app 低权限，schema-update=false，异步执行器挂起�?R70 显式启停）：
 *     - �?JVM 重启接入同库（重启恢复：定义/实例/历史全部可见、schema 版本不变=运行期零 DDL）；
 *     - 通过/拒绝/撤回/转办/分页历史、Spring 事务回滚不留引擎痕迹、积压任务被显式启停的执行器消化且无死信�? * 结构检查（编排�?psql 直证，不�?Java；全部查询强制成功，失败�?FAIL 不吞错）�? *   S0 关闭 BPM 结构零残留：System/Infra 基线（全�?Flyway V* 应用后）�?ACT_/FLW_ �? *      （运行期装配面证�?既有 V1.6 真实启动联验 + ModuleWhitelistTest 静态门禁，见决策文档边界）�? *   S1 低权限运行账号无 DDL：app 连接可用（SELECT 1 成功）且 CREATE TABLE �?SQLSTATE 42501 被拒�? *   S2 引擎表数量与 schema.version 前后一致（阶段2 未改结构）；
 *   S3 异步回声探针落库�? * 工具链：优先仓库 tools/（worktree 供给）；否则使用 PATH 上的 java/mvn（CI 已由工作�?provision）；
 *   两者皆缺以退出码 3 明确失败，不静默跳过。套件前�?`mvn install` 兄弟依赖模块
 *   （system/infra 及其依赖链，-Dmaven.test.skip=true），保证 BPM 独立构建消费的是当前提交的产物�? *
 * 语义（对�?scripts/db/run-db018-verify.mjs / test-pg-fixture.mjs 夹具约定）：
 *   - 实例隔离：容器名随机唯一，验后强制清理（docker rm -f -v，重试后验证不存在）�? *   - 失败非零：任一阶段/检查失�?�?非零退出；
 *   - 缺依赖不静默跳过：Docker/工具链不可用 �?退出码 3�? *   - Docker 负载 flaky：容器启动失败先清理残件、换端口重试一次�? * 证据报告：JSON 摘要�?outputs/bpm-001/runtime-report.json（outputs/ 不入库）�? *
 * 用法：node scripts/db/run-bpm001-verify.mjs
 */
import { execFileSync, spawnSync } from 'node:child_process';
import { readFileSync, existsSync, mkdirSync, writeFileSync, readdirSync, rmSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));
const core = join(root, 'services', 'zhongshu-core');
const migrationDir = join(core, 'zszj-server', 'src', 'main', 'resources', 'db', 'migration');
const outDir = join(root, 'outputs', 'bpm-001');

function fail(code, message) { console.error(message); process.exit(code); }

// ---- 工具链解析：tools/ 优先（本�?worktree），否则 PATH（CI 由工作流 provision），皆缺退出码 3 ----
const toolsDir = join(root, 'tools');
const jdkDir = join(toolsDir, 'jdk-17.0.20.1+1');
const mavenBin = join(toolsDir, 'apache-maven-3.9.9', 'bin');
let javaHomeEnv, pathPrefix;
if (existsSync(jdkDir) && existsSync(mavenBin)) {
  javaHomeEnv = jdkDir;
  pathPrefix = process.platform === 'win32' ? `${jdkDir}\\bin;${mavenBin};` : `${jdkDir}/bin:${mavenBin}:`;
} else {
  const probe = spawnSync('mvn', ['-v'], { encoding: 'utf8' });
  const hasJava = spawnSync(process.platform === 'win32' ? 'java.exe' : 'java', ['-version'], { encoding: 'utf8' });
  if (probe.error || probe.status !== 0 || hasJava.error || hasJava.status !== 0) {
    fail(3, `[bpm001] 工具链缺失：�?tools/ 供给�?PATH 上无可用 mvn/java（本地请供给 tools/，CI �?pg-regression.yml provision）`);
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

// ---- psql 严格封装：任何查询失�?空结果都必须显式暴露，不吞错 ----
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
  if (!Number.isInteger(n) || n < 0) throw new Error(`期望非负整数，实际�?{value}」：${sql.slice(0, 120)}`);
  return n;
}

let pass = 0, failCount = 0;
const results = [];
const record = (id, ok, note = '') => { results.push({ id, ok, note }); ok ? pass++ : failCount++; console.log(`[${ok ? 'PASS' : 'FAIL'}] ${id} ${note}`); };

// ---- 拉起一次�?PG（负�?flaky：清理残�?换端口重试一次）----
function dockerRunOnce() {
  const args = ['run', '-d', '--name', container, '-e', 'POSTGRES_PASSWORD=bpm001', '-p', `127.0.0.1:${port}:5432`, 'postgres:17-alpine'];
  const r = process.platform === 'win32'
    ? spawnSync('cmd.exe', ['/d', '/s', '/c', 'docker ' + args.join(' ')], { encoding: 'utf8' })
    : spawnSync('docker', args, { encoding: 'utf8' });
  if (r.status === 0) return true;
  console.error(`[bpm001] docker run 失败（port=${port}）：${((r.stderr ?? '') || '').trim().slice(0, 200)}`);
  return false;
}
console.log(`[bpm001] 拉起临时 PG�?{container} @ 127.0.0.1:${port}）…`);
if (!dockerRunOnce()) {
  spawnSync('docker', ['rm', '-f', '-v', container], { stdio: 'ignore' });
  port = 4332 + Math.floor(Math.random() * 700);
  console.log(`[bpm001] 重试：新端口 ${port}`);
  if (!dockerRunOnce()) fail(3, '[bpm001] PG 夹具容器两次启动失败（Docker 负载竞态按约定重试一次后仍失败）');
}

let ready = false;
// 就绪探测必须�?TCP�?h 127.0.0.1）：initdb 期间的临时服务器只监�?unix socket�?// socket 探测可能误判就绪导致 CREATE DATABASE 落到临时库上失败
for (let i = 0; i < 40; i++) {
  const r = spawnSync('docker', ['exec', container, 'psql', '-h', '127.0.0.1', '-U', 'postgres', '-d', 'postgres', '-At', '-c', 'SELECT 1'], { encoding: 'utf8' });
  if (r.status === 0) { ready = true; break; }
  Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500);
}
if (!ready) { cleanup(); fail(1, '[bpm001] PG 未就�?); }

// ---- 基线库：建库 + 角色授权 + 全部 Flyway 迁移 ----
{
  let created = false, last = '';
  for (let i = 0; i < 10 && !created; i++) {
    const r = psqlRun('postgres', 'postgres', 'CREATE DATABASE zhongshu;');
    created = r.status === 0;
    if (!created) { last = ((r.stderr ?? '') + (r.stdout ?? '')).trim(); Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 1000); }
  }
  if (!created) { cleanup(); fail(1, `[bpm001] 建库失败（重�?10 次）�?{last.slice(0, 400)}`); }
}
{
  const setup = readFileSync(join(core, 'sql', 'postgresql', 'env-setup-test.sql'), 'utf8');
  const r = psqlRun('postgres', 'zhongshu', setup);
  if (r.status !== 0) { cleanup(); fail(1, `[bpm001] 角色授权失败:\n${r.stdout}${r.stderr}`); }
}
{
  const migrations = readdirSync(migrationDir).filter((f) => /^V.*\.sql$/.test(f)).sort();
  const bundled = migrations.map((f) => readFileSync(join(migrationDir, f), 'utf8')).join('\n');
  const r = psqlRun('zhongshu_owner', 'zhongshu', bundled);
  if (r.status !== 0) { cleanup(); fail(1, `[bpm001] 基线迁移执行失败�?{migrations.length} �?V*�?\n${r.stdout}${r.stderr}`); }
  console.log(`[bpm001] 已应用基线迁�?${migrations.length} 个：${migrations.join(', ')}`);
}
if (psqlRun('zhongshu_owner', 'zhongshu',
  'CREATE TABLE bpm_harness_probe(id bigserial PRIMARY KEY, note text NOT NULL, created_at timestamptz NOT NULL DEFAULT now());').status !== 0) {
  cleanup(); fail(1, '[bpm001] 探针表创建失�?);
}

// ---- 兄弟依赖模块产物安装（BPM 独立构建不在 reactor，消费本地仓库产物须来自当前提交�?---
console.log('[bpm001] mvn install 兄弟依赖模块（system/infra 及依赖链，跳过测试编译）�?);
const installArgs = ['-B', '-pl', 'zszj-module-system,zszj-module-infra', '-am', 'install', '-Dmaven.test.skip=true'];
const install = spawnSync(process.platform === 'win32' ? 'cmd.exe' : 'mvn',
  process.platform === 'win32' ? ['/d', '/s', '/c', 'mvn ' + installArgs.join(' ')] : installArgs,
  { cwd: core, env: childEnv, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024, timeout: 20 * 60 * 1000 });
if (install.status !== 0) {
  cleanup();
  fail(1, '[bpm001] 兄弟依赖模块安装失败：\n' + ((install.stdout ?? '') + (install.stderr ?? '')).split('\n').slice(-25).join('\n'));
}

// ---- Maven 子进程（Windows �?Node spawn .cmd �?EINVAL 防护，须�?cmd.exe�?---
function runHarnessTest(testClass, username, password, schemaUpdate, asyncExecutor, timeoutMs) {
  const args = ['-B', '-f', 'zszj-module-bpm/pom.xml', 'test', `-Dtest=${testClass}`];
  const cmd = `mvn ${args.join(' ')}`;
  const r = spawnSync(process.platform === 'win32' ? 'cmd.exe' : 'mvn',
    process.platform === 'win32' ? ['/d', '/s', '/c', cmd] : args,
    {
      cwd: core, env: {
        ...childEnv,
        ZSZJ_BPM_HARNESS_JDBC_URL: `jdbc:postgresql://127.0.0.1:${port}/zhongshu`,
        ZSZJ_BPM_HARNESS_USERNAME: username,
        ZSZJ_BPM_HARNESS_PASSWORD: password,
        ZSZJ_BPM_HARNESS_SCHEMA_UPDATE: schemaUpdate,
        ZSZJ_BPM_HARNESS_ASYNC_EXECUTOR: asyncExecutor,
      },
      encoding: 'utf8', maxBuffer: 64 * 1024 * 1024, timeout: timeoutMs,
    });
  return { ...r, output: (r.stdout ?? '') + (r.stderr ?? '') };
}

// surefire 严格校验：先删本轮报告（防陈旧文件复用），跑后要求存在、身份匹配�?// 用例数精确、零跳过零失败零错误
const reportXml = (testClass) => join(core, 'zszj-module-bpm', 'target', 'surefire-reports',
  `TEST-cn.zszj.module.bpm.harness.${testClass}.xml`);
function assertSurefire(testClass, expectedTests) {
  const xml = reportXml(testClass);
  rmSync(xml, { force: true });
  return () => {
    if (!existsSync(xml)) throw new Error(`本轮 surefire 报告未生成：${xml}`);
    const text = readFileSync(xml, 'utf8');
    const attr = (name) => (text.match(new RegExp(`${name}="(\\d+)"`)) ?? [])[1];
    const suiteName = (text.match(/name="([^"]+)"/) ?? [])[1];
    if (suiteName !== `cn.zszj.module.bpm.harness.${testClass}`) throw new Error(`报告身份不符�?{suiteName}`);
    const tests = Number(attr('tests')), failures = Number(attr('failures')),
      errors = Number(attr('errors')), skipped = Number(attr('skipped'));
    if (tests !== expectedTests) throw new Error(`用例�?${tests} �?预期 ${expectedTests}`);
    if (skipped !== 0) throw new Error(`存在跳过用例 ${skipped}（必测集不得跳过）`);
    if (failures !== 0 || errors !== 0) throw new Error(`failures=${failures} errors=${errors}`);
    return `tests=${tests} failures=0 errors=0 skipped=0`;
  };
}

const countActFlw = (pattern) => psqlScalarInt('postgres', 'zhongshu',
  `SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND lower(table_name) LIKE '${pattern}'`);
const schemaVersion = () => psqlScalar('postgres', 'zhongshu', "SELECT VALUE_ FROM ACT_GE_PROPERTY WHERE NAME_='schema.version'");

// ---- S0：关�?BPM 结构零残留——System/Infra 全基线零流程表（查询失败�?FAIL�?---
let actAfterBootstrap = -1, versionAfterBootstrap = '';
try {
  const act = countActFlw('act\\_%'), flw = countActFlw('flw\\_%');
  record('S0 关闭BPM结构零残留：基线�?ACT_/FLW_ �?, act === 0 && flw === 0, `ACT_=${act} FLW_=${flw}`);
} catch (e) {
  record('S0 关闭BPM结构零残留：基线�?ACT_/FLW_ �?, false, String(e.message ?? e));
}

// ---- S1：低权限运行账号连接可用且无 DDL（区分权限拒�?SQLSTATE 42501 与基础设施错误�?---
// psql 默认 verbosity 不回�?SQLSTATE，须 verbose 才能�?42501 做精确判定（PG15+ 建表被拒�?schema 级）
{
  let ok = false, note = '';
  try {
    psqlScalar('zhongshu_app', 'zhongshu', 'SELECT 1');
    const r = spawnSync('docker', ['exec', '-i', container, 'psql', '-U', 'zhongshu_app', '-d', 'zhongshu',
      '-v', 'ON_ERROR_STOP=1', '-q'], { input: '\\set VERBOSITY verbose\nCREATE TABLE zszj_app_should_fail(id int);', encoding: 'utf8' });
    const output = ((r.stderr ?? '') + (r.stdout ?? '')).trim();
    if (r.status === 0) { note = 'app 竟然建表成功'; }
    else if (output.includes('42501')) { ok = true; note = '建表�?SQLSTATE 42501（权限不足）被拒，app 连接本身可用'; }
    else { note = `建表失败但非权限拒绝�?{output.slice(0, 160)}`; }
  } catch (e) { note = String(e.message ?? e); }
  record('S1 运行账号无DDL：app 连接可用且建表被 42501 拒绝', ok, note);
}

// ---- 阶段1 bootstrap（owner 账号：建�?+ 部署 + 发起 + 留积压）----
console.log('[bpm001] 阶段1 bootstrap：owner 账号建引擎表/部署中性夹�?发起实例/留异步积压�?);
mkdirSync(outDir, { recursive: true });
const bootAssert = assertSurefire('BpmPgHarnessBootstrapTest', 5);
const bootLog = runHarnessTest('BpmPgHarnessBootstrapTest', 'zhongshu_owner', 'owner_local_1', 'true', 'false', 30 * 60 * 1000);
writeFileSync(join(outDir, 'bootstrap.log'), bootLog.output);
let bootOk = false, bootNote = '';
try { bootNote = bootAssert(); bootOk = bootLog.status === 0; if (!bootOk) bootNote += `，但 mvn exit=${bootLog.status}`; }
catch (e) { bootNote = String(e.message ?? e); }
record('P1 bootstrap 引擎建表/部署幂等/发起/租户标签/版本钉住', bootOk, bootNote);
if (!bootOk) {
  console.error('[bpm001] bootstrap 阶段失败，日志见 outputs/bpm-001/bootstrap.log（尾部）：\n' + bootLog.output.split('\n').slice(-40).join('\n'));
  finish();
}

try {
  actAfterBootstrap = countActFlw('act\\_%');
  versionAfterBootstrap = schemaVersion();
} catch (e) {
  console.error('[bpm001] 引导后结构读取失败：' + String(e.message ?? e));
  finish();
}

// ---- 阶段2 runtime（app 低权限：重启恢复 + 全操作断言 + �?DDL；执行器�?R70 显式启停�?---
console.log('[bpm001] 阶段2 runtime：app 低权限重启接入同�?审批操作/事务回滚/积压恢复�?);
const runAssert = assertSurefire('BpmPgHarnessRuntimeTest', 9);
const runLog = runHarnessTest('BpmPgHarnessRuntimeTest', 'zhongshu_app', 'app_local_1', 'false', 'false', 20 * 60 * 1000);
writeFileSync(join(outDir, 'runtime.log'), runLog.output);
let runOk = false, runNote = '';
try { runNote = runAssert(); runOk = runLog.status === 0; if (!runOk) runNote += `，但 mvn exit=${runLog.status}`; }
catch (e) { runNote = String(e.message ?? e); }
record('P2 runtime 重启恢复/通过/拒绝/撤回/转办/分页历史/事务回滚/积压恢复', runOk, runNote);
if (!runOk) {
  console.error('[bpm001] runtime 阶段失败，日志见 outputs/bpm-001/runtime.log（尾部）：\n' + runLog.output.split('\n').slice(-40).join('\n'));
}

// ---- S2/S3：运行期�?DDL（表数量�?schema.version 不变�? 探针落库 ----
{
  let ok = false, note = '';
  try {
    const actAfterRuntime = countActFlw('act\\_%');
    const versionAfterRuntime = schemaVersion();
    ok = actAfterBootstrap === actAfterRuntime && versionAfterBootstrap === versionAfterRuntime;
    note = `ACT_ ${actAfterBootstrap}�?{actAfterRuntime}, schema.version ${versionAfterBootstrap}�?{versionAfterRuntime}`;
  } catch (e) { note = String(e.message ?? e); }
  record('S2 运行期零DDL：引擎表数量�?schema.version 不变', ok, note);
}
{
  let ok = false, note = '';
  try {
    const probeRows = psqlScalarInt('postgres', 'zhongshu', 'SELECT count(*) FROM bpm_harness_probe');
    ok = probeRows >= 3;
    note = `探针�?${probeRows}`;
  } catch (e) { note = String(e.message ?? e); }
  record('S3 异步回声探针落库（owner 引导版本 + app 运行期回声）', ok, note);
}

function finish() {
  cleanup();
  if (!cleaned) record('S4 夹具容器清理', false, `docker rm 重试后仍失败�?{cleanupError}`);
  console.log('\n===== ZS-BPM-001 BPM 独立装配�?PG 验收汇�?=====');
  for (const r of results) console.log(`${r.ok ? 'PASS' : 'FAIL'}  ${r.id} ${r.note}`);
  console.log(`合计 ${results.length} 项，失败 ${failCount} 项`);
  try {
    writeFileSync(join(outDir, 'runtime-report.json'),
      JSON.stringify({ finishedAt: new Date().toISOString(), container, results, pass, failCount }, null, 2));
  } catch { /* 报告落盘失败不影响退出码语义 */ }
  process.exit(failCount === 0 ? 0 : 1);
}
finish();

codex
HEAD 是修复提交 `8a1639f2`，实际父提交是合并提交 `27ee56b8`，`3ea8051c` 是该合并的第一父提交。我会聚焦修复提交的五个文件，并对照 `3ea8051c` 核查；合入 main 的其他改动不作为本轮新增问题。

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'git -c core.quotepath=false diff HEAD''^ HEAD -- .github/workflows/pg-regression.yml scripts/db/run-bpm001-verify.mjs services/zhongshu-core/docs; git diff HEAD''^ HEAD -- services/zhongshu-core/zszj-module-bpm' in E:\zszj-wt-bpm-001-a
 succeeded in 0ms:
diff --git a/.github/workflows/pg-regression.yml b/.github/workflows/pg-regression.yml
index 9a119420..24828e1f 100644
--- a/.github/workflows/pg-regression.yml
+++ b/.github/workflows/pg-regression.yml
@@ -3,7 +3,8 @@ name: pg-regression
 # ZS-DB-019.B 地基：PG 技术回归套件（一次性 PG17 容器，验后自动清理）。
 # 与本地 `node scripts/db/run-pg-regression.mjs` 同一规则。
 # 覆盖：ZS-DB-006 序列/批量/续号、007 字段映射与逻辑删除、008 事务与锁、
-#       CFG-002.A 字典唯一约束、DB-011~015 有界删除、DB-016/017 结构级验证。
+#       CFG-002.A 字典唯一约束、DB-011~015 有界删除、DB-016/017 结构级验证、
+#       ZS-BPM-001 BPM 独立装配与 PG 验收（两阶段）。
 
 on:
   push:
@@ -18,5 +19,13 @@ jobs:
       - uses: actions/setup-node@v4
         with:
           node-version: 22
+      - uses: actions/setup-java@v4
+        with:
+          distribution: temurin
+          java-version: 17
+      # ZS-BPM-001 第 9 套件：BPM 独立构建消费本地仓库产物，须先安装当前提交的兄弟依赖模块
+      # （根 reactor 保持 BPM 注释态=关闭模块，ModuleWhitelistTest 门禁不变）
+      - name: 安装兄弟依赖模块产物
+        run: mvn -B -f services/zhongshu-core/pom.xml -pl zszj-module-system,zszj-module-infra -am install -Dmaven.test.skip=true
       - name: 运行 PG 技术回归套件
         run: node scripts/db/run-pg-regression.mjs
diff --git a/scripts/db/run-bpm001-verify.mjs b/scripts/db/run-bpm001-verify.mjs
index f0db1dd8..0ab1155e 100644
--- a/scripts/db/run-bpm001-verify.mjs
+++ b/scripts/db/run-bpm001-verify.mjs
@@ -5,25 +5,30 @@
  *   阶段1 bootstrap（zhongshu_owner，schema-update=true，异步执行器挂起）：
  *     - 空库上由 Flowable 引擎自建 ACT_ 表（建表责任=迁移期 owner 账号）；
  *     - 部署中性技术夹具（重复部署幂等、变更部署出新版本）、发起运行实例、留下异步积压。
- *   阶段2 runtime（zhongshu_app 低权限，schema-update=false，异步执行器开启）：
+ *   阶段2 runtime（zhongshu_app 低权限，schema-update=false，异步执行器挂起待 R70 显式启停）：
  *     - 新 JVM 重启接入同库（重启恢复：定义/实例/历史全部可见、schema 版本不变=运行期零 DDL）；
- *     - 通过/拒绝/撤回/转办/分页历史、Spring 事务回滚不留引擎痕迹、积压任务被消化且无死信。
- * 结构检查（编排器 psql 直证，不经 Java）：
- *   S0 关闭 BPM 无副作用：System/Infra 基线（全部 Flyway V* 应用后）零 ACT_/FLW_ 表；
- *   S1 低权限运行账号无 DDL：zhongshu_app 建表必须失败；
- *   S2 引擎表数量与 schema.version 前后一致（阶段2 未改结构）。
+ *     - 通过/拒绝/撤回/转办/分页历史、Spring 事务回滚不留引擎痕迹、积压任务被显式启停的执行器消化且无死信。
+ * 结构检查（编排器 psql 直证，不经 Java；全部查询强制成功，失败即 FAIL 不吞错）：
+ *   S0 关闭 BPM 结构零残留：System/Infra 基线（全部 Flyway V* 应用后）零 ACT_/FLW_ 表
+ *      （运行期装配面证明=既有 V1.6 真实启动联验 + ModuleWhitelistTest 静态门禁，见决策文档边界）；
+ *   S1 低权限运行账号无 DDL：app 连接可用（SELECT 1 成功）且 CREATE TABLE 以 SQLSTATE 42501 被拒；
+ *   S2 引擎表数量与 schema.version 前后一致（阶段2 未改结构）；
+ *   S3 异步回声探针落库。
+ * 工具链：优先仓库 tools/（worktree 供给）；否则使用 PATH 上的 java/mvn（CI 已由工作流 provision）；
+ *   两者皆缺以退出码 3 明确失败，不静默跳过。套件前置 `mvn install` 兄弟依赖模块
+ *   （system/infra 及其依赖链，-Dmaven.test.skip=true），保证 BPM 独立构建消费的是当前提交的产物。
  *
  * 语义（对齐 scripts/db/run-db018-verify.mjs / test-pg-fixture.mjs 夹具约定）：
- *   - 实例隔离：容器名/端口随机唯一，进程退出自动清理；
+ *   - 实例隔离：容器名随机唯一，验后强制清理（docker rm -f -v，重试后验证不存在）；
  *   - 失败非零：任一阶段/检查失败 → 非零退出；
- *   - 缺依赖不静默跳过：Docker 不可用 → 明确报错并以退出码 3 结束；
- *   - Docker 负载 flaky：容器启动失败重试一次。
+ *   - 缺依赖不静默跳过：Docker/工具链不可用 → 退出码 3；
+ *   - Docker 负载 flaky：容器启动失败先清理残件、换端口重试一次。
  * 证据报告：JSON 摘要落 outputs/bpm-001/runtime-report.json（outputs/ 不入库）。
  *
  * 用法：node scripts/db/run-bpm001-verify.mjs
  */
 import { execFileSync, spawnSync } from 'node:child_process';
-import { readFileSync, existsSync, mkdirSync, writeFileSync, readdirSync } from 'node:fs';
+import { readFileSync, existsSync, mkdirSync, writeFileSync, readdirSync, rmSync } from 'node:fs';
 import { fileURLToPath } from 'node:url';
 import { join } from 'node:path';
 
@@ -34,54 +39,99 @@ const outDir = join(root, 'outputs', 'bpm-001');
 
 function fail(code, message) { console.error(message); process.exit(code); }
 
-// ---- 工具链与 Docker 预检（缺依赖不静默跳过）----
+// ---- 工具链解析：tools/ 优先（本地/worktree），否则 PATH（CI 由工作流 provision），皆缺退出码 3 ----
 const toolsDir = join(root, 'tools');
 const jdkDir = join(toolsDir, 'jdk-17.0.20.1+1');
 const mavenBin = join(toolsDir, 'apache-maven-3.9.9', 'bin');
-if (!existsSync(jdkDir) || !existsSync(mavenBin)) {
-  fail(3, `[bpm001] 工具链缺失（${jdkDir} / ${mavenBin}）：worktree 须供给 tools/`);
+let javaHomeEnv, pathPrefix;
+if (existsSync(jdkDir) && existsSync(mavenBin)) {
+  javaHomeEnv = jdkDir;
+  pathPrefix = process.platform === 'win32' ? `${jdkDir}\\bin;${mavenBin};` : `${jdkDir}/bin:${mavenBin}:`;
+} else {
+  const probe = spawnSync('mvn', ['-v'], { encoding: 'utf8' });
+  const hasJava = spawnSync(process.platform === 'win32' ? 'java.exe' : 'java', ['-version'], { encoding: 'utf8' });
+  if (probe.error || probe.status !== 0 || hasJava.error || hasJava.status !== 0) {
+    fail(3, `[bpm001] 工具链缺失：无 tools/ 供给且 PATH 上无可用 mvn/java（本地请供给 tools/，CI 由 pg-regression.yml provision）`);
+  }
+  javaHomeEnv = process.env.JAVA_HOME;
+  pathPrefix = '';
 }
+const childEnv = {
+  ...process.env,
+  ...(javaHomeEnv ? { JAVA_HOME: javaHomeEnv } : {}),
+  PATH: `${pathPrefix}${process.env.PATH}`,
+};
+
+// ---- Docker 预检（缺依赖不静默跳过）----
 const dockerUp = spawnSync('docker', ['version', '--format', '{{.Server.Version}}'], { encoding: 'utf8' });
 if (dockerUp.error || dockerUp.status !== 0) {
   fail(3, `[bpm001] Docker 不可用（${dockerUp.error?.message ?? `exit=${dockerUp.status}`}）：验证不得静默跳过`);
 }
 
 const container = `zszj-bpm001-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
-const port = 4332 + Math.floor(Math.random() * 700);
+let port = 4332 + Math.floor(Math.random() * 700);
 let cleaned = false;
-const cleanup = () => { if (!cleaned) { cleaned = true; try { execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { } } };
-process.on('exit', cleanup);
-process.on('SIGINT', () => { cleanup(); process.exit(130); });
+let cleanupError = '';
+const cleanup = () => {
+  if (cleaned) return;
+  for (let attempt = 0; attempt < 3 && !cleaned; attempt++) {
+    const r = spawnSync('docker', ['rm', '-f', '-v', container], { encoding: 'utf8' });
+    if (r.status === 0) { cleaned = true; break; }
+    cleanupError = (r.stderr ?? '').trim();
+    Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500);
+  }
+};
+const onSignal = (signal) => { cleanup(); process.exit(signal === 'SIGINT' ? 130 : 143); };
+process.on('exit', () => { if (!cleaned) cleanup(); });
+process.on('SIGINT', () => onSignal('SIGINT'));
+process.on('SIGTERM', () => onSignal('SIGTERM'));
 
-const psql = (user, db, sql) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', user, '-d', db,
+// ---- psql 严格封装：任何查询失败/空结果都必须显式暴露，不吞错 ----
+const psqlRun = (user, db, sql) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', user, '-d', db,
   '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 });
-const psqlOut = (user, db, sql, host) => spawnSync('docker', ['exec', container, 'psql', ...(host ? ['-h', host] : []), '-U', user, '-d', db, '-At', '-c', sql],
-  { encoding: 'utf8', maxBuffer: 8 * 1024 * 1024 });
-const psqlOk = (user, db, sql) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', user, '-d', db,
-  '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql, encoding: 'utf8' }).status === 0;
+function psqlScalar(user, db, sql) {
+  const r = spawnSync('docker', ['exec', container, 'psql', '-U', user, '-d', db, '-At', '-c', sql],
+    { encoding: 'utf8', maxBuffer: 8 * 1024 * 1024 });
+  if (r.error || r.status !== 0) throw new Error(`psql 查询失败（exit=${r.status ?? 'ERR'}）：${((r.stderr ?? '') || (r.stdout ?? '')).slice(0, 300)}`);
+  const value = (r.stdout ?? '').trim();
+  if (value === '') throw new Error(`psql 查询返回空：${sql.slice(0, 120)}`);
+  return value;
+}
+function psqlScalarInt(user, db, sql) {
+  const value = psqlScalar(user, db, sql);
+  const n = Number(value);
+  if (!Number.isInteger(n) || n < 0) throw new Error(`期望非负整数，实际「${value}」：${sql.slice(0, 120)}`);
+  return n;
+}
 
 let pass = 0, failCount = 0;
 const results = [];
 const record = (id, ok, note = '') => { results.push({ id, ok, note }); ok ? pass++ : failCount++; console.log(`[${ok ? 'PASS' : 'FAIL'}] ${id} ${note}`); };
 
-// ---- 拉起一次性 PG（负载 flaky 重试一次）----
+// ---- 拉起一次性 PG（负载 flaky：清理残件+换端口重试一次）----
 function dockerRunOnce() {
   const args = ['run', '-d', '--name', container, '-e', 'POSTGRES_PASSWORD=bpm001', '-p', `127.0.0.1:${port}:5432`, 'postgres:17-alpine'];
-  if (process.platform === 'win32') {
-    return spawnSync('cmd.exe', ['/d', '/s', '/c', 'docker ' + args.join(' ')], { encoding: 'utf8' }).status === 0;
-  }
-  return spawnSync('docker', args, { encoding: 'utf8' }).status === 0;
+  const r = process.platform === 'win32'
+    ? spawnSync('cmd.exe', ['/d', '/s', '/c', 'docker ' + args.join(' ')], { encoding: 'utf8' })
+    : spawnSync('docker', args, { encoding: 'utf8' });
+  if (r.status === 0) return true;
+  console.error(`[bpm001] docker run 失败（port=${port}）：${((r.stderr ?? '') || '').trim().slice(0, 200)}`);
+  return false;
 }
 console.log(`[bpm001] 拉起临时 PG（${container} @ 127.0.0.1:${port}）…`);
-if (!dockerRunOnce() && !dockerRunOnce()) {
-  fail(3, '[bpm001] PG 夹具容器两次启动失败（Docker 负载竞态按约定重试一次后仍失败）');
+if (!dockerRunOnce()) {
+  spawnSync('docker', ['rm', '-f', '-v', container], { stdio: 'ignore' });
+  port = 4332 + Math.floor(Math.random() * 700);
+  console.log(`[bpm001] 重试：新端口 ${port}`);
+  if (!dockerRunOnce()) fail(3, '[bpm001] PG 夹具容器两次启动失败（Docker 负载竞态按约定重试一次后仍失败）');
 }
 
 let ready = false;
 // 就绪探测必须走 TCP（-h 127.0.0.1）：initdb 期间的临时服务器只监听 unix socket，
 // socket 探测可能误判就绪导致 CREATE DATABASE 落到临时库上失败
 for (let i = 0; i < 40; i++) {
-  if (psqlOut('postgres', 'postgres', 'SELECT 1', '127.0.0.1').status === 0) { ready = true; break; }
+  const r = spawnSync('docker', ['exec', container, 'psql', '-h', '127.0.0.1', '-U', 'postgres', '-d', 'postgres', '-At', '-c', 'SELECT 1'], { encoding: 'utf8' });
+  if (r.status === 0) { ready = true; break; }
   Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500);
 }
 if (!ready) { cleanup(); fail(1, '[bpm001] PG 未就绪'); }
@@ -90,37 +140,41 @@ if (!ready) { cleanup(); fail(1, '[bpm001] PG 未就绪'); }
 {
   let created = false, last = '';
   for (let i = 0; i < 10 && !created; i++) {
-    const r = spawnSync('docker', ['exec', container, 'psql', '-h', '127.0.0.1', '-U', 'postgres', '-q', '-c', 'CREATE DATABASE zhongshu;'], { encoding: 'utf8' });
+    const r = psqlRun('postgres', 'postgres', 'CREATE DATABASE zhongshu;');
     created = r.status === 0;
-    if (!created) { last = (r.stderr ?? '') + (r.stdout ?? ''); Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 1000); }
+    if (!created) { last = ((r.stderr ?? '') + (r.stdout ?? '')).trim(); Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 1000); }
   }
   if (!created) { cleanup(); fail(1, `[bpm001] 建库失败（重试 10 次）：${last.slice(0, 400)}`); }
 }
 {
   const setup = readFileSync(join(core, 'sql', 'postgresql', 'env-setup-test.sql'), 'utf8');
-  const r = psql('postgres', 'zhongshu', setup);
+  const r = psqlRun('postgres', 'zhongshu', setup);
   if (r.status !== 0) { cleanup(); fail(1, `[bpm001] 角色授权失败:\n${r.stdout}${r.stderr}`); }
 }
 {
   const migrations = readdirSync(migrationDir).filter((f) => /^V.*\.sql$/.test(f)).sort();
   const bundled = migrations.map((f) => readFileSync(join(migrationDir, f), 'utf8')).join('\n');
-  const r = psql('zhongshu_owner', 'zhongshu', bundled);
+  const r = psqlRun('zhongshu_owner', 'zhongshu', bundled);
   if (r.status !== 0) { cleanup(); fail(1, `[bpm001] 基线迁移执行失败（${migrations.length} 个 V*）:\n${r.stdout}${r.stderr}`); }
   console.log(`[bpm001] 已应用基线迁移 ${migrations.length} 个：${migrations.join(', ')}`);
 }
-if (spawnSync('docker', ['exec', container, 'psql', '-U', 'zhongshu_owner', '-d', 'zhongshu', '-q', '-c',
-  'CREATE TABLE bpm_harness_probe(id bigserial PRIMARY KEY, note text NOT NULL, created_at timestamptz NOT NULL DEFAULT now());'], { encoding: 'utf8' }).status !== 0) {
+if (psqlRun('zhongshu_owner', 'zhongshu',
+  'CREATE TABLE bpm_harness_probe(id bigserial PRIMARY KEY, note text NOT NULL, created_at timestamptz NOT NULL DEFAULT now());').status !== 0) {
   cleanup(); fail(1, '[bpm001] 探针表创建失败');
 }
 
+// ---- 兄弟依赖模块产物安装（BPM 独立构建不在 reactor，消费本地仓库产物须来自当前提交）----
+console.log('[bpm001] mvn install 兄弟依赖模块（system/infra 及依赖链，跳过测试编译）…');
+const installArgs = ['-B', '-pl', 'zszj-module-system,zszj-module-infra', '-am', 'install', '-Dmaven.test.skip=true'];
+const install = spawnSync(process.platform === 'win32' ? 'cmd.exe' : 'mvn',
+  process.platform === 'win32' ? ['/d', '/s', '/c', 'mvn ' + installArgs.join(' ')] : installArgs,
+  { cwd: core, env: childEnv, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024, timeout: 20 * 60 * 1000 });
+if (install.status !== 0) {
+  cleanup();
+  fail(1, '[bpm001] 兄弟依赖模块安装失败：\n' + ((install.stdout ?? '') + (install.stderr ?? '')).split('\n').slice(-25).join('\n'));
+}
+
 // ---- Maven 子进程（Windows 下 Node spawn .cmd 有 EINVAL 防护，须经 cmd.exe）----
-const childEnv = {
-  ...process.env,
-  JAVA_HOME: jdkDir,
-  PATH: process.platform === 'win32'
-    ? `${jdkDir}\\bin;${mavenBin};${process.env.PATH}`
-    : `${jdkDir}/bin:${mavenBin}:${process.env.PATH}`,
-};
 function runHarnessTest(testClass, username, password, schemaUpdate, asyncExecutor, timeoutMs) {
   const args = ['-B', '-f', 'zszj-module-bpm/pom.xml', 'test', `-Dtest=${testClass}`];
   const cmd = `mvn ${args.join(' ')}`;
@@ -139,76 +193,124 @@ function runHarnessTest(testClass, username, password, schemaUpdate, asyncExecut
     });
   return { ...r, output: (r.stdout ?? '') + (r.stderr ?? '') };
 }
-function surefireSummary(testClass) {
-  const xml = join(core, 'zszj-module-bpm', 'target', 'surefire-reports',
-    `TEST-cn.zszj.module.bpm.harness.${testClass}.xml`);
-  if (!existsSync(xml)) return null;
-  const text = readFileSync(xml, 'utf8');
-  const attr = (name) => Number((text.match(new RegExp(`${name}="(\\d+)"`)) ?? [])[1] ?? -1);
-  return { tests: attr('tests'), failures: attr('failures'), errors: attr('errors'), skipped: attr('skipped') };
+
+// surefire 严格校验：先删本轮报告（防陈旧文件复用），跑后要求存在、身份匹配、
+// 用例数精确、零跳过零失败零错误
+const reportXml = (testClass) => join(core, 'zszj-module-bpm', 'target', 'surefire-reports',
+  `TEST-cn.zszj.module.bpm.harness.${testClass}.xml`);
+function assertSurefire(testClass, expectedTests) {
+  const xml = reportXml(testClass);
+  rmSync(xml, { force: true });
+  return () => {
+    if (!existsSync(xml)) throw new Error(`本轮 surefire 报告未生成：${xml}`);
+    const text = readFileSync(xml, 'utf8');
+    const attr = (name) => (text.match(new RegExp(`${name}="(\\d+)"`)) ?? [])[1];
+    const suiteName = (text.match(/name="([^"]+)"/) ?? [])[1];
+    if (suiteName !== `cn.zszj.module.bpm.harness.${testClass}`) throw new Error(`报告身份不符：${suiteName}`);
+    const tests = Number(attr('tests')), failures = Number(attr('failures')),
+      errors = Number(attr('errors')), skipped = Number(attr('skipped'));
+    if (tests !== expectedTests) throw new Error(`用例数 ${tests} ≠ 预期 ${expectedTests}`);
+    if (skipped !== 0) throw new Error(`存在跳过用例 ${skipped}（必测集不得跳过）`);
+    if (failures !== 0 || errors !== 0) throw new Error(`failures=${failures} errors=${errors}`);
+    return `tests=${tests} failures=0 errors=0 skipped=0`;
+  };
 }
-const countTables = (pattern) => Number((psqlOut('postgres', 'zhongshu',
-  `SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND lower(table_name) LIKE '${pattern}'`).stdout ?? '0').trim()) || 0;
 
-// ---- S0：关闭 BPM 的结构性证明——System/Infra 全基线零流程表 ----
-{
-  const act = countTables('act\\_%'), flw = countTables('flw\\_%');
-  record('S0 关闭BPM无副作用：基线零 ACT_/FLW_ 表', act === 0 && flw === 0, `ACT_=${act} FLW_=${flw}`);
+const countActFlw = (pattern) => psqlScalarInt('postgres', 'zhongshu',
+  `SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND lower(table_name) LIKE '${pattern}'`);
+const schemaVersion = () => psqlScalar('postgres', 'zhongshu', "SELECT VALUE_ FROM ACT_GE_PROPERTY WHERE NAME_='schema.version'");
+
+// ---- S0：关闭 BPM 结构零残留——System/Infra 全基线零流程表（查询失败即 FAIL）----
+let actAfterBootstrap = -1, versionAfterBootstrap = '';
+try {
+  const act = countActFlw('act\\_%'), flw = countActFlw('flw\\_%');
+  record('S0 关闭BPM结构零残留：基线零 ACT_/FLW_ 表', act === 0 && flw === 0, `ACT_=${act} FLW_=${flw}`);
+} catch (e) {
+  record('S0 关闭BPM结构零残留：基线零 ACT_/FLW_ 表', false, String(e.message ?? e));
 }
-// ---- S1：低权限运行账号无 DDL ----
+
+// ---- S1：低权限运行账号连接可用且无 DDL（区分权限拒绝 SQLSTATE 42501 与基础设施错误）----
+// psql 默认 verbosity 不回显 SQLSTATE，须 verbose 才能对 42501 做精确判定（PG15+ 建表被拒为 schema 级）
 {
-  const r = psqlOk('zhongshu_app', 'zhongshu', 'CREATE TABLE zszj_app_should_fail(id int);');
-  record('S1 运行账号无DDL：zhongshu_app 建表被拒', r === false, r ? 'app 竟然建表成功' : '按预期失败');
+  let ok = false, note = '';
+  try {
+    psqlScalar('zhongshu_app', 'zhongshu', 'SELECT 1');
+    const r = spawnSync('docker', ['exec', '-i', container, 'psql', '-U', 'zhongshu_app', '-d', 'zhongshu',
+      '-v', 'ON_ERROR_STOP=1', '-q'], { input: '\\set VERBOSITY verbose\nCREATE TABLE zszj_app_should_fail(id int);', encoding: 'utf8' });
+    const output = ((r.stderr ?? '') + (r.stdout ?? '')).trim();
+    if (r.status === 0) { note = 'app 竟然建表成功'; }
+    else if (output.includes('42501')) { ok = true; note = '建表按 SQLSTATE 42501（权限不足）被拒，app 连接本身可用'; }
+    else { note = `建表失败但非权限拒绝：${output.slice(0, 160)}`; }
+  } catch (e) { note = String(e.message ?? e); }
+  record('S1 运行账号无DDL：app 连接可用且建表被 42501 拒绝', ok, note);
 }
 
 // ---- 阶段1 bootstrap（owner 账号：建表 + 部署 + 发起 + 留积压）----
 console.log('[bpm001] 阶段1 bootstrap：owner 账号建引擎表/部署中性夹具/发起实例/留异步积压…');
 mkdirSync(outDir, { recursive: true });
-const bootLog = runHarnessTest('BpmPgHarnessBootstrapTest', 'zhongshu_owner', 'owner_local_1', 'true', 'false', 20 * 60 * 1000);
+const bootAssert = assertSurefire('BpmPgHarnessBootstrapTest', 5);
+const bootLog = runHarnessTest('BpmPgHarnessBootstrapTest', 'zhongshu_owner', 'owner_local_1', 'true', 'false', 30 * 60 * 1000);
 writeFileSync(join(outDir, 'bootstrap.log'), bootLog.output);
-const bootSummary = surefireSummary('BpmPgHarnessBootstrapTest');
-const bootOk = bootLog.status === 0 && bootSummary && bootSummary.tests > 0 && bootSummary.failures === 0 && bootSummary.errors === 0;
-record('P1 bootstrap 引擎建表/部署幂等/发起/租户标签/版本钉住', bootOk,
-  bootSummary ? `tests=${bootSummary.tests} failures=${bootSummary.failures} errors=${bootSummary.errors} skipped=${bootSummary.skipped}` : `exit=${bootLog.status}（无 surefire 报告）`);
+let bootOk = false, bootNote = '';
+try { bootNote = bootAssert(); bootOk = bootLog.status === 0; if (!bootOk) bootNote += `，但 mvn exit=${bootLog.status}`; }
+catch (e) { bootNote = String(e.message ?? e); }
+record('P1 bootstrap 引擎建表/部署幂等/发起/租户标签/版本钉住', bootOk, bootNote);
 if (!bootOk) {
   console.error('[bpm001] bootstrap 阶段失败，日志见 outputs/bpm-001/bootstrap.log（尾部）：\n' + bootLog.output.split('\n').slice(-40).join('\n'));
   finish();
 }
 
-const actAfterBootstrap = countTables('act\\_%');
-const schemaVersionAfterBootstrap = (psqlOut('postgres', 'zhongshu', "SELECT VALUE_ FROM ACT_GE_PROPERTY WHERE NAME_='schema.version'").stdout ?? '').trim();
+try {
+  actAfterBootstrap = countActFlw('act\\_%');
+  versionAfterBootstrap = schemaVersion();
+} catch (e) {
+  console.error('[bpm001] 引导后结构读取失败：' + String(e.message ?? e));
+  finish();
+}
 
 // ---- 阶段2 runtime（app 低权限：重启恢复 + 全操作断言 + 零 DDL；执行器由 R70 显式启停）----
 console.log('[bpm001] 阶段2 runtime：app 低权限重启接入同库/审批操作/事务回滚/积压恢复…');
-const runLog = runHarnessTest('BpmPgHarnessRuntimeTest', 'zhongshu_app', 'app_local_1', 'false', 'false', 15 * 60 * 1000);
+const runAssert = assertSurefire('BpmPgHarnessRuntimeTest', 9);
+const runLog = runHarnessTest('BpmPgHarnessRuntimeTest', 'zhongshu_app', 'app_local_1', 'false', 'false', 20 * 60 * 1000);
 writeFileSync(join(outDir, 'runtime.log'), runLog.output);
-const runSummary = surefireSummary('BpmPgHarnessRuntimeTest');
-const runOk = runLog.status === 0 && runSummary && runSummary.tests > 0 && runSummary.failures === 0 && runSummary.errors === 0;
-record('P2 runtime 重启恢复/通过/拒绝/撤回/转办/分页历史/事务回滚/积压恢复', runOk,
-  runSummary ? `tests=${runSummary.tests} failures=${runSummary.failures} errors=${runSummary.errors} skipped=${runSummary.skipped}` : `exit=${runLog.status}（无 surefire 报告）`);
+let runOk = false, runNote = '';
+try { runNote = runAssert(); runOk = runLog.status === 0; if (!runOk) runNote += `，但 mvn exit=${runLog.status}`; }
+catch (e) { runNote = String(e.message ?? e); }
+record('P2 runtime 重启恢复/通过/拒绝/撤回/转办/分页历史/事务回滚/积压恢复', runOk, runNote);
 if (!runOk) {
   console.error('[bpm001] runtime 阶段失败，日志见 outputs/bpm-001/runtime.log（尾部）：\n' + runLog.output.split('\n').slice(-40).join('\n'));
 }
 
-// ---- S2：运行期零 DDL（表数量与 schema.version 不变）+ 探针落库 ----
+// ---- S2/S3：运行期零 DDL（表数量与 schema.version 不变）+ 探针落库 ----
+{
+  let ok = false, note = '';
+  try {
+    const actAfterRuntime = countActFlw('act\\_%');
+    const versionAfterRuntime = schemaVersion();
+    ok = actAfterBootstrap === actAfterRuntime && versionAfterBootstrap === versionAfterRuntime;
+    note = `ACT_ ${actAfterBootstrap}→${actAfterRuntime}, schema.version ${versionAfterBootstrap}→${versionAfterRuntime}`;
+  } catch (e) { note = String(e.message ?? e); }
+  record('S2 运行期零DDL：引擎表数量与 schema.version 不变', ok, note);
+}
 {
-  const actAfterRuntime = countTables('act\\_%');
-  const schemaVersionAfterRuntime = (psqlOut('postgres', 'zhongshu', "SELECT VALUE_ FROM ACT_GE_PROPERTY WHERE NAME_='schema.version'").stdout ?? '').trim();
-  const probeRows = Number((psqlOut('postgres', 'zhongshu', 'SELECT count(*) FROM bpm_harness_probe').stdout ?? '0').trim()) || 0;
-  record('S2 运行期零DDL：引擎表数量与 schema.version 不变',
-    actAfterBootstrap === actAfterRuntime && schemaVersionAfterBootstrap === schemaVersionAfterRuntime,
-    `ACT_ ${actAfterBootstrap}→${actAfterRuntime}, schema.version ${schemaVersionAfterBootstrap}→${schemaVersionAfterRuntime}`);
-  record('S3 异步回声探针落库（owner 引导版本 + app 运行期回声）', probeRows >= 3, `探针行=${probeRows}`);
+  let ok = false, note = '';
+  try {
+    const probeRows = psqlScalarInt('postgres', 'zhongshu', 'SELECT count(*) FROM bpm_harness_probe');
+    ok = probeRows >= 3;
+    note = `探针行=${probeRows}`;
+  } catch (e) { note = String(e.message ?? e); }
+  record('S3 异步回声探针落库（owner 引导版本 + app 运行期回声）', ok, note);
 }
 
 function finish() {
   cleanup();
+  if (!cleaned) record('S4 夹具容器清理', false, `docker rm 重试后仍失败：${cleanupError}`);
   console.log('\n===== ZS-BPM-001 BPM 独立装配与 PG 验收汇总 =====');
   for (const r of results) console.log(`${r.ok ? 'PASS' : 'FAIL'}  ${r.id} ${r.note}`);
   console.log(`合计 ${results.length} 项，失败 ${failCount} 项`);
   try {
     writeFileSync(join(outDir, 'runtime-report.json'),
-      JSON.stringify({ finishedAt: new Date().toISOString(), container: cleaned ? container : `${container}(运行中)`, results, pass, failCount }, null, 2));
+      JSON.stringify({ finishedAt: new Date().toISOString(), container, results, pass, failCount }, null, 2));
   } catch { /* 报告落盘失败不影响退出码语义 */ }
   process.exit(failCount === 0 ? 0 : 1);
 }
diff --git a/services/zhongshu-core/docs/BPM引擎表与业务扩展表迁移责任决策.md b/services/zhongshu-core/docs/BPM引擎表与业务扩展表迁移责任决策.md
index 692c678e..bee6e878 100644
--- a/services/zhongshu-core/docs/BPM引擎表与业务扩展表迁移责任决策.md
+++ b/services/zhongshu-core/docs/BPM引擎表与业务扩展表迁移责任决策.md
@@ -36,7 +36,7 @@
 | 重启可恢复 | 阶段 2 以独立 JVM、`schema-update=false`、低权限账号重启接入同库：定义/实例/历史/schema.version 全部一致（R10） | PASS |
 | 事务失败可恢复 | R60：Spring `DataSourceTransactionManager` 回滚事务内的发起不留引擎痕迹；提交事务正常持久化 | PASS |
 | 异步执行器与启停 | bootstrap 执行器挂起留积压（`ACT_RU_JOB`=1、探针零记录）→ runtime 重启后积压跨重启持久保留（执行器不自启），显式 `asyncExecutor.start()` 消化积压且无死信、实时路径再验、`shutdown()` 回到非活动态（R70，isActive 直证） | PASS |
-| 关闭 BPM 无副作用 | S0：System/Infra 全基线零 `ACT_`/`FLW_` 表；server POM/根 reactor 保持 BPM 注释 + `ModuleWhitelistTest` 门禁不变 | PASS |
+| 关闭 BPM 无副作用 | 结构面（本套件 S0）：System/Infra 全基线零 `ACT_`/`FLW_` 表；装配面（既有证据）：server POM/根 reactor 保持 BPM 注释 + `ModuleWhitelistTest` 门禁 + V1.6 记录的真实 PG17+Redis 启动联验（BPM 关闭态下 server 正常装配、关闭模块请求被拒）。启动后零流程副作用的运行期观测归启用 BPM 的批次做开/关对照时补测，本件不作此宣称 | PASS（结构与装配面） |
 | 装配扩展点同构直证 | `BpmPgHarnessConfiguration` 与 `BpmFlowableConfiguration` 同用 `SpringProcessEngineConfiguration` + `setEventListeners` 扩展点，R90 证明监听器在真实 PG 引擎触发 | PASS |
 
 两阶段账号/参数：阶段 1 `zhongshu_owner` + `SCHEMA_UPDATE=true` + `ASYNC_EXECUTOR=false`；
@@ -50,8 +50,15 @@
 - zszj-module-bpm 不在默认 reactor（根 POM 注释态），套件以 `mvn -f zszj-module-bpm/pom.xml`
   独立构建，兄弟模块依赖取自本地仓库已安装产物；**不通过根 POM profile 启用 reactor 成员**
   （`ModuleWhitelistTest` 以根 POM 非注释 `<module>` 为门禁事实源，引入 profile 会被判违规）。
+  因此套件每次运行前先 `mvn install -pl zszj-module-system,zszj-module-infra -am
+  -Dmaven.test.skip=true`（保证消费当前提交的产物，规避「陈旧构件使跨模块测试失真」）；
+  CI（pg-regression.yml）同步 provision JDK 17 与该安装步骤，本地无 tools/ 供给时回退
+  PATH 上的 mvn/java，皆缺退出码 3。
 - 就绪探测必须走 TCP（`-h 127.0.0.1`）：postgres 镜像 initdb 期间的临时服务器只监听
   unix socket，socket 探测可能误判就绪导致建库落到临时服务器失败（2026-09-14 首跑实测）。
+  **移交给量**：本套件已按此修复；`run-db006/008-verify.mjs` 等既有套件仍是 socket 探测，
+  2026-09-14 聚合首跑中二者再次竞态失败、单独复跑即绿——历史登记的「Docker 负载 flaky」
+  相当部分可能即此根因，建议后续批次统一改为 TCP 探测（属各套件所属卡，不在本件改动）。
 
 ## 4. 边界与后续
 
diff --git a/services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessConfiguration.java b/services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessConfiguration.java
index 31c92cd9..17478791 100644
--- a/services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessConfiguration.java
+++ b/services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessConfiguration.java
@@ -64,10 +64,11 @@ public class BpmPgHarnessConfiguration {
         if (!url.startsWith("jdbc:postgresql://")) {
             throw new IllegalStateException("[bpm-pg-harness] 夹具仅面向真实 PostgreSQL，收到 " + url);
         }
+        // 全部环境参数在创建连接池前完成校验；密码缺失同样快速失败（不静默回退空口令）
         HikariConfig config = new HikariConfig();
         config.setJdbcUrl(url);
         config.setUsername(requireEnv(ENV_USERNAME));
-        config.setPassword(System.getenv(ENV_PASSWORD) == null ? "" : System.getenv(ENV_PASSWORD));
+        config.setPassword(requireEnv(ENV_PASSWORD));
         config.setMaximumPoolSize(4);
         config.setMinimumIdle(1);
         config.setPoolName("bpm-pg-harness");
diff --git a/services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessRuntimeTest.java b/services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessRuntimeTest.java
index 6ce0e930..7ea1058d 100644
--- a/services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessRuntimeTest.java
+++ b/services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessRuntimeTest.java
@@ -26,6 +26,7 @@ import static org.junit.jupiter.api.Assertions.assertEquals;
 import static org.junit.jupiter.api.Assertions.assertFalse;
 import static org.junit.jupiter.api.Assertions.assertNotNull;
 import static org.junit.jupiter.api.Assertions.assertNull;
+import static org.junit.jupiter.api.Assertions.assertThrows;
 import static org.junit.jupiter.api.Assertions.assertTrue;
 
 /**
@@ -94,6 +95,11 @@ class BpmPgHarnessRuntimeTest {
                 .processInstanceId(instance.getId()).finished().singleResult();
         assertNotNull(historic, "审批通过后实例应进入历史");
         assertEquals(1, historic.getProcessDefinitionVersion(), "旧实例应在原版本 v1 上完成（版本钉住）");
+        // 结束分支直证：approve 条件必须真实路由到 approvedEnd，不得误接 rejectedEnd 仍伪绿
+        assertEquals(1, historyService.createHistoricActivityInstanceQuery()
+                .processInstanceId(instance.getId()).activityId("approvedEnd").count(), "应恰好经过 approvedEnd");
+        assertEquals(0, historyService.createHistoricActivityInstanceQuery()
+                .processInstanceId(instance.getId()).activityId("rejectedEnd").count(), "通过路径不得经过 rejectedEnd");
         assertEquals(BpmPgHarness.OUTCOME_APPROVE, historyService.createHistoricVariableInstanceQuery()
                 .processInstanceId(instance.getId()).variableName(BpmPgHarness.VAR_OUTCOME)
                 .singleResult().getValue());
@@ -141,6 +147,13 @@ class BpmPgHarnessRuntimeTest {
                 .taskId(task.getId()).singleResult().getAssignee(), "任务历史应记录转办后的受理人");
     }
 
+    /** 回滚实验专用信号：与发起路径可能抛出的任何其它异常明确区分（防止把基础设施错误当回滚路径吞掉）。 */
+    private static final class HarnessRollbackSignal extends RuntimeException {
+        private HarnessRollbackSignal() {
+            super("harness: 模拟业务事务失败");
+        }
+    }
+
     @Test
     @Order(60)
     void springTransactionRollbackLeavesNoEngineTrace() {
@@ -150,20 +163,29 @@ class BpmPgHarnessRuntimeTest {
                 runtimeService.startProcessInstanceByKeyAndTenantId(
                         BpmPgHarness.PROCESS_APPROVAL, "bpm001-A7", null, BpmPgHarness.TENANT_1));
         assertEquals(before + 1, runtimeService.createProcessInstanceQuery().count());
-        // 实验组：回滚事务内的发起不留下任何引擎痕迹（证明引擎走 Spring 事务管理器）
-        try {
-            transactionTemplate.executeWithoutResult(status -> {
-                runtimeService.startProcessInstanceByKeyAndTenantId(
-                        BpmPgHarness.PROCESS_APPROVAL, "bpm001-A8", null, BpmPgHarness.TENANT_1);
-                throw new IllegalStateException("harness: 模拟业务事务失败");
-            });
-        } catch (IllegalStateException expected) {
-            // 由回滚断言承接
-        }
+        // 实验组：先确认事务内发起已成功，再抛专用信号触发回滚
+        assertThrows(HarnessRollbackSignal.class, () ->
+                transactionTemplate.executeWithoutResult(status -> {
+                    ProcessInstance started = runtimeService.startProcessInstanceByKeyAndTenantId(
+                            BpmPgHarness.PROCESS_APPROVAL, "bpm001-A8", null, BpmPgHarness.TENANT_1);
+                    assertNotNull(started, "回滚前事务内发起应已成功（不得由发起自身异常冒充回滚路径）");
+                    throw new HarnessRollbackSignal();
+                }));
+        // 回滚零残留：运行实例/历史实例/历史任务三面均无 A8 痕迹（历史写入与运行态同事务，一并回滚）
         assertEquals(before + 1, runtimeService.createProcessInstanceQuery().count(),
                 "回滚事务内的发起不应持久化");
-        assertNull(runtimeService.createProcessInstanceQuery()
-                .processInstanceBusinessKey("bpm001-A8").singleResult());
+        assertEquals(0, runtimeService.createProcessInstanceQuery()
+                .processInstanceBusinessKey("bpm001-A8").count());
+        assertEquals(0, historyService.createHistoricProcessInstanceQuery()
+                .processInstanceBusinessKey("bpm001-A8").count(), "历史实例不应有 A8 残留");
+        assertEquals(0, historyService.createHistoricTaskInstanceQuery()
+                .processInstanceBusinessKey("bpm001-A8").count(), "历史任务不应有 A8 残留");
+        // 回滚后引擎完好：同一操作重试并提交即成功（完整恢复直证）
+        ProcessInstance retried = transactionTemplate.execute(tx ->
+                runtimeService.startProcessInstanceByKeyAndTenantId(
+                        BpmPgHarness.PROCESS_APPROVAL, "bpm001-A9", null, BpmPgHarness.TENANT_1));
+        assertNotNull(retried, "回滚后重试发起应成功");
+        assertEquals(before + 2, runtimeService.createProcessInstanceQuery().count());
     }
 
     @Test
@@ -196,17 +218,31 @@ class BpmPgHarnessRuntimeTest {
     @Test
     @Order(80)
     void historyAndRuntimeQueriesSupportPaging() {
-        List<HistoricProcessInstance> page = historyService.createHistoricProcessInstanceQuery()
+        // 分页直证：按唯一列（实例 ID）排序保证全序，OFFSET 页与全量切片精确相等，无重复无遗漏
+        List<String> finishedIds = historyService.createHistoricProcessInstanceQuery()
                 .finished()
-                .orderByProcessInstanceEndTime().asc()
-                .listPage(0, 2);
-        assertEquals(2, page.size(), "分页历史查询应返回整页");
-        assertTrue(historyService.createHistoricProcessInstanceQuery().finished().count() >= 4,
-                "通过/拒绝/撤回/转办完成共 4 个实例应进入历史");
-        assertEquals(2, historyService.createHistoricTaskInstanceQuery().finished()
-                .listPage(0, 2).size(), "任务历史分页应可用");
-        assertTrue(runtimeService.createProcessInstanceQuery()
-                .listPage(0, 3).size() <= 3, "运行实例分页应可用");
+                .orderByProcessInstanceId().asc()
+                .list().stream().map(HistoricProcessInstance::getId).toList();
+        assertTrue(finishedIds.size() >= 6, "通过/拒绝/撤回/转办 4 审批 + 2 异步共 6 实例应全部进入历史");
+        List<String> historyPage1 = historyService.createHistoricProcessInstanceQuery()
+                .finished().orderByProcessInstanceId().asc().listPage(0, 2)
+                .stream().map(HistoricProcessInstance::getId).toList();
+        List<String> historyPage2 = historyService.createHistoricProcessInstanceQuery()
+                .finished().orderByProcessInstanceId().asc().listPage(2, 2)
+                .stream().map(HistoricProcessInstance::getId).toList();
+        assertEquals(finishedIds.subList(0, 2), historyPage1, "OFFSET=0 页应精确等于全量前 2 条");
+        assertEquals(finishedIds.subList(2, 4), historyPage2, "OFFSET=2 页应精确等于全量第 3~4 条");
+        assertTrue(historyPage1.stream().noneMatch(historyPage2::contains), "两页不得重复");
+
+        // 运行面：精确计数（A5/A6/A7/A9，A8 已回滚零残留）+ 非零 OFFSET 精确切片
+        List<String> runningIds = runtimeService.createProcessInstanceQuery()
+                .orderByProcessInstanceId().asc()
+                .list().stream().map(ProcessInstance::getId).toList();
+        assertEquals(4, runningIds.size(), "运行实例应恰为 A5/A6/A7/A9");
+        List<String> runningOffsetPage = runtimeService.createProcessInstanceQuery()
+                .orderByProcessInstanceId().asc().listPage(1, 3)
+                .stream().map(ProcessInstance::getId).toList();
+        assertEquals(runningIds.subList(1, 4), runningOffsetPage, "OFFSET=1 页应精确等于全量去掉首条");
     }
 
     @Test

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'Get-Content -Encoding utf8 services/zhongshu-core/zszj-module-bpm/pom.xml; Get-Content -Encoding utf8 services/zhongshu-core/pom.xml; Get-Content -Encoding utf8 scripts/db/run-pg-regression.mjs; Get-Content -Encoding utf8 services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessConfiguration.java' in E:\zszj-wt-bpm-001-a
 succeeded in 0ms:
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
package cn.zszj.module.bpm.harness;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.spring.ProcessEngineFactoryBean;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

/**
 * ZS-BPM-001 ��׼�� BPM ����װ�䣺�ֹ�װ�� SpringProcessEngineConfiguration + Spring �����������
 * �� BpmFlowableConfiguration ������������װ�����ͬ����ͬһ��չ�㣩���������� System ģ����
 * ��ѡ�˲��ԣ������ʸ�� ZS-BPM-002����ר����������ʵ PostgreSQL �ϵĽ����������⻧��ǩ��
 * �첽ִ��������ͣ��
 *
 * ������������ scripts/db/run-bpm001-verify.mjs ���׶�ע�룬ȱʧ������ʧ�ܡ�����Ĭ��������
 *   ZSZJ_BPM_HARNESS_JDBC_URL      ���� jdbc:postgresql:// ��ͷ��
 *   ZSZJ_BPM_HARNESS_USERNAME      bootstrap �׶�=zhongshu_owner��runtime �׶�=zhongshu_app����Ȩ�ޣ���
 *   ZSZJ_BPM_HARNESS_PASSWORD
 *   ZSZJ_BPM_HARNESS_SCHEMA_UPDATE true=�����Թܽ���/�������� owner Ǩ�ƽ׶Σ���false=�����ڲ��Ľṹ��
 *   ZSZJ_BPM_HARNESS_ASYNC_EXECUTOR true=�����첽ִ������runtime ��ѹ�ָ�����false=��������ѹ��bootstrap����
 */
@Configuration(proxyBeanMethods = false)
public class BpmPgHarnessConfiguration {

    static final String ENV_URL = "ZSZJ_BPM_HARNESS_JDBC_URL";
    static final String ENV_USERNAME = "ZSZJ_BPM_HARNESS_USERNAME";
    static final String ENV_PASSWORD = "ZSZJ_BPM_HARNESS_PASSWORD";
    static final String ENV_SCHEMA_UPDATE = "ZSZJ_BPM_HARNESS_SCHEMA_UPDATE";
    static final String ENV_ASYNC_EXECUTOR = "ZSZJ_BPM_HARNESS_ASYNC_EXECUTOR";

    static String requireEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("[bpm-pg-harness] ȱ�ٻ������� " + key + "����֤���þ�Ĭ���������� run-bpm001-verify.mjs ע��");
        }
        return value.trim();
    }

    static boolean requireFlag(String key) {
        String value = requireEnv(key);
        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
            throw new IllegalStateException("[bpm-pg-harness] �������� " + key + " ��Ϊ true/false��ʵ��=" + value);
        }
        return Boolean.parseBoolean(value);
    }

    @Bean(destroyMethod = "close")
    public HikariDataSource bpmHarnessDataSource() {
        String url = requireEnv(ENV_URL);
        if (!url.startsWith("jdbc:postgresql://")) {
            throw new IllegalStateException("[bpm-pg-harness] �о߽�������ʵ PostgreSQL���յ� " + url);
        }
        // ȫ�����������ڴ������ӳ�ǰ���У�飻����ȱʧͬ������ʧ�ܣ�����Ĭ���˿տ��
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(requireEnv(ENV_USERNAME));
        config.setPassword(requireEnv(ENV_PASSWORD));
        config.setMaximumPoolSize(4);
        config.setMinimumIdle(1);
        config.setPoolName("bpm-pg-harness");
        // ������ zszj-spring-boot-starter-mybatis ���� PG ����
        config.setDriverClassName("org.postgresql.Driver");
        return new HikariDataSource(config);
    }

    @Bean
    public PlatformTransactionManager bpmHarnessTransactionManager(DataSource dataSource) {
        // ��Ӧ��������̬ͬ����������ҵ���� Spring DataSourceTransactionManager ������߽�
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public TransactionTemplate bpmHarnessTransactionTemplate(PlatformTransactionManager txManager) {
        return new TransactionTemplate(txManager);
    }

    @Bean
    public SpringProcessEngineConfiguration bpmHarnessProcessEngineConfiguration(
            DataSource dataSource, PlatformTransactionManager transactionManager) {
        SpringProcessEngineConfiguration configuration = new SpringProcessEngineConfiguration();
        configuration.setDataSource(dataSource);
        configuration.setTransactionManager(transactionManager);
        // �����Ǩ�����Σ�bootstrap �׶Σ�owner �˺ţ����������Խ�/������runtime �׶Σ�app ��Ȩ�ޣ���ֹ�Ľṹ
        configuration.setDatabaseSchemaUpdate(requireFlag(ENV_SCHEMA_UPDATE)
                ? ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE
                : ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE);
        configuration.setAsyncExecutorActivate(requireFlag(ENV_ASYNC_EXECUTOR));
        configuration.setDeploymentName("bpmPgHarness");
        // �� BpmFlowableConfiguration ��ͬ����չ�㣺ע�������¼�����
        configuration.setEventListeners(java.util.List.of(new BpmPgHarness.EngineEventRecorder()));
        return configuration;
    }

    @Bean
    public ProcessEngineFactoryBean bpmHarnessProcessEngine(SpringProcessEngineConfiguration configuration) {
        ProcessEngineFactoryBean factoryBean = new ProcessEngineFactoryBean();
        factoryBean.setProcessEngineConfiguration(configuration);
        return factoryBean;
    }

    @Bean
    public RepositoryService repositoryService(ProcessEngine processEngine) {
        return processEngine.getRepositoryService();
    }

    @Bean
    public RuntimeService runtimeService(ProcessEngine processEngine) {
        return processEngine.getRuntimeService();
    }

    @Bean
    public TaskService taskService(ProcessEngine processEngine) {
        return processEngine.getTaskService();
    }

    @Bean
    public HistoryService historyService(ProcessEngine processEngine) {
        return processEngine.getHistoryService();
    }

    @Bean
    public ManagementService managementService(ProcessEngine processEngine) {
        return processEngine.getManagementService();
    }

    @Bean
    public org.flowable.job.service.impl.asyncexecutor.AsyncExecutor bpmHarnessAsyncExecutor(
            SpringProcessEngineConfiguration configuration) {
        // �����湹��ʹ��ͬһʵ����configuration ���Դ���Ĭ��ִ����������������ʽ��ͣ��isActive/start/shutdown��
        return configuration.getAsyncExecutor();
    }

    @Bean
    public JdbcTemplate bpmHarnessJdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public NeutralEchoDelegate neutralEchoDelegate(DataSource dataSource) {
        return new NeutralEchoDelegate(dataSource);
    }
}

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'git diff HEAD''^ HEAD -- services/zhongshu-core/zszj-module-bpm; git -c core.quotepath=false diff HEAD''^ HEAD -- services/zhongshu-core/docs; node --version; Get-Command java,mvn,docker -ErrorAction SilentlyContinue | Select-Object Name,Source; Get-ChildItem -Name tools -ErrorAction SilentlyContinue' in E:\zszj-wt-bpm-001-a
 succeeded in 50ms:

Name       Source                                                                              
----       ------                                                                              
docker.exe C:\Users\Administrator\AppData\Local\Programs\DockerDesktop\resources\bin\docker.exe
apache-maven-3.9.9
jdk-17.0.20.1+1
env.sh
jdk17.zip
maven.zip



exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "node -e 'const fs=require(\"fs\"); for(const f of [\"services/zhongshu-core/zszj-module-bpm/pom.xml\",\"services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessRuntimeTest.java\",\"services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessBootstrapTest.java\"]){process.stdout.write(\"\\nFILE \"+f+\"\\n\");process.stdout.write(fs.readFileSync(f,\"utf8\").split(\"\\n\").map((s,i)=>(i+1)+\": \"+s).join(\"\\n\"));}'" in E:\zszj-wt-bpm-001-a
 exited 1 in 0ms:
[eval]:1
const fs=require(fs); for(const f of [services/zhongshu-core/zszj-module-bpm/pom.xml,services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessRuntimeTest.java,services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessBootstrapTest.java]){process.stdout.write(\nFILE
                                                                                                                                                                                                                                                                                                                                        ^
Expected unicode escape

SyntaxError: Invalid or unexpected token
    at makeContextifyScript (node:internal/vm:194:14)
    at compileScript (node:internal/process/execution:388:10)
    at evalTypeScript (node:internal/process/execution:260:22)
    at node:internal/main/eval_string:71:3

Node.js v24.19.0

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "rg -n -C 3 'zszj-|<modules>|<module>|surefire|flatten|maven.test.skip|skipTests' services/zhongshu-core/pom.xml services/zhongshu-core/zszj-module-system/pom.xml services/zhongshu-core/zszj-module-infra/pom.xml; rg --files outputs; rg -n 'V1.6|真实.*启动|ModuleWhitelistTest' docs services/zhongshu-core/docs -g '*.md'" in E:\zszj-wt-bpm-001-a
 succeeded in 0ms:
services/zhongshu-core/pom.xml-7-    <artifactId>zszj</artifactId>
services/zhongshu-core/pom.xml-8-    <version>${revision}</version>
services/zhongshu-core/pom.xml-9-    <packaging>pom</packaging>
services/zhongshu-core/pom.xml:10:    <modules>
services/zhongshu-core/pom.xml:11:        <module>zszj-dependencies</module>
services/zhongshu-core/pom.xml:12:        <module>zszj-framework</module>
services/zhongshu-core/pom.xml-13-        <!-- Server 主项目 -->
services/zhongshu-core/pom.xml:14:        <module>zszj-server</module>
services/zhongshu-core/pom.xml-15-        <!-- 各种 module 拓展 -->
services/zhongshu-core/pom.xml:16:        <module>zszj-module-system</module>
services/zhongshu-core/pom.xml:17:        <module>zszj-module-infra</module>
services/zhongshu-core/pom.xml:18:<!--        <module>zszj-module-member</module>-->
services/zhongshu-core/pom.xml:19:<!--        <module>zszj-module-bpm</module>-->
services/zhongshu-core/pom.xml:20:<!--        <module>zszj-module-report</module>-->
services/zhongshu-core/pom.xml:21:<!--        <module>zszj-module-mp</module>-->
services/zhongshu-core/pom.xml:22:<!--        <module>zszj-module-pay</module>-->
services/zhongshu-core/pom.xml:23:<!--        <module>zszj-module-mall</module>-->
services/zhongshu-core/pom.xml:24:<!--        <module>zszj-module-crm</module>-->
services/zhongshu-core/pom.xml:25:<!--        <module>zszj-module-erp</module>-->
services/zhongshu-core/pom.xml:26:<!--        <module>zszj-module-iot</module>-->
services/zhongshu-core/pom.xml:27:<!--        <module>zszj-module-mes</module>-->
services/zhongshu-core/pom.xml:28:<!--        <module>zszj-module-wms</module>-->
services/zhongshu-core/pom.xml:29:<!--        <module>zszj-module-hrm</module>-->
services/zhongshu-core/pom.xml:30:<!--        <module>zszj-module-fms</module>-->
services/zhongshu-core/pom.xml:31:<!--        <module>zszj-module-pms</module>-->
services/zhongshu-core/pom.xml:32:<!--        <module>zszj-module-im</module>-->
services/zhongshu-core/pom.xml-33-<!-- 请参考 https://doc.iocoder.cn/ai/build/ 文档，完成 AI 模块的启动！！！ -->
services/zhongshu-core/pom.xml:34:<!--        <module>zszj-module-ai</module>-->
services/zhongshu-core/pom.xml-35-    </modules>
services/zhongshu-core/pom.xml-36-
services/zhongshu-core/pom.xml-37-    <name>${project.artifactId}</name>
--
services/zhongshu-core/pom.xml-44-        <java.version>17</java.version>
services/zhongshu-core/pom.xml-45-        <maven.compiler.source>${java.version}</maven.compiler.source>
services/zhongshu-core/pom.xml-46-        <maven.compiler.target>${java.version}</maven.compiler.target>
services/zhongshu-core/pom.xml:47:        <maven-surefire-plugin.version>3.5.3</maven-surefire-plugin.version>
services/zhongshu-core/pom.xml-48-        <maven-compiler-plugin.version>3.14.0</maven-compiler-plugin.version>
services/zhongshu-core/pom.xml:49:        <flatten-maven-plugin.version>1.7.2</flatten-maven-plugin.version>
services/zhongshu-core/pom.xml:50:        <!-- maven-surefire-plugin 暂时无法通过 bom 的依赖读取（兼容老版本 IDEA 2024 及以前版本） -->
services/zhongshu-core/pom.xml-51-        <lombok.version>1.18.42</lombok.version>
services/zhongshu-core/pom.xml-52-        <spring.boot.version>3.5.15</spring.boot.version>
services/zhongshu-core/pom.xml-53-        <mapstruct.version>1.6.3</mapstruct.version>
--
services/zhongshu-core/pom.xml-58-        <dependencies>
services/zhongshu-core/pom.xml-59-            <dependency>
services/zhongshu-core/pom.xml-60-                <groupId>cn.zszj</groupId>
services/zhongshu-core/pom.xml:61:                <artifactId>zszj-dependencies</artifactId>
services/zhongshu-core/pom.xml-62-                <version>${revision}</version>
services/zhongshu-core/pom.xml-63-                <type>pom</type>
services/zhongshu-core/pom.xml-64-                <scope>import</scope>
--
services/zhongshu-core/pom.xml-69-    <build>
services/zhongshu-core/pom.xml-70-        <pluginManagement>
services/zhongshu-core/pom.xml-71-            <plugins>
services/zhongshu-core/pom.xml:72:                <!-- maven-surefire-plugin 插件，用于运行单元测试。 -->
services/zhongshu-core/pom.xml-73-                <!-- 注意，需要使用 3.0.X+，因为要支持 Junit 5 版本 -->
services/zhongshu-core/pom.xml-74-                <plugin>
services/zhongshu-core/pom.xml-75-                    <groupId>org.apache.maven.plugins</groupId>
services/zhongshu-core/pom.xml:76:                    <artifactId>maven-surefire-plugin</artifactId>
services/zhongshu-core/pom.xml:77:                    <version>${maven-surefire-plugin.version}</version>
services/zhongshu-core/pom.xml-78-                </plugin>
services/zhongshu-core/pom.xml-79-                <!-- maven-compiler-plugin 插件，解决 spring-boot-configuration-processor + Lombok + MapStruct 组合 -->
services/zhongshu-core/pom.xml-80-                <!-- https://stackoverflow.com/questions/33483697/re-run-spring-boot-configuration-annotation-processor-to-update-generated-metada -->
--
services/zhongshu-core/pom.xml-116-                </plugin>
services/zhongshu-core/pom.xml-117-                <plugin>
services/zhongshu-core/pom.xml-118-                    <groupId>org.codehaus.mojo</groupId>
services/zhongshu-core/pom.xml:119:                    <artifactId>flatten-maven-plugin</artifactId>
services/zhongshu-core/pom.xml-120-                </plugin>
services/zhongshu-core/pom.xml-121-            </plugins>
services/zhongshu-core/pom.xml-122-        </pluginManagement>
--
services/zhongshu-core/pom.xml-152-            <!-- 统一 revision 版本 -->
services/zhongshu-core/pom.xml-153-            <plugin>
services/zhongshu-core/pom.xml-154-                <groupId>org.codehaus.mojo</groupId>
services/zhongshu-core/pom.xml:155:                <artifactId>flatten-maven-plugin</artifactId>
services/zhongshu-core/pom.xml:156:                <version>${flatten-maven-plugin.version}</version>
services/zhongshu-core/pom.xml-157-                <configuration>
services/zhongshu-core/pom.xml:158:                    <flattenMode>oss</flattenMode>
services/zhongshu-core/pom.xml-159-                    <updatePomFile>true</updatePomFile>
services/zhongshu-core/pom.xml-160-                </configuration>
services/zhongshu-core/pom.xml-161-                <executions>
services/zhongshu-core/pom.xml-162-                    <execution>
services/zhongshu-core/pom.xml-163-                        <goals>
services/zhongshu-core/pom.xml:164:                            <goal>flatten</goal>
services/zhongshu-core/pom.xml-165-                        </goals>
services/zhongshu-core/pom.xml:166:                        <id>flatten</id>
services/zhongshu-core/pom.xml-167-                        <phase>process-resources</phase>
services/zhongshu-core/pom.xml-168-                    </execution>
services/zhongshu-core/pom.xml-169-                    <execution>
services/zhongshu-core/pom.xml-170-                        <goals>
services/zhongshu-core/pom.xml-171-                            <goal>clean</goal>
services/zhongshu-core/pom.xml-172-                        </goals>
services/zhongshu-core/pom.xml:173:                        <id>flatten.clean</id>
services/zhongshu-core/pom.xml-174-                        <phase>clean</phase>
services/zhongshu-core/pom.xml-175-                    </execution>
services/zhongshu-core/pom.xml-176-                </executions>
--
services/zhongshu-core/zszj-module-infra/pom.xml-8-        <version>${revision}</version>
services/zhongshu-core/zszj-module-infra/pom.xml-9-    </parent>
services/zhongshu-core/zszj-module-infra/pom.xml-10-    <modelVersion>4.0.0</modelVersion>
services/zhongshu-core/zszj-module-infra/pom.xml:11:    <artifactId>zszj-module-infra</artifactId>
services/zhongshu-core/zszj-module-infra/pom.xml-12-    <packaging>jar</packaging>
services/zhongshu-core/zszj-module-infra/pom.xml-13-
services/zhongshu-core/zszj-module-infra/pom.xml-14-    <name>${project.artifactId}</name>
--
services/zhongshu-core/zszj-module-infra/pom.xml-22-        <!-- 业务组件 -->
services/zhongshu-core/zszj-module-infra/pom.xml-23-        <dependency>
services/zhongshu-core/zszj-module-infra/pom.xml-24-            <groupId>cn.zszj</groupId>
services/zhongshu-core/zszj-module-infra/pom.xml:25:            <artifactId>zszj-spring-boot-starter-biz-tenant</artifactId>
services/zhongshu-core/zszj-module-infra/pom.xml-26-        </dependency>
services/zhongshu-core/zszj-module-infra/pom.xml-27-
services/zhongshu-core/zszj-module-infra/pom.xml-28-        <!-- Web 相关 -->
services/zhongshu-core/zszj-module-infra/pom.xml-29-        <dependency>
services/zhongshu-core/zszj-module-infra/pom.xml-30-            <groupId>cn.zszj</groupId>
services/zhongshu-core/zszj-module-infra/pom.xml:31:            <artifactId>zszj-spring-boot-starter-security</artifactId>
services/zhongshu-core/zszj-module-infra/pom.xml-32-        </dependency>
services/zhongshu-core/zszj-module-infra/pom.xml-33-
services/zhongshu-core/zszj-module-infra/pom.xml-34-        <dependency>
services/zhongshu-core/zszj-module-infra/pom.xml-35-            <groupId>cn.zszj</groupId>
services/zhongshu-core/zszj-module-infra/pom.xml:36:            <artifactId>zszj-spring-boot-starter-websocket</artifactId>
services/zhongshu-core/zszj-module-infra/pom.xml-37-        </dependency>
services/zhongshu-core/zszj-module-infra/pom.xml-38-
services/zhongshu-core/zszj-module-infra/pom.xml-39-        <!-- DB 相关 -->
services/zhongshu-core/zszj-module-infra/pom.xml-40-        <dependency>
services/zhongshu-core/zszj-module-infra/pom.xml-41-            <groupId>cn.zszj</groupId>
services/zhongshu-core/zszj-module-infra/pom.xml:42:            <artifactId>zszj-spring-boot-starter-mybatis</artifactId>
services/zhongshu-core/zszj-module-infra/pom.xml-43-        </dependency>
services/zhongshu-core/zszj-module-infra/pom.xml-44-        <dependency>
services/zhongshu-core/zszj-module-infra/pom.xml-45-            <groupId>com.baomidou</groupId>
--
services/zhongshu-core/zszj-module-infra/pom.xml-48-
services/zhongshu-core/zszj-module-infra/pom.xml-49-        <dependency>
services/zhongshu-core/zszj-module-infra/pom.xml-50-            <groupId>cn.zszj</groupId>
services/zhongshu-core/zszj-module-infra/pom.xml:51:            <artifactId>zszj-spring-boot-starter-redis</artifactId>
services/zhongshu-core/zszj-module-infra/pom.xml-52-        </dependency>
services/zhongshu-core/zszj-module-infra/pom.xml-53-
services/zhongshu-core/zszj-module-infra/pom.xml-54-        <!-- Config 配置中心相关 -->
--
services/zhongshu-core/zszj-module-infra/pom.xml-56-        <!-- Job 定时任务相关 -->
services/zhongshu-core/zszj-module-infra/pom.xml-57-        <dependency>
services/zhongshu-core/zszj-module-infra/pom.xml-58-            <groupId>cn.zszj</groupId>
services/zhongshu-core/zszj-module-infra/pom.xml:59:            <artifactId>zszj-spring-boot-starter-job</artifactId>
services/zhongshu-core/zszj-module-infra/pom.xml-60-        </dependency>
services/zhongshu-core/zszj-module-infra/pom.xml-61-
services/zhongshu-core/zszj-module-infra/pom.xml-62-        <!-- 消息队列相关 -->
services/zhongshu-core/zszj-module-infra/pom.xml-63-        <dependency>
services/zhongshu-core/zszj-module-infra/pom.xml-64-            <groupId>cn.zszj</groupId>
services/zhongshu-core/zszj-module-infra/pom.xml:65:            <artifactId>zszj-spring-boot-starter-mq</artifactId>
services/zhongshu-core/zszj-module-infra/pom.xml-66-        </dependency>
services/zhongshu-core/zszj-module-infra/pom.xml-67-
services/zhongshu-core/zszj-module-infra/pom.xml-68-        <!-- Test 测试相关 -->
services/zhongshu-core/zszj-module-infra/pom.xml-69-        <dependency>
services/zhongshu-core/zszj-module-infra/pom.xml-70-            <groupId>cn.zszj</groupId>
services/zhongshu-core/zszj-module-infra/pom.xml:71:            <artifactId>zszj-spring-boot-starter-test</artifactId>
services/zhongshu-core/zszj-module-infra/pom.xml-72-            <scope>test</scope>
services/zhongshu-core/zszj-module-infra/pom.xml-73-        </dependency>
services/zhongshu-core/zszj-module-infra/pom.xml-74-
services/zhongshu-core/zszj-module-infra/pom.xml-75-        <!-- 工具类相关 -->
services/zhongshu-core/zszj-module-infra/pom.xml-76-        <dependency>
services/zhongshu-core/zszj-module-infra/pom.xml-77-            <groupId>cn.zszj</groupId>
services/zhongshu-core/zszj-module-infra/pom.xml:78:            <artifactId>zszj-spring-boot-starter-excel</artifactId>
services/zhongshu-core/zszj-module-infra/pom.xml-79-        </dependency>
services/zhongshu-core/zszj-module-infra/pom.xml-80-
services/zhongshu-core/zszj-module-infra/pom.xml-81-        <dependency>
--
services/zhongshu-core/zszj-module-infra/pom.xml-86-        <!-- 监控相关 -->
services/zhongshu-core/zszj-module-infra/pom.xml-87-        <dependency>
services/zhongshu-core/zszj-module-infra/pom.xml-88-            <groupId>cn.zszj</groupId>
services/zhongshu-core/zszj-module-infra/pom.xml:89:            <artifactId>zszj-spring-boot-starter-monitor</artifactId>
services/zhongshu-core/zszj-module-infra/pom.xml-90-        </dependency>
services/zhongshu-core/zszj-module-infra/pom.xml-91-
services/zhongshu-core/zszj-module-infra/pom.xml-92-        <dependency>
--
services/zhongshu-core/zszj-module-system/pom.xml-8-        <version>${revision}</version>
services/zhongshu-core/zszj-module-system/pom.xml-9-    </parent>
services/zhongshu-core/zszj-module-system/pom.xml-10-    <modelVersion>4.0.0</modelVersion>
services/zhongshu-core/zszj-module-system/pom.xml:11:    <artifactId>zszj-module-system</artifactId>
services/zhongshu-core/zszj-module-system/pom.xml-12-    <packaging>jar</packaging>
services/zhongshu-core/zszj-module-system/pom.xml-13-
services/zhongshu-core/zszj-module-system/pom.xml-14-    <name>${project.artifactId}</name>
--
services/zhongshu-core/zszj-module-system/pom.xml-20-    <dependencies>
services/zhongshu-core/zszj-module-system/pom.xml-21-        <dependency>
services/zhongshu-core/zszj-module-system/pom.xml-22-            <groupId>cn.zszj</groupId>
services/zhongshu-core/zszj-module-system/pom.xml:23:            <artifactId>zszj-module-infra</artifactId>
services/zhongshu-core/zszj-module-system/pom.xml-24-            <version>${revision}</version>
services/zhongshu-core/zszj-module-system/pom.xml-25-        </dependency>
services/zhongshu-core/zszj-module-system/pom.xml-26-
services/zhongshu-core/zszj-module-system/pom.xml-27-        <!-- 业务组件 -->
services/zhongshu-core/zszj-module-system/pom.xml-28-        <dependency>
services/zhongshu-core/zszj-module-system/pom.xml-29-            <groupId>cn.zszj</groupId>
services/zhongshu-core/zszj-module-system/pom.xml:30:            <artifactId>zszj-spring-boot-starter-biz-data-permission</artifactId>
services/zhongshu-core/zszj-module-system/pom.xml-31-        </dependency>
services/zhongshu-core/zszj-module-system/pom.xml-32-        <dependency>
services/zhongshu-core/zszj-module-system/pom.xml-33-            <groupId>cn.zszj</groupId>
services/zhongshu-core/zszj-module-system/pom.xml:34:            <artifactId>zszj-spring-boot-starter-biz-tenant</artifactId>
services/zhongshu-core/zszj-module-system/pom.xml-35-        </dependency>
services/zhongshu-core/zszj-module-system/pom.xml-36-        <dependency>
services/zhongshu-core/zszj-module-system/pom.xml-37-            <groupId>cn.zszj</groupId>
services/zhongshu-core/zszj-module-system/pom.xml:38:            <artifactId>zszj-spring-boot-starter-biz-ip</artifactId>
services/zhongshu-core/zszj-module-system/pom.xml-39-        </dependency>
services/zhongshu-core/zszj-module-system/pom.xml-40-
services/zhongshu-core/zszj-module-system/pom.xml-41-        <!-- Web 相关 -->
services/zhongshu-core/zszj-module-system/pom.xml-42-        <dependency>
services/zhongshu-core/zszj-module-system/pom.xml-43-            <groupId>cn.zszj</groupId>
services/zhongshu-core/zszj-module-system/pom.xml:44:            <artifactId>zszj-spring-boot-starter-security</artifactId>
services/zhongshu-core/zszj-module-system/pom.xml-45-        </dependency>
services/zhongshu-core/zszj-module-system/pom.xml-46-
services/zhongshu-core/zszj-module-system/pom.xml-47-        <!-- 服务保障：限流/幂等（ZS-SEC-010：AuthController @RateLimiter 需要） -->
services/zhongshu-core/zszj-module-system/pom.xml-48-        <dependency>
services/zhongshu-core/zszj-module-system/pom.xml-49-            <groupId>cn.zszj</groupId>
services/zhongshu-core/zszj-module-system/pom.xml:50:            <artifactId>zszj-spring-boot-starter-protection</artifactId>
services/zhongshu-core/zszj-module-system/pom.xml-51-        </dependency>
services/zhongshu-core/zszj-module-system/pom.xml-52-
services/zhongshu-core/zszj-module-system/pom.xml-53-        <dependency>
--
services/zhongshu-core/zszj-module-system/pom.xml-58-        <!-- DB 相关 -->
services/zhongshu-core/zszj-module-system/pom.xml-59-        <dependency>
services/zhongshu-core/zszj-module-system/pom.xml-60-            <groupId>cn.zszj</groupId>
services/zhongshu-core/zszj-module-system/pom.xml:61:            <artifactId>zszj-spring-boot-starter-mybatis</artifactId>
services/zhongshu-core/zszj-module-system/pom.xml-62-        </dependency>
services/zhongshu-core/zszj-module-system/pom.xml-63-
services/zhongshu-core/zszj-module-system/pom.xml-64-        <dependency>
services/zhongshu-core/zszj-module-system/pom.xml-65-            <groupId>cn.zszj</groupId>
services/zhongshu-core/zszj-module-system/pom.xml:66:            <artifactId>zszj-spring-boot-starter-redis</artifactId>
services/zhongshu-core/zszj-module-system/pom.xml-67-        </dependency>
services/zhongshu-core/zszj-module-system/pom.xml-68-
services/zhongshu-core/zszj-module-system/pom.xml-69-        <!-- Job 定时任务相关 -->
services/zhongshu-core/zszj-module-system/pom.xml-70-        <dependency>
services/zhongshu-core/zszj-module-system/pom.xml-71-            <groupId>cn.zszj</groupId>
services/zhongshu-core/zszj-module-system/pom.xml:72:            <artifactId>zszj-spring-boot-starter-job</artifactId>
services/zhongshu-core/zszj-module-system/pom.xml-73-        </dependency>
services/zhongshu-core/zszj-module-system/pom.xml-74-
services/zhongshu-core/zszj-module-system/pom.xml-75-        <!-- 消息队列相关 -->
services/zhongshu-core/zszj-module-system/pom.xml-76-        <dependency>
services/zhongshu-core/zszj-module-system/pom.xml-77-            <groupId>cn.zszj</groupId>
services/zhongshu-core/zszj-module-system/pom.xml:78:            <artifactId>zszj-spring-boot-starter-mq</artifactId>
services/zhongshu-core/zszj-module-system/pom.xml-79-        </dependency>
services/zhongshu-core/zszj-module-system/pom.xml-80-
services/zhongshu-core/zszj-module-system/pom.xml-81-        <!-- Test 测试相关 -->
services/zhongshu-core/zszj-module-system/pom.xml-82-        <dependency>
services/zhongshu-core/zszj-module-system/pom.xml-83-            <groupId>cn.zszj</groupId>
services/zhongshu-core/zszj-module-system/pom.xml:84:            <artifactId>zszj-spring-boot-starter-test</artifactId>
services/zhongshu-core/zszj-module-system/pom.xml-85-            <scope>test</scope>
services/zhongshu-core/zszj-module-system/pom.xml-86-        </dependency>
services/zhongshu-core/zszj-module-system/pom.xml-87-
services/zhongshu-core/zszj-module-system/pom.xml-88-        <!-- 工具类相关 -->
services/zhongshu-core/zszj-module-system/pom.xml-89-        <dependency>
services/zhongshu-core/zszj-module-system/pom.xml-90-            <groupId>cn.zszj</groupId>
services/zhongshu-core/zszj-module-system/pom.xml:91:            <artifactId>zszj-spring-boot-starter-excel</artifactId>
services/zhongshu-core/zszj-module-system/pom.xml-92-        </dependency>
services/zhongshu-core/zszj-module-system/pom.xml-93-
services/zhongshu-core/zszj-module-system/pom.xml-94-        <dependency>
outputs\bpm-001\runtime.log
outputs\bpm-001\runtime-report.json
outputs\bpm-001\final-run.log
outputs\bpm-001\bootstrap.log
docs\06-品牌素材与命名映射.md:48:| 1 | `cn.iocoder.yudao`（含 `cn/zszj` 路径形式） | `cn.zszj` | 全部 Java 源码 import/package、Mapper XML namespace/resultType、`META-INF/spring/*.imports`、SPI/反射字符串、logback logger、测试 | ZS-BRAND-002 | 无运行存量，直接切换；B02/B03 真实 PG 启动复验 |
docs\06-品牌素材与命名映射.md:114:- 本机暂无 JDK/Maven（ZS-ENG-002 工具链任务范围），ZS-BRAND-002 的干净构建、依赖树与真实 PG 启动验收在 B01/B02 环境补证；本轮以静态一致性检查（包声明与路径、FQCN 引用可解析、POM 坐标一致）提供初步证据并如实记录未验证项。
docs\06-品牌素材与命名映射.md:131:| ZS-BRAND-002 | 43d71fad | 后端 10,059 文件改名；静态一致性检查 0 硬性问题 | 干净构建/依赖树/真实 PG 启动（B01/B02 工具链） |
docs\05-底座模块分析与开发任务清单.md:57:V1.5 变更与验证记录（2026-09-08）：品牌与代码命名专项完成首轮开发，按第 16 节第 9 条顺序落地——ZS-BRAND-001 冻结映射（887bbc4f，[06 文档](06-品牌素材与命名映射.md)）、002 后端改名（43d71fad）、003.A 两端静态品牌（ea580b1f）、004 种子与存量迁移（581927d0）、005 代码生成（08ad8e78）、006.A 命名门禁（b2c26ea9）。验证证据：后端静态一致性检查 0 硬性问题；Web 生产构建与小程序 H5 构建通过；vue-tsc 与 04 报告基线一致（0 新增错误）；真实临时 PG17 完成新装/旧样本升级/重复迁移/失败回滚验证；品牌门禁全仓扫描 0 违规、测试 7/7。批次放行边界不变：002 的干净构建/依赖树/真实 PG 启动待 B01/B02 工具链补证，003.B/004.B/004.C/006.B 归 B03/B05/B06 联验；本记录不表示任何主任务已验收。
docs\05-底座模块分析与开发任务清单.md:59:V1.6 变更与验证记录（2026-09-09）：M01 工程骨架六项全部完成（ZS-ENG-001 白名单 295040b3、002 JDK17 基线 f58fd09b、003 环境配置分离 38430267、004 关闭模拟认证 77b5f090、005 收紧管理端点 7c8fd2c0、006 任务启停边界 4557cc00）；M02 推进七项——ZS-DB-001.A PG 驱动与数据源合同（1f2eb7b6）、ZS-DB-003 Flyway 迁移机制（9106476f）、ZS-DB-004 V1 基线迁移与真实 PG 双库验证（391faeef）、ZS-DB-005 升级与恢复规程含 V2 qrtz 回填与三段演练（f7702928）、ZS-DB-002 环境方案与 PEND-004 清单（99e1a93c）、ZS-DB-019.A Docker 夹具自证三条语义（3a0d5d04）；另完成 ZS-CFG-001.A 秘密门禁（ebc7c9ba）、ZS-CFG-003.A 功能目录与套餐校验（2160d600）、ZS-CLIENT-005.A 构建与类型基线门禁（65272cdf）、ZS-GOV-001 文档一致性检查器（1d618920）、ZS-OPS-001.A 聚合流水线骨架（bda57b95）。持续防线：`node scripts/ops/run-local-gates.mjs` 10/10 通过（本地与 CI 同规则）。剩余 66 项待开发任务的推进前提：B02 环境（JDK 17 + Maven ≥3.8 + PostgreSQL 17，按数据库环境方案落地）解锁 M02 真实库验证与 ZS-OPS-001.B；D-07/D-09 决策解锁 M05/M06/B07~B09；B03~B06 联验环境解锁 M03/M04/M08~M12 运行验证；D-10/D-11 与终端范围解锁微信/支付/AI/App。本记录不表示任何主任务已验收。
docs\05-底座模块分析与开发任务清单.md:61:V1.6 补充记录（2026-09-09 晚，工具链引导）：在仓库 tools/ 目录自引导 Temurin JDK 17.0.20.1 + Maven 3.9.9（tools/ 不入库），随后完成首次全量真实构建（zszj-server.jar 170MB）与真实 PG17+Redis 启动联验：Flyway 实际执行 V1+V2、/actuator/health UP、关闭模块与伪 token 均 401 拒绝。构建/启动暴露并修复四类真实缺陷（Map.of 超参、补丁残留垃圾字符、缺 junit/actuator 依赖、Druid PSCache PG 兼容），详见提交 adc5b67e 与各卡开发记录。该证据使 ZS-DB-001.B 的依赖树/打包/启动项提前获得实测支撑；正式 B02 仍需负责人按数据库环境方案落地账号/备份并复验。
docs\05-底座模块分析与开发任务清单.md:63:V1.7 变更记录（2026-09-09）：按各任务卡「状态」字段（内联于「关联」行）重新计数，订正第 2 节顶部汇总——实际为 48 待开发、11 开发中、24 待验收、2 待决策、6 待前置、0 已验收（此前 V1.6 汇总 66/10/7/2/6 滞后于卡片：M01 六项与 M02 的 DB-006～020 多项已随首轮开发转「待验收」、CFG-002 转「开发中」，顶部统计未重新计数）。同步头部状态行：首轮开发已含真实运行代码与数据库迁移变更，不再表述为「未实施运行代码或数据库变更」。本次为纯文档口径订正，未改动任何任务卡实质状态、开发记录或运行代码；README 索引版本同步至 V1.7。
docs\05-底座模块分析与开发任务清单.md:69:V1.10 变更记录（2026-09-09）：ZS-SEC-002（建立接口分类、匿名白名单与方法权限清单，B03；前置 ZS-ENG-001/004/005 均已完成）按用户选定「静态清单 + 漂移基线」形态交付——新增 `ApiInventoryTest`（zszj-server/src/test，仿 `ModuleWhitelistTest` 静态源码扫描、不启动 Spring 上下文）遍历启用模块全部 Controller 生成 328 端点清单（授权类别三分 ANONYMOUS 25／AUTHENTICATED 52／PERMISSION 251，主体 ADMIN 322／MEMBER 6），与已提交基线逐行比对、漂移即失败要求评审，含 3 项结构断言（主体-前缀一致、权限标识合法、匿名端点与目录双向一致）；25 个匿名端点逐项复核均合法必需并登记 5 个残余审查点；运行时合同复用 ZS-SEC-012.A 夹具、新增第 8 组 `ApiClassificationContract`（ASYNC 首次派发不免认证 2 例 + ADMIN/MEMBER 双向串用 2 例）达 35/35，zszj-server 8/8（ApiInventoryTest 4 + ModuleWhitelistTest 4）、`run-local-gates --fast` 10/10；登记[接口清单与匿名白名单](../services/zhongshu-core/docs/接口清单与匿名白名单.md)。顺带修复 ZS-ENG-001 既有 `ModuleWhitelistTest` 断言 bug（首次以工具链运行 zszj-server 全测试时暴露，非 SEC-002 引入；详见第 3 节 ENG-001 开发记录工具链复验补录）。卡片状态 待开发→待验收，第 2 节统计随之 47/25→46/26（余不变）。边界不变：ASYNC/SSE 全面运行时合同与真实 PG/Redis 归 ZS-SEC-012.B，对象授权轴归 ZS-PERM-002.A，SEC-002 未拆分批子项。本记录不表示任何主任务已验收。
docs\05-底座模块分析与开发任务清单.md:85:- 开发记录（2026-09-09）：新增 [ModuleWhitelist](../services/zhongshu-core/zszj-server/src/main/java/cn/zszj/server/ModuleWhitelist.java) 作为唯一启用清单（启用=system/infra；13 个未启用模块及其 admin-api 前缀登记）；[DefaultController](../services/zhongshu-core/zszj-server/src/main/java/cn/zszj/server/controller/DefaultController.java) 不可用响应与白名单挂钩、不再引导上游文档；新增白名单测试 ModuleWhitelistTest（POM 激活一致性、兜底覆盖完整性、未启用包引用拦截）；新增无 JDK 静态检查 `node scripts/eng/verify-module-whitelist.mjs`（0 问题）。核对结论：未启用模块不在依赖闭包内，其 Bean/Job/自动配置无法装配；兜底响应为纯错误返回，无业务写入路径。待验收说明：菜单种子对关闭模块的可见性治理归 ZS-DB-004；测试运行与"必要模块可构建"待 B01 工具链复验。**工具链复验补录（2026-09-09，ZS-SEC-002 期间）**：首次以 tools/ 自引导 JDK17+Maven3.9.9 运行 zszj-server 全测试时，`ModuleWhitelistTest.enabledModulesMatchServerPomDependencies` 暴露既有断言缺陷——`activeModuleArtifacts` 已剥离 `zszj-module-` 前缀返回值，断言却又按该前缀 `startsWith` 过滤 server POM 依赖，actual 恒为空集致必然失败。此缺陷自 ENG-001 交付（295040b3）即存在：当时无 JDK 工具链、该 Java 测试从未运行（见本记录上一句「待 B01 工具链复验」），且门禁 G6 为 Node 版 `verify-module-whitelist.mjs`、G11 `--mvn` 仅跑 common/infra，均不触及 zszj-server Java 测试，故长期未暴露。修复：断言改为与姊妹测试 `enabledModulesMatchRootPomActiveModules` 一致的剥前缀比对（`assertEquals(new LinkedHashSet<>(ModuleWhitelist.ENABLED_MODULES), activeModuleArtifacts(read("pom.xml")))`）。修复后 zszj-server `ModuleWhitelistTest` 4/4、连同 `ApiInventoryTest` 4/4 共 8/8 BUILD SUCCESS。此为 ENG-001 显式推迟之工具链复验的履行（Java 白名单测试确可运行、必要模块依赖闭包与白名单一致），非新增需求；ENG-001 主卡状态维持 待验收。
docs\05-底座模块分析与开发任务清单.md:119:- 开发记录（2026-09-09）：[Infra SecurityConfiguration](../services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/framework/security/config/SecurityConfiguration.java) 改为仅 `/actuator/health` 匿名放行（其余 actuator 端点需认证）、删除 `/druid/**` 匿名放行；local 的 Druid 控制台 `stat-view-servlet` 由匿名开放（且未设账密）改为默认关闭；actuator 暴露白名单 local=`health,info,metrics`、dev=health、基础配置新增默认 `health`、部署模板=health；springdoc/接口文档默认关闭、仅 local 显式开启。文件读取匿名路径保留并标注归 ZS-FILE-001.A。探针实测（2026-09-09 晚）：应用真实启动后 /actuator/health 返回 {"status":"UP"}（此前 jar 无 actuator 依赖导致 404，已补 spring-boot-starter-actuator）；匿名访问反证与授权运维访问待 B03。网络边界（探针端口隔离）归部署任务。
docs\05-底座模块分析与开发任务清单.md:140:- 开发记录（2026-09-09 晚，ZS-DB-001.B 前置证据提前获得）：工具链自引导后真实执行 `mvn dependency:tree` 与 `mvn package`——依赖树含 postgresql 42.7.11(runtime)/flyway-core+pg 11.7.2、**无 mysql-connector**；打包产物 zszj-server.jar(170MB) 内含 PG/Flyway 驱动、无 MySQL 驱动；应用以真实 PG17+Redis 启动成功（48081 端口），Druid PSCache 兼容问题实测发现并关闭 pool-prepared-statements。待验收说明：.B 剩余项（低权限双账号联验、迁移账号分离运行）待正式 B02 环境与负责人授权。
docs\05-底座模块分析与开发任务清单.md:336:- 开发记录（2026-09-09，ZS-SEC-002 接口分类、匿名白名单与方法权限清单）：按用户选定「静态清单 + 漂移基线」形态交付，以**实际 Controller 源码为事实来源**、不启动 Spring 上下文。**清单生成器** `ApiInventoryTest`（置于 zszj-server/src/test，仿 `ModuleWhitelistTest` 静态源码扫描约定）遍历启用模块（`ModuleWhitelist.ENABLED_MODULES`=system/infra）全部 Controller，每端点编码「HTTP 方法｜完整路径（含 /admin-api、/app-api 前缀）｜主体类型｜授权类别｜权限标识｜模块｜Controller」一行，与已提交基线 `zszj-server/src/test/resources/api-inventory-baseline.txt`（328 端点）逐行比对——**漂移即失败**并输出新增/移除差异要求评审（`-Dapi.inventory.update=true` 仅评审通过后重新生成）。**授权类别三分**：ANONYMOUS（@PermitAll，25）／PERMISSION（@PreAuthorize hasPermission/hasRole/hasScope，251）／AUTHENTICATED（两者皆无、仅需登录的合法公共能力，52，逐项编目复核，未标 @PreAuthorize 不默认判为漏洞）。**主体类型静态可判**：包路径 `**.controller.admin.**`→/admin-api→ADMIN(322)、`**.controller.app.**`→/app-api→MEMBER(6)，与运行期 [Web 工具][E47] 依 servletPath 前缀推导一致（编译期由 [Web 自动配置][E32] 装配）。**三项结构断言**：`everyEndpointHasKnownSubjectAndApiPrefix`（主体∈{ADMIN,MEMBER} 且与前缀一致）、`permissionEndpointsHaveWellFormedPermission`（PERMISSION 权限非空且合法：RBAC 为 module:resource:action、OAuth2 开放端点为 scope 如 user.read）、`anonymousWhitelistMatchesCatalogue`（真实匿名端点必登记理由、目录每项必真实存在，双向一致）。**扫描器优于裸 grep**：@PermitAll grep=25=ANONYMOUS 精确吻合；@PreAuthorize grep=253 但真实 PERMISSION=251，差 2 为 OAuth2UserController L32-33 Javadoc 注释中的示例、扫描器按行首 `*` 正确忽略。**匿名白名单三来源核清**：①@PermitAll 注解 25 端点；②`zszj.security.permit-all-urls` 生产默认空（[Security 配置][E29]）；③`AuthorizeRequestsCustomizer` 全仓仅 WebSocket（非 REST、无 admin/app 前缀，不入清单）；`dispatcherTypeMatchers(ASYNC).permitAll()` 仅放行异步二次派发、首次 REQUEST 派发仍 `anyRequest().authenticated()`。**25 匿名端点逐项复核均合法必需、无冗余可移除**（认证入口 9／验证码 2／OAuth2 开放端点 3／短信回调 4／租户查询 3／App 公共数据 3／文件读取 1；租户 simple-list/get-by-website 仅 set id+name、敏感联系字段不填充），另登记 5 个残余审查点归后续任务（租户枚举面→SEC-006、自助注册→LOGIN-004、文件读取→FILE-001.A、短信回调验签→领域、OAuth2 凭据→SEC-003）。**运行时合同复用 ZS-SEC-012.A 夹具不重写**：新增第 8 组 `ApiClassificationContract`（4 例）——无 token 访问异步端点首次 REQUEST 派发 401（证 ASYNC permitAll 不泄漏到首次派发）、有效 token 触发 `request().asyncStarted()`、ADMIN token 访问 /app-api 403、MEMBER token 访问 /app-api code=0，补全 ADMIN/MEMBER **双向**串用矩阵与 /app-api→MEMBER 推导分支（原夹具仅 MEMBER→/admin-api 单向）；配套新增 `fixture/TestAppController`（/app-api MEMBER 主体）、`TestControllers` 加 `Callable` 异步端点，夹具达 8 组 35 用例。**验证**：`-pl :zszj-spring-boot-starter-biz-tenant -Dtest=SecurityFilterChainFixtureTest test` BUILD SUCCESS，Tests run: 35, Failures: 0, Errors: 0, Skipped: 0；`-pl :zszj-server -Dtest=ApiInventoryTest,ModuleWhitelistTest test` BUILD SUCCESS，Tests run: 8（ApiInventoryTest 4/4 + ModuleWhitelistTest 4/4）；`node scripts/ops/run-local-gates.mjs --fast` 10/10。**顺带修复既有缺陷（drive-by，归 ZS-ENG-001）**：首次以工具链运行 zszj-server 全测试暴露 `ModuleWhitelistTest.enabledModulesMatchServerPomDependencies` 既有断言 bug（详见第 3 节 ZS-ENG-001 开发记录「工具链复验补录」），非 SEC-002 引入，一并修复。**文档交付物**：[接口清单与匿名白名单](../services/zhongshu-core/docs/接口清单与匿名白名单.md)（主体推导／匿名三来源／328 端点统计与命令／25 匿名端点目录与理由／5 审查点／SEC-012.A 覆盖映射／漂移门禁评审流程／验收对齐／边界）。**对齐验收（本卡验收项）**：匿名仅能访问获准能力（目录双向门禁 + 夹具 PublicEndpoints）；缺方法权限不能操作受限动作（PERMISSION=251 入清单 + 结构断言 + 夹具 PermissionEndpoints 403）；同租户无对象授权仍拒绝（对象授权轴复用 ZS-PERM-002.A `DeptDataPermissionChecker` + 夹具 ObjectAuthorization）；ADMIN/MEMBER Token 不能串用（主体列静态判定 + 前缀一致性断言 + 新增双向 403/200）；ASYNC 不使首次受保护请求免认证（新增分类完整性 2 例）；路由或注解新增致清单变化时测试失败并要求评审（`inventoryMatchesBaseline` + `anonymousWhitelistMatchesCatalogue` 双门禁）；管理端点复用 ZS-ENG-005（actuator 仅 health、兜底路由由 `ModuleWhitelistTest` 治理）。**待验收说明**：本子项以静态清单（328 端点、4/4 结构断言）+ 漂移门禁 + 运行时合同复用（SEC-012.A 夹具 35/35，含新增 4 例）证明接口分类/匿名白名单/方法权限清单已建立并可门禁化；ASYNC/SSE **全面**运行时合同（流式响应、异步异常出口、跨线程租户上下文传播、[Web][E51]／[移动端][E52] 合同同步、真实 PG/Redis）归 ZS-SEC-012.B（SEC-002 的 2 个 ASYNC 用例仅证分类完整性、不替代 SEC-012.B），真实 PG + 完整多模块启用下的端到端联验随后续模块开发收口。SEC-002 未拆分批子项，主卡状态由 待开发 转 待验收。
docs\05-底座模块分析与开发任务清单.md:386:- 开发记录（2026-09-10，ZS-SEC-008）：按「逐入口参数校验 + 上下文头严格解析 + 请求资源限制 + 过滤器异常统一出口」交付四项调整。①**@RequestBody 参数级 @Valid 校验契约**：类级 `@Validated` 只对 `@RequestParam`/`@PathVariable` 直接约束生效、不触发 `@RequestBody` 级联 Bean 校验，修复两个同类真实缺口——`SocialUserController#socialUnbind`（`SocialUserUnbindReqVO` 已声明 `@InEnum`/`@NotNull`/`@NotEmpty`）与 `SocialClientController#sendSubscribeMessage`（`SocialWxaSubscribeMessageSendReqDTO` 已声明 `@NotNull`/`@NotEmpty`）补参数级 `@Valid`；新增静态防线 `ValidationContractTest`（zszj-server，仿 `ApiInventoryTest`/`ModuleWhitelistTest` 静态源码扫描、不启动 Spring 上下文），强制启用模块（system/infra）全部 Controller 的 `@RequestBody` 携带参数级 `@Valid`/`@Validated`，含 4 条带理由例外目录（`CaptchaController#get`/`#check` 为 `com.anji.captcha` 第三方 SDK `CaptchaVO`；`SmsCallbackController#receiveHuaweiSmsStatus`/`#receiveQiniuSmsStatus` 为 `@RequestBody String` 原文回调、交服务层内部解析验签）+ 双向防腐（未登记缺口或目录腐化均失败）+ 回归护栏（锁两缺口已带 `@Valid`）。②**上下文头严格解析**：[Web 工具][E47] 新增 `parseTenantIdHeader`，`getTenantId`/`getVisitTenantId` 改调它——缺失/空白返回 `null`，逐字符 `Character.isDigit` + `Long.parseLong` 兜溢出，畸形/溢出（`1.5`/`0x1F`/`1e5`/`-1`/超 `Long`）抛受控 `ServiceException`（业务码 400），替代旧 `NumberUtil.isNumber + Long.valueOf`（isNumber 通过但 valueOf 抛 `NumberFormatException`、在 MVC 外逃逸为容器 500 + 栈泄露）。③**过滤器异常统一出口**：`TenantContextWebFilter` 在 MVC 外、`GlobalExceptionHandler`（`@RestControllerAdvice`）捕不到，改 try-catch `ServiceException` 就地 `WebFrameworkUtils.writeJSON` 统一出口（复用 ZS-SEC-005），异常路径提前 return 不设置、`finally` 仍 `TenantContextHolder.clear()` 无残留。④**JSON 请求体缓冲上限**：[JSON 缓存 Wrapper][E46] `CacheRequestBodyFilter` 缓冲前按 `Content-Length` 判定，超 `WebProperties.RequestBody.maxCacheSize`（默认 1MB、`<=0` 不限制、键 `zszj.web.request-body.max-cache-size`）受控拒绝业务码 400；上传/流式与 `/admin/`、`/actuator/` 由 `shouldNotFilter` 排除不受限，无参构造 `-1L` 向后兼容，`ZszjWebAutoConfiguration` 注入上限。**XSS/富文本边界**：主配置 `zszj.xss.enable=false`，明确全局字符串清洗不当作权限或 SQL 防注入（SQL 注入归参数化查询、权限归 ZS-PERM），富文本清洗与前端输出编码为两端协作边界。测试：`WebFrameworkUtilsTest` 11 + `CacheRequestBodyFilterTest` 6（web，`MockHttpServletRequest`/Mock 过滤链纯单测）+ `ValidationContractTest` 3（zszj-server 静态扫描、输出恰好 4 例外）全 BUILD SUCCESS；`mvn -pl :zszj-server -am test` 反应堆 20 模块全 SUCCESS（含 system/web/biz-tenant 编译验证 `@Valid` 与过滤器改动）、`run-local-gates --fast` 10/10；登记[参数校验与请求资源限制规范](../services/zhongshu-core/docs/参数校验与请求资源限制规范.md)。**codex 评审 P1 修复（2026-09-10，commit `76da2a2f` 评审发现 1×P1：chunked 未知长度 body 绕过声明式 `Content-Length` 早拒、`CacheRequestBodyWrapper` 仍全量缓冲可耗尽堆）**：`CacheRequestBodyWrapper` 增双参构造 `(request, maxCacheSize)` 限界读取（8192 buffer 边读边累加，`total > maxCacheSize` 抛新增 `TooLargeException`）、`CacheRequestBodyFilter` 保留 ① `Content-Length` 早拒 + 新增 ② `try-catch` `TooLargeException` 转 `writeJSON` 400（两道防线共用「请求体大小超过上限」出口）、`CacheRequestBodyFilterTest` 增 2 例未知长度回归护栏（匿名 `MockHttpServletRequest` 子类覆盖 `Content-Length=-1` 模拟 chunked：超限拒绝 + 合法放行），`CacheRequestBodyFilterTest` 6→8 例、web 合计 19 例全绿 + 门禁 10/10 复验、修复已提交 `3f4fa736`、codex r1 复评（`--commit 3f4fa736`）0 发现通过；处置详见 [codex-ZS-SEC-008.md](reviews/codex-ZS-SEC-008.md)。待验收说明：本轮为代码级入口校验/解析/资源限制与单元 + 静态契约测试，网关/容器层大小与超时限制、真实链畸形输入端到端拒绝归 ZS-SEC-012.B；限流归 ZS-SEC-010、ID/时间/分页/版本兼容合同归 ZS-SEC-009；XSS 开关生产取值与富文本端到端归 ZS-SEC-012.B 及各业务入口（如公告 SYS-NOTICE）。本记录不表示任何主任务已验收。
docs\05-底座模块分析与开发任务清单.md:843:- 开发记录（2026-09-14，ZS-SYS-001.A 七类基础管理真实 PG/API 回归）：worktree 隔离分支 `feat/sys-001-a`（7 提交）`--no-ff` 合并 main（`e3f5ef29`）。交付 `scripts/sys001/run-sys001-regression.mjs` 编排器（一次性 Docker PG17+Redis 随机名/端口/退出清理/缺 Docker 退出码 3 → V1..V20260913.001 迁移 + pgcrypto 真实 BCrypt 双租户种子 → `application-harness.yaml`（mock-enable=false、验证码关闭、LOGIN-004 门控不开启、HTTP 仅绑回环）启动 zszj-server 真实进程（每次强制 clean package 防陈旧构件）→ Node fetch 直发 HTTP（双租户账密真实 Token）→ psql PG 读回断言 → markdown/JSON 报告）+ `scripts/sys001/cases/*.mjs` 八模块 **50 用例**（用户 6/角色 8/菜单 7/岗位 6/字典 7/配置 6/公告 6/跨类 4）。**矩阵终态 48/49**（全部安全断言——禁用旧 Token 401、停用角色立即失权、越界赋权/自我提权/超上限/他租户批量混入、伪造租户头、MEMBER 串用、LOGIN-004 门控——真实进程实过）。**缺口登记（证据齐全、归口清楚）**：GAP-1 真实 PG × MP 3.5.17 selectOne 光标 × Druid 1.2.28「statement 已关闭」随机 500/401（规避前 24~30 波动）→ 夹具 CLI 关 PSCache 规避（与 BRAND-004.B 独立复现互证），根因修复归 ZS-DB-001 依赖升级；GAP-2 岗位有引用删除不受控 → **本轮修复归口 ZS-IAM-003**（`3097b22f`：PostServiceImpl 注入 UserPostMapper 计数 `executeIgnore` 包裹 + `POST_EXITS_USERS` 1-002-005-004 + 批量先全校验后删 + H2 3 用例 16/16）；GAP-3 真实 PG 上 assign-role-menu 越界不拒绝不落库（PG 语句日志实证事务内无 INSERT，H2 同路径通过，疑 JacksonTypeHandler 泛型擦除 × MP 3.5.17；SYS-ROLE-N1 即此用例）→ 归 ZS-CFG-003.B 跨 PG 复验 + ZS-DB-001，依赖升级后以 SYS-ROLE-N1 复验；GAP-4 menu 深层环校验缺失 → 归 ZS-CFG-003.A/B；GAP-5 V1 个别序列 off-by-one（夹具 setval 兜底）→ 归 ZS-DB-004；公告富文本输出编码归 .B（B06）。**codex 评审弧（gpt-6-astra/xhigh）**：r0 2×P1+4×P2（强制重建防陈旧构件/mvn.cmd/回环绑定/字段对齐/断言收紧/单次读体）→ r1 1×P2 → 追交增量逐提评审（f8b7d25a 2×P2 已修、3097b22f/87e68cf2/30ec1cb9 均 0 发现）→ **全部提交 0×P0/P1**。验证：`run-local-gates --fast` 10/10、`run-pg-regression` 8/8（flaky 按约定重试）、合并后 PG 回归收口复验 8/8；处置详见 [codex-ZS-SYS-001.A.md](reviews/codex-ZS-SYS-001.A.md)。**待验收说明**：退出条件「矩阵全部正反向通过」按 48/49 + GAP-3 归口登记为部分达成（循分批部分交付惯例主卡转开发中）；Web 页面操作联验归 .B（B06）；D-09 组织模型不混入。本记录不表示任何主任务已验收。
docs\05-底座模块分析与开发任务清单.md:895:- 验收：干净构建、依赖树、打包启动类与自动配置注册正确；静态/装配测试能发现旧包漏改和新旧类重复注册；未启用模块仍关闭，保留源码可按批准范围单独编译；真实 PG 启动及完整安全链在 B02/B03 复验，不以 B01 构建代替运行成功。
docs\05-底座模块分析与开发任务清单.md:896:- 开发记录（2026-09-08）：提交 43d71fad，10,059 文件。目录/包/类名以 git mv 保留历史（22 个顶层模块、framework 15 个子模块、67 个 Java 源码根、39 个 Yudao*→Zszj* 类）；配置键、Spring 应用名、环境变量（ZS_*→ZSZJ_*）、Dockerfile/部署脚本、接口文档标题同步。静态一致性检查 `node scripts/brand/verify-backend-naming.mjs` 通过：6,763 个 Java 包声明与路径一致，imports/spring.factories/Mapper XML 的 cn.zszj FQCN 全部可解析，POM 坐标一致，硬性问题 0。评审修正随 581927d0/b2c26ea9 落地：恢复被误改的上游署名链接（github.com/YunaiV/*、gitee.com/zhijiantianya/*）并补保护规则。未验证项（如实登记）：干净构建、依赖树与真实 PG 启动待 ZS-ENG-002 工具链（本机无 JDK/Maven）在 B01/B02 补证。
docs\05-底座模块分析与开发任务清单.md:967:| ZS-DB-001.B | B02 | ZS-DB-001.A、ZS-DB-002 | 真实低权限 PG 连接与启动联验 | 可连接；错权限/不可用 PG 明确失败；随迁移验证业务启动 |
docs\05-底座模块分析与开发任务清单.md:1014:| ZS-OPS-002.A | B01 | ZS-ENG-002、ZS-ENG-003、ZS-ENG-005 | 部署模板、探针/TLS/反代和配置合同 | 模板可审查、敏感管理路径关闭；真实业务启动随 B02/B03，不等待恢复演练 |
docs\05-底座模块分析与开发任务清单.md:1124:| 2026-09-09 | V1.6 | M01 工程骨架六项、M02 数据库多项（DB-001～008/011～017/019/020）、CFG-001～003.A、CLIENT-005.A、OPS-001.A、GOV-001 完成首轮开发；tools/ 自引导 JDK17+Maven3.9.9 后完成真实构建与 dev 启动联验（详见第 2 节 V1.6 记录） |
docs\05-底座模块分析与开发任务清单.md:1128:| 2026-09-09 | V1.10 | ZS-SEC-002（B03；前置 ZS-ENG-001/004/005 已完成）交付静态接口清单生成器 ApiInventoryTest（328 端点、授权类别三分、3 项结构断言、漂移基线门禁）+ 25 匿名端点目录与 5 审查点 + 运行时合同复用 SEC-012.A 夹具新增第 8 组（ASYNC 首次不免认证 + ADMIN/MEMBER 双向串用，35/35）+ 接口清单与匿名白名单文档；zszj-server 8/8、门禁 10/10。顺带修复 ZS-ENG-001 既有 ModuleWhitelistTest 断言 bug（工具链首次运行 zszj-server 全测试暴露，非 SEC-002 引入）。卡片 待开发→待验收，第 2 节统计 47/25→46/26（详见第 2 节 V1.10 记录） |
docs\05-底座模块分析与开发任务清单.md:1148:| 2026-09-13 | V1.30 | ZS-CFG-004（补齐配置值校验、变更审计和恢复流程，B03 部分交付值校验；前置 ZS-CFG-001.B、ZS-CFG-002.B、ZS-DB-003 均已完成）经 worktree 隔离分支 `feat/cfg-004`（impl `38dde380`）`--no-ff` 合并 main（`e218fe04`）：新增 ConfigParamCatalog 参数目录（6 核心参数值合同 + wired 预留标注 + url.druid 热生效修正）与 ConfigValueValidator 值校验（类型/范围/枚举强校验、错误码 CONFIG_VALUE_TYPE_MISMATCH/OUT_OF_RANGE/NOT_IN_ALLOWED_SET；掩码回显改 key 时按目标 key 重校验堵旁路）；乐观锁并发冲突以**独立整数 version 列**交付（迁移 `V20260913.001__infra_config_optimistic_version.sql` 幂等，详情返回 version、更新必须回传、快照==请求才递增、条件 UPDATE 兜底，冲突抛 `CONFIG_UPDATE_CONFLICT`）。**codex 评审弧（gpt-6-astra/xhigh，r0→r4 五弧）**：r0 FAIL 1×P1（乐观锁仅服务端短窗、旧表单覆盖不可检测，H2 实证）→ r1 初版回传 updateTime 作版本 FAIL 2×P1（JSON 毫秒截断误拒 + datetime 秒级精度版本不推进）+1×P2（MapStruct 可伪造初始版本）→ **r2 改型整数 version 列**（创建置 0/更新原子 +1）→ r3 FAIL 1×P2（快照/请求版本未比对一致可致 7→8 复用不推进）→ r4 修复后 **PASS/0 发现**。验证：TDD RED 2 用例 → GREEN 50 tests/0 failures BUILD SUCCESS。卡片 待开发→待验收（B03 部分）；变更审计与恢复流程归 B04 复验、前端 version 契约联调归 Web E2E、真实 PG/API 归 ZS-SYS-001.A。另登记 ZS-DB-019.B 运行注意事项：run-pg-regression.mjs 8 套件在本机与其他构建/评审并行高负载下偶发单套件 Docker 容器启动竞态失败（三次复跑失败套件各不相同、套件单独运行均绿、空载聚合复跑 8/8 全绿），属环境负载性 flaky 非套件缺陷，CI 与收口验证建议空载或重试一次。本记录不表示任何主任务已验收。处置详见 [codex-ZS-CFG-004.md](reviews/codex-ZS-CFG-004.md)。README 索引版本同步 V1.30 |
services/zhongshu-core/docs\参数校验与请求资源限制规范.md:57:**静态防线** [ValidationContractTest](../zszj-server/src/test/java/cn/zszj/server/ValidationContractTest.java)（zszj-server，仿 [ApiInventoryTest] / [ModuleWhitelistTest] 的静态源码扫描约定，不依赖 Spring 上下文）：
services/zhongshu-core/docs\数据库迁移规范.md:45:- 真实库验收（B02）：空库全新执行、旧库 baseline 吸收、重跑幂等、篡改校验和阻止启动、低权限凭据启动——归 ZS-DB-003 的 B02 验收与 ZS-DB-005 升级规程。
docs\03-底座二次开发顺序与验收标准.md:3:> 文档版本：V1.6\
docs\03-底座二次开发顺序与验收标准.md:23:Web 生产构建通过，但 `ts:check` 存在上游基线错误；UniApp 离线缺 `@dcloudio/uni-app@3.0.0-4070620250821001`，类型/构建未执行。真实 PostgreSQL 业务启动、跨组织权限、真机、微信支付、AI Provider 和真实角色 UAT 均未验收。完整记录见 [源码迁入与验证报告](04-源码迁入与验证报告.md)。
docs\03-底座二次开发顺序与验收标准.md:100:- 正向验收：最小工程构建、模块装配和健康探针合同通过，配置缺失给出明确失败；Web/移动端配置只指向产品 API。需要业务 schema 的真实 PG 启动在 B02 与迁移联验，不以 MySQL/H2/Mock 代替，不要求 B01 等待完整 B02；部署模板与探针边界由 ZS-OPS-002.A 交付，恢复/告警在后续子项验收。
docs\03-底座二次开发顺序与验收标准.md:102:- 执行入口：新增 `FoundationModuleWhitelistTest`；Maven reactor、前端 lint/typecheck/build 以各工程原生脚本进入 CI。
docs\03-底座二次开发顺序与验收标准.md:208:| 真实 PostgreSQL/Redis/对象存储 | B02～B05 | 版本、低权限账号、启动日志、迁移结果、备份恢复和测试报告 | 仅源码/本地配置，不标“环境通过” |
docs\03-底座二次开发顺序与验收标准.md:250:| 2026-09-10 | V1.6 | 用户拍板 D-09 已确认，更新决策边界与第 2 节实施纪律（B07 前不提前创建组织/任职表）；D-07 方向已采纳、细节待阶段 2 前确认；同步 docs/02/docs/05/docs/07 与 verify-docs 门禁；批次依赖与验收门禁不变 |
services/zhongshu-core/docs\功能目录.md:35:启用一个模块 = ① 根 pom modules 取消注释 + ② zszj-server 依赖引入 + ③ ModuleCatalog 清单迁移（DISABLED → ENABLED）+ ④ DefaultController 移除对应兜底 + ⑤ V1 后追加该模块菜单的增量迁移 + ⑥ 两道检查（ModuleWhitelistTest / verify-module-whitelist.mjs）通过。停用反向操作。任何一步缺失都会被上述检查拦截。
services/zhongshu-core/docs\BPM引擎表与业务扩展表迁移责任决策.md:39:| 关闭 BPM 无副作用 | 结构面（本套件 S0）：System/Infra 全基线零 `ACT_`/`FLW_` 表；装配面（既有证据）：server POM/根 reactor 保持 BPM 注释 + `ModuleWhitelistTest` 门禁 + V1.6 记录的真实 PG17+Redis 启动联验（BPM 关闭态下 server 正常装配、关闭模块请求被拒）。启动后零流程副作用的运行期观测归启用 BPM 的批次做开/关对照时补测，本件不作此宣称 | PASS（结构与装配面） |
services/zhongshu-core/docs\BPM引擎表与业务扩展表迁移责任决策.md:52:  （`ModuleWhitelistTest` 以根 POM 非注释 `<module>` 为门禁事实源，引入 profile 会被判违规）。
docs\04-源码迁入与验证报告.md:32:- 敏感配置改为环境变量或空模板；集成测试中的凭据字面量用明显的 `REDACTED_SOURCE_*` 占位。真实运行需要配置项目自己的凭据；不保留“自动借用上游演示账号”的启动方式。
docs\04-源码迁入与验证报告.md:51:| 真实 PostgreSQL、整站启动、多端登录/权限联调 | 未执行 | 不能用打包和 H2 通过代替 |
services/zhongshu-core/docs\接口清单与匿名白名单.md:33:清单生成器 [ApiInventoryTest](../zszj-server/src/test/java/cn/zszj/server/ApiInventoryTest.java)（置于 zszj-server/src/test，仿 [ModuleWhitelistTest](../zszj-server/src/test/java/cn/zszj/server/ModuleWhitelistTest.java) 静态源码扫描约定，**不依赖 Spring 上下文**），遍历启用模块（`ModuleWhitelist.ENABLED_MODULES` = system/infra）的 Controller 源码，每端点编码一行：
services/zhongshu-core/docs\接口清单与匿名白名单.md:109:| 管理端点复用 ZS-ENG-005 | actuator 仅暴露 health（[ModuleWhitelistTest](../zszj-server/src/test/java/cn/zszj/server/ModuleWhitelistTest.java) 治理兜底路由） | ZS-ENG-005 |
services/zhongshu-core/docs\接口清单与匿名白名单.md:127:- **管理端点复用 ZS-ENG-005**：actuator 仅 health，兜底路由由 ModuleWhitelistTest 治理（§6）。
services/zhongshu-core/docs\接口清单与匿名白名单.md:131:1. 本清单为**启用模块（system/infra）**的静态源码事实来源；未启用模块的兜底路由由 [ZS-ENG-001 ModuleWhitelistTest](../zszj-server/src/test/java/cn/zszj/server/ModuleWhitelistTest.java) 治理，不纳入本清单。后续启用新业务模块（member/mall/…）时，清单随 `ENABLED_MODULES` 自动扩展并经漂移门禁评审。
docs\reviews\codex-hotfix-B.raw.md:115:| [搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗昡(docs/05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md) | V1.6 | 12 涓ā鍧楅潤鎬佸垎鏋愶紱鎸変紬澧呰姹傝褰曚唬鐮佸樊璺濄€佽皟鏁翠换鍔°€佷紭鍏堢骇銆佷緷璧栧拰楠屾
docs\reviews\codex-hotfix-B-r3.raw.md:804:  | [搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗昡(docs/05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md) | V1.6 | 12 涓ā鍧楅潤鎬佸垎鏋愶紱鎸変紬澧呰姹傝褰曚唬鐮佸樊璺濄€佽皟鏁翠换鍔°€佷紭鍏堢骇銆佷緷璧栧拰
docs\reviews\codex-ZS-BRAND-002.md:79:| 干净构建、依赖树、真实 PG 启动 | ⏳ 待 ZS-ENG-002 工具链在 B01/B02 补证（本机无 JDK/Maven，已如实登记） |
docs\reviews\codex-ZS-BRAND-002.md:87:- 大规模改名的系统性/一致性整体达标，静态检查 0 硬性问题；运行时验证（干净构建、真实 PG 启动）按 05 清单登记为 B01/B02 待补证项，符合「不虚报生产可用」要求。
docs\reviews\codex-ZS-BRAND-001.raw.md:166:| [搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗昡(docs/05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md) | V1.6 | 12 涓ā鍧楅潤鎬佸垎鏋愶
docs\reviews\codex-ZS-BRAND-001.raw.md:361:  V1.6 鍙樻洿涓庨獙璇佽褰曪紙2026-09-09锛夛細M01 宸ョ▼楠ㄦ灦鍏」鍏ㄩ儴瀹屾垚锛圸S-ENG-001 鐧藉悕鍗?295040b3銆?0
docs\reviews\codex-ZS-BRAND-001.raw.md:377:  V1.6 琛ュ厖璁板綍锛?026-09-09 鏅氾紝宸ュ叿閾惧紩瀵硷級锛氬湪浠撳簱 tools/ 鐩綍鑷紩瀵?Temurin JDK 17.0.20.
docs\reviews\codex-ZS-BRAND-006.A.raw.md:3646:| [搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗昡(docs/05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md) | V1.6 | 12 涓ā鍧楅潤鎬佸垎鏋愶
docs\reviews\codex-ZS-BRAND-004.A.raw.md:1317:| [搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗昡(docs/05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md) | V1.6 | 12 涓ā鍧楅潤鎬佸垎鏋愶
docs\reviews\codex-ZS-GOV-001-P0.raw.md:462:V1.6 鍙樻洿涓庨獙璇佽褰曪紙2026-09-09锛夛細M01 宸ョ▼楠ㄦ灦鍏」鍏ㄩ儴瀹屾垚锛圸S-ENG-001 鐧藉悕鍗?295040b3銆?02 
docs\reviews\codex-ZS-GOV-001-P0.raw.md:475:V1.6 琛ュ厖璁板綍锛?026-09-09 鏅氾紝宸ュ叿閾惧紩瀵硷級锛氬湪浠撳簱 tools/ 鐩綍鑷紩瀵?Temurin JDK 17.0.20.1 
docs\reviews\codex-ZS-GOV-001-P0.raw.md:482:鎬烩€斺€斿疄闄呬负 48 寰呭紑鍙戙€?1 寮€鍙戜腑銆?4 寰呴獙鏀躲€? 寰呭喅绛栥€? 寰呭墠缃€? 宸查獙鏀讹紙姝ゅ墠 V1.6 姹囨€?66/1
docs\reviews\codex-ZS-GOV-001-P0.raw.md:514:`锛坺szj-server/src/test锛屼豢 `ModuleWhitelistTest` 闈欐€佹簮鐮佹壂鎻忋€佷笉鍚姩 Spring 涓婁笅鏂囷級閬
docs\reviews\codex-ZS-GOV-001-P0.raw.md:520:entoryTest 4 + ModuleWhitelistTest 4锛夈€乣run-local-gates --fast` 10/10锛涚櫥璁癧鎺ュ彛娓呭
docs\reviews\codex-ZS-GOV-001-P0.raw.md:522:1 鏃㈡湁 `ModuleWhitelistTest` 鏂█ bug锛堥娆′互宸ュ叿閾捐繍琛?zszj-server 鍏ㄦ祴璇曟椂鏆撮湶锛岄潪 SEC-0
docs\reviews\codex-ZS-GOV-001-P0.raw.md:607:佷笉鍐嶅紩瀵间笂娓告枃妗ｏ紱鏂板鐧藉悕鍗曟祴璇?ModuleWhitelistTest锛圥OM 婵€娲讳竴鑷存€с€佸厹搴曡鐩栧畬鏁存€с€佹湭鍚敤鍖呭
docs\reviews\codex-ZS-GOV-001-P0.raw.md:612:.9 杩愯 zszj-server 鍏ㄦ祴璇曟椂锛宍ModuleWhitelistTest.enabledModulesMatchServerPomDepe
docs\reviews\codex-ZS-GOV-001-P0.raw.md:620:zszj-server `ModuleWhitelistTest` 4/4銆佽繛鍚?`ApiInventoryTest` 4/4 鍏?8/8 BUILD SU
docs\reviews\codex-ZS-SEC-002.md:24:- `-pl :zszj-server -Dtest=ApiInventoryTest,ModuleWhitelistTest test`：8/8 BUILD SUCCESS；`-pl :zszj-spring-boot-starter-biz-tenant -Dtest=SecurityFilterChainFixtureTest`：35/35；`run-local-gates --fast` 10/10
docs\reviews\codex-ZS-OPS-001-step4.raw.md:442: * ZS-ENG-001 妯″潡鐧藉悕鍗曢潤鎬佹鏌ワ紙鏈湴/璇勫鍏ュ彛锛孧aven 娴嬭瘯 ModuleWhitelistTest 鐨勬棤 JDK 琛
docs\reviews\codex-ZS-SEC-003.raw.md:1933: 8/8、门禁 10/10。顺带修复 ZS-ENG-001 既有 ModuleWhitelistTest 断言 bug（工具链首次运行 zszj-server
docs\reviews\codex-ZS-SEC-008.raw.md:739:+ * <p>不依赖 Spring 上下文（仿 {@link ApiInventoryTest} / {@link ModuleWhitelistTest} 
docs\reviews\codex-ZS-SEC-008.raw.md:1282: * <p>不依赖 Spring 上下文（仿 {@link ApiInventoryTest} / {@link ModuleWhitelistTest} 的
docs\reviews\codex-ZS-SEC-008.raw.md:3805: | [底座二次开发顺序与验收标准](docs/03-底座二次开发顺序与验收标准.md) | V1.6 | 24 个工作包对应 12 个批次，逐批前置条件、修
docs\reviews\codex-ZS-SEC-008.raw.md:3881:-server，仿 `ApiInventoryTest`/`ModuleWhitelistTest` 静态源码扫描、不启动 Spring 上下文），强制启用模
docs\reviews\codex-ZS-SEC-009.raw.md:1182:ValidationContractTest`（zszj-server，仿 `ApiInventoryTest`/`ModuleWhitelistTest` 
docs\reviews\HANDOFF-ZS-SEC-002.md:17:| 本地验证（后端） | zszj-server `Tests run: 8`（ApiInventoryTest 4 + ModuleWhitelistTest 4）；biz-tenant 夹具 `Tests run: 35`（含新增第 8 组 4 例），均 BUILD SUCCESS |
docs\reviews\HANDOFF-ZS-SEC-002.md:19:| 复现入口（后端） | `source tools/env.sh && cd services/zhongshu-core && mvn -pl :zszj-server "-Dtest=ApiInventoryTest,ModuleWhitelistTest" test && mvn -pl :zszj-spring-boot-starter-biz-tenant "-Dtest=SecurityFilterChainFixtureTest" test`（PowerShell 下按 tools/env.sh 复制 `$env:JAVA_HOME` 与 `$env:PATH`） |
docs\reviews\HANDOFF-ZS-SEC-002.md:38:> ⚠️ **drive-by 修复（改的是他任务既有测试，请重点核查）**：`zszj-server/src/test/java/cn/zszj/server/ModuleWhitelistTest.java`（**+6/-2**）——修复 ZS-ENG-001 既有 `enabledModulesMatchServerPomDependencies` 断言 bug（根因见 §3 判断点 6），**非 SEC-002 引入**，首次以工具链运行 zszj-server 全测试时暴露。
docs\reviews\HANDOFF-ZS-SEC-002.md:54:6. **drive-by 修复 ModuleWhitelistTest 的正确性（重点，因改的是 ENG-001 既有测试）**：根因判断是否准确——`activeModuleArtifacts` 已用 `.replace("zszj-module-","")` 剥前缀返回 `{system,infra}`，而原断言期望侧 `ENABLED_MODULES.map(m->"zszj-module-"+m)`、实际侧却又 `serverDeps.filter(startsWith("zszj-module-"))` 在**已剥前缀**值上过滤致 actual 恒空 `[]`、断言必然失败？修复 `assertEquals(new LinkedHashSet<>(ModuleWhitelist.ENABLED_MODULES), serverDeps)` 是否与姊妹测试 `enabledModulesMatchRootPomActiveModules` 的剥前缀比对约定一致、**仍能验证「未启用模块不进 zszj-server 依赖闭包」**而非退化为空洞断言？此修复归属 ENG-001 是否恰当（SEC-002 顺带、已在 05 文档双卡记录）？
docs\reviews\HANDOFF-ZS-SEC-002.md:65:- **清单范围**：为「启用模块（system/infra）」的静态源码事实来源；未启用模块兜底路由由 ZS-ENG-001 `ModuleWhitelistTest` 治理，不入清单；后续启用新业务模块（member/mall/…）时清单随 `ENABLED_MODULES` 自动扩展并经漂移门禁评审。WebSocket（`AuthorizeRequestsCustomizer` 唯一实现）非 REST、无 admin/app 前缀，不在清单。
docs\reviews\codex-ZS-CFG-001.B-r1.raw.md:12548:E:\锟斤拷锟斤拷之锟斤拷AI锟斤拷锟斤拷平台锟斤拷锟斤拷\services\zhongshu-core\zszj-server\src\test\java\cn\zszj\server\ModuleWhitelistTest.java
docs\reviews\codex-ZS-CFG-001.B-r1.raw.md:12637:E:\锟斤拷锟斤拷之锟斤拷AI锟斤拷锟斤拷平台锟斤拷锟斤拷\services\zhongshu-core\zszj-server\target\test-classes\cn\zszj\server\ModuleWhitelistTest
docs\reviews\codex-ZS-BRAND-003.A.raw.md:13826:erver\ModuleWhitelistTest.java
docs\reviews\codex-ZS-BRAND-003.A.raw.md:13865:zszj\server\ModuleWhitelistTest.class
docs\reviews\codex-ZS-SEC-010.raw.md:4050:| [底座二次开发顺序与验收标准](docs/03-底座二次开发顺序与验收标准.md) | V1.6 | 24 个工作包对应 12 个批次，逐批前置条件、修改位置、正反向验收和执行入口 |
docs\reviews\codex-ZS-SEC-010.raw.md:4086:25 个导入文件仅清除了上游演示凭据或改为环境变量，SQL 表结构保持不变；原始与导入 blob 均已记录。不要在已跟踪的 `.env*` 模板写真实密钥，也不要用演示配置直接启动生产服务。完整范围和验证限制见迁入报告。

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "@'
const fs = require('fs');
for (const f of ['services/zhongshu-core/zszj-module-bpm/pom.xml', 'services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessRuntimeTest.java', 'services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessBootstrapTest.java']) {
process.stdout.write('\\nFILE ' + f + '\\n');
process.stdout.write(fs.readFileSync(f, 'utf8').split('\\n').map((s,i)=>(i+1)+': '+s).join('\\n'));
}
'@ | node" in E:\zszj-wt-bpm-001-a
 succeeded in 2ms:
codex
发现一个关键漏口：bootstrap 通过后读取表数或 schema 版本失败，异常分支只打印错误就进入汇总，没有增加失败计数，可能仍以 0 退出。我会做内存故障注入确认，并继续核查报告校验、清理失败和 Windows 工具链回退。

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "@'
const {spawnSync} = require('node:child_process');
const {resolve} = require('node:path');
const env = {...process.env, JAVA_HOME: resolve('tools/jdk-17.0.20.1+1'), PATH: resolve('tools/jdk-17.0.20.1+1/bin')+';'+resolve('tools/apache-maven-3.9.9/bin')+';'+process.env.PATH};
for (const [command,args] of [['mvn',['-v']], ['mvn.cmd',['-v']], ['cmd.exe',['/d','/s','/c','mvn -v']], ['java.exe',['-version']]]) {
const r = spawnSync(command,args,{env,encoding:'utf8'});
console.log(JSON.stringify({command,args,status:r.status,error:r.error?.code,stdout:r.stdout?.split('\\n').slice(0,3),stderr:r.stderr?.split('\\n').slice(0,3)}));
}
'@ | node" in E:\zszj-wt-bpm-001-a
 succeeded in 177ms:
{"command":"cmd.exe","args":["/d","/s","/c","mvn -v"],"status":0,"stdout":["Apache Maven 3.9.9 (8e8579a9e76f7d015ee5ec7bfcdc97d260186937)\r","Maven home: E:\\zszj-wt-bpm-001-a\\tools\\apache-maven-3.9.9\r","Java version: 17.0.20.1, vendor: Eclipse Adoptium, runtime: E:\\zszj-wt-bpm-001-a\\tools\\jdk-17.0.20.1+1\r"],"stderr":[""]}
{"command":"java.exe","args":["-version"],"status":0,"stdout":[""],"stderr":["openjdk version \"17.0.20.1\" 2026-08-18\r","OpenJDK Runtime Environment Temurin-17.0.20.1+1 (build 17.0.20.1+1)\r","OpenJDK 64-Bit Server VM Temurin-17.0.20.1+1 (build 17.0.20.1+1, mixed mode, sharing)\r"]}

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "@'
const fs = require('node:fs');
const vm = require('node:vm');
const {resolve,join} = require('node:path');
const {pathToFileURL,fileURLToPath} = require('node:url');
const file = resolve('scripts/db/run-bpm001-verify.mjs');
const source = fs.readFileSync(file,'utf8').replace(/"'^import .*;'"\\r?\\n/gm,'').replaceAll('import.meta.url',JSON.stringify(pathToFileURL(file).href));
function test(mode) {
  const files = new Map(), logs = [], launches = [];
  let boot = false, runtime = false, removed = 0, exitCode;
  const report = name => join(resolve('services/zhongshu-core'),'zszj-module-bpm','target','surefire-reports','TEST-cn.zszj.module.bpm.harness.'+name+'.xml');
  const xml = (name,n) => '<testsuite name=\"cn.zszj.module.bpm.harness.'+name+'\" tests=\"'+n+'\" failures=\"0\" errors=\"0\" skipped=\"0\"></testsuite>';
  files.set(report('BpmPgHarnessBootstrapTest'),xml('BpmPgHarnessBootstrapTest',5));
  files.set(report('BpmPgHarnessRuntimeTest'),xml('BpmPgHarnessRuntimeTest',9));
  function spawnSync(command,args,options={}) {
    const success = {status:0,stdout:'',stderr:''};
    if(command === 'mvn' && args.includes('test')) {
      const name = args.find(a=>a.startsWith('-Dtest=')).slice(7);
      launches.push(name);
      if(name.includes('Bootstrap')) boot=true; else runtime=true;
      if(mode"'!=='"'stale_report') files.set(report(name), xml(name,name.includes('Bootstrap')?5:9).replace('skipped=\"0\"',mode==='skipped'?'skipped=\"1\"':'skipped=\"0\"').replace('tests=\"5\"',mode==='wrong_count'?'tests=\"4\"':'tests=\"5\"').replace('harness.'+name,mode==='wrong_identity'?'harness.Other': 'harness.'+name));
      return {...success,status:mode==='maven_error'?1:0};
    }
    if(command "'!== '"'docker') return success;
    if(args[0]==='rm') { removed++; return mode==='cleanup_error'?{status:1,stdout:'',stderr:'injected docker rm error'}:success; }
    const sql = args.includes('-c') ? args[args.indexOf('-c')+1] : '';
    if(options.input?.includes('zszj_app_should_fail')) return {status:1,stdout:'',stderr:'ERROR: 42501: permission denied for schema public'};
    if(sql==='SELECT 1') return {...success,stdout:'1\\n'};
    if(sql.includes('information_schema.tables')) {
      if(mode==='S0_error'&&"'!boot || mode==='"'post_boot_count_error'&&boot&&"'!runtime || mode==='"'S2_error'&&runtime) return {status:2,stdout:'',stderr:'injected psql failure'};
      return {...success,stdout:boot?'35\\n':'0\\n'};
    }
    if(sql.includes('ACT_GE_PROPERTY')) {
      if(mode==='post_boot_version_error'&&"'!runtime) return {status:2,stdout:'"'',stderr:'injected version query failure'};
      if(mode==='post_boot_version_empty'&&"'!runtime) return {...success,stdout:'"''};
      return {...success,stdout:'7.0.1\\n'};
    }
    if(sql.includes('bpm_harness_probe')) return mode==='S3_error'?{status:2,stdout:'',stderr:'injected probe failure'}:{...success,stdout:'3\\n'};
    return success;
  }
  const context = {spawnSync,execFileSync(){throw new Error('Unexpected execFileSync');},fileURLToPath,URL,join,Math,Date,SharedArrayBuffer,Int32Array,Atomics:{wait(){}},
    process:{platform:'linux',env:{PATH:'/mock/bin'},on(){},exit(code){exitCode=code;throw {reviewExit:true};}},
    console:{log(...xs){logs.push(xs.join(' '));},error(...xs){logs.push(xs.join(' '));}},
    existsSync:p=>files.has(p),rmSync:p=>files.delete(p),mkdirSync(){},readdirSync(){return ['V1.sql'];},readFileSync:p=>files.get(p)??'SELECT 1;',writeFileSync:(p,s)=>files.set(p,s)};
  try { vm.runInNewContext(source,context,{timeout:3000}); } catch(e) { if("'!e.reviewExit) throw e; }
  const saved = [...files.entries()].find(([p])=>p.endsWith('"'runtime-report.json'));
  const summary = saved ? JSON.parse(saved[1]):{};
  console.log(JSON.stringify({mode,exitCode,runtimeLaunched:runtime,failCount:summary.failCount,checks:summary.results?.map(r=>[r.id.split(' ')[0],r.ok]),dockerRmCalls:removed,reportedQueryError:logs.some(l=>l.includes('injected')||l.includes('psql'))}));
}
for(const mode of ['success','post_boot_count_error','post_boot_version_error','post_boot_version_empty','S0_error','S2_error','S3_error','cleanup_error','stale_report','skipped','wrong_count','wrong_identity','maven_error']) test(mode);
'@ | node" in E:\zszj-wt-bpm-001-a
 succeeded in 0ms:
{"mode":"success","exitCode":0,"runtimeLaunched":true,"failCount":0,"checks":[["S0",true],["S1",true],["P1",true],["P2",true],["S2",true],["S3",true]],"dockerRmCalls":1,"reportedQueryError":false}
{"mode":"post_boot_count_error","exitCode":0,"runtimeLaunched":false,"failCount":0,"checks":[["S0",true],["S1",true],["P1",true]],"dockerRmCalls":1,"reportedQueryError":true}
{"mode":"post_boot_version_error","exitCode":0,"runtimeLaunched":false,"failCount":0,"checks":[["S0",true],["S1",true],["P1",true]],"dockerRmCalls":1,"reportedQueryError":true}
{"mode":"post_boot_version_empty","exitCode":0,"runtimeLaunched":false,"failCount":0,"checks":[["S0",true],["S1",true],["P1",true]],"dockerRmCalls":1,"reportedQueryError":true}
{"mode":"S0_error","exitCode":1,"runtimeLaunched":true,"failCount":1,"checks":[["S0",false],["S1",true],["P1",true],["P2",true],["S2",true],["S3",true]],"dockerRmCalls":1,"reportedQueryError":true}
{"mode":"S2_error","exitCode":1,"runtimeLaunched":true,"failCount":1,"checks":[["S0",true],["S1",true],["P1",true],["P2",true],["S2",false],["S3",true]],"dockerRmCalls":1,"reportedQueryError":true}
{"mode":"S3_error","exitCode":1,"runtimeLaunched":true,"failCount":1,"checks":[["S0",true],["S1",true],["P1",true],["P2",true],["S2",true],["S3",false]],"dockerRmCalls":1,"reportedQueryError":true}
{"mode":"cleanup_error","exitCode":1,"runtimeLaunched":true,"failCount":1,"checks":[["S0",true],["S1",true],["P1",true],["P2",true],["S2",true],["S3",true],["S4",false]],"dockerRmCalls":3,"reportedQueryError":true}
{"mode":"stale_report","exitCode":1,"runtimeLaunched":false,"failCount":1,"checks":[["S0",true],["S1",true],["P1",false]],"dockerRmCalls":1,"reportedQueryError":false}
{"mode":"skipped","exitCode":1,"runtimeLaunched":false,"failCount":1,"checks":[["S0",true],["S1",true],["P1",false]],"dockerRmCalls":1,"reportedQueryError":false}
{"mode":"wrong_count","exitCode":1,"runtimeLaunched":false,"failCount":1,"checks":[["S0",true],["S1",true],["P1",false]],"dockerRmCalls":1,"reportedQueryError":false}
{"mode":"wrong_identity","exitCode":1,"runtimeLaunched":false,"failCount":1,"checks":[["S0",true],["S1",true],["P1",false]],"dockerRmCalls":1,"reportedQueryError":false}
{"mode":"maven_error","exitCode":1,"runtimeLaunched":false,"failCount":1,"checks":[["S0",true],["S1",true],["P1",false]],"dockerRmCalls":1,"reportedQueryError":false}

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "rg -n -C 1 '<artifactId>zszj-' services/zhongshu-core/zszj-module-system/pom.xml; rg -n 'spawnSync|mvn|toolsDir|jdkDir' scripts/db/run-db018-verify.mjs; git status --short; git log -1 --format=\"%h %ci\"" in E:\zszj-wt-bpm-001-a
 succeeded in 10ms:
8a1639f2 2026-09-14 03:30:08 +0800

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "@'
const fs=require('fs');
for (const [f,a,b] of [['scripts/db/run-bpm001-verify.mjs',38,136],['scripts/db/run-bpm001-verify.mjs',275,346],['services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessConfiguration.java',40,110],['services/zhongshu-core/zszj-dependencies/pom.xml',1,38],['outputs/bpm-001/runtime-report.json',1,100]]) {
console.log('\\nFILE '+f);
console.log(fs.readFileSync(f,'utf8').split('\\n').slice(a-1,b).map((s,i)=>(i+a)+': '+s).join('\\n'));
}
'@ | node
rg -n 'Tests run:|BUILD SUCCESS|BUILD FAILURE|Reactor Summary|zszj-dependencies|zszj-framework' outputs/bpm-001 -g '*.log'
rg -n '<testsuite' services/zhongshu-core/zszj-module-bpm/target/surefire-reports -g '*Harness*.xml'" in E:\zszj-wt-bpm-001-a
 succeeded in 0ms:

FILE scripts/db/run-bpm001-verify.mjs
38: const outDir = join(root, 'outputs', 'bpm-001');
39: 
40: function fail(code, message) { console.error(message); process.exit(code); }
41: 
42: // ---- 工具链解析：tools/ 优先（本地/worktree），否则 PATH（CI 由工作流 provision），皆缺退出码 3 ----
43: const toolsDir = join(root, 'tools');
44: const jdkDir = join(toolsDir, 'jdk-17.0.20.1+1');
45: const mavenBin = join(toolsDir, 'apache-maven-3.9.9', 'bin');
46: let javaHomeEnv, pathPrefix;
47: if (existsSync(jdkDir) && existsSync(mavenBin)) {
48:   javaHomeEnv = jdkDir;
49:   pathPrefix = process.platform === 'win32' ? `${jdkDir}\\bin;${mavenBin};` : `${jdkDir}/bin:${mavenBin}:`;
50: } else {
51:   const probe = spawnSync('mvn', ['-v'], { encoding: 'utf8' });
52:   const hasJava = spawnSync(process.platform === 'win32' ? 'java.exe' : 'java', ['-version'], { encoding: 'utf8' });
53:   if (probe.error || probe.status !== 0 || hasJava.error || hasJava.status !== 0) {
54:     fail(3, `[bpm001] 工具链缺失：无 tools/ 供给且 PATH 上无可用 mvn/java（本地请供给 tools/，CI 由 pg-regression.yml provision）`);
55:   }
56:   javaHomeEnv = process.env.JAVA_HOME;
57:   pathPrefix = '';
58: }
59: const childEnv = {
60:   ...process.env,
61:   ...(javaHomeEnv ? { JAVA_HOME: javaHomeEnv } : {}),
62:   PATH: `${pathPrefix}${process.env.PATH}`,
63: };
64: 
65: // ---- Docker 预检（缺依赖不静默跳过）----
66: const dockerUp = spawnSync('docker', ['version', '--format', '{{.Server.Version}}'], { encoding: 'utf8' });
67: if (dockerUp.error || dockerUp.status !== 0) {
68:   fail(3, `[bpm001] Docker 不可用（${dockerUp.error?.message ?? `exit=${dockerUp.status}`}）：验证不得静默跳过`);
69: }
70: 
71: const container = `zszj-bpm001-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
72: let port = 4332 + Math.floor(Math.random() * 700);
73: let cleaned = false;
74: let cleanupError = '';
75: const cleanup = () => {
76:   if (cleaned) return;
77:   for (let attempt = 0; attempt < 3 && !cleaned; attempt++) {
78:     const r = spawnSync('docker', ['rm', '-f', '-v', container], { encoding: 'utf8' });
79:     if (r.status === 0) { cleaned = true; break; }
80:     cleanupError = (r.stderr ?? '').trim();
81:     Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500);
82:   }
83: };
84: const onSignal = (signal) => { cleanup(); process.exit(signal === 'SIGINT' ? 130 : 143); };
85: process.on('exit', () => { if (!cleaned) cleanup(); });
86: process.on('SIGINT', () => onSignal('SIGINT'));
87: process.on('SIGTERM', () => onSignal('SIGTERM'));
88: 
89: // ---- psql 严格封装：任何查询失败/空结果都必须显式暴露，不吞错 ----
90: const psqlRun = (user, db, sql) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', user, '-d', db,
91:   '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 });
92: function psqlScalar(user, db, sql) {
93:   const r = spawnSync('docker', ['exec', container, 'psql', '-U', user, '-d', db, '-At', '-c', sql],
94:     { encoding: 'utf8', maxBuffer: 8 * 1024 * 1024 });
95:   if (r.error || r.status !== 0) throw new Error(`psql 查询失败（exit=${r.status ?? 'ERR'}）：${((r.stderr ?? '') || (r.stdout ?? '')).slice(0, 300)}`);
96:   const value = (r.stdout ?? '').trim();
97:   if (value === '') throw new Error(`psql 查询返回空：${sql.slice(0, 120)}`);
98:   return value;
99: }
100: function psqlScalarInt(user, db, sql) {
101:   const value = psqlScalar(user, db, sql);
102:   const n = Number(value);
103:   if (!Number.isInteger(n) || n < 0) throw new Error(`期望非负整数，实际「${value}」：${sql.slice(0, 120)}`);
104:   return n;
105: }
106: 
107: let pass = 0, failCount = 0;
108: const results = [];
109: const record = (id, ok, note = '') => { results.push({ id, ok, note }); ok ? pass++ : failCount++; console.log(`[${ok ? 'PASS' : 'FAIL'}] ${id} ${note}`); };
110: 
111: // ---- 拉起一次性 PG（负载 flaky：清理残件+换端口重试一次）----
112: function dockerRunOnce() {
113:   const args = ['run', '-d', '--name', container, '-e', 'POSTGRES_PASSWORD=bpm001', '-p', `127.0.0.1:${port}:5432`, 'postgres:17-alpine'];
114:   const r = process.platform === 'win32'
115:     ? spawnSync('cmd.exe', ['/d', '/s', '/c', 'docker ' + args.join(' ')], { encoding: 'utf8' })
116:     : spawnSync('docker', args, { encoding: 'utf8' });
117:   if (r.status === 0) return true;
118:   console.error(`[bpm001] docker run 失败（port=${port}）：${((r.stderr ?? '') || '').trim().slice(0, 200)}`);
119:   return false;
120: }
121: console.log(`[bpm001] 拉起临时 PG（${container} @ 127.0.0.1:${port}）…`);
122: if (!dockerRunOnce()) {
123:   spawnSync('docker', ['rm', '-f', '-v', container], { stdio: 'ignore' });
124:   port = 4332 + Math.floor(Math.random() * 700);
125:   console.log(`[bpm001] 重试：新端口 ${port}`);
126:   if (!dockerRunOnce()) fail(3, '[bpm001] PG 夹具容器两次启动失败（Docker 负载竞态按约定重试一次后仍失败）');
127: }
128: 
129: let ready = false;
130: // 就绪探测必须走 TCP（-h 127.0.0.1）：initdb 期间的临时服务器只监听 unix socket，
131: // socket 探测可能误判就绪导致 CREATE DATABASE 落到临时库上失败
132: for (let i = 0; i < 40; i++) {
133:   const r = spawnSync('docker', ['exec', container, 'psql', '-h', '127.0.0.1', '-U', 'postgres', '-d', 'postgres', '-At', '-c', 'SELECT 1'], { encoding: 'utf8' });
134:   if (r.status === 0) { ready = true; break; }
135:   Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500);
136: }

FILE scripts/db/run-bpm001-verify.mjs
275: writeFileSync(join(outDir, 'runtime.log'), runLog.output);
276: let runOk = false, runNote = '';
277: try { runNote = runAssert(); runOk = runLog.status === 0; if (!runOk) runNote += `，但 mvn exit=${runLog.status}`; }
278: catch (e) { runNote = String(e.message ?? e); }
279: record('P2 runtime 重启恢复/通过/拒绝/撤回/转办/分页历史/事务回滚/积压恢复', runOk, runNote);
280: if (!runOk) {
281:   console.error('[bpm001] runtime 阶段失败，日志见 outputs/bpm-001/runtime.log（尾部）：\n' + runLog.output.split('\n').slice(-40).join('\n'));
282: }
283: 
284: // ---- S2/S3：运行期零 DDL（表数量与 schema.version 不变）+ 探针落库 ----
285: {
286:   let ok = false, note = '';
287:   try {
288:     const actAfterRuntime = countActFlw('act\\_%');
289:     const versionAfterRuntime = schemaVersion();
290:     ok = actAfterBootstrap === actAfterRuntime && versionAfterBootstrap === versionAfterRuntime;
291:     note = `ACT_ ${actAfterBootstrap}→${actAfterRuntime}, schema.version ${versionAfterBootstrap}→${versionAfterRuntime}`;
292:   } catch (e) { note = String(e.message ?? e); }
293:   record('S2 运行期零DDL：引擎表数量与 schema.version 不变', ok, note);
294: }
295: {
296:   let ok = false, note = '';
297:   try {
298:     const probeRows = psqlScalarInt('postgres', 'zhongshu', 'SELECT count(*) FROM bpm_harness_probe');
299:     ok = probeRows >= 3;
300:     note = `探针行=${probeRows}`;
301:   } catch (e) { note = String(e.message ?? e); }
302:   record('S3 异步回声探针落库（owner 引导版本 + app 运行期回声）', ok, note);
303: }
304: 
305: function finish() {
306:   cleanup();
307:   if (!cleaned) record('S4 夹具容器清理', false, `docker rm 重试后仍失败：${cleanupError}`);
308:   console.log('\n===== ZS-BPM-001 BPM 独立装配与 PG 验收汇总 =====');
309:   for (const r of results) console.log(`${r.ok ? 'PASS' : 'FAIL'}  ${r.id} ${r.note}`);
310:   console.log(`合计 ${results.length} 项，失败 ${failCount} 项`);
311:   try {
312:     writeFileSync(join(outDir, 'runtime-report.json'),
313:       JSON.stringify({ finishedAt: new Date().toISOString(), container, results, pass, failCount }, null, 2));
314:   } catch { /* 报告落盘失败不影响退出码语义 */ }
315:   process.exit(failCount === 0 ? 0 : 1);
316: }
317: finish();
318: 

FILE services/zhongshu-core/zszj-module-bpm/src/test/java/cn/zszj/module/bpm/harness/BpmPgHarnessConfiguration.java
40:     static final String ENV_USERNAME = "ZSZJ_BPM_HARNESS_USERNAME";
41:     static final String ENV_PASSWORD = "ZSZJ_BPM_HARNESS_PASSWORD";
42:     static final String ENV_SCHEMA_UPDATE = "ZSZJ_BPM_HARNESS_SCHEMA_UPDATE";
43:     static final String ENV_ASYNC_EXECUTOR = "ZSZJ_BPM_HARNESS_ASYNC_EXECUTOR";
44: 
45:     static String requireEnv(String key) {
46:         String value = System.getenv(key);
47:         if (value == null || value.isBlank()) {
48:             throw new IllegalStateException("[bpm-pg-harness] 缺少环境变量 " + key + "：验证不得静默跳过，须由 run-bpm001-verify.mjs 注入");
49:         }
50:         return value.trim();
51:     }
52: 
53:     static boolean requireFlag(String key) {
54:         String value = requireEnv(key);
55:         if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
56:             throw new IllegalStateException("[bpm-pg-harness] 环境变量 " + key + " 须为 true/false，实际=" + value);
57:         }
58:         return Boolean.parseBoolean(value);
59:     }
60: 
61:     @Bean(destroyMethod = "close")
62:     public HikariDataSource bpmHarnessDataSource() {
63:         String url = requireEnv(ENV_URL);
64:         if (!url.startsWith("jdbc:postgresql://")) {
65:             throw new IllegalStateException("[bpm-pg-harness] 夹具仅面向真实 PostgreSQL，收到 " + url);
66:         }
67:         // 全部环境参数在创建连接池前完成校验；密码缺失同样快速失败（不静默回退空口令）
68:         HikariConfig config = new HikariConfig();
69:         config.setJdbcUrl(url);
70:         config.setUsername(requireEnv(ENV_USERNAME));
71:         config.setPassword(requireEnv(ENV_PASSWORD));
72:         config.setMaximumPoolSize(4);
73:         config.setMinimumIdle(1);
74:         config.setPoolName("bpm-pg-harness");
75:         // 驱动由 zszj-spring-boot-starter-mybatis 传入 PG 驱动
76:         config.setDriverClassName("org.postgresql.Driver");
77:         return new HikariDataSource(config);
78:     }
79: 
80:     @Bean
81:     public PlatformTransactionManager bpmHarnessTransactionManager(DataSource dataSource) {
82:         // 与应用运行形态同构：引擎与业务共用 Spring DataSourceTransactionManager 的事务边界
83:         return new DataSourceTransactionManager(dataSource);
84:     }
85: 
86:     @Bean
87:     public TransactionTemplate bpmHarnessTransactionTemplate(PlatformTransactionManager txManager) {
88:         return new TransactionTemplate(txManager);
89:     }
90: 
91:     @Bean
92:     public SpringProcessEngineConfiguration bpmHarnessProcessEngineConfiguration(
93:             DataSource dataSource, PlatformTransactionManager transactionManager) {
94:         SpringProcessEngineConfiguration configuration = new SpringProcessEngineConfiguration();
95:         configuration.setDataSource(dataSource);
96:         configuration.setTransactionManager(transactionManager);
97:         // 引擎表迁移责任：bootstrap 阶段（owner 账号）允许引擎自建/升级；runtime 阶段（app 低权限）禁止改结构
98:         configuration.setDatabaseSchemaUpdate(requireFlag(ENV_SCHEMA_UPDATE)
99:                 ? ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE
100:                 : ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE);
101:         configuration.setAsyncExecutorActivate(requireFlag(ENV_ASYNC_EXECUTOR));
102:         configuration.setDeploymentName("bpmPgHarness");
103:         // 与 BpmFlowableConfiguration 相同的扩展点：注册引擎事件监听
104:         configuration.setEventListeners(java.util.List.of(new BpmPgHarness.EngineEventRecorder()));
105:         return configuration;
106:     }
107: 
108:     @Bean
109:     public ProcessEngineFactoryBean bpmHarnessProcessEngine(SpringProcessEngineConfiguration configuration) {
110:         ProcessEngineFactoryBean factoryBean = new ProcessEngineFactoryBean();

FILE services/zhongshu-core/zszj-dependencies/pom.xml
1: <?xml version="1.0" encoding="UTF-8"?>
2: <project xmlns="http://maven.apache.org/POM/4.0.0"
3:          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
4:          xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
5:     <modelVersion>4.0.0</modelVersion>
6: 
7:     <groupId>cn.zszj</groupId>
8:     <artifactId>zszj-dependencies</artifactId>
9:     <version>${revision}</version>
10:     <packaging>pom</packaging>
11: 
12:     <name>${project.artifactId}</name>
13:     <description>基础 bom 文件，管理整个项目的依赖版本</description>
14:     <url>https://github.com/YunaiV/ruoyi-vue-pro</url>
15: 
16:     <properties>
17:         <revision>2026.08-SNAPSHOT</revision>
18:         <flatten-maven-plugin.version>1.7.2</flatten-maven-plugin.version>
19:         <!-- 统一依赖管理 -->
20:         <spring.boot.version>3.5.15</spring.boot.version>
21:         <!-- Web 相关 -->
22:         <springdoc.version>2.8.17</springdoc.version>
23:         <knife4j.version>4.5.0</knife4j.version>
24:         <!-- DB 相关 -->
25:         <druid.version>1.2.28</druid.version>
26:         <mybatis.version>3.5.19</mybatis.version>
27:         <mybatis-plus.version>3.5.17</mybatis-plus.version>
28:         <mybatis-plus-join.version>1.5.9</mybatis-plus-join.version>
29:         <dynamic-datasource.version>4.5.0</dynamic-datasource.version>
30:         <easy-trans.version>3.1.8</easy-trans.version>
31:         <redisson.version>4.7.0</redisson.version>
32:         <dm8.jdbc.version>8.1.3.140</dm8.jdbc.version>
33:         <kingbase.jdbc.version>9.0.1.jre7</kingbase.jdbc.version>
34:         <opengauss.jdbc.version>7.0.0-RC3-og</opengauss.jdbc.version>
35:         <taos.version>3.9.0</taos.version>
36:         <!-- 消息队列 -->
37:         <rocketmq-spring.version>2.3.6</rocketmq-spring.version>
38:         <!-- 服务保障相关 -->

FILE outputs/bpm-001/runtime-report.json
1: {
2:   "finishedAt": "2026-09-13T19:29:39.114Z",
3:   "container": "zszj-bpm001-1789327757080-23584",
4:   "results": [
5:     {
6:       "id": "S0 关闭BPM结构零残留：基线零 ACT_/FLW_ 表",
7:       "ok": true,
8:       "note": "ACT_=0 FLW_=0"
9:     },
10:     {
11:       "id": "S1 运行账号无DDL：app 连接可用且建表被 42501 拒绝",
12:       "ok": true,
13:       "note": "建表按 SQLSTATE 42501（权限不足）被拒，app 连接本身可用"
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
outputs/bpm-001\runtime.log:53:[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.844 s -- in cn.zszj.module.bpm.harness.BpmPgHarnessRuntimeTest
outputs/bpm-001\runtime.log:57:[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
outputs/bpm-001\runtime.log:60:[INFO] BUILD SUCCESS
outputs/bpm-001\bootstrap.log:54:[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.125 s -- in cn.zszj.module.bpm.harness.BpmPgHarnessBootstrapTest
outputs/bpm-001\bootstrap.log:58:[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
outputs/bpm-001\bootstrap.log:61:[INFO] BUILD SUCCESS
services/zhongshu-core/zszj-module-bpm/target/surefire-reports\TEST-cn.zszj.module.bpm.harness.BpmPgHarnessRuntimeTest.xml:2:<testsuite xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="https://maven.apache.org/surefire/maven-surefire-plugin/xsd/surefire-test-report.xsd" version="3.0.2" name="cn.zszj.module.bpm.harness.BpmPgHarnessRuntimeTest" time="3.844" tests="9" errors="0" skipped="0" failures="0">
services/zhongshu-core/zszj-module-bpm/target/surefire-reports\TEST-cn.zszj.module.bpm.harness.BpmPgHarnessBootstrapTest.xml:2:<testsuite xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="https://maven.apache.org/surefire/maven-surefire-plugin/xsd/surefire-test-report.xsd" version="3.0.2" name="cn.zszj.module.bpm.harness.BpmPgHarnessBootstrapTest" time="3.125" tests="5" errors="0" skipped="0" failures="0">

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "@'
const fs=require('fs');
for(const f of ['services/zhongshu-core/.flattened-pom.xml','services/zhongshu-core/zszj-module-system/.flattened-pom.xml','services/zhongshu-core/zszj-module-bpm/.flattened-pom.xml']){
 console.log('FILE '+f);
 if(fs.existsSync(f)) console.log(fs.readFileSync(f,'utf8').split('\\n').filter((s,i,a)=>i<22 || s.includes('zszj-dependencies')||s.includes('<dependencyManagement>')).join('\\n'));
}
'@ | node
rg -n 'zszj-dependencies|Reactor Build Order' outputs -g '*.log' -m 2" in E:\zszj-wt-bpm-001-a
 exited 1 in 0ms:
FILE services/zhongshu-core/.flattened-pom.xml
<?xml version="1.0" encoding="UTF-8"?>
<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <modelVersion>4.0.0</modelVersion>
  <groupId>cn.zszj</groupId>
  <artifactId>zszj</artifactId>
  <version>2026.08-SNAPSHOT</version>
  <packaging>pom</packaging>
  <name>zszj</name>
  <description>众墅之家项目基础脚手架</description>
  <url>https://github.com/YunaiV/ruoyi-vue-pro</url>
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
FILE services/zhongshu-core/zszj-module-system/.flattened-pom.xml
<?xml version="1.0" encoding="UTF-8"?>
<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <modelVersion>4.0.0</modelVersion>
  <groupId>cn.zszj</groupId>
  <artifactId>zszj-module-system</artifactId>
  <version>2026.08-SNAPSHOT</version>
  <name>zszj-module-system</name>
  <description>system 模块下，我们放通用业务，支撑上层的核心业务。
        例如说：用户、部门、权限、数据字典等等</description>
  <url>https://github.com/YunaiV/ruoyi-vue-pro/zszj-module-system</url>
  <dependencies>
    <dependency>
      <groupId>cn.zszj</groupId>
      <artifactId>zszj-module-infra</artifactId>
      <version>2026.08-SNAPSHOT</version>
      <scope>compile</scope>
    </dependency>
    <dependency>
      <groupId>cn.zszj</groupId>
      <artifactId>zszj-spring-boot-starter-biz-data-permission</artifactId>
      <version>2026.08-SNAPSHOT</version>
FILE services/zhongshu-core/zszj-module-bpm/.flattened-pom.xml
<?xml version="1.0" encoding="UTF-8"?>
<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <modelVersion>4.0.0</modelVersion>
  <groupId>cn.zszj</groupId>
  <artifactId>zszj-module-bpm</artifactId>
  <version>2026.08-SNAPSHOT</version>
  <name>zszj-module-bpm</name>
  <description>bpm 包下，业务流程管理（Business Process Management），我们放工作流的功能。
        例如说：流程定义、表单配置、审核中心（我的申请、我的待办、我的已办）等等
        bpm 解释：https://baike.baidu.com/item/BPM/1933

        工作流基于 Flowable 6 实现，分成流程定义、流程表单、流程实例、流程任务等功能模块。</description>
  <url>https://github.com/YunaiV/ruoyi-vue-pro/zszj-module-bpm</url>
  <dependencies>
    <dependency>
      <groupId>cn.zszj</groupId>
      <artifactId>zszj-module-system</artifactId>
      <version>2026.08-SNAPSHOT</version>
      <scope>compile</scope>
    </dependency>
    <dependency>

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "@'
const {spawnSync}=require('child_process');
const {resolve}=require('path');
const env={...process.env,JAVA_HOME:resolve('tools/jdk-17.0.20.1+1'),PATH:resolve('tools/jdk-17.0.20.1+1/bin')+';'+resolve('tools/apache-maven-3.9.9/bin')+';'+process.env.PATH};
const r=spawnSync('cmd.exe',['/d','/s','/c','mvn -o -B -f services/zhongshu-core/pom.xml -pl zszj-module-system,zszj-module-infra -am validate -Dmaven.test.skip=true'],{env,encoding:'utf8',timeout:60000,maxBuffer:8*1024*1024});
console.log('status='+r.status+' error='+(r.error?.message??''));
const lines=((r.stdout??'')+(r.stderr??'')).split('\\n');
console.log(lines.slice(0,42).join('\\n'));
console.log(lines.slice(-35).join('\\n'));
'@ | node" in E:\zszj-wt-bpm-001-a
 succeeded in 655ms:
status=1 error=
[ERROR] Could not create local repository at C:\.m2\repository -> [Help 1]
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR] 
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/LocalRepositoryNotAccessibleException

[ERROR] Could not create local repository at C:\.m2\repository -> [Help 1]
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR] 
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/LocalRepositoryNotAccessibleException


codex
故障注入已确认：引导后的结构查询报错或返回空值时，runtime 整段未执行，报告却是 `failCount=0`、退出码 0。报告陈旧、跳过、数量或身份不符、Maven 非零，以及常规收尾清理失败，均会被拦截。另已复现 Windows PATH 回退误判：同一环境中 `cmd.exe /c mvn -v` 成功，新增的直接 `spawnSync('mvn')` 返回 `ENOENT`。

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "rg -n 'name=\"(user.home|maven.repo.local|maven.home)\"' services/zhongshu-core/zszj-module-bpm/target/surefire-reports -g '*Harness*.xml'; Get-Item 'C:/Users/Administrator/.m2/repository' -ErrorAction SilentlyContinue | Select-Object FullName; Get-Item 'C:/.m2/repository' -ErrorAction SilentlyContinue | Select-Object FullName; rg -n 'localRepository' tools/apache-maven-3.9.9/conf/settings.xml; rg --files tools/apache-maven-3.9.9/lib -g '*core*' -g '*model*'" in E:\zszj-wt-bpm-001-a
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "@'
const fs=require('fs');
const f='scripts/db/run-bpm001-verify.mjs';
console.log(fs.readFileSync(f,'utf8').split('\\n').slice(245,274).map((s,i)=>(246+i)+': '+s).join('\\n'));
'@ | node
git diff --quiet 3ea8051c HEAD"'^ -- .github/workflows/pg-regression.yml scripts/db/run-bpm001-verify.mjs services/zhongshu-core/zszj-module-bpm services/zhongshu-core/docs/BPM引擎表与业务扩展表迁移责任决策.md; Write-Output $LASTEXITCODE' in E:\zszj-wt-bpm-001-a
 succeeded in 0ms:

49:  <!-- localRepository
53:  <localRepository>/path/to/local/repo</localRepository>
tools/apache-maven-3.9.9/lib\maven-model-builder-3.9.9.jar
tools/apache-maven-3.9.9/lib\maven-model-3.9.9.jar
tools/apache-maven-3.9.9/lib\maven-core-3.9.9.jar
tools/apache-maven-3.9.9/lib\httpcore.license
tools/apache-maven-3.9.9/lib\httpcore-4.4.16.jar
FullName                             
--------                             
C:\Users\Administrator\.m2\repository



 succeeded in 0ms:
246: }
247: 
248: // ---- 阶段1 bootstrap（owner 账号：建表 + 部署 + 发起 + 留积压）----
249: console.log('[bpm001] 阶段1 bootstrap：owner 账号建引擎表/部署中性夹具/发起实例/留异步积压…');
250: mkdirSync(outDir, { recursive: true });
251: const bootAssert = assertSurefire('BpmPgHarnessBootstrapTest', 5);
252: const bootLog = runHarnessTest('BpmPgHarnessBootstrapTest', 'zhongshu_owner', 'owner_local_1', 'true', 'false', 30 * 60 * 1000);
253: writeFileSync(join(outDir, 'bootstrap.log'), bootLog.output);
254: let bootOk = false, bootNote = '';
255: try { bootNote = bootAssert(); bootOk = bootLog.status === 0; if (!bootOk) bootNote += `，但 mvn exit=${bootLog.status}`; }
256: catch (e) { bootNote = String(e.message ?? e); }
257: record('P1 bootstrap 引擎建表/部署幂等/发起/租户标签/版本钉住', bootOk, bootNote);
258: if (!bootOk) {
259:   console.error('[bpm001] bootstrap 阶段失败，日志见 outputs/bpm-001/bootstrap.log（尾部）：\n' + bootLog.output.split('\n').slice(-40).join('\n'));
260:   finish();
261: }
262: 
263: try {
264:   actAfterBootstrap = countActFlw('act\\_%');
265:   versionAfterBootstrap = schemaVersion();
266: } catch (e) {
267:   console.error('[bpm001] 引导后结构读取失败：' + String(e.message ?? e));
268:   finish();
269: }
270: 
271: // ---- 阶段2 runtime（app 低权限：重启恢复 + 全操作断言 + 零 DDL；执行器由 R70 显式启停）----
272: console.log('[bpm001] 阶段2 runtime：app 低权限重启接入同库/审批操作/事务回滚/积压恢复…');
273: const runAssert = assertSurefire('BpmPgHarnessRuntimeTest', 9);
274: const runLog = runHarnessTest('BpmPgHarnessRuntimeTest', 'zhongshu_app', 'app_local_1', 'false', 'false', 20 * 60 * 1000);
0

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "@'
const {spawnSync}=require('child_process');
const {resolve}=require('path');
const env={...process.env,JAVA_HOME:resolve('tools/jdk-17.0.20.1+1'),PATH:resolve('tools/jdk-17.0.20.1+1/bin')+';'+resolve('tools/apache-maven-3.9.9/bin')+';'+process.env.PATH};
const r=spawnSync('cmd.exe',['/d','/s','/c','mvn -o -B -f services/zhongshu-core/pom.xml -pl zszj-module-system,zszj-module-infra -am validate -Dmaven.test.skip=true -Dmaven.repo.local=C:/Users/Administrator/.m2/repository'],{env,encoding:'utf8',timeout:60000,maxBuffer:8*1024*1024});
console.log('status='+r.status+' error='+(r.error?.message??''));
const lines=((r.stdout??'')+(r.stderr??'')).split('\\n');
console.log(lines.slice(0,40).join('\\n'));
console.log(lines.slice(-33).join('\\n'));
'@ | node" in E:\zszj-wt-bpm-001-a
 succeeded in 2112ms:
status=0 error=
[INFO] Scanning for projects...
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Build Order:
[INFO] 
[INFO] zszj                                                               [pom]
[INFO] zszj-framework                                                     [pom]
[INFO] zszj-common                                                        [jar]
[INFO] zszj-spring-boot-starter-monitor                                   [jar]
[INFO] zszj-spring-boot-starter-web                                       [jar]
[INFO] zszj-spring-boot-starter-security                                  [jar]
[INFO] zszj-spring-boot-starter-mybatis                                   [jar]
[INFO] zszj-spring-boot-starter-redis                                     [jar]
[INFO] zszj-spring-boot-starter-mq                                        [jar]
[INFO] zszj-spring-boot-starter-job                                       [jar]
[INFO] zszj-spring-boot-starter-test                                      [jar]
[INFO] zszj-spring-boot-starter-biz-tenant                                [jar]
[INFO] zszj-spring-boot-starter-websocket                                 [jar]
[INFO] zszj-spring-boot-starter-protection                                [jar]
[INFO] zszj-spring-boot-starter-biz-ip                                    [jar]
[INFO] zszj-spring-boot-starter-excel                                     [jar]
[INFO] zszj-spring-boot-starter-biz-data-permission                       [jar]
[INFO] zszj-module-infra                                                  [jar]
[INFO] zszj-module-system                                                 [jar]
[INFO] 
[INFO] ----------------------------< cn.zszj:zszj >----------------------------
[INFO] Building zszj 2026.08-SNAPSHOT                                    [1/19]
[INFO]   from pom.xml
[INFO] --------------------------------[ pom ]---------------------------------
[INFO] 
[INFO] --- enforcer:3.5.0:enforce (enforce-jdk17-baseline) @ zszj ---
[INFO] Rule 0: org.apache.maven.enforcer.rules.version.RequireJavaVersion passed
[INFO] Rule 1: org.apache.maven.enforcer.rules.version.RequireMavenVersion passed
[INFO] 
[INFO] -----------------------< cn.zszj:zszj-framework >-----------------------
[INFO] Building zszj-framework 2026.08-SNAPSHOT                          [2/19]
[INFO]   from zszj-framework\pom.xml
[INFO] --------------------------------[ pom ]---------------------------------
[INFO] 
[INFO] --- enforcer:3.5.0:enforce (enforce-jdk17-baseline) @ zszj-framework ---
[INFO] 
[INFO]   from zszj-module-system\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- enforcer:3.5.0:enforce (enforce-jdk17-baseline) @ zszj-module-system ---
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for zszj 2026.08-SNAPSHOT:
[INFO] 
[INFO] zszj ............................................... SUCCESS [  0.177 s]
[INFO] zszj-framework ..................................... SUCCESS [  0.004 s]
[INFO] zszj-common ........................................ SUCCESS [  0.189 s]
[INFO] zszj-spring-boot-starter-monitor ................... SUCCESS [  0.065 s]
[INFO] zszj-spring-boot-starter-web ....................... SUCCESS [  0.028 s]
[INFO] zszj-spring-boot-starter-security .................. SUCCESS [  0.047 s]
[INFO] zszj-spring-boot-starter-mybatis ................... SUCCESS [  0.271 s]
[INFO] zszj-spring-boot-starter-redis ..................... SUCCESS [  0.022 s]
[INFO] zszj-spring-boot-starter-mq ........................ SUCCESS [  0.059 s]
[INFO] zszj-spring-boot-starter-job ....................... SUCCESS [  0.004 s]
[INFO] zszj-spring-boot-starter-test ...................... SUCCESS [  0.015 s]
[INFO] zszj-spring-boot-starter-biz-tenant ................ SUCCESS [  0.017 s]
[INFO] zszj-spring-boot-starter-websocket ................. SUCCESS [  0.019 s]
[INFO] zszj-spring-boot-starter-protection ................ SUCCESS [  0.020 s]
[INFO] zszj-spring-boot-starter-biz-ip .................... SUCCESS [  0.007 s]
[INFO] zszj-spring-boot-starter-excel ..................... SUCCESS [  0.030 s]
[INFO] zszj-spring-boot-starter-biz-data-permission ....... SUCCESS [  0.008 s]
[INFO] zszj-module-infra .................................. SUCCESS [  0.086 s]
[INFO] zszj-module-system ................................. SUCCESS [  0.056 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  1.360 s
[INFO] Finished at: 2026-09-14T03:33:56+08:00
[INFO] ------------------------------------------------------------------------


exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "@'
const {spawnSync}=require('child_process');
const {resolve}=require('path');
const env={...process.env,JAVA_HOME:resolve('tools/jdk-17.0.20.1+1'),PATH:resolve('tools/jdk-17.0.20.1+1/bin')+';'+resolve('tools/apache-maven-3.9.9/bin')+';'+process.env.PATH};
const r=spawnSync('cmd.exe',['/d','/s','/c','mvn -o -B -f services/zhongshu-core/zszj-module-bpm/pom.xml validate -Dmaven.repo.local=E:/zszj-wt-bpm-001-a/outputs'],{env,encoding:'utf8',timeout:20000,maxBuffer:4*1024*1024});
console.log('status='+r.status+' error='+(r.error?.message??''));
console.log(((r.stdout??'')+(r.stderr??'')).split('\\n').slice(0,30).join('\\n'));
'@ | node" in E:\zszj-wt-bpm-001-a
 succeeded in 719ms:
status=1 error=
[INFO] Scanning for projects...
[ERROR] [ERROR] Some problems were encountered while processing the POMs:
[ERROR] Non-resolvable import POM: The following artifacts could not be resolved: cn.zszj:zszj-dependencies:pom:2026.08-SNAPSHOT (absent): Cannot access huaweicloud (https://mirrors.huaweicloud.com/repository/maven/) in offline mode and the artifact cn.zszj:zszj-dependencies:pom:2026.08-SNAPSHOT has not been downloaded from it before. @ cn.zszj:zszj:${revision}, E:\zszj-wt-bpm-001-a\services\zhongshu-core\pom.xml, line 59, column 25
[ERROR] 'dependencies.dependency.version' for cn.zszj:zszj-spring-boot-starter-biz-data-permission:jar is missing. @ cn.zszj:zszj-module-bpm:${revision}, E:\zszj-wt-bpm-001-a\services\zhongshu-core\zszj-module-bpm\pom.xml, line 31, column 21
[ERROR] 'dependencies.dependency.version' for cn.zszj:zszj-spring-boot-starter-biz-tenant:jar is missing. @ cn.zszj:zszj-module-bpm:${revision}, E:\zszj-wt-bpm-001-a\services\zhongshu-core\zszj-module-bpm\pom.xml, line 35, column 21
[ERROR] 'dependencies.dependency.version' for cn.zszj:zszj-spring-boot-starter-web:jar is missing. @ cn.zszj:zszj-module-bpm:${revision}, E:\zszj-wt-bpm-001-a\services\zhongshu-core\zszj-module-bpm\pom.xml, line 41, column 21
[ERROR] 'dependencies.dependency.version' for cn.zszj:zszj-spring-boot-starter-security:jar is missing. @ cn.zszj:zszj-module-bpm:${revision}, E:\zszj-wt-bpm-001-a\services\zhongshu-core\zszj-module-bpm\pom.xml, line 46, column 21
[ERROR] 'dependencies.dependency.version' for cn.zszj:zszj-spring-boot-starter-mybatis:jar is missing. @ cn.zszj:zszj-module-bpm:${revision}, E:\zszj-wt-bpm-001-a\services\zhongshu-core\zszj-module-bpm\pom.xml, line 52, column 21
[ERROR] 'dependencies.dependency.version' for cn.zszj:zszj-spring-boot-starter-test:jar is missing. @ cn.zszj:zszj-module-bpm:${revision}, E:\zszj-wt-bpm-001-a\services\zhongshu-core\zszj-module-bpm\pom.xml, line 58, column 21
[ERROR] 'dependencies.dependency.version' for cn.zszj:zszj-spring-boot-starter-excel:jar is missing. @ cn.zszj:zszj-module-bpm:${revision}, E:\zszj-wt-bpm-001-a\services\zhongshu-core\zszj-module-bpm\pom.xml, line 64, column 21
[ERROR] 'dependencies.dependency.version' for org.flowable:flowable-spring-boot-starter-process:jar is missing. @ cn.zszj:zszj-module-bpm:${revision}, E:\zszj-wt-bpm-001-a\services\zhongshu-core\zszj-module-bpm\pom.xml, line 70, column 21
[ERROR] 'dependencies.dependency.version' for org.flowable:flowable-spring-boot-starter-actuator:jar is missing. @ cn.zszj:zszj-module-bpm:${revision}, E:\zszj-wt-bpm-001-a\services\zhongshu-core\zszj-module-bpm\pom.xml, line 74, column 21
[ERROR] 'dependencies.dependency.version' for org.postgresql:postgresql:jar is missing. @ cn.zszj:zszj-module-bpm:${revision}, E:\zszj-wt-bpm-001-a\services\zhongshu-core\zszj-module-bpm\pom.xml, line 80, column 21
 @ 
[ERROR] The build could not read 1 project -> [Help 1]
[ERROR]   
[ERROR]   The project cn.zszj:zszj-module-bpm:2026.08-SNAPSHOT (E:\zszj-wt-bpm-001-a\services\zhongshu-core\zszj-module-bpm\pom.xml) has 11 errors
[ERROR]     Non-resolvable import POM: The following artifacts could not be resolved: cn.zszj:zszj-dependencies:pom:2026.08-SNAPSHOT (absent): Cannot access huaweicloud (https://mirrors.huaweicloud.com/repository/maven/) in offline mode and the artifact cn.zszj:zszj-dependencies:pom:2026.08-SNAPSHOT has not been downloaded from it before. @ cn.zszj:zszj:${revision}, E:\zszj-wt-bpm-001-a\services\zhongshu-core\pom.xml, line 59, column 25 -> [Help 2]
[ERROR]     'dependencies.dependency.version' for cn.zszj:zszj-spring-boot-starter-biz-data-permission:jar is missing. @ cn.zszj:zszj-module-bpm:${revision}, E:\zszj-wt-bpm-001-a\services\zhongshu-core\zszj-module-bpm\pom.xml, line 31, column 21
[ERROR]     'dependencies.dependency.version' for cn.zszj:zszj-spring-boot-starter-biz-tenant:jar is missing. @ cn.zszj:zszj-module-bpm:${revision}, E:\zszj-wt-bpm-001-a\services\zhongshu-core\zszj-module-bpm\pom.xml, line 35, column 21
[ERROR]     'dependencies.dependency.version' for cn.zszj:zszj-spring-boot-starter-web:jar is missing. @ cn.zszj:zszj-module-bpm:${revision}, E:\zszj-wt-bpm-001-a\services\zhongshu-core\zszj-module-bpm\pom.xml, line 41, column 21
[ERROR]     'dependencies.dependency.version' for cn.zszj:zszj-spring-boot-starter-security:jar is missing. @ cn.zszj:zszj-module-bpm:${revision}, E:\zszj-wt-bpm-001-a\services\zhongshu-core\zszj-module-bpm\pom.xml, line 46, column 21
[ERROR]     'dependencies.dependency.version' for cn.zszj:zszj-spring-boot-starter-mybatis:jar is missing. @ cn.zszj:zszj-module-bpm:${revision}, E:\zszj-wt-bpm-001-a\services\zhongshu-core\zszj-module-bpm\pom.xml, line 52, column 21
[ERROR]     'dependencies.dependency.version' for cn.zszj:zszj-spring-boot-starter-test:jar is missing. @ cn.zszj:zszj-module-bpm:${revision}, E:\zszj-wt-bpm-001-a\services\zhongshu-core\zszj-module-bpm\pom.xml, line 58, column 21
[ERROR]     'dependencies.dependency.version' for cn.zszj:zszj-spring-boot-starter-excel:jar is missing. @ cn.zszj:zszj-module-bpm:${revision}, E:\zszj-wt-bpm-001-a\services\zhongshu-core\zszj-module-bpm\pom.xml, line 64, column 21
[ERROR]     'dependencies.dependency.version' for org.flowable:flowable-spring-boot-starter-process:jar is missing. @ cn.zszj:zszj-module-bpm:${revision}, E:\zszj-wt-bpm-001-a\services\zhongshu-core\zszj-module-bpm\pom.xml, line 70, column 21
[ERROR]     'dependencies.dependency.version' for org.flowable:flowable-spring-boot-starter-actuator:jar is missing. @ cn.zszj:zszj-module-bpm:${revision}, E:\zszj-wt-bpm-001-a\services\zhongshu-core\zszj-module-bpm\pom.xml, line 74, column 21
[ERROR]     'dependencies.dependency.version' for org.postgresql:postgresql:jar is missing. @ cn.zszj:zszj-module-bpm:${revision}, E:\zszj-wt-bpm-001-a\services\zhongshu-core\zszj-module-bpm\pom.xml, line 80, column 21
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.

codex
CI 供给还有一个遗漏：`-pl system,infra -am` 的实际构建列表不包含 `zszj-dependencies` BOM。BPM 独立构建仍需从本地仓库解析它；在没有该 BOM 的仓库中，已复现 `Non-resolvable import POM`。因此新增安装步骤还不能闭合第 1 项。

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "rg -n 'installArgs|mvn -B|const probe =|引导后结构读取失败|process.exit\\(failCount' .github/workflows/pg-regression.yml scripts/db/run-bpm001-verify.mjs; git diff --check HEAD"'^ HEAD; git status --short; rg --files outputs/bpm-001' in E:\zszj-wt-bpm-001-a
 succeeded in 18ms:
outputs/bpm-001\runtime.log
outputs/bpm-001\runtime-report.json
outputs/bpm-001\final-run.log
outputs/bpm-001\bootstrap.log

codex
复核 HEAD `8a1639f2` 的五个修复文件；这些文件相对 `3ea8051c` 与实际父提交 `27ee56b8` 的差异一致。

1. **未闭合（P1）：CI 仍缺 BOM 安装。** [pg-regression.yml:29](E:/zszj-wt-bpm-001-a/.github/workflows/pg-regression.yml:29)、[run-bpm001-verify.mjs:168](E:/zszj-wt-bpm-001-a/scripts/db/run-bpm001-verify.mjs:168)：实测 `-pl system,infra -am` 的构建列表不包含 `zszj-dependencies`。BPM 独立构建仍须从仓库解析该 BOM；缺少它时已复现 `Non-resolvable import POM`。两处安装命令均须显式包含该模块。
2. **闭合：** 本轮 XML 删除、报告身份、精确数量、零跳过/失败/错误及 Maven 退出码校验齐全；故障注入均能拦截。
3. **未闭合（P1）：引导后结构查询仍可伪绿。** [run-bpm001-verify.mjs:266](E:/zszj-wt-bpm-001-a/scripts/db/run-bpm001-verify.mjs:266)：异常分支只打印错误并调用 `finish()`，没有记录失败。注入表数查询失败、版本查询失败或空输出，均得到 **runtime 未执行、`failCount=0`、退出码 0**。须先记录 FAIL 再收尾。
4. **未闭合（P2）：SIGTERM 清理仍不可靠。** [run-bpm001-verify.mjs:84](E:/zszj-wt-bpm-001-a/scripts/db/run-bpm001-verify.mjs:84)：常规收尾的三次重试及失败退出已修，但主流程持续使用同步阻塞调用，长时间 Maven 执行期间信号回调无法运行。CI 取消后进一步强杀会绕过清理，仍可能遗留容器和卷；须让事件循环能够处理终止信号。
5. **闭合：** 启动失败后先删除残件、重新随机选择端口，失败日志包含端口和原因。
6. **闭合：** S0 已限定为结构证据，文档区分既有装配证据与待补运行期观测。
7. **闭合：** 历史与运行实例分页均按唯一 ID 排序，非零 OFFSET 与全量切片精确比较，运行实例数量固定为 4。
8. **闭合：** 专用异常断言、成功发起后触发回滚、三面零残留及再次提交成功断言齐全。
9. **闭合：** approve 路径已直证 `approvedEnd=1`、`rejectedEnd=0`。
10. **闭合：** 密码缺失或空白会在连接池创建前抛出 `IllegalStateException`。

另有一项修复引入的新问题：

- **P2：Windows PATH 回退误判工具链缺失。** [run-bpm001-verify.mjs:51](E:/zszj-wt-bpm-001-a/scripts/db/run-bpm001-verify.mjs:51)：探测直接执行 `spawnSync('mvn')`，无法启动 Windows 的 `mvn.cmd`。同一 PATH 下已实测直接调用返回 `ENOENT`，而 `cmd.exe /c mvn -v` 成功。无仓库 tools/ 的正常 Windows 环境会错误退出 3；探测须采用与后续执行一致的 Windows 调用方式。

验证包含静态审查、离线 Maven 校验及内存故障注入；未重跑真实 PostgreSQL 全套。

NEEDS_FIX
tokens used
88,392
复核 HEAD `8a1639f2` 的五个修复文件；这些文件相对 `3ea8051c` 与实际父提交 `27ee56b8` 的差异一致。

1. **未闭合（P1）：CI 仍缺 BOM 安装。** [pg-regression.yml:29](E:/zszj-wt-bpm-001-a/.github/workflows/pg-regression.yml:29)、[run-bpm001-verify.mjs:168](E:/zszj-wt-bpm-001-a/scripts/db/run-bpm001-verify.mjs:168)：实测 `-pl system,infra -am` 的构建列表不包含 `zszj-dependencies`。BPM 独立构建仍须从仓库解析该 BOM；缺少它时已复现 `Non-resolvable import POM`。两处安装命令均须显式包含该模块。
2. **闭合：** 本轮 XML 删除、报告身份、精确数量、零跳过/失败/错误及 Maven 退出码校验齐全；故障注入均能拦截。
3. **未闭合（P1）：引导后结构查询仍可伪绿。** [run-bpm001-verify.mjs:266](E:/zszj-wt-bpm-001-a/scripts/db/run-bpm001-verify.mjs:266)：异常分支只打印错误并调用 `finish()`，没有记录失败。注入表数查询失败、版本查询失败或空输出，均得到 **runtime 未执行、`failCount=0`、退出码 0**。须先记录 FAIL 再收尾。
4. **未闭合（P2）：SIGTERM 清理仍不可靠。** [run-bpm001-verify.mjs:84](E:/zszj-wt-bpm-001-a/scripts/db/run-bpm001-verify.mjs:84)：常规收尾的三次重试及失败退出已修，但主流程持续使用同步阻塞调用，长时间 Maven 执行期间信号回调无法运行。CI 取消后进一步强杀会绕过清理，仍可能遗留容器和卷；须让事件循环能够处理终止信号。
5. **闭合：** 启动失败后先删除残件、重新随机选择端口，失败日志包含端口和原因。
6. **闭合：** S0 已限定为结构证据，文档区分既有装配证据与待补运行期观测。
7. **闭合：** 历史与运行实例分页均按唯一 ID 排序，非零 OFFSET 与全量切片精确比较，运行实例数量固定为 4。
8. **闭合：** 专用异常断言、成功发起后触发回滚、三面零残留及再次提交成功断言齐全。
9. **闭合：** approve 路径已直证 `approvedEnd=1`、`rejectedEnd=0`。
10. **闭合：** 密码缺失或空白会在连接池创建前抛出 `IllegalStateException`。

另有一项修复引入的新问题：

- **P2：Windows PATH 回退误判工具链缺失。** [run-bpm001-verify.mjs:51](E:/zszj-wt-bpm-001-a/scripts/db/run-bpm001-verify.mjs:51)：探测直接执行 `spawnSync('mvn')`，无法启动 Windows 的 `mvn.cmd`。同一 PATH 下已实测直接调用返回 `ENOENT`，而 `cmd.exe /c mvn -v` 成功。无仓库 tools/ 的正常 Windows 环境会错误退出 3；探测须采用与后续执行一致的 Windows 调用方式。

验证包含静态审查、离线 Maven 校验及内存故障注入；未重跑真实 PostgreSQL 全套。

NEEDS_FIX
