-- Views will be re-created
DROP VIEW IF EXISTS v_sar_appointment;
DROP VIEW IF EXISTS v_appointment_instance;

ALTER TABLE appointment ALTER COLUMN prisoner_extra_information TYPE varchar(800);

ALTER TABLE appointment_series ALTER COLUMN prisoner_extra_information TYPE varchar(800);