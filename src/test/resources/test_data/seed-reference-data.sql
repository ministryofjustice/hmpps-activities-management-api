--
-- Categories
--
insert into activity_category(activity_category_id, code, name, description) select 1, 'C1', 'Category 1', 'Description of Category 1' where not exists (select 1 from activity_category where activity_category_id = 1);
insert into activity_category(activity_category_id, code, name, description) select 2, 'C2', 'Category 2', 'Description of Category 2' where not exists (select 1 from activity_category where activity_category_id = 2);
insert into activity_category(activity_category_id, code, name, description) select 3, 'C3', 'Category 3', 'Description of Category 3' where not exists (select 1 from activity_category where activity_category_id = 3);

--
-- Tiers
--
insert into activity_tier(activity_tier_id, code, description) select 1, 'T1', 'Tier 1' where not exists (select 1 from activity_tier where activity_tier_id = 1);
insert into activity_tier(activity_tier_id, code, description) select 2, 'T2', 'Tier 2' where not exists (select 1 from activity_tier where activity_tier_id = 2);
insert into activity_tier(activity_tier_id, code, description) select 3, 'T3', 'Tier 3' where not exists (select 1 from activity_tier where activity_tier_id = 3);

--
-- Eligibility rules
--
insert into eligibility_rule (eligibility_rule_id, code, description) select 1, 'OVER_21', 'Must be over 21' where not exists (select 1 from eligibility_rule where eligibility_rule_id = 1);
insert into eligibility_rule (eligibility_rule_id, code, description) select 2, 'FEMALE_18-50', 'Female aged 18 to 15 only' where not exists (select 1 from eligibility_rule where eligibility_rule_id = 2);


--
-- Rollout prisons
--
insert into rollout_prison (rollout_prison_id, code, description, active) select 1, 'PVI', 'HMP Pentonville', true where not exists (select 1 from rollout_prison where rollout_prison_id = 1);
insert into rollout_prison (rollout_prison_id, code, description, active) select 2, 'MDI', 'HMP Moorland', false where not exists (select 1 from rollout_prison where rollout_prison_id = 2);

--
-- Attendance reason codes
--
insert into attendance_reason(attendance_reason_id, code, description) select 1, 'ABS', 'Absent' where not exists (select 1 from attendance_reason where attendance_reason_id = 1);
insert into attendance_reason(attendance_reason_id, code, description) select 2, 'ACCAB', 'Acceptable absence' where not exists (select 1 from attendance_reason where attendance_reason_id = 2);
insert into attendance_reason(attendance_reason_id, code, description) select 3, 'ATT', 'Attended' where not exists (select 1 from attendance_reason where attendance_reason_id = 3);
insert into attendance_reason(attendance_reason_id, code, description) select 4, 'CANC', 'Cancelled' where not exists (select 1 from attendance_reason where attendance_reason_id = 4);
insert into attendance_reason(attendance_reason_id, code, description) select 5, 'NREQ', 'Not required' where not exists (select 1 from attendance_reason where attendance_reason_id = 5);
insert into attendance_reason(attendance_reason_id, code, description) select 6, 'SUS', 'Suspend' where not exists (select 1 from attendance_reason where attendance_reason_id = 6);
insert into attendance_reason(attendance_reason_id, code, description) select 7, 'UNACAB', 'Unacceptable absence' where not exists (select 1 from attendance_reason where attendance_reason_id = 7);
insert into attendance_reason(attendance_reason_id, code, description) select 8, 'REST', 'Rest day (no pay)' where not exists (select 1 from attendance_reason where attendance_reason_id = 8);
