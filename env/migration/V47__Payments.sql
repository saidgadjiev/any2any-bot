CREATE table if not exists paid_subscription_plan
(
    id       SERIAL PRIMARY KEY    NOT NULL,
    currency VARCHAR(10)           NOT NULL,
    price    double precision      NOT NULL,
    period   interval              NOT NULL,
    active   BOOLEAN DEFAULT false NOT NULL
);

create table if not exists paid_subscription
(
    user_id  INT         NOT NULL PRIMARY KEY,
    end_date date        NOT NULL,
    bot_name VARCHAR(32) NOT NULL,
    purchase_date timestamp(0) default now() not null,
    plan_id  INT REFERENCES paid_subscription_plan (id)
);

INSERT INTO paid_subscription_plan(currency, price, period, active)
VALUES ('usd', 1.45, interval '1 months', true);
