-- =============================================
-- BASELINE TABLES
-- =============================================

CREATE TABLE rollout_prison
(
    rollout_prison_id             bigserial   NOT NULL CONSTRAINT rollout_prison_pk PRIMARY KEY,
    code                          varchar(5)  NOT NULL UNIQUE,
    description                   varchar(60) NOT NULL,
    activities_to_be_rolled_out   boolean     NOT NULL DEFAULT FALSE,
    activities_rollout_date       date,
    appointments_to_be_rolled_out boolean     NOT NULL DEFAULT FALSE,
    appointments_rollout_date     date
);

CREATE INDEX idx_rollout_prison_code ON rollout_prison (code);

CREATE TABLE attendance_reason
(
    attendance_reason_id            bigserial   NOT NULL CONSTRAINT attendance_reason_pk PRIMARY KEY,
    code                            varchar(20) NOT NULL UNIQUE,
    description                     varchar(60) NOT NULL,
    attended                        boolean     NOT NULL,
    capture_pay                     boolean     NOT NULL,
    capture_more_detail             boolean     NOT NULL,
    capture_case_note               boolean     NOT NULL,
    capture_incentive_level_warning boolean     NOT NULL,
    capture_other_text              boolean     NOT NULL,
    display_in_absence              boolean     NOT NULL,
    display_sequence                integer,
    notes                           varchar(200)
);

CREATE INDEX idx_attendance_reason_code ON attendance_reason (code);

CREATE TABLE eligibility_rule
(
    eligibility_rule_id bigserial   NOT NULL CONSTRAINT eligibility_rule_pk PRIMARY KEY,
    code                varchar(50) NOT NULL UNIQUE,
    description         varchar(60) NOT NULL
);

CREATE INDEX idx_eligibility_rule_code ON eligibility_rule (code);

CREATE TABLE event_review
(
    event_review_id    bigserial NOT NULL CONSTRAINT event_review_pk PRIMARY KEY,
    service_identifier varchar(200),
    event_type         varchar(100),
    event_time         timestamp,
    prison_code        varchar(5),
    prisoner_number    char(7),
    booking_id         integer,
    event_data         varchar(300),
    acknowledged_time  timestamp,
    acknowledged_by    char(40)
);

CREATE INDEX idx_event_review_event_type ON event_review (event_type);
CREATE INDEX idx_event_review_event_time ON event_review (event_time);
CREATE INDEX idx_event_review_prison_code ON event_review (prison_code);
CREATE INDEX idx_event_review_prisoner_number ON event_review (prisoner_number);
CREATE INDEX idx_event_review_acknowledged_time ON event_review (acknowledged_time);
CREATE INDEX idx_event_review_acknowledged_by ON event_review (acknowledged_by);

CREATE TABLE prisoner_history
(
    prisoner_history_id bigserial    NOT NULL CONSTRAINT prisoner_history_pk PRIMARY KEY,
    history_type        varchar(60)  NOT NULL,
    prison_code         varchar(3)   NOT NULL,
    prisoner_number     varchar(7)   NOT NULL,
    event_description   varchar(200) NOT NULL,
    event_time          timestamp    NOT NULL,
    event_by            varchar(100)
);

CREATE INDEX idx_prisoner_history_prisoner_number ON prisoner_history (prisoner_number);
CREATE INDEX idx_prisoner_history_prison_code ON prisoner_history (prison_code);
CREATE INDEX idx_prisoner_history_event_time ON prisoner_history (event_time);

CREATE TABLE daily_statistics
(
    daily_statistics_id         bigserial  NOT NULL CONSTRAINT daily_statistics_pk PRIMARY KEY,
    statistics_date             date       NOT NULL UNIQUE,
    prison_code                 varchar(3) NOT NULL,
    unemployed                  integer,
    long_term_sick              integer,
    short_term_sick             integer,
    activities_with_allocations integer,
    activities_cancelled        integer,
    activities_run_today        integer,
    attendance_expected         integer,
    attendance_received         integer,
    people_in_work              integer,
    people_in_education         integer,
    vacancies                   integer
);

