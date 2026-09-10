codex.cmd : OpenAI Codex v0.153.4
所在位置 行:1 字符: 146
+ ... ing]::UTF8; & "$env:APPDATA\npm\codex.cmd" review --commit 8c9b6082 2 ...
+                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    + CategoryInfo          : NotSpecified: (OpenAI Codex v0.153.4:String) [], 
    RemoteException
    + FullyQualifiedErrorId : NativeCommandError
 
--------
workdir: E:\众墅之家AI赋能平台底座
model: gpt-6-astra
provider: openai
approval: never
sandbox: workspace-write [workdir, /tmp, $TMPDIR]
reasoning effort: xhigh
reasoning summaries: none
session id: 01a08acf-fbc7-7b90-a18c-3f111c5fdd63
--------
user
commit 8c9b608
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'git 
status --short; git show --stat --oneline --decorate --no-renames 8c9b6082; Get
-ChildItem -Force -Name' in E:\众墅之家AI赋能平台底座
 succeeded in 3ms:
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "git 
show --format=fuller --no-ext-diff --no-renames 8c9b6082; Get-ChildItem -Path .
 -Filter 'AGENTS*' -Recurse -Force -ErrorAction SilentlyContinue | Select-Objec
t -ExpandProperty FullName; Get-ChildItem -Path .codex,.agents -Force -ErrorAct
ion SilentlyContinue; Get-ChildItem -Path E:\\ -Filter 'AGENTS*' -Force -ErrorA
ction SilentlyContinue" in E:\众墅之家AI赋能平台底座
 succeeded in 3508ms:
E:\众墅之家AI赋能平台底座\apps\zhongshu-admin-web\node_modules\.pnpm\caniuse-lite@1.0.300
01775\node_modules\caniuse-lite\data\agents.js
E:\众墅之家AI赋能平台底座\apps\zhongshu-admin-web\node_modules\.pnpm\caniuse-lite@1.0.300
01775\node_modules\caniuse-lite\dist\unpacker\agents.js
E:\众墅之家AI赋能平台底座\apps\zhongshu-admin-web\node_modules\.pnpm\caniuse-lite@1.0.300
01791\node_modules\caniuse-lite\data\agents.js
E:\众墅之家AI赋能平台底座\apps\zhongshu-admin-web\node_modules\.pnpm\caniuse-lite@1.0.300
01791\node_modules\caniuse-lite\dist\unpacker\agents.js
E:\众墅之家AI赋能平台底座\apps\zhongshu-miniapp\node_modules\.pnpm\caniuse-lite@1.0.30001
739\node_modules\caniuse-lite\data\agents.js
E:\众墅之家AI赋能平台底座\apps\zhongshu-miniapp\node_modules\.pnpm\caniuse-lite@1.0.30001
739\node_modules\caniuse-lite\dist\unpacker\agents.js
E:\众墅之家AI赋能平台底座\apps\zhongshu-miniapp\node_modules\.pnpm\caniuse-lite@1.0.30001
749\node_modules\caniuse-lite\data\agents.js
E:\众墅之家AI赋能平台底座\apps\zhongshu-miniapp\node_modules\.pnpm\caniuse-lite@1.0.30001
749\node_modules\caniuse-lite\dist\unpacker\agents.js

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "git 
rev-parse HEAD; Get-Content scripts/gov/verify-docs.mjs; Get-Content scripts/go
v/verify-docs.test.mjs; Get-Content docs/05-底座模块分析与开发任务清单.md -TotalCount 130; G
et-ChildItem scripts/gov -Name; if (Test-Path -LiteralPath 'C:/Users/Administra
tor/.codex/config.toml') { Get-Content -LiteralPath 'C:/Users/Administrator/.co
dex/config.toml' | Select-String 'project_doc|fallback' }" in E:\众墅之家AI赋能平台底座
 succeeded in 0ms:
c9d178b2cfaa39bc972bdbbeb5107a4e66d0e53b
/**
 * ZS-GOV-001 鏂囨。涓€鑷存€ф鏌ュ櫒锛堟湰鍦颁笌 CI 鍚屼竴鍏ュ彛锛夈€? *
 * 鏍￠獙鑼冨洿锛歊EADME.md 涓?docs/0*.md锛堝巻鍙叉姤鍛?docs/04 鍙煡閾炬帴锛屼笉鏌ュ唴瀹瑰彛寰勶級銆? * 瑙勫垯锛? * 
 R1 閾炬帴鏈夋晥锛氬唴鑱旈摼鎺ャ€佸紩鐢ㄥ畾涔夌殑鏈湴鐩爣蹇呴』瀛樺湪锛堝閾?閿氱偣璺宠繃锛夛紱
 *  R2 浠诲姟缂栧彿鍞竴锛?5 鏂囨。 `### ZS-XXX-NNN` 鏍囬涓嶅緱閲嶅锛? *  R3 鐘舵€佹灇涓惧悎娉曪細05 鍗＄墖"鐘舵
€?X"鍙厑璁?7 涓灇涓惧€硷紱
 *  R4 鍐崇瓥闂ㄧ锛氭湭纭鍐崇瓥锛圖-07/D-10/D-11锛変笉寰楄鍐欐垚宸叉壒鍑?宸茬‘璁?宸茶惤鍦帮紱D-09 宸蹭簬 2026-09-
10 纭鏈€灏忔ā鍨嬩笌璐﹀彿鍞竴鎬х粏鍒欙紝绉诲嚭瀹堟姢鍒楄〃锛? *  R5 鐗堟湰涓€鑷达細README 鏂囨。绱㈠紩鐨勭増鏈彿涓庡悇鏂囨。澶撮
儴"鏂囨。鐗堟湰锛歏*"涓€鑷淬€? *  R6 缁熻涓€鑷达細05 搂2 澹版槑鐨勭姸鎬佸垎甯冨繀椤荤瓑浜庡崱鐗囧疄闄呰仛鍚堬紙瑙?task-stats.
mjs锛夛紱鏃犲０鏄庡彞鍒欒烦杩囷紱
 *  R7 README 鎽樿涓€鑷达細README"绱 N 椤逛富浠诲姟锛堚€︼級"鐨勬€绘暟涓庡悇鐘舵€佹暟蹇呴』绛変簬 05 鍗＄墖瀹為檯鑱氬悎
锛涙棤澹版槑鍙ュ垯璺宠繃銆? * 鐢ㄦ硶锛歯ode scripts/gov/verify-docs.mjs锛堥€€鍑虹爜闈?0 = 涓嶄竴鑷达級
 */
import { readFileSync, existsSync, statSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join, resolve } from 'node:path';
import { countStatus, parseSection2Declared, parseReadmeDeclared } from './task
-stats.mjs';

const root = fileURLToPath(new URL('../../', import.meta.url));
const DOCS = ['README.md',
  'docs/01-搴曞骇浠ｇ爜澶嶇敤涓庢敼閫犳柟妗?md',
  'docs/02-涓€鏈熷簳搴ч渶姹傝鏍间笌寰呭喅绛栧彴璐?md',
  'docs/03-搴曞骇浜屾寮€鍙戦『搴忎笌楠屾敹鏍囧噯.md',
  'docs/05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md',
  'docs/06-鍝佺墝绱犳潗涓庡懡鍚嶆槧灏?md'];
const HISTORICAL = /^docs\/04-/; // 鍘嗗彶鎶ュ憡锛氬彧鏌ラ摼鎺ワ紝涓嶉噸鍐欏彛寰?const STATUS_ENUM = 
['寰呭紑鍙?, '寰呭喅绛?, '寰呭墠缃?, '寮€鍙戜腑', '寰呴獙鏀?, '宸查獙鏀?, '鏆傜紦'];
const UNCONFIRMED_DECISIONS = ['D-07', 'D-10', 'D-11']; // D-09 宸蹭簬 2026-09-10 
纭锛岀Щ鍑哄畧鎶ゅ垪琛?const VERSION_HEADER = /鏂囨。鐗堟湰锛歕s*([A-Za-z0-9.]+)/;

export function checkDocs(files, readFile, rootDir, { exists = existsSync } = {
}) {
  const issues = [];
  const contents = new Map();
  for (const f of files) {
    try {
      contents.set(f, readFile(f));
    } catch {
      issues.push({ rule: 'R0-read', file: f, message: '鏂囦欢鏃犳硶璇诲彇' });
    }
  }

  // R1 閾炬帴鏈夋晥锛堟牎楠岃寖鍥达細浠撳簱鍐呯洰鏍囷紱浠撳簱澶栧紩鐢ㄥ睘璺ㄥ伐浣滃尯寮曠敤锛屾墦鍗版彁绀轰笉璁″け璐ワ級
  for (const f of files) {
    const text = contents.get(f) ?? '';
    const baseDir = dirname(resolve(rootDir, f));
    const refs = [];
    for (const m of text.matchAll(/\]\(([^)\s]+)\)/g)) refs.push(m[1]);
    for (const m of text.matchAll(/^\[[^\]]+\]:\s+(\S+)$/gm)) refs.push(m[1]);
    for (const target of refs) {
      if (/^(https?:|mailto:|#)/.test(target)) continue;
      const clean = target.split('#')[0];
      if (!clean) continue;
      // 缁濆璺緞 / 灏栨嫭鍙峰寘瑁圭洰鏍囷細浣滆€呯幆澧冪殑浠撳簱澶栧紩鐢紝涓嶆牎楠?      if (/^<.+>$/.test(cle
an) || /^[A-Za-z]:[\\/]/.test(clean) || clean.startsWith('/')) {
        console.error(`[info] ${f}: 浠撳簱澶?缁濆璺緞寮曠敤锛堜笉鏍￠獙锛? ${target}`);
        continue;
      }
      const abs = resolve(baseDir, decodeURI(clean));
      if (!exists(abs)) {
        if (!abs.startsWith(resolve(rootDir))) {
          console.error(`[info] ${f}: 浠撳簱澶栧紩鐢紙涓嶆牎楠岋級: ${target}`);
        } else {
          issues.push({ rule: 'R1-link', file: f, message: `閾炬帴鐩爣涓嶅瓨鍦? ${targe
t}` });
        }
      }
    }
  }

  // R2 浠诲姟缂栧彿鍞竴锛堜粎 05锛?  for (const f of files) {
    if (!f.includes('05-')) continue;
    const ids = [...(contents.get(f) ?? '').matchAll(/^### (ZS-[A-Z]+-\d{3})/gm
)].map((m) => m[1]);
    const seen = new Set();
    for (const id of ids) {
      if (seen.has(id)) issues.push({ rule: 'R2-dup-id', file: f, message: `浠诲姟
缂栧彿閲嶅: ${id}` });
      seen.add(id);
    }
  }

  // R3 鐘舵€佹灇涓撅紙浠?05锛?  for (const f of files) {
    if (!f.includes('05-')) continue;
    for (const m of (contents.get(f) ?? '').matchAll(/^-\s*鍏宠仈[^\n]*?鐘舵€乗s*([^\
s锛?锛宂+)/gm)) {
      if (!STATUS_ENUM.includes(m[1])) {
        issues.push({ rule: 'R3-status', file: f, message: `闈炴硶浠诲姟鐘舵€? "${m[1]}
"锛堝厑璁革細${STATUS_ENUM.join('/')}锛塦 });
      }
    }
  }

  // R4 鏈‘璁ゅ喅绛栦笉寰楀啓鎴愭棦鎴愪簨瀹烇紙鍚﹀畾鍙ュ紡濡?涓嶆槸宸茬‘璁ょ粨璁?灞炲悎瑙勮〃杩帮級
  for (const f of files) {
    const text = contents.get(f) ?? '';
    for (const d of UNCONFIRMED_DECISIONS) {
      for (const m of text.matchAll(new RegExp(`${d}[^銆俓\n|]{0,12}(宸茬‘璁宸叉壒鍑唡宸查
€氳繃|宸茶惤鍦?`, 'g'))) {
        // 鍏抽敭璇嶅墠鐨勫惁瀹?绂佹鎺緸瑙嗕负鍚堣琛ㄨ堪锛堣鍒欒嚜杩版枃鏈悓鏍蜂細鍛戒腑妯″紡锛?        const befor
eKeyword = text.slice(m.index, m.index + m[0].length).slice(-8, -3);
        if (/涓嶅緱|涓嶅簲|绂佹|涓嶆槸|闈?|鏈粡|灏氭湭$|鏈?/.test(beforeKeyword)) continue;
        issues.push({ rule: 'R4-decision', file: f, message: `鏈‘璁ゅ喅绛?${d} 琚啓鎴
愭棦鎴愪簨瀹? 鈥?{m[0]}鈥 });
      }
    }
  }

  // R5 鐗堟湰涓€鑷达紙README 绱㈠紩 vs 鏂囨。澶达級
  const readme = contents.get('README.md') ?? '';
  for (const m of readme.matchAll(/\[(鏂囨。[^\]]*|[^[\]]+)\]\((docs\/0[0-9][^)]*)
\)\s*\|\s*([A-Za-z0-9.]+)/g)) {
    const [, , docPath, version] = m;
    const doc = files.find((f) => f === docPath);
    if (!doc) continue;
    if (HISTORICAL.test(docPath)) continue;
    const header = VERSION_HEADER.exec(contents.get(doc) ?? '');
    if (!header) issues.push({ rule: 'R5-version', file: docPath, message: '鏂囨。
缂哄皯"鏂囨。鐗堟湰锛?澶? });
    else if (header[1] !== version) {
      issues.push({ rule: 'R5-version', file: docPath, message: `README 绱㈠紩鐗堟湰 
${version} 涓庢枃妗ｅご ${header[1]} 涓嶄竴鑷碻 });
    }
  }

  // R6 缁熻涓€鑷达紙浠?05锛夛細搂2 澹版槑鍒嗗竷蹇呴』绛変簬鍗＄墖瀹為檯鑱氬悎锛涙棤澹版槑鍙ュ垯璺宠繃
  const doc05 = files.find((f) => f.includes('05-'));
  const actual05 = doc05 ? countStatus(contents.get(doc05) ?? '') : null;
  if (doc05) {
    const declared = parseSection2Declared(contents.get(doc05) ?? '');
    if (declared) {
      for (const s of Object.keys(declared)) {
        if (declared[s] !== actual05.counts[s]) {
          issues.push({ rule: 'R6-count', file: doc05, message: `搂2 澹版槑 ${s} ${
declared[s]} 椤癸紝瀹為檯鍗＄墖鑱氬悎 ${actual05.counts[s]} 椤筦 });
        }
      }
    }
  }

  // R7 README 鎽樿涓€鑷达細README 澹版槑鐨勬€绘暟涓庡悇鐘舵€佹暟蹇呴』绛変簬 05 鍗＄墖瀹為檯鑱氬悎锛涙棤澹版槑鍙ユ垨缂?05 
鍒欒烦杩?  const readmeFile = files.find((f) => f === 'README.md');
  if (readmeFile && actual05) {
    const rm = parseReadmeDeclared(contents.get(readmeFile) ?? '');
    if (rm) {
      if (rm.total !== actual05.cardCount) {
        issues.push({ rule: 'R7-readme-sync', file: readmeFile, message: `READM
E 绱 ${rm.total} 椤逛富浠诲姟锛屽疄闄?${actual05.cardCount} 椤筦 });
      }
      for (const s of Object.keys(rm.declared)) {
        if (rm.declared[s] !== actual05.counts[s]) {
          issues.push({ rule: 'R7-readme-sync', file: readmeFile, message: `REA
DME 澹版槑 ${s} ${rm.declared[s]} 椤癸紝瀹為檯 ${actual05.counts[s]} 椤筦 });
        }
      }
    }
  }

  return issues;
}

const invokedDirectly = process.argv[1] && process.argv[1].replaceAll('\\', '/'
).endsWith('scripts/gov/verify-docs.mjs');
if (invokedDirectly) {
  const issues = checkDocs(DOCS, (f) => readFileSync(join(root, f), 'utf8'), ro
ot);
  console.log(JSON.stringify({ files: DOCS.length, issueCount: issues.length, i
ssues }, null, 2));
  process.exitCode = issues.length ? 1 : 0;
}
import test from 'node:test';
import assert from 'node:assert/strict';
import { checkDocs } from './verify-docs.mjs';

const root = '/repo';
const read = (files) => (f) => {
  if (!(f in files)) throw new Error('missing ' + f);
  return files[f];
};
const inFiles = (files) => ({ exists: (p) => files.some((f) => p.split('\\').jo
in('/').endsWith(f)) });

test('姝ｅ父鏂囨。閫氳繃锛堥摼鎺?缂栧彿/鐘舵€?鍐崇瓥/鐗堟湰鍧囦竴鑷达級', () => {
  const files = ['README.md', 'docs/05-x.md'];
  const issues = checkDocs(
    files,
    read({
      'README.md': '| [浠诲姟娓呭崟](docs/05-x.md) | V1.5 | 绱㈠紩 |\n',
      'docs/05-x.md': '> 鏂囨。鐗堟湰锛歏1.5\n\n### ZS-ENG-001锛氱ず渚媆n\n- 鍏宠仈锛歐P-02锛汢01銆傜
被鍒?鏀归€狅紱鐘舵€?寰呴獙鏀讹紱鍓嶇疆 B00銆俓n',
    }),
    root,
    inFiles(files),
  );
  assert.deepEqual(issues, []);
});

test('鏁呮剰鐮村潖閾炬帴浼氬け璐?, () => {
  const issues = checkDocs(
    ['README.md'],
    read({ 'README.md': '[鏂摼](docs/涓嶅瓨鍦?md)\n' }),
    root,
  );
  assert.ok(issues.some((i) => i.rule === 'R1-link'));
});

test('浠诲姟缂栧彿閲嶅浼氬け璐?, () => {
  const doc = '> 鏂囨。鐗堟湰锛歏1.5\n\n### ZS-ENG-001锛欰\n\n### ZS-ENG-001锛欱\n';
  const issues = checkDocs(['docs/05-x.md'], read({ 'docs/05-x.md': doc }), roo
t);
  assert.ok(issues.some((i) => i.rule === 'R2-dup-id'));
});

test('闈炴硶浠诲姟鐘舵€佷細澶辫触锛屽悎娉曟灇涓鹃€氳繃', () => {
  const bad = '- 鍏宠仈锛歐P-02锛涚姸鎬?宸插畬鎴愶紱鍓嶇疆 B00銆?;
  const issues = checkDocs(['docs/05-x.md'], read({ 'docs/05-x.md': bad }), roo
t);
  assert.ok(issues.some((i) => i.rule === 'R3-status'));
  const good = '- 鍏宠仈锛歐P-02锛涚姸鎬?鏆傜紦锛涘墠缃?B00銆?;
  assert.deepEqual(checkDocs(['docs/05-x.md'], read({ 'docs/05-x.md': good }), 
root), []);
});

test('鏈‘璁ゅ喅绛栵紙D-10锛夎鍐欐垚鏃㈡垚浜嬪疄浼氬け璐ワ紝鍚﹀畾琛ㄨ堪閫氳繃', () => {
  const bad = '鎸?D-10 鐨勫凡纭缁撹鎺ュ叆鐪熷疄寰俊銆?;
  const issues = checkDocs(['docs/05-x.md'], read({ 'docs/05-x.md': bad }), roo
t);
  assert.ok(issues.some((i) => i.rule === 'R4-decision'));
  const good = 'D-10 鐨勫€欓€夋帴鍏ユ柟妗堬紝涓嶆槸宸茬‘璁ょ粨璁恒€?;
  assert.deepEqual(checkDocs(['docs/05-x.md'], read({ 'docs/05-x.md': good }), 
root), []);
});

test('README 绱㈠紩鐗堟湰涓庢枃妗ｅご涓嶄竴鑷翠細澶辫触', () => {
  const issues = checkDocs(
    ['README.md', 'docs/06-x.md'],
    read({
      'README.md': '| [鏄犲皠](docs/06-x.md) | V9.9 | 绱㈠紩 |\n',
      'docs/06-x.md': '> 鏂囨。鐗堟湰锛歏1.1\n',
    }),
    root,
  );
  assert.ok(issues.some((i) => i.rule === 'R5-version'));
});

