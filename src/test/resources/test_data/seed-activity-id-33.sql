insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (1, 'MDI', 1, 1, true, false, false, false, 'H', 'Maths', 'Maths Level 1', '2022-10-10', null, 'high', '2022-9-21 00:00:00', 'SEED USER', true);

insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (1, 1, 'BAS', 'Basic', 11, 125, 150, 1);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (1, 1, 'Maths AM', 1, 'L1', 'Location 1', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, time_slot)
values (1, 1, '09:00:00', '10:00:00', true, 'AM'),
       (2, 1, '12:00:00', '13:00:00', true, 'PM'),
       (3, 1, '17:00:00', '18:00:00', true, 'ED');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment, time_slot)
values (1, 1, current_timestamp, '09:00:00', '10:00:00', false, null, null, null, null, 'AM'),
       (2, 1, current_timestamp, '12:00:00', '13:00:00', false, null, null, null, null, 'PM'),
       (3, 1, current_timestamp, '17:00:00', '18:00:00', false, null, null, null, null, 'ED'),
       (4, 1, current_timestamp + interval '1 day', '12:00:00', '13:00:00', false, null, null, null, null, 'PM');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (1, 1, 'A11111A', 10001, 1, '2022-10-10', null, '2022-10-10 09:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE'),
       (2, 1, 'B22222B', 10002, 1, '2022-10-10', null, '2022-10-10 09:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into attendance(scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces)
values (1, 'A11111A', null, null, null, null, 'WAITING', null, null, null),
       (2, 'A11111A', null, null, null, null, 'WAITING', null, null, null),
       (3, 'A11111A', null, null, null, null, 'WAITING', null, null, null),
       (3, 'B22222B', null, null, null, null, 'WAITING', null, null, null),
       (4, 'A11111A', null, null, null, null, 'WAITING', null, null, null);

insert into waiting_list(waiting_list_id, prison_code, prisoner_number, booking_id, application_date, activity_id, activity_schedule_id, requested_by, status, creation_time, created_by)
values (1, 'MDI', 'G4793VF', 1134676, '2023-08-08', 1, 1, 'Prison staff', 'APPROVED', '2023-08-10', 'SEED USER')
