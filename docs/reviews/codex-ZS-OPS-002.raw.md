node.exe : OpenAI Codex v0.154.0
所在位置 C:\Users\Administrator\AppData\Roaming\npm\codex.ps1:24 字符: 5
+     & "node$exe"  "$basedir/node_modules/@openai/codex/bin/codex.js"  ...
+     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    + CategoryInfo          : NotSpecified: (OpenAI Codex v0.154.0:String) [], 
    RemoteException
    + FullyQualifiedErrorId : NativeCommandError
 
--------
workdir: E:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇\.wt\zszj-wt-ops-002-a
model: gpt-6-astra
provider: openai
approval: never
sandbox: workspace-write [workdir, /tmp, $TMPDIR]
reasoning effort: xhigh
reasoning summaries: none
session id: 01a0a286-1df8-7752-ad90-c2f187bf6971
--------
user
commit 8159cf2
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'Get-
Location; git status --short; git show --stat --oneline --decorate --no-renames
 8159cf2c; Get-ChildItem -Force -Name' in E:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇\.wt\zszj-wt-ops-
002-a
 succeeded in 36ms:
8159cf2c (HEAD -> feat/ops-002-a) feat(ops-002-a): 琛ラ綈鑴辨晱閮ㄧ讲妯℃澘銆佹帰閽堣竟鐣屼笌鍙鏌ユ€ф
牎楠?[ZS-OPS-002]
 .gitignore                                         |   1 +
 scripts/ops/verify-deploy-templates.mjs            | 296 +++++++++++++++++++++
 scripts/ops/verify-deploy-templates.test.mjs       | 169 ++++++++++++
 services/zhongshu-core/deploy/.env.example         |  40 +++
 .../zhongshu-core/deploy/docker-compose.deploy.yml | 129 +++++++++
 .../zhongshu-core/deploy/nginx/zszj-server.conf    |  74 ++++++
 ...216\242\351\222\210\350\276\271\347\225\214.md" | 222 ++++++++++++++++
 7 files changed, 931 insertions(+)
Path                                 
----                                 
E:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇\.wt\zszj-wt-ops-002-a
.github
.omx
apps
docs
reference
scripts
services
third_party
tools
.git
.gitignore
README.md
THIRD_PARTY_NOTICES.md
_zs_d_commit_msg.txt



exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '$pat
hs = @('"'E:\\AGENTS.override.md','E:\\AGENTS.md','E:\\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇\\AGENT
S.override.md','E:\\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇\\AGENTS.md','E:\\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇\\.wt\\
AGENTS.override.md','E:\\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇\\.wt\\AGENTS.md','AGENTS.override.md
','AGENTS.md'); foreach ("'$p in $paths) { if (Test-Path -LiteralPath $p) { Wri
te-Output "--- $p"; Get-Content -LiteralPath $p } }; Get-ChildItem -Recurse -Fo
rce -File -Include AGENTS.md,AGENTS.override.md | Select-Object -ExpandProperty
 FullName; git -c core.quotePath=false show --format=fuller --no-ext-diff --no-
renames 8159cf2c' in E:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇\.wt\zszj-wt-ops-002-a
 succeeded in 1174ms:
commit 8159cf2c213f981e49d75dabf6de9276b30421e0
Author:     hlong026 <peihr85@gmail.com>
AuthorDate: Tue Sep 15 08:38:52 2026 +0800
Commit:     hlong026 <peihr85@gmail.com>
CommitDate: Tue Sep 15 08:38:52 2026 +0800

    feat(ops-002-a): 琛ラ綈鑴辨晱閮ㄧ讲妯℃澘銆佹帰閽堣竟鐣屼笌鍙鏌ユ€ф牎楠?[ZS-OPS-002]
    
    鏂板浜や粯鐗╋細
    - services/zhongshu-core/deploy/docker-compose.deploy.yml
      PostgreSQL 16 + Redis 7 + zszj-server + Nginx 缂栨帓妯℃澘
      鎵€鏈夋晱鎰熼厤缃€氳繃 ${ZSZJ_*} 鐜鍙橀噺娉ㄥ叆锛屾棤纭紪鐮佸嚟鎹?      zszj-server healthcheck
 鎺㈤拡 /actuator/health/liveness
      postgres/redis 绔彛浠呯粦瀹?127.0.0.1锛堜笉鍏紑鏆撮湶锛?    - services/zhongshu-core/d
eploy/.env.example
      閰嶇疆娉ㄥ叆鍚堝悓锛歓SZJ_SERVER_PORT/PROFILE/DATASOURCE_*/REDIS_*/TLS_*
      鍏ㄩ儴鍊间负鍗犱綅绗︽垨绌猴紝绂佹鎻愪氦 .env
    - services/zhongshu-core/deploy/nginx/zszj-server.conf
      TLS 缁堢粨锛堣瘉涔?volume mount 娉ㄥ叆锛?      /actuator/ 鍜?/admin/ 鍏抽棴锛坉eny all; re
turn 403锛?    - scripts/ops/verify-deploy-templates.mjs
      C1 妯℃澘鏃犵湡瀹炵瀵嗭紙澶嶇敤 ZS-CFG-001.A 鍒ゅ畾鍙ｅ緞锛?      C2 鎺㈤拡璺緞涓?actuator exposur
e.include 涓€鑷?      C3 鏁忔劅绠＄悊璺緞鍦ㄥ弽浠ｄ腑鍏抽棴
      C4 鍐呴儴绔彛涓嶅叕寮€鏆撮湶
      C5 鐜鍙橀噺鍚堝悓鍙屽悜涓€鑷?      --self-test 璐熷悜瀵圭収锛? 椤瑰叏 PASS锛?    - scripts/ops
/verify-deploy-templates.test.mjs
      21 鐢ㄤ緥锛宯ode --test 鍏ㄧ豢锛堟鍚?+ 璐熷悜瀵圭収锛?    - services/zhongshu-core/docs/閮ㄧ
讲妯℃澘涓庢帰閽堣竟鐣?md
      閮ㄧ讲/鎾ゅ洖姝ラ銆侀厤缃敞鍏ュ悎鍚屻€佹帰閽堣竟鐣屻€乀LS 绾︽潫
      濡傚疄鏍囨敞 .B/.C 鎵规鏈墽琛岄」锛堜笉铏氭姤鐢熶骇鍙敤锛?    
    .gitignore锛氳拷鍔?!services/zhongshu-core/deploy/.env.example 渚嬪锛?    浣块厤缃悎鍚
屾ā鏉垮彲鍏ュ簱锛堥伒寰棦鏈?apps/ 渚嬪浣撲緥锛夈€?    
    楠屾敹杈圭晫锛?A 鎵规閫€鍑烘潯浠讹級锛?    鉁?妯℃澘鍙鏌ワ紙verify-deploy-templates.mjs exit 0锛? 
   鉁?鏁忔劅绠＄悊璺緞鍏抽棴锛坣ginx deny /actuator/ + /admin/锛?    鉁?鐪熷疄涓氬姟鍚姩鑱旈獙锛堝睘 B02/B0
3锛?    鉁?鐩戞祴鍛婅钀藉湴锛堝睘 .B锛屽墠缃?ZS-DB-005 + ZS-JOB-004锛?    鉁?澶囦唤鎭㈠婕旂粌涓?RPO/RTO锛堝
睘 .C锛屽墠缃?ZS-FILE-005.B锛?
diff --git a/.gitignore b/.gitignore
index 80a003cd..af1fc135 100644
--- a/.gitignore
+++ b/.gitignore
@@ -26,6 +26,7 @@ dist-prod/
 !apps/zhongshu-miniapp/env/.env.development
 !apps/zhongshu-miniapp/env/.env.production
 !apps/zhongshu-miniapp/env/.env.test
+!services/zhongshu-core/deploy/.env.example
 
 # Keep versioned editor recommendations from the complete source snapshots.
 !apps/zhongshu-admin-web/.vscode/
