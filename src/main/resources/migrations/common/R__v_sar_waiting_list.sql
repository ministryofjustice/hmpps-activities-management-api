CREATE OR REPLACE VIEW v_sar_waiting_list AS
SELECT wl.waiting_list_id AS waiting_list_id,
       wl.prison_code AS prison_code,
       wl.prisoner_number AS prisoner_number,
       act.summary AS activity_summary,
       wl.application_date AS application_date,
       wl.requested_by as  originator,
       wl.status AS status,
       wl.status_updated_time AS status_date,
       wl.comments AS comments,
       wl.creation_time as created_date
   FROM waiting_list wl
   JOIN activity act ON act.activity_id = wl.activity_id;
