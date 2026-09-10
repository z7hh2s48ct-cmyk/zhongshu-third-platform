import test from 'node:test';
import assert from 'node:assert/strict';
import { checkDocs } from './verify-docs.mjs';
import { backfillSection2 } from './task-stats.mjs';

const root = '/repo';
const read = (files) => (f) => {
  if (!(f in files)) throw new Error('missing ' + f);
  return files[f];
};
const inFiles = (files) => ({ exists: (p) => files.some((f) => p.split('\\').join('/').endsWith(f)) });

test('正常文档通过（链接/编号/状态/决策/版本均一致）', () => {
  const files = ['README.md', 'docs/05-x.md'];
  const issues = checkDocs(
    files,
    read({
      'README.md': '| [任务清单](docs/05-x.md) | V1.5 | 索引 |\n',
      'docs/05-x.md': '> 文档版本：V1.5\n\n### ZS-ENG-001：示例\n\n- 关联：WP-02；B01。类别 改造；状态 待验收；前置 B00。\n',
    }),
    root,
    inFiles(files),
  );
  assert.deepEqual(issues, []);
});

test('故意破坏链接会失败', () => {
  const issues = checkDocs(
    ['README.md'],
    read({ 'README.md': '[断链](docs/不存在.md)\n' }),
    root,
  );
  assert.ok(issues.some((i) => i.rule === 'R1-link'));
});

test('任务编号重复会失败', () => {
  const doc = '> 文档版本：V1.5\n\n### ZS-ENG-001：A\n\n### ZS-ENG-001：B\n';
  const issues = checkDocs(['docs/05-x.md'], read({ 'docs/05-x.md': doc }), root);
  assert.ok(issues.some((i) => i.rule === 'R2-dup-id'));
});

test('非法任务状态会失败，合法枚举通过', () => {
  const bad = '- 关联：WP-02；状态 已完成；前置 B00。';
  const issues = checkDocs(['docs/05-x.md'], read({ 'docs/05-x.md': bad }), root);
  assert.ok(issues.some((i) => i.rule === 'R3-status'));
  const good = '- 关联：WP-02；状态 暂缓；前置 B00。';
  assert.deepEqual(checkDocs(['docs/05-x.md'], read({ 'docs/05-x.md': good }), root), []);
});

test('未确认决策（D-10）被写成既成事实会失败，否定表述通过', () => {
  const bad = '按 D-10 的已确认结论接入真实微信。';
  const issues = checkDocs(['docs/05-x.md'], read({ 'docs/05-x.md': bad }), root);
  assert.ok(issues.some((i) => i.rule === 'R4-decision'));
  const good = 'D-10 的候选接入方案，不是已确认结论。';
  assert.deepEqual(checkDocs(['docs/05-x.md'], read({ 'docs/05-x.md': good }), root), []);
});

test('README 索引版本与文档头不一致会失败', () => {
  const issues = checkDocs(
    ['README.md', 'docs/06-x.md'],
    read({
      'README.md': '| [映射](docs/06-x.md) | V9.9 | 索引 |\n',
      'docs/06-x.md': '> 文档版本：V1.1\n',
    }),
    root,
  );
  assert.ok(issues.some((i) => i.rule === 'R5-version'));
});

test('仓库外引用与外链跳过，不误报', () => {
  const issues = checkDocs(
    ['README.md'],
    read({ 'README.md': '[外链](https://example.com/a) [绝对路径](/Users/x/a.md)\n' }),
    root,
  );
  assert.deepEqual(issues, []);
});

test('R6：§2 声明统计与卡片实际聚合不一致会失败', () => {
  const doc = [
    '## 2. 进度',
    'V1.5 统计（2026-09-10，重新计数）：2 项待开发、0 项待验收。',
    '### ZS-ENG-001：A',
    '- 关联：WP；状态 待开发；前置 无。',
    '### ZS-ENG-002：B',
    '- 关联：WP；状态 待验收；前置 无。',
  ].join('\n');
  // 实际：待开发1、待验收1；声明：待开发2、待验收0 → 不一致
  const issues = checkDocs(['docs/05-x.md'], read({ 'docs/05-x.md': doc }), root);
  assert.ok(issues.some((i) => i.rule === 'R6-count'));
});

test('R6：§2 声明与卡片实际聚合一致时不报 R6', () => {
  const doc = [
    '## 2. 进度',
    'V1.5 统计（2026-09-10）：1 项待开发、1 项待验收。',
    '### ZS-ENG-001：A',
    '- 关联：WP；状态 待开发；前置 无。',
    '### ZS-ENG-002：B',
    '- 关联：WP；状态 待验收；前置 无。',
  ].join('\n');
  const issues = checkDocs(['docs/05-x.md'], read({ 'docs/05-x.md': doc }), root);
  assert.ok(!issues.some((i) => i.rule === 'R6-count'));
});

test('R7：README 声明分布与 docs/05 实际不一致会失败', () => {
  const files = ['README.md', 'docs/05-x.md'];
  const issues = checkDocs(
    files,
    read({
      'README.md': '累计 91 项主任务（83 待开发、2 待决策、6 待前置）\n',
      'docs/05-x.md': '### ZS-ENG-001：A\n- 关联：WP；状态 待验收；前置 无。\n',
    }),
    root,
    inFiles(files),
  );
  assert.ok(issues.some((i) => i.rule === 'R7-readme-sync'));
});