CREATE INDEX idx_daily_statistics_date ON daily_statistics (statistics_date);
CREATE INDEX idx_daily_statistics_prison_code ON daily_statistics (prison_code);

CREATE TABLE activity_category
(
    activity_category_id bigserial    NOT NULL CONSTRAINT activity_category_pk PRIMARY KEY,
    code                 varchar(30)  NOT NULL UNIQUE,
    name                 varchar(100) NOT NULL,
    description          varchar(300)
);

CREATE TABLE activity_tier
(
    activity_tier_id bigserial    NOT NULL CONSTRAINT activity_tier_pk PRIMARY KEY,
    code             varchar(12),
    description      varchar(100) NOT NULL
);

CREATE TABLE activity
(
    activity_id                  bigserial    NOT NULL CONSTRAINT activity_pk PRIMARY KEY,
    prison_code                  varchar(3)   NOT NULL,
    activity_category_id         bigint       NOT NULL REFERENCES activity_category (activity_category_id),
    activity_tier_id             bigint REFERENCES activity_tier (activity_tier_id),
    attendance_required          bool         NOT NULL DEFAULT true,
    in_cell                      bool         NOT NULL DEFAULT false,
    piece_work                   bool         NOT NULL DEFAULT false,
    outside_work                 bool         NOT NULL DEFAULT false,
    pay_per_session              char(1)      NOT NULL DEFAULT 'H',
    summary                      varchar(50)  NOT NULL,
    description                  varchar(300),
    start_date                   date         NOT NULL,
    end_date                     date,
    risk_level                   varchar(10)  NOT NULL,
    minimum_incentive_nomis_code varchar(3)   NOT NULL,
    minimum_incentive_level      varchar(10)  NOT NULL,
    created_time                 timestamp    NOT NULL,
    created_by                   varchar(100) NOT NULL,
    UNIQUE (prison_code, summary)
);

CREATE INDEX idx_activity_prison_code ON activity (prison_code);
CREATE INDEX idx_activity_start_date ON activity (start_date);
CREATE INDEX idx_activity_end_date ON activity (end_date);
CREATE INDEX idx_activity_summary ON activity (summary);
CREATE INDEX idx_activity_category_id ON activity (activity_category_id);
CREATE INDEX idx_activity_tier_id ON activity (activity_tier_id);

CREATE TABLE activity_eligibility
(
    activity_eligibility_id bigserial NOT NULL CONSTRAINT activity_eligibility_pk PRIMARY KEY,
    eligibility_rule_id     bigint    NOT NULL REFERENCES eligibility_rule (eligibility_rule_id),
    activity_id             bigint    NOT NULL REFERENCES activity (activity_id)
);

CREATE INDEX idx_activity_eligibility_rule_id ON activity_eligibility (eligibility_rule_id);
CREATE INDEX idx_activity_eligibility_activity_id ON activity_eligibility (activity_id);

CREATE TABLE activity_schedule
(
    activity_schedule_id          bigserial   NOT NULL CONSTRAINT activity_schedule_id PRIMARY KEY,
    activity_id                   bigint      NOT NULL REFERENCES activity (activity_id),
    description                   varchar(50) NOT NULL,
    internal_location_id          integer,
    internal_location_code        varchar(40),
    internal_location_description varchar(100),
    capacity                      integer     NOT NULL,
    start_date                    date        NOT NULL,
    end_date                      date,
    runs_on_bank_holiday          bool        NOT NULL DEFAULT false
);

CREATE INDEX idx_activity_schedule_activity_id ON activity_schedule (activity_id);
CREATE INDEX idx_activity_schedule_internal_location_id ON activity_schedule (internal_location_id);
CREATE INDEX idx_activity_schedule_internal_location_code ON activity_schedule (internal_location_code);

