ALTER TABLE activity ADD COLUMN off_wing boolean DEFAULT false;
UPDATE activity SET off_wing = false;
ALTER TABLE activity ALTER COLUMN off_wing SET NOT NULL;
