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
  ['services/zhongshu-core/zszj-server/src/main/resources/application.yaml', 'yaml'],
  ['services/zhongshu-core/zszj-server/src/main/resources/application-local.yaml', 'yaml'],
  ['services/zhongshu-core/zszj-server/src/main/resources/application-dev.yaml', 'yaml'],
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

const SECRET_KEY = /(password|passwd|secret|token|access-?key|secret-?key|api-?key|private-?key)\s*[:=]/i;
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
      const value = line.slice(line.search(SECRET_KEY)).split(/[:=]/).slice(1).join(':').trim().replace(/^["']|["']$/g, '');
      if (value && !PLACEHOLDER.test(value) && !SAFE_LITERALS.test(value) && !value.startsWith('${') && !value.includes('# ')) {
        issues.push({ path: rel, line: i + 1, reason: `秘密类键使用了字面量取值: ${line.trim().slice(0, 60)}` });
      }
    }
    // 2. 密钥块与长串
    if (/(BEGIN (RSA )?PRIVATE KEY|BEGIN CERTIFICATE)/.test(line)) {
      issues.push({ path: rel, line: i + 1, reason: '配置文件中出现私钥/证书块' });
    }
    const base64Blob = line.match(/[A-Za-z0-9+/=]{200,}/);
    if (base64Blob) issues.push({ path: rel, line: i + 1, reason: '配置文件中出现超长 Base64 串（疑似密钥）' });
    if (active) {
      const hex = line.match(/\b[0-9a-f]{20,}\b/i);
      if (hex) issues.push({ path: rel, line: i + 1, reason: `出现长十六进制串（疑似凭据/统计 ID）: ${hex[0].slice(0, 12)}…` });
    }
  });
}

// 3. 部署模板必填项必须无默认值（缺配即启动失败，见 script/config/README.md 合同）
const prodRel = 'services/zhongshu-core/script/config/application-prod.yaml';
const prod = readFileSync(join(root, prodRel), 'utf8');
const REQUIRED = [
  'ZSZJ_DATASOURCE_URL', 'ZSZJ_DATASOURCE_USERNAME', 'ZSZJ_DATASOURCE_PASSWORD',
  'ZSZJ_REDIS_HOST', 'ZSZJ_REDIS_PASSWORD',
];
for (const name of REQUIRED) {
  const bare = new RegExp(`\\$\\{${name}\\}`);
  if (!bare.test(prod)) {
    issues.push({ path: prodRel, reason: `必填变量 ${name} 必须为无默认值占位符 \${${name}}（缺配明确失败）` });
  }
}

console.log(JSON.stringify({ scanned, issueCount: issues.length, issues: issues.slice(0, 30) }, null, 2));
process.exitCode = issues.length ? 1 : 0;