CREATE TABLE activity_schedule_slot
(
    activity_schedule_slot_id bigserial NOT NULL CONSTRAINT activity_schedule_slot_id PRIMARY KEY,
    activity_schedule_id      bigint    NOT NULL REFERENCES activity_schedule (activity_schedule_id),
    start_time                time      NOT NULL,
    end_time                  time,
    monday_flag               bool      NOT NULL DEFAULT false,
    tuesday_flag              bool      NOT NULL DEFAULT false,
    wednesday_flag            bool      NOT NULL DEFAULT false,
    thursday_flag             bool      NOT NULL DEFAULT false,
    friday_flag               bool      NOT NULL DEFAULT false,
    saturday_flag             bool      NOT NULL DEFAULT false,
    sunday_flag               bool      NOT NULL DEFAULT false
);

CREATE INDEX idx_act_sched_slot_activity_schedule_id ON activity_schedule_slot (activity_schedule_id);
CREATE INDEX idx_act_sched_slot_start_time ON activity_schedule_slot (start_time);
CREATE INDEX idx_act_sched_slot_end_time ON activity_schedule_slot (end_time);

CREATE TABLE activity_schedule_suspension
(
    activity_schedule_suspension_id bigserial NOT NULL CONSTRAINT activity_schedule_suspension_id PRIMARY KEY,
    activity_schedule_id            bigint    NOT NULL REFERENCES activity_schedule (activity_schedule_id),
    suspended_from                  date      NOT NULL,
    suspended_until                 date
);

CREATE INDEX idx_activity_schedule_suspension_schedule_id ON activity_schedule_suspension (activity_schedule_id);

CREATE TABLE scheduled_instance
(
    scheduled_instance_id bigserial NOT NULL CONSTRAINT scheduled_instance_pk PRIMARY KEY,
    activity_schedule_id  bigint    NOT NULL REFERENCES activity_schedule (activity_schedule_id),
    session_date          date      NOT NULL,
    start_time            time      NOT NULL,
    end_time              time,
    cancelled             boolean   NOT NULL DEFAULT false,
    cancelled_time        timestamp,
    cancelled_by          varchar(100),
    cancelled_reason      varchar(60),
    comment               varchar(250)
);

CREATE INDEX idx_scheduled_instance_schedule_id ON scheduled_instance (activity_schedule_id);
CREATE INDEX idx_scheduled_instance_session_date ON scheduled_instance (session_date);
CREATE INDEX idx_scheduled_instance_start_time ON scheduled_instance (start_time);
CREATE INDEX idx_scheduled_instance_end_time ON scheduled_instance (end_time);
CREATE UNIQUE INDEX idx_scheduled_instance_schedule_id_date_times ON scheduled_instance (activity_schedule_id, session_date, start_time, end_time);

CREATE TABLE attendance
(
    attendance_id                  bigserial  NOT NULL CONSTRAINT attendance_pk PRIMARY KEY,
    scheduled_instance_id          bigint     NOT NULL REFERENCES scheduled_instance (scheduled_instance_id),
    prisoner_number                varchar(7) NOT NULL,
    attendance_reason_id           bigint REFERENCES attendance_reason (attendance_reason_id),
    comment                        varchar(240),
    recorded_time                  timestamp,
    recorded_by                    varchar(100),
    status                         varchar(20), -- WAITING, COMPLETED, LOCKED
    pay_amount                     integer,
    bonus_amount                   integer,
    pieces                         integer,
    issue_payment                  bool,
    case_note_id                   bigint,
    incentive_level_warning_issued bool,
    other_absence_reason           varchar(240)
);

CREATE INDEX idx_attendance_scheduled_instance_id ON attendance (scheduled_instance_id);
CREATE INDEX idx_attendance_prisoner_number ON attendance (prisoner_number);
CREATE INDEX idx_attendance_recorded_time ON attendance (recorded_time);
CREATE UNIQUE INDEX idx_attendance_scheduled_instance_id_prison_number ON attendance (scheduled_instance_id, prisoner_number);

