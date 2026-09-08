/**
 * ZS-DB-001.A 数据源与驱动静态检查（无 Maven 环境的依赖树代理验证；
 * 完整依赖树/打包产物核验在 B01/B02 由 mvn dependency:tree + jar tf 补证）。
 *
 * 检查项：
 *  1. starter 中各数据库驱动（含 mysql-connector-j）均为 optional，不进入运行包；
 *  2. zszj-server 显式依赖 org.postgresql:postgresql（非 optional），且不含任何 MySQL 驱动；
 *  3. 全部环境 yaml 无未注释的 jdbc:mysql / FROM DUAL / 模拟从库 slave 数据源；
 *  4. 部署模板数据源为 PG 占位符。
 * 用法：node scripts/db/verify-datasource-pg.mjs
 */
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));
const core = join(root, 'services', 'zhongshu-core');
const issues = [];
const stripComments = (t) => t.replace(/<!--[\s\S]*?-->/g, '');

// 1. starter 驱动 optional
const starterPom = readFileSync(join(core, 'zszj-framework/zszj-spring-boot-starter-mybatis/pom.xml'), 'utf8');
const driverBlock = starterPom.match(/<!-- DB 相关[\s\S]*?<\/dependency>\s*\n\s*<dependency>\s*\n\s*<groupId>com\.alibaba<\/groupId>/);
if (!driverBlock) issues.push('无法定位 starter 的 DB 驱动声明块');
else {
  const optionalDrivers = [...driverBlock[0].matchAll(/<artifactId>([a-z0-9-]+)<\/artifactId>\s*(?:<optional>true<\/optional>)?/g)]
    .filter(([, a]) => /jdbc|connector|ojdbc|postgresql|sqlserver|kingbase|opengauss|Dm/i.test(a));
  for (const [, artifact] of optionalDrivers) {
    const decl = driverBlock[0].match(new RegExp(`<artifactId>${artifact}</artifactId>[\\s\\S]{0,80}`));
    if (decl && !decl[0].includes('<optional>true</optional>')) {
      issues.push(`starter 驱动 ${artifact} 不是 optional，会进入运行包`);
    }
  }
}

// 2. server 显式 PG、无 MySQL
const serverPom = stripComments(readFileSync(join(core, 'zszj-server/pom.xml'), 'utf8'));
const pgDep = serverPom.match(/<groupId>org\.postgresql<\/groupId>\s*\n\s*<artifactId>postgresql<\/artifactId>([\s\S]{0,60})/);
if (!pgDep) issues.push('zszj-server 未显式声明 postgresql 驱动');
else if (pgDep[1].includes('<optional>true</optional>')) issues.push('zszj-server 的 postgresql 驱动为 optional，将不进入运行包');
if (/com\.mysql|mysql-connector/.test(serverPom)) issues.push('zszj-server 声明了 MySQL 驱动（禁止 MySQL 回退）');

// 3. 环境 yaml 无 MySQL 回退
for (const f of ['application.yaml', 'application-local.yaml', 'application-dev.yaml']) {
  const text = readFileSync(join(core, 'zszj-server/src/main/resources', f), 'utf8');
  const active = text.split('\n').filter((l) => !/^\s*#/.test(l)).join('\n');
  if (/jdbc:mysql:/.test(active)) issues.push(`${f} 存在未注释的 jdbc:mysql 配置`);
  const hasDatasource = /dynamic:.*多数据源配置/.test(active) && /master:/.test(active);
  if (hasDatasource) {
    if (/FROM DUAL/.test(active)) issues.push(`${f} 的 validation-query 为 MySQL 方言（FROM DUAL）`);
    if (/^\s*slave:/.test(active)) issues.push(`${f} 仍存在 slave 模拟从库`);
    if (!/validation-query:/.test(active)) issues.push(`${f} 缺少 validation-query（连接检查）`);
  }
}

// 4. 部署模板
const prodTemplate = readFileSync(join(core, 'script/config/application-prod.yaml'), 'utf8');
if (!prodTemplate.includes('${ZSZJ_DATASOURCE_URL}')) issues.push('部署模板缺少 ZSZJ_DATASOURCE_URL 必填占位符');
if (/jdbc:mysql:/.test(prodTemplate)) issues.push('部署模板含 jdbc:mysql');

console.log(JSON.stringify({ issueCount: issues.length, issues }, null, 2));
process.exitCode = issues.length ? 1 : 0;
