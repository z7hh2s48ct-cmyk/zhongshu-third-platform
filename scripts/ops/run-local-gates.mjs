/**
 * ZS-OPS-001.A 本地基线门禁聚合入口（本地与 CI 同一规则）。
 *
 * 并发执行全部本地检查，任一失败即整体失败并汇总（结果按门禁定义顺序稳定输出）：
 *   G1 来源复制校验器单测 / G2 品牌命名门禁单测 / G2b 命名迁移工具单测 / G3 品牌命名全仓扫描
 *   G4 文档一致性单测 / G5 文档一致性全量（ZS-GOV-001）/ G6 模块白名单（ZS-ENG-001）
 *   G7 数据源 PG 合同（ZS-DB-001.A）/ G8 Flyway 迁移规范（ZS-DB-003）/ G9 配置秘密门禁（ZS-CFG-001.A）
 *   G10 Web 类型检查基线（ZS-CLIENT-005.A，较慢；--fast 跳过）
 *   G11 启用模块后端单测（--mvn 显式启用；需 tools/env.sh 工具链，排除已登记的上游基线失败）
 *   G12 安全链联合回归（ZS-SEC-012.B：--mvn 显式启用；真实安全链夹具 + 全面 async/CORS 端到端 + .A 全量，本地与 CI 同入口）
 *   G13 双端请求层合同回归（ZS-SEC-012.B/CLIENT-003：slow，本地 vitest 双端；CI 由 CLIENT-005.A 流水线覆盖）
 * ZS-OPS-001.B/C 已接入：PG 层走 .github/workflows/pg-regression.yml（run-pg-regression.mjs 同规则）、
 * 安全/基础管理 API 层 = G12 + G14（CI 同入口见 local-baseline-gates.yml security/sys001 job）；
 * G15 双端技术联调门禁（ZS-OPS-001.D：自举 sys001 夹具跑 CLIENT-005.B E2E 18 用例；slow+exclusive；
 *   或经 E2E_BASE_URL 直连 deploy/README-local.md 长驻联验环境）；
 * G16 B09 联合门禁（ZS-OPS-001.E：PG 流程 bpm001 + 首链幂等写回/跨组织反向 bpm003 串联；slow+exclusive）。
 *   流程层 CI 由 pg-regression.yml 通道覆盖（bpm001 #9 + bpm003 #21 同入口）。
 *
 * 提速（ZS-GOV-001 提效方案 P1）：
 *   - 并发：默认按 CPU 核数并发跑门禁（--jobs N 覆盖），反馈时间从「各门禁耗时之和」降到「最慢门禁」。
 *   - 增量：--incremental 按 git 变更路径只跑受影响门禁（品牌全仓扫描 G3、秘密门禁 G9 恒定跑；
 *     变更含门禁脚本自身 scripts/ops/、或存在无法归类且非良性的路径时 fail-safe 回退全量）。
 *     仅供日常快速反馈；批次收口与 CI 必须跑全量（--fast 或含 G10/G11），不得以增量结果代替放行。
 *   - --fast 固化：日常开发默认 --fast（跳过较慢的 G10 vue-tsc），批次收口去掉 --fast 跑全量。
 * 用法：node scripts/ops/run-local-gates.mjs [--fast] [--mvn] [--incremental] [--jobs N] [--plan]
 */
