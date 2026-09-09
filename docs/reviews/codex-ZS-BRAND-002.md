# ZS-BRAND-002 Codex 代码评审

- **任务**：ZS-BRAND-002 统一后端模块、包类、配置与构建产物名称
- **提交**：`43d71fad`
- **改动规模**：10,059 files changed, +130,413 / -130,501 lines（大规模改名）
- **评审时间**：2026-09-09
- **评审工具**：`codex-cli 0.153.4`，模型 `gpt-6-astra`，reasoning effort `xhigh`
- **原始日志**：[codex-ZS-BRAND-002.raw.md](codex-ZS-BRAND-002.raw.md)

## Codex 原始结论

> The renamed database mappings no longer match the shipped PostgreSQL schema. The new naming verifier also rejects valid generated sources after compilation.

### Review comments

- **[P2] Synchronize renamed table mappings with the shipped schema** — `services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/dal/dataobject/demo/demo01/Demo01ContactDO.java:16-17`
  > When initializing PostgreSQL from the shipped `sql/postgresql/ruoyi-vue-pro.sql`, the database still contains `yudao_demo01_contact` and `yudao_demo01_contact_seq`, not these renamed objects. Consequently, the enabled infra module's contact CRUD endpoints fail with missing-relation errors; the demo02/demo03 mappings have the same mismatch. Update the schema and migration together with these annotations, or defer the annotation changes until the SQL rename lands.

- **[P2] Exclude build outputs from the naming verifier** — `scripts/brand/verify-backend-naming.mjs:32-34`
  > After Maven generates MapStruct implementations under `target/generated-sources/annotations`, this scan includes them, but the package-path validation only recognizes `src/main/java` and `src/test/java`. Valid generated classes therefore produce `package-path-mismatch` failures and a nonzero exit. This was reproduced against the commit snapshot by adding an `AuthConvertImpl.java` in the standard generated-source location. Exclude `target` directories or restrict validation to actual source roots so the verifier remains usable after compilation.

## 复核与处置

### 发现 1：demo 表映射与 SQL 脚本不一致

**事实核对**：
- 在提交 `43d71fad` 时点，`Demo01ContactDO.java` 的 `@TableName` 已改为 `zszj_demo01_contact`，但 `sql/postgresql/ruoyi-vue-pro.sql` 仍为 `yudao_demo01_contact`。
- **该问题已被后续提交 `581927d0`（ZS-BRAND-004.A）修复**：当前 `sql/postgresql/ruoyi-vue-pro.sql` 第 5701-5731 行已全部改为 `zszj_demo01_contact`，并新增幂等升级脚本 `sql/postgresql/upgrades/20260908_brand_rename_zszj.sql`。
- 开发记录（05 清单 ZS-BRAND-004 段）明确登记：「真实 PG 验证（docker postgres:17-alpine 临时容器）：新装库与"旧种子+迁移"升级库各 8 项旧标识扫描全 0」。

**严重度评估**：
- **提交时点 P2 有效**：在 `43d71fad` 单独 checkout 时确实会导致 infra demo CRUD 端点 missing-relation 错误。
- **当前状态：已修复**。ZS-BRAND-004.A 在同一批次内补齐了 SQL 迁移，符合 05 清单「配置新旧键不得产生两个不同有效值，冲突应明确失败」的要求。

**处置**：
- ✅ 无需额外修复（已由 ZS-BRAND-004.A 闭环）。
- 📝 经验登记：大规模改名批次应把「Java 注解 + SQL schema + 迁移脚本」作为原子单元提交，避免中间提交出现运行时断裂。后续类似批次（如 ZS-BRAND-004.C 任务/消息持久化引用）须遵循此原则。

### 发现 2：verify-backend-naming.mjs 未排除 target/ 构建产物

**事实核对**：
- 当前 `scripts/brand/verify-backend-naming.mjs` 第 32-34 行：
  ```js
  walk(core, (p) => {
    if (p.endsWith('.java') && !p.includes(`${sep()}` + 'zszj-ui' + `${sep()}`) && !p.includes(`${sep()}` + 'sql' + `${sep()}`)) allJava.push(p);
  });
  ```
- 排除项仅有 `zszj-ui/` 与 `sql/`，**未排除 `target/`、`build/`、`node_modules/`、`.git/`**。
- 第 45 行 `expect = relPath.replace(/^.*\/src\/(main|test)\/java\//, '')` 只识别 `src/main/java` 与 `src/test/java`，Maven 生成的 `target/generated-sources/annotations/**/*.java` 会被判为 `package-path-mismatch`。
- Codex 已通过注入 `AuthConvertImpl.java` 到标准 generated-source 位置复现该失败。

**严重度评估**：
- **确认 P2 仍然有效**（当前 HEAD 未修复）：
  - 不影响当前 CI（CI 在干净 checkout 上运行，无 `target/`）；
  - 但开发者本地执行 `mvn compile` 后再跑 `node scripts/brand/verify-backend-naming.mjs` 会得到假阳性失败，破坏「本地与 CI 同规则」原则（ZS-OPS-001.A 验收项）。

**处置建议**（待修复）：
- 在第 33 行的过滤条件追加 `target/`、`build/`、`node_modules/`、`.git/` 排除：
  ```js
  if (p.endsWith('.java')
      && !p.includes(`${sep()}zszj-ui${sep()}`)
      && !p.includes(`${sep()}sql${sep()}`)
      && !p.includes(`${sep()}target${sep()}`)
      && !p.includes(`${sep()}build${sep()}`)
      && !p.includes(`${sep()}node_modules${sep()}`)
      && !p.includes(`${sep()}.git${sep()}`)) allJava.push(p);
  ```
- 同一脚本的第 72、92、113、126 行的 `walk` 调用也应统一排除规则（建议抽出 `isExcluded(p)` 辅助函数）。
- 修复归入下一轮 ZS-BRAND-006.A 增强或 ZS-OPS-001.A 门禁完善批次。

## 其他核对项（Codex 已验证）

| 项目 | 结果 |
|---|---|
| 6,763 个 Java 包声明与路径一致 | ✅ 静态检查通过（`verify-backend-naming.mjs` 硬性问题 0） |
| imports / spring.factories / Mapper XML 的 `cn.zszj` FQCN 全部可解析 | ✅ 通过 |
| POM 坐标统一为 `cn.zszj` / `zszj-*` | ✅ 通过 |
| 上游署名链接（github.com/YunaiV/*、gitee.com/zhijiantianya/*）保留 | ✅ 评审修正随 `581927d0`/`b2c26ea9` 落地恢复 |
| 干净构建、依赖树、真实 PG 启动 | ⏳ 待 ZS-ENG-002 工具链在 B01/B02 补证（本机无 JDK/Maven，已如实登记） |

## 结论

**评审通过（含 1 项已闭环 P2 + 1 项待修复 P2）**。

- 发现 1（demo 表映射）已由 ZS-BRAND-004.A 在同一批次内闭环，无需额外处置。
- 发现 2（verify-backend-naming.mjs 未排除 target/）**仍然有效**，须在下轮门禁完善批次修复，否则违反「本地与 CI 同规则」原则。
- 大规模改名的系统性/一致性整体达标，静态检查 0 硬性问题；运行时验证（干净构建、真实 PG 启动）按 05 清单登记为 B01/B02 待补证项，符合「不虚报生产可用」要求。
