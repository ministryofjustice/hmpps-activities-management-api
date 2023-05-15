ALTER TABLE activity ADD COLUMN updated_time timestamp;
ALTER TABLE activity ADD COLUMN updated_by varchar(100);

ALTER TABLE activity_schedule ADD COLUMN updated_time timestamp;
ALTER TABLE activity_schedule ADD COLUMN updated_by varchar(100);