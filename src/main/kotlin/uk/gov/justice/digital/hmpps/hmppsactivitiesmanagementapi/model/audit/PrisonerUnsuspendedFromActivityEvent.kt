package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class PrisonerUnsuspendedFromActivityEvent(
  val activityId: Long,
  val activityName: String,
  val prisonerNumber: String,
  val prisonerFirstName: String,
  val prisonerLastName: String,
  val scheduleId: Long,
  val date: LocalDate,
  val startTime: LocalTime,
  val endTime: LocalTime,
  createdAt: LocalDateTime,

) : AuditableEvent(createdAt), HmppsAuditable {

  override fun type() = AuditEventType.PRISONER_UNSUSPENDED_FROM_ACTIVITY

  override fun toString() = "Prisoner $prisonerNumber $prisonerLastName, $prisonerFirstName was unsuspended from " +
    "activity '$activityName'($activityId) scheduled on $date between $startTime and $endTime (scheduleId = $scheduleId). " +
    "${super.toString()}"
}
