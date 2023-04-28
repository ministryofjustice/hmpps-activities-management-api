package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrenceAllocation
import java.time.LocalDate
import java.time.LocalTime

@Schema(
  description =
  """
  Summary search result details of a specific appointment occurrence found via search. Will contain copies of the parent
  appointment's properties unless they have been changed on this appointment occurrence. Contains properties needed to
  make additional API calls and to populate a table of search results.
  """,
)
data class AppointmentOccurrenceSearchResult(
  @Schema(
    description = "The internally generated identifier for the parent appointment",
    example = "12345",
  )
  val appointmentId: Long,

  @Schema(
    description = "The internally generated identifier for this appointment occurrence",
    example = "123456",
  )
  val appointmentOccurrenceId: Long,

  @Schema(
    description = "The parent appointment's type (INDIVIDUAL or GROUP)",
    example = "INDIVIDUAL",
  )
  val appointmentType: AppointmentType,

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
    The prisoner or prisoners attending this appointment occurrence. Appointments of type INDIVIDUAL will have one
    prisoner allocated to each appointment occurrence. Appointments of type GROUP can have more than one prisoner
    allocated to each appointment occurrence
    """,
  )
  val allocations: List<AppointmentOccurrenceAllocation> = emptyList(),

  @Schema(
    description =
    """
    The summary of the parent appointment's category
    """,
  )
  val category: AppointmentCategorySummary,

  @Schema(
    description =
    """
    Free text description for an appointment.  This is used to add more context to the appointment category.
    """,
    example = "Meeting with the governor",
  )
  val appointmentDescription: String?,

  @Schema(
    description =
    """
    The summary of the internal location this appointment occurrence will take place. Can be different to the parent
    appointment if this occurrence has been edited.
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
    description = "The date this appointment occurrence is taking place on",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val startDate: LocalDate,

  @Schema(
    description = "The starting time of this appointment occurrence",
    example = "13:00",
  )
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime,

  @Schema(
    description = "The end time of this appointment occurrence",
    example = "13:30",
  )
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime?,

  @Schema(
    description =
    "Indicates whether the parent appointment was specified to repeat",
    example = "false",
  )
  val isRepeat: Boolean,

  @Schema(
    description = "The sequence number of this appointment occurrence within the recurring appointment series",
    example = "3",
  )
  val sequenceNumber: Int,

  @Schema(
    description = "The sequence number of the final appointment occurrence within the recurring appointment series",
    example = "6",
  )
  val maxSequenceNumber: Int,

  @Schema(
    description = "Indicates whether this appointment occurrence has been changed from its original state",
    example = "false",
  )
  val isEdited: Boolean,

  @Schema(
    description = "Indicates whether this appointment occurrence has been cancelled",
    example = "false",
  )
  val isCancelled: Boolean,
)
