alter table activity_schedule_slot add column time_slot char(2) not null;

alter table scheduled_instance add column time_slot char(2) not null;

alter table exclusion add column time_slot char(2) not null;

drop view v_activity_time_slot;