CREATE TABLE prisoner_waiting
(
    prisoner_waiting_id bigserial    NOT NULL CONSTRAINT prisoner_waiting_pk PRIMARY KEY,
    activity_id         bigint REFERENCES activity (activity_id),
    prisoner_number     varchar(7)   NOT NULL,
    priority            integer      NOT NULL,
    created_time        timestamp    NOT NULL,
    created_by          varchar(100) NOT NULL
);

CREATE INDEX idx_prisoner_waiting_activity_id ON prisoner_waiting (activity_id);
CREATE INDEX idx_prisoner_waiting_prisoner_number ON prisoner_waiting (prisoner_number);
CREATE INDEX idx_prisoner_waiting_created_time ON prisoner_waiting (created_time);

create table prison_pay_band
(
    prison_pay_band_id   bigserial    NOT NULL CONSTRAINT prison_pay_band_pk PRIMARY KEY,
    display_sequence     integer      NOT NULL,
    nomis_pay_band       integer      NOT NULL,
    pay_band_alias       varchar(30)  NOT NULL,
    pay_band_description varchar(100) NOT NULL,
    prison_code          varchar(10)  NOT NULL
);

CREATE UNIQUE INDEX idx_prison_pay_band_prison_code_nomis_pay_band ON prison_pay_band (prison_code, nomis_pay_band);
CREATE UNIQUE INDEX idx_prison_pay_band_prison_code_display_sequence ON prison_pay_band (prison_code, display_sequence);
CREATE INDEX idx_prison_pay_band_prison_code ON prison_pay_band (prison_code);
CREATE INDEX idx_prison_pay_band_pay_band_alias ON prison_pay_band (pay_band_alias);

CREATE TABLE allocation
(
    allocation_id        bigserial    NOT NULL CONSTRAINT allocation_pk PRIMARY KEY,
    activity_schedule_id bigint       NOT NULL REFERENCES activity_schedule (activity_schedule_id),
    prisoner_number      varchar(7)   NOT NULL,
    booking_id           bigint,
    prison_pay_band_id   bigint       NOT NULL references prison_pay_band (prison_pay_band_id),
    start_date           date         NOT NULL,
    end_date             date,
    allocated_time       timestamp    NOT NULL,
    allocated_by         varchar(100) NOT NULL,
    deallocated_time     timestamp,
    deallocated_by       varchar(100),
    deallocated_reason   varchar(100),
    suspended_time       timestamp,
    suspended_by         varchar(100),
    suspended_reason     varchar(100),
    prisoner_status      varchar(30)  NOT NULL
);

CREATE INDEX idx_allocation_activity_schedule_id ON allocation (activity_schedule_id);
CREATE INDEX idx_allocation_prisoner_number ON allocation (prisoner_number);
CREATE INDEX idx_allocation_booking_id ON allocation (booking_id);
CREATE INDEX idx_allocation_start_date ON allocation (start_date);
CREATE INDEX idx_allocation_end_date ON allocation (end_date);
CREATE INDEX idx_allocation_prisoner_status ON allocation (prisoner_status);

CREATE TABLE activity_pay
(
    activity_pay_id      bigserial   NOT NULL CONSTRAINT activity_pay_pk PRIMARY KEY,
    activity_id          bigint      NOT NULL REFERENCES activity (activity_id),
    incentive_nomis_code varchar(3)  NOT NULL,
    incentive_level      varchar(50) NOT NULL,
    prison_pay_band_id   bigint      NOT NULL references prison_pay_band (prison_pay_band_id),
    rate                 integer,
    piece_rate           integer,
    piece_rate_items     integer
);

CREATE INDEX idx_activity_pay_activity_id ON activity_pay (activity_id);

CREATE TABLE event_priority
(
    event_priority_id bigserial   NOT NULL CONSTRAINT event_priority_pk PRIMARY KEY,
    prison_code       varchar(3)  NOT NULL,
    event_type        varchar(30) NOT NULL,
    event_category    varchar(40),
    priority          integer     NOT NULL
);

