package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Schema(
  description =
  """
  Summarises a specific appointment occurrence. Will contain copies of the parent appointment's properties unless they
  have been changed on this appointment occurrence.
  """,
)
data class AppointmentOccurrenceSummary(
  @Schema(
    description = "The internally generated identifier for this appointment occurrence",
    example = "123456",
  )
  val id: Long,

  @Schema(
    description = "The sequence number of this appointment occurrence within the recurring appointment series",
    example = "3",
  )
  val sequenceNumber: Int,

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
    The date and time this appointment occurrence was last edited.
    Will be null if the appointment occurrence has not been independently changed from the original state it was in when
    it was created as part of a recurring series
    """,
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val updated: LocalDateTime?,

  @Schema(
    description =
    """
    The summary of the last user to edit this appointment occurrence. Will be null if the appointment occurrence has not
    been independently changed from the original state it was in when it was created as part of a recurring series
    """,
  )
  val updatedBy: UserSummary?,
)
