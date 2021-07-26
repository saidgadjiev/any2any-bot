ALTER TABLE upload_queue
    ADD COLUMN IF NOT EXISTS thumb_file_id TEXT;
ALTER TABLE upload_queue
    ADD COLUMN IF NOT EXISTS custom_file_name TEXT;
ALTER TABLE upload_queue
    ADD COLUMN IF NOT EXISTS custom_caption TEXT;
ALTER TABLE upload_queue
    ADD COLUMN IF NOT EXISTS custom_thumb tg_file;