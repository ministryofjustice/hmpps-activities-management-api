insert into attendance_reason(attendance_reason_id, code, description) select 1, 'ABS', 'Absent' where not exists (select 1 from attendance_reason where attendance_reason_id = 1);
insert into attendance_reason(attendance_reason_id, code, description) select 2, 'ACCAB', 'Acceptable absence' where not exists (select 1 from attendance_reason where attendance_reason_id = 2);
insert into attendance_reason(attendance_reason_id, code, description) select 3, 'ATT', 'Attended' where not exists (select 1 from attendance_reason where attendance_reason_id = 3);
insert into attendance_reason(attendance_reason_id, code, description) select 4, 'CANC', 'Cancelled' where not exists (select 1 from attendance_reason where attendance_reason_id = 4);
insert into attendance_reason(attendance_reason_id, code, description) select 5, 'NREQ', 'Not required' where not exists (select 1 from attendance_reason where attendance_reason_id = 5);
insert into attendance_reason(attendance_reason_id, code, description) select 6, 'SUS', 'Suspend' where not exists (select 1 from attendance_reason where attendance_reason_id = 6);
insert into attendance_reason(attendance_reason_id, code, description) select 7, 'UNACAB', 'Unacceptable absence' where not exists (select 1 from attendance_reason where attendance_reason_id = 7);
insert into attendance_reason(attendance_reason_id, code, description) select 8, 'REST', 'Rest day (no pay)' where not exists (select 1 from attendance_reason where attendance_reason_id = 8);

alter sequence attendance_reason_attendance_reason_id_seq restart with 9;