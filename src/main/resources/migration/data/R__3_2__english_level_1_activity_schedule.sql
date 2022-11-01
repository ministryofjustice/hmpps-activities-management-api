-- Tuesday AM
insert into activity_schedule(activity_schedule_id, activity_id, description, start_time, end_time, internal_location_id, internal_location_code, internal_location_description, capacity, tuesday_flag)
values (5, 2, 'Tuesday AM Houseblock 3', '10:00:00', '11:00:00', 1, 'EDU-ROOM-1', 'Education - R1', 10, true);

-- Tuesday PM
insert into activity_schedule(activity_schedule_id, activity_id, description, start_time, end_time, internal_location_id, internal_location_code, internal_location_description, capacity, tuesday_flag)
values (6, 2, 'Tuesday PM Houseblock 3', '14:00:00', '15:00:00', 2, 'EDU-ROOM-2', 'Education - R2', 10, true);

-- Thursday AM
insert into activity_schedule(activity_schedule_id, activity_id, description, start_time, end_time, internal_location_id, internal_location_code, internal_location_description, capacity, thursday_flag)
values (7, 2, 'Thursday AM Houseblock 3', '10:00:00', '11:00:00', 1, 'EDU-ROOM-1', 'Education - R1', 10, true);

-- Thursday PM
insert into activity_schedule(activity_schedule_id, activity_id, description, start_time, end_time, internal_location_id, internal_location_code, internal_location_description, capacity, thursday_flag)
values (8, 2, 'Thursday PM Houseblock 3', '14:00:00', '15:00:00', 2, 'EDU-ROOM-2', 'Education - R2', 10, true);

alter sequence activity_schedule_activity_schedule_id_seq restart with 9;
