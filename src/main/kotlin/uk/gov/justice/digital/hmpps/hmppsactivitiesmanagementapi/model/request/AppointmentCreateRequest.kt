package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.FutureOrPresent
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalTime

@Schema(
  description =
  """
  Describes an appointment or series of appointment occurrences to be created, the initial property values and prisoner or
  prisoners to allocate. 
  """
)
data class AppointmentCreateRequest(
  @field:NotNull(message = "Category id must be supplied")
  @Schema(
    description = "The category id for this appointment. Must exist and be active",
    example = "21"
  )
  val categoryId: Long?,

  @field:NotEmpty(message = "Prison code must be supplied")
  @field:Size(max = 3, message = "Prison code should not exceed {max} characters")
  @Schema(
    description = "The NOMIS prison code where this appointment takes place",
    example = "PVI"
  )
  val prisonCode: String?,

  @Schema(
    description =
    """
    The NOMIS internal location id within the specified prison. This must be supplied if inCell is false.
    The internal location id must exist, must be within the prison specified by the prisonCode property and be active. 
    """,
    example = "123"
  )
  val internalLocationId: Long?,

  @Schema(
    description =
    """
    Flag to indicate if the location of the appointment is in cell rather than an internal prison location.
    Internal location id will be ignored if inCell is true
    """,
    example = "false"
  )
  val inCell: Boolean,

  @field:NotNull(message = "Start date must be supplied")
  @field:FutureOrPresent(message = "Start date must not be in the past")
  @Schema(
    description = "The date of the appointment or first appointment occurrence in the series"
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val startDate: LocalDate?,

  @field:NotNull(message = "Start time must be supplied")
  @Schema(
    description = "The starting time of the appointment or first appointment occurrence in the series",
    example = "09:00"
  )
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime?,

  @field:NotNull(message = "End time must be supplied")
  @Schema(
    description = "The end time of the appointment or first appointment occurrence in the series",
    example = "10:30"
  )
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime?,

  @Schema(
    description =
    """
    Notes relating to the appointment.
    The default value if no notes are specified at the occurrence or instance levels
    """,
    example = "This appointment will help adjusting to life outside of prison"
  )
  val comment: String = "",

  @field:NotEmpty(message = "At least one prisoner number must be supplied")
  @Schema(
    description = "The prisoner or prisoners to allocate to the created appointment or series of appointment occurrences",
    example = "[\"A1234BC\"]"
  )
  val prisonerNumbers: List<String> = emptyList()
) {
  @AssertTrue(message = "Internal location id must be supplied if in cell = false")
  public fun isInternalLocationIdValid() = inCell || internalLocationId != null

  @AssertTrue(message = "End time must be after the start time")
  public fun isEndTimeValid() = endTime == null || (startTime != null && endTime > startTime)
}
