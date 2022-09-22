insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, summary, description, start_date, end_date, active, created_time, created_by)
values (2, 'PVI', 2, 2, 'English', 'English Level 2', '2022-10-21', null, true, '2022-10-21 00:00:00', 'SEED USER');

insert into activity_session(activity_session_id, activity_id, description, suspend_until, start_time, end_time, internal_location_id, internal_location_code, internal_location_description, capacity, days_of_week)
values (3, 2, 'English AM', null, '2022-10-21 10:00:00', '2022-10-21 11:00:00', 1, 'L1', 'Location 1', 10, '1000000');

insert into activity_session(activity_session_id, activity_id, description, suspend_until, start_time, end_time, internal_location_id, internal_location_code, internal_location_description, capacity, days_of_week)
values (4, 2, 'English PM', null, '2022-10-21 14:00:00', '2022-10-21 15:00:00', 1, 'L1', 'Location 1', 10, '1000000');

insert into activity_prisoner(activity_prisoner_id, activity_session_id, prisoner_number, iep_level, pay_band, start_date, end_date, active, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (5, 3, 'B11111B', null, null, '2022-10-21', null, true, '2022-10-21 00:00:00', 'FRED BLOGS', null, null, null);

insert into activity_prisoner(activity_prisoner_id, activity_session_id, prisoner_number, iep_level, pay_band, start_date, end_date, active, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (6, 3, 'B22222B', null, null, '2022-10-21', null, true, '2022-10-21 00:00:00', 'FRED BLOGS', null, null, null);

insert into activity_prisoner(activity_prisoner_id, activity_session_id, prisoner_number, iep_level, pay_band, start_date, end_date, active, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (7, 4, 'B11111B', null, null, '2022-10-21', null, true, '2022-10-21 00:00:00', 'FRED BLOGS', null, null, null);

insert into activity_prisoner(activity_prisoner_id, activity_session_id, prisoner_number, iep_level, pay_band, start_date, end_date, active, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (8, 4, 'B22222B', null, null, '2022-10-21', null, true, '2022-10-21 00:00:00', 'FRED BLOGS', null, null, null);
