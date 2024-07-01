package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeriesSchedule
import java.time.LocalDate
import java.time.LocalTime

@Schema(
  description =
  """
  Describes an appointment series to be created, the initial property values and prisoner or prisoners to allocate.
  An appointment series can have only one appointment making it non repeating or can have the number of appointments
  defined by the optional schedule.
  N.B. the full series of appointments specified by the schedule and prisoner numbers will be created in advanced and
  synced to NOMIS.
  """,
)
data class AppointmentSeriesCreateRequest(
  @field:NotNull(message = "Appointment type must be supplied")
  @Schema(
    description = "The appointment type (INDIVIDUAL or GROUP)",
    example = "INDIVIDUAL",
  )
  val appointmentType: AppointmentType?,

  @field:NotEmpty(message = "Prison code must be supplied")
  @field:Size(max = 3, message = "Prison code should not exceed {max} characters")
  @Schema(
    description = "The NOMIS prison code where this appointment series takes place",
    example = "PVI",
  )
  val prisonCode: String?,

  @field:NotEmpty(message = "At least one prisoner number must be supplied")
  @Schema(
    description = "The prisoner or prisoners attending the appointment or appointments in the series",
    example = "[\"A1234BC\"]",
  )
  val prisonerNumbers: List<String> = emptyList(),

  @field:NotEmpty(message = "Category code must be supplied")
  @Schema(
    description = "The NOMIS reference code for this appointment. Must exist and be active",
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
    Flag to indicate if the location of the appointment series is in cell rather than an internal prison location.
    Internal location id will be ignored if inCell is true
    """,
    example = "false",
  )
  val inCell: Boolean,

  @field:NotNull(message = "Start date must be supplied")
  @Schema(
    description = "The date of the first appointment in the series",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val startDate: LocalDate?,

  @field:NotNull(message = "Start time must be supplied")
  @Schema(
    description = "The starting time of the appointment or appointments in the series",
    example = "09:00",
  )
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime?,

  @field:NotNull(message = "End time must be supplied")
  @Schema(
    description = "The end time of the appointment or appointments in the series",
    example = "10:30",
  )
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime?,

  @field:Valid
  @Schema(
    description =
    """
    Describes the schedule of the appointment series i.e. how the appointments in the series will repeat. The frequency
    of those appointments and how many appointments there will be in total in the series.
    Will create a single appointment if not supplied.
    """,
  )
  val schedule: AppointmentSeriesSchedule? = null,

  @field:Size(max = 4000, message = "Extra information must not exceed {max} characters")
  @Schema(
    description =
    """
    Extra information for the prisoner or prisoners attending the appointment or appointments in the series.
    Shown only on the appointments details page and on printed movement slips. Wing staff will be notified there is
    extra information via the unlock list.
    """,
    example = "This appointment will help adjusting to life outside of prison",
  )
  val extraInformation: String? = null,

  @Schema(
    description =
    """
    The id of the original appointment that the new appointment was copied from
    """,
    example = "789",
  )
  val originalAppointmentId: Long? = 0,
) {
  @AssertTrue(message = "Cannot allocate more than one prisoner to an individual appointment")
  private fun isPrisonerNumbers() = appointmentType == AppointmentType.GROUP || prisonerNumbers.size < 2

  @AssertTrue(message = "Internal location id must be supplied if in cell = false")
  private fun isInternalLocationId() = inCell || internalLocationId != null

  @AssertTrue(message = "End time must be after the start time")
  private fun isEndTime() = startTime == null || endTime == null || endTime > startTime

  @AssertTrue(message = "Start date must not be more than 5 days ago")
  private fun isStartDate() = startDate == null || startDate > LocalDate.now().minusDays(6)
}
