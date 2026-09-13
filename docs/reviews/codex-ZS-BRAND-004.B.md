# ZS-BRAND-004.B codex 评审处置（r0→r2 三弧）

- 评审工具：codex（gpt-6-astra / xhigh，read-only sandbox，`codex review --commit <SHA>` @ worktree `E:/zszj-wt-brand-004-b`）
- 评审对象：分支 `feat/brand-004-b`（ZS-BRAND-004.B 缓存/浏览器存储/会话命名切换 B03 联验）
- 结论：**r2 PASS / 0 发现**（评审收敛）
- 原始日志：worktree `outputs/brand-004b/codex-r0.txt`/`r1.txt`/`r2.txt`（未入库；本档案为处置入库）

## 交付物（impl `345c1c12`，2 files）

- `scripts/brand/verify-brand-004b-storage.mjs`——键清单静态门禁：git ls-files 全量提取后端 51 个 Redis 键模式（11 个 `*KeyConstants.java`：system/bpm/crm/erp/hrm/im/iot/trade/mes/pay + 3 个保护层 DAO + 3 处 yaml wx/wa key-prefix）与两端 35 个浏览器存储键（admin-web useCache CACHE_KEY 10 键 + ACCESS_TOKEN/REFRESH_TOKEN；miniapp defineStore 持久化 id + uni.*StorageSync 字面量含 .vue）；断言旧名（yudao/ruoyi/iocoder/youdao/芋道/unibest/yd-）残留 0、命名合同（LOGIN-002 代际键/LOGIN-003 墓碑/LOGIN-004 配额键/TenantRedisCacheManager 租户后缀）缺失 0；8 组旧名注入自检防扫描器空洞。
- `scripts/brand/run-brand-004b-runtime.mjs`——运行期隔离证明：一次性 Docker Redis 7.4 + PG17 夹具（随机名/端口、退出自动清理含 java 子进程终止、缺 Docker 退出码 3、竞态重试一次）；真实 zszj-server（mock-enable=false）账密登录；39 断言——新命名空间签发（`oauth2_access_token:{t}`）、租户后缀（`role:1:1`）与豁免缓存形态、LOGIN-002 并发刷新（3 路并发代际==3、恰 1 个最高代际有效、旧代际全 401 无双键）、LOGIN-003 撤权/退出（禁用后最新令牌立即 401，键清理+墓碑+代际键清理**在自愈请求之前**取证）、旧名隔离直证（真凭据副本写入 `yudao_oauth2_access_token:*` 作阳性对照 + MONITOR 捕获命令流：旧名前缀键零读取、伪造旧名凭据 401、终态无残留）。

## r0（commit 345c1c12）FAIL 1×P1 + 7×P2

| 级别 | 发现 | 处置（`930d365e`） |
|---|---|---|
| P1 | 非 POSIX 平台 Java 子进程不终止，Docker 夹具泄漏进程 | 子进程终止逻辑修复（taskkill/信号双路径），退出自动清理验证 |
| P2 | auth.ts 无分号声明漏提 REFRESH_TOKEN（键提取声明解析缺陷） | 声明解析兼容无分号形态，键清单 35 键复核 |
| P2 | .vue 文件不在 miniapp 存储扫描范围 | 扫描范围纳入 .vue（mp:draft:edit 等字面量入库） |
| P2 | DAO 值形状过滤漏报（yaml key-prefix 类提取被静默丢弃） | 值形状保留并参与命名合同校验 |
| P2 | 撤权清理取证被认证自愈掩盖：E3/E4 在 401 自愈请求之后取证，`checkAccessToken` 的 `revokeWithTombstone` 修复证据 | 取证时序提前——键清理/墓碑/代际键状态在自愈请求**之前**断言 |
| P2 | 旧名反向用例区分度不足：伪造凭据无 DB 行，即使读到旧名缓存也必 401，F-1/F-2 无法证明命名空间隔离 | 升级为隔离直证：真凭据副本写入旧名键作阳性对照 + `MONITOR` 捕获窗口内命令流证「零读取」 |
| P2 | 并发刷新 D3 集合判定误过：响应缺 refreshToken 时集合为 undefined 集、重复值也误过 | 断言逐响应 refreshToken===原令牌且非空 |
| P2 | 缺 Docker 路径 TDZ：清理状态在可用性检查前未初始化，退出码错 | 初始化顺序修正，缺 Docker 退出码 3 复验 |

## r1（commit 930d365e）FAIL 2×P2

| 级别 | 发现 | 处置（`ba13c81d`） |
|---|---|---|
| P2 | 修复中误删 fail() 基础设施失败助手，基础设施故障退出码退化为误报 | 恢复 fail()，基础设施失败=退出码 3 语义回归 |
| P2 | MONITOR 用固定延时等待就绪存在竞态（探针早于订阅生效丢失命令流） | 改为 MONITOR 就绪应答等待后发探针 |

## r2（commit ba13c81d）PASS / 0 发现

codex 复评结论：「No actionable regressions were found」。

## 验证

- 静态门禁：51 Redis 键模式 + 35 两端存储键，旧名残留 0、命名合同缺失 0、注入自检 8/8 PASS
- 运行期证明：Redis 7.4.11-alpine + PostgreSQL 17-alpine 一次性容器，**39/39 断言全绿，RUN_EXIT=0**（worktree `outputs/brand-004b/runtime-report.json`）
- 门禁：`run-local-gates --fast` 10/10（合并前提交后复跑；G3 品牌全仓扫描覆盖本脚本既有放行条目）
- 合并：`--no-ff` 合并回 main（`b1ba1b24`）

## 延后/边界（非阻塞，登记移交）

1. **Druid PSCache + PG 游标缺陷在真实进程复现**（MyBatis-Plus `selectWithCursor` 重复执行报 statement 已关闭；local profile `pool-prepared-statements: false` 未实际生效，本夹具以命令行覆盖规避）——归 ZS-DB-001 复核 local profile 键生效性（与 DB-001.B/B02 环境收口同窗）。
2. 键清单门禁并入 `run-local-gates`（G3 聚合或新增 Gx）留收口时定（本 worktree 未改 run-local-gates.mjs，防跨会话门禁计数漂移）。
3. LOGIN-002 代际键「登录不登记、刷新起 INCR」为 OAuth2TokenServiceImpl 现状合同，已按代码事实断言；「登录即登记代际」如需归 ZS-LOGIN-002 后续。
4. 浏览器真实 E2E（登录/退出后 localStorage 残留清理）归 ZS-CLIENT-005.B/ZS-SYS-001.B（B06）。
