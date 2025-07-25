insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (1, 'MDI', 2, 2, true, false, false, false, 'H', 'Geography', 'Geography Level 1', '2022-10-01', null, 'high', '2022-9-21 00:00:00', 'SEED USER', true),
    (2, 'MDI', 2, 2, true, false, false, false, 'H', 'English', 'English Level 2', '2022-11-01', null, 'high', '2022-9-21 00:00:00', 'SEED USER', true),
    (3, 'PVI', 1, 1, true, false, false, false, 'H', 'Maths', 'Maths Level 1', '2022-10-10', null, 'high', '2022-9-21 00:00:00', 'SEED USER', true),
    (4, 'PVI', 2, 2, true, false, false, false, 'H', 'English', 'English Level 2', '2022-10-21', null, 'high', '2022-9-21 00:00:00', 'SEED USER', true);

insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (1, 1, 'BAS', 'Basic', 1, 101, 0, 0);

insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (2, 2, 'STD', 'Standard', 2, 102, 0, 0);

insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (3, 3, 'ENH', 'Enhanced', 3, 103, 0, 0);

insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (4, 4, 'GLD', 'Gold', 4, 104, 0, 0);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (1, 1, 'Geography AM', 1, 'L1', 'Location MDI 1', 10, '2022-10-01');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, time_slot)
values (1, 1, '09:00:00', '12:00:00', true, 'AM');

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (2, 1, 'Geography PM', 2, 'L2', 'Location MDI 2', 10, '2022-10-01');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, time_slot)
values (2, 2, '13:00:00', '16:30:00', true, 'PM');

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (3, 2, 'English AM', 2, 'L2', 'Location MDI 2', 10, '2022-11-01');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, time_slot)
values (3, 3, '09:00:00', '12:00:00', true, 'AM');

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (4, 2, 'English PM', 1, 'L1', 'Location MDI 1', 10, '2022-11-01');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, time_slot)
values (4, 4, '13:00:00', '16:30:00', true, 'PM');

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (5, 3, 'Maths AM', 3, 'L1', 'Location PVI 1', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, time_slot)
values (5, 5, '10:00:00', '11:00:00', true, 'AM');

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (6, 3, 'Maths PM', 4, 'L2', 'Location PVI 2', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, time_slot)
values (6, 6, '14:00:00', '15:00:00', true, 'PM');

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (7, 4, 'English AM', 4, 'L2', 'Location PVI 2', 10, '2022-10-21');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, time_slot)
values (7, 7, '10:00:00', '11:00:00', true, 'AM');

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (8, 4, 'English PM', 3, 'L1', 'Location PVI 1', 10, '2022-10-21');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, time_slot)
values (8, 8, '14:00:00', '15:00:00', true, 'PM');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (1, 1, 'A11111A', 10001, 1, '2022-10-01', null, '2022-10-01 10:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (2, 1, 'A22222A', 10002, 2, '2022-10-02', null, '2022-10-02 10:00:00', 'MRS BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (3, 2, 'A33333A', 10003, 3, '2022-10-01', null, '2022-10-10 10:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (4, 2, 'A44444A', 10004, 3, '2022-10-02', null, '2022-10-10 10:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (5, 3, 'A33333A', 10003, 3, '2022-11-01', null, '2022-10-10 10:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (6, 3, 'A44444A', 10004, 3, '2022-11-02', null, '2022-10-10 10:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (7, 4, 'A11111A', 10001, 3, '2022-11-01', null, '2022-10-10 10:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (8, 4, 'A22222A', 10002, 3, '2022-11-02', null, '2022-10-10 10:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (9, 5, 'B11111A', 20001, 1, '2022-10-10', null, '2022-10-01 10:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (10, 5, 'B22222A', 20002, 2, '2022-10-11', null, '2022-10-02 10:00:00', 'MRS BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (11, 6, 'B33333A', 20003, 3, '2022-10-10', null, '2022-10-10 10:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (12, 6, 'B44444A', 20004, 3, '2022-10-11', null, '2022-10-10 10:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (13, 7, 'B33333A', 20003, 3, '2022-10-21', null, '2022-10-10 10:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (14, 7, 'B44444A', 20004, 3, '2022-10-22', null, '2022-10-10 10:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (15, 8, 'B11111A', 20001, 3, '2022-11-21', null, '2022-10-10 10:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (16, 8, 'B22222A', 20002, 3, '2022-11-22', null, '2022-10-10 10:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (17, 1, 'C11111A', 30001, 1, '2022-10-01', null, '2022-10-01 10:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (1, 1, '2022-10-01', '09:00:00', '12:00:00', false, null, null, 'AM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (2, 1, '2022-10-02', '10:01:00', '12:00:00', false, null, null, 'AM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (3, 1, '2022-10-03', '09:00:00', '12:00:00', true, current_timestamp, 'ABC123', 'PM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (4, 1, '2022-10-04', '09:00:00', '12:00:00', false, null, null, 'AM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (5, 1, '2022-10-05', '09:00:00', '12:00:00', false, null, null, 'AM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (6, 2, '2022-10-01', '13:00:00', '16:30:00', false, null, null, 'PM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (7, 2, '2022-10-02', '13:00:00', '16:30:00', false, null, null, 'PM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (8, 2, '2022-10-03', '13:00:00', '16:30:00', false, null, null, 'PM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (9, 2, '2022-10-04', '13:00:00', '16:30:00', false, null, null, 'PM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (10, 2, '2022-10-05', '13:00:00', '16:30:00', false, null, null, 'PM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (11, 3, '2022-11-01', '09:00:00', '12:00:00', false, null, null, 'AM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (12, 3, '2022-11-02', '09:00:00', '12:00:00', false, null, null, 'AM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (13, 3, '2022-11-03', '09:00:00', '12:00:00', false, null, null, 'AM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (14, 3, '2022-11-04', '09:00:00', '12:00:00', false, null, null, 'AM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (15, 3, '2022-11-05', '09:00:00', '12:00:00', false, null, null, 'AM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (16, 4, '2022-11-01', '13:00:00', '16:30:00', false, null, null, 'PM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (17, 4, '2022-11-02', '13:00:00', '16:30:00', false, null, null, 'PM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (18, 4, '2022-11-03', '13:00:00', '16:30:00', false, null, null, 'PM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (19, 4, '2022-11-04', '13:00:00', '16:30:00', true, current_timestamp, 'ABC123', 'PM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (20, 4, '2022-11-05', '13:00:00', '16:30:00', true, current_timestamp, 'ABC123', 'PM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (21, 5, '2022-10-10', '10:00:00', '11:00:00', false, null, null, 'AM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (22, 5, '2022-10-11', '10:00:00', '11:00:00', false, null, null, 'AM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (23, 5, '2022-10-12', '10:00:00', '11:00:00', false, null, null, 'AM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (24, 5, '2022-10-13', '10:00:00', '11:00:00', false, null, null, 'AM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (25, 5, '2022-10-14', '10:00:00', '11:00:00', false, null, null, 'AM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (26, 6, '2022-10-10', '14:00:00', '15:00:00', false, null, null, 'PM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (27, 6, '2022-10-11', '14:00:00', '15:00:00', false, null, null, 'PM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (28, 6, '2022-10-12', '14:00:00', '15:00:00', false, null, null, 'PM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (29, 6, '2022-10-13', '14:00:00', '15:00:00', false, null, null, 'PM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (30, 6, '2022-10-14', '14:00:00', '15:00:00', false, null, null, 'PM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (31, 7, '2022-10-21', '10:00:00', '11:00:00', false, null, null, 'AM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (32, 7, '2022-10-22', '10:00:00', '11:00:00', false, null, null, 'AM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (33, 7, '2022-10-23', '10:00:00', '11:00:00', false, null, null, 'AM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (34, 7, '2022-10-24', '10:00:00', '11:00:00', false, null, null, 'AM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (35, 7, '2022-10-25', '10:00:00', '11:00:00', false, null, null, 'AM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (36, 8, '2022-10-21', '14:00:00', '15:00:00', false, null, null, 'PM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (37, 8, '2022-10-22', '14:00:00', '15:00:00', false, null, null, 'PM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (38, 8, '2022-10-23', '14:00:00', '15:00:00', false, null, null, 'PM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (39, 8, '2022-10-24', '14:00:00', '15:00:00', false, null, null, 'PM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (40, 8, '2022-11-25', '14:00:00', '15:00:00', false, null, null, 'PM');

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces)
values (1, 1, 'A11111A', 3, null, null, null, 'WAITING', null, null, null);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces)
values (2, 1, 'A22222A', 3, null, null, null, 'WAITING', null, null, null);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces)
values (3, 2, 'A11111A', 1, null, null, null, 'WAITING', null, null, null);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces)
values (4, 2, 'A22222A', 3, null, null, null, 'WAITING', null, null, null);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces)
values (5, 3, 'A11111A', 3, null, null, null, 'WAITING', null, null, null);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces)
values (6, 3, 'A22222A', 1, null, null, null, 'WAITING', null, null, null);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces)
values (7, 4, 'A11111A', 4, null, null, null, 'WAITING', null, null, null);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces)
values (8, 4, 'A22222A', 4, null, null, null, 'WAITING', null, null, null);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces)
values (9, 5, 'A11111A', 3, null, null, null, 'WAITING', null, null, null);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces)
values (10, 5, 'A22222A', 3, null, null, null, 'WAITING', null, null, null);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, issue_payment)
values (11, 1, 'C11111A', 9, null, null, null, 'COMPLETED', null, null, null, true);

insert into advance_attendance(advance_attendance_id, scheduled_instance_id, prisoner_number, issue_payment, recorded_time, recorded_by)
values (1, 1, 'A11111A', true, null, null);

insert into advance_attendance(advance_attendance_id, scheduled_instance_id, prisoner_number, issue_payment, recorded_time, recorded_by)
values (2, 1, 'A22222A', true, null, null);

insert into advance_attendance(advance_attendance_id, scheduled_instance_id, prisoner_number, issue_payment, recorded_time, recorded_by)
values (3, 2, 'A11111A', true, null, null);

insert into advance_attendance(advance_attendance_id, scheduled_instance_id, prisoner_number, issue_payment, recorded_time, recorded_by)
values (4, 2, 'A22222A', true, null, null);

insert into advance_attendance(advance_attendance_id, scheduled_instance_id, prisoner_number, issue_payment, recorded_time, recorded_by)
values (5, 6, 'A11111A', true, null, null);

insert into advance_attendance(advance_attendance_id, scheduled_instance_id, prisoner_number, issue_payment, recorded_time, recorded_by)
values (6, 6, 'A22222A', true, null, null);
