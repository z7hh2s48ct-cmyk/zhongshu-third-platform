# B06 联验本地环境启动指南（ZS-CLIENT-005.B / SYS-001.B / BRAND-003.B 共用）

> 组成：PG17 + Redis（Docker Compose，infra 层）+ zszj-server（宿主机 jar，local profile）
> 数据：全新库，首启由 Flyway 执行全迁移链（应用以 owner 连接；生产凭据分离见 ZS-DB-002）

## 1. 启动

```bash
cd services/zhongshu-core/deploy
cp env.local.example .env.local    # 首次；本地一次性口令，禁止复用生产值
docker compose -f docker-compose.local.yml --env-file .env.local up -d

# server（宿主机；首次必须带 --spring.flyway.enabled=true 执行全迁移链，此后可去掉）
cd ../zszj-server && mvn -pl :zszj-server -am package -DskipTests
cd target
ZSZJ_DATASOURCE_URL='jdbc:postgresql://127.0.0.1:15432/zhongshu' \
ZSZJ_DATASOURCE_USERNAME=zhongshu_owner \
ZSZJ_DATASOURCE_PASSWORD=local-lianyan-owner \
SPRING_BOOT_ADMIN_CLIENT_ENABLED=false \
"$JAVA_HOME/bin/java" -jar zszj-server.jar \
  --spring.profiles.active=local \
  --server.address=127.0.0.1 \
  --spring.data.redis.port=26379 \
  --spring.flyway.enabled=true
```

> **必须带 `--server.address=127.0.0.1`**（codex r0 P1）：local profile 默认监听 `:::`（全接口），
> 联验环境带默认管理员凭据——compose 只保护了 PG/Redis 回环，应用端口同样不得对外。

## 2. 冒烟

- `GET http://127.0.0.1:48080/actuator/health` → `{"status":"UP"}`
- `POST /admin-api/system/auth/login`（tenant-id: 1，`{"username":"admin","password":"admin123"}`）→ `code:0` + 双令牌

## 3. 端口与账号

| 项 | 值 | 说明 |
|---|---|---|
| PG | 127.0.0.1:15432（容器 5432） | 库 zhongshu；owner=zhongshu_owner；5432 被其他联测实例占用故避让 |
| Redis | 127.0.0.1:26379 | 无口令（仅 127.0.0.1）；避让 16379=单测嵌入式 Redis 约定端口 |
| server | 48080 | local profile（Quartz 关闭、Druid 控制台关闭） |
| admin 登录 | admin / admin123（tenant-id 1） | 用户/角色绑定为本环境一次性种子（凭据不入迁移链），SQL 见下 |

## 4. 已知注意

- **Outbox 积压告警（D-07 延后的正确行为）**：local profile 无 Quartz 且派发触发器属 D-07 联验延后项——E2E 登录/登出产生的 LOGIN-005.B 补偿事件会积累为 PENDING，随等待时间越过 OPS-002.B 阈值把 monitoring 组压 DOWN（告警机制正确）。重置联验数据：`docker exec zszj-local-postgres psql -U zhongshu_owner -d zhongshu -c "TRUNCATE outbox_event;"`（联验可弃数据）；这些事件本身是 LOGIN-005.B 补偿链在真实环境首次生成的证据，投递接线归 D-07

- **端口合同**：16379 是单元测试嵌入式 Redis 的约定端口（application-unit-test.yaml）——联验环境不得占用，否则 jedismock 绑定失败被 RedisTestConfiguration 静默吞掉后单测连上联验持久化 Redis，键跨运行累积产生假失败（本项目实测：sms 配额桶测试 8→11）

- `--spring.flyway.enabled=true`：application.yaml 默认 false（注释所指 ZSZJ_FLYWAY_ENABLED 激活机制在 prod 模板 profile），本地首启必须用命令行参数显式开启
- **MyBatis-Plus 版本合同（codex r0 订正）**：登录 500 根因是 MP 3.5.17 的 BaseMapper.selectOne 内置路由 selectWithCursor（依赖内置、javap 实证，非构建夹带陈旧构件），与 Druid+PG 冲突——已降级 3.5.12（feat 分支）。边界：MPJ 1.5.9 的 MPJBaseServiceImpl/JoinCrudRepository 父类引用 MP 3.5.13+ 的 com.baomidou.mybatisplus.spring.* 类，当前项目未使用这两个入口（E2E 全链验证启动正常），启用前须升级 MPJ 或固定验证
- 首启种子（一次性，未入迁移链——迁移基线按合同不含用户/凭据种子）；重建环境（`down -v`）后重放：

```sql
-- 宿主机执行：docker exec -i zszj-local-postgres psql -U zhongshu_owner -d zhongshu
-- ① admin 用户行（取自供应商基线 ruoyi-vue-pro.sql L5673，bcrypt 口令=admin123）
INSERT INTO system_users (id, username, password, nickname, remark, dept_id, post_ids, email, mobile, sex, avatar, status, login_ip, login_date, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 1, 'admin', '$2a$04$.vBUBOu06CW2pb1L7S9J3.R5FHOZtqXwW7EqhOLfzHCQzQvAHVq0X', '众墅', '管理员', 103, '[]', 'aoteman@126.com', '15612345678', 1, '', 0, '', NULL, '1', now(), '1', now(), 0, 1
WHERE NOT EXISTS (SELECT 1 FROM system_users WHERE username = 'admin');
-- ② 超级管理员角色绑定（幂等）
INSERT INTO system_user_role (id, user_id, role_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 1, 1, 1, '1', now(), '1', now(), 0, 1
WHERE NOT EXISTS (SELECT 1 FROM system_user_role WHERE user_id = 1 AND role_id = 1);
-- ③ 序列同步（显式 id 插入不推进序列；否则后续 user/create 主键冲突）
SELECT setval(pg_get_serial_sequence('system_users','id'), (SELECT COALESCE(MAX(id),0) FROM system_users));
SELECT setval(pg_get_serial_sequence('system_user_role','id'), (SELECT COALESCE(MAX(id),0) FROM system_user_role));
```
