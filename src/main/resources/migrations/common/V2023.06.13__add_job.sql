CREATE TABLE job (
  job_id     bigserial    NOT NULL CONSTRAINT job_pk PRIMARY KEY,
  job_type   varchar(100) NOT NULL,
  started_at timestamp    NOT NULL,
  successful boolean      NOT NULL,
  ended_at   timestamp
);

CREATE INDEX idx_job_type ON job (job_type);
CREATE INDEX idx_started_at ON job (started_at);
