# ZS-MSG-003 codex 评审处置（r0→r3 四弧）

- 评审工具：codex（gpt-6-astra / xhigh，`exec -s read-only` 读评工作区）
- 评审对象：分支 `feat/msg-003`（ZS-MSG-003〔B05/B06〕收件箱与消息落点二次授权——分批子项 .A 技术收件箱及业务落点接口鉴权 + .B Web/移动落点和未知模块提示；前置 ZS-MSG-001、ZS-PERM-002.A、ZS-CLIENT-001.A、ZS-CLIENT-002.A 均已交付；.C 归 B08 待 D-09 不在本卡）
- 结论：**r3 PASS / 0 发现**（四弧收敛：r0 PASS 2×P2+1×P3 → r1 PASS 1×P3 残余表述 → r2 FAIL 1×P3 措辞语义 → r3 PASS 零发现）
- 说明：r0~r3 四弧 raw 输出保留于开发窗 `outputs/msg003-codex-r{0..3}.log`（未入库，outputs/ 已 gitignore），各弧裁定与发现已逐条摘录如下，如实登记。

## 交付内容（impl commit `2b6488d7`，23 files +1251/-14；合并 main `1cfa4c5c`，merge-base `7c75f04b`）

1. **落点注册 SPI（`service/notify/landing` 包六文件）**：`NotifyLandingClient`（WEB/MOBILE 端枚举）+ `NotifyLandingDescriptor`（module/route/params **纯结构性业务引用，不携带消息正文**）+ `NotifyLandingUnavailable`（NOT_REGISTERED / MODULE_DISABLED / REVOKED / CLIENT_UNSUPPORTED 四态）+ `NotifyLandingProvider`（SPI：templateCode/module/**authorize 重新授权硬合同**（必须重读业务并校验、不得缓存授权结论）/resolve 端描述）+ `NotifyLandingRegistry`（装配期收集 Provider Bean，重复模板编码启动即失败，Map.copyOf 只读；**无缓存设计**——解析/授权结论每次现算，组织/权限变化即时生效，以无缓存结构消除卡片「组织/权限变化后刷新缓存」问题）。
2. **解析服务 `NotifyLandingServiceImpl` 五道防线 fail-closed**：①租户上下文缺失即拒绝（不默认 0，循 MSG-001/JOB-002 惯例）→ ②消息存在性（跨租户 ID 经生产 tenant 行级过滤解析为不存在）→ ③归属校验（userId/userType 与登录主体不一致即 ACCESS_DENIED，他人消息不能借落点触碰业务入口）→ ④落点注册判定（模板编码未注册=NOT_REGISTERED；模块不在 `ModuleCatalog.ENABLED_MODULES`=MODULE_DISABLED 且不触碰业务）→ ⑤`provider.authorize` 重新授权（ServiceException→REVOKED，旧消息在业务撤权/删除后不得借落点进入详情、下载附件）→ 端描述缺失=CLIENT_UNSUPPORTED。安全失败（不存在/他人/缺租户）以 `1_002_031_000~002` ServiceException 表达，可用性失败以 `available=false + 原因码 + 可呈现 reason` 返回供前端原样提示。
3. **新端点 `GET /system/notify-message/get-landing`**（AUTHENTICATED，同 my-page 惯例；归属限定在服务层强制）+ **未读 size 双层上限**：控制器 `@Min(1)@Max(100)` 显式 400 + `NotifyMessageServiceImpl.normalizeUnreadSize` 防御性收敛（null/非正→10、超限→100，不信任内部调用方）。
4. **Web 端（.B，admin-web）**：`resolveNotifyMessageLanding` API + `views/system/notify/landing.ts` 纯函数助手（buildNotifyLandingRoute 拼 query（route 已含 query 以 & 续接）/applyNotifyLandingRoute（**不可用一律不导航**）/notifyLandingUnavailableText（reason 优先→原因码兜底→默认文案，杜绝静默））；`Message.vue` 铃铛列表点击消费落点 + `MyNotifyMessageDetail.vue` 详情「前往处理」；导航可达性仍由既有路由守卫 `router/access.ts`（ZS-CLIENT-001.A）最终判定，前端不复制权限判定。
5. **移动端（.B，miniapp）**：落点 API + `pages/message/landing.ts` 同语义纯函数（注入导航函数）；`detail-popup.vue`「前往处理」（loading 抑制重复点击）——导航经 `parseUrl+isTabBarPage` 分支：TabBar 落点 `uni.switchTab`（query 经 `setTabParams` globalData 透传，与工作台菜单跳转同语义），普通页 `uni.navigateTo`，两者 fail 均明确 toast「落点页面不存在或不可达」；直达守卫仍由 `router/interceptor.ts`（ZS-CLIENT-002.A）兜底。**顺带删除 `getMyNotifyMessage` 死代码**（原「我的详情」实为管理面 `/system/notify-message/get` 包装、全仓无调用方，防「我的」面误用管理接口；pages-system 管理详情页保留走管理 /get + `@PreAuthorize` + 菜单权限注册，属合法管理面）。
6. `ApiInventoryTest` 基线 341→342（唯一新增行=get-landing，漂移经评审后再生）。

## 评审弧

| 轮次 | 结论 | 发现 → 处置 |
|---|---|---|
| r0 | **PASS** 0×P0/P1；2×P2+1×P3 | **P2-001** 移动 TabBar 落点一律 navigateTo 会被 uni-app 拒绝且无失败提示（当前无生产 Provider 触发）→ goLanding 经 parseUrl+isTabBarPage 分支 switchTab/setTabParams + 两分支 fail toast；**P2-002** 测试注释宣称跨租户轴「复用 ZS-DB-018 PG 回归」失实（其用例未覆盖消息表与 get-landing 链路）→ 订正为如实边界：H2 不含租户拦截器，跨租户消息专项回归登记为后续专项（见登记边界①）；**P3-003** NOT_FOUND 与 ACCESS_DENIED 错误码/文案可区分，登录用户可探测同租户消息 ID 存在性 → 部分采纳：对外文案统一为「站内信不存在或不可访问」+ 归属不匹配记内部 info 日志，错误码保持区分（验收要求「显式拒绝」证据，与 PERM-002.A FORBIDDEN / LOGIN-006 NOT_OWNED 同型）。另核验通过：归属校验/已读 SQL 保留/关闭模块先拒/size 双层/死代码无调用方/两端不可用分支与编码（30 项只读内存断言）。处置提交 `2d424aa2`（另含自查加固：route 含 query 以 & 续接 +2 用例、三处 resolve 补 catch 防异步处理器告警） |
| r1（P2-001/P2-002/P3-003 处置复评） | **PASS** 0×P0/P1；保留 1×P3 残余 | P2-001 闭合（与 useMenuNavigate 分支一致，内存探针过）；P2-002 失实宣称已纠正（并要求合并归档时回填 docs/05 登记专项、关联 ZS-CLIENT-005.B）；新增归属日志仅含 ID 无敏感内容。**P3 残余**：统一文案后双码仍可经 code 区分，「不能表述为探测面已完全闭合」→ 订正注释为「统一文案，仍保留可区分错误码，接受存在性探测残余风险」。处置提交 `d615e16b` |
| r2（r1 处置复评） | FAIL 1×P3 | 唯一发现：注释「统一对外文案以降低探测门槛」语义反向（"降低门槛"=更易探测）→ 建议改「统一对外文案，但错误码本身仍可区分」。拟回填登记文本被确认覆盖 r1 三项边界要求。处置提交 `5bcc2cc5`（纯注释一行） |
| r3（r2 处置复评） | **PASS / 0 发现** | 措辞订正确认、残余风险接受与「不得宣称闭合」表述保持不变、合并回填要求保留。正式收敛 |

## 验证

- TDD 后端：`NotifyLandingServiceTest` 11 用例（WEB/MOBILE 双端可用落点 / 响应不含正文与模板参数全量（JSON 序列化断言 SECRET 探针）/ 不存在·他人 userId·userType 不匹配·缺租户四类安全拒绝（assertServiceException 逐码）/ 未注册·关闭模块·失权·端不支持四类不可用态 / Registry 重复编码拒止）+ `NotifyMessageServiceImplTest` 增强 10 用例（size 归一表驱动 / 105 行端到端截断到 100 且全属本人 / 他人 IDs 混入批量已读后显式断言他人行 readStatus/readTime 不变）= **21/21 BUILD SUCCESS**（r0~r3 各处置后复跑均 21/21）。
- 前端：admin-web vitest **197/197**（新增 notify-landing.spec 34 断言含 route 含 query 续接）、miniapp vitest **107/107**（新增 18）；两端 vue-tsc **0 错误**、eslint/prettier 改动文件全过。
- 门禁：`run-local-gates --fast` **10/10**；G10 Web 类型检查基线 PASS；G13 双端请求层合同回归 PASS；**G14 SYS-001.A 真实 Docker PG/Redis + server 回归 49/49 PASS**（worktree 经 tools/ junction 修复 mvn.cmd 引号解析问题后直跑）。
- 全量对照：module-system 全量 757 用例中 5 处失败（OAuth2TokenServiceImplTest 2 + SmsCodeServiceImplAttemptLimitTest 3），经主树同基线复跑确认为**既有顺序依赖失败**（clean 表/线程上下文跨类残留），与本卡无关。
- ApiInventoryTest 4/4（基线再生后）。

## 登记边界（r1/r2/r3 认可 + 需求约束，非阻塞，如实登记）

1. **跨技术租户消息专项回归未执行**（r2-P2 订正后登记）：H2 测试上下文不含 tenant 拦截器，跨租户消息链路（真实 Mapper/HTTP 链验证跨租户消息 ID 不可读、不可解析、不可改已读）登记为后续专项，随 **ZS-CLIENT-005.B** 真实接口/PG 环境联调收口；生产隔离本体由 tenant 行级过滤提供（system_notify_message 非 ignore-tables，自动追加 tenant_id 条件），本卡以应用层归属校验 + fail-closed 夹具证明防线顺序。
2. **消息 ID 存在性探测残余风险经评审接受**（r1-P3/r2-P3/r3 确认）：NOT_FOUND/ACCESS_DENIED 对外文案统一，但错误码仍可区分（异常处理器原样输出 code）；双码保留用于内部日志与本卡验收证据，不宣称探测面闭合。
3. **落点注册表 v1 无生产注册方**：当前启用模块（system/infra）暂无站内信生产发送方（BPM/HRM/PMS 等发送方均处停用模块），首批真实注册方预期随 BPM 首链（D-07）接入；authorize 重新授权合同（重读业务、不得缓存结论）、端描述合同与四类不可用态语义已由 SPI + 夹具 Provider 锤实。
4. 落点导航的页面可达性由两端既有守卫最终判定（Web `access.ts` / 移动 `interceptor.ts+access.ts`），本卡不复制权限判定；服务端 API 为真实边界。
