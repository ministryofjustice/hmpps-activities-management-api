package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.LocalAuditRecord
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import java.time.LocalDateTime

class PrisonerAddedToWaitingListEvent(
  val activityId: Long,
  val activityName: String,
  val prisonCode: String,
  val prisonerNumber: String,
  val scheduleId: Long,
  val status: WaitingListStatus,
  createdBy: String,
  createdAt: LocalDateTime,
) : AuditableEvent(
  auditType = AuditType.PRISONER,
  auditEventType = AuditEventType.PRISONER_ADDED_TO_WAITING_LIST,
  details = "Prisoner $prisonerNumber was added to the waiting list for " +
    "activity '$activityName'($activityId) with a status of $status",
  createdTime = createdAt,
  createdBy = createdBy,
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
