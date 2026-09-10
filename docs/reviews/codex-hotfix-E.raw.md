codex.cmd : OpenAI Codex v0.153.4
所在位置 行:1 字符: 24
+ ... 家AI赋能平台底座"; & "$env:APPDATA\npm\codex.cmd" review --commit ec92cb85 2 ...
+                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    + CategoryInfo          : NotSpecified: (OpenAI Codex v0.153.4:String) [], 
    RemoteException
    + FullyQualifiedErrorId : NativeCommandError
 
--------
workdir: E:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇
model: gpt-6-astra
provider: openai
approval: never
sandbox: workspace-write [workdir, /tmp, $TMPDIR]
reasoning effort: xhigh
reasoning summaries: none
session id: 01a08583-de7c-78e0-b4fa-ff1c94117dbc
--------
user
commit ec92cb8
2026-09-09T09:33:32.035226Z ERROR codex_models_manager::manager: failed to refr
esh available models: timeout waiting for child process to exit
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "git 
status --short; git rev-parse --show-toplevel; git show --stat --oneline --deco
rate --no-renames ec92cb85; Get-ChildItem -Force -Name; Get-ChildItem -LiteralP
ath E:\\ -Force -Name AGENTS*" in E:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇
 succeeded in 9ms:
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "git 
-c core.quotepath=false show --format=fuller --no-ext-diff --no-renames ec92cb8
5; Get-ChildItem -Path . -Recurse -Force -File -Include AGENTS.md,AGENTS.overri
de.md -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName; i
f (Test-Path 'C:\\Users\\Administrator\\.codex\\config.toml') { Get-Content -Li
teralPath 'C:\\Users\\Administrator\\.codex\\config.toml' }" in E:\浼楀涔嬪AI璧嬭兘骞
冲彴搴曞骇
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "Get-
Command rg, node -ErrorAction SilentlyContinue | Select-Object Name,Source; git
 -c core.quotepath=false ls-files '*AGENTS*' '*.instructions.md' '*review*'; "'
$paths = @('"'E:\\AGENTS.override.md','E:\\AGENTS.md','AGENTS.override.md','AGE
NTS.md','scripts\\AGENTS.override.md','scripts\\AGENTS.md','scripts\\brand\\AGE
NTS.override.md','scripts\\brand\\AGENTS.md','docs\\AGENTS.override.md','docs\\
AGENTS.md'); foreach ("'$p in $paths) { if (Test-Path -LiteralPath $p) { Write-
Output "--- $p"; Get-Content -LiteralPath $p } }; Get-Content -LiteralPath scri
pts/brand/verify-backend-naming.mjs' in E:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇
 succeeded in 12ms:
rg.exe   C:\Users\Administrator\AppData\Roaming\npm\node_modules\@openai\codex\
node_modules\@openai\codex-win32-x64\...
node.exe C:\Program Files\nodejs\node.exe                                      
                                        
/**
 * ZS-BRAND-002 閸氬海顏弨鐟版倳闂堟瑦鈧椒绔撮懛瀛樷偓褎顥呴弻銉礄閺?JDK/Maven 閻滎垰顣ㄦ稉瀣畱閸掓繃顒炴宀冪槈閿? * 
楠炴彃鍣ｉ弸鍕紦閵嗕椒绶风挧鏍ㄧ埐娑撳海婀＄€?PG 閸氼垰濮╂禒宥嗗瘻 docs/06 缁?7 閼哄倸婀?B01/B02 鐞涖儴鐦夐敍澶堚偓? *
 * 閻劍纭堕敍姝痮de scripts/brand/verify-backend-naming.mjs
 * 濡偓閺屻儵銆嶉敍? *  1. 濮ｅ繋閲?.java 閻?package 婢圭増妲戞稉搴″従閻╊喖缍嶇捄顖氱窞娑撯偓閼疯揪绱?
 *  2. 閼奉亜濮╅柊宥囩枂濞夈劌鍞介敍鍦TA-INF/spring/*.imports閵嗕够pring.factories閿涘鑵戦惃?FQCN 閸欘
垵袙閺嬫劕鍩屽┃鎰垳閺傚洣娆㈤敍? *  3. Mapper XML 閻?namespace/resultType/parameterType 娑?logb
ack logger 娑擃厾娈?cn.zszj FQCN 閸欘垵袙閺嬫劧绱?
 *  4. POM 娑擃厺楠囬崫浣告綏閺嶅洨绮烘稉鈧稉?cn.zszj/zszj-*閿涘odule/娓氭繆绂嗛幐鍥ф倻鐎涙ê婀惃鍕侀崸妤冩窗瑜版洩绱
?
 *  5. 濞堝鏆€閹躲儱鎲￠敍姘稊閻劌鐓欓崘?cn.iocoder/yudao/Yudao/YUDAO/youdao 閸戣櫣骞囨担宥囩枂閸掑棛琚潏
鎾冲毉閵? * 娴犺绔寸涵顒佲偓褎顥呴弻銉ャ亼鐠愩儰浜掗棃鐐烘祩闁偓閸戞亽鈧? */
import { readFileSync, existsSync, readdirSync, statSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join, dirname, resolve } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));
const core = join(root, 'services', 'zhongshu-core');

