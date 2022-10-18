-- Monday AM
insert into activity_schedule(activity_schedule_id, activity_id, description, suspend_until, start_time, end_time, internal_location_id, internal_location_code, internal_location_description, capacity, days_of_week)
values (1, 1, 'Monday AM Houseblock 3', null, '10:00:00', '11:00:00', 1, 'EDU-ROOM-1', 'Education - R1', 10, '1000000');

-- Monday PM
insert into activity_schedule(activity_schedule_id, activity_id, description, suspend_until, start_time, end_time, internal_location_id, internal_location_code, internal_location_description, capacity, days_of_week)
values (2, 1, 'Monday PM Houseblock 3', null, '14:00:00', '15:00:00', 2, 'EDU-ROOM-2', 'Education - R2', 10, '1000000');

-- Wednesday AM
insert into activity_schedule(activity_schedule_id, activity_id, description, suspend_until, start_time, end_time, internal_location_id, internal_location_code, internal_location_description, capacity, days_of_week)
values (3, 1, 'Wednesday AM Houseblock 3', null, '10:00:00', '11:00:00', 1, 'EDU-ROOM-1', 'Education - R1', 10, '0010000');

-- Wednesday PM
insert into activity_schedule(activity_schedule_id, activity_id, description, suspend_until, start_time, end_time, internal_location_id, internal_location_code, internal_location_description, capacity, days_of_week)
values (4, 1, 'Wednesday PM Houseblock 3', null, '14:00:00', '15:00:00', 2, 'EDU-ROOM-2', 'Education - R2', 10, '0010000');
