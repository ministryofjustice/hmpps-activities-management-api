CREATE TABLE rollout_prison (
  rollout_prison_id bigserial   NOT NULL CONSTRAINT rollout_prison_pk PRIMARY KEY,
  code              varchar(5)  NOT NULL UNIQUE,
  description       varchar(60) NOT NULL,
  active            boolean     NOT NULL DEFAULT false,
  rollout_date      date        NOT NULL
);

CREATE INDEX idx_rollout_prison_code ON rollout_prison (code);

CREATE TABLE attendance_reason (
  attendance_reason_id bigserial   NOT NULL CONSTRAINT attendance_reason_pk PRIMARY KEY,
  code                 varchar(10) NOT NULL UNIQUE,
  description          varchar(60) NOT NULL
);

CREATE INDEX idx_attendance_reason_code ON attendance_reason (code);

CREATE TABLE eligibility_rule (
  eligibility_rule_id bigserial   NOT NULL CONSTRAINT eligibility_rule_pk PRIMARY KEY,
  code                varchar(50) NOT NULL UNIQUE,
  description         varchar(60) NOT NULL
);

CREATE INDEX idx_eligibility_rule_code ON eligibility_rule (code);

CREATE TABLE event_consumed (
  event_id        bigserial    NOT NULL CONSTRAINT events_consumed_pk PRIMARY KEY,
  event_type      varchar(60)  NOT NULL,
  event_time      timestamp    NOT NULL,
  prison_code     varchar(3)   NOT NULL,
  booking_id      integer      NOT NULL,
  prisoner_number varchar(7)   NOT NULL,
  event_data      varchar(200) NOT NULL
);

CREATE INDEX idx_event_consumed_prisoner_number ON event_consumed (prisoner_number);
CREATE INDEX idx_event_consumed_event_time ON event_consumed (event_time);
CREATE INDEX idx_event_consumed_event_type ON event_consumed (event_type);
CREATE INDEX idx_event_consumed_prison_code ON event_consumed (prison_code);

