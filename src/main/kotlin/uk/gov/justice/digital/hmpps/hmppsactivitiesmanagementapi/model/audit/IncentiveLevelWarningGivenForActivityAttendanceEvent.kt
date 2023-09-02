package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.LocalAuditRecord
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class IncentiveLevelWarningGivenForActivityAttendanceEvent(
  val activityId: Long,
  val activityName: String,
  val prisonCode: String,
  val prisonerNumber: String,
  val scheduleId: Long,
  val date: LocalDate,
  val startTime: LocalTime,
  val endTime: LocalTime,
  createdAt: LocalDateTime,

) : AuditableEvent(
  auditType = AuditType.PRISONER,
  auditEventType = AuditEventType.INCENTIVE_LEVEL_WARNING_GIVEN_FOR_ACTIVITY_ATTENDANCE,
  details = "An incentive level warning was given to prisoner $prisonerNumber " +
    "for activity '$activityName'($activityId) scheduled on $date between $startTime and $endTime (scheduleId = $scheduleId)",
  createdAt = createdAt,
),
  HmppsAuditable,
  LocalAuditable {
  override fun toLocalAuditRecord(): LocalAuditRecord = LocalAuditRecord(

    username = createdBy,
    auditType = auditType,
    detailType = auditEventType,
    recordedTime = createdAt,
    prisonCode = prisonCode,
    prisonerNumber = prisonerNumber,
    activityId = activityId,
    activityScheduleId = scheduleId,
    message = toString(),
  )

  override fun toJson(): String = generateHmppsActivityAuditJson(
    activityId = activityId,
    activityName = activityName,
    prisonerNumber = prisonerNumber,
    prisonCode = prisonCode,
    scheduleId = scheduleId,
    date = date,
    startTime = startTime,
    endTime = endTime,
    createdAt = createdAt,
    createdBy = createdBy,
  )
}
