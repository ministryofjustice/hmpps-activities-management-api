-- Monday AM
insert into activity_schedule(activity_schedule_id, activity_id, description, start_time, end_time, internal_location_id, internal_location_code, internal_location_description, capacity, monday_flag)
values (1, 1, 'Monday AM Houseblock 3', '10:00:00', '11:00:00', 1, 'EDU-ROOM-1', 'Education - R1', 10, true);

-- Monday PM
insert into activity_schedule(activity_schedule_id, activity_id, description, start_time, end_time, internal_location_id, internal_location_code, internal_location_description, capacity, monday_flag)
values (2, 1, 'Monday PM Houseblock 3', '14:00:00', '15:00:00', 2, 'EDU-ROOM-2', 'Education - R2', 10, true);

-- Wednesday AM
insert into activity_schedule(activity_schedule_id, activity_id, description, start_time, end_time, internal_location_id, internal_location_code, internal_location_description, capacity, wednesday_flag)
values (3, 1, 'Wednesday AM Houseblock 3', '10:00:00', '11:00:00', 1, 'EDU-ROOM-1', 'Education - R1', 10, true);

-- Wednesday PM
insert into activity_schedule(activity_schedule_id, activity_id, description, start_time, end_time, internal_location_id, internal_location_code, internal_location_description, capacity, wednesday_flag)
values (4, 1, 'Wednesday PM Houseblock 3', '14:00:00', '15:00:00', 2, 'EDU-ROOM-2', 'Education - R2', 10, true);

alter sequence activity_schedule_activity_schedule_id_seq restart with 5;
