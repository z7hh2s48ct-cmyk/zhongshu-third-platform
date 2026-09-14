-- ZS-FILE-004.A：主体绑定交付票据/下载会话表——一次性票据原子兑换与后端鉴权取流的持久化基础。
-- 票据只存 SHA-256 散列（不明文）；兑换在消费 SQL 上带 owner·tenant·purpose·status·有效期谓词（供体
-- JdbcDeliveryPort 缺失这些匹配谓词，不能原样当完整授权）。表为全局表（@TenantIgnore，跨租户兑换须能
-- 定位票据行并按谓词显式拒绝，而非被租户插件静默过滤成「不存在」），tenant_id 由服务层显式写入与校验。
CREATE SEQUENCE IF NOT EXISTS infra_file_delivery_ticket_seq START 1;
CREATE TABLE IF NOT EXISTS infra_file_delivery_ticket (
    id int8 NOT NULL DEFAULT nextval('infra_file_delivery_ticket_seq'),
    ticket_hash varchar(64) NOT NULL,
    file_id int8 NOT NULL,
    owner_user_id int8 NOT NULL,
    purpose varchar(16) NOT NULL,
    status varchar(16) NOT NULL,
    delivery_session_id varchar(64) NULL,
    login_session varchar(128) NULL,
    tenant_id int8 NOT NULL,
    expires_time timestamp NOT NULL,
    redeem_time timestamp NULL,
    creator varchar(64) NULL DEFAULT '',
    create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) NULL DEFAULT '',
    update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted int2 NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_file_delivery_ticket_hash ON infra_file_delivery_ticket (ticket_hash);
CREATE INDEX IF NOT EXISTS idx_file_delivery_ticket_session ON infra_file_delivery_ticket (delivery_session_id);
CREATE INDEX IF NOT EXISTS idx_file_delivery_ticket_file ON infra_file_delivery_ticket (file_id);