test('浠撳簱澶栧紩鐢ㄤ笌澶栭摼璺宠繃锛屼笉璇姤', () => {
  const issues = checkDocs(
    ['README.md'],
    read({ 'README.md': '[澶栭摼](https://example.com/a) [缁濆璺緞](/Users/x/a.md)\n
' }),
    root,
  );
  assert.deepEqual(issues, []);
});

test('R6锛毬? 澹版槑缁熻涓庡崱鐗囧疄闄呰仛鍚堜笉涓€鑷翠細澶辫触', () => {
  const doc = [
    '## 2. 杩涘害',
    'V1.5 缁熻锛?026-09-10锛岄噸鏂拌鏁帮級锛? 椤瑰緟寮€鍙戙€? 椤瑰緟楠屾敹銆?,
    '### ZS-ENG-001锛欰',
    '- 鍏宠仈锛歐P锛涚姸鎬?寰呭紑鍙戯紱鍓嶇疆 鏃犮€?,
    '### ZS-ENG-002锛欱',
    '- 鍏宠仈锛歐P锛涚姸鎬?寰呴獙鏀讹紱鍓嶇疆 鏃犮€?,
  ].join('\n');
  // 瀹為檯锛氬緟寮€鍙?銆佸緟楠屾敹1锛涘０鏄庯細寰呭紑鍙?銆佸緟楠屾敹0 鈫?涓嶄竴鑷?  const issues = checkDocs(['do
cs/05-x.md'], read({ 'docs/05-x.md': doc }), root);
  assert.ok(issues.some((i) => i.rule === 'R6-count'));
});

test('R6锛毬? 澹版槑涓庡崱鐗囧疄闄呰仛鍚堜竴鑷存椂涓嶆姤 R6', () => {
  const doc = [
    '## 2. 杩涘害',
    'V1.5 缁熻锛?026-09-10锛夛細1 椤瑰緟寮€鍙戙€? 椤瑰緟楠屾敹銆?,
    '### ZS-ENG-001锛欰',
    '- 鍏宠仈锛歐P锛涚姸鎬?寰呭紑鍙戯紱鍓嶇疆 鏃犮€?,
    '### ZS-ENG-002锛欱',
    '- 鍏宠仈锛歐P锛涚姸鎬?寰呴獙鏀讹紱鍓嶇疆 鏃犮€?,
  ].join('\n');
  const issues = checkDocs(['docs/05-x.md'], read({ 'docs/05-x.md': doc }), roo
t);
  assert.ok(!issues.some((i) => i.rule === 'R6-count'));
});

test('R7锛歊EADME 澹版槑鍒嗗竷涓?docs/05 瀹為檯涓嶄竴鑷翠細澶辫触', () => {
  const files = ['README.md', 'docs/05-x.md'];
  const issues = checkDocs(
    files,
    read({
      'README.md': '绱 91 椤逛富浠诲姟锛?3 寰呭紑鍙戙€? 寰呭喅绛栥€? 寰呭墠缃級\n',
      'docs/05-x.md': '### ZS-ENG-001锛欰\n- 鍏宠仈锛歐P锛涚姸鎬?寰呴獙鏀讹紱鍓嶇疆 鏃犮€俓n',
    }),
    root,
    inFiles(files),
  );
  assert.ok(issues.some((i) => i.rule === 'R7-readme-sync'));
});

test('R6/R7锛氭棤澹版槑鍙ユ椂璺宠繃锛屼笉璇姤', () => {
  const files = ['README.md', 'docs/05-x.md'];
  const issues = checkDocs(
    files,
    read({
      'README.md': '| [娓呭崟](docs/05-x.md) | V1.5 | 绱㈠紩 |\n',
      'docs/05-x.md': '> 鏂囨。鐗堟湰锛歏1.5\n### ZS-ENG-001锛欰\n- 鍏宠仈锛歐P锛涚姸鎬?寰呴獙鏀讹紱鍓嶇疆 
鏃犮€俓n',
    }),
    root,
    inFiles(files),
  );
  assert.deepEqual(issues, []);
});
# 浼楀涔嬪 AI 璧嬭兘骞冲彴锛氬簳搴фā鍧楀垎鏋愪笌寮€鍙戜换鍔℃竻鍗?
> 鏂囨。鐗堟湰锛歏1.16
> 寤虹珛涓庢洿鏂版棩鏈燂細2026-09-08  
> 鐘舵€侊細妯″潡鍒嗘瀽褰㈡垚鐨勫紑鍙戝緟鍔烇紱M01 宸ョ▼楠ㄦ灦銆丮02 鏁版嵁搴撲笌鍝佺墝鍛藉悕涓撻」 ZS-BRAND-001锝?06 宸插畬鎴愰
杞紑鍙戯紙鍚湡瀹炶繍琛屼唬鐮佷笌鏁版嵁搴撹縼绉诲彉鏇达紝瑙佸悇鍗″紑鍙戣褰曪級锛屽叾浣欎换鍔″緟鎸夋壒娆″疄鏂斤紱0 椤瑰凡楠屾敹锛堥獙鏀堕』鐪熷疄鐜
鎸夋壒娆℃斁琛岋級  
> 浠ｇ爜鏍告煡鍩虹嚎锛?00e1dfb77d0b46f58a4a8993c4e4011a90a699d锛堣縼鍏ュ熀绾匡紱鍝佺墝鏀瑰悕鍓嶅熀绾胯鏍囩 br
and-rename-baseline=4ffaaf29锛? 
> 涓婁綅渚濇嵁锛歔闇€姹備笌鍐崇瓥涓诲彴璐(02-涓€鏈熷簳搴ч渶姹傝鏍间笌寰呭喅绛栧彴璐?md)銆乕浜屽紑鎵规涓庨獙鏀舵爣鍑哴(03-搴曞骇浜屾寮
€鍙戦『搴忎笌楠屾敹鏍囧噯.md)銆乕澶嶇敤涓庢敼閫犳柟妗圿(01-搴曞骇浠ｇ爜澶嶇敤涓庢敼閫犳柟妗?md)

## 1. 娓呭崟鑱岃矗涓庣淮鎶よ鍒?
鏈竻鍗曞洖绛旓細鍩轰簬浼楀涔嬪搴曞骇瑕佹眰锛岀幇鏈変唬鐮佸叿浣撻渶瑕佽皟鏁翠粈涔堛€佷负浠€涔堣皟鏁淬€佸湪鍝噷璋冩暣銆佸浣曢獙鏀躲€?
- 02 鏂囨。绠＄悊闇€姹傚拰鍐崇瓥锛?3 鏂囨。绠＄悊 WP 宸ヤ綔鍖呬笌 B00锝濨11 瀹炴柦鎵规锛涙湰鏂囩鐞嗛€愭ā鍧楀垎鏋愪骇鐢熺殑缁嗗寲浠诲
姟锛屼笉鍙﹀缓涓€濂楅渶姹傛垨瀹炴柦椤哄簭銆?- 姣忔妯″潡鍒嗘瀽蹇呴』鍚屾椂璁板綍鈥滀紬澧呰姹?鈫?鐜扮姸璇佹嵁 鈫?宸窛 鈫?淇敼浠诲姟 鈫?楠屾
敹鏍囧噯鈥濄€傚彲鐩存帴澶嶇敤鐨勮兘鍔涗篃搴旇鏄庨獙璇佽竟鐣岋紱灏氭湭鏍搁獙涓嶈兘鍐欐垚宸插彂鐜扮己闄枫€?- 姣忛」浣跨敤绋冲畾鐨?ZS 涓讳换鍔＄紪鍙枫
€傝法鎵规宸ヤ綔浣跨敤鍘熺紪鍙峰姞 .A/.B 绛夌ǔ瀹氬瓙椤癸紝涓嶉噸鎺掍富缂栧彿銆佷笉閲嶅璁℃暟锛涘瓙椤圭户鎵夸富浠诲姟鐨?FND/WP銆佷紭鍏堢骇銆
佹敼閫犱緷鎹笌鑼冨洿锛屾寜绗?16.1 鑺傜嫭绔嬭褰曠姸鎬?璇佹嵁鍜岄獙鏀讹紝鍚庣画鏂板闂浼樺厛琛ュ叆鍘熶换鍔°€?- 姣忛」蹇呴』鏈夊叧鑱?FND
/WP/B 缂栧彿銆佷紭鍏堢骇銆佺被鍒€佺姸鎬併€佸墠缃潯浠躲€佷唬鐮佷綅缃€佽皟鏁磋寖鍥村拰姝ｅ弽鍚戦獙鏀躲€傝繘鍏ュ紑鍙戞椂琛ュ厖澶辫触娴嬭瘯銆佸疄
闄呭彉鏇淬€佽縼绉?鎭㈠鍜岄獙璇佽瘉鎹€?- 浼樺厛绾э細P0 鏄墍鍦ㄦ壒娆℃斁琛屽墠鐨勫繀瑕侀」锛汸1 鏄簳搴у畬鏁撮獙鏀舵垨瀵瑰簲鑳藉姏鍚敤鍓嶅
畬鎴愮殑澧炲己/楠岃瘉椤癸紱P2 鏄悗缃」銆侾0 涓嶈〃绀哄彲浠ョ粫杩囧喅绛栨垨渚濊禆绔嬪嵆缂栫爜銆?- 绫诲埆锛氭敼閫犮€佽ˉ寤恒€侀獙璇侀€傞厤銆佸喅
绛栥€傜姸鎬侊細寰呭紑鍙戙€佸緟鍐崇瓥銆佸緟鍓嶇疆銆佸紑鍙戜腑銆佸緟楠屾敹銆佸凡楠屾敹銆佹殏缂撱€傜姸鎬佸彉鍖栧繀椤绘湁鏃ユ湡鍜屼緷鎹紱鈥滃垎鏋愬畬鎴愨€濅
笉鑳藉皢浠诲姟鏍囦负鈥滃凡楠屾敹鈥濄€?- 鏈増鎵€鏈変换鍔＄殑瀹炵幇銆佽嚜鍔ㄥ寲娴嬭瘯銆佺嫭绔嬬幆澧冨拰鍙戝竷璇佹嵁鍧囦负鈥滄湭瀹炴柦/鏈墽琛屸€濓紝璐
熻矗浜烘殏鏈垎閰嶃€傞潤鎬佷唬鐮佷綅缃彧鏄樊璺濊瘉鎹紝涓嶆槸淇璇佹嵁銆?- D-07銆丏-09 浠嶅緟鍐筹紱D-10銆丏-11 浠嶅悗缃€傝处鍙
?韬唤/缁勭粐瑙勫垯涓嶅緱鍊熸暟鎹簱鎶€鏈敼閫犳彁鍓嶅浐鍖栵紱鏈竻鍗曚笉鎺堟潈杩炴帴鎴栨敼鍔ㄧ敓浜ф暟鎹簱銆?- 宸查獙鏀朵换鍔″簲鍥炲～鍥哄畾鎻愪
氦銆佹祴璇曞懡浠?鎶ュ憡銆佺幆澧冪増鏈€佹鍙嶅悜缁撴灉銆佹仮澶嶈矾寰勫拰鏈獙璇侀」锛屽苟鎸?02 鏂囨。绗?9.3 鑺傜櫥璁伴渶姹傜姸鎬併€傛棤鏁版嵁搴
撳彉鏇存垨澶栭儴楠屾敹鏃跺啓鏄庝笉閫傜敤鍘熷洜銆?
## 2. 鍒嗘瀽杩涘害涓庝换鍔¤寖鍥?
鍒嗘瀽椤哄簭鐢ㄤ簬鐞嗚В浠ｇ爜锛屼笉鏇夸唬 03 鏂囨。鐨勫紑鍙戜緷璧栭『搴忋€?
| 鍒嗘瀽妯″潡 | 鍒嗘瀽鐘舵€?| 鏈増浠诲姟鑼冨洿 |
|---|---|---|
| M01 宸ョ▼瑁呴厤涓庤繍琛岄鏋?| 宸插畬鎴愰潤鎬佸垎鏋愶紝宸茶ˉ褰?| ZS-ENG-001锝?06锛屽叡 6 椤?|
| M02 鏁版嵁搴撲笌鎸佷箙鍖?| 宸插畬鎴愰潤鎬佸垎鏋愶紝宸茶ˉ褰?| ZS-DB-001锝?20锛屽叡 20 椤?|
| M03 鎺ュ彛涓庡畨鍏ㄩ摼璺?| 宸插畬鎴愰潤鎬佸垎鏋愶紝宸茬櫥璁?| 鏂板 ZS-SEC-001锝?12锛屽叡 12 椤癸紱澶嶇敤 ZS-ENG-00
4/005銆乑S-DB-018 |
| M04 鐧诲綍涓庝細璇?| 宸插畬鎴愰潤鎬佸垎鏋愶紝宸茬櫥璁?| ZS-LOGIN-001锝?06锛屽叡 6 椤?|
| M05 缁勭粐銆佷汉鍛樹笌韬唤 | 宸插畬鎴愰潤鎬佸垎鏋愶紝宸茬櫥璁?| ZS-IAM-001锝?04锛屽叡 4 椤癸紱涓氬姟瀹炴柦鍙?D-09 闂ㄧ
 |
| M06 绉熸埛涓庢潈闄?| 宸插畬鎴愰潤鎬佸垎鏋愶紝宸茬櫥璁?| ZS-PERM-001锝?04锛屽叡 4 椤癸紱澶嶇敤 ZS-SEC-001銆乑S-DB
-018 |
| M07 瀛楀吀銆佸弬鏁颁笌妯″潡閰嶇疆 | 宸插畬鎴愰潤鎬佸垎鏋愶紝宸茬櫥璁?| ZS-CFG-001锝?04锛屽叡 4 椤?|
| M08 鏂囦欢涓庡璁?| 宸插畬鎴愰潤鎬佸垎鏋愶紝宸茬櫥璁?| ZS-FILE-001锝?05銆乑S-AUDIT-001/002锛屽叡 7 椤?|
| M09 閫氱煡涓庡緟鍔?| 宸插畬鎴愰潤鎬佸垎鏋愶紝宸茬櫥璁?| ZS-MSG-001锝?04锛屽叡 4 椤?|
| M10 浠诲姟銆佷簨浠朵笌骞傜瓑 | 宸插畬鎴愰潤鎬佸垎鏋愶紝宸茬櫥璁?| ZS-JOB-001锝?04锛屽叡 4 椤癸紱澶嶇敤鏃㈡湁璋冨害/PG/璇锋眰
闃查噸浠诲姟 |
| M11 Flowable 涓庝笟鍔℃帴鍏ヨ鑼?| 宸插畬鎴愰潤鎬佸垎鏋愶紝宸茬櫥璁?| ZS-BPM-001锝?04锛屽叡 4 椤癸紱姝ｅ紡棣栭摼鍙?D
-07/D-09 闂ㄧ |
| M12 鍓嶇鎺ュ叆涓庤繍琛屼繚闅?| 宸插畬鎴愰潤鎬佸垎鏋愶紝宸茬櫥璁?| ZS-CLIENT-001锝?05銆乑S-OPS-001锝?03锛屽叡 8 
椤?|
| 妯垏锛氬熀纭€绠＄悊鍔熻兘鍥炲綊 | 宸茶ˉ榻愰獙鏀舵槧灏?| ZS-SYS-001锛屽叡 1 椤癸紱瑕嗙洊涓冪被鍩虹绠＄悊鍔熻兘 |
| 妯垏锛氬搧鐗屼笌浠ｇ爜鍛藉悕缁熶竴 | 宸叉寜鐢ㄦ埛瑕佹眰鐧昏锛沄1.5 瀹屾垚棣栬疆寮€鍙?| ZS-BRAND-001锝?06锛屽叡 6 椤癸紱B
01 璧峰疄鏂斤紝B06 鎶€鏈棴鐜獙鏀?|
| 妯垏锛氭枃妗ｄ笌浠诲姟娌荤悊 | 宸茬櫥璁扮己鍙?| ZS-GOV-001锛屽叡 1 椤?|

12 涓簳搴фā鍧楀凡瀹屾垚闈欐€佸垎鏋愩€俈1.3 涓?85 椤癸紱鏈増鎸夌敤鎴疯姹傛柊澧炲搧鐗屼笌浠ｇ爜鍛藉悕涓撻」 ZS-BRAND-001锝?0
6锛屽叡 6 椤癸紝绱 91 椤逛富浠诲姟銆俈1.14 缁熻锛?026-09-10锛屾寜鍚勫崱銆岀姸鎬併€嶅瓧娈甸噸鏂拌鏁帮級锛?3 椤瑰緟寮€鍙戙€
?1 椤瑰紑鍙戜腑銆?1 椤瑰緟楠屾敹銆? 椤瑰緟鍐崇瓥銆? 椤瑰緟鍓嶇疆锛? 椤瑰凡楠屾敹锛堝緟楠屾敹=ENG-001锝?06銆丏B-003锝?09/011
锝?18/020銆丼EC-002/003/005/007銆両AM-001銆佸搧鐗?001/002/005銆丟OV-001锛涘紑鍙戜腑=DB-001/002/0
19銆丆FG-001/002/003銆丆LIENT-005銆丱PS-001 涓庡搧鐗?003/004/006锛屽潎涓哄垎鎵瑰瓙椤规湭鍏ㄩ儴鏀跺彛鐨勪富鍗＄姸鎬
侊紱寰呴獙鏀?寮€鍙戜腑鎸囬杞紑鍙戝凡瀹屾垚銆佸緟瀵瑰簲鎵规鐪熷疄鐜鏀捐锛岄潪宸查獙鏀讹級銆?5 椤逛富浠诲姟鎷嗕负 57 涓垎鎵瑰瓙椤癸紝瀛愰
」涓嶅彔鍔犲埌 91 椤圭粺璁°€備换鍔¤鐩栧綋鍓嶅簳搴фā鍧楀垎鏋愬拰鍝佺墝鍛藉悕鑼冨洿锛屼笉绛変簬绌峰敖鏈潵涓氬姟绯荤粺銆丳rovider 鎴栨笭閬撶
殑鍏ㄩ儴寮€鍙戜换鍔°€?
鏈疆瀹屾垚鐨勬槸鍏抽敭鍏ュ彛銆佽皟鐢ㄩ摼銆佹暟鎹璞′笌闇€姹傚樊璺濈殑闈欐€佸垎鏋愶紝涓嶆槸鍏ㄤ粨閫愯瀹夊叏瀹¤锛屼篃鏈墽琛岃繍琛屼唬鐮佷慨鏀广€
佹暟鎹簱杩炴帴/杩佺Щ銆佹瀯寤恒€佺湡瀹為泦鎴愭垨閮ㄧ讲銆傚悗缁紑鍙戜腑鍙戠幇鐨勬柊璺緞鎴栧け璐ユ祴璇曞簲缁х画鏇存柊绋冲畾浠诲姟锛屼笉浠ユ湰杞垎鏋愮
粨璁轰唬鏇胯繍琛岄獙璇併€?
D-09 宸蹭簬 2026-09-10 纭锛歓S-DB-009銆乑S-IAM-001 鍐崇瓥鍖呭凡褰㈡垚涔﹂潰缁撹锛坉ocs/02 搂3銆乨ocs/07
 搂4锛夛紝鐢卞緟鍐崇瓥杞緟楠屾敹锛沍S-DB-010 璐﹀彿鍞竴鎬ц縼绉荤殑鍐崇瓥鍓嶇疆宸茶В闄わ紝鍙帓鏈燂紙浠嶉』閬靛畧 B07 鎵规鍓嶇疆锛夈€
傚緟鍓嶇疆锛歓S-DB-010銆乑S-IAM-002/004銆乑S-PERM-003銆乑S-BPM-003銆乑S-OPS-003锛堝叾涓?ZS-BPM-003
/ZS-OPS-003 鍓嶇疆 D-07 缁嗚妭纭锛夈€傚叾浣欎换鍔′腑鐨勨€滃緟寮€鍙戔€濊〃绀哄叿澶囨妧鏈媶瑙ｅ叆鍙ｏ紝浠嶉』閬靛畧鍚勫崱鐗囩殑鎵规
鍓嶇疆锛涘畠浠苟闈炲彲鍚屾椂绔嬪嵆寮€宸ャ€?
V1.2 鏂囨。楠岃瘉璁板綍锛?026-09-08锛夛細涓存椂鍙妫€鏌ラ€氳繃 84 涓富浠诲姟銆?15 涓敮涓€娓呭崟鍐呮湰鍦伴摼鎺ュ強鐗堟湰/缂栧
彿绛夌粨鏋勬鏌ワ紱璇ョ粨鏋滀粎瀵瑰簲 V1.2锛屾病鏈夐獙璇佷緷璧栧浘鍜屽姛鑳借鐩栵紝鍥犳鏈増缁х画淇锛屼笉鑳芥妸缂栧彿瀛樺湪绛夊悓浜庝緷璧栧彲鎵
ц銆傞暱鏈熸鏌ュ櫒鍙?CI 闂ㄧ浠嶇敱 ZS-GOV-001銆乑S-OPS-001 寮€鍙戙€?
V1.3 鏂囨。楠岃瘉璁板綍锛?026-09-08锛夛細褰撶増浣跨敤涓存椂鍙 Node 妫€鏌ユ牳瀵?85 涓富浠诲姟鍙?77/2/6 鐘舵€佺粺璁°€
?2 涓媶鍒嗕富椤逛笌 50 涓瓙椤癸紱灏?63 涓湭鎷嗗垎浠诲姟鍜?50 涓瓙椤逛綔涓?113 涓墽琛岃妭鐐癸紝缁撳悎 03 鏂囨。鐨?12 涓
壒娆￠棬绂佹鏌ユ樉寮忎緷璧栵紝鏈彂鐜版湭鐭?闈炲師瀛愬墠缃€佹壒娆″€掔疆鎴栧惊鐜€?28 涓敮涓€娓呭崟鍐呮湰鍦伴摼鎺ュ潎瀛樺湪锛孎ND/WP/Z
S 寮曠敤鍜?README/01/02/03/05 鐗堟湰涓€鑷达紱涓冪被鍩虹鍔熻兘鍧囧叧鑱斾唬鐮佸叆鍙ｅ強姝ｅ弽鍚戠敤渚嬶紝鍏?14 涓嫙鏂板鐢ㄤ緥鏍
囪瘑銆備互涓婂彧楠岃瘉鏂囨。缁撴瀯涓庢墍澹版槑鐨勪緷璧栵紝涓嶈瘉鏄庡姛鑳姐€佸畨鍏ㄥ疄鐜版垨娴嬭瘯宸查€氳繃锛屼篃鏈氦浠橀暱鏈熸鏌ュ櫒鎴?CI銆?
V1.4 鏂囨。楠岃瘉璁板綍锛?026-09-08锛夛細涓存椂鍙妫€鏌ラ€氳繃 91 涓讳换鍔″強 83/2/6 鐘舵€佺粺璁°€?5 涓媶鍒嗕富椤瑰拰
 57 瀛愰」锛?23 涓墽琛岃妭鐐圭粨鍚?12 涓壒娆￠棬绂佹棤鏄惧紡寰幆鎴栨壒娆″€掔疆锛?36 涓竻鍗曞唴鏈湴閾炬帴鏈夋晥銆傛柊澧為渶姹?浠诲
姟寮曠敤銆丷EADME/01/02/03/05 鐗堟湰鍙婂師鍥惧昂瀵?鍝堝笇涓€鑷达紱杩欎笉鏄唬鐮佹敼鍚嶃€佸吋瀹硅縼绉汇€佸搧鐗岀晫闈㈡垨闀挎湡 CI 宸
插畬鎴愮殑璇佹嵁銆?
V1.5 鍙樻洿涓庨獙璇佽褰曪紙2026-09-08锛夛細鍝佺墝涓庝唬鐮佸懡鍚嶄笓椤瑰畬鎴愰杞紑鍙戯紝鎸夌 16 鑺傜 9 鏉￠『搴忚惤鍦扳€斺€擹
S-BRAND-001 鍐荤粨鏄犲皠锛?87bbc4f锛孾06 鏂囨。](06-鍝佺墝绱犳潗涓庡懡鍚嶆槧灏?md)锛夈€?02 鍚庣鏀瑰悕锛?3d71fad
锛夈€?03.A 涓ょ闈欐€佸搧鐗岋紙ea580b1f锛夈€?04 绉嶅瓙涓庡瓨閲忚縼绉伙紙581927d0锛夈€?05 浠ｇ爜鐢熸垚锛?8ad8e78锛夈
€?06.A 鍛藉悕闂ㄧ锛坆2c26ea9锛夈€傞獙璇佽瘉鎹細鍚庣闈欐€佷竴鑷存€ф鏌?0 纭€ч棶棰橈紱Web 鐢熶骇鏋勫缓涓庡皬绋嬪簭 H5 鏋
勫缓閫氳繃锛泇ue-tsc 涓?04 鎶ュ憡鍩虹嚎涓€鑷达紙0 鏂板閿欒锛夛紱鐪熷疄涓存椂 PG17 瀹屾垚鏂拌/鏃ф牱鏈崌绾?閲嶅杩佺Щ/澶辫触鍥
炴粴楠岃瘉锛涘搧鐗岄棬绂佸叏浠撴壂鎻?0 杩濊銆佹祴璇?7/7銆傛壒娆℃斁琛岃竟鐣屼笉鍙橈細002 鐨勫共鍑€鏋勫缓/渚濊禆鏍?鐪熷疄 PG 鍚姩寰?B0
1/B02 宸ュ叿閾捐ˉ璇侊紝003.B/004.B/004.C/006.B 褰?B03/B05/B06 鑱旈獙锛涙湰璁板綍涓嶈〃绀轰换浣曚富浠诲姟宸查獙鏀躲
€?
V1.6 鍙樻洿涓庨獙璇佽褰曪紙2026-09-09锛夛細M01 宸ョ▼楠ㄦ灦鍏」鍏ㄩ儴瀹屾垚锛圸S-ENG-001 鐧藉悕鍗?295040b3銆?02 
JDK17 鍩虹嚎 f58fd09b銆?03 鐜閰嶇疆鍒嗙 38430267銆?04 鍏抽棴妯℃嫙璁よ瘉 77b5f090銆?05 鏀剁揣绠＄悊绔偣 
7c8fd2c0銆?06 浠诲姟鍚仠杈圭晫 4557cc00锛夛紱M02 鎺ㄨ繘涓冮」鈥斺€擹S-DB-001.A PG 椹卞姩涓庢暟鎹簮鍚堝悓锛?f2e
b7b6锛夈€乑S-DB-003 Flyway 杩佺Щ鏈哄埗锛?106476f锛夈€乑S-DB-004 V1 鍩虹嚎杩佺Щ涓庣湡瀹?PG 鍙屽簱楠岃瘉锛?91
faeef锛夈€乑S-DB-005 鍗囩骇涓庢仮澶嶈绋嬪惈 V2 qrtz 鍥炲～涓庝笁娈垫紨缁冿紙f7702928锛夈€乑S-DB-002 鐜鏂规涓
?PEND-004 娓呭崟锛?9e1a93c锛夈€乑S-DB-019.A Docker 澶瑰叿鑷瘉涓夋潯璇箟锛?a0d5d04锛夛紱鍙﹀畬鎴?ZS-CFG
-001.A 绉樺瘑闂ㄧ锛坋bc7c9ba锛夈€乑S-CFG-003.A 鍔熻兘鐩綍涓庡椁愭牎楠岋紙2160d600锛夈€乑S-CLIENT-005.A
 鏋勫缓涓庣被鍨嬪熀绾块棬绂侊紙65272cdf锛夈€乑S-GOV-001 鏂囨。涓€鑷存€ф鏌ュ櫒锛?d618920锛夈€乑S-OPS-001.A 鑱氬悎
娴佹按绾块鏋讹紙bda57b95锛夈€傛寔缁槻绾匡細`node scripts/ops/run-local-gates.mjs` 10/10 閫氳繃锛堟湰
鍦颁笌 CI 鍚岃鍒欙級銆傚墿浣?66 椤瑰緟寮€鍙戜换鍔＄殑鎺ㄨ繘鍓嶆彁锛欱02 鐜锛圝DK 17 + Maven 鈮?.8 + PostgreSQL
 17锛屾寜鏁版嵁搴撶幆澧冩柟妗堣惤鍦帮級瑙ｉ攣 M02 鐪熷疄搴撻獙璇佷笌 ZS-OPS-001.B锛汥-07/D-09 鍐崇瓥瑙ｉ攣 M05/M06/B0
7~B09锛汢03~B06 鑱旈獙鐜瑙ｉ攣 M03/M04/M08~M12 杩愯楠岃瘉锛汥-10/D-11 涓庣粓绔寖鍥磋В閿佸井淇?鏀粯/AI/A
pp銆傛湰璁板綍涓嶈〃绀轰换浣曚富浠诲姟宸查獙鏀躲€?
V1.6 琛ュ厖璁板綍锛?026-09-09 鏅氾紝宸ュ叿閾惧紩瀵硷級锛氬湪浠撳簱 tools/ 鐩綍鑷紩瀵?Temurin JDK 17.0.20.1 
+ Maven 3.9.9锛坱ools/ 涓嶅叆搴擄級锛岄殢鍚庡畬鎴愰娆″叏閲忕湡瀹炴瀯寤猴紙zszj-server.jar 170MB锛変笌鐪熷疄 PG1
7+Redis 鍚姩鑱旈獙锛欶lyway 瀹為檯鎵ц V1+V2銆?actuator/health UP銆佸叧闂ā鍧椾笌浼?token 鍧?401 鎷掔
粷銆傛瀯寤?鍚姩鏆撮湶骞朵慨澶嶅洓绫荤湡瀹炵己闄凤紙Map.of 瓒呭弬銆佽ˉ涓佹畫鐣欏瀮鍦惧瓧绗︺€佺己 junit/actuator 渚濊禆銆丏ruid
 PSCache PG 鍏煎锛夛紝璇﹁鎻愪氦 adc5b67e 涓庡悇鍗″紑鍙戣褰曘€傝璇佹嵁浣?ZS-DB-001.B 鐨勪緷璧栨爲/鎵撳寘/鍚姩
椤规彁鍓嶈幏寰楀疄娴嬫敮鎾戯紱姝ｅ紡 B02 浠嶉渶璐熻矗浜烘寜鏁版嵁搴撶幆澧冩柟妗堣惤鍦拌处鍙?澶囦唤骞跺楠屻€?
V1.7 鍙樻洿璁板綍锛?026-09-09锛夛細鎸夊悇浠诲姟鍗°€岀姸鎬併€嶅瓧娈碉紙鍐呰仈浜庛€屽叧鑱斻€嶈锛夐噸鏂拌鏁帮紝璁㈡绗?2 鑺傞《閮ㄦ眹
鎬烩€斺€斿疄闄呬负 48 寰呭紑鍙戙€?1 寮€鍙戜腑銆?4 寰呴獙鏀躲€? 寰呭喅绛栥€? 寰呭墠缃€? 宸查獙鏀讹紙姝ゅ墠 V1.6 姹囨€?66/1
0/7/2/6 婊炲悗浜庡崱鐗囷細M01 鍏」涓?M02 鐨?DB-006锝?20 澶氶」宸查殢棣栬疆寮€鍙戣浆銆屽緟楠屾敹銆嶃€丆FG-002 杞€屽紑
鍙戜腑銆嶏紝椤堕儴缁熻鏈噸鏂拌鏁帮級銆傚悓姝ュご閮ㄧ姸鎬佽锛氶杞紑鍙戝凡鍚湡瀹炶繍琛屼唬鐮佷笌鏁版嵁搴撹縼绉诲彉鏇达紝涓嶅啀琛ㄨ堪涓恒€屾湭瀹炴
柦杩愯浠ｇ爜鎴栨暟鎹簱鍙樻洿銆嶃€傛湰娆′负绾枃妗ｅ彛寰勮姝ｏ紝鏈敼鍔ㄤ换浣曚换鍔″崱瀹炶川鐘舵€併€佸紑鍙戣褰曟垨杩愯浠ｇ爜锛汻EADME 绱
㈠紩鐗堟湰鍚屾鑷?V1.7銆?
V1.8 鍙樻洿璁板綍锛?026-09-09锛夛細ZS-DB-018锛堣ˉ榻?ORM銆佹墜鍐?SQL 涓庣鎴烽殧绂荤殑 PG 鍥炲綊锛夋壙鎺?07ff751
b锛圚2 Java 澶瑰叿锛屽凡鐧昏鎷︽埅鍣ㄧ骇闅旂寰呯湡瀹?PG锛夎ˉ榻?B02 鐪熷疄 PG 鏁版嵁灞傞獙璇佲€斺€斾氦浠?run-db018-ver
ify.mjs锛屼互涓?TenantDatabaseInterceptor 鏀瑰啓绛変环鐨?SQL 鍦?System 鎶€鏈鎴?1/2 鍙岀鎴峰す鍏烽獙
璇?C1~C10锛圕RUD/鍒嗛〉/鍏宠仈/鎵归噺/閫昏緫鍒犻櫎/鎵嬪啓 SQL 鎸?tenant_id=ctx 闅旂锛涗吉閫犱粬绉熸埛瀵硅薄 ID 璇?鏀
?鍒犲潎 0 琛岋紱鍏ㄥ眬琛?蹇界暐娉ㄨВ/绯荤粺娓呯悊涓夌被鍚堟硶鑼冨洿浠ョ粨鏋勬柇瑷€涓庢鍙嶈矾寰勫垪鏄庯紱--self-test 璐熷悜瀵圭収璇佹槑闈
炵┖娲烇級锛屾敞鍐岃繘 run-pg-regression.mjs锛? 濂椾欢鏈湴鍏ㄧ豢锛夛紝run-local-gates --fast 10/10 閫氳
繃锛涘崱鐗囩姸鎬?寰呭紑鍙戔啋寰呴獙鏀讹紝绗?2 鑺傜粺璁￠殢涔?48/24鈫?7/25锛堜綑涓嶅彉锛夈€傝竟鐣屼笉鍙橈細SQL 绾ц瘉鏄庢暟鎹眰鎶€鏈
鎴烽摼锛岄潪 PG 鍘熺敓 RLS锛汷RM 鎷︽埅鍣?Java 绾т笌 HTTP 灞傜鎴锋瘮瀵圭敱 ZS-SEC-012.A/001.A 瑕嗙洊锛涗袱涓氬姟
缁勭粐涓庤法缁勭粐鏈嶅姟鎺堟潈瀹屾暣楠屾敹椤?D-09 鍚庝簬 B08 鏀捐锛宼enant_id鈫抰enant_org_id 鏄犲皠涓?D-09 鍚庝换鍔°
€傛湰璁板綍涓嶈〃绀轰换浣曚富浠诲姟宸查獙鏀躲€?
V1.9 鍙樻洿璁板綍锛?026-09-09锛夛細ZS-PERM-002.A锛堢幇鏈夌敤鎴?閮ㄩ棬瀵硅薄鐨勬妧鏈巿鏉冪煩闃靛強閫氱敤妫€鏌ュ叆鍙ｏ紝B03锛
涘墠缃?ZS-DB-018銆乑S-SEC-012.A 鍧囧凡瀹屾垚锛変氦浠樷€斺€旀柊澧炴鏋剁骇瀵硅薄绾ф鏌ュ叆鍙?DeptDataPermissionC
hecker锛堢敱 ZszjDeptDataPermissionAutoConfiguration 瑁呴厤涓?Bean锛夛紝澶嶇敤涓?DeptDataPerm
issionRule 瀹屽叏涓€鑷寸殑 ALL/鎸囧畾閮ㄩ棬/鏈汉鏁版嵁鑼冨洿璇箟骞跺叡浜叾 LoginUser 涓婁笅鏂囩紦瀛橈紙涓嶉噸澶嶈绠楋級锛
屼綔涓?SQL 闈欓粯杩囨护鐨勬樉寮忔嫆缁濊ˉ鍏咃細checkObjectVisible 瀵瑰悓鎶€鏈鎴锋棤鏉冨璞℃姏 FORBIDDEN銆乧heckB
atchVisible 瀵规壒閲忔贩鍏ヤ换涓€瓒婃潈鏁存壒鎷掔粷锛屾棤鐧诲綍鐢ㄦ埛/闈?ADMIN 鎶ゆ爮涓?rule 瀵归綈銆佸彇涓嶅埌鏉冮檺 fail-c
losed锛涘悓鏃剁櫥璁癧鏁版嵁鏉冮檺鎺堟潈鐭╅樀](../services/zhongshu-core/docs/鏁版嵁鏉冮檺鎺堟潈鐭╅樀.md)锛堢幇鏈?
system_users/system_dept 琛ㄥ垪娉ㄥ唽銆佸瑙掕壊骞堕泦鍚堝苟璇箟銆佽闂矾寰勮鐩栥€佹湭娉ㄥ唽鍗虫湭閫傞厤鐨勫缃級銆傚す鍏
?DeptDataPermissionCheckerTest 13 鐢ㄤ緥锛圓LL/鍛戒腑閮ㄩ棬/鏈汉/鏃犳潈鎷掔粷/鎵归噺娣峰叆鏁存壒鎷掔粷/鎶ゆ爮/fa
il-closed/缂撳瓨澶嶇敤锛夛紝鏁版嵁鏉冮檺 starter 鍏ㄦā鍧?56/56 BUILD SUCCESS锛宺un-local-gates --fa
st 10/10銆傛部鐢?ZS-PERM-001.A 鎯緥锛氬垎鎵瑰瓙椤?.A 浜や粯銆佷富鍗＄姸鎬佸緟鍏ㄩ儴閫傜敤瀛愰」锛?B 椤?D-09/B08锛夋敹
鍙ｅ悗鍐嶅彉鏇达紝鏁?ZS-PERM-002 涓诲崱鐘舵€佺淮鎸?寰呭紑鍙戙€佺 2 鑺傜粺璁′笉鍙樸€傝竟鐣岋細鏈」鍙鐩栨妧鏈鎴峰唴 dept/
self 鏁版嵁鑼冨洿杞达紝璺ㄧ鎴烽殧绂诲鐢?ZS-DB-018 涓嶉噸鍐欙紝涓氬姟缁勭粐 SELF/ASSIGNED 璺ㄧ粍缁囨巿鏉冧笌姝ｅ悜涓氬姟鐭╅
樀褰?ZS-PERM-002.B锛涙鏌ュ櫒涓洪€氱敤鍏ュ彛锛屽悇涓氬姟琛?璺緞鐨勯€愪竴鎺ュ叆闅忛鍩熸ā鍧楀紑鍙戣惤鍦般€傛湰璁板綍涓嶈〃绀轰换浣曚富浠
诲姟宸查獙鏀躲€?
V1.10 鍙樻洿璁板綍锛?026-09-09锛夛細ZS-SEC-002锛堝缓绔嬫帴鍙ｅ垎绫汇€佸尶鍚嶇櫧鍚嶅崟涓庢柟娉曟潈闄愭竻鍗曪紝B03锛涘墠缃?ZS-
ENG-001/004/005 鍧囧凡瀹屾垚锛夋寜鐢ㄦ埛閫夊畾銆岄潤鎬佹竻鍗?+ 婕傜Щ鍩虹嚎銆嶅舰鎬佷氦浠樷€斺€旀柊澧?`ApiInventoryTest
`锛坺szj-server/src/test锛屼豢 `ModuleWhitelistTest` 闈欐€佹簮鐮佹壂鎻忋€佷笉鍚姩 Spring 涓婁笅鏂囷級閬
嶅巻鍚敤妯″潡鍏ㄩ儴 Controller 鐢熸垚 328 绔偣娓呭崟锛堟巿鏉冪被鍒笁鍒?ANONYMOUS 25锛廇UTHENTICATED 52锛廝
ERMISSION 251锛屼富浣?ADMIN 322锛廙EMBER 6锛夛紝涓庡凡鎻愪氦鍩虹嚎閫愯姣斿銆佹紓绉诲嵆澶辫触瑕佹眰璇勫锛屽惈 3 椤圭粨鏋
勬柇瑷€锛堜富浣?鍓嶇紑涓€鑷淬€佹潈闄愭爣璇嗗悎娉曘€佸尶鍚嶇鐐逛笌鐩綍鍙屽悜涓€鑷达級锛?5 涓尶鍚嶇鐐归€愰」澶嶆牳鍧囧悎娉曞繀闇€骞剁櫥璁?5
 涓畫浣欏鏌ョ偣锛涜繍琛屾椂鍚堝悓澶嶇敤 ZS-SEC-012.A 澶瑰叿銆佹柊澧炵 8 缁?`ApiClassificationContract`锛圓
