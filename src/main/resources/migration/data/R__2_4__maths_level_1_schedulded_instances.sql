-- Monday morning instances
insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values
  (1, 1, '2022-10-10', '10:00:00', '11:00:00', false, null , null),
  (2, 1, '2022-10-17', '10:00:00', '11:00:00', false, null , null),
  (3, 1, '2022-10-24', '10:00:00', '11:00:00', false, null , null),
  (4, 1, '2022-10-31', '10:00:00', '11:00:00', false, null , null),
  (5, 1, '2022-11-07', '10:00:00', '11:00:00', false, null , null),
  (6, 1, '2022-11-14', '10:00:00', '11:00:00', false, null , null),
  (7, 1, '2022-11-21', '10:00:00', '11:00:00', false, null , null),
  (8, 1, '2022-11-28', '10:00:00', '11:00:00', false, null , null),
  (9, 1, '2022-12-05', '10:00:00', '11:00:00', false, null , null),
  (10, 1, '2022-12-12', '10:00:00', '11:00:00', false, null , null),
  (11, 1, '2022-12-19', '10:00:00', '11:00:00', false, null , null);

-- Monday afternoon instances
insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values
  (12, 2, '2022-10-10', '14:00:00', '15:00:00', false, null , null),
  (13, 2, '2022-10-17', '14:00:00', '15:00:00', false, null , null),
  (14, 2, '2022-10-24', '14:00:00', '15:00:00', false, null , null),
  (15, 2, '2022-10-31', '14:00:00', '15:00:00', false, null , null),
  (16, 2, '2022-11-07', '14:00:00', '15:00:00', false, null , null),
  (17, 2, '2022-11-14', '14:00:00', '15:00:00', false, null , null),
  (18, 2, '2022-11-21', '14:00:00', '15:00:00', false, null , null),
  (19, 2, '2022-11-28', '14:00:00', '15:00:00', false, null , null),
  (20, 2, '2022-12-05', '14:00:00', '15:00:00', false, null , null),
  (21, 2, '2022-12-12', '14:00:00', '15:00:00', false, null , null),
  (22, 2, '2022-12-19', '14:00:00', '15:00:00', false, null , null);

-- Wednesday morning instances
insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values
  (23, 3, '2022-10-12', '10:00:00', '11:00:00', false, null , null),
  (24, 3, '2022-10-19', '10:00:00', '11:00:00', false, null , null),
  (25, 3, '2022-10-26', '10:00:00', '11:00:00', false, null , null),
  (26, 3, '2022-11-02', '10:00:00', '11:00:00', false, null , null),
  (27, 3, '2022-11-09', '10:00:00', '11:00:00', false, null , null),
  (28, 3, '2022-11-16', '10:00:00', '11:00:00', false, null , null),
  (29, 3, '2022-11-23', '10:00:00', '11:00:00', false, null , null),
  (30, 3, '2022-11-30', '10:00:00', '11:00:00', false, null , null),
  (31, 3, '2022-12-07', '10:00:00', '11:00:00', false, null , null),
  (32, 3, '2022-12-14', '10:00:00', '11:00:00', false, null , null),
  (33, 3, '2022-12-21', '10:00:00', '11:00:00', false, null , null);

-- Wednesday afternoon instances
insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values
  (34, 4, '2022-10-12', '14:00:00', '15:00:00', false, null , null),
  (35, 4, '2022-10-19', '14:00:00', '15:00:00', false, null , null),
  (36, 4, '2022-10-26', '14:00:00', '15:00:00', false, null , null),
  (37, 4, '2022-11-02', '14:00:00', '15:00:00', false, null , null),
  (38, 4, '2022-11-09', '14:00:00', '15:00:00', false, null , null),
  (39, 4, '2022-11-16', '14:00:00', '15:00:00', false, null , null),
  (40, 4, '2022-11-23', '14:00:00', '15:00:00', false, null , null),
  (41, 4, '2022-11-30', '14:00:00', '15:00:00', false, null , null),
  (42, 4, '2022-12-07', '14:00:00', '15:00:00', false, null , null),
  (43, 4, '2022-12-14', '14:00:00', '15:00:00', false, null , null),
  (44, 4, '2022-12-21', '14:00:00', '15:00:00', false, null , null);

alter sequence scheduled_instance_scheduled_instance_id_seq restart with 45