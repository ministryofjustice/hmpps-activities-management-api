package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditType
import java.time.LocalDateTime

@Schema(description = "Set of search filters for searching audit records")
data class AuditRecordSearchFilters(

  @Schema(description = "The code of the prison", example = "PVI")
  val prisonCode: String? = null,

  @Schema(description = "The prisoner number", example = "A1234AA")
  val prisonerNumber: String? = null,

  @Schema(description = "The username of the logged-in user", example = "JONESA")
  val username: String? = null,

  @Schema(description = "The top-level audit category", example = "ACTIVITY")
  val auditType: AuditType? = null,

  @Schema(description = "The specific event type", example = "ACTIVITY_CREATED")
  val auditEventType: AuditEventType? = null,

  @Schema(description = "The date and time on or after which to search for events", example = "2022-09-01T09:01:02")
  val startTime: LocalDateTime? = null,

  @Schema(description = "The date and time on or before which to search for events", example = "2022-09-01T09:01:02")
  val endTime: LocalDateTime? = null,

  @Schema(description = "The ID of the activity", example = "123456")
  val activityId: Long? = null,

  @Schema(description = "The ID of the activity schedule", example = "123456")
  val scheduleId: Long? = null,
)
