# ZS-FILE-003 codex 评审处置（r0→r4 五弧）

- 评审工具：codex（gpt-6-astra / xhigh、high，read-only sandbox）
- 评审对象：分支 `feat/file-003`（ZS-FILE-003 预签名上传的凭证与完成确认）
- 结论：**r4 PASS / 0 发现**（评审收敛）

## 交付内容

1. **凭证模型**：新表 `infra_file_upload_credential`（迁移 `V20260914.002`：CREATE SEQUENCE 对应 @KeySequence + 唯一 token 索引 + H2 schema）；凭证绑定主体/租户/用途/临时对象键（temp/ 前缀 + UUID，平台凭据只写临时区）/大小/类型/scope/有效期 30 分钟。
2. **凭证创建**：复用 FILE-002 合同预校验（大小上限/危险扩展/PUBLIC 白名单按声明）；scope 默认化+枚举校验；master client 必须支持 presign（local 等抛 `FILE_PRESIGN_NOT_SUPPORTED` 禁用直传）；S3 签名有效期与凭证 30 分钟对齐（`presignPutUrl(path, seconds)` 重载）。
3. **完成确认**：读凭证（不信任客户端 configId/path/url）→ 归属校验（owner>0 须匹配登录主体）→ 状态预检（COMPLETED 快速失败）→ 过期校验 → 有界读临时对象（`getContentBounded`：S3 headObject 先验长度超限即中止不拉体 + 流式逐块累计超限断流 + abort；DB 客户端兜底整读）→ 大小/声明一致性 → FILE-002 硬化复验（探测/补扩展名/危险/一致性 + scope 枚举 + PUBLIC 白名单按实际类型）→ 发布正式对象（服务端正式键）→ 核验正式对象散列=快照散列 → 落 infra_file（持久化 file_hash）→ CAS 一次性迁移（WHERE status=WAITING）→ 清临时对象；并发准入信号量 + 失败告警。
4. **事务**：@Transactional 使文件插入与凭证 CAS 同事务，CAS affected=0 抛异常触发插入回滚，不生成重复资产。
5. **测试**：FileUploadCredentialTest 12 用例（全流程唯一资产/伪造 token/他人凭证/过期/空上传/大小不符/重复确认/并发仅一成功/PUT URL 重用不替换正式资产/危险扩展/超限/local 禁用直传）。

## 评审弧

| 轮次 | 结论 | 要点 |
|---|---|---|
| r0 | FAIL 4×P1+5×P2 | P1 PG 迁移缺 seq；P1 文件插入与 CAS 不同事务（重复资产）；P1 完成时未按实际类型复验 PUBLIC 白名单；P1 大小校验在整读之后（堆耗尽）；P2 补扩展名缺失/svg+PUBLIC 缺口/补偿缺失/散列未持久化/presign 24h 与凭证 30min 不齐 |
| r1 | FAIL P1+3×P2 | P1 纯内容探测误拒合法 JAR（zip 容器家族兼容放行）；P2 UUID 熵增未实际落地（毫秒+5位随机）→ fastSimpleUUID；P2 PUBLIC 白名单仍前缀匹配 → 精确 equalsIgnoreCase；P2 S3 超时未读配置 → 明确固定 60s 合同移除未接线键 |
| r2 | FAIL P1 | getContentBounded 落在事务装饰器外侧被 afterCommit 绕过 → 改 decorateCache 内侧包装 |
| r3 | FAIL P1 | 超限异常抛出前未 abort 响应流（close 会继续拉完剩余体）→ 抛出前显式 abort |
| r4 | **PASS / 0 发现** | — |

## 验证

- infra 全量：**278 tests / 0 failures，BUILD SUCCESS**（新增 FileUploadCredentialTest 12 用例）。
- 合并：`--no-ff` 合并回 main（见 docs/05 V1.45 记录）。

## 延后/边界（非阻塞）

1. **发布后失败补偿的自动重放**（散列核验失败/清理失败的正式键人工清理日志已有，自动任务归 FILE-005.A/FILE-005.B）。
2. **存储兼容性独立验证**（S3 条件复制/版本控制的真实桶行为）按卡片要求归环境批次验收。
3. **DBFileClient** 若启用 presign 需单独评估；当前默认不支持即禁用直传。
