package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Schema(
  description =
  """
  Described on the UI as an "Appointment" and represents the scheduled event on a specific date and time.
  All updates and cancellations happen at this appointment level with the parent appointment series being immutable.
  """,
)
data class Appointment(
  @Schema(
    description = "The internally generated identifier for this appointment",
    example = "123456",
  )
  val id: Long,

  @Schema(
    description = "The sequence number of this appointment within the appointment series",
    example = "3",
  )
  val sequenceNumber: Int,

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

  @Schema(description = "The tier for this appointment, as defined by the Future Prison Regime team")
  val tier: EventTier?,

  @Schema(description = "The organiser of this appointment")
  val organiser: EventOrganiser?,

  @Schema(
    description =
    """
    Free text name further describing the appointment. Used as part of the appointment name with the
    format "Custom name (Category description) if specified.
    """,
    example = "Meeting with the governor",
  )
  val customName: String?,

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
    description = "The date this appointment is taking place on",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val startDate: LocalDate,

  @Schema(
    description = "The starting time of this appointment",
    example = "13:00",
  )
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime,

  @Schema(
    description = "The end time of this appointment",
    example = "13:30",
  )
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime?,

  @Schema(
    description =
    """
    Extra information for the prisoner or prisoners attending this appointment.
    Shown only on the appointments details page and on printed movement slips. Wing staff will be notified there is
    extra information via the unlock list.
    """,
    example = "This appointment will help adjusting to life outside of prison",
  )
  val extraInformation: String?,

  @Schema(
    description = "The date and time this appointment was created. Will not change",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val createdTime: LocalDateTime,

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
    Will be null if this appointment has not been altered since it was created
    """,
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val updatedTime: LocalDateTime?,

  @Schema(
    description =
    """
    The username of the user authenticated via HMPPS auth that edited this appointment.
    Will be null if this appointment has not been altered since it was created
    """,
    example = "AAA01U",
  )
  val updatedBy: String?,

  @Schema(
    description =
    """
    The date and time this appointment was cancelled.
    Will be null if this appointment has not been cancelled
    """,
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  var cancelledTime: LocalDateTime?,

  @Schema(
    description =
    """
    The id of the reason why this appointment was cancelled.
    Will be null if this appointment has not been cancelled
    """,
    example = "12345",
  )
  val cancellationReasonId: Long?,

  @Schema(
    description =
    """
    The username of the user authenticated via HMPPS auth that cancelled this appointment.
    Will be null if this appointment has not been cancelled
    """,
    example = "AAA01U",
  )
  val cancelledBy: String?,

  @Schema(
    description =
    """
    Indicates that this appointment has been deleted and removed from scheduled events.
    """,
    example = "false",
  )
  val isDeleted: Boolean,

  @Schema(
    description =
    """
    The prisoner or prisoners attending this appointment. Single appointments such as medical will have one
    attendee. A group appointment e.g. gym or chaplaincy sessions will have more than one attendee.
    Attendees are at the appointment level supporting alteration of attendees in any future appointment.
    """,
  )
  val attendees: List<AppointmentAttendee> = emptyList(),
) {
  fun isCancelled() = cancelledTime != null
}
