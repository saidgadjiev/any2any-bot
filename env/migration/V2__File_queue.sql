CREATE TABLE IF NOT EXISTS file_queue (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES tg_user(user_id) ON DELETE RESTRICT,
    file_id TEXT NOT NULL,
    file_name VARCHAR(256),
    size INT NOT NULL,
    mime_type VARCHAR(256),
    message_id INT NOT NULL,
    created_at TIMESTAMP(0) DEFAULT NOW(),
    format VARCHAR(32) NOT NULL,
    target_format VARCHAR(32) NOT NULL
);