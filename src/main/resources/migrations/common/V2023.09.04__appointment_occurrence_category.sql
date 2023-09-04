ALTER TABLE appointment_occurrence ADD COLUMN category_code varchar(12);
ALTER TABLE appointment_occurrence ADD COLUMN appointment_description varchar(40);

UPDATE appointment_occurrence ao SET
    category_code = (SELECT category_code FROM appointment a WHERE appointment_id = ao.appointment_id),
    appointment_description = (SELECT appointment_description FROM appointment a WHERE appointment_id = ao.appointment_id);

ALTER TABLE appointment_occurrence ALTER COLUMN category_code SET NOT NULL;

CREATE INDEX idx_appointment_occurrence_category_code ON appointment_occurrence (category_code);
CREATE INDEX idx_appointment_occurrence_appointment_description ON appointment_occurrence (appointment_description);
