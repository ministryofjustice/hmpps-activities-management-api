CREATE OR REPLACE VIEW v_appointment_instance
AS
SELECT aa.appointment_attendee_id AS appointment_instance_id,
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
           WHEN a.in_cell THEN NULL::bigint
           ELSE a.internal_location_id
           END AS internal_location_id,
       a.custom_location,
       a.in_cell,
       a.on_wing,
       a.off_wing,
       a.start_date AS appointment_date,
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
           ELSE NOT acr.is_delete
           END AS is_cancelled,
       a.cancelled_time,
       a.cancelled_by,
       CASE
           WHEN aa.removal_reason_id IS NULL THEN false
           ELSE NOT aarr.is_delete
           END AS is_removed,
       aa.removed_time,
       aa.removed_by,
       asrs.cancellation_start_date AS series_cancellation_start_date,
       asrs.cancellation_start_time AS series_cancellation_start_time,
       ass.frequency AS series_frequency
FROM appointment_attendee aa
         JOIN appointment a ON aa.appointment_id = a.appointment_id
         JOIN appointment_series asrs ON asrs.appointment_series_id = a.appointment_series_id
         LEFT JOIN appointment_series_schedule ass on ass.appointment_series_schedule_id = asrs.appointment_series_schedule_id
         LEFT JOIN appointment_cancellation_reason acr ON a.cancellation_reason_id = acr.appointment_cancellation_reason_id
         LEFT JOIN appointment_attendee_removal_reason aarr ON aa.removal_reason_id = aarr.appointment_attendee_removal_reason_id
WHERE NOT aa.is_deleted AND NOT a.is_deleted;