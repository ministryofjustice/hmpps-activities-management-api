package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.LocalAuditRecord
import java.time.LocalDateTime

class PrisonerAllocatedEvent(
  val activityId: Long,
  val activityName: String,
  val prisonCode: String,
  val prisonerNumber: String,
  val scheduleId: Long,
  val scheduleDescription: String,
  val waitingListId: Long? = null,
  createdAt: LocalDateTime,
) : AuditableEvent(
  auditType = AuditType.PRISONER,
  auditEventType = AuditEventType.PRISONER_ALLOCATED,
  details = "Prisoner $prisonerNumber was allocated to " +
    "activity '$activityName'($activityId) and schedule $scheduleDescription($scheduleId)" +
    (waitingListId?.let { " for waiting list '$it'" } ?: ""),
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
    createdAt = createdAt,
    createdBy = createdBy,
  )
}
