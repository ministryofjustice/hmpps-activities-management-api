
insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag, saturday_flag, sunday_flag, runs_on_bank_holiday)
values (1, 1, '10:00:00', '11:00:00', true, false, false, false, false, false, false, false),
       (2, 2, '14:00:00', '15:00:00', true, false, false, false, false, false, false, false),
       (3, 3, '10:00:00', '11:00:00', false, false, true, false, false, false, false, false),
       (4, 4, '14:00:00', '15:00:00', false, false, true, false, false, false, false, false),
       (5, 5, '10:00:00', '11:00:00', false, true, false, false, false, false, false, false),
       (6, 6, '14:00:00', '15:00:00', false, true, false, false, false, false, false, false),
       (7, 7, '10:00:00', '11:00:00', false, false, false, true, false, false, false, false),
       (8, 8, '14:00:00', '15:00:00', false, false, false, true, false, false, false, false),
       (9, 9, '09:00:00', '11:00:00', false, false, false, false, false, true, false, false),
       (10, 10, '15:00:00', '17:00:00', false, false, false, false, false, true, false, false),
       (11, 11, '09:00:00', '11:00:00', false, false, false, false, false, false, true, false),
       (12, 12, '15:00:00', '17:00:00', false, true, false, true, false, false, true, false),
       (13, 12, '19:00:00', '20:00:00', false, true, true, true, false, true, true, false);

alter sequence activity_schedule_slot_activity_schedule_slot_id_seq restart with 14;
