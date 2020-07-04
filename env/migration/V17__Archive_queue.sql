CREATE TABLE IF NOT EXISTS archive_queue (
    id SERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    user_id INT NOT NULL,
    files tg_file[] not null,
    type VARCHAR(32) not null,
    status INT DEFAULT 0 NOT NULL
);