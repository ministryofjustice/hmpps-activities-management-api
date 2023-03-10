package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
  description =
  """
  Summarises an appointment category for display purposes. Contains only properties needed to make additional API calls
  and to display.
  """,
)
data class AppointmentCategorySummary(
  @Schema(
    description = "The internally generated identifier for this appointment category",
    example = "51",
  )
  val id: Long,

  @Schema(
    description = "The NOMIS REFERENCE_CODES.CODE (DOMAIN = 'INT_SCH_RSN') value for mapping to NOMIS",
    example = "CHAP",
  )
  val code: String,

  @Schema(
    description = "The description of the appointment category",
    example = "Chaplaincy",
  )
  val description: String,
)
