CREATE OR REPLACE VIEW v_scheduled_instance_time_slot AS
    SELECT  scheduled_instance.scheduled_instance_id, 'AM' as time_slot
    FROM    activity, activity_schedule, prison_regime, scheduled_instance
    WHERE   activity.activity_id = activity_schedule.activity_id
      AND   activity.prison_code = prison_regime.prison_code
      AND   activity_schedule.activity_schedule_id = scheduled_instance.activity_schedule_id
      AND   scheduled_instance.start_time = prison_regime.am_start
    UNION
    SELECT  scheduled_instance.scheduled_instance_id, 'PM'
    FROM    activity, activity_schedule, prison_regime, scheduled_instance
    WHERE   activity.activity_id = activity_schedule.activity_id
      AND   activity.prison_code = prison_regime.prison_code
      AND   activity_schedule.activity_schedule_id = scheduled_instance.activity_schedule_id
      AND   scheduled_instance.start_time = prison_regime.pm_start
    UNION
    SELECT  scheduled_instance.scheduled_instance_id, 'ED'
    FROM    activity, activity_schedule, prison_regime, scheduled_instance
    WHERE   activity.activity_id = activity_schedule.activity_id
      AND   activity.prison_code = prison_regime.prison_code
      AND   activity_schedule.activity_schedule_id = scheduled_instance.activity_schedule_id
      AND   scheduled_instance.start_time = prison_regime.ed_start;

CREATE OR REPLACE VIEW v_all_attendance AS
    SELECT a.attendance_id,
           si.session_date,
           ts.time_slot,
           a.status,
           ar.code as attendance_reason_code,
           a.issue_payment
    FROM scheduled_instance si
    INNER JOIN attendance a ON si.scheduled_instance_id = a.scheduled_instance_id
    INNER JOIN v_scheduled_instance_time_slot ts ON si.scheduled_instance_id = ts.scheduled_instance_id
    LEFT JOIN attendance_reason ar on a.attendance_reason_id = ar.attendance_reason_id;

CREATE OR REPLACE VIEW v_all_attendance_summary AS
SELECT MIN(aa.attendance_id) as id,
       aa.session_date,
       aa.time_slot,
       aa.status,
       aa.attendance_reason_code,
       aa.issue_payment,
       COUNT(aa.attendance_id) as attendance_count
FROM v_all_attendance aa
GROUP BY aa.session_date,
         aa.time_slot,
         aa.status,
         aa.attendance_reason_code,
         aa.issue_payment;
