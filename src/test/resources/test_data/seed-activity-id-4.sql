insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, minimum_incentive_nomis_code, minimum_incentive_level, created_time, created_by)
values (4, 'PVI', 1, 1, true, false, false, false, 'H', 'Maths', 'Maths Level 1', current_date, null, 'high', 'BAS', 'Basic', current_timestamp, 'SEED USER');

insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (1, 4, 'BAS', 'Basic', 1, 125, 125, 1),
       (2, 4, 'STD', 'Standard', 2, 150, 150, 1),
       (3, 4, 'ENH', 'Enhanced', 3, 175, 175, 1),
       (4, 4, 'BAS', 'Basic', 4, 175, 175, 1);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (1, 4, 'Maths AM', 1, 'L1', 'Location 1', 10, current_date-100);

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag)
values (1, 1, '10:00:00', '11:00:00', true);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (2, 4, 'Maths PM', 2, 'L2', 'Location 2', 10, current_date);

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag)
values (2, 2, '14:00:00', '15:00:00', true);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (1, 1, 'A11111A', 10001, 1, '2022-10-10', null, '2022-10-10 09:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (2, 1, 'A22222A', 10002, 2, '2022-10-10', null, '2022-10-10 09:00:00', 'MRS BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (3, 2, 'A33333A', 10003, 3, '2022-10-10', null, '2022-10-10 10:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (4, 2, 'A44444A', 10004, 4, '2022-10-10', null, '2022-10-10 10:00:00', 'MRS BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into scheduled_instance(activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment)
values (1, current_date, '10:00:00', '11:00:00', false, null, null, null, null);

insert into scheduled_instance(activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment)
values (2, current_date, '14:00:00', '15:00:00', false, null, null, null, null);
