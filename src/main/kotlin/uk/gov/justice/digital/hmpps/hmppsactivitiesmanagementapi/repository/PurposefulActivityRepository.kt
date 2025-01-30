package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.hibernate.jpa.HibernateHints.HINT_CACHEABLE
import org.hibernate.jpa.QueryHints.HINT_FETCH_SIZE
import org.hibernate.jpa.QueryHints.HINT_READONLY
import org.springframework.stereotype.Repository
import java.util.stream.Stream

@Repository
class PurposefulActivityRepository {
  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private val activitiesQuery = """
    WITH date_range AS (
        SELECT
            (CURRENT_DATE - (EXTRACT(DOW FROM CURRENT_DATE)::integer + :weekOffset * 7 + 7))::timestamp AS start_date,
            (CURRENT_DATE - (EXTRACT(DOW FROM CURRENT_DATE)::integer + :weekOffset * 7))::timestamp AS mid_date,
            (CURRENT_DATE - (EXTRACT(DOW FROM CURRENT_DATE)::integer + (:weekOffset - 1) * 7))::timestamp AS end_date
    )
    select
    act.activity_id as "activity_activity_id",
    act.prison_code as "activity_prison_code",
    act.activity_category_id as "activity_activity_category_id",
    actcat.code as "activity_category_code",
    actcat.name as "activity_category_name",
    act.activity_tier_id as "activity_activity_tier_id",
    tier.code as "activity_tier_code",
    tier.description as "activity_tier_description",
    act.attendance_required as "activity_attendance_required",
    act.in_cell as "activity_in_cell",
    act.piece_work as "activity_piece_work",
    act.outside_work as "activity_outside_work",
    act.summary as "activity_summary",
    act.description as "activity_description",
    act.start_date as "activity_start_date",
    act.end_date as "activity_end_date",
    act.created_time as "activity_created_time",
    act.updated_time as "activity_updated_time",
    act.on_wing as "activity_on_wing",
    act.off_wing as "activity_off_wing",
    act.paid as "activity_paid",
    asch.activity_schedule_id as "activity_schedule_activity_schedule_id",
    asch.description as "activity_schedule_description",
    asch.start_date as "activity_schedule_start_date",
    asch.end_date as "activity_schedule_end_date",
    asch.updated_time as "activity_schedule_updated_time",
    si.scheduled_instance_id as "scheduled_instance_scheduled_instance_id",
    si.session_date as "scheduled_instance_session_date",
    si.start_time as "scheduled_instance_start_time",
    si.end_time as "scheduled_instance_end_time",
    si.cancelled as "scheduled_instance_cancelled",
    si.cancelled_time as "scheduled_instance_cancelled_time",
    si.cancelled_reason as "scheduled_instance_cancelled_reason",
    att.attendance_id as "attendance_attendance_id",
    att.prisoner_number as "attendance_prisoner_number",
    att.attendance_reason_id as "attendance_attendance_reason_id",
    atre.code as "attendance_reason_code",
    atre.description as "attendance_reason_description",
    atre.attended as "attendance_reason_attended",
    att.recorded_time as "attendance_recorded_time",
    att.status as "attendance_status",
    att.pay_amount as "attendance_pay_amount",
    att.bonus_amount as "attendance_bonus_amount",
    att.pieces as "attendance_pieces",
    att.issue_payment as "attendance_issue_payment",
    CASE
      WHEN si.session_date < (select mid_date from date_range) THEN 'Final'
    ELSE
      'Provisional'
    END AS "record_status"
    from attendance att
    inner join scheduled_instance si on att.scheduled_instance_id = si.scheduled_instance_id
    	and (si.session_date || ' ' || si.start_time)::timestamp BETWEEN
       	(SELECT start_date FROM date_range) AND
       	(SELECT end_date FROM date_range)
    inner join activity_schedule asch on asch.activity_schedule_id = si.activity_schedule_id
    inner join activity act on act.activity_id = asch.activity_id
    inner join activity_category actcat on actcat.activity_category_id = act.activity_category_id
    left outer join event_tier tier on tier.event_tier_id = act.activity_tier_id
    left outer join attendance_reason atre on atre.attendance_reason_id = att.attendance_reason_id
  
  """

  private val activitiesQueryHeaders = listOf(
    "activity_activity_id", "activity_prison_code", "activity_activity_category_id", "activity_category_code",
    "activity_category_name", "activity_activity_tier_id", "activity_tier_code", "activity_tier_description",
    "activity_attendance_required", "activity_in_cell", "activity_piece_work", "activity_outside_work",
    "activity_summary", "activity_description", "activity_start_date", "activity_end_date",
    "activity_created_time", "activity_updated_time", "activity_on_wing", "activity_off_wing", "activity_paid",
    "activity_schedule_activity_schedule_id", "activity_schedule_description", "activity_schedule_start_date",
    "activity_schedule_end_date", "activity_schedule_updated_time", "scheduled_instance_scheduled_instance_id",
    "scheduled_instance_session_date", "scheduled_instance_start_time", "scheduled_instance_end_time",
    "scheduled_instance_cancelled", "scheduled_instance_cancelled_time", "scheduled_instance_cancelled_reason",
    "attendance_attendance_id", "attendance_prisoner_number", "attendance_attendance_reason_id",
    "attendance_reason_code", "attendance_reason_description", "attendance_reason_attended",
    "attendance_recorded_time", "attendance_status", "attendance_pay_amount", "attendance_bonus_amount",
    "attendance_pieces", "attendance_issue_payment", "record_status",
  )

