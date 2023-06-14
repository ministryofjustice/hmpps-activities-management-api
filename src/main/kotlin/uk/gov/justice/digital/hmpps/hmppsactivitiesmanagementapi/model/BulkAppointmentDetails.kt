package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(
  description = "Describes a set of appointments created as part of a single bulk operation",
)
data class BulkAppointmentDetails(

  @Schema(
    description = "The internally generated identifier for this set of appointments",
    example = "12345",
  )
  val bulkAppointmentId: Long,

  @Schema(
    description = "The details of the set of appointment occurrences created in bulk",
  )
  val occurrences: List<AppointmentOccurrenceDetails>,

  @Schema(
    description = "The date and time this set of appointments was created in bulk. Will not change",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val created: LocalDateTime,

  @Schema(
    description =
    """
    The summary of the user that created this set of appointments in bulk
    """,
  )
  val createdBy: UserSummary,
)
