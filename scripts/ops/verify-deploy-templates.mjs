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
// SQL/psql 原生凭据检测（codex r2 引入；r6 由「消费型整匹配正则」改为「候选独立解析」）：
//   PASSWORD '<字面量>'（CREATE/ALTER ROLE ... PASSWORD '...'）
//   \set <口令类变量> '<字面量>'（psql 客户端变量赋值）
// r6 P2-1 修复：原正则 matchAll 为非重叠遍历，首个匹配会从拼接闭合引号一路消费到
// 真口令起始引号，导致同行第二个 PASSWORD 候选被吞（漏检）。现改为先定位候选关键字，
// 再对每个候选独立解析其后的引号角色——被排除的候选不得消费后续候选。
const SQL_PASSWORD_KEYWORD_RE = /\bPASSWORD\b/gi;
const SQL_SETPASS_KEYWORD_RE = /\\set\s+\w*(?:pass|pwd|secret)\w*/gi;

// SQL 词法上下文状态（codex r5 P2 修复：以完整词法器替代「裸引号奇偶 + 去注释」；
// r6 扩展：E'...' 反斜杠转义串、注释 / 美元引用内的片段级引号配对）
const CTX_NORMAL = 0;
const CTX_LINE_COMMENT = 1;
const CTX_BLOCK_COMMENT = 2;
const CTX_SINGLE_QUOTE = 3;
const CTX_DOUBLE_QUOTE = 4;
const CTX_DOLLAR_QUOTE = 5;
const CTX_ESCAPE_STRING = 6;

// PostgreSQL 美元引用定界符：$$ 或 $tag$（sticky 定位）。
// codex r6 P2-6：tag 字符集对齐未引用标识符（首字符字母/下划线，含非 ASCII 字符）；
// 且开启位置不得位于标识符内部（CREATE TABLE foo$tag$ 是单个标识符，其中的 $tag$ 非美元引用）。
const DOLLAR_TAG_RE = /\$(?:[A-Za-z_\u0080-\uFFFF][A-Za-z0-9_\u0080-\uFFFF]*)?\$/y;
const IDENT_CHAR_RE = /[A-Za-z0-9_$\u0080-\uFFFF]/;
function matchDollarTag(text, i) {
  if (i > 0 && IDENT_CHAR_RE.test(text[i - 1])) return null;
  DOLLAR_TAG_RE.lastIndex = i;
  const m = DOLLAR_TAG_RE.exec(text);
  return m ? m[0] : null;
}

/** 自然语言撇号（如 application's）：前后均为 ASCII 字母 → 不参与 SQL 引号配对（codex r7 P2-3） */
function isApostropheWord(text, i) {
  return /[A-Za-z]/.test(text[i - 1] ?? '') && /[A-Za-z]/.test(text[i + 1] ?? '');
}

/**
 * 构建 SQL 词法上下文图：map[k] = 词法器「到达」字符 k 时所处状态。
 * 跟踪 PostgreSQL 全部引用上下文并跨行保持状态：
 *   -- 行注释、/* 块注释（可嵌套）、'单引号串'（'' 转义）、"双引号标识符"（"" 转义）、
 *   $tag$美元引用$tag$、E'反斜杠转义串'（codex r6 P2-5）。
 * 片段级配对（codex r6 P2-3）：注释内 / 美元引用内的 SQL 文本按「独立片段」配对单引号——
 * 闭合引号 map 记 CTX_SINGLE_QUOTE，片段边界（行尾 / 块注释闭合 / 美元定界符）强制复位，
 * 片段内撇号不跨边界污染；自然语言撇号（字母夹引号，如 application's）不参与配对（r7 P2-3）。由此：注释里被注释掉的拼接表达式引号角色判定正确（不误报），
 * 而注释里 PASSWORD '真实值' 仍可检出。
 * 用于判定 PASSWORD/\set 后的引号究竟是「字面量起始」（开引号）
 * 还是「拼接字符串的闭合引号」（CTX_SINGLE_QUOTE / CTX_ESCAPE_STRING）。
 * @returns {Uint8Array} 长度与 text 一致的状态码数组
 */
