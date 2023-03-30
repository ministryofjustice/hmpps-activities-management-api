package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.LocalAuditRecord
import java.time.LocalDate
import java.time.LocalDateTime

class ActivityUpdatedEvent(
  val activityId: Long,
  val activityName: String,
  val prisonCode: String,
  val categoryCode: String,
  val startDate: LocalDate,
  createdAt: LocalDateTime,

) : AuditableEvent(
  auditType = AuditType.ACTIVITY,
  auditEventType = AuditEventType.ACTIVITY_UPDATED,
  details = "An activity called '$activityName'($activityId) with category $categoryCode and starting on $startDate " +
    "at prison $prisonCode was updated",
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
    activityId = activityId,
    message = toString(),
  )
}
