CREATE OR REPLACE VIEW v_activity_basic
AS SELECT
     a.prison_code,
     a.activity_id,
     s.activity_schedule_id,
     a.summary,
     a.start_date,
     a.end_date,
     c.activity_category_id as category_id,
     c.code as category_code,
     c.name as category_name
FROM activity a
  JOIN activity_schedule s ON s.activity_id = a.activity_id
  JOIN activity_category c ON c.activity_category_id = a.activity_category_id;
