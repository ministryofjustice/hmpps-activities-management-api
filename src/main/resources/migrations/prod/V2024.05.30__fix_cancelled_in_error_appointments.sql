-- This fixes the data where an appointment series was cancelled when it should have been cancelled in error
-- Affected appointments: 42621,42622,42623,42620,42632,42633,42625,42626,42627,42628,42629,42630,42619,42624,42631,42634,42635
update appointment
set is_deleted = true, cancellation_reason_id = 1, updated_time = now()
where appointment_series_id = 39950 and start_date > now();
