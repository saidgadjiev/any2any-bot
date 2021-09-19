create table if not exists audio_watermark
(
    user_id bigint  not null primary key,
    audio   tg_file not null
);
