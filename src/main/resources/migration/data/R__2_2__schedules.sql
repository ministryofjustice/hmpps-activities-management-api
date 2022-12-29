
insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity)
values (1, 1, 'Entry level Maths 1', 1, 'EDU-ROOM-1', 'Education - R1', 10),
       (2, 1, 'Entry level Maths 2', 2, 'EDU-ROOM-2', 'Education - R2', 10),
       (3, 1, 'Entry level Maths 3', 1, 'EDU-ROOM-1', 'Education - R1', 10),
       (4, 1, 'Entry level Maths 4', 2, 'EDU-ROOM-2', 'Education - R2', 10),
       (5, 2, 'Entry level English 1', 1, 'EDU-ROOM-1', 'Education - R1', 10),
       (6, 2, 'Entry level English 2', 2, 'EDU-ROOM-2', 'Education - R2', 10),
       (7, 2, 'Entry level English 3', 1, 'EDU-ROOM-1', 'Education - R1', 10),
       (8, 2, 'Entry level English 4', 2, 'EDU-ROOM-2', 'Education - R2', 10),
       (9, 3, 'Wing cleaning 1', 3, 'W1', 'Wing 1', 10),
       (10, 3, 'Wing cleaning 2', 3, 'W1', 'Wing 1', 10),
       (11, 4, 'Gym session 1', 4, 'GYM-1', 'Gym 1', 10),
       (12, 4, 'Gym session 2', 4, 'GYM-1', 'Gym 1', 10);

alter sequence activity_schedule_activity_schedule_id_seq restart with 13;
