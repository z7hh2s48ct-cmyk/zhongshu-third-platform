import { describe, expect, it } from 'vitest'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

// ZS-BRAND-003.B（B06 多端可用性联验·登录页品牌残留清理）源码契约测试。
//
// 范围（用户裁定：仅 Tier 1 文案）：登录页左侧副标题 login.message 仍是芋道上游默认标语
//   「开箱即用的中后台管理系统」/「Backstage management system」，属卡片验收明令禁止的
//   「旧品牌回退」；另有 vben 遗留死键 sys.login.signInTitle/signInDesc（全仓无 t() 引用）
//   同样承载该旧标语。本契约把「登录相关多语言文案不得残留上游品牌标语」固化为可执行断言：
//   - 负向：zh-CN.ts / en.ts 全文不得出现任一旧品牌残留短语；
//   - 正向：login.message 必须等于冻结的众墅品牌标语（zh 精确、en 拼音派生）；
//   - 死键：signInTitle / signInDesc 必须从两端语言包移除（消除旧标语的最后栖息地）；
//   - 可见性守卫：Login.vue / SocialLogin.vue 确实引用 t('login.message')（否则改的是死文案）。
//
// 为什么是源码契约而非组件挂载：仓库未引入 @vue/test-utils（见 login-entry-contract.spec.ts 同理），
//   且残留是**静态文案**，fs 读取 + 正则断言即可精确覆盖，无需运行时。
//
// 不动 SVG 背景、不动二维码登录（Tier 2/3 经用户裁定不在本轮范围）。

const HERE = path.dirname(fileURLToPath(import.meta.url))
const LOCALE_DIR = path.resolve(HERE, '../../src/locales')
const LOGIN_DIR = path.resolve(HERE, '../../src/views/Login')

const ZH_PATH = path.join(LOCALE_DIR, 'zh-CN.ts')
const EN_PATH = path.join(LOCALE_DIR, 'en.ts')
const ZH = fs.readFileSync(ZH_PATH, 'utf8')
const EN = fs.readFileSync(EN_PATH, 'utf8')

// 冻结的品牌标语（用户裁定：zh 与 VITE_APP_TITLE 一致）。
// en 系**临时**拼音派生展示名：docs/06 §2（第 26 行）仅登记产品代码主前缀 zszj（及 Zszj/ZSZJ_），
//   §1（第 21 行）仅明确工商全称「待用户提供」——全篇既未展开 zszj 的拼音全称，也未登记任何官方英文名。
//   故 en 串系产品侧按 zszj 前缀临时拼音派生的占位（非官方注册名、非长期契约），
//   仅用于消除上游英文残留；官方英文名确认后应替换（届时同步更新本断言）。
const ZH_BRAND_MESSAGE = '众墅之家 AI 赋能平台'
const EN_BRAND_MESSAGE = 'Zhongshu Zhijia AI Enablement Platform'

// 旧品牌残留短语：芋道默认标语（中英）+ 上游关键词（防御性，出现即视为回退）。
const RESIDUE_PHRASES = [
  '开箱即用的中后台管理系统',
  '开箱即用',
  'Backstage management system',
  '芋道',
  'yudao',
  'iocoder',
  'ruoyi',
  '若依'
]

/** 扫描源码中出现的旧品牌残留短语（按 RESIDUE_PHRASES 顺序返回命中项）。抽为纯函数供变异用例验证。 */
function findResidue(src: string): string[] {
  return RESIDUE_PHRASES.filter((p) => src.includes(p))
}

/**
 * 提取顶层 `login: { … }` 块（2 空格缩进；sys.login 为更深层嵌套，天然排除）。
 * 花括号配平不做字符串跳过——语言包该块内无花括号字符串，足够稳健；
 * 由下方「提取器有效性」用例守卫其非空且含 welcome/message，防静默失配导致空断言通过。
 */
