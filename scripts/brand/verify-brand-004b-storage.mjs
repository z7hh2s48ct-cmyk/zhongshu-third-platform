/**
 * ZS-BRAND-004.B 缓存/浏览器存储/会话键清单静态门禁（本地与 CI 同一入口）。
 *
 * 用法：node scripts/brand/verify-brand-004b-storage.mjs [--quiet]
 *
 * 职责（对齐开发计划 §3.2 / docs/05 ZS-BRAND-004 卡）：
 *   1. 全量提取后端 Redis 键模式：各模块 RedisKeyConstants.java、framework 保护层
 *      （幂等/限流/签名/lock4j）RedisDAO/KeyConstants、zszj-server 配置里的 wx/wa 等
 *      key-prefix——以 git ls-files 实际全量为准，不凭手工清单。
 *   2. 提取两端浏览器存储键：admin-web（useCache.ts CACHE_KEY + auth.ts ACCESS_TOKEN/
 *      REFRESH_TOKEN）、miniapp（src/store defineStore 持久化 id + uni.*StorageSync 字面量）。
 *   3. 断言：提取出的键标识与值均无旧产品名（yudao/ruoyi/iocoder/youdao/芋道/unibest/yd-）残留。
 *   4. 命名合同检查：LOGIN-002 会话代际键、LOGIN-003 撤销墓碑键、LOGIN-004 短信配额键、
 *      OAuth2 访问令牌键、保护层幂等/限流/锁键必须存在；TenantRedisCacheManager 的
 *      租户后缀拼接必须仍在（缓存键「name:tenantId:key」语义）。
 *   5. 注入自检（防扫描器空洞）：把带旧名的合成样例喂给同一套提取+判定路径，
 *      任一样例未被命中即门禁失败。
 *
 * 退出码：0 通过；1 发现残留/合同缺失/注入自检失败；2 脚本自身错误。
 * 键清单报告以 JSON 输出到 stdout（清单化报告）；--quiet 时只输出摘要。
 */
import { readFileSync, existsSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('../../', import.meta.url));
const quiet = process.argv.includes('--quiet');

// 旧产品标识（键级判定，比全仓品牌门禁 G3 更窄：只判提取出的键标识/键值，
// 不重复 G3 的全仓文本扫描，也不误伤「芋道源码」等已登记的来源署名）。
export const OLD_NAME_PATTERNS = [
  { name: 'yudao', re: /yudao/gi },
  { name: 'ruoyi', re: /ruoyi/gi },
  { name: 'iocoder', re: /iocoder/gi },
  { name: 'youdao', re: /youdao/gi },
  { name: '芋道', re: /芋道/g },
  { name: 'unibest', re: /unibest/gi },
  { name: 'yd-prefix', re: /\byd-[a-z]/g },
];

/** 对单个键标识/键值做旧名判定；返回未放行命中（键清单口径不放行任何旧名）。 */
export function judgeKey(source, field, value) {
  const violations = [];
  for (const { name, re } of OLD_NAME_PATTERNS) {
    re.lastIndex = 0;
    let m;
    while ((m = re.exec(value))) {
      violations.push({ source, field, value, pattern: name, sample: m[0] });
    }
  }
  return violations;
}

// ---------------------------------------------------------------------------
// git 文件清单（全量事实来源，勿凭手工清单遗漏）
// ---------------------------------------------------------------------------
export function listGitFiles(filter) {
  const files = execFileSync('git', ['ls-files', '-z'], { cwd: root, maxBuffer: 64 * 1024 * 1024 })
    .toString()
    .split('\0')
    .filter(Boolean);
  return filter ? files.filter(filter) : files;
}

function readRepoFile(rel) {
  const abs = `${root}${rel.replaceAll('/', process.platform === 'win32' ? '\\' : '/')}`;
  if (!existsSync(abs)) return null;
  return readFileSync(abs, 'utf8');
}

