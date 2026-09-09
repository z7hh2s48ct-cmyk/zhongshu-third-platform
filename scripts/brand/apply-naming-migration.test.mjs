/**
 * ZS-BRAND 命名迁移工具单测（hotfix-C）。
 *
 * 覆盖两类不变式：
 *  1. P2-1：冻结/必要保留文件在**任何 scope** 下都不得被选中改写；且每条 PRESERVED
 *     规则都必须真实命中文件（防止写成永不匹配的空规则——同 hotfix-B E-3 的静默失效教训），
 *     同时被保护文件必须"确实会被改写"（证明排除是有载荷的，而非空洞断言）。
 *  2. P2-2：映射到 system_tenant.websites 的上游域名，其目标必须互不相同；
 *     并以全部 SQL 种子的真实数据复核租户间不存在同域分词。
 */
import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import {
  SCOPES, SCOPE_PREFIXES, PRESERVED, preservedReason, selectFiles, planScope, transform, root,
} from './apply-naming-migration.mjs';
import { SQL_SEED_REPLACEMENTS } from './naming-rules.mjs';

const UPGRADE_SQL = 'services/zhongshu-core/sql/postgresql/upgrades/20260908_brand_rename_zszj.sql';
const CODEGEN_README = 'services/zhongshu-core/zszj-module-infra/src/main/resources/codegen/README.md';
const PG_SEED = 'services/zhongshu-core/sql/postgresql/ruoyi-vue-pro.sql';

const gitFiles = (...args) => execFileSync('git', ['ls-files', '-z', ...args], { cwd: root, maxBuffer: 64 * 1024 * 1024 })
  .toString('utf8').split('\0').filter(Boolean);

/** 全部 scope 的候选文件（扣除 SCOPE_EXCLUSIONS 与 PRESERVED 之前），用于检验规则是否空写。 */
const candidates = gitFiles(...new Set(SCOPES.flatMap((s) => SCOPE_PREFIXES[s])));

test('PRESERVED 每条规则都真实命中候选文件（防空写静默失效）', () => {
  for (const p of PRESERVED) {
    const hit = candidates.filter((f) => p.re.test(f));
    assert.ok(hit.length > 0, `PRESERVED 规则永不匹配，属静默失效: ${p.re} （${p.reason}）`);
    assert.ok(p.reason && p.reason.length > 10, `PRESERVED 规则缺少可审计的 reason: ${p.re}`);
  }
});

