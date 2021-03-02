alter table downloading_queue
    add column if not exists synced_1 boolean not null default false;

alter table downloading_queue
    add column if not exists synced_2 boolean not null default false;

alter table upload_queue
    add column if not exists synced boolean not null default false;

alter type tg_file add attribute thumb_size BIGINT;

alter table queue
    ADD COLUMN IF NOT EXISTS server INT NOT NULL DEFAULT 1;