// ---------------------------------------------------------------------------
// 后端 Redis 键模式提取
// ---------------------------------------------------------------------------
// RedisKeyConstants 接口：String NAME = "value";
const JAVA_CONST_RE = /String\s+([A-Z][A-Z0-9_]*)\s*=\s*"([^"]*)"\s*;/g;

/** *RedisKeyConstants.java / *KeyConstants.java：全部 String 常量视为键模式。 */
export function extractJavaKeyConstants(text, source) {
  const keys = [];
  let m;
  JAVA_CONST_RE.lastIndex = 0;
  while ((m = JAVA_CONST_RE.exec(text))) {
    keys.push({ source, field: m[1], value: m[2] });
  }
  return keys;
}

// 保护层 RedisDAO：private static final String NAME = "value";
const JAVA_PRIVATE_CONST_RE = /private\s+static\s+final\s+String\s+([A-Z][A-Z0-9_]*)\s*=\s*"([^"]*)"\s*;/g;
// 排除非键的辅助常量：LUA 脚本、Hash 字段名、时间桶类型等（值不含 ':' 且不以 %s 结尾不必然可靠，
// 以名字语义过滤：字段/桶/脚本类常量不是键模式）。
const DAO_HELPER_NAME_RE = /(LUA|FIELD|BUCKET|_TYPE$|SQL|SCRIPT)/;

/** framework 保护层 *RedisDAO.java：仅提取键形态常量（含 ':' 或 %s）。 */
export function extractDaoKeyConstants(text, source) {
  const keys = [];
  let m;
  JAVA_PRIVATE_CONST_RE.lastIndex = 0;
  while ((m = JAVA_PRIVATE_CONST_RE.exec(text))) {
    const [, name, value] = m;
    if (DAO_HELPER_NAME_RE.test(name)) continue;
    if (!value.includes(':') && !value.includes('%s') && !/^(KEY|NONCE|APPID|IDEMPOTENT|RATE|SIGNATURE|LOCK)/.test(name)) continue;
    if (!/^[a-z][a-z0-9_]*(:[%{}a-z0-9_.:-]*)*$/.test(value)) continue; // 小写键形态防误收
    keys.push({ source, field: name, value });
  }
  return keys;
}

