import test from 'node:test';
import assert from 'node:assert/strict';
import { checkDocs } from './verify-docs.mjs';

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
