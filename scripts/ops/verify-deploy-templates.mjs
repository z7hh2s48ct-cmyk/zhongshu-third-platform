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
 * 有界换行搜索（r13 P2-9）：在 [from, min(limit, text.length)) 内定位首个换行，
 * 未找到返回 min(limit, text.length)。美元体等片段内的注释消费须以此替代
 * 无上限 indexOf——否则无换行输入下每个片段都要扫到全文尾再截回，整体退化 O(n²)。
 * @param {string} text
 * @param {number} from 起点
 * @param {number} limit 扫描上限（不含）
 * @returns {number} 首个换行位置；上限内无换行时为 limit
 */
function newlineWithin(text, from, limit) {
  const end = Math.min(limit, text.length);
  let i = from;
  while (i < end && text[i] !== '\n') i += 1;
  return i;
}

/**
 * CAST 调用左括号判定（r13 P2-1；r14 P2-1/P2-5 重写为 ctxMap 感知左向扫描；
 * r15 P2-1/P2-2 改注释区间表驱动；r16 P2-2 区间跳转前置于片段 ctx 检查）：
 * 自左括号左向跳过空白与注释区间，读取完整
 * 标识符词并要求恰为 CAST。注释区间取 buildParenDepthMap 的 commentLeft /
 * commentRight 表——普通注释与片段内局部注释（美元体文本里的块注释与行注释）
 * 同表同路径整段跳过：
 *   - 原 ctxMap 注释标记整段跳过会被注释内配对引号的 CTX_SINGLE_QUOTE 闭引号
 *     标记打断（r15 P2-1 漏检修复）；
 *   - 片段模式原仅跳空白、不跳局部注释（r15 P2-2 漏检修复）；
 *   - 区间跳转须先于片段 ctx 边界检查——美元体行注释内配对引号的 CTX_SINGLE_QUOTE
 *     闭引号标记同样不得阻断整段跳过（r16 P2-2 漏检修复）。
 * 片段内（注释 / 美元引用）不越出片段边界（r13 P2-3）；包含左括号自身的区间
 * （包着左括号的注释 / 片段环境）不跳过；无全文搜索 / 回溯，每字符 O(1)（r14 P2-5）。
 * 词边界完整（xCAST( / 1CAST( 不误认定）。
 * @param {string} text
 * @param {Uint8Array} ctxMap sqlContextMap(text)
 * @param {number} openPos 左括号位置
 * @param {Int32Array} commentLeft 注释区间起点表（-1=非注释区）
 * @param {Int32Array} commentRight 注释区间终点表（不含；-1=非注释区）
 * @returns {boolean}
 */
function isCastCallParen(text, ctxMap, openPos, commentLeft, commentRight) {
  const openCtx = ctxMap[openPos];
  const openInFrag = openCtx === CTX_LINE_COMMENT || openCtx === CTX_BLOCK_COMMENT || openCtx === CTX_DOLLAR_QUOTE;
  let k = openPos - 1;
  for (;;) {
    if (k < 0) return false;
    // r16 P2-2：区间跳转先于片段 ctx 边界检查——注释区间本身即真实注释边界证据，
    // 区间内配对引号的闭引号标记不得阻断整段跳过
    const l = commentLeft[k];
    if (l >= 0 && commentRight[k] <= openPos) { k = l - 1; continue; } // 注释区间整段跳过（含左括号自身的区间不跳）
    if (openInFrag && ctxMap[k] !== openCtx) break; // 越出片段边界：不在片段外继续找词
    if (/\s/.test(text[k])) { k -= 1; continue; }
    break;
  }
  const e = k;
  while (k >= 0 && IDENT_CHAR_RE.test(text[k])) k -= 1;
  if (k === e) return false; // 左括号前无标识符词
  return text.slice(k + 1, e + 1).toUpperCase() === 'CAST';
}

