insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, minimum_incentive_nomis_code, minimum_incentive_level, created_time, created_by, paid)
values (1, 'MDI', 1, 1, true, false, false, false, 'H', 'Maths', 'Maths Level 1', '2022-10-10', null, 'high', 'BAS', 'Basic', '2022-9-21 00:00:00', 'SEED USER', true);

insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (1, 1, 'BAS', 'Basic', 1, 125, 150, 1);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (1, 1, 'Maths AM', 1, 'L1', 'Location 1', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag)
values (1, 1, '09:00:00', '11:00:00', true);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (2, 1, 'Maths PM', 2, 'L2', 'Location 2', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag)
values (2, 2, '13:00:00', '15:00:00', true);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (3, 1, 'Maths ED', 2, 'L2', 'Location 2', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag)
values (3, 3, '18:00:00', '20:00:00', true);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (1, 1, 'A11111A', 10001, 1, '2022-10-10', null, '2022-10-10 09:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (2, 1, 'A22222A', 10002, 2, '2022-10-10', null, '2022-10-10 09:00:00', 'MRS BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (3, 1, 'A33333A', 10003, 2, '2022-10-10', '2022-10-11', '2022-10-10 09:00:00', 'MRS BLOGS', '2022-10-11 09:00:00', 'SYSTEM', 'Allocation end data reached', null, null, null, 'ENDED');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (4, 2, 'A11111A', 10001, 3, '2022-10-10', null, '2022-10-10 10:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (5, 2, 'A22222A', 10002, 3, '2022-10-10', null, '2022-10-10 10:00:00', 'MRS BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (6, 3, 'A11111A', 10001, 3, '2022-10-10', null, '2022-10-10 10:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into scheduled_instance(activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment)
values (1, '2022-10-10', '09:00:00', '11:00:00', false, null, null, null, null);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, paid)
values (1, 1, 'A11111A', null, null, null, null, 'WAITING', null, null, null, true);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, paid)
values (2, 1, 'A22222A', null, null, null, null, 'WAITING', null, null, null, true);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, paid)
values (3, 1, 'A33333A', null, null, null, null, 'WAITING', null, null, null, true);

insert into scheduled_instance(activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment)
values (2, '2022-10-10', '13:00:00', '15:00:00', false, null, null, null, null);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, paid)
values (4, 2, 'A11111A', null, null, null, null, 'WAITING', null, null, null, true);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, paid)
values (5, 2, 'A22222A', null, null, null, null, 'WAITING', null, null, null, true);

insert into scheduled_instance(activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment)
values (3, '2022-10-10', '18:00:00', '20:00:00', false, null, null, null, null);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, paid)
values (6, 3, 'A11111A', null, null, null, null, 'WAITING', null, null, null, true);

insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, minimum_incentive_nomis_code, minimum_incentive_level, created_time, created_by, paid)
values (2, 'MDI', 4, 1, true, false, false, false, 'H', 'Gym', 'Gym', '2022-10-10', null, 'high', 'BAS', 'Basic', '2022-9-21 00:00:00', 'SEED USER', true);

insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (2, 2, 'BAS', 'Basic', 1, 125, 150, 1);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (4, 2, 'Gym AM', 1, 'L1', 'Location 1', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag)
values (4, 4, '09:00:00', '11:00:00', true);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (5, 2, 'Gym PM', 2, 'L2', 'Location 2', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag)
values (5, 5, '13:00:00', '15:00:00', true);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (7, 4, 'A11111A', 10001, 1, '2022-10-10', null, '2022-10-10 09:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (8, 4, 'A22222A', 10002, 2, '2022-10-10', null, '2022-10-10 09:00:00', 'MRS BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (9, 5, 'A11111A', 10001, 3, '2022-10-10', null, '2022-10-10 10:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into scheduled_instance(activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment)
values (4, '2022-10-10', '09:00:00', '11:00:00', false, null, null, null, null);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, paid)
values (7, 4, 'A11111A', null, null, null, null, 'WAITING', null, null, null, true);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, paid)
values (8, 4, 'A22222A', null, null, null, null, 'WAITING', null, null, null, true);

insert into scheduled_instance(activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment)
values (5, '2022-10-10', '13:00:00', '15:00:00', false, null, null, null, null);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, paid)
values (9, 5, 'A11111A', null, null, null, null, 'WAITING', null, null, null, true);