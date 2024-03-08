-- =====================================================================================================
-- Update the AUTO_SUSPENDED description from 'Temporarily released' to 'Temporarily absent'
-- =====================================================================================================
UPDATE attendance_reason SET description = 'Temporarily absent'
WHERE code = 'AUTO_SUSPENDED'
