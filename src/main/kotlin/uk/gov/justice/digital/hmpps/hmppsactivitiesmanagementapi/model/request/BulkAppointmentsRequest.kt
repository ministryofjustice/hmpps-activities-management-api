package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.FutureOrPresent
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalTime

@Schema(
  description =
  """
  Describes a list appointment occurrences to be created in bulk
  """,
)
data class BulkAppointmentsRequest(

  @field:NotEmpty(message = "Prison code must be supplied")
  @field:Size(max = 3, message = "Prison code should not exceed {max} characters")
  @Schema(
    description = "The NOMIS prison code where these appointments takes place",
    example = "PVI",
  )
  val prisonCode: String,

  @field:NotEmpty(message = "Category code must be supplied")
  @Schema(
    description = "The NOMIS reference code for these appointments. Must exist and be active",
    example = "CHAP",
  )
  val categoryCode: String,

  @field:Size(max = 40, message = "Appointment description should not exceed {max} characters")
  @Schema(
    description =
    """
    Free text description for these appointments.  This is used to add more context to the appointment category.
    """,
    example = "Meeting with the governor",
  )
  val appointmentDescription: String?,

  @Schema(
    description =
    """
    The NOMIS internal location id within the specified prison. This must be supplied if inCell is false.
    The internal location id must exist, must be within the prison specified by the prisonCode property and be active. 
    """,
    example = "123",
  )
  val internalLocationId: Long,

  @Schema(
    description =
    """
    Flag to indicate if the location of the appointments is in cell rather than an internal prison location.
    Internal location id will be ignored if inCell is true
    """,
    example = "false",
  )
  val inCell: Boolean,

  @field:NotNull(message = "Start date must be supplied")
  @field:FutureOrPresent(message = "Start date must not be in the past")
  @Schema(
    description = "The date of the appointments",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val startDate: LocalDate,

  @Schema(
    description =
    """
    The list of appointments to create
    """,
  )
  val appointments: List<IndividualAppointment>,
)

data class IndividualAppointment(

  @field:NotNull(message = "A prisoner number must be supplied")
  @Schema(
    description = "The prisoner to allocate to the created appointment",
    example = "A1234BC",
  )
  val prisonerNumber: String,

  @field:NotNull(message = "Start time must be supplied")
  @Schema(
    description = "The starting time of the appointment",
    example = "09:00",
  )
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime,

  @field:NotNull(message = "End time must be supplied")
  @Schema(
    description = "The end time of the appointment",
    example = "10:30",
  )
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime,

  @field:Size(max = 4000, message = "Appointment comment must not exceed {max} characters")
  @Schema(
    description =
    """
    Notes relating to the appointment.
    """,
    example = "This appointment will help adjusting to life outside of prison",
  )
  val comment: String = "",
)