/** zszj-server 配置：key-prefix: wx / wa 等（Redis 键前缀经配置注入，也是键清单的一部分）。 */
export function extractYamlKeyPrefixes(text, source) {
  const keys = [];
  const re = /^\s*key-prefix:\s*([^\s#]+)/gm;
  let m;
  while ((m = re.exec(text))) {
    keys.push({ source, field: 'key-prefix', value: m[1] });
  }
  return keys;
}

/** application.yaml：zszj.tenant.ignore-caches（豁免租户后缀的缓存名，参与键清单与运行期核对）。 */
export function extractTenantIgnoreCaches(text) {
  // [ \t] 而非 \s：\s 会吞换行导致跨块误捕（只捕紧随其后的列表项行）
  const block = text.match(/^[ \t]*ignore-caches:[ \t]*(?:#[^\n]*)?\r?\n((?:[ \t]+-[ \t]+\S+[ \t]*\r?\n?)+)/m);
  if (!block) return [];
  return block[1]
    .split('\n')
    .map((l) => l.trim().replace(/^-\s+/, '').trim())
    .filter(Boolean);
}

export function extractBackendRedisKeys() {
  const keys = [];
  const scannedFiles = { keyConstants: 0, redisDao: 0, yaml: 0 };
  for (const rel of listGitFiles((f) => /KeyConstants\.java$/.test(f) && f.replaceAll('\\', '/').includes('services/zhongshu-core/'))) {
    const text = readRepoFile(rel);
    if (text === null) continue;
    scannedFiles.keyConstants++;
    keys.push(...extractJavaKeyConstants(text, rel.replaceAll('\\', '/')));
  }
  for (const rel of listGitFiles((f) => /RedisDAO\.java$/.test(f) && f.replaceAll('\\', '/').includes('zszj-framework/'))) {
    const text = readRepoFile(rel);
    if (text === null) continue;
    scannedFiles.redisDao++;
    keys.push(...extractDaoKeyConstants(text, rel.replaceAll('\\', '/')));
  }
  for (const rel of listGitFiles((f) => /application.*\.yaml$/.test(f) && f.replaceAll('\\', '/').includes('zszj-server/src/main/resources/'))) {
    const text = readRepoFile(rel);
    if (text === null) continue;
    scannedFiles.yaml++;
    keys.push(...extractYamlKeyPrefixes(text, rel.replaceAll('\\', '/')));
  }
  return { keys, scannedFiles };
}

// ---------------------------------------------------------------------------
// 两端浏览器存储键提取
// ---------------------------------------------------------------------------
const WEB_USECACHE = 'apps/zhongshu-admin-web/src/hooks/web/useCache.ts';
const WEB_AUTH = 'apps/zhongshu-admin-web/src/utils/auth.ts';

/** useCache.ts：CACHE_KEY 字面量表（roleRouters/user/dictCache/tenantId…）。 */
export function extractWebCacheKeyBlock(text, source) {
  const keys = [];
  const block = text.match(/export const CACHE_KEY = \{([\s\S]*?)\n\}/);
  if (!block) return keys;
  const entryRe = /([A-Za-z_][A-Za-z0-9_]*)\s*:\s*'([^']+)'\s*,?/g;
  let m;
  while ((m = entryRe.exec(block[1]))) {
    keys.push({ source, field: m[1], value: m[2] });
  }
  return keys;
}

/** auth.ts：const XxxKey = 'ACCESS_TOKEN' 等令牌存储键常量与 UPPER_SNAKE 字面量。 */
export function extractWebTokenKeys(text, source) {
  const keys = [];
  const constRe = /const\s+(\w*[Kk]ey\w*)\s*=\s*'([^']+)'\s*;/g;
  let m;
  while ((m = constRe.exec(text))) {
    keys.push({ source, field: m[1], value: m[2] });
  }
  const literalRe = /wsCache\.(?:get|set|delete)\(\s*'([A-Z][A-Z_]{2,})'\s*\)/g;
  while ((m = literalRe.exec(text))) {
    if (!keys.some((k) => k.value === m[1])) keys.push({ source, field: 'literal', value: m[1] });
  }
  return keys;
}

/** miniapp：defineStore 持久化 id（pinia persist 落 uni storage）+ uni.*StorageSync 字面量。 */
export function extractMiniappStorageKeys(text, source) {
  const keys = [];
  const storeRe = /defineStore\(\s*'([^']+)'/gs;
  let m;
  while ((m = storeRe.exec(text))) {
    keys.push({ source, field: 'store-id', value: m[1] });
  }
  const storageRe = /uni\.(?:set|get|remove)StorageSync\(\s*'([^']+)'\s*[,)]/g;
  while ((m = storageRe.exec(text))) {
    if (!keys.some((k) => k.value === m[1] && k.field === 'storage')) {
      keys.push({ source, field: 'storage', value: m[1] });
    }
  }
  return keys;
}

export function extractFrontendStorageKeys() {
  const keys = [];
  const stats = { webFiles: 0, miniappFiles: 0 };
  for (const rel of [WEB_USECACHE, WEB_AUTH]) {
    const text = readRepoFile(rel);
    if (text === null) throw new Error(`两端存储键事实文件缺失：${rel}`);
    stats.webFiles++;
    keys.push(...extractWebCacheKeyBlock(text, rel), ...extractWebTokenKeys(text, rel));
  }
  for (const rel of listGitFiles((f) => {
    const p = f.replaceAll('\\', '/');
    return p.startsWith('apps/zhongshu-miniapp/src/') && /\.ts$/.test(p) && !/\/constants\//.test(p);
  })) {
    const text = readRepoFile(rel);
    if (text === null) continue;
    stats.miniappFiles++;
    keys.push(...extractMiniappStorageKeys(text, rel.replaceAll('\\', '/')));
  }
  return { keys, stats };
}

