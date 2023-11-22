insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, minimum_incentive_nomis_code, minimum_incentive_level, created_time, created_by, paid)
values (1, 'MDI', 1, 1, true, false, false, false, 'H', 'Maths 1', 'Maths Level 1', '2022-10-10', null, 'high', 'BAS', 'Basic', '2022-9-21 00:00:00', 'SEED USER', true);

insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (1, 1, 'BAS', 'Basic', 11, 125, 150, 1);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (1, 1, 'Maths AM', 1, 'L1', 'Location 1', 10, '2022-10-10');

insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, minimum_incentive_nomis_code, minimum_incentive_level, created_time, created_by, paid)
values (2, 'MDI', 1, 1, true, false, false, false, 'H', 'Maths 2', 'Maths Level 2', '2022-10-10', null, 'high', 'BAS', 'Basic', '2022-9-21 00:00:00', 'SEED USER', true);

insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (2, 2, 'BAS', 'Basic', 11, 125, 150, 1);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (2, 2, 'Maths AM', 1, 'L2', 'Location 1', 10, '2022-10-10');

insert into waiting_list(waiting_list_id, prison_code, prisoner_number, booking_id, application_date, activity_id, activity_schedule_id, requested_by, status, creation_time, created_by, comments)
values (1, 'MDI', 'ABCD01', 1, '2023-01-01', 1, 1, 'Bob', 'PENDING', current_timestamp, 'Fred', 'Bob left some comments'),
(2, 'MDI', 'ABCD02', 1, '2023-02-01', 1, 1, 'Bob', 'APPROVED', current_timestamp, 'Fred', NULL),
(3, 'MDI', 'ABCD03', 1, '2023-02-28', 1, 1, 'Bob', 'PENDING', current_timestamp, 'Fred', NULL),
(4, 'MDI', 'ABCD04', 1, '2023-03-01', 2, 2, 'Bob', 'PENDING', current_timestamp, 'Fred', NULL),
(5, 'MDI', 'ABCD05', 1, '2023-05-01', 1, 1, 'Bob', 'PENDING', current_timestamp, 'Fred', NULL);
