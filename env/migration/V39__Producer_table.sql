ALTER TABLE downloading_queue ADD COLUMN producer_table varchar(64);
UPDATE ownloading_queue SET producer_table = producer;
ALTER TABLE downloading_queue ALTER COLUMN producer_table SET NOT NULL;

ALTER TABLE upload_queue ADD COLUMN producer_table varchar(64);
UPDATE upload_queue SET producer_table = producer;
ALTER TABLE upload_queue ALTER COLUMN producer_table SET NOT NULL;