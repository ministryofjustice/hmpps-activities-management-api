CREATE TABLE waiting_list (
  waiting_list_id      bigserial    NOT NULL CONSTRAINT waiting_list_pk PRIMARY KEY,
  prison_code          varchar(3)   NOT NULL,
  prisoner_number      varchar(7)   NOT NULL,
  booking_id           bigint       NOT NULL,
  application_date     date         NOT NULL,
  activity_id          bigint       NOT NULL,
  activity_schedule_id bigint       NOT NULL,
  requested_by         varchar(100) NOT NULL,
  status               varchar(20)  NOT NULL,
  creation_time        timestamp    NOT NULL,
  created_by           varchar(20)  NOT NULL,
  comments             varchar(500),
  declined_reason      varchar(100),
  updated_time         timestamp,
  updated_by           varchar(100),
  allocation_id        bigint
);

CREATE INDEX idx_waiting_list_prison_code ON waiting_list (prison_code);
CREATE INDEX idx_waiting_list_prisoner_number ON waiting_list (prisoner_number);
CREATE INDEX idx_waiting_list_application_date ON waiting_list (application_date);
CREATE INDEX idx_waiting_list_status ON waiting_list (status);
CREATE INDEX idx_waiting_list_allocation_id ON waiting_list (allocation_id);
