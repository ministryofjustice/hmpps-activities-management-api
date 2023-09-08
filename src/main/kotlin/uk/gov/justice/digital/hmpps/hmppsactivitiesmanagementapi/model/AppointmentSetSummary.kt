package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
  description =
  """
  Described on the UI as an "Appointment set" or "set of back-to-back appointments".
  Contains the limited summary information needed to display the fact that an appointment or appointment series was
  created as part of a set.
  """,
)
data class AppointmentSetSummary(
  @Schema(
    description = "The internally generated identifier for this appointment set",
    example = "12345",
  )
  val id: Long,

  @Schema(
    description =
    """
    The number of appointment series in the set
    """,
    example = "3",
  )
  val appointmentSeriesCount: Int,

  @Schema(
    description =
    """
    The number of appointments within the appointment series that make up the set
    """,
    example = "6",
  )
  val appointmentCount: Int,
)
