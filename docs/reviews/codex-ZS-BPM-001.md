# codex 评审弧：ZS-BPM-001（BPM 独立装配与 PostgreSQL 验收，B09 技术准备先行件）

> 评审对象：分支 `feat/bpm-001-a` 相对 main 全部改动（impl `3ea8051c` + 合并 main `27ee56b8` + 四轮修复 `8a1639f2`/`6d826348`/`51a1e8a6`/`5f568aff`/`5bc67d61`），合并 main `7548b15b`
> 评审工具：codex exec（OpenAI Codex CLI 0.154.0，模型 `gpt-6-astra`，reasoning effort `xhigh`，sandbox read-only）
> 评审日期：2026-09-14
> 结论：**r0 NEEDS_FIX（3×P1+7×P2）→ r1 NEEDS_FIX（2×P1 未闭合+1×P2 未闭合+1×P2 新增）→ r2 NEEDS_FIX（1×P2 新增）→ r3 NEEDS_FIX（2×P2）→ r4 NEEDS_FIX（1×P2）→ r5 PASS（可合并）**
> 原始日志：[r0](codex-ZS-BPM-001.raw.md)、[r1](codex-ZS-BPM-001.raw-r1.md)、[r2](codex-ZS-BPM-001.raw-r2.md)、[r3](codex-ZS-BPM-001.raw-r3.md)、[r4](codex-ZS-BPM-001.raw-r4.md)、[r5](codex-ZS-BPM-001.raw-r5.md)

## r0（评审 impl 提交，含故障注入实测）：3×P1 + 7×P2，NEEDS_FIX

| # | 级别 | 发现 | 处置（提交） |
|---|---|---|---|
| 1 | P1 | 第 9 套件在 CI 必失败：编排器强制依赖未入库 tools/，pg-regression.yml 只装 Node；独立 Maven 构建缺兄弟模块产物 | 工作流补 setup-java@17 + mvn install 依赖模块步骤；编排器工具链 tools/ 优先→回退 PATH→皆缺退出码 3；套件前置 install（`8a1639f2`） |
| 2 | P1 | skipped>0 仍全绿；陈旧 surefire 报告可复用 | 跑前删本轮 XML；跑后验 testsuite 身份/精确用例数（5/9）/零跳过零失败零错误 + mvn exit==0（`8a1639f2`） |
| 3 | P1 | 结构检查把命令失败转成通过（countTables 吞错、版本空串、S1 任意失败当权限拒绝） | psqlScalar/psqlScalarInt 严格封装；S1 先证 app 连接可用再以 `\set VERBOSITY verbose` 精确判定 SQLSTATE 42501（`8a1639f2`，SQLSTATE 显隐问题当轮实测补齐） |
| 4 | P2 | 清理失败被吞（cleaned 先置位）、无 SIGTERM | rm -f -v 重试 3 次成功才置位、SIGTERM 处理、失败记 S4 非零（`8a1639f2`） |
| 5 | P2 | 启动重试撞同名容器/同端口 | 重试前清残件+换随机端口，失败原因带端口落日志（`8a1639f2`） |
| 6 | P2 | 「关闭 BPM 无副作用」结论超出证据（未启动关闭态 server） | S0 更名「结构零残留」；决策文档区分结构面/装配面（V1.6 真实启动联验+ModuleWhitelistTest），运行期观测归启用 BPM 批次（`8a1639f2`） |
| 7 | P2 | 分页全从 OFFSET 0 起、运行态断言可空洞 | 唯一列（实例 ID）全序 + OFFSET 0/2 页与全量 subList 精确相等 + 运行面精确计数=4（`8a1639f2`） |
| 8 | P2 | 回滚测试吞任意 IllegalStateException、未证恢复 | 专用 HarnessRollbackSignal + assertThrows；发起成功后才抛；运行/历史实例/历史任务三面零残留；重试提交成功直证恢复（`8a1639f2`） |
| 9 | P2 | approve 用例未直证通过分支（误接 rejectedEnd 仍绿） | 补 historicActivity approvedEnd=1 / rejectedEnd=0（`8a1639f2`） |
| 10 | P2 | PASSWORD 缺失静默回退空串，违反快速失败合同 | requireEnv 强制非空，连接池创建前完成全部环境校验（`8a1639f2`） |

