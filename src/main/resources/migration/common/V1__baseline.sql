CREATE TABLE rollout_prison (
  rollout_prison_id serial      NOT NULL CONSTRAINT rollout_prison_pk PRIMARY KEY,
  code              varchar(5)  NOT NULL UNIQUE,
  description       varchar(60) NOT NULL,
  active            boolean     NOT NULL DEFAULT false
);

CREATE TABLE attendance_reason (
  attendance_reason_id serial      NOT NULL CONSTRAINT attendance_reason_pk PRIMARY KEY,
  code                 varchar(5)  NOT NULL UNIQUE,
  description          varchar(60) NOT NULL
);

CREATE TABLE eligibility_rule (
  eligibility_rule_id serial      NOT NULL CONSTRAINT eligibility_rule_pk PRIMARY KEY,
  code                varchar(5)  NOT NULL UNIQUE,
  description         varchar(60) NOT NULL
);

CREATE TABLE event_consumed (
  event_id        serial       NOT NULL CONSTRAINT events_consumed_pk PRIMARY KEY,
  event_type      varchar(60)  NOT NULL,
  event_time      timestamp    NOT NULL,
  prison_code     varchar(3)   NOT NULL,
  booking_id      integer      NOT NULL,
  prisoner_number varchar(7)   NOT NULL,
  event_data      varchar(200) NOT NULL
);

CREATE INDEX idx_event_consumed_prisoner_number ON event_consumed (prisoner_number);

CREATE TABLE prisoner_history (
  prisoner_history_id serial       NOT NULL CONSTRAINT prisoner_history_pk PRIMARY KEY,
  history_type        varchar(60)  NOT NULL,
  prison_code         varchar(3)   NOT NULL,
  prisoner_number     varchar(7)   NOT NULL,
  event_description   varchar(200) NOT NULL,
  event_time          timestamp    NOT NULL,
  event_by            varchar(100)
);

CREATE INDEX idx_prisoner_history_prisoner_number ON prisoner_history (prisoner_number);

CREATE TABLE daily_statistics (
  daily_statistics_id         serial     NOT NULL CONSTRAINT daily_statistics_pk PRIMARY KEY,
  statistics_date             date       NOT NULL UNIQUE,
  prison_code                 varchar(3) NOT NULL,
  unemployed                  integer,
  long_term_sick              integer,
  short_term_sick             integer,
  activities_with_allocations integer,
  sessions_cancelled          integer,
  sessions_run_today          integer,
  attendance_expected         integer,
  attendance_received         integer,
  people_in_work              integer,
  people_in_education         integer,
  vacancies                   integer
);

CREATE TABLE activity_category (
  activity_category_id serial       NOT NULL CONSTRAINT activity_category_pk PRIMARY KEY,
  category_code        varchar(30)  NOT NULL UNIQUE,
  description          varchar(100) NOT NULL
);

CREATE TABLE activity_tier (
  activity_tier integer CONSTRAINT activity_tier_pk PRIMARY KEY,
  description   varchar(100) NOT NULL
);

CREATE TABLE activity (
  activity_id          serial       NOT NULL CONSTRAINT activity_pk PRIMARY KEY,
  prison_code          varchar(3)   NOT NULL,
  activity_category_id integer      NOT NULL REFERENCES activity_category (activity_category_id),
  activity_tier        integer      NOT NULL REFERENCES activity_tier (activity_tier),
  summary              varchar(50)  NOT NULL,
  description          varchar(300) NOT NULL,
  start_date           date         NOT NULL,
  end_date             date,
  active               boolean      NOT NULL DEFAULT false,
  created_at           timestamp    NOT NULL,
  created_by           varchar(100) NOT NULL
);

CREATE INDEX idx_activity_start_date ON activity (start_date);
CREATE INDEX idx_activity_end_date ON activity (end_date);

CREATE TABLE activity_eligibility (
  activity_eligibility_id serial     NOT NULL CONSTRAINT activity_eligibility_pk PRIMARY KEY,
  eligibility_rule_id     integer    NOT NULL REFERENCES eligibility_rule (eligibility_rule_id),
  activity_id             integer    NOT NULL REFERENCES activity (activity_id)
);

