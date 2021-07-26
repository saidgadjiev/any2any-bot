CREATE TABLE IF NOT EXISTS user_settings
(
    user_id    BIGINT     not null,
    bot_name   VARCHAR(32) not null,
    smart_file BOOLEAN     not null,
    PRIMARY KEY (user_id, bot_name)
);

