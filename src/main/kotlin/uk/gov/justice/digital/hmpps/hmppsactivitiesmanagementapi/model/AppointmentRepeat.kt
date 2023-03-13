package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

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
  val period: AppointmentRepeatPeriod?,
  @field:NotNull(message = "Repeat count must be supplied")
  @field:Size(min = 1, message = "Repeat count must be {min} or greater")
  @Schema(
    description =
    """
    The total number of occurrences in the appointment series
    """,
    example = "6",
  )
  val count: Int?,
)
