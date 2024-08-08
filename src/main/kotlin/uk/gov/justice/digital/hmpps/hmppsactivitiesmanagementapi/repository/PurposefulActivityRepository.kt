package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository

interface PurposefulActivityRepositoryCustom {
  fun getPurposefulActivityActivitiesReport(weekOffset: Int): MutableList<Any?>?

  fun getPurposefulActivityAppointmentsReport(weekOffset: Int): MutableList<Any?>?

  fun getPurposefulActivityPrisonRolloutReport(): MutableList<Any?>?
}

@Repository
class PurposefulActivityRepositoryImpl : PurposefulActivityRepositoryCustom {
  @PersistenceContext
  private lateinit var entityManager: EntityManager

  companion object {
    private val log = LoggerFactory.getLogger(PurposefulActivityRepositoryImpl::class.java)
  }

  private val activitiesQuery = """
    WITH date_range AS (
        SELECT
            (CURRENT_DATE - (EXTRACT(DOW FROM CURRENT_DATE)::integer + :weekOffset * 7 + 7))::timestamp AS start_date,
            ((CURRENT_DATE - (EXTRACT(DOW FROM CURRENT_DATE)::integer + :weekOffset * 7) + INTERVAL '23:59:59')::timestamp + INTERVAL '6 day') AS end_date
    )
    select
    act.activity_id as "activity.activity_id",
    act.prison_code as "activity.prison_code",
    act.activity_category_id as "activity.activity_category_id",
    actcat.code as "activity_category.code",
    actcat.name as "activity_category.name",
    actcat.description as "activity_category.description",
    act.activity_tier_id as "activity.activity_tier_id",
    tier.code as "activity_tier.code",
    tier.description as "activity_tier.description",
    act.attendance_required as "activity.attendance_required",
    act.in_cell as "activity.in_cell",
    act.piece_work as "activity.piece_work",
    act.outside_work as "activity.outside_work",
    act.summary as "activity.summary",
    act.description as "activity.description",
    act.start_date as "activity.start_date",
    act.end_date as "activity.end_date",
    act.created_time as "activity.created_time",
    act.updated_time as "activity.updated_time",
    act.on_wing as "activity.on_wing",
    act.off_wing as "activity.off_wing",
    act.paid as "activity.paid",
    asch.activity_schedule_id as "activity_schedule.activity_schedule_id",
    asch.description as "activity_schedule.description",
    asch.start_date as "activity_schedule.start_date",
    asch.end_date as "activity_schedule.end_date",
    asch.updated_time as "activity_schedule.updated_time",
    si.scheduled_instance_id as "scheduled_instance.scheduled_instance_id",
    si.session_date as "scheduled_instance.session_date",
    si.start_time as "scheduled_instance.start_time",
    si.end_time as "scheduled_instance.end_time",
    si.cancelled as "scheduled_instance.cancelled",
    si.cancelled_time as "scheduled_instance.cancelled_time",
    si.cancelled_reason as "scheduled_instance.cancelled_reason",
    allo.allocation_id as "allocation.allocation_id",
    allo.allocated_time as "allocation.allocated_time",
    allo.deallocated_time as "allocation.deallocated_time",
    allo.deallocated_reason as "allocation.deallocated_reason",
    allo.suspended_time as "allocation.suspended_time",
    allo.suspended_reason as "allocation.suspended_reason",
    allo.planned_deallocation_id as "allocation.planned_deallocation_id",
    pade.planned_date as "planned_deallocation.planned_date",
    pade.planned_reason as "planned_deallocation.planned_reason",
    att.attendance_id as "attendance.attendance_id",
    att.prisoner_number as "attendance.prisoner_number",
    att.attendance_reason_id as "attendance.attendance_reason_id",
    atre.code as "attendance_reason.code",
    atre.description as "attendance_reason.description",
    atre.attended as "attendance_reason.attended",
    att.recorded_time as "attendance.recorded_time",
    att.status as "attendance.status",
    att.pay_amount as "attendance.pay_amount",
    att.bonus_amount as "attendance.bonus_amount",
    att.pieces as "attendance.pieces",
    att.issue_payment as "attendance.issue_payment"
    from attendance att
    inner join scheduled_instance si on att.scheduled_instance_id = si.scheduled_instance_id
    	and (si.session_date || ' ' || si.start_time)::timestamp BETWEEN
       	(SELECT start_date FROM date_range) AND
       	(SELECT end_date FROM date_range)
    inner join activity_schedule asch on asch.activity_schedule_id = si.activity_schedule_id
    inner join activity act on act.activity_id = asch.activity_id
    inner join activity_category actcat on actcat.activity_category_id = act.activity_category_id
    left outer join event_tier tier on tier.event_tier_id = act.activity_tier_id
    left outer join allocation allo on allo.activity_schedule_id = asch.activity_schedule_id and allo.prisoner_number = att.prisoner_number and allo.deallocated_by is NULL
    left outer join attendance_reason atre on atre.attendance_reason_id = att.attendance_reason_id
    left outer join planned_deallocation pade on pade.allocation_id = allo.allocation_id
  
  """

  private val appointmentsQuery = """
      WITH date_range AS (
          SELECT
              (CURRENT_DATE - (EXTRACT(DOW FROM CURRENT_DATE)::integer + COALESCE(:weekOffset, 1) * 7 + 7))::timestamp AS start_date,
              ((CURRENT_DATE - (EXTRACT(DOW FROM CURRENT_DATE)::integer + COALESCE(:weekOffset, 1) * 7) + INTERVAL '23:59:59')::timestamp + INTERVAL '6 day') AS end_date
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

    """

  private val rolloutPrisonQuery = """
    select
    rollout_prison.rollout_prison_id as "rollout_prison.rollout_prison_id",
    rollout_prison.code as "rollout_prison.code",
    rollout_prison.description as "rollout_prison.description",
    rollout_prison.activities_to_be_rolled_out as "rollout_prison.activities_to_be_rolled_out",
    rollout_prison.activities_rollout_date as "rollout_prison.activities_rollout_date",
    rollout_prison.appointments_to_be_rolled_out as "rollout_prison.appointments_to_be_rolled_out",
    rollout_prison.appointments_rollout_date as "rollout_prison.appointments_rollout_date"
    from rollout_prison rollout_prison
  """

  @Transactional
  @Override
  override fun getPurposefulActivityActivitiesReport(weekOffset: Int): MutableList<Any?>? {
    val results = entityManager.createNativeQuery(
      activitiesQuery.replace(":weekOffset", weekOffset.toString()),
    ).resultList

    return results
  }

  @Transactional
  @Override
  override fun getPurposefulActivityAppointmentsReport(weekOffset: Int): MutableList<Any?>? {
    val results = entityManager.createNativeQuery(
      appointmentsQuery.replace(":weekOffset", weekOffset.toString()),
    ).resultList

    return results
  }

  @Transactional
  @Override
  override fun getPurposefulActivityPrisonRolloutReport(): MutableList<Any?>? {
    val results = entityManager.createNativeQuery(rolloutPrisonQuery).resultList

    return results
  }
}
