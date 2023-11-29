CREATE TABLE exclusion
(
    exclusion_id              BIGSERIAL             NOT NULL CONSTRAINT exclusion_pk PRIMARY KEY,
    allocation_id             BIGSERIAL             NOT NULL REFERENCES allocation (allocation_id),
    activity_schedule_slot_id BIGSERIAL             NOT NULL REFERENCES activity_schedule_slot (activity_schedule_slot_id),
    monday_flag               BOOLEAN DEFAULT false NOT NULL,
    tuesday_flag              BOOLEAN DEFAULT false NOT NULL,
    wednesday_flag            BOOLEAN DEFAULT false NOT NULL,
    thursday_flag             BOOLEAN DEFAULT false NOT NULL,
    friday_flag               BOOLEAN DEFAULT false NOT NULL,
    saturday_flag             BOOLEAN DEFAULT false NOT NULL,
    sunday_flag               BOOLEAN DEFAULT false NOT NULL
);
