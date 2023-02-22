
insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (1, 1, 'Entry level Maths 1', 27006, 'TLR1', 'MDI-WORK_IND-WS2-TLR1', 10, '2022-10-10'),
       (2, 1, 'Entry level Maths 2', 27007, 'TLR2', 'MDI-WORK_IND-WS2-TLR2', 10, '2022-10-10'),
       (3, 1, 'Entry level Maths 3', 27006, 'TLR1', 'MDI-WORK_IND-WS2-TLR1', 10, '2022-10-10'),
       (4, 1, 'Entry level Maths 4', 27007, 'TLR2', 'MDI-WORK_IND-WS2-TLR2', 10, '2022-10-10'),
       (5, 2, 'Entry level English 1', 27006, 'TLR1', 'MDI-WORK_IND-WS2-TLR1', 10, '2022-10-10'),
       (6, 2, 'Entry level English 2', 27007, 'TLR2', 'MDI-WORK_IND-WS2-TLR2', 10, '2022-10-10'),
       (7, 2, 'Entry level English 3', 27006, 'TLR1', 'MDI-WORK_IND-WS2-TLR1', 10, '2022-10-10'),
       (8, 2, 'Entry level English 4', 27007, 'TLR2', 'MDI-WORK_IND-WS2-TLR2', 10, '2022-10-10'),
       (9, 3, 'Wing cleaning 1', 25538, '1', 'MDI-1', 10, '2022-10-10'),
       (10, 3, 'Wing cleaning 2', 25655, '2', 'MDI-2', 10, '2022-10-10'),
       (11, 4, 'Gym weights session 1', 27206, 'WEIGHTS', 'MDI-GYM-WEIGHTS', 10, '2022-10-10'),
       (12, 4, 'Gym weights session 2', 27206, 'WEIGHTS', 'MDI-GYM-WEIGHTS', 10, '2022-10-10');

alter sequence activity_schedule_activity_schedule_id_seq restart with 13;
