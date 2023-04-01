package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.LocalAuditRecord
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditModelUtils.generateHmppsAuditJson
import java.time.LocalDate
import java.time.LocalDateTime

class ActivityCreatedEvent(
  val activityId: Long,
  val activityName: String,
  val prisonCode: String,
  val categoryCode: String,
  val startDate: LocalDate,
  createdAt: LocalDateTime,

) : AuditableEvent(
  auditType = AuditType.ACTIVITY,
  auditEventType = AuditEventType.ACTIVITY_CREATED,
  details = "An activity called '$activityName'($activityId) with category $categoryCode and starting on $startDate " +
    "at prison $prisonCode was created",
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

  override fun toJson(): String = generateHmppsAuditJson(
    activityId = activityId,
    activityName = activityName,
    prisonCode = prisonCode,
    createdAt = createdAt,
    createdBy = createdBy,
  )
}
