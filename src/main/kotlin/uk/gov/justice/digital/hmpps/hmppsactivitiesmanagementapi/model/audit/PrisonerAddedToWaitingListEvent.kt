package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.LocalAuditRecord
import java.time.LocalDateTime

class PrisonerAddedToWaitingListEvent(
  val activityId: Long,
  val activityName: String,
  val prisonCode: String,
  val prisonerNumber: String,
  createdAt: LocalDateTime,

) : AuditableEvent(
  auditType = AuditType.PRISONER,
  auditEventType = AuditEventType.PRISONER_ADDED_TO_WAITING_LIST,
  details = "Prisoner $prisonerNumber was added to the waiting list for " +
    "activity '$activityName'($activityId)",
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
    message = toString(),
  )

  override fun toJson(): String = generateHmppsAuditJson(
    activityId = activityId,
    activityName = activityName,
    prisonerNumber = prisonerNumber,
    prisonCode = prisonCode,
    createdAt = createdAt,
    createdBy = createdBy,
  )
}
