alter table activity_schedule_slot add column time_slot char(2) not null;

alter table scheduled_instance add column time_slot char(2) not null;

alter table exclusion add column time_slot char(2) not null;

drop view v_activity_time_slot;

CREATE TABLE exclusion_days_of_week (
    id                          serial primary key,
    exclusion_id bigint references exclusion (exclusion_id),
    day_of_week             varchar(10)
);

create index exclusion_days_of_week_idx on exclusion_days_of_week(exclusion_id);

alter table exclusion drop column monday_flag,
                      drop column tuesday_flag,
                      drop column wednesday_flag,
                      drop column thursday_flag,
                      drop column friday_flag,
                      drop column saturday_flag,
                      drop column sunday_flag,
                      drop column slot_start_time,
                      drop column slot_end_time;