function sqlContextMap(text) {
  const n = text.length;
  const map = new Uint8Array(n); // 默认 0 = CTX_NORMAL
  let state = CTX_NORMAL;
  let blockDepth = 0;
  let dollarTag = '';
  let fragSq = false; // 注释 / 美元引用片段内的单引号配对（片段边界强制复位）
  let i = 0;
  while (i < n) {
    map[i] = state; // 记录「到达」状态（字面量起始引号在此记为外层状态）
    const c = text[i];
    const nx = text[i + 1];
    if (state === CTX_NORMAL) {
      if (c === '-' && nx === '-') { state = CTX_LINE_COMMENT; fragSq = false; i += 2; continue; }
      if (c === '/' && nx === '*') { state = CTX_BLOCK_COMMENT; blockDepth = 1; fragSq = false; i += 2; continue; }
      if (c === "'") { state = CTX_SINGLE_QUOTE; i += 1; continue; }
      if (c === '"') { state = CTX_DOUBLE_QUOTE; i += 1; continue; }
      // E'...' 反斜杠转义串（E 前须为空白/运算符等非标识符字符，否则 E 属标识符）
      if ((c === 'E' || c === 'e') && nx === "'" && (i === 0 || !IDENT_CHAR_RE.test(text[i - 1]))) {
        map[i + 1] = CTX_NORMAL; // 开启引号视为 NORMAL 语义（字面量起始）
        state = CTX_ESCAPE_STRING; i += 2; continue;
      }
      const tag = matchDollarTag(text, i);
      if (tag) { state = CTX_DOLLAR_QUOTE; dollarTag = tag; fragSq = false; i += tag.length; continue; }
      i += 1; continue;
    }
    if (state === CTX_LINE_COMMENT) {
      if (c === '\n') { state = CTX_NORMAL; fragSq = false; i += 1; continue; }
      if (c === "'") {
        if (isApostropheWord(text, i)) { i += 1; continue; } // 自然语言撇号不参与配对（r7 P2-3）
        if (fragSq && nx === "'") { map[i + 1] = CTX_LINE_COMMENT; i += 2; continue; }
        if (fragSq) { map[i] = CTX_SINGLE_QUOTE; fragSq = false; i += 1; continue; }
        fragSq = true; i += 1; continue;
      }
      i += 1; continue;
    }
    if (state === CTX_BLOCK_COMMENT) {
      if (c === '/' && nx === '*') { blockDepth += 1; i += 2; continue; }
      if (c === '*' && nx === '/') {
        blockDepth -= 1;
        if (blockDepth === 0) { state = CTX_NORMAL; fragSq = false; }
        i += 2; continue;
      }
      if (c === "'") {
        if (isApostropheWord(text, i)) { i += 1; continue; } // 自然语言撇号不参与配对（r7 P2-3）
        if (fragSq && nx === "'") { map[i + 1] = CTX_BLOCK_COMMENT; i += 2; continue; }
        if (fragSq) { map[i] = CTX_SINGLE_QUOTE; fragSq = false; i += 1; continue; }
        fragSq = true; i += 1; continue;
      }
      i += 1; continue;
    }
    if (state === CTX_SINGLE_QUOTE) {
      if (c === "'" && nx === "'") { map[i + 1] = CTX_SINGLE_QUOTE; i += 2; continue; }
      if (c === "'") { state = CTX_NORMAL; i += 1; continue; }
      i += 1; continue;
    }
    if (state === CTX_ESCAPE_STRING) {
      if (c === '\\') { map[i + 1] = CTX_ESCAPE_STRING; i += 2; continue; }
      if (c === "'" && nx === "'") { map[i + 1] = CTX_ESCAPE_STRING; i += 2; continue; }
      if (c === "'") { state = CTX_NORMAL; i += 1; continue; }
      i += 1; continue;
    }
    if (state === CTX_DOUBLE_QUOTE) {
      if (c === '"' && nx === '"') { map[i + 1] = CTX_DOUBLE_QUOTE; i += 2; continue; }
      if (c === '"') { state = CTX_NORMAL; i += 1; continue; }
      i += 1; continue;
    }
    // CTX_DOLLAR_QUOTE：扫描至匹配的美元定界符；片段内配对单引号（定界符处复位）
    if (text.startsWith(dollarTag, i)) {
      for (let k = i; k < i + dollarTag.length && k < n; k++) map[k] = CTX_DOLLAR_QUOTE;
      state = CTX_NORMAL; fragSq = false; i += dollarTag.length; continue;
    }
    if (c === "'") {
      if (isApostropheWord(text, i)) { i += 1; continue; } // 自然语言撇号不参与配对（r7 P2-3）
      if (fragSq && nx === "'") { map[i + 1] = CTX_DOLLAR_QUOTE; i += 2; continue; }
      if (fragSq) { map[i] = CTX_SINGLE_QUOTE; fragSq = false; i += 1; continue; }
      fragSq = true; i += 1; continue;
    }
    i += 1;
  }
  return map;
}

/** YAML 单引号标量解码：'' → '（其余字符原样） */
function decodeYamlSingle(raw) {
  return raw.replace(/''/g, "'");
}

/**
 * YAML 双引号标量解码：常用转义还原；未定义转义（如 \g）保留原样，避免解码过度。
 * codex r7 P2-2：补 \xNN / \uNNNN / \UNNNNNNNN 十六进制字符转义——
 * \x27 等编码的引号须先解码为字符，否则内嵌 SQL 的引号结构无法被扫描。
 */
