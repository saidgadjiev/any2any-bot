CREATE TABLE IF NOT EXISTS video_watermark (
    user_id INT NOT NULL PRIMARY KEY,
    type VARCHAR(16) NOT NULL,
    position VARCHAR(32) NOT NULL,
    wtext text,
    image tg_file,
    font_size INT,
    color VARCHAR(32),
    image_height INT,
    transparency VARCHAR(5)
);