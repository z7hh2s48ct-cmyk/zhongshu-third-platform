# ZS-CLIENT-001.A codex 评审处置（r0→r9 十弧）

- 评审工具：codex（gpt-6-astra / xhigh，`--dangerously-bypass-approvals-and-sandbox` 读评工作区，多轮以
  **Node 内存复现逐条实测**各判定（各轮 stdout 附复现证据））
- 评审对象：分支 `feat/client-001-a`（ZS-CLIENT-001.A 技术账号 Web 授权导航/缓存会话/关闭模块守卫 +
  登录入口契约 + 导航契约解析器；B06 分批子项）
- 结论：**r9 复评 PASS（0 findings）——r0→r9 十弧正式收敛**；累计 0×P0/P1、P2/P3 存量归零，r8 处置（`650fbe3a`）经 r9 复评确认，不以处置提交自证
- 说明：r1→r9 各弧裁决原文已随评审落盘（`outputs/client001a-codex-r{1..9}-out*`，r5 转码 UTF-8 归档）；
  r0 裁决未随暂存保留，发现与处置按 `fed79051` 提交说明摘录，如实登记。

## 交付内容（impl commit `72c90376`，20 files +3951/-124；处置至 `650fbe3a`；`--no-ff` 合并 main `ed722b31`（r5）+ `50f9e51c`（r6→r9））

1. **纯逻辑授权核心**（`src/router/access.ts`，无 Vue 运行时依赖、vitest 直接覆盖）：以服务端
   get-permission-info 下发菜单树为单一真相源构建授权快照（menuPatterns 精确 + menuPrefixes 祖先前缀 +
   STATIC_ROUTE_ACCESS 逐入口补登锚点，共享/多入口页登记全部合法锚点不压平）+ hasRouteAccess 七级判定
   （未注册/未授权默认拒绝、命中即返回、不回退更宽模块级并集）；leafPatterns（r3 起仅叶子节点参与
   exactAnchors 判定，容器节点剔除）；sanitizeLoginRedirect 消毒登录重定向（scheme/协议相对/反斜杠/
   %2F%2F/../控制字符）；resolveGuardNavigation/resolveBootstrapTarget/isNotFoundMatch/
   resolvePostAuthRedirect 守卫决策纯函数化。
2. **清理合同**（`src/utils/authSession.ts`）：退出/授权装配失败/技术租户变化单入口（页签 + keep-alive
   名单 + 字典快照 + 权限 store + 身份 store + 动态路由 + 用户缓存 + 凭据）；显式声明前端职责边界
   （「接口仍独立拒绝」属服务端 @PreAuthorize）。
3. **接线**（对既有文件最小侵入）：permission.ts 守卫决策全委托 + bootstrap 失败即清理（杜绝半初始化
   路由）；user.ts 只接受服务端本次响应（失效缓存/伪造前端角色不生效、不再写 ROLE_ROUTERS 缓存）；
   resetRouter 放行静态路由集合（修复原 5 项白名单误删 /403、/500、/sso、/social-login、个人中心与
   hidden 子页缺陷）；tagsView clearAuthorizedViews（keep-alive 敏感数据真清空）；dict 同步清理；
   UserInfo/LockPage/TenantVisit/LoginForm/RegisterForm/SocialLogin/MobileForm/ForgetPasswordForm
   全部登录入口接入统一消毒与落地判定。
4. **登录入口契约**（`tests/unit/login-entry-contract.spec.ts`）：源码契约 → 函数级 → 导航实参级演进
   （导航契约解析器 collectNavCalls/isNameTainted/splitTopLevel/countDeclaredBindings/countCodeMatches/
   escapeRegExpName），防「新入口绕过消毒」与「契约被变异绕过」回归。

## 评审弧（Node 内存复现逐轮实测）

