insert into activity(prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values ('PVI', 1, 1, true, false, false, false, 'H', 'Gym', 'Gym induction', current_date, null, 'high', current_timestamp, 'SEED USER', true);

insert into activity_schedule(activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (1, 'Gym induction AM', 1, 'L1', 'Location 1', 10, current_date);

insert into activity_schedule_slot(activity_schedule_id, start_time, end_time, monday_flag, time_slot)
values ( 1, '10:00:00', '11:00:00', true, 'AM');

insert into allocation(activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values ( 1, 'A11111A', 10001, 1, '2022-10-10', null, '2022-10-10 09:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into scheduled_instance(activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment, time_slot)
values (1, current_date, '10:00:00', '11:00:00', false, null, null, null, null, 'AM');

insert into advance_attendance(scheduled_instance_id, prisoner_number, issue_payment, recorded_time, recorded_by)
values (1, 'A11111A', true, '2022-10-15 10:00:00', 'FRED SMITH');

insert into advance_attendance_history(advance_attendance_id, recorded_time, recorded_by, issue_payment)
values (1, '2022-10-14 11:00:00', 'ALICE JONES', false),
       (1, '2022-10-13 11:00:00', 'SARAH WILLIAMS', true);
