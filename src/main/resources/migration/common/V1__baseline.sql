CREATE TABLE rollout_prisons (
  rollout_prison_id serial      NOT NULL CONSTRAINT rollout_prison_pk PRIMARY KEY,
  code              varchar(5)  NOT NULL UNIQUE,
  description       varchar(60) NOT NULL,
  active            boolean     NOT NULL DEFAULT false
);

CREATE TABLE attendance_reasons (
  attendance_reason_id serial      NOT NULL CONSTRAINT attendance_reason_pk PRIMARY KEY,
  code                 varchar(5)  NOT NULL UNIQUE,
  description          varchar(60) NOT NULL
);

CREATE TABLE eligibility_rules (
  eligibility_rule_id serial      NOT NULL CONSTRAINT eligibility_rule_pk PRIMARY KEY,
  code                varchar(5)  NOT NULL UNIQUE,
  description         varchar(60) NOT NULL
);

CREATE TABLE events_consumed (
  event_id          serial       NOT NULL CONSTRAINT events_consumed_pk PRIMARY KEY,
  event_type        varchar(60)  NOT NULL,
  event_time        timestamp    NOT NULL,
  rollout_prison_id integer      NOT NULL REFERENCES rollout_prisons (rollout_prison_id),
  booking_id        integer      NOT NULL,
  prisoner_number   varchar(7)   NOT NULL,
  event_data        varchar(200) NOT NULL
);

CREATE TABLE prisoner_history (
  prisoner_history_id serial       NOT NULL CONSTRAINT prisoner_history_pk PRIMARY KEY,
  history_type        varchar(60)  NOT NULL,
  rollout_prison_id   integer      NOT NULL REFERENCES rollout_prisons (rollout_prison_id),
  prisoner_number     varchar(7)   NOT NULL,
  event_description   varchar(200) NOT NULL,
  event_time          timestamp    NOT NULL,
  event_by            varchar(100)
);

