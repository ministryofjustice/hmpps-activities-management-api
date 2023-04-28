-- =============================================
-- ACTIVITY CATEGORIES
-- =============================================
insert into activity_category(activity_category_id, code, name, description)
values (1, 'SAA_EDUCATION', 'Education', 'Such as classes in English, maths, construction or barbering'),
       (2, 'SAA_INDUSTRIES', 'Industries', 'Such as work like recycling, packing or assembly operated by the prison, external firms or charities'),
       (3, 'SAA_PRISON_JOBS', 'Prison jobs', 'Such as kitchen, cleaning, gardens or other maintenance and services to keep the prison running'),
       (4, 'SAA_GYM_SPORTS_FITNESS', 'Gym, sport, fitness', 'Such as sports clubs, like football, or recreational gym sessions'),
       (5, 'SAA_INDUCTION', 'Induction', 'Such as gym induction, education assessments or health and safety workshops'),
       (6, 'SAA_INTERVENTIONS', 'Intervention programmes', 'Such as programmes for behaviour management, drug and alcohol misuse and community rehabilitation'),
       (7, 'SAA_FAITH_SPIRITUALITY', 'Faith and spirituality', 'Such as chapel, prayer meetings or meditation'),
       (8, 'SAA_NOT_IN_WORK', 'Not in work', 'Such as unemployed, retired, long-term sick, or on remand'),
       (9, 'SAA_OTHER', 'Other', 'Select if the activity you’re creating doesn’t fit any other category');

alter sequence if exists activity_category_activity_category_id_seq restart with 10;

-- =============================================
-- ACTIVITY TIERS
-- =============================================
insert into activity_tier(activity_tier_id, code, description) values (1, 'Tier1', 'Work, education and maintenance');

alter sequence if exists activity_tier_activity_tier_id_seq restart with 2;

-- =============================================
-- ATTENDANCE REASONS
-- =============================================
insert into attendance_reason(attendance_reason_id, code, description, attended, capture_pay,
                              capture_more_detail, capture_case_note,
                              capture_incentive_level_warning, capture_other_text,
                              display_in_absence, display_sequence, notes)
values (1, 'SICK', 'Sick', false, true, true, false, false, false, true, 1, 'Maps to ACCAB in NOMIS'),
       (2, 'REFUSED', 'Refused to attend', false, false, false, true, true, false, true, 2, 'Maps to UNACAB in NOMIS'),
       (3, 'NOT_REQUIRED', 'Not required or excused', false, false, false, false, false, false, true, 3, 'Maps to ACCAB in NOMIS'),
       (4, 'REST', 'Rest day', false, true, false, false, false, false, true, 4, 'Maps to ACCAB in NOMIS'),
       (5, 'CLASH', 'Prisoner''s schedule shows another activity', false, false, false, false, false, false, true, 5, 'Maps to ACCAB in NOMIS'),
       (6, 'OTHER', 'Other absence reason not listed', false, true, false, false, false, true, true, 6, 'Maps to UNACAB in NOMIS'),
       (7, 'SUSPENDED', 'Suspended', false, false, false, false, false, false, false, null, 'Maps to ACCAB in NOMIS'),
       (8, 'CANCELLED', 'Cancelled', false, false, false, false, false, false, false, null, 'Maps to ACCAB in NOMIS'),
       (9, 'ATTENDED', 'Attended', true, false, false, false, false, false, false, null, 'Maps to ATT in NOMIS');

alter sequence if exists attendance_reason_attendance_reason_id_seq restart with 10;

-- =============================================
-- APPOINTMENT CANCELLATION REASONS
-- =============================================
INSERT INTO appointment_cancellation_reason (appointment_cancellation_reason_id, description, is_delete)
VALUES   (1, 'Created in error', true),
         (2, 'Cancelled', false);
