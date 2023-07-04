ALTER TABLE activity ADD COLUMN on_wing boolean DEFAULT false;
UPDATE activity SET on_wing = false;
ALTER TABLE activity ALTER COLUMN on_wing SET NOT NULL;
