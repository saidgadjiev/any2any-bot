CREATE TABLE IF NOT EXISTS meta_queue
(
    id                  SERIAL PRIMARY KEY,
    created_at          TIMESTAMP        DEFAULT NOW() NOT NULL,
    user_id             INT     NOT NULL,
    file                tg_file NOT NULL,
    new_file_name       VARCHAR(256),
    mode                INT     NOT NULL DEFAULT 1,
    progress_message_id INT,
    thumb               tg_file,
    thumb_size          varchar(32),
    reply_to_message_id INT,
    status              INT     NOT NULL DEFAULT 0
);