/**
 * 构建 SQL 词法上下文图：map[k] = 词法器「到达」字符 k 时所处状态。
 * 跟踪 PostgreSQL 全部引用上下文并跨行保持状态：
 *   -- 行注释、/* 块注释（可嵌套）、'单引号串'（'' 转义）、"双引号标识符"（"" 转义）、
 *   $tag$美元引用$tag$、E'反斜杠转义串'（codex r6 P2-5）；美元体内注释以缓存的闭合
 *   定界符为消费上限（r12 P2-8：原每个体内注释都先扫到全文 EOF 再截回，O(n²)）。
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
  let dollarClose = -1; // r12 P2-8：美元引用体闭合定界符位置（进入体内时一次定位，注释 / 闭合检测复用）
  let i = 0;
  while (i < n) {
    map[i] = state; // 记录「到达」状态（字面量起始引号在此记为外层状态）
    const c = text[i];
    const nx = text[i + 1];
    if (state === CTX_NORMAL) {
      // r14 P2-3：注释定界符两字符均标注释 ctx（原仅第一字符留「到达」标记、第二字符
      // 为默认 NORMAL 空洞）——注释区间成为连续 ctx 段：左向扫描（isCastCallParen）可
      // 整段跳过；片段结束「首个 NORMAL」判定不再停在内层定界符第二字符（fragEnd 错停修复）
      if (c === '-' && nx === '-') { map[i] = CTX_LINE_COMMENT; map[i + 1] = CTX_LINE_COMMENT; state = CTX_LINE_COMMENT; fragSq = false; i += 2; continue; }
      if (c === '/' && nx === '*') { map[i] = CTX_BLOCK_COMMENT; map[i + 1] = CTX_BLOCK_COMMENT; state = CTX_BLOCK_COMMENT; blockDepth = 1; fragSq = false; i += 2; continue; }
      if (c === "'") { state = CTX_SINGLE_QUOTE; i += 1; continue; }
      if (c === '"') { state = CTX_DOUBLE_QUOTE; i += 1; continue; }
      // psql 反引号引用（r11 P2-9）：内容交 shell——区间内不做注释 / 引号词法，
      // 避免 shell 参数 `--` 被当作 SQL 行注释、shell 引号被外层配对；
      // 行内未闭合时止于换行（元命令同行边界）
      if (c === '`') {
        let k = i + 1;
        while (k < n && text[k] !== '`' && text[k] !== '\n') k += 1;
        for (let j = i + 1; j < k; j++) map[j] = CTX_NORMAL;
        i = k < n && text[k] === '`' ? k + 1 : k;
        continue;
      }
      // E'...' 反斜杠转义串（E 前须为空白/运算符等非标识符字符，否则 E 属标识符）
      if ((c === 'E' || c === 'e') && nx === "'" && (i === 0 || !IDENT_CHAR_RE.test(text[i - 1]))) {
        map[i + 1] = CTX_NORMAL; // 开启引号视为 NORMAL 语义（字面量起始）
        state = CTX_ESCAPE_STRING; i += 2; continue;
      }
      const tag = matchDollarTag(text, i);
      if (tag) {
        state = CTX_DOLLAR_QUOTE; dollarTag = tag; fragSq = false;
        const dc = text.indexOf(tag, i + tag.length); // r12 P2-8：一次定位闭合定界符
        dollarClose = dc < 0 ? n : dc;
        i += tag.length; continue;
      }
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
      if (c === '/' && nx === '*') { map[i + 1] = CTX_BLOCK_COMMENT; blockDepth += 1; i += 2; continue; } // r14 P2-3：第二字符补齐
      if (c === '*' && nx === '/') {
        map[i + 1] = CTX_BLOCK_COMMENT; // r14 P2-3：终结符第二字符属注释区间
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
    // r12 P2-8：以进入时缓存的闭合位置判闭合（注释 / 字符串跳过不越过该位置）
    if (i === dollarClose) {
      for (let k = i; k < i + dollarTag.length && k < n; k++) map[k] = CTX_DOLLAR_QUOTE;
      state = CTX_NORMAL; fragSq = false; dollarClose = -1; i += dollarTag.length; continue;
    }
    // 体内注释（r10 P2-7；r11 P2-6/7 加固）：
    //   - 注释消费以美元闭合定界符为上限（r11 P2-7）——-- / /* */ 不得跨过 $$，
    //     否则闭合定界符被吞、后续独立 SQL 被误标为美元体文本；
    //   - 注释内引号做局部配对标记（r11 P2-6）：闭引号标 CTX_SINGLE_QUOTE、'' 转义对
    //     保留片段 ctx，供片段内候选的串内判定与语句边界解析；注释结束后局部状态丢弃，
    //     不影响注释外的 fragSq（r10 P2-7 原意）
    if (!fragSq && c === '-' && nx === '-') {
      // r13 P2-9：换行搜索限定在美元体闭合位置以内——原 indexOf 先扫全文再截回，
      // 无换行多美元体输入退化为 O(n²)
      const nl = newlineWithin(text, i, dollarClose >= 0 ? dollarClose : n);
      markFragmentQuotePairs(map, text, i, nl, CTX_DOLLAR_QUOTE);
      i = nl; continue;
    }
    if (!fragSq && c === '/' && nx === '*') {
      let end = skipBlockComment(text, i, 0, dollarClose); // r12 P2-8：以闭合定界符为实际扫描上限
      if (dollarClose >= 0 && dollarClose < end) end = dollarClose;
      markFragmentQuotePairs(map, text, i, end, CTX_DOLLAR_QUOTE);
      i = end; continue;
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

/**
 * 按嵌套深度消费块注释（codex r8 P2-1；r12 P2-8 加扫描上限）。
 * PostgreSQL 块注释支持嵌套（内层注释可开启于外层注释内部），
 * 用 indexOf 搜索首个结束符会在内层闭合处提前退出，使嵌套后的
 * 文本被误判为注释外，导致漏检。
 * r12 P2-8：`limit` 给出片段级扫描上限（如美元引用体的闭合定界符位置）——
 * 注释消费不得越过片段边界，否则体内 `/*` 会先扫到全文 EOF 再截回（O(n²)）。
 * @param {string} text
 * @param {number} from 起点；initialDepth=0 时指向注释开端 `/*` 的 `/`
 * @param {number} initialDepth 0=from 为注释开端；1=from 已在注释内
 * @param {number} [limit] 扫描上限（不含）；默认文本长度
 * @returns {number} 注释结束后的索引（未闭合则 limit）
 */
function skipBlockComment(text, from, initialDepth, limit = text.length) {
  let depth = initialDepth;
  let i = from;
  const n = Math.min(limit, text.length);
  while (i < n) {
    if (text[i] === '/' && text[i + 1] === '*') { depth += 1; i += 2; continue; }
    if (text[i] === '*' && text[i + 1] === '/') { depth -= 1; i += 2; if (depth <= 0) return i; continue; }
    i += 1;
  }
  return n;
}

/**
 * 片段内引号局部配对标记（r11 P2-6）：把 [from, to) 区间逐位置填为片段 ctx，
 * 并对区间内成对单引号做局部配对——闭引号标 CTX_SINGLE_QUOTE、'' 转义对第二引号
 * 保留片段 ctx。供片段内候选的 candInStr 判定（左侧最近引号是否闭标记）与分号
 * 边界解析（scanFragmentLex 的片段态配对）复用；状态局部不跨区间边界，
 * 注释外 fragSq 不受影响（r10 P2-7 原意）。
 * @param {Uint8Array} map sqlContextMap 状态数组
 * @param {string} text
 * @param {number} from 区间起点（含）
 * @param {number} to 区间终点（不含）
 * @param {number} fragCtx 片段填充上下文（CTX_DOLLAR_QUOTE / CTX_LINE_COMMENT / CTX_BLOCK_COMMENT）
 */
function markFragmentQuotePairs(map, text, from, to, fragCtx) {
  let locSq = false;
  for (let k = from; k < to; k++) {
    const ch = text[k];
    if (ch === "'") {
      if (isApostropheWord(text, k)) { map[k] = fragCtx; continue; } // 自然语言撇号不参与配对
      if (locSq && text[k + 1] === "'") { map[k] = fragCtx; map[k + 1] = fragCtx; k += 1; continue; } // '' 转义对
      if (locSq) { map[k] = CTX_SINGLE_QUOTE; locSq = false; continue; } // 闭引号
      locSq = true; map[k] = fragCtx; continue; // 开引号：保持片段 ctx
    }
    map[k] = fragCtx;
  }
}

/**
 * 片段内 SQL 迷你词法（codex r8 P2-9）：判定 [from, idx) 区间后的引号/注释状态。
 * 注释片段（-- / /*）与美元引用体内的 SQL 需要独立语句边界——分号仅在
 * 片段层级的「正常上下文」（不在字符串、不在片段内注释）才结束候选。
 * 与 sqlContextMap 的 fragSq 配对规则一致：'' 转义对、字母夹撇号不参与配对。
 * @param {string} text
 * @param {Uint8Array} ctxMap
 * @param {number} from 起始索引（通常为候选末尾）
 * @param {number} idx 判定位置
 * @param {boolean} fromInSq 起点是否已处于字符串内（codex r9 P2-7：候选位于
 *   片段内字符串中间时，首个引号事件之前的状态需由调用方告知）
 * @returns {{inSq:boolean, inComment:boolean}}
 */
function scanFragmentLex(text, ctxMap, from, idx, fromInSq = false) {
  let inSq = fromInSq;
  let inComment = false;
  let blockDepth = 0;
  let first = true;
  for (let k = from; k < idx; k++) {
    const c = text[k];
    if (inComment) {
      if (blockDepth > 0) {
        if (c === '/' && text[k + 1] === '*') { blockDepth += 1; k += 1; continue; }
        if (c === '*' && text[k + 1] === '/') { blockDepth -= 1; k += 1; continue; }
        continue;
      }
      if (c === '\n') inComment = false;
      continue;
    }
    if (c === "'") {
      if (isApostropheWord(text, k)) continue;
      const m = ctxMap[k];
      if (first) {
        // 首个引号为闭引号（片段态配对标记）→ 起点位于串内，该引号关闭后出串
        first = false;
        inSq = m !== CTX_SINGLE_QUOTE;
        continue;
      }
      if (m === CTX_SINGLE_QUOTE) { inSq = false; continue; }
      if (!inSq) inSq = true; // 串外片段态引号 = 开引号；串内引号（'' 转义对）保持
      continue;
    }
    if (inSq) continue;
    if (c === '-' && text[k + 1] === '-') { inComment = true; k += 1; continue; }
    if (c === '/' && text[k + 1] === '*') { inComment = true; blockDepth = 1; k += 1; continue; }
  }
  return { inSq, inComment };
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
 * YAML 属性名（anchor / tag）消费终点（r12 P2-1）：按 ns-char 词法消费至空白或
 * 流指示符（`,` `[` `]` `{` `}`）；`:` 后跟空白 / 流指示符为映射分隔符同样终止。
 * 原 `/[A-Za-z0-9_-]/` 字符集在 `&a.b` 的点号处截断属性——随后的 `.` 关闭
 * scalarStart，引用键（如 "k # x"）内的 `#` 被误判为行尾注释起点，块标量头漏识别
 * （YAML 规范允许 anchor 名包含点号等 ns-char）。
 * @param {string} line
 * @param {number} from 属性名起点
 * @returns {number} 属性名终点（其后首个非属性字符位置）
 */
function yamlAttrNameEnd(line, from) {
  let j = from;
  while (j < line.length && !/[\s\[\]{},]/.test(line[j])) {
    if (line[j] === ':' && (j + 1 >= line.length || /[\s\[\]{},]/.test(line[j + 1]))) break;
    j += 1;
  }
  return j;
}

/**
 * YAML 块标量头识别（codex r9 P2-2；r10 P2-1/2/3 词法边界加固）：仅在实际节点内容
 * 起点识别 | / > 指示符——先按 YAML 规则剥行尾注释（引号外的 `#`，位于行首或空白
 * 之后），再匹配 `|`/`>` + 缩进指示数字 + chomping 符号（`|2-` 与 `|-2` 两种顺序均合法）。
 * 内容起点自左向右定位：缩进 → 多层 `- ` 序列前缀 → [引号键|普通键] `:` → 节点属性
 * （`&anchor` / `!tag`，YAML 允许属性位于节点内容之前），其后到指示符之间只允许空白。
 * 由此 `command: &cmd |`（节点属性）、`command : |`（冒号前空格）正确识别，
 * plain 标量中段出现的 `- |` / `|2-`（如 SQL 注释文本）不再误入块分支。
 * 返回内容缩进基准 nodeIndent（块标量所属映射/序列条目的缩进，r9 P2-5：序列标记 `- ` 计入层级）。
 * @param {string} line YAML 物理行
 * @returns {null|{mark:string, chomp:string, indentIndicator:number, nodeIndent:number}}
 */
function matchYamlBlockHeader(line) {
  let inSq = false;
  let inDq = false;
  let hashAt = -1;
  let scalarStart = true; // r11 P2-2：引号仅在「标量起始位置」开启——plain 键内部引号保持普通字符
  for (let k = 0; k < line.length; k++) {
    const c = line[k];
    if (inSq) {
      if (c === "'") { if (line[k + 1] === "'") { k += 1; continue; } inSq = false; }
      continue;
    }
    if (inDq) {
      if (c === '\\') { k += 1; continue; }
      if (c === '"') inDq = false;
      continue;
    }
    if ((c === "'" || c === '"') && scalarStart) {
      if (c === "'") inSq = true; else inDq = true;
      scalarStart = false;
      continue;
    }
    if (c === '#' && (k === 0 || line[k - 1] === ' ' || line[k - 1] === '\t')) { hashAt = k; break; }
    if (scalarStart && (c === '&' || c === '!')) { // 节点属性后仍是标量起始（r11 P2-2）
      let j = k + 1;
      if (c === '&') {
        j = yamlAttrNameEnd(line, j); // r12 P2-1：anchor 名按 YAML 词法完整消费（含点号 / 非 ASCII）
      } else if (line[j] === '<') {
        const gt = line.indexOf('>', j + 1);
        j = gt < 0 ? line.length : gt + 1;
      } else {
        j = yamlAttrNameEnd(line, j); // r12 P2-1：tag 名同样完整消费
      }
      k = j - 1;
      continue;
    }
    if (c === ':' && (line[k + 1] === undefined || line[k + 1] === ' ' || line[k + 1] === '\t')) { scalarStart = true; continue; }
    if (c === '[' || c === '{' || c === ',') { scalarStart = true; continue; }
    if (c === '-' && /[ \t]/.test(line[k + 1] ?? '') && (k === 0 || /[ \t]/.test(line[k - 1]))) { scalarStart = true; continue; }
    if (c === ' ' || c === '\t') { continue; }
    scalarStart = false;
  }
  const content = (hashAt < 0 ? line : line.slice(0, hashAt)).replace(/[ \t]+$/, '');
  if (!content) return null;
  const m = /([|>])([+-]?)([1-9]?)([+-]?)$/.exec(content);
  if (!m) return null;
  const markerIdx = m.index;
  // —— r10 P2-1/2/3：自左向右定位「节点内容起点」，plain 标量中段的 - / | 不再误入块分支 ——
  const isWs = (k) => content[k] === ' ' || content[k] === '\t';
  let p = 0;
  while (p < markerIdx && isWs(p)) p += 1;
  let seqCol = -1;
  while (p < markerIdx && content[p] === '-' && isWs(p + 1)) {
    seqCol = p; // 取最内层序列标记列（与旧 lastIndexOf('-') 语义一致）
    p += 1;
    while (p < markerIdx && isWs(p)) p += 1;
  }
  const firstTok = p;
  // 从内容起点起仅允许空白与节点属性（&anchor / !tag），且必须恰好抵达指示符
  const reachesMarker = (from) => {
    let k = from;
    while (k < markerIdx && isWs(k)) k += 1;
    while (k < markerIdx && (content[k] === '&' || content[k] === '!')) {
      while (k < markerIdx && !isWs(k)) k += 1;
      while (k < markerIdx && isWs(k)) k += 1;
    }
    return k === markerIdx;
  };
  const indentIndicator = m[3] ? parseInt(m[3], 10) : 0;
  if (reachesMarker(p)) { // 文档级 / 序列直接块（可带属性）
    return { mark: m[1], chomp: m[2] || m[4] || '', indentIndicator, nodeIndent: seqCol >= 0 ? seqCol : p };
  }
  // 映射形态：收集 ':' 候选（跳过引号串），逐个验证内容起点（`foo:bar: |` 取可验证的分隔符）；
  // 冒号前空格（`command : |`）经键起点回溯兼容
  let q = p;
  while (q < markerIdx) {
    const ch = content[q];
    // 引号键：仅「键起始位置」的引号才是 YAML 引号标量（r11 P2-2）——
    // plain 键内部的引号（如 x-app's-command）保留为普通字符，不得当作引号串起点
    if ((ch === "'" || ch === '"') && q === firstTok) {
      let k = q + 1;
      while (k < markerIdx) {
        if (content[k] === ch) {
          if (content[k + 1] === ch) { k += 2; continue; } // '' / "" 转义对
          break;
        }
        if (ch === '"' && content[k] === '\\') { k += 2; continue; }
        k += 1;
      }
      q = k < markerIdx ? k + 1 : markerIdx;
      continue;
    }
    // 映射分隔符（r11 P2-1）：冒号后须为空白，否则是 plain 标量中段的 `:`（如 `-- :|`）
    if (ch === ':' && q + 1 < markerIdx && isWs(q + 1) && reachesMarker(q + 1)) {
      return { mark: m[1], chomp: m[2] || m[4] || '', indentIndicator, nodeIndent: firstTok };
    }
    q += 1;
  }
  return null;
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
    // YAML 节点属性（&anchor / !tag）：位于标量起始位置时消费属性 token，
    // 仍保持「等待标量内容」状态——属性后的引号才是引号标量（codex r8 P2-2）
    if (scalarStart && (c === '&' || c === '!')) {
      // r13 P2-6：属性名一律按 yamlAttrNameEnd（ns-char 词法）消费——与块标量头路径统一；
      // 原 `/[A-Za-z0-9_-]/` 在锚点名点号处截断，后续 `.` 关闭 scalarStart 致普通引用标量漏检
      let j = i + 1;
      if (c === '!' && line[j] === '<') {
        const gt = line.indexOf('>', j + 1);
        j = gt < 0 ? line.length : gt + 1;
      } else {
        j = yamlAttrNameEnd(line, j);
      }
      plain += line.slice(i, j);
      i = j;
      continue;
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

const PSQL_VAR_NAME_RE = /[A-Za-z0-9_$\u0080-\uFFFF]+/y;

/**
 * psql 变量插值引用长度（codex r9 P2-9；r10 P2-9 扩展；r11 P2-8 词法修正）：从冒号起
 * 识别 :'name' / :"name"（引用形式）与 :name（未引用形式），返回含冒号的总长度；
 * :: 类型转换、未闭合或跨行引用返回 0；未引用形式完整消费变量 token——
 * 允许数字起始与非 ASCII 字符，移除任意 64 字符截断（r11 P2-8）。
 * @param {string} text
 * @param {number} i 冒号位置
 * @returns {number}
 */
function psqlVarRefLength(text, i) {
  if (text[i] !== ':') return 0;
  const q = text[i + 1];
  if (q === ':') return 0; // :: 类型转换，非变量插值
  if (q === "'" || q === '"') {
    let k = i + 2;
    while (k < text.length && text[k] !== q && text[k] !== '\n') k += 1;
    if (k >= text.length || text[k] !== q) return 0;
    return k + 1 - i;
  }
  // 未引用形式 :name（r10 P2-9；r11 P2-8）：完整 token 消费，跨行边界自然不匹配
  PSQL_VAR_NAME_RE.lastIndex = i + 1;
  const m = PSQL_VAR_NAME_RE.exec(text);
  return m ? 1 + m[0].length : 0;
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
 * @returns {Array<{line:number, at:number, message:string}>} line 为片段内 1-based 行号，
 *   at 为片段内命中偏移（YAML 折叠还原行的物理行回映用，r10 P2-11）
 */
function scanSqlText(text) {
  const found = [];
  const n = text.length;
  if (!n) return found;
  const ctxMap = sqlContextMap(text);
  const { depthAt: parenDepth, openAt, commentLeft, commentRight } = buildParenDepthMap(text, ctxMap); // r11 P2-10 深度缓存；r12 P2-6 括号层开括号索引；r15 P2-1/P2-2 注释区间表
  // r14 P2-4：片段结束位置 O(1) 查询表——nextNormal[k] = 自 k 起首个 NORMAL 位置
  //（原实现每候选预扫描片段全程定位 fragEnd，候选数 × 片段长退化为 O(n²)）
  const nextNormal = new Int32Array(n + 1);
  nextNormal[n] = n;
  for (let k = n - 1; k >= 0; k--) nextNormal[k] = ctxMap[k] === CTX_NORMAL ? k : nextNormal[k + 1];
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
    const hit = scanCandidate(text, ctxMap, cand, parenDepth, openAt, nextNormal, commentLeft, commentRight);
    if (hit) {
      const stmtLabel = cand.kind === 'setpass' ? 'psql \\set ' : 'SQL ';
      found.push({ line: lineNo(hit.at), at: hit.at, message: stmtLabel + hit.message + hit.value.slice(0, 20) });
    }
  }
  return found;
}

/**
 * 解析单个 format 说明符（fmt[i] === '%'），沿给定游标消费其宽度/精度/主参数。
 * codex r10 P2-4：与 formatArgCursor 共用同一套词法，供目标占位符（%L 前导）
 * 解析复用——flags、数字宽度、`*` 与 `*n$` 宽度、精度、显式位置 n$ 统一处理。
 * @param {string} fmt 格式文本
 * @param {number} i '%' 位置
 * @param {number} cursor 进入说明符时的下一待消费实参序号（1-based）
 * @returns {{cursor:number, type:string, end:number}} cursor=消费后游标；type=类型字符（缺失为 ''）；end=说明符后一位
 */
function consumeFormatSpecifier(fmt, i, cursor) {
  const n = fmt.length;
  const digitEnd = (p) => {
    let d = p;
    while (d < n && fmt[d] >= '0' && fmt[d] <= '9') d += 1;
    return d;
  };
  let j = i + 1;
  const d0 = digitEnd(j); // 显式位置 n$
  let explicitPos = 0;
  if (d0 > j && fmt[d0] === '$') { explicitPos = parseInt(fmt.slice(j, d0), 10); j = d0 + 1; }
  while (j < n && /[+\- 0#]/.test(fmt[j])) j += 1; // flags
  if (fmt[j] === '*') { // 宽度参数：* 隐式消费，*n$ 显式位置
    const d1 = digitEnd(j + 1);
    if (d1 > j + 1 && fmt[d1] === '$') { cursor = parseInt(fmt.slice(j + 1, d1), 10) + 1; j = d1 + 1; }
    else { cursor += 1; j += 1; }
  }
  j = digitEnd(j); // 数字宽度
  if (fmt[j] === '.') { // 精度：. 与数字，支持 .*
    j += 1;
    if (fmt[j] === '*') {
      const d2 = digitEnd(j + 1);
      if (d2 > j + 1 && fmt[d2] === '$') { cursor = parseInt(fmt.slice(j + 1, d2), 10) + 1; j = d2 + 1; }
      else { cursor += 1; j += 1; }
    }
    j = digitEnd(j);
  }
  // 主参数消费（codex r9 P2-1）：显式位置 → 游标定位其后；隐式 → 顺序消费游标
  if (explicitPos > 0) cursor = explicitPos + 1;
  else cursor += 1;
  const type = j < n ? fmt[j] : '';
  j += 1; // 跳过类型字符（s / I / L 等）
  return { cursor, type, end: j };
}

/**
 * format 参数游标（codex r9 P2-1；r10 P2-4 抽取说明符解析）：按 PostgreSQL format()
 * 「从最后消费的参数继续」语义解析格式串片段——维护游标 cursor（下一个可用隐式参数序号）：
 * `%n$` 显式位置消费参数 n 后 cursor=n+1；隐式说明符消费 cursor 后 cursor+=1；
 * `%*` / `%*n$` 宽度参数同样消费实参；`%%` 转义与 flags / 数字宽度 / 精度不消耗。
 * 返回片段消费完后的 cursor = 紧随片段的下一个说明符所用参数序号（1-based）。
 * @param {string} fmt 格式串中位于目标占位符之前的文本片段
 * @returns {number} 目标占位符对应的实参序号
 */
function formatArgCursor(fmt) {
  const n = fmt.length;
  let cursor = 1;
  let i = 0;
  while (i < n) {
    if (fmt[i] !== '%') { i += 1; continue; }
    if (fmt[i + 1] === '%') { i += 2; continue; } // %% 转义，不消耗实参
    const r = consumeFormatSpecifier(fmt, i, cursor);
    cursor = r.cursor;
    i = r.end;
  }
  return cursor;
}

/**
 * format('… PASSWORD %L …', 实参…) 的占位符-实参映射（codex r7 P2-7）。
 * 仅当关键字位于 format 格式串内且其后紧跟 %L 时启用：口令取值即 %L 对应实参，
 * 只检查该实参内的字面量（%I 角色名等其它实参不属口令值）。
 * @returns {undefined} 非本场景（交回通用扫描）；null 已处理无违规；否则为命中对象
 */
function scanFormatPlaceholder(text, ctxMap, cand) {
  if (ctxMap[cand.pos] !== CTX_SINGLE_QUOTE) return undefined;
  // 关键字后须紧跟 format 说明符（codex r10 P2-4：完整说明符在占位符序号处统一解析）
  let si = cand.end;
  while (si < text.length && /\s/.test(text[si])) si += 1;
  if (text[si] !== '%') return undefined;
  let q = cand.pos - 1;
  while (q >= 0 && !(text[q] === "'" && ctxMap[q] === CTX_NORMAL)) q -= 1;
  if (q < 0) return undefined;
  if (!/format\s*\(\s*$/i.test(text.slice(Math.max(0, q - 24), q))) return undefined;
  let cq = cand.end;
  while (cq < text.length) {
    if (text[cq] === "'" && ctxMap[cq] === CTX_SINGLE_QUOTE) {
      // '' 转义对不结束格式串，须继续寻找真正的闭引号（codex r8 P2-4）
      if (text[cq + 1] === "'" && ctxMap[cq + 1] === CTX_SINGLE_QUOTE) { cq += 2; continue; }
      break;
    }
    cq += 1;
  }
  if (cq >= text.length) return undefined;
  // 占位符序号（1-based）：与前置占位符共用同一说明符解析器（codex r10 P2-4）——
  // 支持 %L / %n$L / %5L / %*L / %*n$L：宽度参数经游标消费后再定位 L 的值参数
  //（codex r9 P2-1：从最后消费的参数继续，显式位置与隐式序列共用同一游标）
  const spec = consumeFormatSpecifier(text, si, formatArgCursor(text.slice(q + 1, cand.pos)));
  if (spec.type !== 'L') return undefined;
  const placeholderIdx = spec.cursor - 1;
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
    const ctx = ctxMap[k];
    // 词法上下文跳过（codex r8 P2-5）：注释 / 美元引用 / 引用标识符内的
    // 分隔符不得参与切分；美元串与引用标识符本身是一个实参，须标记起点
    // r14 P2-3/P2-1：注释定界符两字符均带注释 ctx，进入点恒为第一字符——行注释跳转的
    // 换行符本身带注释尾标记，需防零步进（nl === k 时推进一格）；块注释按开端字符
    // 判定 skipBlockComment 初始深度（depth=0 起算，避免重复计入 '/*' 一层）
    if (ctx === CTX_LINE_COMMENT) { let nl = text.indexOf('\n', k); if (nl < 0) nl = n; if (nl === k) nl += 1; k = nl; continue; }
    if (ctx === CTX_BLOCK_COMMENT) { k = skipBlockComment(text, k, text[k] === '/' && text[k + 1] === '*' ? 0 : 1); continue; }
    if (ctx === CTX_NORMAL && c === '-' && text[k + 1] === '-') { const nl = text.indexOf('\n', k); k = nl < 0 ? n : nl; continue; }
    if (ctx === CTX_NORMAL && c === '/' && text[k + 1] === '*') { k = skipBlockComment(text, k, 0); continue; }
    if (ctx === CTX_DOLLAR_QUOTE) { while (k < n && ctxMap[k] === CTX_DOLLAR_QUOTE) k += 1; continue; }
    if (ctx === CTX_NORMAL && c === '$') {
      const tag = matchDollarTag(text, k);
      if (tag) {
        if (start < 0) start = k;
        const end = text.indexOf(tag, k + tag.length);
        k = end < 0 ? n : end + tag.length;
        continue;
      }
    }
    if (c === '"') {
      if (ctx === CTX_NORMAL) {
        if (start < 0) start = k;
        let j = k + 1;
        while (j < n) {
          if (text[j] === '"' && ctxMap[j] === CTX_DOUBLE_QUOTE) {
            if (text[j + 1] === '"' && ctxMap[j + 1] === CTX_DOUBLE_QUOTE) { j += 2; continue; } // "" 转义对
            break;
          }
          j += 1;
        }
        k = j < n ? j + 1 : n;
        continue;
      }
      k += 1; continue;
    }
    if (c === "'") {
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
 * 括号深度缓存（r11 P2-4/P2-10；r12 P2-2/P2-3/P2-6 扩展）：一次线性扫描生成
 * 「每个位置左侧未闭合括号数」与「每个位置所在括号层的开括号索引」，
 * 供候选的表达式窗口深度与 CAST 判定 O(1) 读取（替代原逐候选全文重扫的 O(n²)）。
 * 语义：字符串字面量 / 引用标识符整体跳过（串内括号不参与）；注释区间（行 / 块）
 * 按独立片段编制深度（r12 P2-2：从 0 起、( +1 / ) -1——注释不影响外层计数，
 * 但注释内候选的 exprDepth 必须读取片段自身的包装括号）；`;`（NORMAL 上下文）
 * 重置计数与括号栈、深度不为负；美元引用体做局部重扫（r12 P2-3）：体内注释 /
 * 字符串跳过，体内代码括号照常计入外层深度（被引用代码的括号结构有效）。
 * @param {string} text
 * @param {Uint8Array} ctxMap
 * @returns {{depthAt: Int32Array, openAt: Int32Array, commentLeft: Int32Array,
 *   commentRight: Int32Array}} depthAt[pos] = pos 左侧未闭合
 *   括号数；openAt[pos] = pos 所在括号层的开括号索引（顶层 / 片段内为 -1）；
 *   commentLeft / commentRight = 注释区间表（r15 P2-1/P2-2，-1=非注释区，区间
 *   [left, right) 含普通注释与片段内局部注释，供 CAST 判定左向整段跳过）
 */
function buildParenDepthMap(text, ctxMap) {
  const n = text.length;
  const depthAt = new Int32Array(n + 1);
  const openAt = new Int32Array(n).fill(-1);
  const commentLeft = new Int32Array(n).fill(-1); // r15 P2-1/P2-2：注释区间起点表
  const commentRight = new Int32Array(n).fill(-1); // 终点（不含）
  const openStack = []; // r12 P2-6：未闭合左括号位置栈（与 count 同步，`;` 清空）
  let count = 0;
  let i = 0;
  const topOpen = () => (openStack.length ? openStack[openStack.length - 1] : -1);
  // 注释文本的片段深度编制（r12 P2-2；r13 P2-3/P2-4/P2-5 重写为层栈）：
  //   - 独立括号栈：depthAt 从 0 起计、openAt 同步填栈顶（原实现只填 depthAt，
  //     注释内 CAST 判定永远失败——r13 P2-3）；
  //   - 嵌套块注释按层隔离：区间内 `/*` 压入新层（独立计数），`*/` 弹层——嵌套注释
  //     中的 `)` 不再抵消外层包装括号（r13 P2-4），嵌套内候选读自身层深度；
  //   - 字符串（'' / \' 转义）整段跳过；自然语言撇号（isApostropheWord）不开启字符串，
  //     与 sqlContextMap 词法一致（r13 P2-5）；
  //   - 双引号标识符（"" 转义）整段跳过（r14 P2-2）：片段内 `"/*"` 等标识符文本
  //     不被当嵌套注释开启（致片段层栈错位、包装括号深度误判）；
  //   - 每字符 O(1)、层栈线性，无回溯重扫。
  const fragTop = (layer) => (layer.stack.length ? layer.stack[layer.stack.length - 1] : -1);
  const fillFragment = (from, to) => {
    const end = Math.min(to, n);
    const layers = [{ d: 0, stack: [] }];
    let k = from;
    while (k < end) {
      const layer = layers[layers.length - 1];
      const cc = text[k];
      if (cc === "'" && !isApostropheWord(text, k)) {
        let j = k + 1;
        while (j < end) {
          // r15 P2-3（同约束）：层内未闭合引号不得吞掉关闭当前层的定界符
          if (layers.length > 1 && text[j] === '*' && text[j + 1] === '/') break;
          if (text[j] === "'") {
            if (text[j + 1] === "'") { j += 2; continue; }
            j += 1; break;
          }
          if (text[j] === '\\' && text[j + 1] === "'") { j += 2; continue; }
          j += 1;
        }
        const top = fragTop(layer);
        for (let t = k; t < j; t += 1) { depthAt[t] = layer.d; openAt[t] = top; }
        k = j;
        continue;
      }
      if (cc === '"') {
        // r14 P2-2：双引号标识符（"" 转义）整段消费——与单引号分支对称，
        // `"/*"` 等标识符文本不被当嵌套注释开启
        let j = k + 1;
        while (j < end) {
          // r15 P2-3：双引号消费受当前注释层的真实结束边界约束——层内未闭合
          // 引号不得吞掉关闭该层的定界符（嵌套层栈得以恢复）
          if (layers.length > 1 && text[j] === '*' && text[j + 1] === '/') break;
          if (text[j] === '"') {
            if (text[j + 1] === '"') { j += 2; continue; }
            j += 1; break;
          }
          j += 1;
        }
        const top = fragTop(layer);
        for (let t = k; t < j; t += 1) { depthAt[t] = layer.d; openAt[t] = top; }
        k = j;
        continue;
      }
      if (cc === '/' && text[k + 1] === '*') {
        // 嵌套块注释开启：内容独立成层（含候选的独立深度），`/*` 两字符留在外层
        const top = fragTop(layer);
        depthAt[k] = layer.d; openAt[k] = top;
        depthAt[k + 1] = layer.d; openAt[k + 1] = top;
        layers.push({ d: 0, stack: [] });
        k += 2;
        continue;
      }
      if (cc === '*' && text[k + 1] === '/' && layers.length > 1) {
        // 嵌套层闭合：先弹层再按外层值填充 '*/' 两字符
        layers.pop();
        const outer = layers[layers.length - 1];
        const top = fragTop(outer);
        depthAt[k] = outer.d; openAt[k] = top;
        depthAt[k + 1] = outer.d; openAt[k + 1] = top;
        k += 2;
        continue;
      }
      if (cc === '(') {
        layer.stack.push(k);
        layer.d += 1;
        depthAt[k] = layer.d; openAt[k] = k;
        k += 1;
        continue;
      }
      if (cc === ')') {
        if (layer.stack.length) layer.stack.pop();
        layer.d = layer.d > 0 ? layer.d - 1 : 0;
        depthAt[k] = layer.d; openAt[k] = fragTop(layer);
        k += 1;
        continue;
      }
      depthAt[k] = layer.d; openAt[k] = fragTop(layer);
      k += 1;
    }
  };
  // r15 P2-1/P2-2；r16 P2-1/P2-3：注释区间表构建——供 isCastCallParen 左向整段跳过注释。
  //   fillSpan：把 [s, e) 内尚未标记的字符补填为区间 [s, e)；遇到已填（更内层）
  //     段整段跳跃，分摊每字符 O(1)、总量 O(n)；
  //   markInnerCommentSpans：片段文本内按独立词法配对块注释——引号（'' / "" /
  //     \' 转义）整体消费、层内未闭合引号不吞关闭当前层的定界符（与 fillFragment
  //     同约束）；只处理块注释配对（行注释内的块注释符与块注释内的行注释符均无
  //     注释语义）；未闭合段不填（保守保持既有行为）。
  //   调用约定（r16）：真实块注释（普通 / 美元体）的外层区间由 skipBlockComment 的
  //   真实嵌套边界独立 fillSpan——注释内引号无词法语义，片段配对不得改写外层终点；
  //   片段配对范围仅取定界符之间的文本。美元体行注释与普通行注释一致：先标记
  //   文本内局部块注释、再回填整行。
  //   外层哨兵（r17 P2-1）：范围收窄让片段配对从空栈起步，引号消费的层界约束
  //   （starts.length > 0）失效——孤立未闭合引号会吞掉体内后续嵌套定界符段，
  //   局部注释漏标、CAST 判定漏检。coversOuterComment=true 时以哨兵压底，从
  //   「已在注释层内」状态起步（与 fillFragment 处理含外层定界符时的层栈一致）：
  //   引号消费自起点即受层界约束（r15 保护恢复）；哨兵永驻（`*/` 遇哨兵栈顶
  //   忽略不弹——该定界符关闭的层在范围外开启或被引号消费跳过，无对应可填
  //   区间），真实区间终点仍由调用方独立 fillSpan，不受哨兵影响。
  const fillSpan = (s, e) => {
    let t = s;
    while (t < e) {
      if (commentLeft[t] >= 0) { t = commentRight[t]; continue; }
      commentLeft[t] = s;
      commentRight[t] = e;
      t += 1;
    }
  };
  const OUTER_SENTINEL = -2; // 外层哨兵标记（与真实 `/*` 起始位置 >= 0 区分）
  const markInnerCommentSpans = (from, to, coversOuterComment) => {
    const starts = coversOuterComment ? [OUTER_SENTINEL] : [];
    let i = from;
    while (i < to) {
      const c = text[i];
      if (c === '/' && text[i + 1] === '*') { starts.push(i); i += 2; continue; }
      if (c === '*' && text[i + 1] === '/' && starts.length > 0) {
        if (starts[starts.length - 1] === OUTER_SENTINEL) { i += 2; continue; } // r17 P2-1：哨兵永驻，忽略不弹
        const s = starts.pop(); // 最内层优先：先闭合先填，外层回填时跳跃补全
        fillSpan(s, i + 2);
        i += 2; continue;
      }
      if (c === "'" && !isApostropheWord(text, i)) {
        let j = i + 1;
        while (j < to) {
          if (starts.length > 0 && text[j] === '*' && text[j + 1] === '/') break; // r15 P2-3 同约束
          if (text[j] === "'") {
            if (text[j + 1] === "'") { j += 2; continue; }
            j += 1; break;
          }
          if (text[j] === '\\' && text[j + 1] === "'") { j += 2; continue; }
          j += 1;
        }
        i = j; continue;
      }
      if (c === '"') {
        let j = i + 1;
        while (j < to) {
          if (starts.length > 0 && text[j] === '*' && text[j + 1] === '/') break;
          if (text[j] === '"') {
            if (text[j + 1] === '"') { j += 2; continue; }
            j += 1; break;
          }
          j += 1;
        }
        i = j; continue;
      }
      i += 1;
    }
  };
  while (i < n) {
    const c = text[i];
    const ctx = ctxMap[i];
    const start = i;
    if (ctx === CTX_LINE_COMMENT) {
      let nl = text.indexOf('\n', i);
      if (nl < 0) nl = n;
      if (nl === i) nl += 1; // 行尾换行符本身带注释尾标记：推进防零步进
      fillFragment(start, nl);
      // r15 P2-1/P2-2：注释区间表——先标记内部伪嵌套段（CAST 与括号之间夹注释
      // 文本时左向扫描可整段跳过），再整段回填外层区间；
      // r17：行注释体无真实外层注释层，与 fillFragment 基层语义一致不设哨兵
      markInnerCommentSpans(start + 2, nl, false);
      fillSpan(start, nl);
      i = nl;
      continue;
    }
    if (ctx === CTX_BLOCK_COMMENT) {
      // r14 P2-3：定界符两字符均标注释 ctx，进入点恒为 '/*' 第一字符——按开端字符
      // 定初始深度（避免 skipBlockComment 重复计入 '/*' 一层）
      i = skipBlockComment(text, i, text[i] === '/' && text[i + 1] === '*' ? 0 : 1);
      fillFragment(start, i);
      // r15 P2-1/P2-2；r16 P2-1：片段文本内局部块注释独立配对（引号语义）、
      // 外层区间按 skipBlockComment 的真实嵌套边界整段回填——注释内引号无
      // 词法语义（如 /* "/*" */ */），片段引号消费不得改写外层区间终点；
      // r17 P2-1：哨兵起步（真实外层为已开启层）——孤立引号不得吞掉体内
      // 后续嵌套定界符段
      markInnerCommentSpans(start + 2, i - 2, true);
      fillSpan(start, i);
      continue;
    }
    if (ctx === CTX_NORMAL && c === '$') { // r12 P2-3：美元引用体局部重扫
      const tag = matchDollarTag(text, i);
      if (tag) {
        const dc = text.indexOf(tag, i + tag.length);
        const bodyEnd = dc < 0 ? n : dc;
        depthAt.fill(count, i, i + tag.length); // 开定界符
        openAt.fill(topOpen(), i, i + tag.length);
        i += tag.length;
        while (i < bodyEnd) {
          const bc = text[i];
          if (bc === '-' && text[i + 1] === '-') { // 体内行注释：片段深度，不影响外层计数
            // r13 P2-9：换行搜索限定在美元体内（原 indexOf 无上限，单次即扫全文）
            const nl = newlineWithin(text, i, bodyEnd);
            fillFragment(i, nl);
            // r15 P2-1/P2-2；r16 P2-3：与普通行注释分支一致——先标记注释文本内
            // 局部块注释（CAST 与括号间夹注释同表跳过），再整段回填行注释区间；
            // r17：行注释体无真实外层注释层，不设哨兵
            markInnerCommentSpans(i + 2, nl, false);
            fillSpan(i, nl);
            i = nl; continue;
          }
          if (bc === '/' && text[i + 1] === '*') { // 体内块注释：同上
            const end = skipBlockComment(text, i, 0, bodyEnd);
            fillFragment(i, end);
            // r15 P2-1/P2-2；r16 P2-1：外层区间按真实嵌套边界回填、片段文本内局部块注释独立配对；
            // r17 P2-1：同普通块注释分支——哨兵起步（孤立引号不吞嵌套定界符段）
            markInnerCommentSpans(i + 2, end - 2, true);
            fillSpan(i, end);
            i = end; continue;
          }
          if (bc === "'") { // 体内字符串整体跳过（'' 转义）：串内括号不参与
            const strFrom = i;
            i += 1;
            while (i < bodyEnd) {
              if (text[i] === "'") {
                if (text[i + 1] === "'") { i += 2; continue; }
                i += 1; break;
              }
              if (text[i] === '\\' && text[i + 1] === "'") { i += 2; continue; }
              i += 1;
            }
            depthAt.fill(count, strFrom, i);
            openAt.fill(topOpen(), strFrom, i);
            continue;
          }
          if (bc === '(') { count += 1; openStack.push(i); depthAt[i] = count; openAt[i] = topOpen(); i += 1; continue; }
          if (bc === ')') { if (openStack.length) { openStack.pop(); count -= 1; } depthAt[i] = count; openAt[i] = topOpen(); i += 1; continue; }
          depthAt[i] = count;
          openAt[i] = topOpen();
          i += 1;
        }
        if (bodyEnd < n) {
          const end = Math.min(n, bodyEnd + tag.length);
          depthAt.fill(count, bodyEnd, end); // 闭定界符
          openAt.fill(topOpen(), bodyEnd, end);
          i = end;
        } else {
          i = n;
        }
        continue;
      }
    }
    // 开引号（NORMAL 及片段配对开形态）整段跳过：串内括号与转义引号不参与
    if (c === "'" && (ctx === CTX_NORMAL || ctx === CTX_DOLLAR_QUOTE || ctx === CTX_LINE_COMMENT || ctx === CTX_BLOCK_COMMENT)) {
      i += 1;
      while (i < n) {
        if (text[i] === "'") {
          if (text[i + 1] === "'") { i += 2; continue; }
          i += 1; break;
        }
        if (text[i] === '\\' && text[i + 1] === "'") { i += 2; continue; }
        i += 1;
      }
      depthAt.fill(count, start, i);
      openAt.fill(topOpen(), start, i);
      continue;
    }
    if (c === '"' && ctx === CTX_NORMAL) {
      i += 1;
      while (i < n) {
        if (text[i] === '"') {
          if (text[i + 1] === '"') { i += 2; continue; }
          i += 1; break;
        }
        i += 1;
      }
      depthAt.fill(count, start, i);
      openAt.fill(topOpen(), start, i);
      continue;
    }
    if (ctx === CTX_NORMAL && c === ';') { count = 0; openStack.length = 0; depthAt[start] = count; openAt[start] = -1; i += 1; continue; }
    if (c === '(') { count += 1; openStack.push(start); depthAt[start] = count; openAt[start] = topOpen(); i += 1; continue; }
    if (c === ')') { if (openStack.length) { openStack.pop(); count -= 1; } depthAt[start] = count; openAt[start] = topOpen(); i += 1; continue; }
    depthAt[start] = count;
    openAt[start] = topOpen();
    i += 1;
  }
  depthAt[n] = count;
  return { depthAt, openAt, commentLeft, commentRight };
}

/**
 * CAST 类型短语消费（r11 P2-3；r12 P2-4/P2-5 词法化）：从 AS 之后消费完整类型说明——
 * 多词类型（character varying / double precision）、引用标识符（"text"）、
 * 类型参数（numeric(10,2)）、数组修饰（int[]）、限定名（pg_catalog.text）。
 * r12 P2-5：引用类型独立解析（"" 转义），不依赖仅由外层 SQL 生成的 ctxMap 标记——
 * 美元体内双引号带 CTX_DOLLAR_QUOTE 时仍能消费；限定名（pg_catalog."text"）同理。
 * r12 P2-4：类型参数按局部词法配对——注释 / 字符串 / 双引号整体跳过（类型参数内
 * 注释中的闭括号不参与配对）；类型词之间的注释按空白消费。
 * 仅消费类型成分，遇其它字符（`)` `,` `||` 等）即交还主状态机。
 * @param {string} text
 * @param {number} from AS 词末尾
 * @param {number} n
 * @returns {number} 类型短语末尾（其后首个非类型字符位置）
 */
function consumeCastType(text, from, n) {
  let k = from;
  for (;;) {
    // 类型词之间的注释按空白消费（r12 P2-4）
    for (;;) {
      if (k < n && /\s/.test(text[k])) { k += 1; continue; }
      if (k < n && text[k] === '-' && text[k + 1] === '-') { const nl = text.indexOf('\n', k); k = nl < 0 ? n : nl; continue; }
      if (k < n && text[k] === '/' && text[k + 1] === '*') { k = skipBlockComment(text, k, 0); continue; }
      break;
    }
    const ch = text[k];
    if (ch === '"') { // 引用标识符独立解析（r12 P2-5）："" 转义对
      k += 1;
      while (k < n) {
        if (text[k] === '"') {
          if (text[k + 1] === '"') { k += 2; continue; }
          k += 1; break;
        }
        k += 1;
      }
      continue;
    }
    if (ch === '(') { // 类型参数按局部词法配对（r12 P2-4）：注释 / 字符串 / 双引号内括号不参与
      let d = 1;
      k += 1;
      while (k < n && d > 0) {
        const cc = text[k];
        if (cc === '-' && text[k + 1] === '-') { const nl = text.indexOf('\n', k); k = nl < 0 ? n : nl; continue; }
        if (cc === '/' && text[k + 1] === '*') { k = skipBlockComment(text, k, 0); continue; }
        if (cc === "'") {
          k += 1;
          while (k < n) {
            if (text[k] === "'") {
              if (text[k + 1] === "'") { k += 2; continue; }
              k += 1; break;
            }
            if (text[k] === '\\' && text[k + 1] === "'") { k += 2; continue; }
            k += 1;
          }
          continue;
        }
        if (cc === '"') {
          k += 1;
          while (k < n) {
            if (text[k] === '"') {
              if (text[k + 1] === '"') { k += 2; continue; }
              k += 1; break;
            }
            k += 1;
          }
          continue;
        }
        if (cc === '(') d += 1;
        else if (cc === ')') d -= 1;
        k += 1;
      }
      continue;
    }
    if (ch && (IDENT_CHAR_RE.test(ch) || ch === '.' || ch === '[' || ch === ']')) { k += 1; continue; }
    break;
  }
  return k;
}

/**
 * 跳到当前括号层的右括号（r11 P2-5；r12 P2-7 片段模式）：子查询 WHERE 的筛选条件
 * 整体跳过——跟踪相对深度（`(` +1 / `)` 归零即本层闭括号）、字符串 / 注释 / 美元引用
 * 区间不参与；NORMAL 上下文分号即语句边界（返回 -1）。
 * r12 P2-7：`fragCtx >= 0` 时进入片段模式（候选位于注释 / 美元引用体内）——按片段
 * 局部词法配对括号：ctx 复位（片段结束）即边界，片段内注释 / 字符串 / 分号不参与
 * 配对；不得整体跳过整个片段上下文（否则找不到片段内的真实右括号）。
 * r13 P2-7/P2-8：片段模式先定位片段结束位置（ctx 复位 NORMAL）作为全部跳转的硬上限——
 * 注释 / 字符串 / 双引号消费不得越过片段边界（防跨片段误报）；双引号标识符
 * （"" 转义）整体消费，其中的 `)` 不参与配对。
 * r14 P2-3/P2-4：注释定界符两字符均标注释 ctx，片段结束（首个 NORMAL）恒为真实边界；
 * fragEnd 改由 nextNormal 查询表 O(1) 读取（原每候选预扫描片段全程，O(n²)）。
 * @param {string} text
 * @param {Uint8Array} ctxMap
 * @param {number} from 起点（WHERE 词末尾）
 * @param {number} n
 * @param {number} [fragCtx] 候选所在片段 ctx（-1=普通 SQL 上下文）
 * @param {Int32Array} [nextNormal] 片段结束查询表：nextNormal[k]=自 k 起首个 NORMAL 位置（fragCtx >= 0 时必传）
 * @returns {number} 本层右括号位置；未找到 / 语句边界返回 -1
 */
function skipToClosingParen(text, ctxMap, from, n, fragCtx = -1, nextNormal) {
  let d = 0;
  let i = from;
  while (i < n) {
    const c = text[i];
    const ctx = ctxMap[i];
    if (fragCtx >= 0) { // r12 P2-7 / r13 P2-7/P2-8：片段模式——局部词法配对 + 片段边界硬上限
      // r13 P2-8：片段结束 = 自 from 起首个脱离片段词法的位置（ctxMap 复位 NORMAL）；
      // 行注释 / 块注释 / 字符串 / 双引号跳转与循环推进全部以此为界——
      // 禁止越过美元闭合定界符等片段边界侵入后续独立 SQL（跨片段误报）
      // r14 P2-3：定界符两字符均标注释 ctx，片段区间连续、首个 NORMAL 即真实边界；
      // r14 P2-4：O(1) 查表（原每候选预扫描片段全程）
      const fragEnd = nextNormal[from];
      while (i < fragEnd) {
        const cc2 = text[i];
        const ctx2 = ctxMap[i];
        if (ctx2 === fragCtx) {
          if (cc2 === '-' && text[i + 1] === '-') { i = newlineWithin(text, i, fragEnd); continue; }
          if (cc2 === '/' && text[i + 1] === '*') { i = skipBlockComment(text, i, 0, fragEnd); continue; }
          if (cc2 === ';') return -1; // 片段内语句终止（与 NORMAL 分号语义对齐）
          if (cc2 === '(') { d += 1; i += 1; continue; }
          if (cc2 === ')') { if (d === 0) return i; d -= 1; i += 1; continue; }
        }
        if (cc2 === "'" && ctx2 !== CTX_SINGLE_QUOTE) {
          if (isApostropheWord(text, i)) { i += 1; continue; } // 自然语言撇号不参与
          // 片段内开引号（配对开形态）：局部扫至闭引号标记（'' 转义对保留片段 ctx）
          i += 1;
          while (i < fragEnd) {
            if (text[i] === "'") {
              if (ctxMap[i] === CTX_SINGLE_QUOTE) { i += 1; break; }
              if (ctxMap[i] === fragCtx && text[i + 1] === "'" && ctxMap[i + 1] === fragCtx) { i += 2; continue; }
            }
            i += 1;
          }
          continue;
        }
        // r13 P2-7：双引号标识符整体消费（"" 转义），其中的 ) 不参与配对——
        // 原实现将 `")"` 内的 ) 当作真实子查询闭括号致误报
        if (cc2 === '"') {
          i += 1;
          while (i < fragEnd) {
            if (text[i] === '"') {
              if (text[i + 1] === '"') { i += 2; continue; }
              i += 1; break;
            }
            i += 1;
          }
          continue;
        }
        i += 1;
      }
      return -1; // 片段内未找到本层右括号（含 i 已达片段边界）
    }
    if (ctx === CTX_LINE_COMMENT) { let nl = text.indexOf('\n', i); if (nl < 0) nl = n; if (nl === i) nl += 1; i = nl; continue; }
    if (ctx === CTX_BLOCK_COMMENT) { i = skipBlockComment(text, i, text[i] === '/' && text[i + 1] === '*' ? 0 : 1); continue; } // r14 P2-3：'/*' 第一字符进入
    if (ctx === CTX_DOLLAR_QUOTE) { i += 1; continue; }
    if (c === "'" && (ctx === CTX_NORMAL || ctx === CTX_ESCAPE_STRING)) {
      i += 1;
      while (i < n) {
        if (text[i] === "'") {
          if (text[i + 1] === "'") { i += 2; continue; }
          i += 1; break;
        }
        if (text[i] === '\\' && text[i + 1] === "'") { i += 2; continue; }
        i += 1;
      }
      continue;
    }
    if (c === '"' && ctx === CTX_NORMAL) {
      i += 1;
      while (i < n) {
        if (text[i] === '"') {
          if (text[i + 1] === '"') { i += 2; continue; }
          i += 1; break;
        }
        i += 1;
      }
      continue;
    }
    if (ctx === CTX_NORMAL && c === ';') return -1;
    if (c === '(') { d += 1; i += 1; continue; }
    if (c === ')') { if (d === 0) return i; d -= 1; i += 1; continue; }
    i += 1;
  }
  return -1;
}

/**
 * 单个口令候选的取值解析（codex r7 语句级结构化取值；r8 边界加固）。
 * psql \set 为单行元命令：参数不能跨行（r8 P2-9，psql 文档），防候选跑野到后续语句。
 * 语句边界：NORMAL 分号 / 片段层级分号（注释与美元引用体内，r8 P2-9）/ 未引用反斜杠
 *（psql 元命令，支持同行 SQL 与元命令混合，r8 P2-9）/ 候选所在片段结束。
 * @param {string} text 全文
 * @param {Uint8Array} ctxMap sqlContextMap(text)
 * @param {{pos:number,end:number,kind:string}} cand 关键字候选（pos=起始，end=末尾后一位）
 * @param {Int32Array} nextNormal r14 P2-4：片段结束 O(1) 查询表（自位置起首个 NORMAL）
 * @param {Int32Array} commentLeft r15 P2-1/P2-2：注释区间起点表（-1=非注释区）
 * @param {Int32Array} commentRight 注释区间终点表（不含）
 * @returns {null|{at:number,value:string,message:string}} 命中位置 / 值 / 消息主体
 */
function scanCandidate(text, ctxMap, cand, parenDepth, openAt, nextNormal, commentLeft, commentRight) {
  const n = text.length;
  const candCtx = ctxMap[cand.pos];
  if (candCtx === CTX_DOUBLE_QUOTE) return null; // 双引号标识符内的 PASSWORD 字样非口令语句
  // 片段内候选：行/块注释内容与美元引用体（r8 P2-9 扩展 DOLLAR_QUOTE）
  const candInFragment = candCtx === CTX_LINE_COMMENT || candCtx === CTX_BLOCK_COMMENT || candCtx === CTX_DOLLAR_QUOTE;
  // 候选位于片段内字符串中（codex r9 P2-7）：左侧最近引号为开形态（非闭引号标记）
  let candInStr = false;
  if (candInFragment) {
    const pq = text.lastIndexOf("'", cand.pos);
    candInStr = pq >= 0 && ctxMap[pq] !== CTX_SINGLE_QUOTE;
  }
  const fmt = scanFormatPlaceholder(text, ctxMap, cand);
  if (fmt !== undefined) return fmt;

  let i = cand.end;
  let mode = 'value'; // value=待取取值 token；after=直接取值已判定（仅 || / 相邻串可延续）；expr=拼接口令表达式窗口
  let exprDepth = 0; // expr 模式：相对括号深度（r8 P2-8）
  let exprExpectOperand = false; // expr 模式：|| 后等待操作数（函数名/变量）
  let inBacktick = false; // psql 反引号引用（r9 P2-8）：内容交 shell，引号内反斜杠非元命令边界
  while (i < n) {
    const c = text[i];
    const ctx = ctxMap[i];

    if (cand.kind === 'setpass' && (c === '\n' || c === '\r')) {
      return null; // psql 元命令边界：参数不能跨行（r8 P2-9，删除 r7.1 行尾反斜杠续行假设）
    }
    if (c === ' ' || c === '\t' || c === '\n' || c === '\r' || c === '\f') { i += 1; continue; }
    if (candInFragment && i > cand.end && ctx === CTX_NORMAL) return null; // 片段结束
    if (ctx === CTX_NORMAL && c === '`') { inBacktick = !inBacktick; i += 1; continue; } // psql 反引号引用切换（r9 P2-8）
    // 片段内注释消费（codex r9 P2-7）：美元体 / 注释片段内的 -- 与 /* */ 属片段文本词法，
    // 先于表达式状态机消费（嵌套块注释按深度）；候选起点位于串内时串内不消费
    if (candInFragment && ctx === candCtx && (c === '/' || c === '-')) {
      const isBlock = c === '/' && text[i + 1] === '*';
      const isLine = c === '-' && text[i + 1] === '-';
      if (isBlock || isLine) {
        const fragLex = scanFragmentLex(text, ctxMap, cand.end, i, candInStr);
        if (!fragLex.inSq) {
          if (isBlock) { i = skipBlockComment(text, i, 0); continue; }
          const nl = text.indexOf('\n', i);
          i = nl < 0 ? n : nl;
          continue;
        }
      }
    }
    if (!candInFragment && !inBacktick) {
      // 注释按嵌套深度跳转（不依赖片段配对标记，避免被注释内配对闭合引号干扰）；
      // r11 P2-9：反引号内的 -- 是 shell 参数（如 printf -- 'x'），不得按 SQL 注释消费
      if (ctx === CTX_LINE_COMMENT) { const nl = text.indexOf('\n', i); i = nl < 0 ? n : nl; continue; }
      if (ctx === CTX_BLOCK_COMMENT) { i = skipBlockComment(text, i, text[i] === '/' && text[i + 1] === '*' ? 0 : 1); continue; } // r8 P2-1 / r14 P2-3：'/*' 第一字符进入
      if (ctx === CTX_NORMAL && c === '-' && text[i + 1] === '-') { const nl = text.indexOf('\n', i); i = nl < 0 ? n : nl; continue; }
      if (ctx === CTX_NORMAL && c === '/' && text[i + 1] === '*') { i = skipBlockComment(text, i, 0); continue; } // r8 P2-1
    }
    if (c === ';') {
      if (ctx === CTX_NORMAL && !inBacktick) return null; // 语句终止：未取到不安全口令（反引号内 shell 文本除外）
      if (candInFragment && ctx === candCtx) {
        // 片段层级语句边界（r8 P2-9）：分号仅在片段内「正常上下文」结束候选，
        // 字符串内与片段内注释中的分号保留（串内起点由 candInStr 告知，r9 P2-7）
        const lex = scanFragmentLex(text, ctxMap, cand.end, i, candInStr);
        if (!lex.inSq && !lex.inComment) return null;
      }
    }
    if (c === '\\' && ctx === CTX_NORMAL && !inBacktick) {
      // psql 元命令边界（r8 P2-9）：未引用反斜杠即切换元命令，支持同行混合；
      // 反引号内容交 shell（r9 P2-8），引号内反斜杠不作边界
      return null;
    }

    if (c === ':' && (mode === 'after' || mode === 'expr') && ctx === CTX_NORMAL) {
      // psql 变量插值 :'name' / :"name"（r9 P2-9）：值来自变量，跳过后保持模式继续拼接
      const vl = psqlVarRefLength(text, i);
      if (vl) { i += vl; continue; }
    }

    if (mode === 'after') {
      // 反引号内 shell 文本（r9 P2-8；r10 P2-8）：非引号字符继续推进寻找字面量，
      // 引号字面量交给下方取值检查（shell 命令里的 '...' 同样是硬编码值来源）
      if (inBacktick && c !== "'") { i += 1; continue; }
      if (c === '|' && text[i + 1] === '|') { mode = 'value'; i += 2; continue; }
      if (c === "'" && ctx === CTX_NORMAL) {
        if (text[i - 1] === ':') {
          // psql 变量插值 :'name'：值来自变量，跳过字面量后继续（保持 after）
          const v = extractSqlValue(text, i, n);
          const j = i + 1 + v.length;
          i = j < n && text[j] === "'" ? j + 1 : j;
          continue;
        }
        // 相邻字符串字面量与 psql \set 多参数均按拼接继续检查（r8 P2-7）
        const v = extractSqlValue(text, i, n);
        const closed = i + 1 + v.length < n && text[i + 1 + v.length] === "'";
        if (isUnsafeValue(v)) return { at: i, value: v, message: '出现硬编码口令字面量: ' };
        i = i + 1 + v.length + (closed ? 1 : 0);
        continue;
      }
      return null; // 取值已判定，后续非拼接 token（如 VALID UNTIL 选项）结束本候选
    }

    if (mode === 'value' && /^NULL\b/i.test(text.slice(i, i + 4)) && !IDENT_CHAR_RE.test(text[i - 1] ?? '')) {
      return null; // PASSWORD NULL：无口令取值（P2-6）
    }

    if (mode === 'expr' && c !== "'") {
      // 拼接口令表达式窗口（r7 P2-4；r8 P2-8 绑定表达式边界；r10 P2-5 子查询隔离）：
      // 表达式 = 操作数（|| 操作数）*，操作数为字面量 / 函数调用 / 变量；括号组内
      // 已消费完操作数后紧跟标识符（WHERE / SELECT / AS 等）即子表达式结束——
      // 不再把 WHERE 筛选条件等的字面量当作拼接值
      if (c === '(') { exprDepth += 1; exprExpectOperand = true; i += 1; continue; }
      if (c === ')') {
        if (exprDepth === 0) return null; // 外层结构闭合 → 表达式结束
        exprDepth -= 1;
        exprExpectOperand = false;
        i += 1; continue;
      }
      if (c === '|' && text[i + 1] === '|') { exprExpectOperand = true; i += 2; continue; }
      if (c === ',' && exprDepth > 0) { exprExpectOperand = true; i += 1; continue; } // 函数实参分隔
      if (IDENT_CHAR_RE.test(c)) {
        if (!exprExpectOperand) {
          // CAST 语法（r9 P2-6 回归保障）：CAST(expr AS type) 的 AS 与类型名是
          // 语法成分，跳过并继续；其余标识符（WHERE / SELECT / FROM 等）后跟
          // 即子表达式结束——不把筛选条件字面量当作拼接值（r10 P2-5）
          let w = i;
          while (w < n && IDENT_CHAR_RE.test(text[w])) w += 1;
          if (/^AS$/i.test(text.slice(i, w))) {
            // r13 P2-2：以 AS 词所在括号层判定 CAST——原 openAt[cand.pos] 读候选位置的层，
            // 无法反映 AS 前已闭合的内层包装括号（CAST((...) AS t) 漏检），且外层别名
            // 会沿用内层 CAST 判定把 WHERE 常量当拼接值（误报）
            const openPos = openAt[i];
            i = w;
            // r13 P2-1：完整 token 左向词法（跳过空白 / 注释 + 词边界），取代 8 字符窗口
            if (openPos >= 0 && isCastCallParen(text, ctxMap, openPos, commentLeft, commentRight)) {
              // r11 P2-3：CAST 括号带内 AS 为类型转换语法——消费完整类型短语
              // （多词类型 / 引用标识符 / 类型参数 / 数组修饰），不让类型后继词终结表达式
              i = consumeCastType(text, i, n);
              continue;
            }
            while (i < n && /\s/.test(text[i])) i += 1;
            while (i < n && (IDENT_CHAR_RE.test(text[i]) || text[i] === '.' || text[i] === '[' || text[i] === ']')) i += 1;
            continue;
          }
          if (/^WHERE$/i.test(text.slice(i, w))) {
            // r11 P2-5：子查询 WHERE——跳过该查询的筛选条件至本层右括号，恢复外层表达式扫描；
            // 无本层右括号（普通 SELECT 的 WHERE）则在语句边界终止，保留 r10 不误报语义；
            // r12 P2-7：候选在片段（注释 / 美元体）内时按片段局部词法配对（fragCtx）
            const close = skipToClosingParen(text, ctxMap, w, n, candInFragment ? candCtx : -1, nextNormal);
            if (close < 0) return null;
            i = close;
            continue;
          }
          return null; // 操作数后直接跟标识符 → 子表达式结束
        }
        while (i < n && (IDENT_CHAR_RE.test(text[i]) || text[i] === '.')) i += 1; // 函数名 / 变量路径
        exprExpectOperand = false; continue;
      }
      if (exprDepth > 0) { i += 1; continue; } // 括号组内其它符号（空白 / 运算符）自由推进
      return null; // 表达式结束
    }

    if (c === "'") {
      if (ctx === CTX_SINGLE_QUOTE || ctx === CTX_ESCAPE_STRING) {
        // 闭引号 → 拼接口令表达式窗口（r8 P2-8：重置表达式状态；
        // r9 P2-6：继承候选左侧已开启的外层包装括号深度）
        mode = 'expr';
        exprDepth = parenDepth[cand.pos]; // r11 P2-10：深度缓存 O(1) 读取
        exprExpectOperand = false;
        i += 1; continue;
      }
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
  // SQL/psql 原生硬编码凭据（codex r6：候选独立解析 + YAML 上下文隔离；r7/r8：块标量收集还原；
  // r9：块头词法边界 / 显式缩进 / 折叠语义 / 序列节点层级）：
  //   - 非 YAML（SQL/.env/conf）：全文建 SQL 词法上下文，注释 / 美元引用内做片段级配对；
  //   - YAML：先按片段隔离（注释 / 引号标量 / 普通文本），标量按 YAML 规则解码后独立建 SQL 上下文，
  //     防注释撇号跨行污染（P2-2）、外层引号转义未解码被漏检（P2-2）；
  //     | / > 块标量（r8 P2-6；r9 P2-2/3/4/5）：仅实际节点内容起点识别块头，内容行整体收集、
  //     按显式缩进或首非空行剥内容缩进（r9 P2-3），同级节点（含序列标记层级，r9 P2-5）
  //     终止收集并交还外层 YAML 解码；`>` 按 YAML 折叠还原（r9 P2-4：单换行→空格、
  //     空行→换行、more-indented 保留换行），行号映射回物理行。
  if (isYaml) {
    for (let i = 0; i < lines.length; i++) {
      const line = lines[i];
      // 块标量指示符（codex r9 P2-2：YAML 词法边界识别——仅实际节点内容起点，
      // 注释 / 普通标量内指示符不触发；支持 |2- 与 |-2 两种指示顺序）：
      // 整体收集内容行，按显式缩进（r9 P2-3）或首非空行剥内容缩进；
      // 同级节点（含序列标记层级，r9 P2-5）终止收集并交还外层按普通 YAML 行处理；
      // `>` 按 YAML 折叠还原（r9 P2-4），并维护还原行到物理行的映射
      const header = matchYamlBlockHeader(line);
      if (header) {
        const { mark, indentIndicator, nodeIndent } = header;
        const rows = [];
        const physRows = [];
        let j = i + 1;
        for (; j < lines.length; j++) {
          const cl = lines[j];
          if (/^\s*$/.test(cl)) { rows.push(null); physRows.push(j); continue; }
          // 缩进仅统计空格（r10 P2-10）：tab 不是 YAML 缩进，以 tab 开头的行按内容收集
          if (cl.match(/^ */)[0].length <= nodeIndent && cl[0] !== '\t') break;
          rows.push(cl); physRows.push(j);
        }
        while (rows.length && rows[rows.length - 1] === null) { rows.pop(); physRows.pop(); } // 块后尾随空行不属内容
        if (rows.length) {
          let base = indentIndicator ? nodeIndent + indentIndicator : null;
          if (base === null) {
            for (const r of rows) { if (r === null) continue; base = r.match(/^ */)[0].length; break; } // 缩进仅空格（r10 P2-10）
          }
          if (base === null) base = nodeIndent + 1;
          const outLines = [];
          const outPhys = [];
          const outSegs = []; // r10 P2-11：输出行内内容段（{start,end,phys}），折叠后精确回映物理行
          let blanks = 0;
          let prevMore = false;
          for (let r = 0; r < rows.length; r++) {
            const row = rows[r];
            if (row === null) { blanks += 1; continue; }
            const ind = row.match(/^ */)[0].length; // 缩进仅空格（r10 P2-10）
            // more-indented（r10 P2-10）：空格缩进更深，或剥除基准缩进后以空白（含 tab）开头
            const more = ind > base || /^\s/.test(row.slice(base));
            const textRow = row.slice(Math.min(base, ind));
            if (mark !== '>' || outLines.length === 0 || blanks > 0 || prevMore || more) {
              for (let b = 0; b < blanks; b++) { outLines.push(''); outPhys.push(physRows[r]); outSegs.push([]); }
              outLines.push(textRow); outPhys.push(physRows[r]);
              outSegs.push([{ start: 0, end: textRow.length, phys: physRows[r] }]);
            } else {
              const segStart = outLines[outLines.length - 1].length + 1;
              outLines[outLines.length - 1] += ' ' + textRow; // 折叠：单换行 → 空格
              outSegs[outSegs.length - 1].push({ start: segStart, end: outLines[outLines.length - 1].length, phys: physRows[r] });
            }
            blanks = 0; prevMore = more;
          }
          const restored = outLines.join('\n');
          const outStarts = [];
          { let acc = 0; for (const l of outLines) { outStarts.push(acc); acc += l.length + 1; } }
          for (const f of scanSqlText(restored)) {
            const li = f.line - 1;
            const col = f.at - outStarts[li];
            let physLine = null;
            const segs = outSegs[li] || [];
            for (const s of segs) { if (col >= s.start && col <= s.end) { physLine = s.phys; break; } }
            if (physLine === null && segs.length) physLine = segs[segs.length - 1].phys;
            issues.push({ rule: 'C1-secret', path: relPath, line: (physLine ?? outPhys[li] ?? i) + 1, message: f.message });
          }
        }
        i = j - 1; // 内容行已整体处理，从终止行继续
        continue;
      }
      for (const seg of splitYamlSegments(line)) {
        const frag = seg.kind === 'sq' ? decodeYamlSingle(seg.raw)
          : seg.kind === 'dq' ? decodeYamlDouble(seg.raw) : seg.raw;
        for (const f of scanSqlText(frag)) {
          issues.push({ rule: 'C1-secret', path: relPath, line: i + 1, message: f.message });
        }
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

  // r8 P2-9：psql 元命令参数不能跨行（官方文档：arguments of a meta-command cannot
  // continue beyond the end of the line），行尾反斜杠即元命令边界，不作续行处理
  const setContinue = checkSecrets('init.sql', "\\set app_pass \\\n'RealSecret123'");
  results.push(['C1 正向（\\set 参数不跨行，行尾反斜杠不续行，r8 P2-9）', setContinue.length === 0]);

  // ---- codex r8 回归（嵌套注释 / YAML 属性 / format 占位符 / 切参词法 / 块标量 / 相邻串 / 表达式边界 / 元命令行内边界） ----
  const nestedBlockComment = checkSecrets('init.sql', "CREATE ROLE app LOGIN PASSWORD /* outer /* inner */*/ 'RealSecret123';");
  results.push(['C1 负向（嵌套块注释深度消费后口令仍检出，r8 P2-1）', nestedBlockComment.length > 0]);

  const yamlAnchorAttr = checkSecrets('compose.yml', "command: [psql, -c, &sql \"CREATE ROLE app LOGIN PASSWORD 'RealSecret123';\"]");
  results.push(['C1 负向（YAML anchor 属性后引号标量仍拆解，口令检出，r8 P2-2）', yamlAnchorAttr.length > 0]);

  const yamlTagAttr = checkSecrets('compose.yml', "command: [psql, -c, !!str \"CREATE ROLE app LOGIN PASSWORD 'RealSecret123';\"]");
  results.push(['C1 负向（YAML !!tag 属性后引号标量仍拆解，口令检出，r8 P2-2）', yamlTagAttr.length > 0]);

  const formatPctEscape = checkSecrets('init.sql', "SELECT format('CREATE ROLE \"app%%I\" LOGIN PASSWORD %L', 'RealSecret123')");
  results.push(['C1 负向（%% 不消耗实参，%L 实参定位正确仍检出，r8 P2-3）', formatPctEscape.length > 0]);

  const formatSqEscape = checkSecrets('init.sql', "SELECT format('CREATE ROLE app LOGIN PASSWORD %L VALID UNTIL ''infinity''', 'RealSecret123')");
  results.push(['C1 负向（格式串双单引号转义不提前闭合，%L 实参仍检出，r8 P2-4）', formatSqEscape.length > 0]);

  const argCommentParen = checkSecrets('init.sql', "SELECT format('CREATE ROLE app LOGIN PASSWORD %L', /* ) */ 'RealSecret123')");
  results.push(['C1 负向（实参切分在注释内不截断，%L 实参仍检出，r8 P2-5）', argCommentParen.length > 0]);

  const argDollarComma = checkSecrets('init.sql', "SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', $$app,other$$, 'RealSecret123')");
  results.push(['C1 负向（美元串内逗号不切分实参，%L 实参仍检出，r8 P2-5）', argDollarComma.length > 0]);

  const blockScalarCrossLine = checkSecrets('compose.yml', "command:\n  - psql\n  - -c\n  - |\n    CREATE ROLE app LOGIN PASSWORD\n    'RealSecret123';");
  results.push(['C1 负向（| 块标量跨行内容还原后口令检出，r8 P2-6）', blockScalarCrossLine.length > 0]);

  const blockScalarFolded = checkSecrets('compose.yml', "command:\n  - psql\n  - -c\n  - >\n    CREATE ROLE app LOGIN PASSWORD\n    'RealSecret123';");
  results.push(['C1 负向（> 块标量跨行内容还原后口令检出，r8 P2-6）', blockScalarFolded.length > 0]);

  const adjacentSqLines = checkSecrets('init.sql', "CREATE ROLE app LOGIN PASSWORD ''\n'RealSecret123';");
  results.push(['C1 负向（相邻字符串跨行拼接，真口令检出，r8 P2-7）', adjacentSqLines.length > 0]);

  const setMultiArgs = checkSecrets('init.sql', "\\set app_pass '' 'RealSecret123'");
  results.push(['C1 负向（\\set 多参数相邻串按拼接检查，真口令检出，r8 P2-7）', setMultiArgs.length > 0]);

  const exprWindowBound = checkSecrets('init.sql', "SELECT 'CREATE ROLE app LOGIN PASSWORD ' || quote_literal(:'app_pass')\nWHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'app')\n\\gexec");
  results.push(['C1 正向（拼接表达式窗口不吞 WHERE 条件字面量，r8 P2-8）', exprWindowBound.length === 0]);

  const gexecInline = checkSecrets('init.sql', "SELECT 'CREATE ROLE app LOGIN PASSWORD ' || quote_literal(:'app_pass') \\gexec\nSELECT 'hello';");
  results.push(['C1 正向（\\gexec 行内边界后不误报后续语句，r8 P2-9）', gexecInline.length === 0]);

  const commentFragmentSemi = checkSecrets('init.sql', "-- SELECT 'CREATE ROLE app LOGIN PASSWORD ' || quote_literal(current_user); SELECT 'hello';");
  results.push(['C1 正向（注释片段内分号结束候选，不误报后续语句，r8 P2-9）', commentFragmentSemi.length === 0]);

  const doDollarFragment = checkSecrets('init.sql', "DO $$ BEGIN EXECUTE 'CREATE ROLE app LOGIN PASSWORD ' || quote_literal(current_user); END $$;\nSELECT 'hello';");
  results.push(['C1 正向（DO 美元体内分号结束候选，不误报后续语句，r8 P2-9）', doDollarFragment.length === 0]);

  // ---- codex r9 回归（format 参数游标 / YAML 块头词法 / 显式缩进 / 折叠 / 序列节点层级 / 外层括号 / 片段注释 / 反引号 / 变量插值） ----
  const formatExplicitPos = checkSecrets('init.sql', "SELECT format('CREATE ROLE %1$I LOGIN PASSWORD %L', :'app_user', 'RealSecret123')");
  results.push(['C1 负向（%1$I 后隐式 %L 用第二实参检出，r9 P2-1）', formatExplicitPos.length > 0]);

  const formatExplicitPosSafe = checkSecrets('init.sql', "SELECT format('CREATE ROLE %1$I LOGIN PASSWORD %L', 'app', :'app_pass')");
  results.push(['C1 正向（显式位置混用时 %L 实参为变量不误报，r9 P2-1）', formatExplicitPosSafe.length === 0]);

  const yamlCommentGt = checkSecrets('compose.yml', "command: [psql, -c, \"CREATE ROLE app LOGIN PASSWORD 'RealSecret123';\"] # >");
  results.push(['C1 负向（注释中的 > 不吞行，口令仍检出，r9 P2-2）', yamlCommentGt.length > 0]);

  const yamlChompOrder = checkSecrets('compose.yml', "command: |2-\n  CREATE ROLE app LOGIN PASSWORD\n  'RealSecret123';");
  results.push(['C1 负向（|2- 指示顺序识别后检出，r9 P2-2）', yamlChompOrder.length > 0]);

  const yamlExplicitIndent = checkSecrets('compose.yml', "command: |2\n    CREATE ROLE app LOGIN\n  PASSWORD 'RealSecret123';");
  results.push(['C1 负向（显式缩进按节点缩进+数字剥离后检出，r9 P2-3）', yamlExplicitIndent.length > 0]);

  const yamlFoldComment = checkSecrets('compose.yml', "command: >\n  SELECT 'CREATE ROLE app LOGIN PASSWORD ' || quote_literal(current_user) -- note\n  || quote_literal('hello')");
  results.push(['C1 正向（> 折叠与注释吞并后不误报 hello，r9 P2-4）', yamlFoldComment.length === 0]);

  const yamlFoldSet = checkSecrets('compose.yml', "command: >\n  \\set app_pass\n  'RealSecret123'");
  results.push(['C1 负向（> 折叠后 \\set 参数合并且检出，r9 P2-4）', yamlFoldSet.length > 0]);

  const yamlSeqCompact = checkSecrets('compose.yml', "- command: |\n    SELECT 1;\n  entrypoint: \"CREATE ROLE app LOGIN PASSWORD 'RealSecret123';\"");
  results.push(['C1 负向（序列标记节点层级终止块收集，同级键回归检索出，r9 P2-5）', yamlSeqCompact.length > 0]);

  const exprParenWrap = checkSecrets('init.sql', "SELECT ('CREATE ROLE app LOGIN PASSWORD ') || quote_literal('RealSecret123');");
  results.push(['C1 负向（外层括号包装的拼接表达式仍检出，r9 P2-6）', exprParenWrap.length > 0]);

  const exprCastWrap = checkSecrets('init.sql', "SELECT CAST('CREATE ROLE app LOGIN PASSWORD ' AS text) || quote_literal('RealSecret123');");
  results.push(['C1 负向（CAST 包装的拼接表达式仍检出，r9 P2-6）', exprCastWrap.length > 0]);

  const fragBlockComment = checkSecrets('init.sql', "DO $$ BEGIN EXECUTE 'CREATE ROLE app LOGIN PASSWORD ' || /* note */ quote_literal('RealSecret123'); END $$;");
  results.push(['C1 负向（美元体内片段注释消费后检出，r9 P2-7）', fragBlockComment.length > 0]);

  const fragNestedComment = checkSecrets('init.sql', "DO $$ BEGIN EXECUTE 'CREATE ROLE app LOGIN PASSWORD ' || /* outer /* inner */ */ quote_literal('RealSecret123'); END $$;");
  results.push(['C1 负向（美元体内嵌套片段注释消费后检出，r9 P2-7）', fragNestedComment.length > 0]);

  const fragLineComment = checkSecrets('init.sql', "-- EXECUTE 'CREATE ROLE app LOGIN PASSWORD ' || /* note */ quote_literal('RealSecret123');");
  results.push(['C1 负向（行注释片段内注释消费后检出，r9 P2-7）', fragLineComment.length > 0]);

  const backtickSet = checkSecrets('init.sql', "\\set app_pass `printf \\%s 'RealSecret123'`");
  results.push(['C1 负向（反引号内反斜杠不截断，硬编码检出，r9 P2-8）', backtickSet.length > 0]);

  const backtickSafe = checkSecrets('init.sql', "\\set app_pass `echo \"$ZSZJ_DATASOURCE_PASSWORD\"`");
  results.push(['C1 正向（反引号内环境变量引用不误报，r9 P2-8）', backtickSafe.length === 0]);

  const setVarThenLiteral = checkSecrets('init.sql', "\\set app_pass '' :'safe' 'RealSecret123'");
  results.push(['C1 负向（空串+变量插值后硬编码后缀仍检出，r9 P2-9）', setVarThenLiteral.length > 0]);

  const setVarOnly = checkSecrets('init.sql', "\\set app_pass '' :'safe'");
  results.push(['C1 正向（空串+变量插值无硬编码不误报，r9 P2-9）', setVarOnly.length === 0]);

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
