------------------
--- Activities ---
------------------

--
-- Categories
--
insert into activity_category(activity_category_id, code, name, description)
values (1, 'SAA_EDUCATION', 'Education', 'Such as classes in English, maths, construction and computer skills'),
       (2, 'SAA_INDUSTRIES', 'Industries', 'Such as work in the prison and with employers and charities'),
       (3, 'SAA_SERVICES', 'Services', 'Such as work in the kitchens and laundry, cleaning, gardening, and mentoring'),
       (4, 'SAA_GYM_SPORTS_FITNESS', 'Gym, sport and fitness', 'Such as sport clubs, like football, fitness classes and gym sessions'),
       (5, 'SAA_INDUCTION', 'Induction', 'Such as gym induction, education assessments, health and safety workshops'),
       (6, 'SAA_INTERVENTIONS', 'Intervention programmes', 'Such as programmes for behaviour management, drug and alcohol misuse and community rehabilitation'),
       (7, 'SAA_LEISURE_SOCIAL', 'Leisure and social', 'Such as association, library time and social clubs, like music or art'),
       (8, 'SAA_UNEMPLOYMENT', 'Not in work', 'Such as unemployed, retired, long-term sick, or on remand');

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
insert into rollout_prison (rollout_prison_id, code, description, active, rollout_date)
values (1, 'PVI', 'HMP Pentonville', true, '2022-12-22'),
       (2, 'MDI', 'HMP Moorland', false, '2022-12-22');

--
-- Attendance reason codes
--
insert into attendance_reason(attendance_reason_id, code, description)
values (1, 'ABS', 'Absent'),
       (2, 'ACCAB', 'Acceptable absence'),
       (3, 'ATT', 'Attended'),
       (4, 'CANC', 'Cancelled'),
       (5, 'NREQ', 'Not required'),
       (6, 'SUS', 'Suspend'),
       (7, 'UNACAB', 'Unacceptable absence'),
       (8, 'REST', 'Rest day (no pay)');

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

--------------------
--- Appointments ---
--------------------

--
-- Appointment categories
--

INSERT INTO appointment_category (appointment_category_id, code, description) VALUES (1, 'AC2', 'Appointment Category 2');
INSERT INTO appointment_category (appointment_category_id, code, description, active, display_order) VALUES (2, 'LAC2', 'Legacy Appointment Category 2', false, 2);
INSERT INTO appointment_category (appointment_category_id, code, description, display_order) VALUES (3, 'AC1', 'Appointment Category 1', 3);
INSERT INTO appointment_category (appointment_category_id, code, description) VALUES (4, 'AC3', 'Appointment Category 3');
INSERT INTO appointment_category (appointment_category_id, code, description, active, display_order) VALUES (5, 'LAC1', 'Legacy Appointment Category 1', false, 1);
