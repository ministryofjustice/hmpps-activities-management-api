-- Regime times for MDI
INSERT INTO prison_regime (prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
VALUES('MDI', '10:00:00', '11:00:00', '13:00:00', '16:30:00', '18:00:00', '20:00:00');

-- Activity: KITCHEN AM
insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (1, 'MDI', 1, 1, true, false, false, false, 'H', 'KITCHEN AM', 'Kitchen Morning', '2022-10-10', null, 'high', '2022-9-21 00:00:00', 'SEED USER', true);

insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (1, 1, 'BAS', 'Basic', 1, 125, 150, 1);

insert into activity_schedule(activity_schedule_id, activity_id, description, capacity, start_date)
values (1, 1, 'Kitchen AM Schedule', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, time_slot)
values (1, 1, '10:00:00', '11:00:00', true, 'AM');

-- Activity: GYM PM
insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (2, 'MDI', 1, 1, true, false, false, false, 'H', 'GYM PM', 'Gym Afternoon', '2022-10-10', null, 'high', '2022-9-21 00:00:00', 'SEED USER', true);

insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (2, 2, 'BAS', 'Basic', 1, 125, 150, 1);

insert into activity_schedule(activity_schedule_id, activity_id, description, capacity, start_date)
values (2, 2, 'Gym PM Schedule', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, time_slot)
values (2, 2, '14:00:00', '15:00:00', true, 'PM');

-- Allocations: A1234AA has KITCHEN AM and GYM PM
insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, allocated_time, allocated_by, prisoner_status)
values (1, 1, 'A1234AA', 10001, 1, '2022-10-10', '2022-10-10 09:01:00', 'MR BLOGS', 'ACTIVE');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, allocated_time, allocated_by, prisoner_status)
values (2, 2, 'A1234AA', 10001, 1, '2022-10-10', '2022-10-10 09:02:00', 'MR BLOGS', 'ACTIVE');

-- Allocation: G1234DX has KITCHEN AM only
insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, allocated_time, allocated_by, prisoner_status)
values (3, 1, 'G1234DX', 10002, 1, '2022-10-10', '2022-10-10 09:03:00', 'MR BLOGS', 'ACTIVE');

-- Allocation: A1234AC has KITCHEN AM (ENDED) and GYM PM (ACTIVE)
insert into allocation(allocation_id,activity_schedule_id,prisoner_number,booking_id,prison_pay_band_id,start_date,end_date,allocated_time,allocated_by,prisoner_status)
values (4,1,'A1234AC',10001,1,'2022-10-10','2026-05-20','2022-10-10 09:04:00','MR BLOGS','ENDED');

insert into allocation(allocation_id,activity_schedule_id,prisoner_number,booking_id,prison_pay_band_id,start_date,allocated_time,allocated_by,prisoner_status)
values (5,2,'A1234AC',10001,1,'2022-10-10','2022-10-10 09:05:00','MR BLOGS','ACTIVE');

-- Event reviews
insert into event_review (event_review_id, service_identifier, event_type, event_time, prison_code, prisoner_number, booking_id, event_data)
values (1, 'myservice', 'event-name', '2023-5-10 10:20:00', 'MDI', 'A1234AA', 1, 'aaaaaaa');

insert into event_review (event_review_id, service_identifier, event_type, event_time, prison_code, prisoner_number, booking_id, event_data)
values (2, 'myservice', 'event-name', '2023-5-10 10:21:00', 'MDI', 'G1234DX', 1, 'aaaaaaa');

-- G1234DD has no allocations
insert into event_review (event_review_id, service_identifier, event_type, event_time, prison_code, prisoner_number, booking_id, event_data)
values (3, 'myservice', 'event-name', '2023-5-10 10:22:00', 'MDI', 'G1234DD', 1, 'aaaaaaa');

-- A1234AC has 2 allocations KITCHEN AM which has ENDED and GYM PM which is ACTIVE
insert into event_review (event_review_id, service_identifier, event_type, event_time, prison_code, prisoner_number, booking_id, event_data)
values (4, 'myservice', 'event-name', '2023-5-10 10:23:00', 'MDI', 'A1234AC', 1, 'aaaaaaa');