// ---------------------------------------------------------------------------
// 命名合同（LOGIN-002/003/004、保护层、租户后缀）
// ---------------------------------------------------------------------------
export const REQUIRED_BACKEND_KEYS = [
  { value: 'oauth2_access_token:%s', why: 'LOGIN-002/003：访问令牌会话键（主体作用域）' },
  { value: 'oauth2_access_token_revoke_tomb:%s', why: 'LOGIN-003：撤销墓碑键（阻塞缓存回填复活旧凭据）' },
  { value: 'oauth2_refresh_session_generation:%s', why: 'LOGIN-002：会话代际号（并发刷新串行化证据）' },
  { value: 'oauth2_access_session_generation:%s', why: 'LOGIN-002：访问令牌代际反查键' },
  { value: 'sms_code_validate_attempts:%s:%s', why: 'LOGIN-004：短信验证码爆破防护计数（手机号+场景作用域）' },
  { value: 'sms_code_send_ip_count:%s:%s:%s', why: 'LOGIN-004：每 IP 短信发送配额（IP+桶作用域）' },
  { value: 'rate_limiter:%s', why: 'SEC-010：登录/刷新限流键' },
  { value: 'idempotent:%s', why: '保护层幂等键' },
  { value: 'lock4j:%s', why: '保护层分布式锁键' },
];

const TENANT_CACHE_MANAGER = 'services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz-tenant/src/main/java/cn/zszj/framework/tenant/core/redis/TenantRedisCacheManager.java';
const TENANT_SUFFIX_SNIPPET = 'name + ":" + TenantContextHolder.getTenantId()';

export function checkNamingContract(backendKeys) {
  const problems = [];
  const values = new Set(backendKeys.map((k) => k.value));
  for (const req of REQUIRED_BACKEND_KEYS) {
    if (!values.has(req.value)) problems.push({ check: 'required-key', missing: req.value, why: req.why });
  }
  // 会话/凭据类键必须带主体占位符（%s 作用域），不允许无作用域的会话键形态
  for (const k of backendKeys) {
    if (/^oauth2_(access_token|access_token_revoke_tomb|refresh_session_generation|access_session_generation)/.test(k.value) && !k.value.includes('%s')) {
      problems.push({ check: 'session-key-scope', value: k.value, why: '会话/凭据键必须带主体占位符（token/租户作用域）' });
    }
  }
  // TenantRedisCacheManager 租户后缀语义必须仍在（缓存键 name:tenantId:key）
  const mgrText = readRepoFile(TENANT_CACHE_MANAGER);
  if (mgrText === null) problems.push({ check: 'tenant-cache-manager', why: `${TENANT_CACHE_MANAGER} 缺失` });
  else if (!mgrText.includes(TENANT_SUFFIX_SNIPPET)) {
    problems.push({ check: 'tenant-cache-manager', why: `租户后缀拼接缺失（预期包含：${TENANT_SUFFIX_SNIPPET}）` });
  }
  // tenant.enable 必须为 true（租户后缀生效前提）；锚定 zszj 根块下两格缩进的 tenant:
  const appYaml = readRepoFile('services/zhongshu-core/zszj-server/src/main/resources/application.yaml') ?? '';
  const tenantBlock = appYaml.match(/^  tenant:[^\n]*\r?\n((?:[ \t]{4}[^\n]*\r?\n?)+)/m);
  const tenantEnabled = tenantBlock ? /^ {4}enable:[ \t]*true/m.test(tenantBlock[1]) : false;
  if (!tenantEnabled) {
    problems.push({ check: 'tenant-enable', why: 'application.yaml zszj.tenant.enable 非 true，租户后缀失效' });
  }
  return problems;
}

