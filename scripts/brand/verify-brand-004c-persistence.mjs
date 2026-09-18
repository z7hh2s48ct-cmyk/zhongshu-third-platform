/**
 * ZS-BRAND-004.C 任务与消息持久化引用静态门禁（本地与 CI 同一入口）。
 *
 * 用法：node scripts/brand/verify-brand-004c-persistence.mjs [--quiet]
 *
 * 职责（对齐开发计划 / docs/05 ZS-BRAND-004 卡 .C 分批）：
 *   1. 任务域：infra_job 种子 handler_name（git ls-files 全量迁移 SQL 提取）与
 *      Java 侧 `implements JobHandler` Bean 名一一对应；无旧名残留。
 *   2. Quartz 持久化：QRTZ_* 全仓 0 行种子（V1/V2 仅 DDL）；调度入口
 *      JobBuilder.newJob(...) 所指 Job 类必须解析到 cn.zszj 包根；SQL 面
 *      包形旧 FQCN（cn.iocoder./cn.yudao./yudao.module|framework|server.）0 残留。
 *   3. Outbox/Inbox：状态机硬约束（PENDING/DISPATCHED/DEAD/SKIPPED）、领取栅栏
 *      列（claim_token 等）、恢复台账与审计常量、受控拒绝错误码锚点齐全；相关
 *      SQL 字面量与 AuditEventTypes 常量无旧名。
 *   4. MSG/序列化：Redis Stream key 为类 SimpleName 动态派生、消费组 = spring.application.name
 *      = zszj-server、yaml key-prefix 无旧名；子类对 getChannel()/getStreamKey() 的路由覆写返回值
 *      必须过旧名判定，方法体走花括号平衡扫描（嵌套块/字符串内花括号不截断），解析失败保守拒绝
 *      （codex r0 P2-4 / r1 P2-B）；Jackson 多态类名落库面（@JsonTypeInfo(Id.CLASS)）按
 *      「package 声明 + 类型声明 class|record|enum|interface（嵌套类 $ 二进制名）」推导实际持久化
 *      FQCN，必须全新包根，过滤命中却推导不出 → fail-loud（codex r0 P2-3 / r1 P2-A，目录推导与
 *      静默缩清单均假绿），且 JsonUtils 无全局 default typing。
 *   5. 旧名判定循 .B 模式清单（verify-brand-004b-storage.mjs 的
 *      OLD_NAME_PATTERNS），另加包形（cn.iocoder./cn.yudao.）增强；只对提取出的
 *      标识/字面量判定，不重复 G3 全仓文本扫描、不误伤已登记来源署名。
 *   6. 注入自检（防扫描器空洞）：合成旧名样例喂同一套提取+判定路径，任一未命中
 *      即门禁失败。
 *
 * 退出码：0 通过；1 发现残留/合同缺失/注入自检失败；2 脚本自身错误。
 * 报告以 JSON 输出 stdout；--quiet 时省略明细清单。
 */
