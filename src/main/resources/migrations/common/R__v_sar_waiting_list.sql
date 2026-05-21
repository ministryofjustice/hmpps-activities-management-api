CREATE OR REPLACE VIEW v_sar_waiting_list AS
SELECT wl.waiting_list_id AS waiting_list_id,
       wl.prison_code AS prison_code,
       wl.prisoner_number AS prisoner_number,
       act.summary AS activity_summary,
       wl.application_date AS application_date,
       replace(wl.requested_by, '_', ' ') AS originator,
       replace(wl.status, '_', ' ') AS status,
       wl.status_updated_time AS status_date,
       wl.comments AS comments,
       wl.creation_time as created_date,
       wl.declined_reason AS declined_reason,
       act_cat.name AS activity_category_name,
       act_cat.description AS activity_category_description,
       act.attendance_required AS attendance_required,
       act.paid AS paid,
       act.outside_work AS outside_work,
       act.risk_level AS risk_level,
       eo.description AS organiser,
       act_sch.dps_location_id AS dps_location_id,
       act.in_cell AS in_cell,
       act.off_wing AS off_wing,
       act.on_wing AS on_wing
   FROM waiting_list wl
   JOIN activity act ON act.activity_id = wl.activity_id
   JOIN activity_schedule act_sch ON act_sch.activity_id = act.activity_id
   JOIN activity_category act_cat ON act_cat.activity_category_id = act.activity_category_id
   LEFT JOIN event_organiser eo on eo.event_organiser_id = act.activity_organiser_id;