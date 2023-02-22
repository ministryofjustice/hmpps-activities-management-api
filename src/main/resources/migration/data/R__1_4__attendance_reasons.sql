insert into attendance_reason(attendance_reason_id, code, description)
values (1, 'ABS', 'Absent'),
       (2, 'ACCAB', 'Acceptable absence'),
       (3, 'ATT', 'Attended'),
       (4, 'CANC', 'Cancelled'),
       (5, 'NREQ', 'Not required'),
       (6, 'SUS', 'Suspend'),
       (7, 'UNACAB', 'Unacceptable absence'),
       (8, 'REST', 'Rest day (no pay)');

alter sequence attendance_reason_attendance_reason_id_seq restart with 9;