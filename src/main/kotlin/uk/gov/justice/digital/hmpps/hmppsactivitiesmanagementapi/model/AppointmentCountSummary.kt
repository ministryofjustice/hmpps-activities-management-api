package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
  description =
  """
  A count summary of appointments based on a category
  """,
)
data class AppointmentCountSummary(

  @Schema(
    description = "The appointment category",
    example = "3",
  )
  val appointmentCategorySummary: AppointmentCategorySummary,

  @Schema(
    description = "The number of appointments",
    example = "25",
  )
  val count: Long,
)
