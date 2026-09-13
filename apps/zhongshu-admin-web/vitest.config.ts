import { fileURLToPath } from 'node:url'
import { defineConfig } from 'vitest/config'

// ZS-CLIENT-003：Web 管理端请求层单元测试配置。
// 独立于 vite.config.ts（不加载业务插件），仅提供 @ 别名与测试环境；
// service.ts 的外部依赖（config / auth / element-plus / router 等）在用例内 mock。
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
    env: {
      VITE_APP_TENANT_ENABLE: 'true',
      VITE_APP_TENANT_VISIT_ENABLE: 'false',
      VITE_BASE_URL: 'https://api.zszj.test',
      VITE_API_URL: '/admin-api',
    },
  },
})
