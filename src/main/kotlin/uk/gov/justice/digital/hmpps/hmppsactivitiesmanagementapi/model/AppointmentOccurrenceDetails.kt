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
  Contains the full details of all the appointment occurrence properties, any properties specified by the parent
  appointment and the summary collection of prisoners allocated to this occurrence.
  All updates and cancellations happen at this occurrence level with the parent appointment being immutable.
  """,
)
data class AppointmentOccurrenceDetails(
  @Schema(
    description = "The internally generated identifier for this appointment occurrence",
    example = "123456",
  )
  val id: Long,

  @Schema(
    description = "The internally generated identifier for the parent appointment",
    example = "12345",
  )
  val appointmentId: Long,

  @Schema(
    description = "Summary of the parent set of appointments created in bulk",
  )
  val bulkAppointment: BulkAppointmentSummary?,

  @Schema(
    description = "The appointment type (INDIVIDUAL or GROUP)",
    example = "INDIVIDUAL",
  )
  val appointmentType: AppointmentType,

  @Schema(
    description = "The sequence number of this appointment occurrence within the recurring appointment series",
    example = "3",
  )
  val sequenceNumber: Int,

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
    The appointment occurrence's name
    """,
  )
  val appointmentName: String,

  @Schema(
    description =
    """
    Summary of the prisoner or prisoners allocated to this appointment occurrence. Prisoners are allocated at the
    occurrence level to allow for per occurrence allocation changes.
    """,
  )
  val prisoners: List<PrisonerSummary> = emptyList(),

  @Schema(
    description =
    """
    The summary of the appointment occurrence's category. Can be different to the parent appointment if this occurrence
    has been edited.
    """,
  )
  val category: AppointmentCategorySummary,

  @Schema(
    description =
    """
    Free text description for an appointment occurrence. This is used to add more context to the category.
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
    """
    Notes relating to this appointment occurrence. Can be different to the parent appointment if this occurrence has
    been edited.
    """,
    example = "This appointment occurrence has been rescheduled due to staff availability",
  )
  val comment: String?,

  @Schema(
    description =
    """
    Describes how the parent appointment was specified to repeat if at all. The period or frequency of the occurrences
    and how many occurrences there are in total in the series. Note that the presence of this property does not mean
    there is always more than one occurrence as a repeat count of one is valid.
    """,
  )
  val repeat: AppointmentRepeat?,

  @Schema(
    description =
    """
    Indicates that this appointment occurrence has been independently changed from the original state it was in when
    it was created as part of a recurring series
    """,
    example = "false",
  )
  val isEdited: Boolean,

  @Schema(
    description =
    """
    Indicates that this appointment occurrence has been cancelled
    """,
    example = "false",
  )
  val isCancelled: Boolean,

  @Schema(
    description =
    """
    Indicates that this appointment occurrence has expired
    """,
    example = "false",
  )
  val isExpired: Boolean,

  @Schema(
    description = "The date and time the parent appointment was created. Will not change",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val created: LocalDateTime,

  @Schema(
    description =
    """
    The summary of the user that created the parent appointment
    """,
  )
  val createdBy: UserSummary,

  @Schema(
    description =
    """
    The date and time this appointment occurrence was last edited.
    """,
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val updated: LocalDateTime?,

  @Schema(
    description =
    """
    The summary of the last user to edit this appointment occurrence.
    """,
  )
  val updatedBy: UserSummary?,

  @Schema(
    description =
    """
    The date and time this appointment occurrence was cancelled
    """,
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val cancelled: LocalDateTime?,

  @Schema(
    description =
    """
    The summary of the user who cancelled this appointment occurrence
    """,
  )
  val cancelledBy: UserSummary?,
)