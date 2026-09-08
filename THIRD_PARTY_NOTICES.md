# 第三方来源与许可证

导入日期：2026-09-08。保留完整功能源码不代表默认启用全部模块；本项目不复制来源仓库的 `.git`、工作区密钥、依赖缓存或构建产物。

| 来源 | 固定版本 | 产品内位置 | 许可证 |
|---|---|---|---|
| [RuoYi-Vue-Pro 官方 JDK 17 提交](https://github.com/YunaiV/ruoyi-vue-pro/commit/8e43004cf68a405cd3485f98f8a539b97ca6544a) | `8e43004cf68a405cd3485f98f8a539b97ca6544a` | `services/zhongshu-core`，8,214 文件 | [原始 LICENSE](services/zhongshu-core/LICENSE)，MIT |
| [Yudao Admin Vue3](https://github.com/yudaocode/yudao-ui-admin-vue3) | `a58e6de223b616b9dc14c95551d9d10faf5a280b` | `apps/zhongshu-admin-web`，2,780 文件 | [原始 LICENSE](apps/zhongshu-admin-web/LICENSE)，MIT |
| [Yudao Admin UniApp](https://github.com/yudaocode/yudao-ui-admin-uniapp) | `6929988b78bc00eaab3dff02cd52a463fb1a0467` | `apps/zhongshu-miniapp`，2,711 文件 | [原始 LICENSE](apps/zhongshu-miniapp/LICENSE)，MIT |
| 众墅设计专项本地供体 | `2bf21e528e8a73481c98d35a061eae60ad897f02` 相对上述官方基线的差异 | `reference/donors/zhongshu-design`，234 文件 | [保留的上游 LICENSE](reference/donors/zhongshu-design/LICENSE)；新增专项内容归属须随项目授权管理 |

## 追溯与差异

- [完整来源清单](third_party/source-copy-manifest.json)：逐文件记录来源 ID、原路径、固定 SHA、Git tree、文件模式、原始 blob 和导入 blob。官方对象取自同源供体保留的 Git 历史并核对官方提交，不将供体分支 HEAD 冒充官方版本。
- [安全净化记录](third_party/source-sanitization.json)：25 个文件清除演示凭据；原始来源仓库保持不变，未测试凭据有效性、未调用或轮换第三方密钥。
- 专项快照仅导出相对官方基线新增/修改的文件，另保留 LICENSE；排除供体根 `.gitignore` 和 `架构分析.md`。这不是第二份可独立运行后端，也不是全部供体 Git 历史。完整供体仍在原目录保留。
- Web/Mini 的已跟踪 `.env*` 是净化后的上游模板，不是私密环境；不得写真实密码、商户密钥、模型密钥或发布证书。
- 上游字体、图片、插件、依赖和新增专项代码的权属不因根 MIT 许可证自动统一。当前完成根许可证保留与版本追溯；完整 SBOM、依赖安全和素材逐项许可核验仍是发布前门禁。

重新分发时必须保留各来源要求的版权和许可证；不要仅凭本文将所有第三方内容认定为同一授权。
