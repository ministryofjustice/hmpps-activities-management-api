package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class IncentiveLevelWarningGivenForActivityAttendanceEvent(
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

  override fun type() = AuditEventType.INCENTIVE_LEVEL_WARNING_GIVEN_FOR_ACTIVITY_ATTENDANCE

  override fun toString() = "An incentive level warning was given to prisoner $prisonerNumber $prisonerLastName, $prisonerFirstName " +
    "for activity '$activityName'($activityId) scheduled on $date between $startTime and $endTime (scheduleId = $scheduleId). " +
    "${super.toString()}"
}
