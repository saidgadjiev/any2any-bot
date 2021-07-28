create table if not exists paid_subscription_tariff
(
    tariff_type VARCHAR(32) NOT NULL PRIMARY KEY,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort INT NOT NULL DEFAULT 0
);

alter table paid_subscription_plan add column tariff VARCHAR(32) REFERENCES paid_subscription_tariff(tariff_type);

insert into paid_subscription_tariff(tariff_type, active, sort) VALUES ('fixed', true, 0);

insert into paid_subscription_tariff(tariff_type, active, sort) VALUES ('flexible', true, 1);

update paid_subscription_plan set tariff = 'fixed';

alter table paid_subscription_plan alter column tariff set not null;