  private val appointmentsQuery = """
  WITH date_range AS (
          SELECT
              (CURRENT_DATE - (EXTRACT(DOW FROM CURRENT_DATE)::integer + COALESCE(:weekOffset, 1) * 7 + 7))::timestamp AS start_date,
              (CURRENT_DATE - (EXTRACT(DOW FROM CURRENT_DATE)::integer + :weekOffset * 7))::timestamp AS mid_date,
              (CURRENT_DATE - (EXTRACT(DOW FROM CURRENT_DATE)::integer + (:weekOffset - 1) * 7))::timestamp AS end_date
      )
      SELECT
      a.appointment_id as "appointment_appointment_id",
      a.appointment_series_id as "appointment_appointment_series_id",
      apse.appointment_type as "appointment_series_appointment_type",
      a.sequence_number as "appointment_sequence_number",
      asas.appointment_set_id as "appointment_set_appointment_set_id",
      a.prison_code as "appointment_prison_code",
      a.category_code as "appointment_category_code",
      a.custom_name as "appointment_custom_name",
      a.appointment_tier_id as "appointment_appointment_tier_id",
      tier.description as "appointment_tier_description",
      a.appointment_organiser_id as "appointment_appointment_organiser_id",
      eo.description as "event_organiser_event_organiser_description",
      a.internal_location_id as "appointment_internal_location_id",
      a.custom_location as "appointment_custom_location",
      a.in_cell as "appointment_in_cell",
      a.on_wing as "appointment_on_wing",
      a.off_wing as "appointment_off_wing",
      a.start_date as "appointment_start_date",
      a.start_time as "appointment_start_time",
      a.end_time as "appointment_end_time",
      a.created_time as "appointment_created_time",
      a.updated_time as "appointment_updated_time",
      a.cancelled_time as "appointment_cancelled_time",
      a.cancellation_reason_id as "appointment_cancellation_reason_id",
      acr.description as "appointment_cancellation_reason_description",
      acr.is_delete as "appointment_cancellation_reason_is_delete",
      a.is_deleted as "appointment_is_deleted",
      aa.appointment_attendee_id as "appointment_attendee_appointment_attendee_id",
      aa.prisoner_number as "appointment_attendee_prisoner_number",
      aa.booking_id as "appointment_attendee_booking_id",
      aa.added_time as "appointment_attendee_added_time",
      aa.attended as "appointment_attendee_attended",
      aa.attendance_recorded_time as "appointment_attendee_attendance_recorded_time",
      aa.removed_time as "appointment_attendee_removed_time",
      aa.is_deleted as "appointment_attendee_is_deleted",
      CASE
        WHEN a.start_date < (select mid_date FROM date_range) THEN 'Final'
      ELSE
        'Provisional'
      END AS "record_status"
      FROM appointment a
      inner join appointment_series apse on apse.appointment_series_id = a.appointment_series_id
      left outer join event_tier tier on tier.event_tier_id = a.appointment_tier_id
      left outer join appointment_set_appointment_series asas on asas.appointment_series_id = apse.appointment_series_id
      left outer join appointment_cancellation_reason acr on acr.appointment_cancellation_reason_id = a.cancellation_reason_id
      left outer join event_organiser eo on eo.event_organiser_id = a.appointment_organiser_id
      inner join appointment_attendee aa on aa.appointment_id = a.appointment_id
      WHERE (a.start_date || ' ' || a.start_time)::timestamp BETWEEN
         (SELECT start_date FROM date_range) AND
         (SELECT end_date FROM date_range);
  """

  private val appointmentQueryHeaders = listOf(
    "appointment_appointment_id", "appointment_appointment_series_id", "appointment_series_appointment_type",
    "appointment_sequence_number", "appointment_set_appointment_set_id", "appointment_prison_code",
    "appointment_category_code", "appointment_custom_name", "appointment_appointment_tier_id",
    "appointment_tier_description", "appointment_appointment_organiser_id", "event_organiser_event_organiser_description",
    "appointment_internal_location_id", "appointment_custom_location", "appointment_in_cell", "appointment_on_wing",
    "appointment_off_wing", "appointment_start_date", "appointment_start_time", "appointment_end_time",
    "appointment_created_time", "appointment_updated_time", "appointment_cancelled_time",
    "appointment_cancellation_reason_id", "appointment_cancellation_reason_description",
    "appointment_cancellation_reason_is_delete", "appointment_is_deleted", "appointment_attendee_appointment_attendee_id",
    "appointment_attendee_prisoner_number", "appointment_attendee_booking_id", "appointment_attendee_added_time",
    "appointment_attendee_attended", "appointment_attendee_attendance_recorded_time", "appointment_attendee_removed_time",
    "appointment_attendee_is_deleted", "record_status",
  )

  fun getPurposefulActivityActivitiesReport(weekOffset: Int) = getData(weekOffset, activitiesQuery, activitiesQueryHeaders)

  fun getPurposefulActivityAppointmentsReport(weekOffset: Int) = getData(weekOffset, appointmentsQuery, appointmentQueryHeaders)

  private fun getData(weekOffset: Int, query: String, headers: List<String>): Stream<*> {
    val query = entityManager.createNativeQuery(query)

    query.setParameter("weekOffset", weekOffset)

    query.setHint(HINT_FETCH_SIZE, "500")
    query.setHint(HINT_CACHEABLE, "false")
    query.setHint(HINT_READONLY, "false")

    return Stream.concat(Stream.of(headers.toTypedArray()), query.resultStream)
  }
}
