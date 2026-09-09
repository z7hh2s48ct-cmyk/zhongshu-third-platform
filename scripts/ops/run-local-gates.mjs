/**
 * ZS-OPS-001.A 本地基线门禁聚合入口（本地与 CI 同一规则）。
 *
 * 依次执行全部本地检查（快 → 慢），任一失败即整体失败并汇总：
 *   1. 来源复制校验器单测（verify-source-copy.test.mjs）
 *   2. 品牌命名门禁单测 + 命名迁移工具单测 + 全仓扫描
 *   3. 文档一致性单测 + 全文档扫描（ZS-GOV-001）
 *   4. 模块白名单静态检查（ZS-ENG-001）
 *   5. 数据源 PG 合同检查（ZS-DB-001.A）
 *   6. Flyway 迁移规范检查（ZS-DB-003）
 *   7. 配置秘密门禁（ZS-CFG-001.A）
 *   8. Web 类型检查基线（ZS-CLIENT-005.A，较慢；--fast 跳过）
 *   9. 启用模块后端单测（--mvn 显式启用；需 tools/env.sh 工具链，排除已登记的上游基线失败）
 * PG/多端 E2E 门禁按 ZS-OPS-001.B~.E 批次接入，不在本骨架。
 * 用法：node scripts/ops/run-local-gates.mjs [--fast] [--mvn]
 */
import { execFileSync, spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));
const fast = process.argv.includes('--fast');
const withMvn = process.argv.includes('--mvn');

const gates = [
  { id: 'G1 来源复制校验器单测', cmd: ['node', '--test', 'scripts/verify-source-copy.test.mjs'] },
  { id: 'G2 品牌命名门禁单测', cmd: ['node', '--test', 'scripts/brand/verify-brand-naming.test.mjs'] },
  { id: 'G2b 命名迁移工具单测（冻结文件排除 + 租户域名唯一）', cmd: ['node', '--test', 'scripts/brand/apply-naming-migration.test.mjs'] },
  { id: 'G3 品牌命名全仓扫描', cmd: ['node', 'scripts/brand/verify-brand-naming.mjs'] },
  { id: 'G4 文档一致性单测', cmd: ['node', '--test', 'scripts/gov/verify-docs.test.mjs'] },
  { id: 'G5 文档一致性全量', cmd: ['node', 'scripts/gov/verify-docs.mjs'] },
  { id: 'G6 模块白名单', cmd: ['node', 'scripts/eng/verify-module-whitelist.mjs'] },
  { id: 'G7 数据源 PG 合同', cmd: ['node', 'scripts/db/verify-datasource-pg.mjs'] },
  { id: 'G8 Flyway 迁移规范', cmd: ['node', 'scripts/db/verify-flyway-migrations.mjs'] },
  { id: 'G9 配置秘密门禁', cmd: ['node', 'scripts/cfg/verify-config-secrets.mjs'] },
];
if (!fast) {
  gates.push({ id: 'G10 Web 类型检查基线', cmd: ['node', 'scripts/client/verify-ts-baseline.mjs'] });
}
if (withMvn) {
  gates.push({ id: 'G11 启用模块后端单测（common/infra，排除上游基线失败）',
    mvnArgs: "-pl zszj-framework/zszj-common,zszj-module-infra -am -Dtest=!CodegenEngineUniappTest#testExecute_treeSearch -Dsurefire.failIfNoSpecifiedTests=false test" });
}

const results = [];
let failed = false;
for (const gate of gates) {
  let status = 'PASS';
  let output = '';
  try {
    if (gate.mvnArgs) {
      // Maven 门禁需先注入 tools 工具链环境（JDK17/Maven 不在系统 PATH）
      const r = spawnSync('bash', ['-c', 'source tools/env.sh && cd services/zhongshu-core && MSYS_NO_PATHCONV=1 "$TOOLS/apache-maven-3.9.9/bin/mvn.cmd" ' + gate.mvnArgs], {
        cwd: root, maxBuffer: 256 * 1024 * 1024, encoding: 'utf8',
        stdio: ['ignore', 'pipe', 'pipe'],
      });
      if (r.status !== 0) throw { stdout: r.stdout ?? '', stderr: r.stderr ?? '' };
      output = r.stdout ?? '';
    } else {
      output = execFileSync(gate.cmd[0], gate.cmd.slice(1), {
        cwd: root,
        maxBuffer: 64 * 1024 * 1024,
        encoding: 'utf8',
        stdio: ['ignore', 'pipe', 'pipe'],
      });
    }
  } catch (e) {
    status = 'FAIL';
    failed = true;
    output = String(e.stdout ?? '') + String(e.stderr ?? '');
  }
  results.push({ id: gate.id, status, tail: output.trimEnd().split('\n').slice(-3).join(' | ').slice(0, 200) });
  console.log(`[${status}] ${gate.id}`);
}

console.log('\n===== 本地基线门禁汇总 =====');
for (const r of results) console.log(`${r.status.padEnd(4)}  ${r.id}`);
const failCount = results.filter((r) => r.status === 'FAIL').length;
console.log(`合计 ${results.length} 项，失败 ${failCount} 项`);
process.exit(failed ? 1 : 0);
