package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.LocalAuditRecord
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditModelUtils.generateHmppsAuditJson
import java.time.LocalDateTime

class PrisonerAcceptedFromWaitingListEvent(
  val activityId: Long,
  val activityName: String,
  val prisonCode: String,
  val prisonerNumber: String,
  createdAt: LocalDateTime,

) : AuditableEvent(
  auditType = AuditType.PRISONER,
  auditEventType = AuditEventType.PRISONER_ACCEPTED_FROM_WAITING_LIST,
  details = "Prisoner $prisonerNumber was accepted onto " +
    "activity '$activityName'($activityId) from the waiting list",
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