## r1（复核 `8a1639f2`，含离线 Maven 校验与故障注入）：8/10 闭合，2×P1 未闭合 + 1×P2 新增，NEEDS_FIX

| # | 级别 | 发现 | 处置（提交） |
|---|---|---|---|
| 1 | P1 | `-pl system,infra -am` 不含 `zszj-dependencies` BOM，干净环境 Non-resolvable import POM（本机 ~/.m2 有缓存故未暴露） | 工作流与编排器安装命令显式加入 zszj-dependencies（`6d826348`） |
| 3 | P1 | 引导后结构读取 catch 只打印+finish，failCount=0 exit 0 伪绿 | 先 record FAIL 再 finish（`6d826348`） |
| 4 | P2 | 同步阻塞期信号回调不可达 | 依赖安装与两阶段 Maven 改事件驱动异步 spawn，SIGINT/SIGTERM 可达（`6d826348`） |
| 新 | P2 | Windows PATH 回退探测 spawnSync('mvn') 无法启动 mvn.cmd（恒 ENOENT 误判） | 探测与执行统一经 cmd.exe /d /s /c（`6d826348`） |

## r2（复核 `6d826348`，含 Windows 子进程复现）：4/4 闭合，1×P2 新增，NEEDS_FIX

| # | 级别 | 发现 | 处置（提交） |
|---|---|---|---|
| 新 | P2 | 超时仅 child.kill() 杀外层 cmd.exe，孙辈 java 持管道致 close 不到、流程挂死（ping 模拟实测：同步 157ms vs HEAD 3043ms） | detached 进程组 + 超时 killTree()：taskkill /T /F（win）/kill(-pid)（posix）整树终止 + 15s 有界兜底 resolve(-2)（`51a1e8a6`） |

## r3（复核 `51a1e8a6`，实测 15s 兜底返回 -2）：原项未闭合 1 + 新增 1，NEEDS_FIX

| # | 级别 | 发现 | 处置（提交） |
|---|---|---|---|
| 原 | P2 | killTree 内 taskkill spawnSync 无超时，自身阻塞则兜底不可达 | taskkill spawnSync 补 timeout 5s + SIGKILL（`5f568aff`） |
| 新 | P2 | detached 使子进程脱离终端进程组，Ctrl+C 不再触达 Maven；onSignal 未杀树 | currentChild 跟踪 + onSignal 显式 killTree 后再清容器退出（`5f568aff`） |

## r4（复核 `5f568aff`，故障注入实测 20s 兜底返回）：1/2 闭合，1×P2 未闭合，NEEDS_FIX

| # | 级别 | 发现 | 处置（提交） |
|---|---|---|---|
| 2 | P2 | killTree 忽略 taskkill 返回的 error/status（超时 ETIMEDOUT 不抛异常），中断路径仍清容器退出、子树存活 | taskkill 重试 2 次，`!r.error && r.status===0` 才算成功；仍未确认则退回 child.kill(SIGKILL) 并 stderr 响亮告警（可能残留，指引人工排查），返回布尔（`5bc67d61`） |

## r5（复核 `5bc67d61`，15 项隔离故障注入）：**PASS（可合并）**

- 判定闭合：taskkill 双重试+严格成功判定+退回直杀+告警+返回值；posix 非阻塞语义正确；中断路径降级处置符合申报。
- 新增问题：未发现 P1/P2。

## 验证记录

- 评审弧全程在 worktree `E:/zszj-wt-bpm-001-a` 只读沙箱内进行，codex 亲跑静态检查/内存故障注入/离线 Maven 校验/Windows 子进程模拟；真实 PG 套件由本侧执行。
- 每轮修复后真实 PG 套件全量实跑 6/6 全绿 exit 0（outputs/bpm-001/final-run.log、r2-final.log、r3-final.log）；合并 main（7548b15b）后主树门禁 10/10（G1~G9+G2b）+ 品牌命名 0 违规 + 套件复跑全绿（outputs/bpm-001/main-final.log）。
- r0~r5 修复引入项共 4 个（异步化引入挂死面、detached 引入中断面、killTree 引入静默失败面、Windows 探测不一致），全部在弧内收敛归零。
