ALTER TABLE prison_regime ADD COLUMN max_days_to_expiry integer default 5;

UPDATE prison_regime set max_days_to_expiry = 5;

ALTER TABLE prison_regime ALTER COLUMN max_days_to_expiry SET NOT NULL;