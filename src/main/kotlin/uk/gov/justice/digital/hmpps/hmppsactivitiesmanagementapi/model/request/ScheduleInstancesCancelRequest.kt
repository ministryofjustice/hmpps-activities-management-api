package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Schema(description = "Request object for cancelling multiple schedule instances")
data class ScheduleInstancesCancelRequest(

  @field:NotEmpty(message = "At least one scheduled instance id must be supplied")
  @Schema(description = "The scheduled instance ids to cancel")
  val scheduleInstanceIds: List<Long>?,

  @field:NotEmpty(message = "Cancellation reason must be supplied")
  @field:Size(max = 100, message = "Cancellation reason must not exceed {max} characters")
  @Schema(description = "The reason for cancelling the schedule instances", example = "No tutor available")
  val reason: String,

  @field:NotEmpty(message = "Username must be supplied")
  @Schema(description = "The username of the user cancelling the schedule instances", example = "RJ56DDE")
  val username: String,

  @Schema(description = "A field for any additional comments", example = "Resume tomorrow")
  val comment: String?,

  @field:NotNull(message = "Issue payment must be supplied")
  @Schema(description = "Should payment be issued? Will be ignored if the activity is unpaid.", example = "true")
  val issuePayment: Boolean?,
)