CREATE TABLE daily_statistics (
  daily_statistics_id         serial  NOT NULL CONSTRAINT daily_statistics_pk PRIMARY KEY,
  statistics_date             date,
  rollout_prison_id           integer NOT NULL REFERENCES rollout_prisons (rollout_prison_id),
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

CREATE TABLE activity_types (
  activity_type_id serial       NOT NULL CONSTRAINT activity_type_pk PRIMARY KEY,
  type_code        varchar(30)  NOT NULL UNIQUE,
  description      varchar(100) NOT NULL
);

CREATE TABLE activity_categories (
  activity_category_id serial       NOT NULL CONSTRAINT activity_category_pk PRIMARY KEY,
  category_code        varchar(30)  NOT NULL UNIQUE,
  description          varchar(100) NOT NULL
);

CREATE TABLE activity_tiers (
  activity_tier integer CONSTRAINT activity_tier_pk PRIMARY KEY,
  description   varchar(100) NOT NULL
);

CREATE TABLE activities (
  activity_id          serial       NOT NULL CONSTRAINT activity_pk PRIMARY KEY,
  rollout_prison_id    integer      NOT NULL REFERENCES rollout_prisons (rollout_prison_id),
  activity_type_id     integer      NOT NULL REFERENCES activity_types (activity_type_id),
  activity_category_id integer      NOT NULL REFERENCES activity_categories (activity_category_id),
  activity_tier        integer      NOT NULL REFERENCES activity_tiers (activity_tier),
  summary              varchar(50)  NOT NULL,
  description          varchar(300) NOT NULL,
  start_date           date         NOT NULL,
  end_date             date,
  active               boolean      NOT NULL DEFAULT false,
  created_at           timestamp    NOT NULL,
  created_by           varchar(100) NOT NULL
);

CREATE TABLE activities_eligibility (
  activity_eligibility_id serial  NOT NULL CONSTRAINT activity_eligibility_pk PRIMARY KEY,
  eligibility_rule_id     integer NOT NULL REFERENCES eligibility_rules (eligibility_rule_id),
  activity_id             integer NOT NULL REFERENCES activities (activity_id)
);

CREATE TABLE activity_schedules (
  activity_schedule_id serial       NOT NULL CONSTRAINT activity_schedule_pk PRIMARY KEY,
  activity_id          integer      NOT NULL REFERENCES activities (activity_id),
  description          varchar(50)  NOT NULL,
  suspend_until        date,
  start_time           timestamp    NOT NULL,
  end_time             timestamp,
  -- internal_location_id ???
  -- internal_location_description ???
  capacity             integer      NOT NULL,
  days_of_week         character(7) NOT NULL -- fixed length??
);

CREATE TABLE activity_sessions (
  activity_session_id serial    NOT NULL CONSTRAINT activity_session_pk PRIMARY KEY,
  rollout_prison_id   integer   NOT NULL REFERENCES rollout_prisons (rollout_prison_id),
  session_date        date      NOT NULL,
  start_time          timestamp NOT NULL,
  end_time            timestamp,
  -- internal_location_id ???
  cancelled           boolean   NOT NULL,
  cancelled_at        timestamp,
  cancelled_by        varchar(100)
);

CREATE TABLE attendances (
  attendance_id        serial     NOT NULL CONSTRAINT attendance_pk PRIMARY KEY,
  activity_session_id  integer    NOT NULL REFERENCES activity_sessions (activity_session_id),
  rollout_prison_id    integer    NOT NULL REFERENCES rollout_prisons (rollout_prison_id),
  prisoner_number      varchar(7) NOT NULL,
  attendance_reason_id integer REFERENCES attendance_reasons (attendance_reason_id),
  comment              varchar(200),
  posted               boolean,
  recorded_at          timestamp,
  recorded_by          varchar(100),
  status               varchar(20),
  pay_amount           integer, -- STORE CURRENCY AS WHOLE NUMBERS ??
  bonus_amount         integer, -- STORE CURRENCY AS WHOLE NUMBERS ??
  pieces               integer
);

CREATE TABLE activity_waiting_lists (
  activity_waiting_list_id serial       NOT NULL CONSTRAINT activity_waiting_list_pk PRIMARY KEY,
  rollout_prison_id        integer      NOT NULL REFERENCES rollout_prisons (rollout_prison_id),
  prisoner_number          varchar(7)   NOT NULL,
  activity_schedule_id     integer REFERENCES activity_schedules (activity_schedule_id),
  priority                 integer      NOT NULL,
  created_at               timestamp    NOT NULL,
  created_by               varchar(100) NOT NULL
);

CREATE TABLE activity_prisoners (
  activity_prisoner_id serial       NOT NULL CONSTRAINT activity_prisoner_pk PRIMARY KEY,
  prisoner_number      varchar(7)   NOT NULL,
  activity_schedule_id integer      NOT NULL REFERENCES activity_schedules (activity_schedule_id),
  iep_level            varchar(3), -- NOT NULL ??
  pay_band             varchar(1), -- NOT NULL ??
  start_date           timestamp    NOT NULL,
  end_date             timestamp,  -- NOT NULL ??
  active               boolean      NOT NULL,
  allocation_at        timestamp    NOT NULL,
  allocated_by         varchar(100) NOT NULL,
  deallocated_at       timestamp,
  deallocated_by       varchar(100),
  deallocation_reason  varchar(100)
);

CREATE TABLE activities_pay (
  activity_pay_id   serial  NOT NULL CONSTRAINT activity_pay_pk PRIMARY KEY,
  activity_id       integer NOT NULL REFERENCES activities (activity_id),
  rollout_prison_id integer NOT NULL REFERENCES rollout_prisons (rollout_prison_id),
  iep_basic_rate    integer, -- STORE CURRENCY AS WHOLE NUMBERS ??
  iep_standard_rate integer, -- STORE CURRENCY AS WHOLE NUMBERS ??
  iep_enhanced_rate integer, -- STORE CURRENCY AS WHOLE NUMBERS ??
  piece_rate        integer, -- STORE CURRENCY AS WHOLE NUMBERS ??
  piece_rate_items  integer
);

CREATE TABLE activity_pay_bands (
  activity_pay_band_id serial     NOT NULL CONSTRAINT activity_pay_band_pk PRIMARY KEY,
  pay_band             varchar(1) NOT NULL,
  rate                 integer, -- STORE CURRENCY AS WHOLE NUMBERS ??
  piece_rate           integer, -- STORE CURRENCY AS WHOLE NUMBERS ??
  piece_rate_items     integer
);
