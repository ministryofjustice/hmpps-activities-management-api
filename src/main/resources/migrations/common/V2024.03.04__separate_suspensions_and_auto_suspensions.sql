-- =====================================================================================
-- Rename AUTO_SUSPENDED to TEMPORARILY_RELEASED
-- =====================================================================================
INSERT INTO attendance_reason(attendance_reason_id, code, description, attended, capture_pay,
                              capture_more_detail, capture_case_note,
                              capture_incentive_level_warning, capture_other_text,
                              display_in_absence, display_sequence, notes)
VALUES (10, 'AUTO_SUSPENDED', 'Temporarily released', false, false, false, false, false, false, false, null, 'Maps to ACCAB in NOMIS');

UPDATE attendance a
SET attendance_reason_id = 10 -- i.e. AUTO_SUSPENDED
WHERE a.attendance_reason_id = 7; -- i.e. SUSPENDED

UPDATE attendance_history ah
SET attendance_reason_id = 10 -- i.e. AUTO_SUSPENDED
WHERE ah.attendance_reason_id = 7; -- i.e. SUSPENDED
