package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
  description = "Summarises a set of appointments created as part of a single bulk operation",
)
data class BulkAppointmentSummary(

  @Schema(
    description = "The internally generated identifier for this set of appointments",
    example = "12345",
  )
  val bulkAppointmentId: Long,

  @Schema(
    description =
    """
    The number of appointments in the set created in bulk
    """,
    example = "3",
  )
  val appointmentCount: Int,
)
