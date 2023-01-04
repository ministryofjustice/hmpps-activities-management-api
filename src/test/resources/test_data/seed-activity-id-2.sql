insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, summary, description, start_date, end_date, risk_level, minimum_incentive_level, created_time, created_by)
values (2, 'PVI', 2, 2, true, false, false, false, 'English', 'English Level 2', '2022-10-21', null, null, null, '2022-9-21 00:00:00', 'SEED USER');

insert into activity_pay(activity_pay_id, activity_id, incentive_level, pay_band, rate, piece_rate, piece_rate_items)
values (2, 2, 'Basic', 'A', 75, 0, 0);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity)
values (3, 2, 'English AM', 3, 'L3', 'Location 3', 10);

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag)
values (1, 3, '10:00:00', '11:00:00', true);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity)
values (4, 2, 'English PM', 4, 'L4', 'Location 4', 10);

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag)
values (2, 4, '14:00:00', '15:00:00', true);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, pay_band, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (5, 3, 'B11111B', 10001, 'A', '2022-10-21', null, '2022-10-21 00:00:00', 'MR BLOGS', null, null, null);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, pay_band, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (6, 3, 'B22222B', 10002, 'B', '2022-10-21', null, '2022-10-21 00:00:00', 'MRS BLOGS', null, null, null);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, pay_band, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (7, 4, 'B11111B', 10001, 'C', '2022-10-21', null, '2022-10-21 00:00:00', 'MR BLOGS', null, null, null);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, pay_band, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (8, 4, 'B22222B', 10002, 'D', '2022-10-21', null, '2022-10-21 00:00:00', 'MRS BLOGS', null, null, null);
