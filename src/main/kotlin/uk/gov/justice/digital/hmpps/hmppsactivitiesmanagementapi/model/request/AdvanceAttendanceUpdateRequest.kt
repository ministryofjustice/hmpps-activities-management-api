package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "Request object for creating an advance attendance record")
data class AdvanceAttendanceUpdateRequest(
  @Schema(description = "Should payment be issued?", example = "true")
  @field:NotNull(message = "Issue payment must be supplied")
  val issuePayment: Boolean? = null,
)