SYNC 棣栨娲惧彂涓嶅厤璁よ瘉 2 渚?+ ADMIN/MEMBER 鍙屽悜涓茬敤 2 渚嬶級杈?35/35锛寊szj-server 8/8锛圓piInv
entoryTest 4 + ModuleWhitelistTest 4锛夈€乣run-local-gates --fast` 10/10锛涚櫥璁癧鎺ュ彛娓呭
崟涓庡尶鍚嶇櫧鍚嶅崟](../services/zhongshu-core/docs/鎺ュ彛娓呭崟涓庡尶鍚嶇櫧鍚嶅崟.md)銆傞『甯︿慨澶?ZS-ENG-00
1 鏃㈡湁 `ModuleWhitelistTest` 鏂█ bug锛堥娆′互宸ュ叿閾捐繍琛?zszj-server 鍏ㄦ祴璇曟椂鏆撮湶锛岄潪 SEC-0
02 寮曞叆锛涜瑙佺 3 鑺?ENG-001 寮€鍙戣褰曞伐鍏烽摼澶嶉獙琛ュ綍锛夈€傚崱鐗囩姸鎬?寰呭紑鍙戔啋寰呴獙鏀讹紝绗?2 鑺傜粺璁￠殢涔?47/
25鈫?6/26锛堜綑涓嶅彉锛夈€傝竟鐣屼笉鍙橈細ASYNC/SSE 鍏ㄩ潰杩愯鏃跺悎鍚屼笌鐪熷疄 PG/Redis 褰?ZS-SEC-012.B锛屽璞℃
巿鏉冭酱褰?ZS-PERM-002.A锛孲EC-002 鏈媶鍒嗘壒瀛愰」銆傛湰璁板綍涓嶈〃绀轰换浣曚富浠诲姟宸查獙鏀躲€?
V1.11 鍙樻洿璁板綍锛?026-09-09锛夛細ZS-SEC-005锛堢粺涓€閿欒鍝嶅簲銆丠TTP 鐘舵€佷笌澶辫触鏃ュ織缁撴灉锛孊03/B04锛涘墠缃
?ZS-SEC-002 宸插畬鎴愶級鎸夌敤鎴烽€夊畾銆屾柟妗圓 闈炵牬鍧忋€嶅舰鎬佷氦浠樷€斺€斾繚鐣欏钩鍙版棦鏈?HTTP 200 浼犺緭 + 涓氬姟鐮佸
绾︼紙涓ょ璇锋眰灞備緷璧栵紝涓嶆敼鐪熷疄 HTTP 鐘舵€佺爜锛夛紝鏂板 `WebFrameworkUtils.writeJSON` 缁熶竴鍑哄彛锛坄se
tCommonResult` 鐧昏 common_result 灞炴€?+ `ServletUtils` 鍐欎綋锛夛紝灏?6 鏂囦欢 8 澶?filter-
direct 鍐欑偣锛圱okenAuthenticationFilter 璁よ瘉 401銆丄ccessDeniedHandlerImpl 鏉冮檺 403銆丄u
thenticationEntryPointImpl 璁よ瘉 401銆乀enantSecurityWebFilter 绉熸埛 403/400/寮傚父 3 鐐广
€丏emoFilter 婕旂ず 901銆丄piEncryptFilter 鍔犲瘑寮傚父锛変粠鍙啓浣撶殑 `ServletUtils.writeJSON` 杩
佺Щ鍒扮粺涓€鍑哄彛锛屼娇 [璁块棶鏃ュ織][E39] 鐨?ApiAccessLogFilter 鎸変笟鍔＄爜璁板綍缁撴灉銆?01/403/429/5xx 涓
嶅啀璇涓烘垚鍔燂紱[寮傚父澶勭悊鍣╙[E37] GlobalExceptionHandler 鐣稿舰 JSON/璇锋眰浣撲笉鍙В鏋愬綊 400锛堝師钀?
defaultExceptionHandler 500锛夛紝涓?InvalidFormatException 鍒嗘敮鏀瑰洖鏄炬湡鏈涚被鍨?`getTarget
Type()` 鑰岄潪鍘熷鍏ュ弬 `getValue()`锛屼笉娉勯湶鏁忔劅鍊笺€傚鐢?ZS-SEC-012.A 澶瑰叿鏂板绗?9 缁?`Unified
ErrorResponse`锛? 鐢ㄤ緥锛氳璇?绉熸埛缂哄け/绉熸埛涓嶅尮閰?filter-direct 鐧昏 common_result銆佺暩褰?JSO
N 400銆侀潪娉曟牸寮忎笉鍥炴樉鏁忔劅鍊笺€?03 璺ㄥ眰涓€鑷达級锛屼互 `WebFrameworkUtils.getCommonResult` 鏂█浣
滀负璁块棶鏃ュ織涓嶈鎴愬姛鐨勫洖褰掓姢鏍忥紝biz-tenant 41/41銆亀eb+biz-tenant Maven 鍏ㄧ豢锛涚櫥璁癧閿欒鍝嶅簲涓庣姸鎬
佺爜鐭╅樀](../services/zhongshu-core/docs/閿欒鍝嶅簲涓庣姸鎬佺爜鐭╅樀.md)锛圚TTP 浼犺緭鐘舵€伱椾笟鍔＄爜鐭╅樀銆
乫ilter-direct 鍑哄彛缁熶竴銆佺暩褰?JSON before/after銆佷袱绔姹傚眰闆舵敼鍔ㄦ槧灏勶級銆備袱绔姹傚眰锛圼Web][E51
] service.ts axios銆乕绉诲姩绔痌[E52] http.ts uni.request锛夌粡瀹炶瘉闆舵敼鍔ㄥ吋瀹逛笟鍔＄爜 401/403/42
9/5xx锛屾柟妗圔锛堟敼鐪熷疄 HTTP 鐘舵€佺爜锛夊洜浼氭柇 Web axios 浠ょ墝鍒锋柊閾捐鍚﹀喅銆傚崱鐗囩姸鎬?寰呭紑鍙戔啋寰呴獙鏀讹紝绗?2
 鑺傜粺璁￠殢涔?46/26鈫?5/27锛堜綑涓嶅彉锛夈€傝竟鐣屼笉鍙橈細鏈」涓嶆壙璇烘墍鏈夎姹傚己琛屽寘 CommonResult锛堟枃浠?浜岃繘鍒?S
SE 鍗曞垪锛夛紝鐪熷疄 HTTP 鐘舵€佽涔変笌涓ょ鍒锋柊/璺宠浆/涓嬭浇鍦ㄧ湡瀹炵幆澧冪殑鍏ㄩ潰楠屾敹褰?ZS-SEC-012.B锛屾棩蹇楄劚鏁忓綊 
ZS-SEC-007锛宼race 鍏宠仈褰?ZS-SEC-006銆傛湰璁板綍涓嶈〃绀轰换浣曚富浠诲姟宸查獙鏀躲€?
V1.12 鍙樻洿璁板綍锛?026-09-09锛夛細ZS-SEC-007锛堢粺涓€璁块棶銆佸紓甯镐笌淇濇姢鍒囬潰鐨勬棩蹇楄劚鏁忥紝B03/B04锛涘墠缃?ZS
-ENG-004 宸插畬鎴愶紝澶嶇敤 ZS-SEC-005 澶辫触澶瑰叿锛変氦浠樺叡浜劚鏁忓伐鍏?`LogSanitizeUtils`锛坺szj-commo
n锛屽洓鍏ュ彛 sanitizeJson/sanitizeMap/sanitizeArgs/sanitizeResponseBody锛涢敭褰掍竴 + 鍐呯疆鍑
嵁鏍归泦 contains 妯＄硦鍖归厤 + 绔偣绾?extraKeys 绮剧‘鍖归厤 + 瀵硅薄/鏁扮粍閫掑綊鎺╃爜 `***` + 澶辫触鍙鎽樿
涓嶅洖閫€鍘熸枃 + 2048 鎴柇锛夛紝钀藉湴璁块棶鏃ュ織 [E39]銆佸紓甯稿鐞嗗櫒 [E37]銆佸箓绛?[E49]銆侀檺娴?[E50] 鍥涘啓鐐癸紙
query/body/鏂规硶鍙傛暟鍑€鍖栥€乺esponseBody 榛樿涓嶈銆佸啓搴撳け璐ヤ笉鍐嶆墦鍗?DTO锛夛紝骞剁撼鍏ュ悓绫荤 5 鍐欑偣 `A
piSignatureAspect`锛堢鍚嶅け璐ュ弬鏁?sanitizeArgs 鑴辨晱 + 閲嶅璇锋眰 sign 浠?MASK 鎺╃爜锛屼粎鏀规棩蹇椾笉
鍔ㄩ獙绛撅紱MASK 鐢?private 鎻愬崌涓?public 渚涘垏闈㈠氨鍦版帺鐮侊級锛涙祴璇?`LogSanitizeUtilsTest` 10 + 5
 鍐欑偣缁勪欢娴嬭瘯锛坧rotection 6 鍚棦鏈?ApiSignatureTest銆亀eb 4锛屼互 Logback ListAppender 鏂█
绉樺瘑涓嶅叆鏃ュ織銆侀潪鏁忔劅瀛楁涓庢柟娉曟弿杩颁繚鐣欍€佸啓搴撳け璐ヤ笉杈撳嚭鏈噣鍖栧璞★級锛孧aven common 10/protection 6
/web 4 鍏?BUILD SUCCESS銆乣run-local-gates --fast` 10/10锛涚櫥璁癧鏃ュ織鑴辨晱绛栫暐](../service
s/zhongshu-core/docs/鏃ュ織鑴辨晱绛栫暐.md)锛堝洓鍏ュ彛濂戠害銆佷簲姝ョ畻娉曘€佷簲鍐欑偣鐭╅樀銆乪xtraKeys 鎵╁睍銆侀獙鏀跺
榻愶級銆傚崱鐗囩姸鎬?寰呭紑鍙戔啋寰呴獙鏀讹紝绗?2 鑺傜粺璁￠殢涔?45/27鈫?4/28锛堜綑涓嶅彉锛夈€傝竟鐣屼笉鍙橈細鐪熷疄鏃ュ織琛ㄨ惤搴撳噣鍖栦笌
鐢熶骇閲囨牱銆佹晱鎰熺鐐归粯璁や笉璁版鏂囩殑杩愯鏃堕獙璇佸綊 B03/B04锛屽紓甯告棩蹇楁潈闄愪笌淇濈暀鏈熷綊 M08 ZS-AUDIT-002锛屽弬
鏁版牎楠屼笌璇锋眰浣撳ぇ灏忛檺鍒跺綊 ZS-SEC-008銆傛湰璁板綍涓嶈〃绀轰换浣曚富浠诲姟宸查獙鏀躲€?
V1.13 鍙樻洿璁板綍锛?026-09-10锛夛細ZS-SEC-003锛堣鑼?Token 浼犺緭涓庣壒娈婅繛鎺ュ嚟鎹紝B03锛涘墠缃?ZS-SEC-00
2 宸插畬鎴愶級鎸夈€屼弗鏍艰В鏋?+ 閲嶅/鍐茬獊鎷掔粷 + URL 鍙傛暟閫氶亾寮€鍏炽€嶉潪鐮村潖褰㈡€佷氦浠樷€斺€旀敼閫?[SecurityFra
meworkUtils][E31] `obtainAuthorization`锛氣憼涓ユ牸 Bearer 鍓嶇紑锛圧FC 6750/7235锛宍regionM
atches` 澶у皬鍐欎笉鏁忔劅 + 蹇呴』绌虹櫧鍒嗛殧锛夋浛浠ｆ棫 `indexOf("Bearer ")` 浠绘剰浣嶇疆瀛愪覆鏌ユ壘 + magic `
substring(+7)`锛屼慨澶?`"xBearer y"` 琚瑙ｆ瀽涓?`"y"` 缂洪櫡锛堝洖褰掓姢鏍忕敤渚嬮攣瀹氾級锛宍"Bearer"` 鍗曠
嫭/`"BearerXyz"` 鏃犵┖鐧藉垎闅?鍓ョ鍚庝负绌哄潎鍒ょ暩褰㈣繑鍥?null銆佹棤鍓嶇紑鎸夎８ Token 鍏煎鏃㈡湁瀹㈡埛绔紱鈶￠噸澶?鍐
茬獊鎷掔粷锛堝嚟鎹蛋绉侀槻鎶わ級锛屾柊澧?`distinctNonEmptyValues` 褰掍竴鍚屾潵婧愬鍊硷紝Header 鎴?Parameter 鍑
虹幇澶氫釜涓嶅悓鍊煎嵆鎷掔粷杩斿洖 null銆佸涓浉鍚屽€煎綊涓€锛涒憿Header 浼樺厛 + 鍙傛暟閫氶亾寮€鍏筹紝鏂板鍥涘弬閲嶈浇 `obtain
Authorization(...,parameterEnabled)`銆佹棫涓夊弬閲嶈浇濮旀墭 `true` 鍚戝悗鍏煎锛屼粎褰?Header 缂哄け涓斿
紑鍏冲紑鍚墠鍥為€€ URL 鍙傛暟锛堜繚鐣?WebSocket `/ws?token=` 绛夋棤娉曡缃?Header 鐨勮幏鍑嗚繛鎺ワ紝涓嶅垹闄ゅ叏閮ㄥ
弬鏁版敮鎸侊級銆傛柊澧?`SecurityProperties.tokenParameterEnabled`锛圔oolean 榛樿 true銆丂NotNul
l锛夛細榛樿淇濈暀 WebSocket 鍙傛暟杩炴帴鑳藉姏锛屽叡浜?閮ㄧ讲鐜鍙疆 false 浠庢湇鍔＄绂佹鏅€氶暱鏁堝嚟鎹繘鍏?URL锛堣
閬胯闂棩蹇椼€佹祻瑙堝櫒鍘嗗彶銆丷eferer銆佷唬鐞嗙暀瀛樻硠闇诧級銆備笁澶勮皟鐢ㄦ柟锛圼Token 杩囨护鍣╙[E30] `doFilterInter
nal`銆乣AuthController` admin 鐧诲嚭銆乣AppAuthController` app 鐧诲嚭锛夌粺涓€浼?`getTokenPara
meterEnabled()`銆備袱绔墠绔疄璇侀浂鏀瑰姩鍏煎锛圵eb `service.ts`銆佸皬绋嬪簭 `interceptor.ts`/`sse.
ts`/`download.ts` 涓€寰?`Authorization: Bearer` Header锛屾棤涓€璧?URL 鍙傛暟锛沇ebSocket 鏄
敮涓€ `?token=` 閫氶亾锛岀敱榛樿 true 淇濈暀锛夈€傛祴璇曟柊澧?`SecurityFrameworkUtilsTest`锛坄MockHtt
pServletRequest` 绾崟娴嬫棤 DB/Redis锛?8 鐢ㄤ緥锛堟爣鍑?澶у皬鍐?瑁?Token/瀛愪覆璇В鏋愬洖褰掓姢鏍?鐣稿舰涓夌被/缂
哄け/trim/鍙傛暟鍥為€€/鍙傛暟甯?Bearer/鍙傛暟閫氶亾鍏抽棴/Header 浼樺厛/閲嶅澶村啿绐佷笌鐩稿悓/閲嶅鍙傛暟鍐茬獊/绌?Heade
r 鍥為€€/parameterName 绌哄拷鐣ワ級锛宻ecurity starter pom 琛?spring-boot-starter-test + m
ockito-inline锛涚櫥璁癧Token 浼犺緭涓庤繛鎺ュ嚟鎹鑼僝(../services/zhongshu-core/docs/Token浼犺緭涓
庤繛鎺ュ嚟鎹鑼?md)銆傞獙璇侊細`mvn -pl :zszj-spring-boot-starter-security -am test` 18/18 
BUILD SUCCESS锛堜笂娓?common 8/web 鍏ㄧ豢锛夛紱`AuthController`锛坄-pl :zszj-module-system 
-am -DskipTests compile`锛夈€乣AppAuthController`锛坄-f zszj-module-member/pom.xml -
DskipTests compile`锛宮ember 鍦ㄦ牴 pom 娉ㄩ噴鎺掗櫎銆佸厛 install security 鍒锋柊鏈湴浠撳簱锛夊潎 BUIL
D SUCCESS锛沗run-local-gates --fast` 10/10銆傚崱鐗囩姸鎬?寰呭紑鍙戔啋寰呴獙鏀讹紝绗?2 鑺傜粺璁￠殢涔?44/28鈫?
3/29锛堜綑涓嶅彉锛夈€傝竟鐣屼笉鍙橈細鏂囦欢涓撶敤鐭椂绁ㄦ嵁涓庡彇娴佸綊 ZS-FILE-004.A 鍦?B04锛沗tokenParameterEnab
led=false` 鐢熶骇纭寲銆佺湡瀹炰唬鐞?鏃ュ織鍚屾涓?WebSocket 绔埌绔繛鎺ラ獙鏀跺綊 ZS-SEC-012.B锛汷Auth2 寮€鏀
剧鐐瑰嚟鎹紙client_secret/basic锛変紶杈撳鏌ユ壙鎺?SEC-002 娈嬩綑瀹℃煡鐐归殢 OAuth2 棰嗗煙钀藉湴銆傛湰璁板綍涓嶈〃绀
轰换浣曚富浠诲姟宸查獙鏀躲€?
## 3. M01锛氬伐绋嬭閰嶄笌杩愯楠ㄦ灦

### ZS-ENG-001锛氬浐鍖栨ā鍧楃櫧鍚嶅崟涓庡叧闂ā鍧楃殑杩愯杈圭晫

- 鍏宠仈锛欶ND-ARCH-001/002銆丗ND-INF-005锛沇P-02/18锛汢01銆備紭鍏堢骇 P0锛涚被鍒?鏀归€狅紱鐘舵€?寰呴獙鏀讹紱鍓嶇疆
 B00 鏉ユ簮闂ㄧ銆?- 浼楀瑕佹眰涓庣幇鐘讹細妯″潡鍖栧崟浣撳彧鍚敤搴曞骇蹇呰妯″潡銆傚綋鍓?[鏍?POM][E01]銆乕Server POM][
E02] 榛樿瑁呴厤 System/Infra锛屼絾 [DefaultController][E04] 瀵瑰叧闂ā鍧椾粛鏈夊厹搴曡矾鐢憋紱瀛樺湪婧愮爜銆佽
彍鍗曟垨鍏滃簳鍝嶅簲閮戒笉鑳界畻妯″潡宸插惎鐢ㄣ€?- 璋冩暣锛氬舰鎴愬敮涓€鍚敤娓呭崟锛屾牳瀵逛緷璧栥€佽嚜鍔ㄩ厤缃€丅ean銆丆ontroller銆丣
ob銆佽彍鍗曚笌鍓嶇鍏ュ彛锛涙湭鍚敤妯″潡鍙繑鍥炴槑纭殑涓嶅彲鐢ㄥ搷搴旓紝浣嗕笉寰楁墽琛屼笟鍔°€佸啓鍏ユ暟鎹垨娉ㄥ唽鍚庡彴浠诲姟銆備繚鐣欐簮鐮佽祫
浜э紝涓嶆暣搴撳垹闄や笟鍔℃ā鍧椼€?- 楠屾敹锛氭柊澧炵櫧鍚嶅崟娴嬭瘯锛涘繀瑕佹ā鍧楀彲鏋勫缓锛涘叧闂ā鍧楁棤鐪熷疄涓氬姟 Bean/浠诲姟/鍙搷浣滆彍
鍗曪紱鐩存帴璇锋眰鍏抽棴妯″潡涓嶈兘鍙戠敓涓氬姟鍐欏叆銆傝縼绉昏彍鍗曠殑鍙樻洿鐢?ZS-DB-004 绠＄悊銆?- 寮€鍙戣褰曪紙2026-09-09锛夛細
鏂板 [ModuleWhitelist](../services/zhongshu-core/zszj-server/src/main/java/cn/zs
zj/server/ModuleWhitelist.java) 浣滀负鍞竴鍚敤娓呭崟锛堝惎鐢?system/infra锛?3 涓湭鍚敤妯″潡鍙婂叾 a
dmin-api 鍓嶇紑鐧昏锛夛紱[DefaultController](../services/zhongshu-core/zszj-server/src
/main/java/cn/zszj/server/controller/DefaultController.java) 涓嶅彲鐢ㄥ搷搴斾笌鐧藉悕鍗曟寕閽┿€
佷笉鍐嶅紩瀵间笂娓告枃妗ｏ紱鏂板鐧藉悕鍗曟祴璇?ModuleWhitelistTest锛圥OM 婵€娲讳竴鑷存€с€佸厹搴曡鐩栧畬鏁存€с€佹湭鍚敤鍖呭
紩鐢ㄦ嫤鎴級锛涙柊澧炴棤 JDK 闈欐€佹鏌?`node scripts/eng/verify-module-whitelist.mjs`锛? 闂锛夈
€傛牳瀵圭粨璁猴細鏈惎鐢ㄦā鍧椾笉鍦ㄤ緷璧栭棴鍖呭唴锛屽叾 Bean/Job/鑷姩閰嶇疆鏃犳硶瑁呴厤锛涘厹搴曞搷搴斾负绾敊璇繑鍥烇紝鏃犱笟鍔″啓鍏ヨ矾
寰勩€傚緟楠屾敹璇存槑锛氳彍鍗曠瀛愬鍏抽棴妯″潡鐨勫彲瑙佹€ф不鐞嗗綊 ZS-DB-004锛涙祴璇曡繍琛屼笌"蹇呰妯″潡鍙瀯寤?寰?B01 宸ュ叿閾惧
楠屻€?*宸ュ叿閾惧楠岃ˉ褰曪紙2026-09-09锛孼S-SEC-002 鏈熼棿锛?*锛氶娆′互 tools/ 鑷紩瀵?JDK17+Maven3.9
.9 杩愯 zszj-server 鍏ㄦ祴璇曟椂锛宍ModuleWhitelistTest.enabledModulesMatchServerPomDepe
ndencies` 鏆撮湶鏃㈡湁鏂█缂洪櫡鈥斺€擿activeModuleArtifacts` 宸插墺绂?`zszj-module-` 鍓嶇紑杩斿洖鍊硷紝鏂
█鍗村張鎸夎鍓嶇紑 `startsWith` 杩囨护 server POM 渚濊禆锛宎ctual 鎭掍负绌洪泦鑷村繀鐒跺け璐ャ€傛缂洪櫡鑷?ENG-00
1 浜や粯锛?95040b3锛夊嵆瀛樺湪锛氬綋鏃舵棤 JDK 宸ュ叿閾俱€佽 Java 娴嬭瘯浠庢湭杩愯锛堣鏈褰曚笂涓€鍙ャ€屽緟 B01 宸ュ叿閾
惧楠屻€嶏級锛屼笖闂ㄧ G6 涓?Node 鐗?`verify-module-whitelist.mjs`銆丟11 `--mvn` 浠呰窇 common/
infra锛屽潎涓嶈Е鍙?zszj-server Java 娴嬭瘯锛屾晠闀挎湡鏈毚闇层€備慨澶嶏細鏂█鏀逛负涓庡濡规祴璇?`enabledModules
MatchRootPomActiveModules` 涓€鑷寸殑鍓ュ墠缂€姣斿锛坄assertEquals(new LinkedHashSet<>(Modu
leWhitelist.ENABLED_MODULES), activeModuleArtifacts(read("pom.xml")))`锛夈€備慨澶嶅悗 
zszj-server `ModuleWhitelistTest` 4/4銆佽繛鍚?`ApiInventoryTest` 4/4 鍏?8/8 BUILD SU
CCESS銆傛涓?ENG-001 鏄惧紡鎺ㄨ繜涔嬪伐鍏烽摼澶嶉獙鐨勫饱琛岋紙Java 鐧藉悕鍗曟祴璇曠‘鍙繍琛屻€佸繀瑕佹ā鍧椾緷璧栭棴鍖呬笌鐧藉悕鍗曚竴
鑷达級锛岄潪鏂板闇€姹傦紱ENG-001 涓诲崱鐘舵€佺淮鎸?寰呴獙鏀躲€?
### ZS-ENG-002锛氱粺涓€ JDK 17 鏋勫缓涓庤繍琛屽熀绾?
- 鍏宠仈锛欴-02銆丗ND-ARCH-001锛沇P-02/20锛汢01銆備紭鍏堢骇 P0锛涚被鍒?鏀归€狅紱鐘舵€?寰呴獙鏀讹紱鍓嶇疆 B00銆?- 浼楀
瑕佹眰涓庣幇鐘讹細[鏍?POM][E01] 缂栬瘧鐩爣涓?17锛孾Dockerfile][E03] 浣跨敤 21-jre锛涜繖鏄熀绾夸笉涓€鑷达紝涓嶇瓑浜
庡凡缁忓彂鐢熻繍琛屾晠闅溿€傛湰鏈哄綋鍓?PATH 鏈彂鐜?java/mvn锛屼笉鎹鏂█杞欢鏈畨瑁呫€?- 璋冩暣锛氭牳楠屽苟璁板綍宸ュ叿閾撅紝缁
熶竴 Maven 缂栬瘧銆佹祴璇曘€佸惎鍔ㄨ鏄庡拰闀滃儚鐨?JDK 17 鍩虹嚎锛涜褰?Maven 鐗堟湰鍙婃瀯寤哄叆鍙ｃ€傝嫢瑕佹敼杩愯鐗堟湰锛屽簲鍏
堝悓姝?D-02锛屼笉鑳介粯璁ゅ崌绾с€?- 楠屾敹锛氬共鍑€鐜鍙寜璇存槑瀹屾垚鏋勫缓鍜屾渶灏忓簲鐢ㄥ惎鍔紱璁板綍瀹為檯 Java/Maven 鐗堟湰
锛涘伐鍏烽摼涓嶆弧瓒宠姹傛椂鏄庣‘澶辫触锛屼笉鑳戒緷璧栨満鍣ㄤ笂鐨勯殣寮忕増鏈€?- 寮€鍙戣褰曪紙2026-09-09锛夛細鏍?POM 鏂板 mav
en-enforcer-plugin锛坮equireJavaVersion [17,18) + requireMavenVersion [3.8,)锛夛紝宸ュ
叿閾句笉婊¤冻鏃舵瀯寤烘槑纭け璐ワ紱[Dockerfile](../services/zhongshu-core/zszj-server/Dockerfil
e) 21-jre鈫?7-jre 瀵归綈 D-02 缁存姢绾匡紱[鏈嶅姟绔?README](../services/zhongshu-core/README.
md) 鐧昏宸ュ叿閾惧熀绾夸笌鏋勫缓鍏ュ彛銆傚伐鍏烽摼鏍搁獙琛ュ綍锛?026-09-09 鏅氾級锛氫互浠撳簱 tools/ 鐩綍鑷紩瀵?Temurin 
JDK 17.0.20.1 + Maven 3.9.9锛坋nforcer 鏍￠獙閫氳繃锛宼ools/ 涓嶅叆搴擄級锛宮vn -pl zszj-server -
am package 鍏ㄩ噺鏋勫缓閫氳繃骞朵骇鍑?zszj-server.jar锛?70MB锛夆€斺€擹S-ENG-001 鐧藉悕鍗曟祴璇曠紪璇戙€乑S-CF
G-003.A/GOV 绛夋柊浠ｇ爜涓€骞堕獙璇併€傚緟楠屾敹璇存槑锛氳法鏈哄櫒鍙噸澶嶅畨瑁呴獙璇佷粛鎸?B01 鎵规鎵ц銆?
### ZS-ENG-003锛氬垎绂讳紬澧呯幆澧冮厤缃笌涓婃父婕旂ず閰嶇疆

