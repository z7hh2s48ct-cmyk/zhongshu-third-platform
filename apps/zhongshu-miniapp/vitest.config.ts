import { fileURLToPath } from 'node:url'
import { defineConfig } from 'vitest/config'

// ZS-CLIENT-003：移动端请求层单元测试配置。
// 独立于 vite.config.ts（不加载 uni-app 插件），仅提供 @ 别名与测试环境；
// 被测代码通过 mock 切断 uni-app 运行时依赖，故使用 node 环境即可。
export default defineConfig({
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  test: {
    environment: 'node',
    include: ['tests/unit/**/*.spec.ts'],
    setupFiles: ['./tests/unit/setup.ts'],
    // interceptor.ts 在模块加载期读取这些开关，测试中固定其取值以获得确定性行为
    env: {
      VITE_APP_TENANT_ENABLE: 'true',
      VITE_APP_TENANT_VISIT_ENABLE: 'false',
      VITE_APP_PROXY_ENABLE: 'false',
      VITE_APP_PROXY_PREFIX: '/api',
      VITE_SERVER_BASEURL: 'https://api.zszj.test',
      VITE_AUTH_MODE: 'double',
      VITE_APP_API_ENCRYPT_HEADER: 'X-Api-Encrypt',
    },
  },
})
