--
-- The time slots for the prison have been set at two extremes very early or very late to prevent test fragility with timings
--
INSERT INTO prison_regime (prison_regime_id, prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
VALUES(3, 'PVI', '00:01:00', '00:02:00', '13:00:00', '16:30:00', '23:58:00', '23:59:00');

insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (1, 'PVI', 1, 1, true, false, false, false, 'H', 'Maths', 'Maths Level 1', '2022-10-10', null, 'high', '2022-9-21 00:00:00', 'SEED USER', true);

insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (2, 'PVI', 1, 1, true, false, false, false, 'H', 'English', 'English Level 1', '2022-10-10', null, 'high', '2022-9-21 00:00:00', 'SEED USER', true);

insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (1, 1, 'BAS', 'Basic', 1, 125, 150, 1);

insert into activity_minimum_education_level(activity_minimum_education_level_id, activity_id, education_level_code, education_level_description, study_area_code, study_area_description)
values (1, 1, '1', 'Reading Measure 1.0', 'ENGLA', 'English Language');

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (1, 1, 'Maths AM', 1, 'L1', 'Location 1', 10, '2022-10-10');

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (2, 2, 'English PM', 1, 'L1', 'Location 1', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag, saturday_flag, sunday_flag)
values (1, 1, '00:01:00', '00:02:00', true, true, true, true, true, true, true);

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag, saturday_flag, sunday_flag)
values (2, 2, '12:01:00', '12:02:00', true, true, true, true, true, true, true);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (1, 1, 'A11111A', 111111, 1, '2022-10-10', '2022-10-10', '2022-10-10 09:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ENDED');

insert into scheduled_instance(activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment)
values (1, current_date, '00:01:00', '00:02:00', false, null, null, null, null);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces)
values (1, 1, 'A11111A', null, null, null, null, 'WAITING', null, null, null);

insert into waiting_list(waiting_list_id, prison_code, prisoner_number, booking_id, application_date, activity_id, activity_schedule_id, requested_by, status, creation_time, created_by)
values (1, 'PVI', 'A11111A', 111111, current_date, 1, 1, 'Prison staff', 'APPROVED', '2023-08-10', 'SEED USER');

insert into local_audit (local_audit_id, username, audit_type, detail_type, recorded_time, prison_code, prisoner_number, activity_id, activity_schedule_id, message)
values (99, 'Bob', 'PRISONER','PRISONER_ALLOCATED', '2020-01-01 00:00:00','PVI', 'A11111A', 1, 1,'Prisoner has been allocated.');

insert into event_review (event_review_id, service_identifier, event_type, event_time, prison_code, prisoner_number, booking_id, event_data)
values (99, 'myservice', 'event-name', '2023-5-10 10:20:00', 'MDI', 'A11111A', 111111, 'aaaaaaa');

insert into appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
values (1, 'INDIVIDUAL', 'PVI', 'AC1', 1, 123, false, now()::date + 1, '08:30', '10:00', now()::timestamp, 'TEST.USER');

insert into appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
values (2, 1, 1, 'PVI', 'AC1', 1, 123, false, now()::date + 1, '08:30', '10:00', now()::timestamp, 'TEST.USER');

insert into appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
values (3, 2, 'A11111A', 111111);
