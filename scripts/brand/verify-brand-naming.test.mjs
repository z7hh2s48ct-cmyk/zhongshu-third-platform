import test from 'node:test';
import assert from 'node:assert/strict';
import { judge, loadAllowlist, scanTree, BINARY } from './verify-brand-naming.mjs';

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
  // hotfix-B P2-8: 署名例外收紧为上下文限定模式，仅放行 @author/注释/元数据等归属上下文
  const ok = judge('services/x/DemoService.java', '// 芋道源码：① 移除暂时用不到的 websocket', entries, now);
  assert.equal(ok.violations.length, 0);
  const author = judge('services/x/DemoService.java', ' * @author 芋道源码', entries, now);
  assert.equal(author.violations.length, 0);
  // 非署名上下文的 芋道源码 应失败（hotfix-B 收紧后不再被 standalone 模式放行）
  const fail = judge('apps/x/src/Login.vue', "tenantName: '芋道源码',", entries, now);
  assert.ok(fail.violations.length > 0, '非署名上下文的芋道源码应失败');
  const title = judge('apps/x/src/Login.vue', "title: '芋道源码管理平台'", entries, now);
  assert.ok(title.violations.length > 0, '产品可见旧标题应失败');
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

test('hotfix-B：yd-* 限期兼容例外受 path 限定，不溢出到小程序/codegen 之外', () => {
  // 管理端文件名含 yd- 前缀必须失败（codex P2-9 复现路径）
  const p = 'apps/zhongshu-admin-web/src/yd-foo.ts';
  assert.ok(judge(p, p, entries, now).violations.length > 0, '管理端 yd- 文件名应失败');
  // 后端其他模块的 yd- 内容必须失败
  const other = judge('services/zhongshu-core/zszj-module-system/src/main/resources/a.txt',
    '<view class="yd-page-container" />', entries, now);
  assert.ok(other.violations.length > 0, '非登记范围的 yd- 内容应失败');
  // 小程序本体与 codegen uniapp 同批夹具在限期内仍放行
  assert.equal(judge('apps/zhongshu-miniapp/src/components/a.vue', '<yd-form-picker />', entries, now).violations.length, 0);
  const fixturePath = 'services/zhongshu-core/zszj-module-infra/src/test/resources/codegen/vue3_admin_uniapp_master_erp/vue/a.vue';
  assert.equal(judge(fixturePath, '<view class="yd-page-container" />', entries, now).violations.length, 0);
  // 同一路径内多次命中须逐一被覆盖判定放行（曾因 content 正则缺 g 标志只匹配首个而误报）
  const multi = 'apps/zhongshu-miniapp/src/components/zszj-ui/yd-form-picker/yd-form-picker.vue';
  assert.equal(judge(multi, multi, entries, now).violations.length, 0, '同一路径内多次 yd- 命中应全部放行');
});

test('hotfix-B：SVG 不再被当作二进制跳过，其余二进制扩展名仍跳过', () => {
  assert.ok(!BINARY.test('apps/zhongshu-admin-web/src/assets/logo.svg'), 'svg 必须进入文本扫描');
  for (const f of ['a.png', 'a.jpg', 'a.ico', 'a.woff2', 'a.jar', 'a.zip']) {
    assert.ok(BINARY.test(f), `${f} 仍应作为二进制跳过`);
  }
  // SVG 内的产品可见品牌串必须被判为违规（原先整文件被跳过）
  const r = judge('apps/zhongshu-admin-web/src/assets/logo.svg',
    '<svg xmlns="http://www.w3.org/2000/svg"><text>芋道管理系统</text></svg>', entries, now);
  assert.ok(r.violations.length > 0, 'SVG 文本内旧品牌串应失败');
});

test('hotfix-B：例外匹配区间必须覆盖命中点，邻近白名单串不得庇护', () => {
  // P2-4：注释内的上游文档域名不得庇护紧随其后的旧产品标题
  const shielded = judge('apps/zhongshu-admin-web/src/a.html',
    '<!-- doc.iocoder.cn -->\n<title>芋道管理系统</title>', entries, now);
  assert.ok(shielded.violations.some((v) => v.pattern === '芋道'), '旧标题应失败，不得被邻近白名单串庇护');
  // 同一文件内的上游域名本身仍应放行
  assert.ok(!shielded.violations.some((v) => v.pattern === 'cn.iocoder'), '上游文档域名应放行');
});

