-- =====================================================================================================
-- Update the event_review table. add a description column and populate the data
-- =====================================================================================================
ALTER TABLE event_review ADD COLUMN event_description varchar(50);

UPDATE event_review set event_description = 'RELEASED' where event_type = 'prison-offender-events.prisoner.released';
UPDATE event_review set event_description = 'RELEASED' where event_type = 'prisoner-offender-search.prisoner.released';
UPDATE event_review set event_description = 'ACTIVITY_SUSPENDED' where event_type = 'prison-offender-events.prisoner.activities-changed' and event_data like '%''SUSPEND''%';
UPDATE event_review set event_description = 'ACTIVITY_ENDED' where event_type = 'prison-offender-events.prisoner.activities-changed' and event_data like '%''END''%';
