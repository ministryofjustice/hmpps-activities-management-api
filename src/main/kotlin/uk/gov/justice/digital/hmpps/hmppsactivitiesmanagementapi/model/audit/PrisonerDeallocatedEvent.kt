package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.LocalAuditRecord
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class PrisonerDeallocatedEvent(
  val activityId: Long,
  val activityName: String,
  val prisonCode: String,
  val prisonerNumber: String,
  val scheduleId: Long,
  val deallocationTime: LocalDateTime,
  val reason: String,
  val deallocatedBy: String,
  createdAt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
) : AuditableEvent(
  auditType = AuditType.PRISONER,
  auditEventType = AuditEventType.PRISONER_DEALLOCATED,
  details = "Prisoner $prisonerNumber was deallocated from " +
    "activity '$activityName'($activityId) and schedule ($scheduleId) on ${deallocationTime.toLocalDate()} at ${deallocationTime.toLocalTime()}",
  createdTime = createdAt,
  createdBy = deallocatedBy,
),
  HmppsAuditable,
  LocalAuditable {
  override fun toLocalAuditRecord(): LocalAuditRecord = LocalAuditRecord(
    username = deallocatedBy,
    auditType = auditType,
    detailType = auditEventType,
    recordedTime = createdTime,
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
    createdAt = createdTime,
    createdBy = deallocatedBy,
  )
}
