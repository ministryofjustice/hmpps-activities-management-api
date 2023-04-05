package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditType
import java.time.LocalDateTime

@Schema(description = "Describes a system even that has been recorded for audit purposes")
data class LocalAuditRecord(

  @Schema(description = "The internally-generated ID for this audit record", example = "123456")
  val localAuditId: Long = -1,

  @Schema(description = "The username of the logg-in user that triggered this event", example = "JONESA")
  val username: String,

  @Schema(description = "The top-level audit category", example = "ACTIVITY")
  val auditType: AuditType,

  @Schema(description = "The specific event type", example = "ACTIVITY_CREATED")
  val auditEventType: AuditEventType,

  @Schema(description = "The date and time at which this event occurred", example = "2022-09-01T09:01:02")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val recordedTime: LocalDateTime,

  @Schema(description = "The code of the prison to which the event relates", example = "PVI")
  val prisonCode: String,

  @Schema(description = "The prisoner number (if any) to which the event relates", example = "A1234AA")
  val prisonerNumber: String? = null,

  @Schema(description = "The ID of the activity (if any) to which the event relates", example = "123456")
  val activityId: Long? = null,

  @Schema(description = "The ID of the activity schedule (if any) to which the event relates", example = "123456")
  val activityScheduleId: Long? = null,

  @Schema(description = "A descriptive message of the event", example = "An activity called 'Some Activity'(1) with category C and starting on 2023-03-23 at prison PBI was created. Event created on 2023-03-22 at 09:00:03 by Bob.")
  val message: String,
)
