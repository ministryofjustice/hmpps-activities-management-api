ALTER TABLE appointment_occurrence ADD COLUMN category_code varchar(12);
ALTER TABLE appointment_occurrence ADD COLUMN appointment_description varchar(40);

UPDATE appointment_occurrence ao SET
    category_code = (SELECT category_code FROM appointment a WHERE appointment_id = ao.appointment_id),
    appointment_description = (SELECT appointment_description FROM appointment a WHERE appointment_id = ao.appointment_id);

ALTER TABLE appointment_occurrence ALTER COLUMN category_code SET NOT NULL;

CREATE INDEX idx_appointment_occurrence_category_code ON appointment_occurrence (category_code);
CREATE INDEX idx_appointment_occurrence_appointment_description ON appointment_occurrence (appointment_description);

CREATE OR REPLACE VIEW v_appointment_instance AS
SELECT aoa.appointment_occurrence_allocation_id                                      AS appointment_instance_id,
       a.appointment_id,
       ao.appointment_occurrence_id,
       aoa.appointment_occurrence_allocation_id,
       a.appointment_type,
       a.prison_code,
       aoa.prisoner_number,
       aoa.booking_id,
       ao.category_code,
       ao.appointment_description,
       CASE
           WHEN ao.in_cell THEN null
           ELSE ao.internal_location_id END                                          AS internal_location_id,
       ao.in_cell,
       ao.start_date                                                                 AS appointment_date,
       ao.start_time,
       ao.end_time,
       ao.comment,
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
                   on ao.cancellation_reason_id = acr.appointment_cancellation_reason_id
WHERE NOT ao.deleted;

CREATE OR REPLACE VIEW v_appointment_occurrence_search AS
SELECT ao.appointment_id,
       ao.appointment_occurrence_id,
       a.appointment_type,
       a.prison_code,
       ao.category_code,
       ao.appointment_description,
       CASE WHEN ao.in_cell THEN null ELSE ao.internal_location_id END               AS internal_location_id,
       ao.in_cell,
       ao.start_date,
       ao.start_time,
       ao.end_time,
       a.appointment_schedule_id IS NOT NULL                                         as is_repeat,
       ao.sequence_number,
       COALESCE(asch.repeat_count, 1)                                                as max_sequence_number,
       ao.comment,
       a.created_by,
       ao.updated IS NOT NULL                                                            as is_edited,
       CASE WHEN ao.cancellation_reason_id IS NULL THEN false ELSE NOT is_delete END AS is_cancelled
FROM appointment_occurrence ao
         JOIN appointment a on a.appointment_id = ao.appointment_id
         LEFT JOIN appointment_schedule asch
                   on a.appointment_schedule_id = asch.appointment_schedule_id
         LEFT JOIN appointment_cancellation_reason acr
                   on ao.cancellation_reason_id = acr.appointment_cancellation_reason_id
WHERE ao.deleted != true;
