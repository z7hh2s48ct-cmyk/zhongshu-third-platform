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
//
// codex r3-P3 加固：函数级检查的「消费」判定过弱——只要函数内任意位置读取过绑定变量的
// .target / .fullPageUrl 即算消费，把 `router.push({ path: tryPostAuth.target })` 替换为
// `window.location.assign(redirect)` 后照过（if 分支的读取仍在场）。升级为**导航实参级**：
// 逐个提取 push / replace / location.assign 的实参文本，实参必须引用解析结果（绑定名、
// 解构名或内联调用）才算消费；直接调用 / 解构 / await 三种形式一并覆盖。

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
    const offenders = LOGIN_FILES.filter((f) => /addRouters\s*\[\s*0\s*\]/.test(f.code)).map(
      (f) => f.rel
    )
    expect(offenders, `以下登录入口仍有 addRouters[0] 兜底: ${offenders.join(', ')}`).toEqual([])
  })

  it('不得对 location.href 做字符串裁剪式整页跳转（绕过消毒结果，且对 percent-encoding 不成立）', () => {
    const offenders = LOGIN_FILES.filter((f) => /location\.href\.replace\s*\(/.test(f.code)).map(
      (f) => f.rel
    )
    expect(offenders, `以下登录入口仍在裁剪 location.href: ${offenders.join(', ')}`).toEqual([])
  })

  it("SSO 回调判定不得用 indexOf('sso') 子串匹配（会把 /system/sso-config 之类站内页误判为回调）", () => {
    const offenders = LOGIN_FILES.filter((f) => /indexOf\(\s*'sso'\s*\)/.test(f.code)).map(
      (f) => f.rel
    )
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

/**
 * 跳过字符串字面量（单/双引号、模板串），返回关闭引号后的下一位置。
 * r4-P3：模板串 `${…}` 插值经 matchDelimiter 递归配平（支持嵌套模板串），
 * 不再把插值内的反引号误当外层闭合（防函数体提取被截断）。
 */
function skipStringLiteral(code: string, start: number): number {
  const quote = code[start]
  let i = start + 1
  while (i < code.length) {
    const c = code[i]
    if (c === '\\') {
      i += 2
      continue
    }
    if (quote === '`' && c === '$' && code[i + 1] === '{') {
      const end = matchDelimiter(code, i + 1, '{', '}')
      if (end === -1) {
        return code.length
      }
      i = end + 1
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
  return matchDelimiter(code, openIndex, '{', '}')
}

/**
 * r3-P3：通用定界符配平（跳过字符串与注释）——openIndex 为开启符位置，
 * 返回匹配关闭符的位置；失败返回 -1。花括号用于函数体提取，圆括号用于导航实参提取。
 */
function matchDelimiter(code: string, openIndex: number, open: string, close: string): number {
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
      const closeIdx = code.indexOf('*/', i + 2)
      if (closeIdx === -1) {
        return -1
      }
      i = closeIdx + 2
      continue
    }
    if (c === open) {
      depth += 1
    } else if (c === close) {
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
 * r4-P3：代码位置掩码——字符串 / 注释覆盖区间的索引标记 false，
 * 供导航调用提取排除字符串文本中的伪调用（如 query: { note: "push(raw)" }）。
 * r6-P3：模板串改为「文本屏蔽、插值保留」——`${…}` 内是真实执行的代码（属性覆盖 /
 * 整体重赋值必须参与失信判定），仅模板字面文本屏蔽；嵌套模板与插值内字符串递归处理。
 */
function maskCodePositions(code: string): boolean[] {
  const mask = new Array<boolean>(code.length).fill(true)
  const blackout = (from: number, to: number) => {
    const end = Math.min(to, code.length)
    for (let k = from; k < end; k++) {
      mask[k] = false
    }
  }
  const scan = (from: number, stop: number) => {
    let i = from
    while (i < stop) {
      const c = code[i]
      if (c === "'" || c === '"') {
        const end = skipStringLiteral(code, i)
        blackout(i, end)
        i = end
        continue
      }
      if (c === '`') {
        mask[i] = false
        let j = i + 1
        while (j < stop) {
          const cj = code[j]
          if (cj === '\\') {
            mask[j] = false
            if (j + 1 < stop) {
              mask[j + 1] = false
            }
            j += 2
            continue
          }
          if (cj === '`') {
            mask[j] = false
            j += 1
            break
          }
          if (cj === '$' && code[j + 1] === '{') {
            const end = matchDelimiter(code, j + 1, '{', '}')
            if (end === -1) {
              blackout(j, stop)
              j = stop
              break
            }
            scan(j + 2, end)
            j = end + 1
            continue
          }
          mask[j] = false
          j += 1
        }
        i = j
        continue
      }
      if (c === '/' && code[i + 1] === '/') {
        const nl = code.indexOf('\n', i)
        const end = nl === -1 || nl >= stop ? stop : nl
        blackout(i, end)
        i = end
        continue
      }
      if (c === '/' && code[i + 1] === '*') {
        const closeIdx = code.indexOf('*/', i + 2)
        const end = closeIdx === -1 ? stop : Math.min(closeIdx + 2, stop)
        blackout(i, end)
        i = end
        continue
      }
      i += 1
    }
  }
  scan(0, code.length)
  return mask
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
const RESOLVE_CALL_RE = /resolvePostAuthRedirect\s*\(/

/** r3-P3：逐个导航调用提取实参文本（lookbehind 不误吃前置字符，定位 '(' 后精确配平） */
const NAV_CALL_RE =
  /(?<![\w.])(?:push|replace)\s*\(|router\s*\.\s*(?:push|replace)\s*\(|location\s*\.\s*assign\s*\(/g

/** r4-P3：导航调用提取结果（实参区间 + 配平状态） */
interface NavCall {
  argStart: number
  argEnd: number
  balanced: boolean
}

/**
 * r4-P3：统一导航调用提取——入口过滤与实参检查共用同一结果（裸 push / 裸 replace /
 * router.push / router.replace / location.assign 同权，覆盖范围一致）；
 * 字符串 / 注释内伪调用（如 query: { note: "push(raw)" }）经掩码排除。
 */
function collectNavCalls(body: string): NavCall[] {
  const mask = maskCodePositions(body)
  const calls: NavCall[] = []
  NAV_CALL_RE.lastIndex = 0
  let m: RegExpExecArray | null
  while ((m = NAV_CALL_RE.exec(body)) !== null) {
    if (!mask[m.index]) {
      continue
    }
    const open = m.index + m[0].length - 1
    const close = matchDelimiter(body, open, '(', ')')
    if (close === -1) {
      calls.push({ argStart: open + 1, argEnd: body.length, balanced: false })
      continue
    }
    calls.push({ argStart: open + 1, argEnd: close, balanced: true })
  }
  return calls
}

/**
 * r5-P3 通用词法工具集（失信判定与导航实参检查共用）：
 *   - splitTopLevel：按顶层定界符深度拆分——解构参数 / 解构条目内的逗号不算分隔
 *     （`({ value: X, unused })` 整体成块，不破坏花括号完整性）；
 *   - findTopLevelEq：顶层赋值等号定位（跳过字符串 / 模板串 / 注释，排除 == / === / =>）；
 *   - bindingName：解构条目绑定名——默认值取 `=` 前、别名取 `:` 后、嵌套取最内层名
 *     （`target` / `target = X` / `target: t` / `target: t = X` / `a: { b: c }`）；
 *   - escapeRegExpName：名字拼入正则前转义全部特殊字符（`$` 不再成为结束锚点）；
 *   - countCodeMatches：仅统计代码位置匹配（字符串 / 注释内伪匹配不计入失信判定）；
 *   - countDeclaredBindings：声明初始化计数（普通声明 + 解构声明条目，防默认值误判重赋值）。
 * r6-P3：拆分与等号定位按词法状态处理——字符串 / 模板串 / 注释中的括号与定界符
 * 不再改变深度（`(x = "{", target)` 曾被 `"{"` 吞掉逗号，遮蔽漏检）。
 */
function splitTopLevel(s: string, sep = ','): string[] {
  const parts: string[] = []
  let depth = 0
  let cur = ''
  let i = 0
  while (i < s.length) {
    const c = s[i]
    if (c === "'" || c === '"' || c === '`') {
      const end = skipStringLiteral(s, i)
      cur += s.slice(i, end)
      i = end
      continue
    }
    if (c === '/' && s[i + 1] === '/') {
      const nl = s.indexOf('\n', i)
      const end = nl === -1 ? s.length : nl
      cur += s.slice(i, end)
      i = end
      continue
    }
    if (c === '/' && s[i + 1] === '*') {
      const closeIdx = s.indexOf('*/', i + 2)
      const end = closeIdx === -1 ? s.length : closeIdx + 2
      cur += s.slice(i, end)
      i = end
      continue
    }
    if (c === '{' || c === '[' || c === '(') {
      depth += 1
    } else if (c === '}' || c === ']' || c === ')') {
      depth = Math.max(0, depth - 1)
    } else if (c === sep && depth === 0) {
      parts.push(cur)
      cur = ''
      i += 1
      continue
    }
    cur += c
    i += 1
  }
  parts.push(cur)
  return parts
}

/** r6-P3：顶层（深度 0）第一个赋值等号；跳过字符串 / 模板串 / 注释，排除 `==` / `===` / `=>` / 关系符 */
function findTopLevelEq(s: string): number {
  let depth = 0
  let i = 0
  while (i < s.length) {
    const c = s[i]
    if (c === "'" || c === '"' || c === '`') {
      i = skipStringLiteral(s, i)
      continue
    }
    if (c === '/' && s[i + 1] === '/') {
      const nl = s.indexOf('\n', i)
      i = nl === -1 ? s.length : nl
      continue
    }
    if (c === '/' && s[i + 1] === '*') {
      const closeIdx = s.indexOf('*/', i + 2)
      i = closeIdx === -1 ? s.length : closeIdx + 2
      continue
    }
    if (c === '{' || c === '[' || c === '(') {
      depth += 1
    } else if (c === '}' || c === ']' || c === ')') {
      depth = Math.max(0, depth - 1)
    } else if (c === '=' && depth === 0) {
      const prev = s[i - 1]
      const next = s[i + 1]
      if (
        next === '=' ||
        next === '>' ||
        prev === '=' ||
        prev === '!' ||
        prev === '<' ||
        prev === '>'
      ) {
        i += 1
        continue
      }
      return i
    }
    i += 1
  }
  return -1
}

/** r7-P3：绑定模式中「代码位置」的嵌套解构花括号区间——跳过字符串键 / 模板串 / 注释中的伪花括号
 *  （`"{ tryPostAuth = 0 }": ignored` 曾被字符串键内的伪默认值误抵扣真实重赋值）；
 *  无代码位置花括号对返回 null。 */
function patternBraceRange(pattern: string): { start: number; end: number } | null {
  let start = -1
  let end = -1
  let i = 0
  while (i < pattern.length) {
    const c = pattern[i]
    if (c === "'" || c === '"' || c === '`') {
      i = skipStringLiteral(pattern, i)
      continue
    }
    if (c === '/' && pattern[i + 1] === '/') {
      const nl = pattern.indexOf('\n', i)
      i = nl === -1 ? pattern.length : nl
      continue
    }
    if (c === '/' && pattern[i + 1] === '*') {
      const closeIdx = pattern.indexOf('*/', i + 2)
      i = closeIdx === -1 ? pattern.length : closeIdx + 2
      continue
    }
    if (c === '{') {
      if (start === -1) {
        start = i
      }
    } else if (c === '}') {
      end = i
    }
    i += 1
  }
  return start !== -1 && end > start ? { start, end } : null
}

/** 解构条目绑定名：`target` / `target = X` / `target: t` / `target: t = X` / `a: { b: c }` 取最内层名
 *  r7-P3：等号定位改词法感知（字符串键内的伪等号不再截断——`"{ a = 0 }": t` 正确取 `t`）。 */
function bindingName(raw: string): string {
  const piece = raw.replace(/^\.\.\./, '').trim()
  if (!piece) {
    return ''
  }
  const eqIdx = findTopLevelEq(piece)
  const head = (eqIdx >= 0 ? piece.slice(0, eqIdx) : piece).trim()
  const colonIdx = head.lastIndexOf(':')
  const tail = (colonIdx >= 0 ? head.slice(colonIdx + 1) : head).trim()
  const m = tail.match(/^\{?\s*([A-Za-z_$][\w$]*)/)
  return m?.[1] ?? ''
}

/** 解构条目绑定名集合（收集嵌套解构的全部绑定）：`{ a: { b, c }, d = X }` → [b, c, d]；
 *  默认值对象（`target = { a: 1 }`）不解构绑定，仅收集 `=` 前的 target。
 *  r6-P3：先按顶层等号分离绑定模式与默认值——默认值对象内部的引用名
 *  （`path = { tryPostAuth }`）不再被递归收集（此前把引用误判为声明）。
 *  r7-P3：嵌套花括号定位同样词法感知——字符串键 / 注释内的伪花括号不产生伪绑定名。 */
function collectBindingNames(raw: string): string[] {
  const out: string[] = []
  const walk = (s: string) => {
    for (const part of splitTopLevel(s)) {
      const piece = part.replace(/^\.\.\./, '').trim()
      if (!piece) {
        continue
      }
      const eqIdx = findTopLevelEq(piece)
      const pattern = (eqIdx >= 0 ? piece.slice(0, eqIdx) : piece).trim()
      if (!pattern) {
        continue
      }
      const braces = patternBraceRange(pattern)
      if (braces) {
        walk(pattern.slice(braces.start + 1, braces.end))
        continue
      }
      const n = bindingName(pattern)
      if (n) {
        out.push(n)
      }
    }
  }
  walk(raw)
  return out
}

/** r5-P3：把名字拼入正则前转义全部特殊字符（`\b` 不适合作含 `$` 标识符的边界） */
function escapeRegExpName(name: string): string {
  return name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

/** 仅统计「代码位置」的匹配次数（正则须带 g；字符串 / 注释内伪匹配经掩码排除） */
function countCodeMatches(body: string, re: RegExp, mask: boolean[]): number {
  let count = 0
  re.lastIndex = 0
  let m: RegExpExecArray | null
  while ((m = re.exec(body)) !== null) {
    if (mask[m.index]) {
      count += 1
    }
    if (m.index === re.lastIndex) {
      re.lastIndex += 1
    }
  }
  return count
}

/** r6-P3：解构条目默认值中「以 name 为直接赋值目标」的数量——
 *  `{ target = X }` / `{ target: t = X }` 的默认值会产生 `target =` / `t =` 文本，按条目精确抵扣；
 *  无默认值条目（`{ target }` / `{ target: t }`）不产生赋值文本，不得抵扣。
 *  r7-P3：嵌套解构花括号定位改用 patternBraceRange 词法感知——字符串键 / 注释中的伪花括号
 *  不再递归出伪默认值（防虚假抵扣掩盖裸重赋值）。 */
function countPatternAssigns(block: string, name: string): number {
  let count = 0
  for (const part of splitTopLevel(block)) {
    const piece = part.replace(/^\.\.\./, '').trim()
    if (!piece) {
      continue
    }
    const eqIdx = findTopLevelEq(piece)
    const pattern = (eqIdx >= 0 ? piece.slice(0, eqIdx) : piece).trim()
    if (!pattern) {
      continue
    }
    const braces = patternBraceRange(pattern)
    if (braces) {
      count += countPatternAssigns(pattern.slice(braces.start + 1, braces.end), name)
      continue
    }
    if (eqIdx >= 0 && bindingName(pattern) === name) {
      count += 1
    }
  }
  return count
}

/** 声明初始化计数：普通 `const X =` + 解构条目默认值（`{ target = X }` / `{ target: t = X }`）——
 *  r6-P3：按条目实际形态抵扣；解构体改用配平扫描（嵌套解构不再被 `[^}]*` 截断），
 *  无默认值条目不再错误抵扣（防「声明数量抵消后续真实重赋值」漏报） */
function countDeclaredBindings(body: string, name: string, mask: boolean[], esc: string): number {
  let count = countCodeMatches(body, new RegExp(`(?:const|let|var)\\s+${esc}\\s*=(?!=)`, 'g'), mask)
  const destructStartRe = /(?<![\w$])(?:const|let|var)\s*\{/g
  destructStartRe.lastIndex = 0
  let m: RegExpExecArray | null
  while ((m = destructStartRe.exec(body)) !== null) {
    if (!mask[m.index]) {
      continue
    }
    const open = m.index + m[0].length - 1
    const close = matchDelimiter(body, open, '{', '}')
    if (close === -1) {
      continue
    }
    count += countPatternAssigns(body.slice(open + 1, close), name)
  }
  return count
}

/**
 * r3-P3：解析结果的可用引用名——绑定 `const X = resolvePostAuthRedirect(...)`（含 await）
 * 与解构 `const { target } = ...`（含别名 `{ target: t }`、默认值 `{ target = X }`）两种形态。
 * r7-P3：解构体改用配平扫描提取（`[^}]*` 曾被字符串键内的伪 `}` 截断，合规代码的引用名
 * 收集失败 → 实参级误判）；条目名解析复用 collectBindingNames 的词法语义逐条对齐。
 * r8-P3：直绑与解构两条路径统一限定代码位置（maskCodePositions）——字符串 / 模板文本 /
 * 注释内的伪声明不产生可信引用（否则 `const note = 'const { … }: redirect } = …'`
 * 使裸 redirect 导航逃过契约检查）；字典键为字符串的解构仍按真实代码位置正常收集。
 */
function collectResolutionNames(body: string): string[] {
  const names: string[] = []
  const mask = maskCodePositions(body)
  const push = (n: string) => {
    if (n && names.indexOf(n) === -1) {
      names.push(n)
    }
  }
  const bindRe =
    /(?:const|let|var)\s+([A-Za-z_$][\w$]*)\s*=\s*(?:await\s+)?resolvePostAuthRedirect\s*\(/g
  let m: RegExpExecArray | null
  while ((m = bindRe.exec(body)) !== null) {
    if (!mask[m.index]) {
      continue
    }
    push(m[1] ?? '')
  }
  const destructStartRe = /(?<![\w$])(?:const|let|var)\s*\{/g
  destructStartRe.lastIndex = 0
  while ((m = destructStartRe.exec(body)) !== null) {
    if (!mask[m.index]) {
      continue
    }
    const open = m.index + m[0].length - 1
    const close = matchDelimiter(body, open, '{', '}')
    if (close === -1) {
      continue
    }
    if (!/^\s*=\s*(?:await\s+)?resolvePostAuthRedirect\s*\(/.test(body.slice(close + 1))) {
      continue
    }
    collectBindingNames(body.slice(open + 1, close)).forEach(push)
  }
  return names
}

/**
 * r4-P3：解析结果绑定的失信判定——以下任一情形后，该名字出现在导航实参中不再视为
 * 「消费了解析结果」：
 *   ① 绑定的 target / fullPageUrl 属性被覆盖赋值（`X.target = redirect`）；
 *   ② 绑定被非声明形式整体重赋值（`X = …`，声明初始化除外）；
 *   ③ 同名标识符出现在嵌套函数/回调的参数列表中（作用域遮蔽，文本层保守判定）。
 * r5-P3 加固（codex 裁决四连）：
 *   - 全部检查限定代码位置（maskCodePositions 掩码）——字符串 / 注释内的伪赋值
 *     （`"X.target = redirect"`）与伪参数（`"(postAuth) => x"`）不触发失信；
 *   - 名字正则统一转义，标识符边界用 `(?<![\w$])` / `(?!...)` 表达——`\b` 对含
 *     `$` 的合法绑定（`$postAuth` / `postAuth$` / `post$Auth`）失效；
 *   - ② 的声明计数纳入解构声明条目——`const { target = HOME_ROUTE } = …` 与别名
 *     `{ target: t = HOME_ROUTE }` 的默认值不再被误判为整体重赋值；
 *   - ③ 的参数列表按顶层逗号拆分——多字段解构 `({ value: X, unused })` 不被切断。
 * r6-P3 加固（codex 裁决四连）：
 *   - ② 声明抵扣按条目实际形态计算——无默认值解构条目不再抵扣（真实重赋值不被抵消）；
 *   - ③ 参数区间按配平提取、拆分跳过字符串 / 模板串 / 注释（`(x = "{", target)` 不再被吞）；
 *   - ① 模板插值保留代码位置（仅屏蔽模板文本），插值内写入参与失信判定；
 *   - collectBindingNames 默认值对象内引用名不再误判为声明（防伪遮蔽误报）。
 */
function isNameTainted(body: string, name: string): boolean {
  const mask = maskCodePositions(body)
  const esc = escapeRegExpName(name)
  // ① 属性覆盖赋值（排除 == / === 比较；字符串 / 注释内伪赋值经掩码排除）
  if (
    countCodeMatches(
      body,
      new RegExp(`(?<![\\w$])${esc}\\s*\\.\\s*(?:target|fullPageUrl)\\s*=(?!=)`, 'g'),
      mask
    ) > 0
  ) {
    return true
  }
  // ② 整体重赋值：声明初始化（含解构条目默认值）之外仍有 `X =` 赋值
  const declCount = countDeclaredBindings(body, name, mask, esc)
  const assignCount = countCodeMatches(body, new RegExp(`(?<![\\w$.])${esc}\\s*=(?!=)`, 'g'), mask)
  if (assignCount > declCount) {
    return true
  }
  // ③ 参数遮蔽：同名标识符出现在嵌套函数/回调（含解构参数 `{ target }`）的参数列表。
  // r6-P3：参数区间按配平提取（默认值里的字符串 / 模板串 / 注释不再截断参数区间），
  // 字符串 / 注释中的伪参数经掩码排除；`(…) =>` 覆盖嵌套回调。
  const paramLists: string[] = []
  const fnRe = /function\s*(?:[A-Za-z_$][\w$]*\s*)?\(/g
  let pm: RegExpExecArray | null
  while ((pm = fnRe.exec(body)) !== null) {
    if (mask[pm.index]) {
      const open = pm.index + pm[0].length - 1
      const close = matchDelimiter(body, open, '(', ')')
      if (close !== -1) {
        paramLists.push(body.slice(open + 1, close))
      }
    }
  }
  for (let i = 0; i < body.length; i++) {
    if (body[i] !== '(' || !mask[i]) {
      continue
    }
    const close = matchDelimiter(body, i, '(', ')')
    if (close === -1) {
      continue
    }
    let j = close + 1
    while (j < body.length) {
      if (/\s/.test(body[j])) {
        j += 1
        continue
      }
      if (body[j] === '/' && body[j + 1] === '*') {
        const end = body.indexOf('*/', j + 2)
        j = end === -1 ? body.length : end + 2
        continue
      }
      if (body[j] === '/' && body[j + 1] === '/') {
        const end = body.indexOf('\n', j)
        j = end === -1 ? body.length : end + 1
        continue
      }
      break
    }
    if (body[j] === '=' && body[j + 1] === '>') {
      paramLists.push(body.slice(i + 1, close))
    }
  }
  const arrowBareRe = /(?:^|[^\w$.])([A-Za-z_$][\w$]*)\s*=>/g
  while ((pm = arrowBareRe.exec(body)) !== null) {
    if (mask[pm.index]) {
      paramLists.push(pm[1] ?? '')
    }
  }
  return paramLists.some((list) =>
    splitTopLevel(list).some((raw) => {
      const piece = raw.replace(/^\.\.\./, '').trim()
      return piece.length > 0 && collectBindingNames(piece).includes(name)
    })
  )
}

/**
 * r3-P3：逐个导航调用验证实参——每个 push / replace / location.assign 的实参文本必须引用
 * 解析结果（绑定/解构名，或内联 resolvePostAuthRedirect 调用），否则解析结果未真正用于导航。
 * 返回未覆盖的实参文本清单（仅用于违规判定，不进错误消息）。
 *
 * r3-P3 教训（codex 变异）：旧检查只看「绑定变量被读取过 .target/.fullPageUrl」——
 * 把 `router.push({ path: tryPostAuth.target })` 替换为 `window.location.assign(redirect)`
 * 后，if 分支里的 tryPostAuth.fullPageUrl 读取仍在场，旧检查全过（漏报）。
 * r4-P3：接收 collectNavCalls 的统一提取结果（入口过滤与实参检查同源）。
 */
function findUncoveredNavArgs(
  navCalls: NavCall[],
  body: string,
  resolutionNames: string[]
): string[] {
  const bad: string[] = []
  navCalls.forEach((call) => {
    if (!call.balanced) {
      bad.push('<unbalanced-nav-call>')
      return
    }
    const argText = body.slice(call.argStart, call.argEnd)
    const byName = resolutionNames.some((n) =>
      new RegExp(`(?<![\\w$])${escapeRegExpName(n)}(?![\\w$])`).test(argText)
    )
    const inline = /resolvePostAuthRedirect\s*\(/.test(argText)
    if (!byName && !inline) {
      bad.push(argText)
    }
  })
  return bad
}

/**
 * 「写 token 且随后导航」的函数级审计（r3-P3 强化为实参级；r4-P3 绑定失信判定）：
 * 命中函数必须
 *   ① 调用 resolvePostAuthRedirect；
 *   ② **每个**导航调用的实参消费解析结果（绑定/解构名或内联调用）——
 *      不再接受「函数内任意位置读取过绑定变量」的弱判定；
 *   ③ 绑定被覆盖赋值或同名遮蔽时不再计入有效引用名（防「名字在场」式漏报）。
 * 入口过滤与实参检查共用 collectNavCalls 的统一提取结果。
 * 返回 `文件#函数名` 违规清单。抽为纯函数以便变异用例验证其有效性。
 */
function findNavigationOffenders(files: LoginSource[]): string[] {
  const offenders: string[] = []
  files.forEach((f) => {
    extractNamedFunctionBodies(f.code).forEach((fn) => {
      const navCalls = collectNavCalls(fn.body)
      if (!SET_TOKEN_RE.test(fn.body) || navCalls.length === 0) {
        return
      }
      const label = `${f.rel}#${fn.name}`
      if (!RESOLVE_CALL_RE.test(fn.body)) {
        offenders.push(label)
        return
      }
      // r4-P3：被覆盖赋值/遮蔽的绑定不再可信（从有效引用名中剔除）
      const names = collectResolutionNames(fn.body).filter((n) => !isNameTainted(fn.body, n))
      if (findUncoveredNavArgs(navCalls, fn.body, names).length > 0) {
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
        if (SET_TOKEN_RE.test(fn.body) && collectNavCalls(fn.body).length > 0) {
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

  it('变异 3（r3-P3 核心）：tryLogin 的 SPA 分支实参被替换为裸 redirect → 「任意位置读取即消费」漏报，实参级必须点名', () => {
    const social = LOGIN_FILES.find((f) => f.rel === 'Login/SocialLogin.vue')
    expect(social, 'SocialLogin.vue 未被扫描到').toBeTruthy()
    const mutated: LoginSource = {
      rel: 'Login/SocialLogin.vue',
      code: (social?.code ?? '').replace(
        'router.push({ path: tryPostAuth.target })',
        'window.location.assign(redirect)'
      )
    }
    // 解析调用仍在场、tryPostAuth.fullPageUrl 仍被 if 读取 —— 旧「任意位置读取即消费」检查在此失效（r3-P3 立论）
    expect(findNavigationOffenders([mutated])).toEqual(['Login/SocialLogin.vue#tryLogin'])
  })

  it('变异 4：handleLogin 的整页跳转实参被替换为裸 redirect → 实参级必须点名 handleLogin', () => {
    const social = LOGIN_FILES.find((f) => f.rel === 'Login/SocialLogin.vue')
    expect(social, 'SocialLogin.vue 未被扫描到').toBeTruthy()
    const mutated: LoginSource = {
      rel: 'Login/SocialLogin.vue',
      code: (social?.code ?? '').replace(
        'window.location.assign(postAuth.fullPageUrl)',
        'window.location.assign(redirect)'
      )
    }
    expect(findNavigationOffenders([mutated])).toEqual(['Login/SocialLogin.vue#handleLogin'])
  })

  it('变异 5（r4-P3）：解析结果属性被覆盖赋值（tryPostAuth.target = redirect）→ 实参级必须点名 tryLogin', () => {
    const social = LOGIN_FILES.find((f) => f.rel === 'Login/SocialLogin.vue')
    expect(social, 'SocialLogin.vue 未被扫描到').toBeTruthy()
    const mutated: LoginSource = {
      rel: 'Login/SocialLogin.vue',
      code: (social?.code ?? '').replace(
        'router.push({ path: tryPostAuth.target })',
        'tryPostAuth.target = redirect\n      router.push({ path: tryPostAuth.target })'
      )
    }
    // 解析调用与名字读取均在场的「名字在场式」变异：仅名字出现不能证明消费未被覆盖的解析结果
    expect(findNavigationOffenders([mutated])).toEqual(['Login/SocialLogin.vue#tryLogin'])
  })

  it('变异 6（r4-P3）：绑定被整体重赋值（postAuth = { target: redirect }）→ 必须点名', () => {
    const code = [
      'const handleLogin = async () => {',
      '  setToken(res)',
      '  const postAuth = resolvePostAuthRedirect(redirect, base)',
      '  postAuth = { target: redirect, fullPageUrl: null }',
      '  push({ path: postAuth.target })',
      '}'
    ].join('\n')
    expect(findNavigationOffenders([{ rel: 'components/Rebind.vue', code }])).toEqual([
      'components/Rebind.vue#handleLogin'
    ])
  })

  it('变异 7（r4-P3）：同名回调参数遮蔽绑定 → 实参不再视为消费解析结果', () => {
    const code = [
      'const handleLogin = async () => {',
      '  setToken(res)',
      '  const postAuth = resolvePostAuthRedirect(redirect, base)',
      '  items.forEach((postAuth) => {',
      '    push({ path: postAuth })',
      '  })',
      '}'
    ].join('\n')
    expect(findNavigationOffenders([{ rel: 'components/Shadow.vue', code }])).toEqual([
      'components/Shadow.vue#handleLogin'
    ])
  })

  it('变异 8（r4-P3）：裸 replace(...) 与 router.push 同权（入口过滤统一）→ 必须点名', () => {
    const code = [
      'const handleLogin = async () => {',
      '  setToken(res)',
      '  const postAuth = resolvePostAuthRedirect(redirect, base)',
      '  replace(redirect)',
      '}'
    ].join('\n')
    expect(findNavigationOffenders([{ rel: 'components/BareReplace.vue', code }])).toEqual([
      'components/BareReplace.vue#handleLogin'
    ])
  })

  it('变异 9（r4-P3）：字符串字面量中的伪 push(...) 不误报（合规实参仍通过）', () => {
    const code = [
      'const handleLogin = async () => {',
      '  setToken(res)',
      '  const postAuth = resolvePostAuthRedirect(redirect, base)',
      '  router.push({ path: postAuth.target, query: { note: "push(raw)" } })',
      '}'
    ].join('\n')
    expect(findNavigationOffenders([{ rel: 'components/QuotedPush.vue', code }])).toEqual([])
  })

  it('变异 10（r4-P3）：模板串嵌套插值不截断函数体，后续裸导航仍检出', () => {
    const code = [
      'const handleLogin = async () => {',
      '  setToken(res)',
      '  const postAuth = resolvePostAuthRedirect(redirect, base)',
      '  const note = `outer${`}`}`',
      '  window.location.assign(redirect)',
      '}'
    ].join('\n')
    expect(findNavigationOffenders([{ rel: 'components/NestedTpl.vue', code }])).toEqual([
      'components/NestedTpl.vue#handleLogin'
    ])
  })

  it('变体覆盖：解构 / 直用（含 await）三种消费形态均判为合规', () => {
    const okCases: string[] = [
      [
        'const handleLogin = async () => {',
        '  setToken(res)',
        '  const { target, fullPageUrl } = resolvePostAuthRedirect(redirect, base)',
        '  if (fullPageUrl) { window.location.assign(fullPageUrl) } else { push({ path: target }) }',
        '}'
      ].join('\n'),
      [
        'const handleLogin = async () => {',
        '  setToken(res)',
        '  if (resolvePostAuthRedirect(redirect, base).fullPageUrl) {',
        '    window.location.assign(resolvePostAuthRedirect(redirect, base).fullPageUrl ?? HOME_ROUTE)',
        '  } else {',
        '    push({ path: resolvePostAuthRedirect(redirect, base).target })',
        '  }',
        '}'
      ].join('\n'),
      [
        'const handleLogin = async () => {',
        '  setToken(res)',
        '  const postAuth = await resolvePostAuthRedirect(redirect, base)',
        '  push({ path: postAuth.target })',
        '}'
      ].join('\n')
    ]
    okCases.forEach((code, i) => {
      expect(
        findNavigationOffenders([{ rel: `components/Ok${i}.vue`, code }]),
        `合规变体 ${i} 被误判`
      ).toEqual([])
    })
  })

  it('变体覆盖：绑定在场但分支导航裸用 redirect（含解构误用）均判违规', () => {
    const badCases: string[] = [
      [
        'const handleLogin = async () => {',
        '  setToken(res)',
        '  const postAuth = resolvePostAuthRedirect(redirect, base)',
        '  if (postAuth.fullPageUrl) { window.location.assign(redirect) } else { push({ path: redirect }) }',
        '}'
      ].join('\n'),
      [
        'const handleLogin = async () => {',
        '  setToken(res)',
        '  const { target } = resolvePostAuthRedirect(redirect, base)',
        '  push({ path: redirect })',
        '}'
      ].join('\n')
    ]
    badCases.forEach((code, i) => {
      expect(
        findNavigationOffenders([{ rel: `components/Bad${i}.vue`, code }]),
        `违规变体 ${i} 漏报`
      ).toEqual([`components/Bad${i}.vue#handleLogin`])
    })
  })

  it('变异 11（r5-P3）：多字段解构参数遮蔽（value: tryPostAuth, unused）→ 实参不再视为消费', () => {
    const code = [
      'const tryLogin = async () => {',
      '  setToken(res)',
      '  const tryPostAuth = resolvePostAuthRedirect(redirect, base)',
      '  ;[{ value: { target: redirect }, unused: 0 }].forEach(({ value: tryPostAuth, unused }) => {',
      '    void unused',
      '    router.push({ path: tryPostAuth.target })',
      '  })',
      '}'
    ].join('\n')
    expect(findNavigationOffenders([{ rel: 'components/DestructShadow.vue', code }])).toEqual([
      'components/DestructShadow.vue#tryLogin'
    ])
  })

  it('变异 12（r5-P3）：解构默认值（含别名）不是整体重赋值 → 合规', () => {
    const okCases: string[] = [
      [
        'const handleLogin = async () => {',
        '  setToken(res)',
        '  const { target = HOME_ROUTE } = resolvePostAuthRedirect(redirect, base)',
        '  push({ path: target })',
        '}'
      ].join('\n'),
      [
        'const handleLogin = async () => {',
        '  setToken(res)',
        '  const { target: t = HOME_ROUTE } = resolvePostAuthRedirect(redirect, base)',
        '  push({ path: t })',
        '}'
      ].join('\n')
    ]
    okCases.forEach((code, i) => {
      expect(
        findNavigationOffenders([{ rel: `components/DestructDefault${i}.vue`, code }]),
        `解构默认值合规变体 ${i} 被误判`
      ).toEqual([])
    })
  })

  it('变异 13（r5-P3）：字符串 / 注释中的伪赋值与伪参数不触发失信 → 合规', () => {
    const code = [
      'const handleLogin = async () => {',
      '  setToken(res)',
      '  const postAuth = resolvePostAuthRedirect(redirect, base)',
      '  const note = "postAuth.target = redirect"',
      '  const doc = "(postAuth) => x"',
      '  // postAuth = redirect',
      '  router.push({ path: postAuth.target })',
      '}'
    ].join('\n')
    expect(findNavigationOffenders([{ rel: 'components/StringText.vue', code }])).toEqual([])
  })

  it('变异 14（r5-P3）：含 $ 的绑定（$postAuth / postAuth$ / post$Auth）正常消费 → 合规', () => {
    ;['$postAuth', 'postAuth$', 'post$Auth'].forEach((n) => {
      const code = [
        'const handleLogin = async () => {',
        '  setToken(res)',
        `  const ${n} = resolvePostAuthRedirect(redirect, base)`,
        `  push({ path: ${n}.target })`,
        '}'
      ].join('\n')
      expect(
        findNavigationOffenders([{ rel: 'components/DollarName.vue', code }]),
        `含 $ 绑定 ${n} 被误判`
      ).toEqual([])
    })
  })

  it('变异 15（r5-P3）：参数默认值数组内的绑定名不产生伪遮蔽（深度拆分判别）→ 合规', () => {
    const code = [
      'const handleLogin = async () => {',
      '  setToken(res)',
      '  const target = resolvePostAuthRedirect(redirect, base)',
      '  items.forEach(({ path = [a, target] }) => {',
      '    console.info(path)',
      '  })',
      '  push({ path: target.target })',
      '}'
    ].join('\n')
    expect(findNavigationOffenders([{ rel: 'components/DefaultComma.vue', code }])).toEqual([])
  })

  it('变异 16（r6-P3）：无默认值解构声明不抵扣后续真实重赋值（含别名）→ 必须点名', () => {
    const cases = [
      [
        'const handleLogin = async () => {',
        '  setToken(res)',
        '  let { target } = resolvePostAuthRedirect(redirect, base)',
        '  target = redirect',
        '  push({ path: target })',
        '}'
      ].join('\n'),
      [
        'const handleLogin = async () => {',
        '  setToken(res)',
        '  let { target: t } = resolvePostAuthRedirect(redirect, base)',
        '  t = redirect',
        '  push({ path: t })',
        '}'
      ].join('\n')
    ]
    cases.forEach((code, i) => {
      expect(
        findNavigationOffenders([{ rel: `components/NoDefault${i}.vue`, code }]),
        `无默认值解构重赋值变体 ${i} 漏报`
      ).toEqual([`components/NoDefault${i}.vue#handleLogin`])
    })
  })

  it('变异 17（r6-P3）：字符串 / 模板串默认值中的括号不吞参数分隔 → 遮蔽仍点名', () => {
    const cases = [
      ';((x = "{", tryPostAuth) => { void x; router.push({ path: tryPostAuth.target }) })(undefined, { target: redirect })',
      ';((x = `{`, tryPostAuth) => { void x; router.push({ path: tryPostAuth.target }) })(undefined, { target: redirect })'
    ]
    cases.forEach((injected, i) => {
      const code = [
        'const tryLogin = async () => {',
        '  setToken(res)',
        '  const tryPostAuth = resolvePostAuthRedirect(redirect, base)',
        `  ${injected}`,
        '}'
      ].join('\n')
      expect(
        findNavigationOffenders([{ rel: `components/StrDefault${i}.vue`, code }]),
        `字符串默认值变体 ${i} 漏报`
      ).toEqual([`components/StrDefault${i}.vue#tryLogin`])
    })
  })

  it('变异 18（r6-P3）：解构默认值对象内的引用名不产生伪遮蔽 → 合规', () => {
    const code = [
      'const handleLogin = async () => {',
      '  setToken(res)',
      '  const tryPostAuth = resolvePostAuthRedirect(redirect, base)',
      '  ;[{}].forEach(({ path = { tryPostAuth } }) => {',
      '    console.info(path)',
      '  })',
      '  push({ path: tryPostAuth.target })',
      '}'
    ].join('\n')
    expect(findNavigationOffenders([{ rel: 'components/DefaultObj.vue', code }])).toEqual([])
  })

  it('变异 19（r6-P3）：模板插值内的属性覆盖 / 整体重赋值参与失信（仅屏蔽模板文本）→ 必须点名', () => {
    const cases = [
      [
        'const handleLogin = async () => {',
        '  setToken(res)',
        '  const tryPostAuth = resolvePostAuthRedirect(redirect, base)',
        '  const note = `${tryPostAuth.target = redirect}`',
        '  push({ path: tryPostAuth.target })',
        '}'
      ].join('\n'),
      [
        'const handleLogin = async () => {',
        '  setToken(res)',
        '  const tryPostAuth = resolvePostAuthRedirect(redirect, base)',
        '  const note = `outer${`${tryPostAuth = redirect}`}`',
        '  push({ path: tryPostAuth.target })',
        '}'
      ].join('\n')
    ]
    cases.forEach((code, i) => {
      expect(
        findNavigationOffenders([{ rel: `components/TplInterp${i}.vue`, code }]),
        `插值内写入变体 ${i} 漏报`
      ).toEqual([`components/TplInterp${i}.vue#handleLogin`])
    })
  })

  it('变异 20（r7-P3）：字符串键内伪默认值不抵扣真实重赋值（真实文件变异）→ 必须点名 tryLogin', () => {
    const social = LOGIN_FILES.find((f) => f.rel === 'Login/SocialLogin.vue')
    expect(social, 'SocialLogin.vue 未被扫描到').toBeTruthy()
    const mutated: LoginSource = {
      rel: 'Login/SocialLogin.vue',
      code: (social?.code ?? '').replace(
        'const tryPostAuth = resolvePostAuthRedirect(redirect, import.meta.env.VITE_BASE_PATH)',
        [
          'let tryPostAuth = resolvePostAuthRedirect(redirect, import.meta.env.VITE_BASE_PATH)',
          'const { "{ tryPostAuth = 0 }": ignored } = {}',
          'void ignored',
          'tryPostAuth = { target: redirect }'
        ].join('\n    ')
      )
    }
    expect(findNavigationOffenders([mutated])).toEqual(['Login/SocialLogin.vue#tryLogin'])
  })

  it('变异 21（r7-P3）：字符串键条目的真实默认值正常抵扣（键内伪等号不截断绑定名）→ 合规', () => {
    const code = [
      'const handleLogin = async () => {',
      '  setToken(res)',
      '  const { "{ tryPostAuth = 0 }": tryPostAuth = HOME_ROUTE } = resolvePostAuthRedirect(redirect, base)',
      '  push({ path: tryPostAuth.target })',
      '}'
    ].join('\n')
    expect(findNavigationOffenders([{ rel: 'components/StringKeyOk.vue', code }])).toEqual([])
  })

  it('变异 22（r7-P3）：注释内伪花括号不抵扣真实重赋值 → 必须点名', () => {
    const code = [
      'const handleLogin = async () => {',
      '  setToken(res)',
      '  let target = resolvePostAuthRedirect(redirect, base)',
      '  const { /* { target = 0 } */ } = {}',
      '  target = redirect',
      '  push({ path: target })',
      '}'
    ].join('\n')
    expect(findNavigationOffenders([{ rel: 'components/CommentBrace.vue', code }])).toEqual([
      'components/CommentBrace.vue#handleLogin'
    ])
  })

  it('变异 23（r8-P3）：字符串内伪解构不产生可信引用（真实文件变异）→ 必须点名 tryLogin', () => {
    const social = LOGIN_FILES.find((f) => f.rel === 'Login/SocialLogin.vue')
    expect(social, 'SocialLogin.vue 未被扫描到').toBeTruthy()
    const mutated: LoginSource = {
      rel: 'Login/SocialLogin.vue',
      code: (social?.code ?? '').replace(
        'router.push({ path: tryPostAuth.target })',
        [
          `const note = 'const { "{ a }": redirect } = resolvePostAuthRedirect(raw, base)'`,
          'window.location.assign(redirect)'
        ].join('\n')
      )
    }
    expect(findNavigationOffenders([mutated])).toEqual(['Login/SocialLogin.vue#tryLogin'])
  })
})
