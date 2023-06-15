package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(
  description = "Describes a set of appointments created as part of a single bulk operation",
)
data class BulkAppointment(

  @Schema(
    description = "The internally generated identifier for this set of appointments",
    example = "12345",
  )
  val id: Long,

  @Schema(
    description = "The set of appointments created in bulk",
  )
  val appointments: List<Appointment>,

  @Schema(
    description = "The date and time this set of appointment was created in bulk. Will not change",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val created: LocalDateTime,

  @Schema(
    description =
    """
    The username of the user authenticated via HMPPS auth that created this set of appointments in bulk.
    Usually a NOMIS username
    """,
    example = "AAA01U",
  )
  val createdBy: String,
)
