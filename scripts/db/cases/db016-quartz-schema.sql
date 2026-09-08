-- =====================================================================
-- ZS-DB-016：Quartz PG 持久化链路——调度表与 PG 委托语句的结构级验证
-- （完整调度运行验收：创建/执行/暂停/恢复/重启恢复需应用运行，随 B02 收口）
-- 断言基准：Quartz PostgreSQLDelegate/JDBCStore 在 V1 qrtz_* 表上的核心语句
-- =====================================================================

-- 1. LOCKS 表：委托获取信号量的核心语句（SELECT ... FOR UPDATE）
INSERT INTO qrtz_locks (sched_name, lock_name) VALUES
  ('zszjScheduler', 'TRIGGER_ACCESS'),
  ('zszjScheduler', 'STATE_ACCESS'),
  ('zszjScheduler', 'CALENDAR_ACCESS');
SELECT lock_name FROM qrtz_locks WHERE sched_name = 'zszjScheduler' AND lock_name = 'TRIGGER_ACCESS' FOR UPDATE;

-- 2. 任务明细：插入/查询 JobDetail
INSERT INTO qrtz_job_details (sched_name, job_name, job_group, description, job_class_name, is_durable, is_nonconcurrent, is_update_data, requests_recovery, job_data)
VALUES ('zszjScheduler', 'db016Job', 'db016Group', 'ZS-DB-016 验证任务', 'cn.zszj.framework.quartz.core.JobHandlerInvoker', 'f', 'f', 'f', 'f', '{}');
SELECT job_name FROM qrtz_job_details WHERE sched_name = 'zszjScheduler' AND job_name = 'db016Job';

-- 3. 触发器：插入 + 状态流转（WAITING → ACQUIRED → 完成）
INSERT INTO qrtz_triggers (sched_name, trigger_name, trigger_group, job_name, job_group, description, next_fire_time, trigger_state, trigger_type, start_time, misfire_instr)
VALUES ('zszjScheduler', 'db016Trigger', 'db016Group', 'db016Job', 'db016Group', 'ZS-DB-016', 1770000000000, 'WAITING', 'CRON', 1770000000000, 0);
UPDATE qrtz_triggers SET trigger_state = 'ACQUIRED' WHERE sched_name = 'zszjScheduler' AND trigger_name = 'db016Trigger';
SELECT trigger_state FROM qrtz_triggers WHERE sched_name = 'zszjScheduler' AND trigger_name = 'db016Trigger';

-- 4. Cron 触发子表
INSERT INTO qrtz_cron_triggers (sched_name, trigger_name, trigger_group, cron_expression, time_zone_id)
VALUES ('zszjScheduler', 'db016Trigger', 'db016Group', '0 0/5 * * * ?', 'Asia/Shanghai');
SELECT cron_expression FROM qrtz_cron_triggers WHERE sched_name = 'zszjScheduler' AND trigger_name = 'db016Trigger';

-- 5. 清理（对应运行期删除语义；先删子表 cron_triggers 再删主表，遵守外键）
DELETE FROM qrtz_cron_triggers WHERE sched_name = 'zszjScheduler' AND trigger_name = 'db016Trigger';
DELETE FROM qrtz_triggers WHERE sched_name = 'zszjScheduler' AND trigger_name = 'db016Trigger';
DELETE FROM qrtz_job_details WHERE sched_name = 'zszjScheduler' AND job_name = 'db016Job';
