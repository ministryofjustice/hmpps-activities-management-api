CREATE TABLE waitinglist_aud
(
    waitingListId                  bigint            NOT NULL,
    prisonCode                     varchar(3),
    prisonerNumber                 varchar(7),
    bookingId                      bigint,
    applicationDate                date,
    activity_schedule_id           bigint,
    requestedBy                    varchar(20),
    comments                       varchar(500),
    createdBy                      varchar(20),
    status                         varchar(20),
    activity_id                    bigint,
    creationTime                   timestamp,
    declinedReason                 varchar(100),
    updatedTime                    timestamp,
    updatedBy                      varchar(100),
    statusUpdatedTime              timestamp,
    allocation_id                  bigint,
    rev                            bigint            NOT NULL REFERENCES revision (id),
    revtype                       smallint          NOT NULL,
    PRIMARY KEY (waitingListId, rev)
);

CREATE INDEX idx_waitinglist_aud_waitingListId ON waitinglist_aud (waitingListId);
CREATE INDEX idx_waitinglist_aud_activity_schedule_id ON waitinglist_aud (activity_schedule_id);
CREATE INDEX idx_waitinglist_aud_activity_id ON waitinglist_aud (activity_id);
CREATE INDEX idx_waitinglist_aud_allocation_id ON waitinglist_aud (allocation_id);
CREATE INDEX idx_waitinglist_aud_status ON waitinglist_aud (status);