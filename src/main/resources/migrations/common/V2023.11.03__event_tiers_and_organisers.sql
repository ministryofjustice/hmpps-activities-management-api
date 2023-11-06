--Organisers
CREATE TABLE event_organiser
(
    event_organiser_id bigserial NOT NULL CONSTRAINT event_organiser_pk PRIMARY KEY,
    code             varchar(20) UNIQUE,
    description      varchar(100) NOT NULL
);

CREATE UNIQUE INDEX idx_event_organiser_code ON event_organiser (code);

INSERT INTO event_organiser (event_organiser_id, code, description) VALUES
    (1, 'PRISON_STAFF', 'Prison staff'),
    (2, 'PRISONER', 'A prisoner or group of prisoners'),
    (3, 'EXTERNAL_PROVIDER', 'An external provider'),
    (4, 'OTHER', 'Someone else');

ALTER TABLE activity ADD COLUMN activity_organiser_id bigint REFERENCES event_organiser (event_organiser_id);


--Tiers
ALTER TABLE activity_tier RENAME TO event_tier;

ALTER TABLE event_tier RENAME COLUMN activity_tier_id TO event_tier_id;

CREATE UNIQUE INDEX idx_event_tier_code ON event_tier (code);

UPDATE event_tier SET code = 'TIER_1', description = 'Tier 1' WHERE event_tier_id = 1;

INSERT INTO event_tier (event_tier_id, code, description) VALUES
    (2, 'TIER_2', 'Tier 2'),
    (3, 'FOUNDATION', 'Routine activities also called "Foundation"');

-- Update existing "not in work" activities
UPDATE activity SET activity_tier_id = 3 WHERE activity_category_id = 8;
