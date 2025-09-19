-- =============================================
-- RAPT is being moved from 'Other' to 'Interventions'
-- =============================================
UPDATE appointment_category
SET appointment_parent_category_id = (
  SELECT appointment_parent_category_id
  FROM appointment_parent_category
  WHERE name = 'Interventions'
)
WHERE code = 'RAPT';