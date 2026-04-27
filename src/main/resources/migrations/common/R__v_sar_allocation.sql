-- =================================================================================================
-- Creates the allocations view necessary to support subject access requests on behalf of a prisoner
-- =================================================================================================
CREATE OR REPLACE VIEW v_sar_allocation AS
SELECT act.prison_code AS prison_code,
       allo.allocation_id AS allocation_id,
       allo.prisoner_number AS prisoner_number,
       replace(allo.prisoner_status, '_', ' ') AS prisoner_status,
       allo.start_date AS start_date,
       allo.end_date AS end_date,
       act.activity_id AS activity_id,
       act.summary AS activity_summary,
       ppb.pay_band_description AS pay_band,
       allo.allocated_time AS created_date,
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
  FROM allocation allo
  JOIN activity_schedule act_sch ON act_sch.activity_schedule_id = allo.activity_schedule_id
  JOIN activity act ON act.activity_id = act_sch.activity_id
  JOIN activity_category act_cat ON act_cat.activity_category_id = act.activity_category_id
  LEFT OUTER JOIN prison_pay_band ppb ON ppb.prison_pay_band_id = allo.prison_pay_band_id
  LEFT JOIN event_organiser eo on eo.event_organiser_id = act.activity_organiser_id
