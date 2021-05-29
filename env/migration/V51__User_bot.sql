CREATE TABLE user_bot(
    user_id int not null,
    bot_name varchar(32) not null,
    primary key (user_id, bot_name)
);

CREATE TABLE bulk_distribution(
    id serial primary key not null,
    user_id int not null,
    bot_name varchar(32) not null,
    message_ru text not null,
    message_en text not null,
    message_uz text not null
);