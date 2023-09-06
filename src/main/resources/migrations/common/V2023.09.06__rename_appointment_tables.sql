-- =============================================
-- APPOINTMENT TABLES WITH CORRECT NAMES
-- =============================================

CREATE TABLE appointment_tier
(
    appointment_tier_id bigserial   NOT NULL CONSTRAINT appointment_tier_pk PRIMARY KEY,
    description                     varchar(100) NOT NULL
);
INSERT INTO appointment_tier
VALUES   (1, 'Tier 1'),
         (2, 'Tier 2'),
         (3, 'No tier, this activity is not considered ''purposeful'' for reporting'),
         (4, 'Not specified');

CREATE TABLE appointment_host
(
    appointment_host_id bigserial   NOT NULL CONSTRAINT appointment_host_pk PRIMARY KEY,
    description                     varchar(100) NOT NULL
);
INSERT INTO appointment_host
VALUES   (1, 'Prison staff'),
         (2, 'A prisoner or group of prisoners'),
         (3, 'An external provider'),
         (4, 'Someone else');

CREATE TABLE appointment_series_schedule
(
    appointment_series_schedule_id  bigserial    NOT NULL CONSTRAINT appointment_series_schedule_pk PRIMARY KEY,
    frequency                       varchar(20)  NOT NULL,
    number_of_appointments          integer      NOT NULL
);
CREATE INDEX idx_appointment_series_schedule_frequency ON appointment_series_schedule (frequency);

CREATE TABLE appointment_series
(
    appointment_series_id           bigserial    NOT NULL CONSTRAINT appointment_series_pk PRIMARY KEY,
    appointment_type                varchar(10)  NOT NULL,
    prison_code                     varchar(6)   NOT NULL,
    category_code                   varchar(12)  NOT NULL,
    custom_name                     varchar(40)  DEFAULT NULL,
    appointment_tier_id             bigint       NOT NULL REFERENCES appointment_tier (appointment_tier_id),
    appointment_host_id             bigint       DEFAULT NULL REFERENCES appointment_host (appointment_host_id),
    internal_location_id            bigint,
    custom_location                 varchar(40)  DEFAULT NULL,
    in_cell                         boolean      NOT NULL DEFAULT false,
    on_wing                         boolean      NOT NULL DEFAULT false,
    off_wing                        boolean      NOT NULL DEFAULT true,
    start_date                      date         NOT NULL,
    start_time                      time         NOT NULL,
    end_time                        time,
    appointment_series_schedule_id  bigint       DEFAULT NULL REFERENCES appointment_series_schedule (appointment_series_schedule_id),
    unlock_notes                    text         DEFAULT NULL,
    extra_information               text         DEFAULT NULL,
    created_time                    timestamp    NOT NULL,
    created_by                      varchar(100) NOT NULL,
    updated_time                    timestamp    DEFAULT NULL,
    updated_by                      varchar(100) DEFAULT NULL,
    is_migrated                     boolean      NOT NULL DEFAULT false
);
CREATE INDEX idx_appointment_series_prison_code ON appointment_series (prison_code);
CREATE INDEX idx_appointment_series_category_code ON appointment_series (category_code);
CREATE INDEX idx_appointment_series_custom_name ON appointment_series (custom_name);
CREATE INDEX idx_appointment_series_appointment_tier_id ON appointment_series (appointment_tier_id);
CREATE INDEX idx_appointment_series_appointment_host_id ON appointment_series (appointment_host_id);
CREATE INDEX idx_appointment_series_internal_location_id ON appointment_series (internal_location_id);
CREATE INDEX idx_appointment_series_custom_location ON appointment_series (custom_location);
CREATE INDEX idx_appointment_series_start_date ON appointment_series (start_date);
CREATE INDEX idx_appointment_series_start_time ON appointment_series (start_time);
CREATE INDEX idx_appointment_series_end_time ON appointment_series (end_time);
CREATE INDEX idx_appointment_series_schedule_id ON appointment_series (appointment_series_schedule_id);
CREATE INDEX idx_appointment_series_created_by ON appointment_series (created_by);

ALTER TABLE appointment RENAME TO appointment_old;
ALTER TABLE appointment_old RENAME CONSTRAINT appointment_pk TO appointment_old_pk;
DROP INDEX IF EXISTS idx_appointment_prison_code;
DROP INDEX IF EXISTS idx_appointment_category_code;
DROP INDEX IF EXISTS idx_appointment_internal_location_id;
DROP INDEX IF EXISTS idx_appointment_start_date;
DROP INDEX IF EXISTS idx_appointment_start_time;
DROP INDEX IF EXISTS idx_appointment_end_time;

