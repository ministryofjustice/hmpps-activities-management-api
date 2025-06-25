CREATE TABLE activity_pay_history
(
    activity_pay_history_id     bigserial      NOT NULL CONSTRAINT activity_pay_history_pk PRIMARY KEY,
    activity_id                 bigint         NOT NULL REFERENCES activity (activity_id),
    incentive_nomis_code        varchar(3)     NOT NULL,
    incentive_level             varchar(50)    NOT NULL,
    prison_pay_band_id          bigint         NOT NULL references prison_pay_band (prison_pay_band_id),
    rate                        integer,
    start_date                  date,
    changed_details             varchar(500)   NOT NULL,
    changed_time                timestamp      NOT NULL,
    changed_by                  varchar(255)   NOT NULL
);

CREATE INDEX idx_activity_pay_history_activity_id ON activity_pay (activity_id);
