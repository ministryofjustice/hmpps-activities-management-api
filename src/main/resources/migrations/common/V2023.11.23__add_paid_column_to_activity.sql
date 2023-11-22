ALTER TABLE activity ADD COLUMN paid boolean;

UPDATE activity SET paid = true;

ALTER TABLE activity ALTER COLUMN paid SET NOT NULL;