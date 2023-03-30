package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditType
import java.time.LocalDateTime

data class AuditRecordSearchFilters(
  val prisonCode: String? = null,
  val username: String? = null,
  val auditType: AuditType? = null,
  val auditEventType: AuditEventType? = null,
  val startTime: LocalDateTime? = null,
  val endTime: LocalDateTime? = null,
  val activityId: Long? = null,
  val scheduleId: Long? = null,
)
