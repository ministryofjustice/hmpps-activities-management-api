package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.FutureOrPresent
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Schema(
  description =
  """
  Describes how to update one or more appointments.
  """,
)
data class AppointmentUpdateRequest(
  @Schema(
    description =
    """
    The updated NOMIS reference code. Must exist and be active.
    """,
    example = "GYMW",
  )
  val categoryCode: String? = null,

  @Schema(description = "The tier code for this appointment", example = "TIER_1")
  val tierCode: String? = null,

  @Schema(description = "The organiser code for this appointment", example = "PRISON_STAFF")
  val organiserCode: String? = null,

  @Schema(
    description =
    """
    The updated NOMIS internal location id within the specified prison. This must be supplied if inCell is false.
    The internal location id must exist, must be within the prison specified by the prisonCode property on the
    appointment and be active. 
    """,
    example = "123",
  )
  val internalLocationId: Long? = null,

  @Schema(
    description =
    """
    Flag to indicate if the location of the appointment or appointments is in cell rather than an internal prison location.
    Internal location id will be ignored if inCell is true
    """,
    example = "false",
  )
  val inCell: Boolean? = null,

  @field:FutureOrPresent(message = "Start date must not be in the past")
  @Schema(
    description =
    """
    The updated date of the appointment. NOTE: this property specifies the start date to use along with the existing
    schedule frequency to move all appointments that will take place after the appointment when used in conjunction
    with the applyTo property
    """,
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val startDate: LocalDate? = null,

  @Schema(
    description = "The updated starting time",
    example = "09:00",
  )
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime? = null,

  @Schema(
    description = "The updated end time",
    example = "10:30",
  )
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime? = null,

  @field:Size(max = 4000, message = "Extra information must not exceed {max} characters")
  @Schema(
    description =
    """
    Updated extra information for the prisoner or prisoners attending the appointment or appointments.
    Shown only on the appointments details page and on printed movement slips. Wing staff will be notified there is
    extra information via the unlock list.
    """,
    example = "This appointment will help adjusting to life outside of prison",
  )
  val extraInformation: String? = null,

  @Schema(
    description = "The prisoner or prisoners to remove from the appointment or appointments",
    example = "[\"A1234BC\"]",
  )
  val removePrisonerNumbers: List<String>? = null,

  @Schema(
    description = "The new prisoner or prisoners that will be attending the appointment or appointments",
    example = "[\"A1234BC\"]",
  )
  val addPrisonerNumbers: List<String>? = null,

  @Schema(
    description =
    """
    Specifies which appointment or appointments this update should apply to.
    Defaults to THIS_APPOINTMENT meaning the update will be applied to the appointment specified by the
    supplied id only.
    """,
    example = "THIS_APPOINTMENT",
  )
  val applyTo: ApplyTo = ApplyTo.THIS_APPOINTMENT,
) {
  fun isPropertyUpdate() = categoryCode != null || internalLocationId != null || inCell != null || startDate != null || startTime != null || endTime != null || extraInformation != null

  @AssertTrue(message = "Internal location id must be supplied if in cell = false")
  private fun isInternalLocationId() = inCell != false || internalLocationId != null

  @AssertTrue(message = "Start time must be in the future")
  private fun isStartTime() = startDate == null || startTime == null || startDate < LocalDate.now() || LocalDateTime.of(startDate, startTime) > LocalDateTime.now()

  @AssertTrue(message = "End time must be after the start time")
  private fun isEndTime() = startTime == null || endTime == null || endTime > startTime

  @AssertTrue(message = "Cannot update start date for all future appointments")
  private fun isApplyTo() = startDate == null || applyTo != ApplyTo.ALL_FUTURE_APPOINTMENTS
}

enum class ApplyTo {
  @Schema(
    description = "Apply the associated update request to the appointment specified by the supplied id only",
  )
  THIS_APPOINTMENT,

  @Schema(
    description =
    """
    Apply the associated update request to the appointment specified by the supplied id and all appointments
    that will take place after that appointment.
    """,
  )
  THIS_AND_ALL_FUTURE_APPOINTMENTS,

  @Schema(
    description = "Apply the associated update request to all appointments in the series that have not yet taken place",
  )
  ALL_FUTURE_APPOINTMENTS,
}
