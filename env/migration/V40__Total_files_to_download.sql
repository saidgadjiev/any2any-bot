ALTER TABLE archive_queue ADD COLUMN IF NOT EXISTS archive_file_path VARCHAR(1014);
ALTER TABLE archive_queue DROP COLUMN IF EXISTS total_file_size;
ALTER TABLE archive_queue ADD COLUMN IF NOT EXISTS archive_is_ready BOOLEAN NOT NULL DEFAULT FALSE;