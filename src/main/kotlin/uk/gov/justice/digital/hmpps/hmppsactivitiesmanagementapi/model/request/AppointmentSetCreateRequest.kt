package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.FutureOrPresent
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Schema(
  description =
  """
  Describes a list of appointment series each with one appointment and one attendee to be created as a connected set
  """,
)
data class AppointmentSetCreateRequest(
  @field:NotEmpty(message = "Prison code must be supplied")
  @field:Size(max = 3, message = "Prison code should not exceed {max} characters")
  @Schema(
    description = "The NOMIS prison code where these appointments takes place",
    example = "PVI",
  )
  val prisonCode: String?,

  @field:NotEmpty(message = "Category code must be supplied")
  @Schema(
    description = "The NOMIS reference code for these appointments. Must exist and be active",
    example = "CHAP",
  )
  val categoryCode: String?,

  @field:NotEmpty(message = "Tier code must be supplied")
  @Schema(
    description = "The tier code for this appointment",
    example = "TIER_1",
    allowableValues = ["TIER_1", "TIER_2", "FOUNDATION"],
  )
  val tierCode: String?,

  @Schema(
    description = "The organiser code for this appointment",
    example = "PRISON_STAFF",
    allowableValues = ["PRISON_STAFF", "PRISONER", "EXTERNAL_PROVIDER", "OTHER"],
  )
  val organiserCode: String?,

  @field:Size(max = 40, message = "Custom name should not exceed {max} characters")
  @Schema(
    description =
    """
    Free text name further describing the appointment series. Will be used to create the appointment name using the
    format "Custom name (Category description) if specified.
    """,
    example = "Meeting with the governor",
  )
  val customName: String?,

  @Schema(
    description =
    """
    The NOMIS internal location id within the specified prison. This must be supplied if inCell is false.
    The internal location id must exist, must be within the prison specified by the prisonCode property and be active. 
    """,
    example = "123",
  )
  val internalLocationId: Long?,

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
  val startDate: LocalDate?,

  @field:NotEmpty(message = "At least one appointment must be supplied")
  @field:Valid
  @Schema(
    description =
    """
    The list of appointments to create
    """,
  )
  val appointments: List<AppointmentSetAppointment> = emptyList(),
) {
  @AssertTrue(message = "Internal location id must be supplied if in cell = false")
  private fun isInternalLocationId() = inCell || internalLocationId != null

  @AssertTrue(message = "Start times must be in the future")
  private fun isStartTime() = startDate == null || startDate < LocalDate.now() || appointments.all { it.startTime == null || LocalDateTime.of(startDate, it.startTime) > LocalDateTime.now() }
}

data class AppointmentSetAppointment(
  @field:NotNull(message = "Prisoner number must be supplied")
  @Schema(
    description = "The prisoner attending the appointment",
    example = "A1234BC",
  )
  val prisonerNumber: String?,

  @field:NotNull(message = "Start time must be supplied")
  @Schema(
    description = "The starting time of the appointment",
    example = "09:00",
  )
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime?,

  @field:NotNull(message = "End time must be supplied")
  @Schema(
    description = "The end time of the appointment",
    example = "10:30",
  )
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime?,

  @field:Size(max = 4000, message = "Extra information must not exceed {max} characters")
  @Schema(
    description =
    """
    Extra information for the prisoner or prisoners attending the appointment. Shown only on the appointments details
    page and on printed movement slips. Wing staff will be notified there is extra information via the unlock list.
    """,
    example = "This appointment will help adjusting to life outside of prison",
  )
  val extraInformation: String? = null,
) {
  @AssertTrue(message = "End time must be after the start time")
  private fun isEndTime() = startTime == null || endTime == null || endTime > startTime
}
