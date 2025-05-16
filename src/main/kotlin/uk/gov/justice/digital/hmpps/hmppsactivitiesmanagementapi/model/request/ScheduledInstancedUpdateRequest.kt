package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Size

@Schema(description = "Request object for updating a cancelled scheduled instance")
data class ScheduledInstancedUpdateRequest(
  @field:Size(max = 60, message = "Cancellation reason must not exceed {max} characters")
  @Schema(description = "The reason for cancelling the schedule instance", example = "No tutor available")
  val cancelledReason: String? = null,

  @field:Size(max = 250, message = "Comment cannot exceed {max} characters")
  @Schema(description = "A field for any additional comments", example = "Resume tomorrow")
  val comment: String? = null,

  @Schema(description = "Should payment be issued? Will be ignored if the activity is unpaid.", example = "true")
  val issuePayment: Boolean? = null,
) {
  @AssertTrue(message = "Comment can only be updated if cancelledReason is provided")
  private fun isCommentWithoutReason() = !(cancelledReason == null && comment != null)
}
