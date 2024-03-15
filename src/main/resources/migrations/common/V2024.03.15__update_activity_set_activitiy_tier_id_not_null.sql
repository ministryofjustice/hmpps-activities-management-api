-- =====================================================================================================
-- Update the Activity table. activity_tier_id is set to mandatory
-- =====================================================================================================
ALTER TABLE activity ALTER COLUMN activity_tier_id SET NOT NULL;