CREATE TABLE appointment
(
    appointment_id                  bigserial    NOT NULL CONSTRAINT appointment_pk PRIMARY KEY,
    appointment_series_id           bigint       NOT NULL REFERENCES appointment_series (appointment_series_id),
    sequence_number                 integer      NOT NULL,
    prison_code                     varchar(6)   NOT NULL,
    category_code                   varchar(12)  NOT NULL,
    custom_name                     varchar(40)  DEFAULT NULL,
    appointment_tier_id             bigint       NOT NULL REFERENCES appointment_tier (appointment_tier_id),
    appointment_host_id             bigint       DEFAULT NULL REFERENCES appointment_host (appointment_host_id),
    internal_location_id            bigint,
    custom_location                 varchar(40)  DEFAULT NULL,
    in_cell                         boolean      NOT NULL DEFAULT false,
    on_wing                         boolean      NOT NULL DEFAULT false,
    off_wing                        boolean      NOT NULL DEFAULT true,
    start_date                      date         NOT NULL,
    start_time                      time         NOT NULL,
    end_time                        time,
    unlock_notes                    text         DEFAULT NULL,
    extra_information               text         DEFAULT NULL,
    created_time                    timestamp    NOT NULL,
    created_by                      varchar(100) NOT NULL,
    updated_time                    timestamp    DEFAULT NULL,
    updated_by                      varchar(100) DEFAULT NULL,
    cancelled_time                  timestamp,
    cancellation_reason_id          bigint       REFERENCES appointment_cancellation_reason (appointment_cancellation_reason_id),
    cancelled_by                    varchar(100),
    is_deleted                      boolean      NOT NULL DEFAULT false
);
CREATE INDEX idx_appointment_appointment_series_id ON appointment (appointment_series_id);
CREATE INDEX idx_appointment_prison_code ON appointment (prison_code);
CREATE INDEX idx_appointment_category_code ON appointment (category_code);
CREATE INDEX idx_appointment_custom_name ON appointment (custom_name);
CREATE INDEX idx_appointment_appointment_tier_id ON appointment (appointment_tier_id);
CREATE INDEX idx_appointment_appointment_host_id ON appointment (appointment_host_id);
CREATE INDEX idx_appointment_internal_location_id ON appointment (internal_location_id);
CREATE INDEX idx_appointment_custom_location ON appointment (custom_location);
CREATE INDEX idx_appointment_start_date ON appointment (start_date);
CREATE INDEX idx_appointment_start_time ON appointment (start_time);
CREATE INDEX idx_appointment_end_time ON appointment (end_time);
CREATE INDEX idx_appointment_created_by ON appointment (created_by);
CREATE INDEX idx_appointment_cancellation_reason_id ON appointment (cancellation_reason_id);

CREATE TABLE appointment_attendee
(
    appointment_attendee_id     bigserial        NOT NULL CONSTRAINT appointment_attendee_pk PRIMARY KEY,
    appointment_id              bigint           NOT NULL REFERENCES appointment (appointment_id),
    prisoner_number             varchar(10)      NOT NULL,
    booking_id                  bigint           NOT NULL,
    added_time                  timestamp        DEFAULT NULL,
    added_by                    varchar(100)     DEFAULT NULL,
    attended                    boolean          DEFAULT NULL,
    attendance_recorded_time    timestamp        DEFAULT NULL,
    attendance_recorded_by      varchar(100)     DEFAULT NULL,
    removed_time                timestamp        DEFAULT NULL,
    removed_by                  varchar(100)     DEFAULT NULL
);
CREATE INDEX idx_appointment_attendee_appointment_id ON appointment_attendee (appointment_id);
CREATE INDEX idx_appointment_attendee_prisoner_number ON appointment_attendee (prisoner_number);
CREATE INDEX idx_appointment_attendee_booking_id ON appointment_attendee (booking_id);