CREATE INDEX idx_event_priority_prison_code ON event_priority (prison_code);
CREATE UNIQUE INDEX idx_event_priority_prison_code_event_type_event_category ON event_priority (prison_code, event_type, event_category);

-- View used to retrieve scheduled activities for a prisoner, or list of prisoners, between dates
-- This filters out the cancelled & suspended sessions, and checks that the activities and allocations are
-- within their start and end dates. Dated/expired records are not returned when using this view.
-- The repository PrisonScheduledActivityRepository and entity PrisonerScheduledActivity sit over this view.
CREATE OR REPLACE VIEW v_prisoner_scheduled_activities as
SELECT si.scheduled_instance_id,
       alloc.allocation_id,
       act.prison_code,
       si.session_date,
       si.start_time,
       si.end_time,
       alloc.prisoner_number,
       alloc.booking_id,
       schedule.internal_location_id,
       schedule.internal_location_code,
       schedule.internal_location_description,
       schedule.description as schedule_description,
       act.activity_id,
       category.code        as activity_category,
       act.summary          as activity_summary,
       si.cancelled,
       CASE
           WHEN suspensions.activity_schedule_suspension_id IS null THEN
               false
           ELSE
               true
           END
                            as suspended
FROM scheduled_instance si
         JOIN activity_schedule schedule
              ON schedule.activity_schedule_id = si.activity_schedule_id
                  AND si.session_date >= schedule.start_date
                  AND (schedule.end_date is null OR schedule.end_date <= si.session_date)
         JOIN allocation alloc
              ON alloc.activity_schedule_id = si.activity_schedule_id
                  AND si.session_date >= alloc.start_date
                  AND (alloc.end_date is null or alloc.end_date <= si.session_date)
         JOIN activity act
              ON act.activity_id = schedule.activity_id
                  AND (act.end_date is null OR act.end_date <= si.session_date)
         JOIN activity_category category on category.activity_category_id = act.activity_category_id
         LEFT JOIN activity_schedule_suspension suspensions
                   ON suspensions.activity_schedule_id = schedule.activity_schedule_id
                       AND si.session_date >= suspensions.suspended_from
                       AND (suspensions.suspended_until is null OR
                            suspensions.suspended_until >= si.session_date);

CREATE TABLE prison_regime
(
    prison_regime_id bigserial  NOT NULL CONSTRAINT prison_regime_pk PRIMARY KEY,
    prison_code      varchar(3) NOT NULL,
    am_start         time       NULL,
    am_finish        time       NULL,
    pm_start         time       NULL,
    pm_finish        time       NULL,
    ed_start         time       NULL,
    ed_finish        time       NULL
);

CREATE UNIQUE INDEX idx_prison_regime_prison_code ON prison_regime (prison_code);

CREATE TABLE activity_minimum_education_level
(
    activity_minimum_education_level_id bigserial   NOT NULL CONSTRAINT activity_minimum_education_level_pk PRIMARY KEY,
    activity_id                         bigint      NOT NULL REFERENCES activity (activity_id),
    education_level_code                varchar(10) NOT NULL,
    education_level_description         varchar(60) NOT NULL
);

CREATE INDEX idx_activity_minimum_education_level_activity_id ON activity_minimum_education_level (activity_id);

CREATE UNIQUE INDEX idx_activity_minimum_edu_level_activity_edu_code ON activity_minimum_education_level (activity_id, education_level_code);

CREATE TABLE attendance_history
(
    attendance_history_id          bigserial    NOT NULL CONSTRAINT attendance_history_pk PRIMARY KEY,
    attendance_id                  bigint       NOT NULL REFERENCES attendance (attendance_id),
    attendance_reason_id           bigint REFERENCES attendance_reason (attendance_reason_id),
    comment                        varchar(240),
    recorded_time                  timestamp    NOT NULL,
    recorded_by                    varchar(100) NOT NULL,
    issue_payment                  bool,
    case_note_id                   bigint,
    incentive_level_warning_issued bool,
    other_absence_reason           varchar(240)
);

