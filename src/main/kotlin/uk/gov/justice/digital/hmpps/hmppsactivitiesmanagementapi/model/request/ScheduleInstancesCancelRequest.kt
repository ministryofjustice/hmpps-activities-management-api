package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

@Schema(description = "Request object for cancelling a schedule instance")
data class ScheduleInstanceCancelRequest(
  @field:NotEmpty(message = "Cancellation reason must be supplied")
  @field:Size(max = 100, message = "Cancellation reason must not exceed {max} characters")
  @Schema(description = "The reason for cancelling the schedule instance", example = "No tutor available")
  val reason: String,

  @field:NotEmpty(message = "Username must be supplied")
  @Schema(description = "The username of the user cancelling the schedule instance", example = "RJ56DDE")
  val username: String,

  @Schema(description = "A field for any additional comments", example = "Resume tomorrow")
  val comment: String?,
)
