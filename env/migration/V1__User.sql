CREATE TABLE IF NOT EXISTS tg_user (
    user_id INT NOT NULL UNIQUE PRIMARY KEY,
    created_at TIMESTAMP(0) DEFAULT now(),
    last_logged_at TIMESTAMP(0) DEFAULT now()
);