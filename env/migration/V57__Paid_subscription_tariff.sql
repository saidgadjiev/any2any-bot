create table if not exists paid_subscription_tariff
(
    tariff_type VARCHAR(32) NOT NULL PRIMARY KEY,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort INT NOT NULL DEFAULT 0
);

alter table paid_subscription_plan add column tariff VARCHAR(32) REFERENCES paid_subscription_tariff(tariff_type);

alter table paid_subscription alter column end_date drop not null;

insert into paid_subscription_tariff(tariff_type, active, sort) VALUES ('fixed', true, 0);

insert into paid_subscription_tariff(tariff_type, active, sort) VALUES ('flexible', true, 1);

update paid_subscription_plan set tariff = 'fixed';

insert into paid_subscription_plan (currency, price, period, tariff, active)
values ('USD', 2, interval '1 month', 'flexible', true);

INSERT INTO paid_subscription_plan(currency, price, period, tariff, active)
VALUES ('usd', 6, interval '3 months', 'flexible', true);

INSERT INTO paid_subscription_plan(currency, price, period, tariff, active)
VALUES ('usd', 12, interval '6 months', 'flexible', true);

INSERT INTO paid_subscription_plan(currency, price, period, tariff, active)
VALUES ('usd', 20, interval '1 years', 'flexible', true);

alter table paid_subscription_plan alter column tariff set not null;

alter table paid_subscription add column subscription_interval interval;
