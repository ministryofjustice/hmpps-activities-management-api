package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

@Schema(
  description =
  """
  Describes how an appointment will repeat. The period or frequency of those occurrences and how many occurrences there
  will be in total in the series.
  """,
)
data class AppointmentRepeat(
  @field:NotNull(message = "Repeat period must be supplied")
  @Schema(
    description =
    """
    The period or frequency of the occurrences in the repeating appointment series. When they will repeat and how often
    """,
    example = "WEEKLY",
  )
  val period: AppointmentFrequency?,
  @field:NotNull(message = "Repeat count must be supplied")
  @field:Min(value = 1, message = "Repeat count must be {value} or greater")
  @Schema(
    description =
    """
    The total number of occurrences in the appointment series
    """,
    example = "6",
  )
  val count: Int?,
)
