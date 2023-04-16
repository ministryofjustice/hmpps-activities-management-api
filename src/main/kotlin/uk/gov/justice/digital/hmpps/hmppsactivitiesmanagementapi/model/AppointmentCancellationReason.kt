package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
  description =
  """
  The permissible appointment cancellation reason
  """,
)
data class AppointmentCancellationReason(
  @Schema(
    description = "The internally generated identifier for this cancellation reason",
    example = "12345",
  )
  val appointmentCancellationReasonId: Long = -1,

  @Schema(
    description = "A human-readable description of the cancellation reason",
    example = "Created in error",
  )
  val description: String,

  @Schema(
    description = "Whether this cancellation reason should be interpreted as a soft delete",
    example = "Created in error",
  )
  val isDelete: Boolean,

)
