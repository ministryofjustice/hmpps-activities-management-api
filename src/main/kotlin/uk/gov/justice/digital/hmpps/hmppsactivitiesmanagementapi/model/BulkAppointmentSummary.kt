package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
  description =
  """
  Described on the UI as an "Appointment set" or "set of back-to-back appointments".
  Contains the limited summary information needed to display the fact that an appointment occurrence was created as
  part of a set.
  """,
)
data class BulkAppointmentSummary(
  @Schema(
    description = "The internally generated identifier for this set of appointments",
    example = "12345",
  )
  val id: Long,

  @Schema(
    description =
    """
    The number of appointments in the set created in bulk
    """,
    example = "3",
  )
  val appointmentCount: Int,
)
