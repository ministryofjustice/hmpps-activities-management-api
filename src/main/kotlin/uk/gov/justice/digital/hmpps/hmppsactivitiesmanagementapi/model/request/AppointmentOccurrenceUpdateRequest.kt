package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.FutureOrPresent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Schema(
  description =
  """
  Describes an appointment or series of appointment occurrences to be created, the initial property values and prisoner or
  prisoners to allocate. 
  """,
)
data class AppointmentOccurrenceUpdateRequest(
  @Schema(
    description =
    """
    The updated NOMIS reference code for the parent appointment. Must exist and be active.
    NOTE: updating the category will apply to all appointment occurrences as the category is associated with the
    parent appointment only. The value for applyTo will be ignored.
    """,
    example = "GYMW",
  )
  val categoryCode: String?,

  @Schema(
    description =
    """
    The updated NOMIS internal location id within the specified prison. This must be supplied if inCell is false.
    The internal location id must exist, must be within the prison specified by the prisonCode property on the
    parent appointment and be active. 
    """,
    example = "123",
  )
  val internalLocationId: Long?,

  @Schema(
    description =
    """
    Flag to indicate if the location of the appointment occurrence is in cell rather than an internal prison location.
    Internal location id will be ignored if inCell is true
    """,
    example = "false",
  )
  val inCell: Boolean?,

  @field:FutureOrPresent(message = "Start date must not be in the past")
  @Schema(
    description =
    """
    The updated date of the appointment occurrence. NOTE: this property specifies the day or date of all or all future
    occurrences when used in conjunction with the applyTo property
    """,
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val startDate: LocalDate?,

  @Schema(
    description = "The updated starting time of the appointment occurrence",
    example = "09:00",
  )
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime?,

  @Schema(
    description = "The updated end time of the appointment occurrence",
    example = "10:30",
  )
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime?,

  @Schema(
    description = "Updated notes relating to the appointment occurrence",
    example = "This appointment occurrence has been rescheduled due to staff availability",
  )
  val comment: String?,

  @Schema(
    description = "The replacement prisoner or prisoners to allocate to the appointment occurrence",
    example = "[\"A1234BC\"]",
  )
  val prisonerNumbers: List<String>?,

  @Schema(
    description =
    """
    Specifies which appointment occurrence or occurrences this update should apply to.
    Defaults to THIS_OCCURRENCE meaning the update will be applied to the appointment occurrence specified by the
    supplied id only.
    """,
    example = "10:30",
  )
  val applyTo: ApplyTo = ApplyTo.THIS_OCCURRENCE,
) {
  @AssertTrue(message = "Internal location id must be supplied if in cell = false")
  private fun isInternalLocationId() = inCell == true || internalLocationId != null

  @AssertTrue(message = "Start time must be in the future")
  private fun isStartTime() = startDate == null || startTime == null || startDate < LocalDate.now() || LocalDateTime.of(startDate, startTime) > LocalDateTime.now()

  @AssertTrue(message = "End time must be after the start time")
  private fun isEndTime() = startTime == null || endTime == null || endTime > startTime
}

enum class ApplyTo {
  @Schema(
    description = "Apply the associated update request to the appointment occurrence specified by the supplied id only",
  )
  THIS_OCCURRENCE,

  @Schema(
    description =
    """
    Apply the associated update request to the appointment occurrence specified by the supplied id and all occurrences
    that will take place after that occurrence.
    """,
  )
  THIS_AND_ALL_FUTURE_OCCURRENCES,

  @Schema(
    description = "Apply the associated update request to all appointment occurrences in the series that have not yet taken place",
  )
  ALL_FUTURE_OCCURRENCES,
}
