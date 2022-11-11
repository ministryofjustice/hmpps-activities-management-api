
insert into activity_schedule(activity_schedule_id, activity_id, description, start_time, end_time, internal_location_id, internal_location_code, internal_location_description, capacity, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag, saturday_flag, sunday_flag)
values (1, 1, 'Monday AM Houseblock 3', '10:00:00', '11:00:00', 1, 'EDU-ROOM-1', 'Education - R1', 10, true, false, false, false, false, false, false),
       (2, 1, 'Monday PM Houseblock 3', '14:00:00', '15:00:00', 2, 'EDU-ROOM-2', 'Education - R2', 10, true, false, false, false, false, false, false),
       (3, 1, 'Wednesday AM Houseblock 3', '10:00:00', '11:00:00', 1, 'EDU-ROOM-1', 'Education - R1', 10, false, false, true, false, false, false, false),
       (4, 1, 'Wednesday PM Houseblock 3', '14:00:00', '15:00:00', 2, 'EDU-ROOM-2', 'Education - R2', 10, false, false, true, false, false, false, false),
       (5, 2, 'Tuesday AM Houseblock 3', '10:00:00', '11:00:00', 1, 'EDU-ROOM-1', 'Education - R1', 10, false, true, false, false, false, false, false),
       (6, 2, 'Tuesday PM Houseblock 3', '14:00:00', '15:00:00', 2, 'EDU-ROOM-2', 'Education - R2', 10, false, true, false, false, false, false, false),
       (7, 2, 'Thursday AM Houseblock 3', '10:00:00', '11:00:00', 1, 'EDU-ROOM-1', 'Education - R1', 10, false, false, false, true, false, false, false),
       (8, 2, 'Thursday PM Houseblock 3', '14:00:00', '15:00:00', 2, 'EDU-ROOM-2', 'Education - R2', 10, false, false, false, true, false, false, false),
       (9, 3, 'Saturday AM Houseblock 1', '09:00:00', '11:00:00', 3, 'W1', 'Wing 1', 10, false, false, false, false, false, true, false),
       (10, 3, 'Saturday PM Houseblock 1', '15:00:00', '17:00:00', 3, 'W1', 'Wing 1', 10, false, false, false, false, false, true, false),
       (11, 4, 'Sunday AM Houseblock 1', '09:00:00', '11:00:00', 4, 'GYM-1', 'Gym 1', 10, false, false, false, false, false, false, true),
       (12, 4, 'Sunday PM Houseblock 1', '15:00:00', '17:00:00', 4, 'GYM-1', 'Gym 1', 10, false, false, false, false, false, false, true);

alter sequence activity_schedule_activity_schedule_id_seq restart with 13;
