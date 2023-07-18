DROP VIEW v_all_attendance_summary;
DROP VIEW v_all_attendance;
DROP VIEW v_activity_time_slot;

CREATE OR REPLACE VIEW v_activity_time_slot
AS
SELECT scheduled_instance.scheduled_instance_id,
       'AM' AS time_slot,
       activity.prison_code,
       activity.activity_id,
       activity.summary,
       activity_category.name
FROM activity,
     activity_schedule,
     prison_regime,
     scheduled_instance,
     activity_category
WHERE activity.activity_id = activity_schedule.activity_id AND activity.prison_code = prison_regime.prison_code AND activity_schedule.activity_schedule_id = scheduled_instance.activity_schedule_id AND scheduled_instance.start_time = prison_regime.am_start AND activity.activity_category_id = activity_category.activity_category_id
UNION
SELECT scheduled_instance.scheduled_instance_id,
       'PM' AS time_slot,
       activity.prison_code,
       activity.activity_id,
       activity.summary,
       activity_category.name
FROM activity,
     activity_schedule,
     prison_regime,
     scheduled_instance,
     activity_category
WHERE activity.activity_id = activity_schedule.activity_id AND activity.prison_code = prison_regime.prison_code AND activity_schedule.activity_schedule_id = scheduled_instance.activity_schedule_id AND scheduled_instance.start_time = prison_regime.pm_start AND activity.activity_category_id = activity_category.activity_category_id
UNION
SELECT scheduled_instance.scheduled_instance_id,
       'ED' AS time_slot,
       activity.prison_code,
       activity.activity_id,
       activity.summary,
       activity_category.name
FROM activity,
     activity_schedule,
     prison_regime,
     scheduled_instance,
     activity_category
WHERE activity.activity_id = activity_schedule.activity_id AND activity.prison_code = prison_regime.prison_code AND activity_schedule.activity_schedule_id = scheduled_instance.activity_schedule_id AND scheduled_instance.start_time = prison_regime.ed_start AND activity.activity_category_id = activity_category.activity_category_id;

CREATE OR REPLACE VIEW v_all_attendance
AS
SELECT a.attendance_id,
       si.session_date,
       ts.time_slot,
       a.status,
       ar.code AS attendance_reason_code,
       a.issue_payment,
       ts.prison_code,
       si.scheduled_instance_id,
       ts.activity_id,
       ts.summary,
       ts.name AS category_name,
       a.prisoner_number,
       a.recorded_time
FROM scheduled_instance si
         JOIN attendance a ON si.scheduled_instance_id = a.scheduled_instance_id
         JOIN v_activity_time_slot ts ON si.scheduled_instance_id = ts.scheduled_instance_id
         LEFT JOIN attendance_reason ar ON a.attendance_reason_id = ar.attendance_reason_id;

CREATE OR REPLACE VIEW v_all_attendance_summary
AS SELECT min(aa.attendance_id) AS id,
          aa.prison_code,
          aa.activity_id,
          aa.category_name,
          aa.session_date,
          aa.time_slot,
          aa.status,
          aa.attendance_reason_code,
          aa.issue_payment,
          count(aa.attendance_id) AS attendance_count
   FROM v_all_attendance aa
   GROUP BY aa.prison_code, aa.activity_id, aa.category_name, aa.session_date, aa.time_slot, aa.status, aa.attendance_reason_code, aa.issue_payment;
