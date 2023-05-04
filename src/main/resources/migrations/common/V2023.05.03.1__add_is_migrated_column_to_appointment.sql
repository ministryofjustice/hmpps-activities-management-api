ALTER TABLE appointment
ADD COLUMN is_migrated bool NOT NULL DEFAULT false;