insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, summary, description, start_date, end_date, active, created_time, created_by)
values (2, 'PVI', 2, 2, 'English', 'English Level 2', '2022-10-21', null, true, '2022-9-21 00:00:00', 'SEED USER');

insert into activity_pay(activity_pay_id, activity_id, iep_basic_rate, iep_standard_rate, iep_enhanced_rate, piece_rate, piece_rate_items)
values (2, 2, 50, 75, 100, 0, 0);

insert into activity_schedule(activity_schedule_id, activity_id, description, suspend_until, start_time, end_time, internal_location_id, internal_location_code, internal_location_description, capacity, days_of_week)
values (3, 2, 'English AM', null, '2022-10-21 10:00:00', '2022-10-21 11:00:00', 1, 'L1', 'Location 1', 10, '1000000');

insert into activity_schedule(activity_schedule_id, activity_id, description, suspend_until, start_time, end_time, internal_location_id, internal_location_code, internal_location_description, capacity, days_of_week)
values (4, 2, 'English PM', null, '2022-10-21 14:00:00', '2022-10-21 15:00:00', 1, 'L1', 'Location 1', 10, '1000000');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, iep_level, pay_band, start_date, end_date, active, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (5, 3, 'B11111B', 'ENH', 'A', '2022-10-21', null, true, '2022-10-21 00:00:00', 'MR BLOGS', null, null, null);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, iep_level, pay_band, start_date, end_date, active, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (6, 3, 'B22222B', 'BAS', 'B', '2022-10-21', null, true, '2022-10-21 00:00:00', 'MRS BLOGS', null, null, null);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, iep_level, pay_band, start_date, end_date, active, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (7, 4, 'B11111B', 'STD', 'C', '2022-10-21', null, true, '2022-10-21 00:00:00', 'MR BLOGS', null, null, null);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, iep_level, pay_band, start_date, end_date, active, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (8, 4, 'B22222B', 'STD', 'D', '2022-10-21', null, true, '2022-10-21 00:00:00', 'MRS BLOGS', null, null, null);