- 鍏宠仈锛欶ND-ARCH-002銆丗ND-DB-001/002銆丗ND-INF-004锛沇P-02/18/19锛汢01銆備紭鍏堢骇 P0锛涚被鍒?鏀归€狅
紱鐘舵€?寰呴獙鏀讹紱鍓嶇疆 B00銆?- 鍗忎綔杈圭晫锛氬悜 ZS-DB-001.A 鎻愪緵鐜閰嶇疆鍚堝悓锛岀敱璇ュ瓙椤硅仈鍚堥獙璇侀┍鍔?杩炴帴鍙傛暟
锛涙湰椤逛笉绛夊緟 ZS-DB-001.A 鎴?.B 鐨勫畬鎴愶紝鐪熷疄涓氬姟搴撹繛鎺ヨ仈楠屽湪 B02銆?- 浼楀瑕佹眰涓庣幇鐘讹細[涓婚厤缃甝[E05]
 榛樿婵€娲?local锛沎local 閰嶇疆][E06] 鍚紨绀烘暟鎹簮銆佷换鍔″拰瀹夊叏璁剧疆銆備繚鐣欑幆澧冨彉閲忓崰浣嶇涓嶇瓑浜庤繍琛屽弬鏁板凡
瀹屾暣楠岃瘉銆?- 璋冩暣锛氬缓绔嬬嫭绔嬬殑寮€鍙戙€佹祴璇曘€侀儴缃查厤缃悎鍚岋紱鏁版嵁搴撱€丷edis銆佸瓨鍌ㄥ湴鍧€涓庡嚟鎹寜鐜娉ㄥ叆锛涙牳瀵硅
秴鏃躲€佽繛鎺ユ睜鍜屽惎鍔ㄥ繀濉」锛涚姝㈠皢鐪熷疄绉樺瘑鍐欏叆鍙楃増鏈帶鍒剁殑妯℃澘銆侾G 杩炴帴閫傞厤褰?ZS-DB-001锛岄伩鍏嶉噸澶嶄换鍔°€?
- 楠屾敹锛氬悇閰嶇疆缁勫悎鐨勬湁鏁堝€肩鍚堢幆澧冨悎鍚岋紱缂哄け蹇呴渶椤圭粰鍑鸿劚鏁忛敊璇笖涓嶉潤榛樺洖閫€婕旂ず鏈嶅姟锛涢厤缃?鏃ュ織鎵弿鏃犵湡瀹炵
瀵嗐€傚彧浜や粯閮ㄧ讲妯℃澘锛屼笉鎹瀹ｅ竷鐢熶骇閮ㄧ讲瀹屾垚銆?- 寮€鍙戣褰曪紙2026-09-09锛夛細鏂板 [鐜閰嶇疆鍚堝悓](../se
rvices/zhongshu-core/script/config/README.md) 涓?[閮ㄧ讲妯℃澘 application-prod.yaml](
../services/zhongshu-core/script/config/application-prod.yaml)锛堝叏閮ㄧ粡 ZSZJ_* 鐜
鍙橀噺娉ㄥ叆锛屽繀濉」鐢ㄦ棤榛樿鍊煎崰浣嶇瀹炵幇鑴辨晱澶辫触璇箟锛夈€俵ocal/dev 婕旂ず閰嶇疆涓€у寲锛氫笂娓稿井淇?閽夐拤/浼佸井娴嬭瘯鍙
?app-id 涓?client-id 鏀圭幆澧冩敞鍏ワ紝鏀粯鍥炶皟婕旂ず鍩熸敼鍗犱綅绗︼紝**娓呴櫎娉ㄩ噴涓畫鐣欑殑 3 涓笂娓告紨绀?secret*
*锛涙湭鍚敤妯″潡鐨?Mapper 鏃ュ織閰嶇疆鎸夌櫧鍚嶅崟瑁佸壀銆傚緟楠屾敹璇存槑锛氱己蹇呴渶椤瑰け璐ヨ涔変笌绉樺瘑鎵弿鐨勬満鍣ㄥ寲闂ㄧ褰?ZS-C
FG-001.A锛汳ySQL 鏁版嵁婧愪笌妯℃嫙浠庡簱娓呯悊褰?ZS-DB-001.A锛涙湁鏁堝€肩粍鍚堥獙璇佸緟 B02銆?
### ZS-ENG-004锛氬叧闂粯璁ゆā鎷熻璇佸拰璇锋眰鍘熸枃璋冭瘯鍏ュ彛

- 鍏宠仈锛欶ND-AUTH-001/002/007銆丗ND-ARCH-002锛沇P-02/11/18锛汢01锛孊03 娣卞叆澶嶆牳銆備紭鍏堢骇 P0锛涚被鍒
?鏀归€狅紱鐘舵€?寰呴獙鏀讹紱鍓嶇疆 ZS-ENG-003銆?- 浼楀瑕佹眰涓庣幇鐘讹細鏈嶅姟绔繀椤婚獙璇佷富浣撳拰鏉冮檺銆俒local 閰嶇疆][E0
6] 鐨?mock-enable 涓?true锛沎DefaultController][E04] 鐨勫尶鍚?/test 鎵撳嵃 query銆乭eader銆乥o
dy锛屽彲鑳借褰曞嚟鎹拰鏁忔劅鍐呭锛涘皻鏈瘉鏄庤閰嶇疆宸茶閮ㄧ讲浣跨敤銆?- 璋冩暣锛氬叡浜幆澧冨強閮ㄧ讲榛樿绂佺敤妯℃嫙璁よ瘉锛涜皟璇曠鐐
圭Щ闄ゆ垨闄愬埗鍦ㄦ樉寮忔祴璇曠幆澧冿紝鏃ュ織鑴辨晱锛涙帓鏌ュ悓绫绘梺璺€傛寮忚璇侀摼鐨勮繘涓€姝ラ棶棰樺湪 M03/M04 澶嶇敤鎴栨墿灞曟浠诲姟銆
?- 楠屾敹锛氭棤鍑嵁銆佷吉閫犳祴璇曡韩浠戒笉鑳介€氳繃鍙椾繚鎶ゆ帴鍙ｏ紱閮ㄧ讲閰嶇疆鏃犳硶寮€鍚湭缁忓厑璁哥殑鏃佽矾锛?test 涓嶅彲琚尶鍚嶇敤浜庤
褰曡姹傚師鏂囷紱姝ｅ父鐧诲綍鍥炲綊閫氳繃銆?- M03 琛ュ厖锛?026-09-08锛夛細[TokenAuthenticationFilter][E30]
 鍦ㄦ甯?Token 鏈瀯閫犲嚭涓讳綋鏃朵細灏濊瘯 mockLoginUser锛涙ā鎷熶富浣撶洿鎺ュ寘鍚姹備腑鐨勭鎴枫€傝ˉ鍏呰鐩栨甯告牎楠屽け
