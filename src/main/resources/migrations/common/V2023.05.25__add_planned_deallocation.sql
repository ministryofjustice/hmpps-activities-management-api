CREATE TABLE planned_deallocation (
  planned_deallocation_id bigserial    NOT NULL CONSTRAINT planned_deallocation_pk PRIMARY KEY,
  planned_date            date         NOT NULL,
  planned_by              varchar(100) NOT NULL,
  planned_reason          varchar(100) NOT NULL,
  planned_at              timestamp    NOT NULL,
  allocation_id           bigint       NOT NULL REFERENCES allocation (allocation_id)
);

ALTER TABLE allocation
  ADD COLUMN planned_deallocation_id bigint REFERENCES planned_deallocation (planned_deallocation_id);
