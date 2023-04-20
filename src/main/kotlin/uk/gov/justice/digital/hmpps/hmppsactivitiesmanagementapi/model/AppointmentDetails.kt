package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Schema(
  description =
  """
  The top level appointment details for display purposes. Contains only properties needed to make additional API calls
  and to display.
  """,
)
data class AppointmentDetails(
  @Schema(
    description = "The internally generated identifier for this appointment",
    example = "12345",
  )
  val id: Long,

  @Schema(
    description = "The appointment type (INDIVIDUAL or GROUP)",
    example = "INDIVIDUAL",
  )
  val appointmentType: AppointmentType,

  @Schema(
    description =
    """
    The summary of the appointment's category
    """,
  )
  val category: AppointmentCategorySummary,

  @Schema(
    description =
    """
    The NOMIS AGENCY_LOCATIONS.AGY_LOC_ID value for mapping to NOMIS.
    Note, this property does not exist on the appointment occurrences and is therefore consistent across all occurrences
    """,
    example = "SKI",
  )
  val prisonCode: String,

  @Schema(
    description =
    """
    The summary of the internal location this appointment will take place. Will be null if in cell = true
    """,
  )
  val internalLocation: AppointmentLocationSummary?,

  @Schema(
    description =
    """
    Flag to indicate if the location of the appointment is in cell rather than an internal prison location.
    Internal location will be null if in cell = true
    """,
    example = "false",
  )
  val inCell: Boolean,

  @Schema(
    description = "The date of the appointment or first appointment occurrence in the series",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val startDate: LocalDate,

  @Schema(
    description = "The starting time of the appointment or first appointment occurrence in the series",
    example = "09:00",
  )
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime,

  @Schema(
    description = "The end time of the appointment or first appointment occurrence in the series",
    example = "10:30",
  )
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime?,

  @Schema(
    description =
    """
    Describes how an appointment was specified to repeat if at all. The period or frequency of the occurrences and how
    many occurrences there are in total in the series. Note that the presence of this property does not mean there is
    always more than one occurrence as a repeat count of one is valid.
    """,
  )
  val repeat: AppointmentRepeat?,

  @Schema(
    description =
    """
    Notes relating to the appointment
    """,
    example = "This appointment will help adjusting to life outside of prison",
  )
  val comment: String,

  @Schema(
    description = "The date and time this appointment was created. Will not change",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val created: LocalDateTime,

  @Schema(
    description =
    """
    The summary of the user that created this appointment
    """,
  )
  val createdBy: UserSummary,

  @Schema(
    description =
    """
    The date and time this appointment was last changed.
    Will be null if the appointment has not been edited since it was created
    """,
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val updated: LocalDateTime?,

  @Schema(
    description =
    """
    The summary of the last user to edit this appointment. Will be null if the appointment has not been altered since
    it was created
    """,
  )
  val updatedBy: UserSummary?,

  @Schema(
    description =
    """
    Summary of the individual occurrence or occurrences of this appointment. Non recurring appointments will have a single
    appointment occurrence containing the same property values as the parent appointment. The same start date, time
    and end time. Recurring appointments will have a series of occurrences. The first in the series will also
    contain the same property values as the parent appointment and subsequent occurrences will have start dates
    following on from the original start date incremented as specified by the appointment's schedule. Each occurrence
    can be edited independently of the parent. All properties of an occurrence override those of the parent appointment
    with a null coalesce back to the parent for nullable properties. The full series of occurrences specified by the
    schedule will be created in advance.
    """,
  )
  val occurrences: List<AppointmentOccurrenceSummary> = emptyList(),

  @Schema(
    description =
    """
    Summary of the prisoner or prisoners allocated to the first future occurrence (or most recent past occurrence if all
    occurrences are in the past) of this appointment. Prisoners are allocated at the occurrence level to allow for per
    occurrence allocation changes. The occurrence summary contains a count of allocated prisoners rather than the full
    list as the expected usage is to show a summary of the occurrences then a link to display the full occurrence details.
    """,
  )
  val prisoners: List<PrisonerSummary> = emptyList(),
)
