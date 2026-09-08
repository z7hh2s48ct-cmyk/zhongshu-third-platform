/**
 * ZS-BRAND-001~006 命名迁移规则（docs/06-品牌素材与命名映射.md 的脚本化实现）。
 *
 * 规则按序生效：先保护（PROTECTED）上游署名 URL，再执行替换（REPLACEMENTS）。
 * 保护项与 docs/06 第 4.1 节"必要保留"对应；替换规则与第 2.1/3 节冻结映射一一对应，
 * 规则顺序保证长串先于短串、域名先于包名、大小写变体分别处理。
 */

// 上游署名/素材地址：整体保持原样（docs/06 第 4.1 节）。
export const PROTECTED = [
  /(gitee|github)\.com\/yudaocode\/[A-Za-z0-9._/-]*/g,
  /static\.iocoder\.cn\/[A-Za-z0-9._/-]*/g,
];

// 通用替换规则：[匹配串, 替换串]（字符串全量替换，顺序敏感）。
export const REPLACEMENTS = [
  // 环境变量：先长后短，消除 ZS_APPLICATION_YUDAO_* 双前缀（映射 #7）
  ['ZS_APPLICATION_LOCAL_YUDAO_', 'ZSZJ_APPLICATION_LOCAL_'],
  ['ZS_APPLICATION_YUDAO_', 'ZSZJ_APPLICATION_'],
  ['ZS_', 'ZSZJ_'],
  // 上游演示域名取值中性化（映射 #8）
  ['dashboard-vue3.yudao.iocoder.cn', 'dashboard.zszj.local'],
  ['dashboard-vben.yudao.iocoder.cn', 'dashboard.zszj.local'],
  ['dashboard.yudao.iocoder.cn', 'dashboard.zszj.local'],
  ['test.yudao.iocoder.cn', 'static.zszj.example.com'],
  ['static.yudao.iocoder.cn', 'static.zszj.example.com'],
  // Java 包根与 GroupId（映射 #1/#2）
  ['cn.iocoder.yudao', 'cn.zszj'],
  ['cn/iocoder/yudao', 'cn/zszj'],
  ['cn.iocoder.boot', 'cn.zszj'],
  // 类前缀与大写环境变量残留（映射 #4）
  ['Yudao', 'Zszj'],
  ['YUDAO_', 'ZSZJ_'],
  // 目录/artifact/键名形式（映射 #3/#5）
  ['yudao-', 'zszj-'],
  ['yudao_', 'zszj_'],
  ['yudao.', 'zszj.'],
  // 兜底：yaml 顶层键、标识符（映射 #3/#5）
  ['yudao', 'zszj'],
  ['youdao', 'zszj'],
];

// 产品可见中文品牌串：仅对配置/部署脚本白名单文件生效，避免覆盖注释署名（docs/06 第 4.1 节）。
export const CHINESE_BRAND_REPLACEMENTS = [
  ['芋道管理系统', '众墅之家 AI 赋能平台'],
  ['芋道快速开发平台', '众墅之家 AI 赋能平台'],
  ['芋道源码', '众墅之家'],
  ['芋道相关配置', '众墅之家相关配置'],
  ['芋道配置项', '众墅之家配置项'],
];

// 中文品牌规则适用的文件后缀判断（相对仓库根）。
export function matchesChineseBrandScope(relativePath) {
  return (
    /\/src\/main\/resources\/application[^/]*\.yaml$/.test(relativePath) ||
    /\/src\/main\/resources\/logback-spring\.xml$/.test(relativePath) ||
    /\/script\/(docker|jenkins|shell)\//.test(relativePath) ||
    /\/Dockerfile$/.test(relativePath) ||
    /\/docker\.env$/.test(relativePath)
  );
}

const SENTINEL_START = '\uE000';
const SENTINEL_END = '\uE001';

/** 对文本执行一轮"保护→替换→还原"。chinese 控制是否叠加中文品牌规则。 */
export function applyRules(text, { chinese = false } = {}) {
  const vault = [];
  let out = text;
  for (const pattern of PROTECTED) {
    out = out.replace(pattern, (m) => {
      vault.push(m);
      return `${SENTINEL_START}${vault.length - 1}${SENTINEL_END}`;
    });
  }
  const rules = chinese ? [...REPLACEMENTS, ...CHINESE_BRAND_REPLACEMENTS] : REPLACEMENTS;
  for (const [from, to] of rules) out = out.split(from).join(to);
  out = out.replace(new RegExp(`${SENTINEL_START}(\\d+)${SENTINEL_END}`, 'g'), (_, i) => vault[Number(i)]);
  return out;
}
