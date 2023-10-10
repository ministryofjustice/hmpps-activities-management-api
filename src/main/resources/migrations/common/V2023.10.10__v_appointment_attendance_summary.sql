CREATE OR REPLACE VIEW v_appointment_attendance_summary AS
SELECT a.appointment_id,
       a.prison_code,
       a.category_code,
       a.custom_name,
       CASE WHEN a.in_cell THEN NULL ELSE a.internal_location_id END    AS internal_location_id,
       a.in_cell,
       a.on_wing,
       a.off_wing,
       a.start_date,
       a.start_time,
       a.end_time,
       a.cancellation_reason_id IS NOT NULL                             AS is_cancelled,
       at.attendee_count,
       at.attended_count,
       at.non_attended_count,
       at.not_recorded_count
FROM appointment a
         LEFT JOIN (
            SELECT
                aa.appointment_id,
                COUNT(aa.appointment_attendee_id) as attendee_count,
                SUM(CASE WHEN aa.attended IS TRUE THEN 1 ELSE 0 END) AS attended_count,
                SUM(CASE WHEN aa.attended IS FALSE THEN 1 ELSE 0 END) AS non_attended_count,
                SUM(CASE WHEN aa.attended IS NULL THEN 1 ELSE 0 END) AS not_recorded_count
            FROM appointment_attendee aa
            WHERE aa.removal_reason_id IS NULL
            GROUP BY aa.appointment_id
        ) AS at ON at.appointment_id = a.appointment_id
WHERE NOT a.is_deleted AND at.attendee_count > 0;
