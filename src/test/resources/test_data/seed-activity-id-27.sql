-- Activity 1
insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, minimum_incentive_nomis_code, minimum_incentive_level, created_time, created_by, paid)
values (1, 'PVI', 1, 1, true, false, false, false, 'H', 'Maths', 'Maths Level 1', '2022-10-10', null, 'high', 'BAS', 'Basic', '2022-9-21 00:00:00', 'SEED USER', true);

insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (1, 1, 'BAS', 'Basic', 1, 125, 150, 1);

insert into activity_minimum_education_level(activity_minimum_education_level_id, activity_id, education_level_code, education_level_description, study_area_code, study_area_description)
values (1, 1, '1', 'Reading Measure 1.0', 'MATH', 'Maths');

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (1, 1, 'Maths AM', 1, 'L1', 'Location 1', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag)
values (1, 1, '10:00:00', '11:00:00', true);

-- Activity 2
insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, minimum_incentive_nomis_code, minimum_incentive_level, created_time, created_by, paid)
values (2, 'PVI', 1, 1, true, false, false, false, 'H', 'English', 'English Level 1', '2022-10-10', null, 'high', 'BAS', 'Basic', '2022-9-21 00:00:00', 'SEED USER', true);

insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (2, 2, 'BAS', 'Basic', 1, 125, 150, 1);

insert into activity_minimum_education_level(activity_minimum_education_level_id, activity_id, education_level_code, education_level_description, study_area_code, study_area_description)
values (2, 2, '1', 'Reading Measure 1.0', 'ENGLA', 'English Language');

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (2, 2, 'English PM', 2, 'L2', 'Location 3', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag)
values (2, 2, '14:00:00', '15:00:00', true);

-- schedules exist for the last 3 days on each activity (note we test for 1 day ago so only scheduled_instance_id 2 and 5 should be included)
insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment)
values (1, 1, current_date - 2, '10:00:00', '11:00:00', false, null, null, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment)
values (2, 1, current_date - 1, '10:00:00', '11:00:00', false, null, null, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment)
values (3, 1, current_date, '10:00:00', '11:00:00', false, null, null, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment)
values (4, 2, current_date - 2, '14:00:00', '15:00:00', false, null, null, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment)
values (5, 2, current_date - 1, '14:00:00', '15:00:00', false, null, null, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment)
values (6, 2, current_date, '14:00:00', '15:00:00', false, null, null, null, null);

-- allocation for prisoner 1 on activity 1 is active so will be included
insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (1, 1, 'A11111A', 10001, 1, '2022-10-10', null, '2022-10-10 09:00:00', 'any', null, null, null, null, null, null, 'ACTIVE');

-- allocation for prisoner 1 on activity 2 is active so will be included
insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (2, 2, 'A11111A', 10001, 1, '2022-10-10', null, '2022-10-10 09:00:00', 'MRS BLOGS', null, null, null, null, null, null, 'ACTIVE');

-- allocation for prisoner 2 on activity 1 is active so will be included
insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (3, 1, 'A22222A', 10002, 1, '2022-10-10', null, '2022-10-10 09:00:00', 'MRS BLOGS', null, null, null, null, null, null, 'ACTIVE');

-- allocation for prisoner 2 on activity 2 has ended so won't be included
insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (4, 2, 'A22222A', 10002, 1, '2022-10-10', current_date - 2, '2022-10-10 09:00:00', 'MRS BLOGS', null, null, null, null, null, null, 'END');

-- allocation for prisoner 3 on activity 1 is active so will be included
insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (5, 1, 'A33333A', 10003, 1, '2022-10-10', null, '2022-10-10 09:00:00', 'any', null, null, null, null, null, null, 'ACTIVE');

-- allocation for prisoner 3 on activity 2 has future end date so will be included
insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (6, 2, 'A33333A', 10003, 1, '2022-10-10', current_date + 1, '2022-10-10 09:00:00', 'any', null, null, null, null, null, null, 'ACTIVE');

-- allocation for prisoner 4 on activity 1 has future start date so won't be included
insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (7, 1, 'A44444A', 10004, 1, current_date + 1, null, '2022-10-10 09:00:00', 'any', null, null, null, null, null, null, 'PENDING');

-- attendance for prisoner 1 on activity 1 scheduled instance 2 days ago is not included (wrong date)
insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, issue_payment)
values (1, 1, 'A11111A', 1, 'any', now(), 'any', 'COMPLETED', 150, 50, null, true);

-- attendance for prisoner 1 on activity 1 scheduled instance 1 day ago is included
insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, issue_payment)
values (2, 2, 'A11111A', 1, 'any', now(), 'any', 'COMPLETED', 150, 50, null, true);

-- attendance for prisoner 1 on activity 1 scheduled instance today is not included (wrong date)
insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, issue_payment)
values (3, 3, 'A11111A', 1, 'any', now(), 'any', 'COMPLETED', 150, 50, null, true);

-- attendance for prisoner 1 on activity 2 scheduled instance 1 day ago is included
insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, issue_payment)
values (4, 5, 'A11111A', 1, 'any', now(), 'any', 'COMPLETED', 150, 50, null, true);

-- attendance for prisoner 2 on activity 1 scheduled instance 1 day ago is included
insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, issue_payment)
values (5, 2, 'A22222A', 1, 'any', now(), 'any', 'COMPLETED', 150, 50, null, true);

-- attendance for prisoner 2 on activity 2 scheduled instance 1 day ago is not included (prisoner allocation ended - attendance should never have existed)
insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, issue_payment)
values (6, 5, 'A22222A', 1, 'any', now(), 'any', 'COMPLETED', 150, 50, null, true);

-- attendance for prisoner 3 on activity 1 scheduled instance 1 day ago is not included (attendance not paid)
insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, issue_payment)
values (7, 2, 'A33333A', 1, 'any', now(), 'any', 'WAITING', 150, 50, null, false);

-- attendance for prisoner 3 on activity 2 scheduled instance 1 day ago is included
insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, issue_payment)
values (8, 5, 'A33333A', 1, 'any', now(), 'any', 'COMPLETED', 150, 50, null, true);

-- attendance for prisoner 4 on activity 1 scheduled instance 1 day ago is not included (allocation has future start date - attendance should never have existed)
insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, issue_payment)
values (9, 2, 'A44444A', 1, 'any', now(), 'any', 'WAITING', 150, 50, null, true);
