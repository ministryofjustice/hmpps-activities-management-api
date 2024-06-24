-- =================================================================================================
-- Creates the allocations view necessary to support subject access requests on behalf of a prisoner
-- =================================================================================================

CREATE OR REPLACE VIEW v_sar_allocation AS
SELECT act.prison_code AS prison_code,
       allo.allocation_id AS allocation_id,
       allo.prisoner_number AS prisoner_number,
       allo.prisoner_status AS prisoner_status,
       allo.start_date AS start_date,
       allo.end_date AS end_date,
       act.activity_id AS activity_id,
       act.summary AS activity_summary,
       ppb.pay_band_description AS pay_band,
       allo.allocated_time AS created_date
  FROM allocation allo
  JOIN activity_schedule act_sch ON act_sch.activity_schedule_id = allo.activity_schedule_id
  JOIN activity act ON act.activity_id = act_sch.activity_id
  LEFT OUTER JOIN prison_pay_band ppb ON ppb.prison_pay_band_id = allo.prison_pay_band_id
