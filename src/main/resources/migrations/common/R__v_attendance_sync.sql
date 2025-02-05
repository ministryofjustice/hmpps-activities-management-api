CREATE OR REPLACE VIEW v_attendance_sync AS
select a.attendance_id,
       a.scheduled_instance_id,
       si.activity_schedule_id,
       si.session_date,
       si.start_time as session_start_time,
       si.end_time   as session_end_time,
       a.prisoner_number,
       a2.booking_id,
       ar.code       as attendance_reason_code,
       a.comment,
       a.status,
       a.pay_amount,
       a.bonus_amount,
       a.issue_payment,
       ar.description as attendance_reason_description,
       a.incentive_level_warning_issued
from attendance a
         join scheduled_instance si on a.scheduled_instance_id = si.scheduled_instance_id
         join allocation a2 on si.activity_schedule_id = a2.activity_schedule_id and
                               a.prisoner_number = a2.prisoner_number and
                               si.session_date >= a2.start_date and
                               (a2.end_date is null or si.session_date <= a2.end_date)
         left join attendance_reason ar on a.attendance_reason_id = ar.attendance_reason_id;
