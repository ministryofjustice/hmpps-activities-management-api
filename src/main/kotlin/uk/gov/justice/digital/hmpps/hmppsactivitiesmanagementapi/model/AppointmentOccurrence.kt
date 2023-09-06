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
  All updates and cancellations happen at this occurrence level with the parent appointment being immutable.
  """,
)
data class AppointmentOccurrence(
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
    description = "The NOMIS REFERENCE_CODES.CODE (DOMAIN = 'INT_SCH_RSN') value for mapping to NOMIS",
    example = "CHAP",
  )
  val categoryCode: String,

  @Schema(
    description =
    """
    Free text description for an appointment occurrence. This is used to add more context to the appointment category.
    """,
    example = "Meeting with the governor",
  )
  val appointmentDescription: String?,

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
    Flag to indicate if the location of the appointment occurrence is in cell rather than an internal prison location.
    Internal location id should be null if in cell = true
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
    Notes relating to this appointment occurrence.
    The comment value from the parent appointment will be used if this is null
    """,
    example = "This appointment occurrence has been rescheduled due to staff availability",
  )
  val comment: String?,

  @Schema(
    description =
    """
    The time at which this appointment occurrence was cancelled (if applicable).
    """,
    example = "2023-01-02T10:45:31",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  var cancelled: LocalDateTime? = null,

  @Schema(
    description =
    """
    The ID of the reason why this appointment occurrence was cancelled (if applicable).
    """,
    example = "12345",
  )
  val cancellationReasonId: Long? = null,

  @Schema(
    description =
    """
    The username of the user authenticated via HMPPS auth that cancelled this appointment instance (if applicable).
    Usually a NOMIS username. Will be null if the appointment occurrence has not been altered independently from the
    parent appointment since it was created
    """,
    example = "AAA01U",
  )
  val cancelledBy: String? = null,

  @Schema(
    description =
    """
    The date and time this appointment occurrence was last changed.
    """,
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val updated: LocalDateTime?,

  @Schema(
    description =
    """
    The username of the user authenticated via HMPPS auth that edited this appointment instance.
    """,
    example = "AAA01U",
  )
  val updatedBy: String?,

  @Schema(
    description =
    """
    The prisoner or prisoners attending this appointment occurrence. Single appointments such as medical will have one
    allocation record. A group appointment e.g. gym or chaplaincy sessions will have more than one allocation record.
    Allocations are at the occurrence level supporting alteration of attendees in any future occurrence.
    When viewing or editing a recurring appointment, the allocations from the next appointment occurrence in the series
    will be used.
    """,
  )
  val allocations: List<AppointmentOccurrenceAllocation> = emptyList(),
) {
  fun isCancelled() = cancelled != null
}