test('R6/R7：无声明句时跳过，不误报', () => {
  const files = ['README.md', 'docs/05-x.md'];
  const issues = checkDocs(
    files,
    read({
      'README.md': '| [清单](docs/05-x.md) | V1.5 | 索引 |\n',
      'docs/05-x.md': '> 文档版本：V1.5\n### ZS-ENG-001：A\n- 关联：WP；状态 待验收；前置 无。\n',
    }),
    root,
    inFiles(files),
  );
  assert.deepEqual(issues, []);
});

// ---- codex P2#4 回归：声明省略非零类别不得逃过校验 ----

test('R6：§2 省略实际非零的类别（漏计）会失败', () => {
  // 声明只写「1 项待开发」（对），却省略了实际存在的「待验收 1」；
  // 旧逻辑只遍历声明键 → 漏检；修复后遍历全枚举、省略类别按 0 计 → 0≠1 触发 R6。
  const doc = [
    '## 2. 进度',
    'V1.5 统计（2026-09-10）：1 项待开发。',
    '### ZS-ENG-001：A',
    '- 关联：WP；状态 待开发；前置 无。',
    '### ZS-ENG-002：B',
    '- 关联：WP；状态 待验收；前置 无。',
  ].join('\n');
  const issues = checkDocs(['docs/05-x.md'], read({ 'docs/05-x.md': doc }), root);
  assert.ok(issues.some((i) => i.rule === 'R6-count' && /待验收/.test(i.message)), '省略非零类别「待验收」应触发 R6');
});

test('R7：README 省略实际非零的类别（漏计）会失败', () => {
  // total=2 对、待开发=1 对，但省略了实际存在的「待验收 1」；修复后应触发 R7。
  const files = ['README.md', 'docs/05-x.md'];
  const issues = checkDocs(
    files,
    read({
      'README.md': '累计 2 项主任务（1 待开发）\n',
      'docs/05-x.md': '### ZS-ENG-001：A\n- 关联：WP；状态 待开发；前置 无。\n### ZS-ENG-002：B\n- 关联：WP；状态 待验收；前置 无。\n',
    }),
    root,
    inFiles(files),
  );
  assert.ok(issues.some((i) => i.rule === 'R7-readme-sync' && /待验收/.test(i.message)), '省略非零类别「待验收」应触发 R7');
});

// ---- codex P2#5 回归：backfillSection2（§2 回填纯函数）——改数字 + 补插缺失非零类别 ----

test('backfillSection2：替换句中已有类别的数字，保留其后说明', () => {
  const text = 'V1.17 统计（2026-09-10）：43 项待开发、6 项待前置；0 项已验收（待验收=ENG）。';
  const out = backfillSection2(text, { 待开发: 42, 待决策: 0, 待前置: 6, 开发中: 0, 待验收: 0, 已验收: 0, 暂缓: 0 });
  assert.match(out, /42 项待开发/, '待开发数字应回填为实际值');
  assert.match(out, /6 项待前置/, '未变类别保持');
  assert.match(out, /（待验收=ENG）/, '分布串之后的说明必须保留');
});

test('backfillSection2：补插句中缺失但实际非零的类别（P2#5 漏计修复）', () => {
  // 原句无「暂缓」；某卡转暂缓后 counts.暂缓=1 → 必须补插，否则 §2 分布漏计（合计 90≠91）。
  const text = 'V1.17 统计（2026-09-10）：42 项待开发、11 项开发中、32 项待验收、0 项待决策、6 项待前置；0 项已验收（说明）。';
  const counts = { 待开发: 42, 开发中: 11, 待验收: 31, 待决策: 0, 待前置: 6, 已验收: 0, 暂缓: 1 };
  const out = backfillSection2(text, counts);
  assert.match(out, /1 项暂缓/, '缺失的非零类别「暂缓」应被补插');
  assert.match(out, /31 项待验收/, '待验收应从 32 回填为 31');
  // 补插后分布合计应等于卡片总数 91（42+11+31+0+6+0+1）
  const sum = [...out.matchAll(/(\d+)\s*项\s*(?:待开发|开发中|待验收|待决策|待前置|已验收|暂缓)/g)]
    .reduce((a, m) => a + Number(m[1]), 0);
  assert.equal(sum, 91, '补插后 §2 分布合计应等于卡片总数（消除漏计）');
});

test('backfillSection2：幂等——已含全部非零类别时二次回填不重复补插', () => {
  const counts = { 待开发: 42, 开发中: 11, 待验收: 31, 待决策: 0, 待前置: 6, 已验收: 0, 暂缓: 1 };
  const base = 'V1.17 统计（2026-09-10）：42 项待开发、11 项开发中、32 项待验收、0 项待决策、6 项待前置；0 项已验收（说明）。';
  const once = backfillSection2(base, counts);
  const twice = backfillSection2(once, counts);
  assert.equal(once, twice, '二次回填应幂等');
  assert.match(once, /1 项暂缓（说明）/, '暂缓补插在分布串末尾、说明之前');
});

test('backfillSection2：无 §2 声明句时原样返回', () => {
  const text = '## 2. 进度\n本版本暂无统计句。\n';
  assert.equal(backfillSection2(text, { 待开发: 1 }), text);
});