import { execFile, spawn, spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { cpus } from 'node:os';

const root = fileURLToPath(new URL('../../', import.meta.url));

// 门禁定义。areas：增量模式下命中这些路径前缀才跑；safety：增量模式恒定跑（全仓合规/安全守卫，
//   或其校验依赖 areas 之外的全仓状态——如 G5 文档链接存在性依赖 docs/ 之外的被链接目标）；
// slow：--fast 跳过；mvn：仅 --mvn 启用。cmd/mvnArgs 与 id 保持与既有基线一致（CI、README 依赖）。
export const GATES = [
  { id: 'G1 来源复制校验器单测', cmd: ['node', '--test', 'scripts/verify-source-copy.test.mjs'], areas: ['scripts/verify-source-copy', 'third_party/'] },
  { id: 'G2 品牌命名门禁单测', cmd: ['node', '--test', 'scripts/brand/verify-brand-naming.test.mjs'], areas: ['scripts/brand/'] },
  { id: 'G2b 命名迁移工具单测（冻结文件排除 + 租户域名唯一）', cmd: ['node', '--test', 'scripts/brand/apply-naming-migration.test.mjs'], areas: ['scripts/brand/', 'services/zhongshu-core/sql/'] },
  { id: 'G3 品牌命名全仓扫描', cmd: ['node', 'scripts/brand/verify-brand-naming.mjs'], areas: ['services/', 'apps/', 'scripts/', 'docs/'], safety: true },
  { id: 'G4 文档一致性单测', cmd: ['node', '--test', 'scripts/gov/verify-docs.test.mjs'], areas: ['scripts/gov/', 'docs/', 'README.md'] },
  { id: 'G5 文档一致性全量', cmd: ['node', 'scripts/gov/verify-docs.mjs'], areas: ['scripts/gov/', 'docs/', 'README.md'], safety: true },
  { id: 'G6 模块白名单', cmd: ['node', 'scripts/eng/verify-module-whitelist.mjs'], areas: ['scripts/eng/', 'services/'] },
  { id: 'G7 数据源 PG 合同', cmd: ['node', 'scripts/db/verify-datasource-pg.mjs'], areas: ['scripts/db/', 'services/'] },
  { id: 'G8 Flyway 迁移规范', cmd: ['node', 'scripts/db/verify-flyway-migrations.mjs'], areas: ['scripts/db/', 'services/'] },
  { id: 'G9 配置秘密门禁', cmd: ['node', 'scripts/cfg/verify-config-secrets.mjs'], areas: ['scripts/cfg/', 'services/', 'apps/'], safety: true },
  { id: 'G10 Web 类型检查基线', cmd: ['node', 'scripts/client/verify-ts-baseline.mjs'], areas: ['apps/zhongshu-admin-web/', 'scripts/client/'], slow: true },
  { id: 'G11 启用模块后端单测（common/infra，排除上游基线失败）', areas: ['services/'], mvn: true,
    mvnArgs: '-pl zszj-framework/zszj-common,zszj-module-infra -am -Dtest=!CodegenEngineUniappTest#testExecute_treeSearch -Dsurefire.failIfNoSpecifiedTests=false test' },
  { id: 'G12 安全链联合回归（ZS-SEC-012.A/.B：真实安全链+全面async+CORS 端到端）', areas: ['services/'], mvn: true,
    mvnArgs: '-pl zszj-framework/zszj-spring-boot-starter-web,zszj-framework/zszj-spring-boot-starter-biz-tenant -am -Dtest=SecurityFilterChainFixtureTest,CrossTenantVisitEnabledFixtureTest,SecurityChainJointRegressionTest,SecurityChainEmbeddedCorsTest,ApiAccessLogFilterAsyncTest -Dsurefire.failIfNoSpecifiedTests=false test' },
  { id: 'G13 双端请求层合同回归（ZS-CLIENT-003：admin-web + miniapp vitest）', areas: ['apps/', 'scripts/ops/'], slow: true,
    cmd: ['node', 'scripts/ops/run-client-contract-tests.mjs'] },
  { id: 'G15 双端技术联调门禁（ZS-OPS-001.D：CLIENT-005.B E2E 18 用例，自举夹具或长驻联验环境）', areas: ['scripts/client005b/', 'scripts/sys001/', 'scripts/ops/', 'services/'], slow: true, exclusive: true,
    cmd: ['node', 'scripts/ops/run-b06-joint-gate.mjs'] }, // 自举夹具含 clean 重建，与 mvn 门禁串行
  { id: 'G14 基础管理 API 层回归（ZS-SYS-001.A：七类矩阵 50 用例，Docker PG/Redis + 真实 server）', areas: ['scripts/sys001/', 'services/'], slow: true, exclusive: true,
    cmd: ['node', 'scripts/sys001/run-sys001-regression.mjs'] }, // exclusive：clean 重建共享 target，须与 mvn 门禁串行（codex r0 P2）
  { id: 'G16 B09 联合门禁（ZS-OPS-001.E：PG 流程 bpm001 + 首链幂等写回/跨组织反向 bpm003）', areas: ['scripts/db/', 'services/'], slow: true, exclusive: true,
    cmd: ['node', 'scripts/ops/run-b09-joint-gate.mjs'] }, // exclusive：串联两个自管理 Docker PG 套件，串行防负载竞态
];

// 增量模式下视为「良性、不触发全量回退」的未归类路径前缀（生成物/评审原始稿/计划稿）
const BENIGN_IGNORE = ['outputs/', 'docs/reviews/', '.omx/'];

/** 构造本次要跑的门禁候选（尊重 --fast/--mvn），不含增量筛选。 */
export function candidateGates({ fast = false, mvn = false } = {}) {
  return GATES.filter((g) => {
    if (g.slow && fast) return false; // G10：--fast 跳过较慢的 Web 类型检查
    if (g.mvn && !mvn) return false;  // G11：仅 --mvn 显式启用
    return true;
  });
}

/**
 * 增量筛选：按变更路径选门禁；任何不确定都 fail-safe 回退全量（宁多跑不漏跑）。
 * 返回 { gates, mode: 'full'|'incremental', reason, changed }。
 * @param {object} o
 * @param {boolean} o.fast @param {boolean} o.mvn @param {boolean} o.incremental
 * @param {string[]|null} o.changedFiles 变更路径（相对仓库根，正斜杠）；null 表示由调用方检测
 */
export function planGates({ fast = false, mvn = false, incremental = false, changedFiles = null }) {
  const candidates = candidateGates({ fast, mvn });
  if (!incremental) return { gates: candidates, mode: 'full', reason: '未启用 --incremental，跑全量', changed: changedFiles ?? [] };

  const changed = changedFiles ?? [];
  if (!changed.length) return { gates: candidates, mode: 'full', reason: '增量：无变更或变更检测失败，回退全量', changed };
  if (changed.some((f) => f.startsWith('scripts/ops/'))) return { gates: candidates, mode: 'full', reason: '增量：变更含门禁脚本自身（scripts/ops/），回退全量', changed };

  // 未归类且非良性路径 → 保守回退全量（无法判断影响面）
  const allAreas = candidates.flatMap((g) => g.areas ?? []);
  const unmapped = changed.filter((f) => !allAreas.some((a) => f.startsWith(a)) && !BENIGN_IGNORE.some((b) => f.startsWith(b)));
  if (unmapped.length) return { gates: candidates, mode: 'full', reason: `增量：${unmapped.length} 个变更路径无法归类（${unmapped.slice(0, 3).join(', ')}${unmapped.length > 3 ? ' …' : ''}），回退全量`, changed };

  const selected = candidates.filter((g) => g.safety || (g.areas ?? []).some((a) => changed.some((f) => f.startsWith(a))));
  return { gates: selected, mode: 'incremental', reason: `增量：按 ${changed.length} 个变更路径选中 ${selected.length}/${candidates.length} 门禁`, changed };
}

/** 检测工作树相对 HEAD 的变更路径（含已跟踪修改与未跟踪新文件）；失败返回 []。 */
function detectChangedFiles() {
  try {
    const opts = { cwd: root, encoding: 'utf8' };
    // --no-renames：跨目录 rename 默认只报目标路径，会漏掉源路径所属区域的门禁（如 Web→miniapp
    // 移动 .ts 只报 miniapp 侧、漏选校 Web 导入的 G10）；关闭 rename 检测使源(删)+目标(增)都上报。
    const tracked = spawnSync('git', ['-c', 'core.quotepath=false', 'diff', '--name-only', '--no-renames', 'HEAD'], opts);
    const untracked = spawnSync('git', ['-c', 'core.quotepath=false', 'ls-files', '--others', '--exclude-standard'], opts);
    if (tracked.status !== 0 || untracked.status !== 0) return [];
    const paths = [...String(tracked.stdout ?? '').split('\n'), ...String(untracked.stdout ?? '').split('\n')]
      .map((s) => s.trim().replaceAll('\\', '/')).filter(Boolean);
    return [...new Set(paths)];
  } catch { return []; }
}

/** 并发跑单个门禁，返回 { id, status, ms, tail }（不抛异常，失败以 status 表达）。 */
function runGate(gate) {
  const started = Date.now();
  return new Promise((resolve) => {
    const done = (status, output) => resolve({
      id: gate.id, status, ms: Date.now() - started,
      tail: String(output ?? '').trimEnd().split('\n').slice(-4).join(' | ').slice(0, 400),
    });
    if (gate.mvnArgs) {
      // Maven 门禁需先注入 tools 工具链环境（JDK17/Maven 不在系统 PATH）
      const child = spawn('bash', ['-c', 'source tools/env.sh && cd services/zhongshu-core && MSYS_NO_PATHCONV=1 "$TOOLS/apache-maven-3.9.9/bin/mvn.cmd" ' + gate.mvnArgs],
        { cwd: root, stdio: ['ignore', 'pipe', 'pipe'] });
      let out = '';
      child.stdout.on('data', (d) => { out += d; });
      child.stderr.on('data', (d) => { out += d; });
      child.on('error', (e) => done('FAIL', out + String(e)));
      child.on('close', (code) => done(code === 0 ? 'PASS' : 'FAIL', out));
    } else {
      execFile(gate.cmd[0], gate.cmd.slice(1), { cwd: root, maxBuffer: 64 * 1024 * 1024, encoding: 'utf8' },
        (err, stdout, stderr) => done(err ? 'FAIL' : 'PASS', err ? String(stdout ?? '') + String(stderr ?? '') : String(stdout ?? '')));
    }
  });
}

/** 以 jobs 上限并发跑门禁；结果按 gates 定义顺序返回（与完成顺序无关），完成即时打印进度。 */
async function runGates(gates, jobs) {
  const results = new Array(gates.length);
  const indexOf = new Map(gates.map((g, i) => [g, i]));
  const exclusive = gates.filter((g) => g.exclusive); // 独占门禁（G14 会 clean 重建共享 target）与 mvn 门禁串行，防并发互踩（codex r0 P2）
  const normal = gates.filter((g) => !g.exclusive);
  let next = 0;
  const worker = async () => {
    while (next < normal.length) {
      const i = indexOf.get(normal[next++]);
      const r = await runGate(gates[i]);
      results[i] = r;
      console.log(`[${r.status}] ${r.id} (${r.ms}ms)`);
    }
  };
  const serial = async () => {
    for (const gate of exclusive) {
      const r = await runGate(gate);
      results[indexOf.get(gate)] = r;
      console.log(`[${r.status}] ${r.id} (${r.ms}ms)`);
    }
  };
  // codex r1 P1：独占道与并发池必须先后而非同起——否则 G14 仍与 G11/G12 重叠、--jobs 1 也会双跑
  await Promise.all(Array.from({ length: Math.min(jobs, normal.length) }, worker));
  await serial();
  return results;
}

async function main() {
  const argv = process.argv.slice(2);
  const fast = argv.includes('--fast');
  const mvn = argv.includes('--mvn');
  const incremental = argv.includes('--incremental');
  const planOnly = argv.includes('--plan');
  const jobsIdx = argv.indexOf('--jobs');
  const jobs = Math.max(1, jobsIdx >= 0 ? (Number(argv[jobsIdx + 1]) || 1) : (cpus().length || 4));

  const changedFiles = incremental ? detectChangedFiles() : [];
  const plan = planGates({ fast, mvn, incremental, changedFiles });
  if (incremental) console.log(`[增量] ${plan.reason}`);

  if (planOnly) {
    console.log(`将运行 ${plan.gates.length} 项门禁（--plan，不执行）：`);
    for (const g of plan.gates) console.log('  - ' + g.id);
    return 0;
  }

  console.log(`运行 ${plan.gates.length} 项门禁（模式 ${plan.mode}，并发 ${Math.min(jobs, plan.gates.length)}${fast ? '，--fast' : ''}${mvn ? '，--mvn' : ''}）…`);
  const t0 = Date.now();
  const results = await runGates(plan.gates, jobs);
  const totalMs = Date.now() - t0;

  console.log('\n===== 本地基线门禁汇总 =====');
  for (const r of results) console.log(`${r.status.padEnd(4)}  ${r.id}`);
  const failed = results.filter((r) => r.status === 'FAIL');
  if (failed.length) {
    console.log('\n----- 失败详情（输出末尾）-----');
    for (const r of failed) console.log(`[FAIL] ${r.id}\n  ${r.tail}`);
  }
  console.log(`合计 ${results.length} 项，失败 ${failed.length} 项（并发耗时 ${totalMs}ms）`);
  return failed.length ? 1 : 0;
}

const invokedDirectly = process.argv[1] && process.argv[1].replaceAll('\\', '/').endsWith('scripts/ops/run-local-gates.mjs');
if (invokedDirectly) {
  main().then((code) => process.exit(code)).catch((e) => { console.error(e); process.exit(1); });
}