CREATE TABLE appointment_set
(
    appointment_set_id          bigserial        NOT NULL CONSTRAINT appointment_set_pk PRIMARY KEY,
    prison_code                 varchar(6)       NOT NULL,
    category_code               varchar(12)      NOT NULL,
    custom_name                 varchar(40)      DEFAULT NULL,
    appointment_tier_id         bigint           NOT NULL REFERENCES appointment_tier (appointment_tier_id),
    appointment_host_id         bigint           DEFAULT NULL REFERENCES appointment_host (appointment_host_id),
    internal_location_id        bigint,
    custom_location             varchar(40)      DEFAULT NULL,
    in_cell                     boolean          NOT NULL DEFAULT false,
    on_wing                     boolean          NOT NULL DEFAULT false,
    off_wing                    boolean          NOT NULL DEFAULT true,
    start_date                  date             NOT NULL,
    created_time                timestamp        NOT NULL,
    created_by                  varchar(100)     NOT NULL
);
CREATE INDEX idx_appointment_set_prison_code ON appointment_set (prison_code);
CREATE INDEX idx_appointment_set_category_code ON appointment_set (category_code);
CREATE INDEX idx_appointment_set_custom_name ON appointment_set (custom_name);
CREATE INDEX idx_appointment_set_appointment_tier_id ON appointment_set (appointment_tier_id);
CREATE INDEX idx_appointment_set_appointment_host_id ON appointment_set (appointment_host_id);
CREATE INDEX idx_appointment_set_internal_location_id ON appointment_set (internal_location_id);
CREATE INDEX idx_appointment_set_custom_location ON appointment_set (custom_location);
CREATE INDEX idx_appointment_set_start_date ON appointment_set (start_date);
CREATE INDEX idx_appointment_set_created_by ON appointment_set (created_by);

CREATE TABLE appointment_set_appointment_series
(
    appointment_set_appointment_series_id   bigserial NOT NULL CONSTRAINT appointment_set_appointment_series_pk PRIMARY KEY,
    appointment_set_id                      bigint    NOT NULL REFERENCES appointment_set (appointment_set_id),
    appointment_series_id                   bigint    NOT NULL REFERENCES appointment_series (appointment_series_id)
);
CREATE INDEX idx_appointment_set_id ON appointment_set_appointment_series (appointment_set_id);
CREATE INDEX idx_appointment_set_appointment_series_id ON appointment_set_appointment_series (appointment_series_id);

-- =============================================
-- UPDATED VIEWS USING NEW TABLES AND COLUMNS
-- =============================================

CREATE OR REPLACE VIEW v_appointment_instance AS
SELECT aa.appointment_attendee_id                                                    AS appointment_instance_id,
       a.appointment_series_id,
       aa.appointment_id,
       aa.appointment_attendee_id,
       asrs.appointment_type,
       a.prison_code,
       aa.prisoner_number,
       aa.booking_id,
       a.category_code,
       a.custom_name,
       a.appointment_tier_id,
       a.appointment_host_id,
       CASE
           WHEN a.in_cell THEN null
           ELSE a.internal_location_id END                                          AS internal_location_id,
       a.custom_location,
       a.in_cell,
       a.on_wing,
       a.off_wing,
       a.start_date                                                                 AS appointment_date,
       a.start_time,
       a.end_time,
       a.unlock_notes,
       a.extra_information,
       a.created_time,
       a.created_by,
       a.updated_time,
       a.updated_by,
       CASE
           WHEN a.cancellation_reason_id IS NULL THEN false
           ELSE NOT acr.is_delete END                                               AS is_cancelled
FROM appointment_attendee aa
     JOIN appointment a on aa.appointment_id = a.appointment_id
     JOIN appointment_series asrs on asrs.appointment_series_id = a.appointment_series_id
     LEFT JOIN appointment_cancellation_reason acr on a.cancellation_reason_id = acr.appointment_cancellation_reason_id
WHERE aa.removed_time IS NULL AND NOT a.is_deleted;

CREATE OR REPLACE VIEW v_appointment_search AS
SELECT a.appointment_series_id,
       a.appointment_id,
       asrs.appointment_type,
       a.prison_code,
       a.category_code,
       a.custom_name,
       a.appointment_tier_id,
       a.appointment_host_id,
       CASE WHEN a.in_cell THEN null ELSE a.internal_location_id END                AS internal_location_id,
       a.custom_location,
       a.in_cell,
       a.on_wing,
       a.off_wing,
       a.start_date,
       a.start_time,
       a.end_time,
       asrs.appointment_series_schedule_id IS NOT NULL                              AS is_repeat,
       a.sequence_number,
       COALESCE(asch.number_of_appointments, 1)                                     AS max_sequence_number,
       a.unlock_notes,
       a.extra_information,
       a.created_by,
       a.updated_time IS NOT NULL                                                   AS is_edited,
       CASE
           WHEN a.cancellation_reason_id IS NULL THEN false
           ELSE NOT acr.is_delete END                                               AS is_cancelled
