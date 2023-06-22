ALTER SEQUENCE bulk_appointment_new_bulk_appointment_id_seq RENAME TO bulk_appointment_bulk_appointment_id_seq;
SELECT setval('bulk_appointment_bulk_appointment_id_seq', (SELECT max(bulk_appointment_id) FROM bulk_appointment), true);
