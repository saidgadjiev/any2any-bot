CREATE TABLE IF NOT EXISTS file_queue (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES tg_user(user_id) ON DELETE RESTRICT,
    file_id VARCHAR(128) NOT NULL,
    file_name VARCHAR(256),
    mime_type VARCHAR(128) NOT NULL,
    size INT NOT NULL,
    message_id INT NOT NULL,
    created_at TIMESTAMP(0) DEFAULT NOW(),
    target_format VARCHAR(32)
);