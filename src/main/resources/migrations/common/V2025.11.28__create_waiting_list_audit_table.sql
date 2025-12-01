CREATE TABLE waiting_list_aud
(
    waiting_list_id                 bigint            NOT NULL,
    prison_code                     varchar(3),
    prisoner_number                 varchar(7),
    booking_id                      bigint,
    application_date                date,
    activity_schedule_id            bigint,
    requested_by                    varchar(20),
    status                          varchar(20),
    created_by                      varchar(20),
    comments                        varchar(500),
    declined_reason                 varchar(100),
    updated_time                    timestamp,
    updated_by                      varchar(100),
    status_updated_time             timestamp,
    rev                             bigint            NOT NULL REFERENCES revision (id),
    revtype                         smallint          NOT NULL,
    PRIMARY KEY (waiting_list_id, rev)
);

CREATE INDEX idx_waiting_list_aud_waiting_list_id ON waiting_list_aud (waiting_list_id);
CREATE INDEX idx_waiting_list_aud_activity_schedule_id ON waiting_list_aud (activity_schedule_id);
CREATE INDEX idx_waiting_list_aud_status ON waiting_list_aud (status);