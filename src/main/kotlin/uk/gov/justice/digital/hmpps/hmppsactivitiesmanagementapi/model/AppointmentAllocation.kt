package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
  description =
  """
  """
)
data class AppointmentAllocation (
  @Schema(
    description = "The internally-generated identifier for this appointment allocation"
  )
  val id: Long,

  @Schema(
    description = "",
    example = ""
  )
  val prisonerNumber: String,

  @Schema(
    description = "",
    example = ""
  )
  val bookingId: Int
)
