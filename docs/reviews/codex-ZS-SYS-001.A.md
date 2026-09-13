# ZS-SYS-001.A codex 评审处置（impl 三弧 + 追交增量逐提评审，0×P0/P1 收敛）

- 评审工具：codex（gpt-6-astra / xhigh，read-only sandbox，`codex review --commit <SHA>` @ worktree `E:/zszj-wt-sys-001-a`）
- 评审对象：分支 `feat/sys-001-a`（ZS-SYS-001.A 七类基础管理真实 PG/API 回归）
- 结论：**全部提交 0×P0/P1**（评审收敛；矩阵 48/49，唯一 FAIL 为跨 PG 缺陷 GAP-3 已登记归口）
- 原始日志：worktree `outputs/sys001a-codex-r{0,1,2}.log`（未入库；本档案为处置入库）

## 提交与评审弧

| 提交 | 内容 | 评审结论 | 处置 |
|---|---|---|---|
| `8e6020ee` | 编排器 + 七类用例矩阵 + 夹具 profile（10 files, +1169） | r0 FAIL 2×P1+4×P2 | `5f2507c9` 六项处置 |
| `5f2507c9` | r0 修复 | r1 1×P2 | `8950c0a9` 受控拒绝断言收紧为业务码域（≥1e9），排除 null/HTTP 基础设施出口 |
| `8950c0a9` | r1 修复 | PASS | — |
| `f8b7d25a` | GAP-1 夹具规避（CLI 关闭 Druid PSCache）+ 构建守卫 + 种子 setval 兜底 | 2×P2 | 已修复 |
| `3097b22f` | GAP-2 岗位引用删除受控（归口 ZS-IAM-003） | **0 发现** | — |
| `87e68cf2` | SYS-MENU-N1 受控自父用例 + GAP-4 登记 | **0 发现** | — |
| `30ec1cb9` | 报告结论实测推导 + shell 调用引号 | **0 发现** | — |

r0 2×P1 要点：①夹具必须强制按当前提交重建（防本地陈旧构件使回归失真——与 SEC-012.B「陈旧构件环境坑」同源教训）；②Windows 下需 `mvn.cmd` 启动器；其余：HTTP 仅绑回环、ConfigRespVO key 字段对齐、受控拒绝断言收紧、响应体单次读取。

## 产品代码改动（最小化，归口注明）

仅 `3097b22f`（GAP-2，归口 **ZS-IAM-003**）：`PostServiceImpl.deletePost/deletePostList` 注入 `UserPostMapper` 新增 `selectCountByPostId` 引用计数（`DataPermissionUtils.executeIgnore` 包裹、保留租户过滤，循 IAM-003 r0 教训），新错误码 `POST_EXITS_USERS`（1-002-005-004），批量先全校验后删（对齐 deleteDeptList 模式）；H2 用例 3 新增（PostServiceImplTest 16/16）+ SYS-POST-N1 改断言新拒绝码。其余 6 提交均为 `scripts/sys001/**` 与夹具 yaml，产品零改动。

## 验证终态

- 矩阵：**48/49 PASS**（规避前 24~30 波动，GAP-1 级联 FAIL 全消）；唯一 FAIL = SYS-ROLE-N1，已定性为 **GAP-3**（见下）
- H2：PostServiceImplTest 16/16、TenantPackageMenuIntersectionTest 8/8、DictData 22/22、DictType 19/19
- `run-local-gates --fast` 10/10；`run-pg-regression` 8/8（首跑 2 套件 Docker 负载 flaky，按约定重试后全绿）
- 合并：`--no-ff` 合并回 main（`e3f5ef29`），合并后 PG 回归收口复验（本日）

## 缺口登记（报告 `outputs/sys001/report.md`，归口清楚）

| 编号 | 缺陷 | 归口 |
|---|---|---|
| GAP-1（已规避） | 真实 PG × MP 3.5.17 selectOne 光标查询 × Druid 1.2.28 语句包装「statement 已关闭」随机 500/401；夹具以 CLI 关 PSCache 规避 | 根因修复归 **ZS-DB-001** 依赖升级（与 BRAND-004.B 独立复现互证） |
| GAP-3 | 真实 PG 上 assign-role-menu 越界既不拒绝也不落库（PG 语句日志证实事务内无 INSERT 无异常；H2 同路径守卫通过；contains 判定自相矛盾），疑 JacksonTypeHandler 泛型擦除 × MP 3.5.17 组合；SYS-ROLE-N1 即此用例 | **ZS-CFG-003.B 跨 PG 复验 + ZS-DB-001**（依赖升级后以 SYS-ROLE-N1 复验；取证已附：PG 语句日志/trace/最小复现对照） |
| GAP-4 | menu 深层环校验缺失（当前仅拦自父） | **ZS-CFG-003.A/B** |
| GAP-5 | V1 个别序列 START 与种子行 off-by-one（system_dict_data_seq 撞 3449；夹具 setval 兜底） | **ZS-DB-004** |
| NOTE-1 | ModuleCatalog 无管理端点（不适用+依据）；公告富文本输出编码归 ZS-SYS-001.B（B06） | — |
