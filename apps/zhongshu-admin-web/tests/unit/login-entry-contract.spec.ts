import { describe, expect, it } from 'vitest'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

// ZS-CLIENT-001.A：登录入口「重定向目的地校验」源码契约测试。
//
// 为什么是源码契约而不是组件挂载测试：仓库未引入 @vue/test-utils（卡片交付要求①），
// 无法挂载 LoginForm / MobileForm 之类组件；但卡片「调整」明确要求**校验登录重定向目的地**，
// 而登录入口是分散在多个 .vue 里的重复代码——纯函数 `sanitizeLoginRedirect` /
// `resolvePostAuthRedirect` 的行为已由 router-access.spec.ts 覆盖，**剩下的风险是「某个入口忘记调用」**。
// 本测试把「所有登录入口必须共用同一消毒函数」固化为可执行契约：
// 新增登录入口若绕过消毒，这里会直接失败（防开放重定向回归）。
//
// 首轮实跑即抓到 codex r0 未点名的两处：
//   - MobileForm.vue（短信登录）：`redirect.value = route?.query?.redirect as string` 未消毒 +
//     `push({ path: redirect.value || permissionStore.addRouters[0].path })` 死代码兜底；
//   - ForgetPasswordForm.vue：同样的未消毒赋值（虽不用于导航，属失效残留状态）。
//
// codex r2-P3 加固：文件级检查在「同文件多登录入口、仅变异其一」时失效——SocialLogin.vue
// 同时有 tryLogin 与 handleLogin。新增函数级契约：凡「写 token 且随后导航」的具名处理函数
// 必须调用 resolvePostAuthRedirect **并消费其返回值**（.target / .fullPageUrl），并对
// tryLogin / handleLogin 分别执行「解析调用被替换为裸 redirect」的变异测试，每个变异必须被点名。

const HERE = path.dirname(fileURLToPath(import.meta.url))
const LOGIN_DIR = path.resolve(HERE, '../../src/views/Login')

function walk(dir: string, out: string[] = []): string[] {
  fs.readdirSync(dir, { withFileTypes: true }).forEach((e) => {
    const p = path.join(dir, e.name)
    if (e.isDirectory()) {
      walk(p, out)
    } else if (e.name.endsWith('.vue')) {
      out.push(p)
    }
  })
  return out
}