function decodeYamlDouble(raw) {
  return raw.replace(/\\(["\\/bfnrt]|x[0-9A-Fa-f]{2}|u[0-9A-Fa-f]{4}|U[0-9A-Fa-f]{8})/g, (m, esc) => {
    switch (esc[0]) {
      case '"': return '"';
      case '\\': return '\\';
      case '/': return '/';
      case 'b': return '\b';
      case 'f': return '\f';
      case 'n': return '\n';
      case 'r': return '\r';
      case 't': return '\t';
      case 'x':
      case 'u': return String.fromCharCode(parseInt(esc.slice(1), 16));
      case 'U': {
        const cp = parseInt(esc.slice(1), 16);
        return cp <= 0x10FFFF ? String.fromCodePoint(cp) : m;
      }
      default: return m;
    }
  });
}

/**
 * 拆一行 YAML 为片段：comment（# 起至行尾）/ sq（'...'）/ dq（"..."）/ plain。
 * codex r7 P2-1：仅「标量起始位置」的引号才是 YAML 引号标量（行首 / `: ` / `- ` /
 * `[ { ,` 之后的首个非空字符）；普通标量内部的引号保留为普通文本——
 * command: [psql, -c, CREATE ROLE app LOGIN PASSWORD 'x';] 的 SQL 引号不得被拆散。
 * 引号标量按 YAML 转义规则扫描（SQ: ''；DQ: \<char>），仅切分不转义（解码在检测前）。
 * codex r6 P2-2：YAML 上下文须与外层文本隔离——注释撇号不得开启跨行 SQL 字符串，
 * 引号标量内容属「内嵌 SQL」，应解码后独立建立 SQL 上下文。
 */
function splitYamlSegments(line) {
  const segs = [];
  let plain = '';
  let scalarStart = true; // 「标量起始位置」：仅此处的引号按 YAML 引号标量解析（r7 P2-1）
  const flush = () => { if (plain) { segs.push({ kind: 'plain', raw: plain }); plain = ''; } };
  let i = 0;
  while (i < line.length) {
    const c = line[i];
    // YAML 行注释：# 位于行首或空白之后（含内联注释）
    if (c === '#' && (i === 0 || /[ \t]/.test(line[i - 1]))) {
      flush(); segs.push({ kind: 'comment', raw: line.slice(i) }); return segs;
    }
    if ((c === "'" || c === '"') && scalarStart) {
      const q = c;
      flush();
      let j = i + 1;
      let raw = '';
      while (j < line.length) {
        if (q === "'" && line[j] === "'" && line[j + 1] === "'") { raw += "''"; j += 2; continue; }
        if (q === '"' && line[j] === '\\' && j + 1 < line.length) { raw += line[j] + line[j + 1]; j += 2; continue; }
        if (line[j] === q) break;
        raw += line[j]; j += 1;
      }
      segs.push({ kind: q === "'" ? 'sq' : 'dq', raw });
      i = j < line.length ? j + 1 : j;
      scalarStart = false;
      continue;
    }
    // 标量边界跟踪：`:`+空白（键分隔）/ `[` `{` `,`（流式节点）/ `-`+空白（块序列项）
    if (c === ':' && (line[i + 1] === undefined || /[ \t]/.test(line[i + 1]))) { plain += c; i += 1; scalarStart = true; continue; }
    if (c === '[' || c === '{' || c === ',') { plain += c; i += 1; scalarStart = true; continue; }
    if (c === '-' && /[ \t]/.test(line[i + 1] ?? '') && (i === 0 || /[ \t]/.test(line[i - 1]))) { plain += c; i += 1; scalarStart = true; continue; }
    if (c === ' ' || c === '\t') { plain += c; i += 1; continue; }
    plain += c; i += 1;
    scalarStart = false;
  }
  flush();
  return segs;
}

/** 从开引号后提取到配对闭引号（'' 与 \x 转义对整体保留，仅用于值读取） */
function extractSqlValue(line, qIdx, limit) {
  let j = qIdx + 1;
  let out = '';
  while (j < limit) {
    const ch = line[j];
    if (ch === "'") {
      if (line[j + 1] === "'") { out += "''"; j += 2; continue; }
      break;
    }
    if (ch === '\\' && j + 1 < limit) { out += line[j] + line[j + 1]; j += 2; continue; }
    out += ch; j += 1;
  }
  return out;
}

/** 值是否为「需要报告的硬编码凭据」：非空、非完整 ${VAR} 占位（r7 P2-8：仅完整匹配豁免）、非常量安全值（redacted/xx/空） */
function isUnsafeValue(v) {
  return Boolean(v) && !PLACEHOLDER_RE.test(v) && !SAFE_LITERALS_RE.test(v);
}

/**
 * SQL 片段凭据扫描（codex r6：候选独立解析；r7：语句级结构化取值解析）。
 * 逐候选定位 PASSWORD / \set 口令关键字，从关键字末尾按 SQL token 解析取值：
 *   - 跳过空白与注释（注释内分号不算语句终止）；跨物理行跟踪同一语句（P2-5）；
 *   - 取值 token：NULL 跳过候选（P2-6）；:'变量' 插值安全并继续；开引号字面量
 *     直接判定，安全值经 || 拼接可继续；闭引号（关键字位于字符串内）转入拼接
 *     表达式窗口，安全字面量豁免后继续遍历剩余字面量（P2-4）；
 *   - 语句边界：NORMAL 上下文分号 / psql 元命令行（行首）/ EOF。
 *   - format('… PASSWORD %L …', 实参…) 命中时按占位符-实参对应仅检查 %L 实参（P2-7）。
 * 候选逐个推进、互不消费：同行「安全拼接 + 真口令」两个候选均被独立检查（P2-1）。
 * @param {string} text 纯 SQL 文本（YAML 场景为解码后的标量内容）
 * @returns {Array<{line:number, message:string}>} line 为片段内 1-based 行号
 */
function scanSqlText(text) {
  const found = [];
  const n = text.length;
  if (!n) return found;
  const ctxMap = sqlContextMap(text);
  const lineStarts = [0];
  for (let k = 0; k < n; k++) if (text[k] === '\n') lineStarts.push(k + 1);
  const lineNo = (idx) => {
    let lo = 0;
    let hi = lineStarts.length - 1;
    while (lo < hi) { const mid = (lo + hi + 1) >> 1; if (lineStarts[mid] <= idx) lo = mid; else hi = mid - 1; }
    return lo + 1;
  };
  const cands = [];
  for (const m of text.matchAll(SQL_PASSWORD_KEYWORD_RE)) cands.push({ pos: m.index, end: m.index + m[0].length, kind: 'sql' });
  for (const m of text.matchAll(SQL_SETPASS_KEYWORD_RE)) cands.push({ pos: m.index, end: m.index + m[0].length, kind: 'setpass' });
  cands.sort((a, b) => a.pos - b.pos);
  for (const cand of cands) {
    const hit = scanCandidate(text, ctxMap, cand);
    if (hit) {
      const stmtLabel = cand.kind === 'setpass' ? 'psql \\set ' : 'SQL ';
      found.push({ line: lineNo(hit.at), message: stmtLabel + hit.message + hit.value.slice(0, 20) });
    }
  }
  return found;
}

/**
 * format('… PASSWORD %L …', 实参…) 的占位符-实参映射（codex r7 P2-7）。
 * 仅当关键字位于 format 格式串内且其后紧跟 %L 时启用：口令取值即 %L 对应实参，
 * 只检查该实参内的字面量（%I 角色名等其它实参不属口令值）。
 * @returns {undefined} 非本场景（交回通用扫描）；null 已处理无违规；否则为命中对象
 */
function scanFormatPlaceholder(text, ctxMap, cand) {
  if (ctxMap[cand.pos] !== CTX_SINGLE_QUOTE) return undefined;
  if (!/^\s*%L/.test(text.slice(cand.end, cand.end + 8))) return undefined;
  let q = cand.pos - 1;
  while (q >= 0 && !(text[q] === "'" && ctxMap[q] === CTX_NORMAL)) q -= 1;
  if (q < 0) return undefined;
  if (!/format\s*\(\s*$/i.test(text.slice(Math.max(0, q - 24), q))) return undefined;
  let cq = cand.end;
  while (cq < text.length && !(text[cq] === "'" && ctxMap[cq] === CTX_SINGLE_QUOTE)) cq += 1;
  if (cq >= text.length) return undefined;
  // 占位符序号（1-based）= 格式串内候选之前的 %L/%I/%s 数 + 1；args[0] 为格式串
  const placeholderIdx = (text.slice(q + 1, cand.pos).match(/%[LsI]/g) || []).length + 1;
  const args = [[q, cq + 1], ...splitCallArgs(text, ctxMap, cq + 1, placeholderIdx + 1)];
  const target = args[placeholderIdx];
  if (!target) return null; // 实参缺失（动态构造等）→ 不判定
  for (let k = target[0]; k < target[1]; k++) {
    if (text[k] !== "'") continue;
    const ctx = ctxMap[k];
    if (ctx === CTX_SINGLE_QUOTE || ctx === CTX_ESCAPE_STRING || ctx === CTX_DOUBLE_QUOTE) continue;
    if (text[k - 1] === ':') continue;
    const v = extractSqlValue(text, k, target[1]);
    if (isUnsafeValue(v)) return { at: k, value: v, message: '拼接口令表达式出现硬编码字面量: ' };
  }
  return null;
}

/** 解析 format() 格式串后其余实参的 [start,end) 区间（顶层逗号切分，最多 maxArgCount 个） */
function splitCallArgs(text, ctxMap, from, maxArgCount) {
  const args = [];
  const n = text.length;
  let depth = 1; // 已处于调用圆括号内部
  let start = -1;
  let k = from;
  const closeArg = (end) => { if (start >= 0) args.push([start, end]); start = -1; };
  while (k < n && args.length < maxArgCount) {
    const c = text[k];
    if (c === "'") {
      const ctx = ctxMap[k];
      if (ctx === CTX_SINGLE_QUOTE || ctx === CTX_ESCAPE_STRING || ctx === CTX_DOUBLE_QUOTE) { k += 1; continue; }
      if (start < 0) start = text[k - 1] === ':' ? k - 1 : k;
      const v = extractSqlValue(text, k, n);
      k = k + 1 + v.length;
      continue;
    }
    if (c === '(') { depth += 1; if (start < 0) start = k; k += 1; continue; }
    if (c === ')') {
      depth -= 1;
      if (depth === 0) { closeArg(k); break; }
      k += 1; continue;
    }
    if (c === ',' && depth === 1) { closeArg(k); k += 1; continue; }
    if (start < 0 && !/\s/.test(c)) start = k;
    k += 1;
  }
  return args;
}

/**
 * 单个口令候选的取值解析（codex r7 语句级结构化取值）。
 * psql \set 为单行元命令：换行即候选边界（行尾反斜杠续行除外），
 * 防候选从 \set 行跑野到后续语句、把 format 格式串误判为口令字面量（r7.1 真实模板误报）。
 * @param {string} text 全文
 * @param {Uint8Array} ctxMap sqlContextMap(text)
 * @param {{pos:number,end:number,kind:string}} cand 关键字候选（pos=起始，end=末尾后一位）
 * @returns {null|{at:number,value:string,message:string}} 命中位置 / 值 / 消息主体
 */
function scanCandidate(text, ctxMap, cand) {
  const n = text.length;
  const candCtx = ctxMap[cand.pos];
  if (candCtx === CTX_DOUBLE_QUOTE) return null; // 双引号标识符内的 PASSWORD 字样非口令语句
  const candInComment = candCtx === CTX_LINE_COMMENT || candCtx === CTX_BLOCK_COMMENT;
  const fmt = scanFormatPlaceholder(text, ctxMap, cand);
  if (fmt !== undefined) return fmt;

  const lineHeadOnlyWs = (k) => {
    let s = k;
    while (s > 0 && text[s - 1] !== '\n') s -= 1;
    return /^[ \t]*$/.test(text.slice(s, k));
  };

  let i = cand.end;
  let mode = 'value'; // value=待取取值 token；after=直接取值已判定（仅 || 可延续）；expr=拼接表达式窗口
  while (i < n) {
    const c = text[i];
    const ctx = ctxMap[i];

    if (cand.kind === 'setpass' && (c === '\n' || c === '\r')) {
      if (text[i - 1] === '\\') { i += 1; continue; } // 行尾反斜杠续行
      return null; // psql \set 为单行元命令：换行即边界（r7.1：禁止跑野到后续语句误报 format 串）
    }
    if (c === ' ' || c === '\t' || c === '\n' || c === '\r' || c === '\f') { i += 1; continue; }
    if (candInComment && i > cand.end && ctx === CTX_NORMAL) return null; // 注释片段结束
    if (!candInComment) {
      // 注释按原文跳转（不依赖片段配对标记，避免被注释内配对闭合引号干扰）
      if (ctx === CTX_LINE_COMMENT) { const nl = text.indexOf('\n', i); i = nl < 0 ? n : nl; continue; }
      if (ctx === CTX_BLOCK_COMMENT) { const close = text.indexOf('*/', i + 1); i = close < 0 ? n : close + 2; continue; }
      if (ctx === CTX_NORMAL && c === '-' && text[i + 1] === '-') { const nl = text.indexOf('\n', i); i = nl < 0 ? n : nl; continue; }
      if (ctx === CTX_NORMAL && c === '/' && text[i + 1] === '*') { const close = text.indexOf('*/', i + 2); i = close < 0 ? n : close + 2; continue; }
    }
    if (c === ';' && ctx === CTX_NORMAL) return null; // 语句终止：未取到不安全口令
    if (c === '\\' && lineHeadOnlyWs(i)) return null; // psql 元命令行边界

    if (mode === 'after') {
      if (c === '|' && text[i + 1] === '|') { mode = 'value'; i += 2; continue; }
      return null; // 取值已判定，后续非拼接 token（如 VALID UNTIL 选项）结束本候选
    }

    if (mode === 'value' && /^NULL\b/i.test(text.slice(i, i + 4)) && !IDENT_CHAR_RE.test(text[i - 1] ?? '')) {
      return null; // PASSWORD NULL：无口令取值（P2-6）
    }

    if (c === "'") {
      if (ctx === CTX_SINGLE_QUOTE || ctx === CTX_ESCAPE_STRING) { mode = 'expr'; i += 1; continue; } // 闭引号 → 拼接窗口
      if (ctx === CTX_DOUBLE_QUOTE) { i += 1; continue; }
      if (text[i - 1] === ':') { // psql 变量插值 :'name' 安全，跳过其字面量
        const v = extractSqlValue(text, i, n);
        const j = i + 1 + v.length;
        i = j < n && text[j] === "'" ? j + 1 : j;
        continue;
      }
      const v = extractSqlValue(text, i, n);
      const closed = i + 1 + v.length < n && text[i + 1 + v.length] === "'";
      if (isUnsafeValue(v)) return { at: i, value: v, message: '出现硬编码口令字面量: ' };
      i = i + 1 + v.length + (closed ? 1 : 0);
      if (mode === 'value') mode = 'after';
      continue;
    }

    i += 1;
  }
  return null;
}

export function checkSecrets(relPath, text) {
  const issues = [];
  // 归一化换行（CRLF→LF），使行偏移与词法图索引对齐
  const norm = text.replace(/\r\n/g, '\n');
  const lines = norm.split('\n');
  const isYaml = /\.ya?ml$/i.test(relPath);
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
      if (value && !PLACEHOLDER_RE.test(value) && !SAFE_LITERALS_RE.test(value)) {
        issues.push({ rule: 'C1-secret', path: relPath, line: lineNo, message: '秘密类键使用了字面量取值: ' + line.trim().slice(0, 60) });
      }
    }
    // 命令行参数中的硬编码凭据（如 redis-server --requirepass RealPassword）
    // 支持两种形式：空格分隔（--requirepass RealPass）和 YAML list（"--requirepass", "RealPass"）
    const cmdMatch = line.match(CMD_CREDENTIAL_RE);
    if (cmdMatch) {
      const credValue = cmdMatch[2];
      if (credValue && !PLACEHOLDER_RE.test(credValue)) {
        issues.push({ rule: 'C1-secret', path: relPath, line: lineNo, message: '命令行参数含硬编码凭据: ' + cmdMatch[1] + ' ' + credValue.slice(0, 20) });
      }
    }
  });
  // SQL/psql 原生硬编码凭据（codex r6：候选独立解析 + YAML 上下文隔离；r7：块标量原样扫描）：
  //   - 非 YAML（SQL/.env/conf）：全文建 SQL 词法上下文，注释 / 美元引用内做片段级配对；
  //   - YAML：先按片段隔离（注释 / 引号标量 / 普通文本），标量按 YAML 规则解码后独立建 SQL 上下文，
  //     防注释撇号跨行污染（P2-2）、外层引号转义未解码被漏检（P2-2）；
  //     | / > 块标量内容为原文，按普通文本行扫描，不做 YAML 引号语义（r7 P2-1）。
  if (isYaml) {
    let blockIndent = -1; // >=0：当前处于块标量内容区（缩进大于指示符行）
    for (let i = 0; i < lines.length; i++) {
      const line = lines[i];
      if (blockIndent >= 0 && !/^\s*$/.test(line)) {
        const ind = line.match(/^\s*/)[0].length;
        if (ind > blockIndent) {
          for (const f of scanSqlText(line)) {
            issues.push({ rule: 'C1-secret', path: relPath, line: i + 1, message: f.message });
          }
          continue;
        }
        blockIndent = -1; // 块标量结束，本行回落常规处理
      }
      for (const seg of splitYamlSegments(line)) {
        const frag = seg.kind === 'sq' ? decodeYamlSingle(seg.raw)
          : seg.kind === 'dq' ? decodeYamlDouble(seg.raw) : seg.raw;
        for (const f of scanSqlText(frag)) {
          issues.push({ rule: 'C1-secret', path: relPath, line: i + 1, message: f.message });
        }
      }
      // 块标量指示符（| / >，含 +/- 与缩进指示数字）开启内容区
      if (!/^\s*#/.test(line) && /(?:^|\s|:)[|>][+-]?\d*[ \t]*(?:#.*)?$/.test(line)) {
        blockIndent = line.match(/^\s*/)[0].length;
      }
    }
  } else {
    for (const f of scanSqlText(norm)) {
      issues.push({ rule: 'C1-secret', path: relPath, line: f.line, message: f.message });
    }
  }
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
  // 定位顶层 services: 映射，确定其直接子键（各服务）的缩进层级，
  // 避免把 nginx.depends_on.zszj-server 等深层同名键误当服务头（codex r3 P2）
  let servicesIdx = -1;
  for (let i = 0; i < lines.length; i++) {
    if (/^services:[ \t]*(?:#.*)?$/.test(lines[i])) { servicesIdx = i; break; }
  }
  let childIndent = null;
  if (servicesIdx >= 0) {
    for (let i = servicesIdx + 1; i < lines.length; i++) {
      const line = lines[i];
      if (/^\s*$/.test(line) || /^\s*#/.test(line)) continue;
      const ind = line.match(/^\s*/)[0].length;
      if (ind === 0) break; // 顶级键，services 段结束
      childIndent = ind;
      break;
    }
  }
  // 服务头：缩进 + 服务名 + 冒号 + 可选 YAML anchor(&x)/行内注释(#)
  const headerRe = new RegExp(`^(\\s*)${serviceName}:[ \\t]*(?:&\\w+[ \\t]*)?(?:#.*)?$`);
  let startIdx = -1;
  let headerIndent = 0;
  const searchFrom = servicesIdx >= 0 ? servicesIdx + 1 : 0;
  for (let i = searchFrom; i < lines.length; i++) {
    const m = lines[i].match(headerRe);
    if (!m) continue;
    const ind = m[1].length;
    // 若已确定 services 直接子键层级，服务头须恰在该层级（排除 depends_on 下深层同名键）
    if (childIndent !== null && ind !== childIndent) continue;
    startIdx = i; headerIndent = ind; break;
  }
  if (startIdx < 0) return null;
  // 收集后续行，直到遇到缩进 <= 服务头缩进的非空非注释行（同级/更高级键）
  const block = [lines[startIdx]];
  for (let i = startIdx + 1; i < lines.length; i++) {
    const line = lines[i];
    // 空行属于块内（YAML 允许）
    if (/^\s*$/.test(line)) { block.push(line); continue; }
    // 注释行：跳过——既不终止块（缩进可低于映射，codex r3 P2），
    // 也不纳入块（防注释内 healthcheck//actuator 字样伪造探针证据，codex r4 P2）
    if (/^\s*#/.test(line)) { continue; }
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

  const sqlConcat = checkSecrets('init.sql', "SELECT 'CREATE ROLE app LOGIN PASSWORD ' || quote_literal(:'app_pass') \\gexec");
  results.push(['C1 正向（SQL 字符串闭合引号不误报）', sqlConcat.length === 0]);

  const sqlCommentApostrophe = checkSecrets('init.sql', "/* application's role */ CREATE ROLE app LOGIN PASSWORD 'RealSecret123';");
  results.push(['C1 负向（块注释内撇号不干扰，硬编码口令仍检出）', sqlCommentApostrophe.length > 0]);

  const sqlCommentedOut = checkSecrets('init.sql', "-- CREATE ROLE app LOGIN PASSWORD 'RealSecret123';");
  results.push(['C1 负向（注释掉的 SQL 凭据仍检出）', sqlCommentedOut.length > 0]);

  const sqlDqIdentifier = checkSecrets('init.sql', 'CREATE ROLE "app--readonly" LOGIN PASSWORD \'RealSecret123\';');
  results.push(['C1 负向（双引号标识符内 -- 不当注释，口令仍检出）', sqlDqIdentifier.length > 0]);

  const composeEmbeddedSql = checkSecrets('compose.yml', 'command: ["psql", "-c", "CREATE ROLE app LOGIN PASSWORD \'RealSecret123\';"]');
  results.push(['C1 负向（compose 内嵌 SQL 凭据仍检出）', composeEmbeddedSql.length > 0]);

  // ---- codex r6 P2 回归（候选独立解析 + YAML 隔离 + 词法补全） ----
  const overlapCands = checkSecrets('init.sql', "SELECT 'CREATE ROLE app LOGIN PASSWORD ' || quote_literal(current_user); CREATE ROLE x LOGIN PASSWORD 'RealSecret123';");
  results.push(['C1 负向（同行安全拼接不消费后续真口令候选，P2-1）', overlapCands.length > 0]);

  const yamlCommentApostrophe = checkSecrets('compose.yml', "# application's init\ncommand: [\"psql\", \"-c\", \"CREATE ROLE app LOGIN PASSWORD 'RealSecret123';\"]");
  results.push(['C1 负向（YAML 注释撇号不跨行污染，DQ 内真口令检出，P2-2）', yamlCommentApostrophe.length > 0]);

  const yamlSqEscape = checkSecrets('compose.yml', "command: ['psql', '-c', 'CREATE ROLE app LOGIN PASSWORD ''RealSecret123'';']");
  results.push(['C1 负向（YAML SQ 标量双撇号转义解码后检出，P2-2）', yamlSqEscape.length > 0]);

  const commentedSafeConcat = checkSecrets('init.sql', "-- SELECT 'CREATE ROLE app LOGIN PASSWORD ' || quote_literal(:'app_pass') \\gexec");
  results.push(['C1 正向（注释内安全拼接不误报，P2-3）', commentedSafeConcat.length === 0]);

  const yamlSafeConcat = checkSecrets('compose.yml', '[\"psql\", \"-c\", \"SELECT \'CREATE ROLE app LOGIN PASSWORD \' || quote_literal(:\'app_pass\') \\gexec\"]');
  results.push(['C1 正向（YAML DQ 内安全拼接不误报，P2-3）', yamlSafeConcat.length === 0]);

  const quoteLiteralLiteral = checkSecrets('init.sql', "SELECT 'CREATE ROLE app LOGIN PASSWORD ' || quote_literal('RealSecret123') \\gexec");
  results.push(['C1 负向（quote_literal 硬编码参数检出，P2-4）', quoteLiteralLiteral.length > 0]);

  const escString = checkSecrets('init.sql', "SELECT E'application\\'s'; CREATE ROLE app LOGIN PASSWORD 'RealSecret123';");
  results.push(['C1 负向（E 串反斜杠转义不吞后续口令，P2-5）', escString.length > 0]);

  const nonAsciiDollar = checkSecrets('init.sql', "SELECT $标签$application's$标签$; CREATE ROLE app LOGIN PASSWORD 'RealSecret123';");
  results.push(['C1 负向（非 ASCII 美元 tag 识别后真口令仍检出，P2-6）', nonAsciiDollar.length > 0]);

  const identDollar = checkSecrets('init.sql', "CREATE TABLE foo$tag$(id int); SELECT 'CREATE ROLE app LOGIN PASSWORD ' || quote_literal(:'app_pass') \\gexec");
  results.push(['C1 正向（标识符内 $tag$ 不开启美元引用，P2-6）', identDollar.length === 0]);

  // ---- codex r7 回归（YAML 标量边界 / 转义解码 / 撇号配对 / 语句级取值解析） ----
  const plainFlowSql = checkSecrets('compose.yml', "command: [psql, -c, CREATE ROLE app LOGIN PASSWORD 'RealSecret123';]");
  results.push(['C1 负向（plain 流标量内 SQL 引号不误拆，口令检出，r7 P2-1）', plainFlowSql.length > 0]);

  const blockScalarSql = checkSecrets('compose.yml', "init: |\n  CREATE ROLE app LOGIN PASSWORD 'RealSecret123';");
  results.push(['C1 负向（| 块标量内容行口令检出，r7 P2-1）', blockScalarSql.length > 0]);

  const hexEscape = checkSecrets('compose.yml', 'command: ["psql", "-c", "CREATE ROLE app LOGIN PASSWORD \\x27RealSecret123\\x27;"]');
  results.push(['C1 负向（\\x27 十六进制转义解码后检出，r7 P2-2）', hexEscape.length > 0]);

  const uniEscape = checkSecrets('compose.yml', 'command: ["psql", "-c", "CREATE ROLE app LOGIN PASSWORD \\u0027RealSecret123\\u0027;"]');
  results.push(['C1 负向（\\u0027 转义解码后检出，r7 P2-2）', uniEscape.length > 0]);

  const commentApos = checkSecrets('init.sql', "-- application's old credential: CREATE ROLE app LOGIN PASSWORD 'RealSecret123';");
  results.push(['C1 负向（行注释撇号不占用引号配对，口令检出，r7 P2-3）', commentApos.length > 0]);

  const windowContinue = checkSecrets('init.sql', "SELECT 'CREATE ROLE app LOGIN PASSWORD ' || quote_literal('' || 'RealSecret123') \\gexec");
  results.push(['C1 负向（空串豁免后继续窗口遍历，检出真口令，r7 P2-4）', windowContinue.length > 0]);

  const windowCrossLine = checkSecrets('init.sql', "SELECT 'CREATE ROLE app LOGIN PASSWORD ' ||\nquote_literal('RealSecret123')\n\\gexec");
  results.push(['C1 负向（拼接窗口跨行继续，检出真口令，r7 P2-5）', windowCrossLine.length > 0]);

  const windowCommentSemi = checkSecrets('init.sql', "SELECT 'CREATE ROLE app LOGIN PASSWORD ' || /* ; */ quote_literal('RealSecret123') \\gexec");
  results.push(['C1 负向（注释内分号不截断窗口，检出真口令，r7 P2-5）', windowCommentSemi.length > 0]);

  const nullValue = checkSecrets('init.sql', "CREATE ROLE app LOGIN PASSWORD NULL; SELECT 'hello';");
  results.push(['C1 正向（PASSWORD NULL 不误取后续语句字面量，r7 P2-6）', nullValue.length === 0]);

  const commentBetween = checkSecrets('init.sql', "CREATE ROLE app LOGIN PASSWORD /* '' */ 'RealSecret123';");
  results.push(['C1 负向（关键字与取值间注释不干扰，口令检出，r7 P2-6）', commentBetween.length > 0]);

  const formatRoleArg = checkSecrets('init.sql', "SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', 'app', :'app_pass')");
  results.push(['C1 正向（format %L 参数为变量不误报角色名参数，r7 P2-7）', formatRoleArg.length === 0]);

  const formatLiteralArg = checkSecrets('init.sql', "SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', 'app', 'RealSecret123')");
  results.push(['C1 负向（format %L 参数为硬编码口令仍检出，r7 P2-7）', formatLiteralArg.length > 0]);

  const prefixBypass = checkSecrets('init.sql', "CREATE ROLE app LOGIN PASSWORD '${VAR}RealSecret123';");
  results.push(['C1 负向（占位符前缀硬编码后缀仍检出，r7 P2-8）', prefixBypass.length > 0]);

  const fullPlaceholder = checkSecrets('init.sql', "CREATE ROLE app LOGIN PASSWORD '${ZSZJ_DATASOURCE_PASSWORD}';");
  results.push(['C1 正向（完整占位符不误报，r7 P2-8）', fullPlaceholder.length === 0]);

  const setBacktick = checkSecrets('init.sql', '\\set app_pass `echo "$ZSZJ_DATASOURCE_PASSWORD"`\n\n-- create role\nSELECT format(\'CREATE ROLE %I LOGIN PASSWORD %L\', :\'app_user\', :\'app_pass\')\n\\gexec');
  results.push(['C1 正向（\\set 反引号命令替换止于行边界，不误报后续 format 串，r7.1）', setBacktick.length === 0]);

  const setContinue = checkSecrets('init.sql', "\\set app_pass \\\n'RealSecret123'");
  results.push(['C1 负向（\\set 行尾反斜杠续行后字面量仍检出，r7.1）', setContinue.length > 0]);

  const noProbe = checkProbeConsistency('services:\n  x:\n    image: y', 'health');
  results.push(['C2 负向（无 healthcheck）', noProbe.length > 0]);

  const noServerProbe = checkProbeConsistency('services:\n  postgres:\n    healthcheck:\n      test: pg_isready\n  zszj-server:\n    image: x', 'health');
  results.push(['C2 负向（zszj-server 无专属探针）', noServerProbe.length > 0]);

  const annotatedHeader = checkProbeConsistency('services:\n  postgres:\n    healthcheck:\n      test: pg_isready\n  zszj-server: # backend\n    image: x', 'health');
  results.push(['C2 负向（带注释服务头缺探针仍检出）', annotatedHeader.length > 0]);

  const dependsOnHeader = checkProbeConsistency('services:\n  nginx:\n    depends_on:\n      zszj-server:\n        condition: service_healthy\n  zszj-server:\n    healthcheck:\n      test: ["CMD","curl","-f","http://localhost:48080/actuator/health"]', 'health');
  results.push(['C2 正向（depends_on 深层同名键不误判服务头）', dependsOnHeader.length === 0]);

  const commentInBlock = checkProbeConsistency('services:\n  zszj-server:\n    image: x\n  # probe\n    healthcheck:\n      test: ["CMD","curl","-f","http://localhost:48080/actuator/health"]', 'health');
  results.push(['C2 正向（块内低缩进注释不截断）', commentInBlock.length === 0]);

  const commentAsEvidence = checkProbeConsistency('services:\n  zszj-server:\n    image: x\n  # TODO: add healthcheck at /actuator/health', 'health');
  results.push(['C2 负向（注释不伪造探针证据）', commentAsEvidence.length > 0]);

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
