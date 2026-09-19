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
  --spring.data.redis.port=16379 \
  --spring.flyway.enabled=true
```

## 2. 冒烟

- `GET http://127.0.0.1:48080/actuator/health` → `{"status":"UP"}`
- `POST /admin-api/system/auth/login`（tenant-id: 1，`{"username":"admin","password":"admin123"}`）→ `code:0` + 双令牌

## 3. 端口与账号

| 项 | 值 | 说明 |
|---|---|---|
| PG | 127.0.0.1:15432（容器 5432） | 库 zhongshu；owner=zhongshu_owner；5432 被其他联测实例占用故避让 |
| Redis | 127.0.0.1:16379 | 无口令（仅 127.0.0.1） |
| server | 48080 | local profile（Quartz 关闭、Druid 控制台关闭） |
| admin 登录 | admin / admin123（tenant-id 1） | 用户/角色绑定为本环境一次性种子（凭据不入迁移链），SQL 见下 |

## 4. 已知注意

- `--spring.flyway.enabled=true`：application.yaml 默认 false（注释所指 ZSZJ_FLYWAY_ENABLED 激活机制在 prod 模板 profile），本地首启必须用命令行参数显式开启
- **增量构建可能夹带陈旧构件**（本环境首启曾出现源码中已不存在的 `selectWithCursor` 语句导致登录 500）——遇诡异运行时行为先 `clean package` 全量重建
- 首启种子（一次性，未入迁移链）：admin 用户取自供应商基线行 + `system_user_role(id=1, user 1↔role 1 super_admin)`；升级联验环境重建时需重放这两条种子