/** 去掉块注释 / 行注释 / HTML 注释，避免把「说明为什么不再这么写」的注释误判为违规代码 */
function stripComments(src: string): string {
  return src
    .replace(/<!--[\s\S]*?-->/g, '')
    .replace(/\/\*[\s\S]*?\*\//g, '')
    .replace(/(^|[^:'"`\\])\/\/[^\n]*/g, '$1')
}

const LOGIN_FILES = walk(LOGIN_DIR).map((f) => ({
  rel: f.split(path.sep).slice(-2).join('/'),
  full: f,
  raw: fs.readFileSync(f, 'utf8'),
  code: stripComments(fs.readFileSync(f, 'utf8'))
}))

/**
 * 从 URL / query 读取**登录重定向目的地**的写法（r1-P3 加固：覆盖引号变体 / 下标取值 / URLSearchParams）。
 * 注意排除 `redirect_uri`：那是 OAuth2 授权服务端同意页（components/SSOLogin.vue）传给后端的
 * 回调地址，由后端按已注册 client 校验，不属前端导航目的地。
 */
const RAW_REDIRECT_RE =
  /query\?\.redirect(?![_A-Za-z0-9])|query\.redirect(?![_A-Za-z0-9])|query\s*\[\s*['"]redirect['"]\s*\]|getUrlValue\(\s*['"]redirect['"]\s*\)|URLSearchParams[\s\S]{0,120}?\.get\(\s*['"]redirect['"]\s*\)/
/** 已接入统一消毒 / 落地解析——必须是**调用**，仅 import 函数名不算（r1-P3：防「删调用留 import」绕过） */
const SANITIZED_RE = /\bsanitizeLoginRedirect\s*\(|\bresolvePostAuthRedirect\s*\(/
const ACCESS_IMPORT_RE = /from\s+'@\/router\/access'/

interface LoginSource {
  rel: string
  code: string
}

/** 审计入口源码：读取 redirect 却未接入消毒 / 落地解析的文件清单。抽为纯函数以便变异用例验证其有效性。 */
function findRawRedirectOffenders(files: LoginSource[]): string[] {
  const offenders: string[] = []
  files.forEach((f) => {
    if (!RAW_REDIRECT_RE.test(f.code)) {
      return
    }
    if (!ACCESS_IMPORT_RE.test(f.code) || !SANITIZED_RE.test(f.code)) {
      offenders.push(f.rel)
    }
  })
  return offenders
}

describe('登录入口重定向契约：全部入口共用同一消毒函数（防开放重定向）', () => {
  it('扫描范围非空且覆盖已知登录入口（防止 glob 静默失配导致空断言通过）', () => {
    expect(LOGIN_FILES.length).toBeGreaterThanOrEqual(6)
    const names = LOGIN_FILES.map((f) => f.rel)
    for (const expected of [
      'components/LoginForm.vue',
      'components/MobileForm.vue',
      'components/RegisterForm.vue',
      'components/ForgetPasswordForm.vue',
      'Login/SocialLogin.vue'
    ]) {
      expect(names, `登录入口未被扫描到: ${expected}`).toContain(expected)
    }
  })

  it('读取 redirect 的入口必须导入并使用 @/router/access 的消毒函数（不得裸用 URL 值）', () => {
    const offenders = findRawRedirectOffenders(LOGIN_FILES)
    expect(offenders, `以下登录入口未接入重定向消毒: ${offenders.join(', ')}`).toEqual([])
  })

  it('不得使用 permissionStore.addRouters[0].path 之类兜底（登录成功时动态路由尚未装配，一旦生效即 TypeError）', () => {
    const offenders = LOGIN_FILES.filter((f) => /addRouters\s*\[\s*0\s*\]/.test(f.code)).map((f) => f.rel)
    expect(offenders, `以下登录入口仍有 addRouters[0] 兜底: ${offenders.join(', ')}`).toEqual([])
  })

  it('不得对 location.href 做字符串裁剪式整页跳转（绕过消毒结果，且对 percent-encoding 不成立）', () => {
    const offenders = LOGIN_FILES.filter((f) => /location\.href\.replace\s*\(/.test(f.code)).map((f) => f.rel)
    expect(offenders, `以下登录入口仍在裁剪 location.href: ${offenders.join(', ')}`).toEqual([])
  })

  it("SSO 回调判定不得用 indexOf('sso') 子串匹配（会把 /system/sso-config 之类站内页误判为回调）", () => {
    const offenders = LOGIN_FILES.filter((f) => /indexOf\(\s*'sso'\s*\)/.test(f.code)).map((f) => f.rel)
    expect(offenders, `以下登录入口仍用 sso 子串匹配: ${offenders.join(', ')}`).toEqual([])
  })

  it('登录成功后的落地必须经 resolvePostAuthRedirect（唯一整页跳转 / SPA 落地判定出口）', () => {
    // 凡是写入访问凭据（setToken）并随后导航的入口，都必须走统一落地解析
    const offenders: string[] = []
    LOGIN_FILES.forEach((f) => {
      const writesToken = /setToken\s*\(/.test(f.code)
      const navigates = /\bpush\s*\(|\breplace\s*\(|location\.assign\s*\(/.test(f.code)
      if (writesToken && navigates && !/\bresolvePostAuthRedirect\s*\(/.test(f.code)) {
        offenders.push(f.rel)
      }
    })
    expect(offenders, `以下登录入口未走统一落地解析: ${offenders.join(', ')}`).toEqual([])
  })

  it('检查器有效性（变异用例）：删除消毒调用、仅保留 import 时，必须判定为违规', () => {
    const intact: LoginSource = {
      rel: 'components/MutationSample.vue',
      code: [
        "import { sanitizeLoginRedirect, resolvePostAuthRedirect } from '@/router/access'",
        'redirect.value = sanitizeLoginRedirect(route?.query?.redirect, HOME_ROUTE)',
        'const postAuth = resolvePostAuthRedirect(redirect.value, import.meta.env.VITE_BASE_PATH)'
      ].join('\n')
    }
    // 原样（调用在场）应通过
    expect(findRawRedirectOffenders([intact])).toEqual([])
    // 删掉两处调用、仅留 import：RAW_REDIRECT_RE 仍命中 redirect 读取，SANITIZED_RE 不得被 import 文本满足
    const mutated: LoginSource = {
      rel: intact.rel,
      code: intact.code
        .replace(
          'redirect.value = sanitizeLoginRedirect(route?.query?.redirect, HOME_ROUTE)',
          'redirect.value = route?.query?.redirect'
        )
        .replace(
          'const postAuth = resolvePostAuthRedirect(redirect.value, import.meta.env.VITE_BASE_PATH)',
          'const postAuth = redirect.value'
        )
    }
    expect(findRawRedirectOffenders([mutated])).toEqual(['components/MutationSample.vue'])
  })

  it('检查器有效性（变体覆盖）：双引号 getUrlValue / query 下标 / URLSearchParams 均须被识别', () => {
    for (const variant of [
      'getUrlValue("redirect")',
      'route.query["redirect"]',
      "new URLSearchParams(location.search).get('redirect')"
    ]) {
      expect(RAW_REDIRECT_RE.test(variant), `未识别 redirect 读取变体: ${variant}`).toBe(true)
    }
  })
})

// ---------------------------------------------------------------------------
// codex r2-P3：函数级契约升级
//   文件级检查（上方 describe）只看「整文件是否存在调用」；当同一文件有多个登录入口、
//   只变异其中一个时全部照过。本段把粒度降到「处理函数」：写 token 且随后导航的函数，
//   必须调用 resolvePostAuthRedirect 并消费其返回值。
// ---------------------------------------------------------------------------

interface NamedFunctionBody {
  name: string
  body: string
}

/** 跳过字符串字面量（单/双引号、模板串），返回关闭引号后的下一位置 */
function skipStringLiteral(code: string, start: number): number {
  const quote = code[start]
  let i = start + 1
  while (i < code.length) {
    const c = code[i]
    if (c === '\\') {
      i += 2
      continue
    }
    if (c === quote) {
      return i + 1
    }
    i += 1
  }
  return code.length
}

/** 从 openIndex（'{' 位置）做花括号配平（跳过字符串与注释），返回匹配 '}' 的位置；失败返回 -1 */
function matchBrace(code: string, openIndex: number): number {
  let depth = 0
  let i = openIndex
  while (i < code.length) {
    const c = code[i]
    if (c === "'" || c === '"' || c === '`') {
      i = skipStringLiteral(code, i)
      continue
    }
    if (c === '/' && code[i + 1] === '/') {
      const nl = code.indexOf('\n', i)
      if (nl === -1) {
        return -1
      }
      i = nl + 1
      continue
    }
    if (c === '/' && code[i + 1] === '*') {
      const close = code.indexOf('*/', i + 2)
      if (close === -1) {
        return -1
      }
      i = close + 2
      continue
    }
    if (c === '{') {
      depth += 1
    } else if (c === '}') {
      depth -= 1
      if (depth === 0) {
        return i
      }
    }
    i += 1
  }
  return -1
}

/**
 * 提取具名函数体：function 声明（含返回类型）/ function 表达式 / 具名箭头函数赋值。
 * 粒度=最外层具名函数：入口把 setToken 写进 `.then(async (res) => …)` 内层回调也没关系，
 * 内层回调仍落在外层具名函数体内，契约按外层函数断言（如 MobileForm.vue 的 signIn）。
 */
function extractNamedFunctionBodies(code: string): NamedFunctionBody[] {
  const out: NamedFunctionBody[] = []
  const pattern =
    /function\s+([A-Za-z_$][\w$]*)\s*\([^)]*\)\s*(?::\s*[^({;]+)?\{|(?:const|let|var)\s+([A-Za-z_$][\w$]*)\s*=\s*(?:async\s+)?(?:function\s*\([^)]*\)|\([^)]*\)\s*=>)\s*\{/g
  let m: RegExpExecArray | null
  while ((m = pattern.exec(code)) !== null) {
    const name = m[1] ?? m[2] ?? '<anonymous>'
    const open = pattern.lastIndex - 1
    const end = matchBrace(code, open)
    if (end !== -1) {
      out.push({ name, body: code.slice(open + 1, end) })
    }
  }
  return out
}

/** 「写访问凭据」与「随后导航」的判定（函数级审计用） */
const SET_TOKEN_RE = /setToken\s*\(/
const NAVIGATE_RE = /(?:^|[^.\w])push\s*\(|router\.push\s*\(|router\.replace\s*\(|location\.assign\s*\(/
const RESOLVE_CALL_RE = /resolvePostAuthRedirect\s*\(/
const RESOLVE_BIND_RE =
  /(?:const|let|var)\s+([A-Za-z_$][\w$]*)\s*=\s*(?:await\s+)?resolvePostAuthRedirect\s*\(/

/**
 * 「写 token 且随后导航」的函数级审计：命中函数必须
 *   ① 调用 resolvePostAuthRedirect；
 *   ② 消费其返回值（绑定变量被读取 .target / .fullPageUrl，或直接/解构消费）。
 * 返回 `文件#函数名` 违规清单。抽为纯函数以便变异用例验证其有效性。
 */
function findNavigationOffenders(files: LoginSource[]): string[] {
  const offenders: string[] = []
  files.forEach((f) => {
    extractNamedFunctionBodies(f.code).forEach((fn) => {
      if (!SET_TOKEN_RE.test(fn.body) || !NAVIGATE_RE.test(fn.body)) {
        return
      }
      const label = `${f.rel}#${fn.name}`
      if (!RESOLVE_CALL_RE.test(fn.body)) {
        offenders.push(label)
        return
      }
      const bind = fn.body.match(RESOLVE_BIND_RE)
      if (bind) {
        const consumed = new RegExp(`\\b${bind[1]}\\.(?:target|fullPageUrl)\\b`).test(fn.body)
        if (!consumed) {
          offenders.push(label)
        }
        return
      }
      // 无绑定变量：允许「调用结果直接取属性」或「解构消费」两种直用形式
      const inlineConsumed =
        /resolvePostAuthRedirect\s*\([^;]*\)\s*\.\s*(?:target|fullPageUrl)\b|\{\s*[^}]*\b(?:target|fullPageUrl)\b[^}]*\}\s*=\s*(?:await\s+)?resolvePostAuthRedirect\s*\(/.test(
          fn.body
        )
      if (!inlineConsumed) {
        offenders.push(label)
      }
    })
  })
  return offenders
}

describe('登录落地解析契约（函数级 + 变异，codex r2-P3）', () => {
  it('提取器有效性：SocialLogin 必须抽出 tryLogin / handleLogin（防正则失配导致空断言通过）', () => {
    const social = LOGIN_FILES.find((f) => f.rel === 'Login/SocialLogin.vue')
    expect(social, 'SocialLogin.vue 未被扫描到').toBeTruthy()
    const names = extractNamedFunctionBodies(social?.code ?? '').map((fn) => fn.name)
    expect(names).toContain('tryLogin')
    expect(names).toContain('handleLogin')
  })

  it('覆盖度守卫：至少 5 个「写 token 且导航」的处理函数被纳入审计（LoginForm / MobileForm / RegisterForm / SocialLogin×2）', () => {
    const units: string[] = []
    LOGIN_FILES.forEach((f) => {
      extractNamedFunctionBodies(f.code).forEach((fn) => {
        if (SET_TOKEN_RE.test(fn.body) && NAVIGATE_RE.test(fn.body)) {
          units.push(`${f.rel}#${fn.name}`)
        }
      })
    })
    expect(units.length).toBeGreaterThanOrEqual(5)
    expect(units).toContain('Login/SocialLogin.vue#tryLogin')
    expect(units).toContain('Login/SocialLogin.vue#handleLogin')
  })

  it('实跑：全部处理函数均调用并消费 resolvePostAuthRedirect 结果', () => {
    const offenders = findNavigationOffenders(LOGIN_FILES)
    expect(offenders, `以下登录处理函数未走统一落地解析: ${offenders.join(', ')}`).toEqual([])
  })

  it('变异 1（r2 原例）：tryLogin 的解析调用被替换为裸 redirect → 文件级检查漏报、函数级必须点名 tryLogin', () => {
    const social = LOGIN_FILES.find((f) => f.rel === 'Login/SocialLogin.vue')
    expect(social, 'SocialLogin.vue 未被扫描到').toBeTruthy()
    const mutated: LoginSource = {
      rel: 'Login/SocialLogin.vue',
      code: (social?.code ?? '').replace(
        'const tryPostAuth = resolvePostAuthRedirect(redirect, import.meta.env.VITE_BASE_PATH)',
        'const tryPostAuth = { target: redirect, fullPageUrl: null }'
      )
    }
    // 旧文件级检查在此场景失效（handleLogin 的调用仍在场，断言全过）——r2-P3 的立论
    expect(findRawRedirectOffenders([mutated])).toEqual([])
    expect(findNavigationOffenders([mutated])).toEqual(['Login/SocialLogin.vue#tryLogin'])
  })

  it('变异 2：handleLogin 的解析调用被替换为裸 redirect → 函数级必须点名 handleLogin', () => {
    const social = LOGIN_FILES.find((f) => f.rel === 'Login/SocialLogin.vue')
    expect(social, 'SocialLogin.vue 未被扫描到').toBeTruthy()
    const mutated: LoginSource = {
      rel: 'Login/SocialLogin.vue',
      code: (social?.code ?? '').replace(
        'const postAuth = resolvePostAuthRedirect(redirect, import.meta.env.VITE_BASE_PATH)',
        'const postAuth = { target: redirect, fullPageUrl: null }'
      )
    }
    expect(findNavigationOffenders([mutated])).toEqual(['Login/SocialLogin.vue#handleLogin'])
  })
})
