package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Schema(
  description =
  """
  Described on the UI as an "Appointment" and represents the scheduled event on a specific date and time.
  Contains the full details of all the appointment properties and the summary collection of prisoners attending this appointment.
  An appointment is part of either a series of an appointments on a schedule or a set of appointments on the same day.
  The summary information of which appointment collection they are part of is included in these details.
  All updates and cancellations happen at this appointment level with the parent appointment series being immutable.
  """,
)
data class AppointmentDetails(
  @Schema(
    description = "The internally generated identifier for this appointment",
    example = "123456",
  )
  val id: Long,

  @Schema(
    description =
    """
    Summary of the appointment series this appointment is part of.
    Will be null if this appointment is part of a set of appointments on the same day.
    """,
  )
  val appointmentSeries: AppointmentSeriesSummary?,

  @Schema(
    description =
    """
    Summary of the appointment set this appointment is part of
    Will be null if this appointment is part of a series of an appointments on a schedule.
    """,
  )
  val appointmentSet: AppointmentSetSummary?,

  @Schema(
    description = "The appointment type (INDIVIDUAL or GROUP)",
    example = "INDIVIDUAL",
  )
  val appointmentType: AppointmentType,

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
    description =
    """
    The appointment's name combining the optional custom name with the category description. If custom name has been
    specified, the name format will be "Custom name (Category description)" 
    """,
  )
  val appointmentName: String,

  @Schema(
    description =
    """
    Summary of the prisoner or prisoners attending this appointment and their attendance record if any.
    Attendees are at the appointment level to allow for per appointment attendee changes.
    """,
  )
  val attendees: List<AppointmentAttendeeSummary> = emptyList(),

  @Schema(
    description =
    """
    The summary of the appointment's category. Can be different to the parent appointment series if this appointment
    has been edited.
    """,
  )
  val category: AppointmentCategorySummary,

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
    The summary of the internal location this appointment will take place. Can be different to the parent
    appointment series if this appointment has been edited.
    Will be null if in cell = true
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
    Indicates that this appointment has expired i.e. it's start date and time is in the past
    """,
    example = "false",
  )
  val isExpired: Boolean,

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
    The username of the user that created this appointment
    """,
  )
  val createdBy: String,

  @Schema(
    description =
    """
    Indicates that this appointment has been independently changed from the original state it was in when
    it was created as part of an appointment series
    """,
    example = "false",
  )
  val isEdited: Boolean,

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
    The username of the user that last edited this appointment.
    Will be null if this appointment has not been altered since it was created
    """,
  )
  val updatedBy: String?,

  @Schema(
    description =
    """
    Indicates that this appointment has been cancelled
    """,
    example = "false",
  )
  val isCancelled: Boolean,

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
    The date and time this appointment was cancelled.
    Will be null if this appointment has not been cancelled
    """,
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val cancelledTime: LocalDateTime?,

  @Schema(
    description =
    """
    The username of the user who cancelled this appointment.
    Will be null if this appointment has not been cancelled
    """,
  )
  val cancelledBy: String?,
)
