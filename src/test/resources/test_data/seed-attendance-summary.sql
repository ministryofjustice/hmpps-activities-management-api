update scheduled_instance set session_date = '2026-01-01';

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, time_slot)
values (101, 1, '2026-01-09', '10:00:00', '11:00:00', false, 'AM'),
       (102, 1, '2026-01-10', '10:00:00', '11:00:00', false, 'AM'),
       (103, 1, '2026-01-15', '10:00:00', '11:00:00', false, 'AM'),
       (104, 1, '2026-01-20', '10:00:00', '11:00:00', false, 'AM'),
       (105, 1, '2026-01-21', '10:00:00', '11:00:00', false, 'AM'),
       (107, 1, '2026-01-15', '11:00:00', '12:00:00', false, 'AM'),
       (108, 1, '2026-01-15', '12:00:00', '13:00:00', false, 'PM'),
       (109, 1, '2026-01-15', '13:00:00', '14:00:00', false, 'PM'),
       (110, 1, '2026-01-15', '14:00:00', '15:00:00', false, 'PM');

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, status)
select fixture.id, fixture.instance_id, fixture.prisoner_number, reason.attendance_reason_id, fixture.status
from (values
    (101, 101, 'A11111A', 'REFUSED', 'COMPLETED'),
    (102, 102, 'A11111A', 'REFUSED', 'COMPLETED'),
    (103, 103, 'A11111A', 'ATTENDED', 'COMPLETED'),
    (104, 104, 'A11111A', 'SICK', 'COMPLETED'),
    (105, 105, 'A11111A', 'REFUSED', 'COMPLETED'),
    (106, 103, 'A22222A', 'REFUSED', 'COMPLETED'),
    (107, 107, 'A11111A', 'REFUSED', 'WAITING'),
    (108, 108, 'A11111A', null, 'COMPLETED'),
    (109, 109, 'A11111A', null, 'WAITING'),
    (110, 110, 'A11111A', 'CANCELLED', 'COMPLETED')
) as fixture(id, instance_id, prisoner_number, reason_code, status)
left join attendance_reason reason on reason.code = fixture.reason_code;
