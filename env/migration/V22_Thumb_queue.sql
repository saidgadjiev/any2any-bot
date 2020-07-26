CREATE TABLE IF NOT EXISTS thumb_queue
(
    id                  SERIAL PRIMARY KEY,
    created_at          TIMESTAMP             DEFAULT NOW() NOT NULL,
    user_id             INT          NOT NULL,
    file                tg_file      NOT NULL,
    thumb               tg_file NOT NULL,
    reply_to_message_id INT,
    status              INT          NOT NULL DEFAULT 0
);