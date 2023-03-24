package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Schema(
  description =
  """
  The top level appointment containing the initial values for all appointment properties.
  Joins together one or more appointment occurrences and optionally a schedule if the appointment is recurring.
  The child appointment occurrences will by default have the same property values.
  The occurrence property values can be changed independently to support rescheduling, cancelling and altered
  attendee lists at an individual occurrence level.
  Editing a property at the appointment level will cascade the edit to all *future* child occurrences
  """,
)
data class Appointment(
  @Schema(
    description = "The internally generated identifier for this appointment",
    example = "12345",
  )
  val id: Long,

  @Schema(
    description = "The NOMIS REFERENCE_CODES.CODE (DOMAIN = 'INT_SCH_RSN') value for mapping to NOMIS",
    example = "CHAP",
  )
  val categoryCode: String,

  @Schema(
    description = "The NOMIS AGENCY_LOCATIONS.AGY_LOC_ID value for mapping to NOMIS",
    example = "SKI",
  )
  val prisonCode: String,

  @Schema(
    description =
    """
    The NOMIS AGENCY_INTERNAL_LOCATIONS.INTERNAL_LOCATION_ID value for mapping to NOMIS.
    Should be null if in cell = true
    """,
    example = "123",
  )
  val internalLocationId: Long?,

  @Schema(
    description =
    """
    Flag to indicate if the location of the appointment is in cell rather than an internal prison location.
    Internal location id should be null if in cell = true
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
    Notes relating to the appointment.
    The default value if no notes are specified at the occurrence or instance levels
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
    The username of the user authenticated via HMPPS auth that created the appointment.
    Usually a NOMIS username
    """,
    example = "AAA01U",
  )
  val createdBy: String,

  @Schema(
    description =
    """
    The date and time this appointment was last changed.
    Will be null if the appointment has not been altered since it was created
    """,
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val updated: LocalDateTime?,

  @Schema(
    description =
    """
    The username of the user authenticated via HMPPS auth that edited the appointment.
    Will be null if the appointment has not been altered since it was created
    """,
    example = "AAA01U",
  )
  val updatedBy: String?,

  @Schema(
    description =
    """
    Indicates that the appointment is a recurring series if not null.
    The appointment schedule properties will specify when each occurrence in the series reoccurs and on which date
    the series ends.
    """,
  )
  val schedule: AppointmentSchedule? = null,

  @Schema(
    description =
    """
    The individual occurrence or occurrences of this appointment. Non recurring appointments will have a single
    appointment occurrence containing the same property values as the parent appointment. The same start date, time
    and end time. Recurring appointments will have a series of occurrences. The first in the series will also
    contain the same property values as the parent appointment and subsequent occurrences will have start dates
    following on from the original start date incremented as specified by the appointment's schedule. Each occurrence
    can be edited independently of the parent. All properties of an occurrence override those of the parent appointment
    with a null coalesce back to the parent for nullable properties. The full series of occurrences specified by the
    schedule will be created in advance.
    """,
  )
  val occurrences: List<AppointmentOccurrence> = emptyList(),
)
