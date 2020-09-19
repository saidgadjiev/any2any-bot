ALTER TABLE unzip_queue ALTER COLUMN file SET DATA TYPE TEXT;
ALTER TABLE rename_queue ALTER COLUMN file SET DATA TYPE TEXT;
ALTER TABLE rename_queue ALTER COLUMN thumb SET DATA TYPE text;
ALTER TABLE archive_queue ALTER COLUMN files SET DATA TYPE TEXT[];
ALTER TABLE meta_queue ALTER COLUMN file SET DATA TYPE TEXT;
ALTER TABLE meta_queue ALTER COLUMN thumb SET DATA TYPE text;

ALTER TYPE tg_file ALTER ATTRIBUTE file_id SET DATA TYPE text;

ALTER TABLE unzip_queue ALTER COLUMN file SET DATA TYPE tg_file USING file::tg_file;
ALTER TABLE rename_queue ALTER COLUMN file SET DATA TYPE tg_file USING file::tg_file;
ALTER TABLE rename_queue ALTER COLUMN thumb SET DATA TYPE tg_file USING file::tg_file;
ALTER TABLE archive_queue ALTER COLUMN files SET DATA TYPE tg_file[] USING files::tg_file[];
ALTER TABLE meta_queue ALTER COLUMN file SET DATA TYPE tg_file USING file::tg_file;
ALTER TABLE meta_queue ALTER COLUMN thumb SET DATA TYPE tg_file USING file::tg_file;

ALTER TYPE tg_file ADD ATTRIBUTE format VARCHAR(32);

ALTER TABLE conversion_queue
    ADD COLUMN files tg_file[];

UPDATE conversion_queue cq2
SET files = ARRAY [cq.val::tg_file]
FROM (SELECT id,
             '("' || file_id || '","' || COALESCE(mime_type, '') || '", "' || COALESCE(file_name, '') || '", ' || size || ',,"' || format ||
             '")' as val
      FROM conversion_queue) cq
WHERE cq.id = cq2.id;

ALTER TABLE conversion_queue ALTER COLUMN files SET NOT NULL;

ALTER TABLE conversion_queue DROP file_id;
ALTER TABLE conversion_queue DROP mime_type;
ALTER TABLE conversion_queue DROP file_name;
ALTER TABLE conversion_queue DROP size;
ALTER TABLE conversion_queue DROP format;