| 轮次 | 结论 | 发现 → 处置 |
|---|---|---|
| r0（`72c90376`） | FAIL（多入口锚点完备性/入口消毒） | 审计脚本逐入口抓出 41 处锚点缺口（BPM 流程详情 15 锚点、CRM 8 详情页、HRM 门户/关联页、AI 工作流双锚点、IoT 段序）→ 共享/多入口页登记全部合法锚点不压平；MobileForm（短信登录）/ForgetPasswordForm（重置密码）接入 sanitizeLoginRedirect + resolvePostAuthRedirect（删除 addRouters[0] 死代码兜底）+ 新增 6 例源码契约 → `fed79051`（120 测试） |
| r1（`fed79051`） | FAIL 3×P2+2×P3 | P2-1 父目录锚点过宽（仅授 /hrm/employee/config 可进 /hrm/employee/detail/4；/iot/device 同类）；P2-2 CRM 共享组件入口仍漏登（customer/business 嵌入 ContactList、contact 嵌入 BusinessList，按目录上溯遗漏组件实际使用方）；P2-3 商城共享详情缺合法入口（售后→订单、会员→订单/售后、客服→订单/商品）；P3-1 MobileForm 短信落地未消费 fullPageUrl（SSO 整页跳转未同源）；P3-2 契约仅验证函数名存在（import 即满足）→ 目录锚点收窄 + 组件宿主链补登 + fullPageUrl 分支 + 契约强化 → `67da593a`（126） |
| r2（`67da593a`） | FAIL 3×P2+1×P3 | P2-1 /crm/clue 锚点过度授权（followup 联系人/商机链接受 CRM_CUSTOMER v-if 限制、宿主链不成立，且正向测试固化了错误授权）；P2-2 /pay 整目录授权收银台（/pay/app、/pay/refund 均非入口）；P2-3 商城装修父锚点兄弟互通（共用父节点 id=517 使 {518}↔{524} 互相授权）；P3-1 契约以文件为单位（同文件其他入口绕过不报）→ clue 锚点移除（三页全拒）+ cashier 收窄为唯一真实入口 /pay/demo/order + id=517 降级 exactAnchors 兄弟隔离 + 契约函数级化 → `7ca52fbd`（134） |
| r3（`7ca52fbd`） | FAIL 1×P2+1×P3 | P2-1 保留父节点时 {517,524} 仍越权模板装修（routerHelper.ts:158 将带 children 的 517 替换为 ParentLayout、容器无页面入口语义但 exactAnchors 仍命中）；P3-1 消费判定可被单分支替换绕过（SPA 分支替换为 window.location.assign(redirect) 后照过）→ leafPatterns 仅叶子参与 + 契约升级导航实参级验证 → `1334a10b`（144） |
| r4（`1334a10b`） | FAIL 3×P3 | P3-1 实参出现绑定名≠消费（属性覆盖 X.target= / 整体重赋值 / 同名回调参数遮蔽）→ isNameTainted 失信判定；P3-2 字符串伪调用误报 + 模板串嵌套插值截断函数体 → maskCodePositions 代码位置掩码 + 递归配平；P3-3 裸 replace 不识别（入口过滤与实参检查覆盖范围不一致）→ collectNavCalls 统一导航调用提取 → `a6bab2b9`（150） |
| r5（`a6bab2b9`） | FAIL 4×P3 | P3-1 多字段解构参数遮蔽漏检（split(',') 破坏解构花括号完整性）→ splitTopLevel 按顶层定界符拆分 + 解构绑定名解析（别名/默认值/嵌套）；P3-2 解构声明默认值误判整体重赋值 → countDeclaredBindings 声明初始化计数；P3-3 失信检查未排除字符串文本（"postAuth.target = redirect" 字面量误报）→ countCodeMatches 仅统计代码位置；P3-4 含 $ 合法绑定误报（绑定名拼入正则使 $ 成结束锚点、\b 不适合作边界）→ escapeRegExpName 全特殊字符转义 → `41055f0e`（155） |
| r6（`7f676a36`） | FAIL 4×P3 | P3-1 解构声明计数抵扣真实重赋值（`countDeclaredBindings` 把 `let {a}=o` 后 `a=x` 误算已初始化）→ countPatternAssigns 条目化逐绑定判定；P3-2 字符串/模板串内括号破坏顶层拆分（`splitTopLevel` 遇 `"(…)"` 误配平）→ 词法状态机 + 配平区间跳过；P3-3 默认值对象内引用误判为声明（`{a: b}` 的 b 被当绑定）→ 顶层等号分离声明与默认值；P3-4 模板插值内写入被屏蔽漏检 → maskCodePositions 保留插值内代码位置 → `7f676a36`（159） |
| r7（`038eba08`） | FAIL 1×P3 | P3-1 字符串键内伪默认值抵消真实重赋值（`o["{a=b}"]` 字面量被当解构默认值）→ patternBraceRange 词法感知花括号区间（跳过字符串/模板/注释）；同源加固 bindingName/collectResolutionNames 配平提取 → `038eba08`（162） |
| r8（`650fbe3a`） | FAIL 1×P3 | P3-1 r7 回归——字符串内伪解构被收为可信引用（`collectResolutionNames` 直绑路径未经位置掩码）→ 直绑/解构两路径统一 maskCodePositions 位置过滤 + 真实文件变异回归 → `650fbe3a`（163） |
| r9（`650fbe3a`） | **PASS（0 findings）** | 以复评结论自证收敛——r8 处置经 r9 复评零发现，十弧 0×P0/P1、P2/P3 存量归零 |

## 验证

- admin-web vitest：110→120→126→134→144→150→155→159→162→**163/163**（5 files，逐弧回归累积；逻辑级单测不依赖
  @vue/test-utils）。
- G10 类型基线：`node scripts/client/verify-ts-baseline.mjs` → 11 == 11、newErrors=[]（各弧零新增）。
- eslint ./src 与 prettier --check（改动文件）各弧全过；仓库既有 32 处格式告警均在未触碰的上游文件中。
- 合并 `--no-ff` main：r5 增量 `ed722b31`（父 `63a20334` + `41055f0e`）；r6→r9 增量 `50f9e51c`（父 `c8011893` + `650fbe3a`，login-entry-contract.spec.ts +453/-77）。

## 登记边界（如实登记）

1. **复评闭环（原「末轮修复未经下一弧复评」已消除）**：r5 处置 `41055f0e` 续经 r6→r8 三弧处置
   （`7f676a36`/`038eba08`/`650fbe3a`），r9 复评 PASS（0 findings）确认收敛——不以处置提交自证，
   以复评结论闭环。
2. **前端只做权限集合成员判断**：服务端 @PreAuthorize 为唯一真实边界，客户端守卫为纵深防御 UX 层；
   .B（Web 消费业务动作与字段授权，前置 ZS-PERM-003、需 D-09）归 B08，本批不做。
3. **enabledModules 运行模块白名单为预留入参**（ZS-CFG-003.A）：前端当前拿不到该信号时传 null，
   降级为「服务端菜单存在性」判定（后端菜单树已按运行白名单 × 套餐/角色权限交集过滤），
   不新增后端代码与 API 端点。
4. r0 裁决原文未随暂存保留：41 处缺口数为审计脚本实跑结果（`outputs/analyze-access*.mjs` 系列），
   r0 发现与处置按 `fed79051` 提交说明摘录。