// ---------------------------------------------------------------------------
// 注入自检（防扫描器空洞）：合成样例必须被同一套提取+判定路径命中
// ---------------------------------------------------------------------------
export function injectionSelfTest() {
  const samples = [
    {
      name: 'backend-redis-key',
      extract: () => extractJavaKeyConstants('String OAUTH2_ACCESS_TOKEN = "yudao_oauth2_access_token:%s";', 'injected'),
      expectField: 'OAUTH2_ACCESS_TOKEN',
    },
    {
      name: 'protection-dao-key',
      extract: () => extractDaoKeyConstants('private static final String RATE_LIMITER = "ruoyi_rate_limiter:%s";', 'injected'),
      expectField: 'RATE_LIMITER',
    },
    {
      name: 'web-cache-key',
      extract: () => extractWebCacheKeyBlock("export const CACHE_KEY = {\n  USER: 'yudao_user',\n  DICT_CACHE: 'dictCache',\n\n}", 'injected'),
      expectField: 'USER',
    },
    {
      name: 'miniapp-storage',
      extract: () => extractMiniappStorageKeys("uni.setStorageSync('yudao_token', t)", 'injected'),
      expectField: 'storage',
    },
    {
      name: 'yaml-key-prefix',
      extract: () => extractYamlKeyPrefixes('      key-prefix: yudao_wx # 旧前缀', 'injected'),
      expectField: 'key-prefix',
    },
  ];
  const results = [];
  let failed = false;
  for (const s of samples) {
    const extracted = s.extract();
    const hit = extracted
      .filter((k) => k.field === s.expectField)
      .flatMap((k) => judgeKey(k.source, k.field, k.value));
    const ok = extracted.length > 0 && hit.length > 0;
    if (!ok) failed = true;
    results.push({ sample: s.name, extracted: extracted.length, oldNameHits: hit.length, pass: ok });
  }
  return { results, pass: !failed };
}

// ---------------------------------------------------------------------------
// 主流程
// ---------------------------------------------------------------------------
const invokedDirectly = process.argv[1] && process.argv[1].replaceAll('\\', '/').endsWith('scripts/brand/verify-brand-004b-storage.mjs');
if (invokedDirectly) {
  try {
    const backend = extractBackendRedisKeys();
    const frontend = extractFrontendStorageKeys();
    const allKeys = [...backend.keys, ...frontend.keys];

    const violations = allKeys.flatMap((k) => judgeKey(k.source, k.field, k.value));
    const contractProblems = checkNamingContract(backend.keys);
    const selfTest = injectionSelfTest();

    const appYaml = readRepoFile('services/zhongshu-core/zszj-server/src/main/resources/application.yaml') ?? '';
    const report = {
      task: 'ZS-BRAND-004.B 键清单静态门禁',
      backend: {
        scannedFiles: backend.scannedFiles,
        keyCount: backend.keys.length,
        keyPatterns: quiet ? undefined : backend.keys,
        tenantIgnoreCaches: extractTenantIgnoreCaches(appYaml),
      },
      frontend: {
        stats: frontend.stats,
        keyCount: frontend.keys.length,
        keys: quiet ? undefined : frontend.keys,
      },
      oldNameViolations: violations,
      namingContractProblems: contractProblems,
      injectionSelfTest: selfTest,
      pass: violations.length === 0 && contractProblems.length === 0 && selfTest.pass,
    };
    console.log(JSON.stringify(report, null, 2));
    console.error(
      `backendKeys=${backend.keys.length} frontendKeys=${frontend.keys.length} `
      + `oldNameViolations=${violations.length} contractProblems=${contractProblems.length} `
      + `injectionSelfTest=${selfTest.pass ? 'PASS' : 'FAIL'} => ${report.pass ? 'PASS' : 'FAIL'}`,
    );
    process.exitCode = report.pass ? 0 : 1;
  } catch (error) {
    console.error(`brand-004b storage check failed: ${error.message}`);
    process.exitCode = 2;
  }
}
