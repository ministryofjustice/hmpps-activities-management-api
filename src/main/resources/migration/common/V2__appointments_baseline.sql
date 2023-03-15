CREATE TABLE appointment_category (
    appointment_category_id bigserial       NOT NULL CONSTRAINT appointment_category_pk PRIMARY KEY,
    parent_id               bigint          REFERENCES appointment_category (appointment_category_id),
    code                    varchar(12)     NOT NULL,
    description             varchar(100)    NOT NULL,
    active                  boolean         NOT NULL DEFAULT true,
    display_order           integer
);

CREATE INDEX idx_appointment_category_parent_id ON appointment_category (parent_id);
CREATE INDEX idx_appointment_category_code ON appointment_category (code);
CREATE INDEX idx_appointment_category_description ON appointment_category (description);
CREATE INDEX idx_appointment_category_display_order ON appointment_category (display_order);

CREATE TABLE appointment_schedule (
    appointment_schedule_id bigserial   NOT NULL CONSTRAINT appointment_schedule_pk PRIMARY KEY,
    repeat_period           varchar(20) NOT NULL,
    repeat_count            integer     NOT NULL
);

CREATE INDEX idx_appointment_schedule_repeat_period ON appointment_schedule (repeat_period);

CREATE TABLE appointment (
    appointment_id          bigserial       NOT NULL CONSTRAINT appointment_pk PRIMARY KEY,
    appointment_category_id bigint          NOT NULL REFERENCES appointment_category (appointment_category_id),
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

CREATE INDEX idx_appointment_appointment_category_id ON appointment (appointment_category_id);
CREATE INDEX idx_appointment_prison_code ON appointment (prison_code);
CREATE INDEX idx_appointment_internal_location_id ON appointment (internal_location_id);
CREATE INDEX idx_appointment_start_date ON appointment (start_date);
CREATE INDEX idx_appointment_start_time ON appointment (start_time);
CREATE INDEX idx_appointment_end_time ON appointment (end_time);
CREATE INDEX idx_appointment_schedule_id ON appointment (appointment_schedule_id);

CREATE TABLE appointment_occurrence (
     appointment_occurrence_id  bigserial       NOT NULL CONSTRAINT appointment_occurrence_pk PRIMARY KEY,
     appointment_id             bigint          NOT NULL REFERENCES appointment (appointment_id),
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

CREATE TABLE appointment_instance (
    appointment_instance_id     bigserial   NOT NULL CONSTRAINT appointment_instance_pk PRIMARY KEY,
    appointment_occurrence_id   bigint      NOT NULL REFERENCES appointment_occurrence (appointment_occurrence_id),
    appointment_category_id     bigint      NOT NULL REFERENCES appointment_category (appointment_category_id),
    prison_code                 varchar(6)  NOT NULL,
    internal_location_id        bigint,
    in_cell                     boolean     NOT NULL DEFAULT false,
    prisoner_number             varchar(10) NOT NULL,
    booking_id                  bigint      NOT NULL,
    appointment_date            date        NOT NULL,
    start_time                  time        NOT NULL,
    end_time                    time,
    comment                     text,
    attended                    boolean,
    cancelled                   boolean     NOT NULL DEFAULT false
);

CREATE INDEX idx_appointment_instance_appointment_occurrence_id ON appointment_instance (appointment_occurrence_id);
CREATE INDEX idx_appointment_instance_appointment_category_id ON appointment_instance (appointment_category_id);
CREATE INDEX idx_appointment_instance_prison_code ON appointment_instance (prison_code);
CREATE INDEX idx_appointment_instance_internal_location_id ON appointment_instance (internal_location_id);
CREATE INDEX idx_appointment_instance_prisoner_number ON appointment_instance (prisoner_number);
CREATE INDEX idx_appointment_instance_booking_id ON appointment_instance (booking_id);
CREATE INDEX idx_appointment_instance_appointment_date ON appointment_instance (appointment_date);
CREATE INDEX idx_appointment_instance_start_time ON appointment_instance (start_time);
CREATE INDEX idx_appointment_instance_end_time ON appointment_instance (end_time);
