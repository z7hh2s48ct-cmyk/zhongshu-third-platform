// ZS-CLIENT-003：Web 管理端单元测试全局 setup。
//
// service.ts 依赖若干由 unplugin-auto-import 注入的全局与浏览器对象；
// 在 node 测试环境提供最小桩，使请求/响应拦截器可被加载与调用。

// useI18n 为自动导入的全局；响应拦截器在成功/错误路径会调用 t()
;(globalThis as any).useI18n = () => ({ t: (key: string) => key })

// base_url 为绝对地址时凭据来源判定不依赖 window；仍提供最小 window 以兼容
// handleAuthorized 等分支（window.location.href/origin）。
if (typeof (globalThis as any).window === 'undefined') {
  ;(globalThis as any).window = {
    location: { origin: 'https://api.zszj.test', href: 'https://api.zszj.test/' },
  }
}

export {}
