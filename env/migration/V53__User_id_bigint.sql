ALTER TABLE tg_user ALTER COLUMN user_id TYPE BIGINT;

ALTER TABLE user_bot ALTER COLUMN user_id TYPE BIGINT;

ALTER TABLE paid_subscription ALTER COLUMN user_id TYPE BIGINT;

ALTER TABLE bulk_distribution ALTER COLUMN user_id TYPE BIGINT;

ALTER TABLE queue ALTER COLUMN user_id TYPE BIGINT;

ALTER TABLE conversion_report ALTER COLUMN user_id TYPE BIGINT;

ALTER TABLE video_watermark ALTER COLUMN user_id TYPE BIGINT;