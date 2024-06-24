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
