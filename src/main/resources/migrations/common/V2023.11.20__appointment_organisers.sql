DROP VIEW v_appointment_search;
DROP VIEW v_appointment_instance;

ALTER TABLE appointment DROP COLUMN appointment_tier_id;
ALTER TABLE appointment DROP COLUMN appointment_host_id;

ALTER TABLE appointment_set DROP COLUMN appointment_tier_id;
ALTER TABLE appointment_set DROP COLUMN appointment_host_id;

ALTER TABLE appointment_series DROP COLUMN appointment_tier_id;
ALTER TABLE appointment_series DROP COLUMN appointment_host_id;

DROP TABLE appointment_tier;
DROP TABLE appointment_host;

ALTER TABLE appointment ADD COLUMN appointment_tier_id bigint REFERENCES event_tier (event_tier_id);
ALTER TABLE appointment ADD COLUMN appointment_organiser_id bigint REFERENCES event_organiser (event_organiser_id);

CREATE INDEX idx_appointment_appointment_tier_id ON appointment (appointment_tier_id);
CREATE INDEX idx_appointment_appointment_organiser_id ON appointment (appointment_organiser_id);

ALTER TABLE appointment_set ADD COLUMN appointment_tier_id bigint REFERENCES event_tier (event_tier_id);
ALTER TABLE appointment_set ADD COLUMN appointment_organiser_id bigint REFERENCES event_organiser (event_organiser_id);

CREATE INDEX idx_appointment_set_tier_id ON appointment_set (appointment_tier_id);
CREATE INDEX idx_appointment_set_organiser_id ON appointment_set (appointment_organiser_id);

ALTER TABLE appointment_series ADD COLUMN appointment_tier_id bigint REFERENCES event_tier (event_tier_id);
ALTER TABLE appointment_series ADD COLUMN appointment_organiser_id bigint REFERENCES event_organiser (event_organiser_id);

CREATE INDEX idx_appointment_series_tier_id ON appointment_series (appointment_tier_id);
CREATE INDEX idx_appointment_series_organiser_id ON appointment_series (appointment_organiser_id);

ALTER INDEX IF EXISTS activity_tier_pk RENAME TO event_tier_pk;

CREATE OR REPLACE VIEW v_appointment_search AS
SELECT a.appointment_series_id,
       a.appointment_id,
       asrs.appointment_type,
       a.prison_code,
       a.category_code,
       a.custom_name,
       a.appointment_tier_id,
       a.appointment_organiser_id,
       CASE WHEN a.in_cell THEN null ELSE a.internal_location_id END                AS internal_location_id,
       a.custom_location,
       a.in_cell,
       a.on_wing,
       a.off_wing,
       a.start_date,
       a.start_time,
       a.end_time,
       asrs.appointment_series_schedule_id IS NOT NULL                              AS is_repeat,
       a.sequence_number,
       COALESCE(asch.number_of_appointments, 1)                                     AS max_sequence_number,
       a.unlock_notes,
       a.extra_information,
       a.created_by,
       a.updated_time IS NOT NULL                                                   AS is_edited,
       CASE
           WHEN a.cancellation_reason_id IS NULL THEN false
           ELSE NOT acr.is_delete END                                               AS is_cancelled
FROM appointment a
         JOIN appointment_series asrs on asrs.appointment_series_id = a.appointment_series_id
         LEFT JOIN appointment_series_schedule asch on asrs.appointment_series_schedule_id = asch.appointment_series_schedule_id
         LEFT JOIN appointment_cancellation_reason acr on a.cancellation_reason_id = acr.appointment_cancellation_reason_id
WHERE NOT a.is_deleted;


CREATE OR REPLACE VIEW v_appointment_instance AS
SELECT aa.appointment_attendee_id                                                    AS appointment_instance_id,
       a.appointment_series_id,
       aa.appointment_id,
       aa.appointment_attendee_id,
       asrs.appointment_type,
       a.prison_code,
       aa.prisoner_number,
       aa.booking_id,
       a.category_code,
       a.custom_name,
       a.appointment_tier_id,
       a.appointment_organiser_id,
       CASE
           WHEN a.in_cell THEN null
           ELSE a.internal_location_id END                                          AS internal_location_id,
       a.custom_location,
       a.in_cell,
       a.on_wing,
       a.off_wing,
       a.start_date                                                                 AS appointment_date,
       a.start_time,
       a.end_time,
       a.unlock_notes,
       a.extra_information,
       a.created_time,
       a.created_by,
       a.updated_time,
       a.updated_by,
       CASE
           WHEN a.cancellation_reason_id IS NULL THEN false
           ELSE NOT acr.is_delete END                                               AS is_cancelled,
       a.cancelled_time,
       a.cancelled_by,
       CASE
           WHEN aa.removal_reason_id IS NULL THEN false
           ELSE NOT aarr.is_delete END                                              AS is_removed,
       aa.removed_time,
       aa.removed_by
FROM appointment_attendee aa
         JOIN appointment a on aa.appointment_id = a.appointment_id
         JOIN appointment_series asrs on asrs.appointment_series_id = a.appointment_series_id
         LEFT JOIN appointment_cancellation_reason acr on a.cancellation_reason_id = acr.appointment_cancellation_reason_id
         LEFT JOIN appointment_attendee_removal_reason aarr on aa.removal_reason_id = aarr.appointment_attendee_removal_reason_id
WHERE NOT aa.is_deleted AND NOT a.is_deleted;