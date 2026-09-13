# ZS-FILE-001.A codex 评审处置（r0→r2 三弧）

- 评审工具：codex（gpt-6-astra / xhigh、high，read-only sandbox）
- 评审对象：分支 `feat/file-001-a`（ZS-FILE-001.A 技术账号/tenant 私有文件归属与授权）
- 结论：**r2 PASS / 0 发现**（评审收敛）

## 交付内容

1. **文件租户化**：迁移 `V20260914.001__infra_file_tenancy.sql`（infra_file 加 tenant_id/owner_user_id/scope，存量默认 PRIVATE=「不能默认全部公开」）；FileDO 改 TenantBaseDO + ownerUserId/scope；FileScopeEnum（PUBLIC/PRIVATE）。
2. **写路径归属**：createFile 两路径记录 ownerUserId（服务端确认）+ scope=PRIVATE + 显式 tenantId；presigned create 的 configId 不信任客户端、空值由 master 兜底。
3. **读取授权统一**：下载端点去 @TenantIgnore（保留 @PermitAll），先跨租户定位记录（getFileByConfigIdAndPathIgnoreTenant，PUBLIC 对任意来源同址可用）再 `validateFileReadable`：PUBLIC 匿名可读；PRIVATE 需「ownerUserId>0 且本人且同租户」或「同租户且 PermissionCommonApi 查得 infra:file:query」（与 @ss.hasPermission 同源；scopes 与后台权限是两套体系被否决）；yaml tenant ignore-urls 放行下载路径。
4. **删除授权**：deleteFileList 显式校验「全部存在且全部同租户」（不依赖拦截器装配），混入越权/不存在 id 整批拒绝。
5. **update-scope 管理端点**（infra:file:update 权限 + scope 合法性校验）；presigned create 端点管理端+App 端**同步禁用**（无上传申请绑定可冒领他人文件，凭证化重新交付归 FILE-003）；ApiInventory 基线 328→327 审查通过。
6. **测试**：FileServiceAuthorizationTest 12 用例（默认 PRIVATE+归属/PUBLIC 匿名/PRIVATE 匿名拒/他租户拒/owner 本人/owner=0 userId=0 冒领拒/管理员放行/无权限拒/批删两向/update-scope 两向）+ 补他区欠账 DatabaseTable 列数断言。

## 评审弧

| 轮次 | 结论 | 要点 |
|---|---|---|
| r0 | FAIL 3×P1+3×P2+P3 | P1 PRIVATE 只查租户不查归属；P1 presigned create 可冒领他人文件；P1 S3 直链绕过平台授权；P2 PUBLIC 登录后 404（跨租户查询）；P2 迁移历史归属无恢复路径；P2 测试未证跨租户隔离；P3 错误码冲突 |
| r1 | FAIL 2×P1+2×P2+P3 | P1 owner=0 凭 userId=0 client-credentials 令牌冒领（owner 分支加 ownerUserId>0）；P1 App 端 /create 漏禁（同步移除）；P2 scopes≠后台权限（管理分支改 PermissionCommonApi 同源）；P2 双端 client 上传模式残留假成功（登记 FILE-003/B06 处置）；P3 批删测试被数量校验短路（拆分） |
| r2 | **PASS / 0 发现** | — |

## 验证

- infra 全量：**248 → 252 tests / 0 failures，BUILD SUCCESS**（含新增授权 12 用例）。
- ApiInventoryTest 基线匹配（327 端点）。
- 合并：`--no-ff` 合并回 main（见 docs/05 V1.38 记录）。

## 延后/边界（非阻塞）

1. **S3/云存储直链绕过**：PRIVATE 的存储桶/CDN 直读限制与受控取流 → ZS-FILE-004.A（主体绑定票据/取流协议，B04 下一环）。
2. **历史文件归属认领与分类迁移流程**（存量 owner=0/tenant=0 的运营处置）→ FILE-001.B（B08）或运营规程；管理面可经 update-scope 显式调整。
3. **双端 client 上传模式残留假成功**（后端端点已禁用，前端模式开关未清）→ FILE-003 凭证化时一并收敛（或 B06 前端批次）。
4. **真实容器全链下载授权测试**（TokenAuthenticationFilter×ignore-urls×下载端点）→ ZS-SYS-001.A 真实 PG/API 回归（他区开发中）。
