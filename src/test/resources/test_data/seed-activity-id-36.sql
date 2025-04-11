insert into prison_regime (prison_regime_id, prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
values(3, 'PVI', '10:00:00', '11:00:00', '13:00:00', '16:30:00', '18:00:00', '20:00:00');

insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, activity_organiser_id, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (1, 'PVI', 1, 2, 1, 'Maths', 'Maths Level 1', current_date + interval '1' day, null, 'high', '2022-9-21 00:00:00', 'SEED USER', false);

insert into activity_schedule(activity_schedule_id, activity_id, description, capacity, start_date)
values (1, 1, 'Maths AM', 10, current_date + interval '1' day);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, prisoner_status)
values (1, 1, 'A11111A', 10001, current_date + interval '1' day, null, '2022-10-10 09:00:00', 'MR BLOGS', null, 'ACTIVE');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, prisoner_status)
values (2, 1, 'B22222B', 10002, current_date + interval '3' day, null, '2022-10-10 09:00:00', 'MR BLOGS', null, 'ACTIVE');


insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, activity_organiser_id, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (2, 'PVI', 1, 2, 1, 'Kitchen', 'Kitchen', current_date + interval '1' day, null, 'high', '2022-9-21 00:00:00', 'SEED USER', false);

insert into activity_schedule(activity_schedule_id, activity_id, description, capacity, start_date)
values (2, 2, 'Maths AM', 10, current_date + interval '1' day);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, prisoner_status)
values (3, 2, 'C33333C', 10003, current_date + interval '1' day, current_date + interval '1' day, '2022-10-10 09:00:00', 'MR BLOGS', null, 'ACTIVE');



insert into prison_regime (prison_regime_id, prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
values(4, 'RSI', '10:00:00', '11:00:00', '13:00:00', '16:30:00', '18:00:00', '20:00:00');

insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, activity_organiser_id, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (100, 'RSI', 1, 2, 1, 'English', 'English Level 1', current_date + interval '1' day, null, 'high', '2022-9-21 00:00:00', 'SEED USER', false);

insert into activity_schedule(activity_schedule_id, activity_id, description, capacity, start_date)
values (100, 100, 'English AM', 10, current_date + interval '1' day);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, prisoner_status)
values (100, 100, 'Z99999Z', 99999, current_date + interval '1' day, null, '2022-10-10 09:00:00', 'MR BLOGS', null, 'ACTIVE');
