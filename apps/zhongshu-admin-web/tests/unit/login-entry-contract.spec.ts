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
 * 从 URL / query 读取**登录重定向目的地**的写法。
 * 注意排除 `redirect_uri`：那是 OAuth2 授权服务端同意页（components/SSOLogin.vue）传给后端的
 * 回调地址，由后端按已注册 client 校验，不属前端导航目的地。
 */
const RAW_REDIRECT_RE =
  /query\?\.redirect(?![_A-Za-z0-9])|query\.redirect(?![_A-Za-z0-9])|getUrlValue\(\s*'redirect'\s*\)/
/** 已接入统一消毒 / 落地解析 */
const SANITIZED_RE = /sanitizeLoginRedirect|resolvePostAuthRedirect/
const ACCESS_IMPORT_RE = /from\s+'@\/router\/access'/

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
    const offenders: string[] = []
    LOGIN_FILES.forEach((f) => {
      if (!RAW_REDIRECT_RE.test(f.code)) {
        return
      }
      if (!ACCESS_IMPORT_RE.test(f.code) || !SANITIZED_RE.test(f.code)) {
        offenders.push(f.rel)
      }
    })
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
      if (writesToken && navigates && !/resolvePostAuthRedirect/.test(f.code)) {
        offenders.push(f.rel)
      }
    })
    expect(offenders, `以下登录入口未走统一落地解析: ${offenders.join(', ')}`).toEqual([])
  })
})
