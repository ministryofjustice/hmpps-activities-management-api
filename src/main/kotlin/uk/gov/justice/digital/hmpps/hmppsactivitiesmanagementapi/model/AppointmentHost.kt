package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
  description =
  """
  The type of host for an appointment
  """,
)
data class AppointmentHost(
  @Schema(
    description = "The internally generated identifier for this appointment host",
    example = "12345",
  )
  val id: Long,

  @Schema(
    description = "The description of the appointment host",
    example = "Prison staff",
  )
  val description: String,
)
