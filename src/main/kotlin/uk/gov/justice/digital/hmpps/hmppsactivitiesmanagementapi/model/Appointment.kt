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
  Described on the UI as an "Appointment series" and only shown for repeat appointments.
  The top level of the standard appointment hierarchy containing the initial property values common to all appointment
  occurrences in the series.
  Contains the collection of all the child appointment occurrences in the series plus the repeat definition if the appointment repeats.
  The properties at this level cannot be changed via the API however the child occurrence property values can be changed
  independently to support rescheduling, cancelling and altered attendee lists per occurrence.
  N.B. there is no collection of allocated prisoners at this top level as all allocations are per occurrence. This is to
  support attendee modification for each scheduled occurrence and to prevent altering the past by editing allocations
  in an appointment series where some occurrences have past.
  """,
)
data class Appointment(
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
    description = "The NOMIS AGENCY_LOCATIONS.AGY_LOC_ID value for mapping to NOMIS",
    example = "SKI",
  )
  val prisonCode: String,

  @Schema(
    description = "The NOMIS REFERENCE_CODES.CODE (DOMAIN = 'INT_SCH_RSN') value for mapping to NOMIS",
    example = "CHAP",
  )
  val categoryCode: String,

  @Schema(
    description =
    """
    Free text description for an appointment. This is used to add more context to the appointment category.
    """,
    example = "Meeting with the governor",
  )
  val appointmentDescription: String?,

  @Schema(
    description =
    """
    The NOMIS AGENCY_INTERNAL_LOCATIONS.INTERNAL_LOCATION_ID value for mapping to NOMIS.
    Will be null if in cell = true
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
    Describes how an appointment was specified to repeat if at all. The period or frequency of the occurrences and how
    many occurrences there are in total in the series. Note that the presence of this property does not mean there is
    always more than one occurrence as a repeat count of one is valid.
    """,
  )
  val repeat: AppointmentRepeat?,

  @Schema(
    description =
    """
    Notes relating to the appointment.
    The default value if no notes are specified at the occurrence or instance levels
    """,
    example = "This appointment will help adjusting to life outside of prison",
  )
  val comment: String?,

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
    The date and time one or more occurrences of this appointment was last changed.
    """,
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val updated: LocalDateTime?,

  @Schema(
    description =
    """
    The username of the user authenticated via HMPPS auth that last edited one or more occurrences of this appointment.
    """,
    example = "AAA01U",
  )
  val updatedBy: String?,

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
