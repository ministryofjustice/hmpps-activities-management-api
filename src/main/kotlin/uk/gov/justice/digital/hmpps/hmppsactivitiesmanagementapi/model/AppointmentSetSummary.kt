package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
  description =
  """
  Described on the UI as an "Appointment set" or "set of back-to-back appointments".
  Contains the limited summary information needed to display the fact that an appointment was created as part of a set.
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
    The number of appointments in the set that have not been deleted. Counts both appointments in the past and
    those scheduled.
    """,
    example = "3",
  )
  val appointmentCount: Int,

  @Schema(
    description =
    """
    The count of the remaining scheduled appointments in the set that have not been cancelled or deleted.
    """,
  )
  val scheduledAppointmentCount: Int,
)
