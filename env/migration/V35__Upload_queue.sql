CREATE TABLE IF NOT EXISTS upload_queue
(
    producer    varchar(64) NOT NULL,
    producer_id INT         NOT NULL,
    progress    TEXT,
    method      VARCHAR(64) NOT NULL,
    body        text        NOT NULL,
    extra       text
) inherits (queue);
