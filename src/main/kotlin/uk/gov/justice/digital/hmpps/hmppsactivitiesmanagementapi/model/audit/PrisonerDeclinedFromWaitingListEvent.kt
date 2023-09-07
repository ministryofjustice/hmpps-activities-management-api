package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.LocalAuditRecord
import java.time.LocalDateTime

class PrisonerDeclinedFromWaitingListEvent(
  val waitingListId: Long,
  val activityId: Long,
  val scheduleId: Long,
  val activityName: String,
  val prisonCode: String,
  val prisonerNumber: String,
  declinedAt: LocalDateTime,
  declinedBy: String,
) : AuditableEvent(
  auditType = AuditType.PRISONER,
  auditEventType = AuditEventType.PRISONER_DECLINED_FROM_WAITING_LIST,
  details = "Prisoner $prisonerNumber was declined from the waiting list ($waitingListId) for " +
    "activity '$activityName'($activityId)",
  createdTime = declinedAt,
  createdBy = declinedBy,
),
  HmppsAuditable,
  LocalAuditable {
  override fun toLocalAuditRecord(): LocalAuditRecord = LocalAuditRecord(

    username = createdBy,
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
    scheduleId = scheduleId,
    activityName = activityName,
    prisonerNumber = prisonerNumber,
    prisonCode = prisonCode,
    createdAt = createdTime,
    createdBy = createdBy,
  )
}
