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
           ELSE NOT acr.is_delete END                                               AS is_cancelled,
       a.created_time,
       a.updated_time
FROM appointment a
         JOIN appointment_series asrs on asrs.appointment_series_id = a.appointment_series_id
         LEFT JOIN appointment_series_schedule asch on asrs.appointment_series_schedule_id = asch.appointment_series_schedule_id
         LEFT JOIN appointment_cancellation_reason acr on a.cancellation_reason_id = acr.appointment_cancellation_reason_id
WHERE NOT a.is_deleted;
