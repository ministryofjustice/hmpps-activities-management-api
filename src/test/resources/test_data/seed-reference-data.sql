--
-- Categories
--
insert into activity_category(activity_category_id, code, description) select 1, 'C1', 'Category 1' where not exists (select 1 from activity_category where activity_category_id = 1);
insert into activity_category(activity_category_id, code, description) select 2, 'C2', 'Category 2' where not exists (select 1 from activity_category where activity_category_id = 2);
insert into activity_category(activity_category_id, code, description) select 3, 'C3', 'Category 3' where not exists (select 1 from activity_category where activity_category_id = 3);

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
