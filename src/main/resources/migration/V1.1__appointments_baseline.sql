CREATE TABLE appointment_schedule (
    appointment_schedule_id bigserial   NOT NULL CONSTRAINT appointment_schedule_pk PRIMARY KEY,
    repeat_period           varchar(20) NOT NULL,
    repeat_count            integer     NOT NULL
);

CREATE INDEX idx_appointment_schedule_repeat_period ON appointment_schedule (repeat_period);

CREATE TABLE appointment (
    appointment_id          bigserial       NOT NULL CONSTRAINT appointment_pk PRIMARY KEY,
    appointment_type        varchar(10)     NOT NULL,
    prison_code             varchar(6)      NOT NULL,
    category_code           varchar(12)     NOT NULL,
    appointment_description varchar(40),
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
    updated_by              varchar(100)
);

CREATE INDEX idx_appointment_category_code ON appointment (category_code);
CREATE INDEX idx_appointment_prison_code ON appointment (prison_code);
CREATE INDEX idx_appointment_internal_location_id ON appointment (internal_location_id);
CREATE INDEX idx_appointment_start_date ON appointment (start_date);
CREATE INDEX idx_appointment_start_time ON appointment (start_time);
CREATE INDEX idx_appointment_end_time ON appointment (end_time);
CREATE INDEX idx_appointment_schedule_id ON appointment (appointment_schedule_id);

CREATE TABLE appointment_cancellation_reason (
     appointment_cancellation_reason_id bigserial NOT NULL CONSTRAINT appointment_cancellation_reason_pk PRIMARY KEY,
     description varchar(50) NOT NULL,
     is_delete boolean NOT NULL
);

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
     cancelled                  timestamp,
     cancellation_reason_id     bigint          REFERENCES appointment_cancellation_reason (appointment_cancellation_reason_id),
     cancelled_by               varchar(100),
     updated                    timestamp,
     updated_by                 varchar(100),
     deleted                    boolean         NOT NULL DEFAULT false
);

CREATE INDEX idx_appointment_occurrence_appointment_id ON appointment_occurrence (appointment_id);
CREATE INDEX idx_appointment_occurrence_internal_location_id ON appointment_occurrence (internal_location_id);
CREATE INDEX idx_appointment_occurrence_start_date ON appointment_occurrence (start_date);
CREATE INDEX idx_appointment_occurrence_start_time ON appointment_occurrence (start_time);
CREATE INDEX idx_appointment_occurrence_end_time ON appointment_occurrence (end_time);
CREATE INDEX idx_appointment_occurrence_cancellation_reason_id ON appointment_occurrence (cancellation_reason_id);

CREATE TABLE appointment_occurrence_allocation (
    appointment_occurrence_allocation_id    bigserial   NOT NULL CONSTRAINT appointment_allocation_pk PRIMARY KEY,
    appointment_occurrence_id               bigint      NOT NULL REFERENCES appointment_occurrence (appointment_occurrence_id),
    prisoner_number                         varchar(10) NOT NULL,
    booking_id                              bigint      NOT NULL
);

CREATE INDEX idx_appointment_occurrence_allocation_appointment_occurrence_id ON appointment_occurrence_allocation (appointment_occurrence_id);
CREATE INDEX idx_appointment_occurrence_allocation_prisoner_number ON appointment_occurrence_allocation (prisoner_number);
CREATE INDEX idx_appointment_occurrence_allocation_booking_id ON appointment_occurrence_allocation (booking_id);

CREATE TABLE bulk_appointment (
        bulk_appointment_id        bigserial   NOT NULL CONSTRAINT bulk_appointment_pk PRIMARY KEY,
        created                    timestamp,
        created_by                 varchar(100)
);

CREATE TABLE bulk_appointment_appointment (
        bulk_appointment_appointment_id        bigserial   NOT NULL CONSTRAINT bulk_appointment_appointment_pk PRIMARY KEY,
        bulk_appointment_id                    bigint      NOT NULL REFERENCES bulk_appointment (bulk_appointment_id) ON DELETE CASCADE,
        appointment_id                         bigint      NOT NULL REFERENCES appointment (appointment_id) ON DELETE CASCADE
);

CREATE OR REPLACE VIEW v_appointment_instance AS
    SELECT
        aoa.appointment_occurrence_allocation_id AS appointment_instance_id,
        a.appointment_id,
        ao.appointment_occurrence_id,
        aoa.appointment_occurrence_allocation_id,
        a.appointment_type,
        a.prison_code,
        aoa.prisoner_number,
        aoa.booking_id,
        a.category_code,
        a.appointment_description,
        CASE WHEN ao.in_cell THEN null ELSE ao.internal_location_id END AS internal_location_id,
        ao.in_cell,
        ao.start_date AS appointment_date,
        ao.start_time,
        ao.end_time,
        COALESCE(ao.comment, a.comment) AS comment,
        CASE WHEN ao.cancellation_reason_id IS NULL THEN false ELSE NOT is_delete END AS is_cancelled,
        a.created,
        a.created_by,
        ao.updated,
        ao.updated_by
    FROM appointment_occurrence_allocation aoa
        JOIN appointment_occurrence ao on aoa.appointment_occurrence_id = ao.appointment_occurrence_id
        JOIN appointment a on a.appointment_id = ao.appointment_id
        LEFT JOIN appointment_cancellation_reason acr on ao.cancellation_reason_id = acr.appointment_cancellation_reason_id;

CREATE OR REPLACE VIEW v_appointment_occurrence_search AS
    SELECT
        ao.appointment_id,
        ao.appointment_occurrence_id,
        a.appointment_type,
        a.prison_code,
        a.category_code,
        a.appointment_description,
        CASE WHEN ao.in_cell THEN null ELSE ao.internal_location_id END AS internal_location_id,
        ao.in_cell,
        ao.start_date,
        ao.start_time,
        ao.end_time,
        a.appointment_schedule_id IS NOT NULL as is_repeat,
        ao.sequence_number,
        COALESCE(asch.repeat_count, 1) as max_sequence_number,
        COALESCE(ao.comment, a.comment) AS comment,
        a.created_by,
        ao.updated IS NULL as is_edited,
        CASE WHEN ao.cancellation_reason_id IS NULL THEN false ELSE NOT is_delete END AS is_cancelled
    FROM
        appointment_occurrence ao JOIN appointment a on a.appointment_id = ao.appointment_id
        LEFT JOIN appointment_schedule asch on a.appointment_schedule_id = asch.appointment_schedule_id
        LEFT JOIN appointment_cancellation_reason acr on ao.cancellation_reason_id = acr.appointment_cancellation_reason_id;
    WHERE ao.deleted != true;


