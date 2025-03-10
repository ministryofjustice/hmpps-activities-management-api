insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (1, 'PVI', 8, 1, true, false, false, false, 'H', 'Retirement', 'Basic retirement', '2022-10-10', null, 'high', '2022-9-21 00:00:00', 'SEED USER', true);

insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (1, 1, 'BAS', 'Basic', 1, 125, 150, 1);

insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (2, 1, 'BAS', 'Basic', 2, 225, 250, 1);

insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (3, 1, 'BAS', 'Basic', 3, 325, 350, 1);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (1, 1, 'Retirement AM', 1, 'L1', 'Location 1', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, time_slot)
values (1, 1, '10:00:00', '11:00:00', true, 'AM');

--current active
insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (1, 1, 'A11111A', 10001, 1, now()::date - 1, null, now() - INTERVAL '1' DAY, 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

--historic ended
insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (2, 1, 'A11111A', 10003, 2, now()::date - 20, now()::date - 2, now() - INTERVAL '30' DAY, 'MRS BLOGS', now() - INTERVAL '3' DAY, 'SYSTEM', 'ENDED', null, null, null, 'ENDED');

insert into planned_deallocation(planned_deallocation_id, planned_date, planned_by, planned_reason, planned_at, allocation_id)
values(1, now()::date - 2, 'MR BLOGS', 'PLANNED', now()::date - 3, 2);

update allocation set planned_deallocation_id = 1 where allocation_id = 2;

--future planned different prisoner
insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (3, 1, 'A22222A', 10001, 1, now()::date - 1, null, now() - INTERVAL '1' DAY, 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into planned_deallocation(planned_deallocation_id, planned_date, planned_by, planned_reason, planned_at, allocation_id)
values(2, now()::date + 2, 'MR BLOGS', 'PLANNED', now()::date - 3, 3);

update allocation set planned_deallocation_id = 2 where allocation_id = 3;

insert into scheduled_instance(activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment, time_slot)
values (1, current_date, '08:30:00', '11:30:00', false, null, null, null, null, 'AM');
