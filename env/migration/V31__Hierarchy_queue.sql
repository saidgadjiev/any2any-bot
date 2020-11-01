CREATE TABLE queue
(
    id                       SERIAL PRIMARY KEY,
    user_id                  INT NOT NULL,
    reply_to_message_id      INT,
    progress_message_id      INT,
    suppress_user_exceptions BOOLEAN      DEFAULT FALSE NOT NULL,
    created_at               TIMESTAMP(0) NOT NULL DEFAULT NOW(),
    started_at               TIMESTAMP(0),
    last_run_at              TIMESTAMP(0),
    completed_at             TIMESTAMP(0),
    status                   INT NOT NULL DEFAULT 0,
    exception                TEXT
);

ALTER TABLE conversion_queue INHERIT queue;

DELETE FROM rename_queue;
ALTER TABLE rename_queue ALTER COLUMN created_at TYPE TIMESTAMP(0);
ALTER TABLE rename_queue ADD COLUMN last_run_at TIMESTAMP(0);
ALTER TABLE rename_queue ADD COLUMN completed_at TIMESTAMP(0);
ALTER TABLE rename_queue INHERIT queue;

DELETE FROM unzip_queue;
ALTER TABLE unzip_queue ALTER COLUMN created_at TYPE TIMESTAMP(0);
ALTER TABLE unzip_queue ADD COLUMN started_at TIMESTAMP(0);
ALTER TABLE unzip_queue ADD COLUMN last_run_at TIMESTAMP(0);
ALTER TABLE unzip_queue ADD COLUMN completed_at TIMESTAMP(0);
ALTER TABLE unzip_queue ADD COLUMN exception TEXT;
ALTER TABLE unzip_queue ADD COLUMN reply_to_message_id INT;
ALTER TABLE unzip_queue ADD COLUMN progress_message_id INT;
ALTER TABLE unzip_queue ADD COLUMN suppress_user_exceptions BOOLEAN      DEFAULT FALSE NOT NULL;
ALTER TABLE unzip_queue DROP COLUMN message_id;
ALTER TABLE unzip_queue INHERIT queue;

DELETE FROM archive_queue;
ALTER TABLE archive_queue ADD COLUMN reply_to_message_id INT;
ALTER TABLE archive_queue ADD COLUMN suppress_user_exceptions BOOLEAN      DEFAULT FALSE NOT NULL;
ALTER TABLE archive_queue ALTER COLUMN created_at TYPE TIMESTAMP(0);
ALTER TABLE archive_queue ADD COLUMN started_at TIMESTAMP(0);
ALTER TABLE archive_queue ADD COLUMN last_run_at TIMESTAMP(0);
ALTER TABLE archive_queue ADD COLUMN completed_at TIMESTAMP(0);
ALTER TABLE archive_queue ADD COLUMN exception TEXT;
ALTER TABLE archive_queue INHERIT queue;




