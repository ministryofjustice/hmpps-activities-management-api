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
	COALESCE((
	    SELECT COUNT(DISTINCT al.allocation_id)
		FROM allocation al
		WHERE al.activity_schedule_id = si.activity_schedule_id
		AND al.start_date <= si.session_date
		AND (al.end_date IS NULL OR al.end_date >= si.session_date)
		GROUP BY al.activity_schedule_id
    ), 0) AS allocations,
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
