package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

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
  auditEventType = AuditEventType.ACTIVITY_UPDATED,
  details = "An activity called '$activityName'($activityId) with category $categoryCode and starting on $startDate " +
    "at prison $prisonCode was updated",
  createdAt = createdAt,
),
  HmppsAuditable
