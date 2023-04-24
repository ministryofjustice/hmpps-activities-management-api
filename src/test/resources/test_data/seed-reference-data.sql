------------------
--- Activities ---
------------------

--
-- Categories
--
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

--
-- Tiers
--
insert into activity_tier(activity_tier_id, code, description)
values (1, 'T1', 'Tier 1'),
       (2, 'T2', 'Tier 2'),
       (3, 'T3', 'Tier 3');

--
-- Eligibility rules
--
insert into eligibility_rule (eligibility_rule_id, code, description)
values (1, 'OVER_21', 'Must be over 21'),
       (2, 'FEMALE_18-50', 'Female aged 18 to 15 only');

--
-- Rollout prisons
--
insert into rollout_prison (rollout_prison_id, code, description, active, rollout_date, appointments_data_source)
values (1, 'PVI', 'HMP Pentonville', true, '2022-12-22', 'PRISON_API'),
       (2, 'MDI', 'HMP Moorland', false, '2022-12-22', 'PRISON_API');

--
-- Attendance reason codes
--
insert into attendance_reason(attendance_reason_id, code, description, attended, capture_pay, capture_more_detail, capture_case_note,
                              capture_incentive_level_warning, capture_other_text, display_in_absence, display_sequence, notes)
values (1, 'SICK', 'Sick', false, true, true, false, false, false, true, 1, 'Maps to ACCAB in NOMIS'),
       (2, 'REFUSED', 'Refused to attend', false, false, false, true, true, false, true, 2, 'Maps to UNACAB in NOMIS'),
       (3, 'NOT_REQUIRED', 'Not required or excused', false, false, false, false, false, false, true, 3, 'Maps to ACCAB in NOMIS'),
       (4, 'REST', 'Rest day', false, true, false, false, false, false, true, 4, 'Maps to ACCAB in NOMIS'),
       (5, 'CLASH', 'Prisoner''s schedule shows another activity', false, false, false, false, false, false, true, 5, 'Maps to ACCAB in NOMIS'),
       (6, 'OTHER', 'Other absence reason not listed', false, true, false, false, false, true, true, 6, 'Maps to UNACAB in NOMIS'),
       (7, 'SUSPENDED', 'Suspended', false, false, false, false, false, false, false, null, 'Maps to ACCAB in NOMIS'),
       (8, 'CANCELLED', 'Cancelled', false, false, false, false, false, false, false, null, 'Maps to ACCAB in NOMIS'),
       (9, 'ATTENDED', 'Attended', true, false, false, false, false, false, false, null, 'Maps to ATT in NOMIS');

--
-- Default prison pay bands
--
insert into prison_pay_band (prison_pay_band_id, display_sequence, nomis_pay_band, pay_band_alias, pay_band_description, prison_code)
values (1, 1, 1, 'Pay band 1 (lowest)', 'Pay band 1 (lowest)', 'PVI'),
       (2, 2, 2, 'Pay band 2', 'Pay band 2', 'PVI'),
       (3, 3, 3, 'Pay band 3', 'Pay band 3', 'PVI'),
       (4, 4, 4, 'Pay band 4', 'Pay band 4', 'PVI'),
       (5, 5, 5, 'Pay band 5', 'Pay band 5', 'PVI'),
       (6, 6, 6, 'Pay band 6', 'Pay band 6', 'PVI'),
       (7, 7, 7, 'Pay band 7', 'Pay band 7', 'PVI'),
       (8, 8, 8, 'Pay band 8', 'Pay band 8', 'PVI'),
       (9, 9, 9, 'Pay band 9', 'Pay band 9', 'PVI'),
       (10, 10, 10, 'Pay band 10 (highest)', 'Pay band 10 (highest)', 'PVI'),
       (11, 1, 1, 'Low', 'Pay band 1 (Lowest)', 'MDI'),
       (12, 2, 2, 'Medium', 'Pay band 2', 'MDI'),
       (13, 3, 3, 'High', 'Pay band 3 (highest)', 'MDI');

INSERT INTO prison_regime
(prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
VALUES('MDI', '09:00:00', '12:00:00', '13:00:00', '16:30:00', '18:00:00', '20:00:00');

--
-- Appointment cancellation reasons
--
INSERT INTO appointment_cancellation_reason (appointment_cancellation_reason_id, description, is_delete)
VALUES   (1, 'Created in error', true),
         (2, 'Cancelled', false);
