CREATE TABLE IF NOT EXISTS rename_queue (
    id SERIAL PRIMARY KEY,
    created_at TIMESTAMP DEFAULT NOW() NOT NULL,
    user_id INT NOT NULL,
    file_id VARCHAR(128) NOT NULL,
    file_name VARCHAR(256) NOT NULL,
    new_file_name VARCHAR(256) NOT NULL,
    mime_type VARCHAR(64) NOT NULL,
    reply_to_message_id INT,
    status INT NOT NULL DEFAULT 0
);