SELECT setval('appointment_series_schedule_appointment_series_schedule_id_seq', (SELECT max(appointment_series_schedule_id) FROM appointment_series_schedule), true);
SELECT setval('appointment_series_appointment_series_id_seq', (SELECT max(appointment_series_id) FROM appointment_series), true);
ALTER SEQUENCE appointment_appointment_id_seq1 RENAME TO appointment_appointment_id_seq;
SELECT setval('appointment_appointment_id_seq', (SELECT max(appointment_id) FROM appointment), true);
SELECT setval('appointment_attendee_appointment_attendee_id_seq', (SELECT max(appointment_attendee_id) FROM appointment_attendee), true);
SELECT setval('appointment_set_appointment_set_id_seq', (SELECT max(appointment_set_id) FROM appointment_set), true);
SELECT setval('appointment_set_appointment_s_appointment_set_appointment_s_seq', (SELECT max(appointment_set_appointment_series_id) FROM appointment_set_appointment_series), true);
