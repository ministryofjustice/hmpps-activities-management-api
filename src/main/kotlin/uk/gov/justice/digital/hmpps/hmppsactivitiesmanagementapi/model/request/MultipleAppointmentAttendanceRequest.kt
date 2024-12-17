package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

@Schema(
  description =
  """
  Describes how to update attendances for multiple appointments.
  """,
)
data class MultipleAppointmentAttendanceRequest(
  @field:NotNull(message = "Appointment id must be supplied")
  @Schema(
    description = "The appointment id of the appointment which is being marked",
    example = "123",
  )
  val appointmentId: Long?,

  @field:NotEmpty(message = "Prisoner numbers must be supplied")
  @Schema(description = "The list of prisoner numbers to update")
  val prisonerNumbers: List<String> = emptyList(),
)
