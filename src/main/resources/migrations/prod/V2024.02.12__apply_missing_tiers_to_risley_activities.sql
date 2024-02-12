-- The Risley activities affected here are both archived so cannot be updated via the UI. These activities were created
-- before the Tier work was released. Both activities are Tier 1 activities

UPDATE activity SET activity_tier_id = (SELECT et.event_tier_id from event_tier et WHERE et.code = 'TIER_1')
 WHERE prison_code = 'RSI' AND activity_tier_id is null and activity_id in (187, 189);
