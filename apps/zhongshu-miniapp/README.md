# 众墅之家 AI 赋能平台 · 移动端（zszj-miniapp）

本目录是众墅之家 AI 赋能平台的移动端工程（微信小程序 / H5 / App 多端），基于上游 [yudao-ui-admin-uniapp](https://github.com/yudaocode/yudao-ui-admin-uniapp)（uni-app + Vue3 + unibest 工程化模板）迁入并按众墅之家要求改造。品牌与命名映射见 [docs/06-品牌素材与命名映射](../../docs/06-品牌素材与命名映射.md)。

## 技术栈

uni-app + Vue 3 + Vite + TypeScript + Pinia + UnoCSS + wot-design-uni；组件库前缀 `yd-*` 为登记保留的兼容缩写（docs/06 第 4.3 节）。

## 快速开始

```bash
# 要求 node >= 20、pnpm >= 9（强制 pnpm）
pnpm install
pnpm dev:h5        # H5 开发
pnpm dev:mp        # 微信小程序开发（需在开发者工具导入 dist/dev/mp-weixin）
pnpm build:h5      # H5 生产构建
pnpm build:mp      # 微信小程序生产构建
```

- 应用名称/接口地址等通过 `env/.env*` 的 `VITE_*` 变量注入，默认应用名"众墅之家"；
- 小程序 AppID、App 图标与上架信息不随展示名自动变更（docs/06 第 5 节外部标识排除）。

## 品牌与命名

- 产品包名 `zszj-miniapp`；组件库目录 `src/components/zszj-ui`；
- Logo 与 App 图标由用户提供的原始 Logo 派生（云形徽标方形裁切/等比缩放），派生脚本 `scripts/brand/derive-brand-assets.mjs`，不重绘商标；
- 上游来源注释、issue 引用、License 与 unibest 上游元数据字段按原样保留。

## 来源与上游说明

- 上游仓库：[yudao-ui-admin-uniapp](https://github.com/yudaocode/yudao-ui-admin-uniapp)；工程化模板：[unibest](https://unibest.tech)
- 上游作者：feige996（unibest）、芋艿（YunaiV）；上游采用 MIT License（本地副本见 [LICENSE](LICENSE)）
- 本产品对其的修改遵循同一开源协议要求。