test('hotfix-B：署名例外不得被 URL 斜杠与引号字符串反向利用', () => {
  // https:// 内的 // 不得被当作行注释起始而庇护同屏产品标题
  const html = judge('apps/zhongshu-admin-web/src/a.html',
    '<link href="https://cdn.example.com/x.css">\n<title>芋道源码管理平台</title>', entries, now);
  assert.ok(html.violations.some((v) => v.pattern === '芋道'), 'URL 斜杠不得庇护产品标题');
  // JSON/JS 字符串字面量中的产品文案不得被引号模式庇护
  const json = judge('apps/zhongshu-admin-web/src/a.json', '{"siteTitle": "芋道源码管理平台"}', entries, now);
  assert.ok(json.violations.some((v) => v.pattern === '芋道'), '引号字符串不得庇护产品文案');
  // 真正的行注释署名仍放行
  const comment = judge('services/x/A.java', '// 芋道源码：① 移除暂时用不到的 websocket', entries, now);
  assert.equal(comment.violations.length, 0);
});

test('hotfix-B r1：codex 评审探针样本——三类庇护路径已闭合', () => {
  // 尾随通配过贪：URL 后的 "> 不得被并入匹配区间而庇护紧随的产品标题
  const a = judge('apps/zhongshu-admin-web/src/a.html',
    '<a href="https://doc.iocoder.cn">芋道管理系统</a>', entries, now);
  assert.ok(a.violations.some((v) => v.pattern === '芋道'), 'URL 尾随通配不得跨引号庇护产品标题');
  // setter 形态限定 src/test：应用侧 setTitle(...) 不得被测试夹具模式庇护
  const b = judge('apps/zhongshu-admin-web/src/a.html', 'setTitle("芋道源码管理平台");', entries, now);
  assert.ok(b.violations.some((v) => v.pattern === '芋道'), '应用侧 set* 调用不得被测试夹具模式庇护');
  // package.json 裸 token：name 等产品元数据字段不得被 provenance 例外庇护
  const c = judge('apps/zhongshu-miniapp/package.json', '{\n  "name": "unibest"\n}', entries, now);
  assert.ok(c.violations.some((v) => v.pattern === 'unibest'), 'package.json name 字段不得被 provenance 例外庇护');
  // 真实 provenance 形态仍放行
  const d = judge('apps/zhongshu-miniapp/package.json',
    '{\n  "unibest-version": "4.1.0",\n  "repository": "https://github.com/feige996/unibest"\n}', entries, now);
  assert.equal(d.violations.length, 0, 'provenance 字段与仓库地址应放行');
  // 真实测试夹具形态仍放行
  const e = judge('services/zhongshu-core/zszj-module-bpm/src/test/java/A.java',
    'group.setName("芋道源码");', entries, now);
  assert.equal(e.violations.length, 0, 'src/test 下的 setter 夹具应放行');
});

test('hotfix-B r2：字符集不得跨行终止符与 JSON 转义（codex r1 两项 P2 回归）', () => {
  // P2-a：.* 不含换行，而显式字符集 [^)] 含——无关赋值不得跳行豁免后续产品代码
  const ex = judge('apps/zhongshu-admin-web/src/a.ts',
    'const example = 1;\nconst title = "芋道源码管理平台";', entries, now);
  assert.ok(ex.violations.some((v) => v.pattern === '芋道'), 'example 赋值不得跨 \n 豁免下一行产品标题');
  const exCrlf = judge('apps/zhongshu-admin-web/src/a.ts',
    'const example = 1;\r\nconst title = "芋道源码管理平台";', entries, now);
  assert.ok(exCrlf.violations.some((v) => v.pattern === '芋道'), 'CRLF 文件同样不得跨行豁免');
  // P2-a 同根因：注释模式不得跨行豁免反引号包裹的产品标题
  const cmt = judge('apps/zhongshu-admin-web/src/a.ts',
    '// 上游模板说明\nconst pageTitle = `芋道源码管理平台`;', entries, now);
  assert.ok(cmt.violations.some((v) => v.pattern === '芋道'), '行注释不得跨 \n 豁免后续反引号标题');
  // P2-b：\s 不含反斜杠——JSON 转义序列 \n 是两个非空白字符，URL 匹配必须止于转义
  const pkg = judge('apps/zhongshu-miniapp/package.json',
    '{\n  "description": "https://unibest.tech\\n芋道管理系统"\n}', entries, now);
  assert.ok(pkg.violations.some((v) => v.pattern === '芋道'), 'JSON 转义后的产品标题不得被上游 URL 遮蔽');
  assert.ok(!pkg.violations.some((v) => v.pattern === 'unibest'), 'URL 本体仍应放行');
  // 正向对照：同行真实形态仍全部放行
  assert.equal(judge('services/x/A.java',
    '@Schema(description = "昵称", example = "芋道源码")', entries, now).violations.length, 0,
    '同行 example 署名应放行');
  assert.equal(judge('services/x/A.java',
    '// 芋道源码 修复了 https://doc.iocoder.cn/x 的问题\nString title = "众墅之家";', entries, now).violations.length, 0,
    '同行注释署名应放行，且不影响下一行');
  assert.equal(judge('apps/zhongshu-admin-web/src/a.md',
    '见 https://github.com/yudaocode/ruoyi-vue-pro\n标题：众墅之家', entries, now).violations.length, 0,
    '上游仓库地址应放行，换行后的自有文案不产生命中');
});

