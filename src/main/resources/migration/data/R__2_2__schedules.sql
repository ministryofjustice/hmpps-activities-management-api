
insert into activity_schedule(activity_schedule_id, activity_id, description, start_time, end_time, internal_location_id, internal_location_code, internal_location_description, capacity, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag, saturday_flag, sunday_flag)
values (1, 1, 'Entry level Maths 1', '10:00:00', '11:00:00', 1, 'EDU-ROOM-1', 'Education - R1', 10, true, false, false, false, false, false, false),
       (2, 1, 'Entry level Maths 2', '14:00:00', '15:00:00', 2, 'EDU-ROOM-2', 'Education - R2', 10, true, false, false, false, false, false, false),
       (3, 1, 'Entry level Maths 3', '10:00:00', '11:00:00', 1, 'EDU-ROOM-1', 'Education - R1', 10, false, false, true, false, false, false, false),
       (4, 1, 'Entry level Maths 4', '14:00:00', '15:00:00', 2, 'EDU-ROOM-2', 'Education - R2', 10, false, false, true, false, false, false, false),
       (5, 2, 'Entry level English 1', '10:00:00', '11:00:00', 1, 'EDU-ROOM-1', 'Education - R1', 10, false, true, false, false, false, false, false),
       (6, 2, 'Entry level English 2', '14:00:00', '15:00:00', 2, 'EDU-ROOM-2', 'Education - R2', 10, false, true, false, false, false, false, false),
       (7, 2, 'Entry level English 3', '10:00:00', '11:00:00', 1, 'EDU-ROOM-1', 'Education - R1', 10, false, false, false, true, false, false, false),
       (8, 2, 'Entry level English 4', '14:00:00', '15:00:00', 2, 'EDU-ROOM-2', 'Education - R2', 10, false, false, false, true, false, false, false),
       (9, 3, 'Wing cleaning 1', '09:00:00', '11:00:00', 3, 'W1', 'Wing 1', 10, false, false, false, false, false, true, false),
       (10, 3, 'Wing cleaning 2', '15:00:00', '17:00:00', 3, 'W1', 'Wing 1', 10, false, false, false, false, false, true, false),
       (11, 4, 'Gym session 1', '09:00:00', '11:00:00', 4, 'GYM-1', 'Gym 1', 10, false, false, false, false, false, false, true),
       (12, 4, 'Gym session 2', '15:00:00', '17:00:00', 4, 'GYM-1', 'Gym 1', 10, false, false, false, false, false, false, true);

alter sequence activity_schedule_activity_schedule_id_seq restart with 13;
