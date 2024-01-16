package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.LocalAuditRecord
import java.time.LocalDateTime

class PrisonerRemovedFromWaitingListEvent(
  val waitingListId: Long,
  val activityId: Long,
  val scheduleId: Long,
  val activityName: String,
  val prisonCode: String,
  val prisonerNumber: String,
  removedAt: LocalDateTime,
  removedBy: String,
) : AuditableEvent(
  auditType = AuditType.PRISONER,
  auditEventType = AuditEventType.PRISONER_REMOVED_FROM_WAITING_LIST,
  details = "Prisoner $prisonerNumber was removed from the waiting list ($waitingListId) for " +
    "activity '$activityName'($activityId)",
  createdAt = removedAt,
  createdBy = removedBy,
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
    scheduleId = scheduleId,
    activityName = activityName,
    prisonerNumber = prisonerNumber,
    prisonCode = prisonCode,
    createdAt = createdAt,
    createdBy = createdBy,
  )
}
