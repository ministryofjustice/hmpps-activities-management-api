package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
  description =
  """
  """
)
data class AppointmentInstance (
  @Schema(
    description = "The internally-generated identifier for this appointment instance"
  )
  val id: Long
)
