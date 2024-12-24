-- This query will identify the appointments data relevant for purposeful activity reporting over a two week period
-- The weeks are considered to begin start of day sunday, and end 1 second to midnight on the saturday.
-- A two week period is included so the purposeful activity reporting team can pick up any changes/amendments to appointments made after the last report was taken

-- the integer param is a week offset. Default is 1 which means the report will run for the period up to the most recent Saturday. 
-- Increase the offset to go back in time. a value of 2 will generate the report for the two weeks up to the saturday before last.

PREPARE get_purposeful_activity_appointments_for_prior_two_weeks (integer) AS
WITH date_range AS (
    SELECT
        (CURRENT_DATE - (EXTRACT(DOW FROM CURRENT_DATE)::integer + COALESCE($1, 1) * 7 + 7))::timestamp AS start_date,
        ((CURRENT_DATE - (EXTRACT(DOW FROM CURRENT_DATE)::integer + COALESCE($1, 1) * 7) + INTERVAL '23:59:59')::timestamp + INTERVAL '6 day') AS end_date
)
SELECT 
a.appointment_id as "appointment.appointment_id",
a.appointment_series_id as "appointment.appointment_series_id",
apse.appointment_type as "appointment_series.appointment_type",
a.sequence_number as "appointment.sequence_number",
asas.appointment_set_id as "appointment_set.appointment_set_id",
a.prison_code as "appointment.prison_code",
a.category_code as "appointment.category_code",
a.custom_name as "appointment.custom_name",
a.appointment_tier_id as "appointment.appointment_tier_id",
tier.description as "appointment_tier.description",
a.internal_location_id as "appointment.internal_location_id",
a.custom_location as "appointment.custom_location",
a.in_cell as "appointment.in_cell",
a.on_wing as "appointment.on_wing",
a.off_wing as "appointment.off_wing",
a.start_date as "appointment.start_date",
a.start_time as "appointment.start_time",
a.end_time as "appointment.end_time",
a.created_time as "appointment.created_time",
a.updated_time as "appointment.updated_time",
a.cancelled_time as "appointment.cancelled_time",
a.cancellation_reason_id as "appointment.cancellation_reason_id",
acr.description as "appointment_cancellation_reason.description",
acr.is_delete as "appointment_cancellation_reason.is_delete",
a.is_deleted as "appointment.is_deleted",
aa.appointment_attendee_id as "appointment_attendee.appointment_attendee_id",
aa.prisoner_number as "appointment_attendee.prisoner_number",
aa.booking_id as "appointment_attendee.booking_id",
aa.added_time as "appointment_attendee.added_time",
aa.attended as "appointment_attendee.attended",
aa.attendance_recorded_time as "appointment_attendee.attendance_recorded_time",
aa.removed_time as "appointment_attendee.removed_time"
FROM appointment a
inner join appointment_series apse on apse.appointment_series_id = a.appointment_series_id
left outer join event_tier tier on tier.event_tier_id = a.appointment_tier_id
left outer join appointment_set_appointment_series asas on asas.appointment_series_id = apse.appointment_series_id
left outer join appointment_cancellation_reason acr on acr.appointment_cancellation_reason_id = a.cancellation_reason_id
inner join appointment_attendee aa on aa.appointment_id = a.appointment_id
WHERE (a.start_date || ' ' || a.start_time)::timestamp BETWEEN 
   (SELECT start_date FROM date_range) AND 
   (SELECT end_date FROM date_range);

-- Execute the prepared statement with different parameter values
EXECUTE get_purposeful_activity_appointments_for_prior_two_weeks(1);

DEALLOCATE get_purposeful_activity_appointments_for_prior_two_weeks;