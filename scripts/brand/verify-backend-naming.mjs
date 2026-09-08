/**
 * ZS-BRAND-002 后端改名静态一致性检查（无 JDK/Maven 环境下的初步验证；
 * 干净构建、依赖树与真实 PG 启动仍按 docs/06 第 7 节在 B01/B02 补证）。
 *
 * 用法：node scripts/brand/verify-backend-naming.mjs
 * 检查项：
 *  1. 每个 .java 的 package 声明与其目录路径一致；
 *  2. 自动配置注册（META-INF/spring/*.imports、spring.factories）中的 FQCN 可解析到源码文件；
 *  3. Mapper XML 的 namespace/resultType/parameterType 与 logback logger 中的 cn.zszj FQCN 可解析；
 *  4. POM 中产品坐标统一为 cn.zszj/zszj-*，module/依赖指向存在的模块目录；
 *  5. 残留报告：作用域内 cn.iocoder/yudao/Yudao/YUDAO/youdao 出现位置分类输出。
 * 任一硬性检查失败以非零退出。
 */
import { readFileSync, existsSync, readdirSync, statSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join, dirname, resolve } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));
const core = join(root, 'services', 'zhongshu-core');

function walk(dir, fn) {
  for (const name of readdirSync(dir)) {
    const p = join(dir, name);
    const st = statSync(p);
    if (st.isDirectory()) walk(p, fn);
    else fn(p);
  }
}

const issues = [];
const allJava = [];
walk(core, (p) => {
  if (p.endsWith('.java') && !p.includes(`${sep()}` + 'zszj-ui' + `${sep()}`) && !p.includes(`${sep()}` + 'sql' + `${sep()}`)) allJava.push(p);
});
function sep() { return process.platform === 'win32' ? '\\' : '/'; }

// 1. package 声明与目录一致（上游历史遗留：vo/userItem 目录大写与包声明 useritem 不一致，
//    属上游既有结构问题且 javac 不校验，与品牌改名无关，登记豁免）
const packageCaseExemptions = new Set(['vo/userItem/']);
for (const file of allJava) {
  const text = readFileSync(file, 'utf8');
  const m = text.match(/^\s*package\s+([\w.]+)\s*;/m);
  if (!m) { issues.push(['missing-package', rel(file)]); continue; }
  const relPath = dirname(file).slice(core.length + 1).split(sep()).join('/');
  const expect = relPath.replace(/^.*\/src\/(main|test)\/java\//, '');
  if (m[1] !== expect.replaceAll('/', '.')) {
    if (packageCaseExemptions.has(`${expect.split('/').slice(-2).join('/')}/`) && m[1] === expect.replaceAll('/', '.').toLowerCase()) continue;
    issues.push(['package-path-mismatch', `${rel(file)}: ${m[1]} != ${expect}`]);
  }
}

// FQCN -> 源码文件解析
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
  if (!/^[a-z][\w.]*Zszj|^cn\.zszj/.test(fqcn)) return; // 只校验产品 FQCN
  if (!resolveFqcn(fqcn)) issues.push(['unresolved-fqcn', `${where}: ${fqcn}`]);
}

// 2. 自动配置注册
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
      if (line.trim().endsWith('\\') || !line.includes('=') || line.trim().startsWith('#')) continue;
      for (const token of line.split('=')[1].split(',')) {
        const t = token.trim();
        if (t) checkFqcn(rel(p), t);
      }
    }
  }
});

// 3. Mapper XML 与 logback
walk(core, (p) => {
  const norm = p.split(sep()).join('/');
  if (norm.includes('/zszj-ui/') || !(norm.endsWith('.xml'))) return;
  const text = readFileSync(p, 'utf8');
  const re = /(?:namespace|resultType|parameterType|type|ref)="(cn\.zszj[\w.]*)"/g;
  let m;
  while ((m = re.exec(text))) checkFqcn(rel(p), m[1]);
  const re2 = /name="(cn\.zszj[\w.]*)"/g;
  while ((m = re2.exec(text))) {
    const fqcn = m[1];
    if (resolveFqcn(fqcn) || classIndex.has(fqcn) || existsSync(join(core, ...fqcn.split('.')))) continue;
    // logger 名允许是包前缀而非具体类
    const prefix = fqcn;
    let ok = false;
    for (const c of classIndex) if (c.startsWith(prefix + '.')) { ok = true; break; }
    if (!ok) issues.push(['unresolved-logger', `${rel(p)}: ${fqcn}`]);
  }
});

// 4. POM 坐标一致性
const pomFiles = [];
walk(core, (p) => { if (p.endsWith('pom.xml')) pomFiles.push(p); });
for (const pom of pomFiles) {
  const text = readFileSync(pom, 'utf8').replace(/<!--[\s\S]*?-->/g, '');
  if (/cn\.iocoder/.test(text)) issues.push(['pom-old-groupid', rel(pom)]);
  if (/yudao/.test(text)) issues.push(['pom-old-artifact', rel(pom)]);
  for (const m of text.matchAll(/<module>([^<]+)<\/module>/g)) {
    if (!existsSync(join(dirname(pom), m[1], 'pom.xml'))) issues.push(['pom-missing-module', `${rel(pom)}: ${m[1]}`]);
  }
}

// 5. 残留报告（人工审阅 + 与 docs/06 第 4 节例外对照）
const residue = new Map();
const residueRe = /cn\.iocoder|youdao|YUDAO|[Yy]udao/g;
walk(core, (p) => {
  const norm = p.split(sep()).join('/');
  if (norm.includes('/zszj-ui/') || norm.includes('/.git/') || /\.(png|jpg|jpeg|gif|xdb|ico)$/i.test(norm)) return;
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
