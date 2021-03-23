DELETE FROM conversion_report;
ALTER TABLE conversion_report ADD CONSTRAINT conversion_report_unique_queue_item_id UNIQUE (queue_item_id);