CREATE TABLE activity_session (
  activity_session_id           serial       NOT NULL CONSTRAINT activity_session_pk PRIMARY KEY,
  activity_id                   integer      NOT NULL REFERENCES activity (activity_id),
  description                   varchar(50)  NOT NULL,
  suspend_until                 date,
  start_time                    timestamp    NOT NULL,
  end_time                      timestamp,
  internal_location_id          integer,
  internal_location_code        varchar(40),
  internal_location_description varchar(100),
  capacity                      integer      NOT NULL,
  days_of_week                  character(7) NOT NULL
);

CREATE TABLE activity_instance (
  activity_instance_id serial     NOT NULL CONSTRAINT activity_instance_pk PRIMARY KEY,
  prison_code          varchar(3) NOT NULL,
  activity_session_id  integer    NOT NULL REFERENCES activity_session (activity_session_id),
  session_date         date       NOT NULL,
  start_time           timestamp  NOT NULL,
  end_time             timestamp,
  internal_location_id integer,
  cancelled            boolean    NOT NULL,
  cancelled_at         timestamp,
  cancelled_by         varchar(100)
);

CREATE TABLE attendance (
  attendance_id        serial     NOT NULL CONSTRAINT attendance_pk PRIMARY KEY,
  activity_instance_id integer    NOT NULL REFERENCES activity_instance (activity_instance_id),
  prison_code          varchar(3) NOT NULL,
  prisoner_number      varchar(7) NOT NULL,
  attendance_reason_id integer REFERENCES attendance_reason (attendance_reason_id),
  comment              varchar(200),
  posted               boolean,
  recorded_at          timestamp,
  recorded_by          varchar(100),
  status               varchar(20),
  pay_amount           integer,
  bonus_amount         integer,
  pieces               integer
);

CREATE INDEX idx_attendance_prisoner_number ON attendance (prisoner_number);

CREATE TABLE activity_waitlist (
  activity_waiting_list_id serial       NOT NULL CONSTRAINT activity_waiting_list_pk PRIMARY KEY,
  prison_code              varchar(3)   NOT NULL,
  prisoner_number          varchar(7)   NOT NULL,
  activity_session_id      integer REFERENCES activity_session (activity_session_id),
  priority                 integer      NOT NULL,
  created_at               timestamp    NOT NULL,
  created_by               varchar(100) NOT NULL
);

CREATE INDEX idx_activity_waitlist_prisoner_number ON activity_waitlist (prisoner_number);

CREATE TABLE activity_prisoner (
  activity_prisoner_id serial       NOT NULL CONSTRAINT activity_prisoner_pk PRIMARY KEY,
  prisoner_number      varchar(7)   NOT NULL,
  activity_session_id  integer      NOT NULL REFERENCES activity_session (activity_session_id),
  iep_level            varchar(3),
  pay_band             varchar(1),
  start_date           timestamp    NOT NULL,
  end_date             timestamp,
  active               boolean      NOT NULL,
  allocation_at        timestamp    NOT NULL,
  allocated_by         varchar(100) NOT NULL,
  deallocated_at       timestamp,
  deallocated_by       varchar(100),
  deallocation_reason  varchar(100)
);

CREATE INDEX idx_activity_prisoner_prisoner_number ON activity_prisoner (prisoner_number);

CREATE TABLE activity_pay (
  activity_pay_id   serial     NOT NULL CONSTRAINT activity_pay_pk PRIMARY KEY,
  activity_id       integer    NOT NULL REFERENCES activity (activity_id),
  prison_code       varchar(3) NOT NULL,
  iep_basic_rate    integer,
  iep_standard_rate integer,
  iep_enhanced_rate integer,
  piece_rate        integer,
  piece_rate_items  integer
);

CREATE TABLE activity_pay_band (
  activity_pay_band_id serial     NOT NULL CONSTRAINT activity_pay_band_pk PRIMARY KEY,
  activity_pay_id      integer    NOT NULL REFERENCES activity_pay (activity_pay_id),
  pay_band             varchar(1) NOT NULL,
  rate                 integer,
  piece_rate           integer,
  piece_rate_items     integer
);
