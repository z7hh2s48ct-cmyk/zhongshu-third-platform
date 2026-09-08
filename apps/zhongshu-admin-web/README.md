# 众墅之家 AI 赋能平台 · Web 管理端（zszj-admin-web）

本目录是众墅之家 AI 赋能平台的 Web 管理端，基于上游 [zszj-ui-admin-vue3](https://github.com/yudaocode/yudao-ui-admin-vue3)（vue-element-plus-admin 体系）迁入并按众墅之家要求改造。品牌与命名映射见 [docs/06-品牌素材与命名映射](../../docs/06-品牌素材与命名映射.md)。

## 技术栈

Vue 3 + Vite + Element Plus + TypeScript + Pinia + Vue Router + VueUse + Vue I18n + UnoCSS + ECharts。

## 快速开始

```bash
# 要求 node >= 20、pnpm >= 9（强制 pnpm）
pnpm install
pnpm dev            # 本地开发（mode: env.local）
pnpm build:prod     # 生产构建
pnpm ts:check       # 类型检查
pnpm lint           # ESLint + Stylelint + Prettier 检查
```

- 后端接口地址、租户开关、验证码开关等通过 `.env*` 文件与 `VITE_*` 环境变量注入；
- 默认登录租户名为"众墅之家"，须与后端种子数据中的租户一致（见 ZS-BRAND-004）；
- 模块启用范围由后端白名单与前端路由守卫共同约束，见底座任务 ZS-ENG-001、ZS-CLIENT-001。

## 品牌与命名

- 产品包名 `zszj-admin-web`；应用标题默认"众墅之家 AI 赋能平台"（`VITE_APP_TITLE`）；
- Logo 与 favicon 由用户提供的原始 Logo 派生（云形徽标方形裁切），派生脚本 `scripts/brand/derive-brand-assets.mjs`，不重绘商标；
- 上游来源注释、issue 引用与 License 按原样保留，不属于品牌清理范围。

## 来源与上游说明

- 上游仓库：[zszj-ui-admin-vue3](https://github.com/yudaocode/yudao-ui-admin-vue3)（基于 [vue-element-plus-admin](https://gitee.com/kailong110120130/vue-element-plus-admin)）
- 上游文档：<https://doc.iocoder.cn/>；上游作者：芋艿（YunaiV）
- 上游采用 MIT License（本地副本见 [LICENSE](LICENSE)），本产品对其的修改遵循同一开源协议要求。
