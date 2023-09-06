package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
  description =
  """
  The tier of the appointment
  """,
)
data class AppointmentTier(
  @Schema(
    description = "The internally generated identifier for this appointment tier",
    example = "12345",
  )
  val appointmentTierId: Long,

  @Schema(
    description = "The description of the appointment tier",
    example = "Tier 2",
  )
  val description: String,
)
