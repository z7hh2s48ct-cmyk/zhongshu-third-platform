-- ZS-MSG-003.C：站内信消息业务组织归属列——org 轴消息可见范围的数据载体
-- （docs/05 line710「覆盖消息列表/正文/管理查询的范围」）。
-- 归属 = 派发时刻收件人的服务端组织上下文（默认任职组织，ZS-IAM-002 MembershipContextResolver）；
-- 无任职/任职失效/系统上下文为 NULL（MEMBER 命名空间不解任职上下文，归属恒 NULL）；
-- org 门与列表过滤仅对 organization_id 非空消息按组织范围裁决；
-- NULL 消息仅本人可见（对象门兜底），不出现在授权操作员的管理视图（超管/平台任职除外），
-- 存量 NULL 消息升级后同样适用（D-09 FND-AUTH-004：对象一旦归属某组织，本人所有权不凌驾组织排除）。
ALTER TABLE system_notify_message ADD COLUMN IF NOT EXISTS organization_id int8 NULL DEFAULT NULL;
CREATE INDEX IF NOT EXISTS idx_system_notify_message_02 ON system_notify_message (organization_id);
COMMENT ON COLUMN system_notify_message.organization_id IS '业务组织归属ID（ZS-MSG-003.C，org 轴消息可见范围载体；NULL=历史/无任职/系统上下文消息）';
