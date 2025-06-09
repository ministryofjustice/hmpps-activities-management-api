CREATE TABLE revision
(
    id                           bigserial    NOT NULL CONSTRAINT revision_pkey PRIMARY KEY,
    "timestamp"                  bigint       NOT NULL,
    username                     varchar(100) NOT NULL
);

CREATE SEQUENCE revision_seq INCREMENT BY 50;

CREATE TABLE activity_aud
(
    activity_id                  bigint    NOT NULL,
    prison_code                  varchar(3),
    summary                      varchar(50),
    description                  varchar(300),
    start_date                   date,
    end_date                     date,
    activity_category_id         bigint,
    activity_tier_id             bigint,
    in_cell                      bool         NOT NULL DEFAULT false,
    on_wing                      bool         NOT NULL DEFAULT false,
    off_wing                     bool         NOT NULL DEFAULT false,
    risk_level                   varchar(10)  NOT NULL,
    paid                         bool         NOT NULL,
    attendance_required          bool         NOT NULL DEFAULT true,
    rev                          bigint       NOT NULL REFERENCES revision (id),
    revtype                      smallint
);

CREATE INDEX idx_activity_aud_start_date ON activity_aud (start_date);
CREATE INDEX idx_activity_aud_end_date ON activity_aud (end_date);
CREATE INDEX idx_activity_aud_summary ON activity_aud (summary);
CREATE INDEX idx_activity_aud_category_id ON activity_aud (activity_category_id);
CREATE INDEX idx_activity_aud_tier_id ON activity_aud (activity_tier_id);