璐ュ悗涓嶅緱闄嶇骇涓烘ā鎷熶富浣擄紝澶嶇敤 ZS-SEC-012 鐨勫叏閾炬祴璇曪紱涓嶅彟寤洪噸澶嶄换鍔°€?- 寮€鍙戣褰曪紙2026-09-09锛夛細[
DefaultController](../services/zhongshu-core/zszj-server/src/main/java/cn/zszj/
server/controller/DefaultController.java) 鐨勫尶鍚?/test 绔偣锛堟墦鍗拌姹傚師鏂囷級鏁存绉婚櫎锛沴oca
l 鐨?`mock-enable` 鐢?true 鏀逛负鐜鍙橀噺娉ㄥ叆涓旈粯璁?false锛坄ZSZJ_APPLICATION_LOCAL_SECURIT
Y_MOCK_ENABLE:false`锛夛紝妗嗘灦 SecurityProperties 榛樿鍊兼湰涓?false锛沎閮ㄧ讲妯℃澘](../service
s/zhongshu-core/script/config/application-prod.yaml) 鏄惧紡澹版槑 `mock-enable: false
` 闃茶閰嶃€傛牳瀵癸細mockLoginUser 鍒嗘敮鐢?mockEnable 闂ㄦ帶锛屽叧闂悗浼€?token 鏃犳硶鏋勯€犱富浣擄紱鍏ㄤ粨鏃犲叾
浠?/test 鏀捐寮曠敤銆傚緟楠屾敹璇存槑锛?姝ｅ父鏍￠獙澶辫触鍚庝笉寰楅檷绾т负妯℃嫙涓讳綋"鍏ㄩ摼娴嬭瘯褰?ZS-SEC-012锛圔03锛夛紱姝ｅ父鐧
诲綍鍥炲綊寰?B01/B03 鐜銆?
### ZS-ENG-005锛氭敹绱х鐞嗙鐐规毚闇插苟淇濈暀鏈€灏忓仴搴锋鏌?
- 鍏宠仈锛欶ND-AUTH-002銆丗ND-ARCH-002锛沇P-19/20锛汢01/B03銆備紭鍏堢骇 P0锛涚被鍒?鏀归€狅紱鐘舵€?寰呴獙鏀讹紱鍓嶇
疆 ZS-ENG-003銆?- 浼楀瑕佹眰涓庣幇鐘讹細[Infra Security 閰嶇疆][E07] 瀵?actuator銆丏ruid 鍜屾帴鍙ｆ枃妗ｈ
矾寰?permitAll锛宭ocal/dev 杩樺紑鏀炬墍鏈?actuator 绔偣锛涜矾寰勬斁琛屼笉绛変簬姣忎釜绔偣閮藉凡娉ㄥ唽锛屼絾瀛樺湪闇€鏀剁揣鐨
勯粯璁ら厤缃€?- 璋冩暣锛氭寜鐜纭畾绔偣鐧藉悕鍗曘€佽闂璇佸拰缃戠粶杈圭晫锛岄粯璁や粎淇濈暀蹇呰涓斾笉娉勯湶璇︽儏鐨勫仴搴锋鏌ワ紱绠＄悊
鏉冮檺涓庝笟鍔℃潈闄愬垎寮€銆傛枃浠惰鍙栫殑瀵硅薄鎺堟潈鍦?M08 鍗曠嫭鍒嗘瀽銆?- 楠屾敹锛氬尶鍚嶇敤鎴蜂笉鑳借鍙栨晱鎰熻繍琛屻€侀厤缃垨鐩戞帶淇℃
伅锛涙巿鏉冭繍缁翠富浣撹兘璁块棶鎵瑰噯绔偣锛涘仴搴锋帰閽堜粛鍙敤锛涘叧闂鐐逛笉鑳介€氳繃鍙︿竴鏉＄鐞嗚矾寰勭粫杩囥€?- 寮€鍙戣褰曪紙2026-
09-09锛夛細[Infra SecurityConfiguration](../services/zhongshu-core/zszj-module-inf
ra/src/main/java/cn/zszj/module/infra/framework/security/config/SecurityConfigu
ration.java) 鏀逛负浠?`/actuator/health` 鍖垮悕鏀捐锛堝叾浣?actuator 绔偣闇€璁よ瘉锛夈€佸垹闄?`/druid
/**` 鍖垮悕鏀捐锛沴ocal 鐨?Druid 鎺у埗鍙?`stat-view-servlet` 鐢卞尶鍚嶅紑鏀撅紙涓旀湭璁捐处瀵嗭級鏀逛负榛樿鍏抽棴锛
沘ctuator 鏆撮湶鐧藉悕鍗?local=`health,info,metrics`銆乨ev=health銆佸熀纭€閰嶇疆鏂板榛樿 `health`銆
侀儴缃叉ā鏉?health锛泂pringdoc/鎺ュ彛鏂囨。榛樿鍏抽棴銆佷粎 local 鏄惧紡寮€鍚€傛枃浠惰鍙栧尶鍚嶈矾寰勪繚鐣欏苟鏍囨敞褰?ZS-
FILE-001.A銆傛帰閽堝疄娴嬶紙2026-09-09 鏅氾級锛氬簲鐢ㄧ湡瀹炲惎鍔ㄥ悗 /actuator/health 杩斿洖 {"status":"U
P"}锛堟鍓?jar 鏃?actuator 渚濊禆瀵艰嚧 404锛屽凡琛?spring-boot-starter-actuator锛夛紱鍖垮悕璁块棶鍙嶈瘉涓
庢巿鏉冭繍缁磋闂緟 B03銆傜綉缁滆竟鐣岋紙鎺㈤拡绔彛闅旂锛夊綊閮ㄧ讲浠诲姟銆?- M03 琛ュ厖锛?026-09-08锛夛細鍖垮悕瑙勫垯鐢辨敞瑙ｃ€
侀厤缃拰鍚勬ā鍧楄嚜瀹氫箟瑙勫垯鍏卞悓缁勬垚锛岄渶鎸夋渶缁?HTTP 鏂规硶涓庤矾寰勯獙璇侊紱绠＄悊绔偣缁х画褰掓湰椤癸紝鍏ㄩ儴涓氬姟鍖垮悕鎺ュ彛鐩樼偣褰
?ZS-SEC-002銆?
### ZS-ENG-006锛氭槑纭?Quartz 涓?Spring 瀹氭椂浠诲姟鐨勫惎鍋滆竟鐣?
- 鍏宠仈锛欶ND-ARCH-002銆丗ND-INF-003/005锛沇P-14/18锛汢01锛孊05 鎵╁睍銆備紭鍏堢骇 P0锛涚被鍒?鏀归€狅紱鐘舵€?寰
呴獙鏀讹紱鍓嶇疆 ZS-ENG-001銆乑S-ENG-003銆?- 浼楀瑕佹眰涓庣幇鐘讹細鏈惎鐢ㄨ兘鍔涗笉寰楁殫涓繍琛屼换鍔°€俵ocal 鎺掗櫎 Qu
artzAutoConfiguration锛屼絾 [YudaoQuartzAutoConfiguration][E08] 浠嶅惎鐢?Spring Schedu
ling锛屼笉鑳芥妸鈥淨uartz 鍏抽棴鈥濆綋鎴愨€滄墍鏈夊畾鏃朵换鍔″叧闂€濄€?- 璋冩暣锛氱洏鐐?Quartz Job銆丼pring Schedul
ed 鍜屾潯浠跺紡鍚庡彴浠诲姟锛岃褰曠敤閫斻€佸紑鍏炽€佺幆澧冮粯璁ゅ€煎拰鏁版嵁鍓綔鐢紱缁欏繀瑕佷换鍔¤缃槑纭惎鍋滆竟鐣岋紝涓嶈姹備竴鍒€鍒囧
叧闂紦瀛樼瓑蹇呰鍐呴儴璋冨害銆?- 楠屾敹锛氬叧闂殑浠诲姟涓嶆墽琛屻€佷笉娓呯悊鏁版嵁锛涜幏鍑嗙殑鍐呴儴浠诲姟鍜?Quartz 鍒嗗埆楠岃瘉锛涙湭鍚敤
涓氬姟妯″潡鏃犲悗鍙板壇浣滅敤銆俀uartz 鏁版嵁搴撹兘鍔涘綊 ZS-DB-016銆?- 寮€鍙戣褰曪紙2026-09-09锛夛細浜や粯 [瀹氭椂浠诲姟鍚
仠鐩樼偣](../services/zhongshu-core/docs/瀹氭椂浠诲姟鍚仠鐩樼偣.md)锛氳皟搴﹀櫒涓ゅ眰锛圦uartz/Spring @
EnableScheduling锛? 涓氬姟 JobHandler 3 涓紙infra 鏃ュ織娓呯悊锛岄渶 Quartz+infra_job 鐧昏鎵嶆墽琛
岋級+ 妗嗘灦鍐呴儴缁存姢璋冨害 2 涓紙Redis Stream 閲嶅彂/娓呯悊锛孈ConditionalOnBean 鏉′欢瑁呴厤涓斿綋鍓嶆棤 Stre
am 鐩戝惉鍣ㄤ笉瑁呴厤锛? 鏈惎鐢ㄦā鍧椾换鍔′笉鍦ㄤ緷璧栭棴鍖呫€備慨姝?local 鐭涚浘閰嶇疆锛歚auto-startup: true`锛堟敞閲婂嵈
瑕佹眰灏介噺涓嶅紑锛夋敼涓洪粯璁?false 鍙幆澧冨彉閲忔樉寮忓紑鍚紝涓?QuartzAutoConfiguration 鎺掗櫎椤逛繚鎸佷竴鑷达紱閮ㄧ讲
妯℃澘榛樿 false銆傚緟楠屾敹璇存槑锛氬紑鍚?鍏抽棴涓ゆ€佺殑杩愯楠岃瘉寰?B01/B05锛決uartz 鏁版嵁搴撹兘鍔涘綊 ZS-DB-016銆?
## 4. M02锛氭暟鎹簱涓庢寔涔呭寲

### ZS-DB-001锛氬畬鎴?PostgreSQL 椹卞姩涓庡敮涓€杩愯鏁版嵁婧愭帴鍏?
- 鍏宠仈锛欶ND-DB-001锛沇P-03锛汢01/B02锛堟寜瀛愰」鐙珛鏀捐锛夈€備紭鍏堢骇 P0锛涚被鍒?鏀归€狅紱鐘舵€?寮€鍙戜腑锛涘墠缃?鎸夌
 16.1 鑺傚悇瀛愰」锛岀姝㈠皢鍚庣疆瀛愰」浣滀负鏃╂湡鎵规鍓嶇疆銆?- 浼楀瑕佹眰涓庣幇鐘讹細D-08 绂佹 MySQL 杩囨浮鍩虹嚎銆俒MyB
atis POM][E09] 鐨?PG 椹卞姩涓?optional锛涗富宸ョ▼閰嶇疆浠嶆湁 MySQL master/slave锛屽皻鏈獙璇佹渶缁堣繍琛屽寘
鍖呭惈 PG 椹卞姩銆?- 璋冩暣锛氬簲鐢ㄦ樉寮忎緷璧?PG 椹卞姩骞舵牳楠屾墦鍖咃紱鎵€鏈夊彲鎵ц閰嶇疆浣跨敤 PG锛屾竻闄ゆ垨绂佺敤妯℃嫙 MySQL 
浠庡簱骞舵牳瀵瑰紩鐢紱杩炴帴妫€鏌ヤ娇鐢?SELECT 1銆備紭鍏堥噰鐢ㄥ崟涓诲簱鏄妧鏈缓璁紝涓嶆彁鍓嶅疄鐜拌鍐欏垎绂汇€?- 楠屾敹锛氬仠姝?涓嶅
畨瑁?MySQL 浠嶅彲瀹屾垚鐩爣鍚姩涓庢祴璇曪紱妫€鏌ヤ緷璧栨爲銆侀┍鍔ㄥ姞杞姐€佹湁鏁堟暟鎹簮鍜岃繛鎺ユ睜鍋ュ悍妫€鏌ワ紱涓嶅彲鐢?PG 蹇呴』鏄庣
‘澶辫触锛屼笉鑳介€€鍥?MySQL銆?- 鍒嗘壒浜や粯锛氳绗?16.1 鑺?ZS-DB-001.A銆乑S-DB-001.B锛涘瓙椤瑰垎鍒櫥璁拌瘉鎹紝鍏
ㄩ儴閫傜敤瀛愰」楠屾敹鍚庢墠鑳藉皢鏈富浠诲姟鏍囦负宸查獙鏀躲€?- 寮€鍙戣褰曪紙2026-09-09锛孼S-DB-001.A锛夛細[MyBatis st
arter POM](../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-my
batis/pom.xml) 鐨?mysql-connector-j 鏀逛负 optional锛堝悇鏂硅█椹卞姩涓€鑷达紝涓嶈繘鍏ヨ繍琛屽寘锛夛紱[zszj-
server POM](../services/zhongshu-core/zszj-server/pom.xml) 鏄惧紡寮曞叆 `org.postgres
ql:postgresql`锛坮untime銆侀潪 optional锛夛紱local/dev 鏁版嵁婧愭敼鍐欎负 PG 鍗曚富搴擄紙master 璧?ZSZJ
_DATASOURCE_* 鐜娉ㄥ叆锛屾ā鎷熶粠搴撲笌澶氭柟瑷€娉ㄩ噴娓呴櫎锛夛紝validation-query 鏀?PG 鏂硅█ SELECT 1锛沝
ev 鐨勪笂娓告紨绀?Redis 鍦板潃涓?Druid 鎺у埗鍙板尶鍚嶅紑鏀句竴骞舵竻鐞嗭紙ENG-005 琛ュ厖锛夈€傛柊澧為潤鎬佹鏌?`node scr
ipts/db/verify-datasource-pg.mjs`锛? 闂锛夈€?- 寮€鍙戣褰曪紙2026-09-09 鏅氾紝ZS-DB-001.B 
鍓嶇疆璇佹嵁鎻愬墠鑾峰緱锛夛細宸ュ叿閾捐嚜寮曞鍚庣湡瀹炴墽琛?`mvn dependency:tree` 涓?`mvn package`鈥斺€斾緷璧栨爲鍚?
postgresql 42.7.11(runtime)/flyway-core+pg 11.7.2銆?*鏃?mysql-connector**锛涙墦鍖呬骇鐗?
zszj-server.jar(170MB) 鍐呭惈 PG/Flyway 椹卞姩銆佹棤 MySQL 椹卞姩锛涘簲鐢ㄤ互鐪熷疄 PG17+Redis 鍚姩鎴愬
姛锛?8081 绔彛锛夛紝Druid PSCache 鍏煎闂瀹炴祴鍙戠幇骞跺叧闂?pool-prepared-statements銆傚緟楠屾敹璇存槑锛
?B 鍓╀綑椤癸紙浣庢潈闄愬弻璐﹀彿鑱旈獙銆佽縼绉昏处鍙峰垎绂昏繍琛岋級寰呮寮?B02 鐜涓庤礋璐ｄ汉鎺堟潈銆?
### ZS-DB-002锛氬浐瀹?PG 鐜骞舵媶鍒嗚縼绉讳笌杩愯鏉冮檺

- 鍏宠仈锛欶ND-DB-002/003/004銆丳END-004锛沇P-03/19锛汢02銆備紭鍏堢骇 P0锛涚被鍒?琛ュ缓锛涚姸鎬?寮€鍙戜腑锛涘墠缃?Z
S-ENG-003锛岀幆澧冧綅缃笌鍒涘缓鏉冮檺闇€鍏堟牳楠屻€?- 浼楀瑕佹眰涓庣幇鐘讹細椤圭洰瑕佹眰鐙珛鏁版嵁搴撱€佷綆鏉冮檺璐﹀彿锛屼紭鍏?PG 1
7銆佸彲鎺ュ彈 16锛涙牳鏌ュ叆鍙ｄ负 [杩愯鏁版嵁婧愰厤缃甝[E06] 涓?[琛ㄥ拰搴忓垪鑴氭湰][E10]锛屼絾鏂囦欢鍐呭涓嶈兘璇佹槑鏈嶅姟鍣ㄥ疄闄呯
姸鎬侊紝鏈疆鏈繛鎺ユ湇鍔″櫒鏍搁獙鐗堟湰銆佹暟鎹簱鎴栨巿鏉冦€?- 璋冩暣锛氭牳楠屽疄闄呯増鏈拰闅旂杈圭晫锛岃褰曟椂鍖恒€佺紪鐮併€佹墿灞曚笌鐜
鐢ㄩ€旓紱寤虹珛鍙鏌ョ殑寤哄簱/鎺堟潈鎿嶄綔鏂规鍜岀嫭绔嬫祴璇曠幆澧冿紝鍒嗙杩佺Щ璐﹀彿涓庡簲鐢ㄨ处鍙凤紝瑕嗙洊琛ㄥ拰搴忓垪鏉冮檺銆?- 楠屾敹锛氬
簲鐢ㄨ处鍙峰彲鎵ц涓氬姟 CRUD/鍙栧簭鍒楀€硷紝浣嗕笉鑳藉缓搴撱€佷慨鏀?schema 鎴栬闂叾浠栭」鐩暟鎹紱鍚庣画杩佺Щ鏂板缓瀵硅薄鐨勬巿鏉冧
粛姝ｇ‘锛涙姤鍛婁笉鍚瘑鐮併€傜湡瀹炵幆澧冨垱寤哄姩浣滈』鍦ㄦ槑纭洰鏍囧拰鎺堟潈鍚庢墽琛屻€?- 寮€鍙戣褰曪紙2026-09-09锛夛細浜や粯 [鏁版
嵁搴撶幆澧冩柟妗圿(../services/zhongshu-core/docs/鏁版嵁搴撶幆澧冩柟妗?md)鈥斺€斿疄渚?搴撹鏍硷紙PG17銆乁TF8銆丄
sia/Shanghai锛夈€佽縼绉讳笌搴旂敤璐﹀彿鍒嗙鍙婃巿鏉?SQL 妯℃澘锛坉efault privileges 瑕嗙洊杩佺Щ鏂板缓瀵硅薄锛夈€佸垵濮
嬪寲椤哄簭锛團lyway V1鈫掕ˉ鎺堚啋app 鍑嵁杩愯鈫抜nit-admin锛夈€佸浠?鎭㈠瑙勭▼妯℃澘銆侀殧绂昏竟鐣岋紱PEND-004 鍥炲～
涓烘柟妗堝凡澶囷紝8 椤规牳楠屾竻鍗曪紙鐗堟湰/鏃跺尯/缂栫爜/鎵╁睍/浣嶇疆/澶囦唤/璧勬簮/鍛藉悕锛夐殢鏂规浜や粯锛屽緟鐜鎺堟潈鍚庢墽琛屽苟鍥炲～銆
傛湰鍦扮嫭绔嬫祴璇曠幆澧冨疄娴嬶紙2026-09-09 鏅氾紝涓€娆℃€?PG17 瀹瑰櫒锛岄獙鍚庢竻鐞嗭級锛氭寜鏂规鎵ц寤哄簱/瑙掕壊/鎺堟潈 鈫?杩佺
Щ璐﹀彿(owner) 鍚姩 Flyway 寤鸿〃 61 寮?鈫?搴旂敤璐﹀彿(app) 浣庢潈闄愯繍琛?health UP銆佷笟鍔℃煡璇㈡甯革紱app 
璐﹀彿 CREATE TABLE 琚嫆锛坧ermission denied锛夈€乨efault privileges 浣?owner 鍚庡缓瀵硅薄鍙 a
pp 璇诲彇锛涢敊璇彛浠ゅ惎鍔ㄤ互闈為浂閫€鍑哄苟鏄庣ず PG 璁よ瘉澶辫触銆佸叏鏂囨棤 MySQL 鍥為€€銆傚緟楠屾敹璇存槑锛氱湡瀹炵幆澧冩牳楠屼笌鎺堟
潈鎵ц寰?B02 鐜涓庤礋璐ｄ汉纭锛屾湰杞湭杩炴帴浠讳綍鏈嶅姟鍣ㄣ€?
### ZS-DB-003锛氱‘瀹氬苟鎺ュ叆鍗曚竴鐗堟湰杩佺Щ鏈哄埗

- 鍏宠仈锛欶ND-DB-005銆丳END-005锛沇P-04锛汢02銆備紭鍏堢骇 P0锛涚被鍒?琛ュ缓锛涚姸鎬?寰呴獙鏀讹紱鍓嶇疆 ZS-DB-001.A銆
乑S-DB-002锛屽疄鏂藉墠瀹屾垚鎶€鏈€夊瀷銆?- 浼楀瑕佹眰涓庣幇鐘讹細蹇呴』浠庣涓€鐗堣褰曟暟鎹簱鍙樺寲锛涗富宸ョ▼鏈帴鍏?Flyway/
Liquibase銆備緵浣撳凡鏈?Flyway锛屼絾鍏?[PG 閰嶇疆][E20] 涓?[杩佺Щ璇存槑][E21] 鐨?out-of-order 璁剧疆涓嶄竴
鑷达紝涓斾笉鑳戒唬鏇垮畬鏁村簳搴у垵濮嬪寲銆?- 璋冩暣锛氬缓璁鐢?Flyway 璺嚎锛涘舰鎴愬苟鍥炲～ PEND-005 鎶€鏈粨璁猴紝鍥哄畾渚濊禆
銆佽縼绉荤洰褰曘€佸叏灞€鐗堟湰椤哄簭銆佸巻鍙茶〃銆佹墽琛屽叆鍙ｅ拰鐜淇濇姢锛涗笉寰楁暣浣撳鍒朵緵浣撶殑璁捐涓氬姟杩佺Щ銆?- 楠屾敹锛氬彧鏈変竴涓
潈濞佽縼绉诲叆鍙ｏ紱寰呮墽琛岀増鏈彲瀹℃煡锛涘凡鎵ц鑴氭湰绡℃敼/鏍￠獙澶辫触浼氶樆姝㈡斁琛岋紱閲嶈窇鏃犲壇浣滅敤锛涘簲鐢ㄦ甯稿惎鍔ㄦ棤闇€楂樻潈闄愯縼
绉诲嚟鎹紱娴嬭瘯 clean 鍔熻兘涓嶈兘浣滅敤浜庨潪涓存椂搴撱€?- 寮€鍙戣褰曪紙2026-09-09锛夛細PEND-005 宸插洖濉紙02 鍙拌处
锛欶lyway 闅?BOM + flyway-database-postgresql銆佸簲鐢ㄥ惎鍔ㄥ敮涓€鏉冨▉鍏ュ彛銆佸崟涓€鐩綍銆佺増鏈彿 V<鏃ユ湡
>.<搴忓彿> 鍏ㄥ眬鍞竴銆乷ut-of-order=false 瑙ｅ喅渚涗綋涓嶄竴鑷淬€乧lean 鍏ㄧ幆澧冪鐢級銆傝惤鍦帮細[zszj-server
 POM](../services/zhongshu-core/zszj-server/pom.xml) 寮曞叆 flyway 渚濊禆锛堢増鏈殢 BOM锛夛
紱[鏁版嵁搴撹縼绉昏鑼僝(../services/zhongshu-core/docs/鏁版嵁搴撹縼绉昏鑼?md) 鍐荤粨鍛藉悕/鍥炴粴/鐜淇濇姢锛涘
熀纭€閰嶇疆涓嶽閮ㄧ讲妯℃澘](../services/zhongshu-core/script/config/application-prod.yaml)鍐
欏叆鍚堝悓锛堥粯璁ゅ叧闂紝缁?ZSZJ_FLYWAY_ENABLED 婵€娲伙紝鍑嵁鍙敞鍏ヤ笓鐢ㄨ縼绉昏处鍙凤級锛涙柊澧為潤鎬佹鏌?`node scr
ipts/db/verify-flyway-migrations.mjs`锛? 闂锛夈€備笌渚涗綋宸紓锛氫笉澶嶅埗鍏舵寜涓氬姟鍖呭垎鍖呯殑澶?locat
ion 涓?undo 鐩綍璁捐锛岄噰鐢ㄥ崟涓€ location + 鏇翠弗鏍?out-of-order=false銆傚緟楠屾敹璇存槑锛氱湡瀹炲簱鐨勭┖搴
撴墽琛?baseline 鍚告敹/绡℃敼鎷︽埅/閲嶈窇骞傜瓑楠屾敹鍦?B02锛沄1 鍩虹嚎杩佺Щ鍐呭褰?ZS-DB-004銆?
### ZS-DB-004锛氭妸搴曞骇寤鸿〃涓庡繀瑕佺瀛愭暟鎹暣鐞嗘垚杩佺Щ鍩虹嚎

- 鍏宠仈锛欶ND-DB-005/006銆丗ND-SYS-001銆丗ND-INF-005锛沇P-04锛汢02銆備紭鍏堢骇 P0锛涚被鍒?鏀归€狅紱鐘舵€?寰呴
獙鏀讹紱鍓嶇疆 ZS-DB-003銆乑S-ENG-001銆?- 浼楀瑕佹眰涓庣幇鐘讹細[PG 涓昏剼鏈琞[E10] 鏈?49 寮犺〃銆?8 涓簭鍒椼€?9
 鏉?DROP TABLE锛涘寘鍚?32 寮?System銆?1 寮?Infra銆? 寮犵ず渚嬭〃鍜?dual銆俒Quartz 鑴氭湰][E11] 鍙︽湁 1
1 寮犺皟搴﹁〃銆備笉鑳藉弽澶嶆墽琛屽師鑴氭湰鍗囩骇宸叉湁搴撱€?- 璋冩暣锛氭暣鐞嗗熀纭€ DDL銆佺储寮曘€佸簭鍒椼€佸繀瑕佸瓧鍏?鑿滃崟/瑙掕壊绉嶅瓙锛
涙紨绀烘暟鎹笌姝ｅ紡鍩虹嚎鍒嗙锛屾牳瀵规紨绀鸿〃鏄惁浠嶈榛樿妯″潡寮曠敤鍐嶅喅瀹氭帓闄わ紱鍒濆绠＄悊鍛橀€氳繃瀹夊叏鍒濆鍖栨祦绋嬪缓绔嬶紝涓嶅
鐢ㄦ紨绀哄嚟鎹€備繚鐣欎笂娓?SQL 鏉ユ簮锛屼笉鍙︾淮鎶ゅ彲鎵ц鐨勭浜屽鍒濆鍖栥€?- 楠屾敹锛氫袱浠界嫭绔嬫柊寤轰复鏃?PG 搴撳緱鍒颁竴鑷寸粨
鏋勫拰蹇呰鏁版嵁锛涘凡杩佺Щ搴撻噸璺戜笉鍒犳暟鎹€佷笉閲嶇疆璐﹀彿锛涘叧闂ā鍧楁棤鍙搷浣滆彍鍗曪紱涓婚敭搴忓垪鍚庣画鎻掑叆涓嶅啿绐併€?- 寮€鍙戣
褰曪紙2026-09-09锛夛細浜や粯 V1 鍩虹嚎杩佺Щ [V20260909.001__system_infra_baseline.sql](../ser
vices/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260909.001__
system_infra_baseline.sql)锛岀敱纭畾鎬ф瀯寤鸿剼鏈?[build-baseline-migration.mjs](../scrip
ts/db/build-baseline-migration.mjs) 鐢熸垚锛堣鍒欏彲澶嶅锛夈€傚唴瀹癸細49 寮犱笟鍔¤〃 + 11 寮?Quartz
 璋冨害琛ㄥ叏閲?DDL锛?6 鏉?DROP 宸茬Щ闄わ級銆佸瓧鍏稿叏閲忥紙186 绫诲瀷/915 椤癸級銆佸惎鐢ㄦā鍧楄彍鍗曞瓙鏍?198 鏉★紙1321 
鏉′腑鎸?system/infra 鍓嶇紑+绁栧厛闂寘瑁佸壀锛夈€佽鑹?2 涓€佺鎴?1锛堜紬澧呬箣瀹讹級銆丱Auth2 default 瀹㈡埛绔€
佹棩蹇楁竻鐞?Job 3 涓紱**鐢ㄦ埛璐﹀彿涓€寰嬩笉杩佺Щ**鈥斺€斿垵濮嬬鐞嗗憳缁?[init-admin.sql.example](../serv
ices/zhongshu-core/sql/postgresql/init-admin.sql.example) 瀹夊叏鍒濆鍖栵紙鍙ｄ护鐢遍儴缃叉柟鐢熸垚
 bcrypt锛屼笉澶嶇敤婕旂ず鍑嵁锛夛紱婕旂ず鏁版嵁 137 鏉¤鍙ュ垎绂昏嚦 [demo-data-optional.sql](../services
/zhongshu-core/sql/postgresql/demo-data-optional.sql)锛堜笉闅忓簲鐢ㄦ墽琛岋級锛涙紨绀鸿〃 DDL 鍥犺
鍚敤妯″潡 infra 绀轰緥浠ｇ爜寮曠敤鑰屼繚鐣欍€傜湡瀹?PG17 鍙屽簱楠岃瘉锛堝鍣ㄩ獙鍚庢竻鐞嗭級锛歏1 搴撲笌鍘熺瀛愬簱琛?鍒?搴忓垪缁撴瀯涓
€鑷达紙宸紓鎭颁负 V1 搴斿惈鐨?11 寮?qrtz 琛級銆佸瓧鍏告暟閲忎竴鑷淬€乂1 鑿滃崟涓虹瀛愬瓙闆嗕笖鍏抽棴妯″潡鑿滃崟 0銆佹紨绀烘暟鎹粎
鍦ㄧ瀛愬簱锛涢噸鏀?V1 鍏ㄩ儴鎶?already exists 涓旀暟鎹浂鍙樺寲锛?8 搴忓垪 last_value 鈮?max(id) 鍐茬獊 0锛沬
nit-admin 娴佺▼瀹炴祴寤虹珛绠＄悊鍛樺苟缁戝畾瓒呯骇绠＄悊鍛樿鑹层€傚緟楠屾敹璇存槑锛欶lyway 鏈綋鐨?baseline 鍚告敹/绡℃敼鎷︽
埅杩愯楠岃瘉闅?B02锛堟湰鏈烘棤 JDK/Maven锛夈€?
### ZS-DB-005锛氳ˉ榻愬凡鏈夊簱鍗囩骇銆佸け璐ユ仮澶嶄笌澶囦唤鎭㈠瑙勭▼

- 鍏宠仈锛欶ND-DB-005/006锛沇P-04/19锛汢02锛岃繍琛屼繚闅滃湪 B11 澶嶆牳銆備紭鍏堢骇 P0锛涚被鍒?琛ュ缓锛涚姸鎬?寰呴獙鏀讹紱鍓
嶇疆 ZS-DB-003銆乑S-DB-004銆?- 浼楀瑕佹眰涓庣幇鐘讹細鐜版湁 [涓诲簱瀵煎嚭鑴氭湰][E10] 鍜?[Quartz 鑴氭湰][E11] 
涓嶈兘璇佹槑鏁版嵁鍙畨鍏ㄥ崌绾э紱鏈疆娌℃湁鐗堟湰鍗囩骇銆佸浠芥垨鎭㈠鎵ц璇佹嵁銆?- 璋冩暣锛氫负缁撴瀯鍙樻洿銆佺储寮曘€佺害鏉熴€佸洖濉缓绔嬫
湁搴忚縼绉伙紱濡傜‘鏈夊凡鏈夊垵濮嬪寲搴擄紝鏍稿缁撴瀯鍜屾暟鎹悗鍒跺畾鏄惧紡鎺ョ鏂规锛岀姝㈢洸鐩?baseline锛涜褰曞彲閫嗗彉鏇淬€佷笉鍙€
嗚竟鐣屻€佸墠鍚戜慨澶嶅拰鎭㈠姝ラ銆?- 楠屾敹锛氬甫鏍蜂緥鍘嗗彶鏁版嵁鐨勪复鏃跺簱鍗囩骇鍚庤褰曚繚鐣欙紱杩佺Щ涓柇涓嶄吉鎶ユ垚鍔燂紱淇/鎭㈠婕
旂粌鎴愬姛骞惰褰曟暟鎹笌鐗堟湰锛涗笉鑳界敤鍒犲簱閲嶅缓鍐掑厖鍗囩骇鎴栨壙璇烘墍鏈夊彉鏇撮兘鍙棤鎹熷洖婊氥€?- 寮€鍙戣褰曪紙2026-09-09锛夛
細浜や粯 [鏁版嵁搴撳崌绾т笌鎭㈠瑙勭▼](../services/zhongshu-core/docs/鏁版嵁搴撳崌绾т笌鎭㈠瑙勭▼.md)锛堣縼绉荤紪
鍐欒鍒?鏃㈡湁搴撴帴绠″厛鏍稿鍚?baseline/涓柇鎭㈠/澶囦唤鎭㈠鍥涜妭锛変笌棣栦釜鐪熷疄宸紓杩佺Щ [V20260909.002__inf
ra_quartz_backfill.sql](../services/zhongshu-core/zszj-server/src/main/resource
s/db/migration/V20260909.002__infra_quartz_backfill.sql)锛堟棦鏈夊垵濮嬪寲搴?qrtz 骞傜瓑鍥炲～锛
夈€傜湡瀹炴紨缁冿紙涓€娆℃€?PG17 瀹瑰櫒锛岄獙鍚庢竻鐞嗭級锛氭棫搴撴敞鍏ュ巻鍙茶鈫扸2 鍥炲～鈫掑巻鍙叉暟鎹繚鐣欙紱涓柇婕旂粌瀹炶瘉鑷姩鎻愪氦
妯″紡娈嬬暀/浜嬪姟鍖呰９妯″紡瀹屽叏鍥炴粴锛堣绋嬫嵁姝ょ姝㈣８鎵ц杩佺Щ锛夛紱pg_dump/pg_restore 澶囦唤鎭㈠鍚庤鏁颁笌 qrtz
 琛ㄥ叏閮ㄤ竴鑷淬€傚緟楠屾敹璇存槑锛欶lyway 鏈綋鎺ョ/绡℃敼鎷︽埅杩愯楠岃瘉闅?B02锛汢11 澶嶆牳杩愯淇濋殰銆?
### ZS-DB-006锛氶獙璇佸簭鍒椾富閿€佹壒閲忔彃鍏ヤ笌绉嶅瓙缁彿

- 鍏宠仈锛欶ND-DB-006銆佷竴鏈熺洰鏍団€滀簨鍔′笌 ID 楠岃瘉鈥濓紱WP-03/17锛汢02銆備紭鍏堢骇 P0锛涚被鍒?楠岃瘉閫傞厤锛涚姸鎬?寰呴
獙鏀讹紱鍓嶇疆 ZS-DB-004銆?- 浼楀瑕佹眰涓庣幇鐘讹細[ID 鐜澶勭悊鍣╙[E12]銆乕MyBatis 閰嶇疆][E13] 宸叉湁 PG 閫傞
厤锛沎AdminUserDO][E14] 浣跨敤 system_users_seq銆?8 涓?DO 鐨勮〃鍚?搴忓垪闈欐€佸鐓ч€氳繃锛屼笉璇佹槑鎵ц閫氳
繃銆?- 璋冩暣锛氫紭鍏堝鐢ㄧ幇鏈夊簭鍒楁柟妗堬紝楠岃瘉鍗曟潯/鎵归噺/骞跺彂鎻掑叆銆両D 鍥炲～鍜屽垵濮嬪寲鍚庣殑搴忓垪鍊硷紱纭浣庢潈闄愯处鍙锋湁蹇呰
搴忓垪鎺堟潈銆傛棤鑾锋壒闇€姹備笉缁熶竴鏀逛负闆姳 ID銆?- 楠屾敹锛氬悇璺緞 ID 姝ｇ‘鍥炲～涓斾笉閲嶅锛涚瀛愭暟鎹悗鎻掑叆鎴愬姛锛涗簨鍔″
洖婊氫笉鐣欎笅涓氬姟璁板綍锛屽厑璁稿簭鍒楀嚭鐜版甯哥┖娲烇紝涓嶆妸 ID 杩炲彿浣滀负楠屾敹瑕佹眰銆?- 寮€鍙戣褰曪紙2026-09-09锛夛細浜や粯鐪
熷疄 PG 楠岃瘉鐢ㄤ緥闆?`node scripts/db/run-db006-verify.mjs`锛堜竴娆℃€?PG17 瀹瑰櫒锛岄獙鍚庢竻鐞嗭紱浠ユ暟
鎹簱鐜鏂规鍙岃处鍙疯繍琛岋級銆傜敤渚嬩笌缁撴灉锛欳1 鍗曟潯鎻掑叆+ID 鍥炲～锛坅pp 浣庢潈闄愶級閫氳繃锛汣2 鎵归噺鎻掑叆 100 鏉￠€氳繃锛
汣3 鍏ㄩ儴搴忓垪 last_value鈮ax(id) 鏃犵瀛愮画鍙峰啿绐侀€氳繃锛汣4 涓変細璇濆苟鍙戞彃鍏?3脳50=150 鏉°€佷富閿浂閲嶅閫
氳繃锛汣5 浜嬪姟鍥炴粴涓氬姟璁板綍闆舵畫鐣欙紙搴忓垪绌烘礊鎸夋枃妗ｅ厑璁革級閫氳繃锛汣6 浣庢潈闄愯处鍙?ALTER SEQUENCE 琚嫆閫氳繃銆傝繍琛
屽叆鍙ｅ凡绾冲叆 ZS-OPS-001 闂ㄧ浣撶郴鐨勫悓娆惧す鍏疯涔夈€傚緟楠屾敹璇存槑锛歁yBatis-Plus Java 渚?ID 鍥炲～鏂█锛園K
eySequence 鎵ц璺緞锛夐殢 B02 搴旂敤绾ф祴璇曢摼琛ヨ瘉锛屾湰鐢ㄤ緥闆嗛獙璇佺殑鏄暟鎹簱灞傚簭鍒楄涔夈€?
### ZS-DB-007锛氱粺涓€瀛楁鏄犲皠涓庨€昏緫鍒犻櫎鍚堝悓

- 鍏宠仈锛欶ND-DB-011锛沇P-03/17锛汢02銆備紭鍏堢骇 P0锛涚被鍒?楠岃瘉閫傞厤锛涚姸鎬?寰呴獙鏀讹紱鍓嶇疆 ZS-DB-004銆?- 浼楀
瑕佹眰涓庣幇鐘讹細[BaseDO][E15] 浣跨敤 Boolean 鍜?LocalDateTime锛岃剼鏈瓨鍦?int2銆乼imestamp锛涚敤鎴峰矖
浣嶉泦鍚堥€氳繃 JSON 澶勭悊鍣ㄥ瓨鍏?varchar銆傝繖鏄緟楠岃瘉鏄犲皠锛屼笉鏄凡澶嶇幇閿欒銆?- 璋冩暣锛氭槑纭竷灏斻€佹椂闂?鏃跺尯銆侀暱
鏂囨湰銆丣SON銆佺┖鍊煎拰閫昏緫鍒犻櫎瑙勫垯锛岄獙璇佽嚜鍔ㄥ～鍏咃紱纭渶鏀圭被鍨嬫椂鍚屾 DO銆乀ypeHandler銆丼QL銆佽縼绉讳笌娴嬭瘯锛屼笉鏈
烘濂楃敤渚涗綋 boolean/jsonb/timestamptz銆傚璁″瓧娈典笉寰椾緷璧栧鎴风鍙俊浼犲叆銆?- 楠屾敹锛氬啓鍏ヨ鍥炪€佺瓫閫夈€
佹帓搴忋€佸垎椤点€佺┖鍊煎拰閫昏緫鍒犻櫎涓€鑷达紱姝ｅ父鏌ヨ鐪嬩笉鍒伴€昏緫鍒犻櫎璁板綍锛涘巻鍙叉暟鎹浆鎹㈠彲楠岃瘉锛涘鎴风浼€犻€氱敤瀹¤瀛楁
涓嶈兘瑕嗙洊鏈嶅姟绔簨瀹炪€?- 寮€鍙戣褰曪紙2026-09-09锛夛細浜や粯鐪熷疄 PG 绫诲瀷涓庨€昏緫鍒犻櫎鍚堝悓楠岃瘉鐢ㄤ緥闆?`node s
cripts/db/run-db007-verify.mjs`锛堜竴娆℃€?PG17 瀹瑰櫒 + V1/V2 鍩虹嚎锛岄獙鍚庢竻鐞嗭級锛? 鐢ㄤ緥鍏ㄨ繃锛歞e
leted 鍒?int2 璇箟涓庨粯璁?0銆乼imestamp 鍐欏叆璇诲洖涓€鑷达紙鏃犳椂鍖烘湰鍦版椂闂寸害瀹氾級銆乺emark varchar(500)
 杈圭晫锛堝姩鎬佸彇 character_maximum_length锛夈€乸ost_ids JSON 鏁扮粍 ::jsonb 鏍￠獙涓庡潖 JSON 鎷掔粷
銆佸彲绌?闈炵┖鍒楀悎鍚屻€侀€昏緫鍒犻櫎 deleted=1 鍚庣墿鐞嗚浠嶅湪銆佸璁″垪瀛樺湪銆傝瘎瀹′慨姝ｅ瀹炶褰曪細棣栬疆 C3/C6 澶辫触婧
愪簬鐢ㄤ緥鑷韩锛堥€夐敊琛?閲嶅鎵ц锛夛紝淇鐢ㄤ緥鑰岄潪杩佸氨銆傚緟楠屾敹璇存槑锛歁yBatis-Plus 鑷姩濉厖銆丂TableLogic 鏌
ヨ杩囨护涓庝吉閫犲璁″瓧娈甸槻鎶ゅ睘搴旂敤灞傝涓猴紝闅?B02 搴旂敤绾ф祴璇曢摼锛圝ava 娴嬭瘯绫伙級琛ヨ瘉銆?
### ZS-DB-008锛氳ˉ榻愮湡瀹?PG 浜嬪姟涓庨攣琛屼负鍥炲綊

- 鍏宠仈锛欶ND-SYS-001銆佷竴鏈熺洰鏍団€滀簨鍔′笌 ID 楠岃瘉鈥濓紱WP-03/17/20锛汢02銆備紭鍏堢骇 P0锛涚被鍒?楠岃瘉閫傞厤锛涚姸
鎬?寰呴獙鏀讹紱鍓嶇疆 ZS-DB-004銆乑S-DB-006銆?- 浼楀瑕佹眰涓庣幇鐘讹細[鍒涘缓鐢ㄦ埛鏈嶅姟][E16] 宸茬敤浜嬪姟瑕嗙洊鐢ㄦ埛鍜屽矖
浣嶅啓鍏ワ紱[BaseMapperX][E17] 鏈?FOR UPDATE 杈呭姪鏂规硶锛屽繀椤诲湪浜嬪姟涓娇鐢ㄣ€傚皻鏃犳湰椤圭洰鐪熷疄 PG 鍥炴粴璇佹
嵁銆?- 璋冩暣锛氬厛琛ュ厖涓€斿け璐ュ拰骞跺彂澶瑰叿锛岄獙璇佺敤鎴峰強宀椾綅鍏宠仈鍘熷瓙鎬с€佹壒閲忓啓鍏ャ€侀攣绛夊緟涓庡紓甯稿鐞嗭紱浠呴拡瀵瑰け璐ユ
垨杈圭晫缂哄け澶勪慨鏀癸紝閬垮厤閲嶅閲嶅啓浜嬪姟妗嗘灦銆?- 楠屾敹锛氬矖浣嶅啓鍏ユ晠鎰忓け璐ユ椂鐢ㄦ埛涓嶆畫鐣欙紱鍚堟硶浜嬪姟瀹屾暣鎻愪氦锛涘苟鍙戦攣鍜岃
秴鏃剁粨鏋滃彲棰勬祴锛涗笉鑳藉皢鏁版嵁搴撳洖婊氱瓑鍚?Redis銆佸閮ㄦ秷鎭悓姝ュ洖婊氾紝璺ㄨ祫婧愪竴鑷存€у彟鎸?B05 楠屾敹銆?- 寮€鍙戣褰曪紙
2026-09-09锛夛細浜や粯鐪熷疄 PG 浜嬪姟涓庨攣鍥炲綊鐢ㄤ緥闆?`node scripts/db/run-db008-verify.mjs`锛堜竴娆
℃€?PG17 瀹瑰櫒 + V1/V2 鍩虹嚎 + 鍙岃处鍙凤紝楠屽悗娓呯悊锛夛紝6 鐢ㄤ緥鍏ㄨ繃锛欳1 鐢ㄦ埛+宀椾綅鍏宠仈浜嬪姟涓矖浣嶅啓鍏ユ晠鎰忓け璐
モ啋鏁翠綋鍥炴粴鐢ㄦ埛闆舵畫鐣欙紱C2 鍚堟硶浜嬪姟瀹屾暣鎻愪氦锛汣3 鍗曡鍙?200 琛屾壒閲忓啓鍏ワ紱C4 骞跺彂閿佲€斺€斾細璇?A 鎸?FOR UP
DATE 琛岄攣銆佷細璇?B 浠?lock_timeout=2s 绛夊緟锛堝疄娴?2066ms 瓒呮椂澶辫触銆佸彲棰勬祴锛夛紝A 閲婃斁鍚?B 绔嬪嵆鑾峰緱閿
侊紙69ms锛夛紱C5 MVCC 鏅€氳涓嶅彈琛岄攣闃诲銆傝瘎瀹′慨姝ｅ瀹炶褰曪細C4 棣栬疆澶辫触婧愪簬 spawn 鏃?input 閫夐」瀵艰嚧
鎸侀攣浼氳瘽鏈繍琛岋紝鏀圭敤 -c 鍗曞懡浠ゆ柟寮忎慨澶嶃€傚緟楠屾敹璇存槑锛氬簲鐢ㄥ眰锛圓dminUserServiceImpl锛変簨鍔¤涔夌殑 Jav
a 绾ф柇瑷€涓?Redis/澶栭儴娑堟伅璺ㄨ祫婧愪竴鑷存€у綊 B05锛堟湰鐢ㄤ緥闆嗚鐩栨暟鎹簱灞備簨鍔′笌閿佽涓猴級銆?
### ZS-DB-009锛氱‘璁よ处鍙峰敮涓€鎬т笌鍒犻櫎鍚庨噸鐢ㄨ鍒?
- 鍏宠仈锛欴-09銆丳END-002銆丗ND-IAM-001/002銆丗ND-SYS-001锛沇P-06锛汢07 鍐崇瓥杈撳叆銆備紭鍏堢骇 P0锛堝疄鏂介棬
绂侊級锛涚被鍒?鍐崇瓥锛涚姸鎬?寰呴獙鏀讹紱鍓嶇疆 璐﹀彿/韬唤/缁勭粐杈圭晫纭锛圖-09 宸蹭簬 2026-09-10 纭锛屽喅绛栬緭鍏ュ凡鎷嶆澘
锛岃寮€鍙戣褰曪級銆?- 浼楀瑕佹眰涓庣幇鐘讹細[鍒涘缓鐢ㄦ埛鏈嶅姟][E16] 鍏堟煡閲嶅悗鍐欏叆锛沎PG 涓昏剼鏈琞[E10] 鐨?username
/mobile/email 鏄櫘閫氱储寮曪紝缂哄皯鏁版嵁搴撳敮涓€绾︽潫锛涘苟鍙戦噸澶嶉闄╂槸闈欐€佹帹鏂紝鏈繍琛屽鐜般€?- 璋冩暣锛氭彁浜ゅ緟纭
鐨勭敤鎴峰悕鍏ㄥ钩鍙拌繕鏄鎴峰唴鍞竴銆佸ぇ灏忓啓鍜岃鑼冨寲銆佹墜鏈哄彿/閭绌哄€笺€佸垹闄ゅ悗閲嶇敤銆佸巻鍙茶韩浠戒繚鐣欙紝浠ュ強涓?D-09
 璐﹀彿/浠昏亴鍏崇郴鐨勫吋瀹硅竟鐣屻€傝褰曞€欓€夋柟妗堝強褰卞搷锛屼笉浠ｇ敤鎴锋壒鍑嗐€?- 楠屾敹锛欴-09 鎴栧叾鏄庣‘鎵瑰噯鐨勬妧鏈处鍙峰瓙鍐崇
瓥褰㈡垚涔﹂潰鍞竴鎬х煩闃靛拰閲嶅鏁版嵁澶勭疆瑙勫垯锛涙湭缁忔壒鍑嗕笉鍒涘缓鏂拌韩浠借〃銆佷笉榛樿涓哄叏灞€鍞竴銆佷笉鑷姩鍚堝苟璐﹀彿銆?- 寮€鍙
戣褰曪紙2026-09-09锛夛細浜や粯 D-09 鍐崇瓥杈撳叆 [07-璐﹀彿鍞竴鎬т笌缁勭粐浠昏亴鍐崇瓥杈撳叆](07-璐﹀彿鍞竴鎬т笌缁勭粐浠昏亴
鍐崇瓥杈撳叆.md)鈥斺€斿惈鐜扮姸骞跺彂澶嶇幇瀹炶瘉锛堜袱骞惰浼氳瘽鍚屽悕鐢ㄦ埛鍙屽弻鎴愬姛浜х敓閲嶅璐﹀彿锛岀湡瀹?PG17 瀹炴祴锛岃剼鏈?scri
pts/db/reproduce-user-duplicate.mjs锛夈€佸叚椤瑰緟鍐崇瓥鐭╅樀锛堝敮涓€浣滅敤鍩?瑙勮寖鍖?鎵嬫満閭/鍒犻櫎閲嶇敤/鍘
嗗彶淇濈暀/缁勭粐浠昏亴鍏崇郴锛変笌鎵瑰噯鍚庣殑钀藉湴鏂瑰紡銆傜姸鎬佺淮鎸佸緟鍐崇瓥锛氱煩闃靛緟鐢ㄦ埛鍕鹃€夛紝鏈壒鍑嗗墠涓嶅垱寤烘柊韬唤琛ㄣ€佷笉鍔犲敮
涓€绾︽潫銆佷笉鍚堝苟璐﹀彿銆?
### ZS-DB-010锛氭寜鎵瑰噯瑙勫垯琛ュ厖鍞竴绾︽潫涓庡苟鍙戝厹搴?
- 鍏宠仈锛欶ND-SYS-001銆丏-09锛沇P-03/06锛汢07銆備紭鍏堢骇 P0锛堢浉鍏冲啓鍏ユ斁琛屽墠锛夛紱绫诲埆 鏀归€狅紱鐘舵€?寰呭墠缃紱鍓
嶇疆 ZS-DB-009 鎵瑰噯鍙?ZS-DB-003銆乑S-DB-004銆?- 鎵规杈圭晫锛欱02 鐨勯€氱敤 PG 娴嬭瘯澶瑰叿涓庡苟鍙戞満鍒跺噯澶囧垎
鍒鐢?ZS-DB-019.A銆乑S-DB-020锛屼笉浣滀负鏈」韬唤绾︽潫鐨勫疄鏂芥垨鏀捐锛屼篃涓嶆彁鍓嶅浐鍖?D-09 瀛楁涓庡敮涓€鎬ц鍒欍
€?- 浼楀瑕佹眰涓庣幇鐘讹細[鐢ㄦ埛鏈嶅姟鏌ラ噸][E16] 涓嶈兘鏇夸唬鏁版嵁搴撳苟鍙戠害鏉燂紱[鐢ㄦ埛琛ㄨ剼鏈琞[E10] 鏈変富閿笉绛変簬涓氬姟閿
敮涓€銆傚綋鍓嶄笉涓烘湭缁忕‘璁ょ殑璐﹀彿浣滅敤鍩熺洿鎺ュ姞绱㈠紩銆?- 璋冩暣锛氬厛澶嶇幇骞跺彂閲嶅锛屽啀鎸夋壒鍑嗚寖鍥撮€夋嫨鍞竴/鏉′欢绱㈠紩绛夊
疄鐜帮紝鍚屾寮傚父鍒扮ǔ瀹氶敊璇爜锛涘厛鎶ュ憡鍘嗗彶鍐茬獊骞舵寜鎵瑰噯鏂规澶勭悊锛岀姝㈡搮鑷垹閲嶏紱鍏崇郴琛ㄩ噸澶嶅叧鑱旀寜鍚勮嚜瑙勫垯璇勫銆?-
 楠屾敹锛氬悓涓€鎵瑰噯鍞竴鎬ц寖鍥村苟鍙戝垱寤烘渶澶氫竴涓垚鍔燂紝鍏朵粬寰楀埌鏄庣‘鍐茬獊锛涘厑璁搁噸澶嶇殑涓嶅悓鑼冨洿涓嶈璇嫤锛涚┖鍊笺€佸垹闄ら
噸寤恒€佹巿鏉冩牎楠屼笌鍗囩骇鍐茬獊鏁版嵁鍧囪鐩栥€傛浠诲姟涓嶉樆濉炲叾浠栨棤鍏?PG 閫傞厤鎺ㄨ繘銆?
### ZS-DB-011锛氶€傞厤璁块棶鏃ュ織鍒嗘壒娓呯悊

- 鍏宠仈锛欶ND-DB-007/008锛沇P-03锛汢02銆備紭鍏堢骇 P0锛涚被鍒?鏀归€狅紱鐘舵€?寰呴獙鏀讹紱鍓嶇疆 ZS-DB-004銆?- 浼楀
瑕佹眰涓庣幇鐘讹細[ApiAccessLogMapper][E22] 浣跨敤 DELETE ... LIMIT锛屼笉绗﹀悎鐩爣 PG 鍒犻櫎璇硶銆?- 璋
冩暣锛氬厛鍐欑湡瀹?PG 澶辫触娴嬭瘯锛屽啀鏀逛负闄愬畾涓婚敭闆?绛夋晥鏂规鐨勬湁鐣屽垹闄わ紝淇濈暀鎴鏃堕棿銆佹壒娆″ぇ灏忓強鍘熸湁鎺堟潈鑼冨洿锛涢渶瑕
佸叕鍏辫緟鍔╁疄鐜版椂鐢变簲椤瑰叡鍚屽鐢ㄣ€?- 楠屾敹锛氳竟鐣屾椂闂翠笉璇垹銆佹瘡鎵逛笉瓒呰繃 limit銆佺┖闆嗗彲鎵ц銆佸惊鐜彲娓呯┖杩囨湡璁板
綍锛屾甯歌褰曚繚鐣欙紱骞跺彂鎵ц涓嶈鍒犮€佷笉澶辨帶閲嶈瘯锛涗笉鍥犲幓鎺?LIMIT 鍙樻垚鍏ㄩ噺鍒犻櫎銆?- 寮€鍙戣褰曪紙2026-09-09锛
夛細[ApiAccessLogMapper](../services/zhongshu-core/zszj-module-infra/src/main/jav
a/cn/zszj/module/infra/dal/mysql/logger/ApiAccessLogMapper.java) 閫傞厤涓?PG 鏈夌晫鍒犻櫎
锛圵HERE id IN (SELECT id ... WHERE create_time < n LIMIT n)锛夈€傜湡瀹?PG17 楠岃瘉锛堜竴娆℃€
у鍣紝楠屽悗娓呯悊锛夛細閫犳暟 5 琛岃繃鏈?2 琛屾湭杩囨湡锛屼袱杞湁鐣屽垹闄ゆ竻绌鸿繃鏈熻褰曪紙棣栬疆 3/娆¤疆 2锛夈€佹湭杩囨湡 2 琛屼繚
鐣欍€佺┖闆嗗啀鎵ц涓嶆姤閿欙紙`node scripts/db/run-db011-015-verify.mjs` D1锛夈€傚緟楠屾敹璇存槑锛氱湡瀹?P
G 闆嗘垚娴嬭瘯闅?ZS-DB-019.B 鍏?CI銆?
### ZS-DB-012锛氶€傞厤閿欒鏃ュ織鍒嗘壒娓呯悊

- 鍏宠仈锛欶ND-DB-007/008锛沇P-03锛汢02銆備紭鍏堢骇 P0锛涚被鍒?鏀归€狅紱鐘舵€?寰呴獙鏀讹紱鍓嶇疆 ZS-DB-004銆?- 浼楀
瑕佹眰涓庣幇鐘讹細[ApiErrorLogMapper][E23] 瀛樺湪鍚岀被 DELETE ... LIMIT銆?- 璋冩暣锛氬厛寤虹珛鐙珛澶辫触娴嬭瘯
锛屾部鐢ㄧ粡楠岃瘉鐨勬湁鐣屾竻鐞嗘ā寮忥紝淇濈暀 create_time 鎴鏉′欢涓庢巿鏉冭寖鍥淬€?- 楠屾敹锛氱嫭绔嬭鐩栬繃鏈?鏈繃鏈熴€佹埅姝㈣
竟鐣屻€佹壒娆′笂闄愩€佺┖闆嗐€侀噸澶嶅拰骞跺彂鎵ц锛涗笉鑳藉彧鐢ㄨ闂棩蹇楁祴璇曟浛浠ｆ湰 Mapper 鐨?PG 楠屾敹銆?- 寮€鍙戣褰曪紙20
26-09-09锛夛細[ApiErrorLogMapper](../services/zhongshu-core/zszj-module-infra/src/
main/java/cn/zszj/module/infra/dal/mysql/logger/ApiErrorLogMapper.java) 鍚屾ā寮忛€傞
厤锛堜富閿泦鏈夌晫鍒犻櫎锛夈€傜湡瀹?PG17 楠岃瘉锛歚node scripts/db/run-db011-015-verify.mjs` D2鈥斺€斾袱
杞竻绌鸿繃鏈熻褰曘€佹湭杩囨湡淇濈暀銆佺┖闆嗕笉鎶ラ敊銆傚緟楠屾敹璇存槑锛氶殢 ZS-DB-019.B 鍏?CI銆?
### ZS-DB-013锛氶€傞厤浠诲姟鏃ュ織鍒嗘壒娓呯悊

- 鍏宠仈锛欶ND-DB-007/008銆丗ND-INF-003锛沇P-03锛汢02銆備紭鍏堢骇 P0锛涚被鍒?鏀归€狅紱鐘舵€?寰呴獙鏀讹紱鍓嶇疆 ZS-D
B-004銆?- 浼楀瑕佹眰涓庣幇鐘讹細[JobLogMapper][E24] 瀛樺湪鍚岀被 DELETE ... LIMIT銆?- 璋冩暣锛氬厛鍐欏け璐ユ
祴璇曞苟淇鏈夌晫鍒犻櫎锛屾牳瀵?Job 鏃ュ織淇濈暀鏈熷強璋冪敤閾撅紝涓嶆敼鎴愬垹闄?Quartz 璋冨害鐘舵€併€?- 楠屾敹锛氳繃鏈熶换鍔℃棩蹇楅€愭
壒鍒犻櫎锛屾湭杩囨湡璁板綍淇濈暀锛涜竟鐣屻€佹壒娆°€佺┖闆嗐€侀噸澶嶅拰骞跺彂閫氳繃锛涗换鍔″畾涔変笌 Quartz 杩愯璁板綍涓嶈璇垹銆?- 寮€鍙
戣褰曪紙2026-09-09锛夛細[JobLogMapper](../services/zhongshu-core/zszj-module-infra/sr
c/main/java/cn/zszj/module/infra/dal/mysql/job/JobLogMapper.java) 鍚屾ā寮忛€傞厤銆傜湡瀹?
PG17 楠岃瘉锛歚node scripts/db/run-db011-015-verify.mjs` D3鈥斺€斾袱杞竻绌鸿繃鏈熴€佹湭杩囨湡淇濈暀銆佺┖
闆嗕笉鎶ラ敊銆傚緟楠屾敹璇存槑锛氶殢 ZS-DB-019.B 鍏?CI銆?
### ZS-DB-014锛氶€傞厤 Access Token 杩囨湡娓呯悊

- 鍏宠仈锛欶ND-DB-007/008銆丗ND-SYS-002锛沇P-03/05锛汢02/B03銆備紭鍏堢骇 P0锛涚被鍒?鏀归€狅紱鐘舵€?寰呴獙鏀讹紱鍓
嶇疆 ZS-DB-004銆?- 浼楀瑕佹眰涓庣幇鐘讹細[OAuth2AccessTokenMapper][E25] 浣跨敤甯?expires_time 鐨?
DELETE ... LIMIT銆?- 璋冩暣锛氬厛澶嶇幇 SQL 澶辫触锛屽啀鏀逛负淇濇寔鍒版湡鏉′欢涓庢巿鏉冭寖鍥寸殑鏈夌晫鍒犻櫎锛涙牳瀵逛笌浼氳瘽缂撳瓨
鍜屽仠鐢ㄦ挙鏉冪殑杈圭晫锛屽畬鏁翠細璇濅竴鑷存€у湪 M04/B03 楠屾敹銆?- 楠屾敹锛氭瘡鎵规湁涓婇檺锛屾湭杩囨湡 Token 涓嶈鍒狅紝鎴杈圭晫
銆佺┖闆嗐€侀噸澶嶅拰骞跺彂閫氳繃锛涙竻鐞嗕笉寰椾娇杩囨湡 Token 鎭㈠鏈夋晥锛涗笉鑳藉彧鍑〃娓呯┖瀹ｇО閫€鍑?鎾ゆ潈閾惧凡閫氳繃銆?- 寮€鍙戣
褰曪紙2026-09-09锛夛細[OAuth2AccessTokenMapper](../services/zhongshu-core/zszj-module
-system/src/main/java/cn/zszj/module/system/dal/mysql/oauth2/OAuth2AccessTokenM
apper.java) 鍚屾ā寮忛€傞厤锛坋xpires_time 鎴淇濈暀锛夈€傜湡瀹?PG17 楠岃瘉锛歚node scripts/db/run-db
011-015-verify.mjs` D4鈥斺€斾袱杞竻绌鸿繃鏈熴€佹湭杩囨湡 2 琛屼繚鐣欍€佺┖闆嗕笉鎶ラ敊銆傚緟楠屾敹璇存槑锛氶殢 ZS-DB-01
9.B 鍏?CI锛涗細璇濅竴鑷存€у綊 M04/B03銆?
### ZS-DB-015锛氶€傞厤 Refresh Token 杩囨湡娓呯悊

- 鍏宠仈锛欶ND-DB-007/008銆丗ND-SYS-002锛沇P-03/05锛汢02/B03銆備紭鍏堢骇 P0锛涚被鍒?鏀归€狅紱鐘舵€?寰呴獙鏀讹紱鍓
嶇疆 ZS-DB-004銆?- 浼楀瑕佹眰涓庣幇鐘讹細[OAuth2RefreshTokenMapper][E26] 瀛樺湪鍚岀被 DELETE ... L
IMIT銆?- 璋冩暣锛氬厛鍐欏け璐ユ祴璇曪紝鍗曠嫭閫傞厤 Refresh Token 鐨勫埌鏈熸潯浠朵笌鎵规锛涗笉寰楀皢 Access Token 鐨勭敓
鍛藉懆鏈熺洿鎺ュ鐢ㄣ€?- 楠屾敹锛氭湭杩囨湡鍒锋柊鍑嵁淇濈暀锛屽凡杩囨湡璁板綍鍙竻鐞嗭紱杈圭晫銆佹壒娆′笂闄愩€佺┖闆嗐€侀噸澶嶅拰骞跺彂閫氳繃锛涙竻
鐞嗕笌姝ｅ父鍒锋柊骞惰鏃朵笉璇垹鏈夋晥鍑嵁銆?- 寮€鍙戣褰曪紙2026-09-09锛夛細[OAuth2RefreshTokenMapper](../
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/d
al/mysql/oauth2/OAuth2RefreshTokenMapper.java) 鍚屾ā寮忛€傞厤锛堢嫭绔?expires_time 鎴锛夈€
傜湡瀹?PG17 楠岃瘉锛歚node scripts/db/run-db011-015-verify.mjs` D5鈥斺€斾袱杞竻绌鸿繃鏈熴€佹湭杩囨湡淇濈
暀銆佺┖闆嗕笉鎶ラ敊銆傚緟楠屾敹璇存槑锛氶殢 ZS-DB-019.B 鍏?CI銆?
### ZS-DB-016锛氶獙璇?Quartz 鐨?PG 鎸佷箙鍖栭摼璺?
- 鍏宠仈锛欶ND-DB-009銆丗ND-INF-003锛沇P-03/14锛汢02銆備紭鍏堢骇 P0锛涚被鍒?楠岃瘉閫傞厤锛涚姸鎬?寰呴獙鏀讹紱鍓嶇疆 ZS-
DB-004銆乑S-ENG-006銆?- 浼楀瑕佹眰涓庣幇鐘讹細[ID 鐜澶勭悊鍣╙[E12] 宸插惈 PostgreSQLDelegate 閫夋嫨閫昏
緫锛沎Quartz SQL][E11] 宸叉湁 11 寮犺〃锛沴ocal 榛樿涓嶅惎鐢?Quartz锛屽洜姝や笉鑳界敤 local 鍚姩浠ｆ浛璋冨害楠屾敹
銆?- 璋冩暣锛氭妸璋冨害琛ㄧ撼鍏ヨ縼绉伙紝楠岃瘉瀹為檯鍚敤鐜銆佹暟鎹簮銆佽〃鍓嶇紑銆乨elegate 鍜屾潈闄愶紱闃叉鑷姩寤鸿〃涓庣増鏈縼绉诲
啿绐侊紱鎸夊け璐ヨ瘉鎹慨姝ｅ凡鏈夐€傞厤锛屼笉閲嶅閫犳柟瑷€銆?- 楠屾敹锛氬垱寤恒€佹墽琛屻€佹殏鍋溿€佹仮澶嶃€佸け璐ユ棩蹇椼€侀噸鍚仮澶嶃€佸苟鍙
戝拰娓呯悊鍦ㄧ湡瀹?PG 閫氳繃锛涗笉鍚岃皟搴﹀疄渚嬩笉閫犳垚鏈粡鍏佽鐨勯噸澶嶆墽琛岋紱璁板綍鍗曞疄渚?澶氬疄渚嬫祴璇曡寖鍥淬€?- 寮€鍙戣褰曪紙20
26-09-09锛夛細璋冨害琛ㄥ凡绾冲叆 V1 鍩虹嚎杩佺Щ锛?1 寮?qrtz 琛紝ZS-DB-004锛夊苟涓烘棦鏈夊簱浜や粯 V2 骞傜瓑鍥炲～锛圸S-
DB-005锛夛紱浜や粯 [db016-quartz-schema.sql](../scripts/db/cases/db016-quartz-schema.
sql) 缁撴瀯绾ч獙璇佲€斺€斿湪鐪熷疄 PG 涓婁互 zszjScheduler 鍓嶇紑鎵ц濮旀墭鏍稿績璇彞锛圠OCKS FOR UPDATE/Job
Detail/Trigger 鐘舵€佹祦杞?Cron 瀛愯〃/澶栭敭椤哄簭娓呯悊锛夊叏閮ㄩ€氳繃銆傚緟楠屾敹璇存槑锛氳皟搴﹀櫒杩愯鏈熼獙鏀讹紙鍒涘缓/鎵ц
/鏆傚仠/鎭㈠/閲嶅惎鎭㈠/骞跺彂锛夐渶搴旂敤浠?quartz 鍚敤鎬佽繍琛岋紝闅?B02 鏀跺彛銆?
### ZS-DB-017锛氶獙璇?PG 鍏冩暟鎹笌浠ｇ爜鐢熸垚缁撴灉

- 鍏宠仈锛欶ND-DB-010/011锛沇P-03/17锛汢02銆備紭鍏堢骇 P1锛涚被鍒?楠岃瘉閫傞厤锛涚姸鎬?寰呴獙鏀讹紱鍓嶇疆 ZS-DB-004銆乑
S-DB-007銆?- 浼楀瑕佹眰涓庣幇鐘讹細[DatabaseTableServiceImpl][E27] 閫氳繃 MyBatis-Plus Genera
tor 璇诲彇鏁版嵁婧愬厓鏁版嵁锛涚洰鍓嶆病鏈?PG 琛ㄣ€佸垪銆佺被鍨嬨€佹敞閲娿€佺储寮曡瘑鍒殑鏈」鐩墽琛岃瘉鎹紝涓嶈兘浠呭洜 dal.mysql
 鍖呭悕鍒や负涓嶆敮鎸併€?- 璋冩暣锛氬缓绔嬪甫涓婚敭/搴忓垪銆佹椂闂淬€佸竷灏斻€侀暱鏂囨湰銆丣SON銆佹敞閲婂拰绱㈠紩鐨勬祴璇曡〃锛屾牳瀵?schema
 閫夋嫨鍙婄敓鎴愭ā鏉匡紱鍙戠幇涓嶆敮鎸侀」鍐嶄慨鏀归€傞厤/妯℃澘锛涚敓鎴愮墿鍙繘鍏ラ殧绂绘祴璇曠洰褰曘€?- 楠屾敹锛氬厓鏁版嵁涓庡疄闄呰〃涓€鑷达紝鐢
熸垚浠ｇ爜鍙紪璇戝苟鍦?PG 瀹屾垚鍩虹 CRUD锛涗笉瑕嗙洊宸叉湁鎵嬪啓浠ｇ爜锛屼笉鐢熸垚 MySQL 涓撶敤 SQL锛屼笉鎶婄ず渚嬩唬鐮佺敓鎴愬綋瀹屾
暣涓氬姟浜や粯銆?- 寮€鍙戣褰曪紙2026-09-09锛夛細浜や粯 [db017-metadata-testtable.sql](../scripts/d
b/cases/db017-metadata-testtable.sql)鈥斺€旀寜鍗＄墖瑙勬牸寤虹珛鍏冩暟鎹祴璇曡〃锛堜富閿?搴忓垪銆佹椂闂淬€佸竷灏?i
nt2銆侀暱鏂囨湰銆丣SON varchar銆佽〃鍒楁敞閲娿€佺储寮曪級骞剁粡 information_schema/pg_catalog 鏍稿鍒?榛樿鍊
?娉ㄩ噴/绱㈠紩涓€鑷达紱CodegenEngine 妯℃澘娓叉煋娴嬭瘯锛圴ue3/Vben5/Uniapp锛夊湪宸ュ叿閾句笅杩愯閫氳繃锛堝敮涓€鎺掗櫎椤逛
负宸茬櫥璁扮殑涓婃父 uniapp 鍩虹嚎澶辫触锛夈€傚緟楠屾敹璇存槑锛歁yBatis-Plus Generator 鍏冩暟鎹鍙栦笌鐢熸垚浠ｇ爜缂栬瘧/C
RUD 楠岃瘉闇€搴旂敤杩愯锛岄殢 B02 鏀跺彛銆?
### ZS-DB-018锛氳ˉ榻?ORM銆佹墜鍐?SQL 涓庣鎴烽殧绂荤殑 PG 鍥炲綊

- 鍏宠仈锛欶ND-AUTH-001/003/004/008锛沇P-07/20锛汢02 鎶€鏈洖褰掋€丅08 涓氬姟缁勭粐鏀捐銆備紭鍏堢骇 P0锛涚被鍒?
楠岃瘉閫傞厤锛涚姸鎬?寰呴獙鏀讹紱鍓嶇疆 ZS-DB-004锛涗笟鍔＄粍缁囪涔夐』 D-09銆?- 浼楀瑕佹眰涓庣幇鐘讹細[TenantDatabaseI
nterceptor][E18] 鎸変笂涓嬫枃鍙婅〃绫诲瀷澶勭悊绉熸埛锛屾棤娉曡瘑鍒?TableInfo 鏃惰烦杩囷紱瀹冧笉鏄?PG 鍘熺敓 RLS锛岀洿鎺?
JDBC 涓嶈嚜鍔ㄧ户鎵?ORM 瑙勫垯銆傝繖鏄鏌ヨ竟鐣岋紝涓嶆槸宸茶瘉瀹炵殑瓒婃潈澶嶇幇銆?- 璋冩暣锛氱敤鐜版湁 System 鎶€鏈鎴峰缓绔嬩袱
绉熸埛澶瑰叿锛岃鐩?CRUD銆佸垎椤点€佸叧鑱斻€佹壒閲忋€侀€昏緫鍒犻櫎鍜屾墜鍐?SQL锛涘垪鏄庡叏灞€琛ㄣ€佸拷鐣ユ敞瑙ｅ拰绯荤粺娓呯悊鐨勫悎娉曡寖鍥淬
€倀enant_id 涓?tenant_org_id 鐨勬槧灏勪粎鐧昏涓?D-09 鍚庝换鍔★紝涓嶇洿鎺ユ敼鍚嶆垨寤鸿韩浠借〃銆?- 楠屾敹锛氬悎娉曡姹傛
纭繑鍥烇紝浼€犱笂涓嬫枃鍜屽叾浠栫鎴峰璞?ID 琚嫆缁濓紱鑼冨洿缁曡繃蹇呴』鏄惧紡鍙楁帶涓旀湁鐢ㄤ緥銆傛椤归€氳繃鍙瘉鏄庢棦鏈夋妧鏈鎴烽摼
锛屼笉绛変簬涓や釜涓氬姟缁勭粐鍜岃法缁勭粐鏈嶅姟鎺堟潈宸查€氳繃銆?- M03 琛ュ厖锛?026-09-08锛夛細鏅€?tenant-id 涓?Token 
绉熸埛涓嶄竴鑷存椂锛孾TenantSecurityWebFilter][E33] 宸叉湁鎷掔粷閫昏緫锛涗絾 visit-tenant-id 鏄彟涓€鏉＄壒鏉
冭矾寰勶紝鍏跺姛鑳?鏁版嵁鏉冮檺璺宠繃闂鐢?ZS-SEC-001 鍗曞垪銆傛湰椤瑰鐢ㄥ叾鍙嶅悜鐢ㄤ緥锛屼笉灏嗘櫘閫氱鎴锋瘮瀵规弿杩颁负瀹屽叏缂哄け銆?
- 寮€鍙戣褰曪紙2026-09-09锛孊02 鎶€鏈洖褰掗儴鍒嗭級锛氬湪 07ff751b锛圱woTenantIsolationTest锛孒2 鍐呭瓨搴
?DeptMapper CRUD+閫昏緫鍒犻櫎锛涜娴嬭瘯宸插瀹炵櫥璁般€孒2 涓婁笅鏂囨湭瑁呴厤 TenantLineInnerInterceptor锛屾
嫤鎴櫒绾ч殧绂婚渶 B02 鐪熷疄 PG 楠岃瘉銆嶏級鍩虹涓婅ˉ榻愮湡瀹?PG 鏁版嵁灞傞獙璇侊紝浜や粯 [run-db018-verify.mjs](.
./scripts/db/run-db018-verify.mjs)鈥斺€斾竴娆℃€?Docker PG17 瀹瑰櫒锛坋nv-setup-test.sql +
 V1/V2 杩佺Щ锛岄獙鍚庤嚜鍔ㄦ竻鐞嗭級锛屼互涓?TenantDatabaseInterceptor 鏀瑰啓缁撴灉閫愬瓧绛変环鐨?SQL锛屽湪 Syste
m 鎶€鏈鎴?tenant_id=1/2 鍙岀鎴峰す鍏蜂笂楠岃瘉 C1~C10锛欳1 CRUD 璇诲彇銆丆2 鍒嗛〉銆丆3 鍏宠仈 JOIN銆丆4 鎵归
噺銆丆5 閫昏緫鍒犻櫎銆丆6 鎵嬪啓鑱氬悎 SQL 鍧囨寜 tenant_id=ctx 闅旂锛堟湰绉熸埛鍙銆佷粬绉熸埛 0 琛岋級锛汣7 浼€犱笂涓嬫
枃鎸?PK 璇?鏀?鍒犱粬绉熸埛瀵硅薄鍧?0 琛屼笖鐩爣瀹屽ソ锛汣8/C9/C10 浠ョ粨鏋勬柇瑷€涓庢鍙嶈矾寰勫垪鏄庝笁绫诲悎娉曡寖鍥粹€斺€斿叏灞€琛
紙system_dict_data/infra_job_log 鏃?tenant_id 鍒楋紝BaseDO+@TenantIgnore锛夈€佸拷鐣ユ敞瑙ｏ紙
system_oauth2_access_token 鏈?tenant_id 鍒楋紝浠?OAuth2AccessTokenMapper.selectByAcc
essToken 鏂规硶绾?@TenantIgnore 鏄惧紡缁曡繃锛夈€佺郴缁熸竻鐞嗭紙TokenCleanJob.execute @TenantIgnor
e 璺ㄧ鎴锋湁鐣屾竻杩囨湡浠ょ墝锛屽鐓?ZS-DB-014/015锛夛紱--self-test 璐熷悜瀵圭収璇佹槑闅旂闈炵┖娲烇紙鍚屼竴琛屼綔鐢ㄥ煙鍐?
0銆佸拷鐣ヨ矾寰勫彲瑙侊級銆傚凡娉ㄥ唽杩?[run-pg-regression.mjs](../scripts/db/run-pg-regression.mj
s)锛? 濂椾欢鏈湴鍏ㄧ豢锛夛紝`node scripts/ops/run-local-gates.mjs --fast` 10/10 閫氳繃銆傝竟鐣屼笌寰
呴獙鏀惰鏄庯細鏈」涓?SQL 绾х瓑鏁堣鍙ラ獙璇侊紝璇佹槑鏁版嵁灞傛妧鏈鎴烽摼锛堟嫤鎴櫒鏀瑰啓鍚庣殑闅旂璇箟锛夛紝闈?PG 鍘熺敓 RLS锛岀
洿鎺?JDBC 涓嶈嚜鍔ㄧ户鎵匡紱鎷︽埅鍣ㄧ粡 MyBatis ORM 瀵圭湡瀹?PG 鐨勭鍒扮闆嗘垚鏂█褰?ZS-DB-019.B锛圥G 闆嗘垚娴嬭瘯
鍏ュ彛锛夛紝鏈」浠ョ瓑鏁?SQL 璇佹槑鏁版嵁灞傞殧绂昏涔夈€侀潪鎵ц鎷︽埅鍣ㄦ湰韬紱ORM 鎷︽埅鍣?Java 绾ц閰嶄笌 HTTP 灞傛櫘閫?t
enant-id 涓?Token 涓嶄竴鑷存嫆缁濈敱 ZS-SEC-012.A锛圫ecurityFilterChainFixtureTest 鍙屾妧鏈鎴峰
す鍏凤級銆乑S-SEC-001.A 瑕嗙洊锛屾湰椤瑰鐢ㄥ叾鍙嶅悜鐢ㄤ緥涓嶉噸澶嶃€備袱涓氬姟缁勭粐涓庤法缁勭粐鏈嶅姟鎺堟潈鐨勫畬鏁撮獙鏀堕』 D-09 鍐崇
瓥鍚庝簬 B08 鏀捐锛泃enant_id鈫抰enant_org_id 鏄犲皠鐧昏涓?D-09 鍚庝换鍔★紝鏈」鏈敼鍚嶃€佹湭寤鸿韩浠借〃銆傛椤归€
氳繃鍙瘉鏄庢棦鏈夋妧鏈鎴烽摼锛屼笉绛変簬涓や釜涓氬姟缁勭粐鍜岃法缁勭粐鏈嶅姟鎺堟潈宸查€氳繃銆?
### ZS-DB-019锛氬缓绔嬬湡瀹?PG 闆嗘垚娴嬭瘯鍏ュ彛涓?CI 闂ㄧ

- 鍏宠仈锛欶ND-DB-001/006/007/008銆丗ND-DOC-002锛沇P-20锛汢02锛堟寜瀛愰」鐙珛鏀捐锛夈€備紭鍏堢骇 P0锛涚被鍒?琛
ュ缓锛涚姸鎬?寮€鍙戜腑锛涘墠缃?鎸夌 16.1 鑺傚悇瀛愰」锛岀姝㈠皢鍚庣疆瀛愰」浣滀负鏃╂湡鎵规鍓嶇疆銆?- 浼楀瑕佹眰涓庣幇鐘讹細[System
 娴嬭瘯閰嶇疆][E19] 鍜?Infra 娴嬭瘯閰嶇疆浣跨敤 H2 MySQL 妯″紡锛涘巻鍙?H2/jedismock 娴嬭瘯閫氳繃涓嶈兘鏇夸唬 PG銆備
緵浣撳悎鍚屾祴璇曞彲鍙傝€冿紝浣嗘湭杩佸叆涓诲伐绋嬮獙鏀堕摼銆?- 璋冩暣锛氬缓绔嬪浐瀹?PG 涓荤増鏈殑鍙噸澶嶄复鏃跺疄渚?闅旂搴撴祴璇曞叆鍙ｅ拰澶瑰
叿锛屽啀閫愰」鍔犲叆杩佺Щ銆佹竻鐞嗐€両D銆佺被鍨嬨€佷簨鍔°€丵uartz銆佸厓鏁版嵁鍜屾妧鏈鎴锋祴璇曪紱FND-SYS-001 鐨勪竷绫诲姛鑳藉洖褰掔
敱 ZS-SYS-001.A 澶嶇敤鏈」澶瑰叿锛屽湪 B03 楠屾敹锛屼笉鍙嶅悜闃诲 B02锛涗繚鐣欐湁浠峰€肩殑蹇€熷崟娴嬶紝涓嶅厑璁?H2 鎴愪负鍞
竴鏁版嵁搴撻棬绂併€?- 楠屾敹锛氭湰鍦颁笌 CI 鐢ㄥ悓涓€鍏ュ彛锛涘け璐ラ€€鍑洪潪闆跺苟淇濈暀鎶ュ憡锛涚己灏?PG 蹇呴』鏄庣‘鎶ユ湭鎵ц/澶辫触锛
屼笉鑳介潤榛樺洖閫€ H2 鎴栬烦杩囧悗澹扮О閫氳繃锛涜褰曠増鏈€佸浐瀹氭彁浜ゅ拰鑼冨洿銆傛竻鐞嗗彧浣滅敤浜庨獙璇佽繃鐨勪复鏃舵祴璇曠洰鏍囥€?- 鍒嗘壒
浜や粯锛氳绗?16.1 鑺?ZS-DB-019.A銆乑S-DB-019.B锛涘瓙椤瑰垎鍒櫥璁拌瘉鎹紝鍏ㄩ儴閫傜敤瀛愰」楠屾敹鍚庢墠鑳藉皢鏈富浠诲姟鏍囦
负宸查獙鏀躲€?- 寮€鍙戣褰曪紙2026-09-09锛孼S-DB-019.A 澶瑰叿閮ㄥ垎锛夛細浜や粯涓€娆℃€?Docker PG17 澶瑰叿 `nod
e scripts/db/test-pg-fixture.mjs`鈥斺€斿鍣ㄥ悕/绔彛闅忔満鍞竴銆佽繘绋嬮€€鍑鸿嚜鍔ㄦ竻鐞嗭紙瀹炰緥闅旂锛夛紱ini
t/assert SQL 浠?ON_ERROR_STOP 鎵ц銆佷换涓€澶辫触鏁翠綋闈為浂锛堝け璐ラ潪闆讹級锛汥ocker 涓嶅彲鐢ㄤ互閫€鍑虹爜 3 鏄庣
‘鎶ラ敊锛堢己渚濊禆涓嶉潤榛樿烦杩囷級銆俙--self-test` 鑷瘉涓夋潯璇箟瀹炴祴閫氳繃锛堟甯歌矾寰?澶辫触鏂█闈為浂/缂哄け杈撳叆闈為浂锛夈€
傚す鍏峰彲鐩存帴鎵ц V1 鍩虹嚎鍋氱粨鏋?鏁版嵁鏂█锛圸S-DB-004 鍙屽簱楠岃瘉鍗充互姝ゆ柟寮忓畬鎴愶級銆?- 寮€鍙戣褰曪紙2026-09-0
9 鏅氾紝宸ュ叿閾惧紩瀵煎悗棣栬疆娴嬭瘯鍩虹嚎锛夛細浠ヨ嚜寮曞 JDK17+Maven 杩愯鍚敤妯″潡娴嬭瘯鈥斺€攝szj-common 31 鐢ㄤ緥鍏
ㄨ繃锛堝惈 ModuleCatalogTest锛夛紱鍙戠幇骞朵慨澶?FileConfigServiceImplTest.testGetFileConfigPa
ge 鏁版嵁涓嶄竴鑷达紙鏃╁墠鍝佺墝鎵规灏嗘煡璇晶"鑺嬮亾"鏀逛负"浼楀涔嬪"浣嗘彃鏁颁晶"鑺嬮亾婧愮爜"鍙楃讲鍚嶄繚鎶ゆ湭鍚屾锛屽凡瀵归綈锛夛紱C
odegenEngineUniappTest.testExecute_treeSearch 缁?000e1dfb 鍩虹嚎宸ヤ綔鏍戝鐓х‘璁や负涓婃父鏃㈡湁澶
辫触锛堥潪鍝佺墝鎵规寮曞叆锛夛紝鐧昏涓哄凡鐭ュ熀绾垮け璐ュ苟鍦ㄩ棬绂佷腑鎸夋柟娉曠骇鎺掗櫎銆傚緟楠屾敹璇存槑锛?B 鐨?PG 鏂硅█鍥炲綊锛圚2鈫扨G 杩
佺Щ鍐崇瓥涓嶅湪鏈す鍏疯寖鍥达級涓庡叏閲忓熀绾挎竻闆舵寜 B02 鏀跺彛銆?- 寮€鍙戣褰曪紙2026-09-09 娣卞锛孋I 鎺ュ叆锛夛細鏂板 [.
github/workflows/pg-regression.yml](../.github/workflows/pg-regression.yml)鈥斺€擯
G 鍥炲綊濂椾欢浠ュ悓涓€瑙勫垯鍏?CI锛坧ush/PR 瑙﹀彂锛夛紝涓庢湰鍦板叆鍙ｅ叡鐢?run-pg-regression.mjs锛涜嚦姝?ZS-DB-0
19.B 鐨勬墽琛屽叆鍙ｄ笌濂椾欢鍦板熀浜や粯瀹屾瘯锛屽墿浣欎负 B02 姝ｅ紡鐜鎺堟潈鍚庣殑瀹屾暣鏀跺彛銆?- 寮€鍙戣褰曪紙2026-09-09 娣
卞锛孼S-DB-019.B 鍦板熀锛夛細浜や粯 PG 鍥炲綊鑱氬悎鍏ュ彛 `node scripts/db/run-pg-regression.mjs`鈥斺
€斾覆鑱?DB-006/007/008/011~015/CFG-002.A 涓?DB-016/017 缁撴瀯绾х敤渚嬪叡 7 濂椾欢锛屽叏杩囷紙鏈湴涓?CI
 鍚岃鍒欙紝B02 姝ｅ紡鎺ュ叆寰呯幆澧冩巿鏉冿級銆?- 寮€鍙戣褰曪紙2026-09-09 娣卞锛宻ystem 妯″潡鍩虹嚎淇锛夛細system 
娴嬭瘯濂椾欢棣栬疆鍏ㄩ噺杩愯锛?71 鐢ㄤ緥锛夋毚闇?8 澶勫け璐ワ紝閫愰」 triage锛? 澶勪负鍝佺墝鎵规鐨勬煡璇?绉嶅瓙鍚嶇О涓嶅绉帮紙SmsC
hannel/SmsTemplate/SocialClient/Tenant 鍒嗛〉鐢ㄤ緥涓?TenantPackage 涓ゅ瑁呴厤闂锛屽悗鑰呭凡浜?b
2716b47 淇锛夆€斺€旀煡璇晶涓庣瀛愪晶宸插叏閮ㄥ榻愪负浼楀涔嬪锛? 澶勪负涓婃父鏃㈡湁澶辫触锛圕odegenEngineUniappTe
st#testExecute_treeSearch锛屽凡鎸夋柟娉曠骇鎺掗櫎鐧昏锛夈€傚榻愬悗 system/infra/common 涓夋ā鍧楁祴璇曞叏缁
匡紙BUILD SUCCESS锛夈€?19.B 鍓╀綑鏀跺彛锛欻2鈫扨G 鏂硅█鍥炲綊鍐崇瓥涓?CI 鎺ュ叆銆傚緟楠屾敹璇存槑锛?A 鐨勫畬鏁撮獙鏀讹紙Mav
en 娴嬭瘯閾炬帴鍏ヤ笌 ZS-DB-019.B 鐨?PG 鎶€鏈洖褰掑浠讹級渚濊禆 ZS-DB-001.B 鐪熷疄杩炴帴涓?B02 宸ュ叿閾撅紝鎸?16
.1 鍓嶇疆鍙﹁鏀跺彛锛涙湰澶瑰叿涓烘湰鍦板厛琛屼氦浠樹欢锛岀姝㈢敤鍏舵浛浠?H2鈫扨G 杩佺Щ鍐崇瓥銆?
### ZS-DB-020锛氳瘎瀹℃煡璇㈢储寮曘€佹壒閲忔洿鏂颁繚鎶や笌骞跺彂浣跨敤瑙勮寖

- 鍏宠仈锛欶ND-DB-012锛沇P-03/17/20锛汢02锛屾湭鏉ラ鍩熷苟鍙戝湪 B09 缁嗗寲銆備紭鍏堢骇 P1锛涚被鍒?楠岃瘉閫傞厤锛涚姸鎬?寰呴
獙鏀讹紱鍓嶇疆 ZS-DB-004銆乑S-DB-008锛涙秹鍙婁笟鍔″敮涓€鎬у彟渚濊禆 ZS-DB-009銆?- 浼楀瑕佹眰涓庣幇鐘讹細[MyBatis 
閰嶇疆][E13] 涓?BlockAttackInnerInterceptor 琚敞閲婏紝娉ㄩ噴鎻愮ず鍙兘褰卞搷鍚堟硶 updateBatch锛涙湰娆?F
ramework/System/Infra 妫€绱㈡湭瑙佺粺涓€涔愯閿佹彃浠舵垨 Version 瀛楁锛屼笉浠ｈ〃搴旂粰鍏ㄩ儴琛ㄦ坊鍔犵増鏈彿銆?- 璋
冩暣锛氬熀浜庡疄闄?Mapper 鏌ヨ涓?PG 鎵ц璁″垝璇勫蹇呰绱㈠紩锛涙鏌ユ棤鏉′欢/澶ц寖鍥存洿鏂板垹闄ょ殑鍚堟硶鐢ㄩ€斿拰闃叉姢鏂规锛涙槑
纭綍鏃剁敤浜嬪姟銆佽閿併€佸敮涓€绾︽潫鎴栦箰瑙傜増鏈€傛寜褰卞搷璇勫閫愮偣瀹炵幇锛屼笉鍏ㄩ噺寮€鍚嫤鎴櫒鎴栨坊鍔犲閿?鐗堟湰瀛楁銆?- 楠屾
敹锛氶珮棰戞煡璇㈡湁鍙鏍歌鍒掍笌鏁版嵁瑙勬ā锛涘嵄闄╂壒閲忓啓鍏ヨ闃绘鎴栭檺鍒跺湪鑾峰噯鐨勪笓鐢ㄨ矾寰勶紱鍚堟硶鏂囦欢閰嶇疆绛夋壒閲忔洿鏂颁笉琚
鎷︼紱閲囩敤鐨勫苟鍙戞満鍒堕兘鏈夊啿绐佹祴璇曪紝涓嶉鎶ユ湭缁忔祴璇曠殑鎬ц兘鎸囨爣銆?- 寮€鍙戣褰曪紙2026-09-09锛夛細浜や粯 [鏌ヨ绱㈠紩涓
庡苟鍙戣鑼冭瘎瀹(../services/zhongshu-core/docs/鏌ヨ绱㈠紩涓庡苟鍙戣鑼冭瘎瀹?md)鈥斺€擝lockAttackInn
erInterceptor 缁存寔鍏抽棴骞剁粰鍑烘浛浠ｉ槻鎶よ鑼冿紙鍏ㄨ〃鍐欎粎闄愯幏鎵逛笓鐢ㄨ矾寰勶紝鐜版湁璺緞锛氭湁鐣屾竻鐞?Job + Flyway
锛夛紱骞跺彂鏈哄埗鎸夊満鏅垎娲撅紙FOR UPDATE/鍞竴绾︽潫/鍘熷瓙 UPDATE锛屼笉寮曞叆鍏ㄥ眬鐗堟湰瀛楁锛夛紱鐧昏 B02 寰呰ˉ璇侀」锛
圗XPLAIN ANALYZE 鎶ュ憡銆佹嫤鎴櫒寮€鍏冲洖褰掑姣斻€佹€ц兘鎸囨爣涓嶉鎵胯锛夈€傚啿绐佹祴璇曞凡鏈夛細搴忓垪骞跺彂锛圸S-DB-006
 C4锛夈€佽閿佽秴鏃讹紙ZS-DB-008 C4锛夈€佸瓧鍏稿苟鍙戝敮涓€锛圸S-CFG-002.A C3锛夈€傚緟楠屾敹璇存槑锛欵XPLAIN 璁″垝瀹
炴祴涓庢暟鎹妯℃寚鏍囧緟 B02 鐜銆?
## 5. M03锛氭帴鍙ｄ笌瀹夊叏閾捐矾

### 5.1 鐜版湁璋冪敤涓诲共涓庡彲澶嶇敤鑳藉姏

鎸夋簮鐮佸０鏄庯紝涓诲共涓猴細CORS 鈫?Trace 鈫?JSON 璇锋眰浣撶紦瀛?鈫?绉熸埛涓婁笅鏂?鈫?API 璁块棶鏃ュ織 鈫?Spring Secu
rity/Token 鈫?绉熸埛瀹夊叏鏍￠獙 鈫?MVC 璺ㄧ鎴疯闂嫤鎴櫒 鈫?Controller 鍙傛暟/鏂规硶鏉冮檺 鈫?Service/Map
per 鈫?鍝嶅簲涓庢棩蹇椼€傛澶勭渷鐣ュ彲閫夎繃婊ゅ櫒锛屼笉浠ｈ〃杩愯鏃惰閰嶃€佸紓姝ユ淳鍙戞垨 AOP 鍐呴儴椤哄簭宸茬粡娴嬭瘯锛涢『搴忎緷鎹 
[WebFilterOrderEnum][E53]銆?
| 灞傛 | 宸叉湁鏈哄埗涓庝唬鐮佸叆鍙?| 浼楀閫傞厤杈圭晫 |
|---|---|---|
| 鎺ュ彛褰掔被 | [Web 鑷姩閰嶇疆][E32] 鎸?Controller 鍖呰ˉ鍏呭墠缂€锛沎WebProperties][E54] 榛樿 adm
in-api/app-api | 鍓嶇紑鍖哄垎鎺ュ彛/鐢ㄦ埛绫诲瀷锛屼笉鏄?Web/灏忕▼搴忕粓绔竟鐣岋紱鍛樺伐绉诲姩绔笉鑳藉洜缁堢涓嶅悓琚嚜鍔ㄦ敼鎴
?MEMBER |
| 璁よ瘉 | [Token 杩囨护鍣╙[E30] 璋冪敤 OAuth2TokenCommonApi 鏍￠獙锛屾瀯閫?LoginUser 骞舵瘮瀵?URL 瀵
瑰簲鐨勭敤鎴风被鍨?| 鏃犳晥 Token 鍙寜鍖垮悕缁х画锛屾渶缁堢敱鍏紑/鍙椾繚鎶よ鍒欏喅瀹氾紱杩欐槸鐜版湁璁捐锛屼笉绛変簬璁よ瘉缁曡繃锛沵ock
 鍙﹁ ZS-ENG-004 |
close-task.mjs
task-stats.mjs
verify-docs.mjs
verify-docs.test.mjs

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'git 
diff 8c9b6082 HEAD -- scripts/gov; git log --oneline -4; git show 8c9b6082:docs
/05-底座模块分析与开发任务清单.md | Select-Object -First 75; Get-Content -Encoding UTF8 scri
pts/gov/verify-docs.mjs; Get-Content -Encoding UTF8 scripts/ops/run-local-gates
.mjs -TotalCount 170' in E:\众墅之家AI赋能平台底座
 succeeded in 19ms:
/**
 * ZS-GOV-001 文档一致性检查器（本地与 CI 同一入口）。
 *
 * 校验范围：README.md 与 docs/0*.md（历史报告 docs/04 只查链接，不查内容口径）。
 * 规则：
 *  R1 链接有效：内联链接、引用定义的本地目标必须存在（外链/锚点跳过）；
 *  R2 任务编号唯一：05 文档 `### ZS-XXX-NNN` 标题不得重复；
 *  R3 状态枚举合法：05 卡片"状态 X"只允许 7 个枚举值；
 *  R4 决策门禁：未确认决策（D-07/D-10/D-11）不得被写成已批准/已确认/已落地；D-09 已于 2026-09-10 确认最小模型与账号唯
一性细则，移出守护列表；
 *  R5 版本一致：README 文档索引的版本号与各文档头部"文档版本：V*"一致。
 *  R6 统计一致：05 §2 声明的状态分布必须等于卡片实际聚合（见 task-stats.mjs）；无声明句则跳过；
 *  R7 README 摘要一致：README"累计 N 项主任务（…）"的总数与各状态数必须等于 05 卡片实际聚合；无声明句则跳过。
 * 用法：node scripts/gov/verify-docs.mjs（退出码非 0 = 不一致）
 */
