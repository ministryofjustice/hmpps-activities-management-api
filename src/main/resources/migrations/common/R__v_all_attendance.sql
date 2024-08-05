CREATE OR REPLACE VIEW v_all_attendance
AS
SELECT att.attendance_id,
       si.session_date,
       ts.time_slot,
       att.status,
       ar.code AS attendance_reason_code,
       att.issue_payment,
       ts.prison_code,
       si.scheduled_instance_id,
       ts.activity_id,
       ts.summary,
       ts.name AS category_name,
       att.prisoner_number,
       att.recorded_time,
       act.attendance_required,
       et.code AS event_tier
FROM scheduled_instance si
         JOIN activity_schedule asch ON si.activity_schedule_id = asch.activity_schedule_id
         JOIN activity act ON asch.activity_id = act.activity_id
         JOIN attendance att ON si.scheduled_instance_id = att.scheduled_instance_id
         JOIN v_activity_time_slot ts ON si.scheduled_instance_id = ts.scheduled_instance_id
         LEFT JOIN attendance_reason ar ON att.attendance_reason_id = ar.attendance_reason_id
         LEFT JOIN event_tier et ON et.event_tier_id = act.activity_tier_id;