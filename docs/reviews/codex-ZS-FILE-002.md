# ZS-FILE-002 codex 评审处置（r0→r2 三弧）

- 评审工具：codex（gpt-6-astra / xhigh、high，read-only sandbox）
- 评审对象：分支 `feat/file-002`（ZS-FILE-002 上传用途、内容校验和唯一对象键）
- 结论：**r2 PASS / 0 发现**（评审收敛）

## 交付内容

1. **唯一对象键**：时间戳唯一后缀默认开启并熵增为 UUID 32 hex（原毫秒+5位随机每毫秒仅 9 万种，同毫秒并发同名有 1/90000 撞键概率）；同日同名并发上传生成不同对象键、互不覆盖（测试以模拟存储并发回读 SHA-256 散列一致证明）。
2. **服务端内容探测**：createFile 始终以纯内容 Tika 探测（调用方声明 type 不被盲信）；探测扩展名与文件名扩展名不一致拒 `FILE_TYPE_MISMATCH`（伪装拒绝）；一致性按 Tika 合法扩展名【集合】归一比较（jpeg/jfif 别名不误拒）；ZIP 容器家族（jar/war/docx/xlsx 等）兼容放行。
3. **危险扩展名隔离**：黑名单（exe/dll/bat/cmd/sh/js/vbs/msi/com/scr/ps1/svg）作用于补全后的最终名且优先于一致性校验；svg 入黑名单（writeAttachment 对 image/* 内联渲染，SVG 携带脚本构成存储型 XSS 面）。
4. **配置合同 FileProperties（zszj.file）**：max-size（16MB，service 层显式校验不依赖 multipart 兜底）、danger-extensions、public-allowed-types（精确 MIME 枚举排除 svg）+ max-concurrent-uploads 上传并发预算信号量（在途准入，超出拒绝）；S3Client apiCallTimeout 固定 60s（慢存储不得持续占用上传线程）；yaml 显式登记。
5. **转 PUBLIC 白名单**：updateFileScope 校验类型在公开素材白名单【精确匹配】（前缀匹配会放进 image/svg+xml）。
6. **测试**：FileUploadHardeningTest 15 用例（并发同名不覆盖+散列一致/PNG 伪装 .txt 拒/调用方 type 不被盲信/危险扩展+双扩展名拒/无扩展名 shell 最终名命中/超限拒/正常回读散列一致/转 PUBLIC 白名单两向/未知二进制不升级/前缀残留不可命中/svg 隔离/jar 容器放行）。

## 评审弧

| 轮次 | 结论 | 要点 |
|---|---|---|
| r0 | FAIL 3×P1+4×P2 | P1 文件名参与探测自证（fake.png 升级 image/png 过白名单）→ 纯内容探测；P1 黑名单先于补扩展名（payload 无名 + shell 内容补成 .sh 漏检）→ 黑名单作用于最终名；P1 默认 image/ 白名单含 svg（存储型 XSS）→ 精确枚举；P2 首选扩展名误拒 jpeg/jfif → 集合归一；P2 唯一键熵不足 → UUID；P2 S3 超时/内存预算未登记 → 接线+信号量 |
| r1 | FAIL P1+4×P2 | P1 yaml danger-extensions 覆盖默认值（svg 失效）→ 同步追加；P2 纯内容探测误拒 JAR（zip 容器）→ 容器家族兼容；P2 唯一键熵增未实际实施 → UUID 落地；P2 PUBLIC 仍前缀匹配 → 精确 equalsIgnoreCase；P2 S3 超时未读配置 → 明确固定 60s 合同移除未接线键 |
| r2 | **PASS / 0 发现** | — |

## 验证

- infra 全量：**266 tests / 0 failures，BUILD SUCCESS**（新增 FileUploadHardeningTest 15 用例）。
- 合并：`--no-ff` 合并回 main（见 docs/05 V1.41 记录）。

## 延后/边界（非阻塞）

1. **上传流式化与在途字节精细预算**：Controller 仍整体读入 byte[]（16MB 上限下可接受）；流式直传与逐块校验归 ZS-FILE-003 凭证化改造。
2. **危险内容隔离区**（检出危险内容转隔离存储而非拒绝）→ FILE-003 临时区设计一并考虑；本批以「拒绝上传=隔离」语义交付。
3. **真实对象键唯一性的存储端保证**（S3 条件写/版本控制防覆盖）→ FILE-003/FILE-004.A 存储策略检查。
