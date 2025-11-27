ALTER TABLE revision
ADD COLUMN revision_date_time timestamptz
    GENERATED ALWAYS AS (
        TO_TIMESTAMP("timestamp" / 1000.0) AT TIME ZONE 'Europe/London'
    ) STORED;