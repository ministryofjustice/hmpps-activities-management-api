insert into activity_tier(activity_tier_id, code, description) select 1, 'Tier1', 'Work, education and maintenance' where not exists (select 1 from activity_tier where activity_tier_id = 1);
