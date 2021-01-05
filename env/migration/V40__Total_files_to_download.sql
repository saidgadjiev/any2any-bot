ALTER TABLE archive_queue ADD COLUMN total_files_to_download INT NOT NULL DEFAULT 0;
ALTER TABLE archive_queue DROP COLUMN total_file_size;