CREATE TABLE activity_organiser
(
    activity_organiser_id bigserial NOT NULL CONSTRAINT activity_organiser_pk PRIMARY KEY,
    code             varchar(20) UNIQUE,
    description      varchar(100) NOT NULL
);

CREATE UNIQUE INDEX idx_activity_organiser_code ON activity_organiser (code);

INSERT INTO activity_organiser (activity_organiser_id, code, description) VALUES
    (1, 'PRISON_STAFF', 'Prison staff'),
    (2, 'PRISONER', 'A prisoner or group of prisoners'),
    (3, 'EXTERNAL_PROVIDER', 'An external provider'),
    (4, 'OTHER', 'Someone else');

ALTER TABLE activity ADD COLUMN activity_organiser_id bigint REFERENCES activity_organiser (activity_organiser_id);

UPDATE activity_tier SET code = 'TIER_1', description = 'Tier 1' WHERE activity_tier_id = 1;

INSERT INTO activity_tier (activity_tier_id, code, description) VALUES
    (2, 'TIER_2', 'Tier 2'),
    (3, 'FOUNDATION', 'Routine activities also called "Foundation"');
