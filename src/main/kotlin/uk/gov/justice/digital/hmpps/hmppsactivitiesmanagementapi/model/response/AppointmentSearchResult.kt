package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentLocationSummary
import java.time.LocalDate
import java.time.LocalTime

@Schema(
  description =
  """
  Summary search result details of a specific appointment found via search. Contains properties needed to
  make additional API calls and to populate a table of search results.
  """,
)
data class AppointmentSearchResult(
  @Schema(
    description = "The internally generated identifier for the appointment series",
    example = "12345",
  )
  val appointmentSeriesId: Long,

  @Schema(
    description = "The internally generated identifier for this appointment",
    example = "123456",
  )
  val appointmentId: Long,

  @Schema(
    description = "The type of the appointment series (INDIVIDUAL or GROUP)",
    example = "INDIVIDUAL",
  )
  val appointmentType: AppointmentType,

  @Schema(
    description =
    """
    The NOMIS AGENCY_LOCATIONS.AGY_LOC_ID value for mapping to NOMIS.
    """,
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
    The prisoner or prisoners attending this appointment. Appointments of type INDIVIDUAL will have one
    prisoner attending to each appointment. Appointments of type GROUP can have more than one prisoner
    attending each appointment
    """,
  )
  val attendees: List<AppointmentAttendeeSearchResult> = emptyList(),

  @Schema(
    description =
    """
    The summary of the category of this appointment
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
    The summary of the internal location this appointment will take place.
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

  @Schema(description = "time slot")
  val timeSlot: TimeSlot,

  @Schema(
    description =
    "Indicates whether the appointment series was specified to repeat via its schedule",
    example = "false",
  )
  val isRepeat: Boolean,

  @Schema(
    description = "The sequence number of this appointment within the appointment series",
    example = "3",
  )
  val sequenceNumber: Int,

  @Schema(
    description = "The sequence number of the final appointment within the appointment series",
    example = "6",
  )
  val maxSequenceNumber: Int,

  @Schema(
    description = "Indicates whether this appointment has been changed from its original state",
    example = "false",
  )
  val isEdited: Boolean,

  @Schema(
    description = "Indicates whether this appointment has been cancelled",
    example = "false",
  )
  val isCancelled: Boolean,

  @Schema(
    description = "Indicates whether this appointment has expired",
    example = "false",
  )
  val isExpired: Boolean,
)
