CREATE OR REPLACE VIEW v_all_attendance AS
SELECT a.attendance_id,
       si.session_date,
       ts.time_slot,
       a.status,
       ar.code as attendance_reason_code,
       a.issue_payment,
       ts.prison_code,
       ts.activity_id,
       ts.name as category_name,
       a.prisoner_number
FROM scheduled_instance si
         INNER JOIN attendance a ON si.scheduled_instance_id = a.scheduled_instance_id
         INNER JOIN v_activity_time_slot ts ON si.scheduled_instance_id = ts.scheduled_instance_id
         LEFT JOIN attendance_reason ar on a.attendance_reason_id = ar.attendance_reason_id;