FROM appointment a
         JOIN appointment_series asrs on asrs.appointment_series_id = a.appointment_series_id
         LEFT JOIN appointment_series_schedule asch on asrs.appointment_series_schedule_id = asch.appointment_series_schedule_id
         LEFT JOIN appointment_cancellation_reason acr on a.cancellation_reason_id = acr.appointment_cancellation_reason_id
WHERE NOT a.is_deleted;

-- =============================================
-- MIGRATE EXISTING DATA
-- =============================================

INSERT INTO appointment_series_schedule
SELECT
    appointment_schedule_id,
    repeat_period,
    repeat_count
FROM appointment_schedule;

INSERT INTO appointment_series (
    appointment_series_id,
    appointment_type,
    prison_code,
    category_code,
    custom_name,
    appointment_tier_id,
    internal_location_id,
    in_cell,
    start_date,
    start_time,
    end_time,
    appointment_series_schedule_id,
    extra_information,
    created_time,
    created_by,
    updated_time,
    updated_by,
    is_migrated
)
SELECT
    appointment_id,
    appointment_type,
    prison_code,
    category_code,
    appointment_description,
    3,
    internal_location_id,
    in_cell,
    start_date,
    start_time,
    end_time,
    appointment_schedule_id,
    comment,
    created,
    created_by,
    updated,
    updated_by,
    is_migrated
FROM appointment_old;

INSERT INTO appointment (
    appointment_id,
    appointment_series_id,
    sequence_number,
    prison_code,
    category_code,
    custom_name,
    appointment_tier_id,
    internal_location_id,
    in_cell,
    start_date,
    start_time,
    end_time,
    extra_information,
    created_time,
    created_by,
    updated_time,
    updated_by,
    cancelled_time,
    cancellation_reason_id,
    cancelled_by,
    is_deleted
)
SELECT
    appointment_occurrence_id,
    appointment_id,
    sequence_number,
    (SELECT prison_code FROM appointment_series WHERE appointment_series_id = appointment_id),
    category_code,
    appointment_description,
    3,
    internal_location_id,
    in_cell,
    start_date,
    start_time,
    end_time,
    comment,
    (SELECT created_time FROM appointment_series WHERE appointment_series_id = appointment_id),
    (SELECT created_by FROM appointment_series WHERE appointment_series_id = appointment_id),
    updated,
    updated_by,
    cancelled,
    cancellation_reason_id,
    cancelled_by,
    deleted
FROM appointment_occurrence;

INSERT INTO appointment_attendee (
    appointment_attendee_id,
    appointment_id,
    prisoner_number,
    booking_id
)
SELECT
    appointment_occurrence_allocation_id,
    appointment_occurrence_id,
    prisoner_number,
    booking_id
FROM appointment_occurrence_allocation;

INSERT INTO appointment_set
(
    appointment_set_id,
    prison_code,
    category_code,
    custom_name,
    appointment_tier_id,
    internal_location_id,
    in_cell,
    start_date,
    created_time,
    created_by
)
SELECT
    bulk_appointment_id,
    prison_code,
    category_code,
    appointment_description,
    3,
    internal_location_id,
    in_cell,
    start_date,
    created,
    created_by
FROM bulk_appointment;

INSERT INTO appointment_set_appointment_series
(
    appointment_set_appointment_series_id,
    appointment_set_id,
    appointment_series_id
)
SELECT
    bulk_appointment_appointment_id,
    bulk_appointment_id,
    appointment_id
FROM bulk_appointment_appointment;

-- =============================================
-- DELETE OLD TABLES AND VIEWS
-- =============================================

DROP VIEW v_appointment_occurrence_search;
DROP TABLE IF EXISTS bulk_appointment_appointment;
DROP TABLE IF EXISTS bulk_appointment;
DROP TABLE IF EXISTS appointment_occurrence_allocation;
DROP TABLE IF EXISTS appointment_occurrence;
DROP TABLE IF EXISTS appointment_old;
DROP TABLE IF EXISTS appointment_schedule;