import { readFileSync, existsSync, statSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join, resolve } from 'node:path';
import { countStatus, parseSection2Declared, parseReadmeDeclared } from './task
-stats.mjs';

const root = fileURLToPath(new URL('../../', import.meta.url));
const DOCS = ['README.md',
  'docs/01-底座代码复用与改造方案.md',
  'docs/02-一期底座需求规格与待决策台账.md',
  'docs/03-底座二次开发顺序与验收标准.md',
  'docs/05-底座模块分析与开发任务清单.md',
  'docs/06-品牌素材与命名映射.md'];
const HISTORICAL = /^docs\/04-/; // 历史报告：只查链接，不重写口径
const STATUS_ENUM = ['待开发', '待决策', '待前置', '开发中', '待验收', '已验收', '暂缓'];
const UNCONFIRMED_DECISIONS = ['D-07', 'D-10', 'D-11']; // D-09 已于 2026-09-10 确
认，移出守护列表
const VERSION_HEADER = /文档版本：\s*([A-Za-z0-9.]+)/;

export function checkDocs(files, readFile, rootDir, { exists = existsSync } = {
}) {
  const issues = [];
  const contents = new Map();
  for (const f of files) {
    try {
      contents.set(f, readFile(f));
    } catch {
      issues.push({ rule: 'R0-read', file: f, message: '文件无法读取' });
    }
  }

  // R1 链接有效（校验范围：仓库内目标；仓库外引用属跨工作区引用，打印提示不计失败）
  for (const f of files) {
    const text = contents.get(f) ?? '';
    const baseDir = dirname(resolve(rootDir, f));
    const refs = [];
    for (const m of text.matchAll(/\]\(([^)\s]+)\)/g)) refs.push(m[1]);
    for (const m of text.matchAll(/^\[[^\]]+\]:\s+(\S+)$/gm)) refs.push(m[1]);
    for (const target of refs) {
      if (/^(https?:|mailto:|#)/.test(target)) continue;
      const clean = target.split('#')[0];
      if (!clean) continue;
      // 绝对路径 / 尖括号包裹目标：作者环境的仓库外引用，不校验
      if (/^<.+>$/.test(clean) || /^[A-Za-z]:[\\/]/.test(clean) || clean.starts
With('/')) {
        console.error(`[info] ${f}: 仓库外/绝对路径引用（不校验）: ${target}`);
        continue;
      }
      const abs = resolve(baseDir, decodeURI(clean));
      if (!exists(abs)) {
        if (!abs.startsWith(resolve(rootDir))) {
          console.error(`[info] ${f}: 仓库外引用（不校验）: ${target}`);
        } else {
          issues.push({ rule: 'R1-link', file: f, message: `链接目标不存在: ${target}`
 });
        }
      }
    }
  }

  // R2 任务编号唯一（仅 05）
  for (const f of files) {
    if (!f.includes('05-')) continue;
    const ids = [...(contents.get(f) ?? '').matchAll(/^### (ZS-[A-Z]+-\d{3})/gm
)].map((m) => m[1]);
    const seen = new Set();
    for (const id of ids) {
      if (seen.has(id)) issues.push({ rule: 'R2-dup-id', file: f, message: `任务编
号重复: ${id}` });
      seen.add(id);
    }
  }

  // R3 状态枚举（仅 05）
  for (const f of files) {
    if (!f.includes('05-')) continue;
    for (const m of (contents.get(f) ?? '').matchAll(/^-\s*关联[^\n]*?状态\s*([^\s；
;，]+)/gm)) {
      if (!STATUS_ENUM.includes(m[1])) {
        issues.push({ rule: 'R3-status', file: f, message: `非法任务状态: "${m[1]}"（允
许：${STATUS_ENUM.join('/')}）` });
      }
    }
  }

  // R4 未确认决策不得写成既成事实（否定句式如"不是已确认结论"属合规表述）
  for (const f of files) {
    const text = contents.get(f) ?? '';
    for (const d of UNCONFIRMED_DECISIONS) {
      for (const m of text.matchAll(new RegExp(`${d}[^。\\n|]{0,12}(已确认|已批准|已通过|
已落地)`, 'g'))) {
        // 关键词前的否定/禁止措辞视为合规表述（规则自述文本同样会命中模式）
        const beforeKeyword = text.slice(m.index, m.index + m[0].length).slice(
-8, -3);
        if (/不得|不应|禁止|不是|非 |未经|尚未$|未$/.test(beforeKeyword)) continue;
        issues.push({ rule: 'R4-decision', file: f, message: `未确认决策 ${d} 被写成既成事
实: …${m[0]}…` });
      }
    }
  }

  // R5 版本一致（README 索引 vs 文档头）
  const readme = contents.get('README.md') ?? '';
  for (const m of readme.matchAll(/\[(文档[^\]]*|[^[\]]+)\]\((docs\/0[0-9][^)]*)\
)\s*\|\s*([A-Za-z0-9.]+)/g)) {
    const [, , docPath, version] = m;
    const doc = files.find((f) => f === docPath);
    if (!doc) continue;
    if (HISTORICAL.test(docPath)) continue;
    const header = VERSION_HEADER.exec(contents.get(doc) ?? '');
    if (!header) issues.push({ rule: 'R5-version', file: docPath, message: '文档缺
少"文档版本："头' });
    else if (header[1] !== version) {
      issues.push({ rule: 'R5-version', file: docPath, message: `README 索引版本 ${
version} 与文档头 ${header[1]} 不一致` });
    }
  }

  // R6 统计一致（仅 05）：§2 声明分布必须等于卡片实际聚合；无声明句则跳过
  const doc05 = files.find((f) => f.includes('05-'));
  const actual05 = doc05 ? countStatus(contents.get(doc05) ?? '') : null;
  if (doc05) {
    const declared = parseSection2Declared(contents.get(doc05) ?? '');
    if (declared) {
      for (const s of Object.keys(declared)) {
        if (declared[s] !== actual05.counts[s]) {
          issues.push({ rule: 'R6-count', file: doc05, message: `§2 声明 ${s} ${d
eclared[s]} 项，实际卡片聚合 ${actual05.counts[s]} 项` });
        }
      }
    }
  }

  // R7 README 摘要一致：README 声明的总数与各状态数必须等于 05 卡片实际聚合；无声明句或缺 05 则跳过
  const readmeFile = files.find((f) => f === 'README.md');
  if (readmeFile && actual05) {
    const rm = parseReadmeDeclared(contents.get(readmeFile) ?? '');
    if (rm) {
      if (rm.total !== actual05.cardCount) {
        issues.push({ rule: 'R7-readme-sync', file: readmeFile, message: `READM
E 累计 ${rm.total} 项主任务，实际 ${actual05.cardCount} 项` });
      }
      for (const s of Object.keys(rm.declared)) {
        if (rm.declared[s] !== actual05.counts[s]) {
          issues.push({ rule: 'R7-readme-sync', file: readmeFile, message: `REA
DME 声明 ${s} ${rm.declared[s]} 项，实际 ${actual05.counts[s]} 项` });
        }
      }
    }
  }

  return issues;
}

const invokedDirectly = process.argv[1] && process.argv[1].replaceAll('\\', '/'
).endsWith('scripts/gov/verify-docs.mjs');
if (invokedDirectly) {
  const issues = checkDocs(DOCS, (f) => readFileSync(join(root, f), 'utf8'), ro
ot);
  console.log(JSON.stringify({ files: DOCS.length, issueCount: issues.length, i
ssues }, null, 2));
  process.exitCode = issues.length ? 1 : 0;
}
/**
 * ZS-OPS-001.A 本地基线门禁聚合入口（本地与 CI 同一规则）。
 *
 * 并发执行全部本地检查，任一失败即整体失败并汇总（结果按门禁定义顺序稳定输出）：
 *   G1 来源复制校验器单测 / G2 品牌命名门禁单测 / G2b 命名迁移工具单测 / G3 品牌命名全仓扫描
 *   G4 文档一致性单测 / G5 文档一致性全量（ZS-GOV-001）/ G6 模块白名单（ZS-ENG-001）
 *   G7 数据源 PG 合同（ZS-DB-001.A）/ G8 Flyway 迁移规范（ZS-DB-003）/ G9 配置秘密门禁（ZS-CFG-001
.A）
 *   G10 Web 类型检查基线（ZS-CLIENT-005.A，较慢；--fast 跳过）
 *   G11 启用模块后端单测（--mvn 显式启用；需 tools/env.sh 工具链，排除已登记的上游基线失败）
 * PG/多端 E2E 门禁按 ZS-OPS-001.B~.E 批次接入，不在本骨架。
 *
 * 提速（ZS-GOV-001 提效方案 P1）：
 *   - 并发：默认按 CPU 核数并发跑门禁（--jobs N 覆盖），反馈时间从「各门禁耗时之和」降到「最慢门禁」。
 *   - 增量：--incremental 按 git 变更路径只跑受影响门禁（品牌全仓扫描 G3、秘密门禁 G9 恒定跑；
 *     变更含门禁脚本自身 scripts/ops/、或存在无法归类且非良性的路径时 fail-safe 回退全量）。
 *     仅供日常快速反馈；批次收口与 CI 必须跑全量（--fast 或含 G10/G11），不得以增量结果代替放行。
 *   - --fast 固化：日常开发默认 --fast（跳过较慢的 G10 vue-tsc），批次收口去掉 --fast 跑全量。
 * 用法：node scripts/ops/run-local-gates.mjs [--fast] [--mvn] [--incremental] [--
jobs N] [--plan]
 */
import { execFile, spawn, spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { cpus } from 'node:os';

const root = fileURLToPath(new URL('../../', import.meta.url));

// 门禁定义。areas：增量模式下命中这些路径前缀才跑；safety：增量模式恒定跑（全仓合规/安全守卫）；
// slow：--fast 跳过；mvn：仅 --mvn 启用。cmd/mvnArgs 与 id 保持与既有基线一致（CI、README 依赖）。
export const GATES = [
  { id: 'G1 来源复制校验器单测', cmd: ['node', '--test', 'scripts/verify-source-copy.tes
t.mjs'], areas: ['scripts/verify-source-copy', 'third_party/'] },
  { id: 'G2 品牌命名门禁单测', cmd: ['node', '--test', 'scripts/brand/verify-brand-nami
ng.test.mjs'], areas: ['scripts/brand/'] },
  { id: 'G2b 命名迁移工具单测（冻结文件排除 + 租户域名唯一）', cmd: ['node', '--test', 'scripts/brand
/apply-naming-migration.test.mjs'], areas: ['scripts/brand/'] },
  { id: 'G3 品牌命名全仓扫描', cmd: ['node', 'scripts/brand/verify-brand-naming.mjs'], 
areas: ['services/', 'apps/', 'scripts/', 'docs/'], safety: true },
  { id: 'G4 文档一致性单测', cmd: ['node', '--test', 'scripts/gov/verify-docs.test.mjs
'], areas: ['scripts/gov/', 'docs/', 'README.md'] },
  { id: 'G5 文档一致性全量', cmd: ['node', 'scripts/gov/verify-docs.mjs'], areas: ['sc
ripts/gov/', 'docs/', 'README.md'] },
  { id: 'G6 模块白名单', cmd: ['node', 'scripts/eng/verify-module-whitelist.mjs'], a
reas: ['scripts/eng/', 'services/'] },
  { id: 'G7 数据源 PG 合同', cmd: ['node', 'scripts/db/verify-datasource-pg.mjs'], a
reas: ['scripts/db/', 'services/'] },
  { id: 'G8 Flyway 迁移规范', cmd: ['node', 'scripts/db/verify-flyway-migrations.mj
s'], areas: ['scripts/db/', 'services/'] },
  { id: 'G9 配置秘密门禁', cmd: ['node', 'scripts/cfg/verify-config-secrets.mjs'], ar
eas: ['scripts/cfg/', 'services/', 'apps/'], safety: true },
  { id: 'G10 Web 类型检查基线', cmd: ['node', 'scripts/client/verify-ts-baseline.mjs'
], areas: ['apps/zhongshu-admin-web/', 'scripts/client/'], slow: true },
  { id: 'G11 启用模块后端单测（common/infra，排除上游基线失败）', areas: ['services/'], mvn: true,
    mvnArgs: '-pl zszj-framework/zszj-common,zszj-module-infra -am -Dtest=!Code
genEngineUniappTest#testExecute_treeSearch -Dsurefire.failIfNoSpecifiedTests=fa
lse test' },
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
 * @param {boolean} o.fast @param {boolean} o.mvn @param {boolean} o.incrementa
l
 * @param {string[]|null} o.changedFiles 变更路径（相对仓库根，正斜杠）；null 表示由调用方检测
 */
export function planGates({ fast = false, mvn = false, incremental = false, cha
ngedFiles = null }) {
  const candidates = candidateGates({ fast, mvn });
  if (!incremental) return { gates: candidates, mode: 'full', reason: '未启用 --in
cremental，跑全量', changed: changedFiles ?? [] };

  const changed = changedFiles ?? [];
  if (!changed.length) return { gates: candidates, mode: 'full', reason: '增量：无变
更或变更检测失败，回退全量', changed };
  if (changed.some((f) => f.startsWith('scripts/ops/'))) return { gates: candid
ates, mode: 'full', reason: '增量：变更含门禁脚本自身（scripts/ops/），回退全量', changed };

  // 未归类且非良性路径 → 保守回退全量（无法判断影响面）
  const allAreas = candidates.flatMap((g) => g.areas ?? []);
  const unmapped = changed.filter((f) => !allAreas.some((a) => f.startsWith(a))
 && !BENIGN_IGNORE.some((b) => f.startsWith(b)));
  if (unmapped.length) return { gates: candidates, mode: 'full', reason: `增量：${
unmapped.length} 个变更路径无法归类（${unmapped.slice(0, 3).join(', ')}${unmapped.length 
> 3 ? ' …' : ''}），回退全量`, changed };

  const selected = candidates.filter((g) => g.safety || (g.areas ?? []).some((a
) => changed.some((f) => f.startsWith(a))));
  return { gates: selected, mode: 'incremental', reason: `增量：按 ${changed.length
} 个变更路径选中 ${selected.length}/${candidates.length} 门禁`, changed };
}

/** 检测工作树相对 HEAD 的变更路径（含已跟踪修改与未跟踪新文件）；失败返回 []。 */
function detectChangedFiles() {
  try {
    const opts = { cwd: root, encoding: 'utf8' };
    const tracked = spawnSync('git', ['-c', 'core.quotepath=false', 'diff', '--
name-only', 'HEAD'], opts);
    const untracked = spawnSync('git', ['-c', 'core.quotepath=false', 'ls-files
', '--others', '--exclude-standard'], opts);
    if (tracked.status !== 0 || untracked.status !== 0) return [];
    const paths = [...String(tracked.stdout ?? '').split('\n'), ...String(untra
cked.stdout ?? '').split('\n')]
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
      tail: String(output ?? '').trimEnd().split('\n').slice(-4).join(' | ').sl
ice(0, 400),
    });
    if (gate.mvnArgs) {
      // Maven 门禁需先注入 tools 工具链环境（JDK17/Maven 不在系统 PATH）
      const child = spawn('bash', ['-c', 'source tools/env.sh && cd services/zh
ongshu-core && MSYS_NO_PATHCONV=1 "$TOOLS/apache-maven-3.9.9/bin/mvn.cmd" ' + g
ate.mvnArgs],
        { cwd: root, stdio: ['ignore', 'pipe', 'pipe'] });
      let out = '';
      child.stdout.on('data', (d) => { out += d; });
      child.stderr.on('data', (d) => { out += d; });
      child.on('error', (e) => done('FAIL', out + String(e)));
      child.on('close', (code) => done(code === 0 ? 'PASS' : 'FAIL', out));
    } else {
      execFile(gate.cmd[0], gate.cmd.slice(1), { cwd: root, maxBuffer: 64 * 102
4 * 1024, encoding: 'utf8' },
        (err, stdout, stderr) => done(err ? 'FAIL' : 'PASS', err ? String(stdou
t ?? '') + String(stderr ?? '') : String(stdout ?? '')));
    }
  });
}

/** 以 jobs 上限并发跑门禁；结果按 gates 定义顺序返回（与完成顺序无关），完成即时打印进度。 */
async function runGates(gates, jobs) {
  const results = new Array(gates.length);
  let next = 0;
  const worker = async () => {
    while (next < gates.length) {
      const i = next++;
      const r = await runGate(gates[i]);
      results[i] = r;
      console.log(`[${r.status}] ${r.id} (${r.ms}ms)`);
    }
  };
  await Promise.all(Array.from({ length: Math.min(jobs, gates.length) }, worker
));
  return results;
}

async function main() {
  const argv = process.argv.slice(2);
  const fast = argv.includes('--fast');
  const mvn = argv.includes('--mvn');
  const incremental = argv.includes('--incremental');
  const planOnly = argv.includes('--plan');
  const jobsIdx = argv.indexOf('--jobs');
  const jobs = Math.max(1, jobsIdx >= 0 ? (Number(argv[jobsIdx + 1]) || 1) : (c
pus().length || 4));

  const changedFiles = incremental ? detectChangedFiles() : [];
  const plan = planGates({ fast, mvn, incremental, changedFiles });
  if (incremental) console.log(`[增量] ${plan.reason}`);

  if (planOnly) {
    console.log(`将运行 ${plan.gates.length} 项门禁（--plan，不执行）：`);
    for (const g of plan.gates) console.log('  - ' + g.id);
    return 0;
  }

  console.log(`运行 ${plan.gates.length} 项门禁（模式 ${plan.mode}，并发 ${Math.min(jobs, 
plan.gates.length)}${fast ? '，--fast' : ''}${mvn ? '，--mvn' : ''}）…`);
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
  console.log(`合计 ${results.length} 项，失败 ${failed.length} 项（并发耗时 ${totalMs}ms）`
);
  return failed.length ? 1 : 0;
}

const invokedDirectly = process.argv[1] && process.argv[1].replaceAll('\\', '/'
).endsWith('scripts/ops/run-local-gates.mjs');
if (invokedDirectly) {
  main().then((code) => process.exit(code)).catch((e) => { console.error(e); pr
ocess.exit(1); });

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "node
 --test scripts/gov/verify-docs.test.mjs; node scripts/gov/task-stats.mjs; node
 scripts/gov/close-task.mjs --sync-only --dry-run; git ls-tree -r --name-only 8
c9b6082 | Select-String '("'^|/)(AGENTS('"\\.override)?\\.md|CLAUDE\\.md|GEMINI
\\.md)"'$'"'; Get-ChildItem -Force -Path scripts,docs,scripts/gov -Name | Selec
t-String '"'^'"\\.'" in E:\众墅之家AI赋能平台底座
 succeeded in 254ms:
{
  "卡片实际聚合": {
    "待开发": 43,
    "待决策": 0,
    "待前置": 6,
    "开发中": 11,
    "待验收": 31,
    "已验收": 0,
    "暂缓": 0
  },
  "卡片总数": 91,
  "状态行总数": 91,
  "非法状态": [],
  "§2声明": {
    "待开发": 43,
    "开发中": 11,
    "待验收": 31,
    "待决策": 0,
    "待前置": 6,
    "已验收": 0
  },
  "README声明": {
    "total": 91,
    "declared": {
      "待开发": 43,
      "开发中": 11,
      "待验收": 31,
      "待前置": 6
    }
  }
}
无需修改：docs/05 §2 与 README 已与卡片实际聚合一致（幂等）。

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "@'
import { execFileSync } from 'node:child_process';
import { checkDocs } from './scripts/gov/verify-docs.mjs';
import { countStatus, parseSection2Declared } from './scripts/gov/task-stats.mj
s';
const files = ['README.md','docs/01-底座代码复用与改造方案.md','docs/02-一期底座需求规格与待决策台账.md'
,'docs/03-底座二次开发顺序与验收标准.md','docs/05-底座模块分析与开发任务清单.md','docs/06-品牌素材与命名映射.md'];
const docs = Object.fromEntries(files.map(f => [f, execFileSync('git',['show',"
'`8c9b6082:${f}`], { encoding:'"'utf8' })]));
let data = {...docs};
console.log('commit baseline issues:', checkDocs(files, f=>data[f], process.cwd
()).map(x=>x.rule));
data['README.md'] = data['README.md'].replace('、31 待验收', '');
console.log('README omitted 31 tasks issues:', checkDocs(files, f=>data[f], pro
cess.cwd()).map(x=>x.rule));
data = {...docs};
data[files[4]] = data[files[4]].replace('31 项待验收、', '');
console.log('section2 omitted 31 tasks issues:', checkDocs(files, f=>data[f], p
rocess.cwd()).map(x=>x.rule));
// Execute the actual close-task source against an in-memory filesystem.
const source = execFileSync('git',['show','8c9b6082:scripts/gov/close-task.mjs'
], { encoding:'utf8' }).replace(/"'^import .*;'"\\r?\\n/gm, '').replace(/"'^con
st root = .*;$/m, '"'const root = ROOT;');
data = {...docs};
const log = [];
const stdout = {log: (...args)=>log.push(args.join(' ')), error: (...args)=>log
.push(args.join(' '))};
const fakeProcess = {argv: ['node','scripts/gov/close-task.mjs', 'ZS-SEC-006', 
'暂缓'], exit(code) {throw Error("'`exit ${code}`);}, exitCode:0};
new Function('"'ROOT','readFileSync','writeFileSync','execFileSync','join','cou
ntStatus','STATUS_ENUM','process','console', source)(
  '', f=>data[f], (f,t)=>{data[f]=t;}, ()=>JSON.stringify(checkDocs(files,f=>da
ta[f],process.cwd())), (_,f)=>f, countStatus,
  ['待开发', '待决策', '待前置', '开发中', '待验收', '已验收', '暂缓'], fakeProcess, stdout
);
console.log('close-task log:',log.join('\\n'));
console.log('actual counts:', countStatus(data[files[4]]).counts);
console.log('written section2:', parseSection2Declared(data[files[4]]));
'@ | node --input-type=module" in E:\众墅之家AI赋能平台底座
 exited 1 in 56ms:
[info] README.md: 仓库外引用（不校验）: ../../企业FDE项目/众墅之家设计小程序/03-众墅之家设计小程序后端技术架构-V1.0.m
d
[info] README.md: 仓库外引用（不校验）: ../../企业FDE项目/众墅之家设计小程序/04-设计小程序V1.2输入基线与追踪.md
[info] README.md: 仓库外引用（不校验）: ../../企业FDE项目/众墅之家设计小程序/众墅之家设计小程序后端实施蓝图-V1.0.md
commit baseline issues: [
  'R7-readme-sync',
  'R7-readme-sync',
  'R7-readme-sync',
  'R7-readme-sync',
  'R7-readme-sync'
]
[info] README.md: 仓库外引用（不校验）: ../../企业FDE项目/众墅之家设计小程序/03-众墅之家设计小程序后端技术架构-V1.0.m
d
[info] README.md: 仓库外引用（不校验）: ../../企业FDE项目/众墅之家设计小程序/04-设计小程序V1.2输入基线与追踪.md
[info] README.md: 仓库外引用（不校验）: ../../企业FDE项目/众墅之家设计小程序/众墅之家设计小程序后端实施蓝图-V1.0.md
README omitted 31 tasks issues: [
  'R7-readme-sync',
  'R7-readme-sync',
  'R7-readme-sync',
  'R7-readme-sync',
  'R7-readme-sync'
]
[info] README.md: 仓库外引用（不校验）: ../../企业FDE项目/众墅之家设计小程序/03-众墅之家设计小程序后端技术架构-V1.0.m
d
[info] README.md: 仓库外引用（不校验）: ../../企业FDE项目/众墅之家设计小程序/04-设计小程序V1.2输入基线与追踪.md
[info] README.md: 仓库外引用（不校验）: ../../企业FDE项目/众墅之家设计小程序/众墅之家设计小程序后端实施蓝图-V1.0.md
section2 omitted 31 tasks issues: [
  'R7-readme-sync',
  'R7-readme-sync',
  'R7-readme-sync',
  'R7-readme-sync',
  'R7-readme-sync'
]
<anonymous_script>:46
  const lines = doc05.split('\n');
                      ^

TypeError: Cannot read properties of undefined (reading 'split')
    at eval (eval at <anonymous> (file:///E:/%E4%BC%97%E5%A2%85%E4%B9%8B%E5%AE%
B6AI%E8%B5%8B%E8%83%BD%E5%B9%B3%E5%8F%B0%E5%BA%95%E5%BA%A7/[eval1]:19:1), <anon
ymous>:46:23)
    at file:///E:/%E4%BC%97%E5%A2%85%E4%B9%8B%E5%AE%B6AI%E8%B5%8B%E8%83%BD%E5%B
9%B3%E5%8F%B0%E5%BA%95%E5%BA%A7/[eval1]:19:130
    at ModuleJob.run (node:internal/modules/esm/module_job:439:25)
    at process.processTicksAndRejections (node:internal/process/task_queues:104
:5)
    at async node:internal/modules/esm/loader:224:26
    at async ModuleLoader.executeModuleJob (node:internal/modules/esm/loader:22
1:20)
    at async asyncRunEntryPointWithESMLoader (node:internal/modules/run_main:10
1:5)

Node.js v24.19.0

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '$Out
putEncoding = [System.Text.UTF8Encoding]::new(); @'"'
import { execFileSync } from 'node:child_process';
import { checkDocs } from './scripts/gov/verify-docs.mjs';
import { countStatus, parseSection2Declared, STATUS_ENUM } from './scripts/gov/
task-stats.mjs';
const files = ['README.md','docs/01-底座代码复用与改造方案.md','docs/02-一期底座需求规格与待决策台账.md'
,'docs/03-底座二次开发顺序与验收标准.md','docs/05-底座模块分析与开发任务清单.md','docs/06-品牌素材与命名映射.md'];
const docs = Object.fromEntries(files.map(f => [f, execFileSync('git',['show',"
'`8c9b6082:${f}`], { encoding:'"'utf8' })]));
let data = {...docs};
const issues = () => checkDocs(files, f=>data[f], process.cwd(), {exists:()=>tr
ue});
console.log('commit baseline issues:', issues());
data['README.md'] = data['README.md'].replace('、31 待验收', '');
console.log('README omitted 31 tasks issues:', issues());
data = {...docs};
data[files[4]] = data[files[4]].replace('31 项待验收、', '');
console.log('section2 omitted 31 tasks issues:', issues());
const source = execFileSync('git',['show','8c9b6082:scripts/gov/close-task.mjs'
], { encoding:'utf8' }).replace(/"'^import .*;'"\\r?\\n/gm, '').replace(/"'^con
st root = .*;$/m, '"'const root = ROOT;');
data = {...docs};
const log = [];
const stdout = {log: (...args)=>log.push(args.join(' ')), error: (...args)=>log
.push(args.join(' '))};
const fakeProcess = {argv: ['node','scripts/gov/close-task.mjs', 'ZS-SEC-006', 
'暂缓'], exit(code) {throw Error("'`exit ${code}`);}, exitCode:0};
new Function('"'ROOT','readFileSync','writeFileSync','execFileSync','join','cou
ntStatus','STATUS_ENUM','process','console', source)(
  '', f=>data[f], (f,t)=>{data[f]=t;}, ()=>JSON.stringify(issues()), (_,f)=>f, 
countStatus, STATUS_ENUM, fakeProcess, stdout
);
console.log('close-task log:',log.join('\\n'));
console.log('actual counts:', countStatus(data[files[4]]).counts);
console.log('written section2:', parseSection2Declared(data[files[4]]));
'@ | node --input-type=module" in E:\众墅之家AI赋能平台底座
 succeeded in 59ms:
[info] docs/01-底座代码复用与改造方案.md: 仓库外/绝对路径引用（不校验）: </Users/honor.pei/Documents/企业F
DE项目/众墅之家设计小程序/后端程序>
commit baseline issues: []
[info] docs/01-底座代码复用与改造方案.md: 仓库外/绝对路径引用（不校验）: </Users/honor.pei/Documents/企业F
DE项目/众墅之家设计小程序/后端程序>
README omitted 31 tasks issues: []
[info] docs/01-底座代码复用与改造方案.md: 仓库外/绝对路径引用（不校验）: </Users/honor.pei/Documents/企业F
DE项目/众墅之家设计小程序/后端程序>
section2 omitted 31 tasks issues: []
[info] docs/01-底座代码复用与改造方案.md: 仓库外/绝对路径引用（不校验）: </Users/honor.pei/Documents/企业F
DE项目/众墅之家设计小程序/后端程序>
close-task log: 已做 4 项改动：
  - ZS-SEC-006 状态：待开发 → 暂缓
  - §2 统计回填：待开发42/开发中11/待验收31/待前置6/暂缓1
  - README 摘要回填：累计 91（42 待开发、11 开发中、31 待验收、6 待前置、1 暂缓）
  - 变更记录建议（贴入 §19 变更记录表）：V<新版本>（2026-09-10）：ZS-SEC-006 转「暂缓」，§2/README 统计按卡片实际聚
合回填。

写盘完成，复核 verify-docs：
[]
actual counts: {
  '待开发': 42,
  '待决策': 0,
  '待前置': 6,
  '开发中': 11,
  '待验收': 31,
  '已验收': 0,
  '暂缓': 1
}
written section2: { '待开发': 42, '开发中': 11, '待验收': 31, '待决策': 0, '待前置': 6, '已验收':
 0 }

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '$i=0
; Get-Content -Encoding UTF8 scripts/gov/close-task.mjs | ForEach-Object { $i++
; if ($i -ge 60 -and $i -le 78) { '"'{0,4}: {1}' -f "'$i, $_ } }; $i=0; Get-Con
tent -Encoding UTF8 scripts/gov/verify-docs.mjs | ForEach-Object { $i++; if ($i
 -ge 115 -and $i -le 149) { '"'{0,4}: {1}' -f "'$i, $_ } }; git diff --numstat;
 git status --short' in E:\众墅之家AI赋能平台底座
 succeeded in 45ms:
warning: unable to access 'C:\Users\Administrator/.config/git/ignore': Permissi
on denied
warning: unable to access 'C:\Users\Administrator/.config/git/ignore': Permissi
on denied
 M services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz-tenant/sr
c/main/java/cn/zszj/framework/tenant/core/web/TenantContextWebFilter.java
 M services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/main/
java/cn/zszj/framework/web/config/WebProperties.java
 M services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/main/
java/cn/zszj/framework/web/config/ZszjWebAutoConfiguration.java
 M services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/main/
java/cn/zszj/framework/web/core/filter/CacheRequestBodyFilter.java
 M services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/main/
java/cn/zszj/framework/web/core/util/WebFrameworkUtils.java
 M services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/syste
m/controller/admin/social/SocialClientController.java
 M services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/syste
m/controller/admin/social/SocialUserController.java
?? docs/reviews/codex-ZS-GOV-001-P0.raw.md
?? docs/reviews/codex-ZS-OPS-001-step4.raw.md
?? docs/reviews/codex-hotfix-C-r1.raw.md
?? docs/reviews/codex-hotfix-C.raw.md
?? docs/reviews/codex-hotfix-D.raw.md
?? docs/reviews/codex-hotfix-E.raw.md
?? outputs/
?? "services/zhongshu-core/docs/\345\217\202\346\225\260\346\240\241\351\252\21
4\344\270\216\350\257\267\346\261\202\350\265\204\346\272\220\351\231\220\345\2
10\266\350\247\204\350\214\203.md"
?? services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/test/
java/cn/zszj/framework/web/core/filter/
?? services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/test/
java/cn/zszj/framework/web/core/util/
?? services/zhongshu-core/zszj-server/src/test/java/cn/zszj/server/ValidationCo
ntractTest.java

codex
All 11 existing tests pass, but valid status transitions can generate incomplet
e statistics, and the new consistency gates accept omitted nonzero categories. 
These defects undermine the synchronization guarantees introduced by this commi
t.

Full review comments:

- [P2] Compare every status when validating summary distributions — E:/众墅之家AI赋能
平台底座/scripts/gov/verify-docs.mjs:140-142
  When a nonzero category is omitted, this loop checks only the remaining decla
red keys. Removing `、31 待验收` from the committed README therefore passes validat
ion despite its distribution accounting for only 60 of 91 tasks. R6 has the sam
e omission problem for §2. Compare all status-enum entries, treating omitted ca
tegories as zero, while preserving the intentional skip for entirely absent dec
larations.

- [P2] Add newly populated states to the section 2 summary — E:/众墅之家AI赋能平台底座/sc
ripts/gov/close-task.mjs:69-71
  Running `node scripts/gov/close-task.mjs ZS-SEC-006 暂缓` against the committed
 documents changes the card and README correctly, but §2 only changes 待开发 from 
43 to 42: it never inserts `1 项暂缓` because that category is absent from the ori
ginal sentence. Consequently, the generated §2 distribution accounts for only 9
0 tasks. Rebuild the distribution or insert missing nonzero categories rather t
han only replacing existing numbers.
All 11 existing tests pass, but valid status transitions can generate incomplete statistics, and the new consistency gates accept omitted nonzero categories. These defects undermine the synchronization guarantees introduced by this commit.

Full review comments:

- [P2] Compare every status when validating summary distributions — E:/众墅之家AI赋能平台底座/scripts/gov/verify-docs.mjs:140-142
  When a nonzero category is omitted, this loop checks only the remaining declared keys. Removing `、31 待验收` from the committed README therefore passes validation despite its distribution accounting for only 60 of 91 tasks. R6 has the same omission problem for §2. Compare all status-enum entries, treating omitted categories as zero, while preserving the intentional skip for entirely absent declarations.

- [P2] Add newly populated states to the section 2 summary — E:/众墅之家AI赋能平台底座/scripts/gov/close-task.mjs:69-71
  Running `node scripts/gov/close-task.mjs ZS-SEC-006 暂缓` against the committed documents changes the card and README correctly, but §2 only changes 待开发 from 43 to 42: it never inserts `1 项暂缓` because that category is absent from the original sentence. Consequently, the generated §2 distribution accounts for only 90 tasks. Rebuild the distribution or insert missing nonzero categories rather than only replacing existing numbers.
