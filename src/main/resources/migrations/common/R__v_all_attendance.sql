CREATE OR REPLACE VIEW v_all_attendance
AS
SELECT att.attendance_id,
       si.session_date,
       si.time_slot,
       att.status,
       ar.code AS attendance_reason_code,
       att.issue_payment,
       act.prison_code,
       si.scheduled_instance_id,
       act.activity_id,
       act.summary,
       ac.name AS category_name,
       att.prisoner_number,
       att.recorded_time,
       act.attendance_required,
       et.code AS event_tier,
       si.start_time,
       si.end_time,
       att.incentive_level_warning_issued,
       ar.description AS attendance_reason_description
FROM scheduled_instance si
         JOIN activity_schedule asch ON si.activity_schedule_id = asch.activity_schedule_id
         JOIN activity act ON asch.activity_id = act.activity_id
         JOIN activity_category ac ON act.activity_category_id = ac.activity_category_id
         JOIN attendance att ON si.scheduled_instance_id = att.scheduled_instance_id
         LEFT JOIN attendance_reason ar ON att.attendance_reason_id = ar.attendance_reason_id
         LEFT JOIN event_tier et ON et.event_tier_id = act.activity_tier_id;