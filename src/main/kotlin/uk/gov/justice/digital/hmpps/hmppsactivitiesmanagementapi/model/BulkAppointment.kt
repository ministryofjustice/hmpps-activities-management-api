package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(
  description = "Describes a list of activities created as part of a single bulk operation",
)
data class BulkAppointment(

  @Schema(
    description = "The internally generated identifier for this bulk appointment",
    example = "12345",
  )
  val bulkAppointmentId: Long = 0,

  @Schema(
    description = "The list of appointments created in bulk.",
  )
  val appointments: List<Appointment>,

  @Schema(
    description = "The date and time this appointment was created. Will not change",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val created: LocalDateTime,

  @Schema(
    description =
    """
    The username of the user authenticated via HMPPS auth that created the appointment.
    Usually a NOMIS username
    """,
    example = "AAA01U",
  )
  val createdBy: String,
)
