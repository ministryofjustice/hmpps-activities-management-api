CREATE OR REPLACE VIEW v_prisoner_scheduled_activities
AS
SELECT si.scheduled_instance_id,
       alloc.allocation_id,
       act.prison_code,
       si.session_date,
       si.start_time,
       si.end_time,
       alloc.prisoner_number,
       alloc.booking_id,
       schedule.internal_location_id,
       schedule.internal_location_code,
       schedule.internal_location_description,
       schedule.description AS schedule_description,
       act.activity_id,
       category.code        AS activity_category,
       act.summary          AS activity_summary,
       si.cancelled,
       CASE
           WHEN alloc.prisoner_status = 'SUSPENDED' OR alloc.prisoner_status = 'AUTO_SUSPENDED'
               THEN true
           ELSE false
           END              AS suspended,
       act.in_cell,
       act.on_wing,
       act.off_wing
FROM scheduled_instance si
         JOIN activity_schedule schedule
              ON schedule.activity_schedule_id = si.activity_schedule_id AND
                 si.session_date >= schedule.start_date AND
                 (schedule.end_date IS NULL OR schedule.end_date >= si.session_date)
         JOIN activity_schedule_slot slot
              ON schedule.activity_schedule_id = slot.activity_schedule_id AND
                 si.start_time = slot.start_time AND
                 slot.week_number = FLOOR(MOD(EXTRACT(day from (si.session_date - date_trunc('week', schedule.start_date))), schedule.schedule_weeks * 7) / 7) + 1
         JOIN allocation alloc ON alloc.activity_schedule_id = si.activity_schedule_id AND
                                  si.session_date >= alloc.start_date AND
                                  (alloc.end_date IS NULL OR alloc.end_date >= si.session_date)
         JOIN activity act ON act.activity_id = schedule.activity_id AND
                              (act.end_date IS NULL OR act.end_date >= si.session_date)
         JOIN activity_category category ON category.activity_category_id = act.activity_category_id
         LEFT JOIN exclusion ex ON alloc.allocation_id = ex.allocation_id and
                                   slot.activity_schedule_slot_id = ex.activity_schedule_slot_id
WHERE TO_CHAR(si.session_date, 'DY') = 'MON' AND ex.monday_flag IS NOT true
   OR TO_CHAR(si.session_date, 'DY') = 'TUE' AND ex.tuesday_flag IS NOT true
   OR TO_CHAR(si.session_date, 'DY') = 'WED' AND ex.wednesday_flag IS NOT true
   OR TO_CHAR(si.session_date, 'DY') = 'THU' AND ex.thursday_flag IS NOT true
   OR TO_CHAR(si.session_date, 'DY') = 'FRI' AND ex.friday_flag IS NOT true
   OR TO_CHAR(si.session_date, 'DY') = 'SAT' AND ex.saturday_flag IS NOT true
   OR TO_CHAR(si.session_date, 'DY') = 'SUN' AND ex.sunday_flag IS NOT true;

CREATE OR REPLACE VIEW v_scheduled_instance_attendance_summary AS
SELECT
    si.scheduled_instance_id,
    act.activity_id,
    acts.activity_schedule_id,
    act.prison_code,
    act.summary,
    act.activity_category_id,
    si.session_date,
    si.start_time,
    si.end_time,
    act.in_cell,
    act.on_wing,
    act.off_wing,
    acts.internal_location_id,
    acts.internal_location_code,
    acts.internal_location_description,
    si.cancelled,
    (SELECT COUNT(scheduled_instance_id) FROM v_prisoner_scheduled_activities psa where psa.scheduled_instance_id = si.scheduled_instance_id) AS allocations,
    att.attendees,
    att.not_recorded,
    att.attended,
    att.absences,
    att.paid
FROM scheduled_instance si
         JOIN activity_schedule acts ON acts.activity_schedule_id = si.activity_schedule_id
         JOIN activity act ON act.activity_id = acts.activity_id
         LEFT JOIN (
    SELECT
        att.scheduled_instance_id,
        COUNT(att.attendance_id) as attendees,
        SUM(CASE WHEN att.status = 'WAITING' THEN 1 ELSE 0 END) AS not_recorded,
        SUM(CASE WHEN ar.code = 'ATTENDED' THEN 1 ELSE 0 END) AS attended,
        SUM(CASE WHEN att.status = 'COMPLETED' AND ar.code != 'ATTENDED' THEN 1 ELSE 0 END) AS absences,
        SUM(CASE WHEN att.issue_payment THEN 1 ELSE 0 END) AS paid
    FROM attendance att
             LEFT JOIN attendance_reason ar ON ar.attendance_reason_id = att.attendance_reason_id
    GROUP BY att.scheduled_instance_id
) AS att ON att.scheduled_instance_id = si.scheduled_instance_id