test('hotfix-B r2：不变式——任何 content 例外均不得跨行终止符豁免后续品牌串', () => {
  // 已证实的三个跨行豁免漏洞（\s 包含 \n\r，故无关前缀可跳行庇护下一行产品文案）：
  //   @author\n芋道源码…   ---\n芋道源码…   foo()\n芋道源码…
  // 注：括号用例必须使 ) 紧邻换行（foo(); 因中间有 ; 本来就不会被庇护，属无效用例）
  const EXPLOITS = [
    ['@author 尾随换行', '@author \n芋道源码管理平台'],
    ['markdown 分隔线后换行', '---\n芋道源码管理平台'],
    ['右括号紧邻换行', 'foo()\n芋道源码管理平台'],
    ['链接右括号后换行', '[文档](https://x.com)\n芋道源码管理平台'],
  ];
  for (const [name, text] of EXPLOITS) {
    const r = judge('apps/zhongshu-admin-web/src/a.md', text, entries, now);
    assert.ok(r.violations.some((v) => v.pattern === '芋道'),
      `${name}不得跨行豁免下一行产品文案: ${JSON.stringify(text)}`);
  }
  // 系统化不变式：对全部 content 模式做「无关前缀 + 换行 + 旧品牌串」穷举探针，
  // 若匹配区间同时跨过换行且吞掉尾部品牌串，则仍存在跨行豁免风险
  const HEADS = ['', '// x', 'example = ', 'example = "', ') ', '- ', '@author ', '@author', '"author"',
    'regex ', '芋道源码', 'name=', ' Created by ', 'foo();', 'x)', '---', '[link](https://x.com)', '*', '  ', '\t',
    'https://doc.iocoder.cn', 'https://unibest.tech', 'github.com/yudaocode/', 'static.iocoder.cn/', 'doc.yudao'];
  const TAILS = ['芋道源码', '芋道管理系统', '芋道', 'unibest', 'yudao', 'Yudao', 'cn.iocoder', 'youdao'];
  for (const e of entries) {
    for (const c of e.content ?? []) {
      const re = new RegExp(c);
      for (const head of HEADS) {
        for (const t of TAILS) {
          for (const nl of ['\n', '\r\n']) {
            const s = head + nl + t;
            re.lastIndex = 0;
            const m = re.exec(s);
            const crosses = !!(m && /[\r\n]/.test(m[0]) && m[0].includes(t) && m.index < head.length);
            assert.ok(!crosses,
              `content 模式跨行豁免: ${c} 对 ${JSON.stringify(s)} 匹配到 ${JSON.stringify(m && m[0])}`);
          }
        }
      }
    }
  }
});

test('hotfix-B：白名单结构自检——content 例外必须带模式，避免静默失效', () => {
  // P1-3 修复后，scope=content 但无 content 模式的条目不再放行任何命中
  // （原 ^docs/0[1-6]- 条目即依赖 pathPass 缺陷生效），此处固化该不变式
  for (const e of entries) {
    if (e.scope === 'content') {
      assert.ok(Array.isArray(e.content) && e.content.length > 0,
        `scope=content 条目必须提供 content 模式: ${e.path ?? e.reason}`);
    }
    if (e.path) assert.doesNotThrow(() => new RegExp(e.path), `path 正则非法: ${e.path}`);
    for (const c of e.content ?? []) assert.doesNotThrow(() => new RegExp(c), `content 正则非法: ${c}`);
  }
});
