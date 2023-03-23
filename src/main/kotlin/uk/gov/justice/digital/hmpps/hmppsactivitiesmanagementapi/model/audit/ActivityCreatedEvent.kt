package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import java.time.LocalDate
import java.time.LocalDateTime

class ActivityCreatedEvent(
  val activityId: Long,
  val activityName: String,
  val prisonCode: String,
  val categoryCode: String,
  val startDate: LocalDate,
  createdAt: LocalDateTime,
  createdBy: String,

) : AuditableEvent(createdAt, createdBy), HmppsAuditable {

  override fun type() = AuditEventType.ACTIVITY_CREATED

  override fun toString() =
    "An activity called '$activityName' with category $categoryCode and starting on $startDate " +
      "at prison $prisonCode. ${super.toString()}"
}
