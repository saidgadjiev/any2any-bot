CREATE TABLE IF NOT EXISTS downloading_queue (
    file tg_file,
    producer varchar(64) NOT NULL,
    producer_id INT NOT NULL,
    progress TEXT,
    file_path VARCHAR(1014),
    delete_parent_dir BOOLEAN NOT NULL DEFAULT FALSE,
    next_run_at TIMESTAMP(0) NOT NULL DEFAULT now()
) inherits (queue);