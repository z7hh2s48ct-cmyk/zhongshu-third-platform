codex.cmd : OpenAI Codex v0.153.4
所在位置 行:1 字符: 146
+ ... ing]::UTF8; & "$env:APPDATA\npm\codex.cmd" review --commit c9d178b2 2 ...
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
session id: 01a08ad0-1ee2-7b92-955c-44b7232fd6c9
--------
user
commit c9d178b
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "Get-
Location; git status --short; git rev-parse --show-toplevel; git show --stat --
oneline --decorate --no-renames c9d178b2; Get-ChildItem -Force -Name; if (Test-
Path 'AGENTS.override.md') { Get-Content 'AGENTS.override.md' }; if (Test-Path 
'AGENTS.md') { Get-Content 'AGENTS.md' }" in E:\众墅之家AI赋能平台底座
 succeeded in 25ms:
c9d178b2 (HEAD -> main) ZS-OPS-001.A: 聚合门禁入口提速 P1 步骤④（run-local-gates 并发 + 增量）
 README.md                                          |  10 +-
 ...273\273\345\212\241\346\270\205\345\215\225.md" |   4 +-
 scripts/ops/run-local-gates.mjs                    | 211 +++++++++++++++------
 scripts/ops/run-local-gates.test.mjs               |  96 ++++++++++
 4 files changed, 256 insertions(+), 65 deletions(-)
Path           
----           
E:\众墅之家AI赋能平台底座
.git
.github
.omx
apps
docs
outputs
reference
scripts
services
third_party
tools
.gitignore
README.md
THIRD_PARTY_NOTICES.md



exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "git 
-c core.quotepath=false show --format=fuller --no-ext-diff --no-renames c9d178b
2; Get-ChildItem -Path . -Filter 'AGENTS*' -Recurse -Force -File -ErrorAction S
ilentlyContinue | Select-Object -ExpandProperty FullName; if (Test-Path 'E:\\AG
ENTS.override.md') { Get-Content 'E:\\AGENTS.override.md' }; if (Test-Path 'E:\
\AGENTS.md') { Get-Content 'E:\\AGENTS.md' }; if (Test-Path 'C:\\Users\\Adminis
trator\\.codex\\config.toml') { Get-Content 'C:\\Users\\Administrator\\.codex\\
config.toml' }" in E:\众墅之家AI赋能平台底座
 succeeded in 1956ms:
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
model = "gpt-6-astra"
model_reasoning_effort = "xhigh"
service_tier = "default"
notify = [ "C:\\Users\\Administrator\\AppData\\Local\\OpenAI\\Codex\\runtimes\\
cua_node\\b58ca2eaa616c2da\\bin\\node_modules\\@oai\\sky\\bin\\windows\\codex-c
omputer-use.exe", "turn-ended" ]
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
b58ca2eaa616c2da\bin\node_repl.exe'
startup_timeout_sec = 120

[mcp_servers.node_repl.env]
NODE_REPL_NATIVE_PIPE_CONNECT_TIMEOUT_MS = "1000"
NODE_REPL_NODE_MODULE_DIRS = 'C:\Users\Administrator\AppData\Local\OpenAI\Codex
\runtimes\cua_node\b58ca2eaa616c2da\bin\node_modules'
NODE_REPL_NODE_PATH = 'C:\Users\Administrator\AppData\Local\OpenAI\Codex\runtim
es\cua_node\b58ca2eaa616c2da\bin\node.exe'
NODE_REPL_TRUSTED_CODE_PATHS = 'C:\Users\Administrator\.codex;C:\Users\Administ
rator\AppData\Local\OpenAI\Codex\runtimes\cua_node\b58ca2eaa616c2da\bin\node_mo
dules'
CODEX_HOME = 'C:\Users\Administrator\.codex'
BROWSER_USE_AVAILABLE_BACKENDS = "chrome,iab"
BROWSER_USE_TINYSKY_ENABLED = "1"
NODE_REPL_INSTRUCTIONS_USE_CASE_BROWSER = ""
NODE_REPL_INSTRUCTIONS_USE_CASE_CHROME = ""
BROWSER_USE_CODEX_APP_BUILD_FLAVOR = "prod"
BROWSER_USE_CODEX_APP_VERSION = "26.903.61454"
NODE_REPL_TRUSTED_SERVICES = '{"browser":"C:/Users/Administrator/.codex/plugins
/cache/openai-bundled/browser/26.903.61454/scripts/browser-service.mjs","sky":"
@oai/sky/service"}'
SKY_CUA_NATIVE_PIPE = "1"
SKY_CUA_NATIVE_PIPE_DIRECTORY = '\\.\pipe\codex-computer-use-ea13de0b-73a1-461f
-ac2a-7e01379e9373'
CODEX_CLI_PATH = 'C:\Users\Administrator\AppData\Local\OpenAI\Codex\bin\fd4c151
a749f3ab4\codex.exe'

[windows]
sandbox = "elevated"

[projects.'c:\users\administrator\documents\codex\2026-09-08\new-chat']
trust_level = "trusted"

[projects.'c:\users\administrator\documents\codex\2026-09-08\bang-ow']
trust_level = "trusted"

[projects.'e:\浼楀涔嬪璁捐骞冲彴']
trust_level = "trusted"

[projects.'e:\浼楀涔嬪ai璧嬭兘骞冲彴搴曞骇']
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

[projects.'c:\users\administrator\documents\codex\2026-09-09\plugin-computer-us
e-openai-bundled-x20']
trust_level = "trusted"

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'Get-
Content scripts/ops/run-local-gates.mjs; Get-Content .github/workflows/local-ba
seline-gates.yml; Get-Content scripts/eng/verify-module-whitelist.mjs; Get-Cont
ent scripts/db/verify-datasource-pg.mjs; Get-Content scripts/db/verify-flyway-m
igrations.mjs; Get-Content scripts/client/verify-ts-baseline.mjs' in E:\众墅之家AI赋
能平台底座
 succeeded in 0ms:
/**
 * ZS-OPS-001.A 鏈湴鍩虹嚎闂ㄧ鑱氬悎鍏ュ彛锛堟湰鍦颁笌 CI 鍚屼竴瑙勫垯锛夈€? *
 * 骞跺彂鎵ц鍏ㄩ儴鏈湴妫€鏌ワ紝浠讳竴澶辫触鍗虫暣浣撳け璐ュ苟姹囨€伙紙缁撴灉鎸夐棬绂佸畾涔夐『搴忕ǔ瀹氳緭鍑猴級锛? *   G1 鏉ユ簮澶嶅埗鏍￠
獙鍣ㄥ崟娴?/ G2 鍝佺墝鍛藉悕闂ㄧ鍗曟祴 / G2b 鍛藉悕杩佺Щ宸ュ叿鍗曟祴 / G3 鍝佺墝鍛藉悕鍏ㄤ粨鎵弿
 *   G4 鏂囨。涓€鑷存€у崟娴?/ G5 鏂囨。涓€鑷存€у叏閲忥紙ZS-GOV-001锛? G6 妯″潡鐧藉悕鍗曪紙ZS-ENG-001锛? *  
 G7 鏁版嵁婧?PG 鍚堝悓锛圸S-DB-001.A锛? G8 Flyway 杩佺Щ瑙勮寖锛圸S-DB-003锛? G9 閰嶇疆绉樺瘑闂ㄧ锛圸S-CFG-
001.A锛? *   G10 Web 绫诲瀷妫€鏌ュ熀绾匡紙ZS-CLIENT-005.A锛岃緝鎱紱--fast 璺宠繃锛? *   G11 鍚敤妯″潡
鍚庣鍗曟祴锛?-mvn 鏄惧紡鍚敤锛涢渶 tools/env.sh 宸ュ叿閾撅紝鎺掗櫎宸茬櫥璁扮殑涓婃父鍩虹嚎澶辫触锛? * PG/澶氱 E2E 闂ㄧ
鎸?ZS-OPS-001.B~.E 鎵规鎺ュ叆锛屼笉鍦ㄦ湰楠ㄦ灦銆? *
 * 鎻愰€燂紙ZS-GOV-001 鎻愭晥鏂规 P1锛夛細
 *   - 骞跺彂锛氶粯璁ゆ寜 CPU 鏍告暟骞跺彂璺戦棬绂侊紙--jobs N 瑕嗙洊锛夛紝鍙嶉鏃堕棿浠庛€屽悇闂ㄧ鑰楁椂涔嬪拰銆嶉檷鍒般€屾渶鎱㈤棬
绂併€嶃€? *   - 澧為噺锛?-incremental 鎸?git 鍙樻洿璺緞鍙窇鍙楀奖鍝嶉棬绂侊紙鍝佺墝鍏ㄤ粨鎵弿 G3銆佺瀵嗛棬绂?G9 鎭
掑畾璺戯紱
 *     鍙樻洿鍚棬绂佽剼鏈嚜韬?scripts/ops/銆佹垨瀛樺湪鏃犳硶褰掔被涓旈潪鑹€х殑璺緞鏃?fail-safe 鍥為€€鍏ㄩ噺锛夈€?
 *     浠呬緵鏃ュ父蹇€熷弽棣堬紱鎵规鏀跺彛涓?CI 蹇呴』璺戝叏閲忥紙--fast 鎴栧惈 G10/G11锛夛紝涓嶅緱浠ュ閲忕粨鏋滀唬鏇挎斁琛屻
€? *   - --fast 鍥哄寲锛氭棩甯稿紑鍙戦粯璁?--fast锛堣烦杩囪緝鎱㈢殑 G10 vue-tsc锛夛紝鎵规鏀跺彛鍘绘帀 --fast 璺戝
叏閲忋€? * 鐢ㄦ硶锛歯ode scripts/ops/run-local-gates.mjs [--fast] [--mvn] [--incrementa
l] [--jobs N] [--plan]
 */
import { execFile, spawn, spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { cpus } from 'node:os';

const root = fileURLToPath(new URL('../../', import.meta.url));

// 闂ㄧ瀹氫箟銆俛reas锛氬閲忔ā寮忎笅鍛戒腑杩欎簺璺緞鍓嶇紑鎵嶈窇锛泂afety锛氬閲忔ā寮忔亽瀹氳窇锛堝叏浠撳悎瑙?瀹夊叏瀹堝崼锛夛紱
// slow锛?-fast 璺宠繃锛沵vn锛氫粎 --mvn 鍚敤銆俢md/mvnArgs 涓?id 淇濇寔涓庢棦鏈夊熀绾夸竴鑷达紙CI銆丷EADME 渚
濊禆锛夈€?export const GATES = [
  { id: 'G1 鏉ユ簮澶嶅埗鏍￠獙鍣ㄥ崟娴?, cmd: ['node', '--test', 'scripts/verify-source-copy
.test.mjs'], areas: ['scripts/verify-source-copy', 'third_party/'] },
  { id: 'G2 鍝佺墝鍛藉悕闂ㄧ鍗曟祴', cmd: ['node', '--test', 'scripts/brand/verify-brand-
naming.test.mjs'], areas: ['scripts/brand/'] },
  { id: 'G2b 鍛藉悕杩佺Щ宸ュ叿鍗曟祴锛堝喕缁撴枃浠舵帓闄?+ 绉熸埛鍩熷悕鍞竴锛?, cmd: ['node', '--test', 'scr
ipts/brand/apply-naming-migration.test.mjs'], areas: ['scripts/brand/'] },
  { id: 'G3 鍝佺墝鍛藉悕鍏ㄤ粨鎵弿', cmd: ['node', 'scripts/brand/verify-brand-naming.mjs
'], areas: ['services/', 'apps/', 'scripts/', 'docs/'], safety: true },
  { id: 'G4 鏂囨。涓€鑷存€у崟娴?, cmd: ['node', '--test', 'scripts/gov/verify-docs.test
.mjs'], areas: ['scripts/gov/', 'docs/', 'README.md'] },
  { id: 'G5 鏂囨。涓€鑷存€у叏閲?, cmd: ['node', 'scripts/gov/verify-docs.mjs'], areas: 
['scripts/gov/', 'docs/', 'README.md'] },
  { id: 'G6 妯″潡鐧藉悕鍗?, cmd: ['node', 'scripts/eng/verify-module-whitelist.mjs'],
 areas: ['scripts/eng/', 'services/'] },
  { id: 'G7 鏁版嵁婧?PG 鍚堝悓', cmd: ['node', 'scripts/db/verify-datasource-pg.mjs'],
 areas: ['scripts/db/', 'services/'] },
  { id: 'G8 Flyway 杩佺Щ瑙勮寖', cmd: ['node', 'scripts/db/verify-flyway-migrations.
mjs'], areas: ['scripts/db/', 'services/'] },
  { id: 'G9 閰嶇疆绉樺瘑闂ㄧ', cmd: ['node', 'scripts/cfg/verify-config-secrets.mjs'],
 areas: ['scripts/cfg/', 'services/', 'apps/'], safety: true },
  { id: 'G10 Web 绫诲瀷妫€鏌ュ熀绾?, cmd: ['node', 'scripts/client/verify-ts-baseline.m
js'], areas: ['apps/zhongshu-admin-web/', 'scripts/client/'], slow: true },
  { id: 'G11 鍚敤妯″潡鍚庣鍗曟祴锛坈ommon/infra锛屾帓闄や笂娓稿熀绾垮け璐ワ級', areas: ['services/'], m
vn: true,
    mvnArgs: '-pl zszj-framework/zszj-common,zszj-module-infra -am -Dtest=!Code
genEngineUniappTest#testExecute_treeSearch -Dsurefire.failIfNoSpecifiedTests=fa
lse test' },
];

// 澧為噺妯″紡涓嬭涓恒€岃壇鎬с€佷笉瑙﹀彂鍏ㄩ噺鍥為€€銆嶇殑鏈綊绫昏矾寰勫墠缂€锛堢敓鎴愮墿/璇勫鍘熷绋?璁″垝绋匡級
const BENIGN_IGNORE = ['outputs/', 'docs/reviews/', '.omx/'];

/** 鏋勯€犳湰娆¤璺戠殑闂ㄧ鍊欓€夛紙灏婇噸 --fast/--mvn锛夛紝涓嶅惈澧為噺绛涢€夈€?*/
export function candidateGates({ fast = false, mvn = false } = {}) {
  return GATES.filter((g) => {
    if (g.slow && fast) return false; // G10锛?-fast 璺宠繃杈冩參鐨?Web 绫诲瀷妫€鏌?    if (
g.mvn && !mvn) return false;  // G11锛氫粎 --mvn 鏄惧紡鍚敤
    return true;
  });
}

/**
 * 澧為噺绛涢€夛細鎸夊彉鏇磋矾寰勯€夐棬绂侊紱浠讳綍涓嶇‘瀹氶兘 fail-safe 鍥為€€鍏ㄩ噺锛堝畞澶氳窇涓嶆紡璺戯級銆? * 杩斿洖 { gate
s, mode: 'full'|'incremental', reason, changed }銆? * @param {object} o
 * @param {boolean} o.fast @param {boolean} o.mvn @param {boolean} o.incrementa
l
 * @param {string[]|null} o.changedFiles 鍙樻洿璺緞锛堢浉瀵逛粨搴撴牴锛屾鏂滄潬锛夛紱null 琛ㄧず鐢辫皟鐢ㄦ柟
妫€娴? */
export function planGates({ fast = false, mvn = false, incremental = false, cha
ngedFiles = null }) {
  const candidates = candidateGates({ fast, mvn });
  if (!incremental) return { gates: candidates, mode: 'full', reason: '鏈惎鐢?--i
ncremental锛岃窇鍏ㄩ噺', changed: changedFiles ?? [] };

  const changed = changedFiles ?? [];
  if (!changed.length) return { gates: candidates, mode: 'full', reason: '澧為噺锛氭
棤鍙樻洿鎴栧彉鏇存娴嬪け璐ワ紝鍥為€€鍏ㄩ噺', changed };
  if (changed.some((f) => f.startsWith('scripts/ops/'))) return { gates: candid
ates, mode: 'full', reason: '澧為噺锛氬彉鏇村惈闂ㄧ鑴氭湰鑷韩锛坰cripts/ops/锛夛紝鍥為€€鍏ㄩ噺', change
d };

  // 鏈綊绫讳笖闈炶壇鎬ц矾寰?鈫?淇濆畧鍥為€€鍏ㄩ噺锛堟棤娉曞垽鏂奖鍝嶉潰锛?  const allAreas = candidates.flat
Map((g) => g.areas ?? []);
  const unmapped = changed.filter((f) => !allAreas.some((a) => f.startsWith(a))
 && !BENIGN_IGNORE.some((b) => f.startsWith(b)));
  if (unmapped.length) return { gates: candidates, mode: 'full', reason: `澧為噺锛?
{unmapped.length} 涓彉鏇磋矾寰勬棤娉曞綊绫伙紙${unmapped.slice(0, 3).join(', ')}${unmapped.l
ength > 3 ? ' 鈥? : ''}锛夛紝鍥為€€鍏ㄩ噺`, changed };

  const selected = candidates.filter((g) => g.safety || (g.areas ?? []).some((a
) => changed.some((f) => f.startsWith(a))));
  return { gates: selected, mode: 'incremental', reason: `澧為噺锛氭寜 ${changed.leng
th} 涓彉鏇磋矾寰勯€変腑 ${selected.length}/${candidates.length} 闂ㄧ`, changed };
}

/** 妫€娴嬪伐浣滄爲鐩稿 HEAD 鐨勫彉鏇磋矾寰勶紙鍚凡璺熻釜淇敼涓庢湭璺熻釜鏂版枃浠讹級锛涘け璐ヨ繑鍥?[]銆?*/
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

/** 骞跺彂璺戝崟涓棬绂侊紝杩斿洖 { id, status, ms, tail }锛堜笉鎶涘紓甯革紝澶辫触浠?status 琛ㄨ揪锛夈€?*/
function runGate(gate) {
  const started = Date.now();
  return new Promise((resolve) => {
    const done = (status, output) => resolve({
      id: gate.id, status, ms: Date.now() - started,
      tail: String(output ?? '').trimEnd().split('\n').slice(-4).join(' | ').sl
ice(0, 400),
    });
    if (gate.mvnArgs) {
      // Maven 闂ㄧ闇€鍏堟敞鍏?tools 宸ュ叿閾剧幆澧冿紙JDK17/Maven 涓嶅湪绯荤粺 PATH锛?      const ch
ild = spawn('bash', ['-c', 'source tools/env.sh && cd services/zhongshu-core &&
 MSYS_NO_PATHCONV=1 "$TOOLS/apache-maven-3.9.9/bin/mvn.cmd" ' + gate.mvnArgs],
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

/** 浠?jobs 涓婇檺骞跺彂璺戦棬绂侊紱缁撴灉鎸?gates 瀹氫箟椤哄簭杩斿洖锛堜笌瀹屾垚椤哄簭鏃犲叧锛夛紝瀹屾垚鍗虫椂鎵撳嵃杩涘害銆?*/
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
  if (incremental) console.log(`[澧為噺] ${plan.reason}`);

  if (planOnly) {
    console.log(`灏嗚繍琛?${plan.gates.length} 椤归棬绂侊紙--plan锛屼笉鎵ц锛夛細`);
    for (const g of plan.gates) console.log('  - ' + g.id);
    return 0;
  }

  console.log(`杩愯 ${plan.gates.length} 椤归棬绂侊紙妯″紡 ${plan.mode}锛屽苟鍙?${Math.min(j
obs, plan.gates.length)}${fast ? '锛?-fast' : ''}${mvn ? '锛?-mvn' : ''}锛夆€);
  const t0 = Date.now();
  const results = await runGates(plan.gates, jobs);
  const totalMs = Date.now() - t0;

  console.log('\n===== 鏈湴鍩虹嚎闂ㄧ姹囨€?=====');
  for (const r of results) console.log(`${r.status.padEnd(4)}  ${r.id}`);
  const failed = results.filter((r) => r.status === 'FAIL');
  if (failed.length) {
    console.log('\n----- 澶辫触璇︽儏锛堣緭鍑烘湯灏撅級-----');
    for (const r of failed) console.log(`[FAIL] ${r.id}\n  ${r.tail}`);
  }
  console.log(`鍚堣 ${results.length} 椤癸紝澶辫触 ${failed.length} 椤癸紙骞跺彂鑰楁椂 ${totalM
s}ms锛塦);
  return failed.length ? 1 : 0;
}

const invokedDirectly = process.argv[1] && process.argv[1].replaceAll('\\', '/'
).endsWith('scripts/ops/run-local-gates.mjs');
if (invokedDirectly) {
  main().then((code) => process.exit(code)).catch((e) => { console.error(e); pr
ocess.exit(1); });
}
name: local-baseline-gates

# ZS-OPS-001.A锛氬熀纭€鏋勫缓/鏂囨。娴佹按绾块鏋垛€斺€斾笌鏈湴 `node scripts/ops/run-local-gates.mj
s` 鍚屼竴瑙勫垯銆?# 浠呰繍琛岄潤鎬?鍗曟祴闂ㄧ锛汸G/澶氱 E2E 闂ㄧ鎸?ZS-OPS-001.B~.E 鎵规鎺ュ叆銆?
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
洊锛?        run: node scripts/ops/run-local-gates.mjs --fast
/**
 * ZS-ENG-001 妯″潡鐧藉悕鍗曢潤鎬佹鏌ワ紙鏈湴/璇勫鍏ュ彛锛孧aven 娴嬭瘯 ModuleWhitelistTest 鐨勬棤 JDK 琛
ュ厖锛夈€? *
 * 妫€鏌ラ」锛? *  1. ModuleWhitelist.java 鐨?ENABLED_MODULES 涓庢牴 pom.xml / zszj-serv
er/pom.xml 婵€娲绘ā鍧椾竴鑷达紱
 *  2. DefaultController @RequestMapping 瑕嗙洊鍏ㄩ儴鏈惎鐢ㄦā鍧楀墠缂€锛屼笖涓嶅惈鍚敤妯″潡鍓嶇紑锛? *  
3. zszj-server 涓绘簮鐮佷笉寮曠敤鏈惎鐢ㄦā鍧楃殑鍖咃紱
 *  4. 鏈惎鐢ㄦā鍧椾笉鍦ㄦ牴 pom 婵€娲?modules 涓紙涓嶅弬涓庣紪璇戣閰嶏級銆? * 鐢ㄦ硶锛歯ode scripts/eng/ve
rify-module-whitelist.mjs
 */
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));
const core = join(root, 'services', 'zhongshu-core');
const rel = (p) => join(core, p);

const issues = [];
const stripComments = (t) => t.replace(/<!--[\s\S]*?-->/g, '');
const moduleArtifacts = (t) => [...stripComments(t).matchAll(/<artifactId>(zszj
-module-[a-z-]+)<\/artifactId>/g)].map((m) => m[1].replace('zszj-module-', ''))
;

// 瑙ｆ瀽鐧藉悕鍗曠被
const whitelistSrc = readFileSync(rel('zszj-framework/zszj-common/src/main/java
/cn/zszj/framework/common/catalog/ModuleCatalog.java'), 'utf8');
const enabledMatch = whitelistSrc.match(/ENABLED_MODULES = List\.of\(([^)]*)\)/
);
if (!enabledMatch) issues.push('鏃犳硶瑙ｆ瀽 ModuleWhitelist.ENABLED_MODULES');
const enabled = enabledMatch ? [...enabledMatch[1].matchAll(/"([a-z-]+)"/g)].ma
p((m) => m[1]) : [];
const disabled = [...whitelistSrc.matchAll(/"([a-z-]+)", List\.of\(([^)]*)\)\)/
g)].map((m) => ({
  module: m[1],
  prefixes: [...m[2].matchAll(/"([^"]+)"/g)].map((x) => x[1]),
}));
if (!enabled.length || !disabled.length) issues.push('鐧藉悕鍗曟竻鍗曚负绌烘垨瑙ｆ瀽澶辫触');

// 1. POM 婵€娲讳竴鑷存€?const rootPom = readFileSync(rel('pom.xml'), 'utf8');
const serverPom = readFileSync(rel('zszj-server/pom.xml'), 'utf8');
const rootModules = [...stripComments(rootPom).matchAll(/<module>(zszj-module-[
a-z-]+)<\/module>/g)].map((m) => m[1].replace('zszj-module-', ''));
const serverDeps = moduleArtifacts(serverPom);
if (JSON.stringify(rootModules.slice().sort()) !== JSON.stringify(enabled.slice
().sort())) {
  issues.push(`鏍?pom 婵€娲讳笟鍔℃ā鍧?[${rootModules}] 涓庣櫧鍚嶅崟 [${enabled}] 涓嶄竴鑷碻);
}
if (JSON.stringify(serverDeps.slice().sort()) !== JSON.stringify(enabled.slice(
).sort())) {
  issues.push(`zszj-server 渚濊禆涓氬姟妯″潡 [${serverDeps}] 涓庣櫧鍚嶅崟 [${enabled}] 涓嶄竴鑷碻)
;
}

// 2. DefaultController 瑕嗙洊
const controllerSrc = readFileSync(rel('zszj-server/src/main/java/cn/zszj/serve
r/controller/DefaultController.java'), 'utf8');
for (const { module, prefixes } of disabled) {
  for (const prefix of prefixes) {
    if (!controllerSrc.includes(`"${prefix}"`)) issues.push(`DefaultController 
缂哄皯 ${module} 鍓嶇紑鍏滃簳: ${prefix}`);
  }
}
for (const m of enabled) {
  if (controllerSrc.includes(`"/admin-api/${m}/**"`)) issues.push(`鍚敤妯″潡 ${m} 
涓嶅簲琚厹搴曟嫤鎴猔);
}

// 3. server 涓绘簮鐮佷笉寮曠敤鏈惎鐢ㄦā鍧楀寘
import { readdirSync, statSync } from 'node:fs';
const walkJava = (dir, fn) => {
  for (const name of readdirSync(dir)) {
    const p = join(dir, name);
    if (statSync(p).isDirectory()) walkJava(p, fn);
    else if (p.endsWith('.java')) fn(p);
  }
};
walkJava(rel('zszj-server/src/main/java'), (p) => {
  const text = readFileSync(p, 'utf8');
  for (const { module } of disabled) {
    if (text.includes(`cn.zszj.module.${module}.`)) issues.push(`${p} 寮曠敤鏈惎鐢ㄦā
鍧楀寘 cn.zszj.module.${module}`);
  }
});

// 4. 鏈惎鐢ㄦā鍧楁簮鐮佺洰褰曞瓨鍦紙淇濈暀璧勪骇锛変絾涓嶆縺娲?for (const { module } of disabled) {
  const dir = rel(`zszj-module-${module}`);
  try {
    statSync(dir);
  } catch {
    issues.push(`鏈惎鐢ㄦā鍧楁簮鐮佺洰褰曠己澶? zszj-module-${module}`);
  }
}

console.log(JSON.stringify({ enabled, disabledModules: disabled.map((d) => d.mo
dule), issueCount: issues.length, issues }, null, 2));
process.exitCode = issues.length ? 1 : 0;
/**
 * ZS-DB-001.A 鏁版嵁婧愪笌椹卞姩闈欐€佹鏌ワ紙鏃?Maven 鐜鐨勪緷璧栨爲浠ｇ悊楠岃瘉锛? * 瀹屾暣渚濊禆鏍?鎵撳寘浜х墿鏍搁獙鍦?
B01/B02 鐢?mvn dependency:tree + jar tf 琛ヨ瘉锛夈€? *
 * 妫€鏌ラ」锛? *  1. starter 涓悇鏁版嵁搴撻┍鍔紙鍚?mysql-connector-j锛夊潎涓?optional锛屼笉杩涘叆杩愯鍖
咃紱
 *  2. zszj-server 鏄惧紡渚濊禆 org.postgresql:postgresql锛堥潪 optional锛夛紝涓斾笉鍚换浣?MySQL
 椹卞姩锛? *  3. 鍏ㄩ儴鐜 yaml 鏃犳湭娉ㄩ噴鐨?jdbc:mysql / FROM DUAL / 妯℃嫙浠庡簱 slave 鏁版嵁婧愶紱
 *  4. 閮ㄧ讲妯℃澘鏁版嵁婧愪负 PG 鍗犱綅绗︺€? * 鐢ㄦ硶锛歯ode scripts/db/verify-datasource-pg.mjs
 */
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));
const core = join(root, 'services', 'zhongshu-core');
const issues = [];
const stripComments = (t) => t.replace(/<!--[\s\S]*?-->/g, '');

// 1. starter 椹卞姩 optional
const starterPom = readFileSync(join(core, 'zszj-framework/zszj-spring-boot-sta
rter-mybatis/pom.xml'), 'utf8');
const driverBlock = starterPom.match(/<!-- DB 鐩稿叧[\s\S]*?<\/dependency>\s*\n\s*
<dependency>\s*\n\s*<groupId>com\.alibaba<\/groupId>/);
if (!driverBlock) issues.push('鏃犳硶瀹氫綅 starter 鐨?DB 椹卞姩澹版槑鍧?);
else {
  const optionalDrivers = [...driverBlock[0].matchAll(/<artifactId>([a-z0-9-]+)
<\/artifactId>\s*(?:<optional>true<\/optional>)?/g)]
    .filter(([, a]) => /jdbc|connector|ojdbc|postgresql|sqlserver|kingbase|open
gauss|Dm/i.test(a));
  for (const [, artifact] of optionalDrivers) {
    const decl = driverBlock[0].match(new RegExp(`<artifactId>${artifact}</arti
factId>[\\s\\S]{0,80}`));
    if (decl && !decl[0].includes('<optional>true</optional>')) {
      issues.push(`starter 椹卞姩 ${artifact} 涓嶆槸 optional锛屼細杩涘叆杩愯鍖卄);
    }
  }
}

// 2. server 鏄惧紡 PG銆佹棤 MySQL
const serverPom = stripComments(readFileSync(join(core, 'zszj-server/pom.xml'),
 'utf8'));
const pgDep = serverPom.match(/<groupId>org\.postgresql<\/groupId>\s*\n\s*<arti
factId>postgresql<\/artifactId>([\s\S]{0,60})/);
if (!pgDep) issues.push('zszj-server 鏈樉寮忓０鏄?postgresql 椹卞姩');
else if (pgDep[1].includes('<optional>true</optional>')) issues.push('zszj-serv
er 鐨?postgresql 椹卞姩涓?optional锛屽皢涓嶈繘鍏ヨ繍琛屽寘');
if (/com\.mysql|mysql-connector/.test(serverPom)) issues.push('zszj-server 澹版槑浜
?MySQL 椹卞姩锛堢姝?MySQL 鍥為€€锛?);

// 3. 鐜 yaml 鏃?MySQL 鍥為€€
for (const f of ['application.yaml', 'application-local.yaml', 'application-dev
.yaml']) {
  const text = readFileSync(join(core, 'zszj-server/src/main/resources', f), 'u
tf8');
  const active = text.split('\n').filter((l) => !/^\s*#/.test(l)).join('\n');
  if (/jdbc:mysql:/.test(active)) issues.push(`${f} 瀛樺湪鏈敞閲婄殑 jdbc:mysql 閰嶇疆`);
  const hasDatasource = /dynamic:.*澶氭暟鎹簮閰嶇疆/.test(active) && /master:/.test(ac
tive);
  if (hasDatasource) {
    if (/FROM DUAL/.test(active)) issues.push(`${f} 鐨?validation-query 涓?MySQL 
鏂硅█锛團ROM DUAL锛塦);
    if (/^\s*slave:/.test(active)) issues.push(`${f} 浠嶅瓨鍦?slave 妯℃嫙浠庡簱`);
    if (!/validation-query:/.test(active)) issues.push(`${f} 缂哄皯 validation-que
ry锛堣繛鎺ユ鏌ワ級`);
  }
}

// 4. 閮ㄧ讲妯℃澘
const prodTemplate = readFileSync(join(core, 'script/config/application-prod.ya
ml'), 'utf8');
if (!prodTemplate.includes('${ZSZJ_DATASOURCE_URL}')) issues.push('閮ㄧ讲妯℃澘缂哄皯 ZS
ZJ_DATASOURCE_URL 蹇呭～鍗犱綅绗?);
if (/jdbc:mysql:/.test(prodTemplate)) issues.push('閮ㄧ讲妯℃澘鍚?jdbc:mysql');

console.log(JSON.stringify({ issueCount: issues.length, issues }, null, 2));
process.exitCode = issues.length ? 1 : 0;
/**
 * ZS-DB-003 杩佺Щ鏈哄埗闈欐€佹鏌ワ紙瑙勮寖瑙?services/zhongshu-core/docs/鏁版嵁搴撹縼绉昏鑼?md锛夈€? *
 * 妫€鏌ラ」锛? *  1. Flyway 渚濊禆宸插湪 zszj-server 澹版槑锛坒lyway-core + flyway-database-po
stgresql锛屾棤鎵嬪啓鐗堟湰鍙凤級锛? *  2. 鍩虹閰嶇疆鍚堝悓锛歟nabled=false銆佸崟涓€ location銆乷ut-of-order
=false銆乿alidate-on-migrate=true銆? *     clean-disabled=true銆乥aseline-on-migrate
=true锛? *  3. 閮ㄧ讲妯℃澘缁?ZSZJ_FLYWAY_ENABLED 婵€娲伙紱
 *  4. 杩佺Щ鐩綍鍐呮枃浠跺懡鍚嶇鍚?V<8浣嶆棩鏈?.<3浣嶅簭鍙?__<鎻忚堪>.sql 涓旂増鏈彿鍞竴锛? *  5. 涓嶅瓨鍦ㄧ浜屽
浼氶殢搴旂敤鎵ц鐨勮縼绉荤洰褰曪紙zszj-server 璧勬簮鍐呬笉寰楁湁鍏朵粬 flyway location 澹版槑锛夈€? * 鐢ㄦ硶锛歯ode s
cripts/db/verify-flyway-migrations.mjs
 */
import { readFileSync, readdirSync, existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));
const core = join(root, 'services', 'zhongshu-core');
const issues = [];

// 1. 渚濊禆
const pom = readFileSync(join(core, 'zszj-server/pom.xml'), 'utf8');
if (!pom.includes('<artifactId>flyway-core</artifactId>')) issues.push('zszj-se
rver 缂哄皯 flyway-core 渚濊禆');
if (!pom.includes('<artifactId>flyway-database-postgresql</artifactId>')) issue
s.push('缂哄皯 flyway-database-postgresql锛團lyway 10+ PG 鏀寔妯″潡锛?);
for (const m of pom.matchAll(/<artifactId>flyway[^<]*<\/artifactId>([\s\S]{0,40
}?<version>)/g)) {
  issues.push(`flyway 渚濊禆涓嶅簲鎵嬪啓鐗堟湰鍙凤紙搴旈殢 Spring Boot BOM锛? ${m[1] && '鍙戠幇 versi
on 鏍囩'}`);
}

// 2. 鍩虹閰嶇疆鍚堝悓
const base = readFileSync(join(core, 'zszj-server/src/main/resources/applicatio
n.yaml'), 'utf8');
const contract = [
  ['enabled: false', 'flyway 榛樿蹇呴』鍏抽棴锛堝熀绾跨敱 ZS-DB-004 浜や粯锛?],
  ['locations: classpath:db/migration', '蹇呴』澹版槑鍞竴杩佺Щ鐩綍'],
  ['out-of-order: false', '蹇呴』绂佹涔卞簭杩佺Щ'],
  ['validate-on-migrate: true', '蹇呴』寮€鍚鏀规牎楠?],
  ['clean-disabled: true', '蹇呴』鍏ㄧ幆澧冪鐢?clean'],
  ['baseline-on-migrate: true', '蹇呴』鍏佽鏃㈡湁鏃у簱鍚告敹涓哄熀绾?],
];
for (const [needle, why] of contract) {
  if (!base.includes(needle)) issues.push(`鍩虹閰嶇疆缂哄皯 ${needle}锛?{why}锛塦);
}

// 3. 閮ㄧ讲妯℃澘
const prod = readFileSync(join(core, 'script/config/application-prod.yaml'), 'u
tf8');
if (!prod.includes('ZSZJ_FLYWAY_ENABLED')) issues.push('閮ㄧ讲妯℃澘缂哄皯 ZSZJ_FLYWAY_E
NABLED 婵€娲诲紑鍏?);

// 4. 杩佺Щ鏂囦欢鍛藉悕涓庡敮涓€鎬?const migrationDir = join(core, 'zszj-server/src/main/res
ources/db/migration');
if (!existsSync(migrationDir)) issues.push('杩佺Щ鐩綍 db/migration 涓嶅瓨鍦?);
else {
  const versions = new Set();
  const nameRe = /^V(\d{8})\.(\d{3})__([a-z0-9_]+)\.sql$/;
  for (const name of readdirSync(migrationDir)) {
    if (!name.endsWith('.sql')) continue;
    const m = nameRe.exec(name);
    if (!m) { issues.push(`杩佺Щ鏂囦欢鍛藉悕涓嶇鍚堣鑼? ${name}锛堝簲涓?V<8浣嶆棩鏈?.<3浣嶅簭鍙?__<mod
ule>_<鎻忚堪>.sql锛塦); continue; }
    const version = `V${m[1]}.${m[2]}`;
    if (versions.has(version)) issues.push(`杩佺Щ鐗堟湰鍙烽噸澶? ${version}`);
    versions.add(version);
  }
}

// 5. 鏃犵浜屽 location
const configFiles = [
  ['application-local.yaml', join(core, 'zszj-server/src/main/resources/applica
tion-local.yaml')],
  ['application-dev.yaml', join(core, 'zszj-server/src/main/resources/applicati
on-dev.yaml')],
  ['application-prod.yaml锛堥儴缃叉ā鏉匡級', join(core, 'script/config/application-prod
.yaml')],
];
for (const [label, path] of configFiles) {
  const text = readFileSync(path, 'utf8');
  const matches = [...text.matchAll(/locations:\s*(.+)$/gm)];
  for (const m of matches) {
    if (!m[1].includes('classpath:db/migration')) issues.push(`${label} 澹版槑浜嗚鑼
冨鐨勮縼绉?location: ${m[1].trim()}`);
  }
}

console.log(JSON.stringify({ issueCount: issues.length, issues }, null, 2));
process.exitCode = issues.length ? 1 : 0;
/**
 * ZS-CLIENT-005.A Web 绫诲瀷妫€鏌ュ熀绾块棬绂併€? *
 * 璇箟锛氬綋鍓?vue-tsc 閿欒蹇呴』 鈯?宸插喕缁撳熀绾匡紙scripts/client/ts-baseline.json锛夛紱
 * 鍑虹幇浠讳綍鍩虹嚎涔嬪鐨勬柊閿欒鍗冲け璐ャ€傚熀绾挎潯鐩慨澶嶅悗搴斾粠 JSON 涓Щ闄わ紙缂╁噺鍩虹嚎锛夈€? *
 * 鍓嶇疆锛歴rc/types/auto-imports.d.ts 鐢?dev 妯″紡鐢熸垚鈥斺€旇嫢鏃犺鏂囦欢锛屾湰鑴氭湰鍏堜互
 *       60 绉掕秴鏃舵媺璧?`pnpm dev` 鐢熸垚锛屽啀鎵ц妫€鏌ャ€? * 鐢ㄦ硶锛歯ode scripts/client/verify
-ts-baseline.mjs
 */
import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { join, dirname } from 'node:path';
import { execFileSync } from 'node:child_process';

const root = fileURLToPath(new URL('../../', import.meta.url));
const app = join(root, 'apps/zhongshu-admin-web');
const baseline = JSON.parse(readFileSync(join(root, 'scripts/client/ts-baseline
.json'), 'utf8'));
const logPath = join(app, 'node_modules/.cache/vue-tsc/tsconfig.tsbuildinfo');
const errTextPath = join(app, 'vue-tsc-errors.txt');

// 鍓嶇疆锛歛uto-imports.d.ts锛坲nplugin 鍦?dev serve 鏃剁敓鎴愶級
const dts = join(app, 'src/types/auto-imports.d.ts');
if (!existsSync(dts)) {
  console.log('auto-imports.d.ts 缂哄け锛屾媺璧?dev server 鐢熸垚锛堟渶澶?60s锛夆€?);
  const child = spawn('pnpm', ['dev'], { cwd: app, shell: true, stdio: 'ignore'
 });
  const deadline = Date.now() + 60_000;
  while (Date.now() < deadline && !existsSync(dts)) {
    Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 1000);
  }
  try { child.kill(); } catch { /* 宸查€€鍑?*/ }
  if (!existsSync(dts)) {
    console.error('鏃犳硶鐢熸垚 src/types/auto-imports.d.ts锛氳鍏堣繍琛屼竴娆?pnpm dev');
    process.exit(2);
  }
}

let errorsText = '';
try {
  errorsText = execFileSync(
    'node',
    ['--max_old_space_size=8192', './node_modules/vue-tsc/bin/vue-tsc.js', '--n
oEmit', '--incremental',
     '--tsBuildInfoFile', 'node_modules/.cache/vue-tsc/tsconfig.tsbuildinfo'],
    { cwd: app, maxBuffer: 64 * 1024 * 1024, encoding: 'utf8' },
  );
} catch (e) {
  errorsText = String(e.stdout ?? '') + String(e.stderr ?? '');
}
writeFileSync(join(app, 'node_modules/.cache/vue-tsc-errors.txt'), errorsText);

const current = [...errorsText.matchAll(/^(src\/[^(]+)\((\d+),\d+\): error (TS\
d+): (.+)$/gm)]
  .map((m) => ({ file: m[1], line: Number(m[2]), code: m[3], message: m[4] }));

// 鎸?file+code 璁℃暟姣斿锛氭秷鎭枃鏈紙鑱斿悎绫诲瀷椤哄簭绛夛級鍦?vue-tsc 杈撳嚭涓彲鑳戒笉绋冲畾锛?// 璁℃暟姣斿瀵规牸寮
忕ǔ瀹氥€佸悓鏃惰兘鍙戠幇鍚岀被鏂板閿欒
const countBy = (list) => {
  const map = new Map();
  for (const e of list) {
    const key = `${e.file}|${e.code}`;
    map.set(key, (map.get(key) ?? 0) + 1);
  }
  return map;
};
const baseCount = countBy(baseline);
const currCount = countBy(current);
const newErrors = [];
for (const [key, n] of currCount) {
  const base = baseCount.get(key) ?? 0;
  if (n > base) {
    newErrors.push({ key, count: n, baseline: base });
  }
}
const recoveredBaselineEntries = [...baseCount.keys()].filter((k) => !currCount
.has(k));

console.log(JSON.stringify({
  currentErrors: current.length,
  baselineEntries: baseline.length,
  newErrors: newErrors.slice(0, 20),
  recoveredBaselineEntries,
}, null, 2));

if (newErrors.length) {
  console.error('瀛樺湪鍩虹嚎涔嬪鐨勬柊绫诲瀷閿欒锛岀姝㈡斁琛岋紙淇鍚庢垨缁忚瘎瀹℃洿鏂板熀绾垮悗鍐嶆彁浜わ級');
  process.exitCode = 1;
} else {
  console.error('閫氳繃锛氬綋鍓嶇被鍨嬮敊璇叏閮ㄤ綅浜庡凡鍐荤粨鍩虹嚎鍐?);
  if (recoveredBaselineEntries.length) {
    console.error(`鎻愮ず锛氬熀绾夸腑 ${recoveredBaselineEntries.length} 绫诲凡涓嶅啀鍑虹幇锛屽彲浠?t
s-baseline.json 绉婚櫎`);
  }
}

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'Get-
Content -Encoding UTF8 scripts/gov/verify-docs.mjs; Get-Content -Encoding UTF8 
scripts/gov/verify-docs.test.mjs; Get-Content -Encoding UTF8 scripts/brand/appl
y-naming-migration.test.mjs; Get-Content -Encoding UTF8 scripts/brand/verify-br
and-naming.test.mjs; Get-Content -Encoding UTF8 scripts/cfg/verify-config-secre
ts.mjs; Get-Content -Encoding UTF8 scripts/brand/verify-brand-naming.mjs' in E:
\众墅之家AI赋能平台底座
 succeeded in 0ms:
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

test('正常文档通过（链接/编号/状态/决策/版本均一致）', () => {
  const files = ['README.md', 'docs/05-x.md'];
  const issues = checkDocs(
    files,
    read({
      'README.md': '| [任务清单](docs/05-x.md) | V1.5 | 索引 |\n',
      'docs/05-x.md': '> 文档版本：V1.5\n\n### ZS-ENG-001：示例\n\n- 关联：WP-02；B01。类别 改造
；状态 待验收；前置 B00。\n',
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
  const issues = checkDocs(['docs/05-x.md'], read({ 'docs/05-x.md': doc }), roo
t);
  assert.ok(issues.some((i) => i.rule === 'R2-dup-id'));
});

test('非法任务状态会失败，合法枚举通过', () => {
  const bad = '- 关联：WP-02；状态 已完成；前置 B00。';
  const issues = checkDocs(['docs/05-x.md'], read({ 'docs/05-x.md': bad }), roo
t);
  assert.ok(issues.some((i) => i.rule === 'R3-status'));
  const good = '- 关联：WP-02；状态 暂缓；前置 B00。';
  assert.deepEqual(checkDocs(['docs/05-x.md'], read({ 'docs/05-x.md': good }), 
root), []);
});

test('未确认决策（D-10）被写成既成事实会失败，否定表述通过', () => {
  const bad = '按 D-10 的已确认结论接入真实微信。';
  const issues = checkDocs(['docs/05-x.md'], read({ 'docs/05-x.md': bad }), roo
t);
  assert.ok(issues.some((i) => i.rule === 'R4-decision'));
  const good = 'D-10 的候选接入方案，不是已确认结论。';
  assert.deepEqual(checkDocs(['docs/05-x.md'], read({ 'docs/05-x.md': good }), 
root), []);
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
    read({ 'README.md': '[外链](https://example.com/a) [绝对路径](/Users/x/a.md)\n' }
),
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
  const issues = checkDocs(['docs/05-x.md'], read({ 'docs/05-x.md': doc }), roo
t);
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
  const issues = checkDocs(['docs/05-x.md'], read({ 'docs/05-x.md': doc }), roo
t);
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
  SCOPES, SCOPE_PREFIXES, PRESERVED, preservedReason, selectFiles, planScope, t
ransform, root,
} from './apply-naming-migration.mjs';
import { SQL_SEED_REPLACEMENTS } from './naming-rules.mjs';

const UPGRADE_SQL = 'services/zhongshu-core/sql/postgresql/upgrades/20260908_br
and_rename_zszj.sql';
const CODEGEN_README = 'services/zhongshu-core/zszj-module-infra/src/main/resou
rces/codegen/README.md';
const PG_SEED = 'services/zhongshu-core/sql/postgresql/ruoyi-vue-pro.sql';
const MYSQL_SEED = 'services/zhongshu-core/sql/mysql/ruoyi-vue-pro.sql';

const gitFiles = (...args) => execFileSync('git', ['ls-files', '-z', ...args], 
{ cwd: root, maxBuffer: 64 * 1024 * 1024 })
  .toString('utf8').split('\0').filter(Boolean);

/** 全部 scope 的候选文件（扣除 SCOPE_EXCLUSIONS 与 PRESERVED 之前），用于检验规则是否空写。 */
const candidates = gitFiles(...new Set(SCOPES.flatMap((s) => SCOPE_PREFIXES[s])
));

test('PRESERVED 每条规则都真实命中候选文件（防空写静默失效）', () => {
  for (const p of PRESERVED) {
    const hit = candidates.filter((f) => p.re.test(f));
    assert.ok(hit.length > 0, `PRESERVED 规则永不匹配，属静默失效: ${p.re} （${p.reason}）`);
    assert.ok(p.reason && p.reason.length > 10, `PRESERVED 规则缺少可审计的 reason: ${p
.re}`);
  }
});

test('P2-1：冻结迁移脚本与 Flyway 基线在任何 scope 下都不被选中', () => {
  for (const scope of SCOPES) {
    const { files, preserved } = selectFiles(scope);
    for (const f of files) {
      assert.equal(preservedReason(f), null, `scope=${scope} 选中了应冻结的文件: ${f}`);
      assert.ok(!/\/upgrades\//.test(f), `scope=${scope} 选中了 upgrades 迁移脚本: ${f
}`);
      assert.ok(!/\/db\/migration\/V[\d.]+__/.test(f), `scope=${scope} 选中了 Flyw
ay 迁移: ${f}`);
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
  assert.ok(!files.includes(CODEGEN_README), 'codegen README 不应被 backend scope 
选中');
  assert.ok(preserved.includes(CODEGEN_README), 'codegen README 应由 PRESERVED 显式
排除');
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
  assert.ok(sql.files.length >= 30, `sql scope 选中文件数异常偏少: ${sql.files.length}`)
;

  const backend = selectFiles('backend');
  assert.ok(backend.files.length >= 8000, `backend scope 选中文件数异常偏少: ${backend.f
iles.length}`);
  assert.ok(backend.files.some((f) => f.endsWith('.java')), 'backend scope 应仍包含
 Java 源码');
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

test('P3：planScope 接受无尾分隔符的自定义根（path.join 而非字符串拼接）', () => {
  // 默认 root 以分隔符结尾，`cwd + file` 恰好可用，掩盖了拼接缺陷。
  // 去掉尾分隔符模拟 process.cwd() 等常规目录路径：若仍用字符串拼接会抛 ENOENT。
  const bareRoot = root.replace(/[\\/]+$/, '');
  assert.ok(bareRoot.length < root.length, '构造的无尾分隔符根应短于默认 root');
  const viaDefault = planScope('sql');
  const viaBare = planScope('sql', bareRoot);
  assert.deepEqual(viaBare.wouldChange, viaDefault.wouldChange);
  assert.deepEqual(viaBare.skipped, viaDefault.skipped);
  assert.equal(viaBare.scanned, viaDefault.scanned);
  assert.ok(viaBare.scanned > 0, 'sql scope 应扫到文件，否则本用例无法证明路径拼接正确');
});

test('P2-2：租户 websites 上游域名的映射目标互不相同', () => {
  const map = new Map(SQL_SEED_REPLACEMENTS);
  // 这三个域名在上游种子的 system_tenant.websites 中实际出现（租户 1 / 121 / 122），
  // 而 websites 受 validTenantWebsiteDuplicate 唯一校验并用于 getTenantByWebsite 路由，
  // 故其目标必须两两不同。
  const websiteDomains = ['www.iocoder.cn', 'test.iocoder.cn', 'zsxq.iocoder.cn
'];
  const targets = websiteDomains.map((d) => {
    assert.ok(map.has(d), `SQL_SEED_REPLACEMENTS 缺少租户域名映射: ${d}`);
    return map.get(d);
  });
  assert.equal(new Set(targets).size, targets.length,
    `租户 websites 域名映射目标存在冲突: ${JSON.stringify(websiteDomains.map((d, i) => [d, 
targets[i]]))}`);
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
  const checkedFiles = new Set();
  for (const f of sqlFiles) {
    const text = readFileSync(root + f, 'utf8');
    if (!text.includes('system_tenant')) continue;
    const rows = [];
    for (const line of text.split(/\r?\n/)) {
      // 表名可能被反引号（MySQL）、双引号（部分 PG dump）包裹或裸写，三者都要匹配。
      // 此前仅接受 `"?`，导致反引号包裹的 MySQL 种子被整体跳过（codex hotfix-C P2）。
      if (!/insert\s+into\s+[`"]?system_tenant[`"]?\s/i.test(line)) continue;
      const id = (line.match(/values\s*\(?\s*'?(\d+)'?/i) || [])[1];
      DOMAIN.lastIndex = 0;
      const doms = [...line.matchAll(DOMAIN)].map((m) => m[1]);
      if (id && doms.length) rows.push({ id, doms });
    }
    if (!rows.length) continue;
    checked++;
    checkedFiles.add(f);
    const owner = new Map();
    for (const { id, doms } of rows) {
      for (const d of doms) {
        assert.ok(!d.endsWith('iocoder.cn'), `${f} 租户 ${id} 的 websites 仍含上游域名: 
${d}`);
        const prev = owner.get(d);
        assert.equal(prev, undefined,
          `${f} 域名 ${d} 同时属于租户 ${prev} 与 ${id}，会使 getTenantByWebsite 结果不确定`);
        owner.set(d, id);
      }
    }
  }
  assert.ok(checked >= 8, `受检 SQL 种子文件数异常偏少: ${checked}`);
  // 方言覆盖断言：MySQL 种子以反引号包裹表名，曾因正则只接受双引号/裸写而被整体跳过，
  // 使租户 1/121 同持 www.zszj.example.com 的真实同域逃过门禁（hotfix-C 漏改的第 8 个方言）。
  // 显式断言 MySQL 与 PostgreSQL 主种子都确实进入了逐行核对，杜绝「少扫一个方言」的静默漏检。
  assert.ok(checkedFiles.has(MYSQL_SEED),
    `MySQL 种子未被租户同域门禁覆盖（反引号表名漏匹配）: ${MYSQL_SEED}`);
  assert.ok(checkedFiles.has(PG_SEED),
    `PostgreSQL 主种子未被租户同域门禁覆盖: ${PG_SEED}`);
});
import test from 'node:test';
import assert from 'node:assert/strict';
import { judge, loadAllowlist, scanTree, BINARY } from './verify-brand-naming.m
js';

const now = new Date('2026-09-09T00:00:00Z');
const entries = loadAllowlist();

test('白名单可加载且条目结构合法', () => {
  assert.ok(entries.length > 0);
  for (const e of entries) assert.ok(['必要保留', '限期兼容'].includes(e.category), e.r
eason);
});

test('上游署名链接与注释不误报（来源引用放行）', () => {
  const r = judge('services/x/ZszjCacheAutoConfiguration.java',
    '// 参考 https://gitee.com/yudaocode/yudao-boot-mini/issues/I86VY2 与 LICENSE 
说明', entries, now);
  assert.equal(r.violations.length, 0);
});

test('故意恢复旧标题/旧 import/生成器默认包名时门禁失败', () => {
  const cases = [
    ['apps/zhongshu-admin-web/index.html', '<title>芋道管理系统</title>'],
    ['services/x/DemoService.java', 'import cn.iocoder.yudao.framework.common.p
ojo.CommonResult;'],
    ['services/x/codegen/java/pom.xml.vm', '<groupId>cn.iocoder.yudao</groupId>
'],
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
  const ok = judge('services/x/DemoService.java', '// 芋道源码：① 移除暂时用不到的 websocket
', entries, now);
  assert.equal(ok.violations.length, 0);
  const author = judge('services/x/DemoService.java', ' * @author 芋道源码', entrie
s, now);
  assert.equal(author.violations.length, 0);
  // 非署名上下文的 芋道源码 应失败（hotfix-B 收紧后不再被 standalone 模式放行）
  const fail = judge('apps/x/src/Login.vue', "tenantName: '芋道源码',", entries, no
w);
  assert.ok(fail.violations.length > 0, '非署名上下文的芋道源码应失败');
  const title = judge('apps/x/src/Login.vue', "title: '芋道源码管理平台'", entries, now
);
  assert.ok(title.violations.length > 0, '产品可见旧标题应失败');
});

test('路径白名单整文件放行且内容计入 allowed', () => {
  const r = judge('services/zhongshu-core/zszj-ui/yudao-ui-admin-vue3/src/App.v
ue', 'yudao 芋道 unibest', entries, now);
  assert.equal(r.violations.length, 0);
  assert.equal(r.allowed.length, 3);
});

test('限期兼容条目到期后放行失效（yd- 前缀）', () => {
  const text = '<yd-form-picker />';
  const before = judge('apps/zhongshu-miniapp/src/pages/index.vue', text, entri
es, new Date('2026-09-09T00:00:00Z'));
  assert.equal(before.violations.length, 0);
  const expired = [{ ...entries.find((e) => e.content?.includes('\\byd-[a-z]'))
, expiresAt: new Date('2026-09-08T23:59:59Z') }];
  const after = judge('apps/zhongshu-miniapp/src/pages/index.vue', text, expire
d, new Date('2026-09-09T00:00:00Z'));
  assert.ok(after.violations.length > 0, '到期兼容项必须失败');
});

test('正常 zszj 内容不产生任何命中', () => {
  const r = judge('apps/x/src/a.vue', '众墅之家 cn.zszj ZSZJ_API zszj-admin-web', e
ntries);
  assert.equal(r.violations.length, 0);
  assert.equal(r.allowed.length, 0);
});

test('hotfix-B：yd-* 限期兼容例外受 path 限定，不溢出到小程序/codegen 之外', () => {
  // 管理端文件名含 yd- 前缀必须失败（codex P2-9 复现路径）
  const p = 'apps/zhongshu-admin-web/src/yd-foo.ts';
  assert.ok(judge(p, p, entries, now).violations.length > 0, '管理端 yd- 文件名应失败');
  // 后端其他模块的 yd- 内容必须失败
  const other = judge('services/zhongshu-core/zszj-module-system/src/main/resou
rces/a.txt',
    '<view class="yd-page-container" />', entries, now);
  assert.ok(other.violations.length > 0, '非登记范围的 yd- 内容应失败');
  // 小程序本体与 codegen uniapp 同批夹具在限期内仍放行
  assert.equal(judge('apps/zhongshu-miniapp/src/components/a.vue', '<yd-form-pi
cker />', entries, now).violations.length, 0);
  const fixturePath = 'services/zhongshu-core/zszj-module-infra/src/test/resour
ces/codegen/vue3_admin_uniapp_master_erp/vue/a.vue';
  assert.equal(judge(fixturePath, '<view class="yd-page-container" />', entries
, now).violations.length, 0);
  // 同一路径内多次命中须逐一被覆盖判定放行（曾因 content 正则缺 g 标志只匹配首个而误报）
  const multi = 'apps/zhongshu-miniapp/src/components/zszj-ui/yd-form-picker/yd
-form-picker.vue';
  assert.equal(judge(multi, multi, entries, now).violations.length, 0, '同一路径内多次
 yd- 命中应全部放行');
});

test('hotfix-B：SVG 不再被当作二进制跳过，其余二进制扩展名仍跳过', () => {
  assert.ok(!BINARY.test('apps/zhongshu-admin-web/src/assets/logo.svg'), 'svg 必
须进入文本扫描');
  for (const f of ['a.png', 'a.jpg', 'a.ico', 'a.woff2', 'a.jar', 'a.zip']) {
    assert.ok(BINARY.test(f), `${f} 仍应作为二进制跳过`);
  }
  // SVG 内的产品可见品牌串必须被判为违规（原先整文件被跳过）
  const r = judge('apps/zhongshu-admin-web/src/assets/logo.svg',
    '<svg xmlns="http://www.w3.org/2000/svg"><text>芋道管理系统</text></svg>', entrie
s, now);
  assert.ok(r.violations.length > 0, 'SVG 文本内旧品牌串应失败');
});

test('hotfix-B：例外匹配区间必须覆盖命中点，邻近白名单串不得庇护', () => {
  // P2-4：注释内的上游文档域名不得庇护紧随其后的旧产品标题
  const shielded = judge('apps/zhongshu-admin-web/src/a.html',
    '<!-- doc.iocoder.cn -->\n<title>芋道管理系统</title>', entries, now);
  assert.ok(shielded.violations.some((v) => v.pattern === '芋道'), '旧标题应失败，不得被邻近白
名单串庇护');
  // 同一文件内的上游域名本身仍应放行
  assert.ok(!shielded.violations.some((v) => v.pattern === 'cn.iocoder'), '上游文档
域名应放行');
});

test('hotfix-B：署名例外不得被 URL 斜杠与引号字符串反向利用', () => {
  // https:// 内的 // 不得被当作行注释起始而庇护同屏产品标题
  const html = judge('apps/zhongshu-admin-web/src/a.html',
    '<link href="https://cdn.example.com/x.css">\n<title>芋道源码管理平台</title>', ent
ries, now);
  assert.ok(html.violations.some((v) => v.pattern === '芋道'), 'URL 斜杠不得庇护产品标题');
  // JSON/JS 字符串字面量中的产品文案不得被引号模式庇护
  const json = judge('apps/zhongshu-admin-web/src/a.json', '{"siteTitle": "芋道源码
管理平台"}', entries, now);
  assert.ok(json.violations.some((v) => v.pattern === '芋道'), '引号字符串不得庇护产品文案');
  // 真正的行注释署名仍放行
  const comment = judge('services/x/A.java', '// 芋道源码：① 移除暂时用不到的 websocket', en
tries, now);
  assert.equal(comment.violations.length, 0);
});

test('hotfix-B r1：codex 评审探针样本——三类庇护路径已闭合', () => {
  // 尾随通配过贪：URL 后的 "> 不得被并入匹配区间而庇护紧随的产品标题
  const a = judge('apps/zhongshu-admin-web/src/a.html',
    '<a href="https://doc.iocoder.cn">芋道管理系统</a>', entries, now);
  assert.ok(a.violations.some((v) => v.pattern === '芋道'), 'URL 尾随通配不得跨引号庇护产品标题'
);
  // setter 形态限定 src/test：应用侧 setTitle(...) 不得被测试夹具模式庇护
  const b = judge('apps/zhongshu-admin-web/src/a.html', 'setTitle("芋道源码管理平台");'
, entries, now);
  assert.ok(b.violations.some((v) => v.pattern === '芋道'), '应用侧 set* 调用不得被测试夹具模式
庇护');
  // package.json 裸 token：name 等产品元数据字段不得被 provenance 例外庇护
  const c = judge('apps/zhongshu-miniapp/package.json', '{\n  "name": "unibest"
\n}', entries, now);
  assert.ok(c.violations.some((v) => v.pattern === 'unibest'), 'package.json na
me 字段不得被 provenance 例外庇护');
  // 真实 provenance 形态仍放行
  const d = judge('apps/zhongshu-miniapp/package.json',
    '{\n  "unibest-version": "4.1.0",\n  "repository": "https://github.com/feig
e996/unibest"\n}', entries, now);
  assert.equal(d.violations.length, 0, 'provenance 字段与仓库地址应放行');
  // 真实测试夹具形态仍放行
  const e = judge('services/zhongshu-core/zszj-module-bpm/src/test/java/A.java'
,
    'group.setName("芋道源码");', entries, now);
  assert.equal(e.violations.length, 0, 'src/test 下的 setter 夹具应放行');
});

test('hotfix-B r2：字符集不得跨行终止符与 JSON 转义（codex r1 两项 P2 回归）', () => {
  // P2-a：.* 不含换行，而显式字符集 [^)] 含——无关赋值不得跳行豁免后续产品代码
  const ex = judge('apps/zhongshu-admin-web/src/a.ts',
    'const example = 1;\nconst title = "芋道源码管理平台";', entries, now);
  assert.ok(ex.violations.some((v) => v.pattern === '芋道'), 'example 赋值不得跨 \n 豁免
下一行产品标题');
  const exCrlf = judge('apps/zhongshu-admin-web/src/a.ts',
    'const example = 1;\r\nconst title = "芋道源码管理平台";', entries, now);
  assert.ok(exCrlf.violations.some((v) => v.pattern === '芋道'), 'CRLF 文件同样不得跨行豁免
');
  // P2-a 同根因：注释模式不得跨行豁免反引号包裹的产品标题
  const cmt = judge('apps/zhongshu-admin-web/src/a.ts',
    '// 上游模板说明\nconst pageTitle = `芋道源码管理平台`;', entries, now);
  assert.ok(cmt.violations.some((v) => v.pattern === '芋道'), '行注释不得跨 \n 豁免后续反引号标
题');
  // P2-b：\s 不含反斜杠——JSON 转义序列 \n 是两个非空白字符，URL 匹配必须止于转义
  const pkg = judge('apps/zhongshu-miniapp/package.json',
    '{\n  "description": "https://unibest.tech\\n芋道管理系统"\n}', entries, now);
  assert.ok(pkg.violations.some((v) => v.pattern === '芋道'), 'JSON 转义后的产品标题不得被上游
 URL 遮蔽');
  assert.ok(!pkg.violations.some((v) => v.pattern === 'unibest'), 'URL 本体仍应放行')
;
  // 正向对照：同行真实形态仍全部放行
  assert.equal(judge('services/x/A.java',
    '@Schema(description = "昵称", example = "芋道源码")', entries, now).violations.l
ength, 0,
    '同行 example 署名应放行');
  assert.equal(judge('services/x/A.java',
    '// 芋道源码 修复了 https://doc.iocoder.cn/x 的问题\nString title = "众墅之家";', entries
, now).violations.length, 0,
    '同行注释署名应放行，且不影响下一行');
  assert.equal(judge('apps/zhongshu-admin-web/src/a.md',
    '见 https://github.com/yudaocode/ruoyi-vue-pro\n标题：众墅之家', entries, now).viol
ations.length, 0,
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
  const HEADS = ['', '// x', 'example = ', 'example = "', ') ', '- ', '@author 
', '@author', '"author"',
    'regex ', '芋道源码', 'name=', ' Created by ', 'foo();', 'x)', '---', '[link](h
ttps://x.com)', '*', '  ', '\t',
    'https://doc.iocoder.cn', 'https://unibest.tech', 'github.com/yudaocode/', 
'static.iocoder.cn/', 'doc.yudao'];
  const TAILS = ['芋道源码', '芋道管理系统', '芋道', 'unibest', 'yudao', 'Yudao', 'cn.iocod
er', 'youdao'];
  for (const e of entries) {
    for (const c of e.content ?? []) {
      const re = new RegExp(c);
      for (const head of HEADS) {
        for (const t of TAILS) {
          for (const nl of ['\n', '\r\n']) {
            const s = head + nl + t;
            re.lastIndex = 0;
            const m = re.exec(s);
            const crosses = !!(m && /[\r\n]/.test(m[0]) && m[0].includes(t) && 
m.index < head.length);
            assert.ok(!crosses,
              `content 模式跨行豁免: ${c} 对 ${JSON.stringify(s)} 匹配到 ${JSON.stringify
(m && m[0])}`);
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
    if (e.path) assert.doesNotThrow(() => new RegExp(e.path), `path 正则非法: ${e.p
ath}`);
    for (const c of e.content ?? []) assert.doesNotThrow(() => new RegExp(c), `
content 正则非法: ${c}`);
  }
});
/**
 * ZS-CFG-001.A 配置秘密门禁：模板无真实秘密、必填项无默认回退。
 *
 * 扫描范围：后端 application*.yaml、部署模板、两端 env 模板。
 * 检查项：
 *  1. 秘密类键（password/secret/token/key 等）的取值只能是：空、${...} 占位符、REDACTED 标记；
 *  2. 配置文件中不得出现长十六进制串（≥20）、RSA/PEM 密钥块、超长 Base64（≥200 字符）；
 *  3. 部署模板中的必填环境变量必须为无默认值占位符（缺配即启动明确失败）。
 * 用法：node scripts/cfg/verify-config-secrets.mjs
 */
import { readFileSync, existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));
const targets = [
  ['services/zhongshu-core/zszj-server/src/main/resources/application.yaml', 'y
aml'],
  ['services/zhongshu-core/zszj-server/src/main/resources/application-local.yam
l', 'yaml'],
  ['services/zhongshu-core/zszj-server/src/main/resources/application-dev.yaml'
, 'yaml'],
  ['services/zhongshu-core/script/config/application-prod.yaml', 'yaml'],
  ['apps/zhongshu-admin-web/.env', 'env'],
  ['apps/zhongshu-admin-web/.env.dev', 'env'],
  ['apps/zhongshu-admin-web/.env.prod', 'env'],
  ['apps/zhongshu-admin-web/.env.stage', 'env'],
  ['apps/zhongshu-admin-web/.env.test', 'env'],
  ['apps/zhongshu-miniapp/env/.env', 'env'],
  ['apps/zhongshu-miniapp/env/.env.development', 'env'],
  ['apps/zhongshu-miniapp/env/.env.production', 'env'],
  ['apps/zhongshu-miniapp/env/.env.test', 'env'],
];

const SECRET_KEY = /(password|passwd|secret|token|access-?key|secret-?key|api-?
key|private-?key)\s*[:=]/i;
const PLACEHOLDER = /^\$\{.*\}$/;
const SAFE_LITERALS = /^(redacted.*|xx|''|"")$/i;
const issues = [];
let scanned = 0;

for (const [rel, kind] of targets) {
  const abs = join(root, rel);
  if (!existsSync(abs)) continue;
  scanned++;
  const text = readFileSync(abs, 'utf8');
  const lines = text.split(/\r?\n/);
  lines.forEach((line, i) => {
    const active = !/^\s*#/.test(line); // 注释行不做键值判定，但仍做密钥块/长串判定
    // 1. 秘密类键的取值
    if (active && SECRET_KEY.test(line)) {
      const value = line.slice(line.search(SECRET_KEY)).split(/[:=]/).slice(1).
join(':').trim().replace(/^["']|["']$/g, '');
      if (value && !PLACEHOLDER.test(value) && !SAFE_LITERALS.test(value) && !v
alue.startsWith('${') && !value.includes('# ')) {
        issues.push({ path: rel, line: i + 1, reason: `秘密类键使用了字面量取值: ${line.tri
m().slice(0, 60)}` });
      }
    }
    // 2. 密钥块与长串
    if (/(BEGIN (RSA )?PRIVATE KEY|BEGIN CERTIFICATE)/.test(line)) {
      issues.push({ path: rel, line: i + 1, reason: '配置文件中出现私钥/证书块' });
    }
    const base64Blob = line.match(/[A-Za-z0-9+/=]{200,}/);
    if (base64Blob) issues.push({ path: rel, line: i + 1, reason: '配置文件中出现超长 Ba
se64 串（疑似密钥）' });
    if (active) {
      const hex = line.match(/\b[0-9a-f]{20,}\b/i);
      if (hex) issues.push({ path: rel, line: i + 1, reason: `出现长十六进制串（疑似凭据/统计 
ID）: ${hex[0].slice(0, 12)}…` });
    }
  });
}

// 3. 部署模板必填项必须无默认值（缺配即启动失败，见 script/config/README.md 合同）
const prodRel = 'services/zhongshu-core/script/config/application-prod.yaml';
const prod = readFileSync(join(root, prodRel), 'utf8');
const REQUIRED = [
  'ZSZJ_DATASOURCE_URL', 'ZSZJ_DATASOURCE_USERNAME', 'ZSZJ_DATASOURCE_PASSWORD'
,
  'ZSZJ_REDIS_HOST', 'ZSZJ_REDIS_PASSWORD',
];
for (const name of REQUIRED) {
  const bare = new RegExp(`\\$\\{${name}\\}`);
  if (!bare.test(prod)) {
    issues.push({ path: prodRel, reason: `必填变量 ${name} 必须为无默认值占位符 \${${name}}（缺
配明确失败）` });
  }
}

console.log(JSON.stringify({ scanned, issueCount: issues.length, issues: issues
.slice(0, 30) }, null, 2));
process.exitCode = issues.length ? 1 : 0;
/**
 * ZS-BRAND-006.A 品牌/代码命名残留门禁（本地与 CI 同一入口）。
 *
 * 用法：node scripts/brand/verify-brand-naming.mjs
 * 扫描 git 跟踪文件中的旧产品标识（文件名 + 文本内容）：
 *   cn.iocoder / yudao / Yudao / YUDAO / youdao / 芋道 / unibest（上游模板标识）
 * 按 scripts/brand/brand-naming-allowlist.json 判定：
 *   path 条目整文件放行；content 条目仅放行命中其 content 正则的匹配；
 *   限期兼容条目 expires 到期后视同未放行（门禁失败）。
 * 未放行的残留使进程以非零退出；不追求全仓零命中，不抹去第三方来源。
 */
import { readFileSync, existsSync, statSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';

const root = fileURLToPath(new URL('../../', import.meta.url));
const allowlistFile = 'scripts/brand/brand-naming-allowlist.json';

// 旧产品标识模式（docs/06 第 2 节；unibest 为登记的上游模板标识；
// yd- 为登记缩写（docs/06 第 4.3 节限期兼容），纳入模式使到期撤除机制生效）
const PATTERNS = [
  { name: 'cn.iocoder', re: /cn\.iocoder/g },
  { name: 'yudao', re: /yudao/gi },
  { name: 'youdao', re: /youdao/gi },
  { name: '芋道', re: /芋道/g },
  { name: 'unibest', re: /unibest/gi },
  { name: 'yd-prefix', re: /\byd-[a-z]/g },
];
// hotfix-B P2-7: SVG 是 XML 文本，可含 <text>/<title> 等品牌串，不再作为二进制跳过（导出以供单测固化）
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
    contentRes: (e.content ?? []).map((c) => new RegExp(c, 'g')), // g 标志用于 cov
erage check 遍历所有匹配；每次使用前重置 lastIndex
    expiresAt: e.expires ? new Date(`${e.expires}T23:59:59Z`) : null,
  }));
}

function entryExpired(entry, now = new Date()) {
  return entry.expiresAt !== null && now > entry.expiresAt;
}

/** 对单个文件的命中做白名单判定；返回未放行命中。 */
export function judge(relativePath, text, entries, now = new Date()) {
  // hotfix-B P1-3: pathPass 仅对无 scope 或 scope=path 的条目生效；
  // scope=content 的条目必须经内容匹配判定，不得整文件豁免
  const pathPass = entries.some((e) =>
    e.pathRe && e.pathRe.test(relativePath) &&
    (!e.scope || e.scope === 'path') &&
    !entryExpired(e, now)
  );
  const hits = [];
  for (const { name, re } of PATTERNS) {
    re.lastIndex = 0; // 模块级 /g 正则，逐文件重置
    let m;
    while ((m = re.exec(text))) hits.push({ pattern: name, index: m.index, text
: m[0] });
  }
  if (pathPass) return { allowed: hits, violations: [] };
  const violations = [];
  for (const hit of hits) {
    const fragment = text.slice(Math.max(0, hit.index - 40), hit.index + 40);
    // hotfix-B P2-4: 白名单 content 正则的匹配区间必须覆盖命中点，
    // 防止邻近 40 字符内的不相关白名单串庇护产品可见品牌残留
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
    // hotfix-B P2-6: 文件/目录名检查——对完整相对路径应用全部 PATTERNS，
    // 不仅检查 basename + 3 个模式，避免目录残留与遗漏模式逃逸
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
      const { violations } = judge(rel, rel, entries, now); // 用路径文本自身做判定（含路径白名
单）
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

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "Get-
Content -Encoding UTF8 scripts/brand/apply-naming-migration.test.mjs | Select-O
bject -Skip 112; Get-Content -Encoding UTF8 scripts/verify-source-copy.test.mjs
; git -c core.quotepath=false ls-files scripts tools .github; Get-Content -Enco
ding UTF8 .gitignore; rg -n 'run-local-gates|\\]\\(["'^)]*(services/|apps/|scri
pts/|'"\\.omx)' README.md docs/0*.md; node --version; node --test scripts/ops/r
un-local-gates.test.mjs" in E:\众墅之家AI赋能平台底座
 succeeded in 119ms:
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
rg: docs/0*.md: 文件名、目录名或卷标语法不正确。 (os error 123)
README.md:18:- 文档治理：`FND-DOC-001～003` 已进入需求基线，当前均为 `DOCUMENTED`；本地文档一致性检查器（ZS-G
OV-001 `verify-docs` R1～R7）已实现并纳入 `run-local-gates`，含 §2/README 统计分布一致性门禁（R6/R7
）与 `close-task` 一键收口回填；真实 CI 接入与 CI_VERIFIED 登记归 ZS-OPS-001
README.md:33:| [现阶段底座开发清单与多端架构](.omx/plans/2026-09-08-底座开发清单与多端架构.md) | V1.0 建议
稿 | 24 个工作包、当前源码证据、复用/二开/自研边界、Web/小程序/App/iOS 接入与验收 |
README.md:82:ZS-BRAND-006 交付的品牌/代码命名残留门禁：扫描全部跟踪文件的文件名与内容（`cn.iocoder`、`yudao`、`
youdao`、`芋道`、`unibest`、登记缩写 `yd-`），按 [scripts/brand/brand-naming-allowlist.json
](scripts/brand/brand-naming-allowlist.json) 以"应改/必要保留/限期兼容"判定；限期兼容项到期未撤除即失败。故意
恢复旧产品标题、旧 `cn.iocoder.yudao` import 或生成器默认包名时门禁失败；上游 LICENSE、署名注释与来源引用不误报。命名映射与
例外依据见 [docs/06-品牌素材与命名映射](docs/06-品牌素材与命名映射.md)。
README.md:87:node scripts/ops/run-local-gates.mjs                       # 全量（含 
Web 类型基线，较慢）
README.md:88:node scripts/ops/run-local-gates.mjs --fast                # 快速静态门
禁（日常与 CI 同规则）
README.md:89:node scripts/ops/run-local-gates.mjs --fast --incremental  # 增量：按 
git 变更路径只跑受影响门禁
README.md:90:node scripts/ops/run-local-gates.mjs --fast --plan         # 只预览将跑
哪些门禁，不执行
v24.19.0
✔ GATES 定义稳定：12 项，G3/G9 为安全守卫，G10 慢检查，G11 需 mvn (0.8474ms)
✔ candidateGates 默认（无 fast/mvn）：含 G10、排除 G11 (0.1285ms)
✔ candidateGates --fast：跳过 G10 与 G11，恰为 10 项（与 CI 一致） (0.0697ms)
✔ candidateGates --mvn（非 fast）：G10 与 G11 均启用，共 12 项 (0.0526ms)
✔ candidateGates --fast --mvn：跳过 G10、启用 G11 (0.0646ms)
✔ planGates 非增量：mode=full，门禁等于候选 (0.1048ms)
✔ planGates 增量 + 仅文档变更：选中 G3/G4/G5/G9，跳过 G1/G2/G6/G7/G8 (0.1037ms)
✔ planGates 增量 + 后端变更：选中 G3/G6/G7/G8/G9（services 命中） (0.0694ms)
✔ planGates 增量 + 后端变更 + mvn：追加 G11 (0.0762ms)
✔ planGates 增量 fail-safe：变更含门禁脚本自身 scripts/ops/ → 回退全量 (0.0917ms)
✔ planGates 增量 fail-safe：无法归类且非良性路径 → 回退全量 (0.1506ms)
✔ planGates 增量 fail-safe：无变更/检测失败 → 回退全量 (0.0523ms)
✔ planGates 增量 + 良性未归类路径 outputs/：不回退全量，仅跑安全守卫 G3/G9 (0.0573ms)
✔ planGates 增量：安全守卫 G3/G9 在任何选中集内恒定出现 (0.1018ms)
ℹ tests 14
ℹ suites 0
ℹ pass 14
ℹ fail 0
ℹ cancelled 0
ℹ skipped 0
ℹ todo 0
ℹ duration_ms 51.8622

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "Get-
Content -Encoding UTF8 scripts/brand/apply-naming-migration.mjs; rg -n '\\]\\([
"'^)]*(services/|apps/|scripts/|'"\\.omx)' README.md docs -g '0*.md'; rg -n 'wr
ite|mkdir|File\\(|OUTPUT|output|generate|tmpdir' services/zhongshu-core/zszj-mo
dule-infra/src/test/java/cn/zszj/module/infra/service/codegen -g '*Test.java'; 
node scripts/ops/run-local-gates.mjs --fast" in E:\众墅之家AI赋能平台底座
 exited 1 in 1224ms:
[PASS] G7 数据源 PG 合同 (44ms)
[PASS] G6 模块白名单 (46ms)
[PASS] G8 Flyway 迁移规范 (49ms)
[PASS] G9 配置秘密门禁 (52ms)
[PASS] G5 文档一致性全量 (61ms)
[PASS] G4 文档一致性单测 (113ms)
[PASS] G2 品牌命名门禁单测 (131ms)
[FAIL] G1 来源复制校验器单测 (135ms)
[PASS] G3 品牌命名全仓扫描 (914ms)
[PASS] G2b 命名迁移工具单测（冻结文件排除 + 租户域名唯一） (1241ms)

===== 本地基线门禁汇总 =====
FAIL  G1 来源复制校验器单测
PASS  G2 品牌命名门禁单测
PASS  G2b 命名迁移工具单测（冻结文件排除 + 租户域名唯一）
PASS  G3 品牌命名全仓扫描
PASS  G4 文档一致性单测
PASS  G5 文档一致性全量
PASS  G6 模块白名单
PASS  G7 数据源 PG 合同
PASS  G8 Flyway 迁移规范
PASS  G9 配置秘密门禁

----- 失败详情（输出末尾）-----
[FAIL] G1 来源复制校验器单测
      syscall: 'symlink', |     path: 'sample.txt', |     dest: 'C:\\Users\\ADM
INI~1\\AppData\\Local\\Temp\\zhongshu-copy-test-b8PKxz\\source\\link.txt' |   }
合计 10 项，失败 1 项（并发耗时 1247ms）

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "@'
import { readFileSync, existsSync, readdirSync } from 'node:fs';
import { resolve } from 'node:path';
import { planGates } from './scripts/ops/run-local-gates.mjs';
import { checkDocs } from './scripts/gov/verify-docs.mjs';
import { judge, loadAllowlist } from './scripts/brand/verify-brand-naming.mjs';
const seed = 'services/zhongshu-core/sql/postgresql/ruoyi-vue-pro.sql';
const before = readFileSync(seed, 'utf8');
const after = before.replaceAll('zsxq.zszj.example.com', 'www.zszj.example.com'
);
console.log('seed mutation applied:', before "'!== after);
console.log('"'incremental seed gates:', planGates({fast:true, incremental:true
, changedFiles:[seed]}).gates.map(g=>g.id.split(' ')[0]));
console.log('brand violations for colliding seed:', judge(seed, after, loadAllo
wlist()).violations.length);
console.log('colliding tenant rows:', after.split(/\\r?\\n/).filter(l=>/insert\
\s+into\\s+["'`"]?system_tenant[`"]?'"\\s/i.test(l) && l.includes('www.zszj.exa
mple.com')).map(l=>l.match(/values\\s*\\(?\\s*'?(\\d+)'?/i)?.[1]));
const deleted = 'services/zhongshu-core/zszj-server/Dockerfile';
const docs = ['README.md', 'docs/' + readdirSync('docs').find(f => f.startsWith
('05-'))];
const issues = checkDocs(docs, f=>readFileSync(f,'utf8'), process.cwd(), {exist
s:p=>resolve(p)"'!==resolve(deleted) && existsSync(p)});
console.log('"'incremental deleted target gates:', planGates({fast:true, increm
ental:true, changedFiles:[deleted]}).gates.map(g=>g.id.split(' ')[0]));
console.log('doc issues from simulated target deletion:', issues.filter(i=>i.me
ssage.includes('Dockerfile')));
'@ | node --input-type=module
node --test scripts/verify-source-copy.test.mjs" in E:\众墅之家AI赋能平台底座
 exited 1 in 67ms:
✔ Git blob hash includes the Git header (0.7537ms)
✔ an exact imported file passes (1.5001ms)
✔ content changes and missing files are reported (0.7336ms)
✔ paths outside the project and duplicate entries are rejected (0.6413ms)
✖ a regular file may not silently become a symbolic link (0.5768ms)
✖ a recorded symlink is checked without following its target (0.4556ms)
✔ non-canonical paths and unknown file modes are rejected (0.32ms)
﹣ executable mode and parent symlink changes are rejected (0.0456ms) # SKIP
✔ a nonempty, consistent manifest passes (0.894ms)
✔ empty or unsupported manifests fail (0.3767ms)
✔ source counts and source-to-target mapping must agree (0.6775ms)
✔ unrecorded source files fail, generated build caches do not (0.8988ms)
✔ sanitization records must exactly match the imported differences (0.7422ms)
ℹ tests 13
ℹ suites 0
ℹ pass 10
ℹ fail 2
ℹ cancelled 0
ℹ skipped 1
ℹ todo 0
ℹ duration_ms 60.8392

✖ failing tests:

test at scripts\verify-source-copy.test.mjs:37:1
✖ a regular file may not silently become a symbolic link (0.5768ms)
  Error: EPERM: operation not permitted, symlink 'sample.txt' -> 'C:\Users\ADMI
NI~1\AppData\Local\Temp\zhongshu-copy-test-TKoEwc\source\link.txt'
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
    dest: 'C:\\Users\\ADMINI~1\\AppData\\Local\\Temp\\zhongshu-copy-test-TKoEwc
\\source\\link.txt'
  }

test at scripts\verify-source-copy.test.mjs:43:1
✖ a recorded symlink is checked without following its target (0.4556ms)
  Error: EPERM: operation not permitted, symlink 'sample.txt' -> 'C:\Users\ADMI
NI~1\AppData\Local\Temp\zhongshu-copy-test-mHEEp8\source\link.txt'
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
    dest: 'C:\\Users\\ADMINI~1\\AppData\\Local\\Temp\\zhongshu-copy-test-mHEEp8
\\source\\link.txt'
  }

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "rg -
n \"@/utils/is\" apps/zhongshu-admin-web/src -g '*.ts' -g '*.vue' -m 1 | Select
-Object -First 8; Get-Content -Encoding UTF8 apps/zhongshu-admin-web/src/utils/
is.ts | Select-Object -First 20; @'
import { mkdtempSync, mkdirSync, writeFileSync, unlinkSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { execFileSync } from 'node:child_process';
import { planGates } from './scripts/ops/run-local-gates.mjs';
const fixture = mkdtempSync(join(tmpdir(), 'gate-review-rename-'));
const git = (...args) => execFileSync('git', ['-c', 'safe.directory=*', ...args
], {cwd: fixture, encoding:'utf8', stdio:['ignore', 'pipe', 'pipe']}).trim();
const oldPath = 'apps/zhongshu-admin-web/src/utils/is.ts';
const newPath = 'apps/zhongshu-miniapp/src/utils/is.ts';
for (const dir of ['apps/zhongshu-admin-web/src/utils','apps/zhongshu-miniapp/s
rc/utils']) mkdirSync(join(fixture,dir), {recursive:true});
git('init');
writeFileSync(join(fixture, oldPath), 'export const isString = value => typeof 
value === \"string\";\\n');
git('add','.');
git('-c','user.name=Review Fixture','-c','user.email=review@example.invalid','-
c','commit.gpgsign=false','commit','-m','fixture');
writeFileSync(join(fixture, newPath), 'export const isString = value => typeof 
value === \"string\";\\n');
unlinkSync(join(fixture, oldPath));
git('add','-A');
const raw = git('-c','core.quotepath=false','diff','--name-only','HEAD');
console.log('git rename status:', git('diff','--name-status','HEAD'));
console.log('detected paths:', JSON.stringify(raw.split('\\n')));
console.log('selected gates:', planGates({incremental:true,changedFiles:raw.spl
it('\\n')}).gates.map(g=>g.id.split(' ')[0]));
console.log('with both rename paths:', planGates({incremental:true,changedFiles
:[oldPath,newPath]}).gates.map(g=>g.id.split(' ')[0]));
'@ | node --input-type=module" in E:\众墅之家AI赋能平台底座
 succeeded in 117ms:
git rename status: R100	apps/zhongshu-admin-web/src/utils/is.ts	apps/zhongshu-m
iniapp/src/utils/is.ts
detected paths: ["apps/zhongshu-miniapp/src/utils/is.ts"]
selected gates: [ 'G3', 'G9' ]
with both rename paths: [ 'G3', 'G9', 'G10' ]

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '$n =
 0; Get-Content -Encoding UTF8 scripts/ops/run-local-gates.mjs | ForEach-Object
 { $n++; if ($n -le 100) { '"'{0,3}: {1}' -f "'$n, $_ } }; git diff --quiet c9d
178b2 -- README.md '"'docs/05-底座模块分析与开发任务清单.md' scripts/ops/run-local-gates.mjs
 scripts/ops/run-local-gates.test.mjs; git status --short; Get-ChildItem -Liter
alPath 'E:\\', 'E:\\众墅之家AI赋能平台底座', 'E:\\众墅之家AI赋能平台底座\\scripts', 'E:\\众墅之家AI赋能平台
底座\\scripts\\ops', 'E:\\众墅之家AI赋能平台底座\\docs' -Force -File -ErrorAction SilentlyC
ontinue | Where-Object { "'$_.Name -eq '"'AGENTS.md' -or "'$_.Name -eq '"'AGENT
S.override.md' } | Select-Object -ExpandProperty FullName" in E:\众墅之家AI赋能平台底座
 succeeded in 62ms:
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
Incremental mode can omit demonstrably affected gates for seed edits, cross-dir
ectory renames, and linked-file deletions. The 14 planner tests pass, but they 
do not cover these cases.

Full review comments:

- [P2] Include G2b's repository data in its dependency mapping — E:/众墅之家AI赋能平台底
座/scripts/ops/run-local-gates.mjs:31-31
  With `--incremental`, SQL seed edits skip G2b because its only area is `scrip
ts/brand/`. However, `apply-naming-migration.test.mjs` reads the actual SQL see
ds and checks cross-tenant website uniqueness. Changing `zsxq.zszj.example.com`
 to `www.zszj.example.com` in the PostgreSQL seed creates a duplicate that G2b 
detects, but the selected G3/G6/G7/G8/G9 gates do not. Include the repository d
ata consumed by this test in its areas, or keep G2b always enabled.

- [P2] Preserve both source and destination paths for renames — E:/众墅之家AI赋能平台底座
/scripts/ops/run-local-gates.mjs:84-84
  For a staged cross-directory rename, `git diff --name-only HEAD` reports only
 the destination when rename detection succeeds. For example, moving `apps/zhon
gshu-admin-web/src/utils/is.ts` into the miniapp selects only G3/G9, even witho
ut `--fast`, skipping G10 despite breaking existing Web imports such as those i
n `App.vue`. Collect both sides of renames, using `--no-renames` or parsing ren
ame-aware status output, before selecting gates.

- [P2] Run document validation when linked targets disappear — E:/众墅之家AI赋能平台底座/
scripts/ops/run-local-gates.mjs:34-34
  Deleting a linked file outside `docs/` skips G5 even though its link-existenc
e checks are affected. For example, deleting `services/zhongshu-core/zszj-serve
r/Dockerfile` selects G3/G6/G7/G8/G9, while `verify-docs.mjs` would report brok
en links in docs/05. Trigger G5 for deleted or renamed link targets, or keep it
 unconditional; changes to Markdown content are not its only dependency.
Incremental mode can omit demonstrably affected gates for seed edits, cross-directory renames, and linked-file deletions. The 14 planner tests pass, but they do not cover these cases.

Full review comments:

- [P2] Include G2b's repository data in its dependency mapping — E:/众墅之家AI赋能平台底座/scripts/ops/run-local-gates.mjs:31-31
  With `--incremental`, SQL seed edits skip G2b because its only area is `scripts/brand/`. However, `apply-naming-migration.test.mjs` reads the actual SQL seeds and checks cross-tenant website uniqueness. Changing `zsxq.zszj.example.com` to `www.zszj.example.com` in the PostgreSQL seed creates a duplicate that G2b detects, but the selected G3/G6/G7/G8/G9 gates do not. Include the repository data consumed by this test in its areas, or keep G2b always enabled.

- [P2] Preserve both source and destination paths for renames — E:/众墅之家AI赋能平台底座/scripts/ops/run-local-gates.mjs:84-84
  For a staged cross-directory rename, `git diff --name-only HEAD` reports only the destination when rename detection succeeds. For example, moving `apps/zhongshu-admin-web/src/utils/is.ts` into the miniapp selects only G3/G9, even without `--fast`, skipping G10 despite breaking existing Web imports such as those in `App.vue`. Collect both sides of renames, using `--no-renames` or parsing rename-aware status output, before selecting gates.

- [P2] Run document validation when linked targets disappear — E:/众墅之家AI赋能平台底座/scripts/ops/run-local-gates.mjs:34-34
  Deleting a linked file outside `docs/` skips G5 even though its link-existence checks are affected. For example, deleting `services/zhongshu-core/zszj-server/Dockerfile` selects G3/G6/G7/G8/G9, while `verify-docs.mjs` would report broken links in docs/05. Trigger G5 for deleted or renamed link targets, or keep it unconditional; changes to Markdown content are not its only dependency.
