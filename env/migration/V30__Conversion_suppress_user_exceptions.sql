ALTER TABLE conversion_queue ADD COLUMN IF NOT EXISTS suppress_user_exceptions BOOLEAN DEFAULT FALSE NOT NULL;

UPDATE conversion_queue SET suppress_user_exceptions = true WHERE status NOT IN(0, 1, 3);