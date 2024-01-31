package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalTime

@Schema(
  description =
  """
  Described on the UI as an "Appointment" and represents the scheduled event on a specific date and time.
  Contains the summary information of a limited set the appointment properties. N.B. does not contain 
  information on the prisoners attending this appointment to improve API performance.
  All updates and cancellations happen at this appointment level with the parent appointment series or set being immutable.
  """,
)
data class AppointmentSummary(
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
    Indicates that this appointment has been deleted and removed from scheduled events.
    """,
    example = "false",
  )
  val isDeleted: Boolean,
)
