alter table paid_subscription rename column end_date to end_at;
alter table paid_subscription rename column purchase_date to purchased_at;
alter table paid_subscription alter column end_at set data type timestamp(0);
