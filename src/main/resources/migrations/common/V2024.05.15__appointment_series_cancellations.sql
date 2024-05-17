-- =====================================================================================================
-- Alter the appointment_series table. add columns for series cancellations
-- =====================================================================================================
ALTER TABLE appointment_series ADD COLUMN cancelled_by varchar(100);
ALTER TABLE appointment_series ADD COLUMN cancelled_time timestamp;
ALTER TABLE appointment_series ADD COLUMN cancellation_start_date date;
ALTER TABLE appointment_series ADD COLUMN cancellation_start_time time;