diff --git a/scripts/ops/verify-deploy-templates.mjs b/scripts/ops/verify-deplo
y-templates.mjs
new file mode 100644
index 00000000..83196e22
--- /dev/null
+++ b/scripts/ops/verify-deploy-templates.mjs
@@ -0,0 +1,296 @@
+/**
+ * ZS-OPS-002.A 閮ㄧ讲妯℃澘鍙鏌ユ€ф牎楠岋紙绾潤鎬侊紝鏈湴涓?CI 鍚屼竴鍏ュ彛锛夈€?+ *
+ * 鏍￠獙椤癸細
+ *   C1 妯℃澘鏃犵湡瀹炵瀵嗭紙澶嶇敤 ZS-CFG-001.A 鍒ゅ畾鍙ｅ緞锛?+ *   C2 鎺㈤拡璺緞涓庡悗绔?actuator 鏆撮湶涓
€鑷?+ *   C3 鏁忔劅绠＄悊璺緞鍦ㄥ弽浠ｄ腑纭疄鍏抽棴
+ *   C4 鍙嶄唬/compose 涓嶆硠闇插唴閮ㄧ鐞嗙鍙?+ *   C5 妯℃澘寮曠敤鐨勭幆澧冨彉閲忎笌鍚堝悓琛ㄥ弻鍚戜竴鑷?+ *
+ * 鐢ㄦ硶锛歯ode scripts/ops/verify-deploy-templates.mjs [--self-test]
+ * 閫€鍑虹爜锛?=鍏ㄩ儴閫氳繃锛?=瀛樺湪 issue锛?=鑴氭湰鑷韩寮傚父
+ */
+import { readFileSync, existsSync } from 'node:fs';
+import { fileURLToPath } from 'node:url';
+import { join } from 'node:path';
+
+const root = fileURLToPath(new URL('../../', import.meta.url));
+
+export const CONTRACT_VARS = [
+  'ZSZJ_SERVER_PORT',
+  'ZSZJ_DATASOURCE_URL',
+  'ZSZJ_DATASOURCE_USERNAME',
+  'ZSZJ_DATASOURCE_PASSWORD',
+  'ZSZJ_REDIS_HOST',
+  'ZSZJ_REDIS_PORT',
+  'ZSZJ_REDIS_PASSWORD',
+  'ZSZJ_PROFILE',
+  // TLS 璇佷功鎸傝浇璺緞锛堣繍缁村繀濉紱璇佷功鍐呭绂佹鍏ュ簱锛屼粎閰嶇疆璺緞锛?+  'ZSZJ_TLS_CERT_PATH',
+  'ZSZJ_TLS_KEY_PATH',
+];
+
+export const DEPLOY_TARGETS = [
+  'services/zhongshu-core/deploy/docker-compose.deploy.yml',
+  'services/zhongshu-core/deploy/.env.example',
+  'services/zhongshu-core/deploy/nginx/zszj-server.conf',
+];
+
+// ---- C1 ----
+
+const SECRET_KEY_RE = /(password|passwd|secret|token|access.?key|secret.?key|a
pi.?key|private.?key)\s*[:=]/i;
+const PLACEHOLDER_RE = /^\$\{[^}]*\}$/;
+const SAFE_LITERALS_RE = /^(redacted.*|xx|''|""|)$/i;
+
+export function checkSecrets(relPath, text) {
+  const issues = [];
+  const lines = text.split(/\r?\n/);
+  lines.forEach((line, i) => {
+    const lineNo = i + 1;
+    if (/BEGIN (RSA |EC |DSA )?PRIVATE KEY|BEGIN CERTIFICATE/.test(line)) {
+      issues.push({ rule: 'C1-secret', path: relPath, line: lineNo, message: '
妯℃澘涓嚭鐜扮閽?璇佷功鍧? });
+      return;
+    }
+    const hexMatch = line.match(/\b[0-9a-fA-F]{32,}\b/);
+    if (hexMatch && !/commit|sha|checksum|hash/i.test(line)) {
+      issues.push({ rule: 'C1-secret', path: relPath, line: lineNo, message: '
鍑虹幇闀垮崄鍏繘鍒朵覆锛堢枒浼煎嚟鎹級: ' + hexMatch[0].slice(0, 12) });
+      return;
+    }
+    if (SECRET_KEY_RE.test(line)) {
+      const eqIdx = line.search(/[:=]/);
+      const value = line.slice(eqIdx + 1).trim().replace(/^["']|["']$/g, '');
+      if (value && !PLACEHOLDER_RE.test(value) && !SAFE_LITERALS_RE.test(value
) && !value.startsWith('${')) {
+        issues.push({ rule: 'C1-secret', path: relPath, line: lineNo, message:
 '绉樺瘑绫婚敭浣跨敤浜嗗瓧闈㈤噺鍙栧€? ' + line.trim().slice(0, 60) });
+      }
+    }
+  });
+  return issues;
+}
+
+
+// ---- C2 鎺㈤拡涓€鑷存€?----
+
+/**
+ * 鏍￠獙 compose healthcheck 鎺㈤拡璺緞涓?actuator 鏆撮湶涓€鑷淬€?+ * @param {string} compo
seText docker-compose 妯℃澘鍐呭
+ * @param {string} actuatorInclude actuator exposure.include 鍊硷紙濡?'health' 鎴?'
health,info'锛?+ */
+export function checkProbeConsistency(composeText, actuatorInclude) {
+  const issues = [];
+  const exposed = actuatorInclude.split(',').map((s) => s.trim()).filter(Boole
an);
+
+  // compose 蹇呴』鍚?healthcheck 瀹氫箟
+  if (!/healthcheck/i.test(composeText)) {
+    issues.push({ rule: 'C2-probe', path: 'deploy/docker-compose.deploy.yml', 
line: 0, message: '閮ㄧ讲妯℃澘缂哄皯 healthcheck 瀹氫箟锛堟帰閽堣竟鐣屾湭澹版槑锛? });
+    return issues;
+  }
+
+  // 鎻愬彇 compose 涓紩鐢ㄧ殑 /actuator/ 璺緞
+  const probePaths = [...composeText.matchAll(/\/actuator\/([\w/-]+)/g)].map((
m) => m[1]);
+  for (const p of probePaths) {
+    const endpoint = p.split('/')[0]; // health/liveness 鈫?health
+    if (!exposed.includes(endpoint)) {
+      issues.push({ rule: 'C2-probe', path: 'deploy/docker-compose.deploy.yml'
, line: 0, message: `鎺㈤拡寮曠敤 /actuator/${p} 浣?actuator exposure.include 鏈惈 "${e
ndpoint}"` });
+    }
+    // liveness/readiness 瀛愯矾寰勯渶瑕?probes.enabled锛堟敮鎸?Spring property 鍜?env var
 涓ょ鍐欐硶锛?+    if (/^health\/(liveness|readiness)/.test(p) &&
+        !/(probes\.enabled|MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED).*true/i.
test(composeText)) {
+      issues.push({ rule: 'C2-probe', path: 'deploy/docker-compose.deploy.yml'
, line: 0, message: `鎺㈤拡寮曠敤 /actuator/${p} 浣嗘湭澹版槑 MANAGEMENT_ENDPOINT_HEALTH_PR
OBES_ENABLED=true` });
+    }
+  }
+  return issues;
+}
+
+// ---- C3 绠＄悊璺緞鍏抽棴 ----
+
+/** 鏁忔劅绠＄悊璺緞鍓嶇紑锛堝繀椤诲湪鍙嶄唬涓叧闂級 */
+const SENSITIVE_PATHS = ['/actuator/', '/admin/'];
+
+/**
+ * 鏍￠獙 nginx 鍙嶄唬閰嶇疆鏄惁鍏抽棴浜嗘晱鎰熺鐞嗚矾寰勩€?+ * @param {string} nginxText nginx 閰嶇疆鍐
呭
+ */
+export function checkManagementClosure(nginxText) {
+  const issues = [];
+  for (const path of SENSITIVE_PATHS) {
+    // 蹇呴』瀛樺湪 location 鍧椾笖鍚?deny all 鎴?return 403
+    const locRe = new RegExp(`location\\s+${path.replace(/\//g, '\\/')}[^{]*\\
{[^}]*(deny\\s+all|return\\s+403)`, 's');
+    if (!locRe.test(nginxText)) {
+      issues.push({ rule: 'C3-mgmt-closure', path: 'deploy/nginx/zszj-server.c
onf', line: 0, message: `鍙嶅悜浠ｇ悊鏈叧闂晱鎰熺鐞嗚矾寰?${path}锛堥渶 deny all 鎴?return 403锛塦
 });
+    }
+  }
+  return issues;
+}
+// ---- C4 绔彛鏆撮湶 ----
+
+/** 鍐呴儴鏈嶅姟绔彛锛堜笉寰楀叕寮€鏆撮湶锛?*/
+const INTERNAL_PORTS = [5432, 6379, 3306, 27017, 5672, 15672];
+
+/**
+ * 鏍￠獙 compose 涓嶅叕寮€鏆撮湶鍐呴儴鏈嶅姟绔彛銆?+ * 缁戝畾 127.0.0.1 鐨勬槧灏勮涓烘湰鍦拌皟璇曞悎娉曪紝涓嶆姤銆?+ *
 @param {string} composeText docker-compose 妯℃澘鍐呭
+ */
+export function checkPortExposure(composeText) {
+  const issues = [];
+  // 鍖归厤 ports 鏄犲皠琛岋細- "HOST:CONTAINER" 鎴?- "IP:HOST:CONTAINER"
+  const portLines = [...composeText.matchAll(/-\s*["']?([^"'\n]+:\d+)["']?/g)]
;
+  for (const m of portLines) {
+    const mapping = m[1].trim();
+    const parts = mapping.split(':');
+    // 鍒ゆ柇鏄惁缁戝畾浜?loopback
+    const boundIp = parts.length >= 3 ? parts[0] : '0.0.0.0';
+    const hostPort = Number(parts.length >= 3 ? parts[1] : parts[0]);
+    if (INTERNAL_PORTS.includes(hostPort) && !/^127\./.test(boundIp)) {
+      issues.push({ rule: 'C4-port', path: 'deploy/docker-compose.deploy.yml',
 line: 0, message: `鍐呴儴鏈嶅姟绔彛 ${hostPort} 鍏紑鏆撮湶锛堢粦瀹?${boundIp}锛夛紝搴旂Щ闄ゆ垨闄愬埗涓?12
7.0.0.1` });
+    }
+  }
+  return issues;
+}
+
+// ---- C5 鐜鍙橀噺鍚堝悓鍙屽悜涓€鑷?----
+
+/**
+ * 鏍￠獙 .env.example 涓?compose 寮曠敤鐨勭幆澧冨彉閲忎笌鍚堝悓琛ㄥ弻鍚戜竴鑷淬€?+ * @param {string} env
ExampleText .env.example 鍐呭
+ * @param {string} composeText docker-compose 妯℃澘鍐呭
+ * @param {string[]} contractVars 鍚堝悓鍙橀噺鍒楄〃
+ */
+export function checkEnvContract(envExampleText, composeText, contractVars) {
+  const issues = [];
+  const contractSet = new Set(contractVars);
+
+  // 鏂瑰悜 1锛氬悎鍚屽彉閲忓繀椤诲湪 .env.example 涓嚭鐜?+  const envKeys = new Set(
+    [...envExampleText.matchAll(/^([A-Z_][A-Z0-9_]*)\s*=/gm)].map((m) => m[1])
+  );
+  for (const v of contractVars) {
+    if (!envKeys.has(v)) {
+      issues.push({ rule: 'C5-env-contract', path: 'deploy/.env.example', line
: 0, message: `鍚堝悓鍙橀噺 ${v} 鍦?.env.example 涓己澶盽 });
+    }
+  }
+
+  // 鏂瑰悜 2锛歝ompose 涓紩鐢ㄧ殑 ${VAR} 蹇呴』鍦ㄥ悎鍚岃〃涓紙闃插菇鐏靛彉閲忥級
+  const composeVars = new Set(
+    [...composeText.matchAll(/\$\{([A-Z_][A-Z0-9_]*)(?::[^}]*)?\}/g)].map((m) 
=> m[1])
+  );
+  for (const v of composeVars) {
+    if (!contractSet.has(v)) {
+      issues.push({ rule: 'C5-env-contract', path: 'deploy/docker-compose.depl
oy.yml', line: 0, message: `compose 寮曠敤浜嗗悎鍚屽鍙橀噺 ${v}锛堝菇鐏靛彉閲忥級` });
+    }
+  }
+
+  return issues;
+}
+// ---- 涓诲叆鍙ｏ細鍏ㄩ噺鎵弿 ----
+
+/** 璇诲彇 actuator exposure.include锛堜粠 application-prod.yaml 鎴?application.yaml锛
?*/
+function readActuatorInclude() {
+  const prodPath = join(root, 'services/zhongshu-core/script/config/applicatio
n-prod.yaml');
+  const mainPath = join(root, 'services/zhongshu-core/zszj-server/src/main/res
ources/application.yaml');
+  for (const p of [prodPath, mainPath]) {
+    if (!existsSync(p)) continue;
+    const text = readFileSync(p, 'utf8');
+    const m = text.match(/exposure:\s*\n\s*include:\s*(.+)/);
+    if (m) return m[1].replace(/#.*$/, '').trim();
+  }
+  return 'health';
+}
+
+function runAll() {
+  const issues = [];
+  let scanned = 0;
+
+  // C1: 绉樺瘑鎵弿锛堝叏閮ㄩ儴缃叉ā鏉匡級
+  for (const rel of DEPLOY_TARGETS) {
+    const abs = join(root, rel);
+    if (!existsSync(abs)) {
+      issues.push({ rule: 'C0-missing', path: rel, line: 0, message: `閮ㄧ讲妯℃澘鏂囦
欢涓嶅瓨鍦? ${rel}` });
+      continue;
+    }
+    scanned++;
+    const text = readFileSync(abs, 'utf8');
+    issues.push(...checkSecrets(rel, text));
+  }
+
+  // C2: 鎺㈤拡涓€鑷存€?+  const composeRel = 'services/zhongshu-core/deploy/docker-
compose.deploy.yml';
+  const composeAbs = join(root, composeRel);
+  if (existsSync(composeAbs)) {
+    const composeText = readFileSync(composeAbs, 'utf8');
+    const actuatorInclude = readActuatorInclude();
+    issues.push(...checkProbeConsistency(composeText, actuatorInclude));
+  }
+
+  // C3: 绠＄悊璺緞鍏抽棴
+  const nginxRel = 'services/zhongshu-core/deploy/nginx/zszj-server.conf';
+  const nginxAbs = join(root, nginxRel);
+  if (existsSync(nginxAbs)) {
+    issues.push(...checkManagementClosure(readFileSync(nginxAbs, 'utf8')));
+  }
+
+  // C4: 绔彛鏆撮湶
+  if (existsSync(composeAbs)) {
+    issues.push(...checkPortExposure(readFileSync(composeAbs, 'utf8')));
+  }
+
+  // C5: 鐜鍙橀噺鍚堝悓
+  const envRel = 'services/zhongshu-core/deploy/.env.example';
+  const envAbs = join(root, envRel);
+  if (existsSync(envAbs) && existsSync(composeAbs)) {
+    issues.push(...checkEnvContract(
+      readFileSync(envAbs, 'utf8'),
+      readFileSync(composeAbs, 'utf8'),
+      CONTRACT_VARS,
+    ));
+  }
+
+  return { scanned, issueCount: issues.length, issues: issues.slice(0, 50) };
+}
+
+// ---- --self-test 璐熷悜瀵圭収 ----
+
+function selfTest() {
+  // 鐢ㄥ凡鐭ヨ繚瑙勫唴瀹归┍鍔ㄥ悇 check锛岃瘉鏄庢柇瑷€鍙け璐ワ紙闈炵┖娲烇級
+  const results = [];
+  const bad = checkSecrets('test.env', 'DB_PASSWORD=hunter2');
+  results.push(['C1 璐熷悜锛堢‖缂栫爜鍙ｄ护锛?, bad.length > 0]);
+
+  const noProbe = checkProbeConsistency('services:\n  x:\n    image: y', 'heal
th');
+  results.push(['C2 璐熷悜锛堟棤 healthcheck锛?, noProbe.length > 0]);
+
+  const openNginx = checkManagementClosure('location / { proxy_pass http://x; 
}');
+  results.push(['C3 璐熷悜锛堢鐞嗚矾寰勬湭鍏抽棴锛?, openNginx.length > 0]);
+
+  const openPort = checkPortExposure('ports:\n      - "5432:5432"');
+  results.push(['C4 璐熷悜锛圖B 绔彛鍏紑锛?, openPort.length > 0]);
+
+  const ghost = checkEnvContract('A=\n', '${GHOST}', CONTRACT_VARS);
+  results.push(['C5 璐熷悜锛堝菇鐏靛彉閲忥級', ghost.length > 0]);
+
+  const allPass = results.every(([, ok]) => ok);
+  for (const [name, ok] of results) console.error(`  [${ok ? 'PASS' : 'FAIL'}]
 ${name}`);
+  return allPass;
+}
+
+// ---- CLI ----
+
+const invokedDirectly = process.argv[1] && process.argv[1].replaceAll('\\', '/
').endsWith('scripts/ops/verify-deploy-templates.mjs');
+if (invokedDirectly) {
+  try {
+    if (process.argv.includes('--self-test')) {
+      const ok = selfTest();
+      console.log(JSON.stringify({ selfTest: ok ? 'PASS' : 'FAIL' }));
+      process.exitCode = ok ? 0 : 1;
+    } else {
+      const report = runAll();
+      console.log(JSON.stringify(report, null, 2));
+      console.error(`scanned=${report.scanned} issueCount=${report.issueCount}
`);
+      process.exitCode = report.issueCount ? 1 : 0;
+    }
+  } catch (e) {
+    console.error(`deploy template check failed: ${e.message}`);
+    process.exitCode = 2;
+  }
+}
diff --git a/scripts/ops/verify-deploy-templates.test.mjs b/scripts/ops/verify-
deploy-templates.test.mjs
new file mode 100644
index 00000000..c4637cd7
--- /dev/null
+++ b/scripts/ops/verify-deploy-templates.test.mjs
@@ -0,0 +1,169 @@
+/**
+ * ZS-OPS-002.A 閮ㄧ讲妯℃澘鍙鏌ユ€ф牎楠屸€斺€旂函闈欐€佸崟娴嬨€?+ * 杩愯锛歯ode --test scripts/ops/
verify-deploy-templates.test.mjs
+ *
+ * 娴嬭瘯绛栫暐锛氱敤鍚堟垚妯℃澘鍐呭椹卞姩鍚?check 绾嚱鏁帮紝姝ｅ悜锛堝悎瑙勬ā鏉库啋0 issue锛?+ * 涓庤礋鍚戯紙杩濊妯℃澘鈫掑懡
涓搴旇鍒欙級瀵圭収锛岃瘉鏄庢牎楠岄潪绌烘礊銆?+ */
+import test from 'node:test';
+import assert from 'node:assert/strict';
+import {
+  checkSecrets,
+  checkProbeConsistency,
+  checkManagementClosure,
+  checkPortExposure,
+  checkEnvContract,
+  CONTRACT_VARS,
+  DEPLOY_TARGETS,
+} from './verify-deploy-templates.mjs';
+
+// ---- C1 绉樺瘑鎵弿 ----
+
+test('C1锛氬悎瑙勬ā鏉匡紙鍗犱綅绗?绌哄€硷級涓嶆姤 issue', () => {
+  const text = [
+    'ZSZJ_DATASOURCE_PASSWORD=${ZSZJ_DATASOURCE_PASSWORD}',
+    'ZSZJ_REDIS_PASSWORD=',
+    'JAVA_OPTS=-Xms512m -Xmx512m',
+  ].join('\n');
+  assert.deepEqual(checkSecrets('deploy/.env.example', text), []);
+});
+
+test('C1锛氱‖缂栫爜鍙ｄ护瑙﹀彂 issue', () => {
+  const text = 'MYSQL_ROOT_PASSWORD=123456\nZSZJ_DATASOURCE_PASSWORD=RealP@ssw
0rd!';
+  const issues = checkSecrets('deploy/.env.example', text);
+  assert.ok(issues.length >= 2, `搴旇嚦灏戞姤 2 鏉★紝瀹為檯 ${issues.length}`);
+  assert.ok(issues.every((i) => i.rule === 'C1-secret'));
+});
+
+test('C1锛歅EM 绉侀挜鍧楄Е鍙?issue', () => {
+  const text = '-----BEGIN RSA PRIVATE KEY-----\nMIIEow...\n-----END RSA PRIVA
TE KEY-----';
+  const issues = checkSecrets('deploy/nginx/tls/server.key', text);
+  assert.ok(issues.some((i) => i.rule === 'C1-secret' && /绉侀挜/.test(i.message)
));
+});
+
+test('C1锛氶暱鍗佸叚杩涘埗涓诧紙鈮?2锛夎Е鍙?issue', () => {
+  const text = 'VUE_APP_BAIDU_CODE=fadc1bd5db1a1d6f581df60a1807f8ab';
+  const issues = checkSecrets('deploy/.env.example', text);
+  assert.ok(issues.some((i) => i.rule === 'C1-secret' && /鍗佸叚杩涘埗/.test(i.messa
ge)));
+});
+
+test('C1锛氭敞閲婅涓殑绉樺瘑浠嶈妫€鍑猴紙闃层€屾敞閲婃帀鍗冲畨鍏ㄣ€嶄吉缁匡級', () => {
+  const text = '# PASSWORD=SuperSecret123\n# 鍘嗗彶閬楃暀锛屽嬁鍒?;
+  const issues = checkSecrets('deploy/.env.example', text);
+  assert.ok(issues.length > 0, '娉ㄩ噴琛屼腑鐨勭湡瀹炵瀵嗕篃搴旀姤鍑?);
+});
+// ---- C2 鎺㈤拡涓€鑷存€?----
+
+test('C2锛歝ompose healthcheck 璺緞涓?actuator include 涓€鑷粹啋閫氳繃', () => {
+  const compose = 'healthcheck:\n  test:\n    - "curl -f http://localhost:4808
0/actuator/health || exit 1"';
+  assert.deepEqual(checkProbeConsistency(compose, 'health'), []);
+});
+
+test('C2锛歝ompose 寮曠敤 /actuator/health/liveness 浣?actuator 鏈惎鐢?probes鈫掓姤 issue
', () => {
+  const compose = 'healthcheck:\n  test:\n    - "curl -f http://localhost:4808
0/actuator/health/liveness || exit 1"';
+  const issues = checkProbeConsistency(compose, 'health');
+  assert.ok(issues.some((i) => i.rule === 'C2-probe' && /probes/i.test(i.messa
ge)));
+});
+
+test('C2锛歝ompose 鏃?healthcheck 瀹氫箟鈫掓姤 issue锛堥儴缃叉ā鏉垮繀椤诲惈鎺㈤拡锛?, () => {
+  const compose = 'services:\n  server:\n    image: zszj-server\n    ports:\n 
     - "48080:48080"';
+  const issues = checkProbeConsistency(compose, 'health');
+  assert.ok(issues.some((i) => i.rule === 'C2-probe' && /healthcheck/i.test(i.
message)));
+});
+
+test('C2锛歛ctuator include 涓虹┖鏃?compose 浠讳綍 /actuator 鎺㈤拡閮芥姤 issue', () => {
+  const compose = 'healthcheck:\n  test:\n    - "curl -f http://localhost:4808
0/actuator/health || exit 1"';
+  const issues = checkProbeConsistency(compose, '');
+  assert.ok(issues.some((i) => i.rule === 'C2-probe'));
+});
+
+// ---- C3 绠＄悊璺緞鍏抽棴 ----
+
+test('C3锛歯ginx deny /actuator/ + /admin/ 鈫掗€氳繃', () => {
+  const nginx = [
+    'location /actuator/ { deny all; return 403; }',
+    'location /admin/ { deny all; return 403; }',
+    'location / { proxy_pass http://zszj-server:48080; }',
+  ].join('\n');
+  assert.deepEqual(checkManagementClosure(nginx), []);
+});
+
+test('C3锛歯ginx 鏈叧闂?/actuator/ 鈫掓姤 issue', () => {
+  const nginx = 'location / {\n    proxy_pass http://zszj-server:48080;\n  }';
+  const issues = checkManagementClosure(nginx);
+  assert.ok(issues.some((i) => i.rule === 'C3-mgmt-closure' && /actuator/.test
(i.message)));
+});
+
+test('C3锛歯ginx 鏈叧闂?/admin/ 鈫掓姤 issue', () => {
+  const nginx = 'location /actuator/ {\n    deny all;\n  }\nlocation / {\n    
proxy_pass http://backend;\n  }';
+  const issues = checkManagementClosure(nginx);
+  assert.ok(issues.some((i) => i.rule === 'C3-mgmt-closure' && /admin/.test(i.
message)));
+});
+// ---- C4 绔彛鏆撮湶 ----
+
+test('C4锛歝ompose 涓嶆毚闇?DB/Redis 绔彛鈫掗€氳繃', () => {
+  const compose = [
+    'services:',
+    '  postgres:',
+    '    image: postgres:16',
+    '  redis:',
+    '    image: redis:7-alpine',
+    '  server:',
+    '    ports:',
+    '      - "127.0.0.1:48080:48080"',
+  ].join('\n');
+  assert.deepEqual(checkPortExposure(compose), []);
+});
+
+test('C4锛歝ompose 鏆撮湶 5432 鍒?0.0.0.0鈫掓姤 issue', () => {
+  const compose = 'services:\n  postgres:\n    ports:\n      - "5432:5432"';
+  const issues = checkPortExposure(compose);
+  assert.ok(issues.some((i) => i.rule === 'C4-port' && /5432/.test(i.message))
);
+});
+
+test('C4锛歝ompose 鏆撮湶 6379鈫掓姤 issue', () => {
+  const compose = 'services:\n  redis:\n    ports:\n      - "6379:6379"';
+  const issues = checkPortExposure(compose);
+  assert.ok(issues.some((i) => i.rule === 'C4-port' && /6379/.test(i.message))
);
+});
+
+test('C4锛氱粦瀹?127.0.0.1 鐨?DB 绔彛涓嶆姤锛堟湰鍦拌皟璇曞悎娉曪級', () => {
+  const compose = 'services:\n  postgres:\n    ports:\n      - "127.0.0.1:5432
:5432"';
+  assert.deepEqual(checkPortExposure(compose), []);
+});
+
+// ---- C5 鐜鍙橀噺鍚堝悓鍙屽悜涓€鑷?----
+
+test('C5锛?env.example 涓?compose 寮曠敤鐨勫彉閲忓畬鍏ㄨ鐩栧悎鍚屸啋閫氳繃', () => {
+  const envExample = CONTRACT_VARS.map((v) => `${v}=`).join('\n');
+  const compose = CONTRACT_VARS.map((v) => `      - \${${v}}`).join('\n');
+  assert.deepEqual(checkEnvContract(envExample, compose, CONTRACT_VARS), []);
+});
+
+test('C5锛氬悎鍚屽彉閲忓湪 .env.example 涓己澶扁啋鎶?issue', () => {
+  const envExample = 'ZSZJ_SERVER_PORT=\n';
+  const compose = '';
+  const issues = checkEnvContract(envExample, compose, CONTRACT_VARS);
+  assert.ok(issues.some((i) => i.rule === 'C5-env-contract' && /ZSZJ_DATASOURC
E_URL/.test(i.message)));
+});
+
+test('C5锛歝ompose 寮曠敤浜嗗悎鍚屽鍙橀噺鈫掓姤 issue锛堥槻骞界伒鍙橀噺锛?, () => {
+  const envExample = CONTRACT_VARS.map((v) => `${v}=`).join('\n');
+  const compose = envExample + '\n      - ${GHOST_VAR_NOT_IN_CONTRACT}';
+  const issues = checkEnvContract(envExample, compose, CONTRACT_VARS);
+  assert.ok(issues.some((i) => i.rule === 'C5-env-contract' && /GHOST_VAR/.tes
t(i.message)));
+});
+
+test('C5锛欳ONTRACT_VARS 鍖呭惈 ZS-ENG-003 宸插畾涔夌殑鍏ㄩ儴蹇呭～椤?, () => {
+  const required = ['ZSZJ_DATASOURCE_URL', 'ZSZJ_DATASOURCE_USERNAME', 'ZSZJ_D
ATASOURCE_PASSWORD', 'ZSZJ_REDIS_HOST', 'ZSZJ_REDIS_PASSWORD'];
+  for (const v of required) assert.ok(CONTRACT_VARS.includes(v), `鍚堝悓缂?${v}`);
+});
+
+// ---- 缁撴瀯瀹屾暣鎬?----
+
+test('DEPLOY_TARGETS 闈炵┖涓旇矾寰勫潎涓?deploy/ 鍓嶇紑', () => {
+  assert.ok(DEPLOY_TARGETS.length > 0);
+  for (const t of DEPLOY_TARGETS) {
+    assert.ok(t.startsWith('services/zhongshu-core/deploy/'), `璺緞搴斾互 deploy/ 
涓烘牴: ${t}`);
+  }
+});
diff --git a/services/zhongshu-core/deploy/.env.example b/services/zhongshu-cor
e/deploy/.env.example
new file mode 100644
index 00000000..99e7966d
--- /dev/null
+++ b/services/zhongshu-core/deploy/.env.example
@@ -0,0 +1,40 @@
+# ZS-OPS-002.A 閰嶇疆娉ㄥ叆鍚堝悓锛?env.example锛?+# 鍓嶇疆锛歓S-ENG-003锛堢幆澧冮厤缃垎绂伙級銆乑S-CFG-00
1.A锛堢瀵嗛棬绂侊級
+#
+# 浣跨敤锛歝p .env.example .env && vim .env锛堝～鍏ョ湡瀹炲€煎悗 .env 涓嶅緱鎻愪氦锛?+# 鍚堝悓鍘熷垯锛?+#  
 1. 鏈枃浠朵腑鎵€鏈夊€煎潎涓哄崰浣嶇鎴栫┖锛岀姝㈠嚭鐜扮湡瀹炲嚟鎹?+#   2. 缂哄繀濉」鏃?zszj-server 搴斿惎鍔ㄥけ璐ュ苟杈撳
嚭鏄庣‘閿欒锛堥潪闈欓粯闄嶇骇锛?+#   3. 鏁忔劅椤癸紙*_PASSWORD銆?_KEY銆?_TOKEN锛変笉寰楄惤闀滃儚銆佷笉寰楀啓鏃ュ織
+
+# ---- 鏈嶅姟绔彛 ----
+# zszj-server HTTP 绔彛锛堥粯璁?48080锛屼笌 Dockerfile EXPOSE 涓€鑷达級
+ZSZJ_SERVER_PORT=48080
+
+# ---- Spring Profile ----
+# 閮ㄧ讲鐜 Profile锛坧rod/staging/test锛屽搴?application-{profile}.yaml锛?+ZSZJ_PROFI
LE=prod
+
+# ---- 鏁版嵁搴擄紙PostgreSQL锛?---
+# JDBC URL锛堟牸寮忥細jdbc:postgresql://HOST:PORT/DB?params锛?+# 瀹瑰櫒鍐呯綉缁滃湴鍧€涓?postgre
s:5432锛涘閮ㄨ闂～瀹為檯鍦板潃
+ZSZJ_DATASOURCE_URL=jdbc:postgresql://postgres:5432/zhongshu
+ZSZJ_DATASOURCE_USERNAME=
+ZSZJ_DATASOURCE_PASSWORD=
+
+# ---- Redis ----
+# 瀹瑰櫒鍐呯綉缁滃湴鍧€涓?redis:6379锛涘閮ㄨ闂～瀹為檯鍦板潃
+ZSZJ_REDIS_HOST=redis
+ZSZJ_REDIS_PORT=6379
+ZSZJ_REDIS_PASSWORD=
+
+# ---- TLS 璇佷功璺緞锛堝涓绘満璺緞锛寁olume mount 娉ㄥ叆 nginx 瀹瑰櫒锛?---
+# 璇佷功鏂囦欢绂佹鍏ョ増鏈簱锛涚敱 certbot/杩愮淮鎵嬪姩鏀剧疆
+ZSZJ_TLS_CERT_PATH=./tls/fullchain.pem
+ZSZJ_TLS_KEY_PATH=./tls/privkey.pem
+
+# ---- 鍙€夛細瀵硅薄瀛樺偍锛圸S-FILE-005.B 浜や粯鍚庤ˉ鍏咃紝鏈壒 .A 涓嶆秹鍙婏級----
+# ZSZJ_OSS_ENDPOINT=
+# ZSZJ_OSS_ACCESS_KEY=
+# ZSZJ_OSS_SECRET_KEY=
+# ZSZJ_OSS_BUCKET=
diff --git a/services/zhongshu-core/deploy/docker-compose.deploy.yml b/services
/zhongshu-core/deploy/docker-compose.deploy.yml
new file mode 100644
index 00000000..d5caae44
--- /dev/null
+++ b/services/zhongshu-core/deploy/docker-compose.deploy.yml
@@ -0,0 +1,129 @@
+# ZS-OPS-002.A 閮ㄧ讲妯℃澘鈥斺€旂嫭绔嬫祴璇曠幆澧冪紪鎺?+# 鍓嶇疆锛歓S-ENG-002锛圝DK17 鍩虹嚎锛夈€乑S-ENG-003锛
堢幆澧冮厤缃垎绂伙級銆乑S-ENG-005锛坅ctuator 鏀剁揣锛?+#
+# 浣跨敤鏂瑰紡锛?+#   cp .env.example .env   # 濉叆鐪熷疄鍊硷紙.env 宸插湪 .gitignore锛?+#   doc
ker compose -f docker-compose.deploy.yml up -d
+#   docker compose -f docker-compose.deploy.yml down  # 鎾ゅ洖
+#
+# 杈圭晫锛氭湰鏂囦欢鏄劚鏁忔ā鏉匡紝涓嶅惈浠讳綍鐪熷疄绉樺瘑銆?+# 鎵€鏈夋晱鎰熼厤缃€氳繃 ${ZSZJ_*} 鐜鍙橀噺娉ㄥ叆锛堣 .env
.example 鍚堝悓琛級銆?+# 绂佹鍦ㄦ鏂囦欢涓嚭鐜扮‖缂栫爜鍙ｄ护銆佺湡瀹炴暟鎹簱鍦板潃鎴?TLS 绉侀挜銆?+
+version: "3.9"
+
+networks:
+  zszj-internal:
+    driver: bridge
+    internal: false   # 闇€瑕佸缃戞媺闀滃儚锛涚敓浜у彲鏀逛负 internal: true
+
+services:
+  # ---- PostgreSQL锛圖-08锛氫粎 PG锛岀 MySQL锛?---
+  postgres:
+    image: postgres:16-alpine
+    container_name: zszj-postgres
+    restart: unless-stopped
+    environment:
+      POSTGRES_DB: zhongshu
+      POSTGRES_USER: ${ZSZJ_DATASOURCE_USERNAME}
+      POSTGRES_PASSWORD: ${ZSZJ_DATASOURCE_PASSWORD}
+    volumes:
+      - zszj-pgdata:/var/lib/postgresql/data
+    networks:
+      - zszj-internal
+    # 涓嶆毚闇插叕寮€绔彛锛涗粎 127.0.0.1 渚涙湰鍦拌皟璇曪紙绉婚櫎鍚庝粎瀹瑰櫒鍐呯綉鍙揪锛?+    ports:
+      - "127.0.0.1:5432:5432"
+    healthcheck:
+      test: ["CMD-SHELL", "pg_isready -U ${ZSZJ_DATASOURCE_USERNAME} -d zhongs
hu"]
+      interval: 10s
+      timeout: 5s
+      retries: 5
+
+  # ---- Redis ----
+  redis:
+    image: redis:7-alpine
+    container_name: zszj-redis
+    restart: unless-stopped
+    command: redis-server --requirepass ${ZSZJ_REDIS_PASSWORD}
+    volumes:
+      - zszj-redisdata:/data
+    networks:
+      - zszj-internal
+    # 涓嶆毚闇插叕寮€绔彛
+    ports:
+      - "127.0.0.1:6379:6379"
+    healthcheck:
+      test: ["CMD", "redis-cli", "-a", "${ZSZJ_REDIS_PASSWORD}", "ping"]
+      interval: 10s
+      timeout: 5s
+      retries: 5
+
+  # ---- zszj-server锛圫pring Boot锛孞DK17锛?---
+  zszj-server:
+    image: zszj-server:latest   # 鐢?CI 鏋勫缓锛岀姝㈠湪妯℃澘涓‖缂栫爜鐪熷疄 registry 鍦板潃
+    container_name: zszj-server
+    restart: unless-stopped
+    depends_on:
+      postgres:
+        condition: service_healthy
+      redis:
+        condition: service_healthy
+    environment:
+      SPRING_PROFILES_ACTIVE: ${ZSZJ_PROFILE}
+      SERVER_PORT: ${ZSZJ_SERVER_PORT}
+      SPRING_DATASOURCE_URL: ${ZSZJ_DATASOURCE_URL}
+      SPRING_DATASOURCE_USERNAME: ${ZSZJ_DATASOURCE_USERNAME}
+      SPRING_DATASOURCE_PASSWORD: ${ZSZJ_DATASOURCE_PASSWORD}
+      SPRING_REDIS_HOST: ${ZSZJ_REDIS_HOST}
+      SPRING_REDIS_PORT: ${ZSZJ_REDIS_PORT}
+      SPRING_REDIS_PASSWORD: ${ZSZJ_REDIS_PASSWORD}
+      # actuator 鎺㈤拡锛歓S-ENG-005 宸插皢 exposure.include 鏀剁揣涓?health
+      MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED: "true"
+      # Spring Boot Admin 瀹㈡埛绔鐢紙ZS-ENG-005锛?+      SPRING_BOOT_ADMIN_CLIENT
_ENABLED: "false"
+    networks:
+      - zszj-internal
+    # 浠呮毚闇茬粰 nginx锛堝唴閮ㄧ綉缁滐級锛屼笉缁戝畾瀹夸富鏈哄叕寮€绔彛
+    expose:
+      - "${ZSZJ_SERVER_PORT}"
+    # 鎺㈤拡杈圭晫锛歭iveness/readiness 鐢?actuator health 鎻愪緵
+    # ZS-ENG-005 宸查厤缃?management.endpoints.web.exposure.include: health
+    healthcheck:
+      test: ["CMD", "curl", "-f", "http://localhost:${ZSZJ_SERVER_PORT}/actuat
or/health/liveness"]
+      interval: 30s
+      timeout: 10s
+      retries: 3
+      start_period: 60s   # JVM 鍐峰惎鍔ㄥ闄愭湡
+
+  # ---- Nginx锛圱LS 缁堢粨 + 鍙嶅悜浠ｇ悊锛?---
+  nginx:
+    image: nginx:1.27-alpine
+    container_name: zszj-nginx
+    restart: unless-stopped
+    depends_on:
+      zszj-server:
+        condition: service_healthy
+    ports:
+      # 浠呮毚闇?HTTPS 鍒板涓绘満锛汬TTP 80 鐢ㄤ簬 Let's Encrypt 楠岃瘉锛屾寜闇€寮€鏀?+      - "443:
443"
+      # - "80:80"   # TLS 璇佷功鐢宠鏃朵复鏃跺紑鍚紝鑾峰緱璇佷功鍚庢敞閲婃帀
+    volumes:
+      - ./nginx/zszj-server.conf:/etc/nginx/conf.d/default.conf:ro
+      # TLS 璇佷功鎸傝浇锛堣矾寰勪负瀹瑰櫒鍐呰矾寰勶紝瀹夸富鏈鸿矾寰勫湪 .env 涓厤缃級
+      # 璇佷功鏂囦欢绂佹鍏ラ暅鍍忥紱閫氳繃 volume mount 娉ㄥ叆
+      - ${ZSZJ_TLS_CERT_PATH:-./tls/fullchain.pem}:/etc/nginx/tls/fullchain.pe
m:ro
+      - ${ZSZJ_TLS_KEY_PATH:-./tls/privkey.pem}:/etc/nginx/tls/privkey.pem:ro
+    networks:
+      - zszj-internal
+    healthcheck:
+      test: ["CMD", "curl", "-f", "http://localhost/nginx-health"]
+      interval: 30s
+      timeout: 5s
+      retries: 3
+
+volumes:
+  zszj-pgdata:
+    driver: local
+  zszj-redisdata:
+    driver: local
diff --git a/services/zhongshu-core/deploy/nginx/zszj-server.conf b/services/zh
ongshu-core/deploy/nginx/zszj-server.conf
new file mode 100644
index 00000000..6edee30b
--- /dev/null
+++ b/services/zhongshu-core/deploy/nginx/zszj-server.conf
@@ -0,0 +1,74 @@
+# ZS-OPS-002.A 鍙嶅悜浠ｇ悊閰嶇疆鈥斺€擳LS 缁堢粨 + 绠＄悊璺緞鍏抽棴
+# 鍓嶇疆锛歓S-ENG-005锛坅ctuator 鏀剁揣锛宔xposure.include: health锛?+#
+# 杈圭晫锛?+#   1. /actuator/* 鍜?/admin/* 蹇呴』鍦ㄦ澶勫叧闂紙deny all锛?+#   2. 涓嶅緱灏嗗唴閮ㄧ鐞
嗙鍙ｏ紙postgres/redis锛夋毚闇插埌鍏綉
+#   3. TLS 璇佷功閫氳繃 volume mount 娉ㄥ叆锛岀姝㈢‖缂栫爜绉侀挜鍐呭
+
+# ---- HTTP 鈫?HTTPS 閲嶅畾鍚戯紙鎸夐渶鍚敤锛?---
+# server {
+#     listen 80;
+#     server_name _;
+#     # Let's Encrypt HTTP-01 楠岃瘉璺緞锛堢敵璇疯瘉涔︽椂涓存椂鍚敤锛?+#     location /.well-kn
own/acme-challenge/ { root /var/www/certbot; }
+#     location / { return 301 https://$host$request_uri; }
+# }
+
+# ---- HTTPS 涓荤珯 ----
+server {
+    listen 443 ssl http2;
+    server_name _;
+
+    # TLS 璇佷功锛堢敱 volume mount 浠庡涓绘満娉ㄥ叆锛岃矾寰勮 .env.example锛?+    ssl_certifica
te     /etc/nginx/tls/fullchain.pem;
+    ssl_certificate_key /etc/nginx/tls/privkey.pem;
+    ssl_protocols       TLSv1.2 TLSv1.3;
+    ssl_ciphers         ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA
256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384;
+    ssl_prefer_server_ciphers off;
+    ssl_session_cache   shared:SSL:10m;
+    ssl_session_timeout 10m;
+
+    # 瀹夊叏鍝嶅簲澶?+    add_header Strict-Transport-Security "max-age=63072000; inc
ludeSubDomains" always;
+    add_header X-Content-Type-Options "nosniff" always;
+    add_header X-Frame-Options "SAMEORIGIN" always;
+
+    # ---- 鏁忔劅绠＄悊璺緞鍏抽棴锛圸S-ENG-005 + ZS-OPS-002.A锛?---
+    # actuator 绔偣锛歓S-ENG-005 宸插皢 exposure.include 鏀剁揣涓?health锛?+    # 浣嗗嵆渚?he
alth 绔偣涔熶笉搴斿澶栨毚闇诧紙浠呬緵瀹瑰櫒鍐?healthcheck 璋冪敤锛?+    location /actuator/ {
+        deny all;
+        return 403;
+    }
+
+    # admin 绠＄悊鍚庡彴璺緞锛氬唴閮ㄨ繍缁翠笓鐢紝涓嶅澶?+    location /admin/ {
+        deny all;
+        return 403;
+    }
+
+    # ---- nginx 鑷韩鍋ュ悍妫€鏌ワ紙compose healthcheck 璋冪敤锛?---
+    location /nginx-health {
+        access_log off;
+        return 200 "ok\n";
+        add_header Content-Type text/plain;
+    }
+
+    # ---- 涓氬姟娴侀噺鍙嶅悜浠ｇ悊 ----
+    location / {
+        proxy_pass http://zszj-server:48080;
+        proxy_set_header Host $host;
+        proxy_set_header X-Real-IP $remote_addr;
+        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
+        proxy_set_header X-Forwarded-Proto $scheme;
+        proxy_connect_timeout 10s;
+        proxy_read_timeout 60s;
+        proxy_send_timeout 60s;
+        # 绂佹浠ｇ悊璁块棶鍐呴儴绠＄悊绔彛
+        # postgres:5432 / redis:6379 浠呭湪 zszj-internal 缃戠粶鍐呭彲杈撅紝姝ゅ涓嶈浆鍙?+    }
+
+    # ---- 瀹㈡埛绔姹備綋闄愬埗锛堥槻澶ф枃浠舵敾鍑伙級----
+    client_max_body_size 50m;
+}
diff --git a/services/zhongshu-core/docs/閮ㄧ讲妯℃澘涓庢帰閽堣竟鐣?md b/services/zhongshu-c
ore/docs/閮ㄧ讲妯℃澘涓庢帰閽堣竟鐣?md
new file mode 100644
index 00000000..a0e3e852
--- /dev/null
+++ b/services/zhongshu-core/docs/閮ㄧ讲妯℃澘涓庢帰閽堣竟鐣?md
@@ -0,0 +1,222 @@
+# 閮ㄧ讲妯℃澘涓庢帰閽堣竟鐣岋紙ZS-OPS-002.A锛?+
+> 缂栧埗鏃ユ湡锛?026-09-15銆傛湰鏂囦欢鏄?ZS-OPS-002.A 鎵规浜や粯鐗╋紝鎻忚堪鑴辨晱閮ㄧ讲妯℃澘鐨勪娇鐢ㄦ柟寮忋€?+> 閰嶇疆娉
ㄥ叆鍚堝悓銆佹帰閽堣竟鐣屽拰 TLS/鍙嶅悜浠ｇ悊绾︽潫銆?+> 鍓嶇疆浜や粯锛歓S-ENG-002锛圝DK17 鍩虹嚎锛夈€乑S-ENG-003锛堢幆澧冮厤
缃垎绂伙級銆乑S-ENG-005锛坅ctuator 鏀剁揣锛夈€?+> **鏈枃浠朵笉澹扮О鐢熶骇鍙敤**锛涢€€鍑烘潯浠朵粎涓恒€屾ā鏉垮彲瀹℃煡銆佹
晱鎰熺鐞嗚矾寰勫叧闂€嶃€?+
+---
+
+## 1. 浜や粯鐗╂竻鍗?+
+| 璺緞 | 绫诲瀷 | 璇存槑 |
+|---|---|---|
+| `services/zhongshu-core/deploy/docker-compose.deploy.yml` | 缂栨帓妯℃澘 | Postgre
SQL 16 + Redis 7 + zszj-server + Nginx锛涜劚鏁忥紝鏃犵‖缂栫爜鍑嵁 |
+| `services/zhongshu-core/deploy/.env.example` | 閰嶇疆鍚堝悓 | 鎵€鏈夊繀濉幆澧冨彉閲忓崰浣嶇锛沗.
env` 涓嶅緱鍏ョ増鏈簱 |
+| `services/zhongshu-core/deploy/nginx/zszj-server.conf` | 鍙嶄唬閰嶇疆 | TLS 缁堢粨 + 
鏁忔劅璺緞鍏抽棴锛?actuator/ + /admin/ deny all锛?|
+| `scripts/ops/verify-deploy-templates.mjs` | 鍙鏌ユ€ф牎楠?| C1鈥揅5 浜旈」闈欐€佹牎楠岋紱`--
self-test` 璐熷悜瀵圭収 |
+| `scripts/ops/verify-deploy-templates.test.mjs` | 鍗曟祴 | 21 鐢ㄤ緥锛宍node --test` 
椹卞姩 |
+
+---
+
+## 2. 鐙珛娴嬭瘯鐜閮ㄧ讲姝ラ
+
+### 2.1 鍓嶆彁
+
+- Docker Engine 鈮?24锛孌ocker Compose V2锛坄docker compose` 瀛愬懡浠わ級
+- 宸叉瀯寤?`zszj-server:latest` 闀滃儚锛堟垨鍙闂殑绉佹湁 registry锛屽湴鍧€濉叆 compose锛?+- TLS 璇
佷功锛堣嚜绛惧悕鎴?Let's Encrypt锛夋斁缃簬 `services/zhongshu-core/deploy/tls/` 鐩綍
+
+### 2.2 閮ㄧ讲
+
+```bash
+cd services/zhongshu-core/deploy
+
+# 1. 澶嶅埗閰嶇疆鍚堝悓妯℃澘
+cp .env.example .env
+
+# 2. 濉叆鐪熷疄鍊硷紙鏁版嵁搴撳彛浠ゃ€丷edis 鍙ｄ护绛夛級
+#    .env 宸插湪 .gitignore 涓紝绂佹鎻愪氦
+vim .env
+
+# 3. 鍚姩锛堝悗鍙帮級
+docker compose -f docker-compose.deploy.yml up -d
+
+# 4. 绛夊緟鍋ュ悍妫€鏌ラ€氳繃锛堢害 60鈥?0s锛?+docker compose -f docker-compose.deploy.yml ps
+```
+
+### 2.3 鎾ゅ洖
+
+```bash
+cd services/zhongshu-core/deploy
+
+# 鍋滄骞跺垹闄ゅ鍣紙淇濈暀鏁版嵁鍗凤級
+docker compose -f docker-compose.deploy.yml down
+
+# 瀹屽叏娓呴櫎锛堝惈鏁版嵁鍗封€斺€斺殸 浼氬垹闄ゆ暟鎹簱鏁版嵁锛?+docker compose -f docker-compose.deploy.y
ml down -v
+```
+
+---
+
+## 3. 閰嶇疆娉ㄥ叆鍚堝悓
+
+### 3.1 鍚堝悓鍙橀噺锛堝繀濉級
+
+鎵€鏈夊彉閲忓畾涔夎 `.env.example`锛涚己浠讳竴蹇呭～椤规椂 `zszj-server` 搴斿惎鍔ㄥけ璐ュ苟杈撳嚭鏄庣‘閿欒锛屼笉寰楅潤榛
橀檷绾с€?+
+| 鍙橀噺鍚?| 璇存槑 | 鏉ユ簮 |
+|---|---|---|
+| `ZSZJ_SERVER_PORT` | HTTP 绔彛锛堥粯璁?48080锛屼笌 Dockerfile EXPOSE 涓€鑷达級 | ENG-003
 |
+| `ZSZJ_PROFILE` | Spring Profile锛坧rod/staging/test锛?| ENG-003 |
+| `ZSZJ_DATASOURCE_URL` | JDBC URL锛屾牸寮?`jdbc:postgresql://HOST:PORT/DB` | ENG-
003 |
+| `ZSZJ_DATASOURCE_USERNAME` | 搴旂敤璐﹀彿锛坺hongshu_app锛岄潪 owner锛?| DB-002 |
+| `ZSZJ_DATASOURCE_PASSWORD` | 搴旂敤璐﹀彿鍙ｄ护锛岀瀵嗙鐞嗘敞鍏?| CFG-001.A |
+| `ZSZJ_REDIS_HOST` | Redis 涓绘満锛堝鍣ㄥ唴濉?`redis`锛?| ENG-003 |
+| `ZSZJ_REDIS_PORT` | Redis 绔彛锛堥粯璁?6379锛?| ENG-003 |
+| `ZSZJ_REDIS_PASSWORD` | Redis 鍙ｄ护锛岀瀵嗙鐞嗘敞鍏?| CFG-001.A |
+| `ZSZJ_TLS_CERT_PATH` | TLS 璇佷功瀹夸富鏈鸿矾寰勶紙volume mount 娉ㄥ叆 nginx锛?| 鏈壒鏂板 |
+| `ZSZJ_TLS_KEY_PATH` | TLS 绉侀挜瀹夸富鏈鸿矾寰勶紙volume mount 娉ㄥ叆 nginx锛?| 鏈壒鏂板 |
+
+### 3.2 涓?ZS-CFG-001.A 鐨勫叧绯?+
+鏈厤缃悎鍚屾槸 ZS-CFG-001.A銆岄儴缃插悎鍚屻€嶇殑鍏蜂綋鍖栧疄鐜帮紙妯℃澘灞傦級锛岀瀵嗘壂鎻忥紙`scripts/cfg/verify-c
onfig-secrets.mjs`锛変笌鏈壒鏍￠獙鑴氭湰锛坄scripts/ops/verify-deploy-templates.mjs` C1 椤癸級
浣跨敤鐩稿悓鍒ゅ畾鍙ｅ緞锛屼笉閲嶅璁拌处銆?+
+### 3.3 鏁忔劅椤圭孩绾?+
+- `*_PASSWORD`銆乣*_KEY`銆乣*_TOKEN` 绫诲€硷細绂佹鍑虹幇鍦ㄤ换浣曞凡鎻愪氦鏂囦欢涓?+- TLS 绉侀挜鍐呭锛氱姝㈠祵鍏
?compose 鎴?nginx.conf锛屽繀椤婚€氳繃 volume mount 娉ㄥ叆
+- `.env` 鏂囦欢锛氱姝㈡彁浜わ紙宸插湪 `.gitignore`锛?+
+---
+
+## 4. 鎺㈤拡杈圭晫
+
+### 4.1 actuator 鏆撮湶锛圸S-ENG-005 宸叉敹绱э級
+
+`application.yaml`锛堟墍鏈?Profile锛変腑锛?+```yaml
+management:
+  endpoints:
+    web:
+      base-path: /actuator
+      exposure:
+        include: health    # 浠?health锛屼笉鍚?env/beans/configprops 绛夋晱鎰熺鐐?+```
+
+### 4.2 Docker healthcheck锛堝鍣ㄥ唴鎺㈤拡锛?+
+compose 涓?`zszj-server` 鐨?healthcheck 璋冪敤 `/actuator/health/liveness`锛?+```yam
l
+healthcheck:
+  test: ["CMD", "curl", "-f", "http://localhost:${ZSZJ_SERVER_PORT}/actuator/h
ealth/liveness"]
+  interval: 30s
+  timeout: 10s
+  retries: 3
+  start_period: 60s   # JVM 鍐峰惎鍔ㄥ闄愭湡锛岄伩鍏嶈鎶?unhealthy
+```
+
+`MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED=true` 閫氳繃 compose 鐜鍙橀噺娉ㄥ叆锛屾縺娲?liv
eness/readiness 瀛愯矾寰勩€?+
+### 4.3 鎺㈤拡涓嶅鍏綉鏆撮湶
+
+Nginx 鍦?TLS 缁堢粨灞傚叧闂簡 `/actuator/` 璺緞锛坄deny all; return 403;`锛夛紝鍏綉鏃犳硶璁块棶浠讳綍 
actuator 绔偣銆傛帰閽堣皟鐢ㄤ粎鍙戠敓鍦?Docker 鍐呴儴缃戠粶锛坙ocalhost healthcheck锛夈€?+
+### 4.4 渚濊禆涓嶅彲鐢ㄦ椂鐨勬槑纭け璐?+
+| 渚濊禆 | 涓嶅彲鐢ㄦ椂琛屼负 | 鍙娴嬩俊鍙?|
+|---|---|---|
+| PostgreSQL | Flyway 杩佺Щ澶辫触锛屽鍣ㄥ惎鍔ㄤ腑姝紝`depends_on: condition: service_health
y` 闃绘 zszj-server 鍚姩 | compose ps 鏄剧ず postgres `unhealthy`锛泎szj-server 澶勪簬 `c
reated` 鐘舵€?|
+| Redis | Spring Boot RedisHealthIndicator 鎶?DOWN锛沨ealthcheck 澶辫触鍚?Docker 閲嶅惎瀹
瑰櫒 | `/actuator/health` 杩斿洖 `{"status":"DOWN"}` |
+| TLS 璇佷功缂哄け | Nginx 鍚姩澶辫触锛坄cannot load certificate`锛夛紝瀹瑰櫒绔嬪嵆閫€鍑?| `docker lo
gs zszj-nginx` 鏄剧ず閿欒 |
+
+---
+
+## 5. TLS / 鍙嶅悜浠ｇ悊杈圭晫
+
+### 5.1 TLS 缁堢粨鏋舵瀯
+
+```
+瀹㈡埛绔?鈫?:443 (nginx, TLS) 鈫?zszj-internal 缃戠粶 鈫?zszj-server:48080 (HTTP)
+```
+
+zszj-server 涓嶇洿鎺ユ毚闇插埌鍏綉锛涘叕缃戞祦閲忓繀椤荤粡杩?nginx TLS 缁堢粨銆?+
+### 5.2 璇佷功绠＄悊
+
+- 璇佷功鏂囦欢锛坄fullchain.pem`銆乣privkey.pem`锛夋斁缃簬瀹夸富鏈?`deploy/tls/` 鐩綍
+- 閫氳繃 docker-compose volume mount锛坄:ro`锛夋敞鍏?nginx 瀹瑰櫒
+- 璇佷功璺緞閫氳繃 `ZSZJ_TLS_CERT_PATH` / `ZSZJ_TLS_KEY_PATH` 鐜鍙橀噺閰嶇疆
+- 绂佹灏嗚瘉涔︽枃浠舵彁浜ゅ埌鐗堟湰搴擄紙`tls/*.pem` 宸插湪 `.gitignore`锛?+
+### 5.3 鏁忔劅绠＄悊璺緞鍏抽棴
+
+nginx.conf 涓細
+```nginx
+location /actuator/ { deny all; return 403; }
+location /admin/    { deny all; return 403; }
+```
+
+鍙鏌ユ€ф牎楠岋紙C3 椤癸級鑷姩楠岃瘉杩欎袱鏉¤鍒欏瓨鍦ㄣ€?+
+---
+
+## 6. 鍙鏌ユ€ф牎楠岃鏄?+
+### 6.1 鏍￠獙椤?+
+| 缂栧彿 | 瑙勫垯 | 璇存槑 |
+|---|---|---|
+| C1 | 妯℃澘鏃犵湡瀹炵瀵?| 妫€娴嬬‖缂栫爜鍙ｄ护銆丳EM 绉侀挜鍧椼€侀暱鍗佸叚杩涘埗涓诧紙鈮?2 瀛楃锛?|
+| C2 | 鎺㈤拡璺緞涓?actuator include 涓€鑷?| compose healthcheck 寮曠敤鐨?/actuator/ 璺緞蹇
呴』鍦?exposure.include 涓?|
+| C3 | 鏁忔劅绠＄悊璺緞鍦ㄥ弽浠ｄ腑鍏抽棴 | nginx.conf 蹇呴』鍚?deny /actuator/ 鍜?/admin/ |
+| C4 | 鍐呴儴绔彛涓嶅叕寮€鏆撮湶 | postgres(5432)/redis(6379) 绛夌鍙ｄ笉寰楃粦瀹?0.0.0.0 |
+| C5 | 鐜鍙橀噺鍚堝悓鍙屽悜涓€鑷?| .env.example 蹇呴』鍚叏閮ㄥ悎鍚屽彉閲忥紱compose 寮曠敤鐨勫彉閲忓繀椤诲湪鍚堝悓琛ㄤ
腑 |
+
+### 6.2 杩愯鏂瑰紡
+
+```bash
+# 瀵瑰綋鍓嶆爲鍏ㄩ噺鎵弿锛堥€€鍑虹爜 0=閫氳繃锛?=鏈?issue锛?+node scripts/ops/verify-deploy-templat
es.mjs
+
+# 璐熷悜瀵圭収锛堣瘉鏄庢牎楠岄潪绌烘礊锛?+node scripts/ops/verify-deploy-templates.mjs --self-tes
t
+
+# 鍗曟祴锛?1 鐢ㄤ緥锛?+node --test scripts/ops/verify-deploy-templates.test.mjs
+```
+
+---
+
+## 7. 楠屾敹杈圭晫澹版槑锛堝瀹炴爣娉級
+
+### 7.1 鏈壒锛圸S-OPS-002.A锛夊凡瀹屾垚
+
+- 鉁?鑴辨晱閮ㄧ讲妯℃澘鍙鏌ワ紙docker-compose.deploy.yml + .env.example + nginx/zszj-serve
r.conf锛?+- 鉁?鏁忔劅绠＄悊璺緞锛?actuator/銆?admin/锛夊湪鍙嶄唬閰嶇疆涓叧闂?+- 鉁?鎺㈤拡杈圭晫澹版槑锛坙iveness 
healthcheck + actuator include: health 涓€鑷存€э級
+- 鉁?TLS 缁堢粨閰嶇疆妯℃澘锛坣ginx + volume mount 璇佷功娉ㄥ叆锛?+- 鉁?閰嶇疆娉ㄥ叆鍚堝悓锛圕ONTRACT_VARS 涓?
.env.example 鍙屽悜涓€鑷达級
+- 鉁?鍙鏌ユ€ц嚜鍔ㄦ牎楠岋紙verify-deploy-templates.mjs锛孋1鈥揅5锛?-self-test 璐熷悜瀵圭収锛?+- 鉁?鐙
珛娴嬭瘯鐜閮ㄧ讲/鎾ゅ洖姝ラ璇存槑锛堟湰鏂囨。绗?2 鑺傦級
+- 鉁?渚濊禆涓嶅彲鐢ㄦ椂鐨勬槑纭け璐ヨ〃鐜帮紙鏈枃妗ｇ 4.4 鑺傦級
+
+### 7.2 鏈壒鏄庣‘涓嶅仛锛堝睘 .B/.C 鎵规锛?+
+- 鉂?**鐪熷疄涓氬姟鍚姩鑱旈獙**锛氬睘 B02/B03 鎵规锛屾湰鎵逛粎鎻愪緵妯℃澘锛屼笉鍦ㄧ湡瀹炵幆澧冨惎鍔ㄦ湇鍔￠獙璇?+- 鉂?**鐩戞祴鍛
婅钀藉湴**锛堟暟鎹簱/Redis/瀵硅薄瀛樺偍/闃熷垪/澶辫触浠诲姟鐨勫疄鏃跺憡璀︼級锛氬睘 ZS-OPS-002.B锛屽墠缃?ZS-DB-005 + 
ZS-JOB-004
+- 鉂?**澶囦唤鎭㈠婕旂粌涓庡疄娴?RPO/RTO**锛氬睘 ZS-OPS-002.C锛屽墠缃?ZS-FILE-005.B锛圔11 鎵规锛?+- 鉂?
**鎭㈠鍒版柊闅旂鐜鍚庢暟鎹?闄勪欢/浜嬩欢涓€鑷存€ч獙璇?*锛氬睘 .C 鎵规
+- 鉂?**瀹炴祴鎭㈠鑰楁椂鍜屼涪澶辩獥鍙ｈ緭鍑?*锛氬睘 .C 鎵规锛屾湰鎵逛笉铏氭姤
+
+---
+
+## 8. 涓庢棦鏈夎剼鏈殑鍏崇郴
+
+| 鑴氭湰 | 鍏崇郴 |
+|---|---|
+| `scripts/cfg/verify-config-secrets.mjs`锛圙9锛?| C1 绉樺瘑鎵弿澶嶇敤鍏跺垽瀹氬彛寰勶紙SECRET_KE
Y_RE + PLACEHOLDER_RE + SAFE_LITERALS_RE锛夛紝涓嶉噸澶嶈璐?|
+| `scripts/ops/run-local-gates.mjs` | 鏈壒鏂板鏍￠獙鑴氭湰灏氭湭鎺ュ叆闂ㄧ鑱氬悎锛堥渶涓讳細璇濅覆琛屽喅瀹氭槸鍚
︽帴绾匡級 |
+| `services/zhongshu-core/script/docker/docker-compose.yml` | legacy ruoyi-vue
-pro 閬楃暀锛屽惈纭紪鐮佸嚟鎹紝鏈壒妯℃澘鏄浛浠ｅ搧锛屼笉淇敼鏃ф枃浠?|
+| `services/zhongshu-core/script/shell/deploy.sh` | 鏃㈡湁閮ㄧ讲鑴氭湰锛屾湰鎵规ā鏉挎槸澹版槑寮忔浛浠ｏ
紝涓よ€呭彲骞跺瓨锛屼笉淇敼鏃ф枃浠?|

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "git 
-c core.quotePath=false ls-files '*AGENTS*' '*CLAUDE*' '*GEMINI*' '*SKILL.md' '
*config.toml'; Get-ChildItem -Force -ErrorAction SilentlyContinue .codex,.agent
s,"'$env:USERPROFILE'"\\.codex -File | Select-Object FullName; Get-Content .git
ignore; Get-Content services/zhongshu-core/zszj-server/Dockerfile; Get-Content 
services/zhongshu-core/zszj-server/src/main/resources/application.yaml; Get-Con
tent services/zhongshu-core/script/config/application-prod.yaml" in E:\浼楀涔嬪AI
璧嬭兘骞冲彴搴曞骇\.wt\zszj-wt-ops-002-a
 succeeded in 80ms:

FullName                                                                       
                               
--------                                                                       
                               
C:\Users\Administrator\.codex\..codex-global-state.json.tmp-1788962413720-1cf6f
f6b-f9a0-4106-a52d-7aefed7c125d
C:\Users\Administrator\.codex\..codex-global-state.json.tmp-1789035993448-60472
157-ef54-4d1a-884d-13d58b7796bd
C:\Users\Administrator\.codex\..codex-global-state.json.tmp-1789260507888-05508
f14-b2ad-4c9e-a4b6-7cf14b665ac8
C:\Users\Administrator\.codex\.codex-global-state.json                         
                               
C:\Users\Administrator\.codex\.codex-global-state.json.bak                     
                               
C:\Users\Administrator\.codex\.sandbox_migration                               
                               
C:\Users\Administrator\.codex\AGENTS.md                                        
                               
C:\Users\Administrator\.codex\auth.json                                        
                               
C:\Users\Administrator\.codex\cap_sid                                          
                               
C:\Users\Administrator\.codex\chrome-native-hosts-v2.json                      
                               
C:\Users\Administrator\.codex\config.toml                                      
                               
C:\Users\Administrator\.codex\goals_1.sqlite                                   
                               
C:\Users\Administrator\.codex\goals_1.sqlite-shm                               
                               
C:\Users\Administrator\.codex\goals_1.sqlite-wal                               
                               
C:\Users\Administrator\.codex\installation_id                                  
                               
C:\Users\Administrator\.codex\keybindings.json                                 
                               
C:\Users\Administrator\.codex\logs_2.sqlite                                    
                               
C:\Users\Administrator\.codex\logs_2.sqlite-shm                                
                               
C:\Users\Administrator\.codex\logs_2.sqlite-wal                                
                               
C:\Users\Administrator\.codex\memories_1.sqlite                                
                               
C:\Users\Administrator\.codex\memories_1.sqlite-shm                            
                               
C:\Users\Administrator\.codex\memories_1.sqlite-wal                            
                               
C:\Users\Administrator\.codex\models_cache.json                                
                               
C:\Users\Administrator\.codex\queue_1.sqlite                                   
                               
C:\Users\Administrator\.codex\queue_1.sqlite-shm                               
                               
C:\Users\Administrator\.codex\queue_1.sqlite-wal                               
                               
C:\Users\Administrator\.codex\session_index.jsonl                              
                               
C:\Users\Administrator\.codex\state_5.sqlite                                   
                               
C:\Users\Administrator\.codex\state_5.sqlite-shm                               
                               
C:\Users\Administrator\.codex\state_5.sqlite-wal                               
                               
C:\Users\Administrator\.codex\thread_history_1.sqlite                          
                               
C:\Users\Administrator\.codex\thread_history_1.sqlite-shm                      
                               
C:\Users\Administrator\.codex\thread_history_1.sqlite-wal                      
                               
C:\Users\Administrator\.codex\transcription-history.jsonl                      
                               
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
!services/zhongshu-core/deploy/.env.example

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

# 濮濄儵顎冮埗?git worktree 楠炴儼顢戠紓鏍ㄥ笓閿涙艾婀銉ょ稊閸栧搫鍞村铏规畱闂呮梻顬囬弽鎴濐啇閸ｎ煉绱?-wt-root <repo>/
.wt閿涘鈧?# 閸?worktree 閸愬懎顔愰悪顒傜彌鐠虹喕閲滈敍灞筋啇閸ｃ劎娲拌ぐ鏇€忚箛鐣屾殣閿涘奔浜掔紒瀛樺瘮閵嗗奔瀵岄弽?status 楠炴彃
鍣ｉ妴宥囨畱闂呮梻顬囨稉宥呭綁闁插繈鈧?.wt/

# outputs/ 瀹搞儰缍旈惀鏇℃姉閺佺繝缍嬮梾鏃傤瀲閿涙矮澶嶉弮鑸垫）韫囨绱?.log閿涘鈧恭odex 閸樼喎顫愮拠鍕吀 JSON/TXT閵?#
 娑撯偓濞嗏剝鈧?patch_*.py閵嗕购eview-* 闂呮梻顬囬崜顖涙拱閵嗕椒鎹㈤崝陇绻嶇悰灞炬閹躲儱鎲℃稉?3D 濡€崇€风挧鍕獓缁?# 閸у洤
鐫樻禍搴㈡拱閸︽澘浼愭担婊€楠囬悧鈺嬬礉娑撳秴鍙嗘惔鎿勭幢閺夊啫鈻夌拠鍕吀婢跺嫮鐤嗛弬鍥ㄣ€傝ぐ鎺撱€傜捄顖氱窞娑?docs/reviews/閵?outpu
ts/
## AdoptOpenJDK 閸嬫粍顒涢崣鎴濈 OpenJDK 娴滃矁绻橀崚璁圭礉閼?Eclipse Temurin 閺勵垰鐣犻惃鍕娴奸潻绱濋幓鎰返
閺囨潙銈介惃鍕旂€规碍鈧?## 閹扮喕闃挎径宥嗘＊閺嶇宕ユ竟顐ゆ畱瀵ら缚顔呴敍浣轰紗鐎涙劕鎽滈敍宀€澧伴惃顕嗙磼
## ZS-ENG-002閿涙俺绻嶇悰宀勬殔閸嶅繋绗岄弸鍕紦閸╄櫣鍤庣€靛綊缍?JDK 17閿涘湒-02 瀹歌尙鈥樼拋?JDK 17 缂佸瓨濮㈢痪鍖＄礉Ma
ven enforcer 瀵搫鍩?[17,18)閿?FROM eclipse-temurin:17-jre

## 閸掓稑缂撻惄顔肩秿閿涘苯鑻熸担璺ㄦ暏鐎瑰啩缍旀稉鍝勪紣娴ｆ粎娲拌ぐ?RUN mkdir -p /zszj-server
WORKDIR /zszj-server
## 鐏忓棗鎮楃粩顖炪€嶉惄顔炬畱 Jar 閺傚洣娆㈤敍灞筋槻閸掕泛鍩岄梹婊冨剼娑?COPY ./target/zszj-server.jar app.jar

## 鐠佸墽鐤?TZ 閺冭泛灏?
ENV TZ=Asia/Shanghai
## 鐠佸墽鐤?JAVA_OPTS 閻滎垰顣ㄩ崣姗€鍣洪敍灞藉讲闁俺绻?docker run -e "JAVA_OPTS=" 鏉╂稖顢戠憰鍡欐磰
ENV JAVA_OPTS="-Xms512m -Xmx512m -Djava.security.egd=file:/dev/./urandom"

## 鎼存梻鏁ら崣鍌涙殶
ENV ARGS=""

## 閺嗘挳婀堕崥搴ｎ伂妞ゅ湱娲伴惃?48080 缁旑垰褰?
EXPOSE 48080

## 閸氼垰濮╅崥搴ｎ伂妞ゅ湱娲?
CMD java ${JAVA_OPTS} -jar app.jar $ARGS
spring:
  application:
    name: zszj-server

  profiles:
    active: local

  main:
    allow-circular-references: true # 閸忎浇顔忓顏嗗箚娓氭繆绂嗛敍灞芥礈娑撴椽銆嶉惄顔芥Ц娑撳鐪伴弸鑸电€敍灞炬￥
濞夋洟浼╅崗宥堢箹娑擃亝鍎忛崘鐐光偓?
  # Servlet 闁板秶鐤?
  servlet:
    # 閺傚洣娆㈡稉濠佺炊閻╃鍙ч柊宥囩枂妞?    multipart:
      max-file-size: 16MB # 閸楁洑閲滈弬鍥︽婢堆冪毈
      max-request-size: 32MB # 鐠佸墽鐤嗛幀璁崇瑐娴肩姷娈戦弬鍥︽婢堆冪毈

  # Jackson 闁板秶鐤嗘い?  jackson:
    serialization:
      write-dates-as-timestamps: true # 鐠佸墽鐤?Date 閻ㄥ嫭鐗稿蹇ョ礉娴ｈ法鏁ら弮鍫曟？閹?      wri
te-date-timestamps-as-nanoseconds: false # 鐠佸墽鐤嗘稉宥勫▏閻?nanoseconds 閻ㄥ嫭鐗稿蹇嬧偓鍌欑伐婵
″倽顕?1611460870.401閿涘矁鈧本妲搁惄瀛樺复 1611460870401
      write-durations-as-timestamps: true # 鐠佸墽鐤?Duration 閻ㄥ嫭鐗稿蹇ョ礉娴ｈ法鏁ら弮鍫曟？閹? 
     fail-on-empty-beans: false # 閸忎浇顔忔惔蹇撳灙閸栨牗妫ょ仦鐐粹偓褏娈?Bean

  # Cache 闁板秶鐤嗘い?  cache:
    type: REDIS
    redis:
      time-to-live: 1h # 鐠佸墽鐤嗘潻鍥ㄦ埂閺冨爼妫挎稉?1 鐏忓繑妞?

server:
  servlet:
    encoding:
      enabled: true
      charset: UTF-8 # 韫囧懘銆忕拋鍓х枂 UTF-8閿涘矂浼╅崗?WebFlux 濞翠礁绱℃潻鏂挎礀閿涘湏I 閸︾儤娅欓敍澶夌窗娑旇京
鐖滈梻顕€顣?
      force: true

--- #################### 閹恒儱褰涢弬鍥ㄣ€傞柊宥囩枂 ####################

springdoc:
  api-docs:
    enabled: false # ZS-ENG-005閿涙碍甯撮崣锝嗘瀮濡楋綁绮拋銈勭瑝閺嗘挳婀堕敍灞肩矌 local 閻滎垰顣ㄩ弰鎯х础瀵偓閸?
    path: /v3/api-docs
  swagger-ui:
    enabled: false # ZS-ENG-005閿涙艾鎮撴稉?    path: /swagger-ui
  default-flat-param-object: true # 閸欏倽顫?https://doc.xiaominfo.com/docs/faq/v4/
knife4j-parameterobject-flat-param 閺傚洦銆?

knife4j:
  enable: true
  setting:
    language: zh_cn

# 瀹搞儰缍斿ù?Flowable 闁板秶鐤?
flowable:
  # 1. false: 姒涙顓婚崐纭风礉Flowable 閸氼垰濮╅弮璁圭礉鐎佃鐦弫鐗堝祦鎼存捁銆冩稉顓濈箽鐎涙娈戦悧鍫熸拱閿涘苯顩ч弸婊€绗夐崠
褰掑帳閵嗗倸鐨㈤幎娑樺毉瀵倸鐖?
  # 2. true: 閸氼垰濮╅弮鏈电窗鐎佃鏆熼幑顔肩氨娑擃厽澧嶉張澶庛€冩潻娑滎攽閺囧瓨鏌婇幙宥勭稊閿涘苯顩ч弸婊嗐€冪€涙ê婀敍灞肩瑝閸嬫艾顦╅悶
鍡礉閸欏秳绠ｉ敍宀冨殰閸斻劌鍨卞楦裤€?
  # 3. create_drop: 閸氼垰濮╅弮鎯板殰閸斻劌鍨卞楦裤€冮敍灞藉彠闂傤厽妞傞懛顏勫З閸掔娀娅庣悰?  # 4. drop_create: 
閸氼垰濮╅弮璁圭礉閸掔娀娅庨弮褑銆冮敍灞藉晙閸掓稑缂撻弬鎷屻€?
  database-schema-update: true # 鐠佸墽鐤嗘稉?false閿涘苯褰查柅姘崇箖 https://github.com/flowa
ble/flowable-sql 閸掓繂顫愰崠?  db-history-used: true # flowable6 姒涙顓?true 閻㈢喐鍨氭穱鈩冧紖
鐞涱煉绱濋弮鐘绘付閹靛濮╃拋鍓х枂
  check-process-definitions: false # 鐠佸墽鐤嗘稉?false閿涘瞼顩﹂悽?/resources/processes 閼奉
亜濮╅柈銊ц BPMN XML 濞翠胶鈻?
  history-level: audit # full閿涙矮绻氱€涙ê宸婚崣鍙夋殶閹诡喚娈戦張鈧妯奸獓閸掝偓绱濋崣顖欑箽鐎涙ê鍙忛柈銊︾ウ缁嬪娴夐崗
宕囩矎閼哄偊绱濋崠鍛濞翠胶鈻煎ù浣芥祮閸氬嫯濡悙鐟板棘閺?
# MyBatis Plus 閻ㄥ嫰鍘ょ純顕€銆?
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true # 閾忕晫鍔ф妯款吇娑?true 閿涘奔绲鹃弰顖濈箷閺勵垱妯夌粈鍝勫箵閹稿洤鐣
炬稉瀣ㄢ偓?  global-config:
    db-config:
      id-type: NONE # 閳ユ粍娅ら懗瑙ｂ偓婵嚹佸蹇ョ礉閸╄桨绨?IdTypeEnvironmentPostProcessor + 閺佺増
宓佸┃鎰畱缁鐎烽敍宀冨殰閸斻劑鈧倿鍘ら幋?AUTO閵嗕浮NPUT 濡€崇础閵?#      id-type: AUTO # 閼奉亜顤?ID閿涘矂鈧
倸鎮?MySQL 缁涘娲块幒銉ㄥ殰婢х偟娈戦弫鐗堝祦鎼?#      id-type: INPUT # 閻劍鍩涙潏鎾冲弳 ID閿涘矂鈧倸鎮?Oracle
閵嗕赋ostgreSQL閵嗕甫ingbase閵嗕笍B2閵嗕笭2 閺佺増宓佹惔?#      id-type: ASSIGN_ID # 閸掑棝鍘?ID閿涘矂绮
拋銈勫▏閻劑娲╅懞杈╃暬濞夋洏鈧倹鏁為幇蹇ョ礉Oracle閵嗕赋ostgreSQL閵嗕甫ingbase閵嗕笍B2閵嗕笭2 閺佺増宓佹惔鎾存閿涘矂娓剁憰浣
稿箵闂勩倕鐤勬担鎾惰娑撳﹦娈?@KeySequence 濞夈劏袙
      logic-delete-value: 1 # 闁槒绶鎻掑灩闂勩倕鈧?姒涙顓绘稉?1)
      logic-not-delete-value: 0 # 闁槒绶張顏勫灩闂勩倕鈧?姒涙顓绘稉?0)
    banner: false # 閸忔娊妫撮幒褍鍩楅崣鎵畱 Banner 閹垫挸宓?
  type-aliases-package: ${zszj.info.base-package}.module.*.dal.dataobject
  encryptor:
    password: "${ZSZJ_APPLICATION_MYBATIS_PLUS_ENCRYPTOR_PASSWORD:}" # source d
emo secret removed; configure explicitly

mybatis-plus-join:
  banner: false # 閺勵垰鎯侀幍鎾冲祪 mybatis plus join banner閿涘矂绮拋顦歳ue
  sub-table-logic: true # 閸忋劌鐪崥顖滄暏閸擃垵銆冮柅鏄忕帆閸掔娀娅庨敍宀勭帛鐠侇槡rue閵嗗倸鍙ч梻顓炴倵閸忓疇浠堥弻銉嚄娑撳
秳绱伴崝鐘插鐞涖劑鈧槒绶崚鐘绘珟
  ms-cache: true # 閹凤附鍩呴崳鈭昦ppedStatement缂傛挸鐡ㄩ敍宀勭帛鐠?true
  table-alias: t # 鐞涖劌鍩嗛崥?姒涙顓?t)
  logic-del-type: on # 閸擃垵銆冮柅鏄忕帆閸掔娀娅庨弶鈥叉閻ㄥ嫪缍呯純顕嗙礉閺€顖涘瘮 WHERE閵嗕副N閿涘矂绮拋?ON

# Spring Data Redis 闁板秶鐤?
spring:
  data:
    redis:
      repositories:
        enabled: false # 妞ゅ湱娲伴張顏冨▏閻劌鍩?Spring Data Redis 閻?Repository閿涘本澧嶆禒銉ф纯閹
恒儳顩﹂悽顭掔礉娣囨繆鐦夐崥顖氬З闁喎瀹?

# VO 鏉烆剚宕查敍鍫熸殶閹诡喚鐐曠拠鎴礆閻╃鍙?
easy-trans:
  is-enable-global: false # 閵嗘劙绮拋銈囶洣閻㈩煉绱濈€佃鈧嗗厴绾喛顓婚崢瀣婢堆佲偓鎴濇儙閻劌鍙忕仦鈧紙鏄忕槯閿涘
牊瀚ら幋顏呭閺?SpringMVC ResponseBody 鏉╂稖顢戦懛顏勫З缂堟槒鐦?)閵嗗倸顩ч弸婊冾嚠娴滃孩鈧嗗厴鐟曚焦鐪板鍫ョ彯閸欘垰鍙ч梻顓
燁劃闁板秶鐤嗛敍灞惧灗闁俺绻?@IgnoreTrans 韫囩晫鏆愰弻鎰嚋閹恒儱褰?

--- #################### 妤犲矁鐦夐惍浣烘祲閸忔娊鍘ょ純?####################

aj:
  captcha:
    jigsaw: classpath:images/jigsaw # 濠婃垵濮╂宀冪槈閿涘苯绨抽崶鎹愮熅瀵板嫸绱濇稉宥夊帳缂冾喖鐨㈡担璺ㄦ暏姒涙顓婚
崶鍓у閿涙稐浜?classpath: 瀵偓婢惰揪绱濋崣?resource 閻╊喖缍嶆稉瀣熅瀵?    pic-click: classpath:imag
es/pic-click # 濠婃垵濮╂宀冪槈閿涘苯绨抽崶鎹愮熅瀵板嫸绱濇稉宥夊帳缂冾喖鐨㈡担璺ㄦ暏姒涙顓婚崶鍓у閿涙稐浜?classpath: 瀵偓
婢惰揪绱濋崣?resource 閻╊喖缍嶆稉瀣熅瀵?    cache-type: redis # 缂傛挸鐡?local/redis...
    cache-number: 1000 # local 缂傛挸鐡ㄩ惃鍕閸?鏉堟儳鍩屾潻娆庨嚋閸婄》绱濆〒鍛存珟缂傛挸鐡?
    timing-clear: 180 # local鐎规碍妞傚〒鍛存珟鏉╁洦婀＄紓鎾崇摠(閸楁洑缍呯粔?,鐠佸墽鐤嗘稉?娴狅綀銆冩稉宥嗗⒔鐞?    t
ype: blockPuzzle # 妤犲矁鐦夐惍浣鸿閸?default 娑撳顫掗柈钘夌杽娓氬瀵查妴淇爈ockPuzzle 濠婃垵娼￠幏鐓庢禈閵嗕恭li
ckWord 閺傚洤鐡ч悙褰掆偓澶堚偓涔竔ctureWord 閺傚洦婀版潏鎾冲弳
    water-mark: 娴兼顣稊瀣啀 # 閸欏厖绗呯憴鎺撴寜閸楃増鏋冪€?閹存垹娈戝鏉戝祪)閿涘苯褰叉担璺ㄦ暏 https://tool.ch
inaz.com/tools/unicode.aspx 娑擃厽鏋冩潪?Unicode閿涘inux 閸欘垵鍏橀棁鈧憰浣芥祮 unicode
    interference-options: 0 # 濠婃垵濮╅獮鍙夊妞?0/1/2)
    req-frequency-limit-enable: false # 閹恒儱褰涚拠閿嬬湴濞嗏剝鏆熸稉鈧崚鍡涙寭闂勬劕鍩楅弰顖氭儊瀵偓閸?true
|false
    req-get-lock-limit: 5 # 妤犲矁鐦夋径杈Е 5 濞嗏槄绱漡et閹恒儱褰涢柨浣哥暰
    req-get-lock-seconds: 10 # 妤犲矁鐦夋径杈Е閸氬函绱濋柨浣哥暰閺冨爼妫块梻鎾
    req-get-minute-limit: 30 # get 閹恒儱褰涙稉鈧崚鍡涙寭閸愬懓顕Ч鍌涙殶闂勬劕鍩?
    req-check-minute-limit: 60 # check 閹恒儱褰涙稉鈧崚鍡涙寭閸愬懓顕Ч鍌涙殶闂勬劕鍩?
    req-verify-minute-limit: 60 # verify 閹恒儱褰涙稉鈧崚鍡涙寭閸愬懓顕Ч鍌涙殶闂勬劕鍩?

--- #################### 濞戝牊浼呴梼鐔峰灙閻╃鍙?####################

# rocketmq 闁板秶鐤嗘い鐧哥礉鐎电懓绨?RocketMQProperties 闁板秶鐤嗙猾?rocketmq:
  # Producer 闁板秶鐤嗘い?  producer:
    group: ${spring.application.name}_PRODUCER # 閻㈢喍楠囬懓鍛瀻缂?
spring:
  # Kafka 闁板秶鐤嗘い鐧哥礉鐎电懓绨?KafkaProperties 闁板秶鐤嗙猾?  kafka:
    # Kafka Producer 闁板秶鐤嗘い?    producer:
      acks: 1 # 0-娑撳秴绨茬粵鏂烩偓?-leader 鎼存梻鐡熼妴淇沴l-閹碘偓閺?leader 閸?follower 鎼存梻鐡熼妴?   
   retries: 3 # 閸欐垿鈧礁銇戠拹銉︽閿涘矂鍣哥拠鏇炲絺闁胶娈戝▎鈩冩殶
      value-serializer: org.springframework.kafka.support.serializer.JsonSerial
izer # 濞戝牊浼呴惃?value 閻ㄥ嫬绨崚妤€瀵?
    # Kafka Consumer 闁板秶鐤嗘い?    consumer:
      auto-offset-reset: earliest # 鐠佸墽鐤嗗☉鍫ｅ瀭閼板懎鍨庣紒鍕付閸掓繄娈戝☉鍫ｅ瀭鏉╂稑瀹虫稉?earliest 
閵嗗倸褰查崣鍌濃偓鍐ㄥ触鐎?https://blog.csdn.net/lishuangzhe7047/article/details/74530417 閻炲
棜袙
      value-deserializer: org.springframework.kafka.support.serializer.JsonDese
rializer
      properties:
        spring.json.trusted.packages: '*'
    # Kafka Consumer Listener 閻╂垵鎯夐崳銊╁帳缂?    listener:
      missing-topics-fatal: false # 濞戝牐鍨傞惄鎴濇儔閹恒儱褰涢惄鎴濇儔閻ㄥ嫪瀵屾０妯圭瑝鐎涙ê婀弮璁圭礉姒涙顓绘导姘
Г闁挎瑣鈧倹澧嶆禒銉┾偓姘崇箖鐠佸墽鐤嗘稉?false 閿涘矁袙閸愯櫕濮ら柨?
--- #################### AI 閻╃鍙ч柊宥囩枂 ####################

spring:
  ai:
    vectorstore: # 閸氭垿鍣虹€涙ê鍋?
      redis:
        initialize-schema: true
        index-name: knowledge_index # Redis 娑擃厼鎮滈柌蹇曞偍瀵洜娈戦崥宥囆為敍姘辨暏娴滃骸鐡ㄩ崒銊ユ嫲濡偓缁
便垹鎮滈柌蹇旀殶閹诡喚娈戠槐銏犵穿閺嶅洩鐦戠粭锔肩礉閹碘偓閺堝娴夐崗宕囨畱閸氭垿鍣洪幖婊呭偍閹垮秳缍旈柈鎴掔窗閸╄桨绨潻娆庨嚋缁便垹绱╂潻娑滎攽
        prefix: "knowledge_segment:" # Redis 娑擃厼鐡ㄩ崒銊ユ倻闁插繑鏆熼幑顔炬畱闁款喖鎮曢崜宥囩磻閿涙俺绻栨稉顏
勫缂傗偓娴兼碍鍧婇崝鐘插煂濮ｅ繋閲滅€涙ê鍋嶉崷?Redis 娑擃厾娈戦崥鎴﹀櫤閺佺増宓侀柨顔兼倳閸撳稄绱濆В蹇庨嚋 document 闁姤妲告稉鈧稉?
hash 缂佹挻鐎?
      qdrant:
        initialize-schema: true
        collection-name: knowledge_segment # Qdrant 娑擃厼鎮滈柌蹇涙肠閸氬牏娈戦崥宥囆為敍姘辨暏娴滃骸鐡ㄩ
崒銊ユ倻闁插繑鏆熼幑顔炬畱闂嗗棗鎮庨弽鍥槕缁楋讣绱濋幍鈧張澶屾祲閸忓磭娈戦崥鎴﹀櫤閹垮秳缍旈柈鎴掔窗閸︺劏绻栨稉顏堟肠閸氬牅鑵戞潻娑滎攽
        host: 127.0.0.1
        port: 6334
      milvus:
        initialize-schema: true
        database-name: default # Milvus 娑擃厽鏆熼幑顔肩氨閻ㄥ嫬鎮曠粔?        collection-name
: knowledge_segment # Milvus 娑擃參娉﹂崥鍫㈡畱閸氬秶袨閿涙氨鏁ゆ禍搴＄摠閸屻劌鎮滈柌蹇旀殶閹诡喚娈戦梿鍡楁値閺嶅洩鐦戠粭锔肩礉閹
碘偓閺堝娴夐崗宕囨畱閸氭垿鍣洪幙宥勭稊闁垝绱伴崷銊ㄧ箹娑擃亪娉﹂崥鍫滆厬鏉╂稖顢?
        client:
          host: 127.0.0.1
          port: 19530
    openai: # OpenAI 鐎规ɑ鏌?
      base-url: ${OPENAI_BASE_URL:https://api.openai.com}
      api-key: ${OPENAI_API_KEY:sk-xxxx}
    azure: # OpenAI 瀵邦喛钂?
      openai:
        endpoint: https://eastusprejade.openai.azure.com
    anthropic: # Anthropic Claude
      base-url: ${ANTHROPIC_BASE_URL:https://api.anthropic.com}
      api-key: ${ANTHROPIC_API_KEY:sk-xxxx}
    ollama:
      base-url: http://127.0.0.1:11434
      chat:
        model: llama3
    stabilityai:
      api-key: ${STABILITYAI_API_KEY:sk-xxxx}
    dashscope: # 闁矮绠熼崡鍐６
      api-key: ${DASHSCOPE_API_KEY:sk-xxxx}
    deepseek: # DeepSeek
      api-key: ${DEEPSEEK_API_KEY:sk-xxxx}
      chat:
        options:
          model: deepseek-v4-flash
    model:
      rerank: false # 閺勵垰鎯佸鈧崥顖椻偓婊堚偓姘疅閸楀啴妫堕垾婵堟畱 Rerank 濡€崇€烽敍灞斤綖閸?dashscope 
瀵偓閸?    mcp:
      server:
        enabled: false
        name: zszj-mcp-server
        version: 1.0.0
        instructions: 娑撯偓娑?MCP 缁€杞扮伐閺堝秴濮?
        sse-endpoint: /sse
      client:
        enabled: false
        name: mcp
        sse:
          connections:
            filesystem:
              url: http://127.0.0.1:8089
              sse-endpoint: /sse
        annotation-scanner:
          enabled: false # TODO @閼哄澹欓敍姘箒 bug https://github.com/spring-projec
ts/spring-ai/issues/4917 闂団偓鐟曚礁鐣奸弬閫涙叏婢?
zszj:
  file: # ZS-FILE-002閿涙矮绗傛导鐘绘妫版繀绗岀猾璇茬€烽崥鍫濇倱
    max-size: 16777216 # 16MB閿涘奔绗?spring.servlet.multipart.max-file-size 鐎靛綊缍?
    danger-extensions: [exe, dll, bat, cmd, sh, js, vbs, msi, com, scr, ps1, sv
g]
    public-allowed-types: [image/png, image/jpeg, image/gif, image/webp, applic
ation/pdf, text/plain]
    max-concurrent-uploads: 32 # 閸︺劑鈧柧绗傛导鐘活暕缁犳绱橲3 鐡掑懏妞傛稉鍝勬祼鐎?60s 閸氬牆鎮撻敍宀冾潌 S3
FileClient閿?  ai:
    gemini: # 鐠嬮攱鐡?Gemini
      enable: true
      api-key: ${GEMINI_API_KEY:sk-xxxx}
      model: gemini-2.5-flash
    doubao: # 鐎涙濡挒鍡楀瘶
      enable: true
      api-key: ${DOUBAO_API_KEY:sk-xxxx}
      model: doubao-seed-2-1-turbo-260628
    hunyuan: # 閼垫崘顔嗗ǎ宄板帗
      enable: true
      api-key: ${HUNYUAN_API_KEY:sk-xxxx}
      model: hy3-preview
    siliconflow: # 绾懎鐔€濞翠礁濮?
      enable: true
      api-key: ${SILICONFLOW_API_KEY:sk-xxxx}
      model: deepseek-ai/DeepSeek-V4-Pro
    xinghuo: # 鐠侇垶顥ｉ弰鐔轰紑
      enable: true
      api-key: ${XINGHUO_API_KEY:sk-xxxx}
      model: x2-flash
    baichuan: # 閻ф儳绐涢弲楦垮厴
      enable: true
      api-key: ${BAICHUAN_API_KEY:sk-xxxx}
      model: Baichuan-M3
    yiyan: # 閺傚洤绺炬稉鈧懛鈧?
      enable: false
      api-key: ${YIYAN_API_KEY:sk-xxxx}
      model: ernie-5.1
    zhipu: # 閺呴缚姘?AI閿涘湙LM閿?      enable: false
      api-key: ${ZHIPU_API_KEY:sk-xxxx}
      model: glm-5.2
    minimax: # MiniMax
      enable: false
      api-key: ${MINIMAX_API_KEY:sk-xxxx}
      model: MiniMax-M3
    moonshot: # 閺堝牅绠ｉ弳妤呮桨閿涘湠IMI閿?      enable: false
      api-key: ${MOONSHOT_API_KEY:sk-xxxx}
      model: kimi-k2.6
    stepfun: # 闂冩儼绌弰鐔绘櫙
      enable: false
      api-key: ${STEPFUN_API_KEY:sk-xxxx}
      model: step-3.7-flash
    grok: # Grok
      enable: false
      api-key: ${GROK_API_KEY:sk-xxxx}
      model: grok-4.3
    midjourney:
      enable: true
  #    base-url: https://api.holdai.top/mj-relax/mj
      base-url: https://api.holdai.top/mj
      api-key: ${MIDJOURNEY_API_KEY:sk-xxxx}
      notify-url: http://java.nat300.top/admin-api/ai/image/midjourney/notify
    suno:
      enable: true
  #    base-url: https://suno-55ishh05u-status2xxs-projects.vercel.app
      base-url: http://127.0.0.1:3001
    web-search:
      enable: true
      api-key: ${WEB_SEARCH_API_KEY:sk-xxxx}

--- #################### 娴兼顣稊瀣啀閻╃鍙ч柊宥囩枂 ####################

zszj:
  info:
    version: 1.0.0
    base-package: cn.zszj
  web:
    admin-ui:
      url: http://dashboard.zszj.local # Admin 缁狅紕鎮婇崥搴″酱 UI 閻ㄥ嫬婀撮崸鈧?
    cors: # ZS-SEC-004閿涙俺娉曢崺鐔告暪缁毖傝礋缁墽鈥橀惂钘夋倳閸楁洩绱濈粋浣诡剾鐟?* 娑撳骸鍤熼幑顔艰嫙鐎涙﹫绱遍悽鐔堕獓/妫板嫬
褰傞惃鍕埂鐎圭偛澧犵粩顖氱厵閸氬秴婀悳顖氼暔妤犲本鏁归弮鑸靛瘻 env 鐟曞棛娲?
      allowed-origin-patterns:
        - "http://localhost:*"
        - "http://127.0.0.1:*"
        - "http://localhost" # ZS-SEC-004 P1-1閿?:*" 娑撳秴灏柊宥嗘￥缁旑垰褰涘┃鎰剁幢閺堫剙婀?VITE
_PORT=80 閺?Origin 娑撶儤妫ょ粩顖氬經 http://localhost
        - "http://127.0.0.1"
      allow-credentials: true
      frame-options: SAMEORIGIN # 姒涙顓婚梼鑼仯閸戣濮幐渚婄幢绾噣娓剁悮?iframe 瀹撳苯鍙嗛弮鑸垫▔瀵繑鏁
兼稉?DISABLE
  xss:
    enable: false
    exclude-urls: # 婵″倷绗呮稉銈勯嚋 url閿涘奔绮庢禒鍛Ц娑撹桨绨″鏃傘仛閿涘苯骞撻幒澶愬帳缂冾喕绡冨▽鈥冲彠缁?      - 
${spring.boot.admin.context-path}/** # 娑撳秴顦╅悶?Spring Boot Admin 閻ㄥ嫯顕Ч?      - 
${management.endpoints.web.base-path}/** # 娑撳秴顦╅悶?Actuator 閻ㄥ嫯顕Ч?  security:
    permit-all_urls:
      - /admin-api/mp/open/** # 瀵邦喕淇婇崗顑跨船閸欏嘲绱戦弨鎯ч挬閸欏府绱濆顔讳繆閸ョ偠鐨熼幒銉ュ經閿涘奔绗夐棁鈧憰浣烘
瑜?    # ZS-LOGIN-001 娴犮倗澧濋悽銊┾偓鏂垮瀻缁傚妫幒褝绱版禒锝囩垳姒涙顓?false閿涘牆鐣ㄩ崗銊╃帛鐠併倧绱漡etAccess
Token 閸欘亣顓荤拋鍧楁６娴犮倗澧濋敍澶堚偓?    # 閻滄壆缍夌純?true 閻ㄥ嫬甯崶鐙呯窗admin-web IM閵嗕沟iniapp IM/鐎广
垺婀囬惃?WebSocket 閹烩剝澧滄禒??token=<refreshToken> 娴ｆ粌鍤熼幑?    # 閿涘牊绁荤憴鍫濇珤 WS 閺冪姵纭堕懛顏勭暰
娑?Header閿涘苯褰ч懗鑺ュ URL 閸欏倹鏆熼敍澶涚礉娓氭繆绂嗛崚閿嬫煀娴犮倗澧濋崶鐐衡偓鈧弨鎹愵攽閵嗗倹顒濇稉楦跨讣缁夌粯婀℃稉瀛樻閸忕厧顔愰敍
?    # 妞よ婀?LOGIN-001.B閿涘牏鐓弮鑸靛綑閹靛銈ㄩ幑?+ 閸撳秶顏?WS 鏉╀胶些閿涘鐣幋鎰倵閺€鐟版礀 false 楠炶泛鍨归
梽銈嗘拱闁款喓鈧?    refresh-token-as-access-token-enabled: true
  api-encrypt:
    enable: false # 閺勵垰鎯佸鈧崥?API 閸旂姴鐦戦妴鍌炵帛鐠併倕鍙ч梻顓ㄧ礉闁灝鍘ゆ担搴ｅ閺?JDK 8閿?u120 娴犮儰绗
呴敍澶婃儙閸斻劍妞?bcprov 閻?JCE 鐠併倛鐦夐幎銉╂晩
    algorithm: AES # 閸旂姴鐦戠粻妤佺《閿涘本鏁幐?AES閵嗕阜SA 缁?    request-key: "${ZSZJ_REQUES
T_KEY:}" # configure explicitly
    response-key: "${ZSZJ_RESPONSE_KEY:}" # configure explicitly
  websocket:
    enable: true # websocket閻ㄥ嫬绱戦崗?    path: /infra/ws # 鐠侯垰绶?
    sender-type: local # 濞戝牊浼呴崣鎴︹偓浣烘畱缁鐎烽敍灞藉讲闁鈧棿璐?local閵嗕购edis閵嗕购ocketmq閵嗕共
afka閵嗕购abbitmq
    sender-rocketmq:
      topic: ${spring.application.name}-websocket # 濞戝牊浼呴崣鎴︹偓浣烘畱 RocketMQ Topic
      consumer-group: ${spring.application.name}-websocket-consumer # 濞戝牊浼呴崣鎴︹偓
浣烘畱 RocketMQ Consumer Group
    sender-rabbitmq:
      exchange: ${spring.application.name}-websocket-exchange # 濞戝牊浼呴崣鎴︹偓浣烘畱 Ra
bbitMQ Exchange
      queue: ${spring.application.name}-websocket-queue # 濞戝牊浼呴崣鎴︹偓浣烘畱 RabbitMQ
 Queue
    sender-kafka:
      topic: ${spring.application.name}-websocket # 濞戝牊浼呴崣鎴︹偓浣烘畱 Kafka Topic
      consumer-group: ${spring.application.name}-websocket-consumer # 濞戝牊浼呴崣鎴︹偓
浣烘畱 Kafka Consumer Group
  swagger:
    title: 娴兼顣稊瀣啀 AI 鐠у鍏橀獮鍐插酱
    description: 閹绘劒绶电粻锛勬倞閸氬骸褰撮妴浣烘暏閹?App 閻ㄥ嫭澧嶉張澶婂閼?    version: ${zszj.info.ve
rsion}
    url: ${zszj.web.admin-ui.url}
    email: xingyu4j@vip.qq.com
    license: MIT
    license-url: https://gitee.com/zhijiantianya/ruoyi-vue-pro/blob/master/LICE
NSE
  codegen:
    base-package: ${zszj.info.base-package}
    db-schemas: ${spring.datasource.dynamic.datasource.master.name}
    front-type: 20 # 閸撳秶顏Ο锛勫閻ㄥ嫮琚崹瀣剁礉閸欏倽顫?CodegenFrontTypeEnum 閺嬫矮濡囩猾?    vo-
type: 10 # VO 閻ㄥ嫮琚崹瀣剁礉閸欏倽顫?CodegenVOTypeEnum 閺嬫矮濡囩猾?    delete-batch-enable: t
rue # 閺勵垰鎯侀悽鐔稿灇閹靛綊鍣洪崚鐘绘珟閹恒儱褰?
    unit-test-enable: false # 閺勵垰鎯侀悽鐔稿灇閸楁洖鍘撳ù瀣槸
    import-enable: false # 閺勵垰鎯侀悽鐔稿灇 Excel 鐎电厧鍙嗛幒銉ュ經
  tenant: # 婢舵氨顫ら幋椋庢祲閸忔娊鍘ょ純顕€銆?
    enable: true
    ignore-urls:
      - /jmreport/* # 缁夘垱婀幎銉ㄣ€冮敍灞炬￥濞夋洘鎯＄敮锔绢潳閹撮绱崣?      # ZS-FILE-001.A閿涙碍鏋冩禒
鏈电瑓鏉炴枻绱欓崥顐㈠岸閸?PUBLIC 缁辩姵娼楅敍灞剧セ鐟欏牆娅?img/src 娑撳秵鎯＄敮?tenant-id 婢惰揪绱氶敍?      # PRIV
ATE 閻ㄥ嫮娅ヨぐ?閸氬瞼顫ら幋閿嬬墡妤犲苯婀?FileService#validateFileReadable 娴?LoginUser 閹笛嗩攽
      - /admin-api/infra/file/*/get/**
    ignore-visit-urls:
      - /admin-api/system/user/profile/**
      - /admin-api/system/auth/**
    ignore-tables:
    ignore-caches:
      - user_role_ids
      - permission_menu_ids
      - oauth_client
      - notify_template
      - mail_account
      - mail_template
      - sms_template
      - iot:device
      - iot:thing_model_list
  sms-code: # 閻厺淇婃宀冪槈閻胶娴夐崗宕囨畱闁板秶鐤嗘い?    expire-times: 10m
    send-frequency: 1m
    send-maximum-quantity-per-day: 10
    begin-code: ${ZSZJ_SMS_CODE_BEGIN:1000} # ZS-LOGIN-004閿涙氨骞嗘晶鍐ㄥ綁闁插繐宕版担?+ 鐎瑰
鍙忔妯款吇闂呭繑婧€娑撳鏅敍娑樻祼鐎规碍绱ㄧ粈铏圭垳 9999 閸欘亜鍘戠拋绋垮毉閻滄澘婀棃鐐垫晸娴?profile
    end-code: ${ZSZJ_SMS_CODE_END:9999} # ZS-LOGIN-004閿涙氨骞嗘晶鍐ㄥ綁闁插繐宕版担?+ 鐎瑰鍙忔妯
款吇闂呭繑婧€娑撳﹦鏅?
  trade:
    order:
      pay-expire-time: 2h # 閺€顖欑帛閻ㄥ嫯绻冮張鐔告闂?      receive-expire-time: 14d # 閺€
鎯版彛閻ㄥ嫯绻冮張鐔告闂?      comment-expire-time: 7d # 鐠囧嫯顔戦惃鍕箖閺堢喐妞傞梻?      status-sync
-to-wxa-enable: true # 閺勵垰鎯侀崥灞绢劄鐠併垹宕熼悩鑸碘偓浣稿煂瀵邦喕淇婄亸蹇曗柤鎼?    express:
      client: kd_100
      kd-niao:
        api-key: "${ZSZJ_APPLICATION_TRADE_EXPRESS_KD_NIAO_API_KEY:}" # source 
demo secret removed; configure explicitly
        business-id: 1809751
        request-type: 1002 # 閸忓秷鍨傞悧?1002閿涙稐绮拹鍦 8001
      kd100:
        key: "${ZSZJ_SOURCE_EXAMPLE_KEY:}" # configure explicitly
        customer: "${ZSZJ_APPLICATION_KD100_CUSTOMER:}" # 閹稿骞嗘晶鍐╂暈閸忋儻绱濇稉宥勫▏閻劋
绗傚〒鍛婄川缁€鍝勵吂閹村嘲褰?
  iot:
    message-bus:
      type: redis # 濞戝牊浼呴幀鑽ゅ殠閻ㄥ嫮琚崹?
debug: false

--- #################### 缁狅紕鎮婄粩顖滃仯閿涘湼S-ENG-005閿涙岸绮拋銈勭矌閸嬨儱鎮嶅Λ鈧弻銉礆 ###########
#########

management:
  endpoints:
    web:
      base-path: /actuator
      exposure:
        include: health

--- #################### 閺佺増宓佹惔鎾圭讣缁変紮绱橺S-DB-003 / PEND-005閿?###################
#

spring:
  flyway:
    enabled: false # V1 閸╄櫣鍤庨悽?ZS-DB-004 娴溿倓绮敍宀勫劥缂冭尙骞嗘晶鍐病 ZSZJ_FLYWAY_ENABLED
 濠碘偓濞?    locations: classpath:db/migration # 閸烆垯绔存潻浣盒╅惄顔肩秿閿涘牐顫夐懠鍐潌 docs/閺佺増宓佹
惔鎾圭讣缁夋槒顫夐懠?md閿?    out-of-order: false # 閸忋劌鐪悧鍫熸拱妞ゅ搫绨敍灞筋樋鐎圭偘绶ョ挧鏉垮悑鐎圭绺肩粔?濠婃艾濮
╅崣鎴濈
    validate-on-migrate: true # 瀹稿弶澧界悰宀冨壖閺堫剝顫︾弧鈩冩暭閿涘牊鐗庢灞芥嫲婢堕亶鍘ら敍澶婂祮闂冪粯顒涢崥顖氬З
    clean-disabled: true # 閸忋劎骞嗘晶鍐洣閻?clean閿涙稒绁寸拠鏇炵氨濞撳懐鎮婇悽?ZS-DB-019 娑撳瓨妞傜€圭偘绶ョ
拹鐔荤煑
    baseline-on-migrate: true # 閺冦垺婀侀弮褍绨辨＃鏍偧閹恒儱鍙嗛弮璺烘儧閺€鏈佃礋閸╄櫣鍤庨敍灞肩瑝闁插秵鏂?V1
    baseline-version: 20260909.001 # 娑?V1 閸╄櫣鍤庢潻浣盒╅悧鍫熸拱娑撯偓閼疯揪绱伴弮銏℃箒閺冄冪氨閸氬憡鏁归崥搴濈
瑝闁插秵鏂?V1閿涘本鏌婄粚鍝勭氨濮濓絽鐖堕幍褑顢?V1
# =====================================================================
# 娴兼顣稊瀣啀闁劎璁查悳顖氼暔濡剝婢橀敍鍦窼-ENG-003 / docs/06 閺勭姴鐨?#7閿涙稑鎮庨崥宀冾潌閸氬瞼娲拌ぐ?README.md
閿?#
# 閻劍纭堕敍姘槻閸掕泛鍩岄柈銊ц閺?zszj-server.jar 閸氬瞼楠?config/application-prod.yaml閿?#     
  楠炶埖鏁為崗?ZSZJ_* 閻滎垰顣ㄩ崣姗€鍣洪妴鍌氬弿闁劌鍤熼幑顔剧病閻滎垰顣ㄩ崣姗€鍣哄▔銊ュ弳閿涘瞼顩﹀銏犲晸閻喎鐤勯崐鐓庡弳鎼存挶鈧?# 韫
囧懎锝炴い閫涘▏閻劍妫ゆ妯款吇閸婄厧宕版担宥囶儊閿涙碍绱￠柊宥嗘閸氼垰濮╅幎?"Could not resolve placeholder"
# 閿涘牐鍔氶弫蹇涙晩鐠囶垽绱濋崣顏勬儓閸欐﹢鍣洪崥宥忕礆閿涘奔绗夐棃娆撶帛閸ョ偤鈧偓濠曟梻銇氶張宥呭閵?# ======================
===============================================

server:
  port: ${ZSZJ_SERVER_PORT:48080}

--- #################### 閺佺増宓佹惔鎿勭礄D-08閿涙矮绮?PostgreSQL閿?####################

spring:
  datasource:
    dynamic:
      primary: master
      datasource:
        master:
          url: ${ZSZJ_DATASOURCE_URL} # 韫囧懎锝為敍瀹ヾbc:postgresql://host:5432/<data
base>
          username: ${ZSZJ_DATASOURCE_USERNAME} # 韫囧懎锝為敍灞界安閻劋缍嗛弶鍐鐠愶箑褰块敍鍫ｇ讣缁夋
槒澶勯崣宄板瀻缁傝缍?ZS-DB-002閿?          password: ${ZSZJ_DATASOURCE_PASSWORD} # 韫囧懎锝?
        druid:
          validation-query: ${ZSZJ_DATASOURCE_VALIDATION_QUERY:SELECT 1} # PG 閺
傜鈻堟妯款吇 SELECT 1

--- #################### Redis ####################

  data:
    redis:
      host: ${ZSZJ_REDIS_HOST} # 韫囧懎锝?
      port: ${ZSZJ_REDIS_PORT:6379}
      password: ${ZSZJ_REDIS_PASSWORD} # 韫囧懎锝為敍鍫㈡晸娴溠冪箑妞よ鎯庨悽?Redis 鐎靛棛鐖滈敍?
--- #################### 閻╂垶甯堕敍鍦窼-ENG-005 閺€鍓佹彛閸氬海娈戦柈銊ц姒涙顓婚崐纭风礆 #############
#######

management:
  endpoints:
    web:
      exposure:
        include: health # 閻㈢喍楠囨禒鍛瘹闂囨彃浠存惔閿嬵梾閺屻儻绱辩拠锔藉剰缁崵顏悙瑙勫瘻 ZS-ENG-005 閻ц棄鎮曢
崡鏇炲綗鐞涘矁鐦庢导?
# Spring Boot Admin 鐎广垺鍩涚粩顖炵帛鐠併倕鍙ч梻顓ㄧ幢婵″倿娓堕崥顖滄暏閸欙箒顢戠拠鍕強鐠愶箑褰挎稉搴ｇ秹缂佹粏绔熼悾?spring.
boot.admin.client.enabled: false

--- #################### 鐎瑰鍙忛敍鍦窼-ENG-004閿?####################

zszj:
  security:
    mock-enable: false # 闁劎璁查悳顖氼暔閺勬儳绱＄粋浣烘暏濡剝瀚欑拋銈堢槈閺冧浇鐭鹃敍鍫燁攱閺嬪爼绮拋銈呭祮娑?false閿涘
本顒濇径鍕▔瀵繐锛愰弰搴ㄦЩ鐠囶垶鍘ら敍?    # ZS-LOGIN-004閿涙氨娅ヨぐ鏇熸煙瀵繘妫幒褉鈧柡鈧柧绔撮張鐔峰涧閼惧嘲鍣妴宀冨閸
欏嘲鐦戦惍浣碘偓宥忕礉閸忔湹缍戦崗銉ュ經姒涙顓婚崗鎶芥４閿涘矂銆忕紒蹇曞箚婢у啫褰夐柌蹇旀▔瀵繐绱戦崥?    login-mode:
      sms-enabled: ${ZSZJ_LOGIN_MODE_SMS_ENABLED:false}
      social-enabled: ${ZSZJ_LOGIN_MODE_SOCIAL_ENABLED:false}
      register-enabled: ${ZSZJ_LOGIN_MODE_REGISTER_ENABLED:false}
      reset-password-enabled: ${ZSZJ_LOGIN_MODE_RESET_PASSWORD_ENABLED:false}
  # ZS-LOGIN-004閿涙岸鐛欑拠浣虹垳閸欐牕鈧厧鐓欐稉搴＄暔閸忋劌寮弫鎵斥偓鏂衡偓鏃傛晸娴溠冪箑妞よ璐熺搾鍐差檮婢堆呮畱闂呭繑婧€閸栨椽妫块
敍灞煎紬缁備礁娴愮€规碍绱ㄧ粈铏圭垳 9999
  sms-code:
    begin-code: ${ZSZJ_SMS_CODE_BEGIN:1000}
    end-code: ${ZSZJ_SMS_CODE_END:9999}
    max-validate-attempts: ${ZSZJ_SMS_MAX_VALIDATE_ATTEMPTS:5}
    attempt-lock-duration: ${ZSZJ_SMS_ATTEMPT_LOCK_DURATION:10m}
    send-maximum-quantity-per-ip-per-hour: ${ZSZJ_SMS_SEND_MAX_PER_IP_HOUR:20}
    send-maximum-quantity-per-ip-per-day: ${ZSZJ_SMS_SEND_MAX_PER_IP_DAY:50}

--- #################### 鐎规碍妞傛禒璇插閿涘湼S-ENG-006 鏉堝湱鏅敍?####################

spring:
  quartz:
    auto-startup: ${ZSZJ_QUARTZ_AUTO_STARTUP:false} # 闁劎璁叉妯款吇娑撳秹娈㈡惔鏃傛暏閸氼垰濮╃拫鍐
ㄥ閸ｎ煉绱濋崥顖滄暏妞ゆ槒绻?ZS-ENG-006 閻╂鍋?

--- #################### 閺佺増宓佹惔鎾圭讣缁変紮绱橺S-DB-003閿?####################

spring:
  flyway:
    enabled: ${ZSZJ_FLYWAY_ENABLED:false} # 閸╄櫣鍤庢潻浣盒╅拃钘夋勾閿涘湼S-DB-004閿涘鎮楃紒蹇曞箚婢у
啫褰夐柌蹇旂负濞?    # 鏉╀胶些鏉╃偞甯存妯款吇閸ョ偠鎯ゆ惔鏃傛暏閺佺増宓佸┃鎰剁幢闂団偓鐟?DDL 娑撴挾鏁ゆ潻浣盒╃拹锕€褰块弮鑸垫▔瀵繑鏁為崗
銉窗
    # url: ${ZSZJ_FLYWAY_URL}
    # user: ${ZSZJ_FLYWAY_USER}
    # password: ${ZSZJ_FLYWAY_PASSWORD}



exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'Sele
ct-String -Path $env:USERPROFILE'"\\.codex\\config.toml -Pattern 'project_doc|f
allback|instructions'; Get-Content -Encoding utf8 "'$env:USERPROFILE'"\\.codex\
\AGENTS.md; Get-ChildItem services/zhongshu-core/zszj-server/src/main/resources
 -Name; Get-Content -Encoding utf8 services/zhongshu-core/script/config/README.
md; Get-Content -Encoding utf8 services/zhongshu-core/zszj-server/pom.xml; rg -
n 'spring-boot.version|dynamic-datasource|RedisProperties|DataSourceAutoConfigu
ration|/actuator|/admin/|spring.boot.admin.context-path' services/zhongshu-core
/zszj-dependencies/pom.xml services/zhongshu-core/zszj-framework services/zhong
shu-core/zszj-server/src/main/java" in E:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇\.wt\zszj-wt-ops-002
-a
 succeeded in 176ms:

C:\Users\Administrator\.codex\config.toml:19:NODE_REPL_INSTRUCTIONS_USE_CASE_BR
OWSER = ""
C:\Users\Administrator\.codex\config.toml:20:NODE_REPL_INSTRUCTIONS_USE_CASE_CH
ROME = ""
db
application-dev.yaml
application-harness.yaml
application-local.yaml
application.yaml
logback-spring.xml
# 浼楀涔嬪鐜閰嶇疆鍚堝悓锛圸S-ENG-003 / ZS-CFG-001.A锛?

鏈枃鏄?`zszj-server` 鍚勭幆澧冮厤缃殑鍞竴鍚堝悓銆傚師鍒欙細鍦板潃涓庡嚟鎹寜鐜娉ㄥ叆锛涚瀵嗗彧瀛樺湪浜庣幆澧冨彉閲忔垨澶栭儴娉ㄥ叆
锛屼笉鍏ョ増鏈簱锛涚己澶卞繀闇€椤规椂鍚姩鏄庣‘澶辫触锛堝崰浣嶇瑙ｆ瀽閿欒锛屽寘鍚彉閲忓悕銆佷笉鍚瀵嗗€硷級锛屼笉闈欓粯鍥為€€婕旂ず鏈嶅姟銆?

## 閰嶇疆灞傛

| 灞傛 | 浣嶇疆 | 鐢ㄩ€?|
|---|---|---|
| 鎵撳寘鍐呯疆 | `zszj-server/src/main/resources/application.yaml` | 鍏ㄧ幆澧冨叕鍏遍粯璁ゅ€硷紙涓嶅
惈鍑嵁銆佷笉鍚幆澧冨湴鍧€锛?|
| 寮€鍙戣鐩?| `application-local.yaml`锛堥粯璁ゆ縺娲伙級/ `application-dev.yaml` | 鏈湴寮€鍙戯紱
婕旂ず鍊煎凡涓€у寲涓虹幆澧冨彉閲忓崰浣嶇 |
| 閮ㄧ讲妯℃澘 | [application-prod.yaml](application-prod.yaml) | 鐢熶骇/鐙珛娴嬭瘯鐜妯℃澘锛屽叏
閮ㄧ粡 `ZSZJ_*` 鐜鍙橀噺娉ㄥ叆锛屽鍒跺埌閮ㄧ讲鏈?jar 鍚岀骇 `config/` 鐩綍鍚庢寜闇€濉厖 |

## 蹇呭～椤逛笌澶辫触璇箟

- 蹇呭～椤瑰湪妯℃澘涓啓浣滄棤榛樿鍊肩殑鍗犱綅绗︼紙濡?`${ZSZJ_DATASOURCE_URL}`锛夛紱婕忛厤鏃?Spring 鍚姩鎶?
  `Could not resolve placeholder 'ZSZJ_DATASOURCE_URL'` 鈥斺€旈敊璇俊鎭彧鍚彉閲忓悕锛屽睘浜庤劚
鏁忛敊璇€?
- 鍙€夐」鍐欎綔甯﹂粯璁ゅ€肩殑鍗犱綅绗︼紙濡?`${ZSZJ_REDIS_PORT:6379}`锛夛紝榛樿鍊煎繀椤绘槸涓€у€硷紝涓嶅緱鏄笂娓告紨
绀烘湇鍔″湴鍧€銆?
- 绂佹鎶婄湡瀹炲瘑閽ュ啓鍏ヤ换浣曞彈鐗堟湰鎺у埗鐨?`.yaml`/`.env*`锛涙彁浜ゅ墠鐢?`node scripts/cfg/verify-con
fig-secrets.mjs`锛圸S-CFG-001.A锛夋壂鎻忋€?

## 鐜鍙橀噺娓呭崟锛堥儴缃叉ā鏉匡級

| 鍙橀噺 | 蹇呭～ | 璇存槑 |
|---|---|---|
| `ZSZJ_SERVER_PORT` | 鍚︼紙榛樿 48080锛?| HTTP 绔彛 |
| `ZSZJ_DATASOURCE_URL` | **鏄?* | 鐩爣鏁版嵁搴?JDBC URL锛圖-08锛歅ostgreSQL锛?|
| `ZSZJ_DATASOURCE_USERNAME` / `ZSZJ_DATASOURCE_PASSWORD` | **鏄?* | 搴旂敤璐﹀彿锛堜綆鏉冮
檺锛岃縼绉绘潈闄愬垎绂诲綊 ZS-DB-002锛?|
| `ZSZJ_DATASOURCE_VALIDATION_QUERY` | 鍚︼紙鎸夋柟瑷€锛?| 杩炴帴鏈夋晥鎬ф鏌?SQL锛孭G 浣跨敤 `SELEC
T 1` |
| `ZSZJ_REDIS_HOST` / `ZSZJ_REDIS_PORT` / `ZSZJ_REDIS_PASSWORD` | **鏄?*锛圥ORT 榛樿
 6379锛?| Redis 杩炴帴 |
| `ZSZJ_LOG_LEVEL_MODULE` | 鍚︼紙榛樿 INFO锛?| 鍚敤妯″潡 Mapper 鏃ュ織绾у埆 |
| `ZSZJ_PROFILE` | **鏄?* | 婵€娲?profile锛堥儴缃叉満缁?`--spring.profiles.active` 鎴栨鍙橀噺
娉ㄥ叆锛?|

璇存槑锛氭暟鎹簮椹卞姩渚濊禆涓庢墦鍖呮牳楠屽綊 ZS-DB-001锛涙ā鎷熶粠搴撶殑娓呴櫎褰?ZS-DB-001锛涙ā鎷熻璇侊紙mock-enable锛夐
粯璁ゅ叧闂綊 ZS-ENG-004銆?

## 涓庡叾浠栦换鍔＄殑杈圭晫

- ZS-ENG-004锛歚zszj.security.mock-enable` 榛樿 false锛沗/test` 璋冭瘯绔偣绉婚櫎鎴栨樉寮忛檺鍒躲€?
- ZS-ENG-005锛歛ctuator/Spring Boot Admin 绔偣鏆撮湶鐧藉悕鍗曪紙local 鐨?`include: '*'` 鏀剁揣锛
夈€?
- ZS-ENG-006锛歈uartz `auto-startup` 涓庝换鍔″惎鍋滆竟鐣岀洏鐐广€?
- ZS-CFG-001.A锛氱瀵嗘壂鎻忓伐鍏蜂笌妯℃澘鎵弿闂ㄧ锛堟壙鎺ユ湰鍚堝悓鐨勬満鍣ㄥ寲妫€鏌ワ級銆?

## 閰嶇疆鍒嗙被锛圸S-CFG-001.A锛?

| 鍒嗙被 | 鍒ゅ畾 | 瀛樻斁 | 涓嬪彂/灞曠ず |
|---|---|---|---|
| 绉樺瘑 | 鍙ｄ护銆佸瘑閽ャ€乼oken銆佸晢鎴峰嚟鎹?| 浠呯幆澧冨彉閲?鎵瑰噯绉樺瘑瀛樺偍锛岀粷涓嶅叆鐗堟湰搴撲笌 infra_config 琛?| 
鎺ュ彛/鏃ュ織/瀵煎嚭涓€寰嬭劚鏁忥紙ZS-CFG-001.B锛?|
| 鏁忔劅 | 鍐呴儴鍦板潃銆佽处鍙峰悕銆佸紑鍏崇粍鍚?| 閮ㄧ讲妯℃澘缁忕幆澧冩敞鍏ワ紱鍏ュ簱鍙傛暟椤绘爣娉ㄦ晱鎰熺骇 | 璇︽儏/瀵煎嚭闇€绠＄悊鏉冮檺骞
惰劚鏁?|
| 鏅€?| 涓氬姟鍙皟鍙傛暟锛堝缂撳瓨鏃堕暱锛?| infra_config 琛ㄥ彲鍏ュ簱 | 缁?visible 鎺у埗璇诲彇锛圕FG-001.B 
绾︽潫鍐欐潈闄愶級 |

绉樺瘑鎵弿闂ㄧ锛堜粨搴撴牴鐩綍杩愯锛夛細`node scripts/cfg/verify-config-secrets.mjs`鈥斺€旂瀵嗙被閿彧
鍏佽绌哄€?鍗犱綅绗︼紱
蹇呭～椤瑰繀椤讳负鏃犻粯璁ゅ€煎崰浣嶇锛涙嫤鎴?RSA/PEM 鍧椼€佽秴闀?Base64銆侀暱鍗佸叚杩涘埗涓层€?
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apa
che.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>cn.zszj</groupId>
        <artifactId>zszj</artifactId>
        <version>${revision}</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>zszj-server</artifactId>
    <packaging>jar</packaging>

    <name>${project.artifactId}</name>
    <description>
        鍚庣 Server 鐨勪富椤圭洰锛岄€氳繃寮曞叆闇€瑕?zszj-module-xxx 鐨勪緷璧栵紝
        浠庤€屽疄鐜版彁渚?RESTful API 缁?zszj-ui-admin銆亃szj-ui-user 绛夊墠绔」鐩€?
        鏈川涓婃潵璇达紝瀹冨氨鏄釜绌哄３锛堝鍣級锛?
    </description>
    <url>https://github.com/YunaiV/ruoyi-vue-pro</url>

    <dependencies>
        <dependency>
            <groupId>cn.zszj</groupId>
            <artifactId>zszj-module-system</artifactId>
            <version>${revision}</version>
        </dependency>
        <dependency>
            <groupId>cn.zszj</groupId>
            <artifactId>zszj-module-infra</artifactId>
            <version>${revision}</version>
        </dependency>

        <!-- ZS-DB-001.A / D-08锛氱洰鏍囨暟鎹簱涓?PostgreSQL锛岃繍琛屽寘鏄惧紡鍖呭惈 PG 椹卞姩锛?
             鍚勬柟瑷€椹卞姩鍦?starter 涓潎涓?optional锛孧ySQL 椹卞姩涓嶈繘鍏ヨ繍琛屽寘锛堟棤 MySQL 鍥為€€锛?
-->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- ZS-DB-003 / PEND-005锛欶lyway 鐗堟湰鍖栬縼绉伙紙鐗堟湰闅?Spring Boot BOM锛夛紝搴旂敤鍚姩
涓哄敮涓€鏉冨▉杩佺Щ鍏ュ彛 -->
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>

        <!-- ZS-ENG-001锛氱櫧鍚嶅崟娴嬭瘯鎵€闇€ JUnit5 -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- ZS-ENG-005锛氭渶灏忓仴搴锋帰閽堬紙/actuator/health锛夛紱璇︽儏绔偣缁忛厤缃櫧鍚嶅崟鏀剁揣 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>

        <!-- 浼氬憳涓績銆傞粯璁ゆ敞閲婏紝淇濊瘉缂栬瘧閫熷害 -->
<!--        <dependency>-->
<!--            <groupId>cn.zszj</groupId>-->
<!--            <artifactId>zszj-module-member</artifactId>-->
<!--            <version>${revision}</version>-->
<!--        </dependency>-->

        <!-- 鏁版嵁鎶ヨ〃銆傞粯璁ゆ敞閲婏紝淇濊瘉缂栬瘧閫熷害 -->
<!--        <dependency>-->
<!--            <groupId>cn.zszj</groupId>-->
<!--            <artifactId>zszj-module-report</artifactId>-->
<!--            <version>${revision}</version>-->
<!--        </dependency>-->
        <!-- 宸ヤ綔娴?-->
<!--        <dependency>-->
<!--            <groupId>cn.zszj</groupId>-->
<!--            <artifactId>zszj-module-bpm</artifactId>-->
<!--            <version>${revision}</version>-->
<!--        </dependency>-->
        <!-- 鏀粯鏈嶅姟銆傞粯璁ゆ敞閲婏紝淇濊瘉缂栬瘧閫熷害 -->
<!--        <dependency>-->
<!--            <groupId>cn.zszj</groupId>-->
<!--            <artifactId>zszj-module-pay</artifactId>-->
<!--            <version>${revision}</version>-->
<!--        </dependency>-->

        <!-- 寰俊鍏紬鍙锋ā鍧椼€傞粯璁ゆ敞閲婏紝淇濊瘉缂栬瘧閫熷害 -->
<!--        <dependency>-->
<!--            <groupId>cn.zszj</groupId>-->
<!--            <artifactId>zszj-module-mp</artifactId>-->
<!--            <version>${revision}</version>-->
<!--        </dependency>-->

        <!-- 鍟嗗煄鐩稿叧妯″潡銆傞粯璁ゆ敞閲婏紝淇濊瘉缂栬瘧閫熷害-->
<!--        <dependency>-->
<!--            <groupId>cn.zszj</groupId>-->
<!--            <artifactId>zszj-module-product</artifactId>-->
<!--            <version>${revision}</version>-->
<!--        </dependency>-->
<!--        <dependency>-->
<!--            <groupId>cn.zszj</groupId>-->
<!--            <artifactId>zszj-module-promotion</artifactId>-->
<!--            <version>${revision}</version>-->
<!--        </dependency>-->
<!--        <dependency>-->
<!--            <groupId>cn.zszj</groupId>-->
<!--            <artifactId>zszj-module-trade</artifactId>-->
<!--            <version>${revision}</version>-->
<!--        </dependency>-->
<!--        <dependency>-->
<!--            <groupId>cn.zszj</groupId>-->
<!--            <artifactId>zszj-module-statistics</artifactId>-->
<!--            <version>${revision}</version>-->
<!--        </dependency>-->

        <!-- CRM 鐩稿叧妯″潡銆傞粯璁ゆ敞閲婏紝淇濊瘉缂栬瘧閫熷害 -->
<!--        <dependency>-->
<!--            <groupId>cn.zszj</groupId>-->
<!--            <artifactId>zszj-module-crm</artifactId>-->
<!--            <version>${revision}</version>-->
<!--        </dependency>-->

        <!-- ERP 鐩稿叧妯″潡銆傞粯璁ゆ敞閲婏紝淇濊瘉缂栬瘧閫熷害 -->
<!--        <dependency>-->
<!--            <groupId>cn.zszj</groupId>-->
<!--            <artifactId>zszj-module-erp</artifactId>-->
<!--            <version>${revision}</version>-->
<!--        </dependency>-->

        <!-- AI 澶фā鍨嬬浉鍏虫ā鍧椼€傞粯璁ゆ敞閲婏紝淇濊瘉缂栬瘧閫熷害 -->
<!--        <dependency>-->
<!--            <groupId>cn.zszj</groupId>-->
<!--            <artifactId>zszj-module-ai</artifactId>-->
<!--            <version>${revision}</version>-->
<!--        </dependency>-->

        <!-- IoT 鐗╄仈缃戠浉鍏虫ā鍧椼€傞粯璁ゆ敞閲婏紝淇濊瘉缂栬瘧閫熷害 -->
<!--        <dependency>-->
<!--            <groupId>cn.zszj</groupId>-->
<!--            <artifactId>zszj-module-iot-biz</artifactId>-->
<!--            <version>${revision}</version>-->
<!--        </dependency>-->

        <!-- MES 鍒堕€犳墽琛岀郴缁熺浉鍏虫ā鍧椼€傞粯璁ゆ敞閲婏紝淇濊瘉缂栬瘧閫熷害 -->
<!--        <dependency>-->
<!--            <groupId>cn.zszj</groupId>-->
<!--            <artifactId>zszj-module-mes</artifactId>-->
<!--            <version>${revision}</version>-->
<!--        </dependency>-->

        <!-- WMS 浠撳簱绠＄悊妯″潡銆傞粯璁ゆ敞閲婏紝淇濊瘉缂栬瘧閫熷害 -->
<!--        <dependency>-->
<!--            <groupId>cn.zszj</groupId>-->
<!--            <artifactId>zszj-module-wms</artifactId>-->
<!--            <version>${revision}</version>-->
<!--        </dependency>-->

        <!-- HRM 浜哄姏璧勬簮绠＄悊妯″潡銆傞粯璁ゆ敞閲婏紝淇濊瘉缂栬瘧閫熷害 -->
<!--        <dependency>-->
<!--            <groupId>cn.zszj</groupId>-->
<!--            <artifactId>zszj-module-hrm</artifactId>-->
<!--            <version>${revision}</version>-->
<!--        </dependency>-->

        <!-- FMS 璐㈠姟绠＄悊妯″潡銆傞粯璁ゆ敞閲婏紝淇濊瘉缂栬瘧閫熷害 -->
<!--        <dependency>-->
<!--            <groupId>cn.zszj</groupId>-->
<!--            <artifactId>zszj-module-fms</artifactId>-->
<!--            <version>${revision}</version>-->
<!--        </dependency>-->

        <!-- PMS 椤圭洰绠＄悊妯″潡銆傞粯璁ゆ敞閲婏紝淇濊瘉缂栬瘧閫熷害 -->
<!--        <dependency>-->
<!--            <groupId>cn.zszj</groupId>-->
<!--            <artifactId>zszj-module-pms</artifactId>-->
<!--            <version>${revision}</version>-->
<!--        </dependency>-->

        <!-- IM 鐩稿叧妯″潡銆傞粯璁ゆ敞閲婏紝淇濊瘉缂栬瘧閫熷害 -->
<!--        <dependency>-->
<!--            <groupId>cn.zszj</groupId>-->
<!--            <artifactId>zszj-module-im</artifactId>-->
<!--            <version>${revision}</version>-->
<!--        </dependency>-->

        <!-- spring boot 閰嶇疆鎵€闇€渚濊禆 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- 鏈嶅姟淇濋殰鐩稿叧 -->
        <dependency>
            <groupId>cn.zszj</groupId>
            <artifactId>zszj-spring-boot-starter-protection</artifactId>
        </dependency>

    </dependencies>

    <build>
        <!-- 璁剧疆鏋勫缓鐨?jar 鍖呭悕 -->
        <finalName>${project.artifactId}</finalName>
        <plugins>
            <!-- 鎵撳寘 -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>${spring.boot.version}</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>repackage</goal> <!-- 灏嗗紩鍏ョ殑 jar 鎵撳叆鍏朵腑 -->
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

</project>
services/zhongshu-core/zszj-dependencies/pom.xml:29:        <dynamic-datasource
.version>4.5.0</dynamic-datasource.version>
services/zhongshu-core/zszj-dependencies/pom.xml:214:                <artifactI
d>dynamic-datasource-spring-boot3-starter</artifactId> <!-- 澶氭暟鎹簮 -->
services/zhongshu-core/zszj-dependencies/pom.xml:215:                <version>$
{dynamic-datasource.version}</version>
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-biz-tenant\src\t
est\resources\application-fixture.yaml:23:      - cn.zszj.framework.datasource.
config.ZszjDataSourceAutoConfiguration
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-biz-tenant\src\t
est\resources\application-fixture.yaml:60:      - /actuator/health
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-biz-tenant\src\t
est\resources\application-fixture.yaml:68:      - /actuator/health
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\web\core\filter\CacheRequestBodyFilterTest.java:114:    @Di
splayName("琚帓闄?URI锛?actuator/锛夆啋 shouldNotFilter 鎺掗櫎锛屼笉缂撳啿")
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\web\core\filter\CacheRequestBodyFilterTest.java:118:       
 request.setRequestURI("/actuator/health");
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-web\src\main\res
ources\banner.txt:3:Spring Boot Version: ${spring-boot.version}
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-test\src\main\ja
va\cn\zszj\framework\test\config\RedisTestConfiguration.java:4:import org.sprin
gframework.boot.autoconfigure.data.redis.RedisProperties;
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-test\src\main\ja
va\cn\zszj\framework\test\config\RedisTestConfiguration.java:19:@EnableConfigur
ationProperties(RedisProperties.class)
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-test\src\main\ja
va\cn\zszj\framework\test\config\RedisTestConfiguration.java:26:    public Redi
sServer redisServer(RedisProperties properties) throws IOException {
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-test\src\main\ja
va\cn\zszj\framework\test\core\ut\BaseDbUnitTest.java:4:import cn.zszj.framewor
k.datasource.config.ZszjDataSourceAutoConfiguration;
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-test\src\main\ja
va\cn\zszj\framework\test\core\ut\BaseDbUnitTest.java:10:import org.springframe
work.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-test\src\main\ja
va\cn\zszj\framework\test\core\ut\BaseDbUnitTest.java:31:            ZszjDataSo
urceAutoConfiguration.class, // 鑷繁鐨?DB 閰嶇疆绫?services/zhongshu-core/zszj-framew
ork\zszj-spring-boot-starter-test\src\main\java\cn\zszj\framework\test\core\ut\
BaseDbUnitTest.java:32:            DataSourceAutoConfiguration.class, // Spring
 DB 鑷姩閰嶇疆绫?services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-test
\src\main\java\cn\zszj\framework\test\core\ut\BaseDbAndRedisUnitTest.java:4:imp
ort cn.zszj.framework.datasource.config.ZszjDataSourceAutoConfiguration;
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-test\src\main\ja
va\cn\zszj\framework\test\core\ut\BaseDbAndRedisUnitTest.java:13:import org.spr
ingframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-test\src\main\ja
va\cn\zszj\framework\test\core\ut\BaseDbAndRedisUnitTest.java:34:            Zs
zjDataSourceAutoConfiguration.class, // 鑷繁鐨?DB 閰嶇疆绫?services/zhongshu-core/zsz
j-framework\zszj-spring-boot-starter-test\src\main\java\cn\zszj\framework\test\
core\ut\BaseDbAndRedisUnitTest.java:35:            DataSourceAutoConfiguration.
class, // Spring DB 鑷姩閰嶇疆绫?services/zhongshu-core/zszj-framework\zszj-spring-b
oot-starter-biz-tenant\src\test\java\cn\zszj\framework\security\fixture\Securit
yFixtureApplication.java:37: * - DataSourceAutoConfiguration銆丷edisAutoConfigura
tion銆丵uartzAutoConfiguration
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-biz-tenant\src\t
est\java\cn\zszj\framework\security\fixture\SecurityFixtureApplication.java:43:
                org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfi
guration.class,
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-mybatis\銆婅妺閬?Spr
ing Boot 澶氭暟鎹簮锛堣鍐欏垎绂伙級鍏ラ棬銆?md:1:<http://www.iocoder.cn/Spring-Boot/dynamic-da
tasource/?yudao>
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-mybatis\src\main
\resources\META-INF\spring\org.springframework.boot.autoconfigure.AutoConfigura
tion.imports:1:cn.zszj.framework.datasource.config.ZszjDataSourceAutoConfigurat
ion
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-web\src\main\jav
a\cn\zszj\framework\web\core\filter\CacheRequestBodyFilter.java:29:    private 
static final String[] IGNORE_URIS = {"/admin/", "/actuator/"};
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-mybatis\src\main
\java\cn\zszj\framework\datasource\config\ZszjDataSourceAutoConfiguration.java:
20:public class ZszjDataSourceAutoConfiguration {
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-mybatis\pom.xml:
88:            <artifactId>dynamic-datasource-spring-boot3-starter</artifactId>
 <!-- 澶氭暟鎹簮 -->



exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "rg -
n 'actuator|permitAll|anyRequest|management|Admin' services/zhongshu-core/zszj-
framework/zszj-spring-boot-starter-security/src/main services/zhongshu-core/zsz
j-module-infra/src/main/java/cn/zszj/module/infra/framework; rg -n 'TenantConte
xtWebFilter|TenantSecurityWebFilter|ignoreUrls|shouldNotFilter' services/zhongs
hu-core/zszj-framework/zszj-spring-boot-starter-biz-tenant/src/main; Get-Conten
t -Encoding utf8 services/zhongshu-core/zszj-server/src/main/java/cn/zszj/serve
r/ZszjServerApplication.java; Get-Content -Encoding utf8 services/zhongshu-core
/zszj-framework/zszj-spring-boot-starter-mybatis/src/main/java/cn/zszj/framewor
k/mybatis/core/util/JdbcUtils.java; Get-Content -Encoding utf8 scripts/cfg/veri
fy-config-secrets.mjs; Get-Content -Encoding utf8 scripts/ops/verify-deploy-tem
plates.test.mjs; Get-Command docker,nginx,node,java,mvn -ErrorAction SilentlyCo
ntinue | Select-Object Name,Source" in E:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇\.wt\zszj-wt-ops-002
-a
 exited 1 in 932ms:
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/fra
mework\monitor\package-info.java:2: * 浣跨敤 Spring Boot Admin 瀹炵幇绠€鍗曠殑鐩戞帶骞冲彴
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-security/src/mai
n\java\cn\zszj\framework\security\config\ZszjWebSecurityConfigurerAdapter.java:
95:     * anyRequest          |   鍖归厤鎵€鏈夎姹傝矾寰?services/zhongshu-core/zszj-fram
ework/zszj-spring-boot-starter-security/src/main\java\cn\zszj\framework\securit
y\config\ZszjWebSecurityConfigurerAdapter.java:105:     * permitAll           |
   鐢ㄦ埛鍙互浠绘剰璁块棶
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-security/src/mai
n\java\cn\zszj\framework\security\config\ZszjWebSecurityConfigurerAdapter.java:
135:        Multimap<HttpMethod, String> permitAllUrls = getPermitAllUrlsFromAn
notations();
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-security/src/mai
n\java\cn\zszj\framework\security\config\ZszjWebSecurityConfigurerAdapter.java:
141:                    .requestMatchers(HttpMethod.GET, "/*.html", "/*.css", "
/*.js").permitAll()
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-security/src/mai
n\java\cn\zszj\framework\security\config\ZszjWebSecurityConfigurerAdapter.java:
143:                    .requestMatchers(HttpMethod.GET, permitAllUrls.get(Http
Method.GET).toArray(new String[0])).permitAll()
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-security/src/mai
n\java\cn\zszj\framework\security\config\ZszjWebSecurityConfigurerAdapter.java:
144:                    .requestMatchers(HttpMethod.POST, permitAllUrls.get(Htt
pMethod.POST).toArray(new String[0])).permitAll()
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-security/src/mai
n\java\cn\zszj\framework\security\config\ZszjWebSecurityConfigurerAdapter.java:
145:                    .requestMatchers(HttpMethod.PUT, permitAllUrls.get(Http
Method.PUT).toArray(new String[0])).permitAll()
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-security/src/mai
n\java\cn\zszj\framework\security\config\ZszjWebSecurityConfigurerAdapter.java:
146:                    .requestMatchers(HttpMethod.DELETE, permitAllUrls.get(H
ttpMethod.DELETE).toArray(new String[0])).permitAll()
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-security/src/mai
n\java\cn\zszj\framework\security\config\ZszjWebSecurityConfigurerAdapter.java:
147:                    .requestMatchers(HttpMethod.HEAD, permitAllUrls.get(Htt
pMethod.HEAD).toArray(new String[0])).permitAll()
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-security/src/mai
n\java\cn\zszj\framework\security\config\ZszjWebSecurityConfigurerAdapter.java:
148:                    .requestMatchers(HttpMethod.PATCH, permitAllUrls.get(Ht
tpMethod.PATCH).toArray(new String[0])).permitAll()
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-security/src/mai
n\java\cn\zszj\framework\security\config\ZszjWebSecurityConfigurerAdapter.java:
150:                    .requestMatchers(securityProperties.getPermitAllUrls().
toArray(new String[0])).permitAll()
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-security/src/mai
n\java\cn\zszj\framework\security\config\ZszjWebSecurityConfigurerAdapter.java:
156:                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permi
tAll() // WebFlux 寮傛璇锋眰锛屾棤闇€璁よ瘉锛岀洰鐨勶細SSE 鍦烘櫙
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-security/src/mai
n\java\cn\zszj\framework\security\config\ZszjWebSecurityConfigurerAdapter.java:
157:                        .anyRequest().authenticated());
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/fra
mework\monitor\銆婅妺閬?Spring Boot 鐩戞帶宸ュ叿 Admin 鍏ラ棬銆?md:1:<http://www.iocoder.cn/S
pring-Boot/Admin/?yudao>
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/fra
mework\security\config\SecurityConfiguration.java:23:                registry.r
equestMatchers("/v3/api-docs/**").permitAll()
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/fra
mework\security\config\SecurityConfiguration.java:24:                        .r
equestMatchers("/webjars/**").permitAll()
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/fra
mework\security\config\SecurityConfiguration.java:25:                        .r
equestMatchers("/swagger-ui.html").permitAll()
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/fra
mework\security\config\SecurityConfiguration.java:26:                        .r
equestMatchers("/swagger-ui/**").permitAll();
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/fra
mework\security\config\SecurityConfiguration.java:29:                registry.r
equestMatchers("/actuator/health").permitAll()
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/fra
mework\security\config\SecurityConfiguration.java:30:                        .r
equestMatchers("/actuator/health/**").permitAll();
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/fra
mework\security\config\SecurityConfiguration.java:34:                registry.r
equestMatchers(buildAdminApi("/infra/file/*/get/**")).permitAll();
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-security/src/mai
n\java\cn\zszj\framework\security\config\SecurityProperties.java:58:    private
 List<String> permitAllUrls = Collections.emptyList();
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-security/src/mai
n\java\cn\zszj\framework\security\config\AuthorizeRequestsCustomizer.java:22:  
  protected String buildAdminApi(String url) {
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-security/src/mai
n\java\cn\zszj\framework\security\config\AuthorizeRequestsCustomizer.java:23:  
      return webProperties.getAdminApi().getPrefix() + url;
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/fra
mework\monitor\config\AdminServerConfiguration.java:3:import de.codecentric.boo
t.admin.server.config.EnableAdminServer;
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/fra
mework\monitor\config\AdminServerConfiguration.java:22: * Spring Boot Admin Ser
ver 閰嶇疆
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/fra
mework\monitor\config\AdminServerConfiguration.java:24: * 鍖呭惈 Admin Server 鐨勫惎鐢
ㄩ厤缃拰瀹夊叏閰嶇疆
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/fra
mework\monitor\config\AdminServerConfiguration.java:26: * 浣跨敤 HTTP Basic 璁よ瘉淇濇姢
 Admin Server 绔偣锛屼笉褰卞搷鐜版湁鐨?Token 璁よ瘉鏈哄埗
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/fra
mework\monitor\config\AdminServerConfiguration.java:31:@EnableAdminServer
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/fra
mework\monitor\config\AdminServerConfiguration.java:32:@ConditionalOnClass(name
 = "de.codecentric.boot.admin.server.config.AdminServerProperties") // 鐩殑锛氭寜闇€
鍚姩 spring boot admin 鐩戞帶鏈嶅姟
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/fra
mework\monitor\config\AdminServerConfiguration.java:33:public class AdminServer
Configuration {
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/fra
mework\monitor\config\AdminServerConfiguration.java:48:     * Spring Boot Admin
 涓撶敤鐨?InMemoryUserDetailsManager
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/fra
mework\monitor\config\AdminServerConfiguration.java:62:     * Spring Boot Admin
 Server 鐨?SecurityFilterChain
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/fra
mework\monitor\config\AdminServerConfiguration.java:76:                // 浠呭尮閰?
Admin Server 鐨勮矾寰?services/zhongshu-core/zszj-module-infra/src/main/java/cn/zsz
j/module/infra/framework\monitor\config\AdminServerConfiguration.java:82:      
                  .requestMatchers(adminSeverContextPath + "/assets/**").permit
All() // 闈欐€佽祫婧愬厑璁稿尶鍚嶈闂?services/zhongshu-core/zszj-module-infra/src/main/java
/cn/zszj/module/infra/framework\monitor\config\AdminServerConfiguration.java:83
:                        .requestMatchers(adminSeverContextPath + "/login").per
mitAll() // 鐧诲綍椤甸潰鍏佽鍖垮悕璁块棶
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/fra
mework\monitor\config\AdminServerConfiguration.java:84:                        
.dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll() // 寮傛璇锋眰鍏佽
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/fra
mework\monitor\config\AdminServerConfiguration.java:85:                        
.anyRequest().authenticated() // 鍏朵粬璇锋眰闇€瑕佽璇?services/zhongshu-core/zszj-modul
e-infra/src/main/java/cn/zszj/module/infra/framework\monitor\config\AdminServer
Configuration.java:87:                // 琛ㄥ崟鐧诲綍閰嶇疆锛堢敤浜?Admin UI 璁块棶锛?services/z
hongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/framework\mon
itor\config\AdminServerConfiguration.java:91:                        .permitAll
()
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/fra
mework\monitor\config\AdminServerConfiguration.java:98:                // HTTP 
Basic 璁よ瘉锛堢敤浜?Admin Client 娉ㄥ唽锛?services/zhongshu-core/zszj-module-infra/src/ma
in/java/cn/zszj/module/infra/framework\monitor\config\AdminServerConfiguration.
java:104:                                adminSeverContextPath + "/instances", 
// Admin Client 娉ㄥ唽绔偣蹇界暐 CSRF
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/fra
mework\monitor\config\AdminServerConfiguration.java:105:                       
         adminSeverContextPath + "/actuator/**" // Actuator 绔偣蹇界暐 CSRF
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/fra
mework\monitor\config\AdminServerConfiguration.java:109:                       
 // 鐗规畩锛歋pring Boot Admin 鍓嶇鍩轰簬 Vue锛岄渶 unsafe-inline 锛?unsafe-eval 鏀寔鍐呰仈鑴氭湰涓庤
〃杈惧紡
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz-tenant/src/m
ain\java\cn\zszj\framework\tenant\config\ZszjTenantAutoConfiguration.java:16:im
port cn.zszj.framework.tenant.core.security.TenantSecurityWebFilter;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz-tenant/src/m
ain\java\cn\zszj\framework\tenant\config\ZszjTenantAutoConfiguration.java:19:im
port cn.zszj.framework.tenant.core.web.TenantContextWebFilter;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz-tenant/src/m
ain\java\cn\zszj\framework\tenant\config\ZszjTenantAutoConfiguration.java:87:  
  public FilterRegistrationBean<TenantContextWebFilter> tenantContextWebFilter(
) {
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz-tenant/src/m
ain\java\cn\zszj\framework\tenant\config\ZszjTenantAutoConfiguration.java:88:  
      FilterRegistrationBean<TenantContextWebFilter> registrationBean = new Fil
terRegistrationBean<>();
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz-tenant/src/m
ain\java\cn\zszj\framework\tenant\config\ZszjTenantAutoConfiguration.java:89:  
      registrationBean.setFilter(new TenantContextWebFilter());
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz-tenant/src/m
ain\java\cn\zszj\framework\tenant\config\ZszjTenantAutoConfiguration.java:116: 
   public FilterRegistrationBean<TenantSecurityWebFilter> tenantSecurityWebFilt
er(TenantProperties tenantProperties,
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz-tenant/src/m
ain\java\cn\zszj\framework\tenant\config\ZszjTenantAutoConfiguration.java:120: 
       FilterRegistrationBean<TenantSecurityWebFilter> registrationBean = new F
ilterRegistrationBean<>();
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz-tenant/src/m
ain\java\cn\zszj\framework\tenant\config\ZszjTenantAutoConfiguration.java:121: 
       registrationBean.setFilter(new TenantSecurityWebFilter(webProperties, te
nantProperties, getTenantIgnoreUrls(),
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz-tenant/src/m
ain\java\cn\zszj\framework\tenant\config\ZszjTenantAutoConfiguration.java:133: 
       Set<String> ignoreUrls = new HashSet<>();
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz-tenant/src/m
ain\java\cn\zszj\framework\tenant\config\ZszjTenantAutoConfiguration.java:147: 
               ignoreUrls.addAll(entry.getKey().getPatternsCondition().getPatte
rns());
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz-tenant/src/m
ain\java\cn\zszj\framework\tenant\config\ZszjTenantAutoConfiguration.java:150: 
               ignoreUrls.addAll(
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz-tenant/src/m
ain\java\cn\zszj\framework\tenant\config\ZszjTenantAutoConfiguration.java:154: 
       return ignoreUrls;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz-tenant/src/m
ain\java\cn\zszj\framework\tenant\config\TenantProperties.java:47:    private S
et<String> ignoreUrls = new HashSet<>();
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz-tenant/src/m
ain\java\cn\zszj\framework\tenant\core\web\TenantContextWebFilter.java:21:publi
c class TenantContextWebFilter extends OncePerRequestFilter {
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz-tenant/src/m
ain\java\cn\zszj\framework\tenant\core\web\TenantContextWebFilter.java:30:    p
rotected boolean shouldNotFilterAsyncDispatch() {
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz-tenant/src/m
ain\java\cn\zszj\framework\tenant\core\security\TenantSecurityWebFilter.java:35
:public class TenantSecurityWebFilter extends ApiRequestFilter {
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz-tenant/src/m
ain\java\cn\zszj\framework\tenant\core\security\TenantSecurityWebFilter.java:44
:    private final Set<String> ignoreUrls;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz-tenant/src/m
ain\java\cn\zszj\framework\tenant\core\security\TenantSecurityWebFilter.java:51
:    public TenantSecurityWebFilter(WebProperties webProperties,
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz-tenant/src/m
ain\java\cn\zszj\framework\tenant\core\security\TenantSecurityWebFilter.java:53
:                                   Set<String> ignoreUrls,
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz-tenant/src/m
ain\java\cn\zszj\framework\tenant\core\security\TenantSecurityWebFilter.java:58
:        this.ignoreUrls = ignoreUrls;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz-tenant/src/m
ain\java\cn\zszj\framework\tenant\core\security\TenantSecurityWebFilter.java:11
7:            || CollUtil.contains(ignoreUrls, apiUri)) {
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz-tenant/src/m
ain\java\cn\zszj\framework\tenant\core\security\TenantSecurityWebFilter.java:12
6:        for (String url : ignoreUrls) {
package cn.zszj.server;

import cn.zszj.framework.common.util.date.DateUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

/**
 * 椤圭洰鐨勫惎鍔ㄧ被
 *
 * 濡傛灉浣犵鍒板惎鍔ㄧ殑闂锛岃璁ょ湡闃呰 https://doc.iocoder.cn/quick-start/ 鏂囩珷
 * 濡傛灉浣犵鍒板惎鍔ㄧ殑闂锛岃璁ょ湡闃呰 https://doc.iocoder.cn/quick-start/ 鏂囩珷
 * 濡傛灉浣犵鍒板惎鍔ㄧ殑闂锛岃璁ょ湡闃呰 https://doc.iocoder.cn/quick-start/ 鏂囩珷
 *
 * @author 鑺嬮亾婧愮爜
 */
@SuppressWarnings("SpringComponentScan") // 蹇界暐 IDEA 鏃犳硶璇嗗埆 ${zszj.info.base-pa
ckage}
@SpringBootApplication(scanBasePackages = {"${zszj.info.base-package}.server", 
"${zszj.info.base-package}.module"})
public class ZszjServerApplication {

    public static void main(String[] args) {
        // 濡傛灉浣犵鍒板惎鍔ㄧ殑闂锛岃璁ょ湡闃呰 https://doc.iocoder.cn/quick-start/ 鏂囩珷
        // 濡傛灉浣犵鍒板惎鍔ㄧ殑闂锛岃璁ょ湡闃呰 https://doc.iocoder.cn/quick-start/ 鏂囩珷
        // 濡傛灉浣犵鍒板惎鍔ㄧ殑闂锛岃璁ょ湡闃呰 https://doc.iocoder.cn/quick-start/ 鏂囩珷

        // ZS-SEC-009锛氬浐瀹氬簲鐢ㄩ粯璁ゆ椂鍖轰负 GMT+8锛堟帴鍙ｈ竟鐣屾椂闂村悎鍚岀殑閮ㄧ讲绾у熀绾匡級锛屼娇鍏ㄩ」鐩?
        // LocalDateTime.now() 鐢熶骇绔笌鍥哄畾鏃跺尯搴忓垪鍖栧榻愶紝涓嶅彈瀹夸富 JVM/瀹瑰櫒鏃跺尯褰卞搷
        // 锛圖ockerfile 宸茶 ENV TZ=Asia/Shanghai锛屾澶勫厹搴曡８ jar / CI / 鏈湴杩愯锛?
        TimeZone.setDefault(TimeZone.getTimeZone(DateUtils.TIME_ZONE_DEFAULT));

        SpringApplication.run(ZszjServerApplication.class, args);
//        new SpringApplicationBuilder(ZszjServerApplication.class)
//                .applicationStartup(new BufferingApplicationStartup(20480))
//                .run(args);

        // 濡傛灉浣犵鍒板惎鍔ㄧ殑闂锛岃璁ょ湡闃呰 https://doc.iocoder.cn/quick-start/ 鏂囩珷
        // 濡傛灉浣犵鍒板惎鍔ㄧ殑闂锛岃璁ょ湡闃呰 https://doc.iocoder.cn/quick-start/ 鏂囩珷
        // 濡傛灉浣犵鍒板惎鍔ㄧ殑闂锛岃璁ょ湡闃呰 https://doc.iocoder.cn/quick-start/ 鏂囩珷
    }

}
package cn.zszj.framework.mybatis.core.util;

import cn.zszj.framework.common.util.object.ObjectUtils;
import cn.zszj.framework.common.util.spring.SpringUtils;
import cn.zszj.framework.mybatis.core.enums.DbTypeEnum;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.mybatisplus.annotation.DbType;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * JDBC 宸ュ叿绫?
 *
 * @author 鑺嬮亾婧愮爜
 */
public class JdbcUtils {

    /**
     * 鍒ゆ柇杩炴帴鏄惁姝ｇ‘
     *
     * @param url      鏁版嵁婧愯繛鎺?
     * @param username 璐﹀彿
     * @param password 瀵嗙爜
     * @return 鏄惁姝ｇ‘
     */
    public static boolean isConnectionOK(String url, String username, String pa
ssword) {
        try (Connection ignored = DriverManager.getConnection(url, username, pa
ssword)) {
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * 鑾峰緱 URL 瀵瑰簲鐨?DB 绫诲瀷
     *
     * @param url URL
     * @return DB 绫诲瀷
     */
    public static DbType getDbType(String url) {
        return com.baomidou.mybatisplus.extension.toolkit.JdbcUtils.getDbType(u
rl);
    }

    /**
     * 閫氳繃褰撳墠鏁版嵁搴撹繛鎺ヨ幏寰楀搴旂殑 DB 绫诲瀷
     *
     * @return DB 绫诲瀷
     */
    public static DbType getDbType() {
        DataSource dataSource;
        try {
            DynamicRoutingDataSource dynamicRoutingDataSource = SpringUtils.get
Bean(DynamicRoutingDataSource.class);
            dataSource = dynamicRoutingDataSource.determineDataSource();
        } catch (NoSuchBeanDefinitionException e) {
            dataSource = SpringUtils.getBean(DataSource.class);
        }
        try (Connection conn = dataSource.getConnection()) {
            return DbTypeEnum.find(conn.getMetaData().getDatabaseProductName())
;
        } catch (SQLException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * 鍒ゆ柇 JDBC 杩炴帴鏄惁涓?SQLServer 鏁版嵁搴?
     *
     * @param url JDBC 杩炴帴
     * @return 鏄惁涓?SQLServer 鏁版嵁搴?
     */
    public static boolean isSQLServer(String url) {
        DbType dbType = getDbType(url);
        return isSQLServer(dbType);
    }

    /**
     * 鍒ゆ柇 JDBC 杩炴帴鏄惁涓?SQLServer 鏁版嵁搴?
     *
     * @param dbType DB 绫诲瀷
     * @return 鏄惁涓?SQLServer 鏁版嵁搴?
     */
    public static boolean isSQLServer(DbType dbType) {
        return ObjectUtils.equalsAny(dbType, DbType.SQL_SERVER, DbType.SQL_SERV
ER2005);
    }

}
/**
 * ZS-CFG-001.A 閰嶇疆绉樺瘑闂ㄧ锛氭ā鏉挎棤鐪熷疄绉樺瘑銆佸繀濉」鏃犻粯璁ゅ洖閫€銆?
 *
 * 鎵弿鑼冨洿锛氬悗绔?application*.yaml銆侀儴缃叉ā鏉裤€佷袱绔?env 妯℃澘銆?
 * 妫€鏌ラ」锛?
 *  1. 绉樺瘑绫婚敭锛坧assword/secret/token/key 绛夛級鐨勫彇鍊煎彧鑳芥槸锛氱┖銆?{...} 鍗犱綅绗︺€丷EDACTED 鏍
囪锛?
 *  2. 閰嶇疆鏂囦欢涓笉寰楀嚭鐜伴暱鍗佸叚杩涘埗涓诧紙鈮?0锛夈€丷SA/PEM 瀵嗛挜鍧椼€佽秴闀?Base64锛堚墺200 瀛楃锛夛紱
 *  3. 閮ㄧ讲妯℃澘涓殑蹇呭～鐜鍙橀噺蹇呴』涓烘棤榛樿鍊煎崰浣嶇锛堢己閰嶅嵆鍚姩鏄庣‘澶辫触锛夈€?
 * 鐢ㄦ硶锛歯ode scripts/cfg/verify-config-secrets.mjs
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
    const active = !/^\s*#/.test(line); // 娉ㄩ噴琛屼笉鍋氶敭鍊煎垽瀹氾紝浣嗕粛鍋氬瘑閽ュ潡/闀夸覆鍒ゅ畾
    // 1. 绉樺瘑绫婚敭鐨勫彇鍊?
    if (active && SECRET_KEY.test(line)) {
      const value = line.slice(line.search(SECRET_KEY)).split(/[:=]/).slice(1).
join(':').trim().replace(/^["']|["']$/g, '');
      if (value && !PLACEHOLDER.test(value) && !SAFE_LITERALS.test(value) && !v
alue.startsWith('${') && !value.includes('# ')) {
        issues.push({ path: rel, line: i + 1, reason: `绉樺瘑绫婚敭浣跨敤浜嗗瓧闈㈤噺鍙栧€? ${li
ne.trim().slice(0, 60)}` });
      }
    }
    // 2. 瀵嗛挜鍧椾笌闀夸覆
    if (/(BEGIN (RSA )?PRIVATE KEY|BEGIN CERTIFICATE)/.test(line)) {
      issues.push({ path: rel, line: i + 1, reason: '閰嶇疆鏂囦欢涓嚭鐜扮閽?璇佷功鍧? });
    }
    const base64Blob = line.match(/[A-Za-z0-9+/=]{200,}/);
    if (base64Blob) issues.push({ path: rel, line: i + 1, reason: '閰嶇疆鏂囦欢涓嚭鐜拌秴
闀?Base64 涓诧紙鐤戜技瀵嗛挜锛? });
    if (active) {
      const hex = line.match(/\b[0-9a-f]{20,}\b/i);
      if (hex) issues.push({ path: rel, line: i + 1, reason: `鍑虹幇闀垮崄鍏繘鍒朵覆锛堢枒浼煎
嚟鎹?缁熻 ID锛? ${hex[0].slice(0, 12)}鈥 });
    }
  });
}

// 3. 閮ㄧ讲妯℃澘蹇呭～椤瑰繀椤绘棤榛樿鍊硷紙缂洪厤鍗冲惎鍔ㄥけ璐ワ紝瑙?script/config/README.md 鍚堝悓锛?
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
    issues.push({ path: prodRel, reason: `蹇呭～鍙橀噺 ${name} 蹇呴』涓烘棤榛樿鍊煎崰浣嶇 \${${n
ame}}锛堢己閰嶆槑纭け璐ワ級` });
  }
}

console.log(JSON.stringify({ scanned, issueCount: issues.length, issues: issues
.slice(0, 30) }, null, 2));
process.exitCode = issues.length ? 1 : 0;
/**
 * ZS-OPS-002.A 閮ㄧ讲妯℃澘鍙鏌ユ€ф牎楠屸€斺€旂函闈欐€佸崟娴嬨€?
 * 杩愯锛歯ode --test scripts/ops/verify-deploy-templates.test.mjs
 *
 * 娴嬭瘯绛栫暐锛氱敤鍚堟垚妯℃澘鍐呭椹卞姩鍚?check 绾嚱鏁帮紝姝ｅ悜锛堝悎瑙勬ā鏉库啋0 issue锛?
 * 涓庤礋鍚戯紙杩濊妯℃澘鈫掑懡涓搴旇鍒欙級瀵圭収锛岃瘉鏄庢牎楠岄潪绌烘礊銆?
 */
import test from 'node:test';
import assert from 'node:assert/strict';
import {
  checkSecrets,
  checkProbeConsistency,
  checkManagementClosure,
  checkPortExposure,
  checkEnvContract,
  CONTRACT_VARS,
  DEPLOY_TARGETS,
} from './verify-deploy-templates.mjs';

// ---- C1 绉樺瘑鎵弿 ----

test('C1锛氬悎瑙勬ā鏉匡紙鍗犱綅绗?绌哄€硷級涓嶆姤 issue', () => {
  const text = [
    'ZSZJ_DATASOURCE_PASSWORD=${ZSZJ_DATASOURCE_PASSWORD}',
    'ZSZJ_REDIS_PASSWORD=',
    'JAVA_OPTS=-Xms512m -Xmx512m',
  ].join('\n');
  assert.deepEqual(checkSecrets('deploy/.env.example', text), []);
});

test('C1锛氱‖缂栫爜鍙ｄ护瑙﹀彂 issue', () => {
  const text = 'MYSQL_ROOT_PASSWORD=123456\nZSZJ_DATASOURCE_PASSWORD=RealP@ssw0
rd!';
  const issues = checkSecrets('deploy/.env.example', text);
  assert.ok(issues.length >= 2, `搴旇嚦灏戞姤 2 鏉★紝瀹為檯 ${issues.length}`);
  assert.ok(issues.every((i) => i.rule === 'C1-secret'));
});

test('C1锛歅EM 绉侀挜鍧楄Е鍙?issue', () => {
  const text = '-----BEGIN RSA PRIVATE KEY-----\nMIIEow...\n-----END RSA PRIVAT
E KEY-----';
  const issues = checkSecrets('deploy/nginx/tls/server.key', text);
  assert.ok(issues.some((i) => i.rule === 'C1-secret' && /绉侀挜/.test(i.message))
);
});

test('C1锛氶暱鍗佸叚杩涘埗涓诧紙鈮?2锛夎Е鍙?issue', () => {
  const text = 'VUE_APP_BAIDU_CODE=fadc1bd5db1a1d6f581df60a1807f8ab';
  const issues = checkSecrets('deploy/.env.example', text);
  assert.ok(issues.some((i) => i.rule === 'C1-secret' && /鍗佸叚杩涘埗/.test(i.messag
e)));
});

test('C1锛氭敞閲婅涓殑绉樺瘑浠嶈妫€鍑猴紙闃层€屾敞閲婃帀鍗冲畨鍏ㄣ€嶄吉缁匡級', () => {
  const text = '# PASSWORD=SuperSecret123\n# 鍘嗗彶閬楃暀锛屽嬁鍒?;
  const issues = checkSecrets('deploy/.env.example', text);
  assert.ok(issues.length > 0, '娉ㄩ噴琛屼腑鐨勭湡瀹炵瀵嗕篃搴旀姤鍑?);
});
// ---- C2 鎺㈤拡涓€鑷存€?----

test('C2锛歝ompose healthcheck 璺緞涓?actuator include 涓€鑷粹啋閫氳繃', () => {
  const compose = 'healthcheck:\n  test:\n    - "curl -f http://localhost:48080
/actuator/health || exit 1"';
  assert.deepEqual(checkProbeConsistency(compose, 'health'), []);
});

test('C2锛歝ompose 寮曠敤 /actuator/health/liveness 浣?actuator 鏈惎鐢?probes鈫掓姤 issue'
, () => {
  const compose = 'healthcheck:\n  test:\n    - "curl -f http://localhost:48080
/actuator/health/liveness || exit 1"';
  const issues = checkProbeConsistency(compose, 'health');
  assert.ok(issues.some((i) => i.rule === 'C2-probe' && /probes/i.test(i.messag
e)));
});

test('C2锛歝ompose 鏃?healthcheck 瀹氫箟鈫掓姤 issue锛堥儴缃叉ā鏉垮繀椤诲惈鎺㈤拡锛?, () => {
  const compose = 'services:\n  server:\n    image: zszj-server\n    ports:\n  
    - "48080:48080"';
  const issues = checkProbeConsistency(compose, 'health');
  assert.ok(issues.some((i) => i.rule === 'C2-probe' && /healthcheck/i.test(i.m
essage)));
});

test('C2锛歛ctuator include 涓虹┖鏃?compose 浠讳綍 /actuator 鎺㈤拡閮芥姤 issue', () => {
  const compose = 'healthcheck:\n  test:\n    - "curl -f http://localhost:48080
/actuator/health || exit 1"';
  const issues = checkProbeConsistency(compose, '');
  assert.ok(issues.some((i) => i.rule === 'C2-probe'));
});

// ---- C3 绠＄悊璺緞鍏抽棴 ----

test('C3锛歯ginx deny /actuator/ + /admin/ 鈫掗€氳繃', () => {
  const nginx = [
    'location /actuator/ { deny all; return 403; }',
    'location /admin/ { deny all; return 403; }',
    'location / { proxy_pass http://zszj-server:48080; }',
  ].join('\n');
  assert.deepEqual(checkManagementClosure(nginx), []);
});

test('C3锛歯ginx 鏈叧闂?/actuator/ 鈫掓姤 issue', () => {
  const nginx = 'location / {\n    proxy_pass http://zszj-server:48080;\n  }';
  const issues = checkManagementClosure(nginx);
  assert.ok(issues.some((i) => i.rule === 'C3-mgmt-closure' && /actuator/.test(
i.message)));
});

test('C3锛歯ginx 鏈叧闂?/admin/ 鈫掓姤 issue', () => {
  const nginx = 'location /actuator/ {\n    deny all;\n  }\nlocation / {\n    p
roxy_pass http://backend;\n  }';
  const issues = checkManagementClosure(nginx);
  assert.ok(issues.some((i) => i.rule === 'C3-mgmt-closure' && /admin/.test(i.m
essage)));
});
// ---- C4 绔彛鏆撮湶 ----

test('C4锛歝ompose 涓嶆毚闇?DB/Redis 绔彛鈫掗€氳繃', () => {
  const compose = [
    'services:',
    '  postgres:',
    '    image: postgres:16',
    '  redis:',
    '    image: redis:7-alpine',
    '  server:',
    '    ports:',
    '      - "127.0.0.1:48080:48080"',
  ].join('\n');
  assert.deepEqual(checkPortExposure(compose), []);
});

test('C4锛歝ompose 鏆撮湶 5432 鍒?0.0.0.0鈫掓姤 issue', () => {
  const compose = 'services:\n  postgres:\n    ports:\n      - "5432:5432"';
  const issues = checkPortExposure(compose);
  assert.ok(issues.some((i) => i.rule === 'C4-port' && /5432/.test(i.message)))
;
});

test('C4锛歝ompose 鏆撮湶 6379鈫掓姤 issue', () => {
  const compose = 'services:\n  redis:\n    ports:\n      - "6379:6379"';
  const issues = checkPortExposure(compose);
  assert.ok(issues.some((i) => i.rule === 'C4-port' && /6379/.test(i.message)))
;
});

test('C4锛氱粦瀹?127.0.0.1 鐨?DB 绔彛涓嶆姤锛堟湰鍦拌皟璇曞悎娉曪級', () => {
  const compose = 'services:\n  postgres:\n    ports:\n      - "127.0.0.1:5432:
5432"';
  assert.deepEqual(checkPortExposure(compose), []);
});

// ---- C5 鐜鍙橀噺鍚堝悓鍙屽悜涓€鑷?----

test('C5锛?env.example 涓?compose 寮曠敤鐨勫彉閲忓畬鍏ㄨ鐩栧悎鍚屸啋閫氳繃', () => {
  const envExample = CONTRACT_VARS.map((v) => `${v}=`).join('\n');
  const compose = CONTRACT_VARS.map((v) => `      - \${${v}}`).join('\n');
  assert.deepEqual(checkEnvContract(envExample, compose, CONTRACT_VARS), []);
});

test('C5锛氬悎鍚屽彉閲忓湪 .env.example 涓己澶扁啋鎶?issue', () => {
  const envExample = 'ZSZJ_SERVER_PORT=\n';
  const compose = '';
  const issues = checkEnvContract(envExample, compose, CONTRACT_VARS);
  assert.ok(issues.some((i) => i.rule === 'C5-env-contract' && /ZSZJ_DATASOURCE
_URL/.test(i.message)));
});

test('C5锛歝ompose 寮曠敤浜嗗悎鍚屽鍙橀噺鈫掓姤 issue锛堥槻骞界伒鍙橀噺锛?, () => {
  const envExample = CONTRACT_VARS.map((v) => `${v}=`).join('\n');
  const compose = envExample + '\n      - ${GHOST_VAR_NOT_IN_CONTRACT}';
  const issues = checkEnvContract(envExample, compose, CONTRACT_VARS);
  assert.ok(issues.some((i) => i.rule === 'C5-env-contract' && /GHOST_VAR/.test
(i.message)));
});

test('C5锛欳ONTRACT_VARS 鍖呭惈 ZS-ENG-003 宸插畾涔夌殑鍏ㄩ儴蹇呭～椤?, () => {
  const required = ['ZSZJ_DATASOURCE_URL', 'ZSZJ_DATASOURCE_USERNAME', 'ZSZJ_DA
TASOURCE_PASSWORD', 'ZSZJ_REDIS_HOST', 'ZSZJ_REDIS_PASSWORD'];
  for (const v of required) assert.ok(CONTRACT_VARS.includes(v), `鍚堝悓缂?${v}`);
});

// ---- 缁撴瀯瀹屾暣鎬?----

test('DEPLOY_TARGETS 闈炵┖涓旇矾寰勫潎涓?deploy/ 鍓嶇紑', () => {
  assert.ok(DEPLOY_TARGETS.length > 0);
  for (const t of DEPLOY_TARGETS) {
    assert.ok(t.startsWith('services/zhongshu-core/deploy/'), `璺緞搴斾互 deploy/ 涓
烘牴: ${t}`);
  }
});

Name       Source                                                              
                
----       ------                                                              
                
docker.exe C:\Users\Administrator\AppData\Local\Programs\DockerDesktop\resource
s\bin\docker.exe
node.exe   C:\Program Files\nodejs\node.exe                                    
                
java.exe   E:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇\tools\jdk-17.0.20.1+1\bin\java.exe             
                     



exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "node
 --test scripts/ops/verify-deploy-templates.test.mjs; node scripts/ops/verify-d
eploy-templates.mjs; node scripts/ops/verify-deploy-templates.mjs --self-test; 
docker version; docker image ls --format '{{.Repository}}:{{.Tag}}'; rg -n 'spr
ing.boot.version|resources|filtering' services/zhongshu-core/pom.xml; Get-Conte
nt -Encoding utf8 services/zhongshu-core/zszj-framework/zszj-spring-boot-starte
r-mybatis/src/main/java/cn/zszj/framework/mybatis/core/util/MyBatisUtils.java; 
rg -n 'SUPERUSER|NOSUPERUSER|zhongshu_app|rolsuper' services/zhongshu-core/scri
pt services/zhongshu-core/docs -g '*.*'" in E:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇\.wt\zszj-wt-op
s-002-a
 succeeded in 859ms:
鉁?C1锛氬悎瑙勬ā鏉匡紙鍗犱綅绗?绌哄€硷級涓嶆姤 issue (4.5756ms)
鉁?C1锛氱‖缂栫爜鍙ｄ护瑙﹀彂 issue (0.5039ms)
鉁?C1锛歅EM 绉侀挜鍧楄Е鍙?issue (0.3961ms)
鉁?C1锛氶暱鍗佸叚杩涘埗涓诧紙鈮?2锛夎Е鍙?issue (0.2897ms)
鉁?C1锛氭敞閲婅涓殑绉樺瘑浠嶈妫€鍑猴紙闃层€屾敞閲婃帀鍗冲畨鍏ㄣ€嶄吉缁匡級 (0.8068ms)
鉁?C2锛歝ompose healthcheck 璺緞涓?actuator include 涓€鑷粹啋閫氳繃 (0.4566ms)
鉁?C2锛歝ompose 寮曠敤 /actuator/health/liveness 浣?actuator 鏈惎鐢?probes鈫掓姤 issue (0.3
078ms)
鉁?C2锛歝ompose 鏃?healthcheck 瀹氫箟鈫掓姤 issue锛堥儴缃叉ā鏉垮繀椤诲惈鎺㈤拡锛?(0.2191ms)
鉁?C2锛歛ctuator include 涓虹┖鏃?compose 浠讳綍 /actuator 鎺㈤拡閮芥姤 issue (0.2496ms)
鉁?C3锛歯ginx deny /actuator/ + /admin/ 鈫掗€氳繃 (0.4629ms)
鉁?C3锛歯ginx 鏈叧闂?/actuator/ 鈫掓姤 issue (0.6084ms)
鉁?C3锛歯ginx 鏈叧闂?/admin/ 鈫掓姤 issue (0.2066ms)
鉁?C4锛歝ompose 涓嶆毚闇?DB/Redis 绔彛鈫掗€氳繃 (0.2968ms)
鉁?C4锛歝ompose 鏆撮湶 5432 鍒?0.0.0.0鈫掓姤 issue (1.1294ms)
鉁?C4锛歝ompose 鏆撮湶 6379鈫掓姤 issue (0.2138ms)
鉁?C4锛氱粦瀹?127.0.0.1 鐨?DB 绔彛涓嶆姤锛堟湰鍦拌皟璇曞悎娉曪級 (0.1501ms)
鉁?C5锛?env.example 涓?compose 寮曠敤鐨勫彉閲忓畬鍏ㄨ鐩栧悎鍚屸啋閫氳繃 (0.3723ms)
鉁?C5锛氬悎鍚屽彉閲忓湪 .env.example 涓己澶扁啋鎶?issue (0.1887ms)
鉁?C5锛歝ompose 寮曠敤浜嗗悎鍚屽鍙橀噺鈫掓姤 issue锛堥槻骞界伒鍙橀噺锛?(0.237ms)
鉁?C5锛欳ONTRACT_VARS 鍖呭惈 ZS-ENG-003 宸插畾涔夌殑鍏ㄩ儴蹇呭～椤?(0.1283ms)
鉁?DEPLOY_TARGETS 闈炵┖涓旇矾寰勫潎涓?deploy/ 鍓嶇紑 (0.1239ms)
鈩?tests 21
鈩?suites 0
鈩?pass 21
鈩?fail 0
鈩?cancelled 0
鈩?skipped 0
鈩?todo 0
鈩?duration_ms 142.9096
{
  "scanned": 3,
  "issueCount": 0,
  "issues": []
}
scanned=3 issueCount=0
  [PASS] C1 璐熷悜锛堢‖缂栫爜鍙ｄ护锛?  [PASS] C2 璐熷悜锛堟棤 healthcheck锛?  [PASS] C3 璐熷悜锛堢鐞嗚矾
寰勬湭鍏抽棴锛?  [PASS] C4 璐熷悜锛圖B 绔彛鍏紑锛?  [PASS] C5 璐熷悜锛堝菇鐏靛彉閲忥級
{"selfTest":"PASS"}
WARNING: Error loading config file: open C:\Users\Administrator\.docker\config.
json: Access is denied.
Client:
 Version:           29.7.2
 API version:       1.55
 Go version:        go1.26.5
 Git commit:        a7dcaa6
 Built:             Wed Aug  5 18:31:33 2026
 OS/Arch:           windows/amd64
 Context:           default
permission denied while trying to connect to the docker API at npipe:////./pipe
/docker_engine
WARNING: Error loading config file: open C:\Users\Administrator\.docker\config.
json: Access is denied.
permission denied while trying to connect to the docker API at npipe:////./pipe
/docker_engine
52:        <spring.boot.version>3.5.15</spring.boot.version>
90:                                <version>${spring.boot.version}</version>
167:                        <phase>process-resources</phase>
package cn.zszj.framework.mybatis.core.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.func.Func1;
import cn.hutool.core.lang.func.LambdaUtil;
import cn.hutool.core.util.StrUtil;
import cn.zszj.framework.common.pojo.PageParam;
import cn.zszj.framework.common.pojo.SortingField;
import cn.zszj.framework.mybatis.core.enums.DbTypeEnum;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * MyBatis 宸ュ叿绫?
 */
public class MyBatisUtils {

    private static final String MYSQL_ESCAPE_CHARACTER = "`";

    private static final Pattern SAFE_COLUMN_NAME_PATTERN = Pattern.compile("^[
a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)*$");

    private static final String FIND_IN_SET_VALUE_PLACEHOLDER = "#{value}";

    private static final String FIND_IN_SET_COLUMN_PLACEHOLDER = "#{column}";

    public static <T> Page<T> buildPage(PageParam pageParam) {
        return buildPage(pageParam, null);
    }

    public static <T> Page<T> buildPage(PageParam pageParam, Collection<Sorting
Field> sortingFields) {
        // 椤电爜 + 鏁伴噺
        Page<T> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize(
));
        page.setOptimizeJoinOfCountSql(false); // 鍏宠仈 issue锛歨ttps://gitee.com/z
hijiantianya/yudao-cloud/issues/ID2QLL
        // 鎺掑簭瀛楁
        if (CollUtil.isNotEmpty(sortingFields)) {
            for (SortingField sortingField : sortingFields) {
                String columnName = buildSafeOrderColumn(sortingField.getField(
));
                if (columnName == null) {
                    continue;
                }
                page.addOrder(new OrderItem().setAsc(isAscOrder(sortingField.ge
tOrder())).setColumn(columnName));
            }
        }
        return page;
    }

    @SuppressWarnings("PatternVariableCanBeUsed")
    public static <T> void addOrder(Wrapper<T> wrapper, Collection<SortingField
> sortingFields) {
        if (CollUtil.isEmpty(sortingFields)) {
            return;
        }
        if (wrapper instanceof QueryWrapper<T>) {
            QueryWrapper<T> query = (QueryWrapper<T>) wrapper;
            for (SortingField sortingField : sortingFields) {
                String columnName = buildSafeOrderColumn(sortingField.getField(
));
                if (columnName == null) {
                    continue;
                }
                query.orderBy(true, isAscOrder(sortingField.getOrder()), column
Name);
            }
        } else if (wrapper instanceof LambdaQueryWrapper<T>) {
            // LambdaQueryWrapper 涓嶇洿鎺ユ敮鎸佸瓧绗︿覆瀛楁鎺掑簭锛屼娇鐢?last 鏂规硶鎷兼帴 ORDER BY
            LambdaQueryWrapper<T> lambdaQuery = (LambdaQueryWrapper<T>) wrapper
;
            StringBuilder orderBy = new StringBuilder();
            for (SortingField sortingField : sortingFields) {
                String columnName = buildSafeOrderColumn(sortingField.getField(
));
                if (columnName == null) {
                    continue;
                }
                if (StrUtil.isNotEmpty(orderBy)) {
                    orderBy.append(", ");
                }
                orderBy.append(columnName).append(" ").append(getOrderDirection
(sortingField.getOrder()));
            }
            if (StrUtil.isNotEmpty(orderBy)) {
                lambdaQuery.last("ORDER BY " + orderBy);
            }
            // 鍙﹀涓€濊矾锛歨ttps://blog.csdn.net/m0_59084856/article/details/13845
0913
        } else {
            throw new IllegalArgumentException("Unsupported wrapper type: " + w
rapper.getClass().getName());
        }

    }

    public static boolean isAscOrder(String order) {
        return SortingField.ORDER_ASC.equals(order);
    }

    public static String getOrderDirection(String order) {
        return isAscOrder(order) ? "ASC" : "DESC";
    }

    private static String buildSafeOrderColumn(String field) {
        String columnName = StrUtil.toUnderlineCase(field);
        if (StrUtil.isEmpty(columnName) || !SAFE_COLUMN_NAME_PATTERN.matcher(co
lumnName).matches()) {
            return null;
        }
        return columnName;
    }

    /**
     * 灏嗘嫤鎴櫒娣诲姞鍒伴摼涓?
     * 鐢变簬 MybatisPlusInterceptor 涓嶆敮鎸佹坊鍔犳嫤鎴櫒锛屾墍浠ュ彧鑳藉叏閲忚缃?
     *
     * @param interceptor 閾?
     * @param inner       鎷︽埅鍣?
     * @param index       浣嶇疆
     */
    public static void addInterceptor(MybatisPlusInterceptor interceptor, Inner
Interceptor inner, int index) {
        List<InnerInterceptor> inners = new ArrayList<>(interceptor.getIntercep
tors());
        inners.add(index, inner);
        interceptor.setInterceptors(inners);
    }

    /**
     * 鑾峰緱 Table 瀵瑰簲鐨勮〃鍚?
     * <p>
     * 鍏煎 MySQL 杞箟琛ㄥ悕 `t_xxx`
     *
     * @param table 琛?
     * @return 鍘婚櫎杞Щ瀛楃鍚庣殑琛ㄥ悕
     */
    public static String getTableName(Table table) {
        String tableName = table.getName();
        if (tableName.startsWith(MYSQL_ESCAPE_CHARACTER) && tableName.endsWith(
MYSQL_ESCAPE_CHARACTER)) {
            tableName = tableName.substring(1, tableName.length() - 1);
        }
        return tableName;
    }

    /**
     * 鏋勫缓 Column 瀵硅薄
     *
     * @param tableName  琛ㄥ悕
     * @param tableAlias 鍒悕
     * @param column     瀛楁鍚?
     * @return Column 瀵硅薄
     */
    public static Column buildColumn(String tableName, Alias tableAlias, String
 column) {
        if (tableAlias != null) {
            tableName = tableAlias.getName();
        }
        return new Column(tableName + StringPool.DOT + column);
    }

    /**
     * 璺ㄦ暟鎹簱鐨?find_in_set 瀹炵幇
     *
     * @param columnName 瀛楁鍚嶇О
     * @return sql
     */
    public static String findInSet(String columnName) {
        return findInSet(columnName, 0);
    }

    /**
     * 璺ㄦ暟鎹簱鐨?find_in_set 瀹炵幇锛岄€傜敤浜庡悓涓€涓?apply 璇彞涓湁澶氫釜鍙傛暟鐨勫満鏅?
     *
     * @param columnName 瀛楁鍚嶇О
     * @param paramIndex apply 鍙傛暟搴忓彿
     * @return sql
     */
    public static String findInSetWithParamIndex(String columnName, int paramIn
dex) {
        return findInSet(columnName, paramIndex);
    }

    /**
     * 璺ㄦ暟鎹簱鐨?find_in_set 瀹炵幇锛岄€傜敤浜庡悓涓€瀛楁鍖归厤澶氫釜鍙傛暟鐨勫満鏅?
     *
     * 姣忎釜鍙傛暟鐢熸垚涓€涓?find_in_set 鏉′欢锛屽苟浣跨敤 OR 杩炴帴銆?
     *
     * @param columnName 瀛楁鍚嶇О
     * @param values 鍙傛暟闆嗗悎
     * @return sql
     */
    public static String findInSet(String columnName, Collection<?> values) {
        return findInSet(JdbcUtils.getDbType(), columnName, values);
    }

    static String findInSet(DbType dbType, String columnName, Collection<?> val
ues) {
        if (CollUtil.isEmpty(values)) {
            throw new IllegalArgumentException("Values cannot be empty");
        }
        return IntStream.range(0, values.size())
                .mapToObj(index -> findInSet(dbType, columnName, index))
                .collect(Collectors.joining(" OR "));
    }

    private static String findInSet(String columnName, int paramIndex) {
        DbType dbType = JdbcUtils.getDbType();
        return findInSet(dbType, columnName, paramIndex);
    }

    static String findInSet(DbType dbType, String columnName, int paramIndex) {
        if (!isSafeColumnName(columnName)) {
            throw new IllegalArgumentException("Invalid column name: " + column
Name);
        }
        if (paramIndex < 0) {
            throw new IllegalArgumentException("Invalid param index: " + paramI
ndex);
        }
        return DbTypeEnum.getFindInSetTemplate(dbType)
                .replace(FIND_IN_SET_COLUMN_PLACEHOLDER, columnName)
                .replace(FIND_IN_SET_VALUE_PLACEHOLDER, "{" + paramIndex + "}")
;
    }

    private static boolean isSafeColumnName(String columnName) {
        return StrUtil.isNotEmpty(columnName) && SAFE_COLUMN_NAME_PATTERN.match
er(columnName).matches();
    }

    /**
     * 灏嗛┘宄板懡鍚嶈浆鎹负涓嬪垝绾垮懡鍚?
     *
     * 浣跨敤鍦烘櫙锛?
     * 1. <a href="https://gitee.com/zhijiantianya/ruoyi-vue-pro/pulls/1357/fil
es">fix:淇"鍟嗗搧缁熻鑱氬悎鍑芥暟鐨勫埆鍚嶄笌鎺掑簭瀛楁涓嶇"瀵艰嚧鐨?SQL 寮傚父</a>
     *
     * @param func 瀛楁鍚嶅嚱鏁?椹煎嘲鍛藉悕)
     * @return 瀛楁鍚?涓嬪垝绾垮懡鍚?
     */
    public static <T> String toUnderlineCase(Func1<T, ?> func) {
        String fieldName = LambdaUtil.getFieldName(func);
        return StrUtil.toUnderlineCase(fieldName);
    }

}
services/zhongshu-core/docs\閮ㄧ讲妯℃澘涓庢帰閽堣竟鐣?md:74:| `ZSZJ_DATASOURCE_USERNAME` | 
搴旂敤璐﹀彿锛坺hongshu_app锛岄潪 owner锛?| DB-002 |
services/zhongshu-core/docs\BPM寮曟搸琛ㄤ笌涓氬姟鎵╁睍琛ㄨ縼绉昏矗浠诲喅绛?md:15:2. **杩愯鏈熼浂 DDL锛氫綆鏉
冮檺杩愯璐﹀彿锛坄zhongshu_app`锛変互 `database-schema-update=false` 鍚姩銆?*
services/zhongshu-core/docs\BPM寮曟搸琛ㄤ笌涓氬姟鎵╁睍琛ㄨ縼绉昏矗浠诲喅绛?md:43:闃舵 2 `zhongshu_app
` + `SCHEMA_UPDATE=false` + `ASYNC_EXECUTOR=false`锛堟墽琛屽櫒鐢?R70 鏄惧紡
services/zhongshu-core/docs\鏁版嵁搴撶幆澧冩柟妗?md:23:| 搴旂敤璐﹀彿 | `zhongshu_app` | 琛?搴忓垪 
CRUD锛圫ELECT/INSERT/UPDATE/DELETE/USAGE锛夛紝鏃?DDL | 搴旂敤鏃ュ父杩愯锛坄ZSZJ_DATASOURCE_USE
RNAME`锛?|
services/zhongshu-core/docs\鏁版嵁搴撶幆澧冩柟妗?md:31:CREATE ROLE zhongshu_app  LOGIN PA
SSWORD '<鐢辩瀵嗙鐞嗘敞鍏?';
services/zhongshu-core/docs\鏁版嵁搴撶幆澧冩柟妗?md:33:GRANT USAGE ON SCHEMA public TO zh
ongshu_app;
services/zhongshu-core/docs\鏁版嵁搴撶幆澧冩柟妗?md:35:GRANT SELECT, INSERT, UPDATE, DELE
TE ON ALL TABLES IN SCHEMA public TO zhongshu_app;
services/zhongshu-core/docs\鏁版嵁搴撶幆澧冩柟妗?md:36:GRANT USAGE, SELECT ON ALL SEQUENC
ES IN SCHEMA public TO zhongshu_app;
services/zhongshu-core/docs\鏁版嵁搴撶幆澧冩柟妗?md:38:  GRANT SELECT, INSERT, UPDATE, DE
LETE ON TABLES TO zhongshu_app;
services/zhongshu-core/docs\鏁版嵁搴撶幆澧冩柟妗?md:40:  GRANT USAGE, SELECT ON SEQUENCES
 TO zhongshu_app;

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "@'
import { readFileSync } from 'node:fs';
import { checkSecrets, checkProbeConsistency, checkManagementClosure, checkPort
Exposure, checkEnvContract, CONTRACT_VARS } from './scripts/ops/verify-deploy-t
emplates.mjs';
const c = readFileSync('services/zhongshu-core/deploy/docker-compose.deploy.yml
','utf8');
const n = readFileSync('services/zhongshu-core/deploy/nginx/zszj-server.conf','
utf8');
const noServerProbe = c.replace(/    healthcheck:\\r?\\n      test: \\[\"CMD\",
 \"curl\", \"-f\", \"http:\\/\\/localhost:\\"'$'"\\{ZSZJ_SERVER_PORT\\}\\/actua
tor\\/health\\/liveness\"\\][\\s\\S]*?start_period:["'^'"\\r\\n]*/, '');
const commentedDenials = n.replace(/    location \\/(?:actuator|admin)\\/ \\{[\
\s\\S]*?    \\}/g, m => m.split('\\n').map(l => '# '+l).join('\\n'));
const renamedHostPort = c.replace('127.0.0.1:5432:5432', '15432:5432');
const hardCodedRedis = c.replaceAll('"'${ZSZJ_REDIS_PASSWORD}'"', 'SyntheticExa
mplePassword"'!'"');
console.log(JSON.stringify({
  missingServerProbe: checkProbeConsistency(noServerProbe,'health'),
  commentedManagementDenials: checkManagementClosure(commentedDenials),
  publiclyRemappedDatabase: checkPortExposure(renamedHostPort),
  hardcodedRedisCommand: checkSecrets('compose',hardCodedRedis.replace('SPRING_
REDIS_PASSWORD: SyntheticExamplePassword"'!'"', 'SPRING_REDIS_PASSWORD: "'${ZSZ
J_REDIS_PASSWORD}'"')),
  literalPasswordFallback: checkSecrets('compose', 'POSTGRES_PASSWORD: "'${ZSZJ
_DATASOURCE_PASSWORD:-SyntheticExamplePassword!}'"'),
}, null, 2));
'@ | node --input-type=module; Get-Content -Encoding utf8 services/zhongshu-cor
e/docs/鏁版嵁搴撶幆澧冩柟妗?md; Get-Content -Encoding utf8 services/zhongshu-core/zszj-mo
dule-infra/src/main/java/cn/zszj/module/infra/framework/monitor/config/AdminSer
verConfiguration.java; rg -n 'proxy_http_version|http_upgrade|Upgrade|infra/ws'
 services/zhongshu-core/script apps/zhongshu-admin-web/src apps/zhongshu-miniap
p -g '*.conf' -g '*.vue' -g '*.ts' -g '*.js' | Select-Object -First 55; Get-Con
tent -Encoding utf8 services/zhongshu-core/zszj-framework/zszj-spring-boot-star
ter-websocket/src/main/java/cn/zszj/framework/websocket/config/ZszjWebSocketAut
oConfiguration.java" in E:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇\.wt\zszj-wt-ops-002-a
 succeeded in 124ms:
apps/zhongshu-admin-web/src\views\iot\ota\task\OtaTaskDetail.vue:128:          
      @click="handleCancelUpgrade(scope.row)"
apps/zhongshu-admin-web/src\views\iot\ota\task\OtaTaskDetail.vue:252:const hand
leCancelUpgrade = async (record: OtaTaskRecord) => {
apps/zhongshu-admin-web/src\views\mall\promotion\kefu\index.vue:27:  (import.me
ta.env.VITE_BASE_URL + '/infra/ws').replace('http', 'ws') +
apps/zhongshu-miniapp\src\pages-infra\web-socket\index.vue:207:  return `${wsUr
l}/infra/ws?token=${token}`
apps/zhongshu-admin-web/src\views\infra\webSocket\index.vue:82:  (import.meta.e
nv.VITE_BASE_URL + '/infra/ws').replace('http', 'ws') +
apps/zhongshu-admin-web/src\views\im\home\store\websocketStore.ts:217:     * 澶嶇
敤 zszj 鍐呯疆 /infra/ws 閫氶亾锛屽悗绔€氳繃 sendObject(type, content) 涓嬪彂
apps/zhongshu-admin-web/src\views\im\home\store\websocketStore.ts:242:      con
st url = `${this.buildWsUrl()}/infra/ws?token=${refreshToken}`
apps/zhongshu-miniapp\src\pages-im\home\store\websocketStore.ts:65:    return `
${wsBase}/infra/ws?token=${encodeURIComponent(token)}`
apps/zhongshu-miniapp\src\pages-mall\kefu\composables\useKefuWebSocket.ts:1:// 
瀹㈡湇瀹炴椂閾捐矾锛氬鐢?zszj 鍐呯疆 /infra/ws 閫氶亾
apps/zhongshu-miniapp\src\pages-mall\kefu\composables\useKefuWebSocket.ts:26:  
return `${wsBase}/infra/ws?token=${token}`
package cn.zszj.framework.websocket.config;

import cn.zszj.framework.mq.redis.config.ZszjRedisMQConsumerAutoConfiguration;
import cn.zszj.framework.mq.redis.core.RedisMQTemplate;
import cn.zszj.framework.websocket.core.handler.JsonWebSocketMessageHandler;
import cn.zszj.framework.websocket.core.listener.WebSocketMessageListener;
import cn.zszj.framework.websocket.core.security.LoginUserHandshakeInterceptor;
import cn.zszj.framework.websocket.core.security.WebSocketAuthorizeRequestsCust
omizer;
import cn.zszj.framework.websocket.core.sender.kafka.KafkaWebSocketMessageConsu
mer;
import cn.zszj.framework.websocket.core.sender.kafka.KafkaWebSocketMessageSende
r;
import cn.zszj.framework.websocket.core.sender.local.LocalWebSocketMessageSende
r;
import cn.zszj.framework.websocket.core.sender.rabbitmq.RabbitMQWebSocketMessag
eConsumer;
import cn.zszj.framework.websocket.core.sender.rabbitmq.RabbitMQWebSocketMessag
eSender;
import cn.zszj.framework.websocket.core.sender.redis.RedisWebSocketMessageConsu
mer;
import cn.zszj.framework.websocket.core.sender.redis.RedisWebSocketMessageSende
r;
import cn.zszj.framework.websocket.core.sender.rocketmq.RocketMQWebSocketMessag
eConsumer;
import cn.zszj.framework.websocket.core.sender.rocketmq.RocketMQWebSocketMessag
eSender;
import cn.zszj.framework.websocket.core.session.WebSocketSessionHandlerDecorato
r;
import cn.zszj.framework.websocket.core.session.WebSocketSessionManager;
import cn.zszj.framework.websocket.core.session.WebSocketSessionManagerImpl;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationPropertie
s;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;

/**
 * WebSocket 鑷姩閰嶇疆
 *
 * @author xingyu4j
 */
@AutoConfiguration(before = ZszjRedisMQConsumerAutoConfiguration.class) // befo
re ZszjRedisMQConsumerAutoConfiguration 鐨勫師鍥犳槸锛岄渶瑕佷繚璇?RedisWebSocketMessageCons
umer 鍏堝垱寤猴紝鎵嶈兘鍒涘缓 RedisMessageListenerContainer
@EnableWebSocket // 寮€鍚?websocket
@ConditionalOnProperty(prefix = "zszj.websocket", value = "enable", matchIfMiss
ing = true) // 鍏佽浣跨敤 zszj.websocket.enable=false 绂佺敤 websocket
@EnableConfigurationProperties(WebSocketProperties.class)
public class ZszjWebSocketAutoConfiguration {

    @Bean
    public WebSocketConfigurer webSocketConfigurer(HandshakeInterceptor[] hands
hakeInterceptors,
                                                   WebSocketHandler webSocketHa
ndler,
                                                   WebSocketProperties webSocke
tProperties) {
        return registry -> registry
                // 娣诲姞 WebSocketHandler
                .addHandler(webSocketHandler, webSocketProperties.getPath())
                .addInterceptors(handshakeInterceptors)
                // 鍏佽璺ㄥ煙锛屽惁鍒欏墠绔繛鎺ヤ細鐩存帴鏂紑
                .setAllowedOriginPatterns("*");
    }

    @Bean
    public HandshakeInterceptor handshakeInterceptor() {
        return new LoginUserHandshakeInterceptor();
    }

    @Bean
    public WebSocketHandler webSocketHandler(WebSocketSessionManager sessionMan
ager,
                                             List<? extends WebSocketMessageLis
tener<?>> messageListeners) {
        // 1. 鍒涘缓 JsonWebSocketMessageHandler 瀵硅薄锛屽鐞嗘秷鎭?
        JsonWebSocketMessageHandler messageHandler = new JsonWebSocketMessageHa
ndler(messageListeners);
        // 2. 鍒涘缓 WebSocketSessionHandlerDecorator 瀵硅薄锛屽鐞嗚繛鎺?
        return new WebSocketSessionHandlerDecorator(messageHandler, sessionMana
ger);
    }

    @Bean
    public WebSocketSessionManager webSocketSessionManager() {
        return new WebSocketSessionManagerImpl();
    }

    @Bean
    public WebSocketAuthorizeRequestsCustomizer webSocketAuthorizeRequestsCusto
mizer(WebSocketProperties webSocketProperties) {
        return new WebSocketAuthorizeRequestsCustomizer(webSocketProperties);
    }

    // ==================== Sender 鐩稿叧 ====================

    @Configuration
    @ConditionalOnProperty(prefix = "zszj.websocket", name = "sender-type", hav
ingValue = "local")
    public class LocalWebSocketMessageSenderConfiguration {

        @Bean
        public LocalWebSocketMessageSender localWebSocketMessageSender(WebSocke
tSessionManager sessionManager) {
            return new LocalWebSocketMessageSender(sessionManager);
        }

    }

    @Configuration
    @ConditionalOnProperty(prefix = "zszj.websocket", name = "sender-type", hav
ingValue = "redis")
    public class RedisWebSocketMessageSenderConfiguration {

        @Bean
        public RedisWebSocketMessageSender redisWebSocketMessageSender(WebSocke
tSessionManager sessionManager,
                                                                       RedisMQT
emplate redisMQTemplate) {
            return new RedisWebSocketMessageSender(sessionManager, redisMQTempl
ate);
        }

        @Bean
        public RedisWebSocketMessageConsumer redisWebSocketMessageConsumer(
                RedisWebSocketMessageSender redisWebSocketMessageSender) {
            return new RedisWebSocketMessageConsumer(redisWebSocketMessageSende
r);
        }

    }

    @Configuration
    @ConditionalOnProperty(prefix = "zszj.websocket", name = "sender-type", hav
ingValue = "rocketmq")
    public class RocketMQWebSocketMessageSenderConfiguration {

        @Bean
        public RocketMQWebSocketMessageSender rocketMQWebSocketMessageSender(
                WebSocketSessionManager sessionManager, RocketMQTemplate rocket
MQTemplate,
                @Value("${zszj.websocket.sender-rocketmq.topic}") String topic)
 {
            return new RocketMQWebSocketMessageSender(sessionManager, rocketMQT
emplate, topic);
        }

        @Bean
        public RocketMQWebSocketMessageConsumer rocketMQWebSocketMessageConsume
r(
                RocketMQWebSocketMessageSender rocketMQWebSocketMessageSender) 
{
            return new RocketMQWebSocketMessageConsumer(rocketMQWebSocketMessag
eSender);
        }

    }

    @Configuration
    @ConditionalOnProperty(prefix = "zszj.websocket", name = "sender-type", hav
ingValue = "rabbitmq")
    public class RabbitMQWebSocketMessageSenderConfiguration {

        @Bean
        public RabbitMQWebSocketMessageSender rabbitMQWebSocketMessageSender(
                WebSocketSessionManager sessionManager, RabbitTemplate rabbitTe
mplate,
                TopicExchange websocketTopicExchange) {
            return new RabbitMQWebSocketMessageSender(sessionManager, rabbitTem
plate, websocketTopicExchange);
        }

        @Bean
        public RabbitMQWebSocketMessageConsumer rabbitMQWebSocketMessageConsume
r(
                RabbitMQWebSocketMessageSender rabbitMQWebSocketMessageSender) 
{
            return new RabbitMQWebSocketMessageConsumer(rabbitMQWebSocketMessag
eSender);
        }

        /**
         * 鍒涘缓 Topic Exchange
         */
        @Bean
        public TopicExchange websocketTopicExchange(@Value("${zszj.websocket.se
nder-rabbitmq.exchange}") String exchange) {
            return new TopicExchange(exchange,
                    true,  // durable: 鏄惁鎸佷箙鍖?
                    false);  // exclusive: 鏄惁鎺掑畠
        }

    }

    @Configuration
    @ConditionalOnProperty(prefix = "zszj.websocket", name = "sender-type", hav
ingValue = "kafka")
    public class KafkaWebSocketMessageSenderConfiguration {

        @Bean
        public KafkaWebSocketMessageSender kafkaWebSocketMessageSender(
                WebSocketSessionManager sessionManager, KafkaTemplate<Object, O
bject> kafkaTemplate,
                @Value("${zszj.websocket.sender-kafka.topic}") String topic) {
            return new KafkaWebSocketMessageSender(sessionManager, kafkaTemplat
e, topic);
        }

        @Bean
        public KafkaWebSocketMessageConsumer kafkaWebSocketMessageConsumer(
                KafkaWebSocketMessageSender kafkaWebSocketMessageSender) {
            return new KafkaWebSocketMessageConsumer(kafkaWebSocketMessageSende
r);
        }

    }

}

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "Get-
Content -Encoding utf8 services/zhongshu-core/zszj-framework/zszj-spring-boot-s
tarter-mybatis/src/main/java/cn/zszj/framework/mybatis/core/util/JdbcUtils.java
 | Select-Object -Skip 55 -First 32; Get-Content -Encoding utf8 services/zhongs
hu-core/zszj-framework/zszj-spring-boot-starter-mybatis/src/main/java/cn/zszj/f
ramework/mybatis/core/util/MyBatisUtils.java | Select-Object -First 5; Get-Cont
ent -Encoding utf8 services/zhongshu-core/zszj-framework/zszj-spring-boot-start
er-mybatis/src/main/java/cn/zszj/framework/mybatis/config/ZszjMybatisAutoConfig
uration.java; rg -n 'spring-boot-admin|zszj-spring-boot-starter-websocket|codeg
en.db-schemas' services/zhongshu-core/zszj-module-infra/pom.xml services/zhongs
hu-core/zszj-module-infra/src/main; "'$files=@('"'services/zhongshu-core/deploy
/docker-compose.deploy.yml','services/zhongshu-core/deploy/nginx/zszj-server.co
nf','scripts/ops/verify-deploy-templates.mjs'); foreach("'$p in $files){ Write-
Output "--- $p"; $i=0; Get-Content -Encoding utf8 -LiteralPath $p | ForEach-Obj
ect { $i++; '"'{0,4} {1}' -f "'$i,$_ } }' in E:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇\.wt\zszj-wt-o
ps-002-a
 succeeded in 30ms:
   1 # ZS-OPS-002.A 閮ㄧ讲妯℃澘鈥斺€旂嫭绔嬫祴璇曠幆澧冪紪鎺?
   2 # 鍓嶇疆锛歓S-ENG-002锛圝DK17 鍩虹嚎锛夈€乑S-ENG-003锛堢幆澧冮厤缃垎绂伙級銆乑S-ENG-005锛坅ctuator 鏀剁
揣锛?
   3 #
   4 # 浣跨敤鏂瑰紡锛?
   5 #   cp .env.example .env   # 濉叆鐪熷疄鍊硷紙.env 宸插湪 .gitignore锛?
   6 #   docker compose -f docker-compose.deploy.yml up -d
   7 #   docker compose -f docker-compose.deploy.yml down  # 鎾ゅ洖
   8 #
   9 # 杈圭晫锛氭湰鏂囦欢鏄劚鏁忔ā鏉匡紝涓嶅惈浠讳綍鐪熷疄绉樺瘑銆?
  10 # 鎵€鏈夋晱鎰熼厤缃€氳繃 ${ZSZJ_*} 鐜鍙橀噺娉ㄥ叆锛堣 .env.example 鍚堝悓琛級銆?
  11 # 绂佹鍦ㄦ鏂囦欢涓嚭鐜扮‖缂栫爜鍙ｄ护銆佺湡瀹炴暟鎹簱鍦板潃鎴?TLS 绉侀挜銆?
  12 
  13 version: "3.9"
  14 
  15 networks:
  16   zszj-internal:
  17     driver: bridge
  18     internal: false   # 闇€瑕佸缃戞媺闀滃儚锛涚敓浜у彲鏀逛负 internal: true
  19 
  20 services:
  21   # ---- PostgreSQL锛圖-08锛氫粎 PG锛岀 MySQL锛?---
  22   postgres:
  23     image: postgres:16-alpine
  24     container_name: zszj-postgres
  25     restart: unless-stopped
  26     environment:
  27       POSTGRES_DB: zhongshu
  28       POSTGRES_USER: ${ZSZJ_DATASOURCE_USERNAME}
  29       POSTGRES_PASSWORD: ${ZSZJ_DATASOURCE_PASSWORD}
  30     volumes:
  31       - zszj-pgdata:/var/lib/postgresql/data
  32     networks:
  33       - zszj-internal
  34     # 涓嶆毚闇插叕寮€绔彛锛涗粎 127.0.0.1 渚涙湰鍦拌皟璇曪紙绉婚櫎鍚庝粎瀹瑰櫒鍐呯綉鍙揪锛?
  35     ports:
  36       - "127.0.0.1:5432:5432"
  37     healthcheck:
  38       test: ["CMD-SHELL", "pg_isready -U ${ZSZJ_DATASOURCE_USERNAME} -d zh
ongshu"]
  39       interval: 10s
  40       timeout: 5s
  41       retries: 5
  42 
  43   # ---- Redis ----
  44   redis:
  45     image: redis:7-alpine
  46     container_name: zszj-redis
  47     restart: unless-stopped
  48     command: redis-server --requirepass ${ZSZJ_REDIS_PASSWORD}
  49     volumes:
  50       - zszj-redisdata:/data
  51     networks:
  52       - zszj-internal
  53     # 涓嶆毚闇插叕寮€绔彛
  54     ports:
  55       - "127.0.0.1:6379:6379"
  56     healthcheck:
  57       test: ["CMD", "redis-cli", "-a", "${ZSZJ_REDIS_PASSWORD}", "ping"]
  58       interval: 10s
  59       timeout: 5s
  60       retries: 5
  61 
  62   # ---- zszj-server锛圫pring Boot锛孞DK17锛?---
  63   zszj-server:
  64     image: zszj-server:latest   # 鐢?CI 鏋勫缓锛岀姝㈠湪妯℃澘涓‖缂栫爜鐪熷疄 registry 鍦板潃
  65     container_name: zszj-server
  66     restart: unless-stopped
  67     depends_on:
  68       postgres:
  69         condition: service_healthy
  70       redis:
  71         condition: service_healthy
  72     environment:
  73       SPRING_PROFILES_ACTIVE: ${ZSZJ_PROFILE}
  74       SERVER_PORT: ${ZSZJ_SERVER_PORT}
  75       SPRING_DATASOURCE_URL: ${ZSZJ_DATASOURCE_URL}
  76       SPRING_DATASOURCE_USERNAME: ${ZSZJ_DATASOURCE_USERNAME}
  77       SPRING_DATASOURCE_PASSWORD: ${ZSZJ_DATASOURCE_PASSWORD}
  78       SPRING_REDIS_HOST: ${ZSZJ_REDIS_HOST}
  79       SPRING_REDIS_PORT: ${ZSZJ_REDIS_PORT}
  80       SPRING_REDIS_PASSWORD: ${ZSZJ_REDIS_PASSWORD}
  81       # actuator 鎺㈤拡锛歓S-ENG-005 宸插皢 exposure.include 鏀剁揣涓?health
  82       MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED: "true"
  83       # Spring Boot Admin 瀹㈡埛绔鐢紙ZS-ENG-005锛?
  84       SPRING_BOOT_ADMIN_CLIENT_ENABLED: "false"
  85     networks:
  86       - zszj-internal
  87     # 浠呮毚闇茬粰 nginx锛堝唴閮ㄧ綉缁滐級锛屼笉缁戝畾瀹夸富鏈哄叕寮€绔彛
  88     expose:
  89       - "${ZSZJ_SERVER_PORT}"
  90     # 鎺㈤拡杈圭晫锛歭iveness/readiness 鐢?actuator health 鎻愪緵
  91     # ZS-ENG-005 宸查厤缃?management.endpoints.web.exposure.include: health
  92     healthcheck:
  93       test: ["CMD", "curl", "-f", "http://localhost:${ZSZJ_SERVER_PORT}/ac
tuator/health/liveness"]
  94       interval: 30s
  95       timeout: 10s
  96       retries: 3
  97       start_period: 60s   # JVM 鍐峰惎鍔ㄥ闄愭湡
  98 
  99   # ---- Nginx锛圱LS 缁堢粨 + 鍙嶅悜浠ｇ悊锛?---
 100   nginx:
 101     image: nginx:1.27-alpine
 102     container_name: zszj-nginx
 103     restart: unless-stopped
 104     depends_on:
 105       zszj-server:
 106         condition: service_healthy
 107     ports:
 108       # 浠呮毚闇?HTTPS 鍒板涓绘満锛汬TTP 80 鐢ㄤ簬 Let's Encrypt 楠岃瘉锛屾寜闇€寮€鏀?
 109       - "443:443"
 110       # - "80:80"   # TLS 璇佷功鐢宠鏃朵复鏃跺紑鍚紝鑾峰緱璇佷功鍚庢敞閲婃帀
 111     volumes:
 112       - ./nginx/zszj-server.conf:/etc/nginx/conf.d/default.conf:ro
 113       # TLS 璇佷功鎸傝浇锛堣矾寰勪负瀹瑰櫒鍐呰矾寰勶紝瀹夸富鏈鸿矾寰勫湪 .env 涓厤缃級
 114       # 璇佷功鏂囦欢绂佹鍏ラ暅鍍忥紱閫氳繃 volume mount 娉ㄥ叆
 115       - ${ZSZJ_TLS_CERT_PATH:-./tls/fullchain.pem}:/etc/nginx/tls/fullchai
n.pem:ro
 116       - ${ZSZJ_TLS_KEY_PATH:-./tls/privkey.pem}:/etc/nginx/tls/privkey.pem
:ro
 117     networks:
 118       - zszj-internal
 119     healthcheck:
 120       test: ["CMD", "curl", "-f", "http://localhost/nginx-health"]
 121       interval: 30s
 122       timeout: 5s
 123       retries: 3
 124 
 125 volumes:
 126   zszj-pgdata:
 127     driver: local
 128   zszj-redisdata:
 129     driver: local
--- services/zhongshu-core/deploy/nginx/zszj-server.conf
   1 # ZS-OPS-002.A 鍙嶅悜浠ｇ悊閰嶇疆鈥斺€擳LS 缁堢粨 + 绠＄悊璺緞鍏抽棴
   2 # 鍓嶇疆锛歓S-ENG-005锛坅ctuator 鏀剁揣锛宔xposure.include: health锛?
   3 #
   4 # 杈圭晫锛?
   5 #   1. /actuator/* 鍜?/admin/* 蹇呴』鍦ㄦ澶勫叧闂紙deny all锛?
   6 #   2. 涓嶅緱灏嗗唴閮ㄧ鐞嗙鍙ｏ紙postgres/redis锛夋毚闇插埌鍏綉
   7 #   3. TLS 璇佷功閫氳繃 volume mount 娉ㄥ叆锛岀姝㈢‖缂栫爜绉侀挜鍐呭
   8 
   9 # ---- HTTP 鈫?HTTPS 閲嶅畾鍚戯紙鎸夐渶鍚敤锛?---
  10 # server {
  11 #     listen 80;
  12 #     server_name _;
  13 #     # Let's Encrypt HTTP-01 楠岃瘉璺緞锛堢敵璇疯瘉涔︽椂涓存椂鍚敤锛?
  14 #     location /.well-known/acme-challenge/ { root /var/www/certbot; }
  15 #     location / { return 301 https://$host$request_uri; }
  16 # }
  17 
  18 # ---- HTTPS 涓荤珯 ----
  19 server {
  20     listen 443 ssl http2;
  21     server_name _;
  22 
  23     # TLS 璇佷功锛堢敱 volume mount 浠庡涓绘満娉ㄥ叆锛岃矾寰勮 .env.example锛?
  24     ssl_certificate     /etc/nginx/tls/fullchain.pem;
  25     ssl_certificate_key /etc/nginx/tls/privkey.pem;
  26     ssl_protocols       TLSv1.2 TLSv1.3;
  27     ssl_ciphers         ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM
-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384;
  28     ssl_prefer_server_ciphers off;
  29     ssl_session_cache   shared:SSL:10m;
  30     ssl_session_timeout 10m;
  31 
  32     # 瀹夊叏鍝嶅簲澶?
  33     add_header Strict-Transport-Security "max-age=63072000; includeSubDoma
ins" always;
  34     add_header X-Content-Type-Options "nosniff" always;
  35     add_header X-Frame-Options "SAMEORIGIN" always;
  36 
  37     # ---- 鏁忔劅绠＄悊璺緞鍏抽棴锛圸S-ENG-005 + ZS-OPS-002.A锛?---
  38     # actuator 绔偣锛歓S-ENG-005 宸插皢 exposure.include 鏀剁揣涓?health锛?
  39     # 浣嗗嵆渚?health 绔偣涔熶笉搴斿澶栨毚闇诧紙浠呬緵瀹瑰櫒鍐?healthcheck 璋冪敤锛?
  40     location /actuator/ {
  41         deny all;
  42         return 403;
  43     }
  44 
  45     # admin 绠＄悊鍚庡彴璺緞锛氬唴閮ㄨ繍缁翠笓鐢紝涓嶅澶?
  46     location /admin/ {
  47         deny all;
  48         return 403;
  49     }
  50 
  51     # ---- nginx 鑷韩鍋ュ悍妫€鏌ワ紙compose healthcheck 璋冪敤锛?---
  52     location /nginx-health {
  53         access_log off;
  54         return 200 "ok\n";
  55         add_header Content-Type text/plain;
  56     }
  57 
  58     # ---- 涓氬姟娴侀噺鍙嶅悜浠ｇ悊 ----
  59     location / {
  60         proxy_pass http://zszj-server:48080;
  61         proxy_set_header Host $host;
  62         proxy_set_header X-Real-IP $remote_addr;
  63         proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
  64         proxy_set_header X-Forwarded-Proto $scheme;
  65         proxy_connect_timeout 10s;
  66         proxy_read_timeout 60s;
  67         proxy_send_timeout 60s;
  68         # 绂佹浠ｇ悊璁块棶鍐呴儴绠＄悊绔彛
  69         # postgres:5432 / redis:6379 浠呭湪 zszj-internal 缃戠粶鍐呭彲杈撅紝姝ゅ涓嶈浆鍙?
  70     }
  71 
  72     # ---- 瀹㈡埛绔姹備綋闄愬埗锛堥槻澶ф枃浠舵敾鍑伙級----
  73     client_max_body_size 50m;
  74 }
--- scripts/ops/verify-deploy-templates.mjs
   1 /**
   2  * ZS-OPS-002.A 閮ㄧ讲妯℃澘鍙鏌ユ€ф牎楠岋紙绾潤鎬侊紝鏈湴涓?CI 鍚屼竴鍏ュ彛锛夈€?
   3  *
   4  * 鏍￠獙椤癸細
   5  *   C1 妯℃澘鏃犵湡瀹炵瀵嗭紙澶嶇敤 ZS-CFG-001.A 鍒ゅ畾鍙ｅ緞锛?
   6  *   C2 鎺㈤拡璺緞涓庡悗绔?actuator 鏆撮湶涓€鑷?
   7  *   C3 鏁忔劅绠＄悊璺緞鍦ㄥ弽浠ｄ腑纭疄鍏抽棴
   8  *   C4 鍙嶄唬/compose 涓嶆硠闇插唴閮ㄧ鐞嗙鍙?
   9  *   C5 妯℃澘寮曠敤鐨勭幆澧冨彉閲忎笌鍚堝悓琛ㄥ弻鍚戜竴鑷?
  10  *
  11  * 鐢ㄦ硶锛歯ode scripts/ops/verify-deploy-templates.mjs [--self-test]
  12  * 閫€鍑虹爜锛?=鍏ㄩ儴閫氳繃锛?=瀛樺湪 issue锛?=鑴氭湰鑷韩寮傚父
  13  */
  14 import { readFileSync, existsSync } from 'node:fs';
  15 import { fileURLToPath } from 'node:url';
  16 import { join } from 'node:path';
  17 
  18 const root = fileURLToPath(new URL('../../', import.meta.url));
  19 
  20 export const CONTRACT_VARS = [
  21   'ZSZJ_SERVER_PORT',
  22   'ZSZJ_DATASOURCE_URL',
  23   'ZSZJ_DATASOURCE_USERNAME',
  24   'ZSZJ_DATASOURCE_PASSWORD',
  25   'ZSZJ_REDIS_HOST',
  26   'ZSZJ_REDIS_PORT',
  27   'ZSZJ_REDIS_PASSWORD',
  28   'ZSZJ_PROFILE',
  29   // TLS 璇佷功鎸傝浇璺緞锛堣繍缁村繀濉紱璇佷功鍐呭绂佹鍏ュ簱锛屼粎閰嶇疆璺緞锛?
  30   'ZSZJ_TLS_CERT_PATH',
  31   'ZSZJ_TLS_KEY_PATH',
  32 ];
  33 
  34 export const DEPLOY_TARGETS = [
  35   'services/zhongshu-core/deploy/docker-compose.deploy.yml',
  36   'services/zhongshu-core/deploy/.env.example',
  37   'services/zhongshu-core/deploy/nginx/zszj-server.conf',
  38 ];
  39 
  40 // ---- C1 ----
  41 
  42 const SECRET_KEY_RE = /(password|passwd|secret|token|access.?key|secret.?k
ey|api.?key|private.?key)\s*[:=]/i;
  43 const PLACEHOLDER_RE = /^\$\{[^}]*\}$/;
  44 const SAFE_LITERALS_RE = /^(redacted.*|xx|''|""|)$/i;
  45 
  46 export function checkSecrets(relPath, text) {
  47   const issues = [];
  48   const lines = text.split(/\r?\n/);
  49   lines.forEach((line, i) => {
  50     const lineNo = i + 1;
  51     if (/BEGIN (RSA |EC |DSA )?PRIVATE KEY|BEGIN CERTIFICATE/.test(line)) 
{
  52       issues.push({ rule: 'C1-secret', path: relPath, line: lineNo, messag
e: '妯℃澘涓嚭鐜扮閽?璇佷功鍧? });
  53       return;
  54     }
  55     const hexMatch = line.match(/\b[0-9a-fA-F]{32,}\b/);
  56     if (hexMatch && !/commit|sha|checksum|hash/i.test(line)) {
  57       issues.push({ rule: 'C1-secret', path: relPath, line: lineNo, messag
e: '鍑虹幇闀垮崄鍏繘鍒朵覆锛堢枒浼煎嚟鎹級: ' + hexMatch[0].slice(0, 12) });
  58       return;
  59     }
  60     if (SECRET_KEY_RE.test(line)) {
  61       const eqIdx = line.search(/[:=]/);
  62       const value = line.slice(eqIdx + 1).trim().replace(/^["']|["']$/g, '
');
  63       if (value && !PLACEHOLDER_RE.test(value) && !SAFE_LITERALS_RE.test(v
alue) && !value.startsWith('${')) {
  64         issues.push({ rule: 'C1-secret', path: relPath, line: lineNo, mess
age: '绉樺瘑绫婚敭浣跨敤浜嗗瓧闈㈤噺鍙栧€? ' + line.trim().slice(0, 60) });
  65       }
  66     }
  67   });
  68   return issues;
  69 }
  70 
  71 
  72 // ---- C2 鎺㈤拡涓€鑷存€?----
  73 
  74 /**
  75  * 鏍￠獙 compose healthcheck 鎺㈤拡璺緞涓?actuator 鏆撮湶涓€鑷淬€?
  76  * @param {string} composeText docker-compose 妯℃澘鍐呭
  77  * @param {string} actuatorInclude actuator exposure.include 鍊硷紙濡?'health'
 鎴?'health,info'锛?
  78  */
  79 export function checkProbeConsistency(composeText, actuatorInclude) {
  80   const issues = [];
  81   const exposed = actuatorInclude.split(',').map((s) => s.trim()).filter(B
oolean);
  82 
  83   // compose 蹇呴』鍚?healthcheck 瀹氫箟
  84   if (!/healthcheck/i.test(composeText)) {
  85     issues.push({ rule: 'C2-probe', path: 'deploy/docker-compose.deploy.ym
l', line: 0, message: '閮ㄧ讲妯℃澘缂哄皯 healthcheck 瀹氫箟锛堟帰閽堣竟鐣屾湭澹版槑锛? });
  86     return issues;
  87   }
  88 
  89   // 鎻愬彇 compose 涓紩鐢ㄧ殑 /actuator/ 璺緞
  90   const probePaths = [...composeText.matchAll(/\/actuator\/([\w/-]+)/g)].m
ap((m) => m[1]);
  91   for (const p of probePaths) {
  92     const endpoint = p.split('/')[0]; // health/liveness 鈫?health
  93     if (!exposed.includes(endpoint)) {
  94       issues.push({ rule: 'C2-probe', path: 'deploy/docker-compose.deploy.
yml', line: 0, message: `鎺㈤拡寮曠敤 /actuator/${p} 浣?actuator exposure.include 鏈惈 
"${endpoint}"` });
  95     }
  96     // liveness/readiness 瀛愯矾寰勯渶瑕?probes.enabled锛堟敮鎸?Spring property 鍜?env
 var 涓ょ鍐欐硶锛?
  97     if (/^health\/(liveness|readiness)/.test(p) &&
  98         !/(probes\.enabled|MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED).*tru
e/i.test(composeText)) {
  99       issues.push({ rule: 'C2-probe', path: 'deploy/docker-compose.deploy.
yml', line: 0, message: `鎺㈤拡寮曠敤 /actuator/${p} 浣嗘湭澹版槑 MANAGEMENT_ENDPOINT_HEALT
H_PROBES_ENABLED=true` });
 100     }
 101   }
 102   return issues;
 103 }
 104 
 105 // ---- C3 绠＄悊璺緞鍏抽棴 ----
 106 
 107 /** 鏁忔劅绠＄悊璺緞鍓嶇紑锛堝繀椤诲湪鍙嶄唬涓叧闂級 */
 108 const SENSITIVE_PATHS = ['/actuator/', '/admin/'];
 109 
 110 /**
 111  * 鏍￠獙 nginx 鍙嶄唬閰嶇疆鏄惁鍏抽棴浜嗘晱鎰熺鐞嗚矾寰勩€?
 112  * @param {string} nginxText nginx 閰嶇疆鍐呭
 113  */
 114 export function checkManagementClosure(nginxText) {
 115   const issues = [];
 116   for (const path of SENSITIVE_PATHS) {
 117     // 蹇呴』瀛樺湪 location 鍧椾笖鍚?deny all 鎴?return 403
 118     const locRe = new RegExp(`location\\s+${path.replace(/\//g, '\\/')}[^{
]*\\{[^}]*(deny\\s+all|return\\s+403)`, 's');
 119     if (!locRe.test(nginxText)) {
 120       issues.push({ rule: 'C3-mgmt-closure', path: 'deploy/nginx/zszj-serv
er.conf', line: 0, message: `鍙嶅悜浠ｇ悊鏈叧闂晱鎰熺鐞嗚矾寰?${path}锛堥渶 deny all 鎴?return 4
03锛塦 });
 121     }
 122   }
 123   return issues;
 124 }
 125 // ---- C4 绔彛鏆撮湶 ----
 126 
 127 /** 鍐呴儴鏈嶅姟绔彛锛堜笉寰楀叕寮€鏆撮湶锛?*/
 128 const INTERNAL_PORTS = [5432, 6379, 3306, 27017, 5672, 15672];
 129 
 130 /**
 131  * 鏍￠獙 compose 涓嶅叕寮€鏆撮湶鍐呴儴鏈嶅姟绔彛銆?
 132  * 缁戝畾 127.0.0.1 鐨勬槧灏勮涓烘湰鍦拌皟璇曞悎娉曪紝涓嶆姤銆?
 133  * @param {string} composeText docker-compose 妯℃澘鍐呭
 134  */
 135 export function checkPortExposure(composeText) {
 136   const issues = [];
 137   // 鍖归厤 ports 鏄犲皠琛岋細- "HOST:CONTAINER" 鎴?- "IP:HOST:CONTAINER"
 138   const portLines = [...composeText.matchAll(/-\s*["']?([^"'\n]+:\d+)["']?
/g)];
 139   for (const m of portLines) {
 140     const mapping = m[1].trim();
 141     const parts = mapping.split(':');
 142     // 鍒ゆ柇鏄惁缁戝畾浜?loopback
 143     const boundIp = parts.length >= 3 ? parts[0] : '0.0.0.0';
 144     const hostPort = Number(parts.length >= 3 ? parts[1] : parts[0]);
 145     if (INTERNAL_PORTS.includes(hostPort) && !/^127\./.test(boundIp)) {
 146       issues.push({ rule: 'C4-port', path: 'deploy/docker-compose.deploy.y
ml', line: 0, message: `鍐呴儴鏈嶅姟绔彛 ${hostPort} 鍏紑鏆撮湶锛堢粦瀹?${boundIp}锛夛紝搴旂Щ闄ゆ垨闄愬埗
涓?127.0.0.1` });
 147     }
 148   }
 149   return issues;
 150 }
 151 
 152 // ---- C5 鐜鍙橀噺鍚堝悓鍙屽悜涓€鑷?----
 153 
 154 /**
 155  * 鏍￠獙 .env.example 涓?compose 寮曠敤鐨勭幆澧冨彉閲忎笌鍚堝悓琛ㄥ弻鍚戜竴鑷淬€?
 156  * @param {string} envExampleText .env.example 鍐呭
 157  * @param {string} composeText docker-compose 妯℃澘鍐呭
 158  * @param {string[]} contractVars 鍚堝悓鍙橀噺鍒楄〃
 159  */
 160 export function checkEnvContract(envExampleText, composeText, contractVars
) {
 161   const issues = [];
 162   const contractSet = new Set(contractVars);
 163 
 164   // 鏂瑰悜 1锛氬悎鍚屽彉閲忓繀椤诲湪 .env.example 涓嚭鐜?
 165   const envKeys = new Set(
 166     [...envExampleText.matchAll(/^([A-Z_][A-Z0-9_]*)\s*=/gm)].map((m) => m
[1])
 167   );
 168   for (const v of contractVars) {
 169     if (!envKeys.has(v)) {
 170       issues.push({ rule: 'C5-env-contract', path: 'deploy/.env.example', 
line: 0, message: `鍚堝悓鍙橀噺 ${v} 鍦?.env.example 涓己澶盽 });
 171     }
 172   }
 173 
 174   // 鏂瑰悜 2锛歝ompose 涓紩鐢ㄧ殑 ${VAR} 蹇呴』鍦ㄥ悎鍚岃〃涓紙闃插菇鐏靛彉閲忥級
 175   const composeVars = new Set(
 176     [...composeText.matchAll(/\$\{([A-Z_][A-Z0-9_]*)(?::[^}]*)?\}/g)].map(
(m) => m[1])
 177   );
 178   for (const v of composeVars) {
 179     if (!contractSet.has(v)) {
 180       issues.push({ rule: 'C5-env-contract', path: 'deploy/docker-compose.
deploy.yml', line: 0, message: `compose 寮曠敤浜嗗悎鍚屽鍙橀噺 ${v}锛堝菇鐏靛彉閲忥級` });
 181     }
 182   }
 183 
 184   return issues;
 185 }
 186 // ---- 涓诲叆鍙ｏ細鍏ㄩ噺鎵弿 ----
 187 
 188 /** 璇诲彇 actuator exposure.include锛堜粠 application-prod.yaml 鎴?application.y
aml锛?*/
 189 function readActuatorInclude() {
 190   const prodPath = join(root, 'services/zhongshu-core/script/config/applic
ation-prod.yaml');
 191   const mainPath = join(root, 'services/zhongshu-core/zszj-server/src/main
/resources/application.yaml');
 192   for (const p of [prodPath, mainPath]) {
 193     if (!existsSync(p)) continue;
 194     const text = readFileSync(p, 'utf8');
 195     const m = text.match(/exposure:\s*\n\s*include:\s*(.+)/);
 196     if (m) return m[1].replace(/#.*$/, '').trim();
 197   }
 198   return 'health';
 199 }
 200 
 201 function runAll() {
 202   const issues = [];
 203   let scanned = 0;
 204 
 205   // C1: 绉樺瘑鎵弿锛堝叏閮ㄩ儴缃叉ā鏉匡級
 206   for (const rel of DEPLOY_TARGETS) {
 207     const abs = join(root, rel);
 208     if (!existsSync(abs)) {
 209       issues.push({ rule: 'C0-missing', path: rel, line: 0, message: `閮ㄧ讲妯
℃澘鏂囦欢涓嶅瓨鍦? ${rel}` });
 210       continue;
 211     }
 212     scanned++;
 213     const text = readFileSync(abs, 'utf8');
 214     issues.push(...checkSecrets(rel, text));
 215   }
 216 
 217   // C2: 鎺㈤拡涓€鑷存€?
 218   const composeRel = 'services/zhongshu-core/deploy/docker-compose.deploy.
yml';
 219   const composeAbs = join(root, composeRel);
 220   if (existsSync(composeAbs)) {
 221     const composeText = readFileSync(composeAbs, 'utf8');
 222     const actuatorInclude = readActuatorInclude();
 223     issues.push(...checkProbeConsistency(composeText, actuatorInclude));
 224   }
 225 
 226   // C3: 绠＄悊璺緞鍏抽棴
 227   const nginxRel = 'services/zhongshu-core/deploy/nginx/zszj-server.conf';
 228   const nginxAbs = join(root, nginxRel);
 229   if (existsSync(nginxAbs)) {
 230     issues.push(...checkManagementClosure(readFileSync(nginxAbs, 'utf8')))
;
 231   }
 232 
 233   // C4: 绔彛鏆撮湶
 234   if (existsSync(composeAbs)) {
 235     issues.push(...checkPortExposure(readFileSync(composeAbs, 'utf8')));
 236   }
 237 
 238   // C5: 鐜鍙橀噺鍚堝悓
 239   const envRel = 'services/zhongshu-core/deploy/.env.example';
 240   const envAbs = join(root, envRel);
 241   if (existsSync(envAbs) && existsSync(composeAbs)) {
 242     issues.push(...checkEnvContract(
 243       readFileSync(envAbs, 'utf8'),
 244       readFileSync(composeAbs, 'utf8'),
 245       CONTRACT_VARS,
 246     ));
 247   }
 248 
 249   return { scanned, issueCount: issues.length, issues: issues.slice(0, 50)
 };
 250 }
 251 
 252 // ---- --self-test 璐熷悜瀵圭収 ----
 253 
 254 function selfTest() {
 255   // 鐢ㄥ凡鐭ヨ繚瑙勫唴瀹归┍鍔ㄥ悇 check锛岃瘉鏄庢柇瑷€鍙け璐ワ紙闈炵┖娲烇級
 256   const results = [];
 257   const bad = checkSecrets('test.env', 'DB_PASSWORD=hunter2');
 258   results.push(['C1 璐熷悜锛堢‖缂栫爜鍙ｄ护锛?, bad.length > 0]);
 259 
 260   const noProbe = checkProbeConsistency('services:\n  x:\n    image: y', '
health');
 261   results.push(['C2 璐熷悜锛堟棤 healthcheck锛?, noProbe.length > 0]);
 262 
 263   const openNginx = checkManagementClosure('location / { proxy_pass http:/
/x; }');
 264   results.push(['C3 璐熷悜锛堢鐞嗚矾寰勬湭鍏抽棴锛?, openNginx.length > 0]);
 265 
 266   const openPort = checkPortExposure('ports:\n      - "5432:5432"');
 267   results.push(['C4 璐熷悜锛圖B 绔彛鍏紑锛?, openPort.length > 0]);
 268 
 269   const ghost = checkEnvContract('A=\n', '${GHOST}', CONTRACT_VARS);
 270   results.push(['C5 璐熷悜锛堝菇鐏靛彉閲忥級', ghost.length > 0]);
 271 
 272   const allPass = results.every(([, ok]) => ok);
 273   for (const [name, ok] of results) console.error(`  [${ok ? 'PASS' : 'FAI
L'}] ${name}`);
 274   return allPass;
 275 }
 276 
 277 // ---- CLI ----
 278 
 279 const invokedDirectly = process.argv[1] && process.argv[1].replaceAll('\\'
, '/').endsWith('scripts/ops/verify-deploy-templates.mjs');
 280 if (invokedDirectly) {
 281   try {
 282     if (process.argv.includes('--self-test')) {
 283       const ok = selfTest();
 284       console.log(JSON.stringify({ selfTest: ok ? 'PASS' : 'FAIL' }));
 285       process.exitCode = ok ? 0 : 1;
 286     } else {
 287       const report = runAll();
 288       console.log(JSON.stringify(report, null, 2));
 289       console.error(`scanned=${report.scanned} issueCount=${report.issueCo
unt}`);
 290       process.exitCode = report.issueCount ? 1 : 0;
 291     }
 292   } catch (e) {
 293     console.error(`deploy template check failed: ${e.message}`);
 294     process.exitCode = 2;
 295   }
 296 }

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'Get-
Content -Encoding utf8 services/zhongshu-core/zszj-framework/zszj-spring-boot-s
tarter-mybatis/src/main/java/cn/zszj/framework/mybatis/config/IdTypeEnvironment
PostProcessor.java; Get-Content -Encoding utf8 services/zhongshu-core/zszj-modu
le-infra/pom.xml | Select-Object -Skip 79 -First 25; Get-ChildItem -LiteralPath
 $env:USERPROFILE'"\\.m2\\repository\\org\\springframework\\boot\\spring-boot-a
utoconfigure -ErrorAction SilentlyContinue | Select-Object Name; Get-ChildItem 
-LiteralPath "'$env:USERPROFILE'"\\.m2\\repository\\com\\baomidou\\dynamic-data
source-spring-boot3-starter -ErrorAction SilentlyContinue | Select-Object Name;
 git status --short; Get-Item -LiteralPath "'$env:USERPROFILE'"\\.codex\\AGENTS
.md | Select-Object FullName,Length" in E:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇\.wt\zszj-wt-ops-00
2-a
 succeeded in 118ms:
warning: unable to access 'C:\Users\Administrator/.config/git/ignore': Permissi
on denied
warning: unable to access 'C:\Users\Administrator/.config/git/ignore': Permissi
on denied
?? _zs_d_commit_msg.txt
Name  
----  
2.7.18
3.5.15
4.5.0 
      



exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'Add-
Type -AssemblyName System.IO.Compression.FileSystem; $p = Join-Path $env:USERPR
OFILE '"'.m2\\repository\\org\\springframework\\boot\\spring-boot-autoconfigure
\\3.5.15\\spring-boot-autoconfigure-3.5.15.jar'; if (Test-Path -LiteralPath "'$
p) { $z = [IO.Compression.ZipFile]::OpenRead($p); try { $r = [IO.StreamReader]:
:new($z.GetEntry('"'META-INF/spring-configuration-metadata.json').Open()); try 
{ "'$m = $r.ReadToEnd() | ConvertFrom-Json; $m.properties | Where-Object { $_.n
ame -match '"'"'^spring'"\\.(data\\.)?redis\\.(host|port|password)"'$'"' } | Se
lect-Object name,defaultValue,sourceType | Format-Table -AutoSize } finally { "
'$r.Dispose() } } finally { $z.Dispose() } }; Get-ChildItem -LiteralPath $env:U
SERPROFILE'"\\.m2\\repository\\com\\baomidou -Directory | Where-Object { "'$_.N
ame -match '"'dynamic-datasource' } | Select-Object Name" in E:\浼楀涔嬪AI璧嬭兘骞冲彴搴
曞骇\.wt\zszj-wt-ops-002-a
 succeeded in 107ms:

name                       defaultValue sourceType                             
                          
----                       ------------ ----------                             
                          
spring.data.redis.host     localhost    org.springframework.boot.autoconfigure.
data.redis.RedisProperties
spring.data.redis.password              org.springframework.boot.autoconfigure.
data.redis.RedisProperties
spring.data.redis.port     6379         org.springframework.boot.autoconfigure.
data.redis.RedisProperties
spring.redis.host                                                              
                          
spring.redis.password                                                          
                          
spring.redis.port                                                              
                          



Name                                   
----                                   
dynamic-datasource-creator             
dynamic-datasource-spring              
dynamic-datasource-spring-boot-common  
dynamic-datasource-spring-boot3-starter



exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "rg -
n 'properties-migrator|spring\\.redis|SPRING_REDIS_' services/zhongshu-core --g
lob '*.java' --glob 'pom.xml'; Add-Type -AssemblyName System.IO.Compression.Fil
eSystem; "'$p = Join-Path $env:USERPROFILE '"'.m2\\repository\\com\\baomidou\\d
ynamic-datasource-spring-boot-common\\4.5.0\\dynamic-datasource-spring-boot-com
mon-4.5.0.jar'; if(Test-Path -LiteralPath "'$p) { $z=[IO.Compression.ZipFile]::
OpenRead($p); try { $r=[IO.StreamReader]::new($z.GetEntry('"'META-INF/spring-co
nfiguration-metadata.json').Open()); try { "'$m=$r.ReadToEnd() | ConvertFrom-Js
on; $m.properties | Where-Object { $_.name -match '"'datasource|primary|enabled
' } | Select-Object -First 15 name,defaultValue | Format-Table -AutoSize } fina
lly { "'$r.Dispose() } } finally { $z.Dispose() } }' in E:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇\.w
t\zszj-wt-ops-002-a
 succeeded in 249ms:

name                                                          defaultValue
----                                                          ------------
spring.datasource.dynamic.aop.allowed-public-only                     True
spring.datasource.dynamic.aop.enabled                                 True
spring.datasource.dynamic.aop.order                                       
spring.datasource.dynamic.atomikos.borrow-connection-timeout              
spring.datasource.dynamic.atomikos.default-isolation-level                
spring.datasource.dynamic.atomikos.maintenance-interval                   
spring.datasource.dynamic.atomikos.max-idle-time                          
spring.datasource.dynamic.atomikos.max-lifetime                           
spring.datasource.dynamic.atomikos.max-pool-size                          
spring.datasource.dynamic.atomikos.min-pool-size                          
spring.datasource.dynamic.atomikos.reap-timeout                           
spring.datasource.dynamic.atomikos.test-query                             
spring.datasource.dynamic.beecp.borrow-semaphore-size                     
spring.datasource.dynamic.beecp.connect-properties                        
spring.datasource.dynamic.beecp.connection-factory-class-name             



exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '$env
:ZSZJ_SERVER_PORT='"'48080'; "'$env:ZSZJ_PROFILE='"'prod'; "'$env:ZSZJ_DATASOUR
CE_URL='"'jdbc:postgresql://postgres:5432/zhongshu'; "'$env:ZSZJ_DATASOURCE_USE
RNAME='"'review_example'; "'$env:ZSZJ_DATASOURCE_PASSWORD='"'SyntheticExamplePa
ssword'; "'$env:ZSZJ_REDIS_HOST='"'redis'; "'$env:ZSZJ_REDIS_PORT='"'6379'; "'$
env:ZSZJ_REDIS_PASSWORD='"'Synthetic Example Password'; docker compose -f servi
ces/zhongshu-core/deploy/docker-compose.deploy.yml config --format json | Conve
rtFrom-Json | Select-Object -ExpandProperty services | Select-Object -ExpandPro
perty redis | Select-Object command,healthcheck | ConvertTo-Json -Depth 5" in E
:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇\.wt\zszj-wt-ops-002-a
 succeeded in 153ms:
WARNING: Error loading config file: open C:\Users\Administrator\.docker\config.
json: Access is denied.
time="2026-09-15T08:48:41+08:00" level=warning msg="E:\\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇\\.wt\
\zszj-wt-ops-002-a\\services\\zhongshu-core\\deploy\\docker-compose.deploy.yml:
 the attribute `version` is obsolete, it will be ignored, please remove it to a
void potential confusion"
{
    "command":  [
                    "redis-server",
                    "--requirepass",
                    "Synthetic",
                    "Example",
                    "Password"
                ],
    "healthcheck":  {
                        "test":  [
                                     "CMD",
                                     "redis-cli",
                                     "-a",
                                     "Synthetic Example Password",
                                     "ping"
                                 ],
                        "timeout":  "5s",
                        "interval":  "10s",
                        "retries":  5
                    }
}

codex
All 21 tests and the supplied static checks pass, but the deployment contains c
onnection-configuration, privilege, healthcheck, and proxy defects. Additional 
negative cases also demonstrate false negatives in the validation checks; Docke
r runtime verification was unavailable.

Full review comments:

- [P1] Bind credentials to the dynamic datasource properties 鈥?E:\浼楀涔嬪AI璧嬭兘骞冲
彴搴曞骇\.wt\zszj-wt-ops-002-a\services\zhongshu-core\deploy\docker-compose.deploy.
yml:75-77
  With the provided Dockerfile and default `prod` profile, these variables do n
ot populate `spring.datasource.dynamic.datasource.master`, which the installed 
dynamic-datasource starter uses. The external `script/config/application-prod.y
aml` is neither packaged nor mounted, so filling `.env` still leaves the master
 datasource unconfigured. Load that configuration and pass its `ZSZJ_*` variabl
es, or inject the dynamic datasource properties explicitly.

- [P1] Use Spring Boot 3 Redis property names 鈥?E:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇\.wt\zszj-w
t-ops-002-a\services\zhongshu-core\deploy\docker-compose.deploy.yml:78-80
  This repository uses Spring Boot 3.5.15, whose `RedisProperties` binds to `sp
ring.data.redis`, not `spring.redis`. These variables therefore leave Redis con
figured as unauthenticated `localhost:6379` inside the application container ra
ther than the supplied Redis service. Use `SPRING_DATA_REDIS_HOST`, `SPRING_DAT
A_REDIS_PORT`, and `SPRING_DATA_REDIS_PASSWORD` so valid deployment credentials
 actually reach the Redis client.

- [P1] Separate PostgreSQL bootstrap and application credentials 鈥?E:\浼楀涔嬪AI璧
嬭兘骞冲彴搴曞骇\.wt\zszj-wt-ops-002-a\services\zhongshu-core\deploy\docker-compose.dep
loy.yml:28-29
  On first initialization of an empty volume, the official PostgreSQL image cre
ates `POSTGRES_USER` as a superuser and database owner. Reusing the application
 credentials here consequently gives `zhongshu_app` unrestricted database privi
leges, contradicting the documented application/owner separation. Use separate 
bootstrap credentials and provision a restricted application role before the se
rver connects.

- [P2] Preserve the Redis password as one command argument 鈥?E:\浼楀涔嬪AI璧嬭兘骞冲彴搴
曞骇\.wt\zszj-wt-ops-002-a\services\zhongshu-core\deploy\docker-compose.deploy.ym
l:48-48
  When a valid password contains whitespace or shell quotes, Compose tokenizes 
this interpolated command string instead of preserving the password. For exampl
e, `Synthetic Example Password` becomes three arguments, while the healthcheck 
correctly retains one argument; Redis then fails to start. Use the exec-list fo
rm with the password as a separate, quoted YAML element.

- [P2] Probe an enabled Nginx listener 鈥?E:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇\.wt\zszj-wt-ops-0
02-a\services\zhongshu-core\deploy\docker-compose.deploy.yml:120-120
  This request targets HTTP port 80, but the mounted replacement configuration 
only enables HTTPS port 443; its HTTP server is entirely commented out. Consequ
ently Nginx remains unhealthy even when certificates and proxying work correctl
y. Provide a loopback HTTP health listener or probe HTTPS with appropriate cert
ificate handling.

- [P2] Align the upstream port with the deployment contract 鈥?E:\浼楀涔嬪AI璧嬭兘骞冲彴
搴曞骇\.wt\zszj-wt-ops-002-a\services\zhongshu-core\deploy\nginx\zszj-server.conf:
60-60
  If an operator changes the supported `ZSZJ_SERVER_PORT` setting, Compose upda
tes the application listener and its healthcheck, but this statically mounted c
onfiguration still forwards to port 48080. The server can become healthy while 
every proxied business request returns 502. Render the upstream from the same p
ort setting or enforce a fixed application port throughout the template.

- [P2] Forward WebSocket upgrades through the proxy 鈥?E:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇\.wt\
zszj-wt-ops-002-a\services\zhongshu-core\deploy\nginx\zszj-server.conf:61-64
  The backend enables `/infra/ws`, and existing admin-web and miniapp clients c
onnect to that endpoint. Nginx 1.27 defaults to HTTP/1.0 upstream requests and 
does not forward the hop-by-hop upgrade headers, so WebSocket handshakes fail t
hrough this otherwise functional REST proxy. Configure HTTP/1.1 and the appropr
iate `Upgrade` and `Connection` headers for WebSocket traffic.

- [P2] Classify internal services by their container port 鈥?E:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞
骇\.wt\zszj-wt-ops-002-a\scripts\ops\verify-deploy-templates.mjs:143-145
  A public mapping such as `15432:5432` currently produces no issue because the
 check compares the host port with `INTERNAL_PORTS`. PostgreSQL is still public
ly reachable, just on a different host port, so this bypasses C4 through ordina
ry port remapping. Classify the service using the container-side port and indep
endently validate its host binding.

- [P2] Exclude commented directives from management checks 鈥?E:\浼楀涔嬪AI璧嬭兘骞冲彴搴
曞骇\.wt\zszj-wt-ops-002-a\scripts\ops\verify-deploy-templates.mjs:118-119
  Commenting out both denial blocks in the supplied Nginx configuration still m
akes `checkManagementClosure` return an empty issue list: this regex matches th
e commented `location`, `deny`, and `return` text. Nginx then forwards those pa
ths through `location /`, including the anonymously accessible health endpoint.
 Check active directives rather than raw comment text and add a commented-block
 negative test.

- [P2] Require the application service's healthcheck 鈥?E:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇\.wt
\zszj-wt-ops-002-a\scripts\ops\verify-deploy-templates.mjs:83-86
  Removing only the `zszj-server` healthcheck from the supplied Compose file st
ill passes C2 because PostgreSQL, Redis, and Nginx retain their own healthcheck
s, and an empty actuator-path list is accepted. Nginx nevertheless depends on `
zszj-server` becoming healthy, so the validated template cannot satisfy that st
artup dependency. Validate the application service's active healthcheck and req
uire its probe path explicitly.

- [P2] Check credentials supplied through Redis command arguments 鈥?E:\浼楀涔嬪AI
璧嬭兘骞冲彴搴曞骇\.wt\zszj-wt-ops-002-a\scripts\ops\verify-deploy-templates.mjs:60-64
  C1 accepts a literal password in `command: redis-server --requirepass Synthet
icPassword` and in the `redis-cli -a` healthcheck because neither line contains
 a secret-named mapping key. These are credential-bearing locations in the newl
y supplied template, so hardcoding them bypasses the advertised secret check. V
alidate those authentication arguments as well as environment mappings and add 
corresponding negative tests.
All 21 tests and the supplied static checks pass, but the deployment contains connection-configuration, privilege, healthcheck, and proxy defects. Additional negative cases also demonstrate false negatives in the validation checks; Docker runtime verification was unavailable.

Full review comments:

- [P1] Bind credentials to the dynamic datasource properties 鈥?E:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇\.wt\zszj-wt-ops-002-a\services\zhongshu-core\deploy\docker-compose.deploy.yml:75-77
  With the provided Dockerfile and default `prod` profile, these variables do not populate `spring.datasource.dynamic.datasource.master`, which the installed dynamic-datasource starter uses. The external `script/config/application-prod.yaml` is neither packaged nor mounted, so filling `.env` still leaves the master datasource unconfigured. Load that configuration and pass its `ZSZJ_*` variables, or inject the dynamic datasource properties explicitly.

- [P1] Use Spring Boot 3 Redis property names 鈥?E:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇\.wt\zszj-wt-ops-002-a\services\zhongshu-core\deploy\docker-compose.deploy.yml:78-80
  This repository uses Spring Boot 3.5.15, whose `RedisProperties` binds to `spring.data.redis`, not `spring.redis`. These variables therefore leave Redis configured as unauthenticated `localhost:6379` inside the application container rather than the supplied Redis service. Use `SPRING_DATA_REDIS_HOST`, `SPRING_DATA_REDIS_PORT`, and `SPRING_DATA_REDIS_PASSWORD` so valid deployment credentials actually reach the Redis client.

- [P1] Separate PostgreSQL bootstrap and application credentials 鈥?E:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇\.wt\zszj-wt-ops-002-a\services\zhongshu-core\deploy\docker-compose.deploy.yml:28-29
  On first initialization of an empty volume, the official PostgreSQL image creates `POSTGRES_USER` as a superuser and database owner. Reusing the application credentials here consequently gives `zhongshu_app` unrestricted database privileges, contradicting the documented application/owner separation. Use separate bootstrap credentials and provision a restricted application role before the server connects.

- [P2] Preserve the Redis password as one command argument 鈥?E:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇\.wt\zszj-wt-ops-002-a\services\zhongshu-core\deploy\docker-compose.deploy.yml:48-48
  When a valid password contains whitespace or shell quotes, Compose tokenizes this interpolated command string instead of preserving the password. For example, `Synthetic Example Password` becomes three arguments, while the healthcheck correctly retains one argument; Redis then fails to start. Use the exec-list form with the password as a separate, quoted YAML element.

- [P2] Probe an enabled Nginx listener 鈥?E:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇\.wt\zszj-wt-ops-002-a\services\zhongshu-core\deploy\docker-compose.deploy.yml:120-120
  This request targets HTTP port 80, but the mounted replacement configuration only enables HTTPS port 443; its HTTP server is entirely commented out. Consequently Nginx remains unhealthy even when certificates and proxying work correctly. Provide a loopback HTTP health listener or probe HTTPS with appropriate certificate handling.

- [P2] Align the upstream port with the deployment contract 鈥?E:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇\.wt\zszj-wt-ops-002-a\services\zhongshu-core\deploy\nginx\zszj-server.conf:60-60
  If an operator changes the supported `ZSZJ_SERVER_PORT` setting, Compose updates the application listener and its healthcheck, but this statically mounted configuration still forwards to port 48080. The server can become healthy while every proxied business request returns 502. Render the upstream from the same port setting or enforce a fixed application port throughout the template.

- [P2] Forward WebSocket upgrades through the proxy 鈥?E:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇\.wt\zszj-wt-ops-002-a\services\zhongshu-core\deploy\nginx\zszj-server.conf:61-64
  The backend enables `/infra/ws`, and existing admin-web and miniapp clients connect to that endpoint. Nginx 1.27 defaults to HTTP/1.0 upstream requests and does not forward the hop-by-hop upgrade headers, so WebSocket handshakes fail through this otherwise functional REST proxy. Configure HTTP/1.1 and the appropriate `Upgrade` and `Connection` headers for WebSocket traffic.

- [P2] Classify internal services by their container port 鈥?E:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇\.wt\zszj-wt-ops-002-a\scripts\ops\verify-deploy-templates.mjs:143-145
  A public mapping such as `15432:5432` currently produces no issue because the check compares the host port with `INTERNAL_PORTS`. PostgreSQL is still publicly reachable, just on a different host port, so this bypasses C4 through ordinary port remapping. Classify the service using the container-side port and independently validate its host binding.

- [P2] Exclude commented directives from management checks 鈥?E:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇\.wt\zszj-wt-ops-002-a\scripts\ops\verify-deploy-templates.mjs:118-119
  Commenting out both denial blocks in the supplied Nginx configuration still makes `checkManagementClosure` return an empty issue list: this regex matches the commented `location`, `deny`, and `return` text. Nginx then forwards those paths through `location /`, including the anonymously accessible health endpoint. Check active directives rather than raw comment text and add a commented-block negative test.

- [P2] Require the application service's healthcheck 鈥?E:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇\.wt\zszj-wt-ops-002-a\scripts\ops\verify-deploy-templates.mjs:83-86
  Removing only the `zszj-server` healthcheck from the supplied Compose file still passes C2 because PostgreSQL, Redis, and Nginx retain their own healthchecks, and an empty actuator-path list is accepted. Nginx nevertheless depends on `zszj-server` becoming healthy, so the validated template cannot satisfy that startup dependency. Validate the application service's active healthcheck and require its probe path explicitly.

- [P2] Check credentials supplied through Redis command arguments 鈥?E:\浼楀涔嬪AI璧嬭兘骞冲彴搴曞骇\.wt\zszj-wt-ops-002-a\scripts\ops\verify-deploy-templates.mjs:60-64
  C1 accepts a literal password in `command: redis-server --requirepass SyntheticPassword` and in the `redis-cli -a` healthcheck because neither line contains a secret-named mapping key. These are credential-bearing locations in the newly supplied template, so hardcoding them bypasses the advertised secret check. Validate those authentication arguments as well as environment mappings and add corresponding negative tests.
