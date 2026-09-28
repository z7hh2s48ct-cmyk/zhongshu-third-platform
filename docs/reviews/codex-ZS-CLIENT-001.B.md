# ZS-CLIENT-001.B 评审处置（Web 消费卡：CodeReview 独立复审 R1 0×P0/P1 + 1×P2 + 6×P3 → 处置 1e0ccf99）

- 评审对象：ZS-CLIENT-001.B Web 消费业务动作与字段授权（B08 Wave4 真正收官卡；§16.1 line1060 子项，父卡 ZS-CLIENT-001；前置 ZS-CLIENT-001.A〔Web 授权导航与清理合同〕+ ZS-PERM-003〔.A 机制 + .B 字段等级目录〕均已交付）
- 隔离分支：worktree `.wt/zszj-wt-client-001-b` 分支 `feat/client-001-b`（自 main `40e86e84`；RED `22161bd2`〔3 files +371〕→ GREEN `aad9b045`〔7 files +271/-2〕→ R1 处置 `1e0ccf99`〔5 files +44/-23〕）
- 卡片性质：**消费接线卡**——服务端对象授权输出端点（前端消费统一裁决的唯一入口）+ Web 消费层（纯逻辑核心 / store / 清理接线）；验收锚点「旧页签不泄露」「前端伪造动作不生效」
- 评审工具：codex 账号用量上限阻断（承 PERM-002.B 起 13 卡先例），改以 **CodeReview 独立复审替代**（R1 → 处置 → R2 两轮）
- 结论：**R1 0×P0/P1 + 1×P2 + 6×P3（零输出语义/封装归一/键语义/401 路径/会话内撤权/masked-hidden 合同/唯一写路径/清理接线/清单基线九域判 clean）→ P2-1 修复 + P3-1~5 修复 + P3-6 登记不改 `1e0ccf99` → R2 增量复审 PASS / 0 发现**

## R1 findings 与处置（`1e0ccf99`）

| 级别 | 编号 | 缺陷 | 处置（`1e0ccf99`） |
|---|---|---|---|
| P2 | P2-1 | **在途 fetch 回写竞态**：`clearObjectAuthorizationState()` 置空 `snapshotMap` 后，在途 fetch 的 await 续体仍对新映射执行写入——上一主体/上一租户的授权快照跨清理存活，同窗口下一次登录免请求复用（logout 时恰有页面 fetch 在途的合理序列），直接抵触「旧页签不泄露」锚点；影响面有界（动作点击与字段值仍由服务端独立裁决/脱敏，泄漏的是 UI 供能面），故 P2 非 P1 | store 引入 `clearEpoch` 清理代数：清理自增，fetch 在 await 前捕获代数、响应返回后代数不一致即丢弃（不回写不返回）；新增在途竞态回归用例（在途→清理→响应到达→快照为 null、映射为空、下次 fetch 重请求） |
| P3 | P3-1 | Controller javadoc「不泄露域注册状态」过度宣称：未接入域 success(null) 与已注册但对象不可见 FORBIDDEN 的对照可间接推断 objectType 注册状态（输出恒为调用者自身授权，枚举面=客户端包内代码常量，危害低） | javadoc 与测试类注释订正为准确表述 |
| P3 | P3-2 | `useObjectAuthorizationStoreWithOut` 未真正循其所引 dict store 先例（dict 显式传全局 pinia，本实现依赖 active pinia——安装前调用会抛 getActivePinia 错误，潜在 axios 拦截器类调用方） | 显式传 `store`（'@/store'），循 dict 先例；object-authorization.spec.ts 补 '@/store' 轻量 mock（循 authorization-snapshot.spec 先例） |
| P3 | P3-3 | swagger `objectType required=true` 与实际零输出合同（@RequestParam required=false + blank 检查）不符 | 注解改 false + 描述订正（零输出语义为有意设计，改注解为小改） |
| P3 | P3-4 | `ObjectAuthorizationRespVO` 在 api 与 utils 双份同名同构声明（合同演进会静默漂移） | 唯一声明收敛至 utils 消费核心，api 模块 import + re-export；`getObjectAuthorization` 返回类型显式注解 |
| P3 | P3-5 | ID 参数 `number` 类型偏离代码库 string-ID 惯例（服务端 Long 超 2^53 经 NumberSerializer 字符串序列化；现 id-type=NONE 小 ID 暂安全，属精度陷阱） | api/store/键函数三处放宽 `number \| string`（模板串同形），登记 NumberSerializer 惯例 |
| P3 | P3-6 | fetch 失败不缓存不去重：对象维不可见 FORBIDDEN 场景每次重渲染重请求并弹全局错误提示（现无页面接线，属潜在） | **登记不改**：不缓存失败=瞬态网络错误不得固化为「未接入域」（语义正确性优先）；列表逐行场景的静默由页面接线期决策（store javadoc 登记合同）；另订正测试方法名笔误 getObjecAuthorization |

## R2 复审（增量 `aad9b045..1e0ccf99`）

**PASS / 0×P0~P3**：P2 竞态确被代数守卫闭合（含双在途 fetch 跨清理交错、清理后合法 fetch 不被误弃两边界）；处置无新缺陷引入；回归用例对「代数守卫被移除」具有检出力。

## 验证（交付时点证据）

- **RED 双证**：Web `Cannot find package '@/utils/objectAuthorization'`（13+ 例红）+ auth-session 3 新断言红；服务端编译红 找不到符号 `ObjectAuthorizationController`；
- **GREEN**：admin-web vitest **229/229**（163 既有 + object-authorization 19 + auth-session 扩展 16，处置后 +1 竞态回归）+ 服务端定向 **68/68**（Controller 5 + PermissionService 47 + FileExportDelivery 16 保护性）+ ApiInventory **4/4**（基线 353→354 重生成，AUTHENTICATED 单行增量）+ **G10 类型基线 81==81 零新增**（81 全为 SEC-009.B 冻结的未启用模块类型债）+ eslint（改动文件）0 + fast 门禁 **10/10** + PG 回归 **20/20**（一次全绿，零迁移零脚本变更）。
