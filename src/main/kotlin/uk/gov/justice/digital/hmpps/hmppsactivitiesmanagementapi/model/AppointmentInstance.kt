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
  Represents an appointment instance for a specific prisoner to attend at the specified location, date and time.
  The fully denormalised representation of the appointments, appointment occurrences and allocations equivalent to a
  row in the NOMIS OFFENDER_IND_SCHEDULES table.
  Appointment instances do not exist as database records and are the product of the join between appointment occurrence
  allocations, appointment occurrences and appointments. 
  The appointment occurrence allocation id is used for the appointment instance id as there is a one to one relationship
  between appointment occurrence allocations and appointment instances.
  Appointment instances are used primarily for the one way sync to NOMIS.
  """,
)
data class AppointmentInstance(
  @Schema(
    description =
    """
    The internally generated identifier for this appointment instance. N.B. this is the appointment occurrence
    allocation id due to there being a one to one relationship between appointment occurrence allocations and
    appointment instances.
    """,
    example = "123456",
  )
  val id: Long,

  @Schema(
    description = "The internally generated identifier for the appointment",
    example = "1234",
  )
  val appointmentId: Long,

  @Schema(
    description = "The internally generated identifier for the appointment occurrence",
    example = "12345",
  )
  val appointmentOccurrenceId: Long,

  @Schema(
    description =
    """
    The internally generated identifier for the appointment occurrence allocation. N.B. this is used as the appointment
    instance id due to there being a one to one relationship between appointment occurrence allocations and appointment
    instances.
    """,
    example = "123456",
  )
  val appointmentOccurrenceAllocationId: Long,

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
    description = "The NOMIS OFFENDERS.OFFENDER_ID_DISPLAY value for mapping to a prisoner record in NOMIS",
    example = "A1234BC",
  )
  val prisonerNumber: String,

  @Schema(
    description = "The NOMIS OFFENDER_BOOKINGS.OFFENDER_BOOK_ID value for mapping to a prisoner booking record in NOMIS",
    example = "456",
  )
  val bookingId: Long,

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
    description = "The date of the appointment instance",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val appointmentDate: LocalDate,

  @Schema(
    description = "The starting time of the appointment instance",
    example = "09:00",
  )
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime,

  @Schema(
    description = "The end time of the appointment instance",
    example = "10:30",
  )
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime?,

  @Schema(
    description =
    """
    Notes relating to the appointment instance.
    Could support adding a note specific to an individual prisoner's attendance of a specific group appointment
    occurrence. Something that is supported within existing systems
    """,
    example = "This appointment will help prisoner A1234BC adjust to life outside of prison",
  )
  val comment: String?,

  @Schema(
    description = "The date and time this appointment instance was created. Will not change",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val created: LocalDateTime,

  @Schema(
    description =
    """
    The username of the user authenticated via HMPPS auth that created the appointment instance.
    Usually a NOMIS username
    """,
    example = "AAA01U",
  )
  val createdBy: String,

  @Schema(
    description =
    """
    The date and time this appointment instance was last changed.
    Will be null if the appointment instance has not been altered since it was created
    """,
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val updated: LocalDateTime?,

  @Schema(
    description =
    """
    The username of the user authenticated via HMPPS auth that edited the appointment instance.
    Will be null if the appointment instance has not been altered since it was created
    """,
    example = "AAA01U",
  )
  val updatedBy: String?,
)
