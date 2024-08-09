alter table activity_schedule_slot add column time_slot char(2);

update activity_schedule_slot set time_slot = 'AM' where start_time < '11:00';
update activity_schedule_slot set time_slot = 'ED' where start_time > '17:00';
update activity_schedule_slot set time_slot = 'PM' where start_time > '13:00' and start_time < '17:00';

alter table scheduled_instance add column time_slot char(2);

update scheduled_instance set time_slot = 'AM' where start_time < '11:00';
update scheduled_instance set time_slot = 'ED' where start_time >= '17:00';
update scheduled_instance set time_slot = 'PM' where start_time >= '13:00' and start_time < '17:00';

alter table exclusion add column time_slot char(2);

drop view v_activity_time_slot cascade;

CREATE TABLE exclusion_days_of_week (
    id                          serial primary key,
    exclusion_id bigint references exclusion (exclusion_id),
    day_of_week             varchar(10)
);

create index exclusion_days_of_week_idx on exclusion_days_of_week(exclusion_id);


insert into exclusion_days_of_week(exclusion_id, day_of_week)
select exclusion_id, 'MONDAY' from exclusion where monday_flag is true;

insert into exclusion_days_of_week(exclusion_id, day_of_week)
select exclusion_id, 'TUESDAY' from exclusion where tuesday_flag is true;

insert into exclusion_days_of_week(exclusion_id, day_of_week)
select exclusion_id, 'WEDNESDAY' from exclusion where wednesday_flag is true;

insert into exclusion_days_of_week(exclusion_id, day_of_week)
select exclusion_id, 'THURSDAY' from exclusion where thursday_flag is true;

insert into exclusion_days_of_week(exclusion_id, day_of_week)
select exclusion_id, 'FRIDAY' from exclusion where friday_flag is true;

insert into exclusion_days_of_week(exclusion_id, day_of_week)
select exclusion_id, 'SATURDAY' from exclusion where saturday_flag is true;

insert into exclusion_days_of_week(exclusion_id, day_of_week)
select exclusion_id, 'SUNDAY' from exclusion where sunday_flag is true;


alter table exclusion drop column monday_flag cascade,
                      drop column tuesday_flag cascade,
                      drop column wednesday_flag cascade,
                      drop column thursday_flag cascade,
                      drop column friday_flag cascade,
                      drop column saturday_flag cascade,
                      drop column sunday_flag cascade,
                      drop column slot_start_time cascade,
                      drop column slot_end_time cascade;

alter table exclusion alter column time_slot set not null;
alter table scheduled_instance alter column time_slot set not null;
alter table activity_schedule_slot alter column time_slot set not null;