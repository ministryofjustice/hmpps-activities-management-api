CREATE TABLE appointment_schedule (
    appointment_schedule_id bigserial   NOT NULL CONSTRAINT appointment_schedule_pk PRIMARY KEY,
    repeat_period           varchar(20) NOT NULL,
    repeat_count            integer     NOT NULL
);

CREATE INDEX idx_appointment_schedule_repeat_period ON appointment_schedule (repeat_period);

CREATE TABLE appointment (
    appointment_id          bigserial       NOT NULL CONSTRAINT appointment_pk PRIMARY KEY,
    appointment_type        varchar(10)     NOT NULL,
    category_code           varchar(12)     NOT NULL,
    prison_code             varchar(6)      NOT NULL,
    internal_location_id    bigint,
    in_cell                 boolean         NOT NULL DEFAULT false,
    start_date              date            NOT NULL,
    start_time              time            NOT NULL,
    end_time                time,
    appointment_schedule_id bigint          REFERENCES appointment_schedule (appointment_schedule_id),
    comment                 text            NOT NULL DEFAULT '',
    created                 timestamp       NOT NULL,
    created_by              varchar(100)    NOT NULL,
    updated                 timestamp,
    updated_by              varchar(100),
    deleted                 boolean         NOT NULL DEFAULT false
);

CREATE INDEX idx_appointment_category_code ON appointment (category_code);
CREATE INDEX idx_appointment_prison_code ON appointment (prison_code);
CREATE INDEX idx_appointment_internal_location_id ON appointment (internal_location_id);
CREATE INDEX idx_appointment_start_date ON appointment (start_date);
CREATE INDEX idx_appointment_start_time ON appointment (start_time);
CREATE INDEX idx_appointment_end_time ON appointment (end_time);
CREATE INDEX idx_appointment_schedule_id ON appointment (appointment_schedule_id);

CREATE TABLE appointment_occurrence (
     appointment_occurrence_id  bigserial       NOT NULL CONSTRAINT appointment_occurrence_pk PRIMARY KEY,
     appointment_id             bigint          NOT NULL REFERENCES appointment (appointment_id),
     sequence_number            integer         NOT NULL,
     internal_location_id       bigint,
     in_cell                    boolean         NOT NULL DEFAULT false,
     start_date                 date            NOT NULL,
     start_time                 time            NOT NULL,
     end_time                   time,
     comment                    text,
     cancelled                  boolean         NOT NULL DEFAULT false,
     updated                    timestamp,
     updated_by                 varchar(100)
);

CREATE INDEX idx_appointment_occurrence_appointment_id ON appointment_occurrence (appointment_id);
CREATE INDEX idx_appointment_occurrence_internal_location_id ON appointment_occurrence (internal_location_id);
CREATE INDEX idx_appointment_occurrence_start_date ON appointment_occurrence (start_date);
CREATE INDEX idx_appointment_occurrence_start_time ON appointment_occurrence (start_time);
CREATE INDEX idx_appointment_occurrence_end_time ON appointment_occurrence (end_time);

CREATE TABLE appointment_occurrence_allocation (
    appointment_occurrence_allocation_id    bigserial   NOT NULL CONSTRAINT appointment_allocation_pk PRIMARY KEY,
    appointment_occurrence_id               bigint      NOT NULL REFERENCES appointment_occurrence (appointment_occurrence_id),
    prisoner_number                         varchar(10) NOT NULL,
    booking_id                              bigint      NOT NULL
);

CREATE INDEX idx_appointment_occurrence_allocation_appointment_occurrence_id ON appointment_occurrence_allocation (appointment_occurrence_id);
CREATE INDEX idx_appointment_occurrence_allocation_prisoner_number ON appointment_occurrence_allocation (prisoner_number);
CREATE INDEX idx_appointment_occurrence_allocation_booking_id ON appointment_occurrence_allocation (booking_id);

CREATE OR REPLACE VIEW v_appointment_instance AS
    SELECT
        aoa.appointment_occurrence_allocation_id AS appointment_instance_id,
        a.appointment_id,
        ao.appointment_occurrence_id,
        aoa.appointment_occurrence_allocation_id,
        a.category_code,
        a.prison_code,
        CASE WHEN ao.in_cell THEN null ELSE ao.internal_location_id END AS internal_location_id,
        ao.in_cell,
        aoa.prisoner_number,
        aoa.booking_id,
        ao.start_date AS appointment_date,
        ao.start_time,
        ao.end_time,
        COALESCE(ao.comment, a.comment) AS comment,
        a.created,
        a.created_by,
        ao.updated,
        ao.updated_by
    FROM appointment_occurrence_allocation aoa
        JOIN appointment_occurrence ao on aoa.appointment_occurrence_id = ao.appointment_occurrence_id
        JOIN appointment a on a.appointment_id = ao.appointment_id
