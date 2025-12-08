CREATE TABLE waiting_list_aud
(
    waiting_list_id                 bigint            NOT NULL,
    application_date                date,
    requested_by                    varchar(20),
    comments                        varchar(500),
    status                          varchar(20),
    rev                             bigint            NOT NULL REFERENCES revision (id),
    revtype                         smallint          NOT NULL,
    PRIMARY KEY (waiting_list_id, rev)
);

CREATE INDEX idx_waiting_list_aud_waiting_list_id ON waiting_list_aud (waiting_list_id);
CREATE INDEX idx_waiting_list_aud_status ON waiting_list_aud (status);