function extractTopLevelLoginBlock(src: string): string {
  const m = /\n {2}login:\s*\{/.exec(src)
  if (!m) {
    return ''
  }
  const open = m.index + m[0].length - 1
  let depth = 0
  for (let i = open; i < src.length; i++) {
    if (src[i] === '{') {
      depth += 1
    } else if (src[i] === '}') {
      depth -= 1
      if (depth === 0) {
        return src.slice(open + 1, i)
      }
    }
  }
  return ''
}

/** 从语言块中取某键的单引号字符串值；不存在返回 null。
 *  左边界 `(^|[\s{,])` 防子串误匹配（如取 `message` 时误命中 `old_message`）。 */
function blockValue(block: string, key: string): string | null {
  const m = new RegExp(`(^|[\\s{,])${key}:\\s*'([^']*)'`, 'm').exec(block)
  return m ? m[2] : null
}

/** 检测某键是否在源码中作为对象键出现（`key:` 形式）。 */
function hasKey(src: string, key: string): boolean {
  return new RegExp(`(^|[\\s{,])${key}\\s*:`, 'm').test(src)
}

const ZH_LOGIN_BLOCK = extractTopLevelLoginBlock(ZH)
const EN_LOGIN_BLOCK = extractTopLevelLoginBlock(EN)

describe('登录页品牌残留契约：多语言文案不得回退上游品牌标语（ZS-BRAND-003.B）', () => {
  it('提取器有效性：zh/en 顶层 login 块非空且含 welcome+message（防正则失配导致空断言通过）', () => {
    for (const [lang, block] of [
      ['zh-CN', ZH_LOGIN_BLOCK],
      ['en', EN_LOGIN_BLOCK]
    ] as const) {
      expect(block.length, `${lang} 顶层 login 块未被提取到`).toBeGreaterThan(0)
      expect(blockValue(block, 'welcome'), `${lang} login.welcome 缺失`).not.toBeNull()
      expect(blockValue(block, 'message'), `${lang} login.message 缺失`).not.toBeNull()
    }
  })

  it('负向：zh-CN.ts 全文无旧品牌残留短语', () => {
    const residue = findResidue(ZH)
    expect(residue, `zh-CN.ts 仍有旧品牌残留: ${residue.join(', ')}`).toEqual([])
  })

  it('负向：en.ts 全文无旧品牌残留短语', () => {
    const residue = findResidue(EN)
    expect(residue, `en.ts 仍有旧品牌残留: ${residue.join(', ')}`).toEqual([])
  })

  it('正向：zh login.message 冻结为「众墅之家 AI 赋能平台」（与 VITE_APP_TITLE 一致）', () => {
    expect(blockValue(ZH_LOGIN_BLOCK, 'message')).toBe(ZH_BRAND_MESSAGE)
  })

  it('正向：en login.message 冻结为拼音派生品牌标语（消除上游英文残留）', () => {
    expect(blockValue(EN_LOGIN_BLOCK, 'message')).toBe(EN_BRAND_MESSAGE)
  })

  it('死键清除：signInTitle / signInDesc 从两端语言包移除（旧标语的最后栖息地）', () => {
    for (const key of ['signInTitle', 'signInDesc']) {
      expect(hasKey(ZH, key), `zh-CN.ts 仍保留死键 ${key}`).toBe(false)
      expect(hasKey(EN, key), `en.ts 仍保留死键 ${key}`).toBe(false)
    }
  })

  it("可见性守卫：Login.vue / SocialLogin.vue 确实渲染 t('login.message')（改的是可见文案而非死文案）", () => {
    for (const rel of ['Login.vue', 'SocialLogin.vue']) {
      const src = fs.readFileSync(path.join(LOGIN_DIR, rel), 'utf8')
      expect(src, `${rel} 未引用 login.message`).toContain("t('login.message')")
    }
  })

  it('检查器有效性（变异）：重新注入旧标语必须被 findResidue 点名', () => {
    expect(findResidue(`message: '开箱即用的中后台管理系统',`)).toContain(
      '开箱即用的中后台管理系统'
    )
    expect(findResidue(`message: 'Backstage management system',`)).toContain(
      'Backstage management system'
    )
    // 合规文案不得误报
    expect(findResidue(`message: '${ZH_BRAND_MESSAGE}',`)).toEqual([])
    expect(findResidue(`message: '${EN_BRAND_MESSAGE}',`)).toEqual([])
  })

  it('检查器有效性（变异）：signInTitle 死键复现必须被 hasKey 点名', () => {
    expect(hasKey(`    login: {\n      signInTitle: 'x',\n    }`, 'signInTitle')).toBe(true)
    expect(hasKey(`    login: {\n      loginButton: 'x',\n    }`, 'signInTitle')).toBe(false)
  })

  it('检查器有效性（变异）：blockValue 左边界防子串误取（old_message 不得被当作 message）', () => {
    // 无左边界时 /message:/ 会误命中 old_message；加边界后仅取真正的 message 键
    expect(blockValue(`old_message: 'bad', message: 'good',`, 'message')).toBe('good')
    expect(blockValue(`old_message: 'bad',`, 'message')).toBeNull()
    // 紧贴 { 或 , 后（无空白）的键仍应被边界 [\s{,] 匹配
    expect(blockValue(`{message: 'good'}`, 'message')).toBe('good')
  })
})
