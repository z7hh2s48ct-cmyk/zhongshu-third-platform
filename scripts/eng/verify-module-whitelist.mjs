/**
 * ZS-ENG-001 模块白名单静态检查（本地/评审入口，Maven 测试 ModuleWhitelistTest 的无 JDK 补充）。
 *
 * 检查项：
 *  1. ModuleWhitelist.java 的 ENABLED_MODULES 与根 pom.xml / zszj-server/pom.xml 激活模块一致；
 *  2. DefaultController @RequestMapping 覆盖全部未启用模块前缀，且不含启用模块前缀；
 *  3. zszj-server 主源码不引用未启用模块的包；
 *  4. 未启用模块不在根 pom 激活 modules 中（不参与编译装配）。
 * 用法：node scripts/eng/verify-module-whitelist.mjs
 */
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));
const core = join(root, 'services', 'zhongshu-core');
const rel = (p) => join(core, p);

const issues = [];
const stripComments = (t) => t.replace(/<!--[\s\S]*?-->/g, '');
const moduleArtifacts = (t) => [...stripComments(t).matchAll(/<artifactId>(zszj-module-[a-z-]+)<\/artifactId>/g)].map((m) => m[1].replace('zszj-module-', ''));

// 解析白名单类
const whitelistSrc = readFileSync(rel('zszj-server/src/main/java/cn/zszj/server/ModuleWhitelist.java'), 'utf8');
const enabledMatch = whitelistSrc.match(/ENABLED_MODULES = List\.of\(([^)]*)\)/);
if (!enabledMatch) issues.push('无法解析 ModuleWhitelist.ENABLED_MODULES');
const enabled = enabledMatch ? [...enabledMatch[1].matchAll(/"([a-z-]+)"/g)].map((m) => m[1]) : [];
const disabled = [...whitelistSrc.matchAll(/prefixes\.put\("([a-z-]+)", List\.of\(([^)]*)\)\)/g)].map((m) => ({
  module: m[1],
  prefixes: [...m[2].matchAll(/"([^"]+)"/g)].map((x) => x[1]),
}));
if (!enabled.length || !disabled.length) issues.push('白名单清单为空或解析失败');

// 1. POM 激活一致性
const rootPom = readFileSync(rel('pom.xml'), 'utf8');
const serverPom = readFileSync(rel('zszj-server/pom.xml'), 'utf8');
const rootModules = [...stripComments(rootPom).matchAll(/<module>(zszj-module-[a-z-]+)<\/module>/g)].map((m) => m[1].replace('zszj-module-', ''));
const serverDeps = moduleArtifacts(serverPom);
if (JSON.stringify(rootModules.slice().sort()) !== JSON.stringify(enabled.slice().sort())) {
  issues.push(`根 pom 激活业务模块 [${rootModules}] 与白名单 [${enabled}] 不一致`);
}
if (JSON.stringify(serverDeps.slice().sort()) !== JSON.stringify(enabled.slice().sort())) {
  issues.push(`zszj-server 依赖业务模块 [${serverDeps}] 与白名单 [${enabled}] 不一致`);
}

// 2. DefaultController 覆盖
const controllerSrc = readFileSync(rel('zszj-server/src/main/java/cn/zszj/server/controller/DefaultController.java'), 'utf8');
for (const { module, prefixes } of disabled) {
  for (const prefix of prefixes) {
    if (!controllerSrc.includes(`"${prefix}"`)) issues.push(`DefaultController 缺少 ${module} 前缀兜底: ${prefix}`);
  }
}
for (const m of enabled) {
  if (controllerSrc.includes(`"/admin-api/${m}/**"`)) issues.push(`启用模块 ${m} 不应被兜底拦截`);
}

// 3. server 主源码不引用未启用模块包
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
    if (text.includes(`cn.zszj.module.${module}.`)) issues.push(`${p} 引用未启用模块包 cn.zszj.module.${module}`);
  }
});

// 4. 未启用模块源码目录存在（保留资产）但不激活
for (const { module } of disabled) {
  const dir = rel(`zszj-module-${module}`);
  try {
    statSync(dir);
  } catch {
    issues.push(`未启用模块源码目录缺失: zszj-module-${module}`);
  }
}

console.log(JSON.stringify({ enabled, disabledModules: disabled.map((d) => d.module), issueCount: issues.length, issues }, null, 2));
process.exitCode = issues.length ? 1 : 0;
