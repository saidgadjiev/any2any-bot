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