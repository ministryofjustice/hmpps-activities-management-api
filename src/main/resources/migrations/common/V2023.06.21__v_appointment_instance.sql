CREATE OR REPLACE VIEW v_appointment_instance AS
SELECT aoa.appointment_occurrence_allocation_id                                      AS appointment_instance_id,
       a.appointment_id,
       ao.appointment_occurrence_id,
       aoa.appointment_occurrence_allocation_id,
       a.appointment_type,
       a.prison_code,
       aoa.prisoner_number,
       aoa.booking_id,
       a.category_code,
       a.appointment_description,
       CASE
           WHEN ao.in_cell THEN null
           ELSE ao.internal_location_id END                                          AS internal_location_id,
       ao.in_cell,
       ao.start_date                                                                 AS appointment_date,
       ao.start_time,
       ao.end_time,
       COALESCE(ao.comment, a.comment)                                               AS comment,
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
