create table if not exists tutorial(
    id SERIAL NOT NULL PRIMARY KEY,
    file_id TEXT NOT NULL,
    bot_name VARCHAR(64) NOT NULL,
    description TEXT NOT NULL,
    cmd VARCHAR(64) NOT NULL
);