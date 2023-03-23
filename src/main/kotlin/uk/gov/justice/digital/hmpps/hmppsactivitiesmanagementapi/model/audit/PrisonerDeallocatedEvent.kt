package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class PrisonerDeallocatedEvent(
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
  createdBy: String,

) : AuditableEvent(createdAt, createdBy), HmppsAuditable {

  override fun type() = AuditEventType.PRISONER_DEALLOCATED

  override fun toString() = "Prisoner $prisonerNumber $prisonerLastName, $prisonerFirstName was deallocated from " +
    "activity '$activityName'($activityId) scheduled on $date between $startTime and $endTime (scheduleId = $scheduleId). " +
    "${super.toString()}"
}
