ALTER TYPE tg_file ADD ATTRIBUTE thumb varchar(256);
ALTER TABLE rename_queue ADD COLUMN thumb tg_file;