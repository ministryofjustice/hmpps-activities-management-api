package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
  description =
  """
  Describes how to mark attendance for an appointment.
  """,
)
data class AppointmentAttendanceRequest(
  @Schema(
    description = "The prisoner or prisoners that attended the appointment",
    example = "[\"A1234BC\"]",
  )
  val attendedPrisonNumbers: List<String> = emptyList(),

  @Schema(
    description = "The prisoner or prisoners that did not attended the appointment",
    example = "[\"A1234BC\"]",
  )
  val nonAttendedPrisonNumbers: List<String> = emptyList(),
)
