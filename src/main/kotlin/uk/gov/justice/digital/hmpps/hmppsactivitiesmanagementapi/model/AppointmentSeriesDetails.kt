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
  The top level of the standard appointment hierarchy containing full details of the initial property values common to
  all appointments in the series for display purposes.
  Contains the summary collection of all the child appointments in the series plus the schedule definition if the
  appointment series repeats.
  The properties at this level cannot be changed via the API however the child appointment property values can be changed
  independently to support rescheduling, cancelling and altered attendee lists per appointment.
  N.B. there is no collection of attending prisoners at this top level as all attendees are per appointment. This is to
  support attendee modification for each scheduled appointment and to prevent altering the past by editing attendees
  in an appointment series where some appointments have past.
  """,
)
data class AppointmentSeriesDetails(
  @Schema(
    description = "The internally generated identifier for this appointment series",
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
    description =
    """
    The appointment series' name combining the optional custom name with the category description. If custom name has been
    specified, the name format will be "Custom name (Category description)" 
    """,
  )
  val appointmentName: String,

  @Schema(
    description =
    """
    The summary of the appointment series' category
    """,
  )
  val category: AppointmentCategorySummary,

  @Schema(
    description =
    """
    Free text name further describing the appointment series. Used as part of the appointment name with the
    format "Custom name (Category description) if specified.
    """,
    example = "Meeting with the governor",
  )
  val customName: String?,

  @Schema(
    description =
    """
    The summary of the internal location this appointment series will take place. Will be null if in cell = true
    """,
  )
  val internalLocation: AppointmentLocationSummary?,

  @Schema(
    description =
    """
    Flag to indicate if the location of the appointment series is in cell rather than an internal prison location.
    Internal location id should be null if in cell = true
    """,
    example = "false",
  )
  val inCell: Boolean,

  @Schema(
    description = "The date of the first appointment in the series",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val startDate: LocalDate,

  @Schema(
    description = "The starting time of the appointment or appointments in the series",
    example = "09:00",
  )
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime,

  @Schema(
    description = "The end time of the appointment or appointments in the series",
    example = "10:30",
  )
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime?,

  @Schema(
    description =
    """
    Describes the schedule of the appointment series i.e. how the appointments in the series repeat. The frequency of
    those appointments and how many appointments there are in total in the series.
    If null, the appointment series has only one appointment. Note that the presence of this property does not mean
    there is more than one appointment as a number of appointments value of one is valid.
    """,
  )
  val schedule: AppointmentSeriesSchedule?,

  @Schema(
    description =
    """
    Extra information for the prisoner or prisoners attending the appointment or appointments in the series.
    Shown only on the appointments details page and on printed movement slips. Wing staff will be notified there is
    extra information via the unlock list.
    """,
    example = "This appointment will help adjusting to life outside of prison",
  )
  val extraInformation: String?,

  @Schema(
    description = "The date and time this appointment series was created. Will not change",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val createdTime: LocalDateTime,

  @Schema(
    description =
    """
    The summary of the user that created this appointment series
    """,
  )
  val createdBy: UserSummary,

  @Schema(
    description =
    """
    The date and time one or more appointments in this series was last changed.
    Will be null if no appointments in the series have been altered since they were created
    """,
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val updatedTime: LocalDateTime?,

  @Schema(
    description =
    """
    The summary of the user that last edited one or more appointments in this series.
    Will be null if no appointments in the series have been altered since they were created
    """,
  )
  val updatedBy: UserSummary?,

  @Schema(
    description =
    """
    Summary of the individual appointment or appointments in this series both expired and scheduled.
    Non recurring appointment series will have a single appointment containing the same property values as the parent
    appointment series. The same start date, time and end time. Recurring appointment series will have one or more
    appointments. The first in the series will also contain the same property values as the parent appointment series
    and subsequent appointments will have start dates following on from the original start date incremented as specified
    by the series' schedule. Each appointment can be edited independently of the parent. All properties of an
    appointment are separate to those of the parent appointment series.
    The full series of appointments specified by the schedule will have been created in advance.
    """,
  )
  val appointments: List<AppointmentSummary> = emptyList(),
)