import { readFileSync, existsSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { OLD_NAME_PATTERNS, judgeKey, extractYamlKeyPrefixes } from './verify-brand-004b-storage.mjs';

const root = fileURLToPath(new URL('../../', import.meta.url));
const quiet = process.argv.includes('--quiet');

// 包形旧名（FQCN/包路径形态）：值级判定补充 .B 未覆盖的「类名落库」口径。
// 故意不做裸词扫描——sql 基线头部的「来源：ruoyi-vue-pro.sql」为已登记来源署名。
export const PACKAGE_SHAPED_OLD_NAME_PATTERNS = [
  { name: 'cn.iocoder-fqcn', re: /cn\.iocoder\.[A-Za-z_][A-Za-z0-9_.]*/g },
  { name: 'cn.yudao-fqcn', re: /\bcn\.yudao\.[A-Za-z_][A-Za-z0-9_.]*/g },
  { name: 'yudao-package', re: /\byudao\.(?:module|framework|server)\.[A-Za-z_][A-Za-z0-9_.]*/g },
];

/** 值级旧名判定 = .B 键级模式 + 包形模式。 */
export function judgeValue(source, field, value) {
  return [...judgeKey(source, field, value), ...judgeRaw(source, field, value, PACKAGE_SHAPED_OLD_NAME_PATTERNS)];
}

function judgeRaw(source, field, value, patterns) {
  const violations = [];
  for (const { name, re } of patterns) {
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
    .filter(Boolean)
    .map((f) => f.replaceAll('\\', '/'));
  return filter ? files.filter(filter) : files;
}

function readRepoFile(rel) {
  const abs = `${root}${rel.replaceAll('/', process.platform === 'win32' ? '\\' : '/')}`;
  if (!existsSync(abs)) return null;
  return readFileSync(abs, 'utf8');
}

const inCoreSql = (f) => f.startsWith('services/zhongshu-core/sql/') || f.startsWith('services/zhongshu-core/zszj-server/src/main/resources/db/migration/');
// 结构化元组提取只对 PG 方言面（产品实际运行方言；Flyway 迁移链 + PG 基线/演示数据）——
// dm/oracle/sqlserver 等供应商方言参考文件使用 N'' 字面量与 GO 批分隔符，不做元组级解析，
// 但其包形旧名/状态字面量仍在全量 SQL 扫描范围内（下方 extractPackageShapedOldNames 用 inCoreSql）。
const inPgSql = (f) => f.startsWith('services/zhongshu-core/sql/postgresql/') || f.startsWith('services/zhongshu-core/zszj-server/src/main/resources/db/migration/');
// 权威新装迁移链（Flyway 实际执行）：种子 ↔ Bean 合同只锚定这一面；
// sql/postgresql/ruoyi-vue-pro.sql 为供应商上游基线（不进 Flyway），其任务种子
// （pay/iot/trade 等）指向未启用模块的 Handler，属合法无 Bean，不参与合同。
const inFlywayMigration = (f) => f.startsWith('services/zhongshu-core/zszj-server/src/main/resources/db/migration/');
const inCoreJava = (f) => f.startsWith('services/zhongshu-core/') && /\.java$/.test(f);

// ---------------------------------------------------------------------------
// ① 任务域：infra_job 种子 handler_name ↔ Java JobHandler Bean 名
// ---------------------------------------------------------------------------
const INFRA_JOB_INSERT_RE = /INSERT\s+INTO\s+infra_job\s*\(([^)]*)\)\s*VALUES\s*/gi;

/** 单行 SQL VALUES 元组切分：按不在单引号内的逗号切分。 */
export function splitSqlTuple(row) {
  const parts = [];
  let cur = '', inStr = false;
  for (let i = 0; i < row.length; i++) {
    const ch = row[i];
    if (ch === "'") {
      if (inStr && row[i + 1] === "'") { cur += "''"; i++; continue; } // 转义引号
      inStr = !inStr;
    }
    if (ch === ',' && !inStr) { parts.push(cur.trim()); cur = ''; continue; }
    cur += ch;
  }
  parts.push(cur.trim());
  return parts;
}

/** 语句切片：从 pos 起到第一条不在单引号内的分号（含）。 */
function statementSlice(text, pos) {
  let inStr = false;
  for (let i = pos; i < text.length; i++) {
    const ch = text[i];
    if (ch === "'") inStr = !inStr;
    else if (ch === ';' && !inStr) return text.slice(pos, i + 1);
  }
  return text.slice(pos);
}

/** 语句内顶层（引号外）圆括号组提取——元组内可含 '…(…)…' 字面量，不得被误切。 */
export function scanTopLevelTupleRows(statement) {
  const rows = [];
  let depth = 0, cur = '', inStr = false;
  for (let i = 0; i < statement.length; i++) {
    const ch = statement[i];
    if (ch === "'") {
      if (inStr && statement[i + 1] === "'") { cur += "''"; i++; continue; }
      inStr = !inStr;
    }
    if (!inStr) {
      if (ch === '(') { depth++; if (depth === 1) { cur = ''; continue; } }
      else if (ch === ')') { depth--; if (depth === 0) { rows.push(cur); cur = ''; continue; } }
      else if (ch === ',' && depth === 0) continue;
    }
    if (depth >= 1) cur += ch;
  }
  return rows;
}

/** 提取 INSERT INTO infra_job 的 handler_name 值（按列名定位索引，不数死位置；
 *  语句边界以引号外分号为准，防止元组含 '…(…)…' 字面量时越过语句尾误收其它表的行）。 */
export function extractInfraJobSeedHandlers(text, source) {
  const handlers = [];
  let m;
  INFRA_JOB_INSERT_RE.lastIndex = 0;
  while ((m = INFRA_JOB_INSERT_RE.exec(text))) {
    const cols = m[1].split(',').map((c) => c.trim().toLowerCase());
    const idx = cols.indexOf('handler_name');
    if (idx < 0) continue;
    const statement = statementSlice(text, m.index + m[0].length);
    for (const row of scanTopLevelTupleRows(statement)) {
      const vals = splitSqlTuple(row);
      if (vals.length > idx) {
        const v = vals[idx].replace(/^'|'$/g, '');
        handlers.push({ source, field: 'handler_name', value: v });
      }
    }
  }
  return handlers;
}

const JOB_HANDLER_IMPL_RE = /class\s+([A-Za-z0-9_]+)\s+(?:\w+\s+)*implements\s+JobHandler\s*\{/;

/** Java 侧 JobHandler 实现 → Spring Bean 名（@Component 缺省 = 类名首字母小写）。 */
export function extractJobHandlerBeanNames(text) {
  const names = [];
  const re = new RegExp(JOB_HANDLER_IMPL_RE.source, 'g');
  let m;
  while ((m = re.exec(text))) {
    const cls = m[1];
    names.push(cls.charAt(0).toLowerCase() + cls.slice(1));
  }
  return names;
}

// ---------------------------------------------------------------------------
// ② Quartz 持久化：QRTZ 种子行 + SQL 包形旧 FQCN + 调度入口 Job 类
// ---------------------------------------------------------------------------
export function extractQrtzSeedInserts(text, source) {
  const rows = [];
  const re = /INSERT\s+(?:INTO\s+)?(QRTZ_[A-Za-z_]+)/gi;
  let m;
  while ((m = re.exec(text))) {
    rows.push({ source, field: 'qrtz-seed-table', value: m[1] });
  }
  return rows;
}

export function extractPackageShapedOldNames(text, source) {
  return judgeRaw(source, 'package-shape', text, PACKAGE_SHAPED_OLD_NAME_PATTERNS);
}

const NEW_JOB_BUILDER_RE = /JobBuilder\.newJob\(\s*([A-Za-z0-9_]+)\.class/g;

/** SchedulerManager 调度入口所指 Job 类（Quartz 持久化 JOB_CLASS_NAME 的源头）。 */
export function extractQuartzJobBuilderClasses(text, source) {
  const refs = [];
  const re = new RegExp(NEW_JOB_BUILDER_RE.source, 'g');
  let m;
  while ((m = re.exec(text))) {
    refs.push({ source, field: 'quartz-job-class', value: m[1] });
  }
  return refs;
}

// ---------------------------------------------------------------------------
// ③ Outbox/Inbox：状态机字面量 + 审计常量
// ---------------------------------------------------------------------------
const JAVA_STRING_CONST_RE = /(?:public|private)?\s*static\s+final\s+String\s+([A-Z][A-Z0-9_]*)\s*=\s*"([^"]*)"\s*;/g;

/** AuditEventTypes 等常量类：String 常量视为事件类型目录。 */
export function extractStringConstants(text, source) {
  const keys = [];
  const re = new RegExp(JAVA_STRING_CONST_RE.source, 'g');
  let m;
  while ((m = re.exec(text))) {
    keys.push({ source, field: m[1], value: m[2] });
  }
  return keys;
}

/** SQL 面单引号字面量（状态值/事件类型等持久化枚举值），供旧名判定。 */
export function extractSqlStringLiterals(text, source) {
  const values = [];
  const re = /'([A-Za-z_][A-Za-z0-9_.:%-]{1,127})'/g;
  let m;
  while ((m = re.exec(text))) {
    values.push({ source, field: 'sql-literal', value: m[1] });
  }
  return values;
}

// ---------------------------------------------------------------------------
// ④ MSG/序列化：stream 子类、应用名、Jackson 多态类名落库面
// ---------------------------------------------------------------------------
const STREAM_BASE_RE = /class\s+([A-Za-z0-9_]+)\s+extends\s+(?:AbstractRedisStreamMessage|AbstractRedisChannelMessage)\b/;

/** Redis Stream/Channel 消息子类清单：子类 SimpleName 即 stream key/channel（动态派生）。 */
export function extractRedisMessageSubclasses(text, source) {
  const refs = [];
  const re = new RegExp(STREAM_BASE_RE.source, 'g');
  let m;
  while ((m = re.exec(text))) {
    refs.push({ source, field: 'redis-message-subclass', value: m[1] });
  }
  return refs;
}

/** spring.application.name（Redis Stream 消费组名，持久化在 Redis 消费组登记里）。 */
export function extractSpringAppName(text) {
  const m = text.match(/^ {2}application:\r?\n {4}name:\s*(\S+)/m);
  return m ? m[1] : null;
}

/** Jackson 多态类名落库面：@JsonTypeInfo(use = Id.CLASS) 接口清单。 */
export function extractJsonTypeInfoInterfaces(text, source) {
  return /@JsonTypeInfo\s*\(\s*use\s*=\s*JsonTypeInfo\.Id\.CLASS/.test(text) ? [source] : [];
}

/**
 * 词法模型边界（codex r3「模型外输入 fail-loud」合同）：掩码器/翻译器覆盖不了的输入结构，
 * 一律产 fatal problem——该文件**跳过全部 Java 判定**（不产出清单、不静默通过，要求人工复核）。
 * 本门禁是「证明无残留」的安全侧工具：解析不了就大声说出来，绝不静默。
 * 合格 Unicode 转义采用 JLS 3.3 翻译（r3 采纳方案①安全子集 + ②兜底）：良性目标字符（字母/
 * 空白/CJK 等）按语义翻译后掩码（真实树存量 \u2003/\u3000 类正则转义零 problem、值判定更准）；
 * 真正无法安全翻译的形状（目标为反斜杠的自指链、翻译轮数超限）fail-loud。
 */
export const JAVA_LEXICAL_MODEL_BOUNDARIES = [
  { check: 'unicode-escape-backslash-target', fatal: true, why: '合格 Unicode 转义目标为反斜杠（\\u005c，改变字符串终止判定/自指风险）：翻译器不建模，需人工复核' },
  { check: 'unicode-escape-invalid-hex', fatal: true, why: '合格反斜杠+u 序列后非 4 位 hex（JLS 3.3 编译期错误，javac 拒绝编译）：翻译器不静默修复非法输入，需人工复核' },
  { check: 'text-block-unterminated', fatal: true, why: 'Java 17 文本块未闭合，无法安全词法分析' },
  { check: 'block-comment-unterminated', fatal: true, why: '块注释未闭合（文件尾前无 */），无法安全词法分析' },
  { check: 'string-literal-unterminated', fatal: true, why: '普通字符串字面量跨行未闭合（Java 非法形状），无法安全词法分析' },
  { check: 'char-literal-unterminated', fatal: true, why: '字符字面量跨行未闭合（Java 非法形状），无法安全词法分析' },
];

/**
 * JLS 3.3 合格 Unicode 转义翻译预处理（codex r3 P2-B 引入 / r4 P2-5 依 JLS 订正为单遍）：javac 在
 * 词法前对原始输入做一次左到右翻译（合格=奇数反斜杠 + u{1,} + 4 位 hex），**翻译产物不再参与
 * Unicode 转义判定**（JLS 3.3 无「反复左剥」——\uu0041 是一次识别多个 u，非递归翻译）；合格反斜
 * 杠+u 序列后非 4 位 hex 是编译期错误（javac 拒绝编译）→ fail-loud，不静默修复非法输入；目标为
 * 反斜杠（\u005c 改变字符串终止判定）→ fatal 兜底。其余（字母/空白/CJK 等）翻译后参与掩码与值
 * 判定（注释藏 \u000a 断行出可执行代码的藏毒形态无处遁形）。
 */
export function translateEligibleUnicodeEscapes(text) {
  let translated = '';
  let i = 0;
  while (i < text.length) {
    if (text[i] !== '\\') { translated += text[i]; i++; continue; }
    let bs = 0;
    while (text[i + bs] === '\\') bs++;
    const after = i + bs;
    let u = 0;
    while (text[after + u] === 'u') u++;
    const hex = text.slice(after + u, after + u + 4);
    if (bs % 2 === 1 && u >= 1) {
      if (!/^[0-9a-fA-F]{4}$/.test(hex)) {
        return { translated: null, problem: { check: 'unicode-escape-invalid-hex', why: JAVA_LEXICAL_MODEL_BOUNDARIES[1].why } };
      }
      const code = parseInt(hex, 16);
      if (code === 0x5c) {
        return { translated: null, problem: { check: 'unicode-escape-backslash-target', why: JAVA_LEXICAL_MODEL_BOUNDARIES[0].why } };
      }
      translated += '\\'.repeat(bs - 1) + String.fromCharCode(code); // 偶数前缀反斜杠原样保留，最后一个反斜杠参与转义
      i = after + u + 4;
      continue;
    }
    translated += text.slice(i, after + u);
    i = after + u;
  }
  return { translated, problem: null }; // 单遍完成：无「翻译产物再扫描」的第二轮
}

/**
 * 单一复用的注释掩码预处理（codex r2 根治 / r3 模型边界合同）：先做合格 Unicode 转义翻译
 * （translateEligibleUnicodeEscapes），再按 Java 词法扫描把 // 行注释与 block 块注释区间替换为
 * 等长空白（保留换行——偏移量/行号不变）；掩码器感知字符串/字符字面量与转义（字面量内的注释
 * 记号不参与词法）。词法模型（r3 显式化，见 JAVA_LEXICAL_MODEL_BOUNDARIES）：行终止符 = \r、
 * \n、\r\n（JLS 3.4）；文本块开界符 = """ + 空白（space/tab/FF）+ 行终止符（JLS 3.10.6），块内
 * 转义（含 \s、\""）不终止扫描；模型外输入 → fatal problem 且 masked=null——调用方必须跳过该
 * 文件的全部判定。所有 Java 文本提取（类型声明匹配、路由字面量提取等）一律只对掩码后文本工作。
 * 掩码幂等性注意：文本块内容原样保留，掩码只可对原文应用一次。
 */
export function maskComments(text) {
  const problems = [];
  const fatal = (check, why) => problems.push({ check, fatal: true, why });
  const tr = translateEligibleUnicodeEscapes(text);
  if (tr.problem) {
    fatal(tr.problem.check, tr.problem.why);
    return { masked: null, problems }; // masked=null 明示「本文件未产出可用掩码，判定须跳过」
  }
  // r3 坐标系合同（自检 masker-unicode-escape-in-comment-translated 实证）：翻译是变长的
  // （\u000a 6 字符 → \n 1 字符），定界扫描与掩码抹除必须同在翻译后文本上进行——在原文上
  // 扫描、译文上抹除会坐标错位（掩码越界/截断，藏毒假绿或误伤真代码）。下游消费者只读返回的
  // masked 串（自身坐标自洽），不受与原文件偏移差异影响。
  const src = tr.translated;
  const chars = src.split('');
  const blank = (from, to) => {
    for (let k = from; k < to && k < chars.length; k++) {
      if (chars[k] !== '\n' && chars[k] !== '\r') chars[k] = ' ';
    }
  };
  // [P2-C] 文本块开界符空白 = space/tab/FF（JLS 3.10.6）；否则按普通字符串词法（如 "" 空串序列）
  const isTextBlockOpen = (idx) => {
    if (!src.startsWith('"""', idx)) return false;
    let j = idx + 3;
    while (j < src.length && (src[j] === ' ' || src[j] === '\t' || src[j] === '\f')) j++;
    return src[j] === '\n' || src[j] === '\r';
  };
  let i = 0;
  while (i < src.length) {
    const ch = src[i];
    if (ch === '"' && isTextBlockOpen(i)) {
      i += 3; // 开界符原样保留
      let closed = false;
      while (i < src.length) {
        if (src[i] === '\\') { i += 2; continue; } // 块内转义（\""、\s 等）不参与终止判定
        if (src.startsWith('"""', i)) { i += 3; closed = true; break; }
        i++;
      }
      if (!closed) fatal('text-block-unterminated', JAVA_LEXICAL_MODEL_BOUNDARIES[2].why);
      continue;
    }
    if (ch === '/' && src[i + 1] === '/') {
      // [P2-A] 行注释在 \r 或 \n 终止（JLS 3.4 三种行终止符；\r\n 两个都消费）
      let j = i;
      while (j < src.length && src[j] !== '\n' && src[j] !== '\r') j++;
      blank(i, j);
      i = j;
      if (src[j] === '\r' && src[j + 1] === '\n') i++;
      continue;
    }
    if (ch === '/' && src[i + 1] === '*') {
      let j = i + 2;
      while (j < src.length && !src.startsWith('*/', j)) j++;
      if (j >= src.length) {
        fatal('block-comment-unterminated', JAVA_LEXICAL_MODEL_BOUNDARIES[3].why);
        blank(i, src.length);
        break;
      }
      const end = j + 2;
      blank(i, end);
      i = end;
      continue;
    }
    if (ch === '"') {
      let j = i + 1;
      let terminated = false;
      while (j < src.length) {
        if (src[j] === '\\') {
          // [r4 P2-4] 反斜杠不得吞掉行终止符：普通字符串/字符字面量中 \<CR|LF> 是 Java 非法形状，
          // 先查终止符再跳过转义对——否则 `prefix\<CR>suffix"` 跨行字面量逃过跨行拒止
          if (src[j + 1] === '\n' || src[j + 1] === '\r') break;
          j += 2; continue;
        }
        if (src[j] === '"') { terminated = true; break; }
        if (src[j] === '\n' || src[j] === '\r') break; // 普通字符串不跨行（Java 非法形状）
        j++;
      }
      if (!terminated) fatal('string-literal-unterminated', JAVA_LEXICAL_MODEL_BOUNDARIES[4].why);
      i = Math.min(j + 1, src.length); // 行尾即状态复位，继续收集其余问题供人工一次复核
      continue;
    }
    if (ch === "'") {
      let j = i + 1;
      let terminated = false;
      while (j < src.length) {
        if (src[j] === '\\') {
          if (src[j + 1] === '\n' || src[j + 1] === '\r') break; // [r4 P2-4] 同上，不吞行终止符
          j += 2; continue; // 八进制/多字符转义如 '\12' 原样跳过
        }
        if (src[j] === "'") { terminated = true; break; }
        if (src[j] === '\n' || src[j] === '\r') break;
        j++;
      }
      if (!terminated) fatal('char-literal-unterminated', JAVA_LEXICAL_MODEL_BOUNDARIES[5].why);
      i = Math.min(j + 1, src.length);
      continue;
    }
    i++;
  }
  // [r4 P3-8] fatal 返回合同统一：任一 fatal → masked=null（该文件不可判定），不再有
  // 「带 fatal 却返回非空掩码」的混合态——调用方以 masked===null 判定跳过全部 Java 判定
  if (problems.some((p) => p.fatal)) return { masked: null, problems };
  return { masked: chars.join(''), problems };
}

/**
 * 从 Java 源码文本推导「实际持久化的 FQCN」（codex r0 P2-3 / r1 P2-A / r2 P2-A）：以 package
 * 声明 + 类型声明为准，不信任文件目录；类型声明覆盖 class|record|enum|interface；嵌套类按
 * 二进制名 `Outer$Inner`（@class 落库即二进制名）。**输入必须是 maskComments 掩码后文本**——
 * 紧邻声明的 Javadoc 中的「record of」等词不得启动类型匹配（r2 P2-A：注释未排除时顶层类型
 * 被劫持、FQCN 推导错误且旧名静默消失）。fail-loud 契约：声明缺失、或「过滤命中 implements
 * 却提取不出任何 FQCN」一律返回 problem，绝不静默缩清单。
 */
export function deriveImplFqcns(text, source, iface) {
  const pkgMatch = text.match(/^[ \t]*package\s+([A-Za-z_][\w.]*)\s*;/m);
  const topLevelMatch = text.match(/\b(?:class|record|enum|interface)\s+([A-Za-z0-9_]+)/);
  if (!pkgMatch) return { results: [], problem: { source, check: 'java-package-decl-missing', why: `多态实现文件缺 package 声明，无法推导持久化 FQCN：${source}` } };
  if (!topLevelMatch) return { results: [], problem: { source, check: 'java-type-decl-missing', why: `多态实现文件缺类型声明，无法推导持久化 FQCN：${source}` } };
  const results = [];
  const implRe = new RegExp(`\\b(?:class|record|enum|interface)\\s+([A-Za-z0-9_]+)[^{;]*\\bimplements\\s+[\\w.,<> \\t]*\\b${iface}\\b`, 'g');
  let m;
  while ((m = implRe.exec(text))) {
    const cls = m[1];
    const fqcn = cls === topLevelMatch[1]
      ? `${pkgMatch[1]}.${cls}`
      : `${pkgMatch[1]}.${topLevelMatch[1]}$${cls}`;
    results.push({ source, field: 'json-typinfo-impl-fqcn', value: fqcn });
  }
  if (!results.length) {
    return {
      results,
      problem: {
        source, check: 'impl-fqcn-derive-empty',
        why: `发现 ${iface} 实现但声明形状不在推导覆盖范围（class|record|enum|interface + implements），拒绝静默缩清单：${source}`,
      },
    };
  }
  return { results, problem: null };
}

/**
 * 花括号平衡的方法体提取（codex r1 P2-B）：从 startIdx 指向的 '{' 起扫描至配平的 '}'；
 * 字符串/字符字面量与行/块注释内的花括号不参与配平（至少字符串为红线）。
 * 不平衡（截断/损坏源）→ ok=false，调用方 fail-loud 保守拒绝，不静默放过。
 */
export function extractBalancedBraceBody(text, startIdx) {
  let depth = 0, inStr = null, inLineComment = false, inBlockComment = false;
  for (let i = startIdx; i < text.length; i++) {
    const ch = text[i];
    const next = text[i + 1];
    if (inLineComment) { if (ch === '\n' || ch === '\r') inLineComment = false; continue; }
    if (inBlockComment) { if (ch === '*' && next === '/') { inBlockComment = false; i++; } continue; }
    if (inStr) {
      if (ch === '\\') { i++; continue; }
      if (ch === inStr) inStr = null;
      continue;
    }
    // Java 17 文本块（掩码后原文保留，r2 P2-C）：整体跳过——块内 /* 等记号不参与深度/字符串状态。
    // [r4 P2-6] 外层 for 每轮自增 1：开/闭界符各只前移 2，由外层自增补足第 3 字符——
    // 若此处 +=3 会越过闭界符后第一个字符（如数组字面量的 '}'），方法体边界失准产生假违规
    if (ch === '"' && text.startsWith('"""', i)) {
      i += 2;
      while (i < text.length) {
        if (text[i] === '\\') { i += 2; continue; } // 块内转义对（\""、\s 等）整跳
        if (text.startsWith('"""', i)) { i += 2; break; }
        i++;
      }
      continue;
    }
    if (ch === '/' && next === '/') { inLineComment = true; i++; continue; }
    if (ch === '/' && next === '*') { inBlockComment = true; i++; continue; }
    if (ch === '"' || ch === "'") { inStr = ch; continue; }
    if (ch === '{') depth++;
    else if (ch === '}') { depth--; if (depth === 0) return { body: text.slice(startIdx + 1, i), ok: true }; }
  }
  return { body: text.slice(startIdx + 1), ok: false };
}

/**
 * 字符串/文本块转义值解码（codex r4 P2-3）：判定必须针对**真实字符串值**而非源码拼写——
 * \171 八进制转出 'y'、文本块续行符 \<行终止> 把 `yu\<LF>dao` 拼回 `yudao`，按拼写判定即漏报。
 * 解码 JLS 3.10.6/3.10.7 全集：\b \t \n \f \r \" \' \\ \s（r5 订正：合法于文本块/普通字符串/char）
 * \0~\377 八进制（1~3 位、三位时首位 ≤3，超界按两位转义 + 字面量处理——\400 = \40 + '0'）；\u 序列不应到达此处（掩码预处理已全文件翻译；残留形状=非法转义）→ fail-loud；
 * 未建模/非法转义 → problem（javac 同样拒绝编译），返回 null 由调用方丢弃该字面量（问题已计入）。
 */
export function decodeJavaEscapeValues(raw, isTextBlock, source, field, problems) {
  let out = '';
  let i = 0;
  const fail = (check, why) => { problems.push({ source, field, check, why }); };
  while (i < raw.length) {
    const ch = raw[i];
    if (ch !== '\\') { out += ch; i++; continue; }
    const n = raw[i + 1];
    if (n === undefined) { fail('escape-trailing-backslash', `字面量以孤立反斜杠结尾（Java 非法形状）：${source}`); return null; }
    if (n === 'b') { out += '\b'; i += 2; continue; }
    if (n === 't') { out += '\t'; i += 2; continue; }
    if (n === 'n') { out += '\n'; i += 2; continue; }
    if (n === 'f') { out += '\f'; i += 2; continue; }
    if (n === 'r') { out += '\r'; i += 2; continue; }
    if (n === '"') { out += '"'; i += 2; continue; }
    if (n === "'") { out += "'"; i += 2; continue; }
    if (n === '\\') { out += '\\'; i += 2; continue; }
    if (n === 's') { out += ' '; i += 2; continue; } // [r5 P2-2] \s 合法于文本块/普通字符串/char（JLS 3.10.7 修正：r4 误限仅文本块）
    if (n >= '0' && n <= '7') {
      const oct0 = (/^[0-7]{1,3}/.exec(raw.slice(i + 1)))[0];
      const oct = oct0.length === 3 && oct0[0] > '3' ? oct0.slice(0, 2) : oct0; // 八进制上限 \377：三位时首位须 0-3，否则只取两位
      out += String.fromCharCode(parseInt(oct, 8));
      i += 1 + oct.length;
      continue;
    }
    if (n === 'u') { fail('escape-illegal-unicode-residue', `字面量内残留 \\u 序列（Unicode 转义应在掩码预处理翻译；字符串级 \\u 非 JLS 转义、javac 报非法转义）：${source}`); return null; }
    if (isTextBlock && (n === '\n' || n === '\r')) {
      i += 2;
      if (n === '\r' && raw[i] === '\n') i++; // \<CRLF> 续行整跳
      continue; // 文本块续行符：删除行终止符拼接前后文本
    }
    fail('escape-illegal', `未建模转义 \\${n === '\n' || n === '\r' ? '<行终止符>' : n}（javac 非法转义或本门禁不建模）：${source}`);
    return null;
  }
  return out;
}

/**
 * 消息子类路由覆写提取（codex r0 P2-4 / r1 P2-B / r2 P2-B / r4 P2-1·P2-2·P2-3）：getChannel()/
 * getStreamKey() 的非默认实现返回值才是真实路由/持久化键名。**输入必须是 maskComments 掩码后
 * 文本**。r4 策略：①先用转义感知扫描器提取文本块并从 body 副本中摘除（置空白保偏移），再对
 * 余文跑 litRe——否则闭界符余引号会与后续字符串开引号错配（`""";` 后接 `return "x"` 被拼成
 * `"; return "` 假字面量，真值漏检）；文本块开界符 = """ + 空白(space/tab/FF) + 行终止
 * （CR/LF/CRLF 全形态，JLS 3.10.6），内容转义感知（\""" 不终止）；②提取的字符串/文本块一律
 * 先经 decodeJavaEscapeValues 解码为真实值再判定；③无覆写=类 SimpleName 动态派生（默认合同），
 * 无需判定。
 */
export function extractMessageRouteOverrides(text, source) {
  const literals = [];
  const problems = [];
  const re = /get(?:Channel|StreamKey)\s*\(\s*\)\s*\{/g;
  let m;
  while ((m = re.exec(text))) {
    const { body, ok } = extractBalancedBraceBody(text, m.index + m[0].length - 1);
    if (!ok) {
      problems.push({ source, check: 'route-override-body-unbalanced', why: `路由覆写方法体花括号不平衡，保守拒绝不静默放过：${source}` });
      continue;
    }
    // ① 文本块优先提取并摘除（r4 P2-1/P2-2）
    const remainder = body.split('');
    let si = 0;
    while (si < body.length) {
      if (body[si] !== '"' || !body.startsWith('"""', si)) { si++; continue; }
      let j = si + 3;
      while (j < body.length && (body[j] === ' ' || body[j] === '\t' || body[j] === '\f')) j++;
      if (body[j] !== '\n' && body[j] !== '\r') { si++; continue; } // 非文本块开界符（如 "" 空串），交回 litRe
      let k = j;
      if (body[k] === '\r') { k++; if (body[k] === '\n') k++; } else { k++; }
      let closedAt = -1;
      let p = k;
      while (p < body.length) {
        if (body[p] === '\\') { p += 2; continue; }
        if (body.startsWith('"""', p)) { closedAt = p; break; }
        p++;
      }
      if (closedAt < 0) {
        problems.push({ source, check: 'text-block-unterminated-in-body', why: `路由覆写方法体内文本块未闭合，保守拒绝：${source}` });
        break;
      }
      const decodedRaw = body.slice(k, closedAt);
      // JLS 3.10.6 / String.stripIndent（r5 P2-1/P2-3 对齐 javac 词法语义）：空白判定用 Java
      // Character.isWhitespace 集合——NBSP（U+00A0/U+2007/U+202F）非 Java 空白，JS trim() 会把
      // NBSP 行误判空行、制造假红（codex r5 实测）；stripIndent 合同 = 非「空行不计（末行始终
      // 计入）」取最小前导空白，剥离 = 每行去 minIndent 前导空白 + 裁行尾空白 + 空行归空，且
      // 剥离先于转义解释（续行拼接才与 javac 值一致）
      const tLines = decodedRaw.split(/\r\n|\n|\r/);
      const isJavaWs = (ch) => /[\t\n\u000B\f\r \u001C-\u001F\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]/.test(ch);
      const isBlankLine = (s) => [...s].every((ch) => isJavaWs(ch));
      const leadingWsLen = (s) => { let n = 0; while (n < s.length && isJavaWs(s[n])) n++; return n; };
      const stripTrailingWs = (s) => { let e = s.length; while (e > 0 && isJavaWs(s[e - 1])) e--; return s.slice(0, e); };
      let minIndent = Infinity;
      tLines.forEach((ln, idx) => {
        if (isBlankLine(ln) && idx !== tLines.length - 1) return; // 空行不计（末行始终计入）
        minIndent = Math.min(minIndent, leadingWsLen(ln));
      });
      if (!Number.isFinite(minIndent)) minIndent = 0;
      const strippedRaw = tLines
        .map((ln) => {
          if (isBlankLine(ln)) return ''; // stripIndent：空行归空
          const lead = leadingWsLen(ln);
          return stripTrailingWs(lead >= minIndent ? ln.slice(minIndent) : ln);
        })
        .join('\n'); // 行终止归一为 LF（javac 值语义；续行解码器按 CR/LF/CRLF 均兼容）
      const decoded = decodeJavaEscapeValues(strippedRaw, true, source, 'message-route-override-textblock', problems);
      if (decoded !== null) literals.push({ source, field: 'message-route-override-textblock', value: decoded });
      for (let z = si; z < Math.min(closedAt + 3, body.length); z++) remainder[z] = ' ';
      si = closedAt + 3;
    }
    // ② 余文普通字符串字面量（转义感知）→ 解码为真实值（r4 P2-3）
    const litRe = /(['"])((?:\\.|(?!\1).)*)\1/g;
    const remainderText = remainder.join('');
    let l;
    while ((l = litRe.exec(remainderText))) {
      const decoded = decodeJavaEscapeValues(l[2], false, source, 'message-route-override', problems);
      if (decoded !== null) literals.push({ source, field: 'message-route-override', value: decoded });
    }
  }
  return { literals, problems };
}

export function hasDefaultTyping(text) {
  return /enableDefaultTyping|activateDefaultTyping|setDefaultTyping/.test(text);
}

// ---------------------------------------------------------------------------
// 命名/机制合同（恢复与受控迁移的静态锚点）
// ---------------------------------------------------------------------------
export const REQUIRED_ANCHORS = [
  { file: 'services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/framework/outbox/OutboxDispatcherService.java', needle: 'claim_token', why: 'JOB-002 每次领取唯一凭证栅栏（不重复副作用合同）' },
  { file: 'services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/framework/outbox/OutboxDispatcherService.java', needle: 'NO_SINK_SUPPORTS_EVENT_TYPE', why: '无 Sink 退避 DEAD 可见不丢弃（无静默丢失合同）' },
  { file: 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260915.021__infra_outbox_recovery.sql', needle: 'SKIPPED', why: '人工恢复 SKIPPED 终态（重复消费止付合同）' },
  { file: 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260915.021__infra_outbox_recovery.sql', needle: 'manual_retry_seq', why: '人工重试上限护栏（无限重放合同）' },
  { file: 'services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/enums/ErrorCodeConstants.java', needle: 'JOB_HANDLER_NOT_WHITELISTED', why: 'JOB-001 未注册 Handler 受控拒绝（不放开越权合同）' },
  { file: 'services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/enums/ErrorCodeConstants.java', needle: 'OUTBOX_RECOVERY_NOT_DEAD', why: 'JOB-004 非 DEAD 恢复受控拒绝（重复恢复合同）' },
  { file: 'services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-mq/src/main/java/cn/zszj/framework/mq/redis/core/stream/AbstractRedisStreamMessage.java', needle: 'getClass().getSimpleName()', why: 'Stream key 类名动态派生（无硬编码 topic 字面量合同）' },
];

export function checkNamingContract({ seedHandlers, beanNames, quartzJobClasses, outboxConstants, appYaml, jsonUtilsText }) {
  const problems = [];
  // 任务域合同：种子 handler_name 必须有活的 JobHandler Bean（旧类找不到的静态防线；
  // 反向「Bean 必须有种子」不成立——未启用模块的 JobHandler 合法无种子）
  const beanSet = new Set(beanNames);
  for (const h of seedHandlers) {
    if (!beanSet.has(h.value)) problems.push({ check: 'job-handler-bean', value: h.value, why: `infra_job 种子 handler_name ${h.value} 无对应 JobHandler Bean（旧类找不到风险）` });
  }
  // Quartz 调度入口 Job 类必须 cn.zszj 包根（经 import 解析；这是 QRTZ_JOB_DETAILS.JOB_CLASS_NAME 的持久化源头）
  for (const ref of quartzJobClasses) {
    const raw = readRepoFile(ref.source) ?? '';
    const text = maskComments(raw).masked; // r2 根治：FQCN 解析输入同样走注释掩码
    const importRe = new RegExp(`import\\s+([\\w.]+)\\.${ref.value};`);
    const im = text.match(importRe);
    const fqcn = im ? `${im[1]}.${ref.value}` : null;
    const resolved = fqcn ?? `(同包推断)cn.zszj.framework.quartz.core.handler.${ref.value}`;
    if (!resolved.startsWith('cn.zszj.')) {
      problems.push({ check: 'quartz-job-class', value: resolved, why: 'Quartz 持久化 JOB_CLASS_NAME 源头非 cn.zszj 包根' });
    }
    // 同包推断必须有 import 或同包证据兜底，避免解析失败被静默放过
    if (!fqcn && !/package\s+cn\.zszj\.framework\.quartz\.core\.handler\s*;/.test(text)) {
      problems.push({ check: 'quartz-job-class-unresolved', value: ref.value, why: 'JobBuilder.newJob 所指类无法解析到 cn.zszj FQCN（import 缺失且非同包）' });
    }
  }
  // 恢复/受控拒绝审计常量（ZS-JOB-004 双轨审计）
  const constValues = new Set(outboxConstants.map((c) => c.value));
  for (const req of ['OUTBOX_EVENT_RETRIED', 'OUTBOX_EVENT_SKIPPED']) {
    if (!constValues.has(req)) problems.push({ check: 'audit-event-type', missing: req, why: 'outbox_recovery_log 双轨审计事件类型缺失' });
  }
  // 应用名 = Redis Stream 消费组（改名后旧组名残留会让新消费组读不到旧流）
  if (appYaml.springAppName !== 'zszj-server') {
    problems.push({ check: 'spring-app-name', value: appYaml.springAppName, why: 'spring.application.name 非 zszj-server（Redis Stream 消费组持久化命名合同）' });
  }
  // 白名单配置锚点（受控迁移的登记侧开关）
  if (!appYaml.hasHandlerWhitelistKey) {
    problems.push({ check: 'handler-whitelist-config', why: 'application.yaml 缺 zszj.job.handler-whitelist 配置锚点' });
  }
  // Jackson 全局 default typing 禁用（否则任意载荷都会落类名 FQCN）
  if (jsonUtilsText === null || hasDefaultTyping(jsonUtilsText)) {
    problems.push({ check: 'jackson-default-typing', why: 'JsonUtils 存在/启用全局 default typing（类名 FQCN 将落库）' });
  }
  // 静态锚点文件必须在（防重构后合同悬空）
  for (const a of REQUIRED_ANCHORS) {
    const text = readRepoFile(a.file);
    if (text === null) problems.push({ check: 'anchor-file-missing', file: a.file, why: a.why });
    else if (!text.includes(a.needle)) problems.push({ check: 'anchor-needle-missing', file: a.file, needle: a.needle, why: a.why });
  }
  return problems;
}

// ---------------------------------------------------------------------------
// 全量盘点（提取 + 判定）
// ---------------------------------------------------------------------------
export function runInventory() {
  const inventory = {
    jobSeeds: { scannedFiles: 0, handlers: [], beans: 0 },
    quartz: { sqlFilesScanned: 0, qrtzSeedRows: 0, jobBuilderRefs: 0 },
    outboxInbox: { migrationFiles: 0, sqlLiterals: 0, auditConstants: 0 },
    msg: { streamSubclasses: 0, routeOverrides: 0, yamlKeyPrefixes: 0, jsonTypeInfoImplFqcns: [] },
    sqlPackageShapedScanned: 0,
  };
  const violations = [];
  const qrtzViolations = [];
  // codex r2 根治：全部 Java 文本提取共用单一注释掩码（每文件恰好掩码一次，problem 只上报一次）。
  // codex r3 合同：掩码器词法模型边界外的输入（fatal problem，见 JAVA_LEXICAL_MODEL_BOUNDARIES）
  // → 该文件返回 null，**跳过全部 Java 判定**（不产出清单、不静默通过），problem 计入 contractProblems。
  const deriveProblems = [];
  const javaMaskMemo = new Map();
  const getMaskedJava = (rel) => {
    if (javaMaskMemo.has(rel)) return javaMaskMemo.get(rel);
    const raw = readRepoFile(rel);
    if (raw === null) return null;
    const { masked, problems } = maskComments(raw);
    deriveProblems.push(...problems.map((p) => ({ source: rel, ...p })));
    const value = (masked === null || problems.some((p) => p.fatal)) ? null : masked;
    javaMaskMemo.set(rel, value);
    return value;
  };

  // ① 任务域 + QRTZ 面 + SQL 包形旧 FQCN
  for (const rel of listGitFiles((f) => inCoreSql(f) && /\.sql$/.test(f))) {
    const text = readRepoFile(rel);
    if (text === null) continue;
    inventory.sqlPackageShapedScanned++;
    // 包形旧名：全量 SQL 面（含供应商方言参考文件）
    violations.push(...extractPackageShapedOldNames(text, rel));
    // 结构化元组提取：仅 PG 方言面
    if (inPgSql(rel)) {
      inventory.jobSeeds.scannedFiles++;
      const handlers = extractInfraJobSeedHandlers(text, rel);
      inventory.jobSeeds.handlers.push(...handlers);
      violations.push(...handlers.flatMap((h) => judgeValue(h.source, h.field, h.value)));
      for (const row of extractQrtzSeedInserts(text, rel)) {
        // QRTZ_* 任何种子行都是违规（迁移仅 DDL；JOB_CLASS_NAME 由运行期以 cn.zszj FQCN 写入）
        qrtzViolations.push({ source: row.source, field: row.field, value: row.value, pattern: 'qrtz-seed-row', sample: row.value });
        violations.push(...judgeValue(row.source, row.field, row.value));
      }
    }
  }
  inventory.quartz.qrtzSeedRows = qrtzViolations.length;
  inventory.quartz.sqlFilesScanned = inventory.sqlPackageShapedScanned; // [r4 P3-10] 报告统计接线：不再恒 0
  // 权威迁移链种子（合同用）：Flyway 实际执行的 db/migration 面
  const contractSeedHandlers = [];
  for (const rel of listGitFiles((f) => inFlywayMigration(f) && /\.sql$/.test(f))) {
    const text = readRepoFile(rel);
    if (text === null) continue;
    contractSeedHandlers.push(...extractInfraJobSeedHandlers(text, rel));
  }
  // JobHandler Bean 名清单（合同用）——注释掩码后提取（r2 根治：javadoc 中的 class/record 词不得伪造 Bean）
  const beanNames = [];
  for (const rel of listGitFiles((f) => inCoreJava(f))) {
    const masked = getMaskedJava(rel);
    if (masked === null) continue;
    beanNames.push(...extractJobHandlerBeanNames(masked));
  }
  inventory.jobSeeds.beanNames = beanNames;
  inventory.jobSeeds.beans = beanNames.length;

  // ② Quartz 调度入口（JobBuilder.newJob → JOB_CLASS_NAME 持久化源头）——掩码后提取
  const quartzJobClasses = [];
  for (const rel of listGitFiles((f) => inCoreJava(f) && /SchedulerManager\.java$/.test(f))) {
    const masked = getMaskedJava(rel);
    if (masked === null) continue;
    const refs = extractQuartzJobBuilderClasses(masked, rel);
    quartzJobClasses.push(...refs);
    for (const r of refs) violations.push(...judgeValue(r.source, r.field, r.value));
  }
  inventory.quartz.jobBuilderRefs = quartzJobClasses.length;

  // ③ Outbox/Inbox：状态机字面量 + 审计常量
  const outboxMigrations = [
    'services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260915.001__infra_outbox_event.sql',
    'services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260915.002__infra_inbox_event.sql',
    'services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260915.021__infra_outbox_recovery.sql',
    'services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260916.002__system_notify_channel_send.sql',
  ];
  for (const rel of outboxMigrations) {
    const text = readRepoFile(rel);
    if (text === null) continue;
    inventory.outboxInbox.migrationFiles++;
    const lits = extractSqlStringLiterals(text, rel);
    inventory.outboxInbox.sqlLiterals += lits.length;
    violations.push(...lits.flatMap((l) => judgeValue(l.source, l.field, l.value)));
  }
  const auditConstants = [];
  for (const rel of listGitFiles((f) => f.endsWith('AuditEventTypes.java'))) {
    const masked = getMaskedJava(rel);
    if (masked === null) continue;
    auditConstants.push(...extractStringConstants(masked, rel));
  }
  inventory.outboxInbox.auditConstants = auditConstants.length;
  violations.push(...auditConstants.flatMap((c) => judgeValue(c.source, c.field, c.value)));

  // ④ MSG/序列化
  const jsonTypeInfoImplFqcns = [];
  for (const rel of listGitFiles((f) => inCoreJava(f))) {
    const masked = getMaskedJava(rel);
    if (masked === null) continue;
    const subs = extractRedisMessageSubclasses(masked, rel);
    inventory.msg.streamSubclasses += subs.length;
    violations.push(...subs.flatMap((s) => judgeValue(s.source, s.field, s.value)));
    // 路由覆写（codex r0 P2-4 / r1 P2-B / r2 P2-B）：子类覆写 getChannel/getStreamKey 的返回
    // 字面量必须过旧名判定；方法体无法安全解析 → fail-loud 不静默放过
    if (subs.length) {
      const { literals: overrides, problems: routeProblems } = extractMessageRouteOverrides(masked, rel);
      inventory.msg.routeOverrides += overrides.length;
      violations.push(...overrides.flatMap((o) => judgeValue(o.source, o.field, o.value)));
      deriveProblems.push(...routeProblems);
    }
    if (extractJsonTypeInfoInterfaces(masked, rel).length) {
      // 该文件是多态接口：找实现类，按 package 声明 + 类型声明推导「实际持久化 FQCN」判定
      // （输入为掩码后文本——r2 P2-A：紧邻声明的 Javadoc 词不得劫持顶层类型匹配）
      const ifaceMatch = masked.match(/interface\s+([A-Za-z0-9_]+)/);
      const iface = ifaceMatch ? ifaceMatch[1] : null;
      if (iface) {
        for (const implRel of listGitFiles((f) => inCoreJava(f))) {
          const implMasked = getMaskedJava(implRel);
          if (implMasked === null) continue;
          if (!new RegExp(`implements\\s+[\\w.,<> \\t]*\\b${iface}\\b`).test(implMasked)) continue;
          const { results, problem } = deriveImplFqcns(implMasked, implRel, iface);
          if (problem) { deriveProblems.push(problem); continue; }
          for (const r of results) {
            jsonTypeInfoImplFqcns.push(r.value);
            violations.push(...judgeValue(r.source, r.field, r.value));
          }
        }
      }
    }
  }
  inventory.msg.jsonTypeInfoImplFqcns = jsonTypeInfoImplFqcns;
  const yamlPrefixes = [];
  for (const rel of listGitFiles((f) => /application.*\.yaml$/.test(f) && f.startsWith('services/zhongshu-core/zszj-server/src/main/resources/'))) {
    const text = readRepoFile(rel);
    if (text === null) continue;
    const ks = extractYamlKeyPrefixes(text, rel);
    yamlPrefixes.push(...ks);
    violations.push(...ks.flatMap((k) => judgeValue(k.source, k.field, k.value)));
  }
  inventory.msg.yamlKeyPrefixes = yamlPrefixes.length;

  return { inventory, violations: [...violations, ...qrtzViolations], beanNames, auditConstants, quartzJobClasses, contractSeedHandlers, deriveProblems };
}

// ---------------------------------------------------------------------------
// 注入自检（防扫描器空洞）：合成样例必须被同一套提取+判定路径命中
// ---------------------------------------------------------------------------
export function injectionSelfTest() {
  const samples = [
    {
      name: 'job-seed-old-handler',
      extract: () => extractInfraJobSeedHandlers(
        "INSERT INTO infra_job (id, name, status, handler_name, handler_param) VALUES (99, '旧任务', 2, 'yudaoDemoJob', '');", 'injected'),
      expectValue: 'yudaoDemoJob',
    },
    {
      // QRTZ 种子行存在即违规（行存在性本身是合同），且行内旧 FQCN 另被包形判定命中
      name: 'qrtz-seed-row',
      extract: () => {
        const sql = "INSERT INTO QRTZ_JOB_DETAILS (SCHED_NAME, JOB_NAME, JOB_CLASS_NAME) VALUES ('schedulerName', 'legacy', 'cn.iocoder.yudao.framework.quartz.core.handler.JobHandlerInvoker');";
        return [...extractQrtzSeedInserts(sql, 'injected'), ...extractPackageShapedOldNames(sql, 'injected')];
      },
      expectValue: null, // 布尔判定：行存在 + FQCN 命中合计 ≥2
      minHits: 2,
    },
    {
      name: 'sql-package-shaped-fqcn',
      extract: () => extractPackageShapedOldNames("-- comment\nUPDATE t SET config = '{\"@class\":\"cn.iocoder.yudao.module.infra.framework.file.core.client.db.DBFileClient\"}';", 'injected'),
      expectValue: null, // 布尔判定：命中 ≥1
      minHits: 1,
    },
    {
      name: 'quartz-job-class-old-ref',
      // 调度入口 Job 类经 import 解析出 FQCN 后交同一判定（模拟 checkNamingContract 的解析路径）
      extract: () => {
        const refs = extractQuartzJobBuilderClasses('JobDetail jobDetail = JobBuilder.newJob(OldInvoker.class)', 'injected');
        return refs.flatMap((r) => judgeValue(r.source, r.field, 'cn.iocoder.yudao.framework.quartz.core.handler.OldInvoker'));
      },
      expectValue: null, // 布尔判定：命中 ≥1
      minHits: 1,
    },
    {
      name: 'audit-constant-old-name',
      extract: () => extractStringConstants('public static final String LEGACY_EVENT = "yudao_order_created";', 'injected'),
      expectValue: 'yudao_order_created',
    },
    {
      name: 'outbox-sql-literal-old-name',
      extract: () => extractSqlStringLiterals("CHECK (status IN ('PENDING', 'yudao_dispatched'))", 'injected'),
      expectValue: 'yudao_dispatched',
    },
    {
      name: 'stream-subclass-scan',
      extract: () => extractRedisMessageSubclasses('public class YudaoDemoMessage extends AbstractRedisStreamMessage {', 'injected'),
      expectValue: 'YudaoDemoMessage', // 子类 SimpleName 即 stream key，须被旧名判定命中
    },
    {
      name: 'yaml-key-prefix-old',
      extract: () => extractYamlKeyPrefixes('      key-prefix: yudao_wx # 旧前缀', 'injected'),
      expectValue: 'yudao_wx',
    },
    {
      // codex r0 P2-3 变异反证：package 声明与目录不一致时，持久化 FQCN 必须按 package 声明
      // 推导（目录推导会假绿），带旧包根的声明必须被打红
      name: 'json-typinfo-package-mismatch-fqcn',
      extract: () => {
        const { results } = deriveImplFqcns(
          'package cn.iocoder.yudao.module.infra.framework.file.core.client.db;\n\npublic class DBFileClientConfig implements FileClientConfig {\n}',
          'any/dir/DBFileClientConfig.java', 'FileClientConfig');
        return results.flatMap((k) => judgeValue(k.source, k.field, k.value));
      },
      expectValue: null, // 布尔判定：命中 ≥1
      minHits: 1,
    },
    {
      // codex r0 P2-3 嵌套类：持久化 FQCN 按「package + Outer$Inner」二进制名推导
      name: 'json-typinfo-nested-class-binary-name',
      extract: () => deriveImplFqcns(
        'package cn.zszj.module.x;\npublic class Outer {\n  public static class Inner implements FileClientConfig {\n  }\n}',
        'any/dir/Outer.java', 'FileClientConfig').results,
      expectDerived: 'cn.zszj.module.x.Outer$Inner', // 值形状断言（非旧名判定）
    },
    {
      // codex r0 P2-4 变异反证：子类覆写 getChannel 返回旧路由名，必须被提取并打红
      name: 'message-route-override-old-name',
      extract: () => extractMessageRouteOverrides(
        'public class X extends AbstractRedisChannelMessage {\n  @Override\n  public String getChannel() { return "yudao_channel"; }\n}', 'injected').literals,
      expectValue: 'yudao_channel',
    },
    {
      // codex r1 P2-A record 形状：record ... implements 同样可成为多态实现，旧包根必须打红
      // （r0 版本仅匹配 class 声明，record 夹具被静默跳过、清单缩水假绿）
      name: 'json-typinfo-record-old-package',
      extract: () => {
        const { results } = deriveImplFqcns(
          'package cn.iocoder.yudao.module.infra.framework.file.core.client.db;\n\npublic record DBFileClientConfig(String basePath) implements FileClientConfig {\n}',
          'any/dir/DBFileClientConfig.java', 'FileClientConfig');
        return results.flatMap((k) => judgeValue(k.source, k.field, k.value));
      },
      expectValue: null, // 布尔判定：命中 ≥1
      minHits: 1,
    },
    {
      // codex r1 P2-A fail-loud：过滤命中 implements 但声明形状提取不出 FQCN → 必须产 problem，
      // 不许静默跳过（否则清单缩水无人知）
      name: 'json-typinfo-undeclared-impl-fail-loud',
      extract: () => {
        const { problem } = deriveImplFqcns('package cn.zszj.module.x;\n// 这里只是注释提及 implements FileClientConfig\n', 'injected', 'FileClientConfig');
        return problem ? [problem] : [];
      },
      expectValue: null, // 布尔判定：产 problem 即通过
      minHits: 1,
    },
    {
      // codex r1 P2-B 嵌套块：方法体含 if 复合块时不得在首个 '}' 截断——后置返回的
      // yudao_channel 必须被扫到（r0 版本 [^}]* 只扫到 zszj_channel，漏检假绿）
      name: 'message-route-override-nested-block',
      extract: () => {
        const { literals } = extractMessageRouteOverrides(
          'public class X extends AbstractRedisChannelMessage {\n'
          + '  public String getChannel() {\n'
          + '    if (userType == null) { return "zszj_channel"; }\n'
          + '    return "yudao_channel";\n'
          + '  }\n'
          + '}', 'injected');
        return literals.flatMap((k) => judgeValue(k.source, k.field, k.value));
      },
      expectValue: null, // 布尔判定：命中 ≥1
      minHits: 1,
    },
    {
      // codex r1 P2-B 字符串内花括号：字面量中的 '}' 不得终止方法体扫描，
      // 否则同文件后续方法（getStreamKey 返回 yudao_stream）整体漏检
      name: 'message-route-override-braces-in-string',
      extract: () => extractMessageRouteOverrides(
        'public class X extends AbstractRedisChannelMessage {\n'
        + '  public String getChannel() { return "brace}inside"; }\n'
        + '  public String getStreamKey() { return "yudao_stream"; }\n'
        + '}', 'injected').literals,
      expectValue: 'yudao_stream',
    },
    {
      // codex r2 P2-A 变异反证（精确形状）：干净包根 + 旧品牌类名 + 紧邻 Javadoc 含「record of」
      // ——r1 无掩码时类型匹配被注释劫持（cls=topLevel=of），旧品牌类名从 FQCN 彻底消失（0 violations
      // 假绿且无 derive-empty problem）；r2 掩码后必须推出真实类 FQCN、旧品牌类名被打红
      name: 'json-typinfo-javadoc-hijack-red',
      extract: () => {
        const src = 'package cn.zszj.module.infra.framework.file.core.client.db;\n\n'
          + '/** A record of file settings. */\n'
          + 'public class YudaoFileClientConfig implements FileClientConfig {\n}';
        const { results } = deriveImplFqcns(maskComments(src).masked, 'injected', 'FileClientConfig');
        return results.flatMap((k) => judgeValue(k.source, k.field, k.value));
      },
      expectValue: null, // 布尔判定：命中 ≥1
      minHits: 1,
    },
    {
      // codex r2 P2-A 形状断言：同场景干净包根——推导必须是真实类 FQCN，而非注释劫持的 $of
      name: 'json-typinfo-javadoc-hijack-shape',
      extract: () => {
        const src = 'package cn.zszj.module.infra.framework.file.core.client.db;\n\n'
          + '/** A record of file settings. */\n'
          + 'public class YudaoFileClientConfig implements FileClientConfig {\n}';
        return deriveImplFqcns(maskComments(src).masked, 'injected', 'FileClientConfig').results;
      },
      expectDerived: 'cn.zszj.module.infra.framework.file.core.client.db.YudaoFileClientConfig',
    },
    {
      // codex r2 P2-B 变异反证：块注释与 return 同行且注释内含引号——掩码前转义感知正则从
      // 注释内启动、把 return 开引号当转义消费（路由漏检且无 problem）；掩码后必须命中
      name: 'message-route-override-comment-inline',
      extract: () => {
        const src = 'public class X extends AbstractRedisChannelMessage {\n'
          + '  public String getChannel() { /* escape " as \\" */ return "yudao_channel"; }\n'
          + '}';
        const { literals } = extractMessageRouteOverrides(maskComments(src).masked, 'injected');
        return literals.flatMap((k) => judgeValue(k.source, k.field, k.value));
      },
      expectValue: null, // 布尔判定：命中 ≥1
      minHits: 1,
    },
    {
      // Java 17 文本块路由值：块内旧名同样参与判定（掩码保留文本块原文，内容提取进判定）
      name: 'message-route-override-textblock',
      extract: () => {
        const src = 'public class X extends AbstractRedisChannelMessage {\n'
          + '  public String getChannel() {\n'
          + '    return """\n'
          + '        yudao_channel\n'
          + '        """;\n'
          + '  }\n'
          + '}';
        const { literals } = extractMessageRouteOverrides(maskComments(src).masked, 'injected');
        return literals.flatMap((k) => judgeValue(k.source, k.field, k.value));
      },
      expectValue: null, // 布尔判定：命中 ≥1
      minHits: 1,
    },
    {
      // 掩码器字符串感知：字符串字面量内的 /* 不得被当块注释掩掉（否则后续路由漏检）
      name: 'masker-string-with-block-comment-token',
      extract: () => {
        const src = 'public class X extends AbstractRedisChannelMessage {\n'
          + '  public String getChannel() { String s = "/* not comment */"; return "yudao_channel"; }\n'
          + '}';
        const { literals } = extractMessageRouteOverrides(maskComments(src).masked, 'injected');
        return literals.flatMap((k) => judgeValue(k.source, k.field, k.value));
      },
      expectValue: null, // 布尔判定：命中 ≥1
      minHits: 1,
    },
    {
      // 文本块未闭合：无法安全词法分析 → maskComments 必须 fail-loud 产 problem，不放过
      name: 'masker-textblock-unterminated-fail-loud',
      extract: () => {
        const { problems } = maskComments('public class X {\n  String s = """\n never closed\n');
        return problems;
      },
      expectValue: null, // 布尔判定：产 problem 即通过
      minHits: 1,
    },
    {
      // codex r3 P2-B 变异反证（JLS 翻译语义）：注释里藏合格 Unicode 转义（\u000a=词法前翻译出的
      // 换行，注释实际断行、后续是可执行代码）→ 翻译预处理后掩码器正确断注释，藏在「注释」里的
      // 可执行方法被正常提取判定（r2 版会把整段抹掉 = 藏毒假绿）
      name: 'masker-unicode-escape-in-comment-translated',
      extract: () => {
        const src = 'public class X extends AbstractRedisChannelMessage {\n'
          + '  // note \\u000a public String getChannel() { return "yudao_channel"; }\n'
          + '}';
        const { masked, problems } = maskComments(src);
        if (problems.length) return []; // 合格转义被翻译，不应有 problem
        const { literals } = extractMessageRouteOverrides(masked, 'injected');
        return literals.flatMap((k) => judgeValue(k.source, k.field, k.value));
      },
      expectValue: null, // 布尔判定：命中 ≥1（藏毒无处遁形）
      minHits: 1,
    },
    {
      // 方案②兜底：合格转义目标为反斜杠（\u005c 自指链风险）→ fatal，整文件跳过判定
      name: 'masker-unicode-backslash-target-fail-loud',
      extract: () => {
        const { masked, problems } = maskComments('public class X {\n  String s = "\\u005c"; return "yudao_channel";\n}\n');
        const fatal = problems.filter((p) => p.check === 'unicode-escape-backslash-target' && p.fatal);
        if (masked !== null || fatal.length === 0) return [];
        const { literals } = extractMessageRouteOverrides(masked, 'injected');
        return literals.length === 0 ? fatal : []; // masked=null，无清单可产出
      },
      expectValue: null, // 布尔判定：fatal problem 在且无清单
      minHits: 1,
    },
    {
      // r4 订正（JLS 3.3 单遍语义 + P2-7 检出力）：\uu0079 是一次识别多个 u 的合格转义（非递归
      // 左剥），单遍翻译出 'y' 且翻译承担命中——多 u 翻译失效则值不解码、旧名漏检、本场景转红
      name: 'masker-unicode-multi-u-translated-load-bearing',
      extract: () => {
        const src = 'public class X extends AbstractRedisChannelMessage {\n'
          + '  public String getChannel() { return "\\uu0079udao_channel"; }\n'
          + '}';
        const { masked, problems } = maskComments(src);
        if (problems.length) return []; // \uu0079 合格，翻译为 y，不应有 problem
        const { literals } = extractMessageRouteOverrides(masked, 'injected');
        return literals.flatMap((k) => judgeValue(k.source, k.field, k.value));
      },
      expectValue: null, // 布尔判定：命中 ≥1 且零误报
      minHits: 1,
    },
    {
      // r4 P2-5（JLS 3.3）：合格反斜杠+u 序列后非 4 位 hex 是 javac 编译期错误 → fail-loud
      // 拒止（不静默修复非法输入——多轮翻译版会把 \u00\u00341 修成 A 假绿）
      name: 'masker-unicode-invalid-hex-fail-loud',
      extract: () => {
        const { masked, problems } = maskComments('public class X {\n  String g = "\\u00g1"; return "yudao_channel";\n}\n');
        const fatal = problems.filter((p) => p.check === 'unicode-escape-invalid-hex' && p.fatal);
        if (masked !== null || fatal.length === 0) return []; // fatal 合同：masked=null 且 fatal 在
        return fatal;
      },
      expectValue: null, // 布尔判定：fatal problem 在即通过
      minHits: 1,
    },
    {
      // 不误报确认 + 八进制解码检出力（r4 P2-3）：\\u0041（偶数反斜杠=转义反斜杠）、'\12'
      // 八进制 char、\171 八进制字符串均为合法形状零 problem；且 \171udao 经值解码后须命中旧名
      // ——删掉值解码器此场景即漏报转红（源码拼写 \171udao 不含 yudao 字面）
      name: 'masker-escaped-backslash-octal-decoded-hit',
      extract: () => {
        const src = 'public class X extends AbstractRedisChannelMessage {\n'
          + '  public String getChannel() {\n'
          + "    char c = '\\12'; // 八进制 char 转义，合法\n"
          + '    String p = "\\\\u0041"; // 偶数反斜杠，非合格 Unicode 转义\n'
          + '    return "\\171udao_channel";\n'
          + '  }\n'
          + '}';
        const { masked, problems } = maskComments(src);
        if (problems.length) return []; // 全部合法形状，不应有 problem
        const { literals, problems: p2 } = extractMessageRouteOverrides(masked, 'injected');
        if (p2.length) return []; // 值解码不应产 problem
        return literals.flatMap((k) => judgeValue(k.source, k.field, k.value));
      },
      expectValue: null, // 布尔判定：命中 ≥1 且零误报
      minHits: 1,
    },
    {
      // r4 P2-2/P2-3：裸 CR 开界行终止 + 块内 \<CRLF> 续行拼接均为合法文本块词法；续行解码
      // 承担命中（yu\<CRLF>dao 拼回 yudao）——行终止全形态或续行解码任一缺失即漏报转红。
      // 续行行取零缩进（javac incidental-indent 剥离以全部行为公共前缀，零缩进行使剥离为零，
      // 续行拼接语义与本门禁解码器一致，场景不引入未建模的缩进剥离差异）
      name: 'route-textblock-cr-terminator-and-line-continuation',
      extract: () => {
        const src = 'public class X extends AbstractRedisChannelMessage {\n'
          + '  public String getChannel() {\n'
          + '    return """\r'
          + '        yu\\\r\n'
          + 'dao_channel\n'
          + '""";\n'
          + '  }\n'
          + '}';
        const { masked, problems } = maskComments(src);
        if (problems.length) return []; // 合法文本块词法，不应有 problem
        const { literals, problems: p2 } = extractMessageRouteOverrides(masked, 'injected');
        if (p2.length) return [];
        return literals.flatMap((k) => judgeValue(k.source, k.field, k.value));
      },
      expectValue: null, // 布尔判定：命中 ≥1
      minHits: 1,
    },
    {
      // r4 P2-3 补全：文本块 incidental-indent 剥离（JLS 3.10.6）承担命中——带缩进续行
      // `yu\<LF>        dao` 在 javac 值为 yudao_channel（公共缩进 8 先剥离、后续行拼接）；
      // 剥离模型缺失则按原文判为 `yu        dao` 漏报转红
      name: 'route-textblock-incidental-indent-stripped',
      extract: () => {
        const src = 'public class X extends AbstractRedisChannelMessage {\n'
          + '  public String getChannel() {\n'
          + '    return """\n'
          + '        yu\\\n'
          + '        dao_channel\n'
          + '        """;\n'
          + '  }\n'
          + '}';
        const { masked, problems } = maskComments(src);
        if (problems.length) return []; // 合法文本块词法，不应有 problem
        const { literals, problems: p2 } = extractMessageRouteOverrides(masked, 'injected');
        if (p2.length) return [];
        return literals.flatMap((k) => judgeValue(k.source, k.field, k.value));
      },
      expectValue: null, // 布尔判定：命中 ≥1
      minHits: 1,
    },
    {
      // r5 P3-4 变异回归（八进制 2/3 位边界）：\400 首位 '4'>3 → 按两位转义 \40（空格）+ 字面
      // '0'，真实值 ' 0 yudao_channel'；「无条件吞三位」变异产出 'Ā yudao_channel'——值形状断言
      // 精确到期望值，变异即转红（judgeValue 命中无法区分二者）
      name: 'route-literal-octal-2digit-vs-3digit-boundary',
      extract: () => {
        const src = 'public class X extends AbstractRedisChannelMessage {\n'
          + '  public String getChannel() { String s = "\\400 yudao_channel"; return "yudao_channel"; }\n'
          + '}';
        const { masked, problems } = maskComments(src);
        if (problems.length) return [];
        const { literals, problems: p2 } = extractMessageRouteOverrides(masked, 'injected');
        if (p2.length) return [];
        return literals; // 值形状断言由 expectDerived 承担
      },
      expectDerived: ' 0 yudao_channel',
    },
    {
      // r5 P3-4 变异回归（空行缩进不参与最小值）：续行后小缩进空白行（2 空格）不参与最小缩进。
      // javac 语义：剥离 min=8 后行序列为 [yu\, '', dao_channel, '']，续行仅消一个终止符、空行
      // 留下空行 → 真实值 'yu\n\ndao_channel\n'；「空行参与」变异剥离 min=2 → 值带缩进前缀——
      // 值形状断言精确区分（两种取值均不含 yudao 连写，命中断言无法区分二者）
      name: 'route-textblock-blank-line-indent-not-participating',
      extract: () => {
        const src = 'public class X extends AbstractRedisChannelMessage {\n'
          + '  public String getChannel() {\n'
          + '    return """\n'
          + '        yu\\\n'
          + '  \n'
          + '        dao_channel\n'
          + '        """;\n'
          + '  }\n'
          + '}';
        const { masked, problems } = maskComments(src);
        if (problems.length) return [];
        const { literals, problems: p2 } = extractMessageRouteOverrides(masked, 'injected');
        if (p2.length) return [];
        return literals; // 值形状断言由 expectDerived 承担
      },
      expectDerived: 'yu\ndao_channel\n', // javac 值：行拼接一条 LF + 续行消一条 LF
    },
    {
      // r5 P3-4 变异回归（花括号提取器文本块闭界偏移，r4 P2-6 防回归）：数组字面量文本块后
      // 跟随其它方法——偏移 +3 变异会越过方法收 '}'、把后续方法体吞进当前 body，其 yudao 诊断
      // 值被误判为违规（命中 0 为正确语义）
      name: 'route-brace-textblock-array-offset-regression',
      extract: () => {
        const src = 'public class X extends AbstractRedisChannelMessage {\n'
          + '  public String getChannel() { return new String[]{"""\n'
          + '        zszj_ok\n'
          + '        """}[0]; }\n'
          + '  public String diagnostic() { return "yudao_diag"; }\n'
          + '}';
        const { masked, problems } = maskComments(src);
        if (problems.length) return [];
        const { literals, problems: p2 } = extractMessageRouteOverrides(masked, 'injected');
        if (p2.length) return [];
        return literals.flatMap((k) => judgeValue(k.source, k.field, k.value));
      },
      expectValue: null,
      minHits: 0, // 负向断言：diagnostic() 的 yudao_diag 不得落入 getChannel 方法体（maxHits 语义由 0 命中承担）
      maxHits: 0,
    },
    {
      // r5 P3-4 变异回归（文本块扫描转义感知防回归）：块内 \"（转义闭界符前半）不得终止扫描——
      // 「取消转义感知」变异会在 """" 处提前闭合，x\ 成孤立反斜杠产 problem、yudao 漏报转红
      name: 'route-textblock-escaped-delimiter-escape-aware',
      extract: () => {
        const src = 'public class X extends AbstractRedisChannelMessage {\n'
          + '  public String getChannel() {\n'
          + '    return """\n'
          + '        x\\"""yudao_channel\n'
          + '        """;\n'
          + '  }\n'
          + '}';
        const { masked, problems } = maskComments(src);
        if (problems.length) return [];
        const { literals, problems: p2 } = extractMessageRouteOverrides(masked, 'injected');
        if (p2.length) return [];
        return literals.flatMap((k) => judgeValue(k.source, k.field, k.value));
      },
      expectValue: null,
      minHits: 1,
    },
    {
      // r5 P3-4 变异回归（字符串扫描反斜杠跨行检查防回归）：普通字符串内 \<LF> 是 Java 非法
      // 形状 → fatal + masked=null；「删除跨行检查」变异静默跳过终止符、无 fatal → 转红
      name: 'masker-string-backslash-crossline-fatal',
      extract: () => {
        const { masked, problems } = maskComments('public class X {\n  String s = "a\\\nb"; return "yudao_channel";\n}\n');
        const fatal = problems.filter((p) => p.check === 'string-literal-unterminated' && p.fatal);
        if (masked !== null || fatal.length === 0) return [];
        return fatal;
      },
      expectValue: null,
      minHits: 1,
    },
    {
      // r5 P2-1 变异回归（NBSP 非 Java 空白）：NBSP 行是内容行（非空、前导空白 0）→ 公共缩进 0、
      // 不剥离、续行拼接隔着缩进不形成 yudao（命中 0 为正确语义）；JS trim() 判空变异会把 NBSP 行
      // 当空行忽略 → 剥离 4 → 拼出 yudao 假红转红
      name: 'route-textblock-nbsp-line-not-blank',
      extract: () => {
        const src = 'public class X extends AbstractRedisChannelMessage {\n'
          + '  public String getChannel() {\n'
          + '    return """\n'
          + '\u00a0\n'
          + '    yu\\\n'
          + '    dao_channel\n'
          + '    """;\n'
          + '  }\n'
          + '}';
        const { masked, problems } = maskComments(src);
        if (problems.length) return [];
        const { literals, problems: p2 } = extractMessageRouteOverrides(masked, 'injected');
        if (p2.length) return [];
        return literals.flatMap((k) => judgeValue(k.source, k.field, k.value));
      },
      expectValue: null,
      minHits: 0, // 负向断言：NBSP 行为内容行，续行拼接不得形成 yudao
      maxHits: 0,
    },
    {
      // codex r3 P2-A 变异反证：行注释以 \r 终止（JLS 三种行终止符）——注释后的 return
      // 不得被当注释吞掉
      name: 'masker-line-comment-cr-terminator',
      extract: () => {
        const src = 'public class X extends AbstractRedisChannelMessage {\n'
          + '  public String getChannel() { // note\r return "yudao_channel";\n }\n'
          + '}';
        const { masked, problems } = maskComments(src);
        if (problems.length) return [];
        const { literals } = extractMessageRouteOverrides(masked, 'injected');
        return literals.flatMap((k) => judgeValue(k.source, k.field, k.value));
      },
      expectValue: null, // 布尔判定：命中 ≥1
      minHits: 1,
    },
    {
      // \r\n 行终止符：两个字符都被消费，注释后代码照常提取
      name: 'masker-line-comment-crlf-terminator',
      extract: () => {
        const src = 'public class X extends AbstractRedisChannelMessage {\n'
          + '  public String getChannel() { // note\r\n return "yudao_channel";\n }\n'
          + '}';
        const { masked, problems } = maskComments(src);
        if (problems.length) return [];
        const { literals } = extractMessageRouteOverrides(masked, 'injected');
        return literals.flatMap((k) => judgeValue(k.source, k.field, k.value));
      },
      expectValue: null, // 布尔判定：命中 ≥1
      minHits: 1,
    },
    {
      // codex r3 P2-C 变异反证：开界符空白含换页符（JLS 允许 space/tab/FF）——"""\f\n 开头的
      // 文本块内容里的 /* 不得被误当未闭合注释（否则后续方法全被抹）
      name: 'masker-textblock-formfeed-open',
      extract: () => {
        const src = 'public class X extends AbstractRedisChannelMessage {\n'
          + '  public String getChannel() {\n'
          + '    return """\f\n'
          + '        /* 不是注释 */\n'
          + '        yudao_channel\n'
          + '        """;\n'
          + '  }\n'
          + '  public String getStreamKey() { return "zszj_ok"; }\n'
          + '}';
        const { masked, problems } = maskComments(src);
        if (problems.length) return [];
        const { literals } = extractMessageRouteOverrides(masked, 'injected');
        return literals.flatMap((k) => judgeValue(k.source, k.field, k.value));
      },
      expectValue: null, // 布尔判定：命中 ≥1 且零误报
      minHits: 1,
    },
    {
      // 文本块内 \s 转义（r3 顺带确认）：转义不参与终止判定，块内旧名照常判定
      name: 'textblock-escape-s-not-terminator',
      extract: () => {
        const src = 'public class X extends AbstractRedisChannelMessage {\n'
          + '  public String getChannel() {\n'
          + '    return """\n'
          + '        \\s   yudao_channel\n'
          + '        """;\n'
          + '  }\n'
          + '}';
        const { masked, problems } = maskComments(src);
        if (problems.length) return [];
        const { literals } = extractMessageRouteOverrides(masked, 'injected');
        return literals.flatMap((k) => judgeValue(k.source, k.field, k.value));
      },
      expectValue: null, // 布尔判定：命中 ≥1
      minHits: 1,
    },
    {
      // 合同：未闭合块注释 → fatal problem（文件跳过判定，不静默通过）
      name: 'masker-block-comment-unterminated-fail-loud',
      extract: () => {
        const { masked, problems } = maskComments('public class X {\n  /* never closed\n  return "yudao_channel";\n');
        const fatal = problems.filter((p) => p.check === 'block-comment-unterminated' && p.fatal);
        if (masked !== null || fatal.length === 0) return []; // r4 P3-8 合同：任一 fatal → masked=null
        return fatal; // 被注释吞掉的内容不得产出清单（masked 已不可用）
      },
      expectValue: null, // 布尔判定：fatal problem 在且清单为空
      minHits: 1,
    },
    {
      // 合同：跨行未闭合普通字符串（Java 非法形状）→ fatal problem
      name: 'masker-string-unterminated-fail-loud',
      extract: () => {
        const { problems } = maskComments('public class X {\n  String s = "no close\n}\n');
        return problems.filter((p) => p.check === 'string-literal-unterminated' && p.fatal);
      },
      expectValue: null, // 布尔判定：产 fatal problem 即通过
      minHits: 1,
    },
  ];
  const results = [];
  let failed = false;
  for (const s of samples) {
    const extracted = s.extract();
    let hits;
    let ok;
    if (s.expectDerived) {
      // 值形状断言：推导出的 FQCN 必须等于预期二进制名（派生正确性，非旧名判定）
      ok = Array.isArray(extracted) && extracted.some((k) => k.value === s.expectDerived);
      hits = ok ? 1 : 0;
    } else if (s.expectValue === null) {
      hits = extracted.length;
      // maxHits（r5 P3-4 负向断言）：限制命中上限（如 maxHits: 0 断言「不得产生任何命中」——
      // 用于越界/误报回归：缺陷引入后命中数变正即转红）
      ok = hits >= (s.minHits ?? 1) && (s.maxHits === undefined || hits <= s.maxHits);
    } else {
      hits = extracted.filter((k) => k.value === s.expectValue)
        .flatMap((k) => judgeValue(k.source, k.field, k.value)).length;
      ok = hits >= (s.minHits ?? 1);
    }
    results.push({ sample: s.name, extracted: Array.isArray(extracted) ? extracted.length : 1, oldNameHits: hits, pass: ok });
    if (!ok) failed = true;
  }
  // 反向：非包形来源署名不得误报（G3 已登记口径在本门禁的值级映射）
  const benign = extractPackageShapedOldNames('-- 来源：sql/postgresql/ruoyi-vue-pro.sql + quartz.sql（经 ZS-BRAND 改名后的新装基线）', 'benign');
  const benignOk = benign.length === 0;
  results.push({ sample: 'benign-provenance-comment-not-flagged', extracted: 1, oldNameHits: benign.length, pass: benignOk });
  if (!benignOk) failed = true;
  return { results, pass: !failed };
}

// ---------------------------------------------------------------------------
// 主流程
// ---------------------------------------------------------------------------
const invokedDirectly = process.argv[1] && process.argv[1].replaceAll('\\', '/').endsWith('scripts/brand/verify-brand-004c-persistence.mjs');
if (invokedDirectly) {
  try {
    const { inventory, violations, beanNames, auditConstants, quartzJobClasses, contractSeedHandlers, deriveProblems } = runInventory();
    const appYamlText = readRepoFile('services/zhongshu-core/zszj-server/src/main/resources/application.yaml') ?? '';
    const jsonUtilsRel = listGitFiles((f) => f.endsWith('util/json/JsonUtils.java'))[0] ?? null;
    const jsonUtilsText = jsonUtilsRel ? maskComments(readRepoFile(jsonUtilsRel) ?? '').masked : null;
    const contractProblems = checkNamingContract({
      seedHandlers: contractSeedHandlers,
      beanNames,
      quartzJobClasses,
      outboxConstants: auditConstants,
      appYaml: {
        springAppName: extractSpringAppName(appYamlText),
        hasHandlerWhitelistKey: /handler-whitelist:/.test(appYamlText),
      },
      jsonUtilsText,
    }).concat(deriveProblems);
    const selfTest = injectionSelfTest();

    const report = {
      task: 'ZS-BRAND-004.C 任务与消息持久化引用静态门禁',
      inventory: quiet ? {
        jobSeeds: { scannedFiles: inventory.jobSeeds.scannedFiles, handlerCount: inventory.jobSeeds.handlers.length, handlers: inventory.jobSeeds.handlers.map((h) => h.value), beanCount: beanNames.length },
        quartz: { sqlFilesScanned: inventory.quartz.sqlFilesScanned, qrtzSeedRows: inventory.quartz.qrtzSeedRows, jobBuilderRefs: inventory.quartz.jobBuilderRefs },
        outboxInbox: inventory.outboxInbox,
        msg: { streamSubclasses: inventory.msg.streamSubclasses, routeOverrides: inventory.msg.routeOverrides, yamlKeyPrefixes: inventory.msg.yamlKeyPrefixes, jsonTypeInfoImplFqcns: inventory.msg.jsonTypeInfoImplFqcns },
      } : inventory,
      oldNameViolations: violations,
      namingContractProblems: contractProblems,
      injectionSelfTest: selfTest,
      pass: violations.length === 0 && contractProblems.length === 0 && selfTest.pass,
    };
    console.log(JSON.stringify(report, null, 2));
    console.error(
      `jobSeeds=${inventory.jobSeeds.handlers.length} jobBeans=${beanNames.length} `
      + `qrtzSeedRows=${inventory.quartz.qrtzSeedRows} sqlFiles=${inventory.quartz.sqlFilesScanned} `
      + `outboxMigrations=${inventory.outboxInbox.migrationFiles} auditConstants=${inventory.outboxInbox.auditConstants} `
      + `streamSubclasses=${inventory.msg.streamSubclasses} routeOverrides=${inventory.msg.routeOverrides} yamlKeyPrefixes=${inventory.msg.yamlKeyPrefixes} `
      + `jsonTypeInfoImpls=${inventory.msg.jsonTypeInfoImplFqcns.length} `
      + `oldNameViolations=${violations.length} contractProblems=${contractProblems.length} `
      + `injectionSelfTest=${selfTest.pass ? 'PASS' : 'FAIL'} => ${report.pass ? 'PASS' : 'FAIL'}`,
    );
    process.exitCode = report.pass ? 0 : 1;
  } catch (error) {
    console.error(`brand-004c persistence check failed: ${error.message}`);
    process.exitCode = 2;
  }
}