// 閺嬪嫬缂?娓氭繆绂嗘禍褏澧块惄顔肩秿娑撳秴寮稉搴ㄦ饯閹焦顥呴弻銉礄ZS-BRAND-002 閸欐垹骞?2閿涘绱癕aven 閻㈢喐鍨氶惃?// t
arget/generated-sources/**/*.java 鐠侯垰绶炴稉宥呮儓 /src/(main|test)/java/ 閸撳秶绱戦敍灞肩窗鐞氼偆
顑?1 妞?// package-path 閺嶏繝鐛欑拠顖氬灲娑?mismatch閿涘vn compile 閸氬孩婀伴崷浼存，缁備礁浜ｉ梼铏偓褝绱濋惍鏉戞
綎閵嗗本婀伴崷棰佺瑢 CI 閸氬矁顫夐崚娆嶁偓宥冣偓?// 閸︺劌鏁稉鈧柆宥呭坊閸樼喕顕?walk() 缂佺喍绔寸捄瀹犵箖閿涘奔绔存径鍕洬閻╂牕鍙忛柈?
5 濞?walk閵嗗倷绮庨崚妤冪卜娑撳秴鎯堥崥搴ｎ伂濠ф劗鐖滈惃鍕獓閻椻晝娲拌ぐ鏇幢
// out/build/dist 閸欘垵鍏橀弰顖氭値濞夋洘绨惍浣稿瘶閸氬稄绱欐俊?erp vo/out/ 閸戝搫绨?VO閿涘绱濇稉鈧獮鎯扮儲鏉╁洣绱板
蹇旑梾閻喎鐤勫▓瀣殌閿涘牆浜ｉ梼瀛樷偓褝绱氶敍灞炬櫊娑撳秶鎾奸崗銉ｂ偓?const SKIP_DIRS = new Set(['target', 'no
de_modules', '.git', '.idea']);
function walk(dir, fn) {
  for (const name of readdirSync(dir)) {
    if (SKIP_DIRS.has(name)) continue;
    const p = join(dir, name);
    const st = statSync(p);
    if (st.isDirectory()) walk(p, fn);
    else fn(p);
  }
}

const issues = [];
const allJava = [];
walk(core, (p) => {
  if (p.endsWith('.java') && !p.includes(`${sep()}` + 'zszj-ui' + `${sep()}`) &
& !p.includes(`${sep()}` + 'sql' + `${sep()}`)) allJava.push(p);
});
function sep() { return process.platform === 'win32' ? '\\' : '/'; }

// 1. package 婢圭増妲戞稉搴ｆ窗瑜版洑绔撮懛杈剧礄娑撳﹥鐖堕崢鍡楀蕉闁鏆€閿涙o/userItem 閻╊喖缍嶆径褍鍟撴稉搴″瘶婢圭増妲?u
seritem 娑撳秳绔撮懛杈剧礉
//    鐏炵偘绗傚〒鍛婃＆閺堝绮ㄩ弸鍕６妫版ü绗?javac 娑撳秵鐗庢宀嬬礉娑撳骸鎼ч悧灞炬暭閸氬秵妫ら崗绛圭礉閻ф槒顔囩挒浣稿帳閿?const 
packageCaseExemptions = new Set(['vo/userItem/']);
for (const file of allJava) {
  const text = readFileSync(file, 'utf8');
  const m = text.match(/^\s*package\s+([\w.]+)\s*;/m);
  if (!m) { issues.push(['missing-package', rel(file)]); continue; }
  const relPath = dirname(file).slice(core.length + 1).split(sep()).join('/');
  const expect = relPath.replace(/^.*\/src\/(main|test)\/java\//, '');
  if (m[1] !== expect.replaceAll('/', '.')) {
    if (packageCaseExemptions.has(`${expect.split('/').slice(-2).join('/')}/`) 
&& m[1] === expect.replaceAll('/', '.').toLowerCase()) continue;
    issues.push(['package-path-mismatch', `${rel(file)}: ${m[1]} != ${expect}`]
);
  }
}

// FQCN -> 濠ф劗鐖滈弬鍥︽鐟欙絾鐎?
const classIndex = new Set();
for (const file of allJava) {
  const text = readFileSync(file, 'utf8');
  const m = text.match(/^\s*package\s+([\w.]+)\s*;/m);
  if (m) classIndex.add(`${m[1]}.${basename(file).replace(/\.java$/, '')}`);
}
function basename(p) { return p.split(sep()).pop(); }
function rel(p) { return p.slice(root.length).split(sep()).join('/'); }
function resolveFqcn(fqcn) {
  if (classIndex.has(fqcn)) return true;
  const path = join(core, ...fqcn.split('.')) + '.java';
  return existsSync(path);
}
function checkFqcn(where, fqcn) {
  if (!/^[a-z][\w.]*Zszj|^cn\.zszj/.test(fqcn)) return; // 閸欘亝鐗庢灞奸獓閸?FQCN
  if (!resolveFqcn(fqcn)) issues.push(['unresolved-fqcn', `${where}: ${fqcn}`])
;
}

// 2. 閼奉亜濮╅柊宥囩枂濞夈劌鍞?
walk(core, (p) => {
  const norm = p.split(sep()).join('/');
  if (norm.includes('/zszj-ui/')) return;
  if (norm.endsWith('.imports')) {
    for (const line of readFileSync(p, 'utf8').split('\n')) {
      const t = line.trim();
      if (t && !t.startsWith('#')) checkFqcn(rel(p), t);
    }
  } else if (norm.endsWith('META-INF/spring.factories')) {
    for (const line of readFileSync(p, 'utf8').split('\n')) {
      if (line.trim().endsWith('\\') || !line.includes('=') || line.trim().star
tsWith('#')) continue;
      for (const token of line.split('=')[1].split(',')) {
        const t = token.trim();
        if (t) checkFqcn(rel(p), t);
      }
    }
  }
});

// 3. Mapper XML 娑?logback
walk(core, (p) => {
  const norm = p.split(sep()).join('/');
  if (norm.includes('/zszj-ui/') || !(norm.endsWith('.xml'))) return;
  const text = readFileSync(p, 'utf8');
  const re = /(?:namespace|resultType|parameterType|type|ref)="(cn\.zszj[\w.]*)
"/g;
  let m;
  while ((m = re.exec(text))) checkFqcn(rel(p), m[1]);
  const re2 = /name="(cn\.zszj[\w.]*)"/g;
  while ((m = re2.exec(text))) {
    const fqcn = m[1];
    if (resolveFqcn(fqcn) || classIndex.has(fqcn) || existsSync(join(core, ...f
qcn.split('.')))) continue;
    // logger 閸氬秴鍘戠拋鍛婃Ц閸栧懎澧犵紓鈧懓宀勬姜閸忚渹缍嬬猾?    const prefix = fqcn;
    let ok = false;
    for (const c of classIndex) if (c.startsWith(prefix + '.')) { ok = true; br
eak; }
    if (!ok) issues.push(['unresolved-logger', `${rel(p)}: ${fqcn}`]);
  }
});

// 4. POM 閸ф劖鐖ｆ稉鈧懛瀛樷偓?const pomFiles = [];
walk(core, (p) => { if (p.endsWith('pom.xml')) pomFiles.push(p); });
for (const pom of pomFiles) {
  const text = readFileSync(pom, 'utf8').replace(/<!--[\s\S]*?-->/g, '');
  if (/cn\.iocoder/.test(text)) issues.push(['pom-old-groupid', rel(pom)]);
  if (/yudao/.test(text)) issues.push(['pom-old-artifact', rel(pom)]);
  for (const m of text.matchAll(/<module>([^<]+)<\/module>/g)) {
    if (!existsSync(join(dirname(pom), m[1], 'pom.xml'))) issues.push(['pom-mis
sing-module', `${rel(pom)}: ${m[1]}`]);
  }
}

// 5. 濞堝鏆€閹躲儱鎲￠敍鍫滄眽瀹搞儱顓搁梼?+ 娑?docs/06 缁?4 閼哄倷绶ユ径鏍ь嚠閻撗嶇礆
const residue = new Map();
const residueRe = /cn\.iocoder|youdao|YUDAO|[Yy]udao/g;
walk(core, (p) => {
  const norm = p.split(sep()).join('/');
  if (norm.includes('/zszj-ui/') || norm.includes('/.git/') || /\.(png|jpg|jpeg
|gif|xdb|ico)$/i.test(norm)) return;
  const text = readFileSync(p, 'utf8');
  const hits = text.match(residueRe);
  if (hits) residue.set(rel(p), hits.length);
});

const hardFailures = issues.length;
console.log(JSON.stringify({
  javaFiles: allJava.length,
  hardIssues: issues.slice(0, 40),
  hardIssueCount: hardFailures,
  residueFiles: residue.size,
  residueTop: [...residue.entries()].sort((a, b) => b[1] - a[1]).slice(0, 30),
}, null, 2));
process.exitCode = hardFailures ? 1 : 0;



 succeeded in 31164ms:
notify = [ "C:\\Users\\Administrator\\AppData\\Local\\OpenAI\\Codex\\runtimes\\
cua_node\\b474a88d5d105afa\\bin\\node_modules\\@oai\\sky\\bin\\windows\\codex-c
omputer-use.exe", "turn-ended" ]
model = "gpt-6-astra"
model_reasoning_effort = "xhigh"
service_tier = "default"
[desktop]
followUpQueueMode = "steer"
conversationDetailMode = "STEPS_COMMANDS"
sansFontSize = 14
codeFontSize = 13
ambient-suggestions-enabled = false

[marketplaces.openai-bundled]
source_type = "local"
source = '\\?\C:\Users\Administrator\.codex\.tmp\bundled-marketplaces\openai-bu
ndled'

[marketplaces.openai-primary-runtime]
source_type = "local"
source = '\\?\C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtim
e\plugins\openai-primary-runtime'

[plugins."codex-app-tools@openai-bundled"]
enabled = true

[plugins."browser@openai-bundled"]
enabled = true

[plugins."unified-computer-use@openai-bundled"]
enabled = true

[plugins."visualize@openai-bundled"]
enabled = true

[plugins."documents@openai-primary-runtime"]
enabled = true

[plugins."pdf@openai-primary-runtime"]
enabled = true

[plugins."spreadsheets@openai-primary-runtime"]
enabled = true

[plugins."presentations@openai-primary-runtime"]
enabled = true

[plugins."template-creator@openai-primary-runtime"]
enabled = true

[plugins."computer-use@openai-bundled"]
enabled = true

[plugins."chrome@openai-bundled"]
enabled = true

[plugins."sites@openai-bundled"]
enabled = true

[mcp_servers.node_repl]
args = []
command = 'C:\Users\Administrator\AppData\Local\OpenAI\Codex\runtimes\cua_node\
b474a88d5d105afa\bin\node_repl.exe'
startup_timeout_sec = 120

[mcp_servers.node_repl.env]
NODE_REPL_NATIVE_PIPE_CONNECT_TIMEOUT_MS = "1000"
NODE_REPL_NODE_MODULE_DIRS = 'C:\Users\Administrator\AppData\Local\OpenAI\Codex
\runtimes\cua_node\b474a88d5d105afa\bin\node_modules'
NODE_REPL_NODE_PATH = 'C:\Users\Administrator\AppData\Local\OpenAI\Codex\runtim
es\cua_node\b474a88d5d105afa\bin\node.exe'
NODE_REPL_TRUSTED_CODE_PATHS = 'C:\Users\Administrator\.codex;C:\Users\Administ
rator\AppData\Local\OpenAI\Codex\runtimes\cua_node\b474a88d5d105afa\bin\node_mo
dules'
CODEX_HOME = 'C:\Users\Administrator\.codex'
BROWSER_USE_AVAILABLE_BACKENDS = "chrome,iab"
BROWSER_USE_TINYSKY_ENABLED = "1"
NODE_REPL_INSTRUCTIONS_USE_CASE_BROWSER = ""
NODE_REPL_INSTRUCTIONS_USE_CASE_CHROME = ""
BROWSER_USE_CODEX_APP_BUILD_FLAVOR = "prod"
BROWSER_USE_CODEX_APP_VERSION = "26.901.51231"
NODE_REPL_TRUSTED_SERVICES = '{"browser":"C:/Users/Administrator/.codex/plugins
/cache/openai-bundled/browser/26.901.51231/scripts/browser-service.mjs","sky":"
@oai/sky/service"}'
SKY_CUA_NATIVE_PIPE = "1"
SKY_CUA_NATIVE_PIPE_DIRECTORY = '\\.\pipe\codex-computer-use-402c5fd1-ba1d-4a56
-b0ac-082e67607983'
CODEX_CLI_PATH = 'C:\Users\Administrator\AppData\Local\OpenAI\Codex\bin\8e5b693
2251c2c1c\codex.exe'

[windows]
sandbox = "elevated"

[projects.'c:\users\administrator\documents\codex\2026-09-08\new-chat']
trust_level = "trusted"

[projects.'c:\users\administrator\documents\codex\2026-09-08\bang-ow']
trust_level = "trusted"

[projects.'e:\娴兼顣稊瀣啀鐠佹崘顓搁獮鍐插酱']
trust_level = "trusted"

[projects.'e:\娴兼顣稊瀣啀ai鐠у鍏橀獮鍐插酱鎼存洖楠?]
trust_level = "trusted"

[projects.'c:\users\administrator\documents\codex\2026-09-08\hong']
trust_level = "trusted"

[projects.'c:\users\administrator\documents\codex\2026-09-08\bang-wo']
trust_level = "trusted"

[projects.'c:\users\administrator\documents\codex\2026-09-08\xi']
trust_level = "trusted"

[projects.'c:\users\administrator\documents\codex\2026-09-09\post-https-api-api
lio-ai-v1beta']
trust_level = "trusted"

[projects.'c:\users\administrator\documents\codex\2026-09-09\plugin-creator-c-u
sers-administrator-codex']
trust_level = "trusted"

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '$Out
putEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new(); git -c co
re.quotepath=false ls-files | Select-String -Pattern '"'("'^|/)(target|node_mod
ules|'"\\.git|\\.idea)(/|"'$)|(''^|/)(AGENTS('"\\.override)?\\.md|\\.openai/hos
ting\\.json)"'$'"'; Get-Content -Encoding utf8 -LiteralPath 'docs/06-鍝佺墝绱犳潗涓庡懡鍚
嶆槧灏?md'; Get-ChildItem -LiteralPath docs -Name; Get-ChildItem -LiteralPath scri
pts/brand -Name; Get-Content -Encoding utf8 -LiteralPath .gitignore; Get-ChildI
tem -LiteralPath .github/workflows -File | ForEach-Object { '--- ' + "'$_.Name;
 Get-Content -Encoding utf8 -LiteralPath $_.FullName }' in E:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇
 succeeded in 119ms:
# 浼楀涔嬪锛氬搧鐗岀礌鏉愪笌鍛藉悕鏄犲皠锛圸S-BRAND-001 鍐荤粨绋匡級

> 鏂囨。鐗堟湰锛歏1.1
> 寤虹珛鏃ユ湡锛?026-09-08锛圴1.1 鍚屾棩琛ュ厖绗?9 鑺傛墽琛岃褰曪級
> 浠诲姟渚濇嵁锛歔05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗昡(05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md) 绗?15.2 鑺?ZS-BRAND-
001
> 鏀瑰悕鍓嶄唬鐮佸熀绾匡細`brand-rename-baseline` 鏍囩锛堟彁浜?4ffaaf29锛?
> 鐘舵€侊細鍛藉悕鏄犲皠鍐荤粨锛涢杞敼鍚嶅凡鎸夌 9 鑺傛墽琛岃褰曡惤鍦帮紝鍒嗘壒鑱旈獙涓庡緟鎻愪緵绱犳潗浠嶆寜 05 鏂囨。璺熻釜

## 1. 鍝佺墝渚濇嵁涓庣礌鏉愬彴璐?

| 椤?| 鍐呭 |
|---|---|
| 鍘熷 Logo | [zszj-logo-user-reference.png](assets/brand/zszj-logo-user-referen
ce.png) |
| 灏哄/鏍煎紡 | 1060脳228锛孭NG 8-bit RGBA锛堝甫閫忔槑閫氶亾锛?026-09-08 瀹炴祴锛岄潪 opaque 搴曞浘锛?|
| SHA-256 | `275efbf794ea5e485bf49111d8681170dc3b07e3d858e07fb32be9d7aeaabe5a` 
|
| 鏍￠獙鍛戒护 | `certutil -hashfile docs/assets/brand/zszj-logo-user-reference.png S
HA256` |
| 鍝佺墝涓昏壊 | 娣辨 `#5C381A`锛圧GB 92,56,26锛屾寜鍘熷浘绗旂敾鍙栨牱锛涙渶缁堣壊鍊间互鐢ㄦ埛鎻愪緵鐭㈤噺鍘熺涓哄噯锛?|
| 鍘熷浘澶勭疆 | 鍘熸牱鐣欐。锛屼笉浣滃唴瀹逛慨鏀癸紱琛嶇敓璧勬簮鍙仛閫忔槑搴﹀埄鐢ㄣ€佺瓑姣旂缉鏀句笌寰芥爣鍖鸿鍒囷紝涓嶉噸缁樺晢鏍囥€佷笉鏀瑰彉鏋
勫浘涓庢枃瀛?|

- 鍝佺墝灞曠ず鍚嶏細**浼楀涔嬪**锛涘钩鍙板叏绉帮細**浼楀涔嬪 AI 璧嬭兘骞冲彴**銆?
- 宸ュ晢鍏ㄧО锛?*寰呯敤鎴锋彁渚?*锛屼笉鍑?Logo 鎴栧浘鐗囨帹鏂紱姝ｅ紡椤佃剼銆佸崗璁€佹硶鍔′俊鎭湪鐢ㄦ埛鎻愪緵鍓嶇暀绌烘垨浣跨敤鍗犱綅璇存
槑锛屼笉浠?浼楀涔嬪"绛夊悓浼佷笟娉ㄥ唽鍚嶃€?
- 05 鏂囨。灏嗚鍥炬弿杩颁负"涓嶆槸宸插埗浣滃ソ鐨勯€忔槑鍥?锛屾寚鐨勬槸瀹冧笉鏄彲鐩存帴鐢ㄤ簬鍚勭鐨勫灏哄鎴愬搧璧勬簮锛涘疄娴嬪叾鑳屾櫙涓洪€
忔槑锛屽彲鐩存帴浣滀负妯増閫忔槑绱犳潗浣跨敤锛屼絾浠嶇己鍥炬爣銆佹繁娴呰儗鏅€傞厤涓庨珮鍒嗚鲸鐜囧彉浣擄紙瑙佺 6 鑺傦級銆?

## 2. 鍛藉悕绾﹀畾鍐荤粨

- 浜у搧浠ｇ爜涓诲墠缂€锛氬皬鍐?`zszj`锛汮ava 绫诲悕鍓嶇紑 `Zszj`锛涚幆澧冨彉閲忓墠缂€ `ZSZJ_`銆?
- 鍏佽鐨勭櫥璁扮缉鍐欎粎闄愭湰鏂囦欢绗?4.3 鑺傚垪鏄庨」锛涢櫎鐧昏椤瑰涓嶅緱鏂板 `zs` / `Zs` / `ZS_` / `yd` 绛夌煭鍓
嶇紑锛屼笉寰椾笌 `zszj` 娣风敤琛ㄨ揪鍚屼竴姒傚康銆?
- 鐢ㄦ埛鍙ｈ堪鐨?"youdao" 鍦ㄥ綋鍓嶆簮鐮佷腑鐨勫疄闄呮嫾鍐欎负 `yudao`锛屾槧灏勪互婧愮爜瀹為檯鎷煎啓涓哄噯锛沗youdao` 涓€骞剁撼鍏
ユ畫鐣欐壂鎻忋€?

### 2.1 鍐荤粨鍊?

| 鍛藉悕灞傛 | 鍐荤粨鍊?| 鏃у€硷紙杩佺Щ鍓嶏級 |
|---|---|---|
| Java 鍖呮牴 | `cn.zszj` | `cn.iocoder.yudao` |
| Maven GroupId | `cn.zszj` | `cn.iocoder.boot` |
| Maven artifactId 鏍?| `zszj` / `zszj-server` / `zszj-module-*` / `zszj-framewo
rk` / `zszj-dependencies` / `zszj-spring-boot-starter-*` | `yudao` / `yudao-ser
ver` / `yudao-module-*` 绛?|
| Java 绫诲墠缂€ | `Zszj*`锛堝 `ZszjQuartzAutoConfiguration`锛?| `Yudao*` |
| Spring 搴旂敤鍚?鍙墽琛?Jar | `zszj-server` | `yudao-server` |
| 浜у搧閰嶇疆鍓嶇紑 | `zszj.*` | `yudao.*` |
| 鐜鍙橀噺鍓嶇紑 | `ZSZJ_*` | `ZS_*`锛堝惈 `ZS_APPLICATION_YUDAO_*`锛?|
| Web 浜у搧鍖呭悕 | `zszj-admin-web` | `yudao-ui-admin-vue3` |
| 绉诲姩浜у搧鍖呭悕 | `zszj-miniapp` | `unibest`锛堜笂娓告ā鏉垮寘鍚嶏級 |

## 3. 鏃у悕鈫掓柊鍚嶆槧灏勬€昏〃锛堝惈娑堣垂鑰呬笌鎵规锛?

| # | 鏃у悕 | 鏂板悕 | 涓昏鏂囦欢/鏁版嵁娑堣垂鑰?| 鎵ц浠诲姟 | 鍏煎/鍥為€€鏂瑰紡 |
|---|---|---|---|---|---|
| 1 | `cn.iocoder.yudao`锛堝惈 `cn/zszj` 璺緞褰㈠紡锛?| `cn.zszj` | 鍏ㄩ儴 Java 婧愮爜 import
/package銆丮apper XML namespace/resultType銆乣META-INF/spring/*.imports`銆丼PI/鍙嶅皠瀛楃
涓层€乴ogback logger銆佹祴璇?| ZS-BRAND-002 | 鏃犺繍琛屽瓨閲忥紝鐩存帴鍒囨崲锛汢02/B03 鐪熷疄 PG 鍚姩澶嶉獙 |
| 2 | `cn.iocoder.boot` | `cn.zszj` | 鍏ㄩ儴 pom.xml 鐨?groupId/parent/渚濊禆鍧愭爣 | ZS-
BRAND-002 | 鏈湴浠撳簱闇€娓呯悊鏃?`cn.iocoder.boot` 鍓嶇紑缂撳瓨浜х墿 |
| 3 | `yudao-module-*`銆乣yudao-framework`銆乣yudao-dependencies`銆乣yudao-server`銆乣y
udao-ui`锛堢洰褰曚笌 artifactId锛?| `zszj-module-*` 绛?| 鐖跺瓙 POM modules銆佷緷璧栧潗鏍囥€丏ocker
file銆佽剼鏈€両DE 宸ョ▼銆佹枃妗ｉ摼鎺?| ZS-BRAND-002 | `git mv` 淇濈暀鍘嗗彶锛涙棫鐩綍鍚嶅湪鏄犲皠琛ㄧ暀鐥?|
| 4 | `Yudao*` 绫诲悕锛?9 涓枃浠讹級 | `Zszj*` | 绫绘枃浠跺悕銆乮mport銆佽嚜鍔ㄩ厤缃敞鍐屾枃浠躲€佸弽灏?鏂囨。瀛楃
涓?| ZS-BRAND-002 | `git mv` + 鍏ㄩ噺寮曠敤鏇挎崲锛涜閰嶆祴璇曞楠?|
| 5 | 閰嶇疆閿?`yudao.*` | `zszj.*` | `application*.yaml`銆乣@ConfigurationProperties
`銆乣@ConditionalOnProperty`銆乤dditional-spring-configuration-metadata銆佹祴璇曢厤缃?| ZS
-BRAND-002 | 鏂版棫閿笉寰楀悓鏃剁敓鏁堬紱鍚姩鏃朵互鏂伴敭涓哄噯锛屾棤鍏煎鏈燂紙褰撳墠鏃犻儴缃插瓨閲忥級 |
| 6 | `spring.application.name=yudao-server`銆丣ar 鍚嶃€乣/zszj-server` 宸ヤ綔鐩綍 | `zs
zj-server` | Dockerfile銆侀儴缃茶剼鏈€丷ocketMQ/Kafka 鍒嗙粍娲剧敓鍚嶃€丄ctuator 鏄剧ず | ZS-BRAND
-002 | 閮ㄧ讲妯℃澘鍚屾锛涙棤瀛橀噺瀹炰緥闇€瑕佽縼绉?|
| 7 | 鐜鍙橀噺 `ZS_*`锛堝惈 `ZS_APPLICATION_YUDAO_*`銆乣ZS_APPLICATION_LOCAL_YUDAO_*`锛
?| `ZSZJ_*`锛坄ZSZJ_APPLICATION_*` 绛夛級 | `application*.yaml` 鍗犱綅绗︺€侀儴缃叉ā鏉裤€佽繍琛屾墜鍐
?| ZS-BRAND-002 | 鏃犲凡閮ㄧ讲鐜锛屼竴娆℃€у垏鎹紱`ZS_` 涓嶄繚鐣欏吋瀹瑰埆鍚?|
| 8 | 浜у搧鍙涓婃父婕旂ず鍩?`dashboard.yudao.iocoder.cn`銆乣test.yudao.iocoder.cn` 绛夐厤缃?绉
嶅瓙鍙栧€?| `dashboard.zszj.local`銆乣static.zszj.example.com` | `application.yaml`銆佺
瀛愮ず渚嬪浘鐗?澶村儚 URL | ZS-BRAND-002 / ZS-BRAND-004 | 鍊间负婕旂ず鍗犱綅锛涗笉閰嶇疆鐪熷疄瑙ｆ瀽 |
| 9 | Web 鍖呭悕 `yudao-ui-admin-vue3`銆佹爣棰?鑺嬮亾绠＄悊绯荤粺"銆佽妺閬?Logo/favicon | `zszj-adm
in-web`銆佷紬澧呬箣瀹跺搧鐗岃祫婧?| `package.json`銆乣pnpm-lock.yaml`銆乣.env*`銆乣index.html`銆丩og
o 缁勪欢銆乫avicon | ZS-BRAND-003.A | 鏉ユ簮鍏冩暟鎹紙author/repository/涓婃父鐗堟湰瀛楁锛変繚鐣?|
| 10 | 绉诲姩鍖呭悕 `unibest`銆佸簲鐢ㄦ爣棰樸€乣src/static/logo.svg`銆乣src/components/zszj-ui` 
| `zszj-miniapp`銆佷紬澧呬箣瀹惰祫婧愩€乣zszj-ui` | `package.json`銆乣manifest.config.ts`銆乣en
v/*`銆佺粍浠跺紩鐢?| ZS-BRAND-003.A | unibest 涓婃父鍏冩暟鎹瓧娈典繚鐣欙紙瑙?4.3 渚嬪锛?|
| 11 | 绉嶅瓙/瀛橀噺鏁版嵁涓棫鏍囪瘑锛氱鎴峰悕"鑺嬮亾婧愮爜"銆佹紨绀鸿处鍙?`yudao`銆乣yudao_demo01_contact` 绛夋紨
绀鸿〃鍚嶃€佸叕鍛?澶村儚/OAuth 婕旂ず绱犳潗 URL銆佽彍鍗?瀛楀吀/鍙傛暟绉嶅瓙涓殑浜у搧鍙鏃у悕 | `浼楀涔嬪`銆乣zszj`銆乣zs
zj_demo01_contact`銆佷腑鎬у崰浣?| `sql/**` 鍏ㄦ柟瑷€绉嶅瓙銆佹柊瑁呭簱銆佸崌绾у簱锛堣縼绉昏剼鏈級 | ZS-BRAND-0
04 | 鏂拌鑴氭湰鐩存帴鏇存柊锛涘瓨閲忓簱鎻愪緵骞傜瓑鍗囩骇鑴氭湰锛涘凡鎵ц杩佺Щ鑴氭湰涓嶆敼鍐?|
| 12 | 浠ｇ爜鐢熸垚妯℃澘涓庣敓鎴愮墿榛樿鍖呭悕锛坄yudao-module-infra` codegen 璧勬簮鍙婃柇瑷€锛?| 鎸?#1/#3/#
4 鍚屾 | codegen 妯℃澘銆乣CodegenEngine*Test` 鏂█銆佺敓鎴愬墠鍚庣浠ｇ爜 | ZS-BRAND-005 | 妯℃澘涓庢
柇瑷€鍚屾壒鏇挎崲锛屼繚璇佹祴璇曞彛寰勪竴鑷?|
| 13 | 鏂囨。涓?README 涓寚鍚戞棫璺緞鐨勯摼鎺?| 鎸囧悜鏀瑰悕鍚庡疄闄呰矾寰?| README銆乨ocs/01/02/03/05 鐨勯摼鎺
ョ洰鏍囦笌鍐呰仈璺緞 | 鍚勬敼鍚嶄换鍔″悓鎵?| docs/04 鍘嗗彶鎶ュ憡涓庡叾蹇収娓呭崟涓嶉噸鍐?|

## 4. 淇濈暀鏃у悕鐨勪緥澶栨竻鍗曪紙璺緞 + 鍘熷洜 + 鎾ら櫎鏉′欢锛?

### 4.1 蹇呰淇濈暀锛堟潵婧愪笌鍘嗗彶璇佹嵁锛屼笉璁炬挙闄ゆ湡锛?

| 鑼冨洿 | 鍘熷洜 |
|---|---|
| `reference/donors/**` | 闅旂渚涗綋蹇収锛屼繚鎸佸師鏍锋槸鏉ユ簮璇佹嵁鐨勪竴閮ㄥ垎 |
| `third_party/*.json` | 杩佸叆娓呭崟銆佸噣鍖栬褰曚笌鍘嗗彶娴嬭瘯姹囨€伙紝浣滀负 2026-09-08 瀵煎叆鍩虹嚎璇佹嵁鍐荤粨 
|
| `docs/04-婧愮爜杩佸叆涓庨獙璇佹姤鍛?md` | 鍘嗗彶鎶ュ憡涓嶉噸鍐欙紱鍏朵腑鏃ц矾寰勪粎鍦ㄨ鏂囨。鍐呬繚鐣?|
| 鍚勭骇 `LICENSE`銆乣THIRD_PARTY_NOTICES.md`銆乣package.json` 涓笂娓?author/repository/
homepage/unibest-* 绛夊厓鏁版嵁瀛楁 | 鐗堟潈涓庢潵婧愮讲鍚嶈姹傦紱"鏃犳棫瀛楃涓?涓嶅緱浣滀负鍒犻櫎鐗堟潈鐨勭悊鐢?|
| 婧愮爜娉ㄩ噴涓殑涓婃父浣滆€呯讲鍚嶏紙濡?`@鑺嬭壙`锛変笌涓婃父鍙傝€冮摼鎺ワ紙濡?`doc.iocoder.cn`銆乣github.com/Yuna
iV/ruoyi-vue-pro`銆乣gitee.com/yudaocode/*`銆乣static.iocoder.cn/*` 绱犳潗鍦板潃锛?| 鏉ユ簮璇存
槑锛涗粎淇濈暀鍦ㄦ敞閲娿€乣.http` 绀轰緥涓庝笂娓稿厓鏁版嵁涓紝涓嶈繘鍏ヤ骇鍝佸彲瑙佺晫闈?|

### 4.2 蹇呰淇濈暀锛堟湭鍚敤涓婃父妯℃澘璧勪骇锛?

| 鑼冨洿 | 鍘熷洜 | 鎾ら櫎/鍚敤鏉′欢 |
|---|---|---|
| `services/zhongshu-core/zszj-ui/**` 鍐呴儴鍏ㄩ儴鍐呭锛堝師 `zszj-ui-admin-vben/vue2/vue
3/uniapp`銆乣yudao-ui-mall-uniapp`锛?| 鏈惎鐢ㄧ殑涓婃父鍓嶇妯℃澘璧勪骇锛涘鍣ㄧ洰褰曟敼鍚嶄负 `zszj-ui`锛屽唴
閮ㄤ繚鐣欎笂娓稿師璨岋紝涓嶅仛鍗婂鏀瑰悕 | 鎸?ZS-BRAND-005/鍚庣画鎵规瀹為檯鎽樺彇鏃讹紝鍏堟墽琛屽悓涓€鍛藉悕鏄犲皠鍐嶅惎鐢?|

### 4.3 闄愭湡鍏煎锛堢櫥璁扮缉鍐?渚嬪锛屽惈鎾ら櫎鏉′欢锛?

| 渚嬪 | 璺緞 | 鍘熷洜 | 鎾ら櫎鏉′欢 |
|---|---|---|---|
| 灏忕▼搴忕粍浠跺墠缂€ `yd-*`锛堢粍浠跺悕銆佹牱寮忕被銆乪asycom 寮曠敤锛?| `apps/zhongshu-miniapp/src/comp
onents/zszj-ui/**` 鍙婂叏閮ㄤ娇鐢ㄧ偣 | 鏀瑰姩闈㈢害 1400 澶勪笖褰撳墠鏃犵Щ鍔ㄧ鏋勫缓楠岃瘉鎵嬫锛圸S-CLIENT-005.
C 鏈畬鎴愶級锛涘厛瀹屾垚鐩綍绾ф敼鍚嶏紝鍓嶇紑鐧昏涓哄吋瀹归」 | 绉诲姩绔?H5/寰俊鏋勫缓闂ㄧ锛圸S-CLIENT-005锛夊彲鐢ㄥ悗锛屾寜鎵
规鏇挎崲涓?`zszj-*` 骞舵挙閿€鏈緥澶?|
| `apps/*/鈥?image/common/zszj-*.png` 涓婃父鏋舵瀯璇存槑鍥?| 涓ょ `.image` 鐩綍 | 涓婃父 README
 鎻掑浘绱犳潗锛屼笉杩涘叆浜у搧杩愯鐣岄潰 | 鍥捐绉婚櫎鎴栨浛鎹负浜у搧绱犳潗鏃舵挙閿€ |

## 5. 澶栭儴鏍囪瘑鎺掗櫎娓呭崟锛堜笉鑷姩鏀瑰姩锛?

浠ヤ笅鏍囪瘑鍗充娇鍚棫鍚嶏紝涔熶笉鍦ㄦ敼鍚嶄换鍔″唴鑷姩鍙樻洿锛涗换浣曞彉鏇撮』鍗曠嫭绔嬮」骞惰幏鎵瑰噯锛?

1. Git 杩滅▼浠撳簱锛坄phlong026/zhongshu-ai-platform`锛変笌鏈湴宸ヤ綔鍖虹洰褰曞悕 `E:\浼楀涔嬪AI璧嬭兘骞冲
彴搴曞骇`銆?
2. 浜у搧椤跺眰鐩綍 `services/zhongshu-core`銆乣apps/zhongshu-admin-web`銆乣apps/zhongshu-
miniapp`锛坄zhongshu-*` 鏄惁缁熶竴涓?`zszj-*` 寰呯敤鎴锋壒鍑嗗悗鍙﹁瀹炴柦锛屾湰涓撻」涓嶈縼绉伙級銆?
3. 宸叉敞鍐屽井淇?AppID/灏忕▼搴?ID銆佹湭鏉?Bundle ID / Android 鍖呭悕銆佹敮浠樹笌寮€鏀惧钩鍙板晢鎴锋爣璇嗐€?
4. 澶栭儴鍥炶皟鍩熷悕銆佸凡澶囨鍩熷悕涓庣湡瀹炶В鏋愯褰曘€?
5. `system_tenant.websites` 绛夌瀛愪腑鐨勫閮ㄦ紨绀哄湴鍧€浠呭仛鍗犱綅涓€у寲锛屼笉鎸囧悜浠讳綍鐪熷疄娉ㄥ唽璧勪骇銆?

## 6. 鍝佺墝绱犳潗闇€姹傛竻鍗?

| 闇€姹?| 鐢ㄩ€?| 鐜扮姸 | 鏉ユ簮 |
|---|---|---|---|
| 妯増閫忔槑 Logo锛堟祬/娣辫儗鏅級 | 鐧诲綍椤点€佸睍寮€瀵艰埅 | 鍘熷浘鍗?1060脳228 閫忔槑 PNG锛屽彲鐩存帴娲剧敓 | 鐢ㄦ埛鍘
熷浘 |
| 鏂瑰舰鍥炬爣锛堚墺256px锛変笌 favicon锛?6/32/48锛?| 娴忚鍣ㄦ爣绛俱€佹敹钘忓す銆佸皬绋嬪簭鍥炬爣浣?| **缂?*锛涚敱鍘熷浘宸
︿晶浜戝舰寰芥爣绛夋瘮瑁佸垏娲剧敓 | 鍘熷浘瑁佸垏 |
| 楂樺垎杈ㄧ巼/鐭㈤噺鍘熺锛圫VG/AI锛?| 鎵撳嵃銆佸ぇ灞忋€佹硶鍔′娇鐢?| **缂?*锛涘凡鍚戠敤鎴蜂紭鍏堢储鍙栵紝鏈彁渚涘墠涓嶆搮鑷噸缁?|
 寰呯敤鎴锋彁渚?|
| 鍗曡壊/鍙嶇櫧鍙樹綋 | 娣辫壊瀵艰埅銆佹按鍗?| **缂?*锛涘繀瑕佹椂鐢遍€忔槑鍘熷浘鎸夊搧鐗岃壊閲嶅缓閫忔槑搴︼紝涓嶆敼鍙樺瓧褰?| 鍘熷浘娲剧敓 
|

琛嶇敓鑴氭湰涓庝骇鐗╅殢 ZS-BRAND-003.A 浜や粯锛屾淳鐢熺畻娉曪紙閫忔槑搴﹀埄鐢ㄣ€佺瓑姣旂缉鏀俱€佸窘鏍囧尯瑁佸垏锛夊湪鎻愪氦淇℃伅涓庤剼鏈
敞閲婁腑璁板綍锛屼繚璇佸彲杩芥函涓斾笉鏀瑰彉鍟嗘爣褰㈢姸銆?

## 7. 鏀瑰悕鎵ц杈圭晫涓庤瘉鎹柟寮?

- 鎵ц椤哄簭锛歓S-BRAND-002锛堝悗绔級鈫?ZS-BRAND-003.A锛堜袱绔潤鎬佸搧鐗岋級鈫?ZS-BRAND-006.A锛堟畫鐣欓棬绂侊
紝B01 鏀跺彛锛夆啋 ZS-BRAND-004.A锛堢瀛愪笌瀛橀噺杩佺Щ锛孊02锛夆啋 ZS-BRAND-005锛堜唬鐮佺敓鎴愶紝B02锛夈€俍S-BRA
ND-003.B銆乑S-BRAND-004.B/.C銆乑S-BRAND-006.B 灞?B03/B05/B06 鑱旈獙瀛愰」锛屾寜 16.1 鑺傚墠缃彟琛屾
敹鍙ｃ€?
- 璺緞鏀瑰悕涓€寰?`git mv`锛屾彁浜ゅ唴淇濈暀 Rename 璁板綍锛涙枃鏈浛鎹㈣鍒欎互杩佺Щ鑴氭湰褰㈠紡闅忔彁浜ゅ叆搴擄紝淇濊瘉鍙瀹°€
?
- `scripts/verify-source-copy.mjs` 鏍￠獙鐨勬槸 2026-09-08 瀵煎叆鍩虹嚎锛坄brand-rename-basel
ine` 鍙婁箣鍓嶆湁鏁堬級銆傚搧鐗屾敼鍚嶆壒娆¤惤鍦板悗锛岃妫€鏌ラ拡瀵瑰綋鍓嶅伐浣滃尯蹇呯劧鍑虹幇宸紓锛屽睘棰勬湡锛涘鍏ヨ瘉鎹敱璇ユ爣绛句笌 `t
hird_party/` 娓呭崟鍐荤粨淇濈暀锛屼笉閲嶆柊鐢熸垚娓呭崟鏉ュ啋鍏?鏃犲樊寮?锛屽悗缁互 ZS-BRAND-006 鐨勫搧鐗屽懡鍚嶉棬绂佷綔涓哄綋
鍓嶅伐浣滃尯鐨勬鏌ュ叆鍙ｃ€?
- 鏀瑰悕涓嶅惎鐢ㄤ换浣曟湭鍚敤妯″潡锛屼笉鏀瑰彉妯″潡鐧藉悕鍗曘€佹暟鎹簱鏂硅█鐩爣锛圖-08锛氫粎 PG锛変笌澶栭儴娉ㄥ唽鏍囪瘑銆?
- 鏈満鏆傛棤 JDK/Maven锛圸S-ENG-002 宸ュ叿閾句换鍔¤寖鍥达級锛孼S-BRAND-002 鐨勫共鍑€鏋勫缓銆佷緷璧栨爲涓庣湡瀹?PG 鍚
姩楠屾敹鍦?B01/B02 鐜琛ヨ瘉锛涙湰杞互闈欐€佷竴鑷存€ф鏌ワ紙鍖呭０鏄庝笌璺緞銆丗QCN 寮曠敤鍙В鏋愩€丳OM 鍧愭爣涓€鑷达級鎻愪緵
鍒濇璇佹嵁骞跺瀹炶褰曟湭楠岃瘉椤广€?

## 8. 楠屾敹瀵圭収锛圸S-BRAND-001锛?

| 楠屾敹椤?| 缁撴灉 |
|---|---|
| 鎵€鏈夋嫢鏈夌殑婧愮爜銆佺敓鎴愬櫒銆佽祫婧愬拰杩愯鏍囪瘑閮芥湁澶勭疆鍒嗙被 | 绗?3锝? 鑺傝鐩栧悗绔€佷袱绔€佺瀛愩€佹ā鏉裤€佹枃妗ｄ笌鑴
氭湰锛涙湭鍚敤璧勪骇鎸?4.2 鍒嗙被 |
| 鏃犱袱涓洰鏍囬噸鍚嶆垨鍓嶇紑娣风敤 | 绗?2.1 鑺傚崟涓€涓诲墠缂€锛沗zs`/`yd` 浠呮寜绗?4.3 鑺傜櫥璁帮紱鏄犲皠鐩爣浜掍笉鍐茬獊 |
| 鍘熷浘鏍￠獙涓€鑷淬€佸繀瑕佺礌鏉愭潵婧愬彲杩借釜 | 绗?1 鑺?SHA-256 瀹炴祴涓€鑷达紱绱犳潗闇€姹備笌娲剧敓杈圭晫瑙佺 6 鑺?|
| 鏈幏鍑嗘敼鍔ㄧ殑澶栭儴鏍囪瘑鍜屽巻鍙茶瘉鎹槑纭帓闄?| 绗?5 鑺傛帓闄ゆ竻鍗曪紱绗?4.1 鑺傝瘉鎹喕缁?|
| 涓嶈兘鎶?鏃犳棫瀛楃涓?浣滀负鍒犻櫎鐗堟潈鐨勭悊鐢?| 绗?4.1 鑺傜讲鍚嶄笌璁稿彲淇濈暀 |

## 9. 鎵ц璁板綍锛?026-09-08 棣栬疆寮€鍙戯級

| 浠诲姟 | 鎻愪氦 | 鍏抽敭缁撴灉 | 寰呮壒娆¤ˉ璇?|
|---|---|---|---|
| ZS-BRAND-001 | 887bbc4f | 鏈枃妗ｅ喕缁擄紱鍘熷浘鏍￠獙涓€鑷?| 宸ュ晢鍏ㄧО/鐭㈤噺鍘熺寰呯敤鎴锋彁渚?|
| ZS-BRAND-002 | 43d71fad | 鍚庣 10,059 鏂囦欢鏀瑰悕锛涢潤鎬佷竴鑷存€ф鏌?0 纭€ч棶棰?| 骞插噣鏋勫缓/渚濊禆
鏍?鐪熷疄 PG 鍚姩锛圔01/B02 宸ュ叿閾撅級 |
| ZS-BRAND-003.A | ea580b1f | 涓ょ闈欐€佸搧鐗屼笌鍖呭悕锛沇eb build 閫氳繃銆佸皬绋嬪簭 H5 build 閫氳繃锛泇
ue-tsc 鏃犳柊澧為敊璇紙涓婃父鍩虹嚎 11 涓収鏃э級 | .B 澶氱鑱旈獙锛圔06锛夛紱寰俊鐪熸満锛圸S-CLIENT-005.C锛?|
| ZS-BRAND-004 | 581927d0 | 鍏ㄦ柟瑷€绉嶅瓙鏀瑰悕 + 骞傜瓑鍗囩骇鑴氭湰锛涚湡瀹?PG17 鏂拌/鏃ф牱鏈崌绾?閲嶅杩佺Щ
/澶辫触鍥炴粴楠岃瘉閫氳繃 | .B 缂撳瓨浼氳瘽锛圔03锛夈€?C 浠诲姟浜嬩欢锛圔05锛夎仈楠?|
| ZS-BRAND-005 | 08ad8e78 | 妯℃澘涓庡す鍏风粺涓€銆佸惎鐢?绂佺敤鍒嗙骇鐧昏锛涙畫鐣欐壂鎻?0 | PG 鐢熸垚鏍蜂緥缂栬瘧楠岃
瘉锛圔02锛?|
| ZS-BRAND-006.A | b2c26ea9 | 鍛藉悕闂ㄧ + 鐧藉悕鍗?+ 7 椤规祴璇曪紱鍏ㄤ粨 13,417 鏂囦欢鎵弿 0 杩濊 |
 .B 鑱斿悎鍥炲綊鏀跺彛锛圔06锛夛紱CI 鎺ュ叆锛圸S-OPS-001锛?|

璇勫淇闅忔壒钀藉湴锛氭仮澶嶈鍓嶇紑鏇挎崲璇敼鐨勪笂娓哥讲鍚嶉摼鎺ワ紙github.com/YunaiV/\*銆乬itee.com/zhijianti
anya/\*锛夈€佽ˉ榻?zsxq/cloud/@iocoder 婕旂ず鍩熸紡椤癸紙鐪熷疄 PG 楠岃瘉涓彂鐜帮級銆佹竻鐞?`mall.zszj.ioco
der.cn` 鍗婃敼鍩熴€傛敼鍚嶅悗鐨勯棬绂佸叆鍙ｄ笌蹇収鏍￠獙杈圭晫瑙?[README](../README.md)銆?
assets
reviews
01-搴曞骇浠ｇ爜澶嶇敤涓庢敼閫犳柟妗?md
02-涓€鏈熷簳搴ч渶姹傝鏍间笌寰呭喅绛栧彴璐?md
03-搴曞骇浜屾寮€鍙戦『搴忎笌楠屾敹鏍囧噯.md
04-婧愮爜杩佸叆涓庨獙璇佹姤鍛?md
05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md
06-鍝佺墝绱犳潗涓庡懡鍚嶆槧灏?md
07-璐﹀彿鍞竴鎬т笌缁勭粐浠昏亴鍐崇瓥杈撳叆.md
apply-naming-migration.mjs
apply-naming-migration.test.mjs
brand-naming-allowlist.json
derive-brand-assets.mjs
naming-rules.mjs
sync-doc-links.mjs
verify-backend-naming.mjs
verify-brand-naming.mjs
verify-brand-naming.test.mjs
.DS_Store
.idea/
.vscode/
.env
.env.*
*.key
*.pem
*.p12
*.pfx
*.jks
node_modules/
dist/
target/
.flattened-pom.xml
dist-prod/

# Only the reviewed upstream templates are tracked; real values belong in
# environment variables or ignored, project-local overrides.
!apps/zhongshu-admin-web/.env
!apps/zhongshu-admin-web/.env.dev
!apps/zhongshu-admin-web/.env.local
!apps/zhongshu-admin-web/.env.prod
!apps/zhongshu-admin-web/.env.stage
!apps/zhongshu-admin-web/.env.test
!apps/zhongshu-miniapp/env/.env
!apps/zhongshu-miniapp/env/.env.development
!apps/zhongshu-miniapp/env/.env.production
!apps/zhongshu-miniapp/env/.env.test

# Keep versioned editor recommendations from the complete source snapshots.
!apps/zhongshu-admin-web/.vscode/
!apps/zhongshu-admin-web/.vscode/extensions.json
!apps/zhongshu-admin-web/.vscode/launch.json
!apps/zhongshu-admin-web/.vscode/settings.json
!apps/zhongshu-miniapp/.vscode/
!apps/zhongshu-miniapp/.vscode/extensions.json
!apps/zhongshu-miniapp/.vscode/launch.json
!apps/zhongshu-miniapp/.vscode/settings.json
!apps/zhongshu-miniapp/.vscode/vue3.code-snippets
tools/
--- local-baseline-gates.yml
name: local-baseline-gates

# ZS-OPS-001.A锛氬熀纭€鏋勫缓/鏂囨。娴佹按绾块鏋垛€斺€斾笌鏈湴 `node scripts/ops/run-local-gates.mj
s` 鍚屼竴瑙勫垯銆?
# 浠呰繍琛岄潤鎬?鍗曟祴闂ㄧ锛汸G/澶氱 E2E 闂ㄧ鎸?ZS-OPS-001.B~.E 鎵规鎺ュ叆銆?

on:
  push:
    branches: [main]
  pull_request:

jobs:
  baseline-gates:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 22
      - name: 杩愯鍩虹嚎闂ㄧ锛?-fast锛氳烦杩囬渶鍓嶇渚濊禆鐨勭被鍨嬫鏌ワ紝鍏剁敱 ZS-CLIENT-005.A 鏈湴闂ㄧ瑕嗙
洊锛?
        run: node scripts/ops/run-local-gates.mjs --fast
--- pg-regression.yml
name: pg-regression

# ZS-DB-019.B 鍦板熀锛歅G 鎶€鏈洖褰掑浠讹紙涓€娆℃€?PG17 瀹瑰櫒锛岄獙鍚庤嚜鍔ㄦ竻鐞嗭級銆?
# 涓庢湰鍦?`node scripts/db/run-pg-regression.mjs` 鍚屼竴瑙勫垯銆?
# 瑕嗙洊锛歓S-DB-006 搴忓垪/鎵归噺/缁彿銆?07 瀛楁鏄犲皠涓庨€昏緫鍒犻櫎銆?08 浜嬪姟涓庨攣銆?
#       CFG-002.A 瀛楀吀鍞竴绾︽潫銆丏B-011~015 鏈夌晫鍒犻櫎銆丏B-016/017 缁撴瀯绾ч獙璇併€?

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
      - name: 杩愯 PG 鎶€鏈洖褰掑浠?
        run: node scripts/db/run-pg-regression.mjs

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '$Out
putEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new(); Select-St
ring -LiteralPath '"'docs/05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md' -Pattern 'ZS-BRAND-00[2345
6]|16\\.1|ZS-OPS-001.A' -Context 2,2; Get-Content -Encoding utf8 -LiteralPath s
cripts/ops/run-local-gates.mjs; Get-Content -Encoding utf8 -LiteralPath scripts
/brand/verify-brand-naming.mjs; node scripts/brand/verify-backend-naming.mjs" i
n E:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇
 succeeded in 2321ms:
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:273:- 寮€鍙戣褰曪紙2026-09-09 娣卞锛孋I 鎺ュ叆锛夛細鏂板 [.gi
thub/workflows/pg-regression.yml](../.github/workflow
s/pg-regression.yml)鈥斺€擯G 鍥炲綊濂椾欢浠ュ悓涓€瑙勫垯鍏?CI锛坧ush/PR 瑙﹀彂锛夛紝涓庢湰鍦板叆鍙ｅ叡鐢?run-pg-re
gression.mjs锛涜嚦姝?ZS-DB-019.B 鐨勬墽琛屽叆鍙ｄ笌濂椾欢鍦板熀浜や粯瀹屾瘯锛屽墿浣欎负 B02 
姝ｅ紡鐜鎺堟潈鍚庣殑瀹屾暣鏀跺彛銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:274:- 寮€鍙戣褰曪紙2026-09-09 娣卞锛孼S-DB-019.B 鍦板熀锛夛
細浜や粯 PG 鍥炲綊鑱氬悎鍏ュ彛 `node scripts/db/run-pg-regression.mjs
`鈥斺€斾覆鑱?DB-006/007/008/011~015/CFG-002.A 涓?DB-016/017 缁撴瀯绾х敤渚嬪叡 7 濂椾欢锛屽叏杩囷紙鏈湴涓
?CI 鍚岃鍒欙紝B02 姝ｅ紡鎺ュ叆寰呯幆澧冩巿鏉冿級銆?
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:275:- 寮€鍙戣褰曪紙2026-09-09 娣卞锛宻ystem 妯″潡鍩虹嚎淇锛
夛細system 娴嬭瘯濂椾欢棣栬疆鍏ㄩ噺杩愯锛?71 鐢ㄤ緥锛夋毚闇?8 澶勫け璐ワ紝閫愰」 triage锛? 澶勪负鍝佺墝鎵规
鐨勬煡璇?绉嶅瓙鍚嶇О涓嶅绉帮紙SmsChannel/SmsTemplate/SocialClient/Tenant 鍒嗛〉鐢ㄤ緥涓?TenantPacka
ge 涓ゅ瑁呴厤闂锛屽悗鑰呭凡浜?b2716b47 淇锛夆€斺€旀煡璇晶涓庣瀛愪晶宸插叏閮ㄥ榻愪负浼楀涔嬪锛?
1 澶勪负涓婃父鏃㈡湁澶辫触锛圕odegenEngineUniappTest#testExecute_treeSearch锛屽凡鎸夋柟娉曠骇鎺掗櫎鐧昏锛夈€
傚榻愬悗 system/infra/common 涓夋ā鍧楁祴璇曞叏缁匡紙BUILD SUCCESS锛夈€?19
.B 鍓╀綑鏀跺彛锛欻2鈫扨G 鏂硅█鍥炲綊鍐崇瓥涓?CI 鎺ュ叆銆傚緟楠屾敹璇存槑锛?A 鐨勫畬鏁撮獙鏀讹紙Maven 娴嬭瘯閾炬帴鍏ヤ笌 ZS-DB-01
9.B 鐨?PG 鎶€鏈洖褰掑浠讹級渚濊禆 ZS-DB-001.B 鐪熷疄杩炴帴涓?B02 宸ュ叿閾撅紝鎸?16.1 鍓嶇疆鍙?
琛屾敹鍙ｏ紱鏈す鍏蜂负鏈湴鍏堣浜や粯浠讹紝绂佹鐢ㄥ叾鏇夸唬 H2鈫扨G 杩佺Щ鍐崇瓥銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:276:
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:277:### ZS-DB-020锛氳瘎瀹℃煡璇㈢储寮曘€佹壒閲忔洿鏂颁繚鎶や笌骞跺彂浣跨敤
瑙勮寖
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:304:### ZS-SEC-001锛氭敹绱ц法绉熸埛璁块棶鐨勬潈闄愯烦杩囨満鍒?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:305:
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:306:- 鍏宠仈锛欶ND-AUTH-001/002/003/004/006/008銆丏-0
9锛沇P-07/08/09锛汢03/B08锛堟寜瀛愰」鐙珛鏀捐锛夈€備紭鍏堢骇 P0锛涚被鍒?鏀归€狅紱鐘舵€?
寰呭紑鍙戯紱鍓嶇疆 鎸夌 16.1 鑺傚悇瀛愰」锛岀姝㈠皢鍚庣疆瀛愰」浣滀负鏃╂湡鎵规鍓嶇疆銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:307:- 浼楀瑕佹眰涓庣幇鐘讹細瀹㈡埛绔笉鑳戒吉閫犱富浣撴垨鍊熸眹鎬绘潈闄愯幏寰楁晱鎰
熸槑缁嗐€俒璺ㄧ鎴锋嫤鎴櫒][E34] 鍦ㄦ嫢鏈?system:tenant:visit 鍚庤缃?visitTenantId 
鍜屽綋鍓嶇鎴凤紱[SecurityFrameworkUtils][E31] 鍒ゅ畾鍒囨崲鍚庯紝[瀹夊叏鏈嶅姟][E35] 鐨勬潈闄愩€佽鑹层€乻cope 
妫€鏌ヨ繑鍥?true锛孾鏁版嵁鏉冮檺澶勭悊鍣╙[E36] 涓嶈拷鍔犳暟鎹寖鍥淬€傝璺緞鏈夋潈闄愬墠缃紝涓嶆槸鏅€氱敤鎴锋棤鏉′欢鍙?
鐢紱鐩爣绉熸埛鏈夋晥鎬т篃鏈湪姝ゅ垏鎹㈠閲嶆銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:308:- 璋冩暣锛氬簳搴ч粯璁ゅ叧闂湭缁忔壒鍑嗙殑璺ㄧ鎴锋祻瑙堣兘鍔涳紝鍓嶅悗绔叡鍚
屾敹鍙ｏ紱鑾锋壒鍚庝互鏈嶅姟绔巿鏉冭褰?绛栫暐闄愬埗鐩爣绉熸埛銆佸璞°€佸姩浣溿€佸瓧娈靛拰鏈夋晥鏈燂紝浠嶆墽琛屽繀瑕佹巿鏉冧笌鐩爣鐘舵€佹牎楠岋紝
璁板綍鍘熶富浣撱€佺洰
鏍囥€佺悊鐢卞拰缁撴灉銆備笉寰楃畝鍗曟妸鎵€鏈夎烦杩囨敼鎴愬師绉熸埛 RBAC 鑰岃褰撳畬鏁磋法缁勭粐鏂规銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:309:- 楠屾敹锛氭棤璁块棶鏉冮檺銆佷吉閫犲ご銆佸仠鐢?鏃犳晥鐩爣鍧囨嫆缁濓紱鍗充娇鍏峰
璁块棶鍏ュ彛鏉冮檺锛屼篃涓嶈兘鑷姩鑾峰緱鎵€鏈夌洰鏍囧姩浣?鏁忔劅瀛楁锛涙巿鏉冩挙閿€涓庝笂涓嬫枃娓呯悊鏈夋晥銆傝幏鎵逛笟鍔＄粍缁囨柟妗堝悗琛ュ厖姝ｅ悜
鐭╅樀锛屼笉灏嗗叧
闂棫鑳藉姏褰撲綔涓氬姟鎺堟潈宸插畬鎴愩€?
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:310:- 鍒嗘壒浜や粯锛氳绗?16.1 鑺?ZS-SEC-001.A銆乑S-SEC-00
1.B锛涘瓙椤瑰垎鍒櫥璁拌瘉鎹紝鍏ㄩ儴閫傜敤瀛愰」楠屾敹鍚庢墠鑳藉皢鏈富浠诲姟鏍囦负宸查獙鏀躲€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:311:- 寮€鍙戣褰曪紙2026-09-09锛孼S-SEC-001.A 榛樿鍏抽棴璺ㄧ
鎴锋潈闄愯烦杩囷級锛氫互**閰嶇疆闂ㄦ帶榛樿鍏抽棴**鏀跺彛璺ㄧ鎴?visit 瓒婃潈鏀惧ぇ閾撅紝鍓嶅悗绔叡鍚屾敹鍙ｏ紝鏃?D-0
9 渚濊禆銆?*鍗曚竴鎺у埗鐐?*锛欸rep 纭 [璺ㄧ鎴锋嫤鎴櫒][E34] `TenantVisitContextInterceptor.preH
andle` 鏄叏浠撳敮涓€ `setVisitTenantId` 鐐癸紝闂ㄦ帶瀹冨嵆浠庢簮澶存帎鏂暣鏉℃斁澶?
閾锯€斺€攙isitTenantId 姘镐笉璁剧疆鍒?[SecurityFrameworkUtils][E31].skipPermissionCheck() 
鎭?false銆乕瀹夊叏鏈嶅姟][E35] 鏉冮檺/瑙掕壊/scope 妫€鏌ユ仮澶嶆甯搞€乀enan
tContextHolder 涓嶅垏鎹㈠垯 [鏁版嵁鏉冮檺澶勭悊鍣╙[E36] 鏁版嵁鑼冨洿姝ｅ父鐢熸晥銆?*鏈敼** skipPermissionChec
k/SecurityFrameworkServiceImpl/DeptDataPermissionR
ule锛岃閬?line 308 璀﹀憡鐨勩€岀畝鍗曟妸璺宠繃鏀规垚鍘熺鎴?RBAC 鑰岃褰撳畬鏁磋法缁勭粐鏂规銆嶉櫡闃便€?*鍚庣**锛氭柊澧?`T
enantProperties.visitEnable`锛坄zszj.tenant.visit-enable`锛岄粯
璁?false锛夛紱闂ㄦ帶缃簬銆寁isitTenantId==褰撳墠绉熸埛銆嶆棭杩斿洖涔嬪悗銆乴oginUser 绌烘鏌ヤ箣鍓嶏紝鏁呴粯璁ゅ叧闂椂浠讳綍
鍒囨崲鍒颁笉鍚岀洰鏍囩鎴风殑璇锋眰锛堢櫥褰曟垨鍖垮悕銆佸惈浼€犲ご銆佸惈鎸佹棫 `system:tenant:visit` 鏉冮檺鑰咃級涓€
寰?403銆岃法绉熸埛璁块棶鑳藉姏鏈惎鐢紝绂佹鍒囨崲绉熸埛銆嶅苟 log.warn 杩借釜銆?*闂ㄦ帶鏄厤缃紑鍏宠€岄潪纭垹闄?*锛氭樉寮?`vi
sit-enable=true` 鍙仮澶嶆棫閾捐矾锛堝緟 ZS-SEC-001.B 浠ュ彈鎺ф巿鏉冩浛鎹級銆?*鍓嶇**锛氫袱绔?
base `.env` 鏂板 `VITE_APP_TENANT_VISIT_ENABLE=false`锛堟棤 mode 瑕嗙洊锛屽叏妯″紡榛樿鍏抽棴锛夛紝
admin-web `service.ts`/miniapp `interceptor.ts` 
闂ㄦ帶 visit-tenant-id 澶存敞鍏ャ€乣ToolHeader.vue`/`user/index.vue` 闂ㄦ帶鍒囨崲鍏ュ彛 UI锛宎dmin-
web `types/env.d.ts` 琛ョ被鍨嬪０鏄庯紱榛樿鍏抽棴鏃跺墠绔笉鍙戝ご銆佸悗绔害鎷掔粷锛屽弻閲?
鏀跺彛銆?*澶瑰叿**锛氱炕杞?ZS-SEC-012.A 濡傚疄鏆撮湶鐨?2 涓?skip=true 鍩虹嚎鐢ㄤ緥鈥斺€擿crossTenantRejecte
dNoPermissionWhenDisabled`(t1-admin+visit=T2)銆乣c
rossTenantRejectedWithOldPermissionWhenDisabled`(t1-visitor 鎸?system:tenant:vis
it+visit=T2) 鍧囩敱 200/skip=true 鏀剁揣涓?403銆岃法绉?
鎴疯闂兘鍔涙湭鍚敤锛岀姝㈠垏鎹㈢鎴枫€嶏紱鏂板 `CrossTenantVisitEnabledFixtureTest`锛坄@SpringBoot
Test properties=zszj.tenant.visit-enable=true`锛? 鐢?
渚嬭瘉鏄庨棬鎺т负閰嶇疆寮€鍏筹細鑾锋壒 visitor 鍒囨崲鈫掓棫鏀惧ぇ琛屼负(skip=true锛屽緟 .B 鏇挎崲)銆佹棤鏉冮檺鑰呪啋浠?403銆屾偍
鏃犳潈鍒囨崲绉熸埛銆嶏紙鍘熸潈闄愭牎楠屾湭澶辨晥锛夈€?*楠岃瘉**锛歁aven `-pl :zszj-spring-boot-sta
rter-biz-tenant "-Dtest=SecurityFilterChainFixtureTest,CrossTenantVisitEnabledF
ixtureTest" test` 鏈湴 BUILD SUCCESS锛孴ests
 run: 33, Failures: 0, Errors: 0, Skipped: 0锛?026-09-09T13:26:09+08:00锛?1 fixtu
re + 2 闂ㄦ帶璇佹槑锛夛紱鍓嶇 admin-web `verify-ts-bas
eline.mjs` 閫氳繃锛坈urrentErrors 11 = baseline 11锛宯ewErrors 0锛夈€乵iniapp `vue-tsc --
noEmit` 0 閿欒銆?*瀵归綈楠屾敹**锛坙ine 308/309锛夛細鏅€氬ご(鏃?
visit)/浼€犲ご/鎸佹棫 visit 鏉冮檺鍧囦笉鑷姩鏀惧ぇ鑼冨洿锛堥粯璁?403 鎷掔粷锛夛紝鍓嶅悗绔叡鍚屾敹鍙ｏ紝鏃?D-09 渚濊禆锛坙in
e 916 閫€鍑烘潯浠讹級銆?*寰呴獙鏀惰鏄?*锛氭湰瀛愰」浠呭叧闂棫鏀惧ぇ鑳藉姏锛宭ine 309 鏄庣‘銆屼笉灏嗗叧闂棫鑳藉姏褰撲綔涓?
鍔℃巿鏉冨凡瀹屾垚銆嶏紱鑾锋壒涓氬姟缁勭粐鏂规(D-09)鍚庣殑鍙楁帶璺ㄧ粍缁囨巿鏉冿紙鏈嶅姟绔巿鏉冭褰?绛栫暐闄愬埗鐩爣绉熸埛/瀵硅薄/鍔ㄤ綔/瀛楁
/鏈夋晥鏈?+ 鐩爣鐘舵€佹牎楠?+ 鍘熶富浣?鐩爣/鐞嗙敱/缁撴灉瀹¤锛変笌姝ｅ悜鎺堟潈鐭╅樀褰?ZS-SEC-001.B銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:312:
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:377:### ZS-SEC-011锛氭槑纭槻閲嶅鎻愪氦涓庢寔涔呭寲骞傜瓑鐨勬帴鍙ｈ竟鐣
?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:378:
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:379:- 鍏宠仈锛氫竴鏈熻寖鍥粹€滃箓绛夆€濄€丗ND-CLIENT-001锛沇P-11/
14锛汢03/B05锛堟寜瀛愰」鐙珛鏀捐锛夈€備紭鍏堢骇 P1锛涚被鍒?鏀归€狅紱鐘舵€?寰呭紑鍙戯紱鍓嶇疆 鎸夌 16.1 鑺傚悇
瀛愰」锛岀姝㈠皢鍚庣疆瀛愰」浣滀负鏃╂湡鎵规鍓嶇疆銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:380:- 浼楀瑕佹眰涓庣幇鐘讹細[IdempotentAspect][E49] 鎻愪緵 
Redis 绐楀彛閿佸拰鍙€夊紓甯稿垹閿紱[榛樿 Key 瑙ｆ瀽鍣╙[E58] 鎸夋柟娉曚笌鍙傛暟璁＄畻閿紝娌℃湁鑷姩鍔犲叆
鏈嶅姟绔鎴?涓讳綋锛涜鑳藉姏涓嶇瓑浜庤秴鏃堕噸璇曘€佽繘绋嬮噸鍚悗鍙繑鍥炲師涓氬姟缁撴灉锛孲ystem/Infra 鏈疆妫€绱篃鏈鏈夋晥娉ㄨ
В浣跨敤銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:381:- 璋冩暣锛氫负鏄庣‘闇€瑕佺殑鍐欏叆瀹氫箟骞傜瓑澶淬€佹湇鍔＄涓讳綋/绉熸埛/鍔
ㄤ綔鑼冨洿銆佽姹傛憳瑕併€佸悓閿笉鍚屽弬鏁板啿绐佸拰缁撴灉澶嶇敤鍚堝悓锛涢槻杩炵偣涓庢寔涔呭寲骞傜瓑鍒嗗紑璇存槑銆備粎涓哄疄闄呮帴鍙ｉ€夋嫨瀹炵幇锛屼笉鍏
ㄩ噺娣诲姞娉ㄨВ锛涗笟
鍔′簨浠?Outbox 鍦?B05/M10 缁嗗寲銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:382:- 楠屾敹锛氬悓涓讳綋鍚岄敭鍚屽弬鏁颁笉浼氶噸澶嶄骇鐢熺粨鏋滐紱鍚岄敭涓嶅悓鍙傛暟鏄
庣‘鍐茬獊锛涗笉鍚屼富浣撲笉璇敤浠栦汉缁撴灉锛涜秴鏃躲€佸苟鍙戙€佽繘绋嬮噸鍚拰缂撳瓨鏁呴殰涓嶄綔瓒呭嚭褰撳墠瀹炵幇鐨勪繚璇侊紱鑾峰噯閲嶈瘯浠嶉噸鏂版牎楠
屾潈闄愶紝涓嶆妸棣栨
鎴愬姛褰撴案涔呮巿鏉冦€?
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:383:- 鍒嗘壒浜や粯锛氳绗?16.1 鑺?ZS-SEC-011.A銆乑S-SEC-01
1.B锛涘瓙椤瑰垎鍒櫥璁拌瘉鎹紝鍏ㄩ儴閫傜敤瀛愰」楠屾敹鍚庢墠鑳藉皢鏈富浠诲姟鏍囦负宸查獙鏀躲€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:384:
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:385:### ZS-SEC-012锛氬缓绔嬬湡瀹炶繃婊ゅ櫒閾句笌鍙岀瀹夊叏鍚堝悓鍥炲綊
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:386:
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:387:- 鍏宠仈锛欶ND-AUTH-002/004/008銆丗ND-CLIENT-001銆
丗ND-DOC-002锛沇P-20锛汢03锛堟寜瀛愰」鐙珛鏀捐锛夈€備紭鍏堢骇 P0锛涚被鍒?琛ュ缓锛涚姸鎬?
寰呭紑鍙戯紱鍓嶇疆 鎸夌 16.1 鑺傚悇瀛愰」锛岀姝㈠皢鍚庣疆瀛愰」浣滀负鏃╂湡鎵规鍓嶇疆銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:388:- 浼楀瑕佹眰涓庣幇鐘讹細[杩囨护鍣ㄩ『搴廬[E53]銆乕Security 閾綸[
E29]銆乕璺ㄧ鎴锋嫤鎴櫒][E34] 鍒嗗涓嶅悓娉ㄥ唽灞傦紱鏈妫€鏌?Security/Tenant star
ter 鏃?src/test锛學eb 娴嬭瘯浠呭彂鐜板姞瀵?鑴辨晱绫伙紝灏氭湭鎵惧埌鏈」鐩叏閾惧洖褰掑叆鍙ｃ€傝繖涓嶆槸鏂█鍏朵粬妯″潡瀹屽叏娌℃湁瀹夊
叏鍗曞厓娴嬭瘯銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:389:- 璋冩暣锛氭柊澧炲寘鍚疄闄?Filter銆丮VC 鎷︽埅鍣ㄣ€佹柟娉曞畨鍏ㄤ笌寮
傚父鍑哄彛鐨勯泦鎴愭祴璇曪紝涓嶅彧鐩存帴璋冪敤 Service锛涗娇鐢ㄦ壒鍑嗙殑鎶€鏈处鍙?涓ょ鎴凤紝瑕嗙洊鍏紑/璁よ瘉/鍔ㄤ綔/瀵硅薄鎺堟潈銆丆
ORS銆佷笂涓嬫枃娓呯悊銆佹棩蹇楃粨鏋溿€乼race銆丣SON銆佹枃浠堕敊璇拰寮傛锛涘悓姝?[Web][E51]銆乕绉诲姩绔痌[E52] 鍚堝悓銆傛秹鍙婄
湡瀹?PG/Redis 鐨勫満鏅褰曠幆澧冧笌渚濊禆锛屼笟鍔＄粍缁囨ā鍨嬪緟 D-09 鍚庤拷鍔犮€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:390:- 楠屾敹锛氭湰鍦颁笌 CI 鍚屽叆鍙ｏ紝姝ｅ悜鎴愬姛涓旀墍鏈夋湭鎺堟潈/閿欒璺
緞鎷掔粷骞跺彲杩借釜锛涘畨鍏ㄨ繃婊ゅ櫒涓嶈兘涓烘柟渚挎祴璇曡绂佺敤锛涚己渚濊禆涓嶉潤榛樿烦杩囧悗鎶ラ€氳繃锛涜褰曞浐瀹氭彁浜ゅ拰鎶ュ憡锛屾湭杩愯閮ㄧ讲/
UAT 涓?
鎻愬崌瀵瑰簲鐘舵€併€?
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:391:- 鍒嗘壒浜や粯锛氳绗?16.1 鑺?ZS-SEC-012.A銆乑S-SEC-01
2.B锛涘瓙椤瑰垎鍒櫥璁拌瘉鎹紝鍏ㄩ儴閫傜敤瀛愰」楠屾敹鍚庢墠鑳藉皢鏈富浠诲姟鏍囦负宸查獙鏀躲€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:392:- 寮€鍙戣褰曪紙2026-09-09锛孼S-SEC-012.A 鐪熷疄瀹夊叏閾惧
け璐ュす鍏凤級锛氫氦浠?`SecurityFilterChainFixtureTest`锛? 缁?31 鐢ㄤ緥锛?
Maven 鍏ュ彛 `-pl :zszj-spring-boot-starter-biz-tenant -Dtest=SecurityFilterChainF
ixtureTest test`锛屾湰鍦?BUILD SUCCESS锛孴ests r
un: 31, Failures: 0, Errors: 0, Skipped: 0锛?026-09-09T12:52:10+08:00锛夈€?*澶瑰叿缃簬
 biz-tenant/src/test 鑰岄潪 security**锛歜iz-tena
nt 宸?compile 渚濊禆 security锛屾祴璇曡惤姝や负闆舵柊澧炰緷璧栬竟锛岃閬?security(test)鈫抌iz-tenant(compi
le)鈫抯ecurity(compile) 鐨?Maven reactor 寰幆锛堝惊鐜湪 Sca
nning for projects 闃舵鍗宠妫€娴嬶紝`-pl` 鏃犳硶缁曡繃锛夛紱鐩稿簲鍥為€€ security/pom.xml 涓凡鎴愭渚濊禆
鐨?spring-boot-starter-test锛坰ecurity 宸叉棤 src/test锛夈€?*鐪熷疄
瀹夊叏閾惧惎鐢ㄣ€佹湭绂佺敤浠讳綍杩囨护鍣?*锛歍okenAuthenticationFilter + Spring Security 閾?+ TenantS
ecurityWebFilter + TenantVisitContextIntercepto
r + `@PreAuthorize` 鏂规硶鏉冮檺 + GlobalExceptionHandler 寮傚父鍑哄彛鎸?[杩囨护鍣ㄩ『搴廬[E53] 鍏ㄩ噺瑁
呴厤锛宍zszj.security.mock-enable=false`锛涢噸鍨嬬鎴?鏁版嵁婧?My
Batis/Redis/Quartz 鑷姩閰嶇疆浠?`spring.autoconfigure.exclude` 鎸夌被鍚嶆帓闄わ紙ASM 璇诲厓鏁版嵁涓嶅
姞杞界被锛夈€?*鍙屾妧鏈鎴峰彲鏋勯€?*锛歍ENANT_1=1銆乀ENANT_2=2锛孧EMBER(1
)/ADMIN(2) userType锛孫Auth2TokenCommonApi/PermissionCommonApi/TenantFrameworkSer
vice 浠?Mock 鎻愪緵锛涜ˉ ApiErrorLogCommonApi no
-op bean锛圙lobalExceptionHandler 寮轰緷璧栵紝鐢熶骇鐢?infra 鎻愪緵锛屽す鍏锋棤 infra鈥斺€旂己澶卞嵆涓婁笅鏂囧姞杞
藉け璐ュ苟鏆撮湶锛屼笉闈欓粯璺宠繃锛夈€傝鐩栵細鍏紑(@PermitAll 3)/璁よ瘉(6)/鏉冮檺(@PreAutho
rize 6)/绉熸埛鏍￠獙(6)/璺ㄧ鎴?visit(4)/瀵硅薄鎺堟潈(3)/寮傚父鍑哄彛涓€鑷存€?3)銆?*涓や釜鐪熷疄琛屼负鍙戠幇**锛堟寜鐪熷疄
浠ｇ爜璺緞淇娴嬭瘯棰勬湡骞舵敞閲婏紝闈炴敼浜у搧浠ｇ爜锛夛細鈶?MockHttpServletRequest.getServl
etPath() 榛樿绌轰覆锛岃嚧 [WebFrameworkUtils][E47].getLoginUserType 杩斿洖 null銆乽serType 
鏍￠獙琚烦杩囷紙MEMBER token 钂欐贩杩?/admin-api锛夛紝鐢?MockMvc
BuilderCustomizer 鍏ㄥ眬璁?servletPath=requestURI 杩樺師鐢熶骇 DispatcherServlet 琛屼负锛涒憽 [
TenantSecurityWebFilter][E33] 鎵ц搴忎负銆岀櫥褰曠敤鎴峰厛姣斿 us
er.tenantId 涓?header tenant-id锛堜笉鍖归厤鈫?03銆庢偍鏃犳潈璁块棶璇ョ鎴风殑鏁版嵁銆忥級锛屼粎 user==null 鎴栧尮
閰嶆墠杩?validTenant锛堢鐢?杩囨湡鈫?03銆佹湭鐭モ啋400锛夈€嶏紝鏁呯鎴峰悎娉曟€ф牎楠屾敼鐢ㄦ棤鐧诲綍鐢ㄦ埛鐨?
鍏紑绔偣 `/open/tenant-required` 鎼虹壒娈婄鎴?header 瑙﹁揪 validTenant銆?*鍩虹嚎澶辫触宸叉毚闇?*锛歝r
ossTenantWithVisitPermission 濡傚疄璁板綍鎸?`system:tenan
t:visit` 鍚?SecurityFrameworkUtils.skipPermissionCheck()=true 鐨勮秺鏉冩斁澶х幇鐘讹紙ZS-SEC
-001.A 寰呮敹绱э級锛岀鍚堟湰瀛愰」銆屽厛鍏佽鏆撮湶鍩虹嚎澶辫触銆嶉€€鍑烘潯浠躲€傚榻愰獙鏀剁孩绾匡紙line
 389锛夛細鏈湴鍏ュ彛鍙鐜般€佹鍚戞垚鍔熶笖鎵€鏈夋湭鎺堟潈/閿欒璺緞鎸夌湡瀹?code+msg 鎷掔粷銆佸畨鍏ㄨ繃婊ゅ櫒鏈负鏂逛究娴嬭瘯绂佺
敤銆佺己渚濊禆涓嶉潤榛樿烦杩囥€?*寰呴獙鏀惰鏄?*锛氭湰瀛愰」浠呰鐩栧畨鍏ㄩ摼涓诲共涓庡弻鎶€鏈鎴凤紱CORS/鏂囦欢閿欒/寮傛娲惧彂/trac
e/鐣稿舰
 JSON/[Web][E51]銆乕绉诲姩绔痌[E52] 鍚堝悓鍚屾鍙婄湡瀹?PG/Redis 鍦烘櫙褰?ZS-SEC-012.B锛屼笟鍔＄粍缁囨ā鍨嬪緟 
D-09銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:393:
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:428:### ZS-LOGIN-005锛氳ˉ榻愪护鐗屾暟鎹簱涓庣紦瀛樼殑涓€鑷存€ф仮澶
?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:429:
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:430:- 鍏宠仈锛欶ND-SYS-002锛沇P-05/14/20锛汢03/B05锛堟寜瀛愰
」鐙珛鏀捐锛夈€備紭鍏堢骇 P0锛涚被鍒?鏀归€狅紱鐘舵€?寰呭紑鍙戯紱鍓嶇疆 鎸夌 16.1 鑺傚悇瀛愰」锛岀姝㈠皢鍚庣疆瀛?
椤逛綔涓烘棭鏈熸壒娆″墠缃€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:431:- 浼楀瑕佹眰涓庣幇鐘讹細[OAuth2TokenServiceImpl](../
services/zhongshu-core/zszj-module-system/src/main
/java/cn/zszj/module/system/service/oauth2/OAuth2TokenServiceImpl.java) 鍦ㄦ暟鎹簱浜
嬪姟鍐呭啓/鍒?Redis锛岀紦瀛樻湭鍛戒腑鍙洖婧愶紱缂撳瓨鎴愬姛鑰屼簨鍔″洖婊氥€佹挙閿€澶辫触鎴栧洖婧愬苟鍙戠殑缁?
鏋滃皻鏈獙鏀躲€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:432:- 璋冩暣锛氬畾涔変簨鍔℃彁浜ゃ€佺紦瀛樺け鏁堛€佹潈濞佹挙閿€鐘舵€佸拰澶辫触琛ュ
伩椤哄簭锛汢03 鍏堝畬鎴愭潈濞佹牎楠屼笌澶辨晥瀹夊叏闂幆锛孊05 鍐嶇敱 ZS-JOB-002 鎵╁睍鍙潬浜嬩欢琛ュ伩锛屼笉鍙嶅悜绛夊緟瀹屾暣 B05
锛涗笉鎶?Redis 鎿嶄綔瑙嗕负琚?PG 鑷姩鍥炴粴銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:433:- 楠屾敹锛氭敞鍏?PG 鎻愪氦澶辫触銆丷edis 涓嶅彲鐢ㄣ€佺紦瀛樺洖濉笌鎾ら
攢绔炴€侊紝涓嶈兘鐣欎笅鍙敤骞界伒鍑嵁鎴栨仮澶嶅凡鎾ら攢浼氳瘽锛涘け璐ユ湁鍛婅鍜屽彲閲嶆斁淇璇佹嵁銆?
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:434:- 鍒嗘壒浜や粯锛氳绗?16.1 鑺?ZS-LOGIN-005.A銆乑S-LOGI
N-005.B锛涘瓙椤瑰垎鍒櫥璁拌瘉鎹紝鍏ㄩ儴閫傜敤瀛愰」楠屾敹鍚庢墠鑳藉皢鏈富浠诲姟鏍囦负宸查獙鏀躲€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:435:
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:436:### ZS-LOGIN-006锛氬缓绔嬩笉鏆撮湶鍑嵁鐨勪細璇濈鐞嗕笌韪㈠嚭鍚堝
悓
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:483:### ZS-PERM-001锛氭牎楠屾巿鏉冨璞″綊灞炰笌鍙巿浜堟潈闄愪笂闄?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:484:
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:485:- 鍏宠仈锛欶ND-AUTH-001/002/004/008锛沇P-07/08锛汢0
3/B08锛堟寜瀛愰」鐙珛鏀捐锛夈€備紭鍏堢骇 P0锛涚被鍒?鏀归€狅紱鐘舵€?寰呭紑鍙戯紱鍓嶇疆 鎸夌 16.1 鑺?
鍚勫瓙椤癸紝绂佹灏嗗悗缃瓙椤逛綔涓烘棭鏈熸壒娆″墠缃€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:486:- 浼楀瑕佹眰涓庣幇鐘讹細[PermissionController](../se
rvices/zhongshu-core/zszj-module-system/src/main/j
ava/cn/zszj/module/system/controller/admin/permission/PermissionController.java
) 鍒嗛厤鑿滃崟浼氳繃婊ゅ椁愯彍鍗曪紝浣嗗垎閰嶇敤鎴疯鑹茬洿鎺ヨ繘鍏?[PermissionSer
viceImpl](../services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/mo
dule/system/service/permission/Permissio
nServiceImpl.java) 鐨勫叧鑱斿啓鍏ワ紱宸茶璺緞缂哄皯瀵规瘡涓鎺堜簣鐢ㄦ埛/瑙掕壊/閮ㄩ棬褰掑睘鍜屽彲鎺堜簣涓婇檺鐨勫畬鏁存樉寮忔牎楠
屻€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:487:- 璋冩暣锛氬湪鏈嶅姟灞傜粺涓€楠岃瘉鐩爣瀛樺湪銆佸悓鎶€鏈鎴?鑾峰噯璺ㄧ粍缁
囪寖鍥淬€佽鑹茬姸鎬佸拰鎿嶄綔鑰呭彲鎺堜簣闆嗗悎锛涚壒鏉冭鑹蹭笌鑷垜鎻愭潈鍗曞垪锛涗繚鐣欏悎鐞嗙鐞嗗憳鑳藉姏銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:488:- 楠屾敹锛氱鏀逛粬绉熸埛鐢ㄦ埛/瑙掕壊/閮ㄩ棬 ID銆佹壒閲忔贩鍏ュ拰鑷垜鎻愭
潈鍧囨嫆缁濓紱鍚堟硶鎺堟潈鎴愬姛锛涢噸澶嶆巿鏉冨箓绛夛紱涓嶈兘鍙嚟褰撳墠璇锋眰 tenant_id 涓哄叧绯昏〃濉€煎氨璁ゅ畾鎵€鏈夊閿璞″畨鍏ㄣ€
?
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:489:- 鍒嗘壒浜や粯锛氳绗?16.1 鑺?ZS-PERM-001.A銆乑S-PERM-
001.B锛涘瓙椤瑰垎鍒櫥璁拌瘉鎹紝鍏ㄩ儴閫傜敤瀛愰」楠屾敹鍚庢墠鑳藉皢鏈富浠诲姟鏍囦负宸查獙鏀躲€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:490:- 寮€鍙戣褰曪紙2026-09-09锛孼S-PERM-001.A 鎺堟潈鐩爣褰
掑睘涓庡彲鎺堜簣涓婇檺鏍￠獙锛夛細鍦?[PermissionServiceImpl](../services/zh
ongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/service/perm
ission/PermissionServiceImpl.java) 鏈嶅姟灞備负涓?
涓巿鏉冨啓鍏ユ柟娉曪紙assignUserRole/assignRoleMenu/assignRoleDataScope锛夎ˉ榻?*鍐欏叆鍓嶇粺涓€鏄惧紡鏍
￠獙**锛屽牭浣忋€岀洿鎺ュ啓鍏宠仈銆佷粠涓?fetch 鐩爣瀵硅薄銆嶇殑缂哄彛鈥斺€斿師瀹炵幇浠呭嚟璇锋眰 tenant_id
 涓哄叧绯昏〃锛圲serRoleDO/RoleMenuDO 缁?MyBatis 绉熸埛鎷︽埅鍣級鑷姩濉€硷紝绡℃敼鐨勪粬绉熸埛 user/role/de
pt ID 浼氳鐩插啓涓鸿法绉熸埛澶栭敭銆?*鏍￠獙缁村害**锛氣憼鐩爣瀛樺湪锛坲ser 缁?userService.
getUser銆乺ole 缁?roleService.getRole/getRoleList銆乨ept 缁?deptService.getDeptList锛宯
ull 鍗虫姏 USER_NOT_EXISTS/ROLE_NOT_EXISTS/D
EPT_NOT_FOUND锛夛紱鈶?*鍚屾妧鏈鎴峰綊灞?*锛堟樉寮忔瘮瀵规瘡涓閿璞?tenantId 涓?TenantContextHolder.
getTenantId()锛屼笉绗︽姏鏂板閿欒鐮?PERMISSION_ASSIGN_USER/ROL
E/DEPT_OTHER_TENANT锛岃返琛?line 488銆屼笉鑳藉彧鍑姹?tenant_id 濉叧绯昏〃灏辫瀹氬閿畨鍏ㄣ€嶏級锛涒憿瑙掕壊
鐘舵€侊紙鏂版巿浜堣鑹茬鐢ㄥ垯鎶涙棦鏈?ROLE_IS_DISABLE锛夛紱鈶?*鍙巿浜堜笂闄?*锛堥潪瓒呯鎿嶄綔鑰呮巿浜堣秴绠?
瑙掕壊鎶?PERMISSION_GRANT_EXCEED_CEILING锛夛紱鈶?*鑷垜鎻愭潈**锛堥潪瓒呯鎿嶄綔鑰呬负鑷韩 userId 鏂板瑙掕壊
鎶?PERMISSION_SELF_ELEVATION锛夛紝鈶ｂ懁鍗?line 487銆岀壒鏉冭鑹蹭笌鑷垜鎻愭潈
鍗曞垪銆嶃€傛柊澧為敊璇爜娈?1-002-009-000~004銆?*渚涚粰璺緞鍏煎锛堝叧閿級**锛歍enantServiceImpl.createT
enant/updateTenantRoleMenu 缁?TenantUtils.execute(n
ewTenantId) 杩愯锛堣鍏蜂綋绉熸埛涓?setIgnore(false)锛夛紝鏁呮柊寤?user/role 鐨?tenantId==鐜绉熸埛锛
屽綊灞炴牎楠屽ぉ鐒堕€氳繃锛涗緵缁欏垎閰?TENANT_ADMIN锛堥潪 super_admin锛変笖鐩爣鐢ㄦ埛鈮犲钩
鍙扮櫥褰曠敤鎴凤紝涓婇檺/鑷垜鎻愭潈涓嶈Е鍙戙€?*鎶ゆ爮**锛氬綊灞炴牎楠屼粎鍦ㄣ€屾湁鎶€鏈鎴蜂笂涓嬫枃涓旀湭蹇界暐绉熸埛銆嶆椂鎵ц锛坕sIgno
re() 鎴?getTenantId()==null 璺宠繃锛屽吋瀹圭郴缁熺骇鎿嶄綔锛夛紱涓婇檺/鑷垜鎻愭潈浠呭湪 getLoginUserId
()!=null 鏃舵墽琛岋紙鏃犵櫥褰曚笂涓嬫枃鐨勭郴缁?渚涚粰璋冪敤璺宠繃锛夛紱瓒呯淇濈暀瀹屾暣绠＄悊鑳藉姏锛坙ine 487銆屼繚鐣欏悎鐞嗙鐞嗗憳鑳藉
姏銆嶏級銆?*淇濈暀鏃㈡湁鑳藉姏**锛歛ssignRoleMenu 鐨勫椁愯彍鍗曡繃婊や粛鍦?Controller锛坔andleTen
antMenu锛夛紱MenuDO 涓哄叏灞€琛ㄦ棤 tenantId锛屾晠浠呮牎楠?roleId 褰掑睘銆?*澶瑰叿**锛圥ermissionServiceT
est锛孊aseDbUnitTest+H2+@MockitoBean 鍗忎綔鏈嶅姟锛夛細鏇存柊 3 涓棦
鏈?assign 鐢ㄤ緥 mock 鍚堟硶褰掑睘璺緞锛涙柊澧?9 鐢ㄤ緥鈥斺€旈敊绉熸埛 user/role/dept 鎷掔粷銆佹壒閲忔贩鍏ワ紙涓€鏈鎴
?涓€浠栫鎴疯鑹叉暣鎵规嫆缁濓級銆佺鐢ㄨ鑹叉嫆缁濄€佽嚜鎴戞彁鏉冩嫆缁濓紙MockedStatic<SecurityFramewo
rkUtils> 娉ㄥ叆 loginUserId锛夈€佽秴涓婇檺鎷掔粷銆侀噸澶嶆巿鏉冨箓绛夛紙createRoleIds 绌哄垯鏃犲啓鍏ユ棤鎶ラ敊锛夈€侀敊绉
熸埛瑙掕壊鎷掔粷锛坅ssignRoleMenu锛夈€?*楠岃瘉**锛歁aven `-pl :zszj-module-sy
stem "-Dtest=PermissionServiceTest" test` 鏈湴 BUILD SUCCESS锛孴ests run: 33, Fail
ures: 0, Errors: 0, Skipped: 0锛?026-09-09
T14:05:26+08:00锛?4 鏃㈡湁 + 9 鏂板锛夈€?*瀵归綈楠屾敹**锛坙ine 488锛夛細绡℃敼浠栫鎴风敤鎴?瑙掕壊/閮ㄩ棬 ID銆佹壒
閲忔贩鍏ャ€佽嚜鎴戞彁鏉冨潎鎷掔粷锛涘悎娉曟巿鏉冩垚鍔燂紱閲嶅鎺堟潈骞傜瓑锛涙樉寮?fetch 骞舵瘮瀵瑰閿璞?tenantId锛?
涓嶅彧鍑叧绯昏〃濉€笺€?*寰呴獙鏀惰鏄?*锛氭湰瀛愰」浠呭仛**鍚屾妧鏈鎴?*褰掑睘鏍￠獙锛涜幏鍑嗚法缁勭粐鑼冨洿锛圖-09锛夌殑鎺堟潈鐩爣/涓
婇檺鏄犲皠涓庢鍚戠煩闃靛綊 ZS-PERM-001.B銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:491:
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:492:### ZS-PERM-002锛氬缓绔嬫煡璇€佸崟瀵硅薄涓庢壒閲忓鍑虹殑鎺堟潈瑕
嗙洊鐭╅樀
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:493:
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:494:- 鍏宠仈锛欶ND-AUTH-003/004/008锛沇P-07/08锛汢03/B0
8锛堟寜瀛愰」鐙珛鏀捐锛夈€備紭鍏堢骇 P0锛涚被鍒?楠岃瘉閫傞厤锛涚姸鎬?寰呭紑鍙戯紱鍓嶇疆 鎸夌 16.1 鑺傚悇瀛?
椤癸紝绂佹灏嗗悗缃瓙椤逛綔涓烘棭鏈熸壒娆″墠缃€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:495:- 浼楀瑕佹眰涓庣幇鐘讹細[DeptDataPermissionRule](../
services/zhongshu-core/zszj-framework/zszj-spring-
boot-starter-biz-data-permission/src/main/java/cn/zszj/framework/datapermission
/core/rule/dept/DeptDataPermissionRule.j
ava) 鍙娉ㄥ唽琛?鍒楁瀯閫犳潯浠讹紱[PermissionServiceImpl](../services/zhongshu-core/zszj-mo
dule-system/src/main/java/cn/zszj/module/syst
em/service/permission/PermissionServiceImpl.java) 鐨勫瑙掕壊鑼冨洿閲囩敤鍚堝苟锛屼笉鑷姩璇佹槑璇︽儏銆佽
仈琛ㄣ€佹墜鍐?SQL 鍜屽鍑哄叏瑕嗙洊銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:496:- 璋冩暣锛氫负宸插惎鐢ㄨ〃鍜岃闂矾寰勯€愪竴鐧昏绉熸埛/閮ㄩ棬/鏈汉/瀵硅
薄鎺堟潈绛栫暐锛涙湭閫傞厤璺緞鎷掔粷鎴栭檺鍒讹紱鎶€鏈す鍏峰彲鍏堝仛锛屼紬澧?SELF/ASSIGNED 绛夋寜 D-09 鑾锋壒鍚庢槧灏勶紱鏍￠獙澶?
瑙掕壊鍚堝苟璇箟銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:497:- 楠屾敹锛氬悓绉熸埛鏃犲璞℃潈闄愩€佽法绉熸埛銆佸凡鐭?ID銆佹壒閲忔贩鍏ャ€佸
鍑哄拰缁熻鍧囨寜鐭╅樀闄愬埗锛涘悎娉曟眹鎬讳笉鎰忓鏆撮湶鏁忔劅鏄庣粏锛涙棦鏈?DB 鍥炲綊澶嶇敤鑰岄潪閲嶅缂栧啓銆?
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:498:- 鍒嗘壒浜や粯锛氳绗?16.1 鑺?ZS-PERM-002.A銆乑S-PERM-
002.B锛涘瓙椤瑰垎鍒櫥璁拌瘉鎹紝鍏ㄩ儴閫傜敤瀛愰」楠屾敹鍚庢墠鑳藉皢鏈富浠诲姟鏍囦负宸查獙鏀躲€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:499:
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:500:### ZS-PERM-003锛氬疄鐜扮粺涓€鍔ㄤ綔涓庡瓧娈垫巿鏉冭緭鍑?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:507:### ZS-PERM-004锛氶獙璇佹巿鏉冪紦瀛樸€佹挙閿€涓庡璁＄殑涓€鑷存€
?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:508:
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:509:- 鍏宠仈锛欶ND-AUTH-002/003/007锛沇P-07/13/20锛汢03
/B08锛堟寜瀛愰」鐙珛鏀捐锛夈€備紭鍏堢骇 P0锛涚被鍒?鏀归€狅紱鐘舵€?寰呭紑鍙戯紱鍓嶇疆 鎸夌 16.1 鑺傚悇
瀛愰」锛岀姝㈠皢鍚庣疆瀛愰」浣滀负鏃╂湡鎵规鍓嶇疆銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:510:- 浼楀瑕佹眰涓庣幇鐘讹細[PermissionServiceImpl](../s
ervices/zhongshu-core/zszj-module-system/src/main/
java/cn/zszj/module/system/service/permission/PermissionServiceImpl.java) 宸叉湁 C
acheEvict/Cacheable 涓庣鐢ㄨ鑹茶繃婊わ紱缂撳瓨椹遍€愩€佷簨鍔℃彁浜ゃ€佸叾浠栬妭
鐐硅鍙栧拰鎶€鏈鎴峰拷鐣ョ紦瀛樼殑缁勫悎灏氭湭鐪熷疄楠岃瘉銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:511:- 璋冩暣锛氬鐢ㄧ幇鏈夐┍閫愮瓥鐣ワ紝琛ュ鑺傜偣/浜嬪姟澶辫触/骞跺彂鍥炲～/濂
楅鍙樻洿/瑙掕壊鍋滅敤鐨勬祴璇曞拰蹇呰淇锛涙槑纭け鏉冪敓鏁堣竟鐣岋紝鎺堟潈鍓嶅悗宸紓绾冲叆瀹¤锛涗笉鎶婃竻缂撳瓨褰撲竾鑳戒慨澶嶃€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:512:- 楠屾敹锛氭挙鏉冨悗鏃?Token銆佹棫鑿滃崟鍜岀紦瀛樹笉鑳界户缁墽琛屽彈闄愬姩
浣滐紱缂撳瓨鏁呴殰涓嶆墿澶ф潈闄愶紱鍚堟硶鎭㈠鍙噸鏂板彇鏉冿紱瀹¤鍖呭惈鎿嶄綔鑰呫€佺洰鏍囥€佸彉鏇淬€佺粨鏋滀笌 trace銆?
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:513:- 鍒嗘壒浜や粯锛氳绗?16.1 鑺?ZS-PERM-004.A銆乑S-PERM-
004.B锛涘瓙椤瑰垎鍒櫥璁拌瘉鎹紝鍏ㄩ儴閫傜敤瀛愰」楠屾敹鍚庢墠鑳藉皢鏈富浠诲姟鏍囦负宸查獙鏀躲€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:514:
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:515:## 9. M07锛氬瓧鍏搞€佸弬鏁颁笌妯″潡閰嶇疆
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:521:### ZS-CFG-001锛氬垝鍒嗛厤缃彲瑙佹€с€佹晱鎰熶俊鎭笌鏈嶅姟绔啓
鏉冮檺
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:522:
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:523:- 鍏宠仈锛欶ND-SYS-001銆丗ND-AUTH-002/007锛沇P-18锛汢
01/B03锛堟寜瀛愰」鐙珛鏀捐锛夈€備紭鍏堢骇 P0锛涚被鍒?鏀归€狅紱鐘舵€?寮€鍙戜腑锛涘墠缃?鎸夌 16.1 
鑺傚悇瀛愰」锛岀姝㈠皢鍚庣疆瀛愰」浣滀负鏃╂湡鎵规鍓嶇疆銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:524:- 浼楀瑕佹眰涓庣幇鐘讹細[ConfigController](../servic
es/zhongshu-core/zszj-module-infra/src/main/java/c
n/zszj/module/infra/controller/admin/config/ConfigController.java) 鐨?get-value-
by-key 鍙?visible 鎺у埗锛屼絾绠＄悊璇︽儏/瀵煎嚭鏄彟涓€鏉¤矾寰勶紱visib
le 涓嶆槸绉樺瘑瀛樺偍鎴栨潈闄愬垎绾ф柟妗堛€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:525:- 璋冩暣锛氬皢閮ㄧ讲绉樺瘑淇濈暀浜庣幆澧?鎵瑰噯绉樺瘑瀛樺偍锛涙暟鎹簱鍙傛暟鏍囨
敞鐢ㄩ€斻€佺被鍨嬨€佹晱鎰熺骇銆佸彲璇讳富浣撳拰鏄惁鐑敓鏁堬紱鎺у埗灏嗘晱鎰熼」鏀规垚 visible 鐨勬潈闄愶紝璇︽儏/瀵煎嚭/鏃ュ織缁熶竴鑴辨晱銆
?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:526:- 楠屾敹锛氭櫘閫氱敤鎴峰彧璇昏幏鍑嗛厤缃紱鏁忔劅椤逛笉鑳介€氳繃鏍囧彲瑙併€佸
鍑烘垨鏃ュ織娉勯湶锛涘悎娉曠鐞嗗姩浣滃彲瀹¤锛涗笉灏嗙幇鏈夋寜閿帴鍙ｈ绉板尶鍚嶄换鎰忚鍙栥€?
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:527:- 鍒嗘壒浜や粯锛氳绗?16.1 鑺?ZS-CFG-001.A銆乑S-CFG-00
1.B锛涘瓙椤瑰垎鍒櫥璁拌瘉鎹紝鍏ㄩ儴閫傜敤瀛愰」楠屾敹鍚庢墠鑳藉皢鏈富浠诲姟鏍囦负宸查獙鏀躲€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:528:- 寮€鍙戣褰曪紙2026-09-09锛孼S-CFG-001.A锛夛細鏂板绉樺瘑
鎵弿闂ㄧ `node scripts/cfg/verify-config-secrets.mjs`锛堢瀵?
绫婚敭浠呭厑璁哥┖鍊?鍗犱綅绗︺€佹嫤鎴?RSA/PEM 鍧椾笌瓒呴暱 Base64/闀垮崄鍏繘鍒朵覆銆侀儴缃叉ā鏉垮繀濉」蹇呴』涓烘棤榛樿鍊煎崰浣嶇
鍗崇己閰嶅惎鍔ㄦ槑纭け璐ワ級锛沎閰嶇疆鍚堝悓](../services/zhongshu-core/script/config/RE
ADME.md) 澧炶ˉ绉樺瘑/鏁忔劅/鏅€氫笁妗ｅ垎绫昏〃銆傞棬绂侀杞嵆鎷︽埅骞舵竻闄わ細娉ㄩ噴涓殑 RSA 妗堜緥绉侀挜/鍏挜瀵广€佸揩閫?00
 婕旂ず瀹㈡埛鍙枫€乄eb 绔笂娓哥櫨搴︾粺璁?ID 涓庢敞閲?RSA 瀵嗛挜瀵广€傚緟楠屾敹璇存槑锛?B锛圔03锛屽弬鏁拌鍐欐潈闄?鍙鎬?杈撳嚭
鑴辨晱锛夋寜鍓嶇疆鍙﹁瀹炴柦銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:529:
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:530:### ZS-CFG-002锛氶獙璇佸瓧鍏哥紪鐮併€佺姸鎬佸拰寮曠敤鐨勭ǔ瀹氭€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:531:
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:532:- 鍏宠仈锛欶ND-SYS-001銆丗ND-DB-005/012锛沇P-17/18锛
汢02/B03锛堟寜瀛愰」鐙珛鏀捐锛夈€備紭鍏堢骇 P0锛涚被鍒?鏀归€狅紱鐘舵€?寮€鍙戜腑锛涘墠缃?鎸夌 16.1
 鑺傚悇瀛愰」锛岀姝㈠皢鍚庣疆瀛愰」浣滀负鏃╂湡鎵规鍓嶇疆銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:533:- 浼楀瑕佹眰涓庣幇鐘讹細[DictTypeServiceImpl](../ser
vices/zhongshu-core/zszj-module-system/src/main/ja
va/cn/zszj/module/system/service/dict/DictTypeServiceImpl.java) 宸叉湁鏌ラ噸銆佸瓙椤瑰垹闄や繚
鎶ゅ拰 deletedTime 閫昏緫锛涗笉鑳戒互 Java 娉ㄩ噴璁や负 PG 鍞竴绱㈠紩宸插瓨鍦ㄣ€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:534:- 璋冩暣锛氭槑纭瓧鍏哥紪鐮佷笉鍙殢鏍囩璋冩暣銆佸仠鐢ㄤ笌鍘嗗彶鏄剧ず瑙勫垯锛
涙寜鐪熷疄浣滅敤鍩熻ˉ鏁版嵁搴撶害鏉熴€佸紩鐢ㄤ繚鎶ゅ拰骞跺彂娴嬭瘯锛涚被鍨嬮噸鍛藉悕蹇呴』鏈夎縼绉伙紝鍖哄垎婕旂ず椤逛笌蹇呰绉嶅瓙銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:535:- 楠屾敹锛氬苟鍙戜笉浜х敓绂佹閲嶅缂栫爜锛涘仠鐢ㄥ悗鏂板啓鍏ユ嫆缁濅絾鍘嗗彶璁
板綍鍙В閲婏紱鏈夊紩鐢ㄧ殑鍒犻櫎/鏀圭爜鍙楁帶锛涗袱绔瓧鍏稿埛鏂颁竴鑷达紝鍗囩骇涓嶈鐩栫敤鎴疯嚜瀹氫箟鏁版嵁銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:536:- 寮€鍙戣褰曪紙2026-09-09锛孼S-CFG-002.A锛夛細鏍稿 V1
 鍩虹嚎鍙戠幇瀛楀吀琛ㄤ粎鏈変富閿害鏉熴€佺己缂栫爜鍞竴绾︽潫锛堟湰浠诲姟鏍稿績缂哄彛锛夛紱浜や粯 [V20260909.003__
system_dict_unique_constraints.sql](../services/zhongshu-core/zszj-server/src/m
ain/resources/db/migration/V20260909.003
__system_dict_unique_constraints.sql)鈥斺€旈儴鍒嗗敮涓€绱㈠紩锛圵HERE deleted=0锛夌害鏉?system_d
ict_type.type 涓?system_dict_data(dict_type,val
ue)锛屽凡鍒犻櫎琛屼笉闃诲鍚岀紪鐮侀噸寤猴紱浜や粯楠岃瘉鐢ㄤ緥闆?`node scripts/db/run-cfg002-verify.mjs` 8 鐢ㄤ
緥鍏ㄨ繃锛氬敮涓€绱㈠紩寤虹珛銆侀噸澶嶇紪鐮侀鎻掓垚鍔?鍐嶆彃鎷掔粷銆佸苟鍙戞彃鍏ュ悓缂栫爜浠呬竴涓垚鍔熴€佸瓧鍏搁」 (type,val
ue) 閲嶅琚嫆銆乂3 閲嶈窇骞傜瓑銆侀€昏緫鍒犻櫎鍚庡悓缂栫爜鍙噸寤轰笖鍦ㄥ唽浠嶅敮涓€銆佷綆鏉冮檺璐﹀彿鍙瀛楀吀锛沄1 鍏ㄩ噺绉嶅瓙锛?86 绫
诲瀷/915 椤癸級閫氳繃鍞竴绾︽潫鈥斺€旇瘉鏄庡湪鍐岀瀛愭棤閲嶅缂栫爜銆傚緟楠屾敹璇存槑锛?B锛圔03锛夊仠鐢ㄨ涔夈€佸紩鐢ㄤ繚鎶や笌涓ょ鍒锋柊鑱
旈獙锛涘苟鍙戠敤
渚嬩负鍙屼細璇濆疄娴嬶紝Java 灞傚苟鍙戝綊 ZS-DB-019.B銆?
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:537:- 鍒嗘壒浜や粯锛氳绗?16.1 鑺?ZS-CFG-002.A銆乑S-CFG-00
2.B锛涘瓙椤瑰垎鍒櫥璁拌瘉鎹紝鍏ㄩ儴閫傜敤瀛愰」楠屾敹鍚庢墠鑳藉皢鏈富浠诲姟鏍囦负宸查獙鏀躲€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:538:
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:539:### ZS-CFG-003锛氬浐鍖栨ā鍧椼€佸椁愪笌鑿滃崟鐨勫垎灞傜敓鏁堣鍒?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:540:
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:541:- 鍏宠仈锛欶ND-ARCH-002銆丗ND-INF-005銆丗ND-SYS-001
锛沇P-18锛汢01/B03锛堟寜瀛愰」鐙珛鏀捐锛夈€備紭鍏堢骇 P0锛涚被鍒?鏀归€狅紱鐘舵€?寮€鍙戜腑锛涘墠缃?
鎸夌 16.1 鑺傚悇瀛愰」锛岀姝㈠皢鍚庣疆瀛愰」浣滀负鏃╂湡鎵规鍓嶇疆銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:542:- 浼楀瑕佹眰涓庣幇鐘讹細[TenantServiceImpl](../servi
ces/zhongshu-core/zszj-module-system/src/main/java
/cn/zszj/module/system/service/tenant/TenantServiceImpl.java) 鎸夊椁愯彍鍗曟洿鏂拌鑹叉巿鏉冿
紝[TenantPackageServiceImpl](../services/zhongs
hu-core/zszj-module-system/src/main/java/cn/zszj/module/system/service/tenant/T
enantPackageServiceImpl.java) 绠＄悊濂楅锛涘畠浠笉鑳藉惎
鐢ㄦ湭缂栬瘧鐨勪笟鍔℃ā鍧楋紝涔熶笉鑳界嫭绔嬩唬鏇挎帴鍙ｉ壌鏉冦€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:543:- 璋冩暣锛氬缓绔嬭繍琛屾ā鍧楃櫧鍚嶅崟銆佸彲鐢ㄥ姛鑳界洰褰曘€佺鎴峰姛鑳借鍙
拰瑙掕壊鏉冮檺鐨勪氦闆嗚鍒欙紱鍙樻洿濂楅鍚屾鑿滃崟/缂撳瓨/鍓嶇瀵艰埅涓旀湇鍔＄閲嶆锛涗笉鎶婃鎶€鏈兘鍔涙墿涓烘湭缁忕‘璁ゆ敹璐规ā鍨嬨€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:544:- 楠屾敹锛氬叧闂ā鍧楁棤鎺ュ彛/Job/鍙搷浣滆彍鍗曪紱濂楅鍥炴敹鍚庣洿璋冧
篃鎷掔粷锛涘椁愭墿澶т笉鑷姩鎺堜簣鎵€鏈夎鑹诧紱绯荤粺淇濈暀瑙掕壊涓庣敤鎴疯嚜瀹氫箟鎺堟潈鎸夋壒鍑嗚鍒欏鐞嗐€?
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:545:- 鍒嗘壒浜や粯锛氳绗?16.1 鑺?ZS-CFG-003.A銆乑S-CFG-00
3.B锛涘瓙椤瑰垎鍒櫥璁拌瘉鎹紝鍏ㄩ儴閫傜敤瀛愰」楠屾敹鍚庢墠鑳藉皢鏈富浠诲姟鏍囦负宸查獙鏀躲€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:546:- 寮€鍙戣褰曪紙2026-09-09锛孼S-CFG-003.A锛夛細妯″潡鐩綍
涓嬫矇 [ModuleCatalog](../services/zhongshu-core/zszj-fr
amework/zszj-common/src/main/java/cn/zszj/framework/common/catalog/ModuleCatalo
g.java)锛坺szj-common锛屽敮涓€浜嬪疄鏉ユ簮锛泂erver 鐨?Modu
leWhitelist 鏀逛负濮旀墭锛夛紝鏂板 `moduleOfMenu/isMenuAllowed` 褰掑睘鍒ゅ畾涓庡崟娴?ModuleCatalogT
est锛沎TenantPackageServiceImpl](../services/zhon
gshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/service/tenant
/TenantPackageServiceImpl.java) 鐨?create
/update 澧炲姞 `validateTenantPackageMenus`鈥斺€斿椁愯彍鍗曞紩鐢ㄦ湭鍚敤妯″潡鍗虫嫆缁濓紙鏂伴敊璇爜 1-002-
016-004锛夛紱浜や粯 [鍔熻兘鐩綍](../services/zhongshu-core/docs/鍔熻兘
鐩綍.md)锛堝惎鐢?鏈惎鐢ㄦ竻鍗曘€佷簲灞傛嫤鎴摼銆佹ā鍧楀惎鍋滃彉鏇磋绋嬶級銆傞獙璇侊細ModuleCatalogTest 3 鐢ㄤ緥銆乿erif
y-module-whitelist锛堥€傞厤鏂扮洰褰曠被鍚?0 闂锛夈€傚緟楠屾敹璇存槑锛?B锛圔03锛夊椁愬彉鏇村悗鐨勮彍鍗?缂?
瀛樺悓姝ヤ笌鏈嶅姟绔噸妫€鑱旈獙锛?濂楅鍥炴敹鍚庣洿璋冩嫆缁?渚濊禆 ZS-SEC 閾撅紝褰?B03銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:547:
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:561:### ZS-FILE-001锛氬缓绔嬫枃浠跺綊灞炰笌缁熶竴瀵硅薄鎺堟潈鍏ュ彛
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:562:
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:563:- 鍏宠仈锛欶ND-INF-001銆丗ND-AUTH-004/008锛沇P-12锛汢
04/B08锛堟寜瀛愰」鐙珛鏀捐锛夈€備紭鍏堢骇 P0锛涚被鍒?鏀归€狅紱鐘舵€?寰呭紑鍙戯紱鍓嶇疆 鎸夌 16.1 
鑺傚悇瀛愰」锛岀姝㈠皢鍚庣疆瀛愰」浣滀负鏃╂湡鎵规鍓嶇疆銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:564:- 浼楀瑕佹眰涓庣幇鐘讹細[FileDO](../services/zhongsh
u-core/zszj-module-infra/src/main/java/cn/zszj/mod
ule/infra/dal/dataobject/file/FileDO.java) 缁ф壙 BaseDO锛屾湭鏄惧紡鍖呭惈 tenant銆佷笟鍔″璞°€佹
墍鏈夎€呮垨鏁忔劅绾э紱[涓嬭浇 Controller](../services/zhongshu-cor
e/zszj-module-infra/src/main/java/cn/zszj/module/infra/controller/admin/file/Fi
leController.java) 鐨?get/** 鏍囨敞 PermitAll
 鍜?TenantIgnore銆備笂浼犻渶瑕佺櫥褰曪紝涓嶈兘璇啓涓烘墍鏈夋枃浠舵帴鍙ｅ潎鍖垮悕銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:565:- 璋冩暣锛氬尯鍒嗗叕寮€绱犳潗鍜岀鏈夐檮浠讹紱璁板綍鏈嶅姟绔‘璁ょ殑涓讳綋銆佹
妧鏈鎴枫€佺敤閫斻€佸璞″紩鐢ㄥ拰鐘舵€侊紱缁熶竴璇诲彇/鍒犻櫎/鎵归噺/瀵煎嚭鎺堟潈锛屽叧闂鏈夐檮浠剁殑鍘熻矾寰勫尶鍚嶆梺璺紱瀛樺偍妗?CDN 
绛栫暐涓庢帴鍙?
涓€骞舵鏌ャ€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:566:- 楠屾敹锛氭巿鏉冭€呬笂浼犲拰涓嬭浇鎴愬姛锛涙棤鍑嵁銆佷粬浜?鍙︿竴鎶€鏈鎴
枫€佺寽 ID/璺緞銆佹壒閲忔贩鍏ヨ秺鏉冩枃浠跺潎涓嶈兘璇诲彇鎴栧垹闄わ紱鍏紑绱犳潗浠嶆寜鎵瑰噯鐢ㄩ€斿彲鐢ㄣ€傚巻鍙叉枃浠跺垎绫昏縼绉讳笉鑳介粯璁ゅ叏閮
ㄥ叕寮€銆?
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:567:- 鍒嗘壒浜や粯锛氳绗?16.1 鑺?ZS-FILE-001.A銆乑S-FILE-
001.B锛涘瓙椤瑰垎鍒櫥璁拌瘉鎹紝鍏ㄩ儴閫傜敤瀛愰」楠屾敹鍚庢墠鑳藉皢鏈富浠诲姟鏍囦负宸查獙鏀躲€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:568:
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:569:### ZS-FILE-002锛氳ˉ榻愪笂浼犵敤閫斻€佸唴瀹规牎楠屽拰鍞竴瀵硅薄閿
?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:584:### ZS-FILE-004锛氭帴鍏ョ煭鏃剁エ鎹拰瀵煎嚭鍏ㄧ▼鏉冮檺閲嶆
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:585:
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:586:- 鍏宠仈锛欶ND-INF-001銆丗ND-AUTH-004/006/007锛沇P-
12锛汢04/B08锛堟寜瀛愰」鐙珛鏀捐锛夈€備紭鍏堢骇 P0锛涚被鍒?琛ュ缓锛涚姸鎬?寰呭紑鍙戯紱鍓嶇疆 鎸夌 1
6.1 鑺傚悇瀛愰」锛岀姝㈠皢鍚庣疆瀛愰」浣滀负鏃╂湡鎵规鍓嶇疆銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:587:- 浼楀瑕佹眰涓庣幇鐘讹細渚涗綋 [JdbcDeliveryPort](../re
ference/donors/zhongshu-design/yudao-module-infra/s
rc/main/java/cn/iocoder/yudao/module/infra/zhongshu/delivery/JdbcDeliveryPort.j
ava) 宸叉湁鏁ｅ垪瀛樺偍銆佸埌鏈熸潯浠跺拰鍘熷瓙涓€娆℃秷璐癸紱浣嗘秷璐?SQL 娌℃湁鎵€鏈夎€?绉熸埛
/鐢ㄩ€斿尮閰嶈皳璇嶏紝涓嶈兘鍘熸牱褰撲綔瀹屾暣鎺堟潈銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:588:- 璋冩暣锛氱鍙?鍘熷瓙鍏戞崲鍧囬獙璇佺櫥褰曚富浣撱€佷細璇濄€乼enant銆佺敤
閫斻€佸璞″拰褰撳墠鏉冮檺锛涢粯璁ら噰鐢ㄥ悗绔壌鏉冨彇娴侊紝涓嶅悜绉佹湁闄勪欢璋冪敤鑰呰繑鍥炲彲缁曡繃骞冲彴鎺堟潈鐨勫瓨鍌?URL锛屽寘鎷煭鏃?GET 
棰?
绛惧悕 URL銆備竴娆℃€хエ鎹彧鍏戞崲缁戝畾涓讳綋/鐧诲綍浼氳瘽銆佽祫浜х増鏈€佺敤閫斿拰鏈夋晥鏈熺殑涓嬭浇浼氳瘽锛涗細璇?ID 涓嶈兘鍗曠嫭浠ｆ浛璁よ
瘉銆傛瘡娆?HEAD/GET/Range/缁紶璇锋眰鍧囬噸妫€韬唤銆佷細璇濄€佸璞″拰鎾ゆ潈鐘舵€侊紝鎸夊悓涓€涓嶅彲鍙樼増鏈彇娴併€傚鍑哄湪鐢熸垚
涓庝氦浠樻椂閲嶆绛?
閫?瀛楁/瀵硅薄鏉冮檺锛涘叕寮€绱犳潗鍗曠嫭璧板凡鎵瑰噯鍏紑閫氶亾銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:589:- 楠屾敹锛氭湰浜哄彲鍘熷瓙鍏戞崲涓€娆★紝骞跺湪鍚屼竴鏈夋晥鐧诲綍/涓嬭浇浼氳瘽鍐
呭畬鎴愰瑙堛€丷ange 鍜屾柇绾跨画浼狅紱閲嶅鍏戞崲涓嶅緱鏂板缓浼氳瘽锛屾柇绾夸笉鑳借姹傞噸澶嶆秷璐瑰師绁ㄦ嵁銆傝浆鍙戠エ鎹€佷笅杞戒細璇濇垨鍦板潃
缁欎粬浜恒€佽法
鎶€鏈鎴疯闂€佸厬鎹㈠悗閫€鍑?鎾ゆ潈/杩囨湡锛屼互鍙婂鍑虹敓鎴愭湡闂存挙鏉冿紝閮戒笉鑳界户缁彂璧锋垚鍔熺殑鍙栨祦璇锋眰銆傚宸插湪閫斾紶杈撴槑纭
垎鍧?鏃堕檺閲嶆骞跺仠姝㈠悗缁緭鍑猴紝鎾ゆ潈鐢熸晥涓婇檺鍦ㄥ惎鐢ㄥ墠閰嶇疆骞跺疄娴嬶紱宸插彂閫?宸蹭笅杞藉瓧鑺傛棤娉曟挙鍥炪€傛鏌ラ噸瀹氬悜銆丆DN銆
佺紦瀛樸€佹棩蹇楀拰鍓嶇涓存椂鏂?
浠讹紝涓嶈兘鏆撮湶瀛樺偍鍑嵁鎴栫紦瀛樹粬浜虹鏈夊搷搴旓紱璁板綍鍏戞崲涓庢瘡娆′氦浠樼粨鏋溿€?
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:590:- 鍒嗘壒浜や粯锛氳绗?16.1 鑺?ZS-FILE-004.A銆乑S-FILE-
004.B锛涘瓙椤瑰垎鍒櫥璁拌瘉鎹紝鍏ㄩ儴閫傜敤瀛愰」楠屾敹鍚庢墠鑳藉皢鏈富浠诲姟鏍囦负宸查獙鏀躲€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:591:
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:592:### ZS-FILE-005锛氬缓绔嬫枃浠跺垹闄ゃ€佸鍎垮璞″拰澶辫触鎭㈠鐘
舵€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:593:
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:594:- 鍏宠仈锛欶ND-INF-001/004锛沇P-12/19锛汢04/B05锛堟寜瀛
愰」鐙珛鏀捐锛夈€備紭鍏堢骇 P1锛涚被鍒?琛ュ缓锛涚姸鎬?寰呭紑鍙戯紱鍓嶇疆 鎸夌 16.1 鑺傚悇瀛愰」锛岀姝㈠皢鍚庣疆
瀛愰」浣滀负鏃╂湡鎵规鍓嶇疆銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:595:- 浼楀瑕佹眰涓庣幇鐘讹細[FileServiceImpl](../service
s/zhongshu-core/zszj-module-infra/src/main/java/cn
/zszj/module/infra/service/file/FileServiceImpl.java) 鍏堜笂浼犲璞″啀鎻掑厓鏁版嵁锛涘垹闄ゅ厛鍒犲瓨鍌
ㄥ啀鍒犺褰曪紝鎵归噺閫愰」鎵ц銆傛暟鎹簱浜嬪姟涓嶈兘鑷姩鍥炴粴瀵硅薄瀛樺偍鍓綔鐢ㄣ€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:596:- 璋冩暣锛氬鍔犲彲鎭㈠鐘舵€?琛ュ伩闃熷垪涓庡璐︼紱淇濈暀寮曠敤鏂囦欢鐨勫垹
闄よ鍒欙紝杩囨湡涓婁紶鍜屽鍑烘寜鐢ㄩ€斾繚鐣欐湡娓呯悊锛涢€愰」璁板綍鎵归噺缁撴灉锛岄伩鍏嶅崐瀹屾垚浼姤鍏ㄦ垚鍔燂紱娓呯悊鍓嶆牳楠屽紩鐢ㄥ拰鎺堟潈銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:597:- 楠屾敹锛氭敞鍏ュ瓨鍌ㄨ秴鏃躲€丏B 鍐欏け璐ャ€佹壒閲忎腑娈靛け璐ュ悗鍙煡鍙
噸璇曪紱閲嶈瘯涓嶈鍒犱粛寮曠敤鏂囦欢锛涙竻鐞嗚寖鍥村彲棰勮涓斾笉璺ㄦ妧鏈鎴凤紱鎭㈠鍚庤褰曚笌瀵硅薄涓€鑷淬€?
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:598:- 鍒嗘壒浜や粯锛氳绗?16.1 鑺?ZS-FILE-005.A銆乑S-FILE-
005.B锛涘瓙椤瑰垎鍒櫥璁拌瘉鎹紝鍏ㄩ儴閫傜敤瀛愰」楠屾敹鍚庢墠鑳藉皢鏈富浠诲姟鏍囦负宸查獙鏀躲€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:599:
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:600:### ZS-AUDIT-001锛氶€傞厤缁熶竴涓氬姟瀹¤浜嬩欢涓庝簨鍔¤竟鐣?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:634:### ZS-MSG-003锛氭敹浠剁涓庢秷鎭惤鐐逛簩娆℃巿鏉?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:635:
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:636:- 鍏宠仈锛欶ND-INF-002銆丗ND-AUTH-004/008锛沇P-10/1
5锛汢05/B06/B08锛堟寜瀛愰」鐙珛鏀捐锛夈€備紭鍏堢骇 P0锛涚被鍒?鏀归€狅紱鐘舵€?寰呭紑鍙戯紱鍓嶇疆 鎸?
绗?16.1 鑺傚悇瀛愰」锛岀姝㈠皢鍚庣疆瀛愰」浣滀负鏃╂湡鎵规鍓嶇疆銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:637:- 浼楀瑕佹眰涓庣幇鐘讹細[NotifyMessageController](..
/services/zhongshu-core/zszj-module-system/src/mai
n/java/cn/zszj/module/system/controller/admin/notify/NotifyMessageController.ja
va) 鐨勬垜鐨勬秷鎭娇鐢ㄧ櫥褰曚富浣擄紱[NotifyMessageMapper](..
/services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/
dal/mysql/notify/NotifyMessageMapper.jav
a) 鐨勬爣璁板凡璇?SQL 宸茬害鏉熺敤鎴峰拰 userType锛屽簲淇濈暀锛屼笉鍒や负浠绘剰 ID 鍙敼銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:638:- 璋冩暣锛氳鐩栨秷鎭垪琛?姝ｆ枃/绠＄悊鏌ヨ鐨勮寖鍥达紝鎸夌娉ㄥ唽娑堟伅钀
界偣锛涜烦杞悗閲嶆柊璇诲彇涓氬姟骞舵巿鏉冿紝涓嶆妸涓氬姟鏁忔劅姝ｆ枃闀挎湡澶嶅埗杩涢€氱煡锛涢檺鍒舵湭璇诲垪琛?size锛涚粍缁?鏉冮檺鍙樺寲鍚庡埛鏂扮紦瀛
樸€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:639:- 楠屾敹锛氫粬浜?IDs 涓嶈兘鏀瑰彉宸茶鐘舵€侊紝鍙︿竴鎶€鏈鎴蜂笉鑳借
鍙栨秷鎭紱鏃ф秷鎭湪涓氬姟鎾ゆ潈鍚庝笉鑳借繘鍏ヨ鎯?涓嬭浇闄勪欢锛涙湭鐭ユ垨鍏抽棴妯″潡钀界偣鏄剧ず鏄庣‘涓嶅彲鐢紝Web/绉诲姩鍒嗗埆鍙敤銆?
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:640:- 鍒嗘壒浜や粯锛氳绗?16.1 鑺?ZS-MSG-003.A銆乑S-MSG-00
3.B銆乑S-MSG-003.C锛涘瓙椤瑰垎鍒櫥璁拌瘉鎹紝鍏ㄩ儴閫傜敤瀛愰」楠屾敹鍚庢墠鑳藉皢鏈富浠诲姟鏍囦负宸查獙鏀躲€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:641:
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:642:### ZS-MSG-004锛氭笭閬撳彂閫佺姸鎬併€佸け璐ラ噸璇曚笌鍥炴墽瀵硅处
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:724:### ZS-CLIENT-001锛氭敹鏁?Web 璺敱涓庢巿鏉冪姸鎬佸悓姝?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:725:
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:726:- 鍏宠仈锛欶ND-WEB-001/002銆丗ND-AUTH-005锛沇P-10锛汢
06/B08锛堟寜瀛愰」鐙珛鏀捐锛夈€備紭鍏堢骇 P0锛涚被鍒?鏀归€狅紱鐘舵€?寰呭紑鍙戯紱鍓嶇疆 鎸夌 16.1 
鑺傚悇瀛愰」锛岀姝㈠皢鍚庣疆瀛愰」浣滀负鏃╂湡鎵规鍓嶇疆銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:727:- 浼楀瑕佹眰涓庣幇鐘讹細[Web permission.ts](../apps/
zhongshu-admin-web/src/permission.ts) 鐧诲綍鍚庤閰嶅姩鎬佽矾鐢憋紱[P
ermission store](../apps/zhongshu-admin-web/src/store/modules/permission.ts) 浠庣
紦瀛樿鑹茶彍鍗曠敓鎴愯矾鐢便€傚彲澶嶇敤锛屼絾闇€瑕佷紬澧呮ā鍧楃櫧鍚嶅崟涓庢挙鏉冨悗鍒锋柊鍚堝悓銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:728:- 璋冩暣锛氱粺涓€妯″潡/椤甸潰娉ㄥ唽鍜屽悗绔彍鍗曟槧灏勶紝鏈惎鐢ㄦā鍧椾笉鍙
繘鍏ワ紱閫€鍑恒€佹挙鏉冦€佹妧鏈鎴峰彉鍖栨竻鐞嗘棫璺敱銆佺紦瀛樸€侀〉绛惧拰鏁版嵁锛涙秷璐?allowedActions/瀛楁鎺堟潈锛屼笉鍦ㄥ墠绔
噸绠?
涓氬姟鏉冮檺锛涙牎楠岀櫥褰曢噸瀹氬悜鐩殑鍦般€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:729:- 楠屾敹锛氬埛鏂板拰鐩磋揪鎺堟潈椤甸潰鍙敤锛涘叧闂ā鍧椼€佸け鏁堢紦瀛樸€佷吉
閫犲墠绔鑹蹭笉鑳芥搷浣滐紱鎾ゆ潈鍚庢棫椤电/杩斿洖缂撳瓨涓嶅睍绀烘棫鏁忔劅鏁版嵁锛屾帴鍙ｄ粛鐙珛鎷掔粷锛涚櫥褰曞け璐ヤ笉鐣欎笅鍗婂垵濮嬪寲璺敱銆?
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:730:- 鍒嗘壒浜や粯锛氳绗?16.1 鑺?ZS-CLIENT-001.A銆乑S-CLI
ENT-001.B锛涘瓙椤瑰垎鍒櫥璁拌瘉鎹紝鍏ㄩ儴閫傜敤瀛愰」楠屾敹鍚庢墠鑳藉皢鏈富浠诲姟鏍囦负宸查獙鏀躲€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:731:
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:732:### ZS-CLIENT-002锛氬缓绔嬬Щ鍔ㄥ鑸敞鍐岃〃涓庣洿杈鹃〉瀹堝崼
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:733:
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:734:- 鍏宠仈锛欶ND-MINI-001/002锛沇P-10锛汢06/B08锛堟寜瀛愰」
鐙珛鏀捐锛夈€備紭鍏堢骇 P0锛涚被鍒?鏀归€狅紱鐘舵€?寰呭紑鍙戯紱鍓嶇疆 鎸夌 16.1 鑺傚悇瀛愰」锛岀姝㈠皢鍚庣疆瀛愰」
浣滀负鏃╂湡鎵规鍓嶇疆銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:735:- 浼楀瑕佹眰涓庣幇鐘讹細[绉诲姩璺敱鎷︽埅鍣╙(../apps/zhongsh
u-miniapp/src/router/interceptor.ts) 鐧诲綍鍚庡師鍒欎笂鍏佽瀛樺湪椤甸潰锛沎Ta
bBar config](../apps/zhongshu-miniapp/src/tabbar/config.ts) 鍥哄畾宸ヤ綔鍙?瀹℃壒/閫氳褰?娑堟
伅/鎴戠殑锛屽皻闈炴湇鍔＄鎺堟潈瀵艰埅锛涗笉绛変簬鍚庣鎺堟潈缂哄け銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:736:- 璋冩暣锛氭湇鍔＄涓嬪彂鑾峰噯鍏ュ彛鏍囪瘑銆佺绫诲瀷鍜屽姩浣滐紝瀹㈡埛绔彧鏄
犲皠宸叉敞鍐岀Щ鍔ㄩ〉闈紱鏍￠獙 TabBar銆佸垎鍖呫€佸喎鍚姩銆佷簩缁寸爜/鍒嗕韩/娑堟伅鐩磋揪椤碉紝鍏抽棴妯″潡鎻愪緵涓嶅彲鐢ㄨ惤鐐癸紱涓嶆妸 We
b c
omponent 璺緞褰撶Щ鍔ㄨ矾鐢憋紱D-09 涔嬪墠涓嶅疄鐜板韬唤鍒囨崲銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:737:- 楠屾敹锛氫笉鍚屾妧鏈潈闄愮湅鍒板搴斿叆鍙ｏ紱鐧诲綍浣嗘棤椤甸潰鎺堟潈銆佺洿杈
惧叧闂?BPM 鎴栨湭鐭ヨ惤鐐瑰潎琚嫤鎴紱鍐峰惎鍔?鎭㈠椤甸潰琛屼负涓€鑷达紱濡?D-09 纭韬唤鍒囨崲锛屽啀楠岃瘉缂撳瓨鍙婂鑸暣浣撳埛鏂般€
?
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:738:- 鍒嗘壒浜や粯锛氳绗?16.1 鑺?ZS-CLIENT-002.A銆乑S-CLI
ENT-002.B锛涘瓙椤瑰垎鍒櫥璁拌瘉鎹紝鍏ㄩ儴閫傜敤瀛愰」楠屾敹鍚庢墠鑳藉皢鏈富浠诲姟鏍囦负宸查獙鏀躲€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:739:
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:740:### ZS-CLIENT-003锛氫慨姝ｈ姹傚嚟鎹寖鍥翠笌寮傚父鏀舵暃
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:754:### ZS-CLIENT-005锛氳ˉ榻愬墠绔被鍨嬨€佹瀯寤哄拰澶氱鍩虹鍥炲
綊
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:755:
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:756:- 鍏宠仈锛欶ND-WEB-001銆丗ND-MINI-001銆丗ND-DOC-003
锛沇P-20/24锛汢01/B06/B11锛堟寜瀛愰」鐙珛鏀捐锛夈€備紭鍏堢骇 P0锛涚被鍒?楠岃瘉閫傞厤锛涚姸
鎬?寮€鍙戜腑锛涘墠缃?鎸夌 16.1 鑺傚悇瀛愰」锛岀姝㈠皢鍚庣疆瀛愰」浣滀负鏃╂湡鎵规鍓嶇疆銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:757:- 浼楀瑕佹眰涓庣幇鐘讹細[Web package.json](../apps/z
hongshu-admin-web/package.json) 鏈?ts:check銆佹瀯寤哄拰 lin
t锛孾绉诲姩 package.json](../apps/zhongshu-miniapp/package.json) 鏈?H5/灏忕▼搴?App 绛夎剼鏈
紱[杩佸叆楠岃瘉鎶ュ憡](04-婧愮爜杩佸叆涓庨獙璇佹姤鍛?md) 鏄庣‘绫诲瀷澶辫触涓庣Щ鍔ㄦ瀯寤烘湭楠岃瘉锛岃剼鏈?
瀛樺湪涓嶇瓑浜庡钩鍙伴€氳繃銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:758:- 璋冩暣锛氬浐瀹?Node/pnpm/閿佹枃浠跺畨瑁呭熀绾匡紝淇绫诲瀷妫€鏌ラ
棶棰樺苟鍒嗙被璁板綍鍩虹嚎锛涘畬鎴?Web銆丠5銆佸井淇″皬绋嬪簭鏋勫缓鍙婄櫥褰?瀵艰埅/娑堟伅/闄勪欢鍥炲綊锛涜澶囪兘鍔涢€氳繃閫傞厤灞傚畾涔夌己澶?
鎷掔粷鏉冮檺闄嶇骇锛孉pp/iOS 绛惧悕鍙戝竷鍙︽寜 B11 楠屾敹銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:759:- 楠屾敹锛氬共鍑€鐜鍙噸澶嶅畨瑁呭苟閫氳繃瀵瑰簲绫诲瀷/鏋勫缓闂ㄧ锛涗袱绔
富閾炬鍙嶅悜娴嬭瘯鍙鐜帮紱鏃犺澶囨潈闄愩€佸悗鍙版仮澶嶅拰鐗堟湰涓嶅吋瀹规槑纭檷绾э紱涓嶈兘鐢?H5 鎴栨ā鎷熷櫒閫氳繃浠ｆ浛寰俊/App 鐪熸
満楠屾敹銆?
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:760:- 鍒嗘壒浜や粯锛氳绗?16.1 鑺?ZS-CLIENT-005.A銆乑S-CLI
ENT-005.B銆乑S-CLIENT-005.C锛涘瓙椤瑰垎鍒櫥璁拌瘉鎹紝鍏ㄩ儴閫傜敤瀛愰」楠屾敹鍚庢墠鑳藉皢鏈富
浠诲姟鏍囦负宸查獙鏀躲€?
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:761:- 寮€鍙戣褰曪紙2026-09-09锛孼S-CLIENT-005.A锛夛細鏋勫缓
涓庣被鍨嬪熀绾垮湪鏈満锛圵indows锛宯ode v24.19.0 + pnpm 10.x锛?026-09-
09 骞插噣瀹夎锛夋牳楠岋細Web `pnpm install` 鍙噸澶嶃€乣build:local` 閫氳繃銆乣build:h5`锛堢Щ鍔ㄧ锛夐€氳繃
銆佸井淇＄ `build:mp` 閫氳繃涓斾骇鐗╂棤鏃у搧鐗屽瓧绗︿覆锛涚被鍨嬫鏌ュ浐鍖栦负鍩虹嚎闂ㄧ `node scri
pts/client/verify-ts-baseline.mjs`鈥斺€斿綋鍓?11 鏉′笂娓稿熀绾块敊璇叏閮ㄤ綅浜庢湭鍚敤妯″潡锛坅i/bpm/crm
/fms/iot/mes锛夛紝鍩虹嚎澶栨柊澧為敊璇嵆澶辫触锛屽熀绾挎潯鐩慨澶嶅悗缂╁噺銆備袱绔敼鍔ㄦ枃浠?ESLint 閫?
杩囥€傚搧鐗屼笓椤规湡闂存湭寮曞叆浠讳綍鏂扮被鍨?鏋勫缓閿欒锛圸S-BRAND-003.A 璁板綍锛夈€傚緟楠屾敹璇存槑锛氬井淇″紑鍙戣€呭伐鍏?鐪熸満涓?
App/iOS 鏋勫缓绛惧悕褰?ZS-CLIENT-005.C锛堝緟鍓嶇疆锛変笌 B11锛涚櫥褰?瀵艰埅/娑堟伅/闄勪欢鍥炲綊褰?.B锛圔0
6锛夈€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:762:
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:763:### ZS-OPS-001锛氬缓绔嬪垎灞傝嚜鍔ㄥ寲涓庢枃妗ｈ瘉鎹棬绂?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:764:
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:765:- 鍏宠仈锛欶ND-DOC-001/002/003銆丗ND-BPM-004锛沇P-2
0锛汢01/B02/B03/B06/B09锛堟寜瀛愰」鐙珛鏀捐锛夈€備紭鍏堢骇 P0锛涚被鍒?琛ュ缓锛涚姸鎬?
寮€鍙戜腑锛涘墠缃?鎸夌 16.1 鑺傚悇瀛愰」锛岀姝㈠皢鍚庣疆瀛愰」浣滀负鏃╂湡鎵规鍓嶇疆銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:766:- 浼楀瑕佹眰涓庣幇鐘讹細[澶嶅埗蹇収娴嬭瘯](../scripts/verif
y-source-copy.test.mjs) 浠呮牎楠岃縼鍏ュ畬鏁存€э紱[杩佸叆楠岃瘉鎶ュ憡](04-婧愮爜杩佸叆涓庨獙璇佹姤
鍛?md) 鐨勫悗绔祴璇曟槸 H2/jedismock 涓庡巻鍙叉瀯寤虹粨鏋滐紝涓嶆槸鎸佺画 PG/鏉冮檺/澶氱鍙戝竷闂ㄧ銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:767:- 璋冩暣锛氬缓绔嬪浐瀹氭彁浜ょ殑鏋勫缓銆佸崟鍏冦€丳G/Redis銆佸弻鎶€鏈
鎴?缁勭粐銆佸瓨鍌ㄣ€佹祦绋嬨€佸绔強鏂囨。妫€鏌ユ祦姘寸嚎锛涘悇灞傚厛鏈夋湰鍦板叆鍙ｅ啀鎺?CI锛屽け璐ラ樆姝㈠搴旀壒娆℃斁琛岋紱淇濈暀璺宠繃鍘熷洜銆
佹祴璇曟暟
鎹殧绂诲拰鏉ユ簮宸紓杩借釜锛屾枃妗ｈ鍒欏疄鐜板綊 ZS-GOV-001銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:768:- 楠屾敹锛氭鍚戞牱渚嬮€氳繃锛涘紩鍏ヨ秺鏉冦€佸け鏁堣縼绉汇€侀噸澶嶆秷璐规垨鍧
忛摼鎺ユ椂瀵瑰簲妫€鏌ュけ璐ワ紱鍚屾彁浜ゆ姤鍛婃湁瀹為檯鐜/鍛戒护/缁撴灉锛涙湭鎵ц澶栭儴楠岃瘉淇濇寔鏈墽琛岋紝涓嶉潬璺宠繃鏁忔劅娴嬭瘯鍙樼豢銆?
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:769:- 鍒嗘壒浜や粯锛氳绗?16.1 鑺?ZS-OPS-001.A銆乑S-OPS-00
1.B銆乑S-OPS-001.C銆乑S-OPS-001.D銆乑S-OPS-001.E锛涘瓙椤瑰垎鍒櫥璁?
璇佹嵁锛屽叏閮ㄩ€傜敤瀛愰」楠屾敹鍚庢墠鑳藉皢鏈富浠诲姟鏍囦负宸查獙鏀躲€?
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:770:- 寮€鍙戣褰曪紙2026-09-09锛孼S-OPS-001.A锛夛細浜や粯鑱氬悎
闂ㄧ鍏ュ彛 `node scripts/ops/run-local-gates.mjs`锛?-fast 璺?
杩囨參閫熼」锛夛細鑱氬悎鏉ユ簮澶嶅埗鏍￠獙鍗曟祴銆佸搧鐗屽懡鍚嶉棬绂併€佹枃妗ｄ竴鑷存€с€佹ā鍧楃櫧鍚嶅崟銆佹暟鎹簮 PG 鍚堝悓銆丗lyway 瑙勮寖銆
侀厤缃瀵嗐€乄eb 绫诲瀷鍩虹嚎鍏?10 椤癸紝浠讳竴澶辫触闃绘鏀捐锛涙柊澧?[.github/workflows/local-baseli
ne-gates.yml](../.github/workflows/local-baseline-gates.yml) 浠ュ悓涓€鍏ュ彛锛?-fast锛夎繍
琛岋紝瀹炵幇"鏈湴涓?CI 鍚岃鍒?銆傝瘎瀹′慨姝ｏ細鏉ユ簮澶嶅埗鏍￠獙鍣ㄧ殑鍙墽琛屼綅鐢ㄤ緥鍦?Window
s 鏃?chmod 璇箟瀵艰嚧璇け璐ワ紝鏀逛负 POSIX 鏉′欢鎵ц锛圕I ubuntu 浠嶈鐩栵級锛汻EADME 闂ㄧ绔犺妭鍐欏叆鎹熷潖宸查噸寤
恒€傞獙璇侊細鑱氬悎闂ㄧ鍏ㄩ噺 10/10 閫氳繃銆傚緟楠屾敹璇存槑锛?B~.E锛圥G/瀹夊叏/澶氱/娴佺▼灞傛祦姘寸嚎锛夋寜鍚勬壒鍓嶇疆鎺?
鍏ワ紝鐪熷疄 CI 鍦ㄥ浐瀹氭彁浜ら€氳繃鍓嶄笉鐧昏 CI_VERIFIED銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:771:
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:772:### ZS-OPS-002锛氳ˉ榻愰儴缃层€佺洃娴嬩笌澶囦唤鎭㈠婕旂粌
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:773:
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:774:- 鍏宠仈锛欶ND-INF-003/004銆丗ND-DB-004/005銆丗ND-A
RCH-002锛沇P-19锛汢01/B05/B11锛堟寜瀛愰」鐙珛鏀捐锛夈€備紭鍏堢骇 P0锛涚被鍒?琛ュ缓
锛涚姸鎬?寰呭紑鍙戯紱鍓嶇疆 鎸夌 16.1 鑺傚悇瀛愰」锛岀姝㈠皢鍚庣疆瀛愰」浣滀负鏃╂湡鎵规鍓嶇疆銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:775:- 浼楀瑕佹眰涓庣幇鐘讹細[Server Dockerfile](../servi
ces/zhongshu-core/zszj-server/Dockerfile) 鍜?[鐜閰嶇疆]
(../services/zhongshu-core/zszj-server/src/main/resources/application-local.yam
l) 鏄幇鏈夎繍琛屽叆鍙ｏ紝涓嶈兘璇佹槑宸叉湁浼楀閮ㄧ讲銆佺綉缁滈殧绂汇€佸憡璀﹀拰鎭㈠婕旂粌銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:776:- 璋冩暣锛氬熀浜?JDK17/PG 鍥哄寲鑴辨晱閮ㄧ讲妯℃澘銆侀厤缃敞鍏ャ€乀L
S/鍙嶅悜浠ｇ悊鍜屾帰閽堣竟鐣岋紱鐩戞祴鏁版嵁搴撱€丷edis銆佸璞″瓨鍌ㄣ€侀槦鍒椾笌澶辫触浠诲姟锛涘 DB銆佹枃浠惰祫浜у拰蹇呰閰嶇疆杩涜涓€鑷?
鎬у浠?鎭㈠锛屾牳瀵规潈闄愩€佸璁′笌寰呮姇閫掍簨浠讹紱鐩爣瀹归噺/RPO/RTO 缁忛渶姹傚拰婕旂粌纭銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:777:- 楠屾敹锛氱嫭绔嬫祴璇曠幆澧冨彲鎸夎鏄庨儴缃插拰鎾ゅ洖锛涗緷璧栦笉鍙敤鏈夊憡璀
?鏄庣‘澶辫触锛涙仮澶嶅埌鏂伴殧绂荤幆澧冨悗鏁版嵁/闄勪欢/浜嬩欢涓€鑷翠笖涓嶅悜鐪熷疄澶栭儴娓犻亾琛ュ彂锛涜緭鍑哄疄娴嬫仮澶嶈€楁椂鍜屼涪澶辩獥鍙ｏ紝涓嶈櫄
鎶ョ敓浜у彲鐢ㄣ€?
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:778:- 鍒嗘壒浜や粯锛氳绗?16.1 鑺?ZS-OPS-002.A銆乑S-OPS-00
2.B銆乑S-OPS-002.C锛涘瓙椤瑰垎鍒櫥璁拌瘉鎹紝鍏ㄩ儴閫傜敤瀛愰」楠屾敹鍚庢墠鑳藉皢鏈富浠诲姟鏍囦负宸查獙鏀躲€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:779:
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:780:### ZS-OPS-003锛氫繚鐣欏悗缃兘鍔涢棬绂佷笌鎺ュ叆楠屾敹娓呭崟
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:789:### ZS-SYS-001锛氳ˉ榻愬熀纭€绠＄悊澶嶇敤鍔熻兘鐨勭湡瀹?PG 涓?W
eb 鍥炲綊
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:790:
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:791:- 鍏宠仈锛欶ND-SYS-001銆丗ND-AUTH-002/004銆丗ND-DB-
001銆丗ND-WEB-001銆丗ND-DOC-003锛沇P-18/20锛汢03/B06锛堟寜瀛?
椤圭嫭绔嬫斁琛岋級銆備紭鍏堢骇 P0锛涚被鍒?楠岃瘉閫傞厤锛涚姸鎬?寰呭紑鍙戯紱鍓嶇疆 鎸夌 16.1 鑺?ZS-SYS-001.A/.B锛屼笉鍙嶅悜绛夊緟
鍏ㄩ儴鍓嶇鏀归€犮€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:792:- 浼楀瑕佹眰涓庣幇鐘讹細涓婁綅闇€姹傝姹傜敤鎴枫€佽鑹层€佽彍鍗曘€佸矖浣嶃
€佸瓧鍏搞€侀厤缃拰鍏憡鍙敤锛沎System 娴嬭瘯閰嶇疆][E19] 鍜?[鍘嗗彶杩佸叆鎶ュ憡](04-婧愮爜杩佸叆涓庨獙璇佹姤鍛?md) 涓嶈兘
浠ｆ浛褰撳墠鐪熷疄 PG 涓?Web 鎿嶄綔楠屾敹銆傚悇鍔熻兘浠ｇ爜鍜岄〉闈㈣绗?15.1 鑺傦紝涓嶅洜浠ｇ爜鍙鐢ㄥ氨鐪佺暐楠屾敹銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:793:- 璋冩暣锛氬鐢?ZS-DB-019.A 鍜?ZS-SEC-012.A 澶瑰叿锛岃
ˉ榻愪竷绫诲姛鑳界殑绠＄悊鍔ㄤ綔銆佷繚瀛樿鍥炪€佺姸鎬併€佸紩鐢ㄣ€佹柟娉曟潈闄愬強閫傜敤鐨勭鎴疯寖鍥村洖褰掞紱鏂板澶辫触鎸夋棦鏈変换鍔′慨澶嶏紝
纭睘鏂扮己鍙ｅ啀鍒嗛厤缂栧彿锛屼笉閲嶅啓鏁村 System銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:840:- 寮€鍙戣褰曪紙2026-09-08锛夛細鍐荤粨鏄犲皠鍙戝竷浜?[鍝佺墝绱犳潗涓
庡懡鍚嶆槧灏刔(06-鍝佺墝绱犳潗涓庡懡鍚嶆槧灏?md)锛屾彁浜?887bbc4f銆傚疄娴嬪師鍥句负 1060脳228 RGBA 
閫忔槑 PNG锛孲HA-256 涓庢湰娓呭崟涓€鑷达紱鍐荤粨鍖呮牴 cn.zszj銆丟roupId cn.zszj銆侀厤缃墠缂€ zszj.*銆佺幆澧冨彉閲
?ZSZJ_*锛涚櫥璁板繀瑕佷繚鐣欙紙渚涗綋/璇佹嵁/缃插悕锛夈€佺鐢ㄨ祫浜э紙zszj-ui 鍐呴儴锛変笌闄愭湡鍏煎锛坹d-* 
鍓嶇紑锛変緥澶栵紱Git 杩滅▼銆亃hongshu-* 椤跺眰鐩綍銆丄ppID/鍩熷悕绛夊閮ㄦ爣璇嗗垪鍏ユ帓闄ゆ竻鍗曘€傚緟楠屾敹璇存槑锛氬伐鍟嗗叏绉颁笌
鐭㈤噺鍘熺寰呯敤鎴锋彁渚涳紱鍝佺墝绱犳潗鏈€缁堥噰绾崇敱鐢ㄦ埛纭銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:841:
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:842:### ZS-BRAND-002锛氱粺涓€鍚庣妯″潡銆佸寘绫汇€侀厤缃笌鏋勫缓浜
х墿鍚嶇О
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:843:
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:844:- 鍏宠仈锛欶ND-BRAND-002/003銆丗ND-ARCH-001/002銆丗
ND-SRC-004锛沇P-02/18/19锛汢01銆備紭鍏堢骇 P0锛涚被鍒?鏀归€狅紱鐘舵€?寰呴獙鏀讹紱
鍓嶇疆 ZS-BRAND-001銆乑S-ENG-001銆乑S-ENG-002銆乑S-ENG-003銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:845:- 浼楀瑕佹眰涓庣幇鐘讹細[鏍?POM][E01]銆乕Server POM][E0
2]銆乕涓婚厤缃甝[E05] 鍜?[Dockerfile][E03] 浣跨敤 yudao 妯″潡/鏈嶅姟/鍖?
鍓嶇紑锛涘悕绉颁笉浠呭瓨鍦ㄤ簬 Java import锛岃繕褰卞搷鍒跺搧銆侀厤缃粦瀹氬強鎵弿瑁呴厤銆?
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:846:- 璋冩暣锛氭寜鏄犲皠缁熶竴浜у搧妯″潡鐩綍銆丮aven parent/Group
Id/artifactId/BOM銆丣ava package/import 涓?Yudao* 绫诲悕銆佸惎鍔ㄧ被
/JAR銆丼pring 搴旂敤鍚嶅拰浜у搧閰嶇疆鍓嶇紑锛屽苟鏍稿鎺ュ彛鏂囨。鏍囬銆佸惎鍔?Banner 鐨勪骇鍝佸睍绀哄悕銆傝仈鏀圭粍浠?Mapper 鎵
弿銆丆ontroller 鍖呰矾寰勪笌鎺ュ彛鍓嶇紑鎺ㄥ銆乆ML namespace/resultType銆丮ETA-IN
F 鑷姩閰嶇疆/SPI銆佸弽灏勫瓧绗︿覆銆佹祴璇曘€佽剼鏈?闀滃儚寮曠敤锛涙櫘閫氶鍩熷悕涓庣涓夋柟绫诲簱涓嶄贡鏀广€傞厤缃柊鏃ч敭涓嶅緱浜х敓涓や釜涓
嶅悓鏈夋晥鍊硷紝鍐茬獊搴旀槑纭け璐ワ紱鍘嗗彶杩愯鏍囪瘑浜ょ敱 ZS-BRAND-004 鍒嗘壒杩佺Щ銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:847:- 楠屾敹锛氬共鍑€鏋勫缓銆佷緷璧栨爲銆佹墦鍖呭惎鍔ㄧ被涓庤嚜鍔ㄩ厤缃敞鍐屾纭
紱闈欐€?瑁呴厤娴嬭瘯鑳藉彂鐜版棫鍖呮紡鏀瑰拰鏂版棫绫婚噸澶嶆敞鍐岋紱鏈惎鐢ㄦā鍧椾粛鍏抽棴锛屼繚鐣欐簮鐮佸彲鎸夋壒鍑嗚寖鍥村崟鐙紪璇戯紱鐪熷疄 PG 鍚
姩鍙婂畬鏁村畨
鍏ㄩ摼鍦?B02/B03 澶嶉獙锛屼笉浠?B01 鏋勫缓浠ｆ浛杩愯鎴愬姛銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:848:- 寮€鍙戣褰曪紙2026-09-08锛夛細鎻愪氦 43d71fad锛?0,059
 鏂囦欢銆傜洰褰?鍖?绫诲悕浠?git mv 淇濈暀鍘嗗彶锛?2 涓《灞傛ā鍧椼€乫ramework 15 涓瓙妯″潡
銆?7 涓?Java 婧愮爜鏍广€?9 涓?Yudao*鈫抁szj* 绫伙級锛涢厤缃敭銆丼pring 搴旂敤鍚嶃€佺幆澧冨彉閲忥紙ZS_*鈫抁SZJ_*锛夈
€丏ockerfile/閮ㄧ讲鑴氭湰銆佹帴鍙ｆ枃妗ｆ爣棰樺悓姝ャ€傞潤鎬佷竴鑷存€ф鏌?`node scripts/bra
nd/verify-backend-naming.mjs` 閫氳繃锛?,763 涓?Java 鍖呭０鏄庝笌璺緞涓€鑷达紝imports/spring.fac
tories/Mapper XML 鐨?cn.zszj FQCN 鍏ㄩ儴鍙В鏋愶紝POM 鍧愭爣涓€
鑷达紝纭€ч棶棰?0銆傝瘎瀹′慨姝ｉ殢 581927d0/b2c26ea9 钀藉湴锛氭仮澶嶈璇敼鐨勪笂娓哥讲鍚嶉摼鎺ワ紙github.com/Yunai
V/*銆乬itee.com/zhijiantianya/*锛夊苟琛ヤ繚鎶よ鍒欍€傛湭楠岃瘉椤癸紙濡傚疄鐧昏锛夛細骞插噣鏋勫缓銆佷緷
璧栨爲涓庣湡瀹?PG 鍚姩寰?ZS-ENG-002 宸ュ叿閾撅紙鏈満鏃?JDK/Maven锛夊湪 B01/B02 琛ヨ瘉銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:849:
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:850:### ZS-BRAND-003锛氭浛鎹?Web 涓庣Щ鍔ㄧ鍝佺墝灞曠ず鍜屼骇鍝佸
寘鍚?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:851:
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:852:- 鍏宠仈锛欶ND-BRAND-001/002銆丗ND-WEB-001銆丗ND-MI
NI-001銆丗ND-SRC-004锛沇P-09/10/18/24锛汢01/B06锛堟寜瀛愰」鐙珛
鏀捐锛夈€備紭鍏堢骇 P0锛涚被鍒?鏀归€狅紱鐘舵€?寮€鍙戜腑锛涘墠缃?鎸夌 16.1 鑺傚悇瀛愰」銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:853:- 浼楀瑕佹眰涓庣幇鐘讹細[Web 鐜妯℃澘](../apps/zhongsh
u-admin-web/.env) 榛樿鏍囬涓鸿妺閬撶鐞嗙郴缁燂紝[Logo 缁勪欢](../apps/zhon
gshu-admin-web/src/layout/components/Logo/src/Logo.vue)銆乕Web Logo](../apps/zhon
gshu-admin-web/src/assets/imgs/logo.png)
銆乕favicon](../apps/zhongshu-admin-web/public/favicon.ico)銆乕绉诲姩 Logo](../apps/zh
ongshu-miniapp/src/static/logo.png) 鍜?[绉诲姩
 manifest](../apps/zhongshu-miniapp/manifest.config.ts) 灏氭湭鎸夌敤鎴峰搧鐗岄獙鏀躲€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:854:- 璋冩暣锛氱粺涓€鐧诲綍椤点€佹祻瑙堝櫒鏍囬銆佸睍寮€/鎶樺彔瀵艰埅銆佸姞杞?绌
烘€併€佸钩鍙板唴鍏充簬椤点€侀€傜敤姘村嵃鍜屽璇█灞曠ず锛涙浛鎹㈢浉搴?Logo/favicon 涓庣Щ鍔ㄧ搴旂敤灞曠ず璧勬簮锛屼繚鐣欏彲璁块棶鏂囨湰
涓斾笉鎷?
浼?瑁佹帀鍥句腑鏂囧瓧銆傛洿鏂颁骇鍝?package name/鎻忚堪銆侀厤缃叆鍙ｄ笌鏋勫缓寮曠敤锛屾棫婕旂ず閾炬帴鍜屼簩缁寸爜鎸変骇鍝佽寖鍥存竻鐞嗭紱娉ㄥ
唽鏍囪瘑/绛惧悕/涓婃灦涓嶈窡闅忓睍绀哄悕鑷姩淇敼銆傚巻鍙蹭綔鑰?璁稿彲璇佷笌鏉ユ簮鍏冩暟鎹户缁繚鐣欙紱鍝佺墝缁熶竴鍏ュ彛渚涘悗缁笟鍔￠〉闈㈠鐢ㄣ
€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:855:- 楠屾敹锛氬熀纭€涓ょ lint/typecheck/build 閫氳繃锛汢06
 鍦ㄧ湡瀹?API 鐜楠岀櫥褰曘€佸鑸€佸埛鏂般€侀€€鍑哄強鍝佺墝鍏ュ彛锛屾闈?绐勫睆銆佹祬娣辫儗鏅拰鎶樺彔鎬佹棤鏃у搧鐗屽洖閫€銆侀敊
鍥俱€佸彉褰€佹柇閾炬垨涓嶅彲璇绘枃瀛楋紱鐪熸満/涓婃灦浠嶇敱鏃㈡湁 B11 浠诲姟鐙珛鏀捐銆?
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:856:- 鍒嗘壒浜や粯锛歓S-BRAND-003.A 浜や粯 B01 闈欐€佸搧鐗屼笌鍖呭
悕锛孼S-BRAND-003.B 浜や粯 B06 澶氱鍙敤鎬ц仈楠岋紱涓嶈 B01 绛夊緟椤甸潰涓氬姟闂幆銆?
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:857:- 寮€鍙戣褰曪紙2026-09-08锛夛細ZS-BRAND-003.A 鎻愪氦 
ea580b1f锛?34 鏂囦欢銆備袱绔寘鍚嶇粺涓€ zszj-admin-web/zszj-miniapp锛?
鏍囬銆侀粯璁ょ櫥褰曠鎴枫€丩ogo/favicon銆佺Щ鍔ㄧ鍏ㄥ App 鍥炬爣鐢卞師鍥惧窘鏍囨淳鐢燂紙`scripts/brand/derive-b
rand-assets.mjs`锛岀函閫忔槑搴﹀埄鐢?瑁佸垏/缂╂斁锛屼笉閲嶇粯锛夛紱Home 涓婃父椤圭洰鍗＄墖涓庣櫥褰曢〉涓婃父鎺?
骞块摼鎺ユ寜浜у搧鑼冨洿娓呯悊锛涚粍浠剁洰褰?yudao-ui鈫抸szj-ui銆傞獙璇侊細Web `pnpm build:local` 閫氳繃锛泇ue-tsc
 鍏ㄩ噺浠呭墿 11 涓凡鐧昏鐨勪笂娓稿熀绾跨被鍨嬮敊璇笖鍧囦笉鍦ㄦ敼鍔ㄦ枃浠讹紙鏈 0 鏂板锛夛紱灏忕▼搴?`build
:h5` 閫氳繃銆傝瘎瀹′慨姝ｏ細涓婃父 YunaiV 閾炬帴璇敼鎭㈠銆乵all 婕旂ず鍩熷崐鏀规竻鐞嗐€傚緟楠屾敹璇存槑锛?B 褰?B06 鐪熸満/澶氱
鑱旈獙锛涘井淇″紑鍙戣€呭伐鍏?鐪熸満鏋勫缓褰?ZS-CLIENT-005銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:858:
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:859:### ZS-BRAND-004锛氳縼绉诲瓨閲忛厤缃€佺紦瀛樹細璇濅笌鎸佷箙鍖栧懡
鍚嶅紩鐢?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:860:
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:861:- 鍏宠仈锛欶ND-BRAND-002/003銆丗ND-DB-005/006銆丗ND
-SYS-002銆丗ND-INF-003锛沇P-04/05/14/19锛汢02/B03/B05
锛堟寜瀛愰」鐙珛鏀捐锛夈€備紭鍏堢骇 P0锛涚被鍒?鏀归€狅紱鐘舵€?寮€鍙戜腑锛涘墠缃?鎸夌 16.1 鑺傚悇瀛愰」銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:862:- 浼楀瑕佹眰涓庣幇鐘讹細[PG 鍒濆鑴氭湰][E10]銆乕涓婚厤缃甝[E05]
銆乕OAuth Token 鏈嶅姟](../services/zhongshu-core/zszj-modu
le-system/src/main/java/cn/zszj/module/system/service/oauth2/OAuth2TokenService
Impl.java) 涓?[Quartz 鑷姩閰嶇疆][E08] 娑夊強閰嶇疆銆佽处鍙蜂細
璇濇垨浠诲姟娑堣垂鑰咃紱鏈灏氭湭璇佹槑鎵€鏈夎繖浜涜矾寰勫潎鏈夋寔涔呭寲鏃у悕锛岄渶鍏堟壂鎻忕湡瀹炲畾涔変笌涓存椂鏍锋湰锛屼笉鎺ㄦ柇鐢熶骇瀛橀噺銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:863:- 璋冩暣锛氱洏鐐硅彍鍗?瀛楀吀/鍙傛暟绉嶅瓙銆丏B/schema/琛ㄥ簭鍒椾腑瀹為
檯鏃т骇鍝佹爣璇嗐€佹枃浠惰矾寰?璧勬簮鍓嶇紑銆丷edis/娴忚鍣ㄥ瓨鍌?key銆丱Auth 瀹㈡埛绔爣璇嗐€佷换鍔?Bean/鎸佷箙
鍖栫被鍚嶅拰娑堟伅璺敱/搴忓垪鍖栧紩鐢ㄣ€傚彧瀵圭‘闇€鍙樻洿椤瑰埗瀹氭柊瑁呬笌鍗囩骇璺嚎锛涘凡鎵ц杩佺Щ鑴氭湰涓嶆敼鍐欙紝涓氬姟鍐呭/鍘嗗彶瀹¤涓
嶅仛鏂囨湰鏇挎崲銆傞厤缃埆鍚嶆湁鍏煎绐楀彛銆佸敮涓€鏈夋晥鍊间笌閫€鍑烘潯浠讹紱浼氳瘽閫夋嫨瀹夊叏杩佺Щ鎴栨槑纭噸鏂扮櫥褰曪紝鏃у嚟鎹笉鑳藉娲汇€佸
弻 key 涓嶈兘缁曡繃绉熸埛/鎾ゆ潈
銆備换鍔?娑堟伅杩佺Щ闇€鍋滆皟搴︽垨鍙楁帶鍒囨崲鍜岄噸鏀剧瓥鐣ワ紝閬垮厤鏂版棫娑堣垂鑰呴噸澶嶆墽琛岋紱鏈粡鎵瑰噯涓嶆敼鐪熷疄澶栭儴瀹㈡埛绔敞鍐屾垨鍥炶皟
銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:864:- 楠屾敹锛氱湡瀹炰复鏃?PG 鐨勭┖搴?甯︽棫鍛藉悕鏍锋湰鍗囩骇銆侀噸澶嶈縼绉诲拰
澶辫触鎭㈠閫氳繃锛涘繀瑕佽祫浜т粛鍙畾浣嶏紱Redis/娴忚鍣ㄩ殧绂汇€佽繃鏈?鎾ゆ潈鍜屽苟鍙戝埛鏂扮鍚堟棦鏈夊悎鍚岋紱浠诲姟鎭㈠涓庨噸鏀炬棤闈欓粯
涓㈠け
銆佷笉閲嶅浜х敓涓氬姟鍓綔鐢ㄣ€佹棤鏃х被鎵句笉鍒颁笖涓嶆斁寮€瓒婃潈銆傛棤瀛橀噺闇€鐢ㄦ鏌ョ粨鏋滆瘉鏄庯紝涓嶈兘榛樿鍙竻搴撱€佹竻缂撳瓨鎴栧垹鏂囦
欢閲嶅缓銆?
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:865:- 鍒嗘壒浜や粯锛歓S-BRAND-004.A 鍦?B02 楠岄厤缃?绉嶅瓙/鎸佷箙
鍖栧紩鐢紝.B 鍦?B03 楠岀紦瀛樹笌浼氳瘽锛?C 鍦?B05 楠屼换鍔?浜嬩欢锛涗笉寰楄姹傛棭鏈熸暣鍗″畬鎴愶紝涔熶笉鎺堟潈
鐢熶骇杩佺Щ銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:866:- 寮€鍙戣褰曪紙2026-09-08锛夛細鎻愪氦 581927d0銆傚熀绾挎牳鏌
ヨ瘉鏄庢棫鏍囪瘑闆嗕腑浜庣瀛愭紨绀烘暟鎹紙绉熸埛/閮ㄩ棬/鐢ㄦ埛/鍏憡/OAuth2 瀹㈡埛绔?yudao_demo* 琛ㄤ笌婕?
绀哄煙锛夛紝瀛楀吀/鑿滃崟/鍙傛暟/浠诲姟绉嶅瓙鏃犳棫浜у搧鏍囪瘑锛堣瘉鎹负鍩虹嚎绉嶅瓙鍏ㄦ枃鎵弿锛岀櫥璁颁簬杩佺Щ鑴氭湰澶达級銆傚叏鏂硅█鏂拌绉嶅瓙瀹
屾垚鏀瑰悕锛涙柊澧炲箓绛夊崌绾ц剼鏈?[20260908_brand_rename_zszj.sql](../services/zhongsh
u-core/sql/postgresql/upgrades/20260908_brand_rename_zszj.sql)锛堝崟浜嬪姟銆佸け璐ユ暣浣撳洖婊氾
級銆傜湡瀹?PG 楠岃瘉锛坉ocker postgres:17-alpine 涓存椂瀹瑰櫒锛岄獙鍚庢竻鐞?
锛夛細鏂拌搴撲笌"鏃х瀛?杩佺Щ"鍗囩骇搴撳悇 8 椤规棫鏍囪瘑鎵弿鍏?0銆佹紨绀烘暟鎹繚鐣欍€侀噸澶嶆墽琛屽箓绛夈€佹敞鍏ュけ璐ラ獙璇佷簨鍔″洖婊氥
€傝瘎瀹′慨姝ｏ細楠岃瘉涓彂鐜板苟琛ラ綈 zsxq/cloud/@iocoder 鍩熷悕绛夋紡椤广€俁edis/娴忚鍣ㄥ瓨鍌?key 缁忔壂鎻忎笉鍚骇鍝
?
鍓嶇紑锛?B 浠ヨ嚜鐒惰繃鏈?閲嶆柊鐧诲綍澶勭疆锛孊03 鑱旈獙锛夛紱浠诲姟鎸佷箙鍖栧紩鐢ㄥ綊 .C锛圔05锛夈€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:867:
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:868:### ZS-BRAND-005锛氱粺涓€浠ｇ爜鐢熸垚妯℃澘涓庡悗缁柊妯″潡鍛藉悕
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:869:
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:870:- 鍏宠仈锛欶ND-BRAND-002/003銆丗ND-DB-010銆丗ND-INF
-005锛沇P-03/17/18锛汢02銆備紭鍏堢骇 P0锛涚被鍒?鏀归€狅紱鐘舵€?寰呴獙鏀讹紱鍓嶇疆 ZS-
BRAND-001銆乑S-BRAND-002銆乑S-DB-017銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:871:- 浼楀瑕佹眰涓庣幇鐘讹細[浠ｇ爜鐢熸垚妯℃澘](../services/zhon
gshu-core/zszj-module-infra/src/main/resources/codege
n) 涓?[鍏冩暟鎹湇鍔[E27] 鏄悗缁笟鍔℃ā鍧楃殑閲嶈鍏ュ彛锛涘彧鏀圭幇鏈夊伐绋嬪悕瀛椾笉鑳戒繚璇佹ā鏉裤€侀粯璁ゅ寘鍚嶄笌鐢熸垚鏂囦欢涓嶅啀
甯﹀洖涓婃父鍓嶇紑銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:872:- 璋冩暣锛氭洿鏂颁竴鏈熷疄闄呬娇鐢ㄧ殑 Java/Vue3/UniApp 妯℃澘銆
侀粯璁ゅ寘鍚?妯″潡鍚嶃€乮mport銆佹潈闄愪笌璧勬簮寮曠敤鍜岀ず渚嬮厤缃紱鍏蜂綋涓氬姟鍚箟涓嶇敱鏀瑰悕浠诲姟瀹氫箟銆傞潪鍚敤妯℃澘鍒椾负绂佺敤
鎴栧緟閫傞厤锛屼笉涓洪獙璇佸懡鍚嶆搮鑷惎鐢ㄥ叾瀹冨墠绔?涓氬姟妯″潡锛涗緵浣撴湭鏉ユ憳鍙栧墠鎵ц鍚屼竴鍛藉悕妫€鏌ャ€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:874:- 寮€鍙戣褰曪紙2026-09-08锛夛細鎻愪氦 08ad8e78銆傚鏍稿叏閮
ㄦā鏉匡細鍖呭悕缁?`${basePackage}` 鍙橀噺娉ㄥ叆锛坄zszj.codegen.base-packa
ge=cn.zszj`锛夛紝鏃犵‖缂栫爜浜у搧鍖呭悕锛汮ava/Vue3 妯℃澘涓哄惎鐢紝UniApp 寰呴€傞厤锛寁ben/vue2 绯荤鐢紙[cod
egen/README.md](../services/zhongshu-core/zszj-modul
e-infra/src/main/resources/codegen/README.md) 鐧昏锛岀姝㈢敓鎴愬埌 zszj-ui 璧勪骇鐩綍锛夛紱娴嬭瘯
澶瑰叿涓庢柇瑷€ 112 涓枃浠跺悓鎵逛腑鎬у寲绀轰緥鍩燂紝淇濇寔 CodegenEngineTest 鍙ｅ緞涓€鑷淬€?
娈嬬暀鎵弿 0锛坄node scripts/brand/verify-brand-naming.mjs`锛夈€傚緟楠屾敹璇存槑锛氫粠鐙珛 PG 娴嬭瘯琛ㄧ
敓鎴愭牱渚嬪苟缂栬瘧楠岃瘉闇€ B02 宸ュ叿閾句笌杩愯鏈嶅姟锛屾湰鏈烘棤 JDK/Maven锛屽凡濡傚疄鐧昏涓哄緟琛ヨ瘉鎹€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:875:
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:876:### ZS-BRAND-006锛氬缓绔嬪搧鐗屽懡鍚嶆畫鐣欐壂鎻忎笌鍥炲綊鏀捐闂ㄧ

  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:877:
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:878:- 鍏宠仈锛欶ND-BRAND-001/002/003銆丗ND-SRC-004銆丗N
D-DOC-001/003锛沇P-01/20锛汢01/B06锛堟寜瀛愰」鐙珛鏀捐锛夈€備紭鍏堢骇 P0锛?
绫诲埆 琛ュ缓锛涚姸鎬?寮€鍙戜腑锛涘墠缃?鎸夌 16.1 鑺傚悇瀛愰」銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:879:- 浼楀瑕佹眰涓庣幇鐘讹細[鏉ユ簮澶嶅埗妫€鏌ュ櫒][E28] 楠岃縼鍏ュ揩鐓э紝
涓嶆槸鍝佺墝娈嬬暀鎴栨敼鍚嶅吋瀹规鏌ュ櫒锛涘巻鍙叉瀯寤鸿褰曚笉璇佹槑鏀瑰悕鍚庡彲鐢ㄣ€傛竻鍗曚腑鐨勫疄闄呰矾寰勪篃浼氬彈鐩綍/鍖呴噸鍛藉悕褰卞搷銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:880:- 璋冩暣锛氬湪 ZS-GOV-001/ZS-OPS-001 鐨勫悓涓€鍏ュ彛澧炲姞
鏂囦欢鍚?鐩綍/鏂囨湰/妯℃澘/鏋勫缓浜х墿鎵弿锛岃鐩?yudao/youdao/Yudao銆乧n.iocoder
 浜у搧鍖呫€佽妺閬撳強鍏跺畠浜у搧鍙鏃фā鏉挎爣璇嗐€傛寜鈥滃簲鏀?蹇呰淇濈暀/闄愭湡鍏煎鈥濈櫧鍚嶅崟鍒ゅ畾锛屼笉浠ュ叏浠撻浂鍛戒腑鎶瑰幓绗笁鏂规
潵婧愩€傜櫥璁?Git 鏀瑰悕鍓嶅悗璺緞鍙婂垎鎵硅瘉鎹紝鍚屾 README銆?1/02/03/05銆佽繍琛?閮ㄧ讲寮曠敤鍜屽綋鍓嶄唬鐮侀摼鎺ワ紱鍘嗗彶蹇
収/04
 鎶ュ憡涓嶉噸鍐欍€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:881:- 楠屾敹锛氭晠鎰忔仮澶嶆棫浜у搧鏍囬銆佹棫 import/鑷姩閰嶇疆娉ㄥ唽鎴栫
敓鎴愬櫒榛樿鍖呭悕鏃堕棬绂佸け璐ワ紱鍚堟硶 LICENSE/鏉ユ簮寮曠敤涓嶈鎶ワ紝鍏煎椤硅秴鏈熷簲澶辫触銆侭06 姹囨€绘柊鏃у懡鍚嶈縼绉汇€佸搧鐗岃
瑙変笌鍩虹绠＄悊/鏂囦欢/浠诲姟鍥炲綊锛岃褰曞浐瀹氭彁浜ゃ€佺幆澧冨拰鎴浘/娴嬭瘯缁撴灉锛涙敼鍚嶄笉鏇夸唬鏉冮檺銆佺湡瀹?PG 鎴栧彂甯冮獙鏀躲€?
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:882:- 鍒嗘壒浜や粯锛歓S-BRAND-006.A 鍦?B01 浜や粯鏈湴/CI 鍩虹
鎵弿锛孼S-BRAND-006.B 鍦?B06 鑱斿悎鍥炲綊鏀跺彛锛涙湰鐗堝彧鏈変换鍔″畾涔夛紝娌℃湁浜や粯妫€鏌ヨ剼鏈€?
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:883:- 寮€鍙戣褰曪紙2026-09-08锛夛細ZS-BRAND-006.A 鎻愪氦 
b2c26ea9銆備氦浠?`scripts/brand/verify-brand-naming.mjs`
 + [brand-naming-allowlist.json](../scripts/brand/brand-naming-allowlist.json)锛
堝簲鏀?蹇呰淇濈暀/闄愭湡鍏煎涓夌骇鍒ゅ畾锛寉d-* 闄愭湡椤瑰甫鍒版湡澶辨晥鏈哄埗锛? 7 涓棬绂?
娴嬭瘯锛汻EADME 鐧昏鏈湴/CI 鍏ュ彛骞惰鏄?verify-source-copy 蹇収闂ㄧ鐨勯€傜敤杈圭晫銆傚叏浠?13,417 涓枃浠舵
壂鎻忚繚瑙勬敹鏁涜嚦 0锛堜繚鐣?50,587 澶勫凡鐧昏鐨勬潵婧愮讲鍚嶅懡涓級锛涙晠鎰忔敞鍏ユ棫鏍囬/鏃?import/鐢熸垚鍣?
榛樿鍖呭悕楠岃瘉闂ㄧ澶辫触銆傛枃妗ｉ摼鎺ュ凡闅忓悇鏀瑰悕鎵规鍚屾锛?4 鎶ュ憡鏈噸鍐欙級銆傚緟楠屾敹璇存槑锛?B 褰?B06 鑱斿悎鍥炲綊鏀跺彛锛涚
湡瀹?CI 鎺ュ叆褰?ZS-OPS-001銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:884:
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:885:### ZS-GOV-001锛氭妸妯″潡浠诲姟杩借釜绾冲叆鏂囨。涓€鑷存€ф鏌?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:905:鏈竻鍗曚笉鏀瑰彉 03 鏂囨。鐨?B01锝濨03 鍏卞悓鍓嶇疆涓庡悗缁壒娆￠棬绂
侊紱B02 鎶€鏈獙璇佷笉瑕佹眰鏈粡鎵瑰噯鐨勮韩浠芥ā鍨嬫彁鍓嶈惤鍦般€備换浣曚笟鍔¤寖鍥淬€佹ā鍨嬫垨澶栭儴鎴愭湰鍙樺寲浠嶅洖鍒?02 涓诲彴璐﹀喅绛栥
€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:906:
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:907:### 16.1 璺ㄦ壒娆″瓙椤逛笌寮€宸ュ墠缃?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:908:
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:909:鏈〃鍙粏鍖?03 鏂囨。鏃㈡湁 B00锝濨11锛屼笉澧炲姞鏂版壒娆°€傝〃涓緷
璧栨槸鏄庣‘浜や粯鐗╃殑寮€宸ュ墠缃紱鍦ㄦ壒娆″唴鍏堝啓澶辫触娴嬭瘯锛屾敼閫犱笌鑱斿悎楠屾敹鍙互骞惰鍑嗗锛屾渶缁堟斁琛岄』璇ユ壒鍏ㄩ儴閫傜敤浜や粯鐗╅
€氳繃銆傗€?
鍚堝悓纭畾鈥欍€佲€樺す鍏峰彲鐢ㄢ€欍€佲€樺弬鑰?澶嶇敤鈥欎笉琛ㄧず鏁撮」鍚庣画鑱旈獙宸插畬鎴愩€備笂涓€鎵逛粎楠岃鎵瑰瓙椤癸紝涓嶈姹傚悓涓€涓讳换鍔
℃湭鏉ュ瓙椤瑰悓鏃剁粨鏉熴€?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:910:
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:911:姣忎釜瀛愰」缁ф壙涓讳换鍔＄殑 FND/WP銆佷紭鍏堢骇銆佷唬鐮佷緷鎹€佽皟鏁翠笌
姝ｅ弽鍚戦獙鏀讹紱琛ㄥ唴澧炲姞鍒嗘壒鑼冨洿銆佸墠缃拰閫€鍑烘潯浠躲€俈1.5 瀛愰」鐘舵€侊細鍝佺墝涓撻」鐨?ZS-BRAND-003.A銆乑S-B
RAND-004.A銆乑S-BRAND-006.A 宸查殢棣栬疆寮€鍙戝畬鎴愶紙寰呭搴旀壒娆℃斁琛岄獙鏀讹級锛屽叾浣?B01锝濨06 瀛愰」浠嶄负寰呭紑鍙戯
紝鍚粍缁?棣栭摼鍐崇瓥鐨?B08/B09 瀛愰」涓哄緟鍓嶇疆锛沍S-CLIENT-005.C 涓哄緟鍓嶇疆锛堢粓绔氦浠樿寖鍥达級锛?
ZS-OPS-002.C 涓哄緟寮€鍙戜絾椤绘弧瓒冲叾鐜/浠诲姟鍓嶇疆銆備富浠诲姟缁熻鎸夌 2 鑺傦紝涓嶅皢瀛愰」璁℃垚鏂扮殑涓讳换鍔°€傚瓙椤规槸鍚
﹀叿澶囧紑宸ユ潯浠朵笌涓讳换鍔＄姸鎬佹槸涓や釜缁村害銆?
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:912:
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:913:| 绋冲畾瀛愰」 | 楠屾敹鎵规 | 寮€宸ュ墠缃氦浠樼墿 | 鏈壒浜や粯 |
 鏈壒閫€鍑烘潯浠讹紙鍙﹀惈涓诲崱楠屾敹锛?|
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:953:| ZS-CLIENT-005.B | B06 | ZS-CLIENT-005.A銆
乑S-CLIENT-001.A銆乑S-CLIENT-002.A銆乑S-CLIENT-003銆?
ZS-CLIENT-004銆乑S-MSG-003.B | 涓ょ鐧诲綍銆佸鑸€佹枃浠躲€佸緟鍔?E2E 鍥炲綊 | 鐪熷疄鎺ュ彛/PG 鐜姝ｅ弽鍚戦€
氳繃锛涙瀯寤哄拰鎺ュ彛鏇胯韩涓嶄唬鏇胯仈璋?|
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:954:| ZS-CLIENT-005.C | B11 | ZS-CLIENT-005.B 
| 鎵瑰噯缁堢鐨勭湡鏈恒€佽澶囪兘鍔涘強鍙戝竷楠屾敹 | 寮辩綉/鎷掔粷鏉冮檺/鎭㈠鏈夎瘉鎹紱App 鍙戝竷鍙﹂渶鑼冨洿
銆佺鍚嶏紱涓嶇瓑寰?AI 鍐崇瓥 |
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:955:| ZS-OPS-001.A | B01 | ZS-GOV-001銆乑S-CLIEN
T-005.A | 鍩虹鏋勫缓/鏂囨。娴佹按绾块鏋?| 鏈湴涓?CI 鍚岃鍒欙紝鍧忛摼鎺?鏋勫缓澶辫触闃绘
鏀捐锛涗笉绛夊緟 PG/澶氱 E2E |
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:956:| ZS-OPS-001.B | B02 | ZS-OPS-001.A銆乑S-DB-
019.B | PG 灞傛祦姘寸嚎鎺ュ叆 | 鍥哄畾鐜鍜屾彁浜わ紝缂?PG/杩佺Щ澶辫触涓嶅彲缁跨伅 |
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:957:| ZS-OPS-001.C | B03 | ZS-OPS-001.B銆乑S-SEC
-012.B銆乑S-SYS-001.A | 瀹夊叏/鍩虹绠＄悊 API 灞傞棬绂?| 鐪熷疄杩囨护鍣ㄥ紑
鍚紝瓒婃潈/鍩虹鍔熻兘澶辫触闃绘 B03 |
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:958:| ZS-OPS-001.D | B06 | ZS-OPS-001.C銆乑S-CLI
ENT-005.B銆乑S-SYS-001.B | Web 涓庡皬绋嬪簭鎶€鏈仈璋冮棬绂?| 鐣岄潰鎿嶄綔
鍜屾帴鍙ｄ竴鑷达紱璺宠繃蹇呮祴鍦烘櫙涓嶅緱鏀捐 |
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:963:| ZS-SYS-001.A | B03 | ZS-DB-019.A銆乑S-SEC-
012.A銆乑S-SEC-008銆乑S-PERM-001.A銆乑S-PERM-004.A銆乑
S-CFG-001.B銆乑S-CFG-002.B銆乑S-CFG-003.B銆乑S-CFG-004銆乑S-IAM-003 | 涓冪被鍩虹绠＄悊鐪熷疄 PG/A
PI 鍥炲綊 | 瑙?15.1 鐭╅樀鍏ㄩ儴姝ｅ弽鍚戠敤渚嬶紝涓嶇瓑寰?Web E2E |
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:964:| ZS-SYS-001.B | B06 | ZS-SYS-001.A銆乑S-CLI
ENT-001.A | 涓冪被 Web 鍩虹绠＄悊鎿嶄綔鍙婂彇鏉冭仈楠?| 鍚岀煩闃甸€氳繃椤甸潰鎿嶄綔澶嶉獙锛涙暟
鎹寔涔呭寲鍜屽悗绔嫆缁濆彲楠岃瘉 |
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:965:| ZS-BRAND-003.A | B01 | ZS-BRAND-001銆乑S-C
LIENT-005.A | 鍘熷浘琛嶇敓璧勬簮銆侀潤鎬佸搧鐗屽拰涓ょ浜у搧鍖呭悕 | 鍩虹鏋勫缓閫氳繃锛涙棫鍙妯?
鏉挎爣璇嗘竻鐞嗭紝鏉ユ簮淇℃伅淇濈暀 |
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:966:| ZS-BRAND-003.B | B06 | ZS-BRAND-003.A銆乑S
-CLIENT-001.A銆乑S-CLIENT-002.A銆乑S-BRAND-004.B |
 鐪熷疄鐧诲綍/瀵艰埅鍦烘櫙涓殑澶氱鍝佺墝鍙敤鎬?| 妗岄潰/绐勫睆/鎶樺彔/涓婚鍥炲綊锛屽浘鏂囨纭笖鏃犳棫鍝佺墝鍥為€€ |
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:967:| ZS-BRAND-004.A | B02 | ZS-BRAND-001銆乑S-B
RAND-002銆乑S-DB-003銆乑S-DB-004銆乑S-DB-019.A | 閰嶇疆銆?
绉嶅瓙鍜岀‘闇€鍙樻洿鐨勬寔涔呭寲寮曠敤杩佺Щ | 鏂拌/鍗囩骇/閲嶈窇/鎭㈠閫氳繃锛涙棤瀛橀噺闇€鏈夎瘉鎹?|
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:968:| ZS-BRAND-004.B | B03 | ZS-BRAND-004.A銆乑S
-LOGIN-005.A銆乑S-CLIENT-003 | 缂撳瓨銆佹祻瑙堝櫒瀛樺偍鍜屼細璇濆懡鍚嶅垏鎹?|
 閫€鍑?鎾ゆ潈/骞跺彂鍒锋柊涓嶇粫杩囷紱鍏煎绐楀彛鍙婇噸鐧诲綍绛栫暐鏄庣‘ |
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:969:| ZS-BRAND-004.C | B05 | ZS-BRAND-004.B銆乑S
-JOB-002銆乑S-JOB-003銆乑S-DB-016 | 浠诲姟涓庢秷鎭寔涔呭寲寮曠敤鍜屾柊鏃у垏
鎹?| 鎭㈠/閲嶅娑堣垂鏃犱涪澶变笌閲嶅鍓綔鐢紝鏃х被寮曠敤鍙楁帶杩佺Щ |
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:970:| ZS-BRAND-006.A | B01 | ZS-BRAND-002銆乑S-B
RAND-003.A銆乑S-OPS-001.A | 鍝佺墝/浠ｇ爜娈嬬暀娓呭崟鍙婂熀纭€ CI 妫€鏌?|
 婕忔敼娉ㄥ叆澶辫触锛屾潵婧愪緥澶栦笉璇姤锛涗笉绛夊緟鍔熻兘鑱旈獙 |
> docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:971:| ZS-BRAND-006.B | B06 | ZS-BRAND-006.A銆乑S
-BRAND-003.B銆乑S-BRAND-004.C銆乑S-BRAND-005銆乑S-SY
S-001.B銆乑S-CLIENT-005.B銆乑S-OPS-001.D | 鍝佺墝銆佹敼鍚嶅吋瀹逛笌浜у搧鍔熻兘鑱斿悎鏀跺彛 | 鐪熷疄 PG/涓ょ/浠诲
姟缁撴灉鍙婃敼鍚嶅悗鐨勬枃妗ｉ摼鎺ュ潎鏈夎瘉鎹?|
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:972:
  docs\05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md:973:鏈媶鍒嗕换鍔′粛鎸変富鍗℃墽琛岋紱璺ㄦ壒娆¤ˉ鍏呬笉寰楄瑙ｉ噴涓烘棭鏈熸暣鍗″墠
缃細ZS-ENG-004/005/006 鍦?B01 瀹屾垚寮€鍏?鏆撮湶妫€鏌ワ紝B03/B05 鐨勬繁娴嬪垎鍒綊瀹夊叏/浠诲姟鍗★紱ZS
-DB-005/014/015/018/020 鍦?B02 棣栭獙锛屼細璇濄€佺粍缁囧敮涓€鎬у拰棰嗗煙骞跺彂鍒嗗埆鐢?LOGIN銆乑S-DB-010/IAM
/PERM銆丅PM 鎵挎帴锛沍S-SEC-003 鐨勬枃浠朵氦浠樺綊 ZS-FILE-004.A銆俍S-LO
GIN-003 涓?ZS-IAM-003 鐨勬妧鏈敓鍛藉懆鏈熷湪 B03 楠岋紝浠昏亴鎵╁睍褰?ZS-IAM-002/004銆俍S-CFG-004 鍦?B0
3 楠屽€兼牎楠屻€丅04 澶嶉獙瀹¤锛沍S-AUDIT-001 鍦?B04 浣跨敤鍚屽簱瀹¤锛屼笉鍏堜緷璧?
B05銆俍S-MSG-001/002/004 涓?ZS-JOB-003 鐨勬妧鏈棴鐜湪 B05 楠岋紝浠昏亴/棰嗗煙/鐪熷疄娓犻亾鍦ㄨ幏鎵规椂鍒嗗埆澶嶉獙
锛沍S-BPM-002 鐨勫疄闄呮祦绋嬭祫鏍煎湪 B09锛屼笉闃诲 B08銆俍S-CLIENT-003 鐨勭櫥褰曡姹?
鍚堝悓鍦?B03 楠岋紝B06 瀵规柊椤甸潰/闄勪欢鍐嶆鍥炲綊銆俍S-GOV-001 鍦?B00/B01 寤虹珛鏈湴涓庢枃妗?CI 瑙勫垯锛屽悗缁粎鎵╁
厖瑕嗙洊锛沍S-OPS-003 涓嶄綔涓?B01锝濨10 鐨勬暣浣撻獙鏀跺墠缃紝鍚勫悗缃兘鍔涚嫭绔嬭В閿併€?
/**
 * ZS-OPS-001.A 鏈湴鍩虹嚎闂ㄧ鑱氬悎鍏ュ彛锛堟湰鍦颁笌 CI 鍚屼竴瑙勫垯锛夈€?
 *
 * 渚濇鎵ц鍏ㄩ儴鏈湴妫€鏌ワ紙蹇?鈫?鎱級锛屼换涓€澶辫触鍗虫暣浣撳け璐ュ苟姹囨€伙細
 *   1. 鏉ユ簮澶嶅埗鏍￠獙鍣ㄥ崟娴嬶紙verify-source-copy.test.mjs锛?
 *   2. 鍝佺墝鍛藉悕闂ㄧ鍗曟祴 + 鍛藉悕杩佺Щ宸ュ叿鍗曟祴 + 鍏ㄤ粨鎵弿
 *   3. 鏂囨。涓€鑷存€у崟娴?+ 鍏ㄦ枃妗ｆ壂鎻忥紙ZS-GOV-001锛?
 *   4. 妯″潡鐧藉悕鍗曢潤鎬佹鏌ワ紙ZS-ENG-001锛?
 *   5. 鏁版嵁婧?PG 鍚堝悓妫€鏌ワ紙ZS-DB-001.A锛?
 *   6. Flyway 杩佺Щ瑙勮寖妫€鏌ワ紙ZS-DB-003锛?
 *   7. 閰嶇疆绉樺瘑闂ㄧ锛圸S-CFG-001.A锛?
 *   8. Web 绫诲瀷妫€鏌ュ熀绾匡紙ZS-CLIENT-005.A锛岃緝鎱紱--fast 璺宠繃锛?
 *   9. 鍚敤妯″潡鍚庣鍗曟祴锛?-mvn 鏄惧紡鍚敤锛涢渶 tools/env.sh 宸ュ叿閾撅紝鎺掗櫎宸茬櫥璁扮殑涓婃父鍩虹嚎澶辫触锛?
 * PG/澶氱 E2E 闂ㄧ鎸?ZS-OPS-001.B~.E 鎵规鎺ュ叆锛屼笉鍦ㄦ湰楠ㄦ灦銆?
 * 鐢ㄦ硶锛歯ode scripts/ops/run-local-gates.mjs [--fast] [--mvn]
 */
import { execFileSync, spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));
const fast = process.argv.includes('--fast');
const withMvn = process.argv.includes('--mvn');

const gates = [
  { id: 'G1 鏉ユ簮澶嶅埗鏍￠獙鍣ㄥ崟娴?, cmd: ['node', '--test', 'scripts/verify-source-copy
.test.mjs'] },
  { id: 'G2 鍝佺墝鍛藉悕闂ㄧ鍗曟祴', cmd: ['node', '--test', 'scripts/brand/verify-brand-
naming.test.mjs'] },
  { id: 'G2b 鍛藉悕杩佺Щ宸ュ叿鍗曟祴锛堝喕缁撴枃浠舵帓闄?+ 绉熸埛鍩熷悕鍞竴锛?, cmd: ['node', '--test', 'scr
ipts/brand/apply-naming-migration.test.mjs'] },
  { id: 'G3 鍝佺墝鍛藉悕鍏ㄤ粨鎵弿', cmd: ['node', 'scripts/brand/verify-brand-naming.mjs
'] },
  { id: 'G4 鏂囨。涓€鑷存€у崟娴?, cmd: ['node', '--test', 'scripts/gov/verify-docs.test
.mjs'] },
  { id: 'G5 鏂囨。涓€鑷存€у叏閲?, cmd: ['node', 'scripts/gov/verify-docs.mjs'] },
  { id: 'G6 妯″潡鐧藉悕鍗?, cmd: ['node', 'scripts/eng/verify-module-whitelist.mjs'] 
},
  { id: 'G7 鏁版嵁婧?PG 鍚堝悓', cmd: ['node', 'scripts/db/verify-datasource-pg.mjs'] 
},
  { id: 'G8 Flyway 杩佺Щ瑙勮寖', cmd: ['node', 'scripts/db/verify-flyway-migrations.
mjs'] },
  { id: 'G9 閰嶇疆绉樺瘑闂ㄧ', cmd: ['node', 'scripts/cfg/verify-config-secrets.mjs'] 
},
];
if (!fast) {
  gates.push({ id: 'G10 Web 绫诲瀷妫€鏌ュ熀绾?, cmd: ['node', 'scripts/client/verify-ts
-baseline.mjs'] });
}
if (withMvn) {
  gates.push({ id: 'G11 鍚敤妯″潡鍚庣鍗曟祴锛坈ommon/infra锛屾帓闄や笂娓稿熀绾垮け璐ワ級',
    mvnArgs: "-pl zszj-framework/zszj-common,zszj-module-infra -am -Dtest=!Code
genEngineUniappTest#testExecute_treeSearch -Dsurefire.failIfNoSpecifiedTests=fa
lse test" });
}

const results = [];
let failed = false;
for (const gate of gates) {
  let status = 'PASS';
  let output = '';
  try {
    if (gate.mvnArgs) {
      // Maven 闂ㄧ闇€鍏堟敞鍏?tools 宸ュ叿閾剧幆澧冿紙JDK17/Maven 涓嶅湪绯荤粺 PATH锛?
      const r = spawnSync('bash', ['-c', 'source tools/env.sh && cd services/zh
ongshu-core && MSYS_NO_PATHCONV=1 "$TOOLS/apache-maven-3.9.9/bin/mvn.cmd" ' + g
ate.mvnArgs], {
        cwd: root, maxBuffer: 256 * 1024 * 1024, encoding: 'utf8',
        stdio: ['ignore', 'pipe', 'pipe'],
      });
      if (r.status !== 0) throw { stdout: r.stdout ?? '', stderr: r.stderr ?? '
' };
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
  results.push({ id: gate.id, status, tail: output.trimEnd().split('\n').slice(
-3).join(' | ').slice(0, 200) });
  console.log(`[${status}] ${gate.id}`);
}

console.log('\n===== 鏈湴鍩虹嚎闂ㄧ姹囨€?=====');
for (const r of results) console.log(`${r.status.padEnd(4)}  ${r.id}`);
const failCount = results.filter((r) => r.status === 'FAIL').length;
console.log(`鍚堣 ${results.length} 椤癸紝澶辫触 ${failCount} 椤筦);
process.exit(failed ? 1 : 0);
/**
 * ZS-BRAND-006.A 鍝佺墝/浠ｇ爜鍛藉悕娈嬬暀闂ㄧ锛堟湰鍦颁笌 CI 鍚屼竴鍏ュ彛锛夈€?
 *
 * 鐢ㄦ硶锛歯ode scripts/brand/verify-brand-naming.mjs
 * 鎵弿 git 璺熻釜鏂囦欢涓殑鏃т骇鍝佹爣璇嗭紙鏂囦欢鍚?+ 鏂囨湰鍐呭锛夛細
 *   cn.iocoder / yudao / Yudao / YUDAO / youdao / 鑺嬮亾 / unibest锛堜笂娓告ā鏉挎爣璇嗭級
 * 鎸?scripts/brand/brand-naming-allowlist.json 鍒ゅ畾锛?
 *   path 鏉＄洰鏁存枃浠舵斁琛岋紱content 鏉＄洰浠呮斁琛屽懡涓叾 content 姝ｅ垯鐨勫尮閰嶏紱
 *   闄愭湡鍏煎鏉＄洰 expires 鍒版湡鍚庤鍚屾湭鏀捐锛堥棬绂佸け璐ワ級銆?
 * 鏈斁琛岀殑娈嬬暀浣胯繘绋嬩互闈為浂閫€鍑猴紱涓嶈拷姹傚叏浠撻浂鍛戒腑锛屼笉鎶瑰幓绗笁鏂规潵婧愩€?
 */
import { readFileSync, existsSync, statSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';

const root = fileURLToPath(new URL('../../', import.meta.url));
const allowlistFile = 'scripts/brand/brand-naming-allowlist.json';

// 鏃т骇鍝佹爣璇嗘ā寮忥紙docs/06 绗?2 鑺傦紱unibest 涓虹櫥璁扮殑涓婃父妯℃澘鏍囪瘑锛?
// yd- 涓虹櫥璁扮缉鍐欙紙docs/06 绗?4.3 鑺傞檺鏈熷吋瀹癸級锛岀撼鍏ユā寮忎娇鍒版湡鎾ら櫎鏈哄埗鐢熸晥锛?
const PATTERNS = [
  { name: 'cn.iocoder', re: /cn\.iocoder/g },
  { name: 'yudao', re: /yudao/gi },
  { name: 'youdao', re: /youdao/gi },
  { name: '鑺嬮亾', re: /鑺嬮亾/g },
  { name: 'unibest', re: /unibest/gi },
  { name: 'yd-prefix', re: /\byd-[a-z]/g },
];
// hotfix-B P2-7: SVG 鏄?XML 鏂囨湰锛屽彲鍚?<text>/<title> 绛夊搧鐗屼覆锛屼笉鍐嶄綔涓轰簩杩涘埗璺宠繃锛堝鍑轰互渚
涘崟娴嬪浐鍖栵級
export const BINARY = /\.(png|jpe?g|gif|ico|bmp|webp|ttf|woff2?|eot|mp3|mp4|xdb
|jar|zip|gz)$/i;
const SKIP_DIRS = /(^|\/)(\.git|node_modules|dist|dist-prod|target|unpackage|\.
vite|\.idea)(\/|$)/;

export function loadAllowlist(read = (p) => readFileSync(p, 'utf8')) {
  const parsed = JSON.parse(read(allowlistFile));
  if (!Array.isArray(parsed.entries)) throw new Error('invalid allowlist');
  return parsed.entries.map((e) => ({
    ...e,
    pathRe: e.path ? new RegExp(e.path) : null,
    contentRes: (e.content ?? []).map((c) => new RegExp(c, 'g')), // g 鏍囧織鐢ㄤ簬 c
overage check 閬嶅巻鎵€鏈夊尮閰嶏紱姣忔浣跨敤鍓嶉噸缃?lastIndex
    expiresAt: e.expires ? new Date(`${e.expires}T23:59:59Z`) : null,
  }));
}

function entryExpired(entry, now = new Date()) {
  return entry.expiresAt !== null && now > entry.expiresAt;
}

/** 瀵瑰崟涓枃浠剁殑鍛戒腑鍋氱櫧鍚嶅崟鍒ゅ畾锛涜繑鍥炴湭鏀捐鍛戒腑銆?*/
export function judge(relativePath, text, entries, now = new Date()) {
  // hotfix-B P1-3: pathPass 浠呭鏃?scope 鎴?scope=path 鐨勬潯鐩敓鏁堬紱
  // scope=content 鐨勬潯鐩繀椤荤粡鍐呭鍖归厤鍒ゅ畾锛屼笉寰楁暣鏂囦欢璞佸厤
  const pathPass = entries.some((e) =>
    e.pathRe && e.pathRe.test(relativePath) &&
    (!e.scope || e.scope === 'path') &&
    !entryExpired(e, now)
  );
  const hits = [];
  for (const { name, re } of PATTERNS) {
    re.lastIndex = 0; // 妯″潡绾?/g 姝ｅ垯锛岄€愭枃浠堕噸缃?
    let m;
    while ((m = re.exec(text))) hits.push({ pattern: name, index: m.index, text
: m[0] });
  }
  if (pathPass) return { allowed: hits, violations: [] };
  const violations = [];
  for (const hit of hits) {
    const fragment = text.slice(Math.max(0, hit.index - 40), hit.index + 40);
    // hotfix-B P2-4: 鐧藉悕鍗?content 姝ｅ垯鐨勫尮閰嶅尯闂村繀椤昏鐩栧懡涓偣锛?
    // 闃叉閭昏繎 40 瀛楃鍐呯殑涓嶇浉鍏崇櫧鍚嶅崟涓插簢鎶や骇鍝佸彲瑙佸搧鐗屾畫鐣?
    const hitOffset = Math.min(40, hit.index);
    const ok = entries.some((e) => {
      if (!e.scope || e.scope !== 'content' || entryExpired(e, now)) return fal
se;
      if (e.pathRe && !e.pathRe.test(relativePath)) return false;
      return e.contentRes.some((cre) => {
        cre.lastIndex = 0;
        let cm;
        while ((cm = cre.exec(fragment))) {
          if (cm.index <= hitOffset && hitOffset < cm.index + cm[0].length) ret
urn true;
        }
        return false;
      });
    });
    if (!ok) violations.push(hit);
  }
  return { allowed: hits.filter((h) => !violations.includes(h)), violations };
}

export function scanTree(entries, now = new Date()) {
  const files = execFileSync('git', ['ls-files', '-z'], { cwd: root, maxBuffer:
 64 * 1024 * 1024 })
    .toString()
    .split('\0')
    .filter(Boolean);
  const report = { scanned: 0, allowedHits: 0, violations: [] };
  for (const rel of files) {
    if (SKIP_DIRS.test(rel)) continue;
    // hotfix-B P2-6: 鏂囦欢/鐩綍鍚嶆鏌モ€斺€斿瀹屾暣鐩稿璺緞搴旂敤鍏ㄩ儴 PATTERNS锛?
    // 涓嶄粎妫€鏌?basename + 3 涓ā寮忥紝閬垮厤鐩綍娈嬬暀涓庨仐婕忔ā寮忛€冮€?
    const nameHits = [];
    for (const { name, re } of PATTERNS) {
      re.lastIndex = 0;
      let m;
      while ((m = re.exec(rel))) nameHits.push({ pattern: name, index: m.index,
 text: m[0] });
    }
    const abs = `${root}${rel.replaceAll('/', process.platform === 'win32' ? '\
\' : '/')}`;
    if (nameHits.length) {
      const { violations } = judge(rel, rel, entries, now); // 鐢ㄨ矾寰勬枃鏈嚜韬仛鍒ゅ畾锛
堝惈璺緞鐧藉悕鍗曪級
      for (const v of violations) report.violations.push({ path: rel, pattern: 
v.pattern, sample: rel });
      if (!violations.length) report.allowedHits += nameHits.length;
    }
    if (BINARY.test(rel) || !existsSync(abs) || !statSync(abs).isFile()) contin
ue;
    let text;
    try {
      const buf = readFileSync(abs);
      if (buf.subarray(0, 8192).includes(0)) continue;
      text = buf.toString('utf8');
    } catch {
      continue;
    }
    report.scanned++;
    const { allowed, violations } = judge(rel, text, entries, now);
    report.allowedHits += allowed.length;
    for (const v of violations) {
      report.violations.push({ path: rel, pattern: v.pattern, sample: text.slic
e(Math.max(0, v.index - 30), v.index + 30).replaceAll('\n', ' ') });
    }
  }
  return report;
}

const invokedDirectly = process.argv[1] && process.argv[1].replaceAll('\\', '/'
).endsWith('scripts/brand/verify-brand-naming.mjs');
if (invokedDirectly) {
  try {
    const entries = loadAllowlist();
    const report = scanTree(entries);
    console.log(JSON.stringify({ ...report, violations: report.violations.slice
(0, 50) }, null, 2));
    console.error(`scanned=${report.scanned} allowedHits=${report.allowedHits} 
violations=${report.violations.length}`);
    process.exitCode = report.violations.length ? 1 : 0;
  } catch (error) {
    console.error(`brand naming check failed: ${error.message}`);
    process.exitCode = 2;
  }
}
{
  "javaFiles": 6776,
  "hardIssues": [],
  "hardIssueCount": 0,
  "residueFiles": 48,
  "residueTop": [
    [
      "services/zhongshu-core/sql/postgresql/upgrades/20260908_brand_rename_zsz
j.sql",
      26
    ],
    [
      "services/zhongshu-core/zszj-module-bpm/src/main/java/cn/zszj/module/bpm/
service/task/BpmTaskServiceImpl.java",
      14
    ],
    [
      "services/zhongshu-core/zszj-module-infra/src/main/resources/codegen/READ
ME.md",
      5
    ],
    [
      "services/zhongshu-core/.github/workflows/yudao-ui-admin.yml",
      4
    ],
    [
      "services/zhongshu-core/zszj-module-bpm/src/main/java/cn/zszj/module/bpm/
service/task/BpmProcessInstanceServiceImpl.java",
      4
    ],
    [
      "services/zhongshu-core/zszj-module-pay/src/main/java/cn/zszj/module/pay/
framework/pay/core/client/impl/weixin/AbstractWxPayClient.java",
      3
    ],
    [
      "services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz-ip/sr
c/main/java/cn/zszj/framework/ip/core/Area.java",
      2
    ],
    [
      "services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-security/
銆婅妺閬?Spring Boot 瀹夊叏妗嗘灦 Spring Security 鍏ラ棬銆?md",
      2
    ],
    [
      "services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/m
ain/java/cn/zszj/framework/web/core/handler/GlobalExceptionHandler.java",
      2
    ],
    [
      "services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/in
fra/service/logger/ApiErrorLogServiceImpl.java",
      2
    ],
    [
      "services/zhongshu-core/zszj-module-mall/zszj-module-promotion/src/main/j
ava/cn/zszj/module/promotion/service/coupon/CouponTemplateServiceImpl.java",
      2
    ],
    [
      "services/zhongshu-core/zszj-module-mall/zszj-module-trade/src/main/java/
cn/zszj/module/trade/convert/order/TradeOrderConvert.java",
      2
    ],
    [
      "services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/s
ystem/service/social/SocialUserServiceImpl.java",
      2
    ],
    [
      "services/zhongshu-core/zszj-framework/zszj-common/銆婅妺閬?Spring Boot 鍙傛暟鏍￠
獙 Validation 鍏ラ棬銆?md",
      1
    ],
    [
      "services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz-tenan
t/src/main/java/cn/zszj/framework/tenant/core/security/TenantSecurityWebFilter.
java",
      1
    ],
    [
      "services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-excel/src
/main/java/cn/zszj/framework/excel/core/handler/ColumnWidthMatchStyleStrategy.j
ava",
      1
    ],
    [
      "services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-job/銆婅妺閬?
Spring Boot 瀹氭椂浠诲姟鍏ラ棬銆?md",
      1
    ],
    [
      "services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-job/銆婅妺閬?
Spring Boot 寮傛浠诲姟鍏ラ棬銆?md",
      1
    ],
    [
      "services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-monitor/銆
婅妺閬?Spring Boot 鐩戞帶宸ュ叿 Admin 鍏ラ棬銆?md",
      1
    ],
    [
      "services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-monitor/銆
婅妺閬?Spring Boot 鐩戞帶绔偣 Actuator 鍏ラ棬銆?md",
      1
    ],
    [
      "services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-monitor/銆
婅妺閬?Spring Boot 閾捐矾杩借釜 SkyWalking 鍏ラ棬銆?md",
      1
    ],
    [
      "services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-mq/銆婅妺閬?S
pring Boot 浜嬩欢鏈哄埗 Event 鍏ラ棬銆?md",
      1
    ],
    [
      "services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-mq/銆婅妺閬?S
pring Boot 娑堟伅闃熷垪 Kafka 鍏ラ棬銆?md",
      1
    ],
    [
      "services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-mq/銆婅妺閬?S
pring Boot 娑堟伅闃熷垪 RabbitMQ 鍏ラ棬銆?md",
      1
    ],
    [
      "services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-mq/銆婅妺閬?S
pring Boot 娑堟伅闃熷垪 RocketMQ 鍏ラ棬銆?md",
      1
    ],
    [
      "services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-mybatis/s
rc/main/java/cn/zszj/framework/mybatis/core/util/MyBatisUtils.java",
      1
    ],
    [
      "services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-mybatis/銆
婅妺閬?Spring Boot MyBatis 鍏ラ棬銆?md",
      1
    ],
    [
      "services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-mybatis/銆
婅妺閬?Spring Boot 澶氭暟鎹簮锛堣鍐欏垎绂伙級鍏ラ棬銆?md",
      1
    ],
    [
      "services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-mybatis/銆
婅妺閬?Spring Boot 鏁版嵁搴撹繛鎺ユ睜鍏ラ棬銆?md",
      1
    ],
    [
      "services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-redis/src
/main/java/cn/zszj/framework/redis/config/ZszjCacheAutoConfiguration.java",
      1
    ]
  ]
}



exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '$Out
putEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new(); node scri
pts/ops/run-local-gates.mjs --fast; $gateExit = $LASTEXITCODE; $lines = Get-Con
tent -Encoding utf8 -LiteralPath '"'docs/05-搴曞骇妯″潡鍒嗘瀽涓庡紑鍙戜换鍔℃竻鍗?md'; "'$ranges 
= @(@(805,855),@(885,903),@(952,971)); foreach ($range in $ranges) { for ($i = 
$range[0]; $i -le $range[1]; $i++) { '"'{0}: {1}' -f "'$i,$lines[$i-1] } }; exi
t $gateExit' in E:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇
 exited 1 in 2656ms:
[FAIL] G1 鏉ユ簮澶嶅埗鏍￠獙鍣ㄥ崟娴?[PASS] G2 鍝佺墝鍛藉悕闂ㄧ鍗曟祴
[PASS] G2b 鍛藉悕杩佺Щ宸ュ叿鍗曟祴锛堝喕缁撴枃浠舵帓闄?+ 绉熸埛鍩熷悕鍞竴锛?[PASS] G3 鍝佺墝鍛藉悕鍏ㄤ粨鎵弿
[PASS] G4 鏂囨。涓€鑷存€у崟娴?[PASS] G5 鏂囨。涓€鑷存€у叏閲?[PASS] G6 妯″潡鐧藉悕鍗?[PASS] G7 鏁版嵁婧?PG
 鍚堝悓
[PASS] G8 Flyway 杩佺Щ瑙勮寖
[PASS] G9 閰嶇疆绉樺瘑闂ㄧ

===== 鏈湴鍩虹嚎闂ㄧ姹囨€?=====
FAIL  G1 鏉ユ簮澶嶅埗鏍￠獙鍣ㄥ崟娴?PASS  G2 鍝佺墝鍛藉悕闂ㄧ鍗曟祴
PASS  G2b 鍛藉悕杩佺Щ宸ュ叿鍗曟祴锛堝喕缁撴枃浠舵帓闄?+ 绉熸埛鍩熷悕鍞竴锛?PASS  G3 鍝佺墝鍛藉悕鍏ㄤ粨鎵弿
PASS  G4 鏂囨。涓€鑷存€у崟娴?PASS  G5 鏂囨。涓€鑷存€у叏閲?PASS  G6 妯″潡鐧藉悕鍗?PASS  G7 鏁版嵁婧?PG 鍚堝悓
PASS  G8 Flyway 杩佺Щ瑙勮寖
PASS  G9 閰嶇疆绉樺瘑闂ㄧ
鍚堣 10 椤癸紝澶辫触 1 椤?805: | 鑿滃崟 | [鎺ュ彛](../services/zhongshu-core/zszj-module-syst
em/src/main/java/cn/zszj/module/system/controller/admin/permission/MenuControll
er.java)銆乕Web 椤甸潰](../apps/zhongshu-admin-web/src/views/system/menu/index.vue) 
| ZS-CFG-003.A銆乑S-CFG-003.B | SYS-MENU-P / SYS-MENU-N | 鐩綍/鑿滃崟/鎸夐挳閰嶇疆涓庢巿鏉冨悗鐨勫
鑸竴鑷达紱鍚堟硶鐖跺瓙鍏崇郴鍙繚瀛?| 闈炴硶鐖跺瓙鍏崇郴銆佹湁寮曠敤鍒犻櫎鍜屽叧闂ā鍧楀叆鍙ｅ彈鎺э紱鏃犳潈鐩磋皟鎷掔粷 |
806: | 宀椾綅 | [鎺ュ彛](../services/zhongshu-core/zszj-module-system/src/main/java/c
n/zszj/module/system/controller/admin/dept/PostController.java)銆乕Web 椤甸潰](../ap
ps/zhongshu-admin-web/src/views/system/post/index.vue) | ZS-IAM-003 | SYS-POST-
P / SYS-POST-N | 鏂板銆佷慨鏀广€佹煡璇㈠拰鐢ㄦ埛宀椾綅鍏宠仈鍙鍥烇紱鍋滅敤璇箟涓€鑷?| 鏈夊紩鐢ㄥ垹闄ゅ拰闈炴硶鐘舵€佸彈鎺э紱鏅
€氱敤鎴?閿欑鎴蜂笉鑳戒慨鏀?|
807: | 瀛楀吀 | [鎺ュ彛](../services/zhongshu-core/zszj-module-system/src/main/java/c
n/zszj/module/system/controller/admin/dict/DictTypeController.java)銆乕Web 椤甸潰](.
./apps/zhongshu-admin-web/src/views/system/dict/index.vue) | ZS-CFG-002.A銆乑S-CF
G-002.B | SYS-DICT-P / SYS-DICT-N | 绫诲瀷涓庡瓧鍏搁」澧炴敼鏌ャ€佹帓搴忋€佹爣绛炬樉绀哄強鍘嗗彶鏄剧ず涓€鑷?| 閲嶅
缂栫爜鍐茬獊銆佸仠鐢ㄩ」鏂板啓鍏ュ強鏈夊紩鐢ㄥ垹鏀瑰彈鎺э紱瓒婃潈鎷掔粷 |
808: | 閰嶇疆 | [鎺ュ彛](../services/zhongshu-core/zszj-module-infra/src/main/java/cn
/zszj/module/infra/controller/admin/config/ConfigController.java)銆乕Web 椤甸潰](../
apps/zhongshu-admin-web/src/views/infra/config/index.vue) | ZS-CFG-001.B銆乑S-CFG
-004 | SYS-CONFIG-P / SYS-CONFIG-N | 鑾峰噯鍙傛暟淇敼銆佸彲瑙佸€艰鍙栧強鐢熸晥鏂瑰紡绗﹀悎鍙傛暟鍚堝悓 | 闈炴硶鍊
笺€佹晱鎰熼」鍙鍖?瀵煎嚭銆佸唴缃繚鎶ょ粫杩囪鎷掍笖涓嶆硠瀵?|
809: | 鍏憡 | [鎺ュ彛](../services/zhongshu-core/zszj-module-system/src/main/java/c
n/zszj/module/system/controller/admin/notice/NoticeController.java)銆乕Web 椤甸潰](.
./apps/zhongshu-admin-web/src/views/system/notice/index.vue) | ZS-SEC-002銆乑S-SE
C-008 | SYS-NOTICE-P / SYS-NOTICE-N | 鏂板銆佺紪杈戙€佹煡璇€佺姸鎬佸彉鏇翠笌鍒犻櫎鎸夌幇鏈夊叕鍛婄鐞嗚涔夋寔涔
呭寲 | 鏃犳潈鍒涘缓/鏀瑰垹銆佺姸鎬佽繃婊ゅけ鏁堝強瀵屾枃鏈彲鎵ц鍐呭琚嫆/瀹夊叏缂栫爜锛涗笉寰楁妸鍏憡绛夊悓绉佷汉绔欏唴淇?|
810: 
811: 瀛楀吀椤瑰叆鍙ｅ彟瑙?[DictDataController](../services/zhongshu-core/zszj-module-syst
em/src/main/java/cn/zszj/module/system/controller/admin/dict/DictDataController
.java)銆傛祴璇曞厛浠?HandlerMapping 鍒楀嚭瀹為檯鍚敤鐨?HTTP 鏂规硶鍜岃矾寰勶紝鍐嶆妸姣忎釜鍏ュ彛缁戝畾鍒颁笂杩版鍙嶅悜鐢ㄤ緥锛
涗笉瀛樺湪鐨勫姛鑳藉簲璁板綍涓嶉€傜敤鍜屼緷鎹紝涓嶈櫄鏋勭鐐广€傛墍鏈夌敤渚嬩互鐙珛娴嬭瘯鏁版嵁杩愯锛氬彈淇濇姢璇锋眰缁忚繃鐪熷疄 Filter/MV
C/鏂规硶鏉冮檺锛屾寜鏃㈡湁琛ㄧ殑鍏ㄥ眬/鎶€鏈鎴峰垎绫婚獙鑼冨洿锛屼笉鑳芥妸鍏ㄥ眬鑿滃崟/瀛楀吀鏈烘鎸夌鎴峰鍒讹紱鍗囩骇鍚庢娊鏌ヨ鍥烇紝涓嶆敼鐪
熷疄涓氬姟鏁版嵁銆侭06 閫氳繃 Web 鎿嶄綔鎻愪氦骞跺啀娆¤鍙栵紝楠岃瘉閿欒鎻愮ず銆佹寜閽?鐩磋皟鎷掔粷鍜?PG 钀藉簱锛屼笉浠ユ埅鍥炬垨鍓嶇 Mo
ck 浠ｆ浛鐪熷疄缁撴灉銆?
812: 
813: 鎷熸柊澧炴墽琛屽叆鍙ｏ細鍚庣 `FoundationSystemManagementIntegrationTest`锛堟垨涓冪被绛変环鐙珛娴嬭瘯
锛夊強 Web `foundation-system-management` E2E 濂椾欢锛涜繘鍏ュ疄鐜版椂鍥哄畾鐩綍銆佽繍琛屽懡浠ゃ€佹暟鎹す鍏峰拰涓
婅堪娴嬭瘯缂栧彿鏄犲皠銆傚厛杩愯鍩虹嚎娴嬭瘯锛屽凡閫氳繃鍔熻兘淇濈暀澶嶇敤璇佹嵁锛涘瓨鍦ㄥけ璐ユ椂鍏堣褰曞け璐ョ粨鏋滐紝鍐嶅彧淇敼瀵瑰簲鍔熻兘銆傚叏閮?
7 琛岀殑姝ｅ弽鍚戝強鎵€鏈夊疄闄呭惎鐢ㄧ鐐规湁缁撴灉鍚庯紝鎵嶅厑璁告彁鍗?FND-SYS-001 瀵瑰簲灞傜骇鐘舵€併€?
814: 
815: ### 15.2 浼楀涔嬪鍝佺墝涓庝唬鐮佸懡鍚嶇粺涓€涓撻」
816: 
817: 鐢ㄦ埛浜?2026-09-08 鎻愪緵 Logo 鍜屸€滀紬澧呬箣瀹垛€濆悕绉帮紝骞惰姹傚簳搴у紑鍙戞椂鍚屾鏇挎崲鐜版湁鍝佺墝涓庝唬鐮佸墠缂€銆
傛湰娆″彧鐧昏寮€鍙戜换鍔★紝涓嶆墽琛屾簮鐮佹壒閲忔敼鍚嶃€傛墍璇寸殑鈥測oudao鈥濆湪褰撳墠婧愮爜涓富瑕佸啓浣?`yudao`锛涘悓鏃剁洏鐐逛骇鍝佸彲
瑙佺殑鑺嬮亾銆丷uoYi銆乽nibest 绛変笂娓告ā鏉挎爣璇嗭紝涓嶈兘鍙浛鎹竴涓瓧绗︿覆銆?
818: 
819: - 鍝佺墝渚濇嵁锛歔鐢ㄦ埛鎻愪緵鐨勫師濮?Logo](assets/brand/zszj-logo-user-reference.png)锛?060
脳228 PNG锛涘凡鍘熸牱鐣欐。锛孲HA-256 涓?`275efbf794ea5e485bf49111d8681170dc3b07e3d858e07fb3
2be9d7aeaabe5a`銆傝繖涓嶆槸宸插埗浣滃ソ鐨?favicon銆侀€忔槑鍥炬垨 App 鍥炬爣锛涗笉閲嶇粯鍟嗘爣銆佷笉鍑浘鐗囨帹鏂伐鍟嗗叏绉帮紝
姝ｅ紡椤佃剼/鍗忚鑻ラ渶浼佷笟鍏ㄧО鍙︽寜鐢ㄦ埛鎻愪緵淇℃伅濉啓銆?
820: - 灞曠ず鍚嶏細鍝佺墝/鍏徃灞曠ず鍚嶇О涓衡€滀紬澧呬箣瀹垛€濓紱骞冲彴鍏ㄧО娌跨敤鈥滀紬澧呬箣瀹?AI 璧嬭兘骞冲彴鈥濄€備互鍘熷浘鏋勫浘鍜岄
鑹蹭负渚濇嵁锛岄€傞厤鐧诲綍銆佸鑸拰灏忓昂瀵稿満鏅紝涓嶈嚜鍔ㄦ妸杩欐闇€姹傛墿鎴愭暣濂楃晫闈㈤噸璁捐銆?
821: - 鍛藉悕绾﹀畾锛氭寜鐢ㄦ埛鍏佽鑼冨洿锛屾嫙浠?`zszj` 涓轰骇鍝佷唬鐮佺粺涓€涓诲墠缂€锛岀被鍚嶄娇鐢?`Zszj`锛岀幆澧冨彉閲忎娇鐢?`
ZSZJ_`锛沗zs` / `Zs` / `ZS_` 浠呬綔涓哄懡鍚嶈〃涓槑纭櫥璁扮殑缂╁啓锛屼笉鑳戒袱涓墠缂€闅忔剰娣风敤銆傚畬鏁?Java 鍖呮牴銆
丟roupId 鍜岃祫婧愬悕绉板湪 ZS-BRAND-001 鍐荤粨鏄犲皠鍚庡疄鏂姐€?
822: - 鑼冨洿锛氫骇鍝佹嫢鏈夊苟缁存姢鐨勫悗绔€乄eb銆佺Щ鍔ㄧ鍙婂叾妯℃澘銆侀厤缃€佹祴璇曘€佹瀯寤?閮ㄧ讲寮曠敤鍧囬』鐩樼偣锛涙湭鍚敤妯″潡
鐨勪骇鍝佸懡鍚嶄篃绾冲叆锛屼笉鍥犳敼鍚嶅惎鐢ㄦā鍧椼€傚師濮嬫潵婧愪粨搴撱€侀殧绂讳緵浣撳揩鐓с€佺涓夋柟渚濊禆鍧愭爣/鏍囧噯 API銆丩ICENSE銆佷綔
鑰呯讲鍚嶅拰涓嶅彲鏀瑰啓鐨勫巻鍙叉潵婧愯瘉鎹繚鐣欙紱浜у搧瀵瑰鍏ュ彛涓庢硶寰?鏉ユ簮璇存槑鍒嗗紑楠屾敹銆?
823: - 鍏煎杈圭晫锛氫笉瑕佹眰鎶婃墍鏈夋暟鎹簱琛ㄣ€佷笟鍔″瓧娈垫垨鏅€氱被鍚嶅己鍔犲墠缂€锛涗粎杩佺Щ鎵胯浇鏃т骇鍝佹爣璇嗙殑椤广€傚綋鍓嶅伐浣
滃尯銆丟it 杩滅▼銆佸煙鍚嶃€佸凡娉ㄥ唽 AppID/Bundle ID/鍖呭悕鍙婂閮ㄥ洖璋冧笉鑷姩鏀瑰姩锛岄€愰」璇勪及鍚庢寜鎵瑰噯鑼冨洿瀹炴柦銆傜幇
鏈?`zhongshu-*` 浜у搧椤跺眰鐩綍鏄惁缁熶竴涓?`zszj-*` 鍦ㄦ槧灏勮〃鍒楁槑锛屼笉鑳芥棤璁板綍鎵╁ぇ涓轰粨搴?澶栭儴璧勬簮鎼縼銆?
824: - 婧愮爜閾炬帴浠嶆寚鍚戝綋鍓嶅疄闄呰矾寰勶紝涓嶉鍏堟敼鎴愬皻涓嶅瓨鍦ㄧ殑璺緞銆傜湡姝ｆ敼鍚嶆椂鍚屾渚濊禆銆佹枃妗ｉ摼鎺ュ拰鏀瑰悕鍓嶅悗鏄犲皠
锛涘師濮嬪揩鐓ф竻鍗曚笉閲嶆柊鐢熸垚鏉ュ啋鍏呪€滄棤宸紓鈥濄€?
825: 
826: | 鍛藉悕灞傛 | 褰撳墠绀轰緥 | 鐩爣绾﹀畾鎴栧鐞?|
827: |---|---|---|
828: | 浜у搧灞曠ず | 鑺嬮亾绠＄悊绯荤粺銆佽妺閬撴簮鐮佸強鏃?Logo | 浼楀涔嬪鍝佺墝銆佸钩鍙板悕绉板拰鐢ㄦ埛鍘熷浘琛嶇敓璧勬簮锛涗笉鍒犻櫎
蹇呴』淇濈暀鐨勬潵婧愯鏄?|
829: | Maven 妯″潡/浜х墿 | yudao銆亂udao-server銆亂udao-module-system | zszj銆亃szj-serve
r銆亃szj-module-system锛涚埗瀛?POM銆佷緷璧栧拰鍚姩璺緞涓€璧锋洿鏂?|
830: | Java 鍛藉悕绌洪棿 | cn.iocoder.yudao銆乊udao* | 鍐荤粨鍚庣殑 zszj 浜у搧鍖呮牴銆乑szj*锛涘悓鏃舵洿鏂版
壂鎻忋€佸弽灏勩€乆ML 鍜岃嚜鍔ㄩ厤缃敞鍐?|
831: | 浜у搧閰嶇疆/鏈嶅姟鍚?| yudao.*銆亂udao-server | zszj.*銆亃szj-server锛涢厤缃埆鍚嶅強瀛橀噺寮曠敤鎸夊
吋瀹规竻鍗曡縼绉?|
832: | 鍓嶇鍖呭拰妯℃澘杈撳嚭 | yudao-ui-admin-vue3銆乽nibest 浜у搧鍖呭悕 | zszj-admin-web銆亃szj-
miniapp锛涗繚鐣欐潵婧愬厓鏁版嵁锛岀敓鎴愪唬鐮佷笉寰楅噸鏂板甫鍥炴棫鍓嶇紑 |
833: 
834: ### ZS-BRAND-001锛氬浐鍖栧搧鐗岀礌鏉愩€佸懡鍚嶆槧灏勪笌鏀瑰悕杈圭晫
835: 
836: - 鍏宠仈锛欶ND-BRAND-001/002/003銆丗ND-SRC-004銆丗ND-DOC-001锛沇P-01/02/18锛汢01銆備紭鍏堢骇 
P0锛涚被鍒?琛ュ缓锛涚姸鎬?寰呴獙鏀讹紱鍓嶇疆 B00銆?
837: - 浼楀瑕佹眰涓庣幇鐘讹細鐢ㄦ埛鍘熷浘宸茬暀妗ｏ紝浣嗗皻鏃犲绔搧鐗岃祫婧愬寘鍜屽畬鏁存敼鍚嶆槧灏勶紱[鏍?POM][E01] 浣跨敤 cn.i
ocoder.boot/yudao锛孾Web 鍖呴厤缃甝(../apps/zhongshu-admin-web/package.json) 涓?[绉诲姩绔寘
閰嶇疆](../apps/zhongshu-miniapp/package.json) 浠嶅惈鍚勮嚜涓婃父妯℃澘鍚嶃€?
838: - 璋冩暣锛氶€愰」褰㈡垚鏃у悕鈫掓柊鍚嶁啋鏂囦欢/鏁版嵁娑堣垂鑰呪啋鎵规鈫掑吋瀹?鍥為€€鏂瑰紡锛涚粺涓€ zszj 涓诲墠缂€鍜屽厑璁哥缉鍐欙
紝鍐荤粨瀹屾暣鍖呮牴/GroupId锛屽尯鍒嗗搧鐗屽睍绀哄悕涓庡緟鎻愪緵鐨勫伐鍟嗗叏绉般€傚綊妗ｅ師鍥剧増鏈€佸昂瀵稿強鏍￠獙鍊硷紝鍒楀嚭妯増/鍥炬爣/娴
呮繁鑳屾櫙/楂樺垎杈ㄧ巼绱犳潗闇€姹傦紱浼樺厛绱㈠彇鐜版湁鐭㈤噺鎴栭€忔槑鍘熺锛屼笉鎿呰嚜閲嶇粯鍟嗘爣銆傛瘡涓繚鐣欐棫鍚嶇殑渚嬪闇€璁板綍璺緞銆佸師鍥
狅紝鍏煎鍒悕鍙︽湁鎾ら櫎鏉′欢銆?
839: - 楠屾敹锛氭墍鏈夋嫢鏈夌殑婧愮爜銆佺敓鎴愬櫒銆佽祫婧愬拰杩愯鏍囪瘑閮芥湁澶勭疆鍒嗙被锛涙棤涓や釜鐩爣閲嶅悕鎴栧墠缂€娣风敤锛涘師鍥炬牎楠屼竴鑷
淬€佸繀瑕佺礌鏉愭潵婧愬彲杩借釜锛涙湭鑾峰噯鏀瑰姩鐨勫閮ㄦ爣璇嗗拰鍘嗗彶璇佹嵁鏄庣‘鎺掗櫎锛屼笉鑳芥妸鈥滄棤鏃у瓧绗︿覆鈥濅綔涓哄垹闄ょ増鏉冪殑鐞嗙敱銆?
840: - 寮€鍙戣褰曪紙2026-09-08锛夛細鍐荤粨鏄犲皠鍙戝竷浜?[鍝佺墝绱犳潗涓庡懡鍚嶆槧灏刔(06-鍝佺墝绱犳潗涓庡懡鍚嶆槧灏?md)锛屾彁浜
?887bbc4f銆傚疄娴嬪師鍥句负 1060脳228 RGBA 閫忔槑 PNG锛孲HA-256 涓庢湰娓呭崟涓€鑷达紱鍐荤粨鍖呮牴 cn.zszj銆丟rou
pId cn.zszj銆侀厤缃墠缂€ zszj.*銆佺幆澧冨彉閲?ZSZJ_*锛涚櫥璁板繀瑕佷繚鐣欙紙渚涗綋/璇佹嵁/缃插悕锛夈€佺鐢ㄨ祫浜э紙zszj-
ui 鍐呴儴锛変笌闄愭湡鍏煎锛坹d-* 鍓嶇紑锛変緥澶栵紱Git 杩滅▼銆亃hongshu-* 椤跺眰鐩綍銆丄ppID/鍩熷悕绛夊閮ㄦ爣璇嗗垪鍏ユ帓闄ゆ
竻鍗曘€傚緟楠屾敹璇存槑锛氬伐鍟嗗叏绉颁笌鐭㈤噺鍘熺寰呯敤鎴锋彁渚涳紱鍝佺墝绱犳潗鏈€缁堥噰绾崇敱鐢ㄦ埛纭銆?
841: 
842: ### ZS-BRAND-002锛氱粺涓€鍚庣妯″潡銆佸寘绫汇€侀厤缃笌鏋勫缓浜х墿鍚嶇О
843: 
844: - 鍏宠仈锛欶ND-BRAND-002/003銆丗ND-ARCH-001/002銆丗ND-SRC-004锛沇P-02/18/19锛汢01銆備紭鍏堢骇
 P0锛涚被鍒?鏀归€狅紱鐘舵€?寰呴獙鏀讹紱鍓嶇疆 ZS-BRAND-001銆乑S-ENG-001銆乑S-ENG-002銆乑S-ENG-003銆?
845: - 浼楀瑕佹眰涓庣幇鐘讹細[鏍?POM][E01]銆乕Server POM][E02]銆乕涓婚厤缃甝[E05] 鍜?[Dockerfile][E0
3] 浣跨敤 yudao 妯″潡/鏈嶅姟/鍖呭墠缂€锛涘悕绉颁笉浠呭瓨鍦ㄤ簬 Java import锛岃繕褰卞搷鍒跺搧銆侀厤缃粦瀹氬強鎵弿瑁呴厤銆?
846: - 璋冩暣锛氭寜鏄犲皠缁熶竴浜у搧妯″潡鐩綍銆丮aven parent/GroupId/artifactId/BOM銆丣ava package/i
mport 涓?Yudao* 绫诲悕銆佸惎鍔ㄧ被/JAR銆丼pring 搴旂敤鍚嶅拰浜у搧閰嶇疆鍓嶇紑锛屽苟鏍稿鎺ュ彛鏂囨。鏍囬銆佸惎鍔?Banner 鐨
勪骇鍝佸睍绀哄悕銆傝仈鏀圭粍浠?Mapper 鎵弿銆丆ontroller 鍖呰矾寰勪笌鎺ュ彛鍓嶇紑鎺ㄥ銆乆ML namespace/resultType銆
丮ETA-INF 鑷姩閰嶇疆/SPI銆佸弽灏勫瓧绗︿覆銆佹祴璇曘€佽剼鏈?闀滃儚寮曠敤锛涙櫘閫氶鍩熷悕涓庣涓夋柟绫诲簱涓嶄贡鏀广€傞厤缃柊鏃ч敭涓嶅緱
浜х敓涓や釜涓嶅悓鏈夋晥鍊硷紝鍐茬獊搴旀槑纭け璐ワ紱鍘嗗彶杩愯鏍囪瘑浜ょ敱 ZS-BRAND-004 鍒嗘壒杩佺Щ銆?
847: - 楠屾敹锛氬共鍑€鏋勫缓銆佷緷璧栨爲銆佹墦鍖呭惎鍔ㄧ被涓庤嚜鍔ㄩ厤缃敞鍐屾纭紱闈欐€?瑁呴厤娴嬭瘯鑳藉彂鐜版棫鍖呮紡鏀瑰拰鏂版棫绫婚噸澶嶆敞
鍐岋紱鏈惎鐢ㄦā鍧椾粛鍏抽棴锛屼繚鐣欐簮鐮佸彲鎸夋壒鍑嗚寖鍥村崟鐙紪璇戯紱鐪熷疄 PG 鍚姩鍙婂畬鏁村畨鍏ㄩ摼鍦?B02/B03 澶嶉獙锛屼笉浠?B01
 鏋勫缓浠ｆ浛杩愯鎴愬姛銆?
848: - 寮€鍙戣褰曪紙2026-09-08锛夛細鎻愪氦 43d71fad锛?0,059 鏂囦欢銆傜洰褰?鍖?绫诲悕浠?git mv 淇濈暀鍘嗗彶锛?2
 涓《灞傛ā鍧椼€乫ramework 15 涓瓙妯″潡銆?7 涓?Java 婧愮爜鏍广€?9 涓?Yudao*鈫抁szj* 绫伙級锛涢厤缃敭銆丼prin
g 搴旂敤鍚嶃€佺幆澧冨彉閲忥紙ZS_*鈫抁SZJ_*锛夈€丏ockerfile/閮ㄧ讲鑴氭湰銆佹帴鍙ｆ枃妗ｆ爣棰樺悓姝ャ€傞潤鎬佷竴鑷存€ф鏌?`node
 scripts/brand/verify-backend-naming.mjs` 閫氳繃锛?,763 涓?Java 鍖呭０鏄庝笌璺緞涓€鑷达紝import
s/spring.factories/Mapper XML 鐨?cn.zszj FQCN 鍏ㄩ儴鍙В鏋愶紝POM 鍧愭爣涓€鑷达紝纭€ч棶棰?0銆傝瘎瀹′
慨姝ｉ殢 581927d0/b2c26ea9 钀藉湴锛氭仮澶嶈璇敼鐨勪笂娓哥讲鍚嶉摼鎺ワ紙github.com/YunaiV/*銆乬itee.com/zh
ijiantianya/*锛夊苟琛ヤ繚鎶よ鍒欍€傛湭楠岃瘉椤癸紙濡傚疄鐧昏锛夛細骞插噣鏋勫缓銆佷緷璧栨爲涓庣湡瀹?PG 鍚姩寰?ZS-ENG-002 宸
ュ叿閾撅紙鏈満鏃?JDK/Maven锛夊湪 B01/B02 琛ヨ瘉銆?
849: 
850: ### ZS-BRAND-003锛氭浛鎹?Web 涓庣Щ鍔ㄧ鍝佺墝灞曠ず鍜屼骇鍝佸寘鍚?
851: 
852: - 鍏宠仈锛欶ND-BRAND-001/002銆丗ND-WEB-001銆丗ND-MINI-001銆丗ND-SRC-004锛沇P-09/10/18/2
4锛汢01/B06锛堟寜瀛愰」鐙珛鏀捐锛夈€備紭鍏堢骇 P0锛涚被鍒?鏀归€狅紱鐘舵€?寮€鍙戜腑锛涘墠缃?鎸夌 16.1 鑺傚悇瀛愰」銆?
853: - 浼楀瑕佹眰涓庣幇鐘讹細[Web 鐜妯℃澘](../apps/zhongshu-admin-web/.env) 榛樿鏍囬涓鸿妺閬撶鐞嗙
郴缁燂紝[Logo 缁勪欢](../apps/zhongshu-admin-web/src/layout/components/Logo/src/Logo.v
ue)銆乕Web Logo](../apps/zhongshu-admin-web/src/assets/imgs/logo.png)銆乕favicon](.
./apps/zhongshu-admin-web/public/favicon.ico)銆乕绉诲姩 Logo](../apps/zhongshu-minia
pp/src/static/logo.png) 鍜?[绉诲姩 manifest](../apps/zhongshu-miniapp/manifest.conf
ig.ts) 灏氭湭鎸夌敤鎴峰搧鐗岄獙鏀躲€?
854: - 璋冩暣锛氱粺涓€鐧诲綍椤点€佹祻瑙堝櫒鏍囬銆佸睍寮€/鎶樺彔瀵艰埅銆佸姞杞?绌烘€併€佸钩鍙板唴鍏充簬椤点€侀€傜敤姘村嵃鍜屽璇█灞曠ず锛
涙浛鎹㈢浉搴?Logo/favicon 涓庣Щ鍔ㄧ搴旂敤灞曠ず璧勬簮锛屼繚鐣欏彲璁块棶鏂囨湰涓斾笉鎷変几/瑁佹帀鍥句腑鏂囧瓧銆傛洿鏂颁骇鍝?package 
name/鎻忚堪銆侀厤缃叆鍙ｄ笌鏋勫缓寮曠敤锛屾棫婕旂ず閾炬帴鍜屼簩缁寸爜鎸変骇鍝佽寖鍥存竻鐞嗭紱娉ㄥ唽鏍囪瘑/绛惧悕/涓婃灦涓嶈窡闅忓睍绀哄悕鑷姩淇敼
銆傚巻鍙蹭綔鑰?璁稿彲璇佷笌鏉ユ簮鍏冩暟鎹户缁繚鐣欙紱鍝佺墝缁熶竴鍏ュ彛渚涘悗缁笟鍔￠〉闈㈠鐢ㄣ€?
855: - 楠屾敹锛氬熀纭€涓ょ lint/typecheck/build 閫氳繃锛汢06 鍦ㄧ湡瀹?API 鐜楠岀櫥褰曘€佸鑸€佸埛鏂般€侀€€
鍑哄強鍝佺墝鍏ュ彛锛屾闈?绐勫睆銆佹祬娣辫儗鏅拰鎶樺彔鎬佹棤鏃у搧鐗屽洖閫€銆侀敊鍥俱€佸彉褰€佹柇閾炬垨涓嶅彲璇绘枃瀛楋紱鐪熸満/涓婃灦浠嶇敱鏃㈡湁 
B11 浠诲姟鐙珛鏀捐銆?
885: ### ZS-GOV-001锛氭妸妯″潡浠诲姟杩借釜绾冲叆鏂囨。涓€鑷存€ф鏌?
886: 
887: - 鍏宠仈锛欶ND-DOC-001/002/003锛沇P-20锛汢00锝濨11 妯垏銆備紭鍏堢骇 P0锛涚被鍒?琛ュ缓锛涚姸鎬?寰呴獙鏀讹紱鍓嶇疆
 鏈竻鍗曟牸寮忓缓绔嬨€?
888: - 浼楀瑕佹眰涓庣幇鐘讹細宸叉湁 [鏉ユ簮澶嶅埗鏍￠獙鑴氭湰][E28]锛屼絾瀹冧笉鏄渶姹?浠诲姟涓€鑷存€ф鏌ュ櫒锛涘綋鍓嶆枃妗ｅ悓姝ラ棬绂
佸皻鏈疄鐜般€傛湰娆″彧寤虹珛浠诲姟鏂囨。锛屼笉鍐掑厖瀹屾垚鑷姩妫€鏌ャ€?
889: - 璋冩暣锛氬湪鍚屼竴鏂囨。妫€鏌ュ叆鍙ｆ牎楠岀増鏈€侀摼鎺ャ€侀渶姹?浠诲姟缂栧彿鍞竴鎬с€丗ND/WP/B 寮曠敤銆佸喅绛栭棬绂併€佷换鍔＄
姸鎬佸強鍒嗗眰璇佹嵁锛涙柊澧炴湰鏂囧瓧娈垫鏌ャ€傝褰曞巻鍙叉瀯寤虹幆澧冧笌褰撳墠鐜鐨勫尯鍒紝鍚庣画鐜澶辫触涓嶈兘鐢ㄥ巻鍙叉垚鍔熻鐩栵紝涔熶笉绡
℃敼鍘嗗彶鎶ュ憡銆?
890: - 楠屾敹锛氭湰鍦板拰 CI 鍚屼竴瑙勫垯锛涙晠鎰忕牬鍧忛摼鎺ャ€佺紪鍙枫€佺増鏈垨缂哄け宸查獙鏀惰瘉鎹細澶辫触锛涙甯告枃妗ｉ€氳繃锛涙湭纭
 D-09 涓嶈兘琚啓鎴愭棦瀹氬疄鐜般€傜湡瀹?CI 鍦ㄥ浐瀹氭彁浜ら€氳繃鍓嶏紝涓嶇櫥璁?CI_VERIFIED銆?
891: - 寮€鍙戣褰曪紙2026-09-09锛夛細浜や粯 `scripts/gov/verify-docs.mjs` + 7 椤规祴璇曪紙verify-
docs.test.mjs锛夈€傝鍒欙細R1 浠撳簱鍐呴摼鎺ユ湁鏁堟€э紙浠撳簱澶?缁濆璺緞寮曠敤闄嶇骇涓烘彁绀猴級銆丷2 浠诲姟缂栧彿鍞竴銆丷3 鐘
舵€佹灇涓惧悎娉曘€丷4 鏈‘璁ゅ喅绛栵紙D-07/D-09/D-10/D-11锛変笉寰楁弿杩颁负鏃㈡垚浜嬪疄锛堝惁瀹氬彞寮忚眮鍏嶏級銆丷5 README 
绱㈠紩鐗堟湰涓庢枃妗ｅご涓€鑷淬€傛帴鍏ュ嵆鍙戠幇骞朵慨澶嶇湡瀹為棶棰橈細05 鏂囨。涓寚鍚戜緵浣撳揩鐓х殑 6 鏉￠摼鎺ヨ鍝佺墝鏀瑰悕鎵规璇敼锛堜緵
浣撲繚鎸佹棫璺緞锛屽凡鍥炰慨锛夈€乴ogo.svg 鏇挎崲鍚庣殑姝婚摼銆傞獙璇侊細褰撳墠鏂囨。 0 杩濊锛涙晠鎰忔敞鍏ユ柇閾?閲嶅缂栧彿/闈炴硶鐘舵€?
鏈‘璁ゅ喅绛栨棦鎴愪簨瀹炲寲/鐗堟湰涓嶄竴鑷翠簲绫荤牬鍧忓叏閮ㄥけ璐ャ€傚緟楠屾敹璇存槑锛氱湡瀹?CI 鎺ュ叆涓?CI_VERIFIED 鐧昏褰?ZS-O
PS-001锛堝浐瀹氭彁浜ら€氳繃鍓嶄笉鐧昏锛夈€?
892: 
893: ## 16. 寤鸿鎺ㄨ繘椤哄簭涓庡緟鍐宠竟鐣?
894: 
895: 1. 鍏堣ˉ娴嬭瘯鍏ュ彛/澶瑰叿锛屽啀杩涜瀵瑰簲鏀归€狅紱浼樺厛 ZS-ENG-001锝?06銆乑S-DB-001/002/019 鐨勭幆澧冧笌娴
嬭瘯鍑嗗銆?
896: 2. 瀹屾垚 ZS-DB-003/004 鐨勫敮涓€杩佺Щ鍩虹嚎锛岄殢鍚庢帹杩?005锝?08銆?11锝?18銆?20锛涜皟搴?浠ｇ爜鐢熸垚鍚敤鍓
嶅畬鎴愬悇鑷獙鏀躲€?
897: 3. ZS-DB-009 鎻愪氦 D-09 鍐崇瓥杈撳叆锛?10 鍦ㄦ壒鍑嗗悗瀹炵幇锛屼笉闃诲鍏朵粬涓嶄緷璧栬处鍙?缁勭粐璇箟鐨勬妧鏈伐浣溿€
?
898: 4. ZS-GOV-001 妯垏瀹炴柦锛涙瘡涓ā鍧楀垎鏋愮粨鏉熸椂鍏堟洿鏂版湰鏂囷紝鍐嶇粰鍑哄鐢ㄦ埛鐨勫樊璺濆拰浠诲姟鎽樿銆?
899: 5. B03 鑱斿悎 M03/M04锛氬厛寤虹珛 ZS-SEC-012 澶瑰叿锛屼紭鍏堝鐞嗚法绉熸埛鏉冮檺璺宠繃銆佸畨鍏ㄩ粯璁ゅ€笺€乀oken 
鐢ㄩ€?鎾ら攢/鍒锋柊銆佹棩蹇椾笌 API 鍚堝悓锛涘鎴风璇锋眰鏀归€?ZS-CLIENT-003 闅忓悎鍚岃仈鍔ㄣ€?
900: 6. B04 瀹屾垚绉佹湁鏂囦欢銆佷氦浠樼エ鎹笌涓氬姟瀹¤锛汢05 瀹屾垚 Outbox銆佹秷璐瑰箓绛夈€佷换鍔℃仮澶嶄笌绔欏唴寰呭姙锛汢06 
瀹屾垚涓ょ瀵艰埅/闄勪欢/鐧诲綍涓庢瀯寤洪棴鐜€備换鍔″彲浠ョ粏鍒嗕氦鍙夊噯澶囷紝浣嗘壒娆℃斁琛岄伒瀹?03 鏂囨。锛屼笉鎶婃湰娈靛綋鎴愭柊鐨勪緷璧栧浘銆
?
901: 7. D-09 鎵瑰噯鍚庡畬鎴?B07 缁勭粐浠昏亴涓?B08 瀵硅薄/鍔ㄤ綔/瀛楁鎺堟潈锛汥-07 鎵瑰噯鍚庤繘鍏?B09 棣栭摼/Flowab
le锛孊10 鍋氱湡瀹炵郴缁熸帴鍏ヤ笌瑙掕壊 UAT銆侭11 鐨勫井淇?鏀粯銆丄I銆丄pp 鍙戝竷鍜屽閲忔寜鍚勮嚜闂ㄧ鍚敤銆?
902: 8. M01锝濵12 鍒嗘瀽鐜板凡瀹屾垚锛屼笅涓€闃舵搴旇浆涓烘寜鎵规瀹炵幇涓庨獙鏀讹紝涓嶅啀缁х画鏋氫妇涓婃父鏃犲叧涓氬姟妯″潡锛涙湰杞笉鎵
ц杩欎簺淇銆?
903: 9. 鍝佺墝涓庡懡鍚嶄笓椤逛粠 B01 寮€濮嬶細鍏?ZS-BRAND-001 鍐荤粨鏄犲皠锛屽啀鍋?002銆?03.A 鍜?006.A锛?04 鎸
?B02/B03/B05 楠屽吋瀹癸紝005 鍦?B02 楠岀敓鎴愬櫒锛?03.B/006.B 鍦?B06 鏀跺彛銆傛棦鏈夋壒娆℃斁琛屽悓鏃惰鐩栬繖浜涙柊澧
炰换鍔★紝涓嶆妸鏀瑰悕鎷栧埌涓氬姟绯荤粺鍏ㄩ儴寤烘垚鍚庛€?
952: | ZS-CLIENT-005.A | B01 | ZS-ENG-002銆乑S-ENG-003 | Node/pnpm/閿佹枃浠躲€乄eb 绫诲瀷淇
銆丠5/灏忕▼搴忓熀纭€鏋勫缓 | 骞插噣瀹夎鍙婂師鐢?lint/typecheck/build 閫氳繃锛涗笉绛夊緟椤甸潰/鎺ュ彛鏀归€?|
953: | ZS-CLIENT-005.B | B06 | ZS-CLIENT-005.A銆乑S-CLIENT-001.A銆乑S-CLIENT-002.A銆
乑S-CLIENT-003銆乑S-CLIENT-004銆乑S-MSG-003.B | 涓ょ鐧诲綍銆佸鑸€佹枃浠躲€佸緟鍔?E2E 鍥炲綊 | 鐪熷疄鎺ュ
彛/PG 鐜姝ｅ弽鍚戦€氳繃锛涙瀯寤哄拰鎺ュ彛鏇胯韩涓嶄唬鏇胯仈璋?|
954: | ZS-CLIENT-005.C | B11 | ZS-CLIENT-005.B | 鎵瑰噯缁堢鐨勭湡鏈恒€佽澶囪兘鍔涘強鍙戝竷楠屾敹 | 寮
辩綉/鎷掔粷鏉冮檺/鎭㈠鏈夎瘉鎹紱App 鍙戝竷鍙﹂渶鑼冨洿銆佺鍚嶏紱涓嶇瓑寰?AI 鍐崇瓥 |
955: | ZS-OPS-001.A | B01 | ZS-GOV-001銆乑S-CLIENT-005.A | 鍩虹鏋勫缓/鏂囨。娴佹按绾块鏋?| 鏈
湴涓?CI 鍚岃鍒欙紝鍧忛摼鎺?鏋勫缓澶辫触闃绘鏀捐锛涗笉绛夊緟 PG/澶氱 E2E |
956: | ZS-OPS-001.B | B02 | ZS-OPS-001.A銆乑S-DB-019.B | PG 灞傛祦姘寸嚎鎺ュ叆 | 鍥哄畾鐜鍜屾彁
浜わ紝缂?PG/杩佺Щ澶辫触涓嶅彲缁跨伅 |
957: | ZS-OPS-001.C | B03 | ZS-OPS-001.B銆乑S-SEC-012.B銆乑S-SYS-001.A | 瀹夊叏/鍩虹绠＄悊
 API 灞傞棬绂?| 鐪熷疄杩囨护鍣ㄥ紑鍚紝瓒婃潈/鍩虹鍔熻兘澶辫触闃绘 B03 |
958: | ZS-OPS-001.D | B06 | ZS-OPS-001.C銆乑S-CLIENT-005.B銆乑S-SYS-001.B | Web 涓庡皬
绋嬪簭鎶€鏈仈璋冮棬绂?| 鐣岄潰鎿嶄綔鍜屾帴鍙ｄ竴鑷达紱璺宠繃蹇呮祴鍦烘櫙涓嶅緱鏀捐 |
959: | ZS-OPS-001.E | B09 | ZS-OPS-001.D銆乑S-BPM-001銆乑S-BPM-002銆乑S-BPM-003 | 鑾锋壒
棣栭摼/娴佺▼鍙婄粍缁囨潈闄愰棬绂?| PG 娴佺▼銆佸箓绛夊啓鍥炲拰璺ㄧ粍缁囧弽鍚戦€氳繃锛汥-07/D-09 涓哄墠缃?|
960: | ZS-OPS-002.A | B01 | ZS-ENG-002銆乑S-ENG-003銆乑S-ENG-005 | 閮ㄧ讲妯℃澘銆佹帰閽?TLS/鍙
嶄唬鍜岄厤缃悎鍚?| 妯℃澘鍙鏌ャ€佹晱鎰熺鐞嗚矾寰勫叧闂紱鐪熷疄涓氬姟鍚姩闅?B02/B03锛屼笉绛夊緟鎭㈠婕旂粌 |
961: | ZS-OPS-002.B | B05 | ZS-OPS-002.A銆乑S-DB-005銆乑S-JOB-004 | 宸插惎鐢ㄤ緷璧?闃熷垪鍛婅涓
庤繍琛岃鏄?| 渚濊禆鏁呴殰鍜岀Н鍘嬭鍙戠幇銆佸け璐ヤ笉闈欓粯锛涗笉绛夊緟鍚庣疆澶栭儴鎺ュ叆 |
962: | ZS-OPS-002.C | B11 | ZS-OPS-002.B銆乑S-FILE-005.B | 鐙珛鐜瀹屾暣鎭㈠涓庤幏鎵瑰閲忔祴璇
?| DB/闄勪欢/浜嬩欢涓€鑷达紱鎭㈠鐜涓嶅悜鐪熷疄娓犻亾琛ュ彂锛涘洖濉疄娴?RPO/RTO |
963: | ZS-SYS-001.A | B03 | ZS-DB-019.A銆乑S-SEC-012.A銆乑S-SEC-008銆乑S-PERM-001.A銆乑
S-PERM-004.A銆乑S-CFG-001.B銆乑S-CFG-002.B銆乑S-CFG-003.B銆乑S-CFG-004銆乑S-IAM-003 | 涓冪被
鍩虹绠＄悊鐪熷疄 PG/API 鍥炲綊 | 瑙?15.1 鐭╅樀鍏ㄩ儴姝ｅ弽鍚戠敤渚嬶紝涓嶇瓑寰?Web E2E |
964: | ZS-SYS-001.B | B06 | ZS-SYS-001.A銆乑S-CLIENT-001.A | 涓冪被 Web 鍩虹绠＄悊鎿嶄綔鍙婂彇
鏉冭仈楠?| 鍚岀煩闃甸€氳繃椤甸潰鎿嶄綔澶嶉獙锛涙暟鎹寔涔呭寲鍜屽悗绔嫆缁濆彲楠岃瘉 |
965: | ZS-BRAND-003.A | B01 | ZS-BRAND-001銆乑S-CLIENT-005.A | 鍘熷浘琛嶇敓璧勬簮銆侀潤鎬佸搧鐗屽拰
涓ょ浜у搧鍖呭悕 | 鍩虹鏋勫缓閫氳繃锛涙棫鍙妯℃澘鏍囪瘑娓呯悊锛屾潵婧愪俊鎭繚鐣?|
966: | ZS-BRAND-003.B | B06 | ZS-BRAND-003.A銆乑S-CLIENT-001.A銆乑S-CLIENT-002.A銆乑S
-BRAND-004.B | 鐪熷疄鐧诲綍/瀵艰埅鍦烘櫙涓殑澶氱鍝佺墝鍙敤鎬?| 妗岄潰/绐勫睆/鎶樺彔/涓婚鍥炲綊锛屽浘鏂囨纭笖鏃犳棫鍝佺墝鍥為
€€ |
967: | ZS-BRAND-004.A | B02 | ZS-BRAND-001銆乑S-BRAND-002銆乑S-DB-003銆乑S-DB-004銆乑S-
DB-019.A | 閰嶇疆銆佺瀛愬拰纭渶鍙樻洿鐨勬寔涔呭寲寮曠敤杩佺Щ | 鏂拌/鍗囩骇/閲嶈窇/鎭㈠閫氳繃锛涙棤瀛橀噺闇€鏈夎瘉鎹?|
968: | ZS-BRAND-004.B | B03 | ZS-BRAND-004.A銆乑S-LOGIN-005.A銆乑S-CLIENT-003 | 缂撳瓨
銆佹祻瑙堝櫒瀛樺偍鍜屼細璇濆懡鍚嶅垏鎹?| 閫€鍑?鎾ゆ潈/骞跺彂鍒锋柊涓嶇粫杩囷紱鍏煎绐楀彛鍙婇噸鐧诲綍绛栫暐鏄庣‘ |
969: | ZS-BRAND-004.C | B05 | ZS-BRAND-004.B銆乑S-JOB-002銆乑S-JOB-003銆乑S-DB-016 | 
浠诲姟涓庢秷鎭寔涔呭寲寮曠敤鍜屾柊鏃у垏鎹?| 鎭㈠/閲嶅娑堣垂鏃犱涪澶变笌閲嶅鍓綔鐢紝鏃х被寮曠敤鍙楁帶杩佺Щ |
970: | ZS-BRAND-006.A | B01 | ZS-BRAND-002銆乑S-BRAND-003.A銆乑S-OPS-001.A | 鍝佺墝/浠ｇ
爜娈嬬暀娓呭崟鍙婂熀纭€ CI 妫€鏌?| 婕忔敼娉ㄥ叆澶辫触锛屾潵婧愪緥澶栦笉璇姤锛涗笉绛夊緟鍔熻兘鑱旈獙 |
971: | ZS-BRAND-006.B | B06 | ZS-BRAND-006.A銆乑S-BRAND-003.B銆乑S-BRAND-004.C銆乑S-B
RAND-005銆乑S-SYS-001.B銆乑S-CLIENT-005.B銆乑S-OPS-001.D | 鍝佺墝銆佹敼鍚嶅吋瀹逛笌浜у搧鍔熻兘鑱斿悎鏀跺彛 |
 鐪熷疄 PG/涓ょ/浠诲姟缁撴灉鍙婃敼鍚嶅悗鐨勬枃妗ｉ摼鎺ュ潎鏈夎瘉鎹?|

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '$Out
putEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new(); node --te
st scripts/verify-source-copy.test.mjs; git status --short; git diff ec92cb85''
^ ec92cb85 -- scripts/verify-source-copy.mjs scripts/verify-source-copy.test.mj
s; Get-Content -Encoding utf8 -LiteralPath scripts/verify-source-copy.test.mjs 
| Select-Object -First 65' in E:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇
 succeeded in 135ms:

鉁?failing tests:

test at scripts\verify-source-copy.test.mjs:37:1
鉁?a regular file may not silently become a symbolic link (0.551ms)
  Error: EPERM: operation not permitted, symlink 'sample.txt' -> 'C:\Users\ADMI
NI~1\AppData\Local\Temp\zhongshu-copy-test-c2KTSG\source\link.txt'
      at symlinkSync (node:fs:1938:11)
      at TestContext.<anonymous> (file:///E:/%E4%BC%97%E5%A2%85%E4%B9%8B%E5%AE%
B6AI%E8%B5%8B%E8%83%BD%E5%B9%B3%E5%8F%B0%E5%BA%95%E5%BA%A7/scripts/verify-sourc
e-copy.test.mjs:39:3)
      at Test.runInAsyncScope (node:async_hooks:227:14)
      at Test.run (node:internal/test_runner/test:1382:25)
      at Test.processPendingSubtests (node:internal/test_runner/test:960:18)
      at Test.postRun (node:internal/test_runner/test:1522:19)
      at Test.run (node:internal/test_runner/test:1447:12)
      at async Test.processPendingSubtests (node:internal/test_runner/test:960:
7) {
    errno: -4048,
    code: 'EPERM',
    syscall: 'symlink',
    path: 'sample.txt',
    dest: 'C:\\Users\\ADMINI~1\\AppData\\Local\\Temp\\zhongshu-copy-test-c2KTSG
\\source\\link.txt'
  }

test at scripts\verify-source-copy.test.mjs:43:1
鉁?a recorded symlink is checked without following its target (0.7012ms)
  Error: EPERM: operation not permitted, symlink 'sample.txt' -> 'C:\Users\ADMI
NI~1\AppData\Local\Temp\zhongshu-copy-test-xvp9Q9\source\link.txt'
      at symlinkSync (node:fs:1938:11)
      at TestContext.<anonymous> (file:///E:/%E4%BC%97%E5%A2%85%E4%B9%8B%E5%AE%
B6AI%E8%B5%8B%E8%83%BD%E5%B9%B3%E5%8F%B0%E5%BA%95%E5%BA%A7/scripts/verify-sourc
e-copy.test.mjs:45:3)
      at Test.runInAsyncScope (node:async_hooks:227:14)
      at Test.run (node:internal/test_runner/test:1382:25)
      at Test.processPendingSubtests (node:internal/test_runner/test:960:18)
      at Test.postRun (node:internal/test_runner/test:1522:19)
      at Test.run (node:internal/test_runner/test:1447:12)
      at async Test.processPendingSubtests (node:internal/test_runner/test:960:
7) {
    errno: -4048,
    code: 'EPERM',
    syscall: 'symlink',
    path: 'sample.txt',
    dest: 'C:\\Users\\ADMINI~1\\AppData\\Local\\Temp\\zhongshu-copy-test-xvp9Q9
\\source\\link.txt'
  }
warning: unable to access 'C:\Users\Administrator/.config/git/ignore': Permissi
on denied
warning: unable to access 'C:\Users\Administrator/.config/git/ignore': Permissi
on denied
?? docs/reviews/codex-hotfix-C-r1.raw.md
?? docs/reviews/codex-hotfix-C.raw.md
?? docs/reviews/codex-hotfix-D.raw.md
?? docs/reviews/codex-hotfix-E.raw.md
import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, writeFileSync, symlinkSync, chmodSync } from '
node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { gitBlobHash, verifyFiles, verifyManifest } from './verify-source-copy.
mjs';

function fixture() {
  const root = mkdtempSync(join(tmpdir(), 'zhongshu-copy-test-'));
  mkdirSync(join(root, 'source'));
  writeFileSync(join(root, 'source', 'sample.txt'), 'hello\n');
  return root;
}

const entry = { path: 'source/sample.txt', mode: '100644', importedBlob: 'ce013
625030ba8dba906f756967f9e9ca394464a' };

test('Git blob hash includes the Git header', () => {
  assert.equal(gitBlobHash(Buffer.from('hello\n')), entry.importedBlob);
});

test('an exact imported file passes', () => {
  assert.deepEqual(verifyFiles(fixture(), [entry]), []);
});

test('content changes and missing files are reported', () => {
  const root = fixture();
  writeFileSync(join(root, 'source', 'sample.txt'), 'changed');
  assert.equal(verifyFiles(root, [entry])[0].reason, 'content-mismatch');
  assert.equal(verifyFiles(root, [{ ...entry, path: 'source/missing.txt' }])[0]
.reason, 'missing');
});

test('paths outside the project and duplicate entries are rejected', () => {
  assert.equal(verifyFiles(fixture(), [{ ...entry, path: '../outside' }])[0].re
ason, 'unsafe-path');
  assert.equal(verifyFiles(fixture(), [entry, entry])[0].reason, 'duplicate-pat
h');
});

test('a regular file may not silently become a symbolic link', () => {
  const root = fixture();
  symlinkSync('sample.txt', join(root, 'source', 'link.txt'));
  assert.equal(verifyFiles(root, [{ ...entry, path: 'source/link.txt' }])[0].re
ason, 'file-type-mismatch');
});

test('a recorded symlink is checked without following its target', () => {
  const root = fixture();
  symlinkSync('sample.txt', join(root, 'source', 'link.txt'));
  assert.deepEqual(verifyFiles(root, [{ path: 'source/link.txt', mode: '120000'
, importedBlob: gitBlobHash(Buffer.from('sample.txt')) }]), []);
});

test('non-canonical paths and unknown file modes are rejected', () => {
  const root = fixture();
  assert.equal(verifyFiles(root, [{ ...entry, path: 'source/../source/sample.tx
t' }])[0].reason, 'unsafe-path');
  assert.equal(verifyFiles(root, [{ ...entry, mode: 'invalid' }])[0].reason, 'i
nvalid-entry');
});

// 鍙墽琛屼綅璇箟鍦?Windows 涓婁笉鍙敤锛坴erifyFiles 鍦?win32 璺宠繃 mode 鏍￠獙锛夛紝
// 璇ョ敤渚嬩粎鍦?POSIX锛堝惈 CI ubuntu锛夋墽琛岋紱symlink 閮ㄥ垎鍚屾牱闇€瑕?POSIX 璇箟
const isPosix = process.platform !== 'win32';
(isPosix ? test : test.skip)('executable mode and parent symlink changes are re
jected', () => {
  const root = fixture();
  chmodSync(join(root, entry.path), 0o755);
  assert.equal(verifyFiles(root, [entry])[0].reason, 'executable-mode-mismatch'
);
  symlinkSync('source', join(root, 'alias'));
  assert.equal(verifyFiles(root, [{ ...entry, path: 'alias/sample.txt' }])[0].r
eason, 'unsafe-parent-link');
});


exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "@'
import { execFileSync } from 'node:child_process';
import { pathToFileURL } from 'node:url';
import { readFileSync } from 'node:fs';
const scriptPath = 'scripts/brand/verify-backend-naming.mjs';
const scriptUrl = pathToFileURL("'`${process.cwd()}/${scriptPath}`).href;
const current = readFileSync(scriptPath, '"'utf8');
const parent = execFileSync('git', ['show', 'ec92cb85"'^:'"' + scriptPath], {en
coding:'utf8'});
for (const [revision, source] of [['parent', parent], ['commit', current]]) {
  const sourceWithFixedUrl = source.replaceAll('import.meta.url', JSON.stringif
y(scriptUrl));
  let output;
  let status = 0;
  try { output = execFileSync(process.execPath, ['--input-type=module', '-e', s
ourceWithFixedUrl], {encoding:'utf8'}); }
  catch (e) { output = e.stdout; status = e.status; }
  const report = JSON.parse(output);
  console.log(JSON.stringify({revision, status, javaFiles:report.javaFiles, har
dIssueCount:report.hardIssueCount, hardIssues:report.hardIssues}));
}
'@ | node --input-type=module" in E:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇
 succeeded in 5411ms:
{"revision":"parent","status":1,"javaFiles":6784,"hardIssueCount":8,"hardIssues
":[["package-path-mismatch","services/zhongshu-core/zszj-module-infra/target/ge
nerated-sources/annotations/cn/zszj/module/infra/convert/codegen/CodegenConvert
Impl.java: cn.zszj.module.infra.convert.codegen != zszj-module-infra/target/gen
erated-sources/annotations/cn/zszj/module/infra/convert/codegen"],["package-pat
h-mismatch","services/zhongshu-core/zszj-module-infra/target/generated-sources/
annotations/cn/zszj/module/infra/convert/config/ConfigConvertImpl.java: cn.zszj
.module.infra.convert.config != zszj-module-infra/target/generated-sources/anno
tations/cn/zszj/module/infra/convert/config"],["package-path-mismatch","service
s/zhongshu-core/zszj-module-infra/target/generated-sources/annotations/cn/zszj/
module/infra/convert/file/FileConfigConvertImpl.java: cn.zszj.module.infra.conv
ert.file != zszj-module-infra/target/generated-sources/annotations/cn/zszj/modu
le/infra/convert/file"],["package-path-mismatch","services/zhongshu-core/zszj-m
odule-infra/target/generated-sources/annotations/cn/zszj/module/infra/convert/r
edis/RedisConvertImpl.java: cn.zszj.module.infra.convert.redis != zszj-module-i
nfra/target/generated-sources/annotations/cn/zszj/module/infra/convert/redis"],
["package-path-mismatch","services/zhongshu-core/zszj-module-system/target/gene
rated-sources/annotations/cn/zszj/module/system/convert/auth/AuthConvertImpl.ja
va: cn.zszj.module.system.convert.auth != zszj-module-system/target/generated-s
ources/annotations/cn/zszj/module/system/convert/auth"],["package-path-mismatch
","services/zhongshu-core/zszj-module-system/target/generated-sources/annotatio
ns/cn/zszj/module/system/convert/oauth2/OAuth2OpenConvertImpl.java: cn.zszj.mod
ule.system.convert.oauth2 != zszj-module-system/target/generated-sources/annota
tions/cn/zszj/module/system/convert/oauth2"],["package-path-mismatch","services
/zhongshu-core/zszj-module-system/target/generated-sources/annotations/cn/zszj/
module/system/convert/tenant/TenantConvertImpl.java: cn.zszj.module.system.conv
ert.tenant != zszj-module-system/target/generated-sources/annotations/cn/zszj/m
odule/system/convert/tenant"],["package-path-mismatch","services/zhongshu-core/
zszj-module-system/target/generated-sources/annotations/cn/zszj/module/system/c
onvert/user/UserConvertImpl.java: cn.zszj.module.system.convert.user != zszj-mo
dule-system/target/generated-sources/annotations/cn/zszj/module/system/convert/
user"]]}
{"revision":"commit","status":0,"javaFiles":6776,"hardIssueCount":0,"hardIssues
":[]}

codex
No actionable regressions found. The verifier eliminates eight generated-source
 false positives without excluding tracked source files, and the revised execut
ion order matches the documented prerequisites. Nine of ten fast baseline gates
 passed; the remaining gate failed on unrelated Windows symlink permissions.
No actionable regressions found. The verifier eliminates eight generated-source false positives without excluding tracked source files, and the revised execution order matches the documented prerequisites. Nine of ten fast baseline gates passed; the remaining gate failed on unrelated Windows symlink permissions.
