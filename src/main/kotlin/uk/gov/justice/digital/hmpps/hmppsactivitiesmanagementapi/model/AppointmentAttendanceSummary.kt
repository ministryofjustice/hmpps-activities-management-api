package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventTierType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentAttendeeSearchResult
import java.time.LocalDate
import java.time.LocalTime

@Schema(
  description =
  """
  Contains the summary information of a limited set the appointment properties along with counts of appointment attendance
  records. Supports management level views of appointment attendance and statistics.
  """,
)
data class AppointmentAttendanceSummary(
  @Schema(description = "The internally generated identifier for this appointment", example = "123456")
  val id: Long,

  @Schema(description = "The NOMIS AGENCY_LOCATIONS.AGY_LOC_ID value for mapping to NOMIS", example = "SKI")
  val prisonCode: String,

  @Schema(
    description =
    """
    The appointment's name combining the optional custom name with the category description. If custom name has been
    specified, the name format will be "Custom name (Category description)" 
    """,
  )
  val appointmentName: String,

  @Schema(description = "The summary of the internal location this appointment will take place. Will be null if in cell = true")
  val internalLocation: AppointmentLocationSummary?,

  @Schema(description = "Flag to indicate if the location of the appointment is in cell", example = "false")
  var inCell: Boolean,

  @Schema(description = "The date this appointment is taking place on")
  @JsonFormat(pattern = "yyyy-MM-dd")
  val startDate: LocalDate,

  @Schema(description = "The starting time of this appointment", example = "13:00")
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime,

  @Schema(description = "The end time of this appointment", example = "13:30")
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime?,

  @Schema(description = "Indicates that this appointment has been cancelled", example = "false")
  val isCancelled: Boolean,

  @Schema(description = "The number of attendees for this appointment", example = "6")
  val attendeeCount: Long,

  @Schema(description = "The number of attendees recorded as having attended this appointment", example = "2")
  val attendedCount: Long,

  @Schema(description = "The number of attendees recorded as having not attended this appointment", example = "1")
  val nonAttendedCount: Long,

  @Schema(description = "The number of attendees whose attendance has not yet been recorded", example = "3")
  val notRecordedCount: Long,

  @Schema(
    description =
    """
    The prisoner or prisoners attending this appointment. Appointments of type INDIVIDUAL will have one
    prisoner attending each appointment. Appointments of type GROUP can have more than one prisoner
    attending each appointment
    """,
  )
  val attendees: List<AppointmentAttendeeSearchResult>,

  @Schema(description = "optional event tier")
  val eventTierType: EventTierType? = null,
)
