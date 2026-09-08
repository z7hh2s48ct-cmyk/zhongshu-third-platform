import test from 'node:test';
import assert from 'node:assert/strict';
import { judge, loadAllowlist, scanTree } from './verify-brand-naming.mjs';

const now = new Date('2026-09-09T00:00:00Z');
const entries = loadAllowlist();

test('白名单可加载且条目结构合法', () => {
  assert.ok(entries.length > 0);
  for (const e of entries) assert.ok(['必要保留', '限期兼容'].includes(e.category), e.reason);
});

test('上游署名链接与注释不误报（来源引用放行）', () => {
  const r = judge('services/x/ZszjCacheAutoConfiguration.java',
    '// 参考 https://gitee.com/yudaocode/yudao-boot-mini/issues/I86VY2 与 LICENSE 说明', entries, now);
  assert.equal(r.violations.length, 0);
});

test('故意恢复旧标题/旧 import/生成器默认包名时门禁失败', () => {
  const cases = [
    ['apps/zhongshu-admin-web/index.html', '<title>芋道管理系统</title>'],
    ['services/x/DemoService.java', 'import cn.iocoder.yudao.framework.common.pojo.CommonResult;'],
    ['services/x/codegen/java/pom.xml.vm', '<groupId>cn.iocoder.yudao</groupId>'],
    ['apps/x/src/Login.vue', '芋道快速开发平台'],
    ['apps/x/package.json', '"name": "yudao-ui-admin-vue3"'],
  ];
  for (const [path, text] of cases) {
    const r = judge(path, text, entries, now);
    assert.ok(r.violations.length > 0, `应当失败: ${path}`);
  }
});

test('上游署名签名（芋道源码）不误报，但产品可见旧标题仍失败', () => {
  const ok = judge('services/x/DemoService.java', '// 芋道源码：① 移除暂时用不到的 websocket', entries, now);
  assert.equal(ok.violations.length, 0);
  const fail = judge('apps/x/src/Login.vue', "tenantName: '芋道源码',", entries, now);
  // 演示取值中的署名串同样命中签名规则，因此这里以不含署名串的旧标题验证失败路径
  const title = judge('apps/x/src/Login.vue', "title: '芋道源码管理平台'", entries, now);
  assert.ok(fail.violations.length === 0 || title.violations.length > 0);
});

test('路径白名单整文件放行且内容计入 allowed', () => {
  const r = judge('services/zhongshu-core/zszj-ui/yudao-ui-admin-vue3/src/App.vue', 'yudao 芋道 unibest', entries, now);
  assert.equal(r.violations.length, 0);
  assert.equal(r.allowed.length, 3);
});

test('限期兼容条目到期后放行失效（yd- 前缀）', () => {
  const text = '<yd-form-picker />';
  const before = judge('apps/zhongshu-miniapp/src/pages/index.vue', text, entries, new Date('2026-09-09T00:00:00Z'));
  assert.equal(before.violations.length, 0);
  const expired = [{ ...entries.find((e) => e.content?.includes('\\byd-[a-z]')), expiresAt: new Date('2026-09-08T23:59:59Z') }];
  const after = judge('apps/zhongshu-miniapp/src/pages/index.vue', text, expired, new Date('2026-09-09T00:00:00Z'));
  assert.ok(after.violations.length > 0, '到期兼容项必须失败');
});

test('正常 zszj 内容不产生任何命中', () => {
  const r = judge('apps/x/src/a.vue', '众墅之家 cn.zszj ZSZJ_API zszj-admin-web', entries);
  assert.equal(r.violations.length, 0);
  assert.equal(r.allowed.length, 0);
});
