package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Schema(
  description =
  """
  Represents a specific appointment occurrence. Non recurring appointments will have a single appointment occurrence
  containing the same property values as the parent appointment. The same start date, time and end time. Recurring
  appointments will have a series of occurrences. The first in the series will also contain the same property values
  as the parent appointment and subsequent occurrences will have start dates following on from the original start date
  incremented as specified by the appointment's schedule. Each occurrence can be edited independently of the parent.
  All properties of an occurrence override those of the parent appointment with a null coalesce back to the parent for
  nullable properties. The full series of occurrences specified by the schedule will be created in advance.
  """,
)
data class AppointmentOccurrence(
  @Schema(
    description = "The internally generated identifier for this appointment occurrence",
    example = "123456",
  )
  val id: Long,

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
    Flag to indicate if the location of the appointment is in cell rather than an internal prison location.
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
    Will be null if the appointment occurrence has not been altered independently from the parent appointment
    since it was created
    """,
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val updated: LocalDateTime?,

  @Schema(
    description =
    """
    The username of the user authenticated via HMPPS auth that edited this appointment instance.
    Usually a NOMIS username. Will be null if the appointment occurrence has not been altered independently from the
    parent appointment since it was created
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
