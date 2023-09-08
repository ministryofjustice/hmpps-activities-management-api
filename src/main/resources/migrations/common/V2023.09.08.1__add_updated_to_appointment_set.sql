ALTER TABLE appointment_set ADD COLUMN updated_time timestamp DEFAULT NULL;
ALTER TABLE appointment_set ADD COLUMN updated_by varchar(100) DEFAULT NULL;
