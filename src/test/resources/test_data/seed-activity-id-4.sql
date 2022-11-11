insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, summary, description, start_date, end_date, created_time, created_by)
values (4, 'PVI', 1, 1, 'Maths', 'Maths Level 1', current_date, null, current_timestamp, 'SEED USER');

insert into activity_schedule(activity_schedule_id, activity_id, description, start_time, end_time, internal_location_id, internal_location_code, internal_location_description, capacity, monday_flag)
values (1, 4, 'Maths AM', '10:00:00', '11:00:00', 1, 'L1', 'Location 1', 10, true);

insert into activity_schedule(activity_schedule_id, activity_id, description, start_time, end_time, internal_location_id, internal_location_code, internal_location_description, capacity, monday_flag)
values (2, 4, 'Maths PM', '14:00:00', '15:00:00', 2, 'L2', 'Location 2', 10, true);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, incentive_level, pay_band, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (1, 1, 'A11111A', 'BAS', 'A', '2022-10-10', null, '2022-10-10 09:00:00', 'MR BLOGS', null, null, null);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, incentive_level, pay_band, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (2, 1, 'A22222A', 'STD', 'B', '2022-10-10', null, '2022-10-10 09:00:00', 'MRS BLOGS', null, null, null);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, incentive_level, pay_band, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (3, 2, 'A11111A', 'STD', 'C', '2022-10-10', null, '2022-10-10 10:00:00', 'MR BLOGS', null, null, null);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, incentive_level, pay_band, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (4, 2, 'A22222A', 'ENH', 'D', '2022-10-10', null, '2022-10-10 10:00:00', 'MRS BLOGS', null, null, null);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, incentive_level, pay_band, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (5, 2, 'A22223A', 'ENH', 'D', current_date + 1, null, current_timestamp, 'MRS BLOGS', null, null, null);

insert into scheduled_instance(activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (1, current_date, '10:00:00', '11:00:00', false, null, null);

insert into scheduled_instance(activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (2, current_date, '14:00:00', '15:00:00', false, null, null);
