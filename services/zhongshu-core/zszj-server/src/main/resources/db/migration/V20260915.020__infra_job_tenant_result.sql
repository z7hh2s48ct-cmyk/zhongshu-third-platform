-- ZS-JOB-001：定时任务租户级执行结果明细表——让「某个租户失败」可单独查询与补偿，不被整体执行结果掩盖。
-- 一次多租户 Job 执行汇总为一条 infra_job_log，本表按「执行日志 + 技术租户」逐行展开：
-- success 逐租户判定、duration_ms 逐租户计时、result_summary/error_summary 由服务层先脱敏后截断（列宽 512 与应用侧一致）。
-- 表为全局表（DO 标 @TenantIgnore）：tenant_id 记录的是「被执行的那个技术租户」，而非当前线程上下文租户，
-- 一次执行会写入多行不同 tenant_id 的数据，因此不能让租户插件按上下文过滤或回填，tenant_id 由服务层显式写入。
-- (job_log_id, tenant_id) 唯一：同一次执行内每租户至多一行，作为重复写入的幂等护栏。
CREATE SEQUENCE IF NOT EXISTS infra_job_tenant_result_seq START 1;
CREATE TABLE IF NOT EXISTS infra_job_tenant_result (
    id int8 NOT NULL DEFAULT nextval('infra_job_tenant_result_seq'),
    job_log_id int8 NOT NULL,
    tenant_id int8 NOT NULL,
    success bool NOT NULL DEFAULT FALSE,
    duration_ms int8 NOT NULL DEFAULT 0,
    result_summary varchar(512) NULL DEFAULT '',
    error_summary varchar(512) NULL DEFAULT '',
    creator varchar(64) NULL DEFAULT '',
    create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) NULL DEFAULT '',
    update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted int2 NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_job_tenant_result UNIQUE (job_log_id, tenant_id),
    CONSTRAINT ck_job_tenant_result_duration CHECK (duration_ms >= 0)
);
-- 按执行日志查明细（含唯一约束背后的访问路径），以及按租户回查失败历史
CREATE INDEX IF NOT EXISTS idx_job_tenant_result_log ON infra_job_tenant_result (job_log_id);
CREATE INDEX IF NOT EXISTS idx_job_tenant_result_tenant ON infra_job_tenant_result (tenant_id, success);
