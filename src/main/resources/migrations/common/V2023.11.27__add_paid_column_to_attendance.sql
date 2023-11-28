ALTER TABLE attendance ADD COLUMN paid boolean;

UPDATE attendance SET paid = true;

ALTER TABLE attendance ALTER COLUMN paid SET NOT NULL;