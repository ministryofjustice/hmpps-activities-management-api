delete from local_audit;

insert into local_audit (local_audit_id, username, audit_type, detail_type, recorded_time, prison_code, prisoner_number, activity_id, activity_schedule_id, message)
values (1, 'Bob', 'ACTIVITY','ACTIVITY_CREATED', '2020-01-01 00:00:00','PVI', 'A123456',1,1,'An activity has been created');

insert into local_audit (local_audit_id, username, audit_type, detail_type, recorded_time, prison_code, prisoner_number, activity_id, activity_schedule_id, message)
values (2, 'Bob', 'ACTIVITY','ACTIVITY_CREATED', '2020-01-01 00:00:00','MDI', 'A123456',1,1,'An activity has been created');

insert into local_audit (local_audit_id, username, audit_type, detail_type, recorded_time, prison_code, prisoner_number, activity_id, activity_schedule_id, message)
values (3, 'Terry', 'ACTIVITY','ACTIVITY_CREATED', '2020-01-01 00:00:00','PVI', 'A123456',1,1,'An activity has been created');

insert into local_audit (local_audit_id, username, audit_type, detail_type, recorded_time, prison_code, prisoner_number, activity_id, activity_schedule_id, message)
values (4, 'Bob', 'PRISONER','PRISONER_ALLOCATED', '2020-01-01 00:00:00','PVI', 'A123456',1,1,'An activity has been created');

insert into local_audit (local_audit_id, username, audit_type, detail_type, recorded_time, prison_code, prisoner_number, activity_id, activity_schedule_id, message)
values (5, 'Bob', 'ACTIVITY','ACTIVITY_UPDATED', '2020-01-01 00:00:00','PVI', 'A123456',1,1,'An activity has been created');

insert into local_audit (local_audit_id, username, audit_type, detail_type, recorded_time, prison_code, prisoner_number, activity_id, activity_schedule_id, message)
values (6, 'Bob', 'ACTIVITY','ACTIVITY_CREATED', '2022-02-03 01:02:00','PVI', 'A123456',1,1,'An activity has been created');

insert into local_audit (local_audit_id, username, audit_type, detail_type, recorded_time, prison_code, prisoner_number, activity_id, activity_schedule_id, message)
values (7, 'Bob', 'ACTIVITY','ACTIVITY_CREATED', '1996-01-01 01:02:00','PVI', 'A123456',1,1,'An activity has been created');

insert into local_audit (local_audit_id, username, audit_type, detail_type, recorded_time, prison_code, prisoner_number, activity_id, activity_schedule_id, message)
values (8, 'Bob', 'ACTIVITY','ACTIVITY_CREATED', '2020-01-01 00:00:00','PVI', 'B987654',1,1,'An activity has been created');

insert into local_audit (local_audit_id, username, audit_type, detail_type, recorded_time, prison_code, prisoner_number, activity_id, activity_schedule_id, message)
values (9, 'Bob', 'ACTIVITY','ACTIVITY_CREATED', '2020-01-01 00:00:00','PVI', 'A123456',42,1,'An activity has been created');

insert into local_audit (local_audit_id, username, audit_type, detail_type, recorded_time, prison_code, prisoner_number, activity_id, activity_schedule_id, message)
values (10, 'Bob', 'ACTIVITY','ACTIVITY_CREATED', '2020-01-01 00:00:00','PVI', 'A123456',1,99,'An activity has been created');