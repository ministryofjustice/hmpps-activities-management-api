-- Tuesday morning english instances
insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values
  (45, 5, '2022-10-11', '10:00:00', '11:00:00', false, null , null),
  (46, 5, '2022-10-18', '10:00:00', '11:00:00', false, null , null),
  (47, 5, '2022-10-25', '10:00:00', '11:00:00', false, null , null),
  (48, 5, '2022-11-01', '10:00:00', '11:00:00', false, null , null),
  (49, 5, '2022-11-08', '10:00:00', '11:00:00', false, null , null),
  (50, 5, '2022-11-15', '10:00:00', '11:00:00', false, null , null),
  (51, 5, '2022-11-22', '10:00:00', '11:00:00', false, null , null),
  (52, 5, '2022-11-29', '10:00:00', '11:00:00', false, null , null),
  (53, 5, '2022-12-06', '10:00:00', '11:00:00', false, null , null),
  (54, 5, '2022-12-13', '10:00:00', '11:00:00', false, null , null),
  (55, 5, '2022-12-20', '10:00:00', '11:00:00', false, null , null);

-- Tuesday afternoon instances
insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values
  (56, 6, '2022-10-11', '14:00:00', '15:00:00', false, null , null),
  (57, 6, '2022-10-18', '14:00:00', '15:00:00', false, null , null),
  (58, 6, '2022-10-25', '14:00:00', '15:00:00', false, null , null),
  (59, 6, '2022-11-01', '14:00:00', '15:00:00', false, null , null),
  (60, 6, '2022-11-08', '14:00:00', '15:00:00', false, null , null),
  (61, 6, '2022-11-15', '14:00:00', '15:00:00', false, null , null),
  (62, 6, '2022-11-22', '14:00:00', '15:00:00', false, null , null),
  (63, 6, '2022-11-29', '14:00:00', '15:00:00', false, null , null),
  (64, 6, '2022-12-06', '14:00:00', '15:00:00', false, null , null),
  (65, 6, '2022-12-13', '14:00:00', '15:00:00', false, null , null),
  (66, 6, '2022-12-20', '14:00:00', '15:00:00', false, null , null);

-- Thursday morning english instances
insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values
  (67, 7, '2022-10-13', '10:00:00', '11:00:00', false, null , null),
  (68, 7, '2022-10-20', '10:00:00', '11:00:00', false, null , null),
  (69, 7, '2022-10-27', '10:00:00', '11:00:00', false, null , null),
  (70, 7, '2022-11-03', '10:00:00', '11:00:00', false, null , null),
  (71, 7, '2022-11-10', '10:00:00', '11:00:00', false, null , null),
  (72, 7, '2022-11-17', '10:00:00', '11:00:00', false, null , null),
  (73, 7, '2022-11-24', '10:00:00', '11:00:00', false, null , null),
  (74, 7, '2022-12-01', '10:00:00', '11:00:00', false, null , null),
  (75, 7, '2022-12-08', '10:00:00', '11:00:00', false, null , null),
  (76, 7, '2022-12-15', '10:00:00', '11:00:00', false, null , null),
  (77, 7, '2022-12-22', '10:00:00', '11:00:00', false, null , null);

-- Thursday afternoon english instances
insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values
  (78, 8, '2022-10-13', '14:00:00', '15:00:00', false, null , null),
  (79, 8, '2022-10-20', '14:00:00', '15:00:00', false, null , null),
  (80, 8, '2022-10-27', '14:00:00', '15:00:00', false, null , null),
  (81, 8, '2022-11-03', '14:00:00', '15:00:00', false, null , null),
  (82, 8, '2022-11-10', '14:00:00', '15:00:00', false, null , null),
  (83, 8, '2022-11-17', '14:00:00', '15:00:00', false, null , null),
  (84, 8, '2022-11-24', '14:00:00', '15:00:00', false, null , null),
  (85, 8, '2022-12-01', '14:00:00', '15:00:00', false, null , null),
  (86, 8, '2022-12-08', '14:00:00', '15:00:00', false, null , null),
  (87, 8, '2022-12-15', '14:00:00', '15:00:00', false, null , null),
  (88, 8, '2022-12-22', '14:00:00', '15:00:00', false, null , null);

alter sequence scheduled_instance_scheduled_instance_id_seq restart with 89;