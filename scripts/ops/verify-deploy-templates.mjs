/**
 * ZS-OPS-002.A 部署模板可审查性校验（纯静态，本地与 CI 同一入口）。
 *
 * 校验项：
 *   C1 模板无真实秘密（复用 ZS-CFG-001.A 判定口径 + command/healthcheck 参数）
 *   C2 探针路径与后端 actuator 暴露一致（须含 zszj-server 专属 healthcheck）
 *   C3 敏感管理路径在反代中确实关闭（排除注释行）
 *   C4 反代/compose 不泄露内部管理端口（按容器端口分类）
 *   C5 模板引用的环境变量与合同表双向一致
 *
 * 用法：node scripts/ops/verify-deploy-templates.mjs [--self-test]
 * 退出码：0=全部通过，1=存在 issue，2=脚本自身异常
 */
import { readFileSync, existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));

export const CONTRACT_VARS = [
  'ZSZJ_SERVER_PORT',
  'ZSZJ_DATASOURCE_URL',
  'ZSZJ_DATASOURCE_USERNAME',
  'ZSZJ_DATASOURCE_PASSWORD',
  'ZSZJ_REDIS_HOST',
  'ZSZJ_REDIS_PORT',
  'ZSZJ_REDIS_PASSWORD',
  'ZSZJ_PROFILE',
  // PostgreSQL 初始化超级用户（与应用账号分离，ZS-DB-002）
  'ZSZJ_PG_BOOTSTRAP_USERNAME',
  'ZSZJ_PG_BOOTSTRAP_PASSWORD',
  // TLS 证书挂载路径（运维必填；证书内容禁止入库，仅配置路径）
  'ZSZJ_TLS_CERT_PATH',
  'ZSZJ_TLS_KEY_PATH',
];

export const DEPLOY_TARGETS = [
  'services/zhongshu-core/deploy/docker-compose.deploy.yml',
  'services/zhongshu-core/deploy/.env.example',
  'services/zhongshu-core/deploy/nginx/zszj-server.conf',
  // PostgreSQL 初始化脚本（创建应用角色）：同样不得含硬编码秘密
  'services/zhongshu-core/deploy/postgres-init/01-create-app-role.sql',
];

// ---- C1 ----

const SECRET_KEY_RE = /(password|passwd|secret|token|access.?key|secret.?key|api.?key|private.?key)\s*[:=]/i;
const PLACEHOLDER_RE = /^\$\{[^}]*\}$/;
const SAFE_LITERALS_RE = /^(redacted.*|xx|''|""|)$/i;
// 命令行参数中的凭据标志（--requirepass、-a 等）
// 支持：空格分隔（--requirepass RealPass）与 YAML exec-list（"--requirepass", "RealPass"）
// flag 后可紧跟闭合引号，再跟分隔符（逗号/空格），再跟值的开引号
const CMD_CREDENTIAL_RE = /(--requirepass|--password|-a)["']?[\s,]+["']?([^\s"',\]]+)/i;
// SQL/psql 原生凭据语法（codex r2 P2 修复）：
//   PASSWORD '<字面量>'（CREATE/ALTER ROLE ... PASSWORD '...'）
//   \set <口令类变量> '<字面量>'（psql 客户端变量赋值）
// 排除安全形式：PASSWORD %L（format 占位符，非引号字面量）、`shell`（命令替换）、${VAR}
const SQL_PASSWORD_RE = /\bPASSWORD\s+'([^']+)'/i;
const SQL_SETPASS_RE = /\\set\s+\w*(?:pass|pwd|secret)\w*\s+'([^']+)'/i;

