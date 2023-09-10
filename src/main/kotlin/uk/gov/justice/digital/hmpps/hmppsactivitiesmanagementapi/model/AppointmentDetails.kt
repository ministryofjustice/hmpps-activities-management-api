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
  Described on the UI as an "Appointment" and represents the scheduled event on a specific date and time.
  Contains the full details of all the appointment properties, any properties specified by the parent
  appointment series and the summary collection of prisoners attending this appointment.
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
    description = "The internally generated identifier for the parent appointment series",
    example = "12345",
  )
  val appointmentSeriesId: Long,

  @Schema(
    description = "Summary of the appointment set the parent appointment series is part of",
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
    Summary of the prisoner or prisoners attending this appointment. Attendees are at the appointment level to allow
    for per appointment attendee changes.
    """,
  )
  val prisoners: List<PrisonerSummary> = emptyList(),

  @Schema(
    description =
    """
    The summary of the appointment's category. Can be different to the parent appointment series if this appointment
    has been edited.
    """,
  )
  val category: AppointmentCategorySummary,

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
    Extra information for the prisoner or prisoners attending this appointment.
    Shown only on the appointments details page and on printed movement slips. Wing staff will be notified there is
    extra information via the unlock list.
    """,
    example = "This appointment will help adjusting to life outside of prison",
  )
  val extraInformation: String?,

  @Schema(
    description =
    """
    Describes the schedule of the parent appointment series i.e. how the appointments in the series repeat. The frequency of
    those appointments and how many appointments there are in total in the series.
    If null, the appointment series has only one appointment. Note that the presence of this property does not mean
    there is more than one appointment as a number of appointments value of one is valid.
    """,
  )
  val schedule: AppointmentSchedule?,

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
    Indicates that this appointment has been cancelled
    """,
    example = "false",
  )
  val isCancelled: Boolean,

  @Schema(
    description =
    """
    Indicates that this appointment has expired i.e. it's start date and time is in the past
    """,
    example = "false",
  )
  val isExpired: Boolean,

  @Schema(
    description = "The date and time this appointment was created. Will not change",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val createdTime: LocalDateTime,

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
    Will be null if this appointment has not been altered since it was created
    """,
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val updatedTime: LocalDateTime?,

  @Schema(
    description =
    """
    The summary of the user that last edited this appointment.
    Will be null if this appointment has not been altered since it was created
    """,
  )
  val updatedBy: UserSummary?,

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
    The summary of the user who cancelled this appointment.
    Will be null if this appointment has not been cancelled
    """,
  )
  val cancelledBy: UserSummary?,
)
