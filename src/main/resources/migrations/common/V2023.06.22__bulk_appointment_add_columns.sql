CREATE INDEX idx_appointment_appointment_description ON appointment (appointment_description);

CREATE TABLE bulk_appointment_new
(
    bulk_appointment_id     bigserial       NOT NULL CONSTRAINT bulk_appointment_new_pk PRIMARY KEY,
    prison_code             varchar(6)      NOT NULL,
    category_code           varchar(12)     NOT NULL,
    appointment_description varchar(40),
    internal_location_id    bigint,
    in_cell                 boolean         NOT NULL DEFAULT false,
    start_date              date            NOT NULL,
    created                 timestamp,
    created_by              varchar(100)
);

INSERT INTO bulk_appointment_new
SELECT
    bulk_appointment_id,
    (SELECT prison_code FROM appointment WHERE appointment_id = (SELECT appointment_id FROM bulk_appointment_appointment baa WHERE baa.bulk_appointment_id = ba.bulk_appointment_id LIMIT 1)),
    (SELECT category_code FROM appointment WHERE appointment_id = (SELECT appointment_id FROM bulk_appointment_appointment baa WHERE baa.bulk_appointment_id = ba.bulk_appointment_id LIMIT 1)),
    (SELECT appointment_description FROM appointment WHERE appointment_id = (SELECT appointment_id FROM bulk_appointment_appointment baa WHERE baa.bulk_appointment_id = ba.bulk_appointment_id LIMIT 1)),
    (SELECT internal_location_id FROM appointment WHERE appointment_id = (SELECT appointment_id FROM bulk_appointment_appointment baa WHERE baa.bulk_appointment_id = ba.bulk_appointment_id LIMIT 1)),
    (SELECT in_cell FROM appointment WHERE appointment_id = (SELECT appointment_id FROM bulk_appointment_appointment baa WHERE baa.bulk_appointment_id = ba.bulk_appointment_id LIMIT 1)),
    (SELECT start_date FROM appointment WHERE appointment_id = (SELECT appointment_id FROM bulk_appointment_appointment baa WHERE baa.bulk_appointment_id = ba.bulk_appointment_id LIMIT 1)),
    created,
    created_by
FROM bulk_appointment ba;

ALTER TABLE bulk_appointment_appointment DROP CONSTRAINT IF EXISTS bulk_appointment_appointment_bulk_appointment_id_fkey;
ALTER TABLE bulk_appointment_appointment DROP CONSTRAINT IF EXISTS bulk_appointment_appointment_appointment_id_fkey;
-- Compatability with integration tests using H2 database
ALTER TABLE bulk_appointment_appointment DROP CONSTRAINT IF EXISTS CONSTRAINT_5E;
ALTER TABLE bulk_appointment_appointment DROP CONSTRAINT IF EXISTS CONSTRAINT_6E;

DROP TABLE bulk_appointment;

ALTER TABLE bulk_appointment_new RENAME CONSTRAINT bulk_appointment_new_pk TO bulk_appointment_pk;
ALTER TABLE bulk_appointment_new RENAME TO bulk_appointment;

ALTER TABLE bulk_appointment_appointment ADD FOREIGN KEY (bulk_appointment_id) REFERENCES bulk_appointment (bulk_appointment_id);
ALTER TABLE bulk_appointment_appointment ADD FOREIGN KEY (appointment_id) REFERENCES appointment (appointment_id);

CREATE INDEX idx_bulk_appointment_category_code ON bulk_appointment (category_code);
CREATE INDEX idx_bulk_appointment_appointment_description ON bulk_appointment (appointment_description);
CREATE INDEX idx_bulk_appointment_internal_location_id ON bulk_appointment (internal_location_id);
CREATE INDEX idx_bulk_appointment_start_date ON bulk_appointment (start_date);

CREATE INDEX idx_bulk_appointment_appointment_bulk_appointment_id ON bulk_appointment_appointment (bulk_appointment_id);
CREATE INDEX idx_bulk_appointment_appointment_appointment_id ON bulk_appointment_appointment (appointment_id);
