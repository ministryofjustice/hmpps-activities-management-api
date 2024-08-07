alter table activity_schedule_slot add column time_slot char(2) not null;

alter table scheduled_instance add column time_slot char(2) not null;

alter table exclusion add column time_slot char(2) not null;

drop view v_activity_time_slot cascade;

CREATE TABLE exclusion_days_of_week (
    id                          serial primary key,
    exclusion_id bigint references exclusion (exclusion_id),
    day_of_week             varchar(10)
);

create index exclusion_days_of_week_idx on exclusion_days_of_week(exclusion_id);

alter table exclusion drop column monday_flag cascade,
                      drop column tuesday_flag cascade,
                      drop column wednesday_flag cascade,
                      drop column thursday_flag cascade,
                      drop column friday_flag cascade,
                      drop column saturday_flag cascade,
                      drop column sunday_flag cascade,
                      drop column slot_start_time cascade,
                      drop column slot_end_time cascade;