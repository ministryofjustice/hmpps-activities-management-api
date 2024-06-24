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