CREATE INDEX idx_attendance_history_attendance_id ON attendance_history (attendance_id);

CREATE TABLE local_audit
(
    local_audit_id       bigserial    NOT NULL CONSTRAINT local_audit_pk PRIMARY KEY,
    username             VARCHAR(100) NOT NULL,
    audit_type           CHAR(10)     NOT NULL,
    detail_type          CHAR(20)     NOT NULL,
    recorded_time        TIMESTAMP    NOT NULL,
    prison_code          VARCHAR(7)   NOT NULL,
    prisoner_number      CHAR(7),
    activity_id          INTEGER,
    activity_schedule_id INTEGER,
    message              VARCHAR(300)
);

CREATE TABLE appointment_schedule
(
    appointment_schedule_id bigserial   NOT NULL CONSTRAINT appointment_schedule_pk PRIMARY KEY,
    repeat_period           varchar(20) NOT NULL,
    repeat_count            integer     NOT NULL
);

CREATE INDEX idx_appointment_schedule_repeat_period ON appointment_schedule (repeat_period);

CREATE TABLE appointment
(
    appointment_id          bigserial    NOT NULL CONSTRAINT appointment_pk PRIMARY KEY,
    appointment_type        varchar(10)  NOT NULL,
    prison_code             varchar(6)   NOT NULL,
    category_code           varchar(12)  NOT NULL,
    appointment_description varchar(40),
    internal_location_id    bigint,
    in_cell                 boolean      NOT NULL DEFAULT false,
    start_date              date         NOT NULL,
    start_time              time         NOT NULL,
    end_time                time,
    appointment_schedule_id bigint REFERENCES appointment_schedule (appointment_schedule_id),
    comment                 text         NOT NULL DEFAULT '',
    created                 timestamp    NOT NULL,
    created_by              varchar(100) NOT NULL,
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

CREATE TABLE appointment_cancellation_reason
(
    appointment_cancellation_reason_id bigserial   NOT NULL CONSTRAINT appointment_cancellation_reason_pk PRIMARY KEY,
    description                        varchar(50) NOT NULL,
    is_delete                          boolean     NOT NULL
);

CREATE TABLE appointment_occurrence
(
    appointment_occurrence_id bigserial NOT NULL CONSTRAINT appointment_occurrence_pk PRIMARY KEY,
    appointment_id            bigint    NOT NULL REFERENCES appointment (appointment_id),
    sequence_number           integer   NOT NULL,
    internal_location_id      bigint,
    in_cell                   boolean   NOT NULL DEFAULT false,
    start_date                date      NOT NULL,
    start_time                time      NOT NULL,
    end_time                  time,
    comment                   text,
    cancelled                 timestamp,
    cancellation_reason_id    bigint REFERENCES appointment_cancellation_reason (appointment_cancellation_reason_id),
    cancelled_by              varchar(100),
    updated                   timestamp,
    updated_by                varchar(100),
    deleted                   boolean   NOT NULL DEFAULT false
);

CREATE INDEX idx_appointment_occurrence_appointment_id ON appointment_occurrence (appointment_id);
CREATE INDEX idx_appointment_occurrence_internal_location_id ON appointment_occurrence (internal_location_id);
CREATE INDEX idx_appointment_occurrence_start_date ON appointment_occurrence (start_date);
CREATE INDEX idx_appointment_occurrence_start_time ON appointment_occurrence (start_time);
CREATE INDEX idx_appointment_occurrence_end_time ON appointment_occurrence (end_time);
CREATE INDEX idx_appointment_occurrence_cancellation_reason_id ON appointment_occurrence (cancellation_reason_id);

CREATE TABLE appointment_occurrence_allocation
(
    appointment_occurrence_allocation_id bigserial   NOT NULL CONSTRAINT appointment_allocation_pk PRIMARY KEY,
    appointment_occurrence_id            bigint      NOT NULL REFERENCES appointment_occurrence (appointment_occurrence_id),
    prisoner_number                      varchar(10) NOT NULL,
    booking_id                           bigint      NOT NULL
);

CREATE INDEX idx_appointment_occurrence_allocation_appointment_occurrence_id ON appointment_occurrence_allocation (appointment_occurrence_id);
CREATE INDEX idx_appointment_occurrence_allocation_prisoner_number ON appointment_occurrence_allocation (prisoner_number);
CREATE INDEX idx_appointment_occurrence_allocation_booking_id ON appointment_occurrence_allocation (booking_id);

CREATE TABLE bulk_appointment
(
    bulk_appointment_id bigserial NOT NULL
        CONSTRAINT bulk_appointment_pk PRIMARY KEY,
    created             timestamp,
    created_by          varchar(100)
);

CREATE TABLE bulk_appointment_appointment
(
    bulk_appointment_appointment_id bigserial NOT NULL
        CONSTRAINT bulk_appointment_appointment_pk PRIMARY KEY,
    bulk_appointment_id             bigint    NOT NULL REFERENCES bulk_appointment (bulk_appointment_id) ON DELETE CASCADE,
    appointment_id                  bigint    NOT NULL REFERENCES appointment (appointment_id) ON DELETE CASCADE
);

CREATE OR REPLACE VIEW v_appointment_instance AS
SELECT aoa.appointment_occurrence_allocation_id                                      AS appointment_instance_id,
       a.appointment_id,
       ao.appointment_occurrence_id,
       aoa.appointment_occurrence_allocation_id,
       a.appointment_type,
       a.prison_code,
       aoa.prisoner_number,
       aoa.booking_id,
       a.category_code,
       a.appointment_description,
       CASE
           WHEN ao.in_cell THEN null
           ELSE ao.internal_location_id END                                          AS internal_location_id,
       ao.in_cell,
       ao.start_date                                                                 AS appointment_date,
       ao.start_time,
       ao.end_time,
       COALESCE(ao.comment, a.comment)                                               AS comment,
       CASE
           WHEN ao.cancellation_reason_id IS NULL THEN false
           ELSE NOT is_delete END                                                    AS is_cancelled,
       a.created,
       a.created_by,
       ao.updated,
       ao.updated_by
FROM appointment_occurrence_allocation aoa
         JOIN appointment_occurrence ao
              on aoa.appointment_occurrence_id = ao.appointment_occurrence_id
         JOIN appointment a on a.appointment_id = ao.appointment_id
         LEFT JOIN appointment_cancellation_reason acr
                   on ao.cancellation_reason_id = acr.appointment_cancellation_reason_id;

CREATE OR REPLACE VIEW v_appointment_occurrence_search AS
SELECT ao.appointment_id,
       ao.appointment_occurrence_id,
       a.appointment_type,
       a.prison_code,
       a.category_code,
       a.appointment_description,
       CASE WHEN ao.in_cell THEN null ELSE ao.internal_location_id END               AS internal_location_id,
       ao.in_cell,
       ao.start_date,
       ao.start_time,
       ao.end_time,
       a.appointment_schedule_id IS NOT NULL                                         as is_repeat,
       ao.sequence_number,
       COALESCE(asch.repeat_count, 1)                                                as max_sequence_number,
       COALESCE(ao.comment, a.comment)                                               AS comment,
       a.created_by,
       ao.updated IS NULL                                                            as is_edited,
       CASE WHEN ao.cancellation_reason_id IS NULL THEN false ELSE NOT is_delete END AS is_cancelled
FROM appointment_occurrence ao
         JOIN appointment a on a.appointment_id = ao.appointment_id
         LEFT JOIN appointment_schedule asch
                   on a.appointment_schedule_id = asch.appointment_schedule_id
         LEFT JOIN appointment_cancellation_reason acr
                   on ao.cancellation_reason_id = acr.appointment_cancellation_reason_id
WHERE ao.deleted != true;

-- =============================================
-- ATTENDANCE SYNC VIEW
-- =============================================

CREATE OR REPLACE VIEW v_attendance_sync AS
select a.attendance_id,
       a.scheduled_instance_id,
       si.activity_schedule_id,
       si.session_date,
       si.start_time as session_start_time,
       si.end_time   as session_end_time,
       a.prisoner_number,
       a2.booking_id,
       ar.code       as attendance_reason_code,
       a.comment,
       a.status,
       a.pay_amount,
       a.bonus_amount,
       a.issue_payment
from attendance a
         join scheduled_instance si on a.scheduled_instance_id = si.scheduled_instance_id
         join allocation a2 on si.activity_schedule_id = a2.activity_schedule_id and
                               a.prisoner_number = a2.prisoner_number
         left join attendance_reason ar on a.attendance_reason_id = ar.attendance_reason_id;

-- =============================================
-- ATTENDANCE SUMMARY VIEWS
-- =============================================

CREATE OR REPLACE VIEW v_activity_time_slot AS
SELECT  scheduled_instance.scheduled_instance_id, 'AM' as time_slot, activity.prison_code, activity.activity_id, activity_category.name
FROM    activity, activity_schedule, prison_regime, scheduled_instance, activity_category
WHERE   activity.activity_id = activity_schedule.activity_id
  AND   activity.prison_code = prison_regime.prison_code
  AND   activity_schedule.activity_schedule_id = scheduled_instance.activity_schedule_id
  AND   scheduled_instance.start_time = prison_regime.am_start
  AND   activity.activity_category_id = activity_category.activity_category_id
UNION
SELECT  scheduled_instance.scheduled_instance_id, 'PM' as time_slot, activity.prison_code, activity.activity_id, activity_category.name
FROM    activity, activity_schedule, prison_regime, scheduled_instance, activity_category
WHERE   activity.activity_id = activity_schedule.activity_id
  AND   activity.prison_code = prison_regime.prison_code
  AND   activity_schedule.activity_schedule_id = scheduled_instance.activity_schedule_id
  AND   scheduled_instance.start_time = prison_regime.pm_start
  AND   activity.activity_category_id = activity_category.activity_category_id
UNION
SELECT  scheduled_instance.scheduled_instance_id, 'ED' as time_slot, activity.prison_code, activity.activity_id, activity_category.name
FROM    activity, activity_schedule, prison_regime, scheduled_instance, activity_category
WHERE   activity.activity_id = activity_schedule.activity_id
  AND   activity.prison_code = prison_regime.prison_code
  AND   activity_schedule.activity_schedule_id = scheduled_instance.activity_schedule_id
  AND   scheduled_instance.start_time = prison_regime.ed_start
  AND   activity.activity_category_id = activity_category.activity_category_id;

CREATE OR REPLACE VIEW v_all_attendance AS
SELECT a.attendance_id,
       si.session_date,
       ts.time_slot,
       a.status,
       ar.code as attendance_reason_code,
       a.issue_payment,
       ts.prison_code,
       ts.activity_id,
       ts.name as category_name
FROM scheduled_instance si
         INNER JOIN attendance a ON si.scheduled_instance_id = a.scheduled_instance_id
         INNER JOIN v_activity_time_slot ts ON si.scheduled_instance_id = ts.scheduled_instance_id
         LEFT JOIN attendance_reason ar on a.attendance_reason_id = ar.attendance_reason_id;

CREATE OR REPLACE VIEW v_all_attendance_summary AS
SELECT MIN(aa.attendance_id) as id,
       aa.prison_code,
       aa.activity_id,
       aa.category_name,
       aa.session_date,
       aa.time_slot,
       aa.status,
       aa.attendance_reason_code,
       aa.issue_payment,
       COUNT(aa.attendance_id) as attendance_count
FROM v_all_attendance aa
GROUP BY aa.prison_code,
         aa.activity_id,
         aa.category_name,
         aa.session_date,
         aa.time_slot,
         aa.status,
         aa.attendance_reason_code,
         aa.issue_payment;