test('P2-1：冻结迁移脚本与 Flyway 基线在任何 scope 下都不被选中', () => {
  for (const scope of SCOPES) {
    const { files, preserved } = selectFiles(scope);
    for (const f of files) {
      assert.equal(preservedReason(f), null, `scope=${scope} 选中了应冻结的文件: ${f}`);
      assert.ok(!/\/upgrades\//.test(f), `scope=${scope} 选中了 upgrades 迁移脚本: ${f}`);
      assert.ok(!/\/db\/migration\/V[\d.]+__/.test(f), `scope=${scope} 选中了 Flyway 迁移: ${f}`);
    }
    // sql scope 必须确实识别出该升级脚本（证明排除发生在此 scope，而非因前缀不覆盖而侥幸通过）
    if (scope === 'sql') {
      assert.ok(preserved.includes(UPGRADE_SQL),
        `sql scope 未把 ${UPGRADE_SQL} 列为 preserved，排除可能未生效`);
    }
  }
});

test('P2-1：codegen 命名门禁说明文档在 backend scope 下不被选中', () => {
  const { files, preserved } = selectFiles('backend');
  assert.ok(!files.includes(CODEGEN_README), 'codegen README 不应被 backend scope 选中');
  assert.ok(preserved.includes(CODEGEN_README), 'codegen README 应由 PRESERVED 显式排除');
});

test('P2-1：排除是有载荷的——被保护文件经 transform 确实会被改写', () => {
  // 若这些文件本来就不会被改写，上面的"未被选中"断言就是空洞通过。
  const upgrade = readFileSync(root + UPGRADE_SQL, 'utf8');
  assert.notEqual(transform(upgrade, 'sql', UPGRADE_SQL), upgrade,
    '升级脚本经 sql scope 规则本会被改写（旧值谓词被改成新值），故 PRESERVED 排除是必需的');

  const readme = readFileSync(root + CODEGEN_README, 'utf8');
  const rewritten = transform(readme, 'backend', CODEGEN_README);
  assert.notEqual(rewritten, readme,
    'codegen README 经 backend scope 规则本会被改写，故 PRESERVED 排除是必需的');
  // 语义反转的具体形态：禁用清单里的旧名被改成本项目新名
  assert.ok(rewritten.includes('`zszj`') && readme.includes('`yudao`'),
    'codegen README 的"不得出现"清单会被反转为禁用 zszj，属语义破坏');
});

test('P2-1：PRESERVED 未过度排除——种子与常规源码仍被选中', () => {
  const sql = selectFiles('sql');
  assert.ok(sql.files.includes(PG_SEED), 'PG 新装种子必须仍在 sql scope 处理范围内');
  assert.ok(sql.files.length >= 30, `sql scope 选中文件数异常偏少: ${sql.files.length}`);

  const backend = selectFiles('backend');
  assert.ok(backend.files.length >= 8000, `backend scope 选中文件数异常偏少: ${backend.files.length}`);
  assert.ok(backend.files.some((f) => f.endsWith('.java')), 'backend scope 应仍包含 Java 源码');
});

test('P2-1：全部 scope 的 dry-run 试算都不改写冻结文件（按路径形态判定）', () => {
  // 注意：本用例刻意不复用 preservedReason()。若断言写成 preservedReason(f)===null，
  // 则删除任一 PRESERVED 条目时该函数也同步返回 null，断言恒真（变异测试实测为空洞）。
  // 改为直接对 dry-run 结果套用冻结路径形态，才能真实端到端验证试算路径。
  const FROZEN_SHAPES = [
    /\/upgrades\//,
    /\/db\/migration\/V[\d.]+__/,
    /codegen\/README\.md$/,
  ];
  for (const scope of SCOPES) {
    const plan = planScope(scope);
    for (const f of plan.wouldChange) {
      for (const re of FROZEN_SHAPES) {
        assert.ok(!re.test(f), `scope=${scope} 的 dry-run 计划改写冻结文件: ${f}`);
      }
    }
  }
});

test('P2-2：租户 websites 上游域名的映射目标互不相同', () => {
  const map = new Map(SQL_SEED_REPLACEMENTS);
  // 这三个域名在上游种子的 system_tenant.websites 中实际出现（租户 1 / 121 / 122），
  // 而 websites 受 validTenantWebsiteDuplicate 唯一校验并用于 getTenantByWebsite 路由，
  // 故其目标必须两两不同。
  const websiteDomains = ['www.iocoder.cn', 'test.iocoder.cn', 'zsxq.iocoder.cn'];
  const targets = websiteDomains.map((d) => {
    assert.ok(map.has(d), `SQL_SEED_REPLACEMENTS 缺少租户域名映射: ${d}`);
    return map.get(d);
  });
  assert.equal(new Set(targets).size, targets.length,
    `租户 websites 域名映射目标存在冲突: ${JSON.stringify(websiteDomains.map((d, i) => [d, targets[i]]))}`);
  assert.equal(map.get('zsxq.iocoder.cn'), 'zsxq.zszj.example.com',
    'zsxq 必须保留独立占位域，不得与 www 同目标');
  // cloud/doc 合并到同一 doc 目标是有意为之：二者只出现在菜单外链与 OAuth2 回调演示值，
  // 不受唯一性约束、不参与租户路由。此处固化该区别，避免被误当作同类缺陷"顺手修掉"。
  assert.equal(map.get('cloud.iocoder.cn'), map.get('doc.iocoder.cn'));
});

test('P2-2：全部 SQL 种子的租户行不存在跨租户同域，且已完成中性化', () => {
  const DOMAIN = /([a-z0-9.-]*\.(?:iocoder\.cn|zszj\.example\.com))/g;
  const sqlFiles = gitFiles('*.sql');
  let checked = 0;
  for (const f of sqlFiles) {
    const text = readFileSync(root + f, 'utf8');
    if (!text.includes('system_tenant')) continue;
    const rows = [];
    for (const line of text.split(/\r?\n/)) {
      if (!/insert\s+into\s+"?system_tenant"?\s/i.test(line)) continue;
      const id = (line.match(/values\s*\(?\s*'?(\d+)'?/i) || [])[1];
      DOMAIN.lastIndex = 0;
      const doms = [...line.matchAll(DOMAIN)].map((m) => m[1]);
      if (id && doms.length) rows.push({ id, doms });
    }
    if (!rows.length) continue;
    checked++;
    const owner = new Map();
    for (const { id, doms } of rows) {
      for (const d of doms) {
        assert.ok(!d.endsWith('iocoder.cn'), `${f} 租户 ${id} 的 websites 仍含上游域名: ${d}`);
        const prev = owner.get(d);
        assert.equal(prev, undefined,
          `${f} 域名 ${d} 同时属于租户 ${prev} 与 ${id}，会使 getTenantByWebsite 结果不确定`);
        owner.set(d, id);
      }
    }
  }
  assert.ok(checked >= 8, `受检 SQL 种子文件数异常偏少: ${checked}`);
});