export function checkSecrets(relPath, text) {
  const issues = [];
  const lines = text.split(/\r?\n/);
  lines.forEach((line, i) => {
    const lineNo = i + 1;
    if (/BEGIN (RSA |EC |DSA )?PRIVATE KEY|BEGIN CERTIFICATE/.test(line)) {
      issues.push({ rule: 'C1-secret', path: relPath, line: lineNo, message: '模板中出现私钥/证书块' });
      return;
    }
    const hexMatch = line.match(/\b[0-9a-fA-F]{32,}\b/);
    if (hexMatch && !/commit|sha|checksum|hash/i.test(line)) {
      issues.push({ rule: 'C1-secret', path: relPath, line: lineNo, message: '出现长十六进制串（疑似凭据）: ' + hexMatch[0].slice(0, 12) });
      return;
    }
    if (SECRET_KEY_RE.test(line)) {
      const eqIdx = line.search(/[:=]/);
      const value = line.slice(eqIdx + 1).trim().replace(/^["']|["']$/g, '');
      if (value && !PLACEHOLDER_RE.test(value) && !SAFE_LITERALS_RE.test(value) && !value.startsWith('${')) {
        issues.push({ rule: 'C1-secret', path: relPath, line: lineNo, message: '秘密类键使用了字面量取值: ' + line.trim().slice(0, 60) });
      }
    }
    // 命令行参数中的硬编码凭据（如 redis-server --requirepass RealPassword）
    // 支持两种形式：空格分隔（--requirepass RealPass）和 YAML list（"--requirepass", "RealPass"）
    const cmdMatch = line.match(CMD_CREDENTIAL_RE);
    if (cmdMatch) {
      const credValue = cmdMatch[2];
      if (credValue && !PLACEHOLDER_RE.test(credValue) && !credValue.startsWith('${')) {
        issues.push({ rule: 'C1-secret', path: relPath, line: lineNo, message: '命令行参数含硬编码凭据: ' + cmdMatch[1] + ' ' + credValue.slice(0, 20) });
      }
    }
    // SQL/psql 原生硬编码凭据（.sql 初始化脚本，codex r2 P2 修复）
    const sqlPwMatch = line.match(SQL_PASSWORD_RE);
    if (sqlPwMatch) {
      const v = sqlPwMatch[1];
      if (v && !PLACEHOLDER_RE.test(v) && !v.startsWith('${') && !SAFE_LITERALS_RE.test(v)) {
        issues.push({ rule: 'C1-secret', path: relPath, line: lineNo, message: 'SQL 中出现硬编码 PASSWORD 字面量: ' + v.slice(0, 20) });
      }
    }
    const sqlSetMatch = line.match(SQL_SETPASS_RE);
    if (sqlSetMatch) {
      const v = sqlSetMatch[1];
      if (v && !PLACEHOLDER_RE.test(v) && !v.startsWith('${') && !SAFE_LITERALS_RE.test(v)) {
        issues.push({ rule: 'C1-secret', path: relPath, line: lineNo, message: 'psql \\set 出现硬编码口令字面量: ' + v.slice(0, 20) });
      }
    }
  });
  return issues;
}


// ---- C2 探针一致性 ----

/**
 * 从 compose 文本中提取指定服务的块（按缩进层级，非空行终止）。
 * YAML 服务块以 "  serviceName:" 开始，以同级或更高级缩进结束。
 * @param {string} composeText
 * @param {string} serviceName
 * @returns {string|null}
 */
function extractServiceBlock(composeText, serviceName) {
  const lines = composeText.split(/\r?\n/);
  // 服务头：任意缩进 + 服务名 + 冒号 + 可选 YAML anchor(&x)/行内注释(#)
  const startRe = new RegExp(`^(\\s*)${serviceName}:[ \\t]*(?:&\\w+[ \\t]*)?(?:#.*)?$`);
  let startIdx = -1;
  let headerIndent = 0;
  for (let i = 0; i < lines.length; i++) {
    const m = lines[i].match(startRe);
    if (m) { startIdx = i; headerIndent = m[1].length; break; }
  }
  if (startIdx < 0) return null;
  // 收集后续行，直到遇到缩进 <= 服务头缩进的非空行（同级/更高级键）
  const block = [lines[startIdx]];
  for (let i = startIdx + 1; i < lines.length; i++) {
    const line = lines[i];
    // 空行属于块内（YAML 允许）
    if (/^\s*$/.test(line)) { block.push(line); continue; }
    const indent = line.match(/^\s*/)[0].length;
    if (indent <= headerIndent) break;
    block.push(line);
  }
  return block.join('\n');
}

/**
 * 校验 compose healthcheck 探针路径与 actuator 暴露一致。
 * 要求：1) compose 含 healthcheck 定义；2) zszj-server 服务须有专属 /actuator 探针
 * @param {string} composeText docker-compose 模板内容
 * @param {string} actuatorInclude actuator exposure.include 值（如 'health' 或 'health,info'）
 */
export function checkProbeConsistency(composeText, actuatorInclude) {
  const issues = [];
  const exposed = actuatorInclude.split(',').map((s) => s.trim()).filter(Boolean);

  // compose 必须含 healthcheck 定义
  if (!/healthcheck/i.test(composeText)) {
    issues.push({ rule: 'C2-probe', path: 'deploy/docker-compose.deploy.yml', line: 0, message: '部署模板缺少 healthcheck 定义（探针边界未声明）' });
    return issues;
  }

  // zszj-server 服务须有专属 healthcheck 且引用 /actuator 路径
  const serverBlock = extractServiceBlock(composeText, 'zszj-server');
  if (serverBlock) {
    if (!/healthcheck/i.test(serverBlock)) {
      issues.push({ rule: 'C2-probe', path: 'deploy/docker-compose.deploy.yml', line: 0, message: 'zszj-server 服务缺少专属 healthcheck（nginx depends_on 需要它）' });
    } else if (!/\/actuator\//.test(serverBlock)) {
      issues.push({ rule: 'C2-probe', path: 'deploy/docker-compose.deploy.yml', line: 0, message: 'zszj-server healthcheck 未引用 /actuator 路径（探针边界不明确）' });
    }
  }

  // 提取 compose 中引用的 /actuator/ 路径
  const probePaths = [...composeText.matchAll(/\/actuator\/([\w/-]+)/g)].map((m) => m[1]);
  for (const p of probePaths) {
    const endpoint = p.split('/')[0]; // health/liveness → health
    if (!exposed.includes(endpoint)) {
      issues.push({ rule: 'C2-probe', path: 'deploy/docker-compose.deploy.yml', line: 0, message: `探针引用 /actuator/${p} 但 actuator exposure.include 未含 "${endpoint}"` });
    }
    // liveness/readiness 子路径需要 probes.enabled（支持 Spring property 和 env var 两种写法）
    if (/^health\/(liveness|readiness)/.test(p) &&
        !/(probes\.enabled|MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED).*true/i.test(composeText)) {
      issues.push({ rule: 'C2-probe', path: 'deploy/docker-compose.deploy.yml', line: 0, message: `探针引用 /actuator/${p} 但未声明 MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED=true` });
    }
  }
  return issues;
}

// ---- C3 管理路径关闭 ----

/** 敏感管理路径前缀（必须在反代中关闭） */
const SENSITIVE_PATHS = ['/actuator/', '/admin/'];

/**
 * 校验 nginx 反代配置是否关闭了敏感管理路径。
 * 仅检查非注释行（排除 # 开头的行）。
 * @param {string} nginxText nginx 配置内容
 */
export function checkManagementClosure(nginxText) {
  const issues = [];
  // 过滤掉注释行（# 开头，允许前导空格）
  const activeText = nginxText.split(/\r?\n/)
    .filter((line) => !/^\s*#/.test(line))
    .join('\n');

  for (const path of SENSITIVE_PATHS) {
    // 必须存在 location 块且含 deny all 或 return 403
    const locRe = new RegExp(`location\\s+${path.replace(/\//g, '\\/')}[^{]*\\{[^}]*(deny\\s+all|return\\s+403)`, 's');
    if (!locRe.test(activeText)) {
      issues.push({ rule: 'C3-mgmt-closure', path: 'deploy/nginx/zszj-server.conf', line: 0, message: `反向代理未关闭敏感管理路径 ${path}（需 deny all 或 return 403，注释行无效）` });
    }
  }
  return issues;
}
// ---- C4 端口暴露 ----

/** 内部服务端口（不得公开暴露，按容器端口分类） */
const INTERNAL_PORTS = [5432, 6379, 3306, 27017, 5672, 15672];

/**
 * 校验 compose 不公开暴露内部服务端口。
 * 按容器端口（冒号右侧）分类，而非宿主机端口——防止 15432:5432 绕过。
 * 绑定 127.0.0.1 的映射视为本地调试合法，不报。
 * @param {string} composeText docker-compose 模板内容
 */
export function checkPortExposure(composeText) {
  const issues = [];
  // 匹配 ports 映射行：- "HOST:CONTAINER" 或 - "IP:HOST:CONTAINER"
  const portLines = [...composeText.matchAll(/-\s*["']?([^"'\n]+:\d+)["']?/g)];
  for (const m of portLines) {
    const mapping = m[1].trim();
    const parts = mapping.split(':');
    // 判断是否绑定了 loopback
    const boundIp = parts.length >= 3 ? parts[0] : '0.0.0.0';
    // 容器端口是最后一个数字（冒号右侧）
    const containerPort = Number(parts[parts.length - 1]);
    if (INTERNAL_PORTS.includes(containerPort) && !/^127\./.test(boundIp)) {
      issues.push({ rule: 'C4-port', path: 'deploy/docker-compose.deploy.yml', line: 0, message: `内部服务容器端口 ${containerPort} 公开暴露（映射 ${mapping}，绑定 ${boundIp}），应移除或限制为 127.0.0.1` });
    }
  }
  return issues;
}

// ---- C5 环境变量合同双向一致 ----

/**
 * 校验 .env.example 与 compose 引用的环境变量与合同表双向一致。
 * @param {string} envExampleText .env.example 内容
 * @param {string} composeText docker-compose 模板内容
 * @param {string[]} contractVars 合同变量列表
 */
export function checkEnvContract(envExampleText, composeText, contractVars) {
  const issues = [];
  const contractSet = new Set(contractVars);

  // 方向 1：合同变量必须在 .env.example 中出现
  const envKeys = new Set(
    [...envExampleText.matchAll(/^([A-Z_][A-Z0-9_]*)\s*=/gm)].map((m) => m[1])
  );
  for (const v of contractVars) {
    if (!envKeys.has(v)) {
      issues.push({ rule: 'C5-env-contract', path: 'deploy/.env.example', line: 0, message: `合同变量 ${v} 在 .env.example 中缺失` });
    }
  }

  // 方向 2：compose 中引用的 ${VAR} 必须在合同表中（防幽灵变量）
  const composeVars = new Set(
    [...composeText.matchAll(/\$\{([A-Z_][A-Z0-9_]*)(?::[^}]*)?\}/g)].map((m) => m[1])
  );
  for (const v of composeVars) {
    if (!contractSet.has(v)) {
      issues.push({ rule: 'C5-env-contract', path: 'deploy/docker-compose.deploy.yml', line: 0, message: `compose 引用了合同外变量 ${v}（幽灵变量）` });
    }
  }

  return issues;
}
// ---- 主入口：全量扫描 ----

/** 读取 actuator exposure.include（从 application-prod.yaml 或 application.yaml） */
function readActuatorInclude() {
  const prodPath = join(root, 'services/zhongshu-core/script/config/application-prod.yaml');
  const mainPath = join(root, 'services/zhongshu-core/zszj-server/src/main/resources/application.yaml');
  for (const p of [prodPath, mainPath]) {
    if (!existsSync(p)) continue;
    const text = readFileSync(p, 'utf8');
    const m = text.match(/exposure:\s*\n\s*include:\s*(.+)/);
    if (m) return m[1].replace(/#.*$/, '').trim();
  }
  return 'health';
}

function runAll() {
  const issues = [];
  let scanned = 0;

  // C1: 秘密扫描（全部部署模板）
  for (const rel of DEPLOY_TARGETS) {
    const abs = join(root, rel);
    if (!existsSync(abs)) {
      issues.push({ rule: 'C0-missing', path: rel, line: 0, message: `部署模板文件不存在: ${rel}` });
      continue;
    }
    scanned++;
    const text = readFileSync(abs, 'utf8');
    issues.push(...checkSecrets(rel, text));
  }

  // C2: 探针一致性
  const composeRel = 'services/zhongshu-core/deploy/docker-compose.deploy.yml';
  const composeAbs = join(root, composeRel);
  if (existsSync(composeAbs)) {
    const composeText = readFileSync(composeAbs, 'utf8');
    const actuatorInclude = readActuatorInclude();
    issues.push(...checkProbeConsistency(composeText, actuatorInclude));
  }

  // C3: 管理路径关闭
  const nginxRel = 'services/zhongshu-core/deploy/nginx/zszj-server.conf';
  const nginxAbs = join(root, nginxRel);
  if (existsSync(nginxAbs)) {
    issues.push(...checkManagementClosure(readFileSync(nginxAbs, 'utf8')));
  }

  // C4: 端口暴露
  if (existsSync(composeAbs)) {
    issues.push(...checkPortExposure(readFileSync(composeAbs, 'utf8')));
  }

  // C5: 环境变量合同
  const envRel = 'services/zhongshu-core/deploy/.env.example';
  const envAbs = join(root, envRel);
  if (existsSync(envAbs) && existsSync(composeAbs)) {
    issues.push(...checkEnvContract(
      readFileSync(envAbs, 'utf8'),
      readFileSync(composeAbs, 'utf8'),
      CONTRACT_VARS,
    ));
  }

  return { scanned, issueCount: issues.length, issues: issues.slice(0, 50) };
}

// ---- --self-test 负向对照 ----

function selfTest() {
  // 用已知违规内容驱动各 check，证明断言可失败（非空洞）
  const results = [];
  const bad = checkSecrets('test.env', 'DB_PASSWORD=hunter2');
  results.push(['C1 负向（硬编码口令）', bad.length > 0]);

  const cmdCred = checkSecrets('compose.yml', 'command: redis-server --requirepass RealSecret123');
  results.push(['C1 负向（命令行硬编码凭据）', cmdCred.length > 0]);

  const listCred = checkSecrets('compose.yml', 'command: ["redis-server", "--requirepass", "RealSecret123"]');
  results.push(['C1 负向（YAML list 硬编码凭据）', listCred.length > 0]);

  const execListCred = checkSecrets('compose.yml', 'test: ["CMD", "redis-cli", "-a", "RealSecret123", "ping"]');
  results.push(['C1 负向（exec-list redis-cli -a 硬编码凭据）', execListCred.length > 0]);

  const sqlPw = checkSecrets('init.sql', "CREATE ROLE app LOGIN PASSWORD 'RealSecret123';");
  results.push(['C1 负向（SQL PASSWORD 字面量）', sqlPw.length > 0]);

  const sqlSet = checkSecrets('init.sql', "\\set app_pass 'RealSecret123'");
  results.push(['C1 负向（psql \\set 口令字面量）', sqlSet.length > 0]);

  const sqlSafe = checkSecrets('init.sql', "SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'u', :'app_pass')");
  results.push(['C1 正向（SQL format %L 占位符不误报）', sqlSafe.length === 0]);

  const noProbe = checkProbeConsistency('services:\n  x:\n    image: y', 'health');
  results.push(['C2 负向（无 healthcheck）', noProbe.length > 0]);

  const noServerProbe = checkProbeConsistency('services:\n  postgres:\n    healthcheck:\n      test: pg_isready\n  zszj-server:\n    image: x', 'health');
  results.push(['C2 负向（zszj-server 无专属探针）', noServerProbe.length > 0]);

  const annotatedHeader = checkProbeConsistency('services:\n  postgres:\n    healthcheck:\n      test: pg_isready\n  zszj-server: # backend\n    image: x', 'health');
  results.push(['C2 负向（带注释服务头缺探针仍检出）', annotatedHeader.length > 0]);

  const blankLineProbe = checkProbeConsistency('services:\n  zszj-server:\n    image: x\n\n    healthcheck:\n      test: ["CMD","curl","-f","http://localhost:48080/actuator/health"]', 'health');
  results.push(['C2 正向（块内空行不误判缺 healthcheck）', blankLineProbe.length === 0]);

  const openNginx = checkManagementClosure('location / { proxy_pass http://x; }');
  results.push(['C3 负向（管理路径未关闭）', openNginx.length > 0]);

  const commentedNginx = checkManagementClosure('# location /actuator/ { deny all; }\n# location /admin/ { deny all; }\nlocation / { proxy_pass http://x; }');
  results.push(['C3 负向（注释行不算关闭）', commentedNginx.length > 0]);

  const openPort = checkPortExposure('ports:\n      - "5432:5432"');
  results.push(['C4 负向（DB 端口公开）', openPort.length > 0]);

  const remappedPort = checkPortExposure('ports:\n      - "15432:5432"');
  results.push(['C4 负向（端口重映射仍检出容器端口）', remappedPort.length > 0]);

  const ghost = checkEnvContract('A=\n', '${GHOST}', CONTRACT_VARS);
  results.push(['C5 负向（幽灵变量）', ghost.length > 0]);

  const allPass = results.every(([, ok]) => ok);
  for (const [name, ok] of results) console.error(`  [${ok ? 'PASS' : 'FAIL'}] ${name}`);
  return allPass;
}

// ---- CLI ----

const invokedDirectly = process.argv[1] && process.argv[1].replaceAll('\\', '/').endsWith('scripts/ops/verify-deploy-templates.mjs');
if (invokedDirectly) {
  try {
    if (process.argv.includes('--self-test')) {
      const ok = selfTest();
      console.log(JSON.stringify({ selfTest: ok ? 'PASS' : 'FAIL' }));
      process.exitCode = ok ? 0 : 1;
    } else {
      const report = runAll();
      console.log(JSON.stringify(report, null, 2));
      console.error(`scanned=${report.scanned} issueCount=${report.issueCount}`);
      process.exitCode = report.issueCount ? 1 : 0;
    }
  } catch (e) {
    console.error(`deploy template check failed: ${e.message}`);
    process.exitCode = 2;
  }
}
