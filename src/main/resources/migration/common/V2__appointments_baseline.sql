CREATE TABLE appointment_category (
    appointment_category_id bigserial       NOT NULL CONSTRAINT appointment_category_pk PRIMARY KEY,
    parent_id               integer         REFERENCES appointment_category (appointment_category_id),
    code                    varchar(12)     NOT NULL,
    description             varchar(100)    NOT NULL,
    active                  boolean         NOT NULL DEFAULT true,
    display_order           integer
);

CREATE INDEX idx_appointment_category_code ON appointment_category (code);
CREATE INDEX idx_appointment_category_description ON appointment_category (description);
CREATE INDEX idx_appointment_category_active ON appointment_category (active);
CREATE INDEX idx_appointment_category_display_order ON appointment_category (display_order);

CREATE TABLE appointment (
    appointment_id          bigserial       NOT NULL CONSTRAINT appointment_pk PRIMARY KEY,
    appointment_category_id integer         NOT NULL REFERENCES appointment_category (appointment_category_id),
    prison_code             varchar(6)      NOT NULL,
    internal_location_id    integer,
    in_cell                 boolean         NOT NULL DEFAULT false,
    start_date              date            NOT NULL,
    start_time              time            NOT NULL,
    end_time                time,
    comment                 text            NOT NULL DEFAULT '',
    created                 timestamp       NOT NULL,
    created_by              varchar(100)    NOT NULL,
    updated                 timestamp,
    updated_by              varchar(100),
    deleted                 boolean         NOT NULL DEFAULT false
);

CREATE INDEX idx_appointment_prison_code ON appointment (prison_code);
CREATE INDEX idx_appointment_internal_location_id ON appointment (internal_location_id);
CREATE INDEX idx_appointment_in_cell ON appointment (in_cell);
CREATE INDEX idx_appointment_start_date ON appointment (start_date);
CREATE INDEX idx_appointment_start_time ON appointment (start_time);
CREATE INDEX idx_appointment_end_time ON appointment (end_time);
CREATE INDEX idx_appointment_deleted ON appointment (deleted);

CREATE TABLE appointment_schedule (
    appointment_schedule_id bigserial   NOT NULL CONSTRAINT appointment_schedule_id PRIMARY KEY,
    appointment_id          bigint      NOT NULL REFERENCES appointment (appointment_id),
    end_date                date        NOT NULL,
    monday_flag             bool        NOT NULL DEFAULT false,
    tuesday_flag            bool        NOT NULL DEFAULT false,
    wednesday_flag          bool        NOT NULL DEFAULT false,
    thursday_flag           bool        NOT NULL DEFAULT false,
    friday_flag             bool        NOT NULL DEFAULT false,
    saturday_flag           bool        NOT NULL DEFAULT false,
    sunday_flag             bool        NOT NULL DEFAULT false
);

CREATE INDEX idx_appointment_schedule_end_date ON appointment_schedule (end_date);

CREATE TABLE appointment_occurrence (
     appointment_occurrence_id  bigserial       NOT NULL CONSTRAINT appointment_pk PRIMARY KEY,
     internal_location_id       integer,
     in_cell                    boolean         NOT NULL DEFAULT false,
     start_date                 date            NOT NULL,
     start_time                 time            NOT NULL,
     end_time                   time,
     comment                    text            NOT NULL DEFAULT '',
     cancelled                  boolean         NOT NULL DEFAULT false,
     updated                    timestamp,
     updated_by                 varchar(100)
);

CREATE INDEX idx_appointment_occurrence_internal_location_id ON appointment_occurrence (internal_location_id);
CREATE INDEX idx_appointment_occurrence_in_cell ON appointment_occurrence (in_cell);
CREATE INDEX idx_appointment_occurrence_start_date ON appointment_occurrence (start_date);
CREATE INDEX idx_appointment_occurrence_start_time ON appointment_occurrence (start_time);
CREATE INDEX idx_appointment_occurrence_end_time ON appointment_occurrence (end_time);
CREATE INDEX idx_appointment_occurrence_cancelled ON appointment_occurrence (cancelled);

CREATE TABLE appointment_occurrence_allocation (
    appointment_occurrence_allocation_id    bigserial   NOT NULL CONSTRAINT appointment_allocation_pk PRIMARY KEY,
    appointment_occurrence_id               integer     NOT NULL REFERENCES appointment_occurrence (appointment_occurrence_id),
    prisoner_number                         varchar(10) NOT NULL,
    booking_id                              integer     NOT NULL
);

CREATE INDEX idx_appointment_occurrence_allocation_prisoner_number ON appointment_occurrence_allocation (prisoner_number);
CREATE INDEX idx_appointment_occurrence_allocation_booking_id ON appointment_occurrence_allocation (booking_id);

CREATE TABLE appointment_instance (
    appointment_instance_id     bigserial   NOT NULL CONSTRAINT appointment_instance_pk PRIMARY KEY,
    appointment_id              integer     NOT NULL REFERENCES appointment (appointment_id),
    appointment_category_id     integer     NOT NULL REFERENCES appointment_category (appointment_category_id),
    prison_code                 varchar(6)  NOT NULL,
    internal_location_id        integer,
    in_cell                     boolean     NOT NULL DEFAULT false,
    prisoner_number             varchar(10) NOT NULL,
    booking_id                  integer     NOT NULL,
    appointment_date            date        NOT NULL,
    start_time                  time        NOT NULL,
    end_time                    time,
    comment                     text,
    attended                    boolean,
    cancelled                   boolean
);

CREATE INDEX idx_appointment_instance_prison_code ON appointment_instance (prison_code);
CREATE INDEX idx_appointment_instance_internal_location_id ON appointment_instance (internal_location_id);
CREATE INDEX idx_appointment_instance_in_cell ON appointment_instance (in_cell);
CREATE INDEX idx_appointment_instance_prisoner_number ON appointment_instance (prisoner_number);
CREATE INDEX idx_appointment_instance_booking_id ON appointment_instance (booking_id);
CREATE INDEX idx_appointment_instance_appointment_date ON appointment_instance (appointment_date);
CREATE INDEX idx_appointment_instance_start_time ON appointment_instance (start_time);
CREATE INDEX idx_appointment_instance_end_time ON appointment_instance (end_time);
CREATE INDEX idx_appointment_instance_attended ON appointment_instance (attended);
CREATE INDEX idx_appointment_instance_cancelled ON appointment_instance (cancelled);
