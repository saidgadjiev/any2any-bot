alter table downloading_queue
    add column if not exists synced_3 boolean not null default false;
