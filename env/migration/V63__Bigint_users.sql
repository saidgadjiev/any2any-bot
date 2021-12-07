ALTER TABLE user_settings ALTER COLUMN user_id TYPE BIGINT;

ALTER TABLE bulk_distribution ALTER COLUMN user_id TYPE BIGINT;

ALTER TABLE black_list ALTER COLUMN user_id TYPE BIGINT;

DROP TABLE IF EXISTS distribution;

DROP TABLE IF EXISTS distribution_message;