CREATE TABLE prisoner_history (
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

CREATE TABLE daily_statistics (
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

CREATE TABLE activity_category (
  activity_category_id bigserial    NOT NULL CONSTRAINT activity_category_pk PRIMARY KEY,
  code                 varchar(30)  NOT NULL UNIQUE,
  name                 varchar(100) NOT NULL,
  description          varchar(300)
);

CREATE TABLE activity_tier (
  activity_tier_id bigserial    NOT NULL CONSTRAINT activity_tier_pk PRIMARY KEY,
  code             varchar(12),
  description      varchar(100) NOT NULL
);

CREATE TABLE activity (
  activity_id          bigserial    NOT NULL CONSTRAINT activity_pk PRIMARY KEY,
  prison_code          varchar(3)   NOT NULL,
  activity_category_id bigint       NOT NULL REFERENCES activity_category (activity_category_id),
  activity_tier_id     bigint       REFERENCES activity_tier (activity_tier_id),
  attendance_required  bool         NOT NULL DEFAULT true,
  in_cell              bool         NOT NULL DEFAULT false,
  piece_work           bool         NOT NULL DEFAULT false,
  outside_work         bool         NOT NULL DEFAULT false,
  summary              varchar(50)  NOT NULL,
  description          varchar(300),
  start_date           date         NOT NULL,
  end_date             date,
  risk_level           varchar(10),
  minimum_incentive_level      varchar(10),
  created_time         timestamp    NOT NULL,
  created_by           varchar(100) NOT NULL,
  UNIQUE (prison_code, summary)
);

CREATE INDEX idx_activity_prison_code ON activity (prison_code);
CREATE INDEX idx_activity_start_date ON activity (start_date);
CREATE INDEX idx_activity_end_date ON activity (end_date);
CREATE INDEX idx_activity_summary ON activity (summary);
CREATE INDEX idx_activity_category_id ON activity (activity_category_id);
CREATE INDEX idx_activity_tier_id ON activity (activity_tier_id);

CREATE TABLE activity_eligibility (
  activity_eligibility_id bigserial NOT NULL CONSTRAINT activity_eligibility_pk PRIMARY KEY,
  eligibility_rule_id     bigint    NOT NULL REFERENCES eligibility_rule (eligibility_rule_id),
  activity_id             bigint    NOT NULL REFERENCES activity (activity_id)
);

CREATE INDEX idx_activity_eligibility_rule_id ON activity_eligibility (eligibility_rule_id);
CREATE INDEX idx_activity_eligibility_activity_id ON activity_eligibility (activity_id);

CREATE TABLE activity_schedule (
  activity_schedule_id          bigserial    NOT NULL CONSTRAINT activity_schedule_id PRIMARY KEY,
  activity_id                   bigint       NOT NULL REFERENCES activity (activity_id),
  description                   varchar(50)  NOT NULL,
  internal_location_id          integer,
  internal_location_code        varchar(40),
  internal_location_description varchar(100),
  capacity                      integer      NOT NULL
);

CREATE INDEX idx_activity_schedule_activity_id ON activity_schedule (activity_id);
CREATE INDEX idx_activity_schedule_internal_location_id ON activity_schedule (internal_location_id);
CREATE INDEX idx_activity_schedule_internal_location_code ON activity_schedule (internal_location_code);

CREATE TABLE activity_schedule_slot (
                                   activity_schedule_slot_id     bigserial    NOT NULL CONSTRAINT activity_schedule_slot_id PRIMARY KEY,
                                   activity_schedule_id          bigint       NOT NULL REFERENCES activity_schedule (activity_schedule_id),
                                   start_time                    time         NOT NULL,
                                   end_time                      time,
                                   monday_flag                   bool         NOT NULL DEFAULT false,
                                   tuesday_flag                  bool         NOT NULL DEFAULT false,
                                   wednesday_flag                bool         NOT NULL DEFAULT false,
                                   thursday_flag                 bool         NOT NULL DEFAULT false,
                                   friday_flag                   bool         NOT NULL DEFAULT false,
                                   saturday_flag                 bool         NOT NULL DEFAULT false,
                                   sunday_flag                   bool         NOT NULL DEFAULT false,
                                   runs_on_bank_holiday          bool         NOT NULL DEFAULT false
);

CREATE INDEX idx_act_sched_slot_activity_schedule_id ON activity_schedule_slot (activity_schedule_id);
CREATE INDEX idx_act_sched_slot_start_time ON activity_schedule_slot (start_time);
CREATE INDEX idx_act_sched_slot_end_time ON activity_schedule_slot (end_time);

CREATE TABLE activity_schedule_suspension (
    activity_schedule_suspension_id bigserial NOT NULL CONSTRAINT activity_schedule_suspension_id PRIMARY KEY,
    activity_schedule_id            bigint    NOT NULL REFERENCES activity_schedule (activity_schedule_id),
    suspended_from                  date      NOT NULL,
    suspended_until                 date
);

CREATE INDEX idx_activity_schedule_suspension_schedule_id ON activity_schedule_suspension (activity_schedule_id);

CREATE TABLE scheduled_instance (
  scheduled_instance_id bigserial NOT NULL CONSTRAINT scheduled_instance_pk PRIMARY KEY,
  activity_schedule_id  bigint    NOT NULL REFERENCES activity_schedule (activity_schedule_id),
  session_date          date      NOT NULL,
  start_time            time      NOT NULL,
  end_time              time,
  cancelled             boolean   NOT NULL DEFAULT false,
  cancelled_time        timestamp,
  cancelled_by          varchar(100)
);

CREATE INDEX idx_scheduled_instance_schedule_id ON scheduled_instance (activity_schedule_id);
CREATE INDEX idx_scheduled_instance_session_date ON scheduled_instance (session_date);
CREATE INDEX idx_scheduled_instance_start_time ON scheduled_instance (start_time);
CREATE INDEX idx_scheduled_instance_end_time ON scheduled_instance (end_time);

CREATE TABLE attendance (
  attendance_id         bigserial  NOT NULL CONSTRAINT attendance_pk PRIMARY KEY,
  scheduled_instance_id bigint     NOT NULL REFERENCES scheduled_instance (scheduled_instance_id),
  prisoner_number       varchar(7) NOT NULL,
  attendance_reason_id  bigint REFERENCES attendance_reason (attendance_reason_id),
  comment               varchar(200),
  posted                boolean,
  recorded_time         timestamp,
  recorded_by           varchar(100),
  status                varchar(20), -- SCH, CANC, COMP, PAID?
  pay_amount            integer,
  bonus_amount          integer,
  pieces                integer
);

CREATE INDEX idx_attendance_scheduled_instance_id ON attendance (scheduled_instance_id);
CREATE INDEX idx_attendance_prisoner_number ON attendance (prisoner_number);
CREATE INDEX idx_attendance_recorded_time ON attendance (recorded_time);
CREATE UNIQUE INDEX idx_attendance_scheduled_instance_id_prison_number ON attendance (scheduled_instance_id, prisoner_number);

CREATE TABLE prisoner_waiting (
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

CREATE TABLE allocation (
  allocation_id        bigserial    NOT NULL CONSTRAINT allocation_pk PRIMARY KEY,
  activity_schedule_id bigint       NOT NULL REFERENCES activity_schedule (activity_schedule_id),
  prisoner_number      varchar(7)   NOT NULL,
  pay_band             varchar(10),
  start_date           date         NOT NULL,
  end_date             date,
  allocated_time       timestamp    NOT NULL,
  allocated_by         varchar(100) NOT NULL,
  deallocated_time     timestamp,
  deallocated_by       varchar(100),
  deallocated_reason   varchar(100)
);

CREATE INDEX idx_allocation_activity_schedule_id ON allocation (activity_schedule_id);
CREATE INDEX idx_allocation_prisoner_number ON allocation (prisoner_number);
CREATE INDEX idx_allocation_start_date ON allocation (start_date);
CREATE INDEX idx_allocation_end_date ON allocation (end_date);

CREATE TABLE activity_pay (
  activity_pay_id   bigserial NOT NULL CONSTRAINT activity_pay_pk PRIMARY KEY,
  activity_id       bigint    NOT NULL REFERENCES activity (activity_id),
  incentive_level   varchar(10),
  pay_band          varchar(10),
  rate              integer,
  piece_rate        integer,
  piece_rate_items  integer
);

CREATE INDEX idx_activity_pay_activity_id ON activity_pay (activity_id);

CREATE TABLE event_priority (
  event_priority_id bigserial   NOT NULL CONSTRAINT event_priority_pk PRIMARY KEY,
  prison_code       varchar(3)  NOT NULL,
  event_type        varchar(30) NOT NULL,
  event_category    varchar(40),
  priority          integer     NOT NULL
);

CREATE INDEX idx_event_priority_prison_code ON event_priority (prison_code);
CREATE UNIQUE INDEX idx_event_priority_prison_code_event_type_event_category ON event_priority (prison_code, event_type, event_category);
