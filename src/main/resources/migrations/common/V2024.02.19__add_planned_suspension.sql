-- ==================================================================
-- Create the new table
-- ==================================================================
CREATE TABLE planned_suspension (
  planned_suspension_id   bigserial    NOT NULL CONSTRAINT planned_suspension_pk PRIMARY KEY,
  allocation_id           bigint       NOT NULL REFERENCES allocation (allocation_id),
  planned_reason          varchar(100) NOT NULL,
  planned_start_date      date         NOT NULL,
  planned_end_date        date,
  planned_by              varchar(100) NOT NULL,
  planned_at              timestamp    NOT NULL,
  updated_by              varchar(100),
  updated_at              